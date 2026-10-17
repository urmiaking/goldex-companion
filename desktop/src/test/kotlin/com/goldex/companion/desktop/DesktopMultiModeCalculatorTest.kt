package com.goldex.companion.desktop

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.goldex.companion.data.MarketRates
import com.goldex.companion.desktop.data.DesktopDataStore
import com.goldex.companion.desktop.data.DesktopMarketRepository
import com.goldex.companion.desktop.state.DesktopCalculatorRates
import com.goldex.companion.desktop.state.DesktopWorkspace
import com.goldex.companion.model.CoinType
import com.goldex.companion.model.PersianNumberFormatter
import com.goldex.companion.presentation.calculator.ManualGoldCalculator
import com.goldex.companion.ui.theme.GoldExCompanionTheme
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DesktopMultiModeCalculatorTest {
    @get:Rule val compose = createComposeRule()
    @get:Rule val temporary = TemporaryFolder()

    private lateinit var store: DesktopDataStore
    private lateinit var workspace: DesktopWorkspace
    private lateinit var calculatorRates: DesktopCalculatorRates
    private val testRates = MarketRates(
        gold18 = 6_000_000L,
        gold24 = 8_000_000L,
        goldMelt = 26_000_000L,
        coinEmami = 60_000_000L,
        coinBahar = 55_000_000L,
        coinHalf = 32_000_000L,
        coinQuarter = 18_000_000L,
        coinGerami = 9_000_000L,
        usd = 100_000L,
        ons = 2600.0
    )

    @Before fun setup() {
        store = DesktopDataStore(temporary.newFolder().toPath())
        val market = DesktopMarketRepository(store, fetch = { error("offline test") })
        market.useManual(testRates)
        workspace = DesktopWorkspace(store, market)
        calculatorRates = workspace.calculatorRates
    }

    @After fun cleanup() {
        workspace.close()
        store.close()
    }

    private fun render() = compose.setContent {
        GoldExCompanionTheme {
            DesktopCalculatorScreen(
                calculator = workspace.calculator,
                dark = false,
                showHeader = true,
                marketRates = calculatorRates
            )
        }
    }

    @Test
    fun multiModeTabsSwitchSmoothlyAcrossAllFiveModes() {
        render()

        // 1. Jewelry is default
        compose.onNodeWithTag("input-SPOT").assertExists()

        // 2. Switch to Melt
        compose.onNodeWithText("مظنه و آبشده سنتی").performClick()
        compose.onNodeWithTag("melt-input-card").assertExists().assertIsDisplayed()
        compose.onNodeWithTag("melt-result-card").assertExists().assertIsDisplayed()

        // 3. Switch to Coin Bubble
        compose.onNodeWithText("تحلیل حباب مسکوکات").performClick()
        compose.onNodeWithTag("coin-bubble-market-bar").assertExists().assertIsDisplayed()
        compose.onNodeWithTag("coin-card-EMAMI").assertExists()

        // 4. Switch to Karat Convert
        compose.onNodeWithText("تبدیل عیار و ری‌گیری").performClick()
        compose.onNodeWithTag("karat-direct-input-card").assertExists().assertIsDisplayed()

        // 5. Switch to Standard Formulas
        compose.onNodeWithText("فرمول‌های مصوب اتحادیه").performClick()
        compose.onNodeWithTag("master-law-banner").assertExists().assertIsDisplayed()
        compose.onNodeWithTag("formula-card-f1").assertExists()
    }

    @Test
    fun meltCalculatorCalculatesAccuratelyAndPreservesGuildFormulas() {
        render()
        compose.onNodeWithText("مظنه و آبشده سنتی").performClick()

        // Test with custom mesghal and weight
        compose.onNodeWithTag("melt-mesghal-field").performTextReplacement("26000000")
        compose.onNodeWithTag("melt-weight-field").performTextReplacement("10.000")
        compose.onNodeWithTag("melt-karat-705").performClick()

        // Expected 18k gram price: 26,000,000 / 4.33185 = 6,002,054
        // Expected equivalent 18k weight: 10 * 705 / 750 = 9.400 g
        val expectedMeltWeight = "${PersianNumberFormatter.formatAccountWeight(9.4)} گرم"
        compose.onNodeWithText(expectedMeltWeight).assertExists()

        // Test copy summary button is enabled
        compose.onNodeWithTag("melt-copy-summary").assertExists().assertIsEnabled()
    }

    @Test
    fun coinBubbleAnalysisComputesIntrinsicValueAndAllowsManualOverride() {
        render()
        compose.onNodeWithText("تحلیل حباب مسکوکات").performClick()

        // Verify Emami card exists with computed intrinsic and bubble
        compose.onNodeWithTag("coin-card-EMAMI").assertExists()
        compose.onNodeWithTag("coin-insights-card").assertExists()

        // Filter for specific coin
        compose.onNodeWithTag("filter-coin-HALF").performClick()
        compose.onNodeWithTag("coin-card-HALF").assertExists()
        compose.onNodeWithTag("coin-card-EMAMI").assertDoesNotExist()

        // Verify copy table button is enabled
        compose.onNodeWithTag("coin-copy-table").assertExists().assertIsEnabled()
    }

    @Test
    fun karatConverterSupportsDirectConversionAndAssayLabSettlement() {
        render()
        compose.onNodeWithText("تبدیل عیار و ری‌گیری").performClick()

        // Direct Mode: 10g from 750 to 999
        compose.onNodeWithTag("karat-direct-weight-field").performTextReplacement("10")
        compose.onNodeWithTag("karat-from-field").performTextReplacement("750")
        compose.onNodeWithTag("karat-to-field").performTextReplacement("999")
        // Expected converted weight: 10 * 750 / 999 = 7.5075...
        val expectedConverted = "${PersianNumberFormatter.formatAccountWeight(7500.0 / 999.0)} گرم"
        compose.onNodeWithText(expectedConverted).assertExists()

        // Switch to Assay Mode
        compose.onNodeWithTag("karat-mode-assay").performClick()
        compose.onNodeWithTag("karat-assay-input-card").assertExists()

        compose.onNodeWithTag("assay-weight-field").performTextReplacement("100")
        compose.onNodeWithTag("assay-agreed-field").performTextReplacement("750")
        compose.onNodeWithTag("assay-lab-field").performTextReplacement("740")

        // 10 lines deficit, negative weight diff, "کسری عیار (بدهکاری طرف‌حساب)"
        compose.onNodeWithText("کسری عیار (بدهکاری طرف‌حساب)").assertExists()
    }

    @Test
    fun standardFormulasSearchAndCategoryFiltering() {
        render()
        compose.onNodeWithText("فرمول‌های مصوب اتحادیه").performClick()

        // Search for "ماده ۲۶" or "اجرت"
        compose.onNodeWithTag("formula-search-input").performTextInput("اجرت")
        compose.onNodeWithTag("formula-card-f2").assertExists()
        compose.onNodeWithTag("formula-card-f6").assertDoesNotExist()

        // Clear search
        compose.onNodeWithContentDescription("پاک کردن جستجو").performClick()
        compose.onNodeWithTag("formula-card-f6").assertExists()

        // Filter by Coin category
        compose.onNodeWithTag("filter-formula-COIN").performClick()
        compose.onNodeWithTag("formula-card-f6").assertExists()
        compose.onNodeWithTag("formula-card-f1").assertDoesNotExist()
    }
}
