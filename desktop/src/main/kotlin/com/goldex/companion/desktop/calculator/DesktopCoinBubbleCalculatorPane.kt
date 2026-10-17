package com.goldex.companion.desktop.calculator

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import com.goldex.companion.desktop.ui.controls.DesktopAlertDialog as AlertDialog
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.goldex.companion.data.MarketRates
import com.goldex.companion.desktop.ui.controls.DesktopActionButton
import com.goldex.companion.desktop.ui.controls.DesktopFilterChip
import com.goldex.companion.desktop.ui.controls.DesktopFilterRow
import com.goldex.companion.desktop.ui.controls.DesktopOutlinedTextField
import com.goldex.companion.model.CoinBubbleResult
import com.goldex.companion.model.CoinType
import com.goldex.companion.model.PersianNumberFormatter
import com.goldex.companion.ui.components.AnimatedPriceTicker
import com.goldex.companion.ui.theme.LocalGoldExColors
import com.goldex.companion.ui.theme.heroCardGradient
import com.goldex.companion.ui.util.ThousandsSeparatorVisualTransformation

@Composable
fun DesktopCoinBubbleCalculatorPane(
    rates: MarketRates
) {
    val colors = LocalGoldExColors.current
    val clipboard = LocalClipboardManager.current

    var selectedCoinFilter by remember { mutableStateOf<CoinType?>(null) }
    val priceOverrides = remember { mutableStateMapOf<CoinType, Long>() }
    var editingCoin by remember { mutableStateOf<CoinType?>(null) }
    var editPriceInput by remember { mutableStateOf("") }
    var copied by remember { mutableStateOf(false) }

    val usd = rates.usd.toDouble()
    val ons = rates.ons
    val gram24Price = if (ons > 0 && usd > 0) (ons * usd) / 31.1035 else 0.0

    fun calculateCoin(coin: CoinType): CoinBubbleResult {
        val intrinsic = (coin.pureWeightGrams * gram24Price) + coin.mintFee
        val marketPrice = priceOverrides[coin]?.toDouble() ?: when (coin) {
            CoinType.EMAMI -> rates.coinEmami.toDouble()
            CoinType.BAHAR -> rates.coinBahar.toDouble()
            CoinType.HALF -> rates.coinHalf.toDouble()
            CoinType.QUARTER -> rates.coinQuarter.toDouble()
            CoinType.GERAMI -> rates.coinGerami.toDouble()
        }
        val bubble = marketPrice - intrinsic
        val bubblePercent = if (intrinsic > 0) (bubble / intrinsic) * 100.0 else 0.0
        return CoinBubbleResult(coin, marketPrice, intrinsic, bubble, bubblePercent)
    }

    val allResults = remember(rates, priceOverrides.toMap()) {
        CoinType.values().map { calculateCoin(it) }
    }

    val displayResults = if (selectedCoinFilter != null) {
        allResults.filter { it.coinType == selectedCoinFilter }
    } else {
        allResults
    }

    val lowestBubbleCoin = allResults.minByOrNull { it.bubblePercent }
    val highestBubbleCoin = allResults.maxByOrNull { it.bubblePercent }

    fun buildSummaryText(): String = buildString {
        appendLine("🪙 جدول جامع تحلیل حباب مسکوکات بانکی (قیراط)")
        appendLine("----------------------------------------")
        appendLine("انس جهانی طلا: ${rates.ons} دلار | دلار آزاد: ${PersianNumberFormatter.formatPrice(rates.usd.toDouble())} تومان")
        appendLine("نرخ تئوریک هر گرم طلای ۲۴ عیار: ${PersianNumberFormatter.formatPrice(gram24Price)} تومان")
        appendLine("----------------------------------------")
        allResults.forEach { r ->
            appendLine("• ${r.coinType.titleFa}:")
            appendLine("  قیمت بازار: ${PersianNumberFormatter.formatPrice(r.marketPrice)} تومان")
            appendLine("  ارزش ذاتی: ${PersianNumberFormatter.formatPrice(r.intrinsicValue)} تومان")
            appendLine("  حباب: ${PersianNumberFormatter.formatPrice(r.bubbleAmount)} تومان (${PersianNumberFormatter.toPersianDigits("%.1f".format(r.bubblePercent))}٪)")
        }
        appendLine("========================================")
        appendLine("محاسبه بر مبنای فرمول رسمی صرافان و زرگران کشور")
    }

    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Market Rates Indicator Bar
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = colors.surface,
            border = BorderStroke(0.8.dp, colors.goldBorder),
            shadowElevation = if (colors.isDark) 0.dp else 1.5.dp,
            modifier = Modifier.fillMaxWidth().testTag("coin-bubble-market-bar")
        ) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(colors.surfaceElevated),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Outlined.MonetizationOn, null, Modifier.size(22.dp), tint = colors.goldPrimary)
                    }
                    Column {
                        Text("مبنای جهانی محاسبه حباب", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = colors.textMain)
                        Text("فرمول انس جهانی × نرخ دلار ÷ ۳۱.۱۰۳۵", fontSize = 11.5.sp, color = colors.textMuted)
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text("انس جهانی طلا", fontSize = 11.sp, color = colors.textMuted)
                        Text("${rates.ons} $", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = colors.textMain)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("دلار آزاد بازار", fontSize = 11.sp, color = colors.textMuted)
                        Text("${PersianNumberFormatter.formatPrice(rates.usd.toDouble())} تومان", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = colors.textMain)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("گرم ۲۴ عیار جهانی", fontSize = 11.sp, color = colors.textMuted)
                        Text("${PersianNumberFormatter.formatPrice(gram24Price)} تومان", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = colors.goldSecondary)
                    }
                }
            }
        }

        // Filter and Copy Action Row
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                DesktopFilterChip(
                    text = "همه مسکوکات",
                    selected = selectedCoinFilter == null,
                    onClick = { selectedCoinFilter = null },
                    modifier = Modifier.testTag("filter-coin-all")
                )
                CoinType.values().forEach { coin ->
                    DesktopFilterChip(
                        text = coin.titleFa.replace("بهار آزادی", "").replace("تمام سکه", "سکه").trim(),
                        selected = selectedCoinFilter == coin,
                        onClick = { selectedCoinFilter = coin },
                        modifier = Modifier.testTag("filter-coin-${coin.name}")
                    )
                }
            }

            DesktopActionButton(
                text = if (copied) "جدول حباب کپی شد ✓" else "کپی جدول حباب مسکوکات",
                onClick = {
                    try {
                        clipboard.setText(AnnotatedString(buildSummaryText()))
                    } catch (_: Throwable) {}
                    copied = true
                },
                icon = if (copied) Icons.Outlined.Check else Icons.Outlined.ContentCopy,
                isSecondary = true,
                height = 42.dp,
                modifier = Modifier.testTag("coin-copy-table")
            )
        }

        // Coin Cards
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
            val columns = if (maxWidth >= 1080.dp) 3 else if (maxWidth >= 720.dp) 2 else 1
            val scrollState = rememberScrollState()

            Column(
                Modifier.fillMaxSize().verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                displayResults.chunked(columns).forEach { rowItems ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        rowItems.forEach { result ->
                            Box(Modifier.weight(1f)) {
                                CoinCard(
                                    result = result,
                                    onEditPrice = {
                                        editingCoin = result.coinType
                                        editPriceInput = result.marketPrice.toLong().toString()
                                    },
                                    onResetPrice = {
                                        priceOverrides.remove(result.coinType)
                                    },
                                    isOverridden = priceOverrides.containsKey(result.coinType)
                                )
                            }
                        }
                        // Fill remainder if needed
                        repeat(columns - rowItems.size) {
                            Spacer(Modifier.weight(1f))
                        }
                    }
                }

                // Market Insights Summary Card
                if (lowestBubbleCoin != null && highestBubbleCoin != null) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = colors.surfaceElevated,
                        border = BorderStroke(0.6.dp, colors.border),
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp).testTag("coin-insights-card")
                    ) {
                        Row(
                            Modifier.fillMaxWidth().padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Icon(Icons.Outlined.Insights, null, Modifier.size(22.dp), tint = colors.goldPrimary)
                                Column {
                                    Text("ارزیابی تحلیلی حباب مسکوکات برای خریداران و سرمایه‌گذاران", fontWeight = FontWeight.Bold, fontSize = 13.5.sp, color = colors.textMain)
                                    Text("سنجش ریسک حباب قیمت بازار نسبت به طلای فیزیکی درون سکه", fontSize = 11.5.sp, color = colors.textMuted)
                                }
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("کمترین حباب (بهترین برای خرید):", fontSize = 11.sp, color = colors.profitGreen)
                                    Text(
                                        "${lowestBubbleCoin.coinType.titleFa} (${PersianNumberFormatter.toPersianDigits("%.1f".format(lowestBubbleCoin.bubblePercent))}٪)",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.textMain
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("بیشترین حباب (بالاترین ریسک):", fontSize = 11.sp, color = colors.errorRed)
                                    Text(
                                        "${highestBubbleCoin.coinType.titleFa} (${PersianNumberFormatter.toPersianDigits("%.1f".format(highestBubbleCoin.bubblePercent))}٪)",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.textMain
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Edit Coin Price Dialog
    editingCoin?.let { coin ->
        AlertDialog(
            onDismissRequest = { editingCoin = null },
            title = { Text("تغییر قیمت دستی ${coin.titleFa}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("قیمت دلخواه بازار را برای محاسبه حباب وارد فرمایید:", fontSize = 13.sp, color = colors.textMuted)
                    DesktopOutlinedTextField(
                        value = editPriceInput,
                        onValueChange = { editPriceInput = it },
                        label = { Text("قیمت سکه به تومان") },
                        prefix = { Text("تومان ", color = colors.textMuted, fontSize = 13.sp) },
                        visualTransformation = ThousandsSeparatorVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("coin-edit-price-field")
                    )
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    DesktopActionButton(
                        text = "انصراف",
                        onClick = { editingCoin = null },
                        icon = Icons.Outlined.Close,
                        isSecondary = true,
                        modifier = Modifier.weight(1f)
                    )
                    DesktopActionButton(
                        text = "ثبت قیمت",
                        onClick = {
                            val price = PersianNumberFormatter.toEnglishDigits(editPriceInput).toLongOrNull()
                            if (price != null && price > 0) {
                                priceOverrides[coin] = price
                            }
                            editingCoin = null
                        },
                        icon = Icons.Outlined.Check,
                        modifier = Modifier.weight(1.2f).testTag("coin-confirm-price")
                    )
                }
            }
        )
    }
}

@Composable
private fun CoinCard(
    result: CoinBubbleResult,
    onEditPrice: () -> Unit,
    onResetPrice: () -> Unit,
    isOverridden: Boolean
) {
    val colors = LocalGoldExColors.current

    val badgeColor = when {
        result.bubblePercent < 12.0 -> colors.profitGreen
        result.bubblePercent < 22.0 -> colors.goldPrimary
        else -> colors.errorRed
    }

    val badgeLabel = when {
        result.bubblePercent < 12.0 -> "حباب منطقی"
        result.bubblePercent < 22.0 -> "حباب متوسط"
        else -> "حباب بالا و پرریسک"
    }

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = colors.surface,
        border = BorderStroke(0.8.dp, if (isOverridden) colors.goldPrimary else colors.goldBorder),
        shadowElevation = if (colors.isDark) 0.dp else 2.dp,
        modifier = Modifier.fillMaxWidth().testTag("coin-card-${result.coinType.name}")
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            // Header
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        Modifier.size(36.dp).clip(RoundedCornerShape(8.dp)).background(colors.surfaceElevated),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Outlined.Paid, null, Modifier.size(20.dp), tint = colors.goldPrimary)
                    }
                    Column {
                        Text(result.coinType.titleFa, fontWeight = FontWeight.Bold, fontSize = 14.5.sp, color = colors.textMain)
                        Text(
                            "وزن: ${PersianNumberFormatter.formatAccountWeight(result.coinType.totalWeightGrams)} گرم | عیار ۹۰۰",
                            fontSize = 11.sp,
                            color = colors.textMuted
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = badgeColor.copy(alpha = 0.15f),
                    border = BorderStroke(0.5.dp, badgeColor.copy(alpha = 0.5f))
                ) {
                    Text(
                        badgeLabel,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = badgeColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            // Market Price Banner
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = colors.surfaceElevated,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("قیمت بازار", fontSize = 11.5.sp, color = colors.textMuted)
                        Text(
                            "${PersianNumberFormatter.formatPrice(result.marketPrice)} تومان",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textMain
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (isOverridden) {
                            DesktopActionButton(
                                text = "بازنشانی",
                                onClick = onResetPrice,
                                icon = Icons.Outlined.RestartAlt,
                                isSecondary = true,
                                height = 34.dp
                            )
                        }
                        DesktopActionButton(
                            text = "تغییر قیمت",
                            onClick = onEditPrice,
                            icon = Icons.Outlined.Edit,
                            isSecondary = true,
                            height = 34.dp
                        )
                    }
                }
            }

            // Intrinsic vs Bubble Breakdown
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("ارزش ذاتی طلا (وزن خالص × نرخ جهانی):", fontSize = 11.5.sp, color = colors.textMuted)
                    Text(
                        "${PersianNumberFormatter.formatPrice(result.intrinsicValue)} تومان",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textSecondary
                    )
                }

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("مبلغ حباب سکه:", fontSize = 12.sp, color = colors.textMain, fontWeight = FontWeight.Medium)
                    Text(
                        "${PersianNumberFormatter.formatPrice(result.bubbleAmount)} تومان",
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = badgeColor
                    )
                }

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("درصد حباب از ارزش ذاتی:", fontSize = 12.sp, color = colors.textMain, fontWeight = FontWeight.Medium)
                    Text(
                        "${PersianNumberFormatter.toPersianDigits("%.1f".format(result.bubblePercent))}٪",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        color = badgeColor
                    )
                }
            }
        }
    }
}
