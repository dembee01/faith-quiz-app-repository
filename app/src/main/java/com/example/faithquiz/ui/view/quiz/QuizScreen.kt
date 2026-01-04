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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.faithquiz.R
import com.example.faithquiz.data.QuestionBank
import com.example.faithquiz.data.model.QuizQuestion
import com.example.faithquiz.data.store.ProgressDataStore
import com.example.faithquiz.ui.navigation.Screen
import com.example.faithquiz.ui.theme.*
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
                .background(Brush.verticalGradient(listOf(DeepRoyalPurple, Color.Black))),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = GlowingGold)
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
    var remainingSeconds by rememberSaveable(level, mode) { mutableIntStateOf(if (mode == "speed") 60 else 0) } // Speed mode resume TODO?
    var startedAt by rememberSaveable(level, mode) { mutableLongStateOf(System.currentTimeMillis()) }
    var totalElapsedSeconds by rememberSaveable(level, mode) { mutableIntStateOf(0) }
    
    var lastProcessedQuestionIndex by rememberSaveable(level, mode) { mutableIntStateOf(-1) }
    
    // Timer ViewModel
    val timerViewModel: TimerViewModel = viewModel()
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
    LaunchedEffect(currentQuestionIndex, score, remainingLives, totalLevelTime, isQuizCompleted) {
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
                    lives = remainingLives
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

    



    // Main Container with Royal Gradient Background
    // Main Container with Royal Gradient Background
    // DivineBackground {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(DeepRoyalPurple, Color.Black)
                )
            )
    ) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(Dimensions.screenPadding)
                .verticalScroll(rememberScrollState())
        ) {
            // Spacer for Divine Timer HUD overlay
            Spacer(modifier = Modifier.height(80.dp))

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
                        tint = GlowingGold
                    )
                }
                
                Text(
                    text = "LEVEL $level",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = GlowingGold,
                    letterSpacing = 1.sp
                )
                
                // Mode indicator
                when (mode) {
                    "survival" -> Text(
                        text = "❤ $remainingLives",
                        style = MaterialTheme.typography.titleMedium,
                        color = GlowingGold
                    )
                    else -> Spacer(modifier = Modifier.width(Dimensions.iconSize))
                }
            }
            
            Spacer(modifier = Modifier.height(Dimensions.spaceMedium))
            
            // Gold Progress Bar
            LinearProgressIndicator(
                progress = { 
                    if (questions.isNotEmpty()) (currentQuestionIndex + 1).toFloat() / questions.size else 0f
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = GlowingGold,
                trackColor = EtherealGlass
            )
            
            Spacer(modifier = Modifier.height(Dimensions.spaceLarge))
            
            // Question Card with Animation
            AnimatedContent(
                targetState = currentQuestion,
                transitionSpec = {
                    ContentTransform(
                        targetContentEnter = fadeIn(animationSpec = tween(500)),
                        initialContentExit = fadeOut(animationSpec = tween(300))
                    )
                },
                label = "Question Animation"
            ) { targetQuestion ->
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = targetQuestion.question,
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Serif
                        ),
                        color = Color.White,
                        modifier = Modifier.padding(vertical = Dimensions.spaceLarge),
                        textAlign = TextAlign.Center
                    )

                    // Options
                    targetQuestion.options.forEachIndexed { index, option ->
                        val isSelected = selectedAnswer == index
                        val isCorrect = index == targetQuestion.correctAnswer
                        
                        val borderColor = when {
                            showAnswerFeedback && isCorrect -> CorrectAnswerGreen
                            showAnswerFeedback && isSelected && !isCorrect -> WrongAnswerRed
                            isSelected -> GlowingGold
                            else -> EtherealGlass
                        }
                        
                        val containerColor = when {
                            showAnswerFeedback && isCorrect -> CorrectAnswerGreen.copy(alpha = 0.3f)
                            showAnswerFeedback && isSelected && !isCorrect -> WrongAnswerRed.copy(alpha = 0.3f)
                            isSelected -> GlowingGold.copy(alpha = 0.2f)
                            else -> EtherealGlass
                        }

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .clickable(enabled = !showAnswerFeedback) {
                                    if (!showAnswerFeedback) selectedAnswer = index
                                },
                            color = containerColor,
                            border = BorderStroke(1.dp, borderColor),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Option Letter Circle
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .background(
                                            color = if(isSelected || (showAnswerFeedback && isCorrect)) borderColor else Color.White.copy(alpha = 0.1f),
                                            shape = CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${('A' + index)}",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = if(isSelected || (showAnswerFeedback && isCorrect)) Color.Black else Color.White
                                    )
                                }
                                
                                Spacer(modifier = Modifier.width(16.dp))
                                
                                Text(
                                    text = option,
                                    color = Color.White.copy(alpha = 0.9f),
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Explanation Panel (Glassmorphic)
            if (showAnswerFeedback) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(EtherealGlass)
                        .padding(16.dp)
                ) {
                    Column {
                        Text(
                            text = "DIVINE INSIGHT",
                            style = MaterialTheme.typography.labelSmall,
                            color = GlowingGold,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = currentQuestion.explanation,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White
                        )
                        currentQuestion.verseReference?.let { ref ->
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = ref, style = MaterialTheme.typography.labelMedium, color = GlowingGold)
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))

            // Action Button
            val buttonColor = if (showAnswerFeedback) GlowingGold else DeepRoyalPurple
            val buttonTextColor = if (showAnswerFeedback) DeepRoyalPurple else GlowingGold
            val buttonText = if (showAnswerFeedback) 
                if (currentQuestionIndex < questions.size - 1) "NEXT QUESTION" else "FINISH QUIZ"
            else "SUBMIT ANSWER"

            Button(
                onClick = {
                    if (!showAnswerFeedback) {
                        if (selectedAnswer != -1) {
                            // Submit Logic
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

                                    ProgressDataStore.addMistakeDetailed(context, level, currentQuestion.question, currentQuestion.options.getOrNull(selectedAnswer) ?: "", currentQuestion.options.getOrNull(currentQuestion.correctAnswer) ?: "", currentQuestion.explanation)
                                }
                                if (mode == "survival") {
                                    remainingLives = (remainingLives - 1).coerceAtLeast(0)
                                    if (remainingLives == 0) isQuizCompleted = true
                                }
                            }
                        }
                    } else {
                        // Next Logic
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
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = buttonColor,
                    disabledContainerColor = Color.White.copy(alpha = 0.1f),
                    contentColor = buttonTextColor
                ),
                border = if (!showAnswerFeedback) BorderStroke(1.dp, GlowingGold) else null
            ) {
                Text(
                    text = buttonText,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
            
            Spacer(modifier = Modifier.height(48.dp))
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

private fun shuffleOptions(question: QuizQuestion, random: Random): QuizQuestion {
    val indexed = question.options.mapIndexed { idx, opt -> idx to opt }.shuffled(random)
    val newOptions = indexed.map { it.second }
    val newCorrectIndex = indexed.indexOfFirst { it.first == question.correctAnswer }
    return question.copy(options = newOptions, correctAnswer = newCorrectIndex)
}

// Map keywords to sound resource IDs for Audio Atmosphere
fun resolveAmbience(questionText: String): Int {
    val text = questionText.lowercase()
    
    // To enable ambience, place mp3 files in res/raw/ and uncomment:
    // val R_raw_rain = R.raw.rain 
    
    return when {
         // text.contains("flood") || text.contains("noah") || text.contains("water") -> R.raw.rain
         // text.contains("desert") || text.contains("moses") || text.contains("wild") -> R.raw.wind
         // text.contains("temple") || text.contains("priest") -> R.raw.temple_chant
         // text.contains("star") || text.contains("night") -> R.raw.night_crickets
         else -> 0 // Silence
    }
}
