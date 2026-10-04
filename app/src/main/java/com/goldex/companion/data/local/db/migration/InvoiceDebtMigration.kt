package com.goldex.companion.data.local.db.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** Empty means the original contract. No invoice, balance or outbox is recalculated. */
val INVOICE_DEBT_MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE barter_invoices ADD COLUMN debtBasis TEXT NOT NULL DEFAULT ''")
    }
}
