package com.goldex.companion.ui.dashboard

import com.goldex.companion.data.AppSettings
import com.goldex.companion.data.MarketRates
import com.goldex.companion.model.TimeHorizon
import com.goldex.companion.model.TrendChartData

data class DashboardUiState(
    val appSettings: AppSettings,
    val rates: MarketRates,
    val savedInvoiceCount: Int,
    val gold18Charts: Map<TimeHorizon, TrendChartData> = emptyMap(),
    val isGold18Loading: Boolean = false
)
