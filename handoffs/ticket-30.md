# Handoff — ticket #30

**Ticket:** #30 — [P2][Chore] Dead code, split intro packages, and a stray okhttp import

## Summary
The onboarding intro was split across two similarly-named packages: `ui.introduction` held the screen (`IntroductionScreen`), while `ui.intro` held its ViewModel and data in a file misleadingly named `IntroScreen.kt` (which contains **no composable**). The screen reached across packages to import `IntroViewModel`, `introPages`, and `IntroEvent`. This change consolidates everything into `ui.introduction`: the ViewModel/data file is moved there and renamed `IntroViewModel.kt` so its name matches its contents, and the three now-redundant cross-package imports are dropped from the screen. Pure reorganization — no behaviour change. The ticket's other two items were already resolved and needed no code change (see Deviations).

## Files changed

**Intro package consolidation**
- `app/src/main/java/com/humblesolutions/humblecontacts/ui/intro/IntroScreen.kt` → **renamed** to `app/src/main/java/com/humblesolutions/humblecontacts/ui/introduction/IntroViewModel.kt` — moved into the used package (`package` line updated to `…ui.introduction`); git tracks it as a rename (98% similarity), preserving history. Holds `IntroViewModel`, `introPages`, `IntroPage`, `IntroUiState`, `IntroEvent` — the name now matches the contents.
- `app/src/main/java/com/humblesolutions/humblecontacts/ui/introduction/IntroductionScreen.kt` — removed the three now-redundant imports (`ui.intro.IntroEvent`, `ui.intro.IntroViewModel`, `ui.intro.introPages`); these types are in the same package now.
- The `ui/intro/` package directory is now empty and removed.

`AppNavGraph.kt` was **not** changed — it only imports `IntroductionScreen`, which did not move.

## How to test
**Automated (required):**
```
./gradlew :app:compileDebugKotlin :app:testDebugUnitTest
```
Both pass. For a package move + import removal, a green compile is the decisive check — a broken reference or a still-needed import would fail to compile. The unit suite (no intro coverage) confirms nothing else regressed.

**Manual smoke (recommended, not a blocker):** the intro flow has no automated UI tests, and behaviour is unchanged, but if verifying on a device/emulator:
1. Fresh first-run launch → the 3-page intro carousel appears.
2. Swipe through all 3 pages → copy renders, page indicator advances.
3. Tap **Get Started** on the last page (and **Skip** if present) → navigates to auth/login.

## Acceptance criteria
- [x] **Intro lives in a single, coherent package** — ViewModel/data moved into `ui.introduction`; `ui.intro` removed; file renamed to match its contents.
- [x] **Unused `IntroScreen` composable removed** — N/A: no such composable existed. `IntroScreen.kt` was a misnamed file holding the (in-use) ViewModel/data; renaming it resolves the confusion the criterion targeted. Nothing was dead code.
- [x] **`import okhttp3.Address` removed; no okhttp reference remains** — already absent on `main` before this ticket; verified no `okhttp` reference anywhere in `app/src`. No change needed.
- [x] **All intro navigation/references updated; app builds and intro still works** — imports updated, `AppNavGraph` needs no change, build green.

## Deviations / decisions
- **Two of the three ticket items were already moot.** The ticket (a QA-finding stub) assumed an unused `IntroScreen` *composable* and a stray `okhttp3.Address` import. Neither exists in the current code: the file named `IntroScreen.kt` never contained a composable (just the ViewModel + data, all used), and no okhttp import remains on `main`. Only the package-split item was genuine. Documented rather than fabricating changes.
- **`git mv` used** to preserve file history across the rename/move.
- **Filename chosen as `IntroViewModel.kt`** to match the primary class; the file's existing header comment already read "IntroViewModel.kt", so name, header, and contents now agree.

## Open questions / follow-ups
- The ticket flagged coordination with **P1-7** (reminders removal, which also edits intro carousel copy). P1-7 is not in the open-issue list, so it appears already merged; this branch is off the latest `main`, so no conflict is expected. Worth a glance if P1-7 is still in flight.
- No automated UI test coverage exists for the intro flow (pre-existing gap); a Compose UI test could be a future addition but is out of scope for this chore.
