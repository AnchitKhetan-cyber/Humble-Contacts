package com.humblesolutions.humblecontacts.utils

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

/**
 * Local sharing/export helpers for the digital visiting card (#64).
 *
 * Nothing is uploaded — the image and `.vcf` are written to the app cache and
 * handed to other apps through the existing `${applicationId}.provider`
 * FileProvider (see AndroidManifest `file_paths` → cache-path). Gallery saves go
 * through MediaStore, mirroring `ContactExporter`'s Downloads write.
 */
object CardShareUtils {

    private const val AUTHORITY_SUFFIX = ".provider"

    /** Writes [bitmap] as a PNG to cache and opens the Android share sheet. */
    fun shareCardImage(context: Context, bitmap: Bitmap, baseName: String) {
        val file = File(context.cacheDir, "$baseName.png")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        val uri = FileProvider.getUriForFile(
            context,
            context.packageName + AUTHORITY_SUFFIX,
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(
            Intent.createChooser(intent, "Share visiting card").apply {
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        )
    }

    /**
     * Saves [bitmap] to the public gallery (Pictures/Humble Contacts). Returns
     * true on success. Uses MediaStore on API 29+ (scoped storage); on older
     * APIs falls back to the app's external files dir.
     */
    fun saveCardImageToGallery(context: Context, bitmap: Bitmap, baseName: String): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, "$baseName.png")
                    put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                    put(
                        MediaStore.Images.Media.RELATIVE_PATH,
                        Environment.DIRECTORY_PICTURES + "/Humble Contacts"
                    )
                }
                val uri: Uri = context.contentResolver.insert(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values
                ) ?: return false
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                } ?: return false
                true
            } else {
                @Suppress("DEPRECATION")
                val dir = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                    "Humble Contacts"
                )
                if (!dir.exists()) dir.mkdirs()
                val file = File(dir, "$baseName.png")
                FileOutputStream(file).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                }
                true
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /** Writes [vcard] as a `.vcf` to cache and opens the Android share sheet. */
    fun shareVCard(context: Context, vcard: String, baseName: String) {
        val file = File(context.cacheDir, "$baseName.vcf")
        file.writeText(vcard)
        val uri = FileProvider.getUriForFile(
            context,
            context.packageName + AUTHORITY_SUFFIX,
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/x-vcard"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(
            Intent.createChooser(intent, "Share contact card").apply {
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        )
    }
}
