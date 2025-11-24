package com.example.faithquiz.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "game_progress")
data class GameProgress(
    @PrimaryKey
    val id: Int = 1,
    val totalScore: Int = 0,
    val totalQuestionsAnswered: Int = 0,
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val powerUpsEarned: Int = 0,
    val powerUpsUsed: Int = 0,
    val levelsUnlocked: Int = 1, // Start with level 1 unlocked
    val lastDailyChallengeDate: String? = null, // ISO date string
    val dailyChallengesCompleted: Int = 0
)

@Entity(tableName = "level_progress")
data class LevelProgress(
    @PrimaryKey
    val level: Int,
    val questionsAnswered: Int = 0,
    val correctAnswers: Int = 0,
    val isUnlocked: Boolean = false,
    val isCompleted: Boolean = false,
    val bestScore: Int = 0
)

data class DailyChallenge(
    val question: Question,
    val isAvailable: Boolean = true,
    val isCompleted: Boolean = false,
    val date: String // ISO date string
)
