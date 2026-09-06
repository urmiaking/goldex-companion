package com.goldex.companion.ui.invoices

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.goldex.companion.data.InvoiceRepository
import com.goldex.companion.data.InvoiceStore
import com.goldex.companion.model.Invoice
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class InvoiceManagerUiState(
    val savedInvoices: List<Invoice> = emptyList(),
    val isInvoiceManagerVisible: Boolean = false
)

class InvoiceManagerViewModel(
    private val repository: InvoiceStore
) : ViewModel() {
    private val _uiState = MutableStateFlow(InvoiceManagerUiState())
    val uiState: StateFlow<InvoiceManagerUiState> = _uiState.asStateFlow()

    init {
        _uiState.value = InvoiceManagerUiState(savedInvoices = repository.getInvoices())
    }

    fun setInvoices(invoices: List<Invoice>) {
        _uiState.update { it.copy(savedInvoices = invoices) }
    }

    fun loadInvoices() {
        _uiState.update { it.copy(savedInvoices = repository.getInvoices()) }
    }

    fun setInvoiceManagerVisible(visible: Boolean) {
        _uiState.update { it.copy(isInvoiceManagerVisible = visible) }
    }

    fun saveInvoice(invoice: Invoice) {
        repository.saveInvoice(invoice)
        _uiState.update { it.copy(savedInvoices = repository.getInvoices()) }
    }

    fun deleteInvoice(id: String) {
        repository.deleteInvoice(id)
        _uiState.update { it.copy(savedInvoices = repository.getInvoices()) }
    }
}

class InvoiceManagerViewModelFactory(
    private val application: Application
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val repository = InvoiceRepository(application.applicationContext)
        return InvoiceManagerViewModel(repository) as T
    }
}
