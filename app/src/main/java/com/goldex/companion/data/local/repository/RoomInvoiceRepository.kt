package com.goldex.companion.data.local.repository

import com.goldex.companion.data.InvoiceStore
import com.goldex.companion.data.local.db.dao.BarterInvoiceDao
import com.goldex.companion.data.local.db.dao.ClassicInvoiceDao
import com.goldex.companion.data.local.db.mappers.toDomain
import com.goldex.companion.data.local.db.mappers.toEntity
import com.goldex.companion.model.BarterInvoice
import com.goldex.companion.model.Invoice
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import com.goldex.companion.data.sync.*

class RoomInvoiceRepository(
    private val barterDao: BarterInvoiceDao,
    private val classicDao: ClassicInvoiceDao,
    private val sync: RoomSyncUnitOfWork? = null
) : InvoiceStore {

    override fun getInvoices(): List<Invoice> {
        return classicDao.queryAll().map { it.toDomain() }
    }

    override fun saveInvoice(invoice: Invoice) {
        write { classicDao.insertSync(invoice.toEntity()); sync?.changed("invoice", invoice.id, SyncJson.record(invoice)) }
    }

    override fun deleteInvoice(id: String) {
        write { classicDao.deleteByIdSync(id); sync?.changed("invoice", id, null) }
    }

    override fun getBarterInvoices(): List<BarterInvoice> {
        return barterDao.queryAll().map { it.toDomain() }
    }

    override fun saveBarterInvoice(invoice: BarterInvoice) {
        write { barterDao.insertSync(invoice.toEntity()); sync?.changed("barterInvoice", invoice.id, SyncJson.record(invoice)) }
    }

    override fun deleteBarterInvoice(id: String) {
        write { barterDao.deleteByIdSync(id); sync?.changed("barterInvoice", id, null) }
    }
    private fun write(action: () -> Unit) { if (sync == null) action() else sync.transaction(action) }

    // Reactive Flow extensions
    fun observeBarterInvoices(): Flow<List<BarterInvoice>> {
        return barterDao.observeAll().map { list -> list.map { it.toDomain() } }
    }

    fun observeInvoices(): Flow<List<Invoice>> {
        return classicDao.observeAll().map { list -> list.map { it.toDomain() } }
    }
}
