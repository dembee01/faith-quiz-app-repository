package com.example.faithquiz.data.repository

import android.content.Context
import androidx.room.withTransaction
import com.example.faithquiz.data.QuestionBank
import com.example.faithquiz.data.QuestionContent
import com.example.faithquiz.data.local.AppDatabase
import com.example.faithquiz.data.local.QuestionDao
import com.example.faithquiz.data.model.Question
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Database projection of [QuestionBank], the app's single authoritative question source.
 */
@Singleton
class QuestionRepository @Inject constructor(
    private val database: AppDatabase,
    private val questionDao: QuestionDao,
    @ApplicationContext context: Context
) {
    private val contentPreferences = context.getSharedPreferences(
        "question_content",
        Context.MODE_PRIVATE
    )

    fun getAllQuestions(): Flow<List<Question>> = questionDao.getAllQuestions()

    fun getQuestionsByLevel(level: Int): Flow<List<Question>> =
        questionDao.getQuestionsByLevel(level.coerceIn(1, 30))

    fun getAllLevels(): Flow<List<Int>> = questionDao.getAllLevels()

    fun getAvailableLevels(): List<Int> = QuestionBank.getAvailableLevels()

    suspend fun getQuestionCount(): Int = questionDao.getQuestionCount()

    suspend fun insertQuestions(questions: List<Question>) = questionDao.insertQuestions(questions)

    suspend fun deleteAllQuestions() = questionDao.deleteAllQuestions()

    suspend fun seedDatabase() = withContext(Dispatchers.IO) {
        val storedVersion = contentPreferences.getInt(CONTENT_VERSION_KEY, 0)
        val canonicalQuestions = QuestionContent.allLevelQuestions().mapIndexed { index, (level, quizQuestion) ->
            QuestionContent.asDatabaseQuestion(index + 1, level, quizQuestion)
        }

        val requiresRefresh = storedVersion != QuestionContent.VERSION ||
            questionDao.getQuestionCount() != canonicalQuestions.size

        if (requiresRefresh) {
            database.withTransaction {
                questionDao.deleteAllQuestions()
                canonicalQuestions.chunked(100).forEach { batch ->
                    questionDao.insertQuestions(batch)
                }
            }
            contentPreferences.edit()
                .putInt(CONTENT_VERSION_KEY, QuestionContent.VERSION)
                .apply()
        }
    }

    private companion object {
        const val CONTENT_VERSION_KEY = "content_version"
    }
}
