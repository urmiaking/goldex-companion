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

class RoomInvoiceRepository(
    private val barterDao: BarterInvoiceDao,
    private val classicDao: ClassicInvoiceDao
) : InvoiceStore {

    override fun getInvoices(): List<Invoice> {
        return classicDao.queryAll().map { it.toDomain() }
    }

    override fun saveInvoice(invoice: Invoice) {
        classicDao.insertSync(invoice.toEntity())
    }

    override fun deleteInvoice(id: String) {
        classicDao.deleteByIdSync(id)
    }

    override fun getBarterInvoices(): List<BarterInvoice> {
        return barterDao.queryAll().map { it.toDomain() }
    }

    override fun saveBarterInvoice(invoice: BarterInvoice) {
        barterDao.insertSync(invoice.toEntity())
    }

    override fun deleteBarterInvoice(id: String) {
        barterDao.deleteByIdSync(id)
    }

    // Reactive Flow extensions
    fun observeBarterInvoices(): Flow<List<BarterInvoice>> {
        return barterDao.observeAll().map { list -> list.map { it.toDomain() } }
    }

    fun observeInvoices(): Flow<List<Invoice>> {
        return classicDao.observeAll().map { list -> list.map { it.toDomain() } }
    }
}
