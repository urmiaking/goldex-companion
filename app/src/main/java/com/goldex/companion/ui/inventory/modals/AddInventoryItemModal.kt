package com.goldex.companion.ui.inventory.modals

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.filled.Refresh
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
import com.goldex.companion.data.MarketRates
import com.goldex.companion.model.InventoryCategory
import com.goldex.companion.model.InventoryItem
import com.goldex.companion.model.Karat
import com.goldex.companion.model.PersianNumberFormatter
import com.goldex.companion.model.WageType
import com.goldex.companion.ui.components.AnimatedPriceText
import com.goldex.companion.ui.components.GoldButton
import com.goldex.companion.ui.components.GoldInputField
import com.goldex.companion.ui.components.LuxurySegmentedControl
import com.goldex.companion.ui.invoices.components.InvoiceCloseVector
import com.goldex.companion.ui.theme.ButtonShape
import com.goldex.companion.ui.theme.LocalGoldExColors
import com.goldex.companion.ui.theme.VazirmatnFamily
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddInventoryItemModal(
    rates: MarketRates,
    onDismiss: () -> Unit,
    onSaveItem: (InventoryItem) -> Unit
) {
    val colors = LocalGoldExColors.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var selectedCategory by remember { mutableStateOf(InventoryCategory.RINGS) }

    fun generateCode(cat: InventoryCategory): String {
        val prefix = when (cat) {
            InventoryCategory.RINGS -> "RNG"
            InventoryCategory.BANGLES -> "BNG"
            InventoryCategory.NECKLACES -> "NCK"
            InventoryCategory.SETS -> "SET"
            InventoryCategory.JEWELRY -> "JWL"
            InventoryCategory.COINS -> "COIN"
            else -> "GLD"
        }
        val num = (100..999).random()
        return "$prefix-$num"
    }

    var codeInput by remember { mutableStateOf(generateCode(InventoryCategory.RINGS)) }
    var titleInput by remember { mutableStateOf("") }
    var rfidInput by remember { mutableStateOf("RF-${(1000..9999).random()}-GL") }
    var locationInput by remember { mutableStateOf("سینی شماره ۱ ویترین اصلی") }

    var grossWeightInput by remember { mutableStateOf("") }
    var stoneWeightInput by remember { mutableStateOf("") }

    var selectedKarat by remember { mutableStateOf(Karat.K18) }
    var isCustomKarat by remember { mutableStateOf(false) }
    var customKaratInput by remember { mutableStateOf("750") }

    var workshopInput by remember { mutableStateOf("کارگاه زرین تهران") }
    var wageType by remember { mutableStateOf(WageType.PERCENTAGE) }
    var wageInput by remember { mutableStateOf("12") }
    var quantityInput by remember { mutableStateOf("1") }

    val grossWeightDouble by remember(grossWeightInput) {
        derivedStateOf { PersianNumberFormatter.parseToCleanDouble(grossWeightInput) ?: 0.0 }
    }
    val stoneWeightDouble by remember(stoneWeightInput) {
        derivedStateOf { PersianNumberFormatter.parseToCleanDouble(stoneWeightInput) ?: 0.0 }
    }
    val netGoldWeightDouble by remember(grossWeightDouble, stoneWeightDouble) {
        derivedStateOf { (grossWeightDouble - stoneWeightDouble).coerceAtLeast(0.0) }
    }

    val wageDouble by remember(wageInput) {
        derivedStateOf { PersianNumberFormatter.parseToCleanDouble(wageInput) ?: 0.0 }
    }

    val quantityInt = remember(quantityInput) {
        PersianNumberFormatter.parseToCleanLong(quantityInput)?.toInt()?.coerceAtLeast(1) ?: 1
    }

    // Official Guild Profit & Tax
    val profitPercent = remember(selectedCategory) {
        when (selectedCategory) {
            InventoryCategory.JEWELRY -> 20.0
            InventoryCategory.COINS -> 0.0
            else -> 7.0
        }
    }
    val taxPercent = 9.0

    val effectiveKaratNum = remember(isCustomKarat, selectedKarat, customKaratInput) {
        if (isCustomKarat) {
            PersianNumberFormatter.parseToCleanLong(customKaratInput)?.toInt()?.coerceIn(100, 1000) ?: 750
        } else {
            when (selectedKarat) {
                Karat.K21 -> 875
                Karat.K24 -> 999
                else -> 750
            }
        }
    }

    // Live Estimation Calculation
    val spotPrice = remember(rates.gold18) { if (rates.gold18 > 0L) rates.gold18 else 23_360_000L }
    val estimatedValues by remember(netGoldWeightDouble, spotPrice, effectiveKaratNum, wageType, wageDouble, profitPercent) {
        derivedStateOf {
            val karatRatio = effectiveKaratNum.toDouble() / 750.0
            val rawGold = (netGoldWeightDouble * spotPrice.toDouble() * karatRatio).toLong()
            val wage = when (wageType) {
                WageType.PERCENTAGE -> (rawGold.toDouble() * (wageDouble / 100.0)).toLong()
                WageType.TOMAN_PER_GRAM -> (netGoldWeightDouble * wageDouble).toLong()
            }
            val profit = if (selectedCategory == InventoryCategory.COINS) 0L else ((rawGold + wage).toDouble() * (profitPercent / 100.0)).toLong()
            val tax = ((wage + profit).toDouble() * (taxPercent / 100.0)).toLong()
            val total = rawGold + wage + profit + tax
            object {
                val rawGoldTomans = rawGold
                val wageTomans = wage
                val profitTaxTomans = profit + tax
                val totalTomans = total
            }
        }
    }

    val isFormValid = titleInput.isNotBlank() && grossWeightDouble > 0.0

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.surface,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 10.dp, bottom = 6.dp)
                    .size(width = 44.dp, height = 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(colors.border)
            )
        }
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.92f)
            ) {
                // Fixed Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
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
                            Text(
                                text = "💎",
                                fontSize = 20.sp
                            )
                        }
                        Column {
                            Text(
                                text = "ثبت محصول در ویترین و انبار",
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textMain,
                                fontFamily = VazirmatnFamily
                            )
                            Text(
                                text = "مشخصات طلا، وزن دیجیتال و بارکد کالا",
                                fontSize = 10.5.sp,
                                color = colors.textSecondary,
                                fontFamily = VazirmatnFamily
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
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
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                HorizontalDivider(color = colors.border.copy(alpha = 0.4f), thickness = 0.7.dp)

                // Scrollable Body
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Category Selector Chips
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "دسته‌بندی زیورآلات و مصنوعات",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textSecondary,
                            fontFamily = VazirmatnFamily
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            InventoryCategory.entries.filter { it != InventoryCategory.ALL }.forEach { cat ->
                                val isSelected = selectedCategory == cat
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = if (isSelected) colors.goldPrimary else colors.surfaceElevated,
                                    border = BorderStroke(
                                        width = if (isSelected) 1.2.dp else 0.8.dp,
                                        color = if (isSelected) colors.goldPrimary else colors.border
                                    ),
                                    modifier = Modifier.clickable {
                                        selectedCategory = cat
                                        codeInput = generateCode(cat)
                                    }
                                ) {
                                    Text(
                                        text = cat.titleFa,
                                        fontSize = 10.5.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) Color.White else colors.textSecondary,
                                        fontFamily = VazirmatnFamily,
                                        modifier = Modifier.padding(horizontal = 11.dp, vertical = 5.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Product Code & RFID Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(modifier = Modifier.weight(1f)) {
                            GoldInputField(
                                value = codeInput,
                                onValueChange = { codeInput = it },
                                label = "کد محصول / اتیکت",
                                trailingText = null,
                                keyboardType = KeyboardType.Text
                            )
                            IconButton(
                                onClick = { codeInput = generateCode(selectedCategory) },
                                modifier = Modifier
                                    .align(Alignment.CenterEnd)
                                    .padding(top = 8.dp, end = 4.dp)
                                    .size(34.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "تولید کد جدید",
                                    tint = colors.goldPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Box(modifier = Modifier.weight(1f)) {
                            GoldInputField(
                                value = rfidInput,
                                onValueChange = { rfidInput = it },
                                label = "تگ RFID / بارکد خوان",
                                trailingText = null,
                                keyboardType = KeyboardType.Text
                            )
                        }
                    }

                    // Title Input
                    GoldInputField(
                        value = titleInput,
                        onValueChange = { titleInput = it },
                        label = "عنوان کامل کالا یا زیورآلات",
                        trailingText = null,
                        keyboardType = KeyboardType.Text
                    )

                    // Smart Weight Measurement Section
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = colors.surfaceElevated.copy(alpha = 0.5f),
                        border = BorderStroke(1.dp, colors.goldBorder.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(text = "⚖️", fontSize = 14.sp)
                                Text(
                                    text = "سنجش دقیق وزن دیجیتال",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.textMain,
                                    fontFamily = VazirmatnFamily
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(modifier = Modifier.weight(1f)) {
                                    GoldInputField(
                                        value = grossWeightInput,
                                        onValueChange = { grossWeightInput = it },
                                        label = "وزن کل ناخالص",
                                        trailingText = "گرم",
                                        isDecimal = true,
                                        keyboardType = KeyboardType.Decimal
                                    )
                                }
                                Box(modifier = Modifier.weight(1f)) {
                                    GoldInputField(
                                        value = stoneWeightInput,
                                        onValueChange = { stoneWeightInput = it },
                                        label = "کسر نگین",
                                        trailingText = "گرم",
                                        isDecimal = true,
                                        keyboardType = KeyboardType.Decimal
                                    )
                                }
                            }

                            // High-contrast Calculated Net Weight Banner
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFF141B2B),
                                border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.5f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "وزن خالص طلا",
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFFFE088),
                                            fontFamily = VazirmatnFamily
                                        )
                                        Text(
                                            text = "مبنای محاسبه مظنه و اجرت",
                                            fontSize = 9.5.sp,
                                            color = Color.White.copy(alpha = 0.7f),
                                            fontFamily = VazirmatnFamily
                                        )
                                    }
                                    AnimatedPriceText(
                                        amount = netGoldWeightDouble,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color(0xFFFBBF24),
                                        unit = "گرم"
                                    )
                                }
                            }
                        }
                    }

                    // Karat Selection with Custom Karat
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "عیار رسمی طلا",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textSecondary,
                                fontFamily = VazirmatnFamily
                            )
                            Text(
                                text = "مبنای عیار انتخابی: ${PersianNumberFormatter.toPersianDigits(effectiveKaratNum.toString())}",
                                fontSize = 10.sp,
                                color = colors.goldPrimary,
                                fontFamily = VazirmatnFamily
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val presets = listOf(
                                Triple("۱۸ (۷۵۰)", Karat.K18, 750),
                                Triple("۲۱ (۸۷۵)", Karat.K21, 875),
                                Triple("۲۴ (۹۹۹)", Karat.K24, 999)
                            )
                            presets.forEach { (label, karat, fineness) ->
                                val isSelected = !isCustomKarat && selectedKarat == karat
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSelected) colors.goldContainer else colors.surfaceElevated,
                                    border = BorderStroke(
                                        width = if (isSelected) 1.5.dp else 1.dp,
                                        color = if (isSelected) colors.goldPrimary else colors.border
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            selectedKarat = karat
                                            isCustomKarat = false
                                            customKaratInput = fineness.toString()
                                        }
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 10.5.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) colors.goldPrimary else colors.textMain,
                                        textAlign = TextAlign.Center,
                                        fontFamily = VazirmatnFamily,
                                        modifier = Modifier.padding(vertical = 7.dp)
                                    )
                                }
                            }

                            // Custom Karat Option
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isCustomKarat) colors.goldContainer else colors.surfaceElevated,
                                border = BorderStroke(
                                    width = if (isCustomKarat) 1.5.dp else 1.dp,
                                    color = if (isCustomKarat) colors.goldPrimary else colors.border
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { isCustomKarat = true }
                            ) {
                                Text(
                                    text = "سفارشی",
                                    fontSize = 10.5.sp,
                                    fontWeight = if (isCustomKarat) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isCustomKarat) colors.goldPrimary else colors.textMain,
                                    textAlign = TextAlign.Center,
                                    fontFamily = VazirmatnFamily,
                                    modifier = Modifier.padding(vertical = 7.dp)
                                )
                            }
                        }

                        if (isCustomKarat) {
                            GoldInputField(
                                value = customKaratInput,
                                onValueChange = { customKaratInput = it },
                                label = "عیار دلخواه (بر مبنای ۱۰۰۰، مثال: ۷۰۵، ۷۴۰)",
                                trailingText = "عیار",
                                keyboardType = KeyboardType.Number
                            )
                        }
                    }

                    // Workshop & Wage (With Percentage / Toman switcher)
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        GoldInputField(
                            value = workshopInput,
                            onValueChange = { workshopInput = it },
                            label = "کارگاه سازنده / بنکدار",
                            trailingText = null,
                            keyboardType = KeyboardType.Text
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "اجرت ساخت طلا",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textSecondary,
                                fontFamily = VazirmatnFamily
                            )

                            LuxurySegmentedControl(
                                items = listOf(WageType.PERCENTAGE, WageType.TOMAN_PER_GRAM),
                                selectedItem = wageType,
                                onItemSelected = { wageType = it },
                                label = { if (it == WageType.PERCENTAGE) "درصدی (٪)" else "تومانی / گرم" },
                                modifier = Modifier.width(176.dp),
                                height = 30.dp,
                                fontSize = 10.5.sp
                            )
                        }

                        GoldInputField(
                            value = wageInput,
                            onValueChange = { wageInput = it },
                            label = if (wageType == WageType.PERCENTAGE) "درصد اجرت ساخت" else "مبلغ اجرت ساخت هر گرم",
                            trailingText = if (wageType == WageType.PERCENTAGE) "٪" else "تومان",
                            isDecimal = wageType == WageType.PERCENTAGE,
                            keyboardType = if (wageType == WageType.PERCENTAGE) KeyboardType.Decimal else KeyboardType.Number
                        )
                    }

                    // Guild Policy Automatic Profit & Tax Banner
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = colors.surfaceElevated.copy(alpha = 0.7f),
                        border = BorderStroke(0.6.dp, colors.border),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 9.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "سود مصوب: ${PersianNumberFormatter.toPersianDigits(profitPercent.toInt().toString())}٪ ${if (selectedCategory == InventoryCategory.JEWELRY) "(جواهر)" else "(طلا)"}",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.profitGreen,
                                fontFamily = VazirmatnFamily
                            )
                            Text(
                                text = "مالیات بر ارزش‌افزوده: ۹٪ (روی اجرت و سود)",
                                fontSize = 10.sp,
                                color = colors.textMuted,
                                fontFamily = VazirmatnFamily
                            )
                        }
                    }

                    // Location & Quantity Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(modifier = Modifier.weight(1.5f)) {
                            GoldInputField(
                                value = locationInput,
                                onValueChange = { locationInput = it },
                                label = "محل نگهداری / شماره سینی ویترین",
                                trailingText = null,
                                keyboardType = KeyboardType.Text
                            )
                        }
                        Box(modifier = Modifier.weight(1f)) {
                            GoldInputField(
                                value = quantityInput,
                                onValueChange = { quantityInput = it },
                                label = "تعداد موجودی",
                                trailingText = "عدد",
                                keyboardType = KeyboardType.Number
                            )
                        }
                    }

                    // Luxury Live Calculation Breakdown Card
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFF131722),
                        border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.35f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "برآورد ارزش ریالی ویترین",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFBBF24),
                                    fontFamily = VazirmatnFamily
                                )
                                Text(
                                    text = "مظنه ۱۸: ${PersianNumberFormatter.formatPrice(spotPrice)} ت",
                                    fontSize = 10.sp,
                                    color = Color(0xFF94A3B8),
                                    fontFamily = VazirmatnFamily
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "قیمت نهایی فروش (با احتساب سود و مالیات):",
                                    fontSize = 11.sp,
                                    color = Color.White.copy(alpha = 0.9f),
                                    fontFamily = VazirmatnFamily
                                )
                                AnimatedPriceText(
                                    amount = estimatedValues.totalTomans,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFFFBBF24),
                                    unit = "تومان"
                                )
                            }

                            HorizontalDivider(
                                color = Color.White.copy(alpha = 0.1f),
                                thickness = 0.6.dp,
                                modifier = Modifier.padding(vertical = 2.dp)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(text = "ارزش طلای خام:", fontSize = 9.5.sp, color = Color(0xFF94A3B8), fontFamily = VazirmatnFamily)
                                    Text(
                                        text = "${PersianNumberFormatter.formatPrice(estimatedValues.rawGoldTomans)} ت",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        fontFamily = VazirmatnFamily
                                    )
                                }
                                Column {
                                    Text(text = "مبلغ اجرت ساخت:", fontSize = 9.5.sp, color = Color(0xFF94A3B8), fontFamily = VazirmatnFamily)
                                    Text(
                                        text = "${PersianNumberFormatter.formatPrice(estimatedValues.wageTomans)} ت",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF34D399),
                                        fontFamily = VazirmatnFamily
                                    )
                                }
                                Column {
                                    Text(text = "سود و مالیات:", fontSize = 9.5.sp, color = Color(0xFF94A3B8), fontFamily = VazirmatnFamily)
                                    Text(
                                        text = "${PersianNumberFormatter.formatPrice(estimatedValues.profitTaxTomans)} ت",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        fontFamily = VazirmatnFamily
                                    )
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(color = colors.border.copy(alpha = 0.4f), thickness = 0.7.dp)

                // Fixed Footer with Action Buttons (Rule: RTL Secondary on Right, Primary on Left)
                Surface(
                    color = colors.surface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Right child (Secondary / Cancel)
                        GoldButton(
                            text = "انصراف",
                            onClick = onDismiss,
                            isSecondary = true,
                            modifier = Modifier.weight(1f)
                        )

                        // Left child (Primary / Confirm)
                        GoldButton(
                            text = "ذخیره و صدور اتیکت بارکد",
                            onClick = {
                                val item = InventoryItem(
                                    id = UUID.randomUUID().toString(),
                                    code = codeInput.ifBlank { generateCode(selectedCategory) },
                                    title = titleInput.trim(),
                                    category = selectedCategory,
                                    location = locationInput.ifBlank { "سینی شماره ۱ ویترین اصلی" },
                                    grossWeightGrams = grossWeightDouble,
                                    stoneWeightGrams = stoneWeightDouble,
                                    karat = selectedKarat,
                                    customKaratValue = effectiveKaratNum,
                                    workshop = workshopInput.ifBlank { "کارگاه زرین تهران" },
                                    wageType = wageType,
                                    wageValue = wageDouble,
                                    wagePercent = if (wageType == WageType.PERCENTAGE) wageDouble else 0.0,
                                    profitPercent = profitPercent,
                                    taxPercent = taxPercent,
                                    rfidTag = rfidInput,
                                    quantity = quantityInt
                                )
                                onSaveItem(item)
                            },
                            enabled = isFormValid,
                            isSecondary = false,
                            icon = Icons.Default.Check,
                            modifier = Modifier.weight(2f)
                        )
                    }
                }
            }
        }
    }
}
