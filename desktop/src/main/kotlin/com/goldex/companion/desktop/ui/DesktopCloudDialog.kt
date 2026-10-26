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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.goldex.companion.desktop.state.DesktopWorkspace
import com.goldex.companion.desktop.sync.*
import com.goldex.companion.desktop.ui.controls.*
import com.goldex.companion.model.PersianNumberFormatter
import com.goldex.companion.ui.theme.*
import kotlinx.coroutines.launch

/** Presentation only: authentication, session storage and synchronization stay in the coordinator. */
@Composable internal fun DesktopCloudDialog(workspace: DesktopWorkspace, onDismiss: () -> Unit) {
    val state by workspace.cloud.state.collectAsState()
    val scope = rememberCoroutineScope()
    var login by remember { mutableStateOf(false) }
    var signingOut by remember { mutableStateOf(false) }
    var confirmRestore by remember { mutableStateOf(false) }
    if (login) {
        DesktopOtpDialog(workspace) { login = false }
    } else {
        CloudAccountDialog(state.copy(busy = state.busy || signingOut), onDismiss,
            onLogin = { login = true }, onEnabledChange = workspace.cloud::setEnabled,
            onSync = { scope.launch { workspace.cloud.syncNow() } },
            onLogout = {
                signingOut = true
                scope.launch { try { workspace.cloud.logout() } finally { signingOut = false } }
            }, onRestore = { confirmRestore = true })
    }
    DesktopDialogHost(confirmRestore) {
        DesktopAlertDialog(onDismissRequest = { confirmRestore = false },
            title = { Text("بازیابی حساب ابری") },
            text = { Text("اطلاعات حساب ابری جایگزین اطلاعات این دستگاه می‌شود. پیش از جایگزینی، پشتیبان محلی مستقل ذخیره می‌شود. ادامه می‌دهید؟") },
            confirmButton = {
                Row(Modifier.width(360.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    DesktopActionButton("انصراف", { confirmRestore = false }, Modifier.weight(1f), Icons.Outlined.Close, isSecondary = true)
                    DesktopActionButton("تأیید و بازیابی", {
                        confirmRestore = false
                        scope.launch { workspace.cloud.restore(true) }
                    }, Modifier.weight(1.5f).testTag("cloud-confirm-restore"), Icons.Outlined.CloudDownload)
                }
            })
    }
}

@Composable internal fun CloudAccountDialog(
    state: DesktopSyncUiState, onDismiss: () -> Unit, onLogin: () -> Unit,
    onEnabledChange: (Boolean) -> Unit, onSync: () -> Unit, onLogout: () -> Unit,
    onRestore: () -> Unit = {}
) {
    val colors = LocalGoldExColors.current
    val needsLogin = state.phone.isBlank() || state.status == DesktopSyncStatus.AUTH_REQUIRED
    val statusColor = when (state.status) {
        DesktopSyncStatus.SYNCED -> colors.marketGainText
        DesktopSyncStatus.SYNCING -> colors.syncBlue
        DesktopSyncStatus.PENDING, DesktopSyncStatus.AUTH_REQUIRED, DesktopSyncStatus.RESTORE_REQUIRED -> colors.goldPrimary
        DesktopSyncStatus.ERROR -> colors.errorRed
        DesktopSyncStatus.OFFLINE, DesktopSyncStatus.DISABLED -> colors.textMuted
    }
    DesktopDialog({ if (!state.busy) onDismiss() }, DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.widthIn(max = 600.dp).fillMaxWidth().heightIn(max = 620.dp).testTag("cloud-account-dialog"),
            shape = RoundedCornerShape(24.dp), color = colors.surface, border = colors.goldHairlineBorder) {
            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Surface(shape = DesktopControls.BodyShape, color = colors.goldContainer) {
                        Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                            Icon(Icons.Outlined.CloudSync, null, Modifier.size(26.dp), tint = colors.goldPrimary)
                        }
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("فضای ابری قیراط", color = colors.textMain, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text("همگام‌سازی حساب‌ها میان ویندوز و اندروید", color = colors.textMuted, fontSize = 12.sp)
                    }
                    DesktopIconButton(onDismiss, Modifier.testTag("cloud-dialog-close"), enabled = !state.busy) {
                        Icon(Icons.Outlined.Close, "بستن فضای ابری", tint = colors.textMuted)
                    }
                }
                HorizontalDivider(color = colors.border)
                Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Surface(shape = RoundedCornerShape(16.dp), color = colors.surfaceElevated, border = colors.hairlineBorder) {
                        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(if (state.status == DesktopSyncStatus.SYNCED) Icons.Outlined.CloudDone else Icons.Outlined.CloudSync,
                                    null, Modifier.size(22.dp), tint = statusColor)
                                Text(state.status.titleFa, Modifier.testTag("cloud-dialog-status"), color = statusColor,
                                    fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                if (state.busy) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = statusColor)
                            }
                            Text(if (state.phone.isBlank()) "برای اتصال، وارد حساب ابری خود شوید."
                                else "حساب متصل: ${PersianNumberFormatter.toPersianDigits(state.phone)}",
                                color = colors.textMain, fontSize = 13.sp)
                            Text("اسناد در انتظار: ${PersianNumberFormatter.toPersianDigits(state.pendingCount.toString())}",
                                Modifier.testTag("cloud-dialog-pending"), color = colors.textSecondary, fontSize = 13.sp)
                            Text(if (state.lastSuccessAt > 0) "آخرین همگام‌سازی: ${workspaceDate(state.lastSuccessAt)}"
                                else "هنوز همگام‌سازی موفقی ثبت نشده است.", color = colors.textMuted, fontSize = 12.sp)
                        }
                    }
                    if (!needsLogin) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("همگام‌سازی ابری", color = colors.textMain, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                Text("ارسال و دریافت فاکتورها، مشتریان و اسناد", color = colors.textMuted, fontSize = 12.sp)
                            }
                            Switch(state.enabled, onEnabledChange, Modifier.testTag("cloud-dialog-enabled"),
                                enabled = !state.busy && LocalDesktopDialogInputEnabled.current)
                        }
                    } else {
                        Text("با همان شمارهٔ حساب اندروید وارد شوید تا اطلاعات دو نسخه در یک حساب قرار بگیرد.",
                            color = colors.textSecondary, fontSize = 13.sp)
                    }
                    if (state.message.isNotBlank()) Text(state.message, Modifier.testTag("cloud-dialog-message"),
                        color = if (state.status == DesktopSyncStatus.ERROR) colors.errorRed else colors.textSecondary, fontSize = 13.sp)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (needsLogin) {
                        DesktopActionButton("بستن", onDismiss, Modifier.weight(1f), Icons.Outlined.Close,
                            isSecondary = true, enabled = !state.busy)
                        DesktopActionButton("ورود به حساب ابری", onLogin, Modifier.weight(1.5f).testTag("cloud-dialog-login"),
                            Icons.Outlined.Login, enabled = !state.busy)
                    } else {
                        DesktopActionButton("خروج از حساب", onLogout, Modifier.weight(1f).testTag("cloud-dialog-logout"),
                            Icons.Outlined.Logout, isSecondary = true, enabled = !state.busy)
                        val needsRestore = state.status == DesktopSyncStatus.RESTORE_REQUIRED
                        DesktopActionButton(if (needsRestore) "بازیابی حساب" else "همگام‌سازی", if (needsRestore) onRestore else onSync,
                            Modifier.weight(1f).testTag(if (needsRestore) "cloud-dialog-restore" else "cloud-dialog-sync"),
                            if (needsRestore) Icons.Outlined.CloudDownload else Icons.Outlined.Sync, enabled = state.enabled && !state.busy, isLoading = state.busy)
                    }
                }
            }
        }
    }
}
