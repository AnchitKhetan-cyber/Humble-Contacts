package com.humblesolutions.humblecontacts.ui.contacts

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit tests for [addTag] — the tag-input helper that trims, ignores blanks,
 * and collapses duplicates case-insensitively while preserving the first-seen
 * spelling and order (ticket #24; behaviour from #16).
 */
class TagInputTest {

    @Test
    fun `adds a trimmed tag to an empty list`() {
        assertEquals(listOf("investor"), addTag(emptyList(), "  investor  "))
    }

    @Test
    fun `appends to existing tags preserving order`() {
        assertEquals(listOf("a", "b"), addTag(listOf("a"), "b"))
    }

    @Test
    fun `blank input is ignored`() {
        val existing = listOf("a")
        assertEquals(existing, addTag(existing, ""))
        assertEquals(existing, addTag(existing, "   "))
    }

    @Test
    fun `exact duplicate is not added`() {
        assertEquals(listOf("sales"), addTag(listOf("sales"), "sales"))
    }

    @Test
    fun `duplicate is detected case-insensitively`() {
        assertEquals(listOf("Sales"), addTag(listOf("Sales"), "sales"))
        assertEquals(listOf("Sales"), addTag(listOf("Sales"), "SALES"))
    }

    @Test
    fun `first-seen spelling is preserved on a case-insensitive duplicate`() {
        // "Sales" already present, so "sales" is dropped and the original kept.
        assertEquals(listOf("Sales"), addTag(listOf("Sales"), "  sales "))
    }
}
