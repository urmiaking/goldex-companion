package com.goldex.companion

import androidx.compose.ui.text.AnnotatedString
import com.goldex.companion.data.MarketRates
import com.goldex.companion.data.PriceSource
import com.goldex.companion.model.*
import com.goldex.companion.ui.util.ThousandsSeparatorVisualTransformation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
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

        val backToEnglish = PersianNumberFormatter.toEnglishDigits("۱۲,۵۰۰٫۵")
        assertEquals("12,500.5", backToEnglish)

        val parsed = PersianNumberFormatter.parsePersianOrEnglish("۱۲,۵۰۰")
        assertNotNull(parsed)
        assertEquals(12500.0, parsed!!, 0.001)

        val parsedLong = PersianNumberFormatter.parseToCleanLong("۳,۷۲۰,۰۰۰")
        assertEquals(3_720_000L, parsedLong)
    }

    @Test
    fun testPersianWordsFormatter() {
        assertEquals("صفر تومان", PersianWordsFormatter.toWords(0))
        assertEquals("یک میلیون تومان", PersianWordsFormatter.toWords(1_000_000))
        assertEquals("سه میلیون و هفتصد و بیست هزار تومان", PersianWordsFormatter.toWords(3_720_000))
    }

    @Test
    fun testVisualTransformationOffsetMapping() {
        val transformation = ThousandsSeparatorVisualTransformation(isPersian = false)
        val input = AnnotatedString("1234567")
        val transformed = transformation.filter(input)

        // "1234567" -> "1،234،567"
        assertEquals("1،234،567", transformed.text.text)

        // Test boundary offset mappings
        val mapping = transformed.offsetMapping
        assertEquals(0, mapping.originalToTransformed(0))
        assertEquals(9, mapping.originalToTransformed(7))
        assertEquals(0, mapping.transformedToOriginal(0))
        assertEquals(7, mapping.transformedToOriginal(9))
    }

    @Test
    fun testMeltGoldMesghalConversion() {
        // Standard formula: mesghal / 4.33185 = gram 18K
        val mesghalPrice = 16_115_000.0
        val gram18k = (mesghalPrice / 4.33185).toLong()
        assertEquals(3_720_119L, gram18k)
    }

    @Test
    fun testCoinBubbleCalculation() {
        val emami = CoinType.EMAMI
        assertEquals(8.13598, emami.totalWeightGrams, 0.001)
        assertEquals(7.322382, emami.pureWeightGrams, 0.001)

        val ons = 2500.0
        val usd = 60_000.0
        val gram24Price = (ons * usd) / 31.1035
        val intrinsic = (emami.pureWeightGrams * gram24Price) + emami.mintFee
        val marketPrice = 42_000_000.0
        val bubble = marketPrice - intrinsic

        assertTrue(intrinsic > 30_000_000.0)
        assertTrue(bubble > 0.0)
    }

    @Test
    fun testDetailedJewelryCalculationWithTax() {
        val grossWeight = 12.0
        val stoneWeight = 2.0
        val netWeight = grossWeight - stoneWeight
        val spot18k = 4_000_000L
        val wagePercent = 10.0
        val profitPercent = 7.0
        val taxPercent = 9.0

        val pureGramSpot = spot18k.toDouble() / (18.0 / 24.0)
        val rawValue = netWeight * Karat.K18.purityRatio * pureGramSpot
        val wageAmount = rawValue * (wagePercent / 100.0)
        val profitAmount = (rawValue + wageAmount) * (profitPercent / 100.0)
        val taxAmount = (wageAmount + profitAmount) * (taxPercent / 100.0)
        val totalPayable = rawValue + wageAmount + profitAmount + taxAmount

        assertEquals(40_000_000.0, rawValue, 0.001)
        assertEquals(4_000_000.0, wageAmount, 0.001)
        assertEquals(3_080_000.0, profitAmount, 0.001)
        assertEquals(637_200.0, taxAmount, 0.001)
        assertEquals(47_717_200.0, totalPayable, 0.001)
    }

    @Test
    fun testMarketRatesAndProviders() {
        val rates = MarketRates(
            gold18 = 22_835_100L,
            source = PriceSource.ISIGNAL
        )
        assertEquals(22_835_100L, rates.gold18)
        assertEquals(PriceSource.ISIGNAL, rates.source)
        assertEquals("آی‌سیگنال (isignal.ir)", rates.source.labelFa)
    }
}
