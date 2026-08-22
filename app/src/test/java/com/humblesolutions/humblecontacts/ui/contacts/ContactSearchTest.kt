package com.humblesolutions.humblecontacts.ui.contacts

import com.humblesolutions.humblecontacts.data.model.Contact
import com.humblesolutions.humblecontacts.data.model.ContactNote
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [contactMatchesQuery] — the widened search predicate that
 * matches name, company, role, email, phone, address, event name, and
 * conversation notes, case-insensitively and on partial (substring) input,
 * with the query trimmed (ticket #29).
 */
class ContactSearchTest {

    private val contact = Contact(
        fullName = "Sarah Connor",
        jobRole = "Engineer",
        company = "Acme Corp",
        email = "sarah@acme.io",
        phone = "+1 555 0134",
        address = "42 Baker Street, London",
        eventName = "AI Summit 2026",
        conversationNotes = listOf(
            ContactNote(text = "Met at the climbing gym, into trail running"),
            ContactNote(text = "Follow up about the API integration")
        )
    )

    // --- new fields (the ticket) ---

    @Test
    fun `matches on email`() {
        assertTrue(contactMatchesQuery(contact, "@acme"))
    }

    @Test
    fun `matches on phone`() {
        assertTrue(contactMatchesQuery(contact, "555 0134"))
    }

    @Test
    fun `matches on address`() {
        assertTrue(contactMatchesQuery(contact, "Baker Street"))
    }

    @Test
    fun `matches on event name`() {
        assertTrue(contactMatchesQuery(contact, "AI Summit"))
    }

    @Test
    fun `matches text in any conversation note`() {
        assertTrue(contactMatchesQuery(contact, "climbing"))
        // second note in the list also matches
        assertTrue(contactMatchesQuery(contact, "API integration"))
    }

    // --- original fields still work ---

    @Test
    fun `matches on name, company and role`() {
        assertTrue(contactMatchesQuery(contact, "Connor"))
        assertTrue(contactMatchesQuery(contact, "Acme"))
        assertTrue(contactMatchesQuery(contact, "Engineer"))
    }

    // --- matching semantics (acceptance criteria) ---

    @Test
    fun `matching is case-insensitive`() {
        assertTrue(contactMatchesQuery(contact, "sarah"))
        assertTrue(contactMatchesQuery(contact, "SARAH"))
        assertTrue(contactMatchesQuery(contact, "CLIMBING"))
    }

    @Test
    fun `matching is partial`() {
        assertTrue(contactMatchesQuery(contact, "acm"))
    }

    @Test
    fun `query is trimmed`() {
        assertTrue(contactMatchesQuery(contact, "  Connor  "))
    }

    @Test
    fun `blank query matches everything`() {
        assertTrue(contactMatchesQuery(contact, ""))
        assertTrue(contactMatchesQuery(contact, "   "))
    }

    @Test
    fun `no match when query is absent from all fields`() {
        assertFalse(contactMatchesQuery(contact, "zzz-nonexistent"))
    }

    @Test
    fun `no note text does not crash and does not match`() {
        val noNotes = contact.copy(conversationNotes = emptyList())
        assertFalse(contactMatchesQuery(noNotes, "climbing"))
    }
}
