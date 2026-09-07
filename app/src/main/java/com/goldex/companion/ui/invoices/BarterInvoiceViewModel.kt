package com.goldex.companion.ui.invoices

import androidx.lifecycle.ViewModel
import com.goldex.companion.model.BankCoinItem
import com.goldex.companion.model.BarterBalance
import com.goldex.companion.model.BarterInvoice
import com.goldex.companion.model.BarterItem
import com.goldex.companion.model.CoinType
import com.goldex.companion.model.CraftedGoldItem
import com.goldex.companion.model.Customer
import com.goldex.companion.model.CustomerRole
import com.goldex.companion.model.InvoiceItemCategory
import com.goldex.companion.model.Karat
import com.goldex.companion.model.MeltGoldItem
import com.goldex.companion.model.ScrapGoldItem
import com.goldex.companion.model.SettlementMethod
import com.goldex.companion.model.WageType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class BarterInvoiceUiState(
    val invoice: BarterInvoice = BarterInvoice(),
    val isItemModalVisible: Boolean = false,
    val targetCategory: InvoiceItemCategory = InvoiceItemCategory.CRAFTED,
    val isAddingToSales: Boolean = true,
    val editingItem: BarterItem? = null,
    val isRateEditDialogVisible: Boolean = false,
    val isSuccessSnackbarVisible: Boolean = false,
    val statusMessage: String = ""
) {
    val balance: BarterBalance get() = invoice.balance
}

class BarterInvoiceViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(BarterInvoiceUiState())
    val uiState: StateFlow<BarterInvoiceUiState> = _uiState.asStateFlow()

    init {
        // Seed realistic default sample items matching Stitch design b4183cc19232433ab7200ba9a9074680
        val sampleSale1 = CraftedGoldItem(
            title = "دستبند کارتیه ۱۸ عیار لوکس",
            karat = Karat.K18,
            grossWeight = 12.80,
            stoneWeight = 0.30,
            netWeight = 12.50,
            spotPrice = 3560000L,
            wageType = WageType.PERCENTAGE,
            wageInput = 7.5,
            wageAmount = 3337500.0,
            profitPercent = 7.0,
            profitAmount = 3348625.0,
            taxPercent = 9.0,
            taxAmount = 601751.0,
            rawGoldValue = 44500000.0,
            totalPayable = 51787876.0,
            equivalent18kWeight = 12.50
        )
        val sampleSale2 = BankCoinItem(
            title = "تمام سکه بهار آزادی طرح جدید",
            coinType = CoinType.EMAMI,
            count = 1,
            hasHologram = true,
            unitPrice = 42500000L,
            totalPayable = 42500000.0,
            equivalent18kWeight = 8.13598 * 1.2
        )

        val sampleReceived1 = ScrapGoldItem(
            title = "طلای متفرقه و کهنه ۱۸ عیار",
            baseKarat = 750,
            karatDeficit = 15,
            payableKarat = 735,
            grossWeight = 8.35,
            stoneWeight = 0.15,
            netWeight = 8.20,
            spotPrice = 3560000L,
            deductionPerGram = 15000L,
            exchangeCommissionPercent = 0.0,
            effectiveGramPrice = 3473800L,
            totalPayable = 28485160.0,
            equivalent18kWeight = 8.20 * (735.0 / 750.0)
        )
        val sampleReceived2 = MeltGoldItem(
            title = "طلای آبشده سنتی (انگ ۱۲۴۸)",
            weight = 5.24,
            labKarat = 735,
            angNumber = "1248",
            labName = "ری‌گیری مشهد",
            spotPrice = 3560000L,
            totalPayable = 18280600.0,
            equivalent18kWeight = 5.24 * (735.0 / 750.0)
        )

        _uiState.update { current ->
            current.copy(
                invoice = current.invoice.copy(
                    customer = Customer(
                        name = "حاج محمد کاظمی",
                        phone = "09123456789",
                        note = "بنکداری تهران • مانده قبلی: ۵.۲۰۰ گرم بستانکار"
                    ),
                    customerRole = CustomerRole.WHOLESALER,
                    spotPrice18k = 3560000L,
                    salesItems = listOf(sampleSale1, sampleSale2),
                    receivedItems = listOf(sampleReceived1, sampleReceived2),
                    cashPosAmount = 30000000L,
                    note = "تهاتر شده با طلای کهنه و آبشده انگ ۱۲۴۸. مانده دفتری در صورتحساب ماهانه."
                )
            )
        }
    }

    fun setLiveRate(rate: Long) {
        if (rate > 0 && rate != _uiState.value.invoice.spotPrice18k) {
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
            BarterInvoiceUiState(
                invoice = BarterInvoice(
                    spotPrice18k = it.invoice.spotPrice18k
                )
            )
        }
    }
}
