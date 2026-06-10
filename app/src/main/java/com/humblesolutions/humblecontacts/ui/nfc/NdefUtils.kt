package com.humblesolutions.humblecontacts.ui.nfc

import android.nfc.NdefMessage
import android.nfc.NdefRecord

object NdefUtils {

    fun createMessage(payload: String): NdefMessage {

        val mimeRecord = NdefRecord.createMime(
            "application/com.humblesolutions.humblecontacts",
            payload.toByteArray()
        )

        return NdefMessage(
            arrayOf(mimeRecord)
        )
    }
}