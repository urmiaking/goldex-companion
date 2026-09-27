package com.goldex.companion.data.local.db.migration

import android.content.Context
import com.goldex.companion.data.PersistenceJsonCodecs
import com.goldex.companion.data.local.db.GoldexDatabase
import com.goldex.companion.data.local.db.mappers.toEntity

object DataMigrationManager {

    private const val PREFS_NAME = "goldex_room_migration_prefs"
    private const val KEY_MIGRATED_V1 = "has_migrated_v1"
    private const val KEY_MIGRATION_TIMESTAMP = "migrated_at"

    private fun validateImport(json: String, ids: List<String>) {
        val source=org.json.JSONArray(json)
        require(source.length()==ids.size && ids.distinct().size==ids.size) { "Legacy record could not be imported" }
        for(i in 0 until source.length()) require(source.getJSONObject(i).getString("id")==ids[i]) { "Legacy ID changed" }
    }

    @Synchronized
    fun migrateIfNeeded(context: Context, database: GoldexDatabase): Boolean {
        val migrationPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (migrationPrefs.getBoolean(KEY_MIGRATED_V1, false) || database.syncDao().marker("legacy-import-v1") != null) {
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
                    validateImport(custJson, customers.map { it.id })
                    if (customers.isNotEmpty()) {
                        database.customerDao().insertAllSync(customers.map { it.toEntity() })
                    }
                }

                // 2. Migrate Ledger Transactions
                val txPrefs = appContext.getSharedPreferences("goldex_ledger_transactions_prefs", Context.MODE_PRIVATE)
                val txJson = txPrefs.getString("transactions_json", null)
                if (!txJson.isNullOrBlank()) {
                    val txs = PersistenceJsonCodecs.decodeLedgerTransactions(txJson)
                    validateImport(txJson, txs.map { it.id })
                    if (txs.isNotEmpty()) {
                        database.ledgerTransactionDao().insertAllSync(txs.map { it.toEntity() })
                    }
                }

                // 3. Migrate Invoices (Barter & Classic)
                val invPrefs = appContext.getSharedPreferences("goldex_invoices_prefs", Context.MODE_PRIVATE)
                val barterJson = invPrefs.getString("barter_invoices_json", null)
                if (!barterJson.isNullOrBlank()) {
                    val barterInvoices = PersistenceJsonCodecs.decodeBarterInvoices(barterJson)
                    validateImport(barterJson, barterInvoices.map { it.id })
                    if (barterInvoices.isNotEmpty()) {
                        database.barterInvoiceDao().insertAllSync(barterInvoices.map { it.toEntity() })
                    }
                }

                val classicJson = invPrefs.getString("invoices_json", null)
                if (!classicJson.isNullOrBlank()) {
                    val classicInvoices = PersistenceJsonCodecs.decodeInvoices(classicJson)
                    validateImport(classicJson, classicInvoices.map { it.id })
                    if (classicInvoices.isNotEmpty()) {
                        database.classicInvoiceDao().insertAllSync(classicInvoices.map { it.toEntity() })
                    }
                }

                // 4. Migrate Inventory & Adjustments
                val invStockPrefs = appContext.getSharedPreferences("goldex_inventory_prefs", Context.MODE_PRIVATE)
                val itemsJson = invStockPrefs.getString("inventory_json", null)
                if (!itemsJson.isNullOrBlank()) {
                    val items = PersistenceJsonCodecs.decodeInventoryItems(itemsJson)
                    validateImport(itemsJson, items.map { it.id })
                    if (items.isNotEmpty()) {
                        database.inventoryDao().insertAllItemsSync(items.map { it.toEntity() })
                    }
                }

                val adjJson = invStockPrefs.getString("adjustments_json", null)
                if (!adjJson.isNullOrBlank()) {
                    val adjustments = PersistenceJsonCodecs.decodeStockAdjustments(adjJson)
                    validateImport(adjJson, adjustments.map { it.id })
                    if (adjustments.isNotEmpty()) {
                        database.inventoryDao().insertAllAdjustmentsSync(adjustments.map { it.toEntity() })
                    }
                }

                // 5. Migrate Portfolio Items
                val portfolioPrefs = appContext.getSharedPreferences("goldex_portfolio_prefs", Context.MODE_PRIVATE)
                val portfolioJson = portfolioPrefs.getString("items_json", null)
                if (!portfolioJson.isNullOrBlank()) {
                    val portfolioItems = PersistenceJsonCodecs.decodePortfolioItems(portfolioJson)
                    validateImport(portfolioJson, portfolioItems.map { it.id })
                    if (portfolioItems.isNotEmpty()) {
                        database.portfolioDao().insertAllSync(portfolioItems.map { it.toEntity() })
                    }
                }
                database.syncDao().checkpoint(com.goldex.companion.data.sync.SyncCheckpoint(id="legacy-import-v1"))
            }

            // Mark as migrated. Existing SharedPreferences are preserved as an immutable safety backup.
            migrationPrefs.edit()
                .putBoolean(KEY_MIGRATED_V1, true)
                .putLong(KEY_MIGRATION_TIMESTAMP, System.currentTimeMillis())
                .apply()

            true
        } catch (e: Exception) {
            // Legacy preferences and the rolled-back database remain intact. Never continue with a partial import.
            throw IllegalStateException("Local data import requires recovery; legacy backup preserved", e)
        }
    }
}
