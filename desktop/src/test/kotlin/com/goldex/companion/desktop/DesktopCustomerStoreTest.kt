package com.goldex.companion.desktop

import com.goldex.companion.data.PersistenceJsonCodecs
import com.goldex.companion.desktop.data.*
import com.goldex.companion.domain.customers.*
import com.goldex.companion.model.*
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.nio.file.Files
import kotlin.test.*

class DesktopCustomerStoreTest {
    @get:Rule val temporary = TemporaryFolder()
    private fun customer() = Customer(id = "customer", name = "طرف حساب آزمون", createdAt = 1)
    private fun receipt() = LedgerTransaction(id = "receipt", customerId = "customer", title = "پرداخت آزمون",
        type = LedgerEntryType.CASH_RIAL, direction = LedgerDirection.PAY, amountTomans = 1_000, timestamp = 2)

    @Test fun olderWorkspaceOpensWithoutMigratingOrLosingUnrelatedData() {
        val path = temporary.newFolder().toPath()
        DesktopDataStore(path).use { store ->
            store.saveDarkTheme(true)
            val before = Files.readString(store.documentPath)
            assertTrue(DesktopCustomerStore(store).getCustomers().isEmpty())
            assertEquals(before, Files.readString(store.documentPath))
            DesktopCustomerStore(store).addCustomer(customer())
            assertTrue(store.loadDarkTheme())
        }
        DesktopDataStore(path).use { store -> assertEquals(listOf(customer()), DesktopCustomerStore(store).getCustomers()) }
    }

    @Test fun failedFinancialOperationRollsBackReceiptAndBalanceInMemoryAndOnDisk() {
        val path = temporary.newFolder().toPath()
        DesktopDataStore(path).use { store ->
            val customers = DesktopCustomerStore(store)
            customers.addCustomer(customer())
            val before = Files.readString(store.documentPath)
            assertFailsWith<IllegalStateException> { customers.transaction {
                customers.addTransaction(receipt())
                customers.updateCustomer(customer().copy(cashDebtTomans = 1_000))
                error("synthetic failure")
            } }
            assertEquals(before, Files.readString(store.documentPath))
            assertTrue(customers.getTransactions("customer").isEmpty())
            assertEquals(0L, customers.getCustomers().single().cashDebtTomans)
            customers.transaction { customers.addTransaction(receipt()); customers.updateCustomer(customer().copy(cashDebtTomans = 1_000)) }
            val backup = JSONObject(Files.readString(store.backupPath))
            assertFalse(backup.has("ledgerTransactions"))
            assertEquals(0L, backup.getJSONArray("customers").getJSONObject(0).getLong("cashDebtTomans"))
        }
        DesktopDataStore(path).use { store ->
            val customers = DesktopCustomerStore(store)
            assertEquals(1_000L, customers.getCustomers().single().cashDebtTomans)
            assertEquals(listOf(receipt()), customers.getTransactions("customer"))
        }
    }

    @Test fun goldCashSettlementUsesSharedPolicyIsIdempotentAndRetainsAgreedRate() {
        val path = temporary.newFolder().toPath()
        DesktopDataStore(path).use { store ->
            val customers = DesktopCustomerStore(store)
            val invoices = DesktopCustomerInvoiceStore(store, customers)
            val expected = customer().copy(goldDebtGrams = 2.125)
            customers.addCustomer(expected)
            val useCase = RecordCustomerSettlementUseCase(customers, invoices, customers)
            val request = SettlementRequest(id = "settlement", customerId = expected.id,
                targetType = LedgerEntryType.GOLD_WEIGHT, paymentType = LedgerEntryType.CASH_RIAL,
                amountTomans = 12_750_000, rateTomans = 6_000_000, rateSource = "دلخواه")
            val scope = OutstandingBalance(2.125, 0)
            val tx = useCase.record(request, expected, scope)
            assertEquals(tx, useCase.record(request, expected, scope))
            assertEquals(0.0, customers.getCustomers().single().goldDebtGrams)
            assertEquals(0L, customers.getCustomers().single().cashDebtTomans)
            assertEquals(-2.125, tx.balanceEffect().goldGrams)
            assertEquals(0L, tx.balanceEffect().cashTomans)
            assertEquals(6_000_000L, tx.settlement!!.rateTomans)
            assertEquals(1, customers.getTransactions(expected.id).size)
        }
        DesktopDataStore(path).use { store -> assertEquals(6_000_000L, DesktopCustomerStore(store).getTransactions("customer").single().settlement!!.rateTomans) }
    }

    @Test fun unknownCustomerReceiptAndSettlementFieldsSurviveUpdatesAndBackup() {
        val path = temporary.newFolder().toPath()
        DesktopDataStore(path).use { store ->
            val customers = DesktopCustomerStore(store)
            customers.addCustomer(customer())
            customers.addTransaction(receipt())
        }
        val file = path.resolve("workspace.json")
        val document = JSONObject(Files.readString(file))
        document.getJSONArray("customers").getJSONObject(0).put("futureContact", JSONObject().put("hint", "retained"))
        document.getJSONArray("ledgerTransactions").getJSONObject(0).put("futureReceipt", true)
        document.put("futureRoot", 42)
        Files.writeString(file, document.toString())
        DesktopDataStore(path).use { store ->
            val customers = DesktopCustomerStore(store)
            customers.updateCustomer(customer().copy(name = "نام جدید"))
            customers.updateTransaction(receipt().copy(note = "یادداشت جدید"))
            val saved = JSONObject(Files.readString(file))
            assertEquals("retained", saved.getJSONArray("customers").getJSONObject(0).getJSONObject("futureContact").getString("hint"))
            assertTrue(saved.getJSONArray("ledgerTransactions").getJSONObject(0).getBoolean("futureReceipt"))
            assertEquals(42, saved.getInt("futureRoot"))
            val export = path.resolve("backup.json")
            store.exportBackup(export)
            assertEquals(saved.toString(), JSONObject(Files.readString(export)).toString())
        }
    }

    @Test fun malformedRecordsNeverSilentlyDisappearAndOriginalFileIsPreserved() {
        val path = temporary.newFolder().toPath()
        DesktopDataStore(path).use { it.saveDarkTheme(false) }
        val file = path.resolve("workspace.json")
        val original = JSONObject(Files.readString(file)).put("customers", JSONArray().put(
            JSONObject(PersistenceJsonCodecs.encodeCustomerString(customer())).put("cashDebtTomans", "bad-money")))
        Files.writeString(file, original.toString())
        assertFailsWith<IllegalStateException> { DesktopDataStore(path) }
        assertEquals(original.toString(), Files.readString(file))
    }

    @Test fun contactDeletionRequiresSettlementAndRetainsAuditDataAndIdentity() {
        val path = temporary.newFolder().toPath()
        DesktopDataStore(path).use { store ->
            val customers = DesktopCustomerStore(store)
            customers.addCustomer(customer().copy(cashDebtTomans = 1_000))
            assertFailsWith<IllegalArgumentException> { customers.deleteCustomer("customer") }
            customers.updateCustomer(customer())
            customers.addTransaction(receipt())
            customers.deleteCustomer("customer")
            assertTrue(customers.getCustomers().isEmpty())
            assertEquals(listOf(receipt()), customers.getTransactions("customer"))
            assertEquals("customer", JSONObject(Files.readString(store.documentPath)).getJSONArray("deletedCustomers").getJSONObject(0).getString("id"))
        }
    }

    @Test fun allocatedInvoiceSettlementsPreserveNestedUnknownFieldsAndReversePaymentOnce() {
        val path = temporary.newFolder().toPath()
        val original = customer().copy(goldDebtGrams = 2.0)
        val request = SettlementRequest(id = "allocated", customerId = original.id, invoiceId = "invoice",
            targetType = LedgerEntryType.GOLD_WEIGHT, paymentType = LedgerEntryType.CASH_RIAL,
            amountTomans = 6_000_000, rateTomans = 6_000_000)
        DesktopDataStore(path).use { store ->
            val customers = DesktopCustomerStore(store)
            val invoices = DesktopCustomerInvoiceStore(store, customers)
            customers.addCustomer(original)
            invoices.saveBarterInvoice(BarterInvoice(id = "invoice", invoiceNumber = "1234", customer = original, createdAt = 1))
            customers.addTransaction(LedgerTransaction(id = "principal", customerId = original.id, invoiceId = "invoice",
                direction = LedgerDirection.PAY, scaleWeightGrams = 2.0, equivalent750WeightGrams = 2.0))
            RecordCustomerSettlementUseCase(customers, invoices, customers).record(request, original, OutstandingBalance(2.0, 0))
            assertEquals(1.0, customers.getCustomers().single().goldDebtGrams)
        }
        val file = path.resolve("workspace.json")
        val document = JSONObject(Files.readString(file))
        document.getJSONArray("barterInvoices").getJSONObject(0).getJSONArray("payments").getJSONObject(0).put("futurePayment", "retained")
        val entries = document.getJSONArray("ledgerTransactions")
        (0 until entries.length()).map { entries.getJSONObject(it) }.first { it.getString("id") == "allocated" }
            .getJSONObject("settlement").put("futureSettlement", true)
        Files.writeString(file, document.toString())
        DesktopDataStore(path).use { store ->
            val customers = DesktopCustomerStore(store)
            val invoices = DesktopCustomerInvoiceStore(store, customers)
            val current = customers.getCustomers().single()
            val useCase = RecordCustomerSettlementUseCase(customers, invoices, customers)
            useCase.record(request.copy(id = "second"), current, OutstandingBalance(1.0, 0))
            assertEquals(0.0, customers.getCustomers().single().goldDebtGrams)
            assertEquals(2, invoices.getBarterInvoices().single().payments.size)
            val saved = JSONObject(Files.readString(file)).getJSONArray("barterInvoices").getJSONObject(0).getJSONArray("payments")
            assertEquals("retained", saved.getJSONObject(0).getString("futurePayment"))
            assertTrue(customers.getTransactions(original.id).first { it.id == "allocated" }.settlement!!.unknownFieldsJson.contains("futureSettlement"))
            val scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.Unconfined)
            try {
                val manager = com.goldex.companion.desktop.customers.CustomerManagerViewModel(customers, customers, invoices, scope, kotlinx.coroutines.Dispatchers.Unconfined)
                val first = customers.getTransactions(original.id).first { it.id == "allocated" }
                manager.deleteLedgerEntry(first)
                manager.deleteLedgerEntry(first)
                assertEquals(1.0, customers.getCustomers().single().goldDebtGrams)
                assertEquals(listOf("second"), invoices.getBarterInvoices().single().payments.map { it.id })
                assertEquals(1.0, invoiceOutstanding(customers.getTransactionsByInvoiceId("invoice")).goldGrams)
            } finally { scope.coroutineContext[kotlinx.coroutines.Job]?.cancel() }
        }
    }
}
