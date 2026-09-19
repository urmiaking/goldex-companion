package com.goldex.companion.ui.inventory

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.goldex.companion.data.InventoryRepository
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
    val isInventoryVisible: Boolean = false
) {
    val filteredItems: List<InventoryItem>
        get() = items.filter { item ->
            val matchesCategory = (selectedCategory == InventoryCategory.ALL || item.category == selectedCategory)
            val matchesSearch = searchQuery.isBlank() ||
                item.title.contains(searchQuery, ignoreCase = true) ||
                item.code.contains(searchQuery, ignoreCase = true) ||
                item.rfidTag.contains(searchQuery, ignoreCase = true) ||
                item.location.contains(searchQuery, ignoreCase = true)
            matchesCategory && matchesSearch
        }

    val totalGoldWeight18k: Double
        get() = items.sumOf { it.weightIn18kGrams * it.quantity }

    val totalMesghalEquivalent: Double
        get() = if (totalGoldWeight18k > 0.0) totalGoldWeight18k / 4.3318 else 0.0

    val totalPiecesCount: Int
        get() = items.sumOf { it.quantity }

    val activeTraysCount: Int
        get() = items.map { it.location }.filter { it.contains("سینی") || it.contains("ویترین") }.distinct().size.coerceAtLeast(1)

    val activeSafesCount: Int
        get() = items.map { it.location }.filter { it.contains("گاوصندوق") || it.contains("انبار") }.distinct().size.coerceAtLeast(1)

    fun categoryCount(category: InventoryCategory): Int {
        return if (category == InventoryCategory.ALL) {
            items.sumOf { it.quantity }
        } else {
            items.filter { it.category == category }.sumOf { it.quantity }
        }
    }
}

class InventoryViewModel(
    private val repository: InventoryStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(InventoryUiState())
    val uiState: StateFlow<InventoryUiState> = _uiState.asStateFlow()

    init {
        loadItems()
    }

    fun loadItems() {
        _uiState.update { it.copy(items = repository.getItems()) }
    }

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
        repository.addItem(item)
        loadItems()
        closeAddModal()
    }

    fun updateItem(item: InventoryItem) {
        repository.updateItem(item)
        loadItems()
    }

    fun deleteItem(id: String) {
        repository.deleteItem(id)
        loadItems()
    }

    fun adjustStock(adjustment: StockAdjustment) {
        repository.adjustStock(adjustment)
        loadItems()
        closeAdjustModal()
    }
}

class InventoryViewModelFactory(
    private val application: Application
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val repository = InventoryRepository(application.applicationContext)
        return InventoryViewModel(repository) as T
    }
}
