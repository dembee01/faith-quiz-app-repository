package com.example.faithquiz.data.local

import androidx.room.*
import com.example.faithquiz.data.model.GameProgress
import kotlinx.coroutines.flow.Flow

@Dao
interface GameProgressDao {
    @Query("SELECT * FROM game_progress WHERE id = 1")
    fun getGameProgress(): Flow<GameProgress?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGameProgress(gameProgress: GameProgress)

    @Update
    suspend fun updateGameProgress(gameProgress: GameProgress)

    @Query("UPDATE game_progress SET totalScore = :score WHERE id = 1")
    suspend fun updateScore(score: Int)

    @Query("UPDATE game_progress SET currentStreak = :streak WHERE id = 1")
    suspend fun updateStreak(streak: Int)

    @Query("UPDATE game_progress SET longestStreak = :longestStreak WHERE id = 1")
    suspend fun updateLongestStreak(longestStreak: Int)

    @Query("UPDATE game_progress SET powerUpsEarned = :powerUpsEarned WHERE id = 1")
    suspend fun updatePowerUpsEarned(powerUpsEarned: Int)

    @Query("UPDATE game_progress SET powerUpsUsed = :powerUpsUsed WHERE id = 1")
    suspend fun updatePowerUpsUsed(powerUpsUsed: Int)

    @Query("UPDATE game_progress SET levelsUnlocked = :levelsUnlocked WHERE id = 1")
    suspend fun updateLevelsUnlocked(levelsUnlocked: Int)

    @Query("UPDATE game_progress SET lastDailyChallengeDate = :date WHERE id = 1")
    suspend fun updateLastDailyChallengeDate(date: String)

    @Query("UPDATE game_progress SET dailyChallengesCompleted = :completed WHERE id = 1")
    suspend fun updateDailyChallengesCompleted(completed: Int)

    @Query("UPDATE game_progress SET totalQuestionsAnswered = totalQuestionsAnswered + 1 WHERE id = 1")
    suspend fun incrementQuestionsAnswered()
}
