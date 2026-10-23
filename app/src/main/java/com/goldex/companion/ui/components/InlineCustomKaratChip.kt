package com.goldex.companion.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.goldex.companion.model.PersianNumberFormatter
import com.goldex.companion.ui.theme.LocalGoldExColors
import com.goldex.companion.ui.theme.VazirmatnFamily
import com.goldex.companion.ui.theme.VazirmatnFeatureSettings

@Composable
internal fun InlineCustomKaratChip(
    value: String,
    onValueChange: (String) -> Unit,
    isSelected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "سفارشی",
    height: androidx.compose.ui.unit.Dp = 36.dp,
    fontSize: androidx.compose.ui.unit.TextUnit = 11.sp,
    accessibilityLabel: String = "عیار سفارشی"
) {
    val colors = LocalGoldExColors.current
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (isSelected) colors.goldContainer.copy(alpha = 0.5f) else colors.surfaceElevated,
        border = BorderStroke(if (isSelected) 1.2.dp else 0.6.dp, if (isSelected) colors.goldPrimary else colors.border),
        modifier = modifier
            .height(height)
            .clickable { onSelect() }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            BasicTextField(
                value = PersianNumberFormatter.toPersianDigits(value),
                onValueChange = {
                    val filtered = it.filter { ch -> ch.isDigit() || ch in '\u06F0'..'\u06F9' }
                    val clean = PersianNumberFormatter.toEnglishDigits(filtered)
                    onValueChange(clean)
                    onSelect()
                },
                singleLine = true,
                modifier = Modifier.semantics { contentDescription = accessibilityLabel },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                textStyle = TextStyle(
                    fontFamily = VazirmatnFamily,
                    fontFeatureSettings = VazirmatnFeatureSettings,
                    fontSize = fontSize,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    color = if (isSelected) colors.goldPrimary else colors.textMain,
                    textDirection = TextDirection.Ltr
                ),
                cursorBrush = SolidColor(colors.goldPrimary),
                decorationBox = { innerTextField ->
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        if (value.isEmpty()) {
                            Text(
                                text = placeholder,
                                fontSize = fontSize,
                                color = colors.textMuted,
                                fontFamily = VazirmatnFamily,
                                textAlign = TextAlign.Center
                            )
                        }
                        innerTextField()
                    }
                }
            )
        }
    }
}
