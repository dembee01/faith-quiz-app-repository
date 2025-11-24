package com.example.faithquiz.ui.view.levelselect

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.faithquiz.R
import com.example.faithquiz.ui.navigation.Screen
import com.example.faithquiz.ui.theme.Dimensions
import com.example.faithquiz.data.store.ProgressDataStore

@Composable
fun LevelSelectScreen(
	navController: NavController
) {
	val context = LocalContext.current
	val highestUnlocked by ProgressDataStore.observeHighestUnlockedLevel(context).collectAsState(initial = 1)

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
				.padding(Dimensions.screenMargin),
			horizontalAlignment = Alignment.CenterHorizontally
		) {
			// Header
			Row(
				modifier = Modifier.fillMaxWidth(),
				horizontalArrangement = Arrangement.SpaceBetween,
				verticalAlignment = Alignment.CenterVertically
			) {
				IconButton(onClick = { 
					if (!navController.popBackStack(Screen.MainMenu.route, false)) {
						navController.navigate(Screen.MainMenu.route) {
							popUpTo(Screen.MainMenu.route) { inclusive = false }
							launchSingleTop = true
						}
					}
				}) {
					Icon(
						imageVector = Icons.AutoMirrored.Filled.ArrowBack,
						contentDescription = stringResource(R.string.back_description),
						tint = MaterialTheme.colorScheme.onPrimary
					)
				}
				Text(
					text = stringResource(R.string.choose_level),
					style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
					color = MaterialTheme.colorScheme.onPrimary
				)
				Spacer(modifier = Modifier.width(Dimensions.iconSize))
			}

			Spacer(modifier = Modifier.height(Dimensions.spaceLarge))

			// Level Grid
			LazyVerticalGrid(
				columns = GridCells.Fixed(3),
				horizontalArrangement = Arrangement.spacedBy(Dimensions.spaceMedium),
				verticalArrangement = Arrangement.spacedBy(Dimensions.spaceMedium),
				modifier = Modifier.fillMaxSize()
			) {
				items(30) { level ->
					val levelNumber = level + 1
					val isUnlocked = levelNumber <= highestUnlocked
					LevelCard(
						level = levelNumber,
						isUnlocked = isUnlocked,
						onClick = {
							if (isUnlocked) {
								navController.navigate(Screen.Quiz.createRoute(levelNumber))
							}
						}
					)
				}
			}
		}
	}
}

@Composable
private fun LevelCard(
	level: Int,
	isUnlocked: Boolean,
	onClick: () -> Unit
) {
	Card(
		modifier = Modifier
			.fillMaxWidth()
			.clickable(enabled = isUnlocked) { onClick() },
		shape = RoundedCornerShape(Dimensions.cornerRadiusLarge),
		colors = CardDefaults.cardColors(
			containerColor = if (isUnlocked) {
				MaterialTheme.colorScheme.surface
			} else {
				MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
			}
		),
		elevation = CardDefaults.cardElevation(
			defaultElevation = if (isUnlocked) Dimensions.cardElevation else 0.dp
		)
	) {
		Box(
			modifier = Modifier
				.fillMaxWidth()
				.padding(Dimensions.paddingLarge),
			contentAlignment = Alignment.Center
		) {
			Column(
				horizontalAlignment = Alignment.CenterHorizontally
			) {
				if (isUnlocked) {
					Text(
						text = level.toString(),
						style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
						color = MaterialTheme.colorScheme.primary
					)
				} else {
					Icon(
						imageVector = Icons.Filled.Lock,
						contentDescription = stringResource(R.string.level_locked),
						tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
						modifier = Modifier.size(32.dp)
					)
				}
			}
		}
	}
}