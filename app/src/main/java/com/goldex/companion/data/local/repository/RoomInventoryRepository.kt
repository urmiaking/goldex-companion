package com.goldex.companion.data.local.repository

import com.goldex.companion.data.InventoryStore
import com.goldex.companion.data.local.db.dao.InventoryDao
import com.goldex.companion.data.local.db.mappers.toDomain
import com.goldex.companion.data.local.db.mappers.toEntity
import com.goldex.companion.model.InventoryItem
import com.goldex.companion.model.StockAdjustment
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import com.goldex.companion.data.sync.*

class RoomInventoryRepository(
    private val inventoryDao: InventoryDao,
    private val sync: RoomSyncUnitOfWork? = null
) : InventoryStore {

    override fun getItems(): List<InventoryItem> {
        return inventoryDao.queryAllItems().map { it.toDomain() }
    }

    override fun addItem(item: InventoryItem) {
        write { inventoryDao.insertItemSync(item.toEntity()); sync?.changed("inventory", item.id, SyncJson.record(item)) }
    }

    override fun updateItem(item: InventoryItem) {
        write { inventoryDao.updateItemSync(item.toEntity()); sync?.changed("inventory", item.id, SyncJson.record(item)) }
    }

    override fun deleteItem(id: String) {
        write { inventoryDao.deleteItemByIdSync(id); sync?.changed("inventory", id, null) }
    }

    override fun adjustStock(adjustment: StockAdjustment) {
        write {
            require(adjustment.quantityChange > 0) { "Stock quantity must be positive" }
            val old = requireNotNull(inventoryDao.getItemSync(adjustment.itemId)) { "Missing stock item" }.toDomain()
            val delta = if (adjustment.type == com.goldex.companion.model.StockAdjustmentType.CHARGE) adjustment.quantityChange else -adjustment.quantityChange
            val quantity = Math.addExact(old.quantity, delta)
            require(quantity >= 0) { "Insufficient stock" }
            val updated = old.copy(quantity = quantity)
            inventoryDao.updateItemSync(updated.toEntity())
            inventoryDao.insertAdjustmentSync(adjustment.toEntity())
            sync?.changed("inventory", updated.id, SyncJson.record(updated))
            sync?.changed("stockAdjustment", adjustment.id, SyncJson.record(adjustment))
        }
    }
    private fun write(action: () -> Unit) { if (sync == null) action() else sync.transaction(action) }

    override fun getAdjustments(): List<StockAdjustment> {
        return inventoryDao.queryAllAdjustments().map { it.toDomain() }
    }

    // Reactive Flow extensions
    fun observeItems(): Flow<List<InventoryItem>> {
        return inventoryDao.observeAllItems().map { list -> list.map { it.toDomain() } }
    }

    fun observeAdjustments(): Flow<List<StockAdjustment>> {
        return inventoryDao.observeAllAdjustments().map { list -> list.map { it.toDomain() } }
    }
}
