package com.goldex.companion.data

import android.content.Context
import android.content.SharedPreferences
import com.goldex.companion.model.Customer
import com.goldex.companion.model.LedgerTransaction

class CustomerRepository(context: Context) : CustomerStore {
    private val prefs: SharedPreferences = context.getSharedPreferences("goldex_customers_prefs", Context.MODE_PRIVATE)
    private val txPrefs: SharedPreferences = context.getSharedPreferences("goldex_ledger_transactions_prefs", Context.MODE_PRIVATE)

    override fun getCustomers(): List<Customer> {
        val json = prefs.getString("customers_json", null)
        if (json.isNullOrBlank()) {
            return emptyList()
        }
        return PersistenceJsonCodecs.decodeCustomers(json)
    }

    override fun addCustomer(customer: Customer) {
        val list = getCustomers().toMutableList()
        list.add(0, customer)
        saveCustomers(list)
    }

    override fun updateCustomer(customer: Customer) {
        val list = getCustomers().map { if (it.id == customer.id) customer else it }
        saveCustomers(list)
    }

    override fun deleteCustomer(id: String) {
        val list = getCustomers().filter { it.id != id }
        saveCustomers(list)
    }

    override fun getTransactions(customerId: String): List<LedgerTransaction> {
        val json = txPrefs.getString("transactions_json", null)
        if (json.isNullOrBlank()) {
            return emptyList()
        }
        val all = PersistenceJsonCodecs.decodeLedgerTransactions(json)
        return all.filter { it.customerId == customerId }
    }

    override fun addTransaction(transaction: LedgerTransaction) {
        val json = txPrefs.getString("transactions_json", null)
        val all = (PersistenceJsonCodecs.decodeLedgerTransactions(json)).toMutableList()
        all.add(0, transaction)
        txPrefs.edit().putString("transactions_json", PersistenceJsonCodecs.encodeLedgerTransactions(all)).apply()
    }

    override fun updateTransaction(transaction: LedgerTransaction) {
        val json = txPrefs.getString("transactions_json", null)
        val all = PersistenceJsonCodecs.decodeLedgerTransactions(json).map {
            if (it.id == transaction.id) transaction else it
        }
        txPrefs.edit().putString("transactions_json", PersistenceJsonCodecs.encodeLedgerTransactions(all)).apply()
    }

    override fun deleteTransaction(id: String) {
        val json = txPrefs.getString("transactions_json", null)
        val all = PersistenceJsonCodecs.decodeLedgerTransactions(json).filter { it.id != id }
        txPrefs.edit().putString("transactions_json", PersistenceJsonCodecs.encodeLedgerTransactions(all)).apply()
    }

    override fun getTransactionsByInvoiceId(invoiceId: String): List<LedgerTransaction> {
        val json = txPrefs.getString("transactions_json", null)
        if (json.isNullOrBlank()) {
            return emptyList()
        }
        val all = PersistenceJsonCodecs.decodeLedgerTransactions(json)
        return all.filter { it.invoiceId == invoiceId }
    }

    override fun deleteTransactionsByInvoiceId(invoiceId: String) {
        val json = txPrefs.getString("transactions_json", null)
        if (json.isNullOrBlank()) return
        val all = PersistenceJsonCodecs.decodeLedgerTransactions(json).filter { it.invoiceId != invoiceId }
        txPrefs.edit().putString("transactions_json", PersistenceJsonCodecs.encodeLedgerTransactions(all)).apply()
    }

    private fun saveCustomers(customers: List<Customer>) {
        prefs.edit().putString("customers_json", PersistenceJsonCodecs.encodeCustomers(customers)).apply()
    }
}

