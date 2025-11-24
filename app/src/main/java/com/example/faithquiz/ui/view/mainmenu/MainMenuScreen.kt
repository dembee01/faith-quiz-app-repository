package com.example.faithquiz.ui.view.mainmenu

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.faithquiz.ui.navigation.Screen
import androidx.compose.ui.res.stringResource
import com.example.faithquiz.R
import com.example.faithquiz.ui.theme.Dimensions
import androidx.compose.ui.platform.LocalContext
import com.example.faithquiz.data.store.ProgressDataStore
import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.coroutines.flow.collectLatest

@Composable
fun MainMenuScreen(
    navController: NavController
) {
    val context = LocalContext.current
    val totalAnswered by ProgressDataStore.observeTotalQuestionsAnswered(context).collectAsState(initial = 0)
    val highScore by ProgressDataStore.observeHighScore(context).collectAsState(initial = 0)
    val lastCompleted by ProgressDataStore.observeLastCompletedLevel(context).collectAsState(initial = 1)
    // val dailyStreak by ProgressDataStore.observeDailyStreak(context).collectAsState(initial = 0)
    val adaptiveLevel by ProgressDataStore.observeAdaptiveLevel(context).collectAsState(initial = 1)
    val devotionStreak by ProgressDataStore.observeDevotionStreak(context).collectAsState(initial = 0)

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
                .padding(Dimensions.screenPadding)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Text(
                text = stringResource(R.string.faith_quiz),
                fontSize = 42.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimary,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(Dimensions.spaceLarge))
            
            Text(
                text = stringResource(R.string.test_your_bible_knowledge),
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(Dimensions.spaceXXLarge))
            
            // Advanced stats board
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(Dimensions.cornerRadiusLarge),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.2f)
                ),
                elevation = CardDefaults.cardElevation(Dimensions.cardElevation)
            ) {
                Column(modifier = Modifier.padding(Dimensions.paddingLarge)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.Star, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(Dimensions.spaceSmall))
                        Text(text = stringResource(id = R.string.your_progress), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    }
                    Spacer(modifier = Modifier.height(Dimensions.spaceMedium))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        StatTile(
                            title = stringResource(R.string.high_score),
                            value = highScore.toString(),
                            icon = Icons.Default.Star
                        )
                        StatTile(
                            title = stringResource(R.string.last_level),
                            value = lastCompleted.toString(),
                            icon = Icons.Default.Flag
                        )
                        StatTile(
                            title = stringResource(R.string.questions),
                            value = totalAnswered.toString(),
                            icon = Icons.AutoMirrored.Filled.Help
                        )
                        StatTile(
                            title = stringResource(R.string.streak),
                            value = devotionStreak.toString(),
                            icon = Icons.Default.Whatshot
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(Dimensions.spaceXLarge))
            
            Button(
                onClick = {
                    navController.navigate(Screen.LevelSelect.route)
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
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = stringResource(R.string.start_quiz_description)
                )
                Spacer(modifier = Modifier.width(Dimensions.spaceSmall))
                Text(
                    text = stringResource(R.string.start_quiz),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    )
                )
            }
            
            Spacer(modifier = Modifier.height(Dimensions.spaceMedium))
            
            // Review button
            Button(
                onClick = { navController.navigate(Screen.Review.route) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(Dimensions.buttonHeight),
                shape = RoundedCornerShape(Dimensions.cornerRadiusLarge),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                    contentColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(imageVector = Icons.Default.Bookmarks, contentDescription = stringResource(R.string.review))
                Spacer(modifier = Modifier.width(Dimensions.spaceSmall))
                Text(
                    text = stringResource(R.string.review),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }

            Spacer(modifier = Modifier.height(Dimensions.spaceMedium))

            // Adaptive recommendation (disabled button just shows level)
            OutlinedButton(
                onClick = { /* no-op */ },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(Dimensions.buttonHeight),
                shape = RoundedCornerShape(Dimensions.cornerRadiusLarge)
            ) {
                Text(text = stringResource(id = R.string.adaptive_recommendation, adaptiveLevel))
            }

            Spacer(modifier = Modifier.height(Dimensions.spaceMedium))

            // Topic packs button
            Button(
                onClick = { navController.navigate(Screen.TopicPacks.route) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(Dimensions.buttonHeight),
                shape = RoundedCornerShape(Dimensions.cornerRadiusLarge)
            ) {
                Icon(Icons.Default.Category, contentDescription = stringResource(id = R.string.topic_packs))
                Spacer(modifier = Modifier.width(Dimensions.spaceSmall))
                Text(text = stringResource(id = R.string.topic_packs))
            }

            // Quick start section removed for a cleaner, more advanced main UI

            Button(
                onClick = {
                    navController.navigate(Screen.Leaderboard.route)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(Dimensions.buttonHeight),
                shape = RoundedCornerShape(Dimensions.cornerRadiusLarge),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                    contentColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Leaderboard,
                    contentDescription = stringResource(R.string.view_leaderboard_description)
                )
                Spacer(modifier = Modifier.width(Dimensions.spaceSmall))
                Text(
                    text = stringResource(R.string.leaderboard),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    )
                )
            }
            
            Spacer(modifier = Modifier.height(Dimensions.spaceMedium))
            
            Button(
                onClick = {
                    navController.navigate(Screen.Settings.route)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(Dimensions.buttonHeight),
                shape = RoundedCornerShape(Dimensions.cornerRadiusLarge),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                    contentColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = stringResource(R.string.open_settings_description)
                )
                Spacer(modifier = Modifier.width(Dimensions.spaceSmall))
                Text(
                    text = stringResource(R.string.settings),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }
    }
}

@Composable
private fun StatTile(title: String, value: String, icon: ImageVector) {
    Card(
        shape = RoundedCornerShape(Dimensions.cornerRadiusMedium),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
        ),
        elevation = CardDefaults.cardElevation(Dimensions.cardElevation)
    ) {
        Column(
            modifier = Modifier.padding(Dimensions.paddingMedium),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(imageVector = icon, contentDescription = title, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(Dimensions.spaceSmall))
            Text(value, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
            Text(title, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
        }
    }
}