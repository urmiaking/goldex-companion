package com.goldex.companion.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle

private val DarkColorScheme = darkColorScheme(
    primary = GoldPrimaryDark,
    onPrimary = Color(0xFF0A0B0E),
    secondary = GoldSecondaryDark,
    onSecondary = Color(0xFF0A0B0E),
    background = Color(0xFF0A0B0E),
    onBackground = Color(0xFFF3F4F8),
    surface = Color(0xFF13161F),
    onSurface = Color(0xFFF3F4F8),
    surfaceVariant = Color(0xFF1E2333),
    onSurfaceVariant = Color(0xFF9EA6B8),
    outline = Color(0xFF242938)
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
            typography = Typography
        ) {
            ProvideTextStyle(value = TextStyle(fontFamily = VazirmatnFamily, fontFeatureSettings = VazirmatnFeatureSettings)) {
                content()
            }
        }
    }
}
