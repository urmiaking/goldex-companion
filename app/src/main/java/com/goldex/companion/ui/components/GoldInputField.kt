package com.goldex.companion.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.goldex.companion.ui.theme.LocalGoldExColors
import com.goldex.companion.ui.theme.VazirmatnFamily
import com.goldex.companion.ui.theme.VazirmatnFeatureSettings
import com.goldex.companion.ui.util.ThousandsSeparatorVisualTransformation

@Composable
fun GoldInputField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingText: String? = null,
    subLabel: String? = null,
    isDecimal: Boolean = false,
    useThousandsSeparator: Boolean = true,
    keyboardType: KeyboardType? = null,
    enabled: Boolean = true
) {
    val colors = LocalGoldExColors.current
    val effectiveKeyboardType = keyboardType ?: if (isDecimal) KeyboardType.Decimal else KeyboardType.Number
    val isText = effectiveKeyboardType == KeyboardType.Text

    Column(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = {
                Text(
                    text = label,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            },
            leadingIcon = leadingIcon,
            trailingIcon = if (trailingText != null) {
                {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = colors.surfaceElevated,
                        modifier = Modifier.padding(end = 10.dp)
                    ) {
                        Text(
                            text = trailingText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = colors.textSecondary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            } else null,
            visualTransformation = if (isText) {
                VisualTransformation.None
            } else {
                ThousandsSeparatorVisualTransformation(
                    isPersian = true,
                    addSeparators = useThousandsSeparator
                )
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = effectiveKeyboardType
            ),
            singleLine = true,
            enabled = enabled,
            textStyle = TextStyle(
                fontFamily = VazirmatnFamily,
                fontFeatureSettings = VazirmatnFeatureSettings,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textMain,
                textDirection = if (isText) TextDirection.Rtl else TextDirection.Ltr
            ),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = colors.surfaceElevated,
                unfocusedContainerColor = colors.surface,
                disabledContainerColor = colors.surfaceVariant,
                focusedBorderColor = colors.goldPrimary,
                unfocusedBorderColor = colors.border,
                focusedLabelColor = colors.goldPrimary,
                unfocusedLabelColor = colors.textSecondary,
                focusedTextColor = colors.textMain,
                unfocusedTextColor = colors.textMain,
                cursorColor = colors.goldPrimary,
                disabledTextColor = colors.textMuted,
                disabledBorderColor = colors.border
            )
        )

        if (!subLabel.isNullOrBlank()) {
            Text(
                text = subLabel,
                fontSize = 11.sp,
                fontWeight = FontWeight.Normal,
                color = colors.goldSecondary,
                modifier = Modifier.padding(start = 6.dp, top = 4.dp)
            )
        }
    }
}
