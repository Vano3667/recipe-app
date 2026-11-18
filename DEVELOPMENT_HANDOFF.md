# DEVELOPMENT_HANDOFF

Persistent continuation guide for AI coding agents working on `Vano3667/recipe-app`.
Read this file first, then `AGENT.md`, `manual user checks.md` (user test results, Section 23) and
`recipe_app_autonomous_development_prompt (1).md` (full specification).
Last updated: 2026-11-18 (agent Jordan, monday.com). Session notes in `docs/handoff/` are merged here.

---

## A. Project overview

| | |
|---|---|
| Project | Home Menu («Домашнее меню»), Android, Russian UI |
| Stack | Kotlin 1.9.24, Compose (BOM 2024.06), Material 3, MVVM, Room 2.6, Hilt 2.51, Navigation Compose, kotlinx.serialization 1.6.3, Coil. minSdk 29, target/compile 34 |
| Code | `app/src/main/java/com/example/homemenu/` (`data/db`, `data/files`, `data/repo`, `domain`, `domain/backup`, `ui/*`) |
| applicationId | `com.example.homemenu` (must never change) |
| Version | versionCode 2, versionName 1.1.0, `BuildConfig.BUILD_DATE` |
| CI | `.github/workflows/android.yml`: `build` (assembleDebug, testDebugUnitTest incl. Robolectric, lintDebug, uploads `home-menu-debug-apk`) + `instrumented` API 29 pixel_4 and API 34 pixel_tablet |
| Commands | `./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug`, `./gradlew :app:connectedDebugAndroidTest` |

### Working rules / environment
- The monday.com agent has no local git/JDK/SDK. CI (GitHub Actions) is the only build/test environment.
- File bodies cannot be read through the GitHub connector. **Working technique (this session):** a temporary
  read-only workflow on branch `agent-diag` (`.github/workflows/agent-diag.yml`) prints files / fixture
  statistics into the job log; the log is read with `get_job_logs`. Never commit secrets; the diag workflow has
  read-only token permissions.
- Branch flow: feature branch from `modernization` -> PR -> green CI -> merge. `modernization` -> `main` at the end.
- Docs-only commits use `[skip ci]`. No force-push, no history rewrite, no destructive migrations.
- User-added root file `new job after apk is finished` is left untouched; its text is not part of the spec.

---

## B. Task checklist (Stage 2)

Statuses: NOT STARTED / IN PROGRESS / IMPLEMENTED / TESTED (CI green) / VERIFIED (also user device check) / BLOCKED.

1. Bulk ingredient input — TESTED. Reorder fix (23.4) TESTED, USER RETEST REQUIRED.
2. Tags + cuisine removal — TESTED. USER REPORTED WORKING.
3. Multiple recipes per meal — TESTED (incl. v1->v2 migration test). USER REPORTED WORKING.
4. ZIP/JSON export/import — TESTED pure layer; DB-level round trip test added this session (see E).
5. Sharing + text import + shopping list share (23.10) — TESTED, USER RETEST REQUIRED.
6. Fix import bug with real fixture (23.2) — FIX IMPLEMENTED (root cause found), CI see E.
7. Six themes + custom colours + compact selector (23.3) — TESTED, USER RETEST REQUIRED.
8. Safe APK updates — DEFERRED by the user for this iteration (do not work on it).

---

## USER TESTING AND REPORTED ISSUES

| Test ID | Task | Feature / issue | User result | Developer result | Status | Next action | Commit |
|---|---|---|---|---|---|---|---|
| UT-23.2 | 6 / 4 | JSON import of `recipes_from_budget_classic_1500kcal.json` | NOT WORKING | REPRODUCED from the file: explicit `null` in non-nullable fields (`cookingTimeMin`, `difficulty`, `category` in all 140 recipes; `amount`, `productUnit` in 185 TO_TASTE ingredients) made kotlinx.serialization throw, so the whole import failed. Fixed in `BackupFormat.kt` (`coerceInputValues`, defaults for `amount`/`unit`). Regression tests use the unmodified fixture. | FIX IMPLEMENTED | Read CI; user retest: Settings -> Import -> the JSON file -> 140 recipes | `47a6c79` |
| UT-23.1 | (APK) | Signature mismatch when installing the 2026-11-17 17:08 (UTC+11) build | reported | Cause: CI artifact = debug APK signed by a random debug key generated on each fresh runner; no original key exists. See F2. | BLOCKED (needs a permanent key; Task 8 deferred) | user: keep using uninstall + ZIP backup until a permanent key is set up | none |
| UT-23.4 | 1 | Ingredient reordering | NOT WORKING | Edit form had no reorder control; up/down buttons added (`IngredientOrder.move`), position = list index on save | DEVELOPER TESTED (CI), USER RETEST REQUIRED | move, save, reopen | `4362222`, merge `4e33f7f` |
| UT-23.3 | 7 | Compact theme selector, 2 per row | needs redesign | Dropdown + 2-column grid, all 6 themes, custom colours unchanged; `AppearanceDropdownTest` | DEVELOPER TESTED (CI), USER RETEST REQUIRED | check on phone + tablet | `8d3ae34`, merge `63f950b` |
| UT-23.10 | 5 | Shopping list to WhatsApp/Telegram with checkboxes | requested | Share whole list or only remaining items; plain text with ☐/☑. These symbols are NOT interactive. WhatsApp has no checklist in normal messages; Telegram checklists cannot be created via an Android share intent. Practical alternative: send "only remaining" again after shopping progress | DEVELOPER TESTED (CI), USER RETEST REQUIRED | share to both apps | `8c4bab5`, merge `e4c16b2` |
| UT-23.5 | 2 | Tags | WORKING | not re-run manually by developer | USER REPORTED — NOT REPRODUCED (no issue) | regression via CI | — |
| UT-23.6 | 3 | Multiple dishes per meal | WORKING | migration + multi-dish tests in CI | USER REPORTED — NOT REPRODUCED (no issue) | regression via CI | — |
| UT-23.7 | 4 | ZIP/JSON import/export in general | WORKING | DB-level round trip test added | see E | read CI | — |

---

## C. Decisions

| Decision | Why |
|---|---|
| (Jordan) Null -> default when reading backups (`coerceInputValues`) | Real user file contains explicit nulls; rejecting the whole file for optional fields is wrong. Export unchanged |
| (Jordan) Fixture used straight from the repo root, never copied/edited | Spec 23.2.7: do not modify the fixture |
| (Jordan) Temporary `agent-diag` branch for reading files | Connector cannot return file bodies |
| (#11) Signing only when `HOMEMENU_KEYSTORE_FILE` exists | Never generate a replacement key |
| (#12) Shared text via `SharedTextInbox` | No nav signature changes |

---

## D. Git history (this session)

- `agent-diag` (temporary, diagnostics only).
- `task6-json-import-fix` from `modernization` `014a1d7`: `47a6c79` fix(import).

---

## E. Testing

PENDING TESTS AND CHECKS: see the table at the end of this section; never record a pending run as passed.

| Check | Run | Status |
|---|---|---|
| CI on `task6-json-import-fix` | push runs after each commit | PENDING |

---

## F. Blockers and known issues

- **F2 Signing (UT-23.1):** every CI APK is signed by a new random debug key, so CI builds cannot update each other. Needs a permanent keystore in GitHub Secrets (`HOMEMENU_KEYSTORE_BASE64`, `HOMEMENU_KEYSTORE_PASSWORD`, `HOMEMENU_KEY_ALIAS`, `HOMEMENU_KEY_PASSWORD`). Part of Task 8, deferred by the user.
- **F11** Robolectric `android-all` download flake: re-run failed jobs.

---

## G. NEXT AGENT — START HERE

1. Read CI for the latest commits listed in E.
2. Continue with the remaining items in B (Task 8 only when the user un-defers it).
