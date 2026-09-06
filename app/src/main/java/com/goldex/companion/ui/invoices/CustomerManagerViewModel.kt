package com.goldex.companion.ui.invoices

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.goldex.companion.data.CustomerRepository
import com.goldex.companion.data.CustomerStore
import com.goldex.companion.model.Customer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class CustomerManagerUiState(
    val customerList: List<Customer> = emptyList(),
    val selectedCustomer: Customer? = null,
    val isCustomerManagerVisible: Boolean = false,
    val isAddCustomerDialogVisible: Boolean = false,
    val isCustomerPickerVisible: Boolean = false
)

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
