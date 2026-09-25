package com.goldex.companion.domain.reporting

/**
 * Time filter periods for financial balance sheets and analytics.
 */
enum class ReportingPeriod(val labelFa: String) {
    TODAY("امروز"),
    THIS_WEEK("این هفته"),
    THIS_MONTH("ماه جاری"),
    THIS_QUARTER("فصل جاری"),
    THIS_YEAR("سالانه"),
    CUSTOM("بازه دلخواه")
}

/**
 * Key Performance Indicators (KPIs) presented in the Hero Vault card.
 */
data class FinancialVaultKpi(
    val grossProfitTomans: Long = 0L,
    val inventoryGoldWeight18k: Double = 0.0,
    val turnoverTomans: Long = 0L,
    val customerReceivablesTomans: Long = 0L,
    val unsettledInvoicesCount: Int = 0,
    val debtorCustomersCount: Int = 0
)

/**
 * Realtime intraday trade summary (Today's Quick Reports).
 */
data class QuickDailySummary(
    val salesCount: Int = 0,
    val salesWeight18k: Double = 0.0,
    val salesAmountTomans: Long = 0L,
    val purchaseCount: Int = 0,
    val purchaseWeight18k: Double = 0.0,
    val purchaseAmountTomans: Long = 0L
)

/**
 * Module 1: Sales Performance & Gross Profit Breakdown
 */
data class SalesPerformanceLedger(
    val grossSalesTomans: Long = 0L,
    val rawGoldValueTomans: Long = 0L,
    val totalWageTomans: Long = 0L,
    val totalProfitTomans: Long = 0L,
    val totalTaxTomans: Long = 0L,
    val totalWeight18k: Double = 0.0,
    val itemsCount: Int = 0
)

/**
 * Module 2: Gold Weight Balance & Vault Inventory
 */
data class GoldInventoryLedger(
    val totalWeight18k: Double = 0.0,
    val showcaseWeight18k: Double = 0.0,
    val vaultWeight18k: Double = 0.0,
    val piecesCount: Int = 0,
    val activeTraysCount: Int = 0,
    val activeSafesCount: Int = 0
)

/**
 * Module 3: Customer & Counterparty Debtors/Creditors Balance
 */
data class DebtorsCreditorsLedger(
    val totalReceivablesTomans: Long = 0L,
    val debtorCount: Int = 0,
    val totalPayablesTomans: Long = 0L,
    val creditorCount: Int = 0,
    val goldDebtGrams: Double = 0.0
)

/**
 * Module 4: VAT & Moadian Tax System (Legal Iranian Gold Tax Law)
 * Note: Under Iranian VAT law for gold, raw gold value is 100% tax-exempt;
 * VAT applies ONLY to wages and profit (اجرت + سود).
 */
data class VatReportLedger(
    val taxableBaseTomans: Long = 0L, // Wage + Profit
    val totalVatCollectedTomans: Long = 0L, // 9% (or customized VAT rate) on taxable base
    val taxExemptRawGoldTomans: Long = 0L, // Raw gold principal
    val vatPercent: Double = 9.0
)

/** Read-only detail rows derived from the same local records as the gateway totals. */
data class SalesCategoryReportRow(
    val title: String,
    val salesTomans: Long,
    val wageTomans: Long,
    val profitTomans: Long,
    val vatTomans: Long
)

data class SalesItemReportRow(
    val title: String,
    val weight18k: Double,
    val earningsTomans: Long
)

data class CustomerBalanceReportRow(
    val id: String,
    val name: String,
    val role: String,
    val cashDebtTomans: Long,
    val goldDebtGrams: Double
)

data class InventoryLocationReportRow(
    val location: String,
    val weight18k: Double,
    val piecesCount: Int
)

data class InventoryMovementReportRow(
    val title: String,
    val typeLabel: String,
    val weightGrams: Double,
    val timestamp: Long
)

data class ReportingDetails(
    val salesCategories: List<SalesCategoryReportRow> = emptyList(),
    val topSalesItems: List<SalesItemReportRow> = emptyList(),
    val profitBucketsTomans: List<Long> = listOf(0L, 0L, 0L, 0L),
    val customerBalances: List<CustomerBalanceReportRow> = emptyList(),
    val inventoryLocations: List<InventoryLocationReportRow> = emptyList(),
    val inventoryMovements: List<InventoryMovementReportRow> = emptyList()
)

/**
 * Specialized Ledger Breakdown Dialog/Sheet types
 */
enum class ReportingBreakdownType {
    SALES_PERFORMANCE,
    GOLD_INVENTORY,
    DEBTORS_CREDITORS,
    VAT_REPORT
}

/**
 * Complete UI State for the Reporting & Balance Sheets Center
 */
data class ReportingUiState(
    val selectedPeriod: ReportingPeriod = ReportingPeriod.THIS_MONTH,
    val customStartDateShamsi: String = "",
    val customEndDateShamsi: String = "",
    val kpi: FinancialVaultKpi = FinancialVaultKpi(),
    val quickSummary: QuickDailySummary = QuickDailySummary(),
    val salesPerformance: SalesPerformanceLedger = SalesPerformanceLedger(),
    val goldInventory: GoldInventoryLedger = GoldInventoryLedger(),
    val debtorsCreditors: DebtorsCreditorsLedger = DebtorsCreditorsLedger(),
    val vatReport: VatReportLedger = VatReportLedger(),
    val details: ReportingDetails = ReportingDetails(),
    val activeBreakdown: ReportingBreakdownType? = null,
    val isReportingVisible: Boolean = false,
    val isCustomDateDialogVisible: Boolean = false,
    val isLoading: Boolean = false
)
