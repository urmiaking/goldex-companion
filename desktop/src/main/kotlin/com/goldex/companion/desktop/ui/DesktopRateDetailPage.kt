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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.testTag
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
import java.math.BigDecimal

internal fun historyAmount(value: Long?, instrument: BoardInstrument): String = value?.let {
    if (instrument.dollar) formatBoardNumber(BigDecimal.valueOf(it,2).toDouble(),true)
    else PersianNumberFormatter.formatPrice(it)
} ?: "—"
internal fun instrumentLabel(instrument: BoardInstrument) = when (instrument) {
    BoardInstrument.GOLD18 -> "طلای ۱۸ عیار (۷۵۰)"; BoardInstrument.GOLD24 -> "طلای ۲۴ عیار (شمش)"
    BoardInstrument.MELT -> "مظنه آبشده (مثقال)"; BoardInstrument.OUNCE -> "انس جهانی طلا"
    BoardInstrument.EMAMI -> "سکه امامی"; BoardInstrument.BAHAR -> "سکه بهار آزادی"
    BoardInstrument.HALF -> "نیم سکه"; BoardInstrument.QUARTER -> "ربع سکه"; BoardInstrument.GERAMI -> "سکه گرمی"
    BoardInstrument.USD -> "دلار آزاد"; BoardInstrument.AED -> "درهم امارات"; BoardInstrument.TETHER -> "تتر"
}
internal fun horizonLabel(horizon: TimeHorizon) = when (horizon) {
    TimeHorizon.TODAY -> "امروز"; TimeHorizon.ONE_WEEK -> "هفتگی"; TimeHorizon.ONE_MONTH -> "ماهانه"; else -> "سالانه"
}

@Composable internal fun DesktopRateDetailPage(workspaceState: WorkspaceState, workspace: DesktopWorkspace) {
    val detail by workspace.rateDetail.state.collectAsState()
    val board by workspace.ratesBoard.state.collectAsState()
    val row = DesktopRatesBoardPolicy.row(detail.instrument,workspaceState.snapshot,board,workspaceState.now)
    DisposableEffect(workspace) {
        val rates=workspace.ratesBoard.observe(); val history=workspace.rateDetail.observe()
        onDispose { rates.cancel(); history.cancel() }
    }
    PageScroll {
        Column(Modifier.fillMaxWidth().testTag("rate-details-page"),verticalArrangement=Arrangement.spacedBy(20.dp)) {
            RateInstrumentSelector(detail,workspace)
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(14.dp)) {
                Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(5.dp)) {
                    PageTitle(row.instrument.title)
                    Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                        BoardAmount(row.value,row.instrument.dollar,workspaceState.reduceMotion,size=23)
                        DetailText(if (row.instrument.dollar) "دلار آمریکا" else "تومان",muted=true)
                        DayChange(row.daily)
                    }
                    DetailText("${row.source} • ${if (row.instrument == BoardInstrument.AED) board.snapshot?.receivedAt?.let(DesktopPortfolioPolicy::observedTime) ?: "—" else workspaceState.snapshot?.observedAt?.let(DesktopPortfolioPolicy::observedTime) ?: "—"}",muted=true,size=11)
                }
                GoldButton("تابلوی نرخ‌ها",{ workspace.navigate(DesktopDestination.RATES) },isSecondary=true,
                    icon=Icons.Outlined.ArrowForward,modifier=Modifier.width(150.dp).testTag("rate-details-close"))
            }
            RateChartCard(detail,workspaceState.reduceMotion,{ workspace.rateDetail.select(detail.instrument,it) },
                { workspace.rateDetail.select(detail.instrument,force=true) })
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val statistics: @Composable (Modifier) -> Unit = { RateStatisticsCard(detail,workspaceState,row,it) }
                val documents: @Composable (Modifier) -> Unit = { RateDocumentsCard(row,workspaceState,workspace,it) }
                if (maxWidth >= 800.dp) Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min),horizontalArrangement=Arrangement.spacedBy(20.dp)) {
                    statistics(Modifier.weight(1.4f).fillMaxHeight()); documents(Modifier.weight(1f).fillMaxHeight())
                } else Column(verticalArrangement=Arrangement.spacedBy(20.dp)) {
                    statistics(Modifier.fillMaxWidth()); documents(Modifier.fillMaxWidth())
                }
            }
        }
    }
}

@Composable private fun RateInstrumentSelector(detail: DesktopRateDetailState, workspace: DesktopWorkspace) {
    val colors=LocalGoldExColors.current
    val items=listOf(BoardInstrument.GOLD18,BoardInstrument.MELT,BoardInstrument.GOLD24,BoardInstrument.EMAMI,BoardInstrument.HALF,
        BoardInstrument.OUNCE,BoardInstrument.BAHAR,BoardInstrument.QUARTER,BoardInstrument.GERAMI,BoardInstrument.USD,BoardInstrument.AED)
    val scroll=rememberScrollState()
    val density=androidx.compose.ui.platform.LocalDensity.current
    LaunchedEffect(detail.instrument) { scroll.animateScrollTo(with(density) { (items.indexOf(detail.instrument)*155.dp).roundToPx() }) }
    Column(verticalArrangement=Arrangement.spacedBy(6.dp)) {
        Box(Modifier.fillMaxWidth().horizontalScroll(scroll).testTag("rate-instrument-selector")) {
            LuxurySegmentedControl(items,detail.instrument,{ workspace.rateDetail.select(it) },::instrumentLabel,
                Modifier.width((items.size*155).dp),height=44.dp,fontSize=11.5.sp,
                containerColor=colors.surface,activePillColor=if(colors.isDark)colors.surfaceElevated else colors.textMain)
        }
        HorizontalScrollbar(rememberScrollbarAdapter(scroll),Modifier.fillMaxWidth().height(5.dp))
    }
}

@Composable private fun RateChartCard(detail: DesktopRateDetailState, reduceMotion: Boolean, onHorizon: (TimeHorizon)->Unit, onRetry: ()->Unit) {
    val c=LocalGoldExColors.current
    DetailCard(Modifier.fillMaxWidth().testTag("rate-chart-card")) {
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val title: @Composable ()->Unit = { Column(verticalArrangement=Arrangement.spacedBy(4.dp)) {
                DetailText("نمودار روند و نوسانات ${horizonLabel(detail.horizon)}",bold=true,size=16,modifier=Modifier.semantics { heading() })
                DetailText(if (detail.horizon == TimeHorizon.TODAY) "نرخ‌های ثبت‌شده در طول روز بازار" else "نرخ پایانی و دامنه معاملات ثبت‌شده",muted=true,size=11)
            } }
            val selector: @Composable ()->Unit = { LuxurySegmentedControl(RateDetailHorizons,detail.horizon,onHorizon,::horizonLabel,
                Modifier.width(240.dp).testTag("rate-detail-horizon"),height=38.dp,fontSize=11.sp) }
            if (maxWidth >= 600.dp) Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween) { title(); selector() }
            else Column(verticalArrangement=Arrangement.spacedBy(14.dp)) { title(); selector() }
        }
        Spacer(Modifier.height(20.dp))
        val history=detail.active
        if (history == null) Box(Modifier.fillMaxWidth().height(290.dp).testTag("rate-history-empty"),contentAlignment=Alignment.Center) {
            Column(horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(14.dp)) {
                if (detail.loading) CircularProgressIndicator(Modifier.size(28.dp),color=c.goldPrimary,strokeWidth=2.dp)
                else Icon(Icons.Outlined.ShowChart,null,Modifier.size(40.dp),tint=c.textMuted)
                DetailText(if (detail.loading) "در حال دریافت تاریخچه این نماد…" else "تاریخچه معتبر برای این نماد و بازه در دسترس نیست.",muted=true)
                if (!detail.loading) GoldButton("تلاش دوباره",onRetry,isSecondary=true,modifier=Modifier.width(140.dp).testTag("rate-history-retry"))
            }
        } else {
            Surface(shape=RoundedCornerShape(14.dp),color=c.surface,border=BorderStroke(.6.dp,c.goldBorder.copy(alpha=.5f))) {
                Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
                        RateMetricBadge("اوج بازه",history.high,detail.instrument,true)
                        RateMetricBadge("کف بازه",history.low,detail.instrument,false)
                    }
                    DesktopRateHistoryChart(history,reduceMotion)
                }
            }
            Spacer(Modifier.height(10.dp))
            DetailText("منبع تاریخچه: شبکه طلا و ارز (TGJU) • ${DesktopPortfolioPolicy.observedTime(history.receivedAt)}${if (detail.cached || detail.error != null) " • ذخیره‌شده" else ""}",muted=true,size=11,modifier=Modifier.testTag("rate-history-source"))
            if (!history.belongsToToday(System.currentTimeMillis())) DetailText("داده روز گذشته؛ تاریخ نقاط نمودار را بررسی کنید.",muted=true,size=11)
            if (detail.loading) DetailText("تاریخچه در پس‌زمینه تازه می‌شود…",muted=true,size=11)
        }
        detail.error?.let { DetailText(it,color=c.errorRed,size=11,modifier=Modifier.testTag("rate-history-error")) }
    }
}

@Composable private fun RateMetricBadge(label: String,value: Long,instrument: BoardInstrument,high: Boolean) {
    val c=LocalGoldExColors.current;val color=if(high)c.marketGainText else c.errorRed
    Surface(color=color.copy(alpha=.08f),shape=RoundedCornerShape(9.dp),border=BorderStroke(.6.dp,color.copy(alpha=.2f))) {
        DetailText("$label: ${historyAmount(value,instrument)} ${if(instrument.dollar) "دلار" else "تومان"}",color=color,bold=true,size=11,modifier=Modifier.padding(horizontal=10.dp,vertical=7.dp))
    }
}

@Composable private fun RateStatisticsCard(detail: DesktopRateDetailState,state: WorkspaceState,row: BoardRow,modifier: Modifier) {
    val stats=remember(detail.annual,state.now) { DesktopRateStatisticsPolicy.calculate(detail.annual,state.now) }
    val unit=if(detail.instrument.dollar) "دلار" else "تومان"
    val current=DesktopRateStatisticsPolicy.currentAmount(row.value,detail.instrument)
    fun deviation(value: Long?) = DesktopRateStatisticsPolicy.differencePercent(current,value)
    DetailCard(modifier.testTag("rate-statistics")) {
        DetailText("دامنه‌های تاریخی و شاخص‌های آماری",bold=true,size=16,modifier=Modifier.semantics { heading() })
        DetailText("۳۰ روز گذشته • میانگین ساده نرخ پایانی روزهای ثبت‌شده",muted=true,size=11)
        Spacer(Modifier.height(18.dp))
        Row(Modifier.fillMaxWidth().background(LocalGoldExColors.current.surfaceElevated).padding(10.dp)) {
            DetailText("شاخص آماری",Modifier.weight(1.45f),muted=true,size=11)
            DetailText("مقدار",Modifier.weight(1.1f),muted=true,size=11)
            DetailText("نرخ فعلی",Modifier.weight(.9f),muted=true,size=11)
            DetailText("ارزیابی",Modifier.weight(.8f),muted=true,size=11)
        }
        StatisticsRow("میانگین نرخ ۳۰ روزه",stats.average30?.let { "${historyAmount(it,detail.instrument)} $unit" },deviation(stats.average30),"میانگین ساده",state.reduceMotion)
        StatisticsRow("بیشترین نرخ ۳۰ روز",stats.high30?.let { "${historyAmount(it,detail.instrument)} $unit" },deviation(stats.high30),"اوج ثبت‌شده",state.reduceMotion)
        StatisticsRow("کمترین نرخ ۳۰ روز",stats.low30?.let { "${historyAmount(it,detail.instrument)} $unit" },deviation(stats.low30),"کف ثبت‌شده",state.reduceMotion)
        StatisticsRow("تغییر از ابتدای سال شمسی",stats.yearChange?.let { "${if(it>0) "+" else ""}${historyAmount(it,detail.instrument)} $unit" },stats.yearChangePercent,"تغییر تاریخی",state.reduceMotion)
        if(detail.instrument in listOf(BoardInstrument.GOLD18,BoardInstrument.GOLD24,BoardInstrument.MELT)) {
            val premium=DesktopRateStatisticsPolicy.goldPremium(state.snapshot,detail.instrument)
            StatisticsRow("حباب اسمی طلا (معادل ۱۸ عیار)",premium?.let { "${PersianNumberFormatter.formatPrice(it.first)} تومان / گرم" },premium?.second,"انس و دلار",state.reduceMotion)
            DetailText("اختلاف نرخ داخلی با ارزش جهانی طلای ۱۸ عیار؛ بر مبنای انس و دلار همین نرخ",muted=true,size=10,lines=2)
        }
        if (detail.instrument.coin != null) {
            val bubble=row.bubble
            StatisticsRow("حباب محاسبه‌شده سکه",bubble?.let { "${PersianNumberFormatter.formatPrice(it.bubbleAmount.toLong())} تومان" },bubble?.bubblePercent,"ارزش ذاتی",state.reduceMotion)
            DetailText("بر اساس انس، دلار، وزن خالص و حق ضرب؛ نرخ خرید یا توصیه معامله نیست.",muted=true,size=11)
        }
        Spacer(Modifier.height(12.dp))
        DetailText(if(detail.statsLoading) "در حال دریافت آمار این نماد…" else "${PersianNumberFormatter.toPersianDigits(stats.samples30.toString())} روز ثبت‌شده در میانگین؛ بدون وزن‌دهی حجم معاملات",muted=true,size=11)
        detail.annual?.let { DetailText("TGJU • ${DesktopPortfolioPolicy.observedTime(it.receivedAt)}${if(RateHistoryKey(detail.instrument,TimeHorizon.ONE_YEAR) in detail.cachedKeys || detail.statsError != null) " • ذخیره‌شده" else ""}",muted=true,size=10) }
        detail.statsError?.let { DetailText(it,muted=true,size=11) }
    }
}

@Composable private fun StatisticsRow(label: String,value: String?,percent: Double?,evaluation: String,reduceMotion: Boolean) {
    val c=LocalGoldExColors.current
    Row(Modifier.fillMaxWidth().padding(horizontal=10.dp,vertical=14.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(7.dp)) {
        DetailText(label,Modifier.weight(1.45f),size=11,bold=true,lines=2)
        if(reduceMotion) DetailText(value ?: "—",Modifier.weight(1.1f),size=11,bold=true,lines=2)
        else AnimatedPriceTicker(value ?: "—",Modifier.weight(1.1f).clipToBounds(),color=c.textMain,fontSize=11.sp,fontWeight=FontWeight.SemiBold)
        val color=when { percent == null || percent == 0.0 -> c.textMuted; percent>0 -> c.marketGainText; else -> c.errorRed }
        DetailText(percent?.let { "${if(it>0) "+" else ""}${PersianNumberFormatter.formatDouble(it,2)}٪" } ?: "—",Modifier.weight(.9f),size=11,color=color,lines=2)
        DetailText(if(value == null) "بدون داده" else evaluation,Modifier.weight(.8f),size=10,muted=true,lines=2)
    }
    HorizontalDivider(color=c.border.copy(alpha=.3f),thickness=.6.dp)
}

@Composable private fun RateDocumentsCard(row: BoardRow,state: WorkspaceState,workspace: DesktopWorkspace,modifier: Modifier) {
    val c=LocalGoldExColors.current
    DetailCard(modifier.testTag("rate-documents")) {
        DetailText("آخرین اسناد با این نرخ",bold=true,size=16,modifier=Modifier.semantics { heading() })
        DetailText("فاکتورها و حواله‌های ثبت‌شده با نماد انتخابی",muted=true,size=11)
        Column(Modifier.fillMaxWidth().weight(1f,false).padding(vertical=30.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(12.dp)) {
            Icon(Icons.Outlined.ReceiptLong,null,Modifier.size(42.dp),tint=c.goldPrimary)
            DetailText("سند قابل نمایش در دسترس نیست",bold=true)
            DetailText("اتصال اسناد واقعی به نماد در نسخه ویندوز هنوز فراهم نشده است.",muted=true,size=12,lines=3)
        }
        basis(row)?.let { selected ->
            GoldButton("محاسبه با این نرخ",{ workspace.applyQuoteToCalculator(selected) },icon=Icons.Outlined.Calculate,
                enabled=row.value != null && !state.saving,modifier=Modifier.fillMaxWidth().testTag("rate-use-calculator"))
            Spacer(Modifier.height(8.dp))
            DetailText("نرخ در ماشین‌حساب قرار می‌گیرد؛ وزن و سایر ورودی‌های شما حفظ می‌شود.",muted=true,size=11,lines=2)
        }
    }
}

@Composable internal fun DetailCard(modifier: Modifier=Modifier,content: @Composable ColumnScope.()->Unit) {
    val c=LocalGoldExColors.current
    Surface(modifier,color=c.surface,shape=RoundedCornerShape(16.dp),border=BorderStroke(.6.dp,c.goldBorder.copy(alpha=.5f)),shadowElevation=if(c.isDark)0.dp else 2.dp) {
        Column(Modifier.padding(24.dp),content=content)
    }
}
@Composable internal fun DetailText(text: String,modifier: Modifier=Modifier,muted: Boolean=false,bold: Boolean=false,size: Int=13,
    color: androidx.compose.ui.graphics.Color=if(muted)LocalGoldExColors.current.textMuted else LocalGoldExColors.current.textMain,lines: Int=1) {
    Text(text,modifier,color=color,fontSize=size.sp,fontWeight=if(bold)FontWeight.SemiBold else FontWeight.Normal,maxLines=lines,overflow=TextOverflow.Ellipsis)
}
