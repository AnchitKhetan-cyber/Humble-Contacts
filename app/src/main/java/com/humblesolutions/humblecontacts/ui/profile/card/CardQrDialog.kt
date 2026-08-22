package com.humblesolutions.humblecontacts.ui.profile.card

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.humblesolutions.humblecontacts.R
import com.humblesolutions.humblecontacts.utils.generateQrBitmap

/**
 * A dialog that shows a scannable QR code for the user's visiting card (#64).
 *
 * [content] is the vCard string (from `VCardBuilder`), so scanning the code with
 * the app's own scanner — or any vCard-aware camera — drops the viewer into a
 * prefilled Add Contact form. [onShare] receives the rendered QR bitmap so the
 * caller can hand it to the Android share sheet. Shared by the editor and the
 * Profile screen.
 */
@Composable
fun CardQrDialog(
    content: String,
    onShare: (Bitmap) -> Unit,
    onDismiss: () -> Unit
) {
    val qr = remember(content) { generateQrBitmap(content, sizePx = 640) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                stringResource(R.string.card_qr_title),
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = androidx.compose.ui.graphics.Color.White,
                    modifier = Modifier.padding(8.dp)
                ) {
                    Box(Modifier.padding(12.dp)) {
                        Image(
                            bitmap = qr.asImageBitmap(),
                            contentDescription = stringResource(R.string.card_qr_title),
                            modifier = Modifier.size(240.dp)
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    stringResource(R.string.card_qr_hint),
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onShare(qr) }) {
                Text(stringResource(R.string.card_qr_share))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.card_close))
            }
        }
    )
}
