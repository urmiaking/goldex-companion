package com.goldex.companion.desktop.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import com.goldex.companion.desktop.data.*
import com.goldex.companion.desktop.state.*
import com.goldex.companion.model.TimeHorizon
import com.goldex.companion.model.PersianNumberFormatter
import com.goldex.companion.ui.components.*
import com.goldex.companion.ui.theme.*
import kotlin.math.roundToInt

@Composable internal fun DesktopDashboardPage(state: WorkspaceState, dashboard: DesktopDashboard, workspace: DesktopWorkspace) {
    val chart by dashboard.state.collectAsState()
    val colors = LocalGoldExColors.current
    PageScroll {
        Row(Modifier.fillMaxWidth().testTag("dashboard-greeting"), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.size(48.dp).background(colors.goldContainer, CircleShape).border(0.6.dp, colors.goldBorder, CircleShape), contentAlignment = Alignment.Center) {
                Text(state.settings.managerName.take(1).ifBlank { "ق" }, color = colors.goldPrimary, fontWeight = FontWeight.Bold, fontSize = 21.sp)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("${DesktopDashboard.greeting(state.now)}، ${state.settings.managerName.ifBlank { "استاد زرگر" }}", color = colors.textMain, fontWeight = FontWeight.Bold, fontSize = 21.sp)
                Text(state.settings.galleryName.ifBlank { "به پیشخوان قیراط خوش آمدید" }, color = colors.textMuted, fontSize = 13.sp)
            }
            Text(DesktopPortfolioPolicy.observedTime(state.now).substringBefore(" •"), color = colors.textMuted, fontSize = 12.sp)
        }
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            if (maxWidth >= 740.dp) Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                Box(Modifier.weight(1.05f)) { AssetVault(state, workspace) }
                Box(Modifier.weight(1f)) { DashboardMarket(state, workspace) }
            } else Column(verticalArrangement = Arrangement.spacedBy(20.dp)) { AssetVault(state, workspace); DashboardMarket(state, workspace) }
        }
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            PageTitle("دسترسی‌های سریع")
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val actions: List<Triple<String, androidx.compose.ui.graphics.vector.ImageVector, () -> Unit>> = listOf(
                    Triple("ثبت دارایی", Icons.Outlined.Add, { workspace.navigate(DesktopDestination.PORTFOLIO); workspace.openAsset() }),
                    Triple("محاسبه طلا", Icons.Outlined.Calculate, { workspace.navigate(DesktopDestination.CALCULATOR) }),
                    Triple("تابلوی نرخ‌ها", Icons.Outlined.ShowChart, { workspace.navigate(DesktopDestination.RATES) }),
                    Triple("انبار و کالاها", Icons.Outlined.Inventory2, { workspace.navigate(DesktopDestination.INVENTORY) }))
                val rows = if (maxWidth >= 740.dp) listOf(actions) else actions.chunked(2)
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    rows.forEach { row -> Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        row.forEachIndexed { index, action -> GoldButton(action.first, action.third, Modifier.weight(1f).testTag("quick-${action.first}"), icon = action.second, isSecondary = index != 0 || row != rows.first()) }
                    } }
                }
            }
        }
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            if (maxWidth >= 950.dp) Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                Box(Modifier.weight(1.45f)) { DashboardTrend(chart, state.now, state.reduceMotion, dashboard) }
                Box(Modifier.weight(1f)) { DashboardInvoices() }
            } else Column(verticalArrangement = Arrangement.spacedBy(20.dp)) { DashboardTrend(chart, state.now, state.reduceMotion, dashboard); DashboardInvoices() }
        }
        Text("دارایی‌های این رایانه مستقل از موجودی انبار و فاکتورهای گوشی هستند.", color = colors.textMuted, fontSize = 12.sp)
    }
}

@Composable private fun AssetVault(state: WorkspaceState, workspace: DesktopWorkspace) {
    val colors = LocalGoldExColors.current
    val summary = state.assets.summary
    val balance = price(summary?.currentValue)
    val balanceSize = when { (balance?.length ?: 0) > 20 -> 22; (balance?.length ?: 0) > 16 -> 26; else -> 32 }
    Column(Modifier.fillMaxWidth().heightIn(min = 268.dp).testTag("dashboard-vault")
        .background(colors.dashboardVaultGradient, RoundedCornerShape(18.dp)).border(0.8.dp, colors.goldBorder.copy(alpha = 0.6f), RoundedCornerShape(18.dp)).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.AccountBalanceWallet, null, tint = colors.goldSecondary, modifier = Modifier.size(22.dp))
            Text("ارزش دارایی‌های شما", Modifier.weight(1f).padding(start = 10.dp), color = Color.White.copy(alpha = 0.85f), fontSize = 14.sp)
            IconButton(onClick = { workspace.navigate(DesktopDestination.PORTFOLIO) }, Modifier.size(40.dp)) { Icon(Icons.Outlined.ArrowBack, "مشاهدهٔ دارایی‌ها", tint = colors.goldSecondary) }
        }
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FittedAmount(balance ?: "—", Modifier.weight(1f), colors.goldSecondary, balanceSize)
            Text("تومان", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp, modifier = Modifier.padding(bottom = 5.dp))
        }
        Text(if (state.assets.rows.isEmpty()) "اولین طلا یا سکهٔ خود را ثبت کنید." else if (summary == null) "برای ارزش‌گذاری کامل، نرخ همهٔ دارایی‌ها لازم است." else "بر پایهٔ ${state.snapshot?.label(state.now)}", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
        HorizontalDivider(color = Color.White.copy(alpha = 0.15f))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            VaultMetric("طلای معادل ۱۸", PersianNumberFormatter.formatWeight(state.assets.goldWeight18), "گرم", Modifier.weight(1f))
            VaultMetric("سود / زیان", price(summary?.profit?.takeIf { state.assets.knownPurchaseBasis }) ?: "—", "تومان", Modifier.weight(1f))
        }
        Text(if (!state.assets.knownPurchaseBasis) "با ثبت مبلغ خرید، سود و زیان نمایش داده می‌شود." else "سود و زیان تحقق‌نیافتهٔ دارایی‌های ثبت‌شده", color = Color.White.copy(alpha = 0.75f), fontSize = 11.sp)
    }
}

@Composable private fun VaultMetric(title: String, value: String, unit: String, modifier: Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(title, color = Color.White.copy(alpha = 0.75f), fontSize = 12.sp)
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            FittedAmount(value, Modifier.weight(1f), Color.White, 19)
            Text(unit, color = Color.White.copy(alpha = 0.75f), fontSize = 10.sp)
        }
    }
}

@Composable private fun FittedAmount(text: String, modifier: Modifier, color: Color, maximumSize: Int) {
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val style = LocalTextStyle.current.copy(fontSize = maximumSize.sp, fontWeight = FontWeight.SemiBold)
    BoxWithConstraints(modifier) {
        val available = with(density) { maxWidth.toPx() }
        val measured = measurer.measure(text, style, maxLines = 1, softWrap = false).size.width.coerceAtLeast(1)
        val fitted = maximumSize * (available / measured).coerceAtMost(1f) * 0.96f
        AnimatedPriceTicker(text, color = color, fontSize = fitted.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable private fun DashboardMarket(state: WorkspaceState, workspace: DesktopWorkspace) {
    val colors = LocalGoldExColors.current
    val rates = state.snapshot?.rates
    LuxuryCard(Modifier.heightIn(min = 268.dp).testTag("dashboard-market"), verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            PageTitle("تابلوی مظنه‌های بازار", Modifier.weight(1f))
            TextButton(onClick = { workspace.navigate(DesktopDestination.RATES) }) { Text("همهٔ نرخ‌ها", color = colors.goldPrimary) }
        }
        QuoteRow("طلای ۱۸ عیار", rates?.gold18)
        QuoteRow("مظنهٔ آبشده", rates?.goldMelt)
        QuoteRow("سکهٔ امامی", rates?.coinEmami)
        QuoteRow("انس جهانی", rates?.ons?.toLong(), "دلار")
        HorizontalDivider(color = colors.border)
        SourceCaption(state)
    }
}

@Composable private fun DashboardTrend(state: DesktopDashboardState, now: Long, reduceMotion: Boolean, dashboard: DesktopDashboard) {
    val colors = LocalGoldExColors.current
    val active = state.active(now)
    LuxuryCard(Modifier.heightIn(min = 340.dp).testTag("dashboard-trend"), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val heading: @Composable () -> Unit = { Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                PageTitle("روند طلای ۱۸ عیار")
                Text("تاریخچهٔ بازار TGJU • تومان / گرم", color = colors.textMuted, fontSize = 12.sp)
            } }
            val selector: @Composable () -> Unit = {
                LuxurySegmentedControl(items = listOf(TimeHorizon.TODAY, TimeHorizon.ONE_WEEK, TimeHorizon.ONE_MONTH), selectedItem = state.horizon,
                    onItemSelected = { dashboard.select(it) }, label = { when(it) { TimeHorizon.TODAY -> "امروز"; TimeHorizon.ONE_WEEK -> "هفتگی"; else -> "ماهانه" } },
                    modifier = Modifier.width(212.dp).testTag("chart-horizon"), height = 36.dp, fontSize = 12.sp)
            }
            if (maxWidth >= 510.dp) Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) { heading(); selector() }
            else Column(verticalArrangement = Arrangement.spacedBy(12.dp)) { heading(); selector() }
        }
        AnimatedContent(state.horizon, transitionSpec = { fadeIn(tween(if (reduceMotion) 0 else 220)).togetherWith(fadeOut(tween(if (reduceMotion) 0 else 140))) }, label = "dashboard-history-horizon") { horizon ->
            val history = state.history[horizon]?.takeIf { it.belongsToToday(now) }
            if (history == null) Box(Modifier.fillMaxWidth().height(194.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (state.loading) CircularProgressIndicator(Modifier.size(26.dp), colors.goldPrimary, strokeWidth = 2.dp)
                    else Icon(Icons.Outlined.ShowChart, null, tint = colors.textMuted, modifier = Modifier.size(32.dp))
                    Text(if (state.loading) "دریافت تاریخچهٔ بازار…" else "تاریخچهٔ این بازه در دسترس نیست", color = colors.textSecondary, fontSize = 13.sp)
                }
            } else HistoryChart(history)
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Column(Modifier.weight(1f)) {
                Text(if (active == null) "نرخ دستی جایگزین تاریخچهٔ بازار نمی‌شود." else "دریافت: ${DesktopPortfolioPolicy.observedTime(active.receivedAt)}", color = colors.textMuted, fontSize = 11.sp)
                if (state.error != null) Text(state.error, color = colors.errorRed, fontSize = 12.sp, modifier = Modifier.testTag("history-error"))
            }
            TextButton(onClick = { dashboard.select(state.horizon, force = true) }, enabled = !state.loading, modifier = Modifier.testTag("refresh-history")) {
                Text(if (state.loading) "در حال دریافت" else "دریافت تازه", color = colors.goldPrimary)
            }
        }
    }
}

@Composable private fun HistoryChart(history: GoldHistorySnapshot) {
    val colors = LocalGoldExColors.current
    val points = history.points
    var selected by remember(history) { mutableStateOf(points.lastIndex) }
    val point = points[selected]
    val low = history.low; val high = history.high
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Amount(price(point.price), Modifier.weight(1f), size = 24)
            Text(PersianNumberFormatter.toPersianDigits(point.label), color = colors.textSecondary, fontSize = 12.sp)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(if (history.change >= 0) Icons.Outlined.TrendingUp else Icons.Outlined.TrendingDown, null, Modifier.size(16.dp), tint = if (history.change >= 0) colors.marketGainText else colors.errorRed)
            Amount(PersianNumberFormatter.formatDelta(history.change, history.changePercent), color = if (history.change >= 0) colors.marketGainText else colors.errorRed, size = 12)
            Text("نسبت به ابتدای داده‌ها", color = colors.textMuted, fontSize = 11.sp)
        }
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Canvas(Modifier.fillMaxWidth().height(134.dp).testTag("history-canvas").clipToBounds()
                .semantics { contentDescription = "نمودار طلای ۱۸ عیار؛ کمینه ${PersianNumberFormatter.formatPrice(low)}، بیشینه ${PersianNumberFormatter.formatPrice(high)} تومان" }
                .pointerInput(history) { detectTapGestures { offset ->
                    val time = points.first().at + ((offset.x / size.width).coerceIn(0f, 1f) * (points.last().at - points.first().at)).toLong()
                    selected = points.indices.minByOrNull { kotlin.math.abs(points[it].at - time) } ?: 0
                } }) {
                val inset = 8.dp.toPx(); val width = size.width - inset * 2; val height = size.height - inset * 2
                fun position(p: GoldHistoryPoint) = Offset(inset + (p.at - points.first().at).toDouble().div((points.last().at - points.first().at).coerceAtLeast(1)).toFloat() * width,
                    inset + height * (if (high == low) 0.5f else 1f - (p.price - low).toDouble().div(high - low).toFloat()))
                repeat(4) { line -> val y = inset + height * line / 3; drawLine(colors.border.copy(alpha = 0.6f), Offset(inset, y), Offset(size.width - inset, y), 1.dp.toPx()) }
                val path = Path(); history.renderedPoints.forEachIndexed { index, p -> val pos = position(p); if (index == 0) path.moveTo(pos.x, pos.y) else path.lineTo(pos.x, pos.y) }
                val fill = Path().apply { addPath(path); lineTo(size.width - inset, size.height - inset); lineTo(inset, size.height - inset); close() }
                drawPath(fill, Brush.verticalGradient(listOf(colors.goldPrimary.copy(alpha = 0.2f), Color.Transparent)))
                drawPath(path, colors.goldPrimary, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
                val marker = position(point)
                drawLine(colors.goldPrimary.copy(alpha = 0.4f), Offset(marker.x, inset), Offset(marker.x, size.height - inset), 1.dp.toPx())
                drawCircle(colors.surface, 5.dp.toPx(), marker); drawCircle(colors.goldPrimary, 3.dp.toPx(), marker)
            }
            Slider(selected.toFloat(), { selected = it.roundToInt().coerceIn(points.indices) }, valueRange = 0f..points.lastIndex.toFloat(),
                modifier = Modifier.height(24.dp).testTag("history-point").semantics { contentDescription = "انتخاب زمان نمودار" }, colors = SliderDefaults.colors(thumbColor = colors.goldPrimary, activeTrackColor = colors.goldPrimary))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("کمینه ${PersianNumberFormatter.formatPrice(low)}", color = colors.textMuted, fontSize = 11.sp)
            Text("بیشینه ${PersianNumberFormatter.formatPrice(high)}", color = colors.textMuted, fontSize = 11.sp)
        }
    }
}

/** Windows invoice creation/sync is not installed yet. Do not claim the phone has no invoices. */
@Composable private fun DashboardInvoices() {
    val colors = LocalGoldExColors.current
    LuxuryCard(Modifier.heightIn(min = 340.dp).testTag("dashboard-invoices")) {
        PageTitle("آخرین فاکتورهای ثبت‌شده")
        HorizontalDivider(color = colors.border)
        Column(Modifier.fillMaxWidth().heightIn(min = 208.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically)) {
            Box(Modifier.size(56.dp).background(colors.goldContainer, RoundedCornerShape(16.dp)), contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.ReceiptLong, null, tint = colors.goldPrimary, modifier = Modifier.size(28.dp))
            }
            Text("فاکتورهای ویندوز هنوز فعال نیست", color = colors.textMain, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text("پس از راه‌اندازی بخش فاکتورها، آخرین ثبت‌ها اینجا نمایش داده می‌شوند. فاکتورهای گوشی به این نسخه منتقل نشده‌اند.", color = colors.textMuted, fontSize = 12.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }
    }
}
