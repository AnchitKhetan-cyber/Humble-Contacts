# Account-deletion OTP gate — required Firebase / domain setup

The account-deletion flow now requires a **final verification gate** before anything is
deleted: a **confirmation link** is emailed (if the account has an email) **and** an **OTP**
is sent to the phone (if the account has a phone). When both are on file, **both** must be
completed. Provider re-auth to actually delete the Firebase Auth account is obtained without
a Google account picker (silent auto-select for Google-only accounts).

The **phone OTP** half works out of the box. The **email confirmation link** half needs the
setup below before it will function at runtime — the code is wired but inert until then.

## 1. Enable email-link sign-in
Firebase console → Authentication → Sign-in method → **Email/Password** → enable
**Email link (passwordless sign-in)**.

## 2. Host an App Links domain (Dynamic Links is gone)
Firebase Dynamic Links (`*.page.link`) was shut down in 2025 and **cannot** be used.
You must host a domain you control that serves Android App Links:

- Serve `https://<your-domain>/.well-known/assetlinks.json` for the app's package
  (`com.humblesolutions.humblecontacts`) and signing certificate SHA-256.
- Add `<your-domain>` to Firebase console → Authentication → Settings → **Authorized domains**.

## 3. Point the code at your domain (keep these in sync)
Replace the placeholder `humblecontacts.example.com` / `/finishDelete` in **both** places:

- `AuthRepository.EMAIL_LINK_CONTINUE_URL`
  (`app/src/main/java/com/humblesolutions/humblecontacts/data/auth/AuthRepository.kt`)
- The App Links `intent-filter` host/pathPrefix in `app/src/main/AndroidManifest.xml`

## How the round trip works (for reference)
1. `DeleteAccountScreen` sends the link via `AuthRepository.sendReauthEmailLink(email)` and
   shows a "check your email" state; `PendingDeletionStore` records the in-progress deletion.
2. The user taps the link → Android opens `MainActivity` with the deep link →
   `MainActivity` verifies it with `FirebaseAuth.isSignInWithEmailLink(...)`, stashes the URL
   in `PendingDeletionStore`, and routes to `Routes.DELETE_ACCOUNT`.
3. `DeleteAccountScreen`'s `LaunchedEffect` consumes the URL via
   `ProfileViewModel.completeEmailLink(...)`. For Email/Password accounts the link
   re-authenticates; for Google-only accounts it counts as confirmation only.
4. Once every required channel is verified, `ProfileViewModel.finalizeDeletion(...)` runs:
   silent Google re-auth (Google-only accounts) → delete Auth account → best-effort cleanup
   of the Firestore user doc, contacts, and Storage business-card images.

## Known limitations
- **Silent Google re-auth** stays picker-free only when exactly one authorized Google account
  exists on the device. With 0 or 2+, it surfaces an actionable error instead of a picker, so
  those users can't delete without re-establishing a single authorized account.
- The email-link round trip is best-effort across process death; opening the delete screen
  fresh (not via the link) clears any leftover in-progress state and starts clean.
