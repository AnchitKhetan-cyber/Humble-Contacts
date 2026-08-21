package com.humblesolutions.humblecontacts.utils

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit tests for [ContactExporter.csv] — the CSV field escaping used by the
 * export. Every field is wrapped in double quotes and any embedded quote is
 * doubled, per RFC 4180, so commas / quotes / newlines inside a field never
 * break the row structure (ticket #24).
 */
class ContactExporterCsvTest {

    @Test
    fun `plain value is wrapped in quotes`() {
        assertEquals("\"Alice\"", ContactExporter.csv("Alice"))
    }

    @Test
    fun `null becomes an empty quoted field`() {
        assertEquals("\"\"", ContactExporter.csv(null))
    }

    @Test
    fun `empty string stays an empty quoted field`() {
        assertEquals("\"\"", ContactExporter.csv(""))
    }

    @Test
    fun `commas are preserved inside the quoted field`() {
        // Quoting is what keeps a comma from splitting into a new column.
        assertEquals("\"Acme, Inc.\"", ContactExporter.csv("Acme, Inc."))
    }

    @Test
    fun `embedded double quotes are doubled`() {
        // She said "hi"  ->  "She said ""hi"""
        assertEquals("\"She said \"\"hi\"\"\"", ContactExporter.csv("She said \"hi\""))
    }

    @Test
    fun `newlines are kept inside the quoted field`() {
        assertEquals("\"line1\nline2\"", ContactExporter.csv("line1\nline2"))
    }

    @Test
    fun `a lone quote is doubled`() {
        assertEquals("\"\"\"\"", ContactExporter.csv("\""))
    }

    @Test
    fun `non-string values are stringified then quoted`() {
        assertEquals("\"42\"", ContactExporter.csv(42))
    }
}
