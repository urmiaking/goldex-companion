package com.goldex.companion.data.license

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.provider.Settings
import com.goldex.companion.BuildConfig
import java.security.MessageDigest
import java.util.UUID

data class DeviceIdentity(
    val fingerprint: String,
    val deviceModel: String,
    val manufacturer: String,
    val osVersion: String,
    val appVersion: String
)

object DeviceIdentityManager {
    private const val PREFS_NAME = "qirato_device_identity"
    private const val KEY_CACHED_FINGERPRINT = "cached_device_fingerprint"

    @SuppressLint("HardwareIds")
    fun getDeviceIdentity(context: Context): DeviceIdentity {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        var cached = prefs.getString(KEY_CACHED_FINGERPRINT, null)

        if (cached.isNullOrBlank()) {
            cached = generateStableFingerprint(context)
            prefs.edit().putString(KEY_CACHED_FINGERPRINT, cached).apply()
        }

        return DeviceIdentity(
            fingerprint = cached,
            deviceModel = Build.MODEL ?: "Android Device",
            manufacturer = Build.MANUFACTURER ?: "Unknown",
            osVersion = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
            appVersion = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})"
        )
    }

    @SuppressLint("HardwareIds")
    private fun generateStableFingerprint(context: Context): String {
        return try {
            val androidId = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ANDROID_ID
            ) ?: ""

            val rawData = buildString {
                append(androidId)
                append("|")
                append(Build.BOARD ?: "")
                append("|")
                append(Build.BRAND ?: "")
                append("|")
                append(Build.DEVICE ?: "")
                append("|")
                append(Build.HARDWARE ?: "")
                append("|")
                append(Build.MANUFACTURER ?: "")
                append("|")
                append(Build.MODEL ?: "")
            }

            val md = MessageDigest.getInstance("SHA-256")
            val digest = md.digest(rawData.toByteArray(Charsets.UTF_8))
            val hex = digest.joinToString("") { "%02x".format(it) }
            
            hex.take(32)
        } catch (_: Exception) {
            UUID.randomUUID().toString().replace("-", "")
        }
    }
}
