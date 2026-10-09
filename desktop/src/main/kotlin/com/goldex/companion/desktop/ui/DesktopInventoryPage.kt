package com.goldex.companion.desktop.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.selection.selectable
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
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
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
        val wide = maxWidth >= 900.dp
        val masterWidth = (maxWidth * .4f).coerceIn(340.dp, 440.dp)

        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(if (spacious) 12.dp else 8.dp)) {
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
                Surface(color = if (state.error != null) colors.errorRed.copy(alpha = .08f) else colors.goldContainer, shape = RoundedCornerShape(12.dp)) {
                    Row(Modifier.fillMaxWidth().padding(start = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(state.error ?: state.notice.orEmpty(), Modifier.weight(1f).testTag("inventory-message"), color = if (state.error != null) colors.errorRed else colors.textMain, fontSize = 12.sp)
                        IconButton(feature::clearMessage) { Icon(Icons.Outlined.Close, "بستن پیام", tint = colors.textMuted) }
                    }
                }
            }

            // 4. Data Content: Master / Detail
            Box(Modifier.weight(1f).fillMaxWidth()) {
                Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Column((if (wide) Modifier.width(masterWidth) else Modifier.weight(1f)).fillMaxHeight().testTag("inventory-master"), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("${digits(items.size.toLong())} کالا • ${state.category.titleFa}", Modifier.weight(1f), color = colors.textMuted, fontSize = 12.sp)
                        }
                        if (items.isEmpty()) {
                            LuxuryCard(Modifier.fillMaxWidth()) {
                                Icon(Icons.Outlined.Diamond, null, Modifier.size(40.dp), tint = colors.goldPrimary)
                                PageTitle(if (state.items.isEmpty()) "ویترین شما از اینجا شروع می‌شود" else "کالایی پیدا نشد")
                                Text(if (state.items.isEmpty()) "اولین قطعه را با وزن، عیار و اجرت ثبت کنید؛ سپس ورود و خروج آن را در همین انبار مدیریت کنید." else "کد یا نام کوتاه‌تری جست‌وجو کنید یا فیلتر دسته‌بندی را تغییر دهید.", color = colors.textMuted, fontSize = 13.sp)
                                if (state.items.isNotEmpty()) TextButton({ feature.search(""); feature.filter(InventoryCategory.ALL) }) { Text("پاک‌کردن فیلترها") }
                            }
                        } else {
                            val scroll = rememberLazyListStateCompat()
                            Box(Modifier.weight(1f)) {
                                LazyColumn(state = scroll, modifier = Modifier.fillMaxSize().padding(end = 8.dp).testTag("inventory-list"), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    items(items, key = { it.id }) { item ->
                                        InventoryListRow(item, state.selectedId == item.id) { feature.select(item.id) }
                                    }
                                }
                                VerticalScrollbar(rememberScrollbarAdapter(scroll), Modifier.align(Alignment.CenterEnd).fillMaxHeight())
                            }
                        }
                    }
                    if (wide) {
                        Box(Modifier.weight(1f).fillMaxHeight().testTag("inventory-detail-pane")) {
                            if (state.selected != null) {
                                InventoryDetails(state, feature, Modifier.fillMaxSize())
                            } else {
                                LuxuryCard(Modifier.fillMaxWidth()) {
                                    Icon(Icons.Outlined.TouchApp, null, Modifier.size(32.dp), tint = colors.goldPrimary)
                                    PageTitle("جزئیات و گردش کالا")
                                    Text("کالا را انتخاب کنید تا اطلاعات کامل، برآورد قیمت و سوابق آن را ببینید.", color = colors.textMuted, fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }
                if (!wide && state.selected != null && !state.hasDialog) {
                    AlertDialog(onDismissRequest = { feature.select(null) }, modifier = Modifier.width(570.dp),
                        title = { Text("جزئیات کالا") }, text = { InventoryDetails(state, feature, Modifier.heightIn(max = 490.dp)) },
                        confirmButton = { GoldButton("بستن", { feature.select(null) }, isSecondary = true) })
                }
            }

            SourceCaption(workspace)
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
        shape = RoundedCornerShape(if (spacious) 20.dp else 14.dp),
        color = DarkCardBg,
        border = colors.goldHairlineBorder,
        shadowElevation = if (colors.isDark) 0.dp else 2.dp
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(brush = colors.specularHairlineBrush)
            )
            Column(
                Modifier.padding(if (spacious) 14.dp else 8.dp),
                verticalArrangement = Arrangement.spacedBy(if (spacious) 10.dp else 6.dp)
            ) {
            // Header Row
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(if (spacious) 36.dp else 28.dp).background(Color(0xFF222B36), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.Lock, null, tint = GoldAccent, modifier = Modifier.size(if (spacious) 18.dp else 14.dp))
                }
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        "موجودی تجمیعی ویترین و خزانه‌داری مرکزی گالری",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = if (spacious) 15.sp else 12.sp,
                        maxLines = 1
                    )
                    if (spacious) {
                        Text("محاسبه آنلاین بر مبنای آخرین مظنه بازار طلا و ارز", color = SubtitleColor, fontSize = 11.sp)
                    }
                }
                IconButton(
                    onClick = { feature.togglePrivacy() },
                    modifier = Modifier.size(if (spacious) 36.dp else 28.dp).testTag("inventory-privacy"),
                    enabled = !state.saving
                ) {
                    Icon(
                        if (state.visible) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff,
                        "نمایش موجودی",
                        tint = SubtitleColor,
                        modifier = Modifier.size(if (spacious) 20.dp else 16.dp)
                    )
                }
                Spacer(Modifier.width(6.dp))
                Surface(
                    color = Color(0xFF1E2630),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, Color(0xFF2E3845))
                ) {
                    Row(
                        Modifier.padding(horizontal = if (spacious) 12.dp else 8.dp, vertical = if (spacious) 6.dp else 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(Modifier.size(5.dp).background(GoldAccent, CircleShape))
                        Text(
                            "${digits(summary.pieces)} قلم کالای فعال",
                            color = GoldAccent,
                            fontSize = if (spacious) 11.sp else 10.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Two Large Metric Cards
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(if (spacious) 14.dp else 8.dp)) {
                // Right: وزن کل موجودی (استاندارد ۷۵۰)
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(if (spacious) 14.dp else 10.dp),
                    color = DarkMetricCardBg,
                    border = BorderStroke(1.dp, Color(0xFF242F3D))
                ) {
                    Column(
                        Modifier.padding(if (spacious) 16.dp else 8.dp),
                        verticalArrangement = Arrangement.spacedBy(if (spacious) 6.dp else 2.dp)
                    ) {
                        Text("وزن کل موجودی (استاندارد ۷۵۰)", color = SubtitleColor, fontSize = if (spacious) 12.sp else 11.sp, fontWeight = FontWeight.Medium)
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                if (state.visible) PersianNumberFormatter.formatWeight(summary.gold18) else "••••",
                                color = GoldAccent,
                                fontSize = if (spacious) 30.sp else 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.width(5.dp))
                            Text("گرم", color = GoldAccent.copy(alpha = 0.85f), fontSize = if (spacious) 13.sp else 11.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(bottom = 3.dp))
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.AutoMirrored.Outlined.TrendingUp, null, tint = GreenGainColor, modifier = Modifier.size(if (spacious) 15.dp else 13.dp))
                            Text(
                                if (state.visible) "معادل ${PersianNumberFormatter.formatWeight(summary.mesghal)} مثقال ۱۷ عیار" else "••••",
                                color = GreenGainColor,
                                fontSize = if (spacious) 11.sp else 10.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // Left: ارزش کل دارایی بر اساس مظنه زنده بازار
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(if (spacious) 14.dp else 10.dp),
                    color = DarkMetricCardBg,
                    border = BorderStroke(1.dp, Color(0xFF242F3D))
                ) {
                    Column(
                        Modifier.padding(if (spacious) 16.dp else 8.dp),
                        verticalArrangement = Arrangement.spacedBy(if (spacious) 6.dp else 2.dp)
                    ) {
                        Text("ارزش کل دارایی (مظنه زنده بازار)", color = SubtitleColor, fontSize = if (spacious) 12.sp else 11.sp, fontWeight = FontWeight.Medium)
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                if (state.visible) (state.metalValue?.let(PersianNumberFormatter::formatPrice) ?: "—") else "••••",
                                color = Color.White,
                                fontSize = if (spacious) 30.sp else 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.width(5.dp))
                            Text("تومان", color = Color.White.copy(alpha = 0.75f), fontSize = if (spacious) 13.sp else 11.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(bottom = 3.dp))
                        }
                        val mesghalRate = if (state.spot18 > 0) (state.spot18 * 4.3318).toLong() else 0L
                        Text(
                            if (state.visible && state.spot18 > 0) {
                                if (spacious) "مظنه مثقال: ${PersianNumberFormatter.formatPrice(mesghalRate)} تومان   •   گرم ۱۸ عیار: ${PersianNumberFormatter.formatPrice(state.spot18)} تومان"
                                else "گرم ۱۸ عیار: ${PersianNumberFormatter.formatPrice(state.spot18)} تومان"
                            } else "گرم ۱۸ عیار: —",
                            color = SubtitleColor,
                            fontSize = if (spacious) 11.sp else 10.sp
                        )
                    }
                }
            }

            // Four Lower Sub-Metric Cards (spacious only)
            if (spacious) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SubMetricCard(
                        icon = Icons.Outlined.Storefront,
                        iconTint = SubtitleColor,
                        label = "طلا در ویترین",
                        badge = "${digits(showcasePieces)} قلم",
                        badgeBg = Color(0xFF222B36),
                        badgeText = SubtitleColor,
                        value = if (state.visible) "${PersianNumberFormatter.formatWeight(showcaseWeight)} گرم" else "••••",
                        modifier = Modifier.weight(1f)
                    )
                    SubMetricCard(
                        icon = Icons.Outlined.Security,
                        iconTint = GreenGainColor,
                        label = "طلا در گاوصندوق",
                        badge = "${digits(safePieces)} قلم",
                        badgeBg = Color(0xFF173826),
                        badgeText = GreenGainColor,
                        value = if (state.visible) "${PersianNumberFormatter.formatWeight(safeWeight)} گرم" else "••••",
                        modifier = Modifier.weight(1f)
                    )
                    SubMetricCard(
                        icon = Icons.Outlined.Diamond,
                        iconTint = GoldAccent,
                        label = "آبشده و شمش",
                        badge = "${digits(meltedPieces)} قلم",
                        badgeBg = Color(0xFF382F1C),
                        badgeText = GoldAccent,
                        value = if (state.visible) "${PersianNumberFormatter.formatWeight(meltedWeight)} گرم" else "••••",
                        modifier = Modifier.weight(1f)
                    )
                    SubMetricCard(
                        icon = Icons.Outlined.MonetizationOn,
                        iconTint = Color(0xFF38BDF8),
                        label = "سکه و مسکوکات",
                        badge = "بانکی وکیوم",
                        badgeBg = Color(0xFF143045),
                        badgeText = Color(0xFF38BDF8),
                        value = if (state.visible) "${digits(coinsPieces)} قطعه" else "••••",
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}
}

@Composable private fun SubMetricCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    label: String,
    badge: String,
    badgeBg: Color,
    badgeText: Color,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF161E26),
        border = BorderStroke(1.dp, Color(0xFF232D38))
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = iconTint, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(label, color = SubtitleColor, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                Surface(shape = RoundedCornerShape(6.dp), color = badgeBg) {
                    Text(badge, color = badgeText, fontSize = 10.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                }
            }
            Text(value, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        }
    }
}

private data class CategoryTab(val category: InventoryCategory, val label: String, val fullLabel: String)

@Composable private fun CategoryFilterPills(state: DesktopInventoryState, feature: DesktopInventory, compact: Boolean = false) {
    val colors = LocalGoldExColors.current
    val tabs = remember {
        listOf(
            CategoryTab(InventoryCategory.ALL, "همه", "همه اقلام"),
            CategoryTab(InventoryCategory.SETS, "سرویس", "سرویس و نیم‌ست"),
            CategoryTab(InventoryCategory.BANGLES, "النگو", "دستبند و النگو"),
            CategoryTab(InventoryCategory.RINGS, "انگشتر", "انگشتر و حلقه"),
            CategoryTab(InventoryCategory.NECKLACES, "گردنبند", "گردنبند و مدال"),
            CategoryTab(InventoryCategory.MISC, "آبشده", "طلای آبشده و شمش"),
            CategoryTab(InventoryCategory.COINS, "سکه", "مسکوکات بانکی"),
            CategoryTab(InventoryCategory.JEWELRY, "جواهر", "سنگ و جواهر")
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

    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        DesktopField(
            value = query,
            onChange = onSearch,
            label = "جستجوی سریع: نام قطعه، بارکد، کد اتیکت، نام سازنده یا عیار...",
            modifier = Modifier.weight(1f).testTag("inventory-search")
        )

        // Location Dropdown Filter
        Box {
            OutlinedButton(
                onClick = { locationMenuExpanded = true },
                modifier = Modifier.height(if (compact) 40.dp else 48.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(0.6.dp, colors.border),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.textMain),
                contentPadding = PaddingValues(horizontal = if (compact) 10.dp else 14.dp)
            ) {
                Icon(Icons.Outlined.Store, null, modifier = Modifier.size(16.dp), tint = colors.goldPrimary)
                Spacer(Modifier.width(6.dp))
                Text(
                    when (locationFilter) {
                        "ویترین" -> "ویترین‌ها"
                        "گاوصندوق" -> "گاوصندوق‌ها"
                        "خزانه" -> "خزانه"
                        else -> "همه موقعیت‌ها (کل گالری)"
                    },
                    fontSize = if (compact) 11.sp else 12.sp,
                    maxLines = 1
                )
                Spacer(Modifier.width(4.dp))
                Icon(Icons.Outlined.ExpandMore, null, modifier = Modifier.size(14.dp), tint = colors.textMuted)
            }
            DropdownMenu(expanded = locationMenuExpanded, onDismissRequest = { locationMenuExpanded = false }) {
                DropdownMenuItem(text = { Text("همه موقعیت‌ها (کل گالری)") }, onClick = { onLocationFilterChange(null); locationMenuExpanded = false })
                DropdownMenuItem(text = { Text("ویترین‌ها") }, onClick = { onLocationFilterChange("ویترین"); locationMenuExpanded = false })
                DropdownMenuItem(text = { Text("گاوصندوق‌ها") }, onClick = { onLocationFilterChange("گاوصندوق"); locationMenuExpanded = false })
                DropdownMenuItem(text = { Text("خزانه") }, onClick = { onLocationFilterChange("خزانه"); locationMenuExpanded = false })
            }
        }

        // History Icon Button
        OutlinedIconButton(
            onClick = onShowHistory,
            modifier = Modifier.size(if (compact) 40.dp else 48.dp).testTag("inventory-history"),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(0.6.dp, colors.border)
        ) {
            Icon(Icons.Outlined.History, "تاریخچه ورود و خروج", tint = colors.goldPrimary, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable private fun InventoryListRow(item: InventoryItem, selected: Boolean, onSelect: () -> Unit) {
    val colors = LocalGoldExColors.current
    Surface(
        color = if (selected) colors.surfaceElevated else colors.surface,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(if (selected) 1.dp else 0.6.dp, if (selected) colors.goldPrimary else colors.border),
        shadowElevation = if (colors.isDark || !selected) 0.dp else 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected, onClick = onSelect)
            .testTag("inventory-item-${item.id}")
    ) {
        Row(
            Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                Modifier
                    .width(3.dp)
                    .height(38.dp)
                    .background(if (selected) colors.goldPrimary else Color.Transparent, RoundedCornerShape(2.dp))
            )
            InventoryImage(item.imageUrl, Modifier.size(38.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        item.title,
                        Modifier.weight(1f),
                        color = colors.textMain,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (item.quantity == 0) colors.syncWarning.copy(alpha = 0.15f) else Color(0xFF1E2630),
                        border = BorderStroke(0.5.dp, if (item.quantity == 0) colors.syncWarning else Color(0xFF2E3845))
                    ) {
                        Text(
                            "${digits(item.quantity.toLong())} قطعه",
                            color = if (item.quantity == 0) colors.syncWarning else colors.textSecondary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                            maxLines = 1
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        "${PersianNumberFormatter.toPersianDigits(item.code)} • ${item.category.titleFa}",
                        color = colors.textMuted,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.weight(1f))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFFC5A059).copy(alpha = 0.12f),
                        border = BorderStroke(0.6.dp, Color(0xFFC5A059).copy(alpha = 0.45f))
                    ) {
                        Text(
                            "${PersianNumberFormatter.formatWeight(item.netGoldWeightGrams)} گرم",
                            color = colors.goldPrimary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                        )
                    }
                }
                Text(
                    "${digits(item.customKaratValue.toLong())} عیار • ${item.location}",
                    color = colors.textSecondary,
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable private fun rememberLazyListStateCompat() = androidx.compose.foundation.lazy.rememberLazyListState()
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

@Composable private fun InventoryDetails(state: DesktopInventoryState, feature: DesktopInventory, modifier: Modifier) {
    val item = state.selected ?: return
    val colors = LocalGoldExColors.current
    val scroll = rememberScrollState()
    Box(modifier) {
        LuxuryCard(
            modifier = Modifier.fillMaxSize().padding(end = 10.dp).verticalScroll(scroll).testTag("inventory-details"),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                InventoryImage(item.imageUrl, Modifier.size(80.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(item.title, color = colors.textMain, fontSize = 18.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text("${PersianNumberFormatter.toPersianDigits(item.code)} • ${item.category.titleFa}", color = colors.textMuted, fontSize = 12.sp)
                    Text("${digits(item.quantity.toLong())} قطعه • ${item.location}", color = colors.textSecondary, fontSize = 12.sp)
                }
            }
            Text("برآورد فروش هر قطعه", color = colors.textMuted, fontSize = 11.sp)
            Amount(state.selectedPrice?.total?.let { PersianNumberFormatter.formatPrice(it) }, size = 23)
            Text(if (state.selectedPrice == null) "نرخ موجود نیست" else "تومان • بر پایه نرخ انتخابی", color = colors.textMuted, fontSize = 10.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GoldButton("ورود", { feature.openMovement(item) }, Modifier.weight(1f).testTag("inventory-charge"), enabled = !state.saving)
                GoldButton("خروج", { feature.openMovement(item, StockAdjustmentType.DEDUCT) }, Modifier.weight(1f).testTag("inventory-deduct"), isSecondary = true, enabled = !state.saving && item.quantity > 0)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GoldButton("ویرایش", { feature.open(item) }, Modifier.weight(1f).testTag("inventory-edit"), isSecondary = true, enabled = !state.saving)
                IconButton({ feature.requestDelete(item) }, Modifier.testTag("inventory-delete"), enabled = !state.saving) { Icon(Icons.Outlined.DeleteOutline, "حذف کالا", tint = colors.errorRed) }
            }
            HorizontalDivider(color = colors.border)
            InventoryDetailRow("موجودی", "${digits(item.quantity.toLong())} قطعه")
            InventoryDetailRow("ناخالص / نگین", "${PersianNumberFormatter.formatWeight(item.grossWeightGrams)} / ${PersianNumberFormatter.formatWeight(item.stoneWeightGrams)} گرم")
            InventoryDetailRow("خالص هر قطعه", "${PersianNumberFormatter.formatWeight(item.netGoldWeightGrams)} گرم")
            InventoryDetailRow("عیار", digits(item.customKaratValue.toLong()))
            InventoryDetailRow("محل", item.location)
            InventoryDetailRow("کارگاه", item.workshop)
            if (item.rfidTag.isNotBlank()) InventoryDetailRow("RFID", PersianNumberFormatter.toPersianDigits(item.rfidTag))
            InventoryDetailRow("اجرت", if (item.wageType == WageType.PERCENTAGE) "${PersianNumberFormatter.toPersianDigits(item.wageValue.toString())}٪" else "${PersianNumberFormatter.formatPrice(item.wageValue.toLong())} تومان / گرم")
            InventoryDetailRow("سود / مالیات", "${PersianNumberFormatter.toPersianDigits(item.profitPercent.toString())}٪ / ${PersianNumberFormatter.toPersianDigits(item.taxPercent.toString())}٪")
            HorizontalDivider(color = colors.border)
            PricePreview(state.selectedPrice)
            GoldButton("چاپ اتیکت کالا", { feature.printLabel(item) }, Modifier.fillMaxWidth().testTag("inventory-print"), isSecondary = true, icon = Icons.Outlined.Print, enabled = !state.saving)
            val history = state.history.filter { it.itemId == item.id }.sortedByDescending { it.timestamp }
            if (history.isNotEmpty()) {
                Text("آخرین گردش‌ها", color = colors.textMain, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                history.take(3).forEach { MovementRow(it) }
            }
        }
        VerticalScrollbar(rememberScrollbarAdapter(scroll), Modifier.align(Alignment.CenterEnd).fillMaxHeight())
    }
}

@Composable private fun InventoryDetailRow(label: String, value: String) {
    val colors = LocalGoldExColors.current
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
        Text(label, Modifier.width(90.dp), color = colors.textMuted, fontSize = 11.sp)
        Text(value, Modifier.weight(1f), color = colors.textMain, fontSize = 12.sp)
    }
}

@Composable private fun PricePreview(price: InventoryPriceBreakdown?) {
    val colors = LocalGoldExColors.current
    Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.testTag("inventory-price")) {
        Text("برآورد فروش هر قطعه", color = colors.textMain, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        if (price == null) Text("برای برآورد، اطلاعات معتبر و نرخ طلای ۱۸ لازم است.", color = colors.textMuted, fontSize = 11.sp)
        else {
            InventoryDetailRow("طلای خام", "${PersianNumberFormatter.formatPrice(price.raw)} تومان")
            InventoryDetailRow("اجرت", "${PersianNumberFormatter.formatPrice(price.wage)} تومان")
            InventoryDetailRow("سود", "${PersianNumberFormatter.formatPrice(price.profit)} تومان")
            InventoryDetailRow("مالیات", "${PersianNumberFormatter.formatPrice(price.tax)} تومان")
            Amount(PersianNumberFormatter.formatPrice(price.total), size = 22)
            Text("تومان • مالیات فقط بر اجرت و سود", color = colors.textMuted, fontSize = 10.sp)
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
