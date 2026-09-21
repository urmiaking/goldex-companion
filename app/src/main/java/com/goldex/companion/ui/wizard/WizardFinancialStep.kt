package com.goldex.companion.ui.wizard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.goldex.companion.model.Karat
import com.goldex.companion.model.PersianNumberFormatter
import com.goldex.companion.ui.components.GoldButton
import com.goldex.companion.ui.components.LuxuryCard
import com.goldex.companion.ui.components.LuxurySegmentedControl
import com.goldex.companion.ui.components.QiratoToast
import com.goldex.companion.ui.theme.LocalGoldExColors

/**
 * Pure Scrollable Content for Step 2: Financial Defaults, Margin, VAT, and Karat.
 */
@Composable
fun WizardFinancialContent(
    financialState: WizardFinancialState,
    onFinancialChange: (WizardFinancialState) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalGoldExColors.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
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

        // Section 1: Retail Profit Margin (سود مجاز فروشنده)
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
                                onFinancialChange(
                                    financialState.copy(
                                        profitPercent = if (next % 1.0 == 0.0) next.toInt().toString() else next.toString()
                                    )
                                )
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
                                onFinancialChange(
                                    financialState.copy(
                                        profitPercent = if (next % 1.0 == 0.0) next.toInt().toString() else next.toString()
                                    )
                                )
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

        }

        // Section 2: VAT on Fabrication / Profit (مالیات بر ارزش افزوده)
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
            }

            // Unified 3-State Segmented Control for the configurable VAT policy.
            val vatOptions = listOf(
                Triple("exempt", "معاف از مالیات", false to "0"),
                Triple("9", "۹٪", true to "9"),
                Triple("10", "۱۰٪ (قانون جدید)", true to "10")
            )
            val currentVatKey = if (!financialState.isVatEnabled || financialState.vatRate == "0") "exempt" else financialState.vatRate
            val selectedOption = vatOptions.find { it.first == currentVatKey } ?: vatOptions[0]

            LuxurySegmentedControl(
                items = vatOptions,
                selectedItem = selectedOption,
                onItemSelected = { opt ->
                    onFinancialChange(
                        financialState.copy(
                            isVatEnabled = opt.third.first,
                            vatRate = opt.third.second
                        )
                    )
                },
                label = { it.second },
                height = 40.dp,
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                text = "مطابق قانون دائمی پایانه‌های فروشگاهی، اصل طلا معاف بوده و در صورت شمولیت، مالیات صرفاً بر مجموع «اجرت ساخت + سود زرگر» محاسبه می‌شود.",
                fontSize = 11.5.sp,
                color = colors.textSecondary,
                lineHeight = 17.sp
            )
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

            // Luxury Segmented Control for Karats
            LuxurySegmentedControl(
                items = listOf(Karat.K18, Karat.K21, Karat.K24),
                selectedItem = financialState.baseKarat,
                onItemSelected = { onFinancialChange(financialState.copy(baseKarat = it)) },
                label = { karat ->
                    when (karat) {
                        Karat.K18 -> "۱۸ عیار (۷۵۰)"
                        Karat.K21 -> "۲۱ عیار (۸۷۵)"
                        Karat.K24 -> "۲۴ عیار (شمش)"
                    }
                },
                height = 42.dp,
                modifier = Modifier.fillMaxWidth()
            )
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

        Spacer(modifier = Modifier.height(12.dp))
    }
}

/**
 * Standalone Financial Defaults Step Screen.
 */
@Composable
fun WizardFinancialStep(
    financialState: WizardFinancialState,
    onFinancialChange: (WizardFinancialState) -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalGoldExColors.current
    val context = LocalContext.current

    Column(modifier = modifier.fillMaxSize()) {
        WizardStepHeader(
            currentStep = WizardStep.FINANCIAL_DEFAULTS,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            WizardFinancialContent(
                financialState = financialState,
                onFinancialChange = onFinancialChange
            )
        }

        // Sticky Footer (RTL: Back on Right, Next on Left)
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = colors.surface,
            border = BorderStroke(0.6.dp, colors.border),
            shadowElevation = 8.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Secondary / Back: Right side in Persian RTL (first in Row)
                GoldButton(
                    text = "بازگشت",
                    icon = WizardArrowRight,
                    isSecondary = true,
                    onClick = onBack,
                    modifier = Modifier.weight(1f)
                )

                // Primary / Next: Left side in Persian RTL (second in Row)
                GoldButton(
                    text = "تأیید و گام بعدی",
                    trailingIcon = WizardArrowLeft,
                    onClick = {
                        val errors = validateWizardFinancial(financialState)
                        if (errors.hasErrors) {
                            val msg = errors.firstErrorMessage ?: "لطفاً مقادیر مالی معتبر وارد کنید."
                            QiratoToast.show(context, msg)
                        } else {
                            onNext()
                        }
                    },
                    modifier = Modifier.weight(2f)
                )
            }
        }
    }
}
