package com.goldex.companion.desktop.wizard

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.goldex.companion.desktop.ui.DesktopField
import com.goldex.companion.desktop.ui.controls.DesktopActionButton
import com.goldex.companion.desktop.ui.controls.DesktopIconButton
import com.goldex.companion.domain.onboarding.OpeningInventoryInput
import com.goldex.companion.model.PersianNumberFormatter
import com.goldex.companion.ui.components.AnimatedPriceTicker
import com.goldex.companion.ui.theme.LocalGoldExColors

@Composable
fun DesktopWizardInventoryPane(
    inventoryState: OpeningInventoryInput,
    onInventoryChange: (OpeningInventoryInput) -> Unit,
    liveGold18Price: Long,
    modifier: Modifier = Modifier
) {
    val colors = LocalGoldExColors.current

    // Computed totals
    val vitrinWeight = PersianNumberFormatter.parseToCleanDouble(inventoryState.vitrinWeight) ?: 0.0
    val meltWeight = PersianNumberFormatter.parseToCleanDouble(inventoryState.meltWeight) ?: 0.0
    val coinsWeight = (inventoryState.coinTamam * 8.133) +
            (inventoryState.coinNim * 4.066) +
            (inventoryState.coinRob * 2.033) +
            (inventoryState.coinQadim * 8.133) +
            (inventoryState.coinGerami * 1.01)

    val totalGoldWeight = vitrinWeight + meltWeight + coinsWeight
    val spot = if (liveGold18Price > 0) liveGold18Price else 6_000_000L
    val totalGoldValue = (totalGoldWeight * spot).toLong()

    val cashTankhah = PersianNumberFormatter.parseToCleanLong(inventoryState.cashTankhah) ?: 0L
    val bankBalances = PersianNumberFormatter.parseToCleanLong(inventoryState.bankBalances) ?: 0L
    val totalCash = cashTankhah + bankBalances

    BoxWithConstraints(modifier = modifier.fillMaxSize().testTag("wizard-inventory-pane")) {
        val isWide = maxWidth >= 960.dp

        if (isWide) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                InventoryInputsCard(
                    inventoryState = inventoryState,
                    onInventoryChange = onInventoryChange,
                    modifier = Modifier.weight(1.3f).fillMaxHeight()
                )

                InventoryValuationCard(
                    totalGoldWeight = totalGoldWeight,
                    vitrinWeight = vitrinWeight,
                    meltWeight = meltWeight,
                    coinsWeight = coinsWeight,
                    totalGoldValue = totalGoldValue,
                    totalCash = totalCash,
                    spot18 = spot,
                    modifier = Modifier.weight(1f).fillMaxHeight()
                )
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                InventoryValuationCard(
                    totalGoldWeight = totalGoldWeight,
                    vitrinWeight = vitrinWeight,
                    meltWeight = meltWeight,
                    coinsWeight = coinsWeight,
                    totalGoldValue = totalGoldValue,
                    totalCash = totalCash,
                    spot18 = spot,
                    modifier = Modifier.fillMaxWidth()
                )

                InventoryInputsCard(
                    inventoryState = inventoryState,
                    onInventoryChange = onInventoryChange,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun InventoryInputsCard(
    inventoryState: OpeningInventoryInput,
    onInventoryChange: (OpeningInventoryInput) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalGoldExColors.current

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = colors.surface,
        border = BorderStroke(0.8.dp, colors.border),
        shadowElevation = if (colors.isDark) 0.dp else 2.dp,
        modifier = modifier.testTag("inventory-inputs-card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(4.dp, 20.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(colors.goldPrimary)
                )
                Text(
                    text = "ثبت اقلام موجودی اول دوره",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textMain
                )
            }

            Text(
                text = "مقادیر واردشده به‌عنوان سند افتتاحیه انبار و گاوصندوق ثبت می‌شوند (پر کردن این فیلدها اختیاری است).",
                fontSize = 12.5.sp,
                color = colors.textSecondary
            )

            HorizontalDivider(color = colors.border.copy(alpha = 0.5f))

            // 1. Showcase Section
            InventorySectionHeader("۱. مصنوعات ساخته ویترین", Icons.Outlined.Storefront)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                DesktopField(
                    value = inventoryState.vitrinWeight,
                    onChange = { onInventoryChange(inventoryState.copy(vitrinWeight = it)) },
                    label = "وزن ناخالص ویترین",
                    unit = "گرم",
                    numeric = true,
                    adornment = Icons.Outlined.Scale,
                    modifier = Modifier.weight(1f).testTag("inventory-showcase-weight")
                )

                DesktopField(
                    value = inventoryState.vitrinOjrat,
                    onChange = { onInventoryChange(inventoryState.copy(vitrinOjrat = it)) },
                    label = "میانگین اجرت ساخت",
                    unit = "٪",
                    numeric = true,
                    adornment = Icons.Outlined.Percent,
                    modifier = Modifier.weight(1f).testTag("inventory-showcase-ojrat")
                )
            }

            HorizontalDivider(color = colors.border.copy(alpha = 0.3f))

            // 2. Melt Gold Section
            InventorySectionHeader("۲. طلای آبشده گاوصندوق", Icons.Outlined.Whatshot)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                DesktopField(
                    value = inventoryState.meltWeight,
                    onChange = { onInventoryChange(inventoryState.copy(meltWeight = it)) },
                    label = "وزن کل طلای آبشده",
                    unit = "گرم",
                    numeric = true,
                    adornment = Icons.Outlined.Scale,
                    modifier = Modifier.weight(1f).testTag("inventory-melt-weight")
                )

                DesktopField(
                    value = inventoryState.meltAyar,
                    onChange = { onInventoryChange(inventoryState.copy(meltAyar = it)) },
                    label = "خط عیار ری‌گیری",
                    unit = "عیار",
                    numeric = true,
                    adornment = Icons.Outlined.Verified,
                    modifier = Modifier.weight(1f).testTag("inventory-melt-ayar")
                )
            }

            HorizontalDivider(color = colors.border.copy(alpha = 0.3f))

            // 3. Official Coins Section
            InventorySectionHeader("۳. مسکوکات بانکی گاوصندوق", Icons.Outlined.MonetizationOn)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                CoinCounterRow(
                    name = "تمام بهار آزادی (طرح جدید - امامی)",
                    weightDesc = "۸٫۱۳۳ گرم • عیار ۹۰۰",
                    count = inventoryState.coinTamam,
                    onCountChange = { onInventoryChange(inventoryState.copy(coinTamam = it.coerceAtLeast(0))) }
                )
                CoinCounterRow(
                    name = "نیم سکه بهار آزادی",
                    weightDesc = "۴٫۰۶۶ گرم • عیار ۹۰۰",
                    count = inventoryState.coinNim,
                    onCountChange = { onInventoryChange(inventoryState.copy(coinNim = it.coerceAtLeast(0))) }
                )
                CoinCounterRow(
                    name = "ربع سکه بهار آزادی",
                    weightDesc = "۲٫۰۳۳ گرم • عیار ۹۰۰",
                    count = inventoryState.coinRob,
                    onCountChange = { onInventoryChange(inventoryState.copy(coinRob = it.coerceAtLeast(0))) }
                )
                CoinCounterRow(
                    name = "تمام بهار آزادی (طرح قدیم)",
                    weightDesc = "۸٫۱۳۳ گرم • عیار ۹۰۰",
                    count = inventoryState.coinQadim,
                    onCountChange = { onInventoryChange(inventoryState.copy(coinQadim = it.coerceAtLeast(0))) }
                )
                CoinCounterRow(
                    name = "سکه یک گرمی بانکی",
                    weightDesc = "۱٫۰۱ گرم • عیار ۹۰۰",
                    count = inventoryState.coinGerami,
                    onCountChange = { onInventoryChange(inventoryState.copy(coinGerami = it.coerceAtLeast(0))) }
                )
            }

            HorizontalDivider(color = colors.border.copy(alpha = 0.3f))

            // 4. Cash & Float Section
            InventorySectionHeader("۴. نقدینگی، تنخواه و موجودی بانک", Icons.Outlined.AccountBalanceWallet)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                DesktopField(
                    value = inventoryState.cashTankhah,
                    onChange = { onInventoryChange(inventoryState.copy(cashTankhah = it)) },
                    label = "تنخواه نقدی صندوق",
                    unit = "تومان",
                    numeric = true,
                    monetary = true,
                    adornment = Icons.Outlined.Payments,
                    modifier = Modifier.weight(1f).testTag("inventory-cash-balance")
                )

                DesktopField(
                    value = inventoryState.bankBalances,
                    onChange = { onInventoryChange(inventoryState.copy(bankBalances = it)) },
                    label = "مجموع موجودی بانک‌ها",
                    unit = "تومان",
                    numeric = true,
                    monetary = true,
                    adornment = Icons.Outlined.AccountBalance,
                    modifier = Modifier.weight(1f).testTag("inventory-bank-balances")
                )
            }
        }
    }
}

@Composable
private fun InventorySectionHeader(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    val colors = LocalGoldExColors.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = colors.goldPrimary,
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = title,
            fontSize = 13.5.sp,
            fontWeight = FontWeight.Bold,
            color = colors.textMain
        )
    }
}

@Composable
private fun CoinCounterRow(
    name: String,
    weightDesc: String,
    count: Int,
    onCountChange: (Int) -> Unit
) {
    val colors = LocalGoldExColors.current

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = colors.surfaceElevated,
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
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = name,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textMain
                )
                Text(
                    text = weightDesc,
                    fontSize = 11.sp,
                    color = colors.textMuted
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Minus button (in RTL: placed towards the text on the right)
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = colors.surface,
                    border = BorderStroke(0.8.dp, colors.border),
                    modifier = Modifier.size(30.dp),
                    onClick = { if (count > 0) onCountChange(count - 1) },
                    enabled = count > 0
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Outlined.Remove,
                            contentDescription = "کاهش تعداد",
                            tint = if (count > 0) colors.textMain else colors.textMuted.copy(alpha = 0.4f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Text(
                    text = PersianNumberFormatter.toPersianDigits(count),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textMain,
                    modifier = Modifier.width(32.dp),
                    textAlign = TextAlign.Center
                )

                // Plus button (in RTL: placed towards the outer left)
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = colors.goldContainer,
                    border = BorderStroke(0.8.dp, colors.goldBorder),
                    modifier = Modifier.size(30.dp),
                    onClick = { onCountChange(count + 1) }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Outlined.Add,
                            contentDescription = "افزایش تعداد",
                            tint = colors.goldPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InventoryValuationCard(
    totalGoldWeight: Double,
    vitrinWeight: Double,
    meltWeight: Double,
    coinsWeight: Double,
    totalGoldValue: Long,
    totalCash: Long,
    spot18: Long,
    modifier: Modifier = Modifier
) {
    val colors = LocalGoldExColors.current

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color.Transparent,
        border = BorderStroke(0.8.dp, colors.goldBorder),
        shadowElevation = if (colors.isDark) 0.dp else 4.dp,
        modifier = modifier.testTag("inventory-valuation-card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    androidx.compose.ui.graphics.Brush.verticalGradient(
                        listOf(
                            colors.surfaceElevated,
                            colors.surface
                        )
                    )
                )
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
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
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(colors.profitGreen)
                    )
                    Text(
                        text = "ارزش لحظه‌ای کل دارایی افتتاحیه",
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textMain
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = colors.goldPrimary.copy(alpha = 0.2f),
                    border = BorderStroke(0.5.dp, colors.goldPrimary.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = "نرخ روز: ${PersianNumberFormatter.formatPrice(spot18.toDouble())} ت",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.goldPrimary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            // Big Ticker
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AnimatedPriceTicker(
                    text = "${PersianNumberFormatter.formatPrice(totalGoldValue.toDouble())} تومان",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    color = colors.goldPrimary
                )

                Spacer(Modifier.height(4.dp))

                Text(
                    text = "مجموع سرمایه طلای فیزیکی بر مبنای نرخ ۱۸ عیار",
                    fontSize = 11.5.sp,
                    color = colors.textMuted
                )
            }

            // Breakdown box
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = colors.surfaceElevated,
                border = BorderStroke(0.6.dp, colors.border),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ValuationRow("جمع کل وزن طلا:", "${PersianNumberFormatter.formatAccountWeight(totalGoldWeight)} گرم", colors.goldSecondary)
                    HorizontalDivider(color = colors.border.copy(alpha = 0.6f))
                    ValuationRow("وزن مصنوعات ویترین:", "${PersianNumberFormatter.formatAccountWeight(vitrinWeight)} گرم", colors.textSecondary)
                    ValuationRow("وزن طلای آبشده:", "${PersianNumberFormatter.formatAccountWeight(meltWeight)} گرم", colors.textSecondary)
                    ValuationRow("وزن مسکوکات بانکی:", "${PersianNumberFormatter.formatAccountWeight(coinsWeight)} گرم", colors.textSecondary)
                    HorizontalDivider(color = colors.border.copy(alpha = 0.6f))
                    ValuationRow("نقدینگی و تنخواه:", "${PersianNumberFormatter.formatPrice(totalCash.toDouble())} تومان", colors.profitGreen)
                }
            }

            // Guild Safety Tip
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = colors.goldPrimary.copy(alpha = 0.08f),
                border = BorderStroke(0.5.dp, colors.goldBorder.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Shield,
                        contentDescription = null,
                        tint = colors.goldPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "اطلاعات ثبت‌شده مستقیماً در انبار امن ذخیره شده و مبنای سود و زیان دوره‌ای قرار می‌گیرند.",
                        fontSize = 11.5.sp,
                        color = colors.textSecondary,
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun ValuationRow(label: String, value: String, valueColor: Color) {
    val colors = LocalGoldExColors.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontSize = 12.sp, color = colors.textSecondary)
        Text(text = value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = valueColor)
    }
}
