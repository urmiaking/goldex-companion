package com.goldex.companion.ui.customers.modals

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.goldex.companion.model.LedgerEntryType
import com.goldex.companion.model.PersianNumberFormatter
import com.goldex.companion.ui.components.AnimatedNumberText
import com.goldex.companion.ui.components.AnimatedPriceText
import com.goldex.companion.ui.components.GoldButton
import com.goldex.companion.ui.components.GoldInputField
import com.goldex.companion.ui.customers.CustomerSettlementUiState
import com.goldex.companion.ui.customers.CustomerSettlementViewModel
import com.goldex.companion.ui.invoices.components.InvoiceCheckVector
import com.goldex.companion.ui.invoices.components.InvoiceCloseVector
import com.goldex.companion.ui.theme.ButtonShape
import com.goldex.companion.ui.theme.LocalGoldExColors
import com.goldex.companion.ui.theme.VazirmatnFamily
import kotlin.math.abs

/**
 * Customer Settlement Modal matching Stitch Design Screen (ID: d17ef17485a740a0962fe7adc89353d2)
 * Follows the Persian Sovereign Aurum visual system: rounded cards, gold gradient pills,
 * clear typography, ergonomic mobile handles, and RTL-compliant action ordering.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerSettlementModal(
    state: CustomerSettlementUiState,
    actions: CustomerSettlementViewModel,
    onConfirm: () -> Unit,
    onIndependentEntry: () -> Unit
) {
    val colors = LocalGoldExColors.current
    val customer = state.customer ?: return
    val preview = state.preview
    var scopeMenu by remember { mutableStateOf(false) }
    var methodMenu by remember { mutableStateOf(false) }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        ModalBottomSheet(
            onDismissRequest = actions::close,
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
            containerColor = colors.surface,
            dragHandle = null
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.94f)
                    .imePadding()
            ) {
                // Drag Handle for Ergonomic Mobile Gestures (Stitch Header)
                Box(
                    modifier = Modifier
                        .padding(top = 12.dp, bottom = 6.dp)
                        .size(width = 48.dp, height = 5.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFDFC293).copy(alpha = 0.8f))
                        .align(Alignment.CenterHorizontally)
                )

                // Header
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colors.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "تسویه حساب ${customer.name}",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textMain,
                                fontFamily = VazirmatnFamily
                            )
                            Text(
                                text = "پرداخت را به ماندهٔ موردنظر اختصاص دهید؛ تبدیل با نرخ همین سند ثبت می‌شود.",
                                fontSize = 11.5.sp,
                                lineHeight = 17.sp,
                                color = colors.textSecondary,
                                fontFamily = VazirmatnFamily
                            )
                        }

                        IconButton(
                            onClick = actions::close,
                            modifier = Modifier
                                .padding(start = 8.dp)
                                .size(34.dp)
                                .clip(ButtonShape)
                                .background(colors.surfaceElevated)
                                .border(0.8.dp, colors.border, ButtonShape)
                        ) {
                            Icon(
                                imageVector = InvoiceCloseVector,
                                contentDescription = "بستن",
                                tint = colors.textSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    HorizontalDivider(
                        color = colors.border.copy(alpha = 0.5f),
                        thickness = 0.7.dp,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                // Scrollable Body Content
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // 1. Settlement Basis / Factor Reference Capsule
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "مبنای تسویه و اختصاص پرداخت:",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.textSecondary,
                            fontFamily = VazirmatnFamily
                        )

                        Box {
                            val currentScopeLabel = state.invoices.firstOrNull { it.id == state.invoiceId }?.let {
                                "بابت فاکتور ${PersianNumberFormatter.toPersianDigits(it.number)} (پیش‌فرض)"
                            } ?: if (state.invoiceId == null) "بابت تسویه حساب کل مشتری (معین کلی)" else "فاکتور تسویه شده یا در دسترس نیست"

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = colors.surfaceElevated,
                                border = BorderStroke(1.dp, colors.goldBorder.copy(alpha = 0.6f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { scopeMenu = true }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = currentScopeLabel,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = colors.textMain,
                                        fontFamily = VazirmatnFamily
                                    )
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = null,
                                        tint = colors.goldPrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            DropdownMenu(
                                expanded = scopeMenu,
                                onDismissRequest = { scopeMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("بابت تسویه حساب کل مشتری (معین کلی)", fontFamily = VazirmatnFamily) },
                                    onClick = { scopeMenu = false; actions.selectInvoice(null) }
                                )
                                state.invoices.forEach { invoice ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                "بابت فاکتور ${PersianNumberFormatter.toPersianDigits(invoice.number)}",
                                                fontFamily = VazirmatnFamily
                                            )
                                        },
                                        onClick = { scopeMenu = false; actions.selectInvoice(invoice.id) }
                                    )
                                }
                            }
                        }
                    }

                    // 2. Selected Balance Summary Card (Stitch: bg-white/80 border-aurum-200)
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = colors.surfaceElevated,
                        border = BorderStroke(1.dp, colors.goldBorder.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(9.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "ماندهٔ انتخاب‌شده",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.goldPrimary,
                                    fontFamily = VazirmatnFamily
                                )
                                val isReceiving = (if (state.targetType == LedgerEntryType.GOLD_WEIGHT) state.scope.goldGrams else state.scope.cashTomans.toDouble()) >= 0
                                Text(
                                    text = if (isReceiving) "دریافت از مشتری" else "پرداخت به مشتری",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isReceiving) Color(0xFFD97706) else Color(0xFF10B981),
                                    fontFamily = VazirmatnFamily
                                )
                            }

                            // Gold Balance Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "طلا:",
                                    fontSize = 12.5.sp,
                                    color = colors.textSecondary,
                                    fontFamily = VazirmatnFamily
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    AnimatedNumberText(
                                        text = PersianNumberFormatter.formatAccountWeight(abs(state.scope.goldGrams)),
                                        unit = "گرم",
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (abs(state.scope.goldGrams) < 1e-10) colors.textMuted else colors.textMain
                                    )
                                    val goldStatus = if (abs(state.scope.goldGrams) < 1e-10) "تسویه" else if (state.scope.goldGrams > 1e-10) "بدهکار" else "بستانکار"
                                    val goldColor = if (abs(state.scope.goldGrams) < 1e-10) Color(0xFF10B981) else if (state.scope.goldGrams > 1e-10) Color(0xFFD97706) else Color(0xFF10B981)
                                    Text(
                                        text = goldStatus,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = goldColor,
                                        fontFamily = VazirmatnFamily
                                    )
                                }
                            }

                            HorizontalDivider(color = colors.border.copy(alpha = 0.5f), thickness = 0.6.dp)

                            // Cash Balance Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "پول:",
                                    fontSize = 12.5.sp,
                                    color = colors.textSecondary,
                                    fontFamily = VazirmatnFamily
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    AnimatedPriceText(
                                        amount = abs(state.scope.cashTomans),
                                        unit = "تومان",
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (state.scope.cashTomans == 0L) colors.textMuted else colors.textMain
                                    )
                                    val cashStatus = if (state.scope.cashTomans == 0L) "تسویه" else if (state.scope.cashTomans > 0) "بدهکار" else "بستانکار"
                                    val cashColor = if (state.scope.cashTomans == 0L) Color(0xFF10B981) else if (state.scope.cashTomans > 0) Color(0xFFD97706) else Color(0xFF10B981)
                                    Text(
                                        text = cashStatus,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = cashColor,
                                        fontFamily = VazirmatnFamily
                                    )
                                }
                            }
                        }
                    }

                    // 3. Which Balance to Settle? (Stitch: 2-column grid button segment)
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "کدام مانده تسویه شود؟",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textMain,
                            fontFamily = VazirmatnFamily
                        )
                        UnitChoices(
                            selected = state.targetType,
                            onSelect = actions::selectTarget,
                            goldEnabled = abs(state.scope.goldGrams) > 1e-10,
                            cashEnabled = state.scope.cashTomans != 0L
                        )
                    }

                    // 4. Barter / Offset Switch (if available)
                    if (state.canOffset) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = colors.surfaceElevated,
                            border = BorderStroke(1.dp, colors.border),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(end = 8.dp),
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    val isReceiving = (if (state.targetType == LedgerEntryType.GOLD_WEIGHT) state.scope.goldGrams else state.scope.cashTomans.toDouble()) >= 0
                                    Text(
                                        text = if (isReceiving) "دریافت از مشتری" else "پرداخت به مشتری",
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.goldPrimary,
                                        fontFamily = VazirmatnFamily
                                    )
                                    Text(
                                        text = "تهاتر با ماندهٔ مقابل (بدون دریافت جدید)",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = colors.textMain,
                                        fontFamily = VazirmatnFamily
                                    )
                                }
                                Switch(
                                    checked = state.offsetExistingCredit,
                                    onCheckedChange = actions::setOffset,
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color(0xFFB89456),
                                        checkedTrackColor = Color(0xFFDFC293)
                                    )
                                )
                            }
                        }
                    }

                    // 5. Payment Method / Offset Unit
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = if (state.offsetExistingCredit) "واحد بستانکاری برای تهاتر" else "روش پرداخت",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textMain,
                            fontFamily = VazirmatnFamily
                        )
                        UnitChoices(
                            selected = state.paymentType,
                            onSelect = actions::selectPayment,
                            enabled = !state.offsetExistingCredit
                        )
                    }

                    // 6. Rate Selection (when needsRate)
                    if (state.needsRate) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = colors.surfaceElevated,
                            border = BorderStroke(1.dp, colors.goldBorder.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "نرخ هر گرم طلای ۱۸ عیار",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.textMain,
                                        fontFamily = VazirmatnFamily
                                    )

                                    // Mini Segment Pill (Market vs Custom Rate)
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = colors.surfaceVariant,
                                        border = BorderStroke(0.8.dp, colors.border),
                                        modifier = Modifier.clip(RoundedCornerShape(8.dp))
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(2.dp),
                                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                                        ) {
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = if (state.isMarketRate) colors.surface else Color.Transparent,
                                                border = if (state.isMarketRate) BorderStroke(0.6.dp, colors.goldBorder.copy(alpha = 0.5f)) else null,
                                                shadowElevation = if (state.isMarketRate) 1.dp else 0.dp,
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .clickable { actions.useMarketRate() }
                                            ) {
                                                Text(
                                                    text = "نرخ بازار",
                                                    fontFamily = VazirmatnFamily,
                                                    fontSize = 11.sp,
                                                    fontWeight = if (state.isMarketRate) FontWeight.Bold else FontWeight.Medium,
                                                    color = if (state.isMarketRate) colors.goldPrimary else colors.textMuted,
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                                )
                                            }

                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = if (!state.isMarketRate) colors.surface else Color.Transparent,
                                                border = if (!state.isMarketRate) BorderStroke(0.6.dp, colors.goldBorder.copy(alpha = 0.5f)) else null,
                                                shadowElevation = if (!state.isMarketRate) 1.dp else 0.dp,
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .clickable { actions.useCustomRate() }
                                            ) {
                                                Text(
                                                    text = "نرخ دلخواه",
                                                    fontFamily = VazirmatnFamily,
                                                    fontSize = 11.sp,
                                                    fontWeight = if (!state.isMarketRate) FontWeight.Bold else FontWeight.Medium,
                                                    color = if (!state.isMarketRate) colors.goldPrimary else colors.textMuted,
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                GoldInputField(
                                    value = state.rateInput,
                                    onValueChange = actions::setRate,
                                    label = "نرخ تبدیل (تومان / گرم)",
                                    trailingText = "تومان",
                                    enabled = !state.isMarketRate,
                                    modifier = Modifier.testTag("settlementRate")
                                )

                                Text(
                                    text = if (state.isMarketRate) "${state.rateSource} • ساعت ${PersianNumberFormatter.toPersianDigits(state.rateObservedAt)}؛ نرخ تا ثبت ثابت می‌ماند"
                                    else if (!state.marketRateAvailable) "نرخ زنده در دسترس نیست؛ نرخ توافق‌شده را وارد کنید."
                                    else "نرخ توافق‌شدهٔ این تسویه؛ نرخ فاکتور قبلی تغییر نمی‌کند.",
                                    color = colors.textMuted,
                                    fontFamily = VazirmatnFamily,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }

                    // 7. Inputs based on paymentType (Cash or Gold)
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = colors.surfaceElevated,
                        border = BorderStroke(1.dp, colors.border),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            if (state.paymentType == LedgerEntryType.CASH_RIAL) {
                                GoldInputField(
                                    value = state.amountInput,
                                    onValueChange = actions::setAmount,
                                    label = if (state.offsetExistingCredit) "مبلغ تهاتر" else "مبلغ پرداخت",
                                    trailingText = "تومان",
                                    modifier = Modifier.testTag("settlementAmount")
                                )

                                if (!state.offsetExistingCredit) {
                                    Box {
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = colors.surface,
                                            border = BorderStroke(1.dp, colors.border),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(12.dp))
                                                .clickable { methodMenu = true }
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 14.dp, vertical = 11.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = state.paymentMethod,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = colors.textMain,
                                                    fontFamily = VazirmatnFamily
                                                )
                                                Icon(
                                                    imageVector = Icons.Default.ArrowDropDown,
                                                    contentDescription = null,
                                                    tint = colors.textSecondary,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }

                                        DropdownMenu(
                                            expanded = methodMenu,
                                            onDismissRequest = { methodMenu = false }
                                        ) {
                                            listOf("حواله بانکی / پایا", "کارتخوان (POS)", "اسکناس نقد").forEach { method ->
                                                DropdownMenuItem(
                                                    text = { Text(method, fontFamily = VazirmatnFamily) },
                                                    onClick = { actions.setMethod(method); methodMenu = false }
                                                )
                                            }
                                        }
                                    }
                                }
                            } else {
                                GoldInputField(
                                    value = state.weightInput,
                                    onValueChange = actions::setWeight,
                                    label = if (state.offsetExistingCredit) "وزن معادل ۷۵۰ برای تهاتر" else "وزن خالص طلای پرداختی (بدون سنگ)",
                                    trailingText = "گرم",
                                    isDecimal = true,
                                    useThousandsSeparator = false,
                                    modifier = Modifier.testTag("settlementWeight")
                                )
                                GoldInputField(
                                    value = state.karatInput,
                                    onValueChange = actions::setKarat,
                                    label = "عیار هزارگانی (مثلاً ۷۵۰)",
                                    enabled = !state.offsetExistingCredit,
                                    useThousandsSeparator = false
                                )
                            }

                            // Quick Fill Auto Calculate Shortcut
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = colors.goldContainer.copy(alpha = 0.35f),
                                border = BorderStroke(1.dp, colors.goldBorder.copy(alpha = 0.5f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable(onClick = actions::fillRemaining)
                            ) {
                                Box(
                                    modifier = Modifier.padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (state.offsetExistingCredit) "⚡ محاسبهٔ تهاتر قابل انجام" else "⚡ محاسبهٔ پرداخت برای تسویهٔ کامل",
                                        color = colors.goldPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.5.sp,
                                        fontFamily = VazirmatnFamily
                                    )
                                }
                            }

                            if (!state.offsetExistingCredit) {
                                GoldInputField(
                                    value = state.trackingCode,
                                    onValueChange = actions::setTracking,
                                    label = "شماره پیگیری / رسید / انگ (اختیاری)",
                                    keyboardType = KeyboardType.Text,
                                    useThousandsSeparator = false
                                )
                            }

                            GoldInputField(
                                value = state.note,
                                onValueChange = actions::setNote,
                                label = "توضیحات (اختیاری)",
                                keyboardType = KeyboardType.Text,
                                useThousandsSeparator = false
                            )
                        }
                    }

                    // 8. Live Settlement Preview Card (Obsidian / Aurum style)
                    if (preview != null) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xFF141B2B),
                            border = BorderStroke(1.dp, Color(0xFFB89456).copy(alpha = 0.6f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "پیش‌نمایش سند تسویه",
                                        fontFamily = VazirmatnFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.5.sp,
                                        color = colors.textMain
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(50),
                                        color = Color(0x3310B981),
                                        border = BorderStroke(0.6.dp, Color(0x6610B981))
                                    ) {
                                        Text(
                                            text = "محاسبه‌شده",
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF10B981),
                                            fontFamily = VazirmatnFamily,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                if (state.needsRate) {
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = Color(0xFF1E232E),
                                        border = BorderStroke(0.6.dp, colors.goldBorder.copy(alpha = 0.4f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = "${PersianNumberFormatter.formatPrice(preview.cashAppliedTomans)} تومان ↔ ${PersianNumberFormatter.formatWeight(preview.goldAppliedGrams)} گرمِ عیار ۷۵۰",
                                            fontFamily = VazirmatnFamily,
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = colors.goldPrimary,
                                            modifier = Modifier
                                                .padding(horizontal = 10.dp, vertical = 8.dp)
                                                .testTag("settlementConversion")
                                        )
                                    }
                                }

                                HorizontalDivider(color = Color(0x33DFB35A), thickness = 0.6.dp)

                                Text(
                                    text = "ماندهٔ حساب مشتری پس از ثبت این تسویه:",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF94A3B8),
                                    fontFamily = VazirmatnFamily
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // Gold after
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = Color(0xFF1E232E),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(8.dp),
                                            verticalArrangement = Arrangement.spacedBy(2.dp)
                                        ) {
                                            Text("طلا:", fontSize = 9.5.sp, color = Color(0xFF94A3B8), fontFamily = VazirmatnFamily)
                                            AnimatedNumberText(
                                                text = PersianNumberFormatter.formatAccountWeight(abs(preview.customerAfter.goldDebtGrams)),
                                                unit = "گرم",
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                            Text(
                                                text = if (preview.customerAfter.goldDebtGrams > 1e-10) "بدهکار" else if (preview.customerAfter.goldDebtGrams < -1e-10) "بستانکار" else "تسویه کامل",
                                                fontSize = 9.sp,
                                                color = if (abs(preview.customerAfter.goldDebtGrams) < 1e-10) Color(0xFF10B981) else Color(0xFFF59E0B),
                                                fontFamily = VazirmatnFamily
                                            )
                                        }
                                    }

                                    // Cash after
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = Color(0xFF1E232E),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(8.dp),
                                            verticalArrangement = Arrangement.spacedBy(2.dp)
                                        ) {
                                            Text("پول:", fontSize = 9.5.sp, color = Color(0xFF94A3B8), fontFamily = VazirmatnFamily)
                                            AnimatedPriceText(
                                                amount = abs(preview.customerAfter.cashDebtTomans),
                                                unit = "تومان",
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                            Text(
                                                text = if (preview.customerAfter.cashDebtTomans > 0) "بدهکار" else if (preview.customerAfter.cashDebtTomans < 0) "بستانکار" else "تسویه کامل",
                                                fontSize = 9.sp,
                                                color = if (preview.customerAfter.cashDebtTomans == 0L) Color(0xFF10B981) else Color(0xFFF59E0B),
                                                fontFamily = VazirmatnFamily
                                            )
                                        }
                                    }
                                }

                                Text(
                                    text = "مازاد پرداخت در واحد خودش ثبت می‌شود. مانده‌ها بدون انتخاب شما تبدیل نمی‌شوند.",
                                    fontFamily = VazirmatnFamily,
                                    color = Color(0xFF94A3B8),
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }

                    // 9. Error display
                    val hasPaymentInput = if (state.paymentType == LedgerEntryType.CASH_RIAL) state.amountInput.isNotBlank() else state.weightInput.isNotBlank()
                    val error = state.error ?: state.validationError?.takeIf { hasPaymentInput }
                    if (error != null) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = colors.errorRed.copy(alpha = 0.15f),
                            border = BorderStroke(0.6.dp, colors.errorRed),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = error,
                                fontFamily = VazirmatnFamily,
                                color = colors.errorRed,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                                    .semantics { liveRegion = LiveRegionMode.Polite }
                            )
                        }
                    }
                }

                // Fixed Sticky Footer with scroll container for test compatibility
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colors.surface)
                        .navigationBarsPadding()
                ) {
                    HorizontalDivider(color = colors.border.copy(alpha = 0.4f), thickness = 0.7.dp)

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // 1. Cancel / Dismiss (First child in Row for RTL = displayed on the RIGHT)
                                GoldButton(
                                    text = "انصراف",
                                    onClick = actions::close,
                                    isSecondary = true,
                                    enabled = !state.isSaving,
                                    modifier = Modifier.weight(1f)
                                )

                                // 2. Confirm / Save (Second child in Row for RTL = displayed on the LEFT)
                                GoldButton(
                                    text = "ثبت تسویه",
                                    onClick = onConfirm,
                                    isSecondary = false,
                                    icon = InvoiceCheckVector,
                                    enabled = preview != null && state.error == null,
                                    isLoading = state.isSaving,
                                    modifier = Modifier
                                        .weight(1.3f)
                                        .testTag("confirmSettlement")
                                )
                            }

                            TextButton(
                                onClick = onIndependentEntry,
                                enabled = !state.isSaving,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 38.dp)
                            ) {
                                Text(
                                    text = "ثبت دریافت / پرداخت مستقل از تسویه",
                                    fontFamily = VazirmatnFamily,
                                    fontSize = 11.sp,
                                    color = colors.textSecondary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Segmented choices component matching Stitch design (grid grid-cols-2 gap-2.5)
 * with Sovereign Aurum gold gradient when active and subtle border when inactive.
 */
@Composable
private fun UnitChoices(
    selected: LedgerEntryType,
    onSelect: (LedgerEntryType) -> Unit,
    goldEnabled: Boolean = true,
    cashEnabled: Boolean = true,
    enabled: Boolean = true
) {
    val colors = LocalGoldExColors.current
    val aurumGradient = Brush.horizontalGradient(
        listOf(Color(0xFFB89456), Color(0xFF9C7A3C))
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        listOf(
            LedgerEntryType.GOLD_WEIGHT to "طلا (گرم ۷۵۰)",
            LedgerEntryType.CASH_RIAL to "پول (تومان)"
        ).forEach { (type, label) ->
            val isSelected = selected == type
            val isItemEnabled = enabled && if (type == LedgerEntryType.GOLD_WEIGHT) goldEnabled else cashEnabled

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (isSelected) Color.Transparent else colors.surfaceElevated,
                border = if (isSelected) null else BorderStroke(1.dp, colors.border),
                shadowElevation = if (isSelected) 2.dp else 0.dp,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .then(
                        if (isSelected) Modifier.background(aurumGradient) else Modifier
                    )
                    .clickable(enabled = isItemEnabled) { onSelect(type) }
            ) {
                Box(
                    modifier = Modifier.padding(vertical = 11.dp, horizontal = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) Color.White else if (isItemEnabled) colors.textMain else colors.textMuted,
                        fontFamily = VazirmatnFamily
                    )
                }
            }
        }
    }
}
