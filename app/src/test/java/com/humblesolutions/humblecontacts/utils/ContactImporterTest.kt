package com.humblesolutions.humblecontacts.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [ContactImporter] — the CSV → Contact parser behind CSV import.
 * Covers header-name mapping, RFC 4180 quoting (embedded commas / quotes /
 * newlines), tags splitting, blank-name skipping, and column tolerance.
 */
class ContactImporterTest {

    @Test
    fun `maps columns by header name, order-independent`() {
        val csv = """
            Company,Full Name,Email
            Acme,Alice,alice@acme.com
        """.trimIndent()

        val result = ContactImporter.parse(csv)

        assertEquals(1, result.contacts.size)
        val c = result.contacts.first()
        assertEquals("Alice", c.fullName)
        assertEquals("Acme", c.company)
        assertEquals("alice@acme.com", c.email)
    }

    @Test
    fun `rows with a blank full name are skipped and counted`() {
        val csv = "Full Name,Email\n,orphan@x.com\nBob,bob@x.com"
        val result = ContactImporter.parse(csv)

        assertEquals(1, result.contacts.size)
        assertEquals("Bob", result.contacts.first().fullName)
        assertEquals(1, result.skippedRows)
    }

    @Test
    fun `quoted field with an embedded comma stays one value`() {
        val csv = "Full Name,Company\nAlice,\"Acme, Inc.\""
        val result = ContactImporter.parse(csv)
        assertEquals("Acme, Inc.", result.contacts.first().company)
    }

    @Test
    fun `doubled quotes inside a quoted field become one quote`() {
        val csv = "Full Name,Notes\nAlice,\"She said \"\"hi\"\"\""
        val result = ContactImporter.parse(csv)
        assertEquals(
            "She said \"hi\"",
            result.contacts.first().conversationNotes.first().text
        )
    }

    @Test
    fun `newline inside a quoted field is preserved, not a row break`() {
        val csv = "Full Name,Notes\nAlice,\"line1\nline2\""
        val result = ContactImporter.parse(csv)
        assertEquals(1, result.contacts.size)
        assertEquals("line1\nline2", result.contacts.first().conversationNotes.first().text)
    }

    @Test
    fun `tags are split on semicolons and trimmed`() {
        val csv = "Full Name,Tags\nAlice,\"investor; follow-up ; expo\""
        val result = ContactImporter.parse(csv)
        assertEquals(listOf("investor", "follow-up", "expo"), result.contacts.first().tags)
    }

    @Test
    fun `unknown columns are ignored and missing ones tolerated`() {
        val csv = "Full Name,Nickname\nAlice,Ally"
        val result = ContactImporter.parse(csv)
        assertEquals("Alice", result.contacts.first().fullName)
        assertEquals("", result.contacts.first().company)
    }

    @Test
    fun `header matching is case and whitespace insensitive`() {
        val csv = "  FULL NAME , Email \nAlice,alice@x.com"
        val result = ContactImporter.parse(csv)
        assertEquals("Alice", result.contacts.first().fullName)
    }

    @Test
    fun `empty input yields no contacts`() {
        assertTrue(ContactImporter.parse("").contacts.isEmpty())
    }

    @Test
    fun `a header-only file yields no contacts`() {
        assertTrue(ContactImporter.parse("Full Name,Email").contacts.isEmpty())
    }

    @Test
    fun `CRLF line endings are handled`() {
        val csv = "Full Name,Email\r\nAlice,alice@x.com\r\n"
        val result = ContactImporter.parse(csv)
        assertEquals(1, result.contacts.size)
        assertEquals("Alice", result.contacts.first().fullName)
    }

    @Test
    fun `imported contacts are marked with the import entry method`() {
        val result = ContactImporter.parse("Full Name\nAlice")
        assertEquals("import", result.contacts.first().entryMethod)
    }
}
