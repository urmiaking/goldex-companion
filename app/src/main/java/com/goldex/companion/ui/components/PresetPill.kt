package com.goldex.companion.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.goldex.companion.ui.theme.LocalGoldExColors

/**
 * Quick-preset pill for values (weights, profit %, tax %, increments).
 */
@Composable
fun PresetPill(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    enabled: Boolean = true,
    shape: Shape = RoundedCornerShape(20.dp),
    containerColor: Color? = null,
    contentColor: Color? = null,
    fontSize: TextUnit = 10.sp,
    contentPadding: PaddingValues = PaddingValues(horizontal = 9.dp, vertical = 5.dp)
) {
    val colors = LocalGoldExColors.current

    val background = containerColor ?: if (isSelected) {
        colors.goldContainer
    } else {
        colors.surfaceElevated
    }

    val border = BorderStroke(
        width = if (isSelected) 1.dp else 0.5.dp,
        color = if (isSelected) colors.goldPrimary else colors.border
    )

    val textColor = contentColor ?: if (isSelected) {
        colors.goldPrimary
    } else {
        colors.textSecondary
    }

    Surface(
        modifier = modifier
            .clip(shape)
            .clickable(enabled = enabled, onClick = onClick),
        shape = shape,
        color = background,
        border = border
    ) {
        Text(
            text = label,
            fontSize = fontSize,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = textColor,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(contentPadding)
        )
    }
}

/**
 * Horizontal row of quick-preset chips.
 */
@Composable
fun <T> PresetChipRow(
    presets: List<Pair<T, String>>,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    isSelected: (T) -> Boolean = { false },
    horizontalArrangement: Arrangement.Horizontal = Arrangement.spacedBy(6.dp),
    trailingContent: @Composable (RowScope.() -> Unit)? = null
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = horizontalArrangement,
        verticalAlignment = Alignment.CenterVertically
    ) {
        presets.forEach { (value, label) ->
            PresetPill(
                label = label,
                isSelected = isSelected(value),
                onClick = { onSelect(value) }
            )
        }
        if (trailingContent != null) {
            trailingContent()
        }
    }
}
