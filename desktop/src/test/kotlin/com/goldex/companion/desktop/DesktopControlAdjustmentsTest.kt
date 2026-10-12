package com.goldex.companion.desktop

import androidx.compose.runtime.*
import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.test.*
import com.goldex.companion.data.*
import com.goldex.companion.desktop.data.*
import com.goldex.companion.desktop.state.*
import com.goldex.companion.desktop.ui.DesktopWorkspaceScreen
import com.goldex.companion.ui.theme.GoldExCompanionTheme
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import kotlin.test.*

@OptIn(ExperimentalTestApi::class)
class DesktopControlAdjustmentsTest {
    @get:Rule val temporary = TemporaryFolder()
    @get:Rule val timeout = org.junit.rules.Timeout.seconds(60)
    private lateinit var store: DesktopDataStore
    private lateinit var workspace: DesktopWorkspace
    @Before fun setup() {
        store = DesktopDataStore(temporary.newFolder().toPath())
        store.saveReduceMotion(true)
        val rates = DesktopMarketRepository.emptyRates().copy(gold18=6_000_000, gold24=8_000_000,
            goldMelt=26_000_000, coinEmami=55_000_000, usd=90_000, ons=4100.0)
        val market = DesktopMarketRepository(store, fetch={rates})
        runBlocking { market.refreshRates() }
        workspace = DesktopWorkspace(store, market, history=DesktopGoldHistoryGateway { error("offline fixture") },
            ratesBoardGateway=object : DesktopRatesBoardGateway {
                override fun cached()=BoardSnapshot(System.currentTimeMillis(),emptyMap(),emptyMap())
                override suspend fun load(previous: BoardSnapshot?)=cached()
            }, rateHistoryGateway=object : DesktopRateHistoryGateway {
                override suspend fun load(instrument: BoardInstrument,horizon: com.goldex.companion.model.TimeHorizon): RateHistorySnapshot = error("offline fixture")
            })
    }
    @After fun cleanup() { workspace.close(); store.close() }
    private fun content() = @Composable {
        val state by workspace.state.collectAsState()
        GoldExCompanionTheme(state.dark) { DesktopWorkspaceScreen(workspace,{},"test") }
    }
    @Test fun everyFooterInstrumentOpensItsOwnDetailAndHeaderBackReturnsToBoard()=runDesktopComposeUiTest(width=1440,height=1000) {
        setContent(content())
        for (instrument in listOf(BoardInstrument.GOLD18,BoardInstrument.MELT,BoardInstrument.GOLD24,
                BoardInstrument.EMAMI,BoardInstrument.USD,BoardInstrument.OUNCE)) {
            onNodeWithTag("footer-rate-${instrument.name}").performScrollTo().performClick()
            waitUntil(5000) { workspace.state.value.destination==DesktopDestination.RATE_DETAIL }
            runOnIdle { assertEquals(instrument,workspace.rateDetail.state.value.instrument) }
            onNodeWithTag("rate-details-close").assertIsDisplayed().performClick()
            runOnIdle { assertEquals(DesktopDestination.RATES,workspace.state.value.destination) }
        }
    }
    @Test fun wideSettingsAlignRateCardWithProfileAndRetainSourceChoiceInBothThemes()=runDesktopComposeUiTest(width=1440,height=1080) {
        workspace.navigate(DesktopDestination.SETTINGS)
        setContent(content())
        for (dark in listOf(false,true)) {
            if (dark) onNodeWithTag("workspace-theme").performClick()
            val profile=onNodeWithTag("settings-profile").fetchSemanticsNode().boundsInRoot
            val preferences=onNodeWithTag("settings-preferences").fetchSemanticsNode().boundsInRoot
            val rates=onNodeWithTag("settings-rate-sources").fetchSemanticsNode().boundsInRoot
            assertEquals(profile.top,preferences.top)
            assertEquals(profile.bottom,preferences.bottom)
            assertEquals(profile.bottom,rates.bottom)
            val address = onNode(hasSetTextAction() and hasAnyAncestor(hasTestTag("settings-address")))
            address.performTextReplacement((1..8).joinToString("\n") { "نشانی آزمایشی $it" })
            val expanded = onNodeWithTag("settings-profile").fetchSemanticsNode().boundsInRoot.height
            assertTrue(expanded > profile.height)
            address.performTextReplacement("نشانی کوتاه")
            val contracted = onNodeWithTag("settings-profile").fetchSemanticsNode().boundsInRoot
            assertTrue(contracted.height < expanded, "Clearing multiline content must release extra card height")
            assertEquals(contracted.bottom,onNodeWithTag("settings-rate-sources").fetchSemanticsNode().boundsInRoot.bottom)
            onNodeWithTag("price-source-TALA_IR").performClick()
            runOnIdle { assertEquals(PriceSource.TALA_IR,workspace.state.value.settingsDraft!!.priceSource) }
            save(onNodeWithTag("workspace-root"),"settings-aligned-${if(dark) "dark" else "light"}.png")
        }
    }
    @Test fun inventoryLocationDropdownPreservesFilterAndDashboardLedgerNavigation()=runDesktopComposeUiTest(width=940,height=700) {
        workspace.navigate(DesktopDestination.INVENTORY)
        setContent(content())
        onNodeWithTag("inventory-location-selector").performClick()
        onNodeWithTag("inventory-location-ویترین").performClick()
        onNodeWithTag("inventory-location-selector").assertTextContains("ویترین‌ها")
        onNodeWithTag("inventory-location-all").assertDoesNotExist()
        onNodeWithTag("inventory-location-selector").performClick()
        onNodeWithTag("inventory-location-all").performClick()
        onNodeWithTag("inventory-location-selector").assertTextContains("همه موقعیت‌ها (کل گالری)")
        save(onNodeWithTag("workspace-root"),"inventory-location-dropdown.png")
        onNodeWithTag("nav-DASHBOARD").performClick()
        onNodeWithText("اطلاعات این رایانه با گوشی به‌صورت خودکار همگام نمی‌شود.").assertDoesNotExist()
        onNodeWithTag("dashboard-invoice-ledger").performScrollTo().performClick()
        runOnIdle { assertEquals(DesktopDestination.INVOICES,workspace.state.value.destination) }
    }
    private fun save(node: SemanticsNodeInteraction,name: String) {
        val bitmap=node.captureToImage().asSkiaBitmap(); val image=org.jetbrains.skia.Image.makeFromBitmap(bitmap)
        val data=image.encodeToData(org.jetbrains.skia.EncodedImageFormat.PNG)!!
        File(System.getProperty("qirato.screenshotDir")).apply{mkdirs()}.resolve(name).writeBytes(data.bytes)
        data.close(); image.close(); bitmap.close()
    }
}
