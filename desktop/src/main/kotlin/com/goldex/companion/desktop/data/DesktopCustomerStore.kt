package com.goldex.companion.desktop.data

import com.goldex.companion.data.*
import com.goldex.companion.data.sync.SyncUnitOfWork
import com.goldex.companion.model.*
import org.json.JSONArray
import org.json.JSONObject
import java.math.BigDecimal

/** Uses the Android-compatible codecs within the desktop's locked, atomic backup document. */
class DesktopCustomerStore(private val storage: DesktopDataStore) : CustomerStore, SyncUnitOfWork {
    override fun <T> transaction(action: () -> T): T = storage.financialTransaction(action)
    override fun getCustomers() = PersistenceJsonCodecs.decodeCustomers(storage.records("customers").toString())
    override fun addCustomer(customer: Customer) = transaction {
        require(getCustomers().none { it.id == customer.id }) { "شناسه طرف‌حساب تکراری است" }
        saveRecord("customers", JSONObject(PersistenceJsonCodecs.encodeCustomerString(customer)), false)
        storage.database.addCustomer(customer)
    }
    override fun updateCustomer(customer: Customer) = transaction {
        saveRecord("customers", JSONObject(PersistenceJsonCodecs.encodeCustomerString(customer)), true)
        storage.database.updateCustomer(customer)
    }
    override fun deleteCustomer(id: String) = transaction {
        val customer = getCustomers().firstOrNull { it.id == id } ?: return@transaction
        require(kotlin.math.abs(customer.goldDebtGrams) <= 1e-10 && customer.cashDebtTomans == 0L) {
            "پیش از حذف طرف‌حساب، مانده حساب را تسویه کنید"
        }
        saveRecord("deletedCustomers", storage.records("customers").let { records ->
            (0 until records.length()).map { records.getJSONObject(it) }.first { it.getString("id") == id }
        }, false)
        // Receipts remain audit records; deleting a settled contact never deletes its history.
        removeRecord("customers", id)
        storage.database.deleteCustomer(id)
    }
    private fun allTransactions() = PersistenceJsonCodecs.decodeLedgerTransactions(storage.records("ledgerTransactions").toString())
    override fun getTransactions(customerId: String) = allTransactions().filter { it.customerId == customerId }
        .sortedWith(compareByDescending<LedgerTransaction> { it.timestamp }.thenByDescending { it.dateTime })
    override fun addTransaction(transaction: LedgerTransaction) = transaction {
        require(allTransactions().none { it.id == transaction.id }) { "شناسه سند تکراری است" }
        require(getCustomers().any { it.id == transaction.customerId }) { "طرف‌حساب یافت نشد" }
        saveRecord("ledgerTransactions", JSONArray(PersistenceJsonCodecs.encodeLedgerTransactions(listOf(transaction))).getJSONObject(0), false)
        storage.database.addTransaction(transaction)
    }
    override fun updateTransaction(transaction: LedgerTransaction) = transaction {
        val old = requireNotNull(allTransactions().firstOrNull { it.id == transaction.id }) { "سند یافت نشد" }
        require(old.customerId == transaction.customerId) { "طرف‌حساب سند قابل تغییر نیست" }
        saveRecord("ledgerTransactions", JSONArray(PersistenceJsonCodecs.encodeLedgerTransactions(listOf(transaction))).getJSONObject(0), true)
        storage.database.updateTransaction(transaction)
    }
    override fun deleteTransaction(id: String): Unit {
        transaction {
            removeRecord("ledgerTransactions", id)
            storage.database.deleteTransaction(id)
        }
    }
    override fun getTransactionsByInvoiceId(invoiceId: String) = allTransactions().filter { it.invoiceId == invoiceId }
    override fun getTransactionsByInvoiceIds(invoiceIds: List<String>): List<LedgerTransaction> {
        val selected = invoiceIds.toSet()
        return allTransactions().filter { it.invoiceId in selected }
    }
    override fun deleteTransactionsByInvoiceId(invoiceId: String): Unit {
        transaction {
            val records = storage.records("ledgerTransactions")
            storage.replaceRecords("ledgerTransactions", JSONArray((0 until records.length())
                .map { records.getJSONObject(it) }.filter { it.optString("invoiceId") != invoiceId }))
            storage.database.deleteTransactionsByInvoiceId(invoiceId)
        }
    }

    internal fun saveRecord(key: String, encoded: JSONObject, mustExist: Boolean) {
        val records = storage.records(key)
        val index = (0 until records.length()).firstOrNull { records.getJSONObject(it).getString("id") == encoded.getString("id") }
        require(!mustExist || index != null) { "رکورد یافت نشد" }
        if (index == null) records.put(encoded) else mergeRecord(records.getJSONObject(index), encoded)
        storage.replaceRecords(key, records)
    }
    internal fun removeRecord(key: String, id: String) {
        val records = storage.records(key)
        storage.replaceRecords(key, JSONArray((0 until records.length()).map { records.getJSONObject(it) }.filter { it.getString("id") != id }))
    }
}

/** Real persisted invoice links; no demonstration invoices participate in account balances. */
class DesktopCustomerInvoiceStore(private val storage: DesktopDataStore, private val customers: DesktopCustomerStore) : InvoiceStore {
    override fun getInvoices() = PersistenceJsonCodecs.decodeInvoices(storage.records("invoices").toString())
    override fun getBarterInvoices() = PersistenceJsonCodecs.decodeBarterInvoices(storage.records("barterInvoices").toString())
    override fun saveInvoice(invoice: Invoice): Unit {
        customers.transaction {
            customers.saveRecord("invoices", JSONArray(PersistenceJsonCodecs.encodeInvoices(listOf(invoice))).getJSONObject(0), false)
            storage.database.saveInvoice(invoice)
        }
    }
    override fun saveBarterInvoice(invoice: BarterInvoice): Unit {
        customers.transaction {
            customers.saveRecord("barterInvoices", JSONArray(PersistenceJsonCodecs.encodeBarterInvoices(listOf(invoice))).getJSONObject(0), false)
            storage.database.saveBarterInvoice(invoice)
        }
    }
    override fun deleteInvoice(id: String): Unit {
        customers.transaction {
            customers.removeRecord("invoices", id)
            storage.database.deleteInvoice(id)
        }
    }
    override fun deleteBarterInvoice(id: String): Unit {
        customers.transaction {
            customers.removeRecord("barterInvoices", id)
            storage.database.deleteBarterInvoice(id)
        }
    }
}

private fun mergeRecord(target: JSONObject, source: JSONObject) {
    source.keySet().forEach { key ->
        val old = target.optJSONObject(key)
        val fresh = source.optJSONObject(key)
        if (old != null && fresh != null) mergeRecord(old, fresh)
        else if (target.optJSONArray(key) != null && source.optJSONArray(key) != null) {
            val previous = target.getJSONArray(key)
            val replacement = source.getJSONArray(key)
            // Retain future fields on surviving nested payments/items while honoring removed rows.
            val byId = (0 until previous.length()).mapNotNull { previous.optJSONObject(it) }
                .filter { it.has("id") }.associateBy { it.getString("id") }
            for (i in 0 until replacement.length()) {
                val encoded = replacement.optJSONObject(i) ?: continue
                val retained = byId[encoded.optString("id")] ?: continue
                mergeRecord(retained, encoded)
                replacement.put(i, retained)
            }
            target.put(key, replacement)
        } else target.put(key, source.get(key))
    }
}

/** Reject corruption before codecs can apply defaults or omit malformed financial records. */
internal object DesktopCustomerDocuments {
    private fun records(document: JSONObject, key: String): JSONArray = if (document.has(key)) document.getJSONArray(key) else JSONArray()
    private fun identities(records: JSONArray) {
        require(records.length() <= 100_000) { "تعداد رکوردها از سقف مجاز بیشتر است" }
        val ids = mutableSetOf<String>()
        for (i in 0 until records.length()) require(records.getJSONObject(i).getString("id").let { it.isNotBlank() && ids.add(it) })
    }
    private fun whole(record: JSONObject, key: String, nonnegative: Boolean = false) {
        if (!record.has(key)) return
        val raw = record.get(key)
        require(raw is Number || (raw is String && raw.matches(Regex("^-?\\d+$")))) { "Invalid whole number for $key: $raw" }
        val number = BigDecimal(raw.toString()).longValueExact()
        require(number != Long.MIN_VALUE && (!nonnegative || number >= 0))
    }
    private fun decimal(record: JSONObject, key: String, nonnegative: Boolean = false) {
        if (!record.has(key)) return
        val raw = record.get(key)
        val d = when (raw) {
            is Number -> raw.toDouble()
            is String -> raw.toDoubleOrNull() ?: Double.NaN
            else -> Double.NaN
        }
        require(d.isFinite() && (!nonnegative || d >= 0)) { "Invalid decimal for $key: $raw" }
    }
    fun validate(document: JSONObject) {
        for (collection in listOf("customers", "deletedCustomers")) {
            val customers = records(document, collection)
            identities(customers)
            for (i in 0 until customers.length()) {
                val record = customers.getJSONObject(i)
                require(record.getString("name").isNotBlank())
                decimal(record, "goldDebtGrams")
                whole(record, "cashDebtTomans")
                whole(record, "createdAt", true)
                if (record.has("isVerified")) require(record.get("isVerified") is Boolean)
            }
            require(PersistenceJsonCodecs.decodeCustomers(customers.toString()).size == customers.length())
        }
        val entries = records(document, "ledgerTransactions")
        identities(entries)
        for (i in 0 until entries.length()) {
            val record = entries.getJSONObject(i)
            require(record.getString("customerId").isNotBlank())
            require(record.getString("type") in LedgerEntryType.values().map { it.name })
            require(record.getString("direction") in LedgerDirection.values().map { it.name })
            for (key in listOf("scaleWeightGrams", "equivalent750WeightGrams")) decimal(record, key, true)
            decimal(record, "resultingGoldBalance")
            whole(record, "amountTomans", true)
            whole(record, "resultingCashBalance")
            whole(record, "timestamp", true)
            if (record.has("karat")) require(BigDecimal(record.get("karat").toString()).intValueExact() in 1..1000)
            if (record.has("settlement")) PersistenceJsonCodecs.decodeLedgerSettlement(record.getJSONObject("settlement").toString())
        }
        require(PersistenceJsonCodecs.decodeLedgerTransactions(entries.toString()).size == entries.length())
        val invoices = records(document, "invoices")
        identities(invoices)
        require(PersistenceJsonCodecs.decodeInvoices(invoices.toString()).size == invoices.length())
        val barter = records(document, "barterInvoices")
        identities(barter)
        require(PersistenceJsonCodecs.decodeBarterInvoices(barter.toString()).size == barter.length())
    }
}
