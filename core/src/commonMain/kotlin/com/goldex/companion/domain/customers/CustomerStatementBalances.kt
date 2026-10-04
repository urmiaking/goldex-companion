package com.goldex.companion.domain.customers

import com.goldex.companion.model.Customer
import com.goldex.companion.model.LedgerTransaction

/** Reconstruct all historical balances before filtering or lazy rendering. */
fun customerStatementBalances(customer: Customer, transactions: List<LedgerTransaction>): Map<String, Pair<Double, Long>> {
    val sorted = transactions.sortedWith(compareByDescending<LedgerTransaction> { it.timestamp }.thenByDescending { it.dateTime })
    var gold = customer.goldDebtGrams
    var cash = customer.cashDebtTomans
    return buildMap {
        for (transaction in sorted) {
            put(transaction.id, gold to cash)
            val effect = transaction.balanceEffect()
            gold -= effect.goldGrams
            cash -= effect.cashTomans
        }
    }
}
