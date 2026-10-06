package com.goldex.companion.desktop.update

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

enum class WindowsUpdatePhase { IDLE, CHECKING, CURRENT, DOWNLOADING, VERIFYING, READY, RESTARTING, FAILED }
data class WindowsUpdateState(
    val phase: WindowsUpdatePhase = WindowsUpdatePhase.IDLE,
    val release: WindowsRelease? = null,
    val received: Long = 0, val total: Long = 0,
    val dialog: Boolean = false, val error: String? = null
) {
    val busy get() = phase in setOf(WindowsUpdatePhase.CHECKING, WindowsUpdatePhase.DOWNLOADING, WindowsUpdatePhase.VERIFYING, WindowsUpdatePhase.RESTARTING)
}

/** Owns update state only; financial storage and workspace drafts stay with their owners. */
class WindowsUpdater(
    private val gateway: WindowsUpdateGateway,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
) : AutoCloseable {
    private val mutable = MutableStateFlow(WindowsUpdateState())
    val state = mutable.asStateFlow()
    private var prepared: PreparedWindowsUpdate? = null
    private var work: Job? = null
    private var started = false

    @Synchronized fun start() {
        if (started) return
        started = true
        scope.launch { check()?.join(); while (isActive) { delay(6 * 60 * 60 * 1000L); check()?.join() } }
    }

    @Synchronized fun check(): Job? {
        if (state.value.busy) return null
        if (prepared != null) { showDialog(); return null }
        mutable.update { it.copy(phase = WindowsUpdatePhase.CHECKING, error = null) }
        work = scope.launch {
            try {
                val release = gateway.check()
                if (release == null) { mutable.update { it.copy(phase = WindowsUpdatePhase.CURRENT) }; return@launch }
                mutable.update { it.copy(phase = WindowsUpdatePhase.DOWNLOADING, release = release, received = 0, total = release.size) }
                val result = gateway.prepare(release, { received, total -> mutable.update { it.copy(received = received, total = total) } },
                    { mutable.update { it.copy(phase = WindowsUpdatePhase.VERIFYING) } })
                currentCoroutineContext().ensureActive()
                prepared = result
                mutable.update { it.copy(phase = WindowsUpdatePhase.READY, dialog = true) }
            } catch (failure: CancellationException) { throw failure }
            catch (_: Exception) { mutable.update { it.copy(phase = WindowsUpdatePhase.FAILED,
                error = "به‌روزرسانی آماده نشد. اینترنت و فضای دیسک را بررسی کنید؛ بستهٔ ZIP باید کامل در پوشهٔ قابل نوشتن Qirato استخراج شده باشد. نسخهٔ فعلی حفظ شده است.") } }
        }
        return work
    }

    fun showDialog() { mutable.update { it.copy(dialog = true) } }
    fun postpone() { if (state.value.phase != WindowsUpdatePhase.RESTARTING) mutable.update { it.copy(dialog = false) } }

    @Synchronized fun cancelDownload() {
        if (state.value.phase !in setOf(WindowsUpdatePhase.CHECKING, WindowsUpdatePhase.DOWNLOADING, WindowsUpdatePhase.VERIFYING)) return
        val job = work
        mutable.update { it.copy(phase = WindowsUpdatePhase.CHECKING) }
        work = scope.launch { job?.cancelAndJoin(); mutable.update { it.copy(phase = WindowsUpdatePhase.IDLE) } }
    }

    @Synchronized fun restart(shutdown: () -> Unit): Job? {
        val update = prepared ?: return null
        if (state.value.busy) return null
        mutable.update { it.copy(phase = WindowsUpdatePhase.RESTARTING, error = null) }
        work = scope.launch {
            try { gateway.launch(update); shutdown() }
            catch (failure: CancellationException) { throw failure }
            catch (_: Exception) { mutable.update { it.copy(phase = WindowsUpdatePhase.READY, error = "راه‌اندازی مجدد آغاز نشد؛ برنامه باز مانده است. دوباره تلاش کنید.") } }
        }
        return work
    }

    override fun close() { runBlocking { scope.coroutineContext[Job]?.cancelAndJoin() } }
}
