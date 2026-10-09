package com.goldex.companion.desktop.data

import com.goldex.companion.model.TimeHorizon
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.math.BigDecimal
import java.math.RoundingMode
import java.net.HttpURLConnection
import java.net.URL
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption.*
import java.time.*

internal val RateHistoryZone: ZoneId = ZoneId.of("Asia/Tehran")
internal val RateDetailHorizons = listOf(TimeHorizon.TODAY, TimeHorizon.ONE_WEEK, TimeHorizon.ONE_MONTH, TimeHorizon.ONE_YEAR)

/** Whole toman for Iranian instruments; whole US cents for the international ounce. */
data class RateHistoryPoint(val at: Long, val price: Long, val low: Long = price, val high: Long = price, val label: String = "")
data class RateHistorySnapshot(val instrument: BoardInstrument, val horizon: TimeHorizon,
    val points: List<RateHistoryPoint>, val receivedAt: Long) {
    val low get() = points.minOf { it.low }
    val high get() = points.maxOf { it.high }
    val renderedPoints get() = if (points.size <= 500) points else
        (listOf(points.first()) + points.chunked((points.size + 199) / 200).flatMap {
            listOf(it.minBy { p -> p.price }, it.maxBy { p -> p.price }).sortedBy { p -> p.at }
        } + points.last()).distinctBy { it.at }.sortedBy { it.at }
    fun belongsToToday(now: Long) = horizon != TimeHorizon.TODAY ||
        day(points.first().at) == day(now)
    fun period(horizon: TimeHorizon, now: Long): RateHistorySnapshot? {
        if (this.horizon != TimeHorizon.ONE_YEAR || horizon == TimeHorizon.TODAY) return null
        val start = day(now).minusDays(when (horizon) { TimeHorizon.ONE_WEEK -> 7; TimeHorizon.ONE_MONTH -> 30; else -> 365 })
        return points.filter { !day(it.at).isBefore(start) }.takeIf { it.size >= 2 }?.let { copy(horizon = horizon, points = it) }
    }
    companion object {
        fun day(at: Long): LocalDate = Instant.ofEpochMilli(at).atZone(RateHistoryZone).toLocalDate()
        fun valid(snapshot: RateHistorySnapshot, now: Long): Boolean = snapshot.horizon in RateDetailHorizons &&
            snapshot.receivedAt in 1..now && snapshot.points.size in 2..10_000 && snapshot.points.all {
                it.at in 1..snapshot.receivedAt && it.price in 1..1_000_000_000_000L && it.low in 1..it.price && it.high in it.price..1_000_000_000_000L
            } && snapshot.points.zipWithNext().all { (a,b) -> a.at < b.at }
    }
}

interface DesktopRateHistoryGateway {
    fun cached(instrument: BoardInstrument, horizon: TimeHorizon): RateHistorySnapshot? = null
    suspend fun load(instrument: BoardInstrument, horizon: TimeHorizon): RateHistorySnapshot
}

/** Independent public-data cache; never changes financial records or the user's valuation source. */
class DesktopRateHistoryRepository(private val directory: Path, private val clock: () -> Long = System::currentTimeMillis,
    private val get: (String) -> String = ::requestRateHistory) : DesktopRateHistoryGateway {
    private fun file(instrument: BoardInstrument, horizon: TimeHorizon) = directory.resolve("rate-history-v1").resolve("${instrument.name}-${horizon.name}.json")
    override fun cached(instrument: BoardInstrument, horizon: TimeHorizon): RateHistorySnapshot? {
        val path = file(instrument,horizon)
        if (!Files.exists(path)) return null
        require(Files.size(path) <= 2_000_000)
        val document = JSONObject(Files.readString(path))
        val result = decodeCache(document)
        require(result.instrument == instrument && result.horizon == horizon && RateHistorySnapshot.valid(result,clock()))
        return result
    }
    override suspend fun load(instrument: BoardInstrument, horizon: TimeHorizon): RateHistorySnapshot = withContext(Dispatchers.IO) {
        require(horizon in RateDetailHorizons)
        val path = file(instrument,horizon)
        // Check readability before writing; preserve corrupt or newer cache files for diagnosis.
        val original = if (Files.exists(path)) { cached(instrument,horizon); JSONObject(Files.readString(path)) } else JSONObject()
        val length = when (horizon) { TimeHorizon.ONE_WEEK -> 14; TimeHorizon.ONE_MONTH -> 40; else -> 400 }
        val endpoint = if (horizon == TimeHorizon.TODAY) "today-table-data/${instrument.indicator}" else
            "summary-table-data/${instrument.indicator}?start=0&length=$length"
        val result = decode(get("https://api.tgju.org/v1/market/indicator/$endpoint"),instrument,horizon,clock())
        currentCoroutineContext().ensureActive()
        val fresh = encodeCache(result)
        fresh.keys().forEach { original.put(it,fresh.get(it)) }
        Files.createDirectories(path.parent)
        val temporary = path.resolveSibling(path.fileName.toString()+".tmp")
        Files.writeString(temporary,original.toString())
        try { Files.move(temporary,path,ATOMIC_MOVE,REPLACE_EXISTING) }
        catch (_: java.nio.file.AtomicMoveNotSupportedException) { Files.move(temporary,path,REPLACE_EXISTING) }
        result
    }
    companion object {
        fun decode(text: String, instrument: BoardInstrument, horizon: TimeHorizon, now: Long): RateHistorySnapshot {
            require(horizon in RateDetailHorizons)
            val rows = JSONObject(text).getJSONArray("data")
            require(rows.length() in 2..10_000)
            val today = RateHistorySnapshot.day(now)
            val intraday = horizon == TimeHorizon.TODAY
            val points = (0 until rows.length()).map { index ->
                val row = rows.getJSONArray(index)
                fun amount(column: Int): Long {
                    val raw = row.getString(column).replace(",", "").trim()
                    require(raw.matches(Regex("[0-9]+(\\.[0-9]{1,2})?")))
                    val value = BigDecimal(raw)
                    require(value.signum() > 0 && value <= BigDecimal("10000000000000"))
                    // Provider Iranian quotes are rials; the existing market policy uses truncated whole toman.
                    return if (instrument.dollar) value.movePointRight(2).longValueExact()
                        else value.divide(BigDecimal.TEN,0,RoundingMode.DOWN).longValueExact()
                }
                val at = if (intraday) today.atTime(LocalTime.parse(row.getString(1))).atZone(RateHistoryZone).toInstant().toEpochMilli()
                    else LocalDate.parse(row.getString(6).replace('/','-')).atStartOfDay(RateHistoryZone).toInstant().toEpochMilli()
                require(at <= now)
                val price = amount(if (intraday) 0 else 3)
                RateHistoryPoint(at,price,if (intraday) price else amount(1),if (intraday) price else amount(2),row.getString(if (intraday) 1 else 7))
            }.sortedBy { it.at }.distinctBy { it.at }
            val start = today.minusDays(when (horizon) { TimeHorizon.TODAY -> 0; TimeHorizon.ONE_WEEK -> 7; TimeHorizon.ONE_MONTH -> 30; else -> 370 })
            val filtered = points.filter { !RateHistorySnapshot.day(it.at).isBefore(start) }
            val result = RateHistorySnapshot(instrument,horizon,filtered,now)
            require(RateHistorySnapshot.valid(result,now)) { "Not enough valid instrument history" }
            return result
        }
        fun encodeCache(snapshot: RateHistorySnapshot) = JSONObject().put("schemaVersion",1)
            .put("instrument",snapshot.instrument.name).put("horizon",snapshot.horizon.name).put("receivedAt",snapshot.receivedAt)
            .put("points",JSONArray(snapshot.points.map { JSONObject().put("at",it.at).put("price",it.price).put("low",it.low).put("high",it.high).put("label",it.label) }))
        fun decodeCache(document: JSONObject): RateHistorySnapshot {
            require(document.getInt("schemaVersion") == 1)
            val rows = document.getJSONArray("points"); require(rows.length() in 2..10_000)
            return RateHistorySnapshot(BoardInstrument.valueOf(document.getString("instrument")),TimeHorizon.valueOf(document.getString("horizon")),
                (0 until rows.length()).map { val r = rows.getJSONObject(it); RateHistoryPoint(r.getLong("at"),r.getLong("price"),r.optLong("low",r.getLong("price")),r.optLong("high",r.getLong("price")),r.optString("label")) },document.getLong("receivedAt"))
        }
    }
}

private fun requestRateHistory(url: String): String {
    val connection = URL(url).openConnection() as HttpURLConnection
    try {
        connection.connectTimeout=6000; connection.readTimeout=6000
        connection.setRequestProperty("User-Agent","Mozilla/5.0 Qirato-Windows")
        connection.setRequestProperty("Accept","application/json")
        check(connection.responseCode == 200)
        val bytes = connection.inputStream.use { it.readNBytes(2_000_001) }
        require(bytes.size <= 2_000_000)
        return bytes.toString(Charsets.UTF_8)
    } finally { connection.disconnect() }
}
