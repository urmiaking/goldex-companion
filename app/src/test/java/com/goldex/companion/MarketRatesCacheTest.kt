package com.goldex.companion

import com.goldex.companion.data.MarketRateItemSummary
import com.goldex.companion.data.MarketRates
import com.goldex.companion.data.MarketRatesCache
import com.goldex.companion.data.PriceSource
import com.goldex.companion.model.MarketRateItemType
import org.junit.Assert.*
import org.junit.Test

class MarketRatesCacheTest {

    @Test
    fun returnsNullWhenCacheIsEmpty() {
        val cache = MarketRatesCache()
        assertNull(cache.getRates())
        assertTrue(cache.getItemSummaries().isEmpty())
    }

    @Test
    fun inMemoryRatesPutAndGetRoundTrip() {
        val cache = MarketRatesCache()
        val rates = MarketRates(
            gold18 = 4285000L,
            gold24 = 5713000L,
            goldMelt = 18560000L,
            coinEmami = 51200000L,
            usd = 68500L,
            ons = 2715.5,
            source = PriceSource.ISIGNAL,
            lastUpdated = "12:30:00",
            isLive = true
        )

        cache.putRates(rates)
        val retrieved = cache.getRates()

        assertNotNull(retrieved)
        assertEquals(rates.gold18, retrieved!!.gold18)
        assertEquals(rates.gold24, retrieved.gold24)
        assertEquals(rates.goldMelt, retrieved.goldMelt)
        assertEquals(rates.coinEmami, retrieved.coinEmami)
        assertEquals(rates.usd, retrieved.usd)
        assertEquals(rates.ons, retrieved.ons, 0.001)
        assertEquals(PriceSource.ISIGNAL, retrieved.source)
        assertEquals("12:30:00", retrieved.lastUpdated)
        assertTrue(retrieved.isLive)
    }

    @Test
    fun inMemoryItemSummariesPutAndGetRoundTrip() {
        val cache = MarketRatesCache()
        val summaries = mapOf(
            MarketRateItemType.GOLD_18K to MarketRateItemSummary(
                type = MarketRateItemType.GOLD_18K,
                currentPrice = 4285000L,
                dayLow = 4250000L,
                dayHigh = 4300000L,
                openPrice = 4250000L,
                changeAmount = 35000L,
                changePercent = 0.82,
                isPositive = true
            ),
            MarketRateItemType.COIN_EMAMI to MarketRateItemSummary(
                type = MarketRateItemType.COIN_EMAMI,
                currentPrice = 51200000L,
                dayLow = 50800000L,
                dayHigh = 51500000L,
                openPrice = 51500000L,
                changeAmount = -300000L,
                changePercent = -0.58,
                isPositive = false
            )
        )

        cache.putItemSummaries(summaries)
        val retrieved = cache.getItemSummaries()

        assertEquals(2, retrieved.size)
        val gold18 = retrieved[MarketRateItemType.GOLD_18K]
        assertNotNull(gold18)
        assertEquals(4285000L, gold18!!.currentPrice)
        assertTrue(gold18.isPositive)

        val emami = retrieved[MarketRateItemType.COIN_EMAMI]
        assertNotNull(emami)
        assertEquals(51200000L, emami!!.currentPrice)
        assertFalse(emami.isPositive)
    }

    @Test
    fun clearWipesAllCacheEntries() {
        val cache = MarketRatesCache()
        val rates = MarketRates(gold18 = 4285000L)
        cache.putRates(rates)

        val summaries = mapOf(
            MarketRateItemType.GOLD_18K to MarketRateItemSummary(
                type = MarketRateItemType.GOLD_18K,
                currentPrice = 4285000L
            )
        )
        cache.putItemSummaries(summaries)

        assertNotNull(cache.getRates())
        assertEquals(1, cache.getItemSummaries().size)

        cache.clear()

        assertNull(cache.getRates())
        assertTrue(cache.getItemSummaries().isEmpty())
    }
}
