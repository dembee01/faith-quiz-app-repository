package com.example.faithquiz.data.local

import androidx.room.*
import com.example.faithquiz.data.model.LevelProgress
import kotlinx.coroutines.flow.Flow

@Dao
interface LevelProgressDao {
    @Query("SELECT * FROM level_progress WHERE level = :level")
    fun getLevelProgress(level: Int): Flow<LevelProgress?>

    @Query("SELECT * FROM level_progress ORDER BY level ASC")
    fun getAllLevelProgress(): Flow<List<LevelProgress>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLevelProgress(levelProgress: LevelProgress)

    @Update
    suspend fun updateLevelProgress(levelProgress: LevelProgress)

    @Query("UPDATE level_progress SET questionsAnswered = questionsAnswered + 1 WHERE level = :level")
    suspend fun incrementQuestionsAnswered(level: Int)

    @Query("UPDATE level_progress SET correctAnswers = correctAnswers + 1 WHERE level = :level")
    suspend fun incrementCorrectAnswers(level: Int)

    @Query("UPDATE level_progress SET isUnlocked = :isUnlocked WHERE level = :level")
    suspend fun updateLevelUnlockStatus(level: Int, isUnlocked: Boolean)

    @Query("UPDATE level_progress SET isCompleted = :isCompleted WHERE level = :level")
    suspend fun updateLevelCompletionStatus(level: Int, isCompleted: Boolean)

    @Query("UPDATE level_progress SET bestScore = :score WHERE level = :level AND bestScore < :score")
    suspend fun updateBestScore(level: Int, score: Int)

    @Query("SELECT COUNT(*) FROM level_progress WHERE isUnlocked = 1")
    suspend fun getUnlockedLevelsCount(): Int

    @Query("SELECT COUNT(*) FROM level_progress WHERE isCompleted = 1")
    suspend fun getCompletedLevelsCount(): Int
}
