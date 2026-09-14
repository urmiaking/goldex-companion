package com.goldex.companion.ui.wizard

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.goldex.companion.model.PersianNumberFormatter
import com.goldex.companion.ui.calculator.AppTab
import com.goldex.companion.ui.components.GoldButton
import com.goldex.companion.ui.components.LuxuryCard
import com.goldex.companion.ui.theme.LocalGoldExColors

@Composable
fun WizardCompletionStep(
    profileState: WizardProfileState,
    financialState: WizardFinancialState,
    onEnterApp: (AppTab) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalGoldExColors.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Step Header Stepper (100% Completed)
        WizardStepHeader(currentStep = WizardStep.COMPLETION)

        // Celebration & Congratulation Emblem
        LuxuryCard(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(20.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Golden Trophy Emblem with Glow
                Box(
                    modifier = Modifier.size(76.dp),
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
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(colors.goldContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = WizardStorefront,
                            contentDescription = null,
                            tint = colors.goldPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = profileState.galleryName,
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
                            text = "آنلاین",
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
                        .clickable { onEnterApp(action.targetTab) },
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

                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = null,
                            tint = colors.textMuted,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Primary Enter Dashboard Button
        GoldButton(
            text = "ورود به داشبورد قیراط",
            icon = Icons.AutoMirrored.Filled.ArrowBack,
            onClick = { onEnterApp(AppTab.HOME) },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

private data class SuggestedAction(
    val icon: ImageVector,
    val title: String,
    val description: String,
    val targetTab: AppTab
)
