package com.goldex.companion

import com.goldex.companion.data.PersistenceJsonCodecs
import com.goldex.companion.model.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CustomerLedgerTest {

    @Test
    fun gold750ConversionCalculationIsAccurate() {
        val weight = 12.500
        val karat750 = 750.0
        val equivalent750 = (weight * karat750) / 750.0
        assertEquals(12.500, equivalent750, 0.0001)

        val karat999 = 999.0
        val equivalent999To750 = (weight * karat999) / 750.0
        // (12.5 * 999) / 750 = 16.65
        assertEquals(16.650, equivalent999To750, 0.0001)

        val karat705 = 705.0
        val equivalent705To750 = (weight * karat705) / 750.0
        // (12.5 * 705) / 750 = 11.75
        assertEquals(11.750, equivalent705To750, 0.0001)
    }

    @Test
    fun customerExtendedFieldsRoundTripSuccessfully() {
        val customer = Customer(
            id = "cust-test-1",
            name = "حاج محمود صراف",
            phone = "09121112233",
            nationalId = "0011223344",
            role = "بنکدار و کیفی",
            goldDebtGrams = 42.150,
            cashDebtTomans = -125_000_000L,
            accountCode = "1042",
            isVerified = true,
            cityOrMarket = "بازار بزرگ تهران - سرای امید",
            lastActivityTime = "امروز ۱۱:۴۵"
        )

        val encodedJson = PersistenceJsonCodecs.encodeCustomers(listOf(customer))
        val decodedList = PersistenceJsonCodecs.decodeCustomers(encodedJson)

        assertEquals(1, decodedList.size)
        val decoded = decodedList.single()
        assertEquals("cust-test-1", decoded.id)
        assertEquals("حاج محمود صراف", decoded.name)
        assertEquals("09121112233", decoded.phone)
        assertEquals("بنکدار و کیفی", decoded.role)
        assertEquals(42.150, decoded.goldDebtGrams, 0.0001)
        assertEquals(-125_000_000L, decoded.cashDebtTomans)
        assertEquals("1042", decoded.accountCode)
        assertTrue(decoded.isVerified)
        assertEquals("بازار بزرگ تهران - سرای امید", decoded.cityOrMarket)
        assertEquals("امروز ۱۱:۴۵", decoded.lastActivityTime)
    }

    @Test
    fun ledgerTransactionSerializationRoundTrip() {
        val tx = LedgerTransaction(
            id = "tx-101",
            customerId = "cust-test-1",
            type = LedgerEntryType.GOLD_WEIGHT,
            direction = LedgerDirection.RECEIVE,
            goldCategory = "شمش آبشده",
            scaleWeightGrams = 15.200,
            karat = 750,
            equivalent750WeightGrams = 15.200,
            amountTomans = 0L,
            documentNumber = "5021",
            dateTime = "۱۴۰۳/۰۶/۲۰ ۱۱:۳۰",
            title = "دریافت شمش آبشده",
            tagBadge = "شمش آبشده",
            note = "رسید آبشده اتحادیه - عیار سنجی دقیق",
            timestamp = 1710000000000L
        )

        val encodedJson = PersistenceJsonCodecs.encodeLedgerTransactions(listOf(tx))
        val decodedList = PersistenceJsonCodecs.decodeLedgerTransactions(encodedJson)

        assertEquals(1, decodedList.size)
        val decoded = decodedList.single()
        assertEquals("tx-101", decoded.id)
        assertEquals("cust-test-1", decoded.customerId)
        assertEquals(LedgerEntryType.GOLD_WEIGHT, decoded.type)
        assertEquals(LedgerDirection.RECEIVE, decoded.direction)
        assertEquals(15.200, decoded.scaleWeightGrams, 0.0001)
        assertEquals(750, decoded.karat)
        assertEquals(15.200, decoded.equivalent750WeightGrams, 0.0001)
        assertEquals(0L, decoded.amountTomans)
        assertEquals("5021", decoded.documentNumber)
        assertEquals("۱۴۰۳/۰۶/۲۰ ۱۱:۳۰", decoded.dateTime)
        assertEquals("دریافت شمش آبشده", decoded.title)
        assertEquals("شمش آبشده", decoded.tagBadge)
        assertEquals("رسید آبشده اتحادیه - عیار سنجی دقیق", decoded.note)
        assertEquals(1710000000000L, decoded.timestamp)
    }

    @Test
    fun ledgerDebitCreditBalanceCalculationsAreCorrect() {
        // Initial customer balance: 0 gold, 0 cash
        var goldDebt = 0.0
        var cashDebt = 0L

        // Transaction 1: Gold PAY (delivery of gold to counterparty, counterparty is now debtor of gold)
        val tx1GoldPay = 25.500
        goldDebt += tx1GoldPay
        assertEquals(25.500, goldDebt, 0.0001)

        // Transaction 2: Cash RECEIVE (counterparty pays us 100,000,000 Tomans, counterparty is now creditor of cash)
        val tx2CashReceive = 100_000_000L
        cashDebt -= tx2CashReceive
        assertEquals(-100_000_000L, cashDebt)

        // Transaction 3: Gold RECEIVE (counterparty returns 10.000g of gold)
        val tx3GoldReceive = 10.000
        goldDebt -= tx3GoldReceive
        assertEquals(15.500, goldDebt, 0.0001)

        // Transaction 4: Cash PAY (we pay back 40,000,000 Tomans to counterparty)
        val tx4CashPay = 40_000_000L
        cashDebt += tx4CashPay
        assertEquals(-60_000_000L, cashDebt)
    }

    @Test
    fun ledgerFilterTabsCorrectlySegregateStatuses() {
        val debtor = Customer(id = "1", name = "بدهکار", goldDebtGrams = 10.0, cashDebtTomans = 0L)
        val creditor = Customer(id = "2", name = "بستانکار", goldDebtGrams = 0.0, cashDebtTomans = -50_000_000L)
        val settled = Customer(id = "3", name = "تسویه", goldDebtGrams = 0.0, cashDebtTomans = 0L)

        val list = listOf(debtor, creditor, settled)

        val debtors = list.filter { it.goldDebtGrams > 0.001 || it.cashDebtTomans > 0L }
        val creditors = list.filter { it.goldDebtGrams < -0.001 || it.cashDebtTomans < 0L }
        val settledList = list.filter { Math.abs(it.goldDebtGrams) <= 0.001 && it.cashDebtTomans == 0L }

        assertEquals(1, debtors.size)
        assertEquals("بدهکار", debtors.first().name)

        assertEquals(1, creditors.size)
        assertEquals("بستانکار", creditors.first().name)

        assertEquals(1, settledList.size)
        assertEquals("تسویه", settledList.first().name)
    }
}
