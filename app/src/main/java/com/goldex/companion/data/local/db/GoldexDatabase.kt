package com.goldex.companion.data.local.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.goldex.companion.data.local.db.converters.DatabaseConverters
import com.goldex.companion.data.local.db.dao.BarterInvoiceDao
import com.goldex.companion.data.local.db.dao.ClassicInvoiceDao
import com.goldex.companion.data.local.db.dao.CustomerDao
import com.goldex.companion.data.local.db.dao.InventoryDao
import com.goldex.companion.data.local.db.dao.LedgerTransactionDao
import com.goldex.companion.data.local.db.dao.PortfolioDao
import com.goldex.companion.data.local.db.entities.BarterInvoiceEntity
import com.goldex.companion.data.local.db.entities.ClassicInvoiceEntity
import com.goldex.companion.data.local.db.entities.CustomerEntity
import com.goldex.companion.data.local.db.entities.InventoryItemEntity
import com.goldex.companion.data.local.db.entities.LedgerTransactionEntity
import com.goldex.companion.data.local.db.entities.PortfolioItemEntity
import com.goldex.companion.data.local.db.entities.StockAdjustmentEntity
import com.goldex.companion.data.sync.*

@Database(
    entities = [
        CustomerEntity::class,
        LedgerTransactionEntity::class,
        BarterInvoiceEntity::class,
        ClassicInvoiceEntity::class,
        InventoryItemEntity::class,
        StockAdjustmentEntity::class,
        PortfolioItemEntity::class,
        SyncMetadata::class, SyncOutbox::class, SyncCheckpoint::class, SyncConflict::class,
        BusinessSettings::class, AssetMetadata::class, SyncStaging::class
    ],
    version = 3,
    exportSchema = true
)
@TypeConverters(DatabaseConverters::class)
abstract class GoldexDatabase : RoomDatabase() {
    abstract fun syncDao(): SyncDao

    abstract fun customerDao(): CustomerDao
    abstract fun ledgerTransactionDao(): LedgerTransactionDao
    abstract fun barterInvoiceDao(): BarterInvoiceDao
    abstract fun classicInvoiceDao(): ClassicInvoiceDao
    abstract fun inventoryDao(): InventoryDao
    abstract fun portfolioDao(): PortfolioDao

    companion object {
        private const val DATABASE_NAME = "goldex_production.db"

        @Volatile
        private var INSTANCE: GoldexDatabase? = null

        fun getInstance(context: Context): GoldexDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    GoldexDatabase::class.java,
                    DATABASE_NAME
                )
                    .allowMainThreadQueries()
                    .addMigrations(SYNC_MIGRATION_1_2, com.goldex.companion.data.local.db.migration.SETTLEMENT_MIGRATION_2_3)
                    .build()
                INSTANCE = instance
                instance
            }
        }

        fun buildInMemory(context: Context): GoldexDatabase {
            return Room.inMemoryDatabaseBuilder(
                context.applicationContext,
                GoldexDatabase::class.java
            )
                .allowMainThreadQueries()
                .build()
        }
    }
}
