package com.goldex.companion.desktop.state

import com.goldex.companion.domain.calculator.GoldCalculationUseCases
import com.goldex.companion.model.CraftedGoldItem
import com.goldex.companion.model.PersianNumberFormatter
import com.goldex.companion.presentation.calculator.CalculatorField
import com.goldex.companion.presentation.calculator.ManualGoldCalculatorState

/** Transfer the validated quote/result snapshot; never recalculate it at a different live price. */
internal fun calculatorInvoiceItem(state: ManualGoldCalculatorState): CraftedGoldItem? {
    val result = state.result ?: return null
    fun number(field: CalculatorField) = PersianNumberFormatter.parseToCleanDouble(state.input(field)) ?: 0.0
    val spot = GoldCalculationUseCases.toSpotPrice18k(PersianNumberFormatter.parseToCleanLong(state.input(CalculatorField.SPOT)) ?: return null, state.priceBasis)
    val karat = number(CalculatorField.KARAT).toInt()
    return CraftedGoldItem(title = "قطعه طلای محاسبه‌شده", customKaratValue = karat,
        grossWeight = result.grossWeight, stoneWeight = result.stoneWeight, netWeight = result.netWeight,
        spotPrice = spot, wageType = state.wageType, wageInput = number(CalculatorField.WAGE), wageAmount = result.wageAmount,
        profitPercent = number(CalculatorField.PROFIT), profitAmount = result.profitAmount,
        taxPercent = number(CalculatorField.TAX), taxAmount = result.taxAmount, rawGoldValue = result.rawGoldValue,
        totalPayable = result.totalPayable, equivalent18kWeight = result.netWeight * karat / 750.0)
}
