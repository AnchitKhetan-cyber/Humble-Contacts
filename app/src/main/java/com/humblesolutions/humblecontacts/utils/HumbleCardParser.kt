package com.humblesolutions.humblecontacts.utils

import com.humblesolutions.humblecontacts.data.model.VisitingCard

/**
 * The visiting-card design carried in a scanned HumbleContacts QR (#64), read
 * from the `X-HC-*` extension fields written by [VCardBuilder]. Present only when
 * the scanned code came from another HumbleContacts user — a plain vCard/OCR
 * scan yields `null`, so the scanned-card → Media behaviour is opt-in by design.
 */
object HumbleCardParser {

    /**
     * Returns the [VisitingCard] encoded in [raw], or `null` if this isn't a
     * HumbleContacts card (no `X-HC-CARD` line). Best-effort: any parse failure
     * yields `null` rather than throwing, so a malformed code just falls back to
     * the normal contact prefill.
     */
    fun parse(raw: String): VisitingCard? {
        return try {
            val lines = raw.replace("\r\n", "\n").replace("\r", "\n").split('\n')
            var card: VisitingCard? = null
            var headline = ""; var bio = ""; var website = ""; var portfolio = ""

            for (line in lines) {
                val colon = line.indexOf(':')
                if (colon <= 0) continue
                val key = line.substring(0, colon).uppercase()
                val value = line.substring(colon + 1)
                when (key) {
                    "X-HC-CARD" -> {
                        val p = value.split('|')
                        card = VisitingCard(
                            template = p.getOrNull(0)?.trim().orEmpty(),
                            accentColor = p.getOrNull(1)?.trim().orEmpty(),
                            background = p.getOrNull(2)?.trim().orEmpty(),
                            fontStyle = p.getOrNull(3)?.trim().orEmpty()
                        )
                    }
                    "X-HC-HEADLINE" -> headline = unescape(value)
                    "X-HC-BIO" -> bio = unescape(value)
                    "X-HC-WEBSITE" -> website = unescape(value)
                    "X-HC-PORTFOLIO" -> portfolio = unescape(value)
                }
            }

            card?.copy(
                headline = headline,
                bio = bio,
                websiteUrl = website,
                portfolioUrl = portfolio
            )
        } catch (e: Exception) {
            null
        }
    }

    /** Mirrors the escaping done by [VCardBuilder]. */
    private fun unescape(value: String): String =
        value.trim()
            .replace("\\n", " ")
            .replace("\\,", ",")
            .replace("\\;", ";")
            .replace("\\\\", "\\")
}
