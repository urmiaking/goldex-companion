package com.goldex.companion.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

data class InvoiceItem(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "قطعه طلا",
    val karat: Karat = Karat.K18,
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
    val totalPayable: Double,
    val effectiveGramPrice: Double
)

data class Invoice(
    val id: String = UUID.randomUUID().toString(),
    val invoiceNumber: String = (100000..999999).random().toString(),
    val createdAt: Long = System.currentTimeMillis(),
    val customer: Customer? = null,
    val items: List<InvoiceItem> = emptyList(),
    val note: String = ""
) {
    val totalGrossWeight: Double get() = items.sumOf { it.grossWeight }
    val totalStoneWeight: Double get() = items.sumOf { it.stoneWeight }
    val totalNetWeight: Double get() = items.sumOf { it.netWeight }
    val totalRawGoldValue: Double get() = items.sumOf { it.rawGoldValue }
    val totalWageAmount: Double get() = items.sumOf { it.wageAmount }
    val totalProfitAmount: Double get() = items.sumOf { it.profitAmount }
    val totalTaxAmount: Double get() = items.sumOf { it.taxAmount }
    val totalPayable: Double get() = items.sumOf { it.totalPayable }
    val effectiveGramPrice: Double get() = if (totalNetWeight > 0) totalPayable / totalNetWeight else 0.0

    fun formatTextInvoice(sourceName: String = "اتحادیه طلا"): String {
        val dateStr = SimpleDateFormat("yyyy/MM/dd - HH:mm", Locale.getDefault()).format(Date(createdAt))
        val sb = StringBuilder()
        sb.append("🧾 فاکتور رسمی زرگری و معامله طلا (گلدکس پرو)\n")
        sb.append("════════════════════════════════════════\n")
        sb.append("شماره فاکتور: #${PersianNumberFormatter.toPersianDigits(invoiceNumber)} • تاریخ: ${PersianNumberFormatter.toPersianDigits(dateStr)}\n")
        sb.append("منبع مظنه: $sourceName\n")
        if (customer != null) {
            sb.append("────────────────────────────────────────\n")
            sb.append("👤 خریدار: ${customer.name}\n")
            if (customer.phone.isNotBlank()) {
                sb.append("📞 تلفن: ${PersianNumberFormatter.toPersianDigits(customer.phone)}\n")
            }
            if (customer.nationalId.isNotBlank()) {
                sb.append("🆔 کد ملی / شناسه: ${PersianNumberFormatter.toPersianDigits(customer.nationalId)}\n")
            }
            if (customer.note.isNotBlank()) {
                sb.append("📝 یادداشت: ${customer.note}\n")
            }
        }
        sb.append("════════════════════════════════════════\n")
        sb.append("📋 اقلام فاکتور (${PersianNumberFormatter.toPersianDigits(items.size.toString())} قلم):\n")
        items.forEachIndexed { index, item ->
            sb.append("${PersianNumberFormatter.toPersianDigits((index + 1).toString())}. ${item.title} (${item.karat.labelFa})\n")
            sb.append("   • وزن ناخالص: ${PersianNumberFormatter.formatWeight(item.grossWeight)} گرم")
            if (item.stoneWeight > 0) {
                sb.append(" | کسر نگین: ${PersianNumberFormatter.formatWeight(item.stoneWeight)} گرم")
            }
            sb.append(" | وزن خالص: ${PersianNumberFormatter.formatWeight(item.netWeight)} گرم\n")
            val wageLabel = if (item.wageType == WageType.PERCENTAGE) {
                "${PersianNumberFormatter.toPersianDigits(item.wageInput.toString())}٪"
            } else {
                "${PersianNumberFormatter.formatPrice(item.wageInput)} تومان/گرم"
            }
            sb.append("   • اجرت: $wageLabel (${PersianNumberFormatter.formatPrice(item.wageAmount)} ت)")
            sb.append(" | سود: ${PersianNumberFormatter.toPersianDigits(item.profitPercent.toString())}٪")
            sb.append(" | مالیات: ${PersianNumberFormatter.toPersianDigits(item.taxPercent.toString())}٪\n")
            sb.append("   • مبلغ ردیف: ${PersianNumberFormatter.formatPrice(item.totalPayable)} تومان\n")
            if (index < items.size - 1) {
                sb.append("   - - - - - - - - - - - - - - - - - -\n")
            }
        }
        sb.append("════════════════════════════════════════\n")
        sb.append("📊 خلاصه مالی فاکتور:\n")
        sb.append("• مجموع وزن خالص: ${PersianNumberFormatter.formatWeight(totalNetWeight)} گرم\n")
        sb.append("• کل ارزش طلای خام: ${PersianNumberFormatter.formatPrice(totalRawGoldValue)} تومان\n")
        sb.append("• کل اجرت ساخت: ${PersianNumberFormatter.formatPrice(totalWageAmount)} تومان\n")
        sb.append("• کل سود مصوب فروشنده: ${PersianNumberFormatter.formatPrice(totalProfitAmount)} تومان\n")
        sb.append("• کل مالیات بر ارزش افزوده (قانونی): ${PersianNumberFormatter.formatPrice(totalTaxAmount)} تومان\n")
        sb.append("────────────────────────────────────────\n")
        sb.append("💰 مبلغ کل نهایی قابل پرداخت: ${PersianNumberFormatter.formatPrice(totalPayable)} تومان\n")
        sb.append("(${PersianWordsFormatter.toWords(totalPayable.toLong())} تومان)\n")
        sb.append("✨ میانگین تمام‌شده هر گرم: ${PersianNumberFormatter.formatPrice(effectiveGramPrice)} تومان\n")
        sb.append("════════════════════════════════════════\n")
        sb.append("توضیحات قانونی: اصل طلا از ۹٪ مالیات معاف بوده و مالیات صرفاً بر اجرت و سود اعمال گردیده است.\n")
        sb.append("GoldEx Pro • همراه هوشمند معامله‌گران طلا\n")
        return sb.toString()
    }
}
