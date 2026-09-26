package com.goldex.companion.data.local.db

import android.content.Context
import com.goldex.companion.data.CustomerStore
import com.goldex.companion.data.InventoryStore
import com.goldex.companion.data.InvoiceStore
import com.goldex.companion.data.PortfolioStore
import com.goldex.companion.data.local.db.migration.DataMigrationManager
import com.goldex.companion.data.local.repository.RoomCustomerRepository
import com.goldex.companion.data.local.repository.RoomInventoryRepository
import com.goldex.companion.data.local.repository.RoomInvoiceRepository
import com.goldex.companion.data.local.repository.RoomPortfolioRepository

object GoldexDatabaseProvider {

    @Volatile
    private var database: GoldexDatabase? = null

    @Volatile
    private var customerRepository: RoomCustomerRepository? = null

    @Volatile
    private var invoiceRepository: RoomInvoiceRepository? = null

    @Volatile
    private var inventoryRepository: RoomInventoryRepository? = null

    @Volatile
    private var portfolioRepository: RoomPortfolioRepository? = null

    fun getDatabase(context: Context): GoldexDatabase {
        return database ?: synchronized(this) {
            val db = GoldexDatabase.getInstance(context)
            DataMigrationManager.migrateIfNeeded(context, db)
            database = db
            db
        }
    }

    fun getCustomerStore(context: Context): CustomerStore {
        return customerRepository ?: synchronized(this) {
            val db = getDatabase(context)
            val repo = RoomCustomerRepository(db.customerDao(), db.ledgerTransactionDao())
            customerRepository = repo
            repo
        }
    }

    fun getInvoiceStore(context: Context): InvoiceStore {
        return invoiceRepository ?: synchronized(this) {
            val db = getDatabase(context)
            val repo = RoomInvoiceRepository(db.barterInvoiceDao(), db.classicInvoiceDao())
            invoiceRepository = repo
            repo
        }
    }

    fun getInventoryStore(context: Context): InventoryStore {
        return inventoryRepository ?: synchronized(this) {
            val db = getDatabase(context)
            val repo = RoomInventoryRepository(db.inventoryDao())
            inventoryRepository = repo
            repo
        }
    }

    fun getPortfolioStore(context: Context): PortfolioStore {
        return portfolioRepository ?: synchronized(this) {
            val db = getDatabase(context)
            val repo = RoomPortfolioRepository(db.portfolioDao())
            portfolioRepository = repo
            repo
        }
    }
}
