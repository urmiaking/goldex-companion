package com.goldex.companion.desktop.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.testTag
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

@Composable internal fun SettingsPage(state: WorkspaceState, workspace: DesktopWorkspace, onBackup: () -> Unit, updater: WindowsUpdater? = null, version: String = "") {
    val draft = state.settingsDraft ?: state.settings
    val colors = LocalGoldExColors.current
    val dirty = draft != state.settings
    PageScroll {
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val profile: @Composable () -> Unit = {
                LuxuryCard {
                    PageTitle("مشخصات گالری")
                    DesktopField(draft.galleryName, { value -> workspace.editSettings { it.copy(galleryName = value.take(120)) } }, "نام گالری", Modifier.testTag("settings-gallery"))
                    DesktopField(draft.managerName, { value -> workspace.editSettings { it.copy(managerName = value.take(80)) } }, "نام زرگر")
                    DesktopField(draft.galleryPhone, { value -> workspace.editSettings { it.copy(galleryPhone = value.take(24)) } }, "شماره تماس", Modifier.testTag("settings-phone"), numeric = true, keyboardType = androidx.compose.ui.text.input.KeyboardType.Phone)
                    DesktopField(draft.unionCode, { value -> workspace.editSettings { it.copy(unionCode = value.take(40)) } }, "کد اتحادیه", Modifier.testTag("settings-union"), numeric = true, keyboardType = androidx.compose.ui.text.input.KeyboardType.Ascii)
                    DesktopField(draft.galleryLicense, { value -> workspace.editSettings { it.copy(galleryLicense = value.take(40)) } }, "پروانه کسب", Modifier.testTag("settings-license"), numeric = true, keyboardType = androidx.compose.ui.text.input.KeyboardType.Ascii)
                    DesktopField(draft.galleryAddress, { value -> workspace.editSettings { it.copy(galleryAddress = value.take(300)) } }, "نشانی گالری", Modifier.testTag("settings-address"), singleLine = false)
                }
            }
            val preferences: @Composable () -> Unit = {
                Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    LuxuryCard {
                        PageTitle("پیش‌فرض‌های محاسبه")
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            DesktopField(draft.defaultProfitPercent, { value -> workspace.editSettings { it.copy(defaultProfitPercent = value) } }, "سود (٪)", Modifier.weight(1f).testTag("settings-profit"), numeric = true)
                            DesktopField(draft.defaultTaxPercent, { value -> workspace.editSettings { it.copy(defaultTaxPercent = value) } }, "مالیات (٪)", Modifier.weight(1f).testTag("settings-tax"), numeric = true)
                        }
                        ChoiceField("نوع اجرت", draft.defaultWageType, WageType.values().toList(), { if (it == WageType.PERCENTAGE) "درصدی" else "تومان در هر گرم" }) { value -> workspace.editSettings { it.copy(defaultWageType = value) } }
                        Text("محاسبه در حال انجام تغییر نمی‌کند. با پاک‌کردن فرم، پیش‌فرض‌های ذخیره‌شده اعمال می‌شوند.", color = colors.textMuted, fontSize = 11.sp)
                    }
                    LuxuryCard {
                        PageTitle("دریافت نرخ‌ها")
                        ChoiceField("منبع ترجیحی", draft.priceSource, PriceSource.values().toList(), { it.labelFa }) { value -> workspace.editSettings { it.copy(priceSource = value) } }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("دریافت خودکار", Modifier.weight(1f), color = colors.textSecondary, fontSize = 13.sp)
                            Switch(draft.autoSyncRates, { value -> workspace.editSettings { it.copy(autoSyncRates = value) } }, colors = SwitchDefaults.colors(checkedTrackColor = colors.goldPrimary))
                        }
                        Text("نرخ دستی تا انتخاب «دریافت آنلاین» حفظ می‌شود.", color = colors.textMuted, fontSize = 11.sp)
                    }
                }
            }
            if (maxWidth >= 850.dp) Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                Box(Modifier.weight(1f)) { profile() }; Box(Modifier.weight(1f)) { preferences() }
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
        LuxuryCard {
            PageTitle("پشتیبان اطلاعات")
            Text("کالاها، گردش موجودی، تنظیمات و آخرین نرخ‌ها روی همین رایانه نگهداری می‌شوند. یک نسخه پشتیبان در محل دلخواه ذخیره کنید و آن را در جای مطمئن نگه دارید.", color = colors.textSecondary, fontSize = 13.sp)
            Text("اطلاعات گوشی و حساب ابری به‌صورت خودکار به این نسخه منتقل نمی‌شود.", color = colors.textMuted, fontSize = 11.sp)
            GoldButton("ذخیره فایل پشتیبان", onBackup, isSecondary = true, enabled = !state.saving, icon = Icons.Outlined.FileDownload)
        }
        if (updater != null) WindowsUpdateCard(updater, version)
        LuxuryCard {
            PageTitle("حرکت رابط")
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("کاهش حرکت صفحات و نمودار", Modifier.weight(1f), color = colors.textSecondary, fontSize = 13.sp)
                Switch(state.reduceMotion, { workspace.setReduceMotion(it) }, enabled = !state.saving,
                    modifier = Modifier.testTag("reduce-motion"), colors = SwitchDefaults.colors(checkedTrackColor = colors.goldPrimary))
            }
        }
    }
}
