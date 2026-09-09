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
    val invoicesList: List<InvoiceListItem> = emptyList()
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
        val defaultSpot = GoldMarketRepository.rates.value.gold18.takeIf { it > 0 } ?: 23_360_000L

        // Seed realistic default sample items matching Stitch design b4183cc19232433ab7200ba9a9074680
        val sampleSale1 = CraftedGoldItem(
            title = "دستبند کارتیه ۱۸ عیار لوکس",
            karat = Karat.K18,
            grossWeight = 12.80,
            stoneWeight = 0.30,
            netWeight = 12.50,
            spotPrice = defaultSpot,
            wageType = WageType.PERCENTAGE,
            wageInput = 7.5,
            wageAmount = 12.50 * defaultSpot * (7.5 / 100.0),
            profitPercent = 7.0,
            profitAmount = (12.50 * defaultSpot * 1.075) * (7.0 / 100.0),
            taxPercent = 9.0,
            taxAmount = ((12.50 * defaultSpot * (7.5 / 100.0)) + ((12.50 * defaultSpot * 1.075) * (7.0 / 100.0))) * 0.09,
            rawGoldValue = 12.50 * defaultSpot,
            totalPayable = (12.50 * defaultSpot) + (12.50 * defaultSpot * (7.5 / 100.0)) + ((12.50 * defaultSpot * 1.075) * (7.0 / 100.0)) + (((12.50 * defaultSpot * (7.5 / 100.0)) + ((12.50 * defaultSpot * 1.075) * (7.0 / 100.0))) * 0.09),
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
            spotPrice = defaultSpot,
            deductionPerGram = 15000L,
            exchangeCommissionPercent = 0.0,
            effectiveGramPrice = defaultSpot - 15000L,
            totalPayable = 8.20 * (defaultSpot - 15000L) * (735.0 / 750.0),
            equivalent18kWeight = 8.20 * (735.0 / 750.0)
        )
        val sampleReceived2 = MeltGoldItem(
            title = "طلای آبشده سنتی (انگ ۱۲۴۸)",
            weight = 5.24,
            labKarat = 735,
            angNumber = "1248",
            labName = "ری‌گیری مشهد",
            spotPrice = defaultSpot,
            totalPayable = 5.24 * defaultSpot * (735.0 / 750.0),
            equivalent18kWeight = 5.24 * (735.0 / 750.0)
        )

        val sampleInvoice1 = BarterInvoice(
            invoiceNumber = "IR-1403-089",
            customer = Customer(
                name = "حاج محمد کاظمی",
                phone = "09123456789",
                note = "بنکداری تهران • مانده قبلی: ۵.۲۰۰ گرم بستانکار"
            ),
            customerRole = CustomerRole.WHOLESALER,
            spotPrice18k = defaultSpot,
            salesItems = listOf(sampleSale1, sampleSale2),
            receivedItems = listOf(sampleReceived1, sampleReceived2),
            cashPosAmount = 30000000L,
            note = "تهاتر شده با طلای کهنه و آبشده انگ ۱۲۴۸. مانده دفتری در صورتحساب ماهانه."
        )

        val card1 = InvoiceListItem(
            id = "sample-1",
            invoiceNumber = "IR-1403-089",
            customerName = "حاج محمد کاظمی",
            customerInitials = "مک",
            isVerified = true,
            createdAtText = "کد فاکتور: IR-1403-089 • امروز، ۱۱:۴۵",
            status = InvoiceStatus.SETTLED,
            statusDetail = "تسویه نقدی کامل",
            itemsSummary = "دستبند کارتیه ۱۸ عیار + نیم‌ست برلیان",
            itemsCountText = "اقلام فاکتور (۲ قلم):",
            line1Detail = "وزن خالص طلا: ۱۲.۵۰ گرم",
            line2Detail = "اجرت ساخت: ۷.۵٪",
            finalAmount = 241500000L,
            amountLabel = "مبلغ نهایی پرداختی:",
            actionButtonText = "مشاهده جزییات",
            actionType = InvoiceCardAction.VIEW_DETAILS,
            barterInvoice = sampleInvoice1
        )

        val sampleSaleCoin = BankCoinItem(
            title = "سکه تمام بهار طرح جدید امامی هولوگرام‌دار",
            coinType = CoinType.EMAMI,
            count = 1,
            hasHologram = true,
            unitPrice = 42300000L,
            totalPayable = 42300000.0,
            equivalent18kWeight = 8.13598 * 1.2
        )
        val sampleInvoice2 = BarterInvoice(
            invoiceNumber = "IR-1403-088",
            customer = Customer(
                name = "خانم سارا رادمنش",
                phone = "09129876543",
                note = "تسویه بیعانه کارتخوان"
            ),
            customerRole = CustomerRole.RETAIL,
            spotPrice18k = defaultSpot,
            salesItems = listOf(sampleSaleCoin),
            receivedItems = emptyList(),
            cashPosAmount = 33840000L,
            note = "پرداخت بیعانه ۳۳،۸۴۰،۰۰۰ ت. مانده قابل تسویه هنگام تحویل: ۸،۴۶۰،۰۰۰ ت."
        )

        val card2 = InvoiceListItem(
            id = "sample-2",
            invoiceNumber = "IR-1403-088",
            customerName = "خانم سارا رادمنش",
            customerInitials = "سر",
            isVerified = false,
            createdAtText = "کد فاکتور: IR-1403-088 • دیروز، ۱۷:۲۰",
            status = InvoiceStatus.PARTIALLY_PAID,
            statusDetail = "۲۰٪ مانده حساب",
            itemsSummary = "سکه تمام بهار طرح جدید امامی هولوگرام‌دار",
            itemsCountText = "اقلام (۱ قطعه بانکی):",
            line1Detail = "پرداخت بیعانه: ۳۳,۸۴۰,۰۰۰ ت",
            line2Detail = "مانده قابل تسویه: ۸,۴۶۰,۰۰۰ ت",
            finalAmount = 42300000L,
            amountLabel = "ارزش فاکتور:",
            actionButtonText = "تسویه حساب مانده",
            actionType = InvoiceCardAction.SETTLE_BALANCE,
            barterInvoice = sampleInvoice2
        )

        val sampleWorkshopCrafted = CraftedGoldItem(
            title = "سرویس گردنبند برلیان مارکیز سفارشی",
            karat = Karat.K18,
            grossWeight = 38.5,
            stoneWeight = 2.1,
            netWeight = 36.4,
            spotPrice = defaultSpot,
            wageType = WageType.PERCENTAGE,
            wageInput = 18.0,
            wageAmount = 36.4 * defaultSpot * 0.18,
            profitPercent = 7.0,
            profitAmount = (36.4 * defaultSpot * 1.18) * 0.07,
            taxPercent = 9.0,
            taxAmount = ((36.4 * defaultSpot * 0.18) + ((36.4 * defaultSpot * 1.18) * 0.07)) * 0.09,
            rawGoldValue = 36.4 * defaultSpot,
            totalPayable = (36.4 * defaultSpot) + (36.4 * defaultSpot * 0.18) + ((36.4 * defaultSpot * 1.18) * 0.07) + (((36.4 * defaultSpot * 0.18) + ((36.4 * defaultSpot * 1.18) * 0.07)) * 0.09),
            equivalent18kWeight = 36.4
        )
        val sampleInvoice3 = BarterInvoice(
            invoiceNumber = "IR-1403-087",
            customer = Customer(
                name = "جناب آقای دکتر افشار",
                phone = "09121112233",
                note = "سفارش ویژه کارگاه ساخت"
            ),
            customerRole = CustomerRole.RETAIL,
            spotPrice18k = defaultSpot,
            salesItems = listOf(sampleWorkshopCrafted),
            receivedItems = emptyList(),
            cashPosAmount = 50000000L,
            note = "مرحله مخراج‌کاری و آبکاری. زمان تحویل: ۳ روز آینده."
        )

        val card3 = InvoiceListItem(
            id = "sample-3",
            invoiceNumber = "IR-1403-087",
            customerName = "جناب آقای دکتر افشار",
            customerInitials = "دا",
            isVerified = false,
            createdAtText = "کد فاکتور: IR-1403-087 • تحویل: ۲۲ شهریور",
            status = InvoiceStatus.WORKSHOP,
            statusDetail = "در کارگاه ساخت",
            itemsSummary = "سرویس گردنبند برلیان مارکیز سفارشی",
            itemsCountText = "سفارش ساخت ویژه:",
            line1Detail = "مرحله: مخراج‌کاری و آبکاری",
            line2Detail = "زمان تحویل: ۳ روز آینده",
            finalAmount = 185000000L,
            amountLabel = "برآورد مظنه نهایی:",
            actionButtonText = "ویرایش و تکمیل فاکتور",
            actionType = InvoiceCardAction.EDIT_WORKSHOP,
            barterInvoice = sampleInvoice3
        )

        _uiState.update { current ->
            current.copy(
                invoice = sampleInvoice1,
                invoicesList = listOf(card1, card2, card3)
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

        val newCard = InvoiceListItem(
            id = currentInv.id,
            invoiceNumber = currentInv.invoiceNumber,
            customerName = customerName,
            customerInitials = initials,
            isVerified = true,
            createdAtText = "کد فاکتور: ${currentInv.invoiceNumber} • همین الان",
            status = if (currentInv.balance.isSettled) InvoiceStatus.SETTLED else InvoiceStatus.PARTIALLY_PAID,
            statusDetail = if (currentInv.balance.isSettled) "تسویه نقدی کامل" else "در انتظار پرداخت",
            itemsSummary = (currentInv.salesItems + currentInv.receivedItems).joinToString(" + ") { it.title }.ifBlank { "اقلام طلا و مسکوکات" },
            itemsCountText = "اقلام فاکتور (${com.goldex.companion.model.PersianNumberFormatter.toPersianDigits((currentInv.salesItems.size + currentInv.receivedItems.size).toString())} قلم):",
            line1Detail = "وزن کل: ${com.goldex.companion.model.PersianNumberFormatter.formatWeight(currentInv.salesItems.sumOf { it.equivalent18kWeight })} گرم",
            line2Detail = "روش تسویه: ${currentInv.settlementMethod.labelFa}",
            finalAmount = netAmount,
            amountLabel = "مبلغ نهایی پرداختی:",
            actionButtonText = "مشاهده جزییات",
            actionType = InvoiceCardAction.VIEW_DETAILS,
            barterInvoice = currentInv
        )

        _uiState.update { state ->
            val updatedList = listOf(newCard) + state.invoicesList.filterNot { it.id == newCard.id }
            state.copy(
                invoicesList = updatedList,
                subScreen = InvoicesSubScreen.LIST,
                isSuccessSnackbarVisible = true,
                statusMessage = "فاکتور ${currentInv.invoiceNumber} با موفقیت ثبت گردید"
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
            it.copy(
                invoice = BarterInvoice(
                    spotPrice18k = it.invoice.spotPrice18k
                )
            )
        }
    }
}
