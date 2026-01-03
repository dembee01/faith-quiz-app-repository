package com.example.faithquiz.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.faithquiz.data.model.Question
import com.example.faithquiz.data.model.GameProgress
import com.example.faithquiz.data.model.LevelProgress
import com.example.faithquiz.data.model.LeaderboardEntry

@Database(
    entities = [Question::class, GameProgress::class, LevelProgress::class, LeaderboardEntry::class],
    version = 3,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun questionDao(): QuestionDao
    abstract fun gameProgressDao(): GameProgressDao
    abstract fun levelProgressDao(): LevelProgressDao
    abstract fun leaderboardDao(): LeaderboardDao
}
