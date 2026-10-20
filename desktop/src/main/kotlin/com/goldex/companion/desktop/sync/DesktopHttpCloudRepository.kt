package com.goldex.companion.desktop.sync

import com.goldex.companion.data.license.LicenseStore
import com.goldex.companion.data.license.LicenseResult
import com.goldex.companion.desktop.license.DesktopDeviceIdentity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.nio.file.Path

class DesktopCloudException(val code: String, val status: Int = 0, val details: JSONObject? = null, val retryAfter: Long = 0) : Exception(code)

class DesktopHttpCloudRepository(
    private val directory: Path,
    val keys: DesktopCloudKeys,
    private val licenseStore: LicenseStore,
    private val appVersion: String = "0.56.75"
) {
    private val refreshMutex = Mutex()

    private suspend fun raw(path: String, body: JSONObject?, access: String? = null): JSONObject = withContext(Dispatchers.IO) {
        val connection = URL("https://api.qirato.ir/api/v1/$path").openConnection() as HttpURLConnection
        try {
            connection.requestMethod = if (body == null) "GET" else "POST"
            connection.connectTimeout = 15000
            connection.readTimeout = 30000
            connection.setRequestProperty("User-Agent", "QiratoCompanion-Windows")
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            if (access != null) connection.setRequestProperty("Authorization", "Bearer $access")
            if (body != null) {
                connection.doOutput = true
                connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            }
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val text = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            val json = try { JSONObject(text) } catch (_: Exception) { throw DesktopCloudException("SERVER_ERROR", status) }
            if (status !in 200..299) throw DesktopCloudException(json.optString("code", "SERVER_ERROR"), status, json.optJSONObject("details"), connection.getHeaderField("Retry-After")?.toLongOrNull() ?: 0)
            json
        } finally {
            connection.disconnect()
        }
    }

    suspend fun request(path: String, body: JSONObject?): JSONObject {
        val before = keys.read() ?: throw DesktopCloudException("AUTH_REQUIRED", 401)
        try {
            return raw(path, body, before.getString("accessToken"))
        } catch (e: DesktopCloudException) {
            if (e.status != 401 || e.code != "AUTH_REQUIRED") throw e
        }
        refreshMutex.withLock {
            val current = keys.read() ?: throw DesktopCloudException("AUTH_REQUIRED", 401)
            if (current.getString("accessToken") == before.getString("accessToken")) {
                val token = current.getString("refreshToken")
                val fresh = raw("auth/refresh", JSONObject().put("refreshToken", token).put("signature", keys.sign("refresh:$token")))
                current.put("accessToken", fresh.getString("accessToken")).put("refreshToken", fresh.getString("refreshToken"))
                keys.write(current)
            }
        }
        return raw(path, body, keys.read()!!.getString("accessToken"))
    }

    suspend fun requestCode(phone: String): JSONObject = raw("auth/otp/request", JSONObject().put("phone", phone))

    suspend fun verifyCode(challenge: JSONObject, code: String, phone: String): JSONObject {
        val identity = DesktopDeviceIdentity.get(directory, appVersion)
        val public = keys.publicKey()
        var license = licenseStore.getCachedInfo()
        if (license.token == null) {
            val trialResult = licenseStore.activateTrial()
            if (trialResult is LicenseResult.Success) {
                license = trialResult.info
            }
        }
        val token = license.token ?: throw DesktopCloudException("LICENSE_REQUIRED", 403)
        val id = challenge.getString("challengeId")
        val nonce = challenge.getString("nonce")
        val result = raw(
            "auth/otp/verify",
            JSONObject()
                .put("challengeId", id)
                .put("code", code)
                .put("fingerprint", identity.fingerprint)
                .put("publicKey", public)
                .put("signature", keys.sign("otp:$id:$nonce:${identity.fingerprint}:$public"))
                .put("licenseToken", token)
        )
        result.put("phone", phone)
        return result
    }

    suspend fun logout() {
        try { request("auth/logout", JSONObject()) } finally { keys.clear() }
    }
}
