package com.goldex.companion.desktop.sync

import com.goldex.companion.data.license.LicenseStore
import com.goldex.companion.data.sync.SyncJson
import com.goldex.companion.desktop.data.DesktopDataStore
import com.goldex.companion.desktop.data.db.DesktopSqliteDatabase
import com.goldex.companion.desktop.data.db.DesktopSyncStaging
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONArray
import org.json.JSONObject
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import java.util.UUID

enum class DesktopSyncStatus(val titleFa: String) {
    DISABLED("غیرفعال"),
    AUTH_REQUIRED("نیاز به ورود"),
    PENDING("در انتظار همگام‌سازی"),
    SYNCING("در حال ارسال و دریافت"),
    SYNCED("همگام با سرور"),
    OFFLINE("آفلاین"),
    RESTORE_REQUIRED("آماده بازیابی حساب"),
    ERROR("خطا در همگام‌سازی")
}

data class DesktopSyncUiState(
    val enabled: Boolean = false,
    val status: DesktopSyncStatus = DesktopSyncStatus.DISABLED,
    val phone: String = "",
    val lastSuccessAt: Long = 0L,
    val busy: Boolean = false,
    val message: String = "",
    val pendingCount: Int = 0,
    val restoredGeneration: Long = 0L
)

class DesktopSyncCoordinator(
    private val directory: Path,
    val licenseStore: LicenseStore,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO),
    val database: DesktopSqliteDatabase? = null,
    private val onDataChanged: (() -> Unit)? = null,
    apiOverride: DesktopCloudRepository? = null
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
    val api: DesktopCloudRepository = apiOverride ?: DesktopHttpCloudRepository(directory, keys, licenseStore)
    private val mutex = Mutex()
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
            _state.update { it.copy(busy = false, message = errorMessage(e)) }
            Result.failure(e)
        }
    }

    suspend fun verifyOtp(challenge: JSONObject, code: String, phone: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            _state.update { it.copy(busy = true, message = "در حال اعتبارسنجی...") }
            mutex.withLock {
                val res = api.verifyCode(challenge, code, phone)
                keys.write(res)
                _state.update { it.copy(enabled = true, status = DesktopSyncStatus.PENDING, phone = phone, message = "ورود انجام شد؛ در حال بررسی حساب") }
                saveConfig()
            }
            val result = syncNow()
            if (_state.value.status == DesktopSyncStatus.RESTORE_REQUIRED) Result.success(Unit) else result
        } catch (e: CancellationException) { throw e
        } catch (e: Exception) {
            _state.update { it.copy(busy = false, status = DesktopSyncStatus.ERROR, message = errorMessage(e)) }
            Result.failure(e)
        } finally { _state.update { it.copy(busy = false) } }
    }

    suspend fun syncNow(): Result<Unit> = withContext(Dispatchers.IO) { mutex.withLock { syncLocked() } }

    private suspend fun syncLocked(): Result<Unit> {
        if (keys.read() == null) {
            _state.update { it.copy(status = DesktopSyncStatus.AUTH_REQUIRED) }
            return Result.failure(DesktopCloudException("AUTH_REQUIRED"))
        }
        val db = database
        if (db == null) {
            _state.update { it.copy(busy = false, status = DesktopSyncStatus.ERROR, message = "ذخیره‌ساز محلی در دسترس نیست") }
            return Result.failure(DesktopCloudException("LOCAL_STORAGE_UNAVAILABLE"))
        }
        return try {
            _state.update { it.copy(busy = true, status = DesktopSyncStatus.SYNCING, message = "در حال همگام‌سازی...") }
            val bootstrap = api.request("cloud/bootstrap", null)
            val workspaceId = bootstrap.getString("workspaceId")
            val writerEpoch = bootstrap.getLong("writerEpoch")
            val cloudInitialized = bootstrap.getBoolean("initialized")

            var cp = db.checkpoint()
            if (cp.workspaceId.isNotBlank() && cp.workspaceId != workspaceId) {
                throw DesktopCloudException("ACCOUNT_SWITCH_REQUIRES_BACKUP")
            }
            if (cp.initialized && cp.writerEpoch != writerEpoch) throw DesktopCloudException("RESET_REQUIRED")

            var changesApplied = false

            // Initial snapshot if local not yet initialized
            if (!cp.initialized) {
                if (cloudInitialized) {
                    throw DesktopCloudException("RESTORE_REQUIRED")
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
                    db.saveCheckpoint(db.checkpoint().copy(initialized = true, uploadSnapshotId = "", workspaceId = workspaceId, writerEpoch = writerEpoch))
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
        } catch (e: CancellationException) { throw e
        } catch (e: Exception) {
            val status = when ((e as? DesktopCloudException)?.code) {
                "AUTH_REQUIRED", "FRESH_OTP_REQUIRED" -> DesktopSyncStatus.AUTH_REQUIRED
                "RESTORE_REQUIRED", "RESET_REQUIRED" -> DesktopSyncStatus.RESTORE_REQUIRED
                else -> DesktopSyncStatus.ERROR
            }
            _state.update { it.copy(busy = false, status = status, message = errorMessage(e)) }
            Result.failure(e)
        } finally { _state.update { it.copy(busy = false) } }
    }

    /** Authentication alone never replaces local data. Only this confirmed action can activate a snapshot. */
    suspend fun restore(confirmReplacement: Boolean): Result<Unit> = withContext(Dispatchers.IO) { mutex.withLock {
        if (!confirmReplacement) return@withLock Result.failure(DesktopCloudException("RESTORE_CONFIRMATION_REQUIRED"))
        val db = database ?: return@withLock Result.failure(DesktopCloudException("LOCAL_STORAGE_UNAVAILABLE"))
        try {
            _state.update { it.copy(busy = true, status = DesktopSyncStatus.SYNCING, message = "در حال بازیابی حساب ابری...") }
            val info = api.request("cloud/bootstrap")
            val workspace = info.getString("workspaceId")
            val checkpoint = db.checkpoint()
            if (checkpoint.workspaceId.isNotBlank() && checkpoint.workspaceId != workspace) throw DesktopCloudException("ACCOUNT_SWITCH_REQUIRES_BACKUP")
            val cut = checkpoint.nextSequence
            db.backupBeforeCloudRestore(directory.resolve("cloud-backups").resolve("backup-${UUID.randomUUID()}.sqlite"))
            val start = api.request("cloud/snapshot/start", JSONObject().put("direction", "download").put("writerEpoch", info.getLong("writerEpoch")))
            val id = start.getString("snapshotId")
            for (page in 0 until start.getInt("pages")) {
                val result = api.request("cloud/snapshot/page", JSONObject().put("snapshotId", id).put("page", page))
                val records = result.getJSONArray("records")
                val hash = MessageDigest.getInstance("SHA-256").digest(SyncJson.canonical(records).toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
                if (hash != result.getString("hash")) throw DesktopCloudException("SNAPSHOT_HASH_MISMATCH")
                db.stageSnapshot(DesktopSyncStaging(id, page, records.toString()))
            }
            db.activateSnapshot(id, workspace, start.getLong("revision"), info.getLong("writerEpoch"), info.getLong("lastSequence"), cut)
            onDataChanged?.invoke()
            _state.update { it.copy(busy = false, status = DesktopSyncStatus.PENDING, restoredGeneration = it.restoredGeneration + 1, message = "اطلاعات حساب بازیابی شد") }
            // A later retry may fail independently; the confirmed snapshot is already safely activated.
            Result.success(Unit)
        } catch (e: CancellationException) { throw e
        } catch (e: Exception) {
            val status = if ((e as? DesktopCloudException)?.code in listOf("AUTH_REQUIRED", "FRESH_OTP_REQUIRED")) DesktopSyncStatus.AUTH_REQUIRED else DesktopSyncStatus.RESTORE_REQUIRED
            _state.update { it.copy(busy = false, status = status, message = errorMessage(e)) }
            Result.failure(e)
        } finally { _state.update { it.copy(busy = false) } }
    } }

    companion object {
        fun errorMessage(error: Exception): String = when ((error as? DesktopCloudException)?.code) {
            "AUTH_REQUIRED", "FRESH_OTP_REQUIRED" -> "برای اتصال به ابر دوباره وارد حساب شوید"
            "RESTORE_REQUIRED", "RESET_REQUIRED" -> "حساب قبلی پیدا شد؛ برای دریافت اطلاعات، بازیابی را تأیید کنید"
            "LICENSE_REQUIRED" -> "اتصال ابری به اشتراک یا دوره آزمایشی معتبر نیاز دارد"
            "OTP_INVALID" -> "کد ورود صحیح نیست"
            "OTP_EXPIRED" -> "کد ورود منقضی شده است؛ کد تازه درخواست کنید"
            "RATE_LIMITED" -> "کمی صبر کنید و دوباره تلاش کنید"
            "SNAPSHOT_HASH_MISMATCH" -> "بررسی صحت نسخه ابری موفق نبود؛ اطلاعات محلی محفوظ است"
            "LOCAL_CHANGED_DURING_RESTORE" -> "اطلاعات محلی هنگام بازیابی تغییر کرد؛ دوباره تأیید کنید"
            "ACCOUNT_SWITCH_REQUIRES_BACKUP" -> "اطلاعات این نصب به حساب دیگری متصل است؛ ابتدا پشتیبان و فضای جدا لازم است"
            "VERSION_CONFLICT" -> "نسخه‌ها اختلاف دارند؛ اطلاعات محلی و صف ارسال محفوظ است"
            "CLOUD_DISABLED", "SMS_UNAVAILABLE" -> "سرویس ورود ابری در دسترس نیست؛ کمی بعد تلاش کنید"
            else -> "ارتباط با سرویس ابری برقرار نشد؛ اطلاعات محلی و صف ارسال محفوظ است"
        }
    }

    suspend fun logout() = withContext(Dispatchers.IO) { mutex.withLock {
        try { api.logout() } catch (e: CancellationException) { throw e } catch (_: Exception) {} finally { keys.clear() }
        _state.update { it.copy(enabled = false, status = DesktopSyncStatus.DISABLED, phone = "", message = "", pendingCount = database?.outboxCount() ?: 0, restoredGeneration = 0) }
        saveConfig()
    } }

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
