package com.goldex.companion.ui.hub

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.goldex.companion.ui.components.GoldButton
import com.goldex.companion.ui.theme.LocalGoldExColors

/**
 * Gold Union Standard Formulas Guide (Stitch Screen ID: 1e8173ae11924cad8cabf7f74a1c042b)
 *
 * Official Iranian Gold & Jewelry Union calculation rules, tax exemption laws (Article 26 VAT Law),
 * and standard bazaar formulas for jewelry, melt, karat conversion, coin bubbles, and barters.
 */

private enum class FormulaCategory(val title: String) {
    ALL("همه فرمول‌ها"),
    INVOICE("محاسبه فاکتور"),
    KARAT("تبدیل و کسری عیار"),
    COIN("حباب و مسکوکات"),
    MELT("تهاتر و آبشده")
}

private data class FormulaItem(
    val id: String,
    val title: String,
    val category: FormulaCategory,
    val categoryBadge: String,
    val mathFormula: String,
    val parameters: List<Pair<String, String>>,
    val unionTip: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StandardFormulasScreen(
    onBack: () -> Unit,
    onNavigateCalculator: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalGoldExColors.current
    val context = LocalContext.current
    var selectedCategory by remember { mutableStateOf(FormulaCategory.ALL) }

    val masterFormulaText = "قیمت نهایی فاکتور = [ (وزن خالص × (مظنه روز ÷ ۴.۳۳۱۸) + اجرت ساخت) × ۱.۰۷ ] × ۱.۰۹"

    val formulas = remember {
        listOf(
            FormulaItem(
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
            FormulaItem(
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
            FormulaItem(
                id = "f3",
                title = "فرمول سود قانونی طلافروش (خرده‌فروشی)",
                category = FormulaCategory.INVOICE,
                categoryBadge = "سود مصوب",
                mathFormula = "سود طلافروش = (ارزش طلای خام + مبلغ اجرت ساخت) × ۷٪",
                parameters = listOf(
                    "سقف سود قانونی" to "مطابق مصوبه اتحادیه طلا و جواهر، حداکثر سود خرده‌فروشی ۷٪ است.",
                    "مبنای محاسبه سود" to "سود فروشنده به مجموع اصل طلا و اجرت ساخت تعلق می‌گیرد."
                ),
                unionTip = "اخذ هرگونه درصد یا وجه مازاد بر ۷٪ تخلف صنفی محسوب می‌شود."
            ),
            FormulaItem(
                id = "f4",
                title = "فرمول مالیات بر ارزش افزوده (ماده ۲۶ ق.م.ا)",
                category = FormulaCategory.INVOICE,
                categoryBadge = "مالیات ۹٪",
                mathFormula = "مالیات بر ارزش افزوده (۹٪) = (مبلغ اجرت ساخت + سود طلافروش) × ۹٪",
                parameters = listOf(
                    "مأخذ محاسبه مالیات" to "صرفاً خدمات ساخت (اجرت) و سود حاصل از فروش.",
                    "معافیت اصل طلا" to "طبق قانون دائمی مالیات بر ارزش افزوده، هیچ مالیاتی به ارزش خود طلا تعلق نمی‌گیرد."
                ),
                unionTip = "محاسبه مالیات بر روی اصل قیمت طلا غیرقانونی بوده و فاکتور را از اعتبار ساقط می‌کند."
            ),
            FormulaItem(
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
            FormulaItem(
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
            FormulaItem(
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

    val filteredFormulas = remember(selectedCategory) {
        if (selectedCategory == FormulaCategory.ALL) {
            formulas
        } else {
            formulas.filter { it.category == selectedCategory }
        }
    }

    val copyToClipboard: (String, String) -> Unit = { text, message ->
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Gold Union Formula", text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    val shareFormulasText: () -> Unit = {
        val shareBody = buildString {
            appendLine("📋 راهنمای فرمول‌های استاندارد اتحادیه طلا و جواهر (سامانه قیراط):")
            appendLine()
            appendLine("⭐️ فرمول جامع محاسبه فاکتور قانونی:")
            appendLine(masterFormulaText)
            appendLine()
            appendLine("📌 قانون معافیت اصل طلا (ماده ۲۶ ق.م.ا):")
            appendLine("اصل طلا از ۹٪ مالیات معاف است. مالیات صرفاً به (اجرت + سود) تعلق می‌گیرد.")
            appendLine()
            appendLine("۱. ارزش طلای خام = وزن × (مظنه ÷ ۴.۳۳۱۸)")
            appendLine("۲. سود طلافروش = (ارزش خام + اجرت) × ۷٪")
            appendLine("۳. مالیات قانونی = (اجرت + سود) × ۹٪")
            appendLine("۴. قیمت ذاتی سکه = (انس × دلار × وزن × ۰.۹۰۰) ÷ ۳۱.۱۰۳۵")
        }
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "فرمول‌های استاندارد اتحادیه طلا و جواهر")
            putExtra(Intent.EXTRA_TEXT, shareBody)
        }
        context.startActivity(Intent.createChooser(intent, "اشتراک‌گذاری فرمول‌های استاندارد"))
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Scaffold(
            modifier = modifier
                .fillMaxSize()
                .background(colors.background),
            containerColor = colors.background,
            topBar = {
                Surface(
                    color = colors.surface,
                    border = BorderStroke(0.6.dp, colors.goldBorder.copy(alpha = 0.5f)),
                    shadowElevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            IconButton(
                                onClick = onBack,
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(colors.surfaceElevated)
                            ) {
                                Icon(
                                    imageVector = HubArrowRight,
                                    contentDescription = "بازگشت",
                                    tint = colors.goldPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Column {
                                Text(
                                    text = "فرمول‌های استاندارد اتحادیه طلا",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.5.sp,
                                    color = colors.textMain
                                )
                                Text(
                                    text = "مقررات و ضوابط رسمی صنف طلا و جواهر کشور",
                                    fontSize = 10.5.sp,
                                    color = colors.textMuted
                                )
                            }
                        }

                        IconButton(
                            onClick = shareFormulasText,
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(colors.surfaceElevated)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "اشتراک‌گذاری",
                                tint = colors.goldPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            },
            bottomBar = {
                Surface(
                    color = colors.surface,
                    border = BorderStroke(0.8.dp, colors.goldBorder),
                    shadowElevation = 16.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        GoldButton(
                            text = "ورود به ماشین‌حساب و اجرای فرمول‌ها",
                            onClick = onNavigateCalculator,
                            isSecondary = false,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // ==========================================
                // 1. Dark Sovereign Master Monitor Card
                // ==========================================
                item {
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = Color(0xFF111726),
                        border = BorderStroke(
                            width = 1.2.dp,
                            brush = Brush.verticalGradient(
                                listOf(
                                    Color(0xFFF59E0B),
                                    Color(0xFFD97706).copy(alpha = 0.35f)
                                )
                            )
                        ),
                        shadowElevation = 12.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0xFFF59E0B).copy(alpha = 0.18f))
                                            .border(0.8.dp, Color(0xFFF59E0B), RoundedCornerShape(8.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = HubMenuBook,
                                            contentDescription = null,
                                            tint = Color(0xFFF59E0B),
                                            modifier = Modifier.size(17.dp)
                                        )
                                    }

                                    Column {
                                        Text(
                                            text = "فرمول جامع مصوب اتحادیه طلا",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.5.sp,
                                            color = Color.White
                                        )
                                        Text(
                                            text = "مبنای رسمی صدور فاکتور و سامانه جامع تجارت",
                                            fontSize = 10.sp,
                                            color = Color(0xFF94A3B8)
                                        )
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFF10B981).copy(alpha = 0.15f),
                                    border = BorderStroke(0.6.dp, Color(0xFF10B981).copy(alpha = 0.5f))
                                ) {
                                    Text(
                                        text = "ماده ۲۶ ق.م.ا",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF34D399),
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                    )
                                }
                            }

                            // Master Formula Code Display Box
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = Color(0xFF090D15),
                                border = BorderStroke(0.8.dp, Color(0xFF334155)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "مبلغ نهایی فاکتور خریدار:",
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFFFBBF24)
                                    )
                                    Text(
                                        text = "[ (وزن × (مظنه ÷ ۴.۳۳۱۸) + اجرت) × ۱.۰۷ ] × ۱.۰۹",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 13.5.sp,
                                        color = Color.White,
                                        lineHeight = 22.sp
                                    )
                                }
                            }

                            // Copy Master Formula Button
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color.White.copy(alpha = 0.08f),
                                    border = BorderStroke(0.8.dp, Color(0xFFF59E0B).copy(alpha = 0.4f)),
                                    modifier = Modifier.clickable {
                                        copyToClipboard(masterFormulaText, "فرمول جامع اتحادیه با موفقیت کپی شد")
                                    }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = HubCopy,
                                            contentDescription = null,
                                            tint = Color(0xFFF59E0B),
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Text(
                                            text = "کپی فرمول جامع",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFFDE68A)
                                        )
                                    }
                                }
                            }

                            HorizontalDivider(color = Color(0xFF334155).copy(alpha = 0.6f), thickness = 0.6.dp)

                            // Law Exemption Callout
                            Row(
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = Color(0xFF38BDF8),
                                    modifier = Modifier.size(16.dp).padding(top = 1.dp)
                                )
                                Text(
                                    text = "اصل طلا از پرداخت ۹٪ مالیات معاف است. مالیات بر ارزش افزوده منحصراً به مجموع «اجرت ساخت طلا + سود فروشنده» تعلق می‌گیرد.",
                                    fontSize = 10.5.sp,
                                    lineHeight = 17.sp,
                                    color = Color(0xFFE2E8F0),
                                    fontWeight = FontWeight.Normal
                                )
                            }
                        }
                    }
                }

                // ==========================================
                // 2. Filter Category Pills (Horizontal Scroll)
                // ==========================================
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FormulaCategory.values().forEach { category ->
                            val isSelected = category == selectedCategory
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) colors.goldPrimary else colors.surface,
                                border = BorderStroke(
                                    width = 0.8.dp,
                                    color = if (isSelected) colors.goldPrimary else colors.border
                                ),
                                shadowElevation = if (isSelected) 3.dp else 0.dp,
                                modifier = Modifier.clickable { selectedCategory = category }
                            ) {
                                Text(
                                    text = category.title,
                                    fontSize = 11.5.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Color.Black else colors.textMuted,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                                )
                            }
                        }
                    }
                }

                // ==========================================
                // 3. Official Formula Cards
                // ==========================================
                items(filteredFormulas, key = { it.id }) { item ->
                    FormulaCardItem(
                        item = item,
                        onCopy = { copyToClipboard(it, "فرمول با موفقیت کپی شد") }
                    )
                }

                // Spacing above bottom bar
                item {
                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }
    }
}

@Composable
private fun FormulaCardItem(
    item: FormulaItem,
    onCopy: (String) -> Unit
) {
    val colors = LocalGoldExColors.current

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = colors.surface,
        border = BorderStroke(0.8.dp, colors.goldBorder),
        shadowElevation = if (colors.isDark) 0.dp else 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Card Header: Title & Category Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(colors.goldPrimary)
                    )
                    Text(
                        text = item.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.5.sp,
                        color = colors.textMain
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = colors.goldContainer,
                    border = BorderStroke(0.6.dp, colors.goldBorder)
                ) {
                    Text(
                        text = item.categoryBadge,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.goldPrimary,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                    )
                }
            }

            // Math Formula Box
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (colors.isDark) Color(0xFF0F141E) else Color(0xFFF8F9FA),
                border = BorderStroke(0.8.dp, colors.border.copy(alpha = 0.7f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.mathFormula,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (colors.isDark) Color(0xFFFDE68A) else Color(0xFF92400E),
                        lineHeight = 19.sp,
                        modifier = Modifier.weight(1f)
                    )

                    IconButton(
                        onClick = { onCopy(item.mathFormula) },
                        modifier = Modifier
                            .size(30.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(colors.surfaceElevated)
                    ) {
                        Icon(
                            imageVector = HubCopy,
                            contentDescription = "کپی",
                            tint = colors.goldPrimary,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }

            // Parameters Breakdown
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                item.parameters.forEach { (label, desc) ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "•",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.goldPrimary
                        )
                        Text(
                            text = "$label: ",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = colors.textMain
                        )
                        Text(
                            text = desc,
                            fontSize = 11.sp,
                            color = colors.textMuted,
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            // Official Guild Warning / Tip Footer
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = colors.profitGreen.copy(alpha = 0.09f),
                border = BorderStroke(0.6.dp, colors.profitGreen.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = colors.profitGreen,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = item.unionTip,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (colors.isDark) Color(0xFF6EE7B7) else Color(0xFF065F46)
                    )
                }
            }
        }
    }
}
