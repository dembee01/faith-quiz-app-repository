package com.example.faithquiz

import com.example.faithquiz.data.model.Question
import com.example.faithquiz.data.repository.DataStoreRepository
import com.example.faithquiz.data.repository.PowerUpRepository
import com.example.faithquiz.data.repository.DailyChallengeRepository
import com.example.faithquiz.data.repository.GameProgressRepository
import com.example.faithquiz.domain.usecase.GetQuestionsForLevelUseCase
import com.example.faithquiz.ui.viewmodel.QuizViewModel
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class QuizViewModelTest {
	private lateinit var viewModel: QuizViewModel
	private lateinit var getQuestionsForLevelUseCase: GetQuestionsForLevelUseCase
	private lateinit var dataStoreRepository: DataStoreRepository
	private lateinit var powerUpRepository: PowerUpRepository
	private lateinit var dailyChallengeRepository: DailyChallengeRepository
	private lateinit var gameProgressRepository: GameProgressRepository
	private val testDispatcher = StandardTestDispatcher()

	@Before
	fun setup() {
		Dispatchers.setMain(testDispatcher)
		getQuestionsForLevelUseCase = mockk()
		dataStoreRepository = mockk()
		powerUpRepository = mockk()
		dailyChallengeRepository = mockk()
		gameProgressRepository = mockk()
		
		// Mock Android Log calls
		mockkStatic(android.util.Log::class)
		every { android.util.Log.d(any(), any<String>()) } returns 0
		every { android.util.Log.e(any(), any<String>(), any()) } returns 0
		
		viewModel = QuizViewModel(
			getQuestionsForLevelUseCase,
			dataStoreRepository,
			powerUpRepository,
			dailyChallengeRepository,
			gameProgressRepository
		)

		// Defaults for flows used by loadProgress
		every { dataStoreRepository.currentQuestion } returns flowOf(0)
		every { dataStoreRepository.score } returns flowOf(0)
		every { dataStoreRepository.streak } returns flowOf(0)
		every { dataStoreRepository.totalQuestionsAnswered } returns flowOf(0)
	}

	@After
	fun tearDown() {
		Dispatchers.resetMain()
	}

	@Test
	fun `loadQuestions should update state with questions`() = runTest {
		// Given
		val testQuestions = listOf(
			Question(
				id = 1,
				level = 1,
				question = "Test question?",
				choices = listOf("A", "B", "C", "D"),
				correctAnswerIndex = 0,
				explanation = null,
				source = null,
				tags = null
			)
		)
		coEvery { getQuestionsForLevelUseCase(1) } returns flowOf(testQuestions)
		every { powerUpRepository.getAvailablePowerUps() } returns flowOf(emptyList())
		every { dailyChallengeRepository.isDailyChallengeAvailable() } returns flowOf(false)

		// When
		viewModel.loadQuestions(1)
		testDispatcher.scheduler.advanceUntilIdle()

		// Then
		val questions = viewModel.questions.value
		assertEquals(testQuestions, questions)
		val uiState = viewModel.uiState.value
		assertFalse(uiState.isLoading)
		assertEquals(1, uiState.totalQuestions)
	}

	@Test
	fun `selectAnswer should update score and streak correctly`() = runTest {
		// Given
		val testQuestions = listOf(
			Question(
				id = 1,
				level = 1,
				question = "Test question?",
				choices = listOf("A", "B", "C", "D"),
				correctAnswerIndex = 0
			)
		)
		coEvery { getQuestionsForLevelUseCase(1) } returns flowOf(testQuestions)
		every { powerUpRepository.getAvailablePowerUps() } returns flowOf(emptyList())
		every { dailyChallengeRepository.isDailyChallengeAvailable() } returns flowOf(false)
		coEvery { powerUpRepository.checkAndEarnPowerUp(any()) } returns Unit
		coEvery { dataStoreRepository.saveScore(any()) } returns Unit
		coEvery { dataStoreRepository.saveStreak(any()) } returns Unit
		coEvery { dataStoreRepository.saveTotalQuestionsAnswered(any()) } returns Unit
		coEvery { gameProgressRepository.updateLevelProgress(any(), any(), any()) } returns Unit

		// When
		viewModel.loadQuestions(1)
		testDispatcher.scheduler.advanceUntilIdle()
		
		// Get the correct shuffled index for the first question
		val shuffledQuestions = viewModel.shuffledQuestions.value
		val correctShuffledIndex = shuffledQuestions.first().correctShuffledIndex
		
		viewModel.selectAnswer(correctShuffledIndex)
		testDispatcher.scheduler.advanceUntilIdle()

		// Then
		val uiState = viewModel.uiState.value
		assertTrue(uiState.showAnswerFeedback)
		assertTrue(uiState.isCorrectAnswer)
		assertEquals(1, uiState.score)
		assertEquals(1, uiState.streak)
		assertEquals(1, uiState.totalQuestionsAnswered)
	}

	@Test
	fun `selectAnswer should reset streak for wrong answer`() = runTest {
		// Given
		val testQuestions = listOf(
			Question(
				id = 1,
				level = 1,
				question = "Test question?",
				choices = listOf("A", "B", "C", "D"),
				correctAnswerIndex = 0
			)
		)
		coEvery { getQuestionsForLevelUseCase(1) } returns flowOf(testQuestions)
		every { powerUpRepository.getAvailablePowerUps() } returns flowOf(emptyList())
		every { dailyChallengeRepository.isDailyChallengeAvailable() } returns flowOf(false)
		coEvery { powerUpRepository.checkAndEarnPowerUp(any()) } returns Unit
		coEvery { dataStoreRepository.saveScore(any()) } returns Unit
		coEvery { dataStoreRepository.saveStreak(any()) } returns Unit
		coEvery { dataStoreRepository.saveTotalQuestionsAnswered(any()) } returns Unit
		coEvery { gameProgressRepository.updateLevelProgress(any(), any(), any()) } returns Unit

		// When
		viewModel.loadQuestions(1)
		testDispatcher.scheduler.advanceUntilIdle()
		viewModel.selectAnswer(1)
		testDispatcher.scheduler.advanceUntilIdle()

		// Then
		val uiState = viewModel.uiState.value
		assertTrue(uiState.showAnswerFeedback)
		assertFalse(uiState.isCorrectAnswer)
		assertEquals(0, uiState.score)
		assertEquals(0, uiState.streak)
		assertEquals(1, uiState.totalQuestionsAnswered)
	}

	@Test
	fun `resetQuiz should reset all state`() = runTest {
		// Given
		coEvery { dataStoreRepository.resetProgress() } returns Unit

		// When
		viewModel.resetQuiz()
		testDispatcher.scheduler.advanceUntilIdle()

		// Then
		val uiState = viewModel.uiState.value
		assertEquals(0, uiState.currentQuestion)
		assertEquals(0, uiState.score)
		assertEquals(0, uiState.streak)
		assertEquals(0, uiState.totalQuestionsAnswered)
		assertEquals(-1, uiState.selectedAnswer)
		assertFalse(uiState.isCorrectAnswer)
		assertFalse(uiState.showAnswerFeedback)
	}
}
