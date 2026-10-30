package com.goldex.companion.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.goldex.companion.model.InventoryCategory
import com.goldex.companion.model.InventoryItem
import com.goldex.companion.model.PersianNumberFormatter
import com.goldex.companion.ui.theme.goldButtonText
import com.goldex.companion.ui.theme.LocalGoldExColors
import com.goldex.companion.ui.theme.VazirmatnFamily

/** Dialog content only; hosts own the modal/scrim. Draft selection never deducts stock. */
@Composable
fun InventorySalePickerContent(
    products: List<InventoryItem>,
    onDismiss: () -> Unit,
    onConfirm: (String, Int) -> Unit,
    searchField: (@Composable (String, (String) -> Unit) -> Unit)? = null,
    modifier: Modifier = Modifier,
    bottomSheet: Boolean = false
) {
    val c = LocalGoldExColors.current
    var query by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(InventoryCategory.ALL) }
    var selectedId by remember { mutableStateOf<String?>(null) }
    var countText by remember { mutableStateOf("1") }
    var submitting by remember { mutableStateOf(false) }
    val stock = remember(products) { products.filter { it.quantity > 0 } }
    val categories = remember(stock) { listOf(InventoryCategory.ALL) + stock.map { it.category }.distinct() }
    val matches = remember(stock, query, category) {
        stock.filter {
            (category == InventoryCategory.ALL || it.category == category) &&
                (query.isBlank() || it.title.contains(query.trim(), true) ||
                    it.code.contains(query.trim(), true) || it.rfidTag.contains(query.trim(), true))
        }
    }
    val chosen = stock.firstOrNull { it.id == selectedId }
    val count = countText.map {
        when (it) {
            in '۰'..'۹' -> ('0'.code + it.code - '۰'.code).toChar()
            in '٠'..'٩' -> ('0'.code + it.code - '٠'.code).toChar()
            else -> it
        }
    }.joinToString("").toIntOrNull()
    val canSave = chosen != null && inventoryProductIsSellable(chosen) &&
        count != null && count in 1..chosen.quantity

    Surface(
        modifier = modifier.fillMaxWidth().widthIn(max = 640.dp).heightIn(max = 620.dp)
            .testTag("inventory-sale-dialog"),
        color = c.surface,
        shape = if (bottomSheet) RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            else RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, c.goldBorder), shadowElevation = 16.dp
    ) {
        Column {
            if (bottomSheet) {
                Box(
                    Modifier.fillMaxWidth().padding(top = 9.dp, bottom = 2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(Modifier.size(width = 36.dp, height = 4.dp)
                        .clip(RoundedCornerShape(50))
                        .background(c.textMuted.copy(alpha = 0.3f)))
                }
            }
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(11.dp),
                    color = c.goldContainer.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, c.goldBorder)
                ) {
                    Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                        Text("▣", fontSize = 22.sp, color = c.goldPrimary)
                    }
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        "انتخاب کالا از انبار", fontFamily = VazirmatnFamily,
                        fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = c.textMain
                    )
                    Text(
                        "افزودن مستقیم به اقلام فروش فاکتور",
                        fontFamily = VazirmatnFamily, fontSize = 11.sp, color = c.textMuted
                    )
                }
                Surface(
                    onClick = onDismiss,
                    modifier = Modifier.size(36.dp).testTag("inventory-dialog-close"),
                    shape = CircleShape, color = c.surfaceElevated,
                    border = BorderStroke(1.dp, c.border)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("×", color = c.textSecondary, fontSize = 23.sp)
                    }
                }
            }
            HorizontalDivider(color = c.border)
            Column(Modifier.fillMaxWidth().weight(1f, fill = false)
                .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SummaryPill("کالاهای موجود", stock.size.toString())
                    SummaryPill("موجودی", stock.sumOf { it.quantity.toLong() }.toString() + " عدد")
                }
                if (searchField != null) {
                    searchField(query) { query = it }
                } else {
                    Surface(shape = RoundedCornerShape(12.dp), color = c.surfaceElevated,
                        border = BorderStroke(1.dp, c.border)) {
                        Row(Modifier.fillMaxWidth().height(48.dp).padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            Text("⌕", color = c.goldPrimary, fontSize = 23.sp)
                            Spacer(Modifier.width(8.dp))
                            BasicTextField(query, { query = it }, singleLine = true,
                                modifier = Modifier.weight(1f).testTag("invoice-inventory-search"),
                                textStyle = TextStyle(color = c.textMain, fontFamily = VazirmatnFamily,
                                    fontSize = 13.sp), cursorBrush = SolidColor(c.goldPrimary),
                                decorationBox = { inner ->
                                    Box {
                                        if (query.isEmpty()) Text("جستجو با نام، کد یا بارکد...",
                                            color = c.textMuted, fontFamily = VazirmatnFamily, fontSize = 13.sp)
                                        inner()
                                    }
                                })
                            if (query.isNotEmpty()) TextButton(onClick = { query = "" }) {
                                Text("پاک کردن", fontFamily = VazirmatnFamily, fontSize = 10.sp)
                            }
                        }
                    }
                }
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(categories, key = { it.name }) { item ->
                        val chosenCategory = item == category
                        Surface(onClick = { category = item }, shape = RoundedCornerShape(10.dp),
                            color = if (chosenCategory) c.goldContainer else c.surfaceElevated,
                            border = BorderStroke(1.dp, if (chosenCategory) c.goldPrimary else c.border)) {
                            Text(item.titleFa, modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                                fontFamily = VazirmatnFamily, fontSize = 11.sp,
                                fontWeight = if (chosenCategory) FontWeight.Bold else FontWeight.Medium,
                                color = if (chosenCategory) c.goldPrimary else c.textSecondary)
                        }
                    }
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("کالاهای قابل انتخاب", fontFamily = VazirmatnFamily,
                        fontSize = 12.sp, fontWeight = FontWeight.Bold, color = c.textMain)
                    Text(PersianNumberFormatter.toPersianDigits(matches.size.toString()) + " نتیجه",
                        fontFamily = VazirmatnFamily, fontSize = 11.sp, color = c.textMuted)
                }
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f, fill = false)
                        .heightIn(max = 260.dp).testTag("inventory-picker-results"),
                    verticalArrangement = Arrangement.spacedBy(7.dp),
                    contentPadding = PaddingValues(bottom = 5.dp)
                ) {
                    if (matches.isEmpty()) item {
                        Box(Modifier.fillMaxWidth().height(125.dp), contentAlignment = Alignment.Center) {
                            Text("کالایی پیدا نشد؛ جستجو یا دسته‌بندی را تغییر دهید",
                                color = c.textMuted, fontFamily = VazirmatnFamily, fontSize = 12.sp)
                        }
                    }
                    items(matches, key = { it.id }) { item ->
                        val enabled = inventoryProductIsSellable(item)
                        val selected = item.id == selectedId
                        Surface(
                            onClick = { if (enabled) { selectedId = item.id; countText = "1" } },
                            enabled = enabled,
                            modifier = Modifier.fillMaxWidth().testTag("inventory-product-" + item.id),
                            shape = RoundedCornerShape(13.dp),
                            color = if (selected) c.goldContainer.copy(alpha = 0.6f) else c.surfaceElevated,
                            border = BorderStroke(if (selected) 1.5.dp else 1.dp,
                                if (selected) c.goldPrimary else c.border)
                        ) {
                            Row(Modifier.padding(9.dp), verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Box(Modifier.size(34.dp).clip(CircleShape)
                                    .background(if (selected) c.goldPrimary else c.surface),
                                    contentAlignment = Alignment.Center) {
                                    Text(if (selected) "✓" else "○", fontSize = 20.sp,
                                        color = if (selected) c.goldButtonText else c.textMuted)
                                }
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                    Text(item.title, color = c.textMain, fontFamily = VazirmatnFamily,
                                        fontWeight = FontWeight.Medium, fontSize = 12.sp, maxLines = 1,
                                        overflow = TextOverflow.Ellipsis)
                                    Text(item.code + " • " + item.category.titleFa,
                                        color = c.textSecondary, fontFamily = VazirmatnFamily, fontSize = 10.sp)
                                    Text(
                                        when {
                                            !enabled -> "نوع، قیمت یا وکیوم سکه باید در انبار تکمیل شود"
                                            item.category == InventoryCategory.COINS ->
                                                (item.coinType?.titleFa ?: "") + " • " +
                                                    PersianNumberFormatter.toPersianDigits(item.coinUnitPrice.toString()) + " تومان"
                                            else -> "وزن خالص: " + PersianNumberFormatter.toPersianDigits(
                                                item.netGoldWeightGrams.toString()) + " گرم"
                                        },
                                        color = if (enabled) c.textMuted else c.goldPrimary,
                                        fontFamily = VazirmatnFamily, fontSize = 10.sp, maxLines = 1,
                                        overflow = TextOverflow.Ellipsis)
                                }
                                Surface(shape = RoundedCornerShape(8.dp), color = c.surface,
                                    border = BorderStroke(1.dp, c.border)) {
                                    Text(PersianNumberFormatter.toPersianDigits(item.quantity.toString()) + " عدد",
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                        color = c.textSecondary, fontFamily = VazirmatnFamily,
                                        fontWeight = FontWeight.Bold, fontSize = 10.sp)
                                }
                            }
                        }
                    }
                }
            }
            HorizontalDivider(color = c.border)
            Column(Modifier.fillMaxWidth()
                .then(if (bottomSheet) Modifier.navigationBarsPadding() else Modifier)
                .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(9.dp)) {
                if (chosen != null && inventoryProductIsSellable(chosen)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("تعداد فروش", fontFamily = VazirmatnFamily,
                                fontWeight = FontWeight.Bold, fontSize = 12.sp, color = c.textMain)
                            Text("موجودی: " + PersianNumberFormatter.toPersianDigits(chosen.quantity.toString()) + " عدد",
                                fontFamily = VazirmatnFamily, fontSize = 10.sp, color = c.textMuted)
                        }
                        QuantityControl("−", (count ?: 0) > 1) {
                            countText = ((count ?: 1) - 1).toString()
                        }
                        BasicTextField(countText, { countText = it.take(6) }, singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.width(56.dp).padding(horizontal = 10.dp)
                                .testTag("inventory-sale-quantity"),
                            textStyle = TextStyle(color = c.textMain, fontSize = 17.sp,
                                fontWeight = FontWeight.Bold))
                        QuantityControl("+", (count ?: 0) < chosen.quantity) {
                            countText = ((count ?: 0) + 1).toString()
                        }
                    }
                } else {
                    Text("یک کالا را برای افزودن به فاکتور انتخاب کنید.",
                        fontFamily = VazirmatnFamily, fontSize = 11.sp, color = c.textMuted)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f).height(45.dp),
                        shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, c.border)) {
                        Text("انصراف", fontFamily = VazirmatnFamily,
                            fontSize = 12.sp, color = c.textSecondary)
                    }
                    Button(onClick = {
                        val item = chosen
                        val qty = count
                        if (item != null && qty != null && canSave) {
                            submitting = true
                            onConfirm(item.id, qty)
                        }
                    }, enabled = canSave, modifier = Modifier.weight(1.5f).height(45.dp)
                        .testTag("inventory-sale-confirm"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = c.goldPrimary,
                            contentColor = c.goldButtonText)) {
                        Text("افزودن به فاکتور", fontFamily = VazirmatnFamily,
                            fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}

fun inventoryProductIsSellable(item: InventoryItem): Boolean =
    item.quantity > 0 && (item.category != InventoryCategory.COINS ||
        (item.coinType != null && item.coinUnitPrice?.let { it > 0 } == true &&
            item.coinHasHologram != null))

@Composable
private fun SummaryPill(title: String, value: String) {
    val c = LocalGoldExColors.current
    Surface(shape = RoundedCornerShape(9.dp), color = c.surfaceElevated,
        border = BorderStroke(1.dp, c.border)) {
        Row(Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, color = c.textSecondary, fontSize = 10.sp, fontFamily = VazirmatnFamily)
            Text(PersianNumberFormatter.toPersianDigits(value), color = c.goldPrimary,
                fontSize = 11.sp, fontFamily = VazirmatnFamily, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun QuantityControl(symbol: String, enabled: Boolean, onClick: () -> Unit) {
    val c = LocalGoldExColors.current
    Surface(onClick = onClick, enabled = enabled, shape = RoundedCornerShape(9.dp),
        modifier = Modifier.size(37.dp), color = c.surfaceElevated,
        border = BorderStroke(1.dp, c.goldBorder)) {
        Box(contentAlignment = Alignment.Center) {
            Text(symbol, color = if (enabled) c.goldPrimary else c.textMuted, fontSize = 20.sp)
        }
    }
}