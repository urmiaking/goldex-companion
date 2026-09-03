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
    fun testVisualTransformationSeparated() {
        val transformation = ThousandsSeparatorVisualTransformation(isPersian = false, addSeparators = true)
        val input = AnnotatedString("1234567")
        val transformed = transformation.filter(input)

        // "1234567" -> "1،234،567"
        assertEquals("1،234،567", transformed.text.text)

        val mapping = transformed.offsetMapping
        assertEquals(0, mapping.originalToTransformed(0))
        assertEquals(9, mapping.originalToTransformed(7))
        assertEquals(0, mapping.transformedToOriginal(0))
        assertEquals(7, mapping.transformedToOriginal(9))
    }

    @Test
    fun testVisualTransformationPersianUnseparated() {
        val transformation = ThousandsSeparatorVisualTransformation(isPersian = true, addSeparators = false)
        val input = AnnotatedString("10.5")
        val transformed = transformation.filter(input)

        assertEquals("۱۰.۵", transformed.text.text)
    }

    @Test
    fun testMeltGoldMesghalConversion() {
        val mesghalPrice = 16_115_000.0
        val gram18k = (mesghalPrice / 4.33185).toLong()
        assertEquals(3_720_119L, gram18k)
    }

    @Test
    fun testCoinBubbleCalculation() {
        val emami = CoinType.EMAMI
        assertEquals(8.13598, emami.totalWeightGrams, 0.001)
        assertEquals(7.322382, emami.pureWeightGrams, 0.001)

        val ons = 4435.0
        val usd = 221_500.0
        val gram24Price = (ons * usd) / 31.1035
        val intrinsic = (emami.pureWeightGrams * gram24Price) + emami.mintFee
        val marketPrice = 234_000_000.0
        val bubble = marketPrice - intrinsic

        assertTrue(intrinsic > 200_000_000.0)
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
            gold18 = 23_360_000L,
            source = PriceSource.TGJU
        )
        assertEquals(23_360_000L, rates.gold18)
        assertEquals(PriceSource.TGJU, rates.source)
        assertEquals("شبکه طلا و ارز (tgju.org)", rates.source.labelFa)
    }

    @Test
    fun testPortfolioValuationAndProfit() {
        val rates = MarketRates(
            gold18 = 20_000_000L,
            coinEmami = 200_000_000L
        )

        // 10 grams of 18k gold bought at 18,000,000/g = 180,000,000 total
        val goldItem = com.goldex.companion.data.PortfolioItem(
            title = "دستبند طلا",
            category = com.goldex.companion.data.PortfolioCategory.GOLD,
            weightGrams = 10.0,
            karat = Karat.K18,
            purchasePriceTotal = 180_000_000L
        )

        val goldCurrentVal = goldItem.calculateCurrentValue(rates)
        assertEquals(200_000_000L, goldCurrentVal)
        assertEquals(20_000_000L, goldItem.calculateProfit(rates))
        assertEquals(11.11, goldItem.calculateProfitPercent(rates), 0.01)

        // 2 Emami coins bought at 190,000,000 each = 380,000,000 total
        val coinItem = com.goldex.companion.data.PortfolioItem(
            title = "سکه تمام",
            category = com.goldex.companion.data.PortfolioCategory.COIN,
            quantity = 2,
            coinType = CoinType.EMAMI,
            purchasePriceTotal = 380_000_000L
        )

        val coinCurrentVal = coinItem.calculateCurrentValue(rates)
        assertEquals(400_000_000L, coinCurrentVal)
        assertEquals(20_000_000L, coinItem.calculateProfit(rates))
    }
}
