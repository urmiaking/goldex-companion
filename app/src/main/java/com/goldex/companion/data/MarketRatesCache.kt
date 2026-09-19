package com.goldex.companion.data

import android.content.Context
import android.content.SharedPreferences
import com.goldex.companion.model.MarketRateItemType
import java.util.concurrent.ConcurrentHashMap

/**
 * Two-tier cache for MarketRates and item-specific summaries:
 * - Level 1: In-memory for zero-latency instant access (<1ms)
 * - Level 2: SharedPreferences persistent disk storage for cold-start and offline access
 */
class MarketRatesCache(context: Context? = null) {

    private val prefs: SharedPreferences? = context?.applicationContext?.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    @Volatile
    private var memoryRates: MarketRates? = null

    private val memoryItemSummaries = ConcurrentHashMap<MarketRateItemType, MarketRateItemSummary>()

    fun getRates(): MarketRates? {
        memoryRates?.let { return it }

        val json = prefs?.getString(KEY_RATES, null) ?: return null
        val decoded = PersistenceJsonCodecs.decodeMarketRates(json)
        if (decoded != null) {
            memoryRates = decoded
        }
        return decoded
    }

    fun putRates(rates: MarketRates) {
        memoryRates = rates
        prefs?.let { sp ->
            try {
                val json = PersistenceJsonCodecs.encodeMarketRates(rates)
                sp.edit().putString(KEY_RATES, json).apply()
            } catch (_: Exception) {
                // Disk write failure must never crash the app
            }
        }
    }

    fun getItemSummaries(): Map<MarketRateItemType, MarketRateItemSummary> {
        if (memoryItemSummaries.isNotEmpty()) {
            return memoryItemSummaries.toMap()
        }

        val json = prefs?.getString(KEY_ITEM_SUMMARIES, null) ?: return emptyMap()
        val decoded = PersistenceJsonCodecs.decodeMarketRateItemSummaries(json)
        if (decoded.isNotEmpty()) {
            memoryItemSummaries.putAll(decoded)
        }
        return decoded
    }

    fun putItemSummaries(summaries: Map<MarketRateItemType, MarketRateItemSummary>) {
        if (summaries.isEmpty()) return
        memoryItemSummaries.putAll(summaries)
        prefs?.let { sp ->
            try {
                val json = PersistenceJsonCodecs.encodeMarketRateItemSummaries(summaries)
                sp.edit().putString(KEY_ITEM_SUMMARIES, json).apply()
            } catch (_: Exception) {
                // Disk write failure must never crash the app
            }
        }
    }

    fun clear() {
        memoryRates = null
        memoryItemSummaries.clear()
        prefs?.edit()?.clear()?.apply()
    }

    companion object {
        private const val PREFS_NAME = "goldex_market_rates_cache"
        private const val KEY_RATES = "cached_market_rates"
        private const val KEY_ITEM_SUMMARIES = "cached_item_summaries"

        @Volatile
        private var instance: MarketRatesCache? = null

        fun getInstance(context: Context? = null): MarketRatesCache {
            val existing = instance
            if (existing != null && (context == null || existing.prefs != null)) {
                return existing
            }
            return synchronized(this) {
                val cur = instance
                if (cur != null && (context == null || cur.prefs != null)) {
                    cur
                } else {
                    MarketRatesCache(context).also { instance = it }
                }
            }
        }
    }
}
