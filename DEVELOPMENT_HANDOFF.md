# DEVELOPMENT_HANDOFF

Persistent continuation guide for AI coding agents working on this repository.
Read this file first, then `AGENT.md` (older notes, Russian; its "commit to main" rule is superseded).
Last updated: 2026-11-17 03:30 UTC (agent #10 "Beverly").
Full text of earlier versions: git history of this file (agent #7 = `a9c8d61`, agent #8 = `09af78a`, agent #9 = `cc95a03`, agent #10 part 1 = `123a8ab`).
The per-task "Important files" list and the older decision tables are NOT repeated here: read them in `cc95a03` / `a9c8d61`.

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
| CI | `.github/workflows/android.yml` (workflow id 412025045): job `build` (assembleDebug, testDebugUnitTest incl. Robolectric, lintDebug) + `instrumented (29, pixel_4)` + `instrumented (34, pixel_tablet)`. Runs on push and on pull_request (two runs per PR commit). ~5 min build, ~10-12 min instrumented. |
| Commands (CI) | `./gradlew assembleDebug testDebugUnitTest lintDebug`, `./gradlew connectedDebugAndroidTest` |
| Other docs | `README.md`, `DESIGN.md`, `TEST_REPORT.md` (outdated, run #10), `ENVIRONMENT.md`, `CREDITS.md`, `AGENT.md` (outdated) |

### Working rules
- No local JDK/Gradle/SDK. Code changes go through the GitHub API; **CI is the only build/test environment**.
- **Per task: feature branch from `modernization`, commits there, draft PR into `modernization`, read its checks, mark ready (draft=false), merge only when `build` and both instrumented jobs are green in at least one run on the head commit.** `modernization` -> `main` only via PR #1 after all tasks are verified and migration is tested.
- Docs-only commits: `[skip ci]`, may go straight to `modernization`.
- Never force-push, rewrite history, or commit keystores/passwords/tokens.
- Tooling: "get file contents" returns only the blob SHA. Read bodies from commit diffs (`get_commit` `full_patch`). `87a602d` = original upload of every file; `perPage=1, page=N` returns one file (README = 4, build.gradle.kts = 6, **AppFlowsTest = 8**, MainActivity = 11, Mappers = 12, Repositories = 18, SeedData = 19, Seeder = 20, Models = 27, MainViewModel = 28, Components = 29, MenuScreen = 32, MenuViewModel = 33, AppNav = 34, RecipeDetailScreen = 35, RecipeEditScreen = 36, RecipeViewModels = 37, RecipesScreen = 38, SettingsScreen = 39, SettingsViewModel = 40, Color.kt = 43, Theme.kt = 44, BackupJsonTest = 53). For a later multi-file commit, `get_commit` `detail=stats` lists files in order; then `full_patch` with `perPage=1, page=N`. Reconstruct = original + every later patch (`list_commits` with `path`). After each write, check the commit's diff stats.
- **Actions tools are available:** `actions_list` (`list_workflow_runs`, `list_workflow_jobs` with run id), `get_job_logs` (`run_id` + `failed_only=true` + `return_content=true`, or `job_id`) return real logs; `actions_run_trigger` `rerun_failed_jobs` re-runs a flaky job.
- **Kotlin DSL gotcha:** in `app/build.gradle.kts` use `import java.time.X` at the top (FQN clashes with the `java` accessor).
- Files whose full current body is known exactly: `AppFlowsTest.kt` @ `18d4bdb`, `BackupAnalyzerTest.kt` @ `c892225`, `SeedData.kt` + `Seeder.kt` @ `3d0ac4e`, `app/build.gradle.kts` @ `8e68c59`, `SettingsScreen.kt` @ `dafe021`, `README.md` @ `a3dd68e`, this file.

---

## B. Task checklist

1. [ ] Bulk ingredient input — TESTED (CI green; manual UI check pending)
2. [ ] Tags + full cuisine removal — TESTED (code + README clean; tag UI tests pending)
3. [ ] Multiple recipes per meal — TESTED (CI green; manual UI check and migration test pending)
4. [ ] Complete ZIP and JSON export/import — TESTED for the pure layer (`BackupAnalyzerTest`, merged `1eb19dc`); `BackupManager` staging/rollback and instrumented read-back untested
5. [ ] Recipe sharing + text import — TESTED (CI green; manual Sharesheet/TXT check pending)
6. [ ] Fix existing import bug — BLOCKED, WAITING FOR USER FILE (F3)
7. [ ] Six themes + custom colours — TESTED (merged; colour scan of all UI code complete, clean; AppearanceContent UI test pending)
8. [ ] Safe APK updates — IN PROGRESS (versioning + build date TESTED); signing BLOCKED (F2)

Statuses: NOT STARTED / IN PROGRESS / IMPLEMENTED / TESTED / VERIFIED / BLOCKED. TESTED = CI build + unit + Robolectric + lint + instrumented green; VERIFIED also needs a manual device check.

---

## C. Implementation status

**Global state (2026-11-17 03:30 UTC):** `modernization` @ `2c61d54` (merge of PR #6) + this docs commit. No open feature PRs. PR #1 (`modernization` -> `main`) stays draft, do not merge.

### Task 1 — Bulk ingredient input: TESTED
- Parser `domain/IngredientLineParser.kt` + tests; dialog `ui/recipes/BulkIngredientsDialog.kt` (`cfbd8d2`, `7fcf342`); button "Вставить списком" (tag `bulk_ingredients`) in a `FlowRow` next to `add_ingredient` (`ab3c0f8`). Order preserved.
- Missing: Robolectric/instrumented dialog test; risk: if `vm.addCustomProduct` never calls back, remaining rows are not added.

### Task 2 — Tags + cuisine removal: TESTED
- Cuisine removed everywhere in UI/domain/seed/README. `TagMultiSelect` at the bottom of the edit form (tags `selected_tag_<id>`, `tags_dropdown`, `tag_option_<id>`, `new_tag`).
- Kept on purpose: DB column `recipes.cuisine` and backup v1 field.
- Missing: tests for tag select/create/remove (tags above make an instrumented test easy).

### Task 3 — Multiple recipes per meal: TESTED
- `menu_entries.position`, add/replace/reorder/move/clear; UI `DishRow` in `MenuScreen.kt` (`2e76d8f`; tags `dish_<id>`, `dish_up_/down_/replace_/remove_<id>`, `slot_add_<date>_<slot>`).
- Missing: check `MenuGeneratorUseCase` / `ShoppingListUseCase` for one-dish assumptions; dishes()/reorder tests; migration test (F4); README menu line.

### Task 4 — ZIP/JSON export/import: TESTED (pure layer)
- `domain/backup/BackupFormat.kt` v2 + `ImportAnalysis.kt` (`862a437`), `data/files/BackupManager.kt`, Settings UI. `BackupAnalyzerTest.kt` (`c892225`, PR #5, merged `1eb19dc`).
- Missing: `BackupManager` failed-import rollback test, ZIP round trip with real photo files, instrumented import read-back.

### Task 5 — Sharing + text import: TESTED
- `RecipeDetailRoute.kt` (`a8b6896`, `27e4800`): share text, Sharesheet, clipboard, TXT export. `TextImportDialog.kt` (`3bc4e46`). Always creates a NEW recipe.
- Optional: incoming `ACTION_SEND text/plain` intent-filter; Robolectric dialog test.

### Task 6 — Existing import bug: BLOCKED — WAITING FOR USER FILE
- When it arrives: reproduce in a unit test against `BackupJson.decode` / `BackupAnalyzer` / `BackupManager` staging, find root cause, fix, verify DB and photos.

### Task 7 — Themes + custom colours: TESTED (merged `d71f480`)
- **Agent #10 scan of modernization-only UI code** (read creating/changing commits in full): `AppearanceSection.kt` (`8bc9e0d`), `TextImportDialog.kt` (`3bc4e46`), `BulkIngredientsDialog.kt` (`cfbd8d2`, `7fcf342`), `RecipeDetailRoute.kt` (`a8b6896`, `27e4800`), `MenuScreen.kt` (`2e76d8f`), `RecipeEditScreen.kt` (`5be7b4d`, `ab3c0f8`). Result: no `Color(0x...)`, `Color.White`, `Color.Black`; all colours come from `MaterialTheme.colorScheme`. `AppearanceSection` uses `Color(palette.*)` on purpose (previews and swatches drawn in the previewed theme's colours). Together with agent #9's scan of `main`: **no hard-coded colours to fix. No code change.**
- Missing: Robolectric test of AppearanceContent (tags `theme_card_<ID>`, `custom_role_<ROLE>`, `picker_*`, `appearance_reset`), manual check.

### Task 8 — Safe APK updates: IN PROGRESS, signing BLOCKED
- Done: versionCode 2 / 1.1.0, `BuildConfig.BUILD_DATE`, About shows version, build number, build date.
- Missing: release signing (F2), CI `assembleRelease`, update-over-install test, Room schema export + migration test (F4).

### Technical decisions (agent #10; earlier ones in `cc95a03`, `a9c8d61`)
| Decision | Why | Commit |
|---|---|---|
| Task 4 tests as a new file only | No risk of corrupting files reconstructed from diffs | c892225 |
| Merge PR #5 with API 34 green in one run, failing F7 in the other | Known flake in an unrelated UI test (test-only PR) | 1eb19dc |
| F7 fix in the test (retry + hide keyboard), not in the app | App behaviour is correct (passes on API 29 and in 2/5 tablet runs); the lost click is a test timing issue | 18d4bdb |
| Task 7: keep `Color(palette.*)` in AppearanceSection | Previews must show the previewed theme, not the current one | no change |

---

## D. Git history

- `main` @ `2ceef75` green (CI #11).
- Agents #1-#9: see `cc95a03` / `a9c8d61` versions of this file.
- Agent #10 (2026-11-17 02:30-03:30 UTC), all pushes verified by API response and by reading the diff back:
  - re-ran failed API 34 job of run 43976106683 (`653c783`) -> green.
  - `task4-backup-tests`: `c892225` -> PR #5 -> merged `1eb19dc`.
  - docs `123a8ab` [skip ci].
  - `f7-tablet-flake`: `18d4bdb` (`AppFlowsTest.kt`, diff +22/-4) -> PR #6 -> merged `2c61d54`.
  - this docs commit [skip ci].

---

## E. Testing

| Commit | Run | build | API 29 | API 34 |
|---|---|---|---|---|
| `653c783` (modernization push) | 43976106683 attempt 1 / attempt 2 (re-run) | green / green | green / green | FAILED F7 / green |
| `653c783` (PR #1) | 43976116784 | green | green | FAILED F7 |
| `c892225` (PR #5) | 44004559986 / 44004586584 | green / green | green / green | FAILED F7 / green |
| `1eb19dc` (modernization push / PR #1) | 44007089016 / 44007099179 | green / green | green / green | green / green |
| `18d4bdb` (PR #6, F7 fix) | 44007417894 / 44007447223 | green / green | green / green | **green / green** |

## PENDING TESTS AND CHECKS

| Commit | What | How to read | Status |
|---|---|---|---|
| `2c61d54` (modernization, merge of PR #6) | push CI + PR #1 CI | `actions_list` `list_workflow_runs` branch `modernization`; failed logs via `get_job_logs` | PENDING, not read |

---

## F. Blockers and known issues

- **F2 Signing (Task 8, user):** no release key; CI APKs use random debug keys and cannot update each other. Plan: keystore in GitHub Secrets (`KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`), `signingConfigs.release` from env vars, CI `assembleRelease`. Waiting: how the current app was installed and whether the user has the original keystore. Never generate a replacement key without the user's decision.
- **F3 Import bug file (Task 6, user):** waiting for the failing file.
- **F4 Room schemas not committed:** needed for a 1->2 `MigrationTestHelper` test. Plan: `ksp { arg("room.schemaLocation", "$projectDir/schemas") }` + `sourceSets["androidTest"].assets.srcDir("$projectDir/schemas")` + `androidTestImplementation("androidx.room:room-testing:2.6.1")` in `app/build.gradle.kts` (body known @ `8e68c59`); v1 schema must be generated from `2ceef75` (e.g. temporary CI step that checks out `2ceef75`, builds with schema export and uploads `app/schemas` as an artifact), then committed as `app/schemas/com.example.homemenu.data.db.AppDatabase/1.json`.
- **F5** `AGENT.md` and `TEST_REPORT.md` outdated.
- **F7 Flaky API 34 tablet test:** FIXED in `18d4bdb` (helper `openProductSearch()`: hide keyboard, wait for idle, click `add_ingredient`, retry up to 3x). Green in both runs of PR #6. Watch the next runs; if it fails again, read the log via `get_job_logs`.
- **F9 Possible flaky `build` job:** one unexplained failure on `8e68c59` (run 43973547049); not seen since.
- **F10 Tooling limit:** no terminal or Gradle; whole-file rewrites of large Kotlin files are high-risk. Prefer small files and verify diff stats.

---

## G. NEXT AGENT — START HERE

1. **State:** `modernization` @ `2c61d54` + this docs commit. Tasks 1, 2, 3, 5, 7 TESTED; Task 4 pure layer TESTED; Task 8 versioning TESTED, signing BLOCKED (F2); Task 6 BLOCKED (F3). F7 fixed. No open feature PRs. PR #1 stays draft.
2. **First:** read the CI of `2c61d54` (section PENDING).
3. **Next actions, in order:** F4 migration test (see plan in F) -> Task 2 instrumented tag test (new test method or new file in `app/src/androidTest`, uses the tags listed in C) -> Task 3 use-case check + README menu line -> Task 7 Robolectric AppearanceContent test -> Task 5 optional share intent -> Task 8 signing (after user answer) -> Task 6 (after user file). Update `TEST_REPORT.md`.
4. **Precautions:** never change `applicationId`; no `fallbackToDestructiveMigration`; keep legacy `cuisine` column; do not merge PR #1 until CI green and migration tested; no secrets in git; check every write's diff stats; mark PRs ready before merging.

---

## H. Manual checks for the user (on a device)

- Task 1: Recipe edit -> "Вставить списком" -> paste several lines -> "Разобрать" -> edit/remove rows -> add -> order kept.
- Task 2: Recipe edit -> tags at the very bottom; select several, create one, remove one; no "Кухня" anywhere.
- Task 3: Menu -> one meal with 3 dishes -> reorder, replace, remove -> restart app -> same list.
- Task 4: Settings -> export ZIP and JSON -> import each back -> preview shows counts and duplicates -> choose Skip / Update / Copies -> photos and menu restored.
- Task 5: Share -> "Как текст" (Sharesheet), "Скопировать текст", "Сохранить как TXT"; Recipes -> "Из текста" -> paste -> preview -> save.
- Task 7: Settings -> "Оформление" -> each of the 6 themes applies at once; "Свои цвета" -> restart -> kept; reset. Tag chips readable in every theme.
- Task 8: Settings -> "О приложении" shows версия 1.1.0, Сборка 2, Дата сборки. Update over the installed app only works with the same signing key (F2).
