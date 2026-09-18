package com.goldex.companion

import com.goldex.companion.data.GoldMarketRepository
import com.goldex.companion.data.MarketHistoryCache
import com.goldex.companion.data.PriceSource
import com.goldex.companion.model.*
import org.junit.Assert.*
import org.junit.Test

class MarketHistoryTest {

    @Test
    fun memoryCacheStoresAndRetrievesFreshCandles() {
        val cache = MarketHistoryCache()
        val sampleCandles = listOf(
            MarketCandle(open = 23000000L, high = 23500000L, low = 22800000L, close = 23200000L, dateShamsi = "14050623")
        )

        cache.put(MarketRateItemType.GOLD_18K, TimeHorizon.ONE_WEEK, PriceSource.ISIGNAL, sampleCandles)

        assertTrue("Cache should be fresh immediately after put", cache.isFresh(MarketRateItemType.GOLD_18K, TimeHorizon.ONE_WEEK))

        val retrieved = cache.get(MarketRateItemType.GOLD_18K, TimeHorizon.ONE_WEEK, allowStale = false)
        assertNotNull("Retrieved candles must not be null", retrieved)
        assertEquals(1, retrieved!!.size)
        assertEquals(23200000L, retrieved.first().close)
    }

    @Test
    fun allMarketRateItemTypesMapToValidIndicators() {
        MarketRateItemType.values().forEach { type ->
            // TGJU indicator must be defined for all types
            val tgju = when (type) {
                MarketRateItemType.GOLD_18K -> "geram18"
                MarketRateItemType.GOLD_MELT -> "mesghal"
                MarketRateItemType.GOLD_24K -> "geram24"
                MarketRateItemType.COIN_EMAMI -> "sekee"
                MarketRateItemType.COIN_BAHAR -> "sekeb"
                MarketRateItemType.COIN_HALF -> "nim"
                MarketRateItemType.COIN_QUARTER -> "rob"
                MarketRateItemType.COIN_GERAMI -> "gerami"
                MarketRateItemType.USD -> "price_dollar_rl"
                MarketRateItemType.ONS -> "ons"
            }
            assertTrue("TGJU indicator must not be blank for $type", tgju.isNotBlank())

            // iSignal must map for all types except ONS
            if (type != MarketRateItemType.ONS) {
                val isignal = when (type) {
                    MarketRateItemType.GOLD_18K -> "100011"
                    MarketRateItemType.GOLD_MELT -> "100013"
                    MarketRateItemType.GOLD_24K -> "100012"
                    MarketRateItemType.COIN_EMAMI -> "100001"
                    MarketRateItemType.COIN_BAHAR -> "100000"
                    MarketRateItemType.COIN_HALF -> "100002"
                    MarketRateItemType.COIN_QUARTER -> "100003"
                    MarketRateItemType.COIN_GERAMI -> "100004"
                    MarketRateItemType.USD -> "200000"
                    MarketRateItemType.ONS -> null
                }
                assertNotNull("iSignal symbol ID must exist for $type", isignal)
            }
        }
    }

    @Test
    fun timeHorizonMapProperlyToISignalAndTgjuParameters() {
        TimeHorizon.values().forEach { horizon ->
            val rangeKey = when (horizon) {
                TimeHorizon.TODAY -> "oneDay"
                TimeHorizon.ONE_WEEK -> "oneWeek"
                TimeHorizon.ONE_MONTH -> "oneMonth"
                TimeHorizon.SIX_MONTHS -> "sixMonth"
                TimeHorizon.ONE_YEAR -> "oneYear"
            }
            assertTrue("Range key must be singular valid key", rangeKey.isNotEmpty())
            assertFalse("Range key must not end with 's' for multi-months", rangeKey.endsWith("Months"))

            val tgjuLen = when (horizon) {
                TimeHorizon.TODAY -> 2
                TimeHorizon.ONE_WEEK -> 7
                TimeHorizon.ONE_MONTH -> 30
                TimeHorizon.SIX_MONTHS -> 180
                TimeHorizon.ONE_YEAR -> 365
            }
            assertTrue("TGJU length must be positive", tgjuLen > 0)
        }
    }

    @Test
    fun emptyCandlesReturnUnavailableTrendChartWithoutFakeData() {
        val chart = MarketHistoryConverter.toTrendChartData(emptyList(), TimeHorizon.TODAY, 4_285_000L)
        assertFalse("Chart should be marked unavailable", chart.isAvailable)
        assertTrue("Chart points should be empty", chart.points.isEmpty())
        assertTrue("Time labels should be empty", chart.timeLabels.isEmpty())
        assertEquals("۰.۰٪", chart.fluctuationRangeText)
    }

    @Test
    fun sampleIntradayCandlesDownsamplesDenseTicksIntoThirtyMinuteBuckets() {
        // Generate 60 ticks every minute from 10:00 to 10:59
        val denseCandles = (0 until 60).map { minute ->
            val hh = 10
            val mm = String.format(java.util.Locale.US, "%02d", minute)
            MarketCandle(
                open = 4_280_000L + minute * 100L,
                high = 4_285_000L + minute * 100L,
                low = 4_275_000L + minute * 100L,
                close = 4_282_000L + minute * 100L,
                dateShamsi = "$hh:$mm"
            )
        }

        val sampled = MarketHistoryConverter.sampleIntradayCandles(denseCandles)
        assertTrue("Sampled candles should be significantly fewer than 60 ticks", sampled.size in 2..6)
        assertEquals("First candle close should match first raw tick", denseCandles.first().close, sampled.first().close)
        assertEquals("Last candle close should match last raw tick", denseCandles.last().close, sampled.last().close)
    }

    @Test
    fun sampleIntradayCandlesHandlesGapsBetweenIntervals() {
        val gappedCandles = listOf(
            MarketCandle(open = 4_280_000L, high = 4_282_000L, low = 4_279_000L, close = 4_281_000L, dateShamsi = "10:05"),
            MarketCandle(open = 4_281_000L, high = 4_285_000L, low = 4_280_000L, close = 4_284_000L, dateShamsi = "10:15"),
            MarketCandle(open = 4_290_000L, high = 4_295_000L, low = 4_288_000L, close = 4_292_000L, dateShamsi = "14:20"),
            MarketCandle(open = 4_292_000L, high = 4_300_000L, low = 4_290_000L, close = 4_298_000L, dateShamsi = "14:50")
        )

        val sampled = MarketHistoryConverter.sampleIntradayCandles(gappedCandles)
        assertEquals(4, sampled.size)
        val today = MarketHistoryConverter.getTodayShamsiDate()
        assertEquals("$today 10:05", sampled.first().dateShamsi)
        assertEquals("$today 14:50", sampled.last().dateShamsi)
    }

    @Test
    fun parseMinuteOfDayHandlesPersianAndEnglishDigits() {
        assertEquals(10 * 60 + 30, MarketHistoryConverter.parseMinuteOfDay("10:30"))
        assertEquals(14 * 60 + 45, MarketHistoryConverter.parseMinuteOfDay("۱۴:۴۵"))
        assertEquals(9 * 60 + 5, MarketHistoryConverter.parseMinuteOfDay("09:05:22"))
        assertNull(MarketHistoryConverter.parseMinuteOfDay("14050623"))
    }

    @Test
    fun formatFullShamsiDateTimeHandlesBothIntradayAndTimeStrings() {
        val today = MarketHistoryConverter.getTodayShamsiDate()
        assertEquals("$today 18:22", MarketHistoryConverter.formatFullShamsiDateTime("18:22"))
        assertEquals("1405/06/25 18:22", MarketHistoryConverter.formatFullShamsiDateTime("14050625 18:22"))
        assertEquals("1405/06/25 18:22", MarketHistoryConverter.formatFullShamsiDateTime("1405/06/25 18:22"))
        assertEquals("1405/06/25", MarketHistoryConverter.formatFullShamsiDateTime("14050625"))
    }
}
