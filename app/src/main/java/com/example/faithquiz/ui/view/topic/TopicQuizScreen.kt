package com.example.faithquiz.ui.view.topic

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.faithquiz.R
import com.example.faithquiz.data.TopicQuestionBank
import com.example.faithquiz.data.model.QuizQuestion
import com.example.faithquiz.data.store.ProgressDataStore
import com.example.faithquiz.ui.theme.*
import com.example.faithquiz.ui.view.components.DivineBackground
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

    val topicTitle = when (topicType) {
        TopicQuestionBank.TopicType.GOSPELS -> "Gospels Quiz"
        TopicQuestionBank.TopicType.PROPHETS -> "Prophets Quiz"
        TopicQuestionBank.TopicType.PARABLES -> "Parables Quiz"
    }

    val session by ProgressDataStore.observeTopicSession(context).collectAsState(initial = null)
    if (session == null) {
        DivineBackground {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = GoldAccent)
            }
        }
        return
    }

    val incomingTopicId = topic.lowercase()
    val savedSession = session!!
    val isResuming = savedSession.topic == incomingTopicId && savedSession.seed != 0L
    val randomSeed = rememberSaveable(incomingTopicId, savedSession.seed) {
        if (isResuming) savedSession.seed else Random.nextLong()
    }

    val questions = remember(topicType, randomSeed) {
        TopicQuestionBank.getQuestionsForTopic(topicType)
            .mapIndexed { i, q -> shuffleOptions(q, Random(randomSeed + i)) }
            .shuffled(Random(randomSeed))
    }

    var currentQuestionIndex by rememberSaveable(incomingTopicId, randomSeed) {
        mutableIntStateOf(
            if (isResuming && questions.isNotEmpty()) {
                savedSession.currentIndex.coerceIn(0, questions.lastIndex)
            } else {
                0
            }
        )
    }
    var selectedAnswer by rememberSaveable(incomingTopicId, randomSeed) { mutableIntStateOf(-1) }
    var showAnswerFeedback by rememberSaveable(incomingTopicId, randomSeed) { mutableStateOf(false) }
    var score by rememberSaveable(incomingTopicId, randomSeed) {
        mutableIntStateOf(if (isResuming) savedSession.score.coerceIn(0, questions.size) else 0)
    }
    var isQuizCompleted by rememberSaveable(incomingTopicId, randomSeed) { mutableStateOf(false) }
    var totalElapsedSeconds by rememberSaveable(incomingTopicId, randomSeed) { mutableIntStateOf(0) }

    val timerViewModel: TimerViewModel = hiltViewModel()
    val currentQuestionTime by timerViewModel.currentQuestionTime.collectAsState()
    val totalLevelTime by timerViewModel.totalLevelTime.collectAsState()

    LaunchedEffect(Unit) {
        timerViewModel.setTotalTime(0)
        timerViewModel.startTimer()
    }

    LaunchedEffect(currentQuestionIndex, isQuizCompleted, showAnswerFeedback) {
        if (!isQuizCompleted && !showAnswerFeedback) {
            timerViewModel.resetQuestionTime()
            timerViewModel.startTimer()
        } else {
            timerViewModel.pauseTimer()
        }
    }

    LaunchedEffect(isQuizCompleted) {
        if (isQuizCompleted) {
            timerViewModel.pauseTimer()
            totalElapsedSeconds = timerViewModel.totalLevelTime.value.toInt()

            ProgressDataStore.updateTopicScoreIfHigher(context, topic, score)
            ProgressDataStore.clearTopicSession(context)
            ProgressDataStore.addTimeSpentSeconds(context, totalElapsedSeconds)
            ProgressDataStore.incrementQuestionsAnswered(context, questions.size)
        }
    }

    LaunchedEffect(currentQuestionIndex, score, isQuizCompleted) {
        if (!isQuizCompleted) {
            ProgressDataStore.saveTopicSession(context, incomingTopicId, currentQuestionIndex, score, randomSeed)
        }
    }

    if (isQuizCompleted) {
        ProfessionalResultsScreen(
            navController = navController,
            score = score,
            totalQuestions = questions.size,
            level = 0,
            timeSpentSeconds = totalElapsedSeconds,
            customTitle = "${topicTitle.uppercase()} COMPLETE",
            onRetry = {
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

    val currentQuestion = if (questions.isNotEmpty() && currentQuestionIndex < questions.size) questions[currentQuestionIndex] else null

    DivineBackground {
        Box(modifier = Modifier.fillMaxSize()) {
        if (currentQuestion != null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 22.dp, vertical = 16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Spacer(modifier = Modifier.height(72.dp))

                // Header Top Bar
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
                        text = topicTitle.uppercase(),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = SlateTextPrimary,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.width(48.dp))
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Progress Bar
                LinearProgressIndicator(
                    progress = { (currentQuestionIndex + 1).toFloat() / questions.size },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = GoldAccent,
                    trackColor = SlateSurfaceVariant
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Question Box
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
                    Column {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp)
                                .border(1.dp, SlateCardBorder, RoundedCornerShape(24.dp)),
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = SlateSurface)
                        ) {
                            Text(
                                text = targetQuestion.question,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = SlateTextPrimary,
                                modifier = Modifier.padding(22.dp),
                                textAlign = TextAlign.Center
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        targetQuestion.options.forEachIndexed { index, option ->
                            val isSelected = selectedAnswer == index
                            val isCorrect = index == targetQuestion.correctAnswer

                            val borderColor = when {
                                showAnswerFeedback && isCorrect -> CorrectAnswerGreen
                                showAnswerFeedback && isSelected && !isCorrect -> WrongAnswerRed
                                isSelected -> GoldAccent
                                else -> Color.Transparent
                            }

                            val containerColor = when {
                                showAnswerFeedback && isCorrect -> CorrectAnswerGreen
                                showAnswerFeedback && isSelected && !isCorrect -> WrongAnswerRed
                                isSelected -> SlateBackgroundTop
                                else -> SlateCardLight
                            }

                            val textColor = when {
                                showAnswerFeedback && (isCorrect || (isSelected && !isCorrect)) -> Color.White
                                isSelected -> Color.White
                                else -> SlateButtonText
                            }

                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp)
                                    .padding(vertical = 4.dp)
                                    .clip(RoundedCornerShape(28.dp))
                                    .clickable(enabled = !showAnswerFeedback, role = Role.RadioButton) {
                                        if (!showAnswerFeedback) selectedAnswer = index
                                    },
                                color = containerColor,
                                border = if (borderColor != Color.Transparent) BorderStroke(2.dp, borderColor) else null,
                                shadowElevation = 2.dp
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 20.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .background(
                                                color = if (isSelected || (showAnswerFeedback && isCorrect)) GoldAccent else SlateSurfaceVariant,
                                                shape = CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "${('A' + index)}",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected || (showAnswerFeedback && isCorrect)) SlateButtonText else SlateTextPrimary
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Text(
                                        text = option,
                                        color = textColor,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                if (showAnswerFeedback) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, SlateCardBorder, RoundedCornerShape(20.dp)),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = SlateSurface)
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Text(
                                text = "DIVINE INSIGHT",
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
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "${currentQuestion.verseReference} • ${currentQuestion.translation}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = GoldAccent
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                val buttonColor = if (showAnswerFeedback) SlateCardLight else SlateSurface
                val buttonTextColor = if (showAnswerFeedback) SlateButtonText else SlateTextPrimary
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
                                scope.launch {
                                    val reviewKey = ProgressDataStore.createReviewKey(0, currentQuestion.question)
                                    ProgressDataStore.recordReviewResult(context, reviewKey, isCorrect)
                                    if (!isCorrect) {
                                        ProgressDataStore.addMistakeDetailed(
                                            context,
                                            0,
                                            currentQuestion.question,
                                            currentQuestion.options.getOrNull(selectedAnswer).orEmpty(),
                                            currentQuestion.options.getOrNull(currentQuestion.correctAnswer).orEmpty(),
                                            currentQuestion.explanation
                                        )
                                    }
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
                        disabledContainerColor = SlateSurface.copy(alpha = 0.5f),
                        contentColor = buttonTextColor
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                ) {
                    Text(
                        text = buttonText,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(36.dp))
            }
        } else {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No questions found for this topic.", color = SlateTextPrimary)
            }
        }

        DivineTimerHUD(
            questionTimeSeconds = currentQuestionTime,
            totalTimeSeconds = totalLevelTime,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 8.dp)
        )
        } // end inner Box
    }
}

private fun shuffleOptions(question: QuizQuestion, random: Random): QuizQuestion {
    val indexed = question.options.mapIndexed { idx, opt -> idx to opt }.shuffled(random)
    val newOptions = indexed.map { it.second }
    val newCorrectIndex = indexed.indexOfFirst { it.first == question.correctAnswer }
    return question.copy(options = newOptions, correctAnswer = newCorrectIndex)
}

