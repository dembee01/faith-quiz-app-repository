package com.example.faithquiz.ui.view.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.faithquiz.ui.theme.*
import java.util.Locale

/**
 * Divine Timer HUD - Floating Slate Indigo timer overlay for quiz screens.
 * Displays current question time and total level time.
 */
@Composable
fun DivineTimerHUD(
    questionTimeSeconds: Long,
    totalTimeSeconds: Long,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")

    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    val progress = (questionTimeSeconds % 60) / 60f

    Box(
        modifier = modifier
            .padding(12.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(SlateSurface)
            .border(1.dp, SlateCardBorder, RoundedCornerShape(24.dp))
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .wrapContentSize(),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(54.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeWidth = 4.dp.toPx()
                    val radius = size.minDimension / 2 - strokeWidth / 2

                    drawCircle(
                        color = SlateSurfaceVariant,
                        radius = radius,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )

                    drawCircle(
                        color = GoldAccent.copy(alpha = glowAlpha * 0.4f),
                        radius = radius + 3f,
                        style = Stroke(width = strokeWidth + 4f, cap = StrokeCap.Round)
                    )

                    drawArc(
                        brush = Brush.sweepGradient(
                            listOf(GoldAccent.copy(alpha = 0.5f), GoldAccent)
                        ),
                        startAngle = -90f,
                        sweepAngle = 360 * progress,
                        useCenter = false,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }

                Text(
                    text = "${questionTimeSeconds}s",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = GoldAccent
                )
            }

            Column(horizontalAlignment = Alignment.Start) {
                Text(
                    text = "TOTAL TIME",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = SlateTextSecondary
                )
                Text(
                    text = formatTime(totalTimeSeconds),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = SlateTextPrimary
                )
            }
        }
    }
}

private fun formatTime(seconds: Long): String {
    val mins = seconds / 60
    val secs = seconds % 60
    return String.format(Locale.getDefault(), "%02d:%02d", mins, secs)
}

