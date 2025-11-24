package com.example.faithquiz.data.repository

import android.content.Context
import com.example.faithquiz.data.local.GameProgressDao
import com.example.faithquiz.data.local.QuestionDao
import com.example.faithquiz.data.model.DailyChallenge
import com.example.faithquiz.data.model.Question
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DailyChallengeRepository @Inject constructor(
    private val gameProgressDao: GameProgressDao,
    private val questionDao: QuestionDao,
    private val context: Context
) {
    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    fun getDailyChallenge(): Flow<DailyChallenge?> {
        return gameProgressDao.getGameProgress().map { gameProgress ->
            val progress = gameProgress ?: return@map null
            val today = dateFormatter.format(Date())
            
            // Check if daily challenge is available
            if (progress.lastDailyChallengeDate != today) {
                // Generate a new daily challenge
                generateDailyChallenge(today)
            } else {
                null // Already completed today
            }
        }
    }

    private suspend fun generateDailyChallenge(date: String): DailyChallenge? {
        // Get a random question from level 1-5 for daily challenge
        val questions = questionDao.getQuestionsByLevelRange(1, 5)
        if (questions.isEmpty()) return null
        
        val randomQuestion = questions.random()
        return DailyChallenge(
            question = randomQuestion,
            isAvailable = true,
            isCompleted = false,
            date = date
        )
    }

    suspend fun completeDailyChallenge() {
        val today = dateFormatter.format(Date())
        val currentProgress = gameProgressDao.getGameProgress().first()
        val progress = currentProgress ?: return
        
        gameProgressDao.updateLastDailyChallengeDate(today)
        gameProgressDao.updateDailyChallengesCompleted(progress.dailyChallengesCompleted + 1)
    }

    fun isDailyChallengeAvailable(): Flow<Boolean> {
        return gameProgressDao.getGameProgress().map { gameProgress ->
            val progress = gameProgress ?: return@map false
            val today = dateFormatter.format(Date())
            progress.lastDailyChallengeDate != today
        }
    }

    fun getDailyChallengeProgress(): Flow<Int> {
        return gameProgressDao.getGameProgress().map { gameProgress ->
            gameProgress?.dailyChallengesCompleted ?: 0
        }
    }
}
