package com.goldex.companion.desktop

import com.goldex.companion.desktop.update.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import org.json.*
import org.junit.*
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.net.URI
import java.nio.file.*
import java.security.MessageDigest
import java.util.zip.*
import kotlin.test.*

class WindowsUpdateTest {
    @get:Rule val temporary = TemporaryFolder()
    private val installed = WindowsVersion(0, 56, 37)
    private fun metadata(version: String, windowsTag: Boolean = false, windowsAsset: Boolean = true): JSONObject {
        val tag = (if (windowsTag) "windows-v" else "v") + version
        val asset = JSONObject().put("name", "Qirato-Windows-x64-$version.zip").put("state", "uploaded").put("size", 100)
            .put("digest", "sha256:" + "a".repeat(64))
            .put("browser_download_url", "https://github.com/urmiaking/goldex-companion/releases/download/$tag/Qirato-Windows-x64-$version.zip")
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
        for (mutation in listOf<(JSONObject) -> Unit>({ it.put("digest", "") }, { it.put("browser_download_url", "https://example.com/Qirato.zip") })) {
            val release = metadata("0.56.39", windowsTag = true)
            mutation(release.getJSONArray("assets").getJSONObject(0))
            assertFailsWith<IllegalArgumentException> { WindowsReleasePolicy.newest(listOf(JSONArray().put(metadata("0.56.38")).put(release)), installed) }
        }
    }

    @Test fun installedChannelRequiresMsiWhileLegacyPortableStillFindsZip() {
        val candidate = metadata("0.56.40")
        val assets = candidate.getJSONArray("assets")
        val msi = JSONObject(assets.getJSONObject(0).toString())
        msi.put("name", "Qirato-Windows-x64-0.56.40.msi")
        msi.put("browser_download_url", msi.getString("browser_download_url").replace(".zip", ".msi"))
        assets.put(msi)
        val pages = listOf(JSONArray().put(candidate).put(metadata("0.56.41")))
        assertEquals(WindowsVersion(0,56,41), WindowsReleasePolicy.newest(pages, installed)?.version)
        val release = assertNotNull(WindowsReleasePolicy.newest(pages, installed, WindowsPackage.MSI))
        assertEquals(WindowsVersion(0,56,40), release.version)
        assertEquals(WindowsPackage.MSI, release.kind)
        msi.put("digest", "")
        assertFails { WindowsReleasePolicy.newest(pages, installed, WindowsPackage.MSI) }
    }

    private fun release(bytes: ByteArray) = WindowsRelease(WindowsVersion(0,56,38), "windows-v0.56.38", "", URI("https://github.com"), bytes.size.toLong(),
        MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it.toInt() and 255) })

    @Test fun verifiesDownloadedBytesAndReportsCompletion() {
        val bytes = ByteArray(300000) { (it % 127).toByte() }
        val output = temporary.root.toPath().resolve("package.zip")
        var received = 0L
        WindowsUpdateDownload.copyVerified(bytes.inputStream(), output, release(bytes), { count, _ -> received = count })
        assertContentEquals(bytes, Files.readAllBytes(output)); assertEquals(bytes.size.toLong(), received)
    }

    @Test fun removesOnlyOwnedPartialFilesOnTruncationCorruptionCancellation() {
        val bytes = "valid package".toByteArray()
        for (input in listOf(bytes.copyOf(2), "wrong package".toByteArray(), bytes + 1.toByte())) {
            val path = temporary.root.toPath().resolve("partial.zip")
            assertFails { WindowsUpdateDownload.copyVerified(input.inputStream(), path, release(bytes)) }
            assertFalse(Files.exists(path))
        }
        val path = temporary.root.toPath().resolve("cancel.zip")
        assertFailsWith<CancellationException> { WindowsUpdateDownload.copyVerified(bytes.inputStream(), path, release(bytes), checkActive = { throw CancellationException() }) }
        assertFalse(Files.exists(path))
        Files.writeString(path, "retained")
        assertFails { WindowsUpdateDownload.copyVerified(bytes.inputStream(), path, release(bytes)) }
        assertEquals("retained", Files.readString(path))
    }

    private fun archive(names: List<String>): Path {
        val zip = temporary.newFile().toPath()
        ZipOutputStream(Files.newOutputStream(zip)).use { output -> names.forEach { name ->
            output.putNextEntry(ZipEntry(name)); if (!name.endsWith('/')) output.write("test".toByteArray()); output.closeEntry()
        } }
        return zip
    }

    @Test fun extractsACompleteBundleWithoutChangingExternalData() {
        val data = temporary.newFile("workspace.json").toPath(); Files.writeString(data, "synthetic records")
        val zip = archive(listOf("Qirato/Qirato.exe", "Qirato/app/Qirato.cfg", "Qirato/runtime/", "Qirato/runtime/bin/test.dll"))
        val bundle = WindowsUpdateArchive.extract(zip, temporary.newFolder().toPath())
        assertTrue(Files.isRegularFile(bundle.resolve("Qirato.exe")))
        assertEquals("synthetic records", Files.readString(data))
    }

    @Test fun rejectsTraversalWindowsAliasesDuplicateAndIncompleteBundles() {
        for (name in listOf("../escape", "Qirato/../escape", "Qirato/app/CON.txt", "Qirato/app/file:stream", "Qirato/app/a\\b", "/Qirato/test", "Qirato/app/file.")) {
            assertFails { WindowsUpdateArchive.extract(archive(listOf(name)), temporary.newFolder().toPath()) }
        }
        assertFails { WindowsUpdateArchive.extract(archive(listOf("Qirato/app/A", "Qirato/app/a")), temporary.newFolder().toPath()) }
        assertFails { WindowsUpdateArchive.extract(archive(listOf("Qirato/Qirato.exe")), temporary.newFolder().toPath()) }
    }

    private class Gateway(val release: WindowsRelease?) : WindowsUpdateGateway {
        var launches = 0; var downloads = 0; var fails = false; var suspendDownload = false
        override suspend fun check() = release
        override suspend fun prepare(release: WindowsRelease, progress: (Long,Long)->Unit, verifying:()->Unit): PreparedWindowsUpdate {
            downloads++; progress(release.size, release.size)
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

    @Test fun backgroundTicksKeepPostponedReadyUpdateQuietButManualCheckReopensIt() = runBlocking {
        val ticks = Channel<Unit>(Channel.UNLIMITED)
        val intervals = Channel<Long>(Channel.UNLIMITED)
        val gateway = Gateway(release("bytes".toByteArray()))
        val updater = WindowsUpdater(gateway, waitForNextCheck = { intervals.send(it); ticks.receive() })
        try {
            updater.start()
            withTimeout(3000) { intervals.receive() }
            assertEquals(WindowsUpdatePhase.READY, updater.state.value.phase); assertTrue(updater.state.value.dialog)
            updater.postpone(); ticks.send(Unit)
            withTimeout(3000) { intervals.receive() }
            assertFalse(updater.state.value.dialog); assertEquals(1, gateway.downloads)
            updater.check(); assertTrue(updater.state.value.dialog)
        } finally { updater.close() }
    }

    @Test fun downloadsAutomaticallyButPostponesWithoutClosingAndReusesReadyPackage() = runBlocking {
        val gateway = Gateway(release("bytes".toByteArray())); val updater = WindowsUpdater(gateway)
        try {
            updater.check()?.join(); assertEquals(WindowsUpdatePhase.READY, updater.state.value.phase)
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
            updater.check()?.join(); gateway.fails = true
            var closed = false; updater.restart { closed = true }?.join()
            assertFalse(closed); assertEquals(WindowsUpdatePhase.READY, updater.state.value.phase); assertNotNull(updater.state.value.error)
            gateway.fails = false; updater.restart { closed = true }?.join(); assertTrue(closed)
        } finally { updater.close() }
    }

    @Test fun cancellationNeverOffersPartialUpdate() = runBlocking {
        val gateway = Gateway(release("bytes".toByteArray())).apply { suspendDownload = true }; val updater = WindowsUpdater(gateway)
        try {
            updater.check()
            withTimeout(3000) { while (updater.state.value.phase != WindowsUpdatePhase.DOWNLOADING) delay(10) }
            updater.cancelDownload()
            withTimeout(3000) { while (updater.state.value.phase != WindowsUpdatePhase.IDLE) delay(10) }
            assertFalse(updater.state.value.dialog); assertNull(updater.restart { fail("Must not close") }); assertEquals(0, gateway.launches)
        } finally { updater.close() }
    }
}
