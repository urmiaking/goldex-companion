package com.goldex.companion.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.goldex.companion.data.local.db.entities.LedgerTransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LedgerTransactionDao {

    @Query("SELECT * FROM ledger_transactions WHERE customerId = :customerId ORDER BY timestamp DESC")
    fun observeByCustomerId(customerId: String): Flow<List<LedgerTransactionEntity>>

    @Query("SELECT * FROM ledger_transactions WHERE customerId = :customerId ORDER BY timestamp DESC")
    fun queryByCustomerId(customerId: String): List<LedgerTransactionEntity>

    @Query("SELECT * FROM ledger_transactions WHERE invoiceId = :invoiceId")
    fun queryByInvoiceId(invoiceId: String): List<LedgerTransactionEntity>

    @Query("SELECT * FROM ledger_transactions WHERE invoiceId = :invoiceId")
    suspend fun getTransactionsByInvoiceId(invoiceId: String): List<LedgerTransactionEntity>

    @Query("SELECT * FROM ledger_transactions ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<LedgerTransactionEntity>>

    @Query("SELECT * FROM ledger_transactions ORDER BY timestamp DESC")
    fun queryAll(): List<LedgerTransactionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(tx: LedgerTransactionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertSync(tx: LedgerTransactionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(txs: List<LedgerTransactionEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAllSync(txs: List<LedgerTransactionEntity>)

    @Update
    suspend fun update(tx: LedgerTransactionEntity)

    @Update
    fun updateSync(tx: LedgerTransactionEntity)

    @Query("DELETE FROM ledger_transactions WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM ledger_transactions WHERE id = :id")
    fun deleteByIdSync(id: String)

    @Query("DELETE FROM ledger_transactions WHERE invoiceId = :invoiceId")
    suspend fun deleteByInvoiceId(invoiceId: String)

    @Query("DELETE FROM ledger_transactions WHERE invoiceId = :invoiceId")
    fun deleteByInvoiceIdSync(invoiceId: String)

    @Query("DELETE FROM ledger_transactions")
    suspend fun clear()
}
