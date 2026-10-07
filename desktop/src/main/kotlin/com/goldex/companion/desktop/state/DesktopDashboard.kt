package com.goldex.companion.desktop.state

import com.goldex.companion.desktop.data.*
import com.goldex.companion.model.TimeHorizon
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.time.Instant
import java.time.ZoneId
import java.util.concurrent.atomic.AtomicLong

data class DesktopDashboardState(
    val horizon: TimeHorizon = TimeHorizon.TODAY,
    val history: Map<TimeHorizon, GoldHistorySnapshot> = emptyMap(),
    val loading: Boolean = false,
    val error: String? = null
) {
    fun active(now: Long) = history[horizon]?.takeIf { it.belongsToToday(now) }
}

/** Owns chart requests and cache. Page composition never fetches or fabricates financial data. */
class DesktopDashboard(private val gateway: DesktopGoldHistoryGateway,
    private val scope: CoroutineScope, private val clock: () -> Long = System::currentTimeMillis) {
    private val mutable = MutableStateFlow(DesktopDashboardState())
    val state = mutable.asStateFlow()
    private var request: Job? = null
    private val generation = AtomicLong()

    @Synchronized fun select(horizon: TimeHorizon, force: Boolean = false) {
        require(horizon in listOf(TimeHorizon.TODAY, TimeHorizon.ONE_WEEK, TimeHorizon.ONE_MONTH))
        mutable.update { it.copy(horizon = horizon, error = null) }
        val cached = state.value.active(clock())
        if (!force && cached != null && clock() - cached.receivedAt in 0..300_000) {
            generation.incrementAndGet(); request?.cancel(); mutable.update { it.copy(loading = false) }; return
        }
        val ticket = generation.incrementAndGet()
        request?.cancel()
        mutable.update { it.copy(loading = true) }
        request = scope.launch {
            try {
                val result = gateway.load(horizon)
                require(result.horizon == horizon && result.points.size >= 2)
                synchronized(this@DesktopDashboard) {
                    if (generation.get() == ticket) mutable.update { it.copy(history = it.history + (horizon to result), loading = false) }
                }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { synchronized(this@DesktopDashboard) {
                if (generation.get() == ticket) mutable.update { it.copy(loading = false,
                    error = "تاریخچه تازه دریافت نشد؛ اتصال اینترنت را بررسی و دوباره تلاش کنید.") }
            } }
        }
    }

    companion object {
        fun greeting(now: Long): String = when (Instant.ofEpochMilli(now).atZone(ZoneId.of("Asia/Tehran")).hour) {
            in 5..10 -> "صبح بخیر"
            in 11..16 -> "روز بخیر"
            in 17..20 -> "عصر بخیر"
            else -> "شب بخیر"
        }
    }
}
