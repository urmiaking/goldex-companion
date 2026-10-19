package com.goldex.companion.desktop

import com.goldex.companion.data.*
import com.goldex.companion.desktop.data.db.DesktopDataMigrationManager
import com.goldex.companion.desktop.data.db.DesktopSqliteDatabase
import com.goldex.companion.model.*
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.nio.file.Files
import java.sql.DriverManager
import kotlin.test.*

class DesktopSqliteDatabaseTest {

    @get:Rule
    val temporary = TemporaryFolder()

    private fun dbFile() = temporary.newFolder().toPath().resolve("qirato.db")

    private fun samplePortfolioItem() = PortfolioItem(
        id = "port-1",
        title = "طلای آبشده ۱۸ عیار",
        category = PortfolioCategory.GOLD,
        weightGrams = 10.5,
        purchasePriceTotal = 63_000_000
    )

    private fun sampleCustomer() = Customer(
        id = "cust-1",
        name = "حاج رضا زرگر",
        phone = "09123456789",
        role = "بنکدار و همکار بازار",
        goldDebtGrams = 0.0,
        cashDebtTomans = 0L
    )

    private fun sampleTransaction() = LedgerTransaction(
        id = "tx-1",
        customerId = "cust-1",
        documentNumber = "DOC-101",
        title = "تحویل آبشده",
        type = LedgerEntryType.GOLD_WEIGHT,
        direction = LedgerDirection.RECEIVE,
        scaleWeightGrams = 5.25,
        equivalent750WeightGrams = 5.25,
        amountTomans = 0L,
        timestamp = 1700000000000L
    )

    private fun sampleInventoryItem() = InventoryItem(
        id = "inv-1",
        code = "1000001",
        title = "انگشتر طلا فیوژن",
        category = InventoryCategory.RINGS,
        location = "ویترین اصلی",
        grossWeightGrams = 3.5,
        stoneWeightGrams = 0.2,
        karat = Karat.K18,
        customKaratValue = 750,
        wageType = WageType.PERCENTAGE,
        wagePercent = 12.0,
        wageValue = 12.0,
        profitPercent = 7.0,
        taxPercent = 9.0,
        quantity = 2,
        createdAt = 1700000000000L
    )

    @Test
    fun databaseFileCreationAndPragmas() {
        val file = dbFile()
        DesktopSqliteDatabase.open(file).use { db ->
            assertTrue(Files.exists(file))
            assertEquals("wal", db.getPragma("journal_mode")?.lowercase())
            assertEquals("1", db.getPragma("foreign_keys"))
        }
    }

    @Test
    fun portfolioCRUDOperations() {
        val file = dbFile()
        DesktopSqliteDatabase.open(file).use { db ->
            assertTrue(db.getItems().isEmpty())

            val item = samplePortfolioItem()
            db.addItem(item)
            assertEquals(listOf(item), db.getItems())

            val updated = item.copy(title = "طلای آبشده اصلاح شده", weightGrams = 12.0)
            db.addItem(updated)
            assertEquals(listOf(updated), db.getItems())

            db.deleteItem(item.id)
            assertTrue(db.getItems().isEmpty())
        }
    }

    @Test
    fun settingsAndPreferencesPersistence() {
        val file = dbFile()
        DesktopSqliteDatabase.open(file).use { db ->
            assertEquals(AppSettings(), db.loadSettings())
            assertFalse(db.loadDarkTheme())
            assertFalse(db.loadReduceMotion())
            assertFalse(db.loadInventoryVisible())
            assertFalse(db.hasSeededOpeningInventory())

            val custom = AppSettings(
                galleryName = "طلا و جواهر خورشید",
                managerName = "محمد زرگر",
                defaultProfitPercent = "8.5",
                defaultTaxPercent = "9"
            )
            db.saveSettings(custom)
            db.saveDarkTheme(true)
            db.saveReduceMotion(true)
            db.saveInventoryVisible(true)
            db.markOpeningInventorySeeded()
        }

        // Reopen to ensure durability
        DesktopSqliteDatabase.open(file).use { db ->
            assertEquals("طلا و جواهر خورشید", db.loadSettings().galleryName)
            assertEquals("8.5", db.loadSettings().defaultProfitPercent)
            assertTrue(db.loadDarkTheme())
            assertTrue(db.loadReduceMotion())
            assertTrue(db.loadInventoryVisible())
            assertTrue(db.hasSeededOpeningInventory())
        }
    }

    @Test
    fun inventoryCRUDAndStockAdjustment() {
        val file = dbFile()
        DesktopSqliteDatabase.open(file).use { db ->
            val item = sampleInventoryItem()
            db.inventoryStore.addItem(item)
            assertEquals(listOf(item), db.getInventoryItems())

            // Stock adjustment
            val adjustment = StockAdjustment(
                id = "adj-1",
                itemId = item.id,
                itemTitle = item.title,
                type = StockAdjustmentType.DEDUCT,
                quantityChange = 1,
                weightGrams = 1.75,
                reason = "فروش به مشتری",
                timestamp = 1700000005000L
            )
            db.inventoryStore.adjustStock(adjustment)

            val adjusted = db.getInventoryItems().single()
            assertEquals(1, adjusted.quantity)
            assertEquals(listOf(adjustment), db.getStockAdjustments())

            db.inventoryStore.deleteItem(item.id)
            assertTrue(db.getInventoryItems().isEmpty())
            // Stock adjustment audit log survives product deletion
            assertEquals(listOf(adjustment), db.getStockAdjustments())
        }
    }

    @Test
    fun customerAndLedgerTransactionsWithReferentialCheck() {
        val file = dbFile()
        DesktopSqliteDatabase.open(file).use { db ->
            val customer = sampleCustomer()
            db.addCustomer(customer)
            assertEquals(listOf(customer), db.getCustomers())

            // Unregistered customer transaction must fail
            assertFails {
                db.addTransaction(sampleTransaction().copy(customerId = "non-existent"))
            }

            // Valid transaction
            val tx = sampleTransaction()
            db.addTransaction(tx)
            assertEquals(listOf(tx), db.getTransactions(customer.id))

            // Non-zero balance deletion prevention
            db.updateCustomer(customer.copy(cashDebtTomans = 50_000L))
            assertFailsWith<IllegalArgumentException> {
                db.deleteCustomer(customer.id)
            }

            // Settled customer deletion succeeds and archives contact
            db.updateCustomer(customer.copy(cashDebtTomans = 0L))
            db.deleteCustomer(customer.id)
            assertTrue(db.getCustomers().isEmpty())
            // Transactions remain as immutable audit trail
            assertEquals(listOf(tx), db.getTransactions(customer.id))
        }
    }

    @Test
    fun invoicesAndBarterInvoicesPersistence() {
        val file = dbFile()
        DesktopSqliteDatabase.open(file).use { db ->
            val classic = Invoice(
                id = "inv-c-1",
                invoiceNumber = "CL-200",
                customer = sampleCustomer(),
                createdAt = 1700000000000L
            )
            db.saveInvoice(classic)
            assertEquals(listOf(classic), db.getInvoices())

            val barter = BarterInvoice(
                id = "inv-b-1",
                invoiceNumber = "BT-300",
                customer = sampleCustomer(),
                spotPrice18k = 6_500_000L,
                debtBasis = InvoiceDebtBasis.CASH,
                createdAt = 1700000000000L
            )
            db.saveBarterInvoice(barter)
            assertEquals(listOf(barter), db.getBarterInvoices())

            db.deleteInvoice(classic.id)
            db.deleteBarterInvoice(barter.id)
            assertTrue(db.getInvoices().isEmpty())
            assertTrue(db.getBarterInvoices().isEmpty())
        }
    }

    @Test
    fun transactionRollbackLeavesDatabaseUnchangedOnFailure() {
        val file = dbFile()
        DesktopSqliteDatabase.open(file).use { db ->
            val customer = sampleCustomer()
            db.addCustomer(customer)

            assertFailsWith<IllegalStateException> {
                db.transaction {
                    db.updateCustomer(customer.copy(cashDebtTomans = 100_000_000L))
                    db.addTransaction(sampleTransaction().copy(id = "tx-rollback", amountTomans = 100_000_000L))
                    error("Synthetic business failure triggering rollback")
                }
            }

            // Verify rolled back
            assertEquals(0L, db.getCustomers().single().cashDebtTomans)
            assertTrue(db.getTransactions(customer.id).isEmpty())
        }
    }

    @Test
    fun autoMigrationFromLegacyWorkspaceJson() {
        val folder = temporary.newFolder().toPath()
        val jsonFile = folder.resolve("workspace.json")

        val customer = sampleCustomer()
        val tx = sampleTransaction()
        val portfolio = samplePortfolioItem()
        val inv = sampleInventoryItem()

        val jsonContent = JSONObject().apply {
            put("schemaVersion", 1)
            put("settings", JSONObject(PersistenceJsonCodecs.encodeSettings(AppSettings(galleryName = "گالری مهاجرت یافته"))))
            put("darkTheme", true)
            put("reduceMotion", true)
            put("inventoryVisible", true)
            put("openingInventorySeeded", true)
            put("portfolio", JSONArray(PersistenceJsonCodecs.encodePortfolioItems(listOf(portfolio))))
            put("inventory", JSONArray(PersistenceJsonCodecs.encodeInventoryItems(listOf(inv))))
            put("customers", JSONArray().put(JSONObject(PersistenceJsonCodecs.encodeCustomerString(customer))))
            put("ledgerTransactions", JSONArray(PersistenceJsonCodecs.encodeLedgerTransactions(listOf(tx))))
        }.toString(2)

        Files.writeString(jsonFile, jsonContent)

        val db = DesktopSqliteDatabase.open(folder.resolve("qirato.db"))
        db.use {
            val migrated = DesktopDataMigrationManager.migrateIfNeeded(folder, it)
            assertTrue(migrated)

            assertEquals("گالری مهاجرت یافته", it.loadSettings().galleryName)
            assertTrue(it.loadDarkTheme())
            assertTrue(it.loadReduceMotion())
            assertTrue(it.loadInventoryVisible())
            assertTrue(it.hasSeededOpeningInventory())
            assertEquals(listOf(portfolio), it.getItems())
            assertEquals(listOf(inv), it.getInventoryItems())
            assertEquals(listOf(customer), it.getCustomers())
            assertEquals(listOf(tx), it.getTransactions(customer.id))
        }
    }
}
