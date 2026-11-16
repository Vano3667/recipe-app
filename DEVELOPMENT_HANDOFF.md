# DEVELOPMENT_HANDOFF.md

Persistent continuation guide for AI coding agents working on this repository.
Read this file first, then `AGENT.md` (older notes from the first agent, in Russian; its "commit to main" rule is superseded below).
Last updated: 2026-11-17 (recovery session, agent #2).

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
| Signing | **No signingConfig in Gradle.** CI only builds `assembleDebug`, signed with the runner's auto-generated debug keystore (see section F, critical). |
| CI | `.github/workflows/android.yml`: job `build` (assembleDebug, testDebugUnitTest incl. Robolectric UI tests, lintDebug with abortOnError) + job `instrumented` (connectedDebugAndroidTest on emulators API 29 pixel_4 and API 34 pixel_tablet). Artifacts: `home-menu-debug-apk`, `reports`, `instrumented-reports-29/34`. |
| Other docs | `README.md` (features), `DESIGN.md` (palette, fonts, tokens), `TEST_REPORT.md` (last verified test results, run #10), `ENVIRONMENT.md`, `CREDITS.md`, `AGENT.md` |

### Working rules (apply to every agent)
- Agents have no local JDK/Gradle/Android SDK. Code is changed through the GitHub API; **CI is the only build and test environment**. After every code push, wait for the workflow run and read its job logs.
- Commit to `modernization`, not `main`. Merge to `main` only through a pull request after CI is green.
- Docs-only commits get `[skip ci]` in the message.
- Never force-push, never rewrite history, never commit keystores or passwords.
- Tooling note (agent #2 environment): the GitHub "get file contents" tool returned only the blob SHA, not the file body. File bodies were reconstructed from commit diffs (`get_commit` with full patch; the upload commit `87a602d` contains every original file in full). If your environment can read files directly, prefer that.

---

## B. Original task specification (checklist)

1. [ ] Bulk ingredient input (paste many lines, parse name/amount/unit, keep order, edit/delete each, keep single-add, prevent duplicates)
2. [ ] Tags redesign (multi-select at bottom of recipe form, create via separate action, dedupe) and complete removal of cuisine classification from all screens
3. [ ] Multiple recipes per meal (breakfast/lunch/dinner/snack; add, remove, replace, reorder; per-day; safe migration)
4. [ ] Complete ZIP and JSON export/import (all data, images, manifest, format version; validate, preview, duplicates Skip/Update/Separate, transactional, verify, report)
5. [ ] Recipe sharing (readable text: clipboard, Sharesheet, Telegram, WhatsApp, email, TXT) and text-based recipe import with editable preview
6. [ ] Fix the existing import bug (requires the user's original failing file)
7. [ ] Six predefined themes + custom colors (Settings → Appearance, persistent, contrast checks, reset)
8. [ ] Safe APK updates (same applicationId, same signing key, versionCode increments, data preserved, Settings → About)

Supporting requirements:
- [ ] Database migrations tested against a real v1 database (not only clean install)
- [ ] Data preservation across updates (recipes, ingredients, images, tags, menus, settings, themes)
- [ ] UI/UX consistency
- [ ] Tests (unit, Robolectric, instrumented, migration, import/export)
- [ ] Release APK build and delivery

---

## C. Development status

Statuses: NOT STARTED / IN PROGRESS / IMPLEMENTED — NOT VERIFIED / TESTING / COMPLETED AND VERIFIED / BLOCKED.

**Global state: the `modernization` branch does not compile** (CI runs #13–#16 failed at `:app:compileDebugKotlin`). Nothing on `modernization` is verified. `main` is the last working version.

### Task 1 — Bulk ingredient input: IN PROGRESS (groundwork only)
- Done: `MeasureUnit.TO_TASTE` ("по вкусу", amountless, stored as 0) in `domain/model/Models.kt`; `ScaledAmount.format` handles it. Repositories commit `aa8edfe` adds case-insensitive product dedupe and a bulk product lookup.
- Missing: text parser (pure, unit-tested), bulk-paste dialog in `ui/recipes/RecipeEditScreen.kt`, duplicate protection, tests.
- Next action: add `domain/IngredientLineParser.kt` + `IngredientLineParserTest`, then the dialog.

### Task 2 — Tags + cuisine removal: IN PROGRESS (domain done, UI broken)
- Done (`047d9e6`): `cuisine` removed from domain `Recipe`; `DEFAULT_CUISINES` deleted; DB column `recipes.cuisine` intentionally kept as legacy (no table rebuild, old values preserved); Seeder no longer writes cuisine; importer keeps the old cuisine value on UPDATE. Tag dedupe case-insensitive in repository.
- Broken: UI still references cuisine (compile errors listed in section F).
- Missing: remove cuisine from edit form, detail screen, search/filters (`domain/RecipeSearch.kt`, `ui/recipes/RecipesScreen.kt` must be checked), settings; tag multi-select component at the bottom of the form; "new tag" action; tests.

### Task 3 — Multiple recipes per meal: IN PROGRESS (data layer done, not verified)
- Done (`047d9e6`, `aa8edfe`): `menu_entries.position` column; `(date, slot)` index made non-unique; `MenuDao.getSlot/insert/insertAll/updateAll/count` (old `find`, `upsert`, `insertAllIgnore` removed); `MenuEntry.position`; repository methods for add / replace / reorder / move dish.
- Not verified: whether `ui/menu/MenuScreen.kt` and `MenuViewModel.kt` show and edit several dishes per meal (they compile, but UI support for multiple dishes is not confirmed). `MenuGeneratorUseCase` and `ShoppingListUseCase` must be checked for the one-dish assumption.
- Missing: multi-dish UI (list per meal, add, remove, replace, reorder), tests, migration test.

### Task 4 — ZIP/JSON export/import: IMPLEMENTED — NOT VERIFIED (data layer only)
- Done (`862a437`, `1bec32b`): backup format v2 in `domain/backup/BackupFormat.kt` (manifest, Base64 images for JSON, menu positions, recipe description/prepTime/ovenTemp, tags with isCustom, theme settings; v1 files still readable), pure analyzer `domain/backup/ImportAnalysis.kt` (`BackupAnalyzer`, `ImportPreview`, `ImportReport`, `DuplicateMode`).
  `data/files/BackupManager.kt`: `exportZip(uri)`, `exportJson(uri)`, `recipeShareFile(recipe)`, `writeText(uri, text)`, `stage(uri)` → `StagedImport` (ZIP or JSON autodetect, BOM strip, size limits, zip-slip guard, SHA-256 manifest check, image magic-byte check, preview), `commit(staged, mode)` (one Room transaction, fresh photo names, rollback of copied photos, read-back verification), `discard(staged)`.
- Broken: `ui/settings/SettingsViewModel.kt` still calls removed `exportTo` / `importFrom`.
- Missing: Settings UI (export ZIP / export JSON, pick file → preview dialog → duplicate mode → confirm → report), unit tests for v2 round trip, Robolectric/instrumented import test with real DB read-back, example ZIP/JSON in `docs/`.
- Review notes: `existingByKey()` queries names twice with a redundant fallback (harmless, simplify). `ExportResult` replaced old `ImportResult`.

### Task 5 — Sharing + text import: NOT STARTED
- Existing on main: single-recipe share as importable `.zip` via FileProvider (`recipeShareFile`). `writeText` helper exists for TXT export.
- Missing: readable text formatter (pure), Share button actions (clipboard, Sharesheet `ACTION_SEND text/plain` covers Telegram/WhatsApp/email, TXT via `CreateDocument`), text recipe parser + editable preview, TXT file import, tests.

### Task 6 — Existing import bug: BLOCKED — WAITING FOR USER FILE
- The user's original failing file has not been provided. The new staged importer adds tolerance (any `.json` inside a ZIP, BOM, plain JSON), but **the original bug is not reproduced and must not be reported as fixed**.
- Next action: when the file arrives, reproduce with a unit test against `BackupManager.stageStream` / `BackupJson.decode`, find root cause, add converter, verify DB persistence.

### Task 7 — Six themes + custom colors: IN PROGRESS (storage only)
- Done (`047d9e6`): `user_settings.themeId` (default `LEGACY` = current look) and `user_settings.customColors` (JSON, nullable); exported/imported in backup v2.
- Missing: `AppThemeId` enum + 6 palettes in `ui/theme/`, theme provider in `Theme.kt`/`MainActivity.kt`, Settings → Appearance with previews, color picker with contrast check, reset, tests. Existing `ThemeMode` (SYSTEM/LIGHT/DARK) and test `darkThemeCanBeSelected` must keep working or be migrated deliberately.

### Task 8 — Safe APK updates: NOT STARTED (critical blocker, see F)
- Missing: release signing config read from CI secrets / local `keystore.properties` (git-ignored), release build job, versionCode/versionName bump policy, Settings → About (BuildConfig.VERSION_NAME / VERSION_CODE / build time), update-over-install test.

### Supporting — Database migration: IMPLEMENTED — NOT VERIFIED
- `AppDatabase.VERSION = 2`, `MIGRATION_1_2` (additive only: ALTER TABLE ADD COLUMN ×5, index swap). No destructive fallback. `data/db/DatabaseSafety.kt` copies the old DB file to `filesDir/db_backups` (keeps 5) before Room upgrades it; called in `di/AppModule.kt`.
- Risk: `app/schemas/` is **not committed**, so there is no exported v1 schema JSON and no `MigrationTestHelper` test yet. Room column default values (`'LEGACY'`, `''`, `0`) in the migration SQL must match what Room generates from the `@ColumnInfo(defaultValue=...)` annotations, otherwise Room throws "Migration didn't properly handle" on first launch after update. Only a migration test proves this.

---

## D. Technical decisions

| Decision | Reason | Commit |
|---|---|---|
| Keep legacy `recipes.cuisine` column, remove cuisine only from domain/UI | Avoid table rebuild in migration; keep old data recoverable | 047d9e6 |
| Multi-dish meals = several `menu_entries` rows per (date, slot) ordered by `position`; non-unique index | Additive migration; every old meal becomes a meal with one dish at position 0 | 047d9e6 |
| No FK from menu_entries to recipes | Inherited design: deleting a scheduled recipe asks the user to clear its slots | original |
| Pre-migration byte copy of the DB | Extra safety net for user data if a migration ever fails | 047d9e6 |
| Backup format v2: ZIP = `recipes.json` + `photos/` + `manifest.json` (SHA-256 per file); JSON = same data + Base64 `images` map | Self-contained, integrity-checked; JSON restorable without companion files; v1 still readable | 862a437, 1bec32b |
| Import is two-phase: stage (no DB writes) → preview → commit in one transaction → read-back verification | Never partially import, never silently overwrite | 1bec32b |
| Duplicate detection by normalized recipe name (`BackupAnalyzer.key`) | Ids differ between devices | 862a437 |
| Imported photos always get new UUID names | Never overwrite photos on the device | original, kept |
| Theme stored in DB (`user_settings`), not SharedPreferences | Survives updates, is included in backup | 047d9e6 |
| `TO_TASTE` unit stored with amount 0 | "по вкусу" ingredients from bulk/text import | 047d9e6 |
| Pending decision: signing | Requires user input (section F) | — |
| Pending decision: versioning | Proposal: `versionCode` +1 every release, `versionName` semver 1.0 → 1.1.0 for the modernization release | — |

---

## E. Git history and recovery point

- Last verified commit: `2ceef75` on `main` (CI run #11 green on main, #12 green on modernization: build, unit, Robolectric, lint, instrumented API 29 + 34).
- Current branch: `modernization` @ `1bec32b` (+ this docs commit).
- Modernization commits (2026-11-16, agent #1, interrupted):
  - `047d9e6` Data v2: several dishes per meal, recipe description/prep time/oven temp, theme columns, safe 1→2 migration with pre-migration DB backup; cuisine removed from domain — CI #13 failed
  - `aa8edfe` Repositories: multi-dish meals, case-insensitive tag/product dedupe, bulk product lookup — CI #14 failed
  - `862a437` Backup format v2 + import analyzer — CI #15 failed
  - `1bec32b` BackupManager full ZIP/JSON export, staged import — CI #16 failed
- Earlier milestones on main: `f4f4043` photo import fixed (CI #10 green), `b1b4ab6` CI #9 green, `87a602d` first full upload.
- Open pull requests: none.
- Uncommitted changes: unknown/inaccessible. Agent #1 worked directly through GitHub; any local sandbox state from that session is not reachable. Everything recoverable is in the commits above.
- Latest successfully built APK: debug APK artifact of CI run #11/#12 (commit `2ceef75`, versionCode 1, debug-signed with an ephemeral CI key). No release APK has ever been built.
- Latest test results: run #10–#12 green (see `TEST_REPORT.md`). Runs #13–#16: compile failure, no tests executed.

---

## F. Known issues and blockers

### F1. Compile errors on `modernization` (CI run #16, `1bec32b`)
```
ui/recipes/RecipeDetailScreen.kt:240  Unresolved reference: cuisine (shows cuisine on detail screen)
ui/recipes/RecipeEditScreen.kt:63     Unresolved reference: DEFAULT_CUISINES
ui/recipes/RecipeEditScreen.kt:298    DEFAULT_CUISINES.forEach { ... } (cuisine chips in edit form)
ui/recipes/RecipeViewModels.kt:141    Cannot find a parameter with this name: cuisine (Recipe(...) constructor)
ui/recipes/RecipeViewModels.kt:153    Unresolved reference: cuisine (edit state from recipe)
ui/settings/SettingsViewModel.kt:39   Unresolved reference: exportTo  (now exportZip / exportJson)
ui/settings/SettingsViewModel.kt:44   Unresolved reference: importFrom (now stage → preview → commit / discard)
```
Kotlin stops at the first failing module; after these are fixed, test sources (`Fixtures.kt`, `BackupJsonTest.kt`, `LogicTest.kt`, `ComposeScreensTest.kt`, `AppFlowsTest.kt`) may also reference `cuisine`, `ImportResult`, `exportTo`, `importFrom`, `MenuDao.upsert/find` and need updating.

### F2. APK signing — CRITICAL for Task 8
- There is no release signing configuration. Every CI run builds a debug APK signed with a **new random debug keystore** generated on the runner. Two CI APKs therefore normally have different signatures, and Android refuses to install one over the other.
- If the user's phone has an APK from a CI artifact, its key is almost certainly lost. In that case a one-time uninstall/reinstall is unavoidable: **before that, the user must export a backup with the currently installed app** (Settings → backup ZIP, v1 format, readable by the new importer), then reinstall and import.
- Plan: create one permanent release keystore (by the user locally, or generated once in a workflow and stored only as GitHub Secrets `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`), add `signingConfigs.release` that reads env/`keystore.properties`, add a CI job `assembleRelease`. From then on every release updates in place.
- Awaiting user answer: how the current app was installed (CI artifact, local Android Studio build, or other) and whether they hold any keystore.

### F3. Import bug file — BLOCKED
Waiting for the user's original file that fails to import.

### F4. Room schemas not committed
`ksp { arg("room.schemaLocation", "$projectDir/schemas") }` is set but `app/schemas/` is not in git. To write a 1→2 migration test, the v1 schema JSON must be generated from commit `2ceef75` (e.g. a CI step that uploads `app/schemas/` as an artifact, then commit `app/schemas/com.example.homemenu.data.db.AppDatabase/1.json`), and `androidTestImplementation("androidx.room:room-testing:2.6.1")` + `sourceSets["androidTest"].assets.srcDir("$projectDir/schemas")` added.

### F5. Agent #1 left no documentation of the modernization work
`AGENT.md` still describes the state of 2026-11-11 (main, photo fix). This file supersedes it.

---

## G. NEXT AGENT — START HERE

1. **Verified state:** `main` @ `2ceef75` builds and passes all tests. `modernization` @ `1bec32b` contains data-layer work for tasks 2, 3, 4, 7 and the DB v2 migration, but **does not compile**. Nothing on `modernization` is verified.
2. **Most important unfinished task:** make `modernization` compile and pass CI again (F1), without reverting the data-layer work.
3. **Files to inspect next:**
   - `app/src/main/java/com/example/homemenu/ui/recipes/RecipeDetailScreen.kt` (line ~240)
   - `app/src/main/java/com/example/homemenu/ui/recipes/RecipeEditScreen.kt` (lines ~63, ~298)
   - `app/src/main/java/com/example/homemenu/ui/recipes/RecipeViewModels.kt` (lines ~141, ~153)
   - `app/src/main/java/com/example/homemenu/ui/settings/SettingsViewModel.kt` (lines ~39, ~44) and `SettingsScreen.kt`
   - `app/src/main/java/com/example/homemenu/domain/RecipeSearch.kt`, `ui/recipes/RecipesScreen.kt` (cuisine filters)
   - all files under `app/src/test` and `app/src/androidTest`
4. **Next implementation action:**
   a. Remove cuisine UI (detail row, edit chips, edit-state field, filters).
   b. Rewire `SettingsViewModel`: export via `exportZip`; import via `stage` → keep `StagedImport` in state → preview dialog in `SettingsScreen` with duplicate mode (Skip / Update / Separate) → `commit` → show `ImportReport`; `discard` on cancel. Add "Export JSON" button (`exportJson`).
   c. Update tests that use removed APIs. Add `BackupV2RoundTripTest` (pure: `BackupJson`, `BackupAnalyzer`).
   d. Push, wait for CI, read logs, fix until green. Then update this file and `TEST_REPORT.md`.
5. **Required tests:** CI `build` job (assembleDebug, testDebugUnitTest, lintDebug) and `instrumented` job on API 29 and 34.
6. **Blockers:** F2 signing (user answer needed before any release APK), F3 import bug file.
7. **Precautions:** never change `applicationId`; never add `fallbackToDestructiveMigration`; keep the legacy `cuisine` column; do not merge into `main` until CI is green and migration 1→2 is tested against a real v1 database; never commit keystores or passwords.

Suggested order after the build is green: F4 migration test → Task 3 multi-dish UI → Task 2 tags multi-select → Task 1 bulk ingredients → Task 4 UI polish + examples → Task 5 → Task 7 → Task 8 (after user answer) → Task 6 (after user file).
