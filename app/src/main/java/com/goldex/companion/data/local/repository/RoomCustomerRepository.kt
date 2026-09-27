package com.goldex.companion.data.local.repository

import com.goldex.companion.data.CustomerStore
import com.goldex.companion.data.local.db.dao.CustomerDao
import com.goldex.companion.data.local.db.dao.LedgerTransactionDao
import com.goldex.companion.data.local.db.mappers.toDomain
import com.goldex.companion.data.local.db.mappers.toEntity
import com.goldex.companion.model.Customer
import com.goldex.companion.model.LedgerTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import com.goldex.companion.data.sync.*

class RoomCustomerRepository(
    private val customerDao: CustomerDao,
    private val ledgerDao: LedgerTransactionDao,
    private val sync: RoomSyncUnitOfWork? = null
) : CustomerStore {

    override fun getCustomers(): List<Customer> {
        return customerDao.queryAll().map { it.toDomain() }
    }

    override fun addCustomer(customer: Customer) {
        write { customerDao.insertSync(customer.toEntity()); sync?.changed("customer", customer.id, SyncJson.record(customer)) }
    }

    override fun updateCustomer(customer: Customer) {
        write { customerDao.updateSync(customer.toEntity()); sync?.changed("customer", customer.id, SyncJson.record(customer)) }
    }

    override fun deleteCustomer(id: String) {
        write { customerDao.deleteByIdSync(id); sync?.changed("customer", id, null) }
    }

    override fun getTransactions(customerId: String): List<LedgerTransaction> {
        return ledgerDao.queryByCustomerId(customerId).map { it.toDomain() }
    }

    override fun addTransaction(transaction: LedgerTransaction) {
        write { ledgerDao.insertSync(transaction.toEntity()); sync?.changed("ledger", transaction.id, SyncJson.record(transaction)) }
    }

    override fun updateTransaction(transaction: LedgerTransaction) {
        write { ledgerDao.updateSync(transaction.toEntity()); sync?.changed("ledger", transaction.id, SyncJson.record(transaction)) }
    }

    override fun deleteTransaction(id: String) {
        write { ledgerDao.deleteByIdSync(id); sync?.changed("ledger", id, null) }
    }

    override fun getTransactionsByInvoiceId(invoiceId: String): List<LedgerTransaction> {
        return ledgerDao.queryByInvoiceId(invoiceId).map { it.toDomain() }
    }

    override fun deleteTransactionsByInvoiceId(invoiceId: String) {
        write {
            val ids = ledgerDao.queryByInvoiceId(invoiceId).map { it.id }
            ledgerDao.deleteByInvoiceIdSync(invoiceId)
            ids.forEach { sync?.changed("ledger", it, null) }
        }
    }
    private fun write(action: () -> Unit) { if (sync == null) action() else sync.transaction(action) }

    // Reactive Flow extensions for modern and multiplatform consumers
    fun observeCustomers(): Flow<List<Customer>> {
        return customerDao.observeAll().map { list -> list.map { it.toDomain() } }
    }

    fun observeTransactions(customerId: String): Flow<List<LedgerTransaction>> {
        return ledgerDao.observeByCustomerId(customerId).map { list -> list.map { it.toDomain() } }
    }
}
