package com.goldex.companion.ui.calculator.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.goldex.companion.data.MarketRates
import com.goldex.companion.domain.calculator.KaratConversionPolicy
import com.goldex.companion.model.Karat
import com.goldex.companion.model.PersianNumberFormatter
import com.goldex.companion.ui.calculator.CalcContentCopy
import com.goldex.companion.ui.calculator.CalcSwapHoriz
import com.goldex.companion.ui.calculator.KaratConvertMode
import com.goldex.companion.ui.calculator.KaratConvertUiState
import com.goldex.companion.ui.components.*
import com.goldex.companion.ui.theme.LocalGoldExColors
import com.goldex.companion.ui.theme.heroCardGradient

@Composable
fun KaratConvertScreen(
    uiState: KaratConvertUiState,
    rates: MarketRates,
    onBack: () -> Unit,
    onConvertWeightChanged: (String) -> Unit,
    onConvertFromKarat: (Karat) -> Unit,
    onConvertToKarat: (Karat) -> Unit,
    onCustomFromKaratChanged: (String) -> Unit,
    onCustomToKaratChanged: (String) -> Unit,
    onSwapConvertKarats: () -> Unit,
    onSetConvertMode: (KaratConvertMode) -> Unit,
    onAssayKaratChanged: (String) -> Unit,
    onAgreedKaratChanged: (String) -> Unit,
    onTransferToInvoice: () -> Unit,
    modifier: Modifier = Modifier,
    onSelectCustomFromKarat: () -> Unit = {},
    onSelectCustomToKarat: () -> Unit = {}
) {
    val colors = LocalGoldExColors.current
    val context = LocalContext.current
    val direct = uiState.convertMode == KaratConvertMode.DIRECT
    val cash = uiState.assayCashTomans
    val valid = uiState.invoiceGold750 != null
    val resultText = "وزن مبدا: ${PersianNumberFormatter.toPersianDigits(uiState.convertWeightInput)} گرم\n" + if (direct) {
        "عیار مبدا: ${PersianNumberFormatter.toPersianDigits(uiState.fromInput)} • مقصد: ${PersianNumberFormatter.toPersianDigits(uiState.toInput)}\n" +
        "وزن در عیار مقصد: ${weightText(uiState.conversion?.targetWeight)} گرم\n" +
            "معادل ۷۵۰: ${weightText(uiState.conversion?.gold750Weight)} گرم\n" +
            "طلای خالص: ${weightText(uiState.conversion?.pureGoldWeight)} گرم"
    } else {
        "عیار شرط: ${PersianNumberFormatter.toPersianDigits(uiState.agreedKaratInput)} • آزمایشگاه: ${PersianNumberFormatter.toPersianDigits(uiState.assayKaratInput)}\n" +
        "وزن تعدیل‌شده در عیار شرط: ${weightText(uiState.assay?.adjustedWeight)} گرم\n" +
            "اختلاف طلای ۷۵۰: ${uiState.assay?.let { PersianNumberFormatter.formatSignedWeight(it.gold750Difference) } ?: "—"} گرم\n" +
            "ارزش اختلاف: ${cash?.let { signedTomans(it) } ?: "—"} تومان"
    }
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Scaffold(
            modifier = modifier.fillMaxSize(),
            containerColor = colors.background,
            topBar = {
                Row(
                    Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, "بازگشت", tint = colors.goldPrimary)
                    }
                    Text("تبدیل عیار و محاسبه شرطی", color = colors.textMain, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        ) { padding ->
            Column(
                Modifier.fillMaxSize().padding(padding).imePadding().verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp).padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                LuxurySegmentedControl(
                    items = KaratConvertMode.values().toList(), selectedItem = uiState.convertMode,
                    onItemSelected = onSetConvertMode, label = { it.labelFa }, modifier = Modifier.fillMaxWidth(), height = 48.dp, fontSize = 14.sp
                )
                ConversionCard {
                    GoldInputField(
                        value = uiState.convertWeightInput, onValueChange = onConvertWeightChanged,
                        label = "وزن طلا", trailingText = "گرم", isDecimal = true,
                        modifier = Modifier.fillMaxWidth().testTag("convert-weight")
                    )
                    if (direct) {
                        KaratChoice("عیار مبدا", uiState.convertFromKarat, uiState.customFromKarat, onConvertFromKarat, onCustomFromKaratChanged, onSelectCustomFromKarat, "from")
                        GoldButton("جابه‌جایی مبدا و مقصد", onSwapConvertKarats, Modifier.fillMaxWidth(), icon = CalcSwapHoriz, isSecondary = true)
                        KaratChoice("عیار مقصد", uiState.convertToKarat, uiState.customToKarat, onConvertToKarat, onCustomToKaratChanged, onSelectCustomToKarat, "to")
                    } else {
                        GoldInputField(
                            value = uiState.agreedKaratInput, onValueChange = onAgreedKaratChanged, label = "عیار شرط",
                            useThousandsSeparator = false, modifier = Modifier.fillMaxWidth().testTag("assay-agreed"),
                            isError = invalidKarat(uiState.agreedKaratInput), errorMessage = "عیار باید بین ۱ تا ۱۰۰۰ باشد"
                        )
                        GoldInputField(
                            value = uiState.assayKaratInput, onValueChange = onAssayKaratChanged, label = "عیار آزمایشگاه",
                            useThousandsSeparator = false, modifier = Modifier.fillMaxWidth().testTag("assay-actual"),
                            isError = invalidKarat(uiState.assayKaratInput), errorMessage = "عیار باید بین ۱ تا ۱۰۰۰ باشد"
                        )
                    }
                    Text("۱ تا ۲۴: عیار معمول • ۲۵ تا ۱۰۰۰: عیار در هزار", color = colors.textSecondary, fontSize = 12.sp)
                }
                Surface(
                    shape = RoundedCornerShape(16.dp), color = Color.Transparent,
                    border = BorderStroke(0.6.dp, colors.goldBorder), modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.background(colors.heroCardGradient).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(if (direct) "نتیجه تبدیل" else "نتیجه ری‌گیری", color = colors.goldSecondary, fontWeight = FontWeight.Bold)
                        if (!valid) {
                            Text("وزن و عیار معتبر را وارد کنید", color = Color.White, modifier = Modifier.testTag("convert-empty"))
                        } else if (direct) {
                            ResultValue("وزن در عیار مقصد", weightText(uiState.conversion?.targetWeight), "گرم")
                            ResultValue("معادل استاندارد ۷۵۰", weightText(uiState.conversion?.gold750Weight), "گرم")
                            ResultValue("طلای خالص ۱۰۰۰", weightText(uiState.conversion?.pureGoldWeight), "گرم")
                            Text("وزن × عیار مبدا ÷ عیار مقصد", color = colors.goldSecondary, fontSize = 12.sp)
                        } else {
                            val assay = uiState.assay!!
                            ResultValue("وزن تعدیل‌شده در عیار شرط", weightText(assay.adjustedWeight), "گرم")
                            ResultValue("اختلاف طلای ۷۵۰", PersianNumberFormatter.formatSignedWeight(assay.gold750Difference), "گرم")
                            ResultValue("ارزش اختلاف", cash?.let { signedTomans(it) } ?: "—", "تومان")
                            Text(
                                when {
                                    assay.gold750Difference > 0 -> "به نفع صاحب طلا (بستانکار)"
                                    assay.gold750Difference < 0 -> "به عهده صاحب طلا (بدهکار)"
                                    else -> "بدون اختلاف عیار"
                                }, color = colors.goldSecondary, fontSize = 13.sp
                            )
                            Text("اختلاف طلای ۷۵۰ = وزن × (عیار آزمایشگاه − عیار شرط) ÷ ۷۵۰", color = Color.White, fontSize = 12.sp)
                            Text(
                                if (rates.gold18 > 0L) "نرخ مبنا: ${PersianNumberFormatter.formatPrice(rates.gold18.toDouble())} تومان برای هر گرم ۷۵۰"
                                else "نرخ طلا موجود نیست؛ ارزش تومانی محاسبه نمی‌شود",
                                color = Color.White, fontSize = 12.sp
                            )
                        }
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GoldButton("کپی", {
                        (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("تبدیل عیار", resultText))
                    }, Modifier.weight(1f), icon = CalcContentCopy, isSecondary = true, enabled = valid)
                    GoldButton("اشتراک", {
                        context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, resultText) }, "اشتراک نتیجه"))
                    }, Modifier.weight(1f), icon = Icons.Outlined.Share, isSecondary = true, enabled = valid)
                }
                GoldButton("افزودن طلای ۷۵۰ به فاکتور", onTransferToInvoice, Modifier.fillMaxWidth().testTag("convert-transfer"), icon = Icons.Outlined.Add, enabled = valid)
                Text("وزن معادل ۷۵۰ به فاکتور افزوده می‌شود؛ اختلاف ری‌گیری پرداخت ثبت نمی‌کند.", color = colors.textSecondary, fontSize = 12.sp)
            }
        }
    }
}

private fun signedTomans(value: Long): String = PersianNumberFormatter.toPersianDigits(com.goldex.companion.platform.DecimalFormatting.groupedInteger(value).removePrefix("-")) + if (value < 0) "-" else if (value > 0) "+" else ""

private fun invalidKarat(input: String): Boolean = input.isNotEmpty() && input.toIntOrNull()?.let(KaratConversionPolicy::fineness) == null
private fun weightText(weight: Double?): String = weight?.let(PersianNumberFormatter::formatWeight) ?: "—"

@Composable
private fun ConversionCard(content: @Composable ColumnScope.() -> Unit) {
    val colors = LocalGoldExColors.current
    Surface(shape = RoundedCornerShape(16.dp), color = colors.surface, border = BorderStroke(0.6.dp, colors.goldBorder.copy(alpha = 0.5f)), shadowElevation = if (colors.isDark) 0.dp else 2.dp) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
    }
}

@Composable
private fun KaratChoice(title: String, selected: Karat, custom: String?, onSelect: (Karat) -> Unit, onCustom: (String) -> Unit, onSelectCustom: () -> Unit, tag: String) {
    val colors = LocalGoldExColors.current
    Text(title, color = colors.textMain, fontWeight = FontWeight.Medium, fontSize = 14.sp)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Karat.values().forEach { karat ->
            val active = custom == null && selected == karat
            Surface(
                onClick = { onSelect(karat) }, shape = RoundedCornerShape(10.dp),
                color = if (active) colors.goldContainer else colors.surfaceElevated,
                border = BorderStroke(0.6.dp, if (active) colors.goldPrimary else colors.border),
                modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("$tag-${karat.karatNumber}")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(PersianNumberFormatter.toPersianDigits(karat.karatNumber.toString()), color = if (active) colors.goldPrimary else colors.textMain, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        InlineCustomKaratChip(value = custom ?: "", onValueChange = onCustom, isSelected = custom != null,
            onSelect = onSelectCustom, modifier = Modifier.weight(1.3f).testTag("$tag-custom"), height = 48.dp, fontSize = 14.sp, accessibilityLabel = "$title سفارشی")
    }
    if (custom != null && invalidKarat(custom)) Text("عیار باید بین ۱ تا ۱۰۰۰ باشد", color = colors.errorRed, fontSize = 12.sp)
}

@Composable
private fun ResultValue(label: String, value: String, unit: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, color = Color.White, fontSize = 13.sp)
        AnimatedPriceTicker(text = "$value $unit", color = LocalGoldExColors.current.goldSecondary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
    }
}
