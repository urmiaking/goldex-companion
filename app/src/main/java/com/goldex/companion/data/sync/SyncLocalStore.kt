package com.goldex.companion.data.sync

import android.content.Context
import com.goldex.companion.data.PersistenceJsonCodecs as Codecs
import com.goldex.companion.data.local.db.GoldexDatabase
import com.goldex.companion.data.local.db.mappers.*
import org.json.*
import java.io.File
import java.util.UUID

class SyncLocalStore(private val context: Context, val db: GoldexDatabase) {
    val dao = db.syncDao()
    fun checkpoint() = dao.checkpoint() ?: SyncCheckpoint()
    /** Full enumeration is restricted to bootstrap/backup. Normal sync uses metadata by record ID. */
    fun currentRecords(): List<JSONObject> {
        val result = mutableListOf<JSONObject>()
        fun add(type: String, values: List<Any>) { values.forEach { value -> val payload=SyncJson.record(value); result.add(JSONObject().put("type",type).put("id",payload.getString("id")).put("payload",payload).put("version",1).put("deleted",false)) } }
        add("customer",db.customerDao().queryAll().map { it.toDomain() })
        add("ledger",db.ledgerTransactionDao().queryAll().map { it.toDomain() })
        add("invoice",db.classicInvoiceDao().queryAll().map { it.toDomain() })
        add("barterInvoice",db.barterInvoiceDao().queryAll().map { it.toDomain() })
        add("inventory",db.inventoryDao().queryAllItems().map { it.toDomain() })
        add("stockAdjustment",db.inventoryDao().queryAllAdjustments().map { it.toDomain() })
        add("portfolio",db.portfolioDao().queryAll().map { it.toDomain() })
        dao.business()?.let { result.add(JSONObject().put("type","businessSettings").put("id","business").put("payload",JSONObject(it.payload)).put("version",1).put("deleted",false)) }
        return result
    }
    fun prepareUpload(snapshotId: String, workspace: String, epoch: Long): List<SyncStaging> {
        db.runInTransaction {
            val cp = checkpoint()
            require(cp.workspaceId.isBlank() || cp.workspaceId == workspace)
            val records = currentRecords()
            dao.clearMetadata(); dao.clearOutbox()
            records.forEach { r -> dao.metadata(SyncMetadata(r.getString("type"),r.getString("id"),workspace,1,1,r.getJSONObject("payload").toString(),false)) }
            records.chunked(100).forEachIndexed { page, list -> dao.stage(SyncStaging(snapshotId,page,JSONArray(list).toString())) }
            // Even an empty dataset has one explicit page.
            if (records.isEmpty()) dao.stage(SyncStaging(snapshotId,0,"[]"))
            dao.checkpoint(cp.copy(workspaceId=workspace,writerEpoch=epoch,nextSequence=1,uploadSnapshotId=snapshotId))
        }
        return dao.staged(snapshotId)
    }
    fun backup(): File {
        val file = File(context.filesDir,"cloud-backups/backup-${UUID.randomUUID()}.json")
        file.parentFile!!.mkdirs()
        db.runInTransaction {
            file.writeText(JSONObject().put("records",JSONArray(currentRecords())).put("metadata",JSONArray(dao.allMetadata().map { JSONObject().put("type",it.type).put("id",it.id).put("version",it.version).put("localVersion",it.localVersion).put("deleted",it.deleted).put("payload",it.payload) }))
                .put("conflicts",JSONArray(dao.conflicts().map { JSONObject().put("operationId",it.operationId).put("local",it.localPayload).put("remote",it.remotePayload) }))
                .put("assets",JSONArray(listOfNotNull(dao.asset("logo"),dao.asset("stamp")).map { JSONObject().put("id",it.id).put("localUri",it.localUri).put("sha256",it.sha256).put("remoteId",it.remoteId) }))
                .put("outbox",JSONArray(dao.allOutbox().map { JSONObject().put("operationId",it.operationId).put("sequence",it.sequence).put("payload",JSONObject(it.payload)) })).toString())
        }
        return file
    }
    private fun decode(type: String, payload: JSONObject): Any {
        require(payload.getString("id").isNotBlank())
        val encoded=JSONArray().put(payload).toString()
        val result: Any = when(type) {
            "customer" -> Codecs.decodeCustomers(encoded).single()
            "ledger" -> Codecs.decodeLedgerTransactions(encoded).single()
            "invoice" -> Codecs.decodeInvoices(encoded).single()
            "barterInvoice" -> Codecs.decodeBarterInvoices(encoded).single()
            "inventory" -> Codecs.decodeInventoryItems(encoded).single()
            "stockAdjustment" -> Codecs.decodeStockAdjustments(encoded).single()
            "portfolio" -> Codecs.decodePortfolioItems(encoded).single()
            "businessSettings" -> { SyncJson.applyBusiness(com.goldex.companion.data.AppSettings(),payload); return payload }
            else -> throw CloudException("UNSUPPORTED_RECORD")
        }
        // A tolerant legacy decoder must not silently default malformed cloud fields.
        val again = SyncJson.record(result)
        again.keys().forEach { key -> if (payload.has(key) && SyncJson.canonical(payload.get(key)) != SyncJson.canonical(again.get(key))) throw CloudException("INVALID_RECORD") }
        return result
    }
    fun applyRecord(type: String, id: String, payload: JSONObject?) {
        if (payload == null) {
            when(type) {
                "customer" -> db.customerDao().deleteByIdSync(id)
                "ledger" -> db.ledgerTransactionDao().deleteByIdSync(id)
                "invoice" -> db.classicInvoiceDao().deleteByIdSync(id)
                "barterInvoice" -> db.barterInvoiceDao().deleteByIdSync(id)
                "inventory" -> db.inventoryDao().deleteItemByIdSync(id)
                "stockAdjustment" -> db.openHelper.writableDatabase.execSQL("DELETE FROM stock_adjustments WHERE id=?",arrayOf(id))
                "portfolio" -> db.portfolioDao().deleteByIdSync(id)
                else -> throw CloudException("INVALID_RECORD")
            }; return
        }
        require(payload.getString("id") == id)
        when(val record=decode(type,payload)) {
            is com.goldex.companion.model.Customer -> db.customerDao().insertSync(record.toEntity())
            is com.goldex.companion.model.LedgerTransaction -> db.ledgerTransactionDao().insertSync(record.toEntity())
            is com.goldex.companion.model.Invoice -> db.classicInvoiceDao().insertSync(record.toEntity())
            is com.goldex.companion.model.BarterInvoice -> db.barterInvoiceDao().insertSync(record.toEntity())
            is com.goldex.companion.model.InventoryItem -> db.inventoryDao().insertItemSync(record.toEntity())
            is com.goldex.companion.model.StockAdjustment -> db.inventoryDao().insertAdjustmentSync(record.toEntity())
            is com.goldex.companion.data.PortfolioItem -> db.portfolioDao().insertSync(record.toEntity())
            is JSONObject -> dao.business(BusinessSettings(payload=record.toString()))
        }
    }
    fun activateSnapshot(snapshotId: String, workspace: String, revision: Long, epoch: Long, sequence: Long, expectedNextSequence: Long) {
        db.runInTransaction {
            // A local edit during download invalidates the user's replacement confirmation.
            if (checkpoint().nextSequence != expectedNextSequence) throw CloudException("LOCAL_CHANGED_DURING_RESTORE")
            val records=dao.staged(snapshotId).flatMap { p -> val a=JSONArray(p.payload); (0 until a.length()).map { a.getJSONObject(it) } }
            require(records.map { it.getString("type")+":"+it.getString("id") }.distinct().size == records.size)
            records.filterNot { it.getBoolean("deleted") }.forEach { decode(it.getString("type"),it.getJSONObject("payload")) }
            listOf("customers","ledger_transactions","classic_invoices","barter_invoices","inventory_items","stock_adjustments","portfolio_items","business_settings").forEach { db.openHelper.writableDatabase.execSQL("DELETE FROM $it") }
            dao.clearMetadata(); dao.clearOutbox()
            dao.conflicts().forEach { dao.clearConflict(it.operationId) }
            dao.asset(AssetMetadata("logo","")); dao.asset(AssetMetadata("stamp",""))
            records.forEach { r ->
                val type=r.getString("type"); val id=r.getString("id"); val deleted=r.getBoolean("deleted"); val version=r.getLong("version")
                val payload=if(deleted) null else r.getJSONObject("payload")
                if(!deleted) applyRecord(type,id,payload)
                dao.metadata(SyncMetadata(type,id,workspace,version,version,payload?.toString(),deleted))
            }
            if(dao.business()==null) dao.business(BusinessSettings(payload=SyncJson.business(com.goldex.companion.data.AppSettings()).toString()))
            dao.checkpoint(SyncCheckpoint(workspaceId=workspace,cursor=revision,writerEpoch=epoch,nextSequence=sequence+1,initialized=true))
            dao.checkpoint(SyncCheckpoint(id="onboarding-complete"))
            dao.clearStage(snapshotId)
        }
    }
    /** User chooses the entire local group; later operations retain their order. */
    fun chooseLocalConflict(operationId: String) {
        db.runInTransaction {
            val first=dao.first() ?: throw CloudException("NO_CONFLICT")
            require(first.operationId==operationId && first.status=="CONFLICT")
            val review=dao.conflicts().firstOrNull { it.operationId==operationId } ?: throw CloudException("REVIEW_REQUIRED")
            val remote=JSONObject(review.remotePayload).getJSONArray("records")
            val bases=mutableMapOf<String,Long>(); val payloads=mutableMapOf<String,JSONObject?>()
            for(i in 0 until remote.length()) {
                val r=remote.getJSONObject(i); val key=r.getString("type")+":"+r.getString("id")
                if(r.getBoolean("deleted")) throw CloudException("TOMBSTONE_CONFLICT")
                bases[key]=r.getLong("version"); payloads[key]=r.optJSONObject("payload")
            }
            dao.allOutbox().forEach { item ->
                val original=JSONObject(item.payload).getJSONArray("changes"); val rebased=JSONArray(); var changed=false
                for(i in 0 until original.length()) {
                    val c=original.getJSONObject(i); val key=c.getString("type")+":"+c.getString("id")
                    if(key in bases) {
                        if(!c.has("localPayload")) throw CloudException("REVIEW_REQUIRED")
                        val wanted=c.optJSONObject("localPayload")
                        val patch=SyncJson.diff(payloads[key],wanted)
                        val version=bases.getValue(key)+1
                        patch.put("type",c.getString("type")).put("id",c.getString("id")).put("baseVersion",version-1).put("localVersion",version).put("localPayload",wanted ?: JSONObject.NULL)
                        rebased.put(patch); bases[key]=version; payloads[key]=wanted; changed=true
                    } else rebased.put(c)
                }
                if(changed) {
                    dao.acknowledge(item.operationId)
                    dao.enqueue(item.copy(operationId=UUID.randomUUID().toString(),payload=JSONObject().put("changes",rebased).toString(),status="PENDING"))
                }
            }
            for(i in 0 until remote.length()) {
                val r=remote.getJSONObject(i); val type=r.getString("type"); val id=r.getString("id"); val key="$type:$id"
                val old=dao.metadata(type,id) ?: throw CloudException("INVALID_RECORD")
                dao.metadata(old.copy(version=r.getLong("version"),localVersion=bases.getValue(key)))
            }
            dao.clearConflict(operationId)
        }
    }
    fun acknowledge(item: SyncOutbox, result: JSONObject) {
        db.runInTransaction {
            require(result.getString("operationId") == item.operationId)
            val sent=JSONObject(item.payload).getJSONArray("changes")
            val ack=result.getJSONArray("changes")
            require(ack.length()==sent.length())
            for(i in 0 until sent.length()) {
                val c=sent.getJSONObject(i); val accepted=ack.getJSONObject(i)
                require(c.getString("type")==accepted.getString("type") && c.getString("id")==accepted.getString("id"))
                require(c.getLong("localVersion")==accepted.getLong("version"))
                val old=dao.metadata(c.getString("type"),c.getString("id")) ?: error("Missing metadata")
                dao.metadata(old.copy(version=accepted.getLong("version")))
            }
            dao.acknowledge(item.operationId)
        }
    }
    fun applyPull(result: JSONObject) {
        db.runInTransaction {
            val groups=result.getJSONArray("groups")
            for(g in 0 until groups.length()) {
                val changes=groups.getJSONObject(g).getJSONArray("changes")
                for(i in 0 until changes.length()) {
                    val c=changes.getJSONObject(i); val type=c.getString("type"); val id=c.getString("id"); val version=c.getLong("version")
                    val old=dao.metadata(type,id)
                    if(old != null && version <= old.version) continue // Own ACKed echo; do not overwrite newer local edits.
                    if(old != null && old.localVersion > old.version) throw CloudException("VERSION_CONFLICT",409,c)
                    if((old?.version ?: 0) != c.getLong("baseVersion")) throw CloudException("RESET_REQUIRED",409)
                    val fresh=SyncJson.apply(old?.payload?.let(::JSONObject),c)
                    applyRecord(type,id,fresh)
                    dao.metadata(SyncMetadata(type,id,checkpoint().workspaceId,version,version,fresh?.toString(),fresh==null))
                }
            }
            dao.checkpoint(checkpoint().copy(cursor=result.getLong("cursor")))
        }
    }
}
