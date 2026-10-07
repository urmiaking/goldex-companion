package com.goldex.companion.desktop.state

import com.goldex.companion.desktop.data.MarketSnapshot
import com.goldex.companion.domain.calculator.GoldCalculationUseCases
import com.goldex.companion.model.PriceBasisTab
import com.goldex.companion.presentation.calculator.CalculatorField
import com.goldex.companion.presentation.calculator.ManualGoldCalculator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class DesktopCalculatorRateState(val automatic: Boolean = true, val quote: MarketSnapshot? = null)

/** Desktop quote binding; shared financial policy and manual form remain unchanged. */
class DesktopCalculatorRates(private val calculator: ManualGoldCalculator, initial: MarketSnapshot?) {
    private val mutable = MutableStateFlow(DesktopCalculatorRateState(quote = initial))
    val state = mutable.asStateFlow()

    init { applyLatest() }

    @Synchronized fun updateQuote(quote: MarketSnapshot?) {
        mutable.update { it.copy(quote = quote) }
        if (state.value.automatic) applyLatest()
    }

    @Synchronized fun setInput(field: CalculatorField, value: String) {
        if (field == CalculatorField.SPOT) mutable.update { it.copy(automatic = false) }
        calculator.setInput(field, value)
    }

    @Synchronized fun setPriceBasis(basis: PriceBasisTab) {
        calculator.setPriceBasis(basis)
        if (state.value.automatic) applyLatest()
    }

    @Synchronized fun useMarketRate() {
        mutable.update { it.copy(automatic = true) }
        applyLatest()
    }

    @Synchronized fun reset() {
        calculator.reset()
        useMarketRate()
    }

    private fun applyLatest() {
        val rates = state.value.quote?.rates ?: return
        val basis = calculator.state.value.priceBasis
        val supplied = when (basis) {
            PriceBasisTab.K18 -> rates.gold18
            PriceBasisTab.K24 -> rates.gold24
            PriceBasisTab.MESGHAL -> rates.goldMelt
        }
        val value = supplied.takeIf { it > 0 } ?: if (rates.gold18 > 0) GoldCalculationUseCases.fromSpotPrice18k(rates.gold18, basis) else return
        calculator.setInput(CalculatorField.SPOT, value.toString())
    }
}
