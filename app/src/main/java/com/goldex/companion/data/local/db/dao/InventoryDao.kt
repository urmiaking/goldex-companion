package com.goldex.companion.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.goldex.companion.data.local.db.entities.InventoryItemEntity
import com.goldex.companion.data.local.db.entities.StockAdjustmentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface InventoryDao {

    @Query("SELECT * FROM inventory_items ORDER BY createdAt DESC")
    fun observeAllItems(): Flow<List<InventoryItemEntity>>

    @Query("SELECT * FROM inventory_items ORDER BY createdAt DESC")
    fun queryAllItems(): List<InventoryItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: InventoryItemEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertItemSync(item: InventoryItemEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllItems(items: List<InventoryItemEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAllItemsSync(items: List<InventoryItemEntity>)

    @Update
    suspend fun updateItem(item: InventoryItemEntity)

    @Update
    fun updateItemSync(item: InventoryItemEntity)

    @Query("DELETE FROM inventory_items WHERE id = :id")
    suspend fun deleteItemById(id: String)

    @Query("DELETE FROM inventory_items WHERE id = :id")
    fun deleteItemByIdSync(id: String)

    @Query("SELECT * FROM stock_adjustments ORDER BY timestamp DESC")
    fun observeAllAdjustments(): Flow<List<StockAdjustmentEntity>>

    @Query("SELECT * FROM stock_adjustments ORDER BY timestamp DESC")
    fun queryAllAdjustments(): List<StockAdjustmentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAdjustment(adj: StockAdjustmentEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAdjustmentSync(adj: StockAdjustmentEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllAdjustments(adjs: List<StockAdjustmentEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAllAdjustmentsSync(adjs: List<StockAdjustmentEntity>)

    @Query("DELETE FROM inventory_items")
    suspend fun clearItems()

    @Query("DELETE FROM stock_adjustments")
    suspend fun clearAdjustments()
}
