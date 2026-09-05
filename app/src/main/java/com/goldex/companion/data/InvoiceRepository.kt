package com.goldex.companion.data

import android.content.Context
import android.content.SharedPreferences
import com.goldex.companion.model.Invoice

class InvoiceRepository(context: Context) : InvoiceStore {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("qirat_invoices_prefs", Context.MODE_PRIVATE)

    override fun getInvoices(): List<Invoice> {
        val json = prefs.getString("invoices_json", null) ?: return emptyList()
        return PersistenceJsonCodecs.decodeInvoices(json)
    }

    override fun saveInvoice(invoice: Invoice) {
        val list = getInvoices().toMutableList()
        val index = list.indexOfFirst { it.id == invoice.id }
        if (index >= 0) {
            list[index] = invoice
        } else {
            list.add(0, invoice)
        }
        persist(list)
    }

    override fun deleteInvoice(id: String) {
        val list = getInvoices().filterNot { it.id == id }
        persist(list)
    }

    private fun persist(list: List<Invoice>) {
        prefs.edit().putString("invoices_json", PersistenceJsonCodecs.encodeInvoices(list)).apply()
    }
}
