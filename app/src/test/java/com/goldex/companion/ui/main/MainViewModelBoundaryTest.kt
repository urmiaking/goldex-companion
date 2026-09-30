package com.goldex.companion.ui.main

import androidx.lifecycle.ViewModelStore
import com.goldex.companion.data.*
import com.goldex.companion.model.*
import com.goldex.companion.ui.calculator.AppTab
import com.goldex.companion.ui.invoices.BarterInvoiceViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelBoundaryTest {
    private val dispatcher = StandardTestDispatcher()
    private val viewModels = ViewModelStore()
    @Before fun setup() { Dispatchers.setMain(dispatcher) }
    @After fun close() { viewModels.clear(); Dispatchers.resetMain() }

    @Test fun cachedOfflineStartupAndNavigationDoNotFetchOrLoseProviderSelection() {
        val settings = Settings()
        val market = Market()
        val connectivity = object : ConnectivityObserver { override val status = MutableStateFlow(ConnectionStatus.OFFLINE) }
        val vm = MainViewModel(settings, market, market, market, market, connectivity, dispatcher)
        viewModels.put("main", vm)
        dispatcher.scheduler.runCurrent()
        assertEquals(ConnectionStatus.OFFLINE, vm.uiState.value.connectionStatus)
        assertEquals(1_000_000L, vm.uiState.value.rates.gold18)
        assertEquals("1000000", vm.uiState.value.spotPriceInput)
        assertEquals(PriceSource.TGJU, market.currentSource.value)
        vm.selectTab(AppTab.MORE)
        assertEquals(AppTab.MORE, vm.uiState.value.selectedTab)
        vm.openRateDetail(MarketRateItemType.GOLD_18K)
        dispatcher.scheduler.runCurrent()
        assertTrue(vm.uiState.value.isRateDetailVisible)
        assertEquals(market.cachedHistory, vm.uiState.value.rateDetailHistory)
        assertFalse(vm.uiState.value.isHistoryLoading)
        assertEquals(0, market.fetches)
    }

    @Test fun persianInputEventsKeepFinancialResultsAndInvoiceStagingCompatible() {
        val market = Market()
        val vm = MainViewModel(Settings(), market, market, market, market,
            object : ConnectivityObserver { override val status = MutableStateFlow(ConnectionStatus.OFFLINE) }, dispatcher)
        viewModels.put("main", vm)
        vm.onGrossWeightChanged("۲٫۰۰۰")
        vm.onStoneWeightChanged("۰٫۵۰۰")
        vm.onWageChanged("۱۰")
        vm.onProfitPercentChanged("۷")
        vm.onTaxPercentChanged("۹")
        val result = requireNotNull(vm.uiState.value.jewelryResult)
        assertEquals(1.5, result.netWeight, 0.0)
        assertEquals(1_500_000.0, result.rawGoldValue, 0.0)
        assertEquals(23_895.0, result.taxAmount, 0.0)
        assertEquals(1_789_395.0, result.totalPayable, 0.0)
        vm.addItemToInvoice()
        val invoice = vm.buildCurrentInvoice()
        assertEquals(result.totalPayable, invoice.items.single().totalPayable, 0.0)
        vm.clearInvoice()
        assertTrue(vm.uiState.value.invoiceItems.isEmpty())
    }

    @Test fun barterInvoiceUsesInjectedMarketQuote() {
        val vm = BarterInvoiceViewModel(currentGold18 = { 3_000_000L })
        viewModels.put("barter", vm)
        assertEquals(3_000_000L, vm.uiState.value.invoice.spotPrice18k)
        vm.openNewInvoice(customSpotPrice = 4_000_000L)
        assertEquals(4_000_000L, vm.uiState.value.invoice.spotPrice18k)
    }

    private class Settings : SettingsStore {
        override val settings = MutableStateFlow(AppSettings(autoSyncRates = false, priceSource = PriceSource.TGJU))
        override fun loadSettings() = settings.value
        override fun saveSettings(newSettings: AppSettings) { settings.value = newSettings }
    }
    private class Market : MarketRatesStore, MarketHistoryStore, MarketCacheReader, MarketSourceInitializer {
        override val rates = MutableStateFlow(MarketRates(gold18 = 1_000_000L, source = PriceSource.TGJU, isLive = false))
        override val currentSource = MutableStateFlow(PriceSource.ISIGNAL)
        val cachedHistory = mapOf(TimeHorizon.TODAY to listOf(MarketCandle(1, 2, 1, 2, "1405/07/08")))
        var fetches = 0
        override fun setSourceSilently(source: PriceSource) { currentSource.value = source }
        override suspend fun setSource(source: PriceSource) { fetches++; currentSource.value = source }
        override suspend fun cycleSource(): PriceSource { fetches++; return currentSource.value }
        override suspend fun refreshRates(): MarketRates { fetches++; return rates.value }
        override suspend fun getHistory(type: MarketRateItemType, horizon: TimeHorizon, preferredSource: PriceSource): List<MarketCandle> { fetches++; return emptyList() }
        override suspend fun getAllHorizonsHistory(type: MarketRateItemType, preferredSource: PriceSource): Map<TimeHorizon, List<MarketCandle>> { fetches++; return emptyMap() }
        override fun getCachedRates() = rates.value
        override fun getCachedAllHorizonsHistory(type: MarketRateItemType) = cachedHistory
        override fun getCachedTodayCandlesForBoard() = emptyMap<MarketRateItemType, List<MarketCandle>>()
        override fun getCachedDashboardGold18Charts() = emptyMap<TimeHorizon, TrendChartData>()
    }
}
