# Account-deletion email verification (Google sign-in)

Google sign-in accounts must click a **confirmation link** emailed to them before deletion
completes. Because the Firebase client can't send email, this uses two Cloud Functions in
`functions/`. Email/Password accounts (password prompt) and Phone accounts (OTP) are
unaffected.

## Flow
1. User taps **Delete Forever** → the app calls the callable **`requestAccountDeletion`**.
2. The function writes a one-time token to `account_deletions/{uid}` and emails a link to
   the account email pointing at the HTTPS **`confirmAccountDeletion`** endpoint
   (`https://us-central1-<project>.cloudfunctions.net/confirmAccountDeletion?uid=…&token=…`
   — a plain Cloud Functions URL, **no App Links domain required**).
3. The app shows a "check your email" state and **listens** to `account_deletions/{uid}`.
4. User taps the link → `confirmAccountDeletion` validates the token and sets
   `confirmed: true` → the app's listener fires → it does the **silent Google re-auth** and
   deletes the Auth account, user doc, contacts, and Storage images.

## Setup required before this works
1. **Install dependencies**: `cd functions && npm install`.
2. **SMTP credentials** for nodemailer — copy `functions/.env.example` to `functions/.env`
   (git-ignored) or set them as Firebase secrets:
   `SMTP_HOST`, `SMTP_PORT`, `SMTP_USER`, `SMTP_PASS`, `SMTP_FROM`.
3. **Deploy**: `firebase deploy --only functions`.
4. **Firestore rule** so a signed-in user can read (listen to) and delete their own request,
   while only the Cloud Functions (admin) create/confirm it:

   ```
   match /account_deletions/{uid} {
     allow read, delete: if request.auth != null && request.auth.uid == uid;
     allow create, update: if false; // Cloud Functions (admin) only
   }
   ```

## Notes / limitations
- Region is assumed `us-central1` in both the function link and `FirebaseFunctions.getInstance()`.
  If you deploy elsewhere, update `REGION` in `functions/index.js` and the app's
  `FirebaseFunctions.getInstance("<region>")`.
- The confirmation listener works while the app stays alive (user taps the link in a browser
  and returns). If the app is killed, re-opening the delete screen re-attaches the listener
  and picks up an already-confirmed request.
- Deletion is still authorized by the silent Google re-auth; the email is the confirmation gate.
- Not runtime-tested here — requires the deploy + SMTP setup above.
