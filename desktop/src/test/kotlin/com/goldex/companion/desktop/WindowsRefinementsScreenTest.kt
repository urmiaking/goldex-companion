package com.goldex.companion.desktop

import androidx.compose.runtime.*
import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.test.*
import com.goldex.companion.desktop.data.*
import com.goldex.companion.desktop.state.*
import com.goldex.companion.desktop.ui.DesktopWorkspaceScreen
import com.goldex.companion.desktop.update.*
import com.goldex.companion.model.*
import com.goldex.companion.presentation.calculator.CalculatorField
import com.goldex.companion.ui.theme.GoldExCompanionTheme
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import org.junit.*
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.net.URI
import java.nio.file.Path
import kotlin.test.*

@OptIn(ExperimentalTestApi::class)
class WindowsRefinementsScreenTest {
    @get:Rule val temporary = TemporaryFolder()
    private lateinit var store: DesktopDataStore
    private lateinit var workspace: DesktopWorkspace
    private var updater: WindowsUpdater? = null
    @Before fun setup() {
        store = DesktopDataStore(temporary.newFolder().toPath())
        store.saveInventoryVisible(true)
        store.saveMarket(MarketSnapshot(DesktopMarketRepository.emptyRates().copy(gold18 = 6_000_000), 100_000, QuoteKind.ONLINE))
        repeat(14) { index -> store.inventory.addItem(InventoryItem(id = "item-$index", code = "RNG-$index", title = "حلقه طلای آوا ${index + 1}", grossWeightGrams = 4.125, stoneWeightGrams = .125, quantity = 3, location = "سینی ویترین اصلی")) }
        workspace = DesktopWorkspace(store, DesktopMarketRepository(store, { error("offline fixture") }), history = DesktopGoldHistoryGateway { error("offline") })
    }
    @After fun close() { updater?.close(); workspace.close(); store.close() }

    @Test fun startupOffersUpdateAndDownloadNeverBlocksWorkOrOpensPrompt() = runDesktopComposeUiTest(width = 1400, height = 980) {
        val finish = Channel<Unit>(Channel.UNLIMITED)
        var downloads = 0
        var restarts = 0
        val release = WindowsRelease(WindowsVersion(0,56,99), "windows-v0.56.99", "• بهبود انبار و محاسبه", URI("https://github.com"), 100, "a".repeat(64))
        updater = WindowsUpdater(object : WindowsUpdateGateway {
            override suspend fun check() = release
            override suspend fun prepare(release: WindowsRelease, progress: (Long,Long)->Unit, verifying: ()->Unit): PreparedWindowsUpdate {
                downloads++; progress(50,100); finish.receive(); verifying()
                return PreparedWindowsUpdate(release, Path.of("app"), Path.of("stage"), Path.of("bundle"))
            }
            override suspend fun launch(update: PreparedWindowsUpdate) = error("UI emits intent only")
        })
        setContent { val state by workspace.state.collectAsState(); GoldExCompanionTheme(state.dark) { DesktopWorkspaceScreen(workspace, {}, "0.56.43", updater, { restarts++ }) } }
        waitUntil(5000) { updater!!.state.value.phase == WindowsUpdatePhase.AVAILABLE }
        assertEquals(0, downloads)
        onNodeWithTag("update-dialog").assertDoesNotExist()
        onNodeWithTag("nav-CALCULATOR").performClick()
        onNodeWithTag("calculator-rate-status").assertTextEquals("نرخ ذخیره‌شده")
        onNodeWithTag("input-SPOT").assertTextContains("۶،۰۰۰،۰۰۰")
        onNodeWithTag("open-updater").performClick()
        waitUntil(5000) { updater!!.state.value.received == 50L }
        onNodeWithText("دانلود ۵۰٪").assertExists()
        onNodeWithTag("update-dialog").assertDoesNotExist()
        onNodeWithTag("input-GROSS_WEIGHT").performTextReplacement("2.125")
        assertEquals("2.125", workspace.calculator.state.value.input(CalculatorField.GROSS_WEIGHT))
        save(onNodeWithTag("workspace-root"), "update-background-download.png")
        onNodeWithTag("nav-INVENTORY").performClick()
        onNodeWithTag("inventory-add").performClick()
        field("inventory-title").performTextInput("پیش‌نویس هنگام دانلود")
        finish.trySend(Unit)
        waitUntil(5000) { updater!!.state.value.phase == WindowsUpdatePhase.READY }
        assertFalse(updater!!.state.value.dialog)
        field("inventory-title").assertTextContains("پیش‌نویس هنگام دانلود")
        onNodeWithTag("update-dialog").assertDoesNotExist()
        runOnIdle { workspace.inventory.discard() }
        onNodeWithTag("open-updater").performClick()
        onNodeWithText("• بهبود انبار و محاسبه").assertExists()
        onNodeWithTag("restart-update").performClick()
        assertEquals(1, restarts)
        onNodeWithTag("postpone-update").performClick()
        onNodeWithTag("update-dialog").assertDoesNotExist()
        assertEquals(1, downloads)
    }

    @Test fun inventoryGivesMoreSpaceToSelectionAndUnitsStayOnLeftInBothThemes() = runDesktopComposeUiTest(width = 1400, height = 980) {
        workspace.navigate(DesktopDestination.INVENTORY)
        setContent { val state by workspace.state.collectAsState(); GoldExCompanionTheme(state.dark) { DesktopWorkspaceScreen(workspace, {}, "0.56.43") } }
        onNodeWithTag("inventory-item-item-0").performClick().assertIsSelected()
        val master = onNodeWithTag("inventory-master").fetchSemanticsNode().boundsInRoot
        val detail = onNodeWithTag("inventory-detail-pane").fetchSemanticsNode().boundsInRoot
        assertTrue(detail.width > master.width)
        assertTrue(onNodeWithTag("inventory-item-item-0").fetchSemanticsNode().boundsInRoot.height < 100f)
        onNodeWithTag("inventory-charge").assertIsDisplayed()
        save(onNodeWithTag("workspace-root"), "inventory-refined-light.png")
        onNodeWithTag("workspace-theme").performClick()
        waitUntil(5000) { workspace.state.value.dark }
        save(onNodeWithTag("workspace-root"), "inventory-refined-dark.png")
        onNodeWithTag("inventory-add").performClick()
        fun unit(label: String, tag: String, text: String) {
            val node = onNodeWithTag("field-unit-$label", useUnmergedTree = true)
            node.assertTextEquals(text)
            assertTrue(node.fetchSemanticsNode().boundsInRoot.center.x < field(tag).fetchSemanticsNode().boundsInRoot.center.x)
        }
        unit("وزن ناخالص", "inventory-gross", "گرم")
        unit("وزن نگین", "inventory-stone", "گرم")
        unit("عیار دقیق", "inventory-purity", "از ۱۰۰۰")
        unit("تعداد", "inventory-quantity", "قطعه")
        unit("اجرت", "inventory-wage", "٪")
        onNodeWithText("اجرت هر گرم").performClick()
        unit("اجرت", "inventory-wage", "تومان / گرم")
        field("inventory-wage").performTextReplacement("123456")
        field("inventory-gross").performTextReplacement("۲٫۱۲۵")
        save(onNodeWithTag("inventory-dialog"), "inventory-unit-adornments.png")
        assertEquals("۲٫۱۲۵", workspace.inventory.state.value.draft!!.gross)
    }

    @Test fun pagesMoveVerticallyAndRetainHorizontalAlignment() = runDesktopComposeUiTest(width = 1100, height = 800) {
        setContent { GoldExCompanionTheme { DesktopWorkspaceScreen(workspace, {}, "0.56.43") } }
        mainClock.autoAdvance = false
        onNodeWithTag("nav-CALCULATOR").performClick()
        mainClock.advanceTimeBy(80)
        val moving = onNodeWithTag("page-CALCULATOR").fetchSemanticsNode().positionInRoot
        mainClock.advanceTimeBy(500)
        val resting = onNodeWithTag("page-CALCULATOR").fetchSemanticsNode().positionInRoot
        assertEquals(resting.x, moving.x)
        assertTrue(moving.y > resting.y)
        onNodeWithTag("nav-DASHBOARD").performClick()
        mainClock.advanceTimeBy(80)
        val back = onNodeWithTag("page-DASHBOARD").fetchSemanticsNode().positionInRoot
        mainClock.advanceTimeBy(500)
        val backRest = onNodeWithTag("page-DASHBOARD").fetchSemanticsNode().positionInRoot
        assertEquals(backRest.x, back.x)
        assertTrue(back.y < backRest.y)
        mainClock.autoAdvance = true
    }

    @Test fun explicitManualQuoteDoesNotClaimAnApiSource() = runDesktopComposeUiTest(width = 1400, height = 980) {
        runBlocking { workspace.saveManualRates(DesktopMarketRepository.emptyRates().copy(gold18 = 7_000_000))!!.join() }
        workspace.navigate(DesktopDestination.CALCULATOR)
        setContent { GoldExCompanionTheme { DesktopWorkspaceScreen(workspace, {}, "0.56.43") } }
        onNodeWithTag("calculator-rate-status").assertTextEquals("نرخ دستی")
        onNodeWithText("ثبت‌شده توسط شما", substring = true).assertExists()
        onNodeWithTag("input-SPOT").assertTextContains("۷،۰۰۰،۰۰۰")
    }

    private fun ComposeUiTest.field(tag: String) = onNode(hasSetTextAction() and hasAnyAncestor(hasTestTag(tag)))
    private fun save(node: SemanticsNodeInteraction, name: String) {
        val bitmap = node.captureToImage().asSkiaBitmap()
        org.jetbrains.skia.Image.makeFromBitmap(bitmap).use { image -> image.encodeToData()!!.use { data ->
            File(System.getProperty("qirato.screenshotDir"), name).apply { parentFile.mkdirs(); writeBytes(data.bytes) }
        } }
        bitmap.close()
    }
}
