package com.humblesolutions.humblecontacts.ui.contacts

import android.os.Parcelable
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.android.parcel.Parcelize
import kotlinx.coroutines.tasks.await
import kotlinx.serialization.Serializable

object BusinessCardParser {

    // Must match the deploy region of the `parseBusinessCard` Cloud Function (REGION in
    // functions/index.js). The function holds the Gemini API key as a secret — the app
    // ships no key and never calls Gemini directly.
    private const val REGION = "us-central1"
    private const val CALLABLE = "parseBusinessCard"

    private val functions by lazy { FirebaseFunctions.getInstance(REGION) }

    /**
     * Sends the OCR text to the authenticated Cloud Function, which calls Gemini
     * server-side and returns the parsed contact JSON. Requires a signed-in user
     * (the callable attaches the Firebase Auth token automatically).
     */
    suspend fun parse(text: String): ContactInfo {
        val result = functions
            .getHttpsCallable(CALLABLE)
            .call(mapOf("text" to text))
            .await()

        @Suppress("UNCHECKED_CAST")
        val data = result.data as? Map<String, Any?> ?: emptyMap()

        fun field(key: String) = (data[key] as? String).orEmpty()

        return ContactInfo(
            name = field("name"),
            designation = field("designation"),
            company = field("company"),
            email = field("email"),
            phone = field("phone"),
            linkedin = field("linkedin"),
            address = field("address"),
        )
    }
}

@Parcelize
@Serializable
data class ContactInfo(
    val name: String = "",
    val designation: String = "",
    val company: String = "",
    val email: String = "",
    val phone: String = "",
    val linkedin: String = "",
    val address: String = ""
) : Parcelable
