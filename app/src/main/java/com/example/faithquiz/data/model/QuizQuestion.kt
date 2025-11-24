package com.example.faithquiz.data.model

data class QuizQuestion(
	val question: String,
	val options: List<String>,
	val correctAnswer: Int,
	val explanation: String,
	val verseReference: String? = null,
	val verseText: String? = null,
	val commentary: String? = null,
	val crossRefs: List<String>? = null,
	val learnMoreUrl: String? = null
)


