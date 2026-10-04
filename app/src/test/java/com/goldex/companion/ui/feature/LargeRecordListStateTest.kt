package com.goldex.companion.ui.feature

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStore
import com.goldex.companion.data.*
import com.goldex.companion.model.*
import com.goldex.companion.ui.inventory.InventoryViewModel
import com.goldex.companion.ui.invoices.BarterInvoiceViewModel
import com.goldex.companion.ui.invoices.CustomerManagerViewModel
import com.goldex.companion.ui.reporting.ReportingViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

@OptIn(ExperimentalCoroutinesApi::class)
class LargeRecordListStateTest {
    private val executor = Executors.newSingleThreadExecutor { Thread(it, "list-worker") }
    private val worker = executor.asCoroutineDispatcher()
    private val models = ViewModelStore()

    @Before fun setup() { Dispatchers.setMain(Dispatchers.Unconfined) }
    @After fun cleanup() { models.clear(); worker.close(); Dispatchers.resetMain() }
    private fun <T : ViewModel> own(vm: T): T = vm.also { models.put(vm.javaClass.name, vm) }
    private fun onWorker() { assertTrue(Thread.currentThread().name.startsWith("list-worker")) }
    private fun <T> await(flow: StateFlow<T>, predicate: (T) -> Boolean): T =
        runBlocking { withTimeout(10_000) { flow.first(predicate) } }

    @Test fun twoThousandLinkedInvoicesUseBulkLedgerLookupOffTheUiThread() {
        val invoices = (0 until 2000).map { BarterInvoice(id = "invoice-$it", invoiceNumber = "invoice-$it", customer = Customer(id = "owner", name = "مشتری")) }
        val singleReads = AtomicInteger()
        val bulkReads = AtomicInteger()
        val customers = object : MemoryCustomers() {
            override fun getTransactionsByInvoiceId(invoiceId: String): List<LedgerTransaction> {
                singleReads.incrementAndGet()
                error("List projection must use the bulk port")
            }
            override fun getTransactionsByInvoiceIds(invoiceIds: List<String>): List<LedgerTransaction> {
                onWorker(); bulkReads.incrementAndGet()
                return invoiceIds.map { LedgerTransaction(id = "ledger-$it", customerId = "owner", invoiceId = it,
                    type = LedgerEntryType.CASH_RIAL, direction = LedgerDirection.PAY, amountTomans = 1000L) }
            }
        }
        val vm = own(BarterInvoiceViewModel(object : MemoryInvoices(invoices) {
            override fun getBarterInvoices(): List<BarterInvoice> { onWorker(); return super.getBarterInvoices() }
        }, customers, workDispatcher = worker))
        val loaded = await(vm.uiState) { !it.isLoading && it.invoicesList.size == 2000 }
        assertEquals(1, bulkReads.get())
        assertEquals(0, singleReads.get())
        assertTrue(loaded.invoicesList.all { it.status == InvoiceStatus.PARTIALLY_PAID })
        vm.setSearchQuery("invoice-1999")
        assertEquals("invoice-1999", vm.uiState.value.filteredInvoices.single().id)
        vm.openInvoiceDetails(loaded.invoicesList.last())
        assertEquals(1000L, vm.uiState.value.recordedOutstanding?.cashTomans)
        assertEquals(0, singleReads.get())
    }

    @Test fun twoThousandTransfersKeepApplicationOrderAndDoNotAccumulateOnReload() {
        val target = BarterInvoice(id = "target", syncWithLedger = false,
            salesItems = listOf(MeltGoldItem(weight = 1.0, spotPrice = 10_000L, equivalent18kWeight = 1.0, totalPayable = 10_000.0)))
        val transfers = (0 until 2000).map { BarterInvoice(id = "source-$it", syncWithLedger = false,
            settlementMethod = SettlementMethod.TRANSFER, thirdPartyInvoiceId = target.id,
            thirdPartyTransferAmount = 3L) }
        val vm = own(BarterInvoiceViewModel(MemoryInvoices(listOf(target) + transfers), workDispatcher = worker))
        val first = await(vm.uiState) { !it.isLoading && it.invoicesList.size == 2001 }
        assertEquals(4000L, first.invoicesList.first().finalAmount)
        vm.reloadInvoices()
        val second = await(vm.uiState) { !it.isLoading }
        assertEquals(first.invoicesList.first(), second.invoicesList.first())
    }

    @Test fun switchingCustomerWhileReadIsInFlightCannotShowThePreviousStatement() {
        val entered = CountDownLatch(1)
        val release = CountDownLatch(1)
        val a = Customer(id = "a", name = "A")
        val b = Customer(id = "b", name = "B")
        val store = object : MemoryCustomers(listOf(a, b)) {
            override fun getCustomers(): List<Customer> { onWorker(); return super.getCustomers() }
            override fun getTransactions(customerId: String): List<LedgerTransaction> {
                onWorker()
                if (customerId == "a") {
                    entered.countDown()
                    check(release.await(5, TimeUnit.SECONDS))
                    error("Previous statement read failed")
                }
                return (0 until 2000).map { LedgerTransaction(id = "$customerId-$it", customerId = customerId) }
            }
        }
        val vm = own(CustomerManagerViewModel(store, workDispatcher = worker))
        await(vm.uiState) { !it.isLoading }
        vm.openCustomerStatement(a)
        try {
            assertTrue(entered.await(5, TimeUnit.SECONDS))
            vm.openCustomerStatement(b)
            assertTrue(vm.uiState.value.activeCustomerTransactions.isEmpty())
        } finally { release.countDown() }
        val loaded = await(vm.uiState) { !it.isLoading && it.activeCustomerTransactions.size == 2000 }
        assertEquals("b", loaded.selectedCustomerForStatement?.id)
        assertTrue(loaded.activeCustomerTransactions.all { it.customerId == "b" })
        assertEquals(2000, loaded.statementBalances.size)
        assertNull(loaded.error)
    }

    @Test fun inventoryWritesRunOffMainRejectRepeatedSubmitAndKeepDataOnReadFailure() {
        val entered = CountDownLatch(1)
        val release = CountDownLatch(1)
        val writes = AtomicInteger()
        var failRead = false
        val items = (0 until 2000).map { InventoryItem(id = "item-$it", code = "code-$it", title = "کالا $it", quantity = 1) }
        val store = object : InventoryStore {
            override fun getItems(): List<InventoryItem> { onWorker(); check(!failRead); return items }
            override fun addItem(item: InventoryItem) { onWorker(); writes.incrementAndGet(); entered.countDown(); check(release.await(5, TimeUnit.SECONDS)) }
            override fun updateItem(item: InventoryItem) = Unit
            override fun deleteItem(id: String) = Unit
            override fun adjustStock(adjustment: StockAdjustment) = Unit
            override fun getAdjustments(): List<StockAdjustment> = emptyList()
        }
        val vm = own(InventoryViewModel(store, worker))
        await(vm.uiState) { !it.isLoading && it.items.size == 2000 }
        vm.addItem(items.first())
        try { assertTrue(entered.await(5, TimeUnit.SECONDS)); vm.addItem(items.first()) }
        finally { release.countDown() }
        await(vm.uiState) { !it.isLoading }
        assertEquals(1, writes.get())
        failRead = true
        vm.loadItems()
        val failed = await(vm.uiState) { !it.isLoading && it.error != null }
        assertEquals(items, failed.items)
        assertFalse(failed.isSaving)
        failRead = false
        vm.loadItems()
        assertNull(await(vm.uiState) { !it.isLoading }.error)
    }

    @Test fun failedCustomerSaveDoesNotSelectUnsavedCustomerOrSignalSuccess() {
        val callbacks = AtomicInteger()
        val vm = own(CustomerManagerViewModel(object : MemoryCustomers() {
            override fun addCustomer(customer: Customer) { onWorker(); error("Write failed") }
        }, workDispatcher = worker))
        await(vm.uiState) { !it.isLoading }
        vm.setAddCustomerDialogVisible(true)
        vm.addCustomer(Customer(id = "new", name = "مشتری")) { callbacks.incrementAndGet() }
        val failed = await(vm.uiState) { !it.isLoading && it.error != null }
        assertNull(failed.selectedCustomer)
        assertTrue(failed.isAddCustomerDialogVisible)
        assertEquals(0, callbacks.get())
    }

    @Test fun reportSnapshotsAreDeferredUntilOpeningAndLoadedOnWorker() {
        val reads = AtomicInteger()
        val invoices = object : MemoryInvoices(emptyList()) {
            override fun getBarterInvoices(): List<BarterInvoice> { onWorker(); reads.incrementAndGet(); return emptyList() }
        }
        val inventory = object : InventoryStore {
            override fun getItems(): List<InventoryItem> { onWorker(); return emptyList() }
            override fun getAdjustments(): List<StockAdjustment> { onWorker(); return emptyList() }
            override fun addItem(item: InventoryItem) = Unit
            override fun updateItem(item: InventoryItem) = Unit
            override fun deleteItem(id: String) = Unit
            override fun adjustStock(adjustment: StockAdjustment) = Unit
        }
        val settings = object : SettingsStore {
            override val settings: StateFlow<AppSettings> = MutableStateFlow(AppSettings())
            override fun loadSettings(): AppSettings { onWorker(); return settings.value }
            override fun saveSettings(newSettings: AppSettings) = Unit
        }
        val vm = own(ReportingViewModel(invoices, object : MemoryCustomers() {
            override fun getCustomers(): List<Customer> { onWorker(); return emptyList() }
        }, inventory, settings, worker))
        assertEquals(0, reads.get())
        vm.setReportingVisible(true)
        val loaded = await(vm.uiState) { !it.isLoading }
        assertEquals(1, reads.get())
        assertTrue(loaded.isReportingVisible)
        assertNull(loaded.error)
    }

    private open class MemoryCustomers(private val customers: List<Customer> = emptyList()) : CustomerStore {
        override fun getCustomers() = customers
        override fun addCustomer(customer: Customer) = Unit
        override fun updateCustomer(customer: Customer) = Unit
        override fun deleteCustomer(id: String) = Unit
        override fun getTransactions(customerId: String): List<LedgerTransaction> = emptyList()
        override fun addTransaction(transaction: LedgerTransaction) = Unit
    }
    private open class MemoryInvoices(private val invoices: List<BarterInvoice>) : InvoiceStore {
        override fun getInvoices(): List<Invoice> = emptyList()
        override fun saveInvoice(invoice: Invoice) = Unit
        override fun deleteInvoice(id: String) = Unit
        override fun getBarterInvoices() = invoices
    }
}
