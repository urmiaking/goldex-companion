package com.goldex.companion.domain.invoice

import com.goldex.companion.domain.customers.*
import com.goldex.companion.model.*
import org.junit.Assert.*
import org.junit.Test

class InvoiceDebtPolicyTest {
    private val customer = Customer(id = "customer", name = "test", role = "مشتری عادی")
    private fun invoice(basis: InvoiceDebtBasis = InvoiceDebtBasis.GOLD) = BarterInvoice(
        customer = customer, customerRole = CustomerRole.RETAIL, debtBasis = basis, spotPrice18k = 20_000_000,
        salesItems = listOf(MeltGoldItem(weight = 10.0, spotPrice = 20_000_000, totalPayable = 200_000_000.0, equivalent18kWeight = 10.0)),
        payments = listOf(SettlementPaymentItem(method = SettlementMethod.POS, amountTomans = 100_000_000))
    )

    @Test fun retailGoldDebtStaysFiveGramsAndSettlesFor110MillionAtNewRate() {
        val sale = invoice()
        val first = InvoiceLedgerSyncUseCase.generateLedgerSync(sale, customer)
        val debt = invoiceOutstanding(first.transactionsToCreate)
        assertEquals(OutstandingBalance(5.0, 0), debt)
        assertEquals(5.0, sale.remainingBalanceGold18k, 0.0)
        val request = SettlementRequest(customerId = customer.id, targetType = LedgerEntryType.GOLD_WEIGHT,
            paymentType = LedgerEntryType.CASH_RIAL, rateTomans = 22_000_000, amountTomans = 110_000_000)
        val paid = CustomerSettlementPolicy.preview(first.updatedCustomer, request, debt)
        assertEquals(0.0, paid.customerAfter.goldDebtGrams, 0.0)
        assertEquals(0L, paid.customerAfter.cashDebtTomans)
        val partial = CustomerSettlementPolicy.preview(first.updatedCustomer, request.copy(amountTomans = 100_000_000), debt)
        assertEquals(5.0 - 100.0 / 22.0, partial.customerAfter.goldDebtGrams, 1e-10)
    }

    @Test fun cashContractStays100MillionDespiteGoldRateChange() {
        val first = InvoiceLedgerSyncUseCase.generateLedgerSync(invoice(InvoiceDebtBasis.CASH), customer)
        assertEquals(OutstandingBalance(0.0, 100_000_000), invoiceOutstanding(first.transactionsToCreate))
        val paid = CustomerSettlementPolicy.preview(first.updatedCustomer, SettlementRequest(customerId = customer.id,
            targetType = LedgerEntryType.CASH_RIAL, paymentType = LedgerEntryType.CASH_RIAL, amountTomans = 100_000_000,
            rateTomans = 22_000_000))
        assertEquals(0L, paid.customerAfter.cashDebtTomans)
    }

    @Test fun contractUsesAgreedCoinAndScrapValuesAndIncludesInvoiceCharges() {
        val sale = invoice().copy(
            salesItems = listOf(MeltGoldItem(weight = 10.0, spotPrice = 20_000_000, totalPayable = 220_000_000.0, equivalent18kWeight = 10.0)),
            receivedItems = listOf(BankCoinItem(unitPrice = 40_000_000, totalPayable = 40_000_000.0, equivalent18kWeight = 1.8)),
            payments = listOf(SettlementPaymentItem(method = SettlementMethod.BULLION, goldWeight18k = 2.0),
                SettlementPaymentItem(method = SettlementMethod.POS, amountTomans = 40_000_000),
                SettlementPaymentItem(method = SettlementMethod.LEDGER, amountTomans = 100_000_000))
        )
        // 220 - 40 = 180 million = 9g, less 2g bullion and 2g cash. Deferral is not payment.
        assertEquals(5.0, sale.remainingBalanceGold18k, 0.0)
        val result = InvoiceLedgerSyncUseCase.generateLedgerSync(sale, customer)
        assertEquals(OutstandingBalance(5.0, 0), invoiceOutstanding(result.transactionsToCreate))
        assertEquals(customer.goldDebtGrams, InvoiceLedgerSyncUseCase.reverseLedgerSync(result.transactionsToCreate, result.updatedCustomer).goldDebtGrams, 0.0)
    }

    @Test fun purchaseCreditAndSmallDebtsKeepTheirDirectionAndPrecision() {
        val sale = invoice().copy(salesItems = emptyList(), receivedItems = invoice().salesItems, payments = emptyList())
        val result = InvoiceLedgerSyncUseCase.generateLedgerSync(sale, customer)
        assertEquals(-10.0, result.updatedCustomer.goldDebtGrams, 0.0)
        assertFalse(sale.isFullySettled)
        val small = invoice().copy(payments = listOf(SettlementPaymentItem(method = SettlementMethod.POS, amountTomans = 199_999_999)))
        assertFalse(small.isFullySettled)
        assertEquals(1L, small.remainingBalanceTomans)
    }

    @Test fun invalidRateAndOverflowCannotProduceAContract() {
        assertThrows(IllegalArgumentException::class.java) { InvoiceLedgerSyncUseCase.generateLedgerSync(invoice().copy(spotPrice18k = 0), customer) }
        assertThrows(ArithmeticException::class.java) {
            InvoiceDebtPolicy.principalTomans(invoice().copy(salesItems = listOf(MeltGoldItem(weight = 1.0,
                spotPrice = 1, totalPayable = 1e30, equivalent18kWeight = 1.0))))
        }
    }

    @Test fun initialOverpaymentKeepsPhysicalCreditAndInvoiceSettled() {
        val sale = invoice().copy(payments = listOf(
            SettlementPaymentItem(method = SettlementMethod.POS, amountTomans = 210_000_000, goldWeight18k = 10.5),
            SettlementPaymentItem(method = SettlementMethod.TRANSFER, amountTomans = 10_000_000)))
        val result = InvoiceLedgerSyncUseCase.generateLedgerSync(sale, customer)
        assertEquals(0.0, result.updatedCustomer.goldDebtGrams, 0.0)
        assertEquals(-20_000_000L, result.updatedCustomer.cashDebtTomans)
        assertTrue(invoiceOutstanding(result.transactionsToCreate).isSettled)
        assertTrue(sale.isFullySettled)
        assertEquals(LedgerEntryType.CASH_RIAL, result.transactionsToCreate[1].type)
        val cashSale = invoice(InvoiceDebtBasis.CASH).copy(payments = listOf(
            SettlementPaymentItem(method = SettlementMethod.BULLION, goldWeight18k = 11.0)))
        val cashResult = InvoiceLedgerSyncUseCase.generateLedgerSync(cashSale, customer)
        assertEquals(-1.0, cashResult.updatedCustomer.goldDebtGrams, 0.0)
        assertEquals(0L, cashResult.updatedCustomer.cashDebtTomans)
        assertTrue(cashSale.isFullySettled)
    }

    @Test fun declineInRateChangesCashSuggestionNotFiveGramDebt() {
        val first = InvoiceLedgerSyncUseCase.generateLedgerSync(invoice(), customer)
        val paid = CustomerSettlementPolicy.preview(first.updatedCustomer, SettlementRequest(
            customerId = customer.id, targetType = LedgerEntryType.GOLD_WEIGHT, paymentType = LedgerEntryType.CASH_RIAL,
            amountTomans = 90_000_000, rateTomans = 18_000_000))
        assertEquals(0.0, paid.customerAfter.goldDebtGrams, 0.0)
        assertEquals(0L, paid.customerAfter.cashDebtTomans)
    }

    @Test fun wholeTomanPrincipalRoundsHalfUpAndCashPaymentIgnoresCachedEquivalent() {
        val base = invoice().copy(payments = listOf(SettlementPaymentItem(
            method = SettlementMethod.POS, amountTomans = 100_000_000, goldWeight18k = 99.0)))
        assertEquals(5.0, base.remainingBalanceGold18k, 0.0)
        val rounded = base.copy(salesItems = listOf(MeltGoldItem(weight = 1.0, spotPrice = 1,
            totalPayable = 10.5, equivalent18kWeight = 1.0)))
        assertEquals(11L, InvoiceDebtPolicy.principalTomans(rounded))
    }
}
