package com.goldex.companion.domain.customers

import com.goldex.companion.model.*
import com.goldex.companion.platform.SettlementArithmetic
import org.junit.Assert.*
import org.junit.Test

class CustomerSettlementTest {
    private val customer = Customer(id = "customer", name = "test", goldDebtGrams = 10.0)
    private fun cash(amount: Long, rate: Long = 100_000_000) = SettlementRequest(customerId = customer.id,
        targetType = LedgerEntryType.GOLD_WEIGHT, paymentType = LedgerEntryType.CASH_RIAL,
        amountTomans = amount, rateTomans = rate)

    @Test fun cashPaymentReducesGoldWithoutCreatingCashCredit() {
        val result = CustomerSettlementPolicy.preview(customer, cash(500_000_000))
        assertEquals(5.0, result.customerAfter.goldDebtGrams, 1e-12)
        assertEquals(0L, result.customerAfter.cashDebtTomans)
    }

    @Test fun increasedAndDecreasedRatesChangeOnlyConvertedPaymentWeight() {
        for (rate in listOf(105_000_000L, 95_000_000L)) {
            val result = CustomerSettlementPolicy.preview(customer, cash(500_000_000, rate))
            assertEquals(10.0 - 500_000_000.0 / rate, result.customerAfter.goldDebtGrams, 1e-12)
            assertEquals(0L, result.customerAfter.cashDebtTomans)
            assertEquals(500_000_000L, SettlementArithmetic.goldToTomans(result.goldAppliedGrams, rate))
        }
    }

    @Test fun goldPaymentToCashDebtUsesActualAssayAndSettlementRate() {
        val c = customer.copy(goldDebtGrams = 0.0, cashDebtTomans = 1_000_000_000)
        val r = SettlementRequest(customerId = c.id, targetType = LedgerEntryType.CASH_RIAL,
            paymentType = LedgerEntryType.GOLD_WEIGHT, scaleWeightGrams = 10.0, karat = 735, rateTomans = 95_000_000)
        val result = CustomerSettlementPolicy.preview(c, r)
        assertEquals(9.8, result.equivalent750Grams, 1e-12)
        assertEquals(69_000_000L, result.customerAfter.cashDebtTomans)
        assertEquals(0.0, result.customerAfter.goldDebtGrams, 0.0)
    }

    @Test fun onlyActualOverpaymentBecomesCreditInReceiptUnit() {
        val result = CustomerSettlementPolicy.preview(customer, cash(1_050_000_000))
        assertEquals(0.0, result.customerAfter.goldDebtGrams, 0.0)
        assertEquals(-50_000_000L, result.customerAfter.cashDebtTomans)
        val cashDebt = customer.copy(goldDebtGrams = 0.0, cashDebtTomans = 1_000_000_000)
        val gold = SettlementRequest(customerId = customer.id, targetType = LedgerEntryType.CASH_RIAL,
            paymentType = LedgerEntryType.GOLD_WEIGHT, scaleWeightGrams = 10.0, rateTomans = 105_000_000)
        val paid = CustomerSettlementPolicy.preview(cashDebt, gold)
        assertEquals(0L, paid.customerAfter.cashDebtTomans)
        assertEquals(-(10.0 - 1_000_000_000.0 / 105_000_000), paid.customerAfter.goldDebtGrams, 1e-12)
    }

    @Test fun explicitOffsetConsumesExistingCashCreditWithoutAnotherCashReceipt() {
        val c = customer.copy(cashDebtTomans = -500_000_000)
        val result = CustomerSettlementPolicy.preview(c, cash(500_000_000).copy(offsetExistingCredit = true))
        assertEquals(5.0, result.customerAfter.goldDebtGrams, 1e-12)
        assertEquals(0L, result.customerAfter.cashDebtTomans)
        assertThrows(IllegalArgumentException::class.java) {
            CustomerSettlementPolicy.preview(c, cash(600_000_000).copy(offsetExistingCredit = true))
        }
    }

    @Test fun payoutToCreditorAndOffsetInOppositeDirectionAreSymmetric() {
        val creditor = customer.copy(goldDebtGrams = -10.0)
        val paid = CustomerSettlementPolicy.preview(creditor, cash(500_000_000))
        assertEquals(-5.0, paid.customerAfter.goldDebtGrams, 1e-12)
        assertEquals(0L, paid.customerAfter.cashDebtTomans)
        val offset = CustomerSettlementPolicy.preview(creditor.copy(cashDebtTomans = 500_000_000), cash(500_000_000).copy(offsetExistingCredit = true))
        assertEquals(-5.0, offset.customerAfter.goldDebtGrams, 1e-12)
        assertEquals(0L, offset.customerAfter.cashDebtTomans)
    }

    @Test fun reversalUsesStoredEffectsEvenAfterMarketChanges() {
        val c = customer.copy(cashDebtTomans = -500_000_000)
        val result = CustomerSettlementPolicy.preview(c, cash(500_000_000).copy(offsetExistingCredit = true))
        val tx = LedgerTransaction(customerId = c.id, type = LedgerEntryType.CASH_RIAL, amountTomans = 500_000_000,
            settlement = LedgerSettlement(LedgerEntryType.GOLD_WEIGHT, 100_000_000, "custom", "", true, result.effect.goldGrams, result.effect.cashTomans))
        val reversed = result.customerAfter.applyEffect(tx.balanceEffect(), reverse = true)
        assertEquals(c.goldDebtGrams, reversed.goldDebtGrams, 1e-12)
        assertEquals(c.cashDebtTomans, reversed.cashDebtTomans)
        // The offset's cash effect is on the customer, not a second invoice debt.
        val invoiceEntries = listOf(LedgerTransaction(customerId = c.id, type = LedgerEntryType.GOLD_WEIGHT,
            direction = LedgerDirection.PAY, equivalent750WeightGrams = 10.0), tx)
        assertEquals(OutstandingBalance(5.0, 0), invoiceOutstanding(invoiceEntries))
    }

    @Test fun sameUnitReceiptDoesNotNeedMarketRateAndLegacyEffectIsUnchanged() {
        val c = customer.copy(goldDebtGrams = 0.0, cashDebtTomans = 100)
        val result = CustomerSettlementPolicy.preview(c, cash(50).copy(targetType = LedgerEntryType.CASH_RIAL, rateTomans = 0))
        assertEquals(50L, result.customerAfter.cashDebtTomans)
        assertEquals(LedgerEffect(cashTomans = -50), LedgerTransaction(customerId = c.id, type = LedgerEntryType.CASH_RIAL, amountTomans = 50).balanceEffect())
    }

    @Test fun invalidRateAssayAndOverflowFailBeforeAnyFinancialWrite() {
        assertThrows(IllegalArgumentException::class.java) { CustomerSettlementPolicy.preview(customer, cash(100, 0)) }
        assertThrows(IllegalArgumentException::class.java) { CustomerSettlementPolicy.preview(customer, cash(0)) }
        assertThrows(IllegalArgumentException::class.java) { CustomerSettlementPolicy.preview(customer, cash(100).copy(paymentType = LedgerEntryType.GOLD_WEIGHT, scaleWeightGrams = Double.NaN)) }
        assertThrows(ArithmeticException::class.java) { SettlementArithmetic.goldToTomans(100.0, Long.MAX_VALUE) }
        assertEquals(1L, SettlementArithmetic.goldToTomans(0.001, 500))
        assertEquals(0L, SettlementArithmetic.goldToTomans(0.001, 499))
    }

    @Test
    fun generateSettlementDocumentNumberProducesCleanVoucherNumber() {
        val number = generateSettlementDocumentNumber(emptyList())
        val intVal = number.toIntOrNull()
        assertNotNull(intVal)
        assertTrue(intVal!! in 1000..9999)
    }

    @Test
    fun generateSettlementDocumentNumberContinuesSequenceWhenPresent() {
        val next = generateSettlementDocumentNumber(listOf("1001", "1002", "1003"))
        assertEquals("1004", next)
    }

    @Test
    fun generateSettlementDocumentNumberIgnoresLegacyLongTimestampAndAvoidsDuplicates() {
        // Legacy 13-digit timestamp should be ignored rather than overflowing
        val next = generateSettlementDocumentNumber(listOf("1775282345123", "2050"))
        assertEquals("2051", next)
    }
}
