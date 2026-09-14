package com.goldex.companion.ui.wizard

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

internal val WizardAutoAwesome: ImageVector = ImageVector.Builder(
    name = "WizardAutoAwesome",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f
).apply {
    path(fill = SolidColor(Color.White)) {
        moveTo(19f, 9f)
        lineTo(20.25f, 6.25f)
        lineTo(23f, 5f)
        lineTo(20.25f, 3.75f)
        lineTo(19f, 1f)
        lineTo(17.75f, 3.75f)
        lineTo(15f, 5f)
        lineTo(17.75f, 6.25f)
        close()
        moveTo(11.5f, 9.5f)
        lineTo(9f, 4f)
        lineTo(6.5f, 9.5f)
        lineTo(1f, 12f)
        lineTo(6.5f, 14.5f)
        lineTo(9f, 20f)
        lineTo(11.5f, 14.5f)
        lineTo(17f, 12f)
        close()
        moveTo(19f, 15f)
        lineTo(17.75f, 17.75f)
        lineTo(15f, 19f)
        lineTo(17.75f, 20.25f)
        lineTo(19f, 23f)
        lineTo(20.25f, 20.25f)
        lineTo(23f, 19f)
        lineTo(20.25f, 17.75f)
        close()
    }
}.build()

internal val WizardReceiptLong: ImageVector = ImageVector.Builder(
    name = "WizardReceiptLong",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f
).apply {
    path(
        stroke = SolidColor(Color.White),
        strokeLineWidth = 1.8f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round
    ) {
        moveTo(19.5f, 21.5f)
        lineTo(17f, 20f)
        lineTo(14.5f, 21.5f)
        lineTo(12f, 20f)
        lineTo(9.5f, 21.5f)
        lineTo(7f, 20f)
        lineTo(4.5f, 21.5f)
        verticalLineTo(3.5f)
        lineTo(7f, 5f)
        lineTo(9.5f, 3.5f)
        lineTo(12f, 5f)
        lineTo(14.5f, 3.5f)
        lineTo(17f, 5f)
        lineTo(19.5f, 3.5f)
        close()
        moveTo(8f, 9f)
        horizontalLineTo(16f)
        moveTo(8f, 13f)
        horizontalLineTo(14f)
        moveTo(8f, 17f)
        horizontalLineTo(11f)
    }
}.build()

internal val WizardCandlestick: ImageVector = ImageVector.Builder(
    name = "WizardCandlestick",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f
).apply {
    path(
        stroke = SolidColor(Color.White),
        strokeLineWidth = 1.8f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round
    ) {
        moveTo(7f, 3f)
        verticalLineTo(6f)
        moveTo(7f, 14f)
        verticalLineTo(21f)
        moveTo(5f, 6f)
        horizontalLineTo(9f)
        verticalLineTo(14f)
        horizontalLineTo(5f)
        close()
        moveTo(17f, 3f)
        verticalLineTo(9f)
        moveTo(17f, 17f)
        verticalLineTo(21f)
        moveTo(15f, 9f)
        horizontalLineTo(19f)
        verticalLineTo(17f)
        horizontalLineTo(15f)
        close()
    }
}.build()

internal val WizardCheckCircle: ImageVector = ImageVector.Builder(
    name = "WizardCheckCircle",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f
).apply {
    path(fill = SolidColor(Color.White)) {
        moveTo(12f, 2f)
        curveTo(6.48f, 2f, 2f, 6.48f, 2f, 12f)
        curveTo(2f, 17.52f, 6.48f, 22f, 12f, 22f)
        curveTo(17.52f, 22f, 22f, 17.52f, 22f, 12f)
        close()
        moveTo(10f, 16.4f)
        lineTo(5.6f, 12f)
        lineTo(7f, 10.6f)
        lineTo(10f, 13.6f)
        lineTo(17f, 6.6f)
        lineTo(18.4f, 8f)
        close()
    }
}.build()

internal val WizardStorefront: ImageVector = ImageVector.Builder(
    name = "WizardStorefront",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f
).apply {
    path(
        stroke = SolidColor(Color.White),
        strokeLineWidth = 1.8f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round
    ) {
        moveTo(4f, 4f)
        horizontalLineTo(20f)
        lineTo(21f, 9f)
        horizontalLineTo(3f)
        close()
        moveTo(4f, 9f)
        verticalLineTo(20f)
        horizontalLineTo(20f)
        verticalLineTo(9f)
        moveTo(9f, 14f)
        horizontalLineTo(15f)
        verticalLineTo(20f)
        horizontalLineTo(9f)
        close()
    }
}.build()

internal val WizardSecurity: ImageVector = ImageVector.Builder(
    name = "WizardSecurity",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f
).apply {
    path(
        stroke = SolidColor(Color.White),
        strokeLineWidth = 1.8f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round
    ) {
        moveTo(12f, 2f)
        lineTo(4f, 5f)
        verticalLineTo(11f)
        curveTo(4f, 16.5f, 7.5f, 20.8f, 12f, 22f)
        curveTo(16.5f, 20.8f, 20f, 16.5f, 20f, 11f)
        verticalLineTo(5f)
        close()
        moveTo(9.5f, 12f)
        lineTo(11.5f, 14f)
        lineTo(15.5f, 9.5f)
    }
}.build()

internal val WizardApartment: ImageVector = ImageVector.Builder(
    name = "WizardApartment",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f
).apply {
    path(
        stroke = SolidColor(Color.White),
        strokeLineWidth = 1.8f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round
    ) {
        moveTo(3f, 21f)
        horizontalLineTo(21f)
        moveTo(5f, 21f)
        verticalLineTo(5f)
        lineTo(13f, 3f)
        verticalLineTo(21f)
        moveTo(13f, 9f)
        horizontalLineTo(19f)
        verticalLineTo(21f)
        moveTo(8f, 8f)
        horizontalLineTo(10f)
        moveTo(8f, 12f)
        horizontalLineTo(10f)
        moveTo(8f, 16f)
        horizontalLineTo(10f)
        moveTo(15f, 13f)
        horizontalLineTo(17f)
        moveTo(15f, 17f)
        horizontalLineTo(17f)
    }
}.build()

internal val WizardDomain: ImageVector = ImageVector.Builder(
    name = "WizardDomain",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f
).apply {
    path(
        stroke = SolidColor(Color.White),
        strokeLineWidth = 1.8f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round
    ) {
        moveTo(12f, 7f)
        verticalLineTo(3f)
        horizontalLineTo(2f)
        verticalLineTo(21f)
        horizontalLineTo(22f)
        verticalLineTo(7f)
        horizontalLineTo(12f)
        close()
        moveTo(6f, 7f)
        horizontalLineTo(8f)
        moveTo(6f, 11f)
        horizontalLineTo(8f)
        moveTo(6f, 15f)
        horizontalLineTo(8f)
        moveTo(16f, 11f)
        horizontalLineTo(18f)
        moveTo(16f, 15f)
        horizontalLineTo(18f)
    }
}.build()

internal val WizardAccountBalance: ImageVector = ImageVector.Builder(
    name = "WizardAccountBalance",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f
).apply {
    path(
        stroke = SolidColor(Color.White),
        strokeLineWidth = 1.8f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round
    ) {
        moveTo(4f, 10f)
        verticalLineTo(18f)
        moveTo(8.5f, 10f)
        verticalLineTo(18f)
        moveTo(15.5f, 10f)
        verticalLineTo(18f)
        moveTo(20f, 10f)
        verticalLineTo(18f)
        moveTo(2f, 21f)
        horizontalLineTo(22f)
        moveTo(2f, 10f)
        horizontalLineTo(22f)
        moveTo(12f, 2f)
        lineTo(2f, 7f)
        horizontalLineTo(22f)
        close()
    }
}.build()

internal val WizardBalance: ImageVector = ImageVector.Builder(
    name = "WizardBalance",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f
).apply {
    path(
        stroke = SolidColor(Color.White),
        strokeLineWidth = 1.8f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round
    ) {
        moveTo(12f, 3f)
        verticalLineTo(21f)
        moveTo(4f, 7f)
        horizontalLineTo(20f)
        moveTo(4f, 7f)
        lineTo(2f, 14f)
        curveTo(2f, 16f, 4f, 16f, 6f, 16f)
        curveTo(8f, 16f, 10f, 16f, 10f, 14f)
        lineTo(8f, 7f)
        moveTo(20f, 7f)
        lineTo(18f, 14f)
        curveTo(18f, 16f, 20f, 16f, 22f, 16f)
        lineTo(20f, 7f)
        moveTo(8f, 21f)
        horizontalLineTo(16f)
    }
}.build()

internal val WizardMonetizationOn: ImageVector = ImageVector.Builder(
    name = "WizardMonetizationOn",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f
).apply {
    path(
        stroke = SolidColor(Color.White),
        strokeLineWidth = 1.8f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round
    ) {
        moveTo(12f, 2f)
        curveTo(6.48f, 2f, 2f, 6.48f, 2f, 12f)
        curveTo(2f, 17.52f, 6.48f, 22f, 12f, 22f)
        curveTo(17.52f, 22f, 22f, 17.52f, 22f, 12f)
        close()
        moveTo(12f, 6f)
        verticalLineTo(18f)
        moveTo(15f, 9f)
        curveTo(15f, 7.5f, 13.5f, 7.5f, 12f, 7.5f)
        curveTo(10.5f, 7.5f, 9f, 8.5f, 9f, 10f)
        curveTo(9f, 11.5f, 10.5f, 12f, 12f, 12.5f)
        curveTo(13.5f, 13f, 15f, 13.5f, 15f, 15f)
        curveTo(15f, 16.5f, 13.5f, 17f, 12f, 17f)
        curveTo(10.5f, 17f, 9f, 16f, 9f, 15f)
    }
}.build()

internal val WizardViewInAr: ImageVector = ImageVector.Builder(
    name = "WizardViewInAr",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f
).apply {
    path(
        stroke = SolidColor(Color.White),
        strokeLineWidth = 1.8f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round
    ) {
        moveTo(12f, 3f)
        lineTo(4f, 7.5f)
        lineTo(12f, 12f)
        lineTo(20f, 7.5f)
        close()
        moveTo(4f, 7.5f)
        verticalLineTo(16.5f)
        lineTo(12f, 21f)
        verticalLineTo(12f)
        moveTo(20f, 7.5f)
        verticalLineTo(16.5f)
        lineTo(12f, 21f)
    }
}.build()

internal val WizardRemove: ImageVector = ImageVector.Builder(
    name = "WizardRemove",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f
).apply {
    path(
        stroke = SolidColor(Color.White),
        strokeLineWidth = 2.2f,
        strokeLineCap = StrokeCap.Round
    ) {
        moveTo(5f, 12f)
        horizontalLineTo(19f)
    }
}.build()

internal val WizardAdd: ImageVector = ImageVector.Builder(
    name = "WizardAdd",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f
).apply {
    path(
        stroke = SolidColor(Color.White),
        strokeLineWidth = 2.2f,
        strokeLineCap = StrokeCap.Round
    ) {
        moveTo(12f, 5f)
        verticalLineTo(19f)
        moveTo(5f, 12f)
        horizontalLineTo(19f)
    }
}.build()

internal val WizardTrophy: ImageVector = ImageVector.Builder(
    name = "WizardTrophy",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f
).apply {
    path(
        stroke = SolidColor(Color.White),
        strokeLineWidth = 1.8f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round
    ) {
        moveTo(6f, 4f)
        horizontalLineTo(18f)
        verticalLineTo(9f)
        curveTo(18f, 12.3f, 15.3f, 15f, 12f, 15f)
        curveTo(8.7f, 15f, 6f, 12.3f, 6f, 9f)
        close()
        moveTo(6f, 6f)
        horizontalLineTo(3f)
        curveTo(3f, 8.5f, 4.5f, 10.5f, 6f, 11f)
        moveTo(18f, 6f)
        horizontalLineTo(21f)
        curveTo(21f, 8.5f, 19.5f, 10.5f, 18f, 11f)
        moveTo(12f, 15f)
        verticalLineTo(19f)
        moveTo(8f, 21f)
        horizontalLineTo(16f)
    }
}.build()

internal val WizardMonitoring: ImageVector = ImageVector.Builder(
    name = "WizardMonitoring",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f
).apply {
    path(
        stroke = SolidColor(Color.White),
        strokeLineWidth = 1.8f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round
    ) {
        moveTo(3f, 3f)
        verticalLineTo(21f)
        horizontalLineTo(21f)
        moveTo(7f, 14f)
        lineTo(11f, 9f)
        lineTo(15f, 13f)
        lineTo(20f, 6f)
    }
}.build()

internal val WizardCalculate: ImageVector = ImageVector.Builder(
    name = "WizardCalculate",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f
).apply {
    path(
        stroke = SolidColor(Color.White),
        strokeLineWidth = 1.8f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round
    ) {
        moveTo(19f, 3f)
        horizontalLineTo(5f)
        curveTo(3.9f, 3f, 3f, 3.9f, 3f, 5f)
        verticalLineTo(19f)
        curveTo(3f, 20.1f, 3.9f, 21f, 5f, 21f)
        horizontalLineTo(19f)
        curveTo(20.1f, 21f, 21f, 20.1f, 21f, 19f)
        verticalLineTo(5f)
        curveTo(21f, 3.9f, 20.1f, 3f, 19f, 3f)
        close()
        moveTo(6f, 7f)
        horizontalLineTo(18f)
        moveTo(8f, 11f)
        horizontalLineTo(8.01f)
        moveTo(12f, 11f)
        horizontalLineTo(12.01f)
        moveTo(16f, 11f)
        horizontalLineTo(16.01f)
        moveTo(8f, 15f)
        horizontalLineTo(8.01f)
        moveTo(12f, 15f)
        horizontalLineTo(12.01f)
        moveTo(16f, 15f)
        horizontalLineTo(16.01f)
    }
}.build()

internal val WizardInventory: ImageVector = ImageVector.Builder(
    name = "WizardInventory",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f
).apply {
    path(
        stroke = SolidColor(Color.White),
        strokeLineWidth = 1.8f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round
    ) {
        moveTo(4f, 4f)
        horizontalLineTo(20f)
        verticalLineTo(8f)
        horizontalLineTo(4f)
        close()
        moveTo(5f, 8f)
        verticalLineTo(20f)
        horizontalLineTo(19f)
        verticalLineTo(8f)
        moveTo(10f, 12f)
        horizontalLineTo(14f)
    }
}.build()

internal val WizardPercent: ImageVector = ImageVector.Builder(
    name = "WizardPercent",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f
).apply {
    path(
        stroke = SolidColor(Color.White),
        strokeLineWidth = 2f,
        strokeLineCap = StrokeCap.Round
    ) {
        moveTo(19f, 5f)
        lineTo(5f, 19f)
    }
    path(
        stroke = SolidColor(Color.White),
        strokeLineWidth = 2f
    ) {
        moveTo(7.5f, 5f)
        curveTo(8.88f, 5f, 10f, 6.12f, 10f, 7.5f)
        curveTo(10f, 8.88f, 8.88f, 10f, 7.5f, 10f)
        curveTo(6.12f, 10f, 5f, 8.88f, 5f, 7.5f)
        curveTo(5f, 6.12f, 6.12f, 5f, 7.5f, 5f)
        close()
        moveTo(16.5f, 14f)
        curveTo(17.88f, 14f, 19f, 15.12f, 19f, 16.5f)
        curveTo(19f, 17.88f, 17.88f, 19f, 16.5f, 19f)
        curveTo(15.12f, 19f, 14f, 17.88f, 14f, 16.5f)
        curveTo(14f, 15.12f, 15.12f, 14f, 16.5f, 14f)
        close()
    }
}.build()

internal val WizardDiamond: ImageVector = ImageVector.Builder(
    name = "WizardDiamond",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f
).apply {
    path(
        stroke = SolidColor(Color.White),
        strokeLineWidth = 1.8f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round
    ) {
        moveTo(6f, 3f)
        lineTo(18f, 3f)
        lineTo(22f, 9f)
        lineTo(12f, 21f)
        lineTo(2f, 9f)
        close()
        moveTo(2f, 9f)
        horizontalLineTo(22f)
        moveTo(8f, 9f)
        lineTo(12f, 21f)
        lineTo(16f, 9f)
        moveTo(8f, 9f)
        lineTo(6f, 3f)
        moveTo(16f, 9f)
        lineTo(18f, 3f)
        moveTo(12f, 3f)
        lineTo(10f, 9f)
    }
}.build()
