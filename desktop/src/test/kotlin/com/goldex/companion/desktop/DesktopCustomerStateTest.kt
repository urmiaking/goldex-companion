package com.goldex.companion.desktop

import com.goldex.companion.data.CustomerStore
import com.goldex.companion.desktop.customers.*
import com.goldex.companion.desktop.data.*
import com.goldex.companion.domain.customers.*
import com.goldex.companion.model.*
import kotlinx.coroutines.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import kotlin.test.*

class DesktopCustomerStateTest {
    @get:Rule val temporary = TemporaryFolder()
    private fun scope() = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
    private val contact = Customer(id = "customer", name = "آزمون", createdAt = 1)
    private fun receipt(amount: Long, id: String = "receipt") = LedgerTransaction(id = id, customerId = contact.id,
        type = LedgerEntryType.CASH_RIAL, direction = LedgerDirection.PAY, amountTomans = amount, timestamp = 5)

    @Test fun saveEditAndDeleteRecomputeBalancesAndReverseOnlyOnce() {
        DesktopDataStore(temporary.newFolder().toPath()).use { store ->
            val customers = DesktopCustomerStore(store)
            val invoices = DesktopCustomerInvoiceStore(store, customers)
            val scope = scope()
            try {
                customers.addCustomer(contact)
                val manager = CustomerManagerViewModel(customers, customers, invoices, scope, Dispatchers.Unconfined)
                manager.openCustomerStatement(contact)
                manager.openAddLedgerEntry(contact)
                manager.saveLedgerEntry(receipt(1_000))
                assertEquals(1_000L, manager.uiState.value.selectedCustomerForStatement!!.cashDebtTomans)
                assertFalse(manager.uiState.value.isAddLedgerEntryModalVisible)
                val original = customers.getTransactions(contact.id).single()
                manager.openEditLedgerEntry(original)
                manager.saveLedgerEntry(original.copy(amountTomans = 2_000, timestamp = 99))
                assertEquals(2_000L, customers.getCustomers().single().cashDebtTomans)
                assertEquals(5L, customers.getTransactions(contact.id).single().timestamp)
                assertEquals(2_000L, manager.uiState.value.statementBalances[original.id]!!.second)
                manager.deleteLedgerEntry(original)
                manager.deleteLedgerEntry(original)
                assertEquals(0L, customers.getCustomers().single().cashDebtTomans)
                assertTrue(customers.getTransactions(contact.id).isEmpty())
            } finally { scope.cancel() }
        }
    }

    @Test fun balanceWriteFailureKeepsDraftAndDoesNotLeaveReceiptOnDisk() {
        DesktopDataStore(temporary.newFolder().toPath()).use { store ->
            val customers = DesktopCustomerStore(store)
            customers.addCustomer(contact)
            val failing = object : CustomerStore by customers {
                override fun updateCustomer(customer: Customer) { error("synthetic failure") }
            }
            val scope = scope()
            try {
                val manager = CustomerManagerViewModel(failing, customers, DesktopCustomerInvoiceStore(store, customers), scope, Dispatchers.Unconfined)
                manager.openCustomerStatement(contact)
                manager.openAddLedgerEntry(contact)
                var completed = false
                manager.saveLedgerEntry(receipt(1_000)) { completed = true }
                assertFalse(completed)
                assertTrue(manager.uiState.value.isAddLedgerEntryModalVisible)
                assertNotNull(manager.uiState.value.error)
                assertFalse(manager.uiState.value.isSaving)
                assertEquals(0L, customers.getCustomers().single().cashDebtTomans)
                assertTrue(customers.getTransactions(contact.id).isEmpty())
            } finally { scope.cancel() }
        }
    }

    @Test fun editingContactNeverOverwritesMoreRecentFinancialBalance() {
        DesktopDataStore(temporary.newFolder().toPath()).use { store ->
            val customers = DesktopCustomerStore(store)
            customers.addCustomer(contact)
            val scope = scope()
            try {
                val manager = CustomerManagerViewModel(customers, customers, DesktopCustomerInvoiceStore(store, customers), scope, Dispatchers.Unconfined)
                manager.openCustomerStatement(contact)
                manager.saveLedgerEntry(receipt(3_000))
                manager.updateCustomer(contact.copy(name = "نام جدید"))
                assertEquals(3_000L, customers.getCustomers().single().cashDebtTomans)
                assertEquals("نام جدید", manager.uiState.value.selectedCustomerForStatement!!.name)
            } finally { scope.cancel() }
        }
    }

    @Test fun settlementCannotPresentOfflineOrUnknownTimeAsLiveRate() {
        DesktopDataStore(temporary.newFolder().toPath()).use { store ->
            val customers = DesktopCustomerStore(store)
            val debt = contact.copy(goldDebtGrams = 1.0)
            customers.addCustomer(debt)
            val invoices = DesktopCustomerInvoiceStore(store, customers)
            val scope = scope()
            try {
                val settlement = CustomerSettlementViewModel(customers, invoices, RecordCustomerSettlementUseCase(customers, invoices, customers),
                    scope, Dispatchers.Unconfined, marketQuote = { DesktopMarketRepository.emptyRates().copy(gold18 = 6_000_000, isLive = false) })
                settlement.open(debt)
                assertFalse(settlement.state.value.marketRateAvailable)
                assertFalse(settlement.state.value.isMarketRate)
                assertEquals("", settlement.state.value.rateInput)
                settlement.setRate("۶۰۰۰۰۰۰")
                settlement.setAmount("۶٬۰۰۰٬۰۰۰")
                assertEquals(0.0, settlement.state.value.preview!!.customerAfter.goldDebtGrams)
                settlement.confirm()
                assertNull(settlement.state.value.customer)
                assertEquals(0.0, customers.getCustomers().single().goldDebtGrams)
                assertEquals(6_000_000L, customers.getTransactions(contact.id).single().settlement!!.rateTomans)
            } finally { scope.cancel() }
        }
    }

    @Test fun statementFilterKeepsHistoricalBalanceAndBothSidesOfMixedAccount() {
        val gold = LedgerTransaction(id = "gold", customerId = contact.id, direction = LedgerDirection.PAY,
            scaleWeightGrams = 2.0, equivalent750WeightGrams = 2.0, timestamp = 1)
        val cash = receipt(1_000).copy(direction = LedgerDirection.RECEIVE, timestamp = 2)
        val latest = contact.copy(goldDebtGrams = 2.0, cashDebtTomans = -1_000)
        val balances = customerStatementBalances(latest, listOf(cash, gold))
        val state = CustomerManagerUiState(customerList = listOf(latest), activeCustomerTransactions = listOf(cash, gold),
            selectedStatementFilter = StatementFilterTab.GOLD_SALE, statementBalances = balances)
        assertEquals(listOf(gold), state.filteredStatementTransactions)
        assertEquals(2.0 to 0L, state.statementBalances[gold.id])
        assertEquals(1, state.debtorsCount)
        assertEquals(1, state.creditorsCount)
        assertEquals(2.0, state.totalGoldReceivableGrams)
        assertEquals(1_000L, state.totalCashPayableTomans)
    }
}
