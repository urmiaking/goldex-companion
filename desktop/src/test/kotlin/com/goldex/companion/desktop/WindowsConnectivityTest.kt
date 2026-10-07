package com.goldex.companion.desktop

import com.goldex.companion.data.ConnectionStatus
import com.goldex.companion.desktop.data.WindowsConnectivityObserver
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.first
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.*

class WindowsConnectivityTest {
    @Test fun localNetworkDoesNotClaimInternetAndEitherInternetProtocolIsOnline() {
        for (flags in listOf(0, 1, 2, 0x10, 0x20, 0x200, 0x220))
            assertEquals(ConnectionStatus.OFFLINE, WindowsConnectivityObserver.statusFor(flags))
        for (flags in listOf(0x40, 0x400, 0x440, 0x462))
            assertEquals(ConnectionStatus.ONLINE, WindowsConnectivityObserver.statusFor(flags))
    }

    @Test fun startupAndPeriodicObservationChangeConnectionWithoutMarketCalls() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val flags = AtomicInteger(0x40)
        val calls = AtomicInteger()
        val next = Channel<Unit>()
        val observed = Channel<Unit>(Channel.UNLIMITED)
        val observer = WindowsConnectivityObserver(scope, { calls.incrementAndGet(); flags.get() }, { observed.send(Unit); next.receive() })
        try {
            observer.start(); observer.start()
            withTimeout(3000) { observed.receive() }
            assertEquals(1, calls.get())
            assertEquals(ConnectionStatus.ONLINE, observer.status.value)
            flags.set(0x20); next.send(Unit)
            withTimeout(3000) { observer.status.first { it == ConnectionStatus.OFFLINE } }
            flags.set(0x400); next.send(Unit)
            withTimeout(3000) { observer.status.first { it == ConnectionStatus.ONLINE } }
            assertEquals(3, calls.get())
        } finally { scope.coroutineContext[Job]!!.cancelAndJoin() }
    }
}
