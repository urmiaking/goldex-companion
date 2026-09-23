package com.goldex.companion.domain.invoice

import com.goldex.companion.model.BarterInvoice
import com.goldex.companion.model.Customer
import com.goldex.companion.model.CustomerRole
import com.goldex.companion.model.LedgerDirection
import com.goldex.companion.model.LedgerEntryType
import com.goldex.companion.model.LedgerTransaction
import com.goldex.companion.model.SettlementMethod
import com.goldex.companion.model.SettlementPaymentItem
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

data class InvoiceSyncResult(
    val transactionsToCreate: List<LedgerTransaction>,
    val updatedCustomer: Customer
)

object InvoiceLedgerSyncUseCase {

    private fun getEffectivePayments(invoice: BarterInvoice): List<SettlementPaymentItem> {
        if (invoice.payments.isNotEmpty()) {
            return invoice.payments
        }
        val fallback = mutableListOf<SettlementPaymentItem>()
        when (invoice.settlementMethod) {
            SettlementMethod.POS -> {
                if (invoice.cashPosAmount > 0L) {
                    fallback.add(
                        SettlementPaymentItem(
                            method = SettlementMethod.POS,
                            amountTomans = invoice.cashPosAmount,
                            trackingCode = invoice.posTrackingCode
                        )
                    )
                }
            }
            SettlementMethod.TRANSFER -> {
                if (invoice.thirdPartyTransferAmount > 0L || invoice.thirdPartyTransferWeight18k > 0.0) {
                    fallback.add(
                        SettlementPaymentItem(
                            method = SettlementMethod.TRANSFER,
                            amountTomans = invoice.thirdPartyTransferAmount,
                            goldWeight18k = invoice.thirdPartyTransferWeight18k,
                            trackingCode = invoice.thirdPartyTrackingCode,
                            thirdPartyCustomerName = invoice.thirdPartyCustomer?.name ?: ""
                        )
                    )
                }
            }
            SettlementMethod.BULLION -> {
                if (invoice.bullionWeight > 0.0) {
                    val eq18k = invoice.bullionWeight * (invoice.bullionKarat.toDouble() / 750.0)
                    fallback.add(
                        SettlementPaymentItem(
                            method = SettlementMethod.BULLION,
                            goldWeight18k = eq18k,
                            amountTomans = (eq18k * invoice.spotPrice18k).toLong(),
                            bullionKarat = invoice.bullionKarat,
                            bullionAngNumber = invoice.bullionAngNumber
                        )
                    )
                }
            }
            SettlementMethod.LEDGER -> {
                if (invoice.ledgerAmount > 0L) {
                    fallback.add(
                        SettlementPaymentItem(
                            method = SettlementMethod.LEDGER,
                            amountTomans = invoice.ledgerAmount,
                            description = "موعد: ${invoice.ledgerDueDate}"
                        )
                    )
                }
            }
        }
        return fallback
    }

    fun generateLedgerSync(
        invoice: BarterInvoice,
        currentCustomer: Customer
    ): InvoiceSyncResult {
        if (!invoice.syncWithLedger) {
            return InvoiceSyncResult(emptyList(), currentCustomer)
        }

        val dateStr = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault()).format(Date(invoice.createdAt))
        val transactions = mutableListOf<LedgerTransaction>()
        var customer = currentCustomer
        val cleanInvNum = invoice.cleanInvoiceNumber
        val effectivePayments = getEffectivePayments(invoice)
        val balance = invoice.balance

        if (invoice.customerRole == CustomerRole.WHOLESALER) {
            // WHOLESALER: Primary currency is 18k Gold Weight
            val net18kWeightDelta = balance.net18kWeightDelta

            if (abs(net18kWeightDelta) >= 0.001) {
                val isPayToCustomer = net18kWeightDelta > 0 // Delivered gold to customer -> Customer is debtor
                val direction = if (isPayToCustomer) LedgerDirection.PAY else LedgerDirection.RECEIVE
                val weight = abs(net18kWeightDelta)

                val goldDelta = if (direction == LedgerDirection.PAY) weight else -weight
                customer = customer.copy(
                    goldDebtGrams = customer.goldDebtGrams + goldDelta,
                    lastActivityTime = "لحظاتی پیش"
                )

                val tx = LedgerTransaction(
                    customerId = customer.id,
                    documentNumber = (1000..9999).random().toString(),
                    title = if (isPayToCustomer) "فاکتور تهاتر طلا" else "بستانکاری تهاتر طلا",
                    dateTime = dateStr,
                    type = LedgerEntryType.GOLD_WEIGHT,
                    direction = direction,
                    goldCategory = "مصنوعات و آبشده",
                    scaleWeightGrams = weight,
                    karat = 750,
                    equivalent750WeightGrams = weight,
                    note = "ثبت خودکار از فاکتور تهاتر شماره $cleanInvNum",
                    tagBadge = "فاکتور تهاتر",
                    resultingGoldBalance = customer.goldDebtGrams,
                    resultingCashBalance = customer.cashDebtTomans,
                    invoiceId = invoice.id,
                    invoiceNumber = cleanInvNum,
                    isAutoGenerated = true,
                    timestamp = invoice.createdAt
                )
                transactions.add(tx)
            }

            // Settlement payments for wholesaler
            effectivePayments.forEach { p ->
                when (p.method) {
                    SettlementMethod.BULLION -> {
                        val weight = if (p.goldWeight18k > 0.0) {
                            p.goldWeight18k
                        } else if (invoice.spotPrice18k > 0L) {
                            p.amountTomans.toDouble() / invoice.spotPrice18k
                        } else 0.0

                        if (weight >= 0.001) {
                            val isCustomerDelivering = net18kWeightDelta >= 0
                            val dir = if (isCustomerDelivering) LedgerDirection.RECEIVE else LedgerDirection.PAY
                            val goldDelta = if (dir == LedgerDirection.PAY) weight else -weight
                            customer = customer.copy(
                                goldDebtGrams = customer.goldDebtGrams + goldDelta,
                                lastActivityTime = "لحظاتی پیش"
                            )

                            val txBullion = LedgerTransaction(
                                customerId = customer.id,
                                documentNumber = (1000..9999).random().toString(),
                                title = if (dir == LedgerDirection.RECEIVE) "دریافت شمش و آبشده" else "تحویل شمش و آبشده",
                                dateTime = dateStr,
                                type = LedgerEntryType.GOLD_WEIGHT,
                                direction = dir,
                                goldCategory = "آبشده",
                                scaleWeightGrams = weight,
                                karat = p.bullionKarat,
                                equivalent750WeightGrams = weight,
                                angNumber = p.bullionAngNumber,
                                note = buildString {
                                    append("تسویه با شمش و آبشده بابت فاکتور #$cleanInvNum")
                                    if (p.bullionAngNumber.isNotBlank()) append(" (انگ: ${p.bullionAngNumber})")
                                },
                                tagBadge = "تحویل شمش",
                                resultingGoldBalance = customer.goldDebtGrams,
                                resultingCashBalance = customer.cashDebtTomans,
                                invoiceId = invoice.id,
                                invoiceNumber = cleanInvNum,
                                isAutoGenerated = true,
                                timestamp = invoice.createdAt
                            )
                            transactions.add(txBullion)
                        }
                    }
                    SettlementMethod.TRANSFER -> {
                        if (p.goldWeight18k >= 0.001) {
                            val isCustomerDelivering = net18kWeightDelta >= 0
                            val dir = if (isCustomerDelivering) LedgerDirection.RECEIVE else LedgerDirection.PAY
                            val goldDelta = if (dir == LedgerDirection.PAY) p.goldWeight18k else -p.goldWeight18k
                            customer = customer.copy(
                                goldDebtGrams = customer.goldDebtGrams + goldDelta,
                                lastActivityTime = "لحظاتی پیش"
                            )

                            val txTransfer = LedgerTransaction(
                                customerId = customer.id,
                                documentNumber = (1000..9999).random().toString(),
                                title = "حواله طلایی",
                                dateTime = dateStr,
                                type = LedgerEntryType.GOLD_WEIGHT,
                                direction = dir,
                                goldCategory = "حواله",
                                scaleWeightGrams = p.goldWeight18k,
                                karat = 750,
                                equivalent750WeightGrams = p.goldWeight18k,
                                trackingCode = p.trackingCode,
                                note = "حواله طلایی بابت فاکتور #$cleanInvNum",
                                tagBadge = "حواله طلایی",
                                resultingGoldBalance = customer.goldDebtGrams,
                                resultingCashBalance = customer.cashDebtTomans,
                                invoiceId = invoice.id,
                                invoiceNumber = cleanInvNum,
                                isAutoGenerated = true,
                                timestamp = invoice.createdAt
                            )
                            transactions.add(txTransfer)
                        }
                        if (p.amountTomans > 0L) {
                            val isCustomerPaying = balance.netPayableAmount >= 0
                            val dir = if (isCustomerPaying) LedgerDirection.RECEIVE else LedgerDirection.PAY
                            val cashDelta = if (dir == LedgerDirection.PAY) p.amountTomans else -p.amountTomans
                            customer = customer.copy(
                                cashDebtTomans = customer.cashDebtTomans + cashDelta,
                                lastActivityTime = "لحظاتی پیش"
                            )

                            val txCash = LedgerTransaction(
                                customerId = customer.id,
                                documentNumber = (1000..9999).random().toString(),
                                title = if (dir == LedgerDirection.RECEIVE) "دریافت حواله نقدی" else "پرداخت حواله نقدی",
                                dateTime = dateStr,
                                type = LedgerEntryType.CASH_RIAL,
                                direction = dir,
                                amountTomans = p.amountTomans,
                                paymentMethod = "حواله بانکی / پایا",
                                trackingCode = p.trackingCode,
                                note = buildString {
                                    append("حواله نقدی بابت فاکتور #$cleanInvNum")
                                    if (p.trackingCode.isNotBlank()) append(" (کد پیگیری: ${p.trackingCode})")
                                },
                                tagBadge = "تسویه حواله",
                                resultingGoldBalance = customer.goldDebtGrams,
                                resultingCashBalance = customer.cashDebtTomans,
                                invoiceId = invoice.id,
                                invoiceNumber = cleanInvNum,
                                isAutoGenerated = true,
                                timestamp = invoice.createdAt
                            )
                            transactions.add(txCash)
                        }
                    }
                    SettlementMethod.POS -> {
                        if (p.amountTomans > 0L) {
                            val isCustomerPaying = balance.netPayableAmount >= 0
                            val dir = if (isCustomerPaying) LedgerDirection.RECEIVE else LedgerDirection.PAY
                            val cashDelta = if (dir == LedgerDirection.PAY) p.amountTomans else -p.amountTomans
                            customer = customer.copy(
                                cashDebtTomans = customer.cashDebtTomans + cashDelta,
                                lastActivityTime = "لحظاتی پیش"
                            )

                            val txPos = LedgerTransaction(
                                customerId = customer.id,
                                documentNumber = (1000..9999).random().toString(),
                                title = if (dir == LedgerDirection.RECEIVE) "دریافت کارتخوان (POS)" else "پرداخت کارتخوان به مشتری",
                                dateTime = dateStr,
                                type = LedgerEntryType.CASH_RIAL,
                                direction = dir,
                                amountTomans = p.amountTomans,
                                paymentMethod = "کارتخوان (POS)",
                                trackingCode = p.trackingCode,
                                note = buildString {
                                    append("پرداخت کارتخوان بابت فاکتور #$cleanInvNum")
                                    if (p.trackingCode.isNotBlank()) append(" (کد پیگیری: ${p.trackingCode})")
                                },
                                tagBadge = "تسویه کارتخوان",
                                resultingGoldBalance = customer.goldDebtGrams,
                                resultingCashBalance = customer.cashDebtTomans,
                                invoiceId = invoice.id,
                                invoiceNumber = cleanInvNum,
                                isAutoGenerated = true,
                                timestamp = invoice.createdAt
                            )
                            transactions.add(txPos)
                        }
                    }
                    SettlementMethod.LEDGER -> {
                        if (p.amountTomans > 0L) {
                            val dueDate = p.description.ifBlank { invoice.ledgerDueDate }
                            customer = customer.copy(
                                cashDebtTomans = customer.cashDebtTomans + p.amountTomans,
                                lastActivityTime = "لحظاتی پیش"
                            )

                            val txCash = LedgerTransaction(
                                customerId = customer.id,
                                documentNumber = (1000..9999).random().toString(),
                                title = "مانده دفتری فاکتور ($dueDate)",
                                dateTime = dateStr,
                                type = LedgerEntryType.CASH_RIAL,
                                direction = LedgerDirection.PAY,
                                amountTomans = p.amountTomans,
                                note = "انتقال مانده ریالی فاکتور به دفتر معین",
                                tagBadge = "دفتر معین",
                                resultingGoldBalance = customer.goldDebtGrams,
                                resultingCashBalance = customer.cashDebtTomans,
                                invoiceId = invoice.id,
                                invoiceNumber = cleanInvNum,
                                isAutoGenerated = true,
                                timestamp = invoice.createdAt
                            )
                            transactions.add(txCash)
                        }
                    }
                }
            }
        } else {
            // RETAIL: Primary currency is Cash / Toman
            val netPayable = balance.netPayableAmount

            if (netPayable > 1000.0) {
                // Customer owes money for the invoice goods
                val invoiceAmount = netPayable.toLong()
                customer = customer.copy(
                    cashDebtTomans = customer.cashDebtTomans + invoiceAmount,
                    lastActivityTime = "لحظاتی پیش"
                )

                val invoiceTx = LedgerTransaction(
                    customerId = customer.id,
                    documentNumber = (1000..9999).random().toString(),
                    title = "فاکتور فروش طلا",
                    dateTime = dateStr,
                    type = LedgerEntryType.CASH_RIAL,
                    direction = LedgerDirection.PAY,
                    amountTomans = invoiceAmount,
                    paymentMethod = "فاکتور فروش",
                    note = "ثبت خودکار فاکتور فروش شماره $cleanInvNum",
                    tagBadge = "فاکتور فروش",
                    resultingGoldBalance = customer.goldDebtGrams,
                    resultingCashBalance = customer.cashDebtTomans,
                    invoiceId = invoice.id,
                    invoiceNumber = cleanInvNum,
                    isAutoGenerated = true,
                    timestamp = invoice.createdAt
                )
                transactions.add(invoiceTx)

                // Process settlement payments made by customer (reducing debt)
                effectivePayments.forEach { p ->
                    when (p.method) {
                        SettlementMethod.POS -> {
                            if (p.amountTomans > 0L) {
                                customer = customer.copy(
                                    cashDebtTomans = customer.cashDebtTomans - p.amountTomans,
                                    lastActivityTime = "لحظاتی پیش"
                                )

                                val tx = LedgerTransaction(
                                    customerId = customer.id,
                                    documentNumber = (1000..9999).random().toString(),
                                    title = "دریافت کارتخوان (POS)",
                                    dateTime = dateStr,
                                    type = LedgerEntryType.CASH_RIAL,
                                    direction = LedgerDirection.RECEIVE,
                                    amountTomans = p.amountTomans,
                                    paymentMethod = "کارتخوان (POS)",
                                    trackingCode = p.trackingCode,
                                    note = buildString {
                                        append("پرداخت کارتخوان بابت فاکتور #$cleanInvNum")
                                        if (p.trackingCode.isNotBlank()) append(" (کد پیگیری: ${p.trackingCode})")
                                    },
                                    tagBadge = "تسویه کارتخوان",
                                    resultingGoldBalance = customer.goldDebtGrams,
                                    resultingCashBalance = customer.cashDebtTomans,
                                    invoiceId = invoice.id,
                                    invoiceNumber = cleanInvNum,
                                    isAutoGenerated = true,
                                    timestamp = invoice.createdAt
                                )
                                transactions.add(tx)
                            }
                        }
                        SettlementMethod.TRANSFER -> {
                            if (p.amountTomans > 0L) {
                                customer = customer.copy(
                                    cashDebtTomans = customer.cashDebtTomans - p.amountTomans,
                                    lastActivityTime = "لحظاتی پیش"
                                )

                                val tx = LedgerTransaction(
                                    customerId = customer.id,
                                    documentNumber = (1000..9999).random().toString(),
                                    title = "دریافت حواله بانکی",
                                    dateTime = dateStr,
                                    type = LedgerEntryType.CASH_RIAL,
                                    direction = LedgerDirection.RECEIVE,
                                    amountTomans = p.amountTomans,
                                    paymentMethod = "حواله بانکی / پایا",
                                    trackingCode = p.trackingCode,
                                    note = buildString {
                                        append("پرداخت حواله بابت فاکتور #$cleanInvNum")
                                        if (p.trackingCode.isNotBlank()) append(" (کد پیگیری: ${p.trackingCode})")
                                    },
                                    tagBadge = "تسویه حواله",
                                    resultingGoldBalance = customer.goldDebtGrams,
                                    resultingCashBalance = customer.cashDebtTomans,
                                    invoiceId = invoice.id,
                                    invoiceNumber = cleanInvNum,
                                    isAutoGenerated = true,
                                    timestamp = invoice.createdAt
                                )
                                transactions.add(tx)
                            }
                        }
                        SettlementMethod.BULLION -> {
                            val valTomans = if (p.amountTomans > 0L) {
                                p.amountTomans
                            } else if (p.goldWeight18k > 0.0 && invoice.spotPrice18k > 0L) {
                                (p.goldWeight18k * invoice.spotPrice18k).toLong()
                            } else 0L

                            if (valTomans > 0L) {
                                customer = customer.copy(
                                    cashDebtTomans = customer.cashDebtTomans - valTomans,
                                    lastActivityTime = "لحظاتی پیش"
                                )

                                val tx = LedgerTransaction(
                                    customerId = customer.id,
                                    documentNumber = (1000..9999).random().toString(),
                                    title = "دریافت شمش و آبشده",
                                    dateTime = dateStr,
                                    type = LedgerEntryType.CASH_RIAL,
                                    direction = LedgerDirection.RECEIVE,
                                    amountTomans = valTomans,
                                    paymentMethod = "آبشده و شمش",
                                    goldCategory = "آبشده",
                                    scaleWeightGrams = p.goldWeight18k,
                                    karat = p.bullionKarat,
                                    equivalent750WeightGrams = p.goldWeight18k,
                                    angNumber = p.bullionAngNumber,
                                    note = buildString {
                                        append("تسویه با تحویل آبشده بابت فاکتور #$cleanInvNum")
                                        if (p.bullionAngNumber.isNotBlank()) append(" (انگ: ${p.bullionAngNumber})")
                                    },
                                    tagBadge = "تحویل شمش",
                                    resultingGoldBalance = customer.goldDebtGrams,
                                    resultingCashBalance = customer.cashDebtTomans,
                                    invoiceId = invoice.id,
                                    invoiceNumber = cleanInvNum,
                                    isAutoGenerated = true,
                                    timestamp = invoice.createdAt
                                )
                                transactions.add(tx)
                            }
                        }
                        SettlementMethod.LEDGER -> {
                            // Deferral to customer ledger; already represented by invoiceTx debit
                        }
                    }
                }
            } else if (netPayable < -1000.0) {
                // Customer gave more scrap gold than purchase -> customer is creditor (shop owes customer)
                val creditAmount = abs(netPayable).toLong()
                customer = customer.copy(
                    cashDebtTomans = customer.cashDebtTomans - creditAmount,
                    lastActivityTime = "لحظاتی پیش"
                )

                val invoiceTx = LedgerTransaction(
                    customerId = customer.id,
                    documentNumber = (1000..9999).random().toString(),
                    title = "خرید طلای متفرقه",
                    dateTime = dateStr,
                    type = LedgerEntryType.CASH_RIAL,
                    direction = LedgerDirection.RECEIVE,
                    amountTomans = creditAmount,
                    note = "بستانکاری بابت خرید طلای متفرقه فاکتور #$cleanInvNum",
                    tagBadge = "خرید طلا",
                    resultingGoldBalance = customer.goldDebtGrams,
                    resultingCashBalance = customer.cashDebtTomans,
                    invoiceId = invoice.id,
                    invoiceNumber = cleanInvNum,
                    isAutoGenerated = true,
                    timestamp = invoice.createdAt
                )
                transactions.add(invoiceTx)

                // Process settlement payments made TO the customer ("یا بهش مبلغی بدیم")
                effectivePayments.forEach { p ->
                    when (p.method) {
                        SettlementMethod.LEDGER -> {
                            // Remaining credit stays in customer ledger
                        }
                        else -> {
                            val amt = if (p.amountTomans > 0L) {
                                p.amountTomans
                            } else if (p.goldWeight18k > 0.0 && invoice.spotPrice18k > 0L) {
                                (p.goldWeight18k * invoice.spotPrice18k).toLong()
                            } else 0L

                            if (amt > 0L) {
                                customer = customer.copy(
                                    cashDebtTomans = customer.cashDebtTomans + amt,
                                    lastActivityTime = "لحظاتی پیش"
                                )

                                val payTitle = when (p.method) {
                                    SettlementMethod.POS -> "پرداخت کارتخوان به مشتری"
                                    SettlementMethod.TRANSFER -> "پرداخت حواله به مشتری"
                                    else -> "پرداخت وجه تسویه خرید"
                                }
                                val payMethod = when (p.method) {
                                    SettlementMethod.POS -> "کارتخوان (POS)"
                                    SettlementMethod.TRANSFER -> "حواله بانکی / پایا"
                                    else -> "اسکناس نقد"
                                }
                                val tx = LedgerTransaction(
                                    customerId = customer.id,
                                    documentNumber = (1000..9999).random().toString(),
                                    title = payTitle,
                                    dateTime = dateStr,
                                    type = LedgerEntryType.CASH_RIAL,
                                    direction = LedgerDirection.PAY, // Paid to customer -> balances out the credit
                                    amountTomans = amt,
                                    paymentMethod = payMethod,
                                    trackingCode = p.trackingCode,
                                    note = buildString {
                                        append("پرداخت وجه به مشتری بابت تسویه فاکتور #$cleanInvNum")
                                        if (p.trackingCode.isNotBlank()) append(" (کد پیگیری: ${p.trackingCode})")
                                    },
                                    tagBadge = "تسویه خرید طلا",
                                    resultingGoldBalance = customer.goldDebtGrams,
                                    resultingCashBalance = customer.cashDebtTomans,
                                    invoiceId = invoice.id,
                                    invoiceNumber = cleanInvNum,
                                    isAutoGenerated = true,
                                    timestamp = invoice.createdAt
                                )
                                transactions.add(tx)
                            }
                        }
                    }
                }
            }
        }

        return InvoiceSyncResult(transactions, customer)
    }

    fun reverseLedgerSync(
        transactions: List<LedgerTransaction>,
        currentCustomer: Customer
    ): Customer {
        var customer = currentCustomer
        transactions.forEach { tx ->
            when (tx.type) {
                LedgerEntryType.GOLD_WEIGHT -> {
                    val delta = if (tx.direction == LedgerDirection.PAY) {
                        tx.equivalent750WeightGrams
                    } else {
                        -tx.equivalent750WeightGrams
                    }
                    customer = customer.copy(goldDebtGrams = customer.goldDebtGrams - delta)
                }
                LedgerEntryType.CASH_RIAL -> {
                    val delta = if (tx.direction == LedgerDirection.PAY) {
                        tx.amountTomans
                    } else {
                        -tx.amountTomans
                    }
                    customer = customer.copy(cashDebtTomans = customer.cashDebtTomans - delta)
                }
            }
        }
        return customer
    }
}
