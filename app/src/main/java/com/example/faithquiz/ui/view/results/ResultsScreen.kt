package com.example.faithquiz.ui.view.results

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PresentToAll
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.faithquiz.data.store.ProgressDataStore
import com.example.faithquiz.ui.theme.*
import com.example.faithquiz.ui.view.components.DivineBackground
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ResultsScreen(
    navController: NavController
) {
    val context = LocalContext.current

    val totalAttempts by ProgressDataStore.observeTotalAttempts(context).collectAsState(initial = 0)
    val totalAnswered by ProgressDataStore.observeTotalQuestionsAnswered(context).collectAsState(initial = 0)
    val highScore by ProgressDataStore.observeHighScore(context).collectAsState(initial = 0)
    val totalTimeSpent by ProgressDataStore.observeTotalTimeSpent(context).collectAsState(initial = 0L)
    val lastAttempt by ProgressDataStore.observeLastAttempt(context).collectAsState(initial = null)
    val mistakesDetailed by ProgressDataStore.observeMistakesDetailed(context).collectAsState(initial = emptyList())

    val avgTimePerQuestionSec = remember(totalTimeSpent, totalAnswered) {
        if (totalAnswered > 0) (totalTimeSpent / totalAnswered).toInt() else 0
    }

    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Overview", "Mistakes")

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
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Close",
                        tint = SlateTextPrimary
                    )
                }
                Text(
                    text = "QUIZ RESULTS",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = SlateTextPrimary,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            SlateTabRow(
                selectedIndex = selectedTab,
                tabs = tabs,
                onTabSelected = { selectedTab = it }
            )

            Spacer(modifier = Modifier.height(16.dp))

            Box(modifier = Modifier.weight(1f)) {
                when (selectedTab) {
                    0 -> OverviewTab(
                        totalAttempts = totalAttempts,
                        highScore = highScore,
                        totalAnswered = totalAnswered,
                        avgTimePerQuestionSec = avgTimePerQuestionSec,
                        lastAttempt = lastAttempt,
                        mistakesCount = mistakesDetailed.size,
                        onViewMistakes = { selectedTab = 1 }
                    )

                    1 -> MistakesTab(
                        mistakes = mistakesDetailed
                    )
                }
            }
        }
    }
}

@Composable
private fun SlateTabRow(
    selectedIndex: Int,
    tabs: List<String>,
    onTabSelected: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .background(SlateSurface, RoundedCornerShape(28.dp))
            .border(1.dp, SlateCardBorder, RoundedCornerShape(28.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {
        tabs.forEachIndexed { index, title ->
            val isSelected = selectedIndex == index
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(
                        if (isSelected) SlateCardLight else Color.Transparent,
                        RoundedCornerShape(24.dp)
                    )
                    .clickable { onTabSelected(index) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = title.uppercase(),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = if (isSelected) SlateButtonText else SlateTextSecondary
                )
            }
        }
    }
}

@Composable
private fun OverviewTab(
    totalAttempts: Int,
    highScore: Int,
    totalAnswered: Int,
    avgTimePerQuestionSec: Int,
    lastAttempt: ProgressDataStore.LastAttempt?,
    mistakesCount: Int,
    onViewMistakes: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SlateOverviewStatCard(
                title = "TOTAL ATTEMPTS",
                value = totalAttempts.toString(),
                icon = Icons.Filled.PresentToAll,
                modifier = Modifier.weight(1f)
            )
            SlateOverviewStatCard(
                title = "HIGH SCORE",
                value = highScore.toString(),
                icon = Icons.Filled.Star,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SlateOverviewStatCard(
                title = "QUESTIONS ANSWERED",
                value = totalAnswered.toString(),
                icon = Icons.AutoMirrored.Filled.TrendingUp,
                modifier = Modifier.weight(1f)
            )
            SlateOverviewStatCard(
                title = "AVG SPEED",
                value = "${avgTimePerQuestionSec}s / Q",
                icon = Icons.Filled.Timer,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        SlateOverviewSectionHeader("LAST ATTEMPT")
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, SlateCardBorder, RoundedCornerShape(20.dp)),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = SlateSurface)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                if (lastAttempt == null) {
                    Text(
                        text = "No attempts recorded yet.",
                        fontSize = 14.sp,
                        color = SlateTextMuted
                    )
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "LEVEL ${lastAttempt.level}",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = GoldAccent
                        )
                        Text(
                            text = formatDate(lastAttempt.date),
                            fontSize = 12.sp,
                            color = SlateTextMuted
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Score: ${lastAttempt.score}/${lastAttempt.total}",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = SlateTextPrimary
                        )
                        Text(
                            text = "${lastAttempt.timeSeconds}s",
                            fontSize = 14.sp,
                            color = SlateTextSecondary
                        )
                    }

                    Text(
                        text = "Mode: ${if (lastAttempt.mode.isBlank()) "Classic" else lastAttempt.mode.replaceFirstChar { it.titlecase() }}",
                        fontSize = 12.sp,
                        color = SlateTextMuted,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        SlateOverviewSectionHeader("MISTAKES")
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onViewMistakes() }
                .border(1.dp, WrongAnswerRed.copy(alpha = 0.4f), RoundedCornerShape(20.dp)),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = WrongAnswerRed.copy(alpha = 0.12f)
            )
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Recorded Mistakes",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = SlateTextPrimary
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = "$mistakesCount total",
                        fontSize = 13.sp,
                        color = SlateTextSecondary
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                LinearProgressIndicator(
                    progress = {
                        val scale = 50f
                        (mistakesCount.coerceAtMost(scale.toInt()) / scale)
                    },
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                    color = WrongAnswerRed,
                    trackColor = SlateSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Tap to review details",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = GoldAccent
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun MistakesTab(
    mistakes: List<ProgressDataStore.MistakeEntry>
) {
    val sorted = remember(mistakes) { mistakes.sortedByDescending { it.date } }

    if (sorted.isEmpty()) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(Icons.Filled.Star, contentDescription = null, tint = GoldAccent, modifier = Modifier.size(48.dp))
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "No mistakes recorded yet.",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = SlateTextPrimary
            )
            Text(
                text = "Keep up the faithful work!",
                fontSize = 14.sp,
                color = SlateTextSecondary
            )
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(sorted) { entry ->
            SlateMistakeCard(entry = entry)
        }
        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}

@Composable
private fun SlateOverviewStatCard(
    title: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SlateSurface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = GoldAccent,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = SlateTextPrimary
            )
            Text(
                text = title,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = SlateTextSecondary,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun SlateMistakeCard(entry: ProgressDataStore.MistakeEntry) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, SlateCardBorder, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SlateSurface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "LEVEL ${entry.level}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = GoldAccent
                )
                Text(
                    text = formatDate(entry.date),
                    fontSize = 12.sp,
                    color = SlateTextMuted
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = entry.question,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = SlateTextPrimary,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "YOUR ANSWER",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = WrongAnswerRed
                    )
                    Text(
                        text = entry.userAnswer,
                        fontSize = 13.sp,
                        color = SlateTextPrimary
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "CORRECT ANSWER",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = CorrectAnswerGreen
                    )
                    Text(
                        text = entry.correctAnswer,
                        fontSize = 13.sp,
                        color = SlateTextPrimary
                    )
                }
            }

            if (entry.explanation.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SlateBackgroundBottom.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                        .padding(10.dp)
                ) {
                    Text(
                        text = "NOTE: ${entry.explanation}",
                        fontSize = 12.sp,
                        color = SlateTextSecondary
                    )
                }
            }
        }
    }
}

@Composable
fun SlateOverviewSectionHeader(title: String) {
    Text(
        text = title,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.5.sp,
        color = GoldAccent,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

private fun formatDate(timestamp: Long): String {
    val date = Date(timestamp)
    val formatter = SimpleDateFormat("MMM dd", Locale.getDefault())
    return formatter.format(date)
}

