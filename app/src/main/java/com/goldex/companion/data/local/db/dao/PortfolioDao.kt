package com.goldex.companion.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.goldex.companion.data.local.db.entities.PortfolioItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PortfolioDao {

    @Query("SELECT * FROM portfolio_items")
    fun observeAll(): Flow<List<PortfolioItemEntity>>

    @Query("SELECT * FROM portfolio_items")
    fun queryAll(): List<PortfolioItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: PortfolioItemEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertSync(item: PortfolioItemEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<PortfolioItemEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAllSync(items: List<PortfolioItemEntity>)

    @Query("DELETE FROM portfolio_items WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM portfolio_items WHERE id = :id")
    fun deleteByIdSync(id: String)

    @Query("DELETE FROM portfolio_items")
    suspend fun clear()
}
