package com.example.faithquiz.ui.view.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import com.example.faithquiz.R
import com.example.faithquiz.data.store.ProgressDataStore
import com.example.faithquiz.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    navController: NavController
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // State for dialogs
    var showThemeDialog by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    
    // State for settings
    val currentTheme by ProgressDataStore.observeThemeMode(context).collectAsState(initial = "light")
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(DeepRoyalPurple, Color.Black)
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(Dimensions.screenPadding)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
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
                    text = "SETTINGS",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold,
                        color = GlowingGold
                    ),
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
            Spacer(modifier = Modifier.height(32.dp))

            // Appearance Section
            DivineSectionHeader("APPEARANCE")
            DivineSettingItem(
                title = "Theme",
                subtitle = "Current: ${currentTheme.capitalize()}",
                icon = Icons.Filled.DarkMode,
                onClick = { showThemeDialog = true }
            )
            
            Spacer(modifier = Modifier.height(24.dp))

            // Data Section
            DivineSectionHeader("DATA & STORAGE")
            DivineSettingItem(
                title = "Reset Progress",
                subtitle = "Clear all stats and achievements",
                icon = Icons.Filled.DeleteForever,
                iconTint = WrongAnswerRed,
                onClick = { showResetDialog = true }
            )

            Spacer(modifier = Modifier.height(24.dp))
            
            // About Section
            DivineSectionHeader("ABOUT")
            DivineSettingItem(
                title = "About Faith Quiz",
                subtitle = "Version 1.0.0",
                icon = Icons.Filled.Info,
                onClick = { showAboutDialog = true }
            )
            
            Spacer(modifier = Modifier.height(48.dp))
        }
    }
    
    // Theme Dialog
    if (showThemeDialog) {
        DivineDialog(
            title = "Select Theme",
            onDismiss = { showThemeDialog = false }
        ) {
            Column {
                DivineDialogOption("Light Mode", currentTheme == "light") {
                    scope.launch { ProgressDataStore.setThemeMode(context, "light") }
                    showThemeDialog = false
                }
                DivineDialogOption("Dark Mode", currentTheme == "dark") {
                    scope.launch { ProgressDataStore.setThemeMode(context, "dark") }
                    showThemeDialog = false
                }
            }
        }
    }
    
    // Reset Dialog
    if (showResetDialog) {
        DivineDialog(
            title = "Reset Progress?",
            onDismiss = { showResetDialog = false }
        ) {
            Column {
                Text(
                    text = "Are you sure you want to reset all your progress? This action cannot be undone.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.8f)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = { showResetDialog = false }) {
                        Text("CANCEL", color = GlowingGold)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { 
                            scope.launch { ProgressDataStore.reset(context) }
                            showResetDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = WrongAnswerRed)
                    ) {
                        Text("RESET", color = Color.White)
                    }
                }
            }
        }
    }
    
    // About Dialog
    if (showAboutDialog) {
        DivineDialog(
            title = "About Faith Quiz",
            onDismiss = { showAboutDialog = false }
        ) {
            Column {
                Text(
                    text = "Faith Quiz is designed to help you master biblical knowledge through engaging quizzes and challenges.\n\nCreated with faith and code.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.8f)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { showAboutDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = GlowingGold),
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("CLOSE", color = DeepRoyalPurple)
                }
            }
        }
    }
}

@Composable
fun DivineSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge.copy(
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp
        ),
        color = GlowingGold.copy(alpha = 0.8f),
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

@Composable
fun DivineSettingItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconTint: Color = GlowingGold,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onClick() }
            .border(1.dp, GlowingGold.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = EtherealGlass)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold
                    ),
                    color = Color.White
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = GlowingGold.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
fun DivineDialog(
    title: String,
    onDismiss: () -> Unit,
    content: @Composable () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DeepRoyalPurple.copy(alpha = 0.95f)),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, GlowingGold, RoundedCornerShape(16.dp))
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold
                    ),
                    color = GlowingGold
                )
                Spacer(modifier = Modifier.height(16.dp))
                content()
            }
        }
    }
}

@Composable
fun DivineDialogOption(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(
                selectedColor = GlowingGold,
                unselectedColor = Color.White.copy(alpha = 0.6f)
            )
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White
        )
    }
}

private fun String.capitalize(): String {
    return this.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
}