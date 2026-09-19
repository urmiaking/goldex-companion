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
import androidx.compose.ui.graphics.Brush
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
import com.goldex.companion.ui.components.GoldButton
import com.goldex.companion.ui.components.GoldInputField
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
    var customKaratInput by remember { mutableStateOf("750") }

    var workshopInput by remember { mutableStateOf("کارگاه زرین تهران") }
    var wagePercentInput by remember { mutableStateOf("12") }
    var profitPercentInput by remember { mutableStateOf("7") }
    var taxPercentInput by remember { mutableStateOf("9") }
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

    val wagePercentDouble by remember(wagePercentInput) {
        derivedStateOf { PersianNumberFormatter.parseToCleanDouble(wagePercentInput) ?: 0.0 }
    }
    val profitPercentDouble by remember(profitPercentInput) {
        derivedStateOf { PersianNumberFormatter.parseToCleanDouble(profitPercentInput) ?: 7.0 }
    }
    val taxPercentDouble by remember(taxPercentInput) {
        derivedStateOf { PersianNumberFormatter.parseToCleanDouble(taxPercentInput) ?: 9.0 }
    }
    val quantityInt = remember(quantityInput) {
        PersianNumberFormatter.parseToCleanLong(quantityInput)?.toInt()?.coerceAtLeast(1) ?: 1
    }

    // Live Estimation Calculation
    val spotPrice = remember(rates.gold18) { if (rates.gold18 > 0L) rates.gold18 else 23_360_000L }
    val estimatedValues by remember(netGoldWeightDouble, spotPrice, selectedKarat, wagePercentDouble, profitPercentDouble, taxPercentDouble) {
        derivedStateOf {
            val karatRatio = selectedKarat.purityRatio / Karat.K18.purityRatio
            val rawGold = (netGoldWeightDouble * spotPrice.toDouble() * karatRatio).toLong()
            val wage = (rawGold.toDouble() * (wagePercentDouble / 100.0)).toLong()
            val profit = ((rawGold + wage).toDouble() * (profitPercentDouble / 100.0)).toLong()
            val tax = ((wage + profit).toDouble() * (taxPercentDouble / 100.0)).toLong()
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
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header
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
                            Text(
                                text = "💎",
                                fontSize = 20.sp
                            )
                        }
                        Column {
                            Text(
                                text = "ثبت محصول در ویترین و انبار",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textMain,
                                fontFamily = VazirmatnFamily
                            )
                            Text(
                                text = "مشخصات طلا، وزن و بارکد کالا",
                                fontSize = 11.sp,
                                color = colors.textSecondary,
                                fontFamily = VazirmatnFamily
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(colors.surfaceElevated)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "بستن",
                            tint = colors.textSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                HorizontalDivider(color = colors.border.copy(alpha = 0.5f), thickness = 0.7.dp)

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
                        listOf(
                            InventoryCategory.RINGS,
                            InventoryCategory.BANGLES,
                            InventoryCategory.NECKLACES,
                            InventoryCategory.SETS,
                            InventoryCategory.COINS,
                            InventoryCategory.MISC
                        ).forEach { category ->
                            val isSelected = selectedCategory == category
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) colors.goldPrimary else colors.surfaceElevated,
                                border = BorderStroke(
                                    width = 1.dp,
                                    color = if (isSelected) colors.goldPrimary else colors.border
                                ),
                                modifier = Modifier.clickable {
                                    selectedCategory = category
                                    if (codeInput.isBlank() || codeInput.startsWith("RNG") || codeInput.startsWith("BNG") || codeInput.startsWith("NCK") || codeInput.startsWith("SET") || codeInput.startsWith("COIN")) {
                                        codeInput = generateCode(category)
                                    }
                                }
                            ) {
                                Text(
                                    text = category.titleFa,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Color.White else colors.textMain,
                                    fontFamily = VazirmatnFamily,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }

                // Code & RFID Row
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
                                .align(Alignment.CenterStart)
                                .padding(top = 18.dp, start = 4.dp)
                                .size(28.dp)
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
                                    keyboardType = KeyboardType.Decimal
                                )
                            }
                            Box(modifier = Modifier.weight(1f)) {
                                GoldInputField(
                                    value = stoneWeightInput,
                                    onValueChange = { stoneWeightInput = it },
                                    label = "کسر نگین و ملحقات",
                                    trailingText = "گرم",
                                    keyboardType = KeyboardType.Decimal
                                )
                            }
                        }

                        // Calculated Net Weight Banner
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = colors.goldContainer,
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
                                Column {
                                    Text(
                                        text = "وزن خالص طلا",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.goldSecondary,
                                        fontFamily = VazirmatnFamily
                                    )
                                    Text(
                                        text = "مبنای محاسبه مظنه و اجرت",
                                        fontSize = 9.5.sp,
                                        color = colors.goldSecondary.copy(alpha = 0.8f),
                                        fontFamily = VazirmatnFamily
                                    )
                                }
                                Text(
                                    text = "${PersianNumberFormatter.formatWeight(netGoldWeightDouble)} گرم",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Black,
                                    color = colors.goldSecondary,
                                    fontFamily = VazirmatnFamily
                                )
                            }
                        }
                    }
                }

                // Karat Selection
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
                            text = "مبنای سیستمی: ۷۵۰ (۱۸ عیار)",
                            fontSize = 10.sp,
                            color = colors.textMuted,
                            fontFamily = VazirmatnFamily
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            Karat.K18 to "۷۵۰ (۱۸ عیار)",
                            Karat.K21 to "۸۷۵ (۲۱ عیار)",
                            Karat.K24 to "۹۹۹ (۲۴ عیار)"
                        ).forEach { (karat, label) ->
                            val isSelected = selectedKarat == karat
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) colors.goldContainer else colors.surfaceElevated,
                                border = BorderStroke(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) colors.goldPrimary else colors.border
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { selectedKarat = karat }
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
                    }
                }

                // Workshop & Margins
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
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(modifier = Modifier.weight(1f)) {
                            GoldInputField(
                                value = wagePercentInput,
                                onValueChange = { wagePercentInput = it },
                                label = "اجرت ساخت",
                                trailingText = "٪",
                                keyboardType = KeyboardType.Decimal
                            )
                        }
                        Box(modifier = Modifier.weight(1f)) {
                            GoldInputField(
                                value = profitPercentInput,
                                onValueChange = { profitPercentInput = it },
                                label = "سود مغازه",
                                trailingText = "٪",
                                keyboardType = KeyboardType.Decimal
                            )
                        }
                        Box(modifier = Modifier.weight(1f)) {
                            GoldInputField(
                                value = taxPercentInput,
                                onValueChange = { taxPercentInput = it },
                                label = "مالیات",
                                trailingText = "٪",
                                keyboardType = KeyboardType.Decimal
                            )
                        }
                    }
                }

                // Location & Quantity
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(modifier = Modifier.weight(2f)) {
                        GoldInputField(
                            value = locationInput,
                            onValueChange = { locationInput = it },
                            label = "محل استقرار در گالری",
                            trailingText = null,
                            keyboardType = KeyboardType.Text
                        )
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        GoldInputField(
                            value = quantityInput,
                            onValueChange = { quantityInput = it },
                            label = "تعداد قطعه",
                            trailingText = "عدد",
                            keyboardType = KeyboardType.Number
                        )
                    }
                }

                // Luxury Dark Estimation Card
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
                            Column {
                                Text(
                                    text = "ارزش تخمینی ویترین (مظنه روز)",
                                    fontSize = 11.sp,
                                    color = Color(0xFFFBBF24),
                                    fontFamily = VazirmatnFamily
                                )
                                Text(
                                    text = "${PersianNumberFormatter.formatPrice(estimatedValues.totalTomans)} تومان",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White,
                                    fontFamily = VazirmatnFamily
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFFF59E0B).copy(alpha = 0.15f),
                                border = BorderStroke(0.6.dp, Color(0xFFF59E0B).copy(alpha = 0.4f))
                            ) {
                                Text(
                                    text = "لیبل هوشمند حرارتی",
                                    fontSize = 9.5.sp,
                                    color = Color(0xFFFBBF24),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    fontFamily = VazirmatnFamily
                                )
                            }
                        }

                        HorizontalDivider(color = Color.White.copy(alpha = 0.1f), thickness = 0.6.dp)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(text = "طلای خام:", fontSize = 9.5.sp, color = Color(0xFF94A3B8), fontFamily = VazirmatnFamily)
                                Text(
                                    text = "${PersianNumberFormatter.formatPrice(estimatedValues.rawGoldTomans)} ت",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontFamily = VazirmatnFamily
                                )
                            }
                            Column {
                                Text(text = "اجرت ساخت:", fontSize = 9.5.sp, color = Color(0xFF94A3B8), fontFamily = VazirmatnFamily)
                                Text(
                                    text = "${PersianNumberFormatter.formatPrice(estimatedValues.wageTomans)} ت",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
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

                // Two-Action Footer Buttons (Rule: RTL Secondary on Right, Primary on Left)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
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
                                workshop = workshopInput.ifBlank { "کارگاه زرین تهران" },
                                wagePercent = wagePercentDouble,
                                profitPercent = profitPercentDouble,
                                taxPercent = taxPercentDouble,
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

                Spacer(modifier = Modifier.height(10.dp))
            }
        }
    }
}
