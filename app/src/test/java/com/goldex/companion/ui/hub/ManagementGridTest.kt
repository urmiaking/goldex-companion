package com.goldex.companion.ui.hub

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.*
import com.goldex.companion.ui.theme.GoldExCompanionTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], qualifiers = "w800dp-h1000dp")
class ManagementGridTest {
    @get:Rule val compose = createComposeRule()
    @Test fun narrowRtlLargeTextHasFourReadableDestinationsWithoutFakeChips() {
        var clicked = 0
        compose.setContent {
            GoldExCompanionTheme(isDarkTheme = true) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl, LocalDensity provides Density(1f, 1.5f)) {
                    Box(Modifier.width(340.dp)) { ManagementGrid({ clicked = 1 }, { clicked = 2 }, { clicked = 3 }, { clicked = 4 }) }
                }
            }
        }
        listOf("دفتر حساب و معین", "آبشده و ری‌گیری", "گزارشات و ترازنامه", "انبار و ویترین طلا").forEachIndexed { index, title ->
            val node = compose.onNodeWithText(title)
            node.assertIsDisplayed().performClick()
            compose.runOnIdle { assertEquals(index + 1, clicked) }
        }
        compose.onNodeWithText("۴ مانده‌دار").assertDoesNotExist()
        compose.onNodeWithText("۳ پاکت").assertDoesNotExist()
        compose.onNodeWithText("تحلیلی").assertDoesNotExist()
        compose.onNodeWithText("بارکدخوان").assertDoesNotExist()
    }
}
