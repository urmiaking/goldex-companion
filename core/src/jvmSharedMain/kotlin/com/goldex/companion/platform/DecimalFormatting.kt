package com.goldex.companion.platform

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

actual object DecimalFormatting {
    actual fun groupedInteger(value: Long): String =
        DecimalFormat("#,###", DecimalFormatSymbols(Locale.US).apply {
            groupingSeparator = ','
        }).format(value)

    actual fun weight(value: Double): String =
        DecimalFormat("#,##0.000", DecimalFormatSymbols(Locale.US).apply {
            decimalSeparator = '.'
        }).format(value)

    actual fun fixed(value: Double, decimals: Int): String =
        String.format(Locale.US, "%.${decimals}f", value)
}
