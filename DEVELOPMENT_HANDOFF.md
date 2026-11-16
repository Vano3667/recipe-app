# DEVELOPMENT HANDOFF

Persistent continuation guide for AI coding agents working on this repository.
Read this file first, then `AGENT.md` (older notes, Russian; its "commit to main" rule is superseded).
Last updated: 2026-11-16 23:08 UTC (agent #8 "Timothy").
Full text of earlier versions: git history of this file (agent #7 version = `a9c8d61`).

---

## A. Project overview

| Item | Value |
|---|---|
| Project | "Домашнее меню" (Home Menu), offline Android app: recipes, weekly menu, shopping list. UI Russian only. |
| Repository | `Vano3667/recipe-app` (private). Default `main`, integration branch `modernization`. Draft PR #1 `modernization` -> `main` (CI tracking, do not merge yet). |
| Stack | Kotlin 1.9.24, Jetpack Compose (BOM 2024.06.00) + Material 3, MVVM (`data`/`domain`/`ui`), Hilt 2.51.1, Navigation Compose 2.7.7, Coroutines/Flow |
| Database | Room 2.6.1 (KSP), file `home_menu.db`, schema **v2** on `modernization` (main: v1), `MIGRATION_1_2` additive, pre-migration copy in `DatabaseSafety.kt` |
| Other libs | kotlinx.serialization 1.6.3 (backup JSON, custom colours JSON), Coil 2.6.0 (photos in `filesDir/photos`) |
| Build | Gradle 8.7 (wrapper NOT committed; CI runs `gradle wrapper --gradle-version 8.7`), JDK 17, minSdk 29, target/compile 34 |
| applicationId | `com.example.homemenu` (must never change) |
| Version | `modernization`: `versionCode = 2`, `versionName = "1.1.0"`, `BuildConfig.BUILD_DATE` (UTC date). `main`: 1 / "1.0". |
| Signing | No signingConfig. CI builds `assembleDebug` with the runner's random debug key (F2). |
| CI | `.github/workflows/android.yml`: job `build` (assembleDebug, testDebugUnitTest incl. Robolectric, lintDebug abortOnError) + jobs `instrumented (29, pixel_4)` and `instrumented (34, pixel_tablet)`. Runs on push and on pull_request (two runs per PR commit). ~4 min build, ~10 min instrumented. |
| Commands (CI) | `./gradlew assembleDebug testDebugUnitTest lintDebug`, `./gradlew connectedDebugAndroidTest` |
| Other docs | `README.md`, `DESIGN.md`, `TEST_REPORT.md` (results up to run #10, outdated), `ENVIRONMENT.md`, `CREDITS.md`, `AGENT.md` (outdated) |

### Important files (under `app/src/main/java/com/example/homemenu/`)
- Task 1: `domain/IngredientLineParser.kt`, `ui/recipes/BulkIngredientsDialog.kt`, `ui/recipes/RecipeEditScreen.kt`
- Task 2: `ui/recipes/RecipeEditScreen.kt` (`TagMultiSelect`), `data/seed/SeedData.kt`, `data/seed/Seeder.kt`
- Task 3: `ui/menu/MenuScreen.kt`, `ui/menu/MenuViewModel.kt`, `data/repo/Repositories.kt` (MenuRepository)
- Task 4: `data/files/BackupManager.kt`, `domain/backup/*`, `ui/settings/SettingsViewModel.kt`
- Task 5: `ui/recipes/RecipeDetailRoute.kt`, `ui/recipes/RecipeDetailScreen.kt`, `domain/RecipeTextParser.kt`, `ui/recipes/RecipeTextImport.kt`, `ui/recipes/TextImportDialog.kt`, `ui/recipes/RecipesScreen.kt`
- Task 7: `ui/theme/ThemePalettes.kt` (pure), `ui/theme/Theme.kt`, `ui/theme/Color.kt`, `ui/MainViewModel.kt`, `MainActivity.kt`, `ui/settings/AppearanceViewModel.kt`, `ui/settings/AppearanceSection.kt`, `ui/settings/SettingsScreen.kt`
- Task 8: `app/build.gradle.kts`, `ui/settings/SettingsScreen.kt` (About card)
- DB: `data/db/AppDatabase.kt`, `data/db/Entities.kt`, `data/db/DatabaseSafety.kt`

### Working rules
- No local JDK/Gradle/SDK. Code changes go through the GitHub API; **CI is the only build/test environment**.
- **Per task: feature branch from `modernization`, commits there, draft PR into `modernization`, read its checks, mark ready (`update_pull_request` draft=false; a draft cannot be merged), merge only when `build` and both instrumented jobs are green in at least one run on the head commit.** `modernization` -> `main` only via PR #1 after all tasks are verified and migration is tested.
- Docs-only commits: `[skip ci]`. Multi-part changes: intermediate parts `[skip ci]`, last part without it.
- Never force-push, rewrite history, or commit keystores/passwords/tokens.
- Tooling: "get file contents" returns only the blob SHA. Read bodies from commit diffs (`get_commit` `full_patch`). `87a602d` = original upload of every file; with `perPage=1, page=N` one file is returned (build.gradle.kts = 6, MainActivity = 11, Mappers = 12, Repositories = 18, SeedData = 19, Seeder = 20, Models = 27, MainViewModel = 28, MenuScreen = 32, MenuViewModel = 33, AppNav = 34, RecipeDetailScreen = 35, RecipeEditScreen = 36, RecipeViewModels = 37, RecipesScreen = 38, SettingsScreen = 39, SettingsViewModel = 40, Color.kt = 43, Theme.kt = 44). Reconstruct = original + every later patch (`list_commits` with `path` gives the list). **After each write, check the commit's diff stats: they must show only the intended lines.**
- Files whose full current body is known exactly (agent #8): `SeedData.kt` + `Seeder.kt` @ `3d0ac4e`, `app/build.gradle.kts` @ `8e68c59`, `SettingsScreen.kt` @ `dafe021`, this file @ its latest commit (written whole).
- **Kotlin DSL gotcha:** in `app/build.gradle.kts` write `import java.time.X` at the top; a fully qualified `java.time.X` clashes with the generated `java` extension accessor and fails configuration (seen on `dafe021`, fixed in `8e68c59`).
- `push_files` can write several files in ONE commit. Never write two commits to the same branch in parallel.
- `search_code` (`repo:Vano3667/recipe-app`) returns code fragments but indexes only `main`.
- **CI visibility:** no Actions tool; `pull_request_read` method `get_check_runs` returns the head commit's jobs with status/conclusion (no logs). Logs of a failed job: ask the user to open the job link and paste the failing step.

---

## B. Task checklist

1. [ ] Bulk ingredient input — TESTED (CI green; manual UI check pending)
2. [ ] Tags + full cuisine removal — TESTED (code clean, merged `45e3485`; README line + tag UI tests pending)
3. [ ] Multiple recipes per meal — TESTED (CI green; manual UI check and migration test pending)
4. [ ] Complete ZIP and JSON export/import — IMPLEMENTED (round-trip/rollback tests missing)
5. [ ] Recipe sharing + text import — TESTED (CI green; manual Sharesheet/TXT check pending)
6. [ ] Fix existing import bug — BLOCKED, WAITING FOR USER FILE
7. [ ] Six themes + custom colours — TESTED (CI green, merged; hard-coded colour scan + UI test pending)
8. [ ] Safe APK updates — IN PROGRESS (versioning + build date TESTED, merged `653c783`); signing BLOCKED

Supporting: migration test on real v1 DB (F4); data preservation; UI consistency; release APK.

---

## C. Development status and technical decisions

Statuses: NOT STARTED / IN PROGRESS / IMPLEMENTED / TESTED / VERIFIED / BLOCKED. TESTED = CI build + unit + Robolectric + lint + instrumented green; VERIFIED would also need a manual check on a device.

**Global state (2026-11-16 23:08 UTC):** `modernization` @ `653c783` (merge of PR #4) + this docs commit. Contains `45e3485` (merge of PR #3). Every merged head had build + API 29 + API 34 green in at least one run.

### Task 1 — Bulk ingredient input: TESTED
- Parser `domain/IngredientLineParser.kt` (`9da0cbc`) + tests (`0fec6c8`). Dialog `ui/recipes/BulkIngredientsDialog.kt` (`cfbd8d2`, `ab3c0f8`, `7fcf342`): "Вставить списком" -> paste -> "Разобрать" -> editable preview, remove, hints, duplicates unchecked -> "Добавить (n)". Order preserved. Single-add untouched. `BulkIngredientsTest` green.
- Missing: Robolectric/instrumented test of the dialog; known risk: if `vm.addCustomProduct` fails and never calls back, remaining rows are not added.

### Task 2 — Tags + cuisine removal: TESTED
- Earlier: cuisine removed from domain (`047d9e6`, incl. `DEFAULT_CUISINES`), edit state (`5961b4f`), detail (`4328ad3`), edit form (`5be7b4d`, `CuisineField` deleted). Tags multi-select at the bottom (`TagMultiSelect`: removable chips, dropdown with checkboxes, separate "Новый тег"), case-insensitive dedupe (`aa8edfe`).
- Agent #8: audit via commit diffs (`Models.kt`, `RecipeEditScreen.kt`, `Mappers.kt`, `Seeder.kt` clean); last code trace `SeedData.SeedRecipe.cuisine` removed in `3d0ac4e` (PR #3, CI build + API 29 + API 34 green in both runs, merged `45e3485`).
- Kept on purpose: DB column `recipes.cuisine` (no table rebuild; existing values preserved) and backup v1 `BackupRecipe.cuisine` (old backups stay readable; `BackupJsonTest` uses it).
- Missing: README.md line "категория, кухня (готовые варианты и свои)" in "Возможности" (README history: `87a602d`, `717fc83`, `eaea137`); tests for tag select/create/remove.

### Task 3 — Multiple recipes per meal: TESTED
- Data (`047d9e6`, `aa8edfe`): `menu_entries.position`, non-unique `(date, slot)` index, addToSlot/replaceRecipe/reorder/move/clear with renumbering; same recipe twice in one meal refused. UI (`4c03f6a`, `2e76d8f`).
- Missing: check `MenuGeneratorUseCase` / `ShoppingListUseCase` for one-dish assumptions; tests for dishes()/reorder; migration test (F4).

### Task 4 — ZIP/JSON export/import: IMPLEMENTED
- Format v2 (manifest, SHA-256, Base64 images in JSON, v1 readable), staged import with preview / duplicate modes (skip, update, separate) / one transaction / photo rollback / read-back (`862a437`, `1bec32b`); Settings UI (`94891a7`, `16c5dfb`); `BackupJsonTest` v2 (`1a764a1`). Backup includes `themeId` and `customColors`.
- Missing: round trip with photos, failed-import rollback test, instrumented import read-back.

### Task 5 — Sharing + text import: TESTED
- Share text + Sharesheet + clipboard (`a8b6896`); TXT export (`c5faad8`, `27e4800`); text parser (`1e8f0b8`, tests `ac6bcb7`); import UI (`527450f`, `3bc4e46`, `1b8484c`), tests `RecipeTextImportTest` (`bbc9053`). Always creates a NEW recipe.
- Missing / optional: incoming `ACTION_SEND text/plain` intent-filter; pure share formatter + round-trip test; Robolectric test of the dialog.

### Task 6 — Existing import bug: BLOCKED — WAITING FOR USER FILE
- Requested by agents #5-#8. When it arrives: reproduce in a unit test against `BackupManager` staging / `BackupJson.decode`, find root cause, fix, verify DB and photos.

### Task 7 — Themes + custom colours: TESTED (merged `d71f480`)
- See PR #2 and `a9c8d61` version of this file. Missing: hard-coded colour scan (`Color(0x...)`, `Color.White/Black`, `tagColors(hint, dark)` callers), Robolectric test of AppearanceContent, manual check.

### Task 8 — Safe APK updates: IN PROGRESS, signing BLOCKED
- Done earlier: build number in About (`16c5dfb`).
- Agent #8 (PR #4, merged `653c783`): `2027982` + fix `8e68c59` build.gradle.kts: `versionCode` 2, `versionName` "1.1.0", `BuildConfig.BUILD_DATE` (UTC date at configuration time, `import java.time.*` at the top); `dafe021` About card line "Дата сборки …" (tag `about_build_date`). applicationId/namespace unchanged.
- Missing: release signing (F2), CI `assembleRelease`, update-over-install test, Room schema export + migration test (F4).

### Technical decisions (agent #8 additions; earlier table in `a9c8d61`)
| Decision | Why | Commit |
|---|---|---|
| Keep `recipes.cuisine` column and backup v1 field | No destructive migration; old backups import | 3d0ac4e |
| versionCode +1 per APK given to the user; 1.1.0 for the modernization release | Android refuses updates with a lower/equal code | 2027982 |
| BUILD_DATE as date only (UTC), not a timestamp | Readable in About; same format on every machine | 2027982 |
| `import java.time.*` in build script | FQN clashes with `java` accessor | 8e68c59 |
| Separate "Дата сборки" line, existing `about_build` line unchanged | Existing tests keep their tags | dafe021 |

---

## D. Git history

- `main` @ `2ceef75` green (CI #11).
- Agents #1-#7: see `a9c8d61` version of this file (section E there).
- Agent #8 (2026-11-16 22:20-23:08 UTC):
  - `task2-cuisine-cleanup`: `3d0ac4e` -> PR #3 -> merged `45e3485`.
  - `task8-versioning`: `2027982`, `dafe021` (build FAILED at configuration, both runs), `8e68c59` fix -> PR #4 -> merged `653c783`.
  - docs: `9a4d522` and this commit on `modernization` [skip ci].
- All pushes verified by the API response and by reading the commit diff back. Merges verified by the merge API response (`merged: true`).

---

## E. Testing

| Commit | Run | build | API 29 | API 34 |
|---|---|---|---|---|
| `3d0ac4e` (PR #3) | 43970267337 (push) | green | green | green |
| `3d0ac4e` (PR #3) | 43970340643 (PR) | green | green | green |
| `dafe021` (PR #4) | 43970678742, 43970740872 | FAILED ~1 min (config) | skipped | skipped |
| `8e68c59` (PR #4) | 43973554458 | green | green | green |
| `8e68c59` (PR #4) | 43973547049 | **FAILED after 3.5 min** (cause unknown, no logs) | skipped | skipped |

- The `8e68c59` failure in run 43973547049: same code passed in 43973554458, so most likely a flaky unit/Robolectric test or runner issue, NOT proven. Next agent: if any later run fails `build` again, ask the user for the failing step of job 133671611335 (Actions tab) and fix the flaky test.
- Merge commits `45e3485` and `653c783` themselves: push CI on `modernization` will run; not read yet.

## PENDING TESTS AND CHECKS

| Commit | What | How to read | Status |
|---|---|---|---|
| `653c783` (modernization, merge of PR #4) | push CI (build + instrumented) | Draft PR #1 head is the docs commit [skip ci] -> no checks there. Next code commit on a PR will show it; or ask the user for the Actions result | PENDING, not read |
| `8e68c59` run 43973547049 | build failure root cause | user: job 133671611335 log | UNKNOWN |

---

## F. Blockers and known issues

- **F2 Signing (Task 8, user):** no release key; CI APKs use random debug keys and cannot update each other. If the installed app came from CI, export a backup ZIP first, then reinstall. Plan: keystore in GitHub Secrets (`KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`), `signingConfigs.release` reading env vars, CI `assembleRelease`. Waiting: how the current app was installed (CI artifact / Android Studio / other) and whether the user has the original keystore. Independent work continues.
- **F3 Import bug file (Task 6, user):** waiting for the failing file. Independent work continues.
- **F4 Room schemas not committed:** needed for a 1->2 `MigrationTestHelper` test. Generate v1 schema from `2ceef75` in CI, commit `app/schemas/.../1.json`, add `room-testing`.
- **F5** `AGENT.md` and `TEST_REPORT.md` outdated.
- **F7 Flaky instrumented job on API 34:** earlier 1 of 3 and 1 of 2 runs failed; agent #8's runs were all green on API 34.
- **F9 (new) Possible flaky `build` job:** see section E (`8e68c59`, run 43973547049).

---

## G. NEXT AGENT — START HERE

1. **State:** `modernization` @ `653c783` + docs. Tasks 1, 2, 3, 5, 7 TESTED; Task 8 versioning TESTED, signing BLOCKED; Task 4 IMPLEMENTED; Task 6 BLOCKED. No open feature PRs. PR #1 stays draft.
2. **Next action:** new branch `task7-colour-scan` from `modernization`: find hard-coded colours (`search_code` on `main`: `Color(0x`, `Color.White`, `Color.Black`, `tagColors(`), map each to `MaterialTheme.colorScheme` roles, reconstruct each file from `87a602d` + later patches, verify diff stats, PR, CI, merge.
3. **Then:** README cuisine line (Task 2) -> Task 4 round-trip/rollback unit tests (`BackupJsonTest`, `BackupManager` staging) -> F4 migration test -> Task 5 optional share intent -> Task 8 signing (after user answer) -> Task 6 (after user file). Update `TEST_REPORT.md`.
4. **Precautions:** never change `applicationId`; no `fallbackToDestructiveMigration`; keep legacy `cuisine` column; do not merge PR #1 until CI green and migration tested; no secrets in git; check every write's diff stats; mark PRs ready before merging.

---

## H. MANUAL CHECKS ON A DEVICE (not possible for agents)

- Task 1: Recipe edit -> "Вставить списком" -> paste 5 lines -> preview -> edit one, remove one -> add -> save -> reopen: order and amounts kept.
- Task 2: Recipe edit -> tags at the very bottom; select several, create one, remove one; no "Кухня" anywhere.
- Task 3: Menu -> one meal with 3 dishes -> reorder, replace, remove -> restart app -> same list.
- Task 5: Share -> "Как текст" (Sharesheet), "Скопировать текст", "Сохранить как TXT"; Recipes -> "Из текста" -> paste -> preview -> save.
- Task 7: Settings -> "Оформление" -> each of the 6 themes applies at once on all tabs; "Свои цвета" -> restart -> kept; reset.
- Task 8: Settings -> "О приложении" shows версия 1.1.0, Сборка 2, Дата сборки. Update over the installed app only works with the same signing key (F2).
