package com.goldex.companion.desktop

import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.goldex.companion.presentation.calculator.CalculatorField
import com.goldex.companion.presentation.calculator.ManualGoldCalculator
import com.goldex.companion.ui.theme.GoldExCompanionTheme
import org.jetbrains.skia.EncodedImageFormat
import org.junit.Rule
import org.junit.Test
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DesktopCalculatorScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test fun persianInputsRenderFinancialResultAndResetDisablesExport() {
        val calculator = ManualGoldCalculator()
        compose.setContent { GoldExCompanionTheme { DesktopCalculatorScreen(calculator, false) {} } }
        compose.onNodeWithTag("copy").assertIsNotEnabled()
        compose.onNodeWithTag("input-SPOT").performTextReplacement("۶۰۰۰۰۰۰")
        compose.onNodeWithTag("input-GROSS_WEIGHT").performTextReplacement("۲٫۵۰۰")
        compose.onNodeWithTag("input-STONE_WEIGHT").performTextReplacement("۰٫۵۰۰")
        compose.onNodeWithTag("input-WAGE").performTextReplacement("۱۰")
        compose.onNodeWithText("۱۴,۳۱۵,۱۶۰").assertExists()
        compose.onNodeWithTag("copy").assertIsEnabled()
        compose.onNodeWithTag("input-TAX").performScrollTo().performTextReplacement("10")
        compose.onNodeWithText("۱۴,۳۳۶,۴۰۰").assertExists()
        compose.onNodeWithTag("reset").performClick()
        compose.onNodeWithTag("copy").assertIsNotEnabled()
        compose.runOnIdle { assertNull(calculator.state.value.result) }
    }

    @Test fun invalidWeightDoesNotKeepPreviousTotalVisible() {
        val calculator = ManualGoldCalculator().apply {
            setInput(CalculatorField.SPOT, "6000000")
            setInput(CalculatorField.GROSS_WEIGHT, "2")
        }
        compose.setContent { GoldExCompanionTheme { DesktopCalculatorScreen(calculator, false) {} } }
        compose.onNodeWithTag("copy").assertIsEnabled()
        compose.onNodeWithTag("input-GROSS_WEIGHT").performTextReplacement("-2")
        compose.onNodeWithTag("copy").assertIsNotEnabled()
        compose.runOnIdle { assertNull(calculator.state.value.result) }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test fun keyboardCanMoveFromPriceToWeight() {
        val calculator = ManualGoldCalculator()
        compose.setContent { GoldExCompanionTheme { DesktopCalculatorScreen(calculator, false) {} } }
        compose.onNodeWithTag("input-SPOT").performClick().assertIsFocused()
        compose.onNodeWithTag("input-SPOT").performKeyInput { pressKey(Key.Tab) }
        compose.onNodeWithTag("input-GROSS_WEIGHT").assertIsFocused()
    }

    @Test fun themesRenderTheSameQuoteWithBundledPersianFonts() {
        val calculator = ManualGoldCalculator().apply {
            setInput(CalculatorField.SPOT, "6000000")
            setInput(CalculatorField.GROSS_WEIGHT, "2.500")
            setInput(CalculatorField.STONE_WEIGHT, "0.500")
            setInput(CalculatorField.WAGE, "10")
        }
        var dark by androidx.compose.runtime.mutableStateOf(false)
        compose.setContent {
            GoldExCompanionTheme(isDarkTheme = dark) {
                DesktopCalculatorScreen(calculator, dark) { dark = !dark }
            }
        }
        val before = calculator.state.value.result
        saveScreenshot("calculator-light.png")
        compose.onNodeWithTag("theme").performClick()
        compose.onNodeWithText("حالت روز").assertExists()
        saveScreenshot("calculator-dark.png")
        compose.runOnIdle { assertEquals(before, calculator.state.value.result) }
    }

    private fun saveScreenshot(name: String) {
        val image = compose.onRoot().captureToImage().asSkiaBitmap()
        val encoded = org.jetbrains.skia.Image.makeFromBitmap(image).encodeToData(EncodedImageFormat.PNG)!!
        val directory = File(System.getProperty("qirato.screenshotDir"))
        directory.mkdirs()
        File(directory, name).writeBytes(encoded.bytes)
    }
}
