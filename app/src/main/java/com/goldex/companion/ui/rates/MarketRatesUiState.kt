package com.goldex.companion.ui.rates

import com.goldex.companion.data.MarketRates

data class MarketRatesUiState(
    val rates: MarketRates,
    val isRefreshing: Boolean
)
