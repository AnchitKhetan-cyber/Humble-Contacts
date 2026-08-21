package com.humblesolutions.humblecontacts.ui.contacts

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

/**
 * Creates a FileProvider URI for a **not-yet-written** cache file, for the camera
 * to write a full-resolution capture into via `ActivityResultContracts.TakePicture`.
 *
 * The previous `TakePicturePreview` contract handed back only a small thumbnail
 * bitmap, whose text was too low-resolution for OCR — the card scan produced
 * garbled output. Capturing to a full-size file fixes the source image quality.
 */
fun createImageCaptureUri(context: Context): Uri {
    val file = File(
        context.cacheDir,
        "business_card_${System.currentTimeMillis()}.jpg"
    )
    return FileProvider.getUriForFile(
        context,
        "${context.packageName}.provider",
        file
    )
}
