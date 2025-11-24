package com.example.faithquiz.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.faithquiz.data.model.Question
import com.example.faithquiz.data.repository.DataStoreRepository
import com.example.faithquiz.data.repository.PowerUpRepository
import com.example.faithquiz.data.repository.DailyChallengeRepository
import com.example.faithquiz.data.repository.GameProgressRepository
import com.example.faithquiz.data.model.PowerUpType
import com.example.faithquiz.data.model.PowerUp
import com.example.faithquiz.data.model.GameProgress
import com.example.faithquiz.data.model.LevelProgress
import com.example.faithquiz.domain.usecase.GetQuestionsForLevelUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import javax.inject.Inject
import kotlin.random.Random

@HiltViewModel
class QuizViewModel @Inject constructor(
    private val getQuestionsForLevelUseCase: GetQuestionsForLevelUseCase,
    private val dataStoreRepository: DataStoreRepository,
    private val powerUpRepository: PowerUpRepository,
    private val dailyChallengeRepository: DailyChallengeRepository,
    private val gameProgressRepository: GameProgressRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(QuizUiState())
    val uiState: StateFlow<QuizUiState> = _uiState.asStateFlow()

    private val _questions = MutableStateFlow<List<Question>>(emptyList())
    val questions: StateFlow<List<Question>> = _questions.asStateFlow()
    
    private val _shuffledQuestions = MutableStateFlow<List<ShuffledQuestion>>(emptyList())
    val shuffledQuestions: StateFlow<List<ShuffledQuestion>> = _shuffledQuestions.asStateFlow()
    
    private val _availablePowerUps = MutableStateFlow<List<PowerUp>>(emptyList())
    val availablePowerUps: StateFlow<List<PowerUp>> = _availablePowerUps.asStateFlow()
    
    private val _isDailyChallengeAvailable = MutableStateFlow(false)
    val isDailyChallengeAvailable: StateFlow<Boolean> = _isDailyChallengeAvailable.asStateFlow()
    
    private val _currentLevelProgress = MutableStateFlow<LevelProgress?>(null)
    val currentLevelProgress: StateFlow<LevelProgress?> = _currentLevelProgress.asStateFlow()

    fun loadQuestions(level: Int) {
        viewModelScope.launch {
            try {
                android.util.Log.d("QuizViewModel", "Loading questions for level $level...")
                
                // Use timeout to prevent hanging
                withTimeout(8000) { // 8 second timeout
                    getQuestionsForLevelUseCase(level)
                        .onStart { 
                            android.util.Log.d("QuizViewModel", "Starting to load questions for level $level")
                            _uiState.update { it.copy(isLoading = true) } 
                        }
                        .onEach { questions ->
                            android.util.Log.d("QuizViewModel", "Loaded ${questions.size} questions for level $level")
                            _questions.value = questions
                            val shuffledQuestions = questions.map { question ->
                                ShuffledQuestion.fromQuestion(question)
                            }
                            _shuffledQuestions.value = shuffledQuestions
                            _uiState.update { 
                                it.copy(
                                    isLoading = false,
                                    currentQuestion = 0,
                                    totalQuestions = questions.size
                                )
                            }
                            // Launch these operations in separate coroutines
                            launch { loadProgress() }
                            launch { loadPowerUps() }
                            launch { checkDailyChallenge() }
                        }
                        .catch { error ->
                            android.util.Log.e("QuizViewModel", "Error loading questions for level $level", error)
                            _uiState.update { 
                                it.copy(
                                    isLoading = false,
                                    error = error.message ?: "Unknown error occurred"
                                )
                            }
                        }
                        .collect()
                }
            } catch (e: Exception) {
                android.util.Log.e("QuizViewModel", "Unexpected error loading questions for level $level", e)
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        error = "Failed to load questions: ${e.message}"
                    )
                }
            }
        }
    }

    private suspend fun loadProgress() {
        data class ProgressSnapshot(
            val currentQuestion: Int,
            val score: Int,
            val streak: Int,
            val totalAnswered: Int
        )

        combine(
            dataStoreRepository.currentQuestion,
            dataStoreRepository.score,
            dataStoreRepository.streak,
            dataStoreRepository.totalQuestionsAnswered
        ) { currentQuestion, score, streak, totalAnswered ->
            ProgressSnapshot(currentQuestion, score, streak, totalAnswered)
        }.collect { snapshot ->
            _uiState.update { state ->
                state.copy(
                    currentQuestion = snapshot.currentQuestion,
                    score = snapshot.score,
                    streak = snapshot.streak,
                    totalQuestionsAnswered = snapshot.totalAnswered
                )
            }
        }
    }

    fun selectAnswer(answerIndex: Int) {
        val currentState = _uiState.value
        val currentShuffledQuestion = _shuffledQuestions.value.getOrNull(currentState.currentQuestion)
        
        if (currentShuffledQuestion != null && !currentState.showAnswerFeedback) {
            val isCorrect = answerIndex == currentShuffledQuestion.correctShuffledIndex
            val newStreak = if (isCorrect) currentState.streak + 1 else 0
            
            // Calculate streak bonus (+10 points for every 5 correct answers in a row)
            val streakBonus = if (isCorrect && newStreak > 0 && newStreak % 5 == 0) 10 else 0
            
            // Calculate base score (1 point for correct answer)
            val baseScore = if (isCorrect) 1 else 0
            val totalScore = baseScore + streakBonus
            
            val newScore = currentState.score + totalScore
            
            _uiState.update { 
                it.copy(
                    selectedAnswer = answerIndex,
                    isCorrectAnswer = isCorrect,
                    showAnswerFeedback = true,
                    score = newScore,
                    streak = newStreak,
                    totalQuestionsAnswered = it.totalQuestionsAnswered + 1,
                    totalCorrectAnswers = if (isCorrect) it.totalCorrectAnswers + 1 else it.totalCorrectAnswers,
                    streakBonus = streakBonus
                )
            }
            
            viewModelScope.launch {
                dataStoreRepository.saveScore(newScore)
                dataStoreRepository.saveStreak(newStreak)
                dataStoreRepository.saveTotalQuestionsAnswered(currentState.totalQuestionsAnswered + 1)
                
                // Check if power-up should be earned (every 10 correct answers)
                if (isCorrect) {
                    val totalCorrectAnswers = currentState.totalCorrectAnswers + 1
                    powerUpRepository.checkAndEarnPowerUp(totalCorrectAnswers)
                    loadPowerUps() // Refresh power-ups
                }
                
                                    // Update level progress for unlocking system
                    val currentLevel = currentShuffledQuestion.level
                    val correctAnswers = if (isCorrect) 1 else 0
                    gameProgressRepository.updateLevelProgress(currentLevel, correctAnswers, newScore)
            }
        }
    }

    fun nextQuestion() {
        val currentState = _uiState.value
        if (currentState.currentQuestion < currentState.totalQuestions - 1) {
            val nextQuestion = currentState.currentQuestion + 1
            _uiState.update { 
                it.copy(
                    currentQuestion = nextQuestion,
                    selectedAnswer = -1,
                    isCorrectAnswer = false,
                    showAnswerFeedback = false
                )
            }
            
            viewModelScope.launch {
                dataStoreRepository.saveCurrentQuestion(nextQuestion)
            }
        }
    }

    fun resetQuiz() {
        _uiState.update { 
            it.copy(
                currentQuestion = 0,
                score = 0,
                streak = 0,
                totalQuestionsAnswered = 0,
                selectedAnswer = -1,
                isCorrectAnswer = false,
                showAnswerFeedback = false
            )
        }
        
        viewModelScope.launch {
            dataStoreRepository.resetProgress()
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    private suspend fun loadPowerUps() {
        powerUpRepository.getAvailablePowerUps().collect { powerUps ->
            _availablePowerUps.value = powerUps
        }
    }

    private suspend fun checkDailyChallenge() {
        dailyChallengeRepository.isDailyChallengeAvailable().collect { isAvailable ->
            _isDailyChallengeAvailable.value = isAvailable
        }
    }

    fun usePowerUp(type: PowerUpType) {
        viewModelScope.launch {
            val success = powerUpRepository.usePowerUp(type)
            if (success) {
                loadPowerUps() // Refresh power-ups
            }
        }
    }

    fun skipQuestion() {
        val currentState = _uiState.value
        if (currentState.currentQuestion < currentState.totalQuestions - 1) {
            val nextQuestion = currentState.currentQuestion + 1
            _uiState.update { 
                it.copy(
                    currentQuestion = nextQuestion,
                    selectedAnswer = -1,
                    isCorrectAnswer = false,
                    showAnswerFeedback = false
                )
            }
            
            viewModelScope.launch {
                dataStoreRepository.saveCurrentQuestion(nextQuestion)
            }
        }
    }

    fun useFiftyFifty() {
        // This will be implemented in the UI to remove two wrong answers
        usePowerUp(PowerUpType.FIFTY_FIFTY)
    }
}

    data class QuizUiState(
        val isLoading: Boolean = false,
        val currentQuestion: Int = 0,
        val score: Int = 0,
        val streak: Int = 0,
        val totalQuestionsAnswered: Int = 0,
        val totalCorrectAnswers: Int = 0,
        val totalQuestions: Int = 0,
        val selectedAnswer: Int = -1,
        val isCorrectAnswer: Boolean = false,
        val showAnswerFeedback: Boolean = false,
        val showExplanation: Boolean = false,
        val streakBonus: Int = 0,
        val isDailyChallenge: Boolean = false,
        val error: String? = null
    )

data class ShuffledQuestion(
    val id: Int,
    val question: String,
    val shuffledOptions: List<String>,
    val correctShuffledIndex: Int,
    val level: Int,
    val category: String,
    val explanation: String?,
    val source: String?
) {
    companion object {
        fun fromQuestion(question: Question): ShuffledQuestion {
            val shuffledOptions = question.choices.toMutableList()
            val correctAnswer = shuffledOptions[question.correctAnswerIndex]
            
            // Shuffle the options
            shuffledOptions.shuffle()
            
            // Find the new index of the correct answer after shuffling
            val correctShuffledIndex = shuffledOptions.indexOf(correctAnswer)
            
            return ShuffledQuestion(
                id = question.id,
                question = question.question,
                shuffledOptions = shuffledOptions,
                correctShuffledIndex = correctShuffledIndex,
                level = question.level,
                category = "Bible",
                explanation = question.explanation,
                source = question.source
            )
        }
    }
}
