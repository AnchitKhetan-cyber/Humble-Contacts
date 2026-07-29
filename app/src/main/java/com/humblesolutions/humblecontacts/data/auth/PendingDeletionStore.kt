package com.humblesolutions.humblecontacts.data.auth

import android.content.Context

/**
 * Bridges the account-deletion gate across the email-confirmation-link round trip.
 *
 * Tapping the confirmation email leaves the app and returns via a deep link — a fresh
 * Activity intent — so any in-memory gate state (which channels have been completed,
 * the link itself) would be lost. The minimal flags needed to resume the deletion are
 * persisted here and cleared once deletion finishes or is abandoned.
 *
 * This holds NO credentials or secrets — only booleans, the account email, and the
 * returning email-link URL (which is single-use and only meaningful to the signed-in
 * user on this device).
 */
class PendingDeletionStore(context: Context) {

    private val prefs =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** True once the user has started the delete gate (email link sent / OTP in progress). */
    var inProgress: Boolean
        get() = prefs.getBoolean(KEY_IN_PROGRESS, false)
        set(value) = prefs.edit().putBoolean(KEY_IN_PROGRESS, value).apply()

    /** The account email the confirmation link was sent to (needed to complete the link). */
    var email: String?
        get() = prefs.getString(KEY_EMAIL, null)
        set(value) = prefs.edit().putString(KEY_EMAIL, value).apply()

    /** The returning email-link URL, stashed by MainActivity for the delete screen to consume. */
    var emailLink: String?
        get() = prefs.getString(KEY_EMAIL_LINK, null)
        set(value) = prefs.edit().putString(KEY_EMAIL_LINK, value).apply()

    /** True once the email confirmation link has been tapped and verified. */
    var emailVerified: Boolean
        get() = prefs.getBoolean(KEY_EMAIL_VERIFIED, false)
        set(value) = prefs.edit().putBoolean(KEY_EMAIL_VERIFIED, value).apply()

    /** True once the phone OTP has been entered and verified. */
    var phoneVerified: Boolean
        get() = prefs.getBoolean(KEY_PHONE_VERIFIED, false)
        set(value) = prefs.edit().putBoolean(KEY_PHONE_VERIFIED, value).apply()

    fun clear() = prefs.edit().clear().apply()

    private companion object {
        const val PREFS_NAME = "pending_deletion"
        const val KEY_IN_PROGRESS = "in_progress"
        const val KEY_EMAIL = "email"
        const val KEY_EMAIL_LINK = "email_link"
        const val KEY_EMAIL_VERIFIED = "email_verified"
        const val KEY_PHONE_VERIFIED = "phone_verified"
    }
}
