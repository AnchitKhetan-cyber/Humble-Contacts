# Handoff — Ticket #1

**Ticket:** #1 — [P0][Bug] Account deletion corrupts user data when re-auth is required

## Summary
Account deletion previously deleted a user's Firestore data (contacts + user doc) **before** deleting the Firebase Auth account. Because deleting an Auth account frequently throws `FirebaseAuthRecentLoginRequiredException`, users were left with their data wiped but their login still working — and the UI reported success and navigated them out. This change reverses the flow: the user is **re-authenticated first**, and only if that succeeds is the account deleted in the order **Auth account → Firestore user doc → contacts → Storage business-card images**. Re-authentication is wired for all three providers in use (Email/Password, Google, Phone). The false-success UI path is removed, so any failure keeps the user on-screen with their data intact, and a second, unreachable delete dialog that carried the same bug was deleted.

## Files changed
**Auth / data layer**
- `app/src/main/java/com/humblesolutions/humblecontacts/data/auth/AuthRepository.kt` — adds re-authentication support: `currentProviderId()`, `currentEmail`/`currentPhoneNumber`, credential builders (`buildEmailCredential`/`buildGoogleCredential`/`buildPhoneCredential`), `sendReauthOtp(...)` for the phone OTP flow, and `reauthenticate(credential)` which calls `currentUser.reauthenticate(...)`.
- `app/src/main/java/com/humblesolutions/humblecontacts/data/repository/ContactRepository.kt` — `deleteAllContacts`/`deleteUserDocument` now take an explicit `ownerUid` (default preserves old behaviour) so they target the correct paths *after* the Auth user is gone; adds `deleteBusinessCardImages(ownerUid)` to list-and-delete `business_cards/<uid>/…` Storage files.

**ViewModel**
- `app/src/main/java/com/humblesolutions/humblecontacts/ui/profile/ProfileViewModel.kt` — `deleteAccount` now takes an `AuthCredential`, re-authenticates first (fail/cancel ⇒ delete nothing + `onError`), snapshots the uid before deleting Auth, deletes Auth first, then best-effort cleans up user doc / contacts / Storage. `onSuccess()` only fires after the Auth account is actually deleted. Removes the old `REAUTH_REQUIRED` special-case. Adds passthroughs the UI needs to build credentials.

**UI**
- `app/src/main/java/com/humblesolutions/humblecontacts/ui/profile/DeleteAccountScreen.kt` — the confirm action now runs `startReauth()`, which branches on the user's provider to collect a fresh credential (password dialog for Email, Google account picker via `GoogleSignInHelper`, OTP dialog for Phone) before calling `deleteAccount`. Adds the password and OTP re-auth dialogs. On any error it shows a toast and **does not** call `onDeleteSuccess()`.
- `app/src/main/java/com/humblesolutions/humblecontacts/ui/profile/ProfileScreen.kt` — removes an unreachable inline delete dialog (`showDeleteDialog` was never set to `true`) that bypassed re-auth and duplicated the bug, plus its now-unused state. The live delete entry point (`onNavigateToDeleteAccount` → `DeleteAccountScreen`) is unchanged.

## How to test
Requires a device/emulator and **disposable** Firebase test accounts (never a real user's). Verify results in the Firebase console.

1. Build/run the app: `./gradlew :app:installDebug` (or run from Android Studio). Compilation is verified via `./gradlew :app:compileDebugKotlin`.
2. **Email/Password (required):**
   - Sign in with an Email test account, add a contact with a business-card image.
   - Let the login go "stale" (e.g. sign in earlier, or trigger deletion after some time).
   - Go to Profile → Delete Account, complete the checklist + type `DELETE`, confirm.
   - At the password prompt, tap **Cancel** → verify a toast, you stay on-screen, and in the console the user, contacts, and `business_cards/<uid>/…` are all still present.
   - Repeat and enter the **correct** password → verify "Account deleted successfully", you land on the logged-out screen, and in the console the Auth user, `users/<uid>` doc, all contacts, and `business_cards/<uid>/…` are gone.
   - Enter a **wrong** password → verify an error toast and no deletion.
3. **Google (if testable):** same flow; at confirm, the Google account picker appears; cancelling it deletes nothing; completing it deletes the account + data.
4. **Phone (if testable):** same flow; an OTP is sent to the account's number; wrong/blank code or Cancel deletes nothing; correct code deletes the account + data.

## Acceptance criteria
- [x] **Deleting with a stale login re-prompts to authenticate first; cancel/fail ⇒ no data deleted, account still works.** Re-auth runs before any delete; on error/cancel the flow returns before touching data. *(Logic verified by code + compile; on-device confirmation pending — see Open questions.)*
- [x] **After success, Auth account, `users/<uid>`, all contacts, and `business_cards/<uid>/…` are gone.** `deleteAccount` deletes all four; Storage cleanup added. *(Console verification pending on-device.)*
- [x] **No code path deletes Firestore/contacts while the Auth account still exists.** Auth is deleted first in `ProfileViewModel`; the second (unreachable, non-reauth) delete dialog in `ProfileScreen` was removed.
- [x] **UI never shows "deleted"/navigates to logged-out unless the Auth account was actually deleted.** `onSuccess()` fires only after `deleteCurrentUser()` succeeds; the error path no longer calls `onDeleteSuccess()`, and the old `REAUTH_REQUIRED → onDeleteSuccess()` bug is removed.
- [x] **Re-auth works for testable providers (Email required; Google/Phone if testable); error names the next step.** All three providers wired; messages are actionable (e.g. "Re-authentication cancelled. Your account was not deleted."). *(Runtime confirmation per provider pending on-device.)*

## Deviations / decisions
- **Order & security-rule assumption:** Implemented Auth-first exactly per the ticket's confirmed decision. Because the data deletes then run while unauthenticated, this relies on the Firestore/Storage rules being open/test-mode (confirmed by manager during planning). If a real user's data later fails to clean up, security rules are the first suspect.
- **uid captured before Auth deletion:** `deleteAllContacts`/`deleteUserDocument` were changed to accept an explicit `ownerUid`. Necessary because once the Auth account is deleted, `auth.currentUser` is null and the repository's `uid` getter returns `""`, which would target wrong paths.
- **Post-Auth cleanup is best-effort:** After the Auth account is deleted, user doc / contacts / Storage deletions are wrapped in `runCatching` and logged on failure — a cleanup hiccup does not turn a real account deletion into a reported failure (the account is already gone). The Auth delete itself is still hard-failed (stops the flow, reports error).
- **Removed dead code:** `ProfileScreen`'s inline delete dialog was unreachable (`showDeleteDialog` never set true) and re-implemented the buggy flow, so it was removed rather than updated, to guarantee a single re-auth-gated delete path.
- **Full three-provider scope:** Per decision, Email/Google/Phone re-auth are all built and wired (not stubbed).

## Open questions / follow-ups
- **On-device verification outstanding:** Only compilation is verified in this environment. The acceptance criteria above need a device + test accounts to confirm at runtime (especially the console checks and the Google/Phone paths).
- **Confirm the security-rule assumption** for production: if rules require `request.auth`, the post-Auth data deletes would be rejected and leave orphaned data; that would require revisiting the order or a server-side cascade (out of scope here).
- **Related follow-ups (separate tickets, per the ticket):** orphaned-Storage cleanup on single-contact delete; auth-path consolidation.
