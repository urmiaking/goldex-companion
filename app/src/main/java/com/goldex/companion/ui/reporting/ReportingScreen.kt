package com.goldex.companion.ui.reporting

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.goldex.companion.domain.reporting.*
import com.goldex.companion.model.PersianNumberFormatter
import com.goldex.companion.ui.components.GoldButton
import com.goldex.companion.ui.components.LuxuryCard
import com.goldex.companion.ui.theme.LocalGoldExColors

@Composable
fun ReportingScreen(
    uiState: ReportingUiState,
    onBack: () -> Unit,
    onSelectPeriod: (ReportingPeriod) -> Unit,
    onOpenBreakdown: (ReportingBreakdownType) -> Unit,
    onCloseBreakdown: () -> Unit,
    onNavigateInventory: () -> Unit,
    onNavigateCustomerLedger: () -> Unit,
    onOpenCustomDateDialog: () -> Unit,
    onCloseCustomDateDialog: () -> Unit,
    onSubmitCustomRange: (startMs: Long, endMs: Long, startShamsi: String, endShamsi: String) -> Unit
) {
    val colors = LocalGoldExColors.current
    val scrollState = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ==========================================
            // 1. Top Header & Period Control
            // ==========================================
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            modifier = Modifier
                                .size(40.dp)
                                .clickable { onBack() },
                            shape = CircleShape,
                            color = colors.surfaceElevated,
                            border = BorderStroke(0.6.dp, colors.goldBorder),
                            shadowElevation = if (colors.isDark) 0.dp else 1.dp
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = ReportingArrowForward,
                                    contentDescription = "بازگشت",
                                    tint = colors.textMain,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Column {
                            Text(
                                text = "مرکز گزارشات و ترازنامه‌ها",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                color = colors.textMain
                            )
                            Text(
                                text = "تحلیل عملکرد مالی و ترازنامه‌های زرگری",
                                fontSize = 11.sp,
                                color = colors.textMuted
                            )
                        }
                    }
                }

                // Period Filter Chips Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ReportingPeriod.entries.forEach { period ->
                        val isSelected = uiState.selectedPeriod == period
                        val chipBg = if (isSelected) {
                            if (colors.isDark) colors.goldPrimary else Color(0xFF1E2333)
                        } else {
                            colors.surfaceElevated
                        }
                        val chipTextColor = if (isSelected) {
                            if (colors.isDark) Color(0xFF141B2B) else Color.White
                        } else {
                            colors.textSecondary
                        }

                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = chipBg,
                            border = BorderStroke(
                                0.6.dp,
                                if (isSelected) colors.goldPrimary else colors.border.copy(alpha = 0.5f)
                            ),
                            modifier = Modifier.clickable {
                                if (period == ReportingPeriod.CUSTOM) {
                                    onOpenCustomDateDialog()
                                } else {
                                    onSelectPeriod(period)
                                }
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                if (period == ReportingPeriod.CUSTOM) {
                                    Icon(
                                        imageVector = ReportingCalendar,
                                        contentDescription = null,
                                        tint = chipTextColor,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                                Text(
                                    text = if (period == ReportingPeriod.CUSTOM && uiState.customStartDateShamsi.isNotBlank()) {
                                        "${PersianNumberFormatter.toPersianDigits(uiState.customStartDateShamsi)} تا ${PersianNumberFormatter.toPersianDigits(uiState.customEndDateShamsi)}"
                                    } else {
                                        period.labelFa
                                    },
                                    fontSize = 11.5.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = chipTextColor
                                )
                            }
                        }
                    }
                }
            }

            // ==========================================
            // 2. Hero KPI Vault Card
            // ==========================================
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFF141B2B),
                border = BorderStroke(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        listOf(
                            Color(0xFFD4AF37).copy(alpha = 0.5f),
                            Color(0xFFB8860B).copy(alpha = 0.15f)
                        )
                    )
                ),
                shadowElevation = 10.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    // Golden top specular hairline
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.dp)
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        Color.Transparent,
                                        Color(0xFFD4AF37),
                                        Color.Transparent
                                    )
                                )
                            )
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Vault Card Header
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFFD4AF37).copy(alpha = 0.18f))
                                    .border(0.8.dp, Color(0xFFD4AF37).copy(alpha = 0.35f), RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = ReportingAccountBalance,
                                    contentDescription = null,
                                    tint = Color(0xFFE9C349),
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Column {
                                Text(
                                    text = "تراز کل ${uiState.selectedPeriod.labelFa}",
                                    fontSize = 10.sp,
                                    color = Color(0xFF94A3B8)
                                )
                                Text(
                                    text = "خلاصه شاخص‌های عملکرد زرگری",
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFF8FAFC)
                                )
                            }
                        }

                        // 2x2 Grid of KPIs
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // KPI 1: Gross Profit
                                KpiGridCard(
                                    title = "سود ناخالص دوره",
                                    value = PersianNumberFormatter.formatWithSeparators(uiState.kpi.grossProfitTomans),
                                    unit = "تومان",
                                    subtitle = "سود اجرت و عیار",
                                    subtitleColor = Color(0xFF34D399),
                                    valueColor = Color.White,
                                    modifier = Modifier.weight(1f)
                                )

                                // KPI 2: Gold Weight
                                KpiGridCard(
                                    title = "موجودی وزنی طلا",
                                    value = PersianNumberFormatter.formatWeight(uiState.kpi.inventoryGoldWeight18k),
                                    unit = "گرم ۱۸",
                                    subtitle = "ویترین + گاوصندوق",
                                    subtitleColor = Color(0xFFCBD5E1),
                                    valueColor = Color(0xFFFBBF24),
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // KPI 3: Turnover
                                val turnoverMillions = uiState.kpi.turnoverTomans / 1_000_000L
                                val turnoverText = if (turnoverMillions > 0L) {
                                    PersianNumberFormatter.formatWithSeparators(turnoverMillions)
                                } else {
                                    PersianNumberFormatter.formatWithSeparators(uiState.kpi.turnoverTomans)
                                }
                                val turnoverUnit = if (turnoverMillions > 0L) "میلیون ت" else "تومان"

                                KpiGridCard(
                                    title = "گردش ریالی حساب‌ها",
                                    value = turnoverText,
                                    unit = turnoverUnit,
                                    subtitle = "ورودی و خروجی",
                                    subtitleColor = Color(0xFF94A3B8),
                                    valueColor = Color.White,
                                    modifier = Modifier.weight(1f)
                                )

                                // KPI 4: Customer Receivables
                                KpiGridCard(
                                    title = "مطالبات مانده مشتریان",
                                    value = PersianNumberFormatter.formatWithSeparators(uiState.kpi.customerReceivablesTomans),
                                    unit = "تومان",
                                    subtitle = "${PersianNumberFormatter.toPersianDigits(uiState.kpi.unsettledInvoicesCount.toString())} فقره تسویه‌نشده",
                                    subtitleColor = Color(0xFFF87171),
                                    valueColor = Color(0xFFFCA5A5),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }

            // ==========================================
            // 3. Specialized Gold Ledgers (۴ سرفصل)
            // ==========================================
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ترازنامه‌های تخصصی زرگری",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textMain
                    )
                    Text(
                        text = "سرفصل‌های حسابداری",
                        fontSize = 11.sp,
                        color = colors.textMuted
                    )
                }

                LuxuryCard(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        // Module 1: Profit & Sales
                        SpecializedLedgerRow(
                            title = "سود و زیان و عملکرد فروش",
                            subtitle = "سود قطعی، اجرت ساخت، سود عیار و پورسانت‌ها",
                            icon = ReportingInsights,
                            iconBg = Color(0xFFFEF3C7),
                            iconTint = Color(0xFFD97706),
                            onClick = { onOpenBreakdown(ReportingBreakdownType.SALES_PERFORMANCE) }
                        )

                        HorizontalDivider(color = colors.border.copy(alpha = 0.35f), thickness = 0.5.dp)

                        // Module 2: Gold Weight Balance & Vault
                        SpecializedLedgerRow(
                            title = "تراز وزنی و انبار طلا",
                            subtitle = "گردش طلای ۱۸، آبشده شرطی، بنکداری و ویترین",
                            icon = ReportingScale,
                            iconBg = Color(0xFFFFFBEB),
                            iconTint = Color(0xFFB45309),
                            onClick = { onOpenBreakdown(ReportingBreakdownType.GOLD_INVENTORY) }
                        )

                        HorizontalDivider(color = colors.border.copy(alpha = 0.35f), thickness = 0.5.dp)

                        // Module 3: Customer Debtors & Creditors
                        SpecializedLedgerRow(
                            title = "صورتحساب بدهکاران و بستانکاران",
                            subtitle = "تراز وزنی و ریالی طرفین حساب",
                            icon = ReportingContacts,
                            iconBg = Color(0xFFECFDF5),
                            iconTint = Color(0xFF059669),
                            onClick = { onOpenBreakdown(ReportingBreakdownType.DEBTORS_CREDITORS) }
                        )

                        HorizontalDivider(color = colors.border.copy(alpha = 0.35f), thickness = 0.5.dp)

                        // Module 4: VAT & Moadian Tax System
                        SpecializedLedgerRow(
                            title = "گزارش مالیات ارزش‌افزوده",
                            subtitle = "صورت‌های مالیاتی فصلی بر مبنای اجرت و سود",
                            icon = ReportingReceipt,
                            iconBg = Color(0xFFEFF6FF),
                            iconTint = Color(0xFF2563EB),
                            onClick = { onOpenBreakdown(ReportingBreakdownType.VAT_REPORT) }
                        )
                    }
                }
            }

            // ==========================================
            // 4. Realtime Quick Reports Section (امروز)
            // ==========================================
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "گزارشات سریع امروز",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textMain,
                    modifier = Modifier.padding(horizontal = 2.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Today Sales
                    val salesMillions = uiState.quickSummary.salesAmountTomans / 1_000_000L
                    val salesAmountText = if (salesMillions > 0L) {
                        "${PersianNumberFormatter.formatWithSeparators(salesMillions)} م.ت"
                    } else {
                        "${PersianNumberFormatter.formatWithSeparators(uiState.quickSummary.salesAmountTomans)} ت"
                    }

                    QuickReportCard(
                        title = "فروش امروز",
                        countText = "${PersianNumberFormatter.toPersianDigits(uiState.quickSummary.salesCount.toString())} مورد",
                        weightText = "وزن کل: ${PersianNumberFormatter.formatWeight(uiState.quickSummary.salesWeight18k)} گرم",
                        amountLabel = "مبلغ کل:",
                        amountText = salesAmountText,
                        icon = ReportingPointOfSale,
                        iconTint = colors.goldPrimary,
                        modifier = Modifier.weight(1f)
                    )

                    // Today Purchases
                    val purchaseMillions = uiState.quickSummary.purchaseAmountTomans / 1_000_000L
                    val purchaseAmountText = if (purchaseMillions > 0L) {
                        "${PersianNumberFormatter.formatWithSeparators(purchaseMillions)} م.ت"
                    } else {
                        "${PersianNumberFormatter.formatWithSeparators(uiState.quickSummary.purchaseAmountTomans)} ت"
                    }

                    QuickReportCard(
                        title = "خرید امروز",
                        countText = "${PersianNumberFormatter.toPersianDigits(uiState.quickSummary.purchaseCount.toString())} مورد",
                        weightText = "وزن کل: ${PersianNumberFormatter.formatWeight(uiState.quickSummary.purchaseWeight18k)} گرم",
                        amountLabel = "پرداختی:",
                        amountText = purchaseAmountText,
                        icon = ReportingShoppingBag,
                        iconTint = colors.goldSecondary,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }

    // ==========================================
    // 5. Specialized Breakdown Dialogs
    // ==========================================
    uiState.activeBreakdown?.let { breakdownType ->
        BreakdownDetailsDialog(
            type = breakdownType,
            uiState = uiState,
            onDismiss = onCloseBreakdown,
            onNavigateInventory = {
                onCloseBreakdown()
                onNavigateInventory()
            },
            onNavigateCustomerLedger = {
                onCloseBreakdown()
                onNavigateCustomerLedger()
            }
        )
    }

    // Custom Date Range Dialog
    if (uiState.isCustomDateDialogVisible) {
        CustomDateRangeDialog(
            currentStart = uiState.customStartDateShamsi,
            currentEnd = uiState.customEndDateShamsi,
            onDismiss = onCloseCustomDateDialog,
            onSubmit = onSubmitCustomRange
        )
    }
}

@Composable
private fun KpiGridCard(
    title: String,
    value: String,
    unit: String,
    subtitle: String,
    subtitleColor: Color,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF1E283C),
        border = BorderStroke(0.6.dp, Color.White.copy(alpha = 0.08f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = title,
                fontSize = 10.sp,
                color = Color(0xFF94A3B8),
                maxLines = 1
            )
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = value,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                    color = valueColor
                )
                Text(
                    text = unit,
                    fontSize = 9.5.sp,
                    color = Color(0xFF94A3B8),
                    modifier = Modifier.padding(bottom = 1.dp)
                )
            }
            Text(
                text = subtitle,
                fontSize = 9.sp,
                fontWeight = FontWeight.Medium,
                color = subtitleColor,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun SpecializedLedgerRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconBg: Color,
    iconTint: Color,
    onClick: () -> Unit
) {
    val colors = LocalGoldExColors.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 11.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(19.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textMain
                )
                Text(
                    text = subtitle,
                    fontSize = 10.sp,
                    color = colors.textMuted
                )
            }
        }

        Surface(
            shape = CircleShape,
            color = colors.surfaceElevated,
            modifier = Modifier.size(24.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = ReportingArrowForward,
                    contentDescription = null,
                    tint = colors.textMuted,
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }
}

@Composable
private fun QuickReportCard(
    title: String,
    countText: String,
    weightText: String,
    amountLabel: String,
    amountText: String,
    icon: ImageVector,
    iconTint: Color,
    modifier: Modifier = Modifier
) {
    val colors = LocalGoldExColors.current

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = colors.surface,
        border = BorderStroke(0.6.dp, colors.goldBorder),
        shadowElevation = if (colors.isDark) 0.dp else 1.dp,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = colors.textSecondary
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(17.dp)
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(
                    text = countText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                    color = colors.textMain
                )
                Text(
                    text = weightText,
                    fontSize = 10.sp,
                    color = colors.textMuted
                )
            }

            HorizontalDivider(color = colors.border.copy(alpha = 0.35f), thickness = 0.5.dp)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = amountLabel,
                    fontSize = 10.sp,
                    color = colors.textMuted
                )
                Text(
                    text = amountText,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textMain
                )
            }
        }
    }
}

@Composable
private fun BreakdownDetailsDialog(
    type: ReportingBreakdownType,
    uiState: ReportingUiState,
    onDismiss: () -> Unit,
    onNavigateInventory: () -> Unit,
    onNavigateCustomerLedger: () -> Unit
) {
    val colors = LocalGoldExColors.current

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = colors.surface,
            border = BorderStroke(0.8.dp, colors.goldBorder),
            shadowElevation = 16.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header
                val (title, icon, iconTint, iconBg) = when (type) {
                    ReportingBreakdownType.SALES_PERFORMANCE ->
                        Quadruple("سود و زیان و عملکرد فروش", ReportingInsights, Color(0xFFD97706), Color(0xFFFEF3C7))
                    ReportingBreakdownType.GOLD_INVENTORY ->
                        Quadruple("تراز وزنی و انبار طلا", ReportingScale, Color(0xFFB45309), Color(0xFFFFFBEB))
                    ReportingBreakdownType.DEBTORS_CREDITORS ->
                        Quadruple("صورتحساب بدهکاران و بستانکاران", ReportingContacts, Color(0xFF059669), Color(0xFFECFDF5))
                    ReportingBreakdownType.VAT_REPORT ->
                        Quadruple("گزارش مالیات ارزش‌افزوده", ReportingReceipt, Color(0xFF2563EB), Color(0xFFEFF6FF))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(iconBg),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = iconTint,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Column {
                            Text(
                                text = title,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textMain
                            )
                            Text(
                                text = "دوره: ${uiState.selectedPeriod.labelFa}",
                                fontSize = 10.sp,
                                color = colors.textMuted
                            )
                        }
                    }
                }

                HorizontalDivider(color = colors.border.copy(alpha = 0.4f), thickness = 0.6.dp)

                // Body content per type
                when (type) {
                    ReportingBreakdownType.SALES_PERFORMANCE -> {
                        val sp = uiState.salesPerformance
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            BreakdownRow("کل فروش ناخالص:", "${PersianNumberFormatter.formatWithSeparators(sp.grossSalesTomans)} تومان")
                            BreakdownRow("ارزش طلای خام فروخته شده:", "${PersianNumberFormatter.formatWithSeparators(sp.rawGoldValueTomans)} تومان")
                            BreakdownRow("مجموع اجرت ساخت:", "${PersianNumberFormatter.formatWithSeparators(sp.totalWageTomans)} تومان", valueColor = colors.goldPrimary)
                            BreakdownRow("مجموع سود مصوب گالری:", "${PersianNumberFormatter.formatWithSeparators(sp.totalProfitTomans)} تومان", valueColor = colors.profitGreen)
                            BreakdownRow("مالیات بر ارزش‌افزوده وصولی:", "${PersianNumberFormatter.formatWithSeparators(sp.totalTaxTomans)} تومان")
                            BreakdownRow("مجموع وزن ۱۸ عیار فروخته شده:", "${PersianNumberFormatter.formatWeight(sp.totalWeight18k)} گرم")
                            BreakdownRow("تعداد اقلام فروش رفته:", "${PersianNumberFormatter.toPersianDigits(sp.itemsCount.toString())} مورد")
                        }
                    }

                    ReportingBreakdownType.GOLD_INVENTORY -> {
                        val gi = uiState.goldInventory
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            BreakdownRow("مجموع وزن ۱۸ عیار انبار:", "${PersianNumberFormatter.formatWeight(gi.totalWeight18k)} گرم", valueColor = colors.goldPrimary)
                            BreakdownRow("موجودی در ویترین و سینی‌ها:", "${PersianNumberFormatter.formatWeight(gi.showcaseWeight18k)} گرم")
                            BreakdownRow("موجودی در گاوصندوق و انبار:", "${PersianNumberFormatter.formatWeight(gi.vaultWeight18k)} گرم")
                            BreakdownRow("تعداد کل کارهای ثبت‌شده:", "${PersianNumberFormatter.toPersianDigits(gi.piecesCount.toString())} قطعه")
                            BreakdownRow("تعداد سینی‌های فعال:", "${PersianNumberFormatter.toPersianDigits(gi.activeTraysCount.toString())} سینی")
                            BreakdownRow("تعداد گاوصندوق‌های فعال:", "${PersianNumberFormatter.toPersianDigits(gi.activeSafesCount.toString())} گاوصندوق")
                        }
                    }

                    ReportingBreakdownType.DEBTORS_CREDITORS -> {
                        val dc = uiState.debtorsCreditors
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            BreakdownRow("کل مطالبات ریالی (بدهکار به ما):", "${PersianNumberFormatter.formatWithSeparators(dc.totalReceivablesTomans)} تومان", valueColor = colors.errorRed)
                            BreakdownRow("تعداد طرف‌حساب‌های بدهکار:", "${PersianNumberFormatter.toPersianDigits(dc.debtorCount.toString())} مشتری")
                            BreakdownRow("کل بستانکاری‌ها (طلب دیگران از ما):", "${PersianNumberFormatter.formatWithSeparators(dc.totalPayablesTomans)} تومان")
                            BreakdownRow("تعداد طرف‌حساب‌های بستانکار:", "${PersianNumberFormatter.toPersianDigits(dc.creditorCount.toString())} همکار")
                            BreakdownRow("تراز وزنی طلا:", "${PersianNumberFormatter.formatWeight(dc.goldDebtGrams)} گرم ۱۸")
                        }
                    }

                    ReportingBreakdownType.VAT_REPORT -> {
                        val vr = uiState.vatReport
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            BreakdownRow("مأخذ مشمول مالیات (اجرت + سود):", "${PersianNumberFormatter.formatWithSeparators(vr.taxableBaseTomans)} تومان", valueColor = colors.goldPrimary)
                            BreakdownRow("مالیات ارزش افزوده متعلقه (${PersianNumberFormatter.toPersianDigits(vr.vatPercent.toString())}٪):", "${PersianNumberFormatter.formatWithSeparators(vr.totalVatCollectedTomans)} تومان", valueColor = colors.profitGreen)
                            BreakdownRow("ارزش اصل طلای خام (۱۰۰٪ معاف):", "${PersianNumberFormatter.formatWithSeparators(vr.taxExemptRawGoldTomans)} تومان")
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = colors.goldContainer.copy(alpha = 0.35f),
                                border = BorderStroke(0.6.dp, colors.goldBorder),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "مطابق قانون دائمی مالیات بر ارزش‌افزوده طلا مصوب دی‌ماه ۱۴۰۰، اصل طلا، پلاتین و جواهر معاف از مالیات بوده و ۹٪ ارزش افزوده صرفاً به اجرت ساخت و سود تعلق می‌گیرد.",
                                    fontSize = 10.sp,
                                    lineHeight = 15.sp,
                                    color = colors.textSecondary,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(color = colors.border.copy(alpha = 0.4f), thickness = 0.6.dp)

                // Action buttons (Two-action rule: Cancel/Close on right, Primary action on left)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Right action: Dismiss / Close
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(0.8.dp, colors.border)
                    ) {
                        Text(
                            text = "بستن",
                            fontSize = 11.5.sp,
                            color = colors.textSecondary
                        )
                    }

                    // Left action: Deep link to related feature if applicable
                    when (type) {
                        ReportingBreakdownType.GOLD_INVENTORY -> {
                            GoldButton(
                                text = "مشاهده انبار و ویترین",
                                onClick = onNavigateInventory,
                                modifier = Modifier.weight(1.2f)
                            )
                        }
                        ReportingBreakdownType.DEBTORS_CREDITORS -> {
                            GoldButton(
                                text = "دفتر حساب و معین",
                                onClick = onNavigateCustomerLedger,
                                modifier = Modifier.weight(1.2f)
                            )
                        }
                        else -> {
                            // Single dismiss button layout handled cleanly
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BreakdownRow(
    label: String,
    value: String,
    valueColor: Color? = null
) {
    val colors = LocalGoldExColors.current

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            color = colors.textSecondary
        )
        Text(
            text = value,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Bold,
            color = valueColor ?: colors.textMain
        )
    }
}

@Composable
private fun CustomDateRangeDialog(
    currentStart: String,
    currentEnd: String,
    onDismiss: () -> Unit,
    onSubmit: (startMs: Long, endMs: Long, startShamsi: String, endShamsi: String) -> Unit
) {
    val colors = LocalGoldExColors.current
    var startDateInput by remember { mutableStateOf(currentStart.ifBlank { "1403/01/01" }) }
    var endDateInput by remember { mutableStateOf(currentEnd.ifBlank { "1403/12/29" }) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = colors.surface,
            border = BorderStroke(0.8.dp, colors.goldBorder),
            shadowElevation = 16.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "تعیین بازه تاریخی دلخواه",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textMain
                )

                Text(
                    text = "تاریخ شروع و پایان بازه را به صورت شمسی وارد نمایید (مثال: ۱۴۰۳/۰۶/۰۱)",
                    fontSize = 10.5.sp,
                    color = colors.textMuted
                )

                OutlinedTextField(
                    value = startDateInput,
                    onValueChange = { startDateInput = it },
                    label = { Text("از تاریخ (شمسی)", fontSize = 11.sp) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colors.goldPrimary,
                        unfocusedBorderColor = colors.border
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = endDateInput,
                    onValueChange = { endDateInput = it },
                    label = { Text("تا تاریخ (شمسی)", fontSize = 11.sp) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colors.goldPrimary,
                        unfocusedBorderColor = colors.border
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Dialog Buttons (RTL: Cancel on right, Submit on left)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(0.8.dp, colors.border)
                    ) {
                        Text("انصراف", fontSize = 11.5.sp, color = colors.textSecondary)
                    }

                    GoldButton(
                        text = "اعمال فیلتر",
                        onClick = {
                            // Estimate timestamps (e.g. 30 days window)
                            val now = System.currentTimeMillis()
                            val startMs = now - (60L * 86_400_000L)
                            onSubmit(startMs, now, startDateInput, endDateInput)
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

private data class Quadruple<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)
