package com.goldex.companion.ui.util

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

class ThousandsSeparatorVisualTransformation(
    private val isPersian: Boolean = true,
    private val addSeparators: Boolean = true
) : VisualTransformation {

    override fun filter(text: AnnotatedString): TransformedText {
        val original = text.text
        if (original.isEmpty()) {
            return TransformedText(text, OffsetMapping.Identity)
        }

        if (!addSeparators) {
            val converted = if (isPersian) {
                val sb = StringBuilder(original.length)
                for (ch in original) {
                    sb.append(toPersianDigit(ch))
                }
                sb.toString()
            } else {
                original
            }
            return TransformedText(AnnotatedString(converted), OffsetMapping.Identity)
        }

        val parts = original.split('.')
        val intPart = parts[0]
        val hasDecimal = parts.size > 1
        val decimalPart = if (hasDecimal) parts.subList(1, parts.size).joinToString(".") else ""

        val formatted = StringBuilder()
        val origToTrans = IntArray(original.length + 1)
        val transToOrigList = mutableListOf<Int>()

        var transIdx = 0
        val intLen = intPart.length

        for (i in 0 until intLen) {
            origToTrans[i] = transIdx
            val digit = intPart[i]
            val displayDigit = if (isPersian) toPersianDigit(digit) else digit
            formatted.append(displayDigit)
            transToOrigList.add(i)
            transIdx++

            val digitsLeft = intLen - (i + 1)
            if (digitsLeft > 0 && digitsLeft % 3 == 0) {
                formatted.append('،')
                transToOrigList.add(i + 1)
                transIdx++
            }
        }

        origToTrans[intLen] = transIdx

        if (hasDecimal) {
            formatted.append('.')
            transToOrigList.add(intLen)
            transIdx++

            for (j in decimalPart.indices) {
                val origIdx = intLen + 1 + j
                origToTrans[origIdx] = transIdx
                val digit = decimalPart[j]
                val displayDigit = if (isPersian) toPersianDigit(digit) else digit
                formatted.append(displayDigit)
                transToOrigList.add(origIdx)
                transIdx++
            }
        }

        origToTrans[original.length] = transIdx
        transToOrigList.add(original.length)

        val transToOrig = transToOrigList.toIntArray()

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                val safeOffset = offset.coerceIn(0, original.length)
                return origToTrans[safeOffset]
            }

            override fun transformedToOriginal(offset: Int): Int {
                val safeOffset = offset.coerceIn(0, transToOrig.size - 1)
                return transToOrig[safeOffset]
            }
        }

        return TransformedText(AnnotatedString(formatted.toString()), offsetMapping)
    }

    private fun toPersianDigit(ch: Char): Char {
        return when (ch) {
            '0' -> '۰'
            '1' -> '۱'
            '2' -> '۲'
            '3' -> '۳'
            '4' -> '۴'
            '5' -> '۵'
            '6' -> '۶'
            '7' -> '۷'
            '8' -> '۸'
            '9' -> '۹'
            else -> ch
        }
    }
}
