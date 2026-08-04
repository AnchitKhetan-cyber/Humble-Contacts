package com.humblesolutions.humblecontacts.ui.contacts

/**
 * Best-effort parser that turns a raw vCard string (as emitted by most contact
 * apps when they export a QR code) into the same [ContactInfo] shape the
 * business-card OCR flow produces, so both prefill the Add Contact form through
 * one path.
 *
 * Everything here is defensive: unknown lines are ignored and any failure
 * returns an empty [ContactInfo], so a malformed or partial vCard opens a blank
 * form instead of crashing (see ticket #7 acceptance criteria).
 */
object VCardParser {

    fun parse(raw: String): ContactInfo {
        return try {
            parseInternal(raw)
        } catch (e: Exception) {
            android.util.Log.w("VCardParser", "Failed to parse vCard, returning blank", e)
            ContactInfo()
        }
    }

    private fun parseInternal(raw: String): ContactInfo {
        val lines = unfold(raw)

        var formattedName = ""
        var structuredName = ""
        var org = ""
        var title = ""
        var email = ""
        var address = ""
        var linkedin = ""

        // Collect all phone numbers so we can prefer a mobile one.
        var preferredPhone = ""
        var fallbackPhone = ""

        for (line in lines) {
            val colon = line.indexOf(':')
            if (colon <= 0) continue

            val rawKey = line.substring(0, colon)
            val value = unescape(line.substring(colon + 1).trim())
            if (value.isBlank()) continue

            // Property name is everything before the first `;` (params follow it).
            val property = rawKey.substringBefore(';').trim().uppercase()
            val params = rawKey.substringAfter(';', "").uppercase()

            when (property) {
                "FN" -> formattedName = value
                "N" -> structuredName = formatStructuredName(value)
                "ORG" -> org = value.split(';').firstOrNull { it.isNotBlank() }?.trim() ?: value
                "TITLE" -> title = value
                "EMAIL" -> if (email.isBlank()) email = value
                "ADR" -> if (address.isBlank()) address = formatAddress(value)
                "TEL" -> {
                    if (params.contains("CELL") || params.contains("MOBILE")) {
                        if (preferredPhone.isBlank()) preferredPhone = value
                    } else if (fallbackPhone.isBlank()) {
                        fallbackPhone = value
                    }
                }
                "URL", "X-SOCIALPROFILE" -> {
                    if (linkedin.isBlank() && value.contains("linkedin", ignoreCase = true)) {
                        linkedin = extractLinkedInUsername(value)
                    }
                }
            }
        }

        val name = if (formattedName.isNotBlank()) formattedName else structuredName
        val phone = if (preferredPhone.isNotBlank()) preferredPhone else fallbackPhone

        return ContactInfo(
            name = name,
            designation = title,
            company = org,
            email = email,
            phone = phone,
            linkedin = linkedin,
            address = address
        )
    }

    /**
     * Joins RFC 6350 folded lines. Continuation lines begin with a space or tab
     * and belong to the previous logical line.
     */
    private fun unfold(raw: String): List<String> {
        val result = mutableListOf<String>()
        for (line in raw.replace("\r\n", "\n").replace("\r", "\n").split('\n')) {
            if (line.isEmpty()) continue
            if ((line[0] == ' ' || line[0] == '\t') && result.isNotEmpty()) {
                result[result.lastIndex] = result.last() + line.substring(1)
            } else {
                result.add(line)
            }
        }
        return result
    }

    /** vCard escapes `\n`, `\,`, `\;` and `\\` within values. */
    private fun unescape(value: String): String =
        value
            .replace("\\n", " ")
            .replace("\\N", " ")
            .replace("\\,", ",")
            .replace("\\;", ";")
            .replace("\\\\", "\\")
            .trim()

    /** N is `Family;Given;Additional;Prefix;Suffix` — render as "Prefix Given Additional Family Suffix". */
    private fun formatStructuredName(value: String): String {
        val parts = value.split(';')
        val family = parts.getOrNull(0)?.trim().orEmpty()
        val given = parts.getOrNull(1)?.trim().orEmpty()
        val additional = parts.getOrNull(2)?.trim().orEmpty()
        val prefix = parts.getOrNull(3)?.trim().orEmpty()
        val suffix = parts.getOrNull(4)?.trim().orEmpty()
        return listOf(prefix, given, additional, family, suffix)
            .filter { it.isNotBlank() }
            .joinToString(" ")
    }

    /** ADR is `PO;Ext;Street;Locality;Region;Postal;Country` — join the non-blank parts. */
    private fun formatAddress(value: String): String =
        value.split(';')
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .joinToString(", ")

    /**
     * The form stores a LinkedIn username and prepends the profile URL on save,
     * so pull just the username out of a full profile URL when we can.
     */
    private fun extractLinkedInUsername(value: String): String {
        val marker = "linkedin.com/in/"
        val idx = value.indexOf(marker, ignoreCase = true)
        if (idx == -1) return value.trim()
        return value.substring(idx + marker.length)
            .substringBefore('/')
            .substringBefore('?')
            .trim()
    }
}
