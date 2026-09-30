package com.goldex.companion.ui.portfolio

import androidx.lifecycle.ViewModel
import com.goldex.companion.data.PortfolioItem
import com.goldex.companion.data.PortfolioStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class PortfolioManagerUiState(
    val portfolioItems: List<PortfolioItem> = emptyList()
)

class PortfolioManagerViewModel(
    private val repository: PortfolioStore
) : ViewModel() {
    private val _uiState = MutableStateFlow(PortfolioManagerUiState())
    val uiState: StateFlow<PortfolioManagerUiState> = _uiState.asStateFlow()

    init {
        _uiState.value = PortfolioManagerUiState(portfolioItems = repository.getItems())
    }

    fun setItems(items: List<PortfolioItem>) {
        _uiState.update { it.copy(portfolioItems = items) }
    }

    fun loadPortfolio() {
        _uiState.update { it.copy(portfolioItems = repository.getItems()) }
    }

    fun addPortfolioItem(item: PortfolioItem) {
        repository.addItem(item)
        _uiState.update { it.copy(portfolioItems = repository.getItems()) }
    }

    fun deletePortfolioItem(itemId: String) {
        repository.deleteItem(itemId)
        _uiState.update { it.copy(portfolioItems = repository.getItems()) }
    }

    fun updatePortfolioItem(item: PortfolioItem) {
        repository.addItem(item)
        _uiState.update { it.copy(portfolioItems = repository.getItems()) }
    }
}
