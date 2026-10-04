package com.goldex.companion.ui.util

import com.goldex.companion.data.AppSettings
import com.goldex.companion.domain.invoice.OfficialInvoiceDocumentFactory
import com.goldex.companion.model.BarterInvoice
import com.goldex.companion.model.CraftedGoldItem
import com.goldex.companion.model.Customer
import com.goldex.companion.model.Karat
import com.goldex.companion.model.PersianNumberFormatter
import com.goldex.companion.model.WageType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OfficialInvoicePdfGeneratorTest {

    @Test
    fun testOfficialInvoiceDocumentModelCreation() {
        val invoice = BarterInvoice(
            invoiceNumber = "1405540",
            customer = Customer(
                name = "مسعود خدادادی",
                phone = "09905492104",
                nationalId = "ثبت نشده"
            ),
            spotPrice18k = 26_225_300L,
            salesItems = listOf(
                CraftedGoldItem(
                    title = "انگشتر تیفانی",
                    karat = Karat.K18,
                    grossWeight = 12.460,
                    stoneWeight = 1.690,
                    netWeight = 10.770,
                    spotPrice = 26_225_300L,
                    wageType = WageType.PERCENTAGE,
                    wageInput = 7.0,
                    wageAmount = 19_773_876.0,
                    profitPercent = 23.0,
                    profitAmount = 69_507_456.0,
                    taxPercent = 10.0,
                    taxAmount = 8_928_133.0,
                    rawGoldValue = 282_446_481.0,
                    totalPayable = 380_655_946.0,
                    equivalent18kWeight = 10.770
                )
            )
        )
        val settings = AppSettings(
            galleryName = "طلا و جواهری فانی",
            managerName = "محمدحسن فانی",
            galleryPhone = "09905492104",
            galleryAddress = "خوی، مجتمع آناهیتا پلاک جی ۲۲",
            unionCode = "۱۷۶۲۱۷"
        )

        val doc = OfficialInvoiceDocumentFactory.create(invoice, settings)

        assertEquals("Qirat_Invoice_1405540.pdf", doc.fileName)
        assertEquals("طلا و جواهری فانی", doc.sellerName)
        assertEquals("محمدحسن فانی", doc.sellerManager)
        assertEquals("مسعود خدادادی", doc.buyerName)
        assertEquals("QIRAT-1405540", doc.trackingCode)
        assertEquals(1, doc.rows.size)
        assertEquals(380_655_946L, doc.payableAmount)
        assertEquals(282_446_481L, doc.totalRawGoldValue)
        assertEquals("۳۸۰,۶۵۵,۹۴۶", PersianNumberFormatter.formatPrice(doc.payableAmount.toDouble()))
    }

    @Test
    fun testBidiFormattingTokens() {
        val trackingCode = "QIRAT-1405540"
        val bidiSafeTracking = "\u200E$trackingCode"
        assertTrue(bidiSafeTracking.startsWith("\u200E"))

        val phone = "۰۹۹۰۵۴۹۲۱۰۴"
        val labelWithBidiColon = "تلفن:\u200F $phone"
        assertTrue(labelWithBidiColon.contains(":\u200F"))
    }
}
