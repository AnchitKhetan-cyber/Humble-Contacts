package com.humblesolutions.humblecontacts.ui.contacts

import android.os.Parcelable
import com.humblesolutions.humblecontacts.BuildConfig
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.android.parcel.Parcelize
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

object BusinessCardParser {

    private val model = GenerativeModel(
        modelName = "gemini-3.1-flash-lite",
        apiKey = BuildConfig.GEMINI_API_KEY
    )

    suspend fun parse(text: String): ContactInfo {

        val prompt = """
You are an expert at extracting information from business cards.

Extract these fields from the OCR text.

Return ONLY valid JSON.

Do not include markdown.
Do not include explanations.
Do not wrap in ```.

Rules:
- "name" = person's full name only.
- "designation" = job title.
- "company" = organization/company name.
- "email" = email address.
- "phone" = mobile phone number including country code if present.
- "linkedin" = LinkedIn profile URL or username.
- "address" = full postal address exactly as printed on the business card. Include street, building, city, state, postal code and country if present.
- If multiple phone numbers exist, choose the mobile number.
- Ignore websites, QR codes, slogans and all social media except LinkedIn.
- Never use a website URL as the address.
- If a field is unavailable, return an empty string.

JSON format:

{
  "name": "",
  "designation": "",
  "company": "",
  "email": "",
  "phone": "",
  "linkedin": "",
  "address": ""
}

OCR TEXT:

$text
""".trimIndent()

        return try {

            val response = model.generateContent(prompt)

            android.util.Log.d(
                "GEMINI_JSON",
                response.text ?: "null"
            )

            val json = response.text
                ?.replace("```json", "")
                ?.replace("```", "")
                ?.trim()
                ?: "{}"

            val contact = Json {
                ignoreUnknownKeys = true
            }.decodeFromString<ContactInfo>(json)

            android.util.Log.d("PARSED_CONTACT", contact.toString())

            return contact

        } catch (e: Exception) {
            throw e
        }
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