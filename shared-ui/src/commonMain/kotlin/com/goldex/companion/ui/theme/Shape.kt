package com.goldex.companion.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Standard corner radius for all buttons and interactive button-like controls across GoldEx Companion.
 * Matches the 12.dp curvature of the header notification button and primary dashboard cards.
 */
val ButtonCornerRadius = 12.dp
val ButtonShape = RoundedCornerShape(ButtonCornerRadius)

val GoldExShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(14.dp),
    extraLarge = RoundedCornerShape(16.dp)
)
