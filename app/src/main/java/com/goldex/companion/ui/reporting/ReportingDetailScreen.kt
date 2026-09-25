package com.goldex.companion.ui.reporting

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.goldex.companion.domain.reporting.*
import com.goldex.companion.model.PersianNumberFormatter
import com.goldex.companion.ui.hub.HubArrowRight
import com.goldex.companion.ui.theme.ButtonShape
import com.goldex.companion.ui.theme.LocalGoldExColors
import com.goldex.companion.ui.theme.VazirmatnFamily
import kotlin.math.abs

/** Full report pages reached from the existing reports center. */
@Composable
fun ReportingDetailScreen(
    type: ReportingBreakdownType,
    uiState: ReportingUiState,
    onBack: () -> Unit,
    onSelectPeriod: (ReportingPeriod) -> Unit,
    onOpenCustomDateDialog: () -> Unit,
    onNavigateInventory: () -> Unit,
    onNavigateCustomerLedger: () -> Unit
) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        val colors = LocalGoldExColors.current
        val title = when (type) {
            ReportingBreakdownType.SALES_PERFORMANCE -> "گزارش سود و عملکرد فروش"
            ReportingBreakdownType.GOLD_INVENTORY -> "تراز وزنی و انبار طلا"
            ReportingBreakdownType.DEBTORS_CREDITORS -> "گزارش بدهکاران و مانده معین"
            ReportingBreakdownType.VAT_REPORT -> "گزارش ارزش‌افزوده و مالیات فصلی"
        }
        val subtitle = when (type) {
            ReportingBreakdownType.SALES_PERFORMANCE -> "تحلیل معاملات ثبت‌شده در دوره"
            ReportingBreakdownType.GOLD_INVENTORY -> "موجودی فعلی ویترین و خزانه"
            ReportingBreakdownType.DEBTORS_CREDITORS -> "مانده حساب‌های ریالی و وزنی ثبت‌شده"
            ReportingBreakdownType.VAT_REPORT -> "مالیات ثبت‌شده بر اجرت و سود طلا"
        }
        Scaffold(containerColor = colors.background, topBar = {
            Surface(color = colors.surface, border = BorderStroke(0.6.dp, colors.goldBorder)) {
                Row(
                    modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    IconButton(onClick = onBack, modifier = Modifier.size(40.dp)) {
                        Icon(HubArrowRight, contentDescription = "بازگشت به مرکز گزارشات", tint = colors.goldPrimary)
                    }
                    Column {
                        Text(title, fontFamily = VazirmatnFamily, fontSize = 16.sp, fontWeight = FontWeight.Black, color = colors.textMain)
                        Text(subtitle, fontFamily = VazirmatnFamily, fontSize = 11.sp, color = colors.textMuted)
                    }
                }
            }
        }) { padding ->
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (type != ReportingBreakdownType.GOLD_INVENTORY && type != ReportingBreakdownType.DEBTORS_CREDITORS) {
                    DetailPeriodChips(uiState, onSelectPeriod, onOpenCustomDateDialog)
                }
                when (type) {
                    ReportingBreakdownType.SALES_PERFORMANCE -> SalesReportBody(uiState)
                    ReportingBreakdownType.GOLD_INVENTORY -> InventoryReportBody(uiState, onNavigateInventory)
                    ReportingBreakdownType.DEBTORS_CREDITORS -> DebtorsReportBody(uiState, onNavigateCustomerLedger)
                    ReportingBreakdownType.VAT_REPORT -> VatReportBody(uiState)
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun DetailPeriodChips(
    state: ReportingUiState,
    onSelectPeriod: (ReportingPeriod) -> Unit,
    onOpenCustomDateDialog: () -> Unit
) {
    val colors = LocalGoldExColors.current
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        ReportingPeriod.entries.forEach { period ->
            val selected = state.selectedPeriod == period
            Surface(
                shape = ButtonShape,
                color = if (selected) colors.textMain else colors.surface,
                border = BorderStroke(0.7.dp, if (selected) colors.goldBorder else colors.border),
                modifier = Modifier.clickable {
                    if (period == ReportingPeriod.CUSTOM) onOpenCustomDateDialog() else onSelectPeriod(period)
                }
            ) {
                Text(
                    text = if (period == ReportingPeriod.CUSTOM && state.customStartDateShamsi.isNotBlank())
                        "${PersianNumberFormatter.toPersianDigits(state.customStartDateShamsi)} تا ${PersianNumberFormatter.toPersianDigits(state.customEndDateShamsi)}"
                    else period.labelFa,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    fontFamily = VazirmatnFamily,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 11.sp,
                    color = if (selected) colors.surface else colors.textSecondary
                )
            }
        }
    }
}

@Composable
private fun ReportCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    val colors = LocalGoldExColors.current
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = colors.surface,
        border = BorderStroke(0.7.dp, colors.border),
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(title, fontFamily = VazirmatnFamily, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = colors.textMain)
            content()
        }
    }
}

@Composable
private fun ReportHero(label: String, value: String, unit: String, note: String) {
    val colors = LocalGoldExColors.current
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = colors.textMain,
        border = BorderStroke(1.dp, colors.goldPrimary),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Text(label, fontFamily = VazirmatnFamily, fontSize = 12.sp, color = colors.goldSecondary)
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(value, fontFamily = VazirmatnFamily, fontSize = 27.sp, fontWeight = FontWeight.Black, color = colors.surface)
                Text(unit, fontFamily = VazirmatnFamily, fontSize = 12.sp, color = colors.surface, modifier = Modifier.padding(bottom = 5.dp))
            }
            Text(note, fontFamily = VazirmatnFamily, fontSize = 11.sp, color = colors.surface.copy(alpha = 0.75f))
        }
    }
}

@Composable
private fun ReportLine(label: String, value: String, emphasis: Boolean = false) {
    val colors = LocalGoldExColors.current
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, fontFamily = VazirmatnFamily, fontSize = 12.sp, color = colors.textSecondary, modifier = Modifier.weight(1f))
        Text(value, fontFamily = VazirmatnFamily, fontSize = 12.sp, fontWeight = FontWeight.Bold,
            color = if (emphasis) colors.goldPrimary else colors.textMain)
    }
}

@Composable
private fun EmptyReport(text: String) {
    val colors = LocalGoldExColors.current
    Text(text, fontFamily = VazirmatnFamily, fontSize = 12.sp, color = colors.textMuted,
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp))
}

private fun toman(value: Long): String = "${PersianNumberFormatter.formatWithSeparators(value)} تومان"
private fun grams(value: Double): String = "${PersianNumberFormatter.formatWeight(value)} گرم ۱۸"

@Composable
private fun SalesReportBody(state: ReportingUiState) {
    val sales = state.salesPerformance
    val colors = LocalGoldExColors.current
    ReportHero("اجرت و سود ثبت‌شده ${state.selectedPeriod.labelFa}",
        toman(sales.totalWageTomans + sales.totalProfitTomans).removeSuffix(" تومان"), "تومان",
        "سود خالص عملیاتی نیازمند ثبت هزینه‌های فروشگاه است")
    ReportCard("خلاصه عملکرد فروش") {
        ReportLine("کل فروش", toman(sales.grossSalesTomans))
        ReportLine("ارزش پایه اقلام فروش", toman(sales.rawGoldValueTomans))
        ReportLine("اجرت ساخت", toman(sales.totalWageTomans), true)
        ReportLine("سود ثبت‌شده", toman(sales.totalProfitTomans), true)
        ReportLine("مالیات وصول‌شده", toman(sales.totalTaxTomans))
        ReportLine("وزن ۱۸ عیار فروخته‌شده", grams(sales.totalWeight18k))
        ReportLine("تعداد اقلام", PersianNumberFormatter.toPersianDigits(sales.itemsCount.toString()))
    }
    ReportCard("روند اجرت و سود در چهار بخش دوره") {
        val buckets = state.details.profitBucketsTomans
        val max = buckets.maxOrNull()?.coerceAtLeast(1L) ?: 1L
        Row(Modifier.fillMaxWidth().height(110.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Bottom) {
            buckets.forEachIndexed { index, amount ->
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Box(Modifier.fillMaxWidth().height((8 + (amount.toFloat() / max * 74)).dp)
                        .background(colors.goldSecondary, RoundedCornerShape(5.dp)))
                    Text(PersianNumberFormatter.toPersianDigits((index + 1).toString()), fontFamily = VazirmatnFamily,
                        fontSize = 10.sp, color = colors.textMuted)
                }
            }
        }
    }
    ReportCard("تفکیک فروش بر اساس سبد کالایی") {
        if (state.details.salesCategories.isEmpty()) EmptyReport("فروشی در این دوره ثبت نشده است")
        state.details.salesCategories.forEach { row ->
            ReportLine(row.title, toman(row.salesTomans))
            Text("اجرت و سود: ${toman(row.wageTomans + row.profitTomans)}", fontFamily = VazirmatnFamily,
                fontSize = 10.sp, color = colors.textMuted)
        }
    }
    ReportCard("اقلام ساخته‌شده با بیشترین اجرت و سود") {
        if (state.details.topSalesItems.isEmpty()) EmptyReport("قلم طلای ساخته‌شده‌ای در این دوره ثبت نشده است")
        state.details.topSalesItems.forEach { item ->
            ReportLine(item.title, toman(item.earningsTomans), true)
            Text(grams(item.weight18k), fontFamily = VazirmatnFamily, fontSize = 10.sp, color = colors.textMuted)
        }
    }
}

@Composable
private fun InventoryReportBody(state: ReportingUiState, onNavigateInventory: () -> Unit) {
    val inventory = state.goldInventory
    val colors = LocalGoldExColors.current
    ReportHero("موجودی فعلی عیار ۷۵۰", PersianNumberFormatter.formatWeight(inventory.totalWeight18k), "گرم طلا",
        "بر پایه اقلام ثبت‌شده در انبار؛ تراز ابتدای دوره ثبت نشده است")
    ReportCard("تفکیک موجودی") {
        ReportLine("ویترین و سینی‌ها", grams(inventory.showcaseWeight18k))
        ReportLine("گاوصندوق و انبار", grams(inventory.vaultWeight18k))
        ReportLine("تعداد قطعات", PersianNumberFormatter.toPersianDigits(inventory.piecesCount.toString()))
        ReportLine("محل‌های ویترین", PersianNumberFormatter.toPersianDigits(inventory.activeTraysCount.toString()))
        ReportLine("محل‌های انبار", PersianNumberFormatter.toPersianDigits(inventory.activeSafesCount.toString()))
    }
    ReportCard("تفکیک مخازن و محل نگهداری") {
        if (state.details.inventoryLocations.isEmpty()) EmptyReport("هنوز موجودی ثبت نشده است")
        state.details.inventoryLocations.forEach { location ->
            ReportLine(location.location, grams(location.weight18k), true)
            Text("${PersianNumberFormatter.toPersianDigits(location.piecesCount.toString())} قطعه",
                fontFamily = VazirmatnFamily, fontSize = 10.sp, color = colors.textMuted)
        }
    }
    ReportCard("آخرین اسناد تغییر موجودی") {
        if (state.details.inventoryMovements.isEmpty()) EmptyReport("گردش موجودی ثبت نشده است")
        state.details.inventoryMovements.forEach { movement ->
            ReportLine(movement.title, "${PersianNumberFormatter.formatWeight(movement.weightGrams)} گرم")
            Text(movement.typeLabel, fontFamily = VazirmatnFamily, fontSize = 10.sp, color = colors.textMuted)
        }
    }
    OutlinedButton(onClick = onNavigateInventory, shape = ButtonShape, modifier = Modifier.fillMaxWidth()) {
        Text("مشاهده انبار و ویترین", fontFamily = VazirmatnFamily, color = colors.goldPrimary)
    }
}

@Composable
private fun DebtorsReportBody(state: ReportingUiState, onNavigateCustomerLedger: () -> Unit) {
    val colors = LocalGoldExColors.current
    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf(0) }
    val balances = state.details.customerBalances
    val filtered = balances.filter { row ->
        val matchesFilter = when (filter) {
            1 -> row.cashDebtTomans > 0L
            2 -> row.cashDebtTomans < 0L
            3 -> row.goldDebtGrams != 0.0
            else -> true
        }
        matchesFilter && (query.isBlank() || row.name.contains(query, true) || row.role.contains(query, true))
    }
    ReportHero("طلب ریالی ثبت‌شده", PersianNumberFormatter.formatWithSeparators(state.debtorsCreditors.totalReceivablesTomans),
        "تومان", "مانده طرف‌حساب‌ها؛ نرخ بازار برای تبدیل بدهی وزنی اعمال نشده است")
    ReportCard("تراز حساب طرفین") {
        ReportLine("طلب ریالی ما", toman(state.debtorsCreditors.totalReceivablesTomans), true)
        ReportLine("بدهی ریالی ما", toman(state.debtorsCreditors.totalPayablesTomans))
        ReportLine("مانده وزنی خالص", grams(state.debtorsCreditors.goldDebtGrams))
        ReportLine("طرف‌حساب بدهکار", PersianNumberFormatter.toPersianDigits(state.debtorsCreditors.debtorCount.toString()))
        ReportLine("طرف‌حساب بستانکار", PersianNumberFormatter.toPersianDigits(state.debtorsCreditors.creditorCount.toString()))
    }
    OutlinedTextField(
        value = query, onValueChange = { query = it }, label = { Text("جستجوی نام یا نقش طرف‌حساب", fontFamily = VazirmatnFamily) },
        singleLine = true, shape = ButtonShape, modifier = Modifier.fillMaxWidth()
    )
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        listOf("همه", "بدهکار ریالی", "بستانکار ریالی", "مانده وزنی").forEachIndexed { index, label ->
            FilterChip(selected = filter == index, onClick = { filter = index }, label = {
                Text(label, fontFamily = VazirmatnFamily, fontSize = 11.sp)
            }, shape = ButtonShape)
        }
    }
    ReportCard("مانده معین طرف‌حساب‌ها") {
        if (filtered.isEmpty()) EmptyReport("طرف‌حساب منطبق با این جستجو وجود ندارد")
        filtered.forEach { row ->
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(row.name, fontFamily = VazirmatnFamily, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                    color = colors.textMain, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(row.role, fontFamily = VazirmatnFamily, fontSize = 10.sp, color = colors.textMuted)
                ReportLine(if (row.cashDebtTomans >= 0L) "بدهکار ریالی" else "بستانکار ریالی",
                    toman(abs(row.cashDebtTomans)))
                ReportLine("مانده وزنی", grams(row.goldDebtGrams))
            }
            HorizontalDivider(color = colors.border)
        }
    }
    OutlinedButton(onClick = onNavigateCustomerLedger, shape = ButtonShape, modifier = Modifier.fillMaxWidth()) {
        Text("مشاهده دفتر حساب و معین", fontFamily = VazirmatnFamily, color = colors.goldPrimary)
    }
}

@Composable
private fun VatReportBody(state: ReportingUiState) {
    val vat = state.vatReport
    val colors = LocalGoldExColors.current
    ReportHero("مالیات ارزش‌افزوده ثبت‌شده ${state.selectedPeriod.labelFa}",
        PersianNumberFormatter.formatWithSeparators(vat.totalVatCollectedTomans), "تومان",
        "جمع مالیات ثبت‌شده در اقلام طلای ساخته‌شده")
    ReportCard("مأخذ و مالیات") {
        ReportLine("اصل طلای ساخته‌شده", toman(vat.taxExemptRawGoldTomans))
        ReportLine("مأخذ مشمول؛ اجرت و سود", toman(vat.taxableBaseTomans), true)
        ReportLine("مالیات ثبت‌شده", toman(vat.totalVatCollectedTomans), true)
        ReportLine("نرخ پیش‌فرض تنظیمات", "${PersianNumberFormatter.toPersianDigits(vat.vatPercent.toString())}٪")
        Text("مالیات هر فاکتور از مبلغ ثبت‌شده همان فاکتور خوانده می‌شود. نرخ پیش‌فرض بالا لزوماً نرخ همه فاکتورها نیست.",
            fontFamily = VazirmatnFamily, fontSize = 11.sp, color = colors.textMuted)
    }
    ReportCard("فرمول محاسبه طلا") {
        Text("مأخذ مالیات = اجرت ساخت + سود فروشنده؛ اصل طلای خام در این محاسبه وارد نمی‌شود.",
            fontFamily = VazirmatnFamily, fontSize = 12.sp, color = colors.textSecondary)
    }
    ReportCard("تفکیک اقلام فروش ثبت‌شده") {
        if (state.details.salesCategories.isEmpty()) EmptyReport("فروشی در این دوره ثبت نشده است")
        state.details.salesCategories.forEach { row ->
            ReportLine(row.title, toman(row.salesTomans))
            ReportLine("اجرت و سود", toman(row.wageTomans + row.profitTomans))
            ReportLine("مالیات ثبت‌شده", toman(row.vatTomans), true)
            HorizontalDivider(color = colors.border)
        }
        Text("این گزارش بر اساس داده‌های موجود است و جایگزین اظهارنامه یا تأیید سازمان مالیاتی نیست.",
            fontFamily = VazirmatnFamily, fontSize = 10.sp, color = colors.textMuted)
    }
}
