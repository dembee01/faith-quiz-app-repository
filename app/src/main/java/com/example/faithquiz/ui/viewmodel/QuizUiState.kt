package com.example.faithquiz.ui.viewmodel

/**
 * UI state for the Quiz screen.
 * Contains all necessary state for rendering the quiz interface.
 */
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
