package com.goldex.companion.desktop.ui

import androidx.compose.material.icons.outlined.*
import com.goldex.companion.desktop.ui.controls.DesktopActionButton

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.invisibleToUser
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.goldex.companion.desktop.ui.controls.DesktopDialog as Dialog
import com.goldex.companion.desktop.ui.controls.DesktopDialogHost
import androidx.compose.ui.window.DialogProperties
import com.goldex.companion.desktop.customers.*
import com.goldex.companion.desktop.customers.modals.*
import com.goldex.companion.desktop.state.*
import com.goldex.companion.ui.components.*
import com.goldex.companion.ui.theme.LuxuryMotion
import com.goldex.companion.ui.theme.LocalGoldExColors
import kotlin.math.abs

@Composable internal fun CustomerHeaderActions(feature: DesktopCustomers) {
    val state by feature.manager.uiState.collectAsState()
    val customer = state.selectedCustomerForStatement
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (customer == null) WorkspaceControl("مشتری جدید", { feature.manager.setAddCustomerDialogVisible(true) },
            Modifier.testTag("customer-add"), icon = Icons.Outlined.Add, enabled = !feature.saving)
        else {
            WorkspaceControl("ثبت سند مستقل", { feature.manager.openAddLedgerEntry(customer) },
                Modifier.testTag("customer-independent-entry"), icon = Icons.Outlined.NoteAdd, secondary = true, enabled = !feature.saving)
            WorkspaceControl("ثبت دریافت / پرداخت", { feature.openPayment(customer) },
                Modifier.testTag("customer-payment"), icon = Icons.Outlined.Add, enabled = !feature.saving)
        }
    }
}

@OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
@Composable internal fun DesktopCustomersPage(workspace: DesktopWorkspace) {
    val feature = workspace.customers
    val manager = feature.manager
    val state by manager.uiState.collectAsState()
    val reduced = workspace.state.collectAsState().value.reduceMotion
    AnimatedContent(targetState = state, contentKey = { it.selectedCustomerForStatement?.id },
        modifier = Modifier.fillMaxSize().clipToBounds(), transitionSpec = {
            if (reduced) fadeIn(tween(0)).togetherWith(fadeOut(tween(0)))
            else LuxuryMotion.ScreenPushEnter.togetherWith(LuxuryMotion.ScreenPopExit)
        }, label = "customer-statement-navigation") { pageState ->
        val displayed = pageState.selectedCustomerForStatement
        val outgoing = displayed?.id != state.selectedCustomerForStatement?.id
        Box(Modifier.fillMaxSize().then(if (outgoing) Modifier.semantics { invisibleToUser() }.pointerInput(Unit) {
            awaitPointerEventScope { while (true) awaitPointerEvent().changes.forEach { it.consume() } }
        } else Modifier)) {
            if (displayed == null) CustomerLedgerScreen(
                uiState = pageState, onSearchQueryChange = manager::setSearchQuery, onFilterSelect = manager::setLedgerFilter,
                onOpenStatement = manager::openCustomerStatement, onOpenAddEntry = feature::openPayment,
                onEditCustomer = manager::editCustomer, onDeleteCustomer = manager::requestCustomerDeletion,
                onRetry = manager::loadCustomers, onNewInvoice = workspace::openCustomerInvoice)
            else CustomerStatementScreen(customer = displayed, transactions = pageState.filteredStatementTransactions,
                allTransactions = pageState.activeCustomerTransactions, selectedFilter = pageState.selectedStatementFilter,
                onFilterSelect = manager::setStatementFilter,
                onEditTransaction = { if (it.settlement == null) manager.openEditLedgerEntry(it) },
                onDeleteTransaction = { tx, done -> manager.deleteLedgerEntry(tx, done) },
                onRetry = { manager.openCustomerStatement(displayed) }, isLoading = pageState.isLoading,
                isSaving = pageState.isSaving, error = pageState.error, runningBalances = pageState.statementBalances)
        }
    }
}

/** Mounted outside transitions; navigation cannot abandon an unsaved form. */
@Composable internal fun DesktopCustomerDialogs(feature: DesktopCustomers, onSettlementComplete: () -> Unit = {}) {
    val manager = feature.manager
    val state by manager.uiState.collectAsState()
    val settlement by feature.settlement.state.collectAsState()
    DesktopDialogHost(state.takeIf { it.isCustomerManagerVisible && !it.isAddCustomerDialogVisible }) { displayed -> CustomerPickerDialog(
        customers = displayed.customerList, selectedCustomer = displayed.selectedCustomer,
        onSelectCustomer = { customer -> manager.selectCustomer(customer); manager.setCustomerManagerVisible(false); customer?.let(manager::openCustomerStatement) },
        onAddNewCustomerClick = { manager.setAddCustomerDialogVisible(true) },
        onDeleteCustomer = { id, done -> manager.deleteCustomer(id, done) }, onUpdateCustomer = { customer, done -> manager.updateCustomer(customer, done) },
        isSaving = state.isSaving, error = state.error,
        onDismiss = { if (!feature.saving) manager.setCustomerManagerVisible(false) }) }
    DesktopDialogHost(state.takeIf { it.isAddCustomerDialogVisible }) { displayed -> AddCustomerDialog(
        onDismiss = { if (!feature.saving) manager.setAddCustomerDialogVisible(false) },
        onSaveCustomer = { manager.addCustomer(it, autoSelect = false) }, isSaving = displayed.isSaving, error = displayed.error) }
    DesktopDialogHost(state.editingCustomer, contentKey = { it.id }) { customer ->
        key(customer.id) { EditCustomerDialog(customer,
            onDismiss = { if (!feature.saving) manager.closeCustomerEdit() },
            onSaveCustomer = { manager.updateCustomer(it) { manager.closeCustomerEdit() } },
            isSaving = state.isSaving, error = state.error) }
    }
    DesktopDialogHost(state.pendingDeleteCustomer, contentKey = { it.id }) { pending ->
        val customer = state.customerList.firstOrNull { it.id == pending.id } ?: pending
        val settled = abs(customer.goldDebtGrams) <= 1e-10 && customer.cashDebtTomans == 0L
        Dialog(onDismissRequest = { if (!feature.saving) manager.cancelCustomerDeletion() },
            properties = DialogProperties(usePlatformDefaultWidth = false)) {
            LuxuryCard(Modifier.widthIn(max = 560.dp).fillMaxWidth().testTag("customer-delete-confirmation")) {
                Text("حذف طرف‌حساب «${customer.name}»", fontSize = 16.sp, color = LocalGoldExColors.current.textMain)
                Text("حذف پس از تسویه کامل امکان‌پذیر است. اطلاعات مشتری بایگانی می‌شود و سوابق دفترکل حفظ می‌شود.",
                    fontSize = 13.sp, color = LocalGoldExColors.current.textSecondary)
                if (!settled) Text("پیش از حذف طرف‌حساب، مانده حساب را تسویه کنید",
                    color = LocalGoldExColors.current.errorRed, fontSize = 13.sp)
                RecordListStatus(state.isSaving, state.error, {})
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    DesktopActionButton("انصراف", manager::cancelCustomerDeletion,
                        Modifier.weight(1f).testTag("customer-delete-cancel"), isSecondary = true, icon = Icons.Outlined.Close, enabled = !feature.saving)
                    DesktopActionButton("حذف نهایی", { manager.deleteCustomer(customer.id) { manager.cancelCustomerDeletion() } },
                        Modifier.weight(1f).testTag("customer-delete-final"), icon = Icons.Outlined.DeleteOutline, enabled = !feature.saving && settled)
                }
            }
        }
    }
    DesktopDialogHost(state.takeIf { it.isAddLedgerEntryModalVisible && it.ledgerEntryTargetCustomer != null }, modal = true) { displayed ->
        AddLedgerEntryModal(customer = displayed.ledgerEntryTargetCustomer!!, editingTransaction = displayed.editingLedgerTransaction,
            onDismiss = { if (!feature.saving) manager.closeAddLedgerEntry() }, onSaveEntry = {
                if (!feature.isLicensed()) {
                    feature.onLicenseRequired("ثبت سند در دفتر معین نیازمند اشتراک معتبر است.")
                } else {
                    manager.saveLedgerEntry(it)
                }
            },
            isSaving = displayed.isSaving, error = displayed.error)
    }
    DesktopDialogHost(settlement.takeIf { it.customer != null }, modal = true) { displayed -> CustomerSettlementModal(displayed, feature.settlement,
        onConfirm = {
            if (!feature.isLicensed()) {
                feature.onLicenseRequired("ثبت تسویه نیازمند اشتراک معتبر است.")
            } else {
                feature.settlement.confirm { manager.refreshAfterSettlement(); onSettlementComplete() }
            }
        },
        onIndependentEntry = { val customer = displayed.customer; feature.settlement.close(); customer?.let(manager::openAddLedgerEntry) }) }
}
