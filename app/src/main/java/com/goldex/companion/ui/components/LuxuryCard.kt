package com.goldex.companion.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.goldex.companion.ui.theme.LocalGoldExColors
import com.goldex.companion.ui.theme.goldHairlineBorder
import com.goldex.companion.ui.theme.specularHairlineBrush

/**
 * Stitch "Persian Sovereign Aurum" luxury elevated surface card.
 *
 * Features:
 * - 16.dp rounded corners
 * - Gilded hairline border (0.8.dp goldBorder)
 * - Soft ambient elevation shadow
 * - Top specular gold hairline gradient (2.dp)
 */
@Composable
fun LuxuryCard(
    modifier: Modifier = Modifier,
    hasTopHairline: Boolean = true,
    elevation: Dp = 2.dp,
    shape: Shape = RoundedCornerShape(16.dp),
    backgroundColor: Color? = null,
    borderStroke: BorderStroke? = null,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(14.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = LocalGoldExColors.current

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = shape,
        color = backgroundColor ?: colors.surface,
        border = borderStroke ?: colors.goldHairlineBorder,
        shadowElevation = if (colors.isDark) 0.dp else elevation
    ) {
        Column {
            if (hasTopHairline) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .background(brush = colors.specularHairlineBrush)
                )
            }
            Column(
                modifier = Modifier.padding(contentPadding),
                verticalArrangement = verticalArrangement,
                content = content
            )
        }
    }
}
