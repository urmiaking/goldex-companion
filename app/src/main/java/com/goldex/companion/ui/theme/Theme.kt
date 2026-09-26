package com.goldex.companion.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle

// Material controls use the same Stitch tokens as custom app surfaces.
internal val DarkColorScheme = darkColorScheme(
    primary = DarkGoldExColors.goldPrimary,
    onPrimary = DarkGoldExColors.background,
    primaryContainer = Color(0xFF2A2308),
    onPrimaryContainer = Color(0xFFFFE088),
    secondary = DarkGoldExColors.goldSecondary,
    onSecondary = Color(0xFF1E1800),
    secondaryContainer = Color(0xFF4A3B00),
    onSecondaryContainer = DarkGoldExColors.goldSecondary,
    tertiary = DarkGoldExColors.profitGreen,
    onTertiary = DarkGoldExColors.background,
    tertiaryContainer = Color(0xFF064E3B),
    onTertiaryContainer = Color(0xFFA7F3D0),
    background = DarkGoldExColors.background,
    onBackground = DarkGoldExColors.textMain,
    surface = DarkGoldExColors.background,
    onSurface = DarkGoldExColors.textMain,
    surfaceVariant = DarkGoldExColors.surfaceVariant,
    onSurfaceVariant = Color(0xFF94A3B8),
    surfaceTint = DarkGoldExColors.goldPrimary,
    surfaceDim = Color(0xFF08090C),
    surfaceBright = DarkGoldExColors.surface,
    surfaceContainerLowest = Color(0xFF0E1116),
    surfaceContainerLow = Color(0xFF12161D),
    surfaceContainer = DarkGoldExColors.surface,
    surfaceContainerHigh = DarkGoldExColors.surfaceElevated,
    surfaceContainerHighest = Color(0xFF28303E),
    outline = Color(0xFF64748B),
    outlineVariant = DarkGoldExColors.goldBorder,
    error = DarkGoldExColors.errorRed,
    onError = DarkGoldExColors.background,
    errorContainer = Color(0xFF3F1315),
    onErrorContainer = Color(0xFFFFDAD6),
    inverseSurface = DarkGoldExColors.textMain,
    inverseOnSurface = DarkGoldExColors.background,
    inversePrimary = Color(0xFF735C00)
)

private val LightColorScheme = lightColorScheme(
    primary = GoldPrimaryLight,
    onPrimary = Color.White,
    secondary = GoldSecondaryLight,
    onSecondary = Color.White,
    background = Color(0xFFF6F8FA),
    onBackground = Color(0xFF111827),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF111827),
    surfaceVariant = Color(0xFFF1F3F6),
    onSurfaceVariant = Color(0xFF4B5563),
    outline = Color(0xFFE2E8F0)
)

@Composable
fun GoldExCompanionTheme(
    isDarkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (isDarkTheme) DarkColorScheme else LightColorScheme
    val appColors = if (isDarkTheme) DarkGoldExColors else LightGoldExColors

    CompositionLocalProvider(LocalGoldExColors provides appColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = GoldExShapes
        ) {
            ProvideTextStyle(value = TextStyle(fontFamily = VazirmatnFamily, fontFeatureSettings = VazirmatnFeatureSettings)) {
                content()
            }
        }
    }
}
