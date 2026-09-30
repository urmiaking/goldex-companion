package com.goldex.companion.domain

import com.goldex.companion.model.InventoryCategory
import com.goldex.companion.model.InventoryItem
import com.goldex.companion.model.Karat
import org.junit.Assert.assertEquals
import org.junit.Test

class InventoryCalculationTest {

    @Test
    fun netGoldWeightIsAccuratelyCalculated() {
        val item = InventoryItem(
            code = "RNG-104",
            title = "انگشتر نگین‌دار",
            category = InventoryCategory.RINGS,
            grossWeightGrams = 5.420,
            stoneWeightGrams = 0.150,
            karat = Karat.K18
        )

        assertEquals(5.270, item.netGoldWeightGrams, 0.0001)
    }

    @Test
    fun netGoldWeightCannotBeNegative() {
        val item = InventoryItem(
            code = "TEST-01",
            title = "تست سنگ سنگین",
            grossWeightGrams = 2.0,
            stoneWeightGrams = 3.0
        )

        assertEquals(0.0, item.netGoldWeightGrams, 0.0001)
    }

    @Test
    fun equivalent18kWeightConvertsCorrectlyAcrossKarats() {
        val item24k = InventoryItem(
            code = "BAR-01",
            title = "شمش ۲۴ عیار",
            grossWeightGrams = 10.0,
            stoneWeightGrams = 0.0,
            karat = Karat.K24 // 999/750 = 1.332
        )
        assertEquals(10.0 * (Karat.K24.purityRatio / Karat.K18.purityRatio), item24k.weightIn18kGrams, 0.001)

        val item21k = InventoryItem(
            code = "GULF-01",
            title = "طلای ۲۱ عیار خلیجی",
            grossWeightGrams = 10.0,
            stoneWeightGrams = 0.0,
            karat = Karat.K21
        )
        assertEquals(10.0 * (Karat.K21.purityRatio / Karat.K18.purityRatio), item21k.weightIn18kGrams, 0.001)
    }

    @Test
    fun estimatedRetailValueFollowsOfficialGuildFormula() {
        val spot18k = 20_000_000L
        val item = InventoryItem(
            code = "SET-01",
            title = "نیم‌ست ون‌کلیف",
            grossWeightGrams = 10.0,
            stoneWeightGrams = 0.0,
            karat = Karat.K18,
            wagePercent = 10.0,
            profitPercent = 7.0,
            taxPercent = 9.0
        )

        // Raw = 10 * 20_000_000 = 200_000_000
        // Wage = 200_000_000 * 10% = 20_000_000
        // Profit = (200_000_000 + 20_000_000) * 7% = 220_000_000 * 0.07 = 15_400_000
        // Tax = (20_000_000 + 15_400_000) * 9% = 35_400_000 * 0.09 = 3_186_000
        // Total = 200M + 20M + 15.4M + 3.186M = 238_586_000
        val estimatedValue = item.calculateEstimatedValue(spot18k)
        assertEquals(238_586_000L, estimatedValue)
    }

    @Test
    fun estimatedRetailValueWithTomanPerGramWage() {
        val spot18k = 20_000_000L
        val item = InventoryItem(
            code = "CHAIN-01",
            title = "زنجیر ونیزی",
            category = InventoryCategory.NECKLACES,
            grossWeightGrams = 10.0,
            stoneWeightGrams = 0.0,
            karat = Karat.K18,
            wageType = com.goldex.companion.model.WageType.TOMAN_PER_GRAM,
            wageValue = 500_000.0, // 500k toman per gram -> 5,000,000 toman total wage
            profitPercent = 7.0,
            taxPercent = 9.0
        )

        // Raw = 10 * 20_000_000 = 200_000_000
        // Wage = 10 * 500_000 = 5_000_000
        // Profit = (200M + 5M) * 7% = 205_000_000 * 0.07 = 14_350_000
        // Tax = (5M + 14.35M) * 9% = 19_350_000 * 0.09 = 1_741_500
        // Total = 200M + 5M + 14.35M + 1.7415M = 221_091_500
        val estimatedValue = item.calculateEstimatedValue(spot18k)
        assertEquals(221_091_500L, estimatedValue)
    }

    @Test
    fun jewelryDefaultsToTwentyPercentProfitAndCoinsToZero() {
        val jewelry = InventoryItem(
            code = "JWL-1",
            title = "انگشتر برلیان",
            category = InventoryCategory.JEWELRY
        )
        assertEquals(20.0, jewelry.profitPercent, 0.001)

        val coin = InventoryItem(
            code = "COIN-1",
            title = "تمام بهار",
            category = InventoryCategory.COINS
        )
        assertEquals(0.0, coin.profitPercent, 0.001)
    }
}
