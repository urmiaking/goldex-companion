package com.goldex.companion.desktop.ui

import com.goldex.companion.domain.customers.balanceEffect
import com.goldex.companion.model.*
import java.awt.Desktop
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.net.URI

internal object DesktopCustomerActions {
    fun call(phone: String): Result<Unit> = runCatching {
        val number = PersianNumberFormatter.toEnglishDigits(phone).filter { it.isDigit() || it == '+' }
        require(number.isNotBlank())
        require(Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE))
        Desktop.getDesktop().browse(URI("tel", number, null))
    }

    fun statementText(customer: Customer, transactions: List<LedgerTransaction>, balances: Map<String, Pair<Double, Long>>) = buildString {
        appendLine("گردش حساب ${customer.name}")
        appendLine("مانده طلا: ${PersianNumberFormatter.formatWeight(customer.goldDebtGrams)} گرم")
        appendLine("مانده تومان: ${PersianNumberFormatter.formatPrice(customer.cashDebtTomans)} تومان")
        transactions.sortedWith(compareByDescending<LedgerTransaction> { it.timestamp }.thenByDescending { it.dateTime }).forEach { tx ->
            val effect = tx.balanceEffect()
            appendLine("${PersianNumberFormatter.toPersianDigits(tx.cleanDocumentNumber)} | ${tx.dateTime} | ${tx.title}")
            appendLine("${PersianNumberFormatter.formatWeight(effect.goldGrams)} گرم | ${PersianNumberFormatter.formatPrice(effect.cashTomans)} تومان")
            balances[tx.id]?.let { (gold, cash) ->
                appendLine("مانده: ${PersianNumberFormatter.formatWeight(gold)} گرم | ${PersianNumberFormatter.formatPrice(cash)} تومان")
            }
        }
    }

    fun share(customer: Customer, transactions: List<LedgerTransaction>, balances: Map<String, Pair<Double, Long>>): Result<Unit> = runCatching {
        Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(statementText(customer, transactions, balances)), null)
    }
}
