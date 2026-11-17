# DEVELOPMENT_HANDOFF

Persistent continuation guide for AI coding agents working on this repository.
Read this file first, then `AGENT.md` (Russian summary for the user, rewritten by agent #11).
Last updated: 2026-11-17 03:45 UTC (agent #11).
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
| Signing | `signingConfigs.release` from env vars (`HOMEMENU_KEYSTORE_FILE`, `HOMEMENU_KEYSTORE_PASSWORD`, `HOMEMENU_KEY_ALIAS`, `HOMEMENU_KEY_PASSWORD`), inactive without them (merged `0e733b4`). No release key yet; CI builds debug APKs with the runner's random debug key (F2) |
| CI | `.github/workflows/android.yml` (workflow id 412025045): `build` (assembleDebug, testDebugUnitTest incl. Robolectric, lintDebug) + `instrumented (29, pixel_4)` + `instrumented (34, pixel_tablet)`. Push and pull_request events (two runs per PR commit). ~5 min build, ~10 min instrumented |
| Commands (CI) | `./gradlew assembleDebug testDebugUnitTest lintDebug`, `./gradlew connectedDebugAndroidTest` |

### Working rules
- No local JDK/Gradle/SDK; CI is the only build/test environment. Actions tools work: `actions_list` (`list_workflow_runs`, `list_workflow_jobs`), `get_job_logs` (`run_id` + `failed_only` + `return_content`), `actions_run_trigger` (`rerun_failed_jobs`), `pull_request_read` `get_check_runs`.
- Per task: branch from `modernization` -> draft PR into `modernization` -> green `build` + both instrumented on the head commit -> mark ready (draft=false) -> merge. `modernization` -> `main` only via PR #1 after all tasks are verified.
- Docs-only commits: `[skip ci]`, may go straight to `modernization`. Never force-push; no secrets in git.
- **Reading files (agent #11):** "get file contents" returns only the blob SHA. Fast full reconstruction: (1) `get_commit 87a602d detail=full_patch perPage=100` (saved as a large result) -> write every `+` line per file = original upload; (2) `pull_request_read get_diff pullNumber=1` = whole `modernization` vs `main` diff -> `git apply` onto (1), excluding files that `main` changed after `87a602d` (README, TEST_REPORT, AGENT.md, ENVIRONMENT, AppFlowsTest, AndroidManifest, PhotoStorage, PhotoPicker, RecipeEditScreen, file_paths.xml, ComposeScreensTest, android.yml: for those add the `main` patches `717fc83`, `b1b4ab6`, `f4f4043`, `2ceef75`, `abd965d`, `e3d5d1e` first). (3) **Verify** with `git hash-object <file>` against the blob SHA from `get_repository_tree tree_sha=<branch> path_filter=<file>`. Verified this way: `app/build.gradle.kts` = `2a0d72f` (before PR #8).
- Kotlin DSL: `import java.time.X` at the top of `app/build.gradle.kts` (FQN clashes with the `java` accessor).
- Low-risk pattern: new test files that do not touch existing files.
- Feature branches created before `2c61d54` lack the F7 test fix, so their API 34 job may fail on `addRecipeWithIngredient` (`product_search`): that is F7, not the change.

---

## B. Task checklist

1. [ ] Bulk ingredient input — TESTED
2. [ ] Tags + full cuisine removal — TESTED (tag UI tests missing)
3. [ ] Multiple recipes per meal — TESTED (incl. migration test and multi-dish use-case tests, merged `301f98e`)
4. [ ] ZIP/JSON export/import — TESTED for the pure layer; `BackupManager` rollback + ZIP-with-photos round trip untested
5. [ ] Sharing + text import — TESTED
6. [ ] Fix existing import bug — BLOCKED, WAITING FOR USER FILE (F3)
7. [ ] Six themes + custom colours — TESTED; hard-coded colour scan of modernization-only UI files done by agent #11: clean
8. [ ] Safe APK updates — IN PROGRESS: versioning/build date, migration safety tests (`301f98e`) and env-var signing config (`0e733b4`) TESTED; release signing with the original key BLOCKED (F2); update-over-install not tested

Statuses: NOT STARTED / IN PROGRESS / IMPLEMENTED / TESTED / VERIFIED / BLOCKED. TESTED = CI build + unit + Robolectric + lint + instrumented green; VERIFIED also needs a manual device check. **Stage 2 is NOT complete**: do not write "Этап 2 завершён" into README until all 8 are done (user request, 2026-11-17).

---

## C. Implementation status and decisions

**Global state (03:45 UTC):** `modernization` @ `0e733b4` (merge of PR #8) after `301f98e` (merge of PR #7), `b66c2f2` (docs), `2c61d54` (merge of PR #6, F7 fix; merged outside agent #11's actions) + this docs commit. No open feature PRs. PR #1 draft (do not merge).

- Task 1: `domain/IngredientLineParser.kt`, `ui/recipes/BulkIngredientsDialog.kt`. Risk: if `vm.addCustomProduct` never calls back, remaining rows are not added.
- Task 2: cuisine removed from domain/UI/seed/README; `TagMultiSelect` at the bottom of the form. DB column `recipes.cuisine` and backup v1 field kept on purpose (non-destructive).
- Task 3: `menu_entries.position`, non-unique `(date, slot)` index, add/replace/reorder/move with renumbering. `MenuGeneratorUseCase` fills only empty meals and counts every dish as used; `ShoppingListUseCase` sums every entry. Tests: `domain/MultiDishMealTest.kt`.
- Task 4: format v2 (`domain/backup/BackupFormat.kt`, `ImportAnalysis.kt`), `data/files/BackupManager.kt` staging + one transaction + photo rollback; `BackupAnalyzerTest`, `BackupJsonTest`.
- Task 5: share text/Sharesheet/clipboard/TXT, `RecipeTextParser` + import dialog. Optional: incoming `ACTION_SEND text/plain`.
- Task 7: `ui/theme/ThemePalettes.kt`, `Theme.kt`, `ui/settings/AppearanceSection.kt`. Scan for `Color(0x` / `Color.White|Black|Gray|Red|Green` in all added lines of the PR #1 diff outside `ui/theme/`: no hits.
- Task 8 / DB: `AppDatabase.MIGRATION_1_2` (additive only), `DatabaseSafety.backupBeforeMigration` (copy to `filesDir/db_backups`, keeps 5). `data/db/MigrationV1toV2Test.kt` builds v1 with Room 2.6's SQL for the `87a602d` entities, migrates with Room's validation, reads all data back; failing migration leaves v1 untouched and still upgradable; backup taken for v1 and skipped for v2; fresh install.

| Decision (agent #11) | Why | Commit |
|---|---|---|
| Migration test with hand-written v1 SQL instead of exported schema | No v1 schema JSON exists; Room's post-migration validation checks the full v2 schema | `be55327` |
| Signing only when `HOMEMENU_KEYSTORE_FILE` exists | No behaviour change without the user's key; never generate a replacement key | `8f69a02` |
| Merge PR #7 although run 44009302845 failed on API 34 | Failure = F7 `product_search` test (branch predates the F7 fix); same head green in run 44009297706; change is test-only | `301f98e` |

---

## D. Git history

- `main` @ `2ceef75` (green, CI #11).
- Agents #1-#10: see earlier versions of this file.
- Agent #11 (2026-11-17 03:10-03:45 UTC):
  - `f4-migration-test`: `be55327` (MigrationV1toV2Test), `79701f5` (MultiDishMealTest) -> PR #7 -> merged `301f98e`.
  - `task8-signing-config`: `8f69a02` (build.gradle.kts, +17 lines verified) -> PR #8 -> merged `0e733b4`.
  - docs on `modernization`: `b66c2f2` (AGENT.md rewrite + handoff) and this commit, both [skip ci].

---

## E. Testing

| Commit | Run | build | API 29 | API 34 |
|---|---|---|---|---|
| `18d4bdb` (PR #6, F7) | 44007417894 / 44007447223 | green | green | green (per merge message of `2c61d54`) |
| `79701f5` (PR #7 head) | 44009297706 (push) | green | green | green |
| `79701f5` (PR #7 head) | 44009302845 (PR) | green | green | FAILED: F7 `addRecipeWithIngredient` `product_search` 15 s (branch without F7 fix) |
| `8f69a02` (PR #8 head) | 44009421604 (push) | green | green | green |
| `8f69a02` (PR #8 head) | 44009440803 (PR) | green | green | green |

The new Robolectric tests (`MigrationV1toV2Test`, `MultiDishMealTest`) ran inside `build` (testDebugUnitTest) and passed.

## PENDING TESTS AND CHECKS

| Commit | What | How to read | Status |
|---|---|---|---|
| `2c61d54` (modernization push / PR #1) | runs 44009586697, 44009593125 | `actions_list list_workflow_runs` branch `modernization`; failures via `get_job_logs run_id failed_only return_content` | PENDING, not read |
| `301f98e`, `0e733b4` (merges into modernization) | push CI + PR #1 CI | same | PENDING, not read |

Never record these as passed until read.

---

## F. Blockers and known issues

- **F2 Signing (Task 8, user):** need how the installed app was installed and the original keystore (to GitHub Secrets, e.g. `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`; a CI step decodes it to a file, exports the `HOMEMENU_*` env vars and runs `assembleRelease`). If the app came from CI (random debug key), an update over it is impossible: export a ZIP backup first, then reinstall.
- **F3 Import bug file (Task 6, user):** waiting for the failing file + error text.
- **F5** `TEST_REPORT.md` outdated (run #10).
- **F7** flaky API 34 test: fix merged (`2c61d54`); watch the next runs on `modernization`.
- **F9** one unexplained `build` failure on `8e68c59`; not seen since.
- **F10** no terminal/Gradle; use the reconstruction + hash check from section A.

---

## G. NEXT AGENT — START HERE

1. **State:** `modernization` @ `0e733b4` + this docs commit. Tasks 1, 2, 3, 5, 7 TESTED; 4 pure layer TESTED; 6 BLOCKED (F3); 8 IN PROGRESS, signing BLOCKED (F2). Stage 2 not complete.
2. **First:** read every run in PENDING (modernization push CI of `2c61d54`, `301f98e`, `0e733b4`). If only API 34 fails on F7, re-run failed jobs and reopen F7.
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
