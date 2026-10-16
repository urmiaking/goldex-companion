package com.goldex.companion.desktop.ui

import com.goldex.companion.desktop.ui.controls.*

import com.goldex.companion.desktop.ui.controls.DesktopIconButton as IconButton

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.goldex.companion.desktop.ui.controls.DesktopDialog as Dialog
import androidx.compose.ui.window.DialogProperties
import com.goldex.companion.desktop.state.*
import com.goldex.companion.model.*
import com.goldex.companion.ui.components.*
import com.goldex.companion.ui.invoices.*
import com.goldex.companion.ui.invoices.modals.AddInvoicePaymentModal
import com.goldex.companion.ui.theme.*

@Composable internal fun InvoiceHeaderActions(feature: DesktopInvoices) {
    val state by feature.manager.uiState.collectAsState()
    val host by feature.host.collectAsState()
    WorkspaceControl(if (state.subScreen == InvoicesSubScreen.EDITOR) "بایگانی فاکتورها" else "صدور فاکتور جدید",
        { if (state.subScreen == InvoicesSubScreen.EDITOR) feature.manager.navigateBackToList() else feature.newInvoice() },
        Modifier.testTag("invoices-new-btn"), icon = if (state.subScreen == InvoicesSubScreen.EDITOR) Icons.Outlined.ReceiptLong else Icons.Outlined.Add,
        secondary = state.subScreen == InvoicesSubScreen.EDITOR, enabled = !state.isSaving && !host.documentBusy && !feature.hasDialog)
}

@Composable internal fun DesktopInvoicesPage(state: WorkspaceState, workspace: DesktopWorkspace) {
    val feature = workspace.invoices
    val manager = feature.manager
    val invoiceState by manager.uiState.collectAsState()
    val customers by workspace.customers.manager.uiState.collectAsState()
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (invoiceState.statusMessage.isNotBlank()) Surface(color = LocalGoldExColors.current.goldContainer, shape = MaterialTheme.shapes.medium) {
            Row(Modifier.fillMaxWidth().padding(start = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(invoiceState.statusMessage, Modifier.weight(1f).testTag("invoice-message"), color = LocalGoldExColors.current.textMain, fontSize = 13.sp)
                IconButton(manager::clearMessage) { Icon(Icons.Outlined.Close, "بستن پیام فاکتور") }
            }
        }
        if (invoiceState.subScreen == InvoicesSubScreen.LIST) InvoiceDirectory(invoiceState, feature, Modifier.weight(1f))
        else key(invoiceState.invoice.id) {
            Box(Modifier.weight(1f).fillMaxWidth().testTag("invoice-editor"), contentAlignment = Alignment.TopCenter) {
                BarterInvoiceScreen(
                    uiState = invoiceState, marketRates = state.snapshot?.rates ?: com.goldex.companion.desktop.data.DesktopMarketRepository.emptyRates(),
                    customerList = customers.customerList,
                    onSetCustomerRole = manager::setCustomerRole, onSetDebtBasis = manager::setDebtBasis,
                    onOpenDatedSettlement = feature::settleEditor, onSetSettlementMethod = manager::setSettlementMethod,
                    onSetCashPosAmount = manager::setCashPosAmount, onSetLedgerAmount = manager::setLedgerAmount,
                    onSetPosTrackingCode = manager::setPosTrackingCode, onSetLedgerDueDate = manager::setLedgerDueDate,
                    onSetBullionSettlement = manager::setBullionSettlement, onSetThirdPartyTransfer = manager::setThirdPartyTransfer,
                    onSetSettlementPayments = manager::setSettlementPayments, onSetSyncWithLedger = manager::setSyncWithLedger,
                    onSetNote = manager::setNote, onOpenAddItemModal = manager::openAddItemModal,
                    onOpenEditItemModal = manager::openEditItemModal, onCloseItemModal = manager::closeItemModal,
                    onSaveItem = manager::saveItem, onDeleteSalesItem = manager::deleteSalesItem,
                    onDeleteReceivedItem = manager::deleteReceivedItem, onSetRateEditDialogVisible = manager::setRateEditDialogVisible,
                    onUpdateSpotPrice = manager::updateSpotPrice, onOpenCustomerPicker = feature::openPicker,
                    onOpenInvoiceManager = manager::navigateBackToList, onPreviewPdf = { feature.preview(invoiceState.invoice) },
                    onSendSms = { feature.copyMessage(invoiceState.invoice) }, onFinalSubmit = feature::save,
                    defaultProfitPercent = state.settings.defaultProfitPercent, defaultTaxPercent = state.settings.defaultTaxPercent,
                    modifier = Modifier.widthIn(max = 1040.dp).fillMaxSize(), onPaymentDialogChanged = feature::paymentDialog)
            }
        }
    }
}

@Composable private fun InvoiceDirectory(state: BarterInvoiceUiState, feature: DesktopInvoices, modifier: Modifier) {
    val colors = LocalGoldExColors.current
    BoxWithConstraints(modifier.fillMaxSize()) {
        val columns = when { maxWidth >= 1000.dp -> 3; maxWidth >= 660.dp -> 2; else -> 1 }
        LazyVerticalGrid(GridCells.Fixed(columns), modifier = Modifier.fillMaxSize().testTag("invoices-table"),
            contentPadding = PaddingValues(bottom = 12.dp), horizontalArrangement = Arrangement.spacedBy(14.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            item(key = "overview", span = { GridItemSpan(maxLineSpan) }) {
                BoxWithConstraints(Modifier.fillMaxWidth()) {
                    val cards: @Composable (Modifier) -> Unit = { cardModifier ->
                        InvoiceMetric("فاکتورهای ثبت‌شده", PersianNumberFormatter.toPersianDigits(state.invoicesList.size.toString()),
                            "${PersianNumberFormatter.toPersianDigits(state.summary.pending.toString())} فاکتور مانده‌دار", Icons.Outlined.ReceiptLong, cardModifier, "invoice-count")
                        InvoiceMetric("طلای فروخته‌شده", PersianNumberFormatter.formatWeight(state.summary.goldWeight), "گرم • معادل ۱۸ عیار", Icons.Outlined.Scale, cardModifier, "invoice-weight")
                        InvoiceMetric("مجموع فروش", PersianNumberFormatter.formatPrice(state.summary.turnoverTomans), "تومان • کل بایگانی", Icons.Outlined.Payments, cardModifier, "invoice-sales")
                    }
                    if (maxWidth >= 620.dp) Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) { cards(Modifier.weight(1f)) }
                    else Column(verticalArrangement = Arrangement.spacedBy(10.dp)) { cards(Modifier.fillMaxWidth()) }
                }
            }
            item(key = "tools", span = { GridItemSpan(maxLineSpan) }) { InvoiceTools(state, feature) }
            if (state.isLoading || state.error != null) item(key = "load", span = { GridItemSpan(maxLineSpan) }) {
                RecordListStatus(state.isLoading, state.error, feature.manager::reloadInvoices)
            }
            if (state.filteredInvoices.isEmpty() && !state.isLoading && state.error == null) item(key = "empty", span = { GridItemSpan(maxLineSpan) }) {
                LuxuryCard(Modifier.fillMaxWidth().testTag("invoice-empty")) {
                    Text(if (state.invoicesList.isEmpty()) "هنوز فاکتوری ثبت نشده است" else "فاکتوری مطابق جستجو یا فیلتر یافت نشد", color = colors.textMuted)
                    if (state.invoicesList.isNotEmpty()) DesktopActionButton("نمایش همه", { feature.manager.setSearchQuery(""); feature.manager.setSelectedFilter(InvoiceFilterTab.ALL) }, icon = Icons.Outlined.FilterAltOff, isSecondary = true)
                }
            }
            items(state.filteredInvoices, key = { it.id }, contentType = { "invoice" }) { item -> InvoiceCard(item, feature) }
        }
    }
}

@Composable private fun InvoiceMetric(title: String, value: String, subtitle: String, icon: ImageVector, modifier: Modifier, tag: String) {
    val colors = LocalGoldExColors.current
    LuxuryCard(modifier.testTag(tag), hasTopHairline = true, contentPadding = PaddingValues(14.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(icon, null, Modifier.size(20.dp), tint = colors.goldPrimary)
            Text(title, color = colors.textMuted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        AnimatedPriceTicker(value, Modifier.fillMaxWidth().height(36.dp), color = colors.textMain, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text(subtitle, color = colors.textMuted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable private fun InvoiceTools(state: BarterInvoiceUiState, feature: DesktopInvoices) {
    val colors = LocalGoldExColors.current
    val search: @Composable (Modifier) -> Unit = { modifier ->
        DesktopSearchField(state.searchQuery, feature.manager::setSearchQuery, "جستجوی مشتری، شماره یا کالا", modifier, inputTag = "invoices-search")
    }
    val filters: @Composable (Modifier) -> Unit = { modifier ->
        DesktopFilterRow(InvoiceFilterTab.values().toList(), state.selectedFilter, feature.manager::setSelectedFilter,
            label = { when (it) { InvoiceFilterTab.ALL -> "همه"; InvoiceFilterTab.SETTLED -> "تسویه"; InvoiceFilterTab.PENDING -> "مانده‌دار"; InvoiceFilterTab.WORKSHOP -> "کارگاه" } },
            count = { when (it) { InvoiceFilterTab.ALL -> state.invoicesList.size; InvoiceFilterTab.SETTLED -> state.summary.settled; InvoiceFilterTab.PENDING -> state.summary.pending; InvoiceFilterTab.WORKSHOP -> state.summary.workshop } },
            countColor = { if (it == InvoiceFilterTab.PENDING) colors.errorRed else colors.goldPrimary },
            modifier = modifier.testTag("invoice-filters"), fillWidth = false)
    }
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val filterWidth = minOf(540.dp, maxWidth * .6f)
        if (maxWidth >= 780.dp) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            search(Modifier.weight(1f)); filters(Modifier.widthIn(max = filterWidth))
        } else Column(verticalArrangement = Arrangement.spacedBy(10.dp)) { search(Modifier.fillMaxWidth()); filters(Modifier.fillMaxWidth()) }
    }
}

@Composable private fun InvoiceCard(item: InvoiceListItem, feature: DesktopInvoices) {
    val colors = LocalGoldExColors.current
    val ui by feature.manager.uiState.collectAsState()
    val saving = ui.isSaving
    LuxuryCard(Modifier.testTag("invoice-card-${item.id}"), hasTopHairline = true, contentPadding = PaddingValues(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Surface(shape = CircleShape, color = colors.goldContainer, modifier = Modifier.size(38.dp)) {
                Box(contentAlignment = Alignment.Center) { Icon(Icons.Outlined.ReceiptLong, null, tint = colors.goldPrimary) }
            }
            Column(Modifier.weight(1f)) {
                Text(item.customerName, color = colors.textMain, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("شماره ${PersianNumberFormatter.toPersianDigits(item.cleanInvoiceNumber)} • ${item.createdAtText}", color = colors.textMuted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            IconButton({ feature.requestDelete(item) }, Modifier.size(36.dp).testTag("invoice-delete-${item.id}"), enabled = !saving) {
                Icon(Icons.Outlined.DeleteOutline, "حذف فاکتور", tint = colors.errorRed, modifier = Modifier.size(18.dp))
            }
        }
        val items = item.barterInvoice?.let { it.salesItems + it.receivedItems }.orEmpty()
        val summary = items.firstOrNull()?.title.orEmpty() + if (items.size > 1) " (+${PersianNumberFormatter.toPersianDigits((items.size - 1).toString())} قلم دیگر)" else ""
        Text(summary.ifBlank { item.itemsSummary }, color = colors.textSecondary, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Surface(shape = MaterialTheme.shapes.medium, color = colors.surfaceElevated, border = colors.hairlineBorder) {
            Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("مبلغ فاکتور", color = colors.textMuted, fontSize = 12.sp)
                    Text("${PersianNumberFormatter.formatPrice(item.finalAmount)} تومان", color = colors.goldPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
                Text(item.line1Detail, color = colors.textMuted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(item.remainingDetail?.let { "مانده: $it" } ?: "تسویه کامل", Modifier.weight(1f), color = colors.textMain, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    val settled = item.status == InvoiceStatus.SETTLED
                    Surface(shape = WorkspaceControlShape, color = (if (settled) colors.marketGainText else colors.errorRed).copy(alpha = .08f)) {
                        Text(if (settled) "تسویه" else "مانده‌دار", Modifier.padding(horizontal = 10.dp, vertical = 4.dp), color = if (settled) colors.marketGainText else colors.errorRed, fontSize = 11.sp, maxLines = 1)
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            DesktopActionButton("جزئیات", { feature.manager.openInvoiceDetails(item) }, Modifier.weight(1f).testTag("invoice-open-${item.id}"), isSecondary = true, enabled = !saving, icon = Icons.Outlined.Visibility
)
            DesktopActionButton("PDF / چاپ", { item.barterInvoice?.let { feature.preview(it) } }, Modifier.weight(1f).testTag("invoice-preview-${item.id}"), isSecondary = true, enabled = !saving, icon = Icons.Outlined.Print
)
        }
        if (item.status != InvoiceStatus.SETTLED) DesktopActionButton("تسویه فاکتور", { feature.settle(item) },
            Modifier.fillMaxWidth().testTag("invoice-settle-${item.id}"), enabled = !saving, icon = Icons.Outlined.Payments)
    }
}

/** Mounted outside page transitions so financial confirmations retain their state. */
@Composable internal fun DesktopInvoiceDialogs(workspace: DesktopWorkspace) {
    val feature = workspace.invoices
    val host by feature.host.collectAsState()
    val ui by feature.manager.uiState.collectAsState()
    val customers by workspace.customers.manager.uiState.collectAsState()
    val state by workspace.state.collectAsState()
    DesktopDialogHost(host.takeIf { it.customerPicker }) { CustomerPickerDialog(customers = customers.customerList, selectedCustomer = ui.invoice.customer,
        onSelectCustomer = feature::selectCustomer, onAddNewCustomerClick = { workspace.customers.manager.setAddCustomerDialogVisible(true) },
        onDeleteCustomer = { id, done -> workspace.customers.manager.deleteCustomer(id, done) },
        onUpdateCustomer = { customer, done -> workspace.customers.manager.updateCustomer(customer, done) },
        isSaving = customers.isSaving, error = customers.error, onDismiss = feature::closePicker) }
    DesktopDialogHost(host.pendingDeletion, contentKey = { it.id }) { item ->
        Dialog({ feature.requestDelete(null) }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            LuxuryCard(Modifier.widthIn(max = 560.dp).fillMaxWidth().testTag("invoice-delete-confirmation")) {
                Text("حذف فاکتور ${PersianNumberFormatter.toPersianDigits(item.cleanInvoiceNumber)}", color = LocalGoldExColors.current.textMain, fontWeight = FontWeight.Bold)
                Text("فاکتور و اسناد مرتبط حذف و مانده طرف‌حساب اصلاح می‌شود. پرداخت بانکی واقعی برگشت داده نمی‌شود.", color = LocalGoldExColors.current.textSecondary)
                ui.statusMessage.takeIf { it.isNotBlank() }?.let { Text(it, color = LocalGoldExColors.current.errorRed) }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    DesktopActionButton("انصراف", { feature.requestDelete(null) }, Modifier.weight(1f), isSecondary = true, enabled = !ui.isLoading, icon = Icons.Outlined.Close
)
                    DesktopActionButton("حذف فاکتور", feature::delete, Modifier.weight(1f).testTag("invoice-delete-final"), enabled = !ui.isLoading, icon = Icons.Outlined.DeleteOutline
)
                }
            }
        }
    }
    DesktopDialogHost(ui.settlementShortcutInvoice, modal = true, contentKey = { it.id }) { invoice ->
        AddInvoicePaymentModal(invoice, invoice.balance, customers.customerList, ui.invoicesList,
            onDismiss = feature.manager::closeSettlementShortcut,
            onConfirm = { feature.manager.recordPaymentForInvoice(invoice.id, it, feature::refreshed) })
    }
    DesktopDialogHost(host.preview, contentKey = { it.id }) { invoice -> InvoiceDocumentPreview(invoice, state.settings, host.documentBusy,
        onDismiss = { feature.preview(null) }, onSave = { feature.export(invoice) }, onPrint = { feature.export(invoice, true) }) }
}
