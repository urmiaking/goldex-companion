package com.goldex.companion.ui.dashboard

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * Native, zero-dependency lightweight vector icons for DashboardScreen & LiveRatesScreen.
 * Designed strictly following Google Stitch luxury aesthetics and Persian Sovereign Aurum tokens.
 */

internal val DashWalletVector: ImageVector = ImageVector.Builder(
    name = "DashWallet",
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
        moveTo(21f, 18f)
        verticalLineTo(6f)
        curveTo(21f, 4.9f, 20.1f, 4f, 19f, 4f)
        horizontalLineTo(5f)
        curveTo(3.9f, 4f, 3f, 4.9f, 3f, 6f)
        verticalLineTo(18f)
        curveTo(3f, 19.1f, 3.9f, 20f, 5f, 20f)
        horizontalLineTo(19f)
        curveTo(20.1f, 20f, 21f, 19.1f, 21f, 18f)
        close()
        moveTo(3f, 8.5f)
        horizontalLineTo(21f)
        moveTo(16f, 14f)
        curveTo(16f, 14.55f, 16.45f, 15f, 17f, 15f)
        curveTo(17.55f, 15f, 18f, 14.55f, 18f, 14f)
        curveTo(18f, 13.45f, 17.55f, 13f, 17f, 13f)
        curveTo(16.45f, 13f, 16f, 13.45f, 16f, 14f)
        close()
    }
}.build()

internal val DashTrendingUpVector: ImageVector = ImageVector.Builder(
    name = "DashTrendingUp",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f
).apply {
    path(
        stroke = SolidColor(Color.White),
        strokeLineWidth = 2.2f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round
    ) {
        moveTo(23f, 6f)
        lineTo(13.5f, 15.5f)
        lineTo(8.5f, 10.5f)
        lineTo(1f, 18f)
        moveTo(17f, 6f)
        horizontalLineTo(23f)
        verticalLineTo(12f)
    }
}.build()

internal val DashTrendingDownVector: ImageVector = ImageVector.Builder(
    name = "DashTrendingDown",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f
).apply {
    path(
        stroke = SolidColor(Color.White),
        strokeLineWidth = 2.2f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round
    ) {
        moveTo(23f, 18f)
        lineTo(13.5f, 8.5f)
        lineTo(8.5f, 13.5f)
        lineTo(1f, 6f)
        moveTo(17f, 18f)
        horizontalLineTo(23f)
        verticalLineTo(12f)
    }
}.build()

internal val DashCalculateVector: ImageVector = ImageVector.Builder(
    name = "DashCalculate",
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
        moveTo(7f, 7f)
        horizontalLineTo(17f)
        moveTo(7f, 12f)
        horizontalLineTo(9f)
        moveTo(15f, 12f)
        horizontalLineTo(17f)
        moveTo(7f, 16f)
        horizontalLineTo(9f)
        moveTo(15f, 16f)
        horizontalLineTo(17f)
        moveTo(11f, 12f)
        horizontalLineTo(13f)
        moveTo(11f, 16f)
        horizontalLineTo(13f)
    }
}.build()

internal val DashInvoiceVector: ImageVector = ImageVector.Builder(
    name = "DashInvoice",
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
        moveTo(14f, 2f)
        horizontalLineTo(6f)
        curveTo(4.9f, 2f, 4f, 2.9f, 4f, 4f)
        verticalLineTo(20f)
        curveTo(4f, 21.1f, 4.9f, 22f, 6f, 22f)
        horizontalLineTo(18f)
        curveTo(19.1f, 22f, 20f, 21.1f, 20f, 20f)
        verticalLineTo(8f)
        lineTo(14f, 2f)
        close()
        moveTo(14f, 2f)
        verticalLineTo(8f)
        horizontalLineTo(20f)
        moveTo(16f, 13f)
        horizontalLineTo(8f)
        moveTo(16f, 17f)
        horizontalLineTo(8f)
        moveTo(10f, 9f)
        horizontalLineTo(8f)
    }
}.build()

internal val DashBalanceVector: ImageVector = ImageVector.Builder(
    name = "DashBalance",
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
        moveTo(4f, 7f)
        horizontalLineTo(20f)
        moveTo(4f, 7f)
        lineTo(2f, 14f)
        curveTo(2f, 15.5f, 4.5f, 15.5f, 4.5f, 14f)
        lineTo(4f, 7f)
        moveTo(20f, 7f)
        lineTo(18f, 14f)
        curveTo(18f, 15.5f, 20.5f, 15.5f, 20.5f, 14f)
        lineTo(20f, 7f)
        moveTo(8f, 21f)
        horizontalLineTo(16f)
    }
}.build()

internal val DashBubbleVector: ImageVector = ImageVector.Builder(
    name = "DashBubble",
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
        moveTo(12f, 12f)
        moveToRelative(-4f, 0f)
        arcTo(4f, 4f, 0f, isMoreThanHalf = true, isPositiveArc = true, 16f, 12f)
        arcTo(4f, 4f, 0f, isMoreThanHalf = true, isPositiveArc = true, 8f, 12f)
        moveTo(18f, 6f)
        moveToRelative(-2f, 0f)
        arcTo(2f, 2f, 0f, isMoreThanHalf = true, isPositiveArc = true, 20f, 6f)
        arcTo(2f, 2f, 0f, isMoreThanHalf = true, isPositiveArc = true, 16f, 6f)
        moveTo(6f, 18f)
        moveToRelative(-2.5f, 0f)
        arcTo(2.5f, 2.5f, 0f, isMoreThanHalf = true, isPositiveArc = true, 8.5f, 18f)
        arcTo(2.5f, 2.5f, 0f, isMoreThanHalf = true, isPositiveArc = true, 3.5f, 18f)
    }
}.build()

internal val DashCandlestickVector: ImageVector = ImageVector.Builder(
    name = "DashCandlestick",
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
        verticalLineTo(10f)
        moveTo(17f, 18f)
        verticalLineTo(21f)
        moveTo(15f, 10f)
        horizontalLineTo(19f)
        verticalLineTo(18f)
        horizontalLineTo(15f)
        close()
    }
}.build()

internal val DashLedgerVector: ImageVector = ImageVector.Builder(
    name = "DashLedger",
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
        moveTo(12f, 6f)
        curveTo(10f, 4f, 6f, 4f, 3f, 4.5f)
        verticalLineTo(19f)
        curveTo(6f, 18.5f, 10f, 18.5f, 12f, 20.5f)
        curveTo(14f, 18.5f, 18f, 18.5f, 21f, 19f)
        verticalLineTo(4.5f)
        curveTo(18f, 4f, 14f, 4f, 12f, 6f)
        close()
        moveTo(12f, 6f)
        verticalLineTo(20.5f)
    }
}.build()

internal val DashDiamondVector: ImageVector = ImageVector.Builder(
    name = "DashDiamond",
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
        lineTo(12f, 21f)
        lineTo(2f, 9f)
        close()
        moveTo(2f, 9f)
        horizontalLineTo(22f)
        moveTo(12f, 21f)
        lineTo(8f, 9f)
        moveTo(12f, 21f)
        lineTo(16f, 9f)
        moveTo(6f, 3f)
        lineTo(8f, 9f)
        moveTo(18f, 3f)
        lineTo(16f, 9f)
    }
}.build()

internal val DashCoinVector: ImageVector = ImageVector.Builder(
    name = "DashCoin",
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
        moveTo(12f, 2f)
        curveTo(6.48f, 2f, 2f, 6.48f, 2f, 12f)
        curveTo(2f, 17.52f, 6.48f, 22f, 12f, 22f)
        curveTo(17.52f, 22f, 22f, 17.52f, 22f, 12f)
        curveTo(22f, 6.48f, 17.52f, 2f, 12f, 2f)
        close()
        moveTo(12f, 7f)
        verticalLineTo(17f)
        moveTo(9.5f, 9.5f)
        curveTo(9.5f, 8.5f, 10.5f, 8f, 12f, 8f)
        curveTo(13.5f, 8f, 14.5f, 8.7f, 14.5f, 10f)
        curveTo(14.5f, 11.5f, 13f, 12f, 12f, 12f)
        curveTo(11f, 12f, 9.5f, 12.5f, 9.5f, 14f)
        curveTo(9.5f, 15.3f, 10.5f, 16f, 12f, 16f)
        curveTo(13.5f, 16f, 14.5f, 15.5f, 14.5f, 14.5f)
    }
}.build()

internal val DashIngotVector: ImageVector = ImageVector.Builder(
    name = "DashIngot",
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
        moveTo(3f, 7f)
        lineTo(12f, 2f)
        lineTo(21f, 7f)
        lineTo(21f, 17f)
        lineTo(12f, 22f)
        lineTo(3f, 17f)
        close()
        moveTo(3f, 7f)
        lineTo(12f, 12f)
        lineTo(21f, 7f)
        moveTo(12f, 12f)
        verticalLineTo(22f)
    }
}.build()

internal val DashTollVector: ImageVector = ImageVector.Builder(
    name = "DashToll",
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
        moveTo(12f, 4f)
        curveTo(7.58f, 4f, 4f, 7.58f, 4f, 12f)
        curveTo(4f, 16.42f, 7.58f, 20f, 12f, 20f)
        curveTo(16.42f, 20f, 20f, 16.42f, 20f, 12f)
        curveTo(20f, 7.58f, 16.42f, 4f, 12f, 4f)
        close()
        moveTo(12f, 8f)
        curveTo(9.79f, 8f, 8f, 9.79f, 8f, 12f)
        curveTo(8f, 14.21f, 9.79f, 16f, 12f, 16f)
        curveTo(14.21f, 16f, 16f, 14.21f, 16f, 12f)
        curveTo(16f, 9.79f, 14.21f, 8f, 12f, 8f)
        close()
    }
}.build()

internal val DashGlobeVector: ImageVector = ImageVector.Builder(
    name = "DashGlobe",
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
        moveTo(12f, 2f)
        curveTo(6.48f, 2f, 2f, 6.48f, 2f, 12f)
        curveTo(2f, 17.52f, 6.48f, 22f, 12f, 22f)
        curveTo(17.52f, 22f, 22f, 17.52f, 22f, 12f)
        curveTo(22f, 6.48f, 17.52f, 2f, 12f, 2f)
        close()
        moveTo(2f, 12f)
        horizontalLineTo(22f)
        moveTo(12f, 2f)
        curveTo(14.5f, 5.5f, 16f, 8.5f, 16f, 12f)
        curveTo(16f, 15.5f, 14.5f, 18.5f, 12f, 22f)
        curveTo(9.5f, 18.5f, 8f, 15.5f, 8f, 12f)
        curveTo(8f, 8.5f, 9.5f, 5.5f, 12f, 2f)
    }
}.build()

internal val DashScheduleVector: ImageVector = ImageVector.Builder(
    name = "DashSchedule",
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
        moveTo(12f, 2f)
        curveTo(6.5f, 2f, 2f, 6.5f, 2f, 12f)
        curveTo(2f, 17.5f, 6.5f, 22f, 12f, 22f)
        curveTo(17.5f, 22f, 22f, 17.5f, 22f, 12f)
        curveTo(22f, 6.5f, 17.5f, 2f, 12f, 2f)
        close()
        moveTo(12f, 6f)
        verticalLineTo(12f)
        lineTo(16f, 14f)
    }
}.build()

internal val DashTuneVector: ImageVector = ImageVector.Builder(
    name = "DashTune",
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
        moveTo(3f, 6f)
        horizontalLineTo(9f)
        moveTo(13f, 6f)
        horizontalLineTo(21f)
        moveTo(9f, 4f)
        verticalLineTo(8f)
        moveTo(3f, 12f)
        horizontalLineTo(15f)
        moveTo(19f, 12f)
        horizontalLineTo(21f)
        moveTo(15f, 10f)
        verticalLineTo(14f)
        moveTo(3f, 18f)
        horizontalLineTo(7f)
        moveTo(11f, 18f)
        horizontalLineTo(21f)
        moveTo(7f, 16f)
        verticalLineTo(20f)
    }
}.build()

internal val DashLayersVector: ImageVector = ImageVector.Builder(
    name = "DashLayers",
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
        moveTo(12f, 2f)
        lineTo(2f, 7f)
        lineTo(12f, 12f)
        lineTo(22f, 7f)
        close()
        moveTo(2f, 12f)
        lineTo(12f, 17f)
        lineTo(22f, 12f)
        moveTo(2f, 17f)
        lineTo(12f, 22f)
        lineTo(22f, 17f)
    }
}.build()

internal val DashVerifiedVector: ImageVector = ImageVector.Builder(
    name = "DashVerified",
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
        moveTo(12f, 2f)
        lineTo(15.09f, 3.26f)
        lineTo(18.2f, 2.34f)
        lineTo(19.86f, 5.22f)
        lineTo(22.84f, 6.74f)
        lineTo(22.46f, 10.06f)
        lineTo(23.95f, 13f)
        lineTo(22.46f, 15.94f)
        lineTo(22.84f, 19.26f)
        lineTo(19.86f, 20.78f)
        lineTo(18.2f, 23.66f)
        lineTo(15.09f, 22.74f)
        lineTo(12f, 24f)
        lineTo(8.91f, 22.74f)
        lineTo(5.8f, 23.66f)
        lineTo(4.14f, 20.78f)
        lineTo(1.16f, 19.26f)
        lineTo(1.54f, 15.94f)
        lineTo(0.05f, 13f)
        lineTo(1.54f, 10.06f)
        lineTo(1.16f, 6.74f)
        lineTo(4.14f, 5.22f)
        lineTo(5.8f, 2.34f)
        lineTo(8.91f, 3.26f)
        close()
        moveTo(9f, 12f)
        lineTo(11f, 14f)
        lineTo(15f, 10f)
    }
}.build()

internal val DashChevronLeft: ImageVector = ImageVector.Builder(
    name = "DashChevronLeft",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f
).apply {
    path(
        stroke = SolidColor(Color.White),
        strokeLineWidth = 2.2f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round
    ) {
        moveTo(15f, 18f)
        lineTo(9f, 12f)
        lineTo(15f, 6f)
    }
}.build()

