package com.goldex.companion.data.local

import androidx.test.core.app.ApplicationProvider
import com.goldex.companion.data.local.db.GoldexDatabase
import com.goldex.companion.data.local.db.entities.LedgerTransactionEntity
import com.goldex.companion.data.local.repository.RoomCustomerRepository
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class LargeLedgerBulkReadTest {
    @Test fun twoThousandInvoiceIdsAreChunkedWithoutMissingOrDuplicatingRecords() {
        val db = GoldexDatabase.buildInMemory(ApplicationProvider.getApplicationContext())
        try {
            val ids = (0 until 2000).map { "invoice-$it" }
            db.ledgerTransactionDao().insertAllSync(ids.map {
                LedgerTransactionEntity(id = "tx-$it", customerId = "c", invoiceId = it)
            } + LedgerTransactionEntity(id = "unrelated", customerId = "c", invoiceId = "outside"))
            val store = RoomCustomerRepository(db.customerDao(), db.ledgerTransactionDao())
            val records = store.getTransactionsByInvoiceIds(ids + ids.take(5))
            assertEquals(2000, records.size)
            assertEquals(ids.toSet(), records.map { it.invoiceId }.toSet())
            assertTrue(store.getTransactionsByInvoiceIds(emptyList()).isEmpty())
        } finally { db.close() }
    }
}
