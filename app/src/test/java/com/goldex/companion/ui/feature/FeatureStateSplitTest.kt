package com.goldex.companion.ui.feature

import com.goldex.companion.data.AppSettings
import com.goldex.companion.data.CustomerStore
import com.goldex.companion.data.InvoiceStore
import com.goldex.companion.data.MarketRates
import com.goldex.companion.data.PortfolioCategory
import com.goldex.companion.data.PortfolioItem
import com.goldex.companion.data.PortfolioStore
import com.goldex.companion.data.PriceSource
import com.goldex.companion.data.SettingsStore
import com.goldex.companion.model.Customer
import com.goldex.companion.model.Invoice
import com.goldex.companion.model.Karat
import com.goldex.companion.model.LedgerTransaction
import com.goldex.companion.model.WageType
import com.goldex.companion.ui.invoices.CustomerManagerViewModel
import com.goldex.companion.ui.invoices.InvoiceManagerViewModel
import com.goldex.companion.ui.main.MainUiState
import com.goldex.companion.ui.portfolio.PortfolioManagerViewModel
import com.goldex.companion.ui.settings.SettingsViewModel
import com.goldex.companion.ui.update.UpdateViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FeatureStateSplitTest {

    @Test
    fun customerManagerKeepsSelectionInSyncWhenDeletingSelectedCustomer() {
        val fakeStore = object : CustomerStore {
            private val list = mutableListOf<Customer>()
            private val txList = mutableListOf<LedgerTransaction>()
            override fun getCustomers(): List<Customer> = list.toList()
            override fun addCustomer(customer: Customer) { list.add(0, customer) }
            override fun updateCustomer(customer: Customer) {
                val index = list.indexOfFirst { it.id == customer.id }
                if (index >= 0) list[index] = customer
            }
            override fun deleteCustomer(id: String) { list.removeIf { it.id == id } }
            override fun getTransactions(customerId: String): List<LedgerTransaction> = txList.filter { it.customerId == customerId }
            override fun addTransaction(transaction: LedgerTransaction) { txList.add(0, transaction) }
            override fun updateTransaction(transaction: LedgerTransaction) {
                val index = txList.indexOfFirst { it.id == transaction.id }
                if (index >= 0) txList[index] = transaction
            }
            override fun deleteTransaction(id: String) { txList.removeIf { it.id == id } }
        }

        val viewModel = CustomerManagerViewModel(fakeStore)
        val customer = Customer(id = "c-1", name = "رضا")

        viewModel.setCustomers(listOf(customer))
        viewModel.selectCustomer(customer)
        viewModel.deleteCustomer(customer.id)

        assertEquals(emptyList<Customer>(), viewModel.uiState.value.customerList)
        assertNull(viewModel.uiState.value.selectedCustomer)
    }

    @Test
    fun invoiceManagerSavesAndDeletesInvoicesConsistently() {
        val fakeStore = object : InvoiceStore {
            private val list = mutableListOf<Invoice>()
            override fun getInvoices(): List<Invoice> = list.toList()
            override fun saveInvoice(invoice: Invoice) {
                list.removeIf { it.id == invoice.id }
                list.add(0, invoice)
            }
            override fun deleteInvoice(id: String) { list.removeIf { it.id == id } }
        }

        val viewModel = InvoiceManagerViewModel(fakeStore)
        val invoice = Invoice(id = "inv-1", invoiceNumber = "101")

        viewModel.saveInvoice(invoice)
        assertEquals(1, viewModel.uiState.value.savedInvoices.size)
        assertEquals("101", viewModel.uiState.value.savedInvoices.first().invoiceNumber)

        viewModel.deleteInvoice("inv-1")
        assertTrue(viewModel.uiState.value.savedInvoices.isEmpty())
    }

    @Test
    fun portfolioManagerPerformsCrudAgainstPortfolioStore() {
        val fakeStore = object : PortfolioStore {
            private val list = mutableListOf<PortfolioItem>()
            override fun getItems(): List<PortfolioItem> = list.toList()
            override fun addItem(item: PortfolioItem) { list.add(0, item) }
            override fun deleteItem(id: String) { list.removeIf { it.id == id } }
        }

        val viewModel = PortfolioManagerViewModel(fakeStore)
        val item = PortfolioItem(id = "p-1", title = "النگو", category = PortfolioCategory.GOLD, weightGrams = 12.5, karat = Karat.K18)

        viewModel.addPortfolioItem(item)
        assertEquals(1, viewModel.uiState.value.portfolioItems.size)

        viewModel.deletePortfolioItem("p-1")
        assertTrue(viewModel.uiState.value.portfolioItems.isEmpty())
    }

    @Test
    fun settingsManagerUpdatesTaxProfitAndBiometricLock() {
        var persisted = AppSettings()
        val flow = MutableStateFlow(persisted)
        val fakeStore = object : SettingsStore {
            override val settings: StateFlow<AppSettings> = flow
            override fun loadSettings(): AppSettings = persisted
            override fun saveSettings(newSettings: AppSettings) {
                persisted = newSettings
                flow.value = newSettings
            }
        }

        val viewModel = SettingsViewModel(fakeStore)
        viewModel.updateTaxAndProfit("8.5", "10", WageType.TOMAN_PER_GRAM)

        assertEquals("8.5", viewModel.uiState.value.appSettings.defaultProfitPercent)
        assertEquals("10", viewModel.uiState.value.appSettings.defaultTaxPercent)
        assertEquals(WageType.TOMAN_PER_GRAM, viewModel.uiState.value.appSettings.defaultWageType)

        viewModel.toggleBiometricLock(true)
        assertTrue(viewModel.uiState.value.appSettings.isBiometricLockEnabled)
    }

    @Test
    fun updateViewModelTracksDialogDismissalState() {
        val viewModel = UpdateViewModel()
        assertFalse(viewModel.uiState.value.isUpdateDialogDismissed)

        viewModel.dismissUpdateDialog()
        assertTrue(viewModel.uiState.value.isUpdateDialogDismissed)

        viewModel.resetUpdateDialog()
        assertFalse(viewModel.uiState.value.isUpdateDialogDismissed)
    }

    @Test
    fun mainUiStateProjectsToScopedSubscreenStates() {
        val rates = MarketRates(gold18 = 24_000_000L, goldMelt = 104_000_000L)
        val state = MainUiState(
            grossWeightInput = "15.5",
            meltWeightInput = "25.0",
            mesghalPriceInput = "104000000",
            rates = rates
        )

        val jewelryState = state.toJewelryUiState()
        assertEquals("15.5", jewelryState.grossWeightInput)
        assertEquals(24_000_000L, jewelryState.rates.gold18)

        val meltState = state.toMeltUiState()
        assertEquals("25.0", meltState.meltWeightInput)
        assertEquals("104000000", meltState.mesghalPriceInput)
        assertEquals(104_000_000L, meltState.rates.goldMelt)
    }
}
