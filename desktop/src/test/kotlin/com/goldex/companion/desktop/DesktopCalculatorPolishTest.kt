package com.goldex.companion.desktop

import androidx.compose.runtime.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.LayoutDirection
import com.goldex.companion.data.MarketRates
import com.goldex.companion.desktop.calculator.*
import com.goldex.companion.desktop.ui.controls.LocalDesktopReduceMotion
import com.goldex.companion.presentation.calculator.CalculatorField
import com.goldex.companion.presentation.calculator.ManualGoldCalculator
import com.goldex.companion.ui.theme.GoldExCompanionTheme
import com.goldex.companion.ui.theme.LocalGoldExColors
import org.junit.Test
import java.io.File
import kotlin.test.*

@OptIn(ExperimentalTestApi::class)
class DesktopCalculatorPolishTest {
    @Test fun customKaratCanBeTypedThroughPresetValuesAndReplacedInBothTabs() = runDesktopComposeUiTest(width = 1440, height = 1000) {
        val calculator = ManualGoldCalculator().apply {
            setInput(CalculatorField.SPOT, "6000000"); setInput(CalculatorField.GROSS_WEIGHT, "10")
        }
        setContent { GoldExCompanionTheme { DesktopCalculatorScreen(calculator, false, showHeader = false) } }
        val input = onNodeWithTag("input-KARAT")
        input.performScrollTo().performClick().performTextInput("7")
        runOnIdle { assertEquals("7", calculator.state.value.input(CalculatorField.KARAT)) }
        input.performTextInput("5"); input.performTextInput("0")
        runOnIdle { assertEquals("750", calculator.state.value.input(CalculatorField.KARAT)) }
        input.performTextInput("5")
        runOnIdle { assertEquals("7505", calculator.state.value.input(CalculatorField.KARAT)); assertNull(calculator.state.value.result) }
        input.performTextReplacement("۷۴۰")
        runOnIdle { assertEquals("740", calculator.state.value.input(CalculatorField.KARAT)); assertNotNull(calculator.state.value.result) }
        onNodeWithTag("karat-750").performClick()
        runOnIdle { assertEquals("750", calculator.state.value.input(CalculatorField.KARAT)) }
        onNodeWithTag("calculator-tab-MELT").performClick()
        val melt = onNodeWithTag("melt-karat-field")
        melt.performClick(); melt.performTextInput("7"); melt.performTextInput("5"); melt.performTextInput("0"); melt.performTextInput("5")
        melt.assertTextEquals("۷۵۰۵")
        melt.performTextReplacement("۷۴۰")
        onNodeWithTag("melt-copy-summary").assertIsEnabled()
    }

    @Test fun numericInputDirectionAndVisualLeftUnitsArePreserved() = runDesktopComposeUiTest(width = 1440, height = 1000) {
        var convert by mutableStateOf(false)
        setContent { GoldExCompanionTheme { CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            if (convert) DesktopKaratConvertCalculatorPane(MarketRates()) else DesktopMeltCalculatorPane(MarketRates())
        } } }
        fun checkField(tag: String, unit: String) {
            val field = onNodeWithTag(tag)
            val layouts = mutableListOf<TextLayoutResult>()
            field.performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
            assertTrue(layouts.isNotEmpty()); assertTrue(layouts.all { it.layoutInput.style.textDirection == TextDirection.Ltr })
            val badge = onNode(hasText(unit) and hasAnyAncestor(hasTestTag(tag)), useUnmergedTree = true).fetchSemanticsNode()
            assertTrue(badge.boundsInRoot.center.x < field.fetchSemanticsNode().boundsInRoot.center.x, tag)
        }
        checkField("melt-mesghal-field", "تومان ")
        checkField("melt-weight-field", "گرم ")
        runOnIdle { convert = true }
        checkField("karat-direct-weight-field", "گرم ")
        checkField("karat-from-field", "عیار ")
        checkField("karat-to-field", "عیار ")
        onNodeWithTag("karat-mode-assay").performClick()
        checkField("assay-weight-field", "گرم ")
        checkField("assay-agreed-field", "عیار ")
        checkField("assay-lab-field", "عیار ")
    }

    @Test fun coinCompositionNeverInventsBubbleForDiscountedOrUnavailablePrices() {
        assertEquals(.8f, intrinsicMarketShare(80.0, 100.0))
        assertEquals(1f, intrinsicMarketShare(120.0, 100.0))
        assertEquals(0f, intrinsicMarketShare(80.0, 0.0))
        assertEquals(0f, intrinsicMarketShare(Double.NaN, 100.0))
    }

    @Test fun wideCalculatorViewsInBothThemes() = reviewViews(1440)
    @Test fun minimumCalculatorViewsInBothThemes() = reviewViews(940)

    private fun reviewViews(width: Int) = runDesktopComposeUiTest(width = width, height = 900) {
        var tab by mutableStateOf(DesktopCalculatorTab.JEWELRY)
        var dark by mutableStateOf(false)
        val rates = MarketRates(gold18 = 6000000, goldMelt = 26000000, usd = 100000, ons = 2600.0, coinEmami = 60000000,
            coinBahar = 55000000, coinHalf = 32000000, coinQuarter = 18000000, coinGerami = 9000000)
        val calculator = ManualGoldCalculator().apply {
            setInput(CalculatorField.SPOT, "6000000"); setInput(CalculatorField.GROSS_WEIGHT, "10")
        }
        setContent { GoldExCompanionTheme(isDarkTheme = dark) {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl, LocalDesktopReduceMotion provides true) {
                Box(Modifier.fillMaxSize().background(LocalGoldExColors.current.background)) {
                when (tab) {
                    DesktopCalculatorTab.JEWELRY -> DesktopCalculatorScreen(calculator, dark, showHeader = false)
                    DesktopCalculatorTab.MELT -> DesktopMeltCalculatorPane(rates)
                    DesktopCalculatorTab.COIN_BUBBLE -> DesktopCoinBubbleCalculatorPane(rates)
                    DesktopCalculatorTab.KARAT_CONVERT -> DesktopKaratConvertCalculatorPane(rates)
                    DesktopCalculatorTab.STANDARD_FORMULAS -> DesktopStandardFormulasPane("7", "10")
                }
                }
            }
        } }
        for (theme in listOf(false, true)) for (mode in DesktopCalculatorTab.values()) {
            runOnIdle { dark = theme; tab = mode }
            val bitmap = onRoot().captureToImage().asSkiaBitmap()
            val image = org.jetbrains.skia.Image.makeFromBitmap(bitmap)
            val encoded = image.encodeToData(org.jetbrains.skia.EncodedImageFormat.PNG)!!
            File(System.getProperty("qirato.screenshotDir")).apply { mkdirs() }
                .resolve("calculator-${mode.name}-$width-${if (theme) "dark" else "light"}.png").writeBytes(encoded.bytes)
            encoded.close(); image.close(); bitmap.close()
        }
    }
}
