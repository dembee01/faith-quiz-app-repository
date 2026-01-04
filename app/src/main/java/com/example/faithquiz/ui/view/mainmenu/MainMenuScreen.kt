package com.example.faithquiz.ui.view.mainmenu

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.faithquiz.R
import com.example.faithquiz.data.store.ProgressDataStore
import com.example.faithquiz.ui.navigation.Screen
import com.example.faithquiz.ui.theme.*
import com.example.faithquiz.ui.view.components.DivineBackground
import com.example.faithquiz.util.AudioHelper

import kotlinx.coroutines.delay

@Composable
fun MainMenuScreen(
    navController: NavController
) {
    val context = LocalContext.current
    val totalAnswered by ProgressDataStore.observeTotalQuestionsAnswered(context).collectAsState(initial = 0)
    val highScore by ProgressDataStore.observeHighScore(context).collectAsState(initial = 0)
    val lastCompleted by ProgressDataStore.observeLastCompletedLevel(context).collectAsState(initial = 1)
    val devotionStreak by ProgressDataStore.observeDevotionStreak(context).collectAsState(initial = 0)

    val scrollState = rememberScrollState()

    DivineBackground {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(Dimensions.screenPadding)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            // Animated Divine Title
            DivineAnimateTitle()

            Spacer(modifier = Modifier.height(32.dp))

            // Stats Dashboard
            DivineStatsDashboard(
                highScore = highScore,
                lastLevel = lastCompleted,
                totalQuestions = totalAnswered,
                streak = devotionStreak
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Menu Items with Staggered Entry
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                DivineMenuCard(
                    title = "The Covenant Journey",
                    subtitle = "Your Biblical Adventure Map",
                    icon = Icons.Default.Map,
                    delayMs = 100
                ) {
                    AudioHelper.playSelect()
                    navController.navigate(Screen.Journey.route)
                }

                DivineMenuCard(
                    title = "Adaptive Levels",
                    subtitle = "Classic Level Selection",
                    icon = Icons.Default.List,
                    delayMs = 150
                ) {
                    AudioHelper.playSelect()
                    navController.navigate(Screen.LevelSelect.route)
                }



                DivineMenuCard(
                    title = "Review Wisdom",
                    subtitle = "Study Your Past Answers",
                    icon = Icons.Default.Bookmarks,
                    delayMs = 200
                ) {
                    AudioHelper.playSelect()
                    navController.navigate(Screen.Review.route)
                }


                DivineMenuCard(
                    title = "Topic Scrolls",
                    subtitle = "Specific Books & Themes",
                    icon = Icons.Default.Category,
                    delayMs = 300
                ) {
                    AudioHelper.playSelect()
                    navController.navigate(Screen.TopicPacks.route)
                }


                DivineMenuCard(
                    title = "Leaderboard",
                    subtitle = "See Faithful Servants",
                    icon = Icons.Default.Leaderboard,
                    delayMs = 400
                ) {
                    AudioHelper.playSelect()
                    navController.navigate(Screen.Leaderboard.route)
                }


                DivineMenuCard(
                    title = "Settings",
                    subtitle = "Configure Your Experience",
                    icon = Icons.Default.Settings,
                    delayMs = 500
                ) {
                    AudioHelper.playSelect()
                    navController.navigate(Screen.Settings.route)
                }

            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun DivineAnimateTitle() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        // "FAITH QUIZ"
        TypewriterText(
            baseText = "FAITH QUIZ",
            style = MaterialTheme.typography.displayMedium.copy(
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                color = GlowingGold
            ),
            typingDelay = 150,
            backspaceDelay = 100,
            waitAfterType = 3000,
            waitAfterBackspace = 1000
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // "Test Your Bible Knowledge"
        TypewriterText(
            baseText = "Test Your Bible Knowledge",
            style = MaterialTheme.typography.titleMedium.copy(
                fontFamily = FontFamily.Serif,
                color = Color.White.copy(alpha = 0.8f)
            ),
            typingDelay = 50,
            backspaceDelay = 30,
            waitAfterType = 3000,
            waitAfterBackspace = 1000
        )
    }
}

@Composable
fun TypewriterText(
    baseText: String,
    style: androidx.compose.ui.text.TextStyle,
    modifier: Modifier = Modifier,
    typingDelay: Long = 100,
    backspaceDelay: Long = 50,
    waitAfterType: Long = 2000,
    waitAfterBackspace: Long = 500
) {
    var displayedText by remember { mutableStateOf("") }

    LaunchedEffect(baseText) {
        while (true) {
            // Type in
            for (i in 1..baseText.length) {
                displayedText = baseText.substring(0, i)
                delay(typingDelay)
            }
            delay(waitAfterType)

            // Type out
            for (i in baseText.length downTo 0) {
                displayedText = baseText.substring(0, i)
                delay(backspaceDelay)
            }
            delay(waitAfterBackspace)
        }
    }

    Box(contentAlignment = Alignment.Center) {
        // Invisible text to maintain layout size
        Text(
            text = baseText,
            style = style,
            color = Color.Transparent,
            textAlign = TextAlign.Center
        )
        // Visible animated text
        Text(
            text = displayedText,
            style = style,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun DivineStatsDashboard(
    highScore: Int,
    lastLevel: Int,
    totalQuestions: Int,
    streak: Int
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, GlowingGold.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
            .shadow(12.dp, RoundedCornerShape(16.dp), ambientColor = GlowingGold, spotColor = GlowingGold),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = EtherealGlass
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Star, contentDescription = null, tint = GlowingGold)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Your Progress",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontFamily = FontFamily.Serif,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                DivineStatItem(value = highScore.toString(), label = "High Score", icon = Icons.Default.EmojiEvents)
                DivineStatItem(value = lastLevel.toString(), label = "Level", icon = Icons.Default.Flag)
                DivineStatItem(value = totalQuestions.toString(), label = "Questions", icon = Icons.AutoMirrored.Filled.Help)
                DivineStatItem(value = streak.toString(), label = "Streak", icon = Icons.Default.Whatshot)
            }
        }
    }
}

@Composable
fun DivineStatItem(value: String, label: String, icon: ImageVector) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = GlowingGold.copy(alpha = 0.8f),
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(
                color = Color.White.copy(alpha = 0.6f)
            )
        )
    }
}

@Composable
fun DivineMenuCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    delayMs: Int = 0,
    onClick: () -> Unit
) {
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(delayMs.toLong())
        isVisible = true
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically(initialOffsetY = { 50 }, animationSpec = tween(500)) + 
                fadeIn(animationSpec = tween(500))
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .clickable { onClick() }
                .border(1.dp, GlowingGold.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = EtherealGlass
            ),
            elevation = CardDefaults.cardElevation(0.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(GlowingGold.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = GlowingGold,
                        modifier = Modifier.size(28.dp)
                    )
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    )
                }
                
                Spacer(modifier = Modifier.weight(1f))
                
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.5f)
                )
            }
        }
    }
}