package com.example.faithquiz.data.local

import androidx.room.*
import com.example.faithquiz.data.model.Question
import kotlinx.coroutines.flow.Flow

@Dao
interface QuestionDao {
    @Query("SELECT * FROM questions ORDER BY level ASC, id ASC")
    fun getAllQuestions(): Flow<List<Question>>

    @Query("SELECT * FROM questions WHERE level = :level ORDER BY id ASC")
    fun getQuestionsByLevel(level: Int): Flow<List<Question>>

    @Query("SELECT COUNT(*) FROM questions")
    suspend fun getQuestionCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuestions(questions: List<Question>)

    @Query("DELETE FROM questions")
    suspend fun deleteAllQuestions()

    @Query("SELECT DISTINCT level FROM questions ORDER BY level ASC")
    fun getAllLevels(): Flow<List<Int>>

    @Query("SELECT * FROM questions WHERE level BETWEEN :startLevel AND :endLevel ORDER BY RANDOM() LIMIT 1")
    suspend fun getQuestionsByLevelRange(startLevel: Int, endLevel: Int): List<Question>
}
