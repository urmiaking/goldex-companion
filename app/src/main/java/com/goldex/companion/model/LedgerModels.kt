package com.goldex.companion.model

import java.util.UUID

enum class LedgerEntryType(val titleFa: String) {
    GOLD_WEIGHT("تسویه وزنی و طلا"),
    CASH_RIAL("تسویه نقدی و ریالی")
}

enum class LedgerDirection(val titleFa: String) {
    RECEIVE("دریافت از طرف‌حساب"),
    PAY("پرداخت به طرف‌حساب")
}

data class LedgerTransaction(
    val id: String = UUID.randomUUID().toString(),
    val customerId: String,
    val documentNumber: String,
    val title: String,
    val dateTime: String,
    val type: LedgerEntryType = LedgerEntryType.GOLD_WEIGHT,
    val direction: LedgerDirection = LedgerDirection.RECEIVE,
    // Gold specific
    val goldCategory: String = "آبشده", // آبشده, مصنوعات, سکه و شمش
    val scaleWeightGrams: Double = 0.0,
    val karat: Int = 750,
    val equivalent750WeightGrams: Double = 0.0,
    val angNumber: String = "",
    val labName: String = "",
    // Cash specific
    val amountTomans: Long = 0L,
    val paymentMethod: String = "حواله بانکی / پایا", // حواله بانکی / پایا, چک صیادی, کارتخوان (POS), اسکناس نقد
    val destinationBank: String = "",
    val trackingCode: String = "",
    // Common
    val note: String = "",
    val tagBadge: String = "",
    val resultingGoldBalance: Double = 0.0,
    val resultingCashBalance: Long = 0L,
    val timestamp: Long = System.currentTimeMillis()
)

enum class CustomerLedgerFilterTab(val titleFa: String) {
    ALL("همه طرف‌حساب‌ها"),
    DEBTORS("بدهکار به ما"),
    CREDITORS("بستانکار"),
    SETTLED("تسویه‌شده (بی‌حساب)")
}

enum class StatementFilterTab(val titleFa: String) {
    ALL("همه اسناد"),
    GOLD_SALE("فروش طلا و شمش"),
    GOLD_RECEIPT("دریافت طلا و آبشده"),
    CASH_DEPOSIT("واریز وجه / چک"),
    SETTLEMENT("تسویه و تهاتر")
}
