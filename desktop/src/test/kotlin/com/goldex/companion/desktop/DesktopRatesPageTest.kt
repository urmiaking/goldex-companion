package com.goldex.companion.desktop

import androidx.compose.runtime.*
import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.test.*
import com.goldex.companion.data.*
import com.goldex.companion.desktop.data.*
import com.goldex.companion.desktop.state.*
import com.goldex.companion.desktop.ui.DesktopWorkspaceScreen
import com.goldex.companion.model.*
import com.goldex.companion.presentation.calculator.CalculatorField
import com.goldex.companion.ui.theme.GoldExCompanionTheme
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import kotlin.test.*

@OptIn(ExperimentalTestApi::class)
class DesktopRatesPageTest {
    @get:Rule val temporary = TemporaryFolder()
    private lateinit var store: DesktopDataStore
    private lateinit var workspace: DesktopWorkspace
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
            BoardDayQuote(p,p*.995,p*1.004,if (instrument.dollar) -5.4 else p*.005,if (instrument.dollar) -.2 else .5)
        }
        val snapshot = BoardSnapshot(now,references,references.mapValues { (_,q) -> BoardHistory(listOf(q.price*.995,q.price*.997,q.price*.996,q.price),now) })
        workspace = DesktopWorkspace(store,market,ratesBoardGateway=object : DesktopRatesBoardGateway {
            override fun cached() = snapshot
            override suspend fun load(previous: BoardSnapshot?) = snapshot
        })
        workspace.navigate(DesktopDestination.RATES)
    }
    @After fun cleanup() { workspace.close(); store.close() }

    @Test fun wideBoardMatchesReferenceAndFiltersRetainSelectionAcrossNavigation() = runDesktopComposeUiTest(width=1500,height=1060) {
        setContent { val state by workspace.state.collectAsState(); GoldExCompanionTheme(isDarkTheme=state.dark) { DesktopWorkspaceScreen(workspace,{},"0.56.45") } }
        waitUntil(5000) { !workspace.ratesBoard.state.value.cached }
        onNodeWithTag("rates-summary-GOLD18").assertIsDisplayed()
        onNodeWithTag("rates-summary-MELT").assertIsDisplayed()
        onNodeWithTag("rates-gold-currency").assertExists()
        onNodeWithTag("rates-coins").assertExists()
        save(onNodeWithTag("workspace-root"),"rates-stitch-wide-light.png")
        onNodeWithTag("rate-row-TETHER").performScrollTo()
        onNodeWithTag("rate-row-AED").assertExists()
        onNodeWithTag("rate-row-GERAMI").performScrollTo()
        save(onNodeWithTag("workspace-root"),"rates-stitch-coins-light.png")
        onNodeWithTag("workspace-theme").performClick()
        waitUntil(5000) { workspace.state.value.dark }
        save(onNodeWithTag("workspace-root"),"rates-stitch-coins-dark.png")
        onNodeWithText("ارز",useUnmergedTree=true).performScrollTo().performClick()
        onNodeWithTag("rate-row-GOLD18").assertDoesNotExist()
        onNodeWithTag("rate-row-AED").assertExists()
        onNodeWithTag("nav-SETTINGS").performClick()
        onNodeWithTag("nav-RATES").performClick()
        onNodeWithTag("rate-row-AED").assertExists()
        onNodeWithTag("rate-row-GOLD18").assertDoesNotExist()
    }

    @Test fun detailsUseSelectedGoldBasisAndPreserveOtherCalculatorInput() = runDesktopComposeUiTest(width=1500,height=1000) {
        setContent { GoldExCompanionTheme { DesktopWorkspaceScreen(workspace,{},"0.56.45") } }
        runOnIdle { workspace.calculatorRates.setInput(CalculatorField.GROSS_WEIGHT,"2.125") }
        onNodeWithTag("rate-detail-GOLD24").performScrollTo().performClick()
        onNodeWithTag("rate-details-dialog").assertExists()
        save(onNodeWithTag("rate-details-dialog"),"rates-stitch-detail.png")
        onNodeWithTag("rate-use-calculator").performClick()
        runOnIdle {
            assertEquals(DesktopDestination.CALCULATOR,workspace.state.value.destination)
            assertEquals(PriceBasisTab.K24,workspace.calculator.state.value.priceBasis)
            assertEquals(quotes.gold24.toString(),workspace.calculator.state.value.input(CalculatorField.SPOT))
            assertEquals("2.125",workspace.calculator.state.value.input(CalculatorField.GROSS_WEIGHT))
        }
    }

    @Test fun compactManualQuotesStayHonestAndManualRateDialogStillWorks() = runDesktopComposeUiTest(width=850,height=1000) {
        runBlocking { workspace.saveManualRates(quotes.copy(gold24=0,coinGerami=0))!!.join() }
        setContent { val state by workspace.state.collectAsState(); GoldExCompanionTheme(isDarkTheme=state.dark) { DesktopWorkspaceScreen(workspace,{},"0.56.45") } }
        onNodeWithTag("rate-row-GOLD24").performScrollTo()
        assertTrue(onAllNodes(hasText("—") and hasAnyAncestor(hasTestTag("rate-row-GOLD24")),useUnmergedTree=true).fetchSemanticsNodes().isNotEmpty())
        onNodeWithTag("rate-detail-GOLD24").performClick()
        onNodeWithTag("rate-use-calculator").assertIsNotEnabled()
        onNodeWithTag("rate-details-close").performClick()
        onNodeWithTag("rates-summary-GOLD18").performScrollTo()
        save(onNodeWithTag("workspace-root"),"rates-stitch-compact-light.png")
        onNodeWithTag("manual-rates").performScrollTo().performClick()
        onNodeWithTag("save-manual-rates").assertExists()
        onNodeWithText("انصراف").performClick()
        onNodeWithTag("rates-summary-GOLD18").performClick()
        onNodeWithText("تاریخچه معتبر و همسان در دسترس نیست.").assertExists()
        onNodeWithTag("rate-details-close").performClick()
        onNodeWithTag("workspace-theme").performClick()
        waitUntil(5000) { workspace.state.value.dark }
        save(onNodeWithTag("workspace-root"),"rates-stitch-compact-dark.png")
    }

    private fun save(node: SemanticsNodeInteraction,name: String) {
        val bitmap=node.captureToImage().asSkiaBitmap(); val image=org.jetbrains.skia.Image.makeFromBitmap(bitmap)
        val data=image.encodeToData(org.jetbrains.skia.EncodedImageFormat.PNG)!!
        File(System.getProperty("qirato.screenshotDir")).apply { mkdirs() }.resolve(name).writeBytes(data.bytes)
        data.close(); image.close(); bitmap.close()
    }
}
