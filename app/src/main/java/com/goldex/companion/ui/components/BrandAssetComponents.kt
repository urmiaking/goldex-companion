package com.goldex.companion.ui.components

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.URI
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.goldex.companion.ui.theme.LocalGoldExColors

/**
 * Reusable tile for uploading, previewing, and managing store brand assets (logo, commercial stamp).
 */
@Composable
fun ProfileBrandAssetTile(
    title: String,
    actionLabel: String,
    bitmap: ImageBitmap?,
    fallback: String,
    onPick: () -> Unit,
    onClear: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    val colors = LocalGoldExColors.current
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = colors.surface,
        border = BorderStroke(0.8.dp, colors.border)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(colors.goldContainer.copy(alpha = 0.35f))
                    .border(1.dp, colors.goldBorder.copy(alpha = 0.7f), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap,
                        contentDescription = title,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(4.dp),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    Text(
                        text = fallback.ifBlank { "نشان" },
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = colors.goldPrimary
                    )
                }
            }
            Text(title, fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = colors.textMain)
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(
                    onClick = onPick,
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text(actionLabel, fontSize = 9.5.sp, fontWeight = FontWeight.Bold)
                }
                if (onClear != null) {
                    IconButton(onClick = onClear, modifier = Modifier.size(30.dp)) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "حذف $title",
                            tint = colors.errorRed,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * One registered launcher shared by the document picker and OEM-compatible fallbacks.
 */
@Composable
fun rememberBrandImagePicker(
    onImagePicked: (Uri) -> Unit,
    onError: (String) -> Unit = {}
): () -> Unit {
    val context = LocalContext.current
    val currentOnError = rememberUpdatedState(onError)
    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = result.data?.data ?: result.data?.clipData?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.uri
            if (uri != null) onImagePicked(uri)
        }
    }

    return remember(context, picker) {
        {
            try {
                launchBrandImagePicker(context) { picker.launch(it) }
            } catch (_: ActivityNotFoundException) {
                currentOnError.value("برنامه‌ای جهت انتخاب تصویر پیدا نشد.")
            } catch (_: SecurityException) {
                currentOnError.value("دسترسی به انتخاب تصویر امکان‌پذیر نیست.")
            } catch (_: IllegalStateException) {
                currentOnError.value("انتخاب تصویر آماده نیست؛ صفحه را دوباره باز کنید.")
            }
        }
    }
}

/**
 * Safely decodes a bitmap with inSampleSize and bounded dimensions to prevent OutOfMemoryError and OpenGL texture crashes.
 */
fun loadSafeProfileBitmap(context: Context, uriValue: String, maxDimension: Int = 512): Bitmap? {
    if (uriValue.isBlank() || maxDimension <= 0) return null
    return runCatching {
        fun openStream(): InputStream? {
            val candidateFile = when {
                uriValue.startsWith("file:") -> {
                    runCatching { File(URI(uriValue)) }.getOrNull()?.takeIf { it.exists() }
                        ?: (runCatching { Uri.parse(uriValue).path?.let { File(it) } }.getOrNull())?.takeIf { it.exists() }
                }
                File(uriValue).isAbsolute -> File(uriValue).takeIf { it.exists() }
                else -> null
            }
            if (candidateFile != null && candidateFile.exists()) {
                return candidateFile.inputStream()
            }
            return runCatching { context.contentResolver.openInputStream(Uri.parse(uriValue)) }.getOrNull()
        }

        // First pass: decode bounds only (zero memory allocated)
        val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        val boundsStream = openStream() ?: return null
        boundsStream.use { BitmapFactory.decodeStream(it, null, boundsOptions) }
        // Bounds-only decoding returns null even for a valid image; validate its dimensions instead.
        if (boundsOptions.outWidth <= 0 || boundsOptions.outHeight <= 0) return null

        // Calculate power-of-two inSampleSize
        var sampleSize = 1
        val maxOut = maxOf(boundsOptions.outWidth, boundsOptions.outHeight)
        while (maxOut / (sampleSize * 2) >= maxDimension) {
            sampleSize *= 2
        }

        // Second pass: decode sampled bitmap safely
        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val sampledBitmap = openStream()?.use { BitmapFactory.decodeStream(it, null, decodeOptions) } ?: return null

        // Fine resize if needed to guarantee bounds
        val largestDim = maxOf(sampledBitmap.width, sampledBitmap.height)
        if (largestDim > maxDimension) {
            val ratio = maxDimension.toFloat() / largestDim
            val targetW = (sampledBitmap.width * ratio).toInt().coerceAtLeast(1)
            val targetH = (sampledBitmap.height * ratio).toInt().coerceAtLeast(1)
            val scaled = Bitmap.createScaledBitmap(sampledBitmap, targetW, targetH, true)
            if (scaled != sampledBitmap) {
                sampledBitmap.recycle()
            }
            scaled
        } else {
            sampledBitmap
        }
    }.getOrNull()
}

/**
 * Persists an image picked by user into app-internal storage (files/brand_assets/{assetName}.png),
 * safely downsampling it to avoid OOM, and returns the persistent file URI string.
 */
fun persistBrandAssetLocally(
    context: Context,
    sourceUri: Uri,
    assetName: String,
    maxDimension: Int = 1024
): String? {
    return runCatching {
        val bitmap = loadSafeProfileBitmap(context, sourceUri.toString(), maxDimension = maxDimension) ?: return null
        val brandDir = File(context.filesDir, "brand_assets").apply { mkdirs() }
        val destFile = File(brandDir, "${assetName}.png")
        FileOutputStream(destFile).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        bitmap.recycle()
        Uri.fromFile(destFile).toString()
    }.getOrNull()
}

/**
 * Loads an [ImageBitmap] safely from a local persistable content or file URI without memory or texture crashes.
 */
@Composable
fun rememberProfileAssetBitmap(uriValue: String): ImageBitmap? {
    val context = LocalContext.current
    return remember(uriValue) {
        if (uriValue.isBlank()) return@remember null
        loadSafeProfileBitmap(context, uriValue, maxDimension = 512)?.asImageBitmap()
    }
}

/**
 * Takes persistable URI read permission when supported so the selected logo or stamp survives app restarts.
 */
fun persistProfileAssetPermission(context: Context, uri: Uri) {
    runCatching {
        context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
}
