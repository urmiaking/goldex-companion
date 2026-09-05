package com.goldex.companion.ui.calculator

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

internal val CalcCalculate: ImageVector = ImageVector.Builder(
    name = "CalcCalculate",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f
).apply {
    path(
        stroke = SolidColor(Color.White),
        strokeLineWidth = 1.9f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round
    ) {
        moveTo(4f, 4f)
        horizontalLineTo(20f)
        verticalLineTo(20f)
        horizontalLineTo(4f)
        close()
        moveTo(7f, 8f); horizontalLineTo(17f)
        moveTo(7f, 12f); horizontalLineTo(9f)
        moveTo(11f, 12f); horizontalLineTo(13f)
        moveTo(15f, 12f); horizontalLineTo(17f)
        moveTo(7f, 16f); horizontalLineTo(9f)
        moveTo(11f, 16f); horizontalLineTo(13f)
        moveTo(15f, 16f); horizontalLineTo(17f)
    }
}.build()

internal val CalcRestartAlt: ImageVector = ImageVector.Builder(
    name = "CalcRestartAlt",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f
).apply {
    path(
        stroke = SolidColor(Color.White),
        strokeLineWidth = 2f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round
    ) {
        moveTo(12f, 5f)
        curveTo(8.13f, 5f, 5f, 8.13f, 5f, 12f)
        curveTo(5f, 15.87f, 8.13f, 19f, 12f, 19f)
        curveTo(15.87f, 19f, 19f, 15.87f, 19f, 12f)
        curveTo(19f, 9.8f, 18f, 7.84f, 16.4f, 6.5f)
        moveTo(12f, 2f)
        lineTo(12f, 6f)
        lineTo(8f, 4f)
    }
}.build()

internal val CalcStars: ImageVector = ImageVector.Builder(
    name = "CalcStars",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f
).apply {
    path(fill = SolidColor(Color.White)) {
        moveTo(12f, 2f)
        lineTo(15.09f, 8.26f)
        lineTo(22f, 9.27f)
        lineTo(17f, 14.14f)
        lineTo(18.18f, 21.02f)
        lineTo(12f, 17.77f)
        lineTo(5.82f, 21.02f)
        lineTo(7f, 14.14f)
        lineTo(2f, 9.27f)
        lineTo(8.91f, 8.26f)
        close()
    }
}.build()

internal val CalcScale: ImageVector = ImageVector.Builder(
    name = "CalcScale",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f
).apply {
    path(
        stroke = SolidColor(Color.White),
        strokeLineWidth = 1.9f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round
    ) {
        moveTo(12f, 3f)
        verticalLineTo(21f)
        moveTo(5f, 21f)
        horizontalLineTo(19f)
        moveTo(4f, 7f)
        horizontalLineTo(20f)
        moveTo(4f, 7f)
        lineTo(2f, 13f)
        horizontalLineTo(8f)
        lineTo(6f, 7f)
        moveTo(18f, 7f)
        lineTo(16f, 13f)
        horizontalLineTo(22f)
        lineTo(20f, 7f)
    }
}.build()

internal val CalcArchitecture: ImageVector = ImageVector.Builder(
    name = "CalcArchitecture",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f
).apply {
    path(
        stroke = SolidColor(Color.White),
        strokeLineWidth = 1.9f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round
    ) {
        moveTo(12f, 3f)
        lineTo(4f, 20f)
        moveTo(12f, 3f)
        lineTo(20f, 20f)
        moveTo(7f, 14f)
        horizontalLineTo(17f)
        moveTo(12f, 3f)
        verticalLineTo(7f)
    }
}.build()

internal val CalcRemove: ImageVector = ImageVector.Builder(
    name = "CalcRemove",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f
).apply {
    path(
        stroke = SolidColor(Color.White),
        strokeLineWidth = 2.4f,
        strokeLineCap = StrokeCap.Round
    ) {
        moveTo(5f, 12f)
        horizontalLineTo(19f)
    }
}.build()

internal val CalcPercent: ImageVector = ImageVector.Builder(
    name = "CalcPercent",
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
        moveTo(7.5f, 7.5f); lineTo(7.51f, 7.5f)
        moveTo(16.5f, 16.5f); lineTo(16.51f, 16.5f)
    }
}.build()

internal val CalcDiamond: ImageVector = ImageVector.Builder(
    name = "CalcDiamond",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f
).apply {
    path(
        stroke = SolidColor(Color.White),
        strokeLineWidth = 1.9f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round
    ) {
        moveTo(6f, 3f)
        horizontalLineTo(18f)
        lineTo(22f, 9f)
        lineTo(12f, 22f)
        lineTo(2f, 9f)
        close()
        moveTo(2f, 9f)
        horizontalLineTo(22f)
        moveTo(12f, 22f)
        lineTo(9f, 9f)
        lineTo(12f, 3f)
        lineTo(15f, 9f)
        lineTo(12f, 22f)
    }
}.build()

internal val CalcReceiptLong: ImageVector = ImageVector.Builder(
    name = "CalcReceiptLong",
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
        verticalLineTo(20f)
        lineTo(17f, 18.5f)
        lineTo(14f, 20f)
        lineTo(11f, 18.5f)
        lineTo(8f, 20f)
        lineTo(5f, 18.5f)
        lineTo(4f, 20f)
        close()
        moveTo(8f, 8f); horizontalLineTo(16f)
        moveTo(8f, 12f); horizontalLineTo(14f)
    }
}.build()

internal val CalcContentCopy: ImageVector = ImageVector.Builder(
    name = "CalcContentCopy",
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
        moveTo(16f, 1f)
        horizontalLineTo(4f)
        curveTo(2.9f, 1f, 2f, 1.9f, 2f, 3f)
        verticalLineTo(17f)
        horizontalLineTo(4f)
        verticalLineTo(3f)
        horizontalLineTo(16f)
        verticalLineTo(1f)
        close()
        moveTo(19f, 5f)
        horizontalLineTo(8f)
        curveTo(6.9f, 5f, 6f, 5.9f, 6f, 7f)
        verticalLineTo(21f)
        curveTo(6f, 22.1f, 6.9f, 23f, 8f, 23f)
        horizontalLineTo(19f)
        curveTo(20.1f, 23f, 21f, 22.1f, 21f, 21f)
        verticalLineTo(7f)
        curveTo(21f, 5.9f, 20.1f, 5f, 19f, 5f)
        close()
    }
}.build()

internal val CalcPostAdd: ImageVector = ImageVector.Builder(
    name = "CalcPostAdd",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f
).apply {
    path(
        stroke = SolidColor(Color.White),
        strokeLineWidth = 1.9f,
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
        moveTo(12f, 8f); verticalLineTo(16f)
        moveTo(8f, 12f); horizontalLineTo(16f)
    }
}.build()

internal val CalcSwapHoriz: ImageVector = ImageVector.Builder(
    name = "CalcSwapHoriz",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f
).apply {
    path(
        stroke = SolidColor(Color.White),
        strokeLineWidth = 2f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round
    ) {
        moveTo(6.99f, 11f)
        lineTo(3f, 15f)
        lineTo(6.99f, 19f)
        verticalLineTo(16f)
        horizontalLineTo(14f)
        verticalLineTo(14f)
        horizontalLineTo(6.99f)
        verticalLineTo(11f)
        close()
        moveTo(21f, 9f)
        lineTo(17.01f, 5f)
        verticalLineTo(8f)
        horizontalLineTo(10f)
        verticalLineTo(10f)
        horizontalLineTo(17.01f)
        verticalLineTo(13f)
        lineTo(21f, 9f)
        close()
    }
}.build()

internal val CalcFactCheck: ImageVector = ImageVector.Builder(
    name = "CalcFactCheck",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f
).apply {
    path(
        stroke = SolidColor(Color.White),
        strokeLineWidth = 1.9f,
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
        moveTo(10f, 17f)
        lineTo(6f, 13f)
        lineTo(7.41f, 11.59f)
        lineTo(10f, 14.17f)
        lineTo(16.59f, 7.58f)
        lineTo(18f, 9f)
        lineTo(10f, 17f)
        close()
    }
}.build()

internal val CalcSync: ImageVector = ImageVector.Builder(
    name = "CalcSync",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f
).apply {
    path(
        stroke = SolidColor(Color.White),
        strokeLineWidth = 2f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round
    ) {
        moveTo(12f, 4f)
        curveTo(7.58f, 4f, 4f, 7.58f, 4f, 12f)
        curveTo(4f, 13.9f, 4.67f, 15.65f, 5.8f, 17.05f)
        moveTo(12f, 20f)
        curveTo(16.42f, 20f, 20f, 16.42f, 20f, 12f)
        curveTo(20f, 10.1f, 19.33f, 8.35f, 18.2f, 6.95f)
        moveTo(12f, 1f); lineTo(12f, 5f); lineTo(8f, 3f)
        moveTo(12f, 23f); lineTo(12f, 19f); lineTo(16f, 21f)
    }
}.build()
