package com.goldex.companion.desktop.update

import org.json.JSONArray
import java.net.URI

data class WindowsVersion(val major: Int, val minor: Int, val patch: Int) : Comparable<WindowsVersion> {
    override fun compareTo(other: WindowsVersion) = compareValuesBy(this, other, { it.major }, { it.minor }, { it.patch })
    override fun toString() = "$major.$minor.$patch"
    companion object {
        fun parse(value: String): WindowsVersion? {
            if (!value.matches(Regex("[0-9]+\\.[0-9]+\\.[0-9]+"))) return null
            val parts = value.split('.').map { it.toIntOrNull() ?: return null }
            return WindowsVersion(parts[0], parts[1], parts[2])
        }
    }
}

data class WindowsRelease(val version: WindowsVersion, val tag: String, val notes: String, val url: URI, val size: Long, val sha256: String) {
    val installerName get() = "Qirato-Windows-x64-$version.msi"
}

/** Windows discovery deliberately does not use /releases/latest (the Android channel). */
object WindowsReleasePolicy {
    const val REPOSITORY = "urmiaking/goldex-companion"
    const val MAX_ARCHIVE_BYTES = 300L * 1024 * 1024

    fun newest(pages: List<JSONArray>, installed: WindowsVersion): WindowsRelease? {
        val candidates = pages.flatMap { page -> (0 until page.length()).map { page.getJSONObject(it) } }
            .filter { !it.optBoolean("draft") && !it.optBoolean("prerelease") }
            .mapNotNull { release ->
                val tag = release.optString("tag_name")
                val match = Regex("(?:windows-)?v([0-9]+\\.[0-9]+\\.[0-9]+)").matchEntire(tag) ?: return@mapNotNull null
                val version = WindowsVersion.parse(match.groupValues[1]) ?: return@mapNotNull null
                if (version <= installed) return@mapNotNull null
                val assets = release.optJSONArray("assets") ?: return@mapNotNull null
                val asset = (0 until assets.length()).map { assets.getJSONObject(it) }.singleOrNull {
                    it.optString("name") == "Qirato-Windows-x64-$version.msi" && it.optString("state") == "uploaded"
                } ?: return@mapNotNull null
                Triple(version, release, asset)
            }
        val (version, release, asset) = candidates.maxByOrNull { it.first } ?: return null
        val tag = release.getString("tag_name")
        val expected = URI("https://github.com/$REPOSITORY/releases/download/$tag/Qirato-Windows-x64-$version.msi")
        require(URI(asset.getString("browser_download_url")) == expected) { "Unexpected release download URL" }
        val digest = asset.optString("digest")
        require(digest.matches(Regex("sha256:[a-fA-F0-9]{64}"))) { "Release checksum unavailable" }
        val size = asset.getLong("size")
        require(size in 1..MAX_ARCHIVE_BYTES) { "Invalid release size" }
        return WindowsRelease(version, tag, release.optString("body").take(6000), expected, size, digest.substringAfter(':').lowercase())
    }
}
