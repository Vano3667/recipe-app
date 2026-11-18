# DEVELOPMENT_HANDOFF

Persistent continuation guide for AI coding agents working on `Vano3667/recipe-app`.
Read this file first, then `AGENT.md`, `manual user checks.md` (user test results, Section 23) and
`recipe_app_autonomous_development_prompt (1).md` (full specification).
Last updated: 2026-11-18 ~02:50 UTC (agent Jordan, monday.com). Session notes in `docs/handoff/` are merged here.

---

## A. Project overview

| | |
|---|---|
| Project | Home Menu («Домашнее меню»), Android, Russian UI |
| Stack | Kotlin 1.9.24, Compose (BOM 2024.06), Material 3, MVVM, Room 2.6, Hilt 2.51, Navigation Compose, kotlinx.serialization 1.6.3, Coil. minSdk 29, target/compile 34 |
| Code | `app/src/main/java/com/example/homemenu/` (`data/db`, `data/files`, `data/repo`, `domain`, `domain/backup`, `ui/*`) |
| applicationId | `com.example.homemenu` (must never change) |
| Version | versionCode 2, versionName 1.1.0, `BuildConfig.BUILD_DATE` |
| CI | `.github/workflows/android.yml` (id 412025045): `build` (assembleDebug, testDebugUnitTest incl. Robolectric, lintDebug, uploads `home-menu-debug-apk`) + `instrumented` API 29 pixel_4 and API 34 pixel_tablet. Push + pull_request |
| Commands | `./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug`, `./gradlew :app:connectedDebugAndroidTest` |
| User fixture | `recipes_from_budget_classic_1500kcal.json` (repo root, 439,609 bytes, never modify). Read by `app/src/test/.../domain/UserFixture.kt` |

### Working rules / environment
- The monday.com agent has no local git/JDK/SDK. CI (GitHub Actions) is the only build/test environment.
- File bodies cannot be read through the GitHub connector (`get_file_contents` returns only the blob SHA).
  **Working technique:** branch `agent-diag` holds a temporary read-only workflow `.github/workflows/agent-diag.yml`;
  edit it to `cat`/`git show` the files you need, push, then read the job log with `get_job_logs return_content=true`.
  It never builds or writes anything. The branch can be deleted by the owner when no longer useful.
- Branch flow: feature branch from `modernization` -> PR -> green CI -> merge -> `modernization` -> `main`.
- Docs-only commits use `[skip ci]`. No force-push, no history rewrite, no destructive migrations, no secrets.
- User-added root file `new job after apk is finished` is left untouched; its text is not part of the spec.

---

## B. Task checklist (Stage 2)

Statuses: NOT STARTED / IN PROGRESS / IMPLEMENTED / TESTED (CI green) / VERIFIED (also user device check) / BLOCKED.

1. Bulk ingredient input — TESTED; user: add/parse/delete WORKING. Reorder fix (23.4) TESTED, USER RETEST REQUIRED.
2. Tags + cuisine removal — TESTED; user reports WORKING.
3. Multiple recipes per meal — TESTED (incl. v1->v2 migration test, multi-dish tests); user reports WORKING.
4. ZIP/JSON export/import — TESTED: pure layer + new DB-level tests (fixture import, re-import SKIP, JSON round trip, ZIP round trip with photo). User reports general import/export WORKING.
5. Sharing + text import + shopping list share for WhatsApp/Telegram (23.10) — TESTED, USER RETEST REQUIRED.
6. Fix import bug with the real fixture (23.2) — TESTED (CI), USER RETEST REQUIRED.
7. Six themes + custom colours + compact selector (23.3/23.8) — TESTED, USER RETEST REQUIRED.
8. Safe APK updates (incl. 23.9) — DEFERRED by the user for this iteration. Do not work on it until asked.

**Stage 2 (excluding Task 8) is complete** and announced at the top of README.md (2026-11-18).

---

## USER TESTING AND REPORTED ISSUES

| Test ID | Task | Feature / issue | User result | Developer result | Status | Next action | Commit |
|---|---|---|---|---|---|---|---|
| UT-23.2 | 6 / 4 | JSON import of `recipes_from_budget_classic_1500kcal.json` | NOT WORKING | REPRODUCED from the file (fixture statistics + test `root cause - a strict reader rejects explicit nulls`): explicit `null` in non-nullable fields (`cookingTimeMin`, `difficulty`, `category` in all 140 recipes; `amount`, `productUnit` in all 185 TO_TASTE ingredients) made kotlinx.serialization throw, so the whole file was rejected. Fix: `BackupJson.json` `coerceInputValues = true`, `BackupIngredient.amount = 0.0` / `unit = "G"` defaults. Verified in DB: 140 recipes, 841 ingredients, 692 steps, names/amounts/units/order, tags | DEVELOPER TESTED, USER RETEST REQUIRED | Settings -> Import -> the JSON file -> expect 140 recipes | `47a6c79`, tests `51bf521`, `a0dd4cb` |
| UT-23.1 | (APK) | Signature mismatch installing the 2026-11-17 17:08 (UTC+11) build | reported | Build = CI run 44025353134 artifact `home-menu-debug-apk` (06:08 UTC). CI publishes a debug APK; no debug signing config, so each fresh runner generates a random `~/.android/debug.keystore`: every CI APK has a different certificate. No original key exists to restore. If the error appears after uninstalling, the package is still installed for another user/profile (work profile, Second Space, guest) or was uninstalled with keep-data | BLOCKED (permanent key = Task 8, deferred) | user: uninstall for all users (`adb shell pm list packages --user all`), keep ZIP backup; later Task 8 | none |
| UT-23.4 | 1 | Ingredient reordering | NOT WORKING | Edit form had no reorder control; up/down buttons (`ingredient_up_<i>`/`ingredient_down_<i>`), `IngredientOrder.move`; save writes position = index; `toDomain` sorts by position | DEVELOPER TESTED (CI), USER RETEST REQUIRED | move, save, reopen | `4362222`, merge `4e33f7f` |
| UT-23.3 | 7 | Compact theme selector, 2 per row | needs redesign | One-line dropdown (`theme_dropdown`) opens a 2-column grid (`theme_row_N`, `theme_card_<ID>`), all 6 themes, instant apply, custom colours unchanged; `AppearanceDropdownTest` | DEVELOPER TESTED (CI), USER RETEST REQUIRED | check on phone + tablet | `8d3ae34`, merge `63f950b` |
| UT-23.10 | 5 | Shopping list to WhatsApp/Telegram with checkboxes | requested | Android Sharesheet; whole list or only items still to buy; plain text with ☐/☑ + "Куплено X из Y". ☐/☑ are NOT interactive. WhatsApp normal messages have no checklists; Telegram checklists cannot be created from an Android share intent (only inside Telegram). Practical workflow: tick in the app, re-share "only remaining" | DEVELOPER TESTED (CI), USER RETEST REQUIRED | share to both apps | `8c4bab5`, merge `e4c16b2` |
| UT-23.5 | 2 | Tags | WORKING | regression via CI only | USER REPORTED — NOT REPRODUCED (no issue) | — | — |
| UT-23.6 | 3 | Multiple dishes per meal | WORKING | migration + multi-dish tests in CI | USER REPORTED — NOT REPRODUCED (no issue) | — | — |
| UT-23.7 | 4 | ZIP/JSON import/export | WORKING (general) | DB-level round trips added (JSON and ZIP with photo) | DEVELOPER TESTED | — | `51bf521` |
| UT-23.9 | 8 | Version info / update over install | version shown | not worked on (Task 8 deferred) | DEFERRED | — | — |

---

## C. Decisions

| Decision | Why |
|---|---|
| (Jordan) Null -> default when reading backups (`coerceInputValues`) | Real user file contains explicit nulls; rejecting the whole file for optional fields is wrong. Export output unchanged. Wrong types are still rejected (`wrong field types are rejected` test) |
| (Jordan) Fixture read from the repo root, never copied/edited | Spec 23.2.7. A test asserts its size and that it still contains the nulls |
| (Jordan) ZIP round trip calls private `writeZip` via reflection | Robolectric `ContentResolver.openOutputStream` for file URIs is unreliable; reflection exercises the same code as `exportZip` |
| (Jordan) main merged into the work branch through PR #13 | Brings the fixture/spec into the code branches; both sides had the identical handoff, so no conflict |
| (#11) Signing only when `HOMEMENU_KEYSTORE_FILE` exists | Never generate a replacement key |
| (#12) Shared text via `SharedTextInbox` | No nav signature changes |

---

## D. Git history (this session, 2026-11-18)

- `agent-diag` from `modernization`: diagnostics workflow only (`63ad8b9` ... `3344c7f`).
- `task6-json-import-fix` from `modernization` `014a1d7`: `47a6c79` fix(import); `401f2bf` handoff; `b6a37c9` merge of `main` (PR #13); `51bf521` fixture tests; `a0dd4cb` test fix; `77a369a` docs (README announcement, handoff, AGENT.md).
- PR #14 -> `modernization` merged as `6ae5d81`. PR #1 `modernization` -> `main` merged as `4989e62` (main now contains all Stage 2 work). Then this docs commit on `main` (and the same file on `modernization`).

---

## E. Testing

| Commit | Run | build (unit + Robolectric + lint) | API 29 | API 34 |
|---|---|---|---|---|
| `51bf521` fix + fixture tests | 44222596353 | PASSED | PASSED | PASSED |
| `a0dd4cb` (test-only change vs `51bf521`) | 44222690821 attempt 1 | PASSED | PASSED | FAILED: `AppFlowsTest.shoppingListIsBuiltFromMenu` waited 15 s for `gen_confirm` (UI timing flake F12; same production code passed in 44222596353) |
| `a0dd4cb` | 44222690821 attempt 2 (re-run of API 34 only) | — | — | NOT RUN: job failed in 3 s without a runner (F13) |
| `6ae5d81` modernization merge | 44225149576 (push), 44225155544 (PR) | NOT RUN (F13) | skipped | skipped |
| `4989e62` main merge | 44225182905 | NOT RUN (F13) | skipped | skipped |

The code on `6ae5d81` / `4989e62` equals `a0dd4cb` plus docs (README, handoff, AGENT.md). Last full green run for this production code: 44222596353.

### PENDING TESTS AND CHECKS

| Check | How to read | Status |
|---|---|---|
| CI on `main` `4989e62` (and later docs commits) | `actions_list list_workflow_runs` branch `main`; when runners work again: `actions_run_trigger rerun_workflow_run run_id=44225182905` | PENDING — blocked by F13 |
| CI on `modernization` `6ae5d81` | rerun 44225149576 | PENDING — blocked by F13 |
| User device retests UT-23.2, 23.3, 23.4, 23.10 | user | PENDING |

---

## F. Blockers and known issues

- **F13 GitHub Actions jobs no longer start (since ~02:38 UTC 2026-11-18):** every new job ends as `failure` within 2-3 s, with no runner, no steps and no log (log download returns 404). This is an account/runner-level problem (typically: Actions minutes or spending limit for private repositories reached, or a billing lock / GitHub outage). Owner: check GitHub -> Settings -> Billing and plans -> Actions usage / spending limit, then re-run the runs in E.
- **F2 Signing (UT-23.1, Task 8 deferred):** needs a permanent keystore in GitHub Secrets (`HOMEMENU_KEYSTORE_BASE64`, `HOMEMENU_KEYSTORE_PASSWORD`, `HOMEMENU_KEY_ALIAS`, `HOMEMENU_KEY_PASSWORD`); then the workflow decodes it, exports `HOMEMENU_KEYSTORE_FILE` and runs `assembleRelease`. Never generate a replacement key and call it compatible.
- **F11** Robolectric `android-all` download flake: re-run failed jobs.
- **F12** `AppFlowsTest.shoppingListIsBuiltFromMenu` can time out on API 34 tablet waiting for `gen_confirm`. Possible hardening: same retry/IME-close helper as F7.
- **F5** `TEST_REPORT.md` outdated (run #10); this file is the current test record.

---

## G. NEXT AGENT — START HERE

1. Check F13. When jobs start again, re-run 44225182905 (main) and 44225149576 (modernization) and record results in E.
2. If API 34 fails on F12 again, harden `AppFlowsTest` (F7 helper) in a branch from `modernization`.
3. Latest code: `main` `4989e62` == `modernization` `6ae5d81` (+ this docs commit on both).
4. Wait for user retest results for UT-23.2/23.3/23.4/23.10 and update the table above.
5. Task 8 only when the user asks (needs the keystore secrets).
6. Files to inspect first: `domain/backup/BackupFormat.kt`, `data/files/BackupManager.kt`, `app/src/test/.../data/files/FixtureImportDatabaseTest.kt`.
