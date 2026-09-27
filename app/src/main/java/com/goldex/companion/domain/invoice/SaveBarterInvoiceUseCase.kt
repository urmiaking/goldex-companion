package com.goldex.companion.domain.invoice

import com.goldex.companion.data.CustomerStore
import com.goldex.companion.data.InvoiceStore
import com.goldex.companion.data.sync.SyncUnitOfWork
import com.goldex.companion.model.BarterInvoice

class SaveBarterInvoiceUseCase(private val invoices: InvoiceStore, private val customers: CustomerStore?, private val unit: SyncUnitOfWork? = null) {
    fun save(invoice: BarterInvoice) {
        val action = {
            invoices.saveBarterInvoice(invoice)
            val target = invoice.customer
            if (target != null && customers != null) {
                val original = customers.getCustomers().firstOrNull { it.id == target.id }
                    ?: error("Invoice customer is missing")
                val entries = customers.getTransactionsByInvoiceId(invoice.id)
                val reversed = if (entries.isNotEmpty()) InvoiceLedgerSyncUseCase.reverseLedgerSync(entries, original) else original
                if (entries.isNotEmpty()) customers.deleteTransactionsByInvoiceId(invoice.id)
                if (invoice.syncWithLedger) {
                    val result = InvoiceLedgerSyncUseCase.generateLedgerSync(invoice, reversed)
                    result.transactionsToCreate.forEach(customers::addTransaction)
                    customers.updateCustomer(result.updatedCustomer)
                } else if (entries.isNotEmpty()) customers.updateCustomer(reversed)
            }
        }
        if (unit == null) action() else unit.transaction(action)
    }
}
