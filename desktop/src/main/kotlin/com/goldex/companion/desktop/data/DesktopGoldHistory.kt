package com.goldex.companion.desktop.data

import com.goldex.companion.model.TimeHorizon
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

data class GoldHistoryPoint(val at: Long, val price: Long, val label: String)
data class GoldHistorySnapshot(val horizon: TimeHorizon, val points: List<GoldHistoryPoint>, val receivedAt: Long) {
    val low: Long = points.minOf { it.price }
    val high: Long = points.maxOf { it.price }
    // Preserve real bucket extrema and endpoints while bounding per-frame drawing work.
    val renderedPoints: List<GoldHistoryPoint> = if (points.size <= 500) points else
        (listOf(points.first()) + points.chunked((points.size + 199) / 200).flatMap { bucket ->
            listOf(bucket.minBy { it.price }, bucket.maxBy { it.price }).sortedBy { it.at }
        } + points.last()).distinctBy { it.at }.sortedBy { it.at }
    val change: Long get() = points.last().price - points.first().price
    val changePercent: Double get() = change.toDouble() / points.first().price * 100.0
    fun belongsToToday(now: Long): Boolean = horizon != TimeHorizon.TODAY ||
        Instant.ofEpochMilli(receivedAt).atZone(TEHRAN).toLocalDate() == Instant.ofEpochMilli(now).atZone(TEHRAN).toLocalDate()
}
private val TEHRAN = ZoneId.of("Asia/Tehran")

/** Historical TGJU quotes are independent of the user's current/manual valuation quote. */
fun interface DesktopGoldHistoryGateway {
    suspend fun load(horizon: TimeHorizon): GoldHistorySnapshot
}

class DesktopGoldHistoryRepository(private val clock: () -> Long = System::currentTimeMillis) : DesktopGoldHistoryGateway {
    override suspend fun load(horizon: TimeHorizon): GoldHistorySnapshot = withContext(Dispatchers.IO) {
        require(horizon in listOf(TimeHorizon.TODAY, TimeHorizon.ONE_WEEK, TimeHorizon.ONE_MONTH))
        val endpoint = if (horizon == TimeHorizon.TODAY) "today-table-data/geram18" else
            "summary-table-data/geram18?start=0&length=${if (horizon == TimeHorizon.ONE_WEEK) 7 else 30}"
        val connection = URL("https://api.tgju.org/v1/market/indicator/$endpoint").openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = 6000
            connection.readTimeout = 6000
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 Qirato-Windows")
            connection.setRequestProperty("Accept", "application/json")
            check(connection.responseCode == 200)
            val bytes = connection.inputStream.use { it.readNBytes(2_000_001) }
            require(bytes.size <= 2_000_000)
            decode(bytes.toString(Charsets.UTF_8), horizon, clock())
        } finally { connection.disconnect() }
    }

    companion object {
        /** Reject bad records rather than converting malformed external data into a trend. */
        fun decode(text: String, horizon: TimeHorizon, now: Long): GoldHistorySnapshot {
            val rows = JSONObject(text).getJSONArray("data")
            require(rows.length() in 1..10_000)
            val today = Instant.ofEpochMilli(now).atZone(TEHRAN).toLocalDate()
            val points = (0 until rows.length()).map { index ->
                val row = rows.getJSONArray(index)
                val intraday = horizon == TimeHorizon.TODAY
                val raw = row.getString(if (intraday) 0 else 3).replace(",", "").trim()
                val rials = raw.toLongOrNull()
                require(rials != null && rials in 10..10_000_000_000_000L)
                val label = row.getString(if (intraday) 1 else 7)
                val instant = if (intraday) today.atTime(LocalTime.parse(label)).atZone(TEHRAN).toInstant()
                    else LocalDate.parse(row.getString(6).replace('/', '-')).atStartOfDay(TEHRAN).toInstant()
                require(!instant.isAfter(Instant.ofEpochMilli(now)))
                require(intraday || !instant.atZone(TEHRAN).toLocalDate().isBefore(today.minusDays(if (horizon == TimeHorizon.ONE_WEEK) 14 else 60)))
                GoldHistoryPoint(instant.toEpochMilli(), rials / 10, label)
            }.sortedBy { it.at }.distinctBy { it.at }
            require(points.size >= 2) { "Not enough history" }
            return GoldHistorySnapshot(horizon, points, now)
        }
    }
}
