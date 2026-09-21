package com.goldex.companion.ui.main

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.goldex.companion.data.ConnectionStatus
import com.goldex.companion.data.GoldMarketRepository
import com.goldex.companion.data.MarketRates
import com.goldex.companion.data.MarketRatesStore
import com.goldex.companion.data.NetworkMonitor
import com.goldex.companion.data.PriceSource
import com.goldex.companion.data.SettingsRepository
import com.goldex.companion.data.SettingsStore
import com.goldex.companion.domain.calculator.GoldCalculationUseCases
import com.goldex.companion.model.*
import com.goldex.companion.model.MarketRateItemType
import com.goldex.companion.model.PriceBasisTab
import com.goldex.companion.ui.calculator.*
import com.goldex.companion.ui.calculator.screens.MeltUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class MainUiState(
    val selectedTab: AppTab = AppTab.HOME,
    val rates: MarketRates = MarketRates(),
    val isRefreshingRates: Boolean = false,
    val isDarkTheme: Boolean = false,
    val connectionStatus: ConnectionStatus = ConnectionStatus.ONLINE,
    val autoSyncPrice: Boolean = true,

    // Jewelry Calculator State
    val itemTitleInput: String = "",
    val priceBasisTab: PriceBasisTab = PriceBasisTab.K18,
    val isManualSpotDialogVisible: Boolean = false,
    val isStoneWeightDialogVisible: Boolean = false,
    val grossWeightInput: String = "",
    val stoneWeightInput: String = "",
    val selectedKarat: Karat = Karat.K18,
    val karatInput: String = "750",
    val spotPriceInput: String = "",
    val wageType: WageType = WageType.PERCENTAGE,
    val wageInput: String = "10",
    val profitPercentInput: String = "7",
    val taxPercentInput: String = "9",
    val jewelryResult: DetailedJewelryResult? = null,
    val priceInWords: String = "",

    // Active Invoice Staging State
    val invoiceItems: List<InvoiceItem> = emptyList(),
    val selectedCustomer: Customer? = null,

    // Melt Calculator State
    val mesghalPriceInput: String = "",
    val meltWeightInput: String = "",
    val meltGram18kPrice: Long = 0L,
    val meltTotalValue: Double = 0.0,

    // Coin Bubble State
    val selectedCoin: CoinType = CoinType.EMAMI,
    val coinMarketPriceInput: String = "",
    val coinBubbleResult: CoinBubbleResult? = null,

    // Sub-Screen Navigation State
    val isStandardFormulasVisible: Boolean = false,
    val isKaratConvertVisible: Boolean = false,
    val isCoinBubbleVisible: Boolean = false,
    val isMeltVisible: Boolean = false,
    val isRateDetailVisible: Boolean = false,
    val selectedRateDetailType: MarketRateItemType = MarketRateItemType.GOLD_18K,
    val rateDetailHistory: Map<TimeHorizon, List<MarketCandle>> = emptyMap(),
    val isHistoryLoading: Boolean = false,
    val isWizardVisible: Boolean = false,
    val dashboardGold18Charts: Map<TimeHorizon, TrendChartData> = emptyMap(),
    val todayCandlesByType: Map<MarketRateItemType, List<MarketCandle>> = emptyMap()
) {
    fun toJewelryUiState(): JewelryUiState = JewelryUiState(
        itemTitleInput = itemTitleInput,
        priceBasisTab = priceBasisTab,
        isManualSpotDialogVisible = isManualSpotDialogVisible,
        isStoneWeightDialogVisible = isStoneWeightDialogVisible,
        grossWeightInput = grossWeightInput,
        stoneWeightInput = stoneWeightInput,
        selectedKarat = selectedKarat,
        karatInput = karatInput,
        spotPriceInput = spotPriceInput,
        wageType = wageType,
        wageInput = wageInput,
        profitPercentInput = profitPercentInput,
        taxPercentInput = taxPercentInput,
        jewelryResult = jewelryResult,
        priceInWords = priceInWords,
        rates = rates
    )

    fun toMeltUiState(): MeltUiState = MeltUiState(
        meltWeightInput = meltWeightInput,
        mesghalPriceInput = mesghalPriceInput,
        meltGram18kPrice = meltGram18kPrice,
        meltTotalValue = meltTotalValue,
        rates = rates
    )
}

typealias CalculatorUiState = MainUiState

class MainViewModel(application: Application) : AndroidViewModel(application), JewelryActions {

    private val settingsRepository: SettingsStore = SettingsRepository(application.applicationContext)
    private val themePreference = ThemePreference(settingsRepository)
    private val marketRatesRepository: MarketRatesStore = GoldMarketRepository
    private val networkMonitor = NetworkMonitor(application.applicationContext)

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private var autoRefreshJob: Job? = null

    init {
        GoldMarketRepository.init(application.applicationContext)
        loadInitialState()
        observeNetwork()
    }

    private fun loadInitialState() {
        val s = settingsRepository.loadSettings()
        val cachedRates = GoldMarketRepository.getCachedRates()
        val initialRates = cachedRates ?: MarketRates()
        val cachedTodayCandles = GoldMarketRepository.getCachedTodayCandlesForBoard()
        val cachedDashboardCharts = GoldMarketRepository.getCachedDashboardGold18Charts()

        _uiState.update {
            it.copy(
                isDarkTheme = themePreference.load(),
                profitPercentInput = s.defaultProfitPercent,
                taxPercentInput = s.defaultTaxPercent,
                wageType = s.defaultWageType,
                wageInput = if (s.defaultWageType == WageType.PERCENTAGE) "10" else "",
                karatInput = "750",
                selectedKarat = Karat.K18,
                autoSyncPrice = s.autoSyncRates,
                rates = initialRates,
                spotPriceInput = if (initialRates.gold18 > 0L) initialRates.gold18.toString() else "",
                mesghalPriceInput = if (initialRates.goldMelt > 0L) initialRates.goldMelt.toString() else "",
                coinMarketPriceInput = if (initialRates.coinEmami > 0L) initialRates.coinEmami.toString() else "",
                todayCandlesByType = cachedTodayCandles,
                dashboardGold18Charts = cachedDashboardCharts
            )
        }
        GoldMarketRepository.setSourceSilently(s.priceSource)
        calculateAll()

        if (s.autoSyncRates || cachedRates == null) {
            if (s.autoSyncRates) {
                startAutoRatesRefresh()
            }
            refreshRatesSilently()
        }
    }

    fun applySettingsDefaults(profitPercent: String, taxPercent: String, wageType: WageType) {
        _uiState.update {
            it.copy(
                profitPercentInput = profitPercent,
                taxPercentInput = taxPercent,
                wageType = wageType
            )
        }
        calculateJewelry()
    }

    fun updatePriceSource(source: PriceSource, autoSync: Boolean) {
        _uiState.update { it.copy(autoSyncPrice = autoSync) }
        viewModelScope.launch {
            val s = settingsRepository.loadSettings()
            settingsRepository.saveSettings(s.copy(priceSource = source, autoSyncRates = autoSync))
            marketRatesRepository.setSource(source)
            if (autoSync) {
                startAutoRatesRefresh()
                refreshRates()
            } else {
                autoRefreshJob?.cancel()
            }
        }
    }

    // --- Sub-Screen Navigation Visibility ---
    fun setStandardFormulasVisible(visible: Boolean) {
        _uiState.update { it.copy(isStandardFormulasVisible = visible) }
    }

    fun setKaratConvertVisible(visible: Boolean) {
        _uiState.update { it.copy(isKaratConvertVisible = visible) }
    }

    fun setCoinBubbleVisible(visible: Boolean) {
        _uiState.update { it.copy(isCoinBubbleVisible = visible) }
    }

    fun setMeltVisible(visible: Boolean) {
        _uiState.update { it.copy(isMeltVisible = visible) }
    }

    private var historyLoadingJob: Job? = null

    fun openRateDetail(type: MarketRateItemType) {
        val cached = GoldMarketRepository.getCachedAllHorizonsHistory(type)
        val hasData = cached.values.any { it.isNotEmpty() }
        _uiState.update {
            it.copy(
                selectedRateDetailType = type,
                isRateDetailVisible = true,
                rateDetailHistory = cached,
                isHistoryLoading = !hasData
            )
        }
        if (_uiState.value.autoSyncPrice || !hasData) {
            loadRateHistory(type)
        }
    }

    fun loadRateHistory(type: MarketRateItemType) {
        historyLoadingJob?.cancel()
        historyLoadingJob = viewModelScope.launch(Dispatchers.IO) {
            val preferred = _uiState.value.rates.source
            val history = GoldMarketRepository.getAllHorizonsHistory(type, preferred)
            _uiState.update {
                it.copy(
                    rateDetailHistory = history,
                    isHistoryLoading = false
                )
            }
        }
    }

    private var dashboardHistoryJob: Job? = null
    private var boardCandlesJob: Job? = null

    fun loadDashboardGold18History() {
        dashboardHistoryJob?.cancel()
        dashboardHistoryJob = viewModelScope.launch(Dispatchers.IO) {
            val preferred = _uiState.value.rates.source
            val history = GoldMarketRepository.getAllHorizonsHistory(MarketRateItemType.GOLD_18K, preferred)
            val charts = history.mapValues { (horizon, candles) ->
                MarketHistoryConverter.toTrendChartData(candles, horizon, _uiState.value.rates.gold18)
            }
            _uiState.update { it.copy(dashboardGold18Charts = charts) }
        }
    }

    fun loadTodayCandlesForBoard() {
        boardCandlesJob?.cancel()
        boardCandlesJob = viewModelScope.launch(Dispatchers.IO) {
            val preferred = _uiState.value.rates.source
            val types = MarketRateItemType.values()
            val candlesMap = mutableMapOf<MarketRateItemType, List<MarketCandle>>()
            for (type in types) {
                val candles = GoldMarketRepository.getHistory(type, TimeHorizon.TODAY, preferred)
                if (candles.isNotEmpty()) {
                    candlesMap[type] = candles
                }
            }
            if (candlesMap.isNotEmpty()) {
                _uiState.update { it.copy(todayCandlesByType = candlesMap) }
            }
        }
    }

    fun setRateDetailVisible(visible: Boolean) {
        if (!visible) {
            historyLoadingJob?.cancel()
        }
        _uiState.update { it.copy(isRateDetailVisible = visible) }
    }

    fun setWizardVisible(visible: Boolean) {
        _uiState.update { it.copy(isWizardVisible = visible) }
    }

    // --- Tab Selection & App Theme ---
    override fun selectTab(tab: AppTab) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    fun toggleTheme() {
        _uiState.update { current ->
            current.copy(isDarkTheme = themePreference.toggle(current.isDarkTheme))
        }
    }

    // --- Customer Bridge (for active invoice) ---
    fun setSelectedCustomer(customer: Customer?) {
        _uiState.update { it.copy(selectedCustomer = customer) }
    }

    fun selectCustomer(customer: Customer?) = setSelectedCustomer(customer)

    // --- Invoice Staging Actions ---
    override fun addItemToInvoice() {
        val state = _uiState.value
        val res = state.jewelryResult ?: return
        val customKarat = PersianNumberFormatter.parseToCleanLong(state.karatInput)?.toInt() ?: 750
        val item = InvoiceItem(
            title = state.itemTitleInput.ifBlank { "قطعه طلا " },
            karat = state.selectedKarat,
            customKaratValue = customKarat,
            grossWeight = res.grossWeight,
            stoneWeight = res.stoneWeight,
            netWeight = res.netWeight,
            spotPrice = GoldCalculationUseCases.toSpotPrice18k(
                PersianNumberFormatter.parseToCleanLong(state.spotPriceInput) ?: 0L,
                state.priceBasisTab
            ),
            wageType = state.wageType,
            wageInput = PersianNumberFormatter.parsePersianOrEnglish(state.wageInput) ?: 0.0,
            wageAmount = res.wageAmount,
            profitPercent = PersianNumberFormatter.parsePersianOrEnglish(state.profitPercentInput) ?: 0.0,
            profitAmount = res.profitAmount,
            taxPercent = PersianNumberFormatter.parsePersianOrEnglish(state.taxPercentInput) ?: 0.0,
            taxAmount = res.taxAmount,
            rawGoldValue = res.rawGoldValue,
            totalPayable = res.totalPayable,
            effectiveGramPrice = res.effectiveGramPrice
        )
        val currentItems = state.invoiceItems.toMutableList()
        currentItems.add(item)
        _uiState.update { it.copy(invoiceItems = currentItems) }
    }

    fun removeItemFromInvoice(itemId: String) {
        val current = _uiState.value.invoiceItems.filter { it.id != itemId }
        _uiState.update { it.copy(invoiceItems = current) }
    }

    fun clearInvoice() {
        _uiState.update { it.copy(invoiceItems = emptyList()) }
    }

    fun buildCurrentInvoice(): Invoice {
        val state = _uiState.value
        val items = if (state.invoiceItems.isNotEmpty()) {
            state.invoiceItems
        } else if (state.jewelryResult != null) {
            val res = state.jewelryResult
            val customKarat = PersianNumberFormatter.parseToCleanLong(state.karatInput)?.toInt() ?: 750
            listOf(
                InvoiceItem(
                    title = state.itemTitleInput.ifBlank { "قطعه طلا" },
                    karat = state.selectedKarat,
                    customKaratValue = customKarat,
                    grossWeight = res.grossWeight,
                    stoneWeight = res.stoneWeight,
                    netWeight = res.netWeight,
                    spotPrice = GoldCalculationUseCases.toSpotPrice18k(
                        PersianNumberFormatter.parseToCleanLong(state.spotPriceInput) ?: 0L,
                        state.priceBasisTab
                    ),
                    wageType = state.wageType,
                    wageInput = PersianNumberFormatter.parsePersianOrEnglish(state.wageInput) ?: 0.0,
                    wageAmount = res.wageAmount,
                    profitPercent = PersianNumberFormatter.parsePersianOrEnglish(state.profitPercentInput) ?: 0.0,
                    profitAmount = res.profitAmount,
                    taxPercent = PersianNumberFormatter.parsePersianOrEnglish(state.taxPercentInput) ?: 0.0,
                    taxAmount = res.taxAmount,
                    rawGoldValue = res.rawGoldValue,
                    totalPayable = res.totalPayable,
                    effectiveGramPrice = res.effectiveGramPrice
                )
            )
        } else {
            emptyList()
        }
        return Invoice(
            customer = state.selectedCustomer,
            items = items
        )
    }

    // --- Network & Live Rates ---
    private fun observeNetwork() {
        viewModelScope.launch {
            networkMonitor.status.collect { status ->
                _uiState.update { it.copy(connectionStatus = status) }
                if (status == ConnectionStatus.ONLINE && _uiState.value.autoSyncPrice) {
                    refreshRatesSilently()
                }
            }
        }
    }

    private fun startAutoRatesRefresh() {
        autoRefreshJob?.cancel()
        autoRefreshJob = viewModelScope.launch(Dispatchers.IO) {
            while (isActive) {
                delay(60_000L)
                if (_uiState.value.autoSyncPrice && _uiState.value.connectionStatus == ConnectionStatus.ONLINE) {
                    refreshRatesSilently()
                }
            }
        }
    }

    fun refreshRatesSilently() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val newRates = marketRatesRepository.refreshRates()
                applyFetchedRates(newRates)
                loadDashboardGold18History()
                loadTodayCandlesForBoard()
            } catch (_: Exception) { }
        }
    }

    fun refreshRates() {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isRefreshingRates = true) }
            try {
                val newRates = marketRatesRepository.refreshRates()
                applyFetchedRates(newRates)
                loadDashboardGold18History()
                loadTodayCandlesForBoard()
            } catch (_: Exception) { } finally {
                _uiState.update { it.copy(isRefreshingRates = false) }
            }
        }
    }

    fun togglePriceSource() {
        viewModelScope.launch {
            marketRatesRepository.cycleSource()
            refreshRates()
        }
    }

    private fun applyFetchedRates(newRates: MarketRates) {
        _uiState.update { current ->
            val updatedSpot = if (current.autoSyncPrice) {
                when (current.priceBasisTab) {
                    PriceBasisTab.K18 -> if (newRates.gold18 > 0L) newRates.gold18.toString() else current.spotPriceInput
                    PriceBasisTab.K24 -> if (newRates.gold24 > 0L) newRates.gold24.toString() else GoldCalculationUseCases.fromSpotPrice18k(newRates.gold18, PriceBasisTab.K24).toString()
                    PriceBasisTab.MESGHAL -> if (newRates.goldMelt > 0L) newRates.goldMelt.toString() else GoldCalculationUseCases.fromSpotPrice18k(newRates.gold18, PriceBasisTab.MESGHAL).toString()
                }
            } else {
                current.spotPriceInput
            }
            val updatedMesghal = if (current.autoSyncPrice && newRates.goldMelt > 0L) {
                newRates.goldMelt.toString()
            } else {
                current.mesghalPriceInput
            }
            val updatedCoinPrice = if (current.autoSyncPrice) {
                when (current.selectedCoin) {
                    CoinType.EMAMI -> if (newRates.coinEmami > 0L) newRates.coinEmami.toString() else current.coinMarketPriceInput
                    CoinType.BAHAR -> if (newRates.coinBahar > 0L) newRates.coinBahar.toString() else current.coinMarketPriceInput
                    CoinType.HALF -> if (newRates.coinHalf > 0L) newRates.coinHalf.toString() else current.coinMarketPriceInput
                    CoinType.QUARTER -> if (newRates.coinQuarter > 0L) newRates.coinQuarter.toString() else current.coinMarketPriceInput
                    CoinType.GERAMI -> if (newRates.coinGerami > 0L) newRates.coinGerami.toString() else current.coinMarketPriceInput
                }
            } else {
                current.coinMarketPriceInput
            }
            current.copy(
                rates = newRates,
                spotPriceInput = updatedSpot,
                mesghalPriceInput = updatedMesghal,
                coinMarketPriceInput = updatedCoinPrice
            )
        }
        calculateAll()
    }

    fun toggleAutoSyncPrice(enabled: Boolean) {
        _uiState.update { it.copy(autoSyncPrice = enabled) }
        val s = settingsRepository.loadSettings()
        settingsRepository.saveSettings(s.copy(autoSyncRates = enabled))
        if (enabled) {
            startAutoRatesRefresh()
            refreshRatesSilently()
        } else {
            autoRefreshJob?.cancel()
        }
    }

    // --- Jewelry Actions Implementation ---
    private fun sanitizeDecimal(input: String): String {
        val clean = PersianNumberFormatter.toEnglishDigits(input).filter { it.isDigit() || it == '.' }
        val parts = clean.split(".")
        return if (parts.size > 2) "${parts[0]}.${parts.subList(1, parts.size).joinToString("")}" else clean
    }

    fun onItemTitleChanged(newTitle: String) {
        _uiState.update { it.copy(itemTitleInput = newTitle) }
    }

    override fun onGrossWeightChanged(newWeight: String) {
        val clean = sanitizeDecimal(newWeight)
        _uiState.update { it.copy(grossWeightInput = clean) }
        calculateJewelry()
    }

    override fun onStoneWeightChanged(newStone: String) {
        val clean = sanitizeDecimal(newStone)
        _uiState.update { it.copy(stoneWeightInput = clean) }
        calculateJewelry()
    }

    override fun onKaratSelected(karat: Karat) {
        val karatVal = when (karat) {
            Karat.K18 -> "750"
            Karat.K21 -> "875"
            Karat.K24 -> "999"
        }
        _uiState.update { it.copy(selectedKarat = karat, karatInput = karatVal) }
        calculateJewelry()
    }

    override fun onKaratInputChanged(newKarat: String) {
        val clean = PersianNumberFormatter.toEnglishDigits(newKarat).filter { it.isDigit() }.take(4)
        val karatEnum = when (clean) {
            "750" -> Karat.K18
            "875" -> Karat.K21
            "999", "1000" -> Karat.K24
            else -> _uiState.value.selectedKarat
        }
        _uiState.update { it.copy(karatInput = clean, selectedKarat = karatEnum) }
        calculateJewelry()
    }

    override fun onSpotPriceChanged(newPrice: String) {
        val clean = PersianNumberFormatter.toEnglishDigits(newPrice).filter { it.isDigit() }
        _uiState.update { it.copy(spotPriceInput = clean) }
        calculateJewelry()
    }

    override fun applyPresetSpotPrice(price: Long) {
        _uiState.update { it.copy(spotPriceInput = price.toString()) }
        calculateJewelry()
    }

    override fun onWageTypeChanged(type: WageType) {
        _uiState.update { current ->
            val updatedWage = if (type == WageType.TOMAN_PER_GRAM) {
                ""
            } else {
                if (current.wageInput.isBlank()) "10" else current.wageInput
            }
            current.copy(wageType = type, wageInput = updatedWage)
        }
        calculateJewelry()
    }

    override fun onWageChanged(newWage: String) {
        val clean = if (_uiState.value.wageType == WageType.PERCENTAGE) {
            sanitizeDecimal(newWage)
        } else {
            PersianNumberFormatter.toEnglishDigits(newWage).filter { it.isDigit() }
        }
        _uiState.update { it.copy(wageInput = clean) }
        calculateJewelry()
    }

    override fun onProfitPercentChanged(newProfit: String) {
        val clean = sanitizeDecimal(newProfit)
        _uiState.update { it.copy(profitPercentInput = clean) }
        calculateJewelry()
    }

    override fun applyPresetProfit(preset: Double) {
        _uiState.update { it.copy(profitPercentInput = preset.toString()) }
        calculateJewelry()
    }

    override fun onTaxPercentChanged(newTax: String) {
        val clean = sanitizeDecimal(newTax)
        _uiState.update { it.copy(taxPercentInput = clean) }
        calculateJewelry()
    }

    override fun applyPresetTax(preset: Double) {
        _uiState.update { it.copy(taxPercentInput = preset.toString()) }
        calculateJewelry()
    }

    fun addGrossWeight(amount: Double) {
        val current = PersianNumberFormatter.parsePersianOrEnglish(_uiState.value.grossWeightInput) ?: 0.0
        val next = current + amount
        val formatted = if (next % 1.0 == 0.0) {
            next.toLong().toString()
        } else {
            String.format(java.util.Locale.US, "%.3f", next).trimEnd('0').trimEnd('.')
        }
        _uiState.update { it.copy(grossWeightInput = formatted) }
        calculateJewelry()
    }

    override fun resetJewelry() {
        val s = settingsRepository.loadSettings()
        _uiState.update {
            it.copy(
                itemTitleInput = "",
                grossWeightInput = "",
                stoneWeightInput = "",
                selectedKarat = Karat.K18,
                karatInput = "750",
                wageType = WageType.PERCENTAGE,
                wageInput = "10",
                profitPercentInput = s.defaultProfitPercent,
                taxPercentInput = s.defaultTaxPercent
            )
        }
        calculateJewelry()
    }

    override fun setPriceBasisTab(tab: PriceBasisTab) {
        _uiState.update { current ->
            val updatedSpot = if (current.autoSyncPrice) {
                when (tab) {
                    PriceBasisTab.K18 -> if (current.rates.gold18 > 0L) current.rates.gold18.toString() else current.spotPriceInput
                    PriceBasisTab.K24 -> if (current.rates.gold24 > 0L) current.rates.gold24.toString() else GoldCalculationUseCases.fromSpotPrice18k(current.rates.gold18, PriceBasisTab.K24).toString()
                    PriceBasisTab.MESGHAL -> if (current.rates.goldMelt > 0L) current.rates.goldMelt.toString() else GoldCalculationUseCases.fromSpotPrice18k(current.rates.gold18, PriceBasisTab.MESGHAL).toString()
                }
            } else {
                val currentSpot = PersianNumberFormatter.parseToCleanLong(current.spotPriceInput) ?: 0L
                val spot18k = GoldCalculationUseCases.toSpotPrice18k(currentSpot, current.priceBasisTab)
                GoldCalculationUseCases.fromSpotPrice18k(spot18k, tab).toString()
            }
            current.copy(priceBasisTab = tab, spotPriceInput = updatedSpot)
        }
        calculateJewelry()
    }

    override fun incrementWage() {
        val current = PersianNumberFormatter.parsePersianOrEnglish(_uiState.value.wageInput) ?: 0.0
        val next = current + 1.0
        val formatted = if (next % 1.0 == 0.0) next.toLong().toString() else next.toString()
        _uiState.update { it.copy(wageInput = formatted) }
        calculateJewelry()
    }

    override fun decrementWage() {
        val current = PersianNumberFormatter.parsePersianOrEnglish(_uiState.value.wageInput) ?: 0.0
        val next = (current - 1.0).coerceAtLeast(0.0)
        val formatted = if (next % 1.0 == 0.0) next.toLong().toString() else next.toString()
        _uiState.update { it.copy(wageInput = formatted) }
        calculateJewelry()
    }

    override fun setManualSpotDialogVisible(visible: Boolean) {
        _uiState.update { it.copy(isManualSpotDialogVisible = visible) }
    }

    override fun setStoneWeightDialogVisible(visible: Boolean) {
        _uiState.update { it.copy(isStoneWeightDialogVisible = visible) }
    }

    // --- Melt Tab Actions ---
    fun onMesghalPriceChanged(newMesghal: String) {
        val clean = PersianNumberFormatter.toEnglishDigits(newMesghal).filter { it.isDigit() }
        _uiState.update { it.copy(mesghalPriceInput = clean) }
        calculateMelt()
    }

    fun onMeltWeightChanged(newWeight: String) {
        val clean = sanitizeDecimal(newWeight)
        _uiState.update { it.copy(meltWeightInput = clean) }
        calculateMelt()
    }

    // --- Coin Bubble Actions ---
    fun onCoinTypeSelected(coin: CoinType) {
        val marketPrice = when (coin) {
            CoinType.EMAMI -> _uiState.value.rates.coinEmami
            CoinType.BAHAR -> _uiState.value.rates.coinBahar
            CoinType.HALF -> _uiState.value.rates.coinHalf
            CoinType.QUARTER -> _uiState.value.rates.coinQuarter
            CoinType.GERAMI -> _uiState.value.rates.coinGerami
        }
        _uiState.update {
            it.copy(
                selectedCoin = coin,
                coinMarketPriceInput = if (marketPrice > 0L) marketPrice.toString() else it.coinMarketPriceInput
            )
        }
        calculateCoin()
    }

    fun onCoinMarketPriceChanged(newPrice: String) {
        val clean = PersianNumberFormatter.toEnglishDigits(newPrice).filter { it.isDigit() }
        _uiState.update { it.copy(coinMarketPriceInput = clean) }
        calculateCoin()
    }

    // --- Calculations ---
    private fun calculateAll() {
        calculateJewelry()
        calculateMelt()
        calculateCoin()
    }

    private fun calculateJewelry() {
        val state = _uiState.value
        val gross = PersianNumberFormatter.parsePersianOrEnglish(state.grossWeightInput) ?: 0.0
        val stone = PersianNumberFormatter.parsePersianOrEnglish(state.stoneWeightInput) ?: 0.0
        val rawSpot = PersianNumberFormatter.parseToCleanLong(state.spotPriceInput) ?: 0L
        val spot18k = GoldCalculationUseCases.toSpotPrice18k(rawSpot, state.priceBasisTab)
        val wage = PersianNumberFormatter.parsePersianOrEnglish(state.wageInput) ?: 0.0
        val profit = PersianNumberFormatter.parsePersianOrEnglish(state.profitPercentInput) ?: 0.0
        val tax = PersianNumberFormatter.parsePersianOrEnglish(state.taxPercentInput) ?: 0.0
        val customKarat = PersianNumberFormatter.parseToCleanLong(state.karatInput)?.toInt() ?: 750

        val result = GoldCalculationUseCases.calculateJewelry(
            grossWeight = gross,
            stoneWeight = stone,
            karat = state.selectedKarat,
            customKaratValue = customKarat,
            spotPrice18k = spot18k,
            wageType = state.wageType,
            wageInput = wage,
            profitPercent = profit,
            taxPercent = tax
        )

        val words = if (result != null) PersianWordsFormatter.toWords(result.totalPayable.toLong()) else ""
        _uiState.update {
            it.copy(
                jewelryResult = result,
                priceInWords = if (words.isNotBlank()) words else ""
            )
        }
    }

    private fun calculateMelt() {
        val state = _uiState.value
        val mesghal = PersianNumberFormatter.parsePersianOrEnglish(state.mesghalPriceInput) ?: 0.0
        val weight = PersianNumberFormatter.parsePersianOrEnglish(state.meltWeightInput) ?: 0.0

        val result = GoldCalculationUseCases.calculateMelt(
            mesghalPrice = mesghal,
            weight = weight
        )

        _uiState.update {
            it.copy(
                meltGram18kPrice = result.gram18kPrice,
                meltTotalValue = result.totalValue
            )
        }
    }

    private fun calculateCoin() {
        val state = _uiState.value
        val marketPrice = PersianNumberFormatter.parsePersianOrEnglish(state.coinMarketPriceInput) ?: 0.0

        val result = GoldCalculationUseCases.calculateCoinBubble(
            coin = state.selectedCoin,
            marketPrice = marketPrice,
            usd = state.rates.usd,
            ounce = state.rates.ons
        )

        _uiState.update { it.copy(coinBubbleResult = result) }
    }
}
