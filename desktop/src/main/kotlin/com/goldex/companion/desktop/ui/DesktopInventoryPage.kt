package com.goldex.companion.desktop.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
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

@Composable internal fun DesktopInventoryPage(workspace: WorkspaceState, feature: DesktopInventory) {
    val state by feature.state.collectAsState()
    val colors = LocalGoldExColors.current
    Column(Modifier.fillMaxSize().testTag("inventory-page"), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            DesktopField(state.query, feature::search, "جست‌وجوی نام، کد، RFID یا محل", Modifier.weight(1f).testTag("inventory-search"))
            OutlinedIconButton({ feature.showHistory(true) }, Modifier.size(48.dp).testTag("inventory-history"), shape = ButtonShape, border = BorderStroke(.6.dp, colors.goldBorder)) { Icon(Icons.Outlined.History, "تاریخچهٔ ورود و خروج", tint = colors.goldPrimary) }
            GoldButton("کالای جدید", { feature.open() }, Modifier.width(155.dp).testTag("inventory-add"), icon = Icons.Outlined.Add, enabled = !state.saving)
        }
        Column(Modifier.fillMaxWidth().background(colors.heroCardGradient, RoundedCornerShape(18.dp)).border(.8.dp, colors.goldBorder.copy(alpha = .6f), RoundedCornerShape(18.dp)).padding(18.dp).testTag("inventory-summary"), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Inventory2, null, tint = colors.goldSecondary, modifier = Modifier.size(20.dp))
                Text("موجودی ویترین و گاوصندوق", Modifier.weight(1f).padding(start = 8.dp), color = Color.White, fontWeight = FontWeight.SemiBold)
                IconButton({ feature.togglePrivacy() }, Modifier.testTag("inventory-privacy"), enabled = !state.saving) { Icon(if (state.visible) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff, if (state.visible) "پنهان‌کردن موجودی" else "نمایش موجودی", tint = colors.goldSecondary) }
            }
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text("برآورد ارزش طلای موجودی", color = Color.White.copy(alpha = .75f), fontSize = 11.sp)
                    Amount(if (state.visible) state.metalValue?.let(PersianNumberFormatter::formatPrice) else "••••", color = colors.goldSecondary, size = 26)
                }
                Text("تومان • بدون اجرت و سود", color = Color.White.copy(alpha = .75f), fontSize = 11.sp, modifier = Modifier.padding(bottom = 4.dp))
            }
            HorizontalDivider(color = Color.White.copy(alpha = .15f))
            val summary = state.summary
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                InventoryMetric("طلای معادل ۱۸", if (state.visible) PersianNumberFormatter.formatWeight(summary.gold18) else "••••", "گرم", Modifier.weight(1f))
                InventoryMetric("مظنهٔ وزنی", if (state.visible) PersianNumberFormatter.formatWeight(summary.mesghal) else "••••", "مثقال", Modifier.weight(1f))
                InventoryMetric("تعداد قطعات", if (state.visible) digits(summary.pieces) else "••••", "قطعه", Modifier.weight(1f))
                InventoryMetric("محل نگهداری", if (state.visible) "${digits(summary.trays.toLong())} / ${digits(summary.safes.toLong())}" else "••••", "ویترین / انبار", Modifier.weight(1f))
            }
        }
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val categoryWidth = maxOf(maxWidth, 650.dp)
            Box(Modifier.horizontalScroll(rememberScrollState())) {
                LuxurySegmentedControl(InventoryCategory.values().toList(), state.category, feature::filter,
                    label = { categoryShort(it) }, modifier = Modifier.width(categoryWidth).testTag("inventory-categories"), height = 40.dp, fontSize = 12.sp)
            }
        }
        if (state.error != null || state.notice != null) {
            Surface(color = if (state.error != null) colors.errorRed.copy(alpha = .08f) else colors.goldContainer, shape = RoundedCornerShape(12.dp)) {
                Row(Modifier.fillMaxWidth().padding(start = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(state.error ?: state.notice.orEmpty(), Modifier.weight(1f).testTag("inventory-message"), color = if (state.error != null) colors.errorRed else colors.textMain, fontSize = 12.sp)
                    IconButton(feature::clearMessage) { Icon(Icons.Outlined.Close, "بستن پیام", tint = colors.textMuted) }
                }
            }
        }
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
            val wide = maxWidth >= 900.dp
            Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("${digits(state.filtered.size.toLong())} کالا • ${state.category.titleFa}", Modifier.weight(1f), color = colors.textMuted, fontSize = 12.sp)
                        Text("وزن‌ها برای هر قطعه", color = colors.textMuted, fontSize = 11.sp)
                    }
                    if (state.filtered.isEmpty()) LuxuryCard(Modifier.fillMaxWidth()) {
                        Icon(Icons.Outlined.Diamond, null, Modifier.size(40.dp), tint = colors.goldPrimary)
                        PageTitle(if (state.items.isEmpty()) "ویترین شما از اینجا شروع می‌شود" else "کالایی پیدا نشد")
                        Text(if (state.items.isEmpty()) "اولین قطعه را با وزن، عیار و اجرت ثبت کنید؛ سپس ورود و خروج آن را در همین انبار مدیریت کنید." else "کد یا نام کوتاه‌تری جست‌وجو کنید یا دسته را تغییر دهید.", color = colors.textMuted, fontSize = 13.sp)
                        if (state.items.isNotEmpty()) TextButton({ feature.search(""); feature.filter(InventoryCategory.ALL) }) { Text("پاک‌کردن فیلترها") }
                    } else {
                        val scroll = rememberLazyListStateCompat()
                        Box(Modifier.weight(1f)) {
                            LazyColumn(state = scroll, modifier = Modifier.fillMaxSize().padding(end = 12.dp).testTag("inventory-list"), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                items(state.filtered, key = { it.id }) { item ->
                                    Surface(color = if (state.selectedId == item.id) colors.goldContainer else colors.surface,
                                        shape = RoundedCornerShape(16.dp), border = BorderStroke(.6.dp, colors.goldBorder.copy(alpha = if (state.selectedId == item.id) 1f else .5f)),
                                        shadowElevation = if (colors.isDark) 0.dp else 2.dp,
                                        modifier = Modifier.fillMaxWidth().clickable { feature.select(item.id) }.testTag("inventory-item-${item.id}")) {
                                        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                            InventoryImage(item.imageUrl, Modifier.size(58.dp))
                                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                                Text(item.title, color = colors.textMain, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                Text("${PersianNumberFormatter.toPersianDigits(item.code)} • ${item.category.titleFa}", color = colors.textMuted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                Text("${PersianNumberFormatter.formatWeight(item.netGoldWeightGrams)} گرم خالص • عیار ${digits(item.customKaratValue.toLong())}", color = colors.textSecondary, fontSize = 12.sp)
                                            }
                                            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                                Text("${digits(item.quantity.toLong())} قطعه", color = if (item.quantity == 0) colors.syncWarning else colors.goldPrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                                Text(item.location, color = colors.textMuted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.widthIn(max = 120.dp))
                                            }
                                        }
                                    }
                                }
                            }
                            VerticalScrollbar(rememberScrollbarAdapter(scroll), Modifier.align(Alignment.CenterEnd).fillMaxHeight())
                        }
                    }
                }
                if (wide) Box(Modifier.width(325.dp).fillMaxHeight()) {
                    if (state.selected != null) InventoryDetails(state, feature, Modifier.fillMaxSize()) else LuxuryCard(Modifier.fillMaxWidth()) {
                        Icon(Icons.Outlined.TouchApp, null, Modifier.size(32.dp), tint = colors.goldPrimary)
                        PageTitle("جزئیات و گردش کالا")
                        Text("کالا را انتخاب کنید تا اطلاعات کامل، برآورد قیمت و سوابق آن را ببینید.", color = colors.textMuted, fontSize = 13.sp)
                    }
                }
            }
            if (!wide && state.selected != null && !state.hasDialog) AlertDialog(onDismissRequest = { feature.select(null) }, modifier = Modifier.width(570.dp),
                title = { Text("جزئیات کالا") }, text = { InventoryDetails(state, feature, Modifier.heightIn(max = 490.dp)) },
                confirmButton = { GoldButton("بستن", { feature.select(null) }, isSecondary = true) })
        }
        SourceCaption(workspace)
    }
}

@Composable private fun rememberLazyListStateCompat() = androidx.compose.foundation.lazy.rememberLazyListState()
private fun digits(value: Long) = PersianNumberFormatter.toPersianDigits(value.toString())
private fun categoryShort(value: InventoryCategory) = when(value) {
    InventoryCategory.ALL -> "همه"; InventoryCategory.RINGS -> "انگشتر"; InventoryCategory.BANGLES -> "النگو"; InventoryCategory.NECKLACES -> "زنجیر"
    InventoryCategory.SETS -> "سرویس"; InventoryCategory.JEWELRY -> "جواهر"; InventoryCategory.COINS -> "سکه"; InventoryCategory.MISC -> "متفرقه"
}

@Composable private fun InventoryMetric(title: String, value: String, unit: String, modifier: Modifier) {
    val colors = LocalGoldExColors.current
    Column(modifier, verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(title, color = Color.White.copy(alpha = .75f), fontSize = 11.sp)
        val measurer = rememberTextMeasurer()
        val density = LocalDensity.current
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val width = with(density) { maxWidth.toPx() }
            val measured = measurer.measure(value, MaterialTheme.typography.bodyLarge.copy(fontSize = 19.sp), maxLines = 1).size.width.coerceAtLeast(1)
            val size = 19 * (width / measured).coerceAtMost(1f) * .96f
            AnimatedPriceTicker(value, color = colors.goldSecondary, fontSize = size.sp, fontWeight = FontWeight.SemiBold)
        }
        Text(unit, color = Color.White.copy(alpha = .75f), fontSize = 10.sp)
    }
}

@Composable private fun InventoryImage(value: String, modifier: Modifier) {
    val colors = LocalGoldExColors.current
    val bitmap = remember(value) { if (value.isBlank()) null else runCatching { org.jetbrains.skia.Image.makeFromEncoded(InventoryPhoto.bytes(value)).use { it.toComposeImageBitmap() } }.getOrNull() }
    Box(modifier.clip(RoundedCornerShape(12.dp)).background(colors.goldContainer), contentAlignment = Alignment.Center) {
        if (bitmap == null) Icon(Icons.Outlined.Diamond, "بدون تصویر کالا", Modifier.size(28.dp), tint = colors.goldPrimary)
        else Image(bitmap, "تصویر کالا", Modifier.fillMaxSize(), contentScale = androidx.compose.ui.layout.ContentScale.Crop)
    }
}

@Composable private fun InventoryDetails(state: DesktopInventoryState, feature: DesktopInventory, modifier: Modifier) {
    val item = state.selected ?: return
    val colors = LocalGoldExColors.current
    val scroll = rememberScrollState()
    Box(modifier) {
    LuxuryCard(Modifier.fillMaxSize().padding(end = 10.dp).verticalScroll(scroll).testTag("inventory-details"), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        InventoryImage(item.imageUrl, Modifier.fillMaxWidth().height(if (item.imageUrl.isBlank()) 64.dp else 145.dp))
        PageTitle(item.title)
        Text("${PersianNumberFormatter.toPersianDigits(item.code)} • ${item.category.titleFa}", color = colors.textMuted, fontSize = 12.sp)
        Text("برآورد فروش هر قطعه", color = colors.textMuted, fontSize = 11.sp)
        Amount(state.selectedPrice?.total?.let { PersianNumberFormatter.formatPrice(it) }, size = 23)
        Text(if (state.selectedPrice == null) "نرخ موجود نیست" else "تومان • بر پایهٔ نرخ انتخابی", color = colors.textMuted, fontSize = 10.sp)
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
        InventoryDialog(if (draft.id == null) "ثبت کالای جدید" else "ویرایش کالا", "inventory-dialog", state.saving, feature::dismiss, { feature.save() }, "ذخیرهٔ کالا", wide = true) {
            if (state.error != null) Text(state.error.orEmpty(), color = LocalGoldExColors.current.errorRed, fontSize = 12.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    PageTitle("شناسنامهٔ کالا")
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
                    DraftField(draft, "rfid", "شناسهٔ RFID (اختیاری)", draft.rfid, feature) { it.copy(rfid = this) }
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
                        DraftField(draft, "gross", "وزن ناخالص (گرم)", draft.gross, feature, Modifier.weight(1f), true) { it.copy(gross = this) }
                        DraftField(draft, "stone", "وزن نگین (گرم)", draft.stone, feature, Modifier.weight(1f), true) { it.copy(stone = this) }
                    }
                    LuxurySegmentedControl(Karat.values().toList(), draft.karat, feature::karat, label = { it.labelFa.substringBefore(" (") }, modifier = Modifier.fillMaxWidth(), height = 40.dp, fontSize = 12.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        DraftField(draft, "purity", "عیار دقیق (۱۰۰–۱۰۰۰)", draft.purity, feature, Modifier.weight(1f), true) { it.copy(purity = this) }
                        DesktopField(draft.quantity, { value -> feature.edit { it.copy(quantity = value) } }, "تعداد قطعات", Modifier.weight(1f).testTag("inventory-quantity"), numeric = true, error = draft.errors["quantity"], enabled = draft.id == null)
                    }
                    LuxurySegmentedControl(WageType.values().toList(), draft.wageType, { value -> feature.edit { it.copy(wageType = value, wage = "0") } },
                        label = { if (it == WageType.PERCENTAGE) "اجرت درصدی" else "اجرت هر گرم" }, modifier = Modifier.fillMaxWidth(), height = 40.dp, fontSize = 12.sp)
                    DraftField(draft, "wage", if (draft.wageType == WageType.PERCENTAGE) "اجرت (درصد)" else "اجرت هر گرم (تومان)", draft.wage, feature, numeric = true) { it.copy(wage = this) }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        DraftField(draft, "profit", "سود (درصد)", draft.profit, feature, Modifier.weight(1f), true) { it.copy(profit = this) }
                        DraftField(draft, "tax", "مالیات (درصد)", draft.tax, feature, Modifier.weight(1f), true) { it.copy(tax = this) }
                    }
                    PricePreview(state.preview)
                    Text("وزن و قیمت برای یک قطعه‌اند. در دستهٔ سکه سود صفر محاسبه می‌شود.", color = LocalGoldExColors.current.textMuted, fontSize = 11.sp)
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
            DesktopField(draft.count, { value -> feature.editMovement { it.copy(count = value) } }, "تعداد ورود / خروج", Modifier.testTag("movement-count"), numeric = true, error = draft.errors["count"])
            DesktopField(draft.weight, { value -> feature.editMovement { it.copy(weight = value) } }, "وزن ثبت در سند (گرم، اختیاری)", Modifier.testTag("movement-weight"), numeric = true, error = draft.errors["weight"])
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
    if (state.showHistory) AlertDialog(onDismissRequest = { feature.showHistory(false) }, modifier = Modifier.width(780.dp), title = { Text("تاریخچهٔ ورود و خروج انبار") },
        text = {
            if (state.history.isEmpty()) Text("هنوز گردش موجودی ثبت نشده است؛ از جزئیات کالا ورود یا خروج را انتخاب کنید.")
            else LazyColumn(Modifier.heightIn(max = 500.dp).testTag("inventory-history-list"), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(state.history.sortedByDescending { it.timestamp }, key = { it.id }) { MovementRow(it, full = true) }
            }
        }, confirmButton = { GoldButton("بستن", { feature.showHistory(false) }, isSecondary = true) })
    if (state.discard) AlertDialog(onDismissRequest = feature::cancelDiscard, title = { Text("بستن بدون ذخیره؟") }, text = { Text("تغییرات این فرم ذخیره نشده‌اند. برای ادامهٔ ویرایش، به فرم برگردید.") },
        confirmButton = { DialogActions(false, feature::cancelDiscard, feature::discard, "بستن بدون ذخیره", "inventory-discard", "ادامهٔ ویرایش") })
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
    DesktopField(value, { text -> feature.edit { text.change(it) } }, label, modifier.testTag("inventory-$key"), numeric, draft.errors[key])
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
