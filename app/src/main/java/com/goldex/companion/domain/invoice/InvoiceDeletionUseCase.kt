package com.goldex.companion.domain.invoice

import com.goldex.companion.data.CustomerStore
import com.goldex.companion.data.InvoiceStore
import com.goldex.companion.model.BarterInvoice
import com.goldex.companion.model.Customer
import com.goldex.companion.model.LedgerTransaction
import com.goldex.companion.model.SettlementMethod

sealed interface InvoiceDeletionResult {
    data class Deleted(val invoice: BarterInvoice, val remainingInvoices: List<BarterInvoice>) : InvoiceDeletionResult
    data object NotFound : InvoiceDeletionResult
    data object MissingLedgerStore : InvoiceDeletionResult
    data object MissingCustomer : InvoiceDeletionResult
    data object ReferencedByTransfer : InvoiceDeletionResult
    data class Failed(val rollbackSucceeded: Boolean) : InvoiceDeletionResult
}

/** Reverses recorded entries, including settlements, without regenerating invoice calculations. */
class InvoiceDeletionUseCase(
    private val invoices: InvoiceStore,
    private val customers: CustomerStore?
) {
    fun delete(invoiceId: String): InvoiceDeletionResult {
        var invoice: BarterInvoice? = null
        var transactions = emptyList<LedgerTransaction>()
        val originals = mutableListOf<Customer>()
        var transactionsDeleteAttempted = false
        var invoiceDeleteAttempted = false
        try {
            val saved = invoices.getBarterInvoices()
            invoice = saved.firstOrNull { it.id == invoiceId } ?: return InvoiceDeletionResult.NotFound
            if (saved.any {
                    it.id != invoiceId && it.settlementMethod == SettlementMethod.TRANSFER &&
                        it.thirdPartyInvoiceId == invoiceId
                }) {
                return InvoiceDeletionResult.ReferencedByTransfer
            }
            // Even a currently unsynced invoice can still have entries from an earlier save.
            val ledger = customers ?: return InvoiceDeletionResult.MissingLedgerStore
            transactions = ledger.getTransactionsByInvoiceId(invoiceId)
            val currentCustomers = ledger.getCustomers().associateBy { it.id }
            val groups = transactions.groupBy { it.customerId }
            if (groups.keys.any { it !in currentCustomers }) return InvoiceDeletionResult.MissingCustomer
            val updates = groups.map { (customerId, entries) ->
                val current = currentCustomers.getValue(customerId)
                current to InvoiceLedgerSyncUseCase.reverseLedgerSync(entries, current)
            }
            updates.forEach { (original, updated) ->
                originals.add(original)
                ledger.updateCustomer(updated)
            }
            transactionsDeleteAttempted = transactions.isNotEmpty()
            if (transactionsDeleteAttempted) ledger.deleteTransactionsByInvoiceId(invoiceId)
            invoiceDeleteAttempted = true
            invoices.deleteBarterInvoice(invoiceId)
            return InvoiceDeletionResult.Deleted(invoice, saved.filterNot { it.id == invoiceId })
        } catch (_: Exception) {
            // SharedPreferences stores have no cross-store transaction. Compensate for reported
            // write failures using the pre-delete snapshots, including writes that failed after mutation.
            var restored = true
            fun restore(action: () -> Unit) {
                try { action() } catch (_: Exception) { restored = false }
            }
            if (invoiceDeleteAttempted) invoice?.let { original -> restore { invoices.saveBarterInvoice(original) } }
            if (transactionsDeleteAttempted) customers?.let { ledger ->
                restore {
                    val presentIds = ledger.getTransactionsByInvoiceId(invoiceId).map { it.id }.toSet()
                    transactions.filterNot { it.id in presentIds }.asReversed().forEach(ledger::addTransaction)
                }
            }
            originals.asReversed().forEach { original -> restore { customers?.updateCustomer(original) } }
            return InvoiceDeletionResult.Failed(rollbackSucceeded = restored)
        }
    }
}
