package com.humblesolutions.humblecontacts.ui.nfc

import android.app.Activity
import android.app.PendingIntent
import android.content.Intent
import android.nfc.NdefMessage
import android.nfc.NfcAdapter

class NfcHelper(
    private val activity: Activity
) {

    private val nfcAdapter =
        NfcAdapter.getDefaultAdapter(activity)

    fun isNfcSupported(): Boolean =
        nfcAdapter != null

    fun isNfcEnabled(): Boolean =
        nfcAdapter?.isEnabled == true

    fun enableForegroundDispatch() {

        val intent = Intent(
            activity,
            activity::class.java
        ).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)

        val pendingIntent = PendingIntent.getActivity(
            activity,
            0,
            intent,
            PendingIntent.FLAG_MUTABLE
        )

        nfcAdapter?.enableForegroundDispatch(
            activity,
            pendingIntent,
            null,
            null
        )
    }

    fun disableForegroundDispatch() {
        nfcAdapter?.disableForegroundDispatch(activity)
    }


}