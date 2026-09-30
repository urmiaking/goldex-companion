package com.goldex.companion.domain.portfolio

import com.goldex.companion.data.MarketRates
import com.goldex.companion.data.PortfolioItem

data class PortfolioSummary(
    val currentValue: Long,
    val purchaseValue: Long,
    val profit: Long,
    val profitPercent: Double
)

object PortfolioValuation {
    fun summarize(items: List<PortfolioItem>, rates: MarketRates): PortfolioSummary {
        val currentValue = items.sumOf { it.calculateCurrentValue(rates) }
        val purchaseValue = items.sumOf { it.purchasePriceTotal }
        val profit = currentValue - purchaseValue
        val profitPercent = if (purchaseValue > 0L) {
            (profit.toDouble() / purchaseValue.toDouble()) * 100.0
        } else {
            0.0
        }
        return PortfolioSummary(
            currentValue = currentValue,
            purchaseValue = purchaseValue,
            profit = profit,
            profitPercent = profitPercent
        )
    }
}
