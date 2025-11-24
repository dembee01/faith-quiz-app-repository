package com.example.faithquiz.ui.view.dailychallenge

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.faithquiz.R
import com.example.faithquiz.ui.theme.Dimensions
import androidx.compose.ui.platform.LocalContext
import com.example.faithquiz.data.store.ProgressDataStore
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import com.example.faithquiz.ui.navigation.Screen

@Composable
fun DailyChallengeScreen(
    navController: NavController
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
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
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { navController.popBackStack() }
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.back_description),
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
                
                Text(
                    text = stringResource(R.string.daily_challenge),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onPrimary
                )
                
                Spacer(modifier = Modifier.width(Dimensions.iconSize))
            }
            
            Spacer(modifier = Modifier.height(Dimensions.spaceLarge))
            
            // Daily Devotion Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(Dimensions.cornerRadiusLarge),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(Dimensions.paddingLarge),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = stringResource(R.string.daily_challenge_description),
                        modifier = Modifier.size(Dimensions.iconSizeXLarge),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    
                    Spacer(modifier = Modifier.height(Dimensions.spaceLarge))
                    
                    Text(
                        text = stringResource(R.string.daily_challenge),
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(Dimensions.spaceMedium))
                    
                    Text(
                        text = stringResource(R.string.daily_challenge_description),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(Dimensions.spaceMedium))

                    Button(onClick = {
                        // Mark devotion completion and navigate to a quick 1-question quiz (level 1)
                        scope.launch {
                            ProgressDataStore.recordDevotionCompletion(context)
                            navController.navigate(Screen.Quiz.createRoute(1, "practice"))
                        }
                    }) {
                        Text(text = stringResource(id = R.string.continue_button))
                    }
                }
            }
        }
    }
}
