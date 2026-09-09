package com.goldex.companion.ui.invoices.modals

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.goldex.companion.domain.invoice.BarterCalculationUseCases
import com.goldex.companion.model.BankCoinItem
import com.goldex.companion.model.BarterItem
import com.goldex.companion.model.CoinType
import com.goldex.companion.model.CraftedGoldItem
import com.goldex.companion.model.InvoiceItemCategory
import com.goldex.companion.model.Karat
import com.goldex.companion.model.MeltGoldItem
import com.goldex.companion.model.PersianNumberFormatter
import com.goldex.companion.model.ScrapGoldItem
import com.goldex.companion.model.WageType
import com.goldex.companion.ui.components.AnimatedNumberText
import com.goldex.companion.ui.components.AnimatedPriceText
import com.goldex.companion.ui.components.GoldButton
import com.goldex.companion.ui.components.GoldInputField
import com.goldex.companion.ui.components.LuxurySegmentedControl
import com.goldex.companion.ui.invoices.components.InvoiceCheckVector
import com.goldex.companion.ui.invoices.components.InvoiceCloseVector
import com.goldex.companion.ui.theme.LocalGoldExColors
import com.goldex.companion.ui.theme.VazirmatnFamily

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddInvoiceItemModal(
    spotPrice18k: Long,
    defaultCategory: InvoiceItemCategory = InvoiceItemCategory.CRAFTED,
    existingItem: BarterItem? = null,
    onDismiss: () -> Unit,
    onSaveItem: (BarterItem) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val colors = LocalGoldExColors.current

    var selectedCategory by remember {
        mutableStateOf(existingItem?.category ?: defaultCategory)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.surfaceElevated,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .width(44.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(colors.border.copy(alpha = 0.6f))
            )
        }
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 24.dp)
            ) {
                // Header Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = if (existingItem != null) "ویرایش قلم فاکتور" else "افزودن قلم به فاکتور",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textMain,
                            fontFamily = VazirmatnFamily
                        )
                        Text(
                            text = "مظنه ۱۸ عیار مبنا: ${PersianNumberFormatter.formatPrice(spotPrice18k)} تومان",
                            fontSize = 11.sp,
                            color = colors.goldPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = VazirmatnFamily
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(colors.surfaceVariant)
                    ) {
                        Icon(
                            imageVector = InvoiceCloseVector,
                            contentDescription = "بستن",
                            tint = colors.textSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Top Category Navigation Tabs (only selectable if adding new item)
                if (existingItem == null) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = colors.surfaceVariant,
                        border = BorderStroke(0.6.dp, colors.border),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            InvoiceItemCategory.entries.forEach { category ->
                                val isSelected = selectedCategory == category
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(38.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(
                                            if (isSelected) colors.goldPrimary else Color.Transparent
                                        )
                                        .clickable { selectedCategory = category },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = category.titleFa,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) Color.White else colors.textSecondary,
                                        fontFamily = VazirmatnFamily,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                }

                // Body based on category
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 520.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    AnimatedContent(
                        targetState = selectedCategory,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "categoryFormAnim"
                    ) { category ->
                        when (category) {
                            InvoiceItemCategory.CRAFTED -> {
                                CraftedGoldForm(
                                    spotPrice18k = spotPrice18k,
                                    existingItem = existingItem as? CraftedGoldItem,
                                    onDismiss = onDismiss,
                                    onConfirm = onSaveItem
                                )
                            }
                            InvoiceItemCategory.SCRAP -> {
                                ScrapGoldForm(
                                    spotPrice18k = spotPrice18k,
                                    existingItem = existingItem as? ScrapGoldItem,
                                    onDismiss = onDismiss,
                                    onConfirm = onSaveItem
                                )
                            }
                            InvoiceItemCategory.MELT -> {
                                MeltGoldForm(
                                    spotPrice18k = spotPrice18k,
                                    existingItem = existingItem as? MeltGoldItem,
                                    onDismiss = onDismiss,
                                    onConfirm = onSaveItem
                                )
                            }
                            InvoiceItemCategory.COIN -> {
                                BankCoinForm(
                                    existingItem = existingItem as? BankCoinItem,
                                    onDismiss = onDismiss,
                                    onConfirm = onSaveItem
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// 1. Crafted Gold Form (طلای ساخته)
// ---------------------------------------------------------------------------
@Composable
private fun CraftedGoldForm(
    spotPrice18k: Long,
    existingItem: CraftedGoldItem?,
    onDismiss: () -> Unit,
    onConfirm: (CraftedGoldItem) -> Unit
) {
    val colors = LocalGoldExColors.current

    var title by remember { mutableStateOf(existingItem?.title ?: "دستبند کارتیه ۱۸ عیار") }

    val initialIsCustom = existingItem?.let {
        it.customKaratValue !in listOf(750, 705, 999) && it.customKaratValue > 0
    } ?: false
    var isCustomKarat by remember { mutableStateOf(initialIsCustom) }
    var selectedKaratPreset by remember {
        mutableIntStateOf(if (initialIsCustom) -1 else (existingItem?.customKaratValue ?: 750))
    }
    var customKaratStr by remember {
        mutableStateOf(if (initialIsCustom) (existingItem?.customKaratValue?.toString() ?: "750") else "750")
    }

    var grossWeightStr by remember { mutableStateOf(existingItem?.grossWeight?.toString() ?: "12.80") }
    var stoneWeightStr by remember { mutableStateOf(existingItem?.stoneWeight?.toString() ?: "0.30") }
    var wageType by remember { mutableStateOf(existingItem?.wageType ?: WageType.PERCENTAGE) }
    var wageInputStr by remember { mutableStateOf(existingItem?.wageInput?.toString() ?: "7.5") }
    var profitStr by remember { mutableStateOf(existingItem?.profitPercent?.toString() ?: "7.0") }
    var taxStr by remember { mutableStateOf(existingItem?.taxPercent?.toString() ?: "9.0") }

    val grossWeight = grossWeightStr.toDoubleOrNull() ?: 0.0
    val stoneWeight = stoneWeightStr.toDoubleOrNull() ?: 0.0
    val wageInput = wageInputStr.toDoubleOrNull() ?: 0.0
    val profit = profitStr.toDoubleOrNull() ?: 0.0
    val tax = taxStr.toDoubleOrNull() ?: 0.0

    val effectiveKaratValue = if (isCustomKarat) {
        customKaratStr.toIntOrNull() ?: 750
    } else {
        selectedKaratPreset
    }

    val karatEnum = when (effectiveKaratValue) {
        750 -> Karat.K18
        999 -> Karat.K24
        else -> Karat.K18
    }

    val item = remember(title, karatEnum, effectiveKaratValue, grossWeight, stoneWeight, wageType, wageInput, profit, tax, spotPrice18k) {
        BarterCalculationUseCases.calculateCraftedItem(
            title = title,
            karat = karatEnum,
            customKaratValue = effectiveKaratValue,
            grossWeight = grossWeight,
            stoneWeight = stoneWeight,
            spotPrice18k = spotPrice18k,
            wageType = wageType,
            wageInput = wageInput,
            profitPercent = profit,
            taxPercent = tax
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        GoldInputField(
            value = title,
            onValueChange = { title = it },
            label = "شرح کالا یا زیورآلات",
            keyboardType = KeyboardType.Text,
            useThousandsSeparator = false,
            modifier = Modifier.fillMaxWidth()
        )

        // Karat Chips & Custom Karat
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "عیار استاندارد یا دلخواه",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.textSecondary,
                fontFamily = VazirmatnFamily
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(
                    Triple(750, "۷۵۰ (۱۸)", false),
                    Triple(705, "۷۰۵ (سنتی)", false),
                    Triple(999, "۹۹۹ (۲۴)", false),
                    Triple(-1, "عیار دلخواه", true)
                ).forEach { (karatVal, label, isCustom) ->
                    val isSelected = if (isCustom) isCustomKarat else (!isCustomKarat && selectedKaratPreset == karatVal)
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) colors.goldContainer.copy(alpha = 0.5f) else colors.surfaceVariant,
                        border = BorderStroke(if (isSelected) 1.2.dp else 0.6.dp, if (isSelected) colors.goldPrimary else colors.border),
                        modifier = Modifier
                            .weight(1f)
                            .height(34.dp)
                            .clickable {
                                if (isCustom) {
                                    isCustomKarat = true
                                } else {
                                    isCustomKarat = false
                                    selectedKaratPreset = karatVal
                                }
                            }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) colors.goldPrimary else colors.textMain,
                                fontFamily = VazirmatnFamily
                            )
                        }
                    }
                }
            }

            if (isCustomKarat) {
                GoldInputField(
                    value = customKaratStr,
                    onValueChange = { customKaratStr = it },
                    label = "عیار دلخواه (خط)",
                    trailingText = "خط",
                    isDecimal = false,
                    useThousandsSeparator = false,
                    keyboardType = KeyboardType.Number,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Weights: Gross & Stone
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GoldInputField(
                value = grossWeightStr,
                onValueChange = { grossWeightStr = it },
                label = "وزن ناخالص",
                trailingText = "گرم",
                isDecimal = true,
                useThousandsSeparator = false,
                keyboardType = KeyboardType.Decimal,
                modifier = Modifier.weight(1f)
            )
            GoldInputField(
                value = stoneWeightStr,
                onValueChange = { stoneWeightStr = it },
                label = "کسر نگین/موم",
                trailingText = "گرم",
                isDecimal = true,
                useThousandsSeparator = false,
                keyboardType = KeyboardType.Decimal,
                modifier = Modifier.weight(1f)
            )
        }

        // Net Weight Badge
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = colors.surfaceVariant,
            border = BorderStroke(0.6.dp, colors.border),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "وزن خالص محاسبه‌شده:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = colors.textSecondary,
                    fontFamily = VazirmatnFamily
                )
                AnimatedNumberText(
                    text = PersianNumberFormatter.formatWeight(item.netWeight),
                    unit = "گرم",
                    color = colors.goldPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Wage, Profit, Tax
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "نرخ‌ها، اجرت و مالیات",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textSecondary,
                    fontFamily = VazirmatnFamily
                )
                LuxurySegmentedControl(
                    items = listOf(WageType.PERCENTAGE, WageType.TOMAN_PER_GRAM),
                    selectedItem = wageType,
                    onItemSelected = { wageType = it },
                    label = { type ->
                        when (type) {
                            WageType.PERCENTAGE -> "درصدی (٪)"
                            WageType.TOMAN_PER_GRAM -> "تومان/گرم"
                        }
                    },
                    modifier = Modifier.width(160.dp),
                    height = 28.dp,
                    fontSize = 9.5.sp
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GoldInputField(
                    value = wageInputStr,
                    onValueChange = { wageInputStr = it },
                    label = if (wageType == WageType.PERCENTAGE) "اجرت ساخت" else "اجرت هر گرم",
                    trailingText = if (wageType == WageType.PERCENTAGE) "٪" else "تومان",
                    isDecimal = wageType == WageType.PERCENTAGE,
                    useThousandsSeparator = wageType == WageType.TOMAN_PER_GRAM,
                    keyboardType = if (wageType == WageType.PERCENTAGE) KeyboardType.Decimal else KeyboardType.Number,
                    modifier = Modifier.weight(1f)
                )
                GoldInputField(
                    value = profitStr,
                    onValueChange = { profitStr = it },
                    label = "سود فروشنده",
                    trailingText = "٪",
                    isDecimal = true,
                    useThousandsSeparator = false,
                    keyboardType = KeyboardType.Decimal,
                    modifier = Modifier.weight(1f)
                )
                GoldInputField(
                    value = taxStr,
                    onValueChange = { taxStr = it },
                    label = "مالیات قانونی",
                    trailingText = "٪",
                    isDecimal = true,
                    useThousandsSeparator = false,
                    keyboardType = KeyboardType.Decimal,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Live Breakdown Card with AnimatedPriceText & AnimatedNumberText
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = colors.surfaceVariant,
            border = BorderStroke(0.8.dp, colors.goldBorder),
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
                        text = "مبلغ کل ردیف فروش:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textMain,
                        fontFamily = VazirmatnFamily
                    )
                    AnimatedPriceText(
                        amount = item.totalPayable.toLong(),
                        unit = "تومان",
                        color = colors.goldPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "معادل وزنی ۱۸ عیار:",
                        fontSize = 11.sp,
                        color = colors.textSecondary,
                        fontFamily = VazirmatnFamily
                    )
                    AnimatedNumberText(
                        text = PersianNumberFormatter.formatWeight(item.equivalent18kWeight),
                        unit = "گرم",
                        color = colors.textMain,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ارزش طلای خام:",
                        fontSize = 11.sp,
                        color = colors.textSecondary,
                        fontFamily = VazirmatnFamily
                    )
                    AnimatedPriceText(
                        amount = item.rawGoldValue.toLong(),
                        unit = "تومان",
                        color = colors.textSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Action Buttons: Cancel on Right (first), Save on Left (second)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            GoldButton(
                text = "انصراف",
                onClick = onDismiss,
                isSecondary = true,
                modifier = Modifier.weight(0.35f)
            )
            GoldButton(
                text = if (existingItem != null) "ذخیره تغییرات" else "افزودن قلم به فاکتور",
                onClick = { onConfirm(item) },
                icon = InvoiceCheckVector,
                modifier = Modifier.weight(0.65f)
            )
        }
    }
}

// ---------------------------------------------------------------------------
// 2. Scrap Gold Form (طلای متفرقه و کهنه)
// ---------------------------------------------------------------------------
@Composable
private fun ScrapGoldForm(
    spotPrice18k: Long,
    existingItem: ScrapGoldItem?,
    onDismiss: () -> Unit,
    onConfirm: (ScrapGoldItem) -> Unit
) {
    val colors = LocalGoldExColors.current

    var title by remember { mutableStateOf(existingItem?.title ?: "طلای متفرقه و دست‌دوم") }
    val initialIsCustom = existingItem?.let {
        it.baseKarat !in listOf(750, 740, 705) && it.baseKarat > 0
    } ?: false
    var isCustomKarat by remember { mutableStateOf(initialIsCustom) }
    var selectedBaseKaratPreset by remember {
        mutableIntStateOf(if (initialIsCustom) -1 else (existingItem?.baseKarat ?: 750))
    }
    var customBaseKaratStr by remember {
        mutableStateOf(if (initialIsCustom) (existingItem?.baseKarat?.toString() ?: "750") else "750")
    }

    var deficitStr by remember { mutableStateOf(existingItem?.karatDeficit?.toString() ?: "15") }
    var grossWeightStr by remember { mutableStateOf(existingItem?.grossWeight?.toString() ?: "14.80") }
    var stoneWeightStr by remember { mutableStateOf(existingItem?.stoneWeight?.toString() ?: "0.30") }
    var deductionPerGramStr by remember { mutableStateOf(existingItem?.deductionPerGram?.toString() ?: "15000") }
    var commissionStr by remember { mutableStateOf(existingItem?.exchangeCommissionPercent?.toString() ?: "0.0") }

    val grossWeight = grossWeightStr.toDoubleOrNull() ?: 0.0
    val stoneWeight = stoneWeightStr.toDoubleOrNull() ?: 0.0
    val deficit = deficitStr.toIntOrNull() ?: 0
    val deductionPerGram = deductionPerGramStr.toLongOrNull() ?: 0L
    val commission = commissionStr.toDoubleOrNull() ?: 0.0

    val effectiveBaseKarat = if (isCustomKarat) {
        customBaseKaratStr.toIntOrNull() ?: 750
    } else {
        selectedBaseKaratPreset
    }

    val item = remember(title, effectiveBaseKarat, deficit, grossWeight, stoneWeight, spotPrice18k, deductionPerGram, commission) {
        BarterCalculationUseCases.calculateScrapItem(
            title = title,
            baseKarat = effectiveBaseKarat,
            karatDeficit = deficit,
            grossWeight = grossWeight,
            stoneWeight = stoneWeight,
            spotPrice18k = spotPrice18k,
            deductionPerGram = deductionPerGram,
            exchangeCommissionPercent = commission
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        GoldInputField(
            value = title,
            onValueChange = { title = it },
            label = "شرح کالا (دست‌دوم / شکسته)",
            keyboardType = KeyboardType.Text,
            useThousandsSeparator = false,
            modifier = Modifier.fillMaxWidth()
        )

        // Base Karat Row & Custom
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "عیار مبدا و کسری",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textSecondary,
                    fontFamily = VazirmatnFamily
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "عیار پرداختی: ",
                        fontSize = 11.sp,
                        color = colors.textSecondary,
                        fontFamily = VazirmatnFamily
                    )
                    AnimatedNumberText(
                        text = PersianNumberFormatter.toPersianDigits(item.payableKarat.toString()),
                        unit = "خط",
                        color = colors.goldPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(
                    Triple(750, "۷۵۰ (۱۸)", false),
                    Triple(740, "۷۴۰ (رایج)", false),
                    Triple(705, "۷۰۵ (سنتی)", false),
                    Triple(-1, "سایر عیار", true)
                ).forEach { (k, label, isCustom) ->
                    val isSelected = if (isCustom) isCustomKarat else (!isCustomKarat && selectedBaseKaratPreset == k)
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) colors.goldContainer.copy(alpha = 0.5f) else colors.surfaceVariant,
                        border = BorderStroke(if (isSelected) 1.2.dp else 0.6.dp, if (isSelected) colors.goldPrimary else colors.border),
                        modifier = Modifier
                            .weight(1f)
                            .height(34.dp)
                            .clickable {
                                if (isCustom) {
                                    isCustomKarat = true
                                } else {
                                    isCustomKarat = false
                                    selectedBaseKaratPreset = k
                                }
                            }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) colors.goldPrimary else colors.textMain,
                                fontFamily = VazirmatnFamily
                            )
                        }
                    }
                }
            }

            if (isCustomKarat) {
                GoldInputField(
                    value = customBaseKaratStr,
                    onValueChange = { customBaseKaratStr = it },
                    label = "عیار مبدا دلخواه (خط)",
                    trailingText = "خط",
                    isDecimal = false,
                    useThousandsSeparator = false,
                    keyboardType = KeyboardType.Number,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Deficit & Deduction & Commission
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GoldInputField(
                value = deficitStr,
                onValueChange = { deficitStr = it },
                label = "کسری عیار",
                trailingText = "خط",
                isDecimal = false,
                useThousandsSeparator = false,
                keyboardType = KeyboardType.Number,
                modifier = Modifier.weight(1f)
            )
            GoldInputField(
                value = deductionPerGramStr,
                onValueChange = { deductionPerGramStr = it },
                label = "کسر مظنه",
                trailingText = "تومان",
                isDecimal = false,
                useThousandsSeparator = true,
                keyboardType = KeyboardType.Number,
                modifier = Modifier.weight(1.3f)
            )
            GoldInputField(
                value = commissionStr,
                onValueChange = { commissionStr = it },
                label = "کارمزد تعویض",
                trailingText = "٪",
                isDecimal = true,
                useThousandsSeparator = false,
                keyboardType = KeyboardType.Decimal,
                modifier = Modifier.weight(1f)
            )
        }

        // Weights
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GoldInputField(
                value = grossWeightStr,
                onValueChange = { grossWeightStr = it },
                label = "وزن ناخالص",
                trailingText = "گرم",
                isDecimal = true,
                useThousandsSeparator = false,
                keyboardType = KeyboardType.Decimal,
                modifier = Modifier.weight(1f)
            )
            GoldInputField(
                value = stoneWeightStr,
                onValueChange = { stoneWeightStr = it },
                label = "کسر نگین/موم",
                trailingText = "گرم",
                isDecimal = true,
                useThousandsSeparator = false,
                keyboardType = KeyboardType.Decimal,
                modifier = Modifier.weight(1f)
            )
        }

        // Net Weight Badge
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = colors.surfaceVariant,
            border = BorderStroke(0.6.dp, colors.border),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "خالص طلا:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = colors.textSecondary,
                    fontFamily = VazirmatnFamily
                )
                AnimatedNumberText(
                    text = PersianNumberFormatter.formatWeight(item.netWeight),
                    unit = "گرم",
                    color = colors.goldPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Live Summary Card with AnimatedPriceText & AnimatedNumberText
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = colors.surfaceVariant,
            border = BorderStroke(0.8.dp, colors.goldBorder),
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
                        text = "مبلغ خرید متفرقه:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textMain,
                        fontFamily = VazirmatnFamily
                    )
                    AnimatedPriceText(
                        amount = item.totalPayable.toLong(),
                        unit = "تومان",
                        color = colors.goldPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "معادل وزنی ۱۸ عیار:",
                        fontSize = 11.sp,
                        color = colors.textSecondary,
                        fontFamily = VazirmatnFamily
                    )
                    AnimatedNumberText(
                        text = PersianNumberFormatter.formatWeight(item.equivalent18kWeight),
                        unit = "گرم",
                        color = colors.textMain,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "قیمت موثر هر گرم:",
                        fontSize = 11.sp,
                        color = colors.textSecondary,
                        fontFamily = VazirmatnFamily
                    )
                    AnimatedPriceText(
                        amount = item.effectiveGramPrice,
                        unit = "تومان",
                        color = colors.textSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Action Buttons: Cancel on Right (first), Save on Left (second)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            GoldButton(
                text = "انصراف",
                onClick = onDismiss,
                isSecondary = true,
                modifier = Modifier.weight(0.35f)
            )
            GoldButton(
                text = if (existingItem != null) "ذخیره تغییرات" else "افزودن قلم تهاتر",
                onClick = { onConfirm(item) },
                icon = InvoiceCheckVector,
                modifier = Modifier.weight(0.65f)
            )
        }
    }
}

// ---------------------------------------------------------------------------
// 3. Melted Gold Form (طلای آبشده سنتی)
// ---------------------------------------------------------------------------
@Composable
private fun MeltGoldForm(
    spotPrice18k: Long,
    existingItem: MeltGoldItem?,
    onDismiss: () -> Unit,
    onConfirm: (MeltGoldItem) -> Unit
) {
    val colors = LocalGoldExColors.current

    var title by remember { mutableStateOf(existingItem?.title ?: "طلای آبشده سنتی") }
    var weightStr by remember { mutableStateOf(existingItem?.weight?.toString() ?: "10.0") }

    val initialIsCustom = existingItem?.let {
        it.labKarat !in listOf(750, 735, 705) && it.labKarat > 0
    } ?: false
    var isCustomKarat by remember { mutableStateOf(initialIsCustom) }
    var selectedKaratPreset by remember {
        mutableIntStateOf(if (initialIsCustom) -1 else (existingItem?.labKarat ?: 735))
    }
    var customKaratStr by remember {
        mutableStateOf(if (initialIsCustom) (existingItem?.labKarat?.toString() ?: "735") else "735")
    }

    var angNumber by remember { mutableStateOf(existingItem?.angNumber ?: "1248") }
    var labName by remember { mutableStateOf(existingItem?.labName ?: "ری‌گیری مشهد") }

    val weight = weightStr.toDoubleOrNull() ?: 0.0
    val labKarat = if (isCustomKarat) {
        customKaratStr.toIntOrNull() ?: 750
    } else {
        selectedKaratPreset
    }

    val item = remember(title, weight, labKarat, angNumber, labName, spotPrice18k) {
        BarterCalculationUseCases.calculateMeltItem(
            title = title,
            weight = weight,
            labKarat = labKarat,
            angNumber = angNumber,
            labName = labName,
            spotPrice18k = spotPrice18k
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GoldInputField(
                value = angNumber,
                onValueChange = { angNumber = it },
                label = "شماره انگ خطی",
                keyboardType = KeyboardType.Text,
                useThousandsSeparator = false,
                modifier = Modifier.weight(1f)
            )
            GoldInputField(
                value = labName,
                onValueChange = { labName = it },
                label = "آزمایشگاه ری‌گیری",
                keyboardType = KeyboardType.Text,
                useThousandsSeparator = false,
                modifier = Modifier.weight(1f)
            )
        }

        GoldInputField(
            value = weightStr,
            onValueChange = { weightStr = it },
            label = "وزن ترازویی آبشده",
            trailingText = "گرم",
            isDecimal = true,
            useThousandsSeparator = false,
            keyboardType = KeyboardType.Decimal,
            modifier = Modifier.fillMaxWidth()
        )

        // Karat Selector & Custom
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "عیار آزمایشگاه ری‌گیری",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.textSecondary,
                fontFamily = VazirmatnFamily
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(
                    Triple(750, "۷۵۰ (۱۸)", false),
                    Triple(735, "۷۳۵ (رایج)", false),
                    Triple(705, "۷۰۵ (سنتی)", false),
                    Triple(-1, "عیار دلخواه", true)
                ).forEach { (k, label, isCustom) ->
                    val isSelected = if (isCustom) isCustomKarat else (!isCustomKarat && selectedKaratPreset == k)
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) colors.goldContainer.copy(alpha = 0.5f) else colors.surfaceVariant,
                        border = BorderStroke(if (isSelected) 1.2.dp else 0.6.dp, if (isSelected) colors.goldPrimary else colors.border),
                        modifier = Modifier
                            .weight(1f)
                            .height(34.dp)
                            .clickable {
                                if (isCustom) {
                                    isCustomKarat = true
                                } else {
                                    isCustomKarat = false
                                    selectedKaratPreset = k
                                }
                            }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) colors.goldPrimary else colors.textMain,
                                fontFamily = VazirmatnFamily
                            )
                        }
                    }
                }
            }

            if (isCustomKarat) {
                GoldInputField(
                    value = customKaratStr,
                    onValueChange = { customKaratStr = it },
                    label = "عیار دلخواه آزمایشگاه (خط)",
                    trailingText = "خط",
                    isDecimal = false,
                    useThousandsSeparator = false,
                    keyboardType = KeyboardType.Number,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Summary Card with AnimatedPriceText & AnimatedNumberText
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = colors.surfaceVariant,
            border = BorderStroke(0.8.dp, colors.goldBorder),
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
                        text = "ارزش کل آبشده:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textMain,
                        fontFamily = VazirmatnFamily
                    )
                    AnimatedPriceText(
                        amount = item.totalPayable.toLong(),
                        unit = "تومان",
                        color = colors.goldPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "وزن معادل ۱۸ عیار:",
                        fontSize = 11.sp,
                        color = colors.textSecondary,
                        fontFamily = VazirmatnFamily
                    )
                    AnimatedNumberText(
                        text = PersianNumberFormatter.formatWeight(item.equivalent18kWeight),
                        unit = "گرم",
                        color = colors.textMain,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Action Buttons: Cancel on Right (first), Save on Left (second)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            GoldButton(
                text = "انصراف",
                onClick = onDismiss,
                isSecondary = true,
                modifier = Modifier.weight(0.35f)
            )
            GoldButton(
                text = if (existingItem != null) "ذخیره تغییرات" else "افزودن آبشده به فاکتور",
                onClick = { onConfirm(item) },
                icon = InvoiceCheckVector,
                modifier = Modifier.weight(0.65f)
            )
        }
    }
}

// ---------------------------------------------------------------------------
// 4. Bank Coin Form (سکه بانکی)
// ---------------------------------------------------------------------------
@Composable
private fun BankCoinForm(
    existingItem: BankCoinItem?,
    onDismiss: () -> Unit,
    onConfirm: (BankCoinItem) -> Unit
) {
    val colors = LocalGoldExColors.current

    var selectedCoin by remember { mutableStateOf(existingItem?.coinType ?: CoinType.EMAMI) }
    var countStr by remember { mutableStateOf(existingItem?.count?.toString() ?: "1") }
    var hasHologram by remember { mutableStateOf(existingItem?.hasHologram ?: true) }
    var unitPriceStr by remember { mutableStateOf(existingItem?.unitPrice?.toString() ?: "42500000") }

    val count = countStr.toIntOrNull() ?: 1
    val unitPrice = unitPriceStr.toLongOrNull() ?: 0L

    val item = remember(selectedCoin, count, hasHologram, unitPrice) {
        BarterCalculationUseCases.calculateCoinItem(
            coinType = selectedCoin,
            count = count,
            hasHologram = hasHologram,
            unitPrice = unitPrice
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // Coin Type Chips
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "نوع سکه بانکی",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.textSecondary,
                fontFamily = VazirmatnFamily
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                CoinType.entries.forEach { c ->
                    val isSelected = selectedCoin == c
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) colors.goldContainer.copy(alpha = 0.5f) else colors.surfaceVariant,
                        border = BorderStroke(if (isSelected) 1.2.dp else 0.6.dp, if (isSelected) colors.goldPrimary else colors.border),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(34.dp)
                            .clickable { selectedCoin = c }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = c.titleFa,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) colors.goldPrimary else colors.textMain,
                                fontFamily = VazirmatnFamily
                            )
                            Text(
                                text = "${PersianNumberFormatter.formatWeight(c.totalWeightGrams)} گرم",
                                fontSize = 10.sp,
                                color = colors.textSecondary,
                                fontFamily = VazirmatnFamily
                            )
                        }
                    }
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GoldInputField(
                value = countStr,
                onValueChange = { countStr = it },
                label = "تعداد سکه",
                trailingText = "عدد",
                isDecimal = false,
                useThousandsSeparator = false,
                keyboardType = KeyboardType.Number,
                modifier = Modifier.weight(1f)
            )
            GoldInputField(
                value = unitPriceStr,
                onValueChange = { unitPriceStr = it },
                label = "قیمت واحد",
                trailingText = "تومان",
                isDecimal = false,
                useThousandsSeparator = true,
                keyboardType = KeyboardType.Number,
                modifier = Modifier.weight(1.8f)
            )
        }

        // Hologram Toggle
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
                Column {
                    Text(
                        text = "بسته‌بندی و هولوگرام معتبر",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textMain,
                        fontFamily = VazirmatnFamily
                    )
                    Text(
                        text = "دارای تاییدیه رسمی اتحادیه طلا و جواهر",
                        fontSize = 10.sp,
                        color = colors.textSecondary,
                        fontFamily = VazirmatnFamily
                    )
                }
                Switch(
                    checked = hasHologram,
                    onCheckedChange = { hasHologram = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = colors.goldPrimary,
                        checkedTrackColor = colors.goldContainer
                    )
                )
            }
        }

        // Summary Card with AnimatedPriceText & AnimatedNumberText
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = colors.surfaceVariant,
            border = BorderStroke(0.8.dp, colors.goldBorder),
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
                        text = "مبلغ کل سکه‌ها:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textMain,
                        fontFamily = VazirmatnFamily
                    )
                    AnimatedPriceText(
                        amount = item.totalPayable,
                        unit = "تومان",
                        color = colors.goldPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "معادل وزنی ۱۸ عیار:",
                        fontSize = 11.sp,
                        color = colors.textSecondary,
                        fontFamily = VazirmatnFamily
                    )
                    AnimatedNumberText(
                        text = PersianNumberFormatter.formatWeight(item.equivalent18kWeight),
                        unit = "گرم",
                        color = colors.textMain,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Action Buttons: Cancel on Right (first), Save on Left (second)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            GoldButton(
                text = "انصراف",
                onClick = onDismiss,
                isSecondary = true,
                modifier = Modifier.weight(0.35f)
            )
            GoldButton(
                text = if (existingItem != null) "ذخیره تغییرات" else "افزودن سکه به فاکتور",
                onClick = { onConfirm(item) },
                icon = InvoiceCheckVector,
                modifier = Modifier.weight(0.65f)
            )
        }
    }
}
