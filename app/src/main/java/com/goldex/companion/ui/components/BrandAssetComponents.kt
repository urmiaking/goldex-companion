package com.goldex.companion.ui.components

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
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
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
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
 * Robust image picker composable with multi-level fallback chain:
 * 1. Modern Android PhotoPicker (ActivityResultContracts.PickVisualMedia)
 * 2. Native Gallery App (Intent.ACTION_PICK with MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
 * 3. Storage Access Framework (ActivityResultContracts.OpenDocument)
 * 4. Generic Content Picker (Intent.ACTION_GET_CONTENT)
 */
@Composable
fun rememberBrandImagePicker(
    onImagePicked: (Uri) -> Unit,
    onError: (String) -> Unit = {}
): () -> Unit {
    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) onImagePicked(uri)
    }

    val galleryPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val uri = result.data?.data
        if (uri != null) onImagePicked(uri)
    }

    val openDocPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) onImagePicked(uri)
    }

    return remember(photoPicker, galleryPicker, openDocPicker) {
        {
            // 1. Try Modern Android Photo Picker (PickVisualMedia)
            try {
                photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            } catch (_: Exception) {
                // 2. Try Standard Gallery App (ACTION_PICK on MediaStore)
                try {
                    val pickIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI).apply {
                        type = "image/*"
                    }
                    galleryPicker.launch(pickIntent)
                } catch (_: Exception) {
                    // 3. Try SAF Documents / OpenDocument
                    try {
                        openDocPicker.launch(arrayOf("image/*", "image/png", "image/jpeg", "image/webp"))
                    } catch (_: Exception) {
                        // 4. Try ACTION_GET_CONTENT
                        try {
                            val getContentIntent = Intent(Intent.ACTION_GET_CONTENT).apply {
                                type = "image/*"
                                addCategory(Intent.CATEGORY_OPENABLE)
                            }
                            galleryPicker.launch(getContentIntent)
                        } catch (_: Exception) {
                            onError("برنامه‌ای جهت انتخاب تصویر پیدا نشد.")
                        }
                    }
                }
            }
        }
    }
}

/**
 * Safely decodes a bitmap with inSampleSize and bounded dimensions to prevent OutOfMemoryError and OpenGL texture crashes.
 */
fun loadSafeProfileBitmap(context: Context, uriValue: String, maxDimension: Int = 512): Bitmap? {
    if (uriValue.isBlank()) return null
    return runCatching {
        fun openStream(): InputStream? {
            val candidateFile = when {
                uriValue.startsWith("file:") -> {
                    val stripped = uriValue.substringAfter("file:").trimStart('/')
                    File("/$stripped").takeIf { it.exists() }
                        ?: (runCatching { Uri.parse(uriValue).path?.let { File(it) } }.getOrNull())?.takeIf { it.exists() }
                }
                uriValue.startsWith("/") -> File(uriValue).takeIf { it.exists() }
                else -> null
            }
            if (candidateFile != null && candidateFile.exists()) {
                return candidateFile.inputStream()
            }
            return runCatching { context.contentResolver.openInputStream(Uri.parse(uriValue)) }.getOrNull()
        }

        // First pass: decode bounds only (zero memory allocated)
        val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        openStream()?.use { BitmapFactory.decodeStream(it, null, boundsOptions) } ?: return null
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
