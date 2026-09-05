package com.goldex.companion.data

import com.goldex.companion.model.CoinType
import com.goldex.companion.model.Customer
import com.goldex.companion.model.Invoice
import com.goldex.companion.model.InvoiceItem
import com.goldex.companion.model.Karat
import com.goldex.companion.model.WageType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PersistenceJsonCodecsTest {
    @Test
    fun customerLegacyFieldsAndUnknownFieldsRemainCompatible() {
        val json = """
            [{"id":"customer-1","name":"مشتری قدیمی","futureField":"ignored"}]
        """.trimIndent()

        val customers = PersistenceJsonCodecs.decodeCustomers(json)

        assertEquals(1, customers.size)
        assertEquals("customer-1", customers.single().id)
        assertEquals("مشتری قدیمی", customers.single().name)
        assertEquals("", customers.single().phone)
        assertEquals("", customers.single().nationalId)
        assertTrue(customers.single().createdAt > 0L)
    }

    @Test
    fun malformedCustomerRecordsAreIgnoredWithoutCrashing() {
        val customers = PersistenceJsonCodecs.decodeCustomers("[{\"id\":\"\",\"name\":\"\"},{bad json]")
        assertTrue(customers.isEmpty())
    }

    @Test
    fun portfolioUnknownEnumsUseSafeDefaultsAndPrecisionSurvives() {
        val json = """
            [{"id":"asset-1","title":"شمش","category":"UNKNOWN","karat":"INVALID","weightGrams":1.237,"quantity":1,"purchasePriceTotal":1000,"future":true}]
        """.trimIndent()

        val items = PersistenceJsonCodecs.decodePortfolioItems(json)

        assertEquals(1, items.size)
        assertEquals("asset-1", items.single().id)
        assertEquals(PortfolioCategory.GOLD, items.single().category)
        assertEquals(Karat.K18, items.single().karat)
        assertEquals(1.237, items.single().weightGrams, 0.0001)
        assertEquals(1000L, items.single().purchasePriceTotal)
    }

    @Test
    fun invoiceNestedDataAndUnknownEnumsRemainReadable() {
        val json = """
            [{
              "id":"invoice-1",
              "invoiceNumber":"1001",
              "customer":{"id":"customer-1","name":"خریدار","phone":"۰۹۱۲"},
              "items":[{"id":"item-1","title":"انگشتر","karat":"UNKNOWN","wageType":"UNKNOWN","grossWeight":2.125,"netWeight":2.125,"totalPayable":2500000}]
            }]
        """.trimIndent()

        val invoices = PersistenceJsonCodecs.decodeInvoices(json)
        val invoice = invoices.single()
        val item = invoice.items.single()

        assertEquals("invoice-1", invoice.id)
        assertEquals("خریدار", invoice.customer?.name)
        assertEquals("item-1", item.id)
        assertEquals(Karat.K18, item.karat)
        assertEquals(WageType.PERCENTAGE, item.wageType)
        assertEquals(2.125, item.grossWeight, 0.0001)
        assertEquals(2_500_000.0, item.totalPayable, 0.001)
    }

    @Test
    fun supportedRecordsRoundTripStableIdentifiersAndValues() {
        val invoice = Invoice(
            id = "invoice-2",
            invoiceNumber = "1002",
            customer = Customer(id = "customer-2", name = "علی"),
            items = listOf(
                InvoiceItem(
                    id = "item-2",
                    title = "سکه",
                    karat = Karat.K24,
                    grossWeight = 1.001,
                    netWeight = 1.001,
                    spotPrice = 10_000_000L,
                    wageType = WageType.TOMAN_PER_GRAM,
                    wageInput = 100_000.0,
                    wageAmount = 100_000.0,
                    profitPercent = 7.0,
                    profitAmount = 707_000.0,
                    taxPercent = 9.0,
                    taxAmount = 72_630.0,
                    rawGoldValue = 10_000_000.0,
                    totalPayable = 10_879_630.0,
                    effectiveGramPrice = 10_869_760.239
                )
            )
        )

        val decoded = PersistenceJsonCodecs.decodeInvoices(
            PersistenceJsonCodecs.encodeInvoices(listOf(invoice))
        ).single()

        assertEquals(invoice.id, decoded.id)
        assertEquals(invoice.invoiceNumber, decoded.invoiceNumber)
        assertEquals(invoice.customer?.id, decoded.customer?.id)
        assertEquals(invoice.items.single().id, decoded.items.single().id)
        assertEquals(1.001, decoded.items.single().netWeight, 0.0001)
        assertEquals(CoinType.EMAMI, CoinType.valueOf("EMAMI"))
    }
}
