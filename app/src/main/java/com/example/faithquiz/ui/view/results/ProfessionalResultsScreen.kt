package com.example.faithquiz.ui.view.results

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.faithquiz.ui.navigation.Screen
import com.example.faithquiz.ui.theme.*
import com.example.faithquiz.ui.view.components.DivineBackground
import java.util.Locale

@Composable
fun ProfessionalResultsScreen(
    navController: NavController,
    score: Int,
    totalQuestions: Int,
    level: Int,
    timeSpentSeconds: Int,
    customTitle: String? = null,
    onRetry: (() -> Unit)? = null,
    onNext: (() -> Unit)? = null
) {
    val percentage = if (totalQuestions > 0) (score.toFloat() / totalQuestions * 100).toInt() else 0
    val accuracy = score.toFloat() / totalQuestions.coerceAtLeast(1)

    val animatedProgress = remember { Animatable(0f) }
    LaunchedEffect(accuracy) {
        animatedProgress.animateTo(
            targetValue = accuracy,
            animationSpec = tween(durationMillis = 1400, easing = FastOutSlowInEasing)
        )
    }

    DivineBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = customTitle?.uppercase() ?: "LEVEL $level COMPLETE",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = SlateTextPrimary,
                letterSpacing = 1.5.sp
            )

            Spacer(modifier = Modifier.height(36.dp))

            // DONUT CHART
            Box(contentAlignment = Alignment.Center) {
                Canvas(modifier = Modifier.size(190.dp)) {
                    val strokeWidth = 18.dp.toPx()
                    val radius = size.minDimension / 2 - strokeWidth / 2

                    drawCircle(
                        color = SlateSurfaceVariant,
                        radius = radius,
                        style = Stroke(width = strokeWidth)
                    )

                    drawArc(
                        brush = Brush.sweepGradient(
                            listOf(GoldAccent.copy(alpha = 0.7f), GoldAccent)
                        ),
                        startAngle = -90f,
                        sweepAngle = 360 * animatedProgress.value,
                        useCenter = false,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$percentage%",
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold,
                        color = SlateTextPrimary
                    )
                    Text(
                        text = "ACCURACY",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = SlateTextSecondary,
                        letterSpacing = 1.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            // STATS GRID
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                SlateStatCard(
                    label = "TOTAL SCORE",
                    value = "$score/$totalQuestions",
                    modifier = Modifier.weight(1f)
                )
                SlateStatCard(
                    label = "TIME SPENT",
                    value = formatTime(timeSpentSeconds.toLong()),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                val avgTime = if (totalQuestions > 0) timeSpentSeconds / totalQuestions else 0
                SlateStatCard(
                    label = "AVG PACE",
                    value = "${avgTime}s / Q",
                    modifier = Modifier.weight(1f)
                )
                SlateStatCard(
                    label = "RATING",
                    value = getRating(percentage),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // ACTION BUTTONS
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Button(
                    onClick = { navController.popBackStack() },
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SlateSurfaceVariant,
                        contentColor = SlateTextPrimary
                    )
                ) {
                    Text("MENU", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }

                val showNextButton = onNext != null || onRetry != null || percentage >= 60 || level > 0

                if (showNextButton) {
                    Button(
                        onClick = {
                            when {
                                onNext != null && percentage >= 60 -> onNext()
                                onRetry != null && percentage < 60 -> onRetry()
                                else -> {
                                    if (percentage >= 60) {
                                        if (level == 30) {
                                            navController.navigate(Screen.GrandCompletion.route) {
                                                popUpTo(Screen.MainMenu.route) { inclusive = false }
                                            }
                                        } else {
                                            navController.navigate(Screen.Quiz.createRoute(level + 1)) {
                                                popUpTo(Screen.LevelSelect.route)
                                            }
                                        }
                                    } else {
                                        navController.navigate(Screen.Quiz.createRoute(level)) {
                                            popUpTo(Screen.LevelSelect.route)
                                        }
                                    }
                                }
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                        shape = RoundedCornerShape(28.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SlateCardLight,
                            contentColor = SlateButtonText
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                    ) {
                        Text(
                            text = if (percentage >= 60) "NEXT" else "TRY AGAIN",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun SlateStatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SlateSurface)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = SlateTextPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                color = SlateTextSecondary,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

private fun formatTime(seconds: Long): String {
    val mins = seconds / 60
    val secs = seconds % 60
    return String.format(Locale.getDefault(), "%02d:%02d", mins, secs)
}

private fun getRating(percentage: Int): String {
    return when {
        percentage == 100 -> "LEGENDARY"
        percentage >= 90 -> "EXCELLENT"
        percentage >= 80 -> "BLESSED"
        percentage >= 60 -> "PASSED"
        else -> "SEEKER"
    }
}

