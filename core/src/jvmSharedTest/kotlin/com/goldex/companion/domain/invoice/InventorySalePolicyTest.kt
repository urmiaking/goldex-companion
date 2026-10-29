package com.goldex.companion.domain.invoice

import com.goldex.companion.data.*
import com.goldex.companion.data.sync.SyncUnitOfWork
import com.goldex.companion.domain.inventory.InventoryStockPolicy
import com.goldex.companion.model.*
import org.junit.Test
import kotlin.test.*

class InventorySalePolicyTest {
    private class MemoryInventory(initial: InventoryItem) : InventoryStore {
        var item = initial
        val history = mutableListOf<StockAdjustment>()
        override fun getItems() = listOf(item)
        override fun addItem(item: InventoryItem) { this.item = item }
        override fun updateItem(item: InventoryItem) { this.item = item }
        override fun deleteItem(id: String) = Unit
        override fun getAdjustments() = history.toList()
        override fun adjustStock(adjustment: StockAdjustment) {
            item = item.copy(quantity = InventoryStockPolicy.adjustedQuantity(item, adjustment.type, adjustment.quantityChange))
            history += adjustment
        }
    }

    private class MemoryInvoices : InvoiceStore {
        val records = linkedMapOf<String, BarterInvoice>()
        override fun getInvoices(): List<Invoice> = emptyList()
        override fun saveInvoice(invoice: Invoice) = Unit
        override fun deleteInvoice(id: String) = Unit
        override fun getBarterInvoices() = records.values.toList()
        override fun saveBarterInvoice(invoice: BarterInvoice) { records[invoice.id] = invoice }
        override fun deleteBarterInvoice(id: String) { records.remove(id) }
    }

    private class EmptyCustomers : CustomerStore {
        override fun getCustomers(): List<Customer> = emptyList()
        override fun addCustomer(customer: Customer) = Unit
        override fun updateCustomer(customer: Customer) = Unit
        override fun deleteCustomer(id: String) = Unit
        override fun getTransactions(customerId: String): List<LedgerTransaction> = emptyList()
        override fun addTransaction(transaction: LedgerTransaction) = Unit
    }
    private val unit = object : SyncUnitOfWork { override fun <T> transaction(action: () -> T): T = action() }
    private fun item(quantity: Int = 5) = InventoryItem(
        id = "product", code = "10002", title = "دستبند", quantity = quantity,
        grossWeightGrams = 3.0, stoneWeightGrams = 0.25, wageValue = 10.0
    )
    private fun invoice(line: CraftedGoldItem) =
        BarterInvoice(id = "invoice-test", invoiceNumber = "14051234", spotPrice18k = 7_000_000, salesItems = listOf(line))

    @Test fun saleEditRetryAndDeleteOnlyMoveActualStockDelta() {
        val inv = MemoryInventory(item())
        val invoices = MemoryInvoices()
        val customers = EmptyCustomers()
        val save = SaveBarterInvoiceUseCase(invoices, customers, unit, inv)
        val first = invoice(InventorySalePolicy.createLine(inv.item, 2, 7_000_000))
        assertEquals(5, inv.item.quantity, "Draft selection must not change stock")
        save.save(first)
        assertEquals(3, inv.item.quantity)
        assertEquals(1, inv.history.size)
        save.save(first)
        assertEquals(3, inv.item.quantity)
        assertEquals(1, inv.history.size, "Retried save must not deduct twice")
        val edit = first.copy(salesItems = listOf(InventorySalePolicy.createLine(item(), 3, 7_000_000)))
        save.save(edit)
        assertEquals(2, inv.item.quantity)
        assertEquals(2, inv.history.size)
        assertTrue(InvoiceDeletionUseCase(invoices, customers, unit, inv).delete(first.id) is InvoiceDeletionResult.Deleted)
        assertEquals(5, inv.item.quantity)
        assertEquals(3, inv.history.size)
        assertEquals(StockAdjustmentType.CHARGE, inv.history.last().type)
        assertTrue(inv.history.all { it.note.contains("invoiceId=invoice-test") })
    }

    @Test fun insufficientStockFailsBeforeInvoiceIsSaved() {
        val inv = MemoryInventory(item(2))
        val invoices = MemoryInvoices()
        val draft = invoice(InventorySalePolicy.createLine(inv.item, 2, 7_000_000))
        inv.item = inv.item.copy(quantity = 1)
        assertFailsWith<IllegalArgumentException> {
            SaveBarterInvoiceUseCase(invoices, EmptyCustomers(), unit, inv).save(draft)
        }
        assertTrue(invoices.records.isEmpty())
        assertEquals(1, inv.item.quantity)
        assertTrue(inv.history.isEmpty())
    }

    @Test fun manualSalesAndLegacyInvoicesDoNotTouchInventory() {
        val inv = MemoryInventory(item())
        val invoices = MemoryInvoices()
        val line = InventorySalePolicy.createLine(inv.item, 1, 7_000_000).copy(
            inventoryItemId = "", inventoryQuantity = 0
        )
        SaveBarterInvoiceUseCase(invoices, EmptyCustomers(), unit, inv).save(invoice(line))
        assertEquals(5, inv.item.quantity)
        assertTrue(inv.history.isEmpty())
    }

    @Test fun linkedInvoiceSurvivesJsonRoundTripWithoutSchemaMigration() {
        val line = InventorySalePolicy.createLine(item(), 2, 7_000_000)
        val raw = PersistenceJsonCodecs.encodeBarterInvoices(listOf(invoice(line)))
        val restored = PersistenceJsonCodecs.decodeBarterInvoices(raw).single().salesItems.single()
        assertEquals("product", restored.inventoryItemId)
        assertEquals(2, restored.inventoryQuantity)
        val manual = InventorySalePolicy.createLine(item(), 1, 7_000_000).copy(
            inventoryItemId = "", inventoryQuantity = 0
        )
        assertEquals("", PersistenceJsonCodecs.decodeBarterInvoices(
            PersistenceJsonCodecs.encodeBarterInvoices(listOf(invoice(manual)))
        ).single().salesItems.single().inventoryItemId)
    }
}

