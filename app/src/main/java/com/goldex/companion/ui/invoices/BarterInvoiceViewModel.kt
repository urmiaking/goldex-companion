package com.goldex.companion.ui.invoices

import androidx.lifecycle.ViewModel
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

class BarterInvoiceViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(BarterInvoiceUiState())
    val uiState: StateFlow<BarterInvoiceUiState> = _uiState.asStateFlow()

    init {
        val defaultSpot = GoldMarketRepository.rates.value.gold18.takeIf { it > 0 } ?: 0L
        _uiState.update { current ->
            current.copy(
                invoice = BarterInvoice(spotPrice18k = defaultSpot),
                invoicesList = emptyList()
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

    fun submitAndSaveCurrentInvoice() {
        val currentInv = _uiState.value.invoice
        val netAmount = currentInv.balance.totalSalesAmount.toLong().coerceAtLeast(0L)
        val customerName = currentInv.customer?.name?.ifBlank { "مشتری جدید" } ?: "مشتری جدید"
        val initials = customerName.split(" ").take(2).mapNotNull { it.firstOrNull()?.toString() }.joinToString("").ifBlank { "مش" }

        val isSettled = if (currentInv.payments.isNotEmpty()) {
            currentInv.isFullySettled
        } else {
            when (currentInv.settlementMethod) {
                SettlementMethod.TRANSFER -> currentInv.thirdPartyTransferAmount > 0 || currentInv.thirdPartyTransferWeight18k > 0.0
                SettlementMethod.POS -> currentInv.balance.isSettled || (currentInv.cashPosAmount >= netAmount && netAmount > 0)
                SettlementMethod.BULLION -> currentInv.bullionWeight > 0.0
                SettlementMethod.LEDGER -> true
            }
        }

        val line2 = if (currentInv.payments.size > 1) {
            "تسویه چندمرحله‌ای (${com.goldex.companion.model.PersianNumberFormatter.toPersianDigits(currentInv.payments.size.toString())} روش پرداخت)"
        } else if (currentInv.payments.size == 1) {
            "روش تسویه: ${currentInv.payments.first().method.labelFa}"
        } else {
            when (currentInv.settlementMethod) {
                SettlementMethod.TRANSFER -> {
                    val partyName = currentInv.thirdPartyCustomer?.name?.ifBlank { "همکار" } ?: "همکار"
                    "تهاتر سه‌طرفه: حواله به $partyName (${currentInv.thirdPartyInvoiceNumber.ifBlank { "دفتر حساب" }})"
                }
                SettlementMethod.POS -> "روش تسویه: کارتخوان / پوز"
                SettlementMethod.LEDGER -> "روش تسویه: دفتر معین طلایی (${currentInv.ledgerDueDate})"
                SettlementMethod.BULLION -> "روش تسویه: تحویل شمش و آبشده"
            }
        }

        val newCard = InvoiceListItem(
            id = currentInv.id,
            invoiceNumber = currentInv.invoiceNumber,
            customerName = customerName,
            customerInitials = initials,
            isVerified = true,
            createdAtText = "کد فاکتور: ${currentInv.invoiceNumber} • همین الان",
            status = if (isSettled) InvoiceStatus.SETTLED else InvoiceStatus.PARTIALLY_PAID,
            statusDetail = if (isSettled) {
                if (currentInv.payments.size > 1) "تسویه چندمرحله‌ای کامل"
                else if (currentInv.settlementMethod == SettlementMethod.TRANSFER) "تسویه با حواله سه‌طرفه"
                else "تسویه نقدی کامل"
            } else if (currentInv.payments.isNotEmpty() && currentInv.remainingBalanceTomans > 0L) {
                "مانده: ${com.goldex.companion.model.PersianNumberFormatter.formatPrice(currentInv.remainingBalanceTomans.toDouble())} ت"
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

        _uiState.update { state ->
            val updatedInvoices = state.invoicesList.map { invoiceItem ->
                if (currentInv.settlementMethod == SettlementMethod.TRANSFER &&
                    currentInv.thirdPartyInvoiceId.isNotBlank() &&
                    invoiceItem.id == currentInv.thirdPartyInvoiceId
                ) {
                    val deductedAmount = (invoiceItem.finalAmount - currentInv.thirdPartyTransferAmount).coerceAtLeast(0L)
                    val targetStatus = if (deductedAmount <= 0L) InvoiceStatus.SETTLED else InvoiceStatus.PARTIALLY_PAID
                    val targetStatusDetail = if (deductedAmount <= 0L) {
                        "تسویه کامل با تهاتر فاکتور ${currentInv.invoiceNumber}"
                    } else {
                        "مانده پس از تهاتر: ${com.goldex.companion.model.PersianNumberFormatter.formatPrice(deductedAmount.toDouble())} ت"
                    }
                    invoiceItem.copy(
                        finalAmount = deductedAmount,
                        status = targetStatus,
                        statusDetail = targetStatusDetail,
                        line2Detail = "کسر ${com.goldex.companion.model.PersianNumberFormatter.formatWeight(currentInv.thirdPartyTransferWeight18k)} گرم طلا بابت تهاتر حواله ${currentInv.invoiceNumber}"
                    )
                } else {
                    invoiceItem
                }
            }

            val finalList = listOf(newCard) + updatedInvoices.filterNot { it.id == newCard.id }
            state.copy(
                invoicesList = finalList,
                subScreen = InvoicesSubScreen.LIST,
                isSuccessSnackbarVisible = true,
                statusMessage = if (state.isEditingExistingInvoice) {
                    "فاکتور ${currentInv.invoiceNumber} با موفقیت ویرایش شد"
                } else if (currentInv.settlementMethod == SettlementMethod.TRANSFER && currentInv.thirdPartyCustomer != null) {
                    "فاکتور ${currentInv.invoiceNumber} ثبت و تهاتر با ${currentInv.thirdPartyCustomer.name} با موفقیت اعمال شد"
                } else {
                    "فاکتور ${currentInv.invoiceNumber} با موفقیت ثبت گردید"
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
