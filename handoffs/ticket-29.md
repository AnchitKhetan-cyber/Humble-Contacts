# Handoff — ticket #29

**Ticket:** #29 — [P2][Enhancement] Search covers only three fields

## Summary
Contact search previously matched only `fullName`, `company`, and `jobRole`, leaving email, phone, address, event name, and conversation notes unsearchable — even though notes usually hold the most distinctive detail about a person. This change widens the search predicate to also match `email`, `phone`, `address`, `eventName`, and any conversation note's text. Matching stays case-insensitive and partial (`contains(..., ignoreCase = true)`), and the query is trimmed. No UI change was needed — `ContactsScreen` already feeds its search box through this predicate. The predicate was extracted into a pure, unit-testable `contactMatchesQuery()` function and covered by `ContactSearchTest` (12 tests). The scan remains in-memory over loaded contacts; a code comment records that this is page-scoped once pagination (#25) lands, with an indexed search backend as the planned follow-up.

## Files changed

**Contacts / search logic**
- `app/src/main/java/com/humblesolutions/humblecontacts/ui/contacts/ContactSearch.kt` **(new)** — pure top-level `contactMatchesQuery(contact, query)` predicate: trims the query, then matches `fullName`, `company`, `jobRole`, `email`, `phone`, `address`, `eventName`, and any conversation note's text, all case-insensitive; blank query matches everything. Doc comment records the in-memory/page-scoped nature and the indexed-backend follow-up (P2-6). Extracted so the logic is testable without instantiating the `AndroidViewModel`, mirroring the existing `addTag()` pattern in `TagInput.kt`.
- `app/src/main/java/com/humblesolutions/humblecontacts/ui/contacts/ContactViewModel.kt` — `filtered()` now delegates its search check to `contactMatchesQuery(contact, searchQuery)` instead of an inline predicate. The filter-chip logic (`matchesFilter`) is untouched.

**Tests**
- `app/src/test/java/com/humblesolutions/humblecontacts/ui/contacts/ContactSearchTest.kt` **(new)** — 12 JUnit tests over `contactMatchesQuery`: each new field (email/phone/address/event/notes), notes matched in any list position, original fields, case-insensitivity, partial match, trimming, blank-query-matches-all, no-match, and empty-notes safety. Plain JVM tests (no Robolectric), matching the repo's existing test style.

## How to test
**Automated:** `./gradlew :app:testDebugUnitTest --tests "*ContactSearchTest"` — 12 tests, all green; covers every acceptance criterion below.

**Manual (optional):** no new UI; verify through the contacts search box on a device/emulator with a few contacts that have data in the new fields.

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
- **Query is trimmed.** The original called `contains(searchQuery, …)` directly; the query is now trimmed inside `contactMatchesQuery`. This fixes trailing-space misses. Minor behavioural improvement beyond the literal AC.
- **Predicate extracted to a pure function.** The search logic lived inside `ContactViewModel` (an `AndroidViewModel` wired to Firebase), which can't be instantiated in a plain JVM unit test without Robolectric — the style this repo deliberately avoids. Extracting `contactMatchesQuery()` makes the AC directly testable and mirrors the existing `addTag()` pattern. No behaviour change to `filtered()`.
- **Notes handled as a list.** `conversationNotes` is a `List<ContactNote>`, so it can't be matched with a plain `.contains`; `.any { it.text… }` matches if any single note contains the query.
- **Indexed/backend search left out of scope** per the ticket, with the in-memory/page-scoping limitation flagged in a code comment referencing the pagination dependency (#25) and the follow-up.

## Open questions / follow-ups
- **Indexed search backend (P2-6 / follow-up):** once pagination (#25) is active, in-memory search only sees the loaded page. The planned move is to an indexed/backend query — out of scope here, noted in-code.
- Verified via `./gradlew :app:testDebugUnitTest` (12 new tests pass, full suite green); on-device search behaviour can be spot-checked with the manual steps above.
