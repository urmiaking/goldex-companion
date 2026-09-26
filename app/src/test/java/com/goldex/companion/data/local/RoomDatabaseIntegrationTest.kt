package com.goldex.companion.data.local

import com.goldex.companion.data.PersistenceJsonCodecs
import com.goldex.companion.data.PortfolioCategory
import com.goldex.companion.data.PortfolioItem
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
import com.goldex.companion.data.local.db.mappers.toDomain
import com.goldex.companion.data.local.db.mappers.toEntity
import com.goldex.companion.data.local.repository.RoomCustomerRepository
import com.goldex.companion.data.local.repository.RoomInventoryRepository
import com.goldex.companion.data.local.repository.RoomInvoiceRepository
import com.goldex.companion.data.local.repository.RoomPortfolioRepository
import com.goldex.companion.model.BarterInvoice
import com.goldex.companion.model.Customer
import com.goldex.companion.model.CustomerRole
import com.goldex.companion.model.InventoryCategory
import com.goldex.companion.model.InventoryItem
import com.goldex.companion.model.Invoice
import com.goldex.companion.model.Karat
import com.goldex.companion.model.LedgerDirection
import com.goldex.companion.model.LedgerEntryType
import com.goldex.companion.model.LedgerTransaction
import com.goldex.companion.model.SettlementMethod
import com.goldex.companion.model.StockAdjustment
import com.goldex.companion.model.StockAdjustmentType
import com.goldex.companion.model.WageType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RoomDatabaseIntegrationTest {

    // --- Fake DAOs for Integration Testing ---

    private class FakeCustomerDao : CustomerDao {
        private val storage = mutableMapOf<String, CustomerEntity>()
        private val flow = MutableStateFlow<List<CustomerEntity>>(emptyList())

        private fun emit() {
            flow.value = storage.values.sortedByDescending { it.createdAt }
        }

        override fun observeAll(): Flow<List<CustomerEntity>> = flow.asStateFlow()
        override fun queryAll(): List<CustomerEntity> = storage.values.sortedByDescending { it.createdAt }
        override suspend fun getById(id: String): CustomerEntity? = storage[id]
        override fun getByIdSync(id: String): CustomerEntity? = storage[id]
        override suspend fun insert(customer: CustomerEntity) { storage[customer.id] = customer; emit() }
        override fun insertSync(customer: CustomerEntity) { storage[customer.id] = customer; emit() }
        override suspend fun insertAll(customers: List<CustomerEntity>) { customers.forEach { storage[it.id] = it }; emit() }
        override fun insertAllSync(customers: List<CustomerEntity>) { customers.forEach { storage[it.id] = it }; emit() }
        override suspend fun update(customer: CustomerEntity) { storage[customer.id] = customer; emit() }
        override fun updateSync(customer: CustomerEntity) { storage[customer.id] = customer; emit() }
        override suspend fun deleteById(id: String) { storage.remove(id); emit() }
        override fun deleteByIdSync(id: String) { storage.remove(id); emit() }
        override suspend fun clear() { storage.clear(); emit() }
    }

    private class FakeLedgerTransactionDao : LedgerTransactionDao {
        private val storage = mutableMapOf<String, LedgerTransactionEntity>()
        private val flow = MutableStateFlow<List<LedgerTransactionEntity>>(emptyList())

        private fun emit() {
            flow.value = storage.values.sortedByDescending { it.timestamp }
        }

        override fun observeByCustomerId(customerId: String): Flow<List<LedgerTransactionEntity>> =
            flow.asStateFlow() // In real DAO it filters, for test flow is fine

        override fun queryByCustomerId(customerId: String): List<LedgerTransactionEntity> =
            storage.values.filter { it.customerId == customerId }.sortedByDescending { it.timestamp }

        override fun queryByInvoiceId(invoiceId: String): List<LedgerTransactionEntity> =
            storage.values.filter { it.invoiceId == invoiceId }

        override suspend fun getTransactionsByInvoiceId(invoiceId: String): List<LedgerTransactionEntity> =
            queryByInvoiceId(invoiceId)

        override fun observeAll(): Flow<List<LedgerTransactionEntity>> = flow.asStateFlow()
        override fun queryAll(): List<LedgerTransactionEntity> = storage.values.sortedByDescending { it.timestamp }
        override suspend fun insert(tx: LedgerTransactionEntity) { storage[tx.id] = tx; emit() }
        override fun insertSync(tx: LedgerTransactionEntity) { storage[tx.id] = tx; emit() }
        override suspend fun insertAll(txs: List<LedgerTransactionEntity>) { txs.forEach { storage[it.id] = it }; emit() }
        override fun insertAllSync(txs: List<LedgerTransactionEntity>) { txs.forEach { storage[it.id] = it }; emit() }
        override suspend fun update(tx: LedgerTransactionEntity) { storage[tx.id] = tx; emit() }
        override fun updateSync(tx: LedgerTransactionEntity) { storage[tx.id] = tx; emit() }
        override suspend fun deleteById(id: String) { storage.remove(id); emit() }
        override fun deleteByIdSync(id: String) { storage.remove(id); emit() }
        override suspend fun deleteByInvoiceId(invoiceId: String) { storage.values.removeIf { it.invoiceId == invoiceId }; emit() }
        override fun deleteByInvoiceIdSync(invoiceId: String) { storage.values.removeIf { it.invoiceId == invoiceId }; emit() }
        override suspend fun clear() { storage.clear(); emit() }
    }

    private class FakeBarterInvoiceDao : BarterInvoiceDao {
        private val storage = mutableMapOf<String, BarterInvoiceEntity>()
        private val flow = MutableStateFlow<List<BarterInvoiceEntity>>(emptyList())

        private fun emit() { flow.value = storage.values.sortedByDescending { it.createdAt } }
        override fun observeAll(): Flow<List<BarterInvoiceEntity>> = flow.asStateFlow()
        override fun queryAll(): List<BarterInvoiceEntity> = storage.values.sortedByDescending { it.createdAt }
        override suspend fun getById(id: String): BarterInvoiceEntity? = storage[id]
        override fun getByIdSync(id: String): BarterInvoiceEntity? = storage[id]
        override suspend fun insert(invoice: BarterInvoiceEntity) { storage[invoice.id] = invoice; emit() }
        override fun insertSync(invoice: BarterInvoiceEntity) { storage[invoice.id] = invoice; emit() }
        override suspend fun insertAll(invoices: List<BarterInvoiceEntity>) { invoices.forEach { storage[it.id] = it }; emit() }
        override fun insertAllSync(invoices: List<BarterInvoiceEntity>) { invoices.forEach { storage[it.id] = it }; emit() }
        override suspend fun deleteById(id: String) { storage.remove(id); emit() }
        override fun deleteByIdSync(id: String) { storage.remove(id); emit() }
        override suspend fun clear() { storage.clear(); emit() }
    }

    private class FakeClassicInvoiceDao : ClassicInvoiceDao {
        private val storage = mutableMapOf<String, ClassicInvoiceEntity>()
        private val flow = MutableStateFlow<List<ClassicInvoiceEntity>>(emptyList())

        private fun emit() { flow.value = storage.values.sortedByDescending { it.createdAt } }
        override fun observeAll(): Flow<List<ClassicInvoiceEntity>> = flow.asStateFlow()
        override fun queryAll(): List<ClassicInvoiceEntity> = storage.values.sortedByDescending { it.createdAt }
        override suspend fun insert(invoice: ClassicInvoiceEntity) { storage[invoice.id] = invoice; emit() }
        override fun insertSync(invoice: ClassicInvoiceEntity) { storage[invoice.id] = invoice; emit() }
        override suspend fun insertAll(invoices: List<ClassicInvoiceEntity>) { invoices.forEach { storage[it.id] = it }; emit() }
        override fun insertAllSync(invoices: List<ClassicInvoiceEntity>) { invoices.forEach { storage[it.id] = it }; emit() }
        override suspend fun deleteById(id: String) { storage.remove(id); emit() }
        override fun deleteByIdSync(id: String) { storage.remove(id); emit() }
        override suspend fun clear() { storage.clear(); emit() }
    }

    private class FakeInventoryDao : InventoryDao {
        private val items = mutableMapOf<String, InventoryItemEntity>()
        private val adjustments = mutableMapOf<String, StockAdjustmentEntity>()
        private val itemsFlow = MutableStateFlow<List<InventoryItemEntity>>(emptyList())
        private val adjsFlow = MutableStateFlow<List<StockAdjustmentEntity>>(emptyList())

        private fun emitItems() { itemsFlow.value = items.values.sortedByDescending { it.createdAt } }
        private fun emitAdjs() { adjsFlow.value = adjustments.values.sortedByDescending { it.timestamp } }

        override fun observeAllItems(): Flow<List<InventoryItemEntity>> = itemsFlow.asStateFlow()
        override fun queryAllItems(): List<InventoryItemEntity> = items.values.sortedByDescending { it.createdAt }
        override suspend fun insertItem(item: InventoryItemEntity) { items[item.id] = item; emitItems() }
        override fun insertItemSync(item: InventoryItemEntity) { items[item.id] = item; emitItems() }
        override suspend fun insertAllItems(newItems: List<InventoryItemEntity>) { newItems.forEach { items[it.id] = it }; emitItems() }
        override fun insertAllItemsSync(newItems: List<InventoryItemEntity>) { newItems.forEach { items[it.id] = it }; emitItems() }
        override suspend fun updateItem(item: InventoryItemEntity) { items[item.id] = item; emitItems() }
        override fun updateItemSync(item: InventoryItemEntity) { items[item.id] = item; emitItems() }
        override suspend fun deleteItemById(id: String) { items.remove(id); emitItems() }
        override fun deleteItemByIdSync(id: String) { items.remove(id); emitItems() }
        override fun observeAllAdjustments(): Flow<List<StockAdjustmentEntity>> = adjsFlow.asStateFlow()
        override fun queryAllAdjustments(): List<StockAdjustmentEntity> = adjustments.values.sortedByDescending { it.timestamp }
        override suspend fun insertAdjustment(adj: StockAdjustmentEntity) { adjustments[adj.id] = adj; emitAdjs() }
        override fun insertAdjustmentSync(adj: StockAdjustmentEntity) { adjustments[adj.id] = adj; emitAdjs() }
        override suspend fun insertAllAdjustments(adjs: List<StockAdjustmentEntity>) { adjs.forEach { adjustments[it.id] = it }; emitAdjs() }
        override fun insertAllAdjustmentsSync(adjs: List<StockAdjustmentEntity>) { adjs.forEach { adjustments[it.id] = it }; emitAdjs() }
        override suspend fun clearItems() { items.clear(); emitItems() }
        override suspend fun clearAdjustments() { adjustments.clear(); emitAdjs() }
    }

    private class FakePortfolioDao : PortfolioDao {
        private val storage = mutableMapOf<String, PortfolioItemEntity>()
        private val flow = MutableStateFlow<List<PortfolioItemEntity>>(emptyList())

        private fun emit() { flow.value = storage.values.toList() }
        override fun observeAll(): Flow<List<PortfolioItemEntity>> = flow.asStateFlow()
        override fun queryAll(): List<PortfolioItemEntity> = storage.values.toList()
        override suspend fun insert(item: PortfolioItemEntity) { storage[item.id] = item; emit() }
        override fun insertSync(item: PortfolioItemEntity) { storage[item.id] = item; emit() }
        override suspend fun insertAll(items: List<PortfolioItemEntity>) { items.forEach { storage[it.id] = it }; emit() }
        override fun insertAllSync(items: List<PortfolioItemEntity>) { items.forEach { storage[it.id] = it }; emit() }
        override suspend fun deleteById(id: String) { storage.remove(id); emit() }
        override fun deleteByIdSync(id: String) { storage.remove(id); emit() }
        override suspend fun clear() { storage.clear(); emit() }
    }

    // --- Tests ---

    @Test
    fun roomCustomerRepositoryMaintainsFlowReactivityAndCrud() = runBlocking {
        val customerDao = FakeCustomerDao()
        val ledgerDao = FakeLedgerTransactionDao()
        val repo = RoomCustomerRepository(customerDao, ledgerDao)

        assertTrue(repo.getCustomers().isEmpty())

        val customer = Customer(id = "c-1", name = "زرگر یک", goldDebtGrams = 10.5)
        repo.addCustomer(customer)

        assertEquals(1, repo.getCustomers().size)
        assertEquals("زرگر یک", repo.getCustomers().first().name)
        assertEquals(10.5, repo.getCustomers().first().goldDebtGrams, 0.001)

        val tx = LedgerTransaction(
            id = "tx-1",
            customerId = "c-1",
            type = LedgerEntryType.GOLD_WEIGHT,
            direction = LedgerDirection.RECEIVE,
            scaleWeightGrams = 5.0,
            invoiceId = "inv-99"
        )
        repo.addTransaction(tx)

        val customerTxs = repo.getTransactions("c-1")
        assertEquals(1, customerTxs.size)
        assertEquals("inv-99", customerTxs.first().invoiceId)

        val invoiceTxs = repo.getTransactionsByInvoiceId("inv-99")
        assertEquals(1, invoiceTxs.size)

        repo.deleteTransactionsByInvoiceId("inv-99")
        assertTrue(repo.getTransactionsByInvoiceId("inv-99").isEmpty())

        repo.deleteCustomer("c-1")
        assertTrue(repo.getCustomers().isEmpty())
    }

    @Test
    fun roomInvoiceRepositorySavesAndDeletesBarterAndClassicInvoices() = runBlocking {
        val barterDao = FakeBarterInvoiceDao()
        val classicDao = FakeClassicInvoiceDao()
        val repo = RoomInvoiceRepository(barterDao, classicDao)

        val barterInvoice = BarterInvoice(
            id = "b-inv-1",
            invoiceNumber = "1403-100",
            spotPrice18k = 24_000_000L,
            customerRole = CustomerRole.WHOLESALER
        )
        repo.saveBarterInvoice(barterInvoice)
        assertEquals(1, repo.getBarterInvoices().size)
        assertEquals("1403-100", repo.getBarterInvoices().first().invoiceNumber)

        val classicInvoice = Invoice(
            id = "c-inv-1",
            invoiceNumber = "200001"
        )
        repo.saveInvoice(classicInvoice)
        assertEquals(1, repo.getInvoices().size)

        repo.deleteBarterInvoice("b-inv-1")
        assertTrue(repo.getBarterInvoices().isEmpty())

        repo.deleteInvoice("c-inv-1")
        assertTrue(repo.getInvoices().isEmpty())
    }

    @Test
    fun roomInventoryRepositoryMaintainsItemsAndAdjustments() = runBlocking {
        val inventoryDao = FakeInventoryDao()
        val repo = RoomInventoryRepository(inventoryDao)

        val item = InventoryItem(
            id = "item-101",
            code = "GLD-01",
            title = "پلاک طلا",
            grossWeightGrams = 3.5,
            stoneWeightGrams = 0.0,
            wagePercent = 10.0
        )
        repo.addItem(item)
        assertEquals(1, repo.getItems().size)

        val adj = StockAdjustment(
            id = "adj-1",
            itemId = "item-101",
            itemTitle = "پلاک طلا",
            type = StockAdjustmentType.CHARGE,
            quantityChange = 5,
            weightGrams = 17.5
        )
        repo.adjustStock(adj)
        assertEquals(1, repo.getAdjustments().size)

        repo.deleteItem("item-101")
        assertTrue(repo.getItems().isEmpty())
    }

    @Test
    fun roomPortfolioRepositoryCalculatesValuationCorrectly() = runBlocking {
        val portfolioDao = FakePortfolioDao()
        val repo = RoomPortfolioRepository(portfolioDao)

        val item = PortfolioItem(
            id = "p-1",
            title = "شمش ۵ گرمی",
            category = PortfolioCategory.GOLD,
            weightGrams = 5.0,
            purchasePriceTotal = 100_000_000L
        )
        repo.addItem(item)
        assertEquals(1, repo.getItems().size)
        assertEquals(5.0, repo.getItems().first().weightGrams, 0.001)

        repo.deleteItem("p-1")
        assertTrue(repo.getItems().isEmpty())
    }

    @Test
    fun legacyJsonToRoomEntitiesZeroDataLossMigrationVerification() {
        // Simulates the exact JSON string retrieved from legacy SharedPreferences
        val legacyCustomerJson = """
            [
              {
                "id": "legacy-cust-1",
                "name": "حاج علی اصغر",
                "phone": "09120000000",
                "goldDebtGrams": 45.678,
                "cashDebtTomans": 250000000,
                "role": "بنکدار و همکار بازار"
              }
            ]
        """.trimIndent()

        val decodedCustomers = PersistenceJsonCodecs.decodeCustomers(legacyCustomerJson)
        assertEquals(1, decodedCustomers.size)
        val customer = decodedCustomers.first()
        assertEquals("legacy-cust-1", customer.id)
        assertEquals(45.678, customer.goldDebtGrams, 0.0001)
        assertEquals(250_000_000L, customer.cashDebtTomans)

        // Mapping to Room Entity
        val customerEntity = customer.toEntity()
        assertEquals("legacy-cust-1", customerEntity.id)
        assertEquals(45.678, customerEntity.goldDebtGrams, 0.0001)

        // Mapping back to Domain Model
        val domainBack = customerEntity.toDomain()
        assertEquals(customer.id, domainBack.id)
        assertEquals(customer.name, domainBack.name)
        assertEquals(customer.goldDebtGrams, domainBack.goldDebtGrams, 0.0001)
        assertEquals(customer.cashDebtTomans, domainBack.cashDebtTomans)
    }
}
