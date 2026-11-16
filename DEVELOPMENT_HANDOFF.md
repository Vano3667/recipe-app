# DEVELOPMENT_HANDOFF.md

Persistent continuation guide for AI coding agents working on this repository.
Read this file first, then `AGENT.md` (older notes, Russian; its "commit to main" rule is superseded).
Last updated: 2026-11-16 15:58 UTC (agent #4 "Billy").

---

## A. Project overview

| Item | Value |
|---|---|
| Project | "Домашнее меню" (Home Menu), offline Android app: recipes, weekly menu, shopping list. UI Russian only. |
| Repository | `Vano3667/recipe-app` (private). Default `main`, work branch `modernization`. |
| Stack | Kotlin 1.9.24, Jetpack Compose (BOM 2024.06.00) + Material 3, MVVM (`data`/`domain`/`ui`), Hilt 2.51.1, Navigation Compose 2.7.7, Coroutines/Flow |
| Database | Room 2.6.1 (KSP), file `home_menu.db`, schema **v2** on `modernization` (main: v1) |
| Other libs | kotlinx.serialization 1.6.3 (backup JSON), Coil 2.6.0 (photos in `filesDir/photos`, DB keeps file names) |
| Build | Gradle 8.7 (wrapper NOT committed; CI runs `gradle wrapper --gradle-version 8.7`), JDK 17, minSdk 29, target/compile 34 |
| applicationId | `com.example.homemenu` (must never change) |
| Version | `versionCode = 1`, `versionName = "1.0"` |
| Signing | No signingConfig. CI builds `assembleDebug` with the runner's random debug key (F2). |
| CI | `.github/workflows/android.yml`: job `build` (assembleDebug, testDebugUnitTest incl. Robolectric, lintDebug abortOnError) + job `instrumented` (connectedDebugAndroidTest, API 29 pixel_4 and API 34 pixel_tablet). ~4 min build, ~10 min instrumented. |
| Commands (CI) | `./gradlew assembleDebug testDebugUnitTest lintDebug`, `./gradlew connectedDebugAndroidTest` |
| Other docs | `README.md`, `DESIGN.md`, `TEST_REPORT.md` (results up to run #10), `ENVIRONMENT.md`, `CREDITS.md`, `AGENT.md` |

### Working rules
- No local JDK/Gradle/SDK. Code changes go through the GitHub API; **CI is the only build/test environment**. After each code push, wait for the run and read job logs.
- Commit to `modernization`; merge to `main` only via PR after CI is green.
- Docs-only commits: `[skip ci]`. Multi-part changes: intermediate parts `[skip ci]`, last part without it.
- Never force-push, rewrite history, or commit keystores/passwords/tokens.
- Tooling: "get file contents" returns only the blob SHA (use it as `sha` for updates). Read bodies from commit diffs (`get_commit` full_patch; `87a602d` = original upload of every file). `push_files` with 2 files of ~13 KB timed out: write one file per commit.
- Agent #4 credential: Actions READ works (runs, jobs, logs). Re-running jobs = 403, listing PRs = 403. To re-run a flaky job, push a new commit or ask the user.

---

## B. Task checklist

1. [ ] Bulk ingredient input
2. [ ] Tags redesign + full cuisine removal
3. [ ] Multiple recipes per meal
4. [ ] Complete ZIP and JSON export/import
5. [ ] Recipe sharing + text import
6. [ ] Fix existing import bug (needs user file)
7. [ ] Six themes + custom colors
8. [ ] Safe APK updates

Supporting: migration test on real v1 DB; data preservation; UI consistency; tests; release APK.

---

## C. Development status

Statuses: NOT STARTED / IN PROGRESS / IMPLEMENTED / TESTED / VERIFIED / BLOCKED.

**Global state:** `modernization` compiles and passes unit + Robolectric + lint (CI #17 on `5be7b4d`: all jobs green incl. instrumented API 29 and 34; CI #18 on `0fec6c8`: build job green, API 29 green, API 34 one UI test failed, see F7).

### Task 1 — Bulk ingredient input: IN PROGRESS
- TESTED: `domain/IngredientLineParser.kt` (`9da0cbc`) + `IngredientLineParserTest` 9 tests (`0fec6c8`, green in CI #18). Handles leading/trailing amounts, fractions, ranges (upper bound), unicode fractions, RU/EN unit synonyms, kg/l -> g/ml, "по вкусу" -> TO_TASTE, bullets/numbering; unknown lines kept whole (`recognized=false`); `duplicateIndices()` flags repeats in paste and vs existing names.
- Missing: bulk-paste dialog in `ui/recipes/RecipeEditScreen.kt` (paste -> preview list with editable name/amount/unit, delete row, duplicate marker -> "Add all"), mapping to the edit form's ingredient rows (products resolved by name via repository bulk lookup from `aa8edfe`), Robolectric test.

### Task 2 — Tags + cuisine removal: IMPLEMENTED
- Cuisine removed from domain (`047d9e6`), edit state (`5961b4f`), detail (`4328ad3`), edit form (`5be7b4d`). Tags multi-select at bottom, removable chips, separate "new tag" action, case-insensitive dedupe. Legacy DB column `recipes.cuisine` kept. Compiles and existing tests pass (CI #17).
- Missing: grep remaining `cuisine`/`кухн` references (RecipeSearch, RecipesScreen filters, seed, tests), tests for tag select/create/remove.

### Task 3 — Multiple recipes per meal: IN PROGRESS (data layer only)
- Done: `menu_entries.position`, non-unique `(date, slot)` index, DAO/repository add/replace/reorder/move.
- Missing: multi-dish UI in `ui/menu/MenuScreen.kt`/`MenuViewModel.kt`; check `MenuGeneratorUseCase`, `ShoppingListUseCase`; tests; migration test.

### Task 4 — ZIP/JSON export/import: IMPLEMENTED
- Format v2, staged import with preview/duplicate modes/single transaction/read-back (`862a437`, `1bec32b`); Settings UI (`94891a7`, `16c5dfb`); `BackupJsonTest` v2 (`1a764a1`). Compiles, tests pass.
- Missing: round trip with photos, failed-import rollback test, instrumented import read-back.

### Task 5 — Sharing + text import: IN PROGRESS
- Done: readable share text + clipboard in `ui/recipes/RecipeDetailRoute.kt` (`a8b6896`).
- Missing: Sharesheet `ACTION_SEND text/plain` check, TXT export, pure share formatter + tests, text recipe parser (reuse `IngredientLineParser` for the ingredient section) + editable preview, TXT import.

### Task 6 — Existing import bug: BLOCKED — WAITING FOR USER FILE

### Task 7 — Themes + custom colors: IN PROGRESS (storage only)
- Done: `user_settings.themeId` (default LEGACY), `customColors` JSON, in backup v2.
- Missing: 6 palettes, provider, Settings > Appearance, picker, contrast, reset, tests.

### Task 8 — Safe APK updates: IN PROGRESS, signing BLOCKED
- Done: build number in About (`16c5dfb`). Missing: build date field, release signing from secrets, versionCode bump, update-over-install test (F2).

### Supporting — Migration 1->2: IMPLEMENTED (no migration test yet, F4)

---

## D. Technical decisions

| Decision | Reason | Commit |
|---|---|---|
| Keep legacy `recipes.cuisine` column | No table rebuild, old data kept | 047d9e6 |
| Multi-dish meals = several `menu_entries` per (date, slot) by `position` | Additive migration | 047d9e6 |
| Pre-migration DB byte copy (`DatabaseSafety`) | Safety net | 047d9e6 |
| Backup v2 ZIP (`recipes.json` + `photos/` + `manifest.json` SHA-256) / JSON with Base64 images | Self-contained; v1 readable | 862a437, 1bec32b |
| Two-phase import, one transaction, read-back | No partial imports | 1bec32b |
| Duplicates by normalized recipe name | Ids differ between devices | 862a437 |
| Theme in DB `user_settings` | Survives updates, in backup | 047d9e6 |
| `TO_TASTE` amount 0 | "по вкусу" lines | 047d9e6 |
| Detail screen split into Screen + Route files | API rejects large writes | 4328ad3, a8b6896 |
| Bulk parser is pure domain code; number without unit = pieces; kg/l converted; range keeps upper bound; unknown lines kept, never dropped; duplicates only flagged | Testable without Android; no silent data loss | 9da0cbc |
| Pending: signing (F2); versioning proposal versionCode +1 per release, versionName 1.1.0 | — | — |

---

## E. Git history

- Last fully green: `5be7b4d` (CI #17, all jobs). `main` @ `2ceef75` green (CI #11).
- Agent #1: `047d9e6`, `aa8edfe`, `862a437`, `1bec32b` (data layer; CI #13–#16 failed compile).
- Agent #2: `44d94bf` handoff.
- Agent #3: `94891a7`, `5961b4f`, `1a764a1`, `4328ad3`, `a8b6896`, `16c5dfb`, `5be7b4d` (compile fix + UI; CI #17 green).
- Agent #4: `3c0fac1` handoff, `9da0cbc` parser, `0fec6c8` parser tests (CI #18: build green, API 29 green, API 34 F7), this docs commit.
- All pushes verified by the API response (new branch head SHA).

---

## F. Blockers and known issues

- **F2 Signing (Task 8, user):** no release key; CI APKs use random debug keys and cannot update each other. If the installed app came from CI, export a backup ZIP first, then reinstall. Plan: permanent keystore in GitHub Secrets (`KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`), `signingConfigs.release`, CI `assembleRelease`. Waiting: how the current app was installed; does the user have a keystore.
- **F3 Import bug file (Task 6, user):** waiting for the failing file.
- **F4 Room schemas not committed:** needed for a 1->2 `MigrationTestHelper` test. Generate v1 schema from `2ceef75` in CI, commit `app/schemas/.../1.json`, add `room-testing`.
- **F5** `AGENT.md` outdated; this file supersedes it.
- **F6 resolved:** CI logs are readable now. Re-run and PR endpoints still 403.
- **F7 Flaky instrumented test on API 34:** CI #18 `AppFlowsTest.addRecipeWithIngredient` failed on pixel_tablet API 34 ("Не появился элемент с тегом «product_search» за 15000 мс", AppFlowsTest.kt:51), with emulator GPU errors ("Failed to find ColorBuffer"). The same test passed on API 29 in #18 and on both APIs in #17, and #18 changed no app code (only a pure domain file + unit test). Likely emulator flakiness, NOT proven. Check the next run; if it fails again, inspect AppFlowsTest waits and the product search field on the tablet layout.

---

## G. NEXT AGENT — START HERE

1. **State:** `modernization` builds and passes unit/Robolectric/lint. Last fully green commit `5be7b4d`. Latest code commit `0fec6c8`.
2. **First:** read the latest CI run on `modernization`; confirm F7 does not repeat.
3. **Next action:** Task 1 UI. Reconstruct `ui/recipes/RecipeEditScreen.kt` from `87a602d` + diffs `5be7b4d` (and `RecipeViewModels.kt` from `87a602d` + `5961b4f`). Add a "Вставить списком" button next to single add, a new file `ui/recipes/BulkIngredientsDialog.kt` (keep files small), and a ViewModel function that appends parsed rows in order. Add a Robolectric test.
4. **Then:** Task 3 multi-dish UI -> F4 migration test -> Task 2 leftovers -> Task 4 tests -> Task 5 -> Task 7 -> Task 8 (after user answer) -> Task 6 (after user file).
5. **Precautions:** never change `applicationId`; no `fallbackToDestructiveMigration`; keep legacy `cuisine` column; no merge to `main` until CI green and migration tested; no secrets in git.
