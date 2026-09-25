package com.goldex.companion.ui.reporting

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.goldex.companion.domain.reporting.*
import com.goldex.companion.model.PersianNumberFormatter
import com.goldex.companion.ui.components.AnimatedPriceTicker
import com.goldex.companion.ui.components.LuxuryCard
import com.goldex.companion.ui.theme.ButtonShape
import com.goldex.companion.ui.theme.LocalGoldExColors
import com.goldex.companion.ui.theme.LuxuryMotion
import com.goldex.companion.ui.theme.VazirmatnFamily
import com.goldex.companion.ui.theme.heroCardGradient
import com.goldex.companion.ui.theme.goldButtonContainer
import com.goldex.companion.ui.theme.goldButtonText
import com.goldex.companion.ui.theme.specularHairlineBrush
import kotlin.math.abs

/** Full Stitch report pages, backed only by locally recorded figures. */
@Composable
fun ReportingDetailScreen(
    type: ReportingBreakdownType,
    uiState: ReportingUiState,
    onBack: () -> Unit,
    onSelectPeriod: (ReportingPeriod) -> Unit,
    onOpenCustomDateDialog: () -> Unit,
    onNavigateInventory: () -> Unit,
    onNavigateCustomerLedger: () -> Unit,
    onShareReport: () -> Unit
) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        val colors = LocalGoldExColors.current
        val title = when (type) {
            ReportingBreakdownType.SALES_PERFORMANCE -> "سود و زیان و عملکرد فروش"
            ReportingBreakdownType.GOLD_INVENTORY -> "تراز وزنی و انبار طلا"
            ReportingBreakdownType.DEBTORS_CREDITORS -> "بدهکاران و مانده معین"
            ReportingBreakdownType.VAT_REPORT -> "ارزش‌افزوده و مالیات فصلی"
        }
        val subtitle = when (type) {
            ReportingBreakdownType.SALES_PERFORMANCE -> "تحلیل معاملات و حاشیه فروش ثبت‌شده"
            ReportingBreakdownType.GOLD_INVENTORY -> "موجودی فعلی و گردش ثبت‌شده طلا"
            ReportingBreakdownType.DEBTORS_CREDITORS -> "مانده حساب طرف‌حساب‌های زرگری"
            ReportingBreakdownType.VAT_REPORT -> "مالیات ثبت‌شده بر اجرت و سود"
        }
        Scaffold(
            containerColor = colors.background,
            topBar = { ReportingTopBar(title, subtitle, onBack) }
        ) { padding ->
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                ReportingPeriodFilters(uiState, onSelectPeriod, onOpenCustomDateDialog)
                when (type) {
                    ReportingBreakdownType.SALES_PERFORMANCE -> SalesReportBody(uiState, onShareReport)
                    ReportingBreakdownType.GOLD_INVENTORY -> InventoryReportBody(uiState, onNavigateInventory, onShareReport)
                    ReportingBreakdownType.DEBTORS_CREDITORS -> DebtorsReportBody(uiState, onNavigateCustomerLedger, onShareReport)
                    ReportingBreakdownType.VAT_REPORT -> VatReportBody(uiState, onShareReport)
                }
                Spacer(Modifier.height(32.dp))
            }
        }
    }
}

private fun money(value: Long) = PersianNumberFormatter.formatWithSeparators(value)
private fun weight(value: Double) = PersianNumberFormatter.formatWeight(value)
private fun count(value: Int) = PersianNumberFormatter.toPersianDigits(value.toString())

@Composable
private fun ChangingText(text: String, modifier: Modifier = Modifier, color: Color, size: Int = 11,
                         weight: FontWeight = FontWeight.Normal) {
    AnimatedContent(targetState = text,
        transitionSpec = { LuxuryMotion.numberSlideSpec(isIncreasing = true) },
        label = "reportChangingText", modifier = modifier) { current ->
        Text(current, color = color, fontFamily = VazirmatnFamily, fontSize = size.sp,
            fontWeight = weight, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun Ticker(value: String, color: Color, size: Int, modifier: Modifier = Modifier,
                   weight: FontWeight = FontWeight.Bold) {
    AnimatedPriceTicker(value, modifier = modifier, color = color, fontSize = size.sp,
        fontWeight = weight, overflow = TextOverflow.Ellipsis,
        style = TextStyle(fontFamily = VazirmatnFamily))
}

@Composable
private fun ReportHero(
    icon: ImageVector,
    eyebrow: String,
    title: String,
    period: String,
    value: String,
    unit: String,
    note: String,
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = LocalGoldExColors.current
    Surface(
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(0.8.dp, colors.goldBorder),
        shadowElevation = if (colors.isDark) 0.dp else 10.dp,
        color = Color(0xFF141B2B),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(Modifier.background(colors.heroCardGradient)) {
            Box(Modifier.fillMaxWidth().height(2.dp).background(colors.specularHairlineBrush))
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(11.dp)) {
                    Box(Modifier.size(42.dp).clip(RoundedCornerShape(12.dp))
                        .background(colors.goldSecondary.copy(alpha = 0.18f))
                        .border(0.8.dp, colors.goldSecondary.copy(alpha = 0.44f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center) {
                        Icon(icon, contentDescription = null, tint = colors.goldSecondary,
                            modifier = Modifier.size(23.dp))
                    }
                    Column(Modifier.weight(1f)) {
                        ChangingText(eyebrow, color = Color(0xFFCBD5E1), size = 11)
                        Text(title, color = Color.White, fontFamily = VazirmatnFamily,
                            fontSize = 15.sp, fontWeight = FontWeight.Black, maxLines = 1,
                            overflow = TextOverflow.Ellipsis)
                    }
                    Surface(shape = RoundedCornerShape(20.dp),
                        color = colors.goldSecondary.copy(alpha = 0.13f),
                        border = BorderStroke(0.7.dp, colors.goldSecondary.copy(alpha = 0.37f))) {
                        ChangingText(period, modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                            color = colors.goldSecondary, size = 10, weight = FontWeight.Bold)
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Ticker(value, color = Color.White, size = 27,
                            modifier = Modifier.weight(1f, fill = false), weight = FontWeight.Black)
                        Text(unit, color = colors.goldSecondary, fontFamily = VazirmatnFamily,
                            fontSize = 11.sp, modifier = Modifier.padding(bottom = 5.dp))
                    }
                    ChangingText(note, color = Color(0xFFCBD5E1), size = 10)
                }
                content()
            }
        }
    }
}

@Composable
private fun HeroTile(label: String, value: String, unit: String, icon: ImageVector,
                     modifier: Modifier = Modifier, accent: Color? = null) {
    val colors = LocalGoldExColors.current
    Surface(modifier = modifier, shape = RoundedCornerShape(12.dp),
        color = Color.White.copy(alpha = 0.055f),
        border = BorderStroke(0.6.dp, Color.White.copy(alpha = 0.11f))) {
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                Icon(icon, contentDescription = null, tint = accent ?: colors.goldSecondary,
                    modifier = Modifier.size(13.dp))
                Text(label, color = Color(0xFFCBD5E1), fontFamily = VazirmatnFamily,
                    fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                Ticker(value, color = accent ?: Color.White, size = 14,
                    modifier = Modifier.weight(1f, fill = false))
                Text(unit, color = Color(0xFF94A3B8), fontFamily = VazirmatnFamily,
                    fontSize = 9.sp, maxLines = 1)
            }
        }
    }
}

@Composable
private fun HeroGrid(first: @Composable RowScope.() -> Unit, second: @Composable RowScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), content = first)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), content = second)
    }
}

@Composable
private fun SectionTitle(title: String, caption: String = "") {
    val colors = LocalGoldExColors.current
    Row(Modifier.fillMaxWidth().padding(horizontal = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(title, fontFamily = VazirmatnFamily, fontSize = 13.sp, fontWeight = FontWeight.Bold,
            color = colors.textMain)
        if (caption.isNotBlank()) ChangingText(caption, color = colors.textMuted, size = 10)
    }
}

@Composable
private fun LedgerRow(icon: ImageVector, title: String, subtitle: String, value: String,
                      unit: String = "", emphasize: Boolean = false) {
    val colors = LocalGoldExColors.current
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(Modifier.size(37.dp).clip(RoundedCornerShape(10.dp))
            .background(colors.goldContainer), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = colors.goldPrimary, modifier = Modifier.size(19.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(title, fontFamily = VazirmatnFamily, fontSize = 12.sp,
                fontWeight = FontWeight.Bold, color = colors.textMain, maxLines = 1,
                overflow = TextOverflow.Ellipsis)
            if (subtitle.isNotBlank()) ChangingText(subtitle, color = colors.textMuted, size = 10)
        }
        Column(horizontalAlignment = Alignment.End) {
            Ticker(value, color = if (emphasize) colors.goldPrimary else colors.textMain, size = 12)
            if (unit.isNotBlank()) Text(unit, color = colors.textMuted,
                fontFamily = VazirmatnFamily, fontSize = 9.sp)
        }
    }
}

@Composable
private fun EmptyLine(text: String) {
    val colors = LocalGoldExColors.current
    Text(text, modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        fontFamily = VazirmatnFamily, fontSize = 11.sp, color = colors.textMuted)
}

@Composable
private fun SalesReportBody(state: ReportingUiState, onShareReport: () -> Unit) {
    val colors = LocalGoldExColors.current
    val sales = state.salesPerformance
    val earnings = sales.totalWageTomans + sales.totalProfitTomans
    val averageItem = if (sales.itemsCount > 0) sales.grossSalesTomans / sales.itemsCount else 0L
    ReportHero(ReportingInsights, "گزارش عملکرد دوره", "اجرت و سود فروش", state.selectedPeriod.labelFa,
        money(earnings), "تومان", "جمع اجرت و سود ثبت‌شده؛ هزینه‌های عملیاتی در دسترس نیست") {
        HeroGrid(
            first = {
                HeroTile("اجرت ساخت", money(sales.totalWageTomans), "تومان", ReportingShoppingBag, Modifier.weight(1f))
                HeroTile("سود فروشنده", money(sales.totalProfitTomans), "تومان", ReportingInsights,
                    Modifier.weight(1f), colors.goldSecondary)
            },
            second = {
                HeroTile("مالیات ثبت‌شده", money(sales.totalTaxTomans), "تومان", ReportingReceipt,
                    Modifier.weight(1f), colors.errorRed)
                HeroTile("میانگین هر قلم", money(averageItem), "تومان", ReportingPointOfSale,
                    Modifier.weight(1f), colors.profitGreen)
            }
        )
    }
    LuxuryCard(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionTitle("روند اجرت و سود", "چهار بخش دوره")
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            ChartLegend("دوره جاری", colors.goldSecondary)
            ChartLegend("دوره قبل", colors.surfaceVariant)
        }
        val buckets = state.details.profitBucketsTomans
        val previous = state.details.previousProfitBucketsTomans
        val max = (buckets + previous).maxOrNull()?.coerceAtLeast(1L) ?: 1L
        Row(Modifier.fillMaxWidth().height(118.dp), horizontalArrangement = Arrangement.spacedBy(13.dp),
            verticalAlignment = Alignment.Bottom) {
            buckets.forEachIndexed { index, amount ->
                val currentFraction by animateFloatAsState((amount.toFloat() / max).coerceIn(0f, 1f),
                    label = "salesBar$index")
                val previousFraction by animateFloatAsState(((previous.getOrElse(index) { 0L }).toFloat() / max).coerceIn(0f, 1f),
                    label = "previousSalesBar$index")
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Row(Modifier.fillMaxWidth().height(86.dp),
                        horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.Bottom) {
                        Box(Modifier.weight(1f).height((8 + previousFraction * 74).dp)
                            .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                            .background(colors.surfaceVariant))
                        Box(Modifier.weight(1f).height((8 + currentFraction * 74).dp)
                            .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                            .background(colors.goldSecondary))
                    }
                    Text("بخش ${count(index + 1)}", fontFamily = VazirmatnFamily, fontSize = 10.sp,
                        color = colors.textMuted)
                }
            }
        }
    }
    SectionTitle("تفکیک سود بر اساس سبد کالایی")
    LuxuryCard(verticalArrangement = Arrangement.spacedBy(13.dp)) {
        val categories = state.details.salesCategories
        if (categories.isEmpty()) EmptyLine("فروشی در این دوره ثبت نشده است")
        val total = categories.sumOf { it.wageTomans + it.profitTomans }.coerceAtLeast(1L)
        Row(Modifier.fillMaxWidth().height(6.dp).clip(CircleShape).background(colors.surfaceElevated)) {
            categories.forEachIndexed { index, row ->
                val share = ((row.wageTomans + row.profitTomans).toFloat() / total).coerceAtLeast(0f)
                if (share > 0f) Box(Modifier.weight(share).fillMaxHeight()
                    .background(categoryColor(index, colors.goldSecondary, colors.profitGreen, colors.surfaceVariant)))
            }
        }
        categories.forEachIndexed { index, row ->
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                .background(colors.surfaceElevated).padding(horizontal = 9.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                Box(Modifier.size(7.dp).clip(CircleShape).background(
                    categoryColor(index, colors.goldSecondary, colors.profitGreen, colors.surfaceVariant)))
                Text(row.title, modifier = Modifier.weight(1f), color = colors.textMain,
                    fontFamily = VazirmatnFamily, fontSize = 11.sp, maxLines = 1)
                Ticker(money(row.wageTomans + row.profitTomans), colors.profitGreen, 10)
                Ticker("${count((((row.wageTomans + row.profitTomans).toDouble() / total) * 100).toInt())}٪",
                    colors.textSecondary, 9)
            }
        }
    }
    SectionTitle("پرفروش‌ترین و سودآورترین اقلام")
    LuxuryCard(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        val items = state.details.topSalesItems
        if (items.isEmpty()) EmptyLine("قلم ساخته‌شده‌ای در این دوره ثبت نشده است")
        items.forEachIndexed { index, item ->
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                .background(colors.surfaceElevated).padding(9.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.size(24.dp).clip(RoundedCornerShape(7.dp)).background(colors.surfaceVariant),
                    contentAlignment = Alignment.Center) {
                    Text(count(index + 1), color = colors.goldPrimary, fontFamily = VazirmatnFamily,
                        fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                Column(Modifier.weight(1f)) {
                    Text(item.title, color = colors.textMain, fontFamily = VazirmatnFamily,
                        fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1,
                        overflow = TextOverflow.Ellipsis)
                    Text("وزن ${weight(item.weight18k)} گرم ۱۸", color = colors.textMuted,
                        fontFamily = VazirmatnFamily, fontSize = 9.sp)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Ticker("+${money(item.earningsTomans)}", colors.profitGreen, 10)
                    Text("اجرت و سود", color = colors.textMuted, fontFamily = VazirmatnFamily, fontSize = 9.sp)
                }
            }
        }
    }
    ReportShareAction(onShareReport)
}

private fun categoryColor(index: Int, gold: Color, green: Color, neutral: Color): Color =
    when (index % 3) { 0 -> gold; 1 -> green; else -> neutral }

@Composable
private fun ChartLegend(title: String, color: Color) {
    val colors = LocalGoldExColors.current
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(Modifier.size(7.dp).clip(CircleShape).background(color))
        Text(title, color = colors.textMuted, fontFamily = VazirmatnFamily, fontSize = 9.sp)
    }
}

@Composable
private fun InventoryReportBody(state: ReportingUiState, onNavigateInventory: () -> Unit,
                                onShareReport: () -> Unit) {
    val colors = LocalGoldExColors.current
    val inventory = state.goldInventory
    ReportHero(ReportingScale, "تراز وزنی و انبار", "موجودی فعلی عیار ۷۵۰",
        state.selectedPeriod.labelFa, weight(inventory.totalWeight18k), "گرم طلا",
        "موجودی لحظه‌ای؛ گردش زیر مربوط به بازه انتخابی است") {
        HeroGrid(
            first = {
                HeroTile("ویترین", weight(inventory.showcaseWeight18k), "گرم ۱۸", ReportingShoppingBag, Modifier.weight(1f))
                HeroTile("گاوصندوق", weight(inventory.vaultWeight18k), "گرم ۱۸", ReportingAccountBalance, Modifier.weight(1f))
            },
            second = {
                HeroTile("ورود دوره", weight(state.details.periodInboundGrams), "گرم", ReportingArrowForward,
                    Modifier.weight(1f), colors.profitGreen)
                HeroTile("خروج دوره", weight(state.details.periodOutboundGrams), "گرم", ReportingArrowForward,
                    Modifier.weight(1f), colors.goldSecondary)
            }
        )
    }
    SectionTitle("مخازن و محل نگهداری", "${count(inventory.piecesCount)} قطعه")
    LuxuryCard(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        val locations = state.details.inventoryLocations
        if (locations.isEmpty()) EmptyLine("هنوز موجودی ثبت نشده است")
        val total = inventory.totalWeight18k.takeIf { it > 0.0 } ?: 1.0
        locations.forEachIndexed { index, location ->
            LedgerRow(ReportingAccountBalance, location.location,
                "${count(location.piecesCount)} قطعه", weight(location.weight18k), "گرم ۱۸", true)
            val fraction by animateFloatAsState((location.weight18k / total).toFloat().coerceIn(0f, 1f),
                label = "locationBar$index")
            Box(Modifier.fillMaxWidth().height(4.dp).clip(CircleShape).background(colors.surfaceElevated)) {
                Box(Modifier.fillMaxWidth(fraction).fillMaxHeight().background(colors.goldSecondary))
            }
            if (index < locations.lastIndex) HorizontalDivider(color = colors.border)
        }
    }
    SectionTitle("گردش وزنی دوره", state.selectedPeriod.labelFa)
    LuxuryCard(verticalArrangement = Arrangement.spacedBy(13.dp)) {
        val inbound = state.details.periodInboundGrams
        val outbound = state.details.periodOutboundGrams
        val total = (inbound + outbound).takeIf { it > 0.0 } ?: 1.0
        listOf(Triple("ورود", inbound, colors.profitGreen), Triple("خروج", outbound, colors.goldSecondary)).forEachIndexed { index, row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(row.first, color = colors.textSecondary, fontFamily = VazirmatnFamily, fontSize = 11.sp)
                Ticker(weight(row.second), colors.textMain, 12)
            }
            val fraction by animateFloatAsState((row.second / total).toFloat().coerceIn(0f, 1f),
                label = "movementBar$index")
            Box(Modifier.fillMaxWidth().height(8.dp).clip(CircleShape).background(colors.surfaceElevated)) {
                Box(Modifier.fillMaxWidth(fraction).fillMaxHeight().background(row.third))
            }
        }
    }
    SectionTitle("آخرین گردش‌های دوره")
    LuxuryCard(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        val movements = state.details.inventoryMovements
        if (movements.isEmpty()) EmptyLine("گردشی در بازه انتخابی ثبت نشده است")
        movements.forEachIndexed { index, movement ->
            LedgerRow(ReportingScale, movement.title, movement.typeLabel,
                weight(movement.weightGrams), "گرم")
            if (index < movements.lastIndex) HorizontalDivider(color = colors.border)
        }
    }
    OutlinedButton(onClick = onNavigateInventory, modifier = Modifier.fillMaxWidth(), shape = ButtonShape,
        border = BorderStroke(0.8.dp, colors.goldBorder)) {
        Text("مشاهده انبار و ویترین", color = colors.goldPrimary, fontFamily = VazirmatnFamily)
    }
    ReportShareAction(onShareReport)
}

@Composable
private fun DebtorsReportBody(state: ReportingUiState, onNavigateCustomerLedger: () -> Unit,
                              onShareReport: () -> Unit) {
    val colors = LocalGoldExColors.current
    var query by remember { mutableStateOf("") }
    var filter by remember { mutableIntStateOf(0) }
    val balances = state.details.customerBalances
    val filtered = balances.filter { row ->
        val matches = when (filter) {
            1 -> row.cashDebtTomans > 0L
            2 -> row.cashDebtTomans < 0L
            3 -> row.goldDebtGrams != 0.0
            else -> true
        }
        matches && (query.isBlank() || row.name.contains(query, true) || row.role.contains(query, true))
    }
    val debtors = state.debtorsCreditors
    ReportHero(ReportingContacts, "دفتر معین طرف‌حساب‌ها", "مطالبات ثبت‌شده",
        state.selectedPeriod.labelFa, money(debtors.totalReceivablesTomans), "تومان",
        "مانده فعلی حساب‌ها؛ تاریخچه تغییر مانده در دسترس نیست") {
        HeroGrid(
            first = {
                HeroTile("طلب ریالی", money(debtors.totalReceivablesTomans), "تومان", ReportingContacts,
                    Modifier.weight(1f), colors.profitGreen)
                HeroTile("بدهی ریالی", money(debtors.totalPayablesTomans), "تومان", ReportingAccountBalance,
                    Modifier.weight(1f), colors.goldSecondary)
            },
            second = {
                HeroTile("مانده وزنی", weight(debtors.goldDebtGrams), "گرم ۱۸", ReportingScale, Modifier.weight(1f))
                HeroTile("طرف‌حساب فعال", count(balances.size), "حساب", ReportingContacts, Modifier.weight(1f))
            }
        )
    }
    SectionTitle("مانده معین طرف‌حساب‌ها", "${count(filtered.size)} حساب")
    OutlinedTextField(
        value = query, onValueChange = { query = it }, singleLine = true,
        placeholder = { Text("جستجوی نام یا نقش طرف‌حساب", fontFamily = VazirmatnFamily, fontSize = 11.sp) },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = colors.goldPrimary) },
        shape = ButtonShape, modifier = Modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = colors.goldPrimary,
            unfocusedBorderColor = colors.goldBorder, focusedContainerColor = colors.surface,
            unfocusedContainerColor = colors.surface)
    )
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        listOf("همه", "بدهکار", "بستانکار", "مانده وزنی").forEachIndexed { index, label ->
            FilterChip(selected = filter == index, onClick = { filter = index },
                label = { Text(label, fontFamily = VazirmatnFamily, fontSize = 11.sp) },
                shape = RoundedCornerShape(20.dp),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = colors.textMain, selectedLabelColor = colors.surface))
        }
    }
    if (filtered.isEmpty()) LuxuryCard { EmptyLine("طرف‌حساب منطبق با این جستجو وجود ندارد") }
    filtered.forEach { row ->
        LuxuryCard(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(Modifier.size(40.dp).clip(CircleShape).background(colors.goldContainer),
                    contentAlignment = Alignment.Center) {
                    Text(row.name.take(1), color = colors.goldPrimary, fontFamily = VazirmatnFamily,
                        fontSize = 17.sp, fontWeight = FontWeight.Bold)
                }
                Column(Modifier.weight(1f)) {
                    Text(row.name, fontFamily = VazirmatnFamily, color = colors.textMain,
                        fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1,
                        overflow = TextOverflow.Ellipsis)
                    Text(row.role, fontFamily = VazirmatnFamily, color = colors.textMuted, fontSize = 10.sp)
                }
                Text(if (row.cashDebtTomans > 0L) "بدهکار" else if (row.cashDebtTomans < 0L) "بستانکار" else "وزنی",
                    color = colors.goldPrimary, fontFamily = VazirmatnFamily, fontSize = 10.sp)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BalanceTile("مانده ریالی", money(abs(row.cashDebtTomans)), "تومان", Modifier.weight(1f))
                BalanceTile("مانده وزنی", weight(row.goldDebtGrams), "گرم ۱۸", Modifier.weight(1f))
            }
            TextButton(onClick = onNavigateCustomerLedger, modifier = Modifier.align(Alignment.End)) {
                Text("مشاهده دفتر معین", color = colors.goldPrimary,
                    fontFamily = VazirmatnFamily, fontSize = 11.sp)
            }
        }
    }
    OutlinedButton(onClick = onNavigateCustomerLedger, modifier = Modifier.fillMaxWidth(), shape = ButtonShape,
        border = BorderStroke(0.8.dp, colors.goldBorder)) {
        Text("مشاهده دفتر حساب مشتریان", color = colors.goldPrimary, fontFamily = VazirmatnFamily)
    }
    ReportShareAction(onShareReport)
}

@Composable
private fun BalanceTile(label: String, value: String, unit: String, modifier: Modifier = Modifier) {
    val colors = LocalGoldExColors.current
    Column(modifier.clip(RoundedCornerShape(10.dp)).background(colors.surfaceElevated).padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(label, color = colors.textMuted, fontFamily = VazirmatnFamily, fontSize = 10.sp)
        Ticker(value, colors.textMain, 13)
        Text(unit, color = colors.textMuted, fontFamily = VazirmatnFamily, fontSize = 9.sp)
    }
}

@Composable
private fun VatReportBody(state: ReportingUiState, onShareReport: () -> Unit) {
    val colors = LocalGoldExColors.current
    val vat = state.vatReport
    val sales = state.salesPerformance
    ReportHero(ReportingReceipt, "گزارش مالیات طلا", "مالیات ارزش‌افزوده ثبت‌شده",
        state.selectedPeriod.labelFa, money(vat.totalVatCollectedTomans), "تومان",
        "جمع مالیات ثبت‌شده در فاکتورهای بازه انتخابی") {
        HeroGrid(
            first = {
                HeroTile("اصل طلای معاف", money(vat.taxExemptRawGoldTomans), "تومان",
                    ReportingScale, Modifier.weight(1f))
                HeroTile("اجرت مشمول", money(sales.totalWageTomans), "تومان",
                    ReportingShoppingBag, Modifier.weight(1f))
            },
            second = {
                HeroTile("سود مشمول", money(sales.totalProfitTomans), "تومان",
                    ReportingInsights, Modifier.weight(1f))
                HeroTile("مأخذ مشمول", money(vat.taxableBaseTomans), "تومان",
                    ReportingReceipt, Modifier.weight(1f), colors.goldSecondary)
            }
        )
    }
    LuxuryCard(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            Icon(ReportingReceipt, contentDescription = null, tint = colors.goldPrimary,
                modifier = Modifier.size(21.dp))
            Text("مبنای محاسبه ارزش‌افزوده", color = colors.textMain,
                fontFamily = VazirmatnFamily, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }
        Text("مالیات فقط بر اجرت ساخت و سود فروشنده اعمال می‌شود. اصل طلای خام در مأخذ مالیات نیست.",
            color = colors.textSecondary, fontFamily = VazirmatnFamily, fontSize = 11.sp)
        ChangingText("نرخ پیش‌فرض تنظیمات: ${PersianNumberFormatter.toPersianDigits(vat.vatPercent.toString())}٪",
            color = colors.goldPrimary, size = 10)
    }
    SectionTitle("تفکیک اقلام فروش", state.selectedPeriod.labelFa)
    LuxuryCard(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        val rows = state.details.salesCategories
        if (rows.isEmpty()) EmptyLine("فروشی در این دوره ثبت نشده است")
        rows.forEachIndexed { index, row ->
            LedgerRow(ReportingShoppingBag, row.title,
                "اجرت و سود ${money(row.wageTomans + row.profitTomans)} تومان",
                money(row.vatTomans), "مالیات، تومان", true)
            if (index < rows.lastIndex) HorizontalDivider(color = colors.border)
        }
    }
    Text("مبلغ مالیات از فاکتورهای ثبت‌شده خوانده می‌شود؛ نرخ پیش‌فرض ممکن است با نرخ هر فاکتور متفاوت باشد.",
        color = colors.textMuted, fontFamily = VazirmatnFamily, fontSize = 10.sp,
        modifier = Modifier.padding(horizontal = 4.dp))
    ReportShareAction(onShareReport)
}

@Composable
private fun ReportShareAction(onClick: () -> Unit) {
    val colors = LocalGoldExColors.current
    Button(onClick = onClick, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
        shape = ButtonShape,
        colors = ButtonDefaults.buttonColors(containerColor = colors.goldButtonContainer,
            contentColor = colors.goldButtonText)) {
        Icon(ReportingReceipt, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(7.dp))
        Text("اشتراک خلاصه گزارش", fontFamily = VazirmatnFamily, fontWeight = FontWeight.Bold,
            fontSize = 12.sp)
    }
}
