package com.humblesolutions.humblecontacts.ui.contacts

import com.humblesolutions.humblecontacts.data.model.Contact

/**
 * True if [contact] matches the search [query]. Searches across the fields
 * people actually recall someone by — name, company, role, email, phone,
 * address, event name, and conversation notes, where the most distinctive
 * detail usually lives (ticket #29). Matching is case-insensitive, partial
 * (substring), and the query is trimmed; a blank query matches everything.
 *
 * NOTE: callers scan this in-memory over loaded contacts, so it's page-scoped
 * once pagination (#25) is active; an indexed search backend is the planned
 * follow-up (P2-6).
 */
fun contactMatchesQuery(contact: Contact, query: String): Boolean {
    val q = query.trim()
    return q.isBlank() ||
        contact.fullName.contains(q, ignoreCase = true) ||
        contact.company.contains(q, ignoreCase = true) ||
        contact.jobRole.contains(q, ignoreCase = true) ||
        contact.email.contains(q, ignoreCase = true) ||
        contact.phone.contains(q, ignoreCase = true) ||
        contact.address.contains(q, ignoreCase = true) ||
        contact.eventName.contains(q, ignoreCase = true) ||
        contact.conversationNotes.any { it.text.contains(q, ignoreCase = true) }
}
