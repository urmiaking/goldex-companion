package com.goldex.companion.ui.components

import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.LayoutDirection
import com.goldex.companion.ui.theme.GoldExCompanionTheme
import com.goldex.companion.ui.theme.LocalGoldExColors
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class GoldOutlinedTextFieldTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun labelMatchesContainerAcrossFocusValidationAndThemeChanges() {
        val dark = mutableStateOf(false)
        val value = mutableStateOf("")
        val enabled = mutableStateOf(true)
        val error = mutableStateOf(false)
        lateinit var focusManager: FocusManager
        compose.setContent {
            GoldExCompanionTheme(isDarkTheme = dark.value) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    val colors = LocalGoldExColors.current
                    focusManager = LocalFocusManager.current
                    Surface(color = colors.surface) {
                        GoldOutlinedTextField(
                            value = value.value,
                            onValueChange = { value.value = it },
                            enabled = enabled.value,
                            isError = error.value,
                            singleLine = true,
                            modifier = Modifier.testTag("field"),
                            label = { Text("وزن ناخالص", Modifier.testTag("label")) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = colors.surfaceElevated,
                                unfocusedContainerColor = colors.surface,
                                errorContainerColor = colors.surface,
                                disabledContainerColor = colors.surfaceVariant
                            )
                        )
                    }
                }
            }
        }

        for (isDark in listOf(false, true)) {
            compose.runOnIdle {
                dark.value = isDark
                enabled.value = true
                error.value = false
                value.value = ""
            }
            compose.onNodeWithTag("field").performClick()
            assertMatchingBackground()
            compose.onNodeWithTag("field").performTextInput("12.5")
            compose.runOnIdle { assertEquals("12.5", value.value) }
            assertMatchingBackground()
            compose.runOnIdle { focusManager.clearFocus(force = true) }
            assertMatchingBackground()
            compose.runOnIdle { error.value = true }
            compose.onNodeWithTag("field").performClick()
            assertMatchingBackground()
            compose.runOnIdle { enabled.value = false }
            assertMatchingBackground()
        }
    }

    private fun assertMatchingBackground() {
        compose.waitForIdle()
        val label = compose.onNodeWithTag("label", useUnmergedTree = true)
            .captureToImage().toPixelMap()
        val field = compose.onNodeWithTag("field").captureToImage().toPixelMap()
        assertColorEquals(field[field.width / 2, field.height - 10], label[0, 0])
    }

    private fun assertColorEquals(expected: Color, actual: Color) {
        assertEquals(expected.red, actual.red, 0.01f)
        assertEquals(expected.green, actual.green, 0.01f)
        assertEquals(expected.blue, actual.blue, 0.01f)
    }
}
