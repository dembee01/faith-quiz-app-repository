package com.example.faithquiz.ui.view.common

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random
import kotlin.math.sin

@Composable
fun ConfettiAnimation(
    isVisible: Boolean,
    onAnimationComplete: () -> Unit = {}
) {
    var particles by remember { mutableStateOf(List(50) { ConfettiParticle() }) }
    
    LaunchedEffect(isVisible) {
        if (isVisible) {
            particles = List(50) { ConfettiParticle() }
            delay(2000) // Animation duration
            onAnimationComplete()
        }
    }
    
    if (isVisible) {
        Box(modifier = Modifier.fillMaxSize()) {
            particles.forEach { particle ->
                ConfettiParticle(
                    particle = particle,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
fun ConfettiParticle(
    particle: ConfettiParticle,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "confetti")
    
    val x by infiniteTransition.animateFloat(
        initialValue = particle.initialX,
        targetValue = particle.initialX + particle.driftX,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "confetti_x"
    )
    
    val y by infiniteTransition.animateFloat(
        initialValue = particle.initialY,
        targetValue = particle.initialY + 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "confetti_y"
    )
    
    // Rotation animation removed as it's not being used
    
    val scale by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "confetti_scale"
    )
    
    Canvas(modifier = modifier) {
        drawCircle(
            color = particle.color,
            radius = 4.dp.toPx() * scale,
            center = Offset(x, y)
        )
    }
}

data class ConfettiParticle(
    val initialX: Float = Random.nextFloat() * 1000f,
    val initialY: Float = -50f,
    val driftX: Float = Random.nextFloat() * 200f - 100f,
    val color: Color = listOf(
        Color(0xFF4CAF50), // Green
        Color(0xFF2196F3), // Blue
        Color(0xFFFFC107), // Yellow
        Color(0xFFE91E63), // Pink
        Color(0xFF9C27B0), // Purple
        Color(0xFFFF5722)  // Orange
    ).random()
)

@Composable
fun ShakeAnimation(
    isShaking: Boolean,
    content: @Composable () -> Unit
) {
    val shakeAnimation by animateFloatAsState(
        targetValue = if (isShaking) 1f else 0f,
        animationSpec = tween(500, easing = LinearEasing),
        label = "shake"
    )
    
    val shakeOffset by remember {
        derivedStateOf {
            if (shakeAnimation > 0) {
                val shake = sin(shakeAnimation * 20) * 10
                shake * shakeAnimation
            } else 0f
        }
    }
    
    Box(
        modifier = Modifier.offset(x = shakeOffset.dp)
    ) {
        content()
    }
}

@Composable
fun RedFlashAnimation(
    isFlashing: Boolean,
    content: @Composable () -> Unit
) {
    val flashAnimation by animateFloatAsState(
        targetValue = if (isFlashing) 1f else 0f,
        animationSpec = tween(300, easing = LinearEasing),
        label = "flash"
    )
    
    Box(
        modifier = Modifier
            .background(
                if (flashAnimation > 0) {
                    Color.Red.copy(alpha = flashAnimation * 0.3f)
                } else {
                    Color.Transparent
                }
            )
    ) {
        content()
    }
}

@Composable
fun CorrectAnswerAnimation(
    isVisible: Boolean,
    onAnimationComplete: () -> Unit = {}
) {
    LaunchedEffect(isVisible) {
        if (isVisible) {
            // Play success sound (placeholder)
            // In a real app, you would use MediaPlayer or SoundPool
            delay(1000)
            onAnimationComplete()
        }
    }
    
    if (isVisible) {
        ConfettiAnimation(
            isVisible = true,
            onAnimationComplete = { /* Confetti handles its own completion */ }
        )
    }
}

@Composable
fun IncorrectAnswerAnimation(
    isVisible: Boolean,
    onAnimationComplete: () -> Unit = {}
) {
    LaunchedEffect(isVisible) {
        if (isVisible) {
            // Play error sound (placeholder)
            // In a real app, you would use MediaPlayer or SoundPool
            delay(500)
            onAnimationComplete()
        }
    }
}
