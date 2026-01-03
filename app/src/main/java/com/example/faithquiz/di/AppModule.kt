package com.example.faithquiz.di

import android.content.Context
import com.example.faithquiz.data.local.AppDatabase
import com.example.faithquiz.data.local.QuestionDao
import com.example.faithquiz.data.local.GameProgressDao
import com.example.faithquiz.data.local.LevelProgressDao
import com.example.faithquiz.data.local.LeaderboardDao
import com.example.faithquiz.data.repository.DataStoreRepository
import com.example.faithquiz.data.repository.QuestionRepository
import com.example.faithquiz.data.repository.PowerUpRepository
import com.example.faithquiz.data.repository.DailyChallengeRepository
import com.example.faithquiz.data.repository.GameProgressRepository
import com.example.faithquiz.data.repository.LeaderboardRepository
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

    @Provides
    @Singleton
    fun provideQuestionRepository(
        questionDao: QuestionDao,
        @ApplicationContext context: Context
    ): QuestionRepository {
        return QuestionRepository(questionDao, context)
    }

    @Provides
    @Singleton
    fun provideLeaderboardRepository(
        leaderboardDao: LeaderboardDao,
        @ApplicationContext context: Context
    ): LeaderboardRepository {
        return LeaderboardRepository(leaderboardDao, context)
    }

    @Provides
    @Singleton
    fun providePowerUpRepository(
        gameProgressDao: GameProgressDao,
        @ApplicationContext context: Context
    ): PowerUpRepository {
        return PowerUpRepository(gameProgressDao, context)
    }

    @Provides
    @Singleton
    fun provideDailyChallengeRepository(
        gameProgressDao: GameProgressDao,
        questionDao: QuestionDao,
        @ApplicationContext context: Context
    ): DailyChallengeRepository {
        return DailyChallengeRepository(gameProgressDao, questionDao, context)
    }

    @Provides
    @Singleton
    fun provideGameProgressRepository(
        gameProgressDao: GameProgressDao,
        levelProgressDao: LevelProgressDao,
        @ApplicationContext context: Context
    ): GameProgressRepository {
        return GameProgressRepository(gameProgressDao, levelProgressDao, context)
    }

    @Provides
    @Singleton
    fun provideDataStoreRepository(@ApplicationContext context: Context): DataStoreRepository {
        return DataStoreRepository(context)
    }
}
