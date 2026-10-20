package com.goldex.companion.desktop.wizard

import androidx.compose.animation.*
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.animation.core.Animatable
import com.goldex.companion.ui.theme.LuxuryMotion
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.goldex.companion.desktop.ui.controls.DesktopIconButton
import com.goldex.companion.ui.theme.LocalGoldExColors
import kotlinx.coroutines.delay

private data class DesktopIntroSlide(
    val imagePath: String,
    val topBadgeText: String,
    val unionBadgeTitle: String,
    val unionBadgeSubtitle: String,
    val pillCategory: String,
    val headline: String,
    val description: String
)

private val introSlides = listOf(
    DesktopIntroSlide(
        imagePath = "drawable/img_wizard_slide_1.jpg",
        topBadgeText = "مظنه زنده بازار و محاسبه آنی",
        unionBadgeTitle = "استاندارد عیار ۷۵۰ و فاکتور رسمی",
        unionBadgeSubtitle = "مورد تأیید اتحادیه صنف طلا و جواهر",
        pillCategory = "نسل چهارم محاسبات زرگری",
        headline = "خوش‌آمدید به دستیار هوشمند قیـراط",
        description = "سامانه جامع و ابری ویژه صنف طلا و جواهر، بنکداری و ویترین زرگری با استانداردهای رسمی اتحادیه و قانون پایانه‌های فروشگاهی."
    ),
    DesktopIntroSlide(
        imagePath = "drawable/img_wizard_slide_2.jpg",
        topBadgeText = "مدیریت گاوصندوق و تراز دوره",
        unionBadgeTitle = "انبارداری دقیق ویترین و بنکداری",
        unionBadgeSubtitle = "تفکیک مصنوعات، آبشده و مسکوکات",
        pillCategory = "خزانه و سرمایه در گردش",
        headline = "مدیریت یکپارچه دارایی‌ها و گاوصندوق",
        description = "پایش برخط وزن خالص مصنوعات، طلای آبشده انگ‌دار، شمش و بسته‌های سکه بانکی به همراه تراز لحظه‌ای ارزش روز سرمایه."
    ),
    DesktopIntroSlide(
        imagePath = "drawable/img_wizard_slide_3.jpg",
        topBadgeText = "فاکتور جامع دوطرفه و تهاتر",
        unionBadgeTitle = "معافیت قانونی اصل طلا از ارزش افزوده",
        unionBadgeSubtitle = "محاسبه مالیات صرفاً بر اجرت ساخت و سود",
        pillCategory = "حسابداری معین و تهاتر زرگری",
        headline = "صدور فاکتور رسمی و تسویه هوشمند",
        description = "محاسبه آنی تعویض طلای کهنه با نو، ثبت خودکار بدهکار و بستانکار در دفتر معین و آماده‌سازی خروجی چاپی با نشان گالری."
    )
)

private data class FeatureHighlight(
    val icon: ImageVector,
    val title: String,
    val description: String
)

private val featureHighlights = listOf(
    FeatureHighlight(
        icon = Icons.Outlined.Calculate,
        title = "ماشین‌حساب تخصصی طلا و تبدیلات عیار",
        description = "محاسبه دقیق قیمت قطعی با سود مصوب ۷٪، اجرت درصدی یا وزنی، کسر متعلقات و تبدیل فوری عیار ۷۵۰ به ۷۰۵ و ۹۹۵."
    ),
    FeatureHighlight(
        icon = Icons.Outlined.ShowChart,
        title = "تابلوی زنده مظنه بازار و حباب مسکوکات",
        description = "پایش آنلاین آبشده ۱۷ عیار، طلای ۱۸ عیار، انس جهانی و حباب تحلیلی تمام بهار، نیم، ربع و سکه گرمی."
    ),
    FeatureHighlight(
        icon = Icons.Outlined.ReceiptLong,
        title = "صدور فاکتور رسمی و تهاتر طلا",
        description = "محاسبه خودکار ارزش افزوده صرفاً روی اجرت ساخت و سود (معافیت اصل طلا) با خروجی چاپی استاندارد اتحادیه."
    ),
    FeatureHighlight(
        icon = Icons.Outlined.Inventory2,
        title = "دفترداری گاوصندوق، خزانه و تراز سرمایه",
        description = "تفکیک موجودی ویترین، طلای آبشده عیاردار، بسته‌های سکه بانکی و تراز دقیق سرمایه در گردش ریالی و وزنی."
    )
)

@Composable
fun DesktopWizardIntroPane(
    onStartConfiguration: () -> Unit,
    modifier: Modifier = Modifier,
    autoAdvance: Boolean = true
) {
    var activeSlideIndex by remember { mutableStateOf(0) }
    var slideDirection by remember { mutableStateOf(1) }

    // Auto-advance Timer
    val reduceMotion = com.goldex.companion.desktop.ui.controls.LocalDesktopReduceMotion.current
    if (autoAdvance && !reduceMotion) {
        LaunchedEffect(activeSlideIndex, autoAdvance) {
            delay(5000)
            slideDirection = 1
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
                // Right Column in RTL: Interactive Hero Showcase Card with High-Res Imagery
                IntroHeroCard(
                    slide = activeSlide,
                    slideIndex = activeSlideIndex,
                    totalSlides = introSlides.size,
                    slideDirection = slideDirection,
                                        onPrev = {
                        slideDirection = -1
                        activeSlideIndex = if (activeSlideIndex > 0) activeSlideIndex - 1 else introSlides.size - 1
                    },
                    onNext = {
                        slideDirection = 1
                        activeSlideIndex = (activeSlideIndex + 1) % introSlides.size
                    },
                    onSelectSlide = { target ->
                        slideDirection = if (target >= activeSlideIndex) 1 else -1
                        activeSlideIndex = target
                    },
                    modifier = Modifier.weight(1.05f).fillMaxHeight()
                )

                // Left Column in RTL: 4 Pillars & Core Capabilities
                IntroFeaturesSummaryCard(
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
                    slideDirection = slideDirection,
                                        onPrev = {
                        slideDirection = -1
                        activeSlideIndex = if (activeSlideIndex > 0) activeSlideIndex - 1 else introSlides.size - 1
                    },
                    onNext = {
                        slideDirection = 1
                        activeSlideIndex = (activeSlideIndex + 1) % introSlides.size
                    },
                    onSelectSlide = { target ->
                        slideDirection = if (target >= activeSlideIndex) 1 else -1
                        activeSlideIndex = target
                    },
                    modifier = Modifier.fillMaxWidth().height(440.dp)
                )

                IntroFeaturesSummaryCard(
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
    slideDirection: Int,
        onPrev: () -> Unit,
    onNext: () -> Unit,
    onSelectSlide: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalGoldExColors.current

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = colors.surface,
        border = BorderStroke(0.8.dp, colors.goldBorder),
        shadowElevation = if (colors.isDark) 0.dp else 4.dp,
        modifier = modifier.testTag("intro-hero-card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // 1. Photo Stage with Dark Scrim & Badges
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .pointerInput(Unit) {
                        var totalDrag = 0f
                        detectHorizontalDragGestures(
                            onDragStart = { totalDrag = 0f },
                            onHorizontalDrag = { change, dragAmount ->
                                change.consume()
                                totalDrag += dragAmount
                            },
                            onDragEnd = {
                                if (totalDrag > 35f) {
                                    onPrev()
                                } else if (totalDrag < -35f) {
                                    onNext()
                                }
                            }
                        )
                    }
            ) {
                AnimatedContent(
                    targetState = slide,
                    transitionSpec = {
                        if (slideDirection >= 0) {
                            (slideInHorizontally(tween(350, easing = LuxuryMotion.StandardEasing)) { -it } + fadeIn(tween(280)))
                                .togetherWith(slideOutHorizontally(tween(300, easing = LuxuryMotion.AccelerationEasing)) { it } + fadeOut(tween(220)))
                        } else {
                            (slideInHorizontally(tween(350, easing = LuxuryMotion.StandardEasing)) { it } + fadeIn(tween(280)))
                                .togetherWith(slideOutHorizontally(tween(300, easing = LuxuryMotion.AccelerationEasing)) { -it } + fadeOut(tween(220)))
                        }
                    },
                    label = "intro_slide_image",
                    modifier = Modifier.fillMaxSize()
                ) { currentSlide ->
                    Box(modifier = Modifier.fillMaxSize()) {
                        Image(
                            painter = painterResource(currentSlide.imagePath),
                            contentDescription = currentSlide.headline,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )

                        // Rich Scrim Gradient for text contrast over photos
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        listOf(
                                            Color(0x330A0E17),
                                            Color(0x770A0E17),
                                            Color(0xE00A0E17)
                                        )
                                    )
                                )
                        )

                        // Top Floating Badge: Live rate indicator
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color(0xD9101624),
                            border = BorderStroke(0.7.dp, colors.goldBorder.copy(alpha = 0.5f)),
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(colors.profitGreen)
                                )
                                Text(
                                    text = currentSlide.topBadgeText,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }

                        // Bottom Floating Badge: Union Standard
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xF2141C2E),
                            border = BorderStroke(0.6.dp, colors.goldBorder.copy(alpha = 0.45f)),
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(26.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(colors.goldContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.CheckCircle,
                                        contentDescription = null,
                                        tint = colors.goldPrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = currentSlide.unionBadgeTitle,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = currentSlide.unionBadgeSubtitle,
                                        fontSize = 9.5.sp,
                                        color = Color.White.copy(alpha = 0.75f)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // 2. Animated Slide Indicators & Navigation Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Indicator Bars
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(totalSlides) { idx ->
                        val isSelected = idx == slideIndex
                        val barWidth by animateDpAsState(
                            targetValue = if (isSelected) 36.dp else 12.dp,
                            animationSpec = tween(280),
                            label = "intro_indicator_width"
                        )

                        Box(
                            modifier = Modifier
                                .height(6.dp)
                                .width(barWidth)
                                .clip(RoundedCornerShape(3.dp))
                                .background(
                                    if (isSelected) colors.goldPrimary else colors.border.copy(alpha = 0.6f)
                                )
                                .clickable { onSelectSlide(idx) }
                        )
                    }
                }

                // Prev/Next Navigation Buttons
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    DesktopIconButton(
                        onClick = onPrev,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ChevronRight,
                            contentDescription = "اسلاید قبلی",
                            tint = colors.textSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    DesktopIconButton(
                        onClick = onNext,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ChevronLeft,
                            contentDescription = "اسلاید بعدی",
                            tint = colors.textSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // 3. Welcoming Pitch Typography (Theme-Aware Colors!)
            AnimatedContent(
                targetState = slide,
                transitionSpec = {
                    (fadeIn(tween(280)) + slideInVertically(tween(300)) { it / 6 })
                        .togetherWith(fadeOut(tween(180)) + slideOutVertically(tween(200)) { -it / 6 })
                },
                label = "intro_typography_animation"
            ) { currentSlide ->
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = colors.goldContainer,
                        border = BorderStroke(0.6.dp, colors.goldBorder.copy(alpha = 0.4f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.AutoAwesome,
                                contentDescription = null,
                                tint = colors.goldPrimary,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = currentSlide.pillCategory,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.goldPrimary
                            )
                        }
                    }

                    Text(
                        text = currentSlide.headline,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Black,
                        color = colors.textMain
                    )

                    Text(
                        text = currentSlide.description,
                        fontSize = 12.5.sp,
                        color = colors.textSecondary,
                        lineHeight = 21.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun IntroFeaturesSummaryCard(
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
                .padding(20.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                // Header
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
                        text = "امکانات جامع و تخصصی قیـراط",
                        fontSize = 16.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textMain
                    )
                }

                Text(
                    text = "آماده‌سازی نرم‌افزار در ۴ مرحله ساده برای شروع امن معاملات و صدور فاکتورهای رسمی طلا:",
                    fontSize = 12.5.sp,
                    color = colors.textSecondary,
                    lineHeight = 20.sp
                )

                HorizontalDivider(color = colors.border.copy(alpha = 0.6f))

                // 4 Pillars
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    featureHighlights.forEach { feature ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = colors.surfaceElevated,
                            border = BorderStroke(0.5.dp, colors.border.copy(alpha = 0.7f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(colors.goldContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = feature.icon,
                                        contentDescription = null,
                                        tint = colors.goldPrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        text = feature.title,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.textMain
                                    )
                                    Text(
                                        text = feature.description,
                                        fontSize = 11.sp,
                                        color = colors.textMuted,
                                        lineHeight = 16.5.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Database and Security Info Callout
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = colors.goldPrimary.copy(alpha = 0.08f),
                border = BorderStroke(0.6.dp, colors.goldBorder.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Storage,
                        contentDescription = null,
                        tint = colors.goldPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "اطلاعات مالی در پایگاه‌داده ساختاریافته SQLite سیستم شما ذخیره شده و بدون نیاز به اینترنت با حداکثر سرعت در دسترس است.",
                        fontSize = 11.sp,
                        color = colors.textSecondary,
                        lineHeight = 17.sp
                    )
                }
            }
        }
    }
}
