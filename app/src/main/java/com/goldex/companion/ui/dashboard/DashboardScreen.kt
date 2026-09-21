package com.goldex.companion.ui.dashboard

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import com.goldex.companion.ui.theme.LuxuryMotion
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import com.goldex.companion.ui.theme.ButtonShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.goldex.companion.ui.hub.HubShowcase
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.goldex.companion.data.license.LicenseInfo
import com.goldex.companion.data.license.LicenseStatus
import com.goldex.companion.model.InvoiceListItem
import com.goldex.companion.model.InvoiceStatus
import com.goldex.companion.model.MarketCandle
import com.goldex.companion.model.MarketHistoryConverter
import com.goldex.companion.model.PersianNumberFormatter
import com.goldex.companion.model.TimeHorizon
import com.goldex.companion.model.TrendChartData
import com.goldex.companion.ui.components.LuxuryCard
import com.goldex.companion.ui.components.LuxurySegmentedControl
import com.goldex.companion.ui.components.AnimatedNumberText
import com.goldex.companion.ui.components.AnimatedPriceText
import com.goldex.companion.ui.theme.LocalGoldExColors
import com.goldex.companion.ui.theme.VazirmatnFamily

/**
 * DashboardScreen: The Sovereign Goldsmith Executive Dashboard.
 *
 * Faithfully implemented after Google Stitch Screen ID: 5684da48f26e4ceda789b52c96c509a4
 * Adheres strictly to Persian Sovereign Aurum design tokens, RTL layout, and Vazirmatn typography.
 */
@Composable
fun DashboardScreen(
    uiState: DashboardUiState,
    onNavigateCalculator: () -> Unit,
    onNavigateInvoices: () -> Unit,
    onNavigateConvert: () -> Unit,
    onNavigateCoinBubble: () -> Unit,
    onNavigateMelt: () -> Unit,
    onNavigateLedger: () -> Unit,
    onNavigateInventory: () -> Unit = {},
    onOpenLicenseActivation: () -> Unit = {}
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
        // 1.5 Smart License & Trial Alert Banner
        // ==========================================
        DashboardLicenseAlertBanner(
            licenseInfo = uiState.licenseInfo,
            onOpenLicenseActivation = onOpenLicenseActivation
        )

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

                    // Status Pill
                    if (uiState.totalInventoryWeight18k > 0.0) {
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
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF10B981))
                                )
                                Text(
                                    text = "موجودی انبار فعال",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF6FFBBE)
                                )
                            }
                        }
                    }
                }

                // Main Weight Balance
                val weightFormatted = PersianNumberFormatter.formatWeight(uiState.totalInventoryWeight18k)
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
                            text = PersianNumberFormatter.format(uiState.totalInventoryValuationTomans),
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
                    price = if (uiState.rates.goldMelt > 0) PersianNumberFormatter.format(uiState.rates.goldMelt) else "—",
                    delta = "",
                    isPositive = true,
                    unit = "تومان",
                    icon = DashLayersVector,
                    colors = colors
                )

                // Pill 2: طلای ۱۸ عیار
                QuickRatePill(
                    title = "طلای ۱۸ عیار (گرم)",
                    price = if (uiState.rates.gold18 > 0) PersianNumberFormatter.format(uiState.rates.gold18) else "—",
                    delta = "",
                    isPositive = true,
                    unit = "تومان",
                    icon = DashTollVector,
                    colors = colors
                )

                // Pill 3: سکه تمام امامی
                QuickRatePill(
                    title = "سکه تمام بهار آزادی",
                    price = if (uiState.rates.coinEmami > 0) PersianNumberFormatter.format(uiState.rates.coinEmami) else "—",
                    delta = "",
                    isPositive = true,
                    unit = "تومان",
                    icon = DashCoinVector,
                    colors = colors
                )

                // Pill 4: نیم سکه بهار آزادی
                QuickRatePill(
                    title = "نیم سکه بهار آزادی",
                    price = if (uiState.rates.coinHalf > 0) PersianNumberFormatter.format(uiState.rates.coinHalf) else "—",
                    delta = "",
                    isPositive = true,
                    unit = "تومان",
                    icon = DashCoinVector,
                    colors = colors
                )

                // Pill 5: انس جهانی طلا
                QuickRatePill(
                    title = "انس جهانی طلا",
                    price = if (uiState.rates.ons > 0) PersianNumberFormatter.formatWithCommas(uiState.rates.ons.toLong()) else "—",
                    delta = "",
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
                    title = "مدیریت انبار",
                    icon = HubShowcase,
                    onClick = onNavigateInventory,
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
                    LuxurySegmentedControl(
                        items = listOf(0, 1, 2),
                        selectedItem = selectedTimeframe,
                        onItemSelected = { selectedTimeframe = it },
                        label = { when(it) { 0 -> "امروز" 1 -> "هفتگی" else -> "ماهانه" } },
                        modifier = Modifier.width(184.dp),
                        height = 32.dp,
                        fontSize = 10.5.sp
                    )
                }

                val activeHorizon = when (selectedTimeframe) {
                    0 -> TimeHorizon.TODAY
                    1 -> TimeHorizon.ONE_WEEK
                    else -> TimeHorizon.ONE_MONTH
                }
                val activeChart = uiState.gold18Charts[activeHorizon]
                val activeChartAvailable = activeChart?.isAvailable == true && activeChart.points.isNotEmpty()
                val activeRate = activeChart?.candles?.lastOrNull()?.close?.takeIf { it > 0L }
                    ?: uiState.rates.gold18.takeIf { it > 0L }
                    ?: 4_285_000L

                // Keep the quote outside chart AnimatedContent so its digits animate independently.
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        AnimatedPriceText(
                            amount = activeRate,
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

                    if (activeChartAvailable && activeChart != null) {
                        val isPositive = activeChart.isPositive
                        val deltaBg = if (isPositive) Color(0xFF10B981).copy(alpha = 0.12f) else Color(0xFFEF4444).copy(alpha = 0.12f)
                        val deltaBorder = if (isPositive) Color(0xFF10B981).copy(alpha = 0.3f) else Color(0xFFEF4444).copy(alpha = 0.3f)
                        val deltaColor = if (isPositive) Color(0xFF059669) else Color(0xFFEF4444)
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = deltaBg,
                            border = BorderStroke(0.6.dp, deltaBorder)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(
                                    imageVector = DashTrendingUpVector,
                                    contentDescription = null,
                                    tint = deltaColor,
                                    modifier = Modifier.size(12.dp).then(if (!isPositive) Modifier.rotate(180f) else Modifier)
                                )
                                AnimatedNumberText(
                                    text = PersianNumberFormatter.formatDelta(
                                        activeChart.fluctuationAmount,
                                        activeChart.fluctuationPercent
                                    ),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = deltaColor
                                )
                            }
                        }
                    }
                }

                AnimatedContent(
                    targetState = selectedTimeframe,
                    transitionSpec = {
                        (fadeIn(animationSpec = tween(240, easing = LuxuryMotion.StandardEasing)) +
                            scaleIn(initialScale = 0.96f, animationSpec = tween(240, easing = LuxuryMotion.StandardEasing)))
                            .togetherWith(
                                fadeOut(animationSpec = tween(180, easing = LuxuryMotion.AccelerationEasing)) +
                                    scaleOut(targetScale = 0.98f, animationSpec = tween(180, easing = LuxuryMotion.AccelerationEasing))
                            )
                    },
                    label = "dashboardChartHorizonTransition"
                ) { targetTimeframe ->
                    val horizon = when (targetTimeframe) {
                        0 -> TimeHorizon.TODAY
                        1 -> TimeHorizon.ONE_WEEK
                        else -> TimeHorizon.ONE_MONTH
                    }
                    val chart = uiState.gold18Charts[horizon]
                    val isAvailable = chart?.isAvailable == true && chart.points.isNotEmpty()
                    if (!isAvailable) {
                        Box(
                            modifier = Modifier.fillMaxWidth().height(130.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = DashCandlestickVector,
                                    contentDescription = null,
                                    tint = colors.textMuted.copy(alpha = 0.4f),
                                    modifier = Modifier.size(28.dp)
                                )
                                Text(
                                    text = if (horizon == TimeHorizon.TODAY) "داده‌های نوسان امروز هنوز در دسترس نیست"
                                    else "داده‌های نمودار برای این بازه در دسترس نیست",
                                    fontSize = 11.5.sp,
                                    color = colors.textMuted
                                )
                            }
                        }
                    } else {
                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                            GoldTrendCanvasChart(
                                points = chart!!.points,
                                candles = chart.candles,
                                currentPrice = chart.candles.lastOrNull()?.close ?: activeRate,
                                currencyUnit = "تومان",
                                modifier = Modifier.fillMaxWidth().height(130.dp),
                                goldColor = colors.goldPrimary
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
                    text = "مشاهده همه (${PersianNumberFormatter.toPersianDigits(uiState.savedInvoiceCount.toString())})",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.goldPrimary,
                    modifier = Modifier.clickable { onNavigateInvoices() }
                )
            }

            if (uiState.recentInvoices.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = colors.surface,
                    border = BorderStroke(0.6.dp, colors.border.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp, horizontal = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "هنوز فاکتوری صادر نشده است",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textMuted
                        )
                        Text(
                            text = "با ثبت فاکتور تهاتری جدید، گزارش تراکنش‌ها در اینجا نمایش داده می‌شود.",
                            fontSize = 10.5.sp,
                            color = colors.textMuted.copy(alpha = 0.8f),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    uiState.recentInvoices.forEach { invoice ->
                        val isSettled = invoice.status == InvoiceStatus.SETTLED
                        val statusColor = if (isSettled) Color(0xFF10B981) else Color(0xFFD97706)

                        val compactStatus = when {
                            isSettled -> "تسویه کامل"
                            invoice.statusDetail.contains("تهاتر") -> "تهاتر"
                            invoice.statusDetail.contains("مانده") -> "مانده‌دار"
                            invoice.statusDetail.contains("نسیه") -> "نسیه"
                            else -> "در انتظار پرداخت"
                        }

                        val itemsSummaryShort = if (invoice.itemsSummary.isBlank()) {
                            "اقلام طلا"
                        } else {
                            val parts = invoice.itemsSummary.split("+").map { it.trim() }
                            if (parts.size > 1) {
                                "${parts[0]} (+${PersianNumberFormatter.toPersianDigits((parts.size - 1).toString())} قلم)"
                            } else {
                                parts[0]
                            }
                        }

                        TransactionRowItem(
                            title = invoice.customerName.ifBlank { "فاکتور #${invoice.invoiceNumber}" },
                            subtitle = "فاکتور #${invoice.invoiceNumber} • $itemsSummaryShort",
                            amount = PersianNumberFormatter.formatPrice(invoice.finalAmount.toDouble()),
                            unit = "تومان",
                            statusLabel = compactStatus,
                            statusColor = statusColor,
                            icon = DashInvoiceVector,
                            colors = colors,
                            onClick = onNavigateInvoices
                        )
                    }
                }
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
                    if (delta.isNotBlank()) {
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
        shape = ButtonShape,
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
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Icon + Title & Subtitle (fills available middle space)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
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

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        text = title,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textMain,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontFamily = VazirmatnFamily
                    )
                    Text(
                        text = subtitle,
                        fontSize = 10.5.sp,
                        color = colors.textMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontFamily = VazirmatnFamily
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Right: Amount + Status Chip
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = amount,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textMain,
                        maxLines = 1,
                        fontFamily = VazirmatnFamily
                    )
                    Text(
                        text = unit,
                        fontSize = 10.sp,
                        color = colors.textMuted,
                        fontFamily = VazirmatnFamily
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = statusColor.copy(alpha = 0.12f),
                    border = BorderStroke(0.5.dp, statusColor.copy(alpha = 0.35f))
                ) {
                    Text(
                        text = statusLabel,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = statusColor,
                        maxLines = 1,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        fontFamily = VazirmatnFamily
                    )
                }
            }
        }
    }
}

/**
 * Custom Compose Canvas for rendering the smooth golden trend area chart.
 * Supports interactive touch/drag with vertical dashed guideline and tooltip.
 */
@Composable
private fun GoldTrendCanvasChart(
    points: List<Pair<Float, Float>>,
    candles: List<MarketCandle> = emptyList(),
    currentPrice: Long = 0L,
    currencyUnit: String = "تومان",
    modifier: Modifier = Modifier,
    goldColor: Color
) {
    var selectedIndex by remember(points) { mutableStateOf<Int?>(points.indices.lastOrNull()) }

    Box(
        modifier = modifier
            .pointerInput(points) {
                detectTapGestures(
                    onTap = { offset ->
                        if (points.isNotEmpty()) {
                            val w = size.width.toFloat()
                            val closestIdx = points.indices.minByOrNull { i ->
                                val px = points[i].first * w
                                kotlin.math.abs(px - offset.x)
                            }
                            if (closestIdx != null) {
                                selectedIndex = closestIdx
                            }
                        }
                    }
                )
            }
            .pointerInput(points) {
                detectDragGestures(
                    onDragStart = { offset ->
                        if (points.isNotEmpty()) {
                            val w = size.width.toFloat()
                            selectedIndex = points.indices.minByOrNull { i ->
                                val px = points[i].first * w
                                kotlin.math.abs(px - offset.x)
                            }
                        }
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        if (points.isNotEmpty()) {
                            val w = size.width.toFloat()
                            selectedIndex = points.indices.minByOrNull { i ->
                                val px = points[i].first * w
                                kotlin.math.abs(px - change.position.x)
                            }
                        }
                    }
                )
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            if (points.isEmpty()) return@Canvas

            val canvasPoints = points.map { (xNorm, yNorm) ->
                Offset(
                    x = xNorm * width,
                    y = (1f - yNorm) * (height - 24.dp.toPx()) + 12.dp.toPx()
                )
            }

            // Catmull-Rom smooth cubic spline
            val linePath = Path().apply {
                moveTo(canvasPoints.first().x, canvasPoints.first().y)
                for (i in 0 until canvasPoints.size - 1) {
                    val p0 = canvasPoints[i]
                    val p1 = canvasPoints[i + 1]
                    val prev = if (i > 0) canvasPoints[i - 1] else p0
                    val next = if (i + 2 < canvasPoints.size) canvasPoints[i + 2] else p1
                    val c1x = p0.x + (p1.x - prev.x) / 6f
                    val c1y = p0.y + (p1.y - prev.y) / 6f
                    val c2x = p1.x - (next.x - p0.x) / 6f
                    val c2y = p1.y - (next.y - p0.y) / 6f
                    cubicTo(c1x, c1y, c2x, c2y, p1.x, p1.y)
                }
            }

            val fillPath = Path().apply {
                addPath(linePath)
                lineTo(canvasPoints.last().x, height)
                lineTo(canvasPoints.first().x, height)
                close()
            }

            // Draw Area Gradient Fill
            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        goldColor.copy(alpha = 0.35f),
                        goldColor.copy(alpha = 0.10f),
                        Color.Transparent
                    )
                )
            )

            // Draw Stroke Line
            drawPath(
                path = linePath,
                color = goldColor,
                style = Stroke(
                    width = 2.5.dp.toPx(),
                    cap = StrokeCap.Round
                )
            )

            // Draw Live End Node
            val endPoint = canvasPoints.last()
            drawCircle(
                color = goldColor,
                radius = 4.5.dp.toPx(),
                center = endPoint
            )
            drawCircle(
                color = goldColor.copy(alpha = 0.3f),
                radius = 8.dp.toPx(),
                center = endPoint
            )

            // Active Touch/Drag Indicator Line & On-Curve Point
            if (selectedIndex != null && selectedIndex in canvasPoints.indices) {
                val selPoint = canvasPoints[selectedIndex!!]

                // Vertical Dashed Guideline
                drawLine(
                    color = goldColor.copy(alpha = 0.6f),
                    start = Offset(selPoint.x, 6.dp.toPx()),
                    end = Offset(selPoint.x, height - 6.dp.toPx()),
                    strokeWidth = 1.2.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                )

                // Glowing indicator circle on curve
                drawCircle(
                    color = goldColor.copy(alpha = 0.35f),
                    radius = 8.dp.toPx(),
                    center = selPoint
                )
                drawCircle(
                    color = goldColor,
                    radius = 4.5.dp.toPx(),
                    center = selPoint
                )
                drawCircle(
                    color = Color.White,
                    radius = 2.5.dp.toPx(),
                    center = selPoint
                )
            }
        }

        // Floating Interactive Tooltip
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            if (selectedIndex != null && selectedIndex in points.indices) {
                val selIdx = selectedIndex!!
                val normP = points[selIdx]
                val selCandle = candles.getOrNull(selIdx)
                val price = selCandle?.close ?: currentPrice
                val dateOrTime = selCandle?.dateShamsi?.takeIf { it.isNotBlank() } ?: ""

                val ptX = maxWidth * normP.first
                val ptY = (maxHeight - 24.dp) * (1f - normP.second) + 12.dp

                val tooltipW = 135.dp
                val tooltipH = 44.dp

                val targetX = ptX - (tooltipW / 2)
                val clampedX = targetX.coerceIn(4.dp, (maxWidth - tooltipW - 4.dp).coerceAtLeast(4.dp))

                val targetY = ptY - tooltipH - 6.dp
                val clampedY = if (targetY >= 2.dp) {
                    targetY
                } else {
                    (ptY + 8.dp).coerceAtMost(maxHeight - tooltipH - 2.dp)
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF24211A),
                    border = BorderStroke(0.8.dp, goldColor),
                    shadowElevation = 6.dp,
                    modifier = Modifier.offset(x = clampedX, y = clampedY)
                ) {
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                        Column(
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(5.dp)
                                        .clip(CircleShape)
                                        .background(goldColor)
                                )
                                Text(
                                    text = "${PersianNumberFormatter.format(price)} $currencyUnit",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFFFFE088)
                                )
                            }
                            if (dateOrTime.isNotBlank()) {
                                Text(
                                    text = PersianNumberFormatter.toPersianDigits(dateOrTime),
                                    fontSize = 9.sp,
                                    maxLines = 1,
                                    color = Color(0xFFC7B299)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DashboardLicenseAlertBanner(
    licenseInfo: LicenseInfo,
    onOpenLicenseActivation: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalGoldExColors.current

    val shouldShow = when (licenseInfo.status) {
        LicenseStatus.NONE -> true
        LicenseStatus.TRIAL_ACTIVE -> licenseInfo.remainingDays <= 3
        LicenseStatus.TRIAL_EXPIRED, LicenseStatus.REVOKED -> true
        LicenseStatus.LIFETIME -> false
    }

    if (!shouldShow) return

    val isDark = colors.isDark

    val config = when (licenseInfo.status) {
        LicenseStatus.TRIAL_EXPIRED, LicenseStatus.REVOKED -> {
            AlertBannerConfig(
                bg = if (isDark) listOf(Color(0xFF2E1316), Color(0xFF1B0B0D)) else listOf(Color(0xFFFEE2E2), Color(0xFFFECACA)),
                border = if (isDark) Color(0xFFE53935).copy(alpha = 0.55f) else Color(0xFFEF4444),
                accent = if (isDark) Color(0xFFEF5350) else Color(0xFFDC2626),
                icon = DashAlertLock,
                title = "مهلت تست به پایان رسیده است",
                subtitle = "جهت ادامه کار، کد اشتراک را ثبت کنید",
                buttonText = "ثبت کد"
            )
        }
        LicenseStatus.TRIAL_ACTIVE -> {
            val daysText = if (licenseInfo.remainingDays <= 0) {
                "کمتر از ۱ روز"
            } else {
                "${PersianNumberFormatter.toPersianDigits(licenseInfo.remainingDays)} روز"
            }
            AlertBannerConfig(
                bg = if (isDark) listOf(Color(0xFF2C1E0A), Color(0xFF1A1206)) else listOf(Color(0xFFFEF3C7), Color(0xFFFDE68A)),
                border = if (isDark) Color(0xFFF59E0B).copy(alpha = 0.55f) else Color(0xFFF59E0B),
                accent = if (isDark) Color(0xFFFBBF24) else Color(0xFFD97706),
                icon = DashAlertWarning,
                title = "تنها $daysText تا پایان مهلت تست",
                subtitle = "جهت جلوگیری از وقفه، اشتراک را دائمی کنید",
                buttonText = "ارتقا"
            )
        }
        LicenseStatus.NONE -> {
            AlertBannerConfig(
                bg = if (isDark) listOf(Color(0xFF1D1F2C), Color(0xFF141520)) else listOf(Color(0xFFFFFBEB), Color(0xFFFEF3C7)),
                border = if (isDark) colors.goldBorder.copy(alpha = 0.6f) else colors.goldBorder,
                accent = if (isDark) colors.goldPrimary else colors.goldSecondary,
                icon = DashAlertGift,
                title = "مهلت تست ۱۴ روزه رایگان قیراط",
                subtitle = "جهت دسترسی کامل به صدور فاکتور و دفاتر",
                buttonText = "فعال‌سازی"
            )
        }
        else -> return
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onOpenLicenseActivation() },
        shape = RoundedCornerShape(14.dp),
        color = Color.Transparent,
        border = BorderStroke(1.dp, config.border),
        shadowElevation = if (isDark) 0.dp else 1.5.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.horizontalGradient(config.bg))
                .padding(horizontal = 12.dp, vertical = 9.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                // Icon Badge
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(config.accent.copy(alpha = if (isDark) 0.18f else 0.22f))
                        .border(0.8.dp, config.accent.copy(alpha = 0.4f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = config.icon,
                        contentDescription = null,
                        tint = config.accent,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Text Details (Clean and single-line to prevent layout wrapping)
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = config.title,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = if (isDark) Color.White else Color(0xFF1F2937)
                    )
                    Text(
                        text = config.subtitle,
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = if (isDark) Color(0xFFD1D5DB) else Color(0xFF4B5563),
                        lineHeight = 14.sp
                    )
                }

                // Call to Action Chip (Compact)
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = config.accent.copy(alpha = if (isDark) 0.18f else 0.25f),
                    border = BorderStroke(1.dp, config.accent.copy(alpha = 0.55f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = config.buttonText,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = config.accent,
                            maxLines = 1
                        )
                        Icon(
                            imageVector = DashChevronLeft,
                            contentDescription = null,
                            tint = config.accent,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }
        }
    }
}

private data class AlertBannerConfig(
    val bg: List<Color>,
    val border: Color,
    val accent: Color,
    val icon: ImageVector,
    val title: String,
    val subtitle: String,
    val buttonText: String
)
