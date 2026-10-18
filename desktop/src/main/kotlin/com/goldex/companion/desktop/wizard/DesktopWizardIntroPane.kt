package com.goldex.companion.desktop.wizard

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.goldex.companion.desktop.ui.controls.DesktopActionButton
import com.goldex.companion.desktop.ui.controls.DesktopIconButton
import com.goldex.companion.model.PersianNumberFormatter
import com.goldex.companion.ui.theme.LocalGoldExColors
import kotlinx.coroutines.delay

private data class DesktopIntroSlide(
    val title: String,
    val subtitle: String,
    val badge: String,
    val icon: ImageVector,
    val bulletPoints: List<String>
)

private val introSlides = listOf(
    DesktopIntroSlide(
        title = "محاسبه دقیق طلا طبق قانون ماده ۲۶",
        subtitle = "فرمول‌های استاندارد مصوب اتحادیه طلا و جواهر کشور",
        badge = "قانون مالیات بر ارزش افزوده",
        icon = Icons.Outlined.Calculate,
        bulletPoints = listOf(
            "معافیت کامل اصل طلا از پرداخت مالیات بر ارزش افزوده",
            "محاسبه تفکیک‌شده اجرت ساخت، سود گالری و ۹٪ مالیات قانونی",
            "صدور آنی فاکتور رسمی زرگری با فرمت چاپی معتبر و QR کد"
        )
    ),
    DesktopIntroSlide(
        title = "مدیریت جامع ویترین و گاوصندوق",
        subtitle = "کنترل دقیق موجودی فیزیکی طلا، آبشده و مسکوکات",
        badge = "انبار و اتیکت هوشمند",
        icon = Icons.Outlined.Inventory2,
        bulletPoints = listOf(
            "ثبت اوزان با دقت ۳ رقم اعشار (یک میلی‌گرم)",
            "پشتیبانی از انواع طلای ساخته، آبشده، سکه‌های بانکی و متفرقه",
            "چاپ استاندارد اتیکت بارکددار با مشخصات کامل کارگاه و اجرت"
        )
    ),
    DesktopIntroSlide(
        title = "دفتر حساب و گردش طلایی طرف‌حساب‌ها",
        subtitle = "حسابداری دوگانه بر پایه وزن طلای ۱۸ عیار و تومان",
        badge = "حسابداری زرگری",
        icon = Icons.Outlined.PeopleAlt,
        bulletPoints = listOf(
            "دفتر معین مشتریان و بنکداران با ثبت دریافت/پرداخت طلا و ریال",
            "تسویه آسان مانده‌حساب‌ها با تبدیل نرخ روز و صدور رسید تسویه",
            "ترازنامه مالی و گزارش سود و زیان دوره‌ای گالری"
        )
    ),
    DesktopIntroSlide(
        title = "تابلوی زنده مظنه‌ها و تحلیل حباب سکه",
        subtitle = "دریافت خودکار و لحظه‌ای نرخ‌های رسمی بازار طلا و ارز",
        badge = "نبض زنده بازار",
        icon = Icons.Outlined.ShowChart,
        bulletPoints = listOf(
            "نمایش لحظه‌ای مظنه آبشده، گرم ۱۸ عیار، انس جهانی و دلار",
            "محاسبه حباب واقعی انواع مسکوکات بانکی و تعیین ارزش ذاتی",
            "تحلیل روند نمودارهای قیمتی و تاریخچه نوسانات"
        )
    )
)

@Composable
fun DesktopWizardIntroPane(
    onStartConfiguration: () -> Unit,
    modifier: Modifier = Modifier,
    autoAdvance: Boolean = false
) {
    val colors = LocalGoldExColors.current
    var activeSlideIndex by remember { mutableStateOf(0) }

    if (autoAdvance) {
        LaunchedEffect(activeSlideIndex) {
            delay(6000)
            activeSlideIndex = (activeSlideIndex + 1) % introSlides.size
        }
    }

    val activeSlide = introSlides[activeSlideIndex]

    BoxWithConstraints(modifier = modifier.fillMaxSize().testTag("wizard-intro-pane")) {
        val isWide = maxWidth >= 960.dp

        if (isWide) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Left Column: Interactive Visual Showcase Card
                IntroHeroCard(
                    slide = activeSlide,
                    slideIndex = activeSlideIndex,
                    totalSlides = introSlides.size,
                    onPrev = {
                        activeSlideIndex = if (activeSlideIndex > 0) activeSlideIndex - 1 else introSlides.size - 1
                    },
                    onNext = {
                        activeSlideIndex = (activeSlideIndex + 1) % introSlides.size
                    },
                    onSelectSlide = { activeSlideIndex = it },
                    modifier = Modifier.weight(1.1f).fillMaxHeight()
                )

                // Right Column: Key Pillars & Call to Action
                IntroFeaturesSummaryCard(
                    onStart = onStartConfiguration,
                    modifier = Modifier.weight(1f).fillMaxHeight()
                )
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                IntroHeroCard(
                    slide = activeSlide,
                    slideIndex = activeSlideIndex,
                    totalSlides = introSlides.size,
                    onPrev = {
                        activeSlideIndex = if (activeSlideIndex > 0) activeSlideIndex - 1 else introSlides.size - 1
                    },
                    onNext = {
                        activeSlideIndex = (activeSlideIndex + 1) % introSlides.size
                    },
                    onSelectSlide = { activeSlideIndex = it },
                    modifier = Modifier.fillMaxWidth().height(420.dp)
                )

                IntroFeaturesSummaryCard(
                    onStart = onStartConfiguration,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun IntroHeroCard(
    slide: DesktopIntroSlide,
    slideIndex: Int,
    totalSlides: Int,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onSelectSlide: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalGoldExColors.current

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color.Transparent,
        border = BorderStroke(0.8.dp, colors.goldBorder),
        shadowElevation = if (colors.isDark) 0.dp else 4.dp,
        modifier = modifier.testTag("intro-hero-card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            colors.surfaceElevated,
                            colors.surface
                        )
                    )
                )
                .padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Badge and Brand
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Image(
                        painter = painterResource("mipmap-xxxhdpi/ic_launcher.png"),
                        contentDescription = "نشان قیراط",
                        modifier = Modifier.size(36.dp)
                    )
                    Text(
                        text = "قیراط • نرم‌افزار تخصصی زرگری",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.goldPrimary
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = colors.goldPrimary.copy(alpha = 0.2f),
                    border = BorderStroke(0.5.dp, colors.goldPrimary.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = slide.badge,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.goldPrimary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            // Slide Center Content with smooth AnimatedContent
            AnimatedContent(
                targetState = slide,
                transitionSpec = {
                    (fadeIn(tween(300)) + slideInHorizontally(tween(350)) { -it / 6 })
                        .togetherWith(fadeOut(tween(200)) + slideOutHorizontally(tween(250)) { it / 6 })
                },
                label = "intro-slide-animation",
                modifier = Modifier.fillMaxWidth().weight(1f).padding(vertical = 16.dp)
            ) { currentSlide ->
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(colors.goldPrimary.copy(alpha = 0.15f))
                            .border(1.dp, colors.goldPrimary.copy(alpha = 0.4f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = currentSlide.icon,
                            contentDescription = null,
                            tint = colors.goldPrimary,
                            modifier = Modifier.size(34.dp)
                        )
                    }

                    Spacer(Modifier.height(18.dp))

                    Text(
                        text = currentSlide.title,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )

                    Spacer(Modifier.height(6.dp))

                    Text(
                        text = currentSlide.subtitle,
                        fontSize = 13.sp,
                        color = Color.White.copy(alpha = 0.75f)
                    )

                    Spacer(Modifier.height(18.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        currentSlide.bulletPoints.forEach { point ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(colors.goldSecondary)
                                )
                                Text(
                                    text = point,
                                    fontSize = 13.sp,
                                    color = Color.White.copy(alpha = 0.9f)
                                )
                            }
                        }
                    }
                }
            }

            // Slider Navigation Controls (Dots & Arrows)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Indicator Dots
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(totalSlides) { idx ->
                        val isSelected = idx == slideIndex
                        Box(
                            modifier = Modifier
                                .height(6.dp)
                                .width(if (isSelected) 24.dp else 6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(
                                    if (isSelected) colors.goldPrimary else Color.White.copy(alpha = 0.3f)
                                )
                                .clickable { onSelectSlide(idx) }
                        )
                    }
                }

                // Prev/Next Navigation Buttons
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DesktopIconButton(
                        onClick = onPrev,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "اسلاید قبلی",
                            tint = colors.goldPrimary
                        )
                    }
                    DesktopIconButton(
                        onClick = onNext,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                            contentDescription = "اسلاید بعدی",
                            tint = colors.goldPrimary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun IntroFeaturesSummaryCard(
    onStart: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalGoldExColors.current

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = colors.surface,
        border = BorderStroke(0.8.dp, colors.border),
        shadowElevation = if (colors.isDark) 0.dp else 2.dp,
        modifier = modifier.testTag("intro-features-summary")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(4.dp, 20.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(colors.goldPrimary)
                    )
                    Text(
                        text = "آماده‌سازی نرم‌افزار در ۵ گام ساده",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textMain
                    )
                }

                Text(
                    text = "با تکمیل این راهنما، مشخصات گالری، تنظیمات سود و مالیات و موجودی اولیه انبار شما در سیستم ثبت شده و آماده معامله و صدور فاکتور خواهید بود.",
                    fontSize = 13.sp,
                    color = colors.textSecondary,
                    lineHeight = 22.sp
                )

                HorizontalDivider(color = colors.border.copy(alpha = 0.6f))

                // Steps Preview List
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    FeatureCheckRow(
                        number = "۱",
                        title = "مشخصات واحد صنفی و مدیریت",
                        desc = "نام تجاری، شماره پروانه، آدرس، تلفن و نشان تجاری"
                    )
                    FeatureCheckRow(
                        number = "۲",
                        title = "تنظیمات مالی و درصد سود",
                        desc = "درصد سود پیش‌فرض، تنظیمات ارزش افزوده و عیار مبنا"
                    )
                    FeatureCheckRow(
                        number = "۳",
                        title = "ثبت موجودی اولیه (افتتاح دوره)",
                        desc = "موجودی ویترین، آبشده گاوصندوق، مسکوکات و نقدینگی"
                    )
                    FeatureCheckRow(
                        number = "۴",
                        title = "تأیید و صدور سند افتتاحیه",
                        desc = "بررسی نهایی، فعال‌سازی لایسنس و آغاز به کار سیستم"
                    )
                }
            }

            // Start Configuration Action
            DesktopActionButton(
                text = "شروع راه‌اندازی و ثبت مشخصات گالری",
                onClick = onStart,
                icon = Icons.AutoMirrored.Outlined.ArrowForward,
                isSecondary = false,
                height = 50.dp,
                modifier = Modifier.fillMaxWidth().testTag("intro-start-config-btn")
            )
        }
    }
}

@Composable
private fun FeatureCheckRow(
    number: String,
    title: String,
    desc: String
) {
    val colors = LocalGoldExColors.current

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(colors.goldContainer)
                .border(0.8.dp, colors.goldBorder, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = PersianNumberFormatter.toPersianDigits(number),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = colors.goldPrimary
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = title,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textMain
            )
            Text(
                text = desc,
                fontSize = 11.5.sp,
                color = colors.textMuted
            )
        }
    }
}
