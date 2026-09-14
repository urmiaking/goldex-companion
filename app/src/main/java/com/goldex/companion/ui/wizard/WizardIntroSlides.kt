package com.goldex.companion.ui.wizard

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.goldex.companion.R
import com.goldex.companion.ui.components.GoldButton
import com.goldex.companion.ui.components.LuxuryCard
import com.goldex.companion.ui.theme.LocalGoldExColors

data class IntroSlideData(
    val imageRes: Int,
    val topBadgeText: String,
    val unionBadgeTitle: String,
    val unionBadgeSubtitle: String,
    val pillCategory: String,
    val headline: String,
    val description: String
)

data class FeatureHighlight(
    val icon: ImageVector,
    val title: String,
    val description: String
)

/**
 * Top App Signature Bar for the Intro stage (without Skip button).
 */
@Composable
fun WizardIntroTopBar(
    modifier: Modifier = Modifier
) {
    val colors = LocalGoldExColors.current

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = colors.surface,
        border = BorderStroke(0.6.dp, colors.goldBorder.copy(alpha = 0.4f)),
        shape = RoundedCornerShape(16.dp),
        shadowElevation = if (colors.isDark) 0.dp else 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Brand Logo & Title (Right side in Persian RTL)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(11.dp))
                        .background(Color(0xFF0F141C))
                        .border(0.8.dp, colors.goldBorder, RoundedCornerShape(11.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_logo_raw),
                        contentDescription = "قیراط",
                        modifier = Modifier.size(28.dp)
                    )
                }

                Column {
                    Text(
                        text = "قیـراط",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Black,
                        color = colors.goldPrimary,
                        lineHeight = 22.sp
                    )
                    Text(
                        text = "QIRAT FINTECH",
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textMuted,
                        letterSpacing = 1.sp
                    )
                }
            }

            // Union / Standard Badge (Left side in Persian RTL)
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = colors.surfaceElevated,
                border = BorderStroke(0.6.dp, colors.goldBorder.copy(alpha = 0.35f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(
                        imageVector = WizardSecurity,
                        contentDescription = null,
                        tint = colors.goldPrimary,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = "صنف طلا و جواهر",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = colors.textSecondary
                    )
                }
            }
        }
    }
}

/**
 * Pure Scrollable Content for the Intro Step (Hero Slider, Indicator, Welcome Pitch, and Feature Cards).
 */
@Composable
fun WizardIntroContent(
    modifier: Modifier = Modifier
) {
    val colors = LocalGoldExColors.current
    var activeSlideIndex by remember { mutableIntStateOf(0) }
    val progressAnimatable = remember { Animatable(0f) }

    val slides = remember {
        listOf(
            IntroSlideData(
                imageRes = R.drawable.img_wizard_slide_1,
                topBadgeText = "مظنه زنده بازار و محاسبه آنی",
                unionBadgeTitle = "استاندارد عیار ۷۵۰ و فاکتور مؤدیان",
                unionBadgeSubtitle = "مورد تأیید اتحادیه صنف طلا و جواهر",
                pillCategory = "نسل چهارم محاسبات زرگری",
                headline = "خوش‌آمدید به دستیار هوشمند قیـراط",
                description = "سامانه جامع و ابری ویژه صنف طلا و جواهر، بنکداری و ویترین زرگری با استانداردهای رسمی اتحادیه و قانون پایانه‌های فروشگاهی."
            ),
            IntroSlideData(
                imageRes = R.drawable.img_wizard_slide_2,
                topBadgeText = "مدیریت گاوصندوق و تراز دوره",
                unionBadgeTitle = "انبارداری دقیق ویترین و بنکداری",
                unionBadgeSubtitle = "تفکیک مصنوعات، آبشده و مسکوکات",
                pillCategory = "خزانه و سرمایه در گردش",
                headline = "مدیریت یکپارچه دارایی‌ها و گاوصندوق",
                description = "پایش برخط وزن خالص مصنوعات، طلای آبشده انگ‌دار، شمش و بسته‌های سکه بانکی به همراه تراز لحظه‌ای ارزش روز سرمایه."
            ),
            IntroSlideData(
                imageRes = R.drawable.img_wizard_slide_3,
                topBadgeText = "فاکتور جامع دوطرفه و تهاتر",
                unionBadgeTitle = "معافیت قانونی اصل طلا از ارزش افزوده",
                unionBadgeSubtitle = "محاسبه مالیات صرفاً بر اجرت ساخت و سود",
                pillCategory = "حسابداری معین و تهاتر زرگری",
                headline = "صدور فاکتور رسمی و تسویه هوشمند",
                description = "محاسبه آنی تعویض طلای کهنه با نو، ثبت خودکار بدهکار و بستانکار در دفتر معین و آماده‌سازی خروجی چاپی با نشان گالری."
            )
        )
    }

    // Auto-advance Timer with Smooth Animation
    LaunchedEffect(activeSlideIndex) {
        progressAnimatable.snapTo(0f)
        progressAnimatable.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 4800, easing = LinearEasing)
        )
        activeSlideIndex = (activeSlideIndex + 1) % slides.size
    }

    // Gesture swipe support
    var dragAccumulator by remember { mutableFloatStateOf(0f) }

    val currentSlide = slides[activeSlideIndex]

    val features = remember {
        listOf(
            FeatureHighlight(
                icon = WizardCalculate,
                title = "ماشین‌حساب تخصصی طلا و تبدیلات عیار",
                description = "محاسبه دقیق قیمت قطعی با سود مصوب ۷٪، اجرت درصدی یا وزنی، کسر متعلقات و تبدیل فوری عیار ۷۵۰ به ۷۰۵ و ۹۹۵."
            ),
            FeatureHighlight(
                icon = WizardMonitoring,
                title = "تابلوی زنده مظنه بازار و حباب مسکوکات",
                description = "پایش آنلاین آبشده ۱۷ عیار، طلای ۱۸ عیار، انس جهانی و حباب تحلیلی تمام بهار، نیم، ربع و سکه گرمی."
            ),
            FeatureHighlight(
                icon = WizardReceiptLong,
                title = "صدور فاکتور رسمی و سامانه مؤدیان",
                description = "محاسبه خودکار ارزش افزوده صرفاً روی اجرت ساخت و سود (معافیت اصل طلا) با خروجی چاپی استاندارد اتحادیه."
            ),
            FeatureHighlight(
                icon = WizardSecurity,
                title = "دفترداری گاوصندوق، خزانه و تراز سرمایه",
                description = "تفکیک موجودی ویترین، طلای آبشده عیاردار، بسته‌های سکه بانکی و تراز دقیق سرمایه در گردش ریالی و وزنی."
            )
        )
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Stage (Image with Badges and Gesture Detection)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .pointerInput(slides.size) {
                    detectHorizontalDragGestures(
                        onDragStart = { dragAccumulator = 0f },
                        onDragEnd = {
                            val threshold = 40f
                            if (dragAccumulator < -threshold) {
                                // Swiped Left -> Forward in Persian RTL
                                activeSlideIndex = (activeSlideIndex + 1) % slides.size
                            } else if (dragAccumulator > threshold) {
                                // Swiped Right -> Backward in Persian RTL
                                activeSlideIndex = if (activeSlideIndex > 0) activeSlideIndex - 1 else slides.size - 1
                            }
                            dragAccumulator = 0f
                        },
                        onDragCancel = { dragAccumulator = 0f },
                        onHorizontalDrag = { _, dragAmount ->
                            dragAccumulator += dragAmount
                        }
                    )
                }
        ) {
            AnimatedContent(
                targetState = currentSlide,
                transitionSpec = { fadeIn(tween(320)) togetherWith fadeOut(tween(260)) },
                label = "slide_hero"
            ) { slide ->
                LuxuryCard(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(235.dp)
                    ) {
                        Image(
                            painter = painterResource(id = slide.imageRes),
                            contentDescription = slide.headline,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )

                        // Scrim gradient
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        listOf(
                                            Color.Transparent,
                                            Color(0x9910141E),
                                            Color(0xEB10141E)
                                        )
                                    )
                                )
                        )

                        // Top Floating Badge: Live rate / feature indicator
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color(0xCC141B2B),
                            border = BorderStroke(0.6.dp, Color(0x66DFB35A)),
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
                                    text = slide.topBadgeText,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }

                        // Bottom Floating Badge: Union Standard
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xF21C2436),
                            border = BorderStroke(0.6.dp, Color(0x55DFB35A)),
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(colors.goldContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = WizardCheckCircle,
                                        contentDescription = null,
                                        tint = colors.goldPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = slide.unionBadgeTitle,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = slide.unionBadgeSubtitle,
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = colors.textSecondary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Animated Slider Indicators with Progress Filling Timer
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val inactiveTrackColor = if (colors.isDark) Color(0x3DFBBF24) else Color(0x3310141E)
            val borderColor = colors.goldBorder.copy(alpha = if (colors.isDark) 0.5f else 0.35f)

            slides.indices.forEach { index ->
                val isActive = index == activeSlideIndex
                val dotWidth by animateDpAsState(
                    targetValue = if (isActive) 34.dp else 12.dp,
                    animationSpec = tween(280),
                    label = "indicator_width"
                )

                Box(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .height(7.dp)
                        .width(dotWidth)
                        .clip(RoundedCornerShape(3.5.dp))
                        .background(inactiveTrackColor)
                        .border(0.6.dp, borderColor, RoundedCornerShape(3.5.dp))
                        .clickable { activeSlideIndex = index }
                ) {
                    if (isActive) {
                        // Filling progress inside active indicator
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(progressAnimatable.value)
                                .clip(RoundedCornerShape(3.5.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(
                                            colors.goldSecondary,
                                            colors.goldPrimary
                                        )
                                    )
                                )
                        )
                    } else if (index < activeSlideIndex) {
                        // Fully completed previous indicator
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(3.5.dp))
                                .background(colors.goldPrimary.copy(alpha = 0.6f))
                        )
                    }
                }
            }
        }

        // Welcoming Pitch Typography
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = colors.goldContainer,
                border = BorderStroke(0.6.dp, colors.goldBorder.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = WizardAutoAwesome,
                        contentDescription = null,
                        tint = colors.goldPrimary,
                        modifier = Modifier.size(14.dp)
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
                fontWeight = FontWeight.ExtraBold,
                color = colors.textMain,
                textAlign = TextAlign.Center
            )

            Text(
                text = currentSlide.description,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Normal,
                color = colors.textSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        }

        // Section Title: Key App Features
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp, start = 4.dp, end = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(4.dp, 16.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(colors.goldPrimary)
                )
                Text(
                    text = "امکانات جامع و تخصصی قیـراط",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textMain
                )
            }
            Text(
                text = "۴ بخش کلیدی",
                fontSize = 11.sp,
                color = colors.goldPrimary,
                fontWeight = FontWeight.SemiBold
            )
        }

        // 4 Feature Highlight Cards
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            features.forEach { feature ->
                LuxuryCard(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(colors.goldContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = feature.icon,
                                contentDescription = null,
                                tint = colors.goldPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = feature.title,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textMain
                            )
                            Text(
                                text = feature.description,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Normal,
                                color = colors.textSecondary,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}

/**
 * Standalone Intro Screen with TopBar, Content, and CTA Footer.
 */
@Composable
fun WizardIntroSlides(
    onStartWizard: () -> Unit,
    onSkipAsGuest: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        WizardIntroTopBar(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            WizardIntroContent()
        }

        // Sticky Bottom CTA Actions (RTL: Guest on Right, Start on Left)
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = LocalGoldExColors.current.surface,
            border = BorderStroke(0.6.dp, LocalGoldExColors.current.border),
            shadowElevation = 8.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Secondary / Guest: Right side in Persian RTL (first in Row)
                GoldButton(
                    text = "ورود مهمان",
                    isSecondary = true,
                    onClick = onSkipAsGuest,
                    modifier = Modifier.weight(1f)
                )

                // Primary / Start: Left side in Persian RTL (second in Row)
                GoldButton(
                    text = "شروع و ثبت مشخصات",
                    trailingIcon = WizardArrowLeft,
                    onClick = onStartWizard,
                    modifier = Modifier.weight(1.8f)
                )
            }
        }
    }
}
