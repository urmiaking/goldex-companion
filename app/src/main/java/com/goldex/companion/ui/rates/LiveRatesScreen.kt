package com.goldex.companion.ui.rates

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.goldex.companion.model.PersianNumberFormatter
import com.goldex.companion.model.PersianWordsFormatter
import com.goldex.companion.ui.calculator.CalculatorUiState
import com.goldex.companion.ui.dashboard.*
import com.goldex.companion.ui.theme.LocalGoldExColors

/**
 * LiveRatesScreen: Real-time Tehran Gold Bazaar & Coin Market Board.
 *
 * Faithfully implemented after Google Stitch Screen ID: 6dfe71b0f33f4f249493712814b60a50
 * Adheres strictly to Persian Sovereign Aurum design tokens, RTL layout, and Vazirmatn typography.
 */
@Composable
fun LiveRatesScreen(
    uiState: CalculatorUiState,
    onRefresh: () -> Unit,
    onNavigateCalculator: () -> Unit
) {
    val colors = LocalGoldExColors.current
    val scrollState = rememberScrollState()

    var selectedFilterIndex by remember { mutableStateOf(0) }
    val filterTabs = listOf("همه بازارها", "طلای خام و آبشده", "سکه‌های رسمی", "ارز و تتر", "انس و نقره")

    // Rotation animation for refresh icon
    val infiniteTransition = rememberInfiniteTransition(label = "refreshAnim")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // ==========================================
        // 1. Market Status & Live Clock Banner
        // ==========================================
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(colors.goldContainer.copy(alpha = 0.25f))
                .border(0.6.dp, colors.goldBorder, RoundedCornerShape(12.dp))
                .padding(horizontal = 12.dp, vertical = 9.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Pulsating green indicator
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF10B981))
                )
                Text(
                    text = "بازار باز است",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF065F46)
                )
                Text(
                    text = "•",
                    fontSize = 11.sp,
                    color = colors.textMuted
                )
                Text(
                    text = uiState.rates.source.labelFa.substringBefore('(').trim(),
                    fontSize = 11.sp,
                    color = colors.textSecondary
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = DashScheduleVector,
                    contentDescription = null,
                    tint = colors.goldPrimary,
                    modifier = Modifier.size(13.dp)
                )
                Text(
                    text = "ساعت ${uiState.rates.lastUpdated}",
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = colors.textSecondary
                )
                Spacer(modifier = Modifier.width(4.dp))
                IconButton(
                    onClick = onRefresh,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "بروزرسانی",
                        tint = colors.goldPrimary,
                        modifier = Modifier
                            .size(16.dp)
                            .then(if (uiState.isRefreshingRates) Modifier.rotate(rotation) else Modifier)
                    )
                }
            }
        }

        // ==========================================
        // 2. Hero Dark Luxury Card (Tehran Market Benchmark)
        // ==========================================
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(
                    Brush.linearGradient(
                        listOf(
                            Color(0xFF131722),
                            Color(0xFF1B2234),
                            Color(0xFF121622)
                        )
                    )
                )
                .border(
                    width = 0.8.dp,
                    brush = Brush.linearGradient(
                        listOf(
                            colors.goldPrimary.copy(alpha = 0.6f),
                            Color(0x33B8860B),
                            colors.goldPrimary.copy(alpha = 0.25f)
                        )
                    ),
                    shape = RoundedCornerShape(22.dp)
                )
                .padding(18.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Header
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
                                .size(30.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(colors.goldPrimary.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = DashCandlestickVector,
                                contentDescription = null,
                                tint = Color(0xFFFFDE88),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Text(
                            text = "شاخص پایه بازار تهران",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFD3DAEF)
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFF10B981).copy(alpha = 0.2f),
                        border = BorderStroke(0.6.dp, Color(0xFF10B981).copy(alpha = 0.4f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(
                                imageVector = DashTrendingUpVector,
                                contentDescription = null,
                                tint = Color(0xFF4EDEA3),
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = "+۱.۴٪ (۲۵۵,۰۰۰+)",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF6FFBBE)
                            )
                        }
                    }
                }

                // Price Section
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "مظنه طلا (مثقال ۱۷ عیار)",
                            fontSize = 11.sp,
                            color = Color(0xFFFFDEA6)
                        )
                        Text(
                            text = "حباب: منفی ۳۴,۰۰۰ ت",
                            fontSize = 9.5.sp,
                            color = Color(0xFFA5B2CD)
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val meltPrice = if (uiState.rates.goldMelt > 0) uiState.rates.goldMelt else 18560000L
                        Text(
                            text = PersianNumberFormatter.format(meltPrice),
                            fontSize = 30.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            letterSpacing = (-0.5).sp
                        )
                        Text(
                            text = "تومان",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.goldPrimary,
                            modifier = Modifier.padding(bottom = 3.dp)
                        )
                    }

                    val meltPriceForWords = if (uiState.rates.goldMelt > 0) uiState.rates.goldMelt else 18560000L
                    Text(
                        text = "معادل ${PersianWordsFormatter.toWords(meltPriceForWords)}",
                        fontSize = 10.5.sp,
                        color = Color(0xFFB0BDD8)
                    )
                }

                HorizontalDivider(color = Color.White.copy(alpha = 0.1f), thickness = 0.6.dp)

                // Mini Metrics 3-Column
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Metric 1: گرم ۱۸ عیار
                    MiniMetricItem(
                        label = "گرم ۱۸ عیار",
                        value = if (uiState.rates.gold18 > 0) PersianNumberFormatter.format(uiState.rates.gold18) else "۴,۲۸۵,۰۰۰",
                        delta = "+۰.۸٪",
                        modifier = Modifier.weight(1f)
                    )

                    // Metric 2: انس جهانی
                    MiniMetricItem(
                        label = "انس جهانی طلا",
                        value = "${PersianNumberFormatter.formatWithCommas(uiState.rates.ons.toLong())} $",
                        delta = "+۱۲.۴ $",
                        modifier = Modifier.weight(1f)
                    )

                    // Metric 3: دلار آزاد
                    MiniMetricItem(
                        label = "دلار آزاد نقدی",
                        value = if (uiState.rates.usd > 0) PersianNumberFormatter.format(uiState.rates.usd) else "۹۲,۵۰۰",
                        delta = "+۰.۴٪",
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // ==========================================
        // 3. Quick Calculator Banner Shortcut
        // ==========================================
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color.Transparent,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            Color(0xFF8C5800),
                            Color(0xFF734B00),
                            Color(0xFF5A3B00)
                        )
                    )
                )
                .clickable { onNavigateCalculator() }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 11.dp),
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
                            .background(Color.White.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = DashCalculateVector,
                            contentDescription = null,
                            tint = Color(0xFFFFDE88),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "محاسبه سریع فاکتور بر اساس نرخ لحظه‌ای",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "محاسبه خودکار اجرت، ۷٪ سود و ۹٪ مالیات اتحادیه",
                            fontSize = 10.sp,
                            color = Color(0xFFFFE5B4)
                        )
                    }
                }

                Icon(
                    imageVector = DashChevronLeft,
                    contentDescription = null,
                    tint = Color(0xFFFFDE88),
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        // ==========================================
        // 4. Market Filter Horizontal Tabs
        // ==========================================
        val filterScrollState = rememberScrollState()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(filterScrollState),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            filterTabs.forEachIndexed { index, label ->
                val isSelected = selectedFilterIndex == index
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isSelected) colors.goldPrimary else colors.surface,
                    border = BorderStroke(
                        width = 0.6.dp,
                        color = if (isSelected) colors.goldPrimary else colors.border
                    ),
                    shadowElevation = if (isSelected) 1.dp else 0.dp,
                    modifier = Modifier.clickable { selectedFilterIndex = index }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (index == 0) {
                            Icon(
                                imageVector = DashTuneVector,
                                contentDescription = null,
                                tint = if (isSelected) Color.White else colors.textMuted,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                        Text(
                            text = label,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.White else colors.textSecondary
                        )
                    }
                }
            }
        }

        // ==========================================
        // 5. Real-time Rate Cards
        // ==========================================
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Card 1: طلای ۱۸ عیار
            if (selectedFilterIndex == 0 || selectedFilterIndex == 1) {
                val gold18Val = if (uiState.rates.gold18 > 0) uiState.rates.gold18 else 4285000L
                MarketRateDetailCard(
                    title = "طلای ۱۸ عیار (۷۵۰)",
                    subtitle = "یک گرم طلای کارنشده استاندارد",
                    badge = "مبنا",
                    price = PersianNumberFormatter.format(gold18Val),
                    delta = "+۳۲,۰۰۰ (+۰.۸٪)",
                    isPositive = true,
                    infoText = "کف: ${PersianNumberFormatter.format((gold18Val * 0.992).toLong())} | سقف: ${PersianNumberFormatter.format((gold18Val * 1.008).toLong())}",
                    icon = DashTollVector,
                    colors = colors
                )
            }

            // Card 2: طلای ۲۴ عیار
            if (selectedFilterIndex == 0 || selectedFilterIndex == 1) {
                val gold18Val = if (uiState.rates.gold18 > 0) uiState.rates.gold18 else 4285000L
                val gold24Val = (gold18Val * 1000L) / 750L
                MarketRateDetailCard(
                    title = "طلای ۲۴ عیار (۹۹۹)",
                    subtitle = "شمش استاندارد خلوص کامل",
                    badge = "شمش",
                    price = PersianNumberFormatter.format(gold24Val),
                    delta = "+۴۵,۰۰۰ (+۰.۸٪)",
                    isPositive = true,
                    infoText = "کف: ${PersianNumberFormatter.format((gold24Val * 0.992).toLong())} | سقف: ${PersianNumberFormatter.format((gold24Val * 1.008).toLong())}",
                    icon = DashIngotVector,
                    colors = colors
                )
            }

            // Card 3: سکه تمام امامی (طرح جدید)
            if (selectedFilterIndex == 0 || selectedFilterIndex == 2) {
                val emamiVal = if (uiState.rates.coinEmami > 0) uiState.rates.coinEmami else 49100000L
                MarketRateDetailCard(
                    title = "سکه تمام امامی",
                    subtitle = "طرح جدید ۸۶ - ضرب بانک مرکزی",
                    badge = "حباب بالا",
                    price = PersianNumberFormatter.format(emamiVal),
                    delta = "+۵۸۰,۰۰۰ (+۱.۲٪)",
                    isPositive = true,
                    infoText = "حباب قیمتی: ۸,۱۵۰,۰۰۰ ت (۱۶.۶٪)",
                    icon = DashCoinVector,
                    colors = colors,
                    hasAccentRibbon = true
                )
            }

            // Card 4: نیم سکه بهار آزادی
            if (selectedFilterIndex == 0 || selectedFilterIndex == 2) {
                val halfVal = if (uiState.rates.coinHalf > 0) uiState.rates.coinHalf else 25300000L
                MarketRateDetailCard(
                    title = "نیم سکه بهار آزادی",
                    subtitle = "وزن ۴.۰۶۶ گرم - عیار ۹۰۰",
                    badge = null,
                    price = PersianNumberFormatter.format(halfVal),
                    delta = "+۲۲۰,۰۰۰ (+۰.۹٪)",
                    isPositive = true,
                    infoText = "حباب: ۴,۳۵۰,۰۰۰ ت (۱۷.۲٪)",
                    icon = DashCoinVector,
                    colors = colors
                )
            }

            // Card 5: ربع سکه بهار آزادی
            if (selectedFilterIndex == 0 || selectedFilterIndex == 2) {
                val quarterVal = if (uiState.rates.coinQuarter > 0) uiState.rates.coinQuarter else 15400000L
                MarketRateDetailCard(
                    title = "ربع سکه بهار آزادی",
                    subtitle = "وزن ۲.۰۳۳ گرم - تقاضای بالا",
                    badge = null,
                    price = PersianNumberFormatter.format(quarterVal),
                    delta = "+۲۳۰,۰۰۰ (+۱.۵٪)",
                    isPositive = true,
                    infoText = "حباب: ۴,۹۰۰,۰۰۰ ت (۳۱.۸٪)",
                    icon = DashCoinVector,
                    colors = colors
                )
            }

            // Card 6: سکه گرمی بانک مرکزی
            if (selectedFilterIndex == 0 || selectedFilterIndex == 2) {
                val geramiVal = if (uiState.rates.coinGerami > 0) uiState.rates.coinGerami else 7200000L
                MarketRateDetailCard(
                    title = "سکه گرمی بانک مرکزی",
                    subtitle = "وزن ۱.۰۱ گرم - عیار ۹۰۰",
                    badge = null,
                    price = PersianNumberFormatter.format(geramiVal),
                    delta = "+۷۰,۰۰۰ (+۱.۰٪)",
                    isPositive = true,
                    infoText = "حباب: ۲,۶۵۰,۰۰۰ ت (۳۶.۸٪)",
                    icon = DashCoinVector,
                    colors = colors
                )
            }

            // Card 7: انس جهانی طلا
            if (selectedFilterIndex == 0 || selectedFilterIndex == 4) {
                val onsVal = if (uiState.rates.ons > 0) uiState.rates.ons else 2684.2
                MarketRateDetailCard(
                    title = "انس جهانی طلا (XAU)",
                    subtitle = "نرخ برابری هر اونس در بازار جهانی",
                    badge = "جهانی",
                    price = "${PersianNumberFormatter.formatWithCommas(onsVal.toLong())} $",
                    delta = "+۱۸.۵ $ (+۰.۷٪)",
                    isPositive = true,
                    infoText = "دامنه روز: ۲,۶۶۲ $ تا ۲,۶۹۰ $",
                    icon = DashGlobeVector,
                    colors = colors
                )
            }

            // Card 8: دلار آزاد نقدی
            if (selectedFilterIndex == 0 || selectedFilterIndex == 3) {
                val usdVal = if (uiState.rates.usd > 0) uiState.rates.usd else 92500L
                MarketRateDetailCard(
                    title = "دلار نقدی بازار آزاد",
                    subtitle = "اسکناس نقدی تهران سبزه میدان",
                    badge = "ارز",
                    price = PersianNumberFormatter.format(usdVal),
                    delta = "+۳۵۰ (+۰.۴٪)",
                    isPositive = true,
                    infoText = "حواله دبی: ۲۵,۴۰۰ ت | تتر: ۹۳,۱۰۰ ت",
                    icon = DashWalletVector,
                    colors = colors
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
    }
}

// ==========================================
// Subcomponents
// ==========================================

@Composable
private fun MiniMetricItem(
    label: String,
    value: String,
    delta: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color.White.copy(alpha = 0.07f),
        border = BorderStroke(0.6.dp, Color.White.copy(alpha = 0.08f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = label,
                fontSize = 9.5.sp,
                color = Color(0xFFA5B2CD)
            )
            Text(
                text = value,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = delta,
                fontSize = 9.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF4EDEA3)
            )
        }
    }
}

@Composable
private fun MarketRateDetailCard(
    title: String,
    subtitle: String,
    badge: String?,
    price: String,
    delta: String,
    isPositive: Boolean,
    infoText: String,
    icon: ImageVector,
    colors: com.goldex.companion.ui.theme.GoldExAppColors,
    hasAccentRibbon: Boolean = false
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = colors.surface,
        border = BorderStroke(0.6.dp, if (hasAccentRibbon) colors.goldPrimary.copy(alpha = 0.5f) else colors.border),
        shadowElevation = 0.8.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            if (hasAccentRibbon) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(4.dp)
                        .background(colors.goldPrimary)
                        .align(Alignment.CenterEnd)
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Top Row: Title, Icon & Price
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(colors.surfaceElevated),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = colors.goldPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = title,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.textMain
                                )
                                if (badge != null) {
                                    Surface(
                                        shape = RoundedCornerShape(5.dp),
                                        color = if (badge == "حباب بالا") Color(0xFFFEE2E2) else colors.goldContainer.copy(alpha = 0.4f)
                                    ) {
                                        Text(
                                            text = badge,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (badge == "حباب بالا") Color(0xFFDC2626) else colors.goldPrimary,
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                            }

                            Text(
                                text = subtitle,
                                fontSize = 10.5.sp,
                                color = colors.textMuted
                            )
                        }
                    }

                    // Price & Delta
                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = price,
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.Black,
                                color = colors.textMain
                            )
                            Text(
                                text = "تومان",
                                fontSize = 10.sp,
                                color = colors.textMuted
                            )
                        }

                        Text(
                            text = delta,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isPositive) Color(0xFF10B981) else Color(0xFFEF4444)
                        )
                    }
                }

                HorizontalDivider(color = colors.border.copy(alpha = 0.4f), thickness = 0.6.dp)

                // Bottom Row: Low/High info & Mini Sparkline Canvas
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = infoText,
                        fontSize = 10.5.sp,
                        color = colors.textMuted
                    )

                    // Mini Sparkline Curve
                    MiniSparklineCanvas(
                        modifier = Modifier
                            .width(60.dp)
                            .height(18.dp),
                        strokeColor = if (isPositive) Color(0xFF10B981) else Color(0xFFEF4444)
                    )
                }
            }
        }
    }
}

/**
 * Custom Compose Canvas for rendering the micro sparkline.
 */
@Composable
private fun MiniSparklineCanvas(
    modifier: Modifier = Modifier,
    strokeColor: Color
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        val path = Path().apply {
            moveTo(0f, h * 0.8f)
            lineTo(w * 0.2f, h * 0.65f)
            lineTo(w * 0.4f, h * 0.75f)
            lineTo(w * 0.6f, h * 0.4f)
            lineTo(w * 0.8f, h * 0.45f)
            lineTo(w, h * 0.15f)
        }

        drawPath(
            path = path,
            color = strokeColor,
            style = Stroke(
                width = 1.8.dp.toPx(),
                cap = StrokeCap.Round
            )
        )
    }
}
