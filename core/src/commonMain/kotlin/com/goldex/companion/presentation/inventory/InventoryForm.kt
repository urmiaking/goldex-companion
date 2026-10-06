package com.goldex.companion.presentation.inventory

import com.goldex.companion.model.*
import com.goldex.companion.domain.inventory.*
import com.goldex.companion.platform.RandomIdGenerator
import com.goldex.companion.platform.SystemClock
import kotlin.math.abs
import kotlin.math.round

/** Platform-free entry policy. Weights describe one piece; stock movements change quantity only. */
data class InventoryDraft(
    val id: String? = null, val createdAt: Long = 0,
    val code: String = "", val title: String = "", val category: InventoryCategory = InventoryCategory.RINGS,
    val location: String = "ویترین اصلی", val workshop: String = "کارگاه عمومی", val rfid: String = "",
    val gross: String = "", val stone: String = "0", val purity: String = "750", val karat: Karat = Karat.K18,
    val wageType: WageType = WageType.PERCENTAGE, val wage: String = "10", val quantity: String = "1",
    val profit: String = "7", val tax: String = "9", val image: String = "",
    val errors: Map<String, String> = emptyMap()
)

object InventoryForm {
    fun normalize(value: String): String = value.trim().map { character ->
        when (character) {
            in '۰'..'۹' -> '0' + (character - '۰')
            in '٠'..'٩' -> '0' + (character - '٠')
            '٫' -> '.'
            else -> character
        }
    }.joinToString("").replace(",", "").replace("٬", "")

    private fun number(value: String): Double? = normalize(value).takeIf { it.matches(Regex("[0-9]+(\\.[0-9]+)?")) }?.toDoubleOrNull()?.takeIf { it.isFinite() }
    private fun integer(value: String): Int? = normalize(value).takeIf { it.matches(Regex("[0-9]+")) }?.toIntOrNull()
    private fun weight(value: String): Double? = number(value)?.takeIf { it <= 1_000_000 && abs(it * 1000 - round(it * 1000)) < 0.000001 }

    fun validate(draft: InventoryDraft): Map<String, String> = buildMap {
        if (draft.title.trim().isEmpty() || draft.title.length > 150) put("title", "نام کالا را وارد کنید؛ حداکثر ۱۵۰ حرف")
        if (draft.code.trim().isEmpty() || draft.code.length > 80) put("code", "کد کالا الزامی است؛ حداکثر ۸۰ حرف")
        if (draft.category == InventoryCategory.ALL) put("category", "دستهٔ کالا را انتخاب کنید")
        val gross = weight(draft.gross)
        val stone = weight(draft.stone.ifBlank { "0" })
        if (gross == null || gross <= 0) put("gross", "وزن مثبت با حداکثر سه رقم اعشار وارد کنید")
        if (stone == null || stone < 0 || gross != null && stone >= gross) put("stone", "وزن نگین باید کمتر از وزن ناخالص باشد")
        if (integer(draft.purity)?.let { it in 100..1000 } != true) put("purity", "عیار بین ۱۰۰ و ۱۰۰۰ وارد کنید")
        if (integer(draft.quantity)?.let { it in (if (draft.id == null) 1 else 0)..1_000_000 } != true) put("quantity", "تعداد صحیح و حداکثر یک میلیون وارد کنید")
        if (number(draft.wage)?.let { it >= 0 && it <= if (draft.wageType == WageType.PERCENTAGE) 100.0 else 1_000_000_000.0 } != true) put("wage", "اجرت معتبر وارد کنید")
        for ((field, value) in listOf("profit" to draft.profit, "tax" to draft.tax)) if (number(value)?.let { it in 0.0..100.0 } != true) put(field, "درصد بین صفر و ۱۰۰ وارد کنید")
        for ((field, value) in listOf("location" to draft.location, "workshop" to draft.workshop, "rfid" to draft.rfid)) if (value.length > 200) put(field, "حداکثر ۲۰۰ حرف وارد کنید")
    }

    fun from(item: InventoryItem) = InventoryDraft(item.id, item.createdAt, item.code, item.title, item.category, item.location, item.workshop,
        item.rfidTag, item.grossWeightGrams.toString(), item.stoneWeightGrams.toString(), item.customKaratValue.toString(), item.karat,
        item.wageType, item.wageValue.toString(), item.quantity.toString(), item.profitPercent.toString(), item.taxPercent.toString(), item.imageUrl)

    fun toItem(draft: InventoryDraft): InventoryItem {
        require(validate(draft).isEmpty())
        return InventoryItem(id = draft.id ?: RandomIdGenerator.newId(), createdAt = if (draft.id == null) SystemClock.nowMillis() else draft.createdAt,
            code = draft.code.trim(), title = draft.title.trim(), category = draft.category,
            location = draft.location.trim().ifBlank { "ویترین اصلی" }, workshop = draft.workshop.trim().ifBlank { "کارگاه عمومی" },
            rfidTag = draft.rfid.trim(), grossWeightGrams = number(draft.gross)!!, stoneWeightGrams = number(draft.stone.ifBlank { "0" })!!,
            karat = draft.karat, customKaratValue = integer(draft.purity)!!, wageType = draft.wageType,
            wagePercent = if (draft.wageType == WageType.PERCENTAGE) number(draft.wage)!! else 0.0, wageValue = number(draft.wage)!!,
            quantity = integer(draft.quantity)!!, profitPercent = number(draft.profit)!!, taxPercent = number(draft.tax)!!, imageUrl = draft.image)
    }

    fun price(item: InventoryItem, spot18: Long): InventoryPriceBreakdown? = InventoryPricing.breakdown(item, spot18)

    fun preview(draft: InventoryDraft, spot18: Long): InventoryPriceBreakdown? =
        if (validate(draft).isEmpty()) price(toItem(draft.copy(id = draft.id ?: "preview")), spot18) else null

    fun adjustedQuantity(item: InventoryItem, type: StockAdjustmentType, count: Int): Int = InventoryStockPolicy.adjustedQuantity(item, type, count)

    fun matches(item: InventoryItem, query: String): Boolean {
        fun key(value: String) = normalize(value).replace('ي', 'ی').replace('ك', 'ک').lowercase()
        val needle = key(query)
        return needle.isBlank() || listOf(item.title, item.code, item.rfidTag, item.location, item.workshop).any { key(it).contains(needle) }
    }
}
