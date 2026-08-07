package com.example.faithquiz.di

import android.content.Context
import com.example.faithquiz.data.local.AppDatabase
import com.example.faithquiz.data.local.QuestionDao
import com.example.faithquiz.data.local.GameProgressDao
import com.example.faithquiz.data.local.LevelProgressDao
import com.example.faithquiz.data.local.LeaderboardDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return androidx.room.Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java,
            "faith_quiz_database"
        )
        .fallbackToDestructiveMigration()
        .build()
    }

    @Provides
    @Singleton
    fun provideQuestionDao(database: AppDatabase): QuestionDao {
        return database.questionDao()
    }

    @Provides
    @Singleton
    fun provideGameProgressDao(database: AppDatabase): GameProgressDao {
        return database.gameProgressDao()
    }

    @Provides
    @Singleton
    fun provideLevelProgressDao(database: AppDatabase): LevelProgressDao {
        return database.levelProgressDao()
    }

    @Provides
    @Singleton
    fun provideLeaderboardDao(database: AppDatabase): LeaderboardDao {
        return database.leaderboardDao()
    }

}
