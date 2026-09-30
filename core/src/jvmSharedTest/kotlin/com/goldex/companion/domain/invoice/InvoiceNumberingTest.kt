package com.goldex.companion.domain.invoice

import com.goldex.companion.domain.reporting.ShamsiCalendarHelper
import com.goldex.companion.model.BarterInvoice
import com.goldex.companion.model.Invoice
import com.goldex.companion.model.generateBarterInvoiceNumber
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class InvoiceNumberingTest {

    @Test
    fun testGeneratedInvoiceNumberContainsNoHyphen() {
        val invoiceNumber = generateBarterInvoiceNumber()
        assertFalse("Invoice number must not contain hyphen", invoiceNumber.contains("-"))
        assertTrue("Invoice number must be all digits", invoiceNumber.all { it.isDigit() })
    }

    @Test
    fun testGeneratedInvoiceNumberPrefixMatchesShamsiYearOfRegistration() {
        // Year 1405 (e.g. 2026-09-29)
        val cal1405 = Calendar.getInstance().apply {
            set(2026, Calendar.SEPTEMBER, 29, 12, 0, 0)
        }
        val invoiceNumber1405 = generateBarterInvoiceNumber(cal1405.timeInMillis)
        assertTrue("Invoice number should start with 1405", invoiceNumber1405.startsWith("1405"))
        assertEquals("Invoice number should be 7 digits (4 year + 3 serial)", 7, invoiceNumber1405.length)

        // Year 1404 (e.g. 2025-05-15)
        val cal1404 = Calendar.getInstance().apply {
            set(2025, Calendar.MAY, 15, 10, 0, 0)
        }
        val invoiceNumber1404 = generateBarterInvoiceNumber(cal1404.timeInMillis)
        assertTrue("Invoice number should start with 1404", invoiceNumber1404.startsWith("1404"))
        assertEquals(7, invoiceNumber1404.length)

        // Year 1403 (e.g. 2024-08-20)
        val cal1403 = Calendar.getInstance().apply {
            set(2024, Calendar.AUGUST, 20, 15, 0, 0)
        }
        val invoiceNumber1403 = generateBarterInvoiceNumber(cal1403.timeInMillis)
        assertTrue("Invoice number should start with 1403", invoiceNumber1403.startsWith("1403"))
        assertEquals(7, invoiceNumber1403.length)
    }

    @Test
    fun testBarterInvoiceDefaultNumberUsesShamsiYearWithoutHyphen() {
        val now = System.currentTimeMillis()
        val currentShamsiYear = ShamsiCalendarHelper.millisToShamsi(now).first.toString()
        val invoice = BarterInvoice()

        assertFalse("Default barter invoice number must not contain hyphen", invoice.invoiceNumber.contains("-"))
        assertTrue("Default barter invoice number should start with current year", invoice.invoiceNumber.startsWith(currentShamsiYear))
        assertEquals("Clean invoice number should match invoice number", invoice.invoiceNumber, invoice.cleanInvoiceNumber)
    }

    @Test
    fun testClassicInvoiceDefaultNumberUsesShamsiYearWithoutHyphen() {
        val now = System.currentTimeMillis()
        val currentShamsiYear = ShamsiCalendarHelper.millisToShamsi(now).first.toString()
        val invoice = Invoice()

        assertFalse("Default classic invoice number must not contain hyphen", invoice.invoiceNumber.contains("-"))
        assertTrue("Default classic invoice number should start with current year", invoice.invoiceNumber.startsWith(currentShamsiYear))
    }
}
