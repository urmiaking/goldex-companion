package com.goldex.companion.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.goldex.companion.model.InventoryItem
import com.goldex.companion.ui.theme.LocalGoldExColors
import com.goldex.companion.ui.theme.VazirmatnFamily

/** Platform-neutral picker: an invoice draft never reserves or deducts stock. */
@Composable
fun InventorySalePickerContent(
    products: List<InventoryItem>,
    onDismiss: () -> Unit,
    onConfirm: (String, Int) -> Unit
) {
    val colors = LocalGoldExColors.current
    var query by remember { mutableStateOf("") }
    var selectedId by remember { mutableStateOf<String?>(null) }
    var countText by remember { mutableStateOf("1") }
    val selected = products.firstOrNull { it.id == selectedId }
    val count = countText.map { digit ->
        when (digit) {
            in '۰'..'۹' -> ('0'.code + (digit.code - '۰'.code)).toChar()
            in '٠'..'٩' -> ('0'.code + (digit.code - '٠'.code)).toChar()
            else -> digit
        }
    }.joinToString("").toIntOrNull()
    val filtered = products.filter {
        query.isBlank() || it.title.contains(query.trim(), ignoreCase = true) ||
            it.code.contains(query.trim(), ignoreCase = true) ||
            it.rfidTag.contains(query.trim(), ignoreCase = true)
    }
    Surface(
        modifier = Modifier.fillMaxWidth().widthIn(max = 520.dp),
        shape = RoundedCornerShape(20.dp),
        color = colors.surface,
        border = BorderStroke(1.dp, colors.goldBorder)
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("انتخاب کالا از انبار", fontFamily = VazirmatnFamily, fontSize = 17.sp,
                fontWeight = FontWeight.Bold, color = colors.textMain)
            Text("موجودی فقط پس از ثبت نهایی فاکتور کسر می‌شود.", fontFamily = VazirmatnFamily,
                fontSize = 11.sp, color = colors.textSecondary)
            Text("فروش انباری سکه به دلیل نیاز به تعیین نوع و قیمت واحد، فعلاً جداگانه انجام می‌شود.",
                fontFamily = VazirmatnFamily, fontSize = 10.sp, color = colors.textMuted)
            OutlinedTextField(
                value = query, onValueChange = { query = it },
                label = { Text("جست‌وجو با نام، کد یا برچسب کالا", fontFamily = VazirmatnFamily) },
                singleLine = true, modifier = Modifier.fillMaxWidth()
            )
            Column(Modifier.fillMaxWidth().heightIn(max = 310.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(6.dp)) {
                if (filtered.isEmpty()) Text("کالایی با موجودی مثبت پیدا نشد.", color = colors.textMuted,
                    fontFamily = VazirmatnFamily)
                filtered.forEach { item ->
                    Surface(
                        color = if (selectedId == item.id) colors.goldContainer else colors.surfaceElevated,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(0.8.dp, if (selectedId == item.id) colors.goldBorder else colors.border),
                        modifier = Modifier.fillMaxWidth().clickable {
                            selectedId = item.id
                            countText = "1"
                        }
                    ) {
                        Column(Modifier.padding(horizontal = 12.dp, vertical = 9.dp)) {
                            Text(item.title, color = colors.textMain, fontFamily = VazirmatnFamily,
                                fontWeight = FontWeight.Medium, fontSize = 13.sp)
                            Text("کد: ${item.code}  •  موجودی: ${item.quantity} عدد  •  وزن: ${item.netGoldWeightGrams} گرم",
                                color = colors.textSecondary, fontFamily = VazirmatnFamily, fontSize = 11.sp)
                        }
                    }
                }
            }
            if (selected != null) {
                OutlinedTextField(value = countText, onValueChange = { countText = it },
                    label = { Text("تعداد فروش (حداکثر ${selected.quantity})", fontFamily = VazirmatnFamily) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true, modifier = Modifier.fillMaxWidth())
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onDismiss) { Text("انصراف", fontFamily = VazirmatnFamily) }
                Spacer(Modifier.weight(1f))
                Button(onClick = { selected?.let { onConfirm(it.id, count!!) } },
                    enabled = selected != null && count != null && count in 1..selected.quantity) {
                    Text("افزودن به فاکتور", fontFamily = VazirmatnFamily)
                }
            }
        }
    }
}
