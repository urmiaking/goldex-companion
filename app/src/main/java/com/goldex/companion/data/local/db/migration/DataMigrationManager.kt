package com.goldex.companion.data.local.db.migration

import android.content.Context
import com.goldex.companion.data.PersistenceJsonCodecs
import com.goldex.companion.data.local.db.GoldexDatabase
import com.goldex.companion.data.local.db.mappers.toEntity

object DataMigrationManager {

    private const val PREFS_NAME = "goldex_room_migration_prefs"
    private const val KEY_MIGRATED_V1 = "has_migrated_v1"
    private const val KEY_MIGRATION_TIMESTAMP = "migrated_at"

    @Synchronized
    fun migrateIfNeeded(context: Context, database: GoldexDatabase): Boolean {
        val migrationPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (migrationPrefs.getBoolean(KEY_MIGRATED_V1, false)) {
            return false
        }

        val appContext = context.applicationContext

        return try {
            database.runInTransaction {
                // 1. Migrate Customers
                val custPrefs = appContext.getSharedPreferences("goldex_customers_prefs", Context.MODE_PRIVATE)
                val custJson = custPrefs.getString("customers_json", null)
                if (!custJson.isNullOrBlank()) {
                    val customers = PersistenceJsonCodecs.decodeCustomers(custJson)
                    if (customers.isNotEmpty()) {
                        database.customerDao().insertAllSync(customers.map { it.toEntity() })
                    }
                }

                // 2. Migrate Ledger Transactions
                val txPrefs = appContext.getSharedPreferences("goldex_ledger_transactions_prefs", Context.MODE_PRIVATE)
                val txJson = txPrefs.getString("transactions_json", null)
                if (!txJson.isNullOrBlank()) {
                    val txs = PersistenceJsonCodecs.decodeLedgerTransactions(txJson)
                    if (txs.isNotEmpty()) {
                        database.ledgerTransactionDao().insertAllSync(txs.map { it.toEntity() })
                    }
                }

                // 3. Migrate Invoices (Barter & Classic)
                val invPrefs = appContext.getSharedPreferences("goldex_invoices_prefs", Context.MODE_PRIVATE)
                val barterJson = invPrefs.getString("barter_invoices_json", null)
                if (!barterJson.isNullOrBlank()) {
                    val barterInvoices = PersistenceJsonCodecs.decodeBarterInvoices(barterJson)
                    if (barterInvoices.isNotEmpty()) {
                        database.barterInvoiceDao().insertAllSync(barterInvoices.map { it.toEntity() })
                    }
                }

                val classicJson = invPrefs.getString("invoices_json", null)
                if (!classicJson.isNullOrBlank()) {
                    val classicInvoices = PersistenceJsonCodecs.decodeInvoices(classicJson)
                    if (classicInvoices.isNotEmpty()) {
                        database.classicInvoiceDao().insertAllSync(classicInvoices.map { it.toEntity() })
                    }
                }

                // 4. Migrate Inventory & Adjustments
                val invStockPrefs = appContext.getSharedPreferences("goldex_inventory_prefs", Context.MODE_PRIVATE)
                val itemsJson = invStockPrefs.getString("inventory_json", null)
                if (!itemsJson.isNullOrBlank()) {
                    val items = PersistenceJsonCodecs.decodeInventoryItems(itemsJson)
                    if (items.isNotEmpty()) {
                        database.inventoryDao().insertAllItemsSync(items.map { it.toEntity() })
                    }
                }

                val adjJson = invStockPrefs.getString("adjustments_json", null)
                if (!adjJson.isNullOrBlank()) {
                    val adjustments = PersistenceJsonCodecs.decodeStockAdjustments(adjJson)
                    if (adjustments.isNotEmpty()) {
                        database.inventoryDao().insertAllAdjustmentsSync(adjustments.map { it.toEntity() })
                    }
                }

                // 5. Migrate Portfolio Items
                val portfolioPrefs = appContext.getSharedPreferences("goldex_portfolio_prefs", Context.MODE_PRIVATE)
                val portfolioJson = portfolioPrefs.getString("items_json", null)
                if (!portfolioJson.isNullOrBlank()) {
                    val portfolioItems = PersistenceJsonCodecs.decodePortfolioItems(portfolioJson)
                    if (portfolioItems.isNotEmpty()) {
                        database.portfolioDao().insertAllSync(portfolioItems.map { it.toEntity() })
                    }
                }
            }

            // Mark as migrated. Existing SharedPreferences are preserved as an immutable safety backup.
            migrationPrefs.edit()
                .putBoolean(KEY_MIGRATED_V1, true)
                .putLong(KEY_MIGRATION_TIMESTAMP, System.currentTimeMillis())
                .apply()

            true
        } catch (e: Exception) {
            android.util.Log.e("DataMigrationManager", "Data migration failed: ${e.message}", e)
            false
        }
    }
}
