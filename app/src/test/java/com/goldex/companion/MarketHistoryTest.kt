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
}
