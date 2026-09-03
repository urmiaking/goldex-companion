package com.goldex.companion.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// Stitch "Persian Sovereign Aurum" Design Tokens
val GoldPrimaryDark = Color(0xFFDFB35A)         // Champagne Gold for Dark
val GoldSecondaryDark = Color(0xFFC7983B)
val GoldContainerDark = Color(0x24DFB35A)
val GoldBorderDark = Color(0x4DDFB35A)

val GoldPrimaryLight = Color(0xFFD4AF37)        // Stitch Master Champagne Gold #D4AF37
val GoldSecondaryLight = Color(0xFFB8860B)      // Stitch Deep Bullion Gold #B8860B
val GoldContainerLight = Color(0x29D4AF37)      // Soft gold halo
val GoldBorderLight = Color(0x38B8860B)         // Stitch Gilded Hairline Border rgba(184, 134, 11, 0.22)

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
    val goldBullion: Color,
    val goldContainer: Color,
    val goldBorder: Color,
    val textMain: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val profitGreen: Color,
    val errorRed: Color
)

val GoldExAppColors.goldGradient: Brush
    get() = Brush.horizontalGradient(
        listOf(
            Color.Transparent,
            this.goldSecondary,
            this.goldPrimary,
            Color.Transparent
        )
    )

val GoldExAppColors.heroCardGradient: Brush
    get() = Brush.verticalGradient(
        listOf(
            this.goldContainer.copy(alpha = if (isDark) 0.28f else 0.40f),
            this.surface
        )
    )

val GoldExAppColors.hairlineBorder: BorderStroke
    get() = BorderStroke(0.6.dp, this.border)

val GoldExAppColors.goldHairlineBorder: BorderStroke
    get() = BorderStroke(0.8.dp, this.goldBorder)

val GoldExAppColors.specularHairlineBrush: Brush
    get() = Brush.horizontalGradient(
        listOf(
            Color.Transparent,
            this.goldSecondary.copy(alpha = 0.5f),
            this.goldBullion.copy(alpha = 0.85f),
            this.goldPrimary.copy(alpha = 0.5f),
            Color.Transparent
        )
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
    goldBullion = GoldSecondaryDark,
    goldContainer = GoldContainerDark,
    goldBorder = GoldBorderDark,
    textMain = Color(0xFFF3F4F8),
    textSecondary = Color(0xFF9EA6B8),
    textMuted = Color(0xFF676E7E),
    profitGreen = Color(0xFF10B981),
    errorRed = Color(0xFFEF4444)
)

val LightGoldExColors = GoldExAppColors(
    isDark = false,
    background = Color(0xFFF6F8FA),             // Stitch Warm Ivory / Porcelain #F6F8FA
    surface = Color(0xFFFFFFFF),                // Stitch Pure Alabaster White #FFFFFF
    surfaceElevated = Color(0xFFF1F3FF),        // Stitch Surface Container Low #F1F3FF
    surfaceVariant = Color(0xFFE9EDFF),         // Stitch Surface Container #E9EDFF
    border = Color(0x2E141B2B),                 // Stitch Subtle Hairline
    goldPrimary = GoldSecondaryLight,           // Deep Gold for text/buttons
    goldSecondary = GoldPrimaryLight,          // Champagne Gold
    goldBullion = GoldSecondaryLight,          // Stitch Deep Bullion Gold #B8860B
    goldContainer = GoldContainerLight,
    goldBorder = GoldBorderLight,
    textMain = Color(0xFF141B2B),               // Stitch Primary Ink #141B2B
    textSecondary = Color(0xFF4D4635),          // Stitch Secondary Ink #4D4635
    textMuted = Color(0xFF6B7280),              // Stitch Muted Pewter #6B7280
    profitGreen = Color(0xFF10B981),            // Stitch Emerald Bull Gain #10B981
    errorRed = Color(0xFFEF4444)                // Stitch Ruby Bear Loss #EF4444
)

val LocalGoldExColors = staticCompositionLocalOf { LightGoldExColors }
