package com.goldex.companion.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// Stitch "Persian Sovereign Aurum" Design Tokens
val GoldPrimaryDark = Color(0xFFD4AF37)         // Stitch Dark Champagne Gold #D4AF37
val GoldSecondaryDark = Color(0xFFF3C64F)
val GoldContainerDark = Color(0x24D4AF37)
val GoldBorderDark = Color(0x33D4AF37)

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
    get() = Brush.linearGradient(
        if (isDark) listOf(
            Color(0xFF1A1F2B), Color(0xFF12161F), Color(0xFF0A0D12)
        ) else listOf(
            Color(0xFF141B2B), Color(0xFF1C2436), Color(0xFF141B2B)
        )
    )

val GoldExAppColors.hairlineBorder: BorderStroke
    get() = BorderStroke(0.6.dp, this.border)

val GoldExAppColors.goldHairlineBorder: BorderStroke
    get() = BorderStroke(0.6.dp, this.goldBorder)

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

val GoldExAppColors.goldButtonGradient: Brush
    get() = Brush.horizontalGradient(
        if (isDark) listOf(goldSecondary, goldPrimary)
        else listOf(Color(0xFFFAC24B), Color(0xFFE7B342))
    )

val GoldExAppColors.goldButtonContainer: Color
    get() = if (isDark) goldSecondary else Color(0xFFEBB644)

val GoldExAppColors.goldButtonText: Color
    get() = if (isDark) background else Color(0xFF554300)

// Stitch dark dashboard: charcoal canvas, slate cards, warm stone text.
val DarkGoldExColors = GoldExAppColors(
    isDark = true,
    background = Color(0xFF0B0D11),
    surface = Color(0xFF161B22),
    surfaceElevated = Color(0xFF1E2530),
    surfaceVariant = Color(0xFF1A202C),
    border = Color(0x0DFFFFFF),
    goldPrimary = GoldPrimaryDark,
    goldSecondary = GoldSecondaryDark,
    goldBullion = GoldSecondaryDark,
    goldContainer = GoldContainerDark,
    goldBorder = GoldBorderDark,
    textMain = Color(0xFFF8FAFC),
    textSecondary = Color(0xFFD6D3D1),
    textMuted = Color(0xFFA8A29E),
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

// Dashboard keeps a dark vault card in both themes; preserve its light-mode artwork.
val GoldExAppColors.dashboardVaultGradient: Brush
    get() = if (isDark) heroCardGradient else Brush.linearGradient(
        listOf(Color(0xFF141A29), Color(0xFF1D263B), Color(0xFF111622))
    )

val GoldExAppColors.goldHighlight: Color
    get() = if (isDark) Color(0xFFFFE088) else Color(0xFFFFDF88)

val GoldExAppColors.marketGainText: Color
    get() = if (isDark) profitGreen else Color(0xFF059669)
