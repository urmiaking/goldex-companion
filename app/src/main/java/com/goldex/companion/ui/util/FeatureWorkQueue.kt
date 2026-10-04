package com.goldex.companion.ui.util

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** Serial feature operations; financial transactions stay on one worker thread without suspension. */
internal class FeatureWorkQueue(
    private val scope: CoroutineScope,
    private val dispatcher: CoroutineDispatcher,
    private val onBusy: (Boolean) -> Unit,
    private val onError: (Exception) -> Unit
) {
    private val mutex = Mutex()
    private var pending = 0

    fun submit(onSuccess: () -> Unit = {}, onFailure: (Exception) -> Unit = onError, action: () -> Unit): Job {
        pending++
        onBusy(true)
        return scope.launch {
            try {
                mutex.withLock {
                    try {
                        withContext(dispatcher) { action() }
                        onSuccess()
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (error: Exception) {
                        onFailure(error)
                    }
                }
            } finally {
                pending--
                onBusy(pending > 0)
            }
        }
    }
}
