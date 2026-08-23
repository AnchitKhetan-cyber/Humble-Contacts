package com.humblesolutions.humblecontacts.utils

import android.graphics.Bitmap
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter

/**
 * Encodes [content] into a square QR-code [Bitmap] of [sizePx] × [sizePx].
 *
 * Extracted from LinkedAccountsScreen (#20) so both the linked-accounts QR and
 * the visiting-card QR (#64) share one implementation. Uses ZXing, already a
 * project dependency.
 */
fun generateQrBitmap(content: String, sizePx: Int): Bitmap {
    val writer = QRCodeWriter()
    val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx)
    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.RGB_565)
    for (x in 0 until sizePx) {
        for (y in 0 until sizePx) {
            bitmap.setPixel(
                x, y,
                if (bitMatrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE
            )
        }
    }
    return bitmap
}
