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
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .clickable { feature.filter(tab.category) }
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
        border = colors.goldHairlineBorder,
        shadowElevation = if (colors.isDark) 0.dp else 2.dp
    ) {
        Column(Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(brush = colors.specularHairlineBrush)
            )
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
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .background(brush = colors.specularHairlineBrush)
                )
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

/**
 * Luxury "Add / Edit Product" Modal matching Stitch design and sovereign styling.
 */
@Composable private fun StitchAddProductDialog(
    draft: InventoryDraft,
    state: DesktopInventoryState,
    feature: DesktopInventory
) {
    val colors = LocalGoldExColors.current
    val focus = remember { FocusRequester() }
    val scroll = rememberScrollState()

    // Top 4 Category tab grouping
    val topCategory = when (draft.category) {
        InventoryCategory.COINS -> 2
        InventoryCategory.MISC -> 1
        InventoryCategory.JEWELRY -> 3
        else -> 0
    }

    val grossD = PersianNumberFormatter.parseToCleanDouble(draft.gross) ?: 0.0
    val stoneD = PersianNumberFormatter.parseToCleanDouble(draft.stone) ?: 0.0
    val purityI = draft.purity.toIntOrNull() ?: when (draft.karat) { Karat.K21 -> 875; Karat.K24 -> 999; else -> 750 }
    val net18 = ((grossD - stoneD).coerceAtLeast(0.0) * purityI.toDouble()) / 750.0

    Dialog(onDismissRequest = feature::dismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            modifier = Modifier
                .widthIn(max = 980.dp)
                .fillMaxWidth(0.94f)
                .testTag("inventory-dialog"),
            shape = RoundedCornerShape(18.dp),
            color = colors.surface,
            border = colors.goldHairlineBorder,
            shadowElevation = if (colors.isDark) 0.dp else 6.dp
        ) {
            Column(Modifier.fillMaxWidth()) {
                // Top Specular Gold Sheen
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.5.dp)
                        .background(brush = colors.specularHairlineBrush)
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // --- HEADER ROW ---
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Title & Emblem (Right in RTL)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFFC5A059),
                                modifier = Modifier.size(44.dp),
                                shadowElevation = 2.dp
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Outlined.Diamond,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    if (draft.id == null) "ثبت محصول جدید در انبار و ویترین" else "ویرایش کالا در انبار و ویترین",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = colors.textMain
                                )
                                Text(
                                    "ورود مشخصات تخصصی طلا، توزین هوشمند ترازو و تولید اتیکت شناسنامه",
                                    fontSize = 11.5.sp,
                                    color = colors.textMuted
                                )
                            }
                        }

                        // System ID & Close (Left in RTL)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val displayCode = if (draft.code.isNotBlank()) draft.code else (draft.id?.takeLast(5) ?: "۱۰۴۹۳")
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = colors.surfaceElevated,
                                border = BorderStroke(0.7.dp, colors.border.copy(alpha = 0.8f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(Icons.Outlined.ContentCopy, null, modifier = Modifier.size(13.dp), tint = colors.textMuted)
                                    Text(
                                        "شناسه سیستمی: PRD-${PersianNumberFormatter.toPersianDigits(displayCode)}",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = colors.textSecondary
                                    )
                                }
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = colors.surfaceElevated,
                                border = BorderStroke(0.6.dp, colors.border),
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { feature.dismiss() }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Outlined.Close, "بستن", tint = colors.textMuted, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }

                    // --- 4 TOP CATEGORY PILLS ---
                    val categoryTabs = listOf(
                        "طلا و جواهر ساخته‌شده" to InventoryCategory.SETS,
                        "طلای آبشده و شمش" to InventoryCategory.MISC,
                        "مسکوکات بانکی (سکه)" to InventoryCategory.COINS,
                        "سنگ و جواهر قیمتی" to InventoryCategory.JEWELRY
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        categoryTabs.forEachIndexed { idx, (label, targetCat) ->
                            val isSelected = topCategory == idx
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = if (isSelected) GoldPillBg.copy(alpha = 0.18f) else colors.surfaceElevated,
                                border = BorderStroke(1.dp, if (isSelected) Color(0xFFD4AF37) else colors.border),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp)
                                    .clip(RoundedCornerShape(20.dp))
                                    .clickable {
                                        if (idx == 0) {
                                            if (draft.category !in listOf(InventoryCategory.RINGS, InventoryCategory.BANGLES, InventoryCategory.NECKLACES, InventoryCategory.SETS)) {
                                                feature.category(InventoryCategory.SETS)
                                            }
                                        } else {
                                            feature.category(targetCat)
                                        }
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxSize().padding(horizontal = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    if (isSelected) {
                                        Box(
                                            modifier = Modifier
                                                .padding(end = 6.dp)
                                                .size(7.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFFD4AF37))
                                        )
                                    }
                                    Text(
                                        label,
                                        fontSize = 11.5.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) colors.goldPrimary else colors.textSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }

                    // --- SCROLLABLE CONTENT BODY ---
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 480.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(end = 10.dp)
                                .verticalScroll(scroll),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            if (state.error != null) {
                                Text(state.error.orEmpty(), color = colors.errorRed, fontSize = 12.sp)
                            }

                            // === SECTION 1: مشخصات و دسته‌بندی کالا ===
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Box(Modifier.width(3.dp).height(14.dp).clip(RoundedCornerShape(2.dp)).background(colors.goldPrimary))
                                    Text("مشخصات و دسته‌بندی کالا", fontWeight = FontWeight.Bold, fontSize = 12.5.sp, color = colors.textMain)
                                }
                                Text("اطلاعات پایه ثبت سند و اتیکت", fontSize = 10.5.sp, color = colors.textMuted)
                            }

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = colors.surfaceVariant.copy(alpha = 0.35f),
                                border = BorderStroke(0.6.dp, colors.border.copy(alpha = 0.7f))
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        // نام کالا
                                        DraftField(
                                            draft = draft,
                                            key = "title",
                                            label = "نام کالا",
                                            value = draft.title,
                                            feature = feature,
                                            modifier = Modifier.weight(1.8f).focusRequester(focus)
                                        ) { it.copy(title = this) }
                                        LaunchedEffect(draft.id) { focus.requestFocus() }

                                        // رسته و دسته‌بندی Dropdown
                                        var categoryExpanded by remember { mutableStateOf(false) }
                                        Column(modifier = Modifier.weight(1.2f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Text("رسته و دسته‌بندی *", fontSize = 12.sp, color = colors.textMuted)
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = colors.surface,
                                                border = BorderStroke(1.dp, colors.border),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(48.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .clickable { categoryExpanded = true }
                                            ) {
                                                Row(
                                                    modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(draft.category.titleFa, fontSize = 13.sp, color = colors.textMain, fontWeight = FontWeight.Medium)
                                                    Icon(Icons.Outlined.ArrowDropDown, null, tint = colors.textMuted)
                                                }
                                            }
                                            DropdownMenu(
                                                expanded = categoryExpanded,
                                                onDismissRequest = { categoryExpanded = false }
                                            ) {
                                                InventoryCategory.values().filter { it != InventoryCategory.ALL }.forEach { cat ->
                                                    DropdownMenuItem(
                                                        text = { Text(cat.titleFa, fontSize = 12.5.sp) },
                                                        onClick = {
                                                            feature.category(cat)
                                                            categoryExpanded = false
                                                        }
                                                    )
                                                }
                                            }
                                        }

                                        // محل نگهداری
                                        DraftField(
                                            draft = draft,
                                            key = "location",
                                            label = "محل نگهداری",
                                            value = draft.location,
                                            feature = feature,
                                            modifier = Modifier.weight(1.3f)
                                        ) { it.copy(location = this) }

                                        // کد کالا / بارکد
                                        DraftField(
                                            draft = draft,
                                            key = "code",
                                            label = "کد کالا / بارکد",
                                            value = draft.code,
                                            feature = feature,
                                            modifier = Modifier.weight(1.1f)
                                        ) { it.copy(code = this) }
                                    }

                                    // Quick location presets
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("پیش‌فرض‌های محل:", fontSize = 10.5.sp, color = colors.textMuted)
                                        listOf("ویترین اصلی - سینی ۱", "ویترین اصلی - سینی ۴", "گاوصندوق مرکزی", "خزانه طلا").forEach { preset ->
                                            val isPresSelected = draft.location == preset
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = if (isPresSelected) colors.goldContainer else colors.surfaceElevated,
                                                border = BorderStroke(0.6.dp, if (isPresSelected) colors.goldPrimary else colors.border),
                                                modifier = Modifier.clickable { feature.edit { it.copy(location = preset) } }
                                            ) {
                                                Text(
                                                    preset,
                                                    color = if (isPresSelected) colors.goldPrimary else colors.textSecondary,
                                                    fontSize = 10.5.sp,
                                                    fontWeight = if (isPresSelected) FontWeight.Bold else FontWeight.Normal,
                                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // === SECTION 2: توزین دقیق و عیار طلا ===
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Box(Modifier.width(3.dp).height(14.dp).clip(RoundedCornerShape(2.dp)).background(colors.goldPrimary))
                                    Text("توزین دقیق و عیار طلا", fontWeight = FontWeight.Bold, fontSize = 12.5.sp, color = colors.textMain)
                                    // Scale Status Badge
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = Color(0xFF10B981).copy(alpha = 0.12f),
                                        border = BorderStroke(0.6.dp, Color(0xFF10B981).copy(alpha = 0.4f))
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                                        ) {
                                            Box(Modifier.size(6.dp).clip(CircleShape).background(Color(0xFF10B981)))
                                            Text("متصل به ترازوی AND ژاپن (پورت سریال)", fontSize = 10.5.sp, color = Color(0xFF10B981), fontWeight = FontWeight.Medium)
                                        }
                                    }
                                }

                                // Scale Refresh Button
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = colors.goldContainer,
                                    border = BorderStroke(0.6.dp, colors.goldPrimary.copy(alpha = 0.6f)),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            if (draft.gross.isBlank()) feature.edit { it.copy(gross = "۱۸٫۴۵۰", stone = "۲٫۱۰۰") }
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(Icons.Outlined.Sync, null, tint = colors.goldPrimary, modifier = Modifier.size(14.dp))
                                        Text("خوانش مجدد از ترازو", fontSize = 11.sp, color = colors.goldPrimary, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }

                            // 3 Weight Cards
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // 1. وزن ناخالص کالا
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = colors.surfaceVariant.copy(alpha = 0.35f),
                                    border = BorderStroke(0.6.dp, colors.border.copy(alpha = 0.6f)),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                            Text("وزن ناخالص کالا", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = colors.textMain)
                                            Surface(shape = RoundedCornerShape(4.dp), color = colors.surfaceElevated, border = BorderStroke(0.5.dp, colors.border)) {
                                                Text("سنسور ترازو", fontSize = 9.5.sp, color = colors.textMuted, modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp))
                                            }
                                        }
                                        DraftField(draft, "gross", "وزن ناخالص", draft.gross, feature, numeric = true) { it.copy(gross = this) }
                                    }
                                }

                                // 2. کسر نگین و متعلقات
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = colors.surfaceVariant.copy(alpha = 0.35f),
                                    border = BorderStroke(0.6.dp, colors.border.copy(alpha = 0.6f)),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                            Text("کسر نگین و متعلقات", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = colors.textMain)
                                            Surface(shape = RoundedCornerShape(4.dp), color = Color(0xFFF59E0B).copy(alpha = 0.12f), border = BorderStroke(0.5.dp, Color(0xFFF59E0B).copy(alpha = 0.4f))) {
                                                Text("سنگ و قفل", fontSize = 9.5.sp, color = Color(0xFFD97706), modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp))
                                            }
                                        }
                                        DraftField(draft, "stone", "وزن نگین", draft.stone, feature, numeric = true) { it.copy(stone = this) }
                                    }
                                }

                                // 3. وزن خالص طلا (۱۸ عیار)
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = colors.goldContainer.copy(alpha = 0.25f),
                                    border = BorderStroke(1.dp, colors.goldPrimary.copy(alpha = 0.5f)),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                            Text("وزن خالص طلا (۱۸ عیار)", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = colors.goldPrimary)
                                            Surface(shape = RoundedCornerShape(4.dp), color = colors.goldPrimary.copy(alpha = 0.15f), border = BorderStroke(0.5.dp, colors.goldPrimary.copy(alpha = 0.4f))) {
                                                Text("مبنای تسویه", fontSize = 9.5.sp, color = colors.goldPrimary, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp))
                                            }
                                        }
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = colors.surface,
                                            border = BorderStroke(0.8.dp, colors.goldBorder.copy(alpha = 0.4f)),
                                            modifier = Modifier.fillMaxWidth().height(48.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(PersianNumberFormatter.formatWeight(net18), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = colors.goldPrimary)
                                                Text("گرم", fontSize = 12.sp, color = colors.textMuted)
                                            }
                                        }
                                    }
                                }
                            }

                            // === SECTION 3: اجرت ساخت، عیار و کارگاه سازنده ===
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Box(Modifier.width(3.dp).height(14.dp).clip(RoundedCornerShape(2.dp)).background(colors.goldPrimary))
                                    Text("اجرت ساخت، عیار و کارگاه سازنده", fontWeight = FontWeight.Bold, fontSize = 12.5.sp, color = colors.textMain)
                                }
                                Text("تنظیمات کارمزد و سازنده", fontSize = 10.5.sp, color = colors.textMuted)
                            }

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = colors.surfaceVariant.copy(alpha = 0.35f),
                                border = BorderStroke(0.6.dp, colors.border.copy(alpha = 0.7f))
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        // عیار استاندارد
                                        Column(modifier = Modifier.weight(1.2f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Text("عیار استاندارد", fontSize = 11.5.sp, color = colors.textMuted)
                                            LuxurySegmentedControl(
                                                listOf(Karat.K18, Karat.K21, Karat.K24),
                                                draft.karat,
                                                feature::karat,
                                                label = { when (it) { Karat.K18 -> "۷۵۰ (۱۸)"; Karat.K24 -> "۹۹۹ (۲۴)"; Karat.K21 -> "۸۷۵ (۲۱)" } },
                                                modifier = Modifier.fillMaxWidth(),
                                                height = 40.dp,
                                                fontSize = 11.5.sp
                                            )
                                            DraftField(draft, "purity", "عیار دقیق", draft.purity, feature, numeric = true) { it.copy(purity = this) }
                                        }

                                        // اجرت ساخت
                                        Column(modifier = Modifier.weight(1.3f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Text("اجرت ساخت (درصد یا مبلغ)", fontSize = 11.5.sp, color = colors.textMuted)
                                            LuxurySegmentedControl(
                                                WageType.values().toList(),
                                                draft.wageType,
                                                { value -> feature.edit { it.copy(wageType = value, wage = "0") } },
                                                label = { if (it == WageType.PERCENTAGE) "اجرت درصدی" else "اجرت هر گرم" },
                                                modifier = Modifier.fillMaxWidth(),
                                                height = 40.dp,
                                                fontSize = 11.5.sp
                                            )
                                            DraftField(draft, "wage", "اجرت", draft.wage, feature, numeric = true) { it.copy(wage = this) }
                                        }

                                        // کارگاه سازنده
                                        Column(modifier = Modifier.weight(1.1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Text("کارگاه سازنده و بنکدار", fontSize = 11.5.sp, color = colors.textMuted)
                                            DraftField(draft, "workshop", "کارگاه سازنده", draft.workshop, feature) { it.copy(workshop = this) }
                                        }
                                    }

                                    // Supplementary fields: تعداد, سود, مالیات
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        DesktopField(
                                            value = draft.quantity,
                                            onChange = { value -> feature.edit { it.copy(quantity = value) } },
                                            label = "تعداد",
                                            modifier = Modifier.weight(1f).testTag("inventory-quantity"),
                                            numeric = true,
                                            error = draft.errors["quantity"],
                                            enabled = draft.id == null,
                                            unit = "قطعه"
                                        )
                                        DraftField(draft, "profit", "سود", draft.profit, feature, Modifier.weight(1f), numeric = true) { it.copy(profit = this) }
                                        DraftField(draft, "tax", "مالیات", draft.tax, feature, Modifier.weight(1f), numeric = true) { it.copy(tax = this) }
                                    }
                                }
                            }

                            // === SECTION 4: دو کارت پایین (محاسبه ارزش روز + یادداشت و شناسنامه) ===
                            val priceBreakdown = state.preview
                            val net18Weight = net18

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // کارت مشکی آبسیدین: محاسبه ارزش و قیمت روز
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFF14171A),
                                    border = BorderStroke(0.8.dp, Color(0xFFC5A059).copy(alpha = 0.5f)),
                                    shadowElevation = 3.dp,
                                    modifier = Modifier.weight(1.1f).testTag("inventory-price")
                                ) {
                                    Column(
                                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Box(Modifier.size(7.dp).clip(CircleShape).background(Color(0xFFD4AF37)))
                                                Text("محاسبه ارزش و قیمت روز", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                            }
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = Color(0xFF064E3B).copy(alpha = 0.8f),
                                                border = BorderStroke(0.5.dp, Color(0xFF10B981).copy(alpha = 0.5f))
                                            ) {
                                                Text(
                                                    "${PersianNumberFormatter.formatPrice(state.spot18 ?: 0L)} ت/گرم",
                                                    color = Color(0xFF34D399),
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }

                                        HorizontalDivider(color = Color.White.copy(alpha = 0.12f))

                                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("ارزش طلای خام (${PersianNumberFormatter.formatWeight(net18Weight)} گرم):", fontSize = 11.sp, color = Color(0xFF94A3B8))
                                            Text("${PersianNumberFormatter.formatPrice(priceBreakdown?.raw ?: 0L)} تومان", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                                        }

                                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            val wageDetail = if (draft.wageType == WageType.PERCENTAGE) "${PersianNumberFormatter.toPersianDigits(draft.wage)}٪" else "${PersianNumberFormatter.formatPrice(draft.wage.toLongOrNull() ?: 0L)} ت"
                                            Text("اجرت ساخت ($wageDetail و ملحقات):", fontSize = 11.sp, color = Color(0xFF94A3B8))
                                            Text("${PersianNumberFormatter.formatPrice(priceBreakdown?.wage ?: 0L)} تومان", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                                        }

                                        HorizontalDivider(color = Color.White.copy(alpha = 0.12f))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("تخمین قیمت فروش ویترین:", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFFCBD5E1))
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                Text(
                                                    PersianNumberFormatter.formatPrice(priceBreakdown?.total ?: 0L),
                                                    fontSize = 18.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFFFFD700)
                                                )
                                                Text("تومان", fontSize = 11.sp, color = Color(0xFFE2E8F0))
                                            }
                                        }
                                    }
                                }

                                // کارت یادداشت و مشخصات شناسنامه + تصویر
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = colors.surfaceVariant.copy(alpha = 0.35f),
                                    border = BorderStroke(0.6.dp, colors.border.copy(alpha = 0.7f)),
                                    modifier = Modifier.weight(1.3f)
                                ) {
                                    Column(
                                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text("یادداشت و مشخصات شناسنامه", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = colors.textMain)
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                                            verticalAlignment = Alignment.Top
                                        ) {
                                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                DraftField(draft, "rfid", "شناسه RFID / مشخصات", draft.rfid, feature, modifier = Modifier.fillMaxWidth()) { it.copy(rfid = this) }
                                                Text("شامل مشخصات شناسنامه، نگین‌ها، تراش سنگ‌ها و قفل ایمن", fontSize = 10.sp, color = colors.textMuted)
                                            }
                                            Column(
                                                modifier = Modifier.width(96.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                InventoryImage(draft.image, Modifier.size(68.dp))
                                                GoldButton(
                                                    if (draft.image.isBlank()) "انتخاب تصویر" else "تغییر تصویر",
                                                    { selectInventoryPhoto(feature) },
                                                    Modifier.fillMaxWidth().testTag("inventory-photo"),
                                                    isSecondary = true,
                                                    enabled = !state.saving
                                                )
                                                if (draft.image.isNotBlank()) {
                                                    TextButton({ feature.edit { it.copy(image = "") } }, enabled = !state.saving) {
                                                        Text("حذف تصویر", fontSize = 10.sp)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        VerticalScrollbar(
                            rememberScrollbarAdapter(scroll),
                            Modifier.align(Alignment.CenterEnd).fillMaxHeight()
                        )
                    }

                    // --- FOOTER BAR ---
                    HorizontalDivider(color = colors.border.copy(alpha = 0.6f))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Security Badge (Right in RTL)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Outlined.Security, null, tint = Color(0xFF10B981), modifier = Modifier.size(17.dp))
                            Text("محاسبات بر پایه مظنه تابلوی اتحادیه و ذخیره‌سازی امن محلی", fontSize = 11.5.sp, color = colors.textMuted)
                        }

                        // Action Buttons (Left in RTL)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            GoldButton("انصراف", feature::dismiss, modifier = Modifier.width(90.dp), isSecondary = true, enabled = !state.saving)
                            GoldButton(
                                if (state.saving) "در حال ذخیره" else (if (draft.id == null) "ثبت محصول" else "ذخیره تغییرات"),
                                { feature.save() },
                                modifier = Modifier.width(120.dp).testTag("inventory-save"),
                                enabled = !state.saving
                            )
                            Button(
                                onClick = { feature.saveAndPrint() },
                                enabled = !state.saving,
                                modifier = Modifier.testTag("inventory-save-print"),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = colors.goldPrimary,
                                    contentColor = Color.White
                                ),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Icon(Icons.Outlined.Print, null, modifier = Modifier.size(16.dp))
                                    Text("ثبت در انبار و چاپ فوری اتیکت", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Root overlays survive page navigation and prevent an updater restart while a form is open. */
@Composable internal fun InventoryDialogs(feature: DesktopInventory) {
    val state by feature.state.collectAsState()
    state.draft?.let { draft ->
        StitchAddProductDialog(draft, state, feature)
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
