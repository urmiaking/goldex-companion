package com.goldex.companion.model

import com.goldex.companion.platform.SystemClock
import com.goldex.companion.platform.RandomIdGenerator
import com.goldex.companion.domain.invoice.BarterCalculationUseCases
import com.goldex.companion.domain.reporting.ShamsiCalendarHelper

enum class CustomerRole(val titleFa: String) {
    WHOLESALER("همکار / بنکدار (تهاتری)"),
    RETAIL("مشتری عادی (مصرف‌کننده)")
}

enum class InvoiceDebtBasis(val titleFa: String) {
    GOLD("گرمی"),
    CASH("تومان ثابت")
}

enum class InvoiceItemCategory(val titleFa: String) {
    CRAFTED("طلای ساخته"),
    SCRAP("متفرقه / کهنه"),
    MELT("آبشده سنتی"),
    COIN("سکه بانکی")
}

sealed interface BarterItem {
    val id: String
    val title: String
    val category: InvoiceItemCategory
    val totalPayable: Double
    val equivalent18kWeight: Double
}

data class CraftedGoldItem(
    override val id: String = RandomIdGenerator.newId(),
    override val title: String = "دستبند و زیورآلات ساخته ۱۸ عیار",
    val karat: Karat = Karat.K18,
    val customKaratValue: Int = 750,
    val grossWeight: Double,
    val stoneWeight: Double = 0.0,
    val netWeight: Double,
    val spotPrice: Long,
    val wageType: WageType = WageType.PERCENTAGE,
    val wageInput: Double,
    val wageAmount: Double,
    val profitPercent: Double,
    val profitAmount: Double,
    val taxPercent: Double,
    val taxAmount: Double,
    val rawGoldValue: Double,
    override val totalPayable: Double,
    override val equivalent18kWeight: Double
) : BarterItem {
    override val category: InvoiceItemCategory = InvoiceItemCategory.CRAFTED
}

data class ScrapGoldItem(
    override val id: String = RandomIdGenerator.newId(),
    override val title: String = "طلای متفرقه و کهنه مستعمل",
    val baseKarat: Int = 750,
    val karatDeficit: Int = 15,
    val payableKarat: Int = 735,
    val grossWeight: Double,
    val stoneWeight: Double = 0.0,
    val netWeight: Double,
    val spotPrice: Long,
    val deductionPerGram: Long = 15000L,
    val exchangeCommissionPercent: Double = 0.0,
    val effectiveGramPrice: Long,
    override val totalPayable: Double,
    override val equivalent18kWeight: Double
) : BarterItem {
    override val category: InvoiceItemCategory = InvoiceItemCategory.SCRAP
}

data class MeltGoldItem(
    override val id: String = RandomIdGenerator.newId(),
    override val title: String = "طلای آبشده سنتی",
    val weight: Double,
    val labKarat: Int = 750,
    val angNumber: String = "",
    val labName: String = "",
    val spotPrice: Long,
    override val totalPayable: Double,
    override val equivalent18kWeight: Double
) : BarterItem {
    override val category: InvoiceItemCategory = InvoiceItemCategory.MELT
}

data class BankCoinItem(
    override val id: String = RandomIdGenerator.newId(),
    override val title: String = "سکه بانکی",
    val coinType: CoinType = CoinType.EMAMI,
    val count: Int = 1,
    val hasHologram: Boolean = true,
    val unitPrice: Long,
    override val totalPayable: Double,
    override val equivalent18kWeight: Double
) : BarterItem {
    override val category: InvoiceItemCategory = InvoiceItemCategory.COIN
}

enum class SettlementMethod(val labelFa: String, val subtitleFa: String) {
    POS("کارتخوان / پوز", "واریز آنی"),
    TRANSFER("حواله سه‌طرفه", "تهاتر دفتری"),
    LEDGER("دفتر معین", "حساب همکار"),
    BULLION("تحویل شمش", "آبشده و انگ")
}

data class BarterBalance(
    val totalSalesAmount: Double,
    val totalSales18kWeight: Double,
    val totalSalesCoinCount: Int,
    val totalReceivedAmount: Double,
    val totalReceived18kWeight: Double,
    val netPayableAmount: Double,
    val net18kWeightDelta: Double,
    val isCustomerDebtor: Boolean,
    val isSettled: Boolean
)

data class SettlementPaymentItem(
    val id: String = RandomIdGenerator.newId(),
    val method: SettlementMethod,
    val amountTomans: Long = 0L,
    val goldWeight18k: Double = 0.0,
    val trackingCode: String = "",
    val description: String = "",
    val date: String = "",
    val thirdPartyCustomerName: String = "",
    val bullionKarat: Int = 750,
    val bullionAngNumber: String = "",
    val settlement: LedgerSettlement? = null
)

fun generateBarterInvoiceNumber(createdAtMillis: Long = SystemClock.nowMillis()): String {
    val shamsiYear = ShamsiCalendarHelper.millisToShamsi(createdAtMillis).first
    val serial = (100..999).random()
    return "$shamsiYear$serial"
}

data class BarterInvoice(
    val id: String = RandomIdGenerator.newId(),
    val invoiceNumber: String = generateBarterInvoiceNumber(),
    val createdAt: Long = SystemClock.nowMillis(),
    val customer: Customer? = null,
    val customerRole: CustomerRole = CustomerRole.WHOLESALER,
    val spotPrice18k: Long = 23_360_000L,
    val salesItems: List<BarterItem> = emptyList(),
    val receivedItems: List<BarterItem> = emptyList(),
    val settlementMethod: SettlementMethod = SettlementMethod.POS,
    val cashPosAmount: Long = 0L,
    val ledgerAmount: Long = 0L,
    val posTrackingCode: String = "",
    val ledgerDueDate: String = "تسویه ماهانه",
    val bullionWeight: Double = 0.0,
    val bullionKarat: Int = 750,
    val bullionAngNumber: String = "",
    val thirdPartyCustomer: Customer? = null,
    val thirdPartyInvoiceId: String = "",
    val thirdPartyInvoiceNumber: String = "",
    val thirdPartyTransferWeight18k: Double = 0.0,
    val thirdPartyTransferAmount: Long = 0L,
    val thirdPartyTrackingCode: String = "",
    val note: String = "",
    val payments: List<SettlementPaymentItem> = emptyList(),
    val syncWithLedger: Boolean = true,
    // Null preserves the historical role-based contract; never migrate it implicitly.
    val debtBasis: InvoiceDebtBasis? = null,
    val deletedSettlementIds: List<String> = emptyList()
) {
    val isGoldDebt: Boolean get() = debtBasis == InvoiceDebtBasis.GOLD ||
        (debtBasis == null && customerRole == CustomerRole.WHOLESALER)
    val debtPrincipalGold: Double get() = com.goldex.companion.domain.invoice.InvoiceDebtPolicy.principalGold(this)
    val cleanInvoiceNumber: String
        get() = invoiceNumber.removePrefix("IR-").removePrefix("IR").trim()

    val balance: BarterBalance
        get() = BarterCalculationUseCases.calculateBalance(salesItems, receivedItems)

    val totalPaymentsGold18k: Double
        get() = if (spotPrice18k > 0) com.goldex.companion.domain.invoice.InvoiceLedgerSyncUseCase.getEffectivePayments(this)
            .sumOf { com.goldex.companion.domain.invoice.InvoiceDebtPolicy.paymentGold(this, it) } else 0.0

    val totalPaymentsAmount: Long
        get() {
            if (payments.isNotEmpty()) {
                return payments.sumOf { p ->
                    if (p.settlement != null) {
                        if (p.settlement.offsetExistingCredit) 0L else p.amountTomans
                    } else if (p.method == SettlementMethod.LEDGER) {
                        0L
                    } else if (p.method == SettlementMethod.BULLION) {
                        ((p.goldWeight18k * spotPrice18k).toLong())
                    } else {
                        p.amountTomans
                    }
                }
            }
            return when (settlementMethod) {
                SettlementMethod.POS -> cashPosAmount
                SettlementMethod.LEDGER -> 0L
                SettlementMethod.TRANSFER -> thirdPartyTransferAmount
                SettlementMethod.BULLION -> ((bullionWeight * (bullionKarat.toDouble() / 750.0) * spotPrice18k).toLong())
            }
        }

    val remainingBalanceTomans: Long
        get() {
            if (debtBasis != null) {
                val remaining = com.goldex.companion.domain.invoice.InvoiceDebtPolicy.outstanding(this)
                return if (isGoldDebt) com.goldex.companion.platform.SettlementArithmetic.goldToTomans(kotlin.math.abs(remaining.goldGrams), spotPrice18k)
                else kotlin.math.abs(remaining.cashTomans)
            }
            val netPayable = balance.netPayableAmount.toLong()
            return (netPayable - totalPaymentsAmount).coerceAtLeast(0L)
        }

    val remainingBalanceGold18k: Double
        get() {
            if (debtBasis != null) return kotlin.math.abs(com.goldex.companion.domain.invoice.InvoiceDebtPolicy.outstanding(this).goldGrams)
            val netWeight = balance.net18kWeightDelta
            if (netWeight <= 0.001) return 0.0
            val totalPaymentsWeight = if (payments.isNotEmpty()) {
                payments.sumOf { p ->
                    if (p.settlement != null) kotlin.math.abs(p.settlement.invoiceGoldDeltaGrams ?: p.settlement.goldDeltaGrams)
                    else if (p.method == SettlementMethod.LEDGER) 0.0
                    else if (p.goldWeight18k > 0.0) p.goldWeight18k
                    else if (spotPrice18k > 0L) p.amountTomans.toDouble() / spotPrice18k
                    else 0.0
                }
            } else {
                when (settlementMethod) {
                    SettlementMethod.BULLION -> bullionWeight * (bullionKarat.toDouble() / 750.0)
                    SettlementMethod.TRANSFER -> if (thirdPartyTransferWeight18k > 0.0) thirdPartyTransferWeight18k else if (spotPrice18k > 0L) thirdPartyTransferAmount.toDouble() / spotPrice18k else 0.0
                    SettlementMethod.POS -> if (spotPrice18k > 0L) cashPosAmount.toDouble() / spotPrice18k else 0.0
                    SettlementMethod.LEDGER -> 0.0
                }
            }
            return (netWeight - totalPaymentsWeight).coerceAtLeast(0.0)
        }

    val isFullySettled: Boolean
        get() {
            if (debtBasis != null) return com.goldex.companion.domain.invoice.InvoiceDebtPolicy.outstanding(this).isSettled
            return if (customerRole == CustomerRole.WHOLESALER) {
                val netWeight = balance.net18kWeightDelta
                if (netWeight <= 0.001) true
                else remainingBalanceGold18k <= 0.001
            } else {
                val netPayable = balance.netPayableAmount.toLong()
                if (netPayable <= 0L) true
                else totalPaymentsAmount >= netPayable
            }
        }
}

enum class InvoiceStatus(val titleFa: String) {
    SETTLED("تسویه نقدی کامل"),
    PARTIALLY_PAID("۲۰٪ مانده حساب"),
    WORKSHOP("در کارگاه ساخت")
}

enum class InvoiceFilterTab(val titleFa: String) {
    ALL("همه"),
    SETTLED("تسویه شده"),
    PENDING("در انتظار پرداخت"),
    WORKSHOP("سفارش کارگاه")
}

enum class InvoiceCardAction {
    VIEW_DETAILS,
    SETTLE_BALANCE,
    EDIT_WORKSHOP
}

data class InvoiceListItem(
    val id: String = RandomIdGenerator.newId(),
    val invoiceNumber: String,
    val customerName: String,
    val customerInitials: String,
    val isVerified: Boolean = true,
    val createdAtText: String,
    val status: InvoiceStatus = InvoiceStatus.SETTLED,
    val statusDetail: String = "تسویه نقدی کامل",
    val itemsSummary: String,
    val itemsCountText: String,
    val line1Detail: String,
    val line2Detail: String,
    val finalAmount: Long,
    val amountLabel: String = "مبلغ نهایی پرداختی:",
    val actionButtonText: String = "مشاهده جزییات",
    val actionType: InvoiceCardAction = InvoiceCardAction.VIEW_DETAILS,
    val remainingDetail: String? = null,
    val barterInvoice: BarterInvoice? = null
) {
    val cleanInvoiceNumber: String
        get() = invoiceNumber.removePrefix("IR-").removePrefix("IR").trim()
}

