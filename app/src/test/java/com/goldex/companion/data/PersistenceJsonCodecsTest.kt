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
        assertEquals(WageType.PERCENTAGE, decodedItem.wageType)
        assertEquals(12.0, decodedItem.wageValue, 0.001)
        assertEquals(750, decodedItem.customKaratValue)
    }

    @Test
    fun inventoryItemsJewelryAndCustomKaratRoundTrip() {
        val item = InventoryItem(
            id = "inv-jewelry",
            code = "JWL-200",
            title = "سرویس برلیان",
            category = InventoryCategory.JEWELRY,
            grossWeightGrams = 12.500,
            stoneWeightGrams = 2.500,
            karat = Karat.K18,
            customKaratValue = 740,
            wageType = WageType.TOMAN_PER_GRAM,
            wageValue = 500_000.0,
            profitPercent = 20.0,
            quantity = 1
        )
        val json = PersistenceJsonCodecs.encodeInventoryItems(listOf(item))
        val decoded = PersistenceJsonCodecs.decodeInventoryItems(json).single()

        assertEquals(InventoryCategory.JEWELRY, decoded.category)
        assertEquals(Karat.K18, decoded.karat)
        assertEquals(740, decoded.customKaratValue)
        assertEquals(WageType.TOMAN_PER_GRAM, decoded.wageType)
        assertEquals(500_000.0, decoded.wageValue, 0.001)
        assertEquals(20.0, decoded.profitPercent, 0.001)
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

    @Test
    fun marketRatesSerializationAndDecodingRoundTrip() {
        val rates = MarketRates(
            gold18 = 4285000L,
            gold24 = 5713000L,
            goldMelt = 18560000L,
            coinEmami = 51200000L,
            coinBahar = 46500000L,
            coinHalf = 26500000L,
            coinQuarter = 16500000L,
            coinGerami = 8000000L,
            usd = 68500L,
            ons = 2715.5,
            source = PriceSource.ISIGNAL,
            lastUpdated = "14:30:00",
            isLive = true
        )
        val json = PersistenceJsonCodecs.encodeMarketRates(rates)
        val decoded = PersistenceJsonCodecs.decodeMarketRates(json)

        org.junit.Assert.assertNotNull(decoded)
        assertEquals(rates.gold18, decoded!!.gold18)
        assertEquals(rates.gold24, decoded.gold24)
        assertEquals(rates.goldMelt, decoded.goldMelt)
        assertEquals(rates.coinEmami, decoded.coinEmami)
        assertEquals(rates.usd, decoded.usd)
        assertEquals(rates.ons, decoded.ons, 0.001)
        assertEquals(PriceSource.ISIGNAL, decoded.source)
        assertEquals("14:30:00", decoded.lastUpdated)
        assertEquals(true, decoded.isLive)
    }

    @Test
    fun marketRateItemSummariesRoundTrip() {
        val summaries = mapOf(
            com.goldex.companion.model.MarketRateItemType.GOLD_18K to MarketRateItemSummary(
                type = com.goldex.companion.model.MarketRateItemType.GOLD_18K,
                currentPrice = 4285000L,
                dayLow = 4250000L,
                dayHigh = 4300000L,
                openPrice = 4250000L,
                changeAmount = 35000L,
                changePercent = 0.82,
                isPositive = true,
                lastUpdated = 1700000000L
            ),
            com.goldex.companion.model.MarketRateItemType.USD to MarketRateItemSummary(
                type = com.goldex.companion.model.MarketRateItemType.USD,
                currentPrice = 68500L,
                dayLow = 68000L,
                dayHigh = 68900L,
                openPrice = 68900L,
                changeAmount = -400L,
                changePercent = -0.58,
                isPositive = false,
                lastUpdated = 1700000000L
            )
        )
        val json = PersistenceJsonCodecs.encodeMarketRateItemSummaries(summaries)
        val decoded = PersistenceJsonCodecs.decodeMarketRateItemSummaries(json)

        assertEquals(2, decoded.size)
        val gold18 = decoded[com.goldex.companion.model.MarketRateItemType.GOLD_18K]
        org.junit.Assert.assertNotNull(gold18)
        assertEquals(4285000L, gold18!!.currentPrice)
        assertEquals(4250000L, gold18.dayLow)
        assertEquals(4300000L, gold18.dayHigh)
        assertEquals(4250000L, gold18.openPrice)
        assertEquals(35000L, gold18.changeAmount)
        assertEquals(0.82, gold18.changePercent, 0.001)
        assertTrue(gold18.isPositive)
        assertEquals(1700000000L, gold18.lastUpdated)

        val usd = decoded[com.goldex.companion.model.MarketRateItemType.USD]
        org.junit.Assert.assertNotNull(usd)
        assertEquals(68500L, usd!!.currentPrice)
        org.junit.Assert.assertFalse(usd.isPositive)
    }
}
