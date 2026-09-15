package com.goldex.companion

import com.goldex.companion.data.MarketRates
import com.goldex.companion.model.*
import org.junit.Assert.*
import org.junit.Test

class MarketRateDetailTest {

    private val sampleRates = MarketRates(
        gold18 = 4_285_000L,
        gold24 = 5_713_333L,
        goldMelt = 18_550_000L,
        coinEmami = 49_100_000L,
        coinBahar = 44_200_000L,
        coinHalf = 25_300_000L,
        coinQuarter = 15_400_000L,
        coinGerami = 7_200_000L,
        usd = 92_500L,
        ons = 2684.2
    )

    @Test
    fun allMarketRateItemTypesGenerateValidDetailStates() {
        MarketRateItemType.values().forEach { type ->
            val state = MarketRateDetailState.create(type, sampleRates)
            assertNotNull("State for $type must not be null", state)
            assertEquals(type, state.type)
            assertTrue("Title for $type must not be empty", state.title.isNotEmpty())
            assertTrue("Subtitle for $type must not be empty", state.subtitle.isNotEmpty())
            assertTrue("Current price for $type must be positive", state.currentPrice > 0L)
            assertTrue("Day high must be greater than or equal to day low for $type", state.dayHigh >= state.dayLow)
            assertTrue("Currency unit must be specified for $type", state.currencyUnit.isNotEmpty())
        }
    }

    @Test
    fun gold18DetailStateMatchesStitchSpecifications() {
        val state = MarketRateDetailState.create(MarketRateItemType.GOLD_18K, sampleRates)

        assertEquals("طلای ۱۸ عیار (۷۵۰)", state.title)
        assertEquals(4_285_000L, state.currentPrice)
        assertEquals("تومان", state.currencyUnit)
        assertEquals("نرخ رسمی", state.categoryBadge)
        assertTrue(state.isPositive)
        assertEquals(32_000L, state.changeAmount)
        assertEquals(0.84, state.changePercent, 0.001)

        // Balanced metrics
        assertTrue("Day low must be below current price", state.dayLow < state.currentPrice)
        assertTrue("Day high must be above current price", state.dayHigh > state.currentPrice)
        assertEquals("حباب / اسپرد", state.bubbleOrSpreadLabel)

        // Technical stats
        assertTrue("30-day high must exceed day high", state.monthlyStats.thirtyDayHigh > state.dayHigh)
        assertTrue("30-day low must be below day low", state.monthlyStats.thirtyDayLow < state.dayLow)

        // Recent transactions
        assertTrue("Recent transactions must not be empty for 18K", state.recentTransactions.isNotEmpty())
        val buyTx = state.recentTransactions.firstOrNull { it.isBuy }
        assertNotNull("Should contain a buy transaction", buyTx)
        assertEquals("۸۸۴۲", buyTx!!.documentNumber)
    }

    @Test
    fun timeHorizonTrendChartsAreProperlyConfigured() {
        val state = MarketRateDetailState.create(MarketRateItemType.GOLD_18K, sampleRates)

        TimeHorizon.values().forEach { horizon ->
            val chart = state.chartDataByHorizon[horizon]
            assertNotNull("Chart for $horizon must exist", chart)
            assertTrue("Points must not be empty for $horizon", chart!!.points.isNotEmpty())
            assertEquals("Must have 5 time labels for $horizon", 5, chart.timeLabels.size)
            assertTrue("Peak price must be positive for $horizon", chart.peakPrice > 0L)
            assertTrue("Peak X ratio must be in (0, 1) for $horizon", chart.peakXRatio in 0.0f..1.0f)
            assertTrue("Peak Y ratio must be in (0, 1) for $horizon", chart.peakYRatio in 0.0f..1.0f)
        }
    }

    @Test
    fun coinEmamiDetailStateContainsCorrectBubbleMetrics() {
        val state = MarketRateDetailState.create(MarketRateItemType.COIN_EMAMI, sampleRates)

        assertEquals("سکه تمام امامی (طرح جدید)", state.title)
        assertEquals("حباب قیمتی", state.bubbleOrSpreadLabel)
        assertTrue("Coin bubble must be positive", state.bubbleOrSpread > 0L)
        assertTrue("Reference text must mention intrinsic value or bubble", state.referenceIndexText.contains("حباب"))
    }

    @Test
    fun onsGlobalDetailStateUsesUsdCurrency() {
        val state = MarketRateDetailState.create(MarketRateItemType.ONS, sampleRates)

        assertEquals("$", state.currencyUnit)
        assertEquals(2684L, state.currentPrice)
        assertEquals("اسپرد جهانی", state.bubbleOrSpreadLabel)
    }

    @Test
    fun persianNumberFormatterFormatsAmountsCorrectly() {
        val formatted = PersianNumberFormatter.format(4_285_000L)
        assertEquals("۴,۲۸۵,۰۰۰", formatted)

        val formattedWithTomans = "${PersianNumberFormatter.format(4_285_000L)} تومان"
        assertEquals("۴,۲۸۵,۰۰۰ تومان", formattedWithTomans)
    }

    @Test
    fun formatDeltaPlacesSignsBehindNumbers() {
        val positive = PersianNumberFormatter.formatDelta(32000L, 0.8)
        assertEquals("۳۲,۰۰۰+ (۰.۸٪+)", positive)

        val negative = PersianNumberFormatter.formatDelta(-34000L, -1.2)
        assertEquals("۳۴,۰۰۰- (۱.۲٪-)", negative)
    }

    @Test
    fun positiveAndNegativeSignsArePlacedBehindNumbers() {
        val state = MarketRateDetailState.create(MarketRateItemType.GOLD_18K, sampleRates)
        assertFalse("dailyRangeText should not start with +", state.monthlyStats.dailyRangeText.startsWith("+"))
        assertFalse("weeklyChangeText should not start with +", state.monthlyStats.weeklyChangeText.startsWith("+"))
        assertFalse("referenceIndexChange should not start with +", state.referenceIndexChange.startsWith("+"))

        state.chartDataByHorizon.values.forEach { chart ->
            assertFalse("fluctuationRangeText should not start with +", chart.fluctuationRangeText.startsWith("+"))
            assertTrue("fluctuationRangeText should end with + or -", chart.fluctuationRangeText.endsWith("+") || chart.fluctuationRangeText.endsWith("-"))
        }
    }

    @Test
    fun realCandlesTransformToAccurateTrendChartData() {
        val candles = listOf(
            MarketCandle(open = 23000000L, high = 23200000L, low = 22900000L, close = 23100000L, dateShamsi = "14050620"),
            MarketCandle(open = 23100000L, high = 23500000L, low = 23000000L, close = 23400000L, dateShamsi = "14050621"),
            MarketCandle(open = 23400000L, high = 23900000L, low = 23300000L, close = 23800000L, dateShamsi = "14050622"),
            MarketCandle(open = 23800000L, high = 23850000L, low = 23100000L, close = 23283400L, dateShamsi = "14050623")
        )

        val chart = MarketHistoryConverter.toTrendChartData(candles, TimeHorizon.ONE_WEEK, 23000000L)
        assertNotNull(chart)
        assertEquals(4, chart.points.size)
        assertEquals(23900000L, chart.peakPrice)
        assertEquals(2 / 3.0f, chart.peakXRatio, 0.01f)
        assertTrue(chart.points.all { it.first in 0.0f..1.0f && it.second in 0.10f..0.90f })
        assertEquals(4, chart.timeLabels.size)
        assertEquals("امروز", chart.timeLabels.last())
    }

    @Test
    fun realWeekCandlesTransformToFiveDistributedTimeLabels() {
        val candles = (1..7).map { day ->
            MarketCandle(
                open = 23000000L,
                high = 23500000L,
                low = 22800000L,
                close = 23200000L + day * 10000L,
                dateShamsi = "140506${String.format(java.util.Locale.US, "%02d", day)}"
            )
        }
        val chart = MarketHistoryConverter.toTrendChartData(candles, TimeHorizon.ONE_WEEK, 23000000L)
        assertNotNull(chart)
        assertEquals(7, chart.points.size)
        assertEquals(5, chart.timeLabels.size)
        assertEquals("امروز", chart.timeLabels.last())
    }

    @Test
    fun realCandlesGenerateAccurateMonthlyStats() {
        val candles = (1..30).map { day ->
            MarketCandle(
                open = 20000000L + day * 100000L,
                high = 20500000L + day * 100000L,
                low = 19800000L + day * 100000L,
                close = 20200000L + day * 100000L,
                dateShamsi = "140506${String.format(java.util.Locale.US, "%02d", day)}"
            )
        }

        val stats = MarketHistoryConverter.toMonthlyMarketStats(candles, 22000000L)
        assertEquals(23500000L, stats.thirtyDayHigh)
        assertEquals(19900000L, stats.thirtyDayLow)
        assertTrue(stats.weightedAverage in 19900000L..23500000L)
    }

    @Test
    fun marketRateDetailStateIncorporatesRealHistoryMap() {
        val weekCandles = listOf(
            MarketCandle(open = 23000000L, high = 23200000L, low = 22900000L, close = 23100000L, dateShamsi = "14050620"),
            MarketCandle(open = 23100000L, high = 23500000L, low = 23000000L, close = 23400000L, dateShamsi = "14050621")
        )
        val historyMap = mapOf(TimeHorizon.ONE_WEEK to weekCandles)

        val state = MarketRateDetailState.create(
            type = MarketRateItemType.GOLD_18K,
            rates = sampleRates,
            historyByHorizon = historyMap
        )

        assertNotNull(state)
        val weekChart = state.chartDataByHorizon[TimeHorizon.ONE_WEEK]
        assertNotNull(weekChart)
        assertEquals(23500000L, weekChart!!.peakPrice)
        assertEquals(2, weekChart.points.size)
    }
}
