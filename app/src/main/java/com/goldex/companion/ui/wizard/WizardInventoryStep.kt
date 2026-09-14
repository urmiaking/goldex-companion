package com.goldex.companion.ui.wizard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Balance
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.goldex.companion.model.PersianNumberFormatter
import com.goldex.companion.ui.components.GoldButton
import com.goldex.companion.ui.components.GoldInputField
import com.goldex.companion.ui.components.LuxuryCard
import com.goldex.companion.ui.theme.LocalGoldExColors
import com.goldex.companion.ui.theme.heroCardGradient

@Composable
fun WizardInventoryStep(
    inventoryState: WizardInventoryState,
    onInventoryChange: (WizardInventoryState) -> Unit,
    liveGold18Price: Long = 23_360_000L,
    onNext: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalGoldExColors.current

    // Parse values for summary calculations
    val vitrinWeightNum = PersianNumberFormatter.parseToCleanDouble(inventoryState.vitrinWeight) ?: 0.0
    val meltWeightNum = PersianNumberFormatter.parseToCleanDouble(inventoryState.meltWeight) ?: 0.0

    // Coin weights (Emami: 8.133g, Nim: 4.066g, Rob: 2.033g, Qadim: 8.133g, Gerami: 1.01g)
    val coinsWeight = (inventoryState.coinTamam * 8.133) +
            (inventoryState.coinNim * 4.066) +
            (inventoryState.coinRob * 2.033) +
            (inventoryState.coinQadim * 8.133) +
            (inventoryState.coinGerami * 1.01)

    val totalGoldWeight = vitrinWeightNum + meltWeightNum + coinsWeight
    val goldValueToman = (totalGoldWeight * (if (liveGold18Price > 0) liveGold18Price else 23_360_000L)).toLong()

    val cashTankhahNum = PersianNumberFormatter.parseToCleanLong(inventoryState.cashTankhah) ?: 0L
    val bankBalancesNum = PersianNumberFormatter.parseToCleanLong(inventoryState.bankBalances) ?: 0L
    val totalCashLiquidity = cashTankhahNum + bankBalancesNum

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Step Header Stepper
        WizardStepHeader(currentStep = WizardStep.INVENTORY)

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
                    text = "ثبت موجودی اولیه انبار و ویترین",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textMain
                )
            }
            Text(
                text = "جهت تراز دفاتر معین و محاسبه سود، موجودی طلای ساخته‌شده، آبشده، سکه و تنخواه نقدی شروع دوره را وارد نمایید (قابل ویرایش در آینده).",
                fontSize = 12.sp,
                fontWeight = FontWeight.Normal,
                color = colors.textSecondary,
                lineHeight = 18.sp
            )
        }

        // Card 1: طلای ساخته‌شده ویترین
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
                            imageVector = Icons.Default.Storefront,
                            contentDescription = null,
                            tint = colors.goldPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "طلای ساخته‌شده ویترین",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textMain
                        )
                        Text(
                            text = "موجودی مصنوعات طلا در گالری",
                            fontSize = 10.5.sp,
                            color = colors.textMuted
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = colors.goldContainer,
                    border = BorderStroke(0.6.dp, colors.goldBorder.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = "عیار ۷۵۰",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.goldPrimary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            GoldInputField(
                value = inventoryState.vitrinWeight,
                onValueChange = { onInventoryChange(inventoryState.copy(vitrinWeight = it)) },
                label = "وزن خالص طلای ساخته‌شده ۱۸ عیار",
                trailingText = "گرم",
                isDecimal = true,
                modifier = Modifier.fillMaxWidth()
            )

            GoldInputField(
                value = inventoryState.vitrinOjrat,
                onValueChange = { onInventoryChange(inventoryState.copy(vitrinOjrat = it)) },
                label = "میانگین اجرت خرید مصنوعات ویترین",
                trailingText = "درصد",
                isDecimal = true,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Card 2: طلای آبشده و شمش گاوصندوق
        LuxuryCard(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
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
                        imageVector = Icons.Default.ViewInAr,
                        contentDescription = null,
                        tint = colors.goldPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Column {
                    Text(
                        text = "طلای آبشده و شمش گاوصندوق",
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textMain
                    )
                    Text(
                        text = "ذخیره شمش، تکه طلا و عیار انگ‌دار",
                        fontSize = 10.5.sp,
                        color = colors.textMuted
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                GoldInputField(
                    value = inventoryState.meltWeight,
                    onValueChange = { onInventoryChange(inventoryState.copy(meltWeight = it)) },
                    label = "وزن آبشده",
                    trailingText = "گرم",
                    isDecimal = true,
                    modifier = Modifier.weight(1.3f)
                )

                GoldInputField(
                    value = inventoryState.meltAyar,
                    onValueChange = { onInventoryChange(inventoryState.copy(meltAyar = it)) },
                    label = "عیار میانگین",
                    trailingText = "خط",
                    isDecimal = false,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Card 3: مسکوکات طلا و سکه بانکی
        LuxuryCard(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
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
                        imageVector = Icons.Default.MonetizationOn,
                        contentDescription = null,
                        tint = colors.goldPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Column {
                    Text(
                        text = "مسکوکات طلا و سکه بانکی",
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textMain
                    )
                    Text(
                        text = "شمارش بسته‌های سکه پلمپ بانک مرکزی",
                        fontSize = 10.5.sp,
                        color = colors.textMuted
                    )
                }
            }

            // Coin rows
            val coins = listOf(
                CoinRowItem("تمام بهار آزادی (طرح جدید)", "وزن رسمی: ۸.۱۳۳ گرم", inventoryState.coinTamam) { delta ->
                    onInventoryChange(inventoryState.copy(coinTamam = (inventoryState.coinTamam + delta).coerceAtLeast(0)))
                },
                CoinRowItem("نیم سکه بهار آزادی", "وزن رسمی: ۴.۰۶۶ گرم", inventoryState.coinNim) { delta ->
                    onInventoryChange(inventoryState.copy(coinNim = (inventoryState.coinNim + delta).coerceAtLeast(0)))
                },
                CoinRowItem("ربع سکه بهار آزادی", "وزن رسمی: ۲.۰۳۳ گرم", inventoryState.coinRob) { delta ->
                    onInventoryChange(inventoryState.copy(coinRob = (inventoryState.coinRob + delta).coerceAtLeast(0)))
                },
                CoinRowItem("تمام بهار آزادی (طرح قدیم)", "وزن رسمی: ۸.۱۳۳ گرم", inventoryState.coinQadim) { delta ->
                    onInventoryChange(inventoryState.copy(coinQadim = (inventoryState.coinQadim + delta).coerceAtLeast(0)))
                },
                CoinRowItem("سکه یک گرمی (بانک مرکزی)", "وزن رسمی: ۱.۰۱ گرم", inventoryState.coinGerami) { delta ->
                    onInventoryChange(inventoryState.copy(coinGerami = (inventoryState.coinGerami + delta).coerceAtLeast(0)))
                }
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                coins.forEach { item ->
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
                            Column {
                                Text(
                                    text = item.name,
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.textMain
                                )
                                Text(
                                    text = item.weightDesc,
                                    fontSize = 10.5.sp,
                                    color = colors.textMuted
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = colors.surface,
                                    border = BorderStroke(0.6.dp, colors.border),
                                    modifier = Modifier.size(28.dp),
                                    onClick = { item.onAdjust(-1) }
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Remove,
                                            contentDescription = null,
                                            tint = colors.textMain,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }

                                Text(
                                    text = PersianNumberFormatter.toPersianDigits(item.count),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.textMain,
                                    modifier = Modifier.width(28.dp),
                                    textAlign = TextAlign.Center
                                )

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = colors.goldContainer,
                                    border = BorderStroke(0.6.dp, colors.goldBorder),
                                    modifier = Modifier.size(28.dp),
                                    onClick = { item.onAdjust(1) }
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = null,
                                            tint = colors.goldPrimary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Card 4: نقدینگی و بانک‌ها
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
                            imageVector = Icons.Default.AccountBalance,
                            contentDescription = null,
                            tint = colors.goldPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "موجودی نقدینگی و حساب‌ها",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textMain
                        )
                        Text(
                            text = "تنخواه و پوزهای بانکی متصل به حجره",
                            fontSize = 10.5.sp,
                            color = colors.textMuted
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = colors.surfaceElevated,
                    border = BorderStroke(0.6.dp, colors.border)
                ) {
                    Text(
                        text = "ریالی",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textSecondary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            GoldInputField(
                value = inventoryState.cashTankhah,
                onValueChange = { onInventoryChange(inventoryState.copy(cashTankhah = it)) },
                label = "تنخواه نقدی صندوق حجره",
                trailingText = "تومان",
                useThousandsSeparator = true,
                modifier = Modifier.fillMaxWidth()
            )

            GoldInputField(
                value = inventoryState.bankBalances,
                onValueChange = { onInventoryChange(inventoryState.copy(bankBalances = it)) },
                label = "موجودی کل پوز و حساب‌های بانکی متصل",
                trailingText = "تومان",
                useThousandsSeparator = true,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Card 5: Hero Obsidian Summary Card (تراز سرمایه در گردش اول دوره)
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(0.8.dp, colors.goldBorder.copy(alpha = 0.6f)),
            shadowElevation = 4.dp
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.heroCardGradient)
                    .padding(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Balance,
                            contentDescription = null,
                            tint = colors.goldSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "خلاصه تراز سرمایه در گردش اولیه",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(0.6.dp)
                            .background(Color(0x33FFFFFF))
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "مجموع سرمایه وزنی طلا:",
                            fontSize = 12.sp,
                            color = Color(0xFFC5CBD6)
                        )
                        Text(
                            text = "${PersianNumberFormatter.formatWeight(totalGoldWeight)} گرم",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.goldSecondary
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "تخمین ارزش روز طلا و سکه:",
                            fontSize = 12.sp,
                            color = Color(0xFFC5CBD6)
                        )
                        Text(
                            text = "${PersianNumberFormatter.format(goldValueToman)} تومان",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "مجموع نقدینگی ریالی:",
                            fontSize = 12.sp,
                            color = Color(0xFFC5CBD6)
                        )
                        Text(
                            text = "${PersianNumberFormatter.format(totalCashLiquidity)} تومان",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
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

private data class CoinRowItem(
    val name: String,
    val weightDesc: String,
    val count: Int,
    val onAdjust: (Int) -> Unit
)
