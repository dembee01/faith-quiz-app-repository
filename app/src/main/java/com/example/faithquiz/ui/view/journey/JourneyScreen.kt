package com.example.faithquiz.ui.view.journey

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource


import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect

import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
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
    
    // Automatically scroll to the current level
    val listState = rememberLazyListState()
    LaunchedEffect(highestUnlocked) {
        // Scroll to the active level (approximate index)
        val index = (highestUnlocked - 1).coerceAtLeast(0)
        listState.animateScrollToItem(index)
    }

    DivineBackground {


        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = GlowingGold
                    )
                }
                Text(
                    text = "THE COVENANT JOURNEY",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    ),
                    color = GlowingGold,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
            
            // Journey Header Image
            Image(
                painter = painterResource(id = R.drawable.journey_map_header),
                contentDescription = "Covenant Journey Map",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(16.dp)),
                contentScale = ContentScale.Crop
            )
            
            Spacer(modifier = Modifier.height(8.dp))

            // Map List

            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 32.dp),
                reverseLayout = true // Start from bottom (Creation) going up? Or standard top-down?
                // Standard maps usually go BOTTOM to TOP (climbing). Let's try reverse layout.
                // Level 1 at bottom, Level 30 at top.
            ) {
                // Because we use reverseLayout, index 0 is at the bottom.
                // But our list is 1..30.
                // If we want Level 1 at bottom, we should provide list in REVERSE order if using standard layout,
                // OR use reverseLayout = true and provide list in NORMAL order (Level 1 first).
                // Let's use reverseLayout = true.
                
                itemsIndexed(JourneyData.levels) { index, node ->
                    val offsetRatio = when (node.level % 4) {

                        1 -> 0f
                        2 -> 0.6f
                        3 -> 0f
                        0 -> -0.6f
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
                                // Navigate to Quiz
                                // We need to add "journey" mode to QuizScreen navigation
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
    offsetRatio: Float, // -1 (Left) to 1 (Right)
    onNodeClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "JourneyNodePulse")

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isCurrent) 1.2f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Pulse"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp),
        contentAlignment = Alignment.Center
    ) {
        // Draw Path Connector
        // This is complex because we need to connect to the previous/next item's position.
        // For simplicity in this iteration, we'll draw a vertical curving path background?
        // Or just let the nodes float and add 'dashed lines' via Canvas later.
        // Let's draw a simple line to the "Center" for now to hint connection?
        // No, let's just place the nodes first.
        
        DrawingPathBackground(offsetRatio)

        // Node Content
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .align(Alignment.Center)
                .offset(x = (offsetRatio * 100).dp) // simplistic offset
                .clickable(enabled = isUnlocked, onClick = onNodeClick)
        ) {
            // Circle Node
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(if (isCurrent) 80.dp else 70.dp)
                    .scale(if (isCurrent) pulseScale else 1f)
                    .clip(CircleShape)
                    .background(
                        if (isUnlocked) Brush.radialGradient(
                            colors = listOf(GlowingGold, Color(0xFFDAA520))
                        ) else Brush.radialGradient(
                            colors = listOf(Color.Gray, Color.DarkGray)
                        )
                    )
                    .border(
                        width = 4.dp,
                        color = if (isUnlocked) Color.White else Color.Gray,
                        shape = CircleShape
                    )
            ) {
                if (isCompleted) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Completed",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                } else if (!isUnlocked) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Locked",
                        tint = Color.LightGray,
                        modifier = Modifier.size(32.dp)
                    )
                } else {
                    // Current / Unlocked but not completed
                    Text(
                        text = "${node.level}",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Serif
                        ),
                        color = Color.White
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Label
            Text(
                text = node.title,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Bold,
                    shadow = androidx.compose.ui.graphics.Shadow(
                        color = Color.Black,
                        blurRadius = 4f
                    )
                ),
                color = if (isUnlocked) GlowingGold else Color.Gray,
                textAlign = TextAlign.Center
            )
            
            // Description (shown only for unlocked levels)
            if (isUnlocked) {
                Text(
                    text = node.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.5f),
                    textAlign = TextAlign.Center
                )
            }

        }
    }
}

@Composable
fun DrawingPathBackground(offsetRatio: Float) {
    // This is a placeholder for the curve drawing. 
    // Implementing a continuous bezier curve across distinct lazy items is tricky.
    // We can draw a dashed line "towards" the center for now to simulate structure.
    Canvas(modifier = Modifier.fillMaxSize()) {
        val centerX = size.width / 2
        val nodeX = centerX + (offsetRatio * 100).dp.toPx()
        
        // Draw a subtle guide line
        drawLine(
            color = GlowingGold.copy(alpha = 0.2f),
            start = Offset(centerX, 0f),
            end = Offset(nodeX, size.height / 2),
            strokeWidth = 2.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
        )
        drawLine(
            color = GlowingGold.copy(alpha = 0.2f),
            start = Offset(nodeX, size.height / 2),
            end = Offset(centerX, size.height),
            strokeWidth = 2.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
        )
    }
}
