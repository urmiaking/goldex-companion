package com.goldex.companion.ui.rates

import com.goldex.companion.data.MarketRates
import com.goldex.companion.model.MarketCandle
import com.goldex.companion.model.MarketRateItemType

data class MarketRatesUiState(
    val rates: MarketRates,
    val isRefreshing: Boolean,
    val todayCandlesByType: Map<MarketRateItemType, List<MarketCandle>> = emptyMap()
)
