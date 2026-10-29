package com.goldex.companion.desktop.state

import com.goldex.companion.data.AppSettings
import com.goldex.companion.desktop.customers.DesktopCustomers
import com.goldex.companion.desktop.data.DesktopInvoiceDocuments
import com.goldex.companion.model.*
import com.goldex.companion.ui.invoices.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

data class InvoiceHostState(
    val customerPicker: Boolean = false,
    val pendingDeletion: InvoiceListItem? = null,
    val preview: BarterInvoice? = null,
    val documentBusy: Boolean = false,
    val paymentDialog: Boolean = false
)

/** One invoice owner, sharing the customer's transactional store and dated settlement form. */
class DesktopInvoices(
    val customers: DesktopCustomers,
    private val scope: CoroutineScope,
    private val settings: () -> AppSettings,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val isLicensed: () -> Boolean = { true },
    private val onLicenseRequired: (String) -> Unit = {},
    private val inventoryStore: com.goldex.companion.data.InventoryStore? = null,
    private val onInventoryUpdated: () -> Unit = {},
    private val quote: () -> Long
) {
    constructor(
        customers: DesktopCustomers,
        scope: CoroutineScope,
        settings: () -> AppSettings,
        dispatcher: CoroutineDispatcher = Dispatchers.IO,
        quote: () -> Long
    ) : this(
        customers = customers,
        scope = scope,
        settings = settings,
        dispatcher = dispatcher,
        isLicensed = { true },
        onLicenseRequired = {},
        quote = quote
    )
    val manager = BarterInvoiceViewModel(customers.invoices, customers.repository, customers.repository, scope, dispatcher, inventoryStore = inventoryStore, currentGold18 = quote)
    private val mutable = MutableStateFlow(InvoiceHostState())
    val host = mutable.asStateFlow()
    val saving get() = manager.uiState.value.isSaving || host.value.documentBusy
    val hasDraft get() = manager.uiState.value.subScreen == InvoicesSubScreen.EDITOR
    val hasDialog get() = with(host.value) { customerPicker || pendingDeletion != null || preview != null || paymentDialog } ||
        with(manager.uiState.value) { isItemModalVisible || isInventoryPickerVisible || isRateEditDialogVisible || settlementShortcutInvoice != null }

    fun newInvoice(customer: Customer? = null) {
        if (!isLicensed()) {
            onLicenseRequired("جهت صدور فاکتور نیاز به فعال‌سازی اشتراک دارید.")
            return
        }
        if (saving || hasDialog) return
        manager.openNewInvoice(quote())
        manager.setCustomer(customer)
        customers.manager.loadCustomers()
    }
    fun openPicker() { if (!saving) { customers.manager.loadCustomers(); mutable.update { it.copy(customerPicker = true) } } }
    fun closePicker() { if (!saving) mutable.update { it.copy(customerPicker = false) } }
    fun selectCustomer(customer: Customer?) { manager.setCustomer(customer); closePicker() }
    fun paymentDialog(visible: Boolean) { mutable.update { it.copy(paymentDialog = visible) } }
    fun requestDelete(item: InvoiceListItem?) { if (!saving) mutable.update { it.copy(pendingDeletion = item) } }
    fun delete() {
        val item = host.value.pendingDeletion ?: return
        manager.deleteInvoice(item.id) { deleted ->
            if (deleted) { mutable.update { it.copy(pendingDeletion = null) }; customers.manager.loadCustomers(); onInventoryUpdated() }
        }
    }
    fun save() {
        if (!isLicensed()) {
            onLicenseRequired("جهت صدور فاکتور نیاز به فعال‌سازی اشتراک دارید.")
            return
        }
        manager.submitAndSaveCurrentInvoice { customers.manager.loadCustomers(); onInventoryUpdated() }
    }
    fun settle(item: InvoiceListItem) { item.barterInvoice?.let(::settle) }
    fun settle(invoice: BarterInvoice) {
        if (saving) return
        if (!isLicensed()) {
            onLicenseRequired("ثبت تسویه نیازمند اشتراک معتبر است.")
            return
        }
        if (invoice.syncWithLedger && invoice.customer != null) customers.settlement.open(requireNotNull(invoice.customer), invoice.id)
        else manager.openSettlementShortcut(invoice)
    }
    fun settleEditor() { if (manager.canOpenDatedSettlement()) settle(manager.uiState.value.invoice) }
    fun refreshed() { manager.reloadInvoices(); customers.manager.refreshAfterSettlement() }
    fun preview(invoice: BarterInvoice?) { if (!saving) mutable.update { it.copy(preview = invoice) } }
    fun export(invoice: BarterInvoice, print: Boolean = false) {
        if (saving) return
        if (print && !isLicensed()) {
            onLicenseRequired("چاپ فاکتور نیازمند اشتراک معتبر است.")
            return
        }
        val profile = settings()
        mutable.update { it.copy(documentBusy = true) }
        scope.launch {
            try {
                val completed = withContext(Dispatchers.IO) {
                    if (print) DesktopInvoiceDocuments.print(invoice, profile) else DesktopInvoiceDocuments.saveWithDialog(invoice, profile)
                }
                if (completed) manager.reportMessage(if (print) "فاکتور به چاپگر فرستاده شد" else "فایل PDF فاکتور ذخیره شد")
            } catch (_: CancellationException) { throw CancellationException() }
            catch (_: Exception) { manager.reportMessage("خروجی فاکتور انجام نشد؛ مسیر ذخیره یا چاپگر را بررسی کنید") }
            finally { mutable.update { it.copy(documentBusy = false) } }
        }
    }
    fun copyMessage(invoice: BarterInvoice) {
        runCatching { DesktopInvoiceDocuments.copyMessage(invoice) }.fold(
            { manager.reportMessage("متن پیامک فاکتور کپی شد؛ می‌توانید آن را در برنامه پیام‌رسان ارسال کنید") },
            { manager.reportMessage("کپی متن پیامک انجام نشد") })
    }
    fun back() {
        if (saving) return
        when {
            host.value.preview != null -> preview(null)
            host.value.pendingDeletion != null -> requestDelete(null)
            host.value.customerPicker -> closePicker()
            manager.uiState.value.isItemModalVisible -> manager.closeItemModal()
            manager.uiState.value.isRateEditDialogVisible -> manager.setRateEditDialogVisible(false)
            else -> manager.navigateBackToList()
        }
    }
}
