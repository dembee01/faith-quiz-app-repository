package com.example.faithquiz.ui.view.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import com.example.faithquiz.ui.theme.DeepRoyalPurple
import com.example.faithquiz.ui.theme.EtherealGlass
import com.example.faithquiz.ui.theme.GlowingGold
import com.example.faithquiz.ui.theme.Typography

/**
 * Divine Timer HUD - A floating glassmorphism timer overlay for the quiz screen.
 * Shows current question time (circular animated) and total level time.
 */
@Composable
fun DivineTimerHUD(
    questionTimeSeconds: Long,
    totalTimeSeconds: Long,
    modifier: Modifier = Modifier
) {
    // Pulse animation for the glow effect
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )
    
    // Calculate progress (loops every 60 seconds for visual effect)
    val progress = (questionTimeSeconds % 60) / 60f
    
    // Glassmorphism Container
    Box(
        modifier = modifier
            .padding(16.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(EtherealGlass)
            .padding(12.dp)
            .wrapContentSize(),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Question Timer (Circular with Canvas)
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(60.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeWidth = 5.dp.toPx()
                    val radius = size.minDimension / 2 - strokeWidth / 2
                    
                    // Background track circle
                    drawCircle(
                        color = DeepRoyalPurple.copy(alpha = 0.3f),
                        radius = radius,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                    
                    // Glow effect (pulsating)
                    drawCircle(
                        color = GlowingGold.copy(alpha = glowAlpha * 0.5f),
                        radius = radius + 4f,
                        style = Stroke(width = strokeWidth + 6f, cap = StrokeCap.Round)
                    )
                    
                    // Main progress arc
                    drawArc(
                        brush = Brush.sweepGradient(
                            listOf(GlowingGold.copy(alpha = 0.5f), GlowingGold)
                        ),
                        startAngle = -90f,
                        sweepAngle = 360 * progress,
                        useCenter = false,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }

                Text(
                    text = "${questionTimeSeconds}s",
                    style = Typography.labelLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = GlowingGold
                    )
                )
            }

            // Total Time Display
            Column(horizontalAlignment = Alignment.Start) {
                Text(
                    text = "TOTAL TIME",
                    style = Typography.labelSmall.copy(color = Color.White.copy(alpha = 0.7f)),
                    fontSize = 10.sp
                )
                Text(
                    text = formatTime(totalTimeSeconds),
                    style = Typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    ),
                    fontSize = 20.sp
                )
            }
        }
    }
}

private fun formatTime(seconds: Long): String {
    val mins = seconds / 60
    val secs = seconds % 60
    return String.format("%02d:%02d", mins, secs)
}
