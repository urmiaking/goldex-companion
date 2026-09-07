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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.goldex.companion.ui.components.GoldButton
import com.goldex.companion.ui.invoices.components.InvoiceCheckVector
import com.goldex.companion.ui.invoices.components.InvoiceCloseVector
import com.goldex.companion.ui.theme.LocalGoldExColors
import com.goldex.companion.ui.theme.VazirmatnFamily
import com.goldex.companion.ui.theme.VazirmatnFeatureSettings
import com.goldex.companion.ui.theme.goldGradient

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
                    .background(colors.borderSubtle.copy(alpha = 0.6f))
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
                            .background(colors.surfaceContainer)
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
                        color = colors.surfaceContainer,
                        border = BorderStroke(0.6.dp, colors.borderSubtle),
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
    var selectedKarat by remember { mutableStateOf(existingItem?.karat ?: Karat.K18) }
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

    val item = remember(title, selectedKarat, grossWeight, stoneWeight, wageType, wageInput, profit, tax, spotPrice18k) {
        BarterCalculationUseCases.calculateCraftedItem(
            title = title,
            karat = selectedKarat,
            grossWeight = grossWeight,
            stoneWeight = stoneWeight,
            spotPrice18k = spotPrice18k,
            wageType = wageType,
            wageInput = wageInput,
            profitPercent = profit,
            taxPercent = tax
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Description Input
        FormInputBox(label = "شرح کالا یا زیورآلات") {
            LtrTextField(
                value = title,
                onValueChange = { title = it },
                modifier = Modifier.fillMaxWidth(),
                alignCenter = false
            )
        }

        // Karat Chips
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(text = "عیار استاندارد", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = colors.textSecondary)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Karat.entries.forEach { k ->
                    val isSelected = selectedKarat == k
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) colors.goldContainer.copy(alpha = 0.5f) else colors.surfaceContainer,
                        border = BorderStroke(if (isSelected) 1.2.dp else 0.6.dp, if (isSelected) colors.goldPrimary else colors.borderSubtle),
                        modifier = Modifier
                            .weight(1f)
                            .height(34.dp)
                            .clickable { selectedKarat = k }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = k.labelFa,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) colors.goldPrimary else colors.textMain
                            )
                        }
                    }
                }
            }
        }

        // Weights: Gross, Stone, Net
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FormInputBox(label = "وزن ناخالص (گرم)", modifier = Modifier.weight(1f)) {
                LtrTextField(value = grossWeightStr, onValueChange = { grossWeightStr = it })
            }
            FormInputBox(label = "کسر نگین/موم", modifier = Modifier.weight(1f)) {
                LtrTextField(value = stoneWeightStr, onValueChange = { stoneWeightStr = it })
            }
            FormInputBox(label = "وزن خالص", modifier = Modifier.weight(1f), isReadonly = true) {
                Text(
                    text = PersianNumberFormatter.formatWeight(item.netWeight),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.goldPrimary
                )
            }
        }

        // Wage & Profit & Tax
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FormInputBox(
                label = if (wageType == WageType.PERCENTAGE) "اجرت ساخت (٪)" else "اجرت (تومان/گرم)",
                modifier = Modifier.weight(1f)
            ) {
                LtrTextField(value = wageInputStr, onValueChange = { wageInputStr = it })
            }
            FormInputBox(label = "سود فروشنده (٪)", modifier = Modifier.weight(1f)) {
                LtrTextField(value = profitStr, onValueChange = { profitStr = it })
            }
            FormInputBox(label = "مالیات قانونی (٪)", modifier = Modifier.weight(1f)) {
                LtrTextField(value = taxStr, onValueChange = { taxStr = it })
            }
        }

        // Live Breakdown Card
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = colors.surfaceContainer,
            border = BorderStroke(0.8.dp, colors.goldBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "مبلغ کل ردیف فروش:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = colors.textMain)
                    Text(
                        text = "${PersianNumberFormatter.formatPrice(item.totalPayable)} تومان",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        color = colors.goldPrimary
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "معادل وزنی ۱۸ عیار:", fontSize = 11.sp, color = colors.textSecondary)
                    Text(
                        text = "${PersianNumberFormatter.formatWeight(item.equivalent18kWeight)} گرم",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textMain
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
    var baseKarat by remember { mutableIntStateOf(existingItem?.baseKarat ?: 750) }
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

    val item = remember(title, baseKarat, deficit, grossWeight, stoneWeight, spotPrice18k, deductionPerGram, commission) {
        BarterCalculationUseCases.calculateScrapItem(
            title = title,
            baseKarat = baseKarat,
            karatDeficit = deficit,
            grossWeight = grossWeight,
            stoneWeight = stoneWeight,
            spotPrice18k = spotPrice18k,
            deductionPerGram = deductionPerGram,
            exchangeCommissionPercent = commission
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        FormInputBox(label = "شرح کالا (دست‌دوم / شکسته)") {
            LtrTextField(
                value = title,
                onValueChange = { title = it },
                modifier = Modifier.fillMaxWidth(),
                alignCenter = false
            )
        }

        // Base Karat Row
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "عیار مبدا و کسری", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = colors.textSecondary)
                Text(
                    text = "پرداختی نهایی: ${PersianNumberFormatter.toPersianDigits(item.payableKarat.toString())}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.goldPrimary
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(750 to "۷۵۰ (۱۸)", 740 to "۷۴۰ (رایج)", 705 to "۷۰۵ (سنتی)").forEach { (k, label) ->
                    val isSelected = baseKarat == k
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) colors.goldContainer.copy(alpha = 0.5f) else colors.surfaceContainer,
                        border = BorderStroke(if (isSelected) 1.2.dp else 0.6.dp, if (isSelected) colors.goldPrimary else colors.borderSubtle),
                        modifier = Modifier
                            .weight(1f)
                            .height(34.dp)
                            .clickable { baseKarat = k }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) colors.goldPrimary else colors.textMain
                            )
                        }
                    }
                }
            }
        }

        // Deficit & Deduction
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FormInputBox(label = "کسری عیار (خط)", modifier = Modifier.weight(1f)) {
                LtrTextField(value = deficitStr, onValueChange = { deficitStr = it })
            }
            FormInputBox(label = "کسر مظنه (تومان/گرم)", modifier = Modifier.weight(1f)) {
                LtrTextField(value = deductionPerGramStr, onValueChange = { deductionPerGramStr = it })
            }
            FormInputBox(label = "کارمزد تعویض (٪)", modifier = Modifier.weight(1f)) {
                LtrTextField(value = commissionStr, onValueChange = { commissionStr = it })
            }
        }

        // Weights
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FormInputBox(label = "وزن ناخالص", modifier = Modifier.weight(1f)) {
                LtrTextField(value = grossWeightStr, onValueChange = { grossWeightStr = it })
            }
            FormInputBox(label = "کسر نگین/موم", modifier = Modifier.weight(1f)) {
                LtrTextField(value = stoneWeightStr, onValueChange = { stoneWeightStr = it })
            }
            FormInputBox(label = "خالص طلا", modifier = Modifier.weight(1f), isReadonly = true) {
                Text(
                    text = PersianNumberFormatter.formatWeight(item.netWeight),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.goldPrimary
                )
            }
        }

        // Live Summary Card
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = colors.surfaceContainer,
            border = BorderStroke(0.8.dp, colors.goldBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "مبلغ خرید متفرقه:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = colors.textMain)
                    Text(
                        text = "${PersianNumberFormatter.formatPrice(item.totalPayable)} تومان",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        color = colors.goldPrimary
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "معادل وزنی ۱۸ عیار:", fontSize = 11.sp, color = colors.textSecondary)
                    Text(
                        text = "${PersianNumberFormatter.formatWeight(item.equivalent18kWeight)} گرم",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textMain
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Action Buttons
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
    var labKaratStr by remember { mutableStateOf(existingItem?.labKarat?.toString() ?: "735") }
    var angNumber by remember { mutableStateOf(existingItem?.angNumber ?: "1248") }
    var labName by remember { mutableStateOf(existingItem?.labName ?: "ری‌گیری مشهد") }

    val weight = weightStr.toDoubleOrNull() ?: 0.0
    val labKarat = labKaratStr.toIntOrNull() ?: 750

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

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FormInputBox(label = "شماره انگ خطی", modifier = Modifier.weight(1f)) {
                LtrTextField(value = angNumber, onValueChange = { angNumber = it })
            }
            FormInputBox(label = "آزمایشگاه ری‌گیری", modifier = Modifier.weight(1f)) {
                LtrTextField(
                    value = labName,
                    onValueChange = { labName = it },
                    alignCenter = false
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FormInputBox(label = "وزن ترازویی (گرم)", modifier = Modifier.weight(1f)) {
                LtrTextField(value = weightStr, onValueChange = { weightStr = it })
            }
            FormInputBox(label = "عیار آزمایشگاه (ری‌گیری)", modifier = Modifier.weight(1f)) {
                LtrTextField(value = labKaratStr, onValueChange = { labKaratStr = it })
            }
        }

        // Summary Card
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = colors.surfaceContainer,
            border = BorderStroke(0.8.dp, colors.goldBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "ارزش کل آبشده:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = colors.textMain)
                    Text(
                        text = "${PersianNumberFormatter.formatPrice(item.totalPayable)} تومان",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        color = colors.goldPrimary
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "وزن معادل ۱۸ عیار:", fontSize = 11.sp, color = colors.textSecondary)
                    Text(
                        text = "${PersianNumberFormatter.formatWeight(item.equivalent18kWeight)} گرم",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textMain
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Action Buttons
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

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Coin Type Chips
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(text = "نوع سکه بانکی", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = colors.textSecondary)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                CoinType.entries.forEach { c ->
                    val isSelected = selectedCoin == c
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) colors.goldContainer.copy(alpha = 0.5f) else colors.surfaceContainer,
                        border = BorderStroke(if (isSelected) 1.2.dp else 0.6.dp, if (isSelected) colors.goldPrimary else colors.borderSubtle),
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
                                color = if (isSelected) colors.goldPrimary else colors.textMain
                            )
                            Text(
                                text = "${PersianNumberFormatter.formatWeight(c.totalWeightGrams)}g",
                                fontSize = 10.sp,
                                color = colors.textSecondary
                            )
                        }
                    }
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FormInputBox(label = "تعداد (عدد)", modifier = Modifier.weight(1f)) {
                LtrTextField(value = countStr, onValueChange = { countStr = it })
            }
            FormInputBox(label = "قیمت واحد (تومان)", modifier = Modifier.weight(1.5f)) {
                LtrTextField(value = unitPriceStr, onValueChange = { unitPriceStr = it })
            }
        }

        // Hologram Toggle
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = "بسته‌بندی و هولوگرام معتبر", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = colors.textMain)
                Text(text = "دارای تاییدیه رسمی اتحادیه", fontSize = 10.sp, color = colors.textSecondary)
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

        // Summary Card
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = colors.surfaceContainer,
            border = BorderStroke(0.8.dp, colors.goldBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "مبلغ کل سکه‌ها:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = colors.textMain)
                    Text(
                        text = "${PersianNumberFormatter.formatPrice(item.totalPayable)} تومان",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        color = colors.goldPrimary
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "معادل وزنی ۱۸ عیار:", fontSize = 11.sp, color = colors.textSecondary)
                    Text(
                        text = "${PersianNumberFormatter.formatWeight(item.equivalent18kWeight)} گرم",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textMain
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Action Buttons
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

// ---------------------------------------------------------------------------
// Helper UI Components
// ---------------------------------------------------------------------------
@Composable
private fun FormInputBox(
    label: String,
    modifier: Modifier = Modifier,
    isReadonly: Boolean = false,
    content: @Composable () -> Unit
) {
    val colors = LocalGoldExColors.current
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            color = colors.textSecondary,
            fontFamily = VazirmatnFamily,
            maxLines = 1
        )
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = if (isReadonly) colors.surfaceContainer else colors.surfaceElevated,
            border = BorderStroke(1.dp, colors.borderSubtle),
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                content()
            }
        }
    }
}

@Composable
private fun LtrTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    alignCenter: Boolean = true
) {
    val colors = LocalGoldExColors.current
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = TextStyle(
                fontFamily = VazirmatnFamily,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textMain,
                textAlign = if (alignCenter) TextAlign.Center else TextAlign.Start,
                textDirection = TextDirection.Ltr
            ),
            cursorBrush = SolidColor(colors.goldPrimary),
            keyboardOptions = KeyboardOptions.Default,
            modifier = modifier
        )
    }
}
