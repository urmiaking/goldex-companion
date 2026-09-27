package com.goldex.companion.ui.sync

import android.provider.Settings
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.Dialog
import com.goldex.companion.data.sync.*
import com.goldex.companion.model.PersianNumberFormatter
import com.goldex.companion.ui.components.GoldButton
import com.goldex.companion.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

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
    val colors=LocalGoldExColors.current
    val context=LocalContext.current
    val reduced=reducedMotion ?: (Settings.Global.getFloat(context.contentResolver,Settings.Global.ANIMATOR_DURATION_SCALE,1f)==0f)
    Surface(onClick=onClick,shape=ButtonShape,color=colors.surface,border=BorderStroke(0.6.dp,colors.goldBorder),
        modifier=Modifier.padding(end=12.dp).size(48.dp).semantics { contentDescription=state.status.title() }) {
        Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center) {
            Canvas(Modifier.size(28.dp)) {
                val w=size.width; val h=size.height
                val path=Path().apply {
                    moveTo(w*.23f,h*.78f); cubicTo(w*-.06f,h*.78f,w*-.03f,h*.38f,w*.23f,h*.37f)
                    cubicTo(w*.25f,h*.04f,w*.72f,h*.02f,w*.78f,h*.36f)
                    cubicTo(w*1.08f,h*.33f,w*1.12f,h*.78f,w*.83f,h*.78f); close()
                }
                drawPath(path,colors.textSecondary,style=Stroke(1.8.dp.toPx()))
                if(state.status==SyncStatus.OFFLINE) drawLine(colors.textMuted,androidx.compose.ui.geometry.Offset(0f,h*.05f),androidx.compose.ui.geometry.Offset(w,h*.94f),2.dp.toPx())
            }
            if(state.status!=SyncStatus.OFFLINE) AnimatedContent(state.status,transitionSpec={
                val animate = !reduced && initialState==SyncStatus.SYNCING && targetState==SyncStatus.SYNCED
                (fadeIn(tween(if(animate) 250 else 0))+scaleIn(animationSpec=tween(if(animate) 250 else 0),initialScale=if(animate) .75f else 1f)).togetherWith(fadeOut(tween(if(animate) 150 else 0)))
            },label="cloud_status",modifier=Modifier.align(Alignment.BottomStart).padding(5.dp)) { status ->
                val tint=when(status) { SyncStatus.SYNCED->colors.profitGreen; SyncStatus.SYNCING->colors.syncBlue; SyncStatus.ERROR,SyncStatus.CONFLICT,SyncStatus.LICENSE_REQUIRED->colors.syncWarning; else->colors.textMuted }
                val icon=when(status) { SyncStatus.SYNCED->Icons.Default.Check; SyncStatus.SYNCING->Icons.Default.Refresh; SyncStatus.AUTH_REQUIRED->Icons.Default.AccountCircle; SyncStatus.LICENSE_REQUIRED,SyncStatus.WRITER_CHANGED->Icons.Default.Lock; SyncStatus.ERROR,SyncStatus.CONFLICT,SyncStatus.RESTORE_REQUIRED->Icons.Default.Warning; else->Icons.Default.Info }
                var rotation by remember { mutableFloatStateOf(0f) }
                if(status==SyncStatus.SYNCING && !reduced) {
                    val transition=rememberInfiniteTransition(label="sync_rotation")
                    val angle by transition.animateFloat(0f,360f,infiniteRepeatable(tween(1100,easing=LinearEasing)),label="sync_angle")
                    rotation=angle
                }
                Surface(color=colors.surface,shape=ButtonShape) { Icon(icon,contentDescription=state.status.title(),tint=tint,modifier=Modifier.size(15.dp).rotate(rotation)) }
            }
        }
    }
}

@Composable
fun CloudSettingsContent(viewModel: CloudSyncViewModel, modifier: Modifier=Modifier) {
    val state by viewModel.state.collectAsState(); val form by viewModel.form.collectAsState()
    val colors=LocalGoldExColors.current
    val context=LocalContext.current
    LaunchedEffect(form.backupPath) { form.backupPath?.let { path ->
        val uri=androidx.core.content.FileProvider.getUriForFile(context,"${context.packageName}.fileprovider",java.io.File(path))
        val intent=android.content.Intent(android.content.Intent.ACTION_SEND).setType("application/json").putExtra(android.content.Intent.EXTRA_STREAM,uri).addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        context.startActivity(android.content.Intent.createChooser(intent,"ذخیرهٔ پشتیبان محلی"))
        viewModel.backupShared()
    } }
    var phone by remember { mutableStateOf(form.phone) }
    var code by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    Column(modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(20.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
        Text("همگام‌سازی ابری",style=MaterialTheme.typography.titleLarge,color=colors.textMain)
        Text("اطلاعات کاری شما شامل فاکتورها، دفتر حساب، موجودی، پرتفوی، مشخصات فروشگاه و لوگو/مهر به حساب شخصی منتقل می‌شود. فقط یک دستگاه امکان ثبت و ویرایش دارد.",color=colors.textSecondary)
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween) {
            Text("اتصال به ابر",color=colors.textMain)
            Switch(checked=state.enabled,onCheckedChange=viewModel::setEnabled,enabled=!form.busy && !state.busy)
        }
        if(state.enabled) {
            Text(state.status.title(),color=if(state.status==SyncStatus.SYNCED) colors.profitGreen else colors.textSecondary)
            if(state.phone.isNotBlank()) Text("حساب: ${PersianNumberFormatter.toPersianDigits(state.phone)}",color=colors.textMuted)
            Text("عملیات در انتظار: ${PersianNumberFormatter.toPersianDigits(state.pending)}",color=colors.textMuted)
            if(state.lastSuccessAt>0) Text("آخرین موفقیت: ${PersianNumberFormatter.toPersianDigits(SimpleDateFormat("yyyy/MM/dd HH:mm",Locale.US).format(Date(state.lastSuccessAt)))}",color=colors.textMuted)
            if(state.message.isNotBlank()) Text(state.message,color=colors.syncWarning)
            if(state.phone.isBlank() || state.status==SyncStatus.AUTH_REQUIRED) {
                Text("شمارهٔ ورود حساب مستقل از شمارهٔ تماس فروشگاه است. سینک به Trial فعال یا لایسنس دائمی نیاز دارد.",color=colors.textSecondary)
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    OutlinedTextField(value=phone,onValueChange={ phone=it },label={ Text("شماره موبایل") },singleLine=true,keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Phone),modifier=Modifier.fillMaxWidth(),enabled=!form.busy)
                    if(form.challenge!=null) OutlinedTextField(value=code,onValueChange={ code=it },label={ Text("کد ورود") },singleLine=true,keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.NumberPassword),modifier=Modifier.fillMaxWidth(),enabled=!form.busy)
                }
                GoldButton(text=if(form.challenge==null) "دریافت کد ورود" else "تأیید کد",onClick={ if(form.challenge==null) viewModel.requestCode(phone) else viewModel.verifyCode(code) },enabled=!form.busy,isLoading=form.busy,modifier=Modifier.fillMaxWidth())
                if(form.challenge!=null) TextButton(onClick={ viewModel.requestCode(form.phone) },enabled=!form.busy) { Text("ارسال دوبارهٔ کد",color=colors.goldPrimary) }
                TextButton(onClick=viewModel::activateTrial,enabled=!form.busy) { Text("فعال‌سازی دورهٔ آزمایشی",color=colors.goldPrimary) }
            } else {
                GoldButton(text="همگام‌سازی اکنون",onClick=viewModel::sync,enabled=!state.busy && !form.busy,modifier=Modifier.fillMaxWidth())
                if(state.status==SyncStatus.CONFLICT) GoldButton(text="مقایسهٔ نسخه‌های کل گروه",onClick=viewModel::reviewConflict,isSecondary=true,enabled=!form.busy)
                if(state.status==SyncStatus.SYNCED) TextButton(onClick=viewModel::listDevices,enabled=!form.busy) { Text("انتقال به دستگاه دیگر",color=colors.goldPrimary) }
                if(state.status in listOf(SyncStatus.RESTORE_REQUIRED,SyncStatus.CONFLICT)) GoldButton(text="بررسی و بازیابی نسخهٔ ابری",onClick={ confirm="restore" },isSecondary=true,enabled=!form.busy)
                if(state.status==SyncStatus.WRITER_CHANGED) TextButton(onClick=viewModel::reauthenticate,enabled=!form.busy) { Text("تأیید تازهٔ شماره برای انتقال",color=colors.goldPrimary) }
                if(state.status==SyncStatus.WRITER_CHANGED) GoldButton(text="انتقال نویسندگی به این دستگاه",onClick={ confirm="takeover" },isSecondary=true,enabled=!form.busy)
                TextButton(onClick={ confirm="logout" },enabled=!form.busy && !state.busy) { Text("خروج از حساب",color=colors.textMuted) }
            }
        }
        TextButton(onClick=viewModel::exportBackup,enabled=!form.busy && !state.busy) { Text("ذخیرهٔ پشتیبان محلی و اختلاف‌ها",color=colors.goldPrimary) }
        if(state.readOnly || state.phone.isNotBlank() || state.status==SyncStatus.WRITER_CHANGED) TextButton(onClick={ confirm="detach" },enabled=!form.busy && !state.busy) { Text("جداسازی داده برای اتصال به حساب دیگر",color=colors.textMuted) }
        if(form.error.isNotBlank()) Text(form.error,color=colors.errorRed)
        Text("خاموش‌کردن سینک یا پایان مجوز، اطلاعات محلی و نسخهٔ ابری را حذف نمی‌کند. سینک جایگزین پشتیبان مستقل نیست.",color=colors.textMuted,style=MaterialTheme.typography.bodySmall)
    }
    form.review?.let { review ->
        Dialog(onDismissRequest=viewModel::closeReview) {
            Surface(shape=ButtonShape,color=colors.surface,modifier=Modifier.heightIn(max=680.dp)) {
                Column(Modifier.padding(20.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                    Text("مقایسهٔ اختلاف‌ها",style=MaterialTheme.typography.titleLarge,color=colors.textMain)
                    Text("انتخاب نسخهٔ محلی برای تمام رکوردهای این گروه اعمال می‌شود. محاسبات مالی دوباره اجرا نمی‌شوند. هر دو نسخه در پشتیبان محفوظ می‌مانند. برای انتخاب نسخهٔ ابری، بازیابی کامل با تأیید جداگانه انجام دهید.",color=colors.textSecondary)
                    val local=review.getJSONArray("local"); val remote=review.getJSONArray("remote")
                    for(i in 0 until local.length()) {
                        val c=local.getJSONObject(i); val r=remote.getJSONObject(i)
                        Text("${cloudRecordTitle(c.getString("type"))} · ${PersianNumberFormatter.toPersianDigits(c.getString("id"))}",color=colors.goldPrimary)
                        val l=c.optJSONObject("localPayload"); val server=r.optJSONObject("payload")
                        val fields=((l?.keys()?.asSequence()?.toList() ?: emptyList())+(server?.keys()?.asSequence()?.toList() ?: emptyList())).distinct()
                        if(l==null) Text("نسخهٔ محلی: حذف رکورد",color=colors.syncWarning)
                        if(r.getBoolean("deleted")) Text("نسخهٔ ابری: حذف رکورد؛ شناسهٔ حذف‌شده قابل بازگردانی نیست",color=colors.syncWarning)
                        for(field in fields) if(SyncJson.canonical(l?.opt(field))!=SyncJson.canonical(server?.opt(field))) {
                            Text(field,color=colors.textMuted,style=MaterialTheme.typography.labelMedium)
                            Text("محلی: ${PersianNumberFormatter.toPersianDigits(l?.opt(field)?.toString() ?: "حذف‌شده")}",color=colors.textMain)
                            Text("ابری: ${PersianNumberFormatter.toPersianDigits(server?.opt(field)?.toString() ?: "حذف‌شده")}",color=colors.textSecondary)
                        }
                    }
                    Row(horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                        GoldButton("بستن",onClick=viewModel::closeReview,isSecondary=true,modifier=Modifier.weight(1f))
                        GoldButton("انتخاب محلی کل گروه",onClick={ confirm="local" },enabled=!form.busy,modifier=Modifier.weight(1f))
                    }
                }
            }
        }
    }
    form.devices?.let { devices -> Dialog(onDismissRequest=viewModel::closeReview) {
        Surface(shape=ButtonShape,color=colors.surface) { Column(Modifier.padding(20.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(12.dp)) {
            Text("دستگاه مقصد باید قبلاً با همین حساب وارد شده باشد. صف این گوشی پیش از انتقال کامل ارسال می‌شود.",color=colors.textMain)
            val list=devices.getJSONArray("devices")
            for(i in 0 until list.length()) { val device=list.getJSONObject(i); if(!device.getBoolean("isWriter")) GoldButton("انتقال به ${device.getString("id").take(8)}",onClick={ confirm="release:${device.getString("id")}" },isSecondary=true,enabled=!form.busy) }
            TextButton(onClick=viewModel::closeReview) { Text("بستن") }
        } }
    } }
    if(confirm.isNotBlank()) Dialog(onDismissRequest={ confirm="" }) {
        Surface(shape=ButtonShape,color=colors.surface) { Column(Modifier.padding(20.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
            Text(when {
                confirm=="detach"->"پس از ساخت پشتیبان، دادهٔ فعلی به‌صورت محلی نگهداری و ارتباط آن با حساب و صف قبلی جدا می‌شود. برای حساب دارای داده، بازیابی با تأیید جداگانه لازم است. پیش از ادامه پشتیبان را در جای امن ذخیره کنید."
                confirm=="local"->"نسخهٔ محلی برای تمام رکوردهای گروه انتخاب شود؟ هر اختلاف مالی این گروه باید با هم بررسی شود. در صورت تغییر دوبارهٔ سرور، ارسال مجدداً متوقف خواهد شد."
                confirm.startsWith("release:")->"پس از ارسال کامل صف، نویسندگی به دستگاه انتخاب‌شده منتقل می‌شود. ویرایش و ارسال این گوشی متوقف می‌شود."
                confirm=="restore"->"دادهٔ فعال این نصب با نسخهٔ ابری جایگزین می‌شود. پیش از جایگزینی، پشتیبان اطلاعات و صف محلی در حافظهٔ برنامه نگهداری می‌شود. در صورت اختلاف مالی، کل مجموعه بررسی شود."
                confirm=="takeover"->"تغییرات ارسال‌نشدهٔ گوشی قبلی در نسخهٔ ابری نیستند. گوشی قبلی حق ارسال را از دست می‌دهد. تأیید تازهٔ موبایل و سپس بازیابی نسخهٔ ابری لازم است."
                else->"سینک خاموش می‌شود و اطلاعات و صف ارسال روی این دستگاه باقی می‌مانند."
            },color=colors.textMain)
            Row(horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                GoldButton("انصراف",onClick={ confirm="" },isSecondary=true,modifier=Modifier.weight(1f))
                GoldButton("تأیید",onClick={ val action=confirm; confirm=""; when { action=="detach"->viewModel.detach(); action=="local"->viewModel.chooseLocal(); action.startsWith("release:")->viewModel.releaseWriter(action.removePrefix("release:")); action=="restore"->viewModel.restore(); action=="takeover"->viewModel.takeover(); else->viewModel.logout() } },modifier=Modifier.weight(1f))
            }
        } }
    }
}


@Composable
fun CloudSettingsDialog(viewModel: CloudSyncViewModel, onDismiss: () -> Unit, canDismiss: Boolean = true) {
    val colors=LocalGoldExColors.current
    Dialog(onDismissRequest={ if(canDismiss) onDismiss() }) {
        Surface(shape=ButtonShape,color=colors.surface,modifier=Modifier.fillMaxWidth().heightIn(max=680.dp)) {
            Column { Box(Modifier.weight(1f,false)) { CloudSettingsContent(viewModel) }; if(canDismiss) TextButton(onClick=onDismiss,modifier=Modifier.align(Alignment.End)) { Text("بستن",color=colors.goldPrimary) } }
        }
    }
}

private fun cloudRecordTitle(type: String)=when(type) {
    "customer"->"طرف‌حساب"; "ledger"->"دفتر حساب"; "invoice"->"فاکتور"; "barterInvoice"->"فاکتور تهاتر"
    "inventory"->"موجودی"; "stockAdjustment"->"گردش موجودی"; "portfolio"->"پرتفوی"; else->"تنظیمات کسب‌وکار"
}
