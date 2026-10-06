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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.goldex.companion.ui.theme.ButtonShape
import com.goldex.companion.ui.theme.LocalGoldExColors
import com.goldex.companion.ui.theme.VazirmatnFamily
import com.goldex.companion.ui.theme.VazirmatnFeatureSettings
import com.goldex.companion.ui.theme.goldButtonGradient
import com.goldex.companion.ui.theme.goldButtonText
import com.goldex.companion.ui.theme.hairlineBorder

/**
 * Sovereign Aurum luxury button matching unified 12.dp corner radius.
 *
 * Primary:
 * - Horizontal gradient (#FAC24B -> #E7B342)
 * - Antique dark bronze bold text (#554300, 14.sp)
 * - 12.dp shape (ButtonShape), 48.dp height
 *
 * Secondary:
 * - surfaceElevated background
 * - hairlineBorder
 * - goldPrimary icon
 * - textMain text (13.sp)
 * - 12.dp shape (ButtonShape)
 */
@Composable
fun GoldButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
    isSecondary: Boolean = false,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    height: Dp = 48.dp
) {
    val colors = LocalGoldExColors.current

    if (isSecondary) {
        Button(
            onClick = onClick,
            enabled = enabled && !isLoading,
            modifier = modifier.height(height),
            shape = ButtonShape,
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
                    if (trailingIcon != null) {
                        Icon(
                            imageVector = trailingIcon,
                            contentDescription = null,
                            tint = if (enabled) colors.goldPrimary else colors.textMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    } else {
        val buttonContentColor = if (enabled) colors.goldButtonText else colors.textMuted

        Button(
            onClick = onClick,
            enabled = enabled && !isLoading,
            modifier = modifier.height(height),
            shape = ButtonShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Transparent,
                contentColor = colors.goldButtonText,
                disabledContainerColor = Color.Transparent,
                disabledContentColor = colors.textMuted
            ),
            elevation = ButtonDefaults.buttonElevation(
                defaultElevation = if (enabled) 1.5.dp else 0.dp,
                pressedElevation = 0.5.dp,
                focusedElevation = 1.5.dp,
                hoveredElevation = 2.dp,
                disabledElevation = 0.dp
            ),
            border = if (enabled) null else androidx.compose.foundation.BorderStroke(0.6.dp, colors.border.copy(alpha = 0.35f)),
            contentPadding = PaddingValues()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .then(
                        if (enabled) {
                            Modifier.background(
                                brush = colors.goldButtonGradient,
                                shape = ButtonShape
                            )
                        } else {
                            Modifier.background(
                                color = colors.surfaceElevated.copy(alpha = 0.6f),
                                shape = ButtonShape
                            )
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = buttonContentColor,
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
                                tint = buttonContentColor,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                        Text(
                            text = text,
                            fontFamily = VazirmatnFamily,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = buttonContentColor
                        )
                        if (trailingIcon != null) {
                            Icon(
                                imageVector = trailingIcon,
                                contentDescription = null,
                                tint = buttonContentColor,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
