package com.goldex.companion.desktop.data

import com.goldex.companion.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

interface DesktopMarketGateway : MarketRatesStore {
    val snapshot: StateFlow<MarketSnapshot?>
    fun useManual(rates: MarketRates)
}

/** Actual missing quotes stay zero/unavailable; no synthetic coin or currency prices. */
class DesktopMarketRepository(
    private val storage: DesktopDataStore,
    private val fetch: (PriceSource) -> MarketRates = ::fetchProvider,
    private val clock: () -> Long = System::currentTimeMillis
) : DesktopMarketGateway {
    private val initial = storage.cachedMarket()?.let { if (it.kind == QuoteKind.ONLINE) it.copy(kind = QuoteKind.CACHED, rates = it.rates.copy(isLive = false)) else it }
    private val mutableSnapshot = MutableStateFlow(initial)
    override val snapshot = mutableSnapshot.asStateFlow()
    private val mutableRates = MutableStateFlow(initial?.rates ?: emptyRates())
    override val rates = mutableRates.asStateFlow()
    private val mutableSource = MutableStateFlow(storage.loadSettings().priceSource)
    override val currentSource = mutableSource.asStateFlow()
    private val mutex = Mutex()

    override suspend fun setSource(source: PriceSource) { mutableSource.value = source; refreshRates() }
    override suspend fun cycleSource(): PriceSource = PriceSource.values().let { all -> all[(all.indexOf(currentSource.value) + 1) % all.size] }.also { setSource(it) }

    override suspend fun refreshRates(): MarketRates = withContext(Dispatchers.IO) {
        mutex.withLock {
            val preferred = currentSource.value
            val sources = listOf(preferred) + PriceSource.values().filter { it != preferred }
            for (source in sources) {
                kotlin.coroutines.coroutineContext.ensureActive()
                val result = try { fetch(source) } catch (cancelled: CancellationException) { throw cancelled } catch (_: Exception) { continue }
                kotlin.coroutines.coroutineContext.ensureActive()
                if (result.gold18 <= 0) continue
                val next = MarketSnapshot(result.copy(isLive = true), clock(), QuoteKind.ONLINE)
                // Cache failure must be visible, not reported as a successful persisted quote.
                storage.saveMarket(next)
                mutableRates.value = next.rates
                mutableSnapshot.value = next
                return@withLock next.rates
            }
            mutableSnapshot.value = mutableSnapshot.value?.let { if (it.kind == QuoteKind.ONLINE) it.copy(kind = QuoteKind.CACHED, rates = it.rates.copy(isLive = false)) else it }
            mutableRates.value = mutableRates.value.copy(isLive = false)
            throw IllegalStateException("نرخ تازه دریافت نشد؛ اتصال اینترنت را بررسی یا نرخ دستی ثبت کنید")
        }
    }

    override fun useManual(rates: MarketRates) {
        val next = MarketSnapshot(rates.copy(isLive = false), clock(), QuoteKind.MANUAL)
        storage.saveMarket(next)
        mutableRates.value = next.rates
        mutableSnapshot.value = next
    }

    companion object {
        fun emptyRates(source: PriceSource = PriceSource.TGJU) = MarketRates(0, 0, 0, 0, 0, 0, 0, 0, 0, 0.0, "--:--:--", source, false)

        fun fetchProvider(source: PriceSource): MarketRates {
            val endpoint = when (source) {
                PriceSource.TGJU -> "https://call3.tgju.org/ajax.json"
                PriceSource.TALA_IR -> "https://www.tala.ir/ajax/price"
                PriceSource.ISIGNAL -> "https://signalpardazgroup.com/service/signalData@4.0.0/list"
            }
            val connection = URL(endpoint).openConnection() as HttpURLConnection
            try {
                connection.connectTimeout = 6_000; connection.readTimeout = 6_000
                connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) Qirato/Desktop")
                connection.setRequestProperty("Accept", "application/json")
                if (source == PriceSource.ISIGNAL) {
                    connection.requestMethod = "POST"; connection.doOutput = true
                    connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                    connection.outputStream.use { it.write(ISIGNAL_REQUEST.toByteArray(Charsets.UTF_8)) }
                }
                check(connection.responseCode == 200)
                val bytes = connection.inputStream.use { it.readNBytes(2_000_001) }
                require(bytes.size <= 2_000_000)
                return parseProvider(source, JSONObject(String(bytes, Charsets.UTF_8)))
            } finally { connection.disconnect() }
        }

        fun parseProvider(source: PriceSource, root: JSONObject): MarketRates {
            fun amount(value: Any?, rial: Boolean): Long {
                val text = value?.toString()?.replace(",", "")?.trim().orEmpty()
                val parsed = text.toLongOrNull()?.takeIf { it in 1..90_000_000_000_000L } ?: return 0
                return if (rial) parsed / 10 else parsed
            }
            fun ounce(value: Any?) = value?.toString()?.replace(",", "")?.toDoubleOrNull()?.takeIf { it.isFinite() && it in 0.0..1_000_000.0 } ?: 0.0
            val time = DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.of("Asia/Tehran")).format(Instant.now())
            val prices = mutableMapOf<String, Long>()
            var ons = 0.0
            when (source) {
                PriceSource.TGJU -> {
                    val current = root.getJSONObject("current")
                    mapOf("gold18" to "geram18", "gold24" to "geram24", "goldMelt" to "mesghal", "coinEmami" to "sekee", "coinBahar" to "sekeb", "coinHalf" to "nim", "coinQuarter" to "rob", "coinGerami" to "gerami", "usd" to "price_dollar_rl")
                        .forEach { (field, key) -> prices[field] = amount(current.optJSONObject(key)?.opt("p"), true) }
                    ons = ounce(current.optJSONObject("ons")?.opt("p"))
                }
                PriceSource.TALA_IR -> {
                    val gold = root.getJSONObject("gold")
                    val coin = root.optJSONObject("sekke")
                    val currency = root.optJSONObject("arz")
                    mapOf("gold18" to "gold_18k", "gold24" to "gold_24k", "goldMelt" to "gold_bazartehran").forEach { (field, key) -> prices[field] = amount(gold.optJSONObject(key)?.opt("v"), false) }
                    mapOf("coinEmami" to "sekke-jad", "coinBahar" to "sekke-gad", "coinHalf" to "sekke-nim", "coinQuarter" to "sekke-rob", "coinGerami" to "sekke-grm").forEach { (field, key) -> prices[field] = amount(coin?.optJSONObject(key)?.opt("v"), false) }
                    prices["usd"] = amount(currency?.optJSONObject("arz_dolar")?.opt("v"), false)
                    ons = ounce(gold.optJSONObject("gold_ounce")?.opt("v"))
                }
                PriceSource.ISIGNAL -> {
                    val data = root.getJSONObject("data")
                    val names = mapOf("geram18" to "gold18", "geram24" to "gold24", "mazanne" to "goldMelt", "sekee" to "coinEmami", "sekeb" to "coinBahar", "nim" to "coinHalf", "rob" to "coinQuarter", "gerami" to "coinGerami", "USDb" to "usd")
                    for (group in listOf("gold", "coin", "currency")) {
                        val items = data.optJSONObject(group)?.optJSONArray("data") ?: continue
                        for (i in 0 until items.length()) {
                            val item = items.getJSONObject(i)
                            names[item.optString("name")]?.let { prices[it] = amount(item.opt("close"), true) }
                        }
                    }
                }
            }
            require(prices.getOrDefault("gold18", 0) > 0) { "Missing gold quote" }
            return MarketRates(prices.getOrDefault("gold18", 0), prices.getOrDefault("gold24", 0), prices.getOrDefault("goldMelt", 0),
                prices.getOrDefault("coinEmami", 0), prices.getOrDefault("coinBahar", 0), prices.getOrDefault("coinHalf", 0), prices.getOrDefault("coinQuarter", 0),
                prices.getOrDefault("coinGerami", 0), prices.getOrDefault("usd", 0), ons, time, source, true)
        }

        private val ISIGNAL_REQUEST = """[{"market":"gold","filterName":"gold","property":["name","close"]},{"market":"coin","filterName":"coin","property":["name","close"],"filterLists":[[{"field":"subCategory","include":false,"opt":"e","values":["coinParsian","coinBubble"]}]]},{"market":"currency","filterName":"freeCurrency","property":["name","close"],"filterLists":[[{"field":"subCategory","include":true,"opt":"e","values":["free"]}]]}]"""
    }
}
