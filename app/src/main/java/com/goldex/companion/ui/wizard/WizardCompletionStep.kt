package com.goldex.companion.ui.wizard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.goldex.companion.model.PersianNumberFormatter
import com.goldex.companion.ui.calculator.AppTab
import com.goldex.companion.ui.components.GoldButton
import com.goldex.companion.ui.components.LuxuryCard
import com.goldex.companion.ui.components.rememberProfileAssetBitmap
import com.goldex.companion.ui.theme.LocalGoldExColors
import kotlin.math.cos
import kotlin.math.sin

/**
 * Animated Celebration Confetti particle system:
 * Shoots upwards/outwards from the trophy, arcs and falls down under gravity, and smoothly fades out.
 */
@Composable
fun ConfettiCelebration(
    modifier: Modifier = Modifier
) {
    val progress = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 2800, easing = LinearEasing)
        )
    }

    if (progress.value >= 1f) {
        // Animation finished: particles have fallen and faded out completely
        return
    }

    // Palette of vibrant celebratory colors
    val confettiColors = remember {
        listOf(
            Color(0xFFDFB35A), // Sovereign Gold
            Color(0xFFF59E0B), // Warm Amber
            Color(0xFF10B981), // Emerald Green
            Color(0xFF00C853), // Vivid Green
            Color(0xFFEF4444), // Ruby Red
            Color(0xFFF43F5E), // Rose Pink
            Color(0xFF06B6D4), // Cyan
            Color(0xFF3B82F6), // Blue
            Color(0xFF8B5CF6)  // Violet
        )
    }

    val particles = remember {
        val list = mutableListOf<BurstConfettiParticle>()
        val count = 55
        for (i in 0 until count) {
            // Angle distributed predominantly in upper half (-165 deg to -15 deg)
            val angleDeg = -165f + (i.toFloat() / (count - 1)) * 150f + ((i % 5) - 2) * 4f
            val angleRad = Math.toRadians(angleDeg.toDouble()).toFloat()
            val speed = 230f + (i % 8) * 35f
            val vx = speed * cos(angleRad)
            val vy = speed * sin(angleRad) // negative = moving upwards

            val color = confettiColors[i % confettiColors.size]
            val isRect = i % 3 != 0
            val particleWidth = if (isRect) 9f + (i % 4) * 3f else 8f + (i % 3) * 3f
            val particleHeight = if (isRect) 5f + (i % 3) * 2f else particleWidth
            val rotationSpeed = 360f + (i % 6) * 120f
            val initialRotation = (i * 37f) % 360f

            list.add(
                BurstConfettiParticle(
                    vx = vx,
                    vy = vy,
                    color = color,
                    isRect = isRect,
                    width = particleWidth,
                    height = particleHeight,
                    rotationSpeed = rotationSpeed,
                    initialRotation = initialRotation
                )
            )
        }
        list
    }

    val t = progress.value
    val elapsed = t * 2.5f // time in seconds
    val gravity = 680f // gravity acceleration in px/s²
    // Fade out during the final 35% of the animation
    val alpha = if (t < 0.65f) 1f else ((1f - t) / 0.35f).coerceIn(0f, 1f)

    Canvas(modifier = modifier) {
        val centerX = size.width / 2f
        val centerY = size.height / 2f - 10f

        particles.forEach { p ->
            val px = centerX + (p.vx * elapsed)
            val py = centerY + (p.vy * elapsed) + (0.5f * gravity * elapsed * elapsed)
            val rotation = p.initialRotation + (t * p.rotationSpeed)

            rotate(degrees = rotation, pivot = Offset(px, py)) {
                if (p.isRect) {
                    drawRect(
                        color = p.color.copy(alpha = alpha),
                        topLeft = Offset(px - p.width / 2f, py - p.height / 2f),
                        size = Size(p.width, p.height)
                    )
                } else {
                    drawCircle(
                        color = p.color.copy(alpha = alpha),
                        radius = p.width / 2f,
                        center = Offset(px, py)
                    )
                }
            }
        }
    }
}

private data class BurstConfettiParticle(
    val vx: Float,
    val vy: Float,
    val color: Color,
    val isRect: Boolean,
    val width: Float,
    val height: Float,
    val rotationSpeed: Float,
    val initialRotation: Float
)

/**
 * Pure Scrollable Content for Step 4: Completion and Quick Action Suggestions.
 */
@Composable
fun WizardCompletionContent(
    profileState: WizardProfileState,
    financialState: WizardFinancialState,
    licenseState: WizardLicenseState = WizardLicenseState(),
    isValidating: Boolean = false,
    validationError: String? = null,
    onLicenseStateChange: (WizardLicenseState) -> Unit = {},
    onEnterApp: (AppTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalGoldExColors.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Celebration & Congratulation Emblem with Animated Confetti
        LuxuryCard(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(20.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Golden Trophy Emblem with Confetti Burst
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Confetti Particles Background
                    ConfettiCelebration(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(4.dp)
                    )

                    // Central Trophy with Golden Border & Glow
                    Box(
                        modifier = Modifier.size(78.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(
                                    Brush.linearGradient(
                                        listOf(
                                            colors.goldSecondary,
                                            colors.goldPrimary
                                        )
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(colors.surface),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = WizardTrophy,
                                    contentDescription = null,
                                    tint = colors.goldPrimary,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }

                        // Floating checkmark badge
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(colors.profitGreen)
                                .border(1.5.dp, colors.surface, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                        }

                        // Floating spark
                        Icon(
                            imageVector = WizardAutoAwesome,
                            contentDescription = null,
                            tint = colors.goldPrimary,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .size(18.dp)
                        )
                    }
                }

                Text(
                    text = "راه‌اندازی با موفقیت انجام شد!",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    color = colors.textMain
                )

                Text(
                    text = "سامانه بنکداری ابری شما با کلیه فرمول‌های استاندارد، تابلوی مظنه زنده و تنظیمات عیار آماده شروع کار است.",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal,
                    color = colors.textSecondary,
                    textAlign = TextAlign.Center,
                    lineHeight = 19.sp,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }
        }

        // License / Trial Selection Card
        LuxuryCard(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "انتخاب اشتراک و فعال‌سازی امکانات",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textMain
            )

            // Option 1: 14 Days Free Trial
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onLicenseStateChange(licenseState.copy(choice = WizardLicenseChoice.TRIAL))
                    },
                shape = RoundedCornerShape(12.dp),
                color = if (licenseState.choice == WizardLicenseChoice.TRIAL) {
                    colors.goldPrimary.copy(alpha = 0.12f)
                } else colors.surfaceElevated,
                border = BorderStroke(
                    1.dp,
                    if (licenseState.choice == WizardLicenseChoice.TRIAL) {
                        colors.goldPrimary
                    } else colors.border
                )
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    RadioButton(
                        selected = licenseState.choice == WizardLicenseChoice.TRIAL,
                        onClick = {
                            onLicenseStateChange(licenseState.copy(choice = WizardLicenseChoice.TRIAL))
                        },
                        colors = RadioButtonDefaults.colors(
                            selectedColor = colors.goldPrimary
                        )
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "🎁 فعال‌سازی مهلت تست ۱۴ روزه رایگان (پیشنهادی)",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textMain
                        )
                        Text(
                            text = "دسترسی نامحدود به صدور فاکتور و معین؛ بدون نیاز به پرداخت",
                            fontSize = 10.5.sp,
                            color = colors.textMuted
                        )
                    }
                }
            }

            // Option 2: Enter Permanent License Code
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onLicenseStateChange(licenseState.copy(choice = WizardLicenseChoice.CODE))
                    },
                shape = RoundedCornerShape(12.dp),
                color = if (licenseState.choice == WizardLicenseChoice.CODE) {
                    colors.goldPrimary.copy(alpha = 0.12f)
                } else colors.surfaceElevated,
                border = BorderStroke(
                    1.dp,
                    if (licenseState.choice == WizardLicenseChoice.CODE) {
                        colors.goldPrimary
                    } else colors.border
                )
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        RadioButton(
                            selected = licenseState.choice == WizardLicenseChoice.CODE,
                            onClick = {
                                onLicenseStateChange(licenseState.copy(choice = WizardLicenseChoice.CODE))
                            },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = colors.goldPrimary
                            )
                        )

                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = "🔑 کد فعال‌سازی دائمی دارم",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textMain
                            )
                            Text(
                                text = "اتصال کد لایسنس خریداری شده به سخت‌افزار این دستگاه",
                                fontSize = 10.5.sp,
                                color = colors.textMuted
                            )
                        }
                    }

                    if (licenseState.choice == WizardLicenseChoice.CODE) {
                        OutlinedTextField(
                            value = licenseState.licenseCode,
                            onValueChange = {
                                onLicenseStateChange(licenseState.copy(licenseCode = it.uppercase()))
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp),
                            placeholder = {
                                Text(
                                    text = "مثال: QIR-91C7-2M4P",
                                    fontSize = 11.5.sp,
                                    color = colors.textMuted
                                )
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Characters,
                                imeAction = ImeAction.Done
                            ),
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = colors.goldPrimary,
                                unfocusedBorderColor = colors.goldBorder.copy(alpha = 0.5f),
                                focusedContainerColor = colors.surface,
                                unfocusedContainerColor = colors.surface
                            )
                        )
                    }
                }
            }

            // Validation Error Alert Banner
            AnimatedVisibility(visible = !validationError.isNullOrBlank()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    color = colors.errorRed.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, colors.errorRed.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = WizardErrorOutline,
                            contentDescription = null,
                            tint = colors.errorRed,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = validationError ?: "",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.errorRed
                        )
                    }
                }
            }
        }

        // Configuration Summary Card
        LuxuryCard(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Gallery identity row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val logoBitmap = rememberProfileAssetBitmap(profileState.logoUri)
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(colors.goldContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        if (logoBitmap != null) {
                            Image(
                                bitmap = logoBitmap,
                                contentDescription = profileState.galleryName,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(4.dp),
                                contentScale = ContentScale.Fit
                            )
                        } else {
                            Icon(
                                imageVector = WizardStorefront,
                                contentDescription = null,
                                tint = colors.goldPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = profileState.galleryName.ifBlank { "گالری طلا و جواهر" },
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textMain
                            )
                            Icon(
                                imageVector = WizardCheckCircle,
                                contentDescription = null,
                                tint = colors.goldPrimary,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                        Text(
                            text = "پروانه صنف: ${PersianNumberFormatter.toPersianDigits(profileState.unionCode)} • متصدی: ${profileState.managerName}",
                            fontSize = 10.5.sp,
                            color = colors.textMuted
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = colors.profitGreen.copy(alpha = 0.15f),
                    border = BorderStroke(0.6.dp, colors.profitGreen.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(colors.profitGreen)
                        )
                        Text(
                            text = "آماده کار",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.profitGreen
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(0.6.dp)
                    .background(colors.border)
            )

            // 2 Column Configuration Specs
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    color = colors.surfaceElevated,
                    border = BorderStroke(0.6.dp, colors.border)
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "عیار مبنای استاندارد",
                            fontSize = 10.5.sp,
                            color = colors.textMuted
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "۷۵۰",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.goldPrimary
                            )
                            Text(
                                text = "(۱۸ عیار)",
                                fontSize = 10.sp,
                                color = colors.textSecondary
                            )
                        }
                    }
                }

                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    color = colors.surfaceElevated,
                    border = BorderStroke(0.6.dp, colors.border)
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "سود پیش‌فرض بنکداری",
                            fontSize = 10.5.sp,
                            color = colors.textMuted
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "${PersianNumberFormatter.toPersianDigits(financialState.profitPercent)}٪",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textMain
                            )
                            Text(
                                text = "مصوب اتحادیه",
                                fontSize = 10.sp,
                                color = colors.profitGreen
                            )
                        }
                    }
                }
            }
        }

        // Quick Start Next Steps (اقدامات پیشنهادی قیراط)
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "پیشنهاد قیراط برای شروع کار",
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textMain
                )
                Text(
                    text = "اقدامات سریع",
                    fontSize = 11.sp,
                    color = colors.goldPrimary
                )
            }

            val suggestions = listOf(
                SuggestedAction(
                    icon = WizardReceiptLong,
                    title = "صدور اولین فاکتور طلا و جواهر",
                    description = "محاسبه لحظه‌ای مظنه، کارمزد، وزن و متعلقات",
                    targetTab = AppTab.INVOICES
                ),
                SuggestedAction(
                    icon = WizardMonitoring,
                    title = "مشاهده تابلوی زنده مظنه و طلا",
                    description = "نرخ آنلاین طلای ۱۸، آبشده، سکه و انس جهانی",
                    targetTab = AppTab.RATES
                ),
                SuggestedAction(
                    icon = WizardInventory,
                    title = "ثبت موجودی گاوصندوق و ویترین",
                    description = "ورود طلای خام، مصنوعات کارگاهی و بار سکه",
                    targetTab = AppTab.CALCULATOR
                ),
                SuggestedAction(
                    icon = WizardCalculate,
                    title = "ماشین‌حساب تخصصی طلا و جواهر",
                    description = "محاسبه فوری قیمت، اجرت، سود و مالیات روز",
                    targetTab = AppTab.CALCULATOR
                )
            )

            suggestions.forEach { action ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = !isValidating) { onEnterApp(action.targetTab) },
                    shape = RoundedCornerShape(12.dp),
                    color = colors.surface,
                    border = BorderStroke(0.6.dp, colors.border),
                    shadowElevation = if (colors.isDark) 0.dp else 1.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(colors.surfaceElevated),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = action.icon,
                                    contentDescription = null,
                                    tint = colors.goldPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = action.title,
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.textMain
                                )
                                Text(
                                    text = action.description,
                                    fontSize = 10.5.sp,
                                    color = colors.textSecondary
                                )
                            }
                        }

                        // Chevron pointing Left (forward in Persian RTL)
                        Icon(
                            imageVector = WizardChevronLeft,
                            contentDescription = null,
                            tint = colors.goldPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
    }
}

private data class SuggestedAction(
    val icon: ImageVector,
    val title: String,
    val description: String,
    val targetTab: AppTab
)

/**
 * Standalone Completion Step Screen.
 */
@Composable
fun WizardCompletionStep(
    profileState: WizardProfileState,
    financialState: WizardFinancialState,
    onEnterApp: (AppTab) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalGoldExColors.current

    Column(modifier = modifier.fillMaxSize()) {
        WizardStepHeader(
            currentStep = WizardStep.COMPLETION,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            WizardCompletionContent(
                profileState = profileState,
                financialState = financialState,
                onEnterApp = onEnterApp
            )
        }

        // Sticky Footer (RTL: Back on Right, Enter Dashboard on Left)
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = colors.surface,
            border = BorderStroke(0.6.dp, colors.border),
            shadowElevation = 8.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Secondary / Back: Right side in Persian RTL (first in Row)
                GoldButton(
                    text = "بازگشت",
                    icon = WizardArrowRight,
                    isSecondary = true,
                    onClick = onBack,
                    modifier = Modifier.weight(1f)
                )

                // Primary / Enter Dashboard: Left side in Persian RTL (second in Row)
                GoldButton(
                    text = "ورود به داشبورد قیراط",
                    trailingIcon = WizardArrowLeft,
                    onClick = { onEnterApp(AppTab.HOME) },
                    modifier = Modifier.weight(2f)
                )
            }
        }
    }
}
