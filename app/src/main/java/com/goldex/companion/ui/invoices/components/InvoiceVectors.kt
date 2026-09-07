package com.goldex.companion.ui.invoices.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

internal val InvoiceSealVector: ImageVector = ImageVector.Builder(
    name = "InvoiceSeal",
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
        moveTo(9f, 12f)
        lineTo(11f, 14f)
        lineTo(15f, 10f)
    }
}.build()

internal val InvoiceQrVector: ImageVector = ImageVector.Builder(
    name = "InvoiceQr",
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
        moveTo(3f, 3f); horizontalLineTo(9f); verticalLineTo(9f); horizontalLineTo(3f); close()
        moveTo(15f, 3f); horizontalLineTo(21f); verticalLineTo(9f); horizontalLineTo(15f); close()
        moveTo(3f, 15f); horizontalLineTo(9f); verticalLineTo(21f); horizontalLineTo(3f); close()
        moveTo(15f, 15f); horizontalLineTo(17f); verticalLineTo(17f); horizontalLineTo(15f); close()
        moveTo(19f, 15f); horizontalLineTo(21f); verticalLineTo(17f); horizontalLineTo(19f); close()
        moveTo(15f, 19f); horizontalLineTo(17f); verticalLineTo(21f); horizontalLineTo(15f); close()
        moveTo(19f, 19f); horizontalLineTo(21f); verticalLineTo(21f); horizontalLineTo(19f); close()
    }
}.build()

internal val InvoiceDiamondVector: ImageVector = ImageVector.Builder(
    name = "InvoiceDiamond",
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
        moveTo(6f, 3f); horizontalLineTo(18f); lineTo(22f, 9f); lineTo(12f, 22f); lineTo(2f, 9f); close()
        moveTo(2f, 9f); horizontalLineTo(22f)
        moveTo(12f, 22f); lineTo(8f, 9f); lineTo(10f, 3f)
        moveTo(12f, 22f); lineTo(16f, 9f); lineTo(14f, 3f)
    }
}.build()

internal val InvoiceEditVector: ImageVector = ImageVector.Builder(
    name = "InvoiceEdit",
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
        moveTo(11f, 4f); horizontalLineTo(4f); curveTo(2.9f, 4f, 2f, 4.9f, 2f, 6f); verticalLineTo(20f); curveTo(2f, 21.1f, 2.9f, 22f, 4f, 22f); horizontalLineTo(18f); curveTo(19.1f, 22f, 20f, 21.1f, 20f, 20f); verticalLineTo(13f)
        moveTo(18.5f, 2.5f); lineTo(21.5f, 5.5f); lineTo(12f, 15f); horizontalLineTo(9f); verticalLineTo(12f); close()
    }
}.build()

internal val InvoiceTrashVector: ImageVector = ImageVector.Builder(
    name = "InvoiceTrash",
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
        moveTo(3f, 6f); horizontalLineTo(21f)
        moveTo(19f, 6f); verticalLineTo(20f); curveTo(19f, 21.1f, 18.1f, 22f, 17f, 22f); horizontalLineTo(7f); curveTo(5.9f, 22f, 5f, 21.1f, 5f, 20f); verticalLineTo(6f)
        moveTo(8f, 6f); verticalLineTo(4f); curveTo(8f, 2.9f, 8.9f, 2f, 10f, 2f); horizontalLineTo(14f); curveTo(15.1f, 2f, 16f, 2.9f, 16f, 4f); verticalLineTo(6f)
        moveTo(10f, 11f); verticalLineTo(17f)
        moveTo(14f, 11f); verticalLineTo(17f)
    }
}.build()

internal val InvoicePlusVector: ImageVector = ImageVector.Builder(
    name = "InvoicePlus",
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
        moveTo(12f, 5f); verticalLineTo(19f)
        moveTo(5f, 12f); horizontalLineTo(19f)
    }
}.build()

internal val InvoiceCheckVector: ImageVector = ImageVector.Builder(
    name = "InvoiceCheck",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f
).apply {
    path(
        stroke = SolidColor(Color.White),
        strokeLineWidth = 2.4f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round
    ) {
        moveTo(4.5f, 12.5f); lineTo(10f, 18f); lineTo(19.5f, 6.5f)
    }
}.build()

internal val InvoiceCloseVector: ImageVector = ImageVector.Builder(
    name = "InvoiceClose",
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
        moveTo(6f, 18f); lineTo(18f, 6f)
        moveTo(6f, 6f); lineTo(18f, 18f)
    }
}.build()

internal val InvoicePdfVector: ImageVector = ImageVector.Builder(
    name = "InvoicePdf",
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
        moveTo(14f, 2f); horizontalLineTo(6f); curveTo(4.9f, 2f, 4f, 2.9f, 4f, 4f); verticalLineTo(20f); curveTo(4f, 21.1f, 4.9f, 22f, 6f, 22f); horizontalLineTo(18f); curveTo(19.1f, 22f, 20f, 21.1f, 20f, 20f); verticalLineTo(8f); lineTo(14f, 2f); close()
        moveTo(14f, 2f); verticalLineTo(8f); horizontalLineTo(20f)
        moveTo(12f, 18f); verticalLineTo(12f)
        moveTo(9f, 15f); lineTo(12f, 18f); lineTo(15f, 15f)
    }
}.build()

internal val InvoiceSmsVector: ImageVector = ImageVector.Builder(
    name = "InvoiceSms",
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
        moveTo(20f, 4f); horizontalLineTo(4f); curveTo(2.9f, 4f, 2f, 4.9f, 2f, 6f); verticalLineTo(18f); curveTo(2f, 19.1f, 2.9f, 20f, 4f, 20f); horizontalLineTo(20f); curveTo(21.1f, 20f, 22f, 19.1f, 22f, 18f); verticalLineTo(6f); curveTo(22f, 4.9f, 21.1f, 4f, 20f, 4f); close()
        moveTo(20f, 6f); lineTo(12f, 13f); lineTo(4f, 6f)
    }
}.build()

internal val InvoiceStoreVector: ImageVector = ImageVector.Builder(
    name = "InvoiceStore",
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
        moveTo(3f, 3f); horizontalLineTo(21f); lineTo(20f, 9f); horizontalLineTo(4f); close()
        moveTo(4f, 9f); verticalLineTo(20f); curveTo(4f, 20.6f, 4.4f, 21f, 5f, 21f); horizontalLineTo(19f); curveTo(19.6f, 21f, 20f, 20.6f, 20f, 20f); verticalLineTo(9f)
        moveTo(9f, 21f); verticalLineTo(13f); horizontalLineTo(15f); verticalLineTo(21f)
    }
}.build()
