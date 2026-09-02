package com.goldex.companion.model

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
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

data class CalculationResult(
    val pureGoldWeightGrams: Double,
    val rawGoldValue: Double,
    val marginAmount: Double,
    val totalTradeValue: Double,
    val effectivePricePerGram: Double
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

    fun parsePersianOrEnglish(text: String): Double? {
        val clean = text
            .replace('۰', '0')
            .replace('۱', '1')
            .replace('۲', '2')
            .replace('۳', '3')
            .replace('۴', '4')
            .replace('۵', '5')
            .replace('۶', '6')
            .replace('۷', '7')
            .replace('۸', '8')
            .replace('۹', '9')
            .replace(",", "")
            .replace("،", "")
            .replace(" ", "")
            .trim()
        return clean.toDoubleOrNull()
    }
}
