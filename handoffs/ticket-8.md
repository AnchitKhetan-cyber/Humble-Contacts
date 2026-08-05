# Handoff — Ticket #8

**Ticket:** #8 — [P0][Bug] "Replace" on a duplicate contact silently destroys data

## Summary
Choosing **Replace** on a duplicate contact previously deleted the original document and called `addContact()`,
which minted a brand-new `contactId` and dropped everything not present on the freshly scanned object —
conversation notes, media, favourite status, tags — while breaking any QR/deep link pointing at the old id.
It was also non-atomic: if the insert failed after the delete, the contact was lost permanently. This change
reworks `replaceContact()` to **update the existing document in place**: it locates the duplicate, then runs a
single Firestore transaction that reads the existing document and writes a merged copy where the newly scanned
fields overwrite and `contactId`, `ownerId`, `createdAt`, `favourite`, `tags` and `media` are preserved. The
new conversation note is appended to the existing history, `updatedAt` is refreshed, and because there is one
transactional write with no delete step, a mid-way failure leaves the original contact intact.

## Files changed

### Data / repository
- **`data/repository/ContactRepository.kt`** — Rewrote `replaceContact(contact)`. Instead of
  delete-old-then-`addContact()`, it now: (1) locates the duplicate by email/phone (same lookup `addContact`
  uses), falling back to `addContact(contact)` if none is found; (2) uploads any newly scanned business card
  first, outside the transaction and keyed to the preserved id (Cloud Storage isn't transactional and the
  transaction body can be retried); (3) runs a `db.runTransaction` that reads the existing document via
  `transaction.get(ref).toObject(Contact::class.java)` and writes a merged `existing.copy(...)` — overwriting
  the scanned fields, appending the new note to `conversationNotes`, setting `updatedAt = Timestamp.now()`, and
  leaving `contactId`, `ownerId`, `createdAt`, `favourite`, `tags`, `media`, `industry`, `meetingLocation`,
  `eventName` untouched.

_No other files were changed. The ViewModel (`ContactViewModel.replaceContact`) and the Replace dialog in
`Addcontactscreen.kt` already build the `Contact` with the new note/image and call `repo.replaceContact`, so
they required no edits._

## How to test
Requires the Firebase test account and Firestore console access (obtain from the manager via secure channel;
never commit credentials).

1. Check out this branch and build: `./gradlew :app:compileDebugKotlin` (verified `BUILD SUCCESSFUL`). Run the
   app signed into the test account.
2. **Same id + attached data survives:**
   - Create a contact (e.g. Asha Rao, phone + email). Favourite it, add a conversation note, add a tag.
   - In the Firestore console, open the record and note: the document **ID**, `createdAt`, `favourite=true`,
     the note, and the tag.
   - In-app, add a new contact with the **same email or phone** but changed company/job role → the
     "Contact Already Exists" dialog appears → tap **Replace**.
   - Refresh the record: document **ID unchanged**, `createdAt` unchanged, `favourite`/note/tag/`media` all
     still present, and company/jobRole now show the new values, with `updatedAt` newer than `createdAt`.
3. **Note appended:** ensure the original had a note; type a new note during Replace; confirm both notes are in
   `conversationNotes` afterward.
4. **Business-card image:** replacing with a new card updates `businessCardImage` while the separate `media`
   array is preserved; replacing without a new image keeps the old `businessCardImage`.
5. **Atomicity (most important):** enable airplane mode, then tap Replace so the write fails. Confirm in
   Firestore that the **original document still exists, intact** (never deleted).

## Acceptance criteria
- [x] **Replacing a duplicate keeps the same `contactId`; existing QR/deep links still resolve.** The merged
  object is written to `db.collection("contacts").document(existingId)` and `copy()` never changes
  `contactId`.
- [x] **Notes, media, favourite, and tags on the original are all retained.** `merged = existing.copy(...)`
  starts from the existing document; those fields are not in the overwrite list, and `conversationNotes` is
  `existing.conversationNotes + contact.conversationNotes` (preserved plus appended).
- [x] **Newly scanned/entered fields overwrite the old values.** `fullName`, `jobRole`, `company`, `email`,
  `phone`, `linkedIn`, `address`, `meetingDate`, `entryMethod`, and `businessCardImage` (when a new card is
  uploaded) are set from the incoming contact.
- [x] **Atomic — a simulated mid-way failure leaves the original intact.** A single `transaction.set(ref,
  merged)` with no delete step; a failure aborts the transaction, leaving the existing document unchanged.
- [x] **`updatedAt` refreshed; `createdAt` unchanged.** `updatedAt = Timestamp.now()`; `createdAt` is not in
  the overwrite list, so `copy()` preserves it.

## Deviations / decisions
- **Read-merge-write via `runTransaction`** (rather than `SetOptions.merge()` with a field map). The ticket
  allowed transaction/batch/merge; a transaction was chosen so the new note can be appended to the existing
  notes (which requires reading the current value) and to satisfy the explicit "wrap in a transaction/batch"
  guidance.
- **New conversation note is appended**, not discarded — preserves prior history and the note typed during
  Replace.
- **A newly scanned business-card image overwrites `businessCardImage`**, while the separate `media` list is
  preserved. The image is uploaded before the transaction (Cloud Storage is not transactional and the
  transaction body may be retried), keyed to the preserved id so the storage path stays stable.
- **Fields not captured by the scan are preserved** (`industry`, `meetingLocation`, `eventName`) rather than
  blanked, since the incoming `Contact` carries defaults for them.
- **Fallback:** if no duplicate is actually found, the method falls back to `addContact(contact)` (a normal
  insert) instead of failing.

## Open questions / follow-ups
- Verification here covered compilation, a code-path trace against each acceptance criterion, and developer
  manual testing against the Firebase test account. No automated tests were added — repository-level tests for
  the merge/preserve behaviour (ideally against the Firestore emulator) would be a good follow-up.
- `ContactViewModel.replaceContact` ignores the `Boolean` returned by `repo.replaceContact` and always calls
  `onDone()`, so a failed replace still shows the "Contact replaced" toast. Surfacing success/failure to the
  user is a small UX follow-up, out of scope for this data-loss fix.
- Duplicate **detection** semantics (email/phone lookup, the sealed result type, dialog copy) remain out of
  scope per the ticket — see `P2-4`.
