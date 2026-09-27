package com.goldex.companion.data.sync

import com.goldex.companion.data.local.db.GoldexDatabase
import org.json.*
import java.util.UUID

interface SyncUnitOfWork { fun <T> transaction(action: () -> T): T }

/** Nested repository writes share one financial group and one SQLite transaction. */
class RoomSyncUnitOfWork(private val db: GoldexDatabase) : SyncUnitOfWork {
    private data class Pending(val type: String, val id: String, val payload: JSONObject?)
    private val current = ThreadLocal<LinkedHashMap<String, Pending>?>()
    var onCommit: (() -> Unit)? = null
    var writeAllowed: () -> Boolean = { true }
    override fun <T> transaction(action: () -> T): T {
        if (current.get() != null) return action()
        check(writeAllowed()) { "Cloud writer transferred; detach or restore before editing" }
        var value: T? = null
        try {
            db.runInTransaction {
                check(writeAllowed()) { "Cloud writer transferred; detach or restore before editing" }
                current.set(linkedMapOf())
                value = action()
                flush(current.get()!!)
            }
        } finally { current.remove() }
        runCatching { onCommit?.invoke() }
        @Suppress("UNCHECKED_CAST") return value as T
    }
    fun changed(type: String, id: String, payload: JSONObject?) {
        val group = current.get() ?: error("Mutation outside transaction")
        group["$type:$id"] = Pending(type, id, payload)
    }
    private fun flush(group: Map<String, Pending>) {
        val dao = db.syncDao(); val checkpoint = dao.checkpoint() ?: SyncCheckpoint()
        val changes = JSONArray()
        group.values.forEach { p ->
            val old = dao.metadata(p.type, p.id)
            if (old?.deleted == true && p.payload != null) error("Deleted record ID cannot be reused")
            val fresh = SyncJson.preserveUnknown(p.type,old?.payload?.let(::JSONObject),p.payload)
            if ((old?.payload != null && fresh != null && SyncJson.canonical(JSONObject(old.payload)) == SyncJson.canonical(fresh)) || (old?.payload == null && fresh == null)) return@forEach
            val delta = SyncJson.diff(old?.payload?.let(::JSONObject), fresh)
            val version = (old?.localVersion ?: 0) + 1
            delta.put("type", p.type).put("id", p.id).put("baseVersion", version - 1).put("localVersion", version).put("localPayload", fresh ?: JSONObject.NULL)
            changes.put(delta)
            dao.metadata(SyncMetadata(p.type, p.id, checkpoint.workspaceId, old?.version ?: 0, version, fresh?.toString(), fresh == null))
        }
        if (changes.length() == 0) return
        val operationId = UUID.randomUUID().toString()
        val payload = JSONObject().put("changes", changes).toString()
        dao.enqueue(SyncOutbox(operationId, checkpoint.nextSequence, checkpoint.workspaceId, payload))
        dao.checkpoint(checkpoint.copy(nextSequence = checkpoint.nextSequence + 1))
    }
}
