package com.goldex.companion.ui.reporting

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import com.goldex.companion.domain.reporting.*
import com.goldex.companion.model.PersianNumberFormatter
import com.goldex.companion.ui.components.LuxuryCard
import com.goldex.companion.ui.hub.HubArrowRight
import com.goldex.companion.ui.hub.HubChevronLeft
import com.goldex.companion.ui.theme.ButtonShape
import com.goldex.companion.ui.theme.LocalGoldExColors
import com.goldex.companion.ui.theme.LuxuryMotion
import com.goldex.companion.ui.theme.VazirmatnFamily

@Composable
fun ReportingScreen(
    uiState: ReportingUiState,
    onBack: () -> Unit,
    onSelectPeriod: (ReportingPeriod) -> Unit,
    onOpenBreakdown: (ReportingBreakdownType) -> Unit,
    onOpenCustomDateDialog: () -> Unit,
    onCloseCustomDateDialog: () -> Unit,
    onSubmitCustomRange: (startMs: Long, endMs: Long, startShamsi: String, endShamsi: String) -> Unit
) {
    val colors = LocalGoldExColors.current
    val scrollState = rememberScrollState()

    Scaffold(
        containerColor = colors.background,
        topBar = { ReportingTopBar("مرکز گزارشات و ترازنامه‌ها", "تحلیل عملکرد مالی و ترازنامه‌های زرگری", onBack) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ==========================================
            // 1. Period Filter Chips Row
            // ==========================================
            ReportingPeriodFilters(uiState, onSelectPeriod, onOpenCustomDateDialog)

            // ==========================================
            // 2. Hero KPI Vault Card
            // ==========================================
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = Color(0xFF141B2B),
                border = BorderStroke(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        listOf(
                            Color(0xFFD4AF37).copy(alpha = 0.55f),
                            Color(0xFFB8860B).copy(alpha = 0.2f)
                        )
                    )
                ),
                shadowElevation = 12.dp,
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
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Vault Card Header
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFFD4AF37).copy(alpha = 0.20f))
                                    .border(1.dp, Color(0xFFD4AF37).copy(alpha = 0.45f), RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = ReportingAccountBalance,
                                    contentDescription = null,
                                    tint = Color(0xFFE9C349),
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                AnimatedContent(
                                    targetState = uiState.selectedPeriod.labelFa,
                                    transitionSpec = {
                                        LuxuryMotion.numberSlideSpec(isIncreasing = true)
                                    },
                                    label = "periodTitleAnimation"
                                ) { periodLabel ->
                                    Text(
                                        text = "تراز کل $periodLabel",
                                        fontSize = 11.5.sp,
                                        color = Color(0xFF94A3B8),
                                        fontFamily = VazirmatnFamily
                                    )
                                }
                                Text(
                                    text = "خلاصه شاخص‌های عملکرد زرگری",
                                    fontSize = 15.5.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFFF8FAFC),
                                    fontFamily = VazirmatnFamily
                                )
                            }
                        }

                        // 2x2 Grid of KPIs
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
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
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
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

    // Shamsi Date Range Picker Dialog
    if (uiState.isCustomDateDialogVisible && uiState.activeBreakdown == null) {
        ShamsiDateRangePickerDialog(
            currentStartShamsi = uiState.customStartDateShamsi,
            currentEndShamsi = uiState.customEndDateShamsi,
            onDismiss = onCloseCustomDateDialog,
            onConfirmRange = onSubmitCustomRange
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
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFF1E283C),
        border = BorderStroke(0.8.dp, Color.White.copy(alpha = 0.09f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Text(
                text = title,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF94A3B8),
                maxLines = 1,
                fontFamily = VazirmatnFamily
            )
            AnimatedContent(
                targetState = value to unit,
                transitionSpec = {
                    LuxuryMotion.numberSlideSpec(isIncreasing = true)
                },
                label = "kpiValueSlide"
            ) { (targetVal, targetUnit) ->
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = targetVal,
                        fontSize = 17.5.sp,
                        fontWeight = FontWeight.Black,
                        color = valueColor,
                        fontFamily = VazirmatnFamily
                    )
                    Text(
                        text = targetUnit,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFCBD5E1),
                        modifier = Modifier.padding(bottom = 2.dp),
                        fontFamily = VazirmatnFamily
                    )
                }
            }
            AnimatedContent(
                targetState = subtitle,
                transitionSpec = {
                    LuxuryMotion.numberSlideSpec(isIncreasing = true)
                },
                label = "kpiSubtitleSlide"
            ) { targetSub ->
                Text(
                    text = targetSub,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = subtitleColor,
                    maxLines = 1,
                    fontFamily = VazirmatnFamily
                )
            }
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
            .padding(horizontal = 14.dp, vertical = 12.dp),
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
                    color = colors.textMain,
                    fontFamily = VazirmatnFamily
                )
                Text(
                    text = subtitle,
                    fontSize = 10.sp,
                    color = colors.textMuted,
                    fontFamily = VazirmatnFamily
                )
            }
        }

        // Sleek iOS-style drill-down chevron (< in Persian RTL)
        Icon(
            imageVector = HubChevronLeft,
            contentDescription = null,
            tint = colors.textMuted.copy(alpha = 0.55f),
            modifier = Modifier.size(16.dp)
        )
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
                    color = colors.textSecondary,
                    fontFamily = VazirmatnFamily
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(17.dp)
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                AnimatedContent(
                    targetState = countText,
                    transitionSpec = { LuxuryMotion.numberSlideSpec(isIncreasing = true) },
                    label = "quickCountSlide"
                ) { targetCount ->
                    Text(
                        text = targetCount,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        color = colors.textMain,
                        fontFamily = VazirmatnFamily
                    )
                }
                AnimatedContent(
                    targetState = weightText,
                    transitionSpec = { LuxuryMotion.numberSlideSpec(isIncreasing = true) },
                    label = "quickWeightSlide"
                ) { targetWeight ->
                    Text(
                        text = targetWeight,
                        fontSize = 10.sp,
                        color = colors.textMuted,
                        fontFamily = VazirmatnFamily
                    )
                }
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
                    color = colors.textMuted,
                    fontFamily = VazirmatnFamily
                )
                AnimatedContent(
                    targetState = amountText,
                    transitionSpec = { LuxuryMotion.numberSlideSpec(isIncreasing = true) },
                    label = "quickAmountSlide"
                ) { targetAmount ->
                    Text(
                        text = targetAmount,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textMain,
                        fontFamily = VazirmatnFamily
                    )
                }
            }
        }
    }
}
