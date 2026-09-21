package com.goldex.companion.data

import android.content.Context
import android.content.SharedPreferences
import com.goldex.companion.model.BarterInvoice
import com.goldex.companion.model.Invoice

class InvoiceRepository(context: Context) : InvoiceStore {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("qirat_invoices_prefs", Context.MODE_PRIVATE)
    private val barterPrefs: SharedPreferences =
        context.getSharedPreferences("qirat_barter_invoices_prefs", Context.MODE_PRIVATE)

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

    override fun getBarterInvoices(): List<BarterInvoice> {
        val json = barterPrefs.getString("barter_invoices_json", null) ?: return emptyList()
        return PersistenceJsonCodecs.decodeBarterInvoices(json)
    }

    override fun saveBarterInvoice(invoice: BarterInvoice) {
        val list = getBarterInvoices().toMutableList()
        val index = list.indexOfFirst { it.id == invoice.id }
        if (index >= 0) {
            list[index] = invoice
        } else {
            list.add(0, invoice)
        }
        persistBarter(list)
    }

    override fun deleteBarterInvoice(id: String) {
        val list = getBarterInvoices().filterNot { it.id == id }
        persistBarter(list)
    }

    private fun persist(list: List<Invoice>) {
        prefs.edit().putString("invoices_json", PersistenceJsonCodecs.encodeInvoices(list)).apply()
    }

    private fun persistBarter(list: List<BarterInvoice>) {
        barterPrefs.edit().putString("barter_invoices_json", PersistenceJsonCodecs.encodeBarterInvoices(list)).apply()
    }
}
