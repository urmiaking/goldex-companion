package com.goldex.companion.model

import java.util.Calendar
import java.util.Locale
import kotlin.math.roundToLong

/**
 * Represents a single trading candle / OHLC record in the market history.
 * All prices are in whole Tomans.
 */
data class MarketCandle(
    val open: Long,
    val high: Long,
    val low: Long,
    val close: Long,
    val dateShamsi: String,
    val dateGregorian: String = ""
)

object MarketHistoryConverter {

    /**
     * Converts a chronological list of market candles into normalized [TrendChartData].
     */
    fun toTrendChartData(
        candles: List<MarketCandle>,
        horizon: TimeHorizon,
        fallbackBasePrice: Long
    ): TrendChartData {
        if (candles.isEmpty()) {
            return emptyTrendChart(fallbackBasePrice)
        }

        val processedCandles = if (horizon == TimeHorizon.TODAY) {
            sampleIntradayCandles(candles)
        } else {
            candles.map { it.copy(dateShamsi = formatFullShamsiDateTime(it.dateShamsi)) }
        }

        // 1. Identify min, max and peak
        val prices = processedCandles.map { it.close.coerceAtLeast(1L) }
        val minPrice = prices.minOrNull() ?: fallbackBasePrice
        val maxPrice = prices.maxOrNull() ?: fallbackBasePrice
        val priceSpan = (maxPrice - minPrice).toFloat()

        // 2. Normalized points in range [0f, 1f]
        // yNorm is scaled to [0.15f, 0.85f] to prevent touching top/bottom canvas bounds
        val rawPoints = if (processedCandles.size == 1) {
            listOf(0.00f to 0.50f, 1.00f to 0.50f)
        } else {
            processedCandles.mapIndexed { idx, candle ->
                val xNorm = idx.toFloat() / (processedCandles.size - 1).toFloat()
                val price = candle.close.coerceAtLeast(1L)
                val rawY = if (priceSpan > 0f) (price - minPrice).toFloat() / priceSpan else 0.5f
                val clampedY = 0.15f + (rawY * 0.70f)
                xNorm to clampedY
            }
        }

        // 3. Peak detection
        var peakIdx = 0
        var highestPrice = processedCandles.first().close
        processedCandles.forEachIndexed { index, candle ->
            val candlePeak = if (candle.high > 0L) candle.high else candle.close
            if (candlePeak > highestPrice) {
                highestPrice = candlePeak
                peakIdx = index
            }
        }
        val peakX = if (processedCandles.size > 1) peakIdx.toFloat() / (processedCandles.size - 1).toFloat() else 0.5f
        val peakRawY = if (priceSpan > 0f) (highestPrice - minPrice).toFloat() / priceSpan else 0.85f
        val peakY = (0.15f + (peakRawY * 0.70f)).coerceIn(0.20f, 0.92f)

        // 4. Gentle 3-point smoothing filter for dense points (> 5 points)
        // Eliminates micro-noise while strictly preserving endpoints, peak, and bottom.
        val points = if (rawPoints.size > 5) {
            val smoothed = rawPoints.toMutableList()
            for (i in 1 until rawPoints.size - 1) {
                if (i == peakIdx || processedCandles[i].close == maxPrice || processedCandles[i].close == minPrice) {
                    continue
                }
                val prevY = rawPoints[i - 1].second
                val currY = rawPoints[i].second
                val nextY = rawPoints[i + 1].second
                val smoothY = (0.15f * prevY) + (0.70f * currY) + (0.15f * nextY)
                smoothed[i] = rawPoints[i].first to smoothY
            }
            smoothed
        } else {
            rawPoints
        }

        // 5. Horizontal time labels - omitted per user preference to avoid clutter
        val timeLabels = emptyList<String>()

        // 6. Fluctuation percentage (Sign placed strictly behind number: e.g. ۲.۴٪- or ۲.۴٪+)
        val firstPrice = processedCandles.first().open.takeIf { it > 0L } ?: processedCandles.first().close
        val lastPrice = processedCandles.last().close
        val diffAmount = lastPrice - firstPrice
        val pct = if (firstPrice > 0L) {
            (diffAmount.toDouble() / firstPrice.toDouble()) * 100.0
        } else 0.0
        val isPositive = pct >= 0.0
        val sign = if (isPositive) "+" else "-"
        val absPct = kotlin.math.abs(pct)
        val fluctuationText = PersianNumberFormatter.toPersianDigits(
            String.format(Locale.US, "%.1f%%%s", absPct, sign)
        )

        return TrendChartData(
            points = points,
            peakPrice = highestPrice,
            peakXRatio = peakX,
            peakYRatio = peakY,
            timeLabels = timeLabels,
            fluctuationRangeText = fluctuationText,
            isPositive = isPositive,
            candles = processedCandles,
            fluctuationPercent = pct,
            fluctuationAmount = diffAmount,
            isAvailable = true
        )
    }

    /**
     * Computes 30-day technical indicators from the last 30 daily candles.
     */
    fun toMonthlyMarketStats(
        candles: List<MarketCandle>,
        fallbackBasePrice: Long,
        currencyUnit: String = "تومان"
    ): MonthlyMarketStats {
        val isDollar = (currencyUnit == "$")
        val unitSuffix = if (isDollar) " $" else " تومان"

        if (candles.isEmpty()) {
            return if (isDollar) {
                MonthlyMarketStats(
                    dailyRangeText = "۲۴+ $ (۰.۹٪+)",
                    weeklyChangeText = "۵۲+ $ (۱.۹٪+)",
                    thirtyDayHigh = (fallbackBasePrice * 1.025).roundToLong(),
                    thirtyDayLow = (fallbackBasePrice * 0.978).roundToLong(),
                    weightedAverage = (fallbackBasePrice * 0.988).roundToLong()
                )
            } else {
                MonthlyMarketStats(
                    dailyRangeText = "۴۵,۰۰۰+ تومان (۱.۱٪+)",
                    weeklyChangeText = "۱۰۱,۰۰۰+ تومان (۲.۴٪+)",
                    thirtyDayHigh = (fallbackBasePrice * 1.029).roundToLong(),
                    thirtyDayLow = (fallbackBasePrice * 0.976).roundToLong(),
                    weightedAverage = (fallbackBasePrice * 0.984).roundToLong()
                )
            }
        }

        val thirtyDayCandles = if (candles.size > 30) candles.takeLast(30) else candles
        val thirtyHigh = thirtyDayCandles.maxOfOrNull { if (it.high > 0L) it.high else it.close } ?: fallbackBasePrice
        val thirtyLow = thirtyDayCandles.minOfOrNull { if (it.low > 0L) it.low else it.close } ?: fallbackBasePrice
        val avg = thirtyDayCandles.map { it.close }.average().roundToLong().coerceAtLeast(1L)

        // Daily Range (today's candle)
        val latest = thirtyDayCandles.last()
        val dailyDiff = (latest.high - latest.low).coerceAtLeast(0L)
        val dailyDiffPct = if (latest.open > 0L) (dailyDiff.toDouble() / latest.open.toDouble()) * 100.0 else 0.0
        val dailyRangeText = "${PersianNumberFormatter.format(dailyDiff)}+$unitSuffix (${PersianNumberFormatter.toPersianDigits(String.format(Locale.US, "%.1f", dailyDiffPct))}٪+)"

        // Weekly Change (last 7 candles)
        val weeklyCandles = if (candles.size > 7) candles.takeLast(7) else candles
        val weekFirst = weeklyCandles.first().open.takeIf { it > 0L } ?: weeklyCandles.first().close
        val weekLast = weeklyCandles.last().close
        val weekDiff = weekLast - weekFirst
        val weekSign = if (weekDiff >= 0L) "+" else "-"
        val weekPct = if (weekFirst > 0L) (kotlin.math.abs(weekDiff).toDouble() / weekFirst.toDouble()) * 100.0 else 0.0
        val weeklyChangeText = "${PersianNumberFormatter.format(kotlin.math.abs(weekDiff))}$weekSign$unitSuffix (${PersianNumberFormatter.toPersianDigits(String.format(Locale.US, "%.1f", weekPct))}٪$weekSign)"

        return MonthlyMarketStats(
            dailyRangeText = dailyRangeText,
            weeklyChangeText = weeklyChangeText,
            thirtyDayHigh = thirtyHigh,
            thirtyDayLow = thirtyLow,
            weightedAverage = avg
        )
    }

    /**
     * Converts Gregorian date (year, month 1..12, day 1..31) to Solar Hijri / Shamsi (year, month, day).
     * Standard mathematical conversion algorithm without external dependencies.
     */
    fun gregorianToShamsi(gy: Int, gm: Int, gd: Int): Triple<Int, Int, Int> {
        val gdm = intArrayOf(0, 31, 59, 90, 120, 151, 181, 212, 243, 273, 304, 334)
        val gy2 = if (gm > 2) (gy + 1) else gy
        var days = 355666 + (365 * gy) + ((gy2 + 3) / 4) - ((gy2 + 99) / 100) + ((gy2 + 399) / 400) + gd + gdm[gm - 1]
        var jy = -1595 + (33 * (days / 12053))
        days %= 12053
        jy += 4 * (days / 1461)
        days %= 1461
        if (days > 365) {
            jy += ((days - 1) / 365)
            days = (days - 1) % 365
        }
        val jm = if (days < 186) 1 + (days / 31) else 7 + ((days - 186) / 30)
        val jd = 1 + (if (days < 186) (days % 31) else ((days - 186) % 30))
        return Triple(jy, jm, jd)
    }

    /**
     * Returns today's Solar Hijri / Shamsi date string in format "YYYY/MM/DD".
     */
    fun getTodayShamsiDate(timestamp: Long = System.currentTimeMillis()): String {
        val cal = Calendar.getInstance().apply {
            timeInMillis = timestamp
        }
        val gy = cal.get(Calendar.YEAR)
        val gm = cal.get(Calendar.MONTH) + 1
        val gd = cal.get(Calendar.DAY_OF_MONTH)
        val (jy, jm, jd) = gregorianToShamsi(gy, gm, gd)
        val mm = String.format(Locale.US, "%02d", jm)
        val dd = String.format(Locale.US, "%02d", jd)
        return "$jy/$mm/$dd"
    }

    /**
     * Formats 8-digit date string into "YYYY/MM/DD".
     */
    fun formatShamsiDateOnly(raw: String): String {
        val clean = raw.replace("/", "").replace("-", "").trim()
        if (clean.length == 8) {
            val yyyy = clean.substring(0, 4)
            val mm = clean.substring(4, 6)
            val dd = clean.substring(6, 8)
            return "$yyyy/$mm/$dd"
        }
        return raw
    }

    /**
     * Formats a candle date/time string into a complete Shamsi date and time:
     * e.g. "1405/06/25 18:22".
     * In RTL layout, this reads as "۱۴۰۵/۰۶/۲۵ ۱۸:۲۲" (date first, then time).
     */
    fun formatFullShamsiDateTime(raw: String): String {
        val normalized = PersianNumberFormatter.toEnglishDigits(raw).trim()
        if (normalized.isEmpty()) return ""

        val todayShamsi = getTodayShamsiDate()

        if (normalized.contains(":")) {
            val timePart = if (normalized.contains(" ")) normalized.substringAfterLast(" ") else normalized
            val timeTokens = timePart.split(":")
            val hh = timeTokens.getOrNull(0)?.padStart(2, '0') ?: "00"
            val mm = timeTokens.getOrNull(1)?.padStart(2, '0') ?: "00"
            val formattedTime = "$hh:$mm"

            val datePart = if (normalized.contains(" ")) normalized.substringBeforeLast(" ").trim() else ""
            val formattedDate = if (datePart.isNotBlank()) {
                formatShamsiDateOnly(datePart)
            } else {
                todayShamsi
            }
            return "$formattedDate $formattedTime"
        }

        return formatShamsiDateOnly(normalized)
    }

    fun emptyTrendChart(fallbackBasePrice: Long = 0L): TrendChartData {
        return TrendChartData(
            points = emptyList(),
            peakPrice = fallbackBasePrice,
            peakXRatio = 0.5f,
            peakYRatio = 0.5f,
            timeLabels = emptyList(),
            fluctuationRangeText = "۰.۰٪",
            isPositive = true,
            candles = emptyList(),
            fluctuationPercent = 0.0,
            fluctuationAmount = 0L,
            isAvailable = false
        )
    }

    /**
     * Parses minute of day (0..1439) from a time string such as "14:30", "14:30:15", or "1405/06/23 14:30".
     * Supports both English and Persian digits.
     */
    fun parseMinuteOfDay(dateStr: String): Int? {
        val normalized = PersianNumberFormatter.toEnglishDigits(dateStr).trim()
        if (!normalized.contains(":")) return null
        val timePart = if (normalized.contains(" ")) normalized.substringAfterLast(" ") else normalized
        val parts = timePart.split(":")
        if (parts.size < 2) return null
        val hh = parts[0].toIntOrNull() ?: return null
        val mm = parts[1].toIntOrNull() ?: return null
        return if (hh in 0..23 && mm in 0..59) hh * 60 + mm else null
    }

    /**
     * Downsamples dense intraday candles into ~15-minute interval buckets (0..95) from midnight 00:00 to current time.
     * Smooths out micro-second fluctuations while strictly preserving:
     * - Opening trade of the session
     * - Latest/live market quote
     * - High and low extremes
     * If data has gaps, connects nearest available intervals linearly.
     */
    fun sampleIntradayCandles(candles: List<MarketCandle>): List<MarketCandle> {
        if (candles.size <= 8) {
            return candles.map { it.copy(dateShamsi = formatFullShamsiDateTime(it.dateShamsi)) }
        }

        val parsed = candles.map { candle ->
            candle to parseMinuteOfDay(candle.dateShamsi)
        }

        // If timestamps cannot be parsed, fallback to uniform stride sampling (~16 points)
        if (parsed.any { it.second == null }) {
            return sampleUniformly(candles, targetCount = 16).map {
                it.copy(dateShamsi = formatFullShamsiDateTime(it.dateShamsi))
            }
        }

        // Group by 15-minute bucket (0..95 for minuteOfDay in 0..1439)
        val grouped = parsed.groupBy { it.second!! / 15 }
        val sortedBuckets = grouped.toSortedMap()

        val sampled = mutableListOf<MarketCandle>()
        for ((_, bucket) in sortedBuckets) {
            val bucketCandles = bucket.map { it.first }
            val first = bucketCandles.first()
            val last = bucketCandles.last()
            val high = bucketCandles.maxOf { if (it.high > 0L) it.high else it.close }
            val low = bucketCandles.minOf { if (it.low > 0L) it.low else it.close }
            sampled.add(
                MarketCandle(
                    open = if (first.open > 0L) first.open else first.close,
                    high = high,
                    low = low,
                    close = last.close,
                    dateShamsi = formatFullShamsiDateTime(last.dateShamsi),
                    dateGregorian = last.dateGregorian
                )
            )
        }

        // Ensure session opening candle is preserved as first point with full date format
        val firstOriginal = candles.first()
        val firstFormatted = firstOriginal.copy(dateShamsi = formatFullShamsiDateTime(firstOriginal.dateShamsi))
        if (sampled.isNotEmpty() && sampled.first().dateShamsi != firstFormatted.dateShamsi) {
            sampled.add(0, firstFormatted)
        }

        // Ensure latest live candle is preserved as last point with full date format
        val lastOriginal = candles.last()
        val lastFormatted = lastOriginal.copy(dateShamsi = formatFullShamsiDateTime(lastOriginal.dateShamsi))
        if (sampled.isNotEmpty() && sampled.last().dateShamsi != lastFormatted.dateShamsi) {
            sampled.add(lastFormatted)
        }

        return sampled
    }

    private fun sampleUniformly(candles: List<MarketCandle>, targetCount: Int): List<MarketCandle> {
        if (candles.size <= targetCount) return candles
        val step = (candles.size - 1).toDouble() / (targetCount - 1).toDouble()
        val result = mutableListOf<MarketCandle>()
        val seenIndices = mutableSetOf<Int>()
        for (i in 0 until targetCount) {
            val idx = (i * step).roundToLong().toInt().coerceIn(0, candles.lastIndex)
            if (seenIndices.add(idx)) {
                result.add(candles[idx])
            }
        }
        return result
    }
}
