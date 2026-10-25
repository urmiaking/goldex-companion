package com.goldex.companion.desktop.license

import com.goldex.companion.data.license.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.nio.file.Files
import java.nio.file.Path
import java.text.SimpleDateFormat
import java.util.*

class DesktopLicenseRepository(
    private val directory: Path,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO),
    private val appVersion: String = "0.56.73"
) : LicenseStore {

    private val licenseFile: Path = directory.resolve("license.json")
    private val _licenseInfo = MutableStateFlow(loadCachedLicense())
    override val licenseInfo: StateFlow<LicenseInfo> = _licenseInfo.asStateFlow()

    fun reload() {
        _licenseInfo.value = loadCachedLicense()
    }

    override fun getCachedInfo(): LicenseInfo = _licenseInfo.value

    override suspend fun activateTrial(): LicenseResult = withContext(Dispatchers.IO) {
        val identity = DesktopDeviceIdentity.get(directory, appVersion)
        val payload = JSONObject().apply {
            put("fingerprint", identity.fingerprint)
            put("deviceModel", identity.deviceModel)
            put("manufacturer", identity.manufacturer)
            put("osVersion", identity.osVersion)
            put("appVersion", identity.appVersion)
        }

        try {
            val response = executePost("$BASE_API_URL/trial", payload)
            val json = JSONObject(response.body)
            val success = json.optBoolean("success", false)
            val message = json.optString("message", "")
            val statusStr = json.optString("status", "")

            if (response.code in 200..299 && success) {
                val status = when (statusStr) {
                    "LIFETIME" -> LicenseStatus.LIFETIME
                    else -> LicenseStatus.TRIAL_ACTIVE
                }
                val token = json.optString("licenseToken", "")
                val expiresAt = json.optString("expiresAt", "")
                val remainingDays = json.optInt("remainingDays", 7)

                val newInfo = LicenseInfo(
                    status = status,
                    token = token.ifBlank { null },
                    expiresAt = expiresAt.ifBlank { null },
                    remainingDays = remainingDays,
                    lastVerifiedAt = System.currentTimeMillis(),
                    lastMessage = message
                )
                saveLicense(newInfo)
                LicenseResult.Success(newInfo, message)
            } else {
                if (statusStr == "TRIAL_EXPIRED" || response.code == 403) {
                    val expiredInfo = _licenseInfo.value.copy(
                        status = LicenseStatus.TRIAL_EXPIRED,
                        lastMessage = message
                    )
                    saveLicense(expiredInfo)
                }
                LicenseResult.Error(message.ifBlank { "فعال‌سازی مهلت تست با خطا مواجه شد." }, response.code)
            }
        } catch (e: Exception) {
            if (_licenseInfo.value.status == LicenseStatus.NONE) {
                val fallbackTrial = LicenseInfo(
                    status = LicenseStatus.TRIAL_ACTIVE,
                    remainingDays = 7,
                    lastVerifiedAt = System.currentTimeMillis(),
                    lastMessage = "طرح آزمایشی ۷ روزه فعال شد (همگام‌سازی ابری در اولین اتصال)"
                )
                saveLicense(fallbackTrial)
                return@withContext LicenseResult.Success(fallbackTrial, fallbackTrial.lastMessage ?: "")
            }
            LicenseResult.Error("عدم برقراری ارتباط با سرور لایسنس. لطفاً اتصال اینترنت را بررسی کنید.")
        }
    }

    override suspend fun activateCode(code: String): LicenseResult = withContext(Dispatchers.IO) {
        val cleanCode = code.trim().uppercase()
        if (cleanCode.length < 5) {
            return@withContext LicenseResult.Error("کد اشتراک وارد شده معتبر نیست.")
        }

        val identity = DesktopDeviceIdentity.get(directory, appVersion)
        val payload = JSONObject().apply {
            put("code", cleanCode)
            put("fingerprint", identity.fingerprint)
            put("deviceModel", identity.deviceModel)
            put("manufacturer", identity.manufacturer)
            put("osVersion", identity.osVersion)
            put("appVersion", identity.appVersion)
        }

        try {
            val response = executePost("$BASE_API_URL/activate", payload)
            val json = JSONObject(response.body)
            val success = json.optBoolean("success", false)
            val message = json.optString("message", "")

            if (response.code in 200..299 && success) {
                val token = json.optString("licenseToken", "")
                val expiresAt = json.optString("expiresAt", "")

                val newInfo = LicenseInfo(
                    status = LicenseStatus.LIFETIME,
                    licenseCode = cleanCode,
                    token = token.ifBlank { null },
                    expiresAt = expiresAt.ifBlank { null },
                    remainingDays = 99999,
                    lastVerifiedAt = System.currentTimeMillis(),
                    lastMessage = message
                )
                saveLicense(newInfo)
                LicenseResult.Success(newInfo, message)
            } else {
                LicenseResult.Error(message.ifBlank { "کد فعال‌سازی نامعتبر است یا قبلاً استفاده شده." }, response.code)
            }
        } catch (e: Exception) {
            LicenseResult.Error("خطا در ارتباط با سرور فعال‌سازی. لطفاً اینترنت خود را بررسی نمایید.")
        }
    }

    override suspend fun syncStatus(): LicenseResult = withContext(Dispatchers.IO) {
        val identity = DesktopDeviceIdentity.get(directory, appVersion)
        val payload = JSONObject().apply {
            put("fingerprint", identity.fingerprint)
        }

        try {
            val response = executePost("$BASE_API_URL/status", payload)
            val json = JSONObject(response.body)
            val success = json.optBoolean("success", false)

            if (response.code in 200..299 && success) {
                val statusStr = json.optString("status", "")
                val licenseCode = json.optString("licenseCode", "").ifBlank { null }
                val expiresAt = json.optString("expiresAt", "").ifBlank { null }
                val remainingDays = json.optInt("remainingDays", 0)

                val newStatus = when (statusStr) {
                    "LIFETIME" -> LicenseStatus.LIFETIME
                    "TRIAL_ACTIVE" -> LicenseStatus.TRIAL_ACTIVE
                    "TRIAL_EXPIRED" -> LicenseStatus.TRIAL_EXPIRED
                    "REVOKED" -> LicenseStatus.REVOKED
                    else -> LicenseStatus.NONE
                }

                val current = _licenseInfo.value
                val updated = current.copy(
                    status = newStatus,
                    licenseCode = licenseCode ?: current.licenseCode,
                    expiresAt = expiresAt ?: current.expiresAt,
                    remainingDays = remainingDays,
                    lastVerifiedAt = System.currentTimeMillis()
                )
                saveLicense(updated)
                LicenseResult.Success(updated, "وضعیت اشتراک به‌روزرسانی شد.")
            } else {
                LicenseResult.Error("استعلام وضعیت با خطا مواجه شد.")
            }
        } catch (e: Exception) {
            LicenseResult.Error("عدم دسترسی به اینترنت جهت استعلام وضعیت.")
        }
    }

    private fun saveLicense(info: LicenseInfo) {
        try {
            Files.createDirectories(directory)
            val json = JSONObject().apply {
                put("status", info.status.name)
                put("licenseCode", info.licenseCode ?: "")
                put("token", info.token ?: "")
                put("expiresAt", info.expiresAt ?: "")
                put("remainingDays", info.remainingDays)
                put("lastVerifiedAt", info.lastVerifiedAt)
                put("lastMessage", info.lastMessage ?: "")
            }
            Files.writeString(licenseFile, json.toString(2))
        } catch (_: Exception) {}
        _licenseInfo.value = info
    }

    private fun loadCachedLicense(): LicenseInfo {
        if (!Files.exists(licenseFile)) return LicenseInfo(status = LicenseStatus.NONE)
        return try {
            val json = JSONObject(Files.readString(licenseFile))
            val statusStr = json.optString("status", LicenseStatus.NONE.name)
            val status = try { LicenseStatus.valueOf(statusStr) } catch (_: Exception) { LicenseStatus.NONE }
            val code = json.optString("licenseCode", "").ifBlank { null }
            val token = json.optString("token", "").ifBlank { null }
            val expiresAt = json.optString("expiresAt", "").ifBlank { null }
            val remainingDays = json.optInt("remainingDays", 0)
            val lastVerified = json.optLong("lastVerifiedAt", 0L)
            val lastMsg = json.optString("lastMessage", "").ifBlank { null }

            var evaluatedStatus = status
            if (status == LicenseStatus.TRIAL_ACTIVE && !expiresAt.isNullOrBlank()) {
                if (isDatePast(expiresAt)) {
                    evaluatedStatus = LicenseStatus.TRIAL_EXPIRED
                }
            }

            LicenseInfo(
                status = evaluatedStatus,
                licenseCode = code,
                token = token,
                expiresAt = expiresAt,
                remainingDays = remainingDays,
                lastVerifiedAt = lastVerified,
                lastMessage = lastMsg
            )
        } catch (_: Exception) {
            LicenseInfo(status = LicenseStatus.NONE)
        }
    }

    private fun isDatePast(isoDateStr: String): Boolean {
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            val date = sdf.parse(isoDateStr) ?: return false
            date.before(Date())
        } catch (_: Exception) {
            try {
                val sdfFallback = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }
                val date = sdfFallback.parse(isoDateStr) ?: return false
                date.before(Date())
            } catch (_: Exception) {
                false
            }
        }
    }

    private data class HttpResponse(val code: Int, val body: String)

    private fun executePost(urlString: String, jsonBody: JSONObject): HttpResponse {
        var connection: HttpURLConnection? = null
        return try {
            val url = URL(urlString)
            connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("User-Agent", "QiratoCompanion-Windows")
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                setRequestProperty("Accept", "application/json")
                connectTimeout = 9000
                readTimeout = 9000
                doOutput = true
            }

            OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use { writer ->
                writer.write(jsonBody.toString())
                writer.flush()
            }

            val code = connection.responseCode
            val inputStream = if (code in 200..299) connection.inputStream else connection.errorStream
            val body = if (inputStream != null) {
                BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).use { it.readText() }
            } else ""

            HttpResponse(code, body)
        } finally {
            connection?.disconnect()
        }
    }

    companion object {
        private const val BASE_API_URL = "https://api.qirato.ir/api/v1/license"
    }
}
