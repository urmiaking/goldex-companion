package com.goldex.companion.desktop.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.goldex.companion.desktop.data.InventoryPhoto
import com.goldex.companion.desktop.state.*
import com.goldex.companion.model.*
import com.goldex.companion.presentation.inventory.*
import com.goldex.companion.domain.inventory.InventoryPriceBreakdown
import com.goldex.companion.ui.components.*
import com.goldex.companion.ui.theme.*
import java.awt.FileDialog
import java.awt.Frame
import java.nio.file.Path

private val CardBorderColor = Color(0xFF26323F)
private val DarkCardBg = Color(0xFF131920)
private val DarkMetricCardBg = Color(0xFF18202A)
private val GoldAccent = Color(0xFFE5B869)
private val GoldPillBg = Color(0xFFC5A059)
private val SubtitleColor = Color(0xFF9EABB8)
private val GreenGainColor = Color(0xFF4ADE80)

@Composable internal fun DesktopInventoryPage(workspace: WorkspaceState, feature: DesktopInventory) {
    val state by feature.state.collectAsState()
    val colors = LocalGoldExColors.current
    var locationFilter by remember { mutableStateOf<String?>(null) }

    // Additional location filtering if chosen
    val items = remember(state.filtered, locationFilter) {
        if (locationFilter == null) state.filtered
        else state.filtered.filter {
            when (locationFilter) {
                "ویترین" -> it.location.contains("ویترین") || it.location.contains("سینی")
                "گاوصندوق" -> it.location.contains("گاوصندوق") || it.location.contains("انبار")
                "خزانه" -> it.location.contains("خزانه")
                else -> true
            }
        }
    }

    BoxWithConstraints(Modifier.fillMaxSize().testTag("inventory-page")) {
        val spacious = maxWidth >= 950.dp && maxHeight >= 740.dp
        val scrollState = rememberScrollState()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(if (spacious) 14.dp else 10.dp)
        ) {
            // 1. Dark Obsidian Hero Summary Card
            HeroSummaryCard(state, feature, spacious = spacious)

            // 2. Category Filter Pills Row
            CategoryFilterPills(state, feature, compact = !spacious)

            // 3. Search and Filter Bar
            SearchAndFilterBar(
                query = state.query,
                onSearch = feature::search,
                locationFilter = locationFilter,
                onLocationFilterChange = { locationFilter = it },
                onShowHistory = { feature.showHistory(true) },
                compact = !spacious
            )

            if (state.error != null || state.notice != null) {
                Surface(
                    color = if (state.error != null) colors.errorRed.copy(alpha = .08f) else colors.goldContainer,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            state.error ?: state.notice.orEmpty(),
                            Modifier.weight(1f).testTag("inventory-message"),
                            color = if (state.error != null) colors.errorRed else colors.textMain,
                            fontSize = 12.5.sp
                        )
                        IconButton(feature::clearMessage, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Outlined.Close, "بستن پیام", tint = colors.textMuted, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            // 4. Full-width Luxury Data Table (replacing master-detail split)
            InventoryDataTable(
                items = items,
                state = state,
                feature = feature,
                spacious = spacious
            )

            SourceCaption(workspace)
        }

        // 5. Item Details Dialog (when an item is selected from table)
        if (state.selected != null && !state.hasDialog) {
            ItemDetailsDialog(state, feature, onDismiss = { feature.select(null) })
        }
    }
}

@Composable private fun HeroSummaryCard(state: DesktopInventoryState, feature: DesktopInventory, spacious: Boolean) {
    val summary = state.summary
    val allItems = state.items

    val showcaseItems = remember(allItems) { allItems.filter { it.location.contains("ویترین") || it.location.contains("سینی") } }
    val showcaseWeight = remember(showcaseItems) { showcaseItems.sumOf { it.netGoldWeightGrams * it.quantity } }
    val showcasePieces = remember(showcaseItems) { showcaseItems.sumOf { it.quantity.toLong() } }

    val safeItems = remember(allItems) { allItems.filter { it.location.contains("گاوصندوق") || it.location.contains("انبار") || it.location.contains("خزانه") } }
    val safeWeight = remember(safeItems) { safeItems.sumOf { it.netGoldWeightGrams * it.quantity } }
    val safePieces = remember(safeItems) { safeItems.sumOf { it.quantity.toLong() } }

    val meltedItems = remember(allItems) { allItems.filter { it.category == InventoryCategory.MISC || it.title.contains("شمش") || it.title.contains("آبشده") } }
    val meltedWeight = remember(meltedItems) { meltedItems.sumOf { it.netGoldWeightGrams * it.quantity } }
    val meltedPieces = remember(meltedItems) { meltedItems.sumOf { it.quantity.toLong() } }

    val coinItems = remember(allItems) { allItems.filter { it.category == InventoryCategory.COINS || it.title.contains("سکه") } }
    val coinsPieces = remember(coinItems) { coinItems.sumOf { it.quantity.toLong() } }

    val colors = LocalGoldExColors.current

    Surface(
        modifier = Modifier.fillMaxWidth().testTag("inventory-summary"),
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF0F1318),
        border = BorderStroke(1.dp, Color(0xFF1E252E)),
        shadowElevation = if (colors.isDark) 0.dp else 4.dp
    ) {
        Column {
            // Gold specular hairline glow at the very top edge
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.5.dp)
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color(0xFFE5B869).copy(alpha = 0.4f),
                                Color(0xFFFDE68A).copy(alpha = 0.85f),
                                Color(0xFFE5B869).copy(alpha = 0.4f),
                                Color.Transparent
                            )
                        )
                    )
            )
            Column(
                modifier = Modifier.padding(if (spacious) 20.dp else 14.dp),
                verticalArrangement = Arrangement.spacedBy(if (spacious) 16.dp else 12.dp)
            ) {
                // Header Row (RTL: right side is Lock + Title, left side is Pill badge)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Right group: Gold Lock box + Gallery Title & Subtitle
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF241F14),
                            border = BorderStroke(0.8.dp, Color(0xFF4D3E1F)),
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { feature.togglePrivacy() }
                                .testTag("inventory-privacy")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (state.visible) Icons.Outlined.Lock else Icons.Outlined.LockOpen,
                                    contentDescription = "حفظ حریم خصوصی موجودی",
                                    tint = Color(0xFFE5B869),
                                    modifier = Modifier.size(19.dp)
                                )
                            }
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                "موجودی تجمیعی ویترین و خزانه‌داری مرکزی گالری",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = if (spacious) 16.sp else 14.sp,
                                fontFamily = VazirmatnFamily,
                                maxLines = 1
                            )
                            Text(
                                "محاسبه آنلاین بر مبنای آخرین مظنه بازار طلا و ارز",
                                color = Color(0xFF8E9BA8),
                                fontSize = if (spacious) 12.sp else 11.sp,
                                fontFamily = VazirmatnFamily
                            )
                        }
                    }

                    // Left group: Active inventory badge pill with gold dot
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0xFF1E1A12),
                        border = BorderStroke(1.dp, Color(0xFF45361A))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(Color(0xFFE5B869), CircleShape)
                            )
                            Text(
                                "${digits(summary.pieces)} قلم کالای فعال و کدگذاری شده",
                                color = Color(0xFFE5B869),
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = VazirmatnFamily
                            )
                        }
                    }
                }

                // Two Large Metric Cards
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(if (spacious) 14.dp else 10.dp)
                ) {
                    // Right: وزن کل موجودی (استاندارد ۷۵۰)
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFF151A21),
                        border = BorderStroke(1.dp, Color(0xFF202732))
                    ) {
                        Column(
                            modifier = Modifier.padding(if (spacious) 20.dp else 14.dp),
                            verticalArrangement = Arrangement.spacedBy(if (spacious) 10.dp else 6.dp)
                        ) {
                            Text(
                                "وزن کل موجودی (استاندارد ۷۵۰)",
                                color = Color(0xFF9EABB8),
                                fontSize = if (spacious) 13.sp else 11.5.sp,
                                fontWeight = FontWeight.Medium,
                                fontFamily = VazirmatnFamily
                            )
                            Row(
                                verticalAlignment = Alignment.Bottom,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    if (state.visible) PersianNumberFormatter.formatWeight(summary.gold18) else "••••",
                                    color = Color(0xFFF1C77A),
                                    fontSize = if (spacious) 32.sp else 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = VazirmatnFamily
                                )
                                Text(
                                    "گرم",
                                    color = Color(0xFFF1C77A).copy(alpha = 0.85f),
                                    fontSize = if (spacious) 14.sp else 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    fontFamily = VazirmatnFamily,
                                    modifier = Modifier.padding(bottom = 5.dp)
                                )
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Outlined.TrendingUp,
                                    contentDescription = null,
                                    tint = Color(0xFF34D399),
                                    modifier = Modifier.size(if (spacious) 16.dp else 14.dp)
                                )
                                Text(
                                    if (state.visible) "معادل ${PersianNumberFormatter.formatWeight(summary.mesghal)} مثقال طلای آبشده ۱۷ عیار" else "••••",
                                    color = Color(0xFF34D399),
                                    fontSize = if (spacious) 12.sp else 10.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    fontFamily = VazirmatnFamily
                                )
                            }
                        }
                    }

                    // Left: ارزش کل دارایی بر اساس مظنه زنده بازار
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFF151A21),
                        border = BorderStroke(1.dp, Color(0xFF202732))
                    ) {
                        Column(
                            modifier = Modifier.padding(if (spacious) 20.dp else 14.dp),
                            verticalArrangement = Arrangement.spacedBy(if (spacious) 10.dp else 6.dp)
                        ) {
                            Text(
                                "ارزش کل دارایی بر اساس مظنه زنده بازار",
                                color = Color(0xFF9EABB8),
                                fontSize = if (spacious) 13.sp else 11.5.sp,
                                fontWeight = FontWeight.Medium,
                                fontFamily = VazirmatnFamily
                            )
                            Row(
                                verticalAlignment = Alignment.Bottom,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    if (state.visible) (state.metalValue?.let(PersianNumberFormatter::formatPrice) ?: "—") else "••••",
                                    color = Color.White,
                                    fontSize = if (spacious) 32.sp else 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = VazirmatnFamily
                                )
                                Text(
                                    "تومان",
                                    color = Color(0xFF9EABB8),
                                    fontSize = if (spacious) 14.sp else 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    fontFamily = VazirmatnFamily,
                                    modifier = Modifier.padding(bottom = 5.dp)
                                )
                            }
                            val mesghalRate = if (state.spot18 > 0) (state.spot18 * 4.3318).toLong() else 0L
                            Text(
                                if (state.visible && state.spot18 > 0) {
                                    if (spacious) "مظنه مثقال: ${PersianNumberFormatter.formatPrice(mesghalRate)} تومان   •   گرم ۱۸ عیار: ${PersianNumberFormatter.formatPrice(state.spot18)} تومان"
                                    else "گرم ۱۸ عیار: ${PersianNumberFormatter.formatPrice(state.spot18)} تومان"
                                } else "گرم ۱۸ عیار: —",
                                color = Color(0xFF8E9BA8),
                                fontSize = if (spacious) 11.5.sp else 10.sp,
                                fontFamily = VazirmatnFamily
                            )
                        }
                    }
                }

                // Four Lower Sub-Metric Cards
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(if (spacious) 12.dp else 8.dp)
                ) {
                    SubMetricCard(
                        icon = Icons.Outlined.Storefront,
                        iconTint = Color(0xFFE5B869),
                        iconBg = Color(0xFF221D14),
                        iconBorder = Color(0xFF3D3219),
                        label = "طلا در ویترین",
                        badge = "${digits(showcasePieces)} قلم",
                        badgeBg = Color(0xFF241F14),
                        badgeBorder = Color(0xFF42351A),
                        badgeText = Color(0xFFE5B869),
                        value = if (state.visible) "${PersianNumberFormatter.formatWeight(showcaseWeight)} گرم" else "••••",
                        modifier = Modifier.weight(1f)
                    )
                    SubMetricCard(
                        icon = Icons.Outlined.Security,
                        iconTint = Color(0xFF34D399),
                        iconBg = Color(0xFF14241C),
                        iconBorder = Color(0xFF1F4430),
                        label = "طلا در گاوصندوق",
                        badge = "${digits(safePieces)} قلم",
                        badgeBg = Color(0xFF14261D),
                        badgeBorder = Color(0xFF1E4631),
                        badgeText = Color(0xFF34D399),
                        value = if (state.visible) "${PersianNumberFormatter.formatWeight(safeWeight)} گرم" else "••••",
                        modifier = Modifier.weight(1f)
                    )
                    SubMetricCard(
                        icon = Icons.Outlined.Diamond,
                        iconTint = Color(0xFFE5B869),
                        iconBg = Color(0xFF221D14),
                        iconBorder = Color(0xFF3D3219),
                        label = "آبشده و شمش",
                        badge = "${digits(meltedPieces)} قلم",
                        badgeBg = Color(0xFF241F14),
                        badgeBorder = Color(0xFF42351A),
                        badgeText = Color(0xFFE5B869),
                        value = if (state.visible) "${PersianNumberFormatter.formatWeight(meltedWeight)} گرم" else "••••",
                        modifier = Modifier.weight(1f)
                    )
                    SubMetricCard(
                        icon = Icons.Outlined.Adjust,
                        iconTint = Color(0xFF38BDF8),
                        iconBg = Color(0xFF14222C),
                        iconBorder = Color(0xFF1E3A4E),
                        label = "سکه و مسکوکات",
                        badge = "بانکی وکیوم",
                        badgeBg = Color(0xFF132532),
                        badgeBorder = Color(0xFF1A425B),
                        badgeText = Color(0xFF38BDF8),
                        value = if (state.visible) "${digits(coinsPieces)} قطعه" else "••••",
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable private fun SubMetricCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    iconBg: Color,
    iconBorder: Color,
    label: String,
    badge: String,
    badgeBg: Color,
    badgeBorder: Color,
    badgeText: Color,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF13181F),
        border = BorderStroke(1.dp, Color(0xFF1E252E))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Right section (RTL): Icon box + (Label on top, Value on bottom)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(iconBg, RoundedCornerShape(8.dp))
                        .border(0.8.dp, iconBorder, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, null, tint = iconTint, modifier = Modifier.size(17.dp))
                }
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        label,
                        color = Color(0xFF9EABB8),
                        fontSize = 11.sp,
                        fontFamily = VazirmatnFamily,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        value,
                        color = Color.White,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = VazirmatnFamily,
                        maxLines = 1
                    )
                }
            }

            // Left section (RTL): Pill badge
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = badgeBg,
                border = BorderStroke(0.8.dp, badgeBorder)
            ) {
                Text(
                    text = badge,
                    color = badgeText,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = VazirmatnFamily,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.5.dp)
                )
            }
        }
    }
}

private data class CategoryTab(val category: InventoryCategory, val label: String)

@Composable private fun CategoryFilterPills(state: DesktopInventoryState, feature: DesktopInventory, compact: Boolean = false) {
    val colors = LocalGoldExColors.current
    val tabs = remember {
        listOf(
            CategoryTab(InventoryCategory.ALL, "همه"),
            CategoryTab(InventoryCategory.SETS, "سرویس"),
            CategoryTab(InventoryCategory.BANGLES, "النگو"),
            CategoryTab(InventoryCategory.RINGS, "انگشتر"),
            CategoryTab(InventoryCategory.NECKLACES, "گردنبند"),
            CategoryTab(InventoryCategory.MISC, "آبشده و شمش"),
            CategoryTab(InventoryCategory.COINS, "مسکوکات"),
            CategoryTab(InventoryCategory.JEWELRY, "جواهرات")
        )
    }

    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).testTag("inventory-categories"),
        horizontalArrangement = Arrangement.spacedBy(if (compact) 6.dp else 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        tabs.forEach { tab ->
            val selected = state.category == tab.category
            val count = remember(state.items, tab.category) {
                if (tab.category == InventoryCategory.ALL) state.items.sumOf { it.quantity }
                else state.items.filter { it.category == tab.category }.sumOf { it.quantity }
            }

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = if (selected) GoldPillBg else colors.surfaceElevated,
                border = BorderStroke(1.dp, if (selected) Color(0xFFD4AF37) else colors.border),
                modifier = Modifier.clickable { feature.filter(tab.category) }
            ) {
                Row(
                    Modifier.padding(horizontal = if (compact) 10.dp else 14.dp, vertical = if (compact) 5.dp else 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        tab.label,
                        color = if (selected) Color.White else colors.textMain,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        fontSize = if (compact) 11.sp else 12.sp
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "(${digits(count.toLong())})",
                        color = if (selected) Color.White.copy(alpha = 0.9f) else colors.textMuted,
                        fontSize = if (compact) 10.sp else 11.sp
                    )
                }
            }
        }
    }
}

@Composable private fun SearchAndFilterBar(
    query: String,
    onSearch: (String) -> Unit,
    locationFilter: String?,
    onLocationFilterChange: (String?) -> Unit,
    onShowHistory: () -> Unit,
    compact: Boolean = false
) {
    val colors = LocalGoldExColors.current
    var locationMenuExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Search Input Field Box (matching app's clean field design, height 44.dp)
        Surface(
            modifier = Modifier
                .weight(1f)
                .height(44.dp)
                .testTag("inventory-search"),
            shape = RoundedCornerShape(10.dp),
            color = colors.surface,
            border = BorderStroke(1.dp, colors.border)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.Search,
                    contentDescription = null,
                    tint = colors.textMuted,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                    if (query.isEmpty()) {
                        Text(
                            text = "جستجوی سریع: نام قطعه، بارکد، کد اتیکت، نام سازنده یا عیار...",
                            color = colors.textMuted,
                            fontSize = 12.sp,
                            fontFamily = VazirmatnFamily,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    BasicTextField(
                        value = query,
                        onValueChange = onSearch,
                        singleLine = true,
                        textStyle = TextStyle(
                            fontFamily = VazirmatnFamily,
                            fontSize = 13.sp,
                            color = colors.textMain
                        ),
                        cursorBrush = SolidColor(colors.goldPrimary),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                if (query.isNotEmpty()) {
                    IconButton(
                        onClick = { onSearch("") },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = "پاک کردن جستجو",
                            tint = colors.textMuted,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }
        }

        // Location Dropdown Filter (height 44.dp, perfectly aligned)
        Box {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = colors.surface,
                border = BorderStroke(1.dp, if (locationFilter != null) colors.goldPrimary else colors.border),
                modifier = Modifier
                    .height(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { locationMenuExpanded = true }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Store,
                        contentDescription = null,
                        modifier = Modifier.size(17.dp),
                        tint = if (locationFilter != null) colors.goldPrimary else colors.textMuted
                    )
                    Text(
                        text = when (locationFilter) {
                            "ویترین" -> "ویترین‌ها"
                            "گاوصندوق" -> "گاوصندوق‌ها"
                            "خزانه" -> "خزانه مرکزی"
                            else -> "همه موقعیت‌ها (کل گالری)"
                        },
                        fontSize = 12.sp,
                        color = if (locationFilter != null) colors.goldPrimary else colors.textMain,
                        fontWeight = if (locationFilter != null) FontWeight.Bold else FontWeight.Medium,
                        fontFamily = VazirmatnFamily
                    )
                    Icon(
                        imageVector = Icons.Outlined.ExpandMore,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = colors.textMuted
                    )
                }
            }
            DropdownMenu(expanded = locationMenuExpanded, onDismissRequest = { locationMenuExpanded = false }) {
                DropdownMenuItem(
                    text = { Text("همه موقعیت‌ها (کل گالری)", fontFamily = VazirmatnFamily, fontSize = 12.5.sp) },
                    onClick = { onLocationFilterChange(null); locationMenuExpanded = false }
                )
                DropdownMenuItem(
                    text = { Text("ویترین‌ها", fontFamily = VazirmatnFamily, fontSize = 12.5.sp) },
                    onClick = { onLocationFilterChange("ویترین"); locationMenuExpanded = false }
                )
                DropdownMenuItem(
                    text = { Text("گاوصندوق‌ها", fontFamily = VazirmatnFamily, fontSize = 12.5.sp) },
                    onClick = { onLocationFilterChange("گاوصندوق"); locationMenuExpanded = false }
                )
                DropdownMenuItem(
                    text = { Text("خزانه مرکزی", fontFamily = VazirmatnFamily, fontSize = 12.5.sp) },
                    onClick = { onLocationFilterChange("خزانه"); locationMenuExpanded = false }
                )
            }
        }

        // History Button (size 44.dp, perfectly aligned)
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = colors.surface,
            border = BorderStroke(1.dp, colors.border),
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(10.dp))
                .clickable(onClick = onShowHistory)
                .testTag("inventory-history")
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Outlined.History,
                    contentDescription = "تاریخچه ورود و خروج",
                    tint = colors.goldPrimary,
                    modifier = Modifier.size(19.dp)
                )
            }
        }
    }
}

/**
 * Full-width High-density luxury Data Table matching the Stitch visual specification.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TableActionTooltip(
    tooltip: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    TooltipArea(
        tooltip = {
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = Color(0xFF1E252E),
                border = BorderStroke(0.6.dp, Color(0xFF384352)),
                shadowElevation = 4.dp
            ) {
                Text(
                    text = tooltip,
                    color = Color.White,
                    fontSize = 11.sp,
                    fontFamily = VazirmatnFamily,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        },
        delayMillis = 300
    ) {
        IconButton(onClick = onClick, modifier = modifier) {
            content()
        }
    }
}

@Composable private fun InventoryDataTable(
    items: List<InventoryItem>,
    state: DesktopInventoryState,
    feature: DesktopInventory,
    spacious: Boolean
) {
    val colors = LocalGoldExColors.current
    val pageSize = 8
    var currentPage by remember { mutableStateOf(1) }

    val totalPages = remember(items.size) { ((items.size + pageSize - 1) / pageSize).coerceAtLeast(1) }
    LaunchedEffect(items.size) {
        if (currentPage > totalPages) currentPage = totalPages
        if (currentPage < 1) currentPage = 1
    }

    val pageItems = remember(items, currentPage) {
        items.drop((currentPage - 1) * pageSize).take(pageSize)
    }
    val pageWeight = remember(pageItems) {
        pageItems.sumOf { it.netGoldWeightGrams * it.quantity }
    }

    Surface(
        modifier = Modifier.fillMaxWidth().testTag("inventory-table-container"),
        shape = RoundedCornerShape(14.dp),
        color = colors.surface,
        border = BorderStroke(0.8.dp, colors.border),
        shadowElevation = if (colors.isDark) 0.dp else 2.dp
    ) {
        Column(Modifier.fillMaxWidth()) {
            // Table Header Row
            Surface(
                color = colors.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("مشخصات کالا", modifier = Modifier.weight(2.2f), fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = colors.textSecondary)
                    Text("شناسه / RFID", modifier = Modifier.weight(1.1f), fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = colors.textSecondary)
                    Text("موقعیت", modifier = Modifier.weight(1.2f), fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = colors.textSecondary)
                    Text("وزن ناخالص", modifier = Modifier.weight(1.1f), fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = colors.textSecondary)
                    Text("طلای ۱۸", modifier = Modifier.weight(1.0f), fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = colors.textSecondary)
                    Text("عیار", modifier = Modifier.weight(1.0f), fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = colors.textSecondary)
                    Text("اجرت", modifier = Modifier.weight(1.1f), fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = colors.textSecondary)
                    Text("قیمت روز", modifier = Modifier.weight(1.3f), fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = colors.textSecondary)
                    Text("عملیات", modifier = Modifier.weight(1.3f), fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = colors.textSecondary)
                }
            }
            HorizontalDivider(color = colors.border.copy(alpha = 0.6f))

            if (items.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp, horizontal = 20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(Icons.Outlined.Diamond, null, Modifier.size(42.dp), tint = colors.goldPrimary)
                        Text(
                            if (state.items.isEmpty()) "ویترین و انبار شما از اینجا شروع می‌شود" else "کالایی با این مشخصات پیدا نشد",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = colors.textMain
                        )
                        Text(
                            if (state.items.isEmpty()) "اولین قطعه را با استفاده از دکمه «ثبت محصول جدید» اضافه کنید." else "کد، نام یا دسته‌بندی دیگری را جست‌وجو کنید.",
                            color = colors.textMuted,
                            fontSize = 12.5.sp
                        )
                        if (state.items.isNotEmpty()) {
                            TextButton({ feature.search(""); feature.filter(InventoryCategory.ALL) }) {
                                Text("پاک‌کردن فیلترها")
                            }
                        }
                    }
                }
            } else {
                // Table Rows
                Column(Modifier.fillMaxWidth()) {
                    pageItems.forEachIndexed { index, item ->
                        val isSelected = state.selectedId == item.id
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { feature.select(item.id) }
                                .testTag("inventory-item-${item.id}")
                                .background(if (isSelected) colors.goldContainer.copy(alpha = 0.35f) else Color.Transparent)
                                .padding(horizontal = 14.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // مشخصات کالا
                            Row(modifier = Modifier.weight(2.2f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                InventoryImage(item.imageUrl, Modifier.size(42.dp))
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(item.title, fontWeight = FontWeight.Bold, fontSize = 12.5.sp, color = colors.textMain, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(item.workshop.ifBlank { item.category.titleFa }, fontSize = 11.sp, color = colors.textMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }

                            // شناسه / RFID
                            Column(modifier = Modifier.weight(1.1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(PersianNumberFormatter.toPersianDigits(item.code), fontWeight = FontWeight.Bold, fontSize = 12.sp, color = colors.textMain, maxLines = 1)
                                Text(if (item.rfidTag.isNotBlank()) PersianNumberFormatter.toPersianDigits(item.rfidTag) else "RF-${item.code.takeLast(4)}", fontSize = 10.sp, color = colors.textMuted, maxLines = 1)
                            }

                            // موقعیت
                            Box(modifier = Modifier.weight(1.2f)) {
                                val (borderColor, iconVector) = when {
                                    item.location.contains("ویترین") || item.location.contains("سینی") -> Color(0xFFD97706) to Icons.Outlined.Storefront
                                    item.location.contains("گاوصندوق") || item.location.contains("انبار") -> Color(0xFF10B981) to Icons.Outlined.Security
                                    item.location.contains("خزانه") -> Color(0xFF38BDF8) to Icons.Outlined.Shield
                                    else -> colors.border to Icons.Outlined.LocationOn
                                }
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = borderColor.copy(alpha = 0.12f),
                                    border = BorderStroke(0.8.dp, borderColor.copy(alpha = 0.5f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(iconVector, null, tint = borderColor, modifier = Modifier.size(13.dp))
                                        Text(item.location, fontSize = 11.sp, color = colors.textMain, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                }
                            }

                            // وزن ناخالص
                            Column(modifier = Modifier.weight(1.1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text("${PersianNumberFormatter.formatWeight(item.grossWeightGrams)}", fontWeight = FontWeight.Bold, fontSize = 12.5.sp, color = colors.textMain)
                                if (item.stoneWeightGrams > 0) {
                                    Text("${PersianNumberFormatter.formatWeight(item.stoneWeightGrams)}- نگین", fontSize = 10.sp, color = Color(0xFFEF4444))
                                } else {
                                    Text("بدون کسر", fontSize = 10.sp, color = colors.textMuted)
                                }
                            }

                            // طلای ۱۸
                            Box(modifier = Modifier.weight(1.0f)) {
                                Text(PersianNumberFormatter.formatWeight(item.weightIn18kGrams), fontWeight = FontWeight.Bold, fontSize = 12.5.sp, color = colors.textMain)
                            }

                            // عیار
                            Box(modifier = Modifier.weight(1.0f)) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFFC5A059).copy(alpha = 0.15f),
                                    border = BorderStroke(0.6.dp, Color(0xFFC5A059).copy(alpha = 0.45f))
                                ) {
                                    val karatLabel = when (item.customKaratValue) {
                                        750 -> "۱۸ (۷۵۰)"
                                        705 -> "۱۷ (۷۰۵)"
                                        875 -> "۲۱ (۸۷۵)"
                                        900 -> "۲۱.۶ (۹۰۰)"
                                        999, 1000 -> "۲۴ (۹۹۹)"
                                        else -> "${digits(item.customKaratValue.toLong())}"
                                    }
                                    Text(karatLabel, fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = colors.goldPrimary, modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp))
                                }
                            }

                            // اجرت
                            Box(modifier = Modifier.weight(1.1f)) {
                                val wageText = if (item.wageType == WageType.PERCENTAGE) {
                                    "${PersianNumberFormatter.toPersianDigits(item.wageValue.toString())}٪"
                                } else {
                                    "${PersianNumberFormatter.formatPrice(item.wageValue.toLong())} ت"
                                }
                                Text(wageText, fontSize = 11.5.sp, fontWeight = FontWeight.Medium, color = colors.textSecondary)
                            }

                            // قیمت روز
                            Box(modifier = Modifier.weight(1.3f)) {
                                val price = InventoryForm.price(item, state.spot18)?.total
                                Text(
                                    if (price != null && price > 0) "${PersianNumberFormatter.formatPrice(price)}" else "—",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = colors.textMain
                                )
                            }

                            // عملیات با Tooltip
                            Row(
                                modifier = Modifier.weight(1.3f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                TableActionTooltip(tooltip = "چاپ اتیکت", onClick = { feature.printLabel(item) }, modifier = Modifier.size(28.dp)) {
                                    Icon(Icons.Outlined.Print, "چاپ اتیکت", tint = colors.textMuted, modifier = Modifier.size(15.dp))
                                }
                                TableActionTooltip(tooltip = "مشاهده و شناسنامه کالا", onClick = { feature.select(item.id) }, modifier = Modifier.size(28.dp)) {
                                    Icon(Icons.Outlined.Edit, "مشاهده و ویرایش", tint = colors.goldPrimary, modifier = Modifier.size(15.dp))
                                }
                                TableActionTooltip(tooltip = "گردش موجودی (ورود / خروج)", onClick = { feature.openMovement(item) }, modifier = Modifier.size(28.dp)) {
                                    Icon(Icons.Outlined.SwapVert, "گردش موجودی", tint = colors.textSecondary, modifier = Modifier.size(15.dp))
                                }
                                TableActionTooltip(tooltip = "حذف کالا از انبار", onClick = { feature.requestDelete(item) }, modifier = Modifier.size(28.dp)) {
                                    Icon(Icons.Outlined.DeleteOutline, "حذف", tint = colors.errorRed.copy(alpha = 0.8f), modifier = Modifier.size(15.dp))
                                }
                            }
                        }
                        if (index < pageItems.lastIndex) {
                            HorizontalDivider(color = colors.border.copy(alpha = 0.35f))
                        }
                    }
                }

                HorizontalDivider(color = colors.border.copy(alpha = 0.6f))

                // Table Footer with Pagination
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val startIdx = (currentPage - 1) * pageSize + 1
                    val endIdx = (currentPage * pageSize).coerceAtMost(items.size)
                    Text(
                        "نمایش ${digits(startIdx.toLong())} تا ${digits(endIdx.toLong())} از ${digits(items.size.toLong())} قلم موجودی   •   مجموع وزنی اقلام این صفحه: ${PersianNumberFormatter.formatWeight(pageWeight)} گرم",
                        fontSize = 11.5.sp,
                        color = colors.textMuted
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Previous page
                        IconButton(
                            onClick = { if (currentPage > 1) currentPage-- },
                            enabled = currentPage > 1,
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(Icons.Outlined.ChevronRight, "صفحه قبل", tint = if (currentPage > 1) colors.textMain else colors.textMuted.copy(alpha = 0.4f), modifier = Modifier.size(18.dp))
                        }

                        // Numbered page pills
                        (1..totalPages).forEach { pageNum ->
                            val isCurrent = pageNum == currentPage
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (isCurrent) GoldPillBg else Color.Transparent,
                                border = if (isCurrent) null else BorderStroke(0.6.dp, colors.border.copy(alpha = 0.5f)),
                                modifier = Modifier
                                    .size(28.dp)
                                    .clickable { currentPage = pageNum }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        digits(pageNum.toLong()),
                                        fontSize = 11.sp,
                                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isCurrent) Color.White else colors.textSecondary
                                    )
                                }
                            }
                        }

                        // Next page
                        IconButton(
                            onClick = { if (currentPage < totalPages) currentPage++ },
                            enabled = currentPage < totalPages,
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(Icons.Outlined.ChevronLeft, "صفحه بعد", tint = if (currentPage < totalPages) colors.textMain else colors.textMuted.copy(alpha = 0.4f), modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }
}

/**
 * Dedicated Modal Dialog for Item Details & Actions (as requested by user instead of master-detail).
 */
@Composable private fun ItemDetailsDialog(
    state: DesktopInventoryState,
    feature: DesktopInventory,
    onDismiss: () -> Unit
) {
    val item = state.selected ?: return
    val colors = LocalGoldExColors.current
    val scroll = rememberScrollState()

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            modifier = Modifier.width(660.dp).testTag("inventory-details-dialog"),
            shape = RoundedCornerShape(16.dp),
            color = colors.surface,
            border = BorderStroke(0.8.dp, colors.goldBorder.copy(alpha = 0.5f)),
            shadowElevation = if (colors.isDark) 0.dp else 4.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp)
                    .testTag("inventory-details-container"),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(
                            modifier = Modifier.size(36.dp).background(colors.goldContainer, RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Outlined.Diamond, null, tint = colors.goldPrimary, modifier = Modifier.size(20.dp))
                        }
                        Column {
                            Text("شناسنامه و جزئیات کالا", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = colors.textMain)
                            Text(item.title, fontSize = 12.sp, color = colors.textSecondary)
                        }
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.testTag("close-details")) {
                        Icon(Icons.Outlined.Close, "بستن", tint = colors.textMuted)
                    }
                }

                HorizontalDivider(color = colors.border)

                // Body (Scrollable)
                Box(Modifier.fillMaxWidth().heightIn(max = 480.dp)) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(end = 8.dp)
                            .verticalScroll(scroll)
                            .testTag("inventory-details"),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Quick Identity Card
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = colors.surfaceElevated,
                            border = BorderStroke(0.6.dp, colors.border)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                InventoryImage(item.imageUrl, Modifier.size(76.dp))
                                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(item.title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = colors.textMain)
                                    Text("کد کالا: ${PersianNumberFormatter.toPersianDigits(item.code)}   •   دسته: ${item.category.titleFa}", fontSize = 12.sp, color = colors.textMuted)
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Surface(shape = RoundedCornerShape(6.dp), color = colors.goldContainer) {
                                            Text("${digits(item.quantity.toLong())} قطعه موجود", color = colors.goldPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                        }
                                        Text("محل: ${item.location}", fontSize = 11.5.sp, color = colors.textSecondary)
                                    }
                                }
                            }
                        }

                        // Quick Location Transfer Card (انتقال بین انبارها)
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = colors.surfaceElevated,
                            border = BorderStroke(0.6.dp, colors.border)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Icon(Icons.Outlined.SwapHoriz, null, tint = colors.goldPrimary, modifier = Modifier.size(17.dp))
                                    Text("انتقال بین انبارها:", fontSize = 12.sp, color = colors.textMuted, fontFamily = VazirmatnFamily)
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    listOf("ویترین اصلی", "گاوصندوق مرکزی", "خزانه طلا").forEach { targetLoc ->
                                        val isCurrent = item.location.contains(targetLoc.take(5))
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = if (isCurrent) colors.goldContainer else colors.surface,
                                            border = BorderStroke(0.8.dp, if (isCurrent) colors.goldPrimary else colors.border),
                                            modifier = Modifier.then(
                                                if (!isCurrent) Modifier.clickable {
                                                    feature.open(item)
                                                    feature.edit { it.copy(location = targetLoc) }
                                                    feature.save()
                                                } else Modifier
                                            )
                                        ) {
                                            Text(
                                                when {
                                                    targetLoc.contains("ویترین") -> "به ویترین"
                                                    targetLoc.contains("گاوصندوق") -> "به گاوصندوق"
                                                    else -> "به خزانه"
                                                },
                                                fontSize = 11.sp,
                                                color = if (isCurrent) colors.goldPrimary else colors.textMain,
                                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                                                fontFamily = VazirmatnFamily,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Price Preview Card (inventory-price)
                        PricePreview(state.selectedPrice)

                        // Action Buttons: ورود و خروج
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            GoldButton("ورود موجودی", { feature.openMovement(item, StockAdjustmentType.CHARGE) }, Modifier.weight(1f).testTag("inventory-charge"), enabled = !state.saving)
                            GoldButton("خروج موجودی", { feature.openMovement(item, StockAdjustmentType.DEDUCT) }, Modifier.weight(1f).testTag("inventory-deduct"), isSecondary = true, enabled = !state.saving && item.quantity > 0)
                        }

                        // Action Buttons: ویرایش، چاپ، حذف
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            GoldButton("ویرایش مشخصات", { feature.open(item) }, Modifier.weight(1f).testTag("inventory-edit"), isSecondary = true, enabled = !state.saving)
                            GoldButton("چاپ اتیکت", { feature.printLabel(item) }, Modifier.weight(1f).testTag("inventory-print"), isSecondary = true, icon = Icons.Outlined.Print, enabled = !state.saving)
                            IconButton(onClick = { feature.requestDelete(item) }, modifier = Modifier.testTag("inventory-delete"), enabled = !state.saving) {
                                Icon(Icons.Outlined.DeleteOutline, "حذف کالا", tint = colors.errorRed)
                            }
                        }

                        // Specifications Card
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = colors.surfaceElevated,
                            border = BorderStroke(0.6.dp, colors.border)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("مشخصات فنی و وزنی", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = colors.textMain)
                                HorizontalDivider(color = colors.border.copy(alpha = 0.5f))
                                InventoryDetailRow("موجودی", "${digits(item.quantity.toLong())} قطعه")
                                InventoryDetailRow("ناخالص / نگین", "${PersianNumberFormatter.formatWeight(item.grossWeightGrams)} / ${PersianNumberFormatter.formatWeight(item.stoneWeightGrams)} گرم")
                                InventoryDetailRow("خالص هر قطعه", "${PersianNumberFormatter.formatWeight(item.netGoldWeightGrams)} گرم")
                                InventoryDetailRow("معادل ۱۸ عیار", "${PersianNumberFormatter.formatWeight(item.weightIn18kGrams)} گرم")
                                InventoryDetailRow("عیار", digits(item.customKaratValue.toLong()))
                                InventoryDetailRow("محل نگهداری", item.location)
                                if (item.workshop.isNotBlank()) InventoryDetailRow("کارگاه سازنده", item.workshop)
                                if (item.rfidTag.isNotBlank()) InventoryDetailRow("RFID", PersianNumberFormatter.toPersianDigits(item.rfidTag))
                                InventoryDetailRow("اجرت", if (item.wageType == WageType.PERCENTAGE) "${PersianNumberFormatter.toPersianDigits(item.wageValue.toString())}٪" else "${PersianNumberFormatter.formatPrice(item.wageValue.toLong())} تومان / گرم")
                                InventoryDetailRow("سود / مالیات", "${PersianNumberFormatter.toPersianDigits(item.profitPercent.toString())}٪ / ${PersianNumberFormatter.toPersianDigits(item.taxPercent.toString())}٪")
                            }
                        }

                        // Recent Movements
                        val history = state.history.filter { it.itemId == item.id }.sortedByDescending { it.timestamp }
                        if (history.isNotEmpty()) {
                            Text("آخرین گردش‌های این کالا", color = colors.textMain, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            history.take(3).forEach { MovementRow(it) }
                        }
                    }
                    VerticalScrollbar(rememberScrollbarAdapter(scroll), Modifier.align(Alignment.CenterEnd).fillMaxHeight())
                }

                HorizontalDivider(color = colors.border)

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    GoldButton("بستن پنجره", onDismiss, modifier = Modifier.width(130.dp), isSecondary = true)
                }
            }
        }
    }
}

private fun digits(value: Long) = PersianNumberFormatter.toPersianDigits(value.toString())
private fun categoryShort(value: InventoryCategory) = when(value) {
    InventoryCategory.ALL -> "همه"; InventoryCategory.RINGS -> "انگشتر"; InventoryCategory.BANGLES -> "النگو"; InventoryCategory.NECKLACES -> "زنجیر"
    InventoryCategory.SETS -> "سرویس"; InventoryCategory.JEWELRY -> "جواهر"; InventoryCategory.COINS -> "سکه"; InventoryCategory.MISC -> "متفرقه"
}

@Composable private fun InventoryImage(value: String, modifier: Modifier) {
    val colors = LocalGoldExColors.current
    val bitmap = remember(value) { if (value.isBlank()) null else runCatching { org.jetbrains.skia.Image.makeFromEncoded(InventoryPhoto.bytes(value)).use { it.toComposeImageBitmap() } }.getOrNull() }
    Box(modifier.clip(RoundedCornerShape(10.dp)).background(colors.surfaceVariant), contentAlignment = Alignment.Center) {
        if (bitmap == null) Icon(Icons.Outlined.Diamond, "بدون تصویر کالا", Modifier.size(24.dp), tint = colors.goldPrimary)
        else Image(bitmap, "تصویر کالا", Modifier.fillMaxSize(), contentScale = androidx.compose.ui.layout.ContentScale.Crop)
    }
}

@Composable private fun InventoryDetailRow(label: String, value: String) {
    val colors = LocalGoldExColors.current
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
        Text(label, Modifier.width(110.dp), color = colors.textMuted, fontSize = 11.5.sp)
        Text(value, Modifier.weight(1f), color = colors.textMain, fontSize = 12.sp)
    }
}

@Composable private fun PricePreview(price: InventoryPriceBreakdown?) {
    val colors = LocalGoldExColors.current
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = colors.surfaceElevated,
        border = BorderStroke(0.6.dp, colors.goldBorder.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
                .testTag("inventory-price"),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("برآورد فروش هر قطعه", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = colors.textMain)
                Text("تومان • محاسبه بر مبنای مظنه زنده", fontSize = 10.5.sp, color = colors.textMuted)
            }
            HorizontalDivider(color = colors.border.copy(alpha = 0.5f))
            if (price == null) {
                Text("برای برآورد، اطلاعات معتبر و نرخ طلای ۱۸ لازم است.", color = colors.textMuted, fontSize = 11.sp)
            } else {
                InventoryDetailRow("طلای خام", "${PersianNumberFormatter.formatPrice(price.raw)} تومان")
                InventoryDetailRow("اجرت", "${PersianNumberFormatter.formatPrice(price.wage)} تومان")
                InventoryDetailRow("سود", "${PersianNumberFormatter.formatPrice(price.profit)} تومان")
                InventoryDetailRow("مالیات (بر اجرت و سود)", "${PersianNumberFormatter.formatPrice(price.tax)} تومان")
                HorizontalDivider(color = colors.border.copy(alpha = 0.5f))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("قیمت کل برآوردی:", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = colors.textMain)
                    Amount(PersianNumberFormatter.formatPrice(price.total), size = 20)
                }
            }
        }
    }
}

/** Root overlays survive page navigation and prevent an updater restart while a form is open. */
@Composable internal fun InventoryDialogs(feature: DesktopInventory) {
    val state by feature.state.collectAsState()
    state.draft?.let { draft ->
        val focus = remember { FocusRequester() }
        InventoryDialog(if (draft.id == null) "ثبت کالای جدید" else "ویرایش کالا", "inventory-dialog", state.saving, feature::dismiss, { feature.save() }, "ذخیره کالا", wide = true) {
            if (state.error != null) Text(state.error.orEmpty(), color = LocalGoldExColors.current.errorRed, fontSize = 12.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    PageTitle("شناسنامه کالا")
                    DraftField(draft, "title", "نام کالا", draft.title, feature, Modifier.focusRequester(focus)) { it.copy(title = this) }
                    LaunchedEffect(draft.id) { focus.requestFocus() }
                    DraftField(draft, "code", "کد کالا / بارکد", draft.code, feature) { it.copy(code = this) }
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("دسته‌بندی • ${draft.category.titleFa}", color = LocalGoldExColors.current.textMuted, fontSize = 11.sp)
                        LuxurySegmentedControl(InventoryCategory.values().filter { it != InventoryCategory.ALL }, draft.category, feature::category,
                            label = ::categoryShort, modifier = Modifier.fillMaxWidth(), height = 42.dp, fontSize = 11.sp)
                    }
                    DraftField(draft, "location", "محل نگهداری", draft.location, feature) { it.copy(location = this) }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("پیش‌فرض‌های محل:", fontSize = 11.sp, color = LocalGoldExColors.current.textMuted)
                        listOf("ویترین اصلی", "گاوصندوق مرکزی", "خزانه طلا").forEach { preset ->
                            val selected = draft.location == preset
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (selected) LocalGoldExColors.current.goldContainer else LocalGoldExColors.current.surfaceElevated,
                                border = BorderStroke(0.6.dp, if (selected) LocalGoldExColors.current.goldPrimary else LocalGoldExColors.current.border),
                                modifier = Modifier.clickable { feature.edit { it.copy(location = preset) } }
                            ) {
                                Text(
                                    preset,
                                    color = if (selected) LocalGoldExColors.current.goldPrimary else LocalGoldExColors.current.textSecondary,
                                    fontSize = 11.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                    DraftField(draft, "workshop", "کارگاه سازنده", draft.workshop, feature) { it.copy(workshop = this) }
                    DraftField(draft, "rfid", "شناسه RFID (اختیاری)", draft.rfid, feature) { it.copy(rfid = this) }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        InventoryImage(draft.image, Modifier.size(70.dp))
                        Column(Modifier.weight(1f)) {
                            GoldButton("انتخاب تصویر", { selectInventoryPhoto(feature) }, Modifier.fillMaxWidth().testTag("inventory-photo"), isSecondary = true, enabled = !state.saving)
                            if (draft.image.isNotBlank()) TextButton({ feature.edit { it.copy(image = "") } }, enabled = !state.saving) { Text("حذف تصویر") }
                        }
                    }
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    PageTitle("وزن و قیمت‌گذاری")
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        DraftField(draft, "gross", "وزن ناخالص", draft.gross, feature, Modifier.weight(1f), true) { it.copy(gross = this) }
                        DraftField(draft, "stone", "وزن نگین", draft.stone, feature, Modifier.weight(1f), true) { it.copy(stone = this) }
                    }
                    LuxurySegmentedControl(Karat.values().toList(), draft.karat, feature::karat, label = { it.labelFa.substringBefore(" (") }, modifier = Modifier.fillMaxWidth(), height = 40.dp, fontSize = 12.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        DraftField(draft, "purity", "عیار دقیق", draft.purity, feature, Modifier.weight(1f), true) { it.copy(purity = this) }
                        DesktopField(draft.quantity, { value -> feature.edit { it.copy(quantity = value) } }, "تعداد", Modifier.weight(1f).testTag("inventory-quantity"), numeric = true, error = draft.errors["quantity"], enabled = draft.id == null, unit = "قطعه")
                    }
                    LuxurySegmentedControl(WageType.values().toList(), draft.wageType, { value -> feature.edit { it.copy(wageType = value, wage = "0") } },
                        label = { if (it == WageType.PERCENTAGE) "اجرت درصدی" else "اجرت هر گرم" }, modifier = Modifier.fillMaxWidth(), height = 40.dp, fontSize = 12.sp)
                    DraftField(draft, "wage", "اجرت", draft.wage, feature, numeric = true) { it.copy(wage = this) }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        DraftField(draft, "profit", "سود", draft.profit, feature, Modifier.weight(1f), true) { it.copy(profit = this) }
                        DraftField(draft, "tax", "مالیات", draft.tax, feature, Modifier.weight(1f), true) { it.copy(tax = this) }
                    }
                    PricePreview(state.preview)
                    Text("وزن و قیمت برای یک قطعه‌اند. در دسته سکه سود صفر محاسبه می‌شود.", color = LocalGoldExColors.current.textMuted, fontSize = 11.sp)
                    if (draft.id != null) Text("برای تغییر تعداد از ورود و خروج کالا استفاده کنید تا سابقه ثبت شود.", color = LocalGoldExColors.current.textMuted, fontSize = 11.sp)
                }
            }
        }
    }
    state.movement?.let { draft ->
        val item = state.items.firstOrNull { it.id == draft.itemId } ?: return@let
        InventoryDialog("ورود و خروج • ${item.title}", "inventory-movement-dialog", state.saving, feature::dismiss, { feature.saveMovement() }, "ثبت گردش") {
            if (state.error != null) Text(state.error.orEmpty(), color = LocalGoldExColors.current.errorRed, fontSize = 12.sp)
            LuxurySegmentedControl(StockAdjustmentType.values().toList(), draft.type, { type -> feature.editMovement { it.copy(type = type) } }, label = { if (it == StockAdjustmentType.CHARGE) "ورود موجودی" else "خروج موجودی" }, modifier = Modifier.fillMaxWidth(), height = 42.dp)
            Text("موجودی فعلی: ${digits(item.quantity.toLong())} قطعه • ${PersianNumberFormatter.formatWeight(item.netGoldWeightGrams * item.quantity)} گرم خالص", color = LocalGoldExColors.current.textSecondary, fontSize = 13.sp)
            DesktopField(draft.count, { value -> feature.editMovement { it.copy(count = value) } }, "تعداد ورود / خروج", Modifier.testTag("movement-count"), numeric = true, error = draft.errors["count"], unit = "قطعه")
            DesktopField(draft.weight, { value -> feature.editMovement { it.copy(weight = value) } }, "وزن سند (اختیاری)", Modifier.testTag("movement-weight"), numeric = true, error = draft.errors["weight"], unit = "گرم")
            Text("اگر وزن را وارد نکنید، وزن خالص کالا × تعداد ثبت می‌شود. این وزن سند است و وزن هر قطعه را تغییر نمی‌دهد.", color = LocalGoldExColors.current.textMuted, fontSize = 11.sp)
            DesktopField(draft.reason, { value -> feature.editMovement { it.copy(reason = value) } }, "دلیل ورود / خروج", Modifier.testTag("movement-reason"), error = draft.errors["reason"])
            DesktopField(draft.note, { value -> feature.editMovement { it.copy(note = value) } }, "یادداشت (اختیاری)", error = draft.errors["note"], singleLine = false)
            feature.adjustedPreview()?.let { (count, weight) -> LuxuryCard(Modifier.fillMaxWidth()) {
                Text("موجودی پس از ثبت", color = LocalGoldExColors.current.textMuted, fontSize = 12.sp)
                Text("${digits(count.toLong())} قطعه • ${PersianNumberFormatter.formatWeight(weight)} گرم خالص", color = LocalGoldExColors.current.goldPrimary, fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
            } }
        }
    }
    state.pendingDelete?.let { item ->
        AlertDialog(onDismissRequest = { feature.requestDelete(null) }, title = { Text("حذف کالا") }, text = { Text("«${item.title}» با ${digits(item.quantity.toLong())} قطعه از انبار حذف شود؟ سوابق ورود و خروج حفظ می‌شوند و فایل قبلی در پشتیبان محلی می‌ماند.") },
            confirmButton = { DialogActions(state.saving, { feature.requestDelete(null) }, { feature.delete() }, "حذف کالا", "inventory-confirm-delete") })
    }
    if (state.showHistory) AlertDialog(onDismissRequest = { feature.showHistory(false) }, modifier = Modifier.width(780.dp), title = { Text("تاریخچه ورود و خروج انبار") },
        text = {
            if (state.history.isEmpty()) Text("هنوز گردش موجودی ثبت نشده است؛ از جزئیات کالا ورود یا خروج را انتخاب کنید.")
            else LazyColumn(Modifier.heightIn(max = 500.dp).testTag("inventory-history-list"), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(state.history.sortedByDescending { it.timestamp }, key = { it.id }) { MovementRow(it, full = true) }
            }
        }, confirmButton = { GoldButton("بستن", { feature.showHistory(false) }, isSecondary = true) })
    if (state.discard) AlertDialog(onDismissRequest = feature::cancelDiscard, title = { Text("بستن بدون ذخیره؟") }, text = { Text("تغییرات این فرم ذخیره نشده‌اند. برای ادامه ویرایش، به فرم برگردید.") },
        confirmButton = { DialogActions(false, feature::cancelDiscard, feature::discard, "بستن بدون ذخیره", "inventory-discard", "ادامه ویرایش") })
}

@Composable private fun MovementRow(movement: StockAdjustment, full: Boolean = false) {
    val colors = LocalGoldExColors.current
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(if (movement.type == StockAdjustmentType.CHARGE) Icons.Outlined.SouthWest else Icons.Outlined.NorthEast, null, Modifier.size(18.dp), tint = if (movement.type == StockAdjustmentType.CHARGE) colors.profitGreen else colors.syncWarning)
            Text("${if (movement.type == StockAdjustmentType.CHARGE) "ورود" else "خروج"} ${digits(movement.quantityChange.toLong())} قطعه${if (full) " • ${movement.itemTitle}" else ""}", Modifier.weight(1f), color = colors.textMain, fontSize = 12.sp)
        }
        Text("${PersianNumberFormatter.formatWeight(movement.weightGrams)} گرم • ${movement.reason}", color = colors.textMuted, fontSize = 11.sp)
        Text(DesktopPortfolioPolicy.observedTime(movement.timestamp), color = colors.textMuted, fontSize = 10.sp)
        if (full && movement.note.isNotBlank()) Text(movement.note, color = colors.textSecondary, fontSize = 12.sp)
        HorizontalDivider(color = colors.border)
    }
}

@Composable private fun DraftField(draft: InventoryDraft, key: String, label: String, value: String, feature: DesktopInventory, modifier: Modifier = Modifier, numeric: Boolean = false, change: String.(InventoryDraft) -> InventoryDraft) {
    DesktopField(value, { text -> feature.edit { text.change(it) } }, label, modifier.testTag("inventory-$key"), numeric, draft.errors[key],
        monetary = key == "wage" && draft.wageType == WageType.TOMAN_PER_GRAM,
        unit = when (key) { "gross", "stone" -> "گرم"; "profit", "tax" -> "٪"; "wage" -> if (draft.wageType == WageType.PERCENTAGE) "٪" else "تومان / گرم"; "purity" -> "از ۱۰۰۰"; else -> null })
}

@Composable private fun InventoryDialog(title: String, tag: String, busy: Boolean, cancel: () -> Unit, save: () -> Unit, saveLabel: String, wide: Boolean = false, content: @Composable ColumnScope.() -> Unit) {
    val colors = LocalGoldExColors.current
    Dialog(onDismissRequest = cancel, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(modifier = Modifier.width(if (wide) 930.dp else 610.dp).testTag(tag), shape = RoundedCornerShape(16.dp), color = colors.surface,
            border = BorderStroke(.6.dp, colors.goldBorder.copy(alpha = .5f)), shadowElevation = if (colors.isDark) 0.dp else 2.dp) {
            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                Text(title, style = MaterialTheme.typography.titleLarge, color = colors.textMain, fontWeight = FontWeight.Bold)
                val scroll = rememberScrollState()
                Box(Modifier.fillMaxWidth().heightIn(max = 500.dp)) {
                    Column(Modifier.fillMaxWidth().padding(end = 12.dp).verticalScroll(scroll), verticalArrangement = Arrangement.spacedBy(14.dp), content = content)
                    VerticalScrollbar(rememberScrollbarAdapter(scroll), Modifier.align(Alignment.CenterEnd).fillMaxHeight())
                }
                HorizontalDivider(color = colors.border)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) { DialogActions(busy, cancel, save, saveLabel, if (wide) "inventory-save" else "movement-save") }
            }
        }
    }
}

@Composable private fun DialogActions(busy: Boolean, cancel: () -> Unit, save: () -> Unit, label: String, tag: String, cancelLabel: String = "انصراف") {
    Row(Modifier.width(380.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        GoldButton(cancelLabel, cancel, Modifier.weight(1f), isSecondary = true, enabled = !busy)
        GoldButton(if (busy) "در حال ذخیره" else label, save, Modifier.weight(1.4f).testTag(tag), enabled = !busy)
    }
}

private fun selectInventoryPhoto(feature: DesktopInventory) {
    FileDialog(null as Frame?, "انتخاب تصویر کالا (PNG / JPEG)", FileDialog.LOAD).apply {
        isVisible = true
        if (file != null && directory != null) feature.photo(Path.of(directory, file))
        dispose()
    }
}
