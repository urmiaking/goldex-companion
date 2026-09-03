package com.goldex.companion.ui.calculator.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.goldex.companion.data.PortfolioCategory
import com.goldex.companion.data.PortfolioItem
import com.goldex.companion.model.CoinType
import com.goldex.companion.model.Karat
import com.goldex.companion.model.PersianNumberFormatter
import com.goldex.companion.model.PersianWordsFormatter
import com.goldex.companion.ui.calculator.CalculatorUiState
import com.goldex.companion.ui.calculator.GoldCalculatorViewModel
import com.goldex.companion.ui.components.AnimatedPriceTicker
import com.goldex.companion.ui.components.GoldButton
import com.goldex.companion.ui.components.GoldInputField
import com.goldex.companion.ui.components.LuxuryCard
import com.goldex.companion.ui.components.SectionHeader
import com.goldex.companion.ui.theme.LocalGoldExColors

private val PortfolioCoinVector: ImageVector = ImageVector.Builder(
    name = "PortfolioCoin",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f
).apply {
    path(fill = SolidColor(Color.White)) {
        moveTo(12f, 2f)
        curveTo(6.48f, 2f, 2f, 6.48f, 2f, 12f)
        curveTo(2f, 17.52f, 6.48f, 22f, 12f, 22f)
        curveTo(17.52f, 22f, 22f, 17.52f, 22f, 12f)
        curveTo(22f, 6.48f, 17.52f, 2f, 12f, 2f)
        close()
        moveTo(12f, 19.5f)
        curveTo(7.86f, 19.5f, 4.5f, 16.14f, 4.5f, 12f)
        curveTo(4.5f, 7.86f, 7.86f, 4.5f, 12f, 4.5f)
        curveTo(16.14f, 4.5f, 19.5f, 7.86f, 19.5f, 12f)
        curveTo(19.5f, 16.14f, 16.14f, 19.5f, 12f, 19.5f)
        close()
    }
}.build()

@Composable
fun PortfolioTab(
    uiState: CalculatorUiState
) {
    val vm: GoldCalculatorViewModel = viewModel()
    PortfolioTab(viewModel = vm, uiState = uiState)
}

@Composable
fun PortfolioTab(
    viewModel: GoldCalculatorViewModel,
    uiState: CalculatorUiState
) {
    val colors = LocalGoldExColors.current
    var showAddDialog by remember { mutableStateOf(false) }

    // Use unified portfolio items from ViewModel StateFlow
    val items = uiState.portfolioItems

    val totalCurrentVal = items.sumOf { it.calculateCurrentValue(uiState.rates) }
    val totalPurchaseVal = items.sumOf { it.purchasePriceTotal }
    val totalProfit = totalCurrentVal - totalPurchaseVal
    val totalProfitPercent = if (totalPurchaseVal > 0) (totalProfit.toDouble() / totalPurchaseVal.toDouble()) * 100.0 else 0.0

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // Hero Portfolio Summary Card (Stitch Sovereign Aurum with LuxuryCard & SectionHeader)
        LuxuryCard {
            SectionHeader(
                title = "ارزش کل سبد دارایی طلا و سکه",
                subtitle = "ارزش‌گذاری بر مبنای آخرین مظنه بازار",
                icon = Icons.Default.Star,
                trailingContent = {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(colors.goldContainer)
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "${PersianNumberFormatter.toPersianDigits(items.size.toString())} قلم دارایی",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.goldPrimary
                        )
                    }
                }
            )

            AnimatedPriceTicker(
                text = "${PersianNumberFormatter.formatPrice(totalCurrentVal.toDouble())} تومان",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = colors.goldPrimary
            )

            Text(
                text = PersianWordsFormatter.toWords(totalCurrentVal),
                fontSize = 11.sp,
                color = colors.textMain
            )

            HorizontalDivider(color = colors.border, thickness = 0.7.dp)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("مجموع سرمایه اولیه:", fontSize = 11.sp, color = colors.textMuted)
                    AnimatedPriceTicker(
                        text = "${PersianNumberFormatter.formatPrice(totalPurchaseVal.toDouble())} تومان",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textSecondary
                    )
                }

                val isProfit = totalProfit >= 0
                val profitColor = if (isProfit) colors.profitGreen else colors.errorRed
                val profitSign = if (isProfit) "+" else ""

                Column(horizontalAlignment = Alignment.End) {
                    Text("سود / زیان کل:", fontSize = 11.sp, color = colors.textMuted)
                    AnimatedPriceTicker(
                        text = "$profitSign${PersianNumberFormatter.formatPrice(totalProfit.toDouble())} ت ($profitSign${PersianNumberFormatter.toPersianDigits("%.1f".format(totalProfitPercent))}٪)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = profitColor,
                        contentAlignment = Alignment.CenterEnd
                    )
                }
            }
        }

        // Add Asset CTA Button
        GoldButton(
            text = "ثبت دارایی جدید در سبد",
            icon = Icons.Default.Add,
            onClick = { showAddDialog = true }
        )

        // Empty state when no items
        if (items.isEmpty()) {
            LuxuryCard(
                hasTopHairline = false,
                elevation = 1.dp,
                contentPadding = PaddingValues(24.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(colors.goldContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = colors.goldPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Text(
                        text = "سبد دارایی شما هنوز ثبت نشده است",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textMain
                    )
                    Text(
                        text = "با ثبت قطعات طلا و سکه‌های خود، سود و زیان لحظه‌ای آنها را دنبال کنید.",
                        fontSize = 11.sp,
                        color = colors.textMuted,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }

        // Assets List
        items.forEach { item ->
            val curVal = item.calculateCurrentValue(uiState.rates)
            val profit = item.calculateProfit(uiState.rates)
            val profitPct = item.calculateProfitPercent(uiState.rates)
            val isItemProfit = profit >= 0
            val itemProfitColor = if (isItemProfit) colors.profitGreen else colors.errorRed
            val itemProfitSign = if (isItemProfit) "+" else ""

            LuxuryCard(
                hasTopHairline = false,
                elevation = 1.dp,
                contentPadding = PaddingValues(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
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
                                .size(36.dp)
                                .clip(RoundedCornerShape(9.dp))
                                .background(colors.surfaceElevated),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (item.category == PortfolioCategory.GOLD) Icons.Default.Star else PortfolioCoinVector,
                                contentDescription = null,
                                tint = colors.goldPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = item.title,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textMain
                            )
                            val subtitle = if (item.category == PortfolioCategory.GOLD) {
                                "${PersianNumberFormatter.formatWeight(item.weightGrams)} گرم | ${item.karat.labelFa}"
                            } else {
                                "${PersianNumberFormatter.toPersianDigits(item.quantity.toString())} عدد ${item.coinType?.titleFa ?: "سکه"}"
                            }
                            Text(
                                text = subtitle,
                                fontSize = 11.sp,
                                color = colors.textMuted
                            )
                        }
                    }

                    IconButton(
                        onClick = {
                            viewModel.deletePortfolioItem(item.id)
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "حذف دارایی",
                            tint = colors.errorRed.copy(alpha = 0.7f),
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }

                HorizontalDivider(color = colors.border, thickness = 0.5.dp)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("ارزش روز:", fontSize = 10.sp, color = colors.textMuted)
                        AnimatedPriceTicker(
                            text = "${PersianNumberFormatter.formatPrice(curVal.toDouble())} ت",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.goldPrimary
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text("سود / زیان:", fontSize = 10.sp, color = colors.textMuted)
                        AnimatedPriceTicker(
                            text = "$itemProfitSign${PersianNumberFormatter.formatPrice(profit.toDouble())} ت ($itemProfitSign${PersianNumberFormatter.toPersianDigits("%.1f".format(profitPct))}٪)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = itemProfitColor,
                            contentAlignment = Alignment.CenterEnd
                        )
                    }
                }
            }
        }
    }

    // Add Asset Dialog Modal
    if (showAddDialog) {
        AddAssetDialog(
            rates = uiState.rates,
            onDismiss = { showAddDialog = false },
            onConfirm = { newItem ->
                viewModel.addPortfolioItem(newItem)
                showAddDialog = false
            }
        )
    }
}

@Composable
fun AddAssetDialog(
    rates: com.goldex.companion.data.MarketRates,
    onDismiss: () -> Unit,
    onConfirm: (PortfolioItem) -> Unit
) {
    val colors = LocalGoldExColors.current
    var selectedCategory by remember { mutableStateOf(PortfolioCategory.GOLD) }
    var title by remember { mutableStateOf("") }
    var weightInput by remember { mutableStateOf("5.0") }
    var selectedKarat by remember { mutableStateOf(Karat.K18) }
    var coinQuantity by remember { mutableStateOf("1") }
    var selectedCoinType by remember { mutableStateOf(CoinType.EMAMI) }
    var purchasePriceInput by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = colors.surface,
            border = androidx.compose.foundation.BorderStroke(0.8.dp, colors.goldBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "ثبت قلم دارایی جدید",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.goldPrimary
                )

                // Category Switcher
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(colors.surfaceElevated)
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    PortfolioCategory.values().forEach { cat ->
                        val isSel = selectedCategory == cat
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSel) colors.goldContainer else Color.Transparent)
                                .clickable {
                                    selectedCategory = cat
                                    if (title.isBlank()) {
                                        title = if (cat == PortfolioCategory.GOLD) "قطعه طلای ۱۸ عیار" else "سکه تمام بهار امامی"
                                    }
                                }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = cat.labelFa,
                                fontSize = 11.sp,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSel) colors.goldPrimary else colors.textMuted
                            )
                        }
                    }
                }

                // Title Input
                GoldInputField(
                    value = title,
                    onValueChange = { title = it },
                    label = "عنوان دارایی (اختیاری)",
                    useThousandsSeparator = false
                )

                if (selectedCategory == PortfolioCategory.GOLD) {
                    GoldInputField(
                        value = weightInput,
                        onValueChange = { weightInput = it },
                        label = "وزن قطعه طلا",
                        trailingText = "گرم",
                        isDecimal = true,
                        useThousandsSeparator = false
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Karat.values().forEach { k ->
                            val isKaratSel = selectedKarat == k
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isKaratSel) colors.goldContainer else colors.surfaceElevated,
                                border = androidx.compose.foundation.BorderStroke(
                                    0.5.dp,
                                    if (isKaratSel) colors.goldPrimary else colors.border
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { selectedKarat = k }
                            ) {
                                Text(
                                    text = k.labelFa,
                                    fontSize = 11.sp,
                                    color = if (isKaratSel) colors.goldPrimary else colors.textSecondary,
                                    modifier = Modifier.padding(vertical = 6.dp),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }
                } else {
                    GoldInputField(
                        value = coinQuantity,
                        onValueChange = { coinQuantity = it },
                        label = "تعداد سکه",
                        trailingText = "عدد",
                        useThousandsSeparator = false
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        CoinType.values().forEach { coin ->
                            val isCoinSel = selectedCoinType == coin
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isCoinSel) colors.goldContainer else colors.surfaceElevated,
                                border = androidx.compose.foundation.BorderStroke(
                                    0.5.dp,
                                    if (isCoinSel) colors.goldPrimary else colors.border
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedCoinType = coin }
                            ) {
                                Text(
                                    text = coin.titleFa,
                                    fontSize = 11.sp,
                                    color = if (isCoinSel) colors.goldPrimary else colors.textSecondary,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }

                // Purchase Price Input
                GoldInputField(
                    value = purchasePriceInput,
                    onValueChange = { purchasePriceInput = it },
                    label = "قیمت کل پرداختی هنگام خرید",
                    trailingText = "تومان",
                    useThousandsSeparator = true
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("انصراف", fontSize = 11.sp)
                    }

                    Button(
                        onClick = {
                            val w = PersianNumberFormatter.parsePersianOrEnglish(weightInput) ?: 0.0
                            val q = coinQuantity.toIntOrNull() ?: 1
                            val p = PersianNumberFormatter.parseToCleanLong(purchasePriceInput) ?: 0L
                            val finalTitle = if (title.isNotBlank()) title else if (selectedCategory == PortfolioCategory.GOLD) "قطعه طلا" else selectedCoinType.titleFa

                            val item = PortfolioItem(
                                title = finalTitle,
                                category = selectedCategory,
                                weightGrams = w,
                                karat = selectedKarat,
                                quantity = q,
                                coinType = if (selectedCategory == PortfolioCategory.COIN) selectedCoinType else null,
                                purchasePriceTotal = p
                            )
                            onConfirm(item)
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.goldPrimary,
                            contentColor = if (colors.isDark) Color(0xFF0A0B0E) else Color.White
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("ذخیره دارایی", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
