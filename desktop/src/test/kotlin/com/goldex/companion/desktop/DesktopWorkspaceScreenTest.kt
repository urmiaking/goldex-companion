package com.goldex.companion.desktop

import androidx.compose.runtime.*
import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.goldex.companion.data.*
import com.goldex.companion.desktop.data.*
import com.goldex.companion.desktop.state.*
import com.goldex.companion.desktop.ui.DesktopWorkspaceScreen
import com.goldex.companion.presentation.calculator.CalculatorField
import com.goldex.companion.ui.theme.GoldExCompanionTheme
import org.junit.*
import org.junit.rules.TemporaryFolder
import java.io.File
import kotlin.test.assertEquals

class DesktopWorkspaceScreenTest {
    @get:Rule val compose = createComposeRule()
    @get:Rule val temporary = TemporaryFolder()
    private lateinit var store: DesktopDataStore
    private lateinit var workspace: DesktopWorkspace

    @Before fun setup() {
        store = DesktopDataStore(temporary.newFolder().toPath())
        val market = DesktopMarketRepository(store, fetch = { error("offline test") })
        market.useManual(DesktopMarketRepository.emptyRates().copy(gold18 = 6_000_000, coinEmami = 60_000_000, goldMelt = 25_991_100))
        workspace = DesktopWorkspace(store, market)
    }
    @After fun cleanup() { workspace.close(); store.close() }
    private fun render() = compose.setContent {
        val state by workspace.state.collectAsState()
        GoldExCompanionTheme(isDarkTheme = state.dark) { DesktopWorkspaceScreen(workspace, {}, "0.56.37") }
    }
    private fun field(tag: String) = compose.onNode(hasSetTextAction() and hasAnyAncestor(hasTestTag(tag)))

    @Test fun navigationRetainsCalculatorAndUnsavedSettings() {
        render()
        compose.onNodeWithTag("nav-CALCULATOR").performClick()
        compose.onNodeWithTag("input-SPOT").performTextReplacement("۶۰۰۰۰۰۰")
        compose.onNodeWithTag("input-GROSS_WEIGHT").performTextReplacement("۲")
        compose.onNodeWithTag("nav-SETTINGS").performClick()
        field("settings-gallery").performTextReplacement("گالری آزمون")
        compose.onNodeWithTag("nav-RATES").performClick()
        compose.onNodeWithTag("nav-SETTINGS").performClick()
        field("settings-gallery").assertTextContains("گالری آزمون")
        compose.onNodeWithTag("save-settings").performScrollTo().performClick()
        compose.waitUntil(10_000) { store.loadSettings().galleryName == "گالری آزمون" }
        compose.onNodeWithTag("nav-CALCULATOR").performClick()
        compose.onNodeWithTag("input-GROSS_WEIGHT").assertTextContains("۲")
        compose.runOnIdle { assertEquals("2", workspace.calculator.state.value.input(CalculatorField.GROSS_WEIGHT)) }
    }

    @Test fun persianAssetFormSavesAndSearchFiltersRealRecords() {
        render()
        compose.onNodeWithTag("nav-PORTFOLIO").performClick()
        compose.onNodeWithTag("add-asset").performClick()
        field("asset-title").performTextReplacement("شمش آزمون")
        field("asset-weight").performTextReplacement("۲٫۱۲۵")
        field("asset-cost").performTextReplacement("۱۰۰۰۰۰۰۰")
        screenshot("asset-dialog.png", "asset-dialog")
        compose.onNodeWithTag("save-asset").performClick()
        compose.waitUntil(10_000) { store.getItems().size == 1 && workspace.state.value.draft == null }
        compose.onNodeWithText("شمش آزمون").assertExists()
        field("asset-search").performClick().assertIsFocused().performTextReplacement("سکه")
        compose.onNodeWithText("دارایی مطابق جست‌وجو پیدا نشد").assertExists()
        field("asset-search").performTextReplacement("")
        compose.onNodeWithText("شمش آزمون").assertExists()
    }

    @Test fun deletionRequiresConfirmationAndCancelKeepsRecord() {
        val item = PortfolioItem(id = "gold", title = "طلای آزمون", category = PortfolioCategory.GOLD, weightGrams = 2.0)
        store.addItem(item)
        // Re-open the model after seeding synthetic test data, never production demo data.
        workspace.close()
        workspace = DesktopWorkspace(store, DesktopMarketRepository(store, fetch = { error("offline test") }))
        render()
        compose.onNodeWithTag("nav-PORTFOLIO").performClick()
        compose.onNodeWithTag("delete-gold").performClick()
        compose.onNodeWithText("انصراف").performClick()
        compose.runOnIdle { assertEquals(1, store.getItems().size) }
        compose.onNodeWithTag("delete-gold").performClick()
        compose.onNodeWithTag("confirm-delete").performClick()
        compose.waitUntil(10_000) { store.getItems().isEmpty() }
    }

    @Test fun screensRenderInBothThemesWithRealFontsAndUnavailableQuotes() {
        store.addItem(PortfolioItem(title = "شمش آزمون", category = PortfolioCategory.GOLD, weightGrams = 2.125, purchasePriceTotal = 10_000_000))
        workspace.close()
        workspace = DesktopWorkspace(store, DesktopMarketRepository(store, fetch = { error("offline test") }))
        render()
        screenshot("workspace-light.png")
        compose.onNodeWithTag("workspace-theme").performClick()
        compose.waitUntil(10_000) { workspace.state.value.dark }
        screenshot("workspace-dark.png")
        compose.onNodeWithTag("nav-RATES").performClick()
        screenshot("rates-dark.png")
        compose.onNodeWithTag("nav-PORTFOLIO").performClick()
        screenshot("portfolio-dark.png")
        compose.onNodeWithTag("nav-SETTINGS").performClick()
        screenshot("settings-dark.png")
    }

    private fun screenshot(name: String, tag: String = "workspace-root") {
        val bitmap = compose.onNodeWithTag(tag).captureToImage().asSkiaBitmap()
        val image = org.jetbrains.skia.Image.makeFromBitmap(bitmap)
        val encoded = image.encodeToData(org.jetbrains.skia.EncodedImageFormat.PNG)!!
        val directory = File(System.getProperty("qirato.screenshotDir")).apply { mkdirs() }
        File(directory, name).writeBytes(encoded.bytes)
        encoded.close(); image.close(); bitmap.close()
    }
}
