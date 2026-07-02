# Compose Multiplatform Migration — Inventory & Plan

Branch: `feature/multiplatform`. Android app must keep building green after every step.

Target module layout (created by the Compose Multiplatform wizard on the Mac):

```
composeApp/
  src/commonMain/kotlin   ← shared UI + logic (most of the app)
  src/androidMain/kotlin  ← Android-only (Activities, permissions, camera, notifications)
  src/iosMain/kotlin      ← iOS actual implementations
iosApp/                   ← Xcode project (Swift host)
```

---

## 1. Dependency replacement table

| Current (Android-only) | Multiplatform replacement | Notes |
|---|---|---|
| `com.google.firebase:firebase-auth/firestore/storage/messaging` (BOM) | **GitLive `dev.gitlive:firebase-auth / firestore / storage`** | KMP wrapper over native Firebase. iOS side adds Firebase iOS SDK via CocoaPods + `GoogleService-Info.plist`. |
| `firebase-messaging` (FCM) | GitLive `firebase-messaging` + **APNs** on iOS | Push differs per platform; iOS phone-auth also needs APNs. |
| `io.coil-kt:coil-compose:2.7.0` | **Coil 3** (`io.coil-kt.coil3:coil-compose`) | Coil 3 is multiplatform — closest drop-in. |
| `androidx.navigation:navigation-compose` | **Jetpack Navigation (multiplatform)** or **Voyager/Decompose** | Nav-compose has a KMP build now; verify version. |
| `androidx.lifecycle:lifecycle-viewmodel-compose` | **`org.jetbrains.androidx.lifecycle:lifecycle-viewmodel-compose`** (JetBrains KMP fork) | ViewModel works in common. |
| `androidx.datastore:datastore-preferences` | **DataStore multiplatform** (`androidx.datastore:datastore-preferences-core`) | Provide the file path per platform. |
| `com.google.zxing:core` + `zxing-android-embedded` | **iOS: AVFoundation scanner**; Android: keep zxing | Behind a common `QrScanner` interface (expect/actual). |
| `com.google.mlkit:text-recognition` (OCR) | **iOS: Vision framework**; Android: keep MLKit | Behind a common `TextRecognizer` interface. |
| `com.google.ai.client.generativeai:generativeai` (Gemini) | **Ktor HTTP client (multiplatform)** calling the Gemini REST API | The Android SDK is Android-only; use REST from commonMain. |
| Google Sign-In (`credentials`, `googleid`) | **Native per platform** (or drop if phone-auth only) | iOS uses Firebase Google provider / Sign in with Apple. |
| `androidx.work:work-runtime-ktx` (WorkManager) | **iOS: BGTaskScheduler**; Android: keep WorkManager | Behind a common scheduler interface. |
| `kotlinx-coroutines-play-services` | Not needed on iOS | Android-only glue for Play Services Tasks. |

---

## 2. File-by-file buckets

### Bucket A — move to `commonMain` mostly as-is (pure Kotlin / Compose)
- `ui/theme/Color.kt`, `ui/theme/Type.kt` (check Type.kt font refs)
- `ui/profile/CompleteProfileUiState.kt`, `ui/profile/CompleteProfileEvent.kt`
- `ui/splash/SlashUiState.kt`
- `navigation/Screen.kt` (route constants)
- `data/auth/CountryCode.kt`
- `data/model/ShareSettings.kt`, `UserStats.kt`, `VisitingCard.kt`
- `ui/components/BottomNavBar.kt`, `ui/auth/LogoAndIcons.kt` (verify no `android.*`)

### Bucket B — `commonMain`, but need a dependency swap or small refactor
(Firebase / Coil / DataStore / ViewModel / Navigation — replace the import, logic stays)
- **Data models w/ Firebase annotations:** `Contact.kt`, `ContactNote.kt`, `Event.kt`, `Reminder.kt`, `UserProfile.kt` → use GitLive/plain types instead of `com.google.firebase.Timestamp` etc.
- **Repositories:** `FirebaseRepository.kt`, `ProfileRepository.kt`, `ContactRepository.kt`, `data/auth/AuthRepository.kt` → GitLive Firebase.
- **ViewModels:** `AuthViewModel`, `PhoneAuthViewModel`, `ContactViewModel`, `HomeViewModel`, `ProfileViewModel`, `CompleteProfileViewModel`, `SettingsViewModel`, `ui/splash/SplashViewModel` → KMP ViewModel; move Android bits out.
- **Screens (UI):** `LoginScreen`, `RegisterScreen`, `PhoneScreen`, `HomeScreen`, `ContactsScreen`, `Contactdetailscreen`, `Addcontactscreen`, `ProfileScreen`, `CompleteProfileScreen`, `DeleteAccountScreen`, `LinkedAccountsScreen`, `IntroScreen`, `IntroductionScreen`, `SplashScreen`, `ui/theme/Theme.kt`, `AppNavGraph.kt`, `AuthNavGraph.kt`, `AuthComponents.kt`, `CountryCodeDropdown.kt` → move UI to common; extract every `Context`/launcher/camera call into an `expect` interface (see Bucket C).
- **Prefs:** `ThemePreference.kt`, `data/preferences/SettingsPreferences.kt` → DataStore multiplatform.

### Bucket C — stays Android-only (`androidMain`) + needs an iOS `actual`
These are the true platform features. Each gets a `commonMain` interface (`expect`) with Android + iOS implementations:
- `MainActivity.kt`, `ui/splash/SplashActivity.kt` → Android entry points (iOS has its own host in `iosApp`).
- `notifications/NotificationHelper.kt`, `notifications/HumbleFirebaseMessagingService.kt` → notifications/FCM.
- `ui/contacts/saveBitmapAndReturnUri.kt`, `ui/contacts/OCR Function.kt`, `ui/contacts/BusinessCardParser.kt` → camera bitmap + OCR (MLKit→Vision).
- QR scanning in `HomeScreen.kt` (zxing) → common `QrScanner` interface.
- Camera/photo pick + permission launchers in `Addcontactscreen.kt`, `Contactdetailscreen.kt`.
- `data/auth/GoogleSignInHelper.kt` → native sign-in per platform.
- `utils/ContactExporter.kt`, `utils/NetworkUtils.kt` → file export + connectivity (platform APIs).
- Gemini calls in `BusinessCardParser.kt` → Ktor REST from common.

---

## 3. Platform features needing `expect`/`actual`
1. Firebase init + Auth (phone) + Firestore + Storage
2. Camera capture / image picker
3. QR scanner
4. OCR / text recognition
5. Push notifications (FCM / APNs)
6. Local prefs (DataStore path)
7. Connectivity check
8. File export / share
9. Gemini REST client (Ktor) — mostly common, just the HTTP engine is per-platform

---

## 4. Order of work (keep Android green throughout)
1. Wizard-scaffold a Compose Multiplatform project; run empty Android + iOS.
2. Move Bucket A → `commonMain`. Build Android.
3. Swap dependencies for Bucket B (Firebase→GitLive, Coil3, DataStore-mp, KMP ViewModel/Nav). Move logic + screens to `commonMain`. Build Android after each sub-area.
4. Define `expect` interfaces for Bucket C; implement Android `actual` first (should match current behavior). Build Android — must equal the pre-migration baseline.
5. Only then implement iOS `actual`s and run the iOS simulator (Mac).
6. iOS app config (bundle id, signing, Info.plist camera/push, Firebase plist).
7. TestFlight → App Store.

**Gate:** after step 4 the Android app must behave exactly like `main`. If not, fix before touching iOS.
