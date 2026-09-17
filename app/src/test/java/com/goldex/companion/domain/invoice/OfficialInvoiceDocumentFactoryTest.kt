package com.goldex.companion.domain.invoice

import com.goldex.companion.data.AppSettings
import com.goldex.companion.model.BarterInvoice
import com.goldex.companion.model.CraftedGoldItem
import com.goldex.companion.model.Customer
import com.goldex.companion.model.Karat
import com.goldex.companion.model.ScrapGoldItem
import com.goldex.companion.model.WageType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OfficialInvoiceDocumentFactoryTest {
    @Test
    fun documentUsesInvoiceValuesAndSeparatesReceivedRows() {
        val sale = CraftedGoldItem(
            title = "النگو",
            karat = Karat.K18,
            grossWeight = 10.5,
            stoneWeight = 0.5,
            netWeight = 10.0,
            spotPrice = 4_000_000L,
            wageType = WageType.PERCENTAGE,
            wageInput = 10.0,
            wageAmount = 4_000_000.0,
            profitPercent = 7.0,
            profitAmount = 3_080_000.0,
            taxPercent = 9.0,
            taxAmount = 637_200.0,
            rawGoldValue = 40_000_000.0,
            totalPayable = 47_717_200.0,
            equivalent18kWeight = 10.0
        )
        val received = ScrapGoldItem(
            title = "طلای کهنه",
            grossWeight = 2.1,
            stoneWeight = 0.1,
            netWeight = 2.0,
            spotPrice = 4_000_000L,
            effectiveGramPrice = 3_900_000L,
            totalPayable = 7_800_000.0,
            equivalent18kWeight = 1.96
        )
        val invoice = BarterInvoice(
            invoiceNumber = "IR-1403-089",
            customer = Customer(name = "محمد کاظمی", phone = "09120000000", nationalId = "0012345678"),
            spotPrice18k = 4_000_000L,
            salesItems = listOf(sale),
            receivedItems = listOf(received),
            posTrackingCode = "POS-123"
        )

        val document = OfficialInvoiceDocumentFactory.create(
            invoice,
            AppSettings(galleryName = "گالری قیراط", managerName = "مدیر گالری")
        )

        assertEquals("Qirat_Invoice_IR-1403-089.pdf", document.fileName)
        assertEquals("گالری قیراط", document.sellerName)
        assertEquals("محمد کاظمی", document.buyerName)
        assertEquals("POS-123", document.trackingCode)
        assertEquals(2, document.rows.size)
        assertFalse(document.rows.first().isReceived)
        assertTrue(document.rows.last().isReceived)
        assertEquals(40_000_000L, document.totalRawGoldValue)
        assertEquals(7_080_000L, document.totalWageAndProfit)
        assertEquals(637_200L, document.totalTax)
        assertEquals(39_917_200L, document.payableAmount)
    }
}
