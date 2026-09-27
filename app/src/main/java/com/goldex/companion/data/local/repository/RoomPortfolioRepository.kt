package com.goldex.companion.data.local.repository

import com.goldex.companion.data.PortfolioItem
import com.goldex.companion.data.PortfolioStore
import com.goldex.companion.data.local.db.dao.PortfolioDao
import com.goldex.companion.data.local.db.mappers.toDomain
import com.goldex.companion.data.local.db.mappers.toEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import com.goldex.companion.data.sync.*

class RoomPortfolioRepository(
    private val portfolioDao: PortfolioDao,
    private val sync: RoomSyncUnitOfWork? = null
) : PortfolioStore {

    override fun getItems(): List<PortfolioItem> {
        return portfolioDao.queryAll().map { it.toDomain() }
    }

    override fun addItem(item: PortfolioItem) {
        write { portfolioDao.insertSync(item.toEntity()); sync?.changed("portfolio", item.id, SyncJson.record(item)) }
    }

    override fun deleteItem(id: String) {
        write { portfolioDao.deleteByIdSync(id); sync?.changed("portfolio", id, null) }
    }
    private fun write(action: () -> Unit) { if (sync == null) action() else sync.transaction(action) }

    // Reactive Flow extensions
    fun observeItems(): Flow<List<PortfolioItem>> {
        return portfolioDao.observeAll().map { list -> list.map { it.toDomain() } }
    }
}
