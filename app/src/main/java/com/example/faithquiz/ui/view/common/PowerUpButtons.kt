package com.example.faithquiz.ui.view.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.faithquiz.data.model.PowerUp
import com.example.faithquiz.data.model.PowerUpType

@Composable
fun PowerUpButtons(
    availablePowerUps: List<PowerUp>,
    onFiftyFiftyClick: () -> Unit,
    onSkipClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        val fiftyFiftyCount = availablePowerUps.count { it.type == PowerUpType.FIFTY_FIFTY }
        val skipCount = availablePowerUps.count { it.type == PowerUpType.SKIP_QUESTION }
        
        PowerUpButton(
            type = PowerUpType.FIFTY_FIFTY,
            count = fiftyFiftyCount,
            onClick = onFiftyFiftyClick,
            modifier = Modifier.weight(1f)
        )
        
        Spacer(modifier = Modifier.width(8.dp))
        
        PowerUpButton(
            type = PowerUpType.SKIP_QUESTION,
            count = skipCount,
            onClick = onSkipClick,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun PowerUpButton(
    type: PowerUpType,
    count: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (icon, text, color) = when (type) {
        PowerUpType.FIFTY_FIFTY -> Triple(
            Icons.Default.CropSquare,
            "50:50",
            Color(0xFF2196F3) // Blue
        )
        PowerUpType.SKIP_QUESTION -> Triple(
            Icons.Default.SkipNext,
            "Skip",
            Color(0xFFFF9800) // Orange
        )
        PowerUpType.EXTRA_TIME -> Triple(
            Icons.Default.Timer,
            "Time",
            Color(0xFF4CAF50) // Green
        )
        PowerUpType.HINT -> Triple(
            Icons.Default.Lightbulb,
            "Hint",
            Color(0xFF9C27B0) // Purple
        )
    }
    
    Card(
        modifier = modifier
            .height(80.dp)
            .clickable(enabled = count > 0) { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (count > 0) {
                color.copy(alpha = 0.1f)
            } else {
                MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
            }
        ),
        border = if (count > 0) {
            androidx.compose.foundation.BorderStroke(2.dp, color)
        } else null
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = text,
                tint = if (count > 0) color else Color.Gray,
                modifier = Modifier.size(24.dp)
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = if (count > 0) color else Color.Gray,
                textAlign = TextAlign.Center
            )
            
            if (count > 0) {
                Spacer(modifier = Modifier.height(2.dp))
                
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(color),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = count.toString(),
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun StreakBonusIndicator(
    streakBonus: Int,
    modifier: Modifier = Modifier
) {
    if (streakBonus > 0) {
        Card(
            modifier = modifier,
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF4CAF50).copy(alpha = 0.9f)
            )
        ) {
            Row(
                modifier = Modifier.padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = "Streak Bonus",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
                
                Spacer(modifier = Modifier.width(4.dp))
                
                Text(
                    text = "+$streakBonus Streak Bonus!",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = Color.White
                )
            }
        }
    }
}

@Composable
fun DailyChallengeIndicator(
    isAvailable: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (isAvailable) {
        Card(
            modifier = modifier
                .clickable { onClick() },
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF9C27B0).copy(alpha = 0.9f)
            )
        ) {
            Row(
                modifier = Modifier.padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.EmojiEvents,
                    contentDescription = "Daily Challenge",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
                
                Spacer(modifier = Modifier.width(4.dp))
                
                Text(
                    text = "Daily Challenge Available!",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = Color.White
                )
            }
        }
    }
}
