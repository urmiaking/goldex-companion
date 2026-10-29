package com.goldex.companion.desktop.state

import com.goldex.companion.data.InventoryStore
import com.goldex.companion.data.MarketRates
import com.goldex.companion.desktop.data.DesktopDataStore
import com.goldex.companion.desktop.data.InventoryOptions
import com.goldex.companion.desktop.data.DesktopInventoryOptions
import com.goldex.companion.desktop.data.InventoryPhoto
import com.goldex.companion.model.*
import com.goldex.companion.presentation.inventory.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.nio.file.Path
import java.util.concurrent.atomic.AtomicBoolean

data class StockDraft(val itemId: String, val type: StockAdjustmentType = StockAdjustmentType.CHARGE,
    val count: String = "1", val weight: String = "", val reason: String = "دریافت از کارگاه ساخت", val note: String = "", val errors: Map<String, String> = emptyMap())

data class InventorySummary(val pieces: Long, val gold18: Double, val mesghal: Double, val trays: Int, val safes: Int, val categories: Map<InventoryCategory, Long>)
data class DesktopInventoryState(
    val items: List<InventoryItem> = emptyList(), val history: List<StockAdjustment> = emptyList(),
    val query: String = "", val category: InventoryCategory = InventoryCategory.ALL,
    val visible: Boolean = false, val selectedId: String? = null, val draft: InventoryDraft? = null,
    val movement: StockDraft? = null, val pendingDelete: InventoryItem? = null, val showHistory: Boolean = false,
    val saving: Boolean = false, val error: String? = null, val notice: String? = null, val discard: Boolean = false,
    val spot18: Long = 0,
    val rates: MarketRates = MarketRates(),
    val options: InventoryOptions = InventoryOptions(),
    val draftCategoryId: String? = null,
    val filterCategoryId: String? = null
) {
    fun categoryTitle(item: InventoryItem): String =
        options.categories.firstOrNull { it.id == options.categoryIds[item.id] }?.name ?: item.category.titleFa
    val draftCategoryTitle get() = options.categories.firstOrNull { it.id == draftCategoryId }?.name ?: draft?.category?.titleFa.orEmpty()
    val locations get() = (DesktopInventoryOptions.defaultLocations + options.locations.map { it.name } + items.map { it.location }).filter { it.isNotBlank() }.distinct()
    val filtered get() = items.filter {
        (if (filterCategoryId != null) options.categoryIds[it.id] == filterCategoryId
            else category == InventoryCategory.ALL || it.category == category && it.id !in options.categoryIds) &&
            (InventoryForm.matches(it, query) || categoryTitle(it).contains(query.trim()))
    }
    val selected get() = items.firstOrNull { it.id == selectedId }
    val summary get(): InventorySummary {
        val gold = items.sumOf { it.weightIn18kGrams * it.quantity }
        return InventorySummary(items.sumOf { it.quantity.toLong() }, gold, gold / 4.3318,
            items.map { it.location }.filter { it.contains("سینی") || it.contains("ویترین") }.distinct().size,
            items.map { it.location }.filter { it.contains("گاوصندوق") || it.contains("انبار") }.distinct().size,
            items.groupBy { it.category }.mapValues { (_, items) -> items.sumOf { it.quantity.toLong() } })
    }
    val preview get() = draft?.let { InventoryForm.preview(it, spot18) }
    val selectedPrice get() = selected?.let { InventoryForm.price(it, spot18) }
    /** Metal-only vault value, matching Android's summary, without an invented fallback quote. */
    val metalValue get(): Long? {
        if (spot18 !in 1..1_000_000_000_000L) return null
        val value = summary.gold18 * spot18
        return value.takeIf { it.isFinite() && it >= 0 && it < Long.MAX_VALUE.toDouble() }?.toLong()
    }
    val hasDialog get() = draft != null || movement != null || pendingDelete != null || showHistory || discard
}

/** Business workspace feature owner, independent of UI and Android state holders. */
class DesktopInventory(private val storage: DesktopDataStore, private val scope: CoroutineScope) {
    private val store: InventoryStore = storage.inventory
    private val mutable = MutableStateFlow(DesktopInventoryState(items = store.getItems(), history = store.getAdjustments(), visible = storage.loadInventoryVisible(), options = storage.loadInventoryOptions()))
    val state = mutable.asStateFlow()
    private val writing = AtomicBoolean(false)
    private var movementBaseline: StockDraft? = null
    fun reload() {
        mutable.update {
            it.copy(
                items = store.getItems(),
                history = store.getAdjustments(),
                visible = storage.loadInventoryVisible(),
                options = storage.loadInventoryOptions()
            )
        }
    }
    fun quote(spot: Long, marketRates: MarketRates? = null) { mutable.update { it.copy(spot18 = spot, rates = marketRates ?: it.rates) } }
    fun search(value: String) { mutable.update { it.copy(query = value) } }
    fun filter(value: InventoryCategory) { mutable.update { it.copy(category = value, filterCategoryId = null, selectedId = null) } }
    fun filterCustomCategory(id: String) { mutable.update { it.copy(category = InventoryCategory.MISC, filterCategoryId = id, selectedId = null) } }
    fun select(id: String?) { mutable.update { it.copy(selectedId = id) } }
    fun clearMessage() { mutable.update { it.copy(error = null, notice = null) } }
    fun togglePrivacy(): Job? = operation("نمایش موجودی ذخیره نشد") {
        val visible = !state.value.visible; storage.saveInventoryVisible(visible); mutable.update { it.copy(visible = visible) }
    }
    fun open(item: InventoryItem? = null) {
        if (state.value.saving || state.value.hasDialog) return
        val draft = runCatching { item?.let(InventoryForm::from) ?: InventoryDraft(code = storage.nextInventoryCode()) }.getOrElse {
            mutable.update { it.copy(error = "کد عددی آزاد برای کالای جدید موجود نیست") }
            return
        }
        val populatedDraft = if (item == null && draft.category == InventoryCategory.COINS && draft.unitPrice.isBlank()) {
            val c = draft.coinType ?: CoinType.EMAMI
            val price = state.value.rates.getCoinPrice(c)
            if (price > 0L) draft.copy(unitPrice = price.toString()) else draft
        } else draft
        mutable.update { it.copy(draft = populatedDraft, draftCategoryId = item?.id?.let(it.options.categoryIds::get), error = null, notice = null) }
    }
    fun edit(change: (InventoryDraft) -> InventoryDraft) {
        if (!state.value.saving) mutable.update { current -> current.copy(draft = current.draft?.let(change)?.copy(errors = emptyMap())) }
    }
    fun category(value: InventoryCategory) {
        if (state.value.saving || state.value.draft?.let { it.category == value && state.value.draftCategoryId == null } != false) return
        val isCoin = value == InventoryCategory.COINS
        val selectedCoin = if (isCoin) (state.value.draft?.coinType ?: CoinType.EMAMI) else null
        val defaultCoinPrice = if (isCoin && selectedCoin != null) state.value.rates.getCoinPrice(selectedCoin).takeIf { it > 0 }?.toString().orEmpty() else ""
        edit {
            if (isCoin) {
                val coin = selectedCoin ?: CoinType.EMAMI
                it.copy(
                    category = value,
                    coinType = coin,
                    title = if (it.title.isBlank() || CoinType.entries.any { c -> c.titleFa == it.title }) coin.titleFa else it.title,
                    gross = it.gross.ifBlank { coin.totalWeightGrams.toString() },
                    stone = "0",
                    purity = "900",
                    karat = Karat.K21,
                    wage = "0",
                    profit = "0",
                    tax = "0",
                    location = if (it.location == "ویترین اصلی") "گاوصندوق مسکوکات" else it.location,
                    unitPrice = if (it.unitPrice.isBlank()) defaultCoinPrice else it.unitPrice
                )
            } else {
                it.copy(
                    category = value,
                    coinType = null,
                    profit = when (value) { InventoryCategory.JEWELRY -> "20"; else -> "7" },
                    tax = "9",
                    purity = if (it.purity == "900") "750" else it.purity,
                    karat = if (it.karat == Karat.K21 && it.purity == "900") Karat.K18 else it.karat,
                    wage = if (it.wage == "0") "10" else it.wage
                )
            }
        }
        mutable.update { it.copy(draftCategoryId = null) }
    }
    fun coinType(coin: CoinType) {
        if (state.value.saving || state.value.draft == null) return
        val liveRate = state.value.rates.getCoinPrice(coin)
        val rateStr = if (liveRate > 0) liveRate.toString() else ""
        edit {
            it.copy(
                category = InventoryCategory.COINS,
                coinType = coin,
                title = coin.titleFa,
                gross = coin.totalWeightGrams.toString(),
                stone = "0",
                purity = "900",
                karat = Karat.K21,
                wage = "0",
                profit = "0",
                tax = "0",
                unitPrice = rateStr
            )
        }
    }
    fun customCategory(id: String) {
        if (state.value.saving || state.value.draft == null || state.value.options.categories.none { it.id == id }) return
        if (state.value.draftCategoryId != id) edit { it.copy(category = InventoryCategory.MISC, profit = "7") }
        mutable.update { it.copy(draftCategoryId = id) }
    }
    fun addCategory(name: String): Job? = if (state.value.draft == null) null else operation("دسته‌بندی اضافه نشد؛ دوباره تلاش کنید") {
        val option = storage.addInventoryOption(DesktopInventoryOptions.CATEGORIES, name)
        mutable.update { current -> current.copy(options = storage.loadInventoryOptions(), draftCategoryId = option.id,
            draft = current.draft?.copy(category = InventoryCategory.MISC, profit = "7", errors = emptyMap())) }
    }
    fun addLocation(name: String): Job? = if (state.value.draft == null) null else operation("محل نگهداری اضافه نشد؛ دوباره تلاش کنید") {
        val option = storage.addInventoryOption(DesktopInventoryOptions.LOCATIONS, name)
        mutable.update { current -> current.copy(options = storage.loadInventoryOptions(),
            draft = current.draft?.copy(location = option.name, errors = emptyMap())) }
    }
    fun karat(value: Karat) = edit { it.copy(karat = value, purity = when(value) { Karat.K21 -> "875"; Karat.K24 -> "999"; else -> "750" }) }
    fun dismiss() {
        if (state.value.saving) return
        val changed = state.value.movement?.copy(errors = emptyMap())?.let { it != movementBaseline } == true
        if (changed) mutable.update { it.copy(discard = true) } else discard()
    }
    fun cancelDiscard() { mutable.update { it.copy(discard = false) } }
    fun discard() { if (!state.value.saving) mutable.update { it.copy(draft = null, draftCategoryId = null, movement = null, discard = false) } }
    fun save(): Job? {
        val draft = state.value.draft ?: return null
        val categoryId = state.value.draftCategoryId
        val errors = InventoryForm.validate(draft).toMutableMap()
        if (draft.id != null && state.value.items.any { it.id != draft.id && it.code.equals(draft.code.trim(), true) }) errors["code"] = "این کد قبلاً برای کالای دیگری ثبت شده است"
        if (errors.isNotEmpty()) { mutable.update { it.copy(draft = draft.copy(errors = errors)) }; return null }
        return operation("ذخیره کالا انجام نشد؛ اطلاعات قبلی حفظ شده‌اند") {
            val item = storage.saveInventoryProduct(InventoryForm.toItem(draft), draft.id == null, categoryId)
            reload { it.copy(draft = null, selectedId = item.id, notice = "کالا ذخیره شد") }
        }
    }
    fun saveAndPrint(): Job? {
        val draft = state.value.draft ?: return null
        val categoryId = state.value.draftCategoryId
        val errors = InventoryForm.validate(draft).toMutableMap()
        if (draft.id != null && state.value.items.any { it.id != draft.id && it.code.equals(draft.code.trim(), true) }) errors["code"] = "این کد قبلاً برای کالای دیگری ثبت شده است"
        if (errors.isNotEmpty()) { mutable.update { it.copy(draft = draft.copy(errors = errors)) }; return null }
        return operation("ذخیره کالا انجام نشد؛ اطلاعات قبلی حفظ شده‌اند") {
            val item = storage.saveInventoryProduct(InventoryForm.toItem(draft), draft.id == null, categoryId)
            reload { it.copy(draft = null, selectedId = item.id, notice = "کالا ذخیره شد و اتیکت به صف چاپ ارسال گردید") }
            com.goldex.companion.desktop.data.InventoryLabelPrinter.print(item, state.value.categoryTitle(item))
        }
    }
    fun photo(path: Path): Job? = if (state.value.draft == null) null else operation("تصویر قابل ورود نیست؛ PNG یا JPEG با حجم کمتر از ۲۰ مگابایت انتخاب کنید") {
        val image = InventoryPhoto.read(path)
        mutable.update { it.copy(draft = it.draft?.copy(image = image)) }
    }
    fun printLabel(item: InventoryItem): Job? = operation("چاپ انجام نشد؛ اتصال و تنظیمات چاپگر را بررسی کنید") {
        val printed = com.goldex.companion.desktop.data.InventoryLabelPrinter.print(item, state.value.categoryTitle(item))
        mutable.update { it.copy(notice = if (printed) "اتیکت به چاپگر ارسال شد" else "چاپ لغو شد") }
    }
    fun requestDelete(item: InventoryItem?) { if (!state.value.saving) mutable.update { it.copy(pendingDelete = item) } }
    fun delete(): Job? {
        val item = state.value.pendingDelete ?: return null
        return operation("حذف کالا انجام نشد؛ اطلاعات قبلی حفظ شده‌اند") {
            store.deleteItem(item.id); reload { it.copy(pendingDelete = null, selectedId = null, notice = "کالا حذف شد؛ سوابق گردش حفظ شدند") }
        }
    }
    fun openMovement(item: InventoryItem, type: StockAdjustmentType = StockAdjustmentType.CHARGE) {
        if (state.value.saving || state.value.hasDialog) return
        movementBaseline = StockDraft(item.id, type, reason = if (type == StockAdjustmentType.CHARGE) "دریافت از کارگاه ساخت" else "خروج از انبار")
        mutable.update { it.copy(movement = movementBaseline, error = null, notice = null) }
    }
    fun editMovement(change: (StockDraft) -> StockDraft) { if (!state.value.saving) mutable.update { it.copy(movement = it.movement?.let(change)?.copy(errors = emptyMap())) } }
    fun showHistory(visible: Boolean) { mutable.update { it.copy(showHistory = visible) } }
    fun adjustedPreview(): Pair<Int, Double>? {
        val draft = state.value.movement ?: return null
        val item = state.value.items.firstOrNull { it.id == draft.itemId } ?: return null
        return runCatching { val count = InventoryForm.normalize(draft.count).toInt(); val quantity = InventoryForm.adjustedQuantity(item, draft.type, count); quantity to item.netGoldWeightGrams * quantity }.getOrNull()
    }
    fun saveMovement(): Job? {
        val draft = state.value.movement ?: return null
        val item = state.value.items.firstOrNull { it.id == draft.itemId } ?: return null
        val errors = mutableMapOf<String, String>()
        val count = InventoryForm.normalize(draft.count).takeIf { it.matches(Regex("[0-9]+")) }?.toIntOrNull()
        if (count == null || runCatching { InventoryForm.adjustedQuantity(item, draft.type, count) }.isFailure) errors["count"] = "تعداد مثبت وارد کنید؛ خروج نباید بیشتر از موجودی باشد"
        val normalizedWeight = InventoryForm.normalize(draft.weight)
        val weight = if (draft.weight.isBlank()) item.netGoldWeightGrams * (count ?: 0) else normalizedWeight.takeIf { it.matches(Regex("[0-9]+(\\.[0-9]{1,3})?")) }?.toDoubleOrNull()
        if (weight == null || !weight.isFinite() || weight !in 0.0..1_000_000_000_000.0) errors["weight"] = "وزن معتبر با حداکثر سه رقم اعشار وارد کنید"
        if (draft.reason.isBlank() || draft.reason.length > 200) errors["reason"] = "دلیل ورود یا خروج الزامی است"
        if (draft.note.length > 1000) errors["note"] = "یادداشت حداکثر ۱۰۰۰ حرف"
        if (errors.isNotEmpty()) { mutable.update { it.copy(movement = draft.copy(errors = errors)) }; return null }
        val movement = StockAdjustment(itemId = item.id, itemTitle = item.title, type = draft.type, quantityChange = count!!, weightGrams = weight!!, reason = draft.reason.trim(), note = draft.note.trim())
        return operation("گردش ثبت نشد؛ موجودی قبلی و سوابق حفظ شده‌اند") {
            store.adjustStock(movement); reload { it.copy(movement = null, notice = "گردش موجودی ثبت شد") }
        }
    }
    private fun reload(change: (DesktopInventoryState) -> DesktopInventoryState) {
        val items = store.getItems(); val history = store.getAdjustments()
        val options = storage.loadInventoryOptions()
        mutable.update { change(it.copy(items = items, history = history, options = options, draftCategoryId = null)) }
    }
    private fun operation(error: String, block: suspend () -> Unit): Job? {
        if (!writing.compareAndSet(false, true)) return null
        mutable.update { it.copy(saving = true, error = null, notice = null) }
        return scope.launch(Dispatchers.IO) {
            try { block() } catch (failure: CancellationException) { throw failure }
            catch (_: Exception) { mutable.update { it.copy(error = error) } }
            finally { mutable.update { it.copy(saving = false) }; writing.set(false) }
        }
    }
}
