package com.goldex.companion.ui.calculator

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.goldex.companion.data.AppSettings
import com.goldex.companion.data.AppUpdateChecker
import com.goldex.companion.data.ConnectionStatus
import com.goldex.companion.data.CustomerRepository
import com.goldex.companion.data.GoldMarketRepository
import com.goldex.companion.data.InvoiceRepository
import com.goldex.companion.data.MarketRates
import com.goldex.companion.data.NetworkMonitor
import com.goldex.companion.data.PortfolioItem
import com.goldex.companion.data.PortfolioRepository
import com.goldex.companion.data.PriceSource
import com.goldex.companion.data.SettingsRepository
import com.goldex.companion.data.UpdateInfo
import com.goldex.companion.model.*
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class AppTab(val titleFa: String) {
    HOME("خانه"),
    RATES("تابلوی مظنه"),
    CALCULATOR("ماشین‌حساب"),
    INVOICES("فاکتورها"),
    MORE("بیشتر")
}

data class CalculatorUiState(
    val selectedTab: AppTab = AppTab.HOME,
    val rates: MarketRates = MarketRates(),
    val isRefreshingRates: Boolean = false,
    val autoSyncPrice: Boolean = true,
    val isDarkTheme: Boolean = false,

    // Jewelry Tab State
    val itemTitleInput: String = "قطعه طلا ۱",
    val grossWeightInput: String = "10",
    val stoneWeightInput: String = "0",
    val selectedKarat: Karat = Karat.K18,
    val spotPriceInput: String = "23360000",
    val wageType: WageType = WageType.PERCENTAGE,
    val wageInput: String = "12",
    val profitPercentInput: String = "7",
    val taxPercentInput: String = "9",
    val jewelryResult: DetailedJewelryResult? = null,
    val priceInWords: String = "",

    // Multi-Item Invoice & Customer State
    val invoiceItems: List<InvoiceItem> = emptyList(),
    val selectedCustomer: Customer? = null,
    val customerList: List<Customer> = emptyList(),
    val isCustomerPickerVisible: Boolean = false,
    val isAddCustomerDialogVisible: Boolean = false,
    val isCustomerManagerVisible: Boolean = false,

    // Portfolio Items State
    val portfolioItems: List<PortfolioItem> = emptyList(),

    // Melt Tab State
    val mesghalPriceInput: String = "101500000",
    val meltWeightInput: String = "10",
    val meltGram18kPrice: Long = 23431000L,
    val meltTotalValue: Double = 0.0,

    // Coin Bubble State
    val selectedCoin: CoinType = CoinType.EMAMI,
    val coinMarketPriceInput: String = "234000000",
    val coinBubbleResult: CoinBubbleResult? = null,

    // Karat Convert State
    val convertWeightInput: String = "10",
    val convertFromKarat: Karat = Karat.K18,
    val convertToKarat: Karat = Karat.K24,
    val convertedWeight: Double = 7.5,

    // In-App Auto-Updater State
    val updateInfo: UpdateInfo? = null,
    val isCheckingForUpdate: Boolean = false,
    val isUpdateDialogDismissed: Boolean = false,

    // Real-Time Internet Connection & Network Status (Green / Yellow / Red)
    val connectionStatus: ConnectionStatus = ConnectionStatus.ONLINE,

    // Settings & Gallery Branding
    val appSettings: AppSettings = AppSettings(),
    val isSettingsDialogVisible: Boolean = false,
    val isTaxProfitModalVisible: Boolean = false,
    val isPriceSourceModalVisible: Boolean = false,

    // Invoice Persistence & Management
    val savedInvoices: List<Invoice> = emptyList(),
    val isInvoiceManagerVisible: Boolean = false,

    // Jeweler Profile Modal (Stitch ID 4457d74b46974ee99ffc049b24feb860)
    val isJewelerProfileModalVisible: Boolean = false,

    // Gold Union Standard Formulas Guide (Stitch ID 1e8173ae11924cad8cabf7f74a1c042b)
    val isStandardFormulasVisible: Boolean = false
)

class GoldCalculatorViewModel(application: Application) : AndroidViewModel(application) {
    private val customerRepository = CustomerRepository(application.applicationContext)
    private val portfolioRepository = PortfolioRepository(application.applicationContext)
    private val settingsRepository = SettingsRepository(application.applicationContext)
    private val invoiceRepository = InvoiceRepository(application.applicationContext)
    private val networkMonitor = NetworkMonitor(application.applicationContext)

    private val _uiState = MutableStateFlow(CalculatorUiState())
    val uiState: StateFlow<CalculatorUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
        loadInvoices()
        observeNetwork()
        loadInitialRates()
        loadCustomers()
        loadPortfolio()
        calculateAll()
        checkForUpdates(manual = false)
        startAutoRatesRefresh()
    }

    fun loadSettings() {
        val s = settingsRepository.loadSettings()
        _uiState.update {
            it.copy(
                appSettings = s,
                profitPercentInput = s.defaultProfitPercent,
                taxPercentInput = s.defaultTaxPercent,
                wageType = s.defaultWageType
            )
        }
        viewModelScope.launch {
            GoldMarketRepository.setSource(s.priceSource)
        }
    }

    fun updateSettings(newSettings: AppSettings) {
        settingsRepository.saveSettings(newSettings)
        _uiState.update {
            it.copy(
                appSettings = newSettings,
                profitPercentInput = newSettings.defaultProfitPercent,
                taxPercentInput = newSettings.defaultTaxPercent,
                wageType = newSettings.defaultWageType
            )
        }
        viewModelScope.launch {
            GoldMarketRepository.setSource(newSettings.priceSource)
        }
        calculateAll()
    }

    fun setSettingsDialogVisible(visible: Boolean) {
        _uiState.update { it.copy(isSettingsDialogVisible = visible) }
    }

    fun setTaxProfitModalVisible(visible: Boolean) {
        _uiState.update { it.copy(isTaxProfitModalVisible = visible) }
    }

    fun setPriceSourceModalVisible(visible: Boolean) {
        _uiState.update { it.copy(isPriceSourceModalVisible = visible) }
    }

    fun updateTaxAndProfit(profitPercent: String, taxPercent: String, wageType: WageType) {
        val updated = _uiState.value.appSettings.copy(
            defaultProfitPercent = profitPercent,
            defaultTaxPercent = taxPercent,
            defaultWageType = wageType
        )
        updateSettings(updated)
        _uiState.update {
            it.copy(
                profitPercentInput = profitPercent,
                taxPercentInput = taxPercent,
                wageType = wageType
            )
        }
        calculateAll()
    }

    fun updatePriceSource(source: PriceSource, autoSync: Boolean) {
        val updated = _uiState.value.appSettings.copy(
            priceSource = source,
            autoSyncRates = autoSync
        )
        updateSettings(updated)
        refreshRates()
    }

    fun setJewelerProfileModalVisible(visible: Boolean) {
        _uiState.update { it.copy(isJewelerProfileModalVisible = visible) }
    }

    fun setStandardFormulasVisible(visible: Boolean) {
        _uiState.update { it.copy(isStandardFormulasVisible = visible) }
    }

    fun updateJewelerProfile(
        galleryName: String,
        managerName: String,
        unionCode: String,
        phone: String,
        address: String
    ) {
        val updated = _uiState.value.appSettings.copy(
            galleryName = galleryName,
            managerName = managerName,
            unionCode = unionCode,
            galleryPhone = phone,
            galleryAddress = address,
            galleryLicense = "صنف طلا و جواهر: $unionCode"
        )
        updateSettings(updated)
        setJewelerProfileModalVisible(false)
    }

    fun toggleBiometricLock(enabled: Boolean) {
        val updated = _uiState.value.appSettings.copy(
            isBiometricLockEnabled = enabled
        )
        updateSettings(updated)
    }

    fun loadInvoices() {
        _uiState.update { it.copy(savedInvoices = invoiceRepository.getInvoices()) }
    }

    fun saveInvoice(invoice: Invoice) {
        invoiceRepository.saveInvoice(invoice)
        loadInvoices()
    }

    fun deleteInvoice(id: String) {
        invoiceRepository.deleteInvoice(id)
        loadInvoices()
    }

    fun setInvoiceManagerVisible(visible: Boolean) {
        _uiState.update { it.copy(isInvoiceManagerVisible = visible) }
    }

    private fun observeNetwork() {
        viewModelScope.launch {
            networkMonitor.status.collect { netStatus ->
                _uiState.update { current ->
                    current.copy(
                        connectionStatus = if (current.isRefreshingRates) ConnectionStatus.CONNECTING else netStatus
                    )
                }
            }
        }
    }

    private fun startAutoRatesRefresh() {
        viewModelScope.launch {
            while (isActive) {
                delay(60_000L) // بروزرسانی خودکار نرخ‌ها در هر ۱ دقیقه
                if (_uiState.value.autoSyncPrice) {
                    refreshRatesSilently()
                }
            }
        }
    }

    fun refreshRatesSilently() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshingRates = true, connectionStatus = ConnectionStatus.CONNECTING) }
            try {
                val updated = GoldMarketRepository.refreshRates()
                applyFetchedRates(updated)
            } catch (_: Exception) {
            } finally {
                val currentStatus = networkMonitor.checkInitialStatus()
                _uiState.update { it.copy(isRefreshingRates = false, connectionStatus = currentStatus) }
            }
        }
    }

    fun toggleTheme() {
        _uiState.update { it.copy(isDarkTheme = !it.isDarkTheme) }
    }

    private fun loadInitialRates() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshingRates = true, connectionStatus = ConnectionStatus.CONNECTING) }
            try {
                val rates = GoldMarketRepository.refreshRates()
                applyFetchedRates(rates)
            } catch (_: Exception) {
            } finally {
                val currentStatus = networkMonitor.checkInitialStatus()
                _uiState.update { it.copy(isRefreshingRates = false, connectionStatus = currentStatus) }
            }
        }
    }

    fun refreshRates() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshingRates = true, connectionStatus = ConnectionStatus.CONNECTING) }
            try {
                val updated = GoldMarketRepository.refreshRates()
                applyFetchedRates(updated)
            } catch (_: Exception) {
            } finally {
                val currentStatus = networkMonitor.checkInitialStatus()
                _uiState.update { it.copy(isRefreshingRates = false, connectionStatus = currentStatus) }
            }
        }
    }

    fun togglePriceSource() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshingRates = true) }
            GoldMarketRepository.cycleSource()
            val updated = GoldMarketRepository.rates.value
            applyFetchedRates(updated)
        }
    }

    private fun applyFetchedRates(rates: MarketRates) {
        _uiState.update { state ->
            val newSpot = if (state.autoSyncPrice && rates.gold18 > 0L) rates.gold18.toString() else state.spotPriceInput
            val newMesghal = if (rates.goldMelt > 0L) rates.goldMelt.toString() else state.mesghalPriceInput
            val newCoin = when (state.selectedCoin) {
                CoinType.EMAMI -> rates.coinEmami
                CoinType.BAHAR -> rates.coinBahar
                CoinType.HALF -> rates.coinHalf
                CoinType.QUARTER -> rates.coinQuarter
                CoinType.GERAMI -> rates.coinGerami
            }.toString()

            state.copy(
                rates = rates,
                isRefreshingRates = false,
                spotPriceInput = newSpot,
                mesghalPriceInput = newMesghal,
                coinMarketPriceInput = newCoin
            )
        }
        calculateAll()
    }

    fun selectTab(tab: AppTab) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    fun toggleAutoSyncPrice(enabled: Boolean) {
        _uiState.update { it.copy(autoSyncPrice = enabled) }
        if (enabled && _uiState.value.rates.gold18 > 0L) {
            val live18 = _uiState.value.rates.gold18.toString()
            onSpotPriceChanged(live18)
        }
    }

    private fun sanitizeDecimal(input: String): String {
        val clean = PersianNumberFormatter.toEnglishDigits(input).filter { it.isDigit() || it == '.' }
        val parts = clean.split('.')
        return if (parts.size > 2) parts[0] + "." + parts.subList(1, parts.size).joinToString("") else clean
    }

    // --- Jewelry Tab Actions ---
    fun onItemTitleChanged(newTitle: String) {
        _uiState.update { it.copy(itemTitleInput = newTitle) }
    }

    fun onGrossWeightChanged(newWeight: String) {
        val clean = sanitizeDecimal(newWeight)
        _uiState.update { it.copy(grossWeightInput = clean) }
        calculateJewelry()
    }

    fun onStoneWeightChanged(newStone: String) {
        val clean = sanitizeDecimal(newStone)
        _uiState.update { it.copy(stoneWeightInput = clean) }
        calculateJewelry()
    }

    fun onKaratSelected(karat: Karat) {
        _uiState.update { it.copy(selectedKarat = karat) }
        calculateJewelry()
    }

    fun onSpotPriceChanged(newPrice: String) {
        val cleanDigits = PersianNumberFormatter.toEnglishDigits(newPrice).filter { it.isDigit() }
        _uiState.update { it.copy(spotPriceInput = cleanDigits, autoSyncPrice = false) }
        calculateJewelry()
    }

    fun applyPresetSpotPrice(price: Long) {
        _uiState.update { it.copy(spotPriceInput = price.toString(), autoSyncPrice = false) }
        calculateJewelry()
    }

    fun onWageTypeChanged(type: WageType) {
        _uiState.update { it.copy(wageType = type) }
        calculateJewelry()
    }

    fun onWageChanged(newWage: String) {
        val clean = if (_uiState.value.wageType == WageType.PERCENTAGE) {
            sanitizeDecimal(newWage)
        } else {
            PersianNumberFormatter.toEnglishDigits(newWage).filter { it.isDigit() }
        }
        _uiState.update { it.copy(wageInput = clean) }
        calculateJewelry()
    }

    fun onProfitPercentChanged(newProfit: String) {
        val clean = sanitizeDecimal(newProfit)
        _uiState.update { it.copy(profitPercentInput = clean) }
        calculateJewelry()
    }

    fun applyPresetProfit(profit: Double) {
        val str = if (profit % 1.0 == 0.0) profit.toLong().toString() else profit.toString()
        _uiState.update { it.copy(profitPercentInput = str) }
        calculateJewelry()
    }

    fun onTaxPercentChanged(newTax: String) {
        val clean = sanitizeDecimal(newTax)
        _uiState.update { it.copy(taxPercentInput = clean) }
        calculateJewelry()
    }

    fun applyPresetTax(tax: Double) {
        val str = if (tax % 1.0 == 0.0) tax.toLong().toString() else tax.toString()
        _uiState.update { it.copy(taxPercentInput = str) }
        calculateJewelry()
    }

    fun addGrossWeight(amount: Double) {
        val current = PersianNumberFormatter.parsePersianOrEnglish(_uiState.value.grossWeightInput) ?: 0.0
        val next = (current + amount).coerceAtLeast(0.0)
        val formatted = if (next % 1.0 == 0.0) {
            next.toLong().toString()
        } else {
            "%.3f".format(Locale.US, next).trimEnd('0').trimEnd('.')
        }
        _uiState.update { it.copy(grossWeightInput = formatted) }
        calculateJewelry()
    }

    fun resetJewelry() {
        _uiState.update {
            it.copy(
                itemTitleInput = "قطعه طلا ",
                grossWeightInput = "10",
                stoneWeightInput = "0",
                selectedKarat = Karat.K18,
                spotPriceInput = it.rates.gold18.toString(),
                wageType = WageType.PERCENTAGE,
                wageInput = "12",
                profitPercentInput = "7",
                taxPercentInput = "9"
            )
        }
        calculateJewelry()
    }

    // --- Multi-Item Invoice & Customer Actions (Pure Clean MVVM) ---
    fun addItemToInvoice() {
        val state = _uiState.value
        val res = state.jewelryResult ?: return
        val currentCount = state.invoiceItems.size
        val item = InvoiceItem(
            title = state.itemTitleInput.ifBlank { "قطعه طلا " },
            karat = state.selectedKarat,
            grossWeight = res.grossWeight,
            stoneWeight = res.stoneWeight,
            netWeight = res.netWeight,
            spotPrice = PersianNumberFormatter.parseToCleanLong(state.spotPriceInput) ?: 0L,
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
        _uiState.update {
            it.copy(
                invoiceItems = it.invoiceItems + item,
                itemTitleInput = "قطعه طلا "
            )
        }
    }

    fun removeItemFromInvoice(itemId: String) {
        _uiState.update { it.copy(invoiceItems = it.invoiceItems.filter { item -> item.id != itemId }) }
    }

    fun clearInvoice() {
        _uiState.update { it.copy(invoiceItems = emptyList()) }
    }

    fun selectCustomer(customer: Customer?) {
        _uiState.update { it.copy(selectedCustomer = customer, isCustomerPickerVisible = false) }
    }

    fun setCustomerPickerVisible(visible: Boolean) {
        _uiState.update { it.copy(isCustomerPickerVisible = visible) }
    }

    fun setAddCustomerDialogVisible(visible: Boolean) {
        _uiState.update { it.copy(isAddCustomerDialogVisible = visible) }
    }

    fun setCustomerManagerVisible(visible: Boolean) {
        _uiState.update { it.copy(isCustomerManagerVisible = visible) }
    }

    fun loadCustomers() {
        viewModelScope.launch {
            val list = customerRepository.getCustomers()
            _uiState.update { it.copy(customerList = list) }
        }
    }

    fun addCustomer(customer: Customer, autoSelect: Boolean = true) {
        customerRepository.addCustomer(customer)
        val updated = customerRepository.getCustomers()
        _uiState.update {
            it.copy(
                customerList = updated,
                selectedCustomer = if (autoSelect) customer else it.selectedCustomer,
                isAddCustomerDialogVisible = false,
                isCustomerPickerVisible = false
            )
        }
    }

    fun updateCustomer(customer: Customer) {
        customerRepository.updateCustomer(customer)
        val updated = customerRepository.getCustomers()
        _uiState.update {
            it.copy(
                customerList = updated,
                selectedCustomer = if (it.selectedCustomer?.id == customer.id) customer else it.selectedCustomer
            )
        }
    }

    fun deleteCustomer(customerId: String) {
        customerRepository.deleteCustomer(customerId)
        val updated = customerRepository.getCustomers()
        _uiState.update {
            it.copy(
                customerList = updated,
                selectedCustomer = if (it.selectedCustomer?.id == customerId) null else it.selectedCustomer
            )
        }
    }

    // --- Portfolio Management Actions ---
    fun loadPortfolio() {
        val items = portfolioRepository.getItems()
        _uiState.update { it.copy(portfolioItems = items) }
    }

    fun addPortfolioItem(item: PortfolioItem) {
        portfolioRepository.addItem(item)
        val updated = portfolioRepository.getItems()
        _uiState.update { it.copy(portfolioItems = updated) }
    }

    fun deletePortfolioItem(itemId: String) {
        portfolioRepository.deleteItem(itemId)
        val updated = portfolioRepository.getItems()
        _uiState.update { it.copy(portfolioItems = updated) }
    }

    fun updatePortfolioItem(item: PortfolioItem) {
        portfolioRepository.deleteItem(item.id)
        portfolioRepository.addItem(item)
        val updated = portfolioRepository.getItems()
        _uiState.update { it.copy(portfolioItems = updated) }
    }

    // --- In-App Auto-Updater Actions ---
    fun checkForUpdates(manual: Boolean = false) {
        viewModelScope.launch {
            _uiState.update { it.copy(isCheckingForUpdate = true, isUpdateDialogDismissed = false) }
            val info = AppUpdateChecker.check()
            _uiState.update {
                it.copy(
                    updateInfo = info,
                    isCheckingForUpdate = false
                )
            }
        }
    }

    fun dismissUpdateDialog() {
        _uiState.update { it.copy(isUpdateDialogDismissed = true) }
    }

    fun resetUpdateDialog() {
        _uiState.update { it.copy(isUpdateDialogDismissed = false) }
    }

    fun buildCurrentInvoice(): Invoice {
        val state = _uiState.value
        val items = if (state.invoiceItems.isNotEmpty()) {
            state.invoiceItems
        } else if (state.jewelryResult != null) {
            val res = state.jewelryResult
            listOf(
                InvoiceItem(
                    title = state.itemTitleInput.ifBlank { "قطعه طلا" },
                    karat = state.selectedKarat,
                    grossWeight = res.grossWeight,
                    stoneWeight = res.stoneWeight,
                    netWeight = res.netWeight,
                    spotPrice = PersianNumberFormatter.parseToCleanLong(state.spotPriceInput) ?: 0L,
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

    // --- Melt Tab Actions ---
    fun onMesghalPriceChanged(newMesghal: String) {
        val clean = PersianNumberFormatter.toEnglishDigits(newMesghal).filter { it.isDigit() }
        _uiState.update { it.copy(mesghalPriceInput = clean) }
        calculateMelt()
    }

    fun onMeltWeightChanged(newWeight: String) {
        val clean = PersianNumberFormatter.toEnglishDigits(newWeight).filter { it.isDigit() || it == '.' }
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
        }.toString()
        _uiState.update { it.copy(selectedCoin = coin, coinMarketPriceInput = marketPrice) }
        calculateCoin()
    }

    fun onCoinMarketPriceChanged(newPrice: String) {
        val clean = PersianNumberFormatter.toEnglishDigits(newPrice).filter { it.isDigit() }
        _uiState.update { it.copy(coinMarketPriceInput = clean) }
        calculateCoin()
    }

    // --- Karat Convert Actions ---
    fun onConvertWeightChanged(newWeight: String) {
        val clean = PersianNumberFormatter.toEnglishDigits(newWeight).filter { it.isDigit() || it == '.' }
        _uiState.update { it.copy(convertWeightInput = clean) }
        calculateConvert()
    }

    fun onConvertFromKarat(karat: Karat) {
        _uiState.update { it.copy(convertFromKarat = karat) }
        calculateConvert()
    }

    fun onConvertToKarat(karat: Karat) {
        _uiState.update { it.copy(convertToKarat = karat) }
        calculateConvert()
    }

    // --- Calculations ---
    private fun calculateAll() {
        calculateJewelry()
        calculateMelt()
        calculateCoin()
        calculateConvert()
    }

    private fun calculateJewelry() {
        val state = _uiState.value
        val gross = PersianNumberFormatter.parsePersianOrEnglish(state.grossWeightInput) ?: 0.0
        val stone = PersianNumberFormatter.parsePersianOrEnglish(state.stoneWeightInput) ?: 0.0
        val net = (gross - stone).coerceAtLeast(0.0)
        val spot18 = PersianNumberFormatter.parseToCleanLong(state.spotPriceInput) ?: 0L
        val wageVal = PersianNumberFormatter.parsePersianOrEnglish(state.wageInput) ?: 0.0
        val profitPercent = PersianNumberFormatter.parsePersianOrEnglish(state.profitPercentInput) ?: 0.0
        val taxPercent = PersianNumberFormatter.parsePersianOrEnglish(state.taxPercentInput) ?: 0.0

        val words = if (spot18 > 0) PersianWordsFormatter.toWords(spot18) else ""

        if (net <= 0.0 || spot18 <= 0L) {
            _uiState.update { it.copy(jewelryResult = null, priceInWords = words) }
            return
        }

        val pureGramSpot = spot18.toDouble() / (18.0 / 24.0)
        val rawValue = net * state.selectedKarat.purityRatio * pureGramSpot

        val wageAmount = when (state.wageType) {
            WageType.PERCENTAGE -> rawValue * (wageVal / 100.0)
            WageType.TOMAN_PER_GRAM -> net * wageVal
        }

        val profitAmount = (rawValue + wageAmount) * (profitPercent / 100.0)
        val taxAmount = (wageAmount + profitAmount) * (taxPercent / 100.0)
        val totalPayable = rawValue + wageAmount + profitAmount + taxAmount
        val effectivePrice = if (net > 0) totalPayable / net else 0.0

        val result = DetailedJewelryResult(
            grossWeight = gross,
            stoneWeight = stone,
            netWeight = net,
            rawGoldValue = rawValue,
            wageAmount = wageAmount,
            profitAmount = profitAmount,
            taxAmount = taxAmount,
            totalPayable = totalPayable,
            effectiveGramPrice = effectivePrice
        )

        _uiState.update { it.copy(jewelryResult = result, priceInWords = words) }
    }

    private fun calculateMelt() {
        val state = _uiState.value
        val mesghal = PersianNumberFormatter.parsePersianOrEnglish(state.mesghalPriceInput) ?: 0.0
        val weight = PersianNumberFormatter.parsePersianOrEnglish(state.meltWeightInput) ?: 0.0
        val gram18k = (mesghal / 4.33185).toLong()
        val total = (gram18k * weight)
        _uiState.update { it.copy(meltGram18kPrice = gram18k, meltTotalValue = total) }
    }

    private fun calculateCoin() {
        val state = _uiState.value
        val marketPrice = PersianNumberFormatter.parsePersianOrEnglish(state.coinMarketPriceInput) ?: 0.0
        val coin = state.selectedCoin
        val pureWeight = coin.pureWeightGrams

        val usd = state.rates.usd.toDouble()
        val ons = state.rates.ons
        val gram24Price = (ons * usd) / 31.1035
        val intrinsic = (pureWeight * gram24Price) + coin.mintFee
        val bubble = marketPrice - intrinsic
        val bubblePercent = if (intrinsic > 0) (bubble / intrinsic) * 100.0 else 0.0

        val result = CoinBubbleResult(
            coinType = coin,
            marketPrice = marketPrice,
            intrinsicValue = intrinsic,
            bubbleAmount = bubble,
            bubblePercent = bubblePercent
        )
        _uiState.update { it.copy(coinBubbleResult = result) }
    }

    private fun calculateConvert() {
        val state = _uiState.value
        val weight = PersianNumberFormatter.parsePersianOrEnglish(state.convertWeightInput) ?: 0.0
        val fromRatio = state.convertFromKarat.purityRatio
        val toRatio = state.convertToKarat.purityRatio
        val converted = if (toRatio > 0) weight * (fromRatio / toRatio) else 0.0
        _uiState.update { it.copy(convertedWeight = converted) }
    }
}
