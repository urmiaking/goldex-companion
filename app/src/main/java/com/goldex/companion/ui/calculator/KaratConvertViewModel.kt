package com.goldex.companion.ui.calculator

import androidx.lifecycle.ViewModel
import com.goldex.companion.domain.calculator.GoldCalculationUseCases
import com.goldex.companion.model.Karat
import com.goldex.companion.model.PersianNumberFormatter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class KaratConvertMode(val labelFa: String) {
    DIRECT("تبدیل مستقیم عیار"),
    SETTLEMENT("محاسبه شرطی ری‌گیری")
}

data class KaratConvertUiState(
    val convertMode: KaratConvertMode = KaratConvertMode.DIRECT,
    val convertWeightInput: String = "10",
    val convertFromKarat: Karat = Karat.K18,
    val convertToKarat: Karat = Karat.K24,
    val convertedWeight: Double = 7.5,
    val assayKaratInput: String = "742",
    val agreedKaratInput: String = "750"
)

class KaratConvertViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(KaratConvertUiState())
    val uiState: StateFlow<KaratConvertUiState> = _uiState.asStateFlow()

    fun onConvertWeightChanged(newWeight: String) {
        updateAndCalculate {
            it.copy(convertWeightInput = sanitizeDecimal(newWeight))
        }
    }

    fun onConvertFromKarat(karat: Karat) {
        updateAndCalculate { it.copy(convertFromKarat = karat) }
    }

    fun onConvertToKarat(karat: Karat) {
        updateAndCalculate { it.copy(convertToKarat = karat) }
    }

    fun swapConvertKarats() {
        updateAndCalculate {
            it.copy(
                convertFromKarat = it.convertToKarat,
                convertToKarat = it.convertFromKarat
            )
        }
    }

    fun setConvertMode(mode: KaratConvertMode) {
        _uiState.update { it.copy(convertMode = mode) }
    }

    fun onAssayKaratChanged(newKarat: String) {
        _uiState.update { it.copy(assayKaratInput = digitsOnly(newKarat)) }
    }

    fun onAgreedKaratChanged(newKarat: String) {
        _uiState.update { it.copy(agreedKaratInput = digitsOnly(newKarat)) }
    }

    private fun updateAndCalculate(transform: (KaratConvertUiState) -> KaratConvertUiState) {
        _uiState.update { current ->
            val updated = transform(current)
            val weight = PersianNumberFormatter.parsePersianOrEnglish(updated.convertWeightInput) ?: 0.0
            updated.copy(
                convertedWeight = GoldCalculationUseCases.calculateKaratConversion(
                    weight = weight,
                    from = updated.convertFromKarat,
                    to = updated.convertToKarat
                )
            )
        }
    }

    private fun sanitizeDecimal(input: String): String {
        val clean = PersianNumberFormatter.toEnglishDigits(input).filter { it.isDigit() || it == '.' }
        val parts = clean.split('.')
        return if (parts.size > 2) {
            parts[0] + "." + parts.drop(1).joinToString("")
        } else {
            clean
        }
    }

    private fun digitsOnly(input: String): String =
        PersianNumberFormatter.toEnglishDigits(input).filter(Char::isDigit)
}
