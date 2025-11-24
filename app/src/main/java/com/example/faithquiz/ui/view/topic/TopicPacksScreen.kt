package com.example.faithquiz.ui.view.topic

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.faithquiz.R
import com.example.faithquiz.ui.navigation.Screen
import com.example.faithquiz.ui.theme.Dimensions
import com.example.faithquiz.data.TopicQuestionBank
import com.example.faithquiz.data.store.ProgressDataStore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopicPacksScreen(navController: NavController) {
    val context = LocalContext.current
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    val tabs = listOf(
        R.string.gospels,
        R.string.prophets,
        R.string.parables
    )
    
    // Get topic scores from DataStore
    val gospelsScore by ProgressDataStore.observeTopicScore(context, "gospels").collectAsState(initial = 0)
    val prophetsScore by ProgressDataStore.observeTopicScore(context, "prophets").collectAsState(initial = 0)
    val parablesScore by ProgressDataStore.observeTopicScore(context, "parables").collectAsState(initial = 0)
    
    val currentScore = when (selectedTab) {
        0 -> gospelsScore
        1 -> prophetsScore
        2 -> parablesScore
        else -> 0
    }
    
    val topicType = when (selectedTab) {
        0 -> TopicQuestionBank.TopicType.GOSPELS
        1 -> TopicQuestionBank.TopicType.PROPHETS
        2 -> TopicQuestionBank.TopicType.PARABLES
        else -> TopicQuestionBank.TopicType.GOSPELS
    }
    
    val achievementLevel = TopicQuestionBank.getAchievementLevel(currentScore)
    val achievementTitle = TopicQuestionBank.getAchievementTitle(topicType, achievementLevel)
    val encouragementMessage = TopicQuestionBank.getEncouragementMessage(topicType, currentScore)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.secondary
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(Dimensions.screenMargin)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.back_description),
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
                Text(
                    text = stringResource(R.string.topic_packs),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(modifier = Modifier.width(Dimensions.iconSize))
            }

            Spacer(modifier = Modifier.height(Dimensions.spaceLarge))

            // Tabs
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.15f),
                contentColor = MaterialTheme.colorScheme.onPrimary,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = MaterialTheme.colorScheme.onPrimary,
                        height = 3.dp
                    )
                },
                divider = {}
            ) {
                tabs.forEachIndexed { index, resId ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        selectedContentColor = MaterialTheme.colorScheme.onPrimary,
                        unselectedContentColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f),
                        text = {
                            Text(
                                text = stringResource(id = resId),
                                style = MaterialTheme.typography.titleMedium,
                                color = if (selectedTab == index) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(Dimensions.spaceLarge))

            // Content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                // Achievement Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(Dimensions.cornerRadiusLarge),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(Dimensions.paddingLarge)
                    ) {
                        Text(
                            text = "Your Achievement",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(Dimensions.spaceMedium))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = achievementTitle,
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                    color = when (achievementLevel) {
                                        TopicQuestionBank.AchievementLevel.SUPREME -> Color(0xFFFFD700) // Gold
                                        TopicQuestionBank.AchievementLevel.SPECIAL -> Color(0xFFC0C0C0) // Silver
                                        TopicQuestionBank.AchievementLevel.ENCOURAGED -> MaterialTheme.colorScheme.primary
                                        TopicQuestionBank.AchievementLevel.NONE -> MaterialTheme.colorScheme.onSurface
                                    }
                                )
                                Text(
                                    text = "Score: $currentScore/50",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )
                            }
                            
                            // Achievement Icon
                            Icon(
                                imageVector = when (achievementLevel) {
                                    TopicQuestionBank.AchievementLevel.SUPREME -> Icons.Filled.Star
                                    TopicQuestionBank.AchievementLevel.SPECIAL -> Icons.Filled.EmojiEvents
                                    TopicQuestionBank.AchievementLevel.ENCOURAGED -> Icons.Filled.ThumbUp
                                    TopicQuestionBank.AchievementLevel.NONE -> Icons.Filled.School
                                },
                                contentDescription = null,
                                tint = when (achievementLevel) {
                                    TopicQuestionBank.AchievementLevel.SUPREME -> Color(0xFFFFD700)
                                    TopicQuestionBank.AchievementLevel.SPECIAL -> Color(0xFFC0C0C0)
                                    TopicQuestionBank.AchievementLevel.ENCOURAGED -> MaterialTheme.colorScheme.primary
                                    TopicQuestionBank.AchievementLevel.NONE -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                },
                                modifier = Modifier.size(48.dp)
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(Dimensions.spaceMedium))
                        
                        // Progress Bar
                        LinearProgressIndicator(
                            progress = { currentScore.toFloat() / 50f },
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                        
                        Spacer(modifier = Modifier.height(Dimensions.spaceMedium))
                        
                        // Encouragement Message
                        Text(
                            text = encouragementMessage,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(Dimensions.spaceLarge))
                
                // Topic Description
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(Dimensions.cornerRadiusLarge),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(Dimensions.paddingLarge)
                    ) {
                        Text(
                            text = when (selectedTab) {
                                0 -> "Gospels Quiz"
                                1 -> "Prophets Quiz"
                                2 -> "Parables Quiz"
                                else -> "Topic Quiz"
                            },
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(Dimensions.spaceMedium))
                        
                        Text(
                            text = when (selectedTab) {
                                0 -> "Test your knowledge of the four Gospels: Matthew, Mark, Luke, and John. Learn about Jesus' life, teachings, miracles, and the foundation of Christianity."
                                1 -> "Explore the messages of God's prophets throughout the Bible. Discover their warnings, promises, and insights into God's plan for His people."
                                2 -> "Master Jesus' parables and wisdom teachings. Understand the deeper meanings behind His stories and how they apply to our lives today."
                                else -> "Choose a topic to begin your specialized Bible study."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                        )
                        
                        Spacer(modifier = Modifier.height(Dimensions.spaceMedium))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "50 Questions",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                            Text(
                                text = "Specialized Content",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(Dimensions.spaceLarge))
                
                // Start Quiz Button
                Button(
                    onClick = { 
                        val mode = when (selectedTab) {
                            0 -> "gospels"
                            1 -> "prophets"
                            2 -> "parables"
                            else -> "topic"
                        }
                        navController.navigate(Screen.TopicQuiz.createRoute(mode))
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(Dimensions.buttonHeight),
                    shape = RoundedCornerShape(Dimensions.cornerRadiusLarge),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(Dimensions.spaceSmall))
                    Text(
                        text = stringResource(R.string.start_quiz),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
                
                Spacer(modifier = Modifier.height(Dimensions.spaceMedium))
                
                // Achievement Levels Info
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(Dimensions.cornerRadiusLarge),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(Dimensions.paddingLarge)
                    ) {
                        Text(
                            text = "Achievement Levels",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(Dimensions.spaceMedium))
                        
                        AchievementLevelRow("Supreme", "50/50", "Perfect Score!", Color(0xFFFFD700))
                        AchievementLevelRow("Special", "45-49/50", "Excellent!", Color(0xFFC0C0C0))
                        AchievementLevelRow("Encouraged", "1-44/50", "Keep Learning!", MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}

@Composable
private fun AchievementLevelRow(title: String, score: String, description: String, color: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Dimensions.spaceSmall),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = when (title) {
                "Supreme" -> Icons.Filled.Star
                "Special" -> Icons.Filled.EmojiEvents
                else -> Icons.Filled.ThumbUp
            },
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(Dimensions.spaceMedium))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "$score - $description",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}


