package com.goldex.companion.ui.invoices

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.goldex.companion.data.CustomerRepository
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
    val editingLedgerTransaction: LedgerTransaction? = null
) {
    val filteredCustomers: List<Customer>
        get() {
            val query = searchQuery.trim()
            val baseList = when (selectedLedgerFilter) {
                CustomerLedgerFilterTab.ALL -> customerList
                CustomerLedgerFilterTab.DEBTORS -> customerList.filter { it.goldDebtGrams > 0.001 || it.cashDebtTomans > 0L }
                CustomerLedgerFilterTab.CREDITORS -> customerList.filter { it.goldDebtGrams < -0.001 || it.cashDebtTomans < 0L }
                CustomerLedgerFilterTab.SETTLED -> customerList.filter { Math.abs(it.goldDebtGrams) <= 0.001 && it.cashDebtTomans == 0L }
            }
            if (query.isBlank()) return baseList
            return baseList.filter { c ->
                c.name.contains(query, ignoreCase = true) ||
                c.role.contains(query, ignoreCase = true) ||
                c.cityOrMarket.contains(query, ignoreCase = true) ||
                c.phone.contains(query) ||
                c.accountCode.contains(query)
            }
        }

    val filteredStatementTransactions: List<LedgerTransaction>
        get() {
            return when (selectedStatementFilter) {
                StatementFilterTab.ALL -> activeCustomerTransactions
                StatementFilterTab.GOLD_SALE -> activeCustomerTransactions.filter { it.type == LedgerEntryType.GOLD_WEIGHT && it.direction == LedgerDirection.PAY }
                StatementFilterTab.GOLD_RECEIPT -> activeCustomerTransactions.filter { it.type == LedgerEntryType.GOLD_WEIGHT && it.direction == LedgerDirection.RECEIVE }
                StatementFilterTab.CASH_DEPOSIT -> activeCustomerTransactions.filter { it.type == LedgerEntryType.CASH_RIAL }
                StatementFilterTab.SETTLEMENT -> activeCustomerTransactions.filter { it.title.contains("تسویه") || it.note.contains("تسویه") || it.tagBadge.contains("تهاتر") }
            }
        }

    val totalActiveCount: Int
        get() = customerList.size

    val debtorsCount: Int
        get() = customerList.count { it.goldDebtGrams > 0.001 || it.cashDebtTomans > 0L }

    val creditorsCount: Int
        get() = customerList.count { it.goldDebtGrams < -0.001 || it.cashDebtTomans < 0L }

    val settledCount: Int
        get() = customerList.count { Math.abs(it.goldDebtGrams) <= 0.001 && it.cashDebtTomans == 0L }

    val totalGoldReceivableGrams: Double
        get() = customerList.filter { it.goldDebtGrams > 0.0 }.sumOf { it.goldDebtGrams }

    val totalCashReceivableTomans: Long
        get() = customerList.filter { it.cashDebtTomans > 0L }.sumOf { it.cashDebtTomans }

    val totalGoldPayableGrams: Double
        get() = customerList.filter { it.goldDebtGrams < 0.0 }.sumOf { Math.abs(it.goldDebtGrams) }

    val totalCashPayableTomans: Long
        get() = customerList.filter { it.cashDebtTomans < 0L }.sumOf { Math.abs(it.cashDebtTomans) }
}

class CustomerManagerViewModel(
    private val repository: CustomerStore
) : ViewModel() {
    private val _uiState = MutableStateFlow(CustomerManagerUiState())
    val uiState: StateFlow<CustomerManagerUiState> = _uiState.asStateFlow()

    init {
        _uiState.value = CustomerManagerUiState(customerList = repository.getCustomers())
    }

    fun setCustomers(customers: List<Customer>) {
        _uiState.update { current ->
            val selected = customers.firstOrNull { it.id == current.selectedCustomer?.id }
            current.copy(customerList = customers, selectedCustomer = selected ?: current.selectedCustomer)
        }
    }

    fun loadCustomers() {
        _uiState.update { it.copy(customerList = repository.getCustomers()) }
    }

    fun openCustomerLedger() {
        val list = repository.getCustomers()
        _uiState.update {
            it.copy(
                isCustomerLedgerVisible = true,
                customerList = list,
                selectedCustomerForStatement = null,
                isAddLedgerEntryModalVisible = false
            )
        }
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
        val txs = repository.getTransactions(customer.id)
        _uiState.update {
            it.copy(
                selectedCustomerForStatement = customer,
                activeCustomerTransactions = txs,
                selectedStatementFilter = StatementFilterTab.ALL
            )
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
        val target = _uiState.value.customerList.firstOrNull { it.id == transaction.customerId }
            ?: _uiState.value.selectedCustomerForStatement
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

    fun deleteLedgerEntry(transaction: LedgerTransaction) {
        repository.deleteTransaction(transaction.id)

        val target = _uiState.value.customerList.firstOrNull { it.id == transaction.customerId }
            ?: _uiState.value.selectedCustomerForStatement

        if (target != null) {
            val updatedCustomer = when (transaction.type) {
                LedgerEntryType.GOLD_WEIGHT -> {
                    val delta = if (transaction.direction == LedgerDirection.PAY) {
                        transaction.equivalent750WeightGrams
                    } else {
                        -transaction.equivalent750WeightGrams
                    }
                    target.copy(
                        goldDebtGrams = target.goldDebtGrams - delta,
                        lastActivityTime = "لحظاتی پیش"
                    )
                }
                LedgerEntryType.CASH_RIAL -> {
                    val delta = if (transaction.direction == LedgerDirection.PAY) {
                        transaction.amountTomans
                    } else {
                        -transaction.amountTomans
                    }
                    target.copy(
                        cashDebtTomans = target.cashDebtTomans - delta,
                        lastActivityTime = "لحظاتی پیش"
                    )
                }
            }
            repository.updateCustomer(updatedCustomer)
        }

        refreshStatementData()
    }

    fun saveLedgerEntry(transaction: LedgerTransaction) {
        val editing = _uiState.value.editingLedgerTransaction
        val target = _uiState.value.customerList.firstOrNull { it.id == transaction.customerId }
            ?: _uiState.value.ledgerEntryTargetCustomer

        if (editing != null) {
            repository.updateTransaction(transaction)

            if (target != null) {
                // First reverse the old transaction effect
                val intermediateCustomer = when (editing.type) {
                    LedgerEntryType.GOLD_WEIGHT -> {
                        val oldDelta = if (editing.direction == LedgerDirection.PAY) {
                            editing.equivalent750WeightGrams
                        } else {
                            -editing.equivalent750WeightGrams
                        }
                        target.copy(goldDebtGrams = target.goldDebtGrams - oldDelta)
                    }
                    LedgerEntryType.CASH_RIAL -> {
                        val oldDelta = if (editing.direction == LedgerDirection.PAY) {
                            editing.amountTomans
                        } else {
                            -editing.amountTomans
                        }
                        target.copy(cashDebtTomans = target.cashDebtTomans - oldDelta)
                    }
                }

                // Then apply the new transaction effect
                val updatedCustomer = when (transaction.type) {
                    LedgerEntryType.GOLD_WEIGHT -> {
                        val newDelta = if (transaction.direction == LedgerDirection.PAY) {
                            transaction.equivalent750WeightGrams
                        } else {
                            -transaction.equivalent750WeightGrams
                        }
                        intermediateCustomer.copy(
                            goldDebtGrams = intermediateCustomer.goldDebtGrams + newDelta,
                            lastActivityTime = "لحظاتی پیش"
                        )
                    }
                    LedgerEntryType.CASH_RIAL -> {
                        val newDelta = if (transaction.direction == LedgerDirection.PAY) {
                            transaction.amountTomans
                        } else {
                            -transaction.amountTomans
                        }
                        intermediateCustomer.copy(
                            cashDebtTomans = intermediateCustomer.cashDebtTomans + newDelta,
                            lastActivityTime = "لحظاتی پیش"
                        )
                    }
                }
                repository.updateCustomer(updatedCustomer)
            }
        } else {
            repository.addTransaction(transaction)

            if (target != null) {
                val updatedCustomer = when (transaction.type) {
                    LedgerEntryType.GOLD_WEIGHT -> {
                        val delta = if (transaction.direction == LedgerDirection.PAY) {
                            transaction.equivalent750WeightGrams
                        } else {
                            -transaction.equivalent750WeightGrams
                        }
                        target.copy(
                            goldDebtGrams = target.goldDebtGrams + delta,
                            lastActivityTime = "لحظاتی پیش"
                        )
                    }
                    LedgerEntryType.CASH_RIAL -> {
                        val delta = if (transaction.direction == LedgerDirection.PAY) {
                            transaction.amountTomans
                        } else {
                            -transaction.amountTomans
                        }
                        target.copy(
                            cashDebtTomans = target.cashDebtTomans + delta,
                            lastActivityTime = "لحظاتی پیش"
                        )
                    }
                }
                repository.updateCustomer(updatedCustomer)
            }
        }

        refreshStatementData()
    }

    private fun refreshStatementData() {
        val updatedCustomers = repository.getCustomers()
        val currentStatementCust = _uiState.value.selectedCustomerForStatement
        val refreshedStatementCust = if (currentStatementCust != null) {
            updatedCustomers.firstOrNull { it.id == currentStatementCust.id } ?: currentStatementCust
        } else null

        val updatedTxs = if (refreshedStatementCust != null) {
            repository.getTransactions(refreshedStatementCust.id)
        } else emptyList()

        _uiState.update {
            it.copy(
                customerList = updatedCustomers,
                selectedCustomerForStatement = refreshedStatementCust,
                activeCustomerTransactions = updatedTxs,
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

    fun addCustomer(customer: Customer, autoSelect: Boolean = true) {
        repository.addCustomer(customer)
        val updated = repository.getCustomers()
        _uiState.update {
            it.copy(
                customerList = updated,
                selectedCustomer = if (autoSelect) customer else it.selectedCustomer,
                isAddCustomerDialogVisible = false,
                isCustomerPickerVisible = false
            )
        }
    }

    fun updateCustomer(customer: Customer) {
        repository.updateCustomer(customer)
        val updated = repository.getCustomers()
        _uiState.update {
            it.copy(
                customerList = updated,
                selectedCustomer = if (it.selectedCustomer?.id == customer.id) customer else it.selectedCustomer
            )
        }
    }

    fun deleteCustomer(customerId: String) {
        repository.deleteCustomer(customerId)
        val updated = repository.getCustomers()
        _uiState.update {
            it.copy(
                customerList = updated,
                selectedCustomer = if (it.selectedCustomer?.id == customerId) null else it.selectedCustomer
            )
        }
    }
}

class CustomerManagerViewModelFactory(
    private val application: Application
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val repository = CustomerRepository(application.applicationContext)
        return CustomerManagerViewModel(repository) as T
    }
}
