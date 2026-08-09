package com.example.faithquiz.ui.view.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.faithquiz.data.model.LevelProgress
import com.example.faithquiz.ui.theme.*

@Composable
fun LevelCard(
    level: Int,
    levelProgress: LevelProgress?,
    isUnlocked: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val progress = levelProgress ?: LevelProgress(level = level, isUnlocked = isUnlocked)
    val completionPercentage = if (progress.questionsAnswered > 0) {
        (progress.correctAnswers.toFloat() / progress.questionsAnswered.toFloat()) * 100
    } else 0f

    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(116.dp)
            .clickable(enabled = isUnlocked) { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isUnlocked) SlateCardLight else SlateSurfaceVariant
        ),
        border = if (isUnlocked) {
            BorderStroke(1.5.dp, GoldAccent.copy(alpha = 0.6f))
        } else {
            BorderStroke(1.dp, SlateCardBorder)
        },
        elevation = CardDefaults.cardElevation(if (isUnlocked) 3.dp else 0.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(8.dp)
            ) {
                if (isUnlocked) {
                    Text(
                        text = "Level $level",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = SlateButtonText,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    if (progress.questionsAnswered > 0) {
                        LinearProgressIndicator(
                            progress = { completionPercentage / 100f },
                            modifier = Modifier
                                .width(48.dp)
                                .height(4.dp)
                                .clip(CircleShape),
                            color = GoldAccent,
                            trackColor = SlateBackgroundTop.copy(alpha = 0.2f)
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "${progress.correctAnswers}/${progress.questionsAnswered}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = SlateButtonText.copy(alpha = 0.7f)
                        )
                    }

                    if (progress.bestScore > 0) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "Best Score",
                                tint = GoldAccent,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "${progress.bestScore}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = SlateButtonText
                            )
                        }
                    }
                } else {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Locked",
                        tint = SlateTextMuted,
                        modifier = Modifier.size(26.dp)
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Level $level",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = SlateTextMuted,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

