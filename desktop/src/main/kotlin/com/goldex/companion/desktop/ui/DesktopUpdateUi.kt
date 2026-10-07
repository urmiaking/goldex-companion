package com.goldex.companion.desktop.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.SystemUpdateAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.*
import com.goldex.companion.desktop.update.*
import com.goldex.companion.model.PersianNumberFormatter
import com.goldex.companion.ui.components.*
import com.goldex.companion.ui.theme.LocalGoldExColors

/** Remains visible across pages; neither discovery nor download opens a modal. */
@Composable internal fun WindowsUpdateHeader(updater: WindowsUpdater, canOpenPrompt: Boolean) {
    val state by updater.state.collectAsState()
    val colors = LocalGoldExColors.current
    val active = state.phase in setOf(WindowsUpdatePhase.DOWNLOADING, WindowsUpdatePhase.VERIFYING, WindowsUpdatePhase.RESTARTING)
    if (active) {
        val progress = if (state.total > 0) (state.received.toFloat() / state.total).coerceIn(0f, 1f) else null
        Surface(Modifier.width(176.dp).height(48.dp).testTag("update-download-box").semantics {
            progressBarRangeInfo = if (progress == null) ProgressBarRangeInfo.Indeterminate else ProgressBarRangeInfo(progress, 0f..1f)
        }, shape = RoundedCornerShape(14.dp), color = colors.surface, border = BorderStroke(.6.dp, colors.goldBorder.copy(alpha = .5f))) {
            Box(Modifier.clip(RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
                Canvas(Modifier.fillMaxSize()) {
                    drawRect(colors.goldPrimary.copy(alpha = .16f), size = Size(size.width * (progress ?: 0f), size.height))
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (state.phase != WindowsUpdatePhase.DOWNLOADING || progress == null) CircularProgressIndicator(Modifier.size(16.dp), color = colors.goldPrimary, strokeWidth = 2.dp)
                    Text(when (state.phase) {
                        WindowsUpdatePhase.VERIFYING -> "بررسی فایل…"
                        WindowsUpdatePhase.RESTARTING -> "راه‌اندازی…"
                        else -> "دانلود ${progress?.let { PersianNumberFormatter.toPersianDigits((it * 100).toInt().toString()) + "٪" } ?: "…"}"
                    }, color = colors.textMain, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                }
            }
        }
    } else if (state.release != null && state.phase in setOf(WindowsUpdatePhase.AVAILABLE, WindowsUpdatePhase.READY, WindowsUpdatePhase.FAILED)) {
        GoldButton(when (state.phase) {
            WindowsUpdatePhase.READY -> "نصب نسخه جدید"
            WindowsUpdatePhase.FAILED -> "تلاش دوباره"
            else -> "به‌روزرسانی"
        }, updater::activate, Modifier.width(176.dp).testTag("open-updater"), icon = Icons.Outlined.SystemUpdateAlt,
            enabled = state.phase != WindowsUpdatePhase.READY || canOpenPrompt)
    } else if (state.phase == WindowsUpdatePhase.CHECKING) {
        CircularProgressIndicator(Modifier.size(20.dp).testTag("update-checking"), color = colors.goldPrimary, strokeWidth = 2.dp)
    } else if (state.phase == WindowsUpdatePhase.FAILED) {
        TextButton({ updater.check() }, Modifier.testTag("retry-update-check")) { Text("بررسی دوباره آپدیت", color = colors.textSecondary, fontSize = 12.sp) }
    }
}

@Composable internal fun WindowsUpdateCard(updater: WindowsUpdater, version: String) {
    val state by updater.state.collectAsState()
    LuxuryCard {
        PageTitle("به‌روزرسانی قیراط")
        Text("نسخه ${PersianNumberFormatter.toPersianDigits(version)} • ویندوز", color = LocalGoldExColors.current.textMuted, fontSize = 12.sp)
        UpdateProgress(state)
        Text("هنگام شروع و هر ۵ دقیقه نسخه تازه بررسی می‌شود. دانلود با انتخاب شما در پس‌زمینه انجام می‌شود؛ پس از آماده‌شدن، نصب و راه‌اندازی مجدد را تأیید کنید.", color = LocalGoldExColors.current.textSecondary, fontSize = 13.sp)
        GoldButton(when { state.phase == WindowsUpdatePhase.READY -> "نصب نسخه جدید"; state.release != null -> "دریافت به‌روزرسانی"; else -> "بررسی به‌روزرسانی" },
            updater::activate,
            enabled = !state.busy, isSecondary = state.phase != WindowsUpdatePhase.READY,
            icon = Icons.Outlined.SystemUpdateAlt, modifier = Modifier.testTag("check-update"))
        if (state.phase in setOf(WindowsUpdatePhase.DOWNLOADING, WindowsUpdatePhase.CHECKING, WindowsUpdatePhase.VERIFYING))
            TextButton(onClick = updater::cancelDownload) { Text("توقف دریافت") }
    }
}

@Composable internal fun WindowsUpdatePrompt(updater: WindowsUpdater, canRestart: Boolean, onRestart: () -> Unit) {
    val state by updater.state.collectAsState()
    if (!state.dialog) return
    AlertDialog(onDismissRequest = updater::postpone, modifier = Modifier.width(590.dp).testTag("update-dialog"),
        title = { Text(if (state.phase == WindowsUpdatePhase.READY) "نسخه جدید آماده است" else "به‌روزرسانی قیراط") },
        text = {
            Column(Modifier.heightIn(max = 400.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                UpdateProgress(state)
                if (state.phase in setOf(WindowsUpdatePhase.READY, WindowsUpdatePhase.RESTARTING)) {
                    Text("قیراط بسته می‌شود، نسخه جدید جایگزین می‌شود و برنامه دوباره باز می‌شود. اطلاعات ثبت‌شده شما حفظ می‌شوند.", fontSize = 14.sp)
                    Text("پیش از ادامه، تغییرات ذخیره‌نشده را ذخیره کنید؛ ورودی فعلی ماشین‌حساب پس از راه‌اندازی مجدد پاک می‌شود.", color = LocalGoldExColors.current.goldPrimary, fontSize = 12.sp)
                    if (!canRestart) Text("ذخیره اطلاعات در حال انجام است؛ کمی صبر کنید.", fontSize = 12.sp)
                    state.release?.notes?.takeIf { it.isNotBlank() }?.let { notes ->
                        HorizontalDivider(color = LocalGoldExColors.current.border)
                        Text(notes, fontSize = 12.sp, color = LocalGoldExColors.current.textSecondary)
                    }
                }
            }
        }, confirmButton = {
            Row(Modifier.width(440.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                GoldButton("بعداً", updater::postpone, Modifier.weight(1f).testTag("postpone-update"), isSecondary = true, enabled = state.phase != WindowsUpdatePhase.RESTARTING)
                GoldButton(when (state.phase) { WindowsUpdatePhase.RESTARTING -> "در حال بستن"; WindowsUpdatePhase.READY -> "نصب و راه‌اندازی مجدد"; else -> "بررسی دوباره" },
                    { if (state.phase == WindowsUpdatePhase.READY) onRestart() else updater.check() }, Modifier.weight(1.6f).testTag("restart-update"),
                    enabled = !state.busy && (state.phase != WindowsUpdatePhase.READY || canRestart))
            }
        })
}

@Composable private fun UpdateProgress(state: WindowsUpdateState) {
    val colors = LocalGoldExColors.current
    val caption = when (state.phase) {
        WindowsUpdatePhase.IDLE -> "بررسی خودکار هنگام اجرای برنامه"
        WindowsUpdatePhase.CHECKING -> "در حال بررسی نسخه‌ها…"
        WindowsUpdatePhase.CURRENT -> "آخرین نسخه ویندوز نصب است"
        WindowsUpdatePhase.AVAILABLE -> "نسخه ${PersianNumberFormatter.toPersianDigits(state.release?.version.toString())} برای دریافت آماده است"
        WindowsUpdatePhase.DOWNLOADING -> "دریافت نسخه ${PersianNumberFormatter.toPersianDigits(state.release?.version.toString())}"
        WindowsUpdatePhase.VERIFYING -> "در حال بررسی فایل و آماده‌سازی…"
        WindowsUpdatePhase.READY -> "نسخه ${PersianNumberFormatter.toPersianDigits(state.release?.version.toString())} آماده نصب است"
        WindowsUpdatePhase.RESTARTING -> "در حال آماده‌سازی راه‌اندازی مجدد…"
        WindowsUpdatePhase.FAILED -> "دریافت به‌روزرسانی انجام نشد"
    }
    Text(caption, color = if (state.phase == WindowsUpdatePhase.READY) colors.goldPrimary else colors.textMain, fontSize = 13.sp, modifier = Modifier.testTag("update-status"))
    if (state.phase == WindowsUpdatePhase.DOWNLOADING && state.total > 0) {
        val progress = (state.received.toFloat() / state.total).coerceIn(0f, 1f)
        LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth(), color = colors.goldPrimary, trackColor = colors.surfaceElevated)
        Text("${PersianNumberFormatter.toPersianDigits((progress * 100).toInt().toString())}٪", color = colors.textMuted, fontSize = 12.sp)
    } else if (state.busy) Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
        CircularProgressIndicator(Modifier.size(20.dp), color = colors.goldPrimary, strokeWidth = 2.dp)
    }
    state.error?.let { Text(it, color = colors.errorRed, fontSize = 12.sp) }
}
