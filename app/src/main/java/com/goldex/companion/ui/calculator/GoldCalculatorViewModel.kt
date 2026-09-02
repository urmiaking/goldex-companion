package com.goldex.companion.ui.calculator

import androidx.lifecycle.ViewModel
import com.goldex.companion.model.CalculationResult
import com.goldex.companion.model.Karat
import com.goldex.companion.model.PersianNumberFormatter
import com.goldex.companion.model.PriceBasis
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class CalculatorUiState(
    val weightInput: String = "10",
    val spotPriceInput: String = "4,500,000",
    val selectedKarat: Karat = Karat.K18,
    val priceBasis: PriceBasis = PriceBasis.PER_GRAM_18K,
    val marginPercentInput: String = "7",
    val calculationResult: CalculationResult? = null,
    val error: String? = null
)

class GoldCalculatorViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(CalculatorUiState())
    val uiState: StateFlow<CalculatorUiState> = _uiState.asStateFlow()

    init {
        calculate()
    }

    fun onWeightChanged(newWeight: String) {
        _uiState.update { it.copy(weightInput = newWeight) }
        calculate()
    }

    fun onSpotPriceChanged(newPrice: String) {
        _uiState.update { it.copy(spotPriceInput = newPrice) }
        calculate()
    }

    fun onKaratSelected(karat: Karat) {
        _uiState.update { it.copy(selectedKarat = karat) }
        calculate()
    }

    fun onPriceBasisSelected(basis: PriceBasis) {
        _uiState.update { it.copy(priceBasis = basis) }
        calculate()
    }

    fun onMarginChanged(newMargin: String) {
        _uiState.update { it.copy(marginPercentInput = newMargin) }
        calculate()
    }

    fun addWeight(amount: Double) {
        val currentWeight = PersianNumberFormatter.parsePersianOrEnglish(_uiState.value.weightInput) ?: 0.0
        val next = (currentWeight + amount).coerceAtLeast(0.0)
        _uiState.update { it.copy(weightInput = if (next % 1.0 == 0.0) next.toLong().toString() else "%.2f".format(next)) }
        calculate()
    }

    fun reset() {
        _uiState.value = CalculatorUiState(
            weightInput = "",
            spotPriceInput = "",
            selectedKarat = Karat.K18,
            priceBasis = PriceBasis.PER_GRAM_18K,
            marginPercentInput = "7",
            calculationResult = null,
            error = null
        )
    }

    private fun calculate() {
        val state = _uiState.value
        val weight = PersianNumberFormatter.parsePersianOrEnglish(state.weightInput)
        val price = PersianNumberFormatter.parsePersianOrEnglish(state.spotPriceInput)
        val margin = PersianNumberFormatter.parsePersianOrEnglish(state.marginPercentInput) ?: 0.0

        if (weight == null || weight <= 0.0 || price == null || price <= 0.0) {
            _uiState.update { it.copy(calculationResult = null, error = null) }
            return
        }

        val pureWeight = weight * state.selectedKarat.purityRatio
        val pureGramPrice = price / state.priceBasis.ratio
        val rawValue = pureWeight * pureGramPrice
        val marginVal = rawValue * (margin / 100.0)
        val totalVal = rawValue + marginVal
        val effectiveGramPrice = totalVal / weight

        val result = CalculationResult(
            pureGoldWeightGrams = pureWeight,
            rawGoldValue = rawValue,
            marginAmount = marginVal,
            totalTradeValue = totalVal,
            effectivePricePerGram = effectiveGramPrice
        )

        _uiState.update { it.copy(calculationResult = result, error = null) }
    }
}
