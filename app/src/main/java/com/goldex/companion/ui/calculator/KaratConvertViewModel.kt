package com.goldex.companion.ui.calculator

import androidx.lifecycle.ViewModel
import com.goldex.companion.domain.calculator.KaratConversionPolicy
import com.goldex.companion.domain.calculator.KaratConversionResult
import com.goldex.companion.domain.calculator.AssaySettlementResult
import com.goldex.companion.domain.invoice.BarterCalculationUseCases
import com.goldex.companion.model.BarterItem
import com.goldex.companion.model.WageType
import com.goldex.companion.model.Karat
import com.goldex.companion.model.PersianNumberFormatter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class KaratConvertMode(val labelFa: String) {
    DIRECT("تبدیل عیار"),
    SETTLEMENT("ری‌گیری شرطی")
}

data class KaratConvertUiState(
    val convertMode: KaratConvertMode = KaratConvertMode.DIRECT,
    val convertWeightInput: String = "",
    val convertFromKarat: Karat = Karat.K18,
    val convertToKarat: Karat = Karat.K24,
    val convertedWeight: Double = 0.0,
    val customFromKarat: String? = null,
    val customToKarat: String? = null,
    val conversion: KaratConversionResult? = null,
    val assay: AssaySettlementResult? = null,
    val spotPrice18k: Long = 0L,
    val assayKaratInput: String = "",
    val agreedKaratInput: String = "750"
) {
    val assayCashTomans: Long? get() = assay?.valueAt(spotPrice18k)
    val fromInput: String get() = customFromKarat ?: convertFromKarat.karatNumber.toString()
    val toInput: String get() = customToKarat ?: convertToKarat.karatNumber.toString()
    val invoiceGold750: Double? get() = if (convertMode == KaratConvertMode.DIRECT) conversion?.gold750Weight else assay?.gold750Weight
}

class KaratConvertViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(KaratConvertUiState())
    val uiState: StateFlow<KaratConvertUiState> = _uiState.asStateFlow()

    fun onSpotPriceChanged(rate: Long) {
        _uiState.update { it.copy(spotPrice18k = rate) }
    }

    fun onConvertWeightChanged(newWeight: String) {
        updateAndCalculate {
            it.copy(convertWeightInput = sanitizeDecimal(newWeight))
        }
    }

    fun onConvertFromKarat(karat: Karat) {
        updateAndCalculate { it.copy(convertFromKarat = karat, customFromKarat = null) }
    }

    fun onConvertToKarat(karat: Karat) {
        updateAndCalculate { it.copy(convertToKarat = karat, customToKarat = null) }
    }

    fun swapConvertKarats() {
        updateAndCalculate {
            it.copy(
                convertFromKarat = it.convertToKarat,
                convertToKarat = it.convertFromKarat,
                customFromKarat = it.customToKarat,
                customToKarat = it.customFromKarat
            )
        }
    }

    fun setConvertMode(mode: KaratConvertMode) {
        _uiState.update { it.copy(convertMode = mode) }
    }

    fun onAssayKaratChanged(newKarat: String) {
        updateAndCalculate { it.copy(assayKaratInput = digitsOnly(newKarat)) }
    }

    fun onAgreedKaratChanged(newKarat: String) {
        updateAndCalculate { it.copy(agreedKaratInput = digitsOnly(newKarat)) }
    }

    private fun updateAndCalculate(transform: (KaratConvertUiState) -> KaratConvertUiState) {
        _uiState.update { current ->
            val updated = transform(current)
            val weight = PersianNumberFormatter.parsePersianOrEnglish(updated.convertWeightInput) ?: 0.0
            val conversion = KaratConversionPolicy.convert(weight, updated.fromInput.toIntOrNull() ?: 0, updated.toInput.toIntOrNull() ?: 0)
            val assay = KaratConversionPolicy.assay(weight, updated.agreedKaratInput.toIntOrNull() ?: 0, updated.assayKaratInput.toIntOrNull() ?: 0)
            updated.copy(convertedWeight = conversion?.targetWeight ?: 0.0, conversion = conversion, assay = assay)
        }
    }

    fun selectCustomFromKarat() {
        updateAndCalculate { it.copy(customFromKarat = it.customFromKarat ?: "") }
    }

    fun selectCustomToKarat() {
        updateAndCalculate { it.copy(customToKarat = it.customToKarat ?: "") }
    }

    fun onCustomFromKaratChanged(value: String) {
        updateAndCalculate { it.copy(customFromKarat = digitsOnly(value)) }
    }

    fun onCustomToKaratChanged(value: String) {
        updateAndCalculate { it.copy(customToKarat = digitsOnly(value)) }
    }

    fun createInvoiceItem(spotPrice18k: Long): BarterItem? {
        val state = _uiState.value
        val gold750 = state.invoiceGold750?.takeIf { it > 0.0 && it.isFinite() } ?: return null
        if (spotPrice18k <= 0L) return null
        return BarterCalculationUseCases.calculateCraftedItem(
            title = if (state.convertMode == KaratConvertMode.DIRECT) "طلای تبدیل عیار (${state.fromInput} به ${state.toInput})" else "طلای ری‌گیری (${state.assayKaratInput})",
            karat = Karat.K18, customKaratValue = 750, grossWeight = gold750, stoneWeight = 0.0,
            spotPrice18k = spotPrice18k, wageType = WageType.PERCENTAGE, wageInput = 0.0,
            profitPercent = 0.0, taxPercent = 0.0
        ).takeIf { it.totalPayable.isFinite() && it.totalPayable < Long.MAX_VALUE.toDouble() }
    }

    private fun sanitizeDecimal(input: String): String {
        val clean = PersianNumberFormatter.toEnglishDigits(input).replace('٫', '.').filter { it.isDigit() || it == '.' || it == '-' }
        val parts = clean.split('.')
        return if (parts.size > 2) {
            parts[0] + "." + parts.drop(1).joinToString("")
        } else {
            clean
        }
    }

    private fun digitsOnly(input: String): String =
        PersianNumberFormatter.toEnglishDigits(input).filter { it.isDigit() || it == '-' }
}
