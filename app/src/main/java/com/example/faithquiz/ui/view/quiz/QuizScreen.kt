package com.example.faithquiz.ui.view.quiz

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.ContentTransform
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.faithquiz.R
import com.example.faithquiz.data.QuestionBank
import com.example.faithquiz.data.model.QuizQuestion
import com.example.faithquiz.data.store.ProgressDataStore
import com.example.faithquiz.ui.navigation.Screen
import com.example.faithquiz.ui.theme.*
import com.example.faithquiz.ui.view.components.DivineBackground
import com.example.faithquiz.ui.view.components.DivineTimerHUD
import com.example.faithquiz.ui.viewmodel.TimerViewModel
import com.example.faithquiz.util.AudioHelper

import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

@Composable
fun QuizScreen(
    navController: NavController,
    level: Int,
    mode: String = "classic"
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // -- Session Loading Logic --
    // We defer initialization until we check for a saved session
    var isSessionLoaded by remember { mutableStateOf(false) }
    var restoredSession by remember { mutableStateOf<ProgressDataStore.QuizSession?>(null) }

    LaunchedEffect(level) {
        ProgressDataStore.observeQuizSession(context).collect { session ->
            restoredSession = session
            isSessionLoaded = true
        }
    }

    if (!isSessionLoaded) {
        // Loading State
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(SlateBackgroundTop, SlateBackgroundBottom))),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = GoldAccent)
        }
        return
    }

    val session = restoredSession
    val isRestoring = session != null && session.level == level && session.mode == mode

    // Stable random seed: from session or new
    val seedToUse = if (isRestoring) session!!.seed else Random.nextLong()
    val randomSeed = rememberSaveable(level) { seedToUse }

    // Level-specific questions
    val questions = remember(level, randomSeed) {
        QuestionBank.getQuestionsForLevel(level)
            .mapIndexed { i, q -> shuffleOptions(q, Random(randomSeed + i)) }
            .shuffled(Random(randomSeed))
    }
    
    // Quiz state (Restored or Default)
    var currentQuestionIndex by rememberSaveable(level, mode) { mutableIntStateOf(if (isRestoring) session!!.index else 0) }
    var selectedAnswer by rememberSaveable(level, mode) { mutableIntStateOf(-1) }
    var showAnswerFeedback by rememberSaveable(level, mode) { mutableStateOf(false) }
    var score by rememberSaveable(level, mode) { mutableIntStateOf(if (isRestoring) session!!.score else 0) }
    var isQuizCompleted by rememberSaveable(level, mode) { mutableStateOf(false) }
    var remainingLives by rememberSaveable(level, mode) { mutableIntStateOf(if (isRestoring) session!!.lives else (if (mode == "survival") 3 else 0)) }
    var remainingSeconds by rememberSaveable(level, mode) {
        mutableIntStateOf(
            if (mode == "speed") {
                if (isRestoring) session!!.remainingSeconds else 60
            } else {
                0
            }
        )
    }
    var totalElapsedSeconds by rememberSaveable(level, mode) { mutableIntStateOf(0) }
    
    var lastProcessedQuestionIndex by rememberSaveable(level, mode) { mutableIntStateOf(-1) }
    
    // Timer ViewModel
    val timerViewModel: TimerViewModel = hiltViewModel()
    val currentQuestionTime by timerViewModel.currentQuestionTime.collectAsState()
    val totalLevelTime by timerViewModel.totalLevelTime.collectAsState()

    // Initialize Timer on Load
    LaunchedEffect(isRestoring) {
        if (isRestoring) {
             timerViewModel.setTotalTime(session!!.time)
        }
    }

    // Timer Logic
    LaunchedEffect(currentQuestionIndex, isQuizCompleted) {
        if (!isQuizCompleted) {
            timerViewModel.resetQuestionTime()
            timerViewModel.startTimer()
        } else {
            timerViewModel.pauseTimer()
        }
    }
    
    // -- Autosave Logic --
    // Save state whenever index, score, or pause changes
    LaunchedEffect(currentQuestionIndex, score, remainingLives, remainingSeconds, totalLevelTime, isQuizCompleted) {
        if (!isQuizCompleted) {
            ProgressDataStore.saveQuizSession(
                context,
                ProgressDataStore.QuizSession(
                    level = level,
                    mode = mode,
                    index = currentQuestionIndex,
                    score = score,
                    time = totalLevelTime,
                    seed = randomSeed,
                    lives = remainingLives,
                    remainingSeconds = remainingSeconds
                )
            )
        } else {
            ProgressDataStore.clearQuizSession(context)
        }
    }

    DisposableEffect(Unit) {
        onDispose { 
            timerViewModel.pauseTimer() 
            // Final save on exit if not completed
            if (!isQuizCompleted) {
                // We launch in a global scope or runBlocking? 
                // DataStore scope is IO, but onDispose can't call suspend.
                // However, the LaunchedEffect above tracks state changes.
                // The only gap is the time elapsed since last composition.
                // Ideally we'd save here, but we can't easily.
                // The autosave above runs on `totalLevelTime` changes (every second), so we're good!
            }
        }
    }

    // Speed Mode Timer
    LaunchedEffect(mode, remainingSeconds, showAnswerFeedback, isQuizCompleted) {
        if (mode == "speed" && !showAnswerFeedback && !isQuizCompleted) {
            if (remainingSeconds > 0) {
                delay(1000)
                val nextVal = remainingSeconds - 1
                if (nextVal <= 0) isQuizCompleted = true else remainingSeconds = nextVal
            }
        }
    }
    
    // Safety check
    if (currentQuestionIndex >= questions.size || currentQuestionIndex < 0) {
        isQuizCompleted = true
    }
    
    // Stats tracking
    LaunchedEffect(showAnswerFeedback, currentQuestionIndex) {
        if (showAnswerFeedback && lastProcessedQuestionIndex != currentQuestionIndex) {
            ProgressDataStore.incrementQuestionsAnswered(context, 1)
            lastProcessedQuestionIndex = currentQuestionIndex
        }
    }

    // Completion Logic
    LaunchedEffect(isQuizCompleted) {
        if (isQuizCompleted) {
            // Fix: Use accumulated time from ViewModel instead of session duration
            // This prevents the loophole where resuming resets the "time spent" calculation
            val elapsedSec = timerViewModel.totalLevelTime.value.toInt().coerceAtLeast(0)
            totalElapsedSeconds = elapsedSec
            ProgressDataStore.addTimeSpentSeconds(context, elapsedSec)
            ProgressDataStore.incrementTotalAttempts(context)
            ProgressDataStore.setLastAttemptSummary(context, level, score, questions.size, elapsedSec, mode)

            if (mode != "practice") ProgressDataStore.setHighScoreIfGreater(context, score)
            
            val percentage = if (questions.isNotEmpty()) (score.toFloat() / questions.size * 100).toInt() else 0
            if ((mode == "classic" || mode == "journey") && percentage >= 60) {
                ProgressDataStore.setLastCompletedLevel(context, level)
                ProgressDataStore.unlockNextLevel(context, level)
            }
            ProgressDataStore.recordQuizCompletion(context, score, questions.size)
            ProgressDataStore.updateAdaptiveLevelFromAccuracy(context, percentage)
        }
    }

    // Move to Results Screen
    if (isQuizCompleted) {
        com.example.faithquiz.ui.view.results.ProfessionalResultsScreen(
            navController = navController,
            score = score,
            totalQuestions = questions.size,
            level = level,
            timeSpentSeconds = totalElapsedSeconds
        )
        return
    }

    // UI RENDER START
    val currentQuestion = questions[currentQuestionIndex]

    // Audio Atmosphere: Play ambience based on context
    LaunchedEffect(currentQuestion) {
        val ambienceResId = resolveAmbience(currentQuestion.question)
        if (ambienceResId != 0) {
            AudioHelper.playAmbience(context, ambienceResId)
        } else {
            AudioHelper.stopAmbience()
        }
    }
    
    DisposableEffect(Unit) {
        onDispose {
            AudioHelper.stopAmbience()
        }
    }

    



    // Main Container with Slate Gradient Background
    DivineBackground {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 22.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Spacer for Divine Timer HUD overlay
                Spacer(modifier = Modifier.height(76.dp))

                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back_description),
                            tint = SlateTextPrimary
                        )
                    }

                    Text(
                        text = "LEVEL $level",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = SlateTextPrimary,
                        letterSpacing = 1.sp
                    )

                    // Mode indicator
                    when (mode) {
                        "survival" -> Text(
                            text = "❤ $remainingLives",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = GoldAccent
                        )
                        else -> Spacer(modifier = Modifier.width(48.dp))
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Gold Progress Bar
                LinearProgressIndicator(
                    progress = {
                        if (questions.isNotEmpty()) (currentQuestionIndex + 1).toFloat() / questions.size else 0f
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(CircleShape),
                    color = GoldAccent,
                    trackColor = SlateSurfaceVariant
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Question Card with Animation
                AnimatedContent(
                    targetState = currentQuestion,
                    transitionSpec = {
                        ContentTransform(
                            targetContentEnter = fadeIn(animationSpec = tween(400)),
                            initialContentExit = fadeOut(animationSpec = tween(250))
                        )
                    },
                    label = "Question Animation"
                ) { targetQuestion ->
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = targetQuestion.question,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = SlateTextPrimary,
                            modifier = Modifier.padding(vertical = 16.dp),
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Choice Options (Full Width Rounded Pills)
                        targetQuestion.options.forEachIndexed { index, option ->
                            val isSelected = selectedAnswer == index
                            val isCorrect = index == targetQuestion.correctAnswer

                            val (containerColor, contentColor, borderColor) = when {
                                showAnswerFeedback && isCorrect -> Triple(CorrectAnswerGreen, Color.White, CorrectAnswerGreen)
                                showAnswerFeedback && isSelected && !isCorrect -> Triple(WrongAnswerRed, Color.White, WrongAnswerRed)
                                isSelected -> Triple(SlateBackgroundTop, Color.White, GoldAccent)
                                else -> Triple(SlateCardLight, SlateButtonText, SlateCardBorder)
                            }

                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp)
                                    .clickable(enabled = !showAnswerFeedback, role = Role.RadioButton) {
                                        if (!showAnswerFeedback) selectedAnswer = index
                                    },
                                shape = RoundedCornerShape(28.dp),
                                color = containerColor,
                                border = BorderStroke(1.5.dp, borderColor),
                                shadowElevation = if (isSelected || (showAnswerFeedback && isCorrect)) 4.dp else 1.dp
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 20.dp, vertical = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (isSelected || (showAnswerFeedback && (isCorrect || isSelected)))
                                                    Color.White.copy(alpha = 0.25f)
                                                else
                                                    SlateBackgroundTop.copy(alpha = 0.12f)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "${('A' + index)}",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = contentColor
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(16.dp))

                                    Text(
                                        text = option,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = contentColor,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Explanation Card
                if (showAnswerFeedback) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = SlateSurface)
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Text(
                                text = "EXPLANATION & INSIGHT",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = GoldAccent,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = currentQuestion.explanation,
                                fontSize = 14.sp,
                                color = SlateTextPrimary
                            )
                            currentQuestion.verseReference?.let { ref ->
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "$ref • ${currentQuestion.translation}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = SlateTextSecondary
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Action Button
                val buttonColor = if (showAnswerFeedback) GoldAccent else SlateCardLight
                val buttonTextColor = if (showAnswerFeedback) SlateBackgroundBottom else SlateButtonText
                val buttonText = if (showAnswerFeedback)
                    if (currentQuestionIndex < questions.size - 1) "NEXT QUESTION" else "FINISH QUIZ"
                else "SUBMIT ANSWER"

                Button(
                    onClick = {
                        if (!showAnswerFeedback) {
                            if (selectedAnswer != -1) {
                                val isCorrectSelection = selectedAnswer == currentQuestion.correctAnswer
                                showAnswerFeedback = true
                                timerViewModel.pauseTimer()
                                if (isCorrectSelection) {
                                    AudioHelper.playCorrect()
                                    AudioHelper.vibrateSuccess(context)
                                    score++
                                } else {
                                    AudioHelper.playWrong()
                                    AudioHelper.vibrateError(context)
                                    scope.launch {
                                        ProgressDataStore.addMistakeDetailed(
                                            context,
                                            level,
                                            currentQuestion.question,
                                            currentQuestion.options.getOrNull(selectedAnswer) ?: "",
                                            currentQuestion.options.getOrNull(currentQuestion.correctAnswer) ?: "",
                                            currentQuestion.explanation
                                        )
                                    }
                                    if (mode == "survival") {
                                        remainingLives = (remainingLives - 1).coerceAtLeast(0)
                                        if (remainingLives == 0) isQuizCompleted = true
                                    }
                                }
                                scope.launch {
                                    ProgressDataStore.recordReviewResult(
                                        context,
                                        ProgressDataStore.createReviewKey(level, currentQuestion.question),
                                        isCorrectSelection
                                    )
                                }
                            }
                        } else {
                            if (currentQuestionIndex < questions.size - 1) {
                                currentQuestionIndex++
                                selectedAnswer = -1
                                showAnswerFeedback = false
                            } else {
                                isQuizCompleted = true
                            }
                        }
                    },
                    enabled = selectedAnswer != -1 || showAnswerFeedback,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = buttonColor,
                        disabledContainerColor = SlateSurfaceVariant,
                        contentColor = buttonTextColor,
                        disabledContentColor = SlateTextMuted
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                ) {
                    Text(
                        text = buttonText,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(40.dp))
            }

            // Divine Timer HUD
            DivineTimerHUD(
                questionTimeSeconds = currentQuestionTime,
                totalTimeSeconds = totalLevelTime,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 8.dp)
            )
        }
    }
}

private fun shuffleOptions(question: QuizQuestion, random: Random): QuizQuestion {
    val indexed = question.options.mapIndexed { idx, opt -> idx to opt }.shuffled(random)
    val newOptions = indexed.map { it.second }
    val newCorrectIndex = indexed.indexOfFirst { it.first == question.correctAnswer }
    return question.copy(options = newOptions, correctAnswer = newCorrectIndex)
}

// Map keywords to sound resource IDs for Audio Atmosphere
fun resolveAmbience(@Suppress("UNUSED_PARAMETER") questionText: String): Int {
    
    // To enable ambience, place mp3 files in res/raw/ and uncomment:
    // val R_raw_rain = R.raw.rain 
    
    return 0
}
