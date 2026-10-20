package com.goldex.companion.desktop.sync

import com.goldex.companion.data.license.LicenseStore
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.json.JSONObject
import java.nio.file.Files
import java.nio.file.Path

enum class DesktopSyncStatus(val titleFa: String) {
    DISABLED("غیرفعال"),
    AUTH_REQUIRED("نیاز به ورود"),
    PENDING("در انتظار همگام‌سازی"),
    SYNCING("در حال ارسال و دریافت"),
    SYNCED("همگام با سرور"),
    OFFLINE("آفلاین"),
    ERROR("خطا در همگام‌سازی")
}

data class DesktopSyncUiState(
    val enabled: Boolean = false,
    val status: DesktopSyncStatus = DesktopSyncStatus.DISABLED,
    val phone: String = "",
    val lastSuccessAt: Long = 0L,
    val busy: Boolean = false,
    val message: String = "",
    val pendingCount: Int = 0
)

class DesktopSyncCoordinator(
    private val directory: Path,
    val licenseStore: LicenseStore,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {
    val keys = DesktopCloudKeys(directory)
    val api = DesktopHttpCloudRepository(directory, keys, licenseStore)
    private val configFile = directory.resolve("cloud_config.json")

    private val _state = MutableStateFlow(loadConfig())
    val state: StateFlow<DesktopSyncUiState> = _state.asStateFlow()

    fun reload() {
        _state.value = loadConfig()
    }

    init {
        val session = keys.read()
        if (session != null && _state.value.enabled) {
            val phone = session.optString("phone", _state.value.phone)
            _state.update { it.copy(status = DesktopSyncStatus.SYNCED, phone = phone) }
        }
    }

    fun setEnabled(enabled: Boolean) {
        _state.update { it.copy(enabled = enabled, status = if (enabled) (if (keys.read() != null) DesktopSyncStatus.SYNCED else DesktopSyncStatus.AUTH_REQUIRED) else DesktopSyncStatus.DISABLED) }
        saveConfig()
    }

    suspend fun requestOtp(phone: String): Result<JSONObject> = withContext(Dispatchers.IO) {
        try {
            _state.update { it.copy(busy = true, message = "در حال ارسال کد تأیید...") }
            val res = api.requestCode(phone)
            _state.update { it.copy(busy = false, message = "کد تأیید ارسال شد.") }
            Result.success(res)
        } catch (e: Exception) {
            _state.update { it.copy(busy = false, message = e.message ?: "خطا در ارسال کد تأیید") }
            Result.failure(e)
        }
    }

    suspend fun verifyOtp(challenge: JSONObject, code: String, phone: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            _state.update { it.copy(busy = true, message = "در حال اعتبارسنجی...") }
            val res = api.verifyCode(challenge, code, phone)
            keys.write(res)
            _state.update { it.copy(busy = false, enabled = true, status = DesktopSyncStatus.SYNCED, phone = phone, message = "ورود ابری موفقیت‌آمیز بود.") }
            saveConfig()
            Result.success(Unit)
        } catch (e: Exception) {
            _state.update { it.copy(busy = false, message = e.message ?: "کد نامعتبر است.") }
            Result.failure(e)
        }
    }

    suspend fun syncNow(): Result<Unit> = withContext(Dispatchers.IO) {
        if (keys.read() == null) {
            _state.update { it.copy(status = DesktopSyncStatus.AUTH_REQUIRED) }
            return@withContext Result.failure(Exception("نیاز به ورود به حساب ابری"))
        }
        try {
            _state.update { it.copy(busy = true, status = DesktopSyncStatus.SYNCING, message = "در حال همگام‌سازی...") }
            // Query cloud
            api.request("sync/pull", JSONObject().put("since", _state.value.lastSuccessAt))
            val now = System.currentTimeMillis()
            _state.update { it.copy(busy = false, status = DesktopSyncStatus.SYNCED, lastSuccessAt = now, message = "همگام‌سازی با موفقیت انجام شد.") }
            saveConfig()
            Result.success(Unit)
        } catch (e: Exception) {
            _state.update { it.copy(busy = false, status = DesktopSyncStatus.ERROR, message = e.message ?: "خطا در همگام‌سازی") }
            Result.failure(e)
        }
    }

    suspend fun logout() = withContext(Dispatchers.IO) {
        try { api.logout() } catch (_: Exception) {}
        _state.update { it.copy(enabled = false, status = DesktopSyncStatus.DISABLED, phone = "", message = "") }
        saveConfig()
    }

    private fun saveConfig() {
        try {
            val s = _state.value
            val json = JSONObject().apply {
                put("enabled", s.enabled)
                put("phone", s.phone)
                put("lastSuccessAt", s.lastSuccessAt)
            }
            Files.writeString(configFile, json.toString())
        } catch (_: Exception) {}
    }

    private fun loadConfig(): DesktopSyncUiState {
        if (!Files.exists(configFile)) return DesktopSyncUiState()
        return try {
            val json = JSONObject(Files.readString(configFile))
            DesktopSyncUiState(
                enabled = json.optBoolean("enabled", false),
                phone = json.optString("phone", ""),
                lastSuccessAt = json.optLong("lastSuccessAt", 0L)
            )
        } catch (_: Exception) {
            DesktopSyncUiState()
        }
    }
}
