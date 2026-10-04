package com.goldex.companion.data

import androidx.test.core.app.ApplicationProvider
import com.goldex.companion.data.local.db.GoldexDatabase
import com.goldex.companion.data.local.db.mappers.*
import com.goldex.companion.data.local.repository.*
import com.goldex.companion.data.sync.*
import com.goldex.companion.domain.customers.*
import com.goldex.companion.domain.invoice.*
import com.goldex.companion.model.*
import com.goldex.companion.ui.customers.CustomerSettlementViewModel
import com.goldex.companion.ui.invoices.CustomerManagerViewModel
import org.json.JSONArray
import org.json.JSONObject
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class CustomerSettlementIntegrationTest {
    private lateinit var db: GoldexDatabase
    private lateinit var unit: RoomSyncUnitOfWork
    private lateinit var customers: RoomCustomerRepository
    private lateinit var invoices: RoomInvoiceRepository
    private lateinit var record: RecordCustomerSettlementUseCase
    private val customer = Customer(id = "customer", name = "test", goldDebtGrams = 10.0, createdAt = 1)
    private val scope = OutstandingBalance(10.0, 0)
    private fun request() = SettlementRequest(id = "receipt", customerId = customer.id, targetType = LedgerEntryType.GOLD_WEIGHT,
        paymentType = LedgerEntryType.CASH_RIAL, amountTomans = 500_000_000, rateTomans = 100_000_000, rateSource = "دلخواه")

    @Before fun setup() {
        db = GoldexDatabase.buildInMemory(ApplicationProvider.getApplicationContext())
        unit = RoomSyncUnitOfWork(db)
        customers = RoomCustomerRepository(db.customerDao(), db.ledgerTransactionDao(), unit)
        invoices = RoomInvoiceRepository(db.barterInvoiceDao(), db.classicInvoiceDao(), unit)
        record = RecordCustomerSettlementUseCase(customers, invoices, unit)
        customers.addCustomer(customer)
    }
    @After fun close() { db.close() }

    @Test fun settlementRoundTripsInRoomJsonAndCloudAndDoubleSubmitIsIdempotent() {
        val tx = record.record(request(), customer, scope)
        assertEquals(tx, customers.getTransactions(customer.id).single())
        assertEquals(tx, PersistenceJsonCodecs.decodeLedgerTransactions(PersistenceJsonCodecs.encodeLedgerTransactions(listOf(tx))).single())
        val cloud = SyncJson.record(tx)
        assertEquals("100000000", cloud.getJSONObject("settlement").getString("rateTomans"))
        assertEquals(tx, PersistenceJsonCodecs.decodeLedgerTransactions(JSONArray().put(cloud).toString()).single())
        val changes = JSONObject(db.syncDao().allOutbox().last().payload).getJSONArray("changes")
        assertEquals(setOf("customer", "ledger"), (0 until changes.length()).map { changes.getJSONObject(it).getString("type") }.toSet())
        val count = db.syncDao().pendingCount()
        assertEquals(tx, record.record(request(), customer, scope))
        assertEquals(count, db.syncDao().pendingCount())
        assertEquals(5.0, customers.getCustomers().single().goldDebtGrams, 0.0)
        assertEquals(0L, customers.getCustomers().single().cashDebtTomans)
    }

    @Test fun failureAfterReceiptWriteRollsBackLedgerCustomerAndOutbox() {
        val failing = object : CustomerStore by customers {
            override fun updateCustomer(customer: Customer) { customers.updateCustomer(customer); error("interrupted") }
        }
        val count = db.syncDao().pendingCount()
        assertThrows(IllegalStateException::class.java) { RecordCustomerSettlementUseCase(failing, invoices, unit).record(request(), customer, scope) }
        assertTrue(customers.getTransactions(customer.id).isEmpty())
        assertEquals(customer, customers.getCustomers().single())
        assertEquals(count, db.syncDao().pendingCount())
    }

    @Test fun deleteSettlementReversesBothOffsetEffectsAndDoesNotReverseTwice() {
        val c = customer.copy(cashDebtTomans = -500_000_000)
        customers.updateCustomer(c)
        val tx = record.record(request().copy(offsetExistingCredit = true), c, OutstandingBalance(10.0, -500_000_000))
        val vm = CustomerManagerViewModel(customers, unit)
        vm.deleteLedgerEntry(tx)
        vm.deleteLedgerEntry(tx)
        assertEquals(c.goldDebtGrams, customers.getCustomers().single().goldDebtGrams, 0.0)
        assertEquals(c.cashDebtTomans, customers.getCustomers().single().cashDebtTomans)
    }

    @Test fun invoiceEditPreservesDatedSettlementAndDeletionReversesAllRecordedEffects() {
        customers.updateCustomer(customer.copy(goldDebtGrams = 0.0))
        val original = customers.getCustomers().single()
        val item = MeltGoldItem(id = "item", weight = 10.0, spotPrice = 100_000_000,
            totalPayable = 1_000_000_000.0, equivalent18kWeight = 10.0)
        val invoice = BarterInvoice(id = "invoice", customer = original, salesItems = listOf(item), spotPrice18k = 100_000_000,
            settlementMethod = SettlementMethod.LEDGER, createdAt = 1)
        SaveBarterInvoiceUseCase(invoices, customers, unit).save(invoice)
        val now = customers.getCustomers().single()
        val tx = record.record(request().copy(invoiceId = invoice.id), now, invoiceOutstanding(customers.getTransactionsByInvoiceId(invoice.id)))
        SaveBarterInvoiceUseCase(invoices, customers, unit).save(invoice.copy(note = "edited", spotPrice18k = 200_000_000))
        assertEquals(tx, customers.getTransactionsByInvoiceId(invoice.id).first { it.settlement != null })
        assertEquals(5.0, customers.getCustomers().single().goldDebtGrams, 0.0)
        assertEquals(OutstandingBalance(5.0, 0), invoiceOutstanding(customers.getTransactionsByInvoiceId(invoice.id)))
        assertTrue(InvoiceDeletionUseCase(invoices, customers, unit).delete(invoice.id) is InvoiceDeletionResult.Deleted)
        assertEquals(0.0, customers.getCustomers().single().goldDebtGrams, 0.0)
        assertEquals(0L, customers.getCustomers().single().cashDebtTomans)
    }

    @Test fun staleCustomerOrInvoiceCannotCommitAgainstAnOldPreview() {
        customers.updateCustomer(customer.copy(goldDebtGrams = 9.0))
        val count = db.syncDao().pendingCount()
        assertThrows(IllegalArgumentException::class.java) { record.record(request(), customer, scope) }
        assertTrue(customers.getTransactions(customer.id).isEmpty())
        assertEquals(count, db.syncDao().pendingCount())
    }

    @Test fun offlineQuotePersianInputsAndCustomRateProduceCorrectPreview() {
        val vm = CustomerSettlementViewModel(customers, invoices, record) { MarketRates(gold18 = 100_000_000, isLive = false) }
        vm.open(customer)
        assertFalse(vm.state.value.isMarketRate)
        assertEquals("", vm.state.value.rateInput)
        vm.setRate("۱۰۰٬۰۰۰٬۰۰۰"); vm.setAmount("۵۰۰٬۰۰۰٬۰۰۰")
        assertEquals(5.0, vm.state.value.preview!!.customerAfter.goldDebtGrams, 0.0)
        assertTrue(vm.confirm())
        assertNull(vm.state.value.customer)
        assertEquals(0L, customers.getCustomers().single().cashDebtTomans)
    }

    @Test fun marketSnapshotDoesNotFollowLaterPriceChangesAndInvalidPasteCannotSubmit() {
        var quote = MarketRates(gold18 = 100_000_000, isLive = true, lastUpdated = "12:00:00")
        val vm = CustomerSettlementViewModel(customers, invoices, record) { quote }
        vm.open(customer); vm.setAmount("500000000")
        quote = quote.copy(gold18 = 105_000_000)
        assertEquals(5.0, vm.state.value.preview!!.customerAfter.goldDebtGrams, 0.0)
        vm.useMarketRate()
        assertEquals(10.0 - 500_000_000.0 / 105_000_000, vm.state.value.preview!!.customerAfter.goldDebtGrams, 1e-12)
        vm.setAmount("500.5")
        assertNull(vm.state.value.preview)
        assertFalse(vm.confirm())
    }

    @Test fun legacyLedgerJsonAndEntityKeepOriginalBalanceEffects() {
        val old = """[{"id":"old","customerId":"customer","type":"CASH_RIAL","amountTomans":500,"future":"keep"}]"""
        val tx = PersistenceJsonCodecs.decodeLedgerTransactions(old).single()
        assertNull(tx.settlement)
        assertEquals(LedgerEffect(cashTomans = -500), tx.balanceEffect())
        assertEquals(tx, tx.toEntity().toDomain())
    }

    @Test fun unknownSettlementFieldsSurviveAndMalformedMetadataCannotBecomeLegacyReceipt() {
        val tx = record.record(request(), customer, scope)
        val payload = SyncJson.record(tx)
        payload.getJSONObject("settlement").put("futurePolicy", JSONObject().put("name", "future"))
        val decoded = PersistenceJsonCodecs.decodeLedgerTransactions(JSONArray().put(payload).toString()).single()
        assertEquals("future", SyncJson.record(decoded).getJSONObject("settlement").getJSONObject("futurePolicy").getString("name"))
        payload.getJSONObject("settlement").put("cashDeltaTomans", "1.5")
        assertThrows(ArithmeticException::class.java) { PersistenceJsonCodecs.decodeLedgerTransactions(JSONArray().put(payload).toString()) }
        payload.put("settlement", "broken")
        assertThrows(org.json.JSONException::class.java) { PersistenceJsonCodecs.decodeLedgerTransactions(JSONArray().put(payload).toString()) }
    }

    @Test fun versionTwoMigrationPreservesBalancesLedgerAndPendingSyncWithoutRewriting() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val name = "settlement-migration-test.db"
        context.deleteDatabase(name)
        val schema = JSONObject(java.io.File("schemas/com.goldex.companion.data.local.db.GoldexDatabase/2.json").readText()).getJSONObject("database")
        val old = context.openOrCreateDatabase(name, android.content.Context.MODE_PRIVATE, null)
        val entities = schema.getJSONArray("entities")
        for (i in 0 until entities.length()) {
            val entity = entities.getJSONObject(i)
            val table = entity.getString("tableName")
            old.execSQL(entity.getString("createSql").replace("\u0024{TABLE_NAME}", table))
            val indices = entity.getJSONArray("indices")
            for (j in 0 until indices.length()) old.execSQL(indices.getJSONObject(j).getString("createSql").replace("\u0024{TABLE_NAME}", table))
            val fixture: Any = when (table) {
                "customers" -> customer.toEntity()
                "ledger_transactions" -> LedgerTransaction(id = "legacy", customerId = customer.id, timestamp = 1,
                    direction = LedgerDirection.PAY, equivalent750WeightGrams = 10.0).toEntity()
                else -> continue
            }
            val values = android.content.ContentValues()
            val fields = entity.getJSONArray("fields")
            for (j in 0 until fields.length()) {
                val field = fields.getJSONObject(j)
                val value = fixture.javaClass.getDeclaredField(field.getString("fieldPath")).also { it.isAccessible = true }.get(fixture)
                when (value) {
                    null -> values.putNull(field.getString("columnName"))
                    is Boolean -> values.put(field.getString("columnName"), if (value) 1 else 0)
                    is Long -> values.put(field.getString("columnName"), value)
                    is Int -> values.put(field.getString("columnName"), value)
                    is Double -> values.put(field.getString("columnName"), value)
                    else -> values.put(field.getString("columnName"), value.toString())
                }
            }
            old.insertOrThrow(table, null, values)
        }
        old.execSQL("INSERT INTO sync_outbox(operationId, sequence, workspaceId, payload, status) VALUES('pending',1,'workspace','{\"changes\":[]}','PENDING')")
        old.version = 2
        old.close()
        val migrated = androidx.room.Room.databaseBuilder(context, GoldexDatabase::class.java, name)
            .addMigrations(com.goldex.companion.data.local.db.migration.SETTLEMENT_MIGRATION_2_3, com.goldex.companion.data.local.db.migration.INVOICE_DEBT_MIGRATION_3_4).allowMainThreadQueries().build()
        try {
            assertEquals(customer, migrated.customerDao().queryAll().single().toDomain())
            val ledger = migrated.ledgerTransactionDao().queryAll().single()
            assertEquals("legacy", ledger.id)
            assertEquals("", ledger.settlementJson)
            assertNull(ledger.toDomain().settlement)
            assertEquals("pending", migrated.syncDao().allOutbox().single().operationId)
            assertEquals("{\"changes\":[]}", migrated.syncDao().allOutbox().single().payload)
        } finally { migrated.close(); context.deleteDatabase(name) }
    }

    @Test fun invoiceOffsetConsumesItsEarlierCashCreditAndFullSettlementClearsListStatus() {
        val c = customer.copy(goldDebtGrams = 10.0, cashDebtTomans = -500_000_000)
        customers.updateCustomer(c)
        invoices.saveBarterInvoice(BarterInvoice(id = "old-invoice", customer = customer))
        customers.addTransaction(LedgerTransaction(customerId = c.id, invoiceId = "old-invoice", direction = LedgerDirection.PAY,
            equivalent750WeightGrams = 10.0))
        customers.addTransaction(LedgerTransaction(customerId = c.id, invoiceId = "old-invoice", type = LedgerEntryType.CASH_RIAL,
            amountTomans = 500_000_000))
        val first = request().copy(invoiceId = "old-invoice", offsetExistingCredit = true)
        record.record(first, c, invoiceOutstanding(customers.getTransactionsByInvoiceId("old-invoice")))
        assertEquals(OutstandingBalance(5.0, 0), invoiceOutstanding(customers.getTransactionsByInvoiceId("old-invoice")))
        val next = customers.getCustomers().single()
        record.record(request().copy(id = "second", invoiceId = "old-invoice"), next, OutstandingBalance(5.0, 0))
        val vm = com.goldex.companion.ui.invoices.BarterInvoiceViewModel(invoices, customers, unit)
        assertEquals(InvoiceStatus.SETTLED, vm.uiState.value.invoicesList.single().status)
        assertTrue(invoiceOutstanding(customers.getTransactionsByInvoiceId("old-invoice")).isSettled)
    }

    @Test fun invoiceOverpaymentKeepsCustomerCreditWithoutReopeningTheInvoice() {
        invoices.saveBarterInvoice(BarterInvoice(id = "invoice", customer = customer))
        customers.addTransaction(LedgerTransaction(customerId = customer.id, invoiceId = "invoice", direction = LedgerDirection.PAY,
            equivalent750WeightGrams = 10.0))
        record.record(request().copy(amountTomans = 1_050_000_000, invoiceId = "invoice"), customer, scope)
        assertEquals(-50_000_000L, customers.getCustomers().single().cashDebtTomans)
        assertTrue(invoiceOutstanding(customers.getTransactionsByInvoiceId("invoice")).isSettled)
    }

    @Test fun invoiceSettlementAppendsPaymentItemToBarterInvoice() {
        val invoice = BarterInvoice(id = "inv-settle", customer = customer)
        invoices.saveBarterInvoice(invoice)
        customers.addTransaction(LedgerTransaction(customerId = customer.id, invoiceId = "inv-settle", direction = LedgerDirection.PAY,
            equivalent750WeightGrams = 10.0))
        val tx = record.record(request().copy(invoiceId = "inv-settle"), customer, scope)
        val updated = invoices.getBarterInvoices().first { it.id == "inv-settle" }
        assertEquals(1, updated.payments.size)
        assertEquals(tx.id, updated.payments.single().id)
        assertEquals(500_000_000L, updated.payments.single().amountTomans)
    }
    private fun contractualInvoice(basis: InvoiceDebtBasis = InvoiceDebtBasis.GOLD): BarterInvoice {
        val owner = customer.copy(goldDebtGrams = 0.0)
        customers.updateCustomer(owner)
        return BarterInvoice(id = "contract", customer = owner, customerRole = CustomerRole.RETAIL,
            debtBasis = basis, spotPrice18k = 20_000_000,
            salesItems = listOf(MeltGoldItem(id = "ring", weight = 10.0, spotPrice = 20_000_000,
                totalPayable = 200_000_000.0, equivalent18kWeight = 10.0)),
            payments = listOf(SettlementPaymentItem(id = "initial", method = SettlementMethod.POS, amountTomans = 100_000_000)))
    }

    @Test fun fiveGramDebtSurvivesResavesAndDated110MillionPaymentAndDeletion() {
        val invoice = contractualInvoice()
        val save = SaveBarterInvoiceUseCase(invoices, customers, unit)
        repeat(3) { save.save(invoice) }
        assertEquals(OutstandingBalance(5.0, 0), invoiceOutstanding(customers.getTransactionsByInvoiceId(invoice.id)))
        val vm = CustomerSettlementViewModel(customers, invoices, record) { MarketRates(gold18 = 22_000_000, isLive = true, lastUpdated = "12:00") }
        vm.open(customers.getCustomers().single(), invoice.id)
        vm.fillRemaining()
        assertEquals("110000000", vm.state.value.amountInput)
        assertTrue(vm.confirm())
        val receipt = customers.getTransactionsByInvoiceId(invoice.id).single { !it.isAutoGenerated }
        val saved = invoices.getBarterInvoices().single()
        assertEquals(22_000_000L, saved.payments.last().settlement!!.rateTomans)
        assertTrue(saved.isFullySettled)
        repeat(3) { save.save(saved.copy(note = "edited")) }
        assertEquals(receipt, customers.getTransactionsByInvoiceId(invoice.id).single { !it.isAutoGenerated })
        assertEquals(0.0, customers.getCustomers().single().goldDebtGrams, 0.0)
        assertEquals(0L, customers.getCustomers().single().cashDebtTomans)
        assertEquals(InvoiceStatus.SETTLED, com.goldex.companion.ui.invoices.BarterInvoiceViewModel(invoices, customers, unit).uiState.value.invoicesList.single().status)
        CustomerManagerViewModel(customers, unit, invoices).deleteLedgerEntry(receipt)
        assertEquals(5.0, customers.getCustomers().single().goldDebtGrams, 0.0)
        assertEquals(listOf("initial"), invoices.getBarterInvoices().single().payments.map { it.id })
        save.save(invoices.getBarterInvoices().single())
        assertEquals(5.0, customers.getCustomers().single().goldDebtGrams, 0.0)
        assertTrue(InvoiceDeletionUseCase(invoices, customers, unit).delete(invoice.id) is InvoiceDeletionResult.Deleted)
        assertEquals(0.0, customers.getCustomers().single().goldDebtGrams, 0.0)
    }

    @Test fun debtBasisAndDatedRateSurviveRoomBackupAndCloudWithoutRepricing() {
        val invoice = contractualInvoice()
        SaveBarterInvoiceUseCase(invoices, customers, unit).save(invoice)
        val fresh = customers.getCustomers().single()
        record.record(request().copy(invoiceId = invoice.id, rateTomans = 22_000_000, amountTomans = 44_000_000), fresh, OutstandingBalance(5.0, 0))
        val saved = invoices.getBarterInvoices().single()
        assertEquals(3.0, saved.remainingBalanceGold18k, 0.0)
        assertEquals(saved, saved.toEntity().toDomain())
        assertEquals(saved, PersistenceJsonCodecs.decodeBarterInvoices(PersistenceJsonCodecs.encodeBarterInvoices(listOf(saved))).single())
        assertEquals(saved, PersistenceJsonCodecs.decodeBarterInvoices(JSONArray().put(SyncJson.record(saved)).toString()).single())
        val old = SyncJson.record(saved).apply { remove("debtBasis"); remove("payments") }
        assertNull(PersistenceJsonCodecs.decodeBarterInvoices(JSONArray().put(old).toString()).single().debtBasis)
        old.put("debtBasis", "FUTURE_CONTRACT")
        assertThrows(IllegalArgumentException::class.java) { PersistenceJsonCodecs.decodeBarterInvoices(JSONArray().put(old).toString()) }
    }

    @Test fun delayedCoinScrapAndBullionPaymentsConsumeGoldAtTheirRecordedTerms() {
        val invoice = contractualInvoice()
        SaveBarterInvoiceUseCase(invoices, customers, unit).save(invoice)
        val vm = CustomerSettlementViewModel(customers, invoices, record) { MarketRates(isLive = false) }
        vm.open(customers.getCustomers().single(), invoice.id)
        vm.setMethod("سکه (ارزش توافقی)"); vm.setRate("22000000"); vm.setAmount("44000000")
        assertTrue(vm.confirm()) // 2g by agreed coin value, leaves 3g
        vm.open(customers.getCustomers().single(), invoice.id)
        vm.selectPayment(LedgerEntryType.GOLD_WEIGHT); vm.setMethod("طلای مستعمل"); vm.setWeight("1.5"); vm.setKarat("500")
        assertTrue(vm.confirm()) // 1g equivalent after fineness
        vm.open(customers.getCustomers().single(), invoice.id)
        vm.selectPayment(LedgerEntryType.GOLD_WEIGHT); vm.setMethod("شمش / آبشده"); vm.setWeight("2"); vm.setKarat("750")
        assertTrue(vm.confirm())
        val entries = customers.getTransactionsByInvoiceId(invoice.id)
        assertTrue(invoiceOutstanding(entries).isSettled)
        assertEquals(0.0, customers.getCustomers().single().goldDebtGrams, 0.0)
        assertEquals(setOf("سکه (ارزش توافقی)", "طلای مستعمل", "شمش / آبشده"), entries.filterNot { it.isAutoGenerated }.map { it.paymentMethod }.toSet())
    }

    @Test fun legacyRoleAndExplicitDebtBasisCannotSilentlyChangeOnSave() {
        val invoice = contractualInvoice()
        val save = SaveBarterInvoiceUseCase(invoices, customers, unit)
        save.save(invoice)
        assertThrows(IllegalArgumentException::class.java) { save.save(invoice.copy(debtBasis = InvoiceDebtBasis.CASH)) }
        assertEquals(invoice, invoices.getBarterInvoices().single())
        assertEquals(5.0, customers.getCustomers().single().goldDebtGrams, 0.0)
        val vm = com.goldex.companion.ui.invoices.BarterInvoiceViewModel(invoices, customers, unit) { 22_000_000 }
        vm.openNewInvoice(); vm.setDebtBasis(InvoiceDebtBasis.GOLD); vm.setCustomer(customer.copy(role = "مشتری عادی"))
        assertEquals(InvoiceDebtBasis.GOLD, vm.uiState.value.invoice.debtBasis)
        vm.openInvoiceDetails(vm.uiState.value.invoicesList.single()); vm.setLiveRate(99_000_000)
        assertEquals(20_000_000L, vm.uiState.value.invoice.spotPrice18k)
    }

    @Test fun legacyCrossUnitPaymentIsNotDuplicatedBySavingAgain() {
        val invoice = contractualInvoice().copy(debtBasis = null, customerRole = CustomerRole.WHOLESALER)
        val save = SaveBarterInvoiceUseCase(invoices, customers, unit)
        repeat(3) { save.save(invoice) }
        assertEquals(5.0, customers.getCustomers().single().goldDebtGrams, 0.0)
        assertEquals(0L, customers.getCustomers().single().cashDebtTomans)
        assertEquals(OutstandingBalance(5.0, 0), invoiceOutstanding(customers.getTransactionsByInvoiceId(invoice.id)))
    }

    @Test fun migrationThreeToFourPreservesAllFinancialRowsAndPendingOutbox() {
        val legacy = contractualInvoice().copy(debtBasis = null)
        SaveBarterInvoiceUseCase(invoices, customers, unit).save(legacy)
        val beforeCustomer = customers.getCustomers().single()
        val beforeEntries = customers.getTransactions(customer.id)
        val beforeOutbox = db.syncDao().allOutbox()
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val name = "debt-basis-migration-test.db"
        context.deleteDatabase(name)
        val old = context.openOrCreateDatabase(name, android.content.Context.MODE_PRIVATE, null)
        val schema = JSONObject(java.io.File("schemas/com.goldex.companion.data.local.db.GoldexDatabase/3.json").readText()).getJSONObject("database")
        val entities = schema.getJSONArray("entities")
        for (i in 0 until entities.length()) {
            val entity = entities.getJSONObject(i)
            val table = entity.getString("tableName")
            old.execSQL(entity.getString("createSql").replace("\u0024{TABLE_NAME}", table))
            val indices = entity.getJSONArray("indices")
            for (j in 0 until indices.length()) old.execSQL(indices.getJSONObject(j).getString("createSql").replace("\u0024{TABLE_NAME}", table))
            db.openHelper.readableDatabase.query("SELECT * FROM " + table).use { cursor ->
                while (cursor.moveToNext()) {
                    val values = android.content.ContentValues()
                    cursor.columnNames.forEachIndexed { index, column ->
                        if (column != "debtBasis") when (cursor.getType(index)) {
                            android.database.Cursor.FIELD_TYPE_NULL -> values.putNull(column)
                            android.database.Cursor.FIELD_TYPE_INTEGER -> values.put(column, cursor.getLong(index))
                            android.database.Cursor.FIELD_TYPE_FLOAT -> values.put(column, cursor.getDouble(index))
                            else -> values.put(column, cursor.getString(index))
                        }
                    }
                    old.insertOrThrow(table, null, values)
                }
            }
        }
        old.version = 3; old.close()
        val migrated = androidx.room.Room.databaseBuilder(context, GoldexDatabase::class.java, name)
            .addMigrations(com.goldex.companion.data.local.db.migration.INVOICE_DEBT_MIGRATION_3_4).allowMainThreadQueries().build()
        try {
            assertEquals(legacy, migrated.barterInvoiceDao().queryAll().single().toDomain())
            assertEquals(beforeCustomer, migrated.customerDao().queryAll().single().toDomain())
            assertEquals(beforeEntries.toSet(), migrated.ledgerTransactionDao().queryAll().map { it.toDomain() }.toSet())
            assertEquals(beforeOutbox, migrated.syncDao().allOutbox())
        } finally { migrated.close(); context.deleteDatabase(name) }
    }

    @Test fun firstDatedReceiptPreservesImplicitInitialPaymentAcrossResaves() {
        val invoice = contractualInvoice().copy(payments = emptyList(), cashPosAmount = 100_000_000)
        val save = SaveBarterInvoiceUseCase(invoices, customers, unit)
        save.save(invoice)
        record.record(request().copy(invoiceId = invoice.id, amountTomans = 44_000_000, rateTomans = 22_000_000),
            customers.getCustomers().single(), OutstandingBalance(5.0, 0))
        val stored = invoices.getBarterInvoices().single()
        assertEquals(2, stored.payments.size)
        assertEquals(100_000_000L, stored.payments.first().amountTomans)
        repeat(2) { save.save(stored) }
        assertEquals(OutstandingBalance(3.0, 0), invoiceOutstanding(customers.getTransactionsByInvoiceId(invoice.id)))
        assertEquals(3.0, customers.getCustomers().single().goldDebtGrams, 0.0)
    }

}
