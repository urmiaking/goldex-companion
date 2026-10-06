package com.goldex.companion.desktop.update

import kotlinx.coroutines.*
import org.json.JSONObject
import java.nio.file.*
import java.util.UUID
import java.util.concurrent.TimeUnit
import java.util.zip.ZipFile
import kotlin.coroutines.coroutineContext

object WindowsUpdateArchive {
    const val MAX_EXPANDED_BYTES = 1500L * 1024 * 1024
    fun extract(archive: Path, staging: Path, checkActive: () -> Unit = {}) : Path {
        require(Files.isDirectory(staging) && Files.list(staging).use { !it.findAny().isPresent })
        var expanded = 0L
        val names = mutableSetOf<String>()
        ZipFile(archive.toFile()).use { zip ->
            val entries = zip.entries().toList()
            require(entries.size in 1..20000) { "Invalid update archive" }
            entries.forEach { entry ->
                checkActive()
                val name = entry.name.trimEnd('/')
                val parts = name.split('/')
                require(parts.firstOrNull() == "Qirato" && parts.all { part ->
                    part.isNotEmpty() && part != "." && part != ".." && !part.endsWith('.') && !part.endsWith(' ') &&
                        part.none { it < ' ' || it in "\\:<>\"|?*" } &&
                        !part.substringBefore('.').matches(Regex("(?i)(CON|PRN|AUX|NUL|COM[1-9]|LPT[1-9])"))
                }) { "Unsafe archive path" }
                require(names.add(name.lowercase())) { "Duplicate archive path" }
                val target = staging.resolve(name).normalize()
                require(target.startsWith(staging) && target != staging) { "Archive escaped staging" }
                if (entry.isDirectory) Files.createDirectories(target) else {
                    Files.createDirectories(target.parent)
                    zip.getInputStream(entry).use { input -> Files.newOutputStream(target, StandardOpenOption.CREATE_NEW).use { output ->
                        val buffer = ByteArray(65536)
                        while (true) {
                            checkActive()
                            val count = input.read(buffer); if (count < 0) break
                            expanded += count
                            require(expanded <= MAX_EXPANDED_BYTES) { "Expanded archive too large" }
                            output.write(buffer, 0, count)
                        }
                    } }
                }
            }
        }
        val root = staging.resolve("Qirato")
        validateBundle(root)
        return root
    }

    fun validateBundle(root: Path) {
        require(Files.isRegularFile(root.resolve("Qirato.exe")) && Files.isRegularFile(root.resolve("app/Qirato.cfg")) &&
            Files.isDirectory(root.resolve("runtime"))) { "Incomplete Qirato bundle" }
        require(Files.list(root).use { stream -> stream.allMatch { it.fileName.toString() in setOf("Qirato.exe", "app", "runtime", ".qirato-msi") } }) { "Unexpected bundle contents" }
    }
}

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
    private val packageKind get() = if (installRoot?.let { Files.isRegularFile(it.resolve(".qirato-msi")) } == true) WindowsPackage.MSI else WindowsPackage.ZIP
    override suspend fun check() = network.check(version, packageKind)

    override suspend fun prepare(release: WindowsRelease, progress: (Long, Long) -> Unit, verifying: () -> Unit): PreparedWindowsUpdate = withContext(Dispatchers.IO) {
        val root = checkNotNull(installRoot) { "Run the portable packaged application" }.toAbsolutePath().normalize()
        WindowsUpdateArchive.validateBundle(root)
        require(root.fileName.toString().equals("Qirato", ignoreCase = true) && root.parent != null && root.parent.parent != null)
        require(root.toRealPath() == root && !dataDirectory.toAbsolutePath().normalize().startsWith(root)) { "Unsupported installation path" }
        val staging = Files.createDirectory(root.parent.resolve(".qirato-update-${UUID.randomUUID()}"))
        require(release.kind == packageKind) { "Update package does not match installation" }
        val archive = staging.resolve("package.${release.kind.extension}")
        network.download(release, archive, progress)
        coroutineContext.ensureActive(); verifying()
        if (release.kind == WindowsPackage.MSI) {
            val update = PreparedWindowsUpdate(release, root, staging, archive)
            val (script, plan) = writeHelper(update)
            runVerification(ProcessBuilder(powershell().toString(), "-NoProfile", "-NonInteractive", "-WindowStyle", "Hidden", "-ExecutionPolicy", "Bypass",
                "-File", script.toString(), "-Plan", plan.toString(), "-ValidateOnly"), staging.resolve("msi-verify.log"))
            return@withContext update
        }
        val extraction = Files.createDirectory(staging.resolve("extracted"))
        val context = coroutineContext
        val bundle = WindowsUpdateArchive.extract(archive, extraction) { context.ensureActive() }
        val process = ProcessBuilder(bundle.resolve("Qirato.exe").toString(), "--verify-runtime")
            .directory(staging.toFile()).redirectErrorStream(true).redirectOutput(staging.resolve("verify.log").toFile()).start()
        try {
            val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(60)
            while (process.isAlive && System.nanoTime() < deadline) { delay(100); coroutineContext.ensureActive() }
            require(!process.isAlive && process.exitValue() == 0) { "New runtime could not start" }
            require(Files.size(staging.resolve("verify.log")) < 65536 && Files.readAllLines(staging.resolve("verify.log")).contains("Qirato runtime version=${release.version}")) { "New runtime version mismatch" }
        } finally { if (process.isAlive) process.destroyForcibly() }
        PreparedWindowsUpdate(release, root, staging, bundle)
    }

    override suspend fun launch(update: PreparedWindowsUpdate): Unit = withContext(Dispatchers.IO) {
        val root = update.installRoot.toAbsolutePath().normalize()
        require(root == installRoot?.toAbsolutePath()?.normalize())
        require(update.stagingRoot.parent == root.parent && update.release.kind == packageKind)
        if (update.release.kind == WindowsPackage.ZIP) {
            require(update.bundle == update.stagingRoot.resolve("extracted/Qirato"))
            WindowsUpdateArchive.validateBundle(update.bundle)
        } else require(update.bundle == update.stagingRoot.resolve("package.msi"))
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
        val resource = if (update.release.kind == WindowsPackage.MSI) "/update/install-msi.ps1" else "/update/install.ps1"
        checkNotNull(javaClass.getResourceAsStream(resource)).use { Files.copy(it, script, StandardCopyOption.REPLACE_EXISTING) }
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
