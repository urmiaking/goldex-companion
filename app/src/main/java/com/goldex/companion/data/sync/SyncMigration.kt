package com.goldex.companion.data.sync

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val SYNC_MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS sync_metadata (type TEXT NOT NULL, id TEXT NOT NULL, workspaceId TEXT NOT NULL, version INTEGER NOT NULL, localVersion INTEGER NOT NULL, payload TEXT, deleted INTEGER NOT NULL, PRIMARY KEY(type,id))")
        db.execSQL("CREATE TABLE IF NOT EXISTS sync_outbox (operationId TEXT NOT NULL PRIMARY KEY, sequence INTEGER NOT NULL, workspaceId TEXT NOT NULL, payload TEXT NOT NULL, status TEXT NOT NULL)")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_sync_outbox_sequence ON sync_outbox(sequence)")
        db.execSQL("CREATE TABLE IF NOT EXISTS sync_checkpoint (id TEXT NOT NULL PRIMARY KEY, workspaceId TEXT NOT NULL, cursor INTEGER NOT NULL, writerEpoch INTEGER NOT NULL, nextSequence INTEGER NOT NULL, initialized INTEGER NOT NULL, lastSuccessAt INTEGER NOT NULL, uploadSnapshotId TEXT NOT NULL)")
        db.execSQL("CREATE TABLE IF NOT EXISTS sync_conflicts (operationId TEXT NOT NULL PRIMARY KEY, localPayload TEXT NOT NULL, remotePayload TEXT NOT NULL, code TEXT NOT NULL, createdAt INTEGER NOT NULL)")
        db.execSQL("CREATE TABLE IF NOT EXISTS business_settings (id TEXT NOT NULL PRIMARY KEY, payload TEXT NOT NULL)")
        db.execSQL("CREATE TABLE IF NOT EXISTS asset_metadata (id TEXT NOT NULL PRIMARY KEY, localUri TEXT NOT NULL, sha256 TEXT NOT NULL, remoteId TEXT NOT NULL)")
        db.execSQL("CREATE TABLE IF NOT EXISTS sync_staging (snapshotId TEXT NOT NULL, page INTEGER NOT NULL, payload TEXT NOT NULL, PRIMARY KEY(snapshotId,page))")
    }
}
