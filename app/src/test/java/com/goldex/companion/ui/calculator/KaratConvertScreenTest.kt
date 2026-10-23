package com.goldex.companion.ui.calculator

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.goldex.companion.data.MarketRates
import com.goldex.companion.ui.calculator.screens.KaratConvertScreen
import com.goldex.companion.ui.theme.GoldExCompanionTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], qualifiers = "w800dp-h1000dp")
class KaratConvertScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test fun smallPhoneSupportsCustomBothSidesSwapValidationAndCorrectTransfer() {
        val vm = KaratConvertViewModel()
        var transferred: Double? = null
        compose.setContent {
            val state by vm.uiState.collectAsState()
            GoldExCompanionTheme(isDarkTheme = false) {
                Box(Modifier.width(320.dp).height(650.dp)) {
                    KaratConvertScreen(state, MarketRates(), {}, vm::onConvertWeightChanged, vm::onConvertFromKarat,
                        vm::onConvertToKarat, vm::onCustomFromKaratChanged, vm::onCustomToKaratChanged,
                        vm::swapConvertKarats, vm::setConvertMode, vm::onAssayKaratChanged, vm::onAgreedKaratChanged,
                        { transferred = vm.uiState.value.invoiceGold750 },
                        onSelectCustomFromKarat = vm::selectCustomFromKarat, onSelectCustomToKarat = vm::selectCustomToKarat)
                }
            }
        }
        compose.onNodeWithTag("convert-transfer").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithTag("convert-weight").performScrollTo()
        compose.onNode(hasSetTextAction() and hasAnyAncestor(hasTestTag("convert-weight"))).performTextInput("10")
        compose.onNodeWithTag("from-custom").performScrollTo()
        compose.onNode(hasSetTextAction() and hasAnyAncestor(hasTestTag("from-custom"))).performTextInput("742")
        compose.onNodeWithTag("to-custom").performScrollTo()
        compose.onNode(hasSetTextAction() and hasAnyAncestor(hasTestTag("to-custom"))).performTextInput("999")
        compose.runOnIdle { assertEquals("742", vm.uiState.value.fromInput); assertEquals("999", vm.uiState.value.toInput) }
        compose.onNodeWithTag("from-custom").performScrollTo().assertExists()
        compose.onNodeWithTag("to-custom").performScrollTo().assertExists()
        compose.onNodeWithText("جابه‌جایی مبدا و مقصد").performScrollTo().performClick()
        compose.runOnIdle {
            assertEquals("999", vm.uiState.value.fromInput)
            assertEquals("742", vm.uiState.value.toInput)
        }
        compose.onNodeWithTag("convert-transfer").performScrollTo().assertIsEnabled().performClick()
        compose.runOnIdle { assertEquals(10.0 * 999 / 750, transferred!!, 1e-10); vm.onCustomToKaratChanged("1001") }
        compose.onNodeWithTag("convert-transfer").performScrollTo().assertIsNotEnabled()
    }

    @Test fun darkLandscapeAndLargeTextKeepAssayFieldsAndActionsReachable() {
        val vm = KaratConvertViewModel()
        vm.onSpotPriceChanged(15_000_000L)
        vm.onConvertWeightChanged("100")
        vm.setConvertMode(KaratConvertMode.SETTLEMENT)
        vm.onAgreedKaratChanged("995")
        vm.onAssayKaratChanged("990")
        var transferred: Double? = null
        compose.setContent {
            val state by vm.uiState.collectAsState()
            GoldExCompanionTheme(isDarkTheme = true) {
                CompositionLocalProvider(LocalDensity provides Density(1f, 1.6f)) {
                    Box(Modifier.width(640.dp).height(360.dp)) {
                        KaratConvertScreen(state, MarketRates(), {}, vm::onConvertWeightChanged, vm::onConvertFromKarat,
                            vm::onConvertToKarat, vm::onCustomFromKaratChanged, vm::onCustomToKaratChanged,
                            vm::swapConvertKarats, vm::setConvertMode, vm::onAssayKaratChanged, vm::onAgreedKaratChanged,
                            { transferred = vm.uiState.value.invoiceGold750 },
                        onSelectCustomFromKarat = vm::selectCustomFromKarat, onSelectCustomToKarat = vm::selectCustomToKarat)
                    }
                }
            }
        }
        compose.onNodeWithTag("assay-agreed").performScrollTo().assertExists()
        compose.onNodeWithTag("assay-actual").performScrollTo().assertExists()
        compose.onNodeWithText("به عهده صاحب طلا (بدهکار)").performScrollTo().assertExists()
        compose.onNodeWithTag("convert-transfer").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(132.0, transferred!!, 1e-10) }
    }
}
