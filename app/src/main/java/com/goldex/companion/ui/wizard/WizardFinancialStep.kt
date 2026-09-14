package com.goldex.companion.ui.wizard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.goldex.companion.model.Karat
import com.goldex.companion.model.PersianNumberFormatter
import com.goldex.companion.ui.components.GoldButton
import com.goldex.companion.ui.components.LuxuryCard
import com.goldex.companion.ui.theme.LocalGoldExColors

@Composable
fun WizardFinancialStep(
    financialState: WizardFinancialState,
    onFinancialChange: (WizardFinancialState) -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalGoldExColors.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Step Header Stepper
        WizardStepHeader(currentStep = WizardStep.FINANCIAL_DEFAULTS)

        // Section Narrative
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(4.dp, 18.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(colors.goldPrimary)
                )
                Text(
                    text = "تنظیمات پیش‌فرض محاسبات زرگری",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textMain
                )
            }
            Text(
                text = "مقادیر پایه برای تسریع در محاسبه آنی قیمت تابلو، فاکتور خرید و صدور آنی بر اساس مصوبات اتحادیه طلا و جواهر.",
                fontSize = 12.sp,
                fontWeight = FontWeight.Normal,
                color = colors.textSecondary,
                lineHeight = 18.sp
            )
        }

        // Section 1: Retail Profit Margin
        LuxuryCard(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(colors.goldContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = WizardPercent,
                            contentDescription = null,
                            tint = colors.goldPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Text(
                        text = "سود مجاز فروشنده (ویترین)",
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textMain
                    )
                }

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = colors.goldContainer,
                    border = BorderStroke(0.6.dp, colors.goldBorder.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = "حداکثر اتحادیه: ۷٪",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.goldPrimary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            // Profit Stepper Control
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = colors.surfaceElevated,
                border = BorderStroke(0.6.dp, colors.border)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "درصد اعمالی روی مظنه و اجرت:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = colors.textSecondary
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Minus Button
                        Surface(
                            shape = CircleShape,
                            color = colors.surface,
                            border = BorderStroke(0.6.dp, colors.border),
                            modifier = Modifier.size(32.dp),
                            onClick = {
                                val cur = financialState.profitPercent.toDoubleOrNull() ?: 7.0
                                val next = (cur - 0.5).coerceAtLeast(1.0)
                                onFinancialChange(financialState.copy(profitPercent = if (next % 1.0 == 0.0) next.toInt().toString() else next.toString()))
                            }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = WizardRemove,
                                    contentDescription = null,
                                    tint = colors.textMain,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        Text(
                            text = "${PersianNumberFormatter.toPersianDigits(financialState.profitPercent)}٪",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.goldPrimary,
                            modifier = Modifier.width(42.dp),
                            textAlign = TextAlign.Center
                        )

                        // Plus Button
                        Surface(
                            shape = CircleShape,
                            color = colors.surface,
                            border = BorderStroke(0.6.dp, colors.border),
                            modifier = Modifier.size(32.dp),
                            onClick = {
                                val cur = financialState.profitPercent.toDoubleOrNull() ?: 7.0
                                val next = (cur + 0.5).coerceAtMost(10.0)
                                onFinancialChange(financialState.copy(profitPercent = if (next % 1.0 == 0.0) next.toInt().toString() else next.toString()))
                            }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = WizardAdd,
                                    contentDescription = null,
                                    tint = colors.textMain,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Quick Selection Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("5", "6", "7").forEach { pct ->
                    val isSelected = financialState.profitPercent == pct
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) colors.goldPrimary else colors.surfaceElevated,
                        border = BorderStroke(0.6.dp, if (isSelected) colors.goldSecondary else colors.border),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onFinancialChange(financialState.copy(profitPercent = pct)) }
                    ) {
                        Text(
                            text = "${PersianNumberFormatter.toPersianDigits(pct)} درصد",
                            fontSize = 11.5.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color(0xFF241A00) else colors.textSecondary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                }
            }
        }

        // Section 2: VAT on Fabrication / Profit
        LuxuryCard(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(colors.goldContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = WizardAccountBalance,
                            contentDescription = null,
                            tint = colors.goldPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "مالیات بر ارزش افزوده (VAT)",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textMain
                        )
                        Text(
                            text = "قانون دائمی پایانه‌های فروشگاهی",
                            fontSize = 10.5.sp,
                            color = colors.textMuted
                        )
                    }
                }

                Switch(
                    checked = financialState.isVatEnabled,
                    onCheckedChange = { onFinancialChange(financialState.copy(isVatEnabled = it)) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = colors.goldPrimary,
                        uncheckedThumbColor = colors.textMuted,
                        uncheckedTrackColor = colors.surfaceElevated
                    )
                )
            }

            Text(
                text = "مطابق قانون مالیات، اصل ارزش طلا معاف بوده و مالیات صرفاً به مجموع «اجرت ساخت + سود زرگر» تعلق می‌گیرد.",
                fontSize = 11.5.sp,
                color = colors.textSecondary,
                lineHeight = 17.sp
            )

            if (financialState.isVatEnabled) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = colors.surfaceElevated,
                    border = BorderStroke(0.6.dp, colors.border)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "نرخ مصوب ارزش افزوده اجرت:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = colors.textMain
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("9" to "۹٪ (قدیم)", "10" to "۱۰٪ (جدید)").forEach { (rate, label) ->
                                val isSelected = financialState.vatRate == rate
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) colors.goldPrimary else colors.surface,
                                    border = BorderStroke(0.6.dp, if (isSelected) colors.goldSecondary else colors.border),
                                    modifier = Modifier.clickable {
                                        onFinancialChange(financialState.copy(vatRate = rate))
                                    }
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) Color(0xFF241A00) else colors.textSecondary,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Section 3: Base Karat (Standard Calculations)
        LuxuryCard(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(colors.goldContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = WizardDiamond,
                            contentDescription = null,
                            tint = colors.goldPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Text(
                        text = "عیار مبنا و محاسبات مظنه",
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textMain
                    )
                }

                Text(
                    text = "واحد: گرم",
                    fontSize = 11.sp,
                    color = colors.textMuted
                )
            }

            // 3 Karat Cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val karats = listOf(
                    Triple(Karat.K18, "۱۸ عیار", "خلوص ۷۵۰"),
                    Triple(Karat.K21, "۲۱ عیار", "خلوص ۸۷۵"),
                    Triple(Karat.K24, "۲۴ عیار", "شمش ۹۹۹.۹")
                )

                karats.forEach { (karat, title, subtitle) ->
                    val isSelected = financialState.baseKarat == karat
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) Color(0xFF191D2A) else colors.surfaceElevated,
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) colors.goldPrimary else colors.border
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onFinancialChange(financialState.copy(baseKarat = karat)) }
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Text(
                                text = title,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) colors.goldPrimary else colors.textMain
                            )
                            Text(
                                text = subtitle,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Normal,
                                color = if (isSelected) Color(0xFFC5CBD6) else colors.textMuted
                            )
                            Box(
                                modifier = Modifier
                                    .padding(top = 2.dp)
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(if (isSelected) colors.goldPrimary else Color.Transparent)
                            )
                        }
                    }
                }
            }

            // Ratio Info Banner
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = colors.surfaceElevated,
                border = BorderStroke(0.6.dp, colors.border)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "تبدیل مظنه (مبنای مثقال ۷۰۵ به گرم ۷۵۰):",
                        fontSize = 11.5.sp,
                        color = colors.textSecondary
                    )
                    Text(
                        text = "ضریب ۴.۳۳۱۸",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.goldPrimary
                    )
                }
            }
        }

        // Section 4: Formula Preview Callout
        LuxuryCard(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(colors.goldContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = WizardCalculate,
                        contentDescription = null,
                        tint = colors.goldPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "فرمول پیش‌نمایش محاسبات فاکتور",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textMain
                        )
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = colors.profitGreen.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "استاندارد",
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.profitGreen,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }

                    Text(
                        text = "مبلغ = وزن × [مظنه + اجرت + سود (${PersianNumberFormatter.toPersianDigits(financialState.profitPercent)}٪)] + مالیات (${PersianNumberFormatter.toPersianDigits(if (financialState.isVatEnabled) financialState.vatRate else "0")}٪)",
                        fontSize = 11.sp,
                        color = colors.textSecondary,
                        lineHeight = 17.sp,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                Icon(
                    imageVector = WizardAutoAwesome,
                    contentDescription = null,
                    tint = colors.goldPrimary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Navigation Action Row (RTL: Secondary/Back on Right, Primary/Next on Left)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            GoldButton(
                text = "بازگشت",
                icon = Icons.AutoMirrored.Filled.ArrowForward,
                isSecondary = true,
                onClick = onBack,
                modifier = Modifier.weight(1f)
            )

            GoldButton(
                text = "تأیید و گام بعدی",
                icon = Icons.AutoMirrored.Filled.ArrowBack,
                onClick = onNext,
                modifier = Modifier.weight(2f)
            )
        }
    }
}
