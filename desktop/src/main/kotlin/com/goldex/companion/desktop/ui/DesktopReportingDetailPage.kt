package com.goldex.companion.desktop.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.goldex.companion.desktop.state.DesktopDestination
import com.goldex.companion.desktop.ui.controls.*
import com.goldex.companion.domain.reporting.*
import com.goldex.companion.model.PersianNumberFormatter
import com.goldex.companion.ui.theme.ButtonShape
import com.goldex.companion.ui.theme.LocalGoldExColors
import com.goldex.companion.ui.theme.VazirmatnFamily
import kotlin.math.abs

/**
 * Detailed breakdown page for a selected reporting module.
 * Fully follows Windows control contract, RTL layout and Persian typography.
 */
@Composable
internal fun DesktopReportingDetailPage(
    type: ReportingBreakdownType,
    reporting: DesktopReporting,
    workspace: DesktopWorkspace
) {
    val state by reporting.uiState.collectAsState()
    val colors = LocalGoldExColors.current
    val clipboard = LocalClipboardManager.current
    val scrollState = rememberScrollState()

    var copiedMessage by remember { mutableStateOf(false) }
    LaunchedEffect(type, state.selectedPeriod) { copiedMessage = false }

    val periodLabel = if (state.selectedPeriod == ReportingPeriod.CUSTOM && state.customStartDateShamsi.isNotBlank()) {
        "${state.selectedPeriod.labelFa} (${state.customStartDateShamsi} تا ${state.customEndDateShamsi})"
    } else {
        state.selectedPeriod.labelFa
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // Top Toolbar: Period Filters + Custom Date Trigger + Copy Action
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
                modifier = Modifier.testTag("detail-period-filters"),
                fillWidth = false
            )

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                if (state.selectedPeriod == ReportingPeriod.CUSTOM) {
                    DesktopActionButton(
                        text = "تغییر تاریخ",
                        onClick = { reporting.setCustomDateDialogVisible(true) },
                        modifier = Modifier.testTag("detail-change-date"),
                        icon = Icons.Outlined.DateRange,
                        isSecondary = true
                    )
                }

                DesktopActionButton(
                    text = if (copiedMessage) "کپی شد ✓" else "کپی خلاصه",
                    onClick = {
                        val text = DesktopReportingShareText.format(type, state)
                        clipboard.setText(AnnotatedString(text))
                        copiedMessage = true
                    },
                    modifier = Modifier.testTag("detail-copy-summary"),
                    icon = if (copiedMessage) Icons.Outlined.Check else Icons.Outlined.ContentCopy,
                    isSecondary = true
                )
            }
        }

        // Active Period Tag Line
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Outlined.CalendarToday, null, Modifier.size(16.dp), tint = colors.goldPrimary)
            Text(
                "بازه زمانی فعال: $periodLabel",
                fontFamily = VazirmatnFamily,
                fontSize = 13.sp,
                color = colors.textMuted
            )
        }

        // Content according to breakdown type
        when (type) {
            ReportingBreakdownType.SALES_PERFORMANCE -> SalesPerformanceDetailContent(state)
            ReportingBreakdownType.GOLD_INVENTORY -> GoldInventoryDetailContent(state, workspace)
            ReportingBreakdownType.DEBTORS_CREDITORS -> DebtorsCreditorsDetailContent(state, workspace)
            ReportingBreakdownType.VAT_REPORT -> VatReportDetailContent(state)
        }
    }
}

@Composable
private fun SalesPerformanceDetailContent(state: ReportingUiState) {
    val colors = LocalGoldExColors.current
    val sales = state.salesPerformance

    // 4 Key metrics cards
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        DetailMetricCard(
            title = "سود خالص محقق‌شده",
            value = "${PersianNumberFormatter.formatWithSeparators(sales.totalProfitTomans)} تومان",
            subtitle = "سود قانونی معاملات دوره",
            icon = Icons.Outlined.TrendingUp,
            isHighlight = true,
            modifier = Modifier.weight(1f)
        )
        DetailMetricCard(
            title = "مجموع فروش ناخالص",
            value = "${PersianNumberFormatter.formatWithSeparators(sales.grossSalesTomans)} تومان",
            subtitle = "ارزش کل فاکتورهای صادره",
            icon = Icons.Outlined.ReceiptLong,
            modifier = Modifier.weight(1f)
        )
        DetailMetricCard(
            title = "مجموع اجرت ساخت",
            value = "${PersianNumberFormatter.formatWithSeparators(sales.totalWageTomans)} تومان",
            subtitle = "اجرت ثبت‌شده در فروش",
            icon = Icons.Outlined.Hardware,
            modifier = Modifier.weight(1f)
        )
        DetailMetricCard(
            title = "وزن کل طلای ۱۸ عیار",
            value = "${PersianNumberFormatter.formatWeight(sales.totalWeight18k)} گرم",
            subtitle = "${PersianNumberFormatter.toPersianDigits(sales.itemsCount.toString())} قطعه فروخته شده",
            icon = Icons.Outlined.Scale,
            modifier = Modifier.weight(1f)
        )
    }

    // Sales Categories Table
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = colors.surface,
        border = BorderStroke(1.dp, colors.border),
        modifier = Modifier.fillMaxWidth().testTag("sales-categories-card")
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(
                "فروش به تفکیک دسته‌بندی کالا",
                fontFamily = VazirmatnFamily,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textMain
            )

            if (state.details.salesCategories.isEmpty()) {
                Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    Text("در این بازه زمانی فروشی ثبت نشده است", color = colors.textMuted, fontSize = 13.sp)
                }
            } else {
                Row(
                    Modifier.fillMaxWidth().background(colors.surfaceElevated, ButtonShape).padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("دسته‌بندی", modifier = Modifier.weight(2f), fontWeight = FontWeight.Bold, fontSize = 12.5.sp, color = colors.textMuted)
                    Text("مجموع فروش", modifier = Modifier.weight(1.5f), fontWeight = FontWeight.Bold, fontSize = 12.5.sp, color = colors.textMuted)
                    Text("اجرت ساخت", modifier = Modifier.weight(1.5f), fontWeight = FontWeight.Bold, fontSize = 12.5.sp, color = colors.textMuted)
                    Text("سود فروشنده", modifier = Modifier.weight(1.5f), fontWeight = FontWeight.Bold, fontSize = 12.5.sp, color = colors.textMuted)
                    Text("مالیات", modifier = Modifier.weight(1.2f), fontWeight = FontWeight.Bold, fontSize = 12.5.sp, color = colors.textMuted)
                }

                state.details.salesCategories.forEach { row ->
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(row.title, modifier = Modifier.weight(2f), fontSize = 13.sp, color = colors.textMain, fontWeight = FontWeight.SemiBold)
                        Text("${PersianNumberFormatter.formatWithSeparators(row.salesTomans)} ت", modifier = Modifier.weight(1.5f), fontSize = 13.sp, color = colors.textMain)
                        Text("${PersianNumberFormatter.formatWithSeparators(row.wageTomans)} ت", modifier = Modifier.weight(1.5f), fontSize = 13.sp, color = colors.textMuted)
                        Text("${PersianNumberFormatter.formatWithSeparators(row.profitTomans)} ت", modifier = Modifier.weight(1.5f), fontSize = 13.sp, color = colors.goldPrimary, fontWeight = FontWeight.Bold)
                        Text("${PersianNumberFormatter.formatWithSeparators(row.vatTomans)} ت", modifier = Modifier.weight(1.2f), fontSize = 13.sp, color = colors.textMuted)
                    }
                    HorizontalDivider(color = colors.border.copy(alpha = 0.5f), thickness = 0.5.dp)
                }
            }
        }
    }

    // Top Selling Items
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = colors.surface,
        border = BorderStroke(1.dp, colors.border),
        modifier = Modifier.fillMaxWidth().testTag("top-items-card")
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(
                "اقلام پرفروش دوره",
                fontFamily = VazirmatnFamily,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textMain
            )

            if (state.details.topSalesItems.isEmpty()) {
                Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    Text("اطلاعات اقلام در این بازه ثبت نشده است", color = colors.textMuted, fontSize = 13.sp)
                }
            } else {
                Row(
                    Modifier.fillMaxWidth().background(colors.surfaceElevated, ButtonShape).padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("عنوان کالا", modifier = Modifier.weight(2.5f), fontWeight = FontWeight.Bold, fontSize = 12.5.sp, color = colors.textMuted)
                    Text("وزن ۱۸ عیار", modifier = Modifier.weight(1.5f), fontWeight = FontWeight.Bold, fontSize = 12.5.sp, color = colors.textMuted)
                    Text("عایدی فروش", modifier = Modifier.weight(2f), fontWeight = FontWeight.Bold, fontSize = 12.5.sp, color = colors.textMuted)
                }

                state.details.topSalesItems.forEach { item ->
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(item.title, modifier = Modifier.weight(2.5f), fontSize = 13.sp, color = colors.textMain, fontWeight = FontWeight.SemiBold)
                        Text("${PersianNumberFormatter.formatWeight(item.weight18k)} گرم", modifier = Modifier.weight(1.5f), fontSize = 13.sp, color = colors.textMuted)
                        Text("${PersianNumberFormatter.formatWithSeparators(item.earningsTomans)} تومان", modifier = Modifier.weight(2f), fontSize = 13.sp, color = colors.goldPrimary, fontWeight = FontWeight.SemiBold)
                    }
                    HorizontalDivider(color = colors.border.copy(alpha = 0.5f), thickness = 0.5.dp)
                }
            }
        }
    }
}

@Composable
private fun GoldInventoryDetailContent(
    state: ReportingUiState,
    workspace: DesktopWorkspace
) {
    val colors = LocalGoldExColors.current
    val inv = state.goldInventory

    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        DetailMetricCard(
            title = "موجودی کل عیار ۷۵۰",
            value = "${PersianNumberFormatter.formatWeight(inv.totalWeight18k)} گرم",
            subtitle = "${PersianNumberFormatter.toPersianDigits(inv.piecesCount.toString())} قطعه موجود در کل گالری",
            icon = Icons.Outlined.Inventory2,
            isHighlight = true,
            modifier = Modifier.weight(1f)
        )
        DetailMetricCard(
            title = "موجودی ویترین",
            value = "${PersianNumberFormatter.formatWeight(inv.showcaseWeight18k)} گرم",
            subtitle = "${PersianNumberFormatter.toPersianDigits(inv.activeTraysCount.toString())} سینی فعال در ویترین",
            icon = Icons.Outlined.Storefront,
            modifier = Modifier.weight(1f)
        )
        DetailMetricCard(
            title = "موجودی گاوصندوق",
            value = "${PersianNumberFormatter.formatWeight(inv.vaultWeight18k)} گرم",
            subtitle = "${PersianNumberFormatter.toPersianDigits(inv.activeSafesCount.toString())} گاوصندوق ثبت‌شده",
            icon = Icons.Outlined.Security,
            modifier = Modifier.weight(1f)
        )
        DetailMetricCard(
            title = "گردش دوره (ورود / خروج)",
            value = "+${PersianNumberFormatter.formatWeight(state.details.periodInboundGrams)} / -${PersianNumberFormatter.formatWeight(state.details.periodOutboundGrams)} گرم",
            subtitle = "گردش ثبت‌شده فاکتورها در دوره",
            icon = Icons.Outlined.SyncAlt,
            modifier = Modifier.weight(1f)
        )
    }

    // Inventory Locations
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = colors.surface,
        border = BorderStroke(1.dp, colors.border),
        modifier = Modifier.fillMaxWidth().testTag("inventory-locations-card")
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "موجودی طلا بر اساس موقعیت فیزیکی",
                    fontFamily = VazirmatnFamily,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textMain
                )
                DesktopActionButton(
                    text = "مدیریت انبار و ویترین",
                    onClick = { workspace.navigate(DesktopDestination.INVENTORY) },
                    icon = Icons.Outlined.Inventory,
                    isSecondary = true
                )
            }

            if (state.details.inventoryLocations.isEmpty()) {
                Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    Text("موجودی ثبت‌شده‌ای برای انبار یافت نشد", color = colors.textMuted, fontSize = 13.sp)
                }
            } else {
                Row(
                    Modifier.fillMaxWidth().background(colors.surfaceElevated, ButtonShape).padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("موقعیت / سینی", modifier = Modifier.weight(2f), fontWeight = FontWeight.Bold, fontSize = 12.5.sp, color = colors.textMuted)
                    Text("تعداد قطعات", modifier = Modifier.weight(1.5f), fontWeight = FontWeight.Bold, fontSize = 12.5.sp, color = colors.textMuted)
                    Text("وزن ۱۸ عیار", modifier = Modifier.weight(1.5f), fontWeight = FontWeight.Bold, fontSize = 12.5.sp, color = colors.textMuted)
                }

                state.details.inventoryLocations.forEach { loc ->
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(loc.location, modifier = Modifier.weight(2f), fontSize = 13.sp, color = colors.textMain, fontWeight = FontWeight.SemiBold)
                        Text("${PersianNumberFormatter.toPersianDigits(loc.piecesCount.toString())} عدد", modifier = Modifier.weight(1.5f), fontSize = 13.sp, color = colors.textMuted)
                        Text("${PersianNumberFormatter.formatWeight(loc.weight18k)} گرم", modifier = Modifier.weight(1.5f), fontSize = 13.sp, color = colors.goldPrimary, fontWeight = FontWeight.Bold)
                    }
                    HorizontalDivider(color = colors.border.copy(alpha = 0.5f), thickness = 0.5.dp)
                }
            }
        }
    }
}

@Composable
private fun DebtorsCreditorsDetailContent(
    state: ReportingUiState,
    workspace: DesktopWorkspace
) {
    val colors = LocalGoldExColors.current
    val debt = state.debtorsCreditors

    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        DetailMetricCard(
            title = "کل مطالبات ریالی (طلب)",
            value = "${PersianNumberFormatter.formatWithSeparators(debt.totalReceivablesTomans)} تومان",
            subtitle = "${PersianNumberFormatter.toPersianDigits(debt.debtorCount.toString())} طرف‌حساب بدهکار به گالری",
            icon = Icons.Outlined.ArrowDownward,
            isHighlight = true,
            modifier = Modifier.weight(1f)
        )
        DetailMetricCard(
            title = "کل بدهی ریالی (بستانکاری دیگران)",
            value = "${PersianNumberFormatter.formatWithSeparators(debt.totalPayablesTomans)} تومان",
            subtitle = "${PersianNumberFormatter.toPersianDigits(debt.creditorCount.toString())} طرف‌حساب بستانکار از گالری",
            icon = Icons.Outlined.ArrowUpward,
            modifier = Modifier.weight(1f)
        )
        DetailMetricCard(
            title = "تراز وزنی طلای طرف‌حساب‌ها",
            value = "${PersianNumberFormatter.formatWeight(debt.goldDebtGrams)} گرم",
            subtitle = "خالص مانده طلای امانی و حسابی",
            icon = Icons.Outlined.Scale,
            modifier = Modifier.weight(1f)
        )
    }

    // Customer Balances Table
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = colors.surface,
        border = BorderStroke(1.dp, colors.border),
        modifier = Modifier.fillMaxWidth().testTag("debtors-table-card")
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "دفتر معین و مانده حساب طرف‌حساب‌ها",
                    fontFamily = VazirmatnFamily,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textMain
                )
                DesktopActionButton(
                    text = "دفتر مشتریان و تسویه",
                    onClick = { workspace.navigate(DesktopDestination.CUSTOMERS) },
                    icon = Icons.Outlined.People,
                    isSecondary = true
                )
            }

            if (state.details.customerBalances.isEmpty()) {
                Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    Text("طرف‌حسابی با مانده باز یافت نشد", color = colors.textMuted, fontSize = 13.sp)
                }
            } else {
                Row(
                    Modifier.fillMaxWidth().background(colors.surfaceElevated, ButtonShape).padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("نام طرف‌حساب", modifier = Modifier.weight(2f), fontWeight = FontWeight.Bold, fontSize = 12.5.sp, color = colors.textMuted)
                    Text("نقش", modifier = Modifier.weight(1.5f), fontWeight = FontWeight.Bold, fontSize = 12.5.sp, color = colors.textMuted)
                    Text("مانده ریالی", modifier = Modifier.weight(1.8f), fontWeight = FontWeight.Bold, fontSize = 12.5.sp, color = colors.textMuted)
                    Text("مانده طلایی", modifier = Modifier.weight(1.5f), fontWeight = FontWeight.Bold, fontSize = 12.5.sp, color = colors.textMuted)
                    Text("وضعیت", modifier = Modifier.weight(1.2f), fontWeight = FontWeight.Bold, fontSize = 12.5.sp, color = colors.textMuted)
                }

                state.details.customerBalances.forEach { cust ->
                    val isDebtor = cust.cashDebtTomans > 0 || cust.goldDebtGrams > 0
                    val isCreditor = cust.cashDebtTomans < 0 || cust.goldDebtGrams < 0
                    val statusText = when {
                        isDebtor -> "بدهکار"
                        isCreditor -> "بستانکار"
                        else -> "بی‌حساب"
                    }
                    val statusColor = when {
                        isDebtor -> colors.errorRed
                        isCreditor -> colors.goldSecondary
                        else -> colors.textMuted
                    }

                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(cust.name, modifier = Modifier.weight(2f), fontSize = 13.sp, color = colors.textMain, fontWeight = FontWeight.SemiBold)
                        Text(cust.role, modifier = Modifier.weight(1.5f), fontSize = 13.sp, color = colors.textMuted)
                        Text(
                            "${PersianNumberFormatter.formatWithSeparators(abs(cust.cashDebtTomans))} ت",
                            modifier = Modifier.weight(1.8f),
                            fontSize = 13.sp,
                            color = statusColor,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "${PersianNumberFormatter.formatWeight(abs(cust.goldDebtGrams))} گرم",
                            modifier = Modifier.weight(1.5f),
                            fontSize = 13.sp,
                            color = statusColor
                        )
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = statusColor.copy(alpha = 0.12f),
                            modifier = Modifier.weight(1.2f)
                        ) {
                            Text(
                                statusText,
                                Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = statusColor
                            )
                        }
                    }
                    HorizontalDivider(color = colors.border.copy(alpha = 0.5f), thickness = 0.5.dp)
                }
            }
        }
    }
}

@Composable
private fun VatReportDetailContent(state: ReportingUiState) {
    val colors = LocalGoldExColors.current
    val vat = state.vatReport

    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        DetailMetricCard(
            title = "مالیات بر ارزش افزوده قابل پرداخت",
            value = "${PersianNumberFormatter.formatWithSeparators(vat.totalVatCollectedTomans)} تومان",
            subtitle = "مالیات قانونی ثبت‌شده در فاکتورها (${PersianNumberFormatter.toPersianDigits(vat.vatPercent.toString())}٪)",
            icon = Icons.Outlined.Receipt,
            isHighlight = true,
            modifier = Modifier.weight(1f)
        )
        DetailMetricCard(
            title = "مأخذ مشمول مالیات (اجرت + سود)",
            value = "${PersianNumberFormatter.formatWithSeparators(vat.taxableBaseTomans)} تومان",
            subtitle = "مجموع اجرت ساخت و سود ناخالص",
            icon = Icons.Outlined.AccountBalanceWallet,
            modifier = Modifier.weight(1f)
        )
        DetailMetricCard(
            title = "اصل طلای خام (معاف از مالیات)",
            value = "${PersianNumberFormatter.formatWithSeparators(vat.taxExemptRawGoldTomans)} تومان",
            subtitle = "ارزش اصل طلای خام بدون ارزش افزوده",
            icon = Icons.Outlined.VerifiedUser,
            modifier = Modifier.weight(1f)
        )
    }

    // Legal compliance notice box
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = colors.surfaceElevated,
        border = BorderStroke(1.dp, colors.goldBorder.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth().testTag("vat-notice-card")
    ) {
        Row(
            Modifier.padding(18.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(Icons.Outlined.Info, "قانون مالیات", Modifier.size(24.dp), tint = colors.goldPrimary)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    "قانون مالیات بر ارزش افزوده صنف طلا و جواهر (سامانه مودیان)",
                    fontFamily = VazirmatnFamily,
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.goldPrimary
                )
                Text(
                    "طبق قانون دائمی مالیات بر ارزش افزوده، اصل طلای خام، شمش و پلاتین به طور کامل از مالیات بر ارزش افزوده معاف است. مالیات صرفاً به «اجرت ساخت» و «سود فروشنده» تعلق می‌گیرد. فاکتورهای صادرشده در سامانه قیراط کاملاً منطبق بر این قانون و آماده اظهار در سامانه مودیان و پایانه‌های فروشگاهی می‌باشند.",
                    fontFamily = VazirmatnFamily,
                    fontSize = 13.sp,
                    lineHeight = 22.sp,
                    color = colors.textSecondary
                )
            }
        }
    }
}

@Composable
private fun DetailMetricCard(
    title: String,
    value: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isHighlight: Boolean = false,
    modifier: Modifier = Modifier
) {
    val colors = LocalGoldExColors.current
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (isHighlight) colors.surfaceElevated else colors.surface,
        border = BorderStroke(1.dp, if (isHighlight) colors.goldPrimary.copy(alpha = 0.5f) else colors.border),
        shadowElevation = if (isHighlight) 4.dp else 1.dp,
        modifier = modifier
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, fontSize = 12.5.sp, color = colors.textMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Box(
                    Modifier.size(34.dp).clip(RoundedCornerShape(8.dp)).background(colors.goldContainer.copy(alpha = 0.25f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, null, Modifier.size(18.dp), tint = colors.goldPrimary)
                }
            }
            Text(
                value,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = if (isHighlight) colors.goldPrimary else colors.textMain,
                fontFamily = VazirmatnFamily,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(subtitle, fontSize = 11.5.sp, color = colors.textMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}
