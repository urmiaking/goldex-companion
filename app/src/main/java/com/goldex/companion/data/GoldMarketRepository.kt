package com.goldex.companion.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
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

object GoldMarketRepository {
    private val _rates = MutableStateFlow(MarketRates())
    val rates: StateFlow<MarketRates> = _rates.asStateFlow()

    private val _currentSource = MutableStateFlow(PriceSource.ISIGNAL)
    val currentSource: StateFlow<PriceSource> = _currentSource.asStateFlow()

    suspend fun setSource(source: PriceSource) {
        _currentSource.value = source
        refreshRates()
    }

    suspend fun cycleSource(): PriceSource {
        val next = when (_currentSource.value) {
            PriceSource.ISIGNAL -> PriceSource.TALA_IR
            PriceSource.TALA_IR -> PriceSource.TGJU
            PriceSource.TGJU -> PriceSource.ISIGNAL
        }
        setSource(next)
        return next
    }

    suspend fun refreshRates(): MarketRates = withContext(Dispatchers.IO) {
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
}
