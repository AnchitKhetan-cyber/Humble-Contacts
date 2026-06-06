package com.humblesolutions.humblecontacts.ui.contacts

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import java.io.File
import java.io.FileOutputStream

fun saveBitmapAndReturnUri(
    context: Context,
    bitmap: Bitmap
): Uri {

    val file = File(
        context.cacheDir,
        "business_card.jpg"
    )

    FileOutputStream(file).use { stream ->
        bitmap.compress(
            Bitmap.CompressFormat.JPEG,
            100,
            stream
        )
    }

    return Uri.fromFile(file)
}