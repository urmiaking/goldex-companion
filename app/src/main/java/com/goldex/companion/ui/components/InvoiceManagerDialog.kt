package com.goldex.companion.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.goldex.companion.data.AppSettings
import com.goldex.companion.model.Invoice
import com.goldex.companion.model.PersianNumberFormatter
import com.goldex.companion.ui.theme.LocalGoldExColors
import com.goldex.companion.ui.util.PdfInvoiceGenerator
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun InvoiceManagerDialog(
    invoices: List<Invoice>,
    settings: AppSettings,
    onDismiss: () -> Unit,
    onDeleteInvoice: (String) -> Unit
) {
    val colors = LocalGoldExColors.current
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }

    val filteredInvoices = remember(invoices, searchQuery) {
        if (searchQuery.isBlank()) {
            invoices
        } else {
            val q = searchQuery.trim().lowercase()
            invoices.filter { inv ->
                inv.invoiceNumber.contains(q) ||
                        (inv.customer?.name?.lowercase()?.contains(q) == true) ||
                        (inv.customer?.phone?.contains(q) == true) ||
                        inv.items.any { it.title.lowercase().contains(q) }
            }
        }
    }

    val totalWeight = remember(invoices) { invoices.sumOf { it.totalNetWeight } }
    val totalAmount = remember(invoices) { invoices.sumOf { it.totalPayable } }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = colors.surface,
            border = androidx.compose.foundation.BorderStroke(1.dp, colors.goldBorder),
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.90f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "مدیریت و بایگانی فاکتورها",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textMain
                        )
                        Text(
                            text = "سوابق فاکتورهای رسمی و استخراج مجدد PDF",
                            fontSize = 11.sp,
                            color = colors.textMuted
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(colors.surfaceElevated)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "بستن",
                            tint = colors.textMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Stats Banner
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(colors.surfaceElevated)
                        .border(0.6.dp, colors.goldBorder, RoundedCornerShape(14.dp))
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("کل فاکتورها", fontSize = 10.sp, color = colors.textMuted)
                        Text(
                            text = "${PersianNumberFormatter.toPersianDigits(invoices.size.toString())} فقره",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.goldPrimary
                        )
                    }
                    Box(
                        modifier = Modifier
                            .width(0.8.dp)
                            .height(28.dp)
                            .background(colors.border)
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("مجموع وزن طلا", fontSize = 10.sp, color = colors.textMuted)
                        Text(
                            text = "${PersianNumberFormatter.formatWeight(totalWeight)} گرم",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textMain
                        )
                    }
                    Box(
                        modifier = Modifier
                            .width(0.8.dp)
                            .height(28.dp)
                            .background(colors.border)
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("گردش مالی کل", fontSize = 10.sp, color = colors.textMuted)
                        Text(
                            text = "${PersianNumberFormatter.formatPrice(totalAmount)} ت",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.goldPrimary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("جستجو بر اساس نام خریدار یا شماره فاکتور...", fontSize = 11.5.sp) },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = null,
                            tint = colors.goldPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colors.goldPrimary,
                        unfocusedBorderColor = colors.border
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Invoices List
                if (filteredInvoices.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "فاکتوری یافت نشد",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textMuted
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "فاکتورهای صادر شده در این بخش ذخیره می‌شوند.",
                                fontSize = 11.sp,
                                color = colors.textMuted
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(filteredInvoices, key = { it.id }) { invoice ->
                            val dateStr = SimpleDateFormat("yyyy/MM/dd - HH:mm", Locale.getDefault())
                                .format(Date(invoice.createdAt))

                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = colors.surfaceElevated,
                                border = androidx.compose.foundation.BorderStroke(0.6.dp, colors.border),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
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
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(colors.goldContainer)
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = "#${PersianNumberFormatter.toPersianDigits(invoice.invoiceNumber)}",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = colors.goldPrimary
                                                )
                                            }
                                            Text(
                                                text = invoice.customer?.name ?: "مشتری عمومی (نقدی)",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = colors.textMain
                                            )
                                        }

                                        Text(
                                            text = PersianNumberFormatter.toPersianDigits(dateStr),
                                            fontSize = 10.sp,
                                            color = colors.textMuted
                                        )
                                    }

                                    HorizontalDivider(color = colors.border, thickness = 0.5.dp)

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = "${PersianNumberFormatter.toPersianDigits(invoice.items.size.toString())} قلم طلا | وزن: ${PersianNumberFormatter.formatWeight(invoice.totalNetWeight)} گرم",
                                                fontSize = 11.sp,
                                                color = colors.textSecondary
                                            )
                                            Text(
                                                text = "${PersianNumberFormatter.formatPrice(invoice.totalPayable)} تومان",
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = colors.goldPrimary
                                            )
                                        }

                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            IconButton(
                                                onClick = {
                                                    PdfInvoiceGenerator.generateAndShareInvoice(
                                                        context = context,
                                                        invoice = invoice,
                                                        sourceName = settings.priceSource.labelFa,
                                                        settings = settings
                                                    )
                                                },
                                                modifier = Modifier
                                                    .size(34.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(colors.goldContainer)
                                            ) {
                                                Icon(
                                                    Icons.Default.Share,
                                                    contentDescription = "اشتراک‌گذاری PDF",
                                                    tint = colors.goldPrimary,
                                                    modifier = Modifier.size(17.dp)
                                                )
                                            }

                                            IconButton(
                                                onClick = { onDeleteInvoice(invoice.id) },
                                                modifier = Modifier
                                                    .size(34.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(colors.surface)
                                            ) {
                                                Icon(
                                                    Icons.Default.Delete,
                                                    contentDescription = "حذف فاکتور",
                                                    tint = colors.errorRed,
                                                    modifier = Modifier.size(17.dp)
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
        }
    }
}
