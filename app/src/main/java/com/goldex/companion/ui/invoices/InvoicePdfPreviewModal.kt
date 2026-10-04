package com.goldex.companion.ui.invoices

import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.goldex.companion.model.PersianNumberFormatter
import com.goldex.companion.ui.components.GoldButton
import com.goldex.companion.ui.invoices.components.InvoiceCloseVector
import com.goldex.companion.ui.invoices.components.InvoiceMinusVector
import com.goldex.companion.ui.invoices.components.InvoicePlusVector
import com.goldex.companion.ui.invoices.components.InvoicePrintVector
import com.goldex.companion.ui.theme.LocalGoldExColors
import com.goldex.companion.ui.theme.VazirmatnFamily
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

private sealed interface PdfPreviewState {
    data object Loading : PdfPreviewState
    data class Ready(val pages: List<Bitmap>) : PdfPreviewState
    data object Error : PdfPreviewState
}

@Composable
fun InvoicePdfPreviewModal(
    file: File,
    invoiceNumber: String,
    onDismiss: () -> Unit,
    onShare: () -> Unit,
    onPrint: (() -> Unit)? = null
) {
    val colors = LocalGoldExColors.current
    val previewState by produceState<PdfPreviewState>(
        initialValue = PdfPreviewState.Loading,
        key1 = file.absolutePath,
        key2 = file.lastModified()
    ) {
        value = withContext(Dispatchers.IO) { renderPdfPages(file) }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        androidx.compose.runtime.CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = colors.background
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Surface(
                        color = colors.surface,
                        shadowElevation = 3.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = "پیش‌نمایش فاکتور",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.textMain
                                )
                                Text(
                                    text = "شماره ${PersianNumberFormatter.toPersianDigits(invoiceNumber)}",
                                    fontSize = 11.sp,
                                    color = colors.textMuted
                                )
                            }
                            IconButton(onClick = onDismiss) {
                                Icon(Icons.Default.Close, contentDescription = "بستن", tint = colors.textSecondary)
                            }
                        }
                    }

                    var scale by remember { mutableFloatStateOf(1f) }
                    var offset by remember { mutableStateOf(Offset.Zero) }
                    var containerSize by remember { mutableStateOf(IntSize.Zero) }
                    val isZoomed = scale > 1.02f

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .background(colors.surfaceElevated.copy(alpha = 0.65f))
                            .clipToBounds()
                            .onSizeChanged { containerSize = it }
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onDoubleTap = {
                                        if (scale > 1.05f) {
                                            scale = 1f
                                            offset = Offset.Zero
                                        } else {
                                            scale = 2.5f
                                            offset = Offset.Zero
                                        }
                                    }
                                )
                            }
                            .pointerInput(isZoomed) {
                                if (isZoomed) {
                                    detectTransformGestures { _, pan, zoom, _ ->
                                        val newScale = (scale * zoom).coerceIn(1f, 4f)
                                        scale = newScale
                                        if (newScale <= 1.02f) {
                                            scale = 1f
                                            offset = Offset.Zero
                                        } else {
                                            val contentWidth = containerSize.width.toFloat()
                                            val contentHeight = containerSize.height.toFloat()
                                            val maxX = (contentWidth * (newScale - 1f)) / 2f
                                            val maxY = (contentHeight * (newScale - 1f)) / 2f
                                            offset = Offset(
                                                x = (offset.x + pan.x).coerceIn(-maxX, maxX),
                                                y = (offset.y + pan.y).coerceIn(-maxY, maxY)
                                            )
                                        }
                                    }
                                } else {
                                    awaitEachGesture {
                                        awaitFirstDown(requireUnconsumed = false)
                                        do {
                                            val event = awaitPointerEvent()
                                            if (event.changes.size > 1) {
                                                val zoom = event.calculateZoom()
                                                if (zoom > 1.01f || zoom < 0.99f) {
                                                    val newScale = (scale * zoom).coerceIn(1f, 4f)
                                                    scale = newScale
                                                    event.changes.forEach { it.consume() }
                                                }
                                            }
                                        } while (event.changes.any { it.pressed })
                                    }
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        when (val state = previewState) {
                            PdfPreviewState.Loading -> CircularProgressIndicator(color = colors.goldPrimary)
                            PdfPreviewState.Error -> Text(
                                text = "نمایش فایل PDF ممکن نیست",
                                color = colors.errorRed,
                                fontWeight = FontWeight.SemiBold
                            )
                            is PdfPreviewState.Ready -> {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .graphicsLayer {
                                            scaleX = scale
                                            scaleY = scale
                                            translationX = offset.x
                                            translationY = offset.y
                                        }
                                ) {
                                    LazyColumn(
                                        modifier = Modifier.fillMaxSize(),
                                        contentPadding = PaddingValues(14.dp),
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        itemsIndexed(state.pages, key = { index, _ -> index }) { index, bitmap ->
                                            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                                Text(
                                                    text = "صفحه ${PersianNumberFormatter.toPersianDigits((index + 1).toString())}",
                                                    fontSize = 10.5.sp,
                                                    color = colors.textMuted,
                                                    modifier = Modifier.align(Alignment.CenterHorizontally)
                                                )
                                                Image(
                                                    bitmap = bitmap.asImageBitmap(),
                                                    contentDescription = "صفحه ${index + 1} فاکتور",
                                                    contentScale = ContentScale.FillWidth,
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .aspectRatio(bitmap.width.toFloat() / bitmap.height.toFloat())
                                                        .heightIn(min = 180.dp)
                                                        .background(Color.White, RoundedCornerShape(10.dp))
                                                        .border(0.7.dp, colors.border, RoundedCornerShape(10.dp))
                                                )
                                            }
                                        }
                                    }
                                }

                                // Floating Zoom Controls Bar
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = colors.surface.copy(alpha = 0.94f),
                                    border = BorderStroke(0.8.dp, colors.goldBorder.copy(alpha = 0.6f)),
                                    shadowElevation = 6.dp,
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .padding(bottom = 16.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        IconButton(
                                            onClick = {
                                                val newScale = (scale + 0.5f).coerceAtMost(4f)
                                                scale = newScale
                                            },
                                            modifier = Modifier.size(32.dp),
                                            enabled = scale < 4f
                                        ) {
                                            Icon(
                                                imageVector = InvoicePlusVector,
                                                contentDescription = "بزرگنمایی",
                                                tint = if (scale < 4f) colors.textMain else colors.textMuted,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }

                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (scale > 1.05f) colors.goldContainer.copy(alpha = 0.4f) else Color.Transparent,
                                            modifier = Modifier
                                                .clickable {
                                                    scale = 1f
                                                    offset = Offset.Zero
                                                }
                                                .padding(horizontal = 6.dp, vertical = 3.dp)
                                        ) {
                                            Text(
                                                text = "${PersianNumberFormatter.toPersianDigits((scale * 100).toInt().toString())}٪",
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (scale > 1.05f) colors.goldPrimary else colors.textSecondary,
                                                fontFamily = VazirmatnFamily
                                            )
                                        }

                                        IconButton(
                                            onClick = {
                                                val newScale = (scale - 0.5f).coerceAtLeast(1f)
                                                scale = newScale
                                                if (newScale <= 1.02f) {
                                                    scale = 1f
                                                    offset = Offset.Zero
                                                } else {
                                                    val contentWidth = containerSize.width.toFloat()
                                                    val contentHeight = containerSize.height.toFloat()
                                                    val maxX = (contentWidth * (newScale - 1f)) / 2f
                                                    val maxY = (contentHeight * (newScale - 1f)) / 2f
                                                    offset = Offset(
                                                        offset.x.coerceIn(-maxX, maxX),
                                                        offset.y.coerceIn(-maxY, maxY)
                                                    )
                                                }
                                            },
                                            modifier = Modifier.size(32.dp),
                                            enabled = scale > 1f
                                        ) {
                                            Icon(
                                                imageVector = InvoiceMinusVector,
                                                contentDescription = "کوچک‌نمایی",
                                                tint = if (scale > 1f) colors.textMain else colors.textMuted,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }

                                        if (scale > 1.05f) {
                                            IconButton(
                                                onClick = {
                                                    scale = 1f
                                                    offset = Offset.Zero
                                                },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(
                                                    imageVector = InvoiceCloseVector,
                                                    contentDescription = "بازنشانی بزرگنمایی",
                                                    tint = colors.goldPrimary,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Surface(
                        color = colors.surface,
                        shadowElevation = 8.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .navigationBarsPadding()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            GoldButton(
                                text = "بستن",
                                onClick = onDismiss,
                                isSecondary = true,
                                modifier = Modifier.weight(0.9f)
                            )
                            if (onPrint != null) {
                                GoldButton(
                                    text = "چاپ",
                                    onClick = onPrint,
                                    icon = InvoicePrintVector,
                                    enabled = previewState is PdfPreviewState.Ready,
                                    modifier = Modifier.weight(1.1f)
                                )
                            }
                            GoldButton(
                                text = "اشتراک‌گذاری",
                                onClick = onShare,
                                icon = Icons.Default.Share,
                                enabled = previewState is PdfPreviewState.Ready,
                                modifier = Modifier.weight(1.1f)
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun renderPdfPages(file: File): PdfPreviewState = runCatching {
    ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { descriptor ->
        PdfRenderer(descriptor).use { renderer ->
            List(renderer.pageCount) { index ->
                renderer.openPage(index).use { page ->
                    val targetWidth = 2048
                    val renderScale = (targetWidth.toFloat() / page.width.toFloat()).coerceIn(1.5f, 3.5f)
                    val outWidth = (page.width * renderScale).toInt()
                    val outHeight = (page.height * renderScale).toInt()
                    Bitmap.createBitmap(outWidth, outHeight, Bitmap.Config.ARGB_8888).also { bitmap ->
                        bitmap.eraseColor(AndroidColor.WHITE)
                        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    }
                }
            }
        }
    }.let(PdfPreviewState::Ready)
}.getOrElse { PdfPreviewState.Error }

