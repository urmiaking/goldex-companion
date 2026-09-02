package com.goldex.companion.ui.calculator

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.goldex.companion.data.GoldMarketRepository
import com.goldex.companion.data.MarketRates
import com.goldex.companion.data.PriceSource
import com.goldex.companion.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class AppTab(val titleFa: String) {
    JEWELRY("طلا و جواهر"),
    MELT("مظنه آبشده"),
    COIN("حباب سکه"),
    CONVERT("تبدیل عیار")
}

data class CalculatorUiState(
    val selectedTab: AppTab = AppTab.JEWELRY,
    val rates: MarketRates = MarketRates(),
    val isRefreshingRates: Boolean = false,
    val autoSyncPrice: Boolean = true,

    // Jewelry Tab State
    val grossWeightInput: String = "10",
    val stoneWeightInput: String = "0",
    val selectedKarat: Karat = Karat.K18,
    val spotPriceInput: String = "22835100",
    val wageType: WageType = WageType.PERCENTAGE,
    val wageInput: String = "12",
    val profitPercentInput: String = "7",
    val taxPercentInput: String = "9",
    val jewelryResult: DetailedJewelryResult? = null,
    val priceInWords: String = "",

    // Melt Tab State
    val mesghalPriceInput: String = "98975000",
    val meltWeightInput: String = "10",
    val meltGram18kPrice: Long = 22848182L,
    val meltTotalValue: Double = 0.0,

    // Coin Bubble State
    val selectedCoin: CoinType = CoinType.EMAMI,
    val coinMarketPriceInput: String = "228000000",
    val coinBubbleResult: CoinBubbleResult? = null,

    // Karat Convert State
    val convertWeightInput: String = "10",
    val convertFromKarat: Karat = Karat.K18,
    val convertToKarat: Karat = Karat.K24,
    val convertedWeight: Double = 7.5
)

class GoldCalculatorViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(CalculatorUiState())
    val uiState: StateFlow<CalculatorUiState> = _uiState.asStateFlow()

    init {
        loadInitialRates()
        calculateAll()
    }

    private fun loadInitialRates() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshingRates = true) }
            val rates = GoldMarketRepository.refreshRates()
            applyFetchedRates(rates)
        }
    }

    fun refreshRates() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshingRates = true) }
            val updated = GoldMarketRepository.refreshRates()
            applyFetchedRates(updated)
        }
    }

    fun togglePriceSource() {
        viewModelScope.launch {
            val nextSource = if (_uiState.value.rates.source == PriceSource.ISIGNAL) {
                PriceSource.TALA_IR
            } else {
                PriceSource.ISIGNAL
            }
            _uiState.update { it.copy(isRefreshingRates = true) }
            GoldMarketRepository.setSource(nextSource)
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

    // --- Jewelry Tab Actions ---
    fun onGrossWeightChanged(newWeight: String) {
        val clean = PersianNumberFormatter.toEnglishDigits(newWeight).filter { it.isDigit() || it == '.' }
        _uiState.update { it.copy(grossWeightInput = clean) }
        calculateJewelry()
    }

    fun onStoneWeightChanged(newStone: String) {
        val clean = PersianNumberFormatter.toEnglishDigits(newStone).filter { it.isDigit() || it == '.' }
        _uiState.update { it.copy(stoneWeightInput = clean) }
        calculateJewelry()
    }

    fun onKaratSelected(karat: Karat) {
        _uiState.update { it.copy(selectedKarat = karat) }
        calculateJewelry()
    }

    fun onSpotPriceChanged(newPrice: String) {
        val cleanDigits = PersianNumberFormatter.toEnglishDigits(newPrice).filter { it.isDigit() }
        _uiState.update { it.copy(spotPriceInput = cleanDigits) }
        calculateJewelry()
    }

    fun applyPresetSpotPrice(price: Long) {
        _uiState.update { it.copy(spotPriceInput = price.toString()) }
        calculateJewelry()
    }

    fun onWageTypeChanged(type: WageType) {
        _uiState.update { it.copy(wageType = type) }
        calculateJewelry()
    }

    fun onWageChanged(newWage: String) {
        val clean = PersianNumberFormatter.toEnglishDigits(newWage).filter { it.isDigit() || it == '.' }
        _uiState.update { it.copy(wageInput = clean) }
        calculateJewelry()
    }

    fun onProfitPercentChanged(newProfit: String) {
        val clean = PersianNumberFormatter.toEnglishDigits(newProfit).filter { it.isDigit() || it == '.' }
        _uiState.update { it.copy(profitPercentInput = clean) }
        calculateJewelry()
    }

    fun onTaxPercentChanged(newTax: String) {
        val clean = PersianNumberFormatter.toEnglishDigits(newTax).filter { it.isDigit() || it == '.' }
        _uiState.update { it.copy(taxPercentInput = clean) }
        calculateJewelry()
    }

    fun addGrossWeight(amount: Double) {
        val current = PersianNumberFormatter.parsePersianOrEnglish(_uiState.value.grossWeightInput) ?: 0.0
        val next = (current + amount).coerceAtLeast(0.0)
        val formatted = if (next % 1.0 == 0.0) next.toLong().toString() else "%.2f".format(next)
        _uiState.update { it.copy(grossWeightInput = formatted) }
        calculateJewelry()
    }

    fun resetJewelry() {
        _uiState.update {
            it.copy(
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
