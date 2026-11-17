# DEVELOPMENT HANDOFF

Persistent continuation guide for AI coding agents working on this repository.
Read this file first, then `AGENT.md` (older notes, Russian; its "commit to main" rule is superseded).
Last updated: 2026-11-17 00:20 UTC (agent #9 "Roy").
Full text of earlier versions: git history of this file (agent #7 version = `a9c8d61`, agent #8 = `09af78a`).
This version was rewritten whole by agent #9 from the agent #8 text; sections that were unchanged are carried over.

---

## A. Project overview

| Item | Value |
|---|---|
| Project | "Домашнее меню" (Home Menu), recipe / weekly menu / shopping list app, Russian UI, offline |
| Repo | `Vano3667/recipe-app` (private). Branches: `main` (released code), `modernization` (integration branch for the 8 tasks), feature branches per task |
| Platform / stack | Android, Kotlin 1.9.24, Jetpack Compose (BOM 2024.06), Material 3, MVVM (data / domain / ui), ViewModel + StateFlow, Room 2.6, Hilt 2.51, Navigation Compose, Coroutines/Flow |
| Other libs | kotlinx.serialization 1.6.3 (backup JSON, custom colours JSON), Coil 2.6.0 (photos in `filesDir/photos`) |
| Build | Gradle 8.7 (wrapper NOT committed; CI runs `gradle wrapper --gradle-version 8.7`), JDK 17, minSdk 29, target/compile 34 |
| applicationId | `com.example.homemenu` (must never change) |
| Version | `modernization`: `versionCode = 2`, `versionName = "1.1.0"`, `BuildConfig.BUILD_DATE` (UTC date). `main`: 1 / "1.0". |
| Signing | No signingConfig. CI builds `assembleDebug` with the runner's random debug key (F2). |
| CI | `.github/workflows/android.yml`: job `build` (assembleDebug, testDebugUnitTest incl. Robolectric, lintDebug abortOnError) + jobs `instrumented (29, pixel_4)` and `instrumented (34, pixel_tablet)`. Runs on push and on pull_request (two runs per PR commit). ~4 min build, ~10 min instrumented. |
| Commands (CI) | `./gradlew assembleDebug testDebugUnitTest lintDebug`, `./gradlew connectedDebugAndroidTest` |
| Other docs | `README.md` (feature list updated by agent #9), `DESIGN.md`, `TEST_REPORT.md` (results up to run #10, outdated), `ENVIRONMENT.md`, `CREDITS.md`, `AGENT.md` (outdated) |

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
- Docs-only commits: `[skip ci]`, may go straight to `modernization`. Multi-part changes: intermediate parts `[skip ci]`, last part without it.
- Never force-push, rewrite history, or commit keystores/passwords/tokens.
- Tooling: "get file contents" returns only the blob SHA. Read bodies from commit diffs (`get_commit` `full_patch`). `87a602d` = original upload of every file; with `perPage=1, page=N` one file is returned (README = 4, build.gradle.kts = 6, MainActivity = 11, Mappers = 12, Repositories = 18, SeedData = 19, Seeder = 20, Models = 27, MainViewModel = 28, Components = 29, MenuScreen = 32, MenuViewModel = 33, AppNav = 34, RecipeDetailScreen = 35, RecipeEditScreen = 36, RecipeViewModels = 37, RecipesScreen = 38, SettingsScreen = 39, SettingsViewModel = 40, Color.kt = 43, Theme.kt = 44). Reconstruct = original + every later patch (`list_commits` with `path` gives the list). **After each write, check the commit's diff stats: they must show only the intended lines.**
- Files whose full current body is known exactly: `SeedData.kt` + `Seeder.kt` @ `3d0ac4e`, `app/build.gradle.kts` @ `8e68c59`, `SettingsScreen.kt` @ `dafe021`, `README.md` @ `a3dd68e`, this file @ its latest commit (written whole).
- **Kotlin DSL gotcha:** in `app/build.gradle.kts` write `import java.time.X` at the top; a fully qualified `java.time.X` clashes with the generated `java` extension accessor and fails configuration (seen on `dafe021`, fixed in `8e68c59`).
- `push_files` can write several files in ONE commit. Never write two commits to the same branch in parallel.
- `search_code` (`repo:Vano3667/recipe-app`) returns code fragments but indexes only `main`. Use separate queries; `A OR B` with dotted names returned nothing.
- **CI visibility:** no Actions tool; `pull_request_read` method `get_check_runs` returns the head commit's jobs with status/conclusion (no logs). Logs of a failed job: ask the user to open the job link and paste the failing step.

---

## B. Task checklist

1. [ ] Bulk ingredient input — TESTED (CI green; manual UI check pending)
2. [ ] Tags + full cuisine removal — TESTED (code clean, merged `45e3485`; README line removed `a3dd68e`; tag UI tests pending)
3. [ ] Multiple recipes per meal — TESTED (CI green; manual UI check and migration test pending)
4. [ ] Complete ZIP and JSON export/import — IMPLEMENTED (round-trip/rollback tests missing)
5. [ ] Recipe sharing + text import — TESTED (CI green; manual Sharesheet/TXT check pending)
6. [ ] Fix existing import bug — BLOCKED, WAITING FOR USER FILE
7. [ ] Six themes + custom colours — TESTED (CI green, merged; colour scan on `main` done by agent #9, scan of modernization-only UI files + UI test pending)
8. [ ] Safe APK updates — IN PROGRESS (versioning + build date TESTED, merged `653c783`); signing BLOCKED

Supporting: migration test on real v1 DB (F4); data preservation; UI consistency; release APK.

---

## C. Development status and technical decisions

Statuses: NOT STARTED / IN PROGRESS / IMPLEMENTED / TESTED / VERIFIED / BLOCKED. TESTED = CI build + unit + Robolectric + lint + instrumented green; VERIFIED would also need a manual check on a device.

**Global state (2026-11-17 00:20 UTC):** `modernization` @ `653c783` (merge of PR #4) + docs commits `09af78a`, `a3dd68e` (README) and this one, all `[skip ci]`. No open feature PRs. PR #1 (`modernization` -> `main`) open as draft, do not merge.

### Task 1 — Bulk ingredient input: TESTED
- Parser `domain/IngredientLineParser.kt` (`9da0cbc`) + tests (`0fec6c8`). Dialog `ui/recipes/BulkIngredientsDialog.kt` (`cfbd8d2`, `ab3c0f8`, `7fcf342`): "Вставить списком" -> paste -> "Разобрать" -> editable preview, remove, hints, duplicates unchecked -> "Добавить (n)". Order preserved. Single-add untouched. `BulkIngredientsTest` green.
- Missing: Robolectric/instrumented test of the dialog; known risk: if `vm.addCustomProduct` fails and never calls back, remaining rows are not added.

### Task 2 — Tags + cuisine removal: TESTED
- Cuisine removed from domain (`047d9e6`, incl. `DEFAULT_CUISINES`), edit state (`5961b4f`), detail (`4328ad3`), edit form (`5be7b4d`, `CuisineField` deleted), seed data (`3d0ac4e`, merged `45e3485`). Tags multi-select at the bottom (`TagMultiSelect`: removable chips, dropdown with checkboxes, separate "Новый тег"), case-insensitive dedupe (`aa8edfe`).
- Agent #9: README "Возможности" no longer mentions "кухня" (`a3dd68e`, diff: 3 lines + trailing newline).
- Kept on purpose: DB column `recipes.cuisine` (no table rebuild; existing values preserved) and backup v1 `BackupRecipe.cuisine` (old backups stay readable; `BackupJsonTest` uses it).
- Missing: tests for tag select/create/remove.

### Task 3 — Multiple recipes per meal: TESTED
- Data (`047d9e6`, `aa8edfe`): `menu_entries.position`, non-unique `(date, slot)` index, addToSlot/replaceRecipe/reorder/move/clear with renumbering; same recipe twice in one meal refused. UI (`4c03f6a`, `2e76d8f`).
- Missing: check `MenuGeneratorUseCase` / `ShoppingListUseCase` for one-dish assumptions; tests for dishes()/reorder; migration test (F4). README "Меню" line does not yet mention several dishes per meal.

### Task 4 — ZIP/JSON export/import: IMPLEMENTED
- Format v2 (manifest, SHA-256, Base64 images in JSON, v1 readable), staged import with preview / duplicate modes (skip, update, separate) / one transaction / photo rollback / read-back (`862a437`, `1bec32b`); Settings UI (`94891a7`, `16c5dfb`); `BackupJsonTest` v2 (`1a764a1`). Backup includes `themeId` and `customColors`.
- Missing: round trip with photos, failed-import rollback test, instrumented import read-back. README "Резервная копия" line still describes v1 only (update once v2 behaviour is confirmed from `BackupManager` diffs).

### Task 5 — Sharing + text import: TESTED
- Share text + Sharesheet + clipboard (`a8b6896`); TXT export (`c5faad8`, `27e4800`); text parser (`1e8f0b8`, tests `ac6bcb7`); import UI (`527450f`, `3bc4e46`, `1b8484c`), tests `RecipeTextImportTest` (`bbc9053`). Always creates a NEW recipe.
- Missing / optional: incoming `ACTION_SEND text/plain` intent-filter; pure share formatter + round-trip test; Robolectric test of the dialog.

### Task 6 — Existing import bug: BLOCKED — WAITING FOR USER FILE
- Requested by agents #5-#9. When it arrives: reproduce in a unit test against `BackupManager` staging / `BackupJson.decode`, find root cause, fix, verify DB and photos.

### Task 7 — Themes + custom colours: TESTED (merged `d71f480`)
- See PR #2 and `a9c8d61` version of this file for the full description.
- **Agent #9 colour scan (`search_code`, indexes `main` only):**
  - `Color(0x...)` and `Color.White`: only in `ui/theme/Color.kt` (legacy palette constants, legacy colour schemes, `tagColors` table). Expected; nothing to change there.
  - `Color.Black`: no hits.
  - `tagColors(hint, dark)`: one caller, `ui/components/Components.kt` `TagChip` -> `tagColors(tag.colorHint, isAppDark())`. Tag chips keep their fixed palette (contrast >= 4.5:1 by design) and switch light/dark by background luminance (`isAppDark()`), so they stay readable in all 6 themes and custom colours. Decision: keep as is.
  - Not covered by the scan (files exist only on `modernization`): `BulkIngredientsDialog.kt`, `TextImportDialog.kt`, `RecipeTextImport.kt`, `RecipeDetailRoute.kt`, `AppearanceSection.kt` (its preview cards use theme colours on purpose), and new code in `MenuScreen.kt` / `RecipeEditScreen.kt`. Next: read their creating commits (`full_patch`) and grep the patch text for `Color(`.
- Missing: Robolectric test of AppearanceContent, manual check.

### Task 8 — Safe APK updates: IN PROGRESS, signing BLOCKED
- Done: build number in About (`16c5dfb`); `2027982` + fix `8e68c59` build.gradle.kts: `versionCode` 2, `versionName` "1.1.0", `BuildConfig.BUILD_DATE`; `dafe021` About line "Дата сборки …" (tag `about_build_date`). Merged `653c783`. applicationId/namespace unchanged.
- Missing: release signing (F2), CI `assembleRelease`, update-over-install test, Room schema export + migration test (F4).

### Technical decisions (recent; earlier table in `a9c8d61`)
| Decision | Why | Commit |
|---|---|---|
| Keep `recipes.cuisine` column and backup v1 field | No destructive migration; old backups import | 3d0ac4e |
| versionCode +1 per APK given to the user; 1.1.0 for the modernization release | Android refuses updates with a lower/equal code | 2027982 |
| BUILD_DATE as date only (UTC), not a timestamp | Readable in About; same format on every machine | 2027982 |
| `import java.time.*` in build script | FQN clashes with `java` accessor | 8e68c59 |
| Separate "Дата сборки" line, existing `about_build` line unchanged | Existing tests keep their tags | dafe021 |
| Tag chips keep fixed accessible palette under all themes | Already contrast-checked; follows dark/light by luminance | agent #9 (no code change) |
| README changes: only claims already TESTED in code | Avoid documenting unverified behaviour | a3dd68e |

---

## D. Git history

- `main` @ `2ceef75` green (CI #11).
- Agents #1-#7: see `a9c8d61` version of this file (section E there).
- Agent #8: `3d0ac4e` -> PR #3 -> merged `45e3485`; `2027982`, `dafe021`, `8e68c59` -> PR #4 -> merged `653c783`; docs `9a4d522`, `09af78a`.
- Agent #9 (2026-11-17 00:10-00:20 UTC), directly on `modernization`, all `[skip ci]`, pushed and verified by reading the commit diff back:
  - `a3dd68e` docs(readme): cuisine removed from feature list, themes line describes six themes + custom colours.
  - this docs commit.

---

## E. Testing

| Commit | Run | build | API 29 | API 34 |
|---|---|---|---|---|
| `3d0ac4e` (PR #3) | 43970267337 (push) | green | green | green |
| `3d0ac4e` (PR #3) | 43970340643 (PR) | green | green | green |
| `dafe021` (PR #4) | 43970678742, 43970740872 | FAILED ~1 min (config) | skipped | skipped |
| `8e68c59` (PR #4) | 43973554458 | green | green | green |
| `8e68c59` (PR #4) | 43973547049 | **FAILED after 3.5 min** (cause unknown, no logs) | skipped | skipped |

- Agent #9 commits are docs only (`[skip ci]`); no build triggered, nothing to test.
- The `8e68c59` failure in run 43973547049: same code passed in 43973554458, most likely flaky, NOT proven (F9).

## PENDING TESTS AND CHECKS

| Commit | What | How to read | Status |
|---|---|---|---|
| `653c783` (modernization, merge of PR #4) | push CI (build + instrumented) | Next code commit on a PR will show the state; or ask the user for the Actions result of `653c783` | PENDING, not read |
| `8e68c59` run 43973547049 | build failure root cause | user: job 133671611335 log | UNKNOWN |

---

## F. Blockers and known issues

- **F2 Signing (Task 8, user):** no release key; CI APKs use random debug keys and cannot update each other. If the installed app came from CI, export a backup ZIP first, then reinstall. Plan: keystore in GitHub Secrets (`KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`), `signingConfigs.release` reading env vars, CI `assembleRelease`. Waiting: how the current app was installed (CI artifact / Android Studio / other) and whether the user has the original keystore. Never generate a replacement key without the user's decision.
- **F3 Import bug file (Task 6, user):** waiting for the failing file (upload it to the agent chat or commit it to a private branch). Independent work continues.
- **F4 Room schemas not committed:** needed for a 1->2 `MigrationTestHelper` test. Generate v1 schema from `2ceef75` in CI, commit `app/schemas/.../1.json`, add `room-testing`.
- **F5** `AGENT.md` and `TEST_REPORT.md` outdated.
- **F7 Flaky instrumented job on API 34:** earlier 1 of 3 and 1 of 2 runs failed; agent #8's runs were all green on API 34.
- **F9 Possible flaky `build` job:** see section E (`8e68c59`, run 43973547049).
- **F10 (agent #9) Tooling limit:** this agent works only through the GitHub API (no terminal, no Gradle, no Actions logs). Code edits require reconstructing each file from `87a602d` + all later patches; large Kotlin files are high-risk to rewrite whole. Prefer small, well-understood files and always verify diff stats.

---

## G. NEXT AGENT — START HERE

1. **State:** `modernization` @ this docs commit (code = `653c783`). Tasks 1, 2, 3, 5, 7 TESTED; Task 8 versioning TESTED, signing BLOCKED (F2); Task 4 IMPLEMENTED; Task 6 BLOCKED (F3). No open feature PRs. PR #1 stays draft.
2. **Next action:** Task 7 scan of modernization-only UI files: `list_commits` with `path` for each file listed in C/Task 7, read creating commit `full_patch`, look for `Color(0x`, `Color.White`, `Color.Black`. Fix any hit on a branch `task7-colour-scan` by mapping to `MaterialTheme.colorScheme`, PR, CI, merge.
3. **Then:** Task 4 round-trip/rollback unit tests (new test file only, e.g. `app/src/test/.../domain/BackupRoundTripTest.kt`, against `BackupJson` encode/decode; low risk because it does not touch existing files) -> F4 migration test -> Task 3 README menu line -> Task 5 optional share intent -> Task 8 signing (after user answer) -> Task 6 (after user file). Update `TEST_REPORT.md`.
4. **Precautions:** never change `applicationId`; no `fallbackToDestructiveMigration`; keep legacy `cuisine` column; do not merge PR #1 until CI green and migration tested; no secrets in git; check every write's diff stats; mark PRs ready before merging.

---

## H. MANUAL CHECKS ON A DEVICE (not possible for agents)

- Task 1: Recipe edit -> "Вставить списком" -> paste 5 lines -> preview -> edit one, remove one -> add -> save -> reopen: order and amounts kept.
- Task 2: Recipe edit -> tags at the very bottom; select several, create one, remove one; no "Кухня" anywhere.
- Task 3: Menu -> one meal with 3 dishes -> reorder, replace, remove -> restart app -> same list.
- Task 5: Share -> "Как текст" (Sharesheet), "Скопировать текст", "Сохранить как TXT"; Recipes -> "Из текста" -> paste -> preview -> save.
- Task 7: Settings -> "Оформление" -> each of the 6 themes applies at once on all tabs; "Свои цвета" -> restart -> kept; reset. Tag chips readable in every theme.
- Task 8: Settings -> "О приложении" shows версия 1.1.0, Сборка 2, Дата сборки. Update over the installed app only works with the same signing key (F2).
