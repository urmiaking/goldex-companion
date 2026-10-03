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
import androidx.compose.material3.FilterChip
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
                    .fillMaxHeight(0.92f)
                    .imePadding()
            ) {
                // Fixed Header
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colors.surface)
                ) {
                    Box(
                        modifier = Modifier
                            .padding(top = 10.dp, bottom = 4.dp)
                            .size(width = 44.dp, height = 4.dp)
                            .clip(CircleShape)
                            .background(colors.border)
                            .align(Alignment.CenterHorizontally)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = "تسویه حساب طرف‌حساب",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textMain,
                                fontFamily = VazirmatnFamily
                            )
                            Text(
                                text = customer.name,
                                fontSize = 12.sp,
                                color = colors.goldPrimary,
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = VazirmatnFamily
                            )
                        }

                        IconButton(
                            onClick = actions::close,
                            modifier = Modifier
                                .size(34.dp)
                                .clip(ButtonShape)
                                .background(colors.surfaceVariant)
                                .border(0.6.dp, colors.goldBorder, ButtonShape)
                        ) {
                            Icon(
                                imageVector = InvoiceCloseVector,
                                contentDescription = "بستن",
                                tint = colors.textSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    HorizontalDivider(color = colors.border.copy(alpha = 0.4f), thickness = 0.7.dp)
                }

                // Scrollable Content Body
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 18.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // 1. Scope Card (بابت فاکتور یا حساب کلی)
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = colors.surfaceElevated,
                        border = BorderStroke(0.8.dp, colors.border),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "مبنای تسویه",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = colors.textMuted,
                                fontFamily = VazirmatnFamily
                            )

                            Box {
                                val currentScopeLabel = state.invoices.firstOrNull { it.id == state.invoiceId }?.let {
                                    "بابت فاکتور ${PersianNumberFormatter.toPersianDigits(it.number)}"
                                } ?: if (state.invoiceId == null) "بابت کل حساب دفتری مشتری" else "فاکتور تسویه شده یا در دسترس نیست"

                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = colors.surfaceVariant,
                                    border = BorderStroke(0.6.dp, colors.goldBorder.copy(alpha = 0.6f)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable { scopeMenu = true }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = currentScopeLabel,
                                            fontSize = 12.5.sp,
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
                                        text = { Text("حساب کلی مشتری", fontFamily = VazirmatnFamily) },
                                        onClick = { scopeMenu = false; actions.selectInvoice(null) }
                                    )
                                    state.invoices.forEach { invoice ->
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    "فاکتور ${PersianNumberFormatter.toPersianDigits(invoice.number)}",
                                                    fontFamily = VazirmatnFamily
                                                )
                                            },
                                            onClick = { scopeMenu = false; actions.selectInvoice(invoice.id) }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 2. Outstanding Balances Monitor Card (Obsidian style)
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFF141B2B),
                        border = BorderStroke(1.dp, Color(0x4DF59E0B)),
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
                                    text = "ماندهٔ حساب انتخابی",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFCBD5E1),
                                    fontFamily = VazirmatnFamily
                                )
                                Text(
                                    text = if ((if (state.targetType == LedgerEntryType.GOLD_WEIGHT) state.scope.goldGrams else state.scope.cashTomans.toDouble()) >= 0)
                                        "دریافت از مشتری" else "پرداخت به مشتری",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.goldPrimary,
                                    fontFamily = VazirmatnFamily
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Gold Box
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFF1E232E),
                                    border = BorderStroke(0.6.dp, Color(0x33DFB35A)),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(10.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = "مانده طلا (۷۵۰):",
                                            fontSize = 10.sp,
                                            color = Color(0xFF94A3B8),
                                            fontFamily = VazirmatnFamily
                                        )
                                        AnimatedNumberText(
                                            text = PersianNumberFormatter.formatAccountWeight(abs(state.scope.goldGrams)),
                                            unit = "گرم",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (abs(state.scope.goldGrams) < 1e-10) Color(0xFF94A3B8) else Color(0xFFF59E0B)
                                        )
                                        Text(
                                            text = if (state.scope.goldGrams > 1e-10) "بدهکار" else if (state.scope.goldGrams < -1e-10) "بستانکار" else "تسویه",
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = if (abs(state.scope.goldGrams) < 1e-10) Color(0xFF10B981) else Color(0xFFF59E0B),
                                            fontFamily = VazirmatnFamily
                                        )
                                    }
                                }

                                // Cash Box
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFF1E232E),
                                    border = BorderStroke(0.6.dp, Color(0x33DFB35A)),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(10.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = "مانده نقدی ریالی:",
                                            fontSize = 10.sp,
                                            color = Color(0xFF94A3B8),
                                            fontFamily = VazirmatnFamily
                                        )
                                        AnimatedPriceText(
                                            amount = abs(state.scope.cashTomans),
                                            unit = "تومان",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (state.scope.cashTomans == 0L) Color(0xFF94A3B8) else Color.White
                                        )
                                        Text(
                                            text = if (state.scope.cashTomans > 0) "بدهکار" else if (state.scope.cashTomans < 0) "بستانکار" else "تسویه",
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = if (state.scope.cashTomans == 0L) Color(0xFF10B981) else Color(0xFFF59E0B),
                                            fontFamily = VazirmatnFamily
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 3. Target Balance Choice
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = colors.surfaceElevated,
                        border = BorderStroke(0.8.dp, colors.border),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "کدام مانده تسویه شود؟",
                                fontSize = 11.5.sp,
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
                    }

                    // 4. Offset Switch (if available)
                    if (state.canOffset) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = colors.surfaceElevated,
                            border = BorderStroke(0.8.dp, colors.border),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                    Text(
                                        text = "تهاتر با ماندهٔ مقابل",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.textMain,
                                        fontFamily = VazirmatnFamily
                                    )
                                    Text(
                                        text = "تسویه بدون دریافت یا پرداخت جدید نقدی/طلایی",
                                        fontSize = 9.5.sp,
                                        color = colors.textMuted,
                                        fontFamily = VazirmatnFamily
                                    )
                                }
                                Switch(
                                    checked = state.offsetExistingCredit,
                                    onCheckedChange = actions::setOffset,
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = colors.goldPrimary,
                                        checkedTrackColor = colors.goldContainer
                                    )
                                )
                            }
                        }
                    }

                    // 5. Payment / Offset Unit
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = colors.surfaceElevated,
                        border = BorderStroke(0.8.dp, colors.border),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = if (state.offsetExistingCredit) "واحد بستانکاری برای تهاتر" else "روش پرداخت",
                                fontSize = 11.5.sp,
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
                    }

                    // 6. Rate Selection (when needsRate)
                    if (state.needsRate) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = colors.surfaceElevated,
                            border = BorderStroke(0.8.dp, colors.goldBorder.copy(alpha = 0.5f)),
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
                                        text = "نرخ هر گرم طلای ۱۸ عیار",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.textMain,
                                        fontFamily = VazirmatnFamily
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        FilterChip(
                                            selected = state.isMarketRate,
                                            onClick = actions::useMarketRate,
                                            label = { Text("نرخ بازار", fontFamily = VazirmatnFamily, fontSize = 11.sp) },
                                            modifier = Modifier.heightIn(min = 36.dp)
                                        )
                                        FilterChip(
                                            selected = !state.isMarketRate,
                                            onClick = actions::useCustomRate,
                                            label = { Text("نرخ دلخواه", fontFamily = VazirmatnFamily, fontSize = 11.sp) },
                                            modifier = Modifier.heightIn(min = 36.dp)
                                        )
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
                        shape = RoundedCornerShape(14.dp),
                        color = colors.surfaceElevated,
                        border = BorderStroke(0.8.dp, colors.border),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
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
                                        GoldButton(
                                            text = state.paymentMethod,
                                            onClick = { methodMenu = true },
                                            isSecondary = true,
                                            modifier = Modifier.fillMaxWidth()
                                        )
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

                            // Quick Fill Button
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = colors.goldContainer.copy(alpha = 0.5f),
                                border = BorderStroke(0.6.dp, colors.goldBorder),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
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

                    // 8. Live Settlement Preview Card
                    if (preview != null) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xFF141B2B),
                            border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.6f)),
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

                // Fixed Sticky Footer
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
                                .padding(horizontal = 18.dp, vertical = 10.dp),
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

@Composable
private fun UnitChoices(
    selected: LedgerEntryType,
    onSelect: (LedgerEntryType) -> Unit,
    goldEnabled: Boolean = true,
    cashEnabled: Boolean = true,
    enabled: Boolean = true
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(LedgerEntryType.GOLD_WEIGHT to "طلا (گرم ۷۵۰)", LedgerEntryType.CASH_RIAL to "پول (تومان)").forEach { (type, label) ->
            FilterChip(
                selected = selected == type,
                onClick = { onSelect(type) },
                enabled = enabled && if (type == LedgerEntryType.GOLD_WEIGHT) goldEnabled else cashEnabled,
                label = { Text(label, fontFamily = VazirmatnFamily, fontSize = 11.5.sp) },
                modifier = Modifier.weight(1f).heightIn(min = 40.dp)
            )
        }
    }
}
