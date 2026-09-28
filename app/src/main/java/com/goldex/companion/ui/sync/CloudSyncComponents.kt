package com.goldex.companion.ui.sync

import android.provider.Settings
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.goldex.companion.data.sync.*
import com.goldex.companion.model.PersianNumberFormatter
import com.goldex.companion.ui.components.GoldButton
import com.goldex.companion.ui.components.GoldInputField
import com.goldex.companion.ui.components.LuxuryCard
import com.goldex.companion.ui.hub.HubCloud
import com.goldex.companion.ui.hub.HubCloudDownload
import com.goldex.companion.ui.hub.HubCloudOff
import com.goldex.companion.ui.hub.HubDevices
import com.goldex.companion.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

fun SyncStatus.title()=when(this) {
    SyncStatus.DISABLED -> "همگام‌سازی خاموش است"
    SyncStatus.AUTH_REQUIRED -> "ورود به حساب لازم است"
    SyncStatus.PENDING -> "در انتظار همگام‌سازی"
    SyncStatus.SYNCING -> "در حال همگام‌سازی"
    SyncStatus.SYNCED -> "اطلاعات همگام است"
    SyncStatus.OFFLINE -> "آفلاین؛ اطلاعات محلی محفوظ است"
    SyncStatus.LICENSE_REQUIRED -> "مجوز معتبر لازم است"
    SyncStatus.CONFLICT -> "اختلاف نسخه؛ بررسی لازم است"
    SyncStatus.RESTORE_REQUIRED -> "بازیابی اطلاعات لازم است"
    SyncStatus.WRITER_CHANGED -> "دستگاه نویسنده تغییر کرده است"
    SyncStatus.ERROR -> "ارتباط با ابر ناموفق بود"
}

@Composable
fun CloudSyncButton(state: SyncUiState, reducedMotion: Boolean? = null, onClick: () -> Unit) {
    val colors = LocalGoldExColors.current
    val context = LocalContext.current
    val reduced = reducedMotion ?: (Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f)
    val contentDesc = state.status.title()

    Surface(
        onClick = onClick,
        shape = ButtonShape,
        color = colors.surface,
        border = BorderStroke(0.6.dp, colors.goldBorder),
        shadowElevation = if (colors.isDark) 0.dp else 1.5.dp,
        modifier = Modifier
            .padding(end = 12.dp)
            .size(40.dp)
            .semantics { contentDescription = contentDesc }
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (!state.enabled || state.status == SyncStatus.DISABLED) {
                Icon(
                    imageVector = HubCloudOff,
                    contentDescription = contentDesc,
                    tint = colors.textMuted,
                    modifier = Modifier.size(20.dp)
                )
            } else {
                Icon(
                    imageVector = HubCloud,
                    contentDescription = contentDesc,
                    tint = if (state.status == SyncStatus.SYNCING) colors.syncBlue else colors.goldPrimary,
                    modifier = Modifier.size(20.dp)
                )

                if (state.status != SyncStatus.OFFLINE) {
                    val tint = when (state.status) {
                        SyncStatus.SYNCED -> colors.profitGreen
                        SyncStatus.SYNCING -> colors.syncBlue
                        SyncStatus.ERROR, SyncStatus.CONFLICT, SyncStatus.LICENSE_REQUIRED -> colors.syncWarning
                        else -> colors.textMuted
                    }
                    val icon = when (state.status) {
                        SyncStatus.SYNCED -> Icons.Default.Check
                        SyncStatus.SYNCING -> Icons.Default.Refresh
                        SyncStatus.AUTH_REQUIRED -> Icons.Default.AccountCircle
                        SyncStatus.LICENSE_REQUIRED, SyncStatus.WRITER_CHANGED -> Icons.Default.Lock
                        SyncStatus.ERROR, SyncStatus.CONFLICT, SyncStatus.RESTORE_REQUIRED -> Icons.Default.Warning
                        else -> Icons.Default.Info
                    }

                    var rotation by remember { mutableFloatStateOf(0f) }
                    if (state.status == SyncStatus.SYNCING && !reduced) {
                        val transition = rememberInfiniteTransition(label = "sync_rotation")
                        val angle by transition.animateFloat(
                            initialValue = 0f,
                            targetValue = 360f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(1000, easing = LinearEasing),
                                repeatMode = RepeatMode.Restart
                            ),
                            label = "sync_angle"
                        )
                        rotation = angle
                    }

                    AnimatedContent(
                        targetState = state.status,
                        transitionSpec = {
                            val animate = !reduced && initialState == SyncStatus.SYNCING && targetState == SyncStatus.SYNCED
                            (fadeIn(tween(if (animate) 250 else 0)) + scaleIn(animationSpec = tween(if (animate) 250 else 0), initialScale = if (animate) .75f else 1f))
                                .togetherWith(fadeOut(tween(if (animate) 150 else 0)))
                        },
                        label = "cloud_status_badge",
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(2.dp)
                    ) { _ ->
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .clip(CircleShape)
                                .background(colors.surface)
                                .padding(1.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = contentDesc,
                                tint = tint,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .rotate(rotation)
                            )
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(3.dp)
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(colors.errorRed)
                    )
                }
            }
        }
    }
}

@Composable
fun CloudSettingsContent(
    viewModel: CloudSyncViewModel,
    modifier: Modifier = Modifier,
    scrollable: Boolean = true
) {
    val state by viewModel.state.collectAsState()
    val form by viewModel.form.collectAsState()
    val colors = LocalGoldExColors.current
    val context = LocalContext.current

    LaunchedEffect(form.backupPath) {
        form.backupPath?.let { path ->
            val uri = androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", java.io.File(path))
            val intent = android.content.Intent(android.content.Intent.ACTION_SEND)
                .setType("application/json")
                .putExtra(android.content.Intent.EXTRA_STREAM, uri)
                .addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            context.startActivity(android.content.Intent.createChooser(intent, "ذخیرهٔ پشتیبان محلی"))
            viewModel.backupShared()
        }
    }

    var phone by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(form.phone) }
    var code by androidx.compose.runtime.saveable.rememberSaveable(form.challenge?.optString("challengeId")) { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }

    val scrollModifier = if (scrollable) Modifier.verticalScroll(rememberScrollState()) else Modifier
    Column(
        modifier = modifier
            .fillMaxWidth()
            .then(scrollModifier)
            .padding(if (scrollable) 20.dp else 0.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        CloudConnectionToggle(state.enabled, !form.busy && !state.busy, viewModel::setEnabled)

        if (state.enabled) {
            if (state.phone.isNotBlank() && state.status != SyncStatus.AUTH_REQUIRED) {
                CloudAccountSummary(state)
            }

            if (state.message.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = colors.syncWarning.copy(alpha = 0.15f),
                    border = BorderStroke(0.8.dp, colors.syncWarning.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = colors.syncWarning, modifier = Modifier.size(18.dp))
                        Text(state.message, color = colors.syncWarning, fontSize = 12.sp, lineHeight = 18.sp)
                    }
                }
            }

            if (state.phone.isBlank() || state.status == SyncStatus.AUTH_REQUIRED) {
                CloudSignInForm(
                    phone = phone,
                    code = code,
                    form = form,
                    onPhoneChange = { phone = it },
                    onCodeChange = { code = it },
                    onRequest = { viewModel.requestCode(phone) },
                    onVerify = { viewModel.verifyCode(code) },
                    onChangePhone = viewModel::changePhone,
                    onResend = { viewModel.requestCode(form.phone) }
                )

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = colors.goldContainer.copy(alpha = 0.35f),
                    border = BorderStroke(0.8.dp, colors.goldBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("🎁", fontSize = 16.sp)
                            Text(
                                text = "دورهٔ آزمایشی ۱۴ روزه رایگان",
                                fontWeight = FontWeight.Bold,
                                color = colors.goldPrimary,
                                fontSize = 13.sp
                            )
                        }
                        Text(
                            text = "امکان استفاده کامل از خدمات ابری و همگام‌سازی بدون نیاز به پرداخت.",
                            color = colors.textSecondary,
                            style = MaterialTheme.typography.bodySmall
                        )
                        GoldButton(
                            text = "فعال‌سازی دورهٔ آزمایشی",
                            onClick = viewModel::activateTrial,
                            isLoading = form.busy,
                            enabled = !form.busy,
                            isSecondary = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            } else {
                GoldButton(
                    text = if (state.busy || form.busy || state.status == SyncStatus.SYNCING) "در حال همگام‌سازی..." else "همگام‌سازی اکنون",
                    onClick = viewModel::sync,
                    isLoading = state.busy || form.busy || state.status == SyncStatus.SYNCING,
                    enabled = !state.busy && !form.busy,
                    modifier = Modifier.fillMaxWidth()
                )

                if (state.status == SyncStatus.CONFLICT) {
                    GoldButton(
                        text = "مقایسهٔ نسخه‌های کل گروه",
                        onClick = viewModel::reviewConflict,
                        isSecondary = true,
                        enabled = !form.busy,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (state.status in listOf(SyncStatus.RESTORE_REQUIRED, SyncStatus.CONFLICT)) {
                    GoldButton(
                        text = "بررسی و بازیابی نسخهٔ ابری",
                        onClick = { confirm = "restore" },
                        isSecondary = true,
                        enabled = !form.busy,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (state.status == SyncStatus.WRITER_CHANGED) {
                    GoldButton(
                        text = "انتقال نویسندگی به این دستگاه",
                        onClick = { confirm = "takeover" },
                        isSecondary = false,
                        enabled = !form.busy,
                        modifier = Modifier.fillMaxWidth()
                    )
                    GoldButton(
                        text = "تأیید تازهٔ شماره برای انتقال",
                        onClick = viewModel::reauthenticate,
                        isSecondary = true,
                        enabled = !form.busy,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (state.status == SyncStatus.SYNCED) {
                    GoldButton(
                        text = "انتقال به دستگاه دیگر",
                        icon = HubDevices,
                        onClick = viewModel::listDevices,
                        isSecondary = true,
                        enabled = !form.busy,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                GoldButton(
                    text = "خروج از حساب",
                    onClick = { confirm = "logout" },
                    isSecondary = true,
                    enabled = !form.busy && !state.busy,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        if (state.phone.isNotBlank() || state.readOnly) {
            GoldButton(
                text = "ذخیرهٔ پشتیبان محلی و اختلاف‌ها",
                icon = HubCloudDownload,
                onClick = viewModel::exportBackup,
                isSecondary = true,
                enabled = !form.busy && !state.busy,
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (state.readOnly || state.phone.isNotBlank() || state.status == SyncStatus.WRITER_CHANGED) {
            GoldButton(
                text = "جداسازی داده برای اتصال به حساب دیگر",
                onClick = { confirm = "detach" },
                isSecondary = true,
                enabled = !form.busy && !state.busy,
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (form.error.isNotBlank()) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = colors.errorRed.copy(alpha = 0.15f),
                border = BorderStroke(0.8.dp, colors.errorRed.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = colors.errorRed, modifier = Modifier.size(18.dp))
                    Text(form.error, color = colors.errorRed, fontSize = 12.sp, lineHeight = 18.sp)
                }
            }
            if (form.error.contains("مجوز") || state.status == SyncStatus.LICENSE_REQUIRED) {
                GoldButton(
                    text = "فعال‌سازی مهلت تست ۱۴ روزه رایگان",
                    onClick = { viewModel.activateTrial() },
                    isLoading = form.busy,
                    enabled = !form.busy,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Surface(
            shape = RoundedCornerShape(12.dp),
            color = colors.surfaceElevated.copy(alpha = 0.5f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "خاموش‌کردن سینک یا پایان مجوز، اطلاعات محلی و نسخهٔ ابری را حذف نمی‌کند. سینک جایگزین پشتیبان مستقل نیست.",
                color = colors.textMuted,
                style = MaterialTheme.typography.bodySmall,
                lineHeight = 18.sp,
                modifier = Modifier.padding(12.dp)
            )
        }
    }

    form.review?.let { review ->
        Dialog(onDismissRequest = viewModel::closeReview) {
            Surface(shape = RoundedCornerShape(20.dp), color = colors.surface, border = BorderStroke(1.dp, colors.goldBorder.copy(alpha = 0.6f)), modifier = Modifier.heightIn(max = 680.dp)) {
                Column(Modifier.padding(20.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("مقایسهٔ اختلاف‌ها", style = MaterialTheme.typography.titleLarge, color = colors.textMain, fontWeight = FontWeight.Bold)
                    Text("انتخاب نسخهٔ محلی برای تمام رکوردهای این گروه اعمال می‌شود. محاسبات مالی دوباره اجرا نمی‌شوند. هر دو نسخه در پشتیبان محفوظ می‌مانند. برای انتخاب نسخهٔ ابری، بازیابی کامل با تأیید جداگانه انجام دهید.", color = colors.textSecondary, fontSize = 12.sp, lineHeight = 18.sp)
                    val local = review.getJSONArray("local")
                    val remote = review.getJSONArray("remote")
                    for (i in 0 until local.length()) {
                        val c = local.getJSONObject(i)
                        val r = remote.getJSONObject(i)
                        Text("${cloudRecordTitle(c.getString("type"))} · ${PersianNumberFormatter.toPersianDigits(c.getString("id"))}", color = colors.goldPrimary, fontWeight = FontWeight.Bold)
                        val l = c.optJSONObject("localPayload")
                        val server = r.optJSONObject("payload")
                        val fields = ((l?.keys()?.asSequence()?.toList() ?: emptyList()) + (server?.keys()?.asSequence()?.toList() ?: emptyList())).distinct()
                        if (l == null) Text("نسخهٔ محلی: حذف رکورد", color = colors.syncWarning)
                        if (r.getBoolean("deleted")) Text("نسخهٔ ابری: حذف رکورد؛ شناسهٔ حذف‌شده قابل بازگردانی نیست", color = colors.syncWarning)
                        for (field in fields) if (SyncJson.canonical(l?.opt(field)) != SyncJson.canonical(server?.opt(field))) {
                            Text(field, color = colors.textMuted, style = MaterialTheme.typography.labelMedium)
                            Text("محلی: ${PersianNumberFormatter.toPersianDigits(l?.opt(field)?.toString() ?: "حذف‌شده")}", color = colors.textMain)
                            Text("ابری: ${PersianNumberFormatter.toPersianDigits(server?.opt(field)?.toString() ?: "حذف‌شده")}", color = colors.textSecondary)
                        }
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        GoldButton("بستن", onClick = viewModel::closeReview, isSecondary = true, modifier = Modifier.weight(1f))
                        GoldButton("انتخاب محلی کل گروه", onClick = { confirm = "local" }, enabled = !form.busy, modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }

    form.devices?.let { devices ->
        Dialog(onDismissRequest = viewModel::closeReview) {
            Surface(shape = RoundedCornerShape(20.dp), color = colors.surface, border = BorderStroke(1.dp, colors.goldBorder.copy(alpha = 0.6f))) {
                Column(Modifier.padding(20.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("انتقال به دستگاه دیگر", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = colors.textMain)
                    Text("دستگاه مقصد باید قبلاً با همین حساب وارد شده باشد. صف این گوشی پیش از انتقال کامل ارسال می‌شود.", color = colors.textSecondary, fontSize = 12.sp, lineHeight = 18.sp)
                    val list = devices.getJSONArray("devices")
                    for (i in 0 until list.length()) {
                        val device = list.getJSONObject(i)
                        if (!device.getBoolean("isWriter")) {
                            GoldButton(
                                text = "انتقال به ${device.getString("id").take(8)}",
                                icon = HubDevices,
                                onClick = { confirm = "release:${device.getString("id")}" },
                                isSecondary = true,
                                enabled = !form.busy,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                    GoldButton("بستن", onClick = viewModel::closeReview, isSecondary = true, modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }

    if (confirm.isNotBlank()) {
        Dialog(onDismissRequest = { confirm = "" }) {
            Surface(shape = RoundedCornerShape(20.dp), color = colors.surface, border = BorderStroke(1.dp, colors.goldBorder.copy(alpha = 0.6f))) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        text = when {
                            confirm == "detach" -> "جداسازی داده از حساب ابری"
                            confirm == "local" -> "تأیید نسخهٔ محلی"
                            confirm.startsWith("release:") -> "انتقال نویسندگی"
                            confirm == "restore" -> "بازیابی اطلاعات از ابر"
                            confirm == "takeover" -> "انتقال نویسندگی به این گوشی"
                            else -> "خروج از حساب ابری"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = colors.textMain
                    )
                    Text(
                        text = when {
                            confirm == "detach" -> "پس از ساخت پشتیبان، دادهٔ فعلی به‌صورت محلی نگهداری و ارتباط آن با حساب و صف قبلی جدا می‌شود. برای حساب دارای داده، بازیابی با تأیید جداگانه لازم است. پیش از ادامه پشتیبان را در جای امن ذخیره کنید."
                            confirm == "local" -> "نسخهٔ محلی برای تمام رکوردهای گروه انتخاب شود؟ هر اختلاف مالی این گروه باید با هم بررسی شود. در صورت تغییر دوبارهٔ سرور، ارسال مجدداً متوقف خواهد شد."
                            confirm.startsWith("release:") -> "پس از ارسال کامل صف، نویسندگی به دستگاه انتخاب‌شده منتقل می‌شود. ویرایش و ارسال این گوشی متوقف می‌شود."
                            confirm == "restore" -> "دادهٔ فعال این نصب با نسخهٔ ابری جایگزین می‌شود. پیش از جایگزینی، پشتیبان اطلاعات و صف محلی در حافظهٔ برنامه نگهداری می‌شود. در صورت اختلاف مالی، کل مجموعه بررسی شود."
                            confirm == "takeover" -> "تغییرات ارسال‌نشدهٔ گوشی قبلی در نسخهٔ ابری نیستند. گوشی قبلی حق ارسال را از دست می‌دهد. تأیید تازهٔ موبایل و سپس بازیابی نسخهٔ ابری لازم است."
                            else -> "سینک خاموش می‌شود و اطلاعات و صف ارسال روی این دستگاه باقی می‌مانند."
                        },
                        color = colors.textSecondary,
                        fontSize = 12.5.sp,
                        lineHeight = 19.sp
                    )
                    // RTL dialog buttons: Cancel on RIGHT (first child), Confirm on LEFT (second child)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        GoldButton("انصراف", onClick = { confirm = "" }, isSecondary = true, modifier = Modifier.weight(1f))
                        GoldButton(
                            "تأیید",
                            onClick = {
                                val action = confirm
                                confirm = ""
                                when {
                                    action == "detach" -> viewModel.detach()
                                    action == "local" -> viewModel.chooseLocal()
                                    action.startsWith("release:") -> viewModel.releaseWriter(action.removePrefix("release:"))
                                    action == "restore" -> viewModel.restore()
                                    action == "takeover" -> viewModel.takeover()
                                    else -> viewModel.logout()
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Dedicated, luxury Cloud Sync Section designed specifically for the Onboarding Wizard.
 * Features dual selectable option cards (Offline by default, Online with animated expansion),
 * phone authentication, and a prominent celebratory cloud data restoration experience.
 */
@Composable
fun WizardCloudSyncSection(
    viewModel: CloudSyncViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsState()
    val form by viewModel.form.collectAsState()
    val colors = LocalGoldExColors.current

    // Offline is default unless user already enabled cloud or has entered phone
    var isOnlineSelected by androidx.compose.runtime.saveable.rememberSaveable {
        mutableStateOf(state.enabled || state.phone.isNotBlank())
    }

    var phone by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(form.phone) }
    var code by androidx.compose.runtime.saveable.rememberSaveable(form.challenge?.optString("challengeId")) { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }

    LuxuryCard(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Section Header
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
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
                            .size(4.dp, 16.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(colors.goldPrimary)
                    )
                    Text(
                        text = "نحوه ذخیره و همگام‌سازی اطلاعات",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textMain
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = colors.surfaceElevated,
                    border = BorderStroke(0.6.dp, colors.border)
                ) {
                    Text(
                        text = "اختیاری",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = colors.textMuted,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
            Text(
                text = "محل نگهداری اطلاعات را تعیین کنید؛ برنامه به صورت پیش‌فرض کاملاً آفلاین کار می‌کند.",
                fontSize = 11.sp,
                color = colors.textSecondary
            )
        }

        // Dual Square Selection Cards in One Row (No RadioButtons, No Extra Text)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 1. Offline Mode Card (Right side in RTL, first child)
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .height(112.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .clickable {
                        isOnlineSelected = false
                        viewModel.setEnabled(false)
                    },
                shape = RoundedCornerShape(16.dp),
                color = if (!isOnlineSelected) colors.goldContainer.copy(alpha = 0.35f) else colors.surfaceElevated,
                border = BorderStroke(
                    if (!isOnlineSelected) 1.5.dp else 0.8.dp,
                    if (!isOnlineSelected) colors.goldPrimary else colors.border
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (!isOnlineSelected) colors.goldPrimary.copy(alpha = 0.15f) else colors.surface),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = HubCloudOff,
                            contentDescription = null,
                            tint = if (!isOnlineSelected) colors.goldPrimary else colors.textMuted,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "ذخیره در دستگاه",
                        fontSize = 12.sp,
                        fontWeight = if (!isOnlineSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (!isOnlineSelected) colors.textMain else colors.textSecondary,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "(آفلاین)",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Normal,
                        color = if (!isOnlineSelected) colors.goldPrimary else colors.textMuted,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // 2. Online Mode Card (Left side in RTL, second child)
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .height(112.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .clickable {
                        isOnlineSelected = true
                        viewModel.setEnabled(true)
                    },
                shape = RoundedCornerShape(16.dp),
                color = if (isOnlineSelected) colors.goldContainer.copy(alpha = 0.35f) else colors.surfaceElevated,
                border = BorderStroke(
                    if (isOnlineSelected) 1.5.dp else 0.8.dp,
                    if (isOnlineSelected) colors.goldPrimary else colors.border
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isOnlineSelected) colors.goldPrimary.copy(alpha = 0.15f) else colors.surface),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = HubCloud,
                            contentDescription = null,
                            tint = if (isOnlineSelected) colors.goldPrimary else colors.textMuted,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "همگام‌سازی ابری",
                        fontSize = 12.sp,
                        fontWeight = if (isOnlineSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isOnlineSelected) colors.textMain else colors.textSecondary,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "(آنلاین)",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Normal,
                        color = if (isOnlineSelected) colors.goldPrimary else colors.textMuted,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // Animated Content for Online Mode
        AnimatedVisibility(
            visible = isOnlineSelected,
            enter = expandVertically(animationSpec = tween(350)) + fadeIn(animationSpec = tween(350)),
            exit = shrinkVertically(animationSpec = tween(300)) + fadeOut(animationSpec = tween(300))
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                HorizontalDivider(color = colors.border.copy(alpha = 0.5f), thickness = 0.6.dp)

                // If backup / restore is detected on cloud server
                if (state.status in listOf(SyncStatus.RESTORE_REQUIRED, SyncStatus.CONFLICT)) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        color = colors.goldContainer.copy(alpha = 0.35f),
                        border = BorderStroke(1.2.dp, colors.goldPrimary)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(colors.goldPrimary),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = HubCloudDownload,
                                        contentDescription = null,
                                        tint = colors.surface,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "نسخهٔ پشتیبان ابری شما پیدا شد! 🎉",
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.textMain
                                    )
                                    Text(
                                        text = "اطلاعات گالری و فاکتورهای حساب شماره ${PersianNumberFormatter.toPersianDigits(state.phone)} در سرور موجود است.",
                                        fontSize = 11.sp,
                                        color = colors.textSecondary,
                                        lineHeight = 16.sp
                                    )
                                }
                            }

                            Text(
                                text = "آیا مایلید اطلاعات گذشته شما بر روی این دستگاه بازیابی و جایگزین شود؟",
                                fontSize = 11.5.sp,
                                color = colors.textMain,
                                fontWeight = FontWeight.Medium
                            )

                            // RTL buttons: Secondary (cancel/ignore) on RIGHT, Primary (restore) on LEFT
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                GoldButton(
                                    text = "صرف‌نظر و شروع تازه",
                                    onClick = { viewModel.chooseLocal() },
                                    isSecondary = true,
                                    enabled = !form.busy && !state.busy,
                                    modifier = Modifier.weight(1f)
                                )
                                GoldButton(
                                    text = if (state.busy || form.busy) "در حال بازیابی..." else "بازیابی کامل اطلاعات",
                                    icon = HubCloudDownload,
                                    onClick = { confirm = "restore" },
                                    isLoading = state.busy || form.busy,
                                    enabled = !state.busy && !form.busy,
                                    modifier = Modifier.weight(1.5f)
                                )
                            }
                        }
                    }
                } else if (state.restoredGeneration > 0) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = colors.profitGreen.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, colors.profitGreen.copy(alpha = 0.4f))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(colors.profitGreen),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "اطلاعات ابری با موفقیت بازیابی شد ✓",
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.profitGreen
                                )
                                Text(
                                    text = "کلیه سوابق، دفاتر و فاکتورها روی این دستگاه قرار گرفتند.",
                                    fontSize = 10.5.sp,
                                    color = colors.textSecondary
                                )
                            }
                        }
                    }
                }

                // If not signed in / phone needed
                if (state.phone.isBlank() || state.status == SyncStatus.AUTH_REQUIRED) {
                    CloudSignInForm(
                        phone = phone,
                        code = code,
                        form = form,
                        onPhoneChange = { phone = it },
                        onCodeChange = { code = it },
                        onRequest = { viewModel.requestCode(phone) },
                        onVerify = { viewModel.verifyCode(code) },
                        onChangePhone = viewModel::changePhone,
                        onResend = { viewModel.requestCode(form.phone) }
                    )

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = colors.goldContainer.copy(alpha = 0.25f),
                        border = BorderStroke(0.8.dp, colors.goldBorder.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("💡", fontSize = 16.sp)
                            Text(
                                text = "در صورت داشتن حساب قبلی، پس از تأیید شماره، نسخه پشتیبان به صورت خودکار شناسایی و پیشنهاد بازیابی داده می‌شود.",
                                fontSize = 11.sp,
                                color = colors.textSecondary,
                                lineHeight = 16.sp
                            )
                        }
                    }
                } else if (state.status !in listOf(SyncStatus.RESTORE_REQUIRED, SyncStatus.CONFLICT)) {
                    CloudAccountSummary(state)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        GoldButton(
                            text = "خروج از حساب",
                            onClick = { confirm = "logout" },
                            isSecondary = true,
                            enabled = !form.busy && !state.busy,
                            modifier = Modifier.weight(1f)
                        )
                        GoldButton(
                            text = if (state.busy || form.busy || state.status == SyncStatus.SYNCING) "در حال همگام‌سازی..." else "همگام‌سازی اکنون",
                            onClick = viewModel::sync,
                            isLoading = state.busy || form.busy || state.status == SyncStatus.SYNCING,
                            enabled = !state.busy && !form.busy,
                            modifier = Modifier.weight(1.5f)
                        )
                    }
                }

                if (form.error.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = colors.errorRed.copy(alpha = 0.12f),
                        border = BorderStroke(0.8.dp, colors.errorRed.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = colors.errorRed, modifier = Modifier.size(16.dp))
                            Text(form.error, color = colors.errorRed, fontSize = 11.5.sp)
                        }
                    }
                    if (form.error.contains("مجوز") || state.status == SyncStatus.LICENSE_REQUIRED) {
                        GoldButton(
                            text = "فعال‌سازی مهلت تست ۱۴ روزه رایگان",
                            onClick = { viewModel.activateTrial() },
                            isLoading = form.busy,
                            enabled = !form.busy,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }

    // Confirmation Dialogs (RTL: Cancel on RIGHT, Confirm on LEFT)
    if (confirm.isNotBlank()) {
        Dialog(
            onDismissRequest = { confirm = "" },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .clip(RoundedCornerShape(20.dp)),
                color = colors.surface,
                border = BorderStroke(1.dp, colors.goldBorder)
            ) {
                Column(
                    modifier = Modifier.padding(22.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = when (confirm) {
                            "restore" -> "بازیابی اطلاعات از حساب ابری"
                            "local" -> "صرف‌نظر و شروع تازه"
                            else -> "خروج از حساب ابری"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = colors.textMain
                    )
                    Text(
                        text = when (confirm) {
                            "restore" -> "اطلاعات موجود روی سرور ابری بر روی این دستگاه بازیابی و جایگزین خواهد شد. آیا ادامه می‌دهید؟"
                            "local" -> "آیا مایلید نسخه ابری قبلی را نادیده گرفته و به عنوان اطلاعات جدید ادامه دهید؟"
                            else -> "سینک خاموش می‌شود و ارتباط با حساب ابری قطع خواهد شد."
                        },
                        color = colors.textSecondary,
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        GoldButton(
                            text = "انصراف",
                            onClick = { confirm = "" },
                            isSecondary = true,
                            modifier = Modifier.weight(1f)
                        )
                        GoldButton(
                            text = if (confirm == "restore") "تأیید و بازیابی" else "تأیید",
                            onClick = {
                                val action = confirm
                                confirm = ""
                                when (action) {
                                    "restore" -> viewModel.restore()
                                    "local" -> viewModel.chooseLocal()
                                    else -> viewModel.logout()
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CloudSettingsDialog(viewModel: CloudSyncViewModel, onDismiss: () -> Unit, canDismiss: Boolean = true) {
    CloudSettingsModal(onDismiss = onDismiss, canDismiss = canDismiss) { CloudSettingsContent(viewModel) }
}

@Composable
fun CloudConnectionToggle(enabled: Boolean, interactive: Boolean, onChange: (Boolean) -> Unit) {
    val colors = LocalGoldExColors.current
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = colors.surfaceElevated,
        border = BorderStroke(0.6.dp, colors.goldBorder)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (enabled) colors.goldContainer else colors.surface),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (enabled) HubCloud else HubCloudOff,
                    contentDescription = null,
                    tint = if (enabled) colors.goldPrimary else colors.textMuted,
                    modifier = Modifier.size(20.dp)
                )
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("اتصال به ابر", color = colors.textMain, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text("فاکتورها، حساب‌ها و موجودی با حساب شما همگام می‌شوند.", color = colors.textMuted, style = MaterialTheme.typography.bodySmall)
            }
            Switch(
                checked = enabled,
                onCheckedChange = onChange,
                enabled = interactive,
                modifier = Modifier.semantics { contentDescription = "اتصال به ابر" },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = colors.goldPrimary,
                    uncheckedThumbColor = colors.textMuted,
                    uncheckedTrackColor = colors.surface
                )
            )
        }
    }
}

@Composable
fun CloudAccountSummary(state: SyncUiState) {
    val colors = LocalGoldExColors.current
    val statusColor = when (state.status) {
        SyncStatus.SYNCED -> colors.profitGreen
        SyncStatus.SYNCING -> colors.syncBlue
        SyncStatus.ERROR, SyncStatus.CONFLICT, SyncStatus.LICENSE_REQUIRED -> colors.syncWarning
        else -> colors.goldPrimary
    }

    Surface(shape = RoundedCornerShape(16.dp), color = colors.surfaceElevated, border = BorderStroke(0.6.dp, colors.border)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(statusColor)
                    )
                    Text(
                        text = state.status.title(),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = statusColor
                    )
                }

                if (state.pending > 0) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = colors.goldContainer.copy(alpha = 0.5f),
                        border = BorderStroke(0.5.dp, colors.goldBorder)
                    ) {
                        Text(
                            text = "${PersianNumberFormatter.toPersianDigits(state.pending)} تغییر در صف",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.goldPrimary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
            HorizontalDivider(color = colors.border.copy(alpha = 0.5f))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("شمارهٔ حساب", color = colors.textMuted, style = MaterialTheme.typography.bodySmall)
                Text(PersianNumberFormatter.toPersianDigits(state.phone), color = colors.textMain, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodySmall)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("تغییرات در انتظار", color = colors.textMuted, style = MaterialTheme.typography.bodySmall)
                Text(PersianNumberFormatter.toPersianDigits(state.pending), color = colors.textMain, style = MaterialTheme.typography.bodySmall)
            }
            Text(
                if (state.lastSuccessAt > 0) "آخرین اتصال: ${PersianNumberFormatter.toPersianDigits(SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.US).format(Date(state.lastSuccessAt)))}" else "هنوز همگام‌سازی انجام نشده",
                color = colors.textMuted,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}


@Composable
fun CloudSignInForm(phone: String, code: String, form: CloudFormState, onPhoneChange: (String) -> Unit,
    onCodeChange: (String) -> Unit, onRequest: () -> Unit, onVerify: () -> Unit,
    onChangePhone: () -> Unit, onResend: () -> Unit) {
    val colors=LocalGoldExColors.current
    val challenge=form.challenge
    val keyboard=androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
    var seconds by remember(challenge) { mutableIntStateOf(0) }
    LaunchedEffect(challenge,form.requestedAtMillis) {
        if(challenge!=null) do {
            seconds=((challenge.optInt("retryAfter",60)*1000L-(System.currentTimeMillis()-form.requestedAtMillis)+999)/1000).toInt().coerceAtLeast(0)
            if(seconds>0) delay(1000)
        } while(seconds>0)
    }
    Column(verticalArrangement=Arrangement.spacedBy(12.dp)) {
        Text(if(challenge==null) "ورود به حساب ابری" else "تأیید شماره موبایل",color=colors.textMain,fontWeight=FontWeight.Bold)
        Text(if(challenge==null) "شمارهٔ حساب می‌تواند با شمارهٔ تماس فروشگاه متفاوت باشد." else
            "کد ورود برای ${PersianNumberFormatter.toPersianDigits(form.phone)}",color=colors.textSecondary,style=MaterialTheme.typography.bodySmall)
        if(challenge==null) {
            GoldInputField(value=phone,onValueChange={ onPhoneChange(PersianNumberFormatter.toEnglishDigits(it).take(14)) },
                label="شماره موبایل",keyboardType=KeyboardType.Phone,useThousandsSeparator=false,enabled=!form.busy,modifier=Modifier.fillMaxWidth())
            GoldButton("دریافت کد ورود",onClick={ keyboard?.hide(); onRequest() },isLoading=form.busy,
                enabled=!form.busy && Regex("^(09[0-9]{9}|(?:[+]|00)?989[0-9]{9})$").matches(phone.trim()),modifier=Modifier.fillMaxWidth())
        } else {
            if(challenge.optString("otpMode")=="temporary") Surface(shape=ButtonShape,color=colors.goldContainer) {
                Text("کد ورود موقت: ${PersianNumberFormatter.toPersianDigits(challenge.optString("temporaryCode"))}",
                    modifier=Modifier.fillMaxWidth().padding(12.dp),color=colors.goldPrimary,style=MaterialTheme.typography.bodyMedium)
            }
            GoldInputField(value=code,onValueChange={
                val digits=PersianNumberFormatter.toEnglishDigits(it).filter(Char::isDigit).take(6)
                onCodeChange(digits)
                if(digits.length==6) keyboard?.hide()
            },
                label="کد ورود شش‌رقمی",keyboardType=KeyboardType.NumberPassword,useThousandsSeparator=false,enabled=!form.busy,modifier=Modifier.fillMaxWidth())
            GoldButton("تأیید و اتصال",onClick={ keyboard?.hide(); onVerify() },isLoading=form.busy,enabled=!form.busy && code.length==6,modifier=Modifier.fillMaxWidth())
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
                TextButton(onClick=onChangePhone,enabled=!form.busy) { Text("تغییر شماره",color=colors.textMuted) }
                TextButton(onClick=onResend,enabled=!form.busy && seconds==0) {
                    Text(if(seconds>0) "درخواست مجدد · ${PersianNumberFormatter.toPersianDigits(seconds)} ثانیه" else "دریافت کد جدید",
                        color=if(seconds>0) colors.textMuted else colors.goldPrimary)
                }
            }
        }
    }
}

/** Matches the bottom-anchored settings modals (TaxProfitModal / PriceSourceModal). */
@Composable
fun CloudSettingsModal(onDismiss: () -> Unit, canDismiss: Boolean = true, content: @Composable () -> Unit) {
    val colors=LocalGoldExColors.current
    val context=LocalContext.current
    val reduced=Settings.Global.getFloat(context.contentResolver,Settings.Global.ANIMATOR_DURATION_SCALE,1f)==0f
    var visible by remember { mutableStateOf(false) }
    val scope=rememberCoroutineScope()
    val dismiss: () -> Unit = { if(canDismiss && visible) scope.launch {
        visible=false
        if(!reduced) delay(LuxuryMotion.DURATION_MODAL_EXIT.toLong())
        onDismiss()
    }; Unit }
    LaunchedEffect(Unit) { visible=true }
    val scrim by animateFloatAsState(if(visible) .65f else 0f,tween(if(reduced) 0 else LuxuryMotion.DURATION_MODAL_ENTER),label="cloud_scrim")
    Dialog(onDismissRequest=dismiss,properties=DialogProperties(usePlatformDefaultWidth=false,decorFitsSystemWindows=false)) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            BoxWithConstraints(Modifier.fillMaxSize().background(Color.Black.copy(alpha=scrim)).imePadding()
                .clickable(interactionSource=remember { MutableInteractionSource() },indication=null,onClick=dismiss),contentAlignment=Alignment.BottomCenter) {
                val availableHeight=maxHeight*.88f
                AnimatedVisibility(visible=visible,enter=if(reduced) EnterTransition.None else LuxuryMotion.ModalEnter,
                    exit=if(reduced) ExitTransition.None else LuxuryMotion.ModalExit) {
                    Surface(Modifier.fillMaxWidth().heightIn(max=availableHeight)
                        .clickable(interactionSource=remember { MutableInteractionSource() },indication=null,onClick={}),
                        shape=RoundedCornerShape(topStart=32.dp,topEnd=32.dp),color=colors.surface,
                        border=BorderStroke(1.dp,Brush.verticalGradient(listOf(colors.goldPrimary.copy(alpha=.6f),colors.border.copy(alpha=.3f))))) {
                        Column(Modifier.navigationBarsPadding()) {
                            Box(Modifier.padding(top=12.dp).size(40.dp,4.dp).clip(ButtonShape).background(colors.goldBorder).align(Alignment.CenterHorizontally))
                            Row(Modifier.fillMaxWidth().padding(horizontal=20.dp,vertical=16.dp),verticalAlignment=Alignment.CenterVertically,
                                horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                                Box(Modifier.size(44.dp).clip(ButtonShape).background(colors.goldContainer),contentAlignment=Alignment.Center) {
                                    Icon(HubCloudDownload,null,tint=colors.goldPrimary,modifier=Modifier.size(24.dp))
                                }
                                Column(Modifier.weight(1f)) {
                                    Text("همگام‌سازی ابری",color=colors.textMain,style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold)
                                    Text("حساب و اتصال اطلاعات شما",color=colors.textMuted,style=MaterialTheme.typography.bodySmall)
                                }
                                if(canDismiss) IconButton(onClick=dismiss,modifier=Modifier.size(48.dp)) {
                                    Icon(Icons.Default.Close,"بستن همگام‌سازی ابری",tint=colors.textMuted)
                                }
                            }
                            HorizontalDivider(color=colors.goldBorder.copy(alpha=.4f))
                            Box(Modifier.weight(1f,false)) { content() }
                            if(canDismiss) {
                                HorizontalDivider(color=colors.border)
                                GoldButton("بستن",onClick=dismiss,isSecondary=true,modifier=Modifier.fillMaxWidth().padding(20.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun cloudRecordTitle(type: String)=when(type) {
    "customer"->"طرف‌حساب"; "ledger"->"دفتر حساب"; "invoice"->"فاکتور"; "barterInvoice"->"فاکتور تهاتر"
    "inventory"->"موجودی"; "stockAdjustment"->"گردش موجودی"; "portfolio"->"پرتفوی"; else->"تنظیمات کسب‌وکار"
}
