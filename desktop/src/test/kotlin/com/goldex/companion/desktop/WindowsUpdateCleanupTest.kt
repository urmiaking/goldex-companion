package com.goldex.companion.desktop

import org.json.JSONObject
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.nio.file.Files
import java.nio.file.Path
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlin.test.*

/** Exercise the shipped cleanup, including its allowlist, without installing or touching real Programs. */
class WindowsUpdateCleanupTest {
    @get:Rule val temporary = TemporaryFolder()

    @Test fun removesOnlyVerifiedOlderCachesAfterSuccessAndKeepsCurrentRepairSource() {
        val directory = temporary.newFolder().toPath()
        val programs = Files.createDirectories(directory.resolve("Programs"))
        val root = Files.createDirectories(programs.resolve("Qirato"))
        Files.createFile(root.resolve(".qirato-msi"))
        val data = Files.createDirectories(directory.resolve("Qirato/Desktop"))
        val sentinel = Files.writeString(data.resolve("sentinel"), "retained data")
        fun stage(version: String, success: Boolean? = true): Path {
            val stage = Files.createDirectory(programs.resolve(".qirato-update-${UUID.randomUUID()}"))
            val bundle = Files.writeString(stage.resolve("Qirato-Windows-x64-$version.msi"), "fixture")
            Files.writeString(stage.resolve("plan.json"), JSONObject().put("installRoot", root.toString())
                .put("stagingRoot", stage.toString()).put("bundle", bundle.toString())
                .put("dataDirectory", data.toString()).put("version", version).toString())
            if (success != null) Files.writeString(stage.resolve("result.json"), JSONObject().put("success", success).put("version", version).toString())
            return stage
        }
        val current = stage("1.2.3", false)
        val old = stage("1.2.2")
        val failed = stage("1.2.1", false)
        val incomplete = stage("1.2.0", null)
        val future = stage("1.2.4")
        val same = stage("1.2.3", false)
        val unknown = stage("1.1.0").also { Files.writeString(it.resolve("user-file.txt"), "keep") }
        val nested = stage("1.0.0").also { Files.createDirectory(it.resolve("extra")) }
        val mismatched = stage("0.9.0").also {
            Files.writeString(it.resolve("result.json"), JSONObject().put("success", true).put("version", "0.8.0").toString())
        }
        val script = directory.resolve("helper.ps1")
        checkNotNull(javaClass.getResourceAsStream("/update/install-msi.ps1")).use { Files.copy(it, script) }
        val driver = Files.writeString(directory.resolve("driver.ps1"), """
            param([string]${'$'}Directory, [string]${'$'}Current)
            ${'$'}ErrorActionPreference = 'Stop'
            Set-StrictMode -Version Latest
            ${'$'}env:LOCALAPPDATA = ${'$'}Directory
            ${'$'}tokens = ${'$'}null; ${'$'}errors = ${'$'}null
            ${'$'}ast = [Management.Automation.Language.Parser]::ParseFile((Join-Path ${'$'}Directory 'helper.ps1'), [ref]${'$'}tokens, [ref]${'$'}errors)
            if (${ '$'}errors.Count -gt 0) { throw 'Helper syntax failed' }
            foreach (${ '$'}name in @('FullPath','PlainPath','RemovePreviousUpdateStages')) {
                ${'$'}fn = ${'$'}ast.Find({ param(${'$'}n) ${'$'}n -is [Management.Automation.Language.FunctionDefinitionAst] -and ${'$'}n.Name -eq ${'$'}name }, ${'$'}true)
                Invoke-Expression ${'$'}fn.Extent.Text
            }
            RemovePreviousUpdateStages ${'$'}Current (Join-Path ${'$'}Directory 'Programs/Qirato') '1.2.3' (Join-Path ${'$'}Directory 'Qirato/Desktop')
        """.trimIndent())
        fun cleanup(shipped: Boolean = false) {
            val shell = Path.of(System.getenv("SystemRoot") ?: "C:\\Windows", "System32/WindowsPowerShell/v1.0/powershell.exe")
            val log = directory.resolve("cleanup.log")
            val arguments = if (shipped) listOf(script.toString(), "-CleanupOnly", "-InstalledVersion", "1.2.3", "-DataDirectory", data.toString())
                else listOf(driver.toString(), "-Directory", directory.toString(), "-Current", current.toString())
            val process = ProcessBuilder(listOf(shell.toString(), "-NoProfile", "-NonInteractive", "-File") + arguments)
                .apply { environment()["LOCALAPPDATA"] = directory.toString() }
                .redirectErrorStream(true).redirectOutput(log.toFile()).start()
            try {
                assertTrue(process.waitFor(20, TimeUnit.SECONDS))
                assertEquals(0, process.exitValue(), Files.readString(log))
            } finally { if (process.isAlive) process.destroyForcibly().waitFor() }
        }
        cleanup()
        cleanup(shipped = true)
        assertTrue(Files.exists(old), "Failure must not prune previous sources")
        Files.writeString(current.resolve("result.json"), JSONObject().put("success", true).put("version", "1.2.3").toString())
        Files.writeString(same.resolve("result.json"), JSONObject().put("success", true).put("version", "1.2.3").toString())
        cleanup()
        assertFalse(Files.exists(old))
        for (retained in listOf(current, failed, incomplete, future, same, unknown, nested, mismatched)) assertTrue(Files.exists(retained), retained.toString())
        assertTrue(Files.exists(current.resolve("Qirato-Windows-x64-1.2.3.msi")))
        assertEquals("retained data", Files.readString(sentinel))
        cleanup() // Idempotent.
        val legacyHelperCache = stage("1.2.2")
        cleanup(shipped = true)
        assertFalse(Files.exists(legacyHelperCache), "New app startup must recover caches left by legacy helpers")
    }
}
