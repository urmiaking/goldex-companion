package com.goldex.companion.data

import com.goldex.companion.model.CoinType
import com.goldex.companion.model.Customer
import com.goldex.companion.model.InventoryCategory
import com.goldex.companion.model.InventoryItem
import com.goldex.companion.model.Invoice
import com.goldex.companion.model.InvoiceItem
import com.goldex.companion.model.Karat
import com.goldex.companion.model.StockAdjustment
import com.goldex.companion.model.StockAdjustmentType
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
        assertEquals(750, item.customKaratValue)
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
                    customKaratValue = 999,
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
        assertEquals(999, decoded.items.single().customKaratValue)
        assertEquals(1.001, decoded.items.single().netWeight, 0.0001)
        assertEquals(CoinType.EMAMI, CoinType.valueOf("EMAMI"))
    }

    @Test
    fun inventoryItemsSerializationAndLegacyTolerantDecoding() {
        val item = InventoryItem(
            id = "inv-1",
            code = "RNG-104",
            title = "انگشتر البرنادو",
            category = InventoryCategory.RINGS,
            location = "سینی ۱",
            grossWeightGrams = 5.420,
            stoneWeightGrams = 0.150,
            karat = Karat.K18,
            workshop = "زرین",
            wagePercent = 12.0,
            profitPercent = 7.0,
            taxPercent = 9.0,
            rfidTag = "RF-104",
            quantity = 3
        )

        val json = PersistenceJsonCodecs.encodeInventoryItems(listOf(item))
        val decoded = PersistenceJsonCodecs.decodeInventoryItems(json)

        assertEquals(1, decoded.size)
        val decodedItem = decoded.single()
        assertEquals(item.id, decodedItem.id)
        assertEquals(item.code, decodedItem.code)
        assertEquals(item.title, decodedItem.title)
        assertEquals(item.category, decodedItem.category)
        assertEquals(5.420, decodedItem.grossWeightGrams, 0.001)
        assertEquals(0.150, decodedItem.stoneWeightGrams, 0.001)
        assertEquals(5.270, decodedItem.netGoldWeightGrams, 0.001)
        assertEquals(3, decodedItem.quantity)
    }

    @Test
    fun stockAdjustmentCodecPreservesData() {
        val adj = StockAdjustment(
            id = "adj-1",
            itemId = "inv-1",
            itemTitle = "انگشتر",
            type = StockAdjustmentType.CHARGE,
            quantityChange = 5,
            weightGrams = 27.180,
            reason = "دریافت از کارگاه ساخت",
            note = "حواله ۱۲۳"
        )

        val json = PersistenceJsonCodecs.encodeStockAdjustments(listOf(adj))
        val decoded = PersistenceJsonCodecs.decodeStockAdjustments(json)

        assertEquals(1, decoded.size)
        val decodedAdj = decoded.single()
        assertEquals(adj.id, decodedAdj.id)
        assertEquals(adj.itemId, decodedAdj.itemId)
        assertEquals(StockAdjustmentType.CHARGE, decodedAdj.type)
        assertEquals(5, decodedAdj.quantityChange)
        assertEquals(27.180, decodedAdj.weightGrams, 0.001)
        assertEquals("دریافت از کارگاه ساخت", decodedAdj.reason)
    }
}
