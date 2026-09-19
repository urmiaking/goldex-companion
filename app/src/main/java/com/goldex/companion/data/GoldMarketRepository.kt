package com.goldex.companion.data

import android.content.Context
import com.goldex.companion.model.MarketCandle
import com.goldex.companion.model.MarketHistoryConverter
import com.goldex.companion.model.MarketRateItemType
import com.goldex.companion.model.TimeHorizon
import com.goldex.companion.model.TrendChartData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

object GoldMarketRepository : MarketRatesStore, MarketHistoryStore {
    private var historyCache = MarketHistoryCache.getInstance()
    private var ratesCache: MarketRatesCache? = null

    private val _rates = MutableStateFlow(MarketRates())
    override val rates: StateFlow<MarketRates> = _rates.asStateFlow()

    private val _currentSource = MutableStateFlow(PriceSource.ISIGNAL)
    override val currentSource: StateFlow<PriceSource> = _currentSource.asStateFlow()

    fun init(context: Context) {
        val appContext = context.applicationContext
        historyCache = MarketHistoryCache.getInstance(appContext)
        ratesCache = MarketRatesCache.getInstance(appContext)

        // Immediately populate cached rates if available
        ratesCache?.getRates()?.let { cached ->
            if (cached.gold18 > 0L) {
                _rates.value = cached.copy(isLive = false)
            }
        }
    }

    fun getCachedRates(): MarketRates? {
        return ratesCache?.getRates() ?: if (_rates.value.gold18 > 0L) _rates.value else null
    }

    fun getCachedAllHorizonsHistory(type: MarketRateItemType): Map<TimeHorizon, List<MarketCandle>> {
        return historyCache.getAllCachedHorizons(type)
    }

    fun getCachedTodayCandlesForBoard(): Map<MarketRateItemType, List<MarketCandle>> {
        return historyCache.getCachedTodayCandles()
    }

    fun getCachedDashboardGold18Charts(): Map<TimeHorizon, TrendChartData> {
        val basePrice = _rates.value.gold18
        val history = historyCache.getAllCachedHorizons(MarketRateItemType.GOLD_18K)
        return history.mapValues { (horizon, candles) ->
            MarketHistoryConverter.toTrendChartData(candles, horizon, basePrice)
        }
    }

    fun getCachedItemSummaries(): Map<MarketRateItemType, MarketRateItemSummary> {
        return ratesCache?.getItemSummaries() ?: emptyMap()
    }

    fun putCachedItemSummaries(summaries: Map<MarketRateItemType, MarketRateItemSummary>) {
        ratesCache?.putItemSummaries(summaries)
    }

    fun setSourceSilently(source: PriceSource) {
        _currentSource.value = source
    }

    override suspend fun setSource(source: PriceSource) {
        _currentSource.value = source
        refreshRates()
    }

    override suspend fun cycleSource(): PriceSource {
        val next = when (_currentSource.value) {
            PriceSource.ISIGNAL -> PriceSource.TALA_IR
            PriceSource.TALA_IR -> PriceSource.TGJU
            PriceSource.TGJU -> PriceSource.ISIGNAL
        }
        setSource(next)
        return next
    }

    override suspend fun refreshRates(): MarketRates = withContext(Dispatchers.IO) {
        val preferred = _currentSource.value
        val result = when (preferred) {
            PriceSource.ISIGNAL -> fetchFromISignal() ?: fetchFromTgju() ?: fetchFromTalaIr()
            PriceSource.TALA_IR -> fetchFromTalaIr() ?: fetchFromTgju() ?: fetchFromISignal()
            PriceSource.TGJU -> fetchFromTgju() ?: fetchFromISignal() ?: fetchFromTalaIr()
        }

        val finalRates = result ?: _rates.value.copy(
            lastUpdated = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date()),
            isLive = false
        )
        _rates.value = finalRates
        if (result != null) {
            ratesCache?.putRates(finalRates)
        }
        finalRates
    }

    fun fetchFromISignal(): MarketRates? {
        return try {
            val url = URL("https://signalpardazgroup.com/service/signalData@4.0.0/list")
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 6000
            conn.readTimeout = 6000
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Android; Mobile)")
            conn.doOutput = true

            val payload = """
                [
                    {
                        "market": "gold",
                        "filterName": "gold",
                        "property": ["name", "change", "close", "iconUrl", "id", "index", "jDate", "persianName", "time", "percentChange", "unit"]
                    },
                    {
                        "market": "coin",
                        "filterName": "coin",
                        "property": ["name", "change", "close", "iconUrl", "id", "index", "jDate", "persianName", "time", "percentChange", "unit"],
                        "filterLists": [[{"field": "subCategory", "include": false, "opt": "e", "values": ["coinParsian", "coinBubble"]}]]
                    },
                    {
                        "market": "currency",
                        "filterName": "freeCurrency",
                        "property": ["name", "change", "close", "iconUrl", "id", "index", "jDate", "persianName", "time", "percentChange", "unit"],
                        "filterLists": [[{"field": "subCategory", "include": true, "opt": "e", "values": ["free"]}]]
                    }
                ]
            """.trimIndent()

            OutputStreamWriter(conn.outputStream, "UTF-8").use { writer ->
                writer.write(payload)
                writer.flush()
            }

            if (conn.responseCode == 200) {
                val reader = BufferedReader(InputStreamReader(conn.inputStream, "UTF-8"))
                val resp = reader.readText()
                reader.close()

                val root = JSONObject(resp)
                val dataObj = root.optJSONObject("data") ?: return null

                var g18 = 0L
                var g24 = 0L
                var melt = 0L
                var emami = 0L
                var bahar = 0L
                var nim = 0L
                var rob = 0L
                var gerami = 0L
                var usd = 0L

                val goldArr = dataObj.optJSONObject("gold")?.optJSONArray("data")
                if (goldArr != null) {
                    for (i in 0 until goldArr.length()) {
                        val item = goldArr.getJSONObject(i)
                        val name = item.optString("name")
                        val closeRials = item.optLong("close", 0L)
                        val closeToman = closeRials / 10L
                        when (name) {
                            "geram18" -> g18 = closeToman
                            "geram24" -> g24 = closeToman
                            "mazanne" -> melt = closeToman
                        }
                    }
                }

                val coinArr = dataObj.optJSONObject("coin")?.optJSONArray("data")
                if (coinArr != null) {
                    for (i in 0 until coinArr.length()) {
                        val item = coinArr.getJSONObject(i)
                        val name = item.optString("name")
                        val closeRials = item.optLong("close", 0L)
                        val closeToman = closeRials / 10L
                        when (name) {
                            "sekeEmam" -> emami = closeToman
                            "sekeBaharAzadi" -> bahar = closeToman
                            "nim" -> nim = closeToman
                            "rob" -> rob = closeToman
                            "gerami" -> gerami = closeToman
                        }
                    }
                }

                val curArr = dataObj.optJSONObject("freeCurrency")?.optJSONArray("data")
                if (curArr != null) {
                    for (i in 0 until curArr.length()) {
                        val item = curArr.getJSONObject(i)
                        val name = item.optString("name")
                        val closeRials = item.optLong("close", 0L)
                        if (name == "usDollar") {
                            usd = closeRials / 10L
                        }
                    }
                }

                if (g18 > 0L) {
                    val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
                    MarketRates(
                        gold18 = g18,
                        gold24 = if (g24 > 0L) g24 else (g18 * 24.0 / 18.0).toLong(),
                        goldMelt = if (melt > 0L) melt else (g18 * 4.33185).toLong(),
                        coinEmami = if (emami > 0L) emami else 234_000_000L,
                        coinBahar = if (bahar > 0L) bahar else (emami * 0.98).toLong(),
                        coinHalf = if (nim > 0L) nim else (emami * 0.51).toLong(),
                        coinQuarter = if (rob > 0L) rob else (emami * 0.28).toLong(),
                        coinGerami = if (gerami > 0L) gerami else (emami * 0.14).toLong(),
                        usd = if (usd > 0L) usd else 221_500L,
                        ons = 4435.0,
                        lastUpdated = time,
                        source = PriceSource.ISIGNAL,
                        isLive = true
                    )
                } else null
            } else null
        } catch (e: Exception) {
            null
        }
    }

    fun fetchFromTalaIr(): MarketRates? {
        return try {
            val url = URL("https://www.tala.ir/ajax/price")
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 6000
            conn.readTimeout = 6000
            conn.requestMethod = "GET"
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")

            if (conn.responseCode == 200) {
                val reader = BufferedReader(InputStreamReader(conn.inputStream, "UTF-8"))
                val resp = reader.readText()
                reader.close()

                val root = JSONObject(resp)
                val goldObj = root.optJSONObject("gold") ?: return null
                val sekkeObj = root.optJSONObject("sekke")
                val arzObj = root.optJSONObject("arz")

                fun parsePrice(obj: JSONObject?, key: String): Long {
                    val vStr = obj?.optJSONObject(key)?.optString("v", "") ?: ""
                    val clean = vStr.replace(",", "").replace("-", "").trim()
                    val num = clean.toLongOrNull() ?: 0L
                    return abs(num)
                }

                val g18 = parsePrice(goldObj, "gold_18k")
                val g24 = parsePrice(goldObj, "gold_24k")
                val melt = parsePrice(goldObj, "gold_bazartehran")
                val onsStr = goldObj.optJSONObject("gold_ounce")?.optString("v", "4435") ?: "4435"
                val ons = abs(onsStr.replace(",", "").toDoubleOrNull() ?: 4435.0)

                val emami = parsePrice(sekkeObj, "sekke-jad")
                val bahar = parsePrice(sekkeObj, "sekke-gad")
                val nim = parsePrice(sekkeObj, "sekke-nim")
                val rob = parsePrice(sekkeObj, "sekke-rob")
                val gerami = parsePrice(sekkeObj, "sekke-grm")
                
                var usd = parsePrice(arzObj, "arz_dolar")
                if (usd <= 0L) {
                    val derham = parsePrice(arzObj, "arz_derham")
                    if (derham > 0L) {
                        usd = (derham * 3.6725).toLong()
                    }
                }
                if (usd <= 0L) {
                    usd = 221_500L
                }

                if (g18 > 0L) {
                    val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
                    MarketRates(
                        gold18 = g18,
                        gold24 = if (g24 > 0L) g24 else (g18 * 24.0 / 18.0).toLong(),
                        goldMelt = if (melt > 0L) melt else (g18 * 4.33185).toLong(),
                        coinEmami = if (emami > 0L) emami else 234_000_000L,
                        coinBahar = if (bahar > 0L) bahar else (emami * 0.98).toLong(),
                        coinHalf = if (nim > 0L) nim else (emami * 0.51).toLong(),
                        coinQuarter = if (rob > 0L) rob else (emami * 0.28).toLong(),
                        coinGerami = if (gerami > 0L) gerami else (emami * 0.14).toLong(),
                        usd = usd,
                        ons = ons,
                        lastUpdated = time,
                        source = PriceSource.TALA_IR,
                        isLive = true
                    )
                } else null
            } else null
        } catch (e: Exception) {
            null
        }
    }

    fun fetchFromTgju(): MarketRates? {
        return try {
            val url = URL("https://call3.tgju.org/ajax.json")
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 6000
            conn.readTimeout = 6000
            conn.requestMethod = "GET"
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")

            if (conn.responseCode == 200) {
                val reader = BufferedReader(InputStreamReader(conn.inputStream, "UTF-8"))
                val resp = reader.readText()
                reader.close()

                val root = JSONObject(resp)
                val current = root.optJSONObject("current") ?: return null

                fun parseItemPrice(key: String, isRials: Boolean = true): Long {
                    val pStr = current.optJSONObject(key)?.optString("p", "") ?: ""
                    val clean = pStr.replace(",", "").replace("-", "").trim()
                    val raw = clean.toLongOrNull() ?: 0L
                    val absVal = abs(raw)
                    return if (isRials && absVal > 0L) absVal / 10L else absVal
                }

                val usd = parseItemPrice("price_dollar_rl", isRials = true)
                val g18 = parseItemPrice("geram18", isRials = true)
                val melt = parseItemPrice("mesghal", isRials = true)
                val emami = parseItemPrice("sekee", isRials = true)

                val onsStr = current.optJSONObject("ons")?.optString("p", "4435") ?: "4435"
                val ons = abs(onsStr.replace(",", "").toDoubleOrNull() ?: 4435.0)

                if (g18 > 0L) {
                    val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
                    MarketRates(
                        gold18 = g18,
                        gold24 = (g18 * 24.0 / 18.0).toLong(),
                        goldMelt = if (melt > 0L) melt else (g18 * 4.33185).toLong(),
                        coinEmami = if (emami > 0L) emami else 234_000_000L,
                        coinBahar = (emami * 0.98).toLong(),
                        coinHalf = (emami * 0.51).toLong(),
                        coinQuarter = (emami * 0.28).toLong(),
                        coinGerami = (emami * 0.14).toLong(),
                        usd = if (usd > 0L) usd else 221_500L,
                        ons = ons,
                        lastUpdated = time,
                        source = PriceSource.TGJU,
                        isLive = true
                    )
                } else null
            } else null
        } catch (e: Exception) {
            null
        }
    }

    // =========================================================================
    // Market History Store Implementation (Multi-Provider, Caching & Fallback)
    // =========================================================================

    override suspend fun getHistory(
        type: MarketRateItemType,
        horizon: TimeHorizon,
        preferredSource: PriceSource
    ): List<MarketCandle> = withContext(Dispatchers.IO) {
        // 1. Return fresh data from cache if available
        val freshCached = historyCache.get(type, horizon, allowStale = false)
        if (freshCached != null && freshCached.isNotEmpty()) {
            return@withContext freshCached
        }

        // 2. Fetch using multi-provider fallback strategy
        var effectiveSource = preferredSource
        val candles = when (preferredSource) {
            PriceSource.ISIGNAL -> {
                // iSignal has no ONS history; fallback directly to TGJU for ONS
                if (type == MarketRateItemType.ONS) {
                    effectiveSource = PriceSource.TGJU
                    fetchHistoryFromTgju(type, horizon)
                } else {
                    val isignalData = fetchHistoryFromISignal(type, horizon)
                    if (isignalData != null && isignalData.isNotEmpty()) {
                        isignalData
                    } else {
                        effectiveSource = PriceSource.TGJU
                        fetchHistoryFromTgju(type, horizon)
                    }
                }
            }
            PriceSource.TGJU -> {
                val tgjuData = fetchHistoryFromTgju(type, horizon)
                if (tgjuData != null && tgjuData.isNotEmpty()) {
                    tgjuData
                } else {
                    effectiveSource = PriceSource.ISIGNAL
                    fetchHistoryFromISignal(type, horizon)
                }
            }
            PriceSource.TALA_IR -> {
                // Tala.ir does not offer a public historical OHLC API; fallback to TGJU then iSignal
                effectiveSource = PriceSource.TGJU
                fetchHistoryFromTgju(type, horizon) ?: run {
                    effectiveSource = PriceSource.ISIGNAL
                    fetchHistoryFromISignal(type, horizon)
                }
            }
        }

        // 3. Persist successful result in cache
        if (candles != null && candles.isNotEmpty()) {
            historyCache.put(type, horizon, effectiveSource, candles)
            return@withContext candles
        }

        // 4. Offline Fallback: Return stale cache if available, or empty list
        historyCache.get(type, horizon, allowStale = true) ?: emptyList()
    }

    override suspend fun getAllHorizonsHistory(
        type: MarketRateItemType,
        preferredSource: PriceSource
    ): Map<TimeHorizon, List<MarketCandle>> = coroutineScope {
        val todayDeferred = async(Dispatchers.IO) { getHistory(type, TimeHorizon.TODAY, preferredSource) }
        val weekDeferred = async(Dispatchers.IO) { getHistory(type, TimeHorizon.ONE_WEEK, preferredSource) }
        val monthDeferred = async(Dispatchers.IO) { getHistory(type, TimeHorizon.ONE_MONTH, preferredSource) }
        val sixMonthsDeferred = async(Dispatchers.IO) { getHistory(type, TimeHorizon.SIX_MONTHS, preferredSource) }
        val yearDeferred = async(Dispatchers.IO) { getHistory(type, TimeHorizon.ONE_YEAR, preferredSource) }

        mapOf(
            TimeHorizon.TODAY to todayDeferred.await(),
            TimeHorizon.ONE_WEEK to weekDeferred.await(),
            TimeHorizon.ONE_MONTH to monthDeferred.await(),
            TimeHorizon.SIX_MONTHS to sixMonthsDeferred.await(),
            TimeHorizon.ONE_YEAR to yearDeferred.await()
        )
    }

    fun fetchHistoryFromISignal(type: MarketRateItemType, horizon: TimeHorizon): List<MarketCandle>? {
        val (market, symbolId) = getISignalParams(type) ?: return null
        val rangeKey = getISignalRangeKey(horizon)
        var connection: HttpURLConnection? = null
        return try {
            val url = URL("https://signalpardazgroup.com/service/signalData@4.0.0/history?market=$market&symbolId=$symbolId&rangeKey=$rangeKey")
            connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 6000
                readTimeout = 6000
                requestMethod = "GET"
                setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                setRequestProperty("Accept", "application/json")
            }

            if (connection.responseCode == 200) {
                val responseText = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                val root = JSONObject(responseText)
                val dataObj = root.optJSONObject("data") ?: return null
                val valueArr = dataObj.optJSONArray("value") ?: return null

                val candles = ArrayList<MarketCandle>(valueArr.length())
                for (i in 0 until valueArr.length()) {
                    val item = valueArr.optJSONObject(i) ?: continue
                    val openRials = item.optLong("open", 0L)
                    val highRials = item.optLong("high", 0L)
                    val lowRials = item.optLong("low", 0L)
                    val closeRials = item.optLong("close", 0L)
                    val time = item.optString("time", "")

                    candles.add(
                        MarketCandle(
                            open = openRials / 10L,
                            high = highRials / 10L,
                            low = lowRials / 10L,
                            close = closeRials / 10L,
                            dateShamsi = time
                        )
                    )
                }

                // iSignal returns newest items first; reverse to chronological order (past to present)
                candles.reversed()
            } else null
        } catch (_: Exception) {
            null
        } finally {
            connection?.disconnect()
        }
    }

    fun fetchHistoryFromTgju(type: MarketRateItemType, horizon: TimeHorizon): List<MarketCandle>? {
        val indicator = getTgjuIndicator(type)
        return if (horizon == TimeHorizon.TODAY) {
            fetchTgjuTodayIntraday(indicator, type)
                ?: fetchTgjuSummary(indicator, type, length = 2)
        } else {
            val length = getTgjuLength(horizon)
            fetchTgjuSummary(indicator, type, length)
        }
    }

    private fun fetchTgjuTodayIntraday(indicator: String, type: MarketRateItemType): List<MarketCandle>? {
        var connection: HttpURLConnection? = null
        return try {
            val url = URL("https://api.tgju.org/v1/market/indicator/today-table-data/$indicator")
            connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 6000
                readTimeout = 6000
                requestMethod = "GET"
                setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                setRequestProperty("Accept", "application/json")
            }

            if (connection.responseCode == 200) {
                val responseText = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                val root = JSONObject(responseText)
                val dataArr = root.optJSONArray("data") ?: return null
                if (dataArr.length() == 0) return null

                val candles = ArrayList<MarketCandle>(dataArr.length())
                val divisor = if (type == MarketRateItemType.ONS) 1L else 10L
                for (i in 0 until dataArr.length()) {
                    val row = dataArr.optJSONArray(i) ?: continue
                    val rawPrice = parseCleanPrice(row.optString(0, ""))
                    val timeStr = row.optString(1, "")
                    if (rawPrice > 0L) {
                        val priceToman = rawPrice / divisor
                        candles.add(
                            MarketCandle(
                                open = priceToman,
                                high = priceToman,
                                low = priceToman,
                                close = priceToman,
                                dateShamsi = timeStr
                            )
                        )
                    }
                }
                if (candles.isNotEmpty()) candles.reversed() else null
            } else null
        } catch (_: Exception) {
            null
        } finally {
            connection?.disconnect()
        }
    }

    private fun fetchTgjuSummary(indicator: String, type: MarketRateItemType, length: Int): List<MarketCandle>? {
        var connection: HttpURLConnection? = null
        return try {
            val url = URL("https://api.tgju.org/v1/market/indicator/summary-table-data/$indicator?start=0&length=$length")
            connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 6000
                readTimeout = 6000
                requestMethod = "GET"
                setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                setRequestProperty("Accept", "application/json")
            }

            if (connection.responseCode == 200) {
                val responseText = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                val root = JSONObject(responseText)
                val dataArr = root.optJSONArray("data") ?: return null

                val candles = ArrayList<MarketCandle>(dataArr.length())
                val divisor = if (type == MarketRateItemType.ONS) 1L else 10L
                for (i in 0 until dataArr.length()) {
                    val row = dataArr.optJSONArray(i) ?: continue
                    val openVal = parseCleanPrice(row.optString(0, "")) / divisor
                    val lowVal = parseCleanPrice(row.optString(1, "")) / divisor
                    val highVal = parseCleanPrice(row.optString(2, "")) / divisor
                    val closeVal = parseCleanPrice(row.optString(3, "")) / divisor
                    val gregorian = row.optString(6, "")
                    val shamsi = row.optString(7, "")

                    if (closeVal > 0L) {
                        candles.add(
                            MarketCandle(
                                open = if (openVal > 0L) openVal else closeVal,
                                high = if (highVal > 0L) highVal else closeVal,
                                low = if (lowVal > 0L) lowVal else closeVal,
                                close = closeVal,
                                dateShamsi = shamsi,
                                dateGregorian = gregorian
                            )
                        )
                    }
                }
                candles.reversed()
            } else null
        } catch (_: Exception) {
            null
        } finally {
            connection?.disconnect()
        }
    }

    private fun getISignalParams(type: MarketRateItemType): Pair<String, String>? {
        return when (type) {
            MarketRateItemType.GOLD_18K -> "gold" to "100011"
            MarketRateItemType.GOLD_MELT -> "gold" to "100013"
            MarketRateItemType.GOLD_24K -> "gold" to "100012"
            MarketRateItemType.COIN_EMAMI -> "coin" to "100001"
            MarketRateItemType.COIN_BAHAR -> "coin" to "100000"
            MarketRateItemType.COIN_HALF -> "coin" to "100002"
            MarketRateItemType.COIN_QUARTER -> "coin" to "100003"
            MarketRateItemType.COIN_GERAMI -> "coin" to "100004"
            MarketRateItemType.USD -> "currency" to "200000"
            MarketRateItemType.ONS -> null
        }
    }

    private fun getISignalRangeKey(horizon: TimeHorizon): String {
        return when (horizon) {
            TimeHorizon.TODAY -> "oneDay"
            TimeHorizon.ONE_WEEK -> "oneWeek"
            TimeHorizon.ONE_MONTH -> "oneMonth"
            TimeHorizon.SIX_MONTHS -> "sixMonth"
            TimeHorizon.ONE_YEAR -> "oneYear"
        }
    }

    private fun getTgjuIndicator(type: MarketRateItemType): String {
        return when (type) {
            MarketRateItemType.GOLD_18K -> "geram18"
            MarketRateItemType.GOLD_MELT -> "mesghal"
            MarketRateItemType.GOLD_24K -> "geram24"
            MarketRateItemType.COIN_EMAMI -> "sekee"
            MarketRateItemType.COIN_BAHAR -> "sekeb"
            MarketRateItemType.COIN_HALF -> "nim"
            MarketRateItemType.COIN_QUARTER -> "rob"
            MarketRateItemType.COIN_GERAMI -> "gerami"
            MarketRateItemType.USD -> "price_dollar_rl"
            MarketRateItemType.ONS -> "ons"
        }
    }

    private fun getTgjuLength(horizon: TimeHorizon): Int {
        return when (horizon) {
            TimeHorizon.TODAY -> 2
            TimeHorizon.ONE_WEEK -> 7
            TimeHorizon.ONE_MONTH -> 30
            TimeHorizon.SIX_MONTHS -> 180
            TimeHorizon.ONE_YEAR -> 365
        }
    }

    private fun parseCleanPrice(raw: String): Long {
        val clean = raw.replace(",", "").replace("-", "").trim()
        val num = clean.toDoubleOrNull() ?: 0.0
        return abs(num).toLong()
    }
}

