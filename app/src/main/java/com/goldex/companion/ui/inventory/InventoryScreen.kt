package com.goldex.companion.ui.inventory

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.goldex.companion.data.MarketRates
import com.goldex.companion.model.InventoryCategory
import com.goldex.companion.model.InventoryItem
import com.goldex.companion.model.PersianNumberFormatter
import com.goldex.companion.model.StockAdjustment
import com.goldex.companion.model.WageType
import com.goldex.companion.ui.components.AnimatedPriceText
import com.goldex.companion.ui.components.GoldButton
import com.goldex.companion.ui.components.LuxuryCard
import com.goldex.companion.ui.components.QiratoToast
import com.goldex.companion.ui.hub.HubArrowRight
import com.goldex.companion.ui.hub.HubShowcase
import com.goldex.companion.ui.inventory.modals.AddInventoryItemModal
import com.goldex.companion.ui.inventory.modals.AdjustStockModal
import com.goldex.companion.ui.theme.ButtonShape
import com.goldex.companion.ui.theme.LocalGoldExColors
import com.goldex.companion.ui.theme.LuxuryMotion
import com.goldex.companion.ui.theme.VazirmatnFamily

private val InventoryPrintVector: ImageVector by lazy {
    ImageVector.Builder(
        name = "InventoryPrint",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(fill = SolidColor(Color.White)) {
            moveTo(19f, 8f)
            horizontalLineTo(5f)
            curveTo(3.34f, 8f, 2f, 9.34f, 2f, 11f)
            verticalLineTo(17f)
            horizontalLineTo(6f)
            verticalLineTo(21f)
            horizontalLineTo(18f)
            verticalLineTo(17f)
            horizontalLineTo(22f)
            verticalLineTo(11f)
            curveTo(22f, 9.34f, 20.66f, 8f, 19f, 8f)
            close()
            moveTo(16f, 19f)
            horizontalLineTo(8f)
            verticalLineTo(15f)
            horizontalLineTo(16f)
            verticalLineTo(19f)
            close()
            moveTo(19f, 12f)
            curveTo(18.45f, 12f, 18f, 11.55f, 18f, 11f)
            curveTo(18f, 10.45f, 18.45f, 10f, 19f, 10f)
            curveTo(19.55f, 10f, 20f, 10.45f, 20f, 11f)
            curveTo(20f, 11.55f, 19.55f, 12f, 19f, 12f)
            close()
            moveTo(18f, 3f)
            horizontalLineTo(6f)
            verticalLineTo(7f)
            horizontalLineTo(18f)
            verticalLineTo(3f)
            close()
        }
    }.build()
}

@Composable
fun InventoryScreen(
    uiState: InventoryUiState,
    rates: MarketRates,
    onBack: () -> Unit,
    onSelectCategory: (InventoryCategory) -> Unit,
    onSearchQueryChanged: (String) -> Unit,
    onOpenAddModal: () -> Unit,
    onCloseAddModal: () -> Unit,
    onOpenAdjustModal: (InventoryItem) -> Unit,
    onCloseAdjustModal: () -> Unit,
    onSaveNewItem: (InventoryItem) -> Unit,
    onConfirmAdjustment: (StockAdjustment) -> Unit,
    onDeleteItem: (String) -> Unit,
    onTransferToInvoice: (InventoryItem) -> Unit
) {
    val colors = LocalGoldExColors.current
    val context = LocalContext.current

    val spotPrice = remember(rates.gold18) { if (rates.gold18 > 0L) rates.gold18 else 23_360_000L }

    // Valuation in million tomans
    val totalEstimatedValueMillion = remember(uiState.totalGoldWeight18k, spotPrice) {
        val totalTomans = (uiState.totalGoldWeight18k * spotPrice).toLong()
        totalTomans / 1_000_000L
    }

    val itemsList = uiState.filteredItems

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.background)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(4.dp))

                    // 1. Top Sub-Header Bar (Unified Back Arrow Button)
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = colors.surface,
                        border = BorderStroke(1.dp, colors.goldBorder.copy(alpha = 0.4f)),
                        shadowElevation = 2.dp,
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
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                IconButton(
                                    onClick = onBack,
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(ButtonShape)
                                        .background(colors.surfaceElevated)
                                        .border(0.6.dp, colors.goldBorder, ButtonShape)
                                ) {
                                    Icon(
                                        imageVector = HubArrowRight,
                                        contentDescription = "بازگشت",
                                        tint = colors.goldPrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                Column {
                                    Text(
                                        text = "انبار و ویترین طلا",
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
                                                .size(6.dp)
                                                .clip(CircleShape)
                                                .background(colors.profitGreen)
                                        )
                                        Text(
                                            text = "همگام‌سازی لحظه‌ای ترازو و تگ RFID",
                                            fontSize = 10.sp,
                                            color = colors.textSecondary,
                                            fontFamily = VazirmatnFamily
                                        )
                                    }
                                }
                            }

                            // Gold "+ محصول جدید" Button
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = colors.goldPrimary,
                                modifier = Modifier.clickable { onOpenAddModal() }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Text(
                                        text = "محصول جدید",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        fontFamily = VazirmatnFamily
                                    )
                                }
                            }
                        }
                    }
                }

                // 2. Vault Master Valuation Card
                item {
                    LuxuryCard(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
                                            .size(36.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(colors.goldContainer),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = HubShowcase,
                                            contentDescription = null,
                                            tint = colors.goldPrimary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = "ارزش کل موجودی ویترین و گاوصندوق",
                                            fontSize = 11.5.sp,
                                            color = colors.textSecondary,
                                            fontFamily = VazirmatnFamily
                                        )
                                        Text(
                                            text = "بر مبنای طلای ۱۸ عیار",
                                            fontSize = 9.5.sp,
                                            color = colors.textMuted,
                                            fontFamily = VazirmatnFamily
                                        )
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = colors.surfaceElevated,
                                    border = BorderStroke(0.6.dp, colors.border)
                                ) {
                                    Text(
                                        text = "${PersianNumberFormatter.toPersianDigits(uiState.totalItemCount.toString())} قلم کالا",
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.goldPrimary,
                                        fontFamily = VazirmatnFamily,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Bottom
                            ) {
                                Column {
                                    Text(
                                        text = "برآورد ارزش ریالی:",
                                        fontSize = 10.sp,
                                        color = colors.textMuted,
                                        fontFamily = VazirmatnFamily
                                    )
                                    Text(
                                        text = "${PersianNumberFormatter.formatPrice(totalEstimatedValueMillion)} م ت",
                                        fontSize = 19.sp,
                                        fontWeight = FontWeight.Black,
                                        color = colors.goldPrimary,
                                        fontFamily = VazirmatnFamily
                                    )
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "مجموع وزن خالص طلای ۱۸:",
                                        fontSize = 10.sp,
                                        color = colors.textMuted,
                                        fontFamily = VazirmatnFamily
                                    )
                                    Text(
                                        text = "${PersianNumberFormatter.formatWeight(uiState.totalGoldWeight18k)} گرم",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.textMain,
                                        fontFamily = VazirmatnFamily
                                    )
                                }
                            }
                        }
                    }
                }

                // 3. Search Bar
                item {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = colors.surface,
                        border = BorderStroke(0.6.dp, colors.border),
                        modifier = Modifier.fillMaxWidth()
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
                                onValueChange = onSearchQueryChanged,
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
                                            text = "جستجوی عنوان طلا، کد بارکد، سینی یا کارگاه...",
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
                                    onClick = { onSearchQueryChanged("") },
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
                }

                // 4. Category Filter Capsules (Horizontal Scroll matching CustomerLedgerScreen)
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        InventoryCategory.entries.forEach { cat ->
                            val isSelected = uiState.selectedCategory == cat
                            val count = uiState.categoryCount(cat)
                            InventoryFilterCapsuleItem(
                                title = cat.titleFa,
                                count = count,
                                isSelected = isSelected,
                                onClick = { onSelectCategory(cat) }
                            )
                        }
                    }
                }

                // 5. Products List with Filter Transition Animation
                item {
                    AnimatedContent(
                        targetState = Pair(uiState.selectedCategory, itemsList),
                        transitionSpec = {
                            (LuxuryMotion.FilterEnter).togetherWith(LuxuryMotion.FilterExit)
                        },
                        label = "inventoryFilterTransition"
                    ) { (_, list) ->
                        if (list.isEmpty()) {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = colors.surfaceElevated,
                                border = BorderStroke(0.6.dp, colors.border),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 18.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Text(
                                        text = "📦",
                                        fontSize = 32.sp
                                    )
                                    Text(
                                        text = "هیچ طلایی در این دسته‌بندی یافت نشد",
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.textMain,
                                        fontFamily = VazirmatnFamily
                                    )
                                    Text(
                                        text = "می‌توانید با دکمه زیر محصول جدیدی به ویترین اضافه کنید",
                                        fontSize = 11.sp,
                                        color = colors.textMuted,
                                        fontFamily = VazirmatnFamily
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    GoldButton(
                                        text = "+ افزودن محصول به این سینی",
                                        onClick = onOpenAddModal,
                                        isSecondary = false,
                                        height = 40.dp
                                    )
                                }
                            }
                        } else {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                list.forEach { item ->
                                    InventoryItemCard(
                                        item = item,
                                        spotPrice18k = spotPrice,
                                        onTransferToInvoice = { onTransferToInvoice(item) },
                                        onAdjustStock = { onOpenAdjustModal(item) },
                                        onPrintTag = {
                                            QiratoToast.show(context, "اتیکت حرارتی ${item.code} با موفقیت آماده چاپ شد")
                                        },
                                        onDelete = { onDeleteItem(item.id) }
                                    )
                                }
                            }
                        }
                    }
                }

                // 6. Clean Bottom Clearance (Fix item 10)
                item {
                    Spacer(modifier = Modifier.height(16.dp).navigationBarsPadding())
                }
            }

            // Modals
            if (uiState.isAddModalOpen) {
                AddInventoryItemModal(
                    rates = rates,
                    onDismiss = onCloseAddModal,
                    onSaveItem = onSaveNewItem
                )
            }

            if (uiState.isAdjustModalOpen && uiState.selectedItemForAdjustment != null) {
                AdjustStockModal(
                    item = uiState.selectedItemForAdjustment,
                    rates = rates,
                    onDismiss = onCloseAdjustModal,
                    onConfirmAdjustment = onConfirmAdjustment
                )
            }
        }
    }
}

@Composable
private fun InventoryFilterCapsuleItem(
    title: String,
    count: Int,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val colors = LocalGoldExColors.current

    val animatedBg by animateColorAsState(
        targetValue = if (isSelected) Color(0xFF1E232E) else colors.surface,
        animationSpec = tween(durationMillis = 200, easing = LuxuryMotion.StandardEasing),
        label = "invCapsuleBg"
    )
    val animatedBorder by animateColorAsState(
        targetValue = if (isSelected) Color(0x66F59E0B) else colors.border,
        animationSpec = tween(durationMillis = 200, easing = LuxuryMotion.StandardEasing),
        label = "invCapsuleBorder"
    )
    val animatedTextColor by animateColorAsState(
        targetValue = if (isSelected) Color(0xFFFCD34D) else colors.textSecondary,
        animationSpec = tween(durationMillis = 200, easing = LuxuryMotion.StandardEasing),
        label = "invCapsuleTextColor"
    )

    Box(
        modifier = Modifier
            .clip(ButtonShape)
            .background(animatedBg)
            .border(1.dp, animatedBorder, ButtonShape)
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
                    .background(if (colors.isDark) Color(0x33F59E0B) else Color(0x20F59E0B))
                    .padding(horizontal = 6.dp, vertical = 1.dp)
            ) {
                Text(
                    text = PersianNumberFormatter.toPersianDigits(count.toString()),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) Color(0xFFFDE68A) else if (colors.isDark) Color(0xFFFCD34D) else Color(0xFFB45309),
                    fontFamily = VazirmatnFamily
                )
            }
        }
    }
}

@Composable
private fun InventoryItemCard(
    item: InventoryItem,
    spotPrice18k: Long,
    onTransferToInvoice: () -> Unit,
    onAdjustStock: () -> Unit,
    onPrintTag: () -> Unit,
    onDelete: () -> Unit
) {
    val colors = LocalGoldExColors.current
    val estimatedRetailPrice = remember(item, spotPrice18k) {
        item.calculateEstimatedValue(spotPrice18k)
    }

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = colors.surface,
        border = BorderStroke(0.8.dp, colors.goldBorder.copy(alpha = 0.4f)),
        shadowElevation = if (colors.isDark) 0.dp else 1.5.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header Row: Category Monogram, Title, Code Badge, Location Pill
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
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(colors.goldContainer)
                            .border(0.8.dp, colors.goldBorder.copy(alpha = 0.6f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = when (item.category) {
                                InventoryCategory.RINGS -> "💍"
                                InventoryCategory.BANGLES -> "📿"
                                InventoryCategory.NECKLACES -> "🪙"
                                InventoryCategory.SETS -> "✨"
                                InventoryCategory.JEWELRY -> "💎"
                                InventoryCategory.COINS -> "🟡"
                                else -> "🏷️"
                            },
                            fontSize = 18.sp
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.title,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textMain,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontFamily = VazirmatnFamily
                        )
                        Text(
                            text = "${item.workshop} • ${item.location}",
                            fontSize = 10.5.sp,
                            color = colors.textMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontFamily = VazirmatnFamily
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = colors.goldContainer,
                        border = BorderStroke(0.6.dp, colors.goldBorder)
                    ) {
                        Text(
                            text = item.code,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.goldSecondary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            fontFamily = VazirmatnFamily
                        )
                    }

                    if (item.quantity > 1) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = colors.profitGreen.copy(alpha = 0.12f),
                            border = BorderStroke(0.5.dp, colors.profitGreen.copy(alpha = 0.3f))
                        ) {
                            Text(
                                text = "${PersianNumberFormatter.toPersianDigits(item.quantity.toString())} عدد",
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.profitGreen,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                fontFamily = VazirmatnFamily
                            )
                        }
                    }
                }
            }

            // Middle Details Box: Weights, Karat, Wage & Estimated Value
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = colors.surfaceElevated,
                border = BorderStroke(0.6.dp, colors.border),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(text = "وزن کل ناخالص:", fontSize = 9.5.sp, color = colors.textMuted, fontFamily = VazirmatnFamily)
                            Text(
                                text = "${PersianNumberFormatter.formatWeight(item.grossWeightGrams)} گرم",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textMain,
                                fontFamily = VazirmatnFamily
                            )
                        }
                        Column {
                            Text(text = "وزن خالص طلا:", fontSize = 9.5.sp, color = colors.textMuted, fontFamily = VazirmatnFamily)
                            Text(
                                text = "${PersianNumberFormatter.formatWeight(item.netGoldWeightGrams)} گرم",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.goldPrimary,
                                fontFamily = VazirmatnFamily
                            )
                        }
                        Column {
                            Text(text = "عیار رسمی:", fontSize = 9.5.sp, color = colors.textMuted, fontFamily = VazirmatnFamily)
                            Text(
                                text = "${PersianNumberFormatter.toPersianDigits(item.customKaratValue.toString())} عیار",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textMain,
                                fontFamily = VazirmatnFamily
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(text = "اجرت ساخت:", fontSize = 9.5.sp, color = colors.textMuted, fontFamily = VazirmatnFamily)
                            val wageText = if (item.wageType == WageType.PERCENTAGE) {
                                "${PersianNumberFormatter.formatPercent(item.wageValue)}٪"
                            } else {
                                "${PersianNumberFormatter.formatPrice(item.wageValue.toLong())} ت"
                            }
                            Text(
                                text = wageText,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.profitGreen,
                                fontFamily = VazirmatnFamily
                            )
                        }
                    }

                    HorizontalDivider(
                        color = colors.border.copy(alpha = 0.5f),
                        thickness = 0.5.dp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "برآورد مظنه روز ویترین (با سود ${PersianNumberFormatter.toPersianDigits(item.profitPercent.toInt().toString())}٪):",
                            fontSize = 10.sp,
                            color = colors.textSecondary,
                            fontFamily = VazirmatnFamily
                        )
                        AnimatedPriceText(
                            amount = estimatedRetailPrice,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Black,
                            color = colors.goldPrimary,
                            unit = "تومان"
                        )
                    }
                }
            }

            // Action Buttons Toolbar: All aligned with uniform 38.dp height
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Primary Action: انتقال به فاکتور
                GoldButton(
                    text = "انتقال به فاکتور",
                    onClick = onTransferToInvoice,
                    isSecondary = false,
                    height = 38.dp,
                    modifier = Modifier.weight(1.3f)
                )

                // Secondary Action: کسر یا شارژ
                GoldButton(
                    text = "کسر یا شارژ",
                    onClick = onAdjustStock,
                    isSecondary = true,
                    height = 38.dp,
                    modifier = Modifier.weight(1.1f)
                )

                // Print Tag Button (Vector Icon, Uniform 38.dp height)
                IconButton(
                    onClick = onPrintTag,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(ButtonShape)
                        .background(colors.surfaceElevated)
                        .border(0.6.dp, colors.border, ButtonShape)
                ) {
                    Icon(
                        imageVector = InventoryPrintVector,
                        contentDescription = "چاپ اتیکت",
                        tint = colors.goldPrimary,
                        modifier = Modifier.size(17.dp)
                    )
                }

                // Delete Button (Uniform 38.dp height)
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(ButtonShape)
                        .background(colors.surfaceElevated)
                        .border(0.6.dp, colors.border, ButtonShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "حذف",
                        tint = colors.errorRed,
                        modifier = Modifier.size(17.dp)
                    )
                }
            }
        }
    }
}
