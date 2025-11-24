package com.example.faithquiz.ui.view.splash

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.faithquiz.R
import com.example.faithquiz.ui.navigation.Screen
import com.example.faithquiz.ui.theme.Dimensions

@Composable
fun SplashScreen(
	navController: NavController
) {
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
			horizontalAlignment = Alignment.CenterHorizontally,
			verticalArrangement = Arrangement.Center
		) {
			Text(
				text = stringResource(R.string.faith_quiz),
				fontSize = 28.sp,
				fontWeight = FontWeight.Bold,
				color = MaterialTheme.colorScheme.onPrimary,
				textAlign = TextAlign.Center
			)
			Spacer(modifier = Modifier.height(Dimensions.spaceSmall))
			Text(
				text = stringResource(R.string.test_your_bible_knowledge),
				fontSize = 14.sp,
				color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
				textAlign = TextAlign.Center
			)
			Spacer(modifier = Modifier.height(Dimensions.spaceXXLarge))
			Button(
				onClick = {
					navController.navigate(Screen.MainMenu.route) {
						popUpTo(Screen.Splash.route) { inclusive = true }
						launchSingleTop = true
					}
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
				Text(
					text = stringResource(R.string.get_started),
					style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
				)
			}
		}
	}
}