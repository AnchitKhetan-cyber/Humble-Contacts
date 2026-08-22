package com.humblesolutions.humblecontacts.utils

import com.humblesolutions.humblecontacts.data.model.UserProfile

/**
 * Builds a vCard 3.0 string for the user's own digital visiting card (#64).
 *
 * The card's display fields come from [UserProfile]; the card-specific extras
 * (headline, bio, website, portfolio) come from `profile.visitingCard`. This is
 * the single source for both the shared `.vcf` file and the card's QR code, so
 * anyone who scans the QR with the app's existing scanner (or any vCard-aware
 * app) can import the contact.
 *
 * Output is written to round-trip cleanly through
 * [com.humblesolutions.humblecontacts.ui.contacts.VCardParser]:
 * `FN`, `N`, `ORG`, `TITLE`, `TEL`, `EMAIL`, `ADR`, `URL`.
 *
 * ## Privacy
 * Honours the profile's [com.humblesolutions.humblecontacts.data.model.ShareSettings]:
 * a field the user has toggled off (phone / email / company / LinkedIn) is
 * omitted entirely — it never reaches the `.vcf`, the QR, or the shared image.
 */
object VCardBuilder {

    fun build(profile: UserProfile): String {
        val share = profile.shareSettings
        val card = profile.visitingCard
        val sb = StringBuilder()

        sb.append("BEGIN:VCARD\r\n")
        sb.append("VERSION:3.0\r\n")

        val name = profile.name.trim()
        if (name.isNotBlank()) {
            // N is structured (Family;Given;...); FN is the display name. We only
            // have a single display name, so put it in the Given slot of N.
            sb.append("N:;").append(escape(name)).append(";;;\r\n")
            sb.append("FN:").append(escape(name)).append("\r\n")
        }

        if (share.shareCompany && profile.company.isNotBlank()) {
            sb.append("ORG:").append(escape(profile.company.trim())).append("\r\n")
        }

        if (profile.profession.isNotBlank()) {
            sb.append("TITLE:").append(escape(profile.profession.trim())).append("\r\n")
        }

        if (share.sharePhone && profile.phone.isNotBlank()) {
            val full = "${profile.countryCode}${profile.phone}".trim()
            sb.append("TEL;TYPE=CELL:").append(escape(full)).append("\r\n")
        }

        if (share.shareEmail && profile.email.isNotBlank()) {
            sb.append("EMAIL;TYPE=INTERNET:").append(escape(profile.email.trim())).append("\r\n")
        }

        if (profile.address.isNotBlank()) {
            // Put the whole address in the street slot of the structured ADR.
            sb.append("ADR;TYPE=WORK:;;").append(escape(profile.address.trim())).append(";;;;\r\n")
        }

        if (share.shareLinkedIn && profile.linkedInUrl.isNotBlank()) {
            sb.append("URL:").append(escape(profile.linkedInUrl.trim())).append("\r\n")
        }
        if (card.websiteUrl.isNotBlank()) {
            sb.append("URL:").append(escape(card.websiteUrl.trim())).append("\r\n")
        }
        if (card.portfolioUrl.isNotBlank()) {
            sb.append("URL:").append(escape(card.portfolioUrl.trim())).append("\r\n")
        }

        // Headline + bio have no dedicated vCard field; carry them in NOTE.
        val note = listOf(card.headline.trim(), card.bio.trim())
            .filter { it.isNotBlank() }
            .joinToString(" — ")
        if (note.isNotBlank()) {
            sb.append("NOTE:").append(escape(note)).append("\r\n")
        }

        // HumbleContacts extension fields (#64) — invisible to standard vCard
        // parsers (X- prefix) but let another HumbleContacts app faithfully
        // rebuild the *visual* card when it scans this code, to store under the
        // contact's Media. See HumbleCardParser.
        sb.append("X-HC-CARD:")
            .append(card.template).append('|')
            .append(card.accentColor).append('|')
            .append(card.background).append('|')
            .append(card.fontStyle).append("\r\n")
        if (card.headline.isNotBlank()) sb.append("X-HC-HEADLINE:").append(escape(card.headline.trim())).append("\r\n")
        if (card.bio.isNotBlank()) sb.append("X-HC-BIO:").append(escape(card.bio.trim())).append("\r\n")
        if (card.websiteUrl.isNotBlank()) sb.append("X-HC-WEBSITE:").append(escape(card.websiteUrl.trim())).append("\r\n")
        if (card.portfolioUrl.isNotBlank()) sb.append("X-HC-PORTFOLIO:").append(escape(card.portfolioUrl.trim())).append("\r\n")

        sb.append("END:VCARD\r\n")
        return sb.toString()
    }

    /** RFC 6350 value escaping: backslash, newline, comma, semicolon. */
    private fun escape(value: String): String =
        value
            .replace("\\", "\\\\")
            .replace("\n", "\\n")
            .replace(",", "\\,")
            .replace(";", "\\;")
}
