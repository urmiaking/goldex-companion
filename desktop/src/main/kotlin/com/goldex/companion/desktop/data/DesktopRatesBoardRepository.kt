package com.goldex.companion.desktop.data

import com.goldex.companion.data.MarketRates
import com.goldex.companion.model.CoinType
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption.*
import java.time.Instant
import java.time.ZoneId

enum class BoardInstrument(val indicator: String, val title: String, val description: String,
    val group: Int, val coin: CoinType? = null, val dollar: Boolean = false) {
    GOLD18("geram18", "طلای ۱۸ عیار (۷۵۰ استاندارد)", "مبنای ساخت و ویترین کارگاهی", 1),
    GOLD24("geram24", "طلای ۲۴ عیار (۹۹۹ شمش)", "یک گرم طلای خالص و کارنشده", 1),
    MELT("mesghal", "مظنه آبشده تهران (مثقال ۱۷ عیار)", "مبنای سنتی و بنکداری بازار تهران", 1),
    OUNCE("ons", "انس جهانی طلا (XAU/USD)", "نرخ هر اونس طلا در بازار جهانی", 3, dollar = true),
    USD("price_dollar_rl", "دلار بازار آزاد تهران (نقدی)", "اسکناس نقدی بازار آزاد", 2),
    AED("price_aed", "درهم امارات (حواله دبی)", "نرخ اعلامی شبکه طلا و ارز", 2),
    TETHER("usdt-irr", "تتر دیجیتال (USDT)", "نرخ تتر به تومان", 2),
    BAHAR("sekeb", "سکه بهار آزادی (طرح قدیم)", "سکه تمام بانک مرکزی", 4, CoinType.BAHAR),
    EMAMI("sekee", "سکه امامی (طرح جدید)", "سکه تمام بانک مرکزی", 4, CoinType.EMAMI),
    HALF("nim", "نیم سکه بهار آزادی", "مسکوک بانک مرکزی", 4, CoinType.HALF),
    QUARTER("rob", "ربع سکه بهار آزادی", "مسکوک بانک مرکزی", 4, CoinType.QUARTER),
    GERAMI("gerami", "سکه گرمی بانک مرکزی", "مسکوک بانک مرکزی", 4, CoinType.GERAMI);

    fun primary(r: MarketRates?): Double? = when (this) {
        GOLD18 -> r?.gold18?.toDouble(); GOLD24 -> r?.gold24?.toDouble(); MELT -> r?.goldMelt?.toDouble()
        EMAMI -> r?.coinEmami?.toDouble(); BAHAR -> r?.coinBahar?.toDouble(); HALF -> r?.coinHalf?.toDouble()
        QUARTER -> r?.coinQuarter?.toDouble(); GERAMI -> r?.coinGerami?.toDouble(); USD -> r?.usd?.toDouble()
        OUNCE -> r?.ons; else -> null
    }?.takeIf { it.isFinite() && it > 0 }
}

data class BoardDayQuote(val price: Double, val low: Double?, val high: Double?, val change: Double?, val percent: Double?)
data class BoardHistory(val prices: List<Double>, val receivedAt: Long)
data class BoardSnapshot(val receivedAt: Long, val quotes: Map<BoardInstrument, BoardDayQuote>,
    val history: Map<BoardInstrument, BoardHistory> = emptyMap()) {
    fun today(now: Long) = sameBoardDay(receivedAt, now)
}
internal fun sameBoardDay(a: Long, b: Long): Boolean = Instant.ofEpochMilli(a).atZone(ZoneId.of("Asia/Tehran")).toLocalDate() ==
    Instant.ofEpochMilli(b).atZone(ZoneId.of("Asia/Tehran")).toLocalDate()

interface DesktopRatesBoardGateway {
    fun cached(): BoardSnapshot?
    suspend fun load(previous: BoardSnapshot?): BoardSnapshot
}

/** Public, replaceable market cache is separate from financial records and the selected valuation quote. */
class DesktopRatesBoardRepository(private val directory: Path,
    private val clock: () -> Long = System::currentTimeMillis,
    private val get: suspend (String) -> String = ::boardHttpGet) : DesktopRatesBoardGateway {
    private val cache = directory.resolve("rates-board-v1.json")
    private var original = JSONObject()
    private var readable = true

    override fun cached(): BoardSnapshot? {
        if (!Files.exists(cache)) return null
        return try {
            require(Files.size(cache) <= 4_000_000)
            original = JSONObject(Files.readString(cache))
            decodeCache(original)
        } catch (_: Exception) {
            readable = false
            throw IllegalStateException("کش جزئیات نرخ قابل خواندن نیست؛ فایل قبلی حفظ شده است")
        }
    }

    override suspend fun load(previous: BoardSnapshot?): BoardSnapshot = withContext(Dispatchers.IO) {
        val now = clock()
        val quotes = decodeCurrent(get("https://call3.tgju.org/ajax.json"))
        require(quotes.isNotEmpty())
        val permits = Semaphore(3)
        val histories = coroutineScope {
            quotes.keys.map { instrument -> async {
                val fresh = try { permits.withPermit {
                    BoardHistory(decodeHistory(get("https://api.tgju.org/v1/market/indicator/today-table-data/${instrument.indicator}"), instrument), clock())
                } } catch (cancelled: CancellationException) { throw cancelled }
                catch (_: Exception) { previous?.history?.get(instrument)?.takeIf { sameBoardDay(it.receivedAt, now) } }
                instrument to fresh
            } }.awaitAll().mapNotNull { (key, value) -> value?.let { key to it } }.toMap()
        }
        val result = BoardSnapshot(now, quotes, histories)
        check(readable) { "کش قبلی خراب است؛ برای حفظ آن، جایگزینی انجام نشد" }
        // Preserve unknown root fields/indicators for forwards compatibility.
        val document = JSONObject(original.toString())
        val encoded = encodeCache(result)
        document.put("schemaVersion", 1).put("receivedAt", now)
        for (name in listOf("quotes", "history")) {
            val records = document.optJSONObject(name) ?: JSONObject()
            val updates = encoded.getJSONObject(name)
            BoardInstrument.values().filter { !updates.has(it.name) }.forEach { records.remove(it.name) }
            updates.keys().forEach { key ->
                val record = records.optJSONObject(key) ?: JSONObject()
                val fresh = updates.getJSONObject(key)
                fresh.keys().forEach { field -> record.put(field, fresh.get(field)) }
                records.put(key, record)
            }
            document.put(name, records)
        }
        Files.createDirectories(directory)
        val temp = directory.resolve("rates-board-v1.tmp")
        Files.writeString(temp, document.toString())
        try { Files.move(temp, cache, ATOMIC_MOVE, REPLACE_EXISTING) }
        catch (_: java.nio.file.AtomicMoveNotSupportedException) { Files.move(temp, cache, REPLACE_EXISTING) }
        original = document
        result
    }

    companion object {
        private fun number(value: Any?, dollar: Boolean, zero: Boolean = false): Double? {
            val decimal = value?.toString()?.replace(",", "")?.toBigDecimalOrNull() ?: return null
            // Match the selected quote adapter: whole toman truncate sub-toman rials; ounces retain decimals.
            val n = (if (dollar) decimal else decimal.divide(java.math.BigDecimal.TEN).setScale(0,java.math.RoundingMode.DOWN)).toDouble()
            return n.takeIf { it.isFinite() && it in (if (zero) 0.0 else 0.000001)..(if (dollar) 1_000_000.0 else 9_000_000_000_000.0) }
        }
        fun decodeCurrent(text: String): Map<BoardInstrument, BoardDayQuote> {
            val current = JSONObject(text).getJSONObject("current")
            return BoardInstrument.values().mapNotNull { instrument ->
                val row = current.optJSONObject(instrument.indicator) ?: return@mapNotNull null
                val p = number(row.opt("p"), instrument.dollar) ?: return@mapNotNull null
                val low = number(row.opt("l"), instrument.dollar)
                val high = number(row.opt("h"), instrument.dollar)
                val validRange = low != null && high != null && low <= high && p in low..high
                val direction = when (row.optString("dt")) { "low" -> -1; "high" -> 1; "" -> if (row.optDouble("dp", -1.0) == 0.0) 0 else null; else -> null }
                val amount = number(row.opt("d"), instrument.dollar, zero = true)?.let { n -> direction?.let { n * it } }
                val percent = row.optDouble("dp", Double.NaN).takeIf { it.isFinite() && it in 0.0..1000.0 }?.let { n -> direction?.let { n * it } }
                instrument to BoardDayQuote(p, low.takeIf { validRange }, high.takeIf { validRange }, amount, percent)
            }.toMap()
        }
        fun decodeHistory(text: String, instrument: BoardInstrument): List<Double> {
            val rows = JSONObject(text).getJSONArray("data")
            require(rows.length() in 2..10_000)
            val prices = (0 until rows.length()).map { i ->
                val row = rows.getJSONArray(i)
                val time = java.time.LocalTime.parse(row.getString(1))
                time to requireNotNull(number(row.opt(0), instrument.dollar))
            }.sortedBy { it.first }.distinctBy { it.first }.map { it.second }
            require(prices.size >= 2)
            // Bound drawing while retaining real endpoints and bucket extrema.
            return if (prices.size <= 80) prices else listOf(prices.first()) + prices.chunked((prices.size + 29) / 30)
                .flatMap { bucket -> val a = bucket.indexOf(bucket.minOrNull()!!); val b = bucket.indexOf(bucket.maxOrNull()!!)
                    listOf(a,b).distinct().sorted().map { bucket[it] } } + prices.last()
        }
        fun encodeCache(snapshot: BoardSnapshot) = JSONObject().put("schemaVersion", 1).put("receivedAt", snapshot.receivedAt)
            .put("quotes", JSONObject().apply { snapshot.quotes.forEach { (key,q) -> put(key.name, JSONObject()
                .put("price",q.price).put("low",q.low ?: JSONObject.NULL).put("high",q.high ?: JSONObject.NULL)
                .put("change",q.change ?: JSONObject.NULL).put("percent",q.percent ?: JSONObject.NULL)) } })
            .put("history", JSONObject().apply { snapshot.history.forEach { (key,h) -> put(key.name, JSONObject()
                .put("receivedAt",h.receivedAt).put("prices",JSONArray(h.prices))) } })
        fun decodeCache(root: JSONObject): BoardSnapshot {
            require(root.getInt("schemaVersion") == 1)
            val at = root.getLong("receivedAt").also { require(it > 0) }
            val quotes = root.getJSONObject("quotes")
            val history = root.optJSONObject("history")
            fun valid(n: Double) = n.isFinite() && n in 0.000001..9_000_000_000_000.0
            val decoded = BoardInstrument.values().mapNotNull { key ->
                val q = quotes.optJSONObject(key.name) ?: return@mapNotNull null
                val p = q.getDouble("price").also { require(valid(it)) }
                fun optional(name: String) = q.optDouble(name, Double.NaN).takeIf { it.isFinite() }
                val low = optional("low"); val high = optional("high")
                require(low == null || valid(low)); require(high == null || valid(high)); require(low == null || high == null || low <= high)
                val change = optional("change").also { require(it == null || kotlin.math.abs(it) <= 9_000_000_000_000.0) }
                val pct = optional("percent").also { require(it == null || kotlin.math.abs(it) <= 1000) }
                key to BoardDayQuote(p,low,high,change,pct)
            }.toMap()
            val points = BoardInstrument.values().mapNotNull { key ->
                val h = history?.optJSONObject(key.name) ?: return@mapNotNull null
                val prices = h.getJSONArray("prices").let { a -> require(a.length() in 2..82); (0 until a.length()).map { a.getDouble(it).also { n -> require(valid(n)) } } }
                key to BoardHistory(prices,h.getLong("receivedAt").also { require(it > 0) })
            }.toMap()
            return BoardSnapshot(at,decoded,points)
        }
    }
}

private suspend fun boardHttpGet(url: String): String = withContext(Dispatchers.IO) {
    val c = URL(url).openConnection() as HttpURLConnection
    try {
        c.connectTimeout = 6000; c.readTimeout = 6000
        c.setRequestProperty("User-Agent", "Mozilla/5.0 Qirato-Windows"); c.setRequestProperty("Accept", "application/json")
        check(c.responseCode == 200)
        val bytes = c.inputStream.use { it.readNBytes(2_000_001) }; require(bytes.size <= 2_000_000)
        bytes.toString(Charsets.UTF_8)
    } finally { c.disconnect() }
}
