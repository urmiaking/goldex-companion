package com.goldex.companion.data

import com.goldex.companion.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

data class UpdateInfo(
    val isAvailable: Boolean = false,
    val latestVersion: String = "",
    val releaseNotes: String = "",
    val downloadUrl: String = "",
    val releaseName: String = "",
    val releasePageUrl: String = ""
)

object AppUpdateChecker {
    private const val GITHUB_API_URL = "https://api.github.com/repos/urmiaking/goldex-companion/releases/latest"

    suspend fun check(): UpdateInfo = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            val url = URL(GITHUB_API_URL)
            connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("User-Agent", "GoldExCompanion-Android")
                setRequestProperty("Accept", "application/vnd.github.v3+json")
                connectTimeout = 8000
                readTimeout = 8000
            }

            if (connection.responseCode != 200) {
                return@withContext UpdateInfo()
            }

            val response = BufferedReader(InputStreamReader(connection.inputStream)).use { it.readText() }
            val json = JSONObject(response)

            val tagName = json.optString("tag_name", "").trim()
            val releaseName = json.optString("name", tagName).trim()
            val releaseNotes = json.optString("body", "").trim()
            val releasePageUrl = json.optString("html_url", "https://github.com/urmiaking/goldex-companion/releases").trim()

            var downloadUrl = ""
            val assets = json.optJSONArray("assets")
            if (assets != null) {
                for (i in 0 until assets.length()) {
                    val asset = assets.optJSONObject(i) ?: continue
                    val name = asset.optString("name", "")
                    if (name.endsWith(".apk", ignoreCase = true)) {
                        downloadUrl = asset.optString("browser_download_url", "")
                        break
                    }
                }
            }
            if (downloadUrl.isEmpty()) {
                downloadUrl = releasePageUrl
            }

            val currentVersion = BuildConfig.VERSION_NAME
            val isNewer = isVersionNewer(tagName, currentVersion)

            UpdateInfo(
                isAvailable = isNewer,
                latestVersion = tagName,
                releaseNotes = releaseNotes,
                downloadUrl = downloadUrl,
                releaseName = releaseName,
                releasePageUrl = releasePageUrl
            )
        } catch (e: Exception) {
            UpdateInfo()
        } finally {
            connection?.disconnect()
        }
    }

    fun isVersionNewer(remoteTag: String, currentVersion: String): Boolean {
        try {
            val cleanRemote = remoteTag.trim()
                .trimStart('v', 'V')
                .substringBefore('-')
                .substringBefore('+')
                .trim()
            val cleanCurrent = currentVersion.trim()
                .trimStart('v', 'V')
                .substringBefore('-')
                .substringBefore('+')
                .trim()

            val remoteParts = cleanRemote.split(".").mapNotNull { it.trim().toIntOrNull() }
            val currentParts = cleanCurrent.split(".").mapNotNull { it.trim().toIntOrNull() }

            if (remoteParts.isEmpty() || currentParts.isEmpty()) return false

            val maxLen = maxOf(remoteParts.size, currentParts.size)
            for (i in 0 until maxLen) {
                val r = remoteParts.getOrElse(i) { 0 }
                val c = currentParts.getOrElse(i) { 0 }
                if (r > c) return true
                if (r < c) return false
            }
            return false
        } catch (e: Exception) {
            return false
        }
    }
}
