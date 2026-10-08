package com.goldex.companion.desktop.state

import com.goldex.companion.data.*
import com.goldex.companion.desktop.data.*
import com.goldex.companion.model.PriceBasisTab
import com.goldex.companion.presentation.calculator.CalculatorField
import com.goldex.companion.presentation.calculator.ManualGoldCalculator
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.nio.file.Path

enum class DesktopDestination(val title: String, val subtitle: String) {
    DASHBOARD("پیشخوان", "موجودی ویترین و گاوصندوق و نبض بازار"),
    CALCULATOR("ماشین‌حساب", "محاسبه طلا با نرخ انتخابی شما"),
    RATES("تابلوی مظنه‌ها", "قیمت‌ها همراه با منبع و زمان دریافت"),
    INVENTORY("انبار و ویترین طلا", "مدیریت موجودی، ارزش لحظه‌ای و اتیکت"),
    INVOICES("مدیریت فاکتورها", "فاکتورهای ثبت‌شده و تسویه زرگری"),
    PORTFOLIO("سبد قبلی", "دارایی‌های ثبت‌شده در سبد قبلی"),
    SETTINGS("تنظیمات", "مشخصات گالری، ترجیحات و پشتیبان اطلاعات");

    val navigationOrder get() = when (this) {
        DASHBOARD -> 0
        CALCULATOR -> 1
        RATES -> 2
        INVENTORY -> 3
        INVOICES -> 4
        PORTFOLIO, SETTINGS -> 5
    }

    companion object {
        val mainDestinations = listOf(DASHBOARD, CALCULATOR, RATES, INVENTORY, INVOICES, SETTINGS)
    }
}

data class WorkspaceState(
    val destination: DesktopDestination = DesktopDestination.DASHBOARD,
    val settings: AppSettings = AppSettings(), val settingsDraft: AppSettings? = null, val dark: Boolean = false,
    val reduceMotion: Boolean = false,
    val snapshot: MarketSnapshot? = null, val now: Long = System.currentTimeMillis(),
    val connection: ConnectionStatus = ConnectionStatus.OFFLINE,
    val assets: PortfolioProjection = DesktopPortfolioPolicy.project(emptyList(), null),
    val query: String = "", val categoryFilter: PortfolioCategory? = null,
    val draft: PortfolioDraft? = null, val pendingDelete: PortfolioItem? = null,
    val refreshing: Boolean = false, val saving: Boolean = false,
    val error: String? = null, val notice: String? = null
) {
    val visibleAssets get() = assets.rows.filter { (categoryFilter == null || it.item.category == categoryFilter) &&
        (query.isBlank() || PersianTextSearch.normalize(it.item.title).contains(PersianTextSearch.normalize(query))) }
}

private object PersianTextSearch {
    fun normalize(value: String) = value.trim().replace('ي', 'ی').replace('ك', 'ک').lowercase()
}

/** The desktop composition root supplies storage/market; composables receive events and immutable state. */
class DesktopWorkspace(
    private val storage: DesktopDataStore,
    private val market: DesktopMarketGateway,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
    history: DesktopGoldHistoryGateway = DesktopGoldHistoryRepository(),
    private val connectivity: ConnectivityObserver = WindowsConnectivityObserver(scope),
    ratesBoardGateway: DesktopRatesBoardGateway = DesktopRatesBoardRepository(storage.directory),
    private val waitForNextTick: suspend (Long) -> Unit = { delay(it) }
) : AutoCloseable {
    val dashboard = DesktopDashboard(history, scope)
    val ratesBoard = DesktopRatesBoard(ratesBoardGateway, scope)
    val inventory = DesktopInventory(storage, scope)
    private val portfolio: PortfolioStore = storage
    private val preferences: SettingsStore = storage
    val calculator = ManualGoldCalculator(preferences.loadSettings())
    val calculatorRates = DesktopCalculatorRates(calculator, market.snapshot.value)
    private val mutable = MutableStateFlow(WorkspaceState(settings = preferences.loadSettings(), dark = preferences.loadDarkTheme(), reduceMotion = storage.loadReduceMotion(),
        snapshot = market.snapshot.value, assets = DesktopPortfolioPolicy.project(portfolio.getItems(), market.snapshot.value?.rates)))
    val state = mutable.asStateFlow()

    init {
        scope.launch { connectivity.status.collect { connection -> mutable.update { it.copy(connection = connection) } } }
        scope.launch {
            market.snapshot.collect { quote ->
                inventory.quote(quote?.rates?.gold18 ?: 0)
                calculatorRates.updateQuote(quote)
                mutable.update { it.copy(snapshot = quote, assets = DesktopPortfolioPolicy.project(portfolio.getItems(), quote?.rates), now = System.currentTimeMillis()) }
            }
        }
    }

    fun start() = scope.launch {
        (connectivity as? WindowsConnectivityObserver)?.start()
        dashboard.select(dashboard.state.value.horizon)
        // The legacy preference remains serialized for compatibility; desktop rates are always automatic.
        refreshRates(background = true).join()
        while (isActive) {
            waitForNextTick(30_000)
            mutable.update { it.copy(now = System.currentTimeMillis()) }
            if (state.value.destination == DesktopDestination.DASHBOARD && !dashboard.state.value.loading)
                dashboard.select(dashboard.state.value.horizon)
            if (state.value.snapshot?.isFresh(System.currentTimeMillis()) != true) refreshRates(background = true).join()
        }
    }

    fun navigate(destination: DesktopDestination) {
        mutable.update { it.copy(destination = destination, notice = null) }
        if (destination == DesktopDestination.DASHBOARD) dashboard.select(dashboard.state.value.horizon)
    }
    fun search(value: String) { mutable.update { it.copy(query = value) } }
    fun filter(category: PortfolioCategory?) { mutable.update { it.copy(categoryFilter = category) } }
    fun clearMessage() { mutable.update { it.copy(error = null, notice = null) } }
    fun editSettings(change: (AppSettings) -> AppSettings) { if (!state.value.saving) mutable.update { it.copy(settingsDraft = change(it.settingsDraft ?: it.settings)) } }
    fun revertSettings() { mutable.update { it.copy(settingsDraft = null) } }
    fun openAsset(item: PortfolioItem? = null) { if (!inventory.state.value.hasDialog) mutable.update { it.copy(draft = item?.let(DesktopPortfolioPolicy::draft) ?: PortfolioDraft(), notice = null) } }
    fun openInventoryItem() {
        if (state.value.saving || state.value.draft != null || state.value.pendingDelete != null || inventory.state.value.hasDialog || inventory.state.value.saving) return
        navigate(DesktopDestination.INVENTORY)
        inventory.open()
    }
    fun editDraft(change: (PortfolioDraft) -> PortfolioDraft) { if (!state.value.saving) mutable.update { current -> current.copy(draft = current.draft?.let(change)?.copy(errors = emptyMap())) } }
    fun dismissAsset() { if (!state.value.saving) mutable.update { it.copy(draft = null) } }
    fun requestDelete(item: PortfolioItem?) { mutable.update { it.copy(pendingDelete = item) } }

    fun saveAsset(): Job? {
        val draft = state.value.draft ?: return null
        if (state.value.saving) return null
        val errors = DesktopPortfolioPolicy.validate(draft)
        if (errors.isNotEmpty()) { mutable.update { it.copy(draft = draft.copy(errors = errors)) }; return null }
        return operation("ذخیره دارایی انجام نشد؛ فضای دیسک و دسترسی پوشه را بررسی کنید") {
            portfolio.addItem(DesktopPortfolioPolicy.toItem(draft))
            mutable.update { it.copy(draft = null, notice = "دارایی ذخیره شد", assets = DesktopPortfolioPolicy.project(portfolio.getItems(), it.snapshot?.rates)) }
        }
    }

    fun confirmDelete(): Job? {
        val item = state.value.pendingDelete ?: return null
        if (state.value.saving) return null
        return operation("حذف انجام نشد؛ اطلاعات قبلی حفظ شده‌اند") {
            portfolio.deleteItem(item.id)
            mutable.update { it.copy(pendingDelete = null, notice = "دارایی حذف شد", assets = DesktopPortfolioPolicy.project(portfolio.getItems(), it.snapshot?.rates)) }
        }
    }

    fun toggleTheme(): Job? = if (state.value.saving) null else operation("تنظیم ظاهر ذخیره نشد") {
        val dark = !state.value.dark
        preferences.saveDarkTheme(dark)
        mutable.update { it.copy(dark = dark) }
    }

    fun setReduceMotion(enabled: Boolean): Job? = if (state.value.saving) null else operation("تنظیم حرکت ذخیره نشد") {
        storage.saveReduceMotion(enabled)
        mutable.update { it.copy(reduceMotion = enabled) }
    }

    fun saveSettings(settings: AppSettings): Job? {
        if (state.value.saving) return null
        val normalized = settings.copy(defaultProfitPercent = DesktopPortfolioPolicy.normalized(settings.defaultProfitPercent), defaultTaxPercent = DesktopPortfolioPolicy.normalized(settings.defaultTaxPercent))
        val valid = listOf(normalized.defaultProfitPercent, normalized.defaultTaxPercent).all { it.matches(Regex("[0-9]+(\\.[0-9]+)?")) && it.toDoubleOrNull()?.let { value -> value in 0.0..100.0 } == true }
        if (!valid) { mutable.update { it.copy(error = "سود و مالیات باید درصدی بین صفر و ۱۰۰ باشند") }; return null }
        return operation("تنظیمات ذخیره نشد؛ اطلاعات قبلی حفظ شده‌اند") {
            val previous = state.value.settings
            preferences.saveSettings(normalized)
            calculator.updateDefaults(normalized)
            mutable.update { it.copy(settings = normalized, settingsDraft = null, notice = "تنظیمات ذخیره شد؛ پیش‌فرض‌ها برای محاسبه جدید اعمال می‌شوند") }
            if (previous.priceSource != normalized.priceSource) {
                mutable.update { it.copy(refreshing = true) }
                try { market.setSource(normalized.priceSource) } catch (failure: CancellationException) { throw failure }
                catch (_: Exception) { mutable.update { it.copy(error = "تنظیمات ذخیره شد؛ نرخ تازه دریافت نشد") } }
                finally { mutable.update { it.copy(refreshing = false) } }
            }
        }
    }

    fun refreshRates(background: Boolean = false): Job {
        if (state.value.refreshing) return scope.launch { }
        mutable.update { if (background) it.copy(refreshing = true) else it.copy(refreshing = true, error = null, notice = null) }
        if (state.value.destination == DesktopDestination.RATES) ratesBoard.refresh(force = true)
        return scope.launch {
            try {
                if (market.currentSource.value != state.value.settings.priceSource) market.setSource(state.value.settings.priceSource) else market.refreshRates()
            } catch (failure: CancellationException) { throw failure }
            catch (_: Exception) { if (!background) mutable.update { it.copy(error = "نرخ تازه دریافت نشد؛ نرخ قبلی با زمان اصلی حفظ شده است. اتصال اینترنت را بررسی کنید") } }
            finally { mutable.update { it.copy(refreshing = false) } }
        }
    }

    fun saveManualRates(rates: MarketRates): Job? = if (state.value.saving || state.value.refreshing) null else operation("نرخ دستی ذخیره نشد") {
        require(rates.gold18 > 0)
        market.useManual(rates)
        mutable.update { it.copy(notice = "نرخ دستی ثبت شد؛ دریافت خودکار تا انتخاب دریافت آنلاین متوقف است") }
    }

    fun applyQuoteToCalculator() = applyQuoteToCalculator(PriceBasisTab.K18)

    fun applyQuoteToCalculator(basis: PriceBasisTab) {
        val quote = state.value.snapshot ?: return
        val price = when (basis) { PriceBasisTab.K18 -> quote.rates.gold18; PriceBasisTab.K24 -> quote.rates.gold24; PriceBasisTab.MESGHAL -> quote.rates.goldMelt }
        if (price <= 0) return
        calculatorRates.setPriceBasis(basis)
        calculatorRates.useMarketRate()
        mutable.update { it.copy(destination = DesktopDestination.CALCULATOR, notice = "نرخ انتخابی در ماشین‌حساب قرار گرفت؛ پیش از محاسبه آن را بررسی کنید") }
    }

    fun exportBackup(path: Path): Job? = if (state.value.saving) null else operation("پشتیبان ذخیره نشد؛ نام تازه‌ای انتخاب و دسترسی پوشه را بررسی کنید") {
        storage.exportBackup(path)
        mutable.update { it.copy(notice = "فایل پشتیبان ذخیره شد") }
    }

    private fun operation(error: String, block: suspend () -> Unit): Job {
        mutable.update { it.copy(saving = true, error = null, notice = null) }
        return scope.launch(Dispatchers.IO) {
            try { block() } catch (failure: CancellationException) { throw failure }
            catch (_: Exception) { mutable.update { it.copy(error = error) } }
            finally { mutable.update { it.copy(saving = false) } }
        }
    }

    override fun close() { runBlocking { scope.coroutineContext[Job]?.cancelAndJoin() } }
}
