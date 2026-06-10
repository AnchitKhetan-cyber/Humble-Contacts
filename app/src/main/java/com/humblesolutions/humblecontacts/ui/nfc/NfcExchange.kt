package com.humblesolutions.humblecontacts.ui.nfc

data class NfcExchange(
    val contactName: String,
    val company: String?,
    val exchangedAt: Long
)