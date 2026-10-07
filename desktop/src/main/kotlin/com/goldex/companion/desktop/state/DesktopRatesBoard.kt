package com.goldex.companion.desktop.state

import com.goldex.companion.data.PriceSource
import com.goldex.companion.desktop.data.*
import com.goldex.companion.domain.calculator.GoldCalculationUseCases
import com.goldex.companion.model.CoinBubbleResult
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlin.math.abs
import kotlin.math.roundToLong

data class DesktopRatesBoardState(val snapshot: BoardSnapshot? = null, val loading: Boolean = false,
    val cached: Boolean = true, val error: String? = null)

class DesktopRatesBoard(private val gateway: DesktopRatesBoardGateway, private val scope: CoroutineScope,
    private val clock: () -> Long = System::currentTimeMillis) {
    private val mutable = MutableStateFlow(try { DesktopRatesBoardState(snapshot = gateway.cached()) }
        catch (e: Exception) { DesktopRatesBoardState(error = e.message ?: "کش نرخ قابل خواندن نیست") })
    val state = mutable.asStateFlow()
    private var request: Job? = null
    /** The screen owns this subscription; scheduling stays on the workspace scope, away from Compose's frame clock. */
    fun observe(autoRefresh: Boolean): Job = scope.launch {
        refresh()
        if (autoRefresh) while (isActive) { delay(300_000); refresh(force = true) }
    }
    @Synchronized fun refresh(force: Boolean = false) {
        if (request?.isActive == true) return
        if (!force && !state.value.cached && state.value.snapshot?.receivedAt?.let { clock() - it in 0..300_000 } == true) return
        mutable.update { it.copy(loading = true, error = null) }
        request = scope.launch {
            try { val next = gateway.load(state.value.snapshot); mutable.update { it.copy(snapshot = next, loading = false, cached = false) } }
            catch (e: CancellationException) { throw e }
            catch (_: Exception) { mutable.update { it.copy(loading = false, cached = true, error = "جزئیات تازه دریافت نشد؛ اطلاعات ذخیره شده با زمان اصلی حفظ شده است") } }
        }
    }
}

data class BoardRow(val instrument: BoardInstrument, val value: Double?, val daily: BoardDayQuote?,
    val history: List<Double>, val source: String, val bubble: CoinBubbleResult?)

/** Project the selected financial quote; independent TGJU extras cannot override it. */
internal object DesktopRatesBoardPolicy {
    fun row(instrument: BoardInstrument, selected: MarketSnapshot?, board: DesktopRatesBoardState, now: Long): BoardRow {
        val extra = instrument == BoardInstrument.AED || instrument == BoardInstrument.TETHER
        val reference = board.snapshot?.quotes?.get(instrument)
        val value = if (extra) reference?.price else instrument.primary(selected?.rates)
        val sameQuote = extra || (selected?.rates?.source == PriceSource.TGJU && selected.kind != QuoteKind.MANUAL &&
            value != null && reference != null && abs(value - reference.price) < 0.00001)
        val day = reference?.takeIf { sameQuote && board.snapshot?.today(now) == true }
        val history = board.snapshot?.history?.get(instrument)?.takeIf { sameQuote && sameBoardDay(it.receivedAt, now) }?.prices.orEmpty()
        val rates = selected?.rates
        val bubble = instrument.coin?.let { coin -> if (value != null && rates != null && rates.usd > 0 && rates.ons > 0 && rates.ons.isFinite())
            GoldCalculationUseCases.calculateCoinBubble(coin, value, rates.usd, rates.ons) else null }
        return BoardRow(instrument,value,day,history,
            if (extra) "TGJU • ${if (board.cached || board.snapshot?.receivedAt?.let { now - it !in 0..300_000 } != false) "ذخیره شده" else "دریافت آنلاین"}"
            else selected?.label(now) ?: "ناموجود", bubble)
    }
    fun bubbleAmount(row: BoardRow): Long? = row.bubble?.bubbleAmount?.roundToLong()
}
