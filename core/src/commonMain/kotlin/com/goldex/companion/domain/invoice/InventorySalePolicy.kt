package com.goldex.companion.domain.invoice

import com.goldex.companion.data.InventoryStore
import com.goldex.companion.model.*
import kotlin.math.absoluteValue

/**
 * Converts an inventory product to a sale snapshot at the invoice's quote.
 * No stock is reserved until the entire invoice is saved successfully.
 */
object InventorySalePolicy {
    fun createLine(product: InventoryItem, count: Int, quote18: Long): CraftedGoldItem {
        require(product.category != InventoryCategory.COINS) { "سکه باید به‌عنوان قلم سکه بانکی ثبت شود" }
        require(count in 1..product.quantity) { "تعداد درخواستی بیشتر از موجودی انبار است" }
        require(product.grossWeightGrams.isFinite() && product.stoneWeightGrams.isFinite() &&
            product.netGoldWeightGrams.isFinite() &&
            product.wageValue.isFinite() && product.profitPercent.isFinite() && product.taxPercent.isFinite() &&
            product.wageValue >= 0 && product.profitPercent in 0.0..100.0 && product.taxPercent in 0.0..100.0) {
            "مشخصات وزن یا محاسبات کالا معتبر نیست"
        }
        require(quote18 > 0 && product.netGoldWeightGrams > 0 && product.customKaratValue in 1..1000) {
            "برای فروش کالا باید نرخ، وزن خالص و عیار معتبر باشند"
        }
        return BarterCalculationUseCases.calculateCraftedItem(
            title = product.title, karat = product.karat,
            customKaratValue = product.customKaratValue,
            grossWeight = product.grossWeightGrams * count,
            stoneWeight = product.stoneWeightGrams * count,
            spotPrice18k = quote18, wageType = product.wageType,
            wageInput = product.wageValue,
            profitPercent = product.profitPercent,
            taxPercent = product.taxPercent
        ).copy(inventoryItemId = product.id, inventoryQuantity = count)
    }


    /** Uses the stored coin snapshot, never maps bank coins to crafted-gold tax/wage rules. */
    fun createSaleLine(product: InventoryItem, count: Int, quote18: Long): BarterItem {
        if (product.category != InventoryCategory.COINS) return createLine(product, count, quote18)
        require(count in 1..product.quantity) { "تعداد سکه بیشتر از موجودی انبار است" }
        val type = requireNotNull(product.coinType) { "نوع سکه در انبار مشخص نیست؛ اطلاعات کالا را تکمیل کنید" }
        val unitPrice = requireNotNull(product.coinUnitPrice) { "قیمت واحد سکه در انبار مشخص نیست؛ اطلاعات کالا را تکمیل کنید" }
        val hologram = requireNotNull(product.coinHasHologram) { "وضعیت وکیوم سکه را در انبار تکمیل کنید" }
        require(unitPrice in 1..1_000_000_000_000_000L && count.toLong() <= 1_000_000_000_000_000L / unitPrice) {
            "قیمت یا تعداد سکه معتبر نیست"
        }
        return BarterCalculationUseCases.calculateCoinItem(type, count, hologram, unitPrice)
            .copy(title = product.title, inventoryItemId = product.id, inventoryQuantity = count)
    }

    private fun allocations(invoice: BarterInvoice?): Map<String, Int> {
        if (invoice == null) return emptyMap()
        require(invoice.receivedItems.none { it.inventoryItemId.isNotBlank() }) {
            "کالای دریافتی نباید از موجودی فروش کسر شود"
        }
        val seen = mutableSetOf<String>()
        return invoice.salesItems.filter { it.inventoryItemId.isNotBlank() }.onEach {
            require(it.id.isNotBlank() && seen.add(it.id)) { "شناسه قلم انباری تکراری است" }
            require(it.inventoryQuantity in 1..1_000_000) { "تعداد فروش انباری معتبر نیست" }
        }.groupBy { it.inventoryItemId }.mapValues { (_, rows) ->
            rows.sumOf { it.inventoryQuantity.toLong() }.also { require(it <= 1_000_000) }.toInt()
        }
    }

    /**
     * Idempotent by comparing the last persisted invoice to the requested invoice.
     * Must run inside the same database/outbox transaction as invoice and ledger writes.
     */
    fun reconcile(previous: BarterInvoice?, requested: BarterInvoice?, inventory: InventoryStore?) {
        val old = allocations(previous)
        val next = allocations(requested)
        if (old == next) return
        requireNotNull(inventory) { "انبار برای ثبت فروش متصل نیست" }
        val products = inventory.getItems().associateBy { it.id }
        val changes = (old.keys + next.keys).associateWith { id -> (next[id] ?: 0) - (old[id] ?: 0) }
        // Restore inventory first, then validate and deduct; a failed transaction rolls it all back.
        listOf(false, true).forEach { isDeduction ->
            changes.forEach { (id, delta) ->
                if (delta != 0 && (delta > 0) == isDeduction) {
                    val product = requireNotNull(products[id]) { "کالای مرتبط با فاکتور در انبار پیدا نشد" }
                    val change = delta.absoluteValue
                    inventory.adjustStock(StockAdjustment(
                        itemId = id, itemTitle = product.title,
                        type = if (delta > 0) StockAdjustmentType.DEDUCT else StockAdjustmentType.CHARGE,
                        quantityChange = change,
                        weightGrams = product.netGoldWeightGrams * change,
                        reason = if (delta > 0) "فروش فاکتور ${requested?.cleanInvoiceNumber.orEmpty()}" else "برگشت فاکتور ${previous?.cleanInvoiceNumber.orEmpty()}",
                        note = "invoiceId=${requested?.id ?: previous?.id}"
                    ))
                }
            }
        }
    }

    fun hasLinkedItems(invoice: BarterInvoice?): Boolean =
        invoice?.salesItems?.any { it.inventoryItemId.isNotBlank() } == true
}
