package com.example.faithquiz.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.core.view.WindowCompat
import com.example.faithquiz.data.store.ProgressDataStore

private val SlateColorScheme = darkColorScheme(
    primary = SlateBackgroundTop,
    secondary = GoldAccent,
    tertiary = PrimaryBlueAccent,
    background = SlateBackgroundBottom,
    surface = SlateSurface,
    error = WrongAnswerRed,
    onPrimary = androidx.compose.ui.graphics.Color.White,
    onSecondary = SlateButtonText,
    onTertiary = androidx.compose.ui.graphics.Color.White,
    onBackground = SlateTextPrimary,
    onSurface = SlateTextPrimary,
    onError = androidx.compose.ui.graphics.Color.White,
    surfaceVariant = SlateSurfaceVariant,
    onSurfaceVariant = SlateTextSecondary
)

@Composable
fun FaithQuizTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val storedMode by ProgressDataStore.observeThemeMode(context).collectAsState(initial = "dark")
    val textScale by ProgressDataStore.observeTextScale(context).collectAsState(initial = "normal")

    val colorScheme = SlateColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    val currentDensity = LocalDensity.current
    val preferredFontScale = if (textScale == "large") 1.2f else 1f
    CompositionLocalProvider(
        LocalDensity provides Density(
            density = currentDensity.density,
            fontScale = currentDensity.fontScale * preferredFontScale
        )
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
