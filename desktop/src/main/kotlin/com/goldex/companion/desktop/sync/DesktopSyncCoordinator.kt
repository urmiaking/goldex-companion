package com.goldex.companion.desktop.sync

import com.goldex.companion.data.license.LicenseStore
import com.goldex.companion.desktop.data.DesktopDataStore
import com.goldex.companion.desktop.data.db.DesktopSqliteDatabase
import com.goldex.companion.desktop.data.db.DesktopSyncStaging
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.json.JSONArray
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
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO),
    val database: DesktopSqliteDatabase? = null,
    private val onDataChanged: (() -> Unit)? = null
) {
    constructor(
        storage: DesktopDataStore,
        licenseStore: LicenseStore,
        scope: CoroutineScope = CoroutineScope(Dispatchers.IO),
        onDataChanged: (() -> Unit)? = null
    ) : this(
        directory = storage.directory,
        licenseStore = licenseStore,
        scope = scope,
        database = storage.database,
        onDataChanged = onDataChanged
    )

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
        val pendingCount = database?.outboxCount() ?: 0
        if (session != null && _state.value.enabled) {
            val phone = session.optString("phone", _state.value.phone)
            _state.update {
                it.copy(
                    status = if (pendingCount > 0) DesktopSyncStatus.PENDING else DesktopSyncStatus.SYNCED,
                    phone = phone,
                    pendingCount = pendingCount
                )
            }
        }
    }

    fun setEnabled(enabled: Boolean) {
        val pendingCount = database?.outboxCount() ?: 0
        _state.update {
            it.copy(
                enabled = enabled,
                pendingCount = pendingCount,
                status = if (enabled) {
                    if (keys.read() != null) {
                        if (pendingCount > 0) DesktopSyncStatus.PENDING else DesktopSyncStatus.SYNCED
                    } else {
                        DesktopSyncStatus.AUTH_REQUIRED
                    }
                } else {
                    DesktopSyncStatus.DISABLED
                }
            )
        }
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
            syncNow()
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
        val db = database
        if (db == null) {
            val now = System.currentTimeMillis()
            _state.update { it.copy(status = DesktopSyncStatus.SYNCED, lastSuccessAt = now, message = "همگام‌سازی با موفقیت انجام شد.") }
            saveConfig()
            return@withContext Result.success(Unit)
        }
        try {
            _state.update { it.copy(busy = true, status = DesktopSyncStatus.SYNCING, message = "در حال همگام‌سازی...") }
            val bootstrap = api.request("cloud/bootstrap", null)
            val workspaceId = bootstrap.getString("workspaceId")
            val writerEpoch = bootstrap.getLong("writerEpoch")
            val cloudInitialized = bootstrap.getBoolean("initialized")

            var cp = db.checkpoint()
            if (cp.workspaceId.isNotBlank() && cp.workspaceId != workspaceId) {
                throw DesktopCloudException("ACCOUNT_SWITCH_REQUIRES_BACKUP")
            }

            var changesApplied = false

            // Initial snapshot if local not yet initialized
            if (!cp.initialized) {
                if (cloudInitialized) {
                    val start = api.request("cloud/snapshot/start", JSONObject().put("direction", "download").put("writerEpoch", writerEpoch))
                    val snapshotId = start.getString("snapshotId")
                    val pages = start.getInt("pages")
                    for (p in 0 until pages) {
                        val pageResult = api.request("cloud/snapshot/page", JSONObject().put("snapshotId", snapshotId).put("page", p))
                        val records = pageResult.getJSONArray("records")
                        db.stageSnapshot(DesktopSyncStaging(snapshotId, p, records.toString()))
                    }
                    db.activateSnapshot(snapshotId, workspaceId, start.getLong("revision"), writerEpoch, bootstrap.optLong("lastSequence", 0L))
                    cp = db.checkpoint()
                    changesApplied = true
                } else {
                    val snapshotId = cp.uploadSnapshotId.ifBlank {
                        val started = api.request("cloud/snapshot/start", JSONObject().put("direction", "upload").put("writerEpoch", writerEpoch))
                        val id = started.getString("snapshotId")
                        db.prepareUpload(id, workspaceId, writerEpoch)
                        id
                    }
                    val pages = db.stagedSnapshot(snapshotId)
                    for (page in pages) {
                        api.request("cloud/snapshot/page", JSONObject().put("snapshotId", snapshotId).put("page", page.page).put("records", JSONArray(page.payload)))
                    }
                    api.request("cloud/snapshot/commit", JSONObject().put("snapshotId", snapshotId).put("pages", pages.size))
                    db.saveCheckpoint(cp.copy(initialized = true, uploadSnapshotId = ""))
                    db.clearStagedSnapshot(snapshotId)
                    cp = db.checkpoint()
                }
            }

            // Push pending outbox changes
            while (true) {
                val item = db.firstOutbox() ?: break
                val body = JSONObject(item.payload)
                val changes = body.getJSONArray("changes")
                for (i in 0 until changes.length()) {
                    changes.getJSONObject(i).apply {
                        remove("localVersion")
                        remove("localPayload")
                    }
                }
                body.put("protocolVersion", 1)
                    .put("operationId", item.operationId)
                    .put("deviceSequence", item.sequence)
                    .put("writerEpoch", cp.writerEpoch)
                val ack = api.request("cloud/sync/push", body)
                db.acknowledge(item, ack)
            }

            // Pull remote changes
            var hasMore = true
            var upperRevision: Long? = null
            while (hasMore) {
                val pullBody = JSONObject().put("workspaceId", cp.workspaceId).put("cursor", db.checkpoint().cursor)
                if (upperRevision != null) pullBody.put("upperRevision", upperRevision)
                val result = api.request("cloud/sync/pull", pullBody)
                upperRevision = result.getLong("upperRevision")
                hasMore = result.getBoolean("hasMore")
                val groups = result.getJSONArray("groups")
                if (groups.length() > 0) {
                    db.applyPull(result)
                    changesApplied = true
                } else {
                    db.saveCheckpoint(db.checkpoint().copy(cursor = result.getLong("cursor")))
                }
            }

            if (changesApplied) {
                onDataChanged?.invoke()
            }

            val now = System.currentTimeMillis()
            db.saveCheckpoint(db.checkpoint().copy(lastSuccessAt = now))
            val pendingCount = db.outboxCount()
            _state.update {
                it.copy(
                    busy = false,
                    status = if (pendingCount == 0) DesktopSyncStatus.SYNCED else DesktopSyncStatus.PENDING,
                    lastSuccessAt = now,
                    pendingCount = pendingCount,
                    message = "همگام‌سازی با موفقیت انجام شد."
                )
            }
            saveConfig()
            Result.success(Unit)
        } catch (e: Exception) {
            val status = when ((e as? DesktopCloudException)?.code) {
                "AUTH_REQUIRED", "FRESH_OTP_REQUIRED" -> DesktopSyncStatus.AUTH_REQUIRED
                else -> DesktopSyncStatus.ERROR
            }
            _state.update { it.copy(busy = false, status = status, message = e.message ?: "خطا در همگام‌سازی") }
            Result.failure(e)
        }
    }

    suspend fun logout() = withContext(Dispatchers.IO) {
        try { api.logout() } catch (_: Exception) {}
        database?.clearOutbox()
        _state.update { it.copy(enabled = false, status = DesktopSyncStatus.DISABLED, phone = "", message = "", pendingCount = 0) }
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
