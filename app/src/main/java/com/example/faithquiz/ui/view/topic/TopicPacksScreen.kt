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
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.faithquiz.R
import com.example.faithquiz.ui.navigation.Screen
import com.example.faithquiz.ui.theme.*
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
                    colors = listOf(DeepRoyalPurple, Color.Black)
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
                        tint = GlowingGold
                    )
                }
                Text(
                    text = stringResource(R.string.topic_packs),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = GlowingGold,
                    letterSpacing = 2.sp
                )
                Spacer(modifier = Modifier.width(Dimensions.iconSize))
            }

            Spacer(modifier = Modifier.height(Dimensions.spaceLarge))

            // Tabs
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent, // Transparent for Glass effect
                contentColor = GlowingGold,
                edgePadding = 0.dp,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = GlowingGold,
                        height = 3.dp
                    )
                },
                divider = {}
            ) {
                tabs.forEachIndexed { index, resId ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        selectedContentColor = GlowingGold,
                        unselectedContentColor = Color.White.copy(alpha = 0.6f),
                        text = {
                            Text(
                                text = stringResource(id = resId).uppercase(),
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = if (selectedTab == index) GlowingGold else Color.White.copy(alpha = 0.6f)
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
                    colors = CardDefaults.cardColors(containerColor = EtherealGlass)
                ) {
                    Column(
                        modifier = Modifier.padding(Dimensions.paddingLarge)
                    ) {
                        Text(
                            text = "YOUR ACHIEVEMENT",
                            style = MaterialTheme.typography.labelSmall,
                            color = GlowingGold,
                            letterSpacing = 1.sp
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
                                        TopicQuestionBank.AchievementLevel.SUPREME -> Color(0xFFFFD700)
                                        TopicQuestionBank.AchievementLevel.SPECIAL -> Color(0xFFC0C0C0)
                                        else -> Color.White
                                    }
                                )
                                Text(
                                    text = "SCORE: $currentScore/50",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White.copy(alpha = 0.7f)
                                )
                            }
                            
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
                                    TopicQuestionBank.AchievementLevel.ENCOURAGED -> GlowingGold
                                    TopicQuestionBank.AchievementLevel.NONE -> Color.White.copy(alpha = 0.5f)
                                },
                                modifier = Modifier.size(48.dp)
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(Dimensions.spaceMedium))
                        
                        // Progress Bar
                        LinearProgressIndicator(
                            progress = { currentScore.toFloat() / 50f },
                            modifier = Modifier.fillMaxWidth(),
                            color = GlowingGold,
                            trackColor = Color.White.copy(alpha = 0.2f)
                        )
                        
                        Spacer(modifier = Modifier.height(Dimensions.spaceMedium))
                        
                        // Encouragement Message
                        Text(
                            text = encouragementMessage,
                            style = MaterialTheme.typography.bodyMedium.copy(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic),
                            color = Color.White.copy(alpha = 0.9f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(Dimensions.spaceLarge))
                
                // Topic Description
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(Dimensions.cornerRadiusLarge),
                    colors = CardDefaults.cardColors(containerColor = EtherealGlass)
                ) {
                    Column(
                        modifier = Modifier.padding(Dimensions.paddingLarge)
                    ) {
                        Text(
                            text = when (selectedTab) {
                                0 -> "GOSPELS QUIZ"
                                1 -> "PROPHETS QUIZ"
                                2 -> "PARABLES QUIZ"
                                else -> "TOPIC QUIZ"
                            },
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
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
                            color = Color.White.copy(alpha = 0.8f)
                        )
                        
                        Spacer(modifier = Modifier.height(Dimensions.spaceMedium))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "50 Questions",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.6f)
                            )
                            Text(
                                text = "Specialized Content",
                                style = MaterialTheme.typography.labelSmall,
                                color = GlowingGold
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
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GlowingGold,
                        contentColor = DeepRoyalPurple
                    )
                ) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(Dimensions.spaceSmall))
                    Text(
                        text = stringResource(R.string.start_quiz).uppercase(),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
                
                Spacer(modifier = Modifier.height(Dimensions.spaceMedium))
                
                // Achievement Levels Info
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(Dimensions.cornerRadiusLarge),
                    colors = CardDefaults.cardColors(containerColor = EtherealGlass)
                ) {
                    Column(
                        modifier = Modifier.padding(Dimensions.paddingLarge)
                    ) {
                        Text(
                            text = "ACHIEVEMENT LEVELS",
                            style = MaterialTheme.typography.labelSmall,
                            color = GlowingGold,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(Dimensions.spaceMedium))
                        
                        AchievementLevelRow("Supreme", "50/50", "Perfect Score!", Color(0xFFFFD700))
                        AchievementLevelRow("Special", "45-49/50", "Excellent!", Color(0xFFC0C0C0))
                        AchievementLevelRow("Encouraged", "1-44/50", "Keep Learning!", Color.White)
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
                color = Color.White
            )
            Text(
                text = "$score - $description",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.7f)
            )
        }
    }
}
