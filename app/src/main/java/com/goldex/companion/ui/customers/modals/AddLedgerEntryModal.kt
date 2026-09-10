package com.goldex.companion.ui.customers.modals

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.goldex.companion.model.Customer
import com.goldex.companion.model.LedgerDirection
import com.goldex.companion.model.LedgerEntryType
import com.goldex.companion.model.LedgerTransaction
import com.goldex.companion.model.PersianNumberFormatter
import com.goldex.companion.model.PersianWordsFormatter
import com.goldex.companion.ui.components.AnimatedNumberText
import com.goldex.companion.ui.components.AnimatedPriceText
import com.goldex.companion.ui.components.GoldButton
import com.goldex.companion.ui.components.GoldInputField
import com.goldex.companion.ui.customers.LedgerAccountBalanceVector
import com.goldex.companion.ui.customers.LedgerArrowPayVector
import com.goldex.companion.ui.customers.LedgerArrowReceiveVector
import com.goldex.companion.ui.customers.LedgerScaleVector
import com.goldex.companion.ui.customers.LedgerVerifiedVector
import com.goldex.companion.ui.theme.LocalGoldExColors
import com.goldex.companion.ui.theme.LuxuryMotion
import com.goldex.companion.ui.theme.VazirmatnFamily
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddLedgerEntryModal(
    customer: Customer,
    onDismiss: () -> Unit,
    onSaveEntry: (LedgerTransaction) -> Unit
) {
    val colors = LocalGoldExColors.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Mode: Gold / Weight (0) vs Cash / Rial (1)
    var selectedModeIndex by remember { mutableIntStateOf(0) }
    // Direction: Receive from Customer (0) vs Pay to Customer (1)
    var selectedDirectionIndex by remember { mutableIntStateOf(0) }

    // Common Document Info
    val documentNumber = remember { "۸" + (100..999).random().toString() }
    val currentDateStr = remember {
        val sdf = SimpleDateFormat("yyyy/MM/dd - HH:mm", Locale.getDefault())
        PersianNumberFormatter.toPersianDigits(sdf.format(Date()))
    }
    var noteInput by remember { mutableStateOf("") }

    // Gold State
    var goldCategory by remember { mutableStateOf("آبشده") } // آبشده, مصنوعات, سکه و شمش
    var scaleWeightInput by remember { mutableStateOf("50.410") }
    var karatInput by remember { mutableStateOf("750") }
    var angNumberInput by remember { mutableStateOf("") }
    var labNameInput by remember { mutableStateOf("ری‌گیری تهران") }

    // Cash State
    var cashAmountInput by remember { mutableStateOf("25000000") }
    var paymentMethod by remember { mutableStateOf("حواله بانکی / پایا") } // حواله بانکی / پایا, چک صیادی, کارتخوان (POS), اسکناس نقد
    var destinationBank by remember { mutableStateOf("بانک ملت - جاری طلافروشی") }
    var trackingCodeInput by remember { mutableStateOf("") }

    // Derived Calculations for Gold
    val scaleWeightDouble by remember(scaleWeightInput) {
        derivedStateOf {
            scaleWeightInput.trim().replace("٫", ".").toDoubleOrNull() ?: 0.0
        }
    }
    val karatInt by remember(karatInput) {
        derivedStateOf {
            karatInput.trim().toIntOrNull() ?: 750
        }
    }
    val equivalent750Grams by remember(scaleWeightDouble, karatInt) {
        derivedStateOf {
            if (scaleWeightDouble > 0.0 && karatInt > 0) {
                (scaleWeightDouble * karatInt) / 750.0
            } else 0.0
        }
    }
    val karatDeltaGrams by remember(scaleWeightDouble, equivalent750Grams) {
        derivedStateOf {
            scaleWeightDouble - equivalent750Grams
        }
    }

    // Derived Calculations for Cash
    val cashAmountLong by remember(cashAmountInput) {
        derivedStateOf {
            PersianNumberFormatter.parseToCleanLong(cashAmountInput) ?: 0L
        }
    }
    val cashInWords by remember(cashAmountLong) {
        derivedStateOf {
            if (cashAmountLong > 0L) {
                PersianWordsFormatter.toWords(cashAmountLong)
            } else ""
        }
    }

    // Projected Resulting Balance
    val isGoldMode = selectedModeIndex == 0
    val isReceive = selectedDirectionIndex == 0

    val newProjectedGoldBalance by remember(customer.goldDebtGrams, equivalent750Grams, isReceive, isGoldMode) {
        derivedStateOf {
            if (!isGoldMode) customer.goldDebtGrams
            else {
                if (isReceive) customer.goldDebtGrams - equivalent750Grams
                else customer.goldDebtGrams + equivalent750Grams
            }
        }
    }

    val newProjectedCashBalance by remember(customer.cashDebtTomans, cashAmountLong, isReceive, isGoldMode) {
        derivedStateOf {
            if (isGoldMode) customer.cashDebtTomans
            else {
                if (isReceive) customer.cashDebtTomans - cashAmountLong
                else customer.cashDebtTomans + cashAmountLong
            }
        }
    }

    val isFormValid by remember(isGoldMode, scaleWeightDouble, cashAmountLong) {
        derivedStateOf {
            if (isGoldMode) scaleWeightDouble > 0.0 else cashAmountLong > 0L
        }
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = sheetState,
            containerColor = colors.surface,
            dragHandle = {
                Surface(
                    modifier = Modifier.padding(vertical = 10.dp),
                    color = colors.border,
                    shape = RoundedCornerShape(2.dp)
                ) {
                    Box(modifier = Modifier.size(width = 38.dp, height = 4.dp))
                }
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.92f)
            ) {
                // 1. Fixed Header Bar (Fix 5: Stays docked at top)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(colors.goldContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = LedgerAccountBalanceVector,
                                contentDescription = null,
                                tint = colors.goldPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "ثبت سند در دفتر معین",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textMain,
                                fontFamily = VazirmatnFamily
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "سند روزنامه #${PersianNumberFormatter.toPersianDigits(documentNumber)}",
                                    fontSize = 11.sp,
                                    color = colors.textMuted,
                                    fontFamily = VazirmatnFamily
                                )
                                Box(
                                    modifier = Modifier
                                        .size(3.dp)
                                        .clip(CircleShape)
                                        .background(colors.textMuted)
                                )
                                Text(
                                    text = currentDateStr,
                                    fontSize = 11.sp,
                                    color = colors.goldPrimary,
                                    fontWeight = FontWeight.Medium,
                                    fontFamily = VazirmatnFamily
                                )
                            }
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "انصراف و بستن",
                            tint = colors.textMuted
                        )
                    }
                }

                HorizontalDivider(color = colors.border.copy(alpha = 0.5f), thickness = 0.6.dp)

                // 2. Scrollable Middle Body (Fix 5: Scrolls independently between header & footer)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 18.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {

                // 2. Counterparty Snapshot Strip
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = colors.surfaceElevated,
                    border = BorderStroke(0.6.dp, colors.goldBorder.copy(alpha = 0.4f)),
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
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(colors.surfaceElevated)
                                    .border(0.8.dp, colors.goldBorder.copy(alpha = 0.6f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = customer.name.firstOrNull()?.toString() ?: "ط",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Black,
                                    color = colors.goldPrimary,
                                    fontFamily = VazirmatnFamily
                                )
                            }
                            Column {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = customer.name,
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.textMain,
                                        fontFamily = VazirmatnFamily
                                    )
                                    if (customer.accountCode.isNotBlank()) {
                                        Text(
                                            text = "کد ${PersianNumberFormatter.toPersianDigits(customer.accountCode)}",
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = colors.goldPrimary,
                                            fontFamily = VazirmatnFamily,
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(colors.goldContainer)
                                                .padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = customer.role,
                                    fontSize = 11.sp,
                                    color = colors.textMuted,
                                    fontFamily = VazirmatnFamily
                                )
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "مانده معین فعلی:",
                                fontSize = 10.sp,
                                color = colors.textMuted,
                                fontFamily = VazirmatnFamily
                            )
                            Text(
                                text = "${PersianNumberFormatter.formatWeight(customer.goldDebtGrams)} گرم طلا",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (customer.goldDebtGrams >= 0) colors.profitGreen else colors.errorRed,
                                fontFamily = VazirmatnFamily
                            )
                            Text(
                                text = "${PersianNumberFormatter.formatPrice(customer.cashDebtTomans)} تومان",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.textSecondary,
                                fontFamily = VazirmatnFamily
                            )
                        }
                    }
                }

                // 3. Direction Selector with Green Receive & Red Pay (Fix 6: Stitch Screens 1 & 2)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val isReceiveSelected = selectedDirectionIndex == 0
                    val receiveBorderColor by animateColorAsState(
                        targetValue = if (isReceiveSelected) Color(0xFF10B981) else colors.border,
                        animationSpec = tween(durationMillis = 200, easing = LuxuryMotion.StandardEasing),
                        label = "receiveBorder"
                    )
                    val receiveBgColor by animateColorAsState(
                        targetValue = if (isReceiveSelected) Color(0xFF10B981).copy(alpha = 0.15f) else colors.surfaceElevated,
                        animationSpec = tween(durationMillis = 200, easing = LuxuryMotion.StandardEasing),
                        label = "receiveBg"
                    )
                    val receiveTextColor by animateColorAsState(
                        targetValue = if (isReceiveSelected) Color(0xFF10B981) else colors.textMuted,
                        animationSpec = tween(durationMillis = 200, easing = LuxuryMotion.StandardEasing),
                        label = "receiveText"
                    )

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = receiveBgColor,
                        border = BorderStroke(if (isReceiveSelected) 1.2.dp else 0.6.dp, receiveBorderColor),
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { selectedDirectionIndex = 0 }
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 11.dp, horizontal = 8.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = LedgerArrowReceiveVector,
                                contentDescription = null,
                                tint = if (isReceiveSelected) Color(0xFF10B981) else colors.textMuted,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "دریافت از طرف‌حساب",
                                fontSize = 12.sp,
                                fontWeight = if (isReceiveSelected) FontWeight.Bold else FontWeight.Medium,
                                color = receiveTextColor,
                                fontFamily = VazirmatnFamily
                            )
                        }
                    }

                    val isPaySelected = selectedDirectionIndex == 1
                    val payBorderColor by animateColorAsState(
                        targetValue = if (isPaySelected) Color(0xFFEF4444) else colors.border,
                        animationSpec = tween(durationMillis = 200, easing = LuxuryMotion.StandardEasing),
                        label = "payBorder"
                    )
                    val payBgColor by animateColorAsState(
                        targetValue = if (isPaySelected) Color(0xFFEF4444).copy(alpha = 0.15f) else colors.surfaceElevated,
                        animationSpec = tween(durationMillis = 200, easing = LuxuryMotion.StandardEasing),
                        label = "payBg"
                    )
                    val payTextColor by animateColorAsState(
                        targetValue = if (isPaySelected) Color(0xFFEF4444) else colors.textMuted,
                        animationSpec = tween(durationMillis = 200, easing = LuxuryMotion.StandardEasing),
                        label = "payText"
                    )

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = payBgColor,
                        border = BorderStroke(if (isPaySelected) 1.2.dp else 0.6.dp, payBorderColor),
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { selectedDirectionIndex = 1 }
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 11.dp, horizontal = 8.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = LedgerArrowPayVector,
                                contentDescription = null,
                                tint = if (isPaySelected) Color(0xFFEF4444) else colors.textMuted,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "پرداخت به طرف‌حساب",
                                fontSize = 12.sp,
                                fontWeight = if (isPaySelected) FontWeight.Bold else FontWeight.Medium,
                                color = payTextColor,
                                fontFamily = VazirmatnFamily
                            )
                        }
                    }
                }

                // 4. Settlement Mode Tabs (تسویه وزنی و طلا vs تسویه نقدی و ریالی)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val isGoldSelected = selectedModeIndex == 0
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isGoldSelected) colors.surfaceElevated else colors.surface,
                        border = BorderStroke(
                            if (isGoldSelected) 1.dp else 0.5.dp,
                            if (isGoldSelected) colors.goldPrimary else colors.border
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { selectedModeIndex = 0 }
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 10.dp, horizontal = 12.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = LedgerScaleVector,
                                contentDescription = null,
                                tint = if (isGoldSelected) colors.goldPrimary else colors.textMuted,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.size(6.dp))
                            Text(
                                text = "تسویه وزنی و طلا",
                                fontSize = 12.sp,
                                fontWeight = if (isGoldSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isGoldSelected) colors.textMain else colors.textMuted,
                                fontFamily = VazirmatnFamily
                            )
                        }
                    }

                    val isCashSelected = selectedModeIndex == 1
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isCashSelected) colors.surfaceElevated else colors.surface,
                        border = BorderStroke(
                            if (isCashSelected) 1.dp else 0.5.dp,
                            if (isCashSelected) colors.goldPrimary else colors.border
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { selectedModeIndex = 1 }
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 10.dp, horizontal = 12.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = LedgerAccountBalanceVector,
                                contentDescription = null,
                                tint = if (isCashSelected) colors.goldPrimary else colors.textMuted,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.size(6.dp))
                            Text(
                                text = "تسویه نقدی و ریالی",
                                fontSize = 12.sp,
                                fontWeight = if (isCashSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isCashSelected) colors.textMain else colors.textMuted,
                                fontFamily = VazirmatnFamily
                            )
                        }
                    }
                }

                // =========================================================================
                // 5A. GOLD MODE FORM (Screen 1)
                // =========================================================================
                if (isGoldMode) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        // Category Pills
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("آبشده", "مصنوعات", "سکه و شمش").forEach { cat ->
                                val active = goldCategory == cat
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (active) colors.goldPrimary else colors.surfaceElevated,
                                    border = BorderStroke(0.6.dp, if (active) colors.goldPrimary else colors.border),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { goldCategory = cat }
                                ) {
                                    Text(
                                        text = cat,
                                        fontSize = 11.5.sp,
                                        fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                                        color = if (active) Color(0xFF554300) else colors.textSecondary,
                                        textAlign = TextAlign.Center,
                                        fontFamily = VazirmatnFamily,
                                        modifier = Modifier.padding(vertical = 7.dp)
                                    )
                                }
                            }
                        }

                        // Inputs Box: Scale Weight & Karat (Fix 7: GoldInputField)
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = colors.surfaceElevated,
                            border = BorderStroke(0.6.dp, colors.border),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    GoldInputField(
                                        value = scaleWeightInput,
                                        onValueChange = { scaleWeightInput = it },
                                        label = "وزن ترازو",
                                        trailingText = "گرم",
                                        isDecimal = true,
                                        useThousandsSeparator = false,
                                        keyboardType = KeyboardType.Decimal,
                                        modifier = Modifier.weight(1f)
                                    )

                                    Column(modifier = Modifier.weight(1f)) {
                                        GoldInputField(
                                            value = karatInput,
                                            onValueChange = { karatInput = it },
                                            label = "عیار ری‌گیری",
                                            trailingText = "عیار",
                                            isDecimal = false,
                                            useThousandsSeparator = false,
                                            keyboardType = KeyboardType.Number,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.End
                                        ) {
                                            Text(
                                                text = "تنظیم ۷۵۰ استاندارد",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = colors.goldPrimary,
                                                fontFamily = VazirmatnFamily,
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(colors.goldContainer.copy(alpha = 0.5f))
                                                    .clickable { karatInput = "750" }
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }

                                // Ang Number & Lab Name
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    GoldInputField(
                                        value = angNumberInput,
                                        onValueChange = { angNumberInput = it },
                                        label = "شماره قبض / اَنگ",
                                        useThousandsSeparator = false,
                                        keyboardType = KeyboardType.Text,
                                        modifier = Modifier.weight(1f)
                                    )

                                    GoldInputField(
                                        value = labNameInput,
                                        onValueChange = { labNameInput = it },
                                        label = "آزمایشگاه ری‌گیری",
                                        useThousandsSeparator = false,
                                        keyboardType = KeyboardType.Text,
                                        modifier = Modifier.weight(1.3f)
                                    )
                                }

                                // Obsidian Calculation Monitor Card with Animated Numbers (Fix 7)
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = Color(0xFF141B2B),
                                    border = BorderStroke(0.8.dp, colors.goldBorder.copy(alpha = 0.6f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = "فرمول: (وزن × عیار) ÷ ۷۵۰",
                                                fontSize = 10.sp,
                                                color = Color(0xFF94A3B8),
                                                fontFamily = VazirmatnFamily
                                            )
                                            Text(
                                                text = "وزن معادل ۷۵۰ (محاسباتی):",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFFFFE088),
                                                fontFamily = VazirmatnFamily
                                            )
                                        }
                                        Column(horizontalAlignment = Alignment.End) {
                                            AnimatedNumberText(
                                                text = PersianNumberFormatter.formatWeight(equivalent750Grams),
                                                unit = "گرم",
                                                color = Color.White,
                                                fontSize = 17.sp,
                                                fontWeight = FontWeight.Black
                                            )
                                            AnimatedNumberText(
                                                text = "کسر عیار: ${PersianNumberFormatter.formatWeight(karatDeltaGrams)}",
                                                unit = "گرم",
                                                color = Color(0xFFCBD5E1),
                                                fontSize = 10.5.sp,
                                                fontWeight = FontWeight.Normal
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // =========================================================================
                // 5B. CASH MODE FORM (Screen 2)
                // =========================================================================
                if (!isGoldMode) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        // Payment Method Pills
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf("حواله بانکی / پایا", "چک صیادی", "کارتخوان (POS)", "اسکناس نقد").forEach { method ->
                                val active = paymentMethod == method
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (active) colors.goldPrimary else colors.surfaceElevated,
                                    border = BorderStroke(0.6.dp, if (active) colors.goldPrimary else colors.border),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { paymentMethod = method }
                                ) {
                                    Text(
                                        text = method,
                                        fontSize = 10.sp,
                                        fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                                        color = if (active) Color(0xFF554300) else colors.textSecondary,
                                        textAlign = TextAlign.Center,
                                        fontFamily = VazirmatnFamily,
                                        modifier = Modifier.padding(vertical = 7.dp)
                                    )
                                }
                            }
                        }

                        // Cash Inputs Container
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = colors.surfaceElevated,
                            border = BorderStroke(0.6.dp, colors.border),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Cash Amount
                                Column {
                                    if (cashInWords.isNotBlank()) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(bottom = 6.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "مبلغ به حروف:",
                                                fontSize = 11.sp,
                                                color = colors.textMuted,
                                                fontFamily = VazirmatnFamily
                                            )
                                            Text(
                                                text = "$cashInWords تومان",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = colors.goldPrimary,
                                                fontFamily = VazirmatnFamily
                                            )
                                        }
                                    }

                                    GoldInputField(
                                        value = cashAmountInput,
                                        onValueChange = { cashAmountInput = it },
                                        label = "مبلغ واریزی / دریافتی",
                                        trailingText = "تومان",
                                        useThousandsSeparator = true,
                                        keyboardType = KeyboardType.Number,
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    // Quick Amount Increment Buttons
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 8.dp),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        listOf(10_000_000L to "+ ۱۰ م", 50_000_000L to "+ ۵۰ م", 100_000_000L to "+ ۱۰۰ م").forEach { (increment, label) ->
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = colors.surface,
                                                border = BorderStroke(0.5.dp, colors.border),
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clickable {
                                                        val cur = PersianNumberFormatter.parseToCleanLong(cashAmountInput) ?: 0L
                                                        cashAmountInput = (cur + increment).toString()
                                                    }
                                            ) {
                                                Text(
                                                    text = label,
                                                    fontSize = 10.5.sp,
                                                    color = colors.textSecondary,
                                                    textAlign = TextAlign.Center,
                                                    fontFamily = VazirmatnFamily,
                                                    modifier = Modifier.padding(vertical = 5.dp)
                                                )
                                            }
                                        }

                                        // Settle entire balance button
                                        if (customer.cashDebtTomans != 0L) {
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = colors.goldContainer,
                                                border = BorderStroke(0.5.dp, colors.goldBorder),
                                                modifier = Modifier
                                                    .weight(1.3f)
                                                    .clickable {
                                                        cashAmountInput = Math.abs(customer.cashDebtTomans).toString()
                                                    }
                                            ) {
                                                Text(
                                                    text = "تسویه کل مانده",
                                                    fontSize = 10.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = colors.goldPrimary,
                                                    textAlign = TextAlign.Center,
                                                    fontFamily = VazirmatnFamily,
                                                    modifier = Modifier.padding(vertical = 5.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                // Destination Bank & Tracking Code
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    GoldInputField(
                                        value = destinationBank,
                                        onValueChange = { destinationBank = it },
                                        label = "حساب بانکی مقصد",
                                        keyboardType = KeyboardType.Text,
                                        modifier = Modifier.weight(1.3f)
                                    )

                                    GoldInputField(
                                        value = trackingCodeInput,
                                        onValueChange = { trackingCodeInput = it },
                                        label = "کد رهگیری / ارجاع",
                                        keyboardType = KeyboardType.Text,
                                        useThousandsSeparator = false,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }

                // 6. Note / Description Input
                GoldInputField(
                    value = noteInput,
                    onValueChange = { noteInput = it },
                    label = "توضیحات و بابت سند",
                    keyboardType = KeyboardType.Text,
                    modifier = Modifier.fillMaxWidth()
                )

                // 7. Projected Balance Preview Card
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = colors.surfaceElevated,
                    border = BorderStroke(0.6.dp, colors.border),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (isGoldMode) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "مانده طلای قبلی:",
                                    fontSize = 11.sp,
                                    color = colors.textMuted,
                                    fontFamily = VazirmatnFamily
                                )
                                Text(
                                    text = "${PersianNumberFormatter.formatWeight(customer.goldDebtGrams)} گرم ۷۵۰",
                                    fontSize = 11.5.sp,
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
                                    text = if (isReceive) "دریافت طلای آبشده:" else "تحویل طلا به مشتری:",
                                    fontSize = 11.sp,
                                    color = if (isReceive) colors.profitGreen else colors.errorRed,
                                    fontFamily = VazirmatnFamily
                                )
                                AnimatedNumberText(
                                    text = "${if (isReceive) "- " else "+ "}${PersianNumberFormatter.formatWeight(equivalent750Grams)}",
                                    unit = "گرم ۷۵۰",
                                    color = if (isReceive) colors.profitGreen else colors.errorRed,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(0.6.dp)
                                    .background(colors.border)
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "مانده جدید پس از ثبت سند:",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.textMain,
                                    fontFamily = VazirmatnFamily
                                )
                                AnimatedNumberText(
                                    text = PersianNumberFormatter.formatWeight(newProjectedGoldBalance),
                                    unit = "گرم ۷۵۰",
                                    color = colors.goldPrimary,
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "مانده ریالی قبلی:",
                                    fontSize = 11.sp,
                                    color = colors.textMuted,
                                    fontFamily = VazirmatnFamily
                                )
                                Text(
                                    text = "${PersianNumberFormatter.formatPrice(customer.cashDebtTomans)} تومان",
                                    fontSize = 11.5.sp,
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
                                    text = if (isReceive) "دریافت وجه نقد:" else "پرداخت وجه به طرف‌حساب:",
                                    fontSize = 11.sp,
                                    color = if (isReceive) colors.profitGreen else colors.errorRed,
                                    fontFamily = VazirmatnFamily
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = if (isReceive) "- " else "+ ",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isReceive) colors.profitGreen else colors.errorRed,
                                        fontFamily = VazirmatnFamily
                                    )
                                    AnimatedPriceText(
                                        amount = cashAmountLong,
                                        unit = "تومان",
                                        color = if (isReceive) colors.profitGreen else colors.errorRed,
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(0.6.dp)
                                    .background(colors.border)
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "مانده جدید ریالی پس از ثبت:",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.textMain,
                                    fontFamily = VazirmatnFamily
                                )
                                AnimatedPriceText(
                                    amount = newProjectedCashBalance,
                                    unit = "تومان",
                                    color = colors.goldPrimary,
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                    }
                }
            }

            HorizontalDivider(color = colors.border.copy(alpha = 0.5f), thickness = 0.6.dp)

            // 3. Fixed Bottom Footer Bar (Fix 5: Docked at bottom with navigationBarsPadding)
            Surface(
                color = colors.surfaceElevated,
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
            ) {
                // Action Buttons (Strictly adhering to dialog-button-layout.md invariant):
                // In RTL Row:
                // 1. First child = Secondary Action (انصراف) on visual RIGHT
                // 2. Second child = Primary Action (ذخیره سند) on visual LEFT
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    GoldButton(
                        text = "انصراف",
                        onClick = onDismiss,
                        isSecondary = true,
                        modifier = Modifier.weight(1f)
                    )

                    GoldButton(
                        text = "ذخیره سند",
                        onClick = {
                            val tx = if (isGoldMode) {
                                LedgerTransaction(
                                    id = UUID.randomUUID().toString(),
                                    customerId = customer.id,
                                    documentNumber = documentNumber,
                                    title = if (isReceive) "دریافت طلای $goldCategory" else "تحویل طلای $goldCategory",
                                    dateTime = currentDateStr,
                                    type = LedgerEntryType.GOLD_WEIGHT,
                                    direction = if (isReceive) LedgerDirection.RECEIVE else LedgerDirection.PAY,
                                    goldCategory = goldCategory,
                                    scaleWeightGrams = scaleWeightDouble,
                                    karat = karatInt,
                                    equivalent750WeightGrams = equivalent750Grams,
                                    angNumber = angNumberInput,
                                    labName = labNameInput,
                                    note = noteInput.ifBlank { "ثبت سند وزنی طلا در دفتر معین" },
                                    tagBadge = goldCategory,
                                    resultingGoldBalance = newProjectedGoldBalance,
                                    resultingCashBalance = customer.cashDebtTomans
                                )
                            } else {
                                LedgerTransaction(
                                    id = UUID.randomUUID().toString(),
                                    customerId = customer.id,
                                    documentNumber = documentNumber,
                                    title = if (isReceive) "دریافت وجه $paymentMethod" else "پرداخت وجه $paymentMethod",
                                    dateTime = currentDateStr,
                                    type = LedgerEntryType.CASH_RIAL,
                                    direction = if (isReceive) LedgerDirection.RECEIVE else LedgerDirection.PAY,
                                    amountTomans = cashAmountLong,
                                    paymentMethod = paymentMethod,
                                    destinationBank = destinationBank,
                                    trackingCode = trackingCodeInput,
                                    note = noteInput.ifBlank { "ثبت تسویه نقدی در دفتر معین" },
                                    tagBadge = paymentMethod.substringBefore(" "),
                                    resultingGoldBalance = customer.goldDebtGrams,
                                    resultingCashBalance = newProjectedCashBalance
                                )
                            }
                            onSaveEntry(tx)
                        },
                        enabled = isFormValid,
                        isSecondary = false,
                        icon = Icons.Default.Check,
                        modifier = Modifier.weight(1.5f)
                    )
                }
            }
        }
    }
}
