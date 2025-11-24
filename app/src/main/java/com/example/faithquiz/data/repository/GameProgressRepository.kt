package com.example.faithquiz.data.repository

import android.content.Context
import com.example.faithquiz.data.local.GameProgressDao
import com.example.faithquiz.data.local.LevelProgressDao
import com.example.faithquiz.data.model.GameProgress
import com.example.faithquiz.data.model.LevelProgress
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GameProgressRepository @Inject constructor(
    private val gameProgressDao: GameProgressDao,
    private val levelProgressDao: LevelProgressDao,
    private val context: Context
) {
    fun getGameProgress(): Flow<GameProgress?> {
        return gameProgressDao.getGameProgress()
    }

    fun getAllLevelProgress(): Flow<List<LevelProgress>> {
        return levelProgressDao.getAllLevelProgress()
    }

    suspend fun initializeGameProgress() {
        try {
            android.util.Log.d("GameProgressRepository", "Initializing game progress...")
            val currentProgress = gameProgressDao.getGameProgress().first()
            if (currentProgress == null) {
                android.util.Log.d("GameProgressRepository", "No game progress found, creating default...")
                // Initialize with default values
                val defaultProgress = GameProgress(
                    id = 1,
                    totalScore = 0,
                    totalQuestionsAnswered = 0,
                    currentStreak = 0,
                    longestStreak = 0,
                    powerUpsEarned = 0,
                    powerUpsUsed = 0,
                    levelsUnlocked = 1, // Start with level 1 unlocked
                    lastDailyChallengeDate = null,
                    dailyChallengesCompleted = 0
                )
                gameProgressDao.insertGameProgress(defaultProgress)
                android.util.Log.d("GameProgressRepository", "Default game progress created")
            } else {
                android.util.Log.d("GameProgressRepository", "Game progress already exists")
            }
        } catch (e: Exception) {
            android.util.Log.e("GameProgressRepository", "Failed to initialize game progress", e)
            // Don't throw the exception, just log it and continue
            e.printStackTrace()
        }
    }

    suspend fun initializeLevelProgress() {
        try {
            android.util.Log.d("GameProgressRepository", "Initializing level progress...")
            val currentLevels = levelProgressDao.getAllLevelProgress().first()
            if (currentLevels.isEmpty()) {
                android.util.Log.d("GameProgressRepository", "No level progress found, creating default...")
                // Initialize all 30 levels
                val levelProgressList = (1..30).map { level ->
                    LevelProgress(
                        level = level,
                        questionsAnswered = 0,
                        correctAnswers = 0,
                        isUnlocked = level == 1, // Only level 1 is unlocked initially
                        isCompleted = false,
                        bestScore = 0
                    )
                }
                
                // Insert in batches to prevent memory issues
                val batchSize = 10
                levelProgressList.chunked(batchSize).forEachIndexed { index, batch ->
                    android.util.Log.d("GameProgressRepository", "Inserting level progress batch ${index + 1}/${levelProgressList.size / batchSize + 1}")
                    batch.forEach { levelProgress ->
                        levelProgressDao.insertLevelProgress(levelProgress)
                    }
                }
                android.util.Log.d("GameProgressRepository", "Level progress created successfully")
            } else {
                android.util.Log.d("GameProgressRepository", "Level progress already exists (${currentLevels.size} entries)")
            }
        } catch (e: Exception) {
            android.util.Log.e("GameProgressRepository", "Failed to initialize level progress", e)
            // Don't throw the exception, just log it and continue
            e.printStackTrace()
        }
    }

    suspend fun updateLevelProgress(level: Int, correctAnswers: Int, score: Int) {
        // Increment questions answered (always add 1 for each question attempted)
        levelProgressDao.incrementQuestionsAnswered(level)
        
        // Increment correct answers if the answer was correct
        if (correctAnswers > 0) {
            levelProgressDao.incrementCorrectAnswers(level)
        }
        
        // Update best score if current score is higher
        levelProgressDao.updateBestScore(level, score)
        
        // Get current level progress to check total correct answers
        val currentLevelProgress = levelProgressDao.getLevelProgress(level).first()
        val totalCorrectAnswers = currentLevelProgress?.correctAnswers ?: 0
        
        // Check if level should be completed (10+ correct answers total)
        if (totalCorrectAnswers >= 10) {
            levelProgressDao.updateLevelCompletionStatus(level, true)
            
            // Unlock next level if not already unlocked
            val nextLevel = level + 1
            if (nextLevel <= 30) {
                levelProgressDao.updateLevelUnlockStatus(nextLevel, true)
            }
        }
    }

    suspend fun updateGameProgress(
        totalScore: Int,
        currentStreak: Int,
        longestStreak: Int,
        powerUpsEarned: Int,
        powerUpsUsed: Int
    ) {
        gameProgressDao.updateScore(totalScore)
        gameProgressDao.updateStreak(currentStreak)
        gameProgressDao.updateLongestStreak(longestStreak)
        gameProgressDao.updatePowerUpsEarned(powerUpsEarned)
        gameProgressDao.updatePowerUpsUsed(powerUpsUsed)
    }

    suspend fun resetAllProgress() {
        // Reset game progress
        val defaultProgress = GameProgress(
            id = 1,
            totalScore = 0,
            totalQuestionsAnswered = 0,
            currentStreak = 0,
            longestStreak = 0,
            powerUpsEarned = 0,
            powerUpsUsed = 0,
            levelsUnlocked = 1,
            lastDailyChallengeDate = null,
            dailyChallengesCompleted = 0
        )
        gameProgressDao.insertGameProgress(defaultProgress)
        
        // Reset level progress
        val levelProgressList = (1..30).map { level ->
            LevelProgress(
                level = level,
                questionsAnswered = 0,
                correctAnswers = 0,
                isUnlocked = level == 1,
                isCompleted = false,
                bestScore = 0
            )
        }
        levelProgressList.forEach { levelProgress ->
            levelProgressDao.insertLevelProgress(levelProgress)
        }
    }
}
