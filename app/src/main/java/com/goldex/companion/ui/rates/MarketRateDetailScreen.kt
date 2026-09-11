package com.goldex.companion.ui.rates

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.goldex.companion.model.*
import com.goldex.companion.ui.calculator.CalcPostAdd
import com.goldex.companion.ui.calculator.CalcReceiptLong
import com.goldex.companion.ui.components.QiratoToast
import com.goldex.companion.ui.customers.LedgerArrowPayVector
import com.goldex.companion.ui.customers.LedgerArrowReceiveVector
import com.goldex.companion.ui.dashboard.*
import com.goldex.companion.ui.hub.HubArrowRight
import com.goldex.companion.ui.theme.LocalGoldExColors
import java.util.Locale

/**
 * MarketRateDetailScreen: Comprehensive Rate Detail & Trend Chart Screen
 *
 * Implemented based on Google Stitch Screen ID: 57d121df61a34215ba36be0989160e62
 * Supports all market rate items (18K, 24K, Melt, Coins, Ounce, Currency)
 * with interactive time horizon trend charts, technical metrics, and quick goldsmith actions.
 */
@Composable
fun MarketRateDetailScreen(
    state: MarketRateDetailState,
    onBack: () -> Unit,
    onAddToInvoice: (Long, Karat) -> Unit,
    onSetPriceAlert: (MarketRateItemType, Long) -> Unit,
    onViewAllTransactions: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalGoldExColors.current
    val context = LocalContext.current
    var selectedHorizon by remember { mutableStateOf(TimeHorizon.TODAY) }
    var isAlertActive by remember { mutableStateOf(false) }

    // Pulsing animation for live indicators
    val infiniteTransition = rememberInfiniteTransition(label = "pulseAnim")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    val currentChart = state.chartDataByHorizon[selectedHorizon] ?: state.chartDataByHorizon.values.first()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // ==========================================
        // 1. Sub-header Breadcrumb & Actions Bar
        // ==========================================
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Back Button
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = colors.surface,
                    border = BorderStroke(0.6.dp, colors.border),
                    shadowElevation = 1.dp,
                    modifier = Modifier
                        .size(40.dp)
                        .clickable { onBack() }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = HubArrowRight,
                            contentDescription = "بازگشت",
                            tint = colors.textMain,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Title and Status
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = state.title,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textMain
                        )
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF10B981).copy(alpha = pulseAlpha))
                        )
                    }
                    Text(
                        text = state.subtitle,
                        fontSize = 10.5.sp,
                        color = colors.textSecondary
                    )
                }
            }

            // Quick Actions: Alert & Share
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Alert Action Button
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isAlertActive) colors.goldPrimary.copy(alpha = 0.18f) else colors.surface,
                    border = BorderStroke(
                        0.6.dp,
                        if (isAlertActive) colors.goldPrimary else colors.border
                    ),
                    shadowElevation = 1.dp,
                    modifier = Modifier
                        .size(40.dp)
                        .clickable {
                            isAlertActive = !isAlertActive
                            val msg = if (isAlertActive) {
                                "هشدار قیمت برای ${state.title} فعال شد"
                            } else {
                                "هشدار نوسان غیرفعال شد"
                            }
                            QiratoToast.show(context, msg)
                            onSetPriceAlert(state.type, state.currentPrice)
                        }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isAlertActive) IconBellActive else IconBellNone,
                            contentDescription = "تنظیم هشدار",
                            tint = if (isAlertActive) colors.goldPrimary else colors.textSecondary,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }

                // Share Action Button
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = colors.surface,
                    border = BorderStroke(0.6.dp, colors.border),
                    shadowElevation = 1.dp,
                    modifier = Modifier
                        .size(40.dp)
                        .clickable {
                            val shareText = buildString {
                                appendLine("قیمت لحظه‌ای ${state.title}:")
                                appendLine("نرخ: ${PersianNumberFormatter.format(state.currentPrice)} ${state.currencyUnit}")
                                appendLine("تغییر روز: ${if (state.isPositive) "+" else ""}${PersianNumberFormatter.format(state.changeAmount)} (${PersianNumberFormatter.toPersianDigits(String.format(Locale.US, "%.2f", state.changePercent))}٪)")
                                appendLine("کف: ${PersianNumberFormatter.format(state.dayLow)} | سقف: ${PersianNumberFormatter.format(state.dayHigh)}")
                                appendLine("برگرفته از اپلیکیشن تخصصی قیراط")
                            }
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("مظنه طلا", shareText)
                            clipboard.setPrimaryClip(clip)
                            QiratoToast.show(context, "مظنه ${state.title} در حافظه کپی شد")
                        }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "اشتراک‌گذاری",
                            tint = colors.textSecondary,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }
            }
        }

        // ==========================================
        // 2. Main Luxury Bullion Card (Persian Sovereign Aurum)
        // ==========================================
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(
                    Brush.linearGradient(
                        listOf(
                            Color(0xFF1B1914),
                            Color(0xFF24211A),
                            Color(0xFF12110D)
                        )
                    )
                )
                .border(
                    width = 0.8.dp,
                    brush = Brush.linearGradient(
                        listOf(
                            Color(0xFFD4AF37).copy(alpha = 0.65f),
                            Color(0xFFB8860B).copy(alpha = 0.35f),
                            Color(0xFFD4AF37).copy(alpha = 0.20f)
                        )
                    ),
                    shape = RoundedCornerShape(22.dp)
                )
                .padding(18.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                // Top Status Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFFD4AF37).copy(alpha = 0.20f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF4EDEA3))
                                )
                                Text(
                                    text = "بازار زنده",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFFE088)
                                )
                            }
                        }

                        Text(
                            text = "بروزرسانی: لحظه‌ای",
                            fontSize = 10.5.sp,
                            color = Color.White.copy(alpha = 0.55f)
                        )
                    }

                    // Official badge
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF006C49).copy(alpha = 0.25f),
                        border = BorderStroke(0.6.dp, Color(0xFF4EDEA3).copy(alpha = 0.35f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = DashVerifiedVector,
                                contentDescription = null,
                                tint = Color(0xFF6FFBBE),
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = state.categoryBadge,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF6FFBBE)
                            )
                        }
                    }
                }

                // Current Price Display
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        text = "نرخ فعلی معامله",
                        fontSize = 11.5.sp,
                        color = Color.White.copy(alpha = 0.70f)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Text(
                                text = PersianNumberFormatter.format(state.currentPrice),
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White,
                                modifier = Modifier.shadow(
                                    elevation = 8.dp,
                                    shape = RoundedCornerShape(4.dp),
                                    ambientColor = Color(0xFFD4AF37),
                                    spotColor = Color(0xFFD4AF37)
                                )
                            )
                            Text(
                                text = state.currencyUnit,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFFFFE088),
                                modifier = Modifier.padding(bottom = 2.dp)
                            )
                        }

                        // Delta Badge
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (state.isPositive) Color(0xFF10B981).copy(alpha = 0.20f) else Color(0xFFEF4444).copy(alpha = 0.20f),
                            border = BorderStroke(
                                0.6.dp,
                                if (state.isPositive) Color(0xFF10B981).copy(alpha = 0.40f) else Color(0xFFEF4444).copy(alpha = 0.40f)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = if (state.isPositive) DashTrendingUpVector else DashTrendingDownVector,
                                    contentDescription = null,
                                    tint = if (state.isPositive) Color(0xFF6EE7B7) else Color(0xFFFCA5A5),
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "${if (state.isPositive) "+" else ""}${PersianNumberFormatter.format(state.changeAmount)}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (state.isPositive) Color(0xFF6EE7B7) else Color(0xFFFCA5A5)
                                )
                                Text(
                                    text = "(${if (state.isPositive) "+" else ""}${PersianNumberFormatter.toPersianDigits(String.format(Locale.US, "%.2f", state.changePercent))}٪)",
                                    fontSize = 10.sp,
                                    color = if (state.isPositive) Color(0xFF6EE7B7).copy(alpha = 0.8f) else Color(0xFFFCA5A5).copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                }

                // 4 Balanced Metric Tiles (2x2 Grid)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Tile 1: کف قیمت روز
                    MetricTile(
                        title = "کف قیمت روز",
                        value = "${PersianNumberFormatter.format(state.dayLow)} ${state.currencyUnit}",
                        icon = IconArrowDown,
                        iconColor = Color(0xFFF87171),
                        modifier = Modifier.weight(1f)
                    )

                    // Tile 2: سقف قیمت روز
                    MetricTile(
                        title = "سقف قیمت روز",
                        value = "${PersianNumberFormatter.format(state.dayHigh)} ${state.currencyUnit}",
                        icon = IconArrowUp,
                        iconColor = Color(0xFF4ADE80),
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Tile 3: نرخ بازگشایی
                    MetricTile(
                        title = "نرخ بازگشایی",
                        value = "${PersianNumberFormatter.format(state.openPrice)} ${state.currencyUnit}",
                        icon = DashScheduleVector,
                        iconColor = Color(0xFFFFE088),
                        modifier = Modifier.weight(1f)
                    )

                    // Tile 4: حباب یا اسپرد
                    MetricTile(
                        title = state.bubbleOrSpreadLabel,
                        value = "${if (state.bubbleOrSpread > 0) "+" else ""}${PersianNumberFormatter.format(state.bubbleOrSpread)} ${state.currencyUnit}",
                        icon = IconSwapVert,
                        iconColor = Color(0xFF38BDF8),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // ==========================================
        // 3. Interactive Chart Section (روند نوسان بها)
        // ==========================================
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = colors.surface,
            border = BorderStroke(0.6.dp, colors.goldBorder),
            shadowElevation = if (colors.isDark) 0.dp else 1.5.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Chart Header
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
                            imageVector = IconShowChart,
                            contentDescription = null,
                            tint = colors.goldPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "روند نوسان بها",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textMain
                        )
                    }

                    // Fluctuation Pill
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF10B981).copy(alpha = 0.12f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "دامنه نوسان:",
                                fontSize = 10.5.sp,
                                color = colors.textSecondary
                            )
                            Text(
                                text = PersianNumberFormatter.toPersianDigits(currentChart.fluctuationRangeText),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF047857)
                            )
                        }
                    }
                }

                // 5 Time Horizon Filter Tabs
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.surfaceElevated)
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    TimeHorizon.values().forEach { horizon ->
                        val isSelected = selectedHorizon == horizon
                        Surface(
                            shape = RoundedCornerShape(9.dp),
                            color = if (isSelected) colors.surface else Color.Transparent,
                            shadowElevation = if (isSelected) 1.dp else 0.dp,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedHorizon = horizon }
                        ) {
                            Box(
                                modifier = Modifier.padding(vertical = 7.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = horizon.labelFa,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) colors.goldPrimary else colors.textSecondary
                                )
                            }
                        }
                    }
                }

                // Chart Canvas Area
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .padding(top = 10.dp)
                ) {
                    TrendChartCanvas(
                        points = currentChart.points,
                        peakPrice = currentChart.peakPrice,
                        peakXRatio = currentChart.peakXRatio,
                        peakYRatio = currentChart.peakYRatio,
                        currencyUnit = state.currencyUnit,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Horizontal Time Axis
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    currentChart.timeLabels.forEachIndexed { idx, label ->
                        val isLive = idx == 0 || (selectedHorizon == TimeHorizon.TODAY && idx == currentChart.timeLabels.lastIndex)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            if (isLive && selectedHorizon == TimeHorizon.TODAY && idx == currentChart.timeLabels.lastIndex) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(colors.goldPrimary)
                                )
                            }
                            Text(
                                text = PersianNumberFormatter.toPersianDigits(label),
                                fontSize = 10.sp,
                                fontWeight = if (isLive) FontWeight.Bold else FontWeight.Normal,
                                color = if (isLive) colors.goldPrimary else colors.textMuted
                            )
                        }
                    }
                }

                // Reference Basis Footer
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = colors.surfaceElevated,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 9.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = DashGlobeVector,
                                contentDescription = null,
                                tint = colors.goldPrimary,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = state.referenceIndexText,
                                fontSize = 11.sp,
                                color = colors.textSecondary
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = DashTrendingUpVector,
                                contentDescription = null,
                                tint = Color(0xFF10B981),
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = state.referenceIndexChange,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF047857)
                            )
                        }
                    }
                }
            }
        }

        // ==========================================
        // 4. Primary Instant Actions For Goldsmiths
        // ==========================================
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Action 1: ثبت در فاکتور (Primary Gold Gradient Button)
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color.Transparent,
                shadowElevation = 3.dp,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                Color(0xFFE6CA65),
                                Color(0xFFD4AF37),
                                Color(0xFFB8860B)
                            )
                        )
                    )
                    .clickable {
                        onAddToInvoice(state.currentPrice, state.type.defaultPurityKarat)
                    }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 13.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = CalcPostAdd,
                        contentDescription = null,
                        tint = Color(0xFF111827),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "ثبت در فاکتور",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF111827)
                    )
                }
            }

            // Action 2: تنظیم هشدار (Outlined Surface Button)
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = colors.surface,
                border = BorderStroke(0.8.dp, colors.goldBorder),
                shadowElevation = 1.dp,
                modifier = Modifier
                    .weight(1f)
                    .clickable {
                        isAlertActive = !isAlertActive
                        val msg = if (isAlertActive) {
                            "هشدار نوسان نرخ برای ${state.title} فعال شد"
                        } else {
                            "هشدار نوسان قیمت غیرفعال گردید"
                        }
                        QiratoToast.show(context, msg)
                        onSetPriceAlert(state.type, state.currentPrice)
                    }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 13.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = IconBellActive,
                        contentDescription = null,
                        tint = colors.goldPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "تنظیم هشدار",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textMain
                    )
                }
            }
        }

        // ==========================================
        // 5. Detailed Technical Comparison Metrics (آمار و اطلاعات بازار)
        // ==========================================
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = colors.surface,
            border = BorderStroke(0.6.dp, colors.goldBorder),
            shadowElevation = if (colors.isDark) 0.dp else 1.5.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
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
                        Icon(
                            imageVector = IconAnalytics,
                            contentDescription = null,
                            tint = colors.goldPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "آمار و اطلاعات بازار",
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textMain
                        )
                    }

                    Text(
                        text = "تحلیل ۳۰ روزه",
                        fontSize = 10.5.sp,
                        color = colors.textMuted
                    )
                }

                HorizontalDivider(color = colors.border.copy(alpha = 0.4f), thickness = 0.6.dp)

                MetricRow(
                    label = "دامنه نوسان روزانه",
                    value = state.monthlyStats.dailyRangeText,
                    isHighlight = true
                )

                HorizontalDivider(color = colors.border.copy(alpha = 0.4f), thickness = 0.6.dp)

                MetricRow(
                    label = "تغییر هفتگی",
                    value = state.monthlyStats.weeklyChangeText,
                    isHighlight = true
                )

                HorizontalDivider(color = colors.border.copy(alpha = 0.4f), thickness = 0.6.dp)

                MetricRow(
                    label = "بیشترین قیمت (۳۰ روز)",
                    value = "${PersianNumberFormatter.format(state.monthlyStats.thirtyDayHigh)} ${state.currencyUnit}",
                    isHighlight = false
                )

                HorizontalDivider(color = colors.border.copy(alpha = 0.4f), thickness = 0.6.dp)

                MetricRow(
                    label = "کمترین قیمت (۳۰ روز)",
                    value = "${PersianNumberFormatter.format(state.monthlyStats.thirtyDayLow)} ${state.currencyUnit}",
                    isHighlight = false
                )

                HorizontalDivider(color = colors.border.copy(alpha = 0.4f), thickness = 0.6.dp)

                MetricRow(
                    label = "میانگین موزون بازار",
                    value = "${PersianNumberFormatter.format(state.monthlyStats.weightedAverage)} ${state.currencyUnit}",
                    isHighlight = false
                )
            }
        }

        // ==========================================
        // 6. Recent Ledger Transactions Tied to Asset
        // ==========================================
        if (state.recentTransactions.isNotEmpty()) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = colors.surface,
                border = BorderStroke(0.6.dp, colors.goldBorder),
                shadowElevation = if (colors.isDark) 0.dp else 1.5.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
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
                                    .size(28.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(colors.goldPrimary.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = CalcReceiptLong,
                                    contentDescription = null,
                                    tint = colors.goldPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Text(
                                text = "آخرین معاملات ثبت‌شده",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textMain
                            )
                        }

                        Text(
                            text = "مشاهده همه >",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.goldPrimary,
                            modifier = Modifier.clickable { onViewAllTransactions() }
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        state.recentTransactions.forEach { tx ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = colors.surfaceElevated,
                                border = BorderStroke(0.6.dp, colors.border.copy(alpha = 0.5f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
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
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = if (tx.isBuy) Color(0xFF10B981).copy(alpha = 0.18f) else Color(0xFFF59E0B).copy(alpha = 0.18f)
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = if (tx.isBuy) LedgerArrowReceiveVector else LedgerArrowPayVector,
                                                        contentDescription = null,
                                                        tint = if (tx.isBuy) Color(0xFF047857) else Color(0xFFB45309),
                                                        modifier = Modifier.size(11.dp)
                                                    )
                                                    Text(
                                                        text = if (tx.isBuy) "خرید" else "فروش",
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (tx.isBuy) Color(0xFF047857) else Color(0xFFB45309)
                                                    )
                                                }
                                            }

                                            Text(
                                                text = tx.title,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = colors.textMain
                                            )
                                        }

                                        Text(
                                            text = tx.timeText,
                                            fontSize = 10.sp,
                                            color = colors.textMuted
                                        )
                                    }

                                    HorizontalDivider(
                                        color = colors.border.copy(alpha = 0.35f),
                                        thickness = 0.6.dp
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = tx.specDetails,
                                            fontSize = 10.5.sp,
                                            color = colors.textSecondary
                                        )

                                        Row(
                                            verticalAlignment = Alignment.Bottom,
                                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                                        ) {
                                            Text(
                                                text = PersianNumberFormatter.format(tx.totalPrice),
                                                fontSize = 12.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = colors.textMain
                                            )
                                            Text(
                                                text = state.currencyUnit,
                                                fontSize = 9.5.sp,
                                                color = colors.textMuted
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}

// ==========================================
// Subcomponents & Canvas Implementation
// ==========================================

@Composable
private fun MetricTile(
    title: String,
    value: String,
    icon: ImageVector,
    iconColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color.White.copy(alpha = 0.06f),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(13.dp)
                )
                Text(
                    text = title,
                    fontSize = 10.sp,
                    color = Color.White.copy(alpha = 0.65f)
                )
            }

            Text(
                text = value,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun MetricRow(
    label: String,
    value: String,
    isHighlight: Boolean
) {
    val colors = LocalGoldExColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = colors.textSecondary
        )
        Text(
            text = value,
            fontSize = 12.sp,
            fontWeight = if (isHighlight) FontWeight.Bold else FontWeight.Medium,
            color = if (isHighlight) Color(0xFF047857) else colors.textMain
        )
    }
}

/**
 * TrendChartCanvas:
 * Custom Vector Micro-Trading Chart rendering cubic Bezier curve,
 * gradient gold area underneath, horizontal dashed guides, and apex marker.
 */
@Composable
private fun TrendChartCanvas(
    points: List<Pair<Float, Float>>,
    peakPrice: Long,
    peakXRatio: Float,
    peakYRatio: Float,
    currencyUnit: String,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            if (points.isEmpty()) return@Canvas

            // 1. Subtle horizontal benchmark dashed lines
            val dashEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
            val benchmarkColor = Color(0xFFD0C5AF).copy(alpha = 0.35f)
            listOf(0.20f, 0.50f, 0.80f).forEach { yNorm ->
                drawLine(
                    color = benchmarkColor,
                    start = Offset(0f, h * yNorm),
                    end = Offset(w, h * yNorm),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = dashEffect
                )
            }

            // Convert normalized points (where 1.0 is peak/top and 0.0 is bottom) to Canvas coords
            val canvasPoints = points.map { (xNorm, yNorm) ->
                Offset(
                    x = xNorm * w,
                    y = (1f - yNorm) * (h - 24.dp.toPx()) + 12.dp.toPx()
                )
            }

            // 2. Build Smooth Cubic Bezier Line Path
            val linePath = Path().apply {
                moveTo(canvasPoints.first().x, canvasPoints.first().y)
                for (i in 0 until canvasPoints.size - 1) {
                    val p0 = canvasPoints[i]
                    val p1 = canvasPoints[i + 1]
                    val cx = (p0.x + p1.x) / 2f
                    cubicTo(
                        x1 = cx, y1 = p0.y,
                        x2 = cx, y2 = p1.y,
                        x3 = p1.x, y3 = p1.y
                    )
                }
            }

            // 3. Shaded Area Under Curve
            val areaPath = Path().apply {
                addPath(linePath)
                lineTo(canvasPoints.last().x, h)
                lineTo(canvasPoints.first().x, h)
                close()
            }

            drawPath(
                path = areaPath,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFD4AF37).copy(alpha = 0.30f),
                        Color(0xFFD4AF37).copy(alpha = 0.06f),
                        Color(0xFFD4AF37).copy(alpha = 0.00f)
                    ),
                    startY = 0f,
                    endY = h
                )
            )

            // 4. Gold Glowing Stroke
            drawPath(
                path = linePath,
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color(0xFFB8860B),
                        Color(0xFFE6CA65),
                        Color(0xFFD4AF37)
                    )
                ),
                style = Stroke(
                    width = 3.dp.toPx(),
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )

            // 5. Circle Indicators: Start Point, Peak Point, Live End Point
            val startPoint = canvasPoints.first()
            drawCircle(
                color = Color(0xFF735C00),
                radius = 3.5.dp.toPx(),
                center = startPoint
            )

            // Peak Summit Point
            val peakPoint = Offset(
                x = peakXRatio * w,
                y = (1f - peakYRatio) * (h - 24.dp.toPx()) + 12.dp.toPx()
            )
            drawCircle(
                color = Color.White,
                radius = 5.dp.toPx(),
                center = peakPoint
            )
            drawCircle(
                color = Color(0xFFB8860B),
                radius = 5.dp.toPx(),
                center = peakPoint,
                style = Stroke(width = 2.5.dp.toPx())
            )

            // Live End Point
            val endPoint = canvasPoints.last()
            drawCircle(
                color = Color(0xFFD4AF37).copy(alpha = 0.3f),
                radius = 8.dp.toPx(),
                center = endPoint
            )
            drawCircle(
                color = Color(0xFFD4AF37),
                radius = 5.dp.toPx(),
                center = endPoint
            )
            drawCircle(
                color = Color.White,
                radius = 5.dp.toPx(),
                center = endPoint,
                style = Stroke(width = 2.dp.toPx())
            )
        }

        // 6. Floating Peak Value Marker positioned near summit
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val peakX = maxWidth * peakXRatio
            val peakY = maxHeight * (1f - peakYRatio)

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFF24211A),
                border = BorderStroke(0.6.dp, Color(0xFFD4AF37).copy(alpha = 0.5f)),
                shadowElevation = 4.dp,
                modifier = Modifier
                    .offset(
                        x = (peakX - 55.dp).coerceAtLeast(8.dp),
                        y = (peakY - 26.dp).coerceAtLeast(2.dp)
                    )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFD4AF37))
                    )
                    Text(
                        text = "اوج: ${PersianNumberFormatter.format(peakPrice)} $currencyUnit",
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFFE088)
                    )
                }
            }
        }
    }
}

// ==========================================
// Custom Self-Contained Vector Icons
// ==========================================

private val IconArrowDown: ImageVector = ImageVector.Builder(
    name = "IconArrowDown",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f
).apply {
    path(
        stroke = SolidColor(Color.White),
        strokeLineWidth = 2.2f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round
    ) {
        moveTo(12f, 5f)
        lineTo(12f, 19f)
        moveTo(19f, 12f)
        lineTo(12f, 19f)
        lineTo(5f, 12f)
    }
}.build()

private val IconArrowUp: ImageVector = ImageVector.Builder(
    name = "IconArrowUp",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f
).apply {
    path(
        stroke = SolidColor(Color.White),
        strokeLineWidth = 2.2f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round
    ) {
        moveTo(12f, 19f)
        lineTo(12f, 5f)
        moveTo(5f, 12f)
        lineTo(12f, 5f)
        lineTo(19f, 12f)
    }
}.build()

private val IconSwapVert: ImageVector = ImageVector.Builder(
    name = "IconSwapVert",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f
).apply {
    path(
        stroke = SolidColor(Color.White),
        strokeLineWidth = 2.2f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round
    ) {
        moveTo(16f, 17f)
        lineTo(16f, 3f)
        moveTo(12f, 7f)
        lineTo(16f, 3f)
        lineTo(20f, 7f)
        moveTo(8f, 7f)
        lineTo(8f, 21f)
        moveTo(4f, 17f)
        lineTo(8f, 21f)
        lineTo(12f, 17f)
    }
}.build()

private val IconBellActive: ImageVector = ImageVector.Builder(
    name = "IconBellActive",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f
).apply {
    path(
        stroke = SolidColor(Color.White),
        strokeLineWidth = 2f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round
    ) {
        moveTo(18f, 8f)
        curveTo(18f, 4.69f, 15.31f, 2f, 12f, 2f)
        curveTo(8.69f, 2f, 6f, 4.69f, 6f, 8f)
        curveTo(6f, 15f, 3f, 17f, 3f, 17f)
        horizontalLineTo(21f)
        curveTo(21f, 17f, 18f, 15f, 18f, 8f)
        close()
        moveTo(13.73f, 21f)
        curveTo(13.3f, 21.6f, 12.7f, 22f, 12f, 22f)
        curveTo(11.3f, 22f, 10.7f, 21.6f, 10.27f, 21f)
    }
}.build()

private val IconBellNone: ImageVector = ImageVector.Builder(
    name = "IconBellNone",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f
).apply {
    path(
        stroke = SolidColor(Color.White),
        strokeLineWidth = 2f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round
    ) {
        moveTo(18f, 8f)
        curveTo(18f, 4.69f, 15.31f, 2f, 12f, 2f)
        curveTo(8.69f, 2f, 6f, 4.69f, 6f, 8f)
        curveTo(6f, 15f, 3f, 17f, 3f, 17f)
        horizontalLineTo(21f)
        curveTo(21f, 17f, 18f, 15f, 18f, 8f)
        close()
    }
}.build()

private val IconAnalytics: ImageVector = ImageVector.Builder(
    name = "IconAnalytics",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f
).apply {
    path(
        stroke = SolidColor(Color.White),
        strokeLineWidth = 2.2f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round
    ) {
        moveTo(3f, 3f)
        verticalLineTo(21f)
        horizontalLineTo(21f)
        moveTo(18f, 9f)
        lineTo(14f, 13f)
        lineTo(10f, 9f)
        lineTo(6f, 13f)
    }
}.build()

private val IconShowChart: ImageVector = ImageVector.Builder(
    name = "IconShowChart",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f
).apply {
    path(
        stroke = SolidColor(Color.White),
        strokeLineWidth = 2.2f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round
    ) {
        moveTo(3.5f, 18.5f)
        lineTo(9.5f, 12.5f)
        lineTo(13.5f, 16.5f)
        lineTo(20.5f, 7.5f)
    }
}.build()
