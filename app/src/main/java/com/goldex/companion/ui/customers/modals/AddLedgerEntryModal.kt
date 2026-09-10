package com.goldex.companion.ui.customers.modals

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.goldex.companion.model.Customer
import com.goldex.companion.model.LedgerDirection
import com.goldex.companion.model.LedgerEntryType
import com.goldex.companion.model.LedgerTransaction
import com.goldex.companion.model.PersianNumberFormatter
import com.goldex.companion.model.PersianWordsFormatter
import com.goldex.companion.ui.components.GoldButton
import com.goldex.companion.ui.components.LuxurySegmentedControl
import com.goldex.companion.ui.customers.LedgerAccountBalanceVector
import com.goldex.companion.ui.customers.LedgerArrowPayVector
import com.goldex.companion.ui.customers.LedgerArrowReceiveVector
import com.goldex.companion.ui.customers.LedgerScaleVector
import com.goldex.companion.ui.customers.LedgerVerifiedVector
import com.goldex.companion.ui.theme.LocalGoldExColors
import com.goldex.companion.ui.theme.VazirmatnFamily
import com.goldex.companion.ui.theme.VazirmatnFeatureSettings
import com.goldex.companion.ui.theme.goldGradient
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
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 18.dp)
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 1. Header Bar: Document #, Date, Close
                Row(
                    modifier = Modifier.fillMaxWidth(),
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
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(colors.goldGradient),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = customer.name.firstOrNull()?.toString() ?: "ط",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFF554300),
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

                // 3. Direction Selector (دریافت از طرف‌حساب vs پرداخت به طرف‌حساب)
                val directionOptions = listOf("دریافت از طرف‌حساب", "پرداخت به طرف‌حساب")
                LuxurySegmentedControl(
                    items = directionOptions,
                    selectedItem = directionOptions[selectedDirectionIndex],
                    onItemSelected = { selectedDirectionIndex = directionOptions.indexOf(it) },
                    label = { it },
                    modifier = Modifier.fillMaxWidth()
                )

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

                        // Inputs Box: Scale Weight & Karat
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
                                    // Scale Weight Input (Gram) - Numeric typed LTR per rule
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "وزن ترازو (گرم):",
                                            fontSize = 11.sp,
                                            color = colors.textMuted,
                                            fontFamily = VazirmatnFamily
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = colors.surface,
                                            border = BorderStroke(0.6.dp, colors.border)
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(44.dp)
                                                    .padding(horizontal = 10.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                BasicTextField(
                                                    value = scaleWeightInput,
                                                    onValueChange = { scaleWeightInput = it },
                                                    modifier = Modifier.weight(1f),
                                                    textStyle = TextStyle(
                                                        fontFamily = VazirmatnFamily,
                                                        fontFeatureSettings = VazirmatnFeatureSettings,
                                                        fontSize = 14.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = colors.textMain,
                                                        textDirection = TextDirection.Ltr
                                                    ),
                                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                                    cursorBrush = SolidColor(colors.goldPrimary),
                                                    singleLine = true
                                                )
                                                Text(
                                                    text = "گرم",
                                                    fontSize = 11.sp,
                                                    color = colors.textMuted,
                                                    fontFamily = VazirmatnFamily
                                                )
                                            }
                                        }
                                    }

                                    // Karat Input
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "عیار ری‌گیری:",
                                                fontSize = 11.sp,
                                                color = colors.textMuted,
                                                fontFamily = VazirmatnFamily
                                            )
                                            Text(
                                                text = "۷۵۰ استاندارد",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = colors.goldPrimary,
                                                fontFamily = VazirmatnFamily,
                                                modifier = Modifier
                                                    .clickable { karatInput = "750" }
                                                    .padding(horizontal = 4.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = colors.surface,
                                            border = BorderStroke(0.6.dp, colors.border)
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(44.dp)
                                                    .padding(horizontal = 10.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                BasicTextField(
                                                    value = karatInput,
                                                    onValueChange = { karatInput = it },
                                                    modifier = Modifier.weight(1f),
                                                    textStyle = TextStyle(
                                                        fontFamily = VazirmatnFamily,
                                                        fontFeatureSettings = VazirmatnFeatureSettings,
                                                        fontSize = 14.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = colors.textMain,
                                                        textDirection = TextDirection.Ltr
                                                    ),
                                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                    cursorBrush = SolidColor(colors.goldPrimary),
                                                    singleLine = true
                                                )
                                                Text(
                                                    text = "عیار",
                                                    fontSize = 11.sp,
                                                    color = colors.textMuted,
                                                    fontFamily = VazirmatnFamily
                                                )
                                            }
                                        }
                                    }
                                }

                                // Ang Number & Lab Name
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "شماره قبض / اَنگ:",
                                            fontSize = 11.sp,
                                            color = colors.textMuted,
                                            fontFamily = VazirmatnFamily
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = colors.surface,
                                            border = BorderStroke(0.6.dp, colors.border)
                                        ) {
                                            BasicTextField(
                                                value = angNumberInput,
                                                onValueChange = { angNumberInput = it },
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(40.dp)
                                                    .padding(horizontal = 10.dp, vertical = 10.dp),
                                                textStyle = TextStyle(
                                                    fontFamily = VazirmatnFamily,
                                                    fontSize = 12.sp,
                                                    color = colors.textMain,
                                                    textDirection = TextDirection.Ltr
                                                ),
                                                cursorBrush = SolidColor(colors.goldPrimary),
                                                singleLine = true
                                            )
                                        }
                                    }

                                    Column(modifier = Modifier.weight(1.3f)) {
                                        Text(
                                            text = "آزمایشگاه ری‌گیری:",
                                            fontSize = 11.sp,
                                            color = colors.textMuted,
                                            fontFamily = VazirmatnFamily
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = colors.surface,
                                            border = BorderStroke(0.6.dp, colors.border)
                                        ) {
                                            BasicTextField(
                                                value = labNameInput,
                                                onValueChange = { labNameInput = it },
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(40.dp)
                                                    .padding(horizontal = 10.dp, vertical = 10.dp),
                                                textStyle = TextStyle(
                                                    fontFamily = VazirmatnFamily,
                                                    fontSize = 12.sp,
                                                    color = colors.textMain
                                                ),
                                                cursorBrush = SolidColor(colors.goldPrimary),
                                                singleLine = true
                                            )
                                        }
                                    }
                                }

                                // Obsidian Calculation Monitor Card
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
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = PersianNumberFormatter.formatWeight(equivalent750Grams),
                                                    fontSize = 17.sp,
                                                    fontWeight = FontWeight.Black,
                                                    color = Color.White,
                                                    fontFamily = VazirmatnFamily
                                                )
                                                Spacer(modifier = Modifier.size(3.dp))
                                                Text(
                                                    text = "گرم",
                                                    fontSize = 11.sp,
                                                    color = Color(0xFF94A3B8),
                                                    fontFamily = VazirmatnFamily
                                                )
                                            }
                                            Text(
                                                text = "کسر عیار: ${PersianNumberFormatter.formatWeight(karatDeltaGrams)} گرم",
                                                fontSize = 10.sp,
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
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "مبلغ واریزی / دریافتی:",
                                            fontSize = 11.sp,
                                            color = colors.textMuted,
                                            fontFamily = VazirmatnFamily
                                        )
                                        if (cashInWords.isNotBlank()) {
                                            Text(
                                                text = cashInWords,
                                                fontSize = 10.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = colors.goldPrimary,
                                                fontFamily = VazirmatnFamily
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = colors.surface,
                                        border = BorderStroke(0.6.dp, colors.border)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(46.dp)
                                                .padding(horizontal = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            BasicTextField(
                                                value = cashAmountInput,
                                                onValueChange = { cashAmountInput = it },
                                                modifier = Modifier.weight(1f),
                                                textStyle = TextStyle(
                                                    fontFamily = VazirmatnFamily,
                                                    fontFeatureSettings = VazirmatnFeatureSettings,
                                                    fontSize = 16.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = colors.textMain,
                                                    textDirection = TextDirection.Ltr
                                                ),
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                cursorBrush = SolidColor(colors.goldPrimary),
                                                singleLine = true
                                            )
                                            Text(
                                                text = "تومان",
                                                fontSize = 11.sp,
                                                color = colors.textMuted,
                                                fontFamily = VazirmatnFamily
                                            )
                                        }
                                    }

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
                                    Column(modifier = Modifier.weight(1.3f)) {
                                        Text(
                                            text = "حساب بانکی مقصد:",
                                            fontSize = 11.sp,
                                            color = colors.textMuted,
                                            fontFamily = VazirmatnFamily
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = colors.surface,
                                            border = BorderStroke(0.6.dp, colors.border)
                                        ) {
                                            BasicTextField(
                                                value = destinationBank,
                                                onValueChange = { destinationBank = it },
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(40.dp)
                                                    .padding(horizontal = 10.dp, vertical = 10.dp),
                                                textStyle = TextStyle(
                                                    fontFamily = VazirmatnFamily,
                                                    fontSize = 12.sp,
                                                    color = colors.textMain
                                                ),
                                                cursorBrush = SolidColor(colors.goldPrimary),
                                                singleLine = true
                                            )
                                        }
                                    }

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "کد رهگیری / ارجاع:",
                                            fontSize = 11.sp,
                                            color = colors.textMuted,
                                            fontFamily = VazirmatnFamily
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = colors.surface,
                                            border = BorderStroke(0.6.dp, colors.border)
                                        ) {
                                            BasicTextField(
                                                value = trackingCodeInput,
                                                onValueChange = { trackingCodeInput = it },
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(40.dp)
                                                    .padding(horizontal = 10.dp, vertical = 10.dp),
                                                textStyle = TextStyle(
                                                    fontFamily = VazirmatnFamily,
                                                    fontSize = 12.sp,
                                                    color = colors.textMain,
                                                    textDirection = TextDirection.Ltr
                                                ),
                                                cursorBrush = SolidColor(colors.goldPrimary),
                                                singleLine = true
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 6. Note / Description Input
                Column {
                    Text(
                        text = "توضیحات و بابت سند:",
                        fontSize = 11.sp,
                        color = colors.textMuted,
                        fontFamily = VazirmatnFamily
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = colors.surfaceElevated,
                        border = BorderStroke(0.6.dp, colors.border)
                    ) {
                        BasicTextField(
                            value = noteInput,
                            onValueChange = { noteInput = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(42.dp)
                                .padding(horizontal = 10.dp, vertical = 10.dp),
                            textStyle = TextStyle(
                                fontFamily = VazirmatnFamily,
                                fontSize = 12.sp,
                                color = colors.textMain
                            ),
                            cursorBrush = SolidColor(colors.goldPrimary),
                            singleLine = true
                        )
                    }
                }

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
                                horizontalArrangement = Arrangement.SpaceBetween
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
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = if (isReceive) "دریافت طلای آبشده:" else "تحویل طلا به مشتری:",
                                    fontSize = 11.sp,
                                    color = if (isReceive) colors.profitGreen else colors.errorRed,
                                    fontFamily = VazirmatnFamily
                                )
                                Text(
                                    text = "${if (isReceive) "- " else "+ "}${PersianNumberFormatter.formatWeight(equivalent750Grams)} گرم ۷۵۰",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isReceive) colors.profitGreen else colors.errorRed,
                                    fontFamily = VazirmatnFamily
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
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "مانده جدید پس از ثبت سند:",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.textMain,
                                    fontFamily = VazirmatnFamily
                                )
                                Text(
                                    text = "${PersianNumberFormatter.formatWeight(newProjectedGoldBalance)} گرم ۷۵۰",
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Black,
                                    color = colors.goldPrimary,
                                    fontFamily = VazirmatnFamily
                                )
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
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
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = if (isReceive) "دریافت وجه نقد:" else "پرداخت وجه به طرف‌حساب:",
                                    fontSize = 11.sp,
                                    color = if (isReceive) colors.profitGreen else colors.errorRed,
                                    fontFamily = VazirmatnFamily
                                )
                                Text(
                                    text = "${if (isReceive) "- " else "+ "}${PersianNumberFormatter.formatPrice(cashAmountLong)} تومان",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isReceive) colors.profitGreen else colors.errorRed,
                                    fontFamily = VazirmatnFamily
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
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "مانده جدید ریالی پس از ثبت:",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.textMain,
                                    fontFamily = VazirmatnFamily
                                )
                                Text(
                                    text = "${PersianNumberFormatter.formatPrice(newProjectedCashBalance)} تومان",
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Black,
                                    color = colors.goldPrimary,
                                    fontFamily = VazirmatnFamily
                                )
                            }
                        }
                    }
                }

                // 8. Action Buttons (Strictly adhering to dialog-button-layout.md invariant):
                // RTL Row:
                // 1. First child = Secondary Action (انصراف) on visual RIGHT
                // 2. Second child = Primary Action (ذخیره سند) on visual LEFT
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp, bottom = 16.dp),
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
                        isSecondary = false,
                        icon = Icons.Default.Check,
                        modifier = Modifier.weight(1.5f)
                    )
                }
            }
        }
    }
}
