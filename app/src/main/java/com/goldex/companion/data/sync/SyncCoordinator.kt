package com.goldex.companion.data.sync

import android.content.Context
import android.net.Uri
import android.util.Base64
import androidx.work.*
import com.goldex.companion.data.ConnectionStatus
import com.goldex.companion.data.NetworkMonitor
import com.goldex.companion.data.SettingsRepository
import com.goldex.companion.data.license.LicenseRepository
import com.goldex.companion.data.local.db.GoldexDatabaseProvider
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.*
import java.io.File
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

enum class SyncStatus { DISABLED, AUTH_REQUIRED, PENDING, SYNCING, SYNCED, OFFLINE, LICENSE_REQUIRED, CONFLICT, RESTORE_REQUIRED, WRITER_CHANGED, ERROR }
data class SyncUiState(val enabled: Boolean = false, val status: SyncStatus = SyncStatus.DISABLED,
    val pending: Int = 0, val lastSuccessAt: Long = 0, val phone: String = "", val message: String = "",
    val busy: Boolean = false, val restoredGeneration: Long = 0, val readOnly: Boolean = false)

class SyncCoordinator private constructor(private val context: Context) {
    private val preferences = context.getSharedPreferences("qirato_cloud_preferences",Context.MODE_PRIVATE)
    private val db = GoldexDatabaseProvider.getDatabase(context)
    private val local = SyncLocalStore(context,db)
    private val unit = GoldexDatabaseProvider.getSyncUnit(context)
    private val keys = DeviceCloudKeys(context)
    private val api = HttpCloudRepository(context,keys)
    private val scope=CoroutineScope(SupervisorJob()+Dispatchers.IO)
    private val mutex=Mutex()
    private val network=NetworkMonitor(context)
    private val triggers=MutableSharedFlow<Unit>(extraBufferCapacity=1)
    private val _state=MutableStateFlow(SyncUiState(lastSuccessAt=local.checkpoint().lastSuccessAt,enabled=preferences.getBoolean("enabled",false),readOnly=preferences.getBoolean("writerInvalid",false)))
    val state: StateFlow<SyncUiState> = _state.asStateFlow()
    private var onboarding = !SettingsRepository.getInstance(context).loadSettings().hasCompletedOnboarding
    init {
        preferences.edit().putBoolean("transferInProgress",false).apply()
        // Force business-settings import before the first snapshot.
        SettingsRepository.getInstance(context)
        unit.onCommit={ _state.update { if(it.enabled && it.status==SyncStatus.SYNCED) it.copy(status=SyncStatus.PENDING) else it }; requestSync() }
        unit.writeAllowed={ !preferences.getBoolean("writerInvalid",false) && !preferences.getBoolean("transferInProgress",false) }
        scope.launch { local.dao.observePending().collect { count ->
            _state.update { it.copy(pending=count, status=if(count>0 && it.status==SyncStatus.SYNCED) SyncStatus.PENDING else it.status) }
        } }
        scope.launch { network.status.collect { if(it==ConnectionStatus.OFFLINE && _state.value.enabled) _state.update { s -> s.copy(status=SyncStatus.OFFLINE) } else requestSync() } }
        scope.launch { @OptIn(FlowPreview::class) triggers.debounce(1000).collect { if(!onboarding) sync() } }
        setEnabled(_state.value.enabled)
    }
    fun deferForOnboarding(value: Boolean) { onboarding=value; if(!value) requestSync() }
    fun setEnabled(enabled: Boolean) {
        preferences.edit().putBoolean("enabled",enabled).apply()
        _state.update { it.copy(enabled=enabled,status=if(enabled) SyncStatus.PENDING else SyncStatus.DISABLED,message="") }
        val work=WorkManager.getInstance(context)
        if(enabled) {
            work.enqueueUniquePeriodicWork("qirato-cloud-periodic",ExistingPeriodicWorkPolicy.KEEP,
                PeriodicWorkRequestBuilder<CloudSyncWorker>(15,TimeUnit.MINUTES).setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()).build())
            requestSync()
        } else { work.cancelUniqueWork("qirato-cloud-pending"); work.cancelUniqueWork("qirato-cloud-periodic"); work.cancelUniqueWork("qirato-cloud-delayed") }
    }
    fun requestSync() {
        if(!_state.value.enabled || onboarding) return
        triggers.tryEmit(Unit)
    }
    fun reauthenticate() { _state.update { it.copy(status=SyncStatus.AUTH_REQUIRED) } }
    suspend fun requestCode(phone: String): JSONObject = withContext(Dispatchers.IO) { api.requestCode(phone) }
    suspend fun activateTrial() { val result=LicenseRepository(context).activateTrial(); if(result is com.goldex.companion.data.license.LicenseResult.Error) throw CloudException("LICENSE_REQUIRED",403) }
    suspend fun verifyCode(challenge: JSONObject, code: String, phone: String) = mutex.withLock {
        val session=api.verifyCode(challenge,code,phone)
        val cp=local.checkpoint()
        if(cp.workspaceId.isNotBlank() && cp.workspaceId != session.getString("workspaceId")) throw CloudException("ACCOUNT_SWITCH_REQUIRES_BACKUP")
        keys.write(session)
        _state.update { it.copy(phone=phone,status=SyncStatus.PENDING,message="ورود موفق بود") }
        val info=try { api.request("cloud/bootstrap") } catch(e: CloudException) {
            if(e.code=="LICENSE_REQUIRED") { _state.update { it.copy(status=SyncStatus.LICENSE_REQUIRED) }; return@withLock }
            throw e
        }
        val status=when {
            !info.getBoolean("isWriter") -> SyncStatus.WRITER_CHANGED
            !cp.initialized && info.getBoolean("initialized") -> SyncStatus.RESTORE_REQUIRED
            else -> SyncStatus.PENDING
        }
        if(status==SyncStatus.WRITER_CHANGED) preferences.edit().putBoolean("writerInvalid",true).apply()
        _state.update { it.copy(status=status,readOnly=preferences.getBoolean("writerInvalid",false)) }
        if(!onboarding) requestSync()
    }
    suspend fun logout() = mutex.withLock { setEnabled(false); api.logout(); _state.update { it.copy(phone="",status=SyncStatus.DISABLED) } }
    fun showError(e: Exception) { _state.update { it.copy(message=message((e as? CloudException)?.code ?: "SERVER_ERROR"),busy=false) } }
    suspend fun sync(): Boolean = mutex.withLock {
        if(!_state.value.enabled || onboarding) return@withLock true
        if(network.checkInitialStatus()==ConnectionStatus.OFFLINE) { _state.update { it.copy(status=SyncStatus.OFFLINE) }; return@withLock false }
        _state.update { it.copy(status=SyncStatus.SYNCING,busy=true,message="") }
        try {
            val session=keys.read() ?: throw CloudException("AUTH_REQUIRED",401)
            _state.update { it.copy(phone=session.optString("phone")) }
            val bootstrap=api.request("cloud/bootstrap")
            if(!bootstrap.getBoolean("isWriter")) throw CloudException("WRITER_CHANGED",409)
            var cp=local.checkpoint()
            if(cp.workspaceId.isNotEmpty() && cp.workspaceId!=bootstrap.getString("workspaceId")) throw CloudException("ACCOUNT_SWITCH_REQUIRES_BACKUP")
            if(cp.initialized && cp.writerEpoch!=bootstrap.getLong("writerEpoch")) throw CloudException("WRITER_CHANGED",409)
            if(!cp.initialized) {
                if(bootstrap.getBoolean("initialized") && cp.uploadSnapshotId.isBlank()) throw CloudException("RESTORE_REQUIRED",409)
                val snapshotId=cp.uploadSnapshotId.ifBlank {
                    val started=api.request("cloud/snapshot/start",JSONObject().put("direction","upload").put("writerEpoch",bootstrap.getLong("writerEpoch")))
                    started.getString("snapshotId").also { local.prepareUpload(it,bootstrap.getString("workspaceId"),bootstrap.getLong("writerEpoch")) }
                }
                val pages=local.dao.staged(snapshotId)
                for(page in pages) { ensureEnabled(); api.request("cloud/snapshot/page",JSONObject().put("snapshotId",snapshotId).put("page",page.page).put("records",JSONArray(page.payload))) }
                api.request("cloud/snapshot/commit",JSONObject().put("snapshotId",snapshotId).put("pages",pages.size))
                db.runInTransaction { local.dao.checkpoint(local.checkpoint().copy(initialized=true,uploadSnapshotId="")); local.dao.clearStage(snapshotId) }
                cp=local.checkpoint()
            }
            ensureAssets()
            while(_state.value.enabled) {
                val item=local.dao.first() ?: break
                if(item.status=="CONFLICT") throw CloudException("VERSION_CONFLICT",409)
                val body=JSONObject(item.payload); val changes=body.getJSONArray("changes")
                for(i in 0 until changes.length()) changes.getJSONObject(i).also { it.remove("localVersion"); it.remove("localPayload") }
                body.put("protocolVersion",1).put("operationId",item.operationId).put("deviceSequence",item.sequence).put("writerEpoch",cp.writerEpoch)
                try {
                    val serialized=body.toString()
                    val ack=if(changes.length()<=100 && serialized.toByteArray().size<=512*1024) api.request("cloud/sync/push",body) else {
                        if(serialized.toByteArray().size>5*1024*1024) throw CloudException("GROUP_TOO_LARGE",413)
                        val bytes=serialized.toByteArray(Charsets.UTF_8)
                        val chunks=(bytes.indices step 300000).map { start -> Base64.encodeToString(bytes.copyOfRange(start,minOf(start+300000,bytes.size)),Base64.NO_WRAP) }
                        chunks.forEachIndexed { i,chunk -> ensureEnabled(); api.request("cloud/sync/stage",JSONObject().put("operationId",item.operationId).put("writerEpoch",cp.writerEpoch).put("page",i).put("chunk",chunk)) }
                        api.request("cloud/sync/stage/commit",JSONObject().put("operationId",item.operationId).put("pages",chunks.size))
                    }
                    local.acknowledge(item,ack)
                } catch(e: CloudException) {
                    if(e.code=="VERSION_CONFLICT") db.runInTransaction {
                        local.dao.outbox(item.copy(status="CONFLICT")); local.dao.conflict(SyncConflict(item.operationId,item.payload,e.details?.toString() ?: "{}",e.code,System.currentTimeMillis()))
                    }
                    throw e
                }
            }
            ensureEnabled()
            var upper: Long?=null
            do {
                val body=JSONObject().put("workspaceId",cp.workspaceId).put("cursor",local.checkpoint().cursor)
                if(upper!=null) body.put("upperRevision",upper)
                val result=api.request("cloud/sync/pull",body); upper=result.getLong("upperRevision")
                local.applyPull(result)
                ensureEnabled()
            } while(result.getBoolean("hasMore"))
            downloadAssets()
            val now=System.currentTimeMillis(); local.dao.checkpoint(local.checkpoint().copy(lastSuccessAt=now))
            _state.update { it.copy(status=if(local.dao.pendingCount()==0) SyncStatus.SYNCED else SyncStatus.PENDING,busy=false,pending=local.dao.pendingCount(),lastSuccessAt=now,restoredGeneration=it.restoredGeneration+1) }
            true
        } catch(e: CancellationException) { throw e }
        catch(e: Exception) {
            if(!_state.value.enabled) { _state.update { it.copy(status=SyncStatus.DISABLED,busy=false) }; return@withLock true }
            val cloud=e as? CloudException
            val status=when(cloud?.code) {
                "AUTH_REQUIRED", "FRESH_OTP_REQUIRED" -> SyncStatus.AUTH_REQUIRED
                "LICENSE_REQUIRED" -> SyncStatus.LICENSE_REQUIRED
                "VERSION_CONFLICT" -> SyncStatus.CONFLICT
                "RESTORE_REQUIRED", "RESET_REQUIRED" -> SyncStatus.RESTORE_REQUIRED
                "WRITER_CHANGED" -> SyncStatus.WRITER_CHANGED
                else -> SyncStatus.ERROR
            }
            if(status==SyncStatus.WRITER_CHANGED) preferences.edit().putBoolean("writerInvalid",true).apply()
            _state.update { it.copy(status=status,readOnly=preferences.getBoolean("writerInvalid",false),busy=false,message=message(cloud?.code ?: "SERVER_ERROR")) }
            if(cloud?.code=="SNAPSHOT_EXPIRED" && !local.checkpoint().initialized) db.runInTransaction {
                val checkpoint=local.checkpoint(); local.dao.clearStage(checkpoint.uploadSnapshotId); local.dao.checkpoint(checkpoint.copy(uploadSnapshotId=""))
            }
            if(cloud?.status==429 && cloud.retryAfter>0) scheduleRetry(cloud.retryAfter)
            cloud!=null && cloud.status in 400..499 && cloud.status!=429
        }
    }
    private fun ensureEnabled() { if(!_state.value.enabled) throw CloudException("DISABLED") }
    private fun scheduleRetry(seconds: Long) { WorkManager.getInstance(context).enqueueUniqueWork("qirato-cloud-delayed",ExistingWorkPolicy.REPLACE,
        OneTimeWorkRequestBuilder<CloudSyncWorker>().setInitialDelay(seconds,TimeUnit.SECONDS).build()) }
    suspend fun restore(confirmReplacement: Boolean) = mutex.withLock {
        if(!confirmReplacement) throw CloudException("RESTORE_CONFIRMATION_REQUIRED")
        _state.update { it.copy(busy=true,status=SyncStatus.SYNCING) }
        try {
            val info=api.request("cloud/bootstrap")
            if(!info.getBoolean("isWriter")) throw CloudException("WRITER_CHANGED",409)
            val cut=local.checkpoint().nextSequence
            local.backup()
            val start=api.request("cloud/snapshot/start",JSONObject().put("direction","download").put("writerEpoch",info.getLong("writerEpoch")))
            val id=start.getString("snapshotId")
            for(page in 0 until start.getInt("pages")) {
                ensureEnabled()
                val result=api.request("cloud/snapshot/page",JSONObject().put("snapshotId",id).put("page",page))
                val records=result.getJSONArray("records")
                require(sha(SyncJson.canonical(records).toByteArray())==result.getString("hash"))
                local.dao.stage(SyncStaging(id,page,records.toString()))
            }
            local.activateSnapshot(id,info.getString("workspaceId"),start.getLong("revision"),info.getLong("writerEpoch"),info.getLong("lastSequence"),cut)
            preferences.edit().putBoolean("writerInvalid",false).apply()
            downloadAssets()
            _state.update { it.copy(busy=false,status=SyncStatus.PENDING,readOnly=false,restoredGeneration=it.restoredGeneration+1) }
            requestSync()
        } catch(e: Exception) { showError(e); throw e }
    }
    suspend fun takeover(acceptUnsyncedLoss: Boolean) = mutex.withLock {
        require(acceptUnsyncedLoss)
        val devices=api.request("cloud/devices")
        val message="transfer:${devices.getString("workspaceId")}:${devices.getLong("writerEpoch")}:${devices.getString("sessionId")}" 
        api.request("cloud/devices/takeover",JSONObject().put("expectedEpoch",devices.getLong("writerEpoch")).put("acceptUnsyncedLoss",true).put("signature",keys.sign(message)))
        _state.update { it.copy(status=SyncStatus.RESTORE_REQUIRED,message="انتقال انجام شد؛ نسخهٔ ابری را بازیابی کنید") }
    }
    suspend fun backupForExport(): File = mutex.withLock { local.backup() }
    suspend fun detachAccount(confirm: Boolean) = mutex.withLock {
        require(confirm)
        local.backup()
        setEnabled(false)
        runCatching { api.logout() }; keys.clear()
        db.runInTransaction {
            local.dao.clearOutbox(); local.dao.clearMetadata()
            local.dao.conflicts().forEach { local.dao.clearConflict(it.operationId) }
            local.dao.checkpoint(SyncCheckpoint())
            for(name in listOf("logo","stamp")) local.dao.asset(name)?.let { local.dao.asset(it.copy(remoteId="",sha256="")) }
            local.dao.business()?.let { val json=JSONObject(it.payload); json.remove("logoAssetId"); json.remove("stampAssetId"); local.dao.business(it.copy(payload=json.toString())) }
        }
        preferences.edit().putBoolean("writerInvalid",false).apply()
        _state.update { it.copy(phone="",status=SyncStatus.DISABLED,readOnly=false,message="پشتیبان ساخته شد؛ دادهٔ محلی از حساب جدا شد") }
    }
    suspend fun reviewConflict(): JSONObject = mutex.withLock {
        val item = local.dao.first() ?: throw CloudException("NO_CONFLICT")
        if(item.status != "CONFLICT") throw CloudException("NO_CONFLICT")
        val changes = JSONObject(item.payload).getJSONArray("changes")
        val records = JSONArray()
        for(i in 0 until changes.length()) records.put(JSONObject().put("type",changes.getJSONObject(i).getString("type")).put("id",changes.getJSONObject(i).getString("id")))
        val remote = api.request("cloud/conflicts/read",JSONObject().put("records",records))
        local.dao.conflict(SyncConflict(item.operationId,item.payload,remote.toString(),"VERSION_CONFLICT",System.currentTimeMillis()))
        JSONObject().put("operationId",item.operationId).put("local",changes).put("remote",remote.getJSONArray("records"))
    }
    suspend fun chooseLocalConflict(operationId: String) = mutex.withLock {
        local.backup()
        local.chooseLocalConflict(operationId)
        _state.update { it.copy(status=SyncStatus.PENDING,message="نسخهٔ محلی کل گروه انتخاب شد؛ در انتظار تأیید سرور") }
        requestSync()
    }
    suspend fun releaseWriter(targetDeviceId: String) {
        if(!sync()) throw CloudException("TRANSFER_NOT_READY")
        mutex.withLock {
            db.runInTransaction {
                if(local.dao.pendingCount()!=0 || _state.value.status!=SyncStatus.SYNCED) throw CloudException("TRANSFER_NOT_READY")
                preferences.edit().putBoolean("transferInProgress",true).commit()
            }
            try {
            val info=api.request("cloud/bootstrap")
            val message="release:${info.getString("workspaceId")}:${info.getLong("writerEpoch")}:${info.getLong("revision")}:$targetDeviceId"
            api.request("cloud/devices/release",JSONObject().put("revision",info.getLong("revision")).put("targetDeviceId",targetDeviceId).put("signature",keys.sign(message)))
            preferences.edit().putBoolean("writerInvalid",true).apply()
            _state.update { it.copy(status=SyncStatus.WRITER_CHANGED,readOnly=true,message="نویسندگی منتقل شد؛ اطلاعات محلی محفوظ است") }
            } finally { preferences.edit().putBoolean("transferInProgress",false).commit() }
        }
    }
    suspend fun devices(): JSONObject = mutex.withLock { api.request("cloud/devices") }
    private suspend fun ensureAssets() {
        for (name in listOf("logo", "stamp", "signature")) {
            val asset = local.dao.asset(name) ?: continue
            if (asset.localUri.isBlank() || asset.remoteId.isNotBlank()) continue
            val uri = Uri.parse(asset.localUri)
            val bytes = runCatching {
                if (asset.localUri.startsWith("file:") || asset.localUri.startsWith("/")) {
                    val path = uri.path ?: asset.localUri.removePrefix("file://").removePrefix("file:")
                    File(path).inputStream().use { it.readBytesLimited(5 * 1024 * 1024) }
                } else {
                    context.contentResolver.openInputStream(uri)?.use { it.readBytesLimited(5 * 1024 * 1024) }
                }
            }.getOrNull() ?: continue
            val mime = context.contentResolver.getType(uri) ?: if (bytes.take(4) == listOf(82.toByte(), 73.toByte(), 70.toByte(), 70.toByte())) "image/webp" else if (bytes.firstOrNull() == (-119).toByte()) "image/png" else "image/jpeg"
            val hash = sha(bytes)
            val result = api.request("cloud/assets/upload", JSONObject().put("writerEpoch", local.checkpoint().writerEpoch).put("mime", mime).put("sha256", hash).put("data", Base64.encodeToString(bytes, Base64.NO_WRAP)))
            unit.transaction {
                val latest = local.dao.asset(name)
                if (latest?.localUri != asset.localUri) return@transaction
                local.dao.asset(asset.copy(sha256 = hash, remoteId = result.getString("assetId")))
                val assetKey = when (name) {
                    "logo" -> "logoAssetId"
                    "stamp" -> "stampAssetId"
                    else -> "signatureAssetId"
                }
                val business = JSONObject(local.dao.business()!!.payload).put(assetKey, result.getString("assetId"))
                local.dao.business(BusinessSettings(payload = business.toString())); unit.changed("businessSettings", "business", business)
            }
        }
    }
    private suspend fun downloadAssets() {
        val business = local.dao.business()?.payload?.let(::JSONObject) ?: return
        for (name in listOf("logo", "stamp", "signature")) {
            val assetKey = when (name) {
                "logo" -> "logoAssetId"
                "stamp" -> "stampAssetId"
                else -> "signatureAssetId"
            }
            val remote = business.optString(assetKey)
            if (remote.isBlank()) continue
            val cached = local.dao.asset(name)
            val exists = if (cached?.localUri?.startsWith("file:") == true || cached?.localUri?.startsWith("/") == true) {
                val path = Uri.parse(cached.localUri).path ?: cached.localUri.removePrefix("file://").removePrefix("file:")
                File(path).exists()
            } else {
                runCatching { cached?.localUri?.let { context.contentResolver.openInputStream(Uri.parse(it))?.use { true } } == true }.getOrDefault(false)
            }
            if (cached != null && cached.remoteId == remote && cached.localUri.isNotBlank() && exists) continue
            val result = api.request("cloud/assets/download", JSONObject().put("assetId", remote))
            val bytes = Base64.decode(result.getString("data"), Base64.DEFAULT)
            require(bytes.size <= 5 * 1024 * 1024 && sha(bytes) == result.getString("sha256"))
            val file = File(context.filesDir, "cloud-assets/$remote"); file.parentFile!!.mkdirs(); file.writeBytes(bytes)
            local.dao.asset(AssetMetadata(name, Uri.fromFile(file).toString(), result.getString("sha256"), remote))
            val prefKey = when (name) {
                "logo" -> "key_invoice_logo_uri"
                "stamp" -> "key_invoice_stamp_uri"
                else -> "key_invoice_signature_uri"
            }
            context.getSharedPreferences("qirat_settings_prefs", Context.MODE_PRIVATE).edit().putString(prefKey, Uri.fromFile(file).toString()).apply()
        }
        SettingsRepository.getInstance(context).loadSettings()
    }
    private fun java.io.InputStream.readBytesLimited(limit: Int): ByteArray {
        val out=java.io.ByteArrayOutputStream(); val b=ByteArray(8192)
        while(true) { val n=read(b); if(n<0) break; if(out.size()+n>limit) throw CloudException("ASSET_TOO_LARGE"); out.write(b,0,n) }; return out.toByteArray()
    }
    companion object {
        @Volatile private var instance: SyncCoordinator?=null
        fun get(context: Context): SyncCoordinator = instance ?: synchronized(this) { instance ?: SyncCoordinator(context.applicationContext).also { instance=it } }
        fun sha(bytes: ByteArray)=MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
        fun message(code: String)=when(code) {
            "AUTH_REQUIRED" -> "برای اتصال به ابر وارد حساب شوید"
            "LICENSE_REQUIRED" -> "همگام‌سازی به مجوز معتبر نیاز دارد"
            "OTP_INVALID" -> "کد ورود صحیح نیست"
            "OTP_EXPIRED" -> "کد ورود منقضی شده است"
            "RATE_LIMITED" -> "کمی صبر کنید و دوباره تلاش کنید"
            "SMS_UNAVAILABLE", "CLOUD_DISABLED" -> "سرویس ابری هنوز فعال نیست؛ اطلاعات محلی محفوظ است"
            "VERSION_CONFLICT" -> "نسخه‌ها اختلاف دارند؛ تغییرات محلی محفوظ است"
            "RESTORE_REQUIRED", "RESET_REQUIRED" -> "بازیابی نسخهٔ ابری با تأیید شما لازم است"
            "WRITER_CHANGED" -> "دستگاه دیگری نویسندهٔ این حساب است"
            "ACCOUNT_SWITCH_REQUIRES_BACKUP" -> "دادهٔ این نصب به حساب دیگری متصل است؛ ابتدا پشتیبان و فضای جدا لازم است"
            "LOCAL_CHANGED_DURING_RESTORE" -> "هنگام بازیابی دادهٔ محلی تغییر کرد؛ دوباره تأیید کنید"
            "GROUP_TOO_LARGE" -> "حجم عملیات بیشتر از حد مجاز است؛ عملیات در صف حفظ شد"
            else -> "همگام‌سازی انجام نشد؛ اطلاعات و صف ارسال محفوظ است"
        }
    }
}
class CloudSyncWorker(context: Context, parameters: WorkerParameters) : CoroutineWorker(context,parameters) {
    override suspend fun doWork(): Result {
        val coordinator = SyncCoordinator.get(applicationContext)
        if (!coordinator.state.value.enabled) return Result.success()
        if (coordinator.state.value.pending == 0 &&
            System.currentTimeMillis() - coordinator.state.value.lastSuccessAt < 15000) {
            return Result.success()
        }
        if(runAttemptCount>0) delay(kotlin.random.Random.nextLong(0,3000))
        return if(coordinator.sync()) Result.success() else Result.retry()
    }
}
