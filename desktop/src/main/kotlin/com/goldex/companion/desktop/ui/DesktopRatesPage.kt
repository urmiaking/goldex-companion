package com.goldex.companion.desktop.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import com.goldex.companion.desktop.data.*
import com.goldex.companion.desktop.state.*
import com.goldex.companion.model.*
import com.goldex.companion.ui.components.*
import com.goldex.companion.ui.theme.*
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToLong

@Composable internal fun RatesPage(state: WorkspaceState, workspace: DesktopWorkspace) {
    val board by workspace.ratesBoard.state.collectAsState()
    var manual by remember { mutableStateOf(false) }
    var detail by remember { mutableStateOf<BoardInstrument?>(null) }
    var filter by rememberSaveable { mutableStateOf(0) }
    val rows = BoardInstrument.values().map { DesktopRatesBoardPolicy.row(it,state.snapshot,board,state.now) }
    DisposableEffect(workspace.ratesBoard, state.settings.autoSyncRates) {
        val subscription = workspace.ratesBoard.observe(state.settings.autoSyncRates)
        onDispose { subscription.cancel() }
    }
    PageScroll {
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val compact = maxWidth < 940.dp
            val columns = if (maxWidth < 800.dp) 2 else 4
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                RatesWelcome(state, board, compact, onRefresh = { workspace.refreshRates(); workspace.ratesBoard.refresh(true) }, onManual = { manual = true })
                val featured = listOf(BoardInstrument.GOLD18, BoardInstrument.MELT, BoardInstrument.OUNCE, BoardInstrument.EMAMI).map { key -> rows.first { it.instrument == key } }
                featured.chunked(columns).forEach { group ->
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) { group.forEach { row ->
                        SummaryQuote(row, state.reduceMotion, { detail = row.instrument }, Modifier.weight(1f))
                    } }
                }
                LuxurySegmentedControl(items = (0..4).toList(), selectedItem = filter, onItemSelected = { filter = it },
                    label = { listOf("همه بازارها", "طلا و آبشده", "ارز", "انس جهانی", "سکه")[it] },
                    modifier = Modifier.fillMaxWidth().testTag("rates-filter"), height = 36.dp)
                AnimatedContent(filter, transitionSpec = { fadeIn(tween(if (state.reduceMotion) 0 else 180)) togetherWith fadeOut(tween(if (state.reduceMotion) 0 else 120)) }, label = "rates-filter-transition") { active ->
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        val filtered = rows.filter { active == 0 || it.instrument.group == active }
                        if (filtered.any { it.instrument.coin == null }) BoardTable("تابلوی طلا و ارزهای معتبر بازار", "نرخ طلا، مظنه، انس و ارزهای پایه", filtered.filter { it.instrument.coin == null }, false, compact, state, board,
                            onDetail = { detail = it }, onCalculator = { workspace.applyQuoteToCalculator(it) })
                        if (filtered.any { it.instrument.coin != null }) BoardTable("تابلوی مسکوکات بانکی و حباب", "نرخ اعلامی و ارزش ذاتی با فرمول مشترک برنامه", filtered.filter { it.instrument.coin != null }, true, compact, state, board,
                            onDetail = { detail = it }, onCalculator = { workspace.applyQuoteToCalculator(it) })
                    }
                }
                if (board.error != null) Text(board.error!!, Modifier.testTag("rates-board-error"), color = LocalGoldExColors.current.errorRed, fontSize = 12.sp)
            }
        }
    }
    if (manual) ManualRatesDialog(state, workspace) { manual = false }
    detail?.let { key -> RateDetails(rows.first { it.instrument == key }, state, board, onDismiss = { detail = null },
        onCalculator = { basis -> detail = null; workspace.applyQuoteToCalculator(basis) }) }
}

@Composable private fun RatesWelcome(state: WorkspaceState, board: DesktopRatesBoardState, compact: Boolean, onRefresh: () -> Unit, onManual: () -> Unit) {
    val c = LocalGoldExColors.current
    val actions: @Composable () -> Unit = {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            GoldButton(if (state.refreshing || board.loading) "در حال دریافت" else "دریافت آنلاین", onRefresh,
                Modifier.width(160.dp).testTag("rates-refresh"), enabled = !state.refreshing && !board.loading && !state.saving, icon = Icons.Outlined.Refresh)
            GoldButton("ثبت نرخ دستی", onManual, Modifier.width(150.dp).testTag("manual-rates"), isSecondary = true, enabled = !state.refreshing && !state.saving, icon = Icons.Outlined.Edit)
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.size(44.dp).background(c.goldContainer, CircleShape), contentAlignment = Alignment.Center) {
                Text(state.settings.managerName.firstOrNull()?.toString() ?: "ق", color = c.goldPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
            Column(Modifier.weight(1f)) {
                Text("${DesktopDashboard.greeting(state.now)}، ${state.settings.managerName.ifBlank { "همراه زرگر" }}", color = c.textMain, fontWeight = FontWeight.Bold, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(state.settings.galleryName.ifBlank { "نبض بازار طلا و ارز" }, color = c.textMuted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (!compact) actions()
        }
        if (compact) actions()
        Surface(color = c.surface, shape = RoundedCornerShape(12.dp), border = BorderStroke(0.6.dp,c.goldBorder.copy(alpha = .5f))) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Icons.Outlined.Schedule, null, Modifier.size(17.dp), tint = c.goldPrimary)
                Column(Modifier.weight(1f)) { SourceCaption(state) }
                if (board.loading) CircularProgressIndicator(Modifier.size(16.dp), color = c.goldPrimary, strokeWidth = 2.dp)
                Text(state.snapshot?.label(state.now) ?: "ناموجود", color = c.textSecondary, fontSize = 11.sp, maxLines = 1)
            }
        }
    }
}

@Composable private fun BoardCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val c = LocalGoldExColors.current
    Surface(modifier, color = c.surface, shape = RoundedCornerShape(16.dp), border = BorderStroke(.6.dp,c.goldBorder.copy(alpha = .5f)), shadowElevation = if (c.isDark) 0.dp else 2.dp) {
        Column(content = content)
    }
}

@Composable private fun SummaryQuote(row: BoardRow, reduceMotion: Boolean, onClick: () -> Unit, modifier: Modifier) {
    val c = LocalGoldExColors.current
    BoardCard(modifier.testTag("rates-summary-${row.instrument.name}").clickable(role = Role.Button, onClickLabel = "مشاهده جزئیات ${row.instrument.title}", onClick = onClick)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(Modifier.size(7.dp).background(c.goldPrimary,CircleShape))
                Text(row.instrument.title, Modifier.weight(1f), fontSize = 11.sp, color = c.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                BoardAmount(row.value,row.instrument.dollar,reduceMotion,Modifier.weight(1f), 22)
                Text(if (row.instrument.dollar) "دلار" else "تومان", color = c.textMuted, fontSize = 10.sp)
            }
            DayChange(row.daily, small = true)
            Sparkline(row.history, row.daily?.change, Modifier.fillMaxWidth().height(26.dp))
        }
    }
}

@Composable private fun BoardTable(title: String, subtitle: String, rows: List<BoardRow>, coins: Boolean, compact: Boolean,
    state: WorkspaceState, board: DesktopRatesBoardState, onDetail: (BoardInstrument) -> Unit, onCalculator: (PriceBasisTab) -> Unit) {
    val c = LocalGoldExColors.current
    BoardCard(Modifier.fillMaxWidth().testTag(if (coins) "rates-coins" else "rates-gold-currency")) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.size(36.dp).background(c.goldContainer,RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) { Icon(if (coins) Icons.Outlined.StarOutline else Icons.Outlined.Paid,null,Modifier.size(21.dp),tint = c.goldPrimary) }
            Column(Modifier.weight(1f)) {
                Text(title, Modifier.semantics { heading() }, color = c.textMain, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text(subtitle,color = c.textMuted,fontSize = 11.sp)
            }
            if (!compact) Text(if (coins) "عیار استاندارد: ۹۰۰" else "قیمت‌ها: تومان / دلار", color = c.textMuted, fontSize = 10.sp)
        }
        if (!compact) Row(Modifier.fillMaxWidth().background(c.surfaceElevated.copy(alpha = .6f)).padding(horizontal = 16.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TableHeading(if (coins) "نام مسکوک" else "نماد و شاخص", 2.5f)
            TableHeading(if (coins) "نرخ فروش / اعلامی" else "نرخ لحظه‌ای",1.5f)
            TableHeading(if (coins) "نرخ خرید" else "تغییر روزانه",1.25f)
            TableHeading(if (coins) "نوسان روز" else "کمینه / بیشینه",1.4f)
            TableHeading(if (coins) "حباب محاسبه شده" else "اسپرد",1.2f)
            TableHeading(if (coins) "حباب درصد" else "روند روز",.9f)
            TableHeading("عملیات",1.1f)
        }
        rows.forEach { row ->
            HorizontalDivider(color = c.goldBorder.copy(alpha = .12f), thickness = .6.dp)
            if (compact) CompactQuote(row,state,onDetail,onCalculator) else TableQuote(row,coins,state,onDetail,onCalculator)
        }
        HorizontalDivider(color = c.goldBorder.copy(alpha = .12f), thickness = .6.dp)
        Column(Modifier.fillMaxWidth().background(c.surfaceElevated.copy(alpha = .4f)).padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(if (coins) "حباب = نرخ سکه − (ارزش طلای خالص + حق ضرب). نرخ خرید و وضعیت تقاضا در منبع فعلی موجود نیست."
                else "اسپرد در منبع فعلی موجود نیست. درهم و تتر از TGJU هستند؛ سایر نرخ‌ها از منبع انتخابی شما می‌آیند.", color = c.textMuted,fontSize = 10.sp)
            Text("جزئیات روز: TGJU • ${board.snapshot?.receivedAt?.let(DesktopPortfolioPolicy::observedTime) ?: "دریافت نشده"}${if (board.cached) " • ذخیره شده" else ""} • نمودار و تغییر فقط برای نرخ همسان و داده معتبر امروز نمایش داده می‌شود.",color = c.textMuted,fontSize = 10.sp)
        }
    }
}

@Composable private fun RowScope.TableHeading(text: String, weight: Float) { Text(text, Modifier.weight(weight), color = LocalGoldExColors.current.textSecondary,fontSize = 10.sp) }
@Composable private fun InstrumentName(row: BoardRow, modifier: Modifier = Modifier) {
    val c = LocalGoldExColors.current
    Row(modifier,verticalAlignment = Alignment.CenterVertically,horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(Modifier.size(if (row.instrument.coin == null) 8.dp else 30.dp).background(c.goldContainer,CircleShape),contentAlignment = Alignment.Center) {
            if (row.instrument.coin != null) Icon(Icons.Outlined.Toll,null,Modifier.size(17.dp),tint = c.goldPrimary)
        }
        Column(Modifier.weight(1f),verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(row.instrument.title,color = c.textMain,fontSize = 12.sp,fontWeight = FontWeight.Bold)
            Text(row.instrument.coin?.let { "${PersianNumberFormatter.formatWeight(it.totalWeightGrams)} گرم • عیار ۹۰۰" } ?: row.instrument.description,color = c.textMuted,fontSize = 10.sp)
        }
    }
}
@Composable private fun TableQuote(row: BoardRow, coins: Boolean, state: WorkspaceState, onDetail: (BoardInstrument) -> Unit, onCalculator: (PriceBasisTab) -> Unit) {
    val c = LocalGoldExColors.current
    Row(Modifier.fillMaxWidth().testTag("rate-row-${row.instrument.name}").padding(horizontal = 16.dp,vertical = 15.dp),verticalAlignment = Alignment.CenterVertically,horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        InstrumentName(row,Modifier.weight(2.5f))
        Column(Modifier.weight(1.5f),verticalArrangement = Arrangement.spacedBy(4.dp)) {
            BoardAmount(row.value,row.instrument.dollar,state.reduceMotion,size = 16)
            Text(if (row.instrument.dollar) "دلار آمریکا" else "تومان",color = c.textMuted,fontSize = 10.sp)
        }
        Box(Modifier.weight(1.25f)) { if (coins) Text("—",color = c.textMuted,fontSize = 12.sp) else DayChange(row.daily) }
        Box(Modifier.weight(1.4f)) { if (coins) DayChange(row.daily) else Column {
            Text(formatBoardNumber(row.daily?.low,row.instrument.dollar),color = c.textMuted,fontSize = 10.sp)
            Text(formatBoardNumber(row.daily?.high,row.instrument.dollar),color = c.textMuted,fontSize = 10.sp)
        } }
        Box(Modifier.weight(1.2f)) { if (coins) BubbleAmount(row) else Text("—",color = c.textMuted) }
        Box(Modifier.weight(.9f)) { if (coins) BubblePercent(row) else Sparkline(row.history,row.daily?.change,Modifier.fillMaxWidth().height(25.dp)) }
        Box(Modifier.weight(1.1f)) { QuoteAction(row,onDetail,onCalculator) }
    }
}
@Composable private fun CompactQuote(row: BoardRow, state: WorkspaceState, onDetail: (BoardInstrument) -> Unit, onCalculator: (PriceBasisTab) -> Unit) {
    val c = LocalGoldExColors.current
    Column(Modifier.fillMaxWidth().testTag("rate-row-${row.instrument.name}").padding(16.dp),verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically,horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            InstrumentName(row,Modifier.weight(1f))
            Column(horizontalAlignment = Alignment.End) { BoardAmount(row.value,row.instrument.dollar,state.reduceMotion,size = 18); Text(if (row.instrument.dollar) "دلار" else "تومان",fontSize = 10.sp,color = c.textMuted) }
        }
        Row(verticalAlignment = Alignment.CenterVertically,horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.weight(1f)) { DayChange(row.daily) }
            if (row.instrument.coin != null) { BubbleAmount(row); BubblePercent(row) }
            else Sparkline(row.history,row.daily?.change,Modifier.width(70.dp).height(25.dp))
            QuoteAction(row,onDetail,onCalculator)
        }
    }
}

internal fun formatBoardNumber(value: Double?, dollar: Boolean = false): String = value?.takeIf { it.isFinite() && it > 0 }?.let {
    if (dollar) PersianNumberFormatter.toPersianDigits(String.format(Locale.US,"%,.2f",it)) else PersianNumberFormatter.format(it.roundToLong())
} ?: "—"
@Composable private fun BoardAmount(value: Double?, dollar: Boolean, reduceMotion: Boolean, modifier: Modifier = Modifier, size: Int = 18) {
    val c = LocalGoldExColors.current; val text = formatBoardNumber(value,dollar)
    if (reduceMotion) Text(text,modifier,fontSize = size.sp,color = c.textMain,fontWeight = FontWeight.Bold,maxLines = 1)
    else AnimatedPriceTicker(text,modifier,color = c.textMain,fontSize = size.sp,fontWeight = FontWeight.Bold)
}
@Composable private fun DayChange(day: BoardDayQuote?, small: Boolean = false) {
    val c = LocalGoldExColors.current; val change = day?.change
    if (change == null) Text("تغییر روز: —",color = c.textMuted,fontSize = if (small) 10.sp else 9.sp)
    else {
        val color = if (change > 0) c.marketGainText else if (change < 0) c.errorRed else c.textSecondary
        val sign = if (change > 0) "+" else if (change < 0) "−" else ""
        val amount = PersianNumberFormatter.toPersianDigits(String.format(Locale.US,if (abs(change) % 1.0 > .00001) "%,.2f" else "%,.0f",abs(change)))
        val pct = day.percent?.let { " (${PersianNumberFormatter.toPersianDigits(String.format(Locale.US,"%.2f",abs(it)))}٪)" }.orEmpty()
        Text("$sign$amount$pct",Modifier.background(color.copy(alpha = .08f),RoundedCornerShape(6.dp)).padding(horizontal = 6.dp,vertical = 5.dp),color = color,fontSize = if (small) 10.sp else 9.sp,maxLines = 2)
    }
}
@Composable private fun BubbleAmount(row: BoardRow) {
    val c = LocalGoldExColors.current
    val value = DesktopRatesBoardPolicy.bubbleAmount(row)
    Text(value?.let { "${PersianNumberFormatter.format(it)}\nتومان" } ?: "—",color = if ((value ?: 0) < 0) c.marketGainText else c.goldPrimary,fontSize = 10.sp)
}
@Composable private fun BubblePercent(row: BoardRow) {
    val c = LocalGoldExColors.current
    Text(row.bubble?.let { PersianNumberFormatter.toPersianDigits(String.format(Locale.US,"%.2f٪",it.bubblePercent)) } ?: "—",color = c.textSecondary,fontSize = 10.sp)
}
private fun basis(row: BoardRow) = when (row.instrument) { BoardInstrument.GOLD18 -> PriceBasisTab.K18; BoardInstrument.GOLD24 -> PriceBasisTab.K24; BoardInstrument.MELT -> PriceBasisTab.MESGHAL; else -> null }
@Composable private fun QuoteAction(row: BoardRow, onDetail: (BoardInstrument) -> Unit, onCalculator: (PriceBasisTab) -> Unit) {
    val c = LocalGoldExColors.current
    TextButton(onClick = { onDetail(row.instrument) }, modifier = Modifier.testTag("rate-detail-${row.instrument.name}"), contentPadding = PaddingValues(horizontal = 8.dp,vertical = 4.dp), colors = ButtonDefaults.textButtonColors(contentColor = c.textMain)) {
        Text(if (basis(row) != null) "جزئیات و\nمحاسبه" else "جزئیات",fontSize = 10.sp)
    }
}
@Composable private fun Sparkline(values: List<Double>, change: Double?, modifier: Modifier) {
    val c = LocalGoldExColors.current; val color = if ((change ?: 0.0) < 0) c.errorRed else c.marketGainText
    if (values.size < 2) Box(modifier,contentAlignment = Alignment.Center) { Text("—",color = c.textMuted,fontSize = 12.sp) }
    else Canvas(modifier.testTag("rate-sparkline").semantics { }) {
        val lo = values.minOrNull()!!; val range = (values.maxOrNull()!! - lo)
        val p = values.mapIndexed { i,v -> Offset(i.toFloat() / (values.size - 1) * size.width, if (range == 0.0) size.height/2 else (size.height - 4.dp.toPx()) * (1 - (v-lo)/range).toFloat()+2.dp.toPx()) }
        val path = Path().apply { moveTo(p[0].x,p[0].y); for (i in 0 until p.lastIndex) {
            val a = p[if (i == 0) i else i-1]; val b = p[i]; val d = p[i+1]; val e = p[minOf(i+2,p.lastIndex)]
            cubicTo(b.x+(d.x-a.x)/6,b.y+(d.y-a.y)/6,d.x-(e.x-b.x)/6,d.y-(e.y-b.y)/6,d.x,d.y)
        } }
        drawPath(path,color,style = Stroke(1.6.dp.toPx(),cap = StrokeCap.Round))
    }
}
@Composable private fun RateDetails(row: BoardRow, state: WorkspaceState, board: DesktopRatesBoardState, onDismiss: () -> Unit, onCalculator: (PriceBasisTab) -> Unit) {
    val c = LocalGoldExColors.current
    AlertDialog(onDismissRequest = onDismiss, modifier = Modifier.widthIn(max = 660.dp).testTag("rate-details-dialog"), title = { Text(row.instrument.title) }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            BoardAmount(row.value,row.instrument.dollar,state.reduceMotion,size = 28)
            Text(if (row.instrument.dollar) "دلار آمریکا برای هر اونس" else "تومان",color = c.textMuted,fontSize = 12.sp)
            DayChange(row.daily)
            Text("کمینه: ${formatBoardNumber(row.daily?.low,row.instrument.dollar)} • بیشینه: ${formatBoardNumber(row.daily?.high,row.instrument.dollar)}",color = c.textSecondary,fontSize = 12.sp)
            Sparkline(row.history,row.daily?.change,Modifier.fillMaxWidth().height(100.dp))
            if (row.instrument.coin != null) {
                Text("حباب محاسبه شده",color = c.textMain,fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) { BubbleAmount(row); BubblePercent(row) }
                Text("ارزش ذاتی: ${row.bubble?.intrinsicValue?.let { formatBoardNumber(it) } ?: "—"} تومان\nبر اساس انس، دلار، وزن خالص و حق ضرب؛ نرخ خرید یا توصیه معامله نیست.",color = c.textMuted,fontSize = 11.sp)
            }
            Text("${row.source} • ${if (row.instrument in listOf(BoardInstrument.AED,BoardInstrument.TETHER)) board.snapshot?.receivedAt?.let(DesktopPortfolioPolicy::observedTime) ?: "—" else state.snapshot?.observedAt?.let(DesktopPortfolioPolicy::observedTime) ?: "—"}",color = c.textMuted,fontSize = 11.sp)
            if (row.history.isEmpty()) Text("تاریخچه معتبر و همسان در دسترس نیست.",color = c.textMuted,fontSize = 11.sp)
        }
    }, confirmButton = { Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        GoldButton("بستن",onDismiss,isSecondary = true,modifier = Modifier.width(100.dp).testTag("rate-details-close"))
        basis(row)?.let { selected -> GoldButton("محاسبه در فاکتور",{ onCalculator(selected) },enabled = row.value != null && !state.saving,modifier = Modifier.width(175.dp).testTag("rate-use-calculator")) }
    } })
}
