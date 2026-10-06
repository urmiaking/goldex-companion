package com.goldex.companion.presentation.inventory

import com.goldex.companion.model.*
import kotlin.test.*

class InventoryFormTest {
    private fun valid() = InventoryDraft(code = "RNG-123", title = "حلقه", gross = "2.125", stone = "0.125")
    @Test fun localizedDigitsAndMilligramPrecision() {
        val draft = valid().copy(gross = "۲٫۱۲۵", stone = "٠٫١٢٥", quantity = "۳", purity = "۷۵۰")
        assertTrue(InventoryForm.validate(draft).isEmpty())
        val item = InventoryForm.toItem(draft)
        assertEquals(2.0, item.netGoldWeightGrams)
        assertEquals(3, item.quantity)
    }
    @Test fun rejectsCoercionNonFiniteNegativeFractionalCountAndExtraPrecision() {
        for (value in listOf("NaN", "Infinity", "-1", "1.0001", "1e3", "0")) assertTrue("gross" in InventoryForm.validate(valid().copy(gross = value)))
        for (value in listOf("1.5", "0", "1000001", "999999999999999")) assertTrue("quantity" in InventoryForm.validate(valid().copy(quantity = value)))
        assertTrue("stone" in InventoryForm.validate(valid().copy(stone = "2.125")))
        assertTrue("purity" in InventoryForm.validate(valid().copy(purity = "99")))
    }
    @Test fun wholeTomanPreviewExactlyMatchesAndroidFormulaAcrossWageModesAndCategories() {
        for (category in InventoryCategory.values().filter { it != InventoryCategory.ALL }) for (type in WageType.values()) for (purity in listOf(750, 875, 999)) {
            val item = InventoryForm.toItem(valid().copy(category = category, purity = purity.toString(), wageType = type, wage = if (type == WageType.PERCENTAGE) "12.375" else "125000", tax = "9.5"))
            assertEquals(item.calculateEstimatedValue(6_123_457), InventoryForm.price(item, 6_123_457)!!.total)
        }
    }
    @Test fun vatIsOnlyOnWageAndProfitAndCoinsHaveNoProfit() {
        val item = InventoryForm.toItem(valid())
        val price = InventoryForm.price(item, 6_000_000)!!
        assertEquals(12_000_000L, price.raw)
        assertEquals(1_200_000L, price.wage)
        assertEquals(924_000L, price.profit)
        assertEquals(191_160L, price.tax)
        assertEquals(14_315_160L, price.total)
        assertEquals(0L, InventoryForm.price(item.copy(category = InventoryCategory.COINS, profitPercent = 50.0), 6_000_000)!!.profit)
    }
    @Test fun editRetainsIdentityCreationDateAndAllFields() {
        val original = InventoryForm.toItem(valid()).copy(quantity = 0, imageUrl = "example", rfidTag = "rfid", customKaratValue = 875)
        assertEquals(original, InventoryForm.toItem(InventoryForm.from(original)))
    }
    @Test fun stockCanReachZeroButNeverNegativeOrOverflow() {
        val item = InventoryForm.toItem(valid()).copy(quantity = 3)
        assertEquals(0, InventoryForm.adjustedQuantity(item, StockAdjustmentType.DEDUCT, 3))
        assertEquals(5, InventoryForm.adjustedQuantity(item, StockAdjustmentType.CHARGE, 2))
        assertFailsWith<IllegalArgumentException> { InventoryForm.adjustedQuantity(item, StockAdjustmentType.DEDUCT, 4) }
        assertFailsWith<IllegalArgumentException> { InventoryForm.adjustedQuantity(item, StockAdjustmentType.CHARGE, Int.MAX_VALUE) }
        assertFailsWith<IllegalArgumentException> { InventoryForm.adjustedQuantity(item, StockAdjustmentType.CHARGE, 0) }
    }
    @Test fun persianArabicCodeRfidAndLocationSearch() {
        val item = InventoryForm.toItem(valid().copy(title = "حلقه كیانا", code = "RNG-123", rfid = "ABC", location = "سینی 2"))
        for (query in listOf("کیانا", "rng-۱۲۳", "abc", "سینی ۲")) assertTrue(InventoryForm.matches(item, query))
        assertFalse(InventoryForm.matches(item, "ناموجود"))
    }
    @Test fun invalidDraftOrMissingQuoteHasNoPrice() {
        assertNull(InventoryForm.preview(valid().copy(stone = "9"), 6_000_000))
        assertNull(InventoryForm.preview(valid(), 0))
    }
}
