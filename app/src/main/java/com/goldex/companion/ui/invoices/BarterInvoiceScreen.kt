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
import com.goldex.companion.data.GoldMarketRepository
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
    customerList: List<Customer> = emptyList(),
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

    var isSettlementModalVisible by remember { mutableStateOf(false) }

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
                    onChangeCustomerClick = onOpenCustomerPicker
                )

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
                    balance = invoice.balance
                )

                // 6. Settlement & Payment Methods Section (Directly on screen outside modal)
                SettlementSection(
                    invoice = invoice,
                    balance = invoice.balance,
                    customerList = customerList,
                    invoicesList = uiState.invoicesList,
                    onSetSettlementPayments = onSetSettlementPayments,
                    onSetNote = onSetNote,
                    onSetSyncWithLedger = onSetSyncWithLedger,
                    onOpenSettlementModal = { isSettlementModalVisible = true }
                )

                // 7. Final Action Buttons
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    GoldButton(
                        text = "ثبت نهایی و صدور فاکتور رسمی تهاتر",
                        onClick = onFinalSubmit,
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

            // Settlement Modal
            if (isSettlementModalVisible) {
                SettlementModal(
                    invoice = invoice,
                    balance = balance,
                    customerList = customerList,
                    invoicesList = uiState.invoicesList,
                    onMethodSelect = onSetSettlementMethod,
                    onCashPosAmountChange = onSetCashPosAmount,
                    onLedgerAmountChange = onSetLedgerAmount,
                    onPosTrackingCodeChange = onSetPosTrackingCode,
                    onLedgerDueDateChange = onSetLedgerDueDate,
                    onBullionSettlementChange = onSetBullionSettlement,
                    onThirdPartyTransferChange = onSetThirdPartyTransfer,
                    onSettlementPaymentsChange = onSetSettlementPayments,
                    syncWithLedger = invoice.syncWithLedger,
                    onSyncWithLedgerChange = onSetSyncWithLedger,
                    note = invoice.note,
                    onNoteChange = onSetNote,
                    onDismiss = { isSettlementModalVisible = false }
                )
            }

            // Add / Edit Item Modal
            if (uiState.isItemModalVisible) {
                AddInvoiceItemModal(
                    spotPrice18k = invoice.spotPrice18k,
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
                        text = "مشخصات طرف حساب",
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
    balance: com.goldex.companion.model.BarterBalance
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
                        text = "تسویه ترکیبی ریالی-وزنی",
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
// 6. Settlement & Payment Section (Interactive, directly on screen outside modal)
// ---------------------------------------------------------------------------
@Composable
private fun SettlementSection(
    invoice: BarterInvoice,
    balance: BarterBalance,
    customerList: List<Customer>,
    invoicesList: List<InvoiceListItem>,
    onSetSettlementPayments: (List<SettlementPaymentItem>) -> Unit,
    onSetNote: (String) -> Unit,
    onSetSyncWithLedger: (Boolean) -> Unit,
    onOpenSettlementModal: () -> Unit
) {
    val colors = LocalGoldExColors.current
    val netPayableAmount = balance.netPayableAmount
    val absNetPayableLong = kotlin.math.abs(netPayableAmount).toLong()
    val totalPaid = invoice.totalPaymentsAmount
    val remainingBalance = invoice.remainingBalanceTomans

    // Channel selection state
    var selectedChannel by remember { mutableStateOf(SettlementMethod.POS) }

    // Channel specific inputs
    var posStr by remember { mutableStateOf("") }
    var trackingCode by remember { mutableStateOf("") }

    var ledgerStr by remember { mutableStateOf("") }
    var ledgerDueDate by remember { mutableStateOf("تسویه ماهانه") }

    var bullionWeightStr by remember { mutableStateOf("") }
    var bullionKaratStr by remember { mutableStateOf("750") }
    var bullionAngNumber by remember { mutableStateOf("") }

    var thirdPartyCustomer by remember { mutableStateOf<Customer?>(null) }
    var thirdPartyInvoiceId by remember { mutableStateOf("") }
    var thirdPartyInvoiceNumber by remember { mutableStateOf("") }
    var transferWeightStr by remember { mutableStateOf("") }
    var transferAmountStr by remember { mutableStateOf("") }
    var transferTrackingCode by remember { mutableStateOf("") }
    var transferIsGoldMode by remember { mutableStateOf(false) }
    var isThirdPartyPickerVisible by remember { mutableStateOf(false) }

    if (isThirdPartyPickerVisible) {
        ThirdPartyBarterPickerDialog(
            customers = customerList,
            invoices = invoicesList,
            currentInvoiceId = invoice.id,
            onSelectInvoice = { cust, inv ->
                thirdPartyCustomer = cust
                thirdPartyInvoiceId = inv.id
                thirdPartyInvoiceNumber = inv.cleanInvoiceNumber
                val w = transferWeightStr.toDoubleOrNull() ?: kotlin.math.abs(balance.net18kWeightDelta)
                val amt = transferAmountStr.toLongOrNull() ?: absNetPayableLong
                if (transferIsGoldMode) {
                    transferWeightStr = String.format(Locale.US, "%.3f", w)
                    transferAmountStr = ""
                } else {
                    transferAmountStr = amt.toString()
                    transferWeightStr = ""
                }
                isThirdPartyPickerVisible = false
            },
            onSelectCustomerLedger = { cust ->
                thirdPartyCustomer = cust
                thirdPartyInvoiceId = ""
                thirdPartyInvoiceNumber = "حساب دفتری باز"
                val w = transferWeightStr.toDoubleOrNull() ?: kotlin.math.abs(balance.net18kWeightDelta)
                val amt = transferAmountStr.toLongOrNull() ?: absNetPayableLong
                if (transferIsGoldMode) {
                    transferWeightStr = String.format(Locale.US, "%.3f", w)
                    transferAmountStr = ""
                } else {
                    transferAmountStr = amt.toString()
                    transferWeightStr = ""
                }
                isThirdPartyPickerVisible = false
            },
            onDismiss = { isThirdPartyPickerVisible = false }
        )
    }

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
                    shape = RoundedCornerShape(8.dp),
                    color = colors.goldContainer.copy(alpha = 0.4f),
                    border = BorderStroke(0.6.dp, colors.goldBorder),
                    modifier = Modifier.clickable(onClick = onOpenSettlementModal)
                ) {
                    Text(
                        text = "مودال تسویه",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.goldPrimary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.5.dp),
                        fontFamily = VazirmatnFamily
                    )
                }
            }

            // Financial Balance Overview Card
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = colors.surfaceElevated,
                border = BorderStroke(0.8.dp, if (remainingBalance == 0L && invoice.payments.isNotEmpty()) colors.profitGreen.copy(alpha = 0.5f) else colors.goldBorder.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
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
                            text = "${PersianNumberFormatter.formatTomans(absNetPayableLong)} تومان",
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
                            text = "${PersianNumberFormatter.formatTomans(totalPaid)} تومان",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (totalPaid > 0) colors.profitGreen else colors.textMuted,
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
                            text = if (remainingBalance == 0L && invoice.payments.isNotEmpty()) "وضعیت تسویه:" else "مانده پرداخت‌نشده (نسیه/دفتری):",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textMain,
                            fontFamily = VazirmatnFamily
                        )
                        if (remainingBalance == 0L && invoice.payments.isNotEmpty()) {
                            Text(
                                text = "تسویه کامل شد ✓",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.profitGreen,
                                fontFamily = VazirmatnFamily
                            )
                        } else {
                            Text(
                                text = "${PersianNumberFormatter.formatTomans(remainingBalance)} تومان",
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
                            modifier = Modifier.fillMaxWidth()
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
                                    onClick = {
                                        onSetSettlementPayments(invoice.payments.filterNot { it.id == pItem.id })
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
                            text = "هنوز پرداختی ثبت نشده است. در صورت عدم پرداخت نقدی، کل مانده فاکتور به عنوان حساب دفتری (نسیه) ثبت می‌گردد.",
                            fontSize = 10.sp,
                            color = colors.textMuted,
                            fontFamily = VazirmatnFamily
                        )
                    }
                }
            }

            HorizontalDivider(color = colors.border.copy(alpha = 0.5f), thickness = 0.6.dp)

            // Inline Add Payment Step (مستقیماً روی صفحه / بیرون از مدال)
            Text(
                text = "افزودن مرحله پرداخت به فاکتور:",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textMain,
                fontFamily = VazirmatnFamily
            )

            // Channel Selector Tabs
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                SettlementMethod.entries.forEach { method ->
                    val isSelected = selectedChannel == method
                    val bgColor by animateColorAsState(
                        targetValue = if (isSelected) colors.goldContainer.copy(alpha = 0.55f) else colors.surfaceVariant,
                        label = "settlementBg"
                    )
                    val borderColor by animateColorAsState(
                        targetValue = if (isSelected) colors.goldPrimary else colors.border,
                        label = "settlementBorder"
                    )
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = bgColor,
                        border = BorderStroke(if (isSelected) 1.2.dp else 0.6.dp, borderColor),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { selectedChannel = method }
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 7.dp, horizontal = 2.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = method.labelFa,
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) colors.goldPrimary else colors.textMain,
                                fontFamily = VazirmatnFamily,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            // Input Fields for selected channel
            AnimatedContent(
                targetState = selectedChannel,
                transitionSpec = { fadeIn(tween(160)) togetherWith fadeOut(tween(120)) },
                label = "inlineSettlementForm"
            ) { method ->
                when (method) {
                    SettlementMethod.POS -> {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                GoldInputField(
                                    value = posStr,
                                    onValueChange = { input ->
                                        posStr = input.filter { it.isDigit() }
                                    },
                                    label = "مبلغ پرداختی",
                                    trailingText = "تومان",
                                    useThousandsSeparator = true,
                                    modifier = Modifier.weight(1f)
                                )
                                GoldInputField(
                                    value = trackingCode,
                                    onValueChange = { trackingCode = it },
                                    label = "کد پیگیری",
                                    trailingText = "اختیاری",
                                    keyboardType = KeyboardType.Number,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                    SettlementMethod.LEDGER -> {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            GoldInputField(
                                value = ledgerStr,
                                onValueChange = { input ->
                                    ledgerStr = input.filter { it.isDigit() }
                                },
                                label = "مبلغ تعهد",
                                trailingText = "تومان",
                                useThousandsSeparator = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf("تسویه ماهانه", "۱۰ روزه", "پایان هفته", "تسویه امانی").forEach { term ->
                                    val isSelected = ledgerDueDate == term
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isSelected) colors.goldContainer else colors.surfaceVariant,
                                        border = BorderStroke(0.6.dp, if (isSelected) colors.goldPrimary else colors.border),
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { ledgerDueDate = term }
                                    ) {
                                        Text(
                                            text = term,
                                            fontSize = 9.5.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) colors.goldPrimary else colors.textSecondary,
                                            fontFamily = VazirmatnFamily,
                                            modifier = Modifier.padding(vertical = 5.dp),
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                        )
                                    }
                                }
                            }
                        }
                    }
                    SettlementMethod.BULLION -> {
                        val bullionWeight = bullionWeightStr.toDoubleOrNull() ?: 0.0
                        val bullionKarat = bullionKaratStr.toIntOrNull() ?: 750
                        val bullion18kEq = bullionWeight * (bullionKarat.toDouble() / 750.0)
                        val bullionValuation = (bullion18kEq * invoice.spotPrice18k).toLong()

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                GoldInputField(
                                    value = bullionWeightStr,
                                    onValueChange = { bullionWeightStr = it },
                                    label = "وزن شمش",
                                    trailingText = "گرم",
                                    keyboardType = KeyboardType.Decimal,
                                    modifier = Modifier.weight(1f)
                                )
                                GoldInputField(
                                    value = bullionAngNumber,
                                    onValueChange = { bullionAngNumber = it },
                                    label = "شماره انگ",
                                    trailingText = "اختیاری",
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            if (bullionValuation > 0L) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = colors.goldContainer.copy(alpha = 0.3f),
                                    border = BorderStroke(0.5.dp, colors.goldBorder),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "معادل طلای ۱۸ عیار: ${PersianNumberFormatter.formatWeight(bullion18kEq)} گرم • ارزش ریالی: ${PersianNumberFormatter.formatTomans(bullionValuation)} تومان",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = colors.goldPrimary,
                                        fontFamily = VazirmatnFamily,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }
                    SettlementMethod.TRANSFER -> {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = colors.surfaceVariant,
                                border = BorderStroke(0.8.dp, colors.goldBorder),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { isThirdPartyPickerVisible = true }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (thirdPartyCustomer != null) "همکار: ${thirdPartyCustomer?.name} (${if (thirdPartyInvoiceNumber.isNotBlank()) "فاکتور: $thirdPartyInvoiceNumber" else "حساب دفتری"})" else "انتخاب همکار یا فاکتور باز برای حواله سه‌طرفه",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = if (thirdPartyCustomer != null) colors.textMain else colors.goldPrimary,
                                        fontFamily = VazirmatnFamily
                                    )
                                    Text(
                                        text = "انتخاب ›",
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.goldPrimary,
                                        fontFamily = VazirmatnFamily
                                    )
                                }
                            }

                            LuxurySegmentedControl(
                                items = listOf(false, true),
                                selectedItem = transferIsGoldMode,
                                onItemSelected = { isGold ->
                                    transferIsGoldMode = isGold
                                    if (isGold) transferAmountStr = "" else transferWeightStr = ""
                                },
                                label = { if (!it) "حواله نقدی (بانکی / پایا)" else "حواله وزنی طلا (گرم)" },
                                modifier = Modifier.fillMaxWidth(),
                                height = 32.dp,
                                fontSize = 10.5.sp
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (!transferIsGoldMode) {
                                    GoldInputField(
                                        value = transferAmountStr,
                                        onValueChange = { input ->
                                            transferAmountStr = input.filter { it.isDigit() }
                                            transferWeightStr = ""
                                        },
                                        label = "مبلغ حواله",
                                        trailingText = "تومان",
                                        useThousandsSeparator = true,
                                        modifier = Modifier.weight(1.2f)
                                    )
                                } else {
                                    GoldInputField(
                                        value = transferWeightStr,
                                        onValueChange = { input ->
                                            transferWeightStr = input
                                            transferAmountStr = ""
                                        },
                                        label = "وزن حواله",
                                        trailingText = "گرم",
                                        keyboardType = KeyboardType.Decimal,
                                        modifier = Modifier.weight(1.2f)
                                    )
                                }
                                GoldInputField(
                                    value = transferTrackingCode,
                                    onValueChange = { transferTrackingCode = it },
                                    label = "کد پیگیری",
                                    trailingText = "اختیاری",
                                    keyboardType = KeyboardType.Number,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }

            // Quick Auto-Fill remaining balance button (if remainingBalance > 0)
            if (remainingBalance > 0L) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = colors.goldContainer.copy(alpha = 0.35f),
                    border = BorderStroke(0.6.dp, colors.goldBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            when (selectedChannel) {
                                SettlementMethod.POS -> posStr = remainingBalance.toString()
                                SettlementMethod.LEDGER -> ledgerStr = remainingBalance.toString()
                                SettlementMethod.TRANSFER -> {
                                    if (!transferIsGoldMode) {
                                        transferAmountStr = remainingBalance.toString()
                                        transferWeightStr = ""
                                    } else {
                                        if (invoice.spotPrice18k > 0) {
                                            val w = remainingBalance.toDouble() / invoice.spotPrice18k
                                            transferWeightStr = String.format(Locale.US, "%.3f", w)
                                        }
                                        transferAmountStr = ""
                                    }
                                }
                                SettlementMethod.BULLION -> {
                                    if (invoice.spotPrice18k > 0) {
                                        val w = remainingBalance.toDouble() / invoice.spotPrice18k
                                        bullionWeightStr = String.format(Locale.US, "%.3f", w)
                                    }
                                }
                            }
                        }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 7.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "⚡ تنظیم این روش با باقیمانده مانده صافی:",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = colors.textMain,
                            fontFamily = VazirmatnFamily
                        )
                        Text(
                            text = "${PersianNumberFormatter.formatTomans(remainingBalance)} تومان",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.goldPrimary,
                            fontFamily = VazirmatnFamily
                        )
                    }
                }
            }

            // Button: Add to Payments List (OUTSIDE MODAL!)
            GoldButton(
                text = "افزودن این مرحله پرداخت به لیست",
                onClick = {
                    val newPayment = when (selectedChannel) {
                        SettlementMethod.POS -> {
                            val amt = posStr.toLongOrNull() ?: 0L
                            if (amt > 0) {
                                SettlementPaymentItem(
                                    id = UUID.randomUUID().toString(),
                                    method = SettlementMethod.POS,
                                    amountTomans = amt,
                                    trackingCode = trackingCode
                                )
                            } else null
                        }
                        SettlementMethod.LEDGER -> {
                            val amt = ledgerStr.toLongOrNull() ?: 0L
                            if (amt > 0) {
                                SettlementPaymentItem(
                                    id = UUID.randomUUID().toString(),
                                    method = SettlementMethod.LEDGER,
                                    amountTomans = amt,
                                    description = "موعد: $ledgerDueDate"
                                )
                            } else null
                        }
                        SettlementMethod.BULLION -> {
                            val w = bullionWeightStr.toDoubleOrNull() ?: 0.0
                            val k = bullionKaratStr.toIntOrNull() ?: 750
                            val eq18k = w * (k.toDouble() / 750.0)
                            val valTomans = (eq18k * invoice.spotPrice18k).toLong()
                            if (w > 0.0 || valTomans > 0L) {
                                SettlementPaymentItem(
                                    id = UUID.randomUUID().toString(),
                                    method = SettlementMethod.BULLION,
                                    amountTomans = valTomans,
                                    goldWeight18k = eq18k,
                                    bullionKarat = k,
                                    bullionAngNumber = bullionAngNumber
                                )
                            } else null
                        }
                        SettlementMethod.TRANSFER -> {
                            val amt = if (!transferIsGoldMode) (transferAmountStr.toLongOrNull() ?: 0L) else 0L
                            val w = if (transferIsGoldMode) (transferWeightStr.toDoubleOrNull() ?: 0.0) else 0.0
                            if (amt > 0L || w > 0.0) {
                                SettlementPaymentItem(
                                    id = UUID.randomUUID().toString(),
                                    method = SettlementMethod.TRANSFER,
                                    amountTomans = amt,
                                    goldWeight18k = w,
                                    trackingCode = transferTrackingCode,
                                    thirdPartyCustomerName = thirdPartyCustomer?.name ?: ""
                                )
                            } else null
                        }
                    }
                    if (newPayment != null) {
                        onSetSettlementPayments(invoice.payments + newPayment)
                        posStr = ""
                        trackingCode = ""
                        ledgerStr = ""
                        bullionWeightStr = ""
                        bullionAngNumber = ""
                        transferAmountStr = ""
                        transferWeightStr = ""
                        transferTrackingCode = ""
                        thirdPartyCustomer = null
                    }
                },
                icon = InvoicePlusVector,
                isSecondary = true,
                modifier = Modifier.fillMaxWidth()
            )

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
                        val hint = if (invoice.customerRole == CustomerRole.WHOLESALER) {
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
}

// ---------------------------------------------------------------------------
// 7. Settlement Modal (Modal Bottom Sheet)
// ---------------------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettlementModal(
    invoice: BarterInvoice,
    balance: BarterBalance,
    customerList: List<Customer>,
    invoicesList: List<InvoiceListItem>,
    onMethodSelect: (SettlementMethod) -> Unit,
    onCashPosAmountChange: (Long) -> Unit,
    onLedgerAmountChange: (Long) -> Unit,
    onPosTrackingCodeChange: (String) -> Unit,
    onLedgerDueDateChange: (String) -> Unit,
    onBullionSettlementChange: (Double, Int, String) -> Unit,
    onThirdPartyTransferChange: (Customer?, String, String, Double, Long, String) -> Unit,
    onSettlementPaymentsChange: (List<SettlementPaymentItem>) -> Unit = {},
    syncWithLedger: Boolean = true,
    onSyncWithLedgerChange: (Boolean) -> Unit = {},
    note: String,
    onNoteChange: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalGoldExColors.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val netPayableAmount: Double = balance.netPayableAmount
    val selectedMethod: SettlementMethod = invoice.settlementMethod

    var paymentsList by remember(invoice.payments) {
        mutableStateOf(invoice.payments)
    }

    var posStr by remember(invoice.cashPosAmount) {
        mutableStateOf(if (invoice.cashPosAmount > 0) invoice.cashPosAmount.toString() else "")
    }
    var trackingCode by remember(invoice.posTrackingCode) {
        mutableStateOf(invoice.posTrackingCode)
    }

    val absNetPayableLong: Long = kotlin.math.abs(netPayableAmount).toLong()
    val totalPaid: Long = paymentsList.sumOf { it.amountTomans }
    val remainingBalance: Long = (absNetPayableLong - totalPaid).coerceAtLeast(0L)

    var ledgerStr by remember(invoice.ledgerAmount, netPayableAmount) {
        mutableStateOf(
            if (invoice.ledgerAmount > 0) invoice.ledgerAmount.toString()
            else if (selectedMethod == SettlementMethod.LEDGER) absNetPayableLong.toString()
            else ""
        )
    }
    var ledgerDueDate by remember(invoice.ledgerDueDate) {
        mutableStateOf(invoice.ledgerDueDate)
    }

    var bullionWeightStr by remember(invoice.bullionWeight) {
        mutableStateOf(if (invoice.bullionWeight > 0.0) invoice.bullionWeight.toString() else "")
    }
    var bullionKaratStr by remember(invoice.bullionKarat) {
        mutableStateOf(invoice.bullionKarat.toString())
    }
    var bullionAngNumber by remember(invoice.bullionAngNumber) {
        mutableStateOf(invoice.bullionAngNumber)
    }

    val bullionWeight: Double = bullionWeightStr.toDoubleOrNull() ?: 0.0
    val bullionKarat: Int = bullionKaratStr.toIntOrNull() ?: 750
    val bullion18kEq: Double = bullionWeight * (bullionKarat.toDouble() / 750.0)
    val bullionValuation: Long = (bullion18kEq * invoice.spotPrice18k).toLong()
    val bullionRemainingDiff: Long = absNetPayableLong - bullionValuation

    val ledgerRemain: Long = (absNetPayableLong - invoice.cashPosAmount).coerceAtLeast(0L)

    // Third-party transfer state
    var thirdPartyCustomer by remember(invoice.thirdPartyCustomer) {
        mutableStateOf(invoice.thirdPartyCustomer)
    }
    var thirdPartyInvoiceId by remember(invoice.thirdPartyInvoiceId) {
        mutableStateOf(invoice.thirdPartyInvoiceId)
    }
    var thirdPartyInvoiceNumber by remember(invoice.thirdPartyInvoiceNumber) {
        mutableStateOf(invoice.thirdPartyInvoiceNumber)
    }
    var transferIsGoldMode by remember {
        mutableStateOf(invoice.thirdPartyTransferWeight18k > 0.0 && invoice.thirdPartyTransferAmount == 0L)
    }
    var transferWeightStr by remember(invoice.thirdPartyTransferWeight18k) {
        mutableStateOf(
            if (invoice.thirdPartyTransferWeight18k > 0.0) invoice.thirdPartyTransferWeight18k.toString()
            else ""
        )
    }
    var transferAmountStr by remember(invoice.thirdPartyTransferAmount) {
        mutableStateOf(
            if (invoice.thirdPartyTransferAmount > 0L) invoice.thirdPartyTransferAmount.toString()
            else ""
        )
    }
    var transferTrackingCode by remember(invoice.thirdPartyTrackingCode) {
        mutableStateOf(invoice.thirdPartyTrackingCode)
    }
    var isThirdPartyPickerVisible by remember { mutableStateOf(false) }

    if (isThirdPartyPickerVisible) {
        ThirdPartyBarterPickerDialog(
            customers = customerList,
            invoices = invoicesList,
            currentInvoiceId = invoice.id,
            onSelectInvoice = { cust, inv ->
                thirdPartyCustomer = cust
                thirdPartyInvoiceId = inv.id
                thirdPartyInvoiceNumber = inv.invoiceNumber
                val w = transferWeightStr.toDoubleOrNull() ?: kotlin.math.abs(balance.net18kWeightDelta)
                val amt = transferAmountStr.toLongOrNull() ?: absNetPayableLong
                if (transferIsGoldMode) {
                    transferWeightStr = String.format(java.util.Locale.US, "%.3f", w)
                    transferAmountStr = ""
                    onThirdPartyTransferChange(cust, inv.id, inv.invoiceNumber, w, 0L, transferTrackingCode)
                } else {
                    transferAmountStr = amt.toString()
                    transferWeightStr = ""
                    onThirdPartyTransferChange(cust, inv.id, inv.invoiceNumber, 0.0, amt, transferTrackingCode)
                }
                isThirdPartyPickerVisible = false
            },
            onSelectCustomerLedger = { cust ->
                thirdPartyCustomer = cust
                thirdPartyInvoiceId = ""
                thirdPartyInvoiceNumber = "حساب دفتری باز"
                val w = transferWeightStr.toDoubleOrNull() ?: kotlin.math.abs(balance.net18kWeightDelta)
                val amt = transferAmountStr.toLongOrNull() ?: absNetPayableLong
                if (transferIsGoldMode) {
                    transferWeightStr = String.format(java.util.Locale.US, "%.3f", w)
                    transferAmountStr = ""
                    onThirdPartyTransferChange(cust, "", "حساب دفتری باز", w, 0L, transferTrackingCode)
                } else {
                    transferAmountStr = amt.toString()
                    transferWeightStr = ""
                    onThirdPartyTransferChange(cust, "", "حساب دفتری باز", 0.0, amt, transferTrackingCode)
                }
                isThirdPartyPickerVisible = false
            },
            onDismiss = { isThirdPartyPickerVisible = false }
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.surface,
        dragHandle = null,
        modifier = Modifier.fillMaxHeight(0.92f)
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .navigationBarsPadding()
            ) {
                // Standard modal header: grabber, identity block, and close action.
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .width(44.dp)
                            .height(4.dp)
                            .clip(CircleShape)
                            .background(colors.border)
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        Brush.linearGradient(
                                            listOf(colors.goldPrimary, colors.goldSecondary)
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = InvoiceCheckVector,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(19.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "روش تسویه مانده صافی",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.textMain,
                                    fontFamily = VazirmatnFamily
                                )
                                Text(
                                    text = "ترکیب پرداخت و ثبت مانده حساب",
                                    fontSize = 11.sp,
                                    color = colors.textMuted,
                                    fontFamily = VazirmatnFamily
                                )
                            }
                        }

                        Spacer(Modifier.width(12.dp))
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(32.dp)
                                .clip(ButtonShape)
                                .background(colors.surfaceElevated)
                                .border(0.6.dp, colors.border, ButtonShape)
                        ) {
                            Icon(
                                imageVector = InvoiceCloseVector,
                                contentDescription = "بستن",
                                tint = colors.textMuted,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
                HorizontalDivider(color = colors.border, thickness = 0.6.dp)

                // Scrollable Body
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Multi-payment Overview & Balance Status Card
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = colors.surfaceVariant.copy(alpha = 0.5f),
                        border = BorderStroke(0.8.dp, if (remainingBalance == 0L && (paymentsList.isNotEmpty() || invoice.cashPosAmount >= absNetPayableLong)) Color(0xFF34D399).copy(alpha = 0.6f) else colors.goldBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "مبلغ کل صافی فاکتور:",
                                    fontSize = 11.5.sp,
                                    color = colors.textSecondary,
                                    fontFamily = VazirmatnFamily
                                )
                                Text(
                                    text = "${PersianNumberFormatter.formatTomans(absNetPayableLong)} تومان",
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
                                    text = "مجموع مراحل ثبت‌شده:",
                                    fontSize = 11.5.sp,
                                    color = colors.textSecondary,
                                    fontFamily = VazirmatnFamily
                                )
                                Text(
                                    text = "${PersianNumberFormatter.formatTomans(totalPaid)} تومان",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (totalPaid > 0) Color(0xFF34D399) else colors.textMuted,
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
                                    text = "مانده تسویه نشده:",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.textMain,
                                    fontFamily = VazirmatnFamily
                                )
                                if (remainingBalance == 0L && (paymentsList.isNotEmpty() || invoice.cashPosAmount >= absNetPayableLong)) {
                                    Text(
                                        text = "تسویه کامل شد ✓",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF34D399),
                                        fontFamily = VazirmatnFamily
                                    )
                                } else {
                                    Text(
                                        text = "${PersianNumberFormatter.formatTomans(remainingBalance)} تومان",
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.Black,
                                        color = colors.goldPrimary,
                                        fontFamily = VazirmatnFamily
                                    )
                                }
                            }
                        }
                    }

                    // If there are recorded payments, show the list
                    if (paymentsList.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "مراحل پرداخت ثبت‌شده (${PersianNumberFormatter.toPersianDigits(paymentsList.size.toString())}):",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textMain,
                                fontFamily = VazirmatnFamily
                            )
                            paymentsList.forEach { pItem ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = colors.surfaceElevated,
                                    border = BorderStroke(0.6.dp, colors.goldBorder.copy(alpha = 0.5f)),
                                    modifier = Modifier.fillMaxWidth()
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
                                                    .size(30.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(colors.goldContainer.copy(alpha = 0.5f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(text = icon, fontSize = 13.sp)
                                            }
                                            Column {
                                                Text(
                                                    text = pItem.method.labelFa,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = colors.textMain,
                                                    fontFamily = VazirmatnFamily
                                                )
                                                val pDetail = buildString {
                                                    if (pItem.amountTomans > 0) append("${PersianNumberFormatter.formatTomans(pItem.amountTomans)} ت")
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
                                            onClick = {
                                                paymentsList = paymentsList.filterNot { it.id == pItem.id }
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
                    }
            // Header with Net Balance preview
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
                            .size(5.dp, 16.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(colors.goldPrimary)
                    )
                    Text(
                        text = "روش‌های تسویه مانده صافی",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textMain,
                        fontFamily = VazirmatnFamily
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "صافی کل: ",
                        fontSize = 11.sp,
                        color = colors.textSecondary,
                        fontFamily = VazirmatnFamily
                    )
                    AnimatedPriceText(
                        amount = absNetPayableLong,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        color = colors.goldPrimary
                    )
                    Text(
                        text = " تومان",
                        fontSize = 11.sp,
                        color = colors.goldPrimary,
                        fontFamily = VazirmatnFamily
                    )
                }
            }

            // Settlement Channels Selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                SettlementMethod.entries.forEach { method ->
                    val isSelected = selectedMethod == method
                    val bgColor by animateColorAsState(
                        targetValue = if (isSelected) colors.goldContainer.copy(alpha = 0.55f) else colors.surfaceVariant,
                        label = "settlementBg"
                    )
                    val borderColor by animateColorAsState(
                        targetValue = if (isSelected) colors.goldPrimary else colors.border,
                        label = "settlementBorder"
                    )
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = bgColor,
                        border = BorderStroke(if (isSelected) 1.2.dp else 0.6.dp, borderColor),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onMethodSelect(method) }
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = method.labelFa,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) colors.goldPrimary else colors.textMain,
                                fontFamily = VazirmatnFamily,
                                maxLines = 1
                            )
                            Text(
                                text = method.subtitleFa,
                                fontSize = 9.sp,
                                color = colors.textSecondary,
                                fontFamily = VazirmatnFamily,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            // Dynamic Content based on selected settlement method with smooth animation
            AnimatedContent(
                targetState = selectedMethod,
                transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(140)) },
                label = "settlementFormAnim"
            ) { method: SettlementMethod ->
                when (method) {
                    SettlementMethod.POS -> {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            // POS Payment Amount & Tracking Code
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                GoldInputField(
                                    value = posStr,
                                    onValueChange = { input ->
                                        val digits = input.filter { it.isDigit() }
                                        posStr = digits
                                        onCashPosAmountChange(digits.toLongOrNull() ?: 0L)
                                    },
                                    label = "مبلغ پرداختی",
                                    trailingText = "تومان",
                                    useThousandsSeparator = true,
                                    modifier = Modifier.weight(1f)
                                )

                                GoldInputField(
                                    value = trackingCode,
                                    onValueChange = {
                                        trackingCode = it
                                        onPosTrackingCodeChange(it)
                                    },
                                    label = "شماره پیگیری",
                                    trailingText = "کد پیگیری",
                                    keyboardType = KeyboardType.Number,
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            // Remaining Ledger Balance Status Card
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = colors.surfaceVariant,
                                border = BorderStroke(0.6.dp, colors.border),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (ledgerRemain <= 0L) "تسویه نقدی کامل صورت پذیرفت" else "مانده دفتری پرداخت‌نشده:",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = if (ledgerRemain <= 0L) colors.profitGreen else colors.textSecondary,
                                        fontFamily = VazirmatnFamily
                                    )
                                    if (ledgerRemain > 0L) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            AnimatedPriceText(
                                                amount = ledgerRemain,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = colors.errorRed
                                            )
                                            Text(
                                                text = " تومان",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = colors.errorRed,
                                                fontFamily = VazirmatnFamily
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    SettlementMethod.TRANSFER -> {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            // Explanatory badge
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = colors.goldContainer.copy(alpha = 0.35f),
                                border = BorderStroke(0.8.dp, colors.goldBorder)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "💡 حواله سه‌طرفه زرگری:",
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.goldPrimary,
                                        fontFamily = VazirmatnFamily
                                    )
                                    Text(
                                        text = "انتقال تعهد وزنی مانده به حساب همکار یا فاکتور باز",
                                        fontSize = 10.sp,
                                        color = colors.textSecondary,
                                        fontFamily = VazirmatnFamily
                                    )
                                }
                            }

                            // 1. Third Party Customer & Invoice Selection Card
                            if (thirdPartyCustomer == null) {
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = colors.surfaceVariant,
                                    border = BorderStroke(1.dp, colors.goldBorder),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { isThirdPartyPickerVisible = true }
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
                                                    .size(38.dp)
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(colors.goldContainer),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = CustomerIconVector,
                                                    contentDescription = null,
                                                    tint = colors.goldPrimary,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                                Text(
                                                    text = "انتخاب طرف حساب ثالث (طلبکار / همکار)",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = colors.textMain,
                                                    fontFamily = VazirmatnFamily
                                                )
                                                Text(
                                                    text = "جهت تهاتر و کسر از فاکتور باز او",
                                                    fontSize = 10.sp,
                                                    color = colors.textSecondary,
                                                    fontFamily = VazirmatnFamily
                                                )
                                            }
                                        }
                                        Text(
                                            text = "انتخاب از دفتر ❯",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = colors.goldPrimary,
                                            fontFamily = VazirmatnFamily
                                        )
                                    }
                                }
                            } else {
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = colors.surfaceVariant,
                                    border = BorderStroke(1.dp, colors.goldPrimary),
                                    modifier = Modifier.fillMaxWidth()
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
                                                    .size(38.dp)
                                                    .clip(CircleShape)
                                                    .background(colors.goldPrimary),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = thirdPartyCustomer?.name?.firstOrNull()?.toString() ?: "هـ",
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White,
                                                    fontFamily = VazirmatnFamily
                                                )
                                            }
                                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                                Text(
                                                    text = thirdPartyCustomer?.name ?: "طرف حساب ثالث",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = colors.textMain,
                                                    fontFamily = VazirmatnFamily
                                                )
                                                Text(
                                                    text = if (thirdPartyInvoiceNumber.isNotBlank()) "فاکتور پیوست تهاتر: $thirdPartyInvoiceNumber" else "حساب دفتری همکار",
                                                    fontSize = 10.sp,
                                                    color = colors.goldPrimary,
                                                    fontFamily = VazirmatnFamily
                                                )
                                            }
                                        }

                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "تغییر",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = colors.goldPrimary,
                                                modifier = Modifier
                                                    .clickable { isThirdPartyPickerVisible = true }
                                                    .padding(4.dp),
                                                fontFamily = VazirmatnFamily
                                            )
                                            IconButton(
                                                onClick = {
                                                    thirdPartyCustomer = null
                                                    thirdPartyInvoiceId = ""
                                                    thirdPartyInvoiceNumber = ""
                                                    onThirdPartyTransferChange(null, "", "", 0.0, 0L, transferTrackingCode)
                                                },
                                                modifier = Modifier.size(26.dp)
                                            ) {
                                                Icon(
                                                    imageVector = InvoiceCloseVector,
                                                    contentDescription = "حذف انتخاب",
                                                    tint = colors.textMuted,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // 2. Barter Transfer Quantities: Toggle between Cash and Gold
                            LuxurySegmentedControl(
                                items = listOf(false, true),
                                selectedItem = transferIsGoldMode,
                                onItemSelected = { isGold ->
                                    transferIsGoldMode = isGold
                                    if (isGold) {
                                        transferAmountStr = ""
                                        val w = transferWeightStr.toDoubleOrNull() ?: 0.0
                                        onThirdPartyTransferChange(
                                            thirdPartyCustomer,
                                            thirdPartyInvoiceId,
                                            thirdPartyInvoiceNumber,
                                            w,
                                            0L,
                                            transferTrackingCode
                                        )
                                    } else {
                                        transferWeightStr = ""
                                        val amt = transferAmountStr.toLongOrNull() ?: 0L
                                        onThirdPartyTransferChange(
                                            thirdPartyCustomer,
                                            thirdPartyInvoiceId,
                                            thirdPartyInvoiceNumber,
                                            0.0,
                                            amt,
                                            transferTrackingCode
                                        )
                                    }
                                },
                                label = { if (!it) "حواله نقدی (بانکی / پایا)" else "حواله وزنی طلا (گرم)" },
                                modifier = Modifier.fillMaxWidth(),
                                height = 32.dp,
                                fontSize = 10.5.sp
                            )

                            if (!transferIsGoldMode) {
                                GoldInputField(
                                    value = transferAmountStr,
                                    onValueChange = { input ->
                                        val digits = input.filter { it.isDigit() }
                                        transferAmountStr = digits
                                        transferWeightStr = ""
                                        val amt = digits.toLongOrNull() ?: 0L
                                        onThirdPartyTransferChange(
                                            thirdPartyCustomer,
                                            thirdPartyInvoiceId,
                                            thirdPartyInvoiceNumber,
                                            0.0,
                                            amt,
                                            transferTrackingCode
                                        )
                                    },
                                    label = "مبلغ حواله نقدی",
                                    trailingText = "تومان",
                                    useThousandsSeparator = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            } else {
                                GoldInputField(
                                    value = transferWeightStr,
                                    onValueChange = { input ->
                                        transferWeightStr = input
                                        transferAmountStr = ""
                                        val w = input.toDoubleOrNull() ?: 0.0
                                        onThirdPartyTransferChange(
                                            thirdPartyCustomer,
                                            thirdPartyInvoiceId,
                                            thirdPartyInvoiceNumber,
                                            w,
                                            0L,
                                            transferTrackingCode
                                        )
                                    },
                                    label = "وزن حواله طلا",
                                    trailingText = "گرم",
                                    isDecimal = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            // Quick button to set full net barter
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = colors.goldContainer.copy(alpha = 0.5f),
                                border = BorderStroke(0.6.dp, colors.goldBorder),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (transferIsGoldMode) {
                                            val totalW = kotlin.math.abs(balance.net18kWeightDelta)
                                            transferWeightStr = String.format(java.util.Locale.US, "%.3f", totalW)
                                            transferAmountStr = ""
                                            onThirdPartyTransferChange(
                                                thirdPartyCustomer,
                                                thirdPartyInvoiceId,
                                                thirdPartyInvoiceNumber,
                                                totalW,
                                                0L,
                                                transferTrackingCode
                                            )
                                        } else {
                                            val totalA = absNetPayableLong
                                            transferAmountStr = totalA.toString()
                                            transferWeightStr = ""
                                            onThirdPartyTransferChange(
                                                thirdPartyCustomer,
                                                thirdPartyInvoiceId,
                                                thirdPartyInvoiceNumber,
                                                0.0,
                                                totalA,
                                                transferTrackingCode
                                            )
                                        }
                                    }
                            ) {
                                Box(
                                    modifier = Modifier.padding(vertical = 7.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (transferIsGoldMode) {
                                            "⚡ تهاتر کامل مانده فاکتور (${PersianNumberFormatter.formatWeight(kotlin.math.abs(balance.net18kWeightDelta))} گرم طلای ۱۸ عیار)"
                                        } else {
                                            "⚡ تسویه کامل مانده فاکتور (${PersianNumberFormatter.formatPrice(absNetPayableLong.toDouble())} تومان)"
                                        },
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.goldPrimary,
                                        fontFamily = VazirmatnFamily
                                    )
                                }
                            }

                            // Tracking / Receipt Code
                            GoldInputField(
                                value = transferTrackingCode,
                                onValueChange = {
                                    transferTrackingCode = it
                                    val w = transferWeightStr.toDoubleOrNull() ?: 0.0
                                    val amt = transferAmountStr.toLongOrNull() ?: 0L
                                    onThirdPartyTransferChange(
                                        thirdPartyCustomer,
                                        thirdPartyInvoiceId,
                                        thirdPartyInvoiceNumber,
                                        w,
                                        amt,
                                        it
                                    )
                                },
                                label = "شماره پیگیری حواله",
                                trailingText = "کد پیگیری",
                                keyboardType = KeyboardType.Text,
                                modifier = Modifier.fillMaxWidth()
                            )

                            // 3. Dual Balance Impact Preview Card
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = colors.surfaceVariant,
                                border = BorderStroke(0.6.dp, colors.border),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "پیش‌نمایش تراز حساب‌ها پس از ثبت فاکتور:",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.textMain,
                                        fontFamily = VazirmatnFamily
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "فاکتور جاری (${invoice.customer?.name ?: "خریدار"}):",
                                            fontSize = 10.5.sp,
                                            color = colors.textSecondary,
                                            fontFamily = VazirmatnFamily
                                        )
                                        Text(
                                            text = "تسویه کامل (مانده: ۰ گرم)",
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = colors.profitGreen,
                                            fontFamily = VazirmatnFamily
                                        )
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        val targetName = thirdPartyCustomer?.name ?: "شخص ثالث"
                                        Text(
                                            text = "طرف حساب ($targetName):",
                                            fontSize = 10.5.sp,
                                            color = colors.textSecondary,
                                            fontFamily = VazirmatnFamily
                                        )
                                        val w = transferWeightStr.toDoubleOrNull() ?: 0.0
                                        Text(
                                            text = "کسر ${PersianNumberFormatter.formatWeight(w)} گرم طلای ۱۸ عیار",
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

                    SettlementMethod.LEDGER -> {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            // Highlight gold weight remainder as primary unit
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = colors.goldContainer.copy(alpha = 0.35f),
                                border = BorderStroke(1.dp, colors.goldBorder),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Text(
                                            text = "مانده وزنی طلای ۱۸ عیار (مبنای دفتری):",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = colors.textSecondary,
                                            fontFamily = VazirmatnFamily
                                        )
                                        Text(
                                            text = "حساب‌های طلافروشی به وزن طلا ثبت و نگهداری می‌شوند",
                                            fontSize = 9.5.sp,
                                            color = colors.textMuted,
                                            fontFamily = VazirmatnFamily
                                        )
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        AnimatedNumberText(
                                            text = PersianNumberFormatter.formatWeight(kotlin.math.abs(balance.net18kWeightDelta)),
                                            fontSize = 13.5.sp,
                                            fontWeight = FontWeight.Black,
                                            color = colors.goldPrimary
                                        )
                                        Text(
                                            text = " گرم",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = colors.goldPrimary,
                                            fontFamily = VazirmatnFamily
                                        )
                                    }
                                }
                            }

                            // Ledger Amount & Due Date
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                GoldInputField(
                                    value = ledgerStr,
                                    onValueChange = { input ->
                                        val digits = input.filter { it.isDigit() }
                                        ledgerStr = digits
                                        onLedgerAmountChange(digits.toLongOrNull() ?: 0L)
                                    },
                                    label = "مبلغ انتقالی",
                                    trailingText = "تومان",
                                    useThousandsSeparator = true,
                                    modifier = Modifier.weight(1f)
                                )

                                GoldInputField(
                                    value = ledgerDueDate,
                                    onValueChange = {
                                        ledgerDueDate = it
                                        onLedgerDueDateChange(it)
                                    },
                                    label = "موعد و شرایط تسویه",
                                    trailingText = "سررسید",
                                    keyboardType = KeyboardType.Text,
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            // Quick Terms Chips
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf("تسویه ماهانه", "۱۰ روزه", "پایان هفته", "تسویه امانی").forEach { term ->
                                    val isSelected = ledgerDueDate == term
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isSelected) colors.goldContainer.copy(alpha = 0.5f) else colors.surfaceVariant,
                                        border = BorderStroke(0.6.dp, if (isSelected) colors.goldPrimary else colors.border),
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable {
                                                ledgerDueDate = term
                                                onLedgerDueDateChange(term)
                                            }
                                    ) {
                                        Box(
                                            modifier = Modifier.padding(vertical = 5.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = term,
                                                fontSize = 10.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isSelected) colors.goldPrimary else colors.textSecondary,
                                                fontFamily = VazirmatnFamily
                                            )
                                        }
                                    }
                                }
                            }

                            // Counterparty Ledger Card
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = colors.surfaceVariant,
                                border = BorderStroke(0.6.dp, colors.border),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "وضعیت ثبت در حساب طرف معامله:",
                                        fontSize = 11.sp,
                                        color = colors.textSecondary,
                                        fontFamily = VazirmatnFamily
                                    )
                                    Text(
                                        text = if (balance.isCustomerDebtor) "بدهکار قطعی همکار" else "بستانکار قطعی همکار",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (balance.isCustomerDebtor) colors.errorRed else colors.profitGreen,
                                        fontFamily = VazirmatnFamily
                                    )
                                }
                            }
                        }
                    }

                    SettlementMethod.BULLION -> {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            // Bullion Weight & Karat
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                GoldInputField(
                                    value = bullionWeightStr,
                                    onValueChange = {
                                        bullionWeightStr = it
                                        onBullionSettlementChange(
                                            it.toDoubleOrNull() ?: 0.0,
                                            bullionKarat,
                                            bullionAngNumber
                                        )
                                    },
                                    label = "وزن",
                                    trailingText = "گرم",
                                    isDecimal = true,
                                    modifier = Modifier.weight(1f)
                                )

                                GoldInputField(
                                    value = bullionKaratStr,
                                    onValueChange = {
                                        val digits = it.filter { ch -> ch.isDigit() }
                                        bullionKaratStr = digits
                                        onBullionSettlementChange(
                                            bullionWeight,
                                            digits.toIntOrNull() ?: 750,
                                            bullionAngNumber
                                        )
                                    },
                                    label = "عیار ری‌گیری (خط)",
                                    trailingText = "خط",
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            // Quick Karat Chips for Bullion
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf(750 to "۷۵۰ (استاندارد)", 735 to "۷۳۵ (رایج)", 705 to "۷۰۵ (سنتی)").forEach { (k, label) ->
                                    val isSelected = bullionKarat == k
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isSelected) colors.goldContainer.copy(alpha = 0.5f) else colors.surfaceVariant,
                                        border = BorderStroke(0.6.dp, if (isSelected) colors.goldPrimary else colors.border),
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable {
                                                bullionKaratStr = k.toString()
                                                onBullionSettlementChange(bullionWeight, k, bullionAngNumber)
                                            }
                                    ) {
                                        Box(
                                            modifier = Modifier.padding(vertical = 5.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = label,
                                                fontSize = 10.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isSelected) colors.goldPrimary else colors.textSecondary,
                                                fontFamily = VazirmatnFamily
                                            )
                                        }
                                    }
                                }
                            }

                            // Ang Number Input
                            GoldInputField(
                                value = bullionAngNumber,
                                onValueChange = {
                                    bullionAngNumber = it
                                    onBullionSettlementChange(bullionWeight, bullionKarat, it)
                                },
                                label = "شماره انگ و نام آزمایشگاه ری‌گیری",
                                trailingText = "کد انگ",
                                keyboardType = KeyboardType.Text,
                                modifier = Modifier.fillMaxWidth()
                            )

                            // Bullion Valuation Breakdown Card
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = colors.surfaceVariant,
                                border = BorderStroke(0.6.dp, colors.goldBorder.copy(alpha = 0.4f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "ارزش ریالی طلای تحویلی:",
                                            fontSize = 11.sp,
                                            color = colors.textSecondary,
                                            fontFamily = VazirmatnFamily
                                        )
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            AnimatedPriceText(
                                                amount = bullionValuation,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = colors.goldPrimary
                                            )
                                            Text(
                                                text = " تومان",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = colors.goldPrimary,
                                                fontFamily = VazirmatnFamily
                                            )
                                        }
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "معادل وزنی ۱۸ عیار:",
                                            fontSize = 10.5.sp,
                                            color = colors.textSecondary,
                                            fontFamily = VazirmatnFamily
                                        )
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            AnimatedNumberText(
                                                text = PersianNumberFormatter.formatWeight(bullion18kEq),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = colors.textMain
                                            )
                                            Text(
                                                text = " گرم",
                                                fontSize = 10.5.sp,
                                                color = colors.textSecondary,
                                                fontFamily = VazirmatnFamily
                                            )
                                        }
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "باقیمانده تراز پس از کسر شمش:",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = colors.textSecondary,
                                            fontFamily = VazirmatnFamily
                                        )
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            val absBullionDiff = if (bullionRemainingDiff < 0L) -bullionRemainingDiff else bullionRemainingDiff
                                            AnimatedPriceText(
                                                amount = absBullionDiff,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (bullionRemainingDiff == 0L) colors.profitGreen else colors.errorRed
                                            )
                                            Text(
                                                text = if (bullionRemainingDiff == 0L) " (تسویه کامل)" else " تومان",
                                                fontSize = 10.5.sp,
                                                color = if (bullionRemainingDiff == 0L) colors.profitGreen else colors.errorRed,
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

            // Quick Fill Remaining Balance Button
            if (remainingBalance > 0L) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = colors.goldContainer.copy(alpha = 0.4f),
                    border = BorderStroke(0.8.dp, colors.goldBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            when (selectedMethod) {
                                SettlementMethod.POS -> {
                                    posStr = remainingBalance.toString()
                                    onCashPosAmountChange(remainingBalance)
                                }
                                SettlementMethod.LEDGER -> {
                                    ledgerStr = remainingBalance.toString()
                                    onLedgerAmountChange(remainingBalance)
                                }
                                SettlementMethod.TRANSFER -> {
                                    if (!transferIsGoldMode) {
                                        transferAmountStr = remainingBalance.toString()
                                        transferWeightStr = ""
                                        onThirdPartyTransferChange(thirdPartyCustomer, thirdPartyInvoiceId, thirdPartyInvoiceNumber, 0.0, remainingBalance, transferTrackingCode)
                                    } else {
                                        val w = if (invoice.spotPrice18k > 0) remainingBalance.toDouble() / invoice.spotPrice18k else 0.0
                                        transferWeightStr = String.format(Locale.US, "%.3f", w)
                                        transferAmountStr = ""
                                        onThirdPartyTransferChange(thirdPartyCustomer, thirdPartyInvoiceId, thirdPartyInvoiceNumber, w, 0L, transferTrackingCode)
                                    }
                                }
                                SettlementMethod.BULLION -> {
                                    if (invoice.spotPrice18k > 0) {
                                        val w = remainingBalance.toDouble() / invoice.spotPrice18k
                                        bullionWeightStr = String.format(Locale.US, "%.3f", w)
                                        onBullionSettlementChange(w, bullionKarat, bullionAngNumber)
                                    }
                                }
                            }
                        }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "⚡ تنظیم این روش با باقیمانده مانده صافی:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = colors.textMain,
                            fontFamily = VazirmatnFamily
                        )
                        Text(
                            text = "${PersianNumberFormatter.formatTomans(remainingBalance)} تومان",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.goldPrimary,
                            fontFamily = VazirmatnFamily
                        )
                    }
                }
            }

            // Button: Add to Payments List
            GoldButton(
                text = "افزودن این مرحله پرداخت به لیست",
                onClick = {
                    val newPayment = when (selectedMethod) {
                        SettlementMethod.POS -> {
                            val amt = posStr.toLongOrNull() ?: 0L
                            if (amt > 0) {
                                SettlementPaymentItem(
                                    id = UUID.randomUUID().toString(),
                                    method = SettlementMethod.POS,
                                    amountTomans = amt,
                                    trackingCode = trackingCode
                                )
                            } else null
                        }
                        SettlementMethod.LEDGER -> {
                            val amt = ledgerStr.toLongOrNull() ?: 0L
                            if (amt > 0) {
                                SettlementPaymentItem(
                                    id = UUID.randomUUID().toString(),
                                    method = SettlementMethod.LEDGER,
                                    amountTomans = amt,
                                    description = "موعد: $ledgerDueDate"
                                )
                            } else null
                        }
                        SettlementMethod.BULLION -> {
                            val w = bullionWeightStr.toDoubleOrNull() ?: 0.0
                            if (w > 0.0 || bullionValuation > 0L) {
                                SettlementPaymentItem(
                                    id = UUID.randomUUID().toString(),
                                    method = SettlementMethod.BULLION,
                                    amountTomans = bullionValuation,
                                    goldWeight18k = bullion18kEq,
                                    bullionKarat = bullionKarat,
                                    bullionAngNumber = bullionAngNumber
                                )
                            } else null
                        }
                        SettlementMethod.TRANSFER -> {
                            val amt = if (!transferIsGoldMode) (transferAmountStr.toLongOrNull() ?: 0L) else 0L
                            val w = if (transferIsGoldMode) (transferWeightStr.toDoubleOrNull() ?: 0.0) else 0.0
                            if (amt > 0L || w > 0.0) {
                                SettlementPaymentItem(
                                    id = UUID.randomUUID().toString(),
                                    method = SettlementMethod.TRANSFER,
                                    amountTomans = amt,
                                    goldWeight18k = w,
                                    trackingCode = transferTrackingCode,
                                    thirdPartyCustomerName = thirdPartyCustomer?.name ?: ""
                                )
                            } else null
                        }
                    }
                    if (newPayment != null) {
                        paymentsList = paymentsList + newPayment
                        posStr = ""
                        trackingCode = ""
                        ledgerStr = ""
                        bullionWeightStr = ""
                        transferAmountStr = ""
                        transferWeightStr = ""
                    }
                },
                icon = InvoicePlusVector,
                isSecondary = true,
                modifier = Modifier.fillMaxWidth()
            )

            // Notes field using GoldInputField
            GoldInputField(
                value = note,
                onValueChange = onNoteChange,
                label = "توضیحات و شرایط تحویل",
                trailingText = "اختیاری",
                keyboardType = KeyboardType.Text,
                modifier = Modifier.fillMaxWidth()
            )

            // Auto Sync with Customer Ledger Card
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = colors.surfaceElevated,
                border = BorderStroke(0.6.dp, colors.goldBorder.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                        Text(
                            text = "ثبت خودکار در دفتر معین و صورت‌حساب مشتری",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textMain,
                            fontFamily = VazirmatnFamily
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        val hint = if (invoice.customerRole == CustomerRole.WHOLESALER) {
                            "مانده وزنی طلای ۱۸ عیار این فاکتور به تراز دفتری همکار اضافه خواهد شد"
                        } else {
                            "مانده ریالی پرداخت‌نشده این فاکتور به بدهکاری مشتری منظور خواهد شد"
                        }
                        Text(
                            text = hint,
                            fontSize = 10.5.sp,
                            color = colors.textMuted,
                            fontFamily = VazirmatnFamily
                        )
                    }
                    Switch(
                        checked = syncWithLedger,
                        onCheckedChange = onSyncWithLedgerChange,
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

        // Sticky Footer per dialog button layout invariant
        HorizontalDivider(color = colors.border, thickness = 0.6.dp)
        Surface(
            color = colors.surfaceElevated.copy(alpha = 0.4f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // RTL: Secondary button on RIGHT (first child in Row)
                GoldButton(
                    text = "انصراف",
                    onClick = onDismiss,
                    isSecondary = true,
                    modifier = Modifier.weight(1f)
                )
                // RTL: Primary button on LEFT (second child in Row)
                GoldButton(
                    text = "تایید و ثبت روش تسویه",
                    onClick = {
                        val finalPayments = if (paymentsList.isEmpty()) {
                            val single = when (selectedMethod) {
                                SettlementMethod.POS -> {
                                    val amt = posStr.toLongOrNull() ?: 0L
                                    if (amt > 0) listOf(SettlementPaymentItem(method = SettlementMethod.POS, amountTomans = amt, trackingCode = trackingCode))
                                    else emptyList()
                                }
                                SettlementMethod.LEDGER -> {
                                    val amt = ledgerStr.toLongOrNull() ?: 0L
                                    if (amt > 0) listOf(SettlementPaymentItem(method = SettlementMethod.LEDGER, amountTomans = amt, description = "موعد: $ledgerDueDate"))
                                    else emptyList()
                                }
                                SettlementMethod.BULLION -> {
                                    val w = bullionWeightStr.toDoubleOrNull() ?: 0.0
                                    if (w > 0.0 || bullionValuation > 0L) listOf(SettlementPaymentItem(method = SettlementMethod.BULLION, amountTomans = bullionValuation, goldWeight18k = bullion18kEq, bullionKarat = bullionKarat, bullionAngNumber = bullionAngNumber))
                                    else emptyList()
                                }
                                SettlementMethod.TRANSFER -> {
                                    val amt = if (!transferIsGoldMode) (transferAmountStr.toLongOrNull() ?: 0L) else 0L
                                    val w = if (transferIsGoldMode) (transferWeightStr.toDoubleOrNull() ?: 0.0) else 0.0
                                    if (amt > 0L || w > 0.0) listOf(SettlementPaymentItem(method = SettlementMethod.TRANSFER, amountTomans = amt, goldWeight18k = w, trackingCode = transferTrackingCode, thirdPartyCustomerName = thirdPartyCustomer?.name ?: ""))
                                    else emptyList()
                                }
                            }
                            single
                        } else {
                            paymentsList
                        }
                        onSettlementPaymentsChange(finalPayments)
                        onDismiss()
                    },
                    icon = InvoiceCheckVector,
                    modifier = Modifier.weight(1.5f)
                )
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Third-Party Barter Ledger Picker Dialog
// ---------------------------------------------------------------------------
@Composable
private fun ThirdPartyBarterPickerDialog(
    customers: List<Customer>,
    invoices: List<InvoiceListItem>,
    currentInvoiceId: String,
    onSelectInvoice: (Customer, InvoiceListItem) -> Unit,
    onSelectCustomerLedger: (Customer) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalGoldExColors.current
    var searchQuery by remember { mutableStateOf("") }

    val openInvoices = remember(invoices, currentInvoiceId, searchQuery) {
        val list = invoices.filterNot { it.id == currentInvoiceId }
        val q = searchQuery.trim().lowercase()
        if (q.isBlank()) list
        else list.filter {
            it.customerName.lowercase().contains(q) ||
            it.invoiceNumber.lowercase().contains(q) ||
            it.itemsSummary.lowercase().contains(q)
        }
    }

    val filteredCustomers = remember(customers, searchQuery) {
        val q = searchQuery.trim().lowercase()
        if (q.isBlank()) customers
        else customers.filter {
            it.name.lowercase().contains(q) ||
            it.phone.contains(q) ||
            it.note.lowercase().contains(q)
        }
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Dialog(
            onDismissRequest = onDismiss,
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = colors.surface,
                border = BorderStroke(0.8.dp, colors.goldBorder),
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .fillMaxHeight(0.82f)
                    .padding(vertical = 16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(colors.goldContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = CustomerIconVector,
                                    contentDescription = null,
                                    tint = colors.goldPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "انتخاب طرف حساب و فاکتور تهاتر",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.textMain,
                                    fontFamily = VazirmatnFamily
                                )
                                Text(
                                    text = "انتقال تعهد وزنی مانده به همکار یا شخص ثالث",
                                    fontSize = 10.sp,
                                    color = colors.textMuted,
                                    fontFamily = VazirmatnFamily
                                )
                            }
                        }

                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = InvoiceCloseVector,
                                contentDescription = "بستن",
                                tint = colors.textSecondary
                            )
                        }
                    }

                    // Search field
                    GoldInputField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        label = "جستجو بر اساس نام همکار، شماره فاکتور یا اقلام...",
                        trailingText = "جستجو",
                        keyboardType = KeyboardType.Text,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Scrollable content
                    androidx.compose.foundation.lazy.LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (openInvoices.isNotEmpty()) {
                            item {
                                Text(
                                    text = "فاکتورهای باز همکاران جهت تهاتر:",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.goldPrimary,
                                    fontFamily = VazirmatnFamily,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                            }
                            items(openInvoices.size) { idx ->
                                val inv = openInvoices[idx]
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = colors.surfaceVariant,
                                    border = BorderStroke(0.6.dp, colors.goldBorder.copy(alpha = 0.6f)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            val cust = customers.firstOrNull { it.name == inv.customerName }
                                                ?: Customer(name = inv.customerName)
                                            onSelectInvoice(cust, inv)
                                            onDismiss()
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .clip(CircleShape)
                                                    .background(colors.goldContainer),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = inv.customerInitials.ifBlank { "هم" },
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = colors.goldPrimary,
                                                    fontFamily = VazirmatnFamily
                                                )
                                            }
                                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                                Text(
                                                    text = "${inv.customerName} • ${inv.invoiceNumber}",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = colors.textMain,
                                                    fontFamily = VazirmatnFamily
                                                )
                                                Text(
                                                    text = inv.itemsSummary,
                                                    fontSize = 10.sp,
                                                    color = colors.textSecondary,
                                                    fontFamily = VazirmatnFamily,
                                                    maxLines = 1
                                                )
                                                Text(
                                                    text = "${inv.line1Detail} • ${PersianNumberFormatter.formatPrice(inv.finalAmount.toDouble())} ت",
                                                    fontSize = 10.sp,
                                                    color = colors.goldPrimary,
                                                    fontFamily = VazirmatnFamily
                                                )
                                            }
                                        }

                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = colors.goldContainer.copy(alpha = 0.4f)
                                        ) {
                                            Text(
                                                text = "انتخاب فاکتور",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = colors.goldPrimary,
                                                fontFamily = VazirmatnFamily,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        if (filteredCustomers.isNotEmpty()) {
                            item {
                                Text(
                                    text = "دفتر مشتریان و همکاران (تهاتر حساب دفتری):",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.textSecondary,
                                    fontFamily = VazirmatnFamily,
                                    modifier = Modifier.padding(top = 10.dp, bottom = 4.dp)
                                )
                            }
                            items(filteredCustomers.size) { idx ->
                                val cust = filteredCustomers[idx]
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = colors.surfaceVariant.copy(alpha = 0.6f),
                                    border = BorderStroke(0.6.dp, colors.border),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            onSelectCustomerLedger(cust)
                                            onDismiss()
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(34.dp)
                                                    .clip(CircleShape)
                                                    .background(colors.surfaceElevated),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = cust.name.firstOrNull()?.toString() ?: "م",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = colors.textMain,
                                                    fontFamily = VazirmatnFamily
                                                )
                                            }
                                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                                Text(
                                                    text = cust.name,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = colors.textMain,
                                                    fontFamily = VazirmatnFamily
                                                )
                                                Text(
                                                    text = cust.note.ifBlank { cust.phone }.ifBlank { "حساب همکار" },
                                                    fontSize = 10.sp,
                                                    color = colors.textSecondary,
                                                    fontFamily = VazirmatnFamily
                                                )
                                            }
                                        }

                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = colors.surfaceElevated
                                        ) {
                                            Text(
                                                text = "حساب دفتری",
                                                fontSize = 10.sp,
                                                color = colors.textSecondary,
                                                fontFamily = VazirmatnFamily,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Bottom Cancel Button
                    GoldButton(
                        text = "انصراف",
                        onClick = onDismiss,
                        isSecondary = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Rate Edit Dialog
// ---------------------------------------------------------------------------
@Composable
private fun EditRateDialog(
    currentRate: Long,
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit
) {
    val colors = LocalGoldExColors.current
    var rateStr by remember { mutableStateOf(currentRate.toString()) }
    val liveRate = GoldMarketRepository.rates.value.gold18

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
