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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.faithquiz.R
import com.example.faithquiz.data.store.ProgressDataStore
import com.example.faithquiz.ui.theme.*
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
                .padding(Dimensions.screenPadding)
        ) {
            // Header
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Close",
                        tint = GlowingGold
                    )
                }
                Text(
                    text = "QUIZ RESULTS",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold,
                        color = GlowingGold
                    ),
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))

            // Tabs
            DivineTabRow(
                selectedIndex = selectedTab,
                tabs = tabs,
                onTabSelected = { selectedTab = it }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Tab Content
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
private fun DivineTabRow(
    selectedIndex: Int,
    tabs: List<String>,
    onTabSelected: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .background(EtherealGlass, RoundedCornerShape(24.dp))
            .border(1.dp, GlowingGold.copy(alpha = 0.3f), RoundedCornerShape(24.dp))
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
                        if (isSelected) GlowingGold else Color.Transparent,
                        RoundedCornerShape(20.dp)
                    )
                    .clickable { onTabSelected(index) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = title.uppercase(),
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    ),
                    color = if (isSelected) DeepRoyalPurple else Color.White.copy(alpha = 0.7f)
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
        // Stats Cards Row 1
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            DivineStatCard(
                title = "TOTAL\nATTEMPTS",
                value = totalAttempts.toString(),
                icon = Icons.Filled.PresentToAll,
                modifier = Modifier.weight(1f)
            )
            DivineStatCard(
                title = "HIGH\nSCORE",
                value = highScore.toString(),
                icon = Icons.Filled.Star,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Stats Cards Row 2
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            DivineStatCard(
                title = "QUESTIONS\nANSWERED",
                value = totalAnswered.toString(),
                icon = Icons.AutoMirrored.Filled.TrendingUp,
                modifier = Modifier.weight(1f)
            )
            DivineStatCard(
                title = "AVG\nSPEED",
                value = "${avgTimePerQuestionSec}xs",
                icon = Icons.Filled.Timer,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Last Attempt Summary
        DivineSectionHeader("LAST ATTEMPT")
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, GlowingGold.copy(alpha = 0.3f), RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = EtherealGlass)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                if (lastAttempt == null) {
                    Text(
                        text = "No attempts recorded yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "LEVEL ${lastAttempt.level}",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = GlowingGold
                            )
                        )
                        Text(
                            text = formatDate(lastAttempt.date),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Score: ${lastAttempt.score}/${lastAttempt.total}",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.White
                        )
                        Text(
                            text = "${lastAttempt.timeSeconds}s",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                    
                    Text(
                        text = "Mode: ${if (lastAttempt.mode.isBlank()) "Classic" else lastAttempt.mode.replaceFirstChar { it.titlecase() }}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.5f),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Mistakes summary
        DivineSectionHeader("MISTAKES")
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onViewMistakes() }
                .border(1.dp, WrongAnswerRed.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = WrongAnswerRed.copy(alpha = 0.1f)
            )
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Recorded Mistakes",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = "$mistakesCount total",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                LinearProgressIndicator(
                    progress = {
                        val scale = 50f
                        (mistakesCount.coerceAtMost(scale.toInt()) / scale)
                    },
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                    color = WrongAnswerRed,
                    trackColor = Color.White.copy(alpha = 0.1f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Tap to review details",
                    style = MaterialTheme.typography.bodySmall,
                    color = GlowingGold.copy(alpha = 0.8f)
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
            Icon(Icons.Filled.Star, contentDescription = null, tint = GlowingGold, modifier = Modifier.size(48.dp))
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "No mistakes recorded yet.",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.7f)
            )
            Text(
                text = "Keep up the faithful work!",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.5f)
            )
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(sorted) { entry ->
            DivineMistakeCard(entry = entry)
        }
        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}

@Composable
private fun DivineStatCard(
    title: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = EtherealGlass),
        elevation = CardDefaults.cardElevation(0.dp)
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
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = Color.White
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                ),
                color = Color.White.copy(alpha = 0.6f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

@Composable
private fun DivineMistakeCard(entry: ProgressDataStore.MistakeEntry) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = EtherealGlass)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "LEVEL ${entry.level}",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = GlowingGold
                    )
                )
                Text(
                    text = formatDate(entry.date),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.5f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = entry.question,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Medium
                ),
                color = Color.White,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "YOUR ANSWER",
                        style = MaterialTheme.typography.labelSmall,
                        color = WrongAnswerRed
                    )
                    Text(
                        text = entry.userAnswer,
                        style = MaterialTheme.typography.bodySmall,
                        color = WrongAnswerRed.copy(alpha = 0.9f)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "CORRECT ANSWER",
                        style = MaterialTheme.typography.labelSmall,
                        color = CorrectAnswerGreen
                    )
                    Text(
                        text = entry.correctAnswer,
                        style = MaterialTheme.typography.bodySmall,
                        color = CorrectAnswerGreen.copy(alpha = 0.9f)
                    )
                }
            }

            if (entry.explanation.isNotBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                        .padding(8.dp)
                ) {
                    Text(
                        text = "NOTE: ${entry.explanation}",
                        style = MaterialTheme.typography.bodySmall.copy(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic),
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}

@Composable
fun DivineSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge.copy(
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp
        ),
        color = GlowingGold.copy(alpha = 0.8f),
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

private fun formatDate(timestamp: Long): String {
    val date = Date(timestamp)
    val formatter = SimpleDateFormat("MMM dd", Locale.getDefault())
    return formatter.format(date)
}
