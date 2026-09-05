package com.goldex.companion.ui.dashboard

import com.goldex.companion.data.AppSettings
import com.goldex.companion.data.MarketRates

data class DashboardUiState(
    val appSettings: AppSettings,
    val rates: MarketRates,
    val savedInvoiceCount: Int
)
