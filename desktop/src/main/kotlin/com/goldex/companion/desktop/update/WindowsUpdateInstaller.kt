package com.goldex.companion.desktop.update

import kotlinx.coroutines.*
import org.json.JSONObject
import java.nio.file.*
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlin.coroutines.coroutineContext

data class PreparedWindowsUpdate(val release: WindowsRelease, val installRoot: Path, val stagingRoot: Path, val bundle: Path)

interface WindowsUpdateGateway {
    suspend fun check(): WindowsRelease?
    suspend fun prepare(release: WindowsRelease, progress: (Long, Long) -> Unit, verifying: () -> Unit): PreparedWindowsUpdate
    suspend fun launch(update: PreparedWindowsUpdate)
}

class WindowsUpdateInstaller(
    private val version: WindowsVersion,
    private val dataDirectory: Path,
    private val installRoot: Path? = packagedRoot(),
    private val network: WindowsUpdateNetwork = WindowsUpdateNetwork()
) : WindowsUpdateGateway {
    // Development images run directly; automatic installation belongs to MSI installations.
    override suspend fun check() = if (installRoot?.let { Files.isRegularFile(it.resolve(".qirato-msi")) } == true)
        network.check(version) else null

    override suspend fun prepare(release: WindowsRelease, progress: (Long, Long) -> Unit, verifying: () -> Unit): PreparedWindowsUpdate = withContext(Dispatchers.IO) {
        val root = checkNotNull(installRoot) { "Run the installed application" }.toAbsolutePath().normalize()
        require(Files.isRegularFile(root.resolve(".qirato-msi")) && Files.isRegularFile(root.resolve("Qirato.exe")) &&
            Files.isRegularFile(root.resolve("app/Qirato.cfg")) && Files.isDirectory(root.resolve("runtime"))) { "Incomplete installation" }
        require(root.fileName.toString().equals("Qirato", ignoreCase = true) && root.parent != null && root.parent.parent != null)
        require(root.toRealPath() == root && !dataDirectory.toAbsolutePath().normalize().startsWith(root)) { "Unsupported installation path" }
        val staging = Files.createDirectory(root.parent.resolve(".qirato-update-${UUID.randomUUID()}"))
        // Preserve the MSI source filename for repair/reinstallation.
        val installer = staging.resolve(release.installerName)
        network.download(release, installer, progress)
        coroutineContext.ensureActive(); verifying()
        val update = PreparedWindowsUpdate(release, root, staging, installer)
        val (script, plan) = writeHelper(update)
        runVerification(ProcessBuilder(powershell().toString(), "-NoProfile", "-NonInteractive", "-WindowStyle", "Hidden", "-ExecutionPolicy", "Bypass",
            "-File", script.toString(), "-Plan", plan.toString(), "-ValidateOnly"), staging.resolve("msi-verify.log"))
        update
    }

    override suspend fun launch(update: PreparedWindowsUpdate): Unit = withContext(Dispatchers.IO) {
        val root = update.installRoot.toAbsolutePath().normalize()
        require(root == installRoot?.toAbsolutePath()?.normalize())
        require(update.stagingRoot.parent == root.parent && update.bundle == update.stagingRoot.resolve(update.release.installerName))
        val (script, plan) = writeHelper(update)
        Files.deleteIfExists(update.stagingRoot.resolve("ready"))
        val process = ProcessBuilder(powershell().toString(), "-NoProfile", "-NonInteractive", "-WindowStyle", "Hidden", "-ExecutionPolicy", "Bypass", "-File", script.toString(), "-Plan", plan.toString())
            .directory(update.stagingRoot.toFile()).redirectErrorStream(true).redirectOutput(update.stagingRoot.resolve("helper.log").toFile()).start()
        try {
            val ready = update.stagingRoot.resolve("ready")
            val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(15)
            while (!Files.exists(ready) && process.isAlive && System.nanoTime() < deadline) { delay(100); coroutineContext.ensureActive() }
            require(Files.exists(ready) && process.isAlive) { "Update helper could not start" }
        } catch (failure: Exception) { process.destroyForcibly(); throw failure }
    }

    private fun writeHelper(update: PreparedWindowsUpdate): Pair<Path, Path> {
        val script = update.stagingRoot.resolve("install.ps1")
        checkNotNull(javaClass.getResourceAsStream("/update/install-msi.ps1")).use { Files.copy(it, script, StandardCopyOption.REPLACE_EXISTING) }
        val plan = update.stagingRoot.resolve("plan.json")
        val json = JSONObject().put("installRoot", update.installRoot.toString()).put("stagingRoot", update.stagingRoot.toString())
            .put("bundle", update.bundle.toString()).put("processId", ProcessHandle.current().pid())
            .put("version", update.release.version.toString()).put("dataDirectory", dataDirectory.toAbsolutePath().normalize().toString())
            .put("sha256", update.release.sha256).put("size", update.release.size)
        Files.writeString(plan, json.toString(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)
        return script to plan
    }

    private suspend fun runVerification(builder: ProcessBuilder, log: Path) {
        val process = builder.redirectErrorStream(true).redirectOutput(log.toFile()).start()
        try {
            val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(60)
            while (process.isAlive && System.nanoTime() < deadline) { delay(100); coroutineContext.ensureActive() }
            require(!process.isAlive && process.exitValue() == 0) { "Installer validation failed" }
        } finally { if (process.isAlive) process.destroyForcibly() }
    }

    companion object {
        private fun powershell() = Path.of(System.getenv("SystemRoot") ?: "C:\\Windows", "System32", "WindowsPowerShell", "v1.0", "powershell.exe")
        fun packagedRoot(): Path? {
            val launcher = System.getProperty("jpackage.app-path") ?: ProcessHandle.current().info().command().orElse(null) ?: return null
            val path = Path.of(launcher).toAbsolutePath().normalize()
            return path.parent.takeIf { path.fileName.toString().equals("Qirato.exe", ignoreCase = true) }
        }
    }
}
