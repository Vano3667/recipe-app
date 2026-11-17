# DEVELOPMENT_HANDOFF

Persistent continuation guide for AI coding agents working on this repository.
Read this file first, then `AGENT.md` (Russian summary for the user, rewritten by agent #11).
Last updated: 2026-11-17 03:35 UTC (agent #11).
Full text of earlier versions: git history of this file (agent #7 = `a9c8d61`, #8 = `09af78a`, #9 = `cc95a03`, #10 = `123a8ab`).
This version is a whole rewrite; per-task file lists and older decision tables are in `cc95a03` / `a9c8d61`.

---

## A. Project overview

| | |
|---|---|
| Project | Home Menu («Домашнее меню»), Android, Russian UI |
| Stack | Kotlin 1.9.24, Compose (BOM 2024.06), Material 3, MVVM, Room 2.6, Hilt 2.51, Navigation Compose, kotlinx.serialization, Coil. minSdk 29, target/compile 34 |
| Code | `app/src/main/java/com/example/homemenu/` (`data/db`, `data/files`, `data/repo`, `domain`, `domain/backup`, `ui/*`) |
| applicationId | `com.example.homemenu` (must never change) |
| Version | `modernization`: versionCode 2, versionName 1.1.0, `BuildConfig.BUILD_DATE`. `main`: 1 / 1.0 |
| Signing | No release key yet. PR #8 adds an env-var `signingConfigs.release` (inactive without `HOMEMENU_KEYSTORE_FILE`). CI builds debug APKs with the runner's random debug key (F2) |
| CI | `.github/workflows/android.yml` (workflow id 412025045): `build` (assembleDebug, testDebugUnitTest incl. Robolectric, lintDebug) + `instrumented (29, pixel_4)` + `instrumented (34, pixel_tablet)`. Push and pull_request events (two runs per PR commit). ~5 min build, ~10 min instrumented |
| Commands (CI) | `./gradlew assembleDebug testDebugUnitTest lintDebug`, `./gradlew connectedDebugAndroidTest` |

### Working rules
- No local JDK/Gradle/SDK; CI is the only build/test environment. Actions tools work: `actions_list` (`list_workflow_runs`, `list_workflow_jobs`), `get_job_logs` (`failed_only` + `return_content`), `actions_run_trigger` (`rerun_failed_jobs`), `pull_request_read` `get_check_runs`.
- Per task: branch from `modernization` -> draft PR into `modernization` -> green `build` + both instrumented on the head commit -> mark ready (draft=false) -> merge. `modernization` -> `main` only via PR #1 after all tasks are verified.
- Docs-only commits: `[skip ci]`, may go straight to `modernization`. Never force-push; no secrets in git.
- **Reading files (new, agent #11):** "get file contents" returns only the blob SHA. Fast full reconstruction: (1) `get_commit 87a602d detail=full_patch perPage=100` (saved as a large result) -> write every `+` line per file = original upload; (2) `pull_request_read get_diff pullNumber=1` = whole `modernization` vs `main` diff -> `git apply` onto (1), excluding files that `main` changed after `87a602d` (README, TEST_REPORT, AGENT.md, ENVIRONMENT, AppFlowsTest, AndroidManifest, PhotoStorage, PhotoPicker, RecipeEditScreen, file_paths.xml, ComposeScreensTest, android.yml: for those add the `main` patches `717fc83`, `b1b4ab6`, `f4f4043`, `2ceef75`, `abd965d`, `e3d5d1e` first). (3) **Verify** with `git hash-object <file>` against the blob SHA from `get_repository_tree tree_sha=<branch> path_filter=<file>`. Verified this way: `app/build.gradle.kts` = `2a0d72f`.
- Kotlin DSL: `import java.time.X` at the top of `app/build.gradle.kts` (FQN clashes with the `java` accessor).
- Low-risk pattern: new test files that do not touch existing files.

---

## B. Task checklist

1. [ ] Bulk ingredient input — TESTED
2. [ ] Tags + full cuisine removal — TESTED (tag UI tests missing)
3. [ ] Multiple recipes per meal — TESTED; extra tests in PR #7 (CI PENDING)
4. [ ] ZIP/JSON export/import — TESTED for the pure layer; `BackupManager` rollback + ZIP-with-photos round trip untested
5. [ ] Sharing + text import — TESTED
6. [ ] Fix existing import bug — BLOCKED, WAITING FOR USER FILE (F3)
7. [ ] Six themes + custom colours — TESTED; hard-coded colour scan of modernization-only UI files done by agent #11: clean
8. [ ] Safe APK updates — IN PROGRESS: versioning/build date TESTED; migration safety tests in PR #7 (PENDING); env-var signing config in PR #8 (PENDING); release signing BLOCKED (F2)

Statuses: NOT STARTED / IN PROGRESS / IMPLEMENTED / TESTED / VERIFIED / BLOCKED. TESTED = CI build + unit + Robolectric + lint + instrumented green; VERIFIED also needs a manual device check. **Stage 2 is NOT complete**: do not write "Этап 2 завершён" into README until all 8 are done (user request, 2026-11-17).

---

## C. Implementation status and decisions

**Global state (03:35 UTC):** `modernization` @ `2c61d54` (merge of PR #6, F7 fix; merged outside agent #11's actions) + this docs commit. Open: PR #7 (`f4-migration-test`), PR #8 (`task8-signing-config`), PR #1 draft (do not merge).

- Task 1: `domain/IngredientLineParser.kt`, `ui/recipes/BulkIngredientsDialog.kt`. Risk: if `vm.addCustomProduct` never calls back, remaining rows are not added.
- Task 2: cuisine removed from domain/UI/seed/README; `TagMultiSelect` at the bottom of the form. DB column `recipes.cuisine` and backup v1 field kept on purpose (non-destructive).
- Task 3: `menu_entries.position`, non-unique `(date, slot)` index, add/replace/reorder/move with renumbering. Agent #11 checked `MenuGeneratorUseCase` (fills only empty meals, every dish counts as used) and `ShoppingListUseCase` (sums every entry): no one-dish assumption. Tests: `domain/MultiDishMealTest.kt` (PR #7).
- Task 4: format v2 (`domain/backup/BackupFormat.kt`, `ImportAnalysis.kt`), `data/files/BackupManager.kt` staging + one transaction + photo rollback; `BackupAnalyzerTest`, `BackupJsonTest`.
- Task 5: share text/Sharesheet/clipboard/TXT, `RecipeTextParser` + import dialog. Optional: incoming `ACTION_SEND text/plain`.
- Task 7: `ui/theme/ThemePalettes.kt`, `Theme.kt`, `ui/settings/AppearanceSection.kt`. Scan for `Color(0x` / `Color.White|Black|Gray|Red|Green` in all added lines of the PR #1 diff outside `ui/theme/`: no hits.
- Task 8 / DB: `AppDatabase.MIGRATION_1_2` (additive only), `DatabaseSafety.backupBeforeMigration` (copy to `filesDir/db_backups`, keeps 5). New: `data/db/MigrationV1toV2Test.kt` (PR #7) builds v1 with Room 2.6's SQL for the `87a602d` entities, migrates with Room validation, reads all data back; failing migration leaves v1 untouched; backup taken for v1 and skipped for v2; fresh install. This replaces the planned schema-JSON + `MigrationTestHelper` approach (no v1 schema export exists).

| Decision (agent #11) | Why | Commit |
|---|---|---|
| Migration test with hand-written v1 SQL instead of exported schema | No v1 schema JSON; Room's own post-migration validation checks the full v2 schema | `be55327` |
| Signing only when `HOMEMENU_KEYSTORE_FILE` exists | No behaviour change without the user's key; never generate a replacement key | `8f69a02` |

---

## D. Git history

- `main` @ `2ceef75` (green, CI #11).
- Agents #1-#10: see earlier versions of this file.
- Agent #11 (2026-11-17 03:10-03:35 UTC):
  - `f4-migration-test`: `be55327` (MigrationV1toV2Test), `79701f5` (MultiDishMealTest) -> PR #7 (draft).
  - `task8-signing-config`: `8f69a02` (build.gradle.kts, diff +17 lines verified) -> PR #8 (draft).
  - this docs commit on `modernization` (AGENT.md rewrite + handoff) [skip ci].

---

## E. Testing

| Commit | Run | build | API 29 | API 34 |
|---|---|---|---|---|
| `18d4bdb` (PR #6, F7) | 44007417894 / 44007447223 | green | green | green (per merge message of `2c61d54`) |

## PENDING TESTS AND CHECKS

| Commit | What | How to read | Status |
|---|---|---|---|
| `be55327`, `79701f5` (PR #7) | runs 44009198088, 44009217243, 44009297706, 44009302845 | `actions_list list_workflow_runs` branch `f4-migration-test`; failures via `get_job_logs run_id failed_only return_content` | PENDING |
| `8f69a02` (PR #8) | runs 44009421604, 44009440803 | same, branch `task8-signing-config` | PENDING |
| `2c61d54` (modernization push / PR #1) | runs 44009586697, 44009593125 | same, branch `modernization` | PENDING |

Never record these as passed until read.

---

## F. Blockers and known issues

- **F2 Signing (Task 8, user):** need how the installed app was installed and the original keystore (to GitHub Secrets, e.g. `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`; CI decodes it to a file and sets `HOMEMENU_KEYSTORE_FILE` etc., then `assembleRelease`). If the app came from CI (random debug key), an update over it is impossible: export a ZIP backup first, then reinstall.
- **F3 Import bug file (Task 6, user):** waiting for the failing file + error text.
- **F5** `TEST_REPORT.md` outdated (run #10).
- **F7** flaky API 34 test: fix merged (`2c61d54`); watch the next runs.
- **F9** one unexplained `build` failure on `8e68c59`; not seen since.
- **F10** no terminal/Gradle; use the reconstruction + hash check from section A.

---

## G. NEXT AGENT — START HERE

1. **State:** `modernization` @ `2c61d54` + this docs commit. Tasks 1, 2, 3, 5, 7 TESTED; 4 pure layer TESTED; 6 BLOCKED (F3); 8 IN PROGRESS, signing BLOCKED (F2). Stage 2 not complete.
2. **First:** read every run in PENDING. Fix failures in PR #7 / PR #8 (test-only / build-script-only changes). When `build` + both instrumented are green on the head commit: mark ready, merge into `modernization`.
3. **Then:** `BackupManager` failed-import rollback + ZIP-with-photo round trip (Robolectric, new test file); tag UI tests (Task 2); bulk dialog Robolectric test (Task 1); optional `ACTION_SEND text/plain` (Task 5); update `TEST_REPORT.md`; Task 8 CI `assembleRelease` with secrets after the user supplies the key; Task 6 after the user file.
4. **Precautions:** never change `applicationId`; no `fallbackToDestructiveMigration`; keep `recipes.cuisine`; do not merge PR #1 until all tasks are verified; no secrets in git; verify every write's diff stats.

---

## H. Manual checks for the user (on a device)

- Task 1: Recipe edit -> "Вставить списком" -> paste lines -> "Разобрать" -> edit/remove rows -> add -> order kept.
- Task 2: tags at the very bottom; select several, create one, remove one; no "Кухня" anywhere.
- Task 3: Menu -> one meal with 3 dishes -> reorder, replace, remove -> restart -> same list.
- Task 4: Settings -> export ZIP and JSON -> import each back -> preview counts/duplicates -> Skip / Update / Copies -> photos and menu restored.
- Task 5: Share -> "Как текст", "Скопировать текст", "Сохранить как TXT"; Recipes -> "Из текста" -> paste -> preview -> save.
- Task 7: Settings -> "Оформление" -> each of the 6 themes applies at once; "Свои цвета" -> restart -> kept; reset.
- Task 8: Settings -> "О приложении" shows 1.1.0, сборка 2, дата сборки. Update over the installed app only with the same signing key (F2).
