package com.goldex.companion

import com.goldex.companion.model.*
import org.junit.Assert.*
import org.junit.Test

class InvoiceTest {

    @Test
    fun testMultiItemInvoiceAggregation() {
        val item1 = InvoiceItem(
            id = "item_1",
            title = "دستبند کارتیه",
            karat = Karat.K18,
            grossWeight = 10.0,
            stoneWeight = 0.5,
            netWeight = 9.5,
            spotPrice = 20_000_000L,
            wageType = WageType.PERCENTAGE,
            wageInput = 10.0,
            wageAmount = 19_000_000.0,
            profitPercent = 7.0,
            profitAmount = 14_630_000.0,
            taxPercent = 9.0,
            taxAmount = 3_026_700.0,
            rawGoldValue = 190_000_000.0,
            totalPayable = 226_656_700.0,
            effectiveGramPrice = 226_656_700.0 / 9.5
        )

        val item2 = InvoiceItem(
            id = "item_2",
            title = "انگشتر برلیان",
            karat = Karat.K18,
            grossWeight = 5.0,
            stoneWeight = 0.0,
            netWeight = 5.0,
            spotPrice = 20_000_000L,
            wageType = WageType.PERCENTAGE,
            wageInput = 12.0,
            wageAmount = 12_000_000.0,
            profitPercent = 7.0,
            profitAmount = 7_840_000.0,
            taxPercent = 9.0,
            taxAmount = 1_785_600.0,
            rawGoldValue = 100_000_000.0,
            totalPayable = 121_625_600.0,
            effectiveGramPrice = 121_625_600.0 / 5.0
        )

        val customer = Customer(
            name = "حاج احمد کریمی",
            phone = "۰۹۱۲۳۴۵۶۷۸۹",
            nationalId = "۰۰۱۲۳۴۵۶۷۸",
            note = "مشتری VIP"
        )

        val invoice = Invoice(
            invoiceNumber = "123456",
            customer = customer,
            items = listOf(item1, item2)
        )

        assertEquals(15.0, invoice.totalGrossWeight, 0.001)
        assertEquals(0.5, invoice.totalStoneWeight, 0.001)
        assertEquals(14.5, invoice.totalNetWeight, 0.001)
        assertEquals(290_000_000.0, invoice.totalRawGoldValue, 0.001)
        assertEquals(31_000_000.0, invoice.totalWageAmount, 0.001)
        assertEquals(22_470_000.0, invoice.totalProfitAmount, 0.001)
        assertEquals(4_812_300.0, invoice.totalTaxAmount, 0.001)
        assertEquals(348_282_300.0, invoice.totalPayable, 0.001)
        assertEquals(348_282_300.0 / 14.5, invoice.effectiveGramPrice, 0.001)

        val text = invoice.formatTextInvoice("اتحادیه طلا")
        assertTrue(text.contains("حاج احمد کریمی"))
        assertTrue(text.contains("دستبند کارتیه"))
        assertTrue(text.contains("انگشتر برلیان"))
        assertTrue(text.contains("123456") || text.contains(PersianNumberFormatter.toPersianDigits("123456")))
    }

    @Test
    fun testPersianNumberFormatting() {
        assertEquals("۱۲۳۴۵", PersianNumberFormatter.toPersianDigits("12345"))
        assertEquals("12345", PersianNumberFormatter.toEnglishDigits("۱۲۳۴۵"))
        assertEquals(12345L, PersianNumberFormatter.parseToCleanLong("۱۲,۳۴۵"))
        assertEquals(12.345, PersianNumberFormatter.parsePersianOrEnglish("۱۲.۳۴۵")!!, 0.001)
    }

    @Test
    fun testCustomerModelDefaults() {
        val cust = Customer(name = "محمد علیزاده")
        assertNotNull(cust.id)
        assertEquals("محمد علیزاده", cust.name)
        assertEquals("", cust.phone)
        assertEquals("", cust.nationalId)
        assertTrue(cust.createdAt > 0L)
    }

    @Test
    fun testFormatPercentWithoutUglyDecimals() {
        assertEquals("۷", PersianNumberFormatter.formatPercent(7.0))
        assertEquals("۹", PersianNumberFormatter.formatPercent(9.0))
        assertEquals("۱۲", PersianNumberFormatter.formatPercent(12.0))
        assertEquals("۷.۵", PersianNumberFormatter.formatPercent(7.5))
    }

    @Test
    fun testNoDuplicateTomanInTextInvoice() {
        val item = InvoiceItem(
            title = "دستبند",
            grossWeight = 10.0,
            netWeight = 10.0,
            spotPrice = 20_000_000L,
            wageInput = 10.0,
            wageAmount = 20_000_000.0,
            profitPercent = 7.0,
            profitAmount = 15_400_000.0,
            taxPercent = 9.0,
            taxAmount = 3_186_000.0,
            rawGoldValue = 200_000_000.0,
            totalPayable = 238_586_000.0,
            effectiveGramPrice = 23_858_600.0
        )
        val invoice = Invoice(items = listOf(item))
        val text = invoice.formatTextInvoice("اتحادیه")
        assertFalse("Invoice text must not contain duplicate 'تومان تومان'", text.contains("تومان تومان"))
    }

    @Test
    fun testCustomerSearchNormalization() {
        val persianPhone = "۰۹۱۲۳۴۵۶۷۸۹"
        val englishQuery = "0912"
        val normalizedPhone = PersianNumberFormatter.normalizeForSearch(persianPhone)
        val normalizedQuery = PersianNumberFormatter.normalizeForSearch(englishQuery)
        assertTrue(normalizedPhone.contains(normalizedQuery))

        val nationalId = "۰۰۱۲۳۴۵۶۷۸"
        val queryPersian = "۰۱۲"
        assertTrue(PersianNumberFormatter.normalizeForSearch(nationalId).contains(PersianNumberFormatter.normalizeForSearch(queryPersian)))
    }
}
