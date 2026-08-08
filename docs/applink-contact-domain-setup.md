# Contact-sharing App Link — domain setup & link format

Contact-sharing deep links are **Android App Links**: `https://<domain>/contact/{id}`.
Tapping one opens the app straight to that contact.

The old host `humblecontacts.page.link` belonged to **Firebase Dynamic Links, which Google
shut down on 25 Aug 2025**. That host is dead, so it was replaced with a domain we control.

## The link format (for whoever generates shareable links)

```
https://links.humblecontacts.app/contact/{contactId}
```

- `{contactId}` is the Firestore contact id.
- `MainActivity` reads the **last path segment** as the contact id and routes to the contact
  detail screen, so the path must end in the id (no trailing slash, no query needed).
- The QR / custom-scheme form `humblecontacts://contact/{contactId}` is unchanged and still
  works; it needs no domain.

## ⚠️ CONFIRM before production

1. **Domain** — currently `links.humblecontacts.app`. If the manager picks a different domain,
   change it in **exactly two places** (they must match):
   - `MainActivity.CONTACT_LINK_HOST`
   - the App Links `intent-filter` host in `app/src/main/AndroidManifest.xml`
   …and serve `assetlinks.json` from that domain instead.
2. **Release SHA-256** — `public/.well-known/assetlinks.json` currently lists the **debug**
   fingerprint (for local testing) plus a placeholder
   `PASTE_RELEASE_APP_SIGNING_SHA256_FROM_PLAY_CONSOLE`. Replace the placeholder with the
   **App signing key certificate SHA-256** from **Play Console → your app → Setup →
   App signing**. If you also want upload-key-signed builds to verify, add the **Upload key**
   SHA-256 as a third entry. **App Links will not verify in production until this is done.**

## Hosting — Firebase Hosting

`assetlinks.json` is served from Firebase Hosting:

- File: `public/.well-known/assetlinks.json`
- `firebase.json` has a `hosting` block whose `public` dir is `public/`. The default
  `"**/.*"` ignore pattern was deliberately removed so the `.well-known` directory deploys.

Deploy just hosting:

```bash
firebase deploy --only hosting
```

After deploy, confirm it's reachable and JSON:

```bash
curl -sSL https://links.humblecontacts.app/.well-known/assetlinks.json
```

> If the domain is a custom domain, add it under **Firebase console → Hosting → Add custom
> domain** and finish DNS verification first.

## Verify App Links on a device

Install a build, then:

```bash
adb shell pm get-app-links com.humblesolutions.humblecontacts
```

The new domain should show `verified`. Force re-verification if needed:

```bash
adb shell pm verify-app-links --re-verify com.humblesolutions.humblecontacts
```

Test the link opens the app:

```bash
adb shell am start -a android.intent.action.VIEW \
  -d "https://links.humblecontacts.app/contact/EXAMPLE_ID"
```

## Related

- `docs/email-link-deletion-setup.md` covers a **separate** App Link
  (`humblecontacts.example.com` / `/finishDelete`) for account-deletion confirmation. If that
  moves to the same owned domain later, this same `assetlinks.json` (same package + SHA-256s)
  can serve both — only the intent-filter host/pathPrefix and `AuthRepository.EMAIL_LINK_CONTINUE_URL`
  would change. That work is out of scope for this ticket.
