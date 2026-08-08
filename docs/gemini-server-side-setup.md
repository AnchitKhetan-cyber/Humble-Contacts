# Business-card parsing — server-side Gemini key setup

Business-card OCR text is parsed by Gemini **on the server**, inside the
`parseBusinessCard` Cloud Function. The Gemini API key lives **only** in the Functions
secret store — it is not in the app, `local.properties`, `BuildConfig`, or the repo.

## Why
Previously the key was embedded in the APK via `buildConfigField` → `BuildConfig.GEMINI_API_KEY`,
so anyone could extract it from a Play Store build with `apktool` and run up the Gemini bill.
The call now goes: **app → authenticated Cloud Function → Gemini**.

## One-time setup (manager / deployer)

1. **Rotate the key.** The old key already shipped in public APKs — treat it as compromised.
   Create a new Gemini API key and **revoke the old one** in Google AI Studio / Google Cloud.

2. **Store the new key as a Functions secret** (never in code or `.env`):
   ```bash
   firebase functions:secrets:set GEMINI_API_KEY
   # paste the ROTATED key when prompted
   ```

3. **Deploy:**
   ```bash
   firebase deploy --only functions
   ```
   The `parseBusinessCard` function declares `secrets: ["GEMINI_API_KEY"]`, so the key is
   injected at runtime only.

## How it works
- The app calls the callable `parseBusinessCard` with `{ text: <OCR text> }`
  (`BusinessCardParser.parse` in the Android app).
- The function **requires Firebase Auth** (`request.auth`) and rejects anonymous callers.
- It calls `gemini-2.0-flash` with the server-held key, normalises the response
  (strips ```` ``` ```` fences, extracts the JSON object, coerces to fixed fields), and returns
  the parsed contact. Malformed model output yields empty fields rather than an error, so the
  client never crashes.

## The app needs no key
`GEMINI_API_KEY` has been removed from `local.properties` and `app/build.gradle.kts`, and the
`com.google.ai.client.generativeai` dependency is gone. No client build config is required.

## Verify
- `firebase functions:secrets:access GEMINI_API_KEY` shows the secret exists.
- Signed-in user scans a card → contact fields populate.
- A release build contains no key: `apktool d app-release.apk && grep -r "AQ\.\|GEMINI_API_KEY" .`
  finds nothing.
