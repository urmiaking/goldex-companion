package com.goldex.companion.ui.customers

import android.content.Intent
import android.net.Uri
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.goldex.companion.model.Customer
import com.goldex.companion.model.LedgerDirection
import com.goldex.companion.model.LedgerEntryType
import com.goldex.companion.model.LedgerTransaction
import com.goldex.companion.model.PersianNumberFormatter
import com.goldex.companion.model.StatementFilterTab
import com.goldex.companion.ui.components.GoldButton
import com.goldex.companion.ui.components.QiratoToast
import com.goldex.companion.ui.theme.LocalGoldExColors
import com.goldex.companion.ui.theme.VazirmatnFamily

@Composable
fun CustomerStatementScreen(
    customer: Customer,
    transactions: List<LedgerTransaction>,
    selectedFilter: StatementFilterTab,
    onFilterSelect: (StatementFilterTab) -> Unit,
    onOpenAddEntry: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalGoldExColors.current
    val context = LocalContext.current

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(colors.background)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                // 1. Top Sub-Header Bar
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
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = customer.name,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.textMain,
                                        fontFamily = VazirmatnFamily
                                    )
                                    if (customer.isVerified) {
                                        Icon(
                                            imageVector = LedgerVerifiedVector,
                                            contentDescription = "همکار تایید شده",
                                            tint = colors.goldPrimary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = "${customer.role} • کد ${PersianNumberFormatter.toPersianDigits(customer.accountCode)}",
                                    fontSize = 11.sp,
                                    color = colors.textMuted,
                                    fontFamily = VazirmatnFamily
                                )
                            }
                        }

                        // Right action buttons (Call & Share)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            if (customer.phone.isNotBlank()) {
                                IconButton(
                                    onClick = {
                                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${customer.phone}"))
                                        context.startActivity(intent)
                                    },
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(colors.surfaceElevated)
                                ) {
                                    Icon(
                                        imageVector = LedgerPhoneVector,
                                        contentDescription = "تماس با طرف حساب",
                                        tint = colors.goldPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            IconButton(
                                onClick = {
                                    QiratoToast.show(context, "در حال تهیه گزارش گردش حساب ${customer.name}...")
                                },
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(colors.surfaceElevated)
                            ) {
                                Icon(
                                    imageVector = LedgerShareVector,
                                    contentDescription = "اشتراک گذاری صورت حساب",
                                    tint = colors.textSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                // Scrollable List Content with Hero Card & Filters as Header
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Item A: Customer Hero Balance Card (Obsidian)
                    item {
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
                                            text = "تراز طلایی و ریالی معین",
                                            fontSize = 12.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            fontFamily = VazirmatnFamily
                                        )
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(20.dp),
                                        color = if (customer.goldDebtGrams >= 0) Color(0xFF10B981).copy(alpha = 0.15f) else Color(0xFFF43F5E).copy(alpha = 0.15f),
                                        border = BorderStroke(
                                            0.5.dp,
                                            if (customer.goldDebtGrams >= 0) Color(0xFF10B981).copy(alpha = 0.4f) else Color(0xFFF43F5E).copy(alpha = 0.4f)
                                        )
                                    ) {
                                        Text(
                                            text = if (customer.goldDebtGrams > 0) "بدهکار به ما"
                                            else if (customer.goldDebtGrams < 0) "بستانکار"
                                            else "تسویه‌شده (بی‌حساب)",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (customer.goldDebtGrams >= 0) Color(0xFF34D399) else Color(0xFFFB7185),
                                            fontFamily = VazirmatnFamily,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }

                                // 2-Column Balance Matrix
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    // Gold Balance
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = Color(0xFF1D263B),
                                        border = BorderStroke(0.5.dp, Color(0xFF334155)),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Text(
                                                text = "مانده وزنی طلا",
                                                fontSize = 10.sp,
                                                color = Color(0xFF94A3B8),
                                                fontFamily = VazirmatnFamily
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "${PersianNumberFormatter.formatWeight(customer.goldDebtGrams)} گرم",
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Black,
                                                color = Color(0xFFFFE088),
                                                fontFamily = VazirmatnFamily
                                            )
                                            Text(
                                                text = "معادل طلای ۷۵۰ (۱۸ عیار)",
                                                fontSize = 9.sp,
                                                color = Color(0xFF64748B),
                                                fontFamily = VazirmatnFamily
                                            )
                                        }
                                    }

                                    // Cash Balance
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = Color(0xFF1D263B),
                                        border = BorderStroke(0.5.dp, Color(0xFF334155)),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Text(
                                                text = "مانده وجه نقدی",
                                                fontSize = 10.sp,
                                                color = Color(0xFF94A3B8),
                                                fontFamily = VazirmatnFamily
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "${PersianNumberFormatter.formatPrice(customer.cashDebtTomans)}",
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Black,
                                                color = Color.White,
                                                fontFamily = VazirmatnFamily
                                            )
                                            Text(
                                                text = "تومان ایران",
                                                fontSize = 9.sp,
                                                color = Color(0xFF64748B),
                                                fontFamily = VazirmatnFamily
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Item B: Filter Chips Row
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            StatementFilterTab.values().forEach { tab ->
                                val selected = selectedFilter == tab
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = if (selected) colors.surfaceElevated else colors.surface,
                                    border = BorderStroke(
                                        if (selected) 1.dp else 0.5.dp,
                                        if (selected) colors.goldPrimary else colors.border
                                    ),
                                    modifier = Modifier.clickable { onFilterSelect(tab) }
                                ) {
                                    Text(
                                        text = tab.titleFa,
                                        fontSize = 11.5.sp,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (selected) colors.goldPrimary else colors.textSecondary,
                                        fontFamily = VazirmatnFamily,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Item C: Section Title
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "ریز رویدادهای اسناد حسابداری",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textMain,
                                fontFamily = VazirmatnFamily
                            )
                            Text(
                                text = "مرتب‌سازی: جدیدترین",
                                fontSize = 10.5.sp,
                                color = colors.textMuted,
                                fontFamily = VazirmatnFamily
                            )
                        }
                    }

                    // Items D: Transactions Timeline
                    if (transactions.isEmpty()) {
                        item {
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
                                    Icon(
                                        imageVector = LedgerReceiptVector,
                                        contentDescription = null,
                                        tint = colors.textMuted,
                                        modifier = Modifier.size(36.dp)
                                    )
                                    Text(
                                        text = "سندی در این دسته‌بندی یافت نشد",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = colors.textMuted,
                                        fontFamily = VazirmatnFamily
                                    )
                                }
                            }
                        }
                    } else {
                        items(transactions, key = { it.id }) { tx ->
                            StatementTransactionCard(transaction = tx)
                        }
                    }

                    // Spacer for bottom clearance
                    item {
                        Spacer(modifier = Modifier.height(90.dp))
                    }
                }
            }

            // Floating Sticky Action Button ("ثبت دریافت / پرداخت جدید" - Fix 1 GoldButton)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Transparent,
                                colors.background.copy(alpha = 0.85f),
                                colors.background
                            )
                        )
                    )
                    .padding(horizontal = 16.dp, vertical = 16.dp)
            ) {
                GoldButton(
                    text = "ثبت دریافت / پرداخت جدید",
                    icon = Icons.Default.Add,
                    onClick = onOpenAddEntry,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun StatementTransactionCard(transaction: LedgerTransaction) {
    val colors = LocalGoldExColors.current
    val isGold = transaction.type == LedgerEntryType.GOLD_WEIGHT
    val isReceive = transaction.direction == LedgerDirection.RECEIVE

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = colors.surface,
        border = BorderStroke(0.6.dp, colors.goldBorder.copy(alpha = 0.4f)),
        shadowElevation = if (colors.isDark) 0.dp else 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Top Row: Category Icon, Title, Doc #, Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
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
                            .clip(CircleShape)
                            .background(
                                if (isGold) colors.goldContainer else colors.surfaceElevated
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isGold) LedgerScaleVector else LedgerAccountBalanceVector,
                            contentDescription = null,
                            tint = colors.goldPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Column {
                        Text(
                            text = transaction.title,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textMain,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontFamily = VazirmatnFamily
                        )
                        Text(
                            text = "سند #${PersianNumberFormatter.toPersianDigits(transaction.documentNumber)} • ${transaction.dateTime}",
                            fontSize = 10.5.sp,
                            color = colors.textMuted,
                            fontFamily = VazirmatnFamily
                        )
                    }
                }

                if (transaction.tagBadge.isNotBlank()) {
                    Text(
                        text = transaction.tagBadge,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textSecondary,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(colors.surfaceElevated)
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                        fontFamily = VazirmatnFamily
                    )
                }
            }

            // Middle Box: Amount & Details
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = colors.surfaceElevated,
                modifier = Modifier.fillMaxWidth()
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
                        Text(
                            text = if (isGold) {
                                if (isReceive) "تغییر وزنی (بستانکار):" else "تغییر وزنی (بدهکار):"
                            } else {
                                if (isReceive) "بستانکار نقدی:" else "بدهکار نقدی:"
                            },
                            fontSize = 11.5.sp,
                            color = colors.textMuted,
                            fontFamily = VazirmatnFamily
                        )

                        Text(
                            text = if (isGold) {
                                "${if (isReceive) "- " else "+ "}${PersianNumberFormatter.formatWeight(transaction.equivalent750WeightGrams)} گرم طلا"
                            } else {
                                "${if (isReceive) "- " else "+ "}${PersianNumberFormatter.formatPrice(transaction.amountTomans)} تومان"
                            },
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isReceive) colors.profitGreen else colors.errorRed,
                            fontFamily = VazirmatnFamily
                        )
                    }

                    if (transaction.note.isNotBlank()) {
                        Text(
                            text = transaction.note,
                            fontSize = 10.5.sp,
                            color = colors.textSecondary,
                            fontFamily = VazirmatnFamily
                        )
                    }
                }
            }

            // Bottom Line: Balance after document
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "مانده معین پس از این سند:",
                    fontSize = 10.5.sp,
                    color = colors.textMuted,
                    fontFamily = VazirmatnFamily
                )
                Text(
                    text = if (isGold) {
                        "${PersianNumberFormatter.formatWeight(transaction.resultingGoldBalance)} گرم طلا"
                    } else {
                        "${PersianNumberFormatter.formatPrice(transaction.resultingCashBalance)} تومان"
                    },
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.goldPrimary,
                    fontFamily = VazirmatnFamily
                )
            }
        }
    }
}
