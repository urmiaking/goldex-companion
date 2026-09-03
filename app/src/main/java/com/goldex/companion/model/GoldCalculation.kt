package com.goldex.companion.model

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class Karat(val labelFa: String, val karatNumber: Int, val purityRatio: Double) {
    K18("۱۸ عیار (۷۵۰)", 18, 18.0 / 24.0),
    K21("۲۱ عیار (۸۷۵)", 21, 21.0 / 24.0),
    K24("۲۴ عیار (۹۹۹)", 24, 24.0 / 24.0)
}

enum class PriceBasis(val labelFa: String, val ratio: Double) {
    PER_GRAM_18K("مبنای هر گرم ۱۸ عیار", 18.0 / 24.0),
    PER_GRAM_24K("مبنای هر گرم ۲۴ عیار (شِمش)", 1.0)
}

enum class WageType(val labelFa: String) {
    PERCENTAGE("درصدی (٪)"),
    TOMAN_PER_GRAM("تومانی به ازای هر گرم")
}

enum class CoinType(
    val titleFa: String,
    val totalWeightGrams: Double,
    val purity: Double = 0.900,
    val mintFee: Double = 100_000.0
) {
    EMAMI("تمام سکه بهار آزادی (امامی)", 8.13598, 0.900, 100_000.0),
    BAHAR("تمام سکه طرح قدیم", 8.13598, 0.900, 100_000.0),
    HALF("نیم سکه بهار آزادی", 4.06799, 0.900, 80_000.0),
    QUARTER("ربع سکه بهار آزادی", 2.03399, 0.900, 60_000.0),
    GERAMI("سکه یک گرمی بانک مرکزی", 1.01, 0.900, 50_000.0);

    val pureWeightGrams: Double get() = totalWeightGrams * purity
}

data class CalculationResult(
    val pureGoldWeightGrams: Double,
    val rawGoldValue: Double,
    val marginAmount: Double,
    val totalTradeValue: Double,
    val effectivePricePerGram: Double
)

data class DetailedJewelryResult(
    val grossWeight: Double,
    val stoneWeight: Double,
    val netWeight: Double,
    val rawGoldValue: Double,
    val wageAmount: Double,
    val profitAmount: Double,
    val taxAmount: Double,
    val totalPayable: Double,
    val effectiveGramPrice: Double
) {
    fun formatInvoice(spotPrice: Long, karat: Karat): String {
        val dateStr = SimpleDateFormat("yyyy/MM/dd - HH:mm", Locale.getDefault()).format(Date())
        return """
            ========================================
            🧾 فاکتور رسمی محاسبات طلای گلدکس (GoldEx)
            تاریخ و زمان: $dateStr
            ========================================
            • وزن کل: ${PersianNumberFormatter.formatWeight(grossWeight)} گرم
            • کسر وزن سنگ/نگین: ${PersianNumberFormatter.formatWeight(stoneWeight)} گرم
            • وزن خالص طلا: ${PersianNumberFormatter.formatWeight(netWeight)} گرم
            • عیار قطعه: ${karat.labelFa}
            • مظنه روز هر گرم: ${PersianNumberFormatter.formatPrice(spotPrice.toDouble())} تومان
            ----------------------------------------
            • ارزش خام طلا: ${PersianNumberFormatter.formatPrice(rawGoldValue)} تومان
            • مبلغ اجرت ساخت: ${PersianNumberFormatter.formatPrice(wageAmount)} تومان
            • سود طلافروش: ${PersianNumberFormatter.formatPrice(profitAmount)} تومان
            • مالیات بر ارزش افزوده (قانونی): ${PersianNumberFormatter.formatPrice(taxAmount)} تومان
            ----------------------------------------
            💰 جمع کل پرداختی: ${PersianNumberFormatter.formatPrice(totalPayable)} تومان
            ✨ نرخ تمام‌شده هر گرم: ${PersianNumberFormatter.formatPrice(effectiveGramPrice)} تومان
            ========================================
            GoldEx Companion - همراه هوشمند معامله‌گران طلا
        """.trimIndent()
    }
}

data class CoinBubbleResult(
    val coinType: CoinType,
    val marketPrice: Double,
    val intrinsicValue: Double,
    val bubbleAmount: Double,
    val bubblePercent: Double
)

object PersianNumberFormatter {
    private val persianDigits = charArrayOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')

    fun toPersianDigits(input: String): String {
        val sb = StringBuilder()
        for (ch in input) {
            if (ch in '0'..'9') {
                sb.append(persianDigits[ch - '0'])
            } else {
                sb.append(ch)
            }
        }
        return sb.toString()
    }

    fun toEnglishDigits(input: String): String {
        val sb = StringBuilder()
        for (ch in input) {
            when (ch) {
                '۰' -> sb.append('0')
                '۱' -> sb.append('1')
                '۲' -> sb.append('2')
                '۳' -> sb.append('3')
                '۴' -> sb.append('4')
                '۵' -> sb.append('5')
                '۶' -> sb.append('6')
                '۷' -> sb.append('7')
                '۸' -> sb.append('8')
                '۹' -> sb.append('9')
                '٫' -> sb.append('.')
                '/' -> sb.append('.')
                else -> sb.append(ch)
            }
        }
        return sb.toString()
    }

    fun formatPrice(amount: Double): String {
        val symbols = DecimalFormatSymbols(Locale.US).apply {
            groupingSeparator = ','
        }
        val df = DecimalFormat("#,###", symbols)
        return toPersianDigits(df.format(amount.toLong()))
    }

    fun formatWeight(weight: Double): String {
        val symbols = DecimalFormatSymbols(Locale.US).apply {
            decimalSeparator = '.'
        }
        val df = DecimalFormat("#,##0.000", symbols)
        return toPersianDigits(df.format(weight))
    }

    fun formatPercent(value: Double): String {
        val formatted = if (value % 1.0 == 0.0) {
            value.toLong().toString()
        } else {
            "%.2f".format(Locale.US, value).trimEnd('0').trimEnd('.')
        }
        return toPersianDigits(formatted)
    }

    fun normalizeForSearch(text: String): String {
        return toEnglishDigits(text)
            .replace("-", "")
            .replace(" ", "")
            .trim()
            .lowercase()
    }

    fun parsePersianOrEnglish(text: String): Double? {
        val clean = toEnglishDigits(text)
            .replace(",", "")
            .replace("،", "")
            .replace(" ", "")
            .trim()
        return clean.toDoubleOrNull()
    }

    fun parseToCleanLong(text: String): Long? {
        val clean = toEnglishDigits(text)
            .replace(",", "")
            .replace("،", "")
            .replace(" ", "")
            .trim()
        return clean.toLongOrNull()
    }
}
