package com.example.faithquiz.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "questions")
data class Question(
    @PrimaryKey val id: Int,
    val level: Int,
    val question: String,
    val choices: List<String>,
    val correctAnswerIndex: Int,
    val explanation: String? = null,
    val source: String? = null,
    val tags: List<String>? = null
)
