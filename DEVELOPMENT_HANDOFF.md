# DEVELOPMENT_HANDOFF

Persistent continuation guide for AI coding agents working on this repository.
Read this file first, then `AGENT.md` (older notes, Russian; its "commit to main" rule is superseded).
Last updated: 2026-11-17 03:05 UTC (agent #10 "Beverly").
Full text of earlier versions: git history of this file (agent #7 = `a9c8d61`, agent #8 = `09af78a`, agent #9 = `cc95a03`).
This version was rewritten whole by agent #10. The per-task "Important files" list and the older decision tables are NOT repeated here: read them in `cc95a03` / `a9c8d61`.

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
| CI | `.github/workflows/android.yml` (workflow id 412025045): job `build` (assembleDebug, testDebugUnitTest incl. Robolectric, lintDebug) + `instrumented (29, pixel_4)` + `instrumented (34, pixel_tablet)`. Runs on push and on pull_request (two runs per PR commit). ~5 min build, ~10 min instrumented. |
| Commands (CI) | `./gradlew assembleDebug testDebugUnitTest lintDebug`, `./gradlew connectedDebugAndroidTest` |
| Other docs | `README.md`, `DESIGN.md`, `TEST_REPORT.md` (outdated, run #10), `ENVIRONMENT.md`, `CREDITS.md`, `AGENT.md` (outdated) |

### Working rules
- No local JDK/Gradle/SDK. Code changes go through the GitHub API; **CI is the only build/test environment**.
- **Per task: feature branch from `modernization`, commits there, draft PR into `modernization`, read its checks, mark ready (draft=false), merge only when `build` and both instrumented jobs are green in at least one run on the head commit.** `modernization` -> `main` only via PR #1 after all tasks are verified and migration is tested.
- Docs-only commits: `[skip ci]`, may go straight to `modernization`.
- Never force-push, rewrite history, or commit keystores/passwords/tokens.
- Tooling: "get file contents" returns only the blob SHA. Read bodies from commit diffs (`get_commit` `full_patch`). `87a602d` = original upload of every file; `perPage=1, page=N` returns one file (README = 4, build.gradle.kts = 6, MainActivity = 11, Mappers = 12, Repositories = 18, SeedData = 19, Seeder = 20, Models = 27, MainViewModel = 28, Components = 29, MenuScreen = 32, MenuViewModel = 33, AppNav = 34, RecipeDetailScreen = 35, RecipeEditScreen = 36, RecipeViewModels = 37, RecipesScreen = 38, SettingsScreen = 39, SettingsViewModel = 40, Color.kt = 43, Theme.kt = 44, **BackupJsonTest = 53**). Reconstruct = original + every later patch (`list_commits` with `path`). After each write, check the commit's diff stats.
- **New (agent #10): Actions tools ARE available.** `actions_list` (`list_workflow_runs`, `list_workflow_jobs` with run id), `get_job_logs` (`run_id` + `failed_only=true` + `return_content=true`, or `job_id`) return real logs; `actions_run_trigger` `rerun_failed_jobs` re-runs a flaky job. No need to ask the user for logs.
- **Kotlin DSL gotcha:** in `app/build.gradle.kts` use `import java.time.X` at the top (FQN clashes with the `java` accessor).
- Low-risk pattern: new test files that do not touch existing files (agent #10 used it for Task 4).

---

## B. Task checklist

1. [ ] Bulk ingredient input — TESTED (CI green; manual UI check pending)
2. [ ] Tags + full cuisine removal — TESTED (code + README clean; tag UI tests pending)
3. [ ] Multiple recipes per meal — TESTED (CI green; manual UI check and migration test pending)
4. [ ] Complete ZIP and JSON export/import — TESTED for the pure layer (analyzer/manifest/v2 round trip, `BackupAnalyzerTest`, merged `1eb19dc`); `BackupManager` staging/rollback and instrumented read-back still untested
5. [ ] Recipe sharing + text import — TESTED (CI green; manual Sharesheet/TXT check pending)
6. [ ] Fix existing import bug — BLOCKED, WAITING FOR USER FILE (F3)
7. [ ] Six themes + custom colours — TESTED (merged; colour scan of modernization-only UI files + UI test pending)
8. [ ] Safe APK updates — IN PROGRESS (versioning + build date TESTED); signing BLOCKED (F2)

Statuses: NOT STARTED / IN PROGRESS / IMPLEMENTED / TESTED / VERIFIED / BLOCKED. TESTED = CI build + unit + Robolectric + lint + instrumented green; VERIFIED also needs a manual device check.

---

## C. Implementation status

**Global state (2026-11-17 03:05 UTC):** `modernization` @ `1eb19dc` (merge of PR #5) + this docs commit. No open feature PRs. PR #1 (`modernization` -> `main`) stays draft, do not merge.

### Task 1 — Bulk ingredient input: TESTED
- Parser `domain/IngredientLineParser.kt` + tests; dialog `ui/recipes/BulkIngredientsDialog.kt` (paste -> "Разобрать" -> editable preview -> "Добавить (n)"). Order preserved.
- Missing: Robolectric/instrumented dialog test; risk: if `vm.addCustomProduct` never calls back, remaining rows are not added.

### Task 2 — Tags + cuisine removal: TESTED
- Cuisine removed from domain, edit state, detail, edit form, seed data, README. Tags multi-select at the bottom (`TagMultiSelect`), case-insensitive dedupe.
- Kept on purpose: DB column `recipes.cuisine` and backup v1 field (non-destructive, old backups readable).
- Missing: tests for tag select/create/remove.

### Task 3 — Multiple recipes per meal: TESTED
- `menu_entries.position`, non-unique `(date, slot)` index, add/replace/reorder/move/clear with renumbering. UI in `MenuScreen.kt`.
- Missing: check `MenuGeneratorUseCase` / `ShoppingListUseCase` for one-dish assumptions; dishes()/reorder tests; migration test (F4); README menu line.

### Task 4 — ZIP/JSON export/import: TESTED (pure layer)
- Format v2 (`domain/backup/BackupFormat.kt`: manifest with SHA-256, Base64 images for standalone JSON, positions, description/prep/oven, themeId/customColors; v1 readable), `domain/backup/ImportAnalysis.kt` (`BackupAnalyzer`, `DuplicateMode` SKIP/UPDATE/COPY, `ImportPreview`, `ImportReport`), `data/files/BackupManager.kt` staging + one transaction + photo rollback, Settings UI.
- **Agent #10:** `app/src/test/java/com/example/homemenu/domain/BackupAnalyzerTest.kt` (`c892225`, PR #5, merged `1eb19dc`): key normalisation, referenced photos, analyze (counts, duplicates, missing photos, orphan menu rows, blank names, tag dedupe, extra warnings), `isImage` signatures, `safePhotoName` path traversal, `ImportReport.summary`, manifest round trip + broken manifest, v2 round trip with dish order/settings/images. All green in `build`.
- Missing: `BackupManager` failed-import rollback test (needs Robolectric or instrumented DB), ZIP round trip with real photo files, instrumented import read-back.

### Task 5 — Sharing + text import: TESTED
- Share text + Sharesheet + clipboard + TXT export; text parser + import UI (`RecipeTextImport.kt`, `TextImportDialog.kt`); always creates a NEW recipe.
- Optional: incoming `ACTION_SEND text/plain` intent-filter; Robolectric dialog test.

### Task 6 — Existing import bug: BLOCKED — WAITING FOR USER FILE
- Requested by agents #5-#10. When it arrives: reproduce in a unit test against `BackupJson.decode` / `BackupAnalyzer` / `BackupManager` staging, find root cause, fix, verify DB and photos.

### Task 7 — Themes + custom colours: TESTED (merged `d71f480`)
- Six themes + custom colour picker, persisted. Agent #9 scan on `main`: no stray hard-coded colours outside `ui/theme/Color.kt`; tag chips keep their accessible palette.
- Missing: scan of modernization-only UI files (`BulkIngredientsDialog.kt`, `TextImportDialog.kt`, `RecipeTextImport.kt`, `RecipeDetailRoute.kt`, `AppearanceSection.kt`, new code in `MenuScreen.kt` / `RecipeEditScreen.kt`) for `Color(0x`, `Color.White`, `Color.Black`; Robolectric test of AppearanceContent.

### Task 8 — Safe APK updates: IN PROGRESS, signing BLOCKED
- Done: versionCode 2 / 1.1.0, `BuildConfig.BUILD_DATE`, About shows version, build number, build date. applicationId/namespace unchanged.
- Missing: release signing (F2), CI `assembleRelease`, update-over-install test, Room schema export + migration test (F4).

### Technical decisions (agent #10; earlier ones in `cc95a03`, `a9c8d61`)
| Decision | Why | Commit |
|---|---|---|
| Task 4 tests as a new file only | No risk of corrupting existing files reconstructed from diffs | c892225 |
| Merge PR #5 with API 34 green in run 44004586584 although it failed in 44004559986 | Same failure as F7 in a UI test unrelated to the change (test-only PR) | 1eb19dc |

---

## D. Git history

- `main` @ `2ceef75` green (CI #11).
- Agents #1-#9: see `cc95a03` / `a9c8d61` versions of this file.
- Agent #10 (2026-11-17 02:30-03:05 UTC):
  - re-ran failed API 34 job of run 43976106683 (push CI of `653c783`) -> green.
  - `task4-backup-tests`: `c892225` (new test file) -> PR #5 -> merged `1eb19dc` (merge API `merged: true`).
  - this docs commit on `modernization` [skip ci].

---

## E. Testing

| Commit | Run | build | API 29 | API 34 |
|---|---|---|---|---|
| `653c783` (modernization push) | 43976106683 attempt 1 | green | green | FAILED (`AppFlowsTest.addRecipeWithIngredient`: "product_search" not found in 15000 ms) |
| `653c783` (modernization push) | 43976106683 attempt 2 (re-run by agent #10) | green | green | green |
| `653c783` (PR #1) | 43976116784 | green | green | FAILED (same test) |
| `c892225` (PR #5) | 44004559986 | green | green | FAILED (same test) |
| `c892225` (PR #5) | 44004586584 | green | green | green |

## PENDING TESTS AND CHECKS

| Commit | What | How to read | Status |
|---|---|---|---|
| `1eb19dc` (modernization, merge of PR #5) | push CI | `actions_list` `list_workflow_runs` branch `modernization`; failed logs via `get_job_logs` | PENDING, not read |

---

## F. Blockers and known issues

- **F2 Signing (Task 8, user):** no release key; CI APKs use random debug keys and cannot update each other. Plan: keystore in GitHub Secrets (`KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`), `signingConfigs.release` from env vars, CI `assembleRelease`. Waiting: how the current app was installed and whether the user has the original keystore. Never generate a replacement key without the user's decision.
- **F3 Import bug file (Task 6, user):** waiting for the failing file.
- **F4 Room schemas not committed:** needed for a 1->2 `MigrationTestHelper` test. Generate v1 schema from `2ceef75` in CI, commit `app/schemas/.../1.json`, add `room-testing`.
- **F5** `AGENT.md` and `TEST_REPORT.md` outdated.
- **F7 Flaky instrumented test on API 34 (pixel_tablet 2560x1600), now frequent:** `AppFlowsTest.addRecipeWithIngredient` fails at `waitTag("product_search")` (AppFlowsTest.kt:51, 15 s) in 3 of the last 5 API 34 runs; always green on API 29. `AppFlowsTest.kt` unchanged since `f4f4043`; `RecipeEditScreen.kt` changed (Task 1 bulk button, Task 2 tags at bottom). Hypothesis: on the tablet layout the ingredient add control / product search is off-screen or behind the new "Вставить списком" button, so the click before `waitTag` does not open it reliably. Fix: reconstruct `AppFlowsTest.kt` (`87a602d` + `717fc83` + `b1b4ab6` + `f4f4043`), scroll to the node (`performScrollTo`) before clicking, use `useUnmergedTree`, raise the wait to 30 s.
- **F9 Possible flaky `build` job:** one unexplained failure on `8e68c59` (run 43973547049); not seen since.
- **F10 Tooling limit:** no terminal or Gradle; whole-file rewrites of large Kotlin files are high-risk. Prefer small files and verify diff stats.

---

## G. NEXT AGENT — START HERE

1. **State:** `modernization` @ `1eb19dc` + this docs commit. Tasks 1, 2, 3, 5, 7 TESTED; Task 4 pure layer TESTED; Task 8 versioning TESTED, signing BLOCKED (F2); Task 6 BLOCKED (F3). No open feature PRs. PR #1 stays draft.
2. **First:** read the push CI of `1eb19dc` (section PENDING). If only API 34 fails on the F7 test, re-run failed jobs.
3. **Next action:** fix F7 (branch `f7-tablet-flake`, see F7 for the plan), then Task 7 scan of modernization-only UI files, then F4 migration test, then Task 3 README line + use-case check, then Task 5 optional share intent, Task 8 signing (after user answer), Task 6 (after user file). Update `TEST_REPORT.md`.
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
