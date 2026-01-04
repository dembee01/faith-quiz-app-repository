package com.example.faithquiz.ui.view.results

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.QueryBuilder
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
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
import kotlinx.coroutines.launch

@Composable
fun GrandCompletionScreen(
    navController: NavController
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    
    // Stats State
    val totalTimeSeconds = ProgressDataStore.observeTotalTimeSpent(context).collectAsState(initial = 0L)
    val totalQuestions = ProgressDataStore.observeTotalQuestionsAnswered(context).collectAsState(initial = 0)
    val mistakes = ProgressDataStore.observeMistakesDetailed(context).collectAsState(initial = emptyList())
    
    // Animations
    var showContent by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        AudioHelper.playLevelComplete() // Or a more grand sound if available
        delay(300)
        showContent = true
    }

    DivineBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))
            
            // Header Content
            AnimatedVisibility(
                visible = showContent,
                enter = fadeIn() + expandVertically()
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.EmojiEvents,
                        contentDescription = "Trophy",
                        modifier = Modifier.size(80.dp),
                        tint = GlowingGold
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "THE JOURNEY COMPLETE",
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    
                    Text(
                        text = "Biblical Master",
                        style = MaterialTheme.typography.titleLarge,
                        color = GlowingGold,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Stats Grid
            AnimatedVisibility(
                visible = showContent,
                enter = fadeIn(animationSpec = tween(durationMillis = 800, delayMillis = 300)) + slideInVertically(initialOffsetY = { 50 })
            ) {
                Column {
                    Text(
                        text = "Your Covenant Stats",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Total Time
                        StatCard(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Default.QueryBuilder,
                            label = "Time Spent",
                            value = formatTime(totalTimeSeconds.value)
                        )
                        
                        // Questions Answered
                        StatCard(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Default.CheckCircle,
                            label = "Questions",
                            value = "${totalQuestions.value}"
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Accuracy
                        val correct = (totalQuestions.value - mistakes.value.size).coerceAtLeast(0)
                        val accuracy = if (totalQuestions.value > 0) (correct.toFloat() / totalQuestions.value * 100).toInt() else 0
                        
                        StatCard(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Default.Star,
                            label = "Accuracy",
                            value = "$accuracy%"
                        )
                        
                        // Faithfulness (Streak/Completion)
                        StatCard(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Default.History,
                            label = "Levels",
                            value = "30/30"
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(48.dp))
            
            // Encouragement
            AnimatedVisibility(
                visible = showContent,
                enter = fadeIn(animationSpec = tween(durationMillis = 1000, delayMillis = 600))
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = DeepRoyalPurple.copy(alpha = 0.6f)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "\"Well done, good and faithful servant!\"",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.White,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "You have traversed the entire Covenant Journey. Your dedication to learning the Word is inspiring.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(48.dp))

            // Action Button
            AnimatedVisibility(
                visible = showContent,
                enter = fadeIn(animationSpec = tween(durationMillis = 500, delayMillis = 1000)) + expandVertically()
            ) {
                Button(
                    onClick = {
                        AudioHelper.playSelect()
                        // Pop back to main menu, clearing everything
                        navController.navigate(Screen.MainMenu.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GlowingGold,
                        contentColor = DeepRoyalPurple
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.ArrowForward, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "RETURN TO MENU",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun StatCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    value: String
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.1f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = GlowingGold,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.7f)
            )
        }
    }
}

private fun formatTime(seconds: Long): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    if (h > 0) return String.format("%d:%02d:%02d", h, m, s)
    return String.format("%02d:%02d", m, s)
}
