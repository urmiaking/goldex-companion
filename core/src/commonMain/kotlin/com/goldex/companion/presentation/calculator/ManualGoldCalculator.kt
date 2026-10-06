package com.goldex.companion.presentation.calculator

import com.goldex.companion.data.AppSettings
import com.goldex.companion.domain.calculator.GoldCalculationUseCases
import com.goldex.companion.model.DetailedJewelryResult
import com.goldex.companion.model.PersianNumberFormatter
import com.goldex.companion.model.PriceBasisTab
import com.goldex.companion.model.WageType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class CalculatorField { SPOT, GROSS_WEIGHT, STONE_WEIGHT, KARAT, WAGE, PROFIT, TAX }

/** Manual quotes are explicit: this state never manufactures a live market rate. */
data class ManualGoldCalculatorState(
    val inputs: Map<CalculatorField, String>,
    val priceBasis: PriceBasisTab = PriceBasisTab.K18,
    val wageType: WageType = WageType.PERCENTAGE,
    val errors: Map<CalculatorField, String> = emptyMap(),
    val result: DetailedJewelryResult? = null
) {
    fun input(field: CalculatorField): String = inputs[field].orEmpty()
}

/** Owns form parsing and validation; the existing domain policy owns every calculation. */
class ManualGoldCalculator(private val defaults: AppSettings = AppSettings()) {
    private val mutableState = MutableStateFlow(initialState())
    val state = mutableState.asStateFlow()

    fun setInput(field: CalculatorField, value: String) {
        mutableState.update { evaluate(it.copy(inputs = it.inputs + (field to value))) }
    }

    fun setPriceBasis(basis: PriceBasisTab) {
        mutableState.update { current ->
            if (basis == current.priceBasis) current else {
                val price = normalized(current.input(CalculatorField.SPOT)).toLongOrNull()
                val converted = if (price != null && price > 0) {
                    val spot18k = GoldCalculationUseCases.toSpotPrice18k(price, current.priceBasis)
                    GoldCalculationUseCases.fromSpotPrice18k(spot18k, basis).toString()
                } else current.input(CalculatorField.SPOT)
                evaluate(current.copy(priceBasis = basis, inputs = current.inputs + (CalculatorField.SPOT to converted)))
            }
        }
    }

    fun setWageType(type: WageType) {
        mutableState.update { current ->
            // A percentage must never silently become a whole-toman per-gram wage.
            if (current.wageType == type) current else
                evaluate(current.copy(wageType = type, inputs = current.inputs + (CalculatorField.WAGE to "")))
        }
    }

    fun reset() { mutableState.value = initialState() }

    fun summary(): String? {
        val current = state.value
        val result = current.result ?: return null
        return """
            قیراط — محاسبه با نرخ دستی
            وزن خالص: ${PersianNumberFormatter.formatWeight(result.netWeight)} گرم
            ارزش طلا: ${PersianNumberFormatter.formatPrice(result.rawGoldValue)} تومان
            اجرت: ${PersianNumberFormatter.formatPrice(result.wageAmount)} تومان
            سود: ${PersianNumberFormatter.formatPrice(result.profitAmount)} تومان
            مالیات (${PersianNumberFormatter.toPersianDigits(current.input(CalculatorField.TAX))}٪): ${PersianNumberFormatter.formatPrice(result.taxAmount)} تومان
            مبلغ نهایی: ${PersianNumberFormatter.formatPrice(result.totalPayable)} تومان
        """.trimIndent()
    }

    private fun initialState() = ManualGoldCalculatorState(
        inputs = mapOf(
            CalculatorField.KARAT to "750",
            CalculatorField.WAGE to "0",
            CalculatorField.PROFIT to defaults.defaultProfitPercent,
            CalculatorField.TAX to defaults.defaultTaxPercent
        ),
        wageType = defaults.defaultWageType
    )

    private fun evaluate(current: ManualGoldCalculatorState): ManualGoldCalculatorState {
        val errors = mutableMapOf<CalculatorField, String>()
        fun decimal(field: CalculatorField, optional: Boolean = false, scale: Int? = null): Double? {
            val text = normalized(current.input(field))
            if (text.isEmpty()) return if (optional) 0.0 else null
            val value = text.takeIf { it.matches(Regex("[0-9]+(\\.[0-9]+)?")) }?.toDoubleOrNull()
            if (value == null || !value.isFinite() || (scale != null && text.substringAfter('.', "").length > scale)) {
                errors[field] = if (scale == 3) "وزن را با حداکثر سه رقم اعشار وارد کنید" else "عدد معتبر وارد کنید"
                return null
            }
            return value
        }
        val priceText = normalized(current.input(CalculatorField.SPOT))
        val price = priceText.toLongOrNull()?.takeIf { it > 0 }
        if (priceText.isNotEmpty() && price == null) errors[CalculatorField.SPOT] = "نرخ را به تومان و بزرگ‌تر از صفر وارد کنید"
        val gross = decimal(CalculatorField.GROSS_WEIGHT, scale = 3)
        val stone = decimal(CalculatorField.STONE_WEIGHT, optional = true, scale = 3)
        if (gross != null && gross <= 0) errors[CalculatorField.GROSS_WEIGHT] = "وزن باید بزرگ‌تر از صفر باشد"
        if (gross != null && stone != null && stone >= gross) errors[CalculatorField.STONE_WEIGHT] = "کسر نگین باید کمتر از وزن باشد"
        val karat = normalized(current.input(CalculatorField.KARAT)).toIntOrNull()?.takeIf { it in 1..1000 }
        if (karat == null && current.input(CalculatorField.KARAT).isNotEmpty()) errors[CalculatorField.KARAT] = "عیار باید بین ۱ و ۱۰۰۰ باشد"
        val wage = decimal(CalculatorField.WAGE)
        val profit = decimal(CalculatorField.PROFIT)
        val tax = decimal(CalculatorField.TAX)
        listOf(CalculatorField.PROFIT to profit, CalculatorField.TAX to tax).forEach { (field, value) ->
            if (value != null && value > 100) errors[field] = "درصد باید بین صفر و ۱۰۰ باشد"
        }
        if (wage != null && current.wageType == WageType.PERCENTAGE && wage > 100) errors[CalculatorField.WAGE] = "درصد باید بین صفر و ۱۰۰ باشد"
        val result = if (errors.isEmpty() && price != null && gross != null && stone != null &&
            karat != null && wage != null && profit != null && tax != null) {
            GoldCalculationUseCases.calculateJewelry(
                grossWeight = gross, stoneWeight = stone, customKaratValue = karat,
                spotPrice18k = GoldCalculationUseCases.toSpotPrice18k(price, current.priceBasis),
                wageType = current.wageType, wageInput = wage, profitPercent = profit, taxPercent = tax
            )
        } else null
        if (result != null && (!result.totalPayable.isFinite() || result.totalPayable >= Long.MAX_VALUE.toDouble())) {
            errors[CalculatorField.SPOT] = "مبلغ از محدودهٔ مجاز بیشتر است"
        }
        return current.copy(errors = errors.toMap(), result = result.takeIf { errors.isEmpty() })
    }

    private fun normalized(input: String): String = PersianNumberFormatter.toEnglishDigits(input)
        .replace("٬", "").replace("،", "").replace(",", "").trim()
}
