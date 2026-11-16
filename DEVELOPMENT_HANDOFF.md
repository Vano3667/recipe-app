# DEVELOPMENT_HANDOFF.md

Persistent continuation guide for AI coding agents working on this repository.
Read this file first, then `AGENT.md` (older notes from the first agent, in Russian; its "commit to main" rule is superseded below).
Last updated: 2026-11-17 (session of agent #4 "Billy", recovery + documentation only, no code changed).

---

## A. Project overview

| Item | Value |
|---|---|
| Project | "Домашнее меню" (Home Menu), offline Android app: recipes, weekly menu, shopping list. UI is Russian only. |
| Repository | `Vano3667/recipe-app` (private). Default branch `main`. Work branch `modernization`. |
| Language | Kotlin 1.9.24 |
| UI | Jetpack Compose (BOM 2024.06.00), Material 3, compose compiler 1.5.14 |
| Architecture | MVVM, layers `data` / `domain` / `ui`, Hilt 2.51.1 DI, Navigation Compose 2.7.7, Coroutines/Flow |
| Database | Room 2.6.1 (KSP). DB file `home_menu.db`. Schema version on `modernization`: **2** (main: 1) |
| Serialization | kotlinx.serialization 1.6.3 (backup JSON) |
| Images | Coil 2.6.0; photos stored as files in `filesDir/photos`, DB keeps only file names |
| Build | Gradle 8.7 (wrapper is NOT committed; CI runs `gradle wrapper --gradle-version 8.7`). JDK 17. minSdk 29, target/compile 34 |
| Application ID / namespace | `com.example.homemenu` (must never change) |
| Version | `versionCode = 1`, `versionName = "1.0"` (app/build.gradle.kts, unchanged since first upload) |
| Signing | **No signingConfig in Gradle.** CI only builds `assembleDebug`, signed with the runner's auto-generated debug keystore (see F2, critical). |
| CI | `.github/workflows/android.yml`: job `build` (assembleDebug, testDebugUnitTest incl. Robolectric UI tests, lintDebug with abortOnError) + job `instrumented` (connectedDebugAndroidTest on emulators API 29 pixel_4 and API 34 pixel_tablet). Artifacts: `home-menu-debug-apk`, `reports`, `instrumented-reports-29/34`. |
| Other docs | `README.md`, `DESIGN.md`, `TEST_REPORT.md` (last verified results, run #10), `ENVIRONMENT.md`, `CREDITS.md`, `AGENT.md` |

### Working rules (apply to every agent)
- Agents have no local JDK/Gradle/Android SDK. Code is changed through the GitHub API; **CI is the only build and test environment**. After every code push, wait for the workflow run and read its job logs. **If you cannot read CI results, you cannot verify anything (see F6).**
- Commit to `modernization`, not `main`. Merge to `main` only through a pull request after CI is green.
- Docs-only commits get `[skip ci]`. Multi-part code changes: intermediate parts `[skip ci]`, last part without it, so CI runs once on the complete change.
- Never force-push, never rewrite history, never commit keystores, passwords or tokens.
- Tooling note: the GitHub "get file contents" tool returns only the blob SHA, not the file body (confirmed again by agent #4). Reconstruct file bodies from commit diffs (`get_commit` with `full_patch`; upload commit `87a602d` contains every original file in full; later commits contain the diffs). Large single-file writes may be rejected: split big files.

---

## B. Original task specification (checklist)

1. [ ] Bulk ingredient input (paste many lines, parse name/amount/unit, keep order, edit/delete each, keep single-add, prevent duplicates)
2. [ ] Tags redesign (multi-select at bottom of recipe form, create via separate action, dedupe) and complete removal of cuisine classification
3. [ ] Multiple recipes per meal (breakfast/lunch/dinner/snack; add, remove, replace, reorder; safe migration)
4. [ ] Complete ZIP and JSON export/import (all data, images, manifest, version; validate, preview, duplicates Skip/Update/Separate, transactional, verify, report)
5. [ ] Recipe sharing (readable text: clipboard, Sharesheet, TXT) and text-based recipe import with editable preview
6. [ ] Fix the existing import bug (requires the user's original failing file)
7. [ ] Six predefined themes + custom colors (Settings > Appearance, persistent, contrast, reset)
8. [ ] Safe APK updates (same applicationId, same signing key, versionCode increments, data preserved, Settings > About)

Supporting: DB migration tested against a real v1 DB; data preservation across updates; UI consistency; tests (unit, Robolectric, instrumented, migration, import/export); release APK.

---

## C. Development status

Statuses: NOT STARTED / IN PROGRESS / IMPLEMENTED / TESTED / VERIFIED / BLOCKED.

**Global state:** `modernization` @ `5be7b4d` contains agent #3's compile fix for the main sources (7 commits, see E). Its CI result is **UNKNOWN** to agent #4 (F6). Test sources other than `BackupJsonTest.kt` were NOT updated and may still reference removed APIs (`cuisine`, `ImportResult`, `exportTo`, `importFrom`, `MenuDao.upsert/find`), so `testDebugUnitTest` / `connectedDebugAndroidTest` may fail even if `assembleDebug` passes. Nothing on `modernization` is verified. `main` @ `2ceef75` is the last verified version.

### Task 1 — Bulk ingredient input: IN PROGRESS (groundwork only)
- Done: `MeasureUnit.TO_TASTE` ("по вкусу", stored as 0); case-insensitive product dedupe + bulk product lookup (`aa8edfe`).
- Missing: pure parser `domain/IngredientLineParser.kt` + unit tests, bulk-paste dialog with editable preview in `RecipeEditScreen.kt`, duplicate protection.

### Task 2 — Tags + cuisine removal: IMPLEMENTED (unverified)
- Done: cuisine removed from domain (`047d9e6`), edit-state (`5961b4f`), detail screen (`4328ad3`), edit form (`5be7b4d`). Tags multi-select dropdown with checkboxes at the bottom of the form, selected tags as removable chips, separate "new tag" action; dedupe in `RecipeRepository.addTag` (`5be7b4d`). DB column `recipes.cuisine` kept as legacy.
- To check: `domain/RecipeSearch.kt`, `ui/recipes/RecipesScreen.kt`, `SettingsScreen.kt`, seed data and all tests for remaining cuisine references; grep the whole tree (via diffs) for `cuisine`/`кухн`.

### Task 3 — Multiple recipes per meal: IN PROGRESS (data layer only)
- Done (`047d9e6`, `aa8edfe`): `menu_entries.position`, non-unique `(date, slot)` index, DAO + repository add/replace/reorder/move.
- Missing: multi-dish UI in `ui/menu/MenuScreen.kt` / `MenuViewModel.kt`; check `MenuGeneratorUseCase` and `ShoppingListUseCase` for one-dish assumptions; tests; migration test.

### Task 4 — ZIP/JSON export/import: IMPLEMENTED (unverified)
- Data layer (`862a437`, `1bec32b`): backup format v2 (manifest, SHA-256, Base64 images in JSON, v1 readable), `BackupAnalyzer`, `BackupManager.exportZip/exportJson/stage/commit/discard` (one transaction, photo rollback, read-back verification).
- UI (`94891a7` SettingsViewModel, `16c5dfb` SettingsScreen): export ZIP and JSON, staged import with preview, duplicate mode, report, discard on cancel.
- Tests: `BackupJsonTest` updated for v2 fields, multi-dish menu, legacy v1 cuisine file (`1a764a1`). Missing: round trip with photos, failed-import rollback test, instrumented import with DB read-back.

### Task 5 — Sharing + text import: IN PROGRESS
- Done (`a8b6896`): `ui/recipes/RecipeDetailRoute.kt` with readable share text (description, prep time, oven temp, tags, kcal) and copy to clipboard. Existing: single-recipe `.zip` share via FileProvider, `writeText` helper.
- Missing: confirm Sharesheet `ACTION_SEND text/plain`, TXT export via `CreateDocument`, text recipe parser + editable preview, TXT file import, incoming share intent (optional), tests. Move share-text formatting into a pure domain function for unit tests.

### Task 6 — Existing import bug: BLOCKED — WAITING FOR USER FILE
- Original failing file not provided. Do not report as fixed. When it arrives: reproduce in a unit test against `BackupManager.stageStream` / `BackupJson.decode`, find root cause, fix, verify DB persistence and photos.

### Task 7 — Six themes + custom colors: IN PROGRESS (storage only)
- Done (`047d9e6`): `user_settings.themeId` (default `LEGACY`), `user_settings.customColors` (JSON), included in backup v2.
- Missing: `AppThemeId` enum + 6 palettes, theme provider in `Theme.kt`/`MainActivity.kt`, Settings > Appearance with previews, color picker, contrast check, reset, tests. Keep existing `ThemeMode` and test `darkThemeCanBeSelected` working.

### Task 8 — Safe APK updates: IN PROGRESS (About only), signing BLOCKED
- Done (`16c5dfb`): build number shown in About (verify it uses BuildConfig, and add build date via a `buildConfigField`).
- Missing: release signingConfig from CI secrets / git-ignored `keystore.properties`, release job, versionCode/versionName bump, update-over-install test. See F2.

### Supporting — DB migration 1->2: IMPLEMENTED (unverified)
- `MIGRATION_1_2` additive only; no destructive fallback; `DatabaseSafety.kt` copies DB to `filesDir/db_backups` before upgrade. Risk: `app/schemas/` not committed, no `MigrationTestHelper` test (F4).

---

## D. Technical decisions

| Decision | Reason | Commit |
|---|---|---|
| Keep legacy `recipes.cuisine` column, remove cuisine only from domain/UI | No table rebuild; old data recoverable | 047d9e6 |
| Multi-dish meals = several `menu_entries` rows per (date, slot) ordered by `position` | Additive migration; old meals become one dish at position 0 | 047d9e6 |
| No FK menu_entries -> recipes | Inherited design | original |
| Pre-migration byte copy of the DB | Safety net if a migration fails | 047d9e6 |
| Backup v2: ZIP = `recipes.json` + `photos/` + `manifest.json` (SHA-256); JSON = same + Base64 `images` | Self-contained, integrity-checked; v1 still readable | 862a437, 1bec32b |
| Two-phase import: stage -> preview -> commit in one transaction -> read-back | No partial imports, no silent overwrite | 1bec32b |
| Duplicate detection by normalized recipe name | Ids differ between devices | 862a437 |
| Imported photos get new UUID names | Never overwrite device photos | original |
| Theme stored in DB `user_settings` | Survives updates, included in backup | 047d9e6 |
| `TO_TASTE` unit stored with amount 0 | "по вкусу" lines from bulk/text import | 047d9e6 |
| Recipe detail split into `RecipeDetailScreen.kt` (content) + `RecipeDetailRoute.kt` (route, share, delete dialog) | GitHub API rejects large single-file writes | 4328ad3, a8b6896 |
| Pending: signing | Needs user input (F2) | — |
| Pending: versioning | Proposal: `versionCode` +1 per release; `versionName` 1.0 -> 1.1.0 for the modernization release | — |

---

## E. Git history and recovery point

- Last verified commit: `2ceef75` on `main` (CI #11 green; #12 green on modernization at the same code).
- `modernization` head before this docs commit: `5be7b4d`.
- Agent #1 (2026-11-16, interrupted): `047d9e6` data v2 + migration, `aa8edfe` repositories, `862a437` backup format v2, `1bec32b` BackupManager — CI #13–#16 failed at `compileDebugKotlin`.
- Agent #2: `44d94bf` this handoff file (docs only).
- Agent #3 (2026-11-16, compile fix, did not update this file):
  - `94891a7` SettingsViewModel: exportZip/exportJson, stage -> confirm(DuplicateMode) -> commit -> ImportReport, discard
  - `5961b4f` RecipeViewModels: cuisine dropped from edit state, v2 fields carried through
  - `1a764a1` BackupJsonTest: v2 fields, multi-dish menu, legacy v1 cuisine file
  - `4328ad3` RecipeDetailScreen: content without cuisine; description, prep, oven, copy action
  - `a8b6896` RecipeDetailRoute.kt (new): route, readable share text, clipboard, delete dialog
  - `16c5dfb` SettingsScreen: staged import UI, ZIP + JSON export, build number in About
  - `5be7b4d` RecipeEditScreen: no cuisine, v2 fields, tags multi-select at bottom — **CI triggered, result unknown to agent #4**
- Agent #4 (2026-11-17): this docs commit only.
- Open pull requests: unknown (agent #4 credential cannot list PRs).
- Latest built APK: debug artifact of CI #11/#12 (`2ceef75`, versionCode 1, ephemeral debug key). No release APK ever built.

---

## F. Known issues and blockers

### F1. Compile state of `modernization`
Agent #3 addressed every error listed for CI #16 (RecipeDetailScreen, RecipeEditScreen, RecipeViewModels, SettingsViewModel). Result of the CI run on `5be7b4d` not yet read. Test sources still to update: `app/src/test/.../Fixtures.kt`, `ui/LogicTest.kt`, `ui/ComposeScreensTest.kt`, `app/src/androidTest/.../AppFlowsTest.kt`.

### F2. APK signing — CRITICAL for Task 8 (BLOCKED, needs user)
- No release signing config; every CI APK is signed with a new random debug key, so CI APKs normally cannot be installed over each other.
- If the installed app came from a CI artifact, its key is lost: one-time uninstall/reinstall is unavoidable. **Before that, export a backup ZIP from the installed app**, then reinstall and import.
- Plan: one permanent release keystore held by the user, stored only as GitHub Secrets (`KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`); `signingConfigs.release` reads env / git-ignored `keystore.properties`; CI job `assembleRelease`.
- Awaiting user: how the current app was installed, and whether they hold a keystore.

### F3. Import bug file — BLOCKED (Task 6)
Waiting for the user's original file that fails to import.

### F4. Room schemas not committed
`app/schemas/` missing; needed for a 1->2 `MigrationTestHelper` test. Generate v1 schema from `2ceef75` in CI (upload as artifact), commit `app/schemas/com.example.homemenu.data.db.AppDatabase/1.json`, add `room-testing` and androidTest assets srcDir.

### F5. `AGENT.md` outdated (describes 2026-11-11). This file supersedes it.

### F6. CI results not readable — BLOCKS ALL VERIFICATION (new, agent #4)
- The GitHub connection used by agent #4 returns `403 Resource not accessible by personal access token` for Actions (workflow runs, job logs) and Pull requests. Contents and commits are readable.
- Effect: the CI result of `5be7b4d` is unknown, and no further code change can be verified. Pushing more code blind on top of an unknown compile state risks stacking errors.
- Unblock: give the token behind the GitHub connection these repository permissions for `Vano3667/recipe-app`: **Actions: Read**, **Pull requests: Read and write**, **Commit statuses: Read** (plus existing Contents: Read and write, Metadata: Read). Then reconnect GitHub in the agent. Alternative: the user pastes the failing step log of the latest CI run.
- Independent work possible meanwhile: pure, self-contained domain code with unit tests (e.g. `IngredientLineParser`), pushed with `[skip ci]` and verified once CI is readable.

---

## G. NEXT AGENT — START HERE

1. **Verified state:** `main` @ `2ceef75` builds and passes all tests. `modernization` (head = this docs commit, code head `5be7b4d`) has data-layer work for tasks 2/3/4/7, migration 1->2, and agent #3's UI compile fix. **Nothing on `modernization` is verified.**
2. **First action:** confirm you can read GitHub Actions (list workflow runs for branch `modernization`). If you get 403, F6 still applies: ask the user to grant Actions/PR read, do not push untested UI code.
3. **Then:** read the CI run for `5be7b4d` (`get_job_logs` with `failed_only`). Fix compile errors first, then test-source errors (F1 list), until job `build` and job `instrumented` are green. Update `TEST_REPORT.md`.
4. **Then, in order:** F4 migration test -> Task 3 multi-dish UI -> Task 1 bulk ingredients (parser + dialog) -> Task 2 leftover cuisine grep -> Task 4 round-trip/rollback tests -> Task 5 Sharesheet/TXT/text import -> Task 7 themes -> Task 8 signing (after user answer) -> Task 6 (after user file).
5. **Files to inspect first:** `ui/recipes/RecipeEditScreen.kt`, `ui/recipes/RecipeDetailRoute.kt`, `ui/settings/SettingsScreen.kt`, `ui/settings/SettingsViewModel.kt`, `ui/menu/MenuScreen.kt`, `ui/menu/MenuViewModel.kt`, `domain/RecipeSearch.kt`, all files under `app/src/test` and `app/src/androidTest`.
6. **Blockers:** F6 (CI visibility), F2 (signing, user), F3 (import file, user).
7. **Precautions:** never change `applicationId`; never add `fallbackToDestructiveMigration`; keep legacy `cuisine` column; do not merge into `main` until CI is green and migration 1->2 is tested on a real v1 DB; never commit keystores, passwords or tokens.
