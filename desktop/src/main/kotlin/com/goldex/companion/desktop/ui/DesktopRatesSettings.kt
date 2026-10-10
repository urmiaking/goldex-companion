package com.goldex.companion.desktop.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.goldex.companion.data.*
import com.goldex.companion.desktop.data.*
import com.goldex.companion.desktop.state.*
import com.goldex.companion.desktop.update.WindowsUpdater
import com.goldex.companion.model.PersianNumberFormatter
import com.goldex.companion.model.WageType
import com.goldex.companion.ui.components.*
import com.goldex.companion.ui.theme.*
import kotlinx.coroutines.launch
import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

private val rateLabels = listOf("طلای ۱۸ عیار", "طلای ۲۴ عیار", "مظنه آبشده", "سکه امامی", "سکه بهار آزادی", "نیم سکه", "ربع سکه", "سکه گرمی", "دلار آزاد")
private fun values(rates: MarketRates) = listOf(rates.gold18, rates.gold24, rates.goldMelt, rates.coinEmami, rates.coinBahar, rates.coinHalf, rates.coinQuarter, rates.coinGerami, rates.usd)

@Composable internal fun ManualRatesDialog(state: WorkspaceState, workspace: DesktopWorkspace, onDismiss: () -> Unit) {
    val scope = rememberCoroutineScope()
    var inputs by remember { mutableStateOf((state.snapshot?.rates?.let(::values) ?: List(9) { 0L }).map { if (it > 0) it.toString() else "" }) }
    var errors by remember { mutableStateOf(emptyMap<Int, String>()) }
    AlertDialog(onDismissRequest = { if (!state.saving) onDismiss() }, modifier = Modifier.width(620.dp), title = { Text("ثبت نرخ دستی") },
        text = {
            Column(Modifier.heightIn(max = 450.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("نرخ‌ها را به تومان وارد کنید. فقط طلای ۱۸ الزامی است؛ سایر خانه‌های خالی، ناموجود ثبت می‌شوند.", color = LocalGoldExColors.current.textMuted, fontSize = 12.sp)
                inputs.forEachIndexed { i, value -> DesktopField(value, { new -> inputs = inputs.toMutableList().also { it[i] = new }; errors = errors - i }, rateLabels[i], Modifier.testTag("manual-rate-$i"), numeric = true, monetary = true, error = errors[i]) }
            }
        }, confirmButton = {
            Row(Modifier.width(350.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                GoldButton("انصراف", onDismiss, Modifier.weight(1f), isSecondary = true, enabled = !state.saving)
                GoldButton("ثبت نرخ‌ها", {
                    val parsed = inputs.map { input -> DesktopPortfolioPolicy.normalized(input).let { text -> if (text.isBlank()) 0L else text.takeIf { it.matches(Regex("[0-9]+")) }?.toLongOrNull() } }
                    errors = parsed.mapIndexedNotNull { i, number -> if (number == null || number !in 0..9_000_000_000_000L || (i == 0 && number == 0L)) i to "نرخ معتبر به تومان وارد کنید" else null }.toMap()
                    if (errors.isEmpty()) {
                        val p = parsed.map { it!! }
                        val rates = DesktopMarketRepository.emptyRates(state.settings.priceSource).copy(gold18 = p[0], gold24 = p[1], goldMelt = p[2], coinEmami = p[3], coinBahar = p[4], coinHalf = p[5], coinQuarter = p[6], coinGerami = p[7], usd = p[8])
                        workspace.saveManualRates(rates)?.let { job -> scope.launch { job.join(); if (workspace.state.value.error == null) onDismiss() } }
                    }
                }, enabled = !state.saving, modifier = Modifier.weight(1.3f).testTag("save-manual-rates"))
            }
        })
}

@Composable internal fun SettingsPage(state: WorkspaceState, workspace: DesktopWorkspace, onBackup: () -> Unit) {
    val draft = state.settingsDraft ?: state.settings
    val colors = LocalGoldExColors.current
    val dirty = draft != state.settings
    var showSignaturePad by remember { mutableStateOf(false) }
    PageScroll {
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val profile: @Composable () -> Unit = {
                LuxuryCard {
                    PageTitle("مشخصات گالری")
                    DesktopField(draft.galleryName, { value -> workspace.editSettings { it.copy(galleryName = value.take(120)) } }, "نام گالری", Modifier.testTag("settings-gallery"), adornment = Icons.Outlined.Storefront)
                    DesktopField(draft.managerName, { value -> workspace.editSettings { it.copy(managerName = value.take(80)) } }, "نام زرگر", adornment = Icons.Outlined.Person)
                    DesktopField(draft.galleryPhone, { value -> workspace.editSettings { it.copy(galleryPhone = value.take(24)) } }, "شماره تماس", Modifier.testTag("settings-phone"), numeric = true, keyboardType = androidx.compose.ui.text.input.KeyboardType.Phone, adornment = Icons.Outlined.Phone)
                    DesktopField(draft.unionCode, { value -> workspace.editSettings { it.copy(unionCode = value.take(40)) } }, "کد اتحادیه", Modifier.testTag("settings-union"), numeric = true, keyboardType = androidx.compose.ui.text.input.KeyboardType.Ascii, adornment = Icons.Outlined.Badge)
                    DesktopField(draft.galleryLicense, { value -> workspace.editSettings { it.copy(galleryLicense = value.take(40)) } }, "پروانه کسب", Modifier.testTag("settings-license"), numeric = true, keyboardType = androidx.compose.ui.text.input.KeyboardType.Ascii, adornment = Icons.Outlined.Verified)
                    DesktopField(draft.galleryAddress, { value -> workspace.editSettings { it.copy(galleryAddress = value.take(300)) } }, "نشانی گالری", Modifier.testTag("settings-address"), singleLine = false, adornment = Icons.Outlined.LocationOn)

                    // Logo & Signature section (matching Android functionality for invoices)
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (colors.isDark) colors.surfaceElevated else androidx.compose.ui.graphics.Color(0xFFFCFAF5),
                        border = BorderStroke(1.dp, colors.goldBorder.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth().testTag("settings-brand-assets")
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Outlined.CameraAlt, contentDescription = null, tint = colors.goldPrimary, modifier = Modifier.size(18.dp))
                                Text("لوگو و امضای دیجیتال (برای فاکتورها)", fontWeight = FontWeight.Bold, fontSize = 12.5.sp, color = colors.textMain)
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                DesktopBrandAssetTile(
                                    title = "لوگوی واحد صنفی",
                                    actionLabel = if (draft.invoiceLogoUri.isNotBlank()) "تغییر لوگو" else "انتخاب لوگو",
                                    bitmap = rememberBrandAssetBitmap(draft.invoiceLogoUri),
                                    fallback = draft.galleryName.trim().take(2).ifBlank { "لوگو" },
                                    onPick = {
                                        val picked = selectBrandLogoFile()
                                        if (picked != null) {
                                            val brandDir = workspace.dataDirectory.resolve("brand_assets")
                                            Files.createDirectories(brandDir)
                                            val dest = brandDir.resolve("hub_logo.png")
                                            Files.copy(picked.toPath(), dest, StandardCopyOption.REPLACE_EXISTING)
                                            workspace.editSettings { it.copy(invoiceLogoUri = dest.toFile().absolutePath) }
                                        }
                                    },
                                    onClear = if (draft.invoiceLogoUri.isNotBlank()) ({ workspace.editSettings { it.copy(invoiceLogoUri = "") } }) else null,
                                    modifier = Modifier.weight(1f).testTag("settings-logo-tile")
                                )
                                DesktopBrandAssetTile(
                                    title = "امضای دیجیتال زرگر",
                                    actionLabel = if (draft.invoiceSignatureUri.isNotBlank()) "تغییر امضا" else "ثبت امضا",
                                    bitmap = rememberBrandAssetBitmap(draft.invoiceSignatureUri),
                                    fallback = "امضا",
                                    onPick = { showSignaturePad = true },
                                    onClear = if (draft.invoiceSignatureUri.isNotBlank()) ({ workspace.editSettings { it.copy(invoiceSignatureUri = "") } }) else null,
                                    modifier = Modifier.weight(1f).testTag("settings-signature-tile")
                                )
                            }
                        }
                    }
                }
            }
            val preferences: @Composable () -> Unit = {
                Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    LuxuryCard {
                        PageTitle("پیش‌فرض‌های محاسبه")
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            DesktopField(draft.defaultProfitPercent, { value -> workspace.editSettings { it.copy(defaultProfitPercent = value) } }, "سود", Modifier.weight(1f).testTag("settings-profit"), numeric = true, unit = "٪")
                            DesktopField(draft.defaultTaxPercent, { value -> workspace.editSettings { it.copy(defaultTaxPercent = value) } }, "مالیات", Modifier.weight(1f).testTag("settings-tax"), numeric = true, unit = "٪")
                        }
                        ChoiceField("نوع اجرت", draft.defaultWageType, WageType.values().toList(), { if (it == WageType.PERCENTAGE) "درصدی" else "تومان در هر گرم" }) { value -> workspace.editSettings { it.copy(defaultWageType = value) } }
                        Text("محاسبه در حال انجام تغییر نمی‌کند. با پاک‌کردن فرم، پیش‌فرض‌های ذخیره‌شده اعمال می‌شوند.", color = colors.textMuted, fontSize = 11.sp)
                    }
                    LuxuryCard(Modifier.fillMaxWidth()) {
                        PageTitle("دریافت نرخ‌ها")
                        Text("پایگاه قیمت‌گذاری فعال بازار را انتخاب کنید:", fontSize = 12.sp, color = colors.textSecondary)
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            PriceSource.values().forEach { src ->
                                DesktopPriceSourceOption(
                                    source = src,
                                    isSelected = draft.priceSource == src,
                                    onSelect = { workspace.editSettings { it.copy(priceSource = src) } }
                                )
                            }
                        }
                        Text("نرخ‌ها هنگام شروع برنامه و به‌صورت خودکار در پس‌زمینه دریافت می‌شوند. در حالت آفلاین، آخرین نرخ ذخیره‌شده با زمان اصلی آن در دسترس است.", color = colors.textMuted, fontSize = 11.5.sp)
                    }
                }
            }
            if (maxWidth >= 850.dp) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                Box(Modifier.weight(1f)) { profile() }
                Box(Modifier.weight(1f)) { preferences() }
            } else Column(verticalArrangement = Arrangement.spacedBy(20.dp)) { profile(); preferences() }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            GoldButton("برگرداندن تغییرات", workspace::revertSettings, Modifier.weight(1f), isSecondary = true, enabled = dirty && !state.saving)
            GoldButton(if (state.saving) "در حال ذخیره" else "ذخیره تنظیمات", { workspace.saveSettings(draft) }, Modifier.weight(1f).testTag("save-settings"), enabled = dirty && !state.saving, icon = Icons.Outlined.Save)
        }
        if (state.assets.rows.isNotEmpty()) LuxuryCard {
            PageTitle("سبد قبلی")
            Text("دارایی‌های سبد قبلی شما حفظ شده‌اند. موجودی پیشخوان از کالاهای ثبت‌شده در انبار و ویترین محاسبه می‌شود.", color = colors.textMuted, fontSize = 12.sp)
            GoldButton("مشاهده سبد قبلی", { workspace.navigate(DesktopDestination.PORTFOLIO) }, Modifier.testTag("open-previous-portfolio"), isSecondary = true)
        }
        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min).testTag("settings-tools"), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            LuxuryCard(Modifier.weight(1f).fillMaxHeight().testTag("settings-backup")) {
                PageTitle("پشتیبان اطلاعات")
                Text("کالاها، گردش موجودی، تنظیمات و آخرین نرخ‌ها روی این رایانه ذخیره می‌شوند. نسخه پشتیبان را در جای مطمئن نگه دارید.", color = colors.textSecondary, fontSize = 12.sp)
                Text("اطلاعات گوشی و حساب ابری خودکار منتقل نمی‌شود.", color = colors.textMuted, fontSize = 11.sp)
                Spacer(Modifier.weight(1f))
                GoldButton("ذخیره فایل پشتیبان", onBackup, Modifier.fillMaxWidth().testTag("save-backup"), isSecondary = true, enabled = !state.saving, icon = Icons.Outlined.FileDownload)
            }
            LuxuryCard(Modifier.weight(1f).fillMaxHeight().testTag("settings-motion")) {
                PageTitle("حرکت رابط")
                Text("اگر حرکت کمتر را ترجیح می‌دهید، انیمیشن صفحات و نمودارها را کاهش دهید.", color = colors.textSecondary, fontSize = 12.sp)
                Spacer(Modifier.weight(1f))
                Row(Modifier.fillMaxWidth().height(48.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("کاهش حرکت", Modifier.weight(1f), color = colors.textSecondary, fontSize = 13.sp)
                    Switch(state.reduceMotion, { workspace.setReduceMotion(it) }, enabled = !state.saving,
                        modifier = Modifier.testTag("reduce-motion"), colors = SwitchDefaults.colors(checkedTrackColor = colors.goldPrimary))
                }
            }
        }
    }

    if (showSignaturePad) {
        DesktopSignaturePadDialog(
            onDismiss = { showSignaturePad = false },
            onSaveSignature = { strokes, w, h ->
                val brandDir = workspace.dataDirectory.resolve("brand_assets")
                Files.createDirectories(brandDir)
                val dest = brandDir.resolve("hub_signature.png").toFile()
                saveSignatureStrokesToPng(strokes, w, h, dest)
                workspace.editSettings { it.copy(invoiceSignatureUri = dest.absolutePath) }
                showSignaturePad = false
            }
        )
    }
}

@Composable
internal fun DesktopPriceSourceOption(
    source: PriceSource,
    isSelected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalGoldExColors.current
    val (badgeTitle, badgeColor) = when (source) {
        PriceSource.TGJU -> "مرجع رسمی بازار" to colors.goldPrimary
        PriceSource.TALA_IR -> "شبکه پایدار" to colors.profitGreen
        PriceSource.ISIGNAL -> "سیگنال سریع" to androidx.compose.ui.graphics.Color(0xFF38BDF8)
    }

    val desc = when (source) {
        PriceSource.TGJU -> "تابلوی اتحادیه طلا و جواهر تهران • مظنه آبشده و مسکوکات"
        PriceSource.TALA_IR -> "شبکه اطلاع‌رسانی طلا و ارز • پوشش لحظه‌ای بازار"
        PriceSource.ISIGNAL -> "شبکه هوشمند تحلیلی • پوشش انس جهانی و ارز آزاد"
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) colors.goldContainer.copy(alpha = 0.4f) else colors.surfaceElevated,
        border = BorderStroke(
            width = if (isSelected) 1.2.dp else 0.6.dp,
            color = if (isSelected) colors.goldPrimary else colors.border
        ),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onSelect)
            .testTag("price-source-${source.name}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Radio Circle Dot
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .clip(CircleShape)
                    .border(
                        width = if (isSelected) 2.dp else 1.2.dp,
                        color = if (isSelected) colors.goldPrimary else colors.textMuted,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .size(9.dp)
                            .clip(CircleShape)
                            .background(colors.goldPrimary)
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = source.labelFa,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 12.5.sp,
                        color = if (isSelected) colors.goldPrimary else colors.textMain
                    )
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = badgeColor.copy(alpha = 0.15f),
                        border = BorderStroke(0.5.dp, badgeColor.copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = badgeTitle,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = badgeColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Text(
                    text = desc,
                    fontSize = 11.sp,
                    color = colors.textMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
internal fun DesktopBrandAssetTile(
    title: String,
    actionLabel: String,
    bitmap: androidx.compose.ui.graphics.ImageBitmap?,
    fallback: String,
    onPick: () -> Unit,
    onClear: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    val colors = LocalGoldExColors.current
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = colors.surface,
        border = BorderStroke(0.8.dp, colors.border)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(colors.goldContainer.copy(alpha = 0.35f))
                    .border(1.dp, colors.goldBorder.copy(alpha = 0.7f), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap,
                        contentDescription = title,
                        modifier = Modifier.fillMaxSize().padding(4.dp),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    Text(
                        text = fallback.ifBlank { "نشان" },
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = colors.goldPrimary
                    )
                }
            }
            Text(title, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = colors.textMain)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(
                    onClick = onPick,
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text(actionLabel, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = colors.goldPrimary)
                }
                if (onClear != null) {
                    IconButton(onClick = onClear, modifier = Modifier.size(30.dp)) {
                        Icon(
                            Icons.Outlined.Delete,
                            contentDescription = "حذف $title",
                            tint = colors.errorRed,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun DesktopSignaturePadDialog(
    onDismiss: () -> Unit,
    onSaveSignature: (strokes: List<List<Offset>>, width: Int, height: Int) -> Unit
) {
    val colors = LocalGoldExColors.current
    val strokes = remember { mutableStateListOf<List<Offset>>() }
    var currentStroke by remember { mutableStateOf<List<Offset>>(emptyList()) }
    var canvasSize by remember { mutableStateOf(androidx.compose.ui.unit.IntSize.Zero) }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.width(540.dp).testTag("signature-pad-dialog"),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Outlined.Edit, contentDescription = null, tint = colors.goldPrimary, modifier = Modifier.size(20.dp))
                Column {
                    Text("ثبت امضای دیجیتال زرگر", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = colors.textMain)
                    Text("جهت درج مستقیم در فاکتورهای رسمی و اسناد فروش", fontSize = 11.sp, color = colors.textMuted)
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (colors.isDark) colors.surfaceElevated else androidx.compose.ui.graphics.Color(0xFFFCFAF5),
                    border = BorderStroke(1.dp, colors.goldBorder.copy(alpha = 0.6f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                ) {
                    androidx.compose.foundation.Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .onSizeChanged { canvasSize = it }
                            .pointerInput(Unit) {
                                detectDragGestures(
                                    onDragStart = { offset ->
                                        currentStroke = listOf(offset)
                                    },
                                    onDrag = { change, _ ->
                                        change.consume()
                                        currentStroke = currentStroke + change.position
                                    },
                                    onDragEnd = {
                                        if (currentStroke.isNotEmpty()) {
                                            strokes.add(currentStroke)
                                            currentStroke = emptyList()
                                        }
                                    },
                                    onDragCancel = {
                                        currentStroke = emptyList()
                                    }
                                )
                            }
                            .testTag("signature-canvas")
                    ) {
                        for (stroke in strokes) {
                            if (stroke.size >= 2) {
                                val path = androidx.compose.ui.graphics.Path().apply {
                                    moveTo(stroke[0].x, stroke[0].y)
                                    for (i in 1 until stroke.size) {
                                        lineTo(stroke[i].x, stroke[i].y)
                                    }
                                }
                                drawPath(
                                    path = path,
                                    color = colors.textMain,
                                    style = Stroke(
                                        width = 3.5.dp.toPx(),
                                        cap = StrokeCap.Round,
                                        join = StrokeJoin.Round
                                    )
                                )
                            }
                        }
                        if (currentStroke.size >= 2) {
                            val path = androidx.compose.ui.graphics.Path().apply {
                                moveTo(currentStroke[0].x, currentStroke[0].y)
                                for (i in 1 until currentStroke.size) {
                                    lineTo(currentStroke[i].x, currentStroke[i].y)
                                }
                            }
                            drawPath(
                                path = path,
                                color = colors.goldPrimary,
                                style = Stroke(
                                    width = 3.5.dp.toPx(),
                                    cap = StrokeCap.Round,
                                    join = StrokeJoin.Round
                                )
                            )
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "با ماوس یا قلم نوری در کادر بالا امضا کنید.",
                        fontSize = 11.sp,
                        color = colors.textMuted
                    )
                    OutlinedButton(
                        onClick = {
                            strokes.clear()
                            currentStroke = emptyList()
                        },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(30.dp).testTag("clear-signature-pad"),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                    ) {
                        Icon(Icons.Outlined.Delete, contentDescription = null, modifier = Modifier.size(14.dp), tint = colors.errorRed)
                        Spacer(Modifier.width(4.dp))
                        Text("پاک‌کردن", fontSize = 10.5.sp, color = colors.errorRed)
                    }
                }
            }
        },
        confirmButton = {
            Row(Modifier.width(360.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                GoldButton("انصراف", onDismiss, Modifier.weight(1f), isSecondary = true)
                GoldButton(
                    "تأیید و ذخیره امضا",
                    onClick = {
                        val allStrokes = if (currentStroke.isNotEmpty()) strokes + listOf(currentStroke) else strokes.toList()
                        val w = if (canvasSize.width > 0) canvasSize.width else 500
                        val h = if (canvasSize.height > 0) canvasSize.height else 200
                        onSaveSignature(allStrokes, w, h)
                    },
                    modifier = Modifier.weight(1.3f).testTag("save-signature-btn"),
                    enabled = strokes.isNotEmpty() || currentStroke.isNotEmpty()
                )
            }
        }
    )
}

internal fun saveSignatureStrokesToPng(
    strokes: List<List<Offset>>,
    width: Int,
    height: Int,
    destFile: File
) {
    val w = width.coerceAtLeast(100)
    val h = height.coerceAtLeast(60)
    val image = java.awt.image.BufferedImage(w, h, java.awt.image.BufferedImage.TYPE_INT_ARGB)
    val g2d = image.createGraphics()
    try {
        g2d.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON)
        g2d.setRenderingHint(java.awt.RenderingHints.KEY_STROKE_CONTROL, java.awt.RenderingHints.VALUE_STROKE_PURE)
        g2d.composite = java.awt.AlphaComposite.Clear
        g2d.fillRect(0, 0, w, h)
        g2d.composite = java.awt.AlphaComposite.SrcOver
        g2d.color = java.awt.Color(20, 20, 20)
        g2d.stroke = java.awt.BasicStroke(3.5f, java.awt.BasicStroke.CAP_ROUND, java.awt.BasicStroke.JOIN_ROUND)
        for (stroke in strokes) {
            if (stroke.isEmpty()) continue
            if (stroke.size == 1) {
                g2d.fillOval((stroke[0].x - 2).toInt(), (stroke[0].y - 2).toInt(), 4, 4)
                continue
            }
            val path = java.awt.geom.Path2D.Float()
            path.moveTo(stroke[0].x, stroke[0].y)
            for (i in 1 until stroke.size) {
                path.lineTo(stroke[i].x, stroke[i].y)
            }
            g2d.draw(path)
        }
    } finally {
        g2d.dispose()
    }
    javax.imageio.ImageIO.write(image, "PNG", destFile)
}

@Composable
internal fun rememberBrandAssetBitmap(pathOrUri: String): androidx.compose.ui.graphics.ImageBitmap? {
    return remember(pathOrUri) {
        if (pathOrUri.isBlank()) return@remember null
        runCatching {
            val file = when {
                pathOrUri.startsWith("file:") -> File(java.net.URI(pathOrUri))
                else -> File(pathOrUri)
            }
            if (!file.exists()) return@remember null
            val bytes = file.readBytes()
            org.jetbrains.skia.Image.makeFromEncoded(bytes).use { it.toComposeImageBitmap() }
        }.getOrNull()
    }
}

internal fun selectBrandLogoFile(): File? {
    val dialog = FileDialog(null as Frame?, "انتخاب لوگوی واحد صنفی (PNG / JPEG)", FileDialog.LOAD).apply {
        setFilenameFilter { _, name ->
            val l = name.lowercase()
            l.endsWith(".png") || l.endsWith(".jpg") || l.endsWith(".jpeg") || l.endsWith(".webp")
        }
        isVisible = true
    }
    val file = if (dialog.file != null && dialog.directory != null) {
        File(dialog.directory, dialog.file)
    } else null
    dialog.dispose()
    return file
}
