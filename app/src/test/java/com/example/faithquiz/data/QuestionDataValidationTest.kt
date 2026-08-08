package com.example.faithquiz.data

import com.example.faithquiz.data.model.QuizQuestion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class QuestionDataValidationTest {
	private fun normalize(text: String): String =
		text.lowercase().replace("\n", " ").replace(Regex("\\s+"), " ").trim()

	private fun assertValid(question: QuizQuestion) {
		assertTrue("Question text blank", question.question.isNotBlank())
		assertEquals("Exactly 4 options required", 4, question.options.size)
		assertTrue("Options must be non-blank", question.options.all { it.isNotBlank() })
		assertEquals(
			"Options must be distinct",
			question.options.size,
			question.options.map(::normalize).toSet().size
		)
		assertTrue("correctAnswer must be 0..3", question.correctAnswer in 0..3)
		assertTrue("Explanation blank", question.explanation.isNotBlank())
		assertTrue("Source reference blank", question.verseReference?.isNotBlank() == true)
		assertTrue("Translation metadata blank", question.translation.isNotBlank())
	}

	@Test
	fun canonical_database_projection_matches_the_displayed_question_bank() {
		val canonical = QuestionContent.allLevelQuestions()
		val expectedCount = QuestionBank.getAvailableLevels()
			.sumOf { QuestionBank.getQuestionsForLevel(it).size }

		assertEquals(expectedCount, canonical.size)
		canonical.forEachIndexed { index, (level, question) ->
			val stored = QuestionContent.asDatabaseQuestion(index + 1, level, question)
			assertEquals(question.question, stored.question)
			assertEquals(question.options, stored.choices)
			assertEquals(question.correctAnswer, stored.correctAnswerIndex)
			assertTrue(stored.source?.contains(question.translation) == true)
			assertTrue(stored.tags?.contains("content-v${QuestionContent.VERSION}") == true)
		}
	}

	@Test
	fun topics_are_valid_and_unique() {
		TopicQuestionBank.TopicType.values().forEach { topic ->
			val list = TopicQuestionBank.getQuestionsForTopic(topic)
			assertTrue("No questions for topic $topic", list.isNotEmpty())

			// Structure validity
			list.forEach { assertValid(it) }

			// No duplicates by normalized question text
			val normalized = list.map { normalize(it.question) }
			assertEquals("Duplicate questions found in $topic", normalized.size, normalized.toSet().size)
		}
	}

	@Test
	fun levels_are_valid_and_unique() {
		val allQuestions = mutableListOf<List<String>>()
		
		QuestionBank.getAvailableLevels().forEach { level ->
			val list = QuestionBank.getQuestionsForLevel(level)
			assertTrue("No questions for level $level", list.isNotEmpty())
			list.forEach { assertValid(it) }
			
			val normalized = list.map { normalize(it.question) }
			assertEquals("Duplicate questions found in level $level", normalized.size, normalized.toSet().size)
			
			// Verify level content is unique compared to other levels
			// Note: Level 1 has fallback logic, so we skip if it's identical to L1 and we are L30+ (unlikely now)
			// Actually, just check that we don't have exact duplicates of the FIRST question of the set across levels 
			// (since my previous bug was whole levels being identical)
			if (level > 1) {
				val firstQ = normalized.first()
				val isDuplicateLevel = allQuestions.any { it.first() == firstQ }
				assertFalse("Level $level seems to be a duplicate of a previous level", isDuplicateLevel)
			}
			allQuestions.add(normalized)
		}
	}

	@Test
	fun corrected_answers_remain_correct() {
		fun assertAnswer(
			questions: List<QuizQuestion>,
			questionText: String,
			expectedAnswer: String
		) {
			val question = questions.firstOrNull { normalize(it.question) == normalize(questionText) }
			assertNotNull("Missing question: $questionText", question)
			assertEquals(
				"Wrong answer for '$questionText'",
				expectedAnswer,
				question!!.options[question.correctAnswer]
			)
		}

		assertAnswer(
			QuestionBank.getQuestionsForLevel(10),
			"Who married Gomer as a living parable of Israel's unfaithfulness?",
			"Hosea"
		)
		assertAnswer(
			QuestionBank.getQuestionsForLevel(9),
			"According to Proverbs 9:10, what is the beginning of wisdom?",
			"The fear of the LORD"
		)
		assertAnswer(
			QuestionBank.getQuestionsForLevel(15),
			"Which city did David capture and call the City of David?",
			"Jerusalem"
		)
		assertAnswer(
			TopicQuestionBank.getQuestionsForTopic(TopicQuestionBank.TopicType.GOSPELS),
			"Which Gospel writer was traditionally identified as a physician and was not one of the Twelve?",
			"Luke"
		)
		assertAnswer(
			TopicQuestionBank.getQuestionsForTopic(TopicQuestionBank.TopicType.PROPHETS),
			"Before what did the statue of Dagon fall facedown in the Philistine temple?",
			"The Ark of the Covenant"
		)
	}

	@Test
	fun topics_have_exactly_50_items() {
		TopicQuestionBank.TopicType.values().forEach { topic ->
			val list = TopicQuestionBank.getQuestionsForTopic(topic)
			assertEquals("Topic $topic must have exactly 50 questions", 50, list.size)
		}
	}
}
