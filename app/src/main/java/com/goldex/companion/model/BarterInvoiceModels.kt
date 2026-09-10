package com.goldex.companion.model

import com.goldex.companion.domain.invoice.BarterCalculationUseCases
import java.util.UUID

enum class CustomerRole(val titleFa: String) {
    WHOLESALER("همکار / بنکدار (تهاتری)"),
    RETAIL("مشتری عادی (مصرف‌کننده)")
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
    override val id: String = UUID.randomUUID().toString(),
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
    override val id: String = UUID.randomUUID().toString(),
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
    override val id: String = UUID.randomUUID().toString(),
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
    override val id: String = UUID.randomUUID().toString(),
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

data class BarterInvoice(
    val id: String = UUID.randomUUID().toString(),
    val invoiceNumber: String = "IR-${(1403..1405).random()}-${(100..999).random()}",
    val createdAt: Long = System.currentTimeMillis(),
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
    val note: String = ""
) {
    val balance: BarterBalance
        get() = BarterCalculationUseCases.calculateBalance(salesItems, receivedItems)
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
    val id: String = UUID.randomUUID().toString(),
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
    val barterInvoice: BarterInvoice? = null
)

