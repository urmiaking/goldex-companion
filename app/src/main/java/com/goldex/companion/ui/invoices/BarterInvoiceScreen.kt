package com.goldex.companion.ui.invoices

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.runtime.rememberCoroutineScope
import com.goldex.companion.ui.components.AnimatedNumberText
import com.goldex.companion.ui.components.AnimatedPriceText
import com.goldex.companion.ui.theme.LuxuryMotion
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import com.goldex.companion.ui.theme.ButtonShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import com.goldex.companion.ui.theme.VazirmatnFamily
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.goldex.companion.data.MarketRates
import com.goldex.companion.model.BankCoinItem
import com.goldex.companion.model.BarterBalance
import com.goldex.companion.model.BarterInvoice
import com.goldex.companion.model.BarterItem
import com.goldex.companion.model.CraftedGoldItem
import com.goldex.companion.model.Customer
import com.goldex.companion.model.CustomerRole
import com.goldex.companion.model.InvoiceItemCategory
import com.goldex.companion.model.InvoiceListItem
import com.goldex.companion.model.InvoiceStatus
import com.goldex.companion.model.MeltGoldItem
import com.goldex.companion.model.PersianNumberFormatter
import com.goldex.companion.model.ScrapGoldItem
import com.goldex.companion.model.SettlementMethod
import com.goldex.companion.model.SettlementPaymentItem
import com.goldex.companion.ui.components.CustomerIconVector
import com.goldex.companion.ui.hub.HubArrowRight
import com.goldex.companion.ui.components.GoldButton
import com.goldex.companion.ui.components.GoldInputField
import com.goldex.companion.ui.components.LuxurySegmentedControl
import com.goldex.companion.ui.invoices.components.InvoiceCheckVector
import com.goldex.companion.ui.invoices.components.InvoiceCloseVector
import com.goldex.companion.ui.invoices.components.InvoiceEditVector
import com.goldex.companion.ui.invoices.components.InvoiceOfficialHeader
import com.goldex.companion.ui.invoices.components.InvoicePdfVector
import com.goldex.companion.ui.invoices.components.InvoicePlusVector
import com.goldex.companion.ui.invoices.components.InvoiceSmsVector
import com.goldex.companion.ui.invoices.components.InvoiceTrashVector
import com.goldex.companion.ui.invoices.modals.AddInvoiceItemModal
import com.goldex.companion.ui.invoices.modals.AddInvoicePaymentModal
import com.goldex.companion.ui.theme.LocalGoldExColors
import com.goldex.companion.ui.theme.VazirmatnFamily
import com.goldex.companion.ui.theme.goldGradient
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BarterInvoiceScreen(
    uiState: BarterInvoiceUiState,
    marketRates: MarketRates,
    customerList: List<Customer> = emptyList(),
    onSetCustomerRole: (CustomerRole) -> Unit = {},
    onSetDebtBasis: (com.goldex.companion.model.InvoiceDebtBasis) -> Unit = {},
    onOpenDatedSettlement: () -> Unit = {},
    onSetSettlementMethod: (SettlementMethod) -> Unit,
    onSetCashPosAmount: (Long) -> Unit,
    onSetLedgerAmount: (Long) -> Unit = {},
    onSetPosTrackingCode: (String) -> Unit = {},
    onSetLedgerDueDate: (String) -> Unit = {},
    onSetBullionSettlement: (Double, Int, String) -> Unit = { _, _, _ -> },
    onSetThirdPartyTransfer: (Customer?, String, String, Double, Long, String) -> Unit = { _, _, _, _, _, _ -> },
    onSetSettlementPayments: (List<SettlementPaymentItem>) -> Unit = {},
    onSetSyncWithLedger: (Boolean) -> Unit = {},
    onSetNote: (String) -> Unit,
    onOpenAddItemModal: (InvoiceItemCategory, Boolean) -> Unit,
    onOpenEditItemModal: (BarterItem, Boolean) -> Unit,
    onCloseItemModal: () -> Unit,
    onSaveItem: (BarterItem) -> Unit,
    onDeleteSalesItem: (String) -> Unit,
    onDeleteReceivedItem: (String) -> Unit,
    onSetRateEditDialogVisible: (Boolean) -> Unit,
    onUpdateSpotPrice: (Long) -> Unit,
    onOpenCustomerPicker: () -> Unit,
    onOpenInvoiceManager: () -> Unit,
    onPreviewPdf: () -> Unit,
    onSendSms: () -> Unit,
    onFinalSubmit: () -> Unit,
    onNavigateBack: (() -> Unit)? = null,
    defaultProfitPercent: String = "7",
    defaultTaxPercent: String = "9",
    modifier: Modifier = Modifier
) {
    val colors = LocalGoldExColors.current
    val invoice = uiState.invoice
    val balance = uiState.balance

    val dateSolar = remember(invoice.createdAt) {
        SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()).format(Date(invoice.createdAt))
    }

    var isPaymentModalVisible by remember { mutableStateOf(false) }
    var editingPaymentItem by remember { mutableStateOf<SettlementPaymentItem?>(null) }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Scaffold(
            modifier = modifier
                .fillMaxSize()
                .background(colors.background),
            containerColor = colors.background,
            topBar = {
                Surface(
                    color = colors.surface,
                    border = BorderStroke(0.6.dp, colors.goldBorder.copy(alpha = 0.5f)),
                    shadowElevation = 3.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            if (onNavigateBack != null) {
                                IconButton(
                                    onClick = onNavigateBack,
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(ButtonShape)
                                        .background(colors.surfaceElevated)
                                        .border(0.6.dp, colors.goldBorder, ButtonShape)
                                ) {
                                    Icon(
                                        imageVector = HubArrowRight,
                                        contentDescription = "بازگشت",
                                        tint = colors.goldPrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            Column {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(7.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFDFB35A))
                                    )
                                    Text(
                                        text = if (uiState.isEditingExistingInvoice) "ویرایش فاکتور" else "ثبت فاکتور جدید",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = colors.textMain,
                                        fontFamily = VazirmatnFamily
                                    )
                                }
                                Text(
                                    text = if (uiState.isEditingExistingInvoice) "ویرایش اقلام و تسویه فاکتور" else "فاکتور زرگری و تهاتر طلا",
                                    fontSize = 10.5.sp,
                                    color = colors.textMuted,
                                    fontFamily = VazirmatnFamily
                                )
                            }
                        }

                        // Left side: Invoice code badge
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(Color(0x26DFB35A))
                                .border(0.8.dp, Color(0x4DDFB35A), RoundedCornerShape(50))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "کد: ${PersianNumberFormatter.toPersianDigits(invoice.cleanInvoiceNumber)}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.goldPrimary,
                                fontFamily = VazirmatnFamily
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            val scrollState = rememberScrollState()
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(scrollState)
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 1. Meta & Live Gold Rate Bar
                InvoiceMetaAndRateCard(
                    invoiceNumber = invoice.cleanInvoiceNumber,
                    dateSolar = dateSolar,
                    spotPrice18k = invoice.spotPrice18k,
                    onEditRateClick = { onSetRateEditDialogVisible(true) }
                )

                // 2. Customer & Counterparty Card (Simplified)
                CustomerAndAccountCard(
                    customer = invoice.customer,
                    customerRole = invoice.customerRole,
                    onSetCustomerRole = onSetCustomerRole,
                    onChangeCustomerClick = onOpenCustomerPicker
                )

                Surface(shape = RoundedCornerShape(16.dp), color = colors.surface,
                    border = BorderStroke(0.6.dp, colors.goldBorder.copy(alpha = 0.5f))) {
                    Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("مبنای مانده فاکتور", fontFamily = VazirmatnFamily, color = colors.textMain, fontWeight = FontWeight.Bold)
                        LuxurySegmentedControl(
                            items = com.goldex.companion.model.InvoiceDebtBasis.entries.toList(),
                            selectedItem = invoice.debtBasis ?: if (invoice.isGoldDebt) com.goldex.companion.model.InvoiceDebtBasis.GOLD else com.goldex.companion.model.InvoiceDebtBasis.CASH,
                            onItemSelected = onSetDebtBasis, label = { it.titleFa },
                            isItemEnabled = { !uiState.isEditingExistingInvoice }, modifier = Modifier.fillMaxWidth()
                        )
                        Text(if (uiState.isEditingExistingInvoice) "مبنای قرارداد ثبت‌شده ثابت است؛ پرداخت بعدی با نرخ همان تسویه ثبت می‌شود."
                            else if (invoice.isGoldDebt) "صافی فاکتور با نرخ صدور به گرم ۱۸ عیار تبدیل می‌شود؛ مانده گرمی ثابت و تسویه نقدی به نرخ روز است."
                            else "مانده تومان ثابت است؛ نوسان قیمت طلا مبلغ بدهی را تغییر نمی‌دهد.",
                            fontFamily = VazirmatnFamily, fontSize = 11.sp, color = colors.textSecondary)
                    }
                }

                // 3. Sales Items Section (اقلام فروش ما)
                ItemsSectionCard(
                    sectionNumber = 1,
                    title = "اقلام فروش ما (تحویلی)",
                    items = invoice.salesItems,
                    onAddItemClick = { onOpenAddItemModal(InvoiceItemCategory.CRAFTED, true) },
                    onEditItemClick = { onOpenEditItemModal(it, true) },
                    onDeleteItemClick = onDeleteSalesItem,
                    isSales = true
                )

                // 4. Received Items Section (اقلام دریافتی تهاتر)
                ItemsSectionCard(
                    sectionNumber = 2,
                    title = "اقلام دریافتی / تهاتر (از مشتری)",
                    items = invoice.receivedItems,
                    onAddItemClick = { onOpenAddItemModal(InvoiceItemCategory.SCRAP, false) },
                    onEditItemClick = { onOpenEditItemModal(it, false) },
                    onDeleteItemClick = onDeleteReceivedItem,
                    isSales = false
                )

                // 5. Barter Balance & Net Settlement Overview
                BarterBalanceCard(
                    balance = if (invoice.debtBasis == com.goldex.companion.model.InvoiceDebtBasis.GOLD && invoice.spotPrice18k > 0) invoice.balance.copy(net18kWeightDelta = invoice.debtPrincipalGold) else invoice.balance,
                    customerRole = if (invoice.isGoldDebt) CustomerRole.WHOLESALER else CustomerRole.RETAIL
                )

                // 6. Settlement & Payment Methods Section
                SettlementSection(
                    invoice = invoice,
                    balance = invoice.balance,
                    recordedOutstanding = uiState.recordedOutstanding.takeIf { uiState.isEditingExistingInvoice },
                    onOpenAddPaymentModal = {
                        if (uiState.isEditingExistingInvoice && invoice.syncWithLedger && invoice.customer != null) onOpenDatedSettlement()
                        else { editingPaymentItem = null; isPaymentModalVisible = true }
                    },
                    onEditPaymentItem = { item ->
                        editingPaymentItem = item
                        isPaymentModalVisible = true
                    },
                    onSetSettlementPayments = onSetSettlementPayments,
                    onSetNote = onSetNote,
                    onSetSyncWithLedger = onSetSyncWithLedger
                )

                // 7. Final Action Buttons
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    GoldButton(
                        text = "ثبت نهایی و صدور فاکتور رسمی تهاتر",
                        onClick = onFinalSubmit,
                        enabled = !uiState.isLoading && !uiState.isSaving,
                        isLoading = uiState.isSaving,
                        icon = InvoiceCheckVector,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        GoldButton(
                            text = "پیش‌نمایش PDF",
                            onClick = onPreviewPdf,
                            icon = InvoicePdfVector,
                            isSecondary = true,
                            modifier = Modifier.weight(1f)
                        )
                        GoldButton(
                            text = "ارسال پیامک فاکتور",
                            onClick = onSendSms,
                            icon = InvoiceSmsVector,
                            isSecondary = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    GoldButton(
                        text = "مشاهده بایگانی فاکتورها",
                        onClick = onOpenInvoiceManager,
                        isSecondary = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            // Add / Edit Payment Modal
            if (isPaymentModalVisible) {
                AddInvoicePaymentModal(
                    invoice = invoice,
                    balance = balance,
                    customerList = customerList,
                    invoicesList = uiState.invoicesList,
                    existingPayment = editingPaymentItem,
                    onDismiss = {
                        isPaymentModalVisible = false
                        editingPaymentItem = null
                    },
                    onConfirm = { payment ->
                        if (editingPaymentItem != null) {
                            onSetSettlementPayments(invoice.payments.map { if (it.id == payment.id) payment else it })
                        } else {
                            onSetSettlementPayments(invoice.payments + payment)
                        }
                        isPaymentModalVisible = false
                        editingPaymentItem = null
                    }
                )
            }

            // Add / Edit Item Modal
            if (uiState.isItemModalVisible) {
                AddInvoiceItemModal(
                    spotPrice18k = invoice.spotPrice18k,
                    marketRates = marketRates,
                    defaultCategory = uiState.targetCategory,
                    existingItem = uiState.editingItem,
                    defaultProfitPercent = defaultProfitPercent,
                    defaultTaxPercent = defaultTaxPercent,
                    onDismiss = onCloseItemModal,
                    onSaveItem = onSaveItem
                )
            }

            // Manual Rate Edit Dialog
            if (uiState.isRateEditDialogVisible) {
                EditRateDialog(
                    currentRate = invoice.spotPrice18k,
                    liveRate = marketRates.gold18,
                    onDismiss = { onSetRateEditDialogVisible(false) },
                    onConfirm = onUpdateSpotPrice
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// 1. Meta & Rate Card
// ---------------------------------------------------------------------------
@Composable
private fun InvoiceMetaAndRateCard(
    invoiceNumber: String,
    dateSolar: String,
    spotPrice18k: Long,
    onEditRateClick: () -> Unit
) {
    val colors = LocalGoldExColors.current

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = colors.surface,
        border = BorderStroke(0.8.dp, colors.border),
        shadowElevation = if (colors.isDark) 0.dp else 1.5.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = colors.goldContainer.copy(alpha = 0.5f),
                        border = BorderStroke(0.6.dp, colors.goldBorder)
                    ) {
                        Text(
                            text = "فاکتور دوطرفه تهاتر",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.goldPrimary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.5.dp),
                            fontFamily = VazirmatnFamily
                        )
                    }
                    Text(
                        text = PersianNumberFormatter.toPersianDigits(invoiceNumber),
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textMain,
                        fontFamily = VazirmatnFamily
                    )
                }

                Text(
                    text = PersianNumberFormatter.toPersianDigits(dateSolar),
                    fontSize = 11.sp,
                    color = colors.textMuted,
                    fontFamily = VazirmatnFamily
                )
            }

            // Dark Luxury Live Rate Bar
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFF16161A),
                border = BorderStroke(1.dp, Color(0x33D4AF37)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0x33D4AF37)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "⚡", fontSize = 11.sp)
                        }
                        Text(
                            text = "مظنه ۱۸ عیار:",
                            fontSize = 11.5.sp,
                            color = Color(0xCCFFFFFF),
                            fontFamily = VazirmatnFamily
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AnimatedPriceText(
                                amount = spotPrice18k,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFE6CA65)
                            )
                            Text(
                                text = " تومان",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFE6CA65),
                                fontFamily = VazirmatnFamily
                            )
                        }
                    }

                    Surface(
                        shape = ButtonShape,
                        color = Color(0x22FFFFFF),
                        modifier = Modifier.clickable { onEditRateClick() }
                    ) {
                        Text(
                            text = "تغییر نرخ",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            fontFamily = VazirmatnFamily
                        )
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// 2. Customer & Account Card
// ---------------------------------------------------------------------------
@Composable
private fun CustomerAndAccountCard(
    customer: Customer?,
    customerRole: CustomerRole,
    onSetCustomerRole: (CustomerRole) -> Unit,
    onChangeCustomerClick: () -> Unit
) {
    val colors = LocalGoldExColors.current

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = colors.surface,
        border = BorderStroke(0.8.dp, colors.border),
        shadowElevation = if (colors.isDark) 0.dp else 1.5.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(5.dp, 14.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(colors.goldPrimary)
                    )
                    Text(
                        text = "مشخصات طرف حساب و نوع فاکتور",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textMain,
                        fontFamily = VazirmatnFamily
                    )
                }

                Text(
                    text = if (customer != null) "تغییر طرف حساب" else "انتخاب از دفتر",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.goldPrimary,
                    modifier = Modifier
                        .clickable(onClick = onChangeCustomerClick)
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    fontFamily = VazirmatnFamily
                )
            }

            // Customer Role Selector (Retail vs Wholesaler)
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = colors.surfaceElevated,
                border = BorderStroke(0.6.dp, colors.border),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val isRetail = customerRole == CustomerRole.RETAIL
                    // Retail Option
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(9.dp))
                            .background(if (isRetail) colors.goldPrimary else Color.Transparent)
                            .clickable { onSetCustomerRole(CustomerRole.RETAIL) }
                            .padding(vertical = 7.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "💳 فروش ریالی (مشتری)",
                            fontSize = 11.sp,
                            fontWeight = if (isRetail) FontWeight.Bold else FontWeight.Medium,
                            color = if (isRetail) Color.White else colors.textSecondary,
                            fontFamily = VazirmatnFamily
                        )
                    }

                    // Wholesaler Option
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(9.dp))
                            .background(if (!isRetail) colors.goldPrimary else Color.Transparent)
                            .clickable { onSetCustomerRole(CustomerRole.WHOLESALER) }
                            .padding(vertical = 7.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "⚖️ تهاتر وزنی (همکار)",
                            fontSize = 11.sp,
                            fontWeight = if (!isRetail) FontWeight.Bold else FontWeight.Medium,
                            color = if (!isRetail) Color.White else colors.textSecondary,
                            fontFamily = VazirmatnFamily
                        )
                    }
                }
            }

            // Customer Details Interactive Card
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = if (customer != null) colors.goldContainer.copy(alpha = 0.2f) else colors.surfaceElevated,
                border = BorderStroke(0.8.dp, if (customer != null) colors.goldBorder else colors.border),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onChangeCustomerClick)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (customer != null) colors.goldContainer.copy(alpha = 0.6f) else colors.surface)
                                .border(1.dp, if (customer != null) colors.goldBorder else colors.border, RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = customer?.name?.take(2) ?: "👤",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                color = if (customer != null) colors.goldPrimary else colors.textMuted,
                                fontFamily = VazirmatnFamily
                            )
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = customer?.name ?: "انتخاب طرف حساب (مشتری یا همکار)",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (customer != null) colors.textMain else colors.textMuted,
                                fontFamily = VazirmatnFamily
                            )
                            Text(
                                text = if (customer != null) {
                                    customer.phone.ifBlank { customer.note.ifBlank { "بدون یادداشت حساب" } }
                                } else {
                                    "برای انتخاب یا افزودن طرف معامله لمس کنید"
                                },
                                fontSize = 10.5.sp,
                                color = colors.textSecondary,
                                fontFamily = VazirmatnFamily
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (customer != null) colors.goldContainer.copy(alpha = 0.5f) else colors.surface)
                            .border(0.6.dp, if (customer != null) colors.goldBorder else colors.border, RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (customer != null) "تغییر" else "+ انتخاب",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.goldPrimary,
                            fontFamily = VazirmatnFamily
                        )
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// 3. Barter Balance & Net Settlement Overview
// ---------------------------------------------------------------------------
@Composable
private fun BarterBalanceCard(
    balance: com.goldex.companion.model.BarterBalance,
    customerRole: CustomerRole = CustomerRole.WHOLESALER
) {
    val colors = LocalGoldExColors.current

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = colors.surface,
        border = BorderStroke(0.8.dp, colors.border),
        shadowElevation = if (colors.isDark) 0.dp else 1.5.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(5.dp, 14.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(colors.goldPrimary)
                    )
                    Text(
                        text = "تراز مالی و تهاتر دوطرفه",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textMain,
                        fontFamily = VazirmatnFamily
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = colors.profitGreen.copy(alpha = 0.12f),
                    border = BorderStroke(0.6.dp, colors.profitGreen.copy(alpha = 0.4f))
                ) {
                    Text(
                        text = if (customerRole == CustomerRole.WHOLESALER) "مبنای گرمی" else "مبنای تومانی",
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.profitGreen,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        fontFamily = VazirmatnFamily
                    )
                }
            }

            // Two Columns: Sales vs Received
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Column 1: Sales
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = colors.goldContainer.copy(alpha = 0.25f),
                    border = BorderStroke(0.8.dp, colors.goldBorder),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(colors.goldPrimary)
                            )
                            Text(
                                text = "فروش ما (بستانکاری):",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.goldPrimary,
                                fontFamily = VazirmatnFamily
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AnimatedPriceText(
                                amount = balance.totalSalesAmount,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Black,
                                color = colors.textMain
                            )
                            Text(
                                text = " ت",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textSecondary,
                                fontFamily = VazirmatnFamily
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "معادل: ",
                                fontSize = 9.5.sp,
                                color = colors.textSecondary,
                                fontFamily = VazirmatnFamily
                            )
                            AnimatedNumberText(
                                text = PersianNumberFormatter.formatWeight(balance.totalSales18kWeight),
                                fontSize = 9.5.sp,
                                color = colors.textSecondary
                            )
                            Text(
                                text = " گرم",
                                fontSize = 9.5.sp,
                                color = colors.textSecondary,
                                fontFamily = VazirmatnFamily
                            )
                        }
                    }
                }

                // Column 2: Received
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = colors.surfaceElevated,
                    border = BorderStroke(0.8.dp, colors.border),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(colors.textSecondary)
                            )
                            Text(
                                text = "دریافتی ما (بدهکاری):",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textSecondary,
                                fontFamily = VazirmatnFamily
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AnimatedPriceText(
                                amount = balance.totalReceivedAmount,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Black,
                                color = colors.textMain
                            )
                            Text(
                                text = " ت",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textSecondary,
                                fontFamily = VazirmatnFamily
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "معادل: ",
                                fontSize = 9.5.sp,
                                color = colors.textSecondary,
                                fontFamily = VazirmatnFamily
                            )
                            AnimatedNumberText(
                                text = PersianNumberFormatter.formatWeight(balance.totalReceived18kWeight),
                                fontSize = 9.5.sp,
                                color = colors.textSecondary
                            )
                            Text(
                                text = " گرم",
                                fontSize = 9.5.sp,
                                color = colors.textSecondary,
                                fontFamily = VazirmatnFamily
                            )
                        }
                    }
                }
            }

            // Net Balance Bar (صافی مانده)
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = colors.goldContainer.copy(alpha = 0.5f),
                border = BorderStroke(1.dp, colors.goldPrimary.copy(alpha = 0.6f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "صافی مانده:",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                color = colors.textMain,
                                fontFamily = VazirmatnFamily
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (balance.isCustomerDebtor) colors.errorRed.copy(alpha = 0.12f) else colors.profitGreen.copy(alpha = 0.12f)
                            ) {
                                Text(
                                    text = if (balance.isSettled) "تسویه کامل" else if (balance.isCustomerDebtor) "مشتری بدهکار" else "مشتری بستانکار",
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (balance.isCustomerDebtor) colors.errorRed else colors.profitGreen,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.5.dp),
                                    fontFamily = VazirmatnFamily
                                )
                            }
                        }
                        Text(
                            text = "پس از کسر تهاتر طلای مستعمل و آبشده",
                            fontSize = 9.sp,
                            color = colors.textSecondary,
                            fontFamily = VazirmatnFamily
                        )
                    }

                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(1.dp)
                    ) {
                        if (customerRole == CustomerRole.WHOLESALER) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                AnimatedNumberText(
                                    text = PersianNumberFormatter.formatWeight(kotlin.math.abs(balance.net18kWeightDelta)),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Black,
                                    color = colors.goldPrimary
                                )
                                Text(
                                    text = " گرم ۱۸ عیار",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.goldPrimary,
                                    fontFamily = VazirmatnFamily
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "معادل: ",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.textSecondary,
                                    fontFamily = VazirmatnFamily
                                )
                                AnimatedPriceText(
                                    amount = kotlin.math.abs(balance.netPayableAmount),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.textSecondary
                                )
                                Text(
                                    text = " تومان",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.textSecondary,
                                    fontFamily = VazirmatnFamily
                                )
                            }
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                AnimatedPriceText(
                                    amount = kotlin.math.abs(balance.netPayableAmount),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Black,
                                    color = colors.goldPrimary
                                )
                                Text(
                                    text = " تومان",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.goldPrimary,
                                    fontFamily = VazirmatnFamily
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "معادل: ",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.textSecondary,
                                    fontFamily = VazirmatnFamily
                                )
                                AnimatedNumberText(
                                    text = PersianNumberFormatter.formatWeight(kotlin.math.abs(balance.net18kWeightDelta)),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.textSecondary
                                )
                                Text(
                                    text = " گرم ۱۸ عیار",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.textSecondary,
                                    fontFamily = VazirmatnFamily
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// 4 & 5. Items Section Card
// ---------------------------------------------------------------------------
@Composable
private fun ItemsSectionCard(
    sectionNumber: Int,
    title: String,
    items: List<BarterItem>,
    onAddItemClick: () -> Unit,
    onEditItemClick: (BarterItem) -> Unit,
    onDeleteItemClick: (String) -> Unit,
    isSales: Boolean
) {
    val colors = LocalGoldExColors.current
    val coroutineScope = rememberCoroutineScope()
    var deletingItemIds by remember { mutableStateOf(setOf<String>()) }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = colors.surface,
        border = BorderStroke(0.8.dp, colors.border),
        shadowElevation = if (colors.isDark) 0.dp else 1.5.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isSales) colors.goldContainer else colors.surfaceElevated),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = PersianNumberFormatter.toPersianDigits(sectionNumber.toString()),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = if (isSales) colors.goldPrimary else colors.textSecondary
                        )
                    }
                    Text(
                        text = title,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textMain,
                        fontFamily = VazirmatnFamily
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(1.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "(",
                            fontSize = 10.sp,
                            color = colors.textMuted,
                            fontFamily = VazirmatnFamily
                        )
                        AnimatedNumberText(
                            text = PersianNumberFormatter.toPersianDigits(items.size.toString()),
                            fontSize = 10.sp,
                            color = colors.textMuted
                        )
                        Text(
                            text = " قلم)",
                            fontSize = 10.sp,
                            color = colors.textMuted,
                            fontFamily = VazirmatnFamily
                        )
                    }
                }

                Surface(
                    shape = ButtonShape,
                    color = colors.goldContainer.copy(alpha = 0.5f),
                    border = BorderStroke(0.6.dp, colors.goldBorder),
                    modifier = Modifier.clickable { onAddItemClick() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = InvoicePlusVector,
                            contentDescription = null,
                            tint = colors.goldPrimary,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = if (isSales) "افزودن قلم فروش" else "افزودن قلم تهاتر",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.goldPrimary,
                            fontFamily = VazirmatnFamily
                        )
                    }
                }
            }

            // Items List
            if (items.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "هیچ قلمی ثبت نشده است. روی دکمه افزودن کلیک کنید.",
                        fontSize = 11.sp,
                        color = colors.textMuted,
                        fontFamily = VazirmatnFamily
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items.forEach { item ->
                        AnimatedVisibility(
                            visible = item.id !in deletingItemIds,
                            enter = LuxuryMotion.ItemAddEnter,
                            exit = LuxuryMotion.ItemRemoveExit
                        ) {
                            ItemRowCard(
                                item = item,
                                onEdit = { onEditItemClick(item) },
                                onDelete = {
                                    deletingItemIds = deletingItemIds + item.id
                                    coroutineScope.launch {
                                        delay(220)
                                        onDeleteItemClick(item.id)
                                        deletingItemIds = deletingItemIds - item.id
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ItemRowCard(
    item: BarterItem,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val colors = LocalGoldExColors.current

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = colors.surfaceElevated,
        border = BorderStroke(0.8.dp, colors.border),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier
                            .padding(top = 4.dp)
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(colors.goldPrimary)
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                        Text(
                            text = item.title,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textMain,
                            fontFamily = VazirmatnFamily
                        )
                        val subtitle = when (item) {
                            is CraftedGoldItem -> "عیار ${item.karat.labelFa} • ناخالص: ${PersianNumberFormatter.formatWeight(item.grossWeight)} گرم • خالص: ${PersianNumberFormatter.formatWeight(item.netWeight)} گرم"
                            is ScrapGoldItem -> "عیار مبنا: ${item.baseKarat} ← خالص: ${item.payableKarat} • وزن: ${PersianNumberFormatter.formatWeight(item.netWeight)} گرم"
                            is MeltGoldItem -> "عیار خطی: ${item.labKarat} • انگ: ${item.angNumber.ifBlank { "-" }} • وزن: ${PersianNumberFormatter.formatWeight(item.weight)} گرم"
                            is BankCoinItem -> "${PersianNumberFormatter.toPersianDigits(item.count.toString())} عدد • عیار ۹۰۰ • ${if (item.hasHologram) "هولوگرام‌دار" else "ساده"}"
                        }
                        Text(
                            text = subtitle,
                            fontSize = 9.5.sp,
                            color = colors.textSecondary,
                            fontFamily = VazirmatnFamily
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(26.dp)
                    ) {
                        Icon(
                            imageVector = InvoiceEditVector,
                            contentDescription = "ویرایش",
                            tint = colors.textSecondary,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(26.dp)
                    ) {
                        Icon(
                            imageVector = InvoiceTrashVector,
                            contentDescription = "حذف",
                            tint = colors.errorRed,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(0.6.dp)
                    .background(colors.border.copy(alpha = 0.5f))
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "معادل ۱۸ عیار: ",
                        fontSize = 10.sp,
                        color = colors.textMuted,
                        fontFamily = VazirmatnFamily
                    )
                    AnimatedNumberText(
                        text = PersianNumberFormatter.formatWeight(item.equivalent18kWeight),
                        fontSize = 10.sp,
                        color = colors.textMuted
                    )
                    Text(
                        text = " گرم",
                        fontSize = 10.sp,
                        color = colors.textMuted,
                        fontFamily = VazirmatnFamily
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AnimatedPriceText(
                        amount = item.totalPayable,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textMain
                    )
                    Text(
                        text = " تومان",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textMain,
                        fontFamily = VazirmatnFamily
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// 6. Settlement & Payment Section
// ---------------------------------------------------------------------------
@Composable
private fun SettlementSection(
    invoice: BarterInvoice,
    balance: BarterBalance,
    recordedOutstanding: com.goldex.companion.domain.customers.OutstandingBalance?,
    onOpenAddPaymentModal: () -> Unit,
    onEditPaymentItem: (SettlementPaymentItem) -> Unit,
    onSetSettlementPayments: (List<SettlementPaymentItem>) -> Unit,
    onSetNote: (String) -> Unit,
    onSetSyncWithLedger: (Boolean) -> Unit
) {
    val colors = LocalGoldExColors.current
    var pendingDeletePayment by remember { mutableStateOf<SettlementPaymentItem?>(null) }
    val netPayableAmount = balance.netPayableAmount
    val absNetPayableLong = kotlin.math.abs(netPayableAmount).toLong()
    val totalPaid = invoice.totalPaymentsAmount
    val remainingBalance = recordedOutstanding?.cashTomans?.let { kotlin.math.abs(it) } ?: if (invoice.spotPrice18k > 0 || !invoice.isGoldDebt) invoice.remainingBalanceTomans else 0L
    val remainingGold = recordedOutstanding?.goldGrams?.let { kotlin.math.abs(it) } ?: if (invoice.spotPrice18k > 0) invoice.remainingBalanceGold18k else 0.0
    val fullySettled = recordedOutstanding?.isSettled ?: (invoice.spotPrice18k > 0 && invoice.isFullySettled)

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = colors.surface,
        border = BorderStroke(0.8.dp, colors.goldBorder.copy(alpha = 0.5f)),
        shadowElevation = if (colors.isDark) 0.dp else 1.5.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(5.dp, 16.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(colors.goldPrimary)
                    )
                    Text(
                        text = "روش‌های تسویه و پرداخت فاکتور",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textMain,
                        fontFamily = VazirmatnFamily
                    )
                }

                Surface(
                    shape = ButtonShape,
                    color = colors.goldContainer.copy(alpha = 0.5f),
                    border = BorderStroke(0.6.dp, colors.goldBorder),
                    modifier = Modifier.clickable(onClick = onOpenAddPaymentModal)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = InvoicePlusVector,
                            contentDescription = null,
                            tint = colors.goldPrimary,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = "ثبت پرداخت",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.goldPrimary,
                            fontFamily = VazirmatnFamily
                        )
                    }
                }
            }

            // Financial Balance Overview Card
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = colors.surfaceElevated,
                border = BorderStroke(
                    0.8.dp,
                    if (fullySettled) colors.profitGreen.copy(alpha = 0.5f)
                    else colors.goldBorder.copy(alpha = 0.4f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val isWholesaler = invoice.isGoldDebt
                    val totalPaidWeight = invoice.totalPaymentsGold18k

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "صافی کل فاکتور:",
                            fontSize = 11.5.sp,
                            color = colors.textSecondary,
                            fontFamily = VazirmatnFamily
                        )
                        Text(
                            text = if (isWholesaler) {
                                "${PersianNumberFormatter.formatWeight(kotlin.math.abs(if (invoice.spotPrice18k > 0) invoice.debtPrincipalGold else 0.0))} گرم ۱۸ عیار"
                            } else {
                                "${PersianNumberFormatter.formatTomans(absNetPayableLong)} تومان"
                            },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textMain,
                            fontFamily = VazirmatnFamily
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "مجموع مراحل پرداخت‌شده:",
                            fontSize = 11.5.sp,
                            color = colors.textSecondary,
                            fontFamily = VazirmatnFamily
                        )
                        Text(
                            text = if (isWholesaler) {
                                "${PersianNumberFormatter.formatWeight(totalPaidWeight)} گرم ۱۸ عیار"
                            } else {
                                "${PersianNumberFormatter.formatTomans(totalPaid)} تومان"
                            },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if ((if (isWholesaler) totalPaidWeight > 0.0 else totalPaid > 0L)) colors.profitGreen else colors.textMuted,
                            fontFamily = VazirmatnFamily
                        )
                    }

                    HorizontalDivider(color = colors.border.copy(alpha = 0.4f), thickness = 0.6.dp)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (fullySettled) "وضعیت تسویه:" else "مانده پرداخت‌نشده (نسیه/دفتری):",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textMain,
                            fontFamily = VazirmatnFamily
                        )
                        if (fullySettled) {
                            Text(
                                text = "تسویه کامل شد ✓",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.profitGreen,
                                fontFamily = VazirmatnFamily
                            )
                        } else {
                            Text(
                                text = if (isWholesaler) {
                                    "${PersianNumberFormatter.formatAccountWeight(remainingGold)} گرم ۱۸ عیار"
                                } else {
                                    "${PersianNumberFormatter.formatTomans(remainingBalance)} تومان"
                                },
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Black,
                                color = colors.goldPrimary,
                                fontFamily = VazirmatnFamily
                            )
                        }
                    }
                }
            }

            // Recorded Payments List
            if (invoice.payments.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "مراحل پرداخت ثبت‌شده (${PersianNumberFormatter.toPersianDigits(invoice.payments.size.toString())}):",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textMain,
                        fontFamily = VazirmatnFamily
                    )
                    invoice.payments.forEach { pItem ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = colors.surfaceElevated,
                            border = BorderStroke(0.6.dp, colors.goldBorder.copy(alpha = 0.5f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = pItem.settlement == null) { onEditPaymentItem(pItem) }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    val icon = when (pItem.method) {
                                        SettlementMethod.POS -> "💳"
                                        SettlementMethod.LEDGER -> "📒"
                                        SettlementMethod.BULLION -> "🧱"
                                        SettlementMethod.TRANSFER -> "🔄"
                                    }
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(colors.goldContainer.copy(alpha = 0.5f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(text = icon, fontSize = 14.sp)
                                    }
                                    Column {
                                        Text(
                                            text = pItem.method.labelFa,
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = colors.textMain,
                                            fontFamily = VazirmatnFamily
                                        )
                                        val pDetail = buildString {
                                            pItem.settlement?.let { recorded ->
                                                append("${PersianNumberFormatter.toPersianDigits(pItem.date)} • نرخ ثبت: ${PersianNumberFormatter.formatTomans(recorded.rateTomans)} تومان • ")
                                            }
                                            if (pItem.amountTomans > 0) append("${PersianNumberFormatter.formatTomans(pItem.amountTomans)} تومان")
                                            if (pItem.goldWeight18k > 0) {
                                                if (isNotEmpty()) append(" • ")
                                                append("${PersianNumberFormatter.formatWeight(pItem.goldWeight18k)} گرم")
                                            }
                                            if (pItem.trackingCode.isNotBlank()) {
                                                if (isNotEmpty()) append(" • پیگیری: ${PersianNumberFormatter.toPersianDigits(pItem.trackingCode)}")
                                            }
                                            if (pItem.thirdPartyCustomerName.isNotBlank()) {
                                                if (isNotEmpty()) append(" • به: ${pItem.thirdPartyCustomerName}")
                                            }
                                            if (pItem.description.isNotBlank()) {
                                                if (isNotEmpty()) append(" • ${pItem.description}")
                                            }
                                        }
                                        Text(
                                            text = pDetail,
                                            fontSize = 9.5.sp,
                                            color = colors.textSecondary,
                                            fontFamily = VazirmatnFamily
                                        )
                                    }
                                }
                                IconButton(
                                    enabled = true,
                                    onClick = {
                                        if (pItem.settlement != null) {
                                            pendingDeletePayment = pItem
                                        } else {
                                            onSetSettlementPayments(invoice.payments.filterNot { it.id == pItem.id })
                                        }
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = InvoiceTrashVector,
                                        contentDescription = "حذف پرداخت",
                                        tint = Color(0xFFEF5350),
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = colors.surfaceElevated.copy(alpha = 0.5f),
                    border = BorderStroke(0.6.dp, colors.border.copy(alpha = 0.6f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "📒", fontSize = 14.sp)
                        Text(
                            text = "هنوز پرداختی ثبت نشده است. با زدن گزینه «ثبت پرداخت» می‌توانید مراحل پرداخت نقد، کارتخوان، شمش یا حواله را ثبت کنید.",
                            fontSize = 10.sp,
                            color = colors.textMuted,
                            fontFamily = VazirmatnFamily
                        )
                    }
                }
            }

            HorizontalDivider(color = colors.border.copy(alpha = 0.5f), thickness = 0.6.dp)

            // Notes field directly on screen
            GoldInputField(
                value = invoice.note,
                onValueChange = onSetNote,
                label = "توضیحات و شرایط تحویل",
                trailingText = "اختیاری",
                keyboardType = KeyboardType.Text,
                modifier = Modifier.fillMaxWidth()
            )

            // Auto Sync with Customer Ledger Card directly on screen
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = colors.surfaceElevated,
                border = BorderStroke(0.6.dp, colors.goldBorder.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 10.dp)) {
                        Text(
                            text = "ثبت خودکار در دفتر معین",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textMain,
                            fontFamily = VazirmatnFamily
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        val hint = if (invoice.isGoldDebt) {
                            "ثبت مانده وزنی در تراز دفتری همکار"
                        } else {
                            "ثبت مانده پرداخت‌نشده در حساب مشتری"
                        }
                        Text(
                            text = hint,
                            fontSize = 9.5.sp,
                            color = colors.textMuted,
                            fontFamily = VazirmatnFamily
                        )
                    }
                    Switch(
                        checked = invoice.syncWithLedger,
                        onCheckedChange = onSetSyncWithLedger,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = colors.goldPrimary,
                            checkedTrackColor = colors.goldContainer,
                            uncheckedThumbColor = colors.textMuted,
                            uncheckedTrackColor = colors.surface
                        )
                    )
                }
            }
        }
    }

    pendingDeletePayment?.let { itemToDelete ->
        DeleteSettlementConfirmDialog(
            paymentItem = itemToDelete,
            onDismiss = { pendingDeletePayment = null },
            onConfirm = {
                pendingDeletePayment = null
                onSetSettlementPayments(invoice.payments.filterNot { it.id == itemToDelete.id })
            }
        )
    }
}

// ---------------------------------------------------------------------------
// Rate Edit Dialog
// ---------------------------------------------------------------------------
@Composable
private fun EditRateDialog(
    currentRate: Long,
    liveRate: Long,
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit
) {
    val colors = LocalGoldExColors.current
    var rateStr by remember { mutableStateOf(currentRate.toString()) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = colors.surface,
            border = BorderStroke(1.dp, colors.goldBorder),
            shadowElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Title
                Text(
                    text = "تغییر نرخ مبنای ۱۸ عیار",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = VazirmatnFamily,
                    color = colors.textMain
                )

                Text(
                    text = "مظنه مورد نظر برای محاسبات و تبدیل‌های طلای این فاکتور را وارد فرمایید:",
                    fontSize = 12.sp,
                    color = colors.textSecondary,
                    fontFamily = VazirmatnFamily
                )

                // Input using GoldInputField with thousands separator and "تومان"
                GoldInputField(
                    value = rateStr,
                    onValueChange = { input ->
                        val digits = input.filter { it.isDigit() }
                        rateStr = digits
                    },
                    label = "نرخ هر گرم طلای ۱۸ عیار",
                    trailingText = "تومان",
                    useThousandsSeparator = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Quick live rate chip if available and differs from current input
                if (liveRate > 0) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = colors.surfaceVariant,
                        border = BorderStroke(0.5.dp, colors.goldBorder.copy(alpha = 0.4f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { rateStr = liveRate.toString() }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "نرخ زنده تابلو اتحادیه:",
                                fontSize = 11.5.sp,
                                color = colors.textSecondary,
                                fontFamily = VazirmatnFamily
                            )
                            Text(
                                text = "${PersianNumberFormatter.formatPrice(liveRate)} تومان",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.goldPrimary,
                                fontFamily = VazirmatnFamily
                            )
                        }
                    }
                }

                // Invariant: Cancel on Right (first child), Confirm on Left (second child) in RTL
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    GoldButton(
                        text = "انصراف",
                        onClick = onDismiss,
                        isSecondary = true,
                        modifier = Modifier.weight(0.38f)
                    )
                    GoldButton(
                        text = "اعمال نرخ",
                        onClick = {
                            val newRate = rateStr.toLongOrNull() ?: currentRate
                            if (newRate > 0L) {
                                onConfirm(newRate)
                            }
                        },
                        modifier = Modifier.weight(0.62f)
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Delete Settlement Confirmation Dialog
// ---------------------------------------------------------------------------
@Composable
private fun DeleteSettlementConfirmDialog(
    paymentItem: SettlementPaymentItem,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val colors = LocalGoldExColors.current
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = colors.surface,
            border = BorderStroke(1.dp, colors.goldBorder),
            shadowElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Title
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = InvoiceTrashVector,
                        contentDescription = null,
                        tint = Color(0xFFEF5350),
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "حذف پرداخت تسویه",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = VazirmatnFamily,
                        color = colors.textMain
                    )
                }

                Text(
                    text = "این ردیف پرداخت مربوط به یک سند تسویه دفتری است. با حذف آن، سند مربوطه پس از «ثبت نهایی فاکتور» از دفتر حساب مشتری حذف شده و اثر مالی آن بازگردانده خواهد شد.",
                    fontSize = 12.sp,
                    color = colors.textSecondary,
                    lineHeight = 20.sp,
                    fontFamily = VazirmatnFamily
                )

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = colors.surfaceVariant,
                    border = BorderStroke(0.5.dp, colors.goldBorder.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "روش: ${paymentItem.method.labelFa}${if (paymentItem.description.isNotBlank()) " • ${paymentItem.description}" else ""}",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = colors.textMain,
                            fontFamily = VazirmatnFamily
                        )
                        if (paymentItem.amountTomans > 0) {
                            Text(
                                text = "مبلغ: ${PersianNumberFormatter.formatTomans(paymentItem.amountTomans)} تومان",
                                fontSize = 11.sp,
                                color = colors.goldPrimary,
                                fontFamily = VazirmatnFamily
                            )
                        }
                        if (paymentItem.goldWeight18k > 0) {
                            Text(
                                text = "وزن معادل: ${PersianNumberFormatter.formatWeight(paymentItem.goldWeight18k)} گرم",
                                fontSize = 11.sp,
                                color = colors.goldPrimary,
                                fontFamily = VazirmatnFamily
                            )
                        }
                    }
                }

                Text(
                    text = "توجه: در صورتی که فاکتور را ذخیره نکنید، هیچ تغییری در حساب دفتری مشتری اعمال نخواهد شد.",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFFFFA726),
                    fontFamily = VazirmatnFamily
                )

                // Invariant: Cancel on Right (first child), Confirm on Left (second child) in RTL
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    GoldButton(
                        text = "انصراف",
                        onClick = onDismiss,
                        isSecondary = true,
                        modifier = Modifier.weight(0.38f)
                    )
                    GoldButton(
                        text = "حذف از فاکتور",
                        onClick = onConfirm,
                        modifier = Modifier.weight(0.62f)
                    )
                }
            }
        }
    }
}

