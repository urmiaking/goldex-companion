package com.goldex.companion.data

import kotlinx.coroutines.Dispatchers
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

object GoldMarketRepository {
    private val _rates = MutableStateFlow(MarketRates())
    val rates: StateFlow<MarketRates> = _rates.asStateFlow()

    private val _currentSource = MutableStateFlow(PriceSource.ISIGNAL)
    val currentSource: StateFlow<PriceSource> = _currentSource.asStateFlow()

    suspend fun setSource(source: PriceSource) {
        _currentSource.value = source
        refreshRates()
    }

    suspend fun refreshRates(): MarketRates = withContext(Dispatchers.IO) {
        val preferred = _currentSource.value
        val result = if (preferred == PriceSource.ISIGNAL) {
            fetchFromISignal() ?: fetchFromTalaIr()
        } else {
            fetchFromTalaIr() ?: fetchFromISignal()
        }

        val finalRates = result ?: _rates.value.copy(
            lastUpdated = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date()),
            isLive = false
        )
        _rates.value = finalRates
        finalRates
    }

    fun fetchFromISignal(): MarketRates? {
        return try {
            val url = URL("https://signalpardazgroup.com/service/signalData@4.0.0/list")
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 5000
            conn.readTimeout = 5000
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
                        coinEmami = if (emami > 0L) emami else 228_000_000L,
                        coinBahar = if (bahar > 0L) bahar else (emami * 0.98).toLong(),
                        coinHalf = if (nim > 0L) nim else (emami * 0.51).toLong(),
                        coinQuarter = if (rob > 0L) rob else (emami * 0.28).toLong(),
                        coinGerami = if (gerami > 0L) gerami else (emami * 0.14).toLong(),
                        usd = if (usd > 0L) usd else 85_000L,
                        ons = 2500.0,
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
            conn.connectTimeout = 5000
            conn.readTimeout = 5000
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
                    return if (num > 50_000_000L && (key.contains("18k") || key.contains("24k"))) num / 10L else num
                }

                val g18 = parsePrice(goldObj, "gold_18k")
                val g24 = parsePrice(goldObj, "gold_24k")
                val melt = parsePrice(goldObj, "gold_bazartehran")
                val onsStr = goldObj.optJSONObject("gold_ounce")?.optString("v", "2500") ?: "2500"
                val ons = onsStr.replace(",", "").toDoubleOrNull() ?: 2500.0

                val emami = parsePrice(sekkeObj, "sekke-jad")
                val bahar = parsePrice(sekkeObj, "sekke-gad")
                val nim = parsePrice(sekkeObj, "sekke-nim")
                val rob = parsePrice(sekkeObj, "sekke-rob")
                val gerami = parsePrice(sekkeObj, "sekke-grm")
                val usd = parsePrice(arzObj, "arz_dolar")

                if (g18 > 0L) {
                    val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
                    MarketRates(
                        gold18 = g18,
                        gold24 = if (g24 > 0L) g24 else (g18 * 24.0 / 18.0).toLong(),
                        goldMelt = if (melt > 0L) melt else (g18 * 4.33185).toLong(),
                        coinEmami = if (emami > 0L) emami else 228_000_000L,
                        coinBahar = if (bahar > 0L) bahar else (emami * 0.98).toLong(),
                        coinHalf = if (nim > 0L) nim else (emami * 0.51).toLong(),
                        coinQuarter = if (rob > 0L) rob else (emami * 0.28).toLong(),
                        coinGerami = if (gerami > 0L) gerami else (emami * 0.14).toLong(),
                        usd = if (usd > 0L) usd else 85_000L,
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
}
