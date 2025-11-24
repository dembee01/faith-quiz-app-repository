package com.example.faithquiz.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "leaderboard")
data class LeaderboardEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val playerName: String,
    val score: Int,
    val level: Int,
    val date: Long = System.currentTimeMillis()
)
