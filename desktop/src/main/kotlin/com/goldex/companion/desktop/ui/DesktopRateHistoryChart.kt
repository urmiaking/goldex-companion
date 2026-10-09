package com.goldex.companion.desktop.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.*
import com.goldex.companion.desktop.data.*
import com.goldex.companion.model.PersianNumberFormatter
import com.goldex.companion.ui.theme.*
import kotlin.math.abs
import kotlin.math.roundToInt

/** RTL chronology, real timestamps, bounded rendering, pointer and keyboard access to the same values. */
@OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
@Composable internal fun DesktopRateHistoryChart(history: RateHistorySnapshot,reduceMotion: Boolean) {
    val c=LocalGoldExColors.current; val gold=if(c.isDark)c.goldSecondary else c.goldPrimary
    val points=history.points
    var selectedAt by remember(history.instrument,history.horizon) { mutableStateOf<Long?>(null) }
    val selected=points.indexOfFirst { it.at == selectedAt }.takeIf { it >= 0 } ?: points.lastIndex
    var focused by remember { mutableStateOf(false) }
    val point=points[selected]
    val insetPixels=with(androidx.compose.ui.platform.LocalDensity.current) { 12.dp.toPx() }
    val span=(points.last().at-points.first().at).coerceAtLeast(1)
    fun x(p: RateHistoryPoint) = 1f-(p.at-points.first().at).toDouble().div(span).toFloat()
    fun y(p: RateHistoryPoint) = if(history.high==history.low) .5f else (p.price-history.low).toDouble().div(history.high-history.low).toFloat()
    fun selectAt(position: Float,width: Int) { selectedAt=points.minByOrNull { abs(x(it)*(width-2*insetPixels)+insetPixels-position) }?.at }
    val description="${instrumentLabel(history.instrument)}؛ ${historyAmount(point.price,history.instrument)} ${if(history.instrument.dollar) "دلار" else "تومان"}، ${PersianNumberFormatter.toPersianDigits(point.label)}"
    Column(verticalArrangement=Arrangement.spacedBy(10.dp)) {
        BoxWithConstraints(Modifier.fillMaxWidth().height(260.dp).testTag("rate-history-chart").clipToBounds()
            .border(if(focused)1.dp else 0.dp,if(focused)gold else Color.Transparent,RoundedCornerShape(6.dp))
            .semantics {
                contentDescription="نمودار ${instrumentLabel(history.instrument)}؛ کف ${historyAmount(history.low,history.instrument)} و اوج ${historyAmount(history.high,history.instrument)}"
                stateDescription=description
                progressBarRangeInfo=ProgressBarRangeInfo(selected.toFloat(),0f..points.lastIndex.toFloat(),(points.size-2).coerceAtLeast(0))
                setProgress { selectedAt=points[it.roundToInt().coerceIn(points.indices)].at; true }
            }.onFocusChanged { focused=it.isFocused }
            .onKeyEvent { e -> if(e.type != KeyEventType.KeyDown) false else when(e.key) {
                Key.DirectionLeft -> { selectedAt=points[(selected+1).coerceAtMost(points.lastIndex)].at;true }
                Key.DirectionRight -> { selectedAt=points[(selected-1).coerceAtLeast(0)].at;true }
                Key.MoveHome -> { selectedAt=points.first().at;true }; Key.MoveEnd -> { selectedAt=points.last().at;true }; else -> false
            } }.focusable()
            .pointerInput(history) { detectTapGestures { selectAt(it.x,size.width) } }
            .pointerInput(history) { awaitPointerEventScope { while(true) {
                val event=awaitPointerEvent()
                if(event.type == PointerEventType.Move) event.changes.firstOrNull()?.let { selectAt(it.position.x,size.width) }
            } } }) {
            Canvas(Modifier.fillMaxSize()) {
                val inset=12.dp.toPx();val width=(size.width-2*inset).coerceAtLeast(1f);val height=size.height-32.dp.toPx()
                fun location(p: RateHistoryPoint)=Offset(inset+x(p)*width,16.dp.toPx()+(1-y(p))*height)
                repeat(4) { i -> val gy=16.dp.toPx()+height*i/3
                    drawLine(c.border.copy(alpha=.45f),Offset(inset,gy),Offset(size.width-inset,gy),strokeWidth=.6.dp.toPx(),pathEffect=PathEffect.dashPathEffect(floatArrayOf(5.dp.toPx(),5.dp.toPx()))) }
                val positions=history.renderedPoints.map(::location)
                val line=Path().apply {
                    moveTo(positions.first().x,positions.first().y)
                    for(i in 0 until positions.lastIndex) {
                        val a=positions[i];val b=positions[i+1]
                        val previous=positions.getOrElse(i-1) { a };val next=positions.getOrElse(i+2) { b }
                        val low=minOf(a.y,b.y);val high=maxOf(a.y,b.y)
                        // Neighbor tangents are clamped to actual endpoints, keeping extrema truthful.
                        cubicTo(a.x+(b.x-a.x)/3,(a.y+(b.y-previous.y)/6).coerceIn(low,high),
                            b.x-(b.x-a.x)/3,(b.y-(next.y-a.y)/6).coerceIn(low,high),b.x,b.y)
                    }
                }
                val fill=Path().apply { addPath(line);lineTo(positions.last().x,size.height);lineTo(positions.first().x,size.height);close() }
                drawPath(fill,Brush.verticalGradient(listOf(gold.copy(alpha=.3f),gold.copy(alpha=.06f),Color.Transparent)))
                drawPath(line,gold,style=Stroke(3.dp.toPx(),cap=StrokeCap.Round))
                val peak=location(points.maxBy { it.price });drawCircle(c.surface,7.dp.toPx(),peak);drawCircle(gold,4.5.dp.toPx(),peak)
                val latest=location(points.last());drawCircle(c.surface,7.dp.toPx(),latest);drawCircle(c.marketGainText,4.5.dp.toPx(),latest)
                if(selectedAt != null) {
                    val p=location(point)
                    drawLine(gold.copy(alpha=.5f),Offset(p.x,0f),Offset(p.x,size.height),strokeWidth=1.dp.toPx(),pathEffect=PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(),4.dp.toPx())))
                    drawCircle(c.surface,6.dp.toPx(),p);drawCircle(gold,4.dp.toPx(),p)
                }
            }
            if(selectedAt != null || focused) Surface(Modifier.align(Alignment.TopCenter).testTag("rate-history-tooltip"),color=c.surfaceElevated,
                shape=RoundedCornerShape(9.dp),border=BorderStroke(.6.dp,gold.copy(alpha=.5f))) {
                DetailText(description,Modifier.padding(horizontal=12.dp,vertical=7.dp),size=11,bold=true)
            }
        }
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
            listOf(points.lastIndex,points.lastIndex*3/4,points.lastIndex/2,points.lastIndex/4,0).distinct().forEach { index ->
                DetailText(PersianNumberFormatter.toPersianDigits(points[index].label),muted=true,size=10)
            }
        }
        DetailText(description,Modifier.testTag("rate-history-selected"),muted=true,size=11,lines=2)
    }
}
