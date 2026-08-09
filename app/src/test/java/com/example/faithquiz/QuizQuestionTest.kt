package com.example.faithquiz

import com.example.faithquiz.data.model.Question
import com.example.faithquiz.ui.viewmodel.ShuffledQuestion
import org.junit.Assert.assertEquals
import org.junit.Test

class QuizQuestionTest {

    @Test
    fun shuffled_question_keeps_the_correct_answer() {
        val question = Question(
            id = 1,
            level = 1,
            question = "Which prophet was swallowed by a great fish?",
            choices = listOf("Jonah", "Nahum", "Micah", "Joel"),
            correctAnswerIndex = 0,
            explanation = "Jonah 1:17",
            source = "Jonah 1:17"
        )

        repeat(20) {
            val shuffled = ShuffledQuestion.fromQuestion(question)

            assertEquals(4, shuffled.shuffledOptions.size)
            assertEquals(question.choices.toSet(), shuffled.shuffledOptions.toSet())
            assertEquals("Jonah", shuffled.shuffledOptions[shuffled.correctShuffledIndex])
        }
    }

    @Test
    fun question_with_invalid_correct_index_is_rejected() {
        val question = Question(
            id = 1,
            level = 1,
            question = "Question",
            choices = listOf("A", "B", "C", "D"),
            correctAnswerIndex = 4
        )

        try {
            ShuffledQuestion.fromQuestion(question)
        } catch (error: IllegalArgumentException) {
            return
        }

        throw AssertionError("An invalid correct answer index must be rejected.")
    }
}
