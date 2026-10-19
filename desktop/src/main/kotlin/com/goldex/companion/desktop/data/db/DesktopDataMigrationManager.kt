package com.goldex.companion.desktop.data.db

import com.goldex.companion.data.PersistenceJsonCodecs
import com.goldex.companion.desktop.data.DesktopDataStore
import com.goldex.companion.desktop.data.DesktopInventoryOptions
import com.goldex.companion.desktop.data.MarketJson
import org.json.JSONArray
import org.json.JSONObject
import java.nio.file.Files
import java.nio.file.Path

/**
 * Ensures a zero-data-loss, transactional migration from legacy `workspace.json`
 * to the robust relational `DesktopSqliteDatabase`.
 */
object DesktopDataMigrationManager {

    private const val MIGRATION_KEY = "migrated_from_workspace_json_v1"

    @Synchronized
    fun migrateIfNeeded(directory: Path, db: DesktopSqliteDatabase): Boolean {
        if (db.getMetadata(MIGRATION_KEY) == "true") {
            return false
        }

        val jsonFile = directory.resolve("workspace.json")
        if (!Files.exists(jsonFile)) {
            db.setMetadata(MIGRATION_KEY, "true")
            return false
        }

        return try {
            val content = Files.readString(jsonFile)
            if (content.isBlank()) {
                db.setMetadata(MIGRATION_KEY, "true")
                return false
            }

            val doc = JSONObject(content)
            DesktopDataStore.validate(doc)

            db.transaction {
                // 1. Settings & UI Preferences
                if (doc.has("settings")) {
                    val settings = PersistenceJsonCodecs.decodeSettings(doc.getJSONObject("settings").toString())
                    db.saveSettings(settings)
                }
                if (doc.has("darkTheme")) {
                    db.saveDarkTheme(doc.optBoolean("darkTheme", false))
                }
                if (doc.has("reduceMotion")) {
                    db.saveReduceMotion(doc.optBoolean("reduceMotion", false))
                }
                if (doc.has("inventoryVisible")) {
                    db.saveInventoryVisible(doc.optBoolean("inventoryVisible", false))
                }
                if (doc.optBoolean("openingInventorySeeded", false)) {
                    db.markOpeningInventorySeeded()
                }

                // 2. Inventory Options (Categories & Locations)
                val categories = doc.optJSONArray(DesktopInventoryOptions.CATEGORIES) ?: JSONArray()
                for (i in 0 until categories.length()) {
                    val record = categories.getJSONObject(i)
                    db.addInventoryOption(DesktopInventoryOptions.CATEGORIES, record.getString("name"))
                }
                val locations = doc.optJSONArray(DesktopInventoryOptions.LOCATIONS) ?: JSONArray()
                for (i in 0 until locations.length()) {
                    val record = locations.getJSONObject(i)
                    db.addInventoryOption(DesktopInventoryOptions.LOCATIONS, record.getString("name"))
                }

                // 3. Portfolio Items
                val portfolio = doc.optJSONArray("portfolio") ?: JSONArray()
                if (portfolio.length() > 0) {
                    val items = PersistenceJsonCodecs.decodePortfolioItems(portfolio.toString())
                    items.forEach { db.addItem(it) }
                }

                // 4. Inventory Items & Code Sequence
                val inventory = doc.optJSONArray("inventory") ?: JSONArray()
                if (inventory.length() > 0) {
                    val items = PersistenceJsonCodecs.decodeInventoryItems(inventory.toString())
                    for (i in 0 until inventory.length()) {
                        val record = inventory.getJSONObject(i)
                        val item = items[i]
                        val categoryId = if (record.has("customCategoryId") && !record.isNull("customCategoryId")) {
                            record.getString("customCategoryId")
                        } else null
                        db.saveInventoryItem(item, mustExist = false, categoryId = categoryId, updateCategory = true)
                    }
                }

                // 5. Stock Adjustments
                val adjustments = doc.optJSONArray("stockAdjustments") ?: JSONArray()
                if (adjustments.length() > 0) {
                    val list = PersistenceJsonCodecs.decodeStockAdjustments(adjustments.toString())
                    list.forEach { db.inventoryStore.adjustStock(it) }
                }

                // 6. Customers & Deleted Customers
                val customers = doc.optJSONArray("customers") ?: JSONArray()
                if (customers.length() > 0) {
                    val list = PersistenceJsonCodecs.decodeCustomers(customers.toString())
                    list.forEach { db.addCustomer(it) }
                }
                val deleted = doc.optJSONArray("deletedCustomers") ?: JSONArray()
                for (i in 0 until deleted.length()) {
                    val record = deleted.getJSONObject(i)
                    val custId = record.getString("id")
                    // If customer was in deleted list, record to deleted_customers table
                    db.deleteCustomer(custId)
                }

                // 7. Ledger Transactions
                val ledger = doc.optJSONArray("ledgerTransactions") ?: JSONArray()
                if (ledger.length() > 0) {
                    val list = PersistenceJsonCodecs.decodeLedgerTransactions(ledger.toString())
                    list.forEach { db.addTransaction(it) }
                }

                // 8. Invoices (Classic & Barter)
                val invoices = doc.optJSONArray("invoices") ?: JSONArray()
                if (invoices.length() > 0) {
                    val list = PersistenceJsonCodecs.decodeInvoices(invoices.toString())
                    list.forEach { db.saveInvoice(it) }
                }
                val barter = doc.optJSONArray("barterInvoices") ?: JSONArray()
                if (barter.length() > 0) {
                    val list = PersistenceJsonCodecs.decodeBarterInvoices(barter.toString())
                    list.forEach { db.saveBarterInvoice(it) }
                }

                // 9. Market Cache
                doc.optJSONObject("market")?.let { marketJson ->
                    val snapshot = MarketJson.decode(marketJson)
                    db.saveMarket(snapshot)
                }

                db.setMetadata(MIGRATION_KEY, "true")
            }
            true
        } catch (e: Exception) {
            // Never corrupt or delete the original workspace.json on migration errors
            false
        }
    }
}
