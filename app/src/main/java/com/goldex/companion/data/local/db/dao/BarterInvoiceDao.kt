package com.goldex.companion.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.goldex.companion.data.local.db.entities.BarterInvoiceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BarterInvoiceDao {

    @Query("SELECT * FROM barter_invoices ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<BarterInvoiceEntity>>

    @Query("SELECT * FROM barter_invoices ORDER BY createdAt DESC")
    fun queryAll(): List<BarterInvoiceEntity>

    @Query("SELECT * FROM barter_invoices WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): BarterInvoiceEntity?

    @Query("SELECT * FROM barter_invoices WHERE id = :id LIMIT 1")
    fun getByIdSync(id: String): BarterInvoiceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(invoice: BarterInvoiceEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertSync(invoice: BarterInvoiceEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(invoices: List<BarterInvoiceEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAllSync(invoices: List<BarterInvoiceEntity>)

    @Query("DELETE FROM barter_invoices WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM barter_invoices WHERE id = :id")
    fun deleteByIdSync(id: String)

    @Query("DELETE FROM barter_invoices")
    suspend fun clear()
}
