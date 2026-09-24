package com.goldex.companion.ui.reporting

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.goldex.companion.data.CustomerRepository
import com.goldex.companion.data.CustomerStore
import com.goldex.companion.data.InventoryRepository
import com.goldex.companion.data.InventoryStore
import com.goldex.companion.data.InvoiceRepository
import com.goldex.companion.data.InvoiceStore
import com.goldex.companion.data.SettingsRepository
import com.goldex.companion.data.SettingsStore
import com.goldex.companion.domain.reporting.ReportingBreakdownType
import com.goldex.companion.domain.reporting.ReportingPeriod
import com.goldex.companion.domain.reporting.ReportingUiState
import com.goldex.companion.domain.reporting.ReportingUseCases
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class ReportingViewModel(
    private val invoiceStore: InvoiceStore,
    private val customerStore: CustomerStore,
    private val inventoryStore: InventoryStore,
    private val settingsStore: SettingsStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReportingUiState())
    val uiState: StateFlow<ReportingUiState> = _uiState.asStateFlow()

    private var customStartTimestamp: Long? = null
    private var customEndTimestamp: Long? = null

    init {
        loadData()
    }

    fun loadData() {
        _uiState.update { it.copy(isLoading = true) }

        val invoices = invoiceStore.getBarterInvoices()
        val customers = customerStore.getCustomers()
        val inventoryItems = inventoryStore.getItems()
        val settings = settingsStore.loadSettings()

        val periodRange = ReportingUseCases.calculatePeriodTimeRange(
            period = _uiState.value.selectedPeriod,
            customStartMs = customStartTimestamp,
            customEndMs = customEndTimestamp
        )
        val todayRange = ReportingUseCases.calculatePeriodTimeRange(ReportingPeriod.TODAY)

        val kpi = ReportingUseCases.calculateFinancialVaultKpi(
            invoices = invoices,
            customers = customers,
            inventoryItems = inventoryItems,
            periodRange = periodRange
        )

        val quickSummary = ReportingUseCases.calculateQuickDailySummary(
            invoices = invoices,
            todayRange = todayRange
        )

        val salesPerformance = ReportingUseCases.calculateSalesPerformance(
            invoices = invoices,
            periodRange = periodRange
        )

        val goldInventory = ReportingUseCases.calculateGoldInventory(
            items = inventoryItems
        )

        val debtorsCreditors = ReportingUseCases.calculateDebtorsCreditors(
            customers = customers
        )

        val vatReport = ReportingUseCases.calculateVatReport(
            invoices = invoices,
            periodRange = periodRange,
            defaultVatPercent = settings.defaultTaxPercent.toDoubleOrNull() ?: 9.0
        )

        _uiState.update {
            it.copy(
                kpi = kpi,
                quickSummary = quickSummary,
                salesPerformance = salesPerformance,
                goldInventory = goldInventory,
                debtorsCreditors = debtorsCreditors,
                vatReport = vatReport,
                isLoading = false
            )
        }
    }

    fun selectPeriod(period: ReportingPeriod) {
        if (period == ReportingPeriod.CUSTOM) {
            _uiState.update { it.copy(isCustomDateDialogVisible = true) }
            return
        }
        _uiState.update { it.copy(selectedPeriod = period) }
        loadData()
    }

    fun setCustomDateRange(startMs: Long, endMs: Long, startShamsi: String, endShamsi: String) {
        customStartTimestamp = startMs
        customEndTimestamp = endMs
        _uiState.update {
            it.copy(
                selectedPeriod = ReportingPeriod.CUSTOM,
                customStartDateShamsi = startShamsi,
                customEndDateShamsi = endShamsi,
                isCustomDateDialogVisible = false
            )
        }
        loadData()
    }

    fun setCustomDateDialogVisible(visible: Boolean) {
        _uiState.update { it.copy(isCustomDateDialogVisible = visible) }
    }

    fun openBreakdown(type: ReportingBreakdownType) {
        _uiState.update { it.copy(activeBreakdown = type) }
    }

    fun closeBreakdown() {
        _uiState.update { it.copy(activeBreakdown = null) }
    }

    fun setReportingVisible(visible: Boolean) {
        _uiState.update { it.copy(isReportingVisible = visible) }
        if (visible) {
            loadData()
        }
    }
}

class ReportingViewModelFactory(
    private val app: Application
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ReportingViewModel::class.java)) {
            val invoiceStore = InvoiceRepository(app)
            val customerStore = CustomerRepository(app)
            val inventoryStore = InventoryRepository(app)
            val settingsStore = SettingsRepository(app)
            return ReportingViewModel(
                invoiceStore = invoiceStore,
                customerStore = customerStore,
                inventoryStore = inventoryStore,
                settingsStore = settingsStore
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class ${modelClass.name}")
    }
}
