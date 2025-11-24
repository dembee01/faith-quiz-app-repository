package com.example.faithquiz.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.faithquiz.data.model.DailyChallenge
import com.example.faithquiz.data.repository.DailyChallengeRepository
import com.example.faithquiz.data.repository.DataStoreRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DailyChallengeViewModel @Inject constructor(
    private val dailyChallengeRepository: DailyChallengeRepository,
    private val dataStoreRepository: DataStoreRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DailyChallengeUiState())
    val uiState: StateFlow<DailyChallengeUiState> = _uiState.asStateFlow()

    fun loadDailyChallenge() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            dailyChallengeRepository.getDailyChallenge()
                .onEach { dailyChallenge ->
                    if (dailyChallenge != null) {
                        _uiState.update { 
                            it.copy(
                                isLoading = false,
                                dailyChallenge = dailyChallenge
                            )
                        }
                    } else {
                        _uiState.update { 
                            it.copy(
                                isLoading = false,
                                dailyChallenge = null
                            )
                        }
                    }
                }
                .catch { error ->
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            error = error.message ?: "Unknown error occurred"
                        )
                    }
                }
                .collect()
        }
    }

    fun selectAnswer(answerIndex: Int) {
        val currentState = _uiState.value
        val dailyChallenge = currentState.dailyChallenge
        
        if (dailyChallenge != null && !currentState.showAnswerFeedback) {
            val shuffledQuestion = ShuffledQuestion.fromQuestion(dailyChallenge.question)
            val isCorrect = answerIndex == shuffledQuestion.correctShuffledIndex
            
            // Daily challenge is worth double points (2 points for correct answer)
            val pointsEarned = if (isCorrect) 2 else 0
            
            _uiState.update { 
                it.copy(
                    selectedAnswer = answerIndex,
                    isCorrectAnswer = isCorrect,
                    showAnswerFeedback = true,
                    pointsEarned = pointsEarned
                )
            }
            
            // Update total score with double points
            if (isCorrect) {
                viewModelScope.launch {
                    val currentScore = dataStoreRepository.score.first()
                    dataStoreRepository.saveScore(currentScore + pointsEarned)
                }
            }
        }
    }

    fun completeDailyChallenge() {
        viewModelScope.launch {
            dailyChallengeRepository.completeDailyChallenge()
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}

data class DailyChallengeUiState(
    val isLoading: Boolean = false,
    val dailyChallenge: DailyChallenge? = null,
    val selectedAnswer: Int = -1,
    val isCorrectAnswer: Boolean = false,
    val showAnswerFeedback: Boolean = false,
    val pointsEarned: Int = 0,
    val error: String? = null
)
