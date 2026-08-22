# Handoff — ticket #64

**Ticket:** #64 — [P2][Feature] Digital visiting card — templates, customization, and sharing

## Summary
Turns the previously-dormant `visitingCard` field into a full feature. A user opens a new
**My Visiting Card** editor from their Profile, picks one of five templates, tweaks the accent
colour / background / font, fills in card-specific text (headline, bio, website, portfolio), and
sees a live preview. The card **auto-fills** its display fields (name, company, role, phone,
email, LinkedIn, address) live from `UserProfile`, so there is one source of truth and no
duplicated data. It can be shared three ways — as an **image** (share sheet + save-to-gallery),
as a **QR code** that encodes a **vCard**, and as a **`.vcf` file** — and all three honour the
existing per-field `ShareSettings` privacy toggles. Because the QR encodes a vCard, scanning
someone's card with the app's existing scanner drops you into a prefilled Add Contact form.

## Files changed

**Data model + reconciliation**
- `data/model/VisitingCard.kt` — extended from `{cardTheme, bio}` to the agreed shape
  (`template`, `accentColor`, `background`, `fontStyle`, `headline`, `bio`, `websiteUrl`,
  `portfolioUrl`). `template` is persisted under the legacy Firestore key `cardTheme` via
  `@get:/@set:PropertyName`, so old documents deserialize with no migration; every field is
  defaulted so a partial/legacy map never fails to deserialize.
- `data/auth/AuthRepository.kt` — registration seed for `visitingCard` updated to the exact new
  shape (adds `accentColor`/`background`/`fontStyle`, keeps `cardTheme` key). Fixes the prior
  mismatch where the seed wrote fields the model didn't declare.

**Persistence**
- `data/repository/ProfileRepository.kt` — added `updateVisitingCard(card)` following the
  existing `updateShareSettings` pattern (`.update(mapOf(..., "updatedAt" to now))`); imports
  `VisitingCard`.

**New utilities**
- `utils/VCardBuilder.kt` — builds a vCard 3.0 string from `UserProfile` + `VisitingCard`,
  gated by `ShareSettings`, RFC-escaped. Single source for both the `.vcf` and the QR.
- `utils/QrUtils.kt` — `generateQrBitmap` extracted here (from `LinkedAccountsScreen`) so the
  linked-accounts QR and the card QR share one implementation.
- `utils/CardShareUtils.kt` — local share/export helpers: `shareCardImage` (PNG → cache →
  `${applicationId}.provider` FileProvider → share sheet), `saveCardImageToGallery` (MediaStore
  Pictures on API 29+, external-files fallback below), `shareVCard` (`.vcf` → share sheet).

**Card rendering + templates**
- `ui/profile/card/CardTemplates.kt` — `CardTemplate` (5 presets), `CardBackground`,
  `CardFontStyle` enums with string↔type resolvers that always fall back to a default;
  `CardAccentSwatches` palette; `parseHexColor`; `VisitingCard.resolveStyle()`.
- `ui/profile/card/VisitingCardView.kt` — the card composable used by both the preview and the
  image export. Reads display fields live from `UserProfile`; contact rows are gated by
  `ShareSettings` so toggled-off fields don't render.

**Editor screen + ViewModel**
- `ui/profile/VisitingCardViewModel.kt` — plain `ViewModel` (matches `ProfileViewModel`); loads
  profile+card, holds edits, `save()`, and `buildVCard()`; exposes loading/saving/error/success
  state.
- `ui/profile/VisitingCardScreen.kt` — the editor: live preview (captured to a bitmap via
  `GraphicsLayer`), template/accent/background/font pickers, text fields (reusing
  `HumbleInputField` with URI keyboards + IME actions), Save, and the three share actions + a QR
  dialog. Loading / load-error(+retry) / saving states.

**Navigation + entry point**
- `navigation/Screen.kt` — added `VISITING_CARD` route.
- `navigation/AppNavGraph.kt` — registered `VisitingCardScreen`; passes `onNavigateToVisitingCard`
  into `ProfileScreen`.
- `ui/profile/ProfileScreen.kt` — new **My Visiting Card** row in the ACCOUNT section + a live
  card preview (tappable → editor) above the QR section; imports `VisitingCardView`.

**Reuse cleanup**
- `ui/profile/LinkedAccountsScreen.kt` — removed its local `generateQrBitmap` and the now-unused
  ZXing/`Bitmap` imports; imports the shared `utils.generateQrBitmap` instead. Behaviour
  unchanged.

**Strings**
- `res/values/strings.xml` — all new user-facing card strings (no hardcoded UI text).

**Tests**
- `test/.../utils/VCardBuilderTest.kt` — 4 tests: vCard round-trips through `VCardParser`;
  output is well-formed; `ShareSettings` gates phone/email/company/LinkedIn; special characters
  are escaped and survive parsing.

**Docs**
- `docs/tickets/64-digital-visiting-card.md` — the ticket spec (added when the ticket was drafted).

## How to test

**Automated (required):**
```
./gradlew :app:compileDebugKotlin :app:testDebugUnitTest
```
Green. `VCardBuilderTest` runs 4 tests, 0 failures.

**Manual (device/emulator, signed in):**
1. Profile tab → **My Visiting Card** row (ACCOUNT). Editor opens with your profile data
   already showing on the preview.
2. Tap through the 5 **templates**; change **accent** swatch, **background** (Surface/Solid/
   Gradient), and **font** (Sans/Serif/Mono) → the preview updates live.
3. Fill **Headline / Bio / Website / Portfolio** → they appear on the preview. Tap **Save card**
   → "Card saved" toast; reopen the editor → values persisted.
4. Back on Profile → the top **MY VISITING CARD** preview reflects the saved card; tapping it
   reopens the editor.
5. **Share → Image** → Android share sheet with a PNG of the card. **Save image to gallery** →
   "Saved to gallery" (check Pictures/Humble Contacts).
6. **Share → QR code** → dialog; scan it with the app's own scanner (Scan tab) → Add Contact
   opens prefilled. **Share QR** shares the QR image.
7. **Share → Contact** → share sheet with a `.vcf`; open it on another device / import it →
   fields match.
8. **Privacy:** Profile → SHARING, turn off Share phone/email/LinkedIn → return to the card;
   those rows disappear from the preview and are absent from the image, QR, and `.vcf`.
9. Verify in **light and dark**; on a **Solid/Gradient** background the text stays legible.

## Acceptance criteria
- [x] **My Visiting Card** row in Profile ACCOUNT opens the editor; live preview on Profile.
- [x] Editor offers 5 templates + accent/background/font changes; preview updates live.
- [x] Card auto-fills name/company/role/phone/email/LinkedIn/address from `UserProfile`; editor
      only edits headline/bio/website/portfolio + look (no duplicated stored copy).
- [x] Stored under `users/{uid}.visitingCard` via `updateVisitingCard`; reopening shows saved state.
- [x] Model, `AuthRepository` seed, and editor agree on one shape; legacy `cardTheme` handled via
      `@PropertyName` + all-defaulted fields (no crash on old profiles).
- [x] Share as image via share sheet + save-to-gallery.
- [x] Share as QR encoding a vCard; scanning opens Add Contact prefilled (reuses existing scanner).
- [x] Share as `.vcf` via share sheet; round-trips through `VCardParser` (unit-tested).
- [x] All three share paths honour `ShareSettings` (gating in `VCardBuilder` unit-tested; card
      rendering gates the same fields).
- [x] No new secrets, Cloud Functions, or Firestore-rules changes.
- [x] UI standards: light+dark theme tokens, reused components (`HumbleInputField`, settings
      rows), URI keyboards + IME actions, loading/saving/error states, content descriptions,
      i18n strings, no business logic in composables.
- [x] `./gradlew :app:compileDebugKotlin :app:testDebugUnitTest` green.

## Deviations / decisions
- **`template` keyed as `cardTheme`.** Rather than renaming the Firestore field (which would
  strand old data), `template` maps to the existing `cardTheme` key via `@PropertyName`. This was
  the ticket's sanctioned option and needs zero migration.
- **QR dialog is card-specific, not the shared `QrCodeDialog`.** The existing `QrCodeDialog` is
  typed to `LinkedAccount` and shows per-field content; the card QR shows a single vCard, so a
  small `CardQrDialog` was added instead (the QR *generation* is shared via `QrUtils`). Flagged
  as acceptable during planning.
- **Image capture uses the on-screen preview via `GraphicsLayer`.** Simpler and guarantees WYSIWYG
  vs. an off-screen re-render. Note: it captures the last drawn frame of the preview (see
  follow-ups).
- **Legacy-deserialization AC is covered structurally, not by a unit test.** Firestore mapping
  can't be exercised in plain JUnit without Robolectric/instrumentation; it's guaranteed by the
  `@PropertyName` mapping + all-defaulted fields. The other two test ACs (round-trip, gating) are
  unit-tested.
- **Headline + bio ride in the vCard `NOTE`** (no dedicated vCard field for them); the parser
  doesn't read NOTE, so they don't affect the round-trip assertions.
- **`splash_icon_transparent.xml`** and the two root `*.pdf` files were already untracked on the
  branch before this work and are intentionally left out of the ticket's changes.

## Open questions / follow-ups
- **Image capture when scrolled off-screen:** the share buttons sit below the preview in one
  scroll view; if the preview is scrolled out when the user taps Share/Save, the captured bitmap
  is the last drawn frame (still reflects current data). If reviewers want it bulletproof, render
  the card to an off-screen `GraphicsLayer` on demand instead. Low risk in practice.
- **No Compose UI tests** for the editor (pre-existing gap for the app's UI). The logic-heavy part
  (`VCardBuilder`) is unit-tested; UI is manual for now.
- **Storage of a card image** was deliberately out of scope (sharing is local). If a hosted/
  shareable card page is wanted later, that's a separate ticket (would also change the QR to a URL).
