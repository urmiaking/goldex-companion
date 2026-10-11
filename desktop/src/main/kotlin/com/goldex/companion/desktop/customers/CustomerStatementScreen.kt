package com.goldex.companion.desktop.customers

import kotlin.math.abs
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.goldex.companion.domain.customers.balanceEffect
import com.goldex.companion.model.*
import com.goldex.companion.ui.components.*
import com.goldex.companion.ui.theme.*
import com.goldex.companion.desktop.ui.WorkspaceControl
import com.goldex.companion.desktop.ui.WorkspaceControlHeight

/** Main workspace header owns identity, back navigation and registration actions. */
@Composable fun CustomerStatementScreen(
    customer: Customer,
    transactions: List<LedgerTransaction>,
    allTransactions: List<LedgerTransaction> = transactions,
    selectedFilter: StatementFilterTab,
    onFilterSelect: (StatementFilterTab) -> Unit,
    onEditTransaction: (LedgerTransaction) -> Unit = {},
    onDeleteTransaction: (LedgerTransaction, () -> Unit) -> Unit = { _, _ -> },
    onRetry: () -> Unit = {},
    isLoading: Boolean = false,
    isSaving: Boolean = false,
    error: String? = null,
    modifier: Modifier = Modifier,
    runningBalances: Map<String, Pair<Double, Long>> = emptyMap()
) {
    val colors = LocalGoldExColors.current
    var transactionToDelete by remember(customer.id) { mutableStateOf<LedgerTransaction?>(null) }
    LazyColumn(modifier.fillMaxSize().testTag("customer-statement-page"),
        verticalArrangement = Arrangement.spacedBy(14.dp), contentPadding = PaddingValues(bottom = 12.dp)) {
        item(key = "summary") {
            CustomerAccountSummary("تراز حساب مشتری", "${PersianNumberFormatter.toPersianDigits(allTransactions.size.toString())} سند",
                AccountSummaryMetric("مانده طلا", gold = abs(customer.goldDebtGrams),
                    caption = when { customer.goldDebtGrams > 1e-10 -> "بدهکار • عیار ۷۵۰"; customer.goldDebtGrams < -1e-10 -> "بستانکار • عیار ۷۵۰"; else -> "تسویه‌شده • عیار ۷۵۰" }),
                AccountSummaryMetric("مانده نقدی", cash = abs(customer.cashDebtTomans),
                    caption = when { customer.cashDebtTomans > 0 -> "بدهکار"; customer.cashDebtTomans < 0 -> "بستانکار"; else -> "تسویه‌شده" }),
                Modifier.testTag("customer-statement-summary"))
        }
        if (isLoading || error != null) item(key = "status") { RecordListStatus(isLoading, error, onRetry) }
        item(key = "filters") {
            LuxurySegmentedControl(StatementFilterTab.values().toList(), selectedFilter, onFilterSelect,
                label = { tab ->
                    val count = when (tab) {
                        StatementFilterTab.ALL -> allTransactions.size
                        StatementFilterTab.GOLD_SALE -> allTransactions.count { it.type == LedgerEntryType.GOLD_WEIGHT && it.direction == LedgerDirection.PAY }
                        StatementFilterTab.GOLD_RECEIPT -> allTransactions.count { it.type == LedgerEntryType.GOLD_WEIGHT && it.direction == LedgerDirection.RECEIVE }
                        StatementFilterTab.CASH_DEPOSIT -> allTransactions.count { it.type == LedgerEntryType.CASH_RIAL }
                        StatementFilterTab.SETTLEMENT -> allTransactions.count { it.title.contains("تسویه") || it.note.contains("تسویه") || it.tagBadge.contains("تهاتر") }
                    }
                    "${tab.titleFa} (${PersianNumberFormatter.toPersianDigits(count.toString())})"
                }, height = WorkspaceControlHeight, modifier = Modifier.fillMaxWidth().testTag("customer-statement-filters"), fontSize = 12.sp)
        }
        item(key = "heading") {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("ریز رویدادهای اسناد حسابداری", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = colors.textMain)
                Text("${PersianNumberFormatter.toPersianDigits(transactions.size.toString())} سند", fontSize = 12.sp, color = colors.goldPrimary)
            }
        }
        if (transactions.isEmpty() && !isLoading && error == null) item(key = "empty") {
            LuxuryCard { Text("سندی در این دسته‌بندی یافت نشد", fontSize = 14.sp, color = colors.textMuted) }
        }
        items(transactions, key = { "statement-list:${it.id}" }, contentType = { "transaction" }) { tx ->
            val balances = runningBalances[tx.id]
            StatementTransactionCard(tx, balances?.first ?: tx.resultingGoldBalance, balances?.second ?: tx.resultingCashBalance,
                onEditClick = { if (!isSaving) onEditTransaction(tx) }, onDeleteClick = { if (!isSaving) transactionToDelete = tx })
        }
    }
    transactionToDelete?.let { tx ->
        Dialog(onDismissRequest = { if (!isSaving) transactionToDelete = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)) {
            LuxuryCard(Modifier.widthIn(max = 560.dp).fillMaxWidth()) {
                Text("حذف سند در دفتر معین", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = colors.textMain)
                Text("آیا از حذف سند شماره ${PersianNumberFormatter.toPersianDigits(tx.cleanDocumentNumber)} («${tx.title}») اطمینان دارید؟ اثر مالی این سند از مانده حساب مشتری کسر/معکوس خواهد شد.",
                    fontSize = 13.sp, color = colors.textSecondary, lineHeight = 22.sp)
                RecordListStatus(isSaving, error, onRetry)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    WorkspaceControl("انصراف", { transactionToDelete = null }, Modifier.weight(1f), secondary = true, enabled = !isSaving)
                    WorkspaceControl("حذف قطعی", { onDeleteTransaction(tx) { transactionToDelete = null } }, Modifier.weight(1f), enabled = !isSaving)
                }
            }
        }
    }
}

@Composable
private fun MethodChip(
    text: String,
    isGold: Boolean,
    modifier: Modifier = Modifier
) {
    val colors = LocalGoldExColors.current
    val (chipBg, chipText, chipBorder) = when {
        isGold -> when {
            text.contains("آبشده") -> Triple(Color(0x26F59E0B), Color(0xFFF59E0B), Color(0x4DF59E0B))
            text.contains("مصنوعات") -> Triple(Color(0x26EC4899), Color(0xFFF472B6), Color(0x4DEC4899))
            text.contains("سکه") || text.contains("شمش") -> Triple(Color(0x2606B6D4), Color(0xFF22D3EE), Color(0x4D06B6D4))
            else -> Triple(colors.goldContainer.copy(alpha = 0.5f), colors.goldPrimary, colors.goldBorder)
        }
        else -> when {
            text.contains("حواله") || text.contains("پایا") -> Triple(Color(0x263B82F6), Color(0xFF60A5FA), Color(0x4D3B82F6))
            text.contains("چک") -> Triple(Color(0x268B5CF6), Color(0xFFA78BFA), Color(0x4D8B5CF6))
            text.contains("کارتخوان") || text.contains("POS") -> Triple(Color(0x266366F1), Color(0xFF818CF8), Color(0x4D6366F1))
            text.contains("نقد") || text.contains("اسکناس") -> Triple(Color(0x2610B981), Color(0xFF34D399), Color(0x4D10B981))
            else -> Triple(Color(0x2664748B), Color(0xFF94A3B8), Color(0x4D64748B))
        }
    }

    Surface(
        shape = RoundedCornerShape(6.dp),
        color = chipBg,
        border = BorderStroke(0.6.dp, chipBorder),
        modifier = modifier
    ) {
        Text(
            text = text,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = chipText,
            fontFamily = VazirmatnFamily,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
private fun StatementTransactionCard(
    transaction: LedgerTransaction,
    computedGoldBalance: Double = transaction.resultingGoldBalance,
    computedCashBalance: Long = transaction.resultingCashBalance,
    onEditClick: () -> Unit = {},
    onDeleteClick: () -> Unit = {}
) {
    val colors = LocalGoldExColors.current
    val isGold = transaction.type == LedgerEntryType.GOLD_WEIGHT
    val isReceive = transaction.direction == LedgerDirection.RECEIVE

    LuxuryCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(0.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Row 1: Header - Icon + Title & Doc/Date + Direction Badge + Edit/Delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(7.dp))
                            .background(
                                if (isGold) colors.goldContainer.copy(alpha = 0.6f) else colors.surfaceElevated
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isGold) LedgerScaleVector else LedgerAccountBalanceVector,
                            contentDescription = null,
                            tint = colors.goldPrimary,
                            modifier = Modifier.size(15.dp)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = transaction.title,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textMain,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                fontFamily = VazirmatnFamily,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            val badgeText = when {
                                transaction.isAutoGenerated || transaction.invoiceId != null -> {
                                    if (transaction.title.contains("فاکتور") || transaction.title.contains("خرید")) null
                                    else "تسویه فاکتور"
                                }
                                transaction.tagBadge.isNotBlank() -> transaction.tagBadge
                                else -> null
                            }

                            if (badgeText != null) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = colors.goldContainer.copy(alpha = 0.75f)
                                ) {
                                    Text(
                                        text = badgeText,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = colors.goldPrimary,
                                        fontFamily = VazirmatnFamily,
                                        maxLines = 1,
                                        softWrap = false,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }
                        val cleanDoc = transaction.cleanDocumentNumber
                        val cleanInv = transaction.cleanInvoiceNumber
                        val headerSubtitle = if (!cleanInv.isNullOrBlank()) {
                            "سند شماره ${PersianNumberFormatter.toPersianDigits(cleanDoc)} • فاکتور شماره ${PersianNumberFormatter.toPersianDigits(cleanInv)} • ${transaction.dateTime}"
                        } else {
                            "سند شماره ${PersianNumberFormatter.toPersianDigits(cleanDoc)} • ${transaction.dateTime}"
                        }
                        Text(
                            text = headerSubtitle,
                            fontSize = 12.sp,
                            color = colors.textMuted,
                            fontFamily = VazirmatnFamily
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Direction badge
                    val dirColor = if (isReceive) colors.profitGreen else colors.errorRed
                    val dirText = if (isReceive) "دریافت" else "پرداخت"
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = dirColor.copy(alpha = 0.12f),
                        border = BorderStroke(0.5.dp, dirColor.copy(alpha = 0.3f))
                    ) {
                        Text(
                            text = dirText,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = dirColor,
                            fontFamily = VazirmatnFamily,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }

                    if (transaction.settlement == null) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(colors.surfaceElevated)
                                .clickable(enabled = transaction.settlement == null, onClick = onEditClick),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "ویرایش سند",
                                tint = colors.goldPrimary,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(colors.surfaceElevated)
                            .clickable(onClick = onDeleteClick),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "حذف سند",
                            tint = colors.errorRed,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }

            // Row 2: Movement Strip - Method Chip + Details & Prominent Amount
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = colors.surfaceElevated,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.weight(1f, fill = false)
                        ) {
                            val methodLabel = if (isGold) {
                                if (transaction.goldCategory.isNotBlank()) transaction.goldCategory else "طلا ۷۵۰"
                            } else {
                                if (transaction.paymentMethod.isNotBlank()) transaction.paymentMethod else "نقدی"
                            }
                            MethodChip(text = methodLabel, isGold = isGold)

                            val stl = transaction.settlement
                            val detailsText = if (isGold && transaction.scaleWeightGrams > 0.0) {
                                buildString {
                                    append("ترازو: ${PersianNumberFormatter.formatWeight(transaction.scaleWeightGrams)}")
                                    if (transaction.karat != 750) {
                                        append(" (ع ${PersianNumberFormatter.toPersianDigits(transaction.karat.toString())})")
                                    }
                                    if (transaction.angNumber.isNotBlank()) {
                                        append(" • اَنگ: ${PersianNumberFormatter.toPersianDigits(transaction.angNumber)}")
                                    }
                                }
                            } else if (!isGold && (transaction.destinationBank.isNotBlank() || transaction.trackingCode.isNotBlank())) {
                                buildString {
                                    if (transaction.destinationBank.isNotBlank()) {
                                        append(transaction.destinationBank)
                                    }
                                    if (transaction.trackingCode.isNotBlank()) {
                                        if (isNotEmpty()) append(" • ")
                                        append("پیگیری: ${PersianNumberFormatter.toPersianDigits(transaction.trackingCode)}")
                                    }
                                }
                            } else if (stl != null && stl.rateTomans > 0L) {
                                "نرخ ۱۸ عیار: ${PersianNumberFormatter.formatPrice(stl.rateTomans)} ت"
                            } else ""

                            if (detailsText.isNotBlank()) {
                                Text(
                                    text = detailsText,
                                    fontSize = 12.sp,
                                    color = colors.textSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    fontFamily = VazirmatnFamily
                                )
                            }
                        }

                        // Prominent eye-catching amount (NO negative sign, direction indicated by color & badge)
                        Text(
                            text = if (isGold) {
                                "${PersianNumberFormatter.formatWeight(transaction.equivalent750WeightGrams)} گرم"
                            } else {
                                "${PersianNumberFormatter.formatPrice(transaction.amountTomans)} تومان"
                            },
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            color = if (isReceive) colors.profitGreen else colors.errorRed,
                            fontFamily = VazirmatnFamily
                        )
                    }

                    // Note: Render ONLY when note is not blank
                    if (transaction.note.isNotBlank()) {
                        Text(
                            text = "یادداشت: ${transaction.note}",
                            fontSize = 12.sp,
                            color = colors.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontFamily = VazirmatnFamily
                        )
                    }
                }
            }

            // Row 3: Resulting Balance with Clear Interpretation (مانده معین با تفکیک بدهکار/بستانکار و بدون منفی)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "مانده معین پس از ثبت:",
                    fontSize = 12.sp,
                    color = colors.textMuted,
                    fontFamily = VazirmatnFamily
                )

                val hasGold = abs(computedGoldBalance) > 1e-4
                val hasCash = abs(computedCashBalance) > 0L

                if (hasGold && hasCash) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Gold Balance Pill
                        val goldColor = if (computedGoldBalance > 1e-4) colors.errorRed else colors.profitGreen
                        val goldStatus = if (computedGoldBalance > 1e-4) "بدهکار" else "بستانکار"
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Text(
                                text = "${PersianNumberFormatter.formatAccountWeight(abs(computedGoldBalance))} گرم",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = goldColor,
                                fontFamily = VazirmatnFamily
                            )
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = goldColor.copy(alpha = 0.12f),
                                border = BorderStroke(0.5.dp, goldColor.copy(alpha = 0.3f))
                            ) {
                                Text(
                                    text = goldStatus,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = goldColor,
                                    fontFamily = VazirmatnFamily,
                                    modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp)
                                )
                            }
                        }

                        Text(
                            text = "•",
                            fontSize = 12.sp,
                            color = colors.textMuted
                        )

                        // Cash Balance Pill
                        val cashColor = if (computedCashBalance > 0L) colors.errorRed else colors.profitGreen
                        val cashStatus = if (computedCashBalance > 0L) "بدهکار" else "بستانکار"
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Text(
                                text = "${PersianNumberFormatter.formatPrice(abs(computedCashBalance))} ت",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = cashColor,
                                fontFamily = VazirmatnFamily
                            )
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = cashColor.copy(alpha = 0.12f),
                                border = BorderStroke(0.5.dp, cashColor.copy(alpha = 0.3f))
                            ) {
                                Text(
                                    text = cashStatus,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = cashColor,
                                    fontFamily = VazirmatnFamily,
                                    modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }
                } else if (hasGold) {
                    val bal = computedGoldBalance
                    val statusText = if (bal > 1e-4) "بدهکار" else "بستانکار"
                    val statusColor = if (bal > 1e-4) colors.errorRed else colors.profitGreen
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "${PersianNumberFormatter.formatAccountWeight(abs(bal))} گرم",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = statusColor,
                            fontFamily = VazirmatnFamily
                        )
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = statusColor.copy(alpha = 0.12f),
                            border = BorderStroke(0.5.dp, statusColor.copy(alpha = 0.3f))
                        ) {
                            Text(
                                text = statusText,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = statusColor,
                                fontFamily = VazirmatnFamily,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                } else if (hasCash) {
                    val bal = computedCashBalance
                    val statusText = if (bal > 0L) "بدهکار" else "بستانکار"
                    val statusColor = if (bal > 0L) colors.errorRed else colors.profitGreen
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "${PersianNumberFormatter.formatPrice(abs(bal))} تومان",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = statusColor,
                            fontFamily = VazirmatnFamily
                        )
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = statusColor.copy(alpha = 0.12f),
                            border = BorderStroke(0.5.dp, statusColor.copy(alpha = 0.3f))
                        ) {
                            Text(
                                text = statusText,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = statusColor,
                                fontFamily = VazirmatnFamily,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = colors.profitGreen.copy(alpha = 0.12f),
                        border = BorderStroke(0.5.dp, colors.profitGreen.copy(alpha = 0.3f))
                    ) {
                        Text(
                            text = "تسویه‌شده (تراز صفر)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.profitGreen,
                            fontFamily = VazirmatnFamily,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Accurately calculates the chronological running balances (gold in grams, cash in Tomans)
 * for every transaction in a customer's statement, based on the customer's authoritative balances.
 * Tracing backwards from the current balance ensures historical integrity and eliminates 0-balance display.
 */
fun calculateRunningBalances(customer: Customer, allTransactions: List<LedgerTransaction>): Map<String, Pair<Double, Long>> =
    com.goldex.companion.domain.customers.customerStatementBalances(customer, allTransactions)
