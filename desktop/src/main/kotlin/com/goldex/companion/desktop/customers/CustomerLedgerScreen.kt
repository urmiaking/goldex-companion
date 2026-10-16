package com.goldex.companion.desktop.customers

import com.goldex.companion.desktop.ui.controls.DesktopIconButton as IconButton

import com.goldex.companion.desktop.ui.controls.*

import com.goldex.companion.desktop.ui.controls.DesktopFilterRow

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.goldex.companion.model.*
import com.goldex.companion.ui.components.*
import com.goldex.companion.ui.theme.*
import com.goldex.companion.desktop.ui.WorkspaceControl
import com.goldex.companion.desktop.ui.WorkspaceControlHeight
import com.goldex.companion.desktop.ui.WorkspaceControlShape

/** Desktop presentation; customer state and financial policies retain their existing owners. */
@Composable fun CustomerLedgerScreen(
    uiState: CustomerManagerUiState,
    onSearchQueryChange: (String) -> Unit,
    onFilterSelect: (CustomerLedgerFilterTab) -> Unit,
    onOpenStatement: (Customer) -> Unit,
    onOpenAddEntry: (Customer) -> Unit,
    onEditCustomer: (Customer) -> Unit,
    onDeleteCustomer: (Customer) -> Unit,
    onRetry: () -> Unit = {},
    modifier: Modifier = Modifier,
    onNewInvoice: (Customer) -> Unit = {}
) {
    val colors = LocalGoldExColors.current
    BoxWithConstraints(modifier.fillMaxSize().testTag("customer-ledger-page")) {
        val columns = when { maxWidth >= 1000.dp -> 3; maxWidth >= 660.dp -> 2; else -> 1 }
        LazyVerticalGrid(columns = GridCells.Fixed(columns), modifier = Modifier.fillMaxSize().testTag("customer-grid"),
            contentPadding = PaddingValues(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            item(key = "summary", span = { GridItemSpan(maxLineSpan) }) {
                CustomerAccountSummary("تراز کل دفتر مشتریان", "${PersianNumberFormatter.toPersianDigits(uiState.totalActiveCount.toString())} مشتری",
                    AccountSummaryMetric("طلب از مشتریان", uiState.totalCashReceivableTomans, uiState.totalGoldReceivableGrams),
                    AccountSummaryMetric("بدهی به مشتریان", uiState.totalCashPayableTomans, uiState.totalGoldPayableGrams),
                    Modifier.testTag("customer-summary"))
            }
            item(key = "tools", span = { GridItemSpan(maxLineSpan) }) {
                CustomerSearchAndFilters(uiState, onSearchQueryChange, onFilterSelect)
            }
            if (uiState.isLoading || uiState.error != null || uiState.notice != null) {
                item(key = "status", span = { GridItemSpan(maxLineSpan) }) {
                    Column {
                        RecordListStatus(uiState.isLoading, uiState.error, onRetry)
                        uiState.notice?.let { Text(it, color = colors.goldPrimary, fontSize = 13.sp) }
                    }
                }
            }
            if (uiState.filteredCustomers.isEmpty() && !uiState.isLoading && uiState.error == null) {
                item(key = "empty", span = { GridItemSpan(maxLineSpan) }) {
                    LuxuryCard {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Icon(Icons.Outlined.People, null, tint = colors.goldPrimary)
                            Text("طرف‌حسابی با این مشخصات یافت نشد", color = colors.textMuted, fontSize = 14.sp)
                        }
                    }
                }
            }
            items(uiState.filteredCustomers, key = { it.id }, contentType = { "customer" }) { customer ->
                CustomerDesktopCard(customer, uiState.isSaving,
                    { onOpenStatement(customer) }, { onOpenAddEntry(customer) },
                    { onEditCustomer(customer) }, { onDeleteCustomer(customer) }, { onNewInvoice(customer) })
            }
        }
    }
}

@Composable private fun CustomerSearchAndFilters(
    state: CustomerManagerUiState, onSearch: (String) -> Unit, onFilter: (CustomerLedgerFilterTab) -> Unit
) {
    val colors = LocalGoldExColors.current
    val search: @Composable (Modifier) -> Unit = { modifier ->
        DesktopSearchField(state.searchQuery, onSearch, "جستجوی نام، شماره یا کد حساب",
            modifier.testTag("customer-search-container"), inputTag = "customer-search")
    }
    val filters: @Composable (Modifier) -> Unit = { modifier ->
        DesktopFilterRow(CustomerLedgerFilterTab.values().toList(), state.selectedLedgerFilter, onFilter,
            label = { when (it) {
                CustomerLedgerFilterTab.ALL -> "همه"; CustomerLedgerFilterTab.DEBTORS -> "بدهکار"
                CustomerLedgerFilterTab.CREDITORS -> "بستانکار"; CustomerLedgerFilterTab.SETTLED -> "تسویه‌شده"
            } }, count = { when (it) {
                CustomerLedgerFilterTab.ALL -> state.totalActiveCount; CustomerLedgerFilterTab.DEBTORS -> state.debtorsCount
                CustomerLedgerFilterTab.CREDITORS -> state.creditorsCount; CustomerLedgerFilterTab.SETTLED -> state.settledCount
            } }, countColor = { when (it) {
                CustomerLedgerFilterTab.DEBTORS -> colors.errorRed; CustomerLedgerFilterTab.CREDITORS -> colors.marketGainText
                else -> colors.goldPrimary
            } }, modifier = modifier.testTag("customer-filters"), fillWidth = false)
    }

    BoxWithConstraints(Modifier.fillMaxWidth().testTag("customer-toolbar")) {
        val filterWidth = minOf(540.dp, maxWidth * .55f)
        if (maxWidth >= 780.dp) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            search(Modifier.weight(1f))
            filters(Modifier.widthIn(max = filterWidth))
        } else Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            search(Modifier.fillMaxWidth()); filters(Modifier.fillMaxWidth())
        }
    }
}

@Composable private fun CustomerDesktopCard(
    customer: Customer, saving: Boolean, onOpenStatement: () -> Unit, onOpenAddEntry: () -> Unit,
    onEdit: () -> Unit, onDelete: () -> Unit, onNewInvoice: () -> Unit
) {
    val colors = LocalGoldExColors.current
    LuxuryCard(Modifier.testTag("customer-card-${customer.id}"), hasTopHairline = true,
        contentPadding = PaddingValues(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Surface(shape = CircleShape, color = colors.goldContainer, modifier = Modifier.size(38.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Text(customer.name.firstOrNull()?.toString() ?: "م", color = colors.goldPrimary, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(customer.name, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = colors.textMain,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false).testTag("customer-name-${customer.id}"))
                    CustomerAccountStatus(customer.goldDebtGrams, customer.cashDebtTomans, Modifier.testTag("customer-status-${customer.id}"))
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(customerContactLabel(customer), fontSize = 12.sp, color = colors.textMuted,
                        maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Right,
                        style = TextStyle(textDirection = TextDirection.Ltr),
                        modifier = Modifier.weight(1f).testTag("customer-contact-${customer.id}"))
                    IconButton(onEdit, Modifier.size(44.dp).testTag("customer-edit-${customer.id}"), enabled = !saving) {
                        Icon(Icons.Outlined.Edit, "ویرایش مشتری", Modifier.size(18.dp), tint = colors.goldPrimary)
                    }
                    IconButton(onDelete, Modifier.size(44.dp).testTag("customer-delete-${customer.id}"), enabled = !saving) {
                        Icon(Icons.Outlined.DeleteOutline, "حذف مشتری", Modifier.size(18.dp), tint = colors.errorRed)
                    }
                }
            }
        }
        Surface(shape = MaterialTheme.shapes.medium, color = colors.surfaceElevated, border = colors.hairlineBorder) {
            Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically) {
                CustomerBalanceLine("مانده طلا", PersianNumberFormatter.formatAccountWeight(kotlin.math.abs(customer.goldDebtGrams)), "گرم",
                    customer.goldDebtGrams > 1e-10, customer.goldDebtGrams < -1e-10,
                    Modifier.weight(1f).testTag("customer-gold-${customer.id}"))
                Box(Modifier.width(1.dp).height(64.dp).background(colors.border))
                CustomerBalanceLine("مانده نقدی", PersianNumberFormatter.formatPrice(kotlin.math.abs(customer.cashDebtTomans)), "تومان",
                    customer.cashDebtTomans > 0, customer.cashDebtTomans < 0,
                    Modifier.weight(1f).testTag("customer-cash-${customer.id}"))
            }
        }
        DesktopActionButton("ثبت دریافت/پرداخت", onOpenAddEntry, Modifier.fillMaxWidth().testTag("customer-entry-${customer.id}"),
            enabled = !saving, icon = Icons.Outlined.NoteAdd)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DesktopActionButton("ریز گردش", onOpenStatement, Modifier.weight(1f).testTag("customer-statement-${customer.id}").semantics { contentDescription = "ریز گردش حساب" }, isSecondary = true, icon = Icons.Outlined.ReceiptLong)
            DesktopActionButton("ثبت فاکتور", onNewInvoice, Modifier.weight(1f).testTag("customer-invoice-${customer.id}"), enabled = !saving, isSecondary = true, icon = Icons.Outlined.ReceiptLong)
        }
    }
}

@Composable private fun CustomerBalanceLine(label: String, value: String, unit: String, debtor: Boolean, creditor: Boolean,
    modifier: Modifier = Modifier) {
    val colors = LocalGoldExColors.current
    Column(modifier, verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(label, color = colors.textMuted, fontSize = 12.sp, maxLines = 1)
        CustomerMetricNumber(value, 20.sp, colors.textMain, Modifier.fillMaxWidth().height(28.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(unit, color = colors.textMuted, fontSize = 12.sp, maxLines = 1)
            Text(when { debtor -> "بدهکار"; creditor -> "بستانکار"; else -> "تسویه" },
                color = when { debtor -> colors.errorRed; creditor -> colors.marketGainText; else -> colors.textMuted }, fontSize = 11.sp, maxLines = 1)
        }
    }
}
