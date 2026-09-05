package com.goldex.companion.ui.calculator.tabs

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.goldex.companion.model.CoinBubbleResult
import com.goldex.companion.model.CoinType
import com.goldex.companion.model.PersianNumberFormatter
import com.goldex.companion.ui.calculator.*
import com.goldex.companion.ui.components.AnimatedPriceTicker
import com.goldex.companion.ui.components.GoldInputField
import com.goldex.companion.ui.theme.LocalGoldExColors
import com.goldex.companion.ui.theme.heroCardGradient

/**
 * Rebuilt Coin Bubble Analyzer Tab adhering strictly to Google Stitch
 * Design Screen ID: 738122c0846041f986ec5dc86a8bea7c
 * ("قیراط - تحلیل‌گر حباب انواع سکه و انس جهانی")
 */
@Composable
fun CoinBubbleTab(
    viewModel: GoldCalculatorViewModel,
    uiState: CalculatorUiState
) {
    val context = LocalContext.current
    val colors = LocalGoldExColors.current

    // Selected filter: null means all coins
    var selectedFilterCoin by remember { mutableStateOf<CoinType?>(null) }
    var isManualPriceExpanded by remember { mutableStateOf(false) }

    // Calculate coin bubble metrics helper for any coin
    fun calculateMetrics(coin: CoinType, overridePrice: Double? = null): CoinBubbleResult {
        val usd = uiState.rates.usd.toDouble()
        val ons = uiState.rates.ons
        val gram24Price = if (ons > 0 && usd > 0) (ons * usd) / 31.1035 else 0.0
        val intrinsic = (coin.pureWeightGrams * gram24Price) + coin.mintFee

        val marketPrice = overridePrice ?: when (coin) {
            CoinType.EMAMI -> uiState.rates.coinEmami.toDouble()
            CoinType.BAHAR -> uiState.rates.coinBahar.toDouble()
            CoinType.HALF -> uiState.rates.coinHalf.toDouble()
            CoinType.QUARTER -> uiState.rates.coinQuarter.toDouble()
            CoinType.GERAMI -> uiState.rates.coinGerami.toDouble()
        }

        val bubble = marketPrice - intrinsic
        val bubblePercent = if (intrinsic > 0) (bubble / intrinsic) * 100.0 else 0.0
        return CoinBubbleResult(coin, marketPrice, intrinsic, bubble, bubblePercent)
    }

    val coinsToDisplay = if (selectedFilterCoin != null) listOf(selectedFilterCoin!!) else CoinType.values().toList()

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // ─── Subheader / Navigation Bar ──────────────────────────────
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = colors.surface,
            border = BorderStroke(0.8.dp, colors.goldBorder),
            shadowElevation = if (colors.isDark) 0.dp else 1.5.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
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
                            .background(colors.surfaceElevated)
                            .border(0.8.dp, colors.goldBorder, RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MonetizationOn,
                            contentDescription = null,
                            tint = colors.goldPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "تحلیل‌گر حباب انواع سکه",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textMain
                        )
                        Text(
                            text = "محاسبه ارزش ذاتی بر پایه انس طلا و دلار آزاد",
                            fontSize = 10.sp,
                            color = colors.textMuted
                        )
                    }
                }

                // Refresh Live Rates Button
                Button(
                    onClick = {
                        viewModel.refreshRates()
                        Toast.makeText(context, "در حال همگام‌سازی نرخ‌ها...", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.goldPrimary,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Icon(
                        imageVector = CalcSync,
                        contentDescription = "بروزرسانی",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "بروزرسانی", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // ─── 1. Hero Dark Card (شاخص‌های مبنای محاسبه حباب) ─────────
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = Color.Transparent,
            border = BorderStroke(0.8.dp, colors.goldBorder),
            shadowElevation = if (colors.isDark) 0.dp else 4.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.heroCardGradient)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Top Row: Indicator & Badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(colors.profitGreen)
                        )
                        Text(
                            text = "شاخص‌های مبنای محاسبه حباب",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color.White.copy(alpha = 0.08f),
                        border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.15f))
                    ) {
                        Text(
                            text = "زنده اتحادیه",
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        )
                    }
                }

                // Grid 2 Columns: Ounce & USD
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Col 1: Ounce Gold
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color.White.copy(alpha = 0.06f),
                        border = BorderStroke(0.6.dp, Color.White.copy(alpha = 0.12f)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "انس جهانی طلا (Ounce)",
                                fontSize = 10.sp,
                                color = Color.White.copy(alpha = 0.6f)
                            )
                            AnimatedPriceTicker(
                                text = "${PersianNumberFormatter.formatWeight(uiState.rates.ons)} $",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                color = colors.goldPrimary
                            )
                            Text(
                                text = "بازار جهانی فلزات",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Medium,
                                color = colors.profitGreen
                            )
                        }
                    }

                    // Col 2: US Dollar Rate
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color.White.copy(alpha = 0.06f),
                        border = BorderStroke(0.6.dp, Color.White.copy(alpha = 0.12f)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "دلار مبنای بازار طلا",
                                fontSize = 10.sp,
                                color = Color.White.copy(alpha = 0.6f)
                            )
                            AnimatedPriceTicker(
                                text = "${PersianNumberFormatter.formatPrice(uiState.rates.usd.toDouble())} ت",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                            Text(
                                text = "نرخ حواله آزاد",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Medium,
                                color = colors.goldSecondary
                            )
                        }
                    }
                }

                // Formula pill
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color.Black.copy(alpha = 0.35f),
                    border = BorderStroke(0.5.dp, colors.goldBorder.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "فرمول ارزش ذاتی سکه:",
                            fontSize = 10.sp,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                        Text(
                            text = "(انس × دلار × وزن × ۰.۹۰۰) ÷ ۳۱.۱۰۳۵",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.goldPrimary
                        )
                    }
                }
            }
        }

        // ─── 2. Coin Filter Pills Row ────────────────────────────────
        val filterScrollState = rememberScrollState()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(filterScrollState),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // "همه سکه‌ها"
            val isAll = selectedFilterCoin == null
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (isAll) colors.goldPrimary else colors.surfaceElevated,
                border = BorderStroke(if (isAll) 1.dp else 0.5.dp, if (isAll) colors.goldPrimary else colors.border),
                modifier = Modifier.clickable { selectedFilterCoin = null }
            ) {
                Text(
                    text = "همه سکه‌ها (${CoinType.values().size})",
                    fontSize = 11.sp,
                    fontWeight = if (isAll) FontWeight.Bold else FontWeight.Medium,
                    color = if (isAll) Color.Black else colors.textSecondary,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                )
            }

            CoinType.values().forEach { coin ->
                val isSelected = selectedFilterCoin == coin
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isSelected) colors.goldPrimary else colors.surfaceElevated,
                    border = BorderStroke(if (isSelected) 1.dp else 0.5.dp, if (isSelected) colors.goldPrimary else colors.border),
                    modifier = Modifier.clickable { selectedFilterCoin = coin }
                ) {
                    Text(
                        text = coin.titleFa,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) Color.Black else colors.textSecondary,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                    )
                }
            }
        }

        // ─── 3. Coin Bubble Detail Cards ─────────────────────────────
        coinsToDisplay.forEach { coin ->
            val isSelectedForInput = uiState.selectedCoin == coin
            val currentPrice = if (isSelectedForInput) {
                PersianNumberFormatter.parsePersianOrEnglish(uiState.coinMarketPriceInput) ?: 0.0
            } else null
            val res = calculateMetrics(coin, currentPrice)

            Surface(
                shape = RoundedCornerShape(18.dp),
                color = colors.surface,
                border = BorderStroke(
                    if (isSelectedForInput) 1.2.dp else 0.8.dp,
                    if (isSelectedForInput) colors.goldPrimary else colors.goldBorder
                ),
                shadowElevation = if (colors.isDark) 0.dp else 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Header of Coin Card
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
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(colors.goldContainer)
                                    .border(0.6.dp, colors.goldPrimary, RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MonetizationOn,
                                    contentDescription = null,
                                    tint = colors.goldPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = coin.titleFa,
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.textMain
                                )
                                Text(
                                    text = "وزن: ${PersianNumberFormatter.toPersianDigits(coin.totalWeightGrams.toString())} گرم | عیار ۹۰۰ (۲۱.۶)",
                                    fontSize = 10.sp,
                                    color = colors.textMuted
                                )
                            }
                        }

                        // Bubble Percentage Badge
                        val isPositiveBubble = res.bubbleAmount >= 0
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isPositiveBubble) colors.errorRed.copy(alpha = 0.12f) else colors.profitGreen.copy(alpha = 0.12f),
                            border = BorderStroke(
                                0.5.dp,
                                if (isPositiveBubble) colors.errorRed.copy(alpha = 0.4f) else colors.profitGreen.copy(alpha = 0.4f)
                            )
                        ) {
                            Text(
                                text = "حباب: ${PersianNumberFormatter.formatWeight(res.bubblePercent)}٪",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isPositiveBubble) colors.errorRed else colors.profitGreen,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                            )
                        }
                    }

                    // 3 Metric Boxes: Market Price | Intrinsic Gold Value | Bubble Amount
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = colors.surfaceElevated,
                        border = BorderStroke(0.5.dp, colors.border),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Market Price
                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("قیمت روز بازار", fontSize = 9.5.sp, color = colors.textMuted)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = PersianNumberFormatter.formatPrice(res.marketPrice),
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.textMain
                                )
                                Text("تومان", fontSize = 8.5.sp, color = colors.textSecondary)
                            }

                            // Intrinsic Value
                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("ارزش طلای خام", fontSize = 9.5.sp, color = colors.textMuted)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = PersianNumberFormatter.formatPrice(res.intrinsicValue),
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.profitGreen
                                )
                                Text("تومان", fontSize = 8.5.sp, color = colors.textSecondary)
                            }

                            // Bubble Amount
                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("مبلغ حباب سکه", fontSize = 9.5.sp, color = colors.textMuted)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = PersianNumberFormatter.formatPrice(res.bubbleAmount),
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.errorRed
                                )
                                Text("تومان اضافه", fontSize = 8.5.sp, color = colors.errorRed)
                            }
                        }
                    }

                    // Visual Progress Split Bar (Gold Share vs Bubble Share)
                    val intrinsicShare = if (res.marketPrice > 0) ((res.intrinsicValue / res.marketPrice) * 100.0).coerceIn(0.0, 100.0) else 100.0
                    val bubbleShare = (100.0 - intrinsicShare).coerceIn(0.0, 100.0)

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "سهم طلا: ${PersianNumberFormatter.formatWeight(intrinsicShare)}٪",
                                fontSize = 9.5.sp,
                                color = colors.textSecondary
                            )
                            Text(
                                text = "سهم حباب روانی: ${PersianNumberFormatter.formatWeight(bubbleShare)}٪",
                                fontSize = 9.5.sp,
                                color = colors.errorRed
                            )
                        }

                        // Split progress track
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(CircleShape)
                                .background(colors.surfaceElevated)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .weight(intrinsicShare.toFloat().coerceAtLeast(0.01f))
                                    .background(colors.goldPrimary)
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .weight(bubbleShare.toFloat().coerceAtLeast(0.01f))
                                    .background(colors.errorRed)
                            )
                        }
                    }

                    // Price Edit & Adjustment Toggle
                    if (isSelectedForInput) {
                        AnimatedVisibility(visible = isManualPriceExpanded) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                GoldInputField(
                                    value = uiState.coinMarketPriceInput,
                                    onValueChange = { viewModel.onCoinMarketPriceChanged(it) },
                                    label = "تغییر دستی قیمت روز ${coin.titleFa}",
                                    trailingText = "تومان",
                                    useThousandsSeparator = true
                                )
                            }
                        }
                    }

                    // Bottom Action Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isSelectedForInput) "سکه فعال جهت محاسبه" else "کلیک جهت تغییر قیمت دستی",
                            fontSize = 10.sp,
                            color = if (isSelectedForInput) colors.goldPrimary else colors.textMuted
                        )

                        TextButton(
                            onClick = {
                                viewModel.onCoinTypeSelected(coin)
                                isManualPriceExpanded = !isManualPriceExpanded
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = if (isSelectedForInput && isManualPriceExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.Edit,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = colors.goldPrimary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isSelectedForInput && isManualPriceExpanded) "بستن ویرایش" else "تنظیم قیمت",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.goldPrimary
                            )
                        }
                    }
                }
            }
        }

        // ─── 4. Quick Action Button ──────────────────────────────────
        Button(
            onClick = {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val sb = StringBuilder()
                sb.append("🪙 گزارش تحلیلی حباب انواع سکه (قیراط)\n")
                sb.append("انس جهانی: ${PersianNumberFormatter.formatWeight(uiState.rates.ons)} $ | دلار: ${PersianNumberFormatter.formatPrice(uiState.rates.usd.toDouble())} ت\n")
                sb.append("------------------------------------\n")
                CoinType.values().forEach { coin ->
                    val m = calculateMetrics(coin)
                    sb.append("• ${coin.titleFa}: بازار ${PersianNumberFormatter.formatPrice(m.marketPrice)} ت | ذاتی ${PersianNumberFormatter.formatPrice(m.intrinsicValue)} ت | حباب: ${PersianNumberFormatter.formatPrice(m.bubbleAmount)} ت (${PersianNumberFormatter.formatWeight(m.bubblePercent)}٪)\n")
                }
                clipboard.setPrimaryClip(ClipData.newPlainText("Coin Bubbles", sb.toString()))
                Toast.makeText(context, "گزارش حباب انواع سکه در کلیپ‌بورد کپی شد ✓", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = colors.goldPrimary,
                contentColor = Color.Black
            )
        ) {
            Icon(CalcContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("کپی گزارش کامل حباب تمام سکه‌ها", fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
        }
    }
}
