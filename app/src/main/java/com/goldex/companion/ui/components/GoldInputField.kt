package com.goldex.companion.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
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
    enabled: Boolean = true
) {
    Column(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label, fontSize = 13.sp) },
            leadingIcon = leadingIcon,
            trailingIcon = if (trailingText != null) {
                { Text(trailingText, fontSize = 11.sp, color = TextMuted, modifier = Modifier.padding(end = 8.dp)) }
            } else null,
            visualTransformation = ThousandsSeparatorVisualTransformation(isPersian = true),
            keyboardOptions = KeyboardOptions(
                keyboardType = if (isDecimal) KeyboardType.Decimal else KeyboardType.Number
            ),
            singleLine = true,
            enabled = enabled,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = GoldPrimary,
                unfocusedBorderColor = DarkBorder,
                focusedLabelColor = GoldLight,
                unfocusedLabelColor = TextMuted,
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
                color = GoldSecondary,
                modifier = Modifier.padding(start = 6.dp, top = 3.dp)
            )
        }
    }
}
