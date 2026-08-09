package com.example.faithquiz.ui.view.journey

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.faithquiz.R
import com.example.faithquiz.data.JourneyData
import com.example.faithquiz.data.JourneyNode
import com.example.faithquiz.data.store.ProgressDataStore
import com.example.faithquiz.ui.navigation.Screen
import com.example.faithquiz.ui.theme.*
import com.example.faithquiz.ui.view.components.DivineBackground

@Composable
fun JourneyScreen(navController: NavController) {
    val context = LocalContext.current
    val highestUnlocked by ProgressDataStore.observeHighestUnlockedLevel(context).collectAsState(initial = 1)

    val listState = rememberLazyListState()
    LaunchedEffect(highestUnlocked) {
        val index = (highestUnlocked - 1).coerceAtLeast(0)
        listState.animateScrollToItem(index)
    }

    DivineBackground {
        Column(modifier = Modifier.fillMaxSize()) {
            Spacer(modifier = Modifier.height(8.dp))
            // Header Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
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
                    text = "THE COVENANT JOURNEY",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = SlateTextPrimary,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            // Journey Banner
            Image(
                painter = painterResource(id = R.drawable.journey_map_header),
                contentDescription = "Covenant Journey Map",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(20.dp)),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Map List
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 32.dp),
                reverseLayout = true
            ) {
                items(JourneyData.levels) { node ->
                    val offsetRatio = when (node.level % 4) {
                        1 -> 0f
                        2 -> 0.55f
                        3 -> 0f
                        0 -> -0.55f
                        else -> 0f
                    }

                    JourneyNodeItem(
                        node = node,
                        isUnlocked = node.level <= highestUnlocked,
                        isCurrent = node.level == highestUnlocked,
                        isCompleted = node.level < highestUnlocked,
                        offsetRatio = offsetRatio,
                        onNodeClick = {
                            if (node.level <= highestUnlocked) {
                                navController.navigate(Screen.Quiz.createRoute(node.level, "journey"))
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun JourneyNodeItem(
    node: JourneyNode,
    isUnlocked: Boolean,
    isCurrent: Boolean,
    isCompleted: Boolean,
    offsetRatio: Float,
    onNodeClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "JourneyNodePulse")

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isCurrent) 1.15f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Pulse"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(136.dp),
        contentAlignment = Alignment.Center
    ) {
        DrawingPathBackground(offsetRatio)

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .align(Alignment.Center)
                .offset(x = (offsetRatio * 90).dp)
                .clickable(enabled = isUnlocked, onClick = onNodeClick)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(if (isCurrent) 76.dp else 68.dp)
                    .scale(if (isCurrent) pulseScale else 1f)
                    .clip(CircleShape)
                    .background(
                        if (isUnlocked) Brush.radialGradient(
                            colors = listOf(GoldAccent, GoldAccentDark)
                        ) else Brush.radialGradient(
                            colors = listOf(SlateSurfaceVariant, SlateBackgroundBottom)
                        )
                    )
                    .border(
                        width = 3.dp,
                        color = if (isUnlocked) SlateCardLight else SlateCardBorder,
                        shape = CircleShape
                    )
            ) {
                if (isCompleted) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Completed",
                        tint = SlateButtonText,
                        modifier = Modifier.size(30.dp)
                    )
                } else if (!isUnlocked) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Locked",
                        tint = SlateTextMuted,
                        modifier = Modifier.size(28.dp)
                    )
                } else {
                    Text(
                        text = "${node.level}",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = SlateButtonText
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = node.title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = if (isUnlocked) GoldAccent else SlateTextMuted,
                textAlign = TextAlign.Center
            )

            if (isUnlocked) {
                Text(
                    text = node.description,
                    fontSize = 12.sp,
                    color = SlateTextSecondary,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun DrawingPathBackground(offsetRatio: Float) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val centerX = size.width / 2
        val nodeX = centerX + (offsetRatio * 90).dp.toPx()

        drawLine(
            color = GoldAccent.copy(alpha = 0.25f),
            start = Offset(centerX, 0f),
            end = Offset(nodeX, size.height / 2),
            strokeWidth = 2.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
        )
        drawLine(
            color = GoldAccent.copy(alpha = 0.25f),
            start = Offset(nodeX, size.height / 2),
            end = Offset(centerX, size.height),
            strokeWidth = 2.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
        )
    }
}

