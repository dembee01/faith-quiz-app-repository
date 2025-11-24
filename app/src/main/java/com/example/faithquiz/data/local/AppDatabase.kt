package com.example.faithquiz.data.local

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import android.content.Context
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

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "faith_quiz_database"
                )
                .fallbackToDestructiveMigration() // This will recreate the database if migration fails
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
