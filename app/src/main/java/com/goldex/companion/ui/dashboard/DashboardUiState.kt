package com.goldex.companion.ui.dashboard

import com.goldex.companion.data.AppSettings
import com.goldex.companion.data.MarketRates
import com.goldex.companion.data.license.LicenseInfo
import com.goldex.companion.model.InvoiceListItem
import com.goldex.companion.model.TimeHorizon
import com.goldex.companion.model.TrendChartData

data class DashboardUiState(
    val appSettings: AppSettings,
    val rates: MarketRates,
    val savedInvoiceCount: Int,
    val gold18Charts: Map<TimeHorizon, TrendChartData> = emptyMap(),
    val isGold18Loading: Boolean = false,
    val licenseInfo: LicenseInfo = LicenseInfo(),
    val totalInventoryWeight18k: Double = 0.0,
    val totalInventoryValuationTomans: Long = 0L,
    val recentInvoices: List<InvoiceListItem> = emptyList()
)
