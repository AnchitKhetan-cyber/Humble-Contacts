# Handoff — ticket #29

**Ticket:** #29 — [P2][Enhancement] Search covers only three fields

## Summary
Contact search previously matched only `fullName`, `company`, and `jobRole`, leaving email, phone, address, event name, and conversation notes unsearchable — even though notes usually hold the most distinctive detail about a person. This change widens the search predicate in `ContactViewModel.filtered()` to also match `email`, `phone`, `address`, `eventName`, and any conversation note's text. Matching stays case-insensitive and partial (`contains(..., ignoreCase = true)`), and the query is now trimmed once up front and reused for every field. No UI change was needed — `ContactsScreen` already feeds its search box through this predicate. The scan remains in-memory over loaded contacts; a code comment records that this is page-scoped once pagination (#25) lands, with an indexed search backend as the planned follow-up.

## Files changed

**Contacts / search logic**
- `app/src/main/java/com/humblesolutions/humblecontacts/ui/contacts/ContactViewModel.kt` — in `filtered()`, trim `searchQuery` once into `query`; add `email`, `phone`, `address`, `eventName`, and `conversationNotes.any { it.text… }` to the `matchesSearch` predicate (all case-insensitive). Added an explanatory comment noting the in-memory/page-scoped nature and the indexed-backend follow-up. The filter-chip logic below (`matchesFilter`) is untouched.

## How to test
No new UI; verify through the contacts search box on a device/emulator with a few contacts that have data in the new fields.

1. **Notes match:** add a contact with a conversation note like "met at the climbing gym"; type `climbing` in search → the contact appears.
2. **Email match:** search a fragment of a contact's email (e.g. `@acme`) → the contact appears.
3. **Phone match:** search a digit fragment of a phone number → the contact appears.
4. **Address match:** search part of an address (e.g. a street or city) → the contact appears.
5. **Event name match:** search part of an event name → the contact appears.
6. **Case-insensitive + partial:** search `SARAH` finds "Sarah"; search `acm` finds "Acme Corp".
7. **Trimmed:** search `sarah ` (trailing space) → still matches "Sarah".
8. **Empty query:** clear the box → the full list returns.
9. **Regression (original fields):** name, company, and job-role searches still work; the "By …" filter chips (Favourites / Industry / Event / Date) still behave as before.

## Acceptance criteria
- [x] **Searching matches email, phone, notes, address, and event name in addition to name/company/role** — all five fields added to `matchesSearch`; notes via `conversationNotes.any { it.text.contains(query, ignoreCase = true) }`.
- [x] **Matching is case-insensitive and handles partial matches** — every clause uses `contains(query, ignoreCase = true)` (substring = partial).
- [x] **No noticeable input lag on the current in-memory dataset** — same single `contacts.filter { }` pass as before, with `||` short-circuiting; query trimmed once rather than per field. No structural/perf regression on the in-memory dataset.

## Deviations / decisions
- **Trim moved up front.** The original called `contains(searchQuery, …)` directly; the query is now `.trim()`'d once into `query` and reused. This fixes trailing-space misses and avoids re-trimming per field. Minor behavioural improvement beyond the literal AC.
- **Notes handled as a list.** `conversationNotes` is a `List<ContactNote>`, so it can't be matched with a plain `.contains`; `.any { it.text… }` matches if any single note contains the query.
- **Indexed/backend search left out of scope** per the ticket, with the in-memory/page-scoping limitation flagged in a code comment referencing the pagination dependency (#25) and the follow-up.

## Open questions / follow-ups
- **Indexed search backend (P2-6 / follow-up):** once pagination (#25) is active, in-memory search only sees the loaded page. The planned move is to an indexed/backend query — out of scope here, noted in-code.
- Verified via `./gradlew :app:compileDebugKotlin` (per the commit); on-device search behaviour should be spot-checked with the test steps above.
