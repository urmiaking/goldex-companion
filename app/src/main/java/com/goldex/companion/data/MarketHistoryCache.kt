package com.goldex.companion.data

import android.content.Context
import android.content.SharedPreferences
import com.goldex.companion.model.MarketCandle
import com.goldex.companion.model.MarketRateItemType
import com.goldex.companion.model.TimeHorizon
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap

/**
 * High-performance 2-tier cache for market history data:
 * - Level 1: In-memory ConcurrentHashMap for instantaneous UI switches (<1ms)
 * - Level 2: SharedPreferences persistent disk cache for offline access and cold starts
 */
class MarketHistoryCache(context: Context? = null) {

    private val prefs: SharedPreferences? = context?.applicationContext?.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    private data class CacheEntry(
        val timestamp: Long,
        val source: PriceSource,
        val candles: List<MarketCandle>
    )

    private val memoryCache = ConcurrentHashMap<String, CacheEntry>()

    fun get(type: MarketRateItemType, horizon: TimeHorizon, allowStale: Boolean = true): List<MarketCandle>? {
        val key = buildKey(type, horizon)

        // 1. Try Memory Cache
        memoryCache[key]?.let { entry ->
            val isFresh = (System.currentTimeMillis() - entry.timestamp) <= getTtlMs(horizon)
            if (isFresh || allowStale) {
                return entry.candles
            }
        }

        // 2. Try Disk (SharedPreferences) Cache
        val jsonStr = prefs?.getString(key, null) ?: return null
        return try {
            val root = JSONObject(jsonStr)
            val ts = root.optLong("timestamp", 0L)
            val sourceStr = root.optString("source", PriceSource.ISIGNAL.name)
            val source = try { PriceSource.valueOf(sourceStr) } catch (_: Exception) { PriceSource.ISIGNAL }
            val arr = root.optJSONArray("candles") ?: return null

            val candles = ArrayList<MarketCandle>(arr.length())
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                candles.add(
                    MarketCandle(
                        open = obj.optLong("open", 0L),
                        high = obj.optLong("high", 0L),
                        low = obj.optLong("low", 0L),
                        close = obj.optLong("close", 0L),
                        dateShamsi = obj.optString("dateShamsi", ""),
                        dateGregorian = obj.optString("dateGregorian", "")
                    )
                )
            }

            if (candles.isNotEmpty()) {
                val entry = CacheEntry(ts, source, candles)
                memoryCache[key] = entry
                val isFresh = (System.currentTimeMillis() - ts) <= getTtlMs(horizon)
                if (isFresh || allowStale) candles else null
            } else null
        } catch (_: Exception) {
            null
        }
    }

    fun put(type: MarketRateItemType, horizon: TimeHorizon, source: PriceSource, candles: List<MarketCandle>) {
        if (candles.isEmpty()) return
        val key = buildKey(type, horizon)
        val now = System.currentTimeMillis()

        // 1. Update In-Memory Cache
        memoryCache[key] = CacheEntry(now, source, candles)

        // 2. Persist to Disk Asynchronously
        prefs?.let { sp ->
            try {
                val root = JSONObject()
                root.put("timestamp", now)
                root.put("source", source.name)

                val arr = JSONArray()
                candles.forEach { c ->
                    val obj = JSONObject()
                    obj.put("open", c.open)
                    obj.put("high", c.high)
                    obj.put("low", c.low)
                    obj.put("close", c.close)
                    obj.put("dateShamsi", c.dateShamsi)
                    obj.put("dateGregorian", c.dateGregorian)
                    arr.put(obj)
                }
                root.put("candles", arr)

                sp.edit().putString(key, root.toString()).apply()
            } catch (_: Exception) {
                // Disk write failure must never crash the app
            }
        }
    }

    fun isFresh(type: MarketRateItemType, horizon: TimeHorizon): Boolean {
        val key = buildKey(type, horizon)
        val entry = memoryCache[key]
        if (entry != null) {
            return (System.currentTimeMillis() - entry.timestamp) <= getTtlMs(horizon)
        }
        val jsonStr = prefs?.getString(key, null) ?: return false
        return try {
            val root = JSONObject(jsonStr)
            val ts = root.optLong("timestamp", 0L)
            (System.currentTimeMillis() - ts) <= getTtlMs(horizon)
        } catch (_: Exception) {
            false
        }
    }

    private fun buildKey(type: MarketRateItemType, horizon: TimeHorizon): String {
        return "hist_${type.name}_${horizon.name}"
    }

    private fun getTtlMs(horizon: TimeHorizon): Long {
        return when (horizon) {
            TimeHorizon.TODAY -> 10 * 60 * 1000L      // 10 minutes
            TimeHorizon.ONE_WEEK -> 30 * 60 * 1000L   // 30 minutes
            TimeHorizon.ONE_MONTH -> 2 * 3600 * 1000L // 2 hours
            TimeHorizon.SIX_MONTHS -> 6 * 3600 * 1000L// 6 hours
            TimeHorizon.ONE_YEAR -> 12 * 3600 * 1000L // 12 hours
        }
    }

    /**
     * Synchronously retrieves all available cached horizons for a rate item.
     * Hits memory first (<1ms), then disk (<2ms). Returns empty list for horizons without cache.
     */
    fun getAllCachedHorizons(type: MarketRateItemType): Map<TimeHorizon, List<MarketCandle>> {
        return TimeHorizon.values().associateWith { horizon ->
            get(type, horizon, allowStale = true) ?: emptyList()
        }
    }

    /**
     * Synchronously retrieves all available TODAY candles for the provided rate item types.
     */
    fun getCachedTodayCandles(types: Array<MarketRateItemType> = MarketRateItemType.values()): Map<MarketRateItemType, List<MarketCandle>> {
        val result = mutableMapOf<MarketRateItemType, List<MarketCandle>>()
        for (type in types) {
            val candles = get(type, TimeHorizon.TODAY, allowStale = true)
            if (!candles.isNullOrEmpty()) {
                result[type] = candles
            }
        }
        return result
    }

    fun clear() {
        memoryCache.clear()
        prefs?.edit()?.clear()?.apply()
    }

    companion object {
        private const val PREFS_NAME = "goldex_market_history_cache"

        @Volatile
        private var instance: MarketHistoryCache? = null

        fun getInstance(context: Context? = null): MarketHistoryCache {
            val existing = instance
            if (existing != null && (context == null || existing.prefs != null)) {
                return existing
            }
            return synchronized(this) {
                val cur = instance
                if (cur != null && (context == null || cur.prefs != null)) {
                    cur
                } else {
                    MarketHistoryCache(context).also { instance = it }
                }
            }
        }

        fun init(context: Context): MarketHistoryCache {
            return getInstance(context)
        }
    }
}
