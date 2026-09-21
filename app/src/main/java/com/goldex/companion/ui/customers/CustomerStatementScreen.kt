package com.goldex.companion.ui.customers

import android.content.Intent
import android.net.Uri
import kotlin.math.abs
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import com.goldex.companion.ui.theme.ButtonShape
import com.goldex.companion.ui.hub.HubArrowRight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.goldex.companion.model.Customer
import com.goldex.companion.model.LedgerDirection
import com.goldex.companion.model.LedgerEntryType
import com.goldex.companion.model.LedgerTransaction
import com.goldex.companion.model.PersianNumberFormatter
import com.goldex.companion.model.StatementFilterTab
import com.goldex.companion.ui.components.GoldButton
import com.goldex.companion.ui.components.QiratoToast
import com.goldex.companion.ui.theme.LocalGoldExColors
import com.goldex.companion.ui.theme.LuxuryMotion
import com.goldex.companion.ui.theme.VazirmatnFamily

@Composable
fun CustomerStatementScreen(
    customer: Customer,
    transactions: List<LedgerTransaction>,
    allTransactions: List<LedgerTransaction> = transactions,
    selectedFilter: StatementFilterTab,
    onFilterSelect: (StatementFilterTab) -> Unit,
    onOpenAddEntry: () -> Unit,
    onEditTransaction: (LedgerTransaction) -> Unit = {},
    onDeleteTransaction: (LedgerTransaction) -> Unit = {},
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalGoldExColors.current
    val context = LocalContext.current
    var transactionToDelete by remember { mutableStateOf<LedgerTransaction?>(null) }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Scaffold(
            modifier = modifier
                .fillMaxSize()
                .background(colors.background),
            containerColor = colors.background,
            topBar = {
                Surface(
                    color = colors.surface,
                    border = BorderStroke(0.6.dp, colors.goldBorder.copy(alpha = 0.5f)),
                    shadowElevation = 3.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
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
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(7.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFDFB35A))
                                    )
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
                                    fontSize = 10.5.sp,
                                    color = colors.textMuted,
                                    fontFamily = VazirmatnFamily
                                )
                            }
                        }

                        // Right action buttons (Call & Share) with proper spacing
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (customer.phone.isNotBlank()) {
                                IconButton(
                                    onClick = {
                                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${customer.phone}"))
                                        context.startActivity(intent)
                                    },
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(ButtonShape)
                                        .background(colors.surfaceElevated)
                                        .border(0.6.dp, colors.goldBorder, ButtonShape)
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
                                    .size(38.dp)
                                    .clip(ButtonShape)
                                    .background(colors.surfaceElevated)
                                    .border(0.6.dp, colors.goldBorder, ButtonShape)
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
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Scrollable List Content with Hero Card & Filters as Header
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
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

                                    val isDebtor = customer.goldDebtGrams > 0.001 || customer.cashDebtTomans > 0L
                                    val isCreditor = customer.goldDebtGrams < -0.001 || customer.cashDebtTomans < 0L

                                    val heroStatusText = if (isDebtor) "بدهکار به ما"
                                    else if (isCreditor) "بستانکار"
                                    else "تسویه‌شده (بی‌حساب)"

                                    val heroStatusColor = if (isDebtor) Color(0xFFEF4444)
                                    else if (isCreditor) Color(0xFF10B981)
                                    else colors.textMuted

                                    Surface(
                                        shape = RoundedCornerShape(20.dp),
                                        color = heroStatusColor.copy(alpha = 0.15f),
                                        border = BorderStroke(0.5.dp, heroStatusColor.copy(alpha = 0.4f))
                                    ) {
                                        Text(
                                            text = heroStatusText,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = heroStatusColor,
                                            fontFamily = VazirmatnFamily,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }

                                // 2-Column Balance Matrix (Debtor Red, Creditor Green)
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
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Text(
                                                    text = "${PersianNumberFormatter.formatWeight(abs(customer.goldDebtGrams))} گرم",
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Black,
                                                    color = when {
                                                        customer.goldDebtGrams > 0.0001 -> Color(0xFFFB7185)
                                                        customer.goldDebtGrams < -0.0001 -> Color(0xFF34D399)
                                                        else -> Color(0xFFFFE088)
                                                    },
                                                    fontFamily = VazirmatnFamily
                                                )
                                                if (customer.goldDebtGrams > 0.0001 || customer.goldDebtGrams < -0.0001) {
                                                    val goldStatusText = if (customer.goldDebtGrams > 0.0001) "بدهکار" else "بستانکار"
                                                    val goldStatusColor = if (customer.goldDebtGrams > 0.0001) Color(0xFFFB7185) else Color(0xFF34D399)
                                                    Surface(
                                                        shape = RoundedCornerShape(4.dp),
                                                        color = goldStatusColor.copy(alpha = 0.15f)
                                                    ) {
                                                        Text(
                                                            text = goldStatusText,
                                                            fontSize = 8.5.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = goldStatusColor,
                                                            fontFamily = VazirmatnFamily,
                                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                        )
                                                    }
                                                }
                                            }
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
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Text(
                                                    text = PersianNumberFormatter.formatPrice(abs(customer.cashDebtTomans)),
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Black,
                                                    color = when {
                                                        customer.cashDebtTomans > 0L -> Color(0xFFFB7185)
                                                        customer.cashDebtTomans < 0L -> Color(0xFF34D399)
                                                        else -> Color.White
                                                    },
                                                    fontFamily = VazirmatnFamily
                                                )
                                                if (customer.cashDebtTomans != 0L) {
                                                    val cashStatusText = if (customer.cashDebtTomans > 0L) "بدهکار" else "بستانکار"
                                                    val cashStatusColor = if (customer.cashDebtTomans > 0L) Color(0xFFFB7185) else Color(0xFF34D399)
                                                    Surface(
                                                        shape = RoundedCornerShape(4.dp),
                                                        color = cashStatusColor.copy(alpha = 0.15f)
                                                    ) {
                                                        Text(
                                                            text = cashStatusText,
                                                            fontSize = 8.5.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = cashStatusColor,
                                                            fontFamily = VazirmatnFamily,
                                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                        )
                                                    }
                                                }
                                            }
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

                    // Item B: Filter Chips Row (Matching CustomerLedgerScreen styling & selection animation)
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            StatementFilterTab.values().forEach { tab ->
                                val selected = selectedFilter == tab
                                val (count, badgeBg, badgeTextColor) = when (tab) {
                                    StatementFilterTab.ALL -> Triple(
                                        allTransactions.size,
                                        if (colors.isDark) Color(0x33F59E0B) else Color(0x20F59E0B),
                                        if (selected) Color(0xFFFDE68A) else if (colors.isDark) Color(0xFFFCD34D) else Color(0xFFB45309)
                                    )
                                    StatementFilterTab.GOLD_SALE -> Triple(
                                        allTransactions.count { it.type == LedgerEntryType.GOLD_WEIGHT && it.direction == LedgerDirection.PAY },
                                        Color(0x26EF4444),
                                        Color(0xFFEF4444)
                                    )
                                    StatementFilterTab.GOLD_RECEIPT -> Triple(
                                        allTransactions.count { it.type == LedgerEntryType.GOLD_WEIGHT && it.direction == LedgerDirection.RECEIVE },
                                        Color(0x2610B981),
                                        Color(0xFF10B981)
                                    )
                                    StatementFilterTab.CASH_DEPOSIT -> Triple(
                                        allTransactions.count { it.type == LedgerEntryType.CASH_RIAL },
                                        Color(0x263B82F6),
                                        Color(0xFF3B82F6)
                                    )
                                    StatementFilterTab.SETTLEMENT -> Triple(
                                        allTransactions.count { it.title.contains("تسویه") || it.note.contains("تسویه") || it.tagBadge.contains("تهاتر") },
                                        Color(0x268B5CF6),
                                        Color(0xFF8B5CF6)
                                    )
                                }

                                StatementFilterCapsuleItem(
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

                    // Item C: Section Title
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "ریز رویدادهای اسناد حسابداری",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textMain,
                                fontFamily = VazirmatnFamily
                            )
                            Text(
                                text = "${PersianNumberFormatter.toPersianDigits(transactions.size.toString())} سند",
                                fontSize = 10.5.sp,
                                color = colors.goldPrimary,
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = VazirmatnFamily
                            )
                        }
                    }

                    // Items D: Transactions Timeline with Smooth Filter Transition
                    item {
                        AnimatedContent(
                            targetState = Pair(selectedFilter, transactions),
                            transitionSpec = {
                                (LuxuryMotion.FilterEnter).togetherWith(LuxuryMotion.FilterExit)
                            },
                            label = "statementTransactionsTransition"
                        ) { (_, txList) ->
                            if (txList.isEmpty()) {
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = colors.surfaceElevated,
                                    border = BorderStroke(0.6.dp, colors.border),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(20.dp),
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
                                            fontSize = 12.5.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = colors.textMuted,
                                            fontFamily = VazirmatnFamily
                                        )
                                    }
                                }
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    txList.forEach { tx ->
                                        StatementTransactionCard(
                                            transaction = tx,
                                            onEditClick = { onEditTransaction(tx) },
                                            onDeleteClick = { transactionToDelete = tx }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Spacer for bottom clearance
                    item {
                        Spacer(modifier = Modifier.height(84.dp))
                    }
                }

                // Floating Sticky Action Button ("ثبت دریافت / پرداخت جدید")
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
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    GoldButton(
                        text = "ثبت دریافت / پرداخت جدید",
                        icon = Icons.Default.Add,
                        onClick = onOpenAddEntry,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Delete Transaction Confirmation Dialog
            if (transactionToDelete != null) {
                Dialog(
                    onDismissRequest = { transactionToDelete = null },
                    properties = DialogProperties(usePlatformDefaultWidth = false)
                ) {
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = colors.surface,
                            border = BorderStroke(0.6.dp, colors.goldBorder),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Text(
                                    text = "حذف سند در دفتر معین",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = colors.textMain,
                                    fontFamily = VazirmatnFamily
                                )
                                Text(
                                    text = "آیا از حذف سند #${PersianNumberFormatter.toPersianDigits(transactionToDelete?.documentNumber ?: "")} («${transactionToDelete?.title}») اطمینان دارید؟ اثر مالی این سند از مانده حساب مشتری کسر/معکوس خواهد شد.",
                                    fontSize = 13.sp,
                                    color = colors.textSecondary,
                                    lineHeight = 20.sp,
                                    fontFamily = VazirmatnFamily
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // RTL: Secondary/Cancel action MUST be on the right (first child in Row)
                                    GoldButton(
                                        text = "انصراف",
                                        onClick = { transactionToDelete = null },
                                        isSecondary = true,
                                        modifier = Modifier.weight(1f),
                                        height = 44.dp
                                    )
                                    // RTL: Primary/Delete action MUST be on the left (second child in Row)
                                    GoldButton(
                                        text = "حذف قطعی",
                                        onClick = {
                                            val toDel = transactionToDelete
                                            transactionToDelete = null
                                            toDel?.let {
                                                onDeleteTransaction(it)
                                                QiratoToast.show(context, "سند با موفقیت حذف شد و مانده حساب بروزرسانی گردید")
                                            }
                                        },
                                        modifier = Modifier.weight(1f),
                                        height = 44.dp
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

@Composable
private fun StatementFilterCapsuleItem(
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
        label = "statementCapsuleBg"
    )
    val animatedBorder by animateColorAsState(
        targetValue = if (isSelected) Color(0x66F59E0B) else colors.border,
        animationSpec = tween(durationMillis = 200, easing = LuxuryMotion.StandardEasing),
        label = "statementCapsuleBorder"
    )
    val animatedTextColor by animateColorAsState(
        targetValue = if (isSelected) Color(0xFFFCD34D) else colors.textSecondary,
        animationSpec = tween(durationMillis = 200, easing = LuxuryMotion.StandardEasing),
        label = "statementCapsuleTextColor"
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
                    .background(badgeBg)
                    .padding(horizontal = 6.dp, vertical = 1.dp)
            ) {
                Text(
                    text = PersianNumberFormatter.toPersianDigits(count.toString()),
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = badgeTextColor,
                    fontFamily = VazirmatnFamily
                )
            }
        }
    }
}

@Composable
private fun MethodChip(
    text: String,
    isGold: Boolean,
    modifier: Modifier = Modifier
) {
    val colors = LocalGoldExColors.current
    val (chipBg, chipText, chipBorder) = when {
        isGold -> when {
            text.contains("آبشده") -> Triple(Color(0x26F59E0B), Color(0xFFF59E0B), Color(0x4DF59E0B))
            text.contains("مصنوعات") -> Triple(Color(0x26EC4899), Color(0xFFF472B6), Color(0x4DEC4899))
            text.contains("سکه") || text.contains("شمش") -> Triple(Color(0x2606B6D4), Color(0xFF22D3EE), Color(0x4D06B6D4))
            else -> Triple(colors.goldContainer.copy(alpha = 0.5f), colors.goldPrimary, colors.goldBorder)
        }
        else -> when {
            text.contains("حواله") || text.contains("پایا") -> Triple(Color(0x263B82F6), Color(0xFF60A5FA), Color(0x4D3B82F6))
            text.contains("چک") -> Triple(Color(0x268B5CF6), Color(0xFFA78BFA), Color(0x4D8B5CF6))
            text.contains("کارتخوان") || text.contains("POS") -> Triple(Color(0x266366F1), Color(0xFF818CF8), Color(0x4D6366F1))
            text.contains("نقد") || text.contains("اسکناس") -> Triple(Color(0x2610B981), Color(0xFF34D399), Color(0x4D10B981))
            else -> Triple(Color(0x2664748B), Color(0xFF94A3B8), Color(0x4D64748B))
        }
    }

    Surface(
        shape = RoundedCornerShape(6.dp),
        color = chipBg,
        border = BorderStroke(0.6.dp, chipBorder),
        modifier = modifier
    ) {
        Text(
            text = text,
            fontSize = 9.5.sp,
            fontWeight = FontWeight.Bold,
            color = chipText,
            fontFamily = VazirmatnFamily,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
private fun StatementTransactionCard(
    transaction: LedgerTransaction,
    onEditClick: () -> Unit = {},
    onDeleteClick: () -> Unit = {}
) {
    val colors = LocalGoldExColors.current
    val isGold = transaction.type == LedgerEntryType.GOLD_WEIGHT
    val isReceive = transaction.direction == LedgerDirection.RECEIVE

    Surface(
        shape = RoundedCornerShape(13.dp),
        color = colors.surface,
        border = BorderStroke(0.6.dp, colors.goldBorder.copy(alpha = 0.35f)),
        shadowElevation = if (colors.isDark) 0.dp else 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            // Row 1: Header - Icon + Title & Doc/Date + Direction Badge + Edit/Delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(7.dp))
                            .background(
                                if (isGold) colors.goldContainer.copy(alpha = 0.6f) else colors.surfaceElevated
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isGold) LedgerScaleVector else LedgerAccountBalanceVector,
                            contentDescription = null,
                            tint = colors.goldPrimary,
                            modifier = Modifier.size(15.dp)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = transaction.title,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textMain,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                fontFamily = VazirmatnFamily
                            )
                            if (transaction.tagBadge.isNotBlank()) {
                                Text(
                                    text = transaction.tagBadge,
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.goldPrimary,
                                    fontFamily = VazirmatnFamily,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(colors.goldContainer)
                                        .padding(horizontal = 4.dp, vertical = 0.5.dp)
                                )
                            }
                        }
                        Text(
                            text = "سند #${PersianNumberFormatter.toPersianDigits(transaction.documentNumber)} • ${transaction.dateTime}",
                            fontSize = 9.5.sp,
                            color = colors.textMuted,
                            fontFamily = VazirmatnFamily
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    // Direction badge
                    val dirColor = if (isReceive) colors.profitGreen else colors.errorRed
                    val dirText = if (isReceive) "دریافت" else "پرداخت"
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = dirColor.copy(alpha = 0.12f),
                        border = BorderStroke(0.5.dp, dirColor.copy(alpha = 0.3f))
                    ) {
                        Text(
                            text = dirText,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = dirColor,
                            fontFamily = VazirmatnFamily,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(colors.surfaceElevated)
                            .clickable(onClick = onEditClick),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "ویرایش سند",
                            tint = colors.goldPrimary,
                            modifier = Modifier.size(12.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(colors.surfaceElevated)
                            .clickable(onClick = onDeleteClick),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "حذف سند",
                            tint = colors.errorRed,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }

            // Row 2: Movement Strip - Method Chip + Details & Prominent Amount
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = colors.surfaceElevated,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.weight(1f, fill = false)
                        ) {
                            val methodLabel = if (isGold) {
                                if (transaction.goldCategory.isNotBlank()) transaction.goldCategory else "طلا ۷۵۰"
                            } else {
                                if (transaction.paymentMethod.isNotBlank()) transaction.paymentMethod else "نقدی"
                            }
                            MethodChip(text = methodLabel, isGold = isGold)

                            val detailsText = if (isGold && transaction.scaleWeightGrams > 0.0) {
                                buildString {
                                    append("ترازو: ${PersianNumberFormatter.formatWeight(transaction.scaleWeightGrams)}")
                                    if (transaction.karat != 750) {
                                        append(" (ع ${PersianNumberFormatter.toPersianDigits(transaction.karat.toString())})")
                                    }
                                    if (transaction.angNumber.isNotBlank()) {
                                        append(" • اَنگ: ${PersianNumberFormatter.toPersianDigits(transaction.angNumber)}")
                                    }
                                }
                            } else if (!isGold && (transaction.destinationBank.isNotBlank() || transaction.trackingCode.isNotBlank())) {
                                buildString {
                                    if (transaction.destinationBank.isNotBlank()) {
                                        append(transaction.destinationBank)
                                    }
                                    if (transaction.trackingCode.isNotBlank()) {
                                        if (isNotEmpty()) append(" • ")
                                        append("پیگیری: ${PersianNumberFormatter.toPersianDigits(transaction.trackingCode)}")
                                    }
                                }
                            } else ""

                            if (detailsText.isNotBlank()) {
                                Text(
                                    text = detailsText,
                                    fontSize = 9.5.sp,
                                    color = colors.textSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    fontFamily = VazirmatnFamily
                                )
                            }
                        }

                        // Prominent eye-catching amount (NO negative sign, direction indicated by color & badge)
                        Text(
                            text = if (isGold) {
                                "${PersianNumberFormatter.formatWeight(transaction.equivalent750WeightGrams)} گرم"
                            } else {
                                "${PersianNumberFormatter.formatPrice(transaction.amountTomans)} تومان"
                            },
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.Black,
                            color = if (isReceive) colors.profitGreen else colors.errorRed,
                            fontFamily = VazirmatnFamily
                        )
                    }

                    // Note: Render ONLY when note is not blank
                    if (transaction.note.isNotBlank()) {
                        Text(
                            text = "یادداشت: ${transaction.note}",
                            fontSize = 9.sp,
                            color = colors.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontFamily = VazirmatnFamily
                        )
                    }
                }
            }

            // Row 3: Resulting Balance with Clear Interpretation (مانده معین با تفکیک بدهکار/بستانکار و بدون منفی)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "مانده معین پس از ثبت:",
                    fontSize = 9.5.sp,
                    color = colors.textMuted,
                    fontFamily = VazirmatnFamily
                )

                if (isGold) {
                    val bal = transaction.resultingGoldBalance
                    val statusText = when {
                        bal > 0.0001 -> "بدهکار"
                        bal < -0.0001 -> "بستانکار"
                        else -> "تسویه‌شده"
                    }
                    val statusColor = when {
                        bal > 0.0001 -> colors.errorRed
                        bal < -0.0001 -> colors.profitGreen
                        else -> colors.textMuted
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "${PersianNumberFormatter.formatWeight(abs(bal))} گرم",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = statusColor,
                            fontFamily = VazirmatnFamily
                        )
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = statusColor.copy(alpha = 0.12f),
                            border = BorderStroke(0.5.dp, statusColor.copy(alpha = 0.3f))
                        ) {
                            Text(
                                text = statusText,
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = statusColor,
                                fontFamily = VazirmatnFamily,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                } else {
                    val bal = transaction.resultingCashBalance
                    val statusText = when {
                        bal > 0L -> "بدهکار"
                        bal < 0L -> "بستانکار"
                        else -> "تسویه‌شده"
                    }
                    val statusColor = when {
                        bal > 0L -> colors.errorRed
                        bal < 0L -> colors.profitGreen
                        else -> colors.textMuted
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "${PersianNumberFormatter.formatPrice(abs(bal))} تومان",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = statusColor,
                            fontFamily = VazirmatnFamily
                        )
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = statusColor.copy(alpha = 0.12f),
                            border = BorderStroke(0.5.dp, statusColor.copy(alpha = 0.3f))
                        ) {
                            Text(
                                text = statusText,
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = statusColor,
                                fontFamily = VazirmatnFamily,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
