package com.goldex.companion.desktop.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.invisibleToUser
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.goldex.companion.desktop.customers.*
import com.goldex.companion.desktop.customers.modals.*
import com.goldex.companion.desktop.state.*
import com.goldex.companion.ui.components.*
import com.goldex.companion.ui.theme.LuxuryMotion
import com.goldex.companion.ui.theme.LocalGoldExColors

@OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
@Composable internal fun DesktopCustomersPage(workspace: DesktopWorkspace) {
    val feature = workspace.customers
    val manager = feature.manager
    val state by manager.uiState.collectAsState()
    val reduced = workspace.state.collectAsState().value.reduceMotion
    val statement = state.selectedCustomerForStatement
    Column(Modifier.fillMaxSize()) {
        state.notice?.let { Text(it, color = LocalGoldExColors.current.goldPrimary) }
        Row(Modifier.fillMaxWidth().padding(bottom = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            WorkspaceControl("اطلاعات طرف‌حساب‌ها", { manager.setCustomerManagerVisible(true) },
                Modifier.testTag("customer-manage"), secondary = true, enabled = !feature.saving)
            if (statement != null) WorkspaceControl("ثبت سند مستقل", { manager.openAddLedgerEntry(statement) },
                Modifier.testTag("customer-independent-entry"), secondary = true, enabled = !feature.saving)
        }
        AnimatedContent(targetState = state, contentKey = { it.selectedCustomerForStatement?.id }, modifier = Modifier.weight(1f).clipToBounds(), transitionSpec = {
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
                onAddNewCustomer = { manager.setAddCustomerDialogVisible(true) },
                onBack = { workspace.navigate(DesktopDestination.DASHBOARD) }, onRetry = manager::loadCustomers)
            else CustomerStatementScreen(customer = displayed, transactions = pageState.filteredStatementTransactions,
                allTransactions = pageState.activeCustomerTransactions, selectedFilter = pageState.selectedStatementFilter,
                onFilterSelect = manager::setStatementFilter, onOpenAddEntry = { feature.openPayment(displayed) },
                onEditTransaction = { if (it.settlement == null) manager.openEditLedgerEntry(it) },
                onDeleteTransaction = { manager.deleteLedgerEntry(it) }, onBack = manager::closeCustomerStatement,
                onRetry = { manager.openCustomerStatement(displayed) }, isLoading = pageState.isLoading,
                error = pageState.error, runningBalances = pageState.statementBalances,
                onCall = { DesktopCustomerActions.call(it).onFailure { manager.showMessage(error = "برنامه تماس روی این رایانه در دسترس نیست") } },
                onShare = { DesktopCustomerActions.share(displayed, pageState.activeCustomerTransactions, pageState.statementBalances)
                    .onSuccess { manager.showMessage(notice = "گردش حساب برای اشتراک‌گذاری کپی شد") }
                    .onFailure { manager.showMessage(error = "کپی گردش حساب انجام نشد؛ دوباره تلاش کنید") } })
            }
        }
    }
}

/** Mounted outside page transitions, so switching the sidebar cannot abandon an unsaved form. */
@Composable internal fun DesktopCustomerDialogs(feature: DesktopCustomers) {
    val manager = feature.manager
    val state by manager.uiState.collectAsState()
    val settlement by feature.settlement.state.collectAsState()
    if (state.isCustomerManagerVisible && !state.isAddCustomerDialogVisible) CustomerPickerDialog(
        customers = state.customerList, selectedCustomer = state.selectedCustomer,
        onSelectCustomer = { customer -> manager.selectCustomer(customer); manager.setCustomerManagerVisible(false); customer?.let(manager::openCustomerStatement) },
        onAddNewCustomerClick = { manager.setAddCustomerDialogVisible(true) },
        onDeleteCustomer = { id, done -> manager.deleteCustomer(id, done) }, onUpdateCustomer = { customer, done -> manager.updateCustomer(customer, done) },
        isSaving = state.isSaving, error = state.error,
        onDismiss = { if (!feature.saving) manager.setCustomerManagerVisible(false) })
    if (state.isAddCustomerDialogVisible) AddCustomerDialog(
        onDismiss = { if (!feature.saving) manager.setAddCustomerDialogVisible(false) },
        onSaveCustomer = { manager.addCustomer(it, autoSelect = false) }, isSaving = state.isSaving, error = state.error)
    if (state.isAddLedgerEntryModalVisible) state.ledgerEntryTargetCustomer?.let { customer ->
        AddLedgerEntryModal(customer = customer, editingTransaction = state.editingLedgerTransaction,
            onDismiss = { if (!feature.saving) manager.closeAddLedgerEntry() }, onSaveEntry = { manager.saveLedgerEntry(it) },
            isSaving = state.isSaving, error = state.error)
    }
    if (settlement.customer != null) CustomerSettlementModal(settlement, feature.settlement,
        onConfirm = { feature.settlement.confirm { manager.refreshAfterSettlement() } },
        onIndependentEntry = { val customer = settlement.customer; feature.settlement.close(); customer?.let(manager::openAddLedgerEntry) })
}
