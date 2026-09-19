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
        assertEquals(10.0 * (999.0 / 750.0), item24k.weightIn18kGrams, 0.001)

        val item17k = InventoryItem(
            code = "OLD-01",
            title = "طلای سنتی ۷۰۵",
            grossWeightGrams = 10.0,
            stoneWeightGrams = 0.0,
            karat = Karat.K17 // 705/750 = 0.94
        )
        assertEquals(10.0 * (705.0 / 750.0), item17k.weightIn18kGrams, 0.001)
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
}
