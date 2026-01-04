package com.example.faithquiz.ui.view.results

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.faithquiz.ui.navigation.Screen
import com.example.faithquiz.ui.theme.DeepRoyalPurple
import com.example.faithquiz.ui.theme.EtherealGlass
import com.example.faithquiz.ui.theme.GlowingGold
import com.example.faithquiz.ui.theme.Typography
import com.example.faithquiz.ui.theme.Dimensions
import com.example.faithquiz.ui.theme.CorrectAnswerGreen
import com.example.faithquiz.ui.theme.WrongAnswerRed

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
    
    // Animate the chart
    val animatedProgress = remember { Animatable(0f) }
    LaunchedEffect(accuracy) {
        animatedProgress.animateTo(
            targetValue = accuracy,
            animationSpec = tween(durationMillis = 1500, easing = FastOutSlowInEasing)
        )
    }

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
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))
            
            Text(
                text = customTitle?.uppercase() ?: "LEVEL $level COMPLETE",
                style = Typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = GlowingGold,
                letterSpacing = 2.sp
            )
            
            Spacer(modifier = Modifier.height(48.dp))
            
            // DONUT CHART
            Box(contentAlignment = Alignment.Center) {
                Canvas(modifier = Modifier.size(200.dp)) {
                    val strokeWidth = 20.dp.toPx()
                    val radius = size.minDimension / 2 - strokeWidth / 2
                    
                    // Background Ring (Incorrect)
                    drawCircle(
                        color = Color.White.copy(alpha = 0.1f),
                        radius = radius,
                        style = Stroke(width = strokeWidth)
                    )
                    
                    // Foreground Ring (Correct)
                    drawArc(
                        brush = Brush.sweepGradient(
                            listOf(GlowingGold.copy(alpha = 0.6f), GlowingGold)
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
                        style = Typography.displayMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                    Text(
                        text = "ACCURACY",
                        style = Typography.labelSmall,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(48.dp))
            
            // STATS GRID
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                StatCard(
                    label = "TOTAL SCORE",
                    value = "$score/$totalQuestions",
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    label = "TIME SPENT",
                    value = formatTime(timeSpentSeconds.toLong()),
                    modifier = Modifier.weight(1f)
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                val avgTime = if (totalQuestions > 0) timeSpentSeconds / totalQuestions else 0
                StatCard(
                    label = "AVG PACE",
                    value = "${avgTime}s / Q",
                    modifier = Modifier.weight(1f)
                )
                // Placeholder for future expanded stats like "Streak"
                StatCard(
                    label = "RATING",
                    value = getRating(percentage),
                    modifier = Modifier.weight(1f)
                )
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            // ACTION BUTTONS
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Button(
                    onClick = { navController.popBackStack() },
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White.copy(alpha = 0.1f),
                        contentColor = Color.White
                    )
                ) {
                    Text("MENU", fontWeight = FontWeight.Bold)
                }
                
                val nextButtonText = if (percentage >= 60) "NEXT" else "TRY AGAIN"
                val showNextButton = onNext != null || onRetry != null || percentage >= 60 || level > 0 // Always show for standard quiz
                
                if (showNextButton) {
                     Button(
                        onClick = {
                            when {
                                onNext != null && percentage >= 60 -> onNext()
                                onRetry != null && percentage < 60 -> onRetry()
                                else -> {
                                    // Default Level Logic
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
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GlowingGold,
                            contentColor = DeepRoyalPurple
                        )
                    ) {
                        Text(if (percentage >= 60) "NEXT" else "TRY AGAIN", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = EtherealGlass)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = Typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = Color.White
            )
            Text(
                text = label,
                style = Typography.labelSmall,
                color = Color.White.copy(alpha = 0.6f)
            )
        }
    }
}

private fun formatTime(seconds: Long): String {
    val mins = seconds / 60
    val secs = seconds % 60
    return String.format("%02d:%02d", mins, secs)
}

private fun getRating(percentage: Int): String {
    return when {
        percentage == 100 -> "LEGENDARY"
        percentage >= 90 -> "DIVINE"
        percentage >= 80 -> "BLESSED"
        percentage >= 60 -> "FAITHFUL"
        else -> "SEEKER"
    }
}
