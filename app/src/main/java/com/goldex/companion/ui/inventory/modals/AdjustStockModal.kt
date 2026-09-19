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
import androidx.compose.material.icons.filled.Add
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
import com.goldex.companion.data.MarketRates
import com.goldex.companion.model.InventoryItem
import com.goldex.companion.model.PersianNumberFormatter
import com.goldex.companion.model.StockAdjustment
import com.goldex.companion.model.StockAdjustmentType
import com.goldex.companion.ui.components.GoldButton
import com.goldex.companion.ui.components.GoldInputField
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
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

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
                // Modal Header
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
                            Text(text = "🔄", fontSize = 20.sp)
                        }
                        Column {
                            Text(
                                text = "کسر یا شارژ موجودی",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textMain,
                                fontFamily = VazirmatnFamily
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(colors.profitGreen)
                                )
                                Text(
                                    text = "تنظیم کاردکس انبار و سینی‌های ویترین",
                                    fontSize = 11.sp,
                                    color = colors.textSecondary,
                                    fontFamily = VazirmatnFamily
                                )
                            }
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

                // Operation Mode Tabs (شارژ موجودی / کسر موجودی)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(colors.surfaceElevated)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val isCharge = adjustmentType == StockAdjustmentType.CHARGE
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isCharge) colors.profitGreen.copy(alpha = 0.15f) else Color.Transparent,
                        border = if (isCharge) BorderStroke(1.2.dp, colors.profitGreen) else null,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { adjustmentType = StockAdjustmentType.CHARGE }
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "+ شارژ موجودی (ورود)",
                                fontSize = 11.5.sp,
                                fontWeight = if (isCharge) FontWeight.Bold else FontWeight.Medium,
                                color = if (isCharge) colors.profitGreen else colors.textSecondary,
                                fontFamily = VazirmatnFamily
                            )
                        }
                    }

                    val isDeduct = adjustmentType == StockAdjustmentType.DEDUCT
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isDeduct) colors.errorRed.copy(alpha = 0.15f) else Color.Transparent,
                        border = if (isDeduct) BorderStroke(1.2.dp, colors.errorRed) else null,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { adjustmentType = StockAdjustmentType.DEDUCT }
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "- کسر موجودی (خروج)",
                                fontSize = 11.5.sp,
                                fontWeight = if (isDeduct) FontWeight.Bold else FontWeight.Medium,
                                color = if (isDeduct) colors.errorRed else colors.textSecondary,
                                fontFamily = VazirmatnFamily
                            )
                        }
                    }
                }

                // Target Item Info Card
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = colors.surfaceElevated.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, colors.border),
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
                            border = BorderStroke(0.8.dp, colors.border.copy(alpha = 0.7f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "موجودی فعلی در کاردکس:",
                                    fontSize = 10.5.sp,
                                    color = colors.textSecondary,
                                    fontFamily = VazirmatnFamily
                                )
                                Text(
                                    text = "${PersianNumberFormatter.toPersianDigits(currentQty.toString())} عدد | ${PersianNumberFormatter.formatWeight(currentGross)} گرم",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.textMain,
                                    fontFamily = VazirmatnFamily
                                )
                            }
                        }
                    }
                }

                // Quantity Stepper & Gold Weight Input
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Piece Count Stepper
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = colors.surfaceElevated,
                        border = BorderStroke(1.dp, colors.border),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "تعداد قطعه",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textSecondary,
                                fontFamily = VazirmatnFamily
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                IconButton(
                                    onClick = { quantityChange++ },
                                    modifier = Modifier
                                        .size(30.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(colors.surface)
                                        .border(1.dp, colors.border, RoundedCornerShape(8.dp))
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "افزایش",
                                        tint = colors.textMain,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }

                                Text(
                                    text = PersianNumberFormatter.toPersianDigits(quantityChange.toString()),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (adjustmentType == StockAdjustmentType.CHARGE) colors.profitGreen else colors.errorRed,
                                    fontFamily = VazirmatnFamily
                                )

                                IconButton(
                                    onClick = { if (quantityChange > 1) quantityChange-- },
                                    modifier = Modifier
                                        .size(30.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(colors.surface)
                                        .border(1.dp, colors.border, RoundedCornerShape(8.dp))
                                ) {
                                    Text(
                                        text = "—",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.textMain
                                    )
                                }
                            }
                        }
                    }

                    // Weight Input (Gram)
                    Box(modifier = Modifier.weight(1f)) {
                        GoldInputField(
                            value = weightInput,
                            onValueChange = { weightInput = it },
                            label = "وزن کل طلا",
                            trailingText = "گرم",
                            isDecimal = true,
                            keyboardType = KeyboardType.Decimal
                        )
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
                            "مرجوعی فاکتور مشتری"
                        ).forEach { reason ->
                            val isSelected = selectedReason == reason
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = if (isSelected) colors.textMain else colors.surfaceElevated,
                                modifier = Modifier.clickable { selectedReason = reason }
                            ) {
                                Text(
                                    text = reason,
                                    fontSize = 10.5.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) Color(0xFFFBBF24) else colors.textSecondary,
                                    fontFamily = VazirmatnFamily,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }

                // Description / Tracking Input
                GoldInputField(
                    value = noteInput,
                    onValueChange = { noteInput = it },
                    label = "توضیحات، شماره حواله یا بارنامه",
                    trailingText = null,
                    keyboardType = KeyboardType.Text
                )

                // Luxury Balance Summary Card
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
                                text = "پیش‌نمایش تراز کاردکس پس از ثبت",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFBBF24),
                                fontFamily = VazirmatnFamily
                            )
                            Text(
                                text = "آنلاین و قطعی",
                                fontSize = 9.5.sp,
                                color = Color(0xFF94A3B8),
                                fontFamily = VazirmatnFamily
                            )
                        }

                        HorizontalDivider(color = Color.White.copy(alpha = 0.1f), thickness = 0.6.dp)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(text = "موجودی فعلی", fontSize = 9.5.sp, color = Color(0xFF94A3B8), fontFamily = VazirmatnFamily)
                                Text(
                                    text = "${PersianNumberFormatter.toPersianDigits(currentQty.toString())} عدد",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontFamily = VazirmatnFamily
                                )
                                Text(
                                    text = "${PersianNumberFormatter.formatWeight(currentGross)} گرم",
                                    fontSize = 10.sp,
                                    color = Color(0xFF94A3B8),
                                    fontFamily = VazirmatnFamily
                                )
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                val changeSign = if (adjustmentType == StockAdjustmentType.CHARGE) "+" else "-"
                                val changeColor = if (adjustmentType == StockAdjustmentType.CHARGE) colors.profitGreen else colors.errorRed
                                Text(
                                    text = if (adjustmentType == StockAdjustmentType.CHARGE) "افزایش اعمالی" else "کاهش اعمالی",
                                    fontSize = 9.5.sp,
                                    color = changeColor,
                                    fontFamily = VazirmatnFamily
                                )
                                Text(
                                    text = "$changeSign${PersianNumberFormatter.toPersianDigits(quantityChange.toString())} عدد",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = changeColor,
                                    fontFamily = VazirmatnFamily
                                )
                                Text(
                                    text = "$changeSign${PersianNumberFormatter.formatWeight(effectiveWeight)} گرم",
                                    fontSize = 10.sp,
                                    color = changeColor,
                                    fontFamily = VazirmatnFamily
                                )
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(text = "موجودی نهایی", fontSize = 9.5.sp, color = Color(0xFFFBBF24), fontFamily = VazirmatnFamily)
                                Text(
                                    text = "${PersianNumberFormatter.toPersianDigits(newQty.toString())} عدد",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFBBF24),
                                    fontFamily = VazirmatnFamily
                                )
                                Text(
                                    text = "${PersianNumberFormatter.formatWeight(newGross)} گرم",
                                    fontSize = 10.sp,
                                    color = Color(0xFFFBBF24),
                                    fontFamily = VazirmatnFamily
                                )
                            }
                        }

                        HorizontalDivider(color = Color.White.copy(alpha = 0.08f), thickness = 0.5.dp)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "ارزش طلای ${if (adjustmentType == StockAdjustmentType.CHARGE) "افزوده" else "کسر شده"}:",
                                fontSize = 10.sp,
                                color = Color(0xFF94A3B8),
                                fontFamily = VazirmatnFamily
                            )
                            Text(
                                text = "${PersianNumberFormatter.formatPrice(deltaMonetaryValue)} تومان",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontFamily = VazirmatnFamily
                            )
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
                        text = "تایید و ثبت در کاردکس انبار",
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
                        },
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
