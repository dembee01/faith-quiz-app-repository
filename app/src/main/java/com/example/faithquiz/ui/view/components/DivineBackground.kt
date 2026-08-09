package com.example.faithquiz.ui.view.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.withTransform
import com.example.faithquiz.ui.theme.SlateBackgroundBottom
import com.example.faithquiz.ui.theme.SlateBackgroundTop
import com.example.faithquiz.ui.theme.SlateTextSecondary
import kotlin.math.sin
import kotlin.random.Random

@Composable
fun DivineBackground(
    content: @Composable () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "SlateBgAnimations")

    // Gentle rotating aura
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(90000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "AuraRotation"
    )

    val particles = remember { List(24) { SlateParticle() } }

    val time by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing)
        ),
        label = "Time"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(SlateBackgroundTop, SlateBackgroundBottom)
                )
            )
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val centerX = width / 2
            val centerY = height * 0.35f

            // Soft atmospheric sweep
            withTransform({
                rotate(rotation, pivot = Offset(centerX, centerY))
            }) {
                val rayBrush = Brush.sweepGradient(
                    0.0f to Color.Transparent,
                    0.2f to SlateTextSecondary.copy(alpha = 0.04f),
                    0.4f to Color.Transparent,
                    0.7f to SlateTextSecondary.copy(alpha = 0.03f),
                    1.0f to Color.Transparent,
                    center = Offset(centerX, centerY)
                )
                drawCircle(
                    brush = rayBrush,
                    radius = maxOf(width, height) * 1.4f,
                    center = Offset(centerX, centerY)
                )
            }

            // Ambient subtle floating particles
            particles.forEach { particle ->
                val x = (particle.initialX + sin(time * 6.28f + particle.phase) * 30) % width
                val finalX = if (x < 0) x + width else x
                val y = (particle.initialY - (time * height * particle.speed))
                val finalY = if (y < 0) y + height else y
                val alpha = (sin(time * 8f + particle.phase) + 1) / 2 * 0.2f + 0.05f

                drawCircle(
                    color = SlateTextSecondary.copy(alpha = alpha),
                    radius = particle.size,
                    center = Offset(finalX, finalY)
                )
            }
        }

        // Screen content
        content()
    }
}

private data class SlateParticle(
    val initialX: Float = Random.nextFloat() * 1080f,
    val initialY: Float = Random.nextFloat() * 2400f,
    val size: Float = Random.nextFloat() * 3f + 1f,
    val speed: Float = Random.nextFloat() * 0.3f + 0.05f,
    val phase: Float = Random.nextFloat() * 6.28f
)

