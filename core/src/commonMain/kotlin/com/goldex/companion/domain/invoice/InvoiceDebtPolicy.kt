package com.goldex.companion.domain.invoice

import com.goldex.companion.domain.customers.OutstandingBalance
import com.goldex.companion.domain.customers.LedgerEffect
import com.goldex.companion.model.*
import com.goldex.companion.platform.SettlementArithmetic
import kotlin.math.abs

/** The contract converts the net invoice value once, including charges and agreed barter values. */
object InvoiceDebtPolicy {
    data class PaymentEffects(val account: LedgerEffect, val invoice: LedgerEffect)

    fun isPhysicalGold(payment: SettlementPaymentItem): Boolean =
        payment.method == SettlementMethod.BULLION || (payment.method == SettlementMethod.TRANSFER && payment.amountTomans == 0L && payment.goldWeight18k > 0)

    fun principalTomans(invoice: BarterInvoice): Long {
        val value = invoice.balance.netPayableAmount
        require(value.isFinite()) { "مبلغ فاکتور معتبر نیست" }
        return SettlementArithmetic.goldToTomans(abs(value), 1) * if (value < 0) -1 else 1
    }

    fun principalGold(invoice: BarterInvoice): Double =
        if (invoice.debtBasis == InvoiceDebtBasis.GOLD) {
            val amount = principalTomans(invoice)
            if (amount == 0L) 0.0 else SettlementArithmetic.tomansToGold(abs(amount), invoice.spotPrice18k) * if (amount < 0) -1 else 1
        } else invoice.balance.net18kWeightDelta

    fun paymentGold(invoice: BarterInvoice, payment: SettlementPaymentItem): Double {
        payment.settlement?.let { return abs(it.invoiceGoldDeltaGrams ?: it.goldDeltaGrams) }
        if (payment.method == SettlementMethod.LEDGER) return 0.0
        return if (payment.method == SettlementMethod.BULLION || (payment.method == SettlementMethod.TRANSFER && payment.amountTomans == 0L)) payment.goldWeight18k
        else SettlementArithmetic.tomansToGold(payment.amountTomans, invoice.spotPrice18k)
    }

    fun paymentCash(invoice: BarterInvoice, payment: SettlementPaymentItem): Long {
        payment.settlement?.let { return abs(it.invoiceCashDeltaTomans ?: it.cashDeltaTomans) }
        if (payment.method == SettlementMethod.LEDGER) return 0L
        return if (payment.amountTomans > 0 || payment.goldWeight18k == 0.0) payment.amountTomans
        else SettlementArithmetic.goldToTomans(payment.goldWeight18k, invoice.spotPrice18k)
    }

    fun outstanding(invoice: BarterInvoice): OutstandingBalance {
        val goldBasis = invoice.isGoldDebt
        val principal = if (goldBasis) principalGold(invoice) else principalTomans(invoice).toDouble()
        var gold = if (goldBasis) principal else 0.0
        var cash = if (goldBasis) 0L else principalTomans(invoice)
        InvoiceLedgerSyncUseCase.getEffectivePayments(invoice).forEach { p ->
            if (p.settlement != null) {
                gold += p.settlement.invoiceGoldDeltaGrams ?: if (goldBasis) p.settlement.goldDeltaGrams else 0.0
                val delta = p.settlement.invoiceCashDeltaTomans ?: if (!goldBasis) p.settlement.cashDeltaTomans else 0L
                cash = checkedAdd(cash, delta)
            } else {
                val effect = paymentEffects(invoice, p, OutstandingBalance(gold, cash)).invoice
                gold += effect.goldGrams
                cash = checkedAdd(cash, effect.cashTomans)
            }
        }
        require(gold.isFinite()) { "وزن از محدودهٔ مجاز بیشتر است" }
        return OutstandingBalance(if (abs(gold) < 1e-10) 0.0 else gold, cash)
    }

    /** Allocate to this invoice only up to its debt; actual excess remains customer credit. */
    fun paymentEffects(invoice: BarterInvoice, payment: SettlementPaymentItem, scope: OutstandingBalance): PaymentEffects {
        if (payment.method == SettlementMethod.LEDGER) return PaymentEffects(LedgerEffect(), LedgerEffect())
        val sign = if (principalTomans(invoice) < 0) -1 else 1
        val physicalGold = isPhysicalGold(payment)
        val weight = payment.goldWeight18k
        val value = paymentCash(invoice, payment)
        if (invoice.isGoldDebt) {
            val applied = if (physicalGold) minOf(weight, abs(scope.goldGrams)) else {
                val capacity = SettlementArithmetic.goldToTomans(abs(scope.goldGrams), invoice.spotPrice18k)
                if (value >= capacity) abs(scope.goldGrams) else SettlementArithmetic.tomansToGold(value, invoice.spotPrice18k)
            }
            val paidCash = if (physicalGold) 0L else minOf(value, SettlementArithmetic.goldToTomans(abs(scope.goldGrams), invoice.spotPrice18k))
            return PaymentEffects(
                LedgerEffect(-sign * if (physicalGold) weight else applied, if (physicalGold) 0L else -sign * (value - paidCash)),
                LedgerEffect(goldGrams = -sign * applied)
            )
        }
        val applied = minOf(value, abs(scope.cashTomans))
        val excessGold = if (!physicalGold || value <= abs(scope.cashTomans)) 0.0
            else (weight - SettlementArithmetic.tomansToGold(applied, invoice.spotPrice18k)).coerceAtLeast(0.0)
        return PaymentEffects(
            LedgerEffect(-sign * excessGold, -sign * if (physicalGold) applied else value),
            LedgerEffect(cashTomans = -sign * applied)
        )
    }

    private fun checkedAdd(a: Long, b: Long): Long {
        val result = a + b
        require((b >= 0 && result >= a) || (b < 0 && result < a)) { "مبلغ از محدودهٔ مجاز بیشتر است" }
        return result
    }
}
