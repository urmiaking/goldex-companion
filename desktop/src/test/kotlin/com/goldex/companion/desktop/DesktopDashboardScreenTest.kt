package com.goldex.companion.desktop

import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.graphics.asSkiaBitmap
import com.goldex.companion.data.*
import com.goldex.companion.desktop.data.*
import com.goldex.companion.desktop.state.*
import com.goldex.companion.desktop.ui.DesktopWorkspaceScreen
import com.goldex.companion.model.TimeHorizon
import com.goldex.companion.ui.theme.GoldExCompanionTheme
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import kotlin.test.*

@OptIn(ExperimentalTestApi::class)
class DesktopDashboardScreenTest {
    @get:Rule val temporary = TemporaryFolder()
    private lateinit var store: DesktopDataStore
    private lateinit var workspace: DesktopWorkspace
    private val now = System.currentTimeMillis()

    @Before fun setup() {
        store = DesktopDataStore(temporary.newFolder().toPath())
        store.saveSettings(AppSettings(managerName = "استاد زرگر", galleryName = "گالری آزمون قیراط"))
        store.addItem(PortfolioItem(title = "طلای آزمون", category = PortfolioCategory.GOLD, weightGrams = 12.125, purchasePriceTotal = 65_000_000))
        store.inventory.addItem(com.goldex.companion.model.InventoryItem(code = "INV-1", title = "موجودی آزمون", grossWeightGrams = 12.125, quantity = 2))
        store.saveInventoryVisible(true)
        val market = DesktopMarketRepository(store, fetch = { error("offline") })
        market.useManual(DesktopMarketRepository.emptyRates().copy(gold18 = 6_000_000, goldMelt = 25_991_100, coinEmami = 60_000_000, ons = 2650.0))
        workspace = DesktopWorkspace(store, market, history = DesktopGoldHistoryGateway { horizon ->
            GoldHistorySnapshot(horizon, (0..60).map { index ->
                GoldHistoryPoint(now - (60 - index) * 60_000, 5_900_000 + index * 1200L + (index % 7) * 3500L, "${9 + index / 60}:${(index % 60).toString().padStart(2, '0')}")
            }, now)
        })
        workspace.dashboard.select(TimeHorizon.TODAY)
    }
    @After fun cleanup() { workspace.close(); store.close() }

    @Test fun wideDashboardMatchesMobileSectionsInBothThemes() = runDesktopComposeUiTest(width = 1400, height = 980) {
        setContent { val state by workspace.state.collectAsState(); GoldExCompanionTheme(isDarkTheme = state.dark) { DesktopWorkspaceScreen(workspace, {}, "0.56.39") } }
        waitUntil(5000) { workspace.dashboard.state.value.history.isNotEmpty() }
        onNodeWithTag("dashboard-vault").assertIsDisplayed()
        onNodeWithTag("dashboard-market").assertIsDisplayed()
        onNodeWithTag("dashboard-invoices").assertIsDisplayed()
        onNodeWithTag("history-canvas").assertIsDisplayed()
        onNodeWithTag("history-point").performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.SetProgress) { it(0f) }
        onNodeWithText("۹:۰۰").assertExists()
        save(onNodeWithTag("workspace-root"), "dashboard-wide-light.png")
        onNodeWithTag("workspace-theme").performClick()
        waitUntil(5000) { workspace.state.value.dark }
        save(onNodeWithTag("workspace-root"), "dashboard-wide-dark.png")
        onNodeWithText("هفتگی").performClick()
        waitUntil(5000) { workspace.dashboard.state.value.horizon == TimeHorizon.ONE_WEEK && !workspace.dashboard.state.value.loading }
        onNodeWithTag("history-canvas").assertExists()
    }

    @Test fun compactDashboardScrollsToChartAndInvoices() = runDesktopComposeUiTest(width = 940, height = 700) {
        setContent { GoldExCompanionTheme { DesktopWorkspaceScreen(workspace, {}, "0.56.39") } }
        waitUntil(5000) { workspace.dashboard.state.value.history.isNotEmpty() }
        save(onNodeWithTag("workspace-root"), "dashboard-compact.png")
        onNodeWithTag("dashboard-invoices").performScrollTo().assertIsDisplayed()
        save(onNodeWithTag("workspace-root"), "dashboard-compact-invoices.png")
        onNodeWithTag("nav-RATES").performClick()
        onNodeWithTag("nav-DASHBOARD").performClick()
        onNodeWithTag("dashboard-invoices").assertIsDisplayed()
        onNodeWithTag("dashboard-trend").performScrollTo().assertIsDisplayed()
    }

    @Test fun interruptedPageTransitionKeepsNewestDestinationAndDraft() = runDesktopComposeUiTest(width = 1100, height = 800) {
        setContent { GoldExCompanionTheme { DesktopWorkspaceScreen(workspace, {}, "0.56.39") } }
        runOnIdle { workspace.editSettings { it.copy(galleryName = "پیش‌نویس محفوظ") } }
        mainClock.autoAdvance = false
        onNodeWithTag("nav-SETTINGS").performClick()
        mainClock.advanceTimeBy(80)
        onNodeWithTag("nav-RATES").performClick()
        mainClock.advanceTimeBy(60)
        onNodeWithTag("nav-SETTINGS").performClick()
        mainClock.autoAdvance = true
        onNode(hasSetTextAction() and hasAnyAncestor(hasTestTag("settings-gallery"))).assertTextContains("پیش‌نویس محفوظ")
        runOnIdle { assertEquals(DesktopDestination.SETTINGS, workspace.state.value.destination) }
    }

    @Test fun reducedMotionPersistsAndNavigationWorksWithoutTransitionDelay() = runDesktopComposeUiTest(width = 1100, height = 800) {
        runBlocking { workspace.setReduceMotion(true)!!.join() }
        setContent { GoldExCompanionTheme { DesktopWorkspaceScreen(workspace, {}, "0.56.39") } }
        onNodeWithTag("nav-SETTINGS").performClick()
        onNodeWithTag("reduce-motion").performScrollTo().assertIsOn()
        onNodeWithTag("nav-DASHBOARD").performClick()
        onNodeWithTag("dashboard-greeting").assertIsDisplayed()
        assertTrue(store.loadReduceMotion())
    }

    @Test fun largeVaultBalanceRemainsFullyReadableInCompactWindow() = runDesktopComposeUiTest(width = 940, height = 700) {
        workspace.close()
        store.inventory.addItem(com.goldex.companion.model.InventoryItem(code = "INV-BIG", title = "موجودی بزرگ آزمون", grossWeightGrams = 1_000_000.0))
        val market = DesktopMarketRepository(store, fetch = { error("offline") })
        market.useManual(DesktopMarketRepository.emptyRates().copy(gold18 = 1_000_000_000_000))
        workspace = DesktopWorkspace(store, market, history = DesktopGoldHistoryGateway { error("offline") })
        waitUntil(5000) { workspace.inventory.state.value.spot18 == 1_000_000_000_000 }
        setContent { GoldExCompanionTheme { DesktopWorkspaceScreen(workspace, {}, "0.56.39") } }
        val amount = com.goldex.companion.model.PersianNumberFormatter.formatPrice(workspace.inventory.state.value.metalValue!!)
        val layouts = mutableListOf<androidx.compose.ui.text.TextLayoutResult>()
        onNodeWithText(amount).performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.GetTextLayoutResult) { it(layouts) }
        save(onNodeWithTag("workspace-root"), "dashboard-large-balance.png")
        val layout = layouts.single()
        // Desktop paragraphWidth retains max constraints even when text measures to its intrinsic width.
        // Check the actual rendered line and every character, rather than that paragraph-width flag.
        assertFalse(layout.didOverflowHeight)
        assertEquals(amount.length, layout.getLineEnd(0))
        assertTrue(layout.getLineRight(0) - layout.getLineLeft(0) <= layout.size.width + 1)
    }

    private fun save(node: SemanticsNodeInteraction, name: String) {
        val bitmap = node.captureToImage().asSkiaBitmap()
        val image = org.jetbrains.skia.Image.makeFromBitmap(bitmap)
        val data = image.encodeToData(org.jetbrains.skia.EncodedImageFormat.PNG)!!
        File(System.getProperty("qirato.screenshotDir"), name).apply { parentFile.mkdirs(); writeBytes(data.bytes) }
        data.close(); image.close(); bitmap.close()
    }
}
