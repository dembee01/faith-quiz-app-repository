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
		assertTrue("correctAnswer must be 0..3", question.correctAnswer in 0..3)
		assertTrue("Explanation blank", question.explanation.isNotBlank())
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
		QuestionBank.getAvailableLevels().forEach { level ->
			val list = QuestionBank.getQuestionsForLevel(level)
			assertTrue("No questions for level $level", list.isNotEmpty())
			list.forEach { assertValid(it) }
			val normalized = list.map { normalize(it.question) }
			assertEquals("Duplicate questions found in level $level", normalized.size, normalized.toSet().size)
		}
	}

	@Test
	fun topics_expected_answers_remain_correct() {
		// Build a map for quick lookup by normalized question text
		val byText = TopicQuestionBank.getQuestionsForTopic(TopicQuestionBank.TopicType.GOSPELS)
			.associateBy { normalize(it.question) }

		fun assertAnswer(questionText: String, expectedIndex: Int) {
			val q = byText[normalize(questionText)]
			assertNotNull("Missing question: $questionText", q)
			assertEquals("Wrong correctAnswer for '$questionText'", expectedIndex, q!!.correctAnswer)
		}

		// Gospels — key corrected items
		assertAnswer("What did Jesus say about the kingdom of heaven?", 3)
		assertAnswer("Which Gospel emphasizes Jesus as the suffering servant?", 1)
		assertAnswer("Who was the first person to see the empty tomb?", 3)
		assertAnswer("Which Gospel is the shortest?", 1)
		assertAnswer("Who was the first person to call Jesus 'Lord'?", 3)
		assertAnswer("What did Jesus say about the kingdom of God?", 3)
		assertAnswer("What did Jesus say about the world?", 3)
		assertAnswer("Who was the first person to proclaim Jesus as the Son of God?", 1)
		assertAnswer("Which Gospel writer was a fisherman?", 3)
		assertAnswer("What did Jesus say about the Father?", 3)
		assertAnswer("What did Jesus say about the Holy Spirit?", 3)

		// Parables — key corrected items
		val parables = TopicQuestionBank.getQuestionsForTopic(TopicQuestionBank.TopicType.PARABLES)
			.associateBy { normalize(it.question) }
		fun assertParable(questionText: String, expectedIndex: Int) {
			val q = parables[normalize(questionText)]
			assertNotNull("Missing question: $questionText", q)
			assertEquals("Wrong correctAnswer for '$questionText'", expectedIndex, q!!.correctAnswer)
		}
		assertParable("What is the parable of the sower about?", 3)
		assertParable("What does the parable of the prodigal son teach?", 3)
		assertParable("What is the parable of the good Samaritan about?", 3)
		assertParable("What does the parable of the talents teach?", 3)
		assertParable("What is the parable of the lost sheep about?", 3)
		assertParable("What does the parable of the mustard seed teach?", 0)
		assertParable("What is the parable of the wedding feast about?", 3)
		assertParable("What does the parable of the rich fool teach?", 3)
		assertParable("What is the parable of the persistent widow about?", 3)
		assertParable("What does the parable of the two sons teach?", 3)

		// A couple Prophets spot checks
		val prophets = TopicQuestionBank.getQuestionsForTopic(TopicQuestionBank.TopicType.PROPHETS)
			.associateBy { normalize(it.question) }
		fun assertProphet(questionText: String, expectedIndex: Int) {
			val q = prophets[normalize(questionText)]
			assertNotNull("Missing question: $questionText", q)
			assertEquals("Wrong correctAnswer for '$questionText'", expectedIndex, q!!.correctAnswer)
		}
		assertProphet("What did Samuel anoint David to be?", 3)
		assertProphet("What did Isaiah prophesy about the suffering servant?", 3)
		assertProphet("What was Jeremiah's message about the 70-year exile?", 3)
	}

	@Test
	fun topics_have_exactly_50_items() {
		TopicQuestionBank.TopicType.values().forEach { topic ->
			val list = TopicQuestionBank.getQuestionsForTopic(topic)
			assertEquals("Topic $topic must have exactly 50 questions", 50, list.size)
		}
	}
}
