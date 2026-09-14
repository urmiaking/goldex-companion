package com.goldex.companion.ui.wizard

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.goldex.companion.model.PersianNumberFormatter
import com.goldex.companion.ui.theme.LocalGoldExColors

@Composable
fun WizardStepHeader(
    currentStep: WizardStep,
    modifier: Modifier = Modifier
) {
    val colors = LocalGoldExColors.current
    val currentStepNum = currentStep.stepNumber.coerceIn(1, 4)

    val progressTarget = currentStepNum / 4f
    val animatedProgress by animateFloatAsState(
        targetValue = progressTarget,
        animationSpec = tween(durationMillis = 400),
        label = "wizard_progress"
    )

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = colors.surface,
        border = BorderStroke(0.8.dp, colors.goldBorder),
        shadowElevation = if (colors.isDark) 0.dp else 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header Row: Step counter and status dot
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
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(colors.goldPrimary)
                    )
                    Text(
                        text = "مرحله ${PersianNumberFormatter.toPersianDigits(currentStepNum)} از ۴",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.goldPrimary
                    )
                }

                Text(
                    text = when (currentStepNum) {
                        1 -> "مشخصات گالری و پروانه"
                        2 -> "پیش‌فرض‌های مالیاتی و سود"
                        3 -> "موجودی اول دوره و گاوصندوق"
                        4 -> "تکمیل و راه‌اندازی نهایی"
                        else -> ""
                    },
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = colors.textMuted
                )
            }

            // Progress Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(if (colors.isDark) Color(0x3DFBBF24) else Color(0x2E10141E))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(animatedProgress)
                        .clip(RoundedCornerShape(3.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    colors.goldSecondary,
                                    colors.goldPrimary
                                )
                            )
                        )
                )
            }

            // Step Nodes (4 columns)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val steps = listOf(
                    1 to "مشخصات",
                    2 to "تنظیمات مالی",
                    3 to "موجودی اول",
                    4 to "تأیید نهایی"
                )

                steps.forEach { (stepNum, label) ->
                    val isDone = stepNum < currentStepNum
                    val isActive = stepNum == currentStepNum

                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        isDone -> colors.profitGreen
                                        isActive -> colors.goldPrimary
                                        else -> if (colors.isDark) Color(0xFF1E2433) else Color(0xFFECEFF3)
                                    }
                                )
                                .then(
                                    when {
                                        isActive -> Modifier.border(1.5.dp, colors.goldSecondary, CircleShape)
                                        isDone -> Modifier
                                        else -> Modifier.border(0.7.dp, colors.goldBorder.copy(alpha = 0.45f), CircleShape)
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isDone) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                            } else {
                                Text(
                                    text = PersianNumberFormatter.toPersianDigits(stepNum),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isActive) Color(0xFF241A00) else colors.textMuted
                                )
                            }
                        }

                        Text(
                            text = label,
                            fontSize = 10.sp,
                            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                            color = when {
                                isActive -> colors.goldPrimary
                                isDone -> colors.profitGreen
                                else -> colors.textMuted
                            },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}
