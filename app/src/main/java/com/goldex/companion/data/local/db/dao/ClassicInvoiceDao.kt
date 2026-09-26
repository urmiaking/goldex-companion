package com.goldex.companion.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.goldex.companion.data.local.db.entities.ClassicInvoiceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ClassicInvoiceDao {

    @Query("SELECT * FROM classic_invoices ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<ClassicInvoiceEntity>>

    @Query("SELECT * FROM classic_invoices ORDER BY createdAt DESC")
    fun queryAll(): List<ClassicInvoiceEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(invoice: ClassicInvoiceEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertSync(invoice: ClassicInvoiceEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(invoices: List<ClassicInvoiceEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAllSync(invoices: List<ClassicInvoiceEntity>)

    @Query("DELETE FROM classic_invoices WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM classic_invoices WHERE id = :id")
    fun deleteByIdSync(id: String)

    @Query("DELETE FROM classic_invoices")
    suspend fun clear()
}
