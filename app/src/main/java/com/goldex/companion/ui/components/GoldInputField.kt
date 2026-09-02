package com.goldex.companion.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.goldex.companion.ui.theme.*
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
    enabled: Boolean = true
) {
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
                        color = DarkSurfaceElevated,
                        modifier = Modifier.padding(end = 10.dp)
                    ) {
                        Text(
                            text = trailingText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextSecondary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            } else null,
            visualTransformation = if (useThousandsSeparator) {
                ThousandsSeparatorVisualTransformation(isPersian = true)
            } else {
                VisualTransformation.None
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = if (isDecimal) KeyboardType.Decimal else KeyboardType.Number
            ),
            singleLine = true,
            enabled = enabled,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = DarkSurfaceElevated,
                unfocusedContainerColor = DarkSurface,
                disabledContainerColor = DarkSurface,
                focusedBorderColor = GoldPrimary,
                unfocusedBorderColor = DarkBorder,
                focusedLabelColor = GoldLight,
                unfocusedLabelColor = TextSecondary,
                focusedTextColor = TextMain,
                unfocusedTextColor = TextMain,
                cursorColor = GoldPrimary,
                disabledTextColor = TextMuted,
                disabledBorderColor = DarkBorder
            )
        )

        if (!subLabel.isNullOrBlank()) {
            Text(
                text = subLabel,
                fontSize = 11.sp,
                fontWeight = FontWeight.Normal,
                color = GoldSecondary,
                modifier = Modifier.padding(start = 6.dp, top = 4.dp)
            )
        }
    }
}
