package com.goldex.companion.desktop.ui

import com.goldex.companion.desktop.ui.controls.*

import com.goldex.companion.desktop.ui.controls.DesktopIconButton as IconButton

import com.goldex.companion.desktop.ui.controls.DesktopActionButton as GoldButton
import com.goldex.companion.desktop.ui.controls.DesktopOutlinedTextField
import com.goldex.companion.desktop.ui.controls.DesktopChoiceRow

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import com.goldex.companion.desktop.ui.controls.DesktopDialog as Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.IntOffset
import com.goldex.companion.desktop.state.DesktopInventory
import com.goldex.companion.desktop.state.DesktopInventoryState
import com.goldex.companion.desktop.DesktopInlineCustomKaratChip
import com.goldex.companion.model.CoinType
import com.goldex.companion.model.InventoryCategory
import com.goldex.companion.model.Karat
import com.goldex.companion.model.PersianNumberFormatter
import com.goldex.companion.model.WageType
import com.goldex.companion.ui.components.AnimatedPriceText
import com.goldex.companion.presentation.inventory.InventoryDraft
import com.goldex.companion.presentation.inventory.InventoryForm
import com.goldex.companion.ui.theme.LocalGoldExColors
import com.goldex.companion.ui.theme.VazirmatnFamily
import com.goldex.companion.ui.theme.VazirmatnFeatureSettings
import kotlinx.coroutines.launch

/** Inventory entry keeps item metadata separate from the sale valuation shown in item details. */
@Composable
internal fun DesktopInventoryProductDialog(draft: InventoryDraft, state: DesktopInventoryState, feature: DesktopInventory) {
    val colors = LocalGoldExColors.current
    val scroll = rememberScrollState()
    val titleFocus = remember { FocusRequester() }

    BoxWithConstraints {
        val dialogWidth = minOf(1040.dp, maxWidth - 32.dp)
        val dialogHeight = minOf(580.dp, maxHeight - 32.dp)
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
                                val isCoinCategory = draft.category == InventoryCategory.COINS
                                if (isCoinCategory) {
                                    // Dedicated Bank Coin Registration Header & Selector
                                    ProductSectionTitle("انتخاب مسکوک بانکی", "ضرب رسمی بانک مرکزی و صنف طلا")
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        CoinType.entries.forEach { c ->
                                            val isSelected = (draft.coinType ?: CoinType.EMAMI) == c
                                            val liveCoinPrice = state.rates.getCoinPrice(c)
                                            Surface(
                                                shape = RoundedCornerShape(10.dp),
                                                color = if (isSelected) colors.goldContainer.copy(alpha = 0.5f) else colors.surfaceElevated,
                                                border = BorderStroke(
                                                    if (isSelected) 1.2.dp else 0.8.dp,
                                                    if (isSelected) colors.goldPrimary else colors.border.copy(alpha = 0.6f)
                                                ),
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .clickable(enabled = !state.saving) {
                                                        feature.coinType(c)
                                                    }
                                            ) {
                                                Column(
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                                ) {
                                                    Text(
                                                        text = c.titleFa,
                                                        fontSize = 11.sp,
                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                        color = if (isSelected) colors.goldPrimary else colors.textMain,
                                                        fontFamily = VazirmatnFamily,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        modifier = Modifier.fillMaxWidth()
                                                    ) {
                                                        Text(
                                                            text = "${PersianNumberFormatter.formatWeight(c.totalWeightGrams)} گرم",
                                                            fontSize = 10.sp,
                                                            color = colors.textMuted,
                                                            fontFamily = VazirmatnFamily
                                                        )
                                                        if (liveCoinPrice > 0L) {
                                                            Text(
                                                                text = "${PersianNumberFormatter.formatPrice(liveCoinPrice.toDouble())} ت",
                                                                fontSize = 10.sp,
                                                                fontWeight = FontWeight.SemiBold,
                                                                color = if (isSelected) colors.goldPrimary else colors.textSecondary,
                                                                fontFamily = VazirmatnFamily
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.Top) {
                                        ProductField(draft, "title", "نام کامل مسکوک بانکی", draft.title, feature,
                                            Modifier.weight(2f).focusRequester(titleFocus), enabled = !state.saving) { it.copy(title = this) }
                                        ProductDropdown("دسته‌بندی", state.draftCategoryTitle,
                                            InventoryCategory.values().filter { it != InventoryCategory.ALL }.map { it.name to it.titleFa } +
                                                state.options.categories.map { it.id to it.name },
                                            Modifier.weight(1f), "inventory-category", state.saving, "افزودن دسته‌بندی",
                                            onSelect = { id -> InventoryCategory.values().firstOrNull { it.name == id }?.let(feature::category)
                                                ?: feature.customCategory(id) },
                                            onAdd = { name -> feature.addCategory(name)?.join(); feature.state.value.draftCategoryTitle == name.trim() })
                                    }
                                    LaunchedEffect(draft.id) { titleFocus.requestFocus() }
                                    LaunchedEffect(draft.errors) { if (draft.errors.isNotEmpty()) scroll.animateScrollTo(0) }

                                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.Top) {
                                        ProductDropdown("محل نگهداری", draft.location, state.locations.map { it to it },
                                            Modifier.weight(1f), "inventory-location", state.saving, "افزودن محل",
                                            onSelect = { value -> feature.edit { it.copy(location = value) } },
                                            onAdd = { name -> feature.addLocation(name)?.join(); feature.state.value.draft?.location == name.trim() })
                                        ProductField(draft, "rfid", "شناسه وکیوم / بارکد (اختیاری)", draft.rfid, feature, Modifier.weight(1f), enabled = !state.saving) { it.copy(rfid = this) }
                                        ProductField(draft, "quantity", "تعداد سکه", draft.quantity, feature, Modifier.weight(0.6f),
                                            numeric = true, unit = "عدد", enabled = draft.id == null && !state.saving) { it.copy(quantity = this) }
                                    }

                                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                        ProductField(draft, "unitPrice", "قیمت واحد روز (تومان)", draft.unitPrice, feature,
                                            Modifier.weight(1f), numeric = true, monetary = true, unit = "تومان", enabled = !state.saving) { it.copy(unitPrice = this) }

                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = colors.surfaceElevated,
                                            border = BorderStroke(0.8.dp, colors.border.copy(alpha = 0.6f)),
                                            modifier = Modifier.weight(1f).height(48.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column {
                                                    Text("بسته‌بندی و هولوگرام معتبر بانکی", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, color = colors.textMain, fontFamily = VazirmatnFamily)
                                                    Text("دارای تاییدیه رسمی اتحادیه طلا و جواهر", fontSize = 10.sp, color = colors.textSecondary, fontFamily = VazirmatnFamily)
                                                }
                                                Switch(
                                                    checked = draft.hasHologram,
                                                    onCheckedChange = { checked -> feature.edit { it.copy(hasHologram = checked) } },
                                                    enabled = !state.saving,
                                                    colors = SwitchDefaults.colors(checkedThumbColor = colors.goldPrimary, checkedTrackColor = colors.goldContainer)
                                                )
                                            }
                                        }
                                    }

                                    // Live Valuation Summary Card for Coins
                                    val coinCount = (draft.quantity.toIntOrNull() ?: 1).coerceAtLeast(0)
                                    val unitPriceLong = PersianNumberFormatter.parseToCleanLong(draft.unitPrice) ?: (draft.coinType?.let { state.rates.getCoinPrice(it) } ?: 0L)
                                    val totalCoinPayable = coinCount.toLong() * unitPriceLong
                                    val coinWeight = draft.gross.toDoubleOrNull() ?: (draft.coinType?.totalWeightGrams ?: 8.136)
                                    val eq18kGrams = coinCount * coinWeight * (900.0 / 750.0)

                                    Surface(
                                        shape = RoundedCornerShape(16.dp),
                                        color = colors.surfaceElevated,
                                        border = BorderStroke(1.dp, colors.goldBorder.copy(alpha = 0.8f)),
                                        shadowElevation = if (colors.isDark) 0.dp else 2.dp,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                                Surface(
                                                    shape = RoundedCornerShape(8.dp),
                                                    color = colors.goldPrimary.copy(alpha = 0.15f),
                                                    border = BorderStroke(0.6.dp, colors.goldPrimary.copy(alpha = 0.4f))
                                                ) {
                                                    Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                        Icon(Icons.Outlined.Paid, null, tint = colors.goldPrimary, modifier = Modifier.size(15.dp))
                                                        Text("برآورد ارزش مسکوکات بانکی", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = colors.goldPrimary)
                                                    }
                                                }
                                                Text(
                                                    if (draft.hasHologram) "دارای وکیوم و هولوگرام بانکی" else "سکه باز (بدون وکیوم)",
                                                    fontSize = 11.sp,
                                                    color = if (draft.hasHologram) colors.profitGreen else colors.textMuted,
                                                    fontWeight = FontWeight.Medium
                                                )
                                            }
                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                                Text("ارزش کل موجودی سکه‌ها:", fontSize = 12.sp, color = colors.textSecondary)
                                                AnimatedPriceText(amount = totalCoinPayable, unit = "تومان", color = colors.goldPrimary, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                                            }
                                            HorizontalDivider(color = colors.goldBorder.copy(alpha = 0.25f))
                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                Column {
                                                    Text("معادل وزنی ۱۸ عیار:", fontSize = 10.5.sp, color = colors.textSecondary)
                                                    Text("${PersianNumberFormatter.formatWeight(eq18kGrams)} گرم", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = colors.textMain)
                                                }
                                                Column {
                                                    Text("وزن کل ناخالص (۹۰۰ عیار):", fontSize = 10.5.sp, color = colors.textSecondary)
                                                    Text("${PersianNumberFormatter.formatWeight(coinCount * coinWeight)} گرم", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = colors.textMain)
                                                }
                                                Column {
                                                    Text("سود و مالیات مصوب:", fontSize = 10.5.sp, color = colors.textSecondary)
                                                    Text("۰٪ (معاف)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = colors.profitGreen)
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    // Regular Jewelry Layout
                                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.Top) {
                                        ProductField(draft, "title", "نام کالا", draft.title, feature,
                                            Modifier.weight(2f).focusRequester(titleFocus), enabled = !state.saving) { it.copy(title = this) }
                                        ProductDropdown("دسته‌بندی", state.draftCategoryTitle,
                                            InventoryCategory.values().filter { it != InventoryCategory.ALL }.map { it.name to it.titleFa } +
                                                state.options.categories.map { it.id to it.name },
                                            Modifier.weight(1f), "inventory-category", state.saving, "افزودن دسته‌بندی",
                                            onSelect = { id -> InventoryCategory.values().firstOrNull { it.name == id }?.let(feature::category)
                                                ?: feature.customCategory(id) },
                                            onAdd = { name -> feature.addCategory(name)?.join(); feature.state.value.draftCategoryTitle == name.trim() })
                                    }
                                    LaunchedEffect(draft.id) { titleFocus.requestFocus() }
                                    LaunchedEffect(draft.errors) { if (draft.errors.isNotEmpty()) scroll.animateScrollTo(0) }
                                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.Top) {
                                        ProductDropdown("محل نگهداری", draft.location, state.locations.map { it to it },
                                            Modifier.weight(1f), "inventory-location", state.saving, "افزودن محل",
                                            onSelect = { value -> feature.edit { it.copy(location = value) } },
                                            onAdd = { name -> feature.addLocation(name)?.join(); feature.state.value.draft?.location == name.trim() })
                                        ProductField(draft, "rfid", "بارکد / RFID (اختیاری)", draft.rfid, feature, Modifier.weight(1f), enabled = !state.saving) { it.copy(rfid = this) }
                                        ProductField(draft, "quantity", "تعداد", draft.quantity, feature, Modifier.weight(0.6f),
                                            numeric = true, unit = "قطعه", enabled = draft.id == null && !state.saving) { it.copy(quantity = this) }
                                    }

                                    HorizontalDivider(color = colors.goldBorder.copy(alpha = 0.25f))
                                    Row(horizontalArrangement = Arrangement.spacedBy(24.dp), verticalAlignment = Alignment.Top) {
                                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    ProductSectionTitle("وزن و عیار", "وزن هر قطعه، تا سه رقم اعشار")
                                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                        ProductField(draft, "gross", "وزن ناخالص", draft.gross, feature, Modifier.weight(1f), numeric = true, unit = "گرم", enabled = !state.saving) { it.copy(gross = this) }
                                        ProductField(draft, "stone", "وزن نگین", draft.stone, feature, Modifier.weight(1f), numeric = true, unit = "گرم", enabled = !state.saving) { it.copy(stone = this) }
                                    }
                                    ProductPurity(draft, state.saving, feature)
                                    }
                                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    ProductSectionTitle("اجرت و موجودی")
                                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                        DesktopChoiceRow(
                                            listOf(WageType.PERCENTAGE, WageType.TOMAN_PER_GRAM), draft.wageType,
                                            { type -> if (type != draft.wageType) feature.edit {
                                                it.copy(wageType = type, wage = if (type == WageType.PERCENTAGE) "10" else "0")
                                            } },
                                            label = { if (it == WageType.PERCENTAGE) "درصدی" else "هر گرم" },
                                            modifier = Modifier.weight(1f),
                                            isItemEnabled = { !state.saving }
                                        )
                                        ProductField(draft, "wage", "اجرت", draft.wage, feature, Modifier.weight(1f), numeric = true,
                                            unit = if (draft.wageType == WageType.PERCENTAGE) "٪" else "تومان / گرم",
                                            monetary = draft.wageType == WageType.TOMAN_PER_GRAM, enabled = !state.saving) { it.copy(wage = this) }
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
                                            Text("تصویر کالا", color = colors.textMain, fontSize = 13.sp, fontWeight = FontWeight.Medium, maxLines = 1)
                                            Text("اختیاری", color = colors.textSecondary, fontSize = 12.sp, maxLines = 1)
                                        }
                                        if (draft.image.isNotBlank()) {
                                            IconButton({ feature.edit { it.copy(image = "") } }, enabled = !state.saving) {
                                                Icon(Icons.Outlined.DeleteOutline, "حذف تصویر", tint = colors.errorRed)
                                            }
                                        }
                                        GoldButton(if (draft.image.isBlank()) "انتخاب تصویر" else "تغییر تصویر",
                                            { selectInventoryPhoto(feature) }, Modifier.width(140.dp).testTag("inventory-photo"),
                                            isSecondary = true, enabled = !state.saving, icon = Icons.Outlined.Image)
                                    }
                                    }
                                    }
                                }
                            }
                            VerticalScrollbar(rememberScrollbarAdapter(scroll), Modifier.align(Alignment.CenterEnd).fillMaxHeight())
                        }
                        HorizontalDivider(color = colors.goldBorder.copy(alpha = 0.35f))
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically
                        ) {
                            GoldButton("انصراف", feature::dismiss, Modifier.width(120.dp), isSecondary = true, enabled = !state.saving, icon = Icons.Outlined.Close)
                            GoldButton(if (state.saving) "در حال ذخیره" else if (draft.id == null) "ثبت در انبار" else "ذخیره تغییرات",
                                { feature.save() }, Modifier.width(180.dp).testTag("inventory-save"), enabled = !state.saving, icon = Icons.Outlined.Save)
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
        DesktopChoiceFlow(presets + "custom", if (custom) "custom" else draft.purity,
            onItemSelected = { purity ->
                custom = purity == "custom"
                feature.edit { if (custom) it.copy(purity = "") else it.copy(purity = purity,
                    karat = when (purity) { "875" -> Karat.K21; "999" -> Karat.K24; else -> Karat.K18 }) }
            }, label = { purity -> when (purity) {
                "750" -> "۱۸ (۷۵۰)"; "705" -> "۱۷ (۷۰۵)"; "875" -> "۲۱ (۸۷۵)"; "999" -> "۲۴ (۹۹۹)"; else -> "سفارشی"
            } }, enabled = !saving)
        if (custom) Box(Modifier.fillMaxWidth().testTag("inventory-purity")) {
            DesktopOutlinedTextField(draft.purity, { typed ->
                val value = com.goldex.companion.model.PersianNumberFormatter.toEnglishDigits(typed.filter { it.isDigit() })
                feature.edit { it.copy(purity = value, karat = when (value) {
                    "875" -> Karat.K21; "999", "1000" -> Karat.K24; else -> Karat.K18
                }) }
            }, Modifier.fillMaxWidth().testTag("inventory-custom-purity"), enabled = !saving,
                label = { Text("عیار سفارشی", fontSize = 13.sp) }, singleLine = true,
                textStyle = TextStyle(fontFamily = VazirmatnFamily, fontFeatureSettings = VazirmatnFeatureSettings,
                    fontSize = 15.sp, color = colors.textMain, textDirection = TextDirection.Ltr),
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number))
        }
        draft.errors["purity"]?.let { Text(it, color = colors.errorRed, fontSize = 12.sp) }
    }
}

/** A single menu contains existing options and the inline creation form. */
@Composable
private fun ProductDropdown(
    label: String, value: String, choices: List<Pair<String, String>>,
    modifier: Modifier, tag: String, saving: Boolean, addLabel: String,
    onSelect: (String) -> Unit, onAdd: suspend (String) -> Boolean
) {
    val colors = LocalGoldExColors.current
    val scope = rememberCoroutineScope()
    var expanded by remember { mutableStateOf(false) }
    var adding by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val inputFocus = remember { FocusRequester() }
    fun submit() {
        val clean = name.trim()
        if (clean.isEmpty() || clean.length > 50) { error = "نام را وارد کنید؛ حداکثر ۵۰ حرف"; return }
        choices.firstOrNull { it.second == clean }?.let {
            onSelect(it.first); expanded = false; return
        }
        scope.launch {
            if (onAdd(clean)) { expanded = false; adding = false; name = ""; error = null }
            else error = "ثبت نشد؛ دوباره تلاش کنید"
        }
    }
    Box(modifier.testTag(tag)) {
        DesktopOutlinedTextField(
            value, {}, Modifier.fillMaxWidth(), label = { Text(label, fontSize = 13.sp) },
            readOnly = true, enabled = !saving, singleLine = true,
            textStyle = TextStyle(fontFamily = VazirmatnFamily, fontFeatureSettings = VazirmatnFeatureSettings,
                fontSize = 15.sp, color = colors.textMain, textDirection = TextDirection.ContentOrRtl),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = colors.goldPrimary,
                focusedLabelColor = colors.goldPrimary, unfocusedBorderColor = colors.border,
                unfocusedLabelColor = colors.textMuted, focusedContainerColor = colors.surface,
                unfocusedContainerColor = colors.surface, cursorColor = colors.goldPrimary),
            trailingIcon = { Icon(Icons.Outlined.ArrowDropDown, null, tint = colors.textMuted) }
        )
        Box(Modifier.matchParentSize().testTag("$tag-open")
            .semantics { contentDescription = label }
            .clip(DesktopControls.BodyShape)
            .clickable(enabled = !saving, role = Role.DropdownList) { expanded = true; adding = false; error = null })
        if (expanded) Popup(
            popupPositionProvider = InventoryMenuPosition,
            onDismissRequest = { if (!saving) expanded = false },
            properties = PopupProperties(focusable = true)
        ) {
            Surface(shape = RoundedCornerShape(16.dp), color = colors.surface,
                border = BorderStroke(0.6.dp, colors.goldBorder.copy(alpha = 0.5f)),
                shadowElevation = if (colors.isDark) 0.dp else 2.dp) {
            Column(Modifier.width(320.dp).heightIn(max = 360.dp).verticalScroll(rememberScrollState()).testTag("$tag-menu")) {
            if (!adding) {
                choices.forEach { (id, text) ->
                    DropdownMenuItem(text = { Text(text, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        onClick = { onSelect(id); expanded = false }, enabled = !saving,
                        trailingIcon = if (text == value) { { Icon(Icons.Outlined.Check, null, tint = colors.goldPrimary, modifier = Modifier.size(18.dp)) } } else null)
                }
                HorizontalDivider(color = colors.goldBorder.copy(alpha = 0.35f))
                DropdownMenuItem(
                    text = { Text(addLabel, color = colors.goldPrimary) },
                    leadingIcon = { Icon(Icons.Outlined.Add, null, tint = colors.goldPrimary) },
                    modifier = Modifier.testTag("$tag-add"), enabled = !saving,
                    onClick = { adding = true; name = ""; error = null })
            } else {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(addLabel, color = colors.textMain, fontWeight = FontWeight.Bold)
                    DesktopOutlinedTextField(name, { name = it; error = null },
                        Modifier.fillMaxWidth().focusRequester(inputFocus).testTag("$tag-new"),
                        label = { Text("نام جدید") }, singleLine = true, enabled = !saving, isError = error != null,
                        textStyle = TextStyle(fontFamily = VazirmatnFamily, fontFeatureSettings = VazirmatnFeatureSettings),
                        keyboardActions = KeyboardActions(onDone = { submit() }))
                    LaunchedEffect(Unit) { inputFocus.requestFocus() }
                    error?.let { Text(it, color = colors.errorRed, fontSize = 12.sp) }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        GoldButton("بازگشت", { adding = false }, Modifier.weight(1f), isSecondary = true, enabled = !saving, icon = Icons.Outlined.ArrowForward)
                        GoldButton(if (saving) "در حال ثبت" else "افزودن", { submit() },
                            Modifier.weight(1f).testTag("$tag-create"), enabled = !saving, icon = Icons.Outlined.Save)
                    }
                }
            }
        }
        }
        }
    }
}

private object InventoryMenuPosition : PopupPositionProvider {
    override fun calculatePosition(anchorBounds: IntRect, windowSize: IntSize, layoutDirection: LayoutDirection, popupContentSize: IntSize): IntOffset {
        val x = (anchorBounds.right - popupContentSize.width).coerceIn(0, (windowSize.width - popupContentSize.width).coerceAtLeast(0))
        val y = if (anchorBounds.bottom + popupContentSize.height <= windowSize.height) anchorBounds.bottom
            else (anchorBounds.top - popupContentSize.height).coerceAtLeast(0)
        return IntOffset(x, y)
    }
}
