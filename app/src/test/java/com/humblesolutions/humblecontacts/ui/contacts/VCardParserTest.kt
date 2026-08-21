package com.humblesolutions.humblecontacts.ui.contacts

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit tests for [VCardParser] — the defensive vCard → [ContactInfo] parser
 * behind the QR/vCard prefill path. Covers the happy path plus the edge cases
 * the parser is meant to survive: folded lines, escaping, structured names,
 * mobile-number preference, LinkedIn username extraction, and malformed input
 * (which must yield a blank [ContactInfo], never crash) (ticket #24).
 */
class VCardParserTest {

    @Test
    fun `parses a basic vCard`() {
        val vcard = """
            BEGIN:VCARD
            VERSION:3.0
            FN:Alice Smith
            ORG:Acme Inc.
            TITLE:Engineer
            EMAIL:alice@acme.com
            TEL:+11234567890
            END:VCARD
        """.trimIndent()

        val info = VCardParser.parse(vcard)

        assertEquals("Alice Smith", info.name)
        assertEquals("Acme Inc.", info.company)
        assertEquals("Engineer", info.designation)
        assertEquals("alice@acme.com", info.email)
        assertEquals("+11234567890", info.phone)
    }

    @Test
    fun `blank or malformed input returns an empty ContactInfo, never crashes`() {
        assertEquals(ContactInfo(), VCardParser.parse(""))
        assertEquals(ContactInfo(), VCardParser.parse("not a vcard at all"))
    }

    @Test
    fun `structured N is rendered when FN is absent`() {
        // N = Family;Given;Additional;Prefix;Suffix
        val vcard = "BEGIN:VCARD\nN:Smith;Alice;Q;Dr;PhD\nEND:VCARD"
        assertEquals("Dr Alice Q Smith PhD", VCardParser.parse(vcard).name)
    }

    @Test
    fun `FN is preferred over N`() {
        val vcard = "BEGIN:VCARD\nFN:Alice Smith\nN:Smith;Alice;;;\nEND:VCARD"
        assertEquals("Alice Smith", VCardParser.parse(vcard).name)
    }

    @Test
    fun `a mobile number is preferred over a non-mobile one`() {
        val vcard = """
            BEGIN:VCARD
            TEL;TYPE=WORK:111
            TEL;TYPE=CELL:222
            END:VCARD
        """.trimIndent()
        assertEquals("222", VCardParser.parse(vcard).phone)
    }

    @Test
    fun `falls back to a non-mobile number when no mobile is present`() {
        val vcard = "BEGIN:VCARD\nTEL;TYPE=WORK:111\nEND:VCARD"
        assertEquals("111", VCardParser.parse(vcard).phone)
    }

    @Test
    fun `LinkedIn username is extracted from a profile URL`() {
        val vcard = "BEGIN:VCARD\nURL:https://www.linkedin.com/in/alice-smith/\nEND:VCARD"
        assertEquals("alice-smith", VCardParser.parse(vcard).linkedin)
    }

    @Test
    fun `non-LinkedIn URLs are ignored`() {
        val vcard = "BEGIN:VCARD\nURL:https://example.com/alice\nEND:VCARD"
        assertEquals("", VCardParser.parse(vcard).linkedin)
    }

    @Test
    fun `folded continuation lines are unfolded by direct concatenation`() {
        // Per RFC 6350 a fold is CRLF + a single leading space; unfolding drops
        // that space and concatenates directly (no space inserted).
        val vcard = "BEGIN:VCARD\nEMAIL:alice\n smith@example.com\nEND:VCARD"
        assertEquals("alicesmith@example.com", VCardParser.parse(vcard).email)
    }

    @Test
    fun `escaped commas and semicolons are unescaped in values`() {
        val vcard = "BEGIN:VCARD\nORG:Acme\\, Inc.\nEND:VCARD"
        assertEquals("Acme, Inc.", VCardParser.parse(vcard).company)
    }

    @Test
    fun `ADR components are joined into a readable address`() {
        // ADR = PO;Ext;Street;Locality;Region;Postal;Country
        val vcard = "BEGIN:VCARD\nADR:;;221B Baker St;London;;NW1;UK\nEND:VCARD"
        assertEquals("221B Baker St, London, NW1, UK", VCardParser.parse(vcard).address)
    }

    @Test
    fun `the first email wins when several are present`() {
        val vcard = "BEGIN:VCARD\nEMAIL:first@x.com\nEMAIL:second@x.com\nEND:VCARD"
        assertEquals("first@x.com", VCardParser.parse(vcard).email)
    }

    @Test
    fun `CRLF line endings are handled`() {
        val vcard = "BEGIN:VCARD\r\nFN:Alice Smith\r\nEND:VCARD"
        assertEquals("Alice Smith", VCardParser.parse(vcard).name)
    }
}
