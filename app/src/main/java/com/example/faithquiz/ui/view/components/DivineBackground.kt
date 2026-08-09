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
import com.example.faithquiz.ui.theme.DeepRoyalPurple
import com.example.faithquiz.ui.theme.GlowingGold
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

@Composable
fun DivineBackground(
    content: @Composable () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "DivineBgAnimations")
    
    // Rotating God Rays
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(60000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "RayRotation"
    )

    // Particles State
    // We generated 20 random particles
    val particles = remember { List(30) { DivineParticle() } }
    
    // Animate particles (simple time based offset)
    val time by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(10000, easing = LinearEasing)
        ),
        label = "Time"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            // Base Gradient
            .background(
                Brush.verticalGradient(
                    colors = listOf(DeepRoyalPurple, Color.Black)
                )
            )
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val centerX = width / 2
            val centerY = height * 0.3f // Rays originate from top-centerish

            // Draw God Rays (Subtle)
            withTransform({
                rotate(rotation, pivot = Offset(centerX, centerY))
            }) {
                val rayBrush = Brush.sweepGradient(
                    0.0f to Color.Transparent,
                    0.1f to GlowingGold.copy(alpha = 0.05f),
                    0.2f to Color.Transparent,
                    0.3f to GlowingGold.copy(alpha = 0.03f),
                    0.5f to Color.Transparent,
                    0.6f to GlowingGold.copy(alpha = 0.05f),
                    0.8f to Color.Transparent,
                    1.0f to Color.Transparent,
                    center = Offset(centerX, centerY)
                )
                drawCircle(
                    brush = rayBrush,
                    radius = maxOf(width, height) * 1.5f,
                    center = Offset(centerX, centerY)
                )
            }
            
            // Draw Particles
            particles.forEach { particle ->
                val x = (particle.initialX + sin(time * 6.28f + particle.phase) * 50) % width
                // Make positive modulo
                val finalX = if (x < 0) x + width else x
                
                val y = (particle.initialY - (time * height * particle.speed)) 
                val finalY = if (y < 0) y + height else y
                
                val alpha = (sin(time * 10f + particle.phase) + 1) / 2 * 0.3f + 0.1f // Pulse alpha

                drawCircle(
                    color = GlowingGold.copy(alpha = alpha),
                    radius = particle.size,
                    center = Offset(finalX, finalY)
                )
            }
        }
        
        // Content Overlay
        content()
    }
}

private data class DivineParticle(
    val initialX: Float = Random.nextFloat() * 1080f, // Assume max width, mod will handle
    val initialY: Float = Random.nextFloat() * 2400f,
    val size: Float = Random.nextFloat() * 4f + 1f,
    val speed: Float = Random.nextFloat() * 0.5f + 0.1f,
    val phase: Float = Random.nextFloat() * 6.28f
)
