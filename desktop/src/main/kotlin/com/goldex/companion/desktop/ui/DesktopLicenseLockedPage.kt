package com.goldex.companion.desktop.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Verified
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
import com.goldex.companion.desktop.ui.controls.DesktopActionButton
import com.goldex.companion.ui.theme.LocalGoldExColors

@Composable
fun DesktopLicenseLockedPage(
    title: String,
    description: String? = null,
    onActivate: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalGoldExColors.current

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(32.dp)
            .testTag("desktop-license-locked-$title"),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier.widthIn(max = 540.dp).fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = colors.surface,
            border = BorderStroke(1.dp, colors.goldPrimary.copy(alpha = 0.3f)),
            shadowElevation = 12.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                // Circular luxury icon badge
                Surface(
                    shape = CircleShape,
                    color = colors.goldContainer,
                    border = BorderStroke(1.5.dp, colors.goldPrimary.copy(alpha = 0.5f)),
                    modifier = Modifier.size(72.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Outlined.Lock,
                            contentDescription = null,
                            tint = colors.goldPrimary,
                            modifier = Modifier.size(34.dp)
                        )
                    }
                }

                // Title
                Text(
                    text = "دسترسی به $title قفل است",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textMain,
                    textAlign = TextAlign.Center
                )

                // Description
                Text(
                    text = description
                        ?: "مهلت استفاده آزمایشی رایگان برای این ایستگاه کاری به پایان رسیده است. جهت دسترسی نامحدود به $title، صدور فاکتور و محاسبات زرگری، لطفاً اشتراک قیراط را فعال نمایید.",
                    fontSize = 13.5.sp,
                    lineHeight = 22.sp,
                    color = colors.textSecondary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Primary CTA Button using Windows Design System (12dp, 14sp, required 20dp icon)
                DesktopActionButton(
                    text = "فعال‌سازی اشتراک قیراط",
                    icon = Icons.Outlined.Verified,
                    onClick = onActivate,
                    isSecondary = false,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("desktop-locked-activate-btn")
                )

                // Privacy / data safety disclaimer
                Text(
                    text = "کلیه اطلاعات ذخیره‌شده شما روی این رایانه محفوظ است و پس از فعال‌سازی لایسنس بلافاصله قابل استفاده خواهد بود.",
                    fontSize = 11.5.sp,
                    color = colors.textMuted,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
