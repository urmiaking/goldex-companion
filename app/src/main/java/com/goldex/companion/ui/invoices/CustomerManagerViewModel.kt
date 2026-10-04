package com.goldex.companion.ui.invoices

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.goldex.companion.ui.util.FeatureWorkQueue
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import com.goldex.companion.domain.customers.customerStatementBalances
import com.goldex.companion.domain.customers.applyEffect
import com.goldex.companion.domain.customers.balanceEffect
import com.goldex.companion.data.CustomerStore
import com.goldex.companion.model.Customer
import com.goldex.companion.model.CustomerLedgerFilterTab
import com.goldex.companion.model.LedgerDirection
import com.goldex.companion.model.LedgerEntryType
import com.goldex.companion.model.LedgerTransaction
import com.goldex.companion.model.StatementFilterTab
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class CustomerManagerUiState(
    val customerList: List<Customer> = emptyList(),
    val selectedCustomer: Customer? = null,
    val isCustomerManagerVisible: Boolean = false,
    val isAddCustomerDialogVisible: Boolean = false,
    val isCustomerPickerVisible: Boolean = false,
    // Full Screen Ledger & Statement Navigation
    val isCustomerLedgerVisible: Boolean = false,
    val selectedCustomerForStatement: Customer? = null,
    val isAddLedgerEntryModalVisible: Boolean = false,
    val ledgerEntryTargetCustomer: Customer? = null,
    val selectedLedgerFilter: CustomerLedgerFilterTab = CustomerLedgerFilterTab.ALL,
    val selectedStatementFilter: StatementFilterTab = StatementFilterTab.ALL,
    val searchQuery: String = "",
    val activeCustomerTransactions: List<LedgerTransaction> = emptyList(),
    val editingLedgerTransaction: LedgerTransaction? = null,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null,
    val statementBalances: Map<String, Pair<Double, Long>> = emptyMap(),
    val totals: CustomerListTotals = CustomerListTotals(customerList)
) {
    val filteredCustomers: List<Customer> by lazy {
            val query = searchQuery.trim()
            val baseList = when (selectedLedgerFilter) {
                CustomerLedgerFilterTab.ALL -> customerList
                CustomerLedgerFilterTab.DEBTORS -> customerList.filter { it.goldDebtGrams > 1e-10 || it.cashDebtTomans > 0L }
                CustomerLedgerFilterTab.CREDITORS -> customerList.filter { it.goldDebtGrams < -1e-10 || it.cashDebtTomans < 0L }
                CustomerLedgerFilterTab.SETTLED -> customerList.filter { Math.abs(it.goldDebtGrams) <= 1e-10 && it.cashDebtTomans == 0L }
            }
            if (query.isBlank()) return@lazy baseList
            baseList.filter { c ->
                c.name.contains(query, ignoreCase = true) ||
                c.role.contains(query, ignoreCase = true) ||
                c.cityOrMarket.contains(query, ignoreCase = true) ||
                c.phone.contains(query) ||
                c.accountCode.contains(query)
            }
        }

    val filteredStatementTransactions: List<LedgerTransaction> by lazy {
            when (selectedStatementFilter) {
                StatementFilterTab.ALL -> activeCustomerTransactions
                StatementFilterTab.GOLD_SALE -> activeCustomerTransactions.filter { it.settlement?.offsetExistingCredit != true && it.type == LedgerEntryType.GOLD_WEIGHT && it.direction == LedgerDirection.PAY }
                StatementFilterTab.GOLD_RECEIPT -> activeCustomerTransactions.filter { it.settlement?.offsetExistingCredit != true && it.type == LedgerEntryType.GOLD_WEIGHT && it.direction == LedgerDirection.RECEIVE }
                StatementFilterTab.CASH_DEPOSIT -> activeCustomerTransactions.filter { it.settlement?.offsetExistingCredit != true && it.type == LedgerEntryType.CASH_RIAL }
                StatementFilterTab.SETTLEMENT -> activeCustomerTransactions.filter { it.title.contains("تسویه") || it.note.contains("تسویه") || it.tagBadge.contains("تهاتر") }
            }
        }

    val totalActiveCount: Int get() = customerList.size
    val debtorsCount: Int get() = totals.debtors
    val creditorsCount: Int get() = totals.creditors
    val settledCount: Int get() = totals.settled
    val totalGoldReceivableGrams: Double get() = totals.goldReceivable
    val totalCashReceivableTomans: Long get() = totals.cashReceivable
    val totalGoldPayableGrams: Double get() = totals.goldPayable
    val totalCashPayableTomans: Long get() = totals.cashPayable

}

class CustomerManagerViewModel(
    private val repository: CustomerStore,
    private val unit: com.goldex.companion.data.sync.SyncUnitOfWork? = null,
    private val invoices: com.goldex.companion.data.InvoiceStore? = null,
    private val workDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {
    private val _uiState = MutableStateFlow(CustomerManagerUiState())
    val uiState: StateFlow<CustomerManagerUiState> = _uiState.asStateFlow()

    private val work = FeatureWorkQueue(viewModelScope, workDispatcher,
        onBusy = { busy -> _uiState.update { it.copy(isLoading = busy, isSaving = if (busy) it.isSaving else false) } },
        onError = { error -> _uiState.update { it.copy(error = if (error is IllegalArgumentException)
            error.message ?: "اطلاعات سند معتبر نیست" else "عملیات دفتر حساب انجام نشد؛ دوباره تلاش کنید") } })

    init { loadCustomers() }

    fun setCustomers(customers: List<Customer>) {
        val totals = CustomerListTotals(customers)
        _uiState.update { current ->
            val selected = customers.firstOrNull { it.id == current.selectedCustomer?.id }
            current.copy(customerList = customers, totals = totals, selectedCustomer = selected ?: current.selectedCustomer, error = null)
        }
    }

    fun loadCustomers() { work.submit { setCustomers(repository.getCustomers()) } }

    fun openCustomerLedger() {
        _uiState.update { it.copy(isCustomerLedgerVisible = true, selectedCustomerForStatement = null, isAddLedgerEntryModalVisible = false) }
        loadCustomers()
    }

    fun closeCustomerLedger() {
        _uiState.update {
            it.copy(
                isCustomerLedgerVisible = false,
                selectedCustomerForStatement = null,
                isAddLedgerEntryModalVisible = false
            )
        }
    }

    fun openCustomerStatement(customer: Customer) {
        _uiState.update { it.copy(selectedCustomerForStatement = customer, activeCustomerTransactions = emptyList(),
            statementBalances = emptyMap(), selectedStatementFilter = StatementFilterTab.ALL, error = null) }
        work.submit(onFailure = {
            _uiState.update { state -> if (state.selectedCustomerForStatement?.id == customer.id)
                state.copy(error = "بارگذاری گردش حساب انجام نشد؛ دوباره تلاش کنید") else state }
        }) {
            // Skip obsolete queued selections; an in-flight read must not replace a newer customer.
            if (_uiState.value.selectedCustomerForStatement?.id != customer.id) return@submit
            val fresh = repository.getCustomers().firstOrNull { it.id == customer.id } ?: customer
            val txs = repository.getTransactions(customer.id)
            val balances = customerStatementBalances(fresh, txs)
            _uiState.update { if (it.selectedCustomerForStatement?.id == customer.id)
                it.copy(selectedCustomerForStatement = fresh, activeCustomerTransactions = txs, statementBalances = balances) else it }
        }
    }

    fun closeCustomerStatement() {
        _uiState.update { it.copy(selectedCustomerForStatement = null) }
    }

    fun openAddLedgerEntry(customer: Customer) {
        _uiState.update {
            it.copy(
                isAddLedgerEntryModalVisible = true,
                ledgerEntryTargetCustomer = customer,
                editingLedgerTransaction = null
            )
        }
    }

    fun openEditLedgerEntry(transaction: LedgerTransaction) {
        val target = requireNotNull(_uiState.value.customerList.firstOrNull { it.id == transaction.customerId }) { "Ledger owner missing" }
        _uiState.update {
            it.copy(
                isAddLedgerEntryModalVisible = true,
                ledgerEntryTargetCustomer = target,
                editingLedgerTransaction = transaction
            )
        }
    }

    fun closeAddLedgerEntry() {
        _uiState.update {
            it.copy(
                isAddLedgerEntryModalVisible = false,
                ledgerEntryTargetCustomer = null,
                editingLedgerTransaction = null
            )
        }
    }

    fun deleteLedgerEntry(transaction: LedgerTransaction, onComplete: () -> Unit = {}) {
        if (_uiState.value.isSaving) return
        _uiState.update { it.copy(isSaving = true, error = null) }
        work.submit(onSuccess = onComplete) { write { deleteLedgerEntryInternal(transaction) }; refreshStatementData() }
    }

    private fun deleteLedgerEntryInternal(transaction: LedgerTransaction) {
        val saved = repository.getTransactions(transaction.customerId).firstOrNull { it.id == transaction.id } ?: return
        val customer = requireNotNull(repository.getCustomers().firstOrNull { it.id == saved.customerId })
        saved.invoiceId?.let { id ->
            invoices?.getBarterInvoices()?.firstOrNull { it.id == id }?.let { invoice ->
                invoices.saveBarterInvoice(invoice.copy(payments = invoice.payments.filterNot { it.id == saved.id }))
            }
        }
        repository.deleteTransaction(saved.id)
        repository.updateCustomer(customer.applyEffect(saved.balanceEffect(), reverse = true))
    }

    fun saveLedgerEntry(transaction: LedgerTransaction, onComplete: () -> Unit = {}) {
        if (_uiState.value.isSaving) return
        _uiState.update { it.copy(isSaving = true, error = null) }
        val editing = _uiState.value.editingLedgerTransaction
        work.submit(onSuccess = onComplete) { write { saveLedgerEntryInternal(transaction, editing) }; refreshStatementData() }
    }

    private fun saveLedgerEntryInternal(transaction: LedgerTransaction, editing: LedgerTransaction?) {
        require(transaction.settlement == null && editing?.settlement == null) { "برای اصلاح تسویه، آن را حذف و دوباره ثبت کنید" }
        val customer = requireNotNull(repository.getCustomers().firstOrNull { it.id == transaction.customerId }) { "Ledger owner missing" }
        require(editing == null || editing.customerId == customer.id) { "Ledger owner cannot change during edit" }
        val base = if (editing == null) customer else customer.applyEffect(editing.balanceEffect(), reverse = true)
        val updated = base.applyEffect(transaction.balanceEffect()).copy(lastActivityTime = "لحظاتی پیش")
        val tx = transaction.copy(resultingGoldBalance = updated.goldDebtGrams, resultingCashBalance = updated.cashDebtTomans)
        if (editing == null) repository.addTransaction(tx) else repository.updateTransaction(tx)
        repository.updateCustomer(updated)
    }

    fun refreshAfterSettlement() { work.submit { refreshStatementData() } }

    private fun write(action: () -> Unit) { if(unit == null) action() else unit.transaction(action) }

    private fun refreshStatementData() {
        val updatedCustomers = repository.getCustomers()
        val currentStatementCust = _uiState.value.selectedCustomerForStatement
        val refreshedStatementCust = if (currentStatementCust != null) {
            updatedCustomers.firstOrNull { it.id == currentStatementCust.id } ?: currentStatementCust
        } else null

        val totals = CustomerListTotals(updatedCustomers)
        val updatedTxs = if (refreshedStatementCust != null) {
            repository.getTransactions(refreshedStatementCust.id)
        } else emptyList()

        val balances = refreshedStatementCust?.let { customerStatementBalances(it, updatedTxs) }.orEmpty()
        _uiState.update {
            val sameStatement = it.selectedCustomerForStatement?.id == currentStatementCust?.id
            it.copy(
                customerList = updatedCustomers, totals = totals, error = null,
                selectedCustomerForStatement = if (sameStatement) refreshedStatementCust else it.selectedCustomerForStatement,
                activeCustomerTransactions = if (sameStatement) updatedTxs else it.activeCustomerTransactions,
                statementBalances = if (sameStatement) balances else it.statementBalances,
                isAddLedgerEntryModalVisible = false,
                ledgerEntryTargetCustomer = null,
                editingLedgerTransaction = null
            )
        }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun setLedgerFilter(filter: CustomerLedgerFilterTab) {
        _uiState.update { it.copy(selectedLedgerFilter = filter) }
    }

    fun setStatementFilter(filter: StatementFilterTab) {
        _uiState.update { it.copy(selectedStatementFilter = filter) }
    }

    fun setCustomerManagerVisible(visible: Boolean) {
        _uiState.update { it.copy(isCustomerManagerVisible = visible) }
    }

    fun setCustomerPickerVisible(visible: Boolean) {
        _uiState.update { it.copy(isCustomerPickerVisible = visible) }
    }

    fun setAddCustomerDialogVisible(visible: Boolean) {
        _uiState.update { it.copy(isAddCustomerDialogVisible = visible) }
    }

    fun selectCustomer(customer: Customer?) {
        _uiState.update { it.copy(selectedCustomer = customer, isCustomerPickerVisible = false) }
    }

    fun addCustomer(customer: Customer, autoSelect: Boolean = true, onComplete: () -> Unit = {}) {
        if (_uiState.value.isSaving) return
        _uiState.update { it.copy(isSaving = true, error = null) }
        work.submit(onSuccess = onComplete) {
            repository.addCustomer(customer)
            val updated = repository.getCustomers()
            val totals = CustomerListTotals(updated)
            _uiState.update {
                it.copy(
                    customerList = updated, totals = totals, error = null,
                    selectedCustomer = if (autoSelect) customer else it.selectedCustomer,
                    isAddCustomerDialogVisible = false,
                    isCustomerPickerVisible = false
                )
            }

        }
    }

    fun updateCustomer(customer: Customer) {
        if (_uiState.value.isSaving) return
        _uiState.update { it.copy(isSaving = true, error = null) }
        work.submit {
            repository.updateCustomer(customer)
            val updated = repository.getCustomers()
            val totals = CustomerListTotals(updated)
            _uiState.update {
                it.copy(
                    customerList = updated, totals = totals, error = null,
                    selectedCustomer = if (it.selectedCustomer?.id == customer.id) customer else it.selectedCustomer
                )
            }

        }
    }

    fun deleteCustomer(customerId: String) {
        if (_uiState.value.isSaving) return
        _uiState.update { it.copy(isSaving = true, error = null) }
        work.submit {
            repository.deleteCustomer(customerId)
            val updated = repository.getCustomers()
            val totals = CustomerListTotals(updated)
            _uiState.update {
                it.copy(
                    customerList = updated, totals = totals, error = null,
                    selectedCustomer = if (it.selectedCustomer?.id == customerId) null else it.selectedCustomer
                )
            }

        }
    }
}

class CustomerListTotals(customers: List<Customer>) {
    val debtors = customers.count { it.goldDebtGrams > 1e-10 || it.cashDebtTomans > 0L }
    val creditors = customers.count { it.goldDebtGrams < -1e-10 || it.cashDebtTomans < 0L }
    val settled = customers.count { kotlin.math.abs(it.goldDebtGrams) <= 1e-10 && it.cashDebtTomans == 0L }
    val goldReceivable = customers.sumOf { it.goldDebtGrams.coerceAtLeast(0.0) }
    val cashReceivable = customers.sumOf { it.cashDebtTomans.coerceAtLeast(0L) }
    val goldPayable = customers.filter { it.goldDebtGrams < 0.0 }.sumOf { kotlin.math.abs(it.goldDebtGrams) }
    val cashPayable = customers.filter { it.cashDebtTomans < 0L }.sumOf { kotlin.math.abs(it.cashDebtTomans) }
}
