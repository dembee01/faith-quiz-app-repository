package com.example.faithquiz.ui.viewmodel

import com.example.faithquiz.data.model.Question

/**
 * A question with shuffled answer options.
 * The correctShuffledIndex points to the correct answer in the shuffled options list.
 */
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
