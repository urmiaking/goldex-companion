package com.goldex.companion.data.local.db.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** Additive migration; historical coin subtype/price are unknown, never guessed. */
val COIN_INVENTORY_MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE inventory_items ADD COLUMN coinType TEXT")
        db.execSQL("ALTER TABLE inventory_items ADD COLUMN coinUnitPrice INTEGER")
        db.execSQL("ALTER TABLE inventory_items ADD COLUMN coinHasHologram INTEGER")
    }
}
