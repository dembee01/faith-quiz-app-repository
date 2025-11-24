package com.example.faithquiz.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "faith_quiz_preferences")

@Singleton
class DataStoreRepository @Inject constructor(
    private val context: Context
) {
    private object PreferencesKeys {
        val CURRENT_QUESTION = intPreferencesKey("current_question")
        val SCORE = intPreferencesKey("score")
        val STREAK = intPreferencesKey("streak")
        val TOTAL_QUESTIONS_ANSWERED = intPreferencesKey("total_questions_answered")
        val HIGH_SCORE = intPreferencesKey("high_score")
        val LAST_LEVEL = intPreferencesKey("last_level")
    }

    val currentQuestion: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.CURRENT_QUESTION] ?: 0
    }

    val score: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.SCORE] ?: 0
    }

    val streak: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.STREAK] ?: 0
    }

    val totalQuestionsAnswered: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.TOTAL_QUESTIONS_ANSWERED] ?: 0
    }

    val highScore: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.HIGH_SCORE] ?: 0
    }

    val lastLevel: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.LAST_LEVEL] ?: 1
    }

    suspend fun saveCurrentQuestion(currentQuestion: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.CURRENT_QUESTION] = currentQuestion
        }
    }

    suspend fun saveScore(score: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SCORE] = score
        }
    }

    suspend fun saveStreak(streak: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.STREAK] = streak
        }
    }

    suspend fun saveTotalQuestionsAnswered(total: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.TOTAL_QUESTIONS_ANSWERED] = total
        }
    }

    suspend fun saveHighScore(highScore: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.HIGH_SCORE] = highScore
        }
    }

    suspend fun saveLastLevel(level: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.LAST_LEVEL] = level
        }
    }

    suspend fun resetProgress() {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.CURRENT_QUESTION] = 0
            preferences[PreferencesKeys.SCORE] = 0
            preferences[PreferencesKeys.STREAK] = 0
            preferences[PreferencesKeys.TOTAL_QUESTIONS_ANSWERED] = 0
        }
    }
}
