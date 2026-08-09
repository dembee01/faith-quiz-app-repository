package com.example.faithquiz.ui.view.results

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.QueryBuilder
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.faithquiz.data.store.ProgressDataStore
import com.example.faithquiz.ui.navigation.Screen
import com.example.faithquiz.ui.theme.*
import com.example.faithquiz.ui.view.components.DivineBackground
import com.example.faithquiz.util.AudioHelper
import kotlinx.coroutines.delay
import java.util.Locale

@Composable
fun GrandCompletionScreen(
    navController: NavController
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    val totalTimeSeconds = ProgressDataStore.observeTotalTimeSpent(context).collectAsState(initial = 0L)
    val totalQuestions = ProgressDataStore.observeTotalQuestionsAnswered(context).collectAsState(initial = 0)
    val mistakes = ProgressDataStore.observeMistakesDetailed(context).collectAsState(initial = emptyList())

    var showContent by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        AudioHelper.playLevelComplete()
        delay(250)
        showContent = true
    }

    DivineBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 22.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            AnimatedVisibility(
                visible = showContent,
                enter = fadeIn() + expandVertically()
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.EmojiEvents,
                        contentDescription = "Trophy",
                        modifier = Modifier.size(80.dp),
                        tint = GoldAccent
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "THE JOURNEY COMPLETE",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = SlateTextPrimary,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = "Biblical Master",
                        fontSize = 18.sp,
                        color = GoldAccent,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            AnimatedVisibility(
                visible = showContent,
                enter = fadeIn(animationSpec = tween(durationMillis = 600, delayMillis = 200)) + slideInVertically(initialOffsetY = { 40 })
            ) {
                Column {
                    Text(
                        text = "Your Covenant Stats",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = SlateTextSecondary,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        GrandStatCard(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Default.QueryBuilder,
                            label = "Time Spent",
                            value = formatTime(totalTimeSeconds.value)
                        )

                        GrandStatCard(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Default.CheckCircle,
                            label = "Questions",
                            value = "${totalQuestions.value}"
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        val correct = (totalQuestions.value - mistakes.value.size).coerceAtLeast(0)
                        val accuracy = if (totalQuestions.value > 0) (correct.toFloat() / totalQuestions.value * 100).toInt() else 0

                        GrandStatCard(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Default.Star,
                            label = "Accuracy",
                            value = "$accuracy%"
                        )

                        GrandStatCard(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Default.History,
                            label = "Levels",
                            value = "30/30"
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            AnimatedVisibility(
                visible = showContent,
                enter = fadeIn(animationSpec = tween(durationMillis = 800, delayMillis = 400))
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = SlateSurface),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(22.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "\"Well done, good and faithful servant!\"",
                            fontSize = 16.sp,
                            color = SlateTextPrimary,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "You have traversed the entire Covenant Journey. Your dedication to learning the Word is inspiring.",
                            fontSize = 14.sp,
                            color = SlateTextSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(36.dp))

            AnimatedVisibility(
                visible = showContent,
                enter = fadeIn(animationSpec = tween(durationMillis = 400, delayMillis = 600)) + expandVertically()
            ) {
                Button(
                    onClick = {
                        AudioHelper.playSelect()
                        navController.navigate(Screen.MainMenu.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SlateCardLight,
                        contentColor = SlateButtonText
                    ),
                    shape = RoundedCornerShape(28.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = SlateButtonText)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "RETURN TO MENU",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = SlateButtonText
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun GrandStatCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    value: String
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = SlateSurface),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = GoldAccent,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                fontSize = 18.sp,
                color = SlateTextPrimary,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = label,
                fontSize = 12.sp,
                color = SlateTextSecondary
            )
        }
    }
}

private fun formatTime(seconds: Long): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    if (h > 0) return String.format(Locale.getDefault(), "%d:%02d:%02d", h, m, s)
    return String.format(Locale.getDefault(), "%02d:%02d", m, s)
}

