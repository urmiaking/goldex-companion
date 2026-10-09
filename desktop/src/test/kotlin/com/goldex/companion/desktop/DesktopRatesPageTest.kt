package com.goldex.companion.desktop

import androidx.compose.runtime.*
import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.text.TextLayoutResult
import com.goldex.companion.data.*
import com.goldex.companion.desktop.data.*
import com.goldex.companion.desktop.state.*
import com.goldex.companion.desktop.update.*
import com.goldex.companion.desktop.ui.DesktopWorkspaceScreen
import com.goldex.companion.model.*
import com.goldex.companion.presentation.calculator.CalculatorField
import com.goldex.companion.ui.theme.GoldExCompanionTheme
import com.goldex.companion.ui.theme.VazirmatnFamily
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.*
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.net.URI
import java.nio.file.Path
import kotlin.test.*

@OptIn(ExperimentalTestApi::class)
class DesktopRatesPageTest {
    @get:Rule val temporary = TemporaryFolder()
    private lateinit var store: DesktopDataStore
    private lateinit var workspace: DesktopWorkspace
    private val connection = MutableStateFlow(ConnectionStatus.ONLINE)
    private val now = System.currentTimeMillis()
    private val quotes = DesktopMarketRepository.emptyRates().copy(gold18=26_258_100,gold24=35_010_800,goldMelt=113_757_000,
        coinEmami=54_850_000,coinBahar=49_400_000,coinHalf=28_200_000,coinQuarter=17_900_000,coinGerami=8_200_000,usd=92_850,ons=2645.8)
    @Before fun setup() {
        store = DesktopDataStore(temporary.newFolder().toPath())
        store.saveSettings(AppSettings(managerName="محمدحسین فانی",galleryName="طلا و جواهر فانی"))
        val market = DesktopMarketRepository(store,fetch={ quotes })
        runBlocking { market.refreshRates() }
        val references = BoardInstrument.values().associateWith { instrument ->
            val p = instrument.primary(quotes) ?: if (instrument == BoardInstrument.AED) 25_350.0 else 93_150.0
            if (instrument == BoardInstrument.GERAMI) BoardDayQuote(p,p*.995,p*1.004,0.0,0.0)
            else BoardDayQuote(p,p*.995,p*1.004,if (instrument.dollar) -5.4 else p*.005,if (instrument.dollar) -.2 else .5)
        }
        val snapshot = BoardSnapshot(now,references,references.mapValues { (_,q) -> BoardHistory(listOf(q.price*.995,q.price*.997,q.price*.996,q.price),now) })
        workspace = DesktopWorkspace(store,market,connectivity=object : ConnectivityObserver {
            override val status = connection
        },rateHistoryGateway=object : DesktopRateHistoryGateway { override suspend fun load(instrument: BoardInstrument,horizon: TimeHorizon): RateHistorySnapshot = error("Offline fixture") },ratesBoardGateway=object : DesktopRatesBoardGateway {
            override fun cached() = snapshot
            override suspend fun load(previous: BoardSnapshot?) = snapshot
        })
        workspace.navigate(DesktopDestination.RATES)
    }
    @After fun cleanup() { workspace.close(); store.close() }

    @Test fun wideBoardShowsOnlyRequestedMarketsAndDetailsAcrossNavigation() = runDesktopComposeUiTest(width=1280,height=1080) {
        setContent { val state by workspace.state.collectAsState(); GoldExCompanionTheme(isDarkTheme=state.dark) { DesktopWorkspaceScreen(workspace,{},"0.56.47") } }
        waitUntil(5000) { !workspace.ratesBoard.state.value.cached }
        onNodeWithTag("rates-summary-GOLD18").assertIsDisplayed()
        onNodeWithTag("rates-summary-MELT").assertIsDisplayed()
        onNodeWithTag("rates-summary-OUNCE").assertIsDisplayed()
        onNodeWithTag("rates-summary-EMAMI").assertIsDisplayed()
        onNodeWithTag("workspace-header").assertIsDisplayed()
        onNodeWithTag("workspace-date").assertIsDisplayed()
        onAllNodesWithTag("connection-chip").assertCountEquals(1)
        onNodeWithTag("rates-filter").assertDoesNotExist()
        onNodeWithTag("manual-rates").assertDoesNotExist()
        onNodeWithTag("rate-row-TETHER").assertDoesNotExist()
        onNodeWithText("اسپرد").assertDoesNotExist()
        onAllNodesWithText("جزئیات").assertCountEquals(11)
        onNode(hasText("بدون نوسان") and hasAnyAncestor(hasTestTag("rate-row-GERAMI")),useUnmergedTree=true).assertExists()
        val textLayouts = mutableListOf<TextLayoutResult>()
        onNodeWithText(BoardInstrument.GOLD18.title, useUnmergedTree=true)
            .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(textLayouts) }
        assertEquals(VazirmatnFamily, textLayouts.single().layoutInput.style.fontFamily)
        onNodeWithTag("rates-gold-currency").assertExists()
        onNodeWithTag("rates-coins").assertExists()
        onNodeWithTag("rates-summary-GOLD18").performMouseInput { moveTo(center) }
        save(onNodeWithTag("workspace-root"),"rates-refined-header-light.png")
        save(onNodeWithTag("rates-summary-GOLD18"),"rates-card-hover.png")
        onNodeWithTag("rates-gold-currency").performScrollTo()
        save(onNodeWithTag("rates-gold-currency"),"rates-refined-gold-light.png")
        onNodeWithTag("rate-row-AED").assertExists()
        onNodeWithTag("rates-coins").performScrollTo()
        save(onNodeWithTag("rates-coins"),"rates-refined-coins-light.png")
        onNodeWithTag("workspace-theme").performClick()
        waitUntil(5000) { workspace.state.value.dark }
        save(onNodeWithTag("rates-coins"),"rates-refined-coins-dark.png")
        onNodeWithTag("rates-gold-currency").performScrollTo()
        save(onNodeWithTag("rates-gold-currency"),"rates-refined-gold-dark.png")
        onNodeWithTag("nav-SETTINGS").performClick()
        onNodeWithTag("nav-RATES").performClick()
        onNodeWithTag("rate-row-AED").assertExists()
        onNodeWithTag("rate-row-GOLD18").assertExists()
        onNodeWithTag("rate-row-TETHER").assertDoesNotExist()
    }

    @Test fun everyPageSharesTodayAndConnectionWithGreetingOnlyOnDashboard() = runDesktopComposeUiTest(width=1280,height=1000) {
        setContent { GoldExCompanionTheme { DesktopWorkspaceScreen(workspace, {}, "0.56.48") } }
        val date = com.goldex.companion.desktop.ui.workspaceDate(workspace.state.value.now)
        DesktopDestination.mainDestinations.forEach { page ->
            onNodeWithTag("nav-${page.name}").performClick()
            onAllNodesWithTag("connection-chip").assertCountEquals(1)
            onNode(hasText(date) and hasAnyAncestor(hasTestTag("workspace-date"))).assertExists()
            onNodeWithTag("refresh-rates").assertDoesNotExist()
            onNodeWithTag("rates-refresh").assertDoesNotExist()
            if (page != DesktopDestination.DASHBOARD) onNodeWithText(DesktopDashboard.greeting(workspace.state.value.now), substring=true).assertDoesNotExist()
        }
    }

    @Test fun detailsUseSelectedGoldBasisAndPreserveOtherCalculatorInput() = runDesktopComposeUiTest(width=1280,height=1000) {
        setContent { GoldExCompanionTheme { DesktopWorkspaceScreen(workspace,{},"0.56.47") } }
        runOnIdle { workspace.calculatorRates.setInput(CalculatorField.GROSS_WEIGHT,"2.125") }
        onNodeWithTag("rate-detail-GOLD24").performScrollTo().performClick()
        onNodeWithTag("rate-details-page").assertExists()
        save(onNodeWithTag("rate-details-page"),"rates-stitch-detail.png")
        onNodeWithTag("rate-use-calculator").performScrollTo().performClick()
        runOnIdle {
            assertEquals(DesktopDestination.CALCULATOR,workspace.state.value.destination)
            assertEquals(PriceBasisTab.K24,workspace.calculator.state.value.priceBasis)
            assertEquals(quotes.gold24.toString(),workspace.calculator.state.value.input(CalculatorField.SPOT))
            assertEquals("2.125",workspace.calculator.state.value.input(CalculatorField.GROSS_WEIGHT))
        }
    }

    @Test fun compactStoredQuotesStayHonestWithoutManualEntry() = runDesktopComposeUiTest(width=850,height=1000) {
        runBlocking { workspace.saveManualRates(quotes.copy(gold24=0,coinGerami=0))!!.join() }
        setContent { val state by workspace.state.collectAsState(); GoldExCompanionTheme(isDarkTheme=state.dark) { DesktopWorkspaceScreen(workspace,{},"0.56.47") } }
        onNodeWithTag("rate-row-GOLD24").performScrollTo()
        assertTrue(onAllNodes(hasText("—") and hasAnyAncestor(hasTestTag("rate-row-GOLD24")),useUnmergedTree=true).fetchSemanticsNodes().isNotEmpty())
        onNodeWithTag("rate-detail-GOLD24").performClick()
        onNodeWithTag("rate-use-calculator").performScrollTo().assertIsNotEnabled()
        onNodeWithTag("rate-details-close").performScrollTo().performClick()
        onNodeWithTag("rates-summary-GOLD18").performScrollTo()
        save(onNodeWithTag("workspace-root"),"rates-refined-compact-light.png")
        onNodeWithTag("manual-rates").assertDoesNotExist()
        onNodeWithTag("rates-summary-GOLD18").performClick()
        waitUntil(5000) { !workspace.rateDetail.state.value.loading }
        onNodeWithText("تاریخچه معتبر برای این نماد و بازه در دسترس نیست.").assertExists()
        onNodeWithTag("rate-details-close").performScrollTo().performClick()
        onNodeWithTag("workspace-theme").performClick()
        waitUntil(5000) { workspace.state.value.dark }
        save(onNodeWithTag("workspace-root"),"rates-refined-compact-dark.png")
    }

    @Test fun connectionPillDoesNotConfuseOfflineWithUnavailableSavedPrices() = runDesktopComposeUiTest(width=1280,height=1000) {
        connection.value = ConnectionStatus.OFFLINE
        setContent { GoldExCompanionTheme { DesktopWorkspaceScreen(workspace,{},"0.56.47") } }
        waitUntil(5000) { workspace.state.value.connection == ConnectionStatus.OFFLINE && !workspace.ratesBoard.state.value.loading }
        onNodeWithText("آفلاین").assertExists()
        onNodeWithTag("rates-refresh").assertDoesNotExist()
        onNodeWithTag("rate-detail-EMAMI").performScrollTo().performClick()
        onNodeWithTag("rate-details-page").assertExists()
        onNodeWithText("نرخ خرید یا توصیه معامله نیست.", substring=true).performScrollTo().assertExists()
        onNodeWithTag("rate-use-calculator").assertDoesNotExist()
    }

    @Test fun ratesHeaderKeepsUpdateDownloadAndReadyPromptAccessible() = runDesktopComposeUiTest(width=1280,height=1000) {
        val release = WindowsRelease(WindowsVersion(0,56,47), "windows-v0.56.47", "بهبودهای نسخه آزمایشی",
            URI("https://github.com"), 100, "a".repeat(64))
        val updater = WindowsUpdater(object : WindowsUpdateGateway {
            override suspend fun check() = release
            override suspend fun prepare(release: WindowsRelease, progress: (Long,Long)->Unit, verifying: ()->Unit) =
                PreparedWindowsUpdate(release, Path.of("Qirato"), Path.of("staging"), Path.of("bundle"))
            override suspend fun launch(update: PreparedWindowsUpdate) = error("Restart was not requested")
        })
        try {
            setContent { GoldExCompanionTheme { DesktopWorkspaceScreen(workspace,{},"0.56.46",updater) } }
            waitUntil(5000) { updater.state.value.phase == WindowsUpdatePhase.AVAILABLE }
            onNodeWithTag("open-updater").assertIsDisplayed().performClick()
            waitUntil(5000) { updater.state.value.phase == WindowsUpdatePhase.READY }
            onNodeWithTag("open-updater").assertIsDisplayed().performClick()
            onNodeWithTag("update-dialog").assertExists()
            onNodeWithTag("postpone-update").performClick()
            onNodeWithTag("workspace-header").assertIsDisplayed()
        } finally { updater.close() }
    }

    private fun save(node: SemanticsNodeInteraction,name: String) {
        val bitmap=node.captureToImage().asSkiaBitmap(); val image=org.jetbrains.skia.Image.makeFromBitmap(bitmap)
        val data=image.encodeToData(org.jetbrains.skia.EncodedImageFormat.PNG)!!
        File(System.getProperty("qirato.screenshotDir")).apply { mkdirs() }.resolve(name).writeBytes(data.bytes)
        data.close(); image.close(); bitmap.close()
    }
}
