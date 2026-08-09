package com.example.faithquiz.data.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class DailyChallengeSelectorTest {
    @Test
    fun selection_is_stable_for_the_whole_day() {
        val date = LocalDate.of(2026, 8, 8)
        assertEquals(
            DailyChallengeSelector.questionForDate(date),
            DailyChallengeSelector.questionForDate(date)
        )
    }

    @Test
    fun consecutive_days_rotate_questions_with_complete_source_metadata() {
        val first = DailyChallengeSelector.questionForDate(LocalDate.of(2026, 8, 8))
        val second = DailyChallengeSelector.questionForDate(LocalDate.of(2026, 8, 9))

        assertNotEquals(first.question, second.question)
        assertTrue(first.verseReference?.isNotBlank() == true)
        assertTrue(first.translation.isNotBlank())
    }
}
