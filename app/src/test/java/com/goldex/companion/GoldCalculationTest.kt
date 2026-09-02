package com.goldex.companion

import com.goldex.companion.model.Karat
import com.goldex.companion.model.PersianNumberFormatter
import com.goldex.companion.model.PriceBasis
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class GoldCalculationTest {

    @Test
    fun testKaratRatios() {
        assertEquals(0.75, Karat.K18.purityRatio, 0.001)
        assertEquals(0.875, Karat.K21.purityRatio, 0.001)
        assertEquals(1.0, Karat.K24.purityRatio, 0.001)
    }

    @Test
    fun testPersianFormatterConversion() {
        val english = "1234567890"
        val persian = PersianNumberFormatter.toPersianDigits(english)
        assertEquals("۱۲۳۴۵۶۷۸۹۰", persian)

        val parsed = PersianNumberFormatter.parsePersianOrEnglish("۱۲,۵۰۰")
        assertNotNull(parsed)
        assertEquals(12500.0, parsed!!, 0.001)
    }

    @Test
    fun testTradeCalculation() {
        val weight = 10.0 // 10 grams of 18k
        val spotPrice18k = 4_000_000.0 // 4,000,000 per gram of 18k
        val margin = 7.0 // 7%

        val pureWeight = weight * Karat.K18.purityRatio // 7.5 grams
        val pureGramPrice = spotPrice18k / PriceBasis.PER_GRAM_18K.ratio // 4,000,000 / 0.75
        val rawGoldValue = pureWeight * pureGramPrice // 40,000,000
        val marginAmount = rawGoldValue * (margin / 100.0) // 2,800,000
        val totalTradeValue = rawGoldValue + marginAmount // 42,800,000

        assertEquals(7.5, pureWeight, 0.001)
        assertEquals(40_000_000.0, rawGoldValue, 0.001)
        assertEquals(2_800_000.0, marginAmount, 0.001)
        assertEquals(42_800_000.0, totalTradeValue, 0.001)
    }
}
