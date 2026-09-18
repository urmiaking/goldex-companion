package com.goldex.companion.data.license

import android.content.Context
import android.content.SharedPreferences
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class LicenseRepository(private val context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _licenseInfo = MutableStateFlow(loadCachedLicense())
    val licenseInfo: StateFlow<LicenseInfo> = _licenseInfo.asStateFlow()

    fun getCachedInfo(): LicenseInfo = _licenseInfo.value

    /**
     * فعال‌سازی مهلت تست ۱۴ روزه رایگان
     */
    suspend fun activateTrial(): LicenseResult = withContext(Dispatchers.IO) {
        val identity = DeviceIdentityManager.getDeviceIdentity(context)
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
                val remainingDays = json.optInt("remainingDays", 14)

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
            LicenseResult.Error("عدم برقراری ارتباط با سرور لایسنس. لطفاً اتصال اینترنت را بررسی کنید.")
        }
    }

    /**
     * فعال‌سازی با کد لایسنس دائمی (مثلاً QIR-XXXX-XXXX)
     */
    suspend fun activateCode(code: String): LicenseResult = withContext(Dispatchers.IO) {
        val cleanCode = code.trim().uppercase()
        if (cleanCode.length < 5) {
            return@withContext LicenseResult.Error("کد اشتراک وارد شده معتبر نیست.")
        }

        val identity = DeviceIdentityManager.getDeviceIdentity(context)
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

    /**
     * استعلام آخرین وضعیت لایسنس و همگام‌سازی تاریخ سرور
     */
    suspend fun syncStatus(): LicenseResult = withContext(Dispatchers.IO) {
        val identity = DeviceIdentityManager.getDeviceIdentity(context)
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
            // در حالت آفلاین، کش محلی معتبر را حفظ می‌کنیم
            LicenseResult.Error("عدم دسترسی به اینترنت جهت استعلام وضعیت.")
        }
    }

    private fun saveLicense(info: LicenseInfo) {
        prefs.edit()
            .putString(KEY_STATUS, info.status.name)
            .putString(KEY_CODE, info.licenseCode)
            .putString(KEY_TOKEN, info.token)
            .putString(KEY_EXPIRES_AT, info.expiresAt)
            .putInt(KEY_REMAINING_DAYS, info.remainingDays)
            .putLong(KEY_LAST_VERIFIED, info.lastVerifiedAt)
            .putString(KEY_LAST_MESSAGE, info.lastMessage)
            .apply()

        _licenseInfo.value = info
    }

    private fun loadCachedLicense(): LicenseInfo {
        val statusStr = prefs.getString(KEY_STATUS, LicenseStatus.NONE.name) ?: LicenseStatus.NONE.name
        val status = try {
            LicenseStatus.valueOf(statusStr)
        } catch (_: Exception) {
            LicenseStatus.NONE
        }

        val licenseCode = prefs.getString(KEY_CODE, null)
        val token = prefs.getString(KEY_TOKEN, null)
        val expiresAt = prefs.getString(KEY_EXPIRES_AT, null)
        val remainingDays = prefs.getInt(KEY_REMAINING_DAYS, 0)
        val lastVerified = prefs.getLong(KEY_LAST_VERIFIED, 0L)
        val lastMessage = prefs.getString(KEY_LAST_MESSAGE, null)

        // بررسی آفلاین انقضا برای مهلت تست
        var evaluatedStatus = status
        if (status == LicenseStatus.TRIAL_ACTIVE && !expiresAt.isNullOrBlank()) {
            val isExpired = isDatePast(expiresAt)
            if (isExpired) {
                evaluatedStatus = LicenseStatus.TRIAL_EXPIRED
            }
        }

        return LicenseInfo(
            status = evaluatedStatus,
            licenseCode = licenseCode,
            token = token,
            expiresAt = expiresAt,
            remainingDays = remainingDays,
            lastVerifiedAt = lastVerified,
            lastMessage = lastMessage
        )
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
                setRequestProperty("User-Agent", "QiratoCompanion-Android")
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
        private const val PREFS_NAME = "qirato_license_prefs"

        private const val KEY_STATUS = "key_license_status"
        private const val KEY_CODE = "key_license_code"
        private const val KEY_TOKEN = "key_license_token"
        private const val KEY_EXPIRES_AT = "key_license_expires_at"
        private const val KEY_REMAINING_DAYS = "key_remaining_days"
        private const val KEY_LAST_VERIFIED = "key_last_verified_time"
        private const val KEY_LAST_MESSAGE = "key_last_message"
    }
}
