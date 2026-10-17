package com.goldex.companion.desktop.ui

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.goldex.companion.desktop.state.DesktopReporting
import com.goldex.companion.desktop.state.DesktopWorkspace
import com.goldex.companion.desktop.ui.controls.*
import com.goldex.companion.domain.reporting.*
import com.goldex.companion.model.PersianNumberFormatter
import com.goldex.companion.ui.theme.ButtonShape
import com.goldex.companion.ui.theme.LocalGoldExColors
import com.goldex.companion.ui.theme.VazirmatnFamily

/**
 * Main Reporting & Financial Analytics Page in the Windows workspace.
 * Conforms strictly to the Persian Sovereign Aurum design tokens and Windows control contract.
 */
@Composable
internal fun DesktopReportingPage(
    reporting: DesktopReporting,
    workspace: DesktopWorkspace
) {
    val state by reporting.uiState.collectAsState()
    val colors = LocalGoldExColors.current
    val clipboard = LocalClipboardManager.current
    val scrollState = rememberScrollState()

    var copiedMessage by remember { mutableStateOf(false) }
    LaunchedEffect(state.selectedPeriod) { copiedMessage = false }

    if (state.activeBreakdown != null) {
        DesktopReportingDetailPage(state.activeBreakdown!!, reporting, workspace)
        return
    }

    val periodLabel = if (state.selectedPeriod == ReportingPeriod.CUSTOM && state.customStartDateShamsi.isNotBlank()) {
        "${state.selectedPeriod.labelFa} (${state.customStartDateShamsi} تا ${state.customEndDateShamsi})"
    } else {
        state.selectedPeriod.labelFa
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(bottom = 24.dp)
            .testTag("page-REPORTING"),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // 1. Top Filters and Actions Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            DesktopFilterRow(
                items = ReportingPeriod.entries,
                selectedItem = state.selectedPeriod,
                onItemSelected = reporting::selectPeriod,
                label = { it.labelFa },
                modifier = Modifier.testTag("reporting-period-filters"),
                fillWidth = false
            )

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                if (state.selectedPeriod == ReportingPeriod.CUSTOM) {
                    DesktopActionButton(
                        text = "تعیین تاریخ شمسی",
                        onClick = { reporting.setCustomDateDialogVisible(true) },
                        modifier = Modifier.testTag("reporting-open-calendar"),
                        icon = Icons.Outlined.DateRange,
                        isSecondary = true
                    )
                }

                DesktopActionButton(
                    text = if (copiedMessage) "کپی شد ✓" else "کپی خلاصه ترازنامه",
                    onClick = {
                        val text = DesktopReportingShareText.formatOverall(state)
                        clipboard.setText(AnnotatedString(text))
                        copiedMessage = true
                    },
                    modifier = Modifier.testTag("reporting-copy-overall"),
                    icon = if (copiedMessage) Icons.Outlined.Check else Icons.Outlined.ContentCopy,
                    isSecondary = true
                )
            }
        }

        // Active Period Notice Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Outlined.CalendarToday, null, Modifier.size(16.dp), tint = colors.goldPrimary)
            Text(
                "بازه زمانی گزارش: $periodLabel",
                fontFamily = VazirmatnFamily,
                fontSize = 13.sp,
                color = colors.textMuted
            )
        }

        // 2. Hero KPI Vault Card
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFF141B2B),
            border = BorderStroke(1.dp, Brush.verticalGradient(listOf(colors.goldPrimary.copy(alpha = 0.5f), colors.goldPrimary.copy(alpha = 0.15f)))),
            shadowElevation = 8.dp,
            modifier = Modifier.fillMaxWidth().testTag("hero-kpi-card")
        ) {
            Box(Modifier.fillMaxWidth()) {
                // Golden top specular hairline
                Box(
                    Modifier.fillMaxWidth().height(2.dp).background(
                        Brush.horizontalGradient(listOf(Color.Transparent, colors.goldPrimary, Color.Transparent))
                    )
                )

                Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                    // Header of Hero Card
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Box(
                                Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(colors.goldContainer.copy(alpha = 0.35f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Outlined.Analytics, null, Modifier.size(22.dp), tint = colors.goldPrimary)
                            }
                            Column {
                                Text(
                                    "ترازنامه و شاخص‌های کلیدی عملکرد",
                                    fontFamily = VazirmatnFamily,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    "محاسبه بر اساس داده‌های ثبت‌شده در سامانه قیراط",
                                    fontFamily = VazirmatnFamily,
                                    fontSize = 12.sp,
                                    color = Color(0xFF94A3B8)
                                )
                            }
                        }

                        // Status badge (unsettled invoices & debtors)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (state.kpi.unsettledInvoicesCount > 0) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = colors.errorRed.copy(alpha = 0.15f),
                                    border = BorderStroke(1.dp, colors.errorRed.copy(alpha = 0.4f))
                                ) {
                                    Text(
                                        "${PersianNumberFormatter.toPersianDigits(state.kpi.unsettledInvoicesCount.toString())} فاکتور تسویه‌نشده",
                                        Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                        fontFamily = VazirmatnFamily,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = colors.errorRed
                                    )
                                }
                            }
                            if (state.kpi.debtorCustomersCount > 0) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = colors.goldSecondary.copy(alpha = 0.15f),
                                    border = BorderStroke(1.dp, colors.goldSecondary.copy(alpha = 0.4f))
                                ) {
                                    Text(
                                        "${PersianNumberFormatter.toPersianDigits(state.kpi.debtorCustomersCount.toString())} طرف‌حساب بدهکار",
                                        Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                        fontFamily = VazirmatnFamily,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = colors.goldSecondary
                                    )
                                }
                            }
                        }
                    }

                    // 4 Hero Metrics in Responsive Row
                    Row(
                        Modifier.fillMaxWidth().background(Color(0xFF0F1523), RoundedCornerShape(14.dp)).padding(18.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HeroMetric(
                            label = "سود ناخالص محقق‌شده",
                            value = "${PersianNumberFormatter.formatWithSeparators(state.kpi.grossProfitTomans)} تومان",
                            color = colors.goldPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        Box(Modifier.width(1.dp).height(40.dp).background(Color(0xFF1E293B)))
                        HeroMetric(
                            label = "گردش مالی معاملات",
                            value = "${PersianNumberFormatter.formatWithSeparators(state.kpi.turnoverTomans)} تومان",
                            color = Color.White,
                            modifier = Modifier.weight(1f)
                        )
                        Box(Modifier.width(1.dp).height(40.dp).background(Color(0xFF1E293B)))
                        HeroMetric(
                            label = "موجودی طلای ۱۸ عیار",
                            value = "${PersianNumberFormatter.formatWeight(state.kpi.inventoryGoldWeight18k)} گرم",
                            color = Color(0xFFFCD34D),
                            modifier = Modifier.weight(1f)
                        )
                        Box(Modifier.width(1.dp).height(40.dp).background(Color(0xFF1E293B)))
                        HeroMetric(
                            label = "مطالبات از طرف‌حساب‌ها",
                            value = "${PersianNumberFormatter.formatWithSeparators(state.kpi.customerReceivablesTomans)} تومان",
                            color = if (state.kpi.customerReceivablesTomans > 0) colors.errorRed else Color.White,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // 3. Four Interactive Reporting Modules
        Text(
            "ماژول‌های تحلیلی و ترازنامه‌های تخصصی",
            fontFamily = VazirmatnFamily,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = colors.textMain
        )

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            // Module 1: Sales Performance
            ReportingModuleCard(
                title = "عملکرد فروش و سود و زیان",
                subtitle = "حاشیه سود، اجرت ساخت و ارزش فاکتورهای صادره",
                icon = Icons.Outlined.TrendingUp,
                primaryMetric = "${PersianNumberFormatter.formatWithSeparators(state.salesPerformance.totalProfitTomans)} تومان",
                primaryLabel = "سود خالص دوره",
                secondaryMetric = "${PersianNumberFormatter.formatWeight(state.salesPerformance.totalWeight18k)} گرم",
                secondaryLabel = "وزن کل فروش ۱۸ عیار",
                onClick = { reporting.openBreakdown(ReportingBreakdownType.SALES_PERFORMANCE) },
                modifier = Modifier.weight(1f).testTag("module-sales")
            )

            // Module 2: Gold Inventory & Kardex
            ReportingModuleCard(
                title = "تراز وزنی و انبار طلا",
                subtitle = "موجودی عیار ۷۵۰، ویترین، گاوصندوق و کاردکس ورود/خروج",
                icon = Icons.Outlined.Inventory2,
                primaryMetric = "${PersianNumberFormatter.formatWeight(state.goldInventory.totalWeight18k)} گرم",
                primaryLabel = "موجودی کل طلای ۷۵۰",
                secondaryMetric = "${PersianNumberFormatter.toPersianDigits(state.goldInventory.piecesCount.toString())} قطعه",
                secondaryLabel = "تعداد اقلام ثبت‌شده",
                onClick = { reporting.openBreakdown(ReportingBreakdownType.GOLD_INVENTORY) },
                modifier = Modifier.weight(1f).testTag("module-inventory")
            )
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            // Module 3: Debtors & Creditors
            ReportingModuleCard(
                title = "بدهکاران و مانده معین",
                subtitle = "تراز اشخاص، مطالبات ریالی، بدهی‌ها و گردش وزنی",
                icon = Icons.Outlined.AccountBalance,
                primaryMetric = "${PersianNumberFormatter.formatWithSeparators(state.debtorsCreditors.totalReceivablesTomans)} تومان",
                primaryLabel = "مطالبات ریالی (طلب)",
                secondaryMetric = "${PersianNumberFormatter.toPersianDigits(state.debtorsCreditors.debtorCount.toString())} طرف‌حساب",
                secondaryLabel = "تعداد مشتریان بدهکار",
                onClick = { reporting.openBreakdown(ReportingBreakdownType.DEBTORS_CREDITORS) },
                modifier = Modifier.weight(1f).testTag("module-debtors")
            )

            // Module 4: VAT Report
            ReportingModuleCard(
                title = "ارزش‌افزوده و مالیات فصلی",
                subtitle = "مالیات قانونی اجرت و سود منطبق بر سامانه مودیان",
                icon = Icons.Outlined.Receipt,
                primaryMetric = "${PersianNumberFormatter.formatWithSeparators(state.vatReport.totalVatCollectedTomans)} تومان",
                primaryLabel = "مالیات بر ارزش افزوده قابل پرداخت",
                secondaryMetric = "${PersianNumberFormatter.formatWithSeparators(state.vatReport.taxExemptRawGoldTomans)} تومان",
                secondaryLabel = "اصل طلای خام معاف",
                onClick = { reporting.openBreakdown(ReportingBreakdownType.VAT_REPORT) },
                modifier = Modifier.weight(1f).testTag("module-vat")
            )
        }

        // 4. Realtime Daily Summary (معاملات امروز)
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = colors.surface,
            border = BorderStroke(1.dp, colors.border),
            modifier = Modifier.fillMaxWidth().testTag("today-summary-card")
        ) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Outlined.Today, null, Modifier.size(20.dp), tint = colors.goldPrimary)
                        Text(
                            "خلاصه معاملات امروز",
                            fontFamily = VazirmatnFamily,
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textMain
                        )
                    }
                    Text(
                        "${PersianNumberFormatter.toPersianDigits(state.quickSummary.salesCount.toString())} معامله فروش ثبت‌شده امروز",
                        fontSize = 12.sp,
                        color = colors.textMuted
                    )
                }

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Surface(
                        shape = ButtonShape,
                        color = colors.surfaceElevated,
                        border = BorderStroke(1.dp, colors.border),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("فروش امروز", fontSize = 12.sp, color = colors.textMuted)
                            Text(
                                "${PersianNumberFormatter.formatWithSeparators(state.quickSummary.salesAmountTomans)} تومان",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.goldPrimary,
                                fontFamily = VazirmatnFamily
                            )
                            Text(
                                "وزن: ${PersianNumberFormatter.formatWeight(state.quickSummary.salesWeight18k)} گرم",
                                fontSize = 12.sp,
                                color = colors.textMuted
                            )
                        }
                    }

                    Surface(
                        shape = ButtonShape,
                        color = colors.surfaceElevated,
                        border = BorderStroke(1.dp, colors.border),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("خرید / مرجوعی امروز", fontSize = 12.sp, color = colors.textMuted)
                            Text(
                                "${PersianNumberFormatter.formatWithSeparators(state.quickSummary.purchaseAmountTomans)} تومان",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textMain,
                                fontFamily = VazirmatnFamily
                            )
                            Text(
                                "وزن: ${PersianNumberFormatter.formatWeight(state.quickSummary.purchaseWeight18k)} گرم (${PersianNumberFormatter.toPersianDigits(state.quickSummary.purchaseCount.toString())} مورد)",
                                fontSize = 12.sp,
                                color = colors.textMuted
                            )
                        }
                    }
                }
            }
        }
    }

    // Shamsi Date Range Picker Dialog Host
    if (state.isCustomDateDialogVisible) {
        DesktopShamsiDateRangeDialog(
            currentStartShamsi = state.customStartDateShamsi,
            currentEndShamsi = state.customEndDateShamsi,
            onDismiss = { reporting.setCustomDateDialogVisible(false) },
            onConfirmRange = { sMs, eMs, sShamsi, eShamsi ->
                reporting.setCustomDateRange(sMs, eMs, sShamsi, eShamsi)
            }
        )
    }
}

@Composable
private fun HeroMetric(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(horizontal = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(label, fontSize = 11.5.sp, color = Color(0xFF94A3B8), maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(
            value,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = color,
            fontFamily = VazirmatnFamily,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ReportingModuleCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    primaryMetric: String,
    primaryLabel: String,
    secondaryMetric: String,
    secondaryLabel: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalGoldExColors.current
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = colors.surface,
        border = BorderStroke(1.dp, colors.border),
        shadowElevation = 2.dp,
        modifier = modifier.clickable(onClick = onClick)
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(
                        Modifier.size(38.dp).clip(RoundedCornerShape(10.dp)).background(colors.goldContainer.copy(alpha = 0.3f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(icon, null, Modifier.size(20.dp), tint = colors.goldPrimary)
                    }
                    Column {
                        Text(title, fontSize = 14.5.sp, fontWeight = FontWeight.Bold, color = colors.textMain, fontFamily = VazirmatnFamily)
                        Text(subtitle, fontSize = 11.5.sp, color = colors.textMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                Icon(Icons.AutoMirrored.Outlined.ArrowForward, null, Modifier.size(18.dp), tint = colors.goldPrimary)
            }

            HorizontalDivider(color = colors.border.copy(alpha = 0.5f), thickness = 0.5.dp)

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(primaryLabel, fontSize = 11.sp, color = colors.textMuted)
                    Text(primaryMetric, fontSize = 14.5.sp, fontWeight = FontWeight.Bold, color = colors.goldPrimary, fontFamily = VazirmatnFamily)
                }
                Column(verticalArrangement = Arrangement.spacedBy(2.dp), horizontalAlignment = Alignment.End) {
                    Text(secondaryLabel, fontSize = 11.sp, color = colors.textMuted)
                    Text(secondaryMetric, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold, color = colors.textMain, fontFamily = VazirmatnFamily)
                }
            }
        }
    }
}
