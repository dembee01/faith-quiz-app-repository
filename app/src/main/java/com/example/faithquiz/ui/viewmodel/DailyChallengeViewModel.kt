package com.example.faithquiz.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.faithquiz.data.model.DailyChallenge
import com.example.faithquiz.data.repository.DailyChallengeRepository
import com.example.faithquiz.data.repository.DataStoreRepository
import com.example.faithquiz.data.store.ProgressDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DailyChallengeViewModel @Inject constructor(
    private val dailyChallengeRepository: DailyChallengeRepository,
    private val dataStoreRepository: DataStoreRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {
    private val _uiState = MutableStateFlow(DailyChallengeUiState())
    val uiState: StateFlow<DailyChallengeUiState> = _uiState.asStateFlow()

    init {
        loadDailyChallenge()
    }

    private fun loadDailyChallenge() {
        viewModelScope.launch {
            dailyChallengeRepository.getDailyChallenge()
                .catch { error ->
                    _uiState.update {
                        it.copy(isLoading = false, error = error.message ?: "Unable to load today's challenge")
                    }
                }
                .collect { challenge ->
                    _uiState.update {
                        it.copy(isLoading = false, dailyChallenge = challenge, isCompleted = challenge.isCompleted)
                    }
                }
        }
    }

    fun selectAnswer(answerIndex: Int) {
        val state = _uiState.value
        val challenge = state.dailyChallenge ?: return
        if (!challenge.isAvailable || state.showAnswerFeedback || answerIndex !in challenge.question.options.indices) return

        val isCorrect = answerIndex == challenge.question.correctAnswer
        val points = if (isCorrect) 2 else 0
        _uiState.update {
            it.copy(
                selectedAnswer = answerIndex,
                isCorrectAnswer = isCorrect,
                showAnswerFeedback = true,
                pointsEarned = points
            )
        }

        viewModelScope.launch {
            val key = ProgressDataStore.createReviewKey(0, challenge.question.question)
            ProgressDataStore.recordReviewResult(context, key, isCorrect)
            if (!isCorrect) {
                ProgressDataStore.addMistakeDetailed(
                    context = context,
                    level = 0,
                    question = challenge.question.question,
                    userAnswer = challenge.question.options[answerIndex],
                    correctAnswer = challenge.question.options[challenge.question.correctAnswer],
                    explanation = challenge.question.explanation
                )
            }
            if (isCorrect) {
                val currentScore = dataStoreRepository.score.first()
                dataStoreRepository.saveScore(currentScore + points)
            }
        }
    }

    fun completeDailyChallenge() {
        if (!_uiState.value.showAnswerFeedback || _uiState.value.isCompleted) return
        viewModelScope.launch {
            dailyChallengeRepository.completeDailyChallenge()
            ProgressDataStore.recordDevotionCompletion(context)
            _uiState.update { it.copy(isCompleted = true) }
        }
    }

    fun clearError() = _uiState.update { it.copy(error = null) }
}

data class DailyChallengeUiState(
    val isLoading: Boolean = true,
    val dailyChallenge: DailyChallenge? = null,
    val selectedAnswer: Int = -1,
    val isCorrectAnswer: Boolean = false,
    val showAnswerFeedback: Boolean = false,
    val pointsEarned: Int = 0,
    val isCompleted: Boolean = false,
    val error: String? = null
)
