package com.goldex.companion.ui.calculator.tabs

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.goldex.companion.model.Karat
import com.goldex.companion.model.PersianNumberFormatter
import com.goldex.companion.model.PersianWordsFormatter
import com.goldex.companion.model.WageType
import com.goldex.companion.ui.calculator.CalculatorUiState
import com.goldex.companion.ui.calculator.GoldCalculatorViewModel
import com.goldex.companion.ui.components.GoldInputField
import com.goldex.companion.ui.components.ResultRow
import com.goldex.companion.ui.theme.LocalGoldExColors

@Composable
fun JewelryTab(
    viewModel: GoldCalculatorViewModel,
    uiState: CalculatorUiState
) {
    val context = LocalContext.current
    val colors = LocalGoldExColors.current

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Karat & Auto Sync Card
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = colors.surface,
            border = androidx.compose.foundation.BorderStroke(0.7.dp, colors.border),
            shadowElevation = if (colors.isDark) 0.dp else 1.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "عیار قطعه طلا",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = colors.textSecondary
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "همگام با مظنه زنده",
                            fontSize = 11.sp,
                            color = if (uiState.autoSyncPrice) colors.profitGreen else colors.textMuted
                        )
                        Switch(
                            checked = uiState.autoSyncPrice,
                            onCheckedChange = { viewModel.toggleAutoSyncPrice(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = colors.goldPrimary,
                                checkedTrackColor = colors.surfaceElevated,
                                uncheckedThumbColor = colors.textMuted,
                                uncheckedTrackColor = colors.surfaceElevated
                            ),
                            modifier = Modifier.height(22.dp)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Karat.values().forEach { karat ->
                        val isSelected = uiState.selectedKarat == karat
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) colors.goldContainer else colors.surfaceElevated)
                                .border(
                                    if (isSelected) 1.dp else 0.5.dp,
                                    if (isSelected) colors.goldPrimary else colors.border,
                                    RoundedCornerShape(10.dp)
                                )
                                .clickable { viewModel.onKaratSelected(karat) }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = karat.labelFa,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) colors.goldPrimary else colors.textSecondary
                            )
                        }
                    }
                }
            }
        }

        // Inputs Card
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = colors.surface,
            border = androidx.compose.foundation.BorderStroke(0.7.dp, colors.border),
            shadowElevation = if (colors.isDark) 0.dp else 1.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Gross weight input + quick add pills
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "مشخصات وزن", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = colors.textSecondary)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(1.0, 5.0, 10.0).forEach { add ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = colors.surfaceElevated,
                                border = androidx.compose.foundation.BorderStroke(0.5.dp, colors.border),
                                modifier = Modifier.clickable { viewModel.addGrossWeight(add) }
                            ) {
                                Text(
                                    text = "+${PersianNumberFormatter.toPersianDigits(add.toInt().toString())} گرم",
                                    fontSize = 10.sp,
                                    color = colors.goldPrimary,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    GoldInputField(
                        value = uiState.grossWeightInput,
                        onValueChange = { viewModel.onGrossWeightChanged(it) },
                        label = "وزن کل طلا",
                        trailingText = "گرم",
                        isDecimal = true,
                        useThousandsSeparator = false,
                        modifier = Modifier.weight(1f)
                    )

                    GoldInputField(
                        value = uiState.stoneWeightInput,
                        onValueChange = { viewModel.onStoneWeightChanged(it) },
                        label = "کسر وزن نگین",
                        trailingText = "گرم",
                        isDecimal = true,
                        useThousandsSeparator = false,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Spot price input
                GoldInputField(
                    value = uiState.spotPriceInput,
                    onValueChange = { viewModel.onSpotPriceChanged(it) },
                    label = "قیمت خام هر گرم طلای ۱۸ عیار",
                    trailingText = "تومان",
                    subLabel = if (uiState.priceInWords.isNotBlank()) "${uiState.priceInWords} تومان" else null,
                    useThousandsSeparator = true
                )

                // Wage row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "اجرت ساخت", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = colors.textSecondary)
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(colors.surfaceElevated)
                            .border(0.5.dp, colors.border, RoundedCornerShape(8.dp))
                            .padding(2.dp)
                    ) {
                        WageType.values().forEach { type ->
                            val isSel = uiState.wageType == type
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSel) colors.goldContainer else Color.Transparent)
                                    .clickable { viewModel.onWageTypeChanged(type) }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = type.labelFa,
                                    fontSize = 10.sp,
                                    color = if (isSel) colors.goldPrimary else colors.textMuted
                                )
                            }
                        }
                    }
                }

                GoldInputField(
                    value = uiState.wageInput,
                    onValueChange = { viewModel.onWageChanged(it) },
                    label = if (uiState.wageType == WageType.PERCENTAGE) "درصد اجرت ساخت" else "مبلغ اجرت هر گرم",
                    trailingText = if (uiState.wageType == WageType.PERCENTAGE) "٪" else "تومان",
                    isDecimal = uiState.wageType == WageType.PERCENTAGE,
                    useThousandsSeparator = uiState.wageType != WageType.PERCENTAGE
                )

                // Profit & Tax inputs
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    GoldInputField(
                        value = uiState.profitPercentInput,
                        onValueChange = { viewModel.onProfitPercentChanged(it) },
                        label = "سود طلافروش",
                        trailingText = "٪",
                        isDecimal = true,
                        useThousandsSeparator = false,
                        modifier = Modifier.weight(1f)
                    )

                    GoldInputField(
                        value = uiState.taxPercentInput,
                        onValueChange = { viewModel.onTaxPercentChanged(it) },
                        label = "مالیات ارزش‌افزوده",
                        trailingText = "٪",
                        isDecimal = true,
                        useThousandsSeparator = false,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Detailed Result Receipt Card
        AnimatedVisibility(
            visible = uiState.jewelryResult != null,
            enter = fadeIn() + slideInVertically()
        ) {
            uiState.jewelryResult?.let { res ->
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = colors.surface,
                    border = androidx.compose.foundation.BorderStroke(0.8.dp, colors.goldBorder),
                    shadowElevation = if (colors.isDark) 0.dp else 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "فاکتور برآورد قیمت",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.goldPrimary
                        )

                        HorizontalDivider(color = colors.border, thickness = 0.7.dp)

                        ResultRow(
                            label = "وزن خالص طلا",
                            value = "${PersianNumberFormatter.formatWeight(res.netWeight)} گرم"
                        )
                        ResultRow(
                            label = "ارزش خام طلا",
                            value = "${PersianNumberFormatter.formatPrice(res.rawGoldValue)} تومان"
                        )
                        ResultRow(
                            label = "اجرت ساخت",
                            value = "${PersianNumberFormatter.formatPrice(res.wageAmount)} تومان"
                        )
                        ResultRow(
                            label = "سود فروشنده",
                            value = "${PersianNumberFormatter.formatPrice(res.profitAmount)} تومان"
                        )
                        ResultRow(
                            label = "مالیات (بر اجرت و سود)",
                            value = "${PersianNumberFormatter.formatPrice(res.taxAmount)} تومان"
                        )
                        ResultRow(
                            label = "قیمت تمام‌شده هر گرم",
                            value = "${PersianNumberFormatter.formatPrice(res.effectiveGramPrice)} تومان",
                            valueColor = colors.goldSecondary,
                            isHighlight = true
                        )

                        HorizontalDivider(color = colors.border, thickness = 0.7.dp)

                        // Big Luxury Total
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(colors.goldContainer)
                                .border(0.6.dp, colors.goldBorder, RoundedCornerShape(12.dp))
                                .padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(text = "مبلغ کل قابل پرداخت", fontSize = 12.sp, color = colors.textSecondary)
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = "${PersianNumberFormatter.formatPrice(res.totalPayable)} تومان",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.goldPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${PersianWordsFormatter.toWords(res.totalPayable.toLong())} تومان",
                                fontSize = 11.sp,
                                color = colors.textMain,
                                textAlign = TextAlign.Center
                            )
                        }

                        // Actions Row: Copy, Share, and PDF Invoice Export
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val invoice = generateInvoiceText(uiState, res)
                                    val clip = ClipData.newPlainText("GoldEx Invoice", invoice)
                                    clipboard.setPrimaryClip(clip)
                                    Toast.makeText(context, "فاکتور متنی کپی شد", Toast.LENGTH_SHORT).show()
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.goldPrimary),
                                border = androidx.compose.foundation.BorderStroke(0.7.dp, colors.goldBorder),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(text = "کپی متن", fontSize = 10.sp)
                            }

                            Button(
                                onClick = {
                                    com.goldex.companion.ui.util.PdfInvoiceGenerator.generateAndShareInvoice(context, uiState, res)
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = colors.goldSecondary,
                                    contentColor = Color.White
                                ),
                                modifier = Modifier.weight(1.2f)
                            ) {
                                Text(text = "📄 فاکتور PDF", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = {
                                    val invoice = generateInvoiceText(uiState, res)
                                    val sendIntent = Intent().apply {
                                        action = Intent.ACTION_SEND
                                        putExtra(Intent.EXTRA_TEXT, invoice)
                                        type = "text/plain"
                                    }
                                    val shareIntent = Intent.createChooser(sendIntent, "ارسال فاکتور طلا")
                                    context.startActivity(shareIntent)
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = colors.goldPrimary,
                                    contentColor = if (colors.isDark) Color(0xFF0A0B0E) else Color.White
                                ),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(text = "ارسال", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun generateInvoiceText(uiState: CalculatorUiState, res: com.goldex.companion.model.DetailedJewelryResult): String {
    return """
        📄 فاکتور محاسبه طلا (گلدکس پرو)
        ────────────────────────
        عیار: ${uiState.selectedKarat.labelFa}
        وزن ناخالص: ${PersianNumberFormatter.formatWeight(res.grossWeight)} گرم
        وزن نگین: ${PersianNumberFormatter.formatWeight(res.stoneWeight)} گرم
        وزن خالص: ${PersianNumberFormatter.formatWeight(res.netWeight)} گرم
        قیمت خام ۱۸: ${PersianNumberFormatter.formatPrice(uiState.spotPriceInput.toDoubleOrNull() ?: 0.0)} تومان
        ارزش خام طلا: ${PersianNumberFormatter.formatPrice(res.rawGoldValue)} تومان
        اجرت ساخت: ${PersianNumberFormatter.formatPrice(res.wageAmount)} تومان
        سود فروشنده: ${PersianNumberFormatter.formatPrice(res.profitAmount)} تومان
        مالیات (۹٪): ${PersianNumberFormatter.formatPrice(res.taxAmount)} تومان
        فی هر گرم: ${PersianNumberFormatter.formatPrice(res.effectiveGramPrice)} تومان
        ────────────────────────
        مبلغ کل: ${PersianNumberFormatter.formatPrice(res.totalPayable)} تومان
        (${PersianWordsFormatter.toWords(res.totalPayable.toLong())} تومان)
        تاریخ استعلام: ${PersianNumberFormatter.toPersianDigits(uiState.rates.lastUpdated)}
    """.trimIndent()
}
