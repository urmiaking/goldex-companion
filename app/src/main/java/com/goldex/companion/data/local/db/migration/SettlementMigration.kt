package com.goldex.companion.data.local.db.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** Additive only. Old entries retain the empty default and their original accounting effect. */
val SETTLEMENT_MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE ledger_transactions ADD COLUMN settlementJson TEXT NOT NULL DEFAULT ''")
    }
}
