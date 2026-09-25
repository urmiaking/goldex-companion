package com.goldex.companion.ui.reporting

import com.goldex.companion.domain.reporting.ReportingBreakdownType
import com.goldex.companion.domain.reporting.ReportingUiState
import com.goldex.companion.model.PersianNumberFormatter

/** A concise, user-initiated text summary; it never claims to be an official PDF. */
internal fun reportingShareText(type: ReportingBreakdownType, state: ReportingUiState): String {
    fun money(value: Long) = "${PersianNumberFormatter.formatWithSeparators(value)} تومان"
    fun grams(value: Double) = "${PersianNumberFormatter.formatWeight(value)} گرم"
    val period = state.selectedPeriod.labelFa
    return when (type) {
        ReportingBreakdownType.SALES_PERFORMANCE -> buildString {
            appendLine("گزارش عملکرد فروش ـ $period")
            appendLine("اجرت ساخت: ${money(state.salesPerformance.totalWageTomans)}")
            appendLine("سود فروشنده: ${money(state.salesPerformance.totalProfitTomans)}")
            append("مالیات ثبت‌شده: ${money(state.salesPerformance.totalTaxTomans)}")
        }
        ReportingBreakdownType.GOLD_INVENTORY -> buildString {
            appendLine("گزارش تراز وزنی ـ $period")
            appendLine("موجودی فعلی عیار ۷۵۰: ${grams(state.goldInventory.totalWeight18k)}")
            appendLine("ورود دوره: ${grams(state.details.periodInboundGrams)}")
            append("خروج دوره: ${grams(state.details.periodOutboundGrams)}")
        }
        ReportingBreakdownType.DEBTORS_CREDITORS -> buildString {
            appendLine("گزارش مانده فعلی طرف‌حساب‌ها")
            appendLine("مطالبات ریالی: ${money(state.debtorsCreditors.totalReceivablesTomans)}")
            appendLine("بدهی ریالی: ${money(state.debtorsCreditors.totalPayablesTomans)}")
            append("مانده وزنی خالص: ${grams(state.debtorsCreditors.goldDebtGrams)}")
        }
        ReportingBreakdownType.VAT_REPORT -> buildString {
            appendLine("گزارش مالیات ثبت‌شده ـ $period")
            appendLine("اصل طلای معاف: ${money(state.vatReport.taxExemptRawGoldTomans)}")
            appendLine("مأخذ مشمول (اجرت و سود): ${money(state.vatReport.taxableBaseTomans)}")
            append("مالیات ثبت‌شده: ${money(state.vatReport.totalVatCollectedTomans)}")
        }
    }
}
