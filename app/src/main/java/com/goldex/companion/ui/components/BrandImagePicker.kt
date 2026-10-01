package com.goldex.companion.ui.components

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.provider.MediaStore
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts

/** Opens the document picker first, then tries the available image providers on OEM devices. */
internal fun launchBrandImagePicker(context: Context, launch: (Intent) -> Unit) {
    val candidates = listOf(
        {
            Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                type = "image/*"
                addCategory(Intent.CATEGORY_OPENABLE)
            }
        },
        { ActivityResultContracts.GetContent().createIntent(context, "image/*") },
        {
            ActivityResultContracts.PickVisualMedia().createIntent(
                context,
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        },
        // Some gallery apps support a MIME-only ACTION_PICK, but not the MediaStore URI.
        { Intent(Intent.ACTION_PICK).apply { type = "image/*" } },
        {
            Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI).apply {
                type = "image/*"
            }
        },
        // OEM content providers may not advertise CATEGORY_OPENABLE.
        { Intent(Intent.ACTION_GET_CONTENT).apply { type = "image/*" } }
    )
    val tried = mutableSetOf<String>()
    var lastFailure: RuntimeException? = null
    for (candidate in candidates) {
        try {
            val intent = candidate()
            val key = "${intent.action}|${intent.type}|${intent.data}|${intent.categories}"
            if (!tried.add(key)) continue
            launch(intent)
            return
        } catch (failure: ActivityNotFoundException) {
            lastFailure = failure
        } catch (failure: SecurityException) {
            lastFailure = failure
        }
    }
    throw requireNotNull(lastFailure)
}
