package com.goldex.companion.desktop.wizard

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Diamond
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.goldex.companion.desktop.ui.controls.DesktopIconButton
import com.goldex.companion.model.PersianNumberFormatter
import com.goldex.companion.ui.theme.LocalGoldExColors

@Composable
fun DesktopWizardStepHeader(
    currentStep: DesktopWizardStep,
    onStepClick: (DesktopWizardStep) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalGoldExColors.current
    val currentStepNum = currentStep.stepNumber

    val progressTarget = currentStepNum / 5f
    val animatedProgress by animateFloatAsState(
        targetValue = progressTarget,
        animationSpec = tween(durationMillis = 350),
        label = "wizard_progress"
    )

    Surface(
        modifier = modifier.fillMaxWidth().testTag("wizard-step-header"),
        shape = RoundedCornerShape(20.dp),
        color = colors.surface,
        border = BorderStroke(0.8.dp, colors.goldBorder),
        shadowElevation = if (colors.isDark) 0.dp else 4.dp
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Top Bar: Identity & Close Action
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(colors.goldContainer)
                            .border(1.dp, colors.goldBorder, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Diamond,
                            contentDescription = null,
                            tint = colors.goldPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "راهنمای راه‌اندازی و افتتاح دوره طلافروشی",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textMain
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = colors.goldPrimary.copy(alpha = 0.15f),
                                border = BorderStroke(0.5.dp, colors.goldPrimary.copy(alpha = 0.4f))
                            ) {
                                Text(
                                    text = "گام ${PersianNumberFormatter.toPersianDigits(currentStepNum)} از ۵",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.goldPrimary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = currentStep.subtitleFa,
                            fontSize = 12.5.sp,
                            color = colors.textMuted
                        )
                    }
                }

                DesktopIconButton(
                    onClick = onClose,
                    modifier = Modifier.testTag("wizard-close-btn")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "بستن ویزارد راه‌اندازی",
                        tint = colors.textMuted
                    )
                }
            }

            // Animated Progress Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(colors.border.copy(alpha = 0.5f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedProgress)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(3.dp))
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(colors.goldPrimary, colors.goldSecondary)
                            )
                        )
                )
            }

            // Interactive Step Pills
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                DesktopWizardStep.values().forEach { step ->
                    val isPast = step.stepNumber < currentStep.stepNumber
                    val isCurrent = step == currentStep

                    val pillBg = when {
                        isCurrent -> colors.goldContainer
                        isPast -> colors.goldPrimary.copy(alpha = 0.12f)
                        else -> Color.Transparent
                    }

                    val pillBorder = when {
                        isCurrent -> colors.goldPrimary
                        isPast -> colors.goldBorder
                        else -> colors.border.copy(alpha = 0.6f)
                    }

                    val textColor = when {
                        isCurrent -> colors.goldPrimary
                        isPast -> colors.textSecondary
                        else -> colors.textMuted
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = pillBg,
                        border = BorderStroke(if (isCurrent) 1.dp else 0.5.dp, pillBorder),
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 4.dp)
                            .clickable(enabled = isPast) { onStepClick(step) }
                            .testTag("wizard-step-pill-${step.stepNumber}")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp, horizontal = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when {
                                            isCurrent -> colors.goldPrimary
                                            isPast -> colors.profitGreen
                                            else -> colors.border.copy(alpha = 0.5f)
                                        }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isPast) {
                                    Icon(
                                        imageVector = Icons.Filled.Check,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(12.dp)
                                    )
                                } else {
                                    Text(
                                        text = PersianNumberFormatter.toPersianDigits(step.stepNumber),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isCurrent) Color.Black else colors.textMuted
                                    )
                                }
                            }

                            Spacer(Modifier.width(8.dp))

                            Text(
                                text = step.titleFa,
                                fontSize = 12.sp,
                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                                color = textColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}
