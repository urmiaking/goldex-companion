package com.goldex.companion.ui.wizard

import androidx.compose.animation.AnimatedContent
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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CandlestickChart
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Verified
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.goldex.companion.R
import com.goldex.companion.ui.components.GoldButton
import com.goldex.companion.ui.components.LuxuryCard
import com.goldex.companion.ui.theme.ButtonCornerRadius
import com.goldex.companion.ui.theme.LocalGoldExColors

data class IntroSlideData(
    val imageRes: Int,
    val topBadgeText: String,
    val unionBadgeTitle: String,
    val unionBadgeSubtitle: String,
    val pillCategory: String,
    val headline: String,
    val description: String,
    val feature1Icon: ImageVector,
    val feature1Title: String,
    val feature1Description: String,
    val feature2Icon: ImageVector,
    val feature2Title: String,
    val feature2Description: String
)

@Composable
fun WizardIntroSlides(
    onStartWizard: () -> Unit,
    onSkipAsGuest: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalGoldExColors.current
    var activeSlideIndex by remember { mutableIntStateOf(0) }

    val slides = remember {
        listOf(
            IntroSlideData(
                imageRes = R.drawable.img_wizard_slide_1,
                topBadgeText = "مظنه زنده بازار و محاسبه آنی",
                unionBadgeTitle = "استاندارد عیار ۷۵۰ و فاکتور مؤدیان",
                unionBadgeSubtitle = "مورد تأیید اتحادیه صنف طلا و جواهر",
                pillCategory = "نسل چهارم محاسبات زرگری",
                headline = "دستیار جامع و هوشمند محاسبات طلا",
                description = "محاسبه آنی فاکتور رسمی، تابلوی زنده مظنه و حباب سکه، و حسابداری پیشرفته دفاتر معین با دقت عیار ۷۵۰ صنف زرگری.",
                feature1Icon = Icons.Default.ReceiptLong,
                feature1Title = "صدور فاکتور رسمی اتحادیه و استاندارد مودیان",
                feature1Description = "محاسبه خودکار سود ۷٪، اجرت ساخت و ثبت برخط سامانه تجارت",
                feature2Icon = Icons.Default.CandlestickChart,
                feature2Title = "اتصال به تابلوی زنده مظنه و حباب سکه",
                feature2Description = "پایش لحظه‌ای آبشده ۱۷ عیار، انس جهانی و حباب طلا و سکه"
            ),
            IntroSlideData(
                imageRes = R.drawable.img_wizard_slide_2,
                topBadgeText = "مدیریت گاوصندوق و تراز دوره",
                unionBadgeTitle = "انبارداری دقیق ویترین و بنکداری",
                unionBadgeSubtitle = "تفکیک مصنوعات، آبشده و مسکوکات",
                pillCategory = "خزانه و سرمایه در گردش",
                headline = "مدیریت جامع موجودی و دارایی‌های طلا",
                description = "پایش دقیق وزن خالص مصنوعات ویترین، طلای آبشده عیاردار، شمش و بسته‌های سکه بانکی به همراه ارزش روز دارایی‌ها.",
                feature1Icon = Icons.Default.Storefront,
                feature1Title = "دفترداری و انبارداری دقیق گاوصندوق",
                feature1Description = "شمارش لحظه‌ای بار سکه بانکی و تفکیک موجودی ریالی و وزنی",
                feature2Icon = Icons.Default.Security,
                feature2Title = "تراز لحظه‌ای سرمایه در گردش",
                feature2Description = "محاسبه ارزش تخمینی دارایی‌ها بر پایه نرخ مظنه زنده بازار"
            ),
            IntroSlideData(
                imageRes = R.drawable.img_wizard_slide_3,
                topBadgeText = "فاکتور جامع دوطرفه و تهاتر",
                unionBadgeTitle = "معافیت قانونی اصل طلا از ارزش افزوده",
                unionBadgeSubtitle = "محاسبه مالیات صرفاً بر اجرت ساخت و سود",
                pillCategory = "حسابداری معین و تهاتر زرگری",
                headline = "صدور فاکتور هوشمند و تسویه طلایی",
                description = "تعویض طلای کهنه با نو، ثبت خودکار بدهکار و بستانکار در دفتر معین مشتریان و چاپ فاکتور رسمی با سربرگ گالری.",
                feature1Icon = Icons.Default.ReceiptLong,
                feature1Title = "تهاتر هوشمند طلا با طلا و تسویه ریالی",
                feature1Description = "محاسبه دقیق مازاد وزنی یا ریالی فاکتور خرید و فروش همزمان",
                feature2Icon = Icons.Default.CheckCircle,
                feature2Title = "خروجی سند رسمی و پیش‌فاکتور چاپی",
                feature2Description = "آماده‌سازی سند چاپی با نشان تجاری، بارکد و استاندارد اتحادیه"
            )
        )
    }

    val currentSlide = slides[activeSlideIndex]

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Bar: App Signature (Right) & Skip Button (Left)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Brand Logo & Title
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF0F141C))
                        .border(0.8.dp, colors.goldBorder, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_logo_raw),
                        contentDescription = "قیراط",
                        modifier = Modifier.size(30.dp)
                    )
                }

                Column {
                    Text(
                        text = "قیـراط",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = colors.goldPrimary,
                        lineHeight = 22.sp
                    )
                    Text(
                        text = "QIRAT FINTECH",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textMuted,
                        letterSpacing = 1.sp
                    )
                }
            }

            // Skip Button (Secondary Action)
            Surface(
                onClick = onSkipAsGuest,
                shape = RoundedCornerShape(ButtonCornerRadius),
                color = colors.surfaceElevated,
                border = BorderStroke(0.6.dp, colors.goldBorder.copy(alpha = 0.5f)),
                shadowElevation = if (colors.isDark) 0.dp else 1.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "رد کردن",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textMain
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = null,
                        tint = colors.goldPrimary,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
        }

        // Hero Stage (Image with Badges)
        AnimatedContent(
            targetState = currentSlide,
            transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(250)) },
            label = "slide_hero"
        ) { slide ->
            LuxuryCard(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(0.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
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
                                        Color(0xE610141E)
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
                                    imageVector = Icons.Default.Verified,
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

        // Pagination Dots Indicator
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            slides.indices.forEach { index ->
                val isActive = index == activeSlideIndex
                val dotWidth by animateDpAsState(
                    targetValue = if (isActive) 26.dp else 8.dp,
                    animationSpec = tween(300),
                    label = "dot_width"
                )

                Box(
                    modifier = Modifier
                        .padding(horizontal = 3.dp)
                        .height(8.dp)
                        .width(dotWidth)
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (isActive) colors.goldPrimary else colors.surfaceElevated)
                        .clickable { activeSlideIndex = index }
                )
            }
        }

        // Pitch Typography
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
                        imageVector = Icons.Default.AutoAwesome,
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
                fontSize = 20.sp,
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

        // Feature Highlights
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
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
                            imageVector = currentSlide.feature1Icon,
                            contentDescription = null,
                            tint = colors.goldPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = currentSlide.feature1Title,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textMain
                        )
                        Text(
                            text = currentSlide.feature1Description,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Normal,
                            color = colors.textSecondary,
                            lineHeight = 16.sp
                        )
                    }
                }
            }

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
                            imageVector = currentSlide.feature2Icon,
                            contentDescription = null,
                            tint = colors.goldPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = currentSlide.feature2Title,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textMain
                        )
                        Text(
                            text = currentSlide.feature2Description,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Normal,
                            color = colors.textSecondary,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Bottom CTA Actions
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            GoldButton(
                text = "شروع و ثبت مشخصات",
                icon = Icons.AutoMirrored.Filled.ArrowBack,
                onClick = onStartWizard,
                modifier = Modifier.fillMaxWidth()
            )

            GoldButton(
                text = "ورود به عنوان مهمان",
                isSecondary = true,
                onClick = onSkipAsGuest,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
