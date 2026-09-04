package com.goldex.companion.ui.hub

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

internal val HubChevronLeft: ImageVector = ImageVector.Builder(
    name = "HubChevronLeft",
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

internal val HubCheckCircle: ImageVector = ImageVector.Builder(
    name = "HubCheckCircle",
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
        curveTo(22f, 6.48f, 17.52f, 2f, 12f, 2f)
        close()
        moveTo(10f, 16.4f)
        lineTo(5.6f, 12f)
        lineTo(7f, 10.6f)
        lineTo(10f, 13.6f)
        lineTo(17f, 6.6f)
        lineTo(18.4f, 8f)
        lineTo(10f, 16.4f)
        close()
    }
}.build()

internal val HubMenuBook: ImageVector = ImageVector.Builder(
    name = "HubMenuBook",
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

internal val HubInbox: ImageVector = ImageVector.Builder(
    name = "HubInbox",
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
        moveTo(3.5f, 7.5f)
        horizontalLineTo(20.5f)
        verticalLineTo(18f)
        curveTo(20.5f, 19.1f, 19.6f, 20f, 18.5f, 20f)
        horizontalLineTo(5.5f)
        curveTo(4.4f, 20f, 3.5f, 19.1f, 3.5f, 18f)
        close()
        moveTo(3.5f, 13f)
        horizontalLineTo(8.5f)
        lineTo(10f, 15.5f)
        horizontalLineTo(14f)
        lineTo(15.5f, 13f)
        horizontalLineTo(20.5f)
    }
}.build()

internal val HubHandyman: ImageVector = ImageVector.Builder(
    name = "HubHandyman",
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
        moveTo(14.7f, 6.3f)
        lineTo(17.7f, 9.3f)
        moveTo(13.2f, 7.8f)
        lineTo(5f, 16f)
        verticalLineTo(19f)
        horizontalLineTo(8f)
        lineTo(16.2f, 10.8f)
        moveTo(16.5f, 4.5f)
        lineTo(19.5f, 7.5f)
        curveTo(20.3f, 6.7f, 20.3f, 5.3f, 19.5f, 4.5f)
        curveTo(18.7f, 3.7f, 17.3f, 3.7f, 16.5f, 4.5f)
        close()
    }
}.build()

internal val HubShowcase: ImageVector = ImageVector.Builder(
    name = "HubShowcase",
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
        moveTo(3.5f, 4.5f)
        horizontalLineTo(10f)
        verticalLineTo(11f)
        horizontalLineTo(3.5f)
        close()
        moveTo(14f, 4.5f)
        horizontalLineTo(20.5f)
        verticalLineTo(11f)
        horizontalLineTo(14f)
        close()
        moveTo(3.5f, 14f)
        horizontalLineTo(10f)
        verticalLineTo(20.5f)
        horizontalLineTo(3.5f)
        close()
        moveTo(14f, 14f)
        horizontalLineTo(20.5f)
        verticalLineTo(20.5f)
        horizontalLineTo(14f)
        close()
    }
}.build()

internal val HubKaratSync: ImageVector = ImageVector.Builder(
    name = "HubKaratSync",
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
        moveTo(7f, 8f)
        horizontalLineTo(20f)
        lineTo(17f, 5f)
        moveTo(17f, 16f)
        horizontalLineTo(4f)
        lineTo(7f, 19f)
    }
}.build()

internal val HubTrending: ImageVector = ImageVector.Builder(
    name = "HubTrending",
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
        moveTo(3f, 17f)
        lineTo(9f, 11f)
        lineTo(13f, 15f)
        lineTo(21f, 7f)
        moveTo(16f, 7f)
        horizontalLineTo(21f)
        verticalLineTo(12f)
    }
}.build()

internal val HubCloudSync: ImageVector = ImageVector.Builder(
    name = "HubCloudSync",
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
        moveTo(19.35f, 10.04f)
        curveTo(18.67f, 6.59f, 15.64f, 4f, 12f, 4f)
        curveTo(9.11f, 4f, 6.6f, 5.64f, 5.35f, 8.04f)
        curveTo(2.34f, 8.36f, 0f, 10.91f, 0f, 14f)
        curveTo(0f, 17.31f, 2.69f, 20f, 6f, 20f)
        horizontalLineTo(19f)
        curveTo(21.76f, 20f, 24f, 17.76f, 24f, 15f)
        curveTo(24f, 12.36f, 21.95f, 10.22f, 19.35f, 10.04f)
        close()
    }
}.build()

internal val HubPercent: ImageVector = ImageVector.Builder(
    name = "HubPercent",
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

internal val HubQrCode: ImageVector = ImageVector.Builder(
    name = "HubQrCode",
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
        moveTo(3.5f, 3.5f); horizontalLineTo(9.5f); verticalLineTo(9.5f); horizontalLineTo(3.5f); close()
        moveTo(14.5f, 3.5f); horizontalLineTo(20.5f); verticalLineTo(9.5f); horizontalLineTo(14.5f); close()
        moveTo(3.5f, 14.5f); horizontalLineTo(9.5f); verticalLineTo(20.5f); horizontalLineTo(3.5f); close()
        moveTo(14.5f, 14.5f); lineTo(17.5f, 14.5f); lineTo(17.5f, 17.5f)
        moveTo(20.5f, 14.5f); verticalLineTo(17.5f)
        moveTo(14.5f, 20.5f); horizontalLineTo(20.5f)
    }
}.build()

internal val HubFingerprint: ImageVector = ImageVector.Builder(
    name = "HubFingerprint",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f
).apply {
    path(
        stroke = SolidColor(Color.White),
        strokeLineWidth = 1.8f,
        strokeLineCap = StrokeCap.Round
    ) {
        moveTo(12f, 3f)
        curveTo(7.58f, 3f, 4f, 6.58f, 4f, 11f)
        curveTo(4f, 15.42f, 7.58f, 19f, 12f, 19f)
        moveTo(12f, 6.5f)
        curveTo(9.51f, 6.5f, 7.5f, 8.51f, 7.5f, 11f)
        curveTo(7.5f, 13.49f, 9.51f, 15.5f, 12f, 15.5f)
        moveTo(12f, 10f)
        curveTo(11.45f, 10f, 11f, 10.45f, 11f, 11f)
        verticalLineTo(13f)
    }
}.build()

internal val HubPhoneInTalk: ImageVector = ImageVector.Builder(
    name = "HubPhoneInTalk",
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
        horizontalLineTo(8f)
        lineTo(10f, 9f)
        lineTo(7.5f, 10.5f)
        curveTo(9f, 13.5f, 10.5f, 15f, 13.5f, 16.5f)
        lineTo(15f, 14f)
        lineTo(20f, 16f)
        verticalLineTo(20f)
        curveTo(20f, 21.1f, 19.1f, 22f, 18f, 22f)
        curveTo(9.5f, 22f, 2f, 14.5f, 2f, 6f)
        curveTo(2f, 4.9f, 2.9f, 4f, 4f, 4f)
        close()
        moveTo(14f, 4f)
        curveTo(16.5f, 4f, 19f, 6.5f, 19f, 9f)
        moveTo(14f, 8f)
        curveTo(15f, 8f, 16f, 9f, 16f, 10f)
    }
}.build()

internal val HubCloudDownload: ImageVector = ImageVector.Builder(
    name = "HubCloudDownload",
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
        moveTo(12f, 11f)
        verticalLineTo(17f)
        moveTo(9f, 14f)
        lineTo(12f, 17f)
        lineTo(15f, 14f)
        moveTo(19.35f, 10.04f)
        curveTo(18.67f, 6.59f, 15.64f, 4f, 12f, 4f)
        curveTo(9.11f, 4f, 6.6f, 5.64f, 5.35f, 8.04f)
        curveTo(2.34f, 8.36f, 0f, 10.91f, 0f, 14f)
        curveTo(0f, 17.31f, 2.69f, 20f, 6f, 20f)
        horizontalLineTo(19f)
    }
}.build()

internal val HubStorefront: ImageVector = ImageVector.Builder(
    name = "HubStorefront",
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

internal val HubCamera: ImageVector = ImageVector.Builder(
    name = "HubCamera",
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
        moveTo(4f, 7f)
        horizontalLineTo(20f)
        curveTo(21.1f, 7f, 22f, 7.9f, 22f, 9f)
        verticalLineTo(19f)
        curveTo(22f, 20.1f, 21.1f, 21f, 20f, 21f)
        horizontalLineTo(4f)
        curveTo(2.9f, 21f, 2f, 20.1f, 2f, 19f)
        verticalLineTo(9f)
        curveTo(2f, 7.9f, 2.9f, 7f, 4f, 7f)
        close()
        moveTo(9f, 7f)
        lineTo(10.5f, 4.5f)
        horizontalLineTo(13.5f)
        lineTo(15f, 7f)
        moveTo(12f, 11f)
        curveTo(10.34f, 11f, 9f, 12.34f, 9f, 14f)
        curveTo(9f, 15.66f, 10.34f, 17f, 12f, 17f)
        curveTo(13.66f, 17f, 15f, 15.66f, 15f, 14f)
        curveTo(15f, 12.34f, 13.66f, 11f, 12f, 11f)
        close()
    }
}.build()

internal val HubShieldCheck: ImageVector = ImageVector.Builder(
    name = "HubShieldCheck",
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
        lineTo(4f, 5f)
        verticalLineTo(11f)
        curveTo(4f, 16.55f, 7.84f, 21.74f, 12f, 23f)
        curveTo(16.16f, 21.74f, 20f, 16.55f, 20f, 11f)
        verticalLineTo(5f)
        close()
        moveTo(9f, 12f)
        lineTo(11f, 14f)
        lineTo(15f, 10f)
    }
}.build()

