package com.humblesolutions.humblecontacts.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.Exclude

/**
 * One entry in a contact's conversation-notes timeline.
 *
 * A note is either **text** ([text] set) or a **voice note** ([audioUrl] set to
 * the Storage download URL of the recording). [durationMs] is the recording
 * length, used to render the player. All fields default so older text-only
 * notes deserialize unchanged.
 */
@Keep
data class ContactNote(
    val text: String = "",
    val createdAt: Timestamp? = null,
    val audioUrl: String = "",
    val durationMs: Long = 0L
) {
    @get:Exclude
    val isVoice: Boolean get() = audioUrl.isNotBlank()
}
