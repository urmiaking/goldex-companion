package com.goldex.companion.desktop

import com.goldex.companion.desktop.update.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.first
import org.json.*
import org.junit.*
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.net.URI
import java.nio.file.*
import java.security.MessageDigest
import kotlin.test.*

class WindowsUpdateTest {
    @get:Rule val temporary = TemporaryFolder()
    private val installed = WindowsVersion(0, 56, 37)
    private fun metadata(version: String, windowsTag: Boolean = false, windowsAsset: Boolean = true): JSONObject {
        val tag = (if (windowsTag) "windows-v" else "v") + version
        val asset = JSONObject().put("name", "Qirato-Windows-x64-$version.msi").put("state", "uploaded").put("size", 100)
            .put("digest", "sha256:" + "a".repeat(64))
            .put("browser_download_url", "https://github.com/urmiaking/goldex-companion/releases/download/$tag/Qirato-Windows-x64-$version.msi")
        return JSONObject().put("tag_name", tag).put("draft", false).put("prerelease", false).put("body", "تغییرات نسخه")
            .put("assets", if (windowsAsset) JSONArray().put(asset) else JSONArray())
    }

    @Test fun discoversWindowsChannelAcrossPagesDespiteNewerAndroidOnlyRelease() {
        val first = JSONArray().put(metadata("0.56.40", windowsAsset = false)).put(metadata("0.56.38"))
        val second = JSONArray().put(metadata("0.56.39", windowsTag = true))
        assertEquals("windows-v0.56.39", WindowsReleasePolicy.newest(listOf(first, second), installed)?.tag)
        assertNull(WindowsReleasePolicy.newest(listOf(first, second), WindowsVersion(0, 56, 39)))
        assertTrue(WindowsVersion(0, 56, 100) > WindowsVersion(0, 56, 99))
    }

    @Test fun ignoresDraftPrereleaseAndUnrelatedAssets() {
        val page = JSONArray().put(metadata("0.56.38").put("draft", true)).put(metadata("0.56.39").put("prerelease", true))
            .put(metadata("0.56.40").put("tag_name", "experimental-v0.56.40")).put(metadata("0.56.37"))
        assertNull(WindowsReleasePolicy.newest(listOf(page), installed))
    }

    @Test fun refusesMissingDigestAndForeignDownloadRatherThanInstallingAnOlderCandidate() {
        for (mutation in listOf<(JSONObject) -> Unit>({ it.put("digest", "") }, { it.put("browser_download_url", "https://example.com/Qirato.msi") })) {
            val release = metadata("0.56.39", windowsTag = true)
            mutation(release.getJSONArray("assets").getJSONObject(0))
            assertFailsWith<IllegalArgumentException> { WindowsReleasePolicy.newest(listOf(JSONArray().put(metadata("0.56.38")).put(release)), installed) }
        }
    }

    @Test fun discoversOnlyMsiAndKeepsCanonicalInstallerFilename() {
        val installer = metadata("0.56.40")
        val portable = metadata("0.56.41")
        val asset = portable.getJSONArray("assets").getJSONObject(0)
        asset.put("name", "Qirato-Windows-x64-0.56.41.zip")
        asset.put("browser_download_url", asset.getString("browser_download_url").replace(".msi", ".zip"))
        val release = assertNotNull(WindowsReleasePolicy.newest(listOf(JSONArray().put(installer).put(portable)), installed))
        assertEquals(WindowsVersion(0,56,40), release.version)
        assertEquals("Qirato-Windows-x64-0.56.40.msi", release.installerName)
    }

    private fun release(bytes: ByteArray) = WindowsRelease(WindowsVersion(0,56,38), "windows-v0.56.38", "", URI("https://github.com"), bytes.size.toLong(),
        MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it.toInt() and 255) })

    @Test fun verifiesDownloadedBytesAndReportsCompletion() {
        val bytes = ByteArray(300000) { (it % 127).toByte() }
        val output = temporary.root.toPath().resolve("package.msi")
        var received = 0L
        WindowsUpdateDownload.copyVerified(bytes.inputStream(), output, release(bytes), { count, _ -> received = count })
        assertContentEquals(bytes, Files.readAllBytes(output)); assertEquals(bytes.size.toLong(), received)
    }

    @Test fun removesOnlyOwnedPartialFilesOnTruncationCorruptionCancellation() {
        val bytes = "valid package".toByteArray()
        for (input in listOf(bytes.copyOf(2), "wrong package".toByteArray(), bytes + 1.toByte())) {
            val path = temporary.root.toPath().resolve("partial.msi")
            assertFails { WindowsUpdateDownload.copyVerified(input.inputStream(), path, release(bytes)) }
            assertFalse(Files.exists(path))
        }
        val path = temporary.root.toPath().resolve("cancel.msi")
        assertFailsWith<CancellationException> { WindowsUpdateDownload.copyVerified(bytes.inputStream(), path, release(bytes), checkActive = { throw CancellationException() }) }
        assertFalse(Files.exists(path))
        Files.writeString(path, "retained")
        assertFails { WindowsUpdateDownload.copyVerified(bytes.inputStream(), path, release(bytes)) }
        assertEquals("retained", Files.readString(path))
    }

    private class Gateway(val release: WindowsRelease?) : WindowsUpdateGateway {
        var launches = 0; var downloads = 0; var fails = false; var suspendDownload = false; var failDownload = false
        override suspend fun check() = release
        override suspend fun prepare(release: WindowsRelease, progress: (Long,Long)->Unit, verifying:()->Unit): PreparedWindowsUpdate {
            downloads++; progress(release.size, release.size)
            if (failDownload) error("download fixture failure")
            if (suspendDownload) awaitCancellation()
            verifying(); return PreparedWindowsUpdate(release, Path.of("Qirato"), Path.of("stage"), Path.of("bundle"))
        }
        override suspend fun launch(update: PreparedWindowsUpdate) { if (fails) error("test helper failure"); launches++ }
    }

    @Test fun startupAndFiveMinuteTicksCheckWithoutManualActionAndDoNotOverlap() = runBlocking {
        val calls = Channel<Unit>(Channel.UNLIMITED)
        val finish = Channel<Unit>(Channel.UNLIMITED)
        val ticks = Channel<Unit>(Channel.UNLIMITED)
        val intervals = Channel<Long>(Channel.UNLIMITED)
        val gateway = object : WindowsUpdateGateway {
            override suspend fun check(): WindowsRelease? { calls.send(Unit); finish.receive(); return null }
            override suspend fun prepare(release: WindowsRelease, progress: (Long,Long)->Unit, verifying:()->Unit): PreparedWindowsUpdate = error("No release")
            override suspend fun launch(update: PreparedWindowsUpdate) = error("No release")
        }
        val updater = WindowsUpdater(gateway, waitForNextCheck = { intervals.send(it); ticks.receive() })
        try {
            updater.start(); updater.start()
            withTimeout(3000) { calls.receive() }
            assertNull(updater.check()); assertTrue(calls.tryReceive().isFailure)
            finish.send(Unit)
            assertEquals(300000L, withTimeout(3000) { intervals.receive() })
            ticks.send(Unit)
            withTimeout(3000) { calls.receive() }
            finish.send(Unit)
            assertEquals(300000L, withTimeout(3000) { intervals.receive() })
            assertEquals(WindowsUpdatePhase.CURRENT, updater.state.value.phase)
        } finally { updater.close() }
    }

    @Test fun slowBackgroundRecheckKeepsAvailableActionAndNeverShowsCheckingPhase() = runBlocking {
        val waiting = Channel<Unit>(Channel.UNLIMITED)
        val finish = Channel<Unit>(Channel.UNLIMITED)
        val item = release("bytes".toByteArray())
        var checks = 0
        val updater = WindowsUpdater(object : WindowsUpdateGateway {
            override suspend fun check(): WindowsRelease? {
                checks++
                if (checks > 1) { waiting.send(Unit); finish.receive(); error("offline") }
                return item
            }
            override suspend fun prepare(release: WindowsRelease, progress: (Long, Long) -> Unit, verifying: () -> Unit): PreparedWindowsUpdate = error("unused")
            override suspend fun launch(update: PreparedWindowsUpdate) = error("unused")
        })
        try {
            updater.check(background = true)!!.join()
            val repeat = updater.check(background = true)!!
            withTimeout(3000) { waiting.receive() }
            assertEquals(WindowsUpdatePhase.AVAILABLE, updater.state.value.phase)
            assertEquals(item, updater.state.value.release)
            assertFalse(updater.state.value.busy)
            assertNull(updater.check())
            finish.send(Unit); repeat.join()
            assertEquals(WindowsUpdatePhase.AVAILABLE, updater.state.value.phase)
            assertFalse(updater.state.value.dialog)
        } finally { updater.close() }
    }

    @Test fun backgroundTicksKeepPostponedReadyUpdateQuietButManualCheckReopensIt() = runBlocking {
        val ticks = Channel<Unit>(Channel.UNLIMITED)
        val intervals = Channel<Long>(Channel.UNLIMITED)
        val gateway = Gateway(release("bytes".toByteArray()))
        val updater = WindowsUpdater(gateway, waitForNextCheck = { intervals.send(it); ticks.receive() })
        try {
            updater.start()
            withTimeout(3000) { intervals.receive() }
            assertEquals(WindowsUpdatePhase.AVAILABLE, updater.state.value.phase); assertFalse(updater.state.value.dialog)
            ticks.send(Unit); withTimeout(3000) { intervals.receive() }
            assertEquals(WindowsUpdatePhase.AVAILABLE, updater.state.value.phase); assertEquals(0, gateway.downloads)
            assertEquals(0, gateway.downloads); updater.download()?.join()
            assertEquals(WindowsUpdatePhase.READY, updater.state.value.phase); assertFalse(updater.state.value.dialog)
            updater.showDialog(); assertTrue(updater.state.value.dialog)
            updater.postpone(); ticks.send(Unit)
            withTimeout(3000) { intervals.receive() }
            assertFalse(updater.state.value.dialog); assertEquals(1, gateway.downloads)
            updater.check(); assertTrue(updater.state.value.dialog)
        } finally { updater.close() }
    }

    @Test fun discoversQuietlyAndDownloadsOnlyOnIntentThenReusesReadyPackage() = runBlocking {
        val gateway = Gateway(release("bytes".toByteArray())); val updater = WindowsUpdater(gateway)
        try {
            updater.check()?.join(); assertEquals(WindowsUpdatePhase.AVAILABLE, updater.state.value.phase)
            assertFalse(updater.state.value.dialog); assertEquals(0, gateway.downloads)
            updater.download()?.join(); assertEquals(WindowsUpdatePhase.READY, updater.state.value.phase)
            assertFalse(updater.state.value.dialog); updater.showDialog()
            assertTrue(updater.state.value.dialog); assertEquals(0, gateway.launches)
            updater.postpone(); assertFalse(updater.state.value.dialog)
            updater.check()?.join(); assertTrue(updater.state.value.dialog); assertEquals(1, gateway.downloads)
            var closed = false; updater.restart { closed = true }?.join()
            assertEquals(1, gateway.launches); assertTrue(closed)
        } finally { updater.close() }
    }

    @Test fun helperFailureKeepsApplicationOpenAndAllowsRetry() = runBlocking {
        val gateway = Gateway(release("bytes".toByteArray())); val updater = WindowsUpdater(gateway)
        try {
            updater.check()?.join(); updater.download()?.join(); gateway.fails = true
            var closed = false; updater.restart { closed = true }?.join()
            assertFalse(closed); assertEquals(WindowsUpdatePhase.READY, updater.state.value.phase); assertNotNull(updater.state.value.error)
            gateway.fails = false; updater.restart { closed = true }?.join(); assertTrue(closed)
        } finally { updater.close() }
    }

    @Test fun failedDownloadCanRetryAndNeverOffersRestartForPartialBytes() = runBlocking {
        val gateway = Gateway(release("bytes".toByteArray())).apply { failDownload = true }
        val updater = WindowsUpdater(gateway)
        try {
            updater.check()?.join(); updater.download()?.join()
            assertEquals(WindowsUpdatePhase.FAILED, updater.state.value.phase)
            assertFalse(updater.state.value.dialog); assertNotNull(updater.state.value.error)
            assertNull(updater.restart { fail("Partial update cannot restart") })
            gateway.failDownload = false
            updater.activate()
            withTimeout(3000) { updater.state.first { it.phase == WindowsUpdatePhase.READY } }
            assertEquals(2, gateway.downloads); assertFalse(updater.state.value.dialog)
        } finally { updater.close() }
    }

    @Test fun cancellationNeverOffersPartialUpdate() = runBlocking {
        val gateway = Gateway(release("bytes".toByteArray())).apply { suspendDownload = true }; val updater = WindowsUpdater(gateway)
        try {
            updater.check()?.join(); updater.download()
            withTimeout(3000) { while (updater.state.value.phase != WindowsUpdatePhase.DOWNLOADING) delay(10) }
            updater.cancelDownload()
            withTimeout(3000) { while (updater.state.value.phase != WindowsUpdatePhase.AVAILABLE) delay(10) }
            assertFalse(updater.state.value.dialog); assertNull(updater.restart { fail("Must not close") }); assertEquals(0, gateway.launches)
        } finally { updater.close() }
    }
}
