package com.example.faithquiz.ui.view.dailychallenge

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.faithquiz.R
import com.example.faithquiz.ui.theme.Dimensions
import com.example.faithquiz.ui.viewmodel.DailyChallengeViewModel

@Composable
fun DailyChallengeScreen(
    navController: NavController,
    viewModel: DailyChallengeViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(Dimensions.screenPadding)
        ) {
            DailyChallengeHeader(onBack = navController::popBackStack)
            Spacer(Modifier.height(Dimensions.spaceLarge))

            when {
                state.isLoading -> Box(
                    modifier = Modifier.fillMaxWidth().padding(48.dp),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator() }

                state.error != null -> StatusCard(
                    title = "Challenge unavailable",
                    message = state.error.orEmpty(),
                    actionLabel = "Dismiss",
                    onAction = viewModel::clearError
                )

                state.dailyChallenge == null -> StatusCard(
                    title = "Challenge unavailable",
                    message = "Today's question could not be prepared. Please try again later."
                )

                state.isCompleted -> StatusCard(
                    title = "Today's challenge is complete",
                    message = "Come back tomorrow for a new question. Your devotion streak has been updated.",
                    actionLabel = "Back to menu",
                    onAction = navController::popBackStack
                )

                else -> {
                    val challenge = requireNotNull(state.dailyChallenge)
                    Card(
                        modifier = Modifier.fillMaxWidth().testTag("DailyChallengeQuestion"),
                        shape = RoundedCornerShape(Dimensions.cornerRadiusLarge),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(Modifier.padding(Dimensions.paddingLarge)) {
                            Text(
                                text = challenge.date,
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.height(12.dp))
                            Text(
                                text = challenge.question.question,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(20.dp))

                            challenge.question.options.forEachIndexed { index, option ->
                                val selected = state.selectedAnswer == index
                                val correct = index == challenge.question.correctAnswer
                                val feedbackColor = when {
                                    state.showAnswerFeedback && correct -> MaterialTheme.colorScheme.tertiary
                                    state.showAnswerFeedback && selected -> MaterialTheme.colorScheme.error
                                    selected -> MaterialTheme.colorScheme.primary
                                    else -> MaterialTheme.colorScheme.outlineVariant
                                }
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 5.dp)
                                        .clickable(
                                            enabled = !state.showAnswerFeedback,
                                            role = Role.RadioButton
                                        ) { viewModel.selectAnswer(index) }
                                        .semantics {
                                            contentDescription = "Answer ${index + 1}: $option"
                                        },
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(2.dp, feedbackColor),
                                    color = if (selected) {
                                        feedbackColor.copy(alpha = 0.12f)
                                    } else {
                                        MaterialTheme.colorScheme.surface
                                    }
                                ) {
                                    Text(
                                        text = option,
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                }
                            }

                            if (state.showAnswerFeedback) {
                                Spacer(Modifier.height(20.dp))
                                Text(
                                    text = if (state.isCorrectAnswer) "Correct — 2 points earned" else "Not quite",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = if (state.isCorrectAnswer) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(challenge.question.explanation)
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = "${challenge.question.verseReference} • ${challenge.question.translation}",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(Modifier.height(20.dp))
                                Button(
                                    onClick = viewModel::completeDailyChallenge,
                                    modifier = Modifier.fillMaxWidth().height(52.dp)
                                ) { Text("Complete today's challenge") }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DailyChallengeHeader(onBack: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back_description))
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.CalendarMonth, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.daily_challenge),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(Modifier.width(48.dp))
    }
}

@Composable
private fun StatusCard(
    title: String,
    message: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(title, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
            Spacer(Modifier.height(8.dp))
            Text(message, textAlign = TextAlign.Center)
            if (actionLabel != null && onAction != null) {
                Spacer(Modifier.height(20.dp))
                Button(onClick = onAction, colors = ButtonDefaults.buttonColors()) {
                    Text(actionLabel)
                }
            }
        }
    }
}
