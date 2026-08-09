package com.example.faithquiz.ui.view.review

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.faithquiz.data.store.ProgressDataStore
import com.example.faithquiz.ui.navigation.Screen
import com.example.faithquiz.ui.theme.*
import com.example.faithquiz.ui.view.components.DivineBackground
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ReviewScreen(navController: NavController) {
    val context = LocalContext.current

    val mistakes by ProgressDataStore.observeMistakesDetailed(context).collectAsState(initial = emptyList())
    val dueReviews by ProgressDataStore.observeDueReview(context).collectAsState(initial = emptyList())

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
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = SlateTextPrimary
                    )
                }
                Text(
                    text = "WISDOM REVIEW",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = SlateTextPrimary,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Reflect on your journey and strengthen your Bible knowledge.",
                fontSize = 14.sp,
                color = SlateTextSecondary,
                modifier = Modifier.padding(start = 12.dp, bottom = 16.dp)
            )

            if (dueReviews.isNotEmpty()) {
                val dueLevel = dueReviews.mapNotNull(ProgressDataStore::reviewLevelFromKey).firstOrNull()
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = SlateSurface)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = "${dueReviews.size} ${if (dueReviews.size == 1) "item" else "items"} due for review",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = GoldAccent
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Spaced repetition practice reinforces past answers.",
                                fontSize = 12.sp,
                                color = SlateTextSecondary
                            )
                        }
                        Button(
                            onClick = {
                                if (dueLevel == null || dueLevel == 0) {
                                    navController.navigate(Screen.DailyChallenge.route)
                                } else {
                                    navController.navigate(Screen.Quiz.createRoute(dueLevel, "practice"))
                                }
                            },
                            shape = RoundedCornerShape(28.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SlateCardLight,
                                contentColor = SlateButtonText
                            )
                        ) {
                            Text("Practice", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            if (mistakes.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No reviews yet. Keep playing to learn!",
                        fontSize = 16.sp,
                        color = SlateTextMuted
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(mistakes) { entry ->
                        SlateReviewItem(entry = entry)
                    }
                    item { Spacer(modifier = Modifier.height(32.dp)) }
                }
            }
        }
    }
}

@Composable
private fun SlateReviewItem(entry: ProgressDataStore.MistakeEntry) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, SlateCardBorder, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SlateSurface)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
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

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = entry.question,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = SlateTextPrimary
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .background(WrongAnswerRed.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                        .border(1.dp, WrongAnswerRed.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Text(
                        text = "YOU ANSWERED",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = WrongAnswerRed
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = entry.userAnswer,
                        fontSize = 14.sp,
                        color = SlateTextPrimary
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .background(CorrectAnswerGreen.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                        .border(1.dp, CorrectAnswerGreen.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Text(
                        text = "CORRECT ANSWER",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = CorrectAnswerGreen
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = entry.correctAnswer,
                        fontSize = 14.sp,
                        color = SlateTextPrimary
                    )
                }
            }

            if (entry.explanation.isNotBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.Top) {
                    Icon(
                        imageVector = Icons.Filled.Info,
                        contentDescription = "Info",
                        tint = GoldAccent,
                        modifier = Modifier.size(16.dp).padding(top = 2.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = entry.explanation,
                        fontSize = 13.sp,
                        color = SlateTextSecondary
                    )
                }
            }
        }
    }
}

private fun formatDate(timestamp: Long): String {
    val date = Date(timestamp)
    val formatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    return formatter.format(date)
}

