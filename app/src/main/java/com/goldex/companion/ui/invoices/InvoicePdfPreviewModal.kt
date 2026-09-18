package com.goldex.companion.ui.invoices

import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
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
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.goldex.companion.model.PersianNumberFormatter
import com.goldex.companion.ui.components.GoldButton
import com.goldex.companion.ui.theme.LocalGoldExColors
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
    onShare: () -> Unit
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
                                    text = "شماره $invoiceNumber",
                                    fontSize = 11.sp,
                                    color = colors.textMuted
                                )
                            }
                            IconButton(onClick = onDismiss) {
                                Icon(Icons.Default.Close, contentDescription = "بستن", tint = colors.textSecondary)
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .background(colors.surfaceElevated.copy(alpha = 0.65f)),
                        contentAlignment = Alignment.Center
                    ) {
                        when (val state = previewState) {
                            PdfPreviewState.Loading -> CircularProgressIndicator(color = colors.goldPrimary)
                            PdfPreviewState.Error -> Text(
                                text = "نمایش فایل PDF ممکن نیست",
                                color = colors.errorRed,
                                fontWeight = FontWeight.SemiBold
                            )
                            is PdfPreviewState.Ready -> LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(14.dp),
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
                                                .background(androidx.compose.ui.graphics.Color.White, RoundedCornerShape(10.dp))
                                                .border(0.7.dp, colors.border, RoundedCornerShape(10.dp))
                                        )
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
                                modifier = Modifier.weight(1f)
                            )
                            GoldButton(
                                text = "اشتراک‌گذاری PDF",
                                onClick = onShare,
                                icon = Icons.Default.Share,
                                enabled = previewState is PdfPreviewState.Ready,
                                modifier = Modifier.weight(1.6f)
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
                    val scale = 2
                    Bitmap.createBitmap(page.width * scale, page.height * scale, Bitmap.Config.ARGB_8888).also { bitmap ->
                        bitmap.eraseColor(AndroidColor.WHITE)
                        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    }
                }
            }
        }
    }.let(PdfPreviewState::Ready)
}.getOrElse { PdfPreviewState.Error }
