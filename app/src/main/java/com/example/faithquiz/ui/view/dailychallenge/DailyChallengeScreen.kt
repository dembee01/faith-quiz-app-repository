package com.example.faithquiz.ui.view.dailychallenge

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
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.faithquiz.R
import com.example.faithquiz.ui.theme.*
import com.example.faithquiz.ui.view.components.DivineBackground
import com.example.faithquiz.ui.viewmodel.DailyChallengeViewModel

@Composable
fun DailyChallengeScreen(
    navController: NavController,
    viewModel: DailyChallengeViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    DivineBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp, vertical = 16.dp)
        ) {
            DailyChallengeHeader(onBack = navController::popBackStack)
            Spacer(Modifier.height(20.dp))

            when {
                state.isLoading -> Box(
                    modifier = Modifier.fillMaxWidth().padding(48.dp),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator(color = GoldAccent) }

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
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = SlateSurface)
                    ) {
                        Column(Modifier.padding(20.dp)) {
                            Text(
                                text = challenge.date.uppercase(),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = GoldAccent,
                                letterSpacing = 1.sp
                            )
                            Spacer(Modifier.height(10.dp))
                            Text(
                                text = challenge.question.question,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = SlateTextPrimary
                            )
                            Spacer(Modifier.height(20.dp))

                            challenge.question.options.forEachIndexed { index, option ->
                                val selected = state.selectedAnswer == index
                                val correct = index == challenge.question.correctAnswer
                                val (containerColor, contentColor, borderColor) = when {
                                    state.showAnswerFeedback && correct -> Triple(CorrectAnswerGreen, Color.White, CorrectAnswerGreen)
                                    state.showAnswerFeedback && selected && !correct -> Triple(WrongAnswerRed, Color.White, WrongAnswerRed)
                                    selected -> Triple(SlateBackgroundTop, Color.White, GoldAccent)
                                    else -> Triple(SlateCardLight, SlateButtonText, SlateCardBorder)
                                }
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 6.dp)
                                        .clickable(
                                            enabled = !state.showAnswerFeedback,
                                            role = Role.RadioButton
                                        ) { viewModel.selectAnswer(index) }
                                        .semantics {
                                            contentDescription = "Answer ${index + 1}: $option"
                                        },
                                    shape = RoundedCornerShape(28.dp),
                                    color = containerColor,
                                    border = BorderStroke(1.5.dp, borderColor)
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
                                                    if (selected || (state.showAnswerFeedback && (correct || selected)))
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
                                        Spacer(Modifier.width(16.dp))
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

                            if (state.showAnswerFeedback) {
                                Spacer(Modifier.height(20.dp))
                                Text(
                                    text = if (state.isCorrectAnswer) "Correct — 2 points earned!" else "Not quite",
                                    fontSize = 17.sp,
                                    color = if (state.isCorrectAnswer) CorrectAnswerGreen else WrongAnswerRed,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = challenge.question.explanation,
                                    fontSize = 14.sp,
                                    color = SlateTextPrimary
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = "${challenge.question.verseReference} • ${challenge.question.translation}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = SlateTextSecondary
                                )
                                Spacer(Modifier.height(24.dp))
                                Button(
                                    onClick = viewModel::completeDailyChallenge,
                                    modifier = Modifier.fillMaxWidth().height(56.dp),
                                    shape = RoundedCornerShape(28.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = SlateCardLight,
                                        contentColor = SlateButtonText
                                    )
                                ) {
                                    Text(
                                        text = "Complete Today's Challenge",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
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
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back_description), tint = SlateTextPrimary)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = GoldAccent)
            Spacer(Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.daily_challenge),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = SlateTextPrimary
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
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SlateSurface)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(title, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = SlateTextPrimary, textAlign = TextAlign.Center)
            Spacer(Modifier.height(8.dp))
            Text(message, fontSize = 14.sp, color = SlateTextSecondary, textAlign = TextAlign.Center)
            if (actionLabel != null && onAction != null) {
                Spacer(Modifier.height(20.dp))
                Button(
                    onClick = onAction,
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SlateCardLight, contentColor = SlateButtonText)
                ) {
                    Text(actionLabel, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

