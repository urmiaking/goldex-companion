package com.goldex.companion.desktop.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.loadImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.goldex.companion.data.AppSettings
import com.goldex.companion.desktop.data.DesktopInvoiceDocuments
import com.goldex.companion.model.*
import com.goldex.companion.ui.theme.LocalGoldExColors
import com.goldex.companion.ui.theme.hairlineBorder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

@Composable internal fun InvoiceDocumentPreview(invoice: BarterInvoice, settings: AppSettings, busy: Boolean,
    onDismiss: () -> Unit, onSave: () -> Unit, onPrint: () -> Unit) {
    val colors = LocalGoldExColors.current
    val pages by produceState<Result<List<ImageBitmap>>?>(null, invoice, settings) {
        value = withContext(Dispatchers.IO) { runCatching {
            DesktopInvoiceDocuments.render(invoice, settings).map { image ->
                try {
                    val bytes = ByteArrayOutputStream().apply { ImageIO.write(image, "png", this) }.toByteArray()
                    loadImageBitmap(ByteArrayInputStream(bytes))
                } finally { image.flush() }
            }
        } }
    }
    var zoom by remember { mutableStateOf(1f) }
    Dialog({ if (!busy) onDismiss() }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.widthIn(max = 1180.dp).fillMaxWidth().fillMaxHeight(.95f).testTag("invoice-pdf-preview"),
            shape = MaterialTheme.shapes.large, color = colors.background) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("پیش‌نمایش فاکتور ${PersianNumberFormatter.toPersianDigits(invoice.cleanInvoiceNumber)}", Modifier.weight(1f),
                        fontWeight = FontWeight.Bold, color = colors.textMain)
                    IconButton({ zoom = (zoom - .25f).coerceAtLeast(1f) }) { Icon(Icons.Outlined.ZoomOut, "کوچک کردن") }
                    IconButton({ zoom = (zoom + .25f).coerceAtMost(3f) }) { Icon(Icons.Outlined.ZoomIn, "بزرگ کردن") }
                    IconButton({ if (!busy) onDismiss() }, enabled = !busy) { Icon(Icons.Outlined.Close, "بستن پیش‌نمایش") }
                }
                BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
                    val pageWidth = maxWidth * zoom
                    when {
                        pages == null -> CircularProgressIndicator(Modifier.align(Alignment.Center), color = colors.goldPrimary)
                        pages?.isFailure == true -> Text("پیش‌نمایش آماده نشد؛ تصاویر مشخصات واحد صنفی را بررسی کنید", color = colors.errorRed)
                        else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            itemsIndexed(pages!!.getOrThrow()) { index, bitmap ->
                                Column {
                                    Text("صفحه ${PersianNumberFormatter.toPersianDigits((index + 1).toString())}", color = colors.textMuted, fontSize = 12.sp)
                                    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
                                        Image(bitmap, "صفحه فاکتور رسمی", Modifier.width(pageWidth).aspectRatio(595f / 420f).border(colors.hairlineBorder))
                                    }
                                }
                            }
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    WorkspaceControl("بستن", onDismiss, Modifier.weight(1f), secondary = true, enabled = !busy)
                    WorkspaceControl("چاپ", onPrint, Modifier.weight(1f).testTag("invoice-print"), secondary = true, enabled = !busy && pages?.isSuccess == true)
                    WorkspaceControl(if (busy) "در حال آماده‌سازی" else "ذخیره PDF", onSave, Modifier.weight(1f).testTag("invoice-export"), enabled = !busy && pages?.isSuccess == true)
                }
            }
        }
    }
}
