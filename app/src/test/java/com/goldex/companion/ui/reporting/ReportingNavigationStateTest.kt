package com.goldex.companion.ui.reporting

import com.goldex.companion.data.AppSettings
import com.goldex.companion.data.CustomerStore
import com.goldex.companion.data.InventoryStore
import com.goldex.companion.data.InvoiceStore
import com.goldex.companion.data.SettingsStore
import com.goldex.companion.domain.reporting.ReportingBreakdownType
import com.goldex.companion.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReportingNavigationStateTest {
    @Test
    fun openingAndLeavingReportKeepsGatewayNavigationConsistent() {
        val viewModel = ReportingViewModel(
            invoiceStore = object : InvoiceStore {
                override fun getInvoices(): List<Invoice> = emptyList()
                override fun saveInvoice(invoice: Invoice) = Unit
                override fun deleteInvoice(id: String) = Unit
            },
            customerStore = object : CustomerStore {
                override fun getCustomers(): List<Customer> = emptyList()
                override fun addCustomer(customer: Customer) = Unit
                override fun updateCustomer(customer: Customer) = Unit
                override fun deleteCustomer(id: String) = Unit
                override fun getTransactions(customerId: String): List<LedgerTransaction> = emptyList()
                override fun addTransaction(transaction: LedgerTransaction) = Unit
            },
            inventoryStore = object : InventoryStore {
                override fun getItems(): List<InventoryItem> = emptyList()
                override fun addItem(item: InventoryItem) = Unit
                override fun updateItem(item: InventoryItem) = Unit
                override fun deleteItem(id: String) = Unit
                override fun adjustStock(adjustment: StockAdjustment) = Unit
                override fun getAdjustments(): List<StockAdjustment> = emptyList()
            },
            settingsStore = object : SettingsStore {
                override val settings: StateFlow<AppSettings> = MutableStateFlow(AppSettings())
                override fun loadSettings(): AppSettings = settings.value
                override fun saveSettings(newSettings: AppSettings) = Unit
            }
        )

        viewModel.setReportingVisible(true)
        viewModel.openBreakdown(ReportingBreakdownType.VAT_REPORT)
        assertTrue(viewModel.uiState.value.isReportingVisible)
        assertEquals(ReportingBreakdownType.VAT_REPORT, viewModel.uiState.value.activeBreakdown)

        viewModel.closeBreakdown()
        assertTrue(viewModel.uiState.value.isReportingVisible)
        assertNull(viewModel.uiState.value.activeBreakdown)

        viewModel.openBreakdown(ReportingBreakdownType.GOLD_INVENTORY)
        viewModel.setReportingVisible(false)
        assertFalse(viewModel.uiState.value.isReportingVisible)
        assertNull(viewModel.uiState.value.activeBreakdown)
    }
}
