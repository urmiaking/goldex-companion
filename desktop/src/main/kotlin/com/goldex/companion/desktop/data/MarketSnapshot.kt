package com.goldex.companion.desktop.data

import com.goldex.companion.data.MarketRates
import com.goldex.companion.data.PriceSource
import org.json.JSONObject

enum class QuoteKind { ONLINE, CACHED, MANUAL }

data class MarketSnapshot(val rates: MarketRates, val observedAt: Long, val kind: QuoteKind) {
    fun isFresh(now: Long): Boolean = kind == QuoteKind.ONLINE && observedAt > 0 && now - observedAt in 0..120_000
    fun label(now: Long): String = when {
        kind == QuoteKind.MANUAL -> "نرخ دستی"
        isFresh(now) -> "دریافت آنلاین"
        else -> "نرخ ذخیره‌شده"
    }
}

internal object MarketJson {
    val longFields = listOf("gold18", "gold24", "goldMelt", "coinEmami", "coinBahar", "coinHalf", "coinQuarter", "coinGerami", "usd")
    fun encode(snapshot: MarketSnapshot) = JSONObject().apply {
        val r = snapshot.rates
        put("observedAt", snapshot.observedAt); put("kind", snapshot.kind.name); put("source", r.source.name)
        listOf(r.gold18, r.gold24, r.goldMelt, r.coinEmami, r.coinBahar, r.coinHalf, r.coinQuarter, r.coinGerami, r.usd)
            .forEachIndexed { i, price -> put(longFields[i], price) }
        put("ons", r.ons); put("lastUpdated", r.lastUpdated)
    }
    fun decode(obj: JSONObject): MarketSnapshot {
        val values = longFields.map { field -> obj.optLong(field, 0).also { require(it in 0..9_000_000_000_000L) } }
        val ons = obj.optDouble("ons", 0.0).also { require(it.isFinite() && it in 0.0..1_000_000.0) }
        val kind = QuoteKind.valueOf(obj.getString("kind"))
        val time = obj.getLong("observedAt").also { require(it > 0) }
        return MarketSnapshot(MarketRates(values[0], values[1], values[2], values[3], values[4], values[5], values[6], values[7], values[8], ons,
            obj.optString("lastUpdated", "--:--:--"), PriceSource.valueOf(obj.getString("source")), false), time, kind)
    }
}
