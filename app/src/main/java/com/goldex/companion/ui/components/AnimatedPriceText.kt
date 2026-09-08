package com.goldex.companion.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import com.goldex.companion.model.PersianNumberFormatter
import com.goldex.companion.ui.theme.LuxuryMotion
import com.goldex.companion.ui.theme.VazirmatnFamily

/**
 * Animated financial price and metric text with smooth sliding physics.
 * Automatically detects whether the value increased or decreased and slides
 * digits in the natural direction with soft fade.
 */
@Composable
fun AnimatedPriceText(
    amount: Long,
    modifier: Modifier = Modifier,
    unit: String = "",
    color: Color = Color.Unspecified,
    fontSize: androidx.compose.ui.unit.TextUnit = androidx.compose.ui.unit.TextUnit.Unspecified,
    fontWeight: FontWeight? = null,
    style: TextStyle = LocalTextStyle.current
) {
    var previousAmount by remember { mutableStateOf(amount) }
    val isIncreasing = amount >= previousAmount
    previousAmount = amount

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        AnimatedContent(
            targetState = amount,
            transitionSpec = {
                LuxuryMotion.numberSlideSpec(isIncreasing = targetState >= initialState)
            },
            label = "animatedPriceNumber"
        ) { targetValue ->
            Text(
                text = PersianNumberFormatter.formatPrice(targetValue),
                color = color,
                fontSize = fontSize,
                fontWeight = fontWeight,
                fontFamily = VazirmatnFamily,
                style = style
            )
        }

        if (unit.isNotBlank()) {
            Text(
                text = " $unit",
                color = color,
                fontSize = fontSize * 0.7f,
                fontWeight = FontWeight.Medium,
                fontFamily = VazirmatnFamily,
                style = style
            )
        }
    }
}

/**
 * Animated generic number or string counter (weights, counts, percentages)
 */
@Composable
fun AnimatedNumberText(
    text: String,
    modifier: Modifier = Modifier,
    unit: String = "",
    color: Color = Color.Unspecified,
    fontSize: androidx.compose.ui.unit.TextUnit = androidx.compose.ui.unit.TextUnit.Unspecified,
    fontWeight: FontWeight? = null,
    style: TextStyle = LocalTextStyle.current
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        AnimatedContent(
            targetState = text,
            transitionSpec = {
                LuxuryMotion.numberSlideSpec(isIncreasing = true)
            },
            label = "animatedGenericNumber"
        ) { targetValue ->
            Text(
                text = targetValue,
                color = color,
                fontSize = fontSize,
                fontWeight = fontWeight,
                fontFamily = VazirmatnFamily,
                style = style
            )
        }

        if (unit.isNotBlank()) {
            Text(
                text = " $unit",
                color = color,
                fontSize = fontSize * 0.7f,
                fontWeight = FontWeight.Medium,
                fontFamily = VazirmatnFamily,
                style = style
            )
        }
    }
}
