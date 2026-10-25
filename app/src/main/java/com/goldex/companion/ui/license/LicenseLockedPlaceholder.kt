package com.goldex.companion.ui.license

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.goldex.companion.ui.components.GoldButton
import com.goldex.companion.ui.theme.LocalGoldExColors

@Composable
fun LicenseLockedPlaceholder(
    featureName: String,
    description: String? = null,
    onActivate: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalGoldExColors.current

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(24.dp)
            .testTag("license-locked-placeholder-$featureName"),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 64.dp), // Clear bottom dock
            shape = RoundedCornerShape(20.dp),
            color = colors.surface,
            border = BorderStroke(1.dp, colors.goldPrimary.copy(alpha = 0.25f)),
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Circular icon container
                Surface(
                    shape = CircleShape,
                    color = colors.goldContainer,
                    border = BorderStroke(1.5.dp, colors.goldPrimary.copy(alpha = 0.5f)),
                    modifier = Modifier.size(68.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.Lock,
                            contentDescription = null,
                            tint = colors.goldPrimary,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                // Title
                Text(
                    text = "دسترسی به $featureName قفل است",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textMain,
                    textAlign = TextAlign.Center
                )

                // Explanation
                Text(
                    text = description
                        ?: "مهلت استفاده آزمایشی رایگان برای این دستگاه به پایان رسیده است. جهت استفاده نامحدود از $featureName، صدور فاکتور و محاسبات زرگری، لطفاً اشتراک قیراط را فعال نمایید.",
                    fontSize = 13.sp,
                    lineHeight = 22.sp,
                    color = colors.textSecondary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(4.dp))

                // CTA Button
                GoldButton(
                    text = "فعال‌سازی اشتراک قیراط",
                    icon = Icons.Filled.Lock,
                    onClick = onActivate,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("locked-feature-activate-btn")
                )

                // Safety assurance note
                Text(
                    text = "اطلاعات ثبت‌شده شما کاملاً محفوظ است و پس از فعال‌سازی مجدداً در دسترس خواهد بود.",
                    fontSize = 11.5.sp,
                    color = colors.textMuted,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
