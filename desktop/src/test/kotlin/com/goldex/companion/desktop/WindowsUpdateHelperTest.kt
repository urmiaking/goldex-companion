package com.goldex.companion.desktop

import org.json.JSONObject
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.nio.file.*
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlin.test.*

/** Validates the shipped MSI helper in isolated paths without invoking Windows Installer. */
class WindowsUpdateHelperTest {
    @get:Rule val temporary = TemporaryFolder()

    private fun rejects(expectedError: String, mutate: (JSONObject, Path) -> Unit = { _, _ -> }) {
        val local = temporary.newFolder().toPath()
        val root = Files.createDirectories(local.resolve("Programs/Qirato"))
        Files.createFile(root.resolve(".qirato-msi"))
        val stage = Files.createDirectory(root.parent.resolve(".qirato-update-${UUID.randomUUID()}"))
        val data = Files.createDirectories(local.resolve("Qirato/Desktop"))
        val sentinel = Files.writeString(data.resolve("sentinel.txt"), "future records retained")
        val installer = Files.writeString(stage.resolve("Qirato-Windows-x64-0.56.40.msi"), "corrupt installer")
        val script = stage.resolve("install.ps1")
        checkNotNull(javaClass.getResourceAsStream("/update/install-msi.ps1")).use { Files.copy(it, script) }
        val plan = JSONObject().put("installRoot", root.toString()).put("stagingRoot", stage.toString())
            .put("bundle", installer.toString()).put("dataDirectory", data.toString())
            .put("processId", ProcessHandle.current().pid()).put("version", "0.56.40")
            .put("size", Files.size(installer)).put("sha256", "0".repeat(64))
        mutate(plan, root)
        val planFile = Files.writeString(stage.resolve("plan.json"), plan.toString())
        val log = stage.resolve("validation.log")
        val shell = Path.of(System.getenv("SystemRoot") ?: "C:\\Windows", "System32/WindowsPowerShell/v1.0/powershell.exe")
        val process = ProcessBuilder(shell.toString(), "-NoProfile", "-NonInteractive", "-File", script.toString(), "-Plan", planFile.toString(), "-ValidateOnly")
            .apply { environment()["LOCALAPPDATA"] = local.toString() }
            .redirectErrorStream(true).redirectOutput(log.toFile()).start()
        try {
            assertTrue(process.waitFor(20, TimeUnit.SECONDS))
            assertNotEquals(0, process.exitValue())
            assertTrue(Files.readString(log).contains(expectedError), Files.readString(log))
            assertFalse(Files.exists(stage.resolve("ready")))
            assertEquals("future records retained", Files.readString(sentinel))
            assertTrue(Files.exists(root.resolve(".qirato-msi")))
        } finally { if (process.isAlive) process.destroyForcibly().waitFor() }
    }

    @Test fun rejectsDataInsideInstalledApplication() = rejects("Invalid installed update plan") { plan, root ->
        plan.put("dataDirectory", root.resolve("data").toString())
    }
    @Test fun rejectsRenamedInstallerSource() = rejects("Invalid installed update plan") { plan, _ ->
        plan.put("bundle", Path.of(plan.getString("stagingRoot")).resolve("package.msi").toString())
    }
    @Test fun rejectsCorruptInstallerBeforeAcknowledgingOrInstalling() = rejects("Installer checksum mismatch")
}
