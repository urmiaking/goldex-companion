package com.goldex.companion.ui.hub

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.goldex.companion.data.AppSettings
import com.goldex.companion.ui.components.GoldButton
import com.goldex.companion.ui.theme.ButtonShape
import com.goldex.companion.ui.theme.LocalGoldExColors
import com.goldex.companion.ui.theme.LuxuryMotion
import com.goldex.companion.ui.theme.VazirmatnFamily
import com.goldex.companion.ui.theme.goldButtonContainer
import com.goldex.companion.ui.theme.goldButtonText
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun InvoiceBrandingModal(
    settings: AppSettings,
    onDismiss: () -> Unit,
    onSave: (AppSettings) -> Unit,
    onTestPdf: (AppSettings) -> Unit
) {
    val colors = LocalGoldExColors.current
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var draft by remember { mutableStateOf(settings) }
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(settings) { draft = settings }
    LaunchedEffect(Unit) { isVisible = true }

    val handleDismiss: () -> Unit = {
        if (isVisible) {
            coroutineScope.launch {
                isVisible = false
                delay(LuxuryMotion.DURATION_MODAL_EXIT.toLong())
                onDismiss()
            }
        }
    }
    val scrimAlpha by animateFloatAsState(
        targetValue = if (isVisible) 0.65f else 0f,
        animationSpec = tween(
            durationMillis = if (isVisible) LuxuryMotion.DURATION_MODAL_ENTER else LuxuryMotion.DURATION_MODAL_EXIT,
            easing = FastOutSlowInEasing
        ),
        label = "invoiceBrandingScrim"
    )

    val logoPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            persistReadPermission(context, it)
            draft = draft.copy(invoiceLogoUri = it.toString())
        }
    }
    val stampPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            persistReadPermission(context, it)
            draft = draft.copy(invoiceStampUri = it.toString())
        }
    }

    Dialog(
        onDismissRequest = handleDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = scrimAlpha))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = handleDismiss
                    ),
                contentAlignment = Alignment.BottomCenter
            ) {
                AnimatedVisibility(
                    visible = isVisible,
                    enter = LuxuryMotion.ModalEnter,
                    exit = LuxuryMotion.ModalExit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = {}
                            ),
                        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
                        color = colors.surface,
                        border = BorderStroke(
                            width = 1.dp,
                            brush = Brush.verticalGradient(
                                listOf(
                                    colors.goldPrimary.copy(alpha = 0.6f),
                                    colors.border.copy(alpha = 0.3f)
                                )
                            )
                        ),
                        shadowElevation = 24.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .wrapContentHeight()
                                .heightIn(max = 620.dp)
                                .navigationBarsPadding()
                        ) {
                            BrandingModalHeader(onDismiss = handleDismiss)
                            HorizontalDivider(color = colors.border.copy(alpha = 0.5f), thickness = 0.8.dp)

                            LazyColumn(
                                modifier = Modifier
                                    .weight(1f, fill = false)
                                    .fillMaxWidth()
                                    .imePadding(),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                                    start = 20.dp,
                                    end = 20.dp,
                                    top = 14.dp,
                                    bottom = 14.dp
                                ),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                item { LetterheadLivePreview(settings = draft) }
                                item {
                                    BrandAssetSection(
                                        settings = draft,
                                        onPickLogo = { logoPicker.launch(arrayOf("image/png", "image/jpeg", "image/webp")) },
                                        onClearLogo = { draft = draft.copy(invoiceLogoUri = "") },
                                        onWatermarkChange = { draft = draft.copy(invoiceWatermarkEnabled = it) }
                                    )
                                }
                                item {
                                    GuildInformationSection(
                                        settings = draft,
                                        onChange = { draft = it }
                                    )
                                }
                                item {
                                    DigitalStampSection(
                                        settings = draft,
                                        onPickStamp = { stampPicker.launch(arrayOf("image/png", "image/webp")) },
                                        onClearStamp = { draft = draft.copy(invoiceStampUri = "") },
                                        onEnabledChange = { draft = draft.copy(invoiceStampEnabled = it) },
                                        onOpacityChange = { draft = draft.copy(invoiceStampOpacity = it) }
                                    )
                                }
                                item {
                                    QrConfigurationSection(
                                        settings = draft,
                                        onChange = { draft = it }
                                    )
                                }
                                item {
                                    OutlinedButton(
                                        onClick = { onTestPdf(draft) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(42.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        border = BorderStroke(1.dp, colors.goldBorder),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.goldPrimary)
                                    ) {
                                        Text(
                                            "ساخت نمونه آزمایشی PDF",
                                            fontFamily = VazirmatnFamily,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            HorizontalDivider(color = colors.border.copy(alpha = 0.5f), thickness = 0.8.dp)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(colors.surfaceElevated.copy(alpha = 0.4f))
                                    .padding(horizontal = 20.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                GoldButton(
                                    text = "انصراف",
                                    onClick = handleDismiss,
                                    isSecondary = true,
                                    modifier = Modifier.weight(1f)
                                )
                                GoldButton(
                                    text = "ذخیره تغییرات",
                                    onClick = { onSave(draft) },
                                    isSecondary = false,
                                    icon = Icons.Default.Check,
                                    modifier = Modifier.weight(1.6f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BrandingModalHeader(onDismiss: () -> Unit) {
    val colors = LocalGoldExColors.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(width = 44.dp, height = 4.dp)
                .clip(CircleShape)
                .background(colors.border)
                .align(Alignment.CenterHorizontally)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    colors.goldContainer.copy(alpha = 0.4f),
                                    colors.goldContainer.copy(alpha = 0.7f)
                                )
                            )
                        )
                        .border(1.dp, colors.goldBorder, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        tint = colors.goldPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column {
                    Text(
                        "سربرگ و مهر اختصاصی",
                        fontFamily = VazirmatnFamily,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = colors.textMain
                    )
                    Text(
                        "هویت رسمی فاکتور، واترمارک و QR اصالت",
                        fontFamily = VazirmatnFamily,
                        fontSize = 10.5.sp,
                        color = colors.textMuted
                    )
                }
            }
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .size(32.dp)
                    .clip(ButtonShape)
                    .background(colors.surfaceElevated)
                    .border(0.6.dp, colors.goldBorder, ButtonShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "بستن",
                    tint = colors.textMuted,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun LetterheadLivePreview(settings: AppSettings) {
    val colors = LocalGoldExColors.current
    val logo = rememberPersistedBitmap(settings.invoiceLogoUri)
    val stamp = rememberPersistedBitmap(settings.invoiceStampUri)

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = if (colors.isDark) colors.surfaceElevated else Color(0xFFFCFAF5),
        border = BorderStroke(1.dp, colors.goldBorder.copy(alpha = 0.6f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "نمای زنده سربرگ فاکتور",
                    fontFamily = VazirmatnFamily,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textMain
                )
                Text(
                    "سند رسمی قیراط",
                    fontFamily = VazirmatnFamily,
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.goldPrimary
                )
            }
            Surface(
                shape = RoundedCornerShape(15.dp),
                color = Color(0xFFFFFEFA),
                border = BorderStroke(1.dp, colors.border.copy(alpha = 0.7f))
            ) {
                Column(
                    modifier = Modifier.padding(11.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(9.dp)
                        ) {
                            AssetPreview(bitmap = logo, fallback = settings.galleryName.take(2), square = true)
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    settings.galleryName.ifBlank { "نام واحد صنفی" },
                                    fontFamily = VazirmatnFamily,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFF1C1917),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    "کد صنفی ${settings.unionCode.ifBlank { "-" }} • ${settings.managerName}",
                                    fontFamily = VazirmatnFamily,
                                    fontSize = 9.sp,
                                    color = Color(0xFF78716C),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                            if (settings.invoiceStampEnabled) {
                                AssetPreview(
                                    bitmap = stamp,
                                    fallback = "مهر",
                                    square = false,
                                    opacity = settings.invoiceStampOpacity / 100f
                                )
                            }
                            QrPreview(size = 38.dp)
                        }
                    }
                    HorizontalDivider(color = Color(0xFFE7E5E4))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(
                            settings.galleryAddress,
                            modifier = Modifier.weight(1f),
                            fontFamily = VazirmatnFamily,
                            fontSize = 8.5.sp,
                            color = Color(0xFF57534E),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            settings.galleryPhone,
                            fontFamily = VazirmatnFamily,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF8A680E)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BrandAssetSection(
    settings: AppSettings,
    onPickLogo: () -> Unit,
    onClearLogo: () -> Unit,
    onWatermarkChange: (Boolean) -> Unit
) {
    val logo = rememberPersistedBitmap(settings.invoiceLogoUri)
    BrandingSectionCard(title = "نشان تجاری و واترمارک", badge = "PNG / JPG") {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(13.dp))
                .background(LocalGoldExColors.current.surfaceElevated.copy(alpha = 0.6f))
                .padding(10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                AssetPreview(bitmap = logo, fallback = "نشان", square = true, size = 48.dp)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        if (settings.invoiceLogoUri.isBlank()) "نشان پیش‌فرض قیراط" else "نشان اختصاصی فعال",
                        fontFamily = VazirmatnFamily,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = LocalGoldExColors.current.textMain
                    )
                    Text(
                        "پیشنهاد: تصویر مربع با پس‌زمینه شفاف",
                        fontFamily = VazirmatnFamily,
                        fontSize = 9.5.sp,
                        color = LocalGoldExColors.current.textMuted
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (settings.invoiceLogoUri.isNotBlank()) {
                    IconButton(onClick = onClearLogo, modifier = Modifier.size(34.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "حذف نشان", tint = LocalGoldExColors.current.errorRed)
                    }
                }
                OutlinedButton(
                    onClick = onPickLogo,
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 0.dp)
                ) {
                    Text("انتخاب", fontFamily = VazirmatnFamily, fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        SettingToggleRow(
            title = "واترمارک محو در جدول اقلام",
            subtitle = "نشانه امنیتی کم‌رنگ برای اصالت نسخه چاپی",
            checked = settings.invoiceWatermarkEnabled,
            onCheckedChange = onWatermarkChange
        )
    }
}

@Composable
private fun GuildInformationSection(settings: AppSettings, onChange: (AppSettings) -> Unit) {
    BrandingSectionCard(title = "اطلاعات صنفی سربرگ", badge = "اتحادیه") {
        BrandingTextField(
            label = "نام فروشگاه / بنکداری",
            value = settings.galleryName,
            onValueChange = { onChange(settings.copy(galleryName = it)) },
            trailing = { Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(17.dp)) }
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BrandingTextField(
                label = "پروانه / کد صنفی",
                value = settings.unionCode,
                onValueChange = { onChange(settings.copy(unionCode = it)) },
                modifier = Modifier.weight(1f)
            )
            BrandingTextField(
                label = "تلفن رسمی",
                value = settings.galleryPhone,
                onValueChange = { onChange(settings.copy(galleryPhone = it)) },
                keyboardType = KeyboardType.Phone,
                modifier = Modifier.weight(1f),
                trailing = { Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(17.dp)) }
            )
        }
        BrandingTextField(
            label = "نشانی دقیق واحد صنفی",
            value = settings.galleryAddress,
            onValueChange = { onChange(settings.copy(galleryAddress = it)) },
            singleLine = false,
            trailing = { Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(17.dp)) }
        )
    }
}

@Composable
private fun DigitalStampSection(
    settings: AppSettings,
    onPickStamp: () -> Unit,
    onClearStamp: () -> Unit,
    onEnabledChange: (Boolean) -> Unit,
    onOpacityChange: (Int) -> Unit
) {
    val stamp = rememberPersistedBitmap(settings.invoiceStampUri)
    BrandingSectionCard(title = "مهر دیجیتال و امضای زرگر", badge = "هویت صنفی") {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AssetPreview(
                bitmap = stamp,
                fallback = "مهر\n${settings.unionCode}",
                square = false,
                size = 66.dp,
                opacity = settings.invoiceStampOpacity / 100f
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        "شفافیت مهر",
                        fontFamily = VazirmatnFamily,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = LocalGoldExColors.current.textMain
                    )
                    Text(
                        "${settings.invoiceStampOpacity}٪",
                        fontFamily = VazirmatnFamily,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = LocalGoldExColors.current.goldPrimary
                    )
                }
                Slider(
                    value = settings.invoiceStampOpacity.toFloat(),
                    onValueChange = { onOpacityChange(it.toInt()) },
                    valueRange = 30f..100f,
                    colors = SliderDefaults.colors(
                        thumbColor = LocalGoldExColors.current.goldPrimary,
                        activeTrackColor = LocalGoldExColors.current.goldSecondary
                    )
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (settings.invoiceStampUri.isNotBlank()) {
                        OutlinedButton(
                            onClick = onClearStamp,
                            modifier = Modifier.height(34.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp)
                        ) { Text("حذف", fontFamily = VazirmatnFamily, fontSize = 10.sp) }
                    }
                    OutlinedButton(
                        onClick = onPickStamp,
                        modifier = Modifier.height(34.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp)
                    ) { Text("بارگذاری PNG", fontFamily = VazirmatnFamily, fontSize = 10.sp) }
                }
            }
        }
        SettingToggleRow(
            title = "درج خودکار مهر در انتهای فاکتور",
            subtitle = "برای PDF و چاپ رسمی فعال می‌شود",
            checked = settings.invoiceStampEnabled,
            onCheckedChange = onEnabledChange
        )
    }
}

@Composable
private fun QrConfigurationSection(settings: AppSettings, onChange: (AppSettings) -> Unit) {
    BrandingSectionCard(title = "QR اصالت و رهگیری فاکتور", badge = "شبکه قیراط") {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Surface(
                modifier = Modifier.size(82.dp),
                shape = RoundedCornerShape(14.dp),
                color = LocalGoldExColors.current.surfaceElevated,
                border = BorderStroke(1.dp, LocalGoldExColors.current.border)
            ) { Box(contentAlignment = Alignment.Center) { QrPreview(size = 58.dp) } }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                QrOption(
                    text = "استعلام اصالت فاکتور",
                    checked = settings.invoiceQrVerificationEnabled,
                    onCheckedChange = { onChange(settings.copy(invoiceQrVerificationEnabled = it)) }
                )
                QrOption(
                    text = "شناسنامه سنگ و نگین",
                    checked = settings.invoiceQrGemCertificateEnabled,
                    onCheckedChange = { onChange(settings.copy(invoiceQrGemCertificateEnabled = it)) }
                )
                QrOption(
                    text = "کاتالوگ و شبکه اجتماعی",
                    checked = settings.invoiceQrCatalogEnabled,
                    onCheckedChange = { onChange(settings.copy(invoiceQrCatalogEnabled = it)) }
                )
            }
        }
    }
}

@Composable
private fun BrandingSectionCard(
    title: String,
    badge: String,
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = LocalGoldExColors.current
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = colors.surface,
        border = BorderStroke(1.dp, colors.border.copy(alpha = 0.75f)),
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(13.dp),
            verticalArrangement = Arrangement.spacedBy(11.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    title,
                    fontFamily = VazirmatnFamily,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = colors.textMain
                )
                Text(
                    badge,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(colors.goldContainer.copy(alpha = 0.55f))
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                    fontFamily = VazirmatnFamily,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.goldPrimary
                )
            }
            content()
        }
    }
}

@Composable
private fun BrandingTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    keyboardType: KeyboardType = KeyboardType.Text,
    trailing: (@Composable (() -> Unit))? = null
) {
    val colors = LocalGoldExColors.current
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            label,
            fontFamily = VazirmatnFamily,
            fontSize = 10.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = colors.textSecondary
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = singleLine,
            maxLines = if (singleLine) 1 else 2,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            trailingIcon = trailing,
            textStyle = LocalTextStyle.current.copy(
                fontFamily = VazirmatnFamily,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Right
            ),
            shape = RoundedCornerShape(13.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = colors.goldPrimary,
                unfocusedBorderColor = colors.border,
                focusedContainerColor = colors.surface,
                unfocusedContainerColor = colors.surfaceElevated.copy(alpha = 0.55f)
            )
        )
    }
}

@Composable
private fun SettingToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val colors = LocalGoldExColors.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontFamily = VazirmatnFamily, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = colors.textMain)
            Text(subtitle, fontFamily = VazirmatnFamily, fontSize = 9.5.sp, color = colors.textMuted)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = colors.goldButtonText,
                checkedTrackColor = colors.goldButtonContainer
            )
        )
    }
}

@Composable
private fun QrOption(text: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = CheckboxDefaults.colors(checkedColor = LocalGoldExColors.current.goldPrimary)
        )
        Text(
            text,
            fontFamily = VazirmatnFamily,
            fontSize = 10.5.sp,
            fontWeight = FontWeight.Medium,
            color = if (checked) LocalGoldExColors.current.textMain else LocalGoldExColors.current.textMuted
        )
    }
}

@Composable
private fun AssetPreview(
    bitmap: ImageBitmap?,
    fallback: String,
    square: Boolean,
    size: androidx.compose.ui.unit.Dp = 42.dp,
    opacity: Float = 1f
) {
    val shape = if (square) RoundedCornerShape(11.dp) else CircleShape
    Box(
        modifier = Modifier
            .size(size)
            .clip(shape)
            .background(if (square) Color(0xFFFFFBEB) else Color(0xFFFFF1F2))
            .then(
                if (square) Modifier.border(1.dp, Color(0x33D4AF37), shape)
                else Modifier.border(1.dp, Color(0xB9B91C1C), shape)
            ),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit,
                alpha = opacity
            )
        } else {
            Text(
                fallback,
                modifier = if (square) Modifier else Modifier.rotate(-6f),
                fontFamily = VazirmatnFamily,
                fontSize = if (square) 9.sp else 8.sp,
                lineHeight = 10.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                color = if (square) Color(0xFF8A680E) else Color(0xFFB91C1C)
            )
        }
    }
}

@Composable
private fun QrPreview(size: androidx.compose.ui.unit.Dp) {
    val pattern = remember {
        listOf(
            "111010111", "101010101", "111110111", "000101000", "111011101",
            "101110001", "111011111", "001101001", "111001111"
        )
    }
    Column(modifier = Modifier.size(size), verticalArrangement = Arrangement.SpaceEvenly) {
        pattern.forEach { line ->
            Row(modifier = Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.SpaceEvenly) {
                line.forEach { value ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(if (value == '1') Color(0xFF1C1917) else Color.Transparent)
                    )
                }
            }
        }
    }
}

@Composable
private fun rememberPersistedBitmap(uriValue: String): ImageBitmap? {
    val context = LocalContext.current
    return remember(uriValue) {
        if (uriValue.isBlank()) return@remember null
        runCatching {
            context.contentResolver.openInputStream(Uri.parse(uriValue))
                ?.use { BitmapFactory.decodeStream(it) }
                ?.asImageBitmap()
        }.getOrNull()
    }
}

private fun persistReadPermission(context: Context, uri: Uri) {
    runCatching {
        context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
}
