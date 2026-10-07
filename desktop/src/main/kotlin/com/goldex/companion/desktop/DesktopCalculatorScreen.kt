package com.goldex.companion.desktop

import com.goldex.companion.desktop.ui.programErrorText

import androidx.compose.foundation.background
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.goldex.companion.model.PersianNumberFormatter
import com.goldex.companion.model.PriceBasisTab
import com.goldex.companion.model.WageType
import com.goldex.companion.presentation.calculator.CalculatorField
import com.goldex.companion.presentation.calculator.ManualGoldCalculator
import com.goldex.companion.presentation.calculator.ManualGoldCalculatorState
import com.goldex.companion.desktop.state.DesktopCalculatorRates
import com.goldex.companion.desktop.state.DesktopCalculatorRateState
import com.goldex.companion.desktop.state.DesktopPortfolioPolicy
import com.goldex.companion.desktop.data.MarketSnapshot
import com.goldex.companion.desktop.data.QuoteKind
import com.goldex.companion.ui.components.AnimatedPriceTicker
import com.goldex.companion.ui.components.GoldButton
import com.goldex.companion.ui.components.GoldOutlinedTextField
import com.goldex.companion.ui.components.LuxuryCard
import com.goldex.companion.ui.components.LuxurySegmentedControl
import com.goldex.companion.ui.theme.*
import com.goldex.companion.ui.util.ThousandsSeparatorVisualTransformation

@Composable
fun DesktopCalculatorScreen(calculator: ManualGoldCalculator, dark: Boolean, showHeader: Boolean = true, marketRates: DesktopCalculatorRates? = null, onThemeChange: () -> Unit) {
    val state by calculator.state.collectAsState()
    val rateState = marketRates?.state?.collectAsState()?.value
    val colors = LocalGoldExColors.current
    val clipboard = LocalClipboardManager.current
    var copied by remember { mutableStateOf(false) }
    LaunchedEffect(state) { copied = false }
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Column(
            Modifier.fillMaxSize().background(colors.background).padding(if (showHeader) 28.dp else 8.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp)
        ) {
            if (showHeader) Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("قیراط", style = MaterialTheme.typography.headlineLarge, color = colors.goldPrimary, fontWeight = FontWeight.Bold)
                    Text("ماشین‌حساب طلا • نسخه ویندوز", color = colors.textSecondary, fontSize = 14.sp)
                }
                GoldButton(
                    text = if (dark) "حالت روز" else "حالت شب",
                    onClick = onThemeChange, isSecondary = true, modifier = Modifier.width(128.dp).testTag("theme")
                )
            }
            BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
                if (maxWidth >= 940.dp) {
                    Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                        ScrollPane(Modifier.weight(1.15f)) {
                            InputPanel(state, calculator, marketRates, rateState)
                        }
                        ScrollPane(Modifier.weight(1f)) {
                            ResultPanel(state)
                            Actions(state, calculator, marketRates, copied) {
                                calculator.summary()?.let { clipboard.setText(AnnotatedString(rateSummary(it, rateState))); copied = true }
                            }
                        }
                    }
                } else {
                    ScrollPane(Modifier.fillMaxSize()) {
                        InputPanel(state, calculator, marketRates, rateState)
                        ResultPanel(state)
                        Actions(state, calculator, marketRates, copied) {
                            calculator.summary()?.let { clipboard.setText(AnnotatedString(rateSummary(it, rateState))); copied = true }
                        }
                    }
                }
            }
            Text(if (rateState?.automatic == true) "محاسبه با نرخ انتخابی بازار • مبالغ به تومان" else "محاسبه با نرخ واردشده شما • مبالغ به تومان", color = colors.textMuted, fontSize = 12.sp)
        }
    }
}

@Composable
private fun ScrollPane(modifier: Modifier, content: @Composable ColumnScope.() -> Unit) {
    val scroll = rememberScrollState()
    Box(modifier.fillMaxHeight()) {
        Column(
            Modifier.fillMaxSize().padding(end = 12.dp).verticalScroll(scroll),
            verticalArrangement = Arrangement.spacedBy(18.dp), content = content
        )
        VerticalScrollbar(rememberScrollbarAdapter(scroll), Modifier.align(Alignment.CenterEnd).fillMaxHeight())
    }
}

@Composable
private fun InputPanel(state: ManualGoldCalculatorState, calculator: ManualGoldCalculator, marketRates: DesktopCalculatorRates?, rateState: DesktopCalculatorRateState?) {
    LuxuryCard {
        PanelTitle("نرخ و مشخصات طلا")
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(if (rateState?.automatic == true) rateState.quote?.label(System.currentTimeMillis()) ?: "در انتظار نرخ بازار" else "نرخ دستی شما",
                Modifier.weight(1f).testTag("calculator-rate-status"), color = LocalGoldExColors.current.goldPrimary, fontSize = 13.sp)
            if (marketRates != null && rateState?.automatic == false) TextButton(marketRates::useMarketRate, Modifier.testTag("calculator-use-market")) { Text("نرخ بازار") }
        }
        if (rateState?.automatic == true) rateState.quote?.let { quote ->
            val converted = when (state.priceBasis) { PriceBasisTab.K18 -> false; PriceBasisTab.K24 -> quote.rates.gold24 <= 0; PriceBasisTab.MESGHAL -> quote.rates.goldMelt <= 0 }
            Text("${quoteSource(quote)} • ${DesktopPortfolioPolicy.observedTime(quote.observedAt)}${if (converted) " • نرخ معادل از ۱۸ عیار" else ""}",
                color = LocalGoldExColors.current.textMuted, fontSize = 11.sp)
        }
        LuxurySegmentedControl(
            items = PriceBasisTab.values().toList(), selectedItem = state.priceBasis,
            onItemSelected = { if (marketRates != null) marketRates.setPriceBasis(it) else calculator.setPriceBasis(it) },
            label = { when (it) { PriceBasisTab.K18 -> "۱۸ عیار"; PriceBasisTab.K24 -> "۲۴ عیار"; PriceBasisTab.MESGHAL -> "مظنه" } },
            modifier = Modifier.fillMaxWidth().testTag("basis"), height = 44.dp, fontSize = 13.sp
        )
        NumericInput(state, CalculatorField.SPOT, "نرخ مبنا", "تومان", calculator, marketRates = marketRates)
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            NumericInput(state, CalculatorField.GROSS_WEIGHT, "وزن کل", "گرم", calculator, Modifier.weight(1f))
            NumericInput(state, CalculatorField.STONE_WEIGHT, "کسر نگین", "گرم", calculator, Modifier.weight(1f))
        }
        NumericInput(state, CalculatorField.KARAT, "عیار", "عیار", calculator)
    }
    LuxuryCard {
        PanelTitle("اجرت، سود و مالیات")
        LuxurySegmentedControl(
            items = WageType.values().toList(), selectedItem = state.wageType,
            onItemSelected = calculator::setWageType,
            label = { if (it == WageType.PERCENTAGE) "اجرت درصدی" else "اجرت هر گرم" },
            modifier = Modifier.fillMaxWidth().testTag("wageType"), height = 44.dp, fontSize = 13.sp
        )
        NumericInput(state, CalculatorField.WAGE, "اجرت", if (state.wageType == WageType.PERCENTAGE) "٪" else "تومان / گرم", calculator)
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            NumericInput(state, CalculatorField.PROFIT, "سود", "٪", calculator, Modifier.weight(1f))
            NumericInput(state, CalculatorField.TAX, "مالیات", "٪", calculator, Modifier.weight(1f))
        }
        Text("مالیات بر اجرت و سود محاسبه می‌شود.", color = LocalGoldExColors.current.textMuted, fontSize = 12.sp)
    }
}

@Composable
private fun NumericInput(
    state: ManualGoldCalculatorState, field: CalculatorField, label: String, unit: String,
    calculator: ManualGoldCalculator, modifier: Modifier = Modifier, marketRates: DesktopCalculatorRates? = null
) {
    val colors = LocalGoldExColors.current
    val error = state.errors[field]
    val integer = field == CalculatorField.SPOT || field == CalculatorField.KARAT
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        GoldOutlinedTextField(
            value = state.input(field),
            onValueChange = { val value = PersianNumberFormatter.toEnglishDigits(it).replace("٬", "").replace("،", "").replace(",", "")
                if (marketRates != null) marketRates.setInput(field, value) else calculator.setInput(field, value) },
            modifier = Modifier.fillMaxWidth().testTag("input-${field.name}"),
            singleLine = true, isError = error != null,
            label = { Text(label, fontSize = 13.sp) },
            trailingIcon = { Text(unit, Modifier.padding(horizontal = 10.dp), color = colors.textMuted, fontSize = 12.sp) },
            textStyle = TextStyle(fontFamily = VazirmatnFamily, fontFeatureSettings = VazirmatnFeatureSettings,
                fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = colors.textMain, textDirection = TextDirection.Ltr),
            visualTransformation = ThousandsSeparatorVisualTransformation(addSeparators = field == CalculatorField.SPOT),
            keyboardOptions = KeyboardOptions(keyboardType = if (integer) KeyboardType.Number else KeyboardType.Decimal),
            shape = ButtonShape,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = colors.goldPrimary, unfocusedBorderColor = colors.border,
                focusedLabelColor = colors.goldPrimary, unfocusedLabelColor = colors.textSecondary,
                focusedContainerColor = colors.surfaceElevated, unfocusedContainerColor = colors.surface,
                errorBorderColor = colors.errorRed, errorLabelColor = colors.errorRed, cursorColor = colors.goldPrimary
            )
        )
        if (error != null) Text(programErrorText(error), color = colors.errorRed, fontSize = 12.sp)
    }
}

@Composable
private fun ResultPanel(state: ManualGoldCalculatorState) {
    val colors = LocalGoldExColors.current
    val result = state.result
    LuxuryCard(backgroundColor = colors.surfaceElevated) {
        PanelTitle("خلاصه محاسبه")
        Text("مبلغ نهایی", color = colors.textSecondary, fontSize = 14.sp)
        AnimatedPriceTicker(
            text = result?.let { PersianNumberFormatter.formatPrice(it.totalPayable) } ?: "—",
            modifier = Modifier.testTag("total"), color = colors.goldPrimary, fontSize = 32.sp, fontWeight = FontWeight.Bold
        )
        Text("تومان", color = colors.textMuted, fontSize = 13.sp)
        if (result == null) Text("برای محاسبه، فیلدهای لازم را کامل کنید.", color = colors.textMuted, fontSize = 13.sp)
        HorizontalDivider(color = colors.goldBorder)
        ResultLine("وزن خالص", result?.let { PersianNumberFormatter.formatWeight(it.netWeight) }, "گرم")
        ResultLine("ارزش خام طلا", result?.let { PersianNumberFormatter.formatPrice(it.rawGoldValue) }, "تومان")
        ResultLine("اجرت ساخت", result?.let { PersianNumberFormatter.formatPrice(it.wageAmount) }, "تومان")
        ResultLine("سود فروشنده", result?.let { PersianNumberFormatter.formatPrice(it.profitAmount) }, "تومان")
        ResultLine("مالیات", result?.let { PersianNumberFormatter.formatPrice(it.taxAmount) }, "تومان")
        HorizontalDivider(color = colors.goldBorder)
        ResultLine("قیمت هر گرم", result?.let { PersianNumberFormatter.formatPrice(it.effectiveGramPrice) }, "تومان")
    }
}

@Composable
private fun ResultLine(label: String, value: String?, unit: String) {
    val colors = LocalGoldExColors.current
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), color = colors.textSecondary, fontSize = 13.sp, maxLines = 1)
        AnimatedPriceTicker(text = value ?: "—", color = colors.textMain, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        Text(unit, color = colors.textMuted, fontSize = 11.sp)
    }
}

@Composable
private fun Actions(state: ManualGoldCalculatorState, calculator: ManualGoldCalculator, marketRates: DesktopCalculatorRates?, copied: Boolean, onCopy: () -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        GoldButton("پاک کردن", { if (marketRates != null) marketRates.reset() else calculator.reset() }, Modifier.weight(1f).testTag("reset"), isSecondary = true)
        GoldButton(if (copied) "کپی شد" else "کپی خلاصه", onCopy, Modifier.weight(1f).testTag("copy"), enabled = state.result != null)
    }
}

private fun rateSummary(summary: String, rate: DesktopCalculatorRateState?): String =
    if (rate?.automatic == true && rate.quote != null) summary.replace("محاسبه با نرخ دستی",
        "محاسبه با ${rate.quote.label(System.currentTimeMillis())} • ${quoteSource(rate.quote)} • ${DesktopPortfolioPolicy.observedTime(rate.quote.observedAt)}") else summary

private fun quoteSource(quote: MarketSnapshot) = if (quote.kind == QuoteKind.MANUAL) "ثبت‌شده توسط شما" else quote.rates.source.labelFa

@Composable
private fun PanelTitle(title: String) {
    Text(title, color = LocalGoldExColors.current.textMain, fontWeight = FontWeight.Bold, fontSize = 17.sp)
}
