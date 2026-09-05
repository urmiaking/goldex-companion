package com.goldex.companion.domain

import com.goldex.companion.data.MarketRates
import com.goldex.companion.data.PortfolioCategory
import com.goldex.companion.data.PortfolioItem
import com.goldex.companion.domain.calculator.GoldCalculationUseCases
import com.goldex.companion.domain.portfolio.PortfolioValuation
import com.goldex.companion.model.CoinType
import com.goldex.companion.model.Karat
import com.goldex.companion.model.WageType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class GoldCalculationUseCasesTest {
    @Test
    fun jewelryCalculationPreservesVatPolicy() {
        val result = GoldCalculationUseCases.calculateJewelry(
            grossWeight = 12.0,
            stoneWeight = 2.0,
            karat = Karat.K18,
            spotPrice18k = 4_000_000L,
            wageType = WageType.PERCENTAGE,
            wageInput = 10.0,
            profitPercent = 7.0,
            taxPercent = 9.0
        )

        assertNotNull(result)
        assertEquals(40_000_000.0, result!!.rawGoldValue, 0.001)
        assertEquals(4_000_000.0, result.wageAmount, 0.001)
        assertEquals(3_080_000.0, result.profitAmount, 0.001)
        assertEquals(637_200.0, result.taxAmount, 0.001)
        assertEquals(47_717_200.0, result.totalPayable, 0.001)
    }

    @Test
    fun invalidJewelryInputProducesNoResult() {
        val result = GoldCalculationUseCases.calculateJewelry(
            grossWeight = 1.0,
            stoneWeight = 1.0,
            karat = Karat.K18,
            spotPrice18k = 4_000_000L,
            wageType = WageType.PERCENTAGE,
            wageInput = 10.0,
            profitPercent = 7.0,
            taxPercent = 9.0
        )

        assertEquals(null, result)
    }

    @Test
    fun meltAndKaratConversionRemainStable() {
        val melt = GoldCalculationUseCases.calculateMelt(16_115_000.0, 10.0)
        assertEquals(3_720_119L, melt.gram18kPrice)
        assertEquals(37_201_190.0, melt.totalValue, 0.001)

        val converted = GoldCalculationUseCases.calculateKaratConversion(10.0, Karat.K18, Karat.K24)
        assertEquals(7.5, converted, 0.001)
    }

    @Test
    fun coinBubbleAndPortfolioSummaryRemainStable() {
        val coin = GoldCalculationUseCases.calculateCoinBubble(
            coin = CoinType.EMAMI,
            marketPrice = 234_000_000.0,
            usd = 221_500L,
            ounce = 4435.0
        )
        assertEquals(234_000_000.0, coin.marketPrice, 0.001)
        assertEquals(7.322382, CoinType.EMAMI.pureWeightGrams, 0.001)

        val rates = MarketRates(gold18 = 20_000_000L, coinEmami = 200_000_000L)
        val items = listOf(
            PortfolioItem(
                title = "دستبند",
                category = PortfolioCategory.GOLD,
                weightGrams = 10.0,
                karat = Karat.K18,
                purchasePriceTotal = 180_000_000L
            ),
            PortfolioItem(
                title = "سکه",
                category = PortfolioCategory.COIN,
                quantity = 2,
                coinType = CoinType.EMAMI,
                purchasePriceTotal = 380_000_000L
            )
        )
        val summary = PortfolioValuation.summarize(items, rates)
        assertEquals(600_000_000L, summary.currentValue)
        assertEquals(560_000_000L, summary.purchaseValue)
        assertEquals(40_000_000L, summary.profit)
        assertEquals(7.1428, summary.profitPercent, 0.001)
    }
}
