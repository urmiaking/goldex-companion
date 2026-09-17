package com.goldex.companion.domain.invoice

import com.goldex.companion.data.AppSettings
import com.goldex.companion.model.BankCoinItem
import com.goldex.companion.model.BarterInvoice
import com.goldex.companion.model.BarterItem
import com.goldex.companion.model.CraftedGoldItem
import com.goldex.companion.model.MeltGoldItem
import com.goldex.companion.model.PersianNumberFormatter
import com.goldex.companion.model.ScrapGoldItem
import com.goldex.companion.model.SettlementMethod
import com.goldex.companion.model.WageType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class OfficialInvoiceRow(
    val title: String,
    val subtitle: String,
    val karat: Int?,
    val grossWeight: Double?,
    val stoneWeight: Double?,
    val netWeight: Double?,
    val wageLabel: String,
    val profitLabel: String,
    val amountTomans: Long,
    val isReceived: Boolean
)

data class OfficialInvoiceDocument(
    val fileName: String,
    val invoiceNumber: String,
    val issuedDate: String,
    val issuedTime: String,
    val trackingCode: String,
    val sellerName: String,
    val sellerManager: String,
    val sellerLicense: String,
    val sellerPhone: String,
    val sellerAddress: String,
    val buyerName: String,
    val buyerPhone: String,
    val buyerNationalId: String,
    val buyerRole: String,
    val spotPrice18k: Long,
    val rows: List<OfficialInvoiceRow>,
    val totalGrossWeight: Double,
    val totalNetWeight: Double,
    val totalRawGoldValue: Long,
    val totalWageAndProfit: Long,
    val totalTax: Long,
    val totalReceived: Long,
    val payableAmount: Long,
    val settlementLabel: String,
    val note: String
)

object OfficialInvoiceDocumentFactory {
    fun create(invoice: BarterInvoice, settings: AppSettings): OfficialInvoiceDocument {
        val formatter = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.US)
        val (date, time) = formatter.format(Date(invoice.createdAt)).split(" ")
        val salesRows = invoice.salesItems.map { it.toRow(isReceived = false) }
        val receivedRows = invoice.receivedItems.map { it.toRow(isReceived = true) }
        val craftedItems = invoice.salesItems.filterIsInstance<CraftedGoldItem>()
        val totalRaw = craftedItems.sumOf { it.rawGoldValue }.toLong()
        val totalExtras = craftedItems.sumOf { it.wageAmount + it.profitAmount }.toLong()
        val totalTax = craftedItems.sumOf { it.taxAmount }.toLong()
        val grossWeight = (invoice.salesItems + invoice.receivedItems).sumOf { it.grossWeightOrEquivalent() }
        val netWeight = (invoice.salesItems + invoice.receivedItems).sumOf { it.equivalent18kWeight }
        val balance = invoice.balance

        return OfficialInvoiceDocument(
            fileName = "Qirat_Invoice_${invoice.invoiceNumber}.pdf",
            invoiceNumber = invoice.invoiceNumber,
            issuedDate = PersianNumberFormatter.toPersianDigits(date),
            issuedTime = PersianNumberFormatter.toPersianDigits(time),
            trackingCode = invoice.trackingCode(),
            sellerName = settings.galleryName,
            sellerManager = settings.managerName,
            sellerLicense = settings.galleryLicense,
            sellerPhone = settings.galleryPhone,
            sellerAddress = settings.galleryAddress,
            buyerName = invoice.customer?.name?.ifBlank { "مشتری عمومی" } ?: "مشتری عمومی",
            buyerPhone = invoice.customer?.phone?.ifBlank { "ثبت نشده" } ?: "ثبت نشده",
            buyerNationalId = invoice.customer?.nationalId?.ifBlank { "ثبت نشده" } ?: "ثبت نشده",
            buyerRole = invoice.customerRole.titleFa,
            spotPrice18k = invoice.spotPrice18k,
            rows = salesRows + receivedRows,
            totalGrossWeight = grossWeight,
            totalNetWeight = netWeight,
            totalRawGoldValue = totalRaw,
            totalWageAndProfit = totalExtras,
            totalTax = totalTax,
            totalReceived = balance.totalReceivedAmount.toLong(),
            payableAmount = balance.netPayableAmount.toLong().coerceAtLeast(0L),
            settlementLabel = if (invoice.payments.isNotEmpty()) {
                "تسویه چندمرحله‌ای (${PersianNumberFormatter.toPersianDigits(invoice.payments.size.toString())} روش)"
            } else {
                invoice.settlementMethod.labelFa
            },
            note = invoice.note
        )
    }

    private fun BarterInvoice.trackingCode(): String {
        val explicit = when (settlementMethod) {
            SettlementMethod.POS -> posTrackingCode
            SettlementMethod.TRANSFER -> thirdPartyTrackingCode
            SettlementMethod.LEDGER -> thirdPartyInvoiceNumber
            SettlementMethod.BULLION -> bullionAngNumber
        }
        return explicit.ifBlank { "QIRAT-${invoiceNumber.filter { it.isLetterOrDigit() || it == '-' }}" }
    }

    private fun BarterItem.toRow(isReceived: Boolean): OfficialInvoiceRow = when (this) {
        is CraftedGoldItem -> OfficialInvoiceRow(
            title = title,
            subtitle = if (isReceived) "دریافتی از مشتری" else "مصنوع طلا و جواهر",
            karat = customKaratValue.takeIf { it > 0 }
                ?: (karat.purityRatio * 1000.0).toInt(),
            grossWeight = grossWeight,
            stoneWeight = stoneWeight,
            netWeight = netWeight,
            wageLabel = if (wageType == WageType.PERCENTAGE) {
                "${PersianNumberFormatter.formatPercent(wageInput)}٪"
            } else {
                "${PersianNumberFormatter.formatPrice(wageInput)} ت/گ"
            },
            profitLabel = "${PersianNumberFormatter.formatPercent(profitPercent)}٪",
            amountTomans = totalPayable.toLong(),
            isReceived = isReceived
        )

        is ScrapGoldItem -> OfficialInvoiceRow(
            title = title,
            subtitle = "طلای مستعمل؛ عیار پرداختی ${PersianNumberFormatter.toPersianDigits(payableKarat.toString())}",
            karat = payableKarat,
            grossWeight = grossWeight,
            stoneWeight = stoneWeight,
            netWeight = netWeight,
            wageLabel = "—",
            profitLabel = "—",
            amountTomans = totalPayable.toLong(),
            isReceived = isReceived
        )

        is MeltGoldItem -> OfficialInvoiceRow(
            title = title,
            subtitle = listOfNotNull(
                angNumber.takeIf { it.isNotBlank() }?.let { "انگ $it" },
                labName.takeIf { it.isNotBlank() }
            ).joinToString(" • ").ifBlank { "طلای آبشده" },
            karat = labKarat,
            grossWeight = weight,
            stoneWeight = 0.0,
            netWeight = weight,
            wageLabel = "—",
            profitLabel = "—",
            amountTomans = totalPayable.toLong(),
            isReceived = isReceived
        )

        is BankCoinItem -> OfficialInvoiceRow(
            title = title,
            subtitle = "${PersianNumberFormatter.toPersianDigits(count.toString())} عدد • ${coinType.titleFa}",
            karat = null,
            grossWeight = equivalent18kWeight,
            stoneWeight = 0.0,
            netWeight = equivalent18kWeight,
            wageLabel = "—",
            profitLabel = "—",
            amountTomans = totalPayable.toLong(),
            isReceived = isReceived
        )
    }

    private fun BarterItem.grossWeightOrEquivalent(): Double = when (this) {
        is CraftedGoldItem -> grossWeight
        is ScrapGoldItem -> grossWeight
        is MeltGoldItem -> weight
        is BankCoinItem -> equivalent18kWeight
    }
}
