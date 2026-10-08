package com.goldex.companion.desktop

import com.goldex.companion.desktop.ui.programErrorText

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.goldex.companion.model.PersianNumberFormatter
import com.goldex.companion.model.PersianWordsFormatter
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
import com.goldex.companion.ui.components.LuxurySegmentedControl
import com.goldex.companion.ui.theme.*
import com.goldex.companion.ui.util.ThousandsSeparatorVisualTransformation

@Composable
fun DesktopCalculatorScreen(
    calculator: ManualGoldCalculator,
    dark: Boolean,
    showHeader: Boolean = true,
    marketRates: DesktopCalculatorRates? = null,
    onNavigateToInvoice: () -> Unit = {},
    onThemeChange: () -> Unit = {}
) {
    val state by calculator.state.collectAsState()
    val rateState = marketRates?.state?.collectAsState()?.value
    val colors = LocalGoldExColors.current
    val clipboard = LocalClipboardManager.current
    var copied by remember { mutableStateOf(false) }
    LaunchedEffect(state) { copied = false }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Column(
            Modifier.fillMaxSize().background(colors.background).padding(if (showHeader) 28.dp else 4.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            if (showHeader) {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            "ماشین‌حساب تخصصی طلا",
                            style = MaterialTheme.typography.headlineLarge,
                            color = colors.textMain,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "محاسبه دقیق طلا بر اساس آخرین مظنه و نرخ لحظه‌ای بازار",
                            color = colors.textMuted,
                            fontSize = 13.5.sp
                        )
                    }
                    GoldButton(
                        text = if (dark) "حالت روز" else "حالت شب",
                        onClick = onThemeChange,
                        isSecondary = true,
                        modifier = Modifier.width(128.dp).testTag("theme")
                    )
                }
            }

            BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
                if (maxWidth >= 940.dp) {
                    Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(22.dp)) {
                        ScrollPane(Modifier.weight(1.3f)) {
                            InputPanel(state, calculator, marketRates, rateState)
                        }
                        ScrollPane(Modifier.weight(1f)) {
                            ResultPanel(
                                state = state,
                                copied = copied,
                                onCopy = {
                                    calculator.summary()?.let {
                                        clipboard.setText(AnnotatedString(rateSummary(it, rateState)))
                                        copied = true
                                    }
                                },
                                onReset = {
                                    if (marketRates != null) marketRates.reset() else calculator.reset()
                                },
                                onNavigateToInvoice = onNavigateToInvoice
                            )
                            FormulaCard()
                        }
                    }
                } else {
                    ScrollPane(Modifier.fillMaxSize()) {
                        InputPanel(state, calculator, marketRates, rateState)
                        ResultPanel(
                            state = state,
                            copied = copied,
                            onCopy = {
                                calculator.summary()?.let {
                                    clipboard.setText(AnnotatedString(rateSummary(it, rateState)))
                                    copied = true
                                }
                            },
                            onReset = {
                                if (marketRates != null) marketRates.reset() else calculator.reset()
                            },
                            onNavigateToInvoice = onNavigateToInvoice
                        )
                        FormulaCard()
                    }
                }
            }

            Text(
                if (rateState?.automatic == true) "محاسبه با نرخ انتخابی بازار • مبالغ به تومان" else "محاسبه با نرخ واردشده شما • مبالغ به تومان",
                color = colors.textMuted,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun ScrollPane(modifier: Modifier, content: @Composable ColumnScope.() -> Unit) {
    val scroll = rememberScrollState()
    Box(modifier.fillMaxHeight()) {
        Column(
            Modifier.fillMaxSize().padding(end = 12.dp).verticalScroll(scroll),
            verticalArrangement = Arrangement.spacedBy(18.dp),
            content = content
        )
        VerticalScrollbar(rememberScrollbarAdapter(scroll), Modifier.align(Alignment.CenterEnd).fillMaxHeight())
    }
}

@Composable
private fun InputPanel(
    state: ManualGoldCalculatorState,
    calculator: ManualGoldCalculator,
    marketRates: DesktopCalculatorRates?,
    rateState: DesktopCalculatorRateState?
) {
    val colors = LocalGoldExColors.current

    // Card 1: نرخ و مشخصات طلا
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = colors.surface,
        border = BorderStroke(1.dp, colors.border),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            // Header
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = colors.goldContainer,
                    border = BorderStroke(1.dp, colors.goldBorder.copy(alpha = 0.5f)),
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Outlined.Scale,
                            contentDescription = null,
                            tint = colors.goldPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        "نرخ و مشخصات طلا",
                        color = colors.textMain,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    )
                    Text(
                        "تنظیم عیار، وزن قطعه و نرخ مبنای لحظه‌ای",
                        color = colors.textMuted,
                        fontSize = 12.sp
                    )
                }
                // Rate status and market button
                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = if (rateState?.automatic == true)
                            rateState.quote?.label(System.currentTimeMillis()) ?: "در انتظار نرخ بازار"
                        else
                            "نرخ دستی شما",
                        modifier = Modifier.testTag("calculator-rate-status"),
                        color = colors.goldPrimary,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Medium
                    )
                    if (marketRates != null && rateState?.automatic == false) {
                        TextButton(
                            onClick = marketRates::useMarketRate,
                            modifier = Modifier.testTag("calculator-use-market"),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                        ) {
                            Text("دریافت نرخ بازار", color = colors.goldPrimary, fontSize = 11.5.sp)
                        }
                    } else if (rateState?.automatic == true && rateState.quote != null) {
                        val converted = when (state.priceBasis) {
                            PriceBasisTab.K18 -> false
                            PriceBasisTab.K24 -> rateState.quote.rates.gold24 <= 0
                            PriceBasisTab.MESGHAL -> rateState.quote.rates.goldMelt <= 0
                        }
                        Text(
                            "${quoteSource(rateState.quote)} • ${DesktopPortfolioPolicy.observedTime(rateState.quote.observedAt)}${if (converted) " • معادل از ۱۸" else ""}",
                            color = colors.textMuted,
                            fontSize = 10.5.sp
                        )
                    }
                }
            }

            // Price basis selection
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "انتخاب پایه عیار یا مرجع قیمت:",
                    color = colors.textSecondary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
                LuxurySegmentedControl(
                    items = PriceBasisTab.values().toList(),
                    selectedItem = state.priceBasis,
                    onItemSelected = {
                        if (marketRates != null) marketRates.setPriceBasis(it) else calculator.setPriceBasis(it)
                    },
                    label = { tab ->
                        val base = when (tab) {
                            PriceBasisTab.K18 -> "۱۸ عیار"
                            PriceBasisTab.K24 -> "۲۴ عیار"
                            PriceBasisTab.MESGHAL -> "مظنه (مثقال)"
                        }
                        if (tab == state.priceBasis) "● $base" else base
                    },
                    modifier = Modifier.fillMaxWidth().testTag("basis"),
                    height = 44.dp,
                    fontSize = 13.sp
                )
            }

            // Spot / Basis rate field
            val spotLabel = when (state.priceBasis) {
                PriceBasisTab.K18 -> "نرخ مبنا (قیمت هر گرم طلا ۱۸ عیار ۷۵۰):"
                PriceBasisTab.K24 -> "نرخ مبنا (قیمت هر گرم طلا ۲۴ عیار):"
                PriceBasisTab.MESGHAL -> "نرخ مبنا (قیمت یک مثقال طلا ۱۷ عیار ۷۰۵):"
            }
            NumericInput(
                state = state,
                field = CalculatorField.SPOT,
                label = spotLabel,
                unit = "تومان",
                calculator = calculator,
                marketRates = marketRates
            )

            // Gross weight & stone weight row
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                NumericInput(
                    state = state,
                    field = CalculatorField.GROSS_WEIGHT,
                    label = "وزن کل طلا:",
                    sublabel = "(با احتساب متعلقات)",
                    unit = "گرم",
                    calculator = calculator,
                    modifier = Modifier.weight(1f)
                )
                NumericInput(
                    state = state,
                    field = CalculatorField.STONE_WEIGHT,
                    label = "کسر نگین و سنگ:",
                    sublabel = "(از کل کسر می‌گردد)",
                    unit = "گرم",
                    calculator = calculator,
                    modifier = Modifier.weight(1f)
                )
            }

            // Karat field
            NumericInput(
                state = state,
                field = CalculatorField.KARAT,
                label = "عیار استاندارد قطعه:",
                labelNote = "عیار رسمی بازار ایران: ۷۵۰",
                unit = "عیار",
                calculator = calculator
            )
        }
    }

    // Card 2: اجرت، سود و مالیات
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = colors.surface,
        border = BorderStroke(1.dp, colors.border),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            // Header
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = colors.goldContainer,
                    border = BorderStroke(1.dp, colors.goldBorder.copy(alpha = 0.5f)),
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Outlined.ReceiptLong,
                            contentDescription = null,
                            tint = colors.goldPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        "اجرت، سود و مالیات",
                        color = colors.textMain,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    )
                    Text(
                        "محاسبه بر مبنای قانون مالیات بر ارزش افزوده جدید",
                        color = colors.textMuted,
                        fontSize = 12.sp
                    )
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = colors.surfaceElevated,
                    border = BorderStroke(1.dp, colors.border.copy(alpha = 0.6f))
                ) {
                    Text(
                        "قانون مصوب اتحادیه",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        color = colors.textSecondary,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Wage type selection
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "نوع اجرت ساخت کارگاه:",
                    color = colors.textSecondary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
                LuxurySegmentedControl(
                    items = WageType.values().toList(),
                    selectedItem = state.wageType,
                    onItemSelected = calculator::setWageType,
                    label = { type ->
                        when (type) {
                            WageType.PERCENTAGE -> "اجرت درصدی (٪)"
                            WageType.TOMAN_PER_GRAM -> "اجرت هر گرم (تومان)"
                        }
                    },
                    modifier = Modifier.fillMaxWidth().testTag("wageType"),
                    height = 44.dp,
                    fontSize = 13.sp
                )
            }

            // Wage field
            NumericInput(
                state = state,
                field = CalculatorField.WAGE,
                label = "اجرت ساخت:",
                sublabel = if (state.wageType == WageType.PERCENTAGE)
                    "محاسبه درصدی از ارزش طلای خام"
                else
                    "مبلغ به تومان به ازای هر گرم",
                unit = if (state.wageType == WageType.PERCENTAGE) "٪" else "تومان",
                calculator = calculator
            )

            // Profit & Tax fields row
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                NumericInput(
                    state = state,
                    field = CalculatorField.PROFIT,
                    label = "سود فروشنده (مغازه):",
                    unit = "٪",
                    calculator = calculator,
                    modifier = Modifier.weight(1f)
                )
                NumericInput(
                    state = state,
                    field = CalculatorField.TAX,
                    label = "مالیات بر ارزش افزوده:",
                    unit = "٪",
                    calculator = calculator,
                    modifier = Modifier.weight(1f)
                )
            }

            // Bottom informational banner
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = colors.goldContainer.copy(alpha = 0.12f),
                border = BorderStroke(0.6.dp, colors.goldBorder.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Outlined.Info,
                        contentDescription = null,
                        tint = colors.goldPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        "مالیات بر ارزش افزوده صرفاً بر روی حاصل جمع (اجرت ساخت + سود فروشنده) اعمال شده و اصل طلای خام طبق قانون کشوری از مالیات معاف است.",
                        color = colors.textMuted,
                        fontSize = 11.5.sp,
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun NumericInput(
    state: ManualGoldCalculatorState,
    field: CalculatorField,
    label: String,
    unit: String,
    calculator: ManualGoldCalculator,
    modifier: Modifier = Modifier,
    marketRates: DesktopCalculatorRates? = null,
    sublabel: String? = null,
    labelNote: String? = null
) {
    val colors = LocalGoldExColors.current
    val error = state.errors[field]
    val integer = field == CalculatorField.SPOT || field == CalculatorField.KARAT

    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    label,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = colors.textSecondary
                )
                if (sublabel != null) {
                    Text(
                        sublabel,
                        fontSize = 11.5.sp,
                        color = colors.textMuted
                    )
                }
            }
            if (labelNote != null) {
                Text(
                    labelNote,
                    fontSize = 11.5.sp,
                    color = colors.textMuted
                )
            }
        }

        GoldOutlinedTextField(
            value = state.input(field),
            onValueChange = {
                val value = PersianNumberFormatter.toEnglishDigits(it)
                    .replace("٬", "")
                    .replace("،", "")
                    .replace(",", "")
                if (marketRates != null) marketRates.setInput(field, value) else calculator.setInput(field, value)
            },
            modifier = Modifier.fillMaxWidth().testTag("input-${field.name}"),
            singleLine = true,
            isError = error != null,
            trailingIcon = {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = colors.surfaceElevated,
                    border = BorderStroke(1.dp, colors.border.copy(alpha = 0.5f)),
                    modifier = Modifier.padding(start = 4.dp, end = 6.dp)
                ) {
                    Box(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = unit,
                            color = colors.textMuted,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            },
            textStyle = TextStyle(
                fontFamily = VazirmatnFamily,
                fontFeatureSettings = VazirmatnFeatureSettings,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.textMain,
                textDirection = TextDirection.Ltr
            ),
            visualTransformation = ThousandsSeparatorVisualTransformation(addSeparators = field == CalculatorField.SPOT),
            keyboardOptions = KeyboardOptions(keyboardType = if (integer) KeyboardType.Number else KeyboardType.Decimal),
            shape = RoundedCornerShape(10.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = colors.goldPrimary,
                unfocusedBorderColor = colors.border,
                focusedContainerColor = colors.surfaceElevated,
                unfocusedContainerColor = colors.surface,
                errorBorderColor = colors.errorRed,
                errorLabelColor = colors.errorRed,
                cursorColor = colors.goldPrimary
            )
        )
        if (error != null) {
            Text(programErrorText(error), color = colors.errorRed, fontSize = 12.sp)
        }
    }
}

@Composable
private fun ResultPanel(
    state: ManualGoldCalculatorState,
    copied: Boolean,
    onCopy: () -> Unit,
    onReset: () -> Unit,
    onNavigateToInvoice: () -> Unit
) {
    val result = state.result
    val colors = LocalGoldExColors.current

    // Deep Obsidian Card
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color(0xFF13151A),
        border = BorderStroke(1.dp, if (colors.isDark) Color(0x33D4AF37) else Color(0xFF262A33)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(Modifier.size(8.dp).background(Color(0xFFEAB308), CircleShape))
                Text(
                    "خلاصه محاسبه و فاکتور",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }

            // Top Inset Hero Display
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFF0C0D11),
                border = BorderStroke(1.dp, Color(0xFF222630)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "مبلغ نهایی پرداخت مشتری:",
                        color = Color(0xFF9CA3AF),
                        fontSize = 12.5.sp,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Start
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    ) {
                        AnimatedPriceTicker(
                            text = result?.let { PersianNumberFormatter.formatPrice(it.totalPayable) } ?: "—",
                            modifier = Modifier.alignByBaseline().testTag("total"),
                            color = Color(0xFFFBBF24),
                            fontSize = 32.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "تومان",
                            color = Color(0xFFFBBF24),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.alignByBaseline().testTag("total-unit")
                        )
                    }
                    Text(
                        text = if (result != null && result.totalPayable > 0.0)
                            "(${PersianWordsFormatter.toWords(result.totalPayable.toLong())})"
                        else
                            "(سیستم آماده محاسبه است)",
                        color = Color(0xFF9CA3AF),
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Breakdown List
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ObsidianResultLine(
                    label = "وزن خالص طلا",
                    value = result?.let { PersianNumberFormatter.formatWeight(it.netWeight) },
                    unit = "گرم"
                )
                ObsidianResultLine(
                    label = "ارزش خام طلا",
                    value = result?.let { PersianNumberFormatter.formatPrice(it.rawGoldValue) },
                    unit = "تومان"
                )

                val wageStr = PersianNumberFormatter.toPersianDigits(state.input(CalculatorField.WAGE).ifBlank { "0" })
                val wageLabel = if (state.wageType == WageType.PERCENTAGE)
                    "اجرت ساخت طلا ($wageStr٪)"
                else
                    "اجرت ساخت طلا ($wageStr تومان)"
                ObsidianResultLine(
                    label = wageLabel,
                    value = result?.let { PersianNumberFormatter.formatPrice(it.wageAmount) },
                    unit = "تومان"
                )

                val profitStr = PersianNumberFormatter.toPersianDigits(state.input(CalculatorField.PROFIT).ifBlank { "0" })
                ObsidianResultLine(
                    label = "سود فروشنده ($profitStr٪)",
                    value = result?.let { PersianNumberFormatter.formatPrice(it.profitAmount) },
                    unit = "تومان"
                )

                val taxStr = PersianNumberFormatter.toPersianDigits(state.input(CalculatorField.TAX).ifBlank { "0" })
                ObsidianResultLine(
                    label = "مالیات ارزش‌افزوده مصوب ($taxStr٪ سود و اجرت)",
                    value = result?.let { PersianNumberFormatter.formatPrice(it.taxAmount) },
                    unit = "تومان"
                )
            }

            // Inset Pill for Effective Gram Price
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFF0C0D11),
                border = BorderStroke(1.dp, Color(0xFF222630)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "قیمت تمام‌شده هر گرم برای خریدار:",
                        color = Color(0xFFD1D5DB),
                        fontSize = 12.5.sp
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        AnimatedPriceTicker(
                            text = result?.let { PersianNumberFormatter.formatPrice(it.effectiveGramPrice) } ?: "—",
                            color = Color(0xFFFBBF24),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text("تومان", color = Color(0xFF9CA3AF), fontSize = 11.sp)
                    }
                }
            }

            // Primary Button: انتقال مستقیم به صدور فاکتور رسمی
            Button(
                onClick = onNavigateToInvoice,
                enabled = result != null,
                modifier = Modifier.fillMaxWidth().height(46.dp).testTag("transfer-to-invoice"),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(0.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent,
                    disabledContainerColor = Color(0xFF1E222A)
                )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .then(
                            if (result != null)
                                Modifier.background(Brush.horizontalGradient(listOf(Color(0xFFF59E0B), Color(0xFFD97706))))
                            else
                                Modifier.background(Color(0xFF1E222A))
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            "انتقال مستقیم به صدور فاکتور رسمی",
                            color = if (result != null) Color(0xFF111827) else Color(0xFF6B7280),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp
                        )
                        Icon(
                            Icons.Outlined.ReceiptLong,
                            contentDescription = null,
                            tint = if (result != null) Color(0xFF111827) else Color(0xFF6B7280),
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }
            }

            // Secondary Buttons Row: کپی خلاصه & پاک کردن
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onCopy,
                    enabled = result != null,
                    modifier = Modifier.weight(1f).height(42.dp).testTag("copy"),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF1E222A),
                        disabledContainerColor = Color(0xFF16181F),
                        contentColor = Color.White,
                        disabledContentColor = Color(0xFF6B7280)
                    ),
                    border = BorderStroke(1.dp, Color(0xFF374151))
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Outlined.ContentCopy,
                            contentDescription = null,
                            tint = if (result != null) Color.White else Color(0xFF6B7280),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = if (copied) "کپی شد" else "کپی خلاصه",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Button(
                    onClick = onReset,
                    modifier = Modifier.weight(1f).height(42.dp).testTag("reset"),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF1E222A),
                        contentColor = Color(0xFFD1D5DB)
                    ),
                    border = BorderStroke(1.dp, Color(0xFF374151))
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Outlined.DeleteOutline,
                            contentDescription = null,
                            tint = Color(0xFFD1D5DB),
                            modifier = Modifier.size(17.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "پاک کردن",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ObsidianResultLine(label: String, value: String?, unit: String) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(Modifier.size(4.dp).background(Color(0xFFEAB308), CircleShape))
            Text(
                label,
                color = Color(0xFFD1D5DB),
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            AnimatedPriceTicker(
                text = value ?: "—",
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(unit, color = Color(0xFF9CA3AF), fontSize = 11.sp)
        }
    }
}

@Composable
private fun FormulaCard() {
    val colors = LocalGoldExColors.current
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = colors.surface,
        border = BorderStroke(1.dp, colors.border),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Outlined.Info,
                    contentDescription = null,
                    tint = colors.goldPrimary,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    "فرمول نحوه محاسبه استاندارد:",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.5.sp,
                    color = colors.textMain
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("۱. ارزش طلای خام = وزن خالص × قیمت هر گرم طلا ۱۸ عیار", fontSize = 11.5.sp, color = colors.textMuted)
                Text("۲. اجرت ساخت = ارزش خام × درصد اجرت", fontSize = 11.5.sp, color = colors.textMuted)
                Text("۳. سود طلافروش = (ارزش خام + اجرت) × درصد سود (۷٪)", fontSize = 11.5.sp, color = colors.textMuted)
                Text("۴. مالیات بر ارزش افزوده = (اجرت ساخت + سود) × ۱۰٪", fontSize = 11.5.sp, color = colors.textMuted)
                Text("مبلغ نهایی = ارزش خام + اجرت + سود + مالیات", fontSize = 11.5.sp, color = colors.goldPrimary, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

private fun rateSummary(summary: String, rate: DesktopCalculatorRateState?): String =
    if (rate?.automatic == true && rate.quote != null) summary.replace(
        "محاسبه با نرخ دستی",
        "محاسبه با ${rate.quote.label(System.currentTimeMillis())} • ${quoteSource(rate.quote)} • ${DesktopPortfolioPolicy.observedTime(rate.quote.observedAt)}"
    ) else summary

private fun quoteSource(quote: MarketSnapshot) =
    if (quote.kind == QuoteKind.MANUAL) "ثبت‌شده توسط شما" else quote.rates.source.labelFa
