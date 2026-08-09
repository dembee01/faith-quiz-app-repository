package com.example.faithquiz.ui.theme

import androidx.compose.ui.graphics.Color

// Canonical Slate Indigo Design Tokens (Reference Screenshot Palette)
val SlateBackgroundTop = Color(0xFF474E76)
val SlateBackgroundBottom = Color(0xFF2C314E)
val SlateSurface = Color(0xFF383E62)
val SlateSurfaceVariant = Color(0xFF2F3452)
val SlateCardLight = Color(0xFFF6F7FB)
val SlateCardBorder = Color(0x22FFFFFF)

// Typography & Text Colors
val SlateTextPrimary = Color(0xFFFFFFFF)
val SlateTextSecondary = Color(0xFFC5CAE9)
val SlateTextMuted = Color(0xFF9AA0C7)
val SlateButtonText = Color(0xFF383E62)

// Accent & Highlights
val GoldAccent = Color(0xFFFBBF24)
val GoldAccentDark = Color(0xFFD97706)
val PrimaryBlueAccent = Color(0xFF60A5FA)

// Quiz Feedback & State Colors
val CorrectAnswerGreen = Color(0xFF10B981)
val CorrectAnswerGreenLight = Color(0xFF34D399)
val WrongAnswerRed = Color(0xFFEF4444)
val WrongAnswerRedLight = Color(0xFFF87171)
val SelectedOptionSlate = Color(0xFF474E76)

// Legacy compatibility fallbacks mapped to Slate Indigo Theme
val Primary = SlateBackgroundTop
val PrimaryDark = SlateBackgroundBottom
val PrimaryLight = PrimaryBlueAccent
val Accent = GoldAccent
val AccentDark = GoldAccentDark
val LightBlue = Color(0xFFE0E7FF)
val DarkBlue = SlateBackgroundBottom

val Background = SlateBackgroundBottom
val Surface = SlateSurface
val Error = WrongAnswerRed
val Success = CorrectAnswerGreen
val Warning = GoldAccent
val TextPrimary = SlateTextPrimary
val TextSecondary = SlateTextSecondary
val TextHint = SlateTextMuted
val Divider = SlateCardBorder
val CardBackground = SlateSurface

val LevelLocked = Color(0xFF64748B)
val LevelUnlocked = CorrectAnswerGreen
val LevelCurrent = GoldAccent

val BackgroundDark = SlateBackgroundBottom
val SurfaceDark = SlateSurface
val TextPrimaryDark = SlateTextPrimary
val TextSecondaryDark = SlateTextSecondary
val DividerDark = SlateCardBorder
val CardBackgroundDark = SlateSurface


