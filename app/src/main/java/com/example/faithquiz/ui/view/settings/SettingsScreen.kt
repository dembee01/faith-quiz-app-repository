package com.example.faithquiz.ui.view.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.faithquiz.R
import com.example.faithquiz.ui.theme.Dimensions
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
	navController: NavController
) {
	val context = LocalContext.current
	val scope = rememberCoroutineScope()
	
	// State for dialogs
	var showFontSizeDialog by remember { mutableStateOf(false) }
	var showThemeDialog by remember { mutableStateOf(false) }
	var showNotificationsDialog by remember { mutableStateOf(false) }
	var showProfileDialog by remember { mutableStateOf(false) }
	var showSubscriptionDialog by remember { mutableStateOf(false) }
	
	// State for settings
	var currentFontSize by remember { mutableStateOf("Medium") }
	var currentTheme by remember { mutableStateOf("System") }
	var notificationsEnabled by remember { mutableStateOf(true) }

	// TODO: Wire these to actual theming/typography in FaithQuizTheme if needed
	
	Scaffold(
		containerColor = MaterialTheme.colorScheme.background,
		bottomBar = {
			NavigationBar {
				NavigationBarItem(selected = false, onClick = { navController.navigate(com.example.faithquiz.ui.navigation.Screen.MainMenu.route) }, icon = { Icon(Icons.Filled.Home, null) }, label = { Text(stringResource(id = R.string.back_to_menu)) })
				NavigationBarItem(selected = false, onClick = { navController.navigate(com.example.faithquiz.ui.navigation.Screen.LevelSelect.route) }, icon = { Icon(Icons.AutoMirrored.Filled.List, null) }, label = { Text(stringResource(id = R.string.quizzes_title)) })
				NavigationBarItem(selected = false, onClick = { navController.navigate(com.example.faithquiz.ui.navigation.Screen.Results.route) }, icon = { Icon(Icons.Filled.PresentToAll, null) }, label = { Text(stringResource(id = R.string.results)) })
				NavigationBarItem(selected = true, onClick = { /* here */ }, icon = { Icon(Icons.Filled.Settings, null) }, label = { Text(stringResource(id = R.string.settings)) })
			}
		}
	) { innerPadding ->
		Column(
			modifier = Modifier
				.fillMaxSize()
				.padding(innerPadding)
				.background(MaterialTheme.colorScheme.background)
				.verticalScroll(rememberScrollState())
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
					Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(id = R.string.back_description), tint = MaterialTheme.colorScheme.onSurface)
				}
				Text(text = stringResource(id = R.string.settings), style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
				Spacer(modifier = Modifier.width(Dimensions.iconSize))
			}

			// Account section
			SectionHeader(stringResource(id = R.string.account))
			SettingRowProfile(
				imageUrl = "https://lh3.googleusercontent.com/aida-public/AB6AXuBqbmS7TrWFsdXWtNH8U9DXdvr7uyL48urmYUUTJC0q4_GaUpC8OCWwrafuB7pY3iIAwj2c0CEGOusshoEr0GLyksmXYpnbV8kBxHKtcCZWXcFHfaTDUsD-a_XTcljA3jZjTXhfFZam6zrzRyBvuff-TiOB-AXftHWp7Epga2aCtbVRCVBd69ckHRYwIsLm0hkzCMq5l2E1TDdVx6mwvb8ZAy2OG8lTGS-JRlJUGvb975t56UkmC0Mq_9YSGazM50SLFNtytQadX9fg",
				title = stringResource(id = R.string.profile),
				subtitle = "View and edit your profile information",
				onClick = { showProfileDialog = true }
			)
			SettingRowIcon(
				icon = Icons.Filled.Star,
				title = stringResource(id = R.string.subscription),
				subtitle = "Manage your subscription and billing",
				onClick = { showSubscriptionDialog = true }
			)

			// Preferences (Theme toggle + Notifications)
			SectionHeader(stringResource(id = R.string.preferences))
			SettingRowIcon(
				icon = Icons.Filled.WbSunny,
				title = stringResource(id = R.string.theme),
				subtitle = "Current: $currentTheme",
				onClick = { showThemeDialog = true }
			)
			SettingRowIcon(
				icon = Icons.Filled.Notifications,
				title = stringResource(id = R.string.notifications),
				subtitle = if (notificationsEnabled) "Enabled" else "Disabled",
				onClick = { showNotificationsDialog = true }
			)
			
			Spacer(modifier = Modifier.height(Dimensions.spaceLarge))
		}
	}
	
	// Theme Dialog
	if (showThemeDialog) {
		ThemeDialog(
			currentTheme = currentTheme,
			onThemeSelected = { 
				currentTheme = it
				scope.launch { com.example.faithquiz.data.store.ProgressDataStore.setThemeMode(context, if (it == "Dark") "dark" else "light") }
				showThemeDialog = false
			},
			onDismiss = { showThemeDialog = false }
		)
	}
	
	// Notifications Dialog
	if (showNotificationsDialog) {
		NotificationsDialog(
			enabled = notificationsEnabled,
			onToggle = { 
				notificationsEnabled = it
				showNotificationsDialog = false
			},
			onDismiss = { showNotificationsDialog = false }
		)
	}
	
	// Profile Dialog
	if (showProfileDialog) {
		ProfileDialog(
			onDismiss = { showProfileDialog = false }
		)
	}
	
	// Subscription Dialog
	if (showSubscriptionDialog) {
		SubscriptionDialog(
			onDismiss = { showSubscriptionDialog = false }
		)
	}
}

@Composable
private fun FontSizeDialog(
	currentSize: String,
	onSizeSelected: (String) -> Unit,
	onDismiss: () -> Unit
) {
	Dialog(onDismissRequest = onDismiss) {
		Card(
			modifier = Modifier
				.fillMaxWidth()
				.padding(Dimensions.screenPadding),
			shape = RoundedCornerShape(Dimensions.cornerRadiusLarge)
		) {
			Column(
				modifier = Modifier.padding(Dimensions.paddingLarge)
			) {
				Text(
					text = "Font Size",
					style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
					color = MaterialTheme.colorScheme.onSurface
				)
				Spacer(modifier = Modifier.height(Dimensions.spaceMedium))
				
				listOf("Small", "Medium", "Large", "Extra Large").forEach { size ->
					Row(
						modifier = Modifier
							.fillMaxWidth()
							.clickable { onSizeSelected(size) }
							.padding(vertical = Dimensions.spaceSmall),
						verticalAlignment = Alignment.CenterVertically
					) {
						RadioButton(
							selected = currentSize == size,
							onClick = { onSizeSelected(size) }
						)
						Spacer(modifier = Modifier.width(Dimensions.spaceSmall))
						Text(
							text = size,
							style = MaterialTheme.typography.bodyLarge,
							color = MaterialTheme.colorScheme.onSurface
						)
					}
				}
				
				Spacer(modifier = Modifier.height(Dimensions.spaceMedium))
				Button(
					onClick = onDismiss,
					modifier = Modifier.fillMaxWidth(),
					shape = RoundedCornerShape(Dimensions.cornerRadiusMedium)
				) {
					Text("Cancel")
				}
			}
		}
	}
}

@Composable
private fun ThemeDialog(
	currentTheme: String,
	onThemeSelected: (String) -> Unit,
	onDismiss: () -> Unit
) {
	Dialog(onDismissRequest = onDismiss) {
		Card(
			modifier = Modifier
				.fillMaxWidth()
				.padding(Dimensions.screenPadding),
			shape = RoundedCornerShape(Dimensions.cornerRadiusLarge)
		) {
			Column(
				modifier = Modifier.padding(Dimensions.paddingLarge)
			) {
				Text(
					text = "Theme",
					style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
					color = MaterialTheme.colorScheme.onSurface
				)
				Spacer(modifier = Modifier.height(Dimensions.spaceMedium))
				
				listOf("Light", "Dark", "System").forEach { theme ->
					Row(
						modifier = Modifier
							.fillMaxWidth()
							.clickable { onThemeSelected(theme) }
							.padding(vertical = Dimensions.spaceSmall),
						verticalAlignment = Alignment.CenterVertically
					) {
						RadioButton(
							selected = currentTheme == theme,
							onClick = { onThemeSelected(theme) }
						)
						Spacer(modifier = Modifier.width(Dimensions.spaceSmall))
						Text(
							text = theme,
							style = MaterialTheme.typography.bodyLarge,
							color = MaterialTheme.colorScheme.onSurface
						)
					}
				}
				
				Spacer(modifier = Modifier.height(Dimensions.spaceMedium))
				Button(
					onClick = onDismiss,
					modifier = Modifier.fillMaxWidth(),
					shape = RoundedCornerShape(Dimensions.cornerRadiusMedium)
				) {
					Text("Cancel")
				}
			}
		}
	}
}

@Composable
private fun NotificationsDialog(
	enabled: Boolean,
	onToggle: (Boolean) -> Unit,
	onDismiss: () -> Unit
) {
	Dialog(onDismissRequest = onDismiss) {
		Card(
			modifier = Modifier
				.fillMaxWidth()
				.padding(Dimensions.screenPadding),
			shape = RoundedCornerShape(Dimensions.cornerRadiusLarge)
		) {
			Column(
				modifier = Modifier.padding(Dimensions.paddingLarge)
			) {
				Text(
					text = "Notifications",
					style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
					color = MaterialTheme.colorScheme.onSurface
				)
				Spacer(modifier = Modifier.height(Dimensions.spaceMedium))
				
				Text(
					text = "Stay updated with daily challenges, achievements, and new content.",
					style = MaterialTheme.typography.bodyMedium,
					color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
				)
				Spacer(modifier = Modifier.height(Dimensions.spaceMedium))
				
				Row(
					modifier = Modifier.fillMaxWidth(),
					horizontalArrangement = Arrangement.SpaceBetween,
					verticalAlignment = Alignment.CenterVertically
				) {
					Text(
						text = "Enable Notifications",
						style = MaterialTheme.typography.bodyLarge,
						color = MaterialTheme.colorScheme.onSurface
					)
					Switch(
						checked = enabled,
						onCheckedChange = onToggle
					)
				}
				
				Spacer(modifier = Modifier.height(Dimensions.spaceMedium))
				Button(
					onClick = onDismiss,
					modifier = Modifier.fillMaxWidth(),
					shape = RoundedCornerShape(Dimensions.cornerRadiusMedium)
				) {
					Text("Done")
				}
			}
		}
	}
}

@Composable
private fun ProfileDialog(
	onDismiss: () -> Unit
) {
	Dialog(onDismissRequest = onDismiss) {
		Card(
			modifier = Modifier
				.fillMaxWidth()
				.padding(Dimensions.screenPadding),
			shape = RoundedCornerShape(Dimensions.cornerRadiusLarge)
		) {
			Column(
				modifier = Modifier.padding(Dimensions.paddingLarge)
			) {
				Text(
					text = "Profile",
					style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
					color = MaterialTheme.colorScheme.onSurface
				)
				Spacer(modifier = Modifier.height(Dimensions.spaceMedium))
				
				Text(
					text = "Profile management features coming soon!",
					style = MaterialTheme.typography.bodyMedium,
					color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
					textAlign = TextAlign.Center
				)
				
				Spacer(modifier = Modifier.height(Dimensions.spaceMedium))
				Button(
					onClick = onDismiss,
					modifier = Modifier.fillMaxWidth(),
					shape = RoundedCornerShape(Dimensions.cornerRadiusMedium)
				) {
					Text("OK")
				}
			}
		}
	}
}

@Composable
private fun SubscriptionDialog(
	onDismiss: () -> Unit
) {
	Dialog(onDismissRequest = onDismiss) {
		Card(
			modifier = Modifier
				.fillMaxWidth()
				.padding(Dimensions.screenPadding),
			shape = RoundedCornerShape(Dimensions.cornerRadiusLarge)
		) {
			Column(
				modifier = Modifier.padding(Dimensions.paddingLarge)
			) {
				Text(
					text = "Subscription",
					style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
					color = MaterialTheme.colorScheme.onSurface
				)
				Spacer(modifier = Modifier.height(Dimensions.spaceMedium))
				
				Text(
					text = "Subscription management features coming soon!",
					style = MaterialTheme.typography.bodyMedium,
					color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
					textAlign = TextAlign.Center
				)
				
				Spacer(modifier = Modifier.height(Dimensions.spaceMedium))
				Button(
					onClick = onDismiss,
					modifier = Modifier.fillMaxWidth(),
					shape = RoundedCornerShape(Dimensions.cornerRadiusMedium)
				) {
					Text("OK")
				}
			}
		}
	}
}

@Composable
private fun SectionHeader(title: String) {
	Text(
		text = title,
		style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
		color = MaterialTheme.colorScheme.onSurface,
		modifier = Modifier.padding(horizontal = Dimensions.screenPadding, vertical = Dimensions.spaceMedium)
	)
}

@Composable
private fun SettingRowProfile(
	imageUrl: String, 
	title: String, 
	subtitle: String,
	onClick: () -> Unit
) {
	Row(
		modifier = Modifier
			.fillMaxWidth()
			.clickable { onClick() }
			.padding(horizontal = Dimensions.screenPadding, vertical = Dimensions.spaceSmall),
		verticalAlignment = Alignment.CenterVertically
	) {
		AsyncImage(
			model = imageUrl,
			contentDescription = title,
			modifier = Modifier.size(56.dp).background(Color.Transparent, shape = RoundedCornerShape(28.dp))
		)
		Spacer(modifier = Modifier.width(Dimensions.spaceMedium))
		Column(modifier = Modifier.weight(1f)) {
			Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
			Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Color(0xFF637588))
		}
		Icon(
			Icons.Filled.ChevronRight,
			contentDescription = null,
			tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
		)
	}
}

@Composable
private fun SettingRowIcon(
	icon: ImageVector, 
	title: String, 
	subtitle: String,
	onClick: () -> Unit
) {
	Row(
		modifier = Modifier
			.fillMaxWidth()
			.clickable { onClick() }
			.padding(horizontal = Dimensions.screenPadding, vertical = Dimensions.spaceSmall),
		verticalAlignment = Alignment.CenterVertically
	) {
		Box(
			modifier = Modifier.size(48.dp).background(Color(0xFFF0F2F4), shape = RoundedCornerShape(12.dp)),
			contentAlignment = Alignment.Center
		) {
			Icon(icon, contentDescription = title, tint = MaterialTheme.colorScheme.onSurface)
		}
		Spacer(modifier = Modifier.width(Dimensions.spaceMedium))
		Column(modifier = Modifier.weight(1f)) {
			Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
			Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Color(0xFF637588))
		}
		Icon(
			Icons.Filled.ChevronRight,
			contentDescription = null,
			tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
		)
	}
}