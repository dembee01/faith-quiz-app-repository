package com.example.faithquiz.ui.view.topic

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.faithquiz.R
import com.example.faithquiz.ui.theme.Dimensions
import com.example.faithquiz.data.TopicQuestionBank
import com.example.faithquiz.data.store.ProgressDataStore
import kotlinx.coroutines.launch
import kotlin.random.Random
import com.example.faithquiz.data.model.QuizQuestion

@Composable
fun TopicQuizScreen(
    navController: NavController,
    topic: String
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    val topicType = when (topic) {
        "gospels" -> TopicQuestionBank.TopicType.GOSPELS
        "prophets" -> TopicQuestionBank.TopicType.PROPHETS
        "parables" -> TopicQuestionBank.TopicType.PARABLES
        else -> TopicQuestionBank.TopicType.GOSPELS
    }
    
    val randomSeed = rememberSaveable(topicType) { Random.nextLong() }

    val questions = remember(topicType, randomSeed) { 
        TopicQuestionBank.getQuestionsForTopic(topicType)
            .mapIndexed { i, q -> shuffleOptions(q, Random(randomSeed + i)) }
            .shuffled(Random(randomSeed)) 
    }
    
    // Try resuming from saved session
    val session by ProgressDataStore.observeTopicSession(context).collectAsState(initial = Triple("", 0, 0))
    val incomingTopicId = when (topicType) { 
        TopicQuestionBank.TopicType.GOSPELS -> "gospels"
        TopicQuestionBank.TopicType.PROPHETS -> "prophets"
        TopicQuestionBank.TopicType.PARABLES -> "parables"
    }

    var currentQuestionIndex by rememberSaveable(incomingTopicId) { mutableIntStateOf(if (session.first == incomingTopicId) session.second.coerceIn(0, questions.lastIndex) else 0) }
    var selectedAnswer by rememberSaveable(incomingTopicId) { mutableIntStateOf(-1) }
    var showAnswerFeedback by rememberSaveable(incomingTopicId) { mutableStateOf(false) }
    var score by rememberSaveable(incomingTopicId) { mutableIntStateOf(if (session.first == incomingTopicId) session.third.coerceIn(0, 50) else 0) }
    var isQuizCompleted by rememberSaveable { mutableStateOf(false) }
    
    val currentQuestion = if (questions.isNotEmpty() && currentQuestionIndex < questions.size) {
        questions[currentQuestionIndex]
    } else {
        null
    }
    
    val topicTitle = when (topicType) {
        TopicQuestionBank.TopicType.GOSPELS -> "Gospels Quiz"
        TopicQuestionBank.TopicType.PROPHETS -> "Prophets Quiz"
        TopicQuestionBank.TopicType.PARABLES -> "Parables Quiz"
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
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.back_description),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                Text(
                    text = topicTitle,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.width(Dimensions.iconSize))
            }
            
            Spacer(modifier = Modifier.height(Dimensions.spaceMedium))
            
            // Progress indicator
            LinearProgressIndicator(
                progress = { if (questions.isNotEmpty()) (currentQuestionIndex + 1).toFloat() / questions.size else 0f },
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(Dimensions.spaceMedium))
            
            // Question counter
            Text(
                text = "Question ${currentQuestionIndex + 1} of ${questions.size}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            
            Spacer(modifier = Modifier.height(Dimensions.spaceLarge))
            
            // Current question
            currentQuestion?.let { question ->
                Text(
                    text = question.question,
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(Dimensions.spaceLarge))
                
                // Answer options (can change before Submit)
                question.options.forEachIndexed { index, option ->
                    val isSelected = selectedAnswer == index
                    val isCorrect = index == question.correctAnswer
                    val isSelectedWrong = showAnswerFeedback && isSelected && !isCorrect
                    
                    Button(
                        onClick = {
                            if (!showAnswerFeedback) {
                                selectedAnswer = index
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = Dimensions.spaceXSmall),
                        shape = RoundedCornerShape(Dimensions.cornerRadiusMedium),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = when {
                                showAnswerFeedback && isCorrect -> Color(0xFF4CAF50)
                                isSelectedWrong -> Color(0xFFF44336)
                                isSelected && !showAnswerFeedback -> MaterialTheme.colorScheme.secondary
                                else -> MaterialTheme.colorScheme.surface
                            },
                            contentColor = when {
                                showAnswerFeedback && (isCorrect || isSelectedWrong) -> Color.White
                                else -> MaterialTheme.colorScheme.onSurface
                            }
                        ),
                        enabled = !showAnswerFeedback
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (showAnswerFeedback && isCorrect) {
                                Icon(Icons.Filled.Check, contentDescription = null)
                                Spacer(modifier = Modifier.width(Dimensions.spaceSmall))
                            } else if (isSelectedWrong) {
                                Icon(Icons.Filled.Close, contentDescription = null)
                                Spacer(modifier = Modifier.width(Dimensions.spaceSmall))
                            }
                            Text(
                                text = option,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(Dimensions.spaceSmall))
                }
                
                Spacer(modifier = Modifier.height(Dimensions.spaceLarge))
                
                // Submit or Next/Complete controls
                if (!showAnswerFeedback) {
                    Button(
                        onClick = {
                            if (selectedAnswer != -1) {
                                val isCorrectSelection = selectedAnswer == question.correctAnswer
                                showAnswerFeedback = true
                                if (isCorrectSelection) {
                                    score++
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(Dimensions.buttonHeight),
                        enabled = selectedAnswer != -1,
                        shape = RoundedCornerShape(Dimensions.cornerRadiusLarge),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Text(
                            text = "Submit",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
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
                                scope.launch {
                                    ProgressDataStore.updateTopicScoreIfHigher(context, topic, score)
                                    ProgressDataStore.clearTopicSession(context)
                                }
                                navController.popBackStack()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(Dimensions.buttonHeight),
                        shape = RoundedCornerShape(Dimensions.cornerRadiusLarge),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Text(
                            text = if (currentQuestionIndex < questions.size - 1) "Next Question" else "Complete Quiz",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
                
                // Explanation panel with verse reference (after submission)
                if (showAnswerFeedback) {
                    Spacer(modifier = Modifier.height(Dimensions.spaceMedium))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(Dimensions.cornerRadiusLarge),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(Dimensions.paddingLarge)) {
                            Text(
                                text = stringResource(id = R.string.explanation),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(Dimensions.spaceSmall))
                            Text(
                                text = question.explanation,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            question.verseReference?.let { ref ->
                                Spacer(modifier = Modifier.height(Dimensions.spaceSmall))
                                Text(text = ref, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
                            }
                            question.verseText?.let { verse ->
                                Spacer(modifier = Modifier.height(Dimensions.spaceSmall))
                                Text(text = verse, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
                            }
                            question.crossRefs?.takeIf { it.isNotEmpty() }?.let { refs ->
                                Spacer(modifier = Modifier.height(Dimensions.spaceSmall))
                                Text(text = "Cross-refs: ${refs.joinToString()}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(Dimensions.spaceMedium))
                
                // Current score
                Text(
                    text = "Score: $score/${questions.size}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                // Persist session progress on each question render
                LaunchedEffect(currentQuestionIndex, score) {
                    ProgressDataStore.saveTopicSession(context, incomingTopicId, currentQuestionIndex, score)
                }
            } ?: run {
                // Empty state when no questions are available
                Spacer(modifier = Modifier.height(Dimensions.spaceLarge))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(Dimensions.cornerRadiusLarge),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Dimensions.paddingLarge),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(Dimensions.spaceSmall))
                        Text(
                            text = stringResource(id = R.string.no_questions_available),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(Dimensions.spaceMedium))
                        Button(
                            onClick = { navController.popBackStack() },
                            shape = RoundedCornerShape(Dimensions.cornerRadiusLarge)
                        ) {
                            Text(text = stringResource(id = R.string.back))
                        }
                    }
                }
            }
        }
    }
}

private fun shuffleOptions(question: QuizQuestion, random: Random): QuizQuestion {
    val indexed = question.options.mapIndexed { idx, opt -> idx to opt }.shuffled(random)
    val newOptions = indexed.map { it.second }
    val newCorrectIndex = indexed.indexOfFirst { it.first == question.correctAnswer }
    return question.copy(options = newOptions, correctAnswer = newCorrectIndex)
}
