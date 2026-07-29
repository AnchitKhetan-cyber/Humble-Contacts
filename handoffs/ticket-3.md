# Handoff — Ticket #3

**Ticket:** #3 — [P0][Bug] Business-card parsing always fails — invalid Gemini model name

## Summary
`BusinessCardParser` constructed its `GenerativeModel` with `modelName = "gemini-3.1-flash-lite"`, which the `generativeai:0.9.0` client does not serve — so every business-card parse failed at runtime and the "scan a card → auto-fill contact" flow was broken end to end. The fix changes the model name to the manager-approved `gemini-2.0-flash` (supported by the 0.9.0 client, fast and cheap). No other code changed: the prompt, the ```json-fence stripping, and the `ContactInfo` deserialization (`ignoreUnknownKeys = true`) were already correct and only ever failed because the request was rejected before a response came back.

## Files changed
**Business-card parsing**
- `app/src/main/java/com/humblesolutions/humblecontacts/ui/contacts/BusinessCardParser.kt` — `modelName` changed from `"gemini-3.1-flash-lite"` to `"gemini-2.0-flash"` (with an explanatory comment).

## How to test
1. Put a valid key in `local.properties` (gitignored, never committed): `GEMINI_API_KEY=<key>`.
2. Build/install: `./gradlew :app:installDebug` (or run from Android Studio). `compileDebugKotlin` passes.
3. Happy path: scan/pick a clear business-card image → confirm the Add Contact screen pre-fills name/company/phone/email/etc.
4. Error path: scan a blank/garbage image → confirm the existing friendly `GeminiError` message shows (e.g. "No contact details could be extracted") and the app does not crash.
5. Watch logcat tag `GEMINI_JSON` to see the raw model response and `PARSED_CONTACT` for the deserialized result.

## Acceptance criteria
- [~] **Scanning a clear card returns fields and pre-fills Add Contact.** Fix is in place and compiles; **not runtime-verified here** because no `GEMINI_API_KEY` is present in `local.properties` on the build machine. Needs a live scan with a valid key.
- [~] **No "model not found"/invalid-argument error on a valid key.** The invalid model name is replaced with the supported `gemini-2.0-flash`; requires a key to confirm at runtime.
- [x] **Blank/garbage image shows the friendly message, no crash.** Unchanged error handling: `parse()` propagates exceptions and `ContactViewModel.parseBusinessCard()` surfaces them via `GeminiError.getMessage(e)` (no code in that path was modified).
- [x] **No API key or secret committed.** The diff only changes a model-name string; `GEMINI_API_KEY` continues to come from gitignored `local.properties` via the `buildConfigField`.

## Deviations / decisions
- **Fallback not needed (yet):** the ticket says fall back to `gemini-1.5-flash` if `gemini-2.0-flash` is rejected at runtime by the 0.9.0 client. That path was **not** exercised (no key to test with), so `gemini-2.0-flash` stands as the chosen model; if a reviewer's live test rejects it, switch to `gemini-1.5-flash` per the ticket.
- **Left the no-op `try { … } catch (e) { throw e }`** in `parse()` untouched — the ticket lists removing it as an optional nice-to-have, and keeping it holds the diff to the single meaningful change.

## Open questions / follow-ups
- **Live verification pending:** the happy path must be confirmed on a device with a valid `GEMINI_API_KEY` before this is truly closed — that's the whole point of the P0.
- **Out of scope (separate tickets):** migrating off the deprecated `generativeai:0.9.0` SDK; prompt-engineering improvements.
