package com.goldex.companion.desktop.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.goldex.companion.desktop.state.DesktopInventory
import com.goldex.companion.desktop.state.DesktopInventoryState
import com.goldex.companion.model.InventoryCategory
import com.goldex.companion.model.Karat
import com.goldex.companion.model.WageType
import com.goldex.companion.presentation.inventory.InventoryDraft
import com.goldex.companion.presentation.inventory.InventoryForm
import com.goldex.companion.ui.components.GoldButton
import com.goldex.companion.ui.components.GoldOutlinedTextField
import com.goldex.companion.ui.components.LuxurySegmentedControl
import com.goldex.companion.ui.theme.LocalGoldExColors
import com.goldex.companion.ui.theme.VazirmatnFamily
import com.goldex.companion.ui.theme.VazirmatnFeatureSettings

/** Inventory entry keeps item metadata separate from the sale valuation shown in item details. */
@Composable
internal fun DesktopInventoryProductDialog(draft: InventoryDraft, state: DesktopInventoryState, feature: DesktopInventory) {
    val colors = LocalGoldExColors.current
    val scroll = rememberScrollState()
    val titleFocus = remember { FocusRequester() }

    BoxWithConstraints {
        val dialogWidth = minOf(740.dp, maxWidth - 32.dp)
        val dialogHeight = maxHeight - 32.dp
        Dialog(onDismissRequest = feature::dismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                Surface(
                    Modifier.width(dialogWidth).heightIn(max = dialogHeight).testTag("inventory-dialog"),
                    shape = RoundedCornerShape(16.dp), color = colors.surface,
                    border = BorderStroke(0.6.dp, colors.goldBorder.copy(alpha = 0.5f)),
                    shadowElevation = if (colors.isDark) 0.dp else 2.dp
                ) {
                    Column {
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(shape = RoundedCornerShape(12.dp), color = colors.goldContainer) {
                                Icon(Icons.Outlined.Inventory2, null, Modifier.padding(12.dp).size(24.dp), tint = colors.goldPrimary)
                            }
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(if (draft.id == null) "افزودن محصول" else "ویرایش محصول", color = colors.textMain,
                                    fontSize = 18.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                                Text("مشخصات و موجودی کالا در ویترین و انبار", color = colors.textSecondary, fontSize = 12.sp)
                            }
                            IconButton(feature::dismiss, enabled = !state.saving) {
                                Icon(Icons.Outlined.Close, "بستن", tint = colors.textMuted)
                            }
                        }
                        HorizontalDivider(color = colors.goldBorder.copy(alpha = 0.35f))
                        Box(Modifier.weight(1f, fill = false).fillMaxWidth()) {
                            Column(
                                Modifier.fillMaxWidth().verticalScroll(scroll).testTag("inventory-form-scroll")
                                    .padding(horizontal = 24.dp, vertical = 20.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                if (state.error != null || draft.errors.isNotEmpty()) {
                                    Text(state.error ?: "اطلاعات مشخص‌شده را اصلاح کنید.", color = colors.errorRed, fontSize = 12.sp)
                                }
                                ProductField(draft, "title", "نام کالا", draft.title, feature,
                                    Modifier.fillMaxWidth().focusRequester(titleFocus), enabled = !state.saving) { it.copy(title = this) }
                                LaunchedEffect(draft.id) { titleFocus.requestFocus() }
                                LaunchedEffect(draft.errors) { if (draft.errors.isNotEmpty()) scroll.animateScrollTo(0) }
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("دسته‌بندی", color = colors.textSecondary, fontSize = 12.sp)
                                    LuxurySegmentedControl(
                                        InventoryCategory.values().filter { it != InventoryCategory.ALL }, draft.category,
                                        { category -> if (category != draft.category) feature.category(category) },
                                        label = { category -> when (category) {
                                            InventoryCategory.RINGS -> "انگشتر"
                                            InventoryCategory.BANGLES -> "النگو"
                                            InventoryCategory.NECKLACES -> "گردنبند"
                                            InventoryCategory.SETS -> "نیم‌ست"
                                            InventoryCategory.JEWELRY -> "جواهر"
                                            InventoryCategory.COINS -> "سکه"
                                            else -> "متفرقه"
                                        } }, modifier = Modifier.fillMaxWidth(), height = 44.dp, fontSize = 12.sp,
                                        isItemEnabled = { !state.saving }
                                    )
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                    ProductField(draft, "code", "کد کالا", draft.code, feature, Modifier.weight(1f), enabled = !state.saving) { it.copy(code = this) }
                                    ProductField(draft, "rfid", "بارکد / RFID (اختیاری)", draft.rfid, feature, Modifier.weight(1f), enabled = !state.saving) { it.copy(rfid = this) }
                                }

                                HorizontalDivider(color = colors.goldBorder.copy(alpha = 0.25f))
                                ProductSectionTitle("وزن و عیار", "وزن هر قطعه، تا سه رقم اعشار")
                                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                    ProductField(draft, "gross", "وزن ناخالص", draft.gross, feature, Modifier.weight(1f), numeric = true, unit = "گرم", enabled = !state.saving) { it.copy(gross = this) }
                                    ProductField(draft, "stone", "وزن نگین", draft.stone, feature, Modifier.weight(1f), numeric = true, unit = "گرم", enabled = !state.saving) { it.copy(stone = this) }
                                }
                                ProductPurity(draft, state.saving, feature)

                                HorizontalDivider(color = colors.goldBorder.copy(alpha = 0.25f))
                                ProductSectionTitle("اجرت و موجودی")
                                Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                    LuxurySegmentedControl(
                                        listOf(WageType.PERCENTAGE, WageType.TOMAN_PER_GRAM), draft.wageType,
                                        { type -> if (type != draft.wageType) feature.edit {
                                            it.copy(wageType = type, wage = if (type == WageType.PERCENTAGE) "10" else "0")
                                        } },
                                        label = { if (it == WageType.PERCENTAGE) "درصدی" else "هر گرم" },
                                        modifier = Modifier.weight(1f), height = 44.dp, fontSize = 12.sp,
                                        isItemEnabled = { !state.saving }
                                    )
                                    ProductField(draft, "wage", "اجرت", draft.wage, feature, Modifier.weight(1f), numeric = true,
                                        unit = if (draft.wageType == WageType.PERCENTAGE) "٪" else "تومان / گرم",
                                        monetary = draft.wageType == WageType.TOMAN_PER_GRAM, enabled = !state.saving) { it.copy(wage = this) }
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.Top) {
                                    ProductLocation(draft, state, feature, Modifier.weight(1f))
                                    ProductField(draft, "quantity", "تعداد", draft.quantity, feature, Modifier.weight(1f),
                                        numeric = true, unit = "قطعه", enabled = draft.id == null && !state.saving) { it.copy(quantity = this) }
                                }
                                if (draft.id != null) {
                                    Text("برای تغییر تعداد، از ورود و خروج موجودی استفاده کنید.", color = colors.textSecondary, fontSize = 12.sp)
                                }
                                Row(
                                    Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    InventoryImage(draft.image, Modifier.size(56.dp))
                                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text("تصویر کالا", color = colors.textMain, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                        Text("اختیاری • PNG یا JPEG", color = colors.textSecondary, fontSize = 12.sp)
                                    }
                                    if (draft.image.isNotBlank()) {
                                        IconButton({ feature.edit { it.copy(image = "") } }, enabled = !state.saving) {
                                            Icon(Icons.Outlined.DeleteOutline, "حذف تصویر", tint = colors.errorRed)
                                        }
                                    }
                                    GoldButton(if (draft.image.isBlank()) "انتخاب تصویر" else "تغییر تصویر",
                                        { selectInventoryPhoto(feature) }, Modifier.width(140.dp).testTag("inventory-photo"),
                                        isSecondary = true, enabled = !state.saving)
                                }
                            }
                            VerticalScrollbar(rememberScrollbarAdapter(scroll), Modifier.align(Alignment.CenterEnd).fillMaxHeight())
                        }
                        HorizontalDivider(color = colors.goldBorder.copy(alpha = 0.35f))
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically
                        ) {
                            GoldButton("انصراف", feature::dismiss, Modifier.width(120.dp), isSecondary = true, enabled = !state.saving)
                            GoldButton(if (state.saving) "در حال ذخیره" else if (draft.id == null) "ثبت در انبار" else "ذخیره تغییرات",
                                { feature.save() }, Modifier.width(180.dp).testTag("inventory-save"), enabled = !state.saving)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProductSectionTitle(title: String, hint: String? = null) {
    val colors = LocalGoldExColors.current
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(title, color = colors.textMain, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        hint?.let { Text(it, color = colors.textSecondary, fontSize = 12.sp) }
    }
}

@Composable
private fun ProductField(
    draft: InventoryDraft, key: String, label: String, value: String, feature: DesktopInventory,
    modifier: Modifier = Modifier, numeric: Boolean = false, unit: String? = null,
    monetary: Boolean = false, enabled: Boolean = true, change: String.(InventoryDraft) -> InventoryDraft
) {
    DesktopField(value, { text -> feature.edit { text.change(it) } }, label, modifier.testTag("inventory-$key"),
        numeric = numeric, error = draft.errors[key], enabled = enabled, monetary = monetary, unit = unit)
}

@Composable
private fun ProductPurity(draft: InventoryDraft, saving: Boolean, feature: DesktopInventory) {
    val colors = LocalGoldExColors.current
    val presets = listOf("750", "705", "875", "999")
    var custom by remember(draft.id) { mutableStateOf(draft.purity !in presets) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("عیار طلا", color = colors.textSecondary, fontSize = 12.sp)
        LuxurySegmentedControl(presets + "custom", if (custom) "custom" else draft.purity, { purity ->
            val wasCustom = custom
            custom = purity == "custom"
            if (wasCustom && custom) return@LuxurySegmentedControl
            feature.edit { it.copy(purity = if (purity == "custom") "" else purity,
                karat = when (purity) { "875" -> Karat.K21; "999" -> Karat.K24; else -> Karat.K18 }) }
        }, label = { when (it) { "750" -> "۱۸ (۷۵۰)"; "705" -> "۱۷ (۷۰۵)"; "875" -> "۲۱ (۸۷۵)"; "999" -> "۲۴ (۹۹۹)"; else -> "سفارشی" } },
            modifier = Modifier.fillMaxWidth(), height = 44.dp, fontSize = 12.sp, isItemEnabled = { !saving })
        if (custom) {
            ProductField(draft, "purity", "عیار دقیق", draft.purity, feature, Modifier.fillMaxWidth(), numeric = true, unit = "از ۱۰۰۰", enabled = !saving) {
                it.copy(purity = this, karat = when (InventoryForm.normalize(this)) { "875" -> Karat.K21; "999" -> Karat.K24; else -> Karat.K18 })
            }
        } else draft.errors["purity"]?.let { Text(it, color = colors.errorRed, fontSize = 12.sp) }
    }
}

@Composable
private fun ProductLocation(draft: InventoryDraft, state: DesktopInventoryState, feature: DesktopInventory, modifier: Modifier) {
    val colors = LocalGoldExColors.current
    var expanded by remember { mutableStateOf(false) }
    val locations = remember(state.items) {
        (listOf("ویترین اصلی", "گاوصندوق مرکزی", "خزانه طلا") + state.items.map { it.location }).filter { it.isNotBlank() }.distinct()
    }
    Column(modifier.testTag("inventory-location"), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Box {
            GoldOutlinedTextField(
                value = draft.location, onValueChange = { value -> feature.edit { it.copy(location = value) } },
                modifier = Modifier.fillMaxWidth(), label = { Text("محل نگهداری", fontSize = 13.sp) },
                singleLine = true, enabled = !state.saving, isError = draft.errors["location"] != null,
                textStyle = TextStyle(fontFamily = VazirmatnFamily, fontFeatureSettings = VazirmatnFeatureSettings,
                    fontSize = 15.sp, color = colors.textMain, textDirection = TextDirection.ContentOrRtl),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = colors.goldPrimary,
                    focusedLabelColor = colors.goldPrimary, unfocusedBorderColor = colors.border,
                    unfocusedLabelColor = colors.textMuted, focusedContainerColor = colors.surface,
                    unfocusedContainerColor = colors.surface, cursorColor = colors.goldPrimary),
                trailingIcon = { IconButton({ expanded = !expanded }, enabled = !state.saving) {
                    Icon(Icons.Outlined.ArrowDropDown, "انتخاب محل نگهداری", tint = colors.textMuted)
                } }
            )
            DropdownMenu(expanded, { expanded = false }) {
                locations.forEach { location ->
                    DropdownMenuItem(text = { Text(location, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        onClick = { feature.edit { it.copy(location = location) }; expanded = false })
                }
            }
        }
        draft.errors["location"]?.let { Text(it, color = colors.errorRed, fontSize = 11.sp) }
        Text("انتخاب کنید یا نام محل جدید را بنویسید.", color = colors.textSecondary, fontSize = 11.sp)
    }
}
