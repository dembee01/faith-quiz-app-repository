package com.example.faithquiz.data

import com.example.faithquiz.data.model.Question
import com.example.faithquiz.data.model.QuizQuestion

/**
 * Canonical metadata and conversion rules for every question shipped with the app.
 * Increment [VERSION] whenever bundled question wording, answers, or sources change.
 */
object QuestionContent {
    const val VERSION = 2
    const val DEFAULT_TRANSLATION = "Translation-independent"

    private val scriptureReference = Regex(
        pattern = """\b(?:[123]\s*)?(?:Genesis|Exodus|Leviticus|Numbers|Deuteronomy|Joshua|Judges|Ruth|Samuel|Kings|Chronicles|Ezra|Nehemiah|Esther|Job|Psalms?|Proverbs|Ecclesiastes|Song of (?:Solomon|Songs)|Isaiah|Jeremiah|Lamentations|Ezekiel|Daniel|Hosea|Joel|Amos|Obadiah|Jonah|Micah|Nahum|Habakkuk|Zephaniah|Haggai|Zechariah|Malachi|Matthew|Mark|Luke|John|Acts|Romans|Corinthians|Galatians|Ephesians|Philippians|Colossians|Thessalonians|Timothy|Titus|Philemon|Hebrews|James|Peter|Jude|Revelation)\s+\d+(?::\d+(?:[-–]\d+)?)?(?:\s*[-–,]\s*\d+(?::\d+(?:[-–]\d+)?)?)*""",
        option = RegexOption.IGNORE_CASE
    )

    fun enrich(question: QuizQuestion): QuizQuestion {
        val resolvedReference = question.verseReference
            ?.trim()
            ?.takeIf(String::isNotEmpty)
            ?: scriptureReference.find("${question.question} ${question.explanation}")?.value
            ?: "Bible-wide topic"

        val resolvedTranslation = question.translation.trim().ifEmpty { DEFAULT_TRANSLATION }
        return question.copy(
            verseReference = resolvedReference,
            translation = resolvedTranslation
        )
    }

    fun allLevelQuestions(): List<Pair<Int, QuizQuestion>> =
        QuestionBank.getAvailableLevels().flatMap { level ->
            QuestionBank.getQuestionsForLevel(level).map { level to enrich(it) }
        }

    fun asDatabaseQuestion(id: Int, level: Int, question: QuizQuestion): Question {
        val enriched = enrich(question)
        return Question(
            id = id,
            level = level,
            question = enriched.question,
            choices = enriched.options,
            correctAnswerIndex = enriched.correctAnswer,
            explanation = enriched.explanation,
            source = "${enriched.verseReference} • ${enriched.translation}",
            tags = listOf("content-v$VERSION")
        )
    }
}
