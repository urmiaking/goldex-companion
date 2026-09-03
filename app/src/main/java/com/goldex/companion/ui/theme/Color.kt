package com.goldex.companion.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Minimalist Luxury Gold Palette
val GoldPrimaryDark = Color(0xFFDFB35A)         // Champagne Gold for Dark
val GoldSecondaryDark = Color(0xFFC7983B)
val GoldContainerDark = Color(0x24DFB35A)
val GoldBorderDark = Color(0x4DDFB35A)

val GoldPrimaryLight = Color(0xFFB8860B)        // Deep Rich Gold for Light Mode
val GoldSecondaryLight = Color(0xFF946A04)
val GoldContainerLight = Color(0x1FB8860B)
val GoldBorderLight = Color(0x40B8860B)

@Immutable
data class GoldExAppColors(
    val isDark: Boolean,
    val background: Color,
    val surface: Color,
    val surfaceElevated: Color,
    val surfaceVariant: Color,
    val border: Color,
    val goldPrimary: Color,
    val goldSecondary: Color,
    val goldContainer: Color,
    val goldBorder: Color,
    val textMain: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val profitGreen: Color,
    val errorRed: Color
)

val DarkGoldExColors = GoldExAppColors(
    isDark = true,
    background = Color(0xFF0A0B0E),
    surface = Color(0xFF13161F),
    surfaceElevated = Color(0xFF191D2A),
    surfaceVariant = Color(0xFF1E2333),
    border = Color(0xFF242938),
    goldPrimary = GoldPrimaryDark,
    goldSecondary = GoldSecondaryDark,
    goldContainer = GoldContainerDark,
    goldBorder = GoldBorderDark,
    textMain = Color(0xFFF3F4F8),
    textSecondary = Color(0xFF9EA6B8),
    textMuted = Color(0xFF676E7E),
    profitGreen = Color(0xFF10B981),
    errorRed = Color(0xFFFB7185)
)

val LightGoldExColors = GoldExAppColors(
    isDark = false,
    background = Color(0xFFF6F8FA),
    surface = Color(0xFFFFFFFF),
    surfaceElevated = Color(0xFFF1F3F6),
    surfaceVariant = Color(0xFFE9ECEF),
    border = Color(0xFFE2E8F0),
    goldPrimary = GoldPrimaryLight,
    goldSecondary = GoldSecondaryLight,
    goldContainer = GoldContainerLight,
    goldBorder = GoldBorderLight,
    textMain = Color(0xFF111827),
    textSecondary = Color(0xFF4B5563),
    textMuted = Color(0xFF9CA3AF),
    profitGreen = Color(0xFF059669),
    errorRed = Color(0xFFDC2626)
)

val LocalGoldExColors = staticCompositionLocalOf { LightGoldExColors }
