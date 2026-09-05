package com.goldex.companion.ui.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.goldex.companion.model.PersianNumberFormatter
import com.goldex.companion.ui.calculator.CalculatorUiState
import com.goldex.companion.ui.calculator.GoldCalculatorViewModel
import com.goldex.companion.ui.components.LuxuryCard
import com.goldex.companion.ui.theme.LocalGoldExColors

/**
 * DashboardScreen: The Sovereign Goldsmith Executive Dashboard.
 *
 * Faithfully implemented after Google Stitch Screen ID: 5684da48f26e4ceda789b52c96c509a4
 * Adheres strictly to Persian Sovereign Aurum design tokens, RTL layout, and Vazirmatn typography.
 */
@Composable
fun DashboardScreen(
    viewModel: GoldCalculatorViewModel,
    uiState: CalculatorUiState,
    onNavigateCalculator: () -> Unit,
    onNavigateInvoices: () -> Unit,
    onNavigateConvert: () -> Unit,
    onNavigateCoinBubble: () -> Unit,
    onNavigateMelt: () -> Unit,
    onNavigateLedger: () -> Unit
) {
    val colors = LocalGoldExColors.current
    var selectedTimeframe by remember { mutableStateOf(0) } // 0: امروز, 1: هفتگی, 2: ماهانه

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ==========================================
        // 1. Trader Welcome & Guild Accreditation Header
        // ==========================================
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Gold Gradient Avatar Ring
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    colors.goldPrimary,
                                    colors.goldBullion,
                                    Color(0xFFFFDF88)
                                )
                            )
                        )
                        .padding(1.8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(Color(0xFF141B2B)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = uiState.appSettings.managerName.take(1).ifBlank { "ق" },
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.goldPrimary
                        )
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "روز بخیر، ${uiState.appSettings.managerName.ifBlank { "استاد زرگر" }}",
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textMain
                        )
                        Icon(
                            imageVector = DashVerifiedVector,
                            contentDescription = "تأییدیه اتحادیه",
                            tint = colors.goldPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Text(
                        text = "${uiState.appSettings.galleryName.ifBlank { "بنکداری و طلا و جواهر" }} • سرای زرگران",
                        fontSize = 11.sp,
                        color = colors.textMuted
                    )
                }
            }

            // Market Active Badge
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFF10B981).copy(alpha = 0.12f),
                border = BorderStroke(0.6.dp, Color(0xFF10B981).copy(alpha = 0.35f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.5.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF10B981))
                    )
                    Text(
                        text = "بازار زنده",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF007A50)
                    )
                }
            }
        }

        // ==========================================
        // 2. Sovereign Gold Vault Asset Card
        // ==========================================
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF141A29),
                            Color(0xFF1D263B),
                            Color(0xFF111622)
                        )
                    )
                )
                .border(
                    width = 0.8.dp,
                    brush = Brush.linearGradient(
                        listOf(
                            colors.goldPrimary.copy(alpha = 0.6f),
                            Color(0x33B8860B),
                            colors.goldPrimary.copy(alpha = 0.2f)
                        )
                    ),
                    shape = RoundedCornerShape(20.dp)
                )
                .padding(18.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                // Card Top Bar
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
                                .size(32.dp)
                                .clip(RoundedCornerShape(9.dp))
                                .background(colors.goldPrimary.copy(alpha = 0.18f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = DashWalletVector,
                                contentDescription = null,
                                tint = Color(0xFFFFE088),
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Text(
                            text = "موجودی کل (خالص ۱۸ عیار)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFCFD6E8)
                        )
                    }

                    // Delta Pill
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFF10B981).copy(alpha = 0.2f),
                        border = BorderStroke(0.6.dp, Color(0xFF10B981).copy(alpha = 0.4f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = DashTrendingUpVector,
                                contentDescription = null,
                                tint = Color(0xFF4EDEA3),
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = "+۱.۲٪ (۴.۱+ گرم)",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF6FFBBE)
                            )
                        }
                    }
                }

                // Main Weight Balance
                val weightFormatted = PersianNumberFormatter.formatWeight(342.500)
                val weightFontSize = when {
                    weightFormatted.length > 11 -> 22.sp
                    weightFormatted.length > 8 -> 26.sp
                    else -> 30.sp
                }
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = weightFormatted,
                            fontSize = weightFontSize,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            maxLines = 1,
                            softWrap = false
                        )
                        Text(
                            text = "گرم ۷۵۰",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFFDEA6),
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier.padding(bottom = 3.dp)
                        )
                    }

                    // Toman Valuation
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "ارزش ریالی روز بازار:",
                            fontSize = 11.sp,
                            color = Color(0xFFA5B2CD),
                            maxLines = 1,
                            softWrap = false
                        )
                        Text(
                            text = PersianNumberFormatter.format(1468500000),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.goldPrimary,
                            maxLines = 1,
                            softWrap = false
                        )
                        Text(
                            text = "تومان",
                            fontSize = 11.sp,
                            color = Color(0xFFE2E8F0),
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
        }

        // ==========================================
        // 3. Quick Live Rates Horizontal Strip
        // ==========================================
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                        imageVector = DashTollVector,
                        contentDescription = null,
                        tint = colors.goldPrimary,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = "تابلوی زنده مظنه‌های بازار",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textMain
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF10B981))
                    )
                    Text(
                        text = "بروزرسانی لحظه‌ای",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF059669)
                    )
                }
            }

            val ratesScrollState = rememberScrollState()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(ratesScrollState),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Pill 1: مظنه آبشده ۱۷
                QuickRatePill(
                    title = "مظنه آبشده (مثقال)",
                    price = if (uiState.rates.goldMelt > 0) PersianNumberFormatter.format(uiState.rates.goldMelt) else "۱۸,۵۶۰,۰۰۰",
                    delta = "+۰.۴٪",
                    isPositive = true,
                    unit = "تومان",
                    icon = DashLayersVector,
                    colors = colors
                )

                // Pill 2: طلای ۱۸ عیار
                QuickRatePill(
                    title = "طلای ۱۸ عیار (گرم)",
                    price = if (uiState.rates.gold18 > 0) PersianNumberFormatter.format(uiState.rates.gold18) else "۴,۲۸۵,۰۰۰",
                    delta = "+۰.۸٪",
                    isPositive = true,
                    unit = "تومان",
                    icon = DashTollVector,
                    colors = colors
                )

                // Pill 3: سکه تمام امامی
                QuickRatePill(
                    title = "سکه تمام بهار آزادی",
                    price = if (uiState.rates.coinEmami > 0) PersianNumberFormatter.format(uiState.rates.coinEmami) else "۴۹,۱۰۰,۰۰۰",
                    delta = "+۰.۶٪",
                    isPositive = true,
                    unit = "تومان",
                    icon = DashCoinVector,
                    colors = colors
                )

                // Pill 4: نیم سکه بهار آزادی
                QuickRatePill(
                    title = "نیم سکه بهار آزادی",
                    price = if (uiState.rates.coinHalf > 0) PersianNumberFormatter.format(uiState.rates.coinHalf) else "۲۵,۴۰۰,۰۰۰",
                    delta = "+۰.۳٪",
                    isPositive = true,
                    unit = "تومان",
                    icon = DashCoinVector,
                    colors = colors
                )

                // Pill 5: انس جهانی طلا
                QuickRatePill(
                    title = "انس جهانی طلا",
                    price = if (uiState.rates.ons > 0) PersianNumberFormatter.formatWithCommas(uiState.rates.ons.toLong()) else "۲,۶۸۴.۲۰",
                    delta = "+۱.۱٪",
                    isPositive = true,
                    unit = "دلار / اونس",
                    icon = DashGlobeVector,
                    colors = colors
                )
            }
        }

        // ==========================================
        // 4. Quick Actions & Shortcuts (3x2 Grid)
        // ==========================================
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "دسترسی‌های سریع بنکداری",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textMain
                )
                Text(
                    text = "ابزارهای محاسباتی",
                    fontSize = 10.5.sp,
                    color = colors.textMuted
                )
            }

            // Row 1 of Shortcuts
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                QuickActionButton(
                    title = "محاسبه طلا",
                    icon = DashCalculateVector,
                    onClick = onNavigateCalculator,
                    modifier = Modifier.weight(1f)
                )
                QuickActionButton(
                    title = "صدور فاکتور",
                    icon = DashInvoiceVector,
                    onClick = onNavigateInvoices,
                    modifier = Modifier.weight(1f)
                )
                QuickActionButton(
                    title = "تبدیل عیار",
                    icon = DashBalanceVector,
                    onClick = onNavigateConvert,
                    modifier = Modifier.weight(1f)
                )
            }

            // Row 2 of Shortcuts
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                QuickActionButton(
                    title = "حباب مسکوکات",
                    icon = DashBubbleVector,
                    onClick = onNavigateCoinBubble,
                    modifier = Modifier.weight(1f)
                )
                QuickActionButton(
                    title = "مظنه آبشده",
                    icon = DashCandlestickVector,
                    onClick = onNavigateMelt,
                    modifier = Modifier.weight(1f)
                )
                QuickActionButton(
                    title = "دفترچه کیفی",
                    icon = DashLedgerVector,
                    onClick = onNavigateLedger,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // ==========================================
        // 5. Live Market Chart & Price Trends
        // ==========================================
        LuxuryCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header & Timeframe Selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "روند طلای ۱۸ عیار",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textMain
                        )
                        Text(
                            text = "نرخ پایه اتحادیه طلا و جواهر",
                            fontSize = 10.5.sp,
                            color = colors.textMuted
                        )
                    }

                    // Timeframe Toggle Tabs
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = colors.surfaceElevated,
                        border = BorderStroke(0.6.dp, colors.border.copy(alpha = 0.6f))
                    ) {
                        Row(modifier = Modifier.padding(2.dp)) {
                            listOf("امروز", "هفتگی", "ماهانه").forEachIndexed { index, label ->
                                val isSelected = selectedTimeframe == index
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) colors.surface else Color.Transparent,
                                    shadowElevation = if (isSelected) 1.dp else 0.dp,
                                    modifier = Modifier.clickable { selectedTimeframe = index }
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 10.5.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) colors.goldPrimary else colors.textMuted,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Active Quote & Intraday Delta
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = if (uiState.rates.gold18 > 0) PersianNumberFormatter.format(uiState.rates.gold18) else "۴,۲۸۵,۰۰۰",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            color = colors.textMain
                        )
                        Text(
                            text = "تومان / گرم",
                            fontSize = 11.sp,
                            color = colors.textMuted,
                            modifier = Modifier.padding(bottom = 3.dp)
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFF10B981).copy(alpha = 0.12f),
                        border = BorderStroke(0.6.dp, Color(0xFF10B981).copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(
                                imageVector = DashTrendingUpVector,
                                contentDescription = null,
                                tint = Color(0xFF059669),
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = "+۳۲,۰۰۰ (۰.۸+٪)",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF059669)
                            )
                        }
                    }
                }

                // Interactive Smooth Golden Area Chart
                GoldTrendCanvasChart(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp),
                    goldColor = colors.goldPrimary
                )

                // Time Labels (LTR: 10:00 on the left to 18:00 live on the right)
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        listOf("۱۰:۰۰", "۱۲:۰۰", "۱۴:۰۰", "۱۶:۰۰", "۱۸:۰۰ (زنده)").forEachIndexed { idx, hour ->
                            Text(
                                text = hour,
                                fontSize = 9.5.sp,
                                fontWeight = if (idx == 4) FontWeight.Bold else FontWeight.Normal,
                                color = if (idx == 4) colors.goldPrimary else colors.textMuted
                            )
                        }
                    }
                }
            }
        }

        // ==========================================
        // 6. Recent Transactions & Invoices Stream
        // ==========================================
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
                        imageVector = DashInvoiceVector,
                        contentDescription = null,
                        tint = colors.goldPrimary,
                        modifier = Modifier.size(17.dp)
                    )
                    Text(
                        text = "آخرین فاکتورها و تبادلات",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textMain
                    )
                }

                Text(
                    text = "مشاهده همه (${uiState.savedInvoices.size.coerceAtLeast(3)})",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.goldPrimary,
                    modifier = Modifier.clickable { onNavigateInvoices() }
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Item 1: Cartier Bracelet
                TransactionRowItem(
                    title = "دستبند کارتیه ۱۸ عیار",
                    subtitle = "حاج محمد کاظمی • ۱۲.۵۰۰ گرم",
                    amount = "۲۴۱,۵۰۰,۰۰۰",
                    unit = "تومان",
                    statusLabel = "تسویه کامل",
                    statusColor = Color(0xFF10B981),
                    icon = DashDiamondVector,
                    colors = colors,
                    onClick = onNavigateInvoices
                )

                // Item 2: Coin Purchase with Balance
                TransactionRowItem(
                    title = "سکه تمام بهار آزادی",
                    subtitle = "خانم سارا رادمنش • فاکتور #۱۴۰۳۹",
                    amount = "۴۲,۳۰۰,۰۰۰",
                    unit = "تومان",
                    statusLabel = "مانده ۸.۴ م",
                    statusColor = Color(0xFFD97706),
                    icon = DashCoinVector,
                    colors = colors,
                    onClick = onNavigateInvoices
                )

                // Item 3: Workshop Bullion Ingot Receipt
                TransactionRowItem(
                    title = "تحویل شمش آبشده",
                    subtitle = "کارگاه زرگری کمالی • رسید انبار #۷۳",
                    amount = "۵۰.۰۰۰",
                    unit = "گرم ۷۵۰",
                    statusLabel = "ورود به گاوصندوق",
                    statusColor = colors.goldPrimary,
                    icon = DashIngotVector,
                    colors = colors,
                    onClick = onNavigateInvoices
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
private fun QuickRatePill(
    title: String,
    price: String,
    delta: String,
    isPositive: Boolean,
    unit: String,
    icon: ImageVector,
    colors: com.goldex.companion.ui.theme.GoldExAppColors
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = colors.surface,
        border = BorderStroke(0.6.dp, colors.border),
        shadowElevation = 1.dp,
        modifier = Modifier.width(165.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 10.sp,
                    color = colors.textMuted,
                    fontWeight = FontWeight.Medium
                )
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(colors.surfaceElevated),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = colors.goldPrimary,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = price,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textMain
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = unit, fontSize = 9.5.sp, color = colors.textMuted)
                    Text(
                        text = delta,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isPositive) Color(0xFF10B981) else Color(0xFFEF4444)
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickActionButton(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalGoldExColors.current
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = colors.surface,
        border = BorderStroke(0.6.dp, colors.goldBorder),
        shadowElevation = if (colors.isDark) 0.dp else 1.dp,
        modifier = modifier.clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(colors.surfaceElevated),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = colors.goldPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.textMain,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun TransactionRowItem(
    title: String,
    subtitle: String,
    amount: String,
    unit: String,
    statusLabel: String,
    statusColor: Color,
    icon: ImageVector,
    colors: com.goldex.companion.ui.theme.GoldExAppColors,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = colors.surface,
        border = BorderStroke(0.6.dp, colors.goldBorder),
        shadowElevation = if (colors.isDark) 0.dp else 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 11.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
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
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textMain
                        )
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = statusColor.copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = statusLabel,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = statusColor,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp)
                            )
                        }
                    }

                    Text(
                        text = subtitle,
                        fontSize = 10.sp,
                        color = colors.textMuted
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                Text(
                    text = amount,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textMain
                )
                Text(
                    text = unit,
                    fontSize = 9.5.sp,
                    color = colors.textMuted
                )
            }
        }
    }
}

/**
 * Custom Compose Canvas for rendering the smooth golden trend area chart.
 */
@Composable
private fun GoldTrendCanvasChart(
    modifier: Modifier = Modifier,
    goldColor: Color
) {
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height

        val path = Path().apply {
            moveTo(0f, height * 0.75f)
            cubicTo(
                width * 0.15f, height * 0.65f,
                width * 0.25f, height * 0.85f,
                width * 0.35f, height * 0.55f
            )
            cubicTo(
                width * 0.45f, height * 0.35f,
                width * 0.55f, height * 0.50f,
                width * 0.65f, height * 0.30f
            )
            cubicTo(
                width * 0.75f, height * 0.12f,
                width * 0.88f, height * 0.35f,
                width, height * 0.18f
            )
        }

        val fillPath = Path().apply {
            addPath(path)
            lineTo(width, height)
            lineTo(0f, height)
            close()
        }

        // Draw Area Gradient Fill
        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(
                    goldColor.copy(alpha = 0.35f),
                    goldColor.copy(alpha = 0.12f),
                    Color.Transparent
                )
            )
        )

        // Draw Stroke Line
        drawPath(
            path = path,
            color = goldColor,
            style = Stroke(
                width = 3.dp.toPx(),
                cap = StrokeCap.Round
            )
        )

        // Draw Peak Dot
        drawCircle(
            color = Color.White,
            radius = 4.dp.toPx(),
            center = Offset(width * 0.65f, height * 0.30f)
        )
        drawCircle(
            color = goldColor,
            radius = 2.5.dp.toPx(),
            center = Offset(width * 0.65f, height * 0.30f)
        )

        // Draw Live End Node
        drawCircle(
            color = goldColor,
            radius = 5.dp.toPx(),
            center = Offset(width, height * 0.18f)
        )
        drawCircle(
            color = goldColor.copy(alpha = 0.3f),
            radius = 8.5.dp.toPx(),
            center = Offset(width, height * 0.18f)
        )
    }
}
