package com.goldex.companion.ui.invoices

import com.goldex.companion.model.*

/** Desktop form validation only; every financial calculation remains in the shared policy. */
internal fun invoiceItemError(item: BarterItem): String? {
    if (!item.totalPayable.isFinite() || item.totalPayable <= 0 || item.totalPayable >= Long.MAX_VALUE.toDouble() ||
        !item.equivalent18kWeight.isFinite() || item.equivalent18kWeight <= 0) return "وزن و مبلغ قلم باید معتبر و بیشتر از صفر باشد"
    return when (item) {
        is CraftedGoldItem -> when {
            item.spotPrice <= 0 || item.customKaratValue !in 1..1000 -> "نرخ و عیار معتبر را وارد کنید"
            !item.grossWeight.isFinite() || !item.stoneWeight.isFinite() || item.grossWeight <= item.stoneWeight || item.stoneWeight < 0 -> "وزن ناخالص باید از کسر نگین بیشتر باشد"
            listOf(item.wageInput, item.wageAmount, item.profitPercent, item.profitAmount, item.taxPercent, item.taxAmount).any { !it.isFinite() || it < 0 } -> "اجرت، سود و مالیات باید معتبر و نامنفی باشند"
            else -> null
        }
        is ScrapGoldItem -> if (item.spotPrice <= 0 || item.payableKarat !in 1..1000 || item.netWeight <= 0 || item.stoneWeight < 0 ||
            item.grossWeight <= item.stoneWeight || item.deductionPerGram < 0 || item.exchangeCommissionPercent < 0) "وزن، عیار و کسری خرید را بررسی کنید" else null
        is MeltGoldItem -> if (!item.weight.isFinite() || item.weight <= 0 || item.labKarat !in 1..1000 || item.spotPrice <= 0) "وزن، عیار و نرخ آبشده را بررسی کنید" else null
        is BankCoinItem -> if (item.count <= 0 || item.unitPrice <= 0) "تعداد و قیمت سکه باید بیشتر از صفر باشد" else null
    }
}
