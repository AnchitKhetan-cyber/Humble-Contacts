# Handoff — Ticket #7

**Ticket:** #7 — [P0][Bug] Scanning a vCard QR code crashes the app (route not registered)

## Summary
Scanning a standard vCard QR (`BEGIN:VCARD…END:VCARD`) crashed the app with
`IllegalArgumentException: navigation destination … not found`, because the QR handler navigated to
`addContact?vcard=$encoded` — a route that was never registered. This change registers the add-contact
composable under `add_contact?vcard={vcard}` with a nullable `vcard` navArgument (default `null`), so a
scanned vCard can be passed through and plain `add_contact` navigation still resolves to the same screen with
a blank form. A new `VCardParser` does a best-effort parse of the raw vCard into the existing `ContactInfo`
shape, and `AddContactScreen` seeds its prefill state from it, reusing the business-card OCR prefill path. All
parsing is wrapped so any malformed/partial vCard yields a blank form instead of crashing. The QR handler now
builds the route via a `Routes.addContactWithVCard(raw)` helper that URL-encodes the payload.

## Files changed

### Navigation
- **`navigation/Screen.kt`** — Adds `ADD_CONTACT_WITH_ARGS = "add_contact?vcard={vcard}"` and the
  `addContactWithVCard(rawVCard)` helper (URL-encodes the raw vCard so newlines/colons/`+` don't corrupt the
  route). Adds the `android.net.Uri` import for encoding. Existing `ADD_CONTACT` constant is left intact.
- **`navigation/AppNavGraph.kt`** — Registers the add-contact composable under `ADD_CONTACT_WITH_ARGS` with a
  nullable `vcard` `navArgument` (`defaultValue = null`), reads the arg from the back stack entry, and passes
  it into `AddContactScreen`. The vCard QR branch now navigates via `Routes.addContactWithVCard(raw)` instead
  of the unregistered `addContact?vcard=` string. The deeplink (`humblecontacts://contact/`), JSON (`{`), and
  `tel:` branches are unchanged.

### Add Contact UI
- **`ui/contacts/Addcontactscreen.kt`** — `AddContactScreen` gains a `vcard: String? = null` parameter and
  seeds `extractedContact` from `VCardParser.parse(vcard)` when a vCard is present (otherwise an empty
  `ContactInfo`). The existing `LaunchedEffect(extractedContact)` copies those fields into the visible form,
  so vCard prefill rides the same path as business-card OCR.
- **`ui/contacts/VCardParser.kt`** (new) — Best-effort parser producing a `ContactInfo`. Handles RFC 6350 line
  unfolding, value unescaping, structured `N` names, `ORG`/`TITLE`, first `EMAIL`, `ADR` formatting, `TEL`
  with mobile/cell preference over other numbers, and LinkedIn username extraction from `URL`/
  `X-SOCIALPROFILE`. `parse()` wraps everything in try/catch and returns an empty `ContactInfo` on any
  failure (logs a warning under tag `VCardParser`).

## How to test
1. Check out this branch and build: `./gradlew :app:compileDebugKotlin` (verified `BUILD SUCCESSFUL`). Install
   the debug app on a device/emulator.
2. **Standard vCard:** Generate a QR from a full vCard (FN/N, ORG, TITLE, TEL;TYPE=CELL, EMAIL, ADR, and a
   `linkedin.com/in/<user>` URL) and scan it from the app. Add Contact should open **prefilled** — name, job
   role, company (first `ORG` component), mobile phone preferred, email, address, LinkedIn username — with no
   crash.
3. **Normal add:** Open Add Contact via the usual add button (no scan). Form should be blank, no crash.
4. **Malformed/partial vCard:** Scan a QR of a broken vCard (missing colons, empty values, `BEGIN:VCARD` /
   `END:VCARD` only). The form should open (best-effort/blank) without crashing; a warning may appear in
   logcat under `VCardParser`.
5. **Other QR types (regression):** Scan a `humblecontacts://contact/<id>` deeplink, a `{"contactId":"…"}`
   JSON QR, and a `tel:` QR. Each should behave as before with no
   `IllegalArgumentException: navigation destination … not found`.

## Acceptance criteria
- [x] **Scanning a standard vCard QR opens Add Contact prefilled — no crash.** Route
  `add_contact?vcard={vcard}` is registered; `VCardParser.parse` → `ContactInfo` → existing prefill effect.
- [x] **Navigating to Add Contact the normal way still works (arg defaults null, blank form).** Nullable
  `vcard` navArgument with `defaultValue = null`; plain `add_contact` matches the same route.
- [x] **A malformed/partial vCard opens the form without crashing (best-effort prefill).** `VCardParser.parse`
  try/catch returns an empty `ContactInfo` on any failure.
- [x] **No `IllegalArgumentException: navigation destination … not found` on any QR type.** vCard branch now
  targets the registered route; deeplink/JSON/tel branches unchanged.

## Deviations / decisions
- Followed the manager-confirmed approach in the ticket exactly (register a real optional `vcard` arg and
  prefill). No deviations.
- Prefill reuses the existing business-card OCR path (`extractedContact` +
  `LaunchedEffect(extractedContact)`) rather than adding a separate prefill mechanism.
- Route building is centralized in a `Routes.addContactWithVCard` helper so encoding lives next to the route
  pattern.
- An unrelated untracked file (`app/src/main/res/drawable/splash_icon_transparent.xml`) was intentionally left
  out of this branch to keep it scoped to ticket #7.

## Open questions / follow-ups
- End-to-end verification here covered compilation and code-path review plus developer manual testing;
  automated unit tests for `VCardParser` (structured names, phone preference, escaping, malformed input) are
  not included and could be a good follow-up.
- `VCardParser` currently keys LinkedIn detection off the substring `linkedin`; other social profiles are not
  captured. Out of scope for this ticket but worth noting.
- Confirmation dialog for scanned URLs and a dedicated ScanScreen remain out of scope (see `SEC-4`, `P1-6`).
