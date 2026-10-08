package com.goldex.companion.desktop.ui

import androidx.compose.foundation.*
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.invisibleToUser
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.*
import com.goldex.companion.data.*
import com.goldex.companion.desktop.DesktopCalculatorScreen
import com.goldex.companion.desktop.data.*
import com.goldex.companion.desktop.state.*
import com.goldex.companion.desktop.update.WindowsUpdater
import com.goldex.companion.model.*
import com.goldex.companion.ui.components.*
import com.goldex.companion.ui.theme.*
import com.goldex.companion.ui.util.ThousandsSeparatorVisualTransformation

@OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
@Composable
fun DesktopWorkspaceScreen(workspace: DesktopWorkspace, onBackup: () -> Unit, version: String, updater: WindowsUpdater? = null, onRestart: () -> Unit = {}) {
    val state by workspace.state.collectAsState()
    val inventoryState by workspace.inventory.state.collectAsState()
    val colors = LocalGoldExColors.current
    val pages = rememberSaveableStateHolder()
    val focus = LocalFocusManager.current
    LaunchedEffect(state.destination) { focus.clearFocus() }
    LaunchedEffect(updater) { updater?.start() }
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        BoxWithConstraints(Modifier.fillMaxSize().background(colors.background).testTag("workspace-root")) {
            val compact = maxWidth < 1080.dp
            Row(Modifier.fillMaxSize()) {
                Sidebar(state, workspace, compact, version, updater, state.draft == null && state.pendingDelete == null && !inventoryState.hasDialog)
                Column(Modifier.weight(1f).fillMaxHeight().padding(if (compact) 20.dp else 28.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                    WorkspaceHeader(state)
                    if (state.error != null || state.notice != null) {
                        Surface(color = if (state.error != null) colors.errorRed.copy(alpha = 0.08f) else colors.goldContainer, shape = RoundedCornerShape(12.dp)) {
                            Row(Modifier.fillMaxWidth().padding(start = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(state.error ?: state.notice.orEmpty(), Modifier.weight(1f).testTag("workspace-message"), color = if (state.error != null) colors.errorRed else colors.textMain, fontSize = 13.sp)
                                IconButton(onClick = workspace::clearMessage) { Icon(Icons.Outlined.Close, "بستن پیام", Modifier.size(18.dp), tint = colors.textMuted) }
                            }
                        }
                    }
                    AnimatedContent(targetState = state.destination, modifier = Modifier.weight(1f).fillMaxWidth().clipToBounds(),
                        transitionSpec = {
                            if (state.reduceMotion) fadeIn(tween(0)).togetherWith(fadeOut(tween(0)))
                            else (fadeIn(tween(240, delayMillis = 40)) + slideInVertically(tween(300, easing = FastOutSlowInEasing)) { if (targetState.navigationOrder > initialState.navigationOrder) it / 16 else -it / 16 })
                                .togetherWith(fadeOut(tween(140)) + slideOutVertically(tween(200, easing = FastOutSlowInEasing)) { if (targetState.navigationOrder > initialState.navigationOrder) -it / 24 else it / 24 })
                        }, label = "desktop-page-transition") { destination ->
                        val outgoing = destination != state.destination
                        Box(Modifier.fillMaxSize().testTag("page-${destination.name}").then(if (outgoing) Modifier.semantics { invisibleToUser() }.pointerInput(Unit) {
                            awaitPointerEventScope { while (true) awaitPointerEvent().changes.forEach { it.consume() } }
                        } else Modifier)) {
                            pages.SaveableStateProvider(destination.name) {
                                when (destination) {
                                    DesktopDestination.DASHBOARD -> DesktopDashboardPage(state, workspace.dashboard, workspace)
                                    DesktopDestination.CALCULATOR -> DesktopCalculatorScreen(
                                        calculator = workspace.calculator,
                                        dark = state.dark,
                                        showHeader = false,
                                        marketRates = workspace.calculatorRates,
                                        onNavigateToInvoice = { workspace.navigate(DesktopDestination.INVOICES) },
                                        onThemeChange = { workspace.toggleTheme() }
                                    )
                                    DesktopDestination.RATES -> RatesPage(state, workspace)
                                    DesktopDestination.PORTFOLIO -> PortfolioPage(state, workspace)
                                    DesktopDestination.SETTINGS -> SettingsPage(state, workspace, onBackup)
                                    DesktopDestination.INVENTORY -> DesktopInventoryPage(state, workspace.inventory)
                                    DesktopDestination.INVOICES -> DesktopInvoicesPage(state, workspace)
                                }
                            }
                        }
                    }
                }
            }
        }
        state.draft?.let { AssetDialog(it, state.saving, workspace) }
        InventoryDialogs(workspace.inventory)
        if (updater != null && state.draft == null && state.pendingDelete == null && !inventoryState.hasDialog)
            WindowsUpdatePrompt(updater, canRestart = !state.saving && !inventoryState.saving && state.settingsDraft == null, onRestart = onRestart)
        state.pendingDelete?.let { item ->
            AlertDialog(onDismissRequest = { if (!state.saving) workspace.requestDelete(null) },
                title = { Text("حذف دارایی") }, text = { Text("«${item.title}» از فهرست دارایی‌ها حذف شود؟ نسخه قبلی اطلاعات در پشتیبان محلی حفظ می‌شود.") },
                confirmButton = {
                    Row(Modifier.width(330.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        GoldButton("انصراف", { workspace.requestDelete(null) }, Modifier.weight(1f), isSecondary = true, enabled = !state.saving)
                        GoldButton("حذف دارایی", { workspace.confirmDelete() }, enabled = !state.saving, modifier = Modifier.weight(1.2f).testTag("confirm-delete"))
                    }
                })
        }
    }
}

private fun icon(destination: DesktopDestination): ImageVector = when (destination) {
    DesktopDestination.DASHBOARD -> Icons.Outlined.Dashboard
    DesktopDestination.CALCULATOR -> Icons.Outlined.Calculate
    DesktopDestination.RATES -> Icons.Outlined.ShowChart
    DesktopDestination.PORTFOLIO -> Icons.Outlined.AccountBalanceWallet
    DesktopDestination.SETTINGS -> Icons.Outlined.Settings
    DesktopDestination.INVENTORY -> Icons.Outlined.Inventory2
    DesktopDestination.INVOICES -> Icons.Outlined.ReceiptLong
}

@Composable private fun Sidebar(state: WorkspaceState, workspace: DesktopWorkspace, compact: Boolean, version: String, updater: WindowsUpdater?, canOpenPrompt: Boolean) {
    val colors = LocalGoldExColors.current
    Surface(color = colors.surface, modifier = Modifier.width(if (compact) 108.dp else 218.dp).fillMaxHeight()) {
        Column(Modifier.padding(horizontal = if (compact) 10.dp else 18.dp, vertical = if (compact) 18.dp else 26.dp), verticalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 10.dp)) {
            Row(Modifier.fillMaxWidth().padding(bottom = if (compact) 18.dp else 28.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Image(painterResource("mipmap-xxxhdpi/ic_launcher.png"), "نشان قیراط", Modifier.size(44.dp))
                if (!compact) Column {
                    Text("قیراط", color = colors.goldPrimary, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                    Text("همراه زرگر", color = colors.textMuted, fontSize = 11.sp)
                }
            }
            DesktopDestination.mainDestinations.forEach { destination ->
                val selected = state.destination == destination || (destination == DesktopDestination.SETTINGS && state.destination == DesktopDestination.PORTFOLIO)
                val selectionColor by animateColorAsState(if (selected) colors.goldContainer else Color.Transparent,
                    tween(if (state.reduceMotion) 0 else 180), label = "sidebar-selection")
                var focused by remember(destination) { mutableStateOf(false) }
                Surface(color = selectionColor, shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.border(if (focused) 1.dp else 0.dp, if (focused) colors.goldPrimary else Color.Transparent, RoundedCornerShape(14.dp))) {
                    if (compact) Column(Modifier.fillMaxWidth().onFocusChanged { focused = it.isFocused }.selectable(selected, onClick = { workspace.navigate(destination) }).testTag("nav-${destination.name}").padding(vertical = 6.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Icon(icon(destination), null, Modifier.size(23.dp), tint = if (selected) colors.goldPrimary else colors.textMuted)
                        Text(if (destination == DesktopDestination.INVENTORY) "انبار و ویترین" else if (destination == DesktopDestination.INVOICES) "فاکتورها" else destination.title, color = if (selected) colors.goldPrimary else colors.textSecondary, fontSize = 11.sp, maxLines = 1)
                    } else Row(Modifier.fillMaxWidth().onFocusChanged { focused = it.isFocused }.selectable(selected, onClick = { workspace.navigate(destination) }).testTag("nav-${destination.name}").padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(icon(destination), null, Modifier.size(22.dp), tint = if (selected) colors.goldPrimary else colors.textMuted)
                        Text(destination.title, color = if (selected) colors.goldPrimary else colors.textSecondary, fontSize = 14.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium, maxLines = 1)
                    }
                }
            }
            Spacer(Modifier.weight(1f))
            HorizontalDivider(color = colors.border)
            if (updater != null) WindowsUpdateControl(updater, canOpenPrompt, Modifier.fillMaxWidth(), compact)
            WorkspaceControl(if (state.dark) "حالت روز" else "حالت شب", { workspace.toggleTheme() },
                Modifier.fillMaxWidth().testTag("workspace-theme"), icon = if (state.dark) Icons.Outlined.LightMode else Icons.Outlined.DarkMode,
                secondary = true, enabled = !state.saving, compact = compact)
            Text(if (compact) "این رایانه" else "اطلاعات روی این رایانه ذخیره می‌شود", color = colors.textMuted, fontSize = 10.sp, maxLines = 2)
            Text("نسخه ${PersianNumberFormatter.toPersianDigits(version)}", color = colors.textMuted, fontSize = 11.sp)
        }
    }
}

@Composable internal fun PageScroll(content: @Composable ColumnScope.() -> Unit) {
    val scroll = rememberScrollState()
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().padding(end = 12.dp).verticalScroll(scroll), verticalArrangement = Arrangement.spacedBy(20.dp), content = content)
        VerticalScrollbar(rememberScrollbarAdapter(scroll), Modifier.align(Alignment.CenterEnd).fillMaxHeight())
    }
}

@Composable internal fun PageTitle(text: String, modifier: Modifier = Modifier) {
    Text(text, modifier, color = LocalGoldExColors.current.textMain, fontWeight = FontWeight.Bold, fontSize = 17.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
}

@Composable internal fun Amount(text: String?, modifier: Modifier = Modifier, color: Color = LocalGoldExColors.current.textMain, size: Int = 19) {
    AnimatedPriceTicker(text = text ?: "—", modifier = modifier, color = color, fontSize = size.sp, fontWeight = FontWeight.SemiBold)
}
internal fun price(value: Long?): String? = value?.let { PersianNumberFormatter.formatPrice(it) }

@Composable internal fun QuoteRow(label: String, value: Long?, unit: String = "تومان") {
    val colors = LocalGoldExColors.current
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(label, Modifier.weight(1f), color = colors.textSecondary, fontSize = 14.sp, maxLines = 1)
        Amount(price(value?.takeIf { it > 0 }), size = 20)
        Text(unit, color = colors.textMuted, fontSize = 11.sp)
    }
}

@Composable internal fun SourceCaption(state: WorkspaceState) {
    val snapshot = state.snapshot
    Text(if (snapshot == null) "نرخی دریافت نشده؛ دریافت آنلاین یا ثبت نرخ دستی را انتخاب کنید." else
        "${if (snapshot.kind == QuoteKind.MANUAL) "ثبت‌شده توسط شما" else snapshot.rates.source.labelFa} • ${DesktopPortfolioPolicy.observedTime(snapshot.observedAt)}",
        color = LocalGoldExColors.current.textMuted, fontSize = 11.sp, maxLines = 2)
}

internal fun assetDescription(item: PortfolioItem) = if (item.category == PortfolioCategory.GOLD)
    "${PersianNumberFormatter.formatWeight(item.weightGrams)} گرم • ${item.karat.labelFa}" else
    "${PersianNumberFormatter.toPersianDigits(item.quantity.toString())} عدد • ${coinLabel(item.coinType ?: CoinType.EMAMI)}"
internal fun coinLabel(coin: CoinType) = when (coin) { CoinType.EMAMI -> "امامی"; CoinType.BAHAR -> "بهار آزادی"; CoinType.HALF -> "نیم سکه"; CoinType.QUARTER -> "ربع سکه"; CoinType.GERAMI -> "گرمی" }

@Composable private fun PortfolioPage(state: WorkspaceState, workspace: DesktopWorkspace) {
    val colors = LocalGoldExColors.current
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        TextButton({ workspace.navigate(DesktopDestination.SETTINGS) }, Modifier.testTag("previous-portfolio-back")) { Text("بازگشت به تنظیمات") }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            DesktopField(state.query, workspace::search, "جست‌وجوی دارایی", Modifier.weight(1f).testTag("asset-search"))
            GoldButton("ثبت محصول در انبار", workspace::openInventoryItem, icon = Icons.Outlined.Add, modifier = Modifier.width(172.dp).testTag("previous-portfolio-to-inventory"))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            listOf(null to "همه", PortfolioCategory.GOLD to "طلا", PortfolioCategory.COIN to "سکه").forEach { (category, label) ->
                FilterChip(state.categoryFilter == category, { workspace.filter(category) }, label = { Text(label, maxLines = 1) })
            }
            Spacer(Modifier.weight(1f))
            Text("${PersianNumberFormatter.toPersianDigits(state.assets.rows.size.toString())} دارایی", color = colors.textMuted, fontSize = 12.sp)
        }
        if (state.visibleAssets.isEmpty()) {
            LuxuryCard(modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Outlined.AccountBalanceWallet, null, Modifier.size(36.dp), tint = colors.goldPrimary)
                PageTitle(if (state.assets.rows.isEmpty()) "دارایی‌های شما از اینجا شروع می‌شود" else "دارایی مطابق جست‌وجو پیدا نشد")
                Text(if (state.assets.rows.isEmpty()) "قطعه طلا، شمش یا سکه را همراه با مبلغ خرید ثبت کنید؛ ارزش روز با نرخ‌های موجود محاسبه می‌شود." else "نام کوتاه‌تر را جست‌وجو یا فیلتر را تغییر دهید.", color = colors.textMuted, fontSize = 13.sp)
            }
        } else {
            Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("دارایی / مقدار", Modifier.weight(1.5f), color = colors.textMuted, fontSize = 12.sp)
                Text("خرید / ارزش برآوردی", Modifier.weight(1f), color = colors.textMuted, fontSize = 12.sp)
                Text("سود / زیان", Modifier.weight(0.9f), color = colors.textMuted, fontSize = 12.sp)
                Spacer(Modifier.width(80.dp))
            }
            LazyColumn(Modifier.weight(1f).fillMaxWidth().testTag("asset-list"), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(state.visibleAssets, key = { it.item.id }) { row ->
                    LuxuryCard(modifier = Modifier.fillMaxWidth()) {
                        Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1.5f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(row.item.title, color = colors.textMain, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(assetDescription(row.item), color = colors.textMuted, fontSize = 11.sp, maxLines = 2)
                                if (row.item.purchaseDate.isNotBlank()) Text(PersianNumberFormatter.toPersianDigits(row.item.purchaseDate), color = colors.textMuted, fontSize = 10.sp, maxLines = 1)
                            }
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(if (row.item.purchasePriceTotal > 0) price(row.item.purchasePriceTotal).orEmpty() else "خرید نامشخص", color = colors.textMuted, fontSize = 12.sp)
                                Amount(price(row.value), size = 17)
                                if (row.value == null) Text("نرخ موجود نیست", color = colors.syncWarning, fontSize = 10.sp)
                            }
                            Column(Modifier.weight(0.9f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Amount(price(row.profit), color = if (row.profit == null) colors.textMuted else if (row.profit >= 0) colors.profitGreen else colors.errorRed, size = 16)
                                Text("تومان", color = colors.textMuted, fontSize = 10.sp)
                            }
                            Row(Modifier.width(80.dp)) {
                                IconButton(onClick = { workspace.openAsset(row.item) }, modifier = Modifier.testTag("edit-${row.item.id}")) { Icon(Icons.Outlined.Edit, "ویرایش ${row.item.title}", Modifier.size(19.dp), tint = colors.goldPrimary) }
                                IconButton(onClick = { workspace.requestDelete(row.item) }, modifier = Modifier.testTag("delete-${row.item.id}")) { Icon(Icons.Outlined.DeleteOutline, "حذف ${row.item.title}", Modifier.size(19.dp), tint = colors.textMuted) }
                            }
                        }
                    }
                }
            }
        }
        SourceCaption(state)
    }
}

@Composable internal fun DesktopField(value: String, onChange: (String) -> Unit, label: String, modifier: Modifier = Modifier, numeric: Boolean = false, error: String? = null, enabled: Boolean = true, singleLine: Boolean = true, monetary: Boolean = false, keyboardType: KeyboardType = if (numeric) KeyboardType.Decimal else KeyboardType.Text, unit: String? = null, adornment: ImageVector? = null) {
    val colors = LocalGoldExColors.current
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        GoldOutlinedTextField(value, onChange, Modifier.fillMaxWidth(), label = { Text(label, fontSize = 13.sp) }, enabled = enabled, singleLine = singleLine, maxLines = if (singleLine) 1 else 3, isError = error != null,
            textStyle = TextStyle(fontFamily = VazirmatnFamily, fontFeatureSettings = VazirmatnFeatureSettings, fontSize = 15.sp, color = colors.textMain, textDirection = if (numeric) TextDirection.Ltr else TextDirection.ContentOrRtl),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            trailingIcon = when {
                unit != null -> { { Text(unit, Modifier.padding(horizontal = 10.dp).testTag("field-unit-$label"), color = colors.textMuted, fontSize = 12.sp, maxLines = 1) } }
                adornment != null -> { { Icon(adornment, null, Modifier.size(19.dp), tint = colors.textMuted) } }
                else -> null
            },
            visualTransformation = if (monetary) ThousandsSeparatorVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = colors.goldPrimary, focusedLabelColor = colors.goldPrimary, unfocusedBorderColor = colors.border,
                unfocusedLabelColor = colors.textMuted, focusedContainerColor = colors.surface, unfocusedContainerColor = colors.surface, cursorColor = colors.goldPrimary))
        error?.let { Text(programErrorText(it), color = colors.errorRed, fontSize = 11.sp) }
    }
}

@Composable private fun AssetDialog(draft: PortfolioDraft, saving: Boolean, workspace: DesktopWorkspace) {
    val focus = remember { FocusRequester() }
    AlertDialog(onDismissRequest = workspace::dismissAsset,
        modifier = Modifier.width(620.dp).testTag("asset-dialog"), title = { Text(if (draft.id == null) "دارایی جدید" else "ویرایش دارایی") },
        text = {
            Column(Modifier.heightIn(max = 510.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                DesktopField(draft.title, { value -> workspace.editDraft { it.copy(title = value) } }, "نام دارایی", Modifier.focusRequester(focus).testTag("asset-title"), error = draft.errors["title"])
                LaunchedEffect(draft.id) { focus.requestFocus() }
                LuxurySegmentedControl(PortfolioCategory.values().toList(), draft.category, { category -> workspace.editDraft { it.copy(category = category) } },
                    label = { if (it == PortfolioCategory.GOLD) "طلا / شمش" else "سکه" }, modifier = Modifier.fillMaxWidth(), height = 42.dp, fontSize = 13.sp)
                if (draft.category == PortfolioCategory.GOLD) {
                    DesktopField(draft.weight, { value -> workspace.editDraft { it.copy(weight = value) } }, "وزن (گرم)", Modifier.testTag("asset-weight"), numeric = true, error = draft.errors["weight"])
                    LuxurySegmentedControl(Karat.values().toList(), draft.karat, { karat -> workspace.editDraft { it.copy(karat = karat) } }, label = { "${PersianNumberFormatter.toPersianDigits(it.karatNumber.toString())} عیار" }, modifier = Modifier.fillMaxWidth(), height = 42.dp, fontSize = 13.sp)
                } else {
                    ChoiceField("نوع سکه", draft.coin, CoinType.values().toList(), ::coinLabel) { coin -> workspace.editDraft { it.copy(coin = coin) } }
                    DesktopField(draft.quantity, { value -> workspace.editDraft { it.copy(quantity = value) } }, "تعداد", Modifier.testTag("asset-quantity"), numeric = true, error = draft.errors["quantity"])
                }
                DesktopField(draft.cost, { value -> workspace.editDraft { it.copy(cost = value) } }, "مبلغ کل خرید (تومان)", Modifier.testTag("asset-cost"), numeric = true, monetary = true, error = draft.errors["cost"])
                DesktopField(draft.date, { value -> workspace.editDraft { it.copy(date = value) } }, "تاریخ خرید (اختیاری)", error = draft.errors["date"])
                Text("دارایی‌ها روی همین رایانه ذخیره می‌شوند. ارزش روز به موجود بودن نرخ هر دارایی وابسته است.", color = LocalGoldExColors.current.textMuted, fontSize = 11.sp)
            }
        }, confirmButton = {
            Row(Modifier.width(350.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                GoldButton("انصراف", workspace::dismissAsset, Modifier.weight(1f), isSecondary = true, enabled = !saving)
                GoldButton(if (saving) "در حال ذخیره" else "ذخیره دارایی", { workspace.saveAsset() }, enabled = !saving, modifier = Modifier.weight(1.3f).testTag("save-asset"))
            }
        })
}

@Composable internal fun <T> ChoiceField(label: String, selected: T, choices: List<T>, text: (T) -> String, onSelect: (T) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val colors = LocalGoldExColors.current
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, color = colors.textMuted, fontSize = 12.sp)
        Box {
            OutlinedButton({ expanded = true }, Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), border = BorderStroke(0.6.dp, colors.border)) {
                Text(text(selected), Modifier.weight(1f), color = colors.textMain, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Icon(Icons.Outlined.ExpandMore, null, tint = colors.goldPrimary)
            }
            DropdownMenu(expanded, { expanded = false }) { choices.forEach { choice -> DropdownMenuItem(text = { Text(text(choice)) }, onClick = { expanded = false; onSelect(choice) }) } }
        }
    }
}
