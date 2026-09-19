package com.goldex.companion.ui.inventory.modals

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextDirection
import com.goldex.companion.ui.theme.VazirmatnFeatureSettings
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.runtime.rememberCoroutineScope
import com.goldex.companion.ui.theme.LuxuryMotion
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import com.goldex.companion.data.MarketRates
import com.goldex.companion.model.InventoryItem
import com.goldex.companion.model.PersianNumberFormatter
import com.goldex.companion.model.StockAdjustment
import com.goldex.companion.model.StockAdjustmentType
import com.goldex.companion.ui.components.AnimatedPriceText
import com.goldex.companion.ui.components.GoldButton
import com.goldex.companion.ui.components.GoldInputField
import com.goldex.companion.ui.components.LuxurySegmentedControl
import com.goldex.companion.ui.hub.HubKaratSync
import com.goldex.companion.ui.invoices.components.InvoiceCloseVector
import com.goldex.companion.ui.theme.ButtonShape
import com.goldex.companion.ui.theme.LocalGoldExColors
import com.goldex.companion.ui.theme.VazirmatnFamily
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdjustStockModal(
    item: InventoryItem,
    rates: MarketRates,
    onDismiss: () -> Unit,
    onConfirmAdjustment: (StockAdjustment) -> Unit
) {
    val colors = LocalGoldExColors.current

    var adjustmentType by remember { mutableStateOf(StockAdjustmentType.CHARGE) }
    var quantityChange by remember { mutableIntStateOf(1) }
    var weightInput by remember { mutableStateOf("") }
    var selectedReason by remember { mutableStateOf("دریافت از کارگاه ساخت") }
    var noteInput by remember { mutableStateOf("") }

    val weightDouble by remember(weightInput) {
        derivedStateOf { PersianNumberFormatter.parseToCleanDouble(weightInput) ?: 0.0 }
    }

    // Calculations for preview
    val currentQty = item.quantity
    val currentGross = item.grossWeightGrams

    val newQty = when (adjustmentType) {
        StockAdjustmentType.CHARGE -> currentQty + quantityChange
        StockAdjustmentType.DEDUCT -> (currentQty - quantityChange).coerceAtLeast(0)
    }

    val effectiveWeight = if (weightDouble > 0.0) weightDouble else (item.netGoldWeightGrams * quantityChange)
    val newGross = when (adjustmentType) {
        StockAdjustmentType.CHARGE -> currentGross + effectiveWeight
        StockAdjustmentType.DEDUCT -> (currentGross - effectiveWeight).coerceAtLeast(0.0)
    }

    val spotPrice = remember(rates.gold18) { if (rates.gold18 > 0L) rates.gold18 else 23_360_000L }
    val deltaMonetaryValue = remember(effectiveWeight, spotPrice) {
        (effectiveWeight * spotPrice).toLong()
    }

    val coroutineScope = rememberCoroutineScope()
    var isVisible by remember { mutableStateOf(false) }

    val handleDismiss: () -> Unit = {
        if (isVisible) {
            coroutineScope.launch {
                isVisible = false
                delay(LuxuryMotion.DURATION_MODAL_EXIT.toLong())
                onDismiss()
            }
        }
    }

    LaunchedEffect(Unit) {
        isVisible = true
    }

    val scrimAlpha by animateFloatAsState(
        targetValue = if (isVisible) 0.65f else 0f,
        animationSpec = tween(
            durationMillis = if (isVisible) LuxuryMotion.DURATION_MODAL_ENTER else LuxuryMotion.DURATION_MODAL_EXIT,
            easing = FastOutSlowInEasing
        ),
        label = "scrimAlpha"
    )

    Dialog(
        onDismissRequest = handleDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = scrimAlpha))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = handleDismiss
                    ),
                contentAlignment = Alignment.BottomCenter
            ) {
                AnimatedVisibility(
                    visible = isVisible,
                    enter = LuxuryMotion.ModalEnter,
                    exit = LuxuryMotion.ModalExit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = {}
                            ),
                        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
                        color = colors.surface,
                        border = BorderStroke(
                            width = 1.dp,
                            brush = Brush.verticalGradient(
                                listOf(
                                    colors.goldPrimary.copy(alpha = 0.6f),
                                    colors.border.copy(alpha = 0.3f)
                                )
                            )
                        ),
                        shadowElevation = 24.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .wrapContentHeight()
                                .heightIn(max = 580.dp)
                                .navigationBarsPadding()
                        ) {
                            // Top Drag Handle
                            Box(
                                modifier = Modifier
                                    .padding(top = 10.dp, bottom = 4.dp)
                                    .size(width = 44.dp, height = 4.dp)
                                    .clip(CircleShape)
                                    .background(colors.border)
                                    .align(Alignment.CenterHorizontally)
                            )

                            // Fixed Modal Header
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
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
                                imageVector = HubKaratSync,
                                contentDescription = null,
                                tint = colors.goldPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "کسر یا شارژ موجودی",
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textMain,
                                fontFamily = VazirmatnFamily
                            )
                            Text(
                                text = "تنظیم کاردکس انبار و سینی‌های ویترین",
                                fontSize = 10.5.sp,
                                color = colors.textSecondary,
                                fontFamily = VazirmatnFamily
                            )
                        }
                    }

                    // Standard Close Button
                    IconButton(
                        onClick = handleDismiss,
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
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Operation Mode Switching (With LuxurySegmentedControl spring animation)
                    LuxurySegmentedControl(
                        items = listOf(StockAdjustmentType.CHARGE, StockAdjustmentType.DEDUCT),
                        selectedItem = adjustmentType,
                        onItemSelected = { adjustmentType = it },
                        label = {
                            if (it == StockAdjustmentType.CHARGE) "+ شارژ موجودی (ورود)" else "- کسر موجودی (خروج)"
                        },
                        modifier = Modifier.fillMaxWidth(),
                        height = 36.dp,
                        fontSize = 11.5.sp
                    )

                    // Target Item Info Card
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = colors.surfaceElevated.copy(alpha = 0.6f),
                        border = BorderStroke(1.dp, colors.border),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = item.title,
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.textMain,
                                    fontFamily = VazirmatnFamily
                                )
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = colors.goldContainer,
                                    border = BorderStroke(0.6.dp, colors.goldBorder)
                                ) {
                                    Text(
                                        text = item.code,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.goldSecondary,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Text(
                                text = item.location,
                                fontSize = 10.5.sp,
                                color = colors.textSecondary,
                                fontFamily = VazirmatnFamily
                            )

                            // Current Stock Status
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = colors.surface,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "موجودی فعلی: ${PersianNumberFormatter.toPersianDigits(currentQty.toString())} عدد",
                                        fontSize = 10.5.sp,
                                        color = colors.textSecondary,
                                        fontFamily = VazirmatnFamily
                                    )
                                    Text(
                                        text = "وزن ثبت‌شده: ${PersianNumberFormatter.formatWeight(currentGross)} گرم",
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.goldPrimary,
                                        fontFamily = VazirmatnFamily
                                    )
                                }
                            }
                        }
                    }

                    // Adjustment Inputs: Polished Stepper + Weight Input (Aligned & Equal Height)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        // Column 1: Polished Piece Count Stepper
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "تعداد تغییر (قطعه)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textSecondary,
                                fontFamily = VazirmatnFamily
                            )
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = colors.surfaceElevated,
                                border = BorderStroke(1.dp, colors.border),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    // [-] button
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = colors.surface,
                                        border = BorderStroke(0.6.dp, colors.border),
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clickable { if (quantityChange > 1) quantityChange-- }
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = "—",
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = colors.textMain
                                            )
                                        }
                                    }

                                    AnimatedContent(
                                        targetState = quantityChange,
                                        label = "qtyAnim"
                                    ) { qty ->
                                        Text(
                                            text = PersianNumberFormatter.toPersianDigits(qty.toString()),
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Black,
                                            color = if (adjustmentType == StockAdjustmentType.CHARGE) colors.profitGreen else colors.errorRed,
                                            fontFamily = VazirmatnFamily,
                                            modifier = Modifier.widthIn(min = 28.dp),
                                            textAlign = TextAlign.Center
                                        )
                                    }

                                    // [+] button
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = colors.surface,
                                        border = BorderStroke(0.6.dp, colors.border),
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clickable { quantityChange++ }
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.Add,
                                                contentDescription = "افزایش",
                                                tint = colors.goldPrimary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Column 2: Weight Input (Gram)
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "وزن کل طلا",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textSecondary,
                                fontFamily = VazirmatnFamily
                            )
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = colors.surfaceElevated,
                                border = BorderStroke(1.dp, colors.border),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Box(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                                            BasicTextField(
                                                value = PersianNumberFormatter.toPersianDigits(weightInput),
                                                onValueChange = {
                                                    val filtered = it.filter { ch -> ch.isDigit() || ch == '.' || ch == '/' || ch in '\u06F0'..'\u06F9' }
                                                    weightInput = PersianNumberFormatter.toEnglishDigits(filtered)
                                                },
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                                singleLine = true,
                                                textStyle = TextStyle(
                                                    fontFamily = VazirmatnFamily,
                                                    fontFeatureSettings = VazirmatnFeatureSettings,
                                                    fontSize = 15.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = colors.textMain,
                                                    textAlign = TextAlign.Right,
                                                    textDirection = TextDirection.Ltr
                                                ),
                                                cursorBrush = SolidColor(colors.goldPrimary),
                                                modifier = Modifier.fillMaxWidth(),
                                                decorationBox = { innerTextField ->
                                                    Box(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        contentAlignment = Alignment.CenterEnd
                                                    ) {
                                                        if (weightInput.isEmpty()) {
                                                            Text(
                                                                text = PersianNumberFormatter.formatWeight(item.netGoldWeightGrams * quantityChange),
                                                                fontSize = 13.5.sp,
                                                                color = colors.textMuted.copy(alpha = 0.55f),
                                                                fontFamily = VazirmatnFamily,
                                                                textAlign = TextAlign.Right
                                                            )
                                                        }
                                                        innerTextField()
                                                    }
                                                }
                                            )
                                        }
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = colors.surface,
                                        border = BorderStroke(0.5.dp, colors.border),
                                        modifier = Modifier.padding(vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = "گرم",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = colors.textSecondary,
                                            fontFamily = VazirmatnFamily,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Reason Selector Chips
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "علت و سرفصل ${if (adjustmentType == StockAdjustmentType.CHARGE) "شارژ" else "کسر"} موجودی",
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
                                "دریافت از کارگاه ساخت",
                                "انتقال از گاوصندوق پشتی",
                                "اصلاح تراز انبارگردانی",
                                "مرجوعی فاکتور مشتری",
                                "تحویل به ری‌گیری یا آبکاری"
                            ).forEach { reason ->
                                val isSelected = selectedReason == reason
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = if (isSelected) colors.goldPrimary else colors.surfaceElevated,
                                    border = BorderStroke(
                                        width = if (isSelected) 1.2.dp else 0.8.dp,
                                        color = if (isSelected) colors.goldPrimary else colors.border
                                    ),
                                    modifier = Modifier.clickable { selectedReason = reason }
                                ) {
                                    Text(
                                        text = reason,
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

                    // Description / Tracking Input
                    GoldInputField(
                        value = noteInput,
                        onValueChange = { noteInput = it },
                        label = "توضیحات اختیاری، شماره حواله یا بارنامه",
                        trailingText = null,
                        keyboardType = KeyboardType.Text
                    )

                    // Balance Summary Card
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
                            Text(
                                text = "پیش‌نمایش تراز کاردکس پس از این عملیات",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFBBF24),
                                fontFamily = VazirmatnFamily
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(text = "موجودی پس از ثبت:", fontSize = 9.5.sp, color = Color(0xFF94A3B8), fontFamily = VazirmatnFamily)
                                    Text(
                                        text = "${PersianNumberFormatter.toPersianDigits(newQty.toString())} عدد",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        fontFamily = VazirmatnFamily
                                    )
                                }
                                Column {
                                    Text(text = "وزن کل جدید:", fontSize = 9.5.sp, color = Color(0xFF94A3B8), fontFamily = VazirmatnFamily)
                                    Text(
                                        text = "${PersianNumberFormatter.formatWeight(newGross)} گرم",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFFBBF24),
                                        fontFamily = VazirmatnFamily
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(text = "گردش ریالی طلا:", fontSize = 9.5.sp, color = Color(0xFF94A3B8), fontFamily = VazirmatnFamily)
                                    AnimatedPriceText(
                                        amount = deltaMonetaryValue,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Black,
                                        color = if (adjustmentType == StockAdjustmentType.CHARGE) Color(0xFF34D399) else Color(0xFFF87171),
                                        unit = "تومان"
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
                        // Right child (Secondary / Cancel) - Rule: RTL Cancel on right
                        GoldButton(
                            text = "انصراف",
                            onClick = handleDismiss,
                            isSecondary = true,
                            modifier = Modifier.weight(1f)
                        )

                        // Left child (Primary / Confirm) - Rule: RTL Confirm on left
                        GoldButton(
                            text = if (adjustmentType == StockAdjustmentType.CHARGE) "ثبت و شارژ موجودی" else "ثبت و کسر موجودی",
                            onClick = {
                                val adj = StockAdjustment(
                                    id = UUID.randomUUID().toString(),
                                    itemId = item.id,
                                    itemTitle = item.title,
                                    type = adjustmentType,
                                    quantityChange = quantityChange,
                                    weightGrams = effectiveWeight,
                                    reason = selectedReason,
                                    note = noteInput.trim()
                                )
                                onConfirmAdjustment(adj)
                                handleDismiss()
                            },
                            isSecondary = false,
                            icon = Icons.Default.Check,
                            modifier = Modifier.weight(1.8f)
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
