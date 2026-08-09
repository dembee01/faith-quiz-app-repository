package com.example.faithquiz.ui.view.topic

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.faithquiz.R
import com.example.faithquiz.data.TopicQuestionBank
import com.example.faithquiz.data.store.ProgressDataStore
import com.example.faithquiz.ui.navigation.Screen
import com.example.faithquiz.ui.theme.*
import com.example.faithquiz.ui.view.components.DivineBackground

@Composable
fun TopicPacksScreen(navController: NavController) {
    val context = LocalContext.current
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    val tabs = listOf(
        R.string.gospels,
        R.string.prophets,
        R.string.parables
    )

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

    DivineBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 22.dp, vertical = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Header Top Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.back_description),
                        tint = SlateTextPrimary
                    )
                }
                Text(
                    text = stringResource(R.string.topic_packs),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = SlateTextPrimary
                )
                Spacer(modifier = Modifier.width(48.dp))
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Tabs
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                contentColor = GoldAccent,
                edgePadding = 0.dp,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = GoldAccent,
                        height = 3.dp
                    )
                },
                divider = {}
            ) {
                tabs.forEachIndexed { index, resId ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        selectedContentColor = GoldAccent,
                        unselectedContentColor = SlateTextSecondary,
                        text = {
                            Text(
                                text = stringResource(id = resId).uppercase(),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (selectedTab == index) GoldAccent else SlateTextSecondary
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Scrollable Content Body
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                // Achievement Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = SlateSurface)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "YOUR ACHIEVEMENT",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = GoldAccent,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = achievementTitle,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SlateTextPrimary
                                )
                                Text(
                                    text = "SCORE: $currentScore/50",
                                    fontSize = 14.sp,
                                    color = SlateTextSecondary
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
                                tint = GoldAccent,
                                modifier = Modifier.size(44.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        LinearProgressIndicator(
                            progress = { currentScore.toFloat() / 50f },
                            modifier = Modifier.fillMaxWidth(),
                            color = GoldAccent,
                            trackColor = SlateSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = encouragementMessage,
                            fontSize = 13.sp,
                            color = SlateTextSecondary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Topic Description Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = SlateSurface)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = when (selectedTab) {
                                0 -> "GOSPELS QUIZ"
                                1 -> "PROPHETS QUIZ"
                                2 -> "PARABLES QUIZ"
                                else -> "TOPIC QUIZ"
                            },
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = SlateTextPrimary
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = when (selectedTab) {
                                0 -> "Test your knowledge of the four Gospels: Matthew, Mark, Luke, and John. Learn about Jesus' life, teachings, miracles, and the foundation of Christianity."
                                1 -> "Explore the messages of God's prophets throughout the Bible. Discover their warnings, promises, and insights into God's plan for His people."
                                2 -> "Master Jesus' parables and wisdom teachings. Understand the deeper meanings behind His stories and how they apply to our lives today."
                                else -> "Choose a topic to begin your specialized Bible study."
                            },
                            fontSize = 14.sp,
                            color = SlateTextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Start Quiz Button (Pill Action Container)
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
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SlateCardLight,
                        contentColor = SlateButtonText
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                ) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = SlateButtonText)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.start_quiz).uppercase(),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = SlateButtonText
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

