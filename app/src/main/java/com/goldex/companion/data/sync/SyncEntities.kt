package com.goldex.companion.data.sync

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "sync_metadata", primaryKeys = ["type", "id"])
data class SyncMetadata(val type: String, val id: String, val workspaceId: String = "", val version: Long = 0,
    val localVersion: Long = 0, val payload: String? = null, val deleted: Boolean = false)
@Entity(tableName = "sync_outbox", indices = [Index(value = ["sequence"], unique = true)])
data class SyncOutbox(@PrimaryKey val operationId: String, val sequence: Long, val workspaceId: String = "",
    val payload: String, val status: String = "PENDING")
@Entity(tableName = "sync_checkpoint")
data class SyncCheckpoint(@PrimaryKey val id: String = "active", val workspaceId: String = "", val cursor: Long = 0,
    val writerEpoch: Long = 1, val nextSequence: Long = 1, val initialized: Boolean = false, val lastSuccessAt: Long = 0,
    val uploadSnapshotId: String = "")
@Entity(tableName = "sync_conflicts")
data class SyncConflict(@PrimaryKey val operationId: String, val localPayload: String, val remotePayload: String,
    val code: String, val createdAt: Long)
@Entity(tableName = "business_settings")
data class BusinessSettings(@PrimaryKey val id: String = "business", val payload: String)
@Entity(tableName = "asset_metadata")
data class AssetMetadata(@PrimaryKey val id: String, val localUri: String, val sha256: String = "", val remoteId: String = "")
@Entity(tableName = "sync_staging", primaryKeys = ["snapshotId", "page"])
data class SyncStaging(val snapshotId: String, val page: Int, val payload: String)

@Dao
interface SyncDao {
    @Query("SELECT * FROM sync_metadata WHERE type=:type AND id=:id") fun metadata(type: String, id: String): SyncMetadata?
    @Query("SELECT * FROM sync_metadata") fun allMetadata(): List<SyncMetadata>
    @Upsert fun metadata(value: SyncMetadata)
    @Insert fun enqueue(value: SyncOutbox)
    @Upsert fun outbox(value: SyncOutbox)
    @Query("SELECT * FROM sync_outbox ORDER BY sequence LIMIT 1") fun first(): SyncOutbox?
    @Query("SELECT * FROM sync_outbox ORDER BY sequence") fun allOutbox(): List<SyncOutbox>
    @Query("SELECT COUNT(*) FROM sync_outbox") fun pendingCount(): Int
    @Query("SELECT COUNT(*) FROM sync_outbox") fun observePending(): Flow<Int>
    @Query("DELETE FROM sync_outbox WHERE operationId=:id") fun acknowledge(id: String)
    @Query("SELECT * FROM sync_checkpoint WHERE id='active'") fun checkpoint(): SyncCheckpoint?
    @Query("SELECT * FROM sync_checkpoint WHERE id=:id") fun marker(id: String): SyncCheckpoint?
    @Upsert fun checkpoint(value: SyncCheckpoint)
    @Query("SELECT * FROM business_settings WHERE id='business'") fun business(): BusinessSettings?
    @Upsert fun business(value: BusinessSettings)
    @Query("SELECT * FROM business_settings WHERE id='business'") fun observeBusiness(): Flow<BusinessSettings?>
    @Upsert fun conflict(value: SyncConflict)
    @Query("SELECT * FROM sync_conflicts") fun conflicts(): List<SyncConflict>
    @Query("DELETE FROM sync_conflicts WHERE operationId=:id") fun clearConflict(id: String)
    @Upsert fun asset(value: AssetMetadata)
    @Query("SELECT * FROM asset_metadata WHERE id=:id") fun asset(id: String): AssetMetadata?
    @Upsert fun stage(value: SyncStaging)
    @Query("SELECT * FROM sync_staging WHERE snapshotId=:id ORDER BY page") fun staged(id: String): List<SyncStaging>
    @Query("DELETE FROM sync_staging WHERE snapshotId=:id") fun clearStage(id: String)
    @Query("DELETE FROM sync_metadata") fun clearMetadata()
    @Query("DELETE FROM sync_outbox") fun clearOutbox()
}
