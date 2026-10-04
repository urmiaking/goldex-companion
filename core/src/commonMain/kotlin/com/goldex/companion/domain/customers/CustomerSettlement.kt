package com.goldex.companion.domain.customers

import com.goldex.companion.data.CustomerStore
import com.goldex.companion.data.InvoiceStore
import com.goldex.companion.data.sync.SyncUnitOfWork
import com.goldex.companion.model.*
import com.goldex.companion.platform.LocalCalendar
import com.goldex.companion.platform.RandomIdGenerator
import com.goldex.companion.platform.SettlementArithmetic
import com.goldex.companion.platform.SystemClock
import kotlin.math.abs
import kotlin.math.round

/** These are debt effects, not physical receipts. Older entries keep their original one-unit meaning. */
data class LedgerEffect(val goldGrams: Double = 0.0, val cashTomans: Long = 0L)

fun LedgerTransaction.balanceEffect(): LedgerEffect = settlement?.let {
    LedgerEffect(it.goldDeltaGrams, it.cashDeltaTomans)
} ?: run {
    val sign = if (direction == LedgerDirection.PAY) 1 else -1
    if (type == LedgerEntryType.GOLD_WEIGHT) LedgerEffect(goldGrams = sign * equivalent750WeightGrams)
    else LedgerEffect(cashTomans = sign * amountTomans)
}

fun Customer.applyEffect(effect: LedgerEffect, reverse: Boolean = false): Customer {
    val sign = if (reverse) -1 else 1
    val gold = goldDebtGrams + sign * effect.goldGrams
    require(gold.isFinite() && effect.cashTomans != Long.MIN_VALUE) { "مانده از محدودهٔ مجاز بیشتر است" }
    val delta = if (reverse) -effect.cashTomans else effect.cashTomans
    val cash = cashDebtTomans + delta
    require((delta >= 0 && cash >= cashDebtTomans) || (delta < 0 && cash < cashDebtTomans)) { "مبلغ از محدودهٔ مجاز بیشتر است" }
    return copy(goldDebtGrams = if (abs(gold) < 1e-10) 0.0 else gold, cashDebtTomans = cash)
}

data class OutstandingBalance(val goldGrams: Double, val cashTomans: Long) {
    val isSettled: Boolean get() = abs(goldGrams) < 1e-10 && cashTomans == 0L
}

/** A settlement's excess/offset belongs to the customer account, not a second debt on the invoice. */
fun invoiceOutstanding(entries: List<LedgerTransaction>): OutstandingBalance {
    var gold = 0.0
    var cash = 0L
    entries.forEach { tx ->
        val effect = tx.balanceEffect()
        val settlement = tx.settlement
        gold += settlement?.invoiceGoldDeltaGrams ?: if (settlement == null || settlement.targetType == LedgerEntryType.GOLD_WEIGHT) effect.goldGrams else 0.0
        val delta = settlement?.invoiceCashDeltaTomans ?: if (settlement == null || settlement.targetType == LedgerEntryType.CASH_RIAL) effect.cashTomans else 0L
        val nextCash = cash + delta
        require((delta >= 0 && nextCash >= cash) || (delta < 0 && nextCash < cash)) { "مبلغ از محدودهٔ مجاز بیشتر است" }
        cash = nextCash
    }
    require(gold.isFinite()) { "وزن از محدودهٔ مجاز بیشتر است" }
    return OutstandingBalance(if (abs(gold) < 1e-10) 0.0 else gold, cash)
}

data class SettlementRequest(
    val id: String = RandomIdGenerator.newId(),
    val customerId: String,
    val targetType: LedgerEntryType,
    val paymentType: LedgerEntryType,
    val amountTomans: Long = 0L,
    val scaleWeightGrams: Double = 0.0,
    val karat: Int = 750,
    val rateTomans: Long = 0L,
    val rateSource: String = "دلخواه",
    val rateObservedAt: String = "",
    val offsetExistingCredit: Boolean = false,
    val invoiceId: String? = null,
    val paymentMethod: String = "حواله بانکی / پایا",
    val trackingCode: String = "",
    val note: String = "",
    val documentNumber: String? = null
)

fun generateSettlementDocumentNumber(existingDocumentNumbers: Collection<String> = emptySet()): String {
    val used = existingDocumentNumbers.map { it.trim().removePrefix("IR-").removePrefix("IR") }.toSet()
    val maxNumeric = used.mapNotNull { it.toIntOrNull() }.filter { it in 1000..99999 }.maxOrNull()
    if (maxNumeric != null && (maxNumeric + 1).toString() !in used && (maxNumeric + 1) <= 99999) {
        return (maxNumeric + 1).toString()
    }
    repeat(100) {
        val candidate = (1000..9999).random().toString()
        if (candidate !in used) return candidate
    }
    repeat(100) {
        val candidate = (10000..99999).random().toString()
        if (candidate !in used) return candidate
    }
    return (1000..9999).random().toString()
}

data class SettlementPreview(
    val effect: LedgerEffect,
    val customerAfter: Customer,
    val equivalent750Grams: Double,
    val paymentValueTomans: Long,
    val goldAppliedGrams: Double,
    val cashAppliedTomans: Long,
    val isReceiving: Boolean,
    val invoiceEffect: LedgerEffect
)

object CustomerSettlementPolicy {
    fun preview(customer: Customer, request: SettlementRequest, scope: OutstandingBalance = OutstandingBalance(customer.goldDebtGrams, customer.cashDebtTomans)): SettlementPreview {
        require(customer.id == request.customerId) { "طرف حساب تغییر کرده است" }
        require(customer.cashDebtTomans != Long.MIN_VALUE && scope.cashTomans != Long.MIN_VALUE) { "مبلغ از محدودهٔ مجاز بیشتر است" }
        val goldTarget = request.targetType == LedgerEntryType.GOLD_WEIGHT
        val balance = if (goldTarget) scope.goldGrams else scope.cashTomans.toDouble()
        require(balance.isFinite() && abs(balance) > 1e-10) { "این مانده تسویه شده است" }
        val sign = if (balance > 0) 1 else -1
        val isGoldPayment = request.paymentType == LedgerEntryType.GOLD_WEIGHT
        val cross = request.targetType != request.paymentType
        require(!cross || request.rateTomans > 0) { "نرخ هر گرم طلای ۱۸ عیار را وارد کنید" }
        require(request.amountTomans >= 0 && request.scaleWeightGrams.isFinite()) { "مقدار پرداخت معتبر نیست" }
        if (isGoldPayment) {
            require(request.scaleWeightGrams > 0 && request.karat in 1..1000) { "وزن و عیار معتبر وارد کنید" }
            if (!request.offsetExistingCredit) require(abs(request.scaleWeightGrams * 1000 - round(request.scaleWeightGrams * 1000)) < 1e-7) {
                "وزن طلای پرداختی را با حداکثر سه رقم اعشار وارد کنید"
            }
        } else require(request.amountTomans > 0) { "مبلغ پرداخت را وارد کنید" }
        val weight = if (isGoldPayment) request.scaleWeightGrams * request.karat / 750.0 else 0.0
        require(weight.isFinite()) { "وزن از محدودهٔ مجاز بیشتر است" }
        val value = if (isGoldPayment && cross) SettlementArithmetic.goldToTomans(weight, request.rateTomans) else request.amountTomans
        require(!cross || (if (isGoldPayment) value > 0 else request.amountTomans > 0)) { "ارزش پرداخت کمتر از یک تومان است" }
        var goldDelta = 0.0
        var cashDelta = 0L
        var appliedGold = 0.0
        var appliedCash = 0L
        if (!cross) {
            if (goldTarget) { goldDelta = -sign * weight; appliedGold = weight }
            else { cashDelta = -sign * value; appliedCash = value }
        } else if (goldTarget) {
            val capacity = SettlementArithmetic.goldToTomans(abs(scope.goldGrams), request.rateTomans)
            require(capacity > 0) { "ارزش مانده کمتر از یک تومان است" }
            appliedCash = minOf(request.amountTomans, capacity)
            appliedGold = if (appliedCash == capacity) abs(scope.goldGrams) else SettlementArithmetic.tomansToGold(appliedCash, request.rateTomans)
            goldDelta = -sign * appliedGold
            cashDelta = -sign * (request.amountTomans - appliedCash)
        } else {
            appliedCash = minOf(value, abs(scope.cashTomans))
            appliedGold = if (value <= abs(scope.cashTomans)) weight else SettlementArithmetic.tomansToGold(appliedCash, request.rateTomans)
            cashDelta = -sign * appliedCash
            goldDelta = -sign * (weight - appliedGold).coerceAtLeast(0.0)
        }
        if (request.offsetExistingCredit) {
            require(cross) { "برای تهاتر، دو مانده باید واحد متفاوت داشته باشند" }
            if (isGoldPayment) {
                require(sign * customer.goldDebtGrams < 0 && weight <= abs(customer.goldDebtGrams) + 1e-10) { "وزن از بستانکاری طلایی بیشتر است" }
                goldDelta += sign * weight
            } else {
                require(sign * customer.cashDebtTomans < 0 && request.amountTomans <= abs(customer.cashDebtTomans)) { "مبلغ از بستانکاری نقدی بیشتر است" }
                cashDelta += sign * request.amountTomans
            }
        }
        val effect = LedgerEffect(goldDelta, cashDelta)
        var invoiceGold = if (goldTarget) -sign * minOf(appliedGold, abs(scope.goldGrams)) else 0.0
        var invoiceCash = if (!goldTarget) -sign * minOf(appliedCash, abs(scope.cashTomans)) else 0L
        if (request.offsetExistingCredit) {
            if (goldTarget && sign * scope.cashTomans < 0) invoiceCash += sign * minOf(appliedCash, abs(scope.cashTomans))
            if (!goldTarget && sign * scope.goldGrams < 0) invoiceGold += sign * minOf(appliedGold, abs(scope.goldGrams))
        }
        return SettlementPreview(effect, customer.applyEffect(effect), weight, value, appliedGold, appliedCash, sign > 0, LedgerEffect(invoiceGold, invoiceCash))
    }
}

/** Append-only, idempotent and atomic. Historical invoices and receipt rates are never repriced. */
class RecordCustomerSettlementUseCase(
    private val customers: CustomerStore,
    private val invoices: InvoiceStore,
    private val unit: SyncUnitOfWork
) {
    fun record(request: SettlementRequest, expected: Customer, expectedScope: OutstandingBalance): LedgerTransaction = unit.transaction {
        customers.getTransactions(request.customerId).firstOrNull { it.id == request.id }?.let { return@transaction it }
        val customer = customers.getCustomers().firstOrNull { it.id == request.customerId } ?: error("طرف حساب یافت نشد")
        require(customer.goldDebtGrams == expected.goldDebtGrams && customer.cashDebtTomans == expected.cashDebtTomans) { "ماندهٔ حساب تغییر کرده؛ فرم را ببندید و دوباره باز کنید" }
        val invoice = request.invoiceId?.let { id ->
            invoices.getBarterInvoices().firstOrNull { it.id == id }?.also {
                require(it.customer?.id == customer.id && it.syncWithLedger) { "فاکتور به دفتر این مشتری متصل نیست" }
            } ?: error("فاکتور یافت نشد")
        }
        val scope = if (invoice == null) OutstandingBalance(customer.goldDebtGrams, customer.cashDebtTomans)
            else invoiceOutstanding(customers.getTransactionsByInvoiceId(invoice.id))
        require(scope == expectedScope) { "ماندهٔ فاکتور تغییر کرده؛ فرم را دوباره باز کنید" }
        val preview = CustomerSettlementPolicy.preview(customer, request, scope)
        val now = SystemClock.nowMillis()
        val existingTx = customers.getTransactions(customer.id)
        val docNum = request.documentNumber?.trim()?.takeIf { it.isNotEmpty() }
            ?: generateSettlementDocumentNumber(existingTx.map { it.documentNumber })
        val tx = LedgerTransaction(
            id = request.id, customerId = customer.id, documentNumber = docNum,
            title = if (request.offsetExistingCredit) "تهاتر ماندهٔ طلا و تومان" else "تسویهٔ ${if (request.targetType == LedgerEntryType.GOLD_WEIGHT) "ماندهٔ طلایی" else "ماندهٔ تومانی"}",
            dateTime = LocalCalendar.formatDateTime("yyyy/MM/dd HH:mm", now), timestamp = now,
            type = request.paymentType, direction = if (preview.isReceiving) LedgerDirection.RECEIVE else LedgerDirection.PAY,
            scaleWeightGrams = request.scaleWeightGrams, karat = request.karat,
            equivalent750WeightGrams = preview.equivalent750Grams, amountTomans = preview.paymentValueTomans,
            paymentMethod = if (request.offsetExistingCredit) "تهاتر دفتری" else request.paymentMethod,
            goldCategory = if (request.paymentType == LedgerEntryType.GOLD_WEIGHT) request.paymentMethod else "",
            trackingCode = request.trackingCode, note = request.note,
            tagBadge = "تسویه حساب", invoiceId = invoice?.id, invoiceNumber = invoice?.cleanInvoiceNumber,
            resultingGoldBalance = preview.customerAfter.goldDebtGrams, resultingCashBalance = preview.customerAfter.cashDebtTomans,
            settlement = LedgerSettlement(request.targetType, request.rateTomans, request.rateSource, request.rateObservedAt,
                request.offsetExistingCredit, preview.effect.goldGrams, preview.effect.cashTomans,
                invoiceGoldDeltaGrams = preview.invoiceEffect.goldGrams.takeIf { invoice != null },
                invoiceCashDeltaTomans = preview.invoiceEffect.cashTomans.takeIf { invoice != null })
        )
        customers.addTransaction(tx)
        customers.updateCustomer(preview.customerAfter.copy(lastActivityTime = "لحظاتی پیش"))
        if (invoice != null) {
            val paymentItem = if (request.paymentType == LedgerEntryType.GOLD_WEIGHT) {
                SettlementPaymentItem(
                    id = request.id,
                    method = SettlementMethod.BULLION,
                    amountTomans = preview.paymentValueTomans,
                    goldWeight18k = preview.equivalent750Grams,
                    bullionKarat = request.karat,
                    bullionAngNumber = "",
                    description = "تسویه طلایی (سند ${tx.cleanDocumentNumber})"
                )
            } else {
                SettlementPaymentItem(
                    id = request.id,
                    method = if (request.paymentMethod.contains("پوز") || request.paymentMethod.contains("کارتخوان")) SettlementMethod.POS else SettlementMethod.TRANSFER,
                    amountTomans = preview.paymentValueTomans,
                    trackingCode = request.trackingCode,
                    description = "تسویه ${request.paymentMethod} (سند ${tx.cleanDocumentNumber})"
                )
            }
            val updatedPayments = com.goldex.companion.domain.invoice.InvoiceLedgerSyncUseCase.getEffectivePayments(invoice).filterNot { it.id == paymentItem.id } +
                paymentItem.copy(settlement = tx.settlement, date = tx.dateTime)
            invoices.saveBarterInvoice(invoice.copy(payments = updatedPayments))
        }
        tx
    }
}
