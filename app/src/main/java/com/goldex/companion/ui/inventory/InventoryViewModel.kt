package com.goldex.companion.ui.inventory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.goldex.companion.ui.util.FeatureWorkQueue
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import com.goldex.companion.data.InventoryStore
import com.goldex.companion.model.InventoryCategory
import com.goldex.companion.model.InventoryItem
import com.goldex.companion.model.StockAdjustment
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class InventoryUiState(
    val items: List<InventoryItem> = emptyList(),
    val selectedCategory: InventoryCategory = InventoryCategory.ALL,
    val searchQuery: String = "",
    val isAddModalOpen: Boolean = false,
    val isAdjustModalOpen: Boolean = false,
    val selectedItemForAdjustment: InventoryItem? = null,
    val isInventoryVisible: Boolean = false,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null,
    val summary: InventoryListSummary = InventoryListSummary(items)
) {
    val filteredItems: List<InventoryItem>
        by lazy { items.filter { item ->
            val matchesCategory = (selectedCategory == InventoryCategory.ALL || item.category == selectedCategory)
            val matchesSearch = searchQuery.isBlank() ||
                item.title.contains(searchQuery, ignoreCase = true) ||
                item.code.contains(searchQuery, ignoreCase = true) ||
                item.rfidTag.contains(searchQuery, ignoreCase = true) ||
                item.location.contains(searchQuery, ignoreCase = true)
            matchesCategory && matchesSearch
        } }

    val totalGoldWeight18k: Double get() = summary.goldWeight18k
    val totalMesghalEquivalent: Double get() = if (summary.goldWeight18k > 0) summary.goldWeight18k / 4.3318 else 0.0
    val totalPiecesCount: Int get() = summary.pieces
    val totalItemCount: Int get() = items.size
    val activeTraysCount: Int get() = summary.trays
    val activeSafesCount: Int get() = summary.safes
    fun categoryCount(category: InventoryCategory): Int = if (category == InventoryCategory.ALL) summary.pieces else summary.categories[category] ?: 0

}

class InventoryViewModel(
    private val repository: InventoryStore,
    private val workDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {

    private val _uiState = MutableStateFlow(InventoryUiState())
    val uiState: StateFlow<InventoryUiState> = _uiState.asStateFlow()

    private val work = FeatureWorkQueue(viewModelScope, workDispatcher,
        onBusy = { busy -> _uiState.update { it.copy(isLoading = busy, isSaving = if (busy) it.isSaving else false) } },
        onError = { _ -> _uiState.update { it.copy(error = "عملیات انبار انجام نشد؛ دوباره تلاش کنید") } })

    init { loadItems() }

    private fun refreshItems() {
        val items = repository.getItems()
        val summary = InventoryListSummary(items)
        _uiState.update { it.copy(items = items, summary = summary, error = null) }
    }

    fun loadItems() { work.submit { refreshItems() } }

    fun setInventoryVisible(visible: Boolean) {
        _uiState.update { it.copy(isInventoryVisible = visible) }
    }

    fun selectCategory(category: InventoryCategory) {
        _uiState.update { it.copy(selectedCategory = category) }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun openAddModal() {
        _uiState.update { it.copy(isAddModalOpen = true) }
    }

    fun closeAddModal() {
        _uiState.update { it.copy(isAddModalOpen = false) }
    }

    fun openAdjustModal(item: InventoryItem? = null) {
        _uiState.update {
            it.copy(
                isAdjustModalOpen = true,
                selectedItemForAdjustment = item ?: it.items.firstOrNull()
            )
        }
    }

    fun closeAdjustModal() {
        _uiState.update { it.copy(isAdjustModalOpen = false, selectedItemForAdjustment = null) }
    }

    fun addItem(item: InventoryItem) {
        if (_uiState.value.isSaving) return
        _uiState.update { it.copy(isSaving = true, error = null) }
        work.submit {
            repository.addItem(item)
            refreshItems()
            closeAddModal()
        }
    }

    fun updateItem(item: InventoryItem) {
        if (_uiState.value.isSaving) return
        _uiState.update { it.copy(isSaving = true, error = null) }
        work.submit {
            repository.updateItem(item)
            refreshItems()

        }
    }

    fun deleteItem(id: String) {
        if (_uiState.value.isSaving) return
        _uiState.update { it.copy(isSaving = true, error = null) }
        work.submit {
            repository.deleteItem(id)
            refreshItems()

        }
    }

    fun adjustStock(adjustment: StockAdjustment) {
        if (_uiState.value.isSaving) return
        _uiState.update { it.copy(isSaving = true, error = null) }
        work.submit {
            repository.adjustStock(adjustment)
            refreshItems()
            closeAdjustModal()
        }
    }
}

/** Snapshot aggregates are built with data loads, never by each visible card/recomposition. */
class InventoryListSummary(items: List<InventoryItem>) {
    val goldWeight18k = items.sumOf { it.weightIn18kGrams * it.quantity }
    val pieces = items.sumOf { it.quantity }
    val trays = items.asSequence().map { it.location }.filter { it.contains("سینی") || it.contains("ویترین") }.distinct().count()
    val safes = items.asSequence().map { it.location }.filter { it.contains("گاوصندوق") || it.contains("انبار") }.distinct().count()
    val categories = items.groupBy { it.category }.mapValues { (_, group) -> group.sumOf { it.quantity } }
}
