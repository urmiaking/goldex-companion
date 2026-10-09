package com.goldex.companion.desktop.state

import com.goldex.companion.desktop.data.*
import com.goldex.companion.model.MarketHistoryConverter
import com.goldex.companion.model.TimeHorizon
import com.goldex.companion.model.PriceBasisTab
import com.goldex.companion.domain.calculator.GoldCalculationUseCases
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.concurrent.atomic.AtomicLong

data class RateHistoryKey(val instrument: BoardInstrument, val horizon: TimeHorizon)
data class DesktopRateDetailState(val instrument: BoardInstrument = BoardInstrument.GOLD18,
    val horizon: TimeHorizon = TimeHorizon.TODAY,
    val histories: Map<RateHistoryKey,RateHistorySnapshot> = emptyMap(),
    val cachedKeys: Set<RateHistoryKey> = emptySet(), val loading: Boolean = false,
    val statsLoading: Boolean = false, val error: String? = null, val statsError: String? = null) {
    val key get() = RateHistoryKey(instrument,horizon)
    val active get() = histories[key]
    val annual get() = histories[RateHistoryKey(instrument,TimeHorizon.ONE_YEAR)]
    val cached get() = key in cachedKeys
}

/** Instrument identity and timeframe survive composition; late requests cannot replace another route. */
class DesktopRateDetail(private val gateway: DesktopRateHistoryGateway, private val scope: CoroutineScope,
    private val clock: () -> Long = System::currentTimeMillis) {
    private val mutable = MutableStateFlow(DesktopRateDetailState())
    val state = mutable.asStateFlow()
    private val generation = AtomicLong()
    private var request: Job? = null

    @Synchronized fun select(instrument: BoardInstrument, horizon: TimeHorizon = state.value.horizon, force: Boolean = false) {
        require(horizon in RateDetailHorizons && instrument != BoardInstrument.TETHER)
        val ticket = generation.incrementAndGet(); request?.cancel()
        mutable.update { it.copy(instrument=instrument,horizon=horizon,error=null,statsError=null,loading=true,statsLoading=true) }
        request = scope.launch {
            suspend fun acquire(target: TimeHorizon): Boolean {
                val key = RateHistoryKey(instrument,target)
                try {
                    val existing = state.value.histories[key]
                    if (existing == null) {
                        val cached = withContext(Dispatchers.IO) { gateway.cached(instrument,target) }
                        if (cached != null) {
                            require(cached.instrument == instrument && cached.horizon == target && RateHistorySnapshot.valid(cached,clock()))
                            synchronized(this@DesktopRateDetail) { if (generation.get() == ticket)
                                mutable.update { it.copy(histories=it.histories+(key to cached),cachedKeys=it.cachedKeys+key) } }
                        }
                    }
                    val current = state.value.histories[key]
                    if (!force && current != null && key !in state.value.cachedKeys && current.belongsToToday(clock()) && clock()-current.receivedAt in 0..300_000) return true
                    val fresh = gateway.load(instrument,target)
                    require(fresh.instrument == instrument && fresh.horizon == target && RateHistorySnapshot.valid(fresh,clock()))
                    synchronized(this@DesktopRateDetail) { if (generation.get() == ticket)
                        mutable.update { it.copy(histories=it.histories+(key to fresh),cachedKeys=it.cachedKeys-key) } }
                    return true
                } catch (cancelled: CancellationException) { throw cancelled }
                catch (_: Exception) { return false }
            }
            val chartOk = acquire(horizon)
            synchronized(this@DesktopRateDetail) { if (generation.get() == ticket) mutable.update { it.copy(loading=false,
                error=if (chartOk) null else "تاریخچه تازه دریافت نشد؛ داده ذخیره‌شده با تاریخ اصلی نمایش داده می‌شود.") } }
            val statsOk = horizon == TimeHorizon.ONE_YEAR && chartOk || acquire(TimeHorizon.ONE_YEAR)
            synchronized(this@DesktopRateDetail) { if (generation.get() == ticket) mutable.update { it.copy(statsLoading=false,
                statsError=if (statsOk) null else "آمار تازه دریافت نشد؛ آمار ذخیره‌شده با زمان اصلی حفظ شده است.") } }
        }
    }
    fun observe(): Job = scope.launch {
        if(!state.value.loading) select(state.value.instrument)
        while (isActive) { delay(300_000); select(state.value.instrument,force=true) }
    }
}

data class DesktopRateStatistics(val average30: Long?, val high30: Long?, val low30: Long?,
    val samples30: Int, val yearChange: Long?, val yearChangePercent: Double?)

/** Facts from daily OHLC only. No support/resistance, risk grade or volume weighting is invented. */
object DesktopRateStatisticsPolicy {
    /** Nominal premium per gram of 18k, against ounce/USD from the same valuation snapshot. */
    fun goldPremium(snapshot: MarketSnapshot?, instrument: BoardInstrument): Pair<Long,Double>? {
        if(snapshot == null || snapshot.kind == QuoteKind.MANUAL) return null
        val rates=snapshot.rates
        val basis=when(instrument) { BoardInstrument.GOLD18 -> PriceBasisTab.K18; BoardInstrument.GOLD24 -> PriceBasisTab.K24; BoardInstrument.MELT -> PriceBasisTab.MESGHAL; else -> return null }
        val raw=instrument.primary(rates)?.toLong() ?: return null
        if(rates.usd !in 1..1_000_000_000_000L || !rates.ons.isFinite() || rates.ons !in 0.01..10_000_000.0) return null
        val spot=GoldCalculationUseCases.toSpotPrice18k(raw,basis)
        val intrinsic=BigDecimal.valueOf(rates.ons).multiply(BigDecimal.valueOf(rates.usd)).multiply(BigDecimal("0.75"))
            .divide(BigDecimal("31.1035"),0,RoundingMode.HALF_UP).longValueExact()
        if(intrinsic <= 0) return null
        return (spot-intrinsic) to ((spot-intrinsic).toDouble()/intrinsic*100)
    }
    fun currentAmount(value: Double?, instrument: BoardInstrument): Long? = value?.takeIf { it.isFinite() && it > 0 }?.let {
        if(instrument.dollar) BigDecimal.valueOf(it).movePointRight(2).setScale(0,RoundingMode.HALF_UP).longValueExact() else it.toLong()
    }
    fun differencePercent(current: Long?, reference: Long?): Double? = if(current != null && reference != null && reference > 0)
        (current-reference).toDouble()/reference*100 else null
    fun calculate(annual: RateHistorySnapshot?, now: Long): DesktopRateStatistics {
        if (annual == null || annual.horizon != TimeHorizon.ONE_YEAR || !RateHistorySnapshot.valid(annual,now))
            return DesktopRateStatistics(null,null,null,0,null,null)
        val today = RateHistorySnapshot.day(now)
        val start = today.minusDays(29)
        val monthly = annual.points.filter { !RateHistorySnapshot.day(it.at).isBefore(start) }
        val enough = monthly.size >= 2 && RateHistorySnapshot.day(annual.points.first().at) <= start
        val average = if (enough) monthly.fold(BigDecimal.ZERO) { sum,p -> sum+BigDecimal.valueOf(p.price) }
            .divide(BigDecimal.valueOf(monthly.size.toLong()),0,RoundingMode.HALF_UP).longValueExact() else null
        fun shamsiYear(at: Long): Int { val d=RateHistorySnapshot.day(at); return MarketHistoryConverter.gregorianToShamsi(d.year,d.monthValue,d.dayOfMonth).first }
        val currentYear=shamsiYear(now)
        val baseline=annual.points.lastOrNull { shamsiYear(it.at) == currentYear-1 }
        val first=annual.points.firstOrNull { shamsiYear(it.at) == currentYear }
        val validYear = baseline != null && first != null && RateHistorySnapshot.day(baseline.at).plusDays(10) >= RateHistorySnapshot.day(first.at)
        val delta = if (validYear) annual.points.last().price-baseline!!.price else null
        return DesktopRateStatistics(average,if (enough) monthly.maxOf { it.high } else null,
            if (enough) monthly.minOf { it.low } else null,if (enough) monthly.size else 0,
            delta,delta?.toDouble()?.div(baseline!!.price)?.times(100))
    }
}
