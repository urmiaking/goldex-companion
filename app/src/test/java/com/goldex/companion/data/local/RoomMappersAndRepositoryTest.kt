package com.goldex.companion.data.local

import com.goldex.companion.data.local.db.converters.DatabaseConverters
import com.goldex.companion.data.local.db.mappers.toDomain
import com.goldex.companion.data.local.db.mappers.toEntity
import com.goldex.companion.model.BarterInvoice
import com.goldex.companion.model.CraftedGoldItem
import com.goldex.companion.model.Customer
import com.goldex.companion.model.CustomerRole
import com.goldex.companion.model.InventoryCategory
import com.goldex.companion.model.InventoryItem
import com.goldex.companion.model.Karat
import com.goldex.companion.model.LedgerDirection
import com.goldex.companion.model.LedgerEntryType
import com.goldex.companion.model.LedgerTransaction
import com.goldex.companion.model.SettlementMethod
import com.goldex.companion.model.SettlementPaymentItem
import com.goldex.companion.model.StockAdjustment
import com.goldex.companion.model.StockAdjustmentType
import com.goldex.companion.model.WageType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RoomMappersAndRepositoryTest {

    private val converters = DatabaseConverters()

    @Test
    fun customerMappingRoundTripPreservesAllFields() {
        val original = Customer(
            id = "cust-101",
            name = "حاج رضا زرگر",
            phone = "09121234567",
            nationalId = "0012345678",
            note = "معتمد بازار",
            role = "بنکدار تهران",
            goldDebtGrams = 12.345,
            cashDebtTomans = 150_000_000L,
            accountCode = "ACC-99",
            isVerified = true,
            cityOrMarket = "بازار زرگرها"
        )

        val entity = original.toEntity()
        val mappedBack = entity.toDomain()

        assertEquals(original.id, mappedBack.id)
        assertEquals(original.name, mappedBack.name)
        assertEquals(original.phone, mappedBack.phone)
        assertEquals(original.nationalId, mappedBack.nationalId)
        assertEquals(original.goldDebtGrams, mappedBack.goldDebtGrams, 0.0001)
        assertEquals(original.cashDebtTomans, mappedBack.cashDebtTomans)
        assertEquals(original.accountCode, mappedBack.accountCode)
        assertEquals(original.isVerified, mappedBack.isVerified)
        assertEquals(original.cityOrMarket, mappedBack.cityOrMarket)
    }

    @Test
    fun ledgerTransactionMappingRoundTripPreservesAllFields() {
        val original = LedgerTransaction(
            id = "tx-501",
            customerId = "cust-101",
            documentNumber = "IR-9001",
            title = "تسویه طلای آبشده",
            type = LedgerEntryType.GOLD_WEIGHT,
            direction = LedgerDirection.RECEIVE,
            goldCategory = "آبشده",
            scaleWeightGrams = 25.123,
            karat = 750,
            equivalent750WeightGrams = 25.123,
            amountTomans = 0L,
            resultingGoldBalance = 15.0,
            resultingCashBalance = 0L,
            invoiceId = "inv-808",
            invoiceNumber = "1403-112"
        )

        val entity = original.toEntity()
        val mappedBack = entity.toDomain()

        assertEquals(original.id, mappedBack.id)
        assertEquals(original.customerId, mappedBack.customerId)
        assertEquals(original.documentNumber, mappedBack.documentNumber)
        assertEquals(original.type, mappedBack.type)
        assertEquals(original.direction, mappedBack.direction)
        assertEquals(original.scaleWeightGrams, mappedBack.scaleWeightGrams, 0.0001)
        assertEquals(original.invoiceId, mappedBack.invoiceId)
        assertEquals(original.invoiceNumber, mappedBack.invoiceNumber)
    }

    @Test
    fun barterInvoiceMappingRoundTripPreservesNestedItemsAndPayments() {
        val craftedItem = CraftedGoldItem(
            id = "item-1",
            title = "دستبند کارتیه",
            grossWeight = 10.5,
            stoneWeight = 0.5,
            netWeight = 10.0,
            spotPrice = 24_000_000L,
            wageType = WageType.PERCENTAGE,
            wageInput = 10.0,
            wageAmount = 24_000_000.0,
            profitPercent = 7.0,
            profitAmount = 18_480_000.0,
            taxPercent = 9.0,
            taxAmount = 3_823_200.0,
            rawGoldValue = 240_000_000.0,
            totalPayable = 286_303_200.0,
            equivalent18kWeight = 10.0
        )

        val payment = SettlementPaymentItem(
            id = "pay-1",
            method = SettlementMethod.POS,
            amountTomans = 100_000_000L,
            trackingCode = "TRK-12345"
        )

        val original = BarterInvoice(
            id = "inv-barter-1",
            invoiceNumber = "1403-555",
            customerRole = CustomerRole.WHOLESALER,
            spotPrice18k = 24_000_000L,
            salesItems = listOf(craftedItem),
            settlementMethod = SettlementMethod.POS,
            cashPosAmount = 100_000_000L,
            payments = listOf(payment)
        )

        val entity = original.toEntity()
        val mappedBack = entity.toDomain()

        assertEquals(original.id, mappedBack.id)
        assertEquals(original.invoiceNumber, mappedBack.invoiceNumber)
        assertEquals(original.customerRole, mappedBack.customerRole)
        assertEquals(1, mappedBack.salesItems.size)
        assertEquals("دستبند کارتیه", mappedBack.salesItems.first().title)
        assertEquals(1, mappedBack.payments.size)
        assertEquals("TRK-12345", mappedBack.payments.first().trackingCode)
    }

    @Test
    fun inventoryMappingRoundTripPreservesPrecisionAndFormulas() {
        val item = InventoryItem(
            id = "inv-item-1",
            code = "RNG-01",
            title = "انگشتر سولیتر",
            category = InventoryCategory.RINGS,
            grossWeightGrams = 4.250,
            stoneWeightGrams = 0.250,
            karat = Karat.K18,
            wageType = WageType.PERCENTAGE,
            wagePercent = 12.0,
            wageValue = 12.0,
            profitPercent = 7.0,
            taxPercent = 9.0,
            quantity = 2
        )

        val entity = item.toEntity()
        val mappedBack = entity.toDomain()

        assertEquals(item.id, mappedBack.id)
        assertEquals(item.code, mappedBack.code)
        assertEquals(item.netGoldWeightGrams, mappedBack.netGoldWeightGrams, 0.0001)
        assertEquals(item.calculateEstimatedValue(24_000_000L), mappedBack.calculateEstimatedValue(24_000_000L))

        val adj = StockAdjustment(
            id = "adj-1",
            itemId = item.id,
            itemTitle = item.title,
            type = StockAdjustmentType.CHARGE,
            quantityChange = 2,
            weightGrams = 8.5
        )

        val adjEntity = adj.toEntity()
        val adjMappedBack = adjEntity.toDomain()

        assertEquals(adj.id, adjMappedBack.id)
        assertEquals(adj.type, adjMappedBack.type)
        assertEquals(adj.weightGrams, adjMappedBack.weightGrams, 0.0001)
    }

    @Test
    fun databaseConvertersSerializeAndDeserializeSafely() {
        val customer = Customer(id = "c-1", name = "علی رضایی")
        val json = converters.fromCustomer(customer)
        assertNotNull(json)
        val decoded = converters.toCustomer(json)
        assertNotNull(decoded)
        assertEquals("c-1", decoded?.id)
        assertEquals("علی رضایی", decoded?.name)

        val emptyJson = converters.fromBarterItemList(emptyList())
        assertEquals("[]", emptyJson)
        val emptyList = converters.toBarterItemList(emptyJson)
        assertTrue(emptyList.isEmpty())
    }
}
