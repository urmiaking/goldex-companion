package com.goldex.companion.ui.calculator.tabs

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.goldex.companion.model.DetailedJewelryResult
import com.goldex.companion.model.Karat
import com.goldex.companion.model.PersianNumberFormatter
import com.goldex.companion.model.PersianWordsFormatter
import com.goldex.companion.model.WageType
import com.goldex.companion.ui.calculator.CalculatorUiState
import com.goldex.companion.ui.calculator.GoldCalculatorViewModel
import com.goldex.companion.ui.components.AddCustomerDialog
import com.goldex.companion.ui.components.AnimatedPriceTicker
import com.goldex.companion.ui.components.CustomerIconVector
import com.goldex.companion.ui.components.CustomerPickerDialog
import com.goldex.companion.ui.components.GoldInputField
import com.goldex.companion.ui.theme.LocalGoldExColors
import com.goldex.companion.ui.theme.heroCardGradient
import com.goldex.companion.ui.util.PdfInvoiceGenerator

@Composable
fun JewelryTab(
    viewModel: GoldCalculatorViewModel,
    uiState: CalculatorUiState
) {
    val context = LocalContext.current
    val colors = LocalGoldExColors.current

    LaunchedEffect(Unit) {
        viewModel.loadCustomers()
    }

    // Customer Dialogs
    if (uiState.isCustomerPickerVisible) {
        CustomerPickerDialog(
            customers = uiState.customerList,
            selectedCustomer = uiState.selectedCustomer,
            onSelectCustomer = { viewModel.selectCustomer(it) },
            onAddNewCustomerClick = { viewModel.setAddCustomerDialogVisible(true) },
            onDeleteCustomer = { viewModel.deleteCustomer(it) },
            onUpdateCustomer = { viewModel.updateCustomer(it) },
            onDismiss = { viewModel.setCustomerPickerVisible(false) }
        )
    }

    if (uiState.isAddCustomerDialogVisible) {
        AddCustomerDialog(
            onDismiss = { viewModel.setAddCustomerDialogVisible(false) },
            onSaveCustomer = { viewModel.addCustomer(it, autoSelect = true) }
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

        // 1. Live Benchmark Rate & Editable Gold Price Card
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = colors.surface,
            border = androidx.compose.foundation.BorderStroke(0.8.dp, colors.goldBorder),
            shadowElevation = if (colors.isDark) 0.dp else 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Gold gradient accent line on top
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.5.dp)
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color.Transparent, colors.goldSecondary, colors.goldPrimary, Color.Transparent)
                            )
                        )
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
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
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(colors.surfaceElevated),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = null,
                                    tint = colors.goldPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "مبنای قیمت طلا خام ۱۸ عیار",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.textMain
                                    )
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(if (uiState.autoSyncPrice) colors.profitGreen.copy(alpha = 0.12f) else colors.goldContainer)
                                            .padding(horizontal = 5.dp, vertical = 1.dp)
                                    ) {
                                        Text(
                                            text = if (uiState.autoSyncPrice) "زنده اتحادیه" else "دستی (شخصی)",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (uiState.autoSyncPrice) colors.profitGreen else colors.goldPrimary
                                        )
                                    }
                                }
                                Text(
                                    text = if (uiState.autoSyncPrice) "همگام با مظنه لحظه‌ای بازار" else "توسط کاربر قابل ویرایش",
                                    fontSize = 10.sp,
                                    color = colors.textMuted
                                )
                            }
                        }

                        // Auto Sync Switcher Pill
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = colors.surfaceElevated,
                            border = androidx.compose.foundation.BorderStroke(0.5.dp, colors.border),
                            modifier = Modifier.clickable {
                                viewModel.toggleAutoSyncPrice(!uiState.autoSyncPrice)
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = if (uiState.autoSyncPrice) Icons.Default.Lock else Icons.Default.Refresh,
                                    contentDescription = null,
                                    tint = if (uiState.autoSyncPrice) colors.goldPrimary else colors.textMuted,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = if (uiState.autoSyncPrice) "نرخ تثبیت" else "آزاد / ویرایش",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (uiState.autoSyncPrice) colors.goldPrimary else colors.textMuted
                                )
                            }
                        }
                    }

                    // Direct Editable Input for Gold Spot Price per Gram
                    GoldInputField(
                        value = uiState.spotPriceInput,
                        onValueChange = { viewModel.onSpotPriceChanged(it) },
                        label = "نرخ هر گرم طلا خام ۱۸ عیار",
                        trailingText = "تومان",
                        subLabel = if (uiState.priceInWords.isNotBlank()) uiState.priceInWords else null,
                        isDecimal = false,
                        useThousandsSeparator = true
                    )

                    // Quick Live Sync / Preset Button
                    if (uiState.rates.gold18 > 0L) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = colors.surfaceElevated,
                                border = androidx.compose.foundation.BorderStroke(0.5.dp, colors.border),
                                modifier = Modifier.clickable {
                                    viewModel.applyPresetSpotPrice(uiState.rates.gold18)
                                    viewModel.toggleAutoSyncPrice(true)
                                }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = null, tint = colors.goldPrimary, modifier = Modifier.size(12.dp))
                                    Text(
                                        text = "بازنشانی به نرخ اتحادیه: ${PersianNumberFormatter.formatPrice(uiState.rates.gold18.toDouble())} تومان",
                                        fontSize = 10.sp,
                                        color = colors.goldPrimary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 2. Weight, Karat & Item Description Card
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = colors.surface,
            border = androidx.compose.foundation.BorderStroke(0.8.dp, colors.border),
            shadowElevation = if (colors.isDark) 0.dp else 1.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Build,
                            contentDescription = null,
                            tint = colors.goldPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "مشخصات و عیار قطعه",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textMain
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = colors.surfaceElevated,
                            border = androidx.compose.foundation.BorderStroke(0.5.dp, colors.border),
                            modifier = Modifier.clickable {
                                viewModel.resetJewelry()
                                Toast.makeText(context, "فرم این قلم بازنشانی شد ✓", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Text(
                                text = "بازنشانی فرم",
                                fontSize = 9.5.sp,
                                color = colors.goldPrimary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(colors.surfaceElevated)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(text = "دقت ۰.۰۰۱ گرم", fontSize = 10.sp, color = colors.textMuted)
                        }
                    }
                }

                // Item Title Input for Multi-Item Invoice Identification
                GoldInputField(
                    value = uiState.itemTitleInput,
                    onValueChange = { viewModel.onItemTitleChanged(it) },
                    label = "شرح یا عنوان قطعه طلا (اختیاری)",
                    trailingText = "شرح",
                    keyboardType = KeyboardType.Text,
                    useThousandsSeparator = false
                )

                // Gross Weight Input
                GoldInputField(
                    value = uiState.grossWeightInput,
                    onValueChange = { viewModel.onGrossWeightChanged(it) },
                    label = "وزن ناخالص قطعه (با نگین/سنگ)",
                    trailingText = "گرم",
                    isDecimal = true,
                    useThousandsSeparator = false
                )

                // Quick-Add Pills (+1g, +2.5g, +5g, +10g, Reset)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf(
                        1.0 to "+۱ گرم",
                        2.5 to "+۲.۵ گرم",
                        5.0 to "+۵ گرم",
                        10.0 to "+۱۰ گرم"
                    ).forEach { (amt, label) ->
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = colors.surfaceElevated,
                            border = androidx.compose.foundation.BorderStroke(0.5.dp, colors.border),
                            modifier = Modifier.clickable { viewModel.addGrossWeight(amt) }
                        ) {
                            Text(
                                text = label,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                color = colors.textSecondary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                // Purity Grid
                Text(
                    text = "انتخاب عیار استاندارد قطعه",
                    fontSize = 11.sp,
                    color = colors.textSecondary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        Karat.K18 to "۷۵۰",
                        Karat.K21 to "۸۷۵",
                        Karat.K24 to "۹۹۹"
                    ).forEach { (karat, purityCode) ->
                        val isSelected = uiState.selectedKarat == karat
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) colors.textMain else colors.surfaceElevated)
                                .border(
                                    if (isSelected) 1.dp else 0.5.dp,
                                    if (isSelected) colors.goldPrimary else colors.border,
                                    RoundedCornerShape(10.dp)
                                )
                                .clickable { viewModel.onKaratSelected(karat) }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = karat.labelFa,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) colors.goldSecondary else colors.textMain
                                )
                                Text(
                                    text = PersianNumberFormatter.toPersianDigits(purityCode),
                                    fontSize = 9.sp,
                                    color = if (isSelected) colors.goldPrimary else colors.textMuted
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3. Deductions & Editable Commercial Parameters Card
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = colors.surface,
            border = androidx.compose.foundation.BorderStroke(0.8.dp, colors.border),
            shadowElevation = if (colors.isDark) 0.dp else 1.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "کسورات، اجرت، سود و مالیات",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textMain
                    )

                    uiState.jewelryResult?.let { res ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(colors.goldContainer)
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "وزن خالص: ${PersianNumberFormatter.formatWeight(res.netWeight)} گرم",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.goldPrimary
                            )
                        }
                    }
                }

                // Stone Deduction Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(colors.surfaceElevated)
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = "کسر وزن سنگ و نگین", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = colors.textMain)
                        Text(text = "تار و نگین اتمی یا جواهر", fontSize = 10.sp, color = colors.textMuted)
                    }

                    Box(modifier = Modifier.width(130.dp)) {
                        GoldInputField(
                            value = uiState.stoneWeightInput,
                            onValueChange = { viewModel.onStoneWeightChanged(it) },
                            label = "وزن نگین",
                            trailingText = "گرم",
                            isDecimal = true,
                            useThousandsSeparator = false
                        )
                    }
                }

                // Wage Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "اجرت ساخت کارگاهی", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = colors.textMain)
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(colors.surfaceElevated)
                            .padding(2.dp)
                    ) {
                        WageType.values().forEach { type ->
                            val isSel = uiState.wageType == type
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSel) colors.surface else Color.Transparent)
                                    .clickable { viewModel.onWageTypeChanged(type) }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = type.labelFa,
                                    fontSize = 10.sp,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
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

                // Editable Profit % and Legal Tax % (2 Columns)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Col 1: Fully Editable Profit Card
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = colors.surfaceElevated,
                        border = androidx.compose.foundation.BorderStroke(0.5.dp, colors.border),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = "سود فروشنده", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = colors.textMain)
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(colors.goldContainer)
                                        .padding(horizontal = 4.dp, vertical = 1.dp)
                                ) {
                                    Text(text = "قابل ویرایش", fontSize = 9.sp, color = colors.goldPrimary)
                                }
                            }

                            GoldInputField(
                                value = uiState.profitPercentInput,
                                onValueChange = { viewModel.onProfitPercentChanged(it) },
                                label = "درصد سود",
                                trailingText = "٪",
                                isDecimal = true,
                                useThousandsSeparator = false
                            )

                            // Quick Profit Presets
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                listOf(5.0 to "۵٪", 7.0 to "۷٪ مصوب", 9.0 to "۹٪").forEach { (preset, label) ->
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = colors.surface,
                                        border = androidx.compose.foundation.BorderStroke(0.5.dp, colors.border),
                                        modifier = Modifier.clickable { viewModel.applyPresetProfit(preset) }
                                    ) {
                                        Text(
                                            text = label,
                                            fontSize = 9.sp,
                                            color = colors.textSecondary,
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Col 2: Fully Editable Tax % Card
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = colors.surfaceElevated,
                        border = androidx.compose.foundation.BorderStroke(0.5.dp, colors.border),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = "مالیات ارزش‌افزوده", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = colors.textMain)
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(colors.profitGreen.copy(alpha = 0.12f))
                                        .padding(horizontal = 4.dp, vertical = 1.dp)
                                ) {
                                    Text(text = "فقط سود و اجرت", fontSize = 9.sp, color = colors.profitGreen)
                                }
                            }

                            GoldInputField(
                                value = uiState.taxPercentInput,
                                onValueChange = { viewModel.onTaxPercentChanged(it) },
                                label = "درصد مالیات",
                                trailingText = "٪",
                                isDecimal = true,
                                useThousandsSeparator = false
                            )

                            // Quick Tax Presets
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                listOf(9.0 to "۹٪ مصوب", 0.0 to "۰٪ معاف").forEach { (preset, label) ->
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = colors.surface,
                                        border = androidx.compose.foundation.BorderStroke(0.5.dp, colors.border),
                                        modifier = Modifier.clickable { viewModel.applyPresetTax(preset) }
                                    ) {
                                        Text(
                                            text = label,
                                            fontSize = 9.sp,
                                            color = colors.textSecondary,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Current Item Calculation Result Preview & "Add to Invoice" CTA
                uiState.jewelryResult?.let { res ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = colors.surfaceElevated,
                        border = androidx.compose.foundation.BorderStroke(0.8.dp, colors.goldBorder),
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
                                    text = "مبلغ تمام‌شده این قلم (${uiState.itemTitleInput}):",
                                    fontSize = 11.sp,
                                    color = colors.textSecondary
                                )
                                AnimatedPriceTicker(
                                    text = "${PersianNumberFormatter.formatPrice(res.totalPayable)} تومان",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.goldPrimary
                                )
                            }

                            // Add Item to Invoice Button
                            Button(
                                onClick = {
                                    viewModel.addItemToInvoice()
                                    Toast.makeText(context, "قطعه با موفقیت به فاکتور افزوده شد ✓", Toast.LENGTH_SHORT).show()
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = colors.goldPrimary,
                                    contentColor = if (colors.isDark) Color(0xFF0A0B0E) else Color.White
                                ),
                                modifier = Modifier.fillMaxWidth().height(42.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "افزودن این آیتم به فاکتور (${PersianNumberFormatter.toPersianDigits(uiState.invoiceItems.size.toString())} قلم)",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        // 4. Multi-Item Official Invoice Card
        val hasMultiItems = uiState.invoiceItems.isNotEmpty()
        val showInvoice = hasMultiItems || uiState.jewelryResult != null

        AnimatedVisibility(
            visible = showInvoice,
            enter = fadeIn() + slideInVertically()
        ) {
            val invoice = viewModel.buildCurrentInvoice()

            Surface(
                shape = RoundedCornerShape(18.dp),
                color = colors.surface,
                border = androidx.compose.foundation.BorderStroke(0.8.dp, colors.goldBorder),
                shadowElevation = if (colors.isDark) 0.dp else 4.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Top Gold Guilloché Ribbon Accent
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .background(
                                Brush.horizontalGradient(
                                    listOf(colors.goldPrimary, colors.goldSecondary, colors.goldPrimary)
                                )
                            )
                    )

                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Invoice Header & Serial
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = if (hasMultiItems) "فاکتور رسمی زرگری (چندآیتمی)" else "پیش‌فاکتور رسمی زرگری",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.textMain
                                    )
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(colors.goldContainer)
                                            .padding(horizontal = 6.dp, vertical = 1.dp)
                                    ) {
                                        Text(
                                            text = "${PersianNumberFormatter.toPersianDigits(invoice.items.size.toString())} قلم",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = colors.goldPrimary
                                        )
                                    }
                                }
                                Text(
                                    text = "کد فاکتور: #${PersianNumberFormatter.toPersianDigits(invoice.invoiceNumber)} • تاریخ: ${PersianNumberFormatter.toPersianDigits(uiState.rates.lastUpdated)}",
                                    fontSize = 10.sp,
                                    color = colors.textMuted
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(colors.surfaceElevated)
                                    .clickable { viewModel.setInvoiceManagerVisible(true) },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.List,
                                    contentDescription = "بایگانی فاکتورها",
                                    tint = colors.goldPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        // Customer Information Box (Requirement 4 & 5)
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = colors.surfaceElevated,
                            border = androidx.compose.foundation.BorderStroke(0.6.dp, colors.border),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(colors.goldContainer),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = CustomerIconVector,
                                            contentDescription = null,
                                            tint = colors.goldPrimary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    Column {
                                        Text(
                                            text = if (uiState.selectedCustomer != null) {
                                                "خریدار: ${uiState.selectedCustomer.name}"
                                            } else {
                                                "خریدار: مشتری عمومی (نقدی)"
                                            },
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = colors.textMain
                                        )
                                        if (uiState.selectedCustomer?.phone?.isNotBlank() == true) {
                                            Text(
                                                text = "تلفن: ${PersianNumberFormatter.toPersianDigits(uiState.selectedCustomer.phone)}",
                                                fontSize = 10.sp,
                                                color = colors.textSecondary
                                            )
                                        } else {
                                            Text(
                                                text = "امکان افزودن مشخصات مشتری به فاکتور",
                                                fontSize = 10.sp,
                                                color = colors.textMuted
                                            )
                                        }
                                    }
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    if (uiState.selectedCustomer != null) {
                                        IconButton(
                                            onClick = { viewModel.selectCustomer(null) },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "حذف مشتری",
                                                tint = colors.errorRed,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                    OutlinedButton(
                                        onClick = { viewModel.setCustomerPickerVisible(true) },
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                        modifier = Modifier.height(28.dp)
                                    ) {
                                        Text(
                                            text = if (uiState.selectedCustomer != null) "تغییر" else "انتخاب مشتری",
                                            fontSize = 10.sp,
                                            color = colors.goldPrimary
                                        )
                                    }
                                }
                            }
                        }

                        // Multi-Item List Breakdown
                        if (hasMultiItems) {
                            Text(
                                text = "ریز اقلام فاکتور (${PersianNumberFormatter.toPersianDigits(invoice.items.size.toString())} مورد):",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textMain
                            )

                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                invoice.items.forEachIndexed { index, item ->
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = colors.surfaceElevated,
                                        border = androidx.compose.foundation.BorderStroke(0.5.dp, colors.border),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 10.dp, vertical = 8.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    Text(
                                                        text = "${PersianNumberFormatter.toPersianDigits((index + 1).toString())}. ${item.title}",
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = colors.textMain
                                                    )
                                                    Box(
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(4.dp))
                                                            .background(colors.goldContainer)
                                                            .padding(horizontal = 4.dp, vertical = 1.dp)
                                                    ) {
                                                        Text(text = item.karat.labelFa.split(" ").firstOrNull() ?: "۱۸ عیار", fontSize = 9.sp, color = colors.goldPrimary)
                                                    }
                                                }
                                                Text(
                                                    text = "وزن خالص: ${PersianNumberFormatter.formatWeight(item.netWeight)} گ | اجرت: ${if (item.wageType == WageType.PERCENTAGE) "${PersianNumberFormatter.formatPercent(item.wageInput)}٪" else "${PersianNumberFormatter.formatPrice(item.wageInput)} ت"}",
                                                    fontSize = 10.sp,
                                                    color = colors.textSecondary
                                                )
                                            }

                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Text(
                                                    text = "${PersianNumberFormatter.formatPrice(item.totalPayable)} ت",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = colors.goldPrimary
                                                )
                                                IconButton(
                                                    onClick = { viewModel.removeItemFromInvoice(item.id) },
                                                    modifier = Modifier.size(24.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Delete,
                                                        contentDescription = "حذف قلم",
                                                        tint = colors.errorRed.copy(alpha = 0.7f),
                                                        modifier = Modifier.size(15.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Itemization / Summary Container
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = colors.surfaceElevated,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                ItemRow(
                                    label = "مجموع وزن خالص طلا (${PersianNumberFormatter.formatWeight(invoice.totalNetWeight)} گرم):",
                                    value = "${PersianNumberFormatter.formatPrice(invoice.totalRawGoldValue)} تومان",
                                    isBold = true
                                )
                                ItemRow(
                                    label = "کل اجرت ساخت کارگاهی:",
                                    value = "${PersianNumberFormatter.formatPrice(invoice.totalWageAmount)} تومان"
                                )
                                ItemRow(
                                    label = "مجموع سود مصوب فروشنده:",
                                    value = "${PersianNumberFormatter.formatPrice(invoice.totalProfitAmount)} تومان"
                                )
                                ItemRow(
                                    label = "مالیات ارزش‌افزوده (قانونی):",
                                    value = "${PersianNumberFormatter.formatPrice(invoice.totalTaxAmount)} تومان"
                                )
                            }
                        }

                        // Perforated Tear-Line
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Canvas(modifier = Modifier.fillMaxWidth().height(1.dp)) {
                                drawLine(
                                    color = colors.border,
                                    start = Offset(0f, 0f),
                                    end = Offset(size.width, 0f),
                                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f),
                                    strokeWidth = 1.5f
                                )
                            }
                        }

                        // Prominent Payable Grand Total Box
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(colors.heroCardGradient)
                                .border(0.8.dp, colors.goldBorder, RoundedCornerShape(14.dp))
                                .padding(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = "مبلغ کل قابل پرداخت نهایی فاکتور:", fontSize = 12.sp, color = colors.textSecondary)
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(colors.goldPrimary)
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(text = "تسویه رسمی", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            AnimatedPriceTicker(
                                text = "${PersianNumberFormatter.formatPrice(invoice.totalPayable)} تومان",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.goldPrimary,
                                contentAlignment = Alignment.Center
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = PersianWordsFormatter.toWords(invoice.totalPayable.toLong()),
                                fontSize = 11.sp,
                                color = colors.textMain,
                                textAlign = TextAlign.Center
                            )
                        }

                        // Big CTA: Share Official Invoice
                        Button(
                            onClick = {
                                viewModel.saveInvoice(invoice)
                                val textInvoice = invoice.formatTextInvoice(uiState.rates.source.labelFa)
                                val sendIntent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_TEXT, textInvoice)
                                    type = "text/plain"
                                }
                                context.startActivity(Intent.createChooser(sendIntent, "ارسال فاکتور رسمی طلا"))
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colors.goldPrimary,
                                contentColor = if (colors.isDark) Color(0xFF0A0B0E) else Color.White
                            ),
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "صدور و اشتراک فاکتور رسمی زرگری", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }

                        // Secondary Actions: Copy, PDF Export & Clear
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    viewModel.saveInvoice(invoice)
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val textInvoice = invoice.formatTextInvoice(uiState.rates.source.labelFa)
                                    clipboard.setPrimaryClip(ClipData.newPlainText("Qirat Invoice", textInvoice))
                                    Toast.makeText(context, "متن فاکتور کپی و در بایگانی ذخیره شد ✓", Toast.LENGTH_SHORT).show()
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.goldPrimary),
                                border = androidx.compose.foundation.BorderStroke(0.8.dp, colors.goldBorder),
                                modifier = Modifier.weight(1f).height(42.dp)
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(text = "کپی سریع فاکتور", fontSize = 11.sp)
                            }

                            Button(
                                onClick = {
                                    viewModel.saveInvoice(invoice)
                                    PdfInvoiceGenerator.generateAndShareInvoice(
                                        context = context,
                                        invoice = invoice,
                                        sourceName = uiState.rates.source.labelFa,
                                        settings = uiState.appSettings
                                    )
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = colors.surfaceElevated,
                                    contentColor = colors.goldPrimary
                                ),
                                border = androidx.compose.foundation.BorderStroke(0.6.dp, colors.border),
                                modifier = Modifier.weight(1f).height(42.dp)
                            ) {
                                Text(text = "📄 فاکتور رسمی PDF", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        if (hasMultiItems) {
                            TextButton(
                                onClick = {
                                    viewModel.clearInvoice()
                                    Toast.makeText(context, "فاکتور خالی شد", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            ) {
                                Text(text = "پاک کردن اقلام فاکتور", fontSize = 11.sp, color = colors.errorRed)
                            }
                        }
                    }
                }
            }
        }

        // 5. Regulatory Footer Note
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = colors.surfaceElevated,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = colors.goldPrimary,
                    modifier = Modifier.size(18.dp).padding(top = 2.dp)
                )
                Text(
                    text = "محاسبه دقیق طبق آخرین بخشنامه رسمی اتحادیه طلا و جواهر تهران. اصل طلا بر اساس قانون جدید مالیات بر ارزش افزوده مصوب مجلس شورای اسلامی به طور کامل از ۹٪ مالیات معاف است.",
                    fontSize = 11.sp,
                    lineHeight = 18.sp,
                    color = colors.textMuted
                )
            }
        }
    }
}

@Composable
private fun ItemRow(label: String, value: String, isBold: Boolean = false) {
    val colors = LocalGoldExColors.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            color = colors.textSecondary
        )
        AnimatedPriceTicker(
            text = value,
            fontSize = 12.sp,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
            color = if (isBold) colors.textMain else colors.textSecondary,
            contentAlignment = Alignment.CenterEnd
        )
    }
}
