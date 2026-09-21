package com.goldex.companion.ui.security

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.goldex.companion.R
import com.goldex.companion.domain.security.AppLockState
import com.goldex.companion.ui.hub.HubFingerprint
import com.goldex.companion.ui.theme.LocalGoldExColors

/**
 * High-security lock screen adhering to the Persian Sovereign Aurum visual language.
 * Features the official Qirato logo with living breathing glow aura, security badge,
 * Persian typography, and biometric unlock action.
 */
@Composable
fun AppLockScreen(
    state: AppLockState,
    onUnlockClick: () -> Unit
) {
    val context = LocalContext.current
    val colors = LocalGoldExColors.current

    // Send app to background instead of bypassing the lock screen on system back
    BackHandler {
        (context as? Activity)?.moveTaskToBack(true)
    }

    // Automatically initiate biometric prompt upon entering lock screen
    LaunchedEffect(Unit) {
        onUnlockClick()
    }

    // Subtle living breathing pulse animation for ambient gold halo
    val infiniteTransition = rememberInfiniteTransition(label = "halo_pulse")
    val haloAlpha by infiniteTransition.animateFloat(
        initialValue = if (colors.isDark) 0.12f else 0.08f,
        targetValue = if (colors.isDark) 0.32f else 0.22f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "halo_alpha"
    )
    val haloScale by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "halo_scale"
    )

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.background)
        ) {
            // Ambient top and bottom gold highlights
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .align(Alignment.TopCenter)
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                colors.goldPrimary.copy(alpha = if (colors.isDark) 0.06f else 0.04f),
                                Color.Transparent
                            )
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Spacer(modifier = Modifier.height(24.dp))

                // Center Block: Logo, Titles, Description, Error, and Button
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Central Emblem: Official Qirato Logo + Ambient Breathing Halo + Biometric Indicator
                    Box(
                        modifier = Modifier.size(140.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        // Outer breathing radial glow
                        Box(
                            modifier = Modifier
                                .size(140.dp)
                                .graphicsLayer {
                                    scaleX = haloScale
                                    scaleY = haloScale
                                    alpha = haloAlpha
                                }
                                .clip(CircleShape)
                                .background(
                                    Brush.radialGradient(
                                        listOf(
                                            colors.goldPrimary,
                                            colors.goldSecondary.copy(alpha = 0.4f),
                                            Color.Transparent
                                        )
                                    )
                                )
                        )

                        // Central Emblem Container
                        Box(
                            modifier = Modifier
                                .size(108.dp)
                                .clip(CircleShape)
                                .background(
                                    if (colors.isDark) Color(0xFF0D121A) else Color(0xFF1B222E)
                                )
                                .border(
                                    width = 1.8.dp,
                                    brush = Brush.sweepGradient(
                                        listOf(
                                            colors.goldPrimary,
                                            colors.goldSecondary,
                                            colors.goldBullion,
                                            colors.goldPrimary
                                        )
                                    ),
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            // Official Qirato Raw Logo
                            Image(
                                painter = painterResource(id = R.drawable.ic_logo_raw),
                                contentDescription = "لوگوی قیراط",
                                modifier = Modifier.size(64.dp)
                            )
                        }

                        // Floating biometric badge at the bottom-right corner of the logo
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .offset(x = (-4).dp, y = (-4).dp)
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(colors.surfaceElevated)
                                .border(
                                    width = 1.2.dp,
                                    brush = Brush.linearGradient(
                                        listOf(colors.goldPrimary, colors.goldSecondary)
                                    ),
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = HubFingerprint,
                                contentDescription = "قفل اثر انگشت",
                                tint = colors.goldPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Persian Calligraphy Logo
                    Image(
                        painter = painterResource(id = R.drawable.text_persian),
                        contentDescription = "قیراط",
                        modifier = Modifier.height(28.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Security Mode Active Chip
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(colors.surfaceElevated)
                            .border(0.6.dp, colors.border, RoundedCornerShape(20.dp))
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.5.dp)
                                .clip(CircleShape)
                                .background(colors.profitGreen)
                        )
                        Text(
                            text = "حالت امنیتی فعال",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = colors.textSecondary
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Text(
                        text = "برنامه قفل است",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textMain
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "برای دسترسی به مبالغ، دفاتر مالی و صدور فاکتور، لطفاً اثر انگشت خود را تأیید کنید.",
                        fontSize = 12.5.sp,
                        color = colors.textMuted,
                        textAlign = TextAlign.Center,
                        lineHeight = 21.sp,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )

                    // Error / warning banner if authentication was canceled or failed
                    AnimatedVisibility(
                        visible = !state.authErrorMessage.isNullOrBlank(),
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        state.authErrorMessage?.let { message ->
                            Spacer(modifier = Modifier.height(16.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(colors.errorRed.copy(alpha = 0.10f))
                                    .border(
                                        width = 0.8.dp,
                                        color = colors.errorRed.copy(alpha = 0.35f),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = message,
                                    fontSize = 12.sp,
                                    color = colors.errorRed,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    // Primary Luxury Unlock Button with Gold Gradient
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        colors.goldSecondary,
                                        colors.goldPrimary,
                                        colors.goldSecondary
                                    )
                                )
                            )
                            .clickable(onClick = onUnlockClick),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = HubFingerprint,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = Color.White
                            )
                            Spacer(modifier = Modifier.size(8.dp))
                            Text(
                                text = "باز کردن با اثر انگشت",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }

                // Security Encryption Footer
                Row(
                    modifier = Modifier.padding(bottom = 24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = HubFingerprint,
                        contentDescription = null,
                        tint = colors.textMuted.copy(alpha = 0.45f),
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = "محافظت شده با ماژول امنیتی دستگاه",
                        fontSize = 10.5.sp,
                        color = colors.textMuted.copy(alpha = 0.55f)
                    )
                }
            }
        }
    }
}
