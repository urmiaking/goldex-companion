package com.goldex.companion.desktop

import org.json.JSONObject
import org.junit.*
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.nio.file.*
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlin.test.*

/** Executes the shipped PowerShell helper against synthetic disposable app/data directories. */
class WindowsUpdateHelperTest {
    @get:Rule val temporary = TemporaryFolder()
    private val shell = Path.of(System.getenv("SystemRoot") ?: "C:\\Windows", "System32/WindowsPowerShell/v1.0/powershell.exe").toString()
    private lateinit var binary: Path

    @Before fun compileSyntheticApplication() {
        val root = temporary.root.toPath()
        val source = root.resolve("app.cs")
        Files.writeString(source, """
            using System;
            using System.IO;
            using System.Threading;
            public class SyntheticQirato {
                public static void Main() {
                    var root = AppDomain.CurrentDomain.BaseDirectory;
                    var version = File.ReadAllText(Path.Combine(root, "app", "Qirato.cfg"));
                    File.AppendAllText(Environment.GetEnvironmentVariable("QIRATO_UPDATE_TEST_MARKER"), version + "\n");
                    if (version == "broken") return;
                    Thread.Sleep(30000);
                }
            }
        """.trimIndent())
        val build = root.resolve("build.ps1")
        Files.writeString(build, "param([string]" + "$" + "Source,[string]" + "$" + "Output)\nAdd-Type -Path " + "$" + "Source -OutputAssembly " + "$" + "Output -OutputType WindowsApplication -ErrorAction Stop")
        binary = root.resolve("synthetic.exe")
        val process = ProcessBuilder(shell, "-NoProfile", "-NonInteractive", "-File", build.toString(), "-Source", source.toString(), "-Output", binary.toString())
            .redirectErrorStream(true).redirectOutput(root.resolve("compile.log").toFile()).start()
        assertTrue(process.waitFor(30, TimeUnit.SECONDS)); assertEquals(0, process.exitValue(), Files.readString(root.resolve("compile.log")))
    }

    private fun bundle(root: Path, version: String) {
        Files.createDirectories(root.resolve("app")); Files.createDirectories(root.resolve("runtime"))
        Files.copy(binary, root.resolve("Qirato.exe")); Files.writeString(root.resolve("app/Qirato.cfg"), version)
    }

    private fun await(timeout: Long = 10000, condition: () -> Boolean) {
        val deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(timeout)
        while (!condition() && System.nanoTime() < deadline) Thread.sleep(25)
        assertTrue(condition(), "Timed out waiting for synthetic updater")
    }

    private fun scenario(newVersion: String, invalidBundle: Boolean = false, afterReady: ((Path) -> Unit)? = null) {
        val parent = temporary.newFolder("نصب قیراط با فاصله").toPath()
        val root = parent.resolve("Qirato"); bundle(root, "previous")
        val data = temporary.newFolder("data").toPath()
        Files.writeString(data.resolve("workspace.json"), "retained synthetic business data")
        Files.createFile(data.resolve("workspace.lock"))
        val marker = temporary.root.toPath().resolve("starts.txt")
        val app = ProcessBuilder(root.resolve("Qirato.exe").toString()).apply { environment()["QIRATO_UPDATE_TEST_MARKER"] = marker.toString() }.start()
        val stage = Files.createDirectory(parent.resolve(".qirato-update-${UUID.randomUUID()}"))
        val newBundle = stage.resolve("extracted/Qirato"); bundle(newBundle, newVersion)
        val script = stage.resolve("install.ps1")
        checkNotNull(javaClass.getResourceAsStream("/update/install.ps1")).use { Files.copy(it, script) }
        val plan = stage.resolve("plan.json")
        Files.writeString(plan, JSONObject().put("installRoot", root.toString()).put("stagingRoot", stage.toString())
            .put("bundle", if (invalidBundle) parent.resolve("outside").toString() else newBundle.toString())
            .put("dataDirectory", data.toString()).put("processId", app.pid()).put("version", "0.56.38").toString())
        var helper: Process? = null
        try {
            await { Files.exists(marker) }
            helper = ProcessBuilder(shell, "-NoProfile", "-NonInteractive", "-WindowStyle", "Hidden", "-ExecutionPolicy", "Bypass", "-File", script.toString(), "-Plan", plan.toString())
                .directory(stage.toFile()).apply { environment()["QIRATO_UPDATE_TEST_MARKER"] = marker.toString() }
                .redirectErrorStream(true).redirectOutput(stage.resolve("helper-test.log").toFile()).start()
            if (invalidBundle) {
                assertTrue(helper.waitFor(15, TimeUnit.SECONDS)); assertNotEquals(0, helper.exitValue())
                assertFalse(Files.exists(stage.resolve("ready"))); assertTrue(app.isAlive)
                assertEquals("previous", Files.readString(root.resolve("app/Qirato.cfg")))
            } else {
                await { Files.exists(stage.resolve("ready")) }
                assertTrue(app.isAlive); assertEquals("previous", Files.readString(root.resolve("app/Qirato.cfg")))
                assertTrue(Files.isRegularFile(newBundle.resolve("Qirato.exe")))
                afterReady?.invoke(data)
                // Exit the test app only after acknowledgment, as the real UI does.
                app.destroyForcibly().waitFor()
                assertTrue(helper.waitFor(20, TimeUnit.SECONDS), Files.readString(stage.resolve("helper-test.log")))
                val result = JSONObject(Files.readString(stage.resolve("result.json")).removePrefix("\uFEFF"))
                if (newVersion == "broken") {
                    assertNotEquals(0, helper.exitValue()); assertFalse(result.getBoolean("success"))
                    assertEquals("previous", Files.readString(root.resolve("app/Qirato.cfg")))
                    await { Files.readAllLines(marker).count { it == "previous" } == 2 }
                } else {
                    assertEquals(0, helper.exitValue(), Files.readString(stage.resolve("helper-test.log")))
                    assertTrue(result.getBoolean("success")); assertEquals("updated", Files.readString(root.resolve("app/Qirato.cfg")))
                    val backup = Path.of(result.getString("backup")); assertEquals("previous", Files.readString(backup.resolve("app/Qirato.cfg")))
                    assertTrue(Files.readAllLines(marker).contains("updated"))
                }
            }
            assertEquals("retained synthetic business data", Files.readString(data.resolve("workspace.json")))
        } finally {
            helper?.takeIf { it.isAlive }?.destroyForcibly()?.waitFor()
            if (app.isAlive) app.destroyForcibly().waitFor()
            // Terminate only this test's synthetic child apps before JUnit removes its temp tree.
            ProcessHandle.allProcesses().use { processes -> processes.filter { process ->
                process.info().command().orElse("").startsWith(parent.toString(), ignoreCase = true)
            }.forEach { process -> process.destroyForcibly(); process.onExit().get(5, TimeUnit.SECONDS) } }
        }
    }

    @Test fun waitsForExitSwapsBundleAndLaunchesNewApplicationPreservingDataAndBackup() = scenario("updated")
    @Test fun restoresAndLaunchesPreviousApplicationWhenNewApplicationFails() = scenario("broken")
    @Test fun refusesPlanWhoseBundleEscapesStagingWithoutClosingRunningApp() = scenario("updated", invalidBundle = true)
}
