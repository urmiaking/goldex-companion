package com.goldex.companion.data.local.repository

import com.goldex.companion.data.PortfolioItem
import com.goldex.companion.data.PortfolioStore
import com.goldex.companion.data.local.db.dao.PortfolioDao
import com.goldex.companion.data.local.db.mappers.toDomain
import com.goldex.companion.data.local.db.mappers.toEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomPortfolioRepository(
    private val portfolioDao: PortfolioDao
) : PortfolioStore {

    override fun getItems(): List<PortfolioItem> {
        return portfolioDao.queryAll().map { it.toDomain() }
    }

    override fun addItem(item: PortfolioItem) {
        portfolioDao.insertSync(item.toEntity())
    }

    override fun deleteItem(id: String) {
        portfolioDao.deleteByIdSync(id)
    }

    // Reactive Flow extensions
    fun observeItems(): Flow<List<PortfolioItem>> {
        return portfolioDao.observeAll().map { list -> list.map { it.toDomain() } }
    }
}
