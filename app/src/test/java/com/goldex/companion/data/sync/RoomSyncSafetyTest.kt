package com.goldex.companion.data.sync

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import android.content.Context
import com.goldex.companion.data.local.db.mappers.*
import com.goldex.companion.data.local.db.GoldexDatabase
import com.goldex.companion.data.local.repository.RoomCustomerRepository
import com.goldex.companion.data.local.repository.RoomInvoiceRepository
import com.goldex.companion.domain.invoice.SaveBarterInvoiceUseCase
import com.goldex.companion.model.*
import org.json.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28])
class RoomSyncSafetyTest {
    private lateinit var db: GoldexDatabase
    private lateinit var unit: RoomSyncUnitOfWork
    private lateinit var customers: RoomCustomerRepository
    @Before fun setup() {
        db=GoldexDatabase.buildInMemory(ApplicationProvider.getApplicationContext())
        unit=RoomSyncUnitOfWork(db)
        customers=RoomCustomerRepository(db.customerDao(),db.ledgerTransactionDao(),unit)
    }
    @After fun close() { db.close() }
    @Test fun rollbackKeepsCustomerAndOutboxUnchanged() {
        try { unit.transaction { customers.addCustomer(Customer(id="id",name="test")); error("simulated interruption") } } catch (_: Exception) { }
        assertTrue(customers.getCustomers().isEmpty()); assertEquals(0,db.syncDao().pendingCount())
        assertNull(db.syncDao().metadata("customer","id"))
    }
    @Test fun ackDoesNotEraseEditMadeWhileRequestWasInFlight() {
        customers.addCustomer(Customer(id="id",name="first",createdAt=1))
        val sent=db.syncDao().first()!!
        customers.updateCustomer(customers.getCustomers().single().copy(name="second"))
        val store=SyncLocalStore(ApplicationProvider.getApplicationContext(),db)
        store.acknowledge(sent,JSONObject().put("operationId",sent.operationId).put("changes",JSONArray().put(JSONObject().put("type","customer").put("id","id").put("version",1))))
        assertEquals(1,db.syncDao().pendingCount())
        assertEquals(2,db.syncDao().metadata("customer","id")!!.localVersion)
        assertEquals(1,db.syncDao().metadata("customer","id")!!.version)
        assertEquals("second",customers.getCustomers().single().name)
    }
    @Test fun invoiceAndLedgerAreOneGroupAndFailureRollsAllBack() {
        val customer=Customer(id="customer",name="test",createdAt=1)
        customers.addCustomer(customer)
        val invoices=RoomInvoiceRepository(db.barterInvoiceDao(),db.classicInvoiceDao(),unit)
        val invoice=BarterInvoice(id="invoice",customer=customer,createdAt=10)
        SaveBarterInvoiceUseCase(invoices,customers,unit).save(invoice)
        val last=db.syncDao().allOutbox().last()
        assertTrue(JSONObject(last.payload).getJSONArray("changes").toString().contains("barterInvoice"))
        val before=db.syncDao().pendingCount()
        try { unit.transaction { invoices.saveBarterInvoice(invoice.copy(note="new")); customers.updateCustomer(customer.copy(cashDebtTomans=100)); error("interruption") } } catch (_: Exception) { }
        assertEquals("",invoices.getBarterInvoices().single().note)
        assertEquals(0,customers.getCustomers().single().cashDebtTomans)
        assertEquals(before,db.syncDao().pendingCount())
    }
    @Test fun stockChangeAndMovementAreAtomicAndRejectInsufficientStock() {
        val repo=com.goldex.companion.data.local.repository.RoomInventoryRepository(db.inventoryDao(),unit)
        repo.addItem(InventoryItem(id="stock",code="1",title="stock",quantity=2))
        repo.adjustStock(StockAdjustment(id="adj",itemId="stock",itemTitle="stock",type=StockAdjustmentType.DEDUCT,quantityChange=1))
        assertEquals(1,repo.getItems().single().quantity)
        val changes=JSONObject(db.syncDao().allOutbox().last().payload).getJSONArray("changes")
        assertEquals(2,changes.length())
        val count=db.syncDao().pendingCount()
        try { repo.adjustStock(StockAdjustment(itemId="stock",itemTitle="stock",type=StockAdjustmentType.DEDUCT,quantityChange=2)); fail("must reject") } catch (_: IllegalArgumentException) { }
        assertEquals(1,repo.getAdjustments().size); assertEquals(count,db.syncDao().pendingCount())
    }
    @Test fun explicitLocalConflictChoiceRebasesLaterEditsWithoutLosingThem() {
        customers.addCustomer(Customer(id="id",name="first",createdAt=1))
        val first=db.syncDao().first()!!
        customers.updateCustomer(customers.getCustomers().single().copy(name="second"))
        val remote=JSONObject().put("records",JSONArray().put(JSONObject().put("type","customer").put("id","id").put("version",7).put("deleted",false).put("payload",SyncJson.record(Customer(id="id",name="remote",createdAt=1)))))
        db.syncDao().outbox(first.copy(status="CONFLICT"))
        db.syncDao().conflict(SyncConflict(first.operationId,first.payload,remote.toString(),"VERSION_CONFLICT",1))
        SyncLocalStore(ApplicationProvider.getApplicationContext(),db).chooseLocalConflict(first.operationId)
        val queue=db.syncDao().allOutbox()
        assertEquals(2,queue.size); assertEquals(first.sequence,queue[0].sequence)
        assertNotEquals(first.operationId,queue[0].operationId)
        assertEquals(7L,JSONObject(queue[0].payload).getJSONArray("changes").getJSONObject(0).getLong("baseVersion"))
        assertEquals(8L,JSONObject(queue[1].payload).getJSONArray("changes").getJSONObject(0).getLong("baseVersion"))
        assertEquals("second",customers.getCustomers().single().name)
        assertEquals(9L,db.syncDao().metadata("customer","id")!!.localVersion)
        assertTrue(db.syncDao().conflicts().isEmpty())
    }
    @Test fun corruptRestoreKeepsActiveDataQueueAndStaging() {
        customers.addCustomer(Customer(id="safe",name="keep",createdAt=1))
        val record=JSONObject().put("type","customer").put("id","broken").put("version",1).put("deleted",false).put("payload",JSONObject().put("id","broken").put("name","bad").put("cashDebtTomans","not-money"))
        db.syncDao().stage(SyncStaging("snapshot",0,JSONArray().put(record).toString()))
        try { SyncLocalStore(ApplicationProvider.getApplicationContext(),db).activateSnapshot("snapshot","workspace",1,1,0,2); fail("invalid data must stop restore") } catch (_: CloudException) { }
        assertEquals("safe",customers.getCustomers().single().id); assertEquals(1,db.syncDao().pendingCount())
        assertEquals(1,db.syncDao().staged("snapshot").size); assertFalse(db.syncDao().checkpoint()!!.initialized)
    }
    @Test fun versionOneMigratesWithoutLosingAnySevenEntityTables() {
        val context=ApplicationProvider.getApplicationContext<Context>(); val name="sync-migration-test.db"
        context.deleteDatabase(name)
        val schema=JSONObject(File("schemas/com.goldex.companion.data.local.db.GoldexDatabase/1.json").readText()).getJSONObject("database")
        val old=context.openOrCreateDatabase(name,Context.MODE_PRIVATE,null)
        val entities=schema.getJSONArray("entities")
        for(i in 0 until entities.length()) {
            val entity=entities.getJSONObject(i); old.execSQL(entity.getString("createSql").replace("\u0024{TABLE_NAME}",entity.getString("tableName")))
            val indices=entity.getJSONArray("indices")
            for(j in 0 until indices.length()) old.execSQL(indices.getJSONObject(j).getString("createSql").replace("\u0024{TABLE_NAME}",entity.getString("tableName")))
        }
        val fixtures=mapOf(
            "customers" to Customer(id="customer",name="test",createdAt=1).toEntity(),
            "ledger_transactions" to LedgerTransaction(id="ledger",customerId="customer",timestamp=1).toEntity(),
            "classic_invoices" to Invoice(id="classic",createdAt=1).toEntity(),
            "barter_invoices" to BarterInvoice(id="barter",createdAt=1).toEntity(),
            "inventory_items" to InventoryItem(id="inventory",code="1",title="test",createdAt=1).toEntity(),
            "stock_adjustments" to StockAdjustment(id="stock",itemId="inventory",itemTitle="test",timestamp=1).toEntity(),
            "portfolio_items" to com.goldex.companion.data.PortfolioItem(id="stable",title="test",category=com.goldex.companion.data.PortfolioCategory.GOLD,weightGrams=0.001,purchasePriceTotal=9007199254740993L).toEntity()
        )
        for(i in 0 until entities.length()) {
            val schemaEntity=entities.getJSONObject(i); val table=schemaEntity.getString("tableName")
            val fixture=fixtures.getValue(table); val fields=schemaEntity.getJSONArray("fields")
            val columns=(0 until fields.length()).map { fields.getJSONObject(it).getString("columnName") }
            val values=(0 until fields.length()).map {
                val value=fixture.javaClass.getDeclaredField(fields.getJSONObject(it).getString("fieldPath")).apply { isAccessible=true }.get(fixture)
                if(value is Enum<*>) value.name else value
            }.toTypedArray()
            old.execSQL("INSERT INTO $table (${columns.joinToString(",")}) VALUES (${columns.joinToString(",") { "?" }})",values)
        }
        old.version=1; old.close()
        val migrated=Room.databaseBuilder(context,GoldexDatabase::class.java,name).addMigrations(SYNC_MIGRATION_1_2).allowMainThreadQueries().build()
        assertEquals("stable",migrated.portfolioDao().queryAll().single().id)
        assertEquals(9007199254740993L,migrated.portfolioDao().queryAll().single().purchasePriceTotal)
        assertEquals("customer",migrated.customerDao().queryAll().single().id)
        assertEquals("ledger",migrated.ledgerTransactionDao().queryAll().single().id)
        assertEquals("classic",migrated.classicInvoiceDao().queryAll().single().id)
        assertEquals("barter",migrated.barterInvoiceDao().queryAll().single().id)
        assertEquals("inventory",migrated.inventoryDao().queryAllItems().single().id)
        assertEquals("stock",migrated.inventoryDao().queryAllAdjustments().single().id)
        assertEquals(0,migrated.syncDao().pendingCount()); migrated.close(); context.deleteDatabase(name)
    }

    @Test
    fun loadSafeProfileBitmap_returnsNullOnBlankOrMissingFile() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        assertNull(com.goldex.companion.ui.components.loadSafeProfileBitmap(context, ""))
        assertNull(com.goldex.companion.ui.components.loadSafeProfileBitmap(context, "   "))
        assertNull(com.goldex.companion.ui.components.loadSafeProfileBitmap(context, "file:///non/existent/path.png"))
    }
}
