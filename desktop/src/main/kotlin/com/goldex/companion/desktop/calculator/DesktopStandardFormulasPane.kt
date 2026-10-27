package com.goldex.companion.desktop.calculator

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.goldex.companion.desktop.ui.controls.DesktopActionButton
import com.goldex.companion.desktop.ui.controls.DesktopFilterChip
import com.goldex.companion.desktop.ui.controls.DesktopSearchField
import com.goldex.companion.model.PersianNumberFormatter
import com.goldex.companion.ui.components.LuxuryCard
import com.goldex.companion.ui.theme.LocalGoldExColors
import com.goldex.companion.ui.theme.heroCardGradient

enum class FormulaCategory(val title: String) {
    ALL("همه"),
    INVOICE("فاکتور"),
    KARAT("عیار"),
    COIN("مسکوکات"),
    MELT("آبشده")
}

data class FormulaCardItem(
    val id: String,
    val title: String,
    val category: FormulaCategory,
    val categoryBadge: String,
    val mathFormula: String,
    val parameters: List<Pair<String, String>>,
    val unionTip: String
)

@Composable
fun DesktopStandardFormulasPane(
    defaultProfitPercent: String = "7",
    defaultTaxPercent: String = "10"
) {
    val colors = LocalGoldExColors.current
    val clipboard = LocalClipboardManager.current

    var selectedCategory by remember { mutableStateOf(FormulaCategory.ALL) }
    var searchQuery by remember { mutableStateOf("") }
    var copiedFormulaId by remember { mutableStateOf<String?>(null) }
    var copiedAll by remember { mutableStateOf(false) }

    val profitLabel = PersianNumberFormatter.toPersianDigits(defaultProfitPercent.ifBlank { "7" })
    val taxLabel = PersianNumberFormatter.toPersianDigits(defaultTaxPercent.ifBlank { "10" })
    val masterFormulaText = "قیمت نهایی = ارزش طلای خام + اجرت + سود (${profitLabel}٪) + مالیات اجرت و سود (${taxLabel}٪)"

    val formulas = remember(profitLabel, taxLabel) {
        listOf(
            FormulaCardItem(
                id = "f1",
                title = "فرمول ارزش طلای خام ۱۸ عیار (اصل طلا)",
                category = FormulaCategory.INVOICE,
                categoryBadge = "اصل طلا",
                mathFormula = "ارزش طلای خام = وزن خالص (گرم) × (مظنه روز ÷ ۴.۳۳۱۸)",
                parameters = listOf(
                    "وزن خالص" to "وزن قطعه طلای ۱۸ عیار پس از کسر وزن نگین‌های غیرجواهری و ناخالصی.",
                    "مظنه روز (مظنه آبشده)" to "قیمت یک مثقال طلای ۱۷ عیار (عیار ۷۰۵) در بازار تهران.",
                    "ضریب ۴.۳۳۱۸" to "نسبت استاندارد تبدیل ۱ مثقال طلای ۱۷ به ۱ گرم طلای ۱۸ (۴.۶۰۸ × ۷۰۵ ÷ ۷۵۰)."
                ),
                unionTip = "اصل طلای خام ۱۰۰٪ از پرداخت مالیات بر ارزش افزوده معاف است."
            ),
            FormulaCardItem(
                id = "f2",
                title = "فرمول محاسبه اجرت ساخت کارگاهی",
                category = FormulaCategory.INVOICE,
                categoryBadge = "اجرت ساخت",
                mathFormula = "درصدی: ارزش طلای خام × (درصد اجرت ÷ ۱۰۰)\nریالی: وزن خالص (گرم) × اجرت هر گرم (تومان)",
                parameters = listOf(
                    "درصد اجرت" to "توافقی و وابسته به پیچیدگی و ظرافت ساخت النگو، سرویس یا انگشتر (معمولاً بین ۸٪ تا ۲۵٪).",
                    "اجرت ریالی/گرمی" to "مبلغ ثابت توافق‌شده به ازای ساخت هر گرم کارگاه."
                ),
                unionTip = "درج دقیق نحوه محاسبه و مبلغ اجرت در فاکتور رسمی الزامی است."
            ),
            FormulaCardItem(
                id = "f3",
                title = "فرمول سود قانونی طلافروش (خرده‌فروشی)",
                category = FormulaCategory.INVOICE,
                categoryBadge = "سود مصوب ${profitLabel}٪",
                mathFormula = "سود طلافروش = (ارزش طلای خام + مبلغ اجرت ساخت) × ${profitLabel}٪",
                parameters = listOf(
                    "درصد سود" to "درصد سود قانونی مصوب کمیسیون نظارت بر اصناف و اتحادیه (حداکثر ۷٪).",
                    "مبنای محاسبه سود" to "سود فروشنده به مجموع اصل طلا و اجرت ساخت تعلق می‌گیرد."
                ),
                unionTip = "درصد سود باید شفاف و مطابق تنظیمات ثبت‌شده در فاکتور رسمی نمایش داده شود."
            ),
            FormulaCardItem(
                id = "f4",
                title = "فرمول مالیات بر ارزش افزوده (ماده ۲۶)",
                category = FormulaCategory.INVOICE,
                categoryBadge = "مالیات ${taxLabel}٪",
                mathFormula = "مالیات بر ارزش افزوده = (مبلغ اجرت ساخت + سود طلافروش) × ${taxLabel}٪",
                parameters = listOf(
                    "مأخذ محاسبه مالیات" to "صرفاً خدمات ساخت (اجرت) و سود حاصل از فروش.",
                    "معافیت اصل طلا" to "طبق قانون دائمی مالیات بر ارزش افزوده، هیچ مالیاتی به ارزش خود طلا تعلق نمی‌گیرد."
                ),
                unionTip = "محاسبه مالیات بر روی اصل قیمت طلا تخلف قانونی بوده و فاکتور را از اعتبار ساقط می‌کند."
            ),
            FormulaCardItem(
                id = "f5",
                title = "فرمول تبدیل تخصصی و تسویه عیار",
                category = FormulaCategory.KARAT,
                categoryBadge = "تبدیل عیار",
                mathFormula = "وزن در عیار مقصد = (وزن اولیه × عیار اولیه) ÷ عیار مقصد",
                parameters = listOf(
                    "عیار ۷۵۰ (۱۸K)" to "استاندارد رسمی کلیه مصنوعات طلای کارگاهی و ویترینی کشور.",
                    "عیار ۷۰۵ (۱۷K)" to "مبنای مظنه بازار سنتی و شمش آبشده تهران.",
                    "عیار ۹۹۵ و ۹۹۹ (۲۴K)" to "شمش خالص طلای بانک مرکزی و شمش‌های استاندارد جهانی."
                ),
                unionTip = "در تسویه معاملات بنکداری و کارگاهی، تبدیل اوزان همیشه بر پایه عیار ۷۵۰ انجام می‌شود."
            ),
            FormulaCardItem(
                id = "f6",
                title = "فرمول محاسبه ارزش ذاتی و حباب سکه بانکی",
                category = FormulaCategory.COIN,
                categoryBadge = "حباب مسکوکات",
                mathFormula = "قیمت ذاتی = (انس جهانی × نرخ دلار آزاد × وزن سکه × ۰.۹۰۰) ÷ ۳۱.۱۰۳۵\nحباب سکه = قیمت بازار سکه - قیمت ذاتی سکه",
                parameters = listOf(
                    "تمام سکه بهار آزادی / امامی" to "وزن ۸.۱۳۳ گرم با عیار ۹۰۰ (۲۱.۶ عیار).",
                    "نیم سکه بهار آزادی" to "وزن ۴.۰۶۶ گرم با عیار ۹۰۰.",
                    "ربع سکه بهار آزادی" to "وزن ۲.۰۳۳ گرم با عیار ۹۰۰.",
                    "سکه گرمی" to "وزن ۱.۰۱ گرم با عیار ۹۰۰."
                ),
                unionTip = "حباب سکه بیانگر تقاضای سفته‌بازی بازار نسبت به طلای فیزیکی موجود در آن است."
            ),
            FormulaCardItem(
                id = "f7",
                title = "فرمول تعویض و خرید طلای متفرقه (کهنه / آبشده)",
                category = FormulaCategory.MELT,
                categoryBadge = "تهاتر و آبشده",
                mathFormula = "ارزش طلای متفرقه = وزن × (مظنه ÷ ۴.۳۳۱۸) × (عیار متفرقه ÷ ۷۵۰) - کسر ذوب",
                parameters = listOf(
                    "کسر ذوب و ری‌گیری" to "کسر حدود ۱۰ الی ۲۰ خط در هزار بابت کثیفی، لعاب و آبکاری کارگاهی.",
                    "روش تهاتر" to "ارزش طلای متفرقه از مبلغ نهایی فاکتور طلای نو کسر می‌گردد."
                ),
                unionTip = "ثبت اطلاعات هویتی مشتری و فاکتور خرید اولیه در معاملات طلای متفرقه الزامی است."
            )
        )
    }

    val filteredFormulas = remember(selectedCategory, searchQuery, formulas) {
        formulas.filter { item ->
            val matchesCategory = selectedCategory == FormulaCategory.ALL || item.category == selectedCategory
            val matchesSearch = searchQuery.isBlank() ||
                item.title.contains(searchQuery, ignoreCase = true) ||
                item.mathFormula.contains(searchQuery, ignoreCase = true) ||
                item.parameters.any { it.first.contains(searchQuery, ignoreCase = true) || it.second.contains(searchQuery, ignoreCase = true) }
            matchesCategory && matchesSearch
        }
    }

    val scrollState = rememberScrollState()

    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Master Law Banner (Article 26)
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = Color.Transparent,
            border = BorderStroke(0.8.dp, colors.goldBorder),
            modifier = Modifier.fillMaxWidth().testTag("master-law-banner")
        ) {
            Column(
                Modifier.fillMaxWidth().background(colors.heroCardGradient).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(Modifier.size(8.dp).clip(RoundedCornerShape(4.dp)).background(colors.goldPrimary))
                        Text(
                            "قاعده مرجع صدور فاکتور رسمی کشور (ماده ۲۶ قانون مالیات بر ارزش افزوده)",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = colors.goldPrimary.copy(alpha = 0.2f),
                        border = BorderStroke(0.5.dp, colors.goldPrimary.copy(alpha = 0.5f))
                    ) {
                        Text(
                            "فرمول استاندارد صنف",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.goldPrimary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color.Black.copy(alpha = 0.4f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        masterFormulaText,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.goldSecondary,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                    )
                }

                Text(
                    "طبق نص صریح قانون، اصل طلای خام به‌طور کامل از مالیات بر ارزش افزوده معاف است و مالیات صرفاً بر روی خدمات ساخت (اجرت) و سود فروشنده محاسبه می‌گردد.",
                    fontSize = 11.5.sp,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
        }

        // Search and Filter Bar
        Column(
            Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                FormulaCategory.values().forEach { cat ->
                    DesktopFilterChip(
                        text = cat.title,
                        selected = selectedCategory == cat,
                        onClick = { selectedCategory = cat },
                        modifier = Modifier.testTag("filter-formula-${cat.name}")
                    )
                }
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f)) {
                    DesktopSearchField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        hint = "جستجو در فرمول‌ها...",
                        inputTag = "formula-search-input"
                    )
                }

                DesktopActionButton(
                    text = if (copiedAll) "کپی شد ✓" else "کپی کل فرمول‌ها",
                    onClick = {
                        val allText = buildString {
                            appendLine("📋 راهنمای فرمول‌های استاندارد اتحادیه طلا و جواهر (سامانه قیراط):")
                            appendLine("----------------------------------------")
                            formulas.forEach { f ->
                                appendLine("• ${f.title}:")
                                appendLine("  ${f.mathFormula}")
                                appendLine("  نکته: ${f.unionTip}")
                                appendLine()
                            }
                        }
                        try {
                            clipboard.setText(AnnotatedString(allText))
                        } catch (_: Throwable) {}
                        copiedAll = true
                    },
                    icon = if (copiedAll) Icons.Outlined.Check else Icons.Outlined.ContentCopy,
                    isSecondary = true,
                    height = 42.dp,
                    modifier = Modifier.testTag("copy-all-formulas-btn")
                )
            }
        }

        // Formulas List
        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            filteredFormulas.forEach { item ->
                FormulaCard(
                    item = item,
                    copied = copiedFormulaId == item.id,
                    onCopy = {
                        val text = buildString {
                            appendLine("${item.title}:")
                            appendLine(item.mathFormula)
                            appendLine("نکته اتحادیه: ${item.unionTip}")
                        }
                        try {
                            clipboard.setText(AnnotatedString(text))
                        } catch (_: Throwable) {}
                        copiedFormulaId = item.id
                    }
                )
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun FormulaCard(
    item: FormulaCardItem,
    copied: Boolean,
    onCopy: () -> Unit
) {
    val colors = LocalGoldExColors.current

    LuxuryCard(
        contentPadding = PaddingValues(0.dp),
        modifier = Modifier.fillMaxWidth().testTag("formula-card-${item.id}")
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            // Header
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(
                        Modifier.size(36.dp).clip(RoundedCornerShape(8.dp)).background(colors.surfaceElevated),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Outlined.Functions, null, Modifier.size(20.dp), tint = colors.goldPrimary)
                    }
                    Text(item.title, fontWeight = FontWeight.Bold, fontSize = 14.5.sp, color = colors.textMain)
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = colors.goldPrimary.copy(alpha = 0.15f),
                        border = BorderStroke(0.5.dp, colors.goldPrimary.copy(alpha = 0.4f))
                    ) {
                        Text(
                            item.categoryBadge,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.goldPrimary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }

                    DesktopActionButton(
                        text = if (copied) "کپی شد ✓" else "کپی فرمول",
                        onClick = onCopy,
                        icon = if (copied) Icons.Outlined.Check else Icons.Outlined.ContentCopy,
                        isSecondary = true,
                        height = 34.dp
                    )
                }
            }

            // Math Formula Box
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = colors.surfaceElevated,
                border = BorderStroke(0.6.dp, colors.border),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    item.mathFormula,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.goldSecondary,
                    modifier = Modifier.padding(14.dp)
                )
            }

            // Parameters
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                item.parameters.forEach { (name, desc) ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("• $name:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = colors.textMain)
                        Text(desc, fontSize = 12.sp, color = colors.textMuted)
                    }
                }
            }

            // Union Guild Tip
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = colors.surfaceElevated,
                border = BorderStroke(0.5.dp, colors.border),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Outlined.Lightbulb, null, Modifier.size(18.dp), tint = colors.goldPrimary)
                    Text(
                        item.unionTip,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = colors.textMain
                    )
                }
            }
        }
    }
}
