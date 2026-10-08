package com.goldex.companion.desktop.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.goldex.companion.desktop.data.*
import com.goldex.companion.desktop.state.*
import com.goldex.companion.model.PersianNumberFormatter
import com.goldex.companion.model.TimeHorizon
import com.goldex.companion.ui.components.*
import com.goldex.companion.ui.theme.*
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable internal fun DashboardTrend(
    state: DesktopDashboardState, now: Long, reduceMotion: Boolean, dashboard: DesktopDashboard, quote: MarketSnapshot?
) {
    val colors = LocalGoldExColors.current
    val active = state.active(now)
    val current = active?.points?.lastOrNull()?.price ?: quote?.rates?.gold18?.takeIf { it > 0 }
    LuxuryCard(Modifier.testTag("dashboard-trend"), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val heading: @Composable () -> Unit = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.size(8.dp).background(colors.goldPrimary, CircleShape))
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("نوسانات لحظه‌ای طلای ۱۸ عیار", color = colors.textMain, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text("تاریخچه بازار TGJU • تومان به ازای هر گرم ۷۵۰", color = colors.textMuted, fontSize = 11.sp)
                    }
                }
            }
            val selector: @Composable () -> Unit = {
                LuxurySegmentedControl(items = listOf(TimeHorizon.TODAY, TimeHorizon.ONE_WEEK, TimeHorizon.ONE_MONTH),
                    selectedItem = state.horizon, onItemSelected = { dashboard.select(it) },
                    label = { when (it) { TimeHorizon.TODAY -> "امروز"; TimeHorizon.ONE_WEEK -> "هفتگی"; else -> "ماهانه" } },
                    modifier = Modifier.width(184.dp).testTag("chart-horizon"), height = 32.dp, fontSize = 10.5.sp)
            }
            if (maxWidth >= 380.dp) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { heading(); selector() }
            else Column(verticalArrangement = Arrangement.spacedBy(10.dp)) { heading(); selector() }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.Bottom) {
                TrendAmount(current?.let(PersianNumberFormatter::formatPrice) ?: "—", reduceMotion, Modifier.testTag("trend-current-price"), 24, colors.textMain, FontWeight.Black)
                Text("تومان / گرم", color = colors.textMuted, fontSize = 11.sp, modifier = Modifier.padding(bottom = 3.dp))
            }
            if (active != null) {
                val positive = active.change >= 0
                val tint = if (positive) colors.marketGainText else colors.errorRed
                Surface(shape = RoundedCornerShape(14.dp), color = tint.copy(alpha = 0.12f), border = BorderStroke(0.6.dp, tint.copy(alpha = 0.3f))) {
                    Row(Modifier.padding(horizontal = 7.dp, vertical = 3.dp).testTag("trend-delta"), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        Icon(if (positive) Icons.Outlined.TrendingUp else Icons.Outlined.TrendingDown, null, Modifier.size(12.dp), tint = tint)
                        TrendAmount(PersianNumberFormatter.formatDelta(active.change, active.changePercent), reduceMotion, size = 10, tint = tint, weight = FontWeight.Bold)
                    }
                }
            }
        }
        AnimatedContent(state.horizon, transitionSpec = {
            if (reduceMotion) fadeIn(tween(0)).togetherWith(fadeOut(tween(0)))
            else (fadeIn(tween(240, easing = FastOutSlowInEasing)) + scaleIn(initialScale = .96f, animationSpec = tween(240, easing = FastOutSlowInEasing)))
                .togetherWith(fadeOut(tween(180, easing = FastOutLinearInEasing)) + scaleOut(targetScale = .98f, animationSpec = tween(180, easing = FastOutLinearInEasing)))
        }, label = "dashboard-history-horizon") { horizon ->
            val history = state.history[horizon]?.takeIf { it.belongsToToday(now) }
            if (history != null) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("بیشینه: ${PersianNumberFormatter.formatPrice(history.high)} تومان", color = colors.marketGainText, fontSize = 11.sp)
                        Text("کمینه: ${PersianNumberFormatter.formatPrice(history.low)} تومان", color = colors.errorRed, fontSize = 11.sp)
                    }
                    HistoryChart(history, reduceMotion)
                }
            } else Box(Modifier.fillMaxWidth().height(130.dp).testTag("history-unavailable"), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (state.loading) CircularProgressIndicator(Modifier.size(26.dp), colors.goldPrimary, strokeWidth = 2.dp)
                    else Icon(Icons.Outlined.CandlestickChart, null, Modifier.size(28.dp), tint = colors.textMuted.copy(alpha = .4f))
                    Text(if (state.loading) "دریافت تاریخچه بازار…" else if (horizon == TimeHorizon.TODAY) "داده‌های نوسان امروز هنوز در دسترس نیست" else "داده‌های نمودار برای این بازه در دسترس نیست",
                        color = colors.textMuted, fontSize = 11.5.sp)
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(Modifier.size(6.dp).background(colors.marketGainText, CircleShape))
                Column {
                    Text(if (active == null) "نرخ دستی جایگزین تاریخچه بازار نمی‌شود." else "بروزرسانی: ${DesktopPortfolioPolicy.observedTime(active.receivedAt)} • دامنه نوسان: ${PersianNumberFormatter.formatPrice(active.high - active.low)} تومان",
                        color = colors.textMuted, fontSize = 11.sp)
                    if (state.error != null) Text(state.error, color = colors.errorRed, fontSize = 12.sp, modifier = Modifier.testTag("history-error"))
                }
            }

        }
    }
}

@Composable private fun TrendAmount(text: String, reduceMotion: Boolean, modifier: Modifier = Modifier, size: Int, tint: Color, weight: FontWeight) {
    if (reduceMotion) Text(text, modifier, color = tint, fontSize = size.sp, fontWeight = weight, maxLines = 1)
    else AnimatedPriceTicker(text, modifier, color = tint, fontSize = size.sp, fontWeight = weight, maxLines = 1)
}

@Composable private fun HistoryChart(history: GoldHistorySnapshot, reduceMotion: Boolean) {
    val colors = LocalGoldExColors.current
    val gold = if (colors.isDark) colors.goldSecondary else colors.goldPrimary
    val points = history.points
    var selected by remember(history) { mutableStateOf(points.lastIndex) }
    fun x(point: GoldHistoryPoint) = (point.at - points.first().at).toDouble().div((points.last().at - points.first().at).coerceAtLeast(1)).toFloat()
    fun y(point: GoldHistoryPoint) = if (history.high == history.low) .5f else (point.price - history.low).toDouble().div(history.high - history.low).toFloat()
    fun selectAt(position: Float, width: Int) { selected = points.indices.minByOrNull { abs(x(points[it]) * width - position) } ?: 0 }
    val point = points[selected]
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Box(Modifier.fillMaxWidth().height(130.dp).testTag("history-canvas").clipToBounds()
            .semantics {
                contentDescription = "نمودار طلای ۱۸ عیار؛ کمینه ${PersianNumberFormatter.formatPrice(history.low)}، بیشینه ${PersianNumberFormatter.formatPrice(history.high)} تومان"
                stateDescription = "${PersianNumberFormatter.formatPrice(point.price)} تومان، ${PersianNumberFormatter.toPersianDigits(point.label)}"
                progressBarRangeInfo = ProgressBarRangeInfo(selected.toFloat(), 0f..points.lastIndex.toFloat(), points.size - 2)
                setProgress { selected = it.roundToInt().coerceIn(points.indices); true }
            }
            .onKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) false else when (event.key) {
                    Key.DirectionLeft -> { selected = (selected - 1).coerceAtLeast(0); true }
                    Key.DirectionRight -> { selected = (selected + 1).coerceAtMost(points.lastIndex); true }
                    Key.MoveHome -> { selected = 0; true }
                    Key.MoveEnd -> { selected = points.lastIndex; true }
                    else -> false
                }
            }.focusable()
            .pointerInput(history) { detectTapGestures { selectAt(it.x, size.width) } }
            .pointerInput(history) { detectDragGestures(onDragStart = { selectAt(it.x, size.width) }, onDrag = { change, _ -> change.consume(); selectAt(change.position.x, size.width) }) }) {
            Canvas(Modifier.fillMaxSize()) {
                fun location(p: GoldHistoryPoint) = Offset(x(p) * size.width, (1f - y(p)) * (size.height - 24.dp.toPx()) + 12.dp.toPx())
                val locations = history.renderedPoints.map(::location)
                val line = Path().apply {
                    moveTo(locations.first().x, locations.first().y)
                    for (i in 0 until locations.lastIndex) {
                        val p0 = locations[i]; val p1 = locations[i + 1]
                        val previous = locations.getOrElse(i - 1) { p0 }; val next = locations.getOrElse(i + 2) { p1 }
                        cubicTo(p0.x + (p1.x - previous.x) / 6, p0.y + (p1.y - previous.y) / 6,
                            p1.x - (next.x - p0.x) / 6, p1.y - (next.y - p0.y) / 6, p1.x, p1.y)
                    }
                }
                val fill = Path().apply { addPath(line); lineTo(locations.last().x, size.height); lineTo(locations.first().x, size.height); close() }
                drawPath(fill, Brush.verticalGradient(listOf(gold.copy(alpha = .35f), gold.copy(alpha = .10f), Color.Transparent)))
                drawPath(line, gold, style = Stroke(2.5.dp.toPx(), cap = StrokeCap.Round))
                val end = locations.last()
                drawCircle(gold.copy(alpha = .3f), 8.dp.toPx(), end); drawCircle(gold, 4.5.dp.toPx(), end)
                val marker = location(point)
                drawLine(gold.copy(alpha = .6f), Offset(marker.x, 6.dp.toPx()), Offset(marker.x, size.height - 6.dp.toPx()), 1.2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f)))
                drawCircle(gold.copy(alpha = .35f), 8.dp.toPx(), marker); drawCircle(gold, 4.5.dp.toPx(), marker); drawCircle(Color.White, 2.5.dp.toPx(), marker)
            }
            BoxWithConstraints(Modifier.fillMaxSize()) {
                val width = if (PersianNumberFormatter.formatPrice(point.price).length > 15) 170.dp else 135.dp
                val height = 44.dp
                val px = maxWidth * x(point); val py = (maxHeight - 24.dp) * (1f - y(point)) + 12.dp
                val left = (px - width / 2).coerceIn(4.dp, (maxWidth - width - 4.dp).coerceAtLeast(4.dp))
                val top = if (py - height - 6.dp >= 2.dp) py - height - 6.dp else (py + 8.dp).coerceAtMost(maxHeight - height - 2.dp)
                Surface(Modifier.offset(left, top).width(width).testTag("history-tooltip"), color = Color.Transparent,
                    shape = RoundedCornerShape(8.dp), border = BorderStroke(.8.dp, gold), shadowElevation = 6.dp) {
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                        Column(Modifier.background(colors.heroCardGradient).padding(horizontal = 7.dp, vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                Box(Modifier.size(5.dp).background(gold, CircleShape))
                                TrendAmount(PersianNumberFormatter.formatPrice(point.price), reduceMotion, size = 10, tint = Color.White, weight = FontWeight.Black)
                                Text("تومان", color = Color.White, fontSize = 10.5.sp)
                            }
                            Text(PersianNumberFormatter.toPersianDigits(point.label), color = Color.White.copy(alpha = .8f), fontSize = 9.sp, maxLines = 1)
                        }
                    }
                }
            }
        }
    }
}
