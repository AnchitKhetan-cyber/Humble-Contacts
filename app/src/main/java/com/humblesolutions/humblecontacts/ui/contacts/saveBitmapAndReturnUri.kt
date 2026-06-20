package com.humblesolutions.humblecontacts.ui.contacts

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

fun saveBitmapAndReturnUri(
    context: Context,
    bitmap: Bitmap
): Uri {

    val file = File(
        context.cacheDir,
        "business_card_${System.currentTimeMillis()}.jpg"
    )

    FileOutputStream(file).use {
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, it)
    }

    return FileProvider.getUriForFile(
        context,
        "${context.packageName}.provider",
        file
    )
}