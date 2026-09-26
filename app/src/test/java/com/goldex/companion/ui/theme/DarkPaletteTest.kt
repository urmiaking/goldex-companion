package com.goldex.companion.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DarkPaletteTest {
    @Test
    fun normalTextRemainsReadableAcrossDarkSurfaces() {
        val colors = DarkGoldExColors
        val surfaces = listOf(colors.background, colors.surface, colors.surfaceElevated, colors.surfaceVariant)
        val textColors = listOf(colors.textMain, colors.textSecondary, colors.textMuted, colors.goldPrimary, colors.goldSecondary)
        surfaces.forEach { surface ->
            textColors.forEach { foreground -> assertReadable(foreground, surface) }
        }
        assertReadable(colors.marketGainText, colors.surface)
        assertReadable(colors.goldButtonText, colors.goldButtonContainer)
        assertReadable(colors.goldButtonText, colors.goldPrimary)
    }

    @Test
    fun materialControlsUseAppPaletteAndReadableContainerLabels() {
        val scheme = DarkColorScheme
        val colors = DarkGoldExColors
        assertEquals(colors.background, scheme.background)
        assertEquals(colors.surface, scheme.surfaceContainer)
        assertEquals(colors.surfaceElevated, scheme.surfaceContainerHigh)
        assertEquals(colors.goldPrimary, scheme.primary)
        assertEquals(colors.goldSecondary, scheme.secondary)
        assertEquals(colors.profitGreen, scheme.tertiary)
        assertEquals(colors.errorRed, scheme.error)
        listOf(
            scheme.onPrimary to scheme.primary,
            scheme.onPrimaryContainer to scheme.primaryContainer,
            scheme.onSecondary to scheme.secondary,
            scheme.onSecondaryContainer to scheme.secondaryContainer,
            scheme.onTertiary to scheme.tertiary,
            scheme.onTertiaryContainer to scheme.tertiaryContainer,
            scheme.onError to scheme.error,
            scheme.onErrorContainer to scheme.errorContainer,
            scheme.onSurfaceVariant to scheme.surfaceVariant,
            scheme.onSurface to scheme.surfaceContainerHighest,
            scheme.inverseOnSurface to scheme.inverseSurface
        ).forEach { (foreground, background) -> assertReadable(foreground, background) }
    }

    private fun assertReadable(foreground: Color, background: Color) {
        val light = maxOf(foreground.luminance(), background.luminance())
        val dark = minOf(foreground.luminance(), background.luminance())
        val ratio = (light + 0.05f) / (dark + 0.05f)
        assertTrue("Text contrast must be at least 4.5:1, was $ratio", ratio >= 4.5f)
    }
}
