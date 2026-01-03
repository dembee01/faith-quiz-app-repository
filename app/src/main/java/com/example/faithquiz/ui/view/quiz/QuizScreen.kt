package com.example.faithquiz.ui.view.quiz

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.faithquiz.R
import com.example.faithquiz.ui.navigation.Screen
import com.example.faithquiz.ui.theme.Dimensions
import com.example.faithquiz.ui.theme.CorrectAnswerGreen
import com.example.faithquiz.ui.theme.WrongAnswerRed
import com.example.faithquiz.ui.theme.ProgressTrackGray
import kotlinx.coroutines.delay
import com.example.faithquiz.data.QuestionBank
import androidx.compose.ui.platform.LocalContext
import com.example.faithquiz.data.store.ProgressDataStore
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import kotlin.random.Random
import com.example.faithquiz.data.model.QuizQuestion

// Simple quiz question data
// moved to data.model.QuizQuestion

@Composable
fun QuizScreen(
    navController: NavController,
    level: Int,
    mode: String = "classic"
) {
    // Stable random seed per level session so order persists across rotation
    val randomSeed = rememberSaveable(level) { Random.nextLong() }

    // Level-specific questions with per-question option shuffle (stable by seed)
    val questions = remember(level, randomSeed) {
        QuestionBank.getQuestionsForLevel(level)
            .mapIndexed { i, q -> shuffleOptions(q, Random(randomSeed + i)) }
            .shuffled(Random(randomSeed))
    }
    
    // Quiz state (save across configuration changes)
    var currentQuestionIndex by rememberSaveable(level, mode) { mutableIntStateOf(0) }
    var selectedAnswer by rememberSaveable(level, mode) { mutableIntStateOf(-1) }
    var showAnswerFeedback by rememberSaveable(level, mode) { mutableStateOf(false) }
    var score by rememberSaveable(level, mode) { mutableIntStateOf(0) }
    var isQuizCompleted by rememberSaveable(level, mode) { mutableStateOf(false) }
    var remainingLives by rememberSaveable(level, mode) { mutableIntStateOf(if (mode == "survival") 3 else 0) }
    var remainingSeconds by rememberSaveable(level, mode) { mutableIntStateOf(if (mode == "speed") 60 else 0) }
    var startedAt by rememberSaveable(level, mode) { mutableLongStateOf(System.currentTimeMillis()) }
    var totalElapsedSeconds by rememberSaveable(level, mode) { mutableIntStateOf(0) }
    
    LaunchedEffect(level) {
        // Quiz started for level
    }
    
    // Safety check for array bounds
    if (currentQuestionIndex >= questions.size || currentQuestionIndex < 0) {
        isQuizCompleted = true
        return
    }
    
    val currentQuestion = questions[currentQuestionIndex]
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Timer for speed mode
    LaunchedEffect(mode, remainingSeconds, showAnswerFeedback, isQuizCompleted) {
        if (mode == "speed" && !showAnswerFeedback && !isQuizCompleted) {
            if (remainingSeconds > 0) {
                delay(1000)
                val nextVal = remainingSeconds - 1
                if (nextVal <= 0) {
                    isQuizCompleted = true
                } else {
                    remainingSeconds = nextVal
                }
            }
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                .padding(Dimensions.screenPadding)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { navController.popBackStack() }
                    ) {
                                            Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.back_description),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                    }
                    
                    Text(
                    text = stringResource(id = R.string.quizzes_title),
                    style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold
                        ),
                    color = MaterialTheme.colorScheme.onSurface
                    )
                    
                // Mode/status indicator
                when (mode) {
                    "speed" -> {
                    Text(
                            text = "${remainingSeconds}s",
                            style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
                
                    "survival" -> {
                Text(
                            text = "❤ ${remainingLives}",
                            style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                    
                    "practice" -> {
                        Text(
                            text = stringResource(id = R.string.practice_mode),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                    else -> {
                        Spacer(modifier = Modifier.width(Dimensions.iconSize))
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(Dimensions.spaceMedium))
            
            // Progress bar
            LinearProgressIndicator(
                progress = { 
                    if (questions.isNotEmpty()) {
                        (currentQuestionIndex + 1).toFloat() / questions.size
                    } else {
                        0f
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.onSurface,
                trackColor = ProgressTrackGray
            )
            
            Spacer(modifier = Modifier.height(Dimensions.spaceLarge))
            
            // Question card
            Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = currentQuestion.question,
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = Dimensions.paddingXLarge, vertical = Dimensions.spaceLarge)
                )

                // Flat list of options
                currentQuestion.options.forEachIndexed { index, option ->
                    val isSelected = selectedAnswer == index
                    val isCorrect = index == currentQuestion.correctAnswer
                    val containerColor = when {
                        showAnswerFeedback && isCorrect -> CorrectAnswerGreen
                        showAnswerFeedback && isSelected && !isCorrect -> WrongAnswerRed
                        isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                        else -> MaterialTheme.colorScheme.surface
                    }
                    val contentColor = when {
                        showAnswerFeedback && (isCorrect || (isSelected && !isCorrect)) -> Color.White
                        else -> MaterialTheme.colorScheme.onSurface
                    }

                    Surface(
                                            modifier = Modifier
                                                .fillMaxWidth()
                            .height(56.dp)
                            .padding(horizontal = Dimensions.paddingXLarge, vertical = Dimensions.spaceSmall)
                            .clickable(enabled = !showAnswerFeedback) {
                                if (!showAnswerFeedback) {
                                    selectedAnswer = index
                                }
                            },
                        color = containerColor,
                        shadowElevation = 0.dp,
                        tonalElevation = 0.dp
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                .padding(horizontal = Dimensions.paddingSmall),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = option,
                                color = contentColor,
                                                    style = MaterialTheme.typography.bodyLarge,
                                                    modifier = Modifier.weight(1f)
                                                )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(Dimensions.spaceLarge))
            
            // Controls (Submit then Next)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Dimensions.paddingXLarge, vertical = Dimensions.spaceMedium),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (!showAnswerFeedback) {
                    Button(
                        onClick = {
                            if (selectedAnswer != -1) {
                                val isCorrectSelection = selectedAnswer == currentQuestion.correctAnswer
                                showAnswerFeedback = true
                                if (isCorrectSelection) {
                                    score++
                                } else {
                                    scope.launch {
                                        val userAns = currentQuestion.options.getOrNull(selectedAnswer) ?: ""
                                        val correctAns = currentQuestion.options.getOrNull(currentQuestion.correctAnswer) ?: ""
                                        ProgressDataStore.addMistakeDetailed(
                                            context = context,
                                            level = level,
                                            question = currentQuestion.question,
                                            userAnswer = userAns,
                                            correctAnswer = correctAns,
                                            explanation = currentQuestion.explanation
                                        )
                                    }
                                    if (mode == "survival") {
                                        remainingLives = (remainingLives - 1).coerceAtLeast(0)
                                        if (remainingLives == 0) {
                                            isQuizCompleted = true
                                        }
                                    }
                                }
                            }
                        },
                        enabled = selectedAnswer != -1,
                        shape = RoundedCornerShape(Dimensions.cornerRadiusLarge),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary)
                    ) {
                        Text(
                            text = "Submit",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                } else {
                    Button(
                        onClick = {
                            if (currentQuestionIndex < questions.size - 1) {
                                currentQuestionIndex++
                                selectedAnswer = -1
                                showAnswerFeedback = false
                            } else {
                                isQuizCompleted = true
                            }
                        },
                        enabled = true,
                        shape = RoundedCornerShape(Dimensions.cornerRadiusLarge),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary)
                    ) {
                        Text(
                            text = if (currentQuestionIndex < questions.size - 1) stringResource(R.string.next_question) else stringResource(R.string.complete_quiz),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(Dimensions.spaceMedium))
            Text(
                text = stringResource(R.string.score_format, score, questions.size),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
        
        // Explanation panel
        if (showAnswerFeedback) {
            Spacer(modifier = Modifier.height(Dimensions.spaceMedium))

            // Explanation with verse/source
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(Dimensions.cornerRadiusLarge),
                                    colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(Dimensions.paddingLarge)) {
                    Text(
                        text = stringResource(R.string.explanation),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(Dimensions.spaceSmall))
                                            Text(
                                                text = currentQuestion.explanation,
                                                style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    currentQuestion.verseReference?.let { ref ->
                        Spacer(modifier = Modifier.height(Dimensions.spaceSmall))
                        Text(text = ref, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
                    }
                    currentQuestion.verseText?.let { verse ->
                        Spacer(modifier = Modifier.height(Dimensions.spaceSmall))
                        Text(text = verse, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
                    }
                    currentQuestion.commentary?.let { comm ->
                        Spacer(modifier = Modifier.height(Dimensions.spaceSmall))
                        Text(text = comm, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f))
                    }
                    currentQuestion.crossRefs?.takeIf { it.isNotEmpty() }?.let { refs ->
                        Spacer(modifier = Modifier.height(Dimensions.spaceSmall))
                        Text(text = "Cross-refs: ${refs.joinToString()}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
                    }
                    // Learn more link intentionally hidden per request
                }
            }
        }
        
        if (isQuizCompleted) {
            QuizCompletedScreen(
                navController = navController,
                score = score,
                totalQuestions = questions.size,
                level = level,
                mode = mode,
                remainingSeconds = remainingSeconds,
                remainingLives = remainingLives,
                timeSpentSeconds = totalElapsedSeconds
            )
        }
    }

    // When user selects an answer and feedback is shown, increment total answered once per question
    LaunchedEffect(showAnswerFeedback, currentQuestionIndex) {
        if (showAnswerFeedback) {
            ProgressDataStore.incrementQuestionsAnswered(context, 1)
        }
    }

    // Update high score (per level score) on quiz completion
    LaunchedEffect(isQuizCompleted) {
        if (isQuizCompleted) {
            // Compute and persist time spent in this attempt
            val elapsedSec = (((System.currentTimeMillis() - startedAt) / 1000).toInt()).coerceAtLeast(0)
            totalElapsedSeconds = elapsedSec
            ProgressDataStore.addTimeSpentSeconds(context, elapsedSec)
            ProgressDataStore.incrementTotalAttempts(context)
            ProgressDataStore.setLastAttemptSummary(context, level, score, questions.size, elapsedSec, mode)

            // Update high score (per level score)
            if (mode != "practice") {
                ProgressDataStore.setHighScoreIfGreater(context, score)
            }
            val percentage = if (questions.isNotEmpty()) (score.toFloat() / questions.size * 100).toInt() else 0
            if (mode == "classic" && percentage >= 60) {
                ProgressDataStore.setLastCompletedLevel(context, level)
                ProgressDataStore.unlockNextLevel(context, level)
            }
            ProgressDataStore.recordQuizCompletion(context, score, questions.size)
            ProgressDataStore.updateAdaptiveLevelFromAccuracy(context, percentage)
        }
    }
}

private fun shuffleOptions(question: QuizQuestion, random: Random): QuizQuestion {
    val indexed = question.options.mapIndexed { idx, opt -> idx to opt }.shuffled(random)
    val newOptions = indexed.map { it.second }
    val newCorrectIndex = indexed.indexOfFirst { it.first == question.correctAnswer }
    return question.copy(options = newOptions, correctAnswer = newCorrectIndex)
}

@Composable
fun QuizCompletedScreen(
    navController: NavController,
    score: Int,
    totalQuestions: Int,
    level: Int,
    mode: String = "classic",
    remainingSeconds: Int = 0,
    remainingLives: Int = 0,
    timeSpentSeconds: Int = 0
) {
    val context = LocalContext.current
    // scope not needed currently
    val percentage = if (totalQuestions > 0) {
        (score.toFloat() / totalQuestions * 100).toInt()
    } else {
        0
    }

    LaunchedEffect(percentage, mode) {
        if (mode == "classic" && percentage >= 60) {
            // Unlock next level persistently
            ProgressDataStore.unlockNextLevel(context, level)
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.secondary
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "🎉",
                    style = MaterialTheme.typography.displayLarge
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Level $level Complete!",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                )
                            
                            Spacer(modifier = Modifier.height(24.dp))
                            
                Text(
                    text = "Your Score",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                
                Text(
                    text = "$score/$totalQuestions",
                    style = MaterialTheme.typography.displayMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.primary
                )
                
                Text(
                    text = "$percentage%",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Time: ${timeSpentSeconds}s",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                
                Spacer(modifier = Modifier.height(32.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = {
                            navController.popBackStack()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                            contentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    ) {
                        Text(
                            text = stringResource(R.string.back),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = Modifier.padding(8.dp)
                        )
                    }

                    if (mode == "classic" && percentage >= 60 && level < 30) {
                                Button(
                                    onClick = {
                                val nextLevel = level + 1
                                navController.navigate(Screen.Quiz.createRoute(nextLevel)) {
                                    popUpTo(Screen.LevelSelect.route)
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                                ) {
                                    Text(
                                text = stringResource(R.string.next_level),
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold
                                ),
                                modifier = Modifier.padding(8.dp)
        

                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
