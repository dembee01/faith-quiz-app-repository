package com.example.faithquiz.ui.view.results

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PresentToAll
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.faithquiz.R
import com.example.faithquiz.data.store.ProgressDataStore
import com.example.faithquiz.ui.navigation.Screen
import com.example.faithquiz.ui.theme.Dimensions
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

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = false,
                    onClick = { navController.navigate(Screen.MainMenu.route) },
                    icon = { Icon(Icons.Filled.Home, contentDescription = null) },
                    label = { Text(stringResource(id = R.string.back_to_menu)) }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = { navController.navigate(Screen.LevelSelect.route) },
                    icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = null) },
                    label = { Text(stringResource(id = R.string.quizzes_title)) }
                )
                NavigationBarItem(
                    selected = true,
                    onClick = { /* already here */ },
                    icon = { Icon(Icons.Filled.PresentToAll, contentDescription = null) },
                    label = { Text(stringResource(id = R.string.results)) }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = { navController.navigate(Screen.Settings.route) },
                    icon = { Icon(Icons.Filled.Settings, contentDescription = null) },
                    label = { Text(stringResource(id = R.string.settings)) }
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Dimensions.screenPadding),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = stringResource(R.string.back_description),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                Text(
                    text = "Quiz Results",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.width(Dimensions.iconSize))
            }

            // Tabs
            TabRow(
                selectedTabIndex = selectedTab,
                modifier = Modifier.padding(horizontal = Dimensions.screenPadding)
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(Dimensions.spaceMedium))

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
            .padding(Dimensions.screenPadding)
            .verticalScroll(rememberScrollState())
    ) {
        // Stats Cards Row 1
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Dimensions.spaceMedium)
        ) {
            StatCard(
                title = "Total Attempts",
                value = totalAttempts.toString(),
                icon = Icons.Filled.PresentToAll,
                modifier = Modifier.weight(1f)
            )
            StatCard(
                title = "High Score",
                value = highScore.toString(),
                icon = Icons.Filled.Star,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(Dimensions.spaceMedium))

        // Stats Cards Row 2
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Dimensions.spaceMedium)
        ) {
            StatCard(
                title = "Total Answered",
                value = totalAnswered.toString(),
                icon = Icons.AutoMirrored.Filled.TrendingUp,
                modifier = Modifier.weight(1f)
            )
            StatCard(
                title = "Avg Time",
                value = "${avgTimePerQuestionSec}s",
                icon = Icons.Filled.Timer,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(Dimensions.spaceLarge))

        // Last Attempt Summary
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(Dimensions.cornerRadiusLarge)
        ) {
            Column(modifier = Modifier.padding(Dimensions.paddingLarge)) {
                Text(
                    text = "Last Attempt",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(Dimensions.spaceMedium))

                if (lastAttempt == null) {
                    Text(
                        text = "No attempts recorded yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Level ${lastAttempt.level}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = formatDate(lastAttempt.date),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                    Spacer(modifier = Modifier.height(Dimensions.spaceSmall))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Score: ${lastAttempt.score}/${lastAttempt.total}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Time: ${lastAttempt.timeSeconds}s",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }

                    Spacer(modifier = Modifier.height(Dimensions.spaceSmall))

                    Text(
                        text = if (lastAttempt.mode.isBlank()) "Mode: classic" else "Mode: ${lastAttempt.mode}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(Dimensions.spaceLarge))

        // Mistakes summary
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onViewMistakes() },
            shape = RoundedCornerShape(Dimensions.cornerRadiusLarge),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(modifier = Modifier.padding(Dimensions.paddingLarge)) {
                Text(
                    text = "Recorded Mistakes",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(Dimensions.spaceSmall))
                Text(
                    text = "$mistakesCount total",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )
                Spacer(modifier = Modifier.height(Dimensions.spaceSmall))
                LinearProgressIndicator(
                    progress = {
                        // Use a normalized indicator based on last 50 as rough scale
                        val scale = 50f
                        (mistakesCount.coerceAtMost(scale.toInt()) / scale)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(Dimensions.spaceSmall))
                Text(
                    text = "Tap to view details",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun MistakesTab(
    mistakes: List<ProgressDataStore.MistakeEntry>
) {
    val sorted = remember(mistakes) { mistakes.sortedByDescending { it.date } }

    if (sorted.isEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(Dimensions.screenPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "No mistakes recorded yet.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(Dimensions.screenPadding)
    ) {
        items(sorted) { entry ->
            MistakeCard(entry = entry)
            Spacer(modifier = Modifier.height(Dimensions.spaceSmall))
        }
    }
}

@Composable
private fun StatCard(
    title: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(Dimensions.cornerRadiusMedium)
    ) {
        Column(
            modifier = Modifier.padding(Dimensions.paddingMedium),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(Dimensions.spaceSmall))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun MistakeCard(entry: ProgressDataStore.MistakeEntry) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Dimensions.cornerRadiusMedium)
    ) {
        Column(
            modifier = Modifier.padding(Dimensions.paddingMedium)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Level ${entry.level}",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = formatDate(entry.date),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }

            Spacer(modifier = Modifier.height(Dimensions.spaceSmall))

            Text(
                text = entry.question,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(Dimensions.spaceSmall))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Dimensions.spaceMedium)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Your Answer:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                    Text(
                        text = entry.userAnswer,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Correct Answer:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = entry.correctAnswer,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            if (entry.explanation.isNotBlank()) {
                Spacer(modifier = Modifier.height(Dimensions.spaceSmall))
                Text(
                    text = entry.explanation,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

private fun formatDate(timestamp: Long): String {
    val date = Date(timestamp)
    val formatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    return formatter.format(date)
}
