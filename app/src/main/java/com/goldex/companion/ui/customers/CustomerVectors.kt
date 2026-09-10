package com.goldex.companion.ui.customers

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

internal val LedgerAccountBalanceVector: ImageVector = ImageVector.Builder(
    name = "LedgerAccountBalance",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f
).apply {
    path(fill = SolidColor(Color.White)) {
        moveTo(4f, 10f)
        verticalLineTo(17f)
        horizontalLineTo(7f)
        verticalLineTo(10f)
        horizontalLineTo(4f)
        close()
        moveTo(10f, 10f)
        verticalLineTo(17f)
        horizontalLineTo(13f)
        verticalLineTo(10f)
        horizontalLineTo(10f)
        close()
        moveTo(2f, 22f)
        horizontalLineTo(22f)
        verticalLineTo(19f)
        horizontalLineTo(2f)
        verticalLineTo(22f)
        close()
        moveTo(16f, 10f)
        verticalLineTo(17f)
        horizontalLineTo(19f)
        verticalLineTo(10f)
        horizontalLineTo(16f)
        close()
        moveTo(12f, 1f)
        lineTo(2f, 6f)
        verticalLineTo(8f)
        horizontalLineTo(22f)
        verticalLineTo(6f)
        lineTo(12f, 1f)
        close()
    }
}.build()

internal val LedgerScaleVector: ImageVector = ImageVector.Builder(
    name = "LedgerScale",
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
        moveTo(12f, 3f)
        lineTo(4f, 7f)
        moveTo(12f, 3f)
        lineTo(20f, 7f)
        moveTo(4f, 7f)
        lineTo(1f, 14f)
        curveTo(1f, 16f, 7f, 16f, 7f, 14f)
        lineTo(4f, 7f)
        moveTo(20f, 7f)
        lineTo(17f, 14f)
        curveTo(17f, 16f, 23f, 16f, 23f, 14f)
        lineTo(20f, 7f)
    }
}.build()

internal val LedgerArrowReceiveVector: ImageVector = ImageVector.Builder(
    name = "LedgerArrowReceive",
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
        moveTo(5f, 19f)
        lineTo(19f, 5f)
        moveTo(5f, 9f)
        verticalLineTo(19f)
        horizontalLineTo(15f)
    }
}.build()

internal val LedgerArrowPayVector: ImageVector = ImageVector.Builder(
    name = "LedgerArrowPay",
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
        moveTo(5f, 19f)
        lineTo(19f, 5f)
        moveTo(9f, 5f)
        horizontalLineTo(19f)
        verticalLineTo(15f)
    }
}.build()

internal val LedgerReceiptVector: ImageVector = ImageVector.Builder(
    name = "LedgerReceipt",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f
).apply {
    path(fill = SolidColor(Color.White)) {
        moveTo(19.5f, 3.5f)
        lineTo(18f, 2f)
        lineTo(16.5f, 3.5f)
        lineTo(15f, 2f)
        lineTo(13.5f, 3.5f)
        lineTo(12f, 2f)
        lineTo(10.5f, 3.5f)
        lineTo(9f, 2f)
        lineTo(7.5f, 3.5f)
        lineTo(6f, 2f)
        lineTo(4.5f, 3.5f)
        lineTo(3f, 2f)
        verticalLineTo(22f)
        lineTo(4.5f, 20.5f)
        lineTo(6f, 22f)
        lineTo(7.5f, 20.5f)
        lineTo(9f, 22f)
        lineTo(10.5f, 20.5f)
        lineTo(12f, 22f)
        lineTo(13.5f, 20.5f)
        lineTo(15f, 22f)
        lineTo(16.5f, 20.5f)
        lineTo(18f, 22f)
        lineTo(19.5f, 20.5f)
        lineTo(21f, 22f)
        verticalLineTo(2f)
        lineTo(19.5f, 3.5f)
        close()
        moveTo(18f, 17f)
        horizontalLineTo(6f)
        verticalLineTo(15f)
        horizontalLineTo(18f)
        verticalLineTo(17f)
        close()
        moveTo(18f, 13f)
        horizontalLineTo(6f)
        verticalLineTo(11f)
        horizontalLineTo(18f)
        verticalLineTo(13f)
        close()
        moveTo(18f, 9f)
        horizontalLineTo(6f)
        verticalLineTo(7f)
        horizontalLineTo(18f)
        verticalLineTo(9f)
        close()
    }
}.build()

internal val LedgerVerifiedVector: ImageVector = ImageVector.Builder(
    name = "LedgerVerified",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f
).apply {
    path(fill = SolidColor(Color.White)) {
        moveTo(12f, 1f)
        lineTo(15.09f, 4.26f)
        lineTo(18.88f, 3.82f)
        lineTo(20.36f, 7.33f)
        lineTo(23.54f, 9.47f)
        lineTo(22.82f, 13.23f)
        lineTo(24f, 16.89f)
        lineTo(20.82f, 19.03f)
        lineTo(19.34f, 22.54f)
        lineTo(15.55f, 22.1f)
        lineTo(12.46f, 24f)
        lineTo(9.37f, 22.1f)
        lineTo(5.58f, 22.54f)
        lineTo(4.1f, 19.03f)
        lineTo(0.92f, 16.89f)
        lineTo(2.1f, 13.23f)
        lineTo(1.38f, 9.47f)
        lineTo(4.56f, 7.33f)
        lineTo(6.04f, 3.82f)
        lineTo(9.83f, 4.26f)
        close()
        moveTo(10f, 16.5f)
        lineTo(18f, 8.5f)
        lineTo(16.59f, 7.09f)
        lineTo(10f, 13.67f)
        lineTo(7.41f, 11.09f)
        lineTo(6f, 12.5f)
        lineTo(10f, 16.5f)
        close()
    }
}.build()

internal val LedgerPhoneVector: ImageVector = ImageVector.Builder(
    name = "LedgerPhone",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f
).apply {
    path(fill = SolidColor(Color.White)) {
        moveTo(6.62f, 10.79f)
        curveTo(8.06f, 13.62f, 10.38f, 15.94f, 13.21f, 17.38f)
        lineTo(15.41f, 15.18f)
        curveTo(15.69f, 14.9f, 16.08f, 14.82f, 16.43f, 14.94f)
        curveTo(17.55f, 15.31f, 18.76f, 15.51f, 20f, 15.51f)
        curveTo(20.55f, 15.51f, 21f, 15.96f, 21f, 16.51f)
        verticalLineTo(20f)
        curveTo(21f, 20.55f, 20.55f, 21f, 20f, 21f)
        curveTo(10.61f, 21f, 3f, 13.39f, 3f, 4f)
        curveTo(3f, 3.45f, 3.45f, 3f, 4f, 3f)
        horizontalLineTo(7.5f)
        curveTo(8.05f, 3f, 8.5f, 3.45f, 8.5f, 4f)
        curveTo(8.5f, 5.25f, 8.7f, 6.45f, 9.07f, 7.57f)
        curveTo(9.18f, 7.92f, 9.1f, 8.31f, 8.82f, 8.59f)
        lineTo(6.62f, 10.79f)
        close()
    }
}.build()

internal val LedgerDiamondVector: ImageVector = ImageVector.Builder(
    name = "LedgerDiamond",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f
).apply {
    path(fill = SolidColor(Color.White)) {
        moveTo(6f, 2f)
        lineTo(18f, 2f)
        lineTo(22f, 8f)
        lineTo(12f, 22f)
        lineTo(2f, 8f)
        close()
        moveTo(7.41f, 4f)
        lineTo(4.74f, 8f)
        horizontalLineTo(8.59f)
        lineTo(10.02f, 4f)
        close()
        moveTo(11.91f, 4f)
        lineTo(10.48f, 8f)
        horizontalLineTo(13.52f)
        lineTo(12.09f, 4f)
        close()
        moveTo(13.98f, 4f)
        lineTo(15.41f, 8f)
        horizontalLineTo(19.26f)
        lineTo(16.59f, 4f)
        close()
    }
}.build()

internal val LedgerWalletVector: ImageVector = ImageVector.Builder(
    name = "LedgerWallet",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f
).apply {
    path(fill = SolidColor(Color.White)) {
        moveTo(21f, 18f)
        curveTo(21f, 19.1f, 20.1f, 20f, 19f, 20f)
        horizontalLineTo(5f)
        curveTo(3.9f, 20f, 3f, 19.1f, 3f, 18f)
        verticalLineTo(6f)
        curveTo(3f, 4.9f, 3.9f, 4f, 5f, 4f)
        horizontalLineTo(19f)
        curveTo(20.1f, 4f, 21f, 4.9f, 21f, 6f)
        verticalLineTo(9f)
        horizontalLineTo(12f)
        curveTo(10.9f, 9f, 10f, 9.9f, 10f, 11f)
        verticalLineTo(13f)
        curveTo(10f, 14.1f, 10.9f, 15f, 12f, 15f)
        horizontalLineTo(21f)
        verticalLineTo(18f)
        close()
        moveTo(12f, 13f)
        horizontalLineTo(21f)
        verticalLineTo(11f)
        horizontalLineTo(12f)
        verticalLineTo(13f)
        close()
        moveTo(16f, 12.5f)
        curveTo(16.55f, 12.5f, 17f, 12.05f, 17f, 11.5f)
        curveTo(17f, 10.95f, 16.55f, 10.5f, 16f, 10.5f)
        curveTo(15.45f, 10.5f, 15f, 10.95f, 15f, 11.5f)
        curveTo(15f, 12.05f, 15.45f, 12.5f, 16f, 12.5f)
        close()
    }
}.build()

internal val LedgerShareVector: ImageVector = ImageVector.Builder(
    name = "LedgerShare",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f
).apply {
    path(fill = SolidColor(Color.White)) {
        moveTo(18f, 16.08f)
        curveTo(17.24f, 16.08f, 16.56f, 16.38f, 16.04f, 16.85f)
        lineTo(8.91f, 12.7f)
        curveTo(8.96f, 12.47f, 9f, 12.24f, 9f, 12f)
        curveTo(9f, 11.76f, 8.96f, 11.53f, 8.91f, 11.3f)
        lineTo(15.96f, 7.19f)
        curveTo(16.5f, 7.69f, 17.21f, 8f, 18f, 8f)
        curveTo(19.66f, 8f, 21f, 6.66f, 21f, 5f)
        curveTo(21f, 3.34f, 19.66f, 2f, 18f, 2f)
        curveTo(16.34f, 2f, 15f, 3.34f, 15f, 5f)
        curveTo(15f, 5.24f, 15.04f, 5.47f, 15.09f, 5.7f)
        lineTo(8.04f, 9.81f)
        curveTo(7.5f, 9.31f, 6.79f, 9f, 6f, 9f)
        curveTo(4.34f, 9f, 3f, 10.34f, 3f, 12f)
        curveTo(3f, 13.66f, 4.34f, 15f, 6f, 15f)
        curveTo(6.79f, 15f, 7.5f, 14.69f, 8.04f, 14.19f)
        lineTo(15.16f, 18.35f)
        curveTo(15.11f, 18.56f, 15.08f, 18.78f, 15.08f, 19f)
        curveTo(15.08f, 20.61f, 16.39f, 21.92f, 18f, 21.92f)
        curveTo(19.61f, 21.92f, 20.92f, 20.61f, 20.92f, 19f)
        curveTo(20.92f, 17.39f, 19.61f, 16.08f, 18f, 16.08f)
        close()
    }
}.build()
