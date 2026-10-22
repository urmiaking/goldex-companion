package com.goldex.companion.desktop.customers

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.goldex.companion.data.MarketRates
import com.goldex.companion.desktop.data.DesktopCustomerInvoiceStore
import com.goldex.companion.desktop.data.DesktopCustomerStore
import com.goldex.companion.desktop.data.DesktopDataStore
import com.goldex.companion.domain.customers.RecordCustomerSettlementUseCase
import com.goldex.companion.model.Customer
import kotlinx.coroutines.CoroutineScope
import kotlin.math.abs

// The original Android hub chevron has a fixed RTL orientation and stroke.
internal val HubArrowRight: ImageVector = ImageVector.Builder("HubArrowRight", 24.dp, 24.dp, 24f, 24f).apply {
    path(stroke = SolidColor(Color.White), strokeLineWidth = 2.2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
        moveTo(9f, 6f); lineTo(15f, 12f); lineTo(9f, 18f)
    }
}.build()

/** Windows composition root for the Android customer workflows, using the shared money policies. */
class DesktopCustomers(
    storage: DesktopDataStore,
    scope: CoroutineScope,
    val isLicensed: () -> Boolean = { true },
    val onLicenseRequired: (String) -> Unit = {},
    marketQuote: () -> MarketRates
) {
    constructor(storage: DesktopDataStore, scope: CoroutineScope, marketQuote: () -> MarketRates) :
        this(storage, scope, { true }, {}, marketQuote)
    val repository = DesktopCustomerStore(storage)
    val invoices = DesktopCustomerInvoiceStore(storage, repository)
    val manager = CustomerManagerViewModel(repository, repository, invoices, scope)
    val settlement = CustomerSettlementViewModel(repository, invoices,
        RecordCustomerSettlementUseCase(repository, invoices, repository), scope, marketQuote = marketQuote)
    val saving get() = manager.uiState.value.isSaving || settlement.state.value.isSaving
    val hasDialog get() = with(manager.uiState.value) {
        isAddCustomerDialogVisible || isCustomerManagerVisible || isAddLedgerEntryModalVisible || editingCustomer != null || pendingDeleteCustomer != null || settlement.state.value.customer != null
    }
    fun openPayment(customer: Customer) {
        if (!isLicensed()) {
            onLicenseRequired("ثبت پرداخت تسویه نیازمند اشتراک معتبر است.")
            return
        }
        if (abs(customer.goldDebtGrams) > 1e-10 || customer.cashDebtTomans != 0L) settlement.open(customer)
        else manager.openAddLedgerEntry(customer)
    }
    fun back() {
        if (saving) return
        when {
            settlement.state.value.customer != null -> settlement.close()
            manager.uiState.value.isAddLedgerEntryModalVisible -> manager.closeAddLedgerEntry()
            manager.uiState.value.isAddCustomerDialogVisible -> manager.setAddCustomerDialogVisible(false)
            manager.uiState.value.editingCustomer != null -> manager.closeCustomerEdit()
            manager.uiState.value.pendingDeleteCustomer != null -> manager.cancelCustomerDeletion()
            manager.uiState.value.isCustomerManagerVisible -> manager.setCustomerManagerVisible(false)
            else -> manager.closeCustomerStatement()
        }
    }
}
