package com.goldex.companion.ui.security

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.goldex.companion.domain.security.AppLockState
import com.goldex.companion.ui.hub.HubFingerprint
import com.goldex.companion.ui.theme.LocalGoldExColors

/**
 * High-security lock screen adhering to the Persian Sovereign Aurum visual language.
 * Protects financial records, ledgers, inventory, and rates from unauthorized access.
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

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.background),
            contentAlignment = Alignment.Center
        ) {
            // Subtle ambient gold halo behind the lock icon
            Box(
                modifier = Modifier
                    .size(240.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(
                                colors.goldPrimary.copy(alpha = if (colors.isDark) 0.15f else 0.10f),
                                Color.Transparent
                            )
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Gold glowing biometric emblem
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .clip(CircleShape)
                        .background(colors.surfaceElevated)
                        .border(
                            width = 1.5.dp,
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
                        modifier = Modifier.size(48.dp)
                    )
                }

                Spacer(modifier = Modifier.height(28.dp))

                // App branding & title
                Text(
                    text = "قیراط",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.goldPrimary
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "برنامه قفل است",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textMain
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "برای دسترسی به مبالغ، دفاتر مالی و صدور فاکتور، لطفاً اثر انگشت خود را تأیید کنید.",
                    fontSize = 12.5.sp,
                    color = colors.textMuted,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                // Error / warning banner if authentication was canceled or failed
                AnimatedVisibility(
                    visible = !state.authErrorMessage.isNullOrBlank(),
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    state.authErrorMessage?.let { message ->
                        Spacer(modifier = Modifier.height(20.dp))
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

                Spacer(modifier = Modifier.height(32.dp))

                // Primary unlock button
                Button(
                    onClick = onUnlockClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.goldPrimary,
                        contentColor = Color.White
                    )
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
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
