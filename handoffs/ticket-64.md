# Handoff — ticket #64

**Ticket:** #64 — [P2][Feature] Digital visiting card — templates & customization

## Summary
Turns the previously-dormant `visitingCard` field into a full feature. A user opens a new
**My Visiting Card** editor from their Profile, picks one of **ten** templates — each a genuinely
distinct layout (Standard, Executive, Header-band, Split-panel, Centered, Framed, Monogram,
Bold-type, Side-rail, Mono-tech) shown as a **live thumbnail** in the picker — tweaks the accent
colour / background / font, fills in card-specific text (headline, bio, website, portfolio), and
sees a live preview that **crossfades** on template change. The card **auto-fills** its display
fields (name, company, role, phone, email, LinkedIn, address) live from `UserProfile`, so there
is one source of truth and no duplicated data. The saved card is shown on the Profile screen.

**No sharing.** Per the product owner, the card is create / customize / preview / save only —
there is deliberately **no** image / QR / vCard export, and no scan-to-Media flow. (Earlier
revisions built those; they were removed at the owner's request. The pre-existing vCard-QR
*contact* scanner and the linked-accounts QR codes are unaffected — they are not card sharing.)

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


**Card rendering + templates**
- `ui/profile/card/CardTemplates.kt` — `CardLayout` (**10 distinct layout archetypes**),
  `CardTemplate` (**10 presets**, each bound to its own layout + default accent/background/font),
  `CardBackground`, `CardFontStyle` enums with string↔type resolvers that always fall back to a
  default; `CardAccentSwatches` (10-swatch palette); `parseHexColor`;
  `VisitingCard.resolveStyle()`.
- `ui/profile/card/VisitingCardView.kt` — the card composable used by the editor preview and the
  Profile preview. Dispatches on the template's `CardLayout` to render ten visually distinct
  layouts (Standard, Executive, Header-band, Split-panel, Centered, Framed, Monogram, Bold-type,
  Side-rail, Mono-tech) from shared privacy-gated building blocks (`gatedContacts`, `Monogram`,
  `ContactList`, palette/surfacing helpers). Supports a `compact` mode for the picker thumbnails.
  Reads display fields live from `UserProfile`; contact rows gated by `ShareSettings`.

**Editor screen + ViewModel**
- `ui/profile/VisitingCardViewModel.kt` — plain `ViewModel` (matches `ProfileViewModel`); loads
  profile+card, holds edits, `save()`; exposes loading/saving/error/success state.
- `ui/profile/VisitingCardScreen.kt` — the editor: live preview (crossfades on template change),
  a horizontally-scrolling row of live template thumbnails, accent/background/font pickers, text
  fields (reusing `HumbleInputField` with URI keyboards + IME actions), and Save. Loading /
  load-error(+retry) / saving states. **No share actions.**

**Navigation + entry point**
- `navigation/Screen.kt` — added `VISITING_CARD` route.
- `navigation/AppNavGraph.kt` — registered `VisitingCardScreen`; passes `onNavigateToVisitingCard`
  into `ProfileScreen`.
- `ui/profile/ProfileScreen.kt` — new **My Visiting Card** row in the ACCOUNT section + a live
  card preview (tappable → editor) with an **Edit card** button; imports `VisitingCardView`.
- `ui/home/HomeScreen.kt` — a **My Visiting Card** showcase section (live card preview + Edit,
  tappable → editor) between the stats row and Recent Contacts, so the home screen promotes the
  card instead of sitting empty. Loads the profile via `ProfileRepository` on resume; takes a new
  `onNavigateToVisitingCard` callback wired in `AppNavGraph`.

**Strings**
- `res/values/strings.xml` — new user-facing card strings (no hardcoded UI text).

**Docs**
- `docs/tickets/64-digital-visiting-card.md` — the ticket spec (added when the ticket was drafted).

## How to test

**Automated (required):**
```
./gradlew :app:compileDebugKotlin :app:testDebugUnitTest
```
Green.

**Manual (device/emulator, signed in):**
1. Profile tab → **My Visiting Card** row (ACCOUNT). Editor opens with your profile data
   already showing on the preview.
2. Tap through the **10 template thumbnails** (each a live mini-card) → the big preview
   crossfades. Change **accent** swatch, **background** (Surface/Solid/Gradient), and **font**
   (Sans/Serif/Mono) → the preview updates live.
3. Fill **Headline / Bio / Website / Portfolio** → they appear on the preview. Tap **Save card**
   → "Card saved" toast; reopen the editor → values persisted.
4. Back on Profile → the **MY VISITING CARD** preview reflects the saved card; tapping it (or the
   **Edit card** button) reopens the editor.
5. Confirm there is **no** share/QR/export control anywhere on the card or Profile.
6. **Privacy:** Profile → SHARING, turn off Share phone/email/LinkedIn → return to the card;
   those rows disappear from the preview.
7. Verify in **light and dark**; on a **Solid/Gradient** background the text stays legible.

## Acceptance criteria
- [x] **My Visiting Card** row in Profile ACCOUNT opens the editor; live preview on Profile.
- [x] Editor offers 10 templates, each a distinct layout, shown as live thumbnails +
      accent/background/font changes; preview updates live and crossfades on template change.
- [x] Card auto-fills name/company/role/phone/email/LinkedIn/address from `UserProfile`; editor
      only edits headline/bio/website/portfolio + look (no duplicated stored copy).
- [x] Stored under `users/{uid}.visitingCard` via `updateVisitingCard`; reopening shows saved state.
- [x] Model, `AuthRepository` seed, and editor agree on one shape; legacy `cardTheme` handled via
      `@PropertyName` + all-defaulted fields (no crash on old profiles).
- [x] **No card sharing** — no image / QR / vCard export, no scan-to-Media (removed per owner).
- [x] Contact rows honour `ShareSettings` in the card rendering.
- [x] No new secrets, Cloud Functions, or Firestore-rules changes.
- [x] UI standards: light+dark theme tokens, reused components (`HumbleInputField`, settings
      rows), URI keyboards + IME actions, loading/saving/error states, content descriptions,
      i18n strings, no business logic in composables.
- [x] `./gradlew :app:compileDebugKotlin :app:testDebugUnitTest` green.

## Deviations / decisions
- **`template` keyed as `cardTheme`.** Rather than renaming the Firestore field (which would
  strand old data), `template` maps to the existing `cardTheme` key via `@PropertyName`. This was
  the ticket's sanctioned option and needs zero migration.
- **No sharing (owner decision).** The ticket originally scoped image/QR/vCard export and a
  scan-to-Media flow, all of which were built and then **removed at the product owner's request**
  ("no sharing of the visiting card"). Deleted with them: `CardShareUtils`, `VCardBuilder`,
  `HumbleCardParser`, `CardQrDialog`, `VCardBuilderTest`, and `ContactRepository.addMediaImage`.
  Untouched: the pre-existing vCard-QR *contact* scanner (`AddContactScreen` prefill) and the
  linked-accounts QR codes (`LinkedAccountsScreen` / `QrUtils.generateQrBitmap`) — neither is
  card sharing.
- **Legacy deserialization is covered structurally, not by a unit test.** Firestore mapping can't
  be exercised in plain JUnit without Robolectric/instrumentation; it's guaranteed by the
  `@PropertyName` mapping + all-defaulted fields.
- **`splash_icon_transparent.xml`** and the two root `*.pdf` files were already untracked on the
  branch before this work and are intentionally left out of the ticket's changes.

## Open questions / follow-ups
- **No Compose UI tests** for the editor (pre-existing gap for the app's UI); verification is
  manual.
- The ticket title still says "and sharing"; the delivered scope is templates + customization
  only, per the owner's later decision to drop sharing.
