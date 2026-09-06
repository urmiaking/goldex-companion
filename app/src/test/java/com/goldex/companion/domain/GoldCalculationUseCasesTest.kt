package com.goldex.companion.domain

import com.goldex.companion.data.MarketRates
import com.goldex.companion.data.PortfolioCategory
import com.goldex.companion.data.PortfolioItem
import com.goldex.companion.domain.calculator.GoldCalculationUseCases
import com.goldex.companion.domain.portfolio.PortfolioValuation
import com.goldex.companion.model.CoinType
import com.goldex.companion.model.Karat
import com.goldex.companion.model.PriceBasisTab
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

    @Test
    fun priceBasisConversionToAndFrom18k() {
        val base18k = 23_360_000L

        // 18k basis returns same value
        assertEquals(base18k, GoldCalculationUseCases.toSpotPrice18k(base18k, PriceBasisTab.K18))
        assertEquals(base18k, GoldCalculationUseCases.fromSpotPrice18k(base18k, PriceBasisTab.K18))

        // 24k conversion
        val expected24k = 31_146_667L // 23_360_000 * 24 / 18
        assertEquals(expected24k, GoldCalculationUseCases.fromSpotPrice18k(base18k, PriceBasisTab.K24))
        assertEquals(base18k, GoldCalculationUseCases.toSpotPrice18k(expected24k, PriceBasisTab.K24))

        // Mesghal conversion
        val expectedMesghal = 101_192_016L // 23_360_000 * 4.33185
        assertEquals(expectedMesghal, GoldCalculationUseCases.fromSpotPrice18k(base18k, PriceBasisTab.MESGHAL))
        assertEquals(base18k, GoldCalculationUseCases.toSpotPrice18k(expectedMesghal, PriceBasisTab.MESGHAL))

        // Edge cases
        assertEquals(0L, GoldCalculationUseCases.toSpotPrice18k(0L, PriceBasisTab.MESGHAL))
        assertEquals(0L, GoldCalculationUseCases.fromSpotPrice18k(0L, PriceBasisTab.K24))
    }

    @Test
    fun priceBasisConversionMaintainsCalculationConsistency() {
        val spot18k = 23_360_000L
        val spot24k = GoldCalculationUseCases.fromSpotPrice18k(spot18k, PriceBasisTab.K24)
        val spotMesghal = GoldCalculationUseCases.fromSpotPrice18k(spot18k, PriceBasisTab.MESGHAL)

        val res18k = GoldCalculationUseCases.calculateJewelry(
            grossWeight = 10.0,
            stoneWeight = 0.0,
            karat = Karat.K18,
            spotPrice18k = GoldCalculationUseCases.toSpotPrice18k(spot18k, PriceBasisTab.K18),
            wageType = WageType.PERCENTAGE,
            wageInput = 10.0,
            profitPercent = 7.0,
            taxPercent = 9.0
        )

        val res24k = GoldCalculationUseCases.calculateJewelry(
            grossWeight = 10.0,
            stoneWeight = 0.0,
            karat = Karat.K18,
            spotPrice18k = GoldCalculationUseCases.toSpotPrice18k(spot24k, PriceBasisTab.K24),
            wageType = WageType.PERCENTAGE,
            wageInput = 10.0,
            profitPercent = 7.0,
            taxPercent = 9.0
        )

        val resMesghal = GoldCalculationUseCases.calculateJewelry(
            grossWeight = 10.0,
            stoneWeight = 0.0,
            karat = Karat.K18,
            spotPrice18k = GoldCalculationUseCases.toSpotPrice18k(spotMesghal, PriceBasisTab.MESGHAL),
            wageType = WageType.PERCENTAGE,
            wageInput = 10.0,
            profitPercent = 7.0,
            taxPercent = 9.0
        )

        assertNotNull(res18k)
        assertNotNull(res24k)
        assertNotNull(resMesghal)

        // All three should have virtually identical total payable
        assertEquals(res18k!!.rawGoldValue, res24k!!.rawGoldValue, 1.0)
        assertEquals(res18k.rawGoldValue, resMesghal!!.rawGoldValue, 1.0)
        assertEquals(res18k.totalPayable, res24k.totalPayable, 1.0)
        assertEquals(res18k.totalPayable, resMesghal.totalPayable, 1.0)
    }
}
