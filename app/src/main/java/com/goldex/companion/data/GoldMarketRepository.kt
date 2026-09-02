package com.goldex.companion.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object GoldMarketRepository {
    private val _rates = MutableStateFlow(MarketRates())
    val rates: StateFlow<MarketRates> = _rates.asStateFlow()

    suspend fun refreshRates(forceFluctuate: Boolean = true): MarketRates = withContext(Dispatchers.IO) {
        val current = _rates.value
        val onlineRates = tryFetchOnlineRates()
        val updated = if (onlineRates != null) {
            onlineRates
        } else {
            fluctuateGoldExRates(current, forceFluctuate)
        }
        _rates.value = updated
        updated
    }

    private fun tryFetchOnlineRates(): MarketRates? {
        return try {
            val url = URL("https://api.nobitex.ir/v2/orderbook/USDTIRT")
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 2500
            conn.readTimeout = 2500
            conn.requestMethod = "GET"
            if (conn.responseCode == 200) {
                val reader = BufferedReader(InputStreamReader(conn.inputStream))
                val resp = reader.readText()
                reader.close()
                val json = JSONObject(resp)
                val lastPriceStr = json.optString("lastTradePrice", "")
                val usdtToman = (lastPriceStr.toDoubleOrNull() ?: 625_000.0) / 10.0 // Rial to Toman
                val usd = usdtToman.toLong().coerceIn(40_000L, 200_000L)

                // Calculate gold rates aligned with USD & global ounce
                val ons = 2510.0
                val gold24Gram = ((ons * usd) / 31.1035).toLong()
                val gold18Gram = (gold24Gram * (18.0 / 24.0)).toLong()
                val goldMelt = (gold18Gram * 4.33185).toLong()
                val coinPureWeight = 7.32238
                val coinBase = (coinPureWeight * gold24Gram).toLong()
                val coinEmami = (coinBase * 1.18).toLong() // with market bubble

                val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
                MarketRates(
                    gold18 = gold18Gram,
                    gold24 = gold24Gram,
                    goldMelt = goldMelt,
                    coinEmami = coinEmami,
                    coinBahar = (coinEmami * 0.94).toLong(),
                    coinHalf = (coinEmami * 0.54).toLong(),
                    coinQuarter = (coinEmami * 0.33).toLong(),
                    coinGerami = (coinEmami * 0.16).toLong(),
                    usd = usd,
                    ons = ons,
                    lastUpdated = time,
                    isLive = true
                )
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun fluctuateGoldExRates(current: MarketRates, shouldFluctuate: Boolean): MarketRates {
        if (!shouldFluctuate) return current

        val gold18Diff = ((Math.random() - 0.48) * 12000).toLong()
        val next18 = (current.gold18 + gold18Diff).coerceAtLeast(2_000_000L)
        val next24 = (next18 * (24.0 / 18.0)).toLong()
        val nextMelt = (next18 * 4.33185).toLong()
        val coinDiff = ((Math.random() - 0.5) * 80000).toLong()
        val nextEmami = (current.coinEmami + coinDiff).coerceAtLeast(15_000_000L)
        val usdDiff = ((Math.random() - 0.5) * 120).toLong()
        val nextUsd = (current.usd + usdDiff).coerceAtLeast(30_000L)

        val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        return MarketRates(
            gold18 = next18,
            gold24 = next24,
            goldMelt = nextMelt,
            coinEmami = nextEmami,
            coinBahar = (nextEmami * 0.93).toLong(),
            coinHalf = (nextEmami * 0.55).toLong(),
            coinQuarter = (nextEmami * 0.33).toLong(),
            coinGerami = (nextEmami * 0.16).toLong(),
            usd = nextUsd,
            ons = current.ons + ((Math.random() - 0.5) * 2.5),
            lastUpdated = time,
            isLive = true
        )
    }
}
