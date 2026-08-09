package com.example.faithquiz.data.local

import androidx.room.*
import com.example.faithquiz.data.model.LeaderboardEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface LeaderboardDao {
    @Query("SELECT * FROM leaderboard ORDER BY score DESC LIMIT 10")
    fun getTopScores(): Flow<List<LeaderboardEntry>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLeaderboardEntry(entry: LeaderboardEntry)

    @Query("DELETE FROM leaderboard")
    suspend fun clearLeaderboard()
}
