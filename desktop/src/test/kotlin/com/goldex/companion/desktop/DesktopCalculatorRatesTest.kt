package com.goldex.companion.desktop

import com.goldex.companion.desktop.data.*
import com.goldex.companion.desktop.state.*
import com.goldex.companion.domain.calculator.GoldCalculationUseCases
import com.goldex.companion.model.PriceBasisTab
import com.goldex.companion.presentation.calculator.*
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import kotlin.test.*

class DesktopCalculatorRatesTest {
    @get:Rule val temporary = TemporaryFolder()
    private fun quote(spot: Long = 6_000_000) = MarketSnapshot(DesktopMarketRepository.emptyRates().copy(gold18 = spot, gold24 = 8_123_456, goldMelt = 26_123_456), 100_000, QuoteKind.ONLINE)

    @Test fun legacyDisabledPreferenceStillRefreshesStartupAndRetriesOfflineWithCacheAndDraftIntact() = runBlocking {
        val ticks = kotlinx.coroutines.channels.Channel<Unit>(kotlinx.coroutines.channels.Channel.UNLIMITED)
        val intervals = kotlinx.coroutines.channels.Channel<Long>(kotlinx.coroutines.channels.Channel.UNLIMITED)
        val calls = kotlinx.coroutines.channels.Channel<Unit>(kotlinx.coroutines.channels.Channel.UNLIMITED)
        var succeeds = false
        DesktopDataStore(temporary.newFolder().toPath()).use { store ->
            store.saveSettings(store.loadSettings().copy(autoSyncRates = false))
            store.saveMarket(quote().copy(kind = QuoteKind.CACHED))
            val market = DesktopMarketRepository(store, fetch = { calls.trySend(Unit); if (!succeeds) error("offline"); quote(7_000_000).rates })
            DesktopWorkspace(store, market, history = DesktopGoldHistoryGateway { error("offline") },
                waitForNextTick = { intervals.send(it); ticks.receive() }).use { workspace ->
                workspace.calculatorRates.setInput(CalculatorField.GROSS_WEIGHT, "2.125")
                workspace.editSettings { it.copy(galleryName = "پیش‌نویس") }
                workspace.start()
                kotlinx.coroutines.withTimeout(3000) { calls.receive(); intervals.receive() }
                assertEquals(100_000L, workspace.state.value.snapshot!!.observedAt)
                assertEquals("6000000", workspace.calculator.state.value.input(CalculatorField.SPOT))
                assertNull(workspace.state.value.error)
                succeeds = true; ticks.send(Unit)
                kotlinx.coroutines.withTimeout(3000) { calls.receive(); workspace.calculator.state.first { it.input(CalculatorField.SPOT) == "7000000" }; intervals.receive() }
                assertEquals("2.125", workspace.calculator.state.value.input(CalculatorField.GROSS_WEIGHT))
                assertEquals("پیش‌نویس", workspace.state.value.settingsDraft!!.galleryName)
                assertFalse(store.loadSettings().autoSyncRates)
                assertEquals(7_000_000L, store.cachedMarket()!!.rates.gold18)
            }
        }
        Unit
    }

    @Test fun loadsAllActualBasesAndConvertsOnlyMissingQuotesUsingDomainPolicy() {
        val calculator = ManualGoldCalculator()
        val binding = DesktopCalculatorRates(calculator, quote())
        assertEquals("6000000", calculator.state.value.input(CalculatorField.SPOT))
        binding.setPriceBasis(PriceBasisTab.K24)
        assertEquals("8123456", calculator.state.value.input(CalculatorField.SPOT))
        binding.setPriceBasis(PriceBasisTab.MESGHAL)
        assertEquals("26123456", calculator.state.value.input(CalculatorField.SPOT))
        binding.updateQuote(quote().copy(rates = quote().rates.copy(goldMelt = 0)))
        assertEquals(GoldCalculationUseCases.fromSpotPrice18k(6_000_000, PriceBasisTab.MESGHAL).toString(), calculator.state.value.input(CalculatorField.SPOT))
    }

    @Test fun incomingQuotesPreserveManualRateAndOtherInputsUntilUserReturnsToMarket() {
        val calculator = ManualGoldCalculator()
        val binding = DesktopCalculatorRates(calculator, quote())
        binding.setInput(CalculatorField.GROSS_WEIGHT, "2.125")
        binding.setInput(CalculatorField.SPOT, "7000000")
        val result = calculator.state.value.result
        binding.updateQuote(quote(9_000_000))
        assertEquals("7000000", calculator.state.value.input(CalculatorField.SPOT))
        assertEquals(result, calculator.state.value.result)
        assertFalse(binding.state.value.automatic)
        binding.useMarketRate()
        assertEquals("9000000", calculator.state.value.input(CalculatorField.SPOT))
        assertEquals("2.125", calculator.state.value.input(CalculatorField.GROSS_WEIGHT))
        assertTrue(binding.state.value.automatic)
        binding.reset()
        assertEquals("9000000", calculator.state.value.input(CalculatorField.SPOT))
        assertEquals("", calculator.state.value.input(CalculatorField.GROSS_WEIGHT))
    }

    @Test fun independentlyAvailableK24QuoteLoadsWithoutK18() {
        val calculator = ManualGoldCalculator()
        val binding = DesktopCalculatorRates(calculator, quote(0))
        binding.setPriceBasis(PriceBasisTab.K24)
        assertEquals("8123456", calculator.state.value.input(CalculatorField.SPOT))
        binding.setPriceBasis(PriceBasisTab.MESGHAL)
        assertEquals("26123456", calculator.state.value.input(CalculatorField.SPOT))
    }

    @Test fun freshApiQuoteSurvivesRestartAndOfflineFailureWithoutChangingItsTime() = runBlocking {
        val path = temporary.newFolder().toPath()
        DesktopDataStore(path).use { store ->
            DesktopMarketRepository(store, { quote().rates }, { 100_000 }).refreshRates()
        }
        DesktopDataStore(path).use { store ->
            val repository = DesktopMarketRepository(store, { error("offline fixture") }, { 900_000 })
            DesktopWorkspace(store, repository, history = DesktopGoldHistoryGateway { error("offline") }).use { workspace ->
                assertEquals("6000000", workspace.calculator.state.value.input(CalculatorField.SPOT))
                assertEquals(QuoteKind.CACHED, workspace.calculatorRates.state.value.quote!!.kind)
                assertFalse(workspace.calculatorRates.state.value.quote!!.rates.isLive)
                workspace.refreshRates().join()
                assertEquals(100_000, repository.snapshot.value!!.observedAt)
                assertEquals("6000000", workspace.calculator.state.value.input(CalculatorField.SPOT))
                assertNotNull(workspace.state.value.error)
            }
        }
        Unit
    }

    @Test fun missingQuoteNeverSeedsFakePriceAndFirstRefreshPopulatesCalculator() = runBlocking {
        val calculator = ManualGoldCalculator()
        val binding = DesktopCalculatorRates(calculator, null)
        binding.updateQuote(quote(0).copy(rates = DesktopMarketRepository.emptyRates()))
        assertEquals("", calculator.state.value.input(CalculatorField.SPOT))
        DesktopDataStore(temporary.newFolder().toPath()).use { store ->
            DesktopWorkspace(store, DesktopMarketRepository(store, { quote().rates }), history = DesktopGoldHistoryGateway { error("offline") }).use { workspace ->
                workspace.start()
                kotlinx.coroutines.withTimeout(3000) { workspace.calculator.state.first { it.input(CalculatorField.SPOT) == "6000000" } }
            }
        }
        Unit
    }
}
