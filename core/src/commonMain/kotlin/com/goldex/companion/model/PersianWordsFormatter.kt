package com.goldex.companion.model

object PersianWordsFormatter {
    private val yekan = arrayOf("", "یک", "دو", "سه", "چهار", "پنج", "شش", "هفت", "هشت", "نه")
    private val dahgan = arrayOf("", "ده", "بیست", "سی", "چهل", "پنجاه", "شصت", "هفتاد", "هشتاد", "نود")
    private val dahha = arrayOf("ده", "یازده", "دوازده", "سیزده", "چهارده", "پانزده", "شانزده", "هفده", "هجده", "نوزده")
    private val sadgan = arrayOf("", "صد", "دویست", "سیصد", "چهارصد", "پانصد", "ششصد", "هفتصد", "هشتصد", "نهصد")
    private val maghadir = arrayOf("", "هزار", "میلیون", "میلیارد", "تریلیون")

    fun toWords(num: Long): String {
        if (num == 0L) return "صفر تومان"
        if (num < 0) return "منفی " + toWords(-num)

        var n = num
        val parts = mutableListOf<String>()
        var level = 0

        while (n > 0) {
            val chunk = (n % 1000).toInt()
            if (chunk > 0) {
                val chunkText = chunkToWords(chunk)
                val suffix = maghadir[level]
                val combined = if (suffix.isNotEmpty()) "$chunkText $suffix" else chunkText
                parts.add(0, combined)
            }
            n /= 1000
            level++
        }

        return parts.joinToString(" و ") + " تومان"
    }

    private fun chunkToWords(n: Int): String {
        val s = n / 100
        val d = (n % 100) / 10
        val y = n % 10

        val words = mutableListOf<String>()
        if (s > 0) words.add(sadgan[s])

        if (d == 1) {
            words.add(dahha[y])
        } else {
            if (d > 0) words.add(dahgan[d])
            if (y > 0) words.add(yekan[y])
        }

        return words.joinToString(" و ")
    }
}
