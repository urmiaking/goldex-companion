package com.goldex.companion.desktop

import androidx.compose.runtime.*
import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import com.goldex.companion.data.*
import com.goldex.companion.desktop.data.*
import com.goldex.companion.desktop.state.*
import com.goldex.companion.desktop.ui.*
import com.goldex.companion.model.*
import com.goldex.companion.ui.theme.GoldExCompanionTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.time.*
import kotlin.test.*

@OptIn(ExperimentalTestApi::class)
class DesktopRateDetailScreenTest {
    @get:Rule val temporary=TemporaryFolder()
    private lateinit var store: DesktopDataStore
    private lateinit var workspace: DesktopWorkspace
    private val now=System.currentTimeMillis()
    private val zone=ZoneId.of("Asia/Tehran")
    private val rates=DesktopMarketRepository.emptyRates().copy(gold18=26_258_100,gold24=35_010_800,goldMelt=113_757_000,
        coinEmami=54_850_000,coinBahar=49_400_000,coinHalf=28_200_000,coinQuarter=17_900_000,coinGerami=8_200_000,usd=92_850,ons=4134.87)
    @Before fun setup() {
        store=DesktopDataStore(temporary.newFolder().toPath())
        val market=DesktopMarketRepository(store,fetch={rates});runBlocking { market.refreshRates() }
        workspace=DesktopWorkspace(store,market,connectivity=object : ConnectivityObserver { override val status=MutableStateFlow(ConnectionStatus.ONLINE) },
            ratesBoardGateway=object : DesktopRatesBoardGateway {
                override fun cached()=BoardSnapshot(now,emptyMap(),emptyMap())
                override suspend fun load(previous: BoardSnapshot?)=cached()
            },rateHistoryGateway=object : DesktopRateHistoryGateway {
                override suspend fun load(instrument: BoardInstrument,horizon: TimeHorizon): RateHistorySnapshot {
                    val base=(instrument.primary(rates) ?: 25_350.0).let { if(instrument.dollar)(it*100).toLong() else it.toLong() }
                    val today=Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
                    val start=today.atStartOfDay(zone).toInstant().toEpochMilli()
                    val count=if(horizon==TimeHorizon.TODAY)60 else if(horizon==TimeHorizon.ONE_YEAR)365 else if(horizon==TimeHorizon.ONE_MONTH)30 else 7
                    val points=(0 until count).map { i ->
                        val at=if(horizon==TimeHorizon.TODAY)start+((now-start-1)*i/(count-1)).coerceAtLeast(1) else today.minusDays((count-1-i).toLong()).atStartOfDay(zone).toInstant().toEpochMilli()
                        val price=base+(base*.003*kotlin.math.sin(i.toDouble()/8)).toLong()
                        RateHistoryPoint(at,price,price-base/1000,price+base/1000,if(horizon==TimeHorizon.TODAY)Instant.ofEpochMilli(at).atZone(zone).toLocalTime().toString().take(5) else "۱۴۰۵/${(i/30)%12+1}/${i%30+1}")
                    }
                    return RateHistorySnapshot(instrument,horizon,points,now)
                }
            })
        workspace.navigate(DesktopDestination.RATES)
    }
    @After fun cleanup() { workspace.close();store.close() }

    @Test fun summaryCardOpensRealPageWithSharedHeaderAndAllTimeframes()=runDesktopComposeUiTest(width=1440,height=1080) {
        setContent { val state by workspace.state.collectAsState();GoldExCompanionTheme(isDarkTheme=state.dark) { DesktopWorkspaceScreen(workspace,{},"test") } }
        onNodeWithTag("rates-summary-GOLD18").performClick()
        waitUntil(5000) { !workspace.rateDetail.state.value.loading && !workspace.rateDetail.state.value.statsLoading }
        onNodeWithTag("rate-details-page").assertExists();onNodeWithTag("rate-details-dialog").assertDoesNotExist()
        assertEquals(DesktopDestination.RATE_DETAIL,workspace.state.value.destination)
        onAllNodesWithTag("connection-chip").assertCountEquals(1);onNodeWithTag("workspace-date").assertIsDisplayed()
        onNodeWithTag("rate-history-chart").assertIsDisplayed()
        onNodeWithText("همگام‌سازی نرخ‌ها").assertDoesNotExist()
        save(onNodeWithTag("workspace-root"),"rate-detail-light.png")
        onNodeWithTag("rate-statistics").performScrollTo()
        save(onNodeWithTag("workspace-root"),"rate-detail-statistics-light.png")
        onNodeWithText("سند قابل نمایش در دسترس نیست").assertExists()
        onNodeWithText("تسویه شد").assertDoesNotExist()
        onNodeWithTag("rate-chart-card").performScrollTo()
        for(horizon in RateDetailHorizons.drop(1)) {
            onNode(hasText(horizonLabel(horizon)) and hasAnyAncestor(hasTestTag("rate-detail-horizon"))).performClick()
            waitUntil(5000) { workspace.rateDetail.state.value.horizon==horizon && !workspace.rateDetail.state.value.loading }
            assertEquals(horizon,workspace.rateDetail.state.value.active!!.horizon)
        }
        onNodeWithTag("workspace-theme").performClick();waitUntil(5000) { workspace.state.value.dark }
        save(onNodeWithTag("workspace-root"),"rate-detail-dark.png")
        onNodeWithTag("rate-details-close").performScrollTo().performClick()
        onNodeWithTag("rates-summary-GOLD18").assertExists()
    }
    @Test fun tableItemNavigationRetainsScrollAndAllInstrumentsHaveOwnData()=runDesktopComposeUiTest(width=1280,height=900) {
        setContent { GoldExCompanionTheme { DesktopWorkspaceScreen(workspace,{},"test") } }
        onNodeWithTag("rate-row-GERAMI").performScrollTo().performClick()
        waitUntil(5000) { !workspace.rateDetail.state.value.loading }
        assertEquals(BoardInstrument.GERAMI,workspace.rateDetail.state.value.active!!.instrument)
        onNodeWithTag("rate-details-close").performScrollTo().performClick()
        onNodeWithTag("rate-row-GERAMI").assertIsDisplayed()
        for(instrument in BoardInstrument.values().filter { it != BoardInstrument.TETHER }) {
            onNodeWithTag("rate-detail-${instrument.name}").performScrollTo().performClick()
            waitUntil(5000) { !workspace.rateDetail.state.value.loading }
            assertEquals(instrument,workspace.rateDetail.state.value.active!!.instrument)
            assertEquals(DesktopDestination.RATE_DETAIL,workspace.state.value.destination)
            onNodeWithTag("rate-details-close").performScrollTo().performClick()
        }
    }
    @Test fun chartKeyboardAndPointerShowSameActualPriceWithoutChangingCalculatorDraft()=runDesktopComposeUiTest(width=1280,height=1000) {
        workspace.calculatorRates.setInput(com.goldex.companion.presentation.calculator.CalculatorField.SPOT,"123456")
        workspace.openRateDetail(BoardInstrument.OUNCE)
        setContent { GoldExCompanionTheme { DesktopWorkspaceScreen(workspace,{},"test") } }
        waitUntil(5000) { !workspace.rateDetail.state.value.loading }
        val h=workspace.rateDetail.state.value.active!!
        val chart=onNodeWithTag("rate-history-chart")
        chart.performSemanticsAction(SemanticsActions.RequestFocus)
        chart.performKeyInput { pressKey(Key.MoveHome) }
        val state=chart.fetchSemanticsNode().config[SemanticsProperties.StateDescription]
        assertTrue(state.contains(historyAmount(h.points.first().price,BoardInstrument.OUNCE)))
        chart.performKeyInput { pressKey(Key.MoveEnd) }
        assertTrue(chart.fetchSemanticsNode().config[SemanticsProperties.StateDescription].contains(historyAmount(h.points.last().price,BoardInstrument.OUNCE)))
        chart.performMouseInput { moveTo(center) }
        onNodeWithTag("rate-history-tooltip").assertIsDisplayed()
        assertEquals("123456",workspace.calculator.state.value.input(com.goldex.companion.presentation.calculator.CalculatorField.SPOT))
        onNodeWithTag("rate-use-calculator").assertDoesNotExist()
    }
    @Test fun compactLayoutKeepsChartStatisticsAndNavigationAccessible()=runDesktopComposeUiTest(width=940,height=700) {
        workspace.openRateDetail(BoardInstrument.EMAMI)
        setContent { val s by workspace.state.collectAsState();GoldExCompanionTheme(isDarkTheme=s.dark) { DesktopWorkspaceScreen(workspace,{},"test") } }
        waitUntil(5000) { !workspace.rateDetail.state.value.loading }
        onNodeWithTag("workspace-date").assertIsDisplayed();onNodeWithTag("rate-chart-card").performScrollTo()
        save(onNodeWithTag("workspace-root"),"rate-detail-compact.png")
        onNodeWithTag("rate-statistics").performScrollTo().assertIsDisplayed()
        onNodeWithTag("rate-documents").performScrollTo().assertIsDisplayed()
        onNodeWithTag("rate-details-close").performScrollTo().performClick()
        assertEquals(DesktopDestination.RATES,workspace.state.value.destination)
    }
    private fun save(node: SemanticsNodeInteraction,name: String) {
        val bitmap=node.captureToImage().asSkiaBitmap();val image=org.jetbrains.skia.Image.makeFromBitmap(bitmap)
        val data=image.encodeToData(org.jetbrains.skia.EncodedImageFormat.PNG)!!
        File(System.getProperty("qirato.screenshotDir")).apply { mkdirs() }.resolve(name).writeBytes(data.bytes)
        data.close();image.close();bitmap.close()
    }
}
