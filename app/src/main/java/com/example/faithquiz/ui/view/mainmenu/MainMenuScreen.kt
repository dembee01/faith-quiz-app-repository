package com.example.faithquiz.ui.view.mainmenu

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
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
    val reduceMotion by ProgressDataStore.observeReduceMotion(context).collectAsState(initial = false)

    val scrollState = rememberScrollState()

    DivineBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // Title & Subtitle Header
            SlateHeaderTitle(reduceMotion = reduceMotion)

            Spacer(modifier = Modifier.height(24.dp))

            // Stats Dashboard
            SlateStatsDashboard(
                highScore = highScore,
                lastLevel = lastCompleted,
                totalQuestions = totalAnswered,
                streak = devotionStreak
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Menu Items List
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                SlateMenuCard(
                    title = "Daily Challenge",
                    subtitle = "One new question every day",
                    icon = Icons.Default.CalendarMonth,
                    delayMs = 50,
                    reduceMotion = reduceMotion
                ) {
                    AudioHelper.playSelect()
                    navController.navigate(Screen.DailyChallenge.route)
                }

                SlateMenuCard(
                    title = "The Covenant Journey",
                    subtitle = "Your Biblical Adventure Map",
                    icon = Icons.Default.Map,
                    delayMs = 100,
                    reduceMotion = reduceMotion
                ) {
                    AudioHelper.playSelect()
                    navController.navigate(Screen.Journey.route)
                }

                SlateMenuCard(
                    title = "Adaptive Levels",
                    subtitle = "30 Progressive Quiz Levels",
                    icon = Icons.AutoMirrored.Filled.List,
                    delayMs = 150,
                    reduceMotion = reduceMotion
                ) {
                    AudioHelper.playSelect()
                    navController.navigate(Screen.LevelSelect.route)
                }

                SlateMenuCard(
                    title = "Review Wisdom",
                    subtitle = "Study Past & Missed Questions",
                    icon = Icons.Default.Bookmarks,
                    delayMs = 200,
                    reduceMotion = reduceMotion
                ) {
                    AudioHelper.playSelect()
                    navController.navigate(Screen.Review.route)
                }

                SlateMenuCard(
                    title = "Topic Scrolls",
                    subtitle = "Specific Books & Bible Themes",
                    icon = Icons.Default.Category,
                    delayMs = 250,
                    reduceMotion = reduceMotion
                ) {
                    AudioHelper.playSelect()
                    navController.navigate(Screen.TopicPacks.route)
                }

                SlateMenuCard(
                    title = "Leaderboard",
                    subtitle = "See Top Scores & Achievements",
                    icon = Icons.Default.Leaderboard,
                    delayMs = 300,
                    reduceMotion = reduceMotion
                ) {
                    AudioHelper.playSelect()
                    navController.navigate(Screen.Leaderboard.route)
                }

                SlateMenuCard(
                    title = "Settings",
                    subtitle = "Sound, Theme & Accessibility",
                    icon = Icons.Default.Settings,
                    delayMs = 350,
                    reduceMotion = reduceMotion
                ) {
                    AudioHelper.playSelect()
                    navController.navigate(Screen.Settings.route)
                }
            }

            Spacer(modifier = Modifier.height(36.dp))
        }
    }
}

@Composable
fun SlateHeaderTitle(reduceMotion: Boolean = false) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = stringResource(R.string.faith_quiz).uppercase(),
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = SlateTextPrimary,
            letterSpacing = 2.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = stringResource(R.string.test_your_bible_knowledge),
            fontSize = 15.sp,
            color = SlateTextSecondary,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun SlateStatsDashboard(
    highScore: Int,
    lastLevel: Int,
    totalQuestions: Int,
    streak: Int
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, SlateCardBorder, RoundedCornerShape(20.dp))
            .shadow(6.dp, RoundedCornerShape(20.dp), spotColor = Color.Black),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SlateSurface)
    ) {
        Column(
            modifier = Modifier.padding(18.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = GoldAccent,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Your Progress",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = SlateTextPrimary
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SlateStatItem(value = highScore.toString(), label = "High Score", icon = Icons.Default.EmojiEvents)
                SlateStatItem(value = lastLevel.toString(), label = "Level", icon = Icons.Default.Flag)
                SlateStatItem(value = totalQuestions.toString(), label = "Questions", icon = Icons.AutoMirrored.Filled.Help)
                SlateStatItem(value = streak.toString(), label = "Streak", icon = Icons.Default.Whatshot)
            }
        }
    }
}

@Composable
fun SlateStatItem(value: String, label: String, icon: ImageVector) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(SlateSurfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = GoldAccent,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = value,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = SlateTextPrimary
        )
        Text(
            text = label,
            fontSize = 11.sp,
            color = SlateTextMuted
        )
    }
}

@Composable
fun SlateMenuCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    delayMs: Int = 0,
    reduceMotion: Boolean = false,
    onClick: () -> Unit
) {
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (!reduceMotion) delay(delayMs.toLong())
        isVisible = true
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = if (reduceMotion) EnterTransition.None else {
            slideInVertically(initialOffsetY = { 40 }, animationSpec = tween(400)) +
                fadeIn(animationSpec = tween(400))
        }
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(76.dp)
                .clickable(role = Role.Button, onClick = onClick),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = SlateCardLight),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(SlateBackgroundTop.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = SlateButtonText,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = SlateButtonText
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        fontSize = 13.sp,
                        color = SlateButtonText.copy(alpha = 0.7f)
                    )
                }

                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = SlateButtonText.copy(alpha = 0.5f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

