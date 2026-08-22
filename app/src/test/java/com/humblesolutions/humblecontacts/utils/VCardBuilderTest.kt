package com.humblesolutions.humblecontacts.utils

import com.humblesolutions.humblecontacts.data.model.ShareSettings
import com.humblesolutions.humblecontacts.data.model.UserProfile
import com.humblesolutions.humblecontacts.data.model.VisitingCard
import com.humblesolutions.humblecontacts.ui.contacts.VCardParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VCardBuilderTest {

    private fun sampleProfile(share: ShareSettings = ShareSettings()) = UserProfile(
        name = "Alice Smith",
        email = "alice@example.com",
        profession = "Android Engineer",
        company = "Humble Solutions",
        phone = "9876543210",
        countryCode = "+91",
        linkedInUrl = "https://www.linkedin.com/in/alice-smith",
        address = "221B Baker Street",
        shareSettings = share,
        visitingCard = VisitingCard(
            headline = "Building delightful apps",
            bio = "I love clean architecture.",
            websiteUrl = "https://alice.dev",
            portfolioUrl = "https://alice.dev/work"
        )
    )

    @Test
    fun `built vCard round-trips through VCardParser`() {
        val vcard = VCardBuilder.build(sampleProfile())
        val parsed = VCardParser.parse(vcard)

        assertEquals("Alice Smith", parsed.name)
        assertEquals("Android Engineer", parsed.designation)
        assertEquals("Humble Solutions", parsed.company)
        assertEquals("alice@example.com", parsed.email)
        assertEquals("+919876543210", parsed.phone)
        assertEquals("221B Baker Street", parsed.address)
        // The parser extracts the LinkedIn username from the full profile URL.
        assertEquals("alice-smith", parsed.linkedin)
    }

    @Test
    fun `is a well-formed vCard`() {
        val vcard = VCardBuilder.build(sampleProfile())
        assertTrue(vcard.startsWith("BEGIN:VCARD"))
        assertTrue(vcard.contains("VERSION:3.0"))
        assertTrue(vcard.trimEnd().endsWith("END:VCARD"))
    }

    @Test
    fun `share settings gate phone email company and linkedin`() {
        val locked = ShareSettings(
            sharePhone = false,
            shareEmail = false,
            shareCompany = false,
            shareLinkedIn = false
        )
        val vcard = VCardBuilder.build(sampleProfile(locked))

        // The toggled-off fields must not appear at all.
        assertFalse("phone leaked", vcard.contains("TEL"))
        assertFalse("email leaked", vcard.contains("EMAIL"))
        assertFalse("company leaked", vcard.contains("ORG"))
        assertFalse("linkedin leaked", vcard.contains("linkedin"))

        // Name and address (not gated) still present.
        assertTrue(vcard.contains("Alice Smith"))
        assertTrue(vcard.contains("Baker Street"))

        val parsed = VCardParser.parse(vcard)
        assertEquals("", parsed.phone)
        assertEquals("", parsed.email)
        assertEquals("", parsed.company)
        assertEquals("", parsed.linkedin)
    }

    @Test
    fun `embedded HumbleContacts card round-trips through HumbleCardParser`() {
        val profile = sampleProfile().copy(
            visitingCard = VisitingCard(
                template = "executive",
                accentColor = "#1A2D5A",
                background = "solid",
                fontStyle = "serif",
                headline = "Building delightful apps",
                bio = "I love clean architecture.",
                websiteUrl = "https://alice.dev",
                portfolioUrl = "https://alice.dev/work"
            )
        )
        val vcard = VCardBuilder.build(profile)
        val card = HumbleCardParser.parse(vcard)

        assertTrue("should detect a HumbleContacts card", card != null)
        assertEquals("executive", card!!.template)
        assertEquals("#1A2D5A", card.accentColor)
        assertEquals("solid", card.background)
        assertEquals("serif", card.fontStyle)
        assertEquals("Building delightful apps", card.headline)
        assertEquals("I love clean architecture.", card.bio)
        assertEquals("https://alice.dev", card.websiteUrl)
        assertEquals("https://alice.dev/work", card.portfolioUrl)
    }

    @Test
    fun `a plain vCard is not detected as a HumbleContacts card`() {
        val plain = "BEGIN:VCARD\r\nVERSION:3.0\r\nFN:Bob Jones\r\nEND:VCARD\r\n"
        assertTrue(HumbleCardParser.parse(plain) == null)
    }

    @Test
    fun `special characters are escaped and survive parsing`() {
        // Comma and semicolon are vCard value delimiters; the builder must escape
        // them so they come back intact. (Tested on the name, which the parser
        // reads whole via FN — ORG/ADR use ';' as a structural separator.)
        val profile = sampleProfile().copy(name = "Alice, Smith; Jr.")
        val vcard = VCardBuilder.build(profile)
        // The raw value is escaped in the serialized form...
        assertTrue(vcard.contains("FN:Alice\\, Smith\\; Jr."))
        // ...and comes back intact after parsing.
        val parsed = VCardParser.parse(vcard)
        assertEquals("Alice, Smith; Jr.", parsed.name)
    }
}
