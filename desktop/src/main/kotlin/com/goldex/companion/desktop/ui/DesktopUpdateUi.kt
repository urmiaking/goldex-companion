package com.goldex.companion.desktop.ui

import androidx.compose.foundation.layout.*
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

@Composable internal fun WindowsUpdateCard(updater: WindowsUpdater, version: String) {
    val state by updater.state.collectAsState()
    LuxuryCard {
        PageTitle("به‌روزرسانی قیراط")
        Text("نسخهٔ ${PersianNumberFormatter.toPersianDigits(version)} • ویندوز", color = LocalGoldExColors.current.textMuted, fontSize = 12.sp)
        UpdateProgress(state)
        Text("نسخهٔ تازه خودکار دانلود می‌شود. نصب و راه‌اندازی مجدد با تأیید شما انجام می‌شود.", color = LocalGoldExColors.current.textSecondary, fontSize = 13.sp)
        GoldButton(if (state.phase == WindowsUpdatePhase.READY) "نصب نسخهٔ جدید" else "بررسی به‌روزرسانی",
            { if (state.phase == WindowsUpdatePhase.READY) updater.showDialog() else updater.check() },
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
        title = { Text(if (state.phase == WindowsUpdatePhase.READY) "نسخهٔ جدید آماده است" else "به‌روزرسانی قیراط") },
        text = {
            Column(Modifier.heightIn(max = 400.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                UpdateProgress(state)
                if (state.phase in setOf(WindowsUpdatePhase.READY, WindowsUpdatePhase.RESTARTING)) {
                    Text("قیراط بسته می‌شود، نسخهٔ جدید جایگزین می‌شود و برنامه دوباره باز می‌شود. اطلاعات ثبت‌شدهٔ شما حفظ می‌شوند.", fontSize = 14.sp)
                    Text("پیش از ادامه، تغییرات ذخیره‌نشده را ذخیره کنید؛ ورودی فعلی ماشین‌حساب پس از راه‌اندازی مجدد پاک می‌شود.", color = LocalGoldExColors.current.goldPrimary, fontSize = 12.sp)
                    if (!canRestart) Text("ذخیرهٔ اطلاعات در حال انجام است؛ کمی صبر کنید.", fontSize = 12.sp)
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
        WindowsUpdatePhase.CURRENT -> "آخرین نسخهٔ ویندوز نصب است"
        WindowsUpdatePhase.DOWNLOADING -> "دریافت نسخهٔ ${PersianNumberFormatter.toPersianDigits(state.release?.version.toString())}"
        WindowsUpdatePhase.VERIFYING -> "در حال بررسی فایل و آماده‌سازی…"
        WindowsUpdatePhase.READY -> "نسخهٔ ${PersianNumberFormatter.toPersianDigits(state.release?.version.toString())} آمادهٔ نصب است"
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
