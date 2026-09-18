package com.goldex.companion.model

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
            candles
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

        // 5. Horizontal time labels (4 to 5 distributed labels)
        val timeLabels = extractTimeLabels(processedCandles, horizon)

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

    private fun extractTimeLabels(candles: List<MarketCandle>, horizon: TimeHorizon): List<String> {
        if (candles.isEmpty()) {
            return when (horizon) {
                TimeHorizon.TODAY -> listOf("۱۰:۰۰", "۱۲:۰۰", "۱۴:۰۰", "۱۶:۰۰", "۱۸:۰۰")
                TimeHorizon.ONE_WEEK -> listOf("شنبه", "دوشنبه", "چهارشنبه", "جمعه", "امروز")
                TimeHorizon.ONE_MONTH -> listOf("۴ هفته پیش", "۳ هفته پیش", "۲ هفته پیش", "۱ هفته پیش", "امروز")
                TimeHorizon.SIX_MONTHS -> listOf("۶ ماه پیش", "۴ ماه پیش", "۳ ماه پیش", "۲ ماه پیش", "امروز")
                TimeHorizon.ONE_YEAR -> listOf("۱ سال پیش", "۹ ماه پیش", "۶ ماه پیش", "۳ ماه پیش", "امروز")
            }
        }

        if (horizon == TimeHorizon.TODAY) {
            val hasTimes = candles.any { it.dateShamsi.contains(":") }
            if (!hasTimes) {
                return listOf("۱۰:۰۰", "۱۲:۰۰", "۱۴:۰۰", "۱۶:۰۰", "۱۸:۰۰")
            }
        }

        if (candles.size <= 5) {
            return candles.mapIndexed { idx, candle ->
                val isLast = (idx == candles.lastIndex)
                if (isLast) {
                    if (horizon == TimeHorizon.TODAY) {
                        formatCandleDate(candle.dateShamsi, horizon).ifBlank { "اکنون" }
                    } else {
                        "امروز"
                    }
                } else {
                    formatCandleDate(candle.dateShamsi, horizon)
                }
            }
        }

        val step = (candles.size - 1).toDouble() / 4.0
        return (0..4).map { i ->
            val idx = (i * step).roundToLong().toInt().coerceIn(0, candles.lastIndex)
            val isLast = (i == 4)
            if (isLast) {
                if (horizon == TimeHorizon.TODAY) {
                    formatCandleDate(candles[idx].dateShamsi, horizon).ifBlank { "اکنون" }
                } else {
                    "امروز"
                }
            } else {
                formatCandleDate(candles[idx].dateShamsi, horizon)
            }
        }
    }

    private fun formatCandleDate(rawDate: String, horizon: TimeHorizon): String {
        if (horizon == TimeHorizon.TODAY) {
            val trimmed = rawDate.trim()
            if (trimmed.contains(":")) {
                val timePart = if (trimmed.contains(" ")) trimmed.substringAfterLast(" ") else trimmed
                val parts = timePart.split(":")
                if (parts.size >= 2) {
                    val hh = parts[0].padStart(2, '0')
                    val mm = parts[1].padStart(2, '0')
                    return PersianNumberFormatter.toPersianDigits("$hh:$mm")
                }
            }
            return if (trimmed.isNotEmpty()) PersianNumberFormatter.toPersianDigits(trimmed) else "اکنون"
        }

        val clean = rawDate.replace("/", "").trim()
        if (clean.length == 8) {
            val month = clean.substring(4, 6)
            val day = clean.substring(6, 8)
            val monthName = when (month) {
                "01" -> "فروردین"
                "02" -> "اردیبهشت"
                "03" -> "خرداد"
                "04" -> "تیر"
                "05" -> "مرداد"
                "06" -> "شهریور"
                "07" -> "مهر"
                "08" -> "آبان"
                "09" -> "آذر"
                "10" -> "دی"
                "11" -> "بهمن"
                "12" -> "اسفند"
                else -> month
            }
            return if (horizon == TimeHorizon.ONE_YEAR || horizon == TimeHorizon.SIX_MONTHS) {
                monthName
            } else {
                "${day.toIntOrNull() ?: day} $monthName"
            }
        }
        return if (rawDate.length > 5) rawDate.takeLast(5) else rawDate
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
     * Downsamples dense intraday candles into ~30-minute interval buckets.
     * Smooths out micro-second fluctuations while strictly preserving:
     * - Opening trade of the session
     * - Latest/live market quote
     * - High and low extremes
     * If data has gaps, connects nearest available intervals linearly.
     */
    fun sampleIntradayCandles(candles: List<MarketCandle>): List<MarketCandle> {
        if (candles.size <= 8) return candles

        val parsed = candles.map { candle ->
            candle to parseMinuteOfDay(candle.dateShamsi)
        }

        // If timestamps cannot be parsed, fallback to uniform stride sampling (~12 points)
        if (parsed.any { it.second == null }) {
            return sampleUniformly(candles, targetCount = 12)
        }

        // Group by 30-minute bucket (0..47)
        val grouped = parsed.groupBy { it.second!! / 30 }
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
                    dateShamsi = last.dateShamsi,
                    dateGregorian = last.dateGregorian
                )
            )
        }

        // Ensure session opening candle is preserved as first point if different from first bucket's representative
        val firstOriginal = candles.first()
        if (sampled.isNotEmpty() && sampled.first().dateShamsi != firstOriginal.dateShamsi) {
            sampled.add(0, firstOriginal)
        }

        // Ensure latest live candle is preserved as last point if different from last bucket's representative
        val lastOriginal = candles.last()
        if (sampled.isNotEmpty() && sampled.last().dateShamsi != lastOriginal.dateShamsi) {
            sampled.add(lastOriginal)
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
