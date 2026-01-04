package com.example.faithquiz.ui.view.topic

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import com.example.faithquiz.data.TopicQuestionBank
import com.example.faithquiz.data.model.QuizQuestion
import com.example.faithquiz.data.store.ProgressDataStore
import com.example.faithquiz.ui.theme.*
import com.example.faithquiz.ui.view.components.DivineTimerHUD
import com.example.faithquiz.ui.view.results.ProfessionalResultsScreen
import com.example.faithquiz.ui.viewmodel.TimerViewModel
import kotlinx.coroutines.launch
import kotlin.random.Random

@Composable
fun TopicQuizScreen(
    navController: NavController,
    topic: String
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    val topicType = when (topic.lowercase()) {
        "gospels" -> TopicQuestionBank.TopicType.GOSPELS
        "prophets" -> TopicQuestionBank.TopicType.PROPHETS
        "parables" -> TopicQuestionBank.TopicType.PARABLES
        else -> TopicQuestionBank.TopicType.GOSPELS
    }
    
    // Topic Title for UI
    val topicTitle = when (topicType) {
        TopicQuestionBank.TopicType.GOSPELS -> "Gospels Quiz"
        TopicQuestionBank.TopicType.PROPHETS -> "Prophets Quiz"
        TopicQuestionBank.TopicType.PARABLES -> "Parables Quiz"
    }

    // Stable random seed
    val randomSeed = rememberSaveable(topicType) { Random.nextLong() }

    val questions = remember(topicType, randomSeed) { 
        TopicQuestionBank.getQuestionsForTopic(topicType)
            .mapIndexed { i, q -> shuffleOptions(q, Random(randomSeed + i)) }
            .shuffled(Random(randomSeed)) 
    }
    
    // Resume Logic
    val session by ProgressDataStore.observeTopicSession(context).collectAsState(initial = Triple("", 0, 0))
    val incomingTopicId = topic.lowercase()

    val isResuming = session.first == incomingTopicId
    
    var currentQuestionIndex by rememberSaveable(incomingTopicId) { mutableIntStateOf(if (isResuming) session.second.coerceIn(0, questions.lastIndex) else 0) }
    var selectedAnswer by rememberSaveable(incomingTopicId) { mutableIntStateOf(-1) }
    var showAnswerFeedback by rememberSaveable(incomingTopicId) { mutableStateOf(false) }
    var score by rememberSaveable(incomingTopicId) { mutableIntStateOf(if (isResuming) session.third.coerceIn(0, 50) else 0) }
    var isQuizCompleted by rememberSaveable { mutableStateOf(false) }
    var totalElapsedSeconds by rememberSaveable { mutableIntStateOf(0) }

    // Timer ViewModel
    val timerViewModel: TimerViewModel = viewModel()
    val currentQuestionTime by timerViewModel.currentQuestionTime.collectAsState()
    val totalLevelTime by timerViewModel.totalLevelTime.collectAsState()

    // Reset Timer on Start
    LaunchedEffect(Unit) {
        // We reset total time for a new topic session mostly, or we could persist it if we want deeper resume support
        // For now, simpler is better: simple elapsed time
        timerViewModel.setTotalTime(0) 
        timerViewModel.startTimer()
    }

    // Timer Logic
    LaunchedEffect(currentQuestionIndex, isQuizCompleted, showAnswerFeedback) {
        if (!isQuizCompleted && !showAnswerFeedback) {
            timerViewModel.resetQuestionTime()
            timerViewModel.startTimer()
        } else {
            timerViewModel.pauseTimer()
        }
    }

    // Completion Logic
    LaunchedEffect(isQuizCompleted) {
        if (isQuizCompleted) {
            timerViewModel.pauseTimer()
            totalElapsedSeconds = timerViewModel.totalLevelTime.value.toInt()
            
            // Stats updates
            ProgressDataStore.updateTopicScoreIfHigher(context, topic, score)
            ProgressDataStore.clearTopicSession(context)
            ProgressDataStore.addTimeSpentSeconds(context, totalElapsedSeconds)
            ProgressDataStore.incrementQuestionsAnswered(context, questions.size) // Approximate
        }
    }

    // Persist session
    LaunchedEffect(currentQuestionIndex, score, isQuizCompleted) {
        if (!isQuizCompleted) {
            ProgressDataStore.saveTopicSession(context, incomingTopicId, currentQuestionIndex, score)
        }
    }

    // -- RENDERING --
    
    // 1. Completion Screen
    if (isQuizCompleted) {
        ProfessionalResultsScreen(
            navController = navController,
            score = score,
            totalQuestions = questions.size,
            level = 0, // Ignored
            timeSpentSeconds = totalElapsedSeconds,
            customTitle = "${topicTitle.uppercase()} COMPLETE",
            onRetry = {
                // Restart logic
                currentQuestionIndex = 0
                score = 0
                selectedAnswer = -1
                showAnswerFeedback = false
                isQuizCompleted = false
                timerViewModel.setTotalTime(0)
                timerViewModel.startTimer()
                scope.launch { ProgressDataStore.clearTopicSession(context) }
            },
            onNext = {
                navController.popBackStack()
            }
        )
        return
    }
    
    // 2. Quiz UI
    val currentQuestion = if (questions.isNotEmpty() && currentQuestionIndex < questions.size) questions[currentQuestionIndex] else null
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(DeepRoyalPurple, Color.Black)
                )
            )
    ) {
        if (currentQuestion != null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(Dimensions.screenPadding)
                    .verticalScroll(rememberScrollState())
            ) {
                // Spacer for HUD
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
                        text = topicTitle.uppercase(),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = GlowingGold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.width(Dimensions.iconSize))
                }
                
                Spacer(modifier = Modifier.height(Dimensions.spaceMedium))

                // Progress Bar
                LinearProgressIndicator(
                    progress = { (currentQuestionIndex + 1).toFloat() / questions.size },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = GlowingGold,
                    trackColor = EtherealGlass
                )
                
                Spacer(modifier = Modifier.height(Dimensions.spaceLarge))
                
                // Question
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
                    Column {
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
                
                // Explanation
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
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                
                // Button
                val buttonColor = if (showAnswerFeedback) GlowingGold else DeepRoyalPurple
                val buttonTextColor = if (showAnswerFeedback) DeepRoyalPurple else GlowingGold
                val buttonText = if (showAnswerFeedback) 
                    if (currentQuestionIndex < questions.size - 1) "NEXT QUESTION" else "FINISH QUIZ"
                else "SUBMIT ANSWER"

                Button(
                    onClick = {
                        if (!showAnswerFeedback) {
                            if (selectedAnswer != -1) {
                                val isCorrect = selectedAnswer == currentQuestion.correctAnswer
                                showAnswerFeedback = true
                                if (isCorrect) score++
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
        } else {
            // Empty State
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No questions found for this topic.", color = Color.White)
            }
        }

        // HUD
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
