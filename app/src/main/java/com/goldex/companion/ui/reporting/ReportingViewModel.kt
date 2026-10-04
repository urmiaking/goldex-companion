package com.goldex.companion.ui.reporting

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.goldex.companion.ui.util.FeatureWorkQueue
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import com.goldex.companion.data.CustomerStore
import com.goldex.companion.data.InventoryStore
import com.goldex.companion.data.InvoiceStore
import com.goldex.companion.data.SettingsStore
import com.goldex.companion.domain.reporting.ReportingBreakdownType
import com.goldex.companion.domain.reporting.ReportingDetailsUseCase
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
    private val settingsStore: SettingsStore,
    private val workDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReportingUiState())
    val uiState: StateFlow<ReportingUiState> = _uiState.asStateFlow()

    private var customStartTimestamp: Long? = null
    private var customEndTimestamp: Long? = null

    private val work = FeatureWorkQueue(viewModelScope, workDispatcher,
        onBusy = { busy -> _uiState.update { it.copy(isLoading = busy) } },
        onError = { _ -> _uiState.update { it.copy(error = "بارگذاری گزارش انجام نشد؛ دوباره تلاش کنید") } })

    // Reports are loaded when opened; their large snapshots must not block app/list startup.
    fun loadData() {
        val requestedPeriod = _uiState.value.selectedPeriod
        val requestedStart = customStartTimestamp
        val requestedEnd = customEndTimestamp
        _uiState.update { it.copy(error = null) }
        work.submit {

            val invoices = invoiceStore.getBarterInvoices()
            val customers = customerStore.getCustomers()
            val inventoryItems = inventoryStore.getItems()
            val adjustments = inventoryStore.getAdjustments()
            val settings = settingsStore.loadSettings()

            val periodRange = ReportingUseCases.calculatePeriodTimeRange(
                period = requestedPeriod,
                customStartMs = requestedStart,
                customEndMs = requestedEnd
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

            val details = ReportingDetailsUseCase.calculate(
                invoices = invoices,
                customers = customers,
                inventoryItems = inventoryItems,
                adjustments = adjustments,
                periodRange = periodRange
            )

            _uiState.update {
                if (it.selectedPeriod != requestedPeriod || customStartTimestamp != requestedStart || customEndTimestamp != requestedEnd) it
                else it.copy(
                    kpi = kpi,
                    quickSummary = quickSummary,
                    salesPerformance = salesPerformance,
                    goldInventory = goldInventory,
                    debtorsCreditors = debtorsCreditors,
                    vatReport = vatReport,
                    details = details,
                    error = null
                )
            }

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
        _uiState.update { it.copy(isReportingVisible = visible, activeBreakdown = if (visible) it.activeBreakdown else null) }
        if (visible) {
            loadData()
        }
    }
}
