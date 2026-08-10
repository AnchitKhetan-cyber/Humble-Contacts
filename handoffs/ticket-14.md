# Handoff — ticket #14

**Ticket:** #14 — [Security] Scanned URLs open with no confirmation (QR phishing)

## Summary
Scanning a QR whose payload was an `http(s)` URL previously launched the browser immediately, and unrecognised payloads were echoed verbatim in a Toast. This change gates web links behind a confirmation dialog and stops raw echoing. The `http(s)` branch in `onQrCodeScanned` now stores the URL in state and renders a Material3 `AlertDialog` (`QrLinkConfirmDialog`) that names the destination host prominently, with the full URL sanitised and truncated beneath; the browser opens only on confirm, and `startActivity` is wrapped so a malformed URI cannot crash the app. The `else` (unknown) branch now shows a fixed "couldn't read this QR" string instead of the raw payload. All new user-facing text lives in `strings.xml`, and the dialog is theme-aware with a content-described warning icon. Only the `http(s)` and `else` branches were touched — the vCard/JSON/`tel:`/`mailto:`/custom-scheme branches are unchanged.

## Files changed

**Navigation / QR handling**
- `app/src/main/java/com/humblesolutions/humblecontacts/navigation/AppNavGraph.kt` — hoisted `pendingUrl` state in the HOME composable; `http(s)` branch sets `pendingUrl` instead of launching; `else` branch shows `R.string.qr_unreadable` instead of raw text; renders `QrLinkConfirmDialog` when a URL is pending (confirm → `ACTION_VIEW` wrapped in `runCatching`; cancel/dismiss → clear state). Added private `QrLinkConfirmDialog` composable, `hostForDisplay()` (host extraction with fallback), and `sanitizeForDisplay()` (strips ISO control chars, caps length at 120 + ellipsis). Removed the now-unused `android.util.Log` import.

**Strings**
- `app/src/main/res/values/strings.xml` — added `qr_open_link_title`, `qr_open_link_prompt`, `qr_open_link_confirm`, `qr_open_link_cancel`, `qr_open_link_icon_desc`, `qr_open_link_unknown_host`, and `qr_unreadable` for the dialog and the unknown-payload message.

## How to test
Requires a device/emulator and a QR generator.

1. **Confirmation appears:** encode `https://example.com/login?x=1`, scan it → a dialog appears naming host **example.com**. (Repeat with `http://example.com` — plain http also triggers it.)
2. **Cancel opens nothing:** on the dialog tap **Cancel** → no browser, stays in app.
3. **Confirm opens URL:** rescan, tap **Open link** → browser opens the URL.
4. **Look-alike is visible:** encode `https://paypa1.com` → the bold host line makes the impostor domain readable.
5. **Long URL doesn't overflow:** encode a 200+ char URL → host stays one line, full URL truncates with `…`.
6. **Unknown payload sanitised:** encode plain junk text (and a payload with newlines/control chars) → fixed "Couldn't read this QR code." message, never the raw text.
7. **Theme + a11y:** toggle dark mode (dialog readable in both); enable TalkBack (warning icon announces "Warning: external link", host is read aloud).
8. **Regression (untouched branches):** `humblecontacts://contact/<id>` opens the contact; `BEGIN:VCARD…` → add-contact; `{"contactId":"…"}` → contact; `tel:`/`mailto:` open directly (no dialog — expected, only `http(s)` is gated).

## Acceptance criteria
- [x] **Scanning an `http(s)` QR shows a confirmation dialog naming the host before any browser opens** — `http(s)` branch sets `pendingUrl`; `QrLinkConfirmDialog` shows `hostForDisplay(url)` prominently.
- [x] **Cancelling opens nothing; confirming opens the URL** — `onDismiss` clears state with no launch; `onConfirm` calls `startActivity(ACTION_VIEW)`.
- [x] **Unknown/`else` payloads sanitised + truncated, never echoed raw** — `else` shows `R.string.qr_unreadable`; `sanitizeForDisplay()` strips control chars and caps length for the URL preview.
- [x] **Dialog works in light/dark, strings in `strings.xml`, screen-reader labelled** — Material3 `AlertDialog` uses `colorScheme` colours; all text in `strings.xml`; warning icon has `contentDescription`.

## Deviations / decisions
- **Strings externalised to `strings.xml`** per AC #4, even though the rest of the app hardcodes strings inline (there were zero prior `stringResource` usages). Chosen deliberately to satisfy the AC; flagged as a divergence from the surrounding code convention.
- **`UI-STANDARDS.md` is not present in the repo** (referenced by the ticket) and no design was attached. The dialog follows the app's existing Material3 `AlertDialog` pattern (e.g. `DeleteAccountScreen`), which is light/dark-correct by construction.
- **Host display normalisation:** the host is lower-cased and a leading `www.` is stripped for clarity; unparseable URLs fall back to `qr_open_link_unknown_host` ("an unknown site").
- **Crash-safety:** `startActivity` on confirm is wrapped in `runCatching` so a malformed/`no-activity` URI shows the fixed message rather than crashing (beyond the literal AC, but in the spirit of hardening).

## Open questions / follow-ups
- Verified via `assembleDebug`; the visual light/dark render and TalkBack behaviour should be confirmed on-device (developer reports manual testing passed).
- URL reputation / safe-browsing lookups were explicitly out of scope — a possible future enhancement.
- Both changed files end without a trailing newline (pre-existing style in `strings.xml`); harmless, noted for tidiness.
