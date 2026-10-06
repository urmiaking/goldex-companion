package com.goldex.companion.desktop.update

import kotlinx.coroutines.*
import org.json.JSONArray
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URI
import java.nio.file.*
import java.security.MessageDigest
import kotlin.coroutines.coroutineContext

class WindowsUpdateNetwork {
    suspend fun check(installed: WindowsVersion): WindowsRelease? = withContext(Dispatchers.IO) {
        val pages = mutableListOf<JSONArray>()
        for (page in 1..20) {
            coroutineContext.ensureActive()
            val uri = URI("https://api.github.com/repos/${WindowsReleasePolicy.REPOSITORY}/releases?per_page=100&page=$page")
            val bytes = request(uri, "application/vnd.github+json") { connection ->
                connection.inputStream.use { input ->
                    val output = java.io.ByteArrayOutputStream()
                    val buffer = ByteArray(16384)
                    while (true) {
                        coroutineContext.ensureActive()
                        val count = input.read(buffer); if (count < 0) break
                        if (output.size() + count > 8 * 1024 * 1024) throw IOException("Release metadata too large")
                        output.write(buffer, 0, count)
                    }
                    output.toByteArray()
                }
            }
            val records = JSONArray(bytes.toString(Charsets.UTF_8))
            pages += records
            if (records.length() < 100) return@withContext WindowsReleasePolicy.newest(pages, installed)
        }
        throw IOException("Release history exceeds discovery limit")
    }

    suspend fun download(release: WindowsRelease, destination: Path, progress: (Long, Long) -> Unit) = withContext(Dispatchers.IO) {
            request(release.url, "application/octet-stream") { connection ->
                val length = connection.contentLengthLong
                if (length >= 0 && length != release.size) throw IOException("Download size changed")
                val context = coroutineContext
                connection.inputStream.use { input -> WindowsUpdateDownload.copyVerified(input, destination, release, progress) { context.ensureActive() } }
            }
    }

    private suspend fun <T> request(initial: URI, accept: String, read: suspend (HttpURLConnection) -> T): T {
        var uri = initial
        repeat(6) {
            coroutineContext.ensureActive()
            require(uri.scheme == "https" && uri.host in setOf("api.github.com", "github.com", "release-assets.githubusercontent.com", "objects.githubusercontent.com") && uri.userInfo == null && (uri.port == -1 || uri.port == 443)) { "Untrusted update host" }
            val connection = uri.toURL().openConnection() as HttpURLConnection
            try {
                connection.instanceFollowRedirects = false
                connection.connectTimeout = 15000; connection.readTimeout = 20000
                connection.setRequestProperty("User-Agent", "Qirato-Windows-Updater")
                connection.setRequestProperty("Accept", accept)
                connection.setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
                val status = connection.responseCode
                if (status in setOf(301, 302, 303, 307, 308)) {
                    uri = uri.resolve(connection.getHeaderField("Location") ?: throw IOException("Missing redirect"))
                } else {
                    if (status != 200) throw IOException("Update request failed ($status)")
                    return read(connection)
                }
            } finally { connection.disconnect() }
        }
        throw IOException("Too many update redirects")
    }
}

object WindowsUpdateDownload {
    fun copyVerified(input: java.io.InputStream, destination: Path, release: WindowsRelease,
        progress: (Long, Long) -> Unit = { _, _ -> }, checkActive: () -> Unit = {}) {
        var ownsFile = false
        var completed = false
        try {
            val output = Files.newOutputStream(destination, StandardOpenOption.CREATE_NEW)
            ownsFile = true
            val digest = MessageDigest.getInstance("SHA-256")
            var received = 0L
            var notified = 0L
            output.use {
                val buffer = ByteArray(65536)
                while (true) {
                    checkActive()
                    val count = input.read(buffer); if (count < 0) break
                    received += count
                    if (received > release.size || received > WindowsReleasePolicy.MAX_ARCHIVE_BYTES) throw IOException("Download exceeds declared size")
                    digest.update(buffer, 0, count); output.write(buffer, 0, count)
                    if (received - notified >= 262144 || received == release.size) { progress(received, release.size); notified = received }
                }
            }
            val actual = digest.digest().joinToString("") { "%02x".format(it.toInt() and 255) }
            if (received != release.size || actual != release.sha256) throw IOException("Download verification failed")
            completed = true
        } finally { if (ownsFile && !completed) Files.deleteIfExists(destination) }
    }
}
