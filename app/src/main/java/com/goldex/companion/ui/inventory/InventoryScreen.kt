package com.goldex.companion.ui.inventory

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.goldex.companion.data.MarketRates
import com.goldex.companion.model.InventoryCategory
import com.goldex.companion.model.InventoryItem
import com.goldex.companion.model.PersianNumberFormatter
import com.goldex.companion.model.StockAdjustment
import com.goldex.companion.ui.components.GoldButton
import com.goldex.companion.ui.components.GoldInputField
import com.goldex.companion.ui.components.LuxuryCard
import com.goldex.companion.ui.components.QiratoToast
import com.goldex.companion.ui.inventory.modals.AddInventoryItemModal
import com.goldex.companion.ui.inventory.modals.AdjustStockModal
import com.goldex.companion.ui.theme.LocalGoldExColors
import com.goldex.companion.ui.theme.VazirmatnFamily

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
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(6.dp))

                    // 1. Top Sub-Header Bar
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
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                IconButton(
                                    onClick = onBack,
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(colors.surfaceElevated)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                        contentDescription = "بازگشت",
                                        tint = colors.textMain,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                Column {
                                    Text(
                                        text = "انبار و ویترین طلا",
                                        fontSize = 14.sp,
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
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "محصول جدید",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        fontFamily = VazirmatnFamily
                                    )
                                }
                            }
                        }
                    }
                }

                // 2. Sovereign Vault Valuation Card (دارایی طلایی گالری)
                item {
                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        color = Color(0xFF111726),
                        border = BorderStroke(
                            1.dp,
                            Brush.verticalGradient(
                                listOf(
                                    Color(0xFFF59E0B).copy(alpha = 0.5f),
                                    Color(0xFFD97706).copy(alpha = 0.15f)
                                )
                            )
                        ),
                        shadowElevation = 8.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Header Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Color(0xFFF59E0B).copy(alpha = 0.15f))
                                            .border(1.dp, Color(0xFFF59E0B).copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(text = "🏛️", fontSize = 18.sp)
                                    }
                                    Column {
                                        Text(
                                            text = "موجودی تجمیعی ویترین و گاوصندوق",
                                            fontSize = 10.5.sp,
                                            color = Color(0xFF94A3B8),
                                            fontFamily = VazirmatnFamily
                                        )
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                                        ) {
                                            Text(
                                                text = "دارایی طلایی گالری",
                                                fontSize = 14.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White,
                                                fontFamily = VazirmatnFamily
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .size(6.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(0xFFFBBF24))
                                            )
                                        }
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = Color(0xFFF59E0B).copy(alpha = 0.15f),
                                    border = BorderStroke(0.8.dp, Color(0xFFF59E0B).copy(alpha = 0.4f))
                                ) {
                                    Text(
                                        text = "${PersianNumberFormatter.toPersianDigits(uiState.totalPiecesCount.toString())} قلم فعال",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFFBBF24),
                                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                                        fontFamily = VazirmatnFamily
                                    )
                                }
                            }

                            // 2-Column Balance Grid
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Right Box: Total Weight (18k)
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = Color.White.copy(alpha = 0.05f),
                                    border = BorderStroke(0.8.dp, Color.White.copy(alpha = 0.1f)),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(10.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = "کل وزن موجودی (۱۸ عیار)",
                                            fontSize = 10.sp,
                                            color = Color(0xFF94A3B8),
                                            fontFamily = VazirmatnFamily
                                        )
                                        Row(
                                            verticalAlignment = Alignment.Bottom,
                                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                                        ) {
                                            Text(
                                                text = PersianNumberFormatter.formatWeight(uiState.totalGoldWeight18k),
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Black,
                                                color = Color.White,
                                                fontFamily = VazirmatnFamily
                                            )
                                            Text(
                                                text = "گرم",
                                                fontSize = 10.sp,
                                                color = Color(0xFF94A3B8),
                                                fontFamily = VazirmatnFamily
                                            )
                                        }
                                        Text(
                                            text = "معادل ${PersianNumberFormatter.formatWeight(uiState.totalMesghalEquivalent)} مثقال",
                                            fontSize = 9.5.sp,
                                            color = Color(0xFFFBBF24),
                                            fontFamily = VazirmatnFamily
                                        )
                                    }
                                }

                                // Left Box: Spot Valuation
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = Color(0xFFF59E0B).copy(alpha = 0.08f),
                                    border = BorderStroke(0.8.dp, Color(0xFFF59E0B).copy(alpha = 0.25f)),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(10.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = "ارزش مظنه لحظه‌ای",
                                            fontSize = 10.sp,
                                            color = Color(0xFFFBBF24),
                                            fontFamily = VazirmatnFamily
                                        )
                                        Row(
                                            verticalAlignment = Alignment.Bottom,
                                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                                        ) {
                                            Text(
                                                text = PersianNumberFormatter.formatPrice(totalEstimatedValueMillion),
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Black,
                                                color = Color(0xFFFBBF24),
                                                fontFamily = VazirmatnFamily
                                            )
                                            Text(
                                                text = "میلیون تومان",
                                                fontSize = 9.5.sp,
                                                color = Color(0xFF94A3B8),
                                                fontFamily = VazirmatnFamily
                                            )
                                        }
                                        Text(
                                            text = "مظنه تابلو: ${PersianNumberFormatter.formatPrice(spotPrice)} ت",
                                            fontSize = 9.5.sp,
                                            color = Color(0xFF94A3B8),
                                            fontFamily = VazirmatnFamily
                                        )
                                    }
                                }
                            }

                            HorizontalDivider(color = Color.White.copy(alpha = 0.08f), thickness = 0.6.dp)

                            // Quick Metrics Strip
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "🔹 ${PersianNumberFormatter.toPersianDigits(uiState.activeTraysCount.toString())} سینی فعال ویترین",
                                    fontSize = 10.sp,
                                    color = Color(0xFF94A3B8),
                                    fontFamily = VazirmatnFamily
                                )
                                Text(
                                    text = "🔹 ${PersianNumberFormatter.toPersianDigits(uiState.activeSafesCount.toString())} بخش گاوصندوق",
                                    fontSize = 10.sp,
                                    color = Color(0xFF94A3B8),
                                    fontFamily = VazirmatnFamily
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = Color(0xFF10B981),
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Text(
                                        text = "تراز کامل انبار",
                                        fontSize = 10.sp,
                                        color = Color(0xFF10B981),
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = VazirmatnFamily
                                    )
                                }
                            }
                        }
                    }
                }

                // 3. Category Bar (Horizontal Scroll Chips)
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        InventoryCategory.values().forEach { category ->
                            val isSelected = uiState.selectedCategory == category
                            val count = uiState.categoryCount(category)
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = if (isSelected) colors.goldPrimary else colors.surfaceElevated,
                                border = BorderStroke(
                                    width = 1.dp,
                                    color = if (isSelected) colors.goldPrimary else colors.border
                                ),
                                modifier = Modifier.clickable { onSelectCategory(category) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Text(
                                        text = category.titleFa,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) Color.White else colors.textMain,
                                        fontFamily = VazirmatnFamily
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (isSelected) Color.White.copy(alpha = 0.25f) else colors.surface,
                                        modifier = Modifier.padding(start = 2.dp)
                                    ) {
                                        Text(
                                            text = PersianNumberFormatter.toPersianDigits(count.toString()),
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) Color.White else colors.textSecondary,
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                                            fontFamily = VazirmatnFamily
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 4. Product Cards
                val itemsList = uiState.filteredItems
                if (itemsList.isEmpty()) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = colors.surfaceElevated.copy(alpha = 0.5f),
                            border = BorderStroke(1.dp, colors.border.copy(alpha = 0.6f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(text = "📦", fontSize = 32.sp)
                                Text(
                                    text = "هیچ کالایی در این دسته‌بندی یافت نشد",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.textSecondary,
                                    fontFamily = VazirmatnFamily
                                )
                                GoldButton(
                                    text = "+ افزودن اولین محصول",
                                    onClick = onOpenAddModal,
                                    isSecondary = false
                                )
                            }
                        }
                    }
                } else {
                    items(itemsList, key = { it.id }) { item ->
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

                item {
                    Spacer(modifier = Modifier.height(86.dp))
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
private fun InventoryItemCard(
    item: InventoryItem,
    spotPrice18k: Long,
    onTransferToInvoice: () -> Unit,
    onAdjustStock: () -> Unit,
    onPrintTag: () -> Unit,
    onDelete: () -> Unit
) {
    val colors = LocalGoldExColors.current

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = colors.surface,
        border = BorderStroke(1.dp, colors.border.copy(alpha = 0.7f)),
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(13.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header Row: Code & Location
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = colors.goldContainer,
                    border = BorderStroke(0.6.dp, colors.goldBorder)
                ) {
                    Text(
                        text = item.code,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.goldSecondary,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                        fontFamily = VazirmatnFamily
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
                            .background(colors.profitGreen)
                    )
                    Text(
                        text = item.location,
                        fontSize = 10.5.sp,
                        color = colors.textSecondary,
                        fontFamily = VazirmatnFamily
                    )
                }

                if (item.quantity > 1) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = colors.profitGreen.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = "${PersianNumberFormatter.toPersianDigits(item.quantity.toString())} عدد موجود",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.profitGreen,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            fontFamily = VazirmatnFamily
                        )
                    }
                }
            }

            // Title
            Text(
                text = item.title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textMain,
                fontFamily = VazirmatnFamily
            )

            // Weight & Details Grid
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = colors.surfaceElevated.copy(alpha = 0.6f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(text = "وزن کل:", fontSize = 9.5.sp, color = colors.textMuted, fontFamily = VazirmatnFamily)
                        Text(
                            text = "${PersianNumberFormatter.formatWeight(item.grossWeightGrams)} گرم",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textMain,
                            fontFamily = VazirmatnFamily
                        )
                    }
                    Column {
                        Text(text = "وزن خالص:", fontSize = 9.5.sp, color = colors.textMuted, fontFamily = VazirmatnFamily)
                        Text(
                            text = "${PersianNumberFormatter.formatWeight(item.netGoldWeightGrams)} گرم",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.goldPrimary,
                            fontFamily = VazirmatnFamily
                        )
                    }
                    Column {
                        Text(text = "کسر سنگ:", fontSize = 9.5.sp, color = colors.textMuted, fontFamily = VazirmatnFamily)
                        Text(
                            text = "${PersianNumberFormatter.formatWeight(item.stoneWeightGrams)} گرم",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = colors.textSecondary,
                            fontFamily = VazirmatnFamily
                        )
                    }
                    Column {
                        Text(text = "اجرت+سود:", fontSize = 9.5.sp, color = colors.textMuted, fontFamily = VazirmatnFamily)
                        Text(
                            text = "${PersianNumberFormatter.toPersianDigits(item.wagePercent.toInt().toString())}٪ + ${PersianNumberFormatter.toPersianDigits(item.profitPercent.toInt().toString())}٪",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.profitGreen,
                            fontFamily = VazirmatnFamily
                        )
                    }
                }
            }

            // RFID Bar & Verification
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(colors.surfaceElevated.copy(alpha = 0.35f))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "تگ RFID: ${item.rfidTag.ifBlank { "—" }}",
                    fontSize = 10.sp,
                    color = colors.textSecondary,
                    fontFamily = VazirmatnFamily
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = colors.profitGreen,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = "پلمب و اتیکت‌دار",
                        fontSize = 9.5.sp,
                        color = colors.profitGreen,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = VazirmatnFamily
                    )
                }
            }

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Primary: انتقال به فاکتور
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = colors.goldPrimary,
                    modifier = Modifier
                        .weight(1.2f)
                        .clickable { onTransferToInvoice() }
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "انتقال به فاکتور",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontFamily = VazirmatnFamily
                        )
                    }
                }

                // Secondary: کسر یا شارژ موجودی
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = colors.surfaceElevated,
                    border = BorderStroke(1.dp, colors.border),
                    modifier = Modifier
                        .weight(1.2f)
                        .clickable { onAdjustStock() }
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "کسر یا شارژ موجودی",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textMain,
                            fontFamily = VazirmatnFamily
                        )
                    }
                }

                // Print Tag Button
                IconButton(
                    onClick = onPrintTag,
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(colors.surfaceElevated)
                        .border(1.dp, colors.border, RoundedCornerShape(10.dp))
                ) {
                    Text(text = "🖨️", fontSize = 14.sp)
                }

                // Delete Button
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(colors.surfaceElevated)
                        .border(1.dp, colors.border, RoundedCornerShape(10.dp))
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "حذف",
                        tint = colors.errorRed,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
