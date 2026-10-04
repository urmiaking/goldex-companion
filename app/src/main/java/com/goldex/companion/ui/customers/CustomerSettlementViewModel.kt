package com.goldex.companion.ui.customers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.goldex.companion.ui.util.FeatureWorkQueue
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import com.goldex.companion.data.CustomerStore
import com.goldex.companion.data.InvoiceStore
import com.goldex.companion.data.MarketRates
import com.goldex.companion.domain.customers.*
import com.goldex.companion.model.*
import com.goldex.companion.platform.RandomIdGenerator
import com.goldex.companion.platform.SettlementArithmetic
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.math.abs
import kotlin.math.ceil

data class SettlementInvoiceOption(val id: String, val number: String, val balance: OutstandingBalance)

data class CustomerSettlementUiState(
    val customer: Customer? = null,
    val invoices: List<SettlementInvoiceOption> = emptyList(),
    val invoiceId: String? = null,
    val targetType: LedgerEntryType = LedgerEntryType.GOLD_WEIGHT,
    val paymentType: LedgerEntryType = LedgerEntryType.CASH_RIAL,
    val amountInput: String = "",
    val weightInput: String = "",
    val karatInput: String = "750",
    val rateInput: String = "",
    val rateSource: String = "دلخواه",
    val rateObservedAt: String = "",
    val marketRateAvailable: Boolean = false,
    val isMarketRate: Boolean = false,
    val offsetExistingCredit: Boolean = false,
    val paymentMethod: String = "حواله بانکی / پایا",
    val trackingCode: String = "",
    val note: String = "",
    val requestId: String = RandomIdGenerator.newId(),
    val isSaving: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null
) {
    val scope: OutstandingBalance get() = if (invoiceId != null) invoices.firstOrNull { it.id == invoiceId }?.balance ?: OutstandingBalance(0.0, 0)
        else OutstandingBalance(customer?.goldDebtGrams ?: 0.0, customer?.cashDebtTomans ?: 0L)
    val needsRate: Boolean get() = targetType != paymentType
    val request: SettlementRequest? get() = customer?.let {
        SettlementRequest(id = requestId, customerId = it.id, targetType = targetType, paymentType = paymentType,
            amountTomans = PersianNumberFormatter.parseToCleanLong(amountInput) ?: 0,
            scaleWeightGrams = PersianNumberFormatter.parseToCleanDouble(weightInput) ?: 0.0,
            karat = PersianNumberFormatter.parseToCleanLong(karatInput)?.takeIf { it in 1..1000 }?.toInt() ?: 0,
            rateTomans = PersianNumberFormatter.parseToCleanLong(rateInput) ?: 0,
            rateSource = rateSource, rateObservedAt = rateObservedAt, offsetExistingCredit = offsetExistingCredit,
            invoiceId = invoiceId, paymentMethod = paymentMethod, trackingCode = trackingCode, note = note)
    }
    val previewResult: Result<SettlementPreview>? get() = request?.let { request -> runCatching { CustomerSettlementPolicy.preview(requireNotNull(customer), request, scope) } }
    val preview: SettlementPreview? get() = previewResult?.getOrNull()
    val validationError: String? get() = previewResult?.exceptionOrNull()?.let {
        if (it is ArithmeticException) "مقدار از محدودهٔ مجاز بیشتر است" else it.message
    }
    val canOffset: Boolean get() {
        val c = customer ?: return false
        val target = if (targetType == LedgerEntryType.GOLD_WEIGHT) scope.goldGrams else scope.cashTomans.toDouble()
        val other = if (targetType == LedgerEntryType.GOLD_WEIGHT) c.cashDebtTomans.toDouble() else c.goldDebtGrams
        return target * other < 0
    }
}

class CustomerSettlementViewModel(
    private val customers: CustomerStore,
    private val invoices: InvoiceStore,
    private val record: RecordCustomerSettlementUseCase,
    private val workDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val marketQuote: () -> MarketRates
) : ViewModel() {
    private val _state = MutableStateFlow(CustomerSettlementUiState())
    val state = _state.asStateFlow()

    private val work = FeatureWorkQueue(viewModelScope, workDispatcher,
        onBusy = {},
        onError = { error -> _state.update { it.copy(isLoading = false, isSaving = false,
            error = if (error is IllegalArgumentException) error.message else "عملیات تسویه انجام نشد؛ دوباره تلاش کنید") } })

    fun open(customer: Customer, invoiceId: String? = null) {
        if (_state.value.isSaving) return
        val initial = CustomerSettlementUiState(customer = customer, invoiceId = invoiceId, isLoading = true)
        _state.value = initial
        work.submit(onSuccess = {
            if (_state.value.requestId == initial.requestId) { chooseDefaultTarget(); useMarketRate() }
        }, onFailure = { error ->
            _state.update { if (it.requestId == initial.requestId) it.copy(isLoading = false,
                error = if (error is IllegalArgumentException) error.message else "بارگذاری تسویه انجام نشد؛ دوباره تلاش کنید") else it }
        }) {
            if (_state.value.requestId != initial.requestId) return@submit
            val fresh = customers.getCustomers().firstOrNull { it.id == customer.id } ?: error("طرف‌حساب یافت نشد")
            val saved = invoices.getBarterInvoices().filter { it.customer?.id == fresh.id && it.syncWithLedger }
            val entriesByInvoice = customers.getTransactionsByInvoiceIds(saved.map { it.id }).groupBy { it.invoiceId }
            val options = saved.mapNotNull {
                val entries = entriesByInvoice[it.id].orEmpty()
                val balance = invoiceOutstanding(entries)
                if (entries.isEmpty() || balance.isSettled) null else SettlementInvoiceOption(it.id, it.cleanInvoiceNumber, balance)
            }
            _state.update { if (it.requestId == initial.requestId) it.copy(customer = fresh, invoices = options, isLoading = false) else it }
        }
    }

    fun close() { if (!_state.value.isSaving) _state.value = CustomerSettlementUiState() }

    fun selectInvoice(id: String?) {
        require(id == null || _state.value.invoices.any { it.id == id })
        _state.update { it.copy(invoiceId = id, amountInput = "", weightInput = "", offsetExistingCredit = false, error = null) }
        chooseDefaultTarget()
    }

    private fun chooseDefaultTarget() {
        val scope = _state.value.scope
        selectTarget(if (abs(scope.goldGrams) > 1e-10) LedgerEntryType.GOLD_WEIGHT else LedgerEntryType.CASH_RIAL)
    }

    fun selectTarget(type: LedgerEntryType) {
        _state.update { it.copy(targetType = type, offsetExistingCredit = false, error = null) }
    }
    fun selectPayment(type: LedgerEntryType) {
        _state.update { it.copy(paymentType = type, paymentMethod = if (type == LedgerEntryType.GOLD_WEIGHT) "شمش / آبشده" else "حواله بانکی / پایا", offsetExistingCredit = false, error = null) }
    }
    fun useMarketRate() {
        val quote = marketQuote()
        val available = quote.isLive && quote.gold18 > 0 && quote.lastUpdated.isNotBlank() && !quote.lastUpdated.contains("--")
        _state.update {
            if (!available) it.copy(marketRateAvailable = false, isMarketRate = false, rateSource = "دلخواه", rateObservedAt = "", error = null)
            else it.copy(marketRateAvailable = true, isMarketRate = true, rateInput = quote.gold18.toString(),
                rateSource = quote.source.labelFa, rateObservedAt = quote.lastUpdated, error = null)
        }
    }
    fun useCustomRate() { _state.update { it.copy(isMarketRate = false, rateSource = "دلخواه", rateObservedAt = "", error = null) } }
    fun setRate(value: String) { _state.update { it.copy(rateInput = numeric(value, false), isMarketRate = false, rateSource = "دلخواه", rateObservedAt = "", error = null) } }
    fun setAmount(value: String) { _state.update { it.copy(amountInput = numeric(value, false), error = null) } }
    fun setWeight(value: String) { _state.update { it.copy(weightInput = numeric(value, true), error = null) } }
    fun setKarat(value: String) { _state.update { it.copy(karatInput = numeric(value, false), error = null) } }
    fun setMethod(value: String) { _state.update { it.copy(paymentMethod = value) } }
    fun setTracking(value: String) { _state.update { it.copy(trackingCode = value) } }
    fun setNote(value: String) { _state.update { it.copy(note = value) } }

    fun setOffset(enabled: Boolean) {
        val s = _state.value
        if (enabled && !s.canOffset) return
        _state.update { it.copy(offsetExistingCredit = enabled,
            paymentType = if (enabled) { if (it.targetType == LedgerEntryType.GOLD_WEIGHT) LedgerEntryType.CASH_RIAL else LedgerEntryType.GOLD_WEIGHT } else it.paymentType,
            karatInput = if (enabled) "750" else it.karatInput, amountInput = "", weightInput = "", error = null) }
        if (enabled) fillRemaining()
    }

    fun fillRemaining() {
        val s = _state.value
        val request = s.request ?: return
        try {
            if (s.paymentType == LedgerEntryType.CASH_RIAL) {
                val amount = if (s.targetType == LedgerEntryType.CASH_RIAL) abs(s.scope.cashTomans)
                    else SettlementArithmetic.goldToTomans(abs(s.scope.goldGrams), request.rateTomans)
                val capped = if (s.offsetExistingCredit) minOf(amount, abs(requireNotNull(s.customer).cashDebtTomans)) else amount
                _state.update { it.copy(amountInput = capped.toString(), error = null) }
            } else {
                val equivalent = if (s.targetType == LedgerEntryType.GOLD_WEIGHT) abs(s.scope.goldGrams)
                    else SettlementArithmetic.tomansToGold(abs(s.scope.cashTomans), request.rateTomans)
                require(request.karat in 1..1000)
                val physical = equivalent * 750 / request.karat
                // Physical gold is entered at 0.001g; overpayment from rounding is visible in the preview.
                val weight = if (s.offsetExistingCredit) minOf(equivalent, abs(requireNotNull(s.customer).goldDebtGrams))
                    else ceil(physical * 1000 - 1e-9) / 1000
                _state.update { it.copy(weightInput = weight.toString(), error = null) }
            }
        } catch (_: Exception) { _state.update { it.copy(error = "برای محاسبهٔ تسویهٔ کامل، نرخ و عیار معتبر وارد کنید") } }
    }

    fun confirm(onComplete: () -> Unit = {}) {
        val snapshot = _state.value
        if (snapshot.isLoading || snapshot.isSaving || snapshot.customer == null || snapshot.preview == null) return
        _state.update { it.copy(isSaving = true, error = null) }
        work.submit(onSuccess = { _state.value = CustomerSettlementUiState(); onComplete() }) {
            record.record(requireNotNull(snapshot.request), snapshot.customer, snapshot.scope)
        }
    }

    private fun numeric(value: String, @Suppress("UNUSED_PARAMETER") decimal: Boolean): String {
        val latin = PersianNumberFormatter.toEnglishDigits(value).replace('٫', '.')
        // Keep invalid characters visible so pasting a negative/decimal amount cannot silently change its value.
        return latin.replace(",", "").replace("٬", "").replace("،", "").replace(" ", "")
    }
}
