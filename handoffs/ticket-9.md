# Handoff — Ticket #9

**Ticket:** #9 — [P0][Bug] Firestore snapshot listener leak (duplicate callbacks, growing reads)

## Summary
`getContactsRealtime()` previously attached a Firestore snapshot listener and discarded the
`ListenerRegistration` it returned, so the listener could never be detached. `ContactViewModel`
attached one on `init` and attached a *second* on every `refreshContacts()`, with no removal of the
prior one and no cleanup on teardown — stacking live listeners, causing duplicate callbacks, unbounded
memory growth, and billable Firestore reads. This change makes `getContactsRealtime()` **return** the
`ListenerRegistration`; the ViewModel now holds a single registration, detaches it before every
re-registration, and removes it in `onCleared()`. The result is exactly one active listener regardless
of how many refreshes occur, and reads stop when the screen is left. The realtime callback logic is
unchanged, so live add/edit/delete updates still work.

## Files changed
**Data layer**
- `app/src/main/java/com/humblesolutions/humblecontacts/data/repository/ContactRepository.kt` —
  `getContactsRealtime()` now returns `ListenerRegistration` (was `Unit`) and `return`s the
  `addSnapshotListener` result so callers can detach it; added the `ListenerRegistration` import and a
  KDoc explaining callers must hold and `remove()` the registration.

**UI / ViewModel**
- `app/src/main/java/com/humblesolutions/humblecontacts/ui/contacts/ContactViewModel.kt` —
  added a `contactsListener: ListenerRegistration?` field to hold the single active listener; extracted
  `registerContactsListener()` which calls `contactsListener?.remove()` before attaching a new one;
  routed both `init` and `refreshContacts()` through it; added an `onCleared()` override that removes
  and nulls the listener. Added the `ListenerRegistration` import.

## How to test
1. Check out `ticket-9-listener-leak`.
2. Build: `./gradlew compileDebugKotlin` (or `./gradlew assembleDebug`) — expect **BUILD SUCCESSFUL**.
3. Run the app on a device/emulator signed in to a Firebase test account.
4. Open the contacts screen and trigger `refreshContacts()` (pull-to-refresh / refresh action) several
   times. Watch Logcat for the `CONTACT_DEBUG` tag: each Firestore change should log
   `Contacts found = N` **once**, not multiplied by the number of refreshes — confirming a single
   listener.
5. From another client (or the Firebase console) add/edit/delete a contact and confirm the list updates
   live exactly once per change.
6. Navigate away from the contacts screen so the ViewModel is cleared; confirm no further
   `CONTACT_DEBUG` reads fire for that listener (Firestore reads stop).

## Acceptance criteria
- [x] **After multiple `refreshContacts()` calls, exactly one snapshot listener is active** — met.
  `registerContactsListener()` calls `contactsListener?.remove()` before each attach; both entry points
  (`init`, `refreshContacts()`) go through it.
- [x] **No duplicate emissions of the contacts list on a single Firestore change** — met. Only one
  listener remains attached, so the callback fires once per change.
- [x] **The listener is removed in `onCleared()`** — met. `onCleared()` calls `contactsListener?.remove()`
  and nulls the field, stopping reads when the screen is left.
- [x] **Realtime updates still work (add/edit/delete reflects live)** — met. The `addSnapshotListener`
  callback body is unchanged; only the registration is now returned and managed.

## Deviations / decisions
- Followed the ticket's confirmed decision (return the `ListenerRegistration`, hold it, `remove()` before
  re-registering, clear in `onCleared()`) rather than the `callbackFlow` alternative. No behavioural
  deviation from the ticket.
- The re-registration logic was extracted into a private `registerContactsListener()` helper so the
  detach-before-attach discipline lives in one place shared by `init` and `refreshContacts()`.
- An unrelated untracked file (`app/src/main/res/drawable/splash_icon_transparent.xml`) present in the
  working tree was intentionally left out of this ticket's commit.

## Open questions / follow-ups
- Verification here is by code inspection + Logcat; there is no unit-test harness for Firestore listener
  lifecycles in the repo, so no automated test was added.
- Per the ticket's Dependencies note: if `P2-2` (pagination) lands later and changes the query, keep the
  single-listener discipline in the new query path.
