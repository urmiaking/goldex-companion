package com.goldex.companion.desktop.ui

import com.goldex.companion.domain.reporting.ReportingBreakdownType
import com.goldex.companion.domain.reporting.ReportingUiState
import com.goldex.companion.model.PersianNumberFormatter

/**
 * Text formatter for desktop reporting summaries, ready for clipboard export.
 */
internal object DesktopReportingShareText {
    private fun money(value: Long) = "${PersianNumberFormatter.formatWithSeparators(value)} تومان"
    private fun grams(value: Double) = "${PersianNumberFormatter.formatWeight(value)} گرم"

    fun format(type: ReportingBreakdownType, state: ReportingUiState): String {
        val period = state.selectedPeriod.labelFa
        return when (type) {
            ReportingBreakdownType.SALES_PERFORMANCE -> buildString {
                appendLine("گزارش عملکرد فروش و سود ـ $period")
                appendLine("سود فروشنده: ${money(state.salesPerformance.totalProfitTomans)}")
                appendLine("اجرت ساخت: ${money(state.salesPerformance.totalWageTomans)}")
                appendLine("وزن کل فروش ۱۸ عیار: ${grams(state.salesPerformance.totalWeight18k)}")
                append("مالیات ثبت‌شده: ${money(state.salesPerformance.totalTaxTomans)}")
            }
            ReportingBreakdownType.GOLD_INVENTORY -> buildString {
                appendLine("گزارش تراز وزنی و انبار طلا ـ $period")
                appendLine("موجودی کل عیار ۷۵۰: ${grams(state.goldInventory.totalWeight18k)}")
                appendLine("ورود دوره: ${grams(state.details.periodInboundGrams)}")
                append("خروج دوره: ${grams(state.details.periodOutboundGrams)}")
            }
            ReportingBreakdownType.DEBTORS_CREDITORS -> buildString {
                appendLine("گزارش مانده طرف‌حساب‌های زرگری")
                appendLine("مطالبات ریالی (طلب): ${money(state.debtorsCreditors.totalReceivablesTomans)}")
                appendLine("بدهی ریالی: ${money(state.debtorsCreditors.totalPayablesTomans)}")
                append("مانده وزنی خالص: ${grams(state.debtorsCreditors.goldDebtGrams)}")
            }
            ReportingBreakdownType.VAT_REPORT -> buildString {
                appendLine("گزارش ارزش‌افزوده و مالیات ـ $period")
                appendLine("اصل طلای معاف: ${money(state.vatReport.taxExemptRawGoldTomans)}")
                appendLine("مأخذ مشمول (اجرت و سود): ${money(state.vatReport.taxableBaseTomans)}")
                append("مالیات ثبت‌شده: ${money(state.vatReport.totalVatCollectedTomans)}")
            }
        }
    }

    fun formatOverall(state: ReportingUiState): String {
        val period = state.selectedPeriod.labelFa
        return buildString {
            appendLine("ترازنامه و خلاصه گزارشات زرگری ـ $period")
            appendLine("سود ناخالص محقق‌شده: ${money(state.kpi.grossProfitTomans)}")
            appendLine("گردش مالی کل: ${money(state.kpi.turnoverTomans)}")
            appendLine("موجودی طلای ۱۸ عیار: ${grams(state.kpi.inventoryGoldWeight18k)}")
            appendLine("مطالبات از طرف‌حساب‌ها: ${money(state.kpi.customerReceivablesTomans)}")
            append("فاکتورهای تسویه‌نشده: ${PersianNumberFormatter.toPersianDigits(state.kpi.unsettledInvoicesCount.toString())}")
        }
    }
}
