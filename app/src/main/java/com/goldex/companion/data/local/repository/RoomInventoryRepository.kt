package com.goldex.companion.data.local.repository

import com.goldex.companion.data.InventoryStore
import com.goldex.companion.data.local.db.dao.InventoryDao
import com.goldex.companion.data.local.db.mappers.toDomain
import com.goldex.companion.data.local.db.mappers.toEntity
import com.goldex.companion.model.InventoryItem
import com.goldex.companion.model.StockAdjustment
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomInventoryRepository(
    private val inventoryDao: InventoryDao
) : InventoryStore {

    override fun getItems(): List<InventoryItem> {
        return inventoryDao.queryAllItems().map { it.toDomain() }
    }

    override fun addItem(item: InventoryItem) {
        inventoryDao.insertItemSync(item.toEntity())
    }

    override fun updateItem(item: InventoryItem) {
        inventoryDao.updateItemSync(item.toEntity())
    }

    override fun deleteItem(id: String) {
        inventoryDao.deleteItemByIdSync(id)
    }

    override fun adjustStock(adjustment: StockAdjustment) {
        inventoryDao.insertAdjustmentSync(adjustment.toEntity())
    }

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
