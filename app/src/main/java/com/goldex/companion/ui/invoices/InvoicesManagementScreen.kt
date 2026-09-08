package com.goldex.companion.ui.invoices

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.goldex.companion.model.InvoiceCardAction
import com.goldex.companion.model.InvoiceFilterTab
import com.goldex.companion.model.InvoiceListItem
import com.goldex.companion.model.InvoiceStatus
import com.goldex.companion.model.PersianNumberFormatter
import com.goldex.companion.ui.components.AnimatedNumberText
import com.goldex.companion.ui.components.AnimatedPriceText
import com.goldex.companion.ui.invoices.components.InvoiceCheckVector
import com.goldex.companion.ui.invoices.components.InvoicePdfVector
import com.goldex.companion.ui.invoices.components.InvoicePlusVector
import com.goldex.companion.ui.theme.LocalGoldExColors
import com.goldex.companion.ui.theme.LuxuryMotion
import com.goldex.companion.ui.theme.VazirmatnFamily
import com.goldex.companion.ui.theme.goldGradient

@Composable
fun InvoicesManagementScreen(
    uiState: BarterInvoiceUiState,
    onSearchQueryChange: (String) -> Unit,
    onFilterSelect: (InvoiceFilterTab) -> Unit,
    onNewInvoiceClick: () -> Unit,
    onInvoiceItemClick: (InvoiceListItem) -> Unit,
    onExportPdfClick: (InvoiceListItem) -> Unit,
    onScanQrClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalGoldExColors.current
    val invoices = uiState.filteredInvoices
    val allInvoices = uiState.invoicesList

    val settledCount = remember(allInvoices) { allInvoices.count { it.status == InvoiceStatus.SETTLED } }
    val pendingCount = remember(allInvoices) { allInvoices.count { it.status == InvoiceStatus.PARTIALLY_PAID } }
    val workshopCount = remember(allInvoices) { allInvoices.count { it.status == InvoiceStatus.WORKSHOP } }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. KPI Overview Banner (Screen 6 Hero Section)
            InvoicesKpiOverviewBanner(
                totalInvoicesCount = allInvoices.size.coerceAtLeast(48),
                totalGoldWeightGrams = 342.5,
                totalTurnoverMillionTomans = 2450L
            )

            // 2. Search & Filter Bar
            SearchAndQuickFilterBar(
                query = uiState.searchQuery,
                onQueryChange = onSearchQueryChange,
                onScanQrClick = onScanQrClick
            )

            // 3. Filter Capsules
            FilterCapsulesRow(
                selectedFilter = uiState.selectedFilter,
                allCount = allInvoices.size.coerceAtLeast(48),
                settledCount = settledCount.coerceAtLeast(42),
                pendingCount = pendingCount.coerceAtLeast(4),
                workshopCount = workshopCount.coerceAtLeast(2),
                onSelectFilter = onFilterSelect
            )

            // 4. Invoices List with smooth animation on filter and query change
            AnimatedContent(
                targetState = uiState.selectedFilter to invoices.isEmpty(),
                transitionSpec = {
                    (LuxuryMotion.FilterEnter).togetherWith(LuxuryMotion.FilterExit)
                },
                label = "invoicesListFilterTransition"
            ) { (_, isEmpty) ->
                if (isEmpty) {
                    EmptyInvoicesCard(
                        onResetSearch = {
                            onSearchQueryChange("")
                            onFilterSelect(InvoiceFilterTab.ALL)
                        }
                    )
                } else {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        invoices.forEach { invoiceItem ->
                            InvoiceTransactionCard(
                                item = invoiceItem,
                                onCardClick = { onInvoiceItemClick(invoiceItem) },
                                onPdfClick = { onExportPdfClick(invoiceItem) }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 5. New Invoice Action Banner / Floating Trigger
            NewInvoiceActionCard(
                onNewInvoiceClick = onNewInvoiceClick
            )
        }
    }
}

/**
 * 1. KPI Overview Banner matching Stitch Screen 6
 */
@Composable
private fun InvoicesKpiOverviewBanner(
    totalInvoicesCount: Int,
    totalGoldWeightGrams: Double,
    totalTurnoverMillionTomans: Long,
    modifier: Modifier = Modifier
) {
    val colors = LocalGoldExColors.current

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color(0xFF141B2B),
        border = BorderStroke(1.dp, Color(0x4DF59E0B)),
        shadowElevation = 8.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(
                            Color(0xFF1C222E),
                            Color(0xFF151922),
                            Color(0xFF0F131A)
                        )
                    )
                )
                .padding(16.dp)
        ) {
            // Ambient soft glowing background blobs
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(Color(0x1AF59E0B))
            )

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header row: Title + Growth Badge
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp),
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
                                .background(Color(0x26F59E0B))
                                .border(0.8.dp, Color(0x40F59E0B), RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "📊",
                                fontSize = 14.sp
                            )
                        }
                        Text(
                            text = "عملکرد زرگری در شهریور ۱۴۰۳",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFDE68A),
                            fontFamily = VazirmatnFamily
                        )
                    }

                    // Growth pill badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(Color(0x33059669))
                            .border(0.8.dp, Color(0x4D10B981), RoundedCornerShape(50))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "▲",
                                fontSize = 9.sp,
                                color = Color(0xFF34D399)
                            )
                            Text(
                                text = "+۱۲.۴٪ رشد",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF34D399),
                                fontFamily = VazirmatnFamily
                            )
                        }
                    }
                }

                // 3 Stats Boxes
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    KpiStatBox(
                        title = "کل فاکتورها",
                        value = PersianNumberFormatter.toPersianDigits(totalInvoicesCount.toString()),
                        unit = "فقره",
                        valueColor = Color(0xFFFCD34D),
                        modifier = Modifier.weight(1f)
                    )
                    KpiStatBox(
                        title = "وزن طلای فروش",
                        value = PersianNumberFormatter.toPersianDigits(String.format(java.util.Locale.US, "%.1f", totalGoldWeightGrams)),
                        unit = "گرم",
                        valueColor = Color.White,
                        modifier = Modifier.weight(1f)
                    )
                    KpiStatBox(
                        title = "گردش مالی کل",
                        value = PersianNumberFormatter.toPersianDigits(PersianNumberFormatter.formatPrice(totalTurnoverMillionTomans)),
                        unit = "م.تومان",
                        valueColor = Color(0xFFFCD34D),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun KpiStatBox(
    title: String,
    value: String,
    unit: String,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0x0DFFFFFF))
            .border(0.7.dp, Color(0x1AFFFFFF), RoundedCornerShape(12.dp))
            .padding(vertical = 8.dp, horizontal = 6.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFFCBD5E1),
                fontFamily = VazirmatnFamily
            )
            AnimatedNumberText(
                text = value,
                fontSize = 16.sp,
                fontWeight = FontWeight.Black,
                color = valueColor,
                unit = unit
            )
        }
    }
}

/**
 * 2. Search & Filter Bar matching Stitch Screen 6
 */
@Composable
private fun SearchAndQuickFilterBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onScanQrClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalGoldExColors.current

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Search Input Box
        Box(
            modifier = Modifier
                .weight(1f)
                .height(46.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(colors.surface)
                .border(1.dp, colors.border, RoundedCornerShape(16.dp))
                .padding(horizontal = 12.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "🔍",
                    fontSize = 14.sp
                )

                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (query.isEmpty()) {
                        Text(
                            text = "جستجوی مشتری، شناسه فاکتور، تلفن...",
                            fontSize = 12.sp,
                            color = colors.textMuted,
                            fontFamily = VazirmatnFamily
                        )
                    }
                    BasicTextField(
                        value = query,
                        onValueChange = onQueryChange,
                        singleLine = true,
                        textStyle = TextStyle(
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = colors.textMain,
                            fontFamily = VazirmatnFamily,
                            textAlign = TextAlign.Right,
                            textDirection = TextDirection.Rtl
                        ),
                        cursorBrush = SolidColor(colors.goldPrimary),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // QR/Barcode Scan Icon Button
                IconButton(
                    onClick = onScanQrClick,
                    modifier = Modifier.size(28.dp)
                ) {
                    Text(
                        text = "📷",
                        fontSize = 16.sp
                    )
                }
            }
        }

        // Filter / Sort Action Button
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(colors.surface)
                .border(1.dp, colors.border, RoundedCornerShape(16.dp))
                .clickable { /* Filter and Sort Action */ },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "⚡",
                fontSize = 18.sp
            )
        }
    }
}

/**
 * 3. Filter Capsules matching Stitch Screen 6
 */
@Composable
private fun FilterCapsulesRow(
    selectedFilter: InvoiceFilterTab,
    allCount: Int,
    settledCount: Int,
    pendingCount: Int,
    workshopCount: Int,
    onSelectFilter: (InvoiceFilterTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterCapsuleItem(
            title = "همه",
            count = allCount,
            isSelected = selectedFilter == InvoiceFilterTab.ALL,
            badgeBg = Color(0x33F59E0B),
            badgeTextColor = Color(0xFFFDE68A),
            onClick = { onSelectFilter(InvoiceFilterTab.ALL) }
        )
        FilterCapsuleItem(
            title = "تسویه شده",
            count = settledCount,
            isSelected = selectedFilter == InvoiceFilterTab.SETTLED,
            badgeBg = Color(0x2610B981),
            badgeTextColor = Color(0xFF10B981),
            onClick = { onSelectFilter(InvoiceFilterTab.SETTLED) }
        )
        FilterCapsuleItem(
            title = "در انتظار پرداخت",
            count = pendingCount,
            isSelected = selectedFilter == InvoiceFilterTab.PENDING,
            badgeBg = Color(0x33F59E0B),
            badgeTextColor = Color(0xFFF59E0B),
            onClick = { onSelectFilter(InvoiceFilterTab.PENDING) }
        )
        FilterCapsuleItem(
            title = "سفارش کارگاه",
            count = workshopCount,
            isSelected = selectedFilter == InvoiceFilterTab.WORKSHOP,
            badgeBg = Color(0x263B82F6),
            badgeTextColor = Color(0xFF3B82F6),
            onClick = { onSelectFilter(InvoiceFilterTab.WORKSHOP) }
        )
    }
}

@Composable
private fun FilterCapsuleItem(
    title: String,
    count: Int,
    isSelected: Boolean,
    badgeBg: Color,
    badgeTextColor: Color,
    onClick: () -> Unit
) {
    val colors = LocalGoldExColors.current

    val animatedBg by animateColorAsState(
        targetValue = if (isSelected) Color(0xFF1E232E) else colors.surface,
        animationSpec = tween(durationMillis = 200, easing = LuxuryMotion.StandardEasing),
        label = "capsuleBg"
    )
    val animatedBorder by animateColorAsState(
        targetValue = if (isSelected) Color(0x66F59E0B) else colors.border,
        animationSpec = tween(durationMillis = 200, easing = LuxuryMotion.StandardEasing),
        label = "capsuleBorder"
    )
    val animatedTextColor by animateColorAsState(
        targetValue = if (isSelected) Color(0xFFFCD34D) else colors.textSecondary,
        animationSpec = tween(durationMillis = 200, easing = LuxuryMotion.StandardEasing),
        label = "capsuleTextColor"
    )

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(animatedBg)
            .border(1.dp, animatedBorder, RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = title,
                fontSize = 11.5.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = animatedTextColor,
                fontFamily = VazirmatnFamily
            )

            // Count badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(badgeBg)
                    .padding(horizontal = 6.dp, vertical = 1.dp)
            ) {
                Text(
                    text = PersianNumberFormatter.toPersianDigits(count.toString()),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = badgeTextColor,
                    fontFamily = VazirmatnFamily
                )
            }
        }
    }
}

/**
 * 4. Invoice Transaction Card matching Stitch Screen 6
 */
@Composable
private fun InvoiceTransactionCard(
    item: InvoiceListItem,
    onCardClick: () -> Unit,
    onPdfClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalGoldExColors.current

    val (badgeBg, badgeBorder, badgeTextColor) = when (item.status) {
        InvoiceStatus.SETTLED -> Triple(Color(0x2610B981), Color(0x4D10B981), Color(0xFF10B981))
        InvoiceStatus.PARTIALLY_PAID -> Triple(Color(0x33F59E0B), Color(0x66F59E0B), Color(0xFFF59E0B))
        InvoiceStatus.WORKSHOP -> Triple(Color(0x263B82F6), Color(0x4D3B82F6), Color(0xFF3B82F6))
    }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = colors.surface,
        border = BorderStroke(1.dp, colors.border),
        shadowElevation = 2.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Top Row: Avatar + Name & Code + Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Customer monogram avatar
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0x26DFB35A))
                            .border(1.dp, Color(0x4DDFB35A), RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = item.customerInitials,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.goldPrimary,
                            fontFamily = VazirmatnFamily
                        )
                    }

                    Column(
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = item.customerName,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textMain,
                                fontFamily = VazirmatnFamily,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (item.isVerified) {
                                Box(
                                    modifier = Modifier
                                        .size(15.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF10B981)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = InvoiceCheckVector,
                                        contentDescription = "تایید شده",
                                        tint = Color.White,
                                        modifier = Modifier.size(10.dp)
                                    )
                                }
                            }
                        }

                        Text(
                            text = PersianNumberFormatter.toPersianDigits(item.createdAtText),
                            fontSize = 10.5.sp,
                            color = colors.textMuted,
                            fontFamily = VazirmatnFamily
                        )
                    }
                }

                // Status Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(badgeBg)
                        .border(0.8.dp, badgeBorder, RoundedCornerShape(50))
                        .padding(horizontal = 9.dp, vertical = 3.5.dp)
                ) {
                    Text(
                        text = item.statusDetail,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = badgeTextColor,
                        fontFamily = VazirmatnFamily
                    )
                }
            }

            // Middle Box: Items Summary and Weight/Wages
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.surfaceElevated)
                    .border(0.8.dp, colors.border, RoundedCornerShape(12.dp))
                    .padding(10.dp)
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = item.itemsCountText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = colors.textMuted,
                            fontFamily = VazirmatnFamily
                        )
                        Text(
                            text = item.itemsSummary,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textMain,
                            fontFamily = VazirmatnFamily,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(0.6.dp)
                            .background(colors.border)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = PersianNumberFormatter.toPersianDigits(item.line1Detail),
                            fontSize = 10.5.sp,
                            color = colors.textSecondary,
                            fontFamily = VazirmatnFamily
                        )
                        Text(
                            text = PersianNumberFormatter.toPersianDigits(item.line2Detail),
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = when (item.status) {
                                InvoiceStatus.PARTIALLY_PAID -> colors.errorRed
                                InvoiceStatus.WORKSHOP -> Color(0xFF3B82F6)
                                else -> colors.textSecondary
                            },
                            fontFamily = VazirmatnFamily
                        )
                    }
                }
            }

            // Bottom Row: Price & Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(1.dp)
                ) {
                    Text(
                        text = item.amountLabel,
                        fontSize = 10.sp,
                        color = colors.textMuted,
                        fontFamily = VazirmatnFamily
                    )
                    AnimatedPriceText(
                        amount = item.finalAmount,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        color = colors.textMain,
                        unit = "تومان"
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // PDF Button
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(colors.surfaceElevated)
                            .border(1.dp, colors.border, RoundedCornerShape(12.dp))
                            .clickable(onClick = onPdfClick)
                            .padding(horizontal = 9.dp, vertical = 7.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = InvoicePdfVector,
                                contentDescription = "PDF",
                                tint = colors.textSecondary,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = "PDF",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textSecondary,
                                fontFamily = VazirmatnFamily
                            )
                        }
                    }

                    // Primary Action Button
                    val (actionBg, actionText) = when (item.status) {
                        InvoiceStatus.SETTLED -> Pair(
                            Color(0xFF8C6F16),
                            Color.White
                        )
                        InvoiceStatus.PARTIALLY_PAID -> Pair(
                            Color(0xFFD97706),
                            Color.White
                        )
                        InvoiceStatus.WORKSHOP -> Pair(
                            Color(0xFF1E232E),
                            Color.White
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(actionBg)
                            .clickable(onClick = onCardClick)
                            .padding(horizontal = 12.dp, vertical = 7.dp)
                    ) {
                        Text(
                            text = item.actionButtonText,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = actionText,
                            fontFamily = VazirmatnFamily
                        )
                    }
                }
            }
        }
    }
}

/**
 * 5. Action Card / Floating Trigger for Creating a New Invoice
 */
@Composable
private fun NewInvoiceActionCard(
    onNewInvoiceClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color(0xFF141B2B),
        border = BorderStroke(1.dp, Color(0x4DF59E0B)),
        shadowElevation = 6.dp,
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onNewInvoiceClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            Color(0xFF8C6F16),
                            Color(0xFFC7983B),
                            Color(0xFF8C6F16)
                        )
                    )
                )
                .padding(vertical = 14.dp, horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(Color(0x33000000)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = InvoicePlusVector,
                        contentDescription = "صدور فاکتور جدید",
                        tint = Color.White,
                        modifier = Modifier.size(15.dp)
                    )
                }

                Text(
                    text = "صدور فاکتور جامع جدید و تهاتر طلا",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontFamily = VazirmatnFamily
                )
            }
        }
    }
}

@Composable
private fun EmptyInvoicesCard(
    onResetSearch: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalGoldExColors.current

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = colors.surface,
        border = BorderStroke(1.dp, colors.border),
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "🔎",
                fontSize = 32.sp
            )
            Text(
                text = "هیچ فاکتوری با مشخصات مورد نظر یافت نشد",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textMain,
                fontFamily = VazirmatnFamily
            )
            Text(
                text = "می‌توانید فیلترها را حذف کنید یا فاکتور جدید صادر نمایید.",
                fontSize = 11.5.sp,
                color = colors.textMuted,
                fontFamily = VazirmatnFamily
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.surfaceElevated)
                    .border(1.dp, colors.goldBorder, RoundedCornerShape(12.dp))
                    .clickable(onClick = onResetSearch)
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "مشاهده همه فاکتورها",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.goldPrimary,
                    fontFamily = VazirmatnFamily
                )
            }
        }
    }
}
