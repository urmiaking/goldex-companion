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

class RoomCustomerRepository(
    private val customerDao: CustomerDao,
    private val ledgerDao: LedgerTransactionDao
) : CustomerStore {

    override fun getCustomers(): List<Customer> {
        return customerDao.queryAll().map { it.toDomain() }
    }

    override fun addCustomer(customer: Customer) {
        customerDao.insertSync(customer.toEntity())
    }

    override fun updateCustomer(customer: Customer) {
        customerDao.updateSync(customer.toEntity())
    }

    override fun deleteCustomer(id: String) {
        customerDao.deleteByIdSync(id)
    }

    override fun getTransactions(customerId: String): List<LedgerTransaction> {
        return ledgerDao.queryByCustomerId(customerId).map { it.toDomain() }
    }

    override fun addTransaction(transaction: LedgerTransaction) {
        ledgerDao.insertSync(transaction.toEntity())
    }

    override fun updateTransaction(transaction: LedgerTransaction) {
        ledgerDao.updateSync(transaction.toEntity())
    }

    override fun deleteTransaction(id: String) {
        ledgerDao.deleteByIdSync(id)
    }

    override fun getTransactionsByInvoiceId(invoiceId: String): List<LedgerTransaction> {
        return ledgerDao.queryByInvoiceId(invoiceId).map { it.toDomain() }
    }

    override fun deleteTransactionsByInvoiceId(invoiceId: String) {
        ledgerDao.deleteByInvoiceIdSync(invoiceId)
    }

    // Reactive Flow extensions for modern and multiplatform consumers
    fun observeCustomers(): Flow<List<Customer>> {
        return customerDao.observeAll().map { list -> list.map { it.toDomain() } }
    }

    fun observeTransactions(customerId: String): Flow<List<LedgerTransaction>> {
        return ledgerDao.observeByCustomerId(customerId).map { list -> list.map { it.toDomain() } }
    }
}
