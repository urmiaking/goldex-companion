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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import com.goldex.companion.model.MeltGoldItem
import com.goldex.companion.model.PersianNumberFormatter
import com.goldex.companion.model.ScrapGoldItem
import com.goldex.companion.model.SettlementMethod
import com.goldex.companion.ui.components.GoldButton
import com.goldex.companion.ui.components.GoldInputField
import com.goldex.companion.ui.components.LuxurySegmentedControl
import com.goldex.companion.ui.invoices.components.InvoiceCheckVector
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

@Composable
fun BarterInvoiceScreen(
    uiState: BarterInvoiceUiState,
    onSetCustomerRole: (CustomerRole) -> Unit,
    onSetSettlementMethod: (SettlementMethod) -> Unit,
    onSetCashPosAmount: (Long) -> Unit,
    onSetLedgerAmount: (Long) -> Unit = {},
    onSetPosTrackingCode: (String) -> Unit = {},
    onSetLedgerDueDate: (String) -> Unit = {},
    onSetBullionSettlement: (Double, Int, String) -> Unit = { _, _, _ -> },
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
    modifier: Modifier = Modifier
) {
    val colors = LocalGoldExColors.current
    val invoice = uiState.invoice
    val balance = uiState.balance

    val dateSolar = remember(invoice.createdAt) {
        // Formatted timestamp
        SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()).format(Date(invoice.createdAt))
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 0. Optional Back Navigation Header to Invoices List
            if (onNavigateBack != null) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = colors.surface,
                    border = BorderStroke(1.dp, colors.border),
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
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(colors.surfaceElevated)
                                    .border(1.dp, colors.border, RoundedCornerShape(10.dp))
                                    .clickable(onClick = onNavigateBack),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "→",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.textMain
                                )
                            }
                            Column {
                                Text(
                                    text = "فاکتور جامع دوطرفه و تهاتر زرگری",
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.textMain,
                                    fontFamily = VazirmatnFamily
                                )
                                Text(
                                    text = "بازگشت به فهرست معاملات و فاکتورها",
                                    fontSize = 10.sp,
                                    color = colors.textMuted,
                                    fontFamily = VazirmatnFamily
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(Color(0x26DFB35A))
                                .border(0.8.dp, Color(0x4DDFB35A), RoundedCornerShape(50))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "کد: ${PersianNumberFormatter.toPersianDigits(invoice.invoiceNumber)}",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.goldPrimary,
                                fontFamily = VazirmatnFamily
                            )
                        }
                    }
                }
            }

            // 1. Meta & Live Gold Rate Bar
            InvoiceMetaAndRateCard(
                invoiceNumber = invoice.invoiceNumber,
                dateSolar = dateSolar,
                spotPrice18k = invoice.spotPrice18k,
                onEditRateClick = { onSetRateEditDialogVisible(true) }
            )

            // 2. Customer & Account Card
            CustomerAndAccountCard(
                customer = invoice.customer,
                customerRole = invoice.customerRole,
                onRoleChange = onSetCustomerRole,
                onChangeCustomerClick = onOpenCustomerPicker
            )

            // 3. Barter Balance & Net Settlement Overview
            BarterBalanceCard(
                balance = balance,
                spotPrice18k = invoice.spotPrice18k
            )

            // 4. Sales Items Section (اقلام فروش ما)
            ItemsSectionCard(
                sectionNumber = 1,
                title = "اقلام فروش ما (تحویلی)",
                items = invoice.salesItems,
                onAddItemClick = { onOpenAddItemModal(InvoiceItemCategory.CRAFTED, true) },
                onEditItemClick = { onOpenEditItemModal(it, true) },
                onDeleteItemClick = onDeleteSalesItem,
                isSales = true
            )

            // 5. Received Items Section (اقلام دریافتی تهاتر)
            ItemsSectionCard(
                sectionNumber = 2,
                title = "اقلام دریافتی / تهاتر (از مشتری)",
                items = invoice.receivedItems,
                onAddItemClick = { onOpenAddItemModal(InvoiceItemCategory.SCRAP, false) },
                onEditItemClick = { onOpenEditItemModal(it, false) },
                onDeleteItemClick = onDeleteReceivedItem,
                isSales = false
            )

            // 6. Payment & Settlement Methods
            SettlementCard(
                invoice = invoice,
                balance = balance,
                onMethodSelect = onSetSettlementMethod,
                onCashPosAmountChange = onSetCashPosAmount,
                onLedgerAmountChange = onSetLedgerAmount,
                onPosTrackingCodeChange = onSetPosTrackingCode,
                onLedgerDueDateChange = onSetLedgerDueDate,
                onBullionSettlementChange = onSetBullionSettlement,
                note = invoice.note,
                onNoteChange = onSetNote
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

            Spacer(modifier = Modifier.height(18.dp))
        }

        // Add / Edit Item Modal
        if (uiState.isItemModalVisible) {
            AddInvoiceItemModal(
                spotPrice18k = invoice.spotPrice18k,
                defaultCategory = uiState.targetCategory,
                existingItem = uiState.editingItem,
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
        color = colors.surfaceElevated,
        border = BorderStroke(1.dp, colors.border),
        shadowElevation = 1.5.dp,
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
                        shape = RoundedCornerShape(8.dp),
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
    onRoleChange: (CustomerRole) -> Unit,
    onChangeCustomerClick: () -> Unit
) {
    val colors = LocalGoldExColors.current

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = colors.surfaceElevated,
        border = BorderStroke(1.dp, colors.border),
        shadowElevation = 1.5.dp,
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
                        text = "مشخصات طرف معامله و حساب",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textMain,
                        fontFamily = VazirmatnFamily
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = colors.surfaceVariant,
                    border = BorderStroke(0.6.dp, colors.border),
                    modifier = Modifier.clickable { onChangeCustomerClick() }
                ) {
                    Text(
                        text = "دفتر معین",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.goldPrimary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.5.dp),
                        fontFamily = VazirmatnFamily
                    )
                }
            }

            // Role Animated Segmented Control
            LuxurySegmentedControl(
                items = CustomerRole.entries,
                selectedItem = customerRole,
                onItemSelected = onRoleChange,
                label = { it.titleFa },
                modifier = Modifier.fillMaxWidth(),
                height = 36.dp,
                fontSize = 11.sp
            )

            // Customer Details Card
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = colors.surfaceVariant.copy(alpha = 0.6f),
                border = BorderStroke(0.8.dp, colors.goldBorder),
                modifier = Modifier.fillMaxWidth()
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
                                .size(38.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(colors.goldContainer.copy(alpha = 0.5f))
                                .border(1.dp, colors.goldBorder, RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = customer?.name?.take(2) ?: "طرف",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Black,
                                color = colors.goldPrimary,
                                fontFamily = VazirmatnFamily
                            )
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = customer?.name ?: "انتخاب مشتری یا همکار...",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textMain,
                                fontFamily = VazirmatnFamily
                            )
                            Text(
                                text = customer?.note?.ifBlank { customer.phone } ?: "بدون مانده حسابی قبلی",
                                fontSize = 10.sp,
                                color = colors.textSecondary,
                                fontFamily = VazirmatnFamily
                            )
                        }
                    }

                    Text(
                        text = "تغییر",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.goldPrimary,
                        modifier = Modifier
                            .clickable { onChangeCustomerClick() }
                            .padding(4.dp),
                        fontFamily = VazirmatnFamily
                    )
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
    spotPrice18k: Long
) {
    val colors = LocalGoldExColors.current

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = colors.surfaceElevated,
        border = BorderStroke(1.2.dp, colors.goldBorder),
        shadowElevation = 2.dp,
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
                    color = colors.goldContainer.copy(alpha = 0.35f),
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
                                text = "g",
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
                    color = colors.surfaceVariant,
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
                                text = "g",
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
        color = colors.surfaceElevated,
        border = BorderStroke(1.dp, colors.border),
        shadowElevation = 1.5.dp,
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
                            .background(if (isSales) colors.goldContainer else colors.surfaceVariant),
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
                    shape = RoundedCornerShape(8.dp),
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
        color = colors.surfaceVariant.copy(alpha = 0.5f),
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
                            is CraftedGoldItem -> "عیار ${item.karat.labelFa} • ناخالص: ${PersianNumberFormatter.formatWeight(item.grossWeight)}g • خالص: ${PersianNumberFormatter.formatWeight(item.netWeight)}g"
                            is ScrapGoldItem -> "عیار مبنا: ${item.baseKarat} ← خالص: ${item.payableKarat} • وزن: ${PersianNumberFormatter.formatWeight(item.netWeight)}g"
                            is MeltGoldItem -> "عیار خطی: ${item.labKarat} • انگ: ${item.angNumber.ifBlank { "-" }} • وزن: ${PersianNumberFormatter.formatWeight(item.weight)}g"
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
                        text = "g",
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
// 6. Payment & Settlement Methods
// ---------------------------------------------------------------------------
@Composable
private fun SettlementCard(
    invoice: BarterInvoice,
    balance: BarterBalance,
    onMethodSelect: (SettlementMethod) -> Unit,
    onCashPosAmountChange: (Long) -> Unit,
    onLedgerAmountChange: (Long) -> Unit,
    onPosTrackingCodeChange: (String) -> Unit,
    onLedgerDueDateChange: (String) -> Unit,
    onBullionSettlementChange: (Double, Int, String) -> Unit,
    note: String,
    onNoteChange: (String) -> Unit
) {
    val colors = LocalGoldExColors.current
    val netPayableAmount: Double = balance.netPayableAmount
    val selectedMethod: SettlementMethod = invoice.settlementMethod

    var posStr by remember(invoice.cashPosAmount) {
        mutableStateOf(if (invoice.cashPosAmount > 0) invoice.cashPosAmount.toString() else "")
    }
    var trackingCode by remember(invoice.posTrackingCode) {
        mutableStateOf(invoice.posTrackingCode)
    }

    val absNetPayableLong: Long = kotlin.math.abs(netPayableAmount).toLong()

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

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = colors.surfaceElevated,
        border = BorderStroke(1.dp, colors.border),
        shadowElevation = 1.5.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
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
                                    label = "مبلغ پرداختی نقدی / پوز",
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
                                    label = "شماره پیگیری / ارجاع",
                                    trailingText = "کد ارجاع",
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

                    SettlementMethod.LEDGER -> {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
                                    label = "مبلغ انتقالی به دفتر معین",
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
                                    label = "وزن شمش / آبشده تحویلی",
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

            // Notes field using GoldInputField
            GoldInputField(
                value = note,
                onValueChange = onNoteChange,
                label = "توضیحات و شرایط تحویل",
                trailingText = "اختیاری",
                keyboardType = KeyboardType.Text,
                modifier = Modifier.fillMaxWidth()
            )
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
