package com.example.faithquiz.ui.view.settings

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import com.example.faithquiz.data.store.ProgressDataStore
import com.example.faithquiz.ui.theme.*
import com.example.faithquiz.ui.view.components.DivineBackground
import com.example.faithquiz.util.CrashReporter
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    navController: NavController
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var showThemeDialog by remember { mutableStateOf(false) }
    var showTextScaleDialog by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }

    val currentTheme by ProgressDataStore.observeThemeMode(context).collectAsState(initial = "dark")
    val textScale by ProgressDataStore.observeTextScale(context).collectAsState(initial = "normal")
    val reduceMotion by ProgressDataStore.observeReduceMotion(context).collectAsState(initial = false)
    val crashReporting by ProgressDataStore.observeCrashReportingEnabled(context).collectAsState(initial = false)

    DivineBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 22.dp, vertical = 16.dp)
                .verticalScroll(rememberScrollState())
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
                    text = "SETTINGS",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = SlateTextPrimary,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
            Spacer(modifier = Modifier.height(24.dp))

            // Appearance Section
            SlateSectionHeader("APPEARANCE")
            SlateSettingItem(
                title = "Theme",
                subtitle = "Current: ${currentTheme.capitalize()}",
                icon = Icons.Filled.DarkMode,
                onClick = { showThemeDialog = true }
            )

            Spacer(modifier = Modifier.height(20.dp))
            SlateSectionHeader("ACCESSIBILITY")
            SlateSettingItem(
                title = "Text size",
                subtitle = if (textScale == "large") "Large" else "Standard",
                icon = Icons.Filled.TextFields,
                onClick = { showTextScaleDialog = true }
            )
            SlateSwitchSettingItem(
                title = "Reduce motion",
                subtitle = "Show content without typing and entrance animations",
                icon = Icons.Filled.MotionPhotosOff,
                checked = reduceMotion,
                onCheckedChange = { enabled ->
                    scope.launch { ProgressDataStore.setReduceMotion(context, enabled) }
                }
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Data Section
            SlateSectionHeader("DATA & STORAGE")
            SlateSettingItem(
                title = "Reset Progress",
                subtitle = "Clear all stats and achievements",
                icon = Icons.Filled.DeleteForever,
                iconTint = WrongAnswerRed,
                onClick = { showResetDialog = true }
            )

            SlateSwitchSettingItem(
                title = "Crash reporting",
                subtitle = if (CrashReporter.isConfigured) {
                    "Privately send technical crash details; no personal data"
                } else {
                    "Unavailable until a Sentry DSN is supplied at build time"
                },
                icon = Icons.Filled.BugReport,
                checked = crashReporting && CrashReporter.isConfigured,
                enabled = CrashReporter.isConfigured,
                onCheckedChange = { enabled ->
                    scope.launch { ProgressDataStore.setCrashReportingEnabled(context, enabled) }
                }
            )

            Spacer(modifier = Modifier.height(20.dp))

            // About Section
            SlateSectionHeader("ABOUT")
            SlateSettingItem(
                title = "About Faith Quiz",
                subtitle = "Version 1.0.0",
                icon = Icons.Filled.Info,
                onClick = { showAboutDialog = true }
            )

            Spacer(modifier = Modifier.height(40.dp))
        }
    }

    if (showThemeDialog) {
        SlateDialog(
            title = "Select Theme",
            onDismiss = { showThemeDialog = false }
        ) {
            Column {
                SlateDialogOption("Dark Mode (Slate Indigo)", currentTheme == "dark") {
                    scope.launch { ProgressDataStore.setThemeMode(context, "dark") }
                    showThemeDialog = false
                }
                SlateDialogOption("Light Mode", currentTheme == "light") {
                    scope.launch { ProgressDataStore.setThemeMode(context, "light") }
                    showThemeDialog = false
                }
            }
        }
    }

    if (showTextScaleDialog) {
        SlateDialog(
            title = "Select Text Size",
            onDismiss = { showTextScaleDialog = false }
        ) {
            Column {
                SlateDialogOption("Standard", textScale == "normal") {
                    scope.launch { ProgressDataStore.setTextScale(context, "normal") }
                    showTextScaleDialog = false
                }
                SlateDialogOption("Large", textScale == "large") {
                    scope.launch { ProgressDataStore.setTextScale(context, "large") }
                    showTextScaleDialog = false
                }
            }
        }
    }

    if (showResetDialog) {
        SlateDialog(
            title = "Reset Progress?",
            onDismiss = { showResetDialog = false }
        ) {
            Column {
                Text(
                    text = "Are you sure you want to reset all your progress? This action cannot be undone.",
                    fontSize = 14.sp,
                    color = SlateTextSecondary
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = { showResetDialog = false }) {
                        Text("CANCEL", color = GoldAccent, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            scope.launch { ProgressDataStore.reset(context) }
                            showResetDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = WrongAnswerRed),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Text("RESET", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (showAboutDialog) {
        SlateDialog(
            title = "About Faith Quiz",
            onDismiss = { showAboutDialog = false }
        ) {
            Column {
                Text(
                    text = "Faith Quiz is designed to help you master biblical knowledge through engaging quizzes and challenges.\n\nCreated with faith and code.",
                    fontSize = 14.sp,
                    color = SlateTextSecondary
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { showAboutDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = SlateCardLight, contentColor = SlateButtonText),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("CLOSE", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun SlateSectionHeader(title: String) {
    Text(
        text = title,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.5.sp,
        color = GoldAccent,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

@Composable
fun SlateSettingItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconTint: Color = GoldAccent,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SlateSurface)
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
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = SlateTextPrimary
                )
                Text(
                    text = subtitle,
                    fontSize = 13.sp,
                    color = SlateTextSecondary
                )
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = SlateTextMuted
            )
        }
    }
}

@Composable
fun SlateDialog(
    title: String,
    onDismiss: () -> Unit,
    content: @Composable () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = SlateSurface),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, SlateCardBorder, RoundedCornerShape(24.dp))
        ) {
            Column(modifier = Modifier.padding(22.dp)) {
                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = SlateTextPrimary
                )
                Spacer(modifier = Modifier.height(16.dp))
                content()
            }
        }
    }
}

@Composable
fun SlateDialogOption(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(
                selectedColor = GoldAccent,
                unselectedColor = SlateTextMuted
            )
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            fontSize = 15.sp,
            color = SlateTextPrimary
        )
    }
}

private fun String.capitalize(): String {
    return this.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
}

@Composable
fun SlateSwitchSettingItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SlateSurface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = enabled) { onCheckedChange(!checked) }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (enabled) GoldAccent else SlateTextMuted,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = SlateTextPrimary)
                Text(subtitle, fontSize = 13.sp, color = SlateTextSecondary)
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                enabled = enabled,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = SlateBackgroundBottom,
                    checkedTrackColor = GoldAccent
                )
            )
        }
    }
}

