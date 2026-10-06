package com.goldex.companion.desktop

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.goldex.companion.presentation.calculator.ManualGoldCalculator
import com.goldex.companion.ui.theme.GoldExCompanionTheme
import java.awt.Dimension
import com.goldex.companion.presentation.calculator.CalculatorField
import org.jetbrains.skia.Data
import org.jetbrains.skia.Typeface

fun main(args: Array<String>) {
    if ("--verify-runtime" in args) {
        // Exercise the bundled font, native Skia and shared financial core without opening a window.
        val font = checkNotNull(ManualGoldCalculator::class.java.getResourceAsStream("/font/vazirmatn_regular.ttf"))
        font.use { input ->
            Data.makeFromBytes(input.readBytes()).use { data ->
                checkNotNull(Typeface.makeFromData(data)).use { check(it.familyName.isNotBlank()) }
            }
        }
        val calculator = ManualGoldCalculator().apply {
            setInput(CalculatorField.SPOT, "6000000")
            setInput(CalculatorField.GROSS_WEIGHT, "2")
            setInput(CalculatorField.WAGE, "10")
        }
        check(calculator.state.value.result?.totalPayable?.toLong() == 14_315_160L)
        println("Qirato runtime verification passed")
        return
    }
    launchCalculator()
}

private fun launchCalculator() = application {
    val calculator = remember { ManualGoldCalculator() }
    var dark by remember { mutableStateOf(false) }
    Window(
        onCloseRequest = ::exitApplication,
        title = "قیراط | ماشین‌حساب طلا",
        state = rememberWindowState(width = 1180.dp, height = 860.dp)
    ) {
        window.minimumSize = Dimension(800, 640)
        GoldExCompanionTheme(isDarkTheme = dark) {
            DesktopCalculatorScreen(calculator, dark, onThemeChange = { dark = !dark })
        }
    }
}
