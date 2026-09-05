package com.goldex.companion.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.goldex.companion.ui.theme.LocalGoldExColors
import com.goldex.companion.ui.theme.VazirmatnFamily
import com.goldex.companion.ui.theme.VazirmatnFeatureSettings
import com.goldex.companion.ui.theme.goldButtonGradient
import com.goldex.companion.ui.theme.goldButtonText
import com.goldex.companion.ui.theme.hairlineBorder

/**
 * Sovereign Aurum luxury button matching user-approved button style (media_1788630349237.png).
 *
 * Primary:
 * - Horizontal gradient (#FAC24B -> #E7B342)
 * - Antique dark bronze bold text (#554300, 14.sp)
 * - 24.dp pill shape, 48.dp height
 *
 * Secondary:
 * - surfaceElevated background
 * - hairlineBorder
 * - goldPrimary icon
 * - textMain text (13.sp)
 */
@Composable
fun GoldButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    isSecondary: Boolean = false,
    enabled: Boolean = true,
    isLoading: Boolean = false
) {
    val colors = LocalGoldExColors.current

    if (isSecondary) {
        Button(
            onClick = onClick,
            enabled = enabled && !isLoading,
            modifier = modifier.height(48.dp),
            shape = RoundedCornerShape(20.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = colors.surfaceElevated,
                contentColor = colors.textMain,
                disabledContainerColor = colors.surfaceElevated.copy(alpha = 0.5f),
                disabledContentColor = colors.textMuted
            ),
            border = if (enabled) {
                colors.hairlineBorder
            } else {
                androidx.compose.foundation.BorderStroke(0.6.dp, colors.border.copy(alpha = 0.4f))
            },
            contentPadding = PaddingValues(horizontal = 16.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = colors.goldPrimary,
                    strokeWidth = 2.dp
                )
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (icon != null) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (enabled) colors.goldPrimary else colors.textMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Text(
                        text = text,
                        fontFamily = VazirmatnFamily,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (enabled) colors.textMain else colors.textMuted
                    )
                }
            }
        }
    } else {
        val buttonTextAndIconColor = colors.goldButtonText
        val gradientBrush = if (enabled) {
            colors.goldButtonGradient
        } else {
            Brush.horizontalGradient(
                listOf(
                    colors.border.copy(alpha = 0.5f),
                    colors.surfaceVariant.copy(alpha = 0.5f),
                    colors.border.copy(alpha = 0.5f)
                )
            )
        }

        Button(
            onClick = onClick,
            enabled = enabled && !isLoading,
            modifier = modifier.height(48.dp),
            shape = RoundedCornerShape(24.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Transparent,
                contentColor = buttonTextAndIconColor,
                disabledContainerColor = Color.Transparent,
                disabledContentColor = buttonTextAndIconColor.copy(alpha = 0.5f)
            ),
            elevation = ButtonDefaults.buttonElevation(
                defaultElevation = if (enabled) 1.5.dp else 0.dp,
                pressedElevation = 0.5.dp,
                focusedElevation = 1.5.dp,
                hoveredElevation = 2.dp,
                disabledElevation = 0.dp
            ),
            contentPadding = PaddingValues()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = gradientBrush,
                        shape = RoundedCornerShape(24.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = buttonTextAndIconColor,
                        strokeWidth = 2.dp
                    )
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        if (icon != null) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = if (enabled) buttonTextAndIconColor else buttonTextAndIconColor.copy(alpha = 0.5f),
                                modifier = Modifier.size(19.dp)
                            )
                        }
                        Text(
                            text = text,
                            fontFamily = VazirmatnFamily,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (enabled) buttonTextAndIconColor else buttonTextAndIconColor.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        }
    }
}
