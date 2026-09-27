package com.goldex.companion.ui.sync

import android.provider.Settings
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import com.goldex.companion.ui.hub.HubCloudDownload
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
    var phone by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(form.phone) }
    var code by androidx.compose.runtime.saveable.rememberSaveable(form.challenge?.optString("challengeId")) { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    Column(modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(20.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
        CloudConnectionToggle(state.enabled, !form.busy && !state.busy, viewModel::setEnabled)
        if(state.enabled) {
            if(state.phone.isNotBlank() && state.status!=SyncStatus.AUTH_REQUIRED) CloudAccountSummary(state)
            if(state.message.isNotBlank()) Text(state.message,color=colors.syncWarning)
            if(state.phone.isBlank() || state.status==SyncStatus.AUTH_REQUIRED) {
                CloudSignInForm(phone, code, form, onPhoneChange={ phone=it }, onCodeChange={ code=it },
                    onRequest={ viewModel.requestCode(phone) }, onVerify={ viewModel.verifyCode(code) },
                    onChangePhone=viewModel::changePhone, onResend={ viewModel.requestCode(form.phone) })
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
        if(state.phone.isNotBlank() || state.readOnly) TextButton(onClick=viewModel::exportBackup,enabled=!form.busy && !state.busy) { Text("ذخیرهٔ پشتیبان محلی و اختلاف‌ها",color=colors.goldPrimary) }
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
    CloudSettingsModal(onDismiss=onDismiss, canDismiss=canDismiss) { CloudSettingsContent(viewModel) }
}

@Composable
fun CloudConnectionToggle(enabled: Boolean, interactive: Boolean, onChange: (Boolean) -> Unit) {
    val colors=LocalGoldExColors.current
    Surface(shape=RoundedCornerShape(16.dp), color=colors.surfaceElevated, border=BorderStroke(0.6.dp,colors.goldBorder)) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment=Alignment.CenterVertically,
            horizontalArrangement=Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(1f), verticalArrangement=Arrangement.spacedBy(4.dp)) {
                Text("اتصال به ابر", color=colors.textMain, fontWeight=FontWeight.SemiBold)
                Text("فاکتورها، حساب‌ها و موجودی با حساب شما همگام می‌شوند.", color=colors.textMuted, style=MaterialTheme.typography.bodySmall)
            }
            Switch(checked=enabled,onCheckedChange=onChange,enabled=interactive,
                modifier=Modifier.semantics { contentDescription="اتصال به ابر" },
                colors=SwitchDefaults.colors(checkedThumbColor=colors.goldPrimary,checkedTrackColor=colors.goldContainer,
                    uncheckedThumbColor=colors.textMuted,uncheckedTrackColor=colors.surface))
        }
    }
}

@Composable
fun CloudAccountSummary(state: SyncUiState) {
    val colors=LocalGoldExColors.current
    Surface(shape=RoundedCornerShape(16.dp),color=colors.surfaceElevated,border=BorderStroke(.6.dp,colors.border)) {
        Column(Modifier.fillMaxWidth().padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
            Text(state.status.title(),fontWeight=FontWeight.SemiBold,
                color=when(state.status) { SyncStatus.SYNCED->colors.profitGreen; SyncStatus.SYNCING->colors.syncBlue;
                    SyncStatus.ERROR,SyncStatus.CONFLICT,SyncStatus.LICENSE_REQUIRED->colors.syncWarning; else->colors.textSecondary })
            HorizontalDivider(color=colors.border)
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
                Text("شمارهٔ حساب",color=colors.textMuted,style=MaterialTheme.typography.bodySmall)
                Text(PersianNumberFormatter.toPersianDigits(state.phone),color=colors.textMain,style=MaterialTheme.typography.bodySmall)
            }
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
                Text("تغییرات در انتظار",color=colors.textMuted,style=MaterialTheme.typography.bodySmall)
                Text(PersianNumberFormatter.toPersianDigits(state.pending),color=colors.textMain,style=MaterialTheme.typography.bodySmall)
            }
            Text(if(state.lastSuccessAt>0) "آخرین اتصال: ${PersianNumberFormatter.toPersianDigits(SimpleDateFormat("yyyy/MM/dd HH:mm",Locale.US).format(Date(state.lastSuccessAt)))}" else "هنوز همگام‌سازی انجام نشده",
                color=colors.textMuted,style=MaterialTheme.typography.bodySmall)
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
