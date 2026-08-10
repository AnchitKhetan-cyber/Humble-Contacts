# Release hardening — backup off + R8 minify/shrink/obfuscate

Two release-build protections (ticket #12/QA #15–16). **Debug builds are unaffected.**

## 1. Backup disabled
`android:allowBackup="false"` on `<application>` in `AndroidManifest.xml`. This stops
`adb backup` from pulling the app's local data (which includes third parties' contact
details and cached business-card images) off-device.

> If device-to-device backup is ever wanted, replace this with `android:dataExtractionRules`
> / `android:fullBackupContent` that **exclude** contacts and cached card images — do not
> simply set `allowBackup="true"`.

## 2. R8 minify + resource shrink (release only)
In `app/build.gradle.kts` the `release` build type sets:
```kotlin
isMinifyEnabled = true
shrinkResources = true
```
This shrinks and obfuscates code and strips unused resources.

### Why keep rules are required
R8 renames and removes code it believes is unused. **Firestore model classes are populated by
reflection** — `toObject(Contact::class.java)` matches Firestore field names to Kotlin
property names. If R8 renames a property, that field silently deserializes as empty. The keep
rules in `app/proguard-rules.pro` prevent this.

### What is kept (see `app/proguard-rules.pro`)
- **`data.model.**`** — all Firestore models, their members and constructors. The classes also
  carry `@Keep` (androidx) as in-code documentation of intent.
- **`@Keep`-annotated classes/members** anywhere.
- **Annotation attributes / Signature** so Firestore `@Exclude` / `@PropertyName` and generics
  survive.
- **Kotlin metadata** and **kotlinx.serialization** generated serializers (defensive).
- **Firebase / ML Kit / zxing** ship their own consumer keep rules via their AARs, so their
  internals are retained automatically.

## Verifying the release build
Config validity (R8 runs, keep rules compile, APK produced):
```bash
./gradlew :app:assembleRelease
```
An unsigned APK is emitted at `app/build/outputs/apk/release/`. The R8 mapping is written to
`app/build/outputs/mapping/release/mapping.txt` (keep this for deobfuscating crash traces).

### On-device round-trip (needs a SIGNED release build)
The release variant has no local signing key (Play App Signing), so install/run requires the
signing key. On a signed minified release build, verify with a Firebase test account and watch
Logcat for R8 `ClassNotFoundException` / missing-field issues:

1. **Contacts round-trip:** add a contact → scan & parse a business card → save → see it in the
   list → open detail → edit a field → toggle favourite → **CSV export** (open the file, check
   all columns are populated).
2. **Auth flows:** sign in with **Google**, **Email**, and **Phone**.
3. **QR:** scan a `humblecontacts://contact/{id}` code (zxing).

No missing fields and no crashes = keep rules are correct. If a field comes back empty only on
the release build, a keep rule for that model is missing.
