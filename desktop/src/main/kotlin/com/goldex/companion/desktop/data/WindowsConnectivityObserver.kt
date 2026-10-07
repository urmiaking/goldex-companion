package com.goldex.companion.desktop.data

import com.goldex.companion.data.ConnectionStatus
import com.goldex.companion.data.ConnectivityObserver
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.nio.file.Path
import java.util.concurrent.TimeUnit

/** Windows reports Internet reachability independently of market prices/providers. */
class WindowsConnectivityObserver(
    private val scope: CoroutineScope,
    private val readFlags: () -> Int = ::readWindowsConnectivity,
    private val waitForNextCheck: suspend () -> Unit = { delay(30_000) }
) : ConnectivityObserver {
    private val mutable = MutableStateFlow(ConnectionStatus.OFFLINE)
    override val status = mutable.asStateFlow()
    private var started = false

    @Synchronized fun start() {
        if (started) return
        started = true
        scope.launch {
            while (isActive) {
                val flags = withContext(Dispatchers.IO) { readFlags() }
                mutable.value = statusFor(flags)
                waitForNextCheck()
            }
        }
    }

    companion object {
        // NLM_CONNECTIVITY_IPV4_INTERNET | NLM_CONNECTIVITY_IPV6_INTERNET.
        fun statusFor(flags: Int) = if (flags and 0x440 != 0) ConnectionStatus.ONLINE else ConnectionStatus.OFFLINE
    }
}

internal fun readWindowsConnectivity(): Int {
    if (!System.getProperty("os.name").startsWith("Windows", ignoreCase = true)) return 0
    val shell = Path.of(System.getenv("SystemRoot") ?: "C:\\Windows", "System32/WindowsPowerShell/v1.0/powershell.exe")
    val script = """
        ${'$'}ErrorActionPreference = 'Stop'
        ${'$'}manager = [Activator]::CreateInstance([Type]::GetTypeFromCLSID([Guid]'DCB00C01-570F-4A9B-8D69-199FDBA5723B'))
        try { [int]${'$'}manager.GetConnectivity() }
        finally { [void][Runtime.InteropServices.Marshal]::FinalReleaseComObject(${'$'}manager) }
    """.trimIndent()
    val process = try {
        ProcessBuilder(shell.toString(), "-NoProfile", "-NonInteractive", "-WindowStyle", "Hidden", "-Command", script)
            .redirectError(ProcessBuilder.Redirect.DISCARD).start()
    } catch (_: Exception) { return 0 }
    return try {
        if (!process.waitFor(5, TimeUnit.SECONDS) || process.exitValue() != 0) 0
        else process.inputStream.use { it.readNBytes(32).toString(Charsets.UTF_8).trim().toIntOrNull() ?: 0 }
    } finally {
        if (process.isAlive) process.destroyForcibly()
        process.inputStream.close()
        process.outputStream.close()
        process.errorStream.close()
    }
}
