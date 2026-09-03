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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
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
import com.goldex.companion.ui.components.AnimatedPriceTicker
import com.goldex.companion.ui.components.GoldInputField
import com.goldex.companion.ui.theme.LocalGoldExColors
import com.goldex.companion.ui.util.PdfInvoiceGenerator

@Composable
fun JewelryTab(
    viewModel: GoldCalculatorViewModel,
    uiState: CalculatorUiState
) {
    val context = LocalContext.current
    val colors = LocalGoldExColors.current

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // 1. Live Benchmark Rate Card (Stitch Component #1)
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

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
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
                                    fontSize = 11.sp,
                                    color = colors.textSecondary
                                )
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(colors.profitGreen.copy(alpha = 0.12f))
                                        .padding(horizontal = 5.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        text = "زنده اتحادیه",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.profitGreen
                                    )
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                AnimatedPriceTicker(
                                    text = PersianNumberFormatter.formatPrice(uiState.spotPriceInput.toDoubleOrNull() ?: 0.0),
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.textMain
                                )
                                Text(
                                    text = "تومان / گرم",
                                    fontSize = 11.sp,
                                    color = colors.textMuted
                                )
                            }
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
                                text = if (uiState.autoSyncPrice) "نرخ تثبیت" else "آزاد",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (uiState.autoSyncPrice) colors.goldPrimary else colors.textMuted
                            )
                        }
                    }
                }
            }
        }

        // 2. Weight & Purity Configuration Card (Stitch Component #2)
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
                            text = "مشخصات وزن و عیار قطعه",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textMain
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

                // Gross Weight Input
                GoldInputField(
                    value = uiState.grossWeightInput,
                    onValueChange = { viewModel.onGrossWeightChanged(it) },
                    label = "وزن ناخالص قطعه (با نگین/سنگ)",
                    trailingText = "گرم",
                    isDecimal = true,
                    useThousandsSeparator = false
                )

                // Quick-Add Pills (Stitch Exact: +1g, +2.5g, +5g, +10g, Reset)
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

                    Spacer(modifier = Modifier.weight(1f))

                    // Reset Pill
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = colors.surfaceElevated,
                        border = androidx.compose.foundation.BorderStroke(0.5.dp, colors.border),
                        modifier = Modifier.clickable { viewModel.onGrossWeightChanged("0") }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                tint = colors.errorRed,
                                modifier = Modifier.size(11.dp)
                            )
                            Text(text = "صفر", fontSize = 10.sp, color = colors.errorRed)
                        }
                    }
                }

                // Purity Grid (Stitch Standard Karat Selector)
                Text(
                    text = "انتخاب عیار استاندارد",
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

        // 3. Deductions & Commercial Parameters Card (Stitch Component #3)
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
                        text = "کسورات و پارامترهای تجاری",
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
                        Text(text = "تار و پیوند اتمی یا جواهر", fontSize = 10.sp, color = colors.textMuted)
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

                // Wage row
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

                // Retailer Profit & Legal VAT Cards (Stitch 2-Column Grid)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Col 1: Profit Card with Progress Bar
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = colors.surfaceElevated,
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = "سود قانونی", fontSize = 11.sp, color = colors.textMain)
                                Text(text = "۷٪ مصوب", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = colors.goldPrimary)
                            }
                            // Progress bar
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .clip(CircleShape)
                                    .background(colors.border)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(0.7f)
                                        .height(4.dp)
                                        .background(colors.goldPrimary)
                                )
                            }
                            Text(text = "سقف استاندارد صنف", fontSize = 9.sp, color = colors.textMuted)
                        }
                    }

                    // Col 2: Legal VAT Card with Verified Badge
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = colors.surfaceElevated,
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = "مالیات ارزش‌افزوده", fontSize = 11.sp, color = colors.textMain)
                                Text(text = "۹٪ خدمات", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = colors.profitGreen)
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = colors.profitGreen,
                                    modifier = Modifier.size(11.dp)
                                )
                                Text(text = "معافیت کامل اصل طلا", fontSize = 9.sp, color = colors.profitGreen, fontWeight = FontWeight.Medium)
                            }
                            Text(text = "فقط بر سود + اجرت", fontSize = 9.sp, color = colors.textMuted)
                        }
                    }
                }
            }
        }

        // 4. Official Luxury Receipt Card with Perforated Tear Line (Stitch Component #4)
        AnimatedVisibility(
            visible = uiState.jewelryResult != null,
            enter = fadeIn() + slideInVertically()
        ) {
            uiState.jewelryResult?.let { res ->
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
                            // Invoice Header
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
                                            text = "پیش‌فاکتور رسمی زرگری",
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
                                            Text(text = "معتبر", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = colors.goldPrimary)
                                        }
                                    }
                                    Text(
                                        text = "کد رهگیری: #GLD-${PersianNumberFormatter.toPersianDigits("88412")} • زمان: ${PersianNumberFormatter.toPersianDigits(uiState.rates.lastUpdated)}",
                                        fontSize = 10.sp,
                                        color = colors.textMuted
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(colors.surfaceElevated),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = null,
                                        tint = colors.goldPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            // Itemization Container
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
                                        label = "ارزش طلای خام خالص (${PersianNumberFormatter.formatWeight(res.netWeight)} گرم):",
                                        value = "${PersianNumberFormatter.formatPrice(res.rawGoldValue)} تومان",
                                        isBold = true
                                    )
                                    ItemRow(
                                        label = "کل اجرت ساخت کارگاهی (${uiState.wageInput}٪):",
                                        value = "${PersianNumberFormatter.formatPrice(res.wageAmount)} تومان"
                                    )
                                    ItemRow(
                                        label = "سود مصوب گالری (۷٪):",
                                        value = "${PersianNumberFormatter.formatPrice(res.profitAmount)} تومان"
                                    )
                                    ItemRow(
                                        label = "مالیات بر ارزش افزوده (۹٪ اجرت و سود):",
                                        value = "${PersianNumberFormatter.formatPrice(res.taxAmount)} تومان"
                                    )
                                }
                            }

                            // Perforated Tear-Line (Stitch Line with circular notches)
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

                            // Prominent Payable Total Box (Stitch Golden Box)
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(colors.goldContainer)
                                    .border(0.8.dp, colors.goldBorder, RoundedCornerShape(14.dp))
                                    .padding(14.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = "مبلغ کل قابل پرداخت نهایی:", fontSize = 12.sp, color = colors.textSecondary)
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(colors.goldPrimary)
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(text = "با تسویه آنی", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                AnimatedPriceTicker(
                                    text = "${PersianNumberFormatter.formatPrice(res.totalPayable)} تومان",
                                    fontSize = 24.sp,
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

                            // Big CTA: Share Official Invoice
                            Button(
                                onClick = {
                                    val invoice = generateInvoiceText(uiState, res)
                                    val sendIntent = Intent().apply {
                                        action = Intent.ACTION_SEND
                                        putExtra(Intent.EXTRA_TEXT, invoice)
                                        type = "text/plain"
                                    }
                                    context.startActivity(Intent.createChooser(sendIntent, "ارسال فاکتور طلا"))
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

                            // Secondary Actions: Copy & PDF
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        val invoice = generateInvoiceText(uiState, res)
                                        clipboard.setPrimaryClip(ClipData.newPlainText("GoldEx Invoice", invoice))
                                        Toast.makeText(context, "کپی شد ✓", Toast.LENGTH_SHORT).show()
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.goldPrimary),
                                    border = androidx.compose.foundation.BorderStroke(0.8.dp, colors.goldBorder),
                                    modifier = Modifier.weight(1f).height(42.dp)
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(text = "کپی سریع مبلغ", fontSize = 11.sp)
                                }

                                Button(
                                    onClick = {
                                        PdfInvoiceGenerator.generateAndShareInvoice(context, uiState, res)
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
                        }
                    }
                }
            }
        }

        // 5. Regulatory Footer Note (Stitch Component #5)
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
            color = if (isBold) colors.textMain else colors.textSecondary
        )
    }
}

private fun generateInvoiceText(uiState: CalculatorUiState, res: com.goldex.companion.model.DetailedJewelryResult): String {
    return """
        📄 فاکتور رسمی زرگری (گلدکس پرو)
        ────────────────────────
        عیار: ${uiState.selectedKarat.labelFa}
        وزن کل ناخالص: ${PersianNumberFormatter.formatWeight(res.grossWeight)} گرم
        کسر وزن نگین: ${PersianNumberFormatter.formatWeight(res.stoneWeight)} گرم
        وزن خالص طلا: ${PersianNumberFormatter.formatWeight(res.netWeight)} گرم
        قیمت خام ۱۸: ${PersianNumberFormatter.formatPrice(uiState.spotPriceInput.toDoubleOrNull() ?: 0.0)} تومان
        ارزش خام طلا: ${PersianNumberFormatter.formatPrice(res.rawGoldValue)} تومان
        اجرت ساخت (${uiState.wageInput}٪): ${PersianNumberFormatter.formatPrice(res.wageAmount)} تومان
        سود مصوب گالری (۷٪): ${PersianNumberFormatter.formatPrice(res.profitAmount)} تومان
        مالیات قانونی (۹٪): ${PersianNumberFormatter.formatPrice(res.taxAmount)} تومان
        ────────────────────────
        مبلغ نهایی قابل پرداخت: ${PersianNumberFormatter.formatPrice(res.totalPayable)} تومان
        (${PersianWordsFormatter.toWords(res.totalPayable.toLong())} تومان)
        کد رهگیری: #GLD-88412 • منبع: ${uiState.rates.source.labelFa}
    """.trimIndent()
}
