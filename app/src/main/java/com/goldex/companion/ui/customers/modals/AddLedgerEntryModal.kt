package com.goldex.companion.ui.customers.modals

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
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
import com.goldex.companion.ui.components.LuxurySegmentedControl
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

    // =========================================================================
    // Gold State
    // =========================================================================
    var goldCategory by remember { mutableStateOf("آبشده") } // آبشده, مصنوعات, سکه و شمش

    // 1. آبشده
    var scaleWeightInput by remember { mutableStateOf("50.410") }
    var karatInput by remember { mutableStateOf("750") }
    var angNumberInput by remember { mutableStateOf("") }
    var labNameInput by remember { mutableStateOf("ری‌گیری تهران") }

    // 2. مصنوعات
    var craftedTitleInput by remember { mutableStateOf("النگو") }
    var craftedGrossWeightInput by remember { mutableStateOf("15.250") }
    var craftedStoneWeightInput by remember { mutableStateOf("0.000") }
    var craftedKaratInput by remember { mutableStateOf("750") }
    var craftedWorkshopInput by remember { mutableStateOf("") }

    // 3. سکه و شمش
    var selectedCoinOrBar by remember { mutableStateOf("سکه تمام") } // سکه تمام, نیم سکه, ربع سکه, سکه گرمی, شمش طلا
    var coinCountInput by remember { mutableStateOf("1") }
    var coinSerialInput by remember { mutableStateOf("") }
    // شمش
    var barWeightInput by remember { mutableStateOf("10.000") }
    var barKaratInput by remember { mutableStateOf("995") }
    var barBrandInput by remember { mutableStateOf("پارس شمش") }
    var barSerialInput by remember { mutableStateOf("") }

    // =========================================================================
    // Cash State
    // =========================================================================
    var cashAmountInput by remember { mutableStateOf("25000000") }
    var paymentMethod by remember { mutableStateOf("حواله بانکی / پایا") } // حواله بانکی / پایا, چک صیادی, کارتخوان (POS), اسکناس نقد

    // 1. حواله بانکی / پایا
    var destinationBank by remember { mutableStateOf("بانک ملت - جاری طلافروشی") }
    var trackingCodeInput by remember { mutableStateOf("") }
    var shebaInput by remember { mutableStateOf("") }

    // 2. چک صیادی
    var sayadIdInput by remember { mutableStateOf("") }
    var chequeSerialInput by remember { mutableStateOf("") }
    var chequeDueDateInput by remember { mutableStateOf("۱۴۰۳/۰۸/۱۵") }
    var chequeBankInput by remember { mutableStateOf("بانک ملی") }
    var chequeIssuerInput by remember { mutableStateOf(customer.name) }

    // 3. کارتخوان (POS)
    var posTerminalInput by remember { mutableStateOf("کارتخوان ملت فروشگاه") }
    var cardLast4Input by remember { mutableStateOf("") }
    var posRrnInput by remember { mutableStateOf("") }

    // 4. اسکناس نقد
    var cashPersonInput by remember { mutableStateOf(customer.name) }
    var cashierReceiptInput by remember { mutableStateOf("صندوق اصلی فروشگاه") }
    var cashNoteDetailsInput by remember { mutableStateOf("") }

    // =========================================================================
    // Derived Calculations for Gold
    // =========================================================================
    // --- آبشده ---
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
    val meltedEquiv750 by remember(scaleWeightDouble, karatInt) {
        derivedStateOf {
            if (scaleWeightDouble > 0.0 && karatInt > 0) {
                (scaleWeightDouble * karatInt) / 750.0
            } else 0.0
        }
    }
    val meltedDeltaGrams by remember(scaleWeightDouble, meltedEquiv750) {
        derivedStateOf {
            scaleWeightDouble - meltedEquiv750
        }
    }

    // --- مصنوعات ---
    val craftedGrossDouble by remember(craftedGrossWeightInput) {
        derivedStateOf {
            craftedGrossWeightInput.trim().replace("٫", ".").toDoubleOrNull() ?: 0.0
        }
    }
    val craftedStoneDouble by remember(craftedStoneWeightInput) {
        derivedStateOf {
            craftedStoneWeightInput.trim().replace("٫", ".").toDoubleOrNull() ?: 0.0
        }
    }
    val craftedNetDouble by remember(craftedGrossDouble, craftedStoneDouble) {
        derivedStateOf {
            (craftedGrossDouble - craftedStoneDouble).coerceAtLeast(0.0)
        }
    }
    val craftedKaratInt by remember(craftedKaratInput) {
        derivedStateOf {
            craftedKaratInput.trim().toIntOrNull() ?: 750
        }
    }
    val craftedEquiv750 by remember(craftedNetDouble, craftedKaratInt) {
        derivedStateOf {
            if (craftedNetDouble > 0.0 && craftedKaratInt > 0) {
                (craftedNetDouble * craftedKaratInt) / 750.0
            } else 0.0
        }
    }

    // --- سکه و شمش ---
    val coinCountInt by remember(coinCountInput) {
        derivedStateOf {
            (coinCountInput.trim().toIntOrNull() ?: 1).coerceAtLeast(1)
        }
    }
    val coinUnitWeight = when (selectedCoinOrBar) {
        "سکه تمام" -> 8.133
        "نیم سکه" -> 4.066
        "ربع سکه" -> 2.033
        "سکه گرمی" -> 1.100
        else -> 8.133
    }
    val coinTotalWeight by remember(selectedCoinOrBar, coinCountInt) {
        derivedStateOf {
            coinCountInt * coinUnitWeight
        }
    }
    val coinEquiv750 by remember(coinTotalWeight) {
        derivedStateOf {
            (coinTotalWeight * 900.0) / 750.0
        }
    }

    val barWeightDouble by remember(barWeightInput) {
        derivedStateOf {
            barWeightInput.trim().replace("٫", ".").toDoubleOrNull() ?: 0.0
        }
    }
    val barKaratInt by remember(barKaratInput) {
        derivedStateOf {
            barKaratInput.trim().toIntOrNull() ?: 995
        }
    }
    val barEquiv750 by remember(barWeightDouble, barKaratInt) {
        derivedStateOf {
            if (barWeightDouble > 0.0 && barKaratInt > 0) {
                (barWeightDouble * barKaratInt) / 750.0
            } else 0.0
        }
    }

    // --- مقادیر تجمیعی طلای معین ---
    val equivalent750Grams by remember(
        goldCategory,
        meltedEquiv750,
        craftedEquiv750,
        selectedCoinOrBar,
        coinEquiv750,
        barEquiv750
    ) {
        derivedStateOf {
            when (goldCategory) {
                "آبشده" -> meltedEquiv750
                "مصنوعات" -> craftedEquiv750
                "سکه و شمش" -> if (selectedCoinOrBar == "شمش طلا") barEquiv750 else coinEquiv750
                else -> meltedEquiv750
            }
        }
    }

    val effectivePhysicalWeight by remember(
        goldCategory,
        scaleWeightDouble,
        craftedGrossDouble,
        selectedCoinOrBar,
        coinTotalWeight,
        barWeightDouble
    ) {
        derivedStateOf {
            when (goldCategory) {
                "آبشده" -> scaleWeightDouble
                "مصنوعات" -> craftedGrossDouble
                "سکه و شمش" -> if (selectedCoinOrBar == "شمش طلا") barWeightDouble else coinTotalWeight
                else -> scaleWeightDouble
            }
        }
    }

    // =========================================================================
    // Derived Calculations for Cash
    // =========================================================================
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

    val isFormValid by remember(isGoldMode, effectivePhysicalWeight, cashAmountLong) {
        derivedStateOf {
            if (isGoldMode) effectivePhysicalWeight > 0.0 else cashAmountLong > 0L
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

                // 4. Settlement Mode Segmented Control (تسویه وزنی و طلا vs تسویه نقدی و ریالی)
                LuxurySegmentedControl(
                    items = listOf(0, 1),
                    selectedItem = selectedModeIndex,
                    onItemSelected = { selectedModeIndex = it },
                    label = { mode -> if (mode == 0) "تسویه وزنی و طلا" else "تسویه نقدی و ریالی" },
                    modifier = Modifier.fillMaxWidth(),
                    height = 42.dp,
                    fontSize = 12.sp
                )

                // =========================================================================
                // 5A. GOLD MODE FORM (Screen 1)
                // =========================================================================
                if (isGoldMode) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        // Category Switching Box (Segmented Control)
                        LuxurySegmentedControl(
                            items = listOf("آبشده", "مصنوعات", "سکه و شمش"),
                            selectedItem = goldCategory,
                            onItemSelected = { goldCategory = it },
                            label = { it },
                            modifier = Modifier.fillMaxWidth(),
                            height = 36.dp,
                            fontSize = 11.5.sp
                        )

                        // Inputs Box: Conditional based on goldCategory
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
                                when (goldCategory) {
                                    "آبشده" -> {
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

                                            GoldInputField(
                                                value = karatInput,
                                                onValueChange = { karatInput = it },
                                                label = "عیار ری‌گیری",
                                                trailingText = "عیار",
                                                isDecimal = false,
                                                useThousandsSeparator = false,
                                                keyboardType = KeyboardType.Number,
                                                modifier = Modifier.weight(1f)
                                            )
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

                                        // Obsidian Calculation Monitor Card with Animated Numbers
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
                                                        text = "کسر عیار: ${PersianNumberFormatter.formatWeight(meltedDeltaGrams)}",
                                                        unit = "گرم",
                                                        color = Color(0xFFCBD5E1),
                                                        fontSize = 10.5.sp,
                                                        fontWeight = FontWeight.Normal
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    "مصنوعات" -> {
                                        GoldInputField(
                                            value = craftedTitleInput,
                                            onValueChange = { craftedTitleInput = it },
                                            label = "عنوان یا نوع مصنوع (النگو، سرویس، دستبند...)",
                                            keyboardType = KeyboardType.Text,
                                            modifier = Modifier.fillMaxWidth()
                                        )

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            GoldInputField(
                                                value = craftedGrossWeightInput,
                                                onValueChange = { craftedGrossWeightInput = it },
                                                label = "وزن ناخالص",
                                                trailingText = "گرم",
                                                isDecimal = true,
                                                useThousandsSeparator = false,
                                                keyboardType = KeyboardType.Decimal,
                                                modifier = Modifier.weight(1f)
                                            )

                                            GoldInputField(
                                                value = craftedStoneWeightInput,
                                                onValueChange = { craftedStoneWeightInput = it },
                                                label = "کسر نگین و متعلقات",
                                                trailingText = "گرم",
                                                isDecimal = true,
                                                useThousandsSeparator = false,
                                                keyboardType = KeyboardType.Decimal,
                                                modifier = Modifier.weight(1f)
                                            )
                                        }

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            GoldInputField(
                                                value = craftedKaratInput,
                                                onValueChange = { craftedKaratInput = it },
                                                label = "عیار مصنوع طلا",
                                                trailingText = "عیار",
                                                isDecimal = false,
                                                useThousandsSeparator = false,
                                                keyboardType = KeyboardType.Number,
                                                modifier = Modifier.weight(1f)
                                            )

                                            GoldInputField(
                                                value = craftedWorkshopInput,
                                                onValueChange = { craftedWorkshopInput = it },
                                                label = "کد کارگاه / مدل",
                                                useThousandsSeparator = false,
                                                keyboardType = KeyboardType.Text,
                                                modifier = Modifier.weight(1.3f)
                                            )
                                        }

                                        // Obsidian Calculation Monitor Card for Crafted Gold
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
                                                        text = "وزن خالص: ${PersianNumberFormatter.formatWeight(craftedNetDouble)} گرم (عیار ${PersianNumberFormatter.toPersianDigits(craftedKaratInt.toString())})",
                                                        fontSize = 10.5.sp,
                                                        color = Color(0xFF94A3B8),
                                                        fontFamily = VazirmatnFamily
                                                    )
                                                    Text(
                                                        text = "وزن معادل ۷۵۰ در دفتر:",
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(0xFFFFE088),
                                                        fontFamily = VazirmatnFamily
                                                    )
                                                }
                                                Column(horizontalAlignment = Alignment.End) {
                                                    AnimatedNumberText(
                                                        text = PersianNumberFormatter.formatWeight(craftedEquiv750),
                                                        unit = "گرم",
                                                        color = Color.White,
                                                        fontSize = 17.sp,
                                                        fontWeight = FontWeight.Black
                                                    )
                                                    if (craftedStoneDouble > 0.0) {
                                                        Text(
                                                            text = "کسر نگین: ${PersianNumberFormatter.formatWeight(craftedStoneDouble)} گرم",
                                                            fontSize = 10.sp,
                                                            color = Color(0xFFCBD5E1),
                                                            fontFamily = VazirmatnFamily
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    "سکه و شمش" -> {
                                        // Sub-selection of Coin or Bar (Full Width Switching Control - Issue #86)
                                        LuxurySegmentedControl(
                                            items = listOf("سکه تمام", "نیم سکه", "ربع سکه", "سکه گرمی", "شمش طلا"),
                                            selectedItem = selectedCoinOrBar,
                                            onItemSelected = { selectedCoinOrBar = it },
                                            label = { it },
                                            modifier = Modifier.fillMaxWidth(),
                                            height = 36.dp,
                                            fontSize = 10.5.sp
                                        )

                                        if (selectedCoinOrBar != "شمش طلا") {
                                            // Coin specific fields
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                GoldInputField(
                                                    value = coinCountInput,
                                                    onValueChange = { coinCountInput = it },
                                                    label = "تعداد سکه",
                                                    trailingText = "عدد",
                                                    isDecimal = false,
                                                    useThousandsSeparator = false,
                                                    keyboardType = KeyboardType.Number,
                                                    modifier = Modifier.weight(1f)
                                                )

                                                Surface(
                                                    shape = RoundedCornerShape(12.dp),
                                                    color = colors.surface,
                                                    border = BorderStroke(0.6.dp, colors.border),
                                                    modifier = Modifier.weight(1.3f)
                                                ) {
                                                    Column(modifier = Modifier.padding(10.dp)) {
                                                        Text(
                                                            text = "وزن کل فیزیکی سکه‌ها:",
                                                            fontSize = 10.sp,
                                                            color = colors.textMuted,
                                                            fontFamily = VazirmatnFamily
                                                        )
                                                        Text(
                                                            text = "${PersianNumberFormatter.formatWeight(coinTotalWeight)} گرم (عیار ۹۰۰)",
                                                            fontSize = 12.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = colors.textMain,
                                                            fontFamily = VazirmatnFamily
                                                        )
                                                    }
                                                }
                                            }

                                            GoldInputField(
                                                value = coinSerialInput,
                                                onValueChange = { coinSerialInput = it },
                                                label = "شماره پلمپ",
                                                keyboardType = KeyboardType.Text,
                                                modifier = Modifier.fillMaxWidth()
                                            )

                                            // Obsidian Calculation Monitor Card for Coins
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
                                                            text = "${PersianNumberFormatter.toPersianDigits(coinCountInt.toString())} عدد ${selectedCoinOrBar} (هر واحد ${PersianNumberFormatter.formatWeight(coinUnitWeight)}g)",
                                                            fontSize = 10.5.sp,
                                                            color = Color(0xFF94A3B8),
                                                            fontFamily = VazirmatnFamily
                                                        )
                                                        Text(
                                                            text = "معادل ۷۵۰ در حساب مشتری:",
                                                            fontSize = 12.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = Color(0xFFFFE088),
                                                            fontFamily = VazirmatnFamily
                                                        )
                                                    }
                                                    Column(horizontalAlignment = Alignment.End) {
                                                        AnimatedNumberText(
                                                            text = PersianNumberFormatter.formatWeight(coinEquiv750),
                                                            unit = "گرم ۷۵۰",
                                                            color = Color.White,
                                                            fontSize = 17.sp,
                                                            fontWeight = FontWeight.Black
                                                        )
                                                        Text(
                                                            text = "فرمول: (وزن × ۹۰۰) ÷ ۷۵۰",
                                                            fontSize = 9.5.sp,
                                                            color = Color(0xFFCBD5E1),
                                                            fontFamily = VazirmatnFamily
                                                        )
                                                    }
                                                }
                                            }
                                        } else {
                                            // Gold Bar specific fields
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                                            ) {
                                                GoldInputField(
                                                    value = barWeightInput,
                                                    onValueChange = { barWeightInput = it },
                                                    label = "وزن شمش",
                                                    trailingText = "گرم",
                                                    isDecimal = true,
                                                    useThousandsSeparator = false,
                                                    keyboardType = KeyboardType.Decimal,
                                                    modifier = Modifier.weight(1f)
                                                )

                                                GoldInputField(
                                                    value = barKaratInput,
                                                    onValueChange = { barKaratInput = it },
                                                    label = "عیار شمش",
                                                    trailingText = "عیار",
                                                    isDecimal = false,
                                                    useThousandsSeparator = false,
                                                    keyboardType = KeyboardType.Number,
                                                    modifier = Modifier.weight(1f)
                                                )
                                            }

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                                            ) {
                                                GoldInputField(
                                                    value = barBrandInput,
                                                    onValueChange = { barBrandInput = it },
                                                    label = "برند / سازنده شمش",
                                                    useThousandsSeparator = false,
                                                    keyboardType = KeyboardType.Text,
                                                    modifier = Modifier.weight(1.2f)
                                                )

                                                GoldInputField(
                                                    value = barSerialInput,
                                                    onValueChange = { barSerialInput = it },
                                                    label = "شماره پلمپ",
                                                    useThousandsSeparator = false,
                                                    keyboardType = KeyboardType.Text,
                                                    modifier = Modifier.weight(1f)
                                                )
                                            }

                                            // Obsidian Calculation Monitor Card for Gold Bar
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
                                                            text = "شمش ${barBrandInput.ifBlank { "استاندارد" }} • فیزیکی: ${PersianNumberFormatter.formatWeight(barWeightDouble)}g",
                                                            fontSize = 10.5.sp,
                                                            color = Color(0xFF94A3B8),
                                                            fontFamily = VazirmatnFamily
                                                        )
                                                        Text(
                                                            text = "معادل ۷۵۰ در حساب مشتری:",
                                                            fontSize = 12.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = Color(0xFFFFE088),
                                                            fontFamily = VazirmatnFamily
                                                        )
                                                    }
                                                    Column(horizontalAlignment = Alignment.End) {
                                                        AnimatedNumberText(
                                                            text = PersianNumberFormatter.formatWeight(barEquiv750),
                                                            unit = "گرم ۷۵۰",
                                                            color = Color.White,
                                                            fontSize = 17.sp,
                                                            fontWeight = FontWeight.Black
                                                        )
                                                        Text(
                                                            text = "فرمول: (وزن × $barKaratInt) ÷ ۷۵۰",
                                                            fontSize = 9.5.sp,
                                                            color = Color(0xFFCBD5E1),
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
                    }
                }

                // =========================================================================
                // 5B. CASH MODE FORM (Screen 2)
                // =========================================================================
                if (!isGoldMode) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        // Payment Method Switching Box (Segmented Control)
                        LuxurySegmentedControl(
                            items = listOf("حواله بانکی / پایا", "چک صیادی", "کارتخوان (POS)", "اسکناس نقد"),
                            selectedItem = paymentMethod,
                            onItemSelected = { paymentMethod = it },
                            label = { it },
                            modifier = Modifier.fillMaxWidth(),
                            height = 36.dp,
                            fontSize = 10.5.sp
                        )

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
                                // Cash Amount (Common for all cash methods)
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
                                        label = if (paymentMethod == "چک صیادی") "مبلغ چک" else "مبلغ پرداختی / دریافتی",
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

                                // Specialized fields per payment method
                                when (paymentMethod) {
                                    "حواله بانکی / پایا" -> {
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

                                        GoldInputField(
                                            value = shebaInput,
                                            onValueChange = { shebaInput = it },
                                            label = "شماره شبا یا شماره حساب (اختیاری)",
                                            keyboardType = KeyboardType.Text,
                                            useThousandsSeparator = false,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }

                                    "چک صیادی" -> {
                                        GoldInputField(
                                            value = sayadIdInput,
                                            onValueChange = { sayadIdInput = it },
                                            label = "شناسه صیادی (۱۶ رقم)",
                                            keyboardType = KeyboardType.Number,
                                            useThousandsSeparator = false,
                                            modifier = Modifier.fillMaxWidth()
                                        )

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            GoldInputField(
                                                value = chequeSerialInput,
                                                onValueChange = { chequeSerialInput = it },
                                                label = "سریال و سری چک",
                                                keyboardType = KeyboardType.Text,
                                                useThousandsSeparator = false,
                                                modifier = Modifier.weight(1f)
                                            )

                                            GoldInputField(
                                                value = chequeDueDateInput,
                                                onValueChange = { chequeDueDateInput = it },
                                                label = "تاریخ سررسید چک",
                                                keyboardType = KeyboardType.Text,
                                                useThousandsSeparator = false,
                                                modifier = Modifier.weight(1f)
                                            )
                                        }

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            GoldInputField(
                                                value = chequeBankInput,
                                                onValueChange = { chequeBankInput = it },
                                                label = "بانک صادرکننده",
                                                keyboardType = KeyboardType.Text,
                                                modifier = Modifier.weight(1f)
                                            )

                                            GoldInputField(
                                                value = chequeIssuerInput,
                                                onValueChange = { chequeIssuerInput = it },
                                                label = "صاحب حساب / صادرکننده",
                                                keyboardType = KeyboardType.Text,
                                                modifier = Modifier.weight(1.2f)
                                            )
                                        }
                                    }

                                    "کارتخوان (POS)" -> {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            GoldInputField(
                                                value = posTerminalInput,
                                                onValueChange = { posTerminalInput = it },
                                                label = "دستگاه / پایانه کارتخوان",
                                                keyboardType = KeyboardType.Text,
                                                modifier = Modifier.weight(1.3f)
                                            )

                                            GoldInputField(
                                                value = cardLast4Input,
                                                onValueChange = { cardLast4Input = it },
                                                label = "۴ رقم آخر کارت",
                                                keyboardType = KeyboardType.Number,
                                                useThousandsSeparator = false,
                                                modifier = Modifier.weight(0.9f)
                                            )
                                        }

                                        GoldInputField(
                                            value = posRrnInput,
                                            onValueChange = { posRrnInput = it },
                                            label = "شماره پیگیری / شماره ارجاع رسید (RRN)",
                                            keyboardType = KeyboardType.Text,
                                            useThousandsSeparator = false,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }

                                    "اسکناس نقد" -> {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            GoldInputField(
                                                value = cashPersonInput,
                                                onValueChange = { cashPersonInput = it },
                                                label = "نام تحویل‌دهنده / گیرنده",
                                                keyboardType = KeyboardType.Text,
                                                modifier = Modifier.weight(1.2f)
                                            )

                                            GoldInputField(
                                                value = cashierReceiptInput,
                                                onValueChange = { cashierReceiptInput = it },
                                                label = "صندوق / کد قبض نقد",
                                                keyboardType = KeyboardType.Text,
                                                modifier = Modifier.weight(1f)
                                            )
                                        }

                                        GoldInputField(
                                            value = cashNoteDetailsInput,
                                            onValueChange = { cashNoteDetailsInput = it },
                                            label = "شرح و جزئیات بسته نقدی (اختیاری)",
                                            keyboardType = KeyboardType.Text,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
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
                                    text = if (isReceive) "دریافت طلا ($goldCategory):" else "تحویل طلا به طرف‌حساب ($goldCategory):",
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
                                    text = if (isReceive) "دریافت ($paymentMethod):" else "پرداخت ($paymentMethod):",
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
                                when (goldCategory) {
                                    "آبشده" -> LedgerTransaction(
                                        id = UUID.randomUUID().toString(),
                                        customerId = customer.id,
                                        documentNumber = documentNumber,
                                        title = if (isReceive) "دریافت طلای آبشده" else "تحویل طلای آبشده",
                                        dateTime = currentDateStr,
                                        type = LedgerEntryType.GOLD_WEIGHT,
                                        direction = if (isReceive) LedgerDirection.RECEIVE else LedgerDirection.PAY,
                                        goldCategory = "آبشده",
                                        scaleWeightGrams = scaleWeightDouble,
                                        karat = karatInt,
                                        equivalent750WeightGrams = equivalent750Grams,
                                        angNumber = angNumberInput,
                                        labName = labNameInput,
                                        note = buildString {
                                            if (noteInput.isNotBlank()) append(noteInput)
                                            else append("ثبت طلای آبشده در دفتر معین")
                                            if (angNumberInput.isNotBlank()) append(" - شماره اَنگ: $angNumberInput")
                                            if (labNameInput.isNotBlank()) append(" ($labNameInput)")
                                        },
                                        tagBadge = "آبشده",
                                        resultingGoldBalance = newProjectedGoldBalance,
                                        resultingCashBalance = customer.cashDebtTomans
                                    )
                                    "مصنوعات" -> LedgerTransaction(
                                        id = UUID.randomUUID().toString(),
                                        customerId = customer.id,
                                        documentNumber = documentNumber,
                                        title = if (isReceive) "دریافت مصنوعات (${craftedTitleInput.ifBlank { "طلا" }})" else "تحویل مصنوعات (${craftedTitleInput.ifBlank { "طلا" }})",
                                        dateTime = currentDateStr,
                                        type = LedgerEntryType.GOLD_WEIGHT,
                                        direction = if (isReceive) LedgerDirection.RECEIVE else LedgerDirection.PAY,
                                        goldCategory = "مصنوعات",
                                        scaleWeightGrams = craftedNetDouble,
                                        karat = craftedKaratInt,
                                        equivalent750WeightGrams = equivalent750Grams,
                                        angNumber = craftedWorkshopInput,
                                        labName = craftedTitleInput,
                                        note = buildString {
                                            append("مصنوعات: ${craftedTitleInput.ifBlank { "طلا" }} - ناخالص: ${PersianNumberFormatter.formatWeight(craftedGrossDouble)}g")
                                            if (craftedStoneDouble > 0) append(" (کسر نگین: ${PersianNumberFormatter.formatWeight(craftedStoneDouble)}g)")
                                            if (craftedWorkshopInput.isNotBlank()) append(" - کد کارگاه: $craftedWorkshopInput")
                                            if (noteInput.isNotBlank()) append(" - $noteInput")
                                        },
                                        tagBadge = "مصنوعات",
                                        resultingGoldBalance = newProjectedGoldBalance,
                                        resultingCashBalance = customer.cashDebtTomans
                                    )
                                    "سکه و شمش" -> {
                                        if (selectedCoinOrBar != "شمش طلا") {
                                            LedgerTransaction(
                                                id = UUID.randomUUID().toString(),
                                                customerId = customer.id,
                                                documentNumber = documentNumber,
                                                title = if (isReceive) "دریافت $selectedCoinOrBar (${PersianNumberFormatter.toPersianDigits(coinCountInt.toString())} عدد)" else "تحویل $selectedCoinOrBar (${PersianNumberFormatter.toPersianDigits(coinCountInt.toString())} عدد)",
                                                dateTime = currentDateStr,
                                                type = LedgerEntryType.GOLD_WEIGHT,
                                                direction = if (isReceive) LedgerDirection.RECEIVE else LedgerDirection.PAY,
                                                goldCategory = "سکه و شمش",
                                                scaleWeightGrams = coinTotalWeight,
                                                karat = 900,
                                                equivalent750WeightGrams = equivalent750Grams,
                                                angNumber = coinSerialInput,
                                                labName = selectedCoinOrBar,
                                                note = buildString {
                                                    append("$selectedCoinOrBar به تعداد ${PersianNumberFormatter.toPersianDigits(coinCountInt.toString())} عدد (وزن فیزیکی: ${PersianNumberFormatter.formatWeight(coinTotalWeight)}g)")
                                                    if (coinSerialInput.isNotBlank()) append(" - پلمپ: $coinSerialInput")
                                                    if (noteInput.isNotBlank()) append(" - $noteInput")
                                                },
                                                tagBadge = selectedCoinOrBar,
                                                resultingGoldBalance = newProjectedGoldBalance,
                                                resultingCashBalance = customer.cashDebtTomans
                                            )
                                        } else {
                                            LedgerTransaction(
                                                id = UUID.randomUUID().toString(),
                                                customerId = customer.id,
                                                documentNumber = documentNumber,
                                                title = if (isReceive) "دریافت شمش طلا (${barBrandInput.ifBlank { "استاندارد" }})" else "تحویل شمش طلا (${barBrandInput.ifBlank { "استاندارد" }})",
                                                dateTime = currentDateStr,
                                                type = LedgerEntryType.GOLD_WEIGHT,
                                                direction = if (isReceive) LedgerDirection.RECEIVE else LedgerDirection.PAY,
                                                goldCategory = "سکه و شمش",
                                                scaleWeightGrams = barWeightDouble,
                                                karat = barKaratInt,
                                                equivalent750WeightGrams = equivalent750Grams,
                                                angNumber = barSerialInput,
                                                labName = barBrandInput,
                                                note = buildString {
                                                    append("شمش طلا ${barBrandInput.ifBlank { "استاندارد" }} عیار $barKaratInt (وزن فیزیکی: ${PersianNumberFormatter.formatWeight(barWeightDouble)}g)")
                                                    if (barSerialInput.isNotBlank()) append(" - پلمپ: $barSerialInput")
                                                    if (noteInput.isNotBlank()) append(" - $noteInput")
                                                },
                                                tagBadge = "شمش طلا",
                                                resultingGoldBalance = newProjectedGoldBalance,
                                                resultingCashBalance = customer.cashDebtTomans
                                            )
                                        }
                                    }
                                    else -> LedgerTransaction(
                                        id = UUID.randomUUID().toString(),
                                        customerId = customer.id,
                                        documentNumber = documentNumber,
                                        title = if (isReceive) "دریافت طلای $goldCategory" else "تحویل طلای $goldCategory",
                                        dateTime = currentDateStr,
                                        type = LedgerEntryType.GOLD_WEIGHT,
                                        direction = if (isReceive) LedgerDirection.RECEIVE else LedgerDirection.PAY,
                                        goldCategory = goldCategory,
                                        scaleWeightGrams = effectivePhysicalWeight,
                                        karat = karatInt,
                                        equivalent750WeightGrams = equivalent750Grams,
                                        angNumber = angNumberInput,
                                        labName = labNameInput,
                                        note = noteInput.ifBlank { "ثبت سند وزنی طلا در دفتر معین" },
                                        tagBadge = goldCategory,
                                        resultingGoldBalance = newProjectedGoldBalance,
                                        resultingCashBalance = customer.cashDebtTomans
                                    )
                                }
                            } else {
                                when (paymentMethod) {
                                    "حواله بانکی / پایا" -> LedgerTransaction(
                                        id = UUID.randomUUID().toString(),
                                        customerId = customer.id,
                                        documentNumber = documentNumber,
                                        title = if (isReceive) "دریافت حواله بانکی" else "پرداخت حواله بانکی",
                                        dateTime = currentDateStr,
                                        type = LedgerEntryType.CASH_RIAL,
                                        direction = if (isReceive) LedgerDirection.RECEIVE else LedgerDirection.PAY,
                                        amountTomans = cashAmountLong,
                                        paymentMethod = paymentMethod,
                                        destinationBank = destinationBank,
                                        trackingCode = trackingCodeInput,
                                        note = buildString {
                                            append("حواله بانکی: $destinationBank")
                                            if (shebaInput.isNotBlank()) append(" - شبا/حساب: $shebaInput")
                                            if (trackingCodeInput.isNotBlank()) append(" - پیگیری: $trackingCodeInput")
                                            if (noteInput.isNotBlank()) append(" - $noteInput")
                                        },
                                        tagBadge = "حواله",
                                        resultingGoldBalance = customer.goldDebtGrams,
                                        resultingCashBalance = newProjectedCashBalance
                                    )
                                    "چک صیادی" -> LedgerTransaction(
                                        id = UUID.randomUUID().toString(),
                                        customerId = customer.id,
                                        documentNumber = documentNumber,
                                        title = if (isReceive) "دریافت چک صیادی (${chequeBankInput.ifBlank { "بانکی" }})" else "پرداخت چک صیادی (${chequeBankInput.ifBlank { "بانکی" }})",
                                        dateTime = currentDateStr,
                                        type = LedgerEntryType.CASH_RIAL,
                                        direction = if (isReceive) LedgerDirection.RECEIVE else LedgerDirection.PAY,
                                        amountTomans = cashAmountLong,
                                        paymentMethod = paymentMethod,
                                        destinationBank = "${chequeBankInput.ifBlank { "بانک" }} - صادرکننده: ${chequeIssuerInput.ifBlank { customer.name }}",
                                        trackingCode = sayadIdInput,
                                        note = buildString {
                                            append("چک صیادی: ${sayadIdInput.ifBlank { "—" }}")
                                            if (chequeSerialInput.isNotBlank()) append(" (سریال: $chequeSerialInput)")
                                            if (chequeDueDateInput.isNotBlank()) append(" - سررسید: $chequeDueDateInput")
                                            if (chequeBankInput.isNotBlank() || chequeIssuerInput.isNotBlank()) append(" - بانک ${chequeBankInput} / ${chequeIssuerInput}")
                                            if (noteInput.isNotBlank()) append(" - $noteInput")
                                        },
                                        tagBadge = "چک صیادی",
                                        resultingGoldBalance = customer.goldDebtGrams,
                                        resultingCashBalance = newProjectedCashBalance
                                    )
                                    "کارتخوان (POS)" -> LedgerTransaction(
                                        id = UUID.randomUUID().toString(),
                                        customerId = customer.id,
                                        documentNumber = documentNumber,
                                        title = if (isReceive) "دریافت از کارتخوان" else "پرداخت از کارتخوان",
                                        dateTime = currentDateStr,
                                        type = LedgerEntryType.CASH_RIAL,
                                        direction = if (isReceive) LedgerDirection.RECEIVE else LedgerDirection.PAY,
                                        amountTomans = cashAmountLong,
                                        paymentMethod = paymentMethod,
                                        destinationBank = posTerminalInput.ifBlank { "کارتخوان فروشگاه" },
                                        trackingCode = posRrnInput,
                                        note = buildString {
                                            append("کارتخوان: ${posTerminalInput.ifBlank { "فروشگاه" }}")
                                            if (cardLast4Input.isNotBlank()) append(" - ۴ رقم کارت: $cardLast4Input")
                                            if (posRrnInput.isNotBlank()) append(" - پیگیری: $posRrnInput")
                                            if (noteInput.isNotBlank()) append(" - $noteInput")
                                        },
                                        tagBadge = "کارتخوان",
                                        resultingGoldBalance = customer.goldDebtGrams,
                                        resultingCashBalance = newProjectedCashBalance
                                    )
                                    "اسکناس نقد" -> LedgerTransaction(
                                        id = UUID.randomUUID().toString(),
                                        customerId = customer.id,
                                        documentNumber = documentNumber,
                                        title = if (isReceive) "دریافت اسکناس نقد" else "پرداخت اسکناس نقد",
                                        dateTime = currentDateStr,
                                        type = LedgerEntryType.CASH_RIAL,
                                        direction = if (isReceive) LedgerDirection.RECEIVE else LedgerDirection.PAY,
                                        amountTomans = cashAmountLong,
                                        paymentMethod = paymentMethod,
                                        destinationBank = cashierReceiptInput.ifBlank { "صندوق طلافروشی" },
                                        trackingCode = "",
                                        note = buildString {
                                            append("اسکناس نقد: تحویل ${cashPersonInput.ifBlank { customer.name }}")
                                            if (cashierReceiptInput.isNotBlank()) append(" (${cashierReceiptInput})")
                                            if (cashNoteDetailsInput.isNotBlank()) append(" - $cashNoteDetailsInput")
                                            if (noteInput.isNotBlank()) append(" - $noteInput")
                                        },
                                        tagBadge = "نقد",
                                        resultingGoldBalance = customer.goldDebtGrams,
                                        resultingCashBalance = newProjectedCashBalance
                                    )
                                    else -> LedgerTransaction(
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
}
