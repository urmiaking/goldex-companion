package com.goldex.companion.desktop.state

import com.goldex.companion.data.CustomerStore
import com.goldex.companion.data.InventoryStore
import com.goldex.companion.data.InvoiceStore
import com.goldex.companion.data.SettingsStore
import com.goldex.companion.domain.reporting.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Desktop state holder for the GoldEx reporting and accounting center.
 * Connects directly to desktop stores and shared domain reporting use cases.
 */
class DesktopReporting(
    private val invoiceStore: InvoiceStore,
    private val customerStore: CustomerStore,
    private val inventoryStore: InventoryStore,
    private val settingsStore: SettingsStore,
    private val scope: CoroutineScope,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    private val _uiState = MutableStateFlow(ReportingUiState())
    val uiState: StateFlow<ReportingUiState> = _uiState.asStateFlow()

    private var customStartTimestamp: Long? = null
    private var customEndTimestamp: Long? = null
    private var loadJob: Job? = null

    fun loadData() {
        val requestedPeriod = _uiState.value.selectedPeriod
        val requestedStart = customStartTimestamp
        val requestedEnd = customEndTimestamp
        _uiState.update { it.copy(isLoading = true, error = null) }
        loadJob?.cancel()
        loadJob = scope.launch(dispatcher) {
            try {
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
                        isLoading = false,
                        error = null
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _uiState.update { it.copy(isLoading = false, error = "بارگذاری گزارش انجام نشد؛ دوباره تلاش کنید") }
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
}
