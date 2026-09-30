package com.goldex.companion.data

import com.goldex.companion.model.MarketCandle
import com.goldex.companion.model.MarketRateItemType
import com.goldex.companion.model.TimeHorizon
import com.goldex.companion.model.TrendChartData
import kotlinx.coroutines.flow.StateFlow

/** Read-only local market snapshots. Loading these must not fetch from the network. */
interface MarketCacheReader {
    fun getCachedRates(): MarketRates?
    fun getCachedAllHorizonsHistory(type: MarketRateItemType): Map<TimeHorizon, List<MarketCandle>>
    fun getCachedTodayCandlesForBoard(): Map<MarketRateItemType, List<MarketCandle>>
    fun getCachedDashboardGold18Charts(): Map<TimeHorizon, TrendChartData>
}

/** Restores the selected provider without triggering a refresh during startup. */
interface MarketSourceInitializer {
    fun setSourceSilently(source: PriceSource)
}

enum class ConnectionStatus { ONLINE, CONNECTING, OFFLINE }

interface ConnectivityObserver {
    val status: StateFlow<ConnectionStatus>
}
