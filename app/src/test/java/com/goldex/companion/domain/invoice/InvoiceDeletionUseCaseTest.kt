package com.goldex.companion.domain.invoice

import com.goldex.companion.data.CustomerStore
import com.goldex.companion.data.InvoiceStore
import com.goldex.companion.model.*
import com.goldex.companion.ui.invoices.BarterInvoiceViewModel
import com.goldex.companion.ui.invoices.InvoicesSubScreen
import org.junit.Assert.*
import org.junit.Test

class InvoiceDeletionUseCaseTest {
    private val opening = Customer(id = "customer", name = "مشتری", goldDebtGrams = 4.125, cashDebtTomans = -80_000L)
    private val sale = MeltGoldItem(weight = 10.0, spotPrice = 1_000_000L, totalPayable = 10_000_000.0, equivalent18kWeight = 10.0)
    private fun invoice(role: CustomerRole = CustomerRole.RETAIL, payments: List<SettlementPaymentItem> = emptyList()) =
        BarterInvoice(id = "invoice", customer = opening, customerRole = role, spotPrice18k = 1_000_000L,
            salesItems = listOf(sale), payments = payments)

    @Test
    fun cashSaleAndPurchaseReverseEverySettlementMethodWithoutDoubleReversal() {
        for (method in SettlementMethod.entries) {
            for (purchase in listOf(false, true)) {
                val base = invoice(payments = listOf(SettlementPaymentItem(method = method, amountTomans = 2_000_000L, goldWeight18k = 2.0)))
                val saved = if (purchase) base.copy(salesItems = emptyList(), receivedItems = listOf(sale)) else base
                val sync = InvoiceLedgerSyncUseCase.generateLedgerSync(saved, opening)
                assertTrue(sync.transactionsToCreate.isNotEmpty())
                val store = MemoryInvoiceStore(saved)
                val ledger = MemoryCustomerStore(listOf(sync.updatedCustomer), sync.transactionsToCreate)
                val useCase = InvoiceDeletionUseCase(store, ledger)

                assertTrue(useCase.delete(saved.id) is InvoiceDeletionResult.Deleted)
                assertBalances(opening, ledger.getCustomers().single())
                assertTrue(store.getBarterInvoices().isEmpty())
                assertTrue(ledger.entries.isEmpty())
                assertEquals(InvoiceDeletionResult.NotFound, useCase.delete(saved.id))
                assertBalances(opening, ledger.getCustomers().single())
            }
        }
    }

    @Test
    fun wholesalerMixedCashGoldAndDeferredPaymentsReturnToOpeningBalances() {
        val saved = invoice(CustomerRole.WHOLESALER, listOf(
            SettlementPaymentItem(method = SettlementMethod.POS, amountTomans = 1_000_000L),
            SettlementPaymentItem(method = SettlementMethod.TRANSFER, goldWeight18k = 0.875),
            SettlementPaymentItem(method = SettlementMethod.BULLION, goldWeight18k = 2.125),
            SettlementPaymentItem(method = SettlementMethod.LEDGER, amountTomans = 500_000L)
        ))
        val sync = InvoiceLedgerSyncUseCase.generateLedgerSync(saved, opening)
        assertEquals(5, sync.transactionsToCreate.size)
        val ledger = MemoryCustomerStore(listOf(sync.updatedCustomer), sync.transactionsToCreate)
        assertTrue(InvoiceDeletionUseCase(MemoryInvoiceStore(saved), ledger).delete(saved.id) is InvoiceDeletionResult.Deleted)
        assertBalances(opening, ledger.getCustomers().single())
        assertTrue(ledger.entries.isEmpty())
    }

    @Test
    fun legacyPaymentFieldsReverseRecordedEntries() {
        val saved = invoice().copy(cashPosAmount = 2_000_000L)
        val sync = InvoiceLedgerSyncUseCase.generateLedgerSync(saved, opening)
        assertEquals(2, sync.transactionsToCreate.size)
        val ledger = MemoryCustomerStore(listOf(sync.updatedCustomer), sync.transactionsToCreate)
        assertTrue(InvoiceDeletionUseCase(MemoryInvoiceStore(saved), ledger).delete(saved.id) is InvoiceDeletionResult.Deleted)
        assertBalances(opening, ledger.getCustomers().single())
    }

    @Test
    fun usesActualEntryOwnersAndAmountsDespiteStaleCustomerAndDisabledSync() {
        val saved = invoice().copy(customer = null, syncWithLedger = false)
        val other = Customer(id = "other", name = "همکار", cashDebtTomans = -300L)
        val cash = LedgerTransaction(id = "cash", customerId = opening.id, invoiceId = saved.id,
            type = LedgerEntryType.CASH_RIAL, direction = LedgerDirection.PAY, amountTomans = 700L)
        val gold = LedgerTransaction(id = "gold", customerId = opening.id, invoiceId = saved.id,
            type = LedgerEntryType.GOLD_WEIGHT, direction = LedgerDirection.RECEIVE, equivalent750WeightGrams = 0.125)
        val payment = cash.copy(id = "payment", customerId = other.id, direction = LedgerDirection.RECEIVE, amountTomans = 200L)
        val unrelated = cash.copy(id = "manual", invoiceId = null, amountTomans = 90L)
        val otherInvoice = cash.copy(id = "other-invoice", invoiceId = "keep", amountTomans = 60L)
        val ledger = MemoryCustomerStore(listOf(opening.copy(cashDebtTomans = opening.cashDebtTomans + 850L,
            goldDebtGrams = opening.goldDebtGrams - 0.125), other.copy(cashDebtTomans = -500L)),
            listOf(cash, gold, payment, unrelated, otherInvoice))
        val keep = saved.copy(id = "keep")
        val store = MemoryInvoiceStore(saved, keep)

        assertTrue(InvoiceDeletionUseCase(store, ledger).delete(saved.id) is InvoiceDeletionResult.Deleted)
        assertBalances(opening.copy(cashDebtTomans = opening.cashDebtTomans + 150L), ledger.getCustomers().first { it.id == opening.id })
        assertBalances(other, ledger.getCustomers().first { it.id == other.id })
        assertEquals(listOf(unrelated, otherInvoice), ledger.entries)
        assertEquals(listOf(keep), store.getBarterInvoices())
    }

    @Test
    fun unsyncedInvoiceWithNoEntriesDoesNotChangeCustomerBalance() {
        val saved = invoice().copy(syncWithLedger = false)
        val ledger = MemoryCustomerStore(listOf(opening))
        assertTrue(InvoiceDeletionUseCase(MemoryInvoiceStore(saved), ledger).delete(saved.id) is InvoiceDeletionResult.Deleted)
        assertEquals(listOf(opening), ledger.getCustomers())
        assertTrue(ledger.entries.isEmpty())
    }

    @Test
    fun missingCustomerBlocksBeforeAnyMutation() {
        val saved = invoice()
        val valid = LedgerTransaction(customerId = opening.id, invoiceId = saved.id)
        val orphan = valid.copy(id = "orphan", customerId = "missing")
        val ledger = MemoryCustomerStore(listOf(opening), listOf(valid, orphan))
        val store = MemoryInvoiceStore(saved)
        assertEquals(InvoiceDeletionResult.MissingCustomer, InvoiceDeletionUseCase(store, ledger).delete(saved.id))
        assertEquals(listOf(opening), ledger.getCustomers())
        assertEquals(listOf(valid, orphan), ledger.entries)
        assertEquals(listOf(saved), store.getBarterInvoices())
    }

    @Test
    fun missingLedgerStoreAndUnknownIdCannotDeleteOrReverseAnything() {
        val saved = invoice()
        val store = MemoryInvoiceStore(saved)
        assertEquals(InvoiceDeletionResult.MissingLedgerStore, InvoiceDeletionUseCase(store, null).delete(saved.id))
        assertEquals(InvoiceDeletionResult.NotFound, InvoiceDeletionUseCase(store, null).delete("unknown"))
        assertEquals(listOf(saved), store.getBarterInvoices())
    }

    @Test
    fun deletingTransferTargetIsBlockedUntilSourceIsRemoved() {
        val target = invoice()
        val source = target.copy(id = "source", settlementMethod = SettlementMethod.TRANSFER, thirdPartyInvoiceId = target.id)
        val store = MemoryInvoiceStore(target, source)
        val useCase = InvoiceDeletionUseCase(store, MemoryCustomerStore())
        assertEquals(InvoiceDeletionResult.ReferencedByTransfer, useCase.delete(target.id))
        assertEquals(listOf(target, source), store.getBarterInvoices())
        assertTrue(useCase.delete(source.id) is InvoiceDeletionResult.Deleted)
        assertTrue(useCase.delete(target.id) is InvoiceDeletionResult.Deleted)
    }

    @Test
    fun reportedFailuresAtEachWriteRestoreInvoiceEntriesAndCustomerBalances() {
        for (failure in listOf("customer", "transactions", "invoice")) {
            val saved = invoice(payments = listOf(SettlementPaymentItem(method = SettlementMethod.POS, amountTomans = 2_000_000L)))
            val sync = InvoiceLedgerSyncUseCase.generateLedgerSync(saved, opening)
            val store = MemoryInvoiceStore(saved).apply { failDeleteOnce = failure == "invoice" }
            val ledger = MemoryCustomerStore(listOf(sync.updatedCustomer), sync.transactionsToCreate).apply {
                failUpdateOnce = failure == "customer"
                failDeleteOnce = failure == "transactions"
            }
            val useCase = InvoiceDeletionUseCase(store, ledger)
            assertEquals(InvoiceDeletionResult.Failed(true), useCase.delete(saved.id))
            assertEquals(listOf(saved), store.getBarterInvoices())
            assertEquals(sync.transactionsToCreate.toSet(), ledger.entries.toSet())
            assertEquals(listOf(sync.updatedCustomer), ledger.getCustomers())
            assertTrue(useCase.delete(saved.id) is InvoiceDeletionResult.Deleted)
            assertBalances(opening, ledger.getCustomers().single())
        }
    }

    @Test
    fun rollbackFailureIsReportedExplicitly() {
        val saved = invoice()
        val store = MemoryInvoiceStore(saved).apply { failDeleteOnce = true; failSave = true }
        val ledger = MemoryCustomerStore(listOf(opening))
        assertEquals(InvoiceDeletionResult.Failed(false), InvoiceDeletionUseCase(store, ledger).delete(saved.id))
    }

    @Test
    fun viewModelClearsDeletedEditorAndPreservesSearchAndFilter() {
        val saved = invoice()
        val store = MemoryInvoiceStore(saved)
        val viewModel = BarterInvoiceViewModel(store, MemoryCustomerStore(listOf(opening)))
        viewModel.openInvoiceDetails(viewModel.uiState.value.invoicesList.single())
        viewModel.setSearchQuery("مشتری")
        viewModel.setSelectedFilter(InvoiceFilterTab.PENDING)
        viewModel.openEditItemModal(sale, true)
        assertTrue(viewModel.deleteInvoice(saved.id))
        val state = viewModel.uiState.value
        assertTrue(state.invoicesList.isEmpty())
        assertEquals(InvoicesSubScreen.LIST, state.subScreen)
        assertFalse(state.isEditingExistingInvoice)
        assertFalse(state.isItemModalVisible)
        assertNull(state.editingItem)
        assertNotEquals(saved.id, state.invoice.id)
        assertTrue(state.isSuccessSnackbarVisible)
        assertEquals("مشتری", state.searchQuery)
        assertEquals(InvoiceFilterTab.PENDING, state.selectedFilter)
        assertFalse(viewModel.deleteInvoice(saved.id))
    }

    @Test
    fun failedDeletionKeepsCardAndEditorAndShowsFailure() {
        val saved = invoice()
        val store = MemoryInvoiceStore(saved).apply { failDeleteOnce = true }
        val viewModel = BarterInvoiceViewModel(store, MemoryCustomerStore(listOf(opening)))
        viewModel.openInvoiceDetails(viewModel.uiState.value.invoicesList.single())
        assertFalse(viewModel.deleteInvoice(saved.id))
        assertEquals(saved, viewModel.uiState.value.invoice)
        assertEquals(saved.id, viewModel.uiState.value.invoicesList.single().id)
        assertEquals(InvoicesSubScreen.EDITOR, viewModel.uiState.value.subScreen)
        assertFalse(viewModel.uiState.value.isSuccessSnackbarVisible)
        assertTrue(viewModel.uiState.value.statusMessage.isNotBlank())
    }

    @Test
    fun deletingTransferSourceRebuildsTargetWithRemainingTransfersAndSurvivesReload() {
        val target = invoice().copy(id = "target")
        val source = target.copy(id = "source", settlementMethod = SettlementMethod.TRANSFER,
            thirdPartyInvoiceId = target.id, thirdPartyTransferAmount = 2_000_000L)
        val remaining = source.copy(id = "remaining", thirdPartyTransferAmount = 1_000_000L)
        val store = MemoryInvoiceStore(source, remaining, target)
        val ledger = MemoryCustomerStore(listOf(opening))
        val viewModel = BarterInvoiceViewModel(store, ledger)
        fun targetAmount(vm: BarterInvoiceViewModel) = vm.uiState.value.invoicesList.first { it.id == target.id }.finalAmount
        assertEquals(7_000_000L, targetAmount(viewModel))
        assertTrue(viewModel.deleteInvoice(source.id))
        assertEquals(9_000_000L, targetAmount(viewModel))
        assertEquals(9_000_000L, targetAmount(BarterInvoiceViewModel(store, ledger)))
        assertEquals(target, store.getBarterInvoices().first { it.id == target.id })
    }

    @Test
    fun resavingTransferSourceDoesNotAccumulateDeductionsAndDeletionRestoresTarget() {
        val target = invoice().copy(id = "target")
        val source = target.copy(id = "source", settlementMethod = SettlementMethod.TRANSFER,
            thirdPartyInvoiceId = target.id, thirdPartyTransferAmount = 2_000_000L, syncWithLedger = false)
        val store = MemoryInvoiceStore(source, target)
        val viewModel = BarterInvoiceViewModel(store, MemoryCustomerStore(listOf(opening)))
        viewModel.openInvoiceDetails(viewModel.uiState.value.invoicesList.first { it.id == source.id })
        viewModel.submitAndSaveCurrentInvoice()
        viewModel.submitAndSaveCurrentInvoice()
        assertEquals(8_000_000L, viewModel.uiState.value.invoicesList.first { it.id == target.id }.finalAmount)
        assertTrue(viewModel.deleteInvoice(source.id))
        assertEquals(10_000_000L, viewModel.uiState.value.invoicesList.single().finalAmount)
    }

    private fun assertBalances(expected: Customer, actual: Customer) {
        assertEquals(expected.goldDebtGrams, actual.goldDebtGrams, 0.000001)
        assertEquals(expected.cashDebtTomans, actual.cashDebtTomans)
    }
}

private class MemoryInvoiceStore(vararg initial: BarterInvoice) : InvoiceStore {
    private val saved = initial.toMutableList()
    var failDeleteOnce = false
    var failSave = false
    override fun getInvoices(): List<Invoice> = emptyList()
    override fun saveInvoice(invoice: Invoice) = Unit
    override fun deleteInvoice(id: String) = Unit
    override fun getBarterInvoices() = saved.toList()
    override fun saveBarterInvoice(invoice: BarterInvoice) {
        check(!failSave)
        val index = saved.indexOfFirst { it.id == invoice.id }
        if (index >= 0) saved[index] = invoice else saved.add(0, invoice)
    }
    override fun deleteBarterInvoice(id: String) {
        saved.removeAll { it.id == id }
        if (failDeleteOnce) { failDeleteOnce = false; error("invoice write failed after mutation") }
    }
}

private class MemoryCustomerStore(
    initial: List<Customer> = emptyList(),
    initialEntries: List<LedgerTransaction> = emptyList()
) : CustomerStore {
    private val saved = initial.toMutableList()
    val entries = initialEntries.toMutableList()
    var failUpdateOnce = false
    var failDeleteOnce = false
    override fun getCustomers() = saved.toList()
    override fun addCustomer(customer: Customer) { saved.add(customer) }
    override fun deleteCustomer(id: String) { saved.removeAll { it.id == id } }
    override fun updateCustomer(customer: Customer) {
        val index = saved.indexOfFirst { it.id == customer.id }
        check(index >= 0)
        saved[index] = customer
        if (failUpdateOnce) { failUpdateOnce = false; error("customer write failed after mutation") }
    }
    override fun getTransactions(customerId: String) = entries.filter { it.customerId == customerId }
    override fun addTransaction(transaction: LedgerTransaction) { entries.add(0, transaction) }
    override fun getTransactionsByInvoiceId(invoiceId: String) = entries.filter { it.invoiceId == invoiceId }
    override fun deleteTransactionsByInvoiceId(invoiceId: String) {
        entries.removeAll { it.invoiceId == invoiceId }
        if (failDeleteOnce) { failDeleteOnce = false; error("ledger write failed after mutation") }
    }
}
