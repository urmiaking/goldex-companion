package com.goldex.companion.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.graphics.Paint as AndroidPaint
import android.graphics.Path as AndroidPath
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.goldex.companion.ui.theme.LocalGoldExColors
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max
import kotlin.math.min

/**
 * A virtual touch/stylus signature pad modal dialog.
 * Allows goldsmiths to draw their signature with smooth bezier strokes,
 * clear, cancel, or confirm and save as a transparent PNG asset.
 */
@Composable
fun SignaturePadDialog(
    onDismiss: () -> Unit,
    onSignatureSaved: (uri: String) -> Unit,
    initialTitle: String = "ثبت امضای دیجیتال زرگر"
) {
    val context = LocalContext.current
    val colors = LocalGoldExColors.current

    val strokes = remember { mutableStateListOf<List<Offset>>() }
    var currentStroke by remember { mutableStateOf<List<Offset>>(emptyList()) }
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.94f)
                    .padding(vertical = 24.dp),
                shape = RoundedCornerShape(20.dp),
                color = colors.surface,
                border = BorderStroke(1.dp, colors.goldBorder.copy(alpha = 0.6f)),
                shadowElevation = 12.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Header Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(colors.goldContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = null,
                                    tint = colors.goldPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = initialTitle,
                                    fontSize = 14.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.textMain
                                )
                                Text(
                                    text = "جهت درج مستقیم در فاکتورهای رسمی و اسناد فروش",
                                    fontSize = 11.sp,
                                    color = colors.textMuted
                                )
                            }
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "بستن",
                                tint = colors.textSecondary
                            )
                        }
                    }

                    // Canvas Container Card
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(230.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.White)
                            .border(1.5.dp, colors.goldBorder.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                            .pointerInput(Unit) {
                                detectDragGestures(
                                    onDragStart = { offset ->
                                        currentStroke = listOf(offset)
                                    },
                                    onDrag = { change, _ ->
                                        change.consume()
                                        currentStroke = currentStroke + change.position
                                    },
                                    onDragEnd = {
                                        if (currentStroke.isNotEmpty()) {
                                            strokes.add(currentStroke)
                                            currentStroke = emptyList()
                                        }
                                    },
                                    onDragCancel = {
                                        currentStroke = emptyList()
                                    }
                                )
                            }
                    ) {
                        Canvas(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(8.dp)
                        ) {
                            if (canvasSize.width != size.width.toInt() || canvasSize.height != size.height.toInt()) {
                                canvasSize = IntSize(size.width.toInt(), size.height.toInt())
                            }

                            // Subtle guide line at the bottom
                            val lineY = size.height * 0.78f
                            drawLine(
                                color = Color(0xFFD1D5DB),
                                start = Offset(size.width * 0.08f, lineY),
                                end = Offset(size.width * 0.92f, lineY),
                                strokeWidth = 1.2f,
                                pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 8f))
                            )

                            // Render all finished strokes with smooth quadratic beziers
                            val inkColor = Color(0xFF0F172A)
                            val allStrokes = if (currentStroke.isNotEmpty()) strokes + listOf(currentStroke) else strokes

                            for (stroke in allStrokes) {
                                if (stroke.isEmpty()) continue
                                if (stroke.size == 1) {
                                    drawCircle(
                                        color = inkColor,
                                        radius = 2.5f,
                                        center = stroke[0]
                                    )
                                } else {
                                    val path = Path().apply {
                                        moveTo(stroke[0].x, stroke[0].y)
                                        for (i in 1 until stroke.size) {
                                            val p0 = stroke[i - 1]
                                            val p1 = stroke[i]
                                            val midX = (p0.x + p1.x) / 2f
                                            val midY = (p0.y + p1.y) / 2f
                                            quadraticBezierTo(p0.x, p0.y, midX, midY)
                                        }
                                        lineTo(stroke.last().x, stroke.last().y)
                                    }
                                    drawPath(
                                        path = path,
                                        color = inkColor,
                                        style = Stroke(
                                            width = 3.6f,
                                            cap = StrokeCap.Round,
                                            join = StrokeJoin.Round
                                        )
                                    )
                                }
                            }
                        }

                        // Empty Hint & Guideline Label
                        if (strokes.isEmpty() && currentStroke.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(bottom = 20.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "با انگشت یا قلم در این کادر امضا بزنید",
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF94A3B8)
                                )
                            }
                        }

                        // Clear Button on Top Left of Canvas
                        if (strokes.isNotEmpty() || currentStroke.isNotEmpty()) {
                            IconButton(
                                onClick = {
                                    strokes.clear()
                                    currentStroke = emptyList()
                                },
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(6.dp)
                                    .size(30.dp)
                                    .background(Color(0xFFF1F5F9), RoundedCornerShape(8.dp))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "پاک کردن",
                                    tint = colors.errorRed,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    // Dialog Actions (RTL compliant: Secondary on the right, Primary on the left)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Secondary / Cancel Action (Right child in RTL)
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, colors.border)
                        ) {
                            Text(
                                text = "انصراف",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.textSecondary
                            )
                        }

                        // Primary / Confirm Action (Left child in RTL)
                        val hasSignature = strokes.isNotEmpty()
                        GoldButton(
                            text = "تأیید و ذخیره امضا",
                            onClick = {
                                if (hasSignature && canvasSize.width > 0 && canvasSize.height > 0) {
                                    val uri = exportSignatureToBitmapFile(
                                        context = context,
                                        strokes = strokes.toList(),
                                        canvasWidth = canvasSize.width,
                                        canvasHeight = canvasSize.height
                                    )
                                    if (uri != null) {
                                        onSignatureSaved(uri)
                                    }
                                }
                            },
                            enabled = hasSignature,
                            modifier = Modifier
                                .weight(1.3f)
                                .height(44.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Exports drawn strokes into a clean cropped transparent PNG file in internal storage.
 */
private fun exportSignatureToBitmapFile(
    context: Context,
    strokes: List<List<Offset>>,
    canvasWidth: Int,
    canvasHeight: Int
): String? = runCatching {
    if (strokes.isEmpty()) return null

    // Determine bounding box
    var minX = Float.MAX_VALUE
    var minY = Float.MAX_VALUE
    var maxX = Float.MIN_VALUE
    var maxY = Float.MIN_VALUE

    for (stroke in strokes) {
        for (pt in stroke) {
            minX = min(minX, pt.x)
            minY = min(minY, pt.y)
            maxX = max(maxX, pt.x)
            maxY = max(maxY, pt.y)
        }
    }

    val padding = 24f
    val boundLeft = max(0f, minX - padding)
    val boundTop = max(0f, minY - padding)
    val boundRight = min(canvasWidth.toFloat(), maxX + padding)
    val boundBottom = min(canvasHeight.toFloat(), maxY + padding)

    val cropW = max(50, (boundRight - boundLeft).toInt())
    val cropH = max(30, (boundBottom - boundTop).toInt())

    // Render to high-quality Bitmap
    val bitmap = Bitmap.createBitmap(cropW, cropH, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)

    val paint = AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG or AndroidPaint.DITHER_FLAG).apply {
        color = 0xFF0F172A.toInt() // Deep luxury ink
        style = AndroidPaint.Style.STROKE
        strokeWidth = 5.5f
        strokeCap = AndroidPaint.Cap.ROUND
        strokeJoin = AndroidPaint.Join.ROUND
    }

    canvas.translate(-boundLeft, -boundTop)

    for (stroke in strokes) {
        if (stroke.isEmpty()) continue
        if (stroke.size == 1) {
            canvas.drawCircle(stroke[0].x, stroke[0].y, 3f, paint)
        } else {
            val path = AndroidPath().apply {
                moveTo(stroke[0].x, stroke[0].y)
                for (i in 1 until stroke.size) {
                    val p0 = stroke[i - 1]
                    val p1 = stroke[i]
                    val midX = (p0.x + p1.x) / 2f
                    val midY = (p0.y + p1.y) / 2f
                    quadTo(p0.x, p0.y, midX, midY)
                }
                lineTo(stroke.last().x, stroke.last().y)
            }
            canvas.drawPath(path, paint)
        }
    }

    // Save to files/brand_assets/seller_signature.png
    val brandDir = File(context.filesDir, "brand_assets").apply { mkdirs() }
    val signatureFile = File(brandDir, "seller_signature.png")
    FileOutputStream(signatureFile).use { out ->
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
    }
    bitmap.recycle()

    Uri.fromFile(signatureFile).toString()
}.getOrNull()
