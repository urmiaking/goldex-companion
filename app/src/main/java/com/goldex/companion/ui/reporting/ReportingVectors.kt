package com.goldex.companion.ui.reporting

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

internal val ReportingAccountBalance: ImageVector = ImageVector.Builder(
    name = "ReportingAccountBalance",
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
        moveTo(16f, 10f)
        verticalLineTo(17f)
        horizontalLineTo(19f)
        verticalLineTo(10f)
        horizontalLineTo(16f)
        close()
        moveTo(2f, 22f)
        horizontalLineTo(21f)
        verticalLineTo(19f)
        horizontalLineTo(2f)
        verticalLineTo(22f)
        close()
        moveTo(11.5f, 1f)
        lineTo(2f, 6f)
        verticalLineTo(8f)
        horizontalLineTo(21f)
        verticalLineTo(6f)
        lineTo(11.5f, 1f)
        close()
    }
}.build()

internal val ReportingInsights: ImageVector = ImageVector.Builder(
    name = "ReportingInsights",
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
        moveTo(3f, 18f)
        lineTo(9f, 12f)
        lineTo(13f, 16f)
        lineTo(21f, 8f)
        moveTo(16f, 8f)
        horizontalLineTo(21f)
        verticalLineTo(13f)
    }
}.build()

internal val ReportingScale: ImageVector = ImageVector.Builder(
    name = "ReportingScale",
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
        moveTo(5f, 7f)
        horizontalLineTo(19f)
        moveTo(5f, 7f)
        lineTo(2f, 13f)
        curveTo(2f, 15f, 4f, 16f, 5f, 16f)
        curveTo(6f, 16f, 8f, 15f, 8f, 13f)
        lineTo(5f, 7f)
        close()
        moveTo(19f, 7f)
        lineTo(16f, 13f)
        curveTo(16f, 15f, 18f, 16f, 19f, 16f)
        curveTo(20f, 16f, 22f, 15f, 22f, 13f)
        lineTo(19f, 7f)
        close()
        moveTo(9f, 21f)
        horizontalLineTo(15f)
    }
}.build()

internal val ReportingContacts: ImageVector = ImageVector.Builder(
    name = "ReportingContacts",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f
).apply {
    path(fill = SolidColor(Color.White)) {
        moveTo(12f, 12f)
        curveTo(14.21f, 12f, 16f, 10.21f, 16f, 8f)
        curveTo(16f, 5.79f, 14.21f, 4f, 12f, 4f)
        curveTo(9.79f, 4f, 8f, 5.79f, 8f, 8f)
        curveTo(8f, 10.21f, 9.79f, 12f, 12f, 12f)
        close()
        moveTo(12f, 14f)
        curveTo(9.33f, 14f, 4f, 15.34f, 4f, 18f)
        verticalLineTo(20f)
        horizontalLineTo(20f)
        verticalLineTo(18f)
        curveTo(20f, 15.34f, 14.67f, 14f, 12f, 14f)
        close()
    }
}.build()

internal val ReportingReceipt: ImageVector = ImageVector.Builder(
    name = "ReportingReceipt",
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
        moveTo(5f, 3f)
        lineTo(7f, 4.5f)
        lineTo(9f, 3f)
        lineTo(11f, 4.5f)
        lineTo(13f, 3f)
        lineTo(15f, 4.5f)
        lineTo(17f, 3f)
        lineTo(19f, 4.5f)
        verticalLineTo(21f)
        lineTo(17f, 19.5f)
        lineTo(15f, 21f)
        lineTo(13f, 19.5f)
        lineTo(11f, 21f)
        lineTo(9f, 19.5f)
        lineTo(7f, 21f)
        lineTo(5f, 19.5f)
        close()
        moveTo(8f, 9f)
        horizontalLineTo(16f)
        moveTo(8f, 13f)
        horizontalLineTo(16f)
    }
}.build()

internal val ReportingPointOfSale: ImageVector = ImageVector.Builder(
    name = "ReportingPointOfSale",
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
        verticalLineTo(18f)
        curveTo(20f, 19.1f, 19.1f, 20f, 18f, 20f)
        horizontalLineTo(6f)
        curveTo(4.9f, 20f, 4f, 19.1f, 4f, 18f)
        close()
        moveTo(7f, 8f)
        horizontalLineTo(17f)
        moveTo(8f, 12f)
        horizontalLineTo(9f)
        moveTo(12f, 12f)
        horizontalLineTo(13f)
        moveTo(16f, 12f)
        horizontalLineTo(17f)
        moveTo(8f, 15f)
        horizontalLineTo(9f)
        moveTo(12f, 15f)
        horizontalLineTo(13f)
        moveTo(16f, 15f)
        horizontalLineTo(17f)
    }
}.build()

internal val ReportingShoppingBag: ImageVector = ImageVector.Builder(
    name = "ReportingShoppingBag",
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
        moveTo(5f, 8f)
        horizontalLineTo(19f)
        lineTo(20f, 20f)
        curveTo(20f, 20.6f, 19.5f, 21f, 19f, 21f)
        horizontalLineTo(5f)
        curveTo(4.5f, 21f, 4f, 20.6f, 4f, 20f)
        lineTo(5f, 8f)
        close()
        moveTo(9f, 8f)
        verticalLineTo(6f)
        curveTo(9f, 4.34f, 10.34f, 3f, 12f, 3f)
        curveTo(13.66f, 3f, 15f, 4.34f, 15f, 6f)
        verticalLineTo(8f)
    }
}.build()

internal val ReportingCalendar: ImageVector = ImageVector.Builder(
    name = "ReportingCalendar",
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
        moveTo(4f, 5f)
        horizontalLineTo(20f)
        verticalLineTo(20f)
        curveTo(20f, 20.6f, 19.5f, 21f, 19f, 21f)
        horizontalLineTo(5f)
        curveTo(4.5f, 21f, 4f, 20.6f, 4f, 20f)
        verticalLineTo(5f)
        close()
        moveTo(16f, 2f)
        verticalLineTo(6f)
        moveTo(8f, 2f)
        verticalLineTo(6f)
        moveTo(4f, 10f)
        horizontalLineTo(20f)
    }
}.build()

internal val ReportingArrowForward: ImageVector = ImageVector.Builder(
    name = "ReportingArrowForward",
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
        moveTo(5f, 12f)
        horizontalLineTo(19f)
        moveTo(13f, 6f)
        lineTo(19f, 12f)
        lineTo(13f, 18f)
    }
}.build()
