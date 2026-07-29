# Handoff — Ticket #2

**Ticket:** #2 — [P0][Bug] App crashes on launch on Android 7.x (API 24–25) — NotificationChannel

## Summary
`NotificationHelper.createChannels()` created `NotificationChannel` objects (an API 26+ class) with no version guard, and `MainActivity.onCreate()` calls it on every launch — so every Android 7.0/7.1 (API 24–25) user hit a 100% startup crash (`NoClassDefFoundError`/`NoSuchMethodError`). The fix adds an early return at the top of `createChannels()` when `Build.VERSION.SDK_INT < Build.VERSION_CODES.O`, so channel creation is skipped on pre-26 devices (where channels don't exist) and runs unchanged on API 26+. The guard lives inside the method, so all callers are protected without changing any call sites. `minSdk` stays `24`; no functionality is lost on 24–25 because notification channels are a no-op concept there.

## Files changed
**Notifications**
- `app/src/main/java/com/humblesolutions/humblecontacts/notifications/NotificationHelper.kt` — add `import android.os.Build`; add `if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return` as the first line of `createChannels()` so the `NotificationChannel` code only runs on API 26+.

## How to test
1. Build: `./gradlew :app:compileDebugKotlin` (passes).
2. Lint the `NewApi` check: `./gradlew :app:lintDebug`, then confirm the report has **no `NewApi`/`NotificationChannel` error** for `NotificationHelper.createChannels` (`app/build/reports/lint-results-debug.txt`).
3. (Runtime, not run here — see below) Launch the app on an **API 24** and an **API 25** emulator and confirm it reaches the first screen without crashing.
4. On an **API 26+** device/emulator, confirm notification channels are still created (Settings → Apps → Humble Contacts → Notifications shows "Follow-up Reminders" and "General") and that reminder/general notifications still post.

## Acceptance criteria
- [~] **App launches without crashing on API 24 emulator.** Provably fixed in code (the `NotificationChannel` path is unreachable when `SDK_INT < O`), but **not runtime-verified** — no API 24 system image is installed in this environment. Accepted on code + lint verification per developer.
- [~] **App launches without crashing on API 25 emulator.** Same as above.
- [x] **On API 26+, channels are still created and notifications still post (no regression).** The guard only short-circuits below API 26; all code at/after the guard is unchanged (see diff).
- [x] **`minSdk` remains 24.** No change to `app/build.gradle.kts` in this diff.
- [x] **No lint error for calling a 26+ API without a guard on this path.** `lintDebug` no longer reports a `NewApi` error on `NotificationChannel`/`createChannels` (the early-return `SDK_INT` guard is recognized by lint).

## Deviations / decisions
- **Guard style:** used an early `return` (`if (SDK_INT < O) return`) rather than wrapping the whole body in an `if (SDK_INT >= O) { … }` block. Same effect and same protection for all callers; the early return keeps the diff minimal and the method flat. The ticket explicitly allowed either form as long as the guard is inside the method.
- **Runtime verification skipped by agreement:** the fix is trivially correct and lint-verified; booting an API 24 emulator required downloading a ~1 GB system image, so the developer accepted code + lint verification instead of on-device 7.x testing.

## Open questions / follow-ups
- **Pre-existing lint errors (out of scope):** `lintDebug` reports 2 `MissingPermission` errors on the `notify()` calls in `NotificationHelper.kt` (lines ~87 and ~121) about `POST_NOTIFICATIONS`. These pre-date this change (the `notify` calls were not touched) and are unrelated to the crash; `MainActivity` already requests `POST_NOTIFICATIONS` at runtime. Worth a separate cleanup ticket to add an explicit permission check / lint annotation.
- **Optional runtime confirmation:** if desired before release, run the app once on an API 24 or 25 emulator to tick the first two acceptance boxes empirically.
