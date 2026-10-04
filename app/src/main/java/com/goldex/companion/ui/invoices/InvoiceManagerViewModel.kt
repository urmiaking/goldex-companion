package com.goldex.companion.ui.invoices

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.goldex.companion.ui.util.FeatureWorkQueue
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import com.goldex.companion.data.InvoiceStore
import com.goldex.companion.model.Invoice
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class InvoiceManagerUiState(
    val savedInvoices: List<Invoice> = emptyList(),
    val isInvoiceManagerVisible: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null
)

class InvoiceManagerViewModel(
    private val repository: InvoiceStore,
    private val workDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {
    private val _uiState = MutableStateFlow(InvoiceManagerUiState())
    val uiState: StateFlow<InvoiceManagerUiState> = _uiState.asStateFlow()

    private val work = FeatureWorkQueue(viewModelScope, workDispatcher,
        onBusy = { busy -> _uiState.update { it.copy(isLoading = busy) } },
        onError = { _ -> _uiState.update { it.copy(error = "عملیات بایگانی فاکتور انجام نشد؛ دوباره تلاش کنید") } })
    init { loadInvoices() }

    fun setInvoices(invoices: List<Invoice>) {
        _uiState.update { it.copy(savedInvoices = invoices) }
    }

    fun loadInvoices() {
        work.submit {
            val saved = repository.getInvoices()
            _uiState.update { it.copy(savedInvoices = saved, error = null) }
        }
    }

    fun setInvoiceManagerVisible(visible: Boolean) {
        _uiState.update { it.copy(isInvoiceManagerVisible = visible) }
    }

    fun saveInvoice(invoice: Invoice) {
        work.submit {
            repository.saveInvoice(invoice)
            val saved = repository.getInvoices()
            _uiState.update { it.copy(savedInvoices = saved, error = null) }
        }
    }

    fun deleteInvoice(id: String) {
        work.submit {
            repository.deleteInvoice(id)
            val saved = repository.getInvoices()
            _uiState.update { it.copy(savedInvoices = saved, error = null) }
        }
    }
}
