package com.example.faithquiz.data.repository

import android.content.Context
import com.example.faithquiz.data.local.LeaderboardDao
import com.example.faithquiz.data.model.LeaderboardEntry
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LeaderboardRepository @Inject constructor(
    private val leaderboardDao: LeaderboardDao,
    private val context: Context
) {
    fun getTopScores(): Flow<List<LeaderboardEntry>> {
        return leaderboardDao.getTopScores()
    }

    suspend fun addScore(playerName: String, score: Int, level: Int) {
        val entry = LeaderboardEntry(
            playerName = playerName,
            score = score,
            level = level
        )
        leaderboardDao.insertLeaderboardEntry(entry)
    }

    suspend fun clearLeaderboard() {
        leaderboardDao.clearLeaderboard()
    }
}
