---
title: "[P2][Feature] Digital visiting card — templates, customization, and sharing"
labels: enhancement
---

# [P2][Feature] Digital visiting card — templates, customization, and sharing

## Story / Why
Users want a shareable digital visiting card that represents them professionally. Today the
app stores a bare `VisitingCard` model (`cardTheme`, `bio`) on every profile, but there is
**no UI to build, view, or share a card** — it's dormant scaffolding. This ticket delivers the
end-to-end feature: a user picks a preset template, tweaks its accent colour / background /
font, fills in a few card-specific fields, previews the result on their Profile, and shares it
as an **image**, a **QR code**, or a **vCard (.vcf)**.

Because the app already has a **scan-QR → parse vCard → Add Contact** flow, encoding the card's
QR as a vCard means any HumbleContacts user who scans someone's card lands in a prefilled Add
Contact form — the feature closes a loop that already half-exists.

## Context

**Decisions locked with the manager (2026-08-22):**
1. **No fixed design — build against the brand kit.** There is no Figma/mockup. Design the
   editor, templates, and preview using the app's existing Material 3 theme
   (`ui/theme/Color.kt`, `Theme.kt`, `Type.kt`) and the established component patterns
   (`SettingsSection` / `SettingsRow` cards, `HumbleInputField`, rounded 16dp surfaces). Do
   **not** invent a new visual language.
2. **Card content auto-fills from the profile; the card only adds extras.** The card pulls
   `name`, `company`, `profession` (role), `phone` + `countryCode`, `email`, `linkedInUrl`, and
   `address` from the existing `UserProfile` — one source of truth, no duplicated/ drifting
   copies. The card editor edits only **card-specific** fields: `headline`, `bio`, `websiteUrl`,
   `portfolioUrl`, and the **look** (`template`, `accentColor`, `background`, `fontStyle`).
3. **The QR encodes a vCard** — the *same* vCard string as the `.vcf` export — so scanning it
   with the app's existing scanner prefills Add Contact. No hosted card page / deep-link infra
   (out of scope).
4. **Entry point + preview:** add a **"My Visiting Card"** row to the Profile screen's
   `ACCOUNT` section that opens a new editor screen, and render a **live card preview** at the
   top of the Profile screen.

**Existing code this builds on (reuse — do not reinvent):**
- **Model:** `data/model/VisitingCard.kt` — currently `{ cardTheme, bio }`. **Extend it** (see
  Scope). Note the **existing Firestore mismatch to reconcile:** `AuthRepository.kt` (~line 383)
  already *seeds* a richer map on registration — `headline`, `bio`, `websiteUrl`, `portfolioUrl`,
  `cardTheme` — that the model doesn't fully capture. Make the model, the AuthRepository seed,
  and the editor agree on one shape.
- **Persistence:** `data/repository/ProfileRepository.kt` — follow the existing
  `.update(mapOf("field" to value, "updatedAt" to Timestamp.now()))` pattern
  (see `updateShareSettings`). Add an `updateVisitingCard(card: VisitingCard)`.
  `ProfileRepository.getCurrentUserProfile()` already returns `visitingCard`.
- **QR generation:** `ui/profile/LinkedAccountsScreen.kt` already has `generateQrBitmap(content, sizePx)`,
  `qrContentFor`, `QrCodeDialog`, and the `LinkedAccount` type. **ZXing is already a dependency**
  (`com.google.zxing:core`, `com.journeyapps:zxing-android-embedded`). Reuse `generateQrBitmap`;
  do not add a QR library. Consider extracting the QR helpers to a shared util if the card needs
  them outside `LinkedAccountsScreen`.
- **vCard:** `ui/contacts/VCardParser.kt` **parses** vCards (RFC 6350: `FN`, `N`, `ORG`,
  `TITLE`, `EMAIL`, `TEL`, `ADR`, `URL`) — there is **no generator yet**. Add a `VCardBuilder`
  that emits a vCard string whose fields round-trip cleanly through `VCardParser` (verify with a
  unit test). Respect the per-field **share settings** (`ShareSettings`: `sharePhone`,
  `shareEmail`, `shareLinkedIn`) already used to gate what goes into shared cards on the Profile
  screen — a field toggled off must not appear in the card's vCard/QR.
- **Sharing / image export:** `utils/ContactExporter.kt` shows the MediaStore write pattern for
  saving a file to `Downloads/Humble Contacts`. For the **image**, render the card composable to
  a `Bitmap` and hand it to the Android share sheet (`Intent.ACTION_SEND`, `image/*`, via
  `FileProvider`); also offer save-to-gallery. For the **.vcf**, share via `ACTION_SEND`
  (`text/x-vcard`). Reuse the app's existing `FileProvider` authority if one is configured
  (check `AndroidManifest.xml`); add one only if none exists.
- **Navigation:** `navigation/Screen.kt` (add a `VISITING_CARD` route), `navigation/AppNavGraph.kt`
  (register it near the `EDIT_PROFILE` block ~line 305; pass the nav callback into `ProfileScreen`
  like `onNavigateToEditProfile`).

## 🔑 Access & prerequisites
- **No new credentials or secrets.** Everything runs against the already-configured Firebase
  project (Firestore + Auth). Firebase Storage is available but **not required** — image/vCard
  sharing is local (share sheet + MediaStore), so nothing is uploaded.
- **No new Cloud Functions** and **no Firestore rules changes** — the card lives inside the
  existing `users/{uid}` document under `visitingCard`, already covered by current rules.
- Standard local dev setup only (the repo's `google-services.json` / `local.properties` as
  already used to build the app).
- If, during build, the dev believes Storage *should* host the card image (e.g. for a future
  hosted page), **stop and ask the manager** — it's explicitly out of scope here.

## Scope

**1. Data model + reconciliation**
- Extend `VisitingCard` to a single agreed shape:
  `template: String`, `accentColor: String` (hex, e.g. `"#2E7D32"`), `background: String`,
  `fontStyle: String`, `headline: String`, `bio: String`, `websiteUrl: String`,
  `portfolioUrl: String`. Keep `@Keep`, keep all-defaulted constructor (Firestore needs it),
  and **migrate the legacy `cardTheme` field** — either keep it mapped or fold it into
  `template` with a `@PropertyName` so existing docs deserialize without loss.
- Update `AuthRepository`'s registration seed (~line 383) to write exactly this shape.
- Confirm `ProfileRepository.getCurrentUserProfile()` still deserializes older profiles whose
  `visitingCard` map has the old keys (defensive defaults — no crash on missing fields).

**2. Templates + customization**
- Provide **4–5 preset templates** (define as a typed list/enum with brand-kit-derived
  defaults — e.g. Minimal, Bold, Classic, Gradient). Each template = a layout + default
  accent/background/font.
- On top of the chosen template the user can change: **accent colour** (a small preset swatch
  palette drawn from the theme), **background** (e.g. solid / subtle gradient / surface), and
  **font style** (a small curated set). No free-form drag-and-drop layout editing (out of scope).

**3. Editor screen** (`ui/profile/VisitingCardScreen.kt` + `VisitingCardViewModel.kt`)
- Loads the current `UserProfile` (for auto-filled fields) + its `visitingCard`.
- Sections: template picker, look controls (accent/background/font), card-specific text fields
  (`headline`, `bio`, `websiteUrl`, `portfolioUrl`), and a **live preview** that updates as the
  user edits.
- **Save** persists via `ProfileRepository.updateVisitingCard(...)`. Standard
  loading / saving / error states.

**4. Preview on Profile**
- Render the card preview at the top of `ProfileScreen` (above or within the hero) using the
  saved template + profile data. Tapping it (or the new `ACCOUNT` row) opens the editor.

**5. Sharing**
- **Image:** render the card to a bitmap → share sheet (`image/*`) + save-to-gallery option.
- **QR:** generate via the existing `generateQrBitmap`, encoding the card's **vCard string**;
  show it in a dialog (reuse `QrCodeDialog`'s look) with a share/save option.
- **vCard (.vcf):** build the vCard via the new `VCardBuilder`, share via `ACTION_SEND`
  (`text/x-vcard`).
- All three respect `ShareSettings` (omit phone/email/LinkedIn the user has toggled off).

**6. Strings & tests**
- All user-facing text in `res/values/strings.xml` (no hardcoded strings).
- Unit tests: `VCardBuilder` output round-trips through `VCardParser`; share-settings gating
  omits toggled-off fields; legacy `visitingCard` map still deserializes.

## Acceptance Criteria
- [ ] A **"My Visiting Card"** row appears in the Profile `ACCOUNT` section and opens the editor;
      a **live card preview** renders at the top of the Profile screen.
- [ ] The editor offers **4–5 templates** and lets the user change **accent colour, background,
      and font style**; the preview updates live.
- [ ] Card **auto-fills** name/company/role/phone/email/LinkedIn/address from `UserProfile`; the
      editor only edits `headline`, `bio`, `websiteUrl`, `portfolioUrl`, and the look. Changing
      the profile is reflected in the card (no duplicated stored copy of those fields).
- [ ] The card is stored under `users/{uid}.visitingCard` via
      `ProfileRepository.updateVisitingCard`; reopening the editor shows the saved state.
- [ ] `VisitingCard` model, `AuthRepository` seed, and editor **agree on one field shape**;
      profiles created before this change still load without crashing (legacy `cardTheme`
      handled).
- [ ] **Share as image** works via the Android share sheet and can be saved to the gallery.
- [ ] **Share as QR** shows a QR that **encodes a vCard**; scanning it with the app's existing
      scanner opens Add Contact prefilled with the card's details.
- [ ] **Share as vCard (.vcf)** works via the share sheet; the file round-trips through
      `VCardParser`.
- [ ] All three share paths **honour `ShareSettings`** — a field toggled off on the Profile
      screen does not appear in the image, QR, or vCard.
- [ ] No new secrets, Cloud Functions, or Firestore-rules changes.
- [ ] UI standards below are met (light + dark, insets, states, a11y, i18n).
- [ ] `./gradlew :app:compileDebugKotlin :app:testDebugUnitTest` is green; new unit tests pass.

## 🖼️ UI standards (Definition of Done for the visual work)
No design is attached — **build against the app's brand kit / Material 3 theme** (this is the
"no design — build it yourself" case). Then:
- [ ] **Design-system tokens & shared components.** Use `MaterialTheme.colorScheme` / typography
      tokens and reuse the existing `SettingsSection`, `SettingsRow`, `HumbleInputField`,
      rounded-16dp `Surface`/`Card` patterns. No one-off hardcoded colours/sizes except the
      user-chosen card accent (which is data, stored on the model).
- [ ] **Light + dark themes.** Verify the editor, preview, and every template in both. The
      user-selected accent/background must remain legible (sufficient contrast) in both modes.
- [ ] **Native components** — Material 3 pickers, dialogs, text fields, bottom sheets, etc. If a
      template effect can't be done natively, tell the manager, explain the trade-off, proceed
      with the closest native approach.
- [ ] **Edge-to-edge + safe areas.** Content respects status bar, cutout, and the bottom
      gesture / nav bar; only decorative background bleeds under system bars.
- [ ] **Responsive** across small phone → large phone → tablet/foldable and both orientations;
      cap content width and centre on large screens. Card preview scales without clipping.
- [ ] **Correct truncation** — long name/headline/bio ellipsize (`…`) cleanly on the card;
      layout doesn't break.
- [ ] **Keyboard:** right IME type per field (URL keyboard for website/portfolio), Next/Done
      actions wired, focused field stays visible above the keyboard, tap-outside dismisses.
- [ ] **States:** loading (profile fetch), saving (disable Save + progress), error (inline,
      design-consistent), and empty (no card yet → sensible default template).
- [ ] **State preserved** across rotation / config change / process death (editor form input,
      chosen template).
- [ ] **Accessibility:** content descriptions on the card preview, template swatches, colour
      swatches, and share actions; logical focus order; ≥48dp touch targets; WCAG-AA contrast.
- [ ] **Dynamic type** — layouts survive the largest font scale.
- [ ] **No hardcoded user-facing strings** — all via `strings.xml`.
- [ ] **Architecture:** no business logic in composables — a `VisitingCardViewModel` drives
      state; repository does the Firestore/vCard/image work.

## Out of scope
- Free-form drag-and-drop layout editing (only template + accent/background/font).
- A hosted/shareable web card page or deep links (QR encodes a vCard, not a URL).
- Uploading the card image to Firebase Storage.
- New Cloud Functions or Firestore-rules changes.
- NFC sharing (a separate concern; there's an `onNavigateToNfc` stub but it's not part of this).
- Changing the profile-editing flow itself (the card auto-reads existing profile fields).

## Dependencies
- None external. Reuses existing ZXing (QR), Coil (images), Firebase (Auth/Firestore), and the
  in-repo `VCardParser`, `generateQrBitmap`, and MediaStore export patterns. No new libraries
  expected — flag it if you find you need one.

## References
- `app/src/main/java/com/humblesolutions/humblecontacts/data/model/VisitingCard.kt`
- `app/src/main/java/com/humblesolutions/humblecontacts/data/model/UserProfile.kt`
- `app/src/main/java/com/humblesolutions/humblecontacts/data/repository/ProfileRepository.kt`
- `app/src/main/java/com/humblesolutions/humblecontacts/data/auth/AuthRepository.kt` (~L383 — card seed to reconcile)
- `app/src/main/java/com/humblesolutions/humblecontacts/ui/profile/ProfileScreen.kt` (entry row + preview)
- `app/src/main/java/com/humblesolutions/humblecontacts/ui/profile/LinkedAccountsScreen.kt` (`generateQrBitmap`, `QrCodeDialog`, `LinkedAccount`)
- `app/src/main/java/com/humblesolutions/humblecontacts/ui/contacts/VCardParser.kt` (parser to round-trip against)
- `app/src/main/java/com/humblesolutions/humblecontacts/utils/ContactExporter.kt` (MediaStore export pattern)
- `app/src/main/java/com/humblesolutions/humblecontacts/data/model/ShareSettings.kt` (per-field share gating)
- `app/src/main/java/com/humblesolutions/humblecontacts/navigation/Screen.kt` & `AppNavGraph.kt` (routing)

## Kickoff prompt
```
/start-ticket 64
```
