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
            return fallbackTrendChart(horizon, fallbackBasePrice)
        }

        // 1. Identify min, max and peak
        val prices = candles.map { it.close.coerceAtLeast(1L) }
        val minPrice = prices.minOrNull() ?: fallbackBasePrice
        val maxPrice = prices.maxOrNull() ?: fallbackBasePrice
        val priceSpan = (maxPrice - minPrice).toFloat()

        // 2. Normalized points in range [0f, 1f]
        // yNorm is scaled to [0.15f, 0.85f] to prevent touching top/bottom canvas bounds
        val rawPoints = if (candles.size == 1) {
            listOf(0.00f to 0.50f, 1.00f to 0.50f)
        } else {
            candles.mapIndexed { idx, candle ->
                val xNorm = idx.toFloat() / (candles.size - 1).toFloat()
                val price = candle.close.coerceAtLeast(1L)
                val rawY = if (priceSpan > 0f) (price - minPrice).toFloat() / priceSpan else 0.5f
                val clampedY = 0.15f + (rawY * 0.70f)
                xNorm to clampedY
            }
        }

        // 3. Peak detection
        var peakIdx = 0
        var highestPrice = candles.first().close
        candles.forEachIndexed { index, candle ->
            val candlePeak = if (candle.high > 0L) candle.high else candle.close
            if (candlePeak > highestPrice) {
                highestPrice = candlePeak
                peakIdx = index
            }
        }
        val peakX = if (candles.size > 1) peakIdx.toFloat() / (candles.size - 1).toFloat() else 0.5f
        val peakRawY = if (priceSpan > 0f) (highestPrice - minPrice).toFloat() / priceSpan else 0.85f
        val peakY = (0.15f + (peakRawY * 0.70f)).coerceIn(0.20f, 0.92f)

        // 4. Gentle 3-point smoothing filter for dense points (> 5 points)
        // Eliminates micro-noise while strictly preserving endpoints, peak, and bottom.
        // Skip smoothing for TODAY horizon so intraday and hourly price movements remain sharp and unblurred.
        val points = if (rawPoints.size > 5 && horizon != TimeHorizon.TODAY) {
            val smoothed = rawPoints.toMutableList()
            for (i in 1 until rawPoints.size - 1) {
                if (i == peakIdx || candles[i].close == maxPrice || candles[i].close == minPrice) {
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
        val timeLabels = extractTimeLabels(candles, horizon)

        // 6. Fluctuation percentage (Sign placed strictly behind number: e.g. ۲.۴٪- or ۲.۴٪+)
        val firstPrice = candles.first().open.takeIf { it > 0L } ?: candles.first().close
        val lastPrice = candles.last().close
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
            candles = candles,
            fluctuationPercent = pct,
            fluctuationAmount = diffAmount
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

    private fun fallbackTrendChart(horizon: TimeHorizon, peakPrice: Long): TrendChartData {
        return when (horizon) {
            TimeHorizon.TODAY -> TrendChartData(
                points = listOf(0.00f to 0.22f, 0.25f to 0.38f, 0.65f to 0.85f, 0.80f to 0.32f, 1.00f to 0.48f),
                peakPrice = peakPrice,
                peakXRatio = 0.65f,
                peakYRatio = 0.85f,
                timeLabels = listOf("۱۰:۰۰", "۱۲:۰۰", "۱۴:۰۰", "۱۶:۰۰", "۱۸:۰۰"),
                fluctuationRangeText = "۱.۱٪+",
                isPositive = true,
                fluctuationPercent = 1.1,
                fluctuationAmount = (peakPrice * 0.011).roundToLong()
            )
            TimeHorizon.ONE_WEEK -> TrendChartData(
                points = listOf(0.00f to 0.30f, 0.35f to 0.45f, 0.52f to 0.88f, 0.70f to 0.60f, 1.00f to 0.68f),
                peakPrice = (peakPrice * 1.008).roundToLong(),
                peakXRatio = 0.52f,
                peakYRatio = 0.88f,
                timeLabels = listOf("شنبه", "دوشنبه", "چهارشنبه", "جمعه", "امروز"),
                fluctuationRangeText = "۲.۴٪+",
                isPositive = true,
                fluctuationPercent = 2.4,
                fluctuationAmount = (peakPrice * 0.024).roundToLong()
            )
            TimeHorizon.ONE_MONTH -> TrendChartData(
                points = listOf(0.00f to 0.18f, 0.40f to 0.48f, 0.78f to 0.90f, 0.90f to 0.72f, 1.00f to 0.78f),
                peakPrice = (peakPrice * 1.025).roundToLong(),
                peakXRatio = 0.78f,
                peakYRatio = 0.90f,
                timeLabels = listOf("۴ هفته پیش", "۳ هفته پیش", "۲ هفته پیش", "۱ هفته پیش", "امروز"),
                fluctuationRangeText = "۴.۵٪+",
                isPositive = true,
                fluctuationPercent = 4.5,
                fluctuationAmount = (peakPrice * 0.045).roundToLong()
            )
            TimeHorizon.SIX_MONTHS -> TrendChartData(
                points = listOf(0.00f to 0.12f, 0.42f to 0.40f, 0.75f to 0.75f, 0.88f to 0.92f, 1.00f to 0.86f),
                peakPrice = (peakPrice * 1.065).roundToLong(),
                peakXRatio = 0.88f,
                peakYRatio = 0.92f,
                timeLabels = listOf("۶ ماه پیش", "۴ ماه پیش", "۳ ماه پیش", "۲ ماه پیش", "امروز"),
                fluctuationRangeText = "۱۴.۲٪+",
                isPositive = true,
                fluctuationPercent = 14.2,
                fluctuationAmount = (peakPrice * 0.142).roundToLong()
            )
            TimeHorizon.ONE_YEAR -> TrendChartData(
                points = listOf(0.00f to 0.08f, 0.50f to 0.45f, 0.70f to 0.68f, 0.85f to 0.94f, 1.00f to 0.88f),
                peakPrice = (peakPrice * 1.150).roundToLong(),
                peakXRatio = 0.85f,
                peakYRatio = 0.94f,
                timeLabels = listOf("۱ سال پیش", "۹ ماه پیش", "۶ ماه پیش", "۳ ماه پیش", "امروز"),
                fluctuationRangeText = "۳۸.۶٪+",
                isPositive = true,
                fluctuationPercent = 38.6,
                fluctuationAmount = (peakPrice * 0.386).roundToLong()
            )
        }
    }
}
