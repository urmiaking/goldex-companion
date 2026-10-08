package com.goldex.companion.desktop

import androidx.compose.runtime.*
import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.text.TextLayoutResult
import com.goldex.companion.data.*
import com.goldex.companion.desktop.data.*
import com.goldex.companion.desktop.state.*
import com.goldex.companion.desktop.ui.DesktopWorkspaceScreen
import com.goldex.companion.model.*
import com.goldex.companion.ui.theme.GoldExCompanionTheme
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import kotlin.test.*

@OptIn(ExperimentalTestApi::class)
class DesktopMobileParityTest {
    @get:Rule val temporary = TemporaryFolder()
    private lateinit var store: DesktopDataStore
    private lateinit var workspace: DesktopWorkspace
    private val item = InventoryItem(id = "stock", code = "ST-1", title = "قطعه آزمون", grossWeightGrams = 4.125, stoneWeightGrams = .125, customKaratValue = 875, quantity = 3)
    @Before fun setup() {
        store = DesktopDataStore(temporary.newFolder().toPath())
        store.inventory.addItem(item)
        // A retained old portfolio record must not inflate the current inventory balance.
        store.addItem(PortfolioItem(id = "old", title = "دارایی قبلی", category = PortfolioCategory.GOLD, weightGrams = 100.0))
        store.saveInventoryVisible(true)
        val market = DesktopMarketRepository(store, fetch = { error("offline") })
        market.useManual(DesktopMarketRepository.emptyRates().copy(gold18 = 6_000_000))
        workspace = DesktopWorkspace(store, market, history = DesktopGoldHistoryGateway { error("offline") })
    }
    @After fun cleanup() { workspace.close(); store.close() }

    @Test fun settingsToolsShareRowAndHeightWithUnitAdornmentsAndNoRateToggle() = runDesktopComposeUiTest(width = 1400, height = 1000) {
        workspace.navigate(DesktopDestination.SETTINGS)
        setContent { val state by workspace.state.collectAsState(); GoldExCompanionTheme(state.dark) { DesktopWorkspaceScreen(workspace, {}, "0.56.48") } }
        onNodeWithText("دریافت خودکار").assertDoesNotExist()
        onNodeWithText("به‌روزرسانی قیراط").assertDoesNotExist()
        onNodeWithTag("field-unit-سود", useUnmergedTree=true).assertTextEquals("٪")
        onNodeWithTag("field-unit-مالیات", useUnmergedTree=true).assertTextEquals("٪")
        onNodeWithTag("settings-tools").performScrollTo()
        val backup = onNodeWithTag("settings-backup").fetchSemanticsNode().boundsInRoot
        val motion = onNodeWithTag("settings-motion").fetchSemanticsNode().boundsInRoot
        assertEquals(backup.top, motion.top)
        assertEquals(backup.height, motion.height)
        onNodeWithTag("save-backup").assertIsDisplayed()
        onNodeWithTag("reduce-motion").performClick()
        waitUntil(5000) { workspace.state.value.reduceMotion }
        save(onNodeWithTag("workspace-root"), "settings-polished-light.png")
        onNodeWithTag("workspace-theme").performClick()
        waitUntil(5000) { workspace.state.value.dark }
        save(onNodeWithTag("workspace-root"), "settings-polished-dark.png")
    }

    @Test fun settingsIdentifiersRenderWithoutPriceGroupingAndSurviveReopen() = runDesktopComposeUiTest(width = 1400, height = 980) {
        setContent { val state by workspace.state.collectAsState(); GoldExCompanionTheme(state.dark) { DesktopWorkspaceScreen(workspace, {}, "0.56.42") } }
        onNodeWithTag("nav-SETTINGS").performClick()
        fun field(tag: String) = onNode(hasSetTextAction() and hasAnyAncestor(hasTestTag(tag)))
        val identifiers = mapOf("settings-phone" to "+98 09123456789", "settings-union" to "۰۰۱۲۳۴۵۶", "settings-license" to "AB-00123456")
        identifiers.forEach { (tag, value) ->
            field(tag).performTextReplacement(value)
            val layouts = mutableListOf<TextLayoutResult>()
            field(tag).performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
            assertEquals(value, layouts.single().layoutInput.text.text)
        }
        field("settings-gallery").performTextReplacement("گالری شماره ۱۲")
        field("settings-address").performTextReplacement("نشانی آزمایشی\nطبقه دوم")
        field("settings-profit").performTextReplacement("۷٫۵")
        save(onNodeWithTag("workspace-root"), "settings-identifiers-light.png")
        onNodeWithTag("workspace-theme").performClick()
        waitUntil(5000) { workspace.state.value.dark }
        save(onNodeWithTag("workspace-root"), "settings-identifiers-dark.png")
        onNodeWithTag("nav-CALCULATOR").performClick()
        onNodeWithTag("input-SPOT").performTextReplacement("۶۰۰۰۰۰۰")
        val priceLayouts = mutableListOf<TextLayoutResult>()
        onNodeWithTag("input-SPOT").performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(priceLayouts) }
        val priceText = priceLayouts.single().layoutInput.text.text
        assertTrue(priceText.contains(',') || priceText.contains('،'))
        onNodeWithTag("nav-SETTINGS").performClick()
        onNodeWithTag("save-settings").performScrollTo().performClick()
        waitUntil(5000) { workspace.state.value.settingsDraft == null && !workspace.state.value.saving }
        val directory = store.directory
        workspace.close(); store.close()
        store = DesktopDataStore(directory)
        assertEquals(identifiers["settings-phone"], store.loadSettings().galleryPhone)
        assertEquals(identifiers["settings-union"], store.loadSettings().unionCode)
        assertEquals(identifiers["settings-license"], store.loadSettings().galleryLicense)
        assertEquals("7.5", store.loadSettings().defaultProfitPercent)
        assertEquals("نشانی آزمایشی\nطبقه دوم", store.loadSettings().galleryAddress)
    }

    @Test fun dashboardAndNavigationUseTheSameInventoryIncludingStockChangesAndPrivacy() = runDesktopComposeUiTest(width = 1400, height = 980) {
        setContent { val state by workspace.state.collectAsState(); GoldExCompanionTheme(state.dark) { DesktopWorkspaceScreen(workspace, {}, "0.56.42") } }
        waitUntil(5000) { workspace.inventory.state.value.spot18 == 6_000_000L }
        onNodeWithTag("nav-PORTFOLIO").assertDoesNotExist()
        onNodeWithText(PersianNumberFormatter.formatWeight(14.0)).assertExists()
        onNodeWithText(PersianNumberFormatter.formatPrice(84_000_000)).assertExists()
        onNodeWithTag("dashboard-open-inventory").assertDoesNotExist()
        onNodeWithTag("nav-INVENTORY").performClick()
        onNodeWithTag("inventory-item-stock").assertExists()
        onNodeWithTag("inventory-privacy").performClick()
        waitUntil(5000) { !workspace.inventory.state.value.visible }
        onNodeWithTag("nav-DASHBOARD").performClick()
        onNodeWithText(PersianNumberFormatter.formatPrice(84_000_000)).assertDoesNotExist()
        onNodeWithTag("dashboard-privacy").performClick()
        waitUntil(5000) { workspace.inventory.state.value.visible }
        runOnIdle { workspace.inventory.openMovement(item, StockAdjustmentType.DEDUCT) }
        onNodeWithTag("movement-save").performClick()
        waitUntil(5000) { workspace.inventory.state.value.movement == null && workspace.inventory.state.value.items.single().quantity == 2 }
        val state = workspace.inventory.state.value
        onNodeWithText(PersianNumberFormatter.formatWeight(state.summary.gold18)).assertExists()
        onNodeWithText(PersianNumberFormatter.formatPrice(state.metalValue!!)).assertExists()
        save(onNodeWithTag("workspace-root"), "unified-dashboard.png")
        onNodeWithTag("nav-SETTINGS").performClick()
        onNodeWithTag("open-previous-portfolio").performScrollTo().performClick()
        onNodeWithText("دارایی قبلی").assertExists()
        onNodeWithTag("previous-portfolio-back").performClick()
        assertEquals(DesktopDestination.SETTINGS, workspace.state.value.destination)
        assertEquals("old", store.getItems().single().id)
    }

    @Test fun dashboardQuickAddCreatesFullInventoryRecordAndNoPortfolioRecord() = runDesktopComposeUiTest(width = 940, height = 800) {
        setContent { GoldExCompanionTheme { DesktopWorkspaceScreen(workspace, {}, "0.56.42") } }
        onNodeWithTag("quick-ثبت محصول").performScrollTo().performClick()
        fun field(tag: String) = onNode(hasSetTextAction() and hasAnyAncestor(hasTestTag(tag)))
        field("inventory-title").performTextReplacement("کالای ثبت‌شده از پیشخوان")
        field("inventory-gross").performTextReplacement("۲٫۱۲۵")
        field("inventory-quantity").performTextReplacement("۲")
        onNodeWithTag("inventory-save").performClick()
        waitUntil(5000) { workspace.inventory.state.value.draft == null && workspace.inventory.state.value.items.size == 2 }
        assertEquals(DesktopDestination.INVENTORY, workspace.state.value.destination)
        assertEquals(2.125, store.inventory.getItems().first { it.title == "کالای ثبت‌شده از پیشخوان" }.grossWeightGrams)
        assertEquals(1, store.getItems().size)
        save(onNodeWithTag("workspace-root"), "unified-inventory-compact.png")
    }

    @Test fun missingQuoteKeepsInventoryWeightWithoutInventingMarketValue() = runDesktopComposeUiTest(width = 940, height = 800) {
        waitUntil(5000) { workspace.inventory.state.value.spot18 == 6_000_000L }
        workspace.inventory.quote(0)
        setContent { GoldExCompanionTheme { DesktopWorkspaceScreen(workspace, {}, "0.56.42") } }
        onNodeWithText(PersianNumberFormatter.formatWeight(14.0)).assertExists()
        onNode(hasText("—") and hasAnyAncestor(hasTestTag("dashboard-inventory-value"))).assertExists()
        onNodeWithText(PersianNumberFormatter.formatPrice(84_000_000)).assertDoesNotExist()
        assertNull(workspace.inventory.state.value.metalValue)
    }

    private fun save(node: SemanticsNodeInteraction, name: String) {
        val bitmap = node.captureToImage().asSkiaBitmap()
        val image = org.jetbrains.skia.Image.makeFromBitmap(bitmap)
        val data = image.encodeToData(org.jetbrains.skia.EncodedImageFormat.PNG)!!
        File(System.getProperty("qirato.screenshotDir"), name).apply { parentFile.mkdirs(); writeBytes(data.bytes) }
        data.close(); image.close(); bitmap.close()
    }
}
