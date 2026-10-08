package com.goldex.companion.desktop.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
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
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.*
import com.goldex.companion.data.ConnectionStatus
import com.goldex.companion.desktop.data.*
import com.goldex.companion.desktop.state.*
import com.goldex.companion.model.*
import com.goldex.companion.ui.components.*
import com.goldex.companion.ui.theme.*
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToLong

// Rates-specific desktop scale from the supplied references; theme owns fonts and colors.
private object RatesType {
    val title = 13.5.sp
    val secondary = 10.5.sp
    val caption = 10.sp
    val tableHeading = 11.sp
}

@Composable internal fun RatesPage(state: WorkspaceState, workspace: DesktopWorkspace) {
    val board by workspace.ratesBoard.state.collectAsState()
    var detail by remember { mutableStateOf<BoardInstrument?>(null) }
    // Tether is hidden from this board; its stored data and provider contract stay compatible.
    val rows = BoardInstrument.values().filter { it != BoardInstrument.TETHER }
        .map { DesktopRatesBoardPolicy.row(it, state.snapshot, board, state.now) }
    DisposableEffect(workspace.ratesBoard) {
        val subscription = workspace.ratesBoard.observe()
        onDispose { subscription.cancel() }
    }
    PageScroll {
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val compact = maxWidth < 860.dp
            val columns = if (maxWidth < 800.dp) 2 else 4
            Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                val featured = listOf(BoardInstrument.GOLD18, BoardInstrument.MELT, BoardInstrument.OUNCE, BoardInstrument.EMAMI)
                    .map { key -> rows.first { it.instrument == key } }
                featured.chunked(columns).forEach { group ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        group.forEach { row ->
                            SummaryQuote(row, state.reduceMotion, { detail = row.instrument }, Modifier.weight(1f))
                        }
                    }
                }
                BoardTable("تابلوی طلا و ارزهای معتبر بازار", "نرخ‌های رسمی بنکداری، طلای خام، شمش و ارزهای پایه معاملاتی",
                    rows.filter { it.instrument.coin == null }, false, compact, state, board, onDetail = { detail = it })
                BoardTable("تابلوی رسمی انواع مسکوکات بانکی و حباب", "نرخ اعلامی، ارزش ذاتی طلا و حباب محاسبه‌شده",
                    rows.filter { it.instrument.coin != null }, true, compact, state, board, onDetail = { detail = it })
                board.error?.let { RatesText(it, Modifier.testTag("rates-board-error"), color = LocalGoldExColors.current.errorRed, fontSize = 12.sp) }
            }
        }
    }
    detail?.let { key ->
        RateDetails(rows.first { it.instrument == key }, state, board, onDismiss = { detail = null },
            onCalculator = { basis -> detail = null; workspace.applyQuoteToCalculator(basis) })
    }
}

private fun boardClock(timestamp: Long): String = PersianNumberFormatter.toPersianDigits(
    Instant.ofEpochMilli(timestamp).atZone(ZoneId.of("Asia/Tehran")).format(DateTimeFormatter.ofPattern("HH:mm")))

@Composable private fun BoardCard(modifier: Modifier = Modifier, onClick: (() -> Unit)? = null, content: @Composable ColumnScope.() -> Unit) {
    val c = LocalGoldExColors.current
    Surface(modifier, color = c.surface, shape = RoundedCornerShape(16.dp),
        border = BorderStroke(.6.dp, c.border.copy(alpha = if (c.isDark) .14f else .08f)), shadowElevation = if (c.isDark) 0.dp else 2.dp) {
        Column(Modifier.then(if (onClick == null) Modifier else Modifier.clickable(role = Role.Button,
            onClickLabel = "مشاهده جزئیات", onClick = onClick)), content = content)
    }
}

@Composable private fun SummaryQuote(row: BoardRow, reduceMotion: Boolean, onClick: () -> Unit, modifier: Modifier) {
    val c = LocalGoldExColors.current
    BoardCard(modifier.testTag("rates-summary-${row.instrument.name}"), onClick = onClick) {
        Column(Modifier.fillMaxWidth().heightIn(min = 182.dp).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                InstrumentDot(row.instrument)
                RatesText(summaryTitle(row.instrument), Modifier.weight(1f), fontSize = 11.sp, fontWeight = FontWeight.Medium,
                    color = c.textMain, maxLines = 1, overflow = TextOverflow.Ellipsis)
                PercentChange(row.daily)
            }
            HorizontalDivider(color = c.border.copy(alpha = .08f), thickness = .6.dp)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                BoardAmount(row.value, row.instrument.dollar, reduceMotion, size = 26)
                RatesText(if (row.instrument.dollar) "دلار آمریکا" else "تومان", color = c.textMuted, fontSize = 11.sp)
            }
            HorizontalDivider(color = c.border.copy(alpha = .05f), thickness = .6.dp)
            SummaryContext(row)
            Sparkline(row.history, row.daily?.change, Modifier.fillMaxWidth().height(28.dp))
        }
    }
}

private fun summaryTitle(instrument: BoardInstrument): String = when (instrument) {
    BoardInstrument.GOLD18 -> "طلای ۱۸ عیار (۷۵۰)"
    BoardInstrument.MELT -> "مظنه آبشده (مثقال)"
    BoardInstrument.OUNCE -> "انس طلا (XAU/USD)"
    BoardInstrument.EMAMI -> "سکه امامی (طرح جدید)"
    else -> instrument.title
}

@Composable private fun InstrumentDot(instrument: BoardInstrument) {
    val c = LocalGoldExColors.current
    val color = when (instrument) {
        BoardInstrument.OUNCE -> c.textMuted
        BoardInstrument.USD -> c.profitGreen
        BoardInstrument.AED -> c.syncBlue
        BoardInstrument.GOLD18, BoardInstrument.EMAMI -> c.goldSecondary
        else -> c.goldPrimary
    }
    Box(Modifier.size(9.dp).background(color, CircleShape))
}

@Composable private fun SummaryContext(row: BoardRow) {
    val c = LocalGoldExColors.current
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween) {
        when (row.instrument) {
            BoardInstrument.GOLD18 -> {
                RatesText("کمینه: ${formatBoardNumber(row.daily?.low)}", color = c.textMuted, fontSize = 9.sp)
                RatesText("بیشینه: ${formatBoardNumber(row.daily?.high)}", color = c.textMuted, fontSize = 9.sp)
            }
            BoardInstrument.OUNCE -> {
                RatesText("نوسان ۲۴ ساعته:", color = c.textMuted, fontSize = 9.sp)
                RatesText(row.daily?.change?.let { "${signedBoardNumber(it)} دلار" } ?: "—",
                    color = changeColor(row.daily?.change), fontSize = 10.sp)
            }
            BoardInstrument.EMAMI -> {
                RatesText("حباب سکه:", color = c.textMuted, fontSize = 9.sp)
                RatesText(DesktopRatesBoardPolicy.bubbleAmount(row)?.let { "${PersianNumberFormatter.format(it)} تومان" } ?: "—",
                    color = c.goldPrimary, fontSize = 10.sp, maxLines = 1)
            }
            else -> {
                // The provider has no melted-gold premium; do not fabricate a bubble from a daily change.
                RatesText("حباب مظنه:", color = c.textMuted, fontSize = 9.sp)
                RatesText("—", color = c.textMuted, fontSize = 10.sp)
            }
        }
    }
}

@Composable private fun BoardTable(title: String, subtitle: String, rows: List<BoardRow>, coins: Boolean,
    compact: Boolean, state: WorkspaceState, board: DesktopRatesBoardState, onDetail: (BoardInstrument) -> Unit) {
    val c = LocalGoldExColors.current
    BoardCard(Modifier.fillMaxWidth().testTag(if (coins) "rates-coins" else "rates-gold-currency")) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.size(38.dp).background(c.goldContainer, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                Icon(if (coins) Icons.Outlined.StarOutline else Icons.Outlined.Paid, null, Modifier.size(23.dp), tint = c.goldPrimary)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                RatesText(title, Modifier.semantics { heading() }, color = c.textMain, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                RatesText(subtitle, color = c.textMuted, fontSize = 11.sp)
            }
            if (!compact) Surface(color = c.surfaceElevated.copy(alpha = .7f), shape = RoundedCornerShape(12.dp),
                border = BorderStroke(.6.dp, c.border.copy(alpha = .08f))) {
                RatesText(if (coins) "عیار استاندارد مسکوکات: ۹۰۰ (۲۱٫۶)" else "واحد قیمت‌ها: تومان رسمی",
                    Modifier.padding(horizontal = 12.dp, vertical = 6.dp), color = c.textMuted, fontSize = 10.sp)
            }
        }
        if (!compact) Row(Modifier.fillMaxWidth().background(c.surfaceElevated.copy(alpha = .35f))
            .padding(horizontal = 20.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (coins) {
                TableHeading("نام مسکوک", 3.1f)
                TableHeading("نرخ اعلامی (تومان)", 1.5f)
                TableHeading("نرخ خرید (تومان)", 1.4f)
                TableHeading("نوسان روز", 1.65f)
                TableHeading("حباب سکه (مبلغ و درصد)", 1.6f)
                TableHeading("عملیات", 1f)
            } else {
                TableHeading("نماد و شاخص", 2.9f)
                TableHeading("نرخ لحظه‌ای (تومان)", 1.5f)
                TableHeading("تغییر روزانه", 1.55f)
                TableHeading("کمینه / بیشینه روز", 1.65f)
                TableHeading("نمودار روند", .85f)
                TableHeading("عملیات", 1.05f)
            }
        }
        rows.forEach { row ->
            HorizontalDivider(color = c.border.copy(alpha = .08f), thickness = .6.dp)
            if (compact) CompactQuote(row, state, onDetail) else TableQuote(row, coins, state, onDetail)
        }
        HorizontalDivider(color = c.border.copy(alpha = .08f), thickness = .6.dp)
        TableFooter(coins, compact, state, board)
    }
}

@Composable private fun TableFooter(coins: Boolean, compact: Boolean, state: WorkspaceState, board: DesktopRatesBoardState) {
    val c = LocalGoldExColors.current
    val source = if (coins) "ارزش ذاتی: طلای خالص × (انس × دلار ÷ ۳۱٫۱۰۳۵) + حق ضرب"
        else "منبع نرخ‌ها: ${state.snapshot?.label(state.now) ?: "ناموجود"} • درهم: TGJU"
    val metadata = if (coins) "نرخ خرید: — • ${state.snapshot?.label(state.now) ?: "ناموجود"} • ${state.snapshot?.observedAt?.let(::boardClock) ?: "—"}"
        else "جزئیات روز و نمودار: TGJU • ${board.snapshot?.receivedAt?.let(::boardClock) ?: "دریافت نشده"}${if (board.cached) " • ذخیره‌شده" else ""}"
    val modifier = Modifier.fillMaxWidth().background(c.surfaceElevated.copy(alpha = .25f)).padding(horizontal = 20.dp, vertical = 12.dp)
    if (compact) Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        RatesText(source, color = c.textMuted, fontSize = RatesType.caption)
        RatesText(metadata, color = c.textMuted, fontSize = RatesType.caption)
    } else Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(18.dp)) {
        RatesText(source, Modifier.weight(1.4f), color = c.textMuted, fontSize = RatesType.caption)
        RatesText(metadata, Modifier.weight(1f), color = c.textMuted, fontSize = RatesType.caption)
    }
}

@Composable private fun RowScope.TableHeading(text: String, weight: Float) {
    RatesText(text, Modifier.weight(weight), color = LocalGoldExColors.current.textMain,
        fontSize = RatesType.tableHeading, fontWeight = FontWeight.Medium)
}

@Composable private fun InstrumentName(row: BoardRow, modifier: Modifier = Modifier) {
    val c = LocalGoldExColors.current
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
        if (row.instrument.coin == null) InstrumentDot(row.instrument)
        else {
            val label = when (row.instrument) {
                BoardInstrument.BAHAR -> "قدیم"; BoardInstrument.EMAMI -> "امامی"; BoardInstrument.HALF -> "نیم"
                BoardInstrument.QUARTER -> "ربع"; else -> "گرمی"
            }
            val accent = if (row.instrument == BoardInstrument.EMAMI) c.goldPrimary
                else if (row.instrument == BoardInstrument.QUARTER) c.errorRed else c.textSecondary
            Box(Modifier.size(34.dp).background(accent.copy(alpha = .06f), CircleShape)
                .border(.6.dp, accent.copy(alpha = .15f), CircleShape), contentAlignment = Alignment.Center) {
                RatesText(label, color = accent, fontSize = 10.sp, fontWeight = FontWeight.Medium)
            }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            RatesText(row.instrument.title, color = c.textMain, fontSize = RatesType.title, fontWeight = FontWeight.SemiBold)
            RatesText(row.instrument.coin?.let { "وزن: ${PersianNumberFormatter.formatWeight(it.totalWeightGrams)} گرم | طلای ۹۰۰" }
                ?: row.instrument.description, color = c.textMuted, fontSize = RatesType.secondary)
        }
    }
}

@Composable private fun TableQuote(row: BoardRow, coins: Boolean, state: WorkspaceState, onDetail: (BoardInstrument) -> Unit) {
    val c = LocalGoldExColors.current
    Row(Modifier.fillMaxWidth().testTag("rate-row-${row.instrument.name}").heightIn(min = 70.dp)
        .padding(horizontal = 20.dp, vertical = 11.dp), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        InstrumentName(row, Modifier.weight(if (coins) 3.1f else 2.9f))
        Box(Modifier.weight(1.5f)) { InlineQuote(row, state.reduceMotion) }
        if (coins) {
            RatesText("—", Modifier.weight(1.4f), color = c.textMuted, fontSize = RatesType.title)
            Box(Modifier.weight(1.65f)) { DayChange(row.daily) }
            Column(Modifier.weight(1.6f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                BubbleAmount(row)
                BubblePercent(row)
            }
            Box(Modifier.weight(1f)) { QuoteAction(row, onDetail) }
        } else {
            Box(Modifier.weight(1.55f)) { DayChange(row.daily) }
            RatesText("${formatBoardNumber(row.daily?.low, row.instrument.dollar)} / ${formatBoardNumber(row.daily?.high, row.instrument.dollar)}",
                Modifier.weight(1.65f), color = c.textMuted, fontSize = RatesType.secondary, maxLines = 2)
            Box(Modifier.weight(.85f).padding(horizontal = 4.dp)) {
                Sparkline(row.history, row.daily?.change, Modifier.fillMaxWidth().height(24.dp))
            }
            Box(Modifier.weight(1.05f)) { QuoteAction(row, onDetail) }
        }
    }
}

@Composable private fun InlineQuote(row: BoardRow, reduceMotion: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        BoardAmount(row.value, row.instrument.dollar, reduceMotion, size = 15)
        RatesText(if (row.instrument.dollar) "دلار" else "تومان", color = LocalGoldExColors.current.textMuted, fontSize = 10.sp)
    }
}

@Composable private fun CompactQuote(row: BoardRow, state: WorkspaceState, onDetail: (BoardInstrument) -> Unit) {
    val c = LocalGoldExColors.current
    Column(Modifier.fillMaxWidth().testTag("rate-row-${row.instrument.name}").padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            InstrumentName(row, Modifier.weight(1f))
            InlineQuote(row, state.reduceMotion)
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.weight(1f)) { DayChange(row.daily) }
            if (row.instrument.coin != null) Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                BubbleAmount(row); BubblePercent(row)
            } else Sparkline(row.history, row.daily?.change, Modifier.width(72.dp).height(25.dp))
            QuoteAction(row, onDetail)
        }
        if (row.instrument.coin == null) RatesText(
            "کمینه: ${formatBoardNumber(row.daily?.low, row.instrument.dollar)} • بیشینه: ${formatBoardNumber(row.daily?.high, row.instrument.dollar)}",
            color = c.textMuted, fontSize = RatesType.secondary)
        else RatesText("نرخ خرید: —", color = c.textMuted, fontSize = RatesType.secondary)
    }
}

internal fun formatBoardNumber(value: Double?, dollar: Boolean = false): String = value?.takeIf { it.isFinite() && it > 0 }?.let {
    if (dollar) PersianNumberFormatter.toPersianDigits(String.format(Locale.US, "%,.2f", it))
    else PersianNumberFormatter.format(it.roundToLong())
} ?: "—"

private fun signedBoardNumber(value: Double): String {
    val sign = if (value > 0) "+" else if (value < 0) "−" else ""
    return sign + PersianNumberFormatter.toPersianDigits(String.format(Locale.US,
        if (abs(value) % 1.0 > .00001) "%,.2f" else "%,.0f", abs(value)))
}

@Composable private fun BoardAmount(value: Double?, dollar: Boolean, reduceMotion: Boolean, modifier: Modifier = Modifier, size: Int = 18) {
    val c = LocalGoldExColors.current
    val text = formatBoardNumber(value, dollar)
    if (reduceMotion) RatesText(text, modifier.clipToBounds(), fontSize = size.sp, color = c.textMain,
        fontWeight = FontWeight.Black, maxLines = 1, softWrap = false)
    else AnimatedPriceTicker(text, modifier.clipToBounds(), color = c.textMain, fontSize = size.sp, fontWeight = FontWeight.Black)
}

@Composable private fun changeColor(value: Double?): Color {
    val c = LocalGoldExColors.current
    return if (value != null && value > 0) c.marketGainText else if (value != null && value < 0) c.errorRed else c.textMuted
}

@Composable private fun PercentChange(day: BoardDayQuote?) {
    val color = changeColor(day?.change)
    Row(Modifier.background(color.copy(alpha = .08f), RoundedCornerShape(6.dp)).padding(horizontal = 6.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        day?.percent?.takeIf { it != 0.0 }?.let {
            Icon(if (it < 0) Icons.Outlined.KeyboardArrowDown else Icons.Outlined.KeyboardArrowUp, null, Modifier.size(12.dp))
        }
        RatesText(day?.percent?.let { "${signedBoardNumber(it)}٪" } ?: "—", color = color, fontSize = 9.sp, maxLines = 1, textDirection = TextDirection.Ltr)
    }
}

@Composable private fun DayChange(day: BoardDayQuote?, small: Boolean = false) {
    val c = LocalGoldExColors.current
    val change = day?.change
    if (change == null) RatesText("—", color = c.textMuted, fontSize = RatesType.secondary)
    else if (change == 0.0) Surface(color = c.surfaceElevated, shape = RoundedCornerShape(6.dp), border = c.hairlineBorder) {
        RatesText("بدون نوسان", Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            color = c.textSecondary, fontSize = RatesType.caption)
    } else {
        val color = changeColor(change)
        Row(Modifier.background(color.copy(alpha = .06f), RoundedCornerShape(6.dp))
            .border(.6.dp, color.copy(alpha = .2f), RoundedCornerShape(6.dp)).padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            Icon(if (change < 0) Icons.Outlined.KeyboardArrowDown else Icons.Outlined.KeyboardArrowUp, null,
                Modifier.size(12.dp), tint = color)
            val pct = day.percent?.let { " (${signedBoardNumber(it)}٪)" }.orEmpty()
            RatesText("${signedBoardNumber(change)}$pct", color = color, fontSize = if (small) 9.sp else RatesType.caption,
                fontWeight = FontWeight.Medium, maxLines = 2, textDirection = TextDirection.Ltr)
        }
    }
}

@Composable private fun BubbleAmount(row: BoardRow) {
    val c = LocalGoldExColors.current
    RatesText(DesktopRatesBoardPolicy.bubbleAmount(row)?.let { "${PersianNumberFormatter.format(it)} تومان" } ?: "—",
        color = if ((row.bubble?.bubbleAmount ?: 0.0) < 0) c.marketGainText else c.goldPrimary,
        fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
}

@Composable private fun BubblePercent(row: BoardRow) {
    val c = LocalGoldExColors.current
    val percent = row.bubble?.bubblePercent
    // The existing Android coin UI marks >18% as high; retain that visual cue without investment-risk claims.
    val high = percent != null && percent > 18.0
    val color = if (high) c.errorRed else if (percent != null && percent < 0) c.marketGainText else c.goldPrimary
    RatesText(percent?.let {
        PersianNumberFormatter.toPersianDigits(String.format(Locale.US, "%.2f٪", it)) + if (high) " (حباب بالا)" else ""
    } ?: "—", Modifier.background(color.copy(alpha = .08f), RoundedCornerShape(4.dp))
        .padding(horizontal = 6.dp, vertical = 3.dp), color = color, fontSize = 9.sp, maxLines = 1)
}

private fun basis(row: BoardRow) = when (row.instrument) {
    BoardInstrument.GOLD18 -> PriceBasisTab.K18
    BoardInstrument.GOLD24 -> PriceBasisTab.K24
    BoardInstrument.MELT -> PriceBasisTab.MESGHAL
    else -> null
}

@Composable private fun QuoteAction(row: BoardRow, onDetail: (BoardInstrument) -> Unit) {
    val c = LocalGoldExColors.current
    Button(onClick = { onDetail(row.instrument) }, modifier = Modifier.height(32.dp).testTag("rate-detail-${row.instrument.name}"),
        shape = ButtonShape, contentPadding = PaddingValues(horizontal = 14.dp, vertical = 5.dp),
        colors = ButtonDefaults.buttonColors(containerColor = c.surfaceElevated.copy(alpha = .7f), contentColor = c.textMain),
        elevation = ButtonDefaults.buttonElevation(0.dp), border = null) {
        RatesText("جزئیات", fontSize = 11.sp, fontWeight = FontWeight.Medium, maxLines = 1)
    }
}

@Composable private fun RatesText(text: String, modifier: Modifier = Modifier, color: Color = LocalGoldExColors.current.textMain,
    fontSize: TextUnit = RatesType.secondary, fontWeight: FontWeight? = null, maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip, softWrap: Boolean = true, lineHeight: TextUnit = fontSize * 1.4f,
    textDirection: TextDirection = TextDirection.ContentOrRtl) {
    Text(text, modifier, color = color, fontSize = fontSize, fontWeight = fontWeight, maxLines = maxLines,
        overflow = overflow, softWrap = softWrap, lineHeight = lineHeight, style = LocalTextStyle.current.copy(textDirection = textDirection))
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
