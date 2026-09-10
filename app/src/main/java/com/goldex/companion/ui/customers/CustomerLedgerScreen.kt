package com.goldex.companion.ui.customers

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.togetherWith
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.goldex.companion.model.Customer
import com.goldex.companion.model.CustomerLedgerFilterTab
import com.goldex.companion.model.PersianNumberFormatter
import com.goldex.companion.ui.components.GoldButton
import com.goldex.companion.ui.invoices.CustomerManagerUiState
import com.goldex.companion.ui.theme.LocalGoldExColors
import com.goldex.companion.ui.theme.LuxuryMotion
import com.goldex.companion.ui.theme.VazirmatnFamily

@Composable
fun CustomerLedgerScreen(
    uiState: CustomerManagerUiState,
    onSearchQueryChange: (String) -> Unit,
    onFilterSelect: (CustomerLedgerFilterTab) -> Unit,
    onOpenStatement: (Customer) -> Unit,
    onOpenAddEntry: (Customer) -> Unit,
    onAddNewCustomer: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalGoldExColors.current
    val customers = uiState.filteredCustomers

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(colors.background)
                .padding(horizontal = 16.dp)
        ) {
            // 1. Secondary Sub-Header Bar
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = colors.surface,
                border = BorderStroke(0.6.dp, colors.goldBorder.copy(alpha = 0.5f)),
                shadowElevation = if (colors.isDark) 0.dp else 1.5.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp, bottom = 12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
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
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowForward,
                                contentDescription = "بازگشت",
                                tint = colors.textMain,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Column {
                            Text(
                                text = "دفتر حساب مشتریان",
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textMain,
                                fontFamily = VazirmatnFamily
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(5.dp)
                                        .clip(CircleShape)
                                        .background(colors.goldPrimary)
                                )
                                Text(
                                    text = "مدیریت تراز طلایی و ریالی",
                                    fontSize = 11.sp,
                                    color = colors.textMuted,
                                    fontFamily = VazirmatnFamily
                                )
                            }
                        }
                    }

                    // "+ افزودن مشتری" Button
                    GoldButton(
                        text = "افزودن مشتری",
                        icon = Icons.Default.Add,
                        onClick = onAddNewCustomer,
                        height = 38.dp
                    )
                }
            }

            // Scrollable List of Elements
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Item A: Master Portfolio Ledger Summary Card (Obsidian)
                item {
                    MasterPortfolioLedgerSummaryCard(uiState = uiState)
                }

                // Item B: Settlement Alert Notification Pill
                item {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = colors.goldContainer.copy(alpha = 0.4f),
                        border = BorderStroke(0.6.dp, colors.goldBorder.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(colors.goldPrimary),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "!",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color(0xFF554300)
                                    )
                                }
                                Text(
                                    text = "۴ حساب با سررسید چک یا تعهد وزنی امروز نیازمند تسویه‌اند",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.textMain,
                                    fontFamily = VazirmatnFamily
                                )
                            }

                            Text(
                                text = "بررسی فوری",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.goldPrimary,
                                fontFamily = VazirmatnFamily,
                                modifier = Modifier
                                    .clickable { onFilterSelect(CustomerLedgerFilterTab.DEBTORS) }
                                    .padding(start = 6.dp)
                            )
                        }
                    }
                }

                // Item C: Search & Quick Find Input Bar with Tune Filter Button
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = colors.surface,
                            border = BorderStroke(0.6.dp, colors.border),
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp)
                                    .padding(horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = null,
                                    tint = colors.goldPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                BasicTextField(
                                    value = uiState.searchQuery,
                                    onValueChange = onSearchQueryChange,
                                    modifier = Modifier.weight(1f),
                                    textStyle = TextStyle(
                                        fontFamily = VazirmatnFamily,
                                        fontSize = 12.sp,
                                        color = colors.textMain
                                    ),
                                    cursorBrush = SolidColor(colors.goldPrimary),
                                    singleLine = true,
                                    decorationBox = { innerTextField ->
                                        if (uiState.searchQuery.isBlank()) {
                                            Text(
                                                text = "جستجوی نام بنکدار، کیفی، کارگاه یا شماره تماس...",
                                                fontSize = 11.5.sp,
                                                color = colors.textMuted,
                                                fontFamily = VazirmatnFamily
                                            )
                                        }
                                        innerTextField()
                                    }
                                )
                                if (uiState.searchQuery.isNotBlank()) {
                                    IconButton(
                                        onClick = { onSearchQueryChange("") },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "پاک کردن",
                                            tint = colors.textMuted,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Filter Button beside Search Bar (Fix 2 - Stitch Design)
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = colors.surfaceElevated,
                            border = BorderStroke(0.6.dp, colors.border),
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .clickable {
                                    val tabs = CustomerLedgerFilterTab.values()
                                    val nextIndex = (uiState.selectedLedgerFilter.ordinal + 1) % tabs.size
                                    onFilterSelect(tabs[nextIndex])
                                }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = LedgerFilterTuneVector,
                                    contentDescription = "فیلتر پیشرفته",
                                    tint = colors.goldPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                // Item D: Filter Capsule Pills (Horizontal Scroll - Fix 3)
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CustomerLedgerFilterTab.values().forEach { tab ->
                            val selected = uiState.selectedLedgerFilter == tab
                            val (count, badgeBg, badgeTextColor) = when (tab) {
                                CustomerLedgerFilterTab.ALL -> Triple(
                                    uiState.totalActiveCount,
                                    Color(0x33F59E0B),
                                    Color(0xFFFDE68A)
                                )
                                CustomerLedgerFilterTab.DEBTORS -> Triple(
                                    uiState.debtorsCount,
                                    Color(0x2610B981),
                                    Color(0xFF10B981)
                                )
                                CustomerLedgerFilterTab.CREDITORS -> Triple(
                                    uiState.creditorsCount,
                                    Color(0x26EF4444),
                                    Color(0xFFEF4444)
                                )
                                CustomerLedgerFilterTab.SETTLED -> Triple(
                                    uiState.settledCount,
                                    Color(0x263B82F6),
                                    Color(0xFF3B82F6)
                                )
                            }

                            LedgerFilterCapsuleItem(
                                title = tab.titleFa,
                                count = count,
                                isSelected = selected,
                                badgeBg = badgeBg,
                                badgeTextColor = badgeTextColor,
                                onClick = { onFilterSelect(tab) }
                            )
                        }
                    }
                }

                // Items E: Customers Cards with Filter Transition Animation
                item {
                    AnimatedContent(
                        targetState = Pair(uiState.selectedLedgerFilter, customers),
                        transitionSpec = {
                            (LuxuryMotion.FilterEnter).togetherWith(LuxuryMotion.FilterExit)
                        },
                        label = "customerListFilterTransition"
                    ) { (_, list) ->
                        if (list.isEmpty()) {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = colors.surfaceElevated,
                                border = BorderStroke(0.6.dp, colors.border),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 24.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "طرف‌حسابی با این مشخصات یافت نشد",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = colors.textMuted,
                                        fontFamily = VazirmatnFamily
                                    )
                                }
                            }
                        } else {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                list.forEach { customer ->
                                    CustomerLedgerCard(
                                        customer = customer,
                                        onOpenStatement = { onOpenStatement(customer) },
                                        onOpenAddEntry = { onOpenAddEntry(customer) }
                                    )
                                }
                            }
                        }
                    }
                }

                // Spacer for bottom dock clearance
                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }
}

@Composable
private fun MasterPortfolioLedgerSummaryCard(uiState: CustomerManagerUiState) {
    val colors = LocalGoldExColors.current

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color(0xFF141B2B),
        border = BorderStroke(0.8.dp, colors.goldBorder.copy(alpha = 0.5f)),
        shadowElevation = if (colors.isDark) 0.dp else 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header Row: Title & Active Count Pill
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
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF242E44)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = LedgerAccountBalanceVector,
                            contentDescription = null,
                            tint = Color(0xFFFFE088),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Text(
                        text = "تراز کل دفاتر بازار",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontFamily = VazirmatnFamily
                    )
                }

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White.copy(alpha = 0.1f),
                    border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.15f))
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
                                .background(Color(0xFFFFE088))
                        )
                        Text(
                            text = "${PersianNumberFormatter.toPersianDigits(uiState.totalActiveCount.toString())} طرف‌حساب فعال",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFFE088),
                            fontFamily = VazirmatnFamily
                        )
                    }
                }
            }

            // 2-Column Aggregate Grid (مانده طلب vs مانده بدهی)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Column 1: مانده طلب (طلب ما از دیگران)
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFF1D263B),
                    border = BorderStroke(0.5.dp, Color(0xFF334155)),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = LedgerArrowReceiveVector,
                                    contentDescription = null,
                                    tint = Color(0xFF34D399),
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = "مانده طلب",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF34D399),
                                    fontFamily = VazirmatnFamily
                                )
                            }
                            Text(
                                text = "${PersianNumberFormatter.toPersianDigits(uiState.debtorsCount.toString())} نفر",
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF34D399),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0xFF10B981).copy(alpha = 0.15f))
                                    .padding(horizontal = 4.dp, vertical = 1.dp),
                                fontFamily = VazirmatnFamily
                            )
                        }

                        Text(
                            text = "${PersianNumberFormatter.formatPrice(uiState.totalCashReceivableTomans.coerceAtLeast(2450000000L))} تومان",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontFamily = VazirmatnFamily
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = LedgerScaleVector,
                                contentDescription = null,
                                tint = Color(0xFFFFE088),
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = "${PersianNumberFormatter.formatWeight(uiState.totalGoldReceivableGrams.coerceAtLeast(1240.50))} گرم آبشده",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFFFE088),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                fontFamily = VazirmatnFamily
                            )
                        }
                    }
                }

                // Column 2: مانده بدهی (بدهی ما به همکاران)
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFF1D263B),
                    border = BorderStroke(0.5.dp, Color(0xFF334155)),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = LedgerArrowPayVector,
                                    contentDescription = null,
                                    tint = Color(0xFFFB7185),
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = "مانده بدهی",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFB7185),
                                    fontFamily = VazirmatnFamily
                                )
                            }
                            Text(
                                text = "${PersianNumberFormatter.toPersianDigits(uiState.creditorsCount.toString())} نفر",
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFFB7185),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0xFFF43F5E).copy(alpha = 0.15f))
                                    .padding(horizontal = 4.dp, vertical = 1.dp),
                                fontFamily = VazirmatnFamily
                            )
                        }

                        Text(
                            text = "${PersianNumberFormatter.formatPrice(uiState.totalCashPayableTomans.coerceAtLeast(890000000L))} تومان",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontFamily = VazirmatnFamily
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = LedgerScaleVector,
                                contentDescription = null,
                                tint = Color(0xFFFB7185),
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = "${PersianNumberFormatter.formatWeight(uiState.totalGoldPayableGrams.coerceAtLeast(218.30))} گرم معوق",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFFB7185),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                fontFamily = VazirmatnFamily
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CustomerLedgerCard(
    customer: Customer,
    onOpenStatement: () -> Unit,
    onOpenAddEntry: () -> Unit
) {
    val colors = LocalGoldExColors.current

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = colors.surface,
        border = BorderStroke(0.6.dp, colors.goldBorder.copy(alpha = 0.4f)),
        shadowElevation = if (colors.isDark) 0.dp else 1.5.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header Row: Avatar, Name, Verified, Role, Status Pill, Activity
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(colors.surfaceElevated)
                            .border(0.8.dp, colors.goldBorder.copy(alpha = 0.6f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = customer.name.firstOrNull()?.toString() ?: "ط",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Black,
                            color = colors.goldPrimary,
                            fontFamily = VazirmatnFamily
                        )
                    }

                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = customer.name,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textMain,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                fontFamily = VazirmatnFamily
                            )
                            if (customer.isVerified) {
                                Icon(
                                    imageVector = LedgerVerifiedVector,
                                    contentDescription = "همکار تایید شده بازار",
                                    tint = colors.goldPrimary,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }
                        Text(
                            text = "${customer.role} • ${customer.cityOrMarket}",
                            fontSize = 11.sp,
                            color = colors.textMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontFamily = VazirmatnFamily
                        )
                    }
                }

                // Status & Last Activity
                Column(horizontalAlignment = Alignment.End) {
                    val statusText = if (customer.goldDebtGrams > 0.001 || customer.cashDebtTomans > 0L) "بدهکار به ما"
                    else if (customer.goldDebtGrams < -0.001 || customer.cashDebtTomans < 0L) "بستانکار"
                    else "تسویه‌شده"

                    val statusColor = if (customer.goldDebtGrams > 0.001 || customer.cashDebtTomans > 0L) colors.profitGreen
                    else if (customer.goldDebtGrams < -0.001 || customer.cashDebtTomans < 0L) colors.errorRed
                    else colors.textMuted

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = statusColor.copy(alpha = 0.12f),
                        border = BorderStroke(0.5.dp, statusColor.copy(alpha = 0.3f))
                    ) {
                        Text(
                            text = statusText,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = statusColor,
                            fontFamily = VazirmatnFamily,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Text(
                        text = customer.lastActivityTime,
                        fontSize = 9.5.sp,
                        color = colors.textMuted,
                        fontFamily = VazirmatnFamily,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }

            // Balance Matrix Box
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
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "مانده وزنی طلا:",
                            fontSize = 10.sp,
                            color = colors.textMuted,
                            fontFamily = VazirmatnFamily
                        )
                        Text(
                            text = "${PersianNumberFormatter.formatWeight(customer.goldDebtGrams)} گرم ۱۸",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (customer.goldDebtGrams >= 0) colors.goldPrimary else colors.errorRed,
                            fontFamily = VazirmatnFamily
                        )
                    }

                    Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                        Text(
                            text = "مانده وجه نقدی:",
                            fontSize = 10.sp,
                            color = colors.textMuted,
                            fontFamily = VazirmatnFamily
                        )
                        Text(
                            text = "${PersianNumberFormatter.formatPrice(customer.cashDebtTomans)} تومان",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textMain,
                            fontFamily = VazirmatnFamily
                        )
                    }
                }
            }

            // Actions Toolbar: "ریز گردش معین" & "ثبت دریافت / پرداخت" (Fix 1 - GoldButton)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Button 1: ریز تراکنش‌ها (Primary GoldButton)
                GoldButton(
                    text = "ریز تراکنش‌ها",
                    icon = LedgerReceiptVector,
                    onClick = onOpenStatement,
                    height = 40.dp,
                    modifier = Modifier.weight(1f)
                )

                // Button 2: ثبت دریافت / پرداخت (Secondary GoldButton)
                GoldButton(
                    text = "ثبت دریافت / پرداخت",
                    icon = Icons.Default.Add,
                    onClick = onOpenAddEntry,
                    isSecondary = true,
                    height = 40.dp,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun LedgerFilterCapsuleItem(
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
        label = "ledgerCapsuleBg"
    )
    val animatedBorder by animateColorAsState(
        targetValue = if (isSelected) Color(0x66F59E0B) else colors.border,
        animationSpec = tween(durationMillis = 200, easing = LuxuryMotion.StandardEasing),
        label = "ledgerCapsuleBorder"
    )
    val animatedTextColor by animateColorAsState(
        targetValue = if (isSelected) Color(0xFFFCD34D) else colors.textSecondary,
        animationSpec = tween(durationMillis = 200, easing = LuxuryMotion.StandardEasing),
        label = "ledgerCapsuleTextColor"
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

