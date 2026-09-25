package com.goldex.companion.ui.components

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
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
 * Loads an [ImageBitmap] safely from a local persistable content URI.
 */
@Composable
fun rememberProfileAssetBitmap(uriValue: String): ImageBitmap? {
    val context = LocalContext.current
    return remember(uriValue) {
        if (uriValue.isBlank()) return@remember null
        runCatching {
            context.contentResolver.openInputStream(Uri.parse(uriValue))
                ?.use { BitmapFactory.decodeStream(it) }
                ?.asImageBitmap()
        }.getOrNull()
    }
}

/**
 * Takes persistable URI read permission so the selected logo or stamp survives app restarts.
 */
fun persistProfileAssetPermission(context: Context, uri: Uri) {
    runCatching {
        context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
}
