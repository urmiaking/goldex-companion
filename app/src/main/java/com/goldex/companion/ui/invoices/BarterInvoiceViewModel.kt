package com.goldex.companion.ui.invoices

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.goldex.companion.data.CustomerRepository
import com.goldex.companion.data.CustomerStore
import com.goldex.companion.data.InvoiceRepository
import com.goldex.companion.data.InvoiceStore
import com.goldex.companion.domain.invoice.InvoiceLedgerSyncUseCase
import com.goldex.companion.domain.invoice.InvoiceDeletionUseCase
import com.goldex.companion.domain.invoice.InvoiceDeletionResult
import com.goldex.companion.model.PersianNumberFormatter
import com.goldex.companion.model.BarterBalance
import com.goldex.companion.model.BarterInvoice
import com.goldex.companion.model.BarterItem
import com.goldex.companion.model.Customer
import com.goldex.companion.model.CustomerRole
import com.goldex.companion.model.InvoiceItemCategory
import com.goldex.companion.model.SettlementMethod
import com.goldex.companion.model.SettlementPaymentItem
import com.goldex.companion.model.InvoiceCardAction
import com.goldex.companion.model.InvoiceFilterTab
import com.goldex.companion.model.InvoiceListItem
import com.goldex.companion.model.InvoiceStatus
import com.goldex.companion.data.GoldMarketRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class InvoicesSubScreen {
    LIST,
    EDITOR
}

data class BarterInvoiceUiState(
    val subScreen: InvoicesSubScreen = InvoicesSubScreen.LIST,
    val invoice: BarterInvoice = BarterInvoice(),
    val isItemModalVisible: Boolean = false,
    val targetCategory: InvoiceItemCategory = InvoiceItemCategory.CRAFTED,
    val isAddingToSales: Boolean = true,
    val editingItem: BarterItem? = null,
    val isRateEditDialogVisible: Boolean = false,
    val isSuccessSnackbarVisible: Boolean = false,
    val statusMessage: String = "",
    val searchQuery: String = "",
    val selectedFilter: InvoiceFilterTab = InvoiceFilterTab.ALL,
    val invoicesList: List<InvoiceListItem> = emptyList(),
    val isEditingExistingInvoice: Boolean = false
) {
    val balance: BarterBalance get() = invoice.balance

    val filteredInvoices: List<InvoiceListItem> get() {
        val byTab = when (selectedFilter) {
            InvoiceFilterTab.ALL -> invoicesList
            InvoiceFilterTab.SETTLED -> invoicesList.filter { it.status == InvoiceStatus.SETTLED }
            InvoiceFilterTab.PENDING -> invoicesList.filter { it.status == InvoiceStatus.PARTIALLY_PAID }
            InvoiceFilterTab.WORKSHOP -> invoicesList.filter { it.status == InvoiceStatus.WORKSHOP }
        }
        if (searchQuery.isBlank()) return byTab
        val q = searchQuery.trim().lowercase()
        return byTab.filter { item ->
            item.invoiceNumber.lowercase().contains(q) ||
            item.customerName.lowercase().contains(q) ||
            item.itemsSummary.lowercase().contains(q) ||
            item.statusDetail.lowercase().contains(q)
        }
    }
}

class BarterInvoiceViewModel(
    private val invoiceStore: InvoiceStore? = null,
    private val customerStore: CustomerStore? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(BarterInvoiceUiState())
    val uiState: StateFlow<BarterInvoiceUiState> = _uiState.asStateFlow()

    init {
        val defaultSpot = GoldMarketRepository.rates.value.gold18.takeIf { it > 0 } ?: 0L
        val savedBarterInvoices = invoiceStore?.getBarterInvoices() ?: emptyList()
        val initialItems = createInvoiceList(savedBarterInvoices)
        _uiState.update { current ->
            current.copy(
                invoice = BarterInvoice(spotPrice18k = defaultSpot),
                invoicesList = initialItems
            )
        }
    }

    fun setSubScreen(subScreen: InvoicesSubScreen) {
        _uiState.update { it.copy(subScreen = subScreen) }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun setSelectedFilter(filter: InvoiceFilterTab) {
        _uiState.update { it.copy(selectedFilter = filter) }
    }

    fun openNewInvoice(customSpotPrice: Long? = null) {
        val liveRate = GoldMarketRepository.rates.value.gold18.takeIf { it > 0 } ?: 23_360_000L
        val rateToUse = customSpotPrice ?: (if (_uiState.value.invoice.spotPrice18k > 0L) _uiState.value.invoice.spotPrice18k else liveRate)
        _uiState.update {
            it.copy(
                subScreen = InvoicesSubScreen.EDITOR,
                isEditingExistingInvoice = false,
                invoice = BarterInvoice(
                    spotPrice18k = rateToUse
                )
            )
        }
    }

    fun openInvoiceDetails(item: InvoiceListItem) {
        val liveRate = GoldMarketRepository.rates.value.gold18.takeIf { it > 0 } ?: 23_360_000L
        val invoiceToEdit = item.barterInvoice ?: BarterInvoice(
            invoiceNumber = item.invoiceNumber,
            customer = Customer(name = item.customerName),
            spotPrice18k = liveRate
        )
        _uiState.update {
            it.copy(
                subScreen = InvoicesSubScreen.EDITOR,
                isEditingExistingInvoice = true,
                invoice = invoiceToEdit
            )
        }
    }

    fun navigateBackToList() {
        _uiState.update { it.copy(subScreen = InvoicesSubScreen.LIST) }
    }

    private fun createInvoiceListItem(currentInv: BarterInvoice): InvoiceListItem {
        val netAmount = currentInv.balance.totalSalesAmount.toLong().coerceAtLeast(0L)
        val customerName = currentInv.customer?.name?.ifBlank { "مشتری جدید" } ?: "مشتری جدید"
        val initials = customerName.split(" ").take(2).mapNotNull { it.firstOrNull()?.toString() }.joinToString("").ifBlank { "مش" }

        val hasAnyPayment = currentInv.payments.isNotEmpty() ||
                currentInv.cashPosAmount > 0L ||
                currentInv.bullionWeight > 0.0 ||
                currentInv.thirdPartyTransferAmount > 0L ||
                currentInv.thirdPartyTransferWeight18k > 0.0

        val isSettled = if (currentInv.balance.isSettled) {
            true
        } else if (currentInv.payments.isNotEmpty()) {
            currentInv.isFullySettled
        } else {
            when (currentInv.settlementMethod) {
                SettlementMethod.TRANSFER -> currentInv.thirdPartyTransferAmount > 0 || currentInv.thirdPartyTransferWeight18k > 0.0
                SettlementMethod.POS -> currentInv.cashPosAmount >= netAmount && netAmount > 0
                SettlementMethod.BULLION -> currentInv.bullionWeight > 0.0
                SettlementMethod.LEDGER -> false
            }
        }

        val line2 = if (currentInv.payments.size > 1) {
            "تسویه چندمرحله‌ای (${com.goldex.companion.model.PersianNumberFormatter.toPersianDigits(currentInv.payments.size.toString())} روش پرداخت)"
        } else if (currentInv.payments.size == 1) {
            "روش تسویه: ${currentInv.payments.first().method.labelFa}"
        } else if (!hasAnyPayment) {
            "نسیه / ثبت در حساب دفتری"
        } else {
            when (currentInv.settlementMethod) {
                SettlementMethod.TRANSFER -> {
                    val partyName = currentInv.thirdPartyCustomer?.name?.ifBlank { "همکار" } ?: "همکار"
                    "تهاتر سه‌طرفه: حواله به $partyName (${currentInv.thirdPartyInvoiceNumber.ifBlank { "دفتر حساب" }})"
                }
                SettlementMethod.POS -> if (currentInv.cashPosAmount > 0) "روش تسویه: کارتخوان / پوز" else "نسیه / حساب دفتری"
                SettlementMethod.LEDGER -> "روش تسویه: دفتر معین طلایی (${currentInv.ledgerDueDate})"
                SettlementMethod.BULLION -> "روش تسویه: تحویل شمش و آبشده"
            }
        }

        return InvoiceListItem(
            id = currentInv.id,
            invoiceNumber = currentInv.cleanInvoiceNumber,
            customerName = customerName,
            customerInitials = initials,
            isVerified = true,
            createdAtText = "کد فاکتور: ${currentInv.cleanInvoiceNumber} • همین الان",
            status = if (isSettled) InvoiceStatus.SETTLED else InvoiceStatus.PARTIALLY_PAID,
            statusDetail = if (isSettled) {
                if (currentInv.balance.isSettled && !hasAnyPayment) "تسویه با تهاتر اقلام"
                else if (currentInv.payments.size > 1) "تسویه چندمرحله‌ای کامل"
                else if (currentInv.settlementMethod == SettlementMethod.TRANSFER) "تسویه با حواله سه‌طرفه"
                else "تسویه نقدی کامل"
            } else if (currentInv.payments.isNotEmpty() && currentInv.remainingBalanceTomans > 0L) {
                "مانده: ${com.goldex.companion.model.PersianNumberFormatter.formatPrice(currentInv.remainingBalanceTomans.toDouble())} ت"
            } else if (!hasAnyPayment) {
                "نسیه (مانده دفتری)"
            } else "در انتظار پرداخت",
            itemsSummary = (currentInv.salesItems + currentInv.receivedItems).joinToString(" + ") { it.title }.ifBlank { "اقلام طلا و مسکوکات" },
            itemsCountText = "اقلام فاکتور (${com.goldex.companion.model.PersianNumberFormatter.toPersianDigits((currentInv.salesItems.size + currentInv.receivedItems.size).toString())} قلم):",
            line1Detail = "وزن کل: ${com.goldex.companion.model.PersianNumberFormatter.formatWeight(currentInv.salesItems.sumOf { it.equivalent18kWeight })} گرم",
            line2Detail = line2,
            finalAmount = netAmount,
            amountLabel = "مبلغ نهایی فاکتور:",
            actionButtonText = "مشاهده جزییات",
            actionType = InvoiceCardAction.VIEW_DETAILS,
            barterInvoice = currentInv
        )
    }

    fun submitAndSaveCurrentInvoice() {
        val currentInv = _uiState.value.invoice

        // 1. Persist invoice to store
        invoiceStore?.saveBarterInvoice(currentInv)

        // 2. Synchronize with Customer Ledger
        val targetCustomer = currentInv.customer
        if (targetCustomer != null && customerStore != null) {
            val allCustomers = customerStore.getCustomers()
            val matchedCustomer = allCustomers.firstOrNull { it.id == targetCustomer.id } ?: targetCustomer

            val existingTxs = customerStore.getTransactionsByInvoiceId(currentInv.id)
            val customerAfterReversal = if (existingTxs.isNotEmpty()) {
                customerStore.deleteTransactionsByInvoiceId(currentInv.id)
                InvoiceLedgerSyncUseCase.reverseLedgerSync(existingTxs, matchedCustomer)
            } else {
                matchedCustomer
            }

            if (currentInv.syncWithLedger) {
                val syncResult = InvoiceLedgerSyncUseCase.generateLedgerSync(currentInv, customerAfterReversal)
                syncResult.transactionsToCreate.forEach { tx ->
                    customerStore.addTransaction(tx)
                }
                customerStore.updateCustomer(syncResult.updatedCustomer)
            } else if (existingTxs.isNotEmpty()) {
                customerStore.updateCustomer(customerAfterReversal)
            }
        }

        _uiState.update { state ->
            val savedInvoices = invoiceStore?.getBarterInvoices()
                ?: (listOf(currentInv) + state.invoicesList.mapNotNull { it.barterInvoice }.filterNot { it.id == currentInv.id })
            val finalList = createInvoiceList(savedInvoices)
            state.copy(
                invoicesList = finalList,
                subScreen = InvoicesSubScreen.LIST,
                isSuccessSnackbarVisible = true,
                statusMessage = if (state.isEditingExistingInvoice) {
                    "فاکتور ${currentInv.cleanInvoiceNumber} با موفقیت ویرایش شد"
                } else if (currentInv.settlementMethod == SettlementMethod.TRANSFER && currentInv.thirdPartyCustomer != null) {
                    "فاکتور ${currentInv.cleanInvoiceNumber} ثبت و تهاتر با ${currentInv.thirdPartyCustomer.name} با موفقیت اعمال شد"
                } else {
                    "فاکتور ${currentInv.cleanInvoiceNumber} با موفقیت ثبت گردید"
                }
            )
        }
    }

    fun setSettlementPayments(payments: List<SettlementPaymentItem>) {
        _uiState.update {
            it.copy(
                invoice = it.invoice.copy(
                    payments = payments
                )
            )
        }
    }

    fun setLiveRate(rate: Long) {
        if (rate > 0) {
            _uiState.update { it.copy(invoice = it.invoice.copy(spotPrice18k = rate)) }
        }
    }

    fun setCustomer(customer: Customer?) {
        _uiState.update { it.copy(invoice = it.invoice.copy(customer = customer)) }
    }

    fun setCustomerRole(role: CustomerRole) {
        _uiState.update { it.copy(invoice = it.invoice.copy(customerRole = role)) }
    }

    fun setSettlementMethod(method: SettlementMethod) {
        _uiState.update { it.copy(invoice = it.invoice.copy(settlementMethod = method)) }
    }

    fun setCashPosAmount(amount: Long) {
        _uiState.update { it.copy(invoice = it.invoice.copy(cashPosAmount = amount)) }
    }

    fun setLedgerAmount(amount: Long) {
        _uiState.update { it.copy(invoice = it.invoice.copy(ledgerAmount = amount)) }
    }

    fun setPosTrackingCode(code: String) {
        _uiState.update { it.copy(invoice = it.invoice.copy(posTrackingCode = code)) }
    }

    fun setLedgerDueDate(dueDate: String) {
        _uiState.update { it.copy(invoice = it.invoice.copy(ledgerDueDate = dueDate)) }
    }

    fun setBullionSettlement(weight: Double, karat: Int, angNumber: String) {
        _uiState.update {
            it.copy(
                invoice = it.invoice.copy(
                    bullionWeight = weight,
                    bullionKarat = karat,
                    bullionAngNumber = angNumber
                )
            )
        }
    }

    fun setThirdPartyTransfer(
        customer: Customer?,
        invoiceId: String,
        invoiceNumber: String,
        weight18k: Double,
        amount: Long,
        trackingCode: String
    ) {
        _uiState.update {
            it.copy(
                invoice = it.invoice.copy(
                    thirdPartyCustomer = customer,
                    thirdPartyInvoiceId = invoiceId,
                    thirdPartyInvoiceNumber = invoiceNumber,
                    thirdPartyTransferWeight18k = weight18k,
                    thirdPartyTransferAmount = amount,
                    thirdPartyTrackingCode = trackingCode
                )
            )
        }
    }

    fun setNote(note: String) {
        _uiState.update { it.copy(invoice = it.invoice.copy(note = note)) }
    }

    fun openAddItemModal(category: InvoiceItemCategory, isSales: Boolean) {
        _uiState.update {
            it.copy(
                isItemModalVisible = true,
                targetCategory = category,
                isAddingToSales = isSales,
                editingItem = null
            )
        }
    }

    fun openEditItemModal(item: BarterItem, isSales: Boolean) {
        _uiState.update {
            it.copy(
                isItemModalVisible = true,
                targetCategory = item.category,
                isAddingToSales = isSales,
                editingItem = item
            )
        }
    }

    fun closeItemModal() {
        _uiState.update { it.copy(isItemModalVisible = false, editingItem = null) }
    }

    fun saveItem(item: BarterItem) {
        val isSales = _uiState.value.isAddingToSales
        val editing = _uiState.value.editingItem

        _uiState.update { state ->
            val updatedInvoice = if (isSales) {
                val items = if (editing != null) {
                    state.invoice.salesItems.map { if (it.id == editing.id) item else it }
                } else {
                    state.invoice.salesItems + item
                }
                state.invoice.copy(salesItems = items)
            } else {
                val items = if (editing != null) {
                    state.invoice.receivedItems.map { if (it.id == editing.id) item else it }
                } else {
                    state.invoice.receivedItems + item
                }
                state.invoice.copy(receivedItems = items)
            }
            state.copy(invoice = updatedInvoice, isItemModalVisible = false, editingItem = null)
        }
    }

    fun addSalesItem(item: BarterItem) {
        _uiState.update { state ->
            state.copy(invoice = state.invoice.copy(salesItems = state.invoice.salesItems + item))
        }
    }

    fun deleteSalesItem(id: String) {
        _uiState.update { state ->
            state.copy(invoice = state.invoice.copy(salesItems = state.invoice.salesItems.filterNot { it.id == id }))
        }
    }

    fun deleteReceivedItem(id: String) {
        _uiState.update { state ->
            state.copy(invoice = state.invoice.copy(receivedItems = state.invoice.receivedItems.filterNot { it.id == id }))
        }
    }

    fun setRateEditDialogVisible(visible: Boolean) {
        _uiState.update { it.copy(isRateEditDialogVisible = visible) }
    }

    fun updateSpotPrice(newPrice: Long) {
        if (newPrice > 0) {
            _uiState.update {
                it.copy(
                    invoice = it.invoice.copy(spotPrice18k = newPrice),
                    isRateEditDialogVisible = false
                )
            }
        }
    }

    fun setSyncWithLedger(sync: Boolean) {
        _uiState.update { it.copy(invoice = it.invoice.copy(syncWithLedger = sync)) }
    }

    fun deleteInvoice(invoiceId: String): Boolean {
        val result = invoiceStore?.let { InvoiceDeletionUseCase(it, customerStore).delete(invoiceId) }
            ?: InvoiceDeletionResult.Failed(rollbackSucceeded = true)
        val deleted = result is InvoiceDeletionResult.Deleted
        val message = when (result) {
            is InvoiceDeletionResult.Deleted -> "فاکتور ${PersianNumberFormatter.toPersianDigits(result.invoice.cleanInvoiceNumber)} حذف و آثار دفتری و پرداخت‌های مرتبط برگشت داده شد"
            InvoiceDeletionResult.NotFound -> "فاکتور مورد نظر یافت نشد"
            InvoiceDeletionResult.MissingLedgerStore -> "حذف فاکتور بدون دسترسی به دفتر حساب امکان‌پذیر نیست"
            InvoiceDeletionResult.MissingCustomer -> "طرف‌حساب اسناد مرتبط یافت نشد؛ ابتدا دفتر حساب را بررسی کنید"
            InvoiceDeletionResult.ReferencedByTransfer -> "ابتدا حواله فاکتورهای مرتبط به این فاکتور را حذف یا ویرایش کنید"
            is InvoiceDeletionResult.Failed -> if (result.rollbackSucceeded) {
                "حذف فاکتور انجام نشد؛ اطلاعات قبلی حفظ شد"
            } else {
                "خطا در بازیابی اطلاعات؛ پیش از تلاش مجدد دفتر حساب را بررسی کنید"
            }
        }
        _uiState.update { state ->
            val clearEditor = (deleted || result == InvoiceDeletionResult.NotFound) && state.invoice.id == invoiceId
            state.copy(
                invoicesList = when (result) {
                    is InvoiceDeletionResult.Deleted -> createInvoiceList(result.remainingInvoices)
                    InvoiceDeletionResult.NotFound -> createInvoiceList(state.invoicesList.mapNotNull { it.barterInvoice }.filterNot { it.id == invoiceId })
                    else -> state.invoicesList
                },
                invoice = if (clearEditor) BarterInvoice(spotPrice18k = state.invoice.spotPrice18k) else state.invoice,
                subScreen = if (clearEditor) InvoicesSubScreen.LIST else state.subScreen,
                isEditingExistingInvoice = if (clearEditor) false else state.isEditingExistingInvoice,
                isItemModalVisible = if (clearEditor) false else state.isItemModalVisible,
                editingItem = if (clearEditor) null else state.editingItem,
                isSuccessSnackbarVisible = deleted,
                statusMessage = message
            )
        }
        return deleted
    }

    // Transfer target cards are a projection; rebuilding removes a deleted source's deduction
    // and reapplies only the transfers that still exist, including after an app restart.
    private fun createInvoiceList(invoices: List<BarterInvoice>): List<InvoiceListItem> {
        var cards = invoices.map(::createInvoiceListItem)
        invoices.asReversed().forEach { source ->
            if (source.settlementMethod == SettlementMethod.TRANSFER && source.thirdPartyInvoiceId.isNotBlank()) {
                cards = cards.map { card ->
                    if (card.id != source.thirdPartyInvoiceId || card.id == source.id) card else {
                        val remaining = (card.finalAmount - source.thirdPartyTransferAmount).coerceAtLeast(0L)
                        card.copy(
                            finalAmount = remaining,
                            status = if (remaining == 0L) InvoiceStatus.SETTLED else InvoiceStatus.PARTIALLY_PAID,
                            statusDetail = if (remaining == 0L) "تسویه کامل با تهاتر فاکتور ${source.cleanInvoiceNumber}"
                                else "مانده پس از تهاتر: ${PersianNumberFormatter.formatPrice(remaining.toDouble())} ت",
                            line2Detail = "کسر ${PersianNumberFormatter.formatWeight(source.thirdPartyTransferWeight18k)} گرم طلا بابت تهاتر حواله ${source.cleanInvoiceNumber}"
                        )
                    }
                }
            }
        }
        return cards
    }

    fun resetNewInvoice() {
        _uiState.update {
            it.copy(
                invoice = BarterInvoice(
                    spotPrice18k = it.invoice.spotPrice18k
                )
            )
        }
    }
}

class BarterInvoiceViewModelFactory(
    private val application: Application
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val invoiceStore = InvoiceRepository(application.applicationContext)
        val customerStore = CustomerRepository(application.applicationContext)
        return BarterInvoiceViewModel(invoiceStore, customerStore) as T
    }
}

