package com.goldex.companion.desktop.update

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

enum class WindowsUpdatePhase { IDLE, CHECKING, CURRENT, AVAILABLE, DOWNLOADING, VERIFYING, READY, RESTARTING, FAILED }
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
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
    private val waitForNextCheck: suspend (Long) -> Unit = { delay(it) }
) : AutoCloseable {
    private val mutable = MutableStateFlow(WindowsUpdateState())
    val state = mutable.asStateFlow()
    private var prepared: PreparedWindowsUpdate? = null
    private var work: Job? = null
    private var started = false

    @Synchronized fun start() {
        if (started) return
        started = true
        scope.launch {
            check(background = true)?.join()
            while (isActive) { waitForNextCheck(CHECK_INTERVAL_MS); check(background = true)?.join() }
        }
    }

    @Synchronized fun check(background: Boolean = false): Job? {
        if (state.value.busy) return null
        if (prepared != null) { if (!background) showDialog(); return null }
        mutable.update { it.copy(phase = WindowsUpdatePhase.CHECKING, error = null) }
        work = scope.launch {
            try {
                val release = gateway.check()
                if (release == null) { mutable.update { it.copy(phase = WindowsUpdatePhase.CURRENT, release = null, dialog = false) }; return@launch }
                mutable.update { it.copy(phase = WindowsUpdatePhase.AVAILABLE, release = release, received = 0, total = release.size, dialog = false) }
            } catch (failure: CancellationException) { throw failure }
            catch (_: Exception) { mutable.update { it.copy(phase = WindowsUpdatePhase.FAILED,
                error = "بررسی به‌روزرسانی انجام نشد؛ اتصال اینترنت را بررسی کنید و دوباره تلاش کنید.") } }
        }
        return work
    }

    /** Download is a user intent; discovering a release never interrupts the workspace. */
    @Synchronized fun download(): Job? {
        if (state.value.busy || prepared != null) return null
        val release = state.value.release ?: return null
        mutable.update { it.copy(phase = WindowsUpdatePhase.DOWNLOADING, received = 0, total = release.size, dialog = false, error = null) }
        work = scope.launch {
            try {
                val result = gateway.prepare(release, { received, total -> mutable.update { it.copy(received = received, total = total) } },
                    { mutable.update { it.copy(phase = WindowsUpdatePhase.VERIFYING) } })
                val context = currentCoroutineContext()
                synchronized(this@WindowsUpdater) {
                    context.ensureActive()
                    prepared = result
                    mutable.update { it.copy(phase = WindowsUpdatePhase.READY, dialog = false) }
                }
            } catch (failure: CancellationException) { throw failure }
            catch (_: Exception) { mutable.update { it.copy(phase = WindowsUpdatePhase.FAILED,
                error = "به‌روزرسانی آماده نشد. اینترنت و فضای دیسک را بررسی کنید؛ پوشه برنامه باید قابل نوشتن باشد. نسخه فعلی حفظ شده است.") } }
        }
        return work
    }

    fun showDialog() { if (state.value.phase == WindowsUpdatePhase.READY) mutable.update { it.copy(dialog = true) } }
    fun activate() { when {
        state.value.phase == WindowsUpdatePhase.READY -> showDialog()
        state.value.release != null && state.value.phase in setOf(WindowsUpdatePhase.AVAILABLE, WindowsUpdatePhase.FAILED) -> download()
        !state.value.busy -> check()
    } }
    fun postpone() { if (state.value.phase != WindowsUpdatePhase.RESTARTING) mutable.update { it.copy(dialog = false) } }

    @Synchronized fun cancelDownload() {
        if (state.value.phase !in setOf(WindowsUpdatePhase.CHECKING, WindowsUpdatePhase.DOWNLOADING, WindowsUpdatePhase.VERIFYING)) return
        val job = work
        job?.cancel()
        mutable.update { it.copy(phase = WindowsUpdatePhase.CHECKING) }
        work = scope.launch { job?.join(); mutable.update { it.copy(phase = if (it.release != null) WindowsUpdatePhase.AVAILABLE else WindowsUpdatePhase.IDLE, received = 0, dialog = false) } }
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

    companion object { const val CHECK_INTERVAL_MS = 5 * 60 * 1000L }
}
