package com.goldex.companion.ui.customers.modals

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
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
import androidx.compose.ui.draw.rotate
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
import com.goldex.companion.ui.components.LuxurySegmentedControl
import com.goldex.companion.ui.customers.CustomerSettlementUiState
import com.goldex.companion.ui.customers.CustomerSettlementViewModel
import com.goldex.companion.ui.invoices.components.InvoiceCheckVector
import com.goldex.companion.ui.invoices.components.InvoiceCloseVector
import com.goldex.companion.ui.theme.ButtonShape
import com.goldex.companion.ui.theme.LocalGoldExColors
import com.goldex.companion.ui.theme.VazirmatnFamily
import kotlin.math.abs

/**
 * Customer Settlement Modal with luxury animations, full-width expandable basis selector,
 * animated spring-sliding segmented controls (LuxurySegmentedControl),
 * deep dark modal backdrop, and live calculation preview directly under payment input.
 * Strictly adheres to Persian goldsmith convention: بدهکار قرمز (Red) / بستانکار سبز (Green).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerSettlementModal(
    state: CustomerSettlementUiState,
    actions: CustomerSettlementViewModel,
    onConfirm: () -> Unit,
    @Suppress("UNUSED_PARAMETER") onIndependentEntry: () -> Unit = {}
) {
    val colors = LocalGoldExColors.current
    val customer = state.customer ?: return
    val preview = state.preview
    var isScopeExpanded by remember { mutableStateOf(false) }
    var methodMenu by remember { mutableStateOf(false) }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        ModalBottomSheet(
            onDismissRequest = actions::close,
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            containerColor = colors.surface,
            scrimColor = Color.Black.copy(alpha = 0.68f),
            dragHandle = null
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.92f)
                    .imePadding()
            ) {
                // Ergonomic Drag Handle
                Box(
                    modifier = Modifier
                        .padding(top = 10.dp, bottom = 4.dp)
                        .size(width = 42.dp, height = 4.5.dp)
                        .clip(CircleShape)
                        .background(colors.border)
                        .align(Alignment.CenterHorizontally)
                )

                // Compact Header Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "تسویه حساب ${customer.name}",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textMain,
                            fontFamily = VazirmatnFamily
                        )
                        Text(
                            text = "پرداخت را اختصاص دهید؛ تبدیل با نرخ همین سند محاسبه می‌شود.",
                            fontSize = 11.sp,
                            color = colors.textSecondary,
                            fontFamily = VazirmatnFamily
                        )
                    }

                    IconButton(
                        onClick = actions::close,
                        modifier = Modifier
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

                HorizontalDivider(color = colors.border.copy(alpha = 0.5f), thickness = 0.7.dp)

                // Scrollable Body Content
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(11.dp)
                ) {
                    // 1. Full-Width Expandable Settlement Scope Selector
                    val currentScopeLabel = state.invoices.firstOrNull { it.id == state.invoiceId }?.let {
                        "بابت فاکتور ${PersianNumberFormatter.toPersianDigits(it.number)}"
                    } ?: if (state.invoiceId == null) "بابت تسویه حساب کل مشتری (معین کلی)" else "فاکتور تسویه شده"

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = colors.surfaceElevated,
                        border = BorderStroke(0.8.dp, if (isScopeExpanded) colors.goldPrimary else colors.border),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { isScopeExpanded = !isScopeExpanded }
                                    .padding(vertical = 4.dp, horizontal = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        text = "مبنای تسویه و اختصاص پرداخت:",
                                        fontSize = 10.5.sp,
                                        color = colors.textSecondary,
                                        fontFamily = VazirmatnFamily
                                    )
                                    Text(
                                        text = currentScopeLabel,
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.goldPrimary,
                                        fontFamily = VazirmatnFamily
                                    )
                                }

                                val rotationAngle by animateFloatAsState(
                                    targetValue = if (isScopeExpanded) 180f else 0f,
                                    label = "chevronRotation"
                                )
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowDown,
                                    contentDescription = if (isScopeExpanded) "بستن گزینه‌ها" else "انتخاب مبنا",
                                    tint = colors.goldPrimary,
                                    modifier = Modifier
                                        .size(22.dp)
                                        .rotate(rotationAngle)
                                )
                            }

                            // Full-width options list with checkmarks
                            AnimatedVisibility(
                                visible = isScopeExpanded,
                                enter = expandVertically() + fadeIn(),
                                exit = shrinkVertically() + fadeOut()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 6.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    HorizontalDivider(color = colors.border.copy(alpha = 0.5f), thickness = 0.5.dp)

                                    // Option 1: General Account
                                    val isGeneralSelected = state.invoiceId == null
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (isGeneralSelected) colors.goldContainer.copy(alpha = 0.45f) else colors.surface,
                                        border = BorderStroke(0.6.dp, if (isGeneralSelected) colors.goldPrimary else colors.border),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .clickable {
                                                actions.selectInvoice(null)
                                                isScopeExpanded = false
                                            }
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 12.dp, vertical = 9.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "بابت تسویه حساب کل مشتری (معین کلی)",
                                                fontSize = 11.5.sp,
                                                fontWeight = if (isGeneralSelected) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isGeneralSelected) colors.goldPrimary else colors.textMain,
                                                fontFamily = VazirmatnFamily
                                            )
                                            if (isGeneralSelected) {
                                                Icon(
                                                    imageVector = InvoiceCheckVector,
                                                    contentDescription = null,
                                                    tint = colors.goldPrimary,
                                                    modifier = Modifier.size(15.dp)
                                                )
                                            }
                                        }
                                    }

                                    // Invoices options
                                    state.invoices.forEach { invoice ->
                                        val isInvSelected = state.invoiceId == invoice.id
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = if (isInvSelected) colors.goldContainer.copy(alpha = 0.45f) else colors.surface,
                                            border = BorderStroke(0.6.dp, if (isInvSelected) colors.goldPrimary else colors.border),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(10.dp))
                                                .clickable {
                                                    actions.selectInvoice(invoice.id)
                                                    isScopeExpanded = false
                                                }
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 12.dp, vertical = 9.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "بابت فاکتور ${PersianNumberFormatter.toPersianDigits(invoice.number)}",
                                                    fontSize = 11.5.sp,
                                                    fontWeight = if (isInvSelected) FontWeight.Bold else FontWeight.Medium,
                                                    color = if (isInvSelected) colors.goldPrimary else colors.textMain,
                                                    fontFamily = VazirmatnFamily
                                                )
                                                if (isInvSelected) {
                                                    Icon(
                                                        imageVector = InvoiceCheckVector,
                                                        contentDescription = null,
                                                        tint = colors.goldPrimary,
                                                        modifier = Modifier.size(15.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 2. Selected Balance Overview Card (Compact & High Contrast)
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = colors.surfaceElevated,
                        border = BorderStroke(0.8.dp, colors.goldBorder.copy(alpha = 0.45f)),
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
                                    text = "ماندهٔ حساب انتخابی",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.goldPrimary,
                                    fontFamily = VazirmatnFamily
                                )
                                val isReceiving = (if (state.targetType == LedgerEntryType.GOLD_WEIGHT) state.scope.goldGrams else state.scope.cashTomans.toDouble()) >= 0
                                Text(
                                    text = if (isReceiving) "دریافت از مشتری" else "پرداخت به مشتری",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isReceiving) Color(0xFFDC2626) else Color(0xFF10B981),
                                    fontFamily = VazirmatnFamily
                                )
                            }

                            // Gold row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "طلا:",
                                    fontSize = 12.sp,
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
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (abs(state.scope.goldGrams) < 1e-10) colors.textMuted else colors.textMain
                                    )
                                    val goldStatus = if (abs(state.scope.goldGrams) < 1e-10) "تسویه" else if (state.scope.goldGrams > 1e-10) "بدهکار" else "بستانکار"
                                    val goldColor = if (abs(state.scope.goldGrams) < 1e-10) Color(0xFF10B981) else if (state.scope.goldGrams > 1e-10) Color(0xFFDC2626) else Color(0xFF10B981)
                                    Text(
                                        text = goldStatus,
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = goldColor,
                                        fontFamily = VazirmatnFamily
                                    )
                                }
                            }

                            HorizontalDivider(color = colors.border.copy(alpha = 0.4f), thickness = 0.6.dp)

                            // Cash row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "پول:",
                                    fontSize = 12.sp,
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
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (state.scope.cashTomans == 0L) colors.textMuted else colors.textMain
                                    )
                                    val cashStatus = if (state.scope.cashTomans == 0L) "تسویه" else if (state.scope.cashTomans > 0) "بدهکار" else "بستانکار"
                                    val cashColor = if (state.scope.cashTomans == 0L) Color(0xFF10B981) else if (state.scope.cashTomans > 0) Color(0xFFDC2626) else Color(0xFF10B981)
                                    Text(
                                        text = cashStatus,
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = cashColor,
                                        fontFamily = VazirmatnFamily
                                    )
                                }
                            }
                        }
                    }

                    // 3. Target Balance to Settle (Luxury Animated Segmented Control)
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "کدام مانده تسویه شود؟",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textMain,
                            fontFamily = VazirmatnFamily
                        )
                        LuxurySegmentedControl(
                            items = listOf(LedgerEntryType.GOLD_WEIGHT, LedgerEntryType.CASH_RIAL),
                            selectedItem = state.targetType,
                            onItemSelected = actions::selectTarget,
                            label = { if (it == LedgerEntryType.GOLD_WEIGHT) "طلا (گرم ۷۵۰)" else "پول (تومان)" },
                            isItemEnabled = {
                                if (it == LedgerEntryType.GOLD_WEIGHT) abs(state.scope.goldGrams) > 1e-10 else state.scope.cashTomans != 0L
                            },
                            height = 38.dp,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // 4. Barter / Offset Switch (if available)
                    if (state.canOffset) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = colors.surfaceElevated,
                            border = BorderStroke(0.7.dp, colors.border),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(
                                    modifier = Modifier.weight(1f).padding(end = 8.dp),
                                    verticalArrangement = Arrangement.spacedBy(1.dp)
                                ) {
                                    val isReceiving = (if (state.targetType == LedgerEntryType.GOLD_WEIGHT) state.scope.goldGrams else state.scope.cashTomans.toDouble()) >= 0
                                    Text(
                                        text = if (isReceiving) "دریافت از مشتری" else "پرداخت به مشتری",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.goldPrimary,
                                        fontFamily = VazirmatnFamily
                                    )
                                    Text(
                                        text = "تهاتر با ماندهٔ مقابل (بدون دریافت جدید)",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = colors.textMain,
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

                    // 5. Payment Method / Offset Unit (Luxury Animated Segmented Control)
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = if (state.offsetExistingCredit) "واحد بستانکاری برای تهاتر" else "روش پرداخت",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textMain,
                            fontFamily = VazirmatnFamily
                        )
                        LuxurySegmentedControl(
                            items = listOf(LedgerEntryType.GOLD_WEIGHT, LedgerEntryType.CASH_RIAL),
                            selectedItem = state.paymentType,
                            onItemSelected = actions::selectPayment,
                            label = { if (it == LedgerEntryType.GOLD_WEIGHT) "طلا (گرم ۷۵۰)" else "پول (تومان)" },
                            isItemEnabled = { !state.offsetExistingCredit },
                            height = 38.dp,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // 6. Rate Selection (when needsRate)
                    if (state.needsRate) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = colors.surfaceElevated,
                            border = BorderStroke(0.8.dp, colors.goldBorder.copy(alpha = 0.45f)),
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

                                    // Compact Luxury Animated Rate Selector
                                    LuxurySegmentedControl(
                                        items = listOf(true, false),
                                        selectedItem = state.isMarketRate,
                                        onItemSelected = { isMarket ->
                                            if (isMarket) actions.useMarketRate() else actions.useCustomRate()
                                        },
                                        label = { if (it) "نرخ بازار" else "نرخ دلخواه" },
                                        height = 32.dp,
                                        fontSize = 11.sp,
                                        modifier = Modifier.width(170.dp)
                                    )
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

                    // 7. Payment Amount / Weight Input & Calculation Shortcut
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = colors.surfaceElevated,
                        border = BorderStroke(0.8.dp, colors.border),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(9.dp)
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
                                            shape = RoundedCornerShape(10.dp),
                                            color = colors.surface,
                                            border = BorderStroke(0.7.dp, colors.border),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(10.dp))
                                                .clickable { methodMenu = true }
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 12.dp, vertical = 9.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = state.paymentMethod,
                                                    fontSize = 11.5.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = colors.textMain,
                                                    fontFamily = VazirmatnFamily
                                                )
                                                Icon(
                                                    imageVector = Icons.Default.KeyboardArrowDown,
                                                    contentDescription = null,
                                                    tint = colors.textSecondary,
                                                    modifier = Modifier.size(18.dp)
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
                                shape = RoundedCornerShape(10.dp),
                                color = colors.goldContainer.copy(alpha = 0.4f),
                                border = BorderStroke(0.8.dp, colors.goldBorder),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable(onClick = actions::fillRemaining)
                            ) {
                                Box(
                                    modifier = Modifier.padding(vertical = 9.dp),
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
                        }
                    }

                    // 8. LIVE RESULTING STATUS CARD ("مانده پس از تسویه")
                    // Directly shows the user whether the balance is fully settled, or how much remains
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = colors.surfaceElevated,
                        border = BorderStroke(
                            1.dp,
                            if (preview != null && abs(preview.customerAfter.goldDebtGrams) < 1e-10 && preview.customerAfter.cashDebtTomans == 0L)
                                Color(0xFF10B981)
                            else colors.goldBorder.copy(alpha = 0.6f)
                        ),
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
                                    text = "وضعیت مانده پس از این تسویه:",
                                    fontFamily = VazirmatnFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.5.sp,
                                    color = colors.textMain
                                )

                                if (preview != null) {
                                    val isFullySettled = abs(preview.customerAfter.goldDebtGrams) < 1e-10 && preview.customerAfter.cashDebtTomans == 0L
                                    Surface(
                                        shape = RoundedCornerShape(50),
                                        color = if (isFullySettled) Color(0x2610B981) else Color(0x26F59E0B),
                                        border = BorderStroke(0.6.dp, if (isFullySettled) Color(0xFF10B981) else Color(0xFFF59E0B))
                                    ) {
                                        Text(
                                            text = if (isFullySettled) "✓ تسویه کامل" else "دارای مانده",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isFullySettled) Color(0xFF10B981) else Color(0xFFF59E0B),
                                            fontFamily = VazirmatnFamily,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }
                                } else {
                                    Text(
                                        text = "در انتظار ورود مقدار",
                                        fontSize = 10.sp,
                                        color = colors.textMuted,
                                        fontFamily = VazirmatnFamily
                                    )
                                }
                            }

                            if (preview != null) {
                                if (state.needsRate) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = colors.surfaceVariant,
                                        border = BorderStroke(0.6.dp, colors.goldBorder.copy(alpha = 0.4f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = "${PersianNumberFormatter.formatPrice(preview.cashAppliedTomans)} تومان ↔ ${PersianNumberFormatter.formatWeight(preview.goldAppliedGrams)} گرمِ عیار ۷۵۰",
                                            fontFamily = VazirmatnFamily,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = colors.goldPrimary,
                                            modifier = Modifier
                                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                                .testTag("settlementConversion")
                                        )
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // Gold after
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = colors.surfaceVariant,
                                        border = BorderStroke(0.5.dp, colors.border),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(8.dp),
                                            verticalArrangement = Arrangement.spacedBy(2.dp)
                                        ) {
                                            Text("مانده طلا:", fontSize = 9.5.sp, color = colors.textSecondary, fontFamily = VazirmatnFamily)
                                            AnimatedNumberText(
                                                text = PersianNumberFormatter.formatAccountWeight(abs(preview.customerAfter.goldDebtGrams)),
                                                unit = "گرم",
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = colors.textMain
                                            )
                                            val goldAfterStatus = if (abs(preview.customerAfter.goldDebtGrams) < 1e-10) "تسویه کامل" else if (preview.customerAfter.goldDebtGrams > 1e-10) "بدهکار" else "بستانکار"
                                            val goldAfterColor = if (abs(preview.customerAfter.goldDebtGrams) < 1e-10) Color(0xFF10B981) else if (preview.customerAfter.goldDebtGrams > 1e-10) Color(0xFFDC2626) else Color(0xFF10B981)
                                            Text(
                                                text = goldAfterStatus,
                                                fontSize = 9.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = goldAfterColor,
                                                fontFamily = VazirmatnFamily
                                            )
                                        }
                                    }

                                    // Cash after
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = colors.surfaceVariant,
                                        border = BorderStroke(0.5.dp, colors.border),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(8.dp),
                                            verticalArrangement = Arrangement.spacedBy(2.dp)
                                        ) {
                                            Text("مانده پول:", fontSize = 9.5.sp, color = colors.textSecondary, fontFamily = VazirmatnFamily)
                                            AnimatedPriceText(
                                                amount = abs(preview.customerAfter.cashDebtTomans),
                                                unit = "تومان",
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = colors.textMain
                                            )
                                            val cashAfterStatus = if (preview.customerAfter.cashDebtTomans == 0L) "تسویه کامل" else if (preview.customerAfter.cashDebtTomans > 0) "بدهکار" else "بستانکار"
                                            val cashAfterColor = if (preview.customerAfter.cashDebtTomans == 0L) Color(0xFF10B981) else if (preview.customerAfter.cashDebtTomans > 0) Color(0xFFDC2626) else Color(0xFF10B981)
                                            Text(
                                                text = cashAfterStatus,
                                                fontSize = 9.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = cashAfterColor,
                                                fontFamily = VazirmatnFamily
                                            )
                                        }
                                    }
                                }

                                val hasExcess = (state.scope.goldGrams > 1e-10 && preview.customerAfter.goldDebtGrams < -1e-10) ||
                                        (state.scope.cashTomans > 0 && preview.customerAfter.cashDebtTomans < 0)
                                if (hasExcess) {
                                    Text(
                                        text = "مبلغ یا وزن مازاد به عنوان بستانکاری در حساب مشتری ثبت خواهد شد.",
                                        fontFamily = VazirmatnFamily,
                                        color = Color(0xFF10B981),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            } else {
                                Text(
                                    text = "مبلغ یا وزن پرداخت را وارد کنید تا وضعیت مانده و تسویه به‌صورت زنده نمایش داده شود.",
                                    fontFamily = VazirmatnFamily,
                                    color = colors.textMuted,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }

                    // 9. Optional Metadata (Tracking Code & Notes)
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

                    // 10. Error banner
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

                // Fixed Sticky Footer (Cancel on Right, Confirm on Left)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colors.surface)
                        .navigationBarsPadding()
                ) {
                    HorizontalDivider(color = colors.border.copy(alpha = 0.5f), thickness = 0.7.dp)

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 10.dp),
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
                    }
                }
            }
        }
    }
}
