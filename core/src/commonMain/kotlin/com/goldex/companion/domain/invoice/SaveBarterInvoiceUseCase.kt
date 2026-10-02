package com.goldex.companion.domain.invoice

import com.goldex.companion.data.CustomerStore
import com.goldex.companion.data.InvoiceStore
import com.goldex.companion.data.sync.SyncUnitOfWork
import com.goldex.companion.model.BarterInvoice

class SaveBarterInvoiceUseCase(private val invoices: InvoiceStore, private val customers: CustomerStore?, private val unit: SyncUnitOfWork? = null) {
    fun save(invoice: BarterInvoice) {
        val action = {
            val previousSettlements = customers?.getTransactionsByInvoiceId(invoice.id).orEmpty().filter { it.settlement != null }
            require(previousSettlements.isEmpty() || (invoice.syncWithLedger && previousSettlements.all { it.customerId == invoice.customer?.id })) {
                "فاکتور دارای تسویه است؛ اتصال آن به مشتری و دفتر باید حفظ شود"
            }
            invoices.saveBarterInvoice(invoice)
            val target = invoice.customer
            if (target != null && customers != null) {
                val original = customers.getCustomers().firstOrNull { it.id == target.id }
                    ?: error("Invoice customer is missing")
                val allEntries = customers.getTransactionsByInvoiceId(invoice.id)
                val settlements = allEntries.filter { it.settlement != null }
                require(settlements.isEmpty() || (invoice.syncWithLedger && settlements.all { it.customerId == target.id })) {
                    "فاکتور دارای تسویه است؛ اتصال آن به مشتری و دفتر باید حفظ شود"
                }
                val entries = allEntries.filter { it.settlement == null }
                val reversed = if (entries.isNotEmpty()) InvoiceLedgerSyncUseCase.reverseLedgerSync(entries, original) else original
                if (allEntries.isNotEmpty()) customers.deleteTransactionsByInvoiceId(invoice.id)
                settlements.forEach(customers::addTransaction)
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
