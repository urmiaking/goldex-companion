package com.goldex.companion.ui.calculator.screens

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.goldex.companion.model.CoinBubbleResult
import com.goldex.companion.model.CoinType
import com.goldex.companion.model.PersianNumberFormatter
import com.goldex.companion.ui.calculator.*
import com.goldex.companion.ui.components.AnimatedPriceTicker
import com.goldex.companion.ui.components.GoldButton
import com.goldex.companion.ui.components.GoldInputField
import com.goldex.companion.ui.hub.HubArrowRight
import com.goldex.companion.ui.theme.LocalGoldExColors
import com.goldex.companion.ui.theme.heroCardGradient

/**
 * Dedicated Full Screen: Coin Bubble Analyzer
 * Adheres strictly to Google Stitch Screen ID: 738122c0846041f986ec5dc86a8bea7c
 * ("قیراط - تحلیل‌گر حباب انواع سکه و انس جهانی")
 */
@Composable
fun CoinBubbleScreen(
    viewModel: GoldCalculatorViewModel,
    uiState: CalculatorUiState,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val colors = LocalGoldExColors.current
    val scrollState = rememberScrollState()

    var selectedFilterCoin by remember { mutableStateOf<CoinType?>(null) }
    var isManualPriceExpanded by remember { mutableStateOf(false) }

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

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Scaffold(
            modifier = modifier
                .fillMaxSize()
                .background(colors.background),
            containerColor = colors.background,
            topBar = {
                Surface(
                    color = colors.surface,
                    border = BorderStroke(0.6.dp, colors.goldBorder.copy(alpha = 0.5f)),
                    shadowElevation = 3.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            IconButton(
                                onClick = onBack,
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(colors.surfaceElevated)
                                    .border(0.6.dp, colors.goldBorder, CircleShape)
                            ) {
                                Icon(
                                    imageVector = HubArrowRight,
                                    contentDescription = "بازگشت",
                                    tint = colors.goldPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Column {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(7.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFF59E0B))
                                    )
                                    Text(
                                        text = "تحلیل‌گر حباب انواع سکه",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = colors.textMain
                                    )
                                }
                                Text(
                                    text = "محاسبه ارزش ذاتی بر پایه انس طلا و دلار آزاد",
                                    fontSize = 10.5.sp,
                                    color = colors.textMuted
                                )
                            }
                        }

                        // Refresh Rates Button
                        GoldButton(
                            text = "بروزرسانی",
                            icon = CalcSync,
                            onClick = {
                                viewModel.refreshRates()
                                Toast.makeText(context, "در حال همگام‌سازی نرخ‌ها...", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.height(36.dp)
                        )
                    }
                }
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(scrollState)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // ─── 1. Hero Dark Card (شاخص‌های مبنای محاسبه حباب) ─────────
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.Transparent,
                    border = BorderStroke(0.6.dp, colors.goldBorder),
                    shadowElevation = if (colors.isDark) 0.dp else 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(colors.heroCardGradient)
                            .padding(16.dp),
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
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF34D399))
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
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFFFDE68A),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        // 2 Columns: World Ounce + USD Spot
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // World Ounce (Gold Spot)
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = Color.White.copy(alpha = 0.05f),
                                border = BorderStroke(0.6.dp, Color.White.copy(alpha = 0.10f)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "انس جهانی طلا (Ounce):",
                                        fontSize = 10.5.sp,
                                        color = Color(0xFF94A3B8)
                                    )
                                    Row(
                                        verticalAlignment = Alignment.Bottom,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = PersianNumberFormatter.formatDouble(uiState.rates.ons, 2),
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color(0xFFFBBF24)
                                        )
                                        Text(
                                            text = "$",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFE2E8F0),
                                            modifier = Modifier.padding(bottom = 2.dp)
                                        )
                                    }
                                    Text(
                                        text = "هر انس = ۳۱.۱۰۳۵ گرم خالص",
                                        fontSize = 9.sp,
                                        color = Color(0xFF34D399)
                                    )
                                }
                            }

                            // USD Exchange Rate
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = Color.White.copy(alpha = 0.05f),
                                border = BorderStroke(0.6.dp, Color.White.copy(alpha = 0.10f)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "دلار مبنای بازار طلا:",
                                        fontSize = 10.5.sp,
                                        color = Color(0xFF94A3B8)
                                    )
                                    Row(
                                        verticalAlignment = Alignment.Bottom,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = PersianNumberFormatter.formatPrice(uiState.rates.usd.toDouble()),
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color.White
                                        )
                                        Text(
                                            text = "تومان",
                                            fontSize = 10.sp,
                                            color = Color(0xFFE2E8F0),
                                            modifier = Modifier.padding(bottom = 2.dp)
                                        )
                                    }
                                    Text(
                                        text = "شاخص بازار آزاد تهران",
                                        fontSize = 9.sp,
                                        color = Color(0xFFCBD5E1)
                                    )
                                }
                            }
                        }

                        // Formula Banner inside Dark Card
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color.Black.copy(alpha = 0.35f),
                            border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.08f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "فرمول ارزش ذاتی سکه:",
                                    fontSize = 10.sp,
                                    color = Color(0xFF94A3B8)
                                )
                                Text(
                                    text = "(وزن طلا × عیار ۹۰۰ × انس × دلار) ÷ ۳۱.۱۰۳۵",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.goldPrimary
                                )
                            }
                        }
                    }
                }

                // ─── 2. Coin Type Filter Chips ───────────────────────────────
                val filterScrollState = rememberScrollState()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(filterScrollState),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // "All Coins" chip
                    val isAllSelected = selectedFilterCoin == null
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isAllSelected) colors.goldContainer else colors.surface,
                        border = BorderStroke(
                            if (isAllSelected) 1.2.dp else 0.5.dp,
                            if (isAllSelected) colors.goldPrimary else colors.border
                        ),
                        modifier = Modifier.clickable { selectedFilterCoin = null }
                    ) {
                        Text(
                            text = "همه مسکوکات",
                            fontSize = 11.5.sp,
                            fontWeight = if (isAllSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isAllSelected) colors.goldPrimary else colors.textSecondary,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                        )
                    }

                    // Individual Coins
                    CoinType.values().forEach { coin ->
                        val isSelected = selectedFilterCoin == coin
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) colors.goldContainer else colors.surface,
                            border = BorderStroke(
                                if (isSelected) 1.2.dp else 0.5.dp,
                                if (isSelected) colors.goldPrimary else colors.border
                            ),
                            modifier = Modifier.clickable { selectedFilterCoin = coin }
                        ) {
                            Text(
                                text = coin.displayName,
                                fontSize = 11.5.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) colors.goldPrimary else colors.textSecondary,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                            )
                        }
                    }
                }

                // ─── 3. Coin Bubble Detail Cards ─────────────────────────────
                coinsToDisplay.forEach { coin ->
                    val metrics = calculateMetrics(coin)
                    val isBubbleHigh = metrics.bubblePercent > 18.0

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = colors.surface,
                        border = BorderStroke(0.6.dp, colors.goldBorder.copy(alpha = 0.5f)),
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
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(CircleShape)
                                            .background(colors.goldContainer)
                                            .border(0.8.dp, colors.goldPrimary, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "۹۰۰",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = colors.goldPrimary
                                        )
                                    }

                                    Column {
                                        Text(
                                            text = coin.displayName,
                                            fontSize = 13.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = colors.textMain
                                        )
                                        Text(
                                            text = "وزن: ${coin.weightGrams} گرم (طلای خالص: ${coin.pureWeightGrams} گرم)",
                                            fontSize = 10.sp,
                                            color = colors.textMuted
                                        )
                                    }
                                }

                                // Bubble Percentage Badge
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isBubbleHigh) Color(0xFFEF4444).copy(alpha = 0.12f) else colors.goldPrimary.copy(alpha = 0.12f),
                                    border = BorderStroke(
                                        0.6.dp,
                                        if (isBubbleHigh) Color(0xFFEF4444).copy(alpha = 0.4f) else colors.goldPrimary.copy(alpha = 0.4f)
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = "حباب: ${PersianNumberFormatter.formatDouble(metrics.bubblePercent, 1)}٪",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isBubbleHigh) Color(0xFFEF4444) else colors.goldPrimary
                                        )
                                    }
                                }
                            }

                            // Dual-color Split Progress Bar (Intrinsic Gold vs Bubble)
                            val intrinsicRatio = if (metrics.marketPrice > 0) (metrics.intrinsicValue / metrics.marketPrice).toFloat().coerceIn(0.1f, 0.95f) else 0.8f
                            val bubbleRatio = 1f - intrinsicRatio

                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(10.dp)
                                        .clip(RoundedCornerShape(5.dp))
                                ) {
                                    // Gold portion
                                    Box(
                                        modifier = Modifier
                                            .weight(intrinsicRatio)
                                            .fillMaxHeight()
                                            .background(colors.goldPrimary)
                                    )
                                    // Bubble portion
                                    Box(
                                        modifier = Modifier
                                            .weight(bubbleRatio)
                                            .fillMaxHeight()
                                            .background(Color(0xFFEF4444))
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .clip(CircleShape)
                                                .background(colors.goldPrimary)
                                        )
                                        Text("ارزش طلای سکه: ${PersianNumberFormatter.formatDouble((intrinsicRatio * 100.0).toDouble(), 0)}٪", fontSize = 9.sp, color = colors.textSecondary)
                                    }
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFFEF4444))
                                        )
                                        Text("سهم حباب: ${PersianNumberFormatter.formatDouble((bubbleRatio * 100.0).toDouble(), 0)}٪", fontSize = 9.sp, color = Color(0xFFEF4444))
                                    }
                                }
                            }

                            // 2-Column Metrics: Market Price vs Intrinsic Gold Value
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(colors.surfaceElevated)
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text("ارزش ذاتی طلای سکه:", fontSize = 10.sp, color = colors.textSecondary)
                                    AnimatedPriceTicker(
                                        text = "${PersianNumberFormatter.formatPrice(metrics.intrinsicValue)} تومان",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.goldPrimary
                                    )
                                }

                                Column(
                                    horizontalAlignment = Alignment.End,
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Text("قیمت معاملاتی بازار:", fontSize = 10.sp, color = colors.textSecondary)
                                    AnimatedPriceTicker(
                                        text = "${PersianNumberFormatter.formatPrice(metrics.marketPrice)} تومان",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.textMain
                                    )
                                }
                            }

                            // Bubble Amount Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "مبلغ حباب قیمت روز:",
                                    fontSize = 11.sp,
                                    color = colors.textSecondary
                                )
                                Text(
                                    text = "${PersianNumberFormatter.formatPrice(metrics.bubbleAmount)} تومان",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isBubbleHigh) Color(0xFFEF4444) else colors.goldPrimary
                                )
                            }
                        }
                    }
                }

                // ─── 4. Quick Actions ────────────────────────────────────────
                GoldButton(
                    text = "کپی جدول تحلیلی حباب مسکوکات",
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val reportText = buildString {
                            appendLine("📊 گزارش حباب انواع مسکوکات طلا (قیراط)")
                            appendLine("─────────────────────────")
                            appendLine("انس جهانی: ${uiState.rates.ons}$ | دلار مبنا: ${PersianNumberFormatter.formatPrice(uiState.rates.usd.toDouble())} تومان")
                            CoinType.values().forEach { c ->
                                val m = calculateMetrics(c)
                                appendLine("• ${c.displayName}: بازار ${PersianNumberFormatter.formatPrice(m.marketPrice)} ت | ذاتی ${PersianNumberFormatter.formatPrice(m.intrinsicValue)} ت | حباب: ${PersianNumberFormatter.formatDouble(m.bubblePercent, 1)}٪")
                            }
                        }
                        clipboard.setPrimaryClip(ClipData.newPlainText("Coin Bubble Report", reportText))
                        Toast.makeText(context, "گزارش تحلیلی حباب سکه‌ها کپی شد ✓", Toast.LENGTH_SHORT).show()
                    },
                    icon = CalcContentCopy,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}
