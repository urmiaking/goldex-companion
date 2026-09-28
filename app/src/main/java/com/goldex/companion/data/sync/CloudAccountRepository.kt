package com.goldex.companion.data.sync

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.AtomicFile
import android.util.Base64
import com.goldex.companion.data.license.DeviceIdentityManager
import com.goldex.companion.data.license.LicenseRepository
import com.goldex.companion.data.license.LicenseResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.*
import java.security.spec.ECGenParameterSpec
import javax.crypto.*
import javax.crypto.spec.GCMParameterSpec

class CloudException(val code: String, val status: Int = 0, val details: JSONObject? = null, val retryAfter: Long = 0) : Exception(code)
interface AccountRepository {
    suspend fun requestCode(phone: String): JSONObject
    suspend fun verifyCode(challenge: JSONObject, code: String, phone: String): JSONObject
    suspend fun logout()
}
interface CloudSyncRepository { suspend fun request(path: String, body: JSONObject? = null): JSONObject }

class DeviceCloudKeys(context: Context) {
    private val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
    private val file = AtomicFile(File(context.noBackupFilesDir, "cloud_session.enc"))
    private val signAlias = "qirato-cloud-device-v1"
    private val encAlias = "qirato-cloud-session-v1"
    @Synchronized private fun signingKey(): PrivateKey {
        if (!store.containsAlias(signAlias)) KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC, "AndroidKeyStore").apply {
            initialize(KeyGenParameterSpec.Builder(signAlias, KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY)
                .setAlgorithmParameterSpec(ECGenParameterSpec("secp256r1")).setDigests(KeyProperties.DIGEST_SHA256).build())
        }.generateKeyPair()
        return store.getKey(signAlias, null) as PrivateKey
    }
    fun publicKey(): String { signingKey(); return Base64.encodeToString(store.getCertificate(signAlias).publicKey.encoded, Base64.NO_WRAP) }
    fun sign(message: String): String = Base64.encodeToString(Signature.getInstance("SHA256withECDSA").run {
        initSign(signingKey()); update(message.toByteArray(Charsets.UTF_8)); sign()
    }, Base64.NO_WRAP)
    @Synchronized private fun encryptionKey(): SecretKey {
        if (!store.containsAlias(encAlias)) KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
            init(KeyGenParameterSpec.Builder(encAlias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build())
        }.generateKey()
        return store.getKey(encAlias, null) as SecretKey
    }
    @Synchronized fun read(): JSONObject? {
        if (!file.baseFile.exists()) return null
        return try {
            val bytes = file.readFully(); require(bytes.size > 12)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.DECRYPT_MODE, encryptionKey(), GCMParameterSpec(128, bytes.copyOfRange(0,12))) }
            JSONObject(String(cipher.doFinal(bytes.copyOfRange(12,bytes.size)), Charsets.UTF_8))
        } catch (_: Exception) { throw CloudException("AUTH_REQUIRED", 401) }
    }
    @Synchronized fun write(value: JSONObject) {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.ENCRYPT_MODE, encryptionKey()) }
        val encrypted = cipher.iv + cipher.doFinal(value.toString().toByteArray(Charsets.UTF_8))
        val output = file.startWrite()
        try { output.write(encrypted); file.finishWrite(output) } catch (e: Exception) { file.failWrite(output); throw e }
    }
    fun clear() { file.delete() }
}

class HttpCloudRepository(private val context: Context, val keys: DeviceCloudKeys) : AccountRepository, CloudSyncRepository {
    private val refreshMutex = Mutex()
    private suspend fun raw(path: String, body: JSONObject?, access: String? = null): JSONObject = withContext(Dispatchers.IO) {
        val connection = URL("https://api.qirato.ir/api/v1/$path").openConnection() as HttpURLConnection
        try {
            connection.requestMethod = if (body == null) "GET" else "POST"
            connection.connectTimeout = 15000; connection.readTimeout = 30000
            connection.setRequestProperty("User-Agent", "QiratoCompanion-Android")
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            if (access != null) connection.setRequestProperty("Authorization", "Bearer $access")
            if (body != null) { connection.doOutput = true; connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) } }
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val text = stream?.bufferedReader(Charsets.UTF_8)?.use { reader ->
                val builder = StringBuilder(); val buffer = CharArray(8192)
                while (true) { val n = reader.read(buffer); if (n < 0) break; builder.append(buffer,0,n); if (builder.length > 16 * 1024 * 1024) throw CloudException("RESPONSE_TOO_LARGE") }
                builder.toString()
            }.orEmpty()
            val json = try { JSONObject(text) } catch (_: Exception) { throw CloudException("SERVER_ERROR", status) }
            if (status !in 200..299) throw CloudException(json.optString("code", "SERVER_ERROR"), status, json.optJSONObject("details"), connection.getHeaderField("Retry-After")?.toLongOrNull() ?: 0)
            json
        } finally { connection.disconnect() }
    }
    override suspend fun request(path: String, body: JSONObject?): JSONObject {
        val before = keys.read() ?: throw CloudException("AUTH_REQUIRED",401)
        try { return raw(path, body, before.getString("accessToken")) }
        catch (e: CloudException) { if (e.status != 401 || e.code != "AUTH_REQUIRED") throw e }
        refreshMutex.withLock {
            val current = keys.read() ?: throw CloudException("AUTH_REQUIRED",401)
            if (current.getString("accessToken") == before.getString("accessToken")) {
                val token = current.getString("refreshToken")
                val fresh = raw("auth/refresh", JSONObject().put("refreshToken",token).put("signature",keys.sign("refresh:$token")))
                current.put("accessToken",fresh.getString("accessToken")).put("refreshToken",fresh.getString("refreshToken")); keys.write(current)
            }
        }
        return raw(path,body,keys.read()!!.getString("accessToken"))
    }
    override suspend fun requestCode(phone: String): JSONObject = raw("auth/otp/request",JSONObject().put("phone",phone))
    override suspend fun verifyCode(challenge: JSONObject, code: String, phone: String): JSONObject {
        val identity = DeviceIdentityManager.getDeviceIdentity(context)
        val public = keys.publicKey()
        val licenseRepo = LicenseRepository(context)
        var license = licenseRepo.getCachedInfo()
        if (license.token == null) {
            val trialResult = licenseRepo.activateTrial()
            if (trialResult is LicenseResult.Success) {
                license = trialResult.info
            }
        }
        val token = license.token ?: throw CloudException("LICENSE_REQUIRED",403)
        val id = challenge.getString("challengeId"); val nonce = challenge.getString("nonce")
        val result = raw("auth/otp/verify",JSONObject().put("challengeId",id).put("code",code).put("fingerprint",identity.fingerprint)
            .put("publicKey",public).put("signature",keys.sign("otp:$id:$nonce:${identity.fingerprint}:$public")).put("licenseToken",token))
        result.put("phone",phone)
        return result
    }
    override suspend fun logout() { try { request("auth/logout",JSONObject()) } finally { keys.clear() } }
}
