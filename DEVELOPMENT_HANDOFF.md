# DEVELOPMENT_HANDOFF

Persistent continuation guide for AI coding agents working on `Vano3667/recipe-app`.
Read this file first, then `AGENT.md`, `manual user checks.md` (user test results, Section 23) and
`recipe_app_autonomous_development_prompt (1).md` (full specification).
Last updated: 2026-11-19 ~03:30 UTC (agent Steven, monday.com: Stage 2 re-verification, then Task 8 started on the user's request).
Previous: 2026-11-18 ~02:50 UTC (agent Jordan). Session notes in `docs/handoff/` are merged here.

---

## A. Project overview

| | |
|---|---|
| Project | Home Menu («Домашнее меню»), Android, Russian UI |
| Stack | Kotlin 1.9.24, Compose (BOM 2024.06), Material 3, MVVM, Room 2.6, Hilt 2.51, Navigation Compose, kotlinx.serialization 1.6.3, Coil. minSdk 29, target/compile 34 |
| Code | `app/src/main/java/com/example/homemenu/` (`data/db`, `data/files`, `data/repo`, `domain`, `domain/backup`, `ui/*`) |
| applicationId | `com.example.homemenu` (must never change) |
| Version | versionName 1.2.0; versionCode = `HOMEMENU_VERSION_CODE` env (CI: `github.run_number`), minimum 3 (local builds). Before Task 8: hard-coded 2 / 1.1.0. Shown in Settings > «О приложении» with `BuildConfig.BUILD_DATE` |
| CI | `.github/workflows/android.yml` (id 412025045): `build` (assembleDebug, testDebugUnitTest incl. Robolectric, lintDebug, `home-menu-debug-apk`; with signing secrets also signed `assembleRelease` -> `home-menu-release-apk` + certificate), `instrumented` API 29 pixel_4 and API 34 pixel_tablet, `update-check` (API 34, only with signing secrets). Manual `generate-signing-key.yml` |
| Commands | `./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug`, `./gradlew :app:connectedDebugAndroidTest`, signed: `HOMEMENU_KEYSTORE_FILE=... HOMEMENU_KEYSTORE_PASSWORD=... HOMEMENU_KEY_ALIAS=... HOMEMENU_KEY_PASSWORD=... ./gradlew :app:assembleRelease` |
| User fixture | `recipes_from_budget_classic_1500kcal.json` (repo root, 439,609 bytes, never modify). Read by `app/src/test/.../domain/UserFixture.kt` |

### Working rules / environment
- The monday.com agent has no local git/JDK/SDK. CI (GitHub Actions) is the only build/test environment.
- File bodies cannot be read through the GitHub connector (`get_file_contents` returns only the blob SHA).
  Working techniques: (1) read raw URLs `https://raw.githubusercontent.com/Vano3667/recipe-app/<branch-or-sha>/<path>` with a web page reader (works on 2026-11-19);
  (2) branch `agent-diag` holds a temporary read-only workflow `.github/workflows/agent-diag.yml` (cat files, read job log).
- Branch flow: feature branch -> PR -> green CI -> merge. Docs-only commits use `[skip ci]`. No force-push, no history rewrite, no destructive migrations, no secrets in the repo.
- User-added root file `new job after apk is finished` is left untouched; its text is not part of the spec.

---

## B. Task checklist

Statuses: NOT STARTED / IN PROGRESS / IMPLEMENTED / TESTED (CI green) / VERIFIED (also user device check) / BLOCKED.

1. Bulk ingredient input — TESTED; user: add/parse/delete WORKING. Reorder fix (23.4) TESTED, USER RETEST REQUIRED.
2. Tags + cuisine removal — TESTED; user reports WORKING.
3. Multiple recipes per meal — TESTED (incl. v1->v2 migration test, multi-dish tests); user reports WORKING.
4. ZIP/JSON export/import — TESTED (fixture import into Room, re-import SKIP, JSON round trip, ZIP round trip with photo). User reports general import/export WORKING.
5. Sharing + text import + shopping list share for WhatsApp/Telegram (23.10) — TESTED, USER RETEST REQUIRED.
6. Fix import bug with the real fixture (23.2) — TESTED (CI), USER RETEST REQUIRED.
7. Six themes + custom colours + compact selector (23.3/23.8) — TESTED, USER RETEST REQUIRED.
8. Safe APK updates — IN PROGRESS (started 2026-11-19 on the user's request).
   - A Package identity: `applicationId` unchanged — DONE.
   - B Signing: release signing from env/GitHub Secrets, CI builds and verifies a signed APK — IMPLEMENTED; **BLOCKED** until the owner adds the 4 secrets (F2).
   - C Versioning: versionCode increases every CI run (run number), versionName 1.2.0 — IMPLEMENTED, CI green.
   - D Data preservation: explicit additive migrations only, no destructive fallback, migration test v1->v2 — TESTED. Update-over-install emulator check (`update-check`) — IMPLEMENTED, NOT RUN (needs secrets).
   - E Version info: version / build number / build date from BuildConfig in Settings — DONE (user confirmed version shown).
   - F Verification on device: PENDING (user, after secrets + one last reinstall).

**Stage 2 (Tasks 1-7)** is complete and announced at the top of README.md.

---

## USER TESTING AND REPORTED ISSUES

| Test ID | Task | Feature / issue | User result | Developer result | Status | Next action | Commit |
|---|---|---|---|---|---|---|---|
| UT-23.2 | 6 / 4 | JSON import of `recipes_from_budget_classic_1500kcal.json` | NOT WORKING | REPRODUCED (explicit `null` in non-nullable fields). Fix: `coerceInputValues = true` + defaults. Verified in DB: 140 recipes, 841 ingredients, 692 steps, names/amounts/units/order, tags | DEVELOPER TESTED, USER RETEST REQUIRED | Settings -> Import -> the JSON file -> expect 140 recipes | `47a6c79`, tests `51bf521`, `a0dd4cb` |
| UT-23.1 | 8 | Signature mismatch installing the 2026-11-17 17:08 (UTC+11) build | reported | Cause: CI published debug APKs signed by a random per-runner debug key (run 44025353134 = that build). No original key exists. Task 8 adds a permanent release key from GitHub Secrets; the first APK with it still needs one last uninstall (it cannot match any earlier random key); from then on updates install over the app | FIX IMPLEMENTED, BLOCKED (owner must add secrets) | owner: run `generate-signing-key.yml`, add 4 secrets; user: export ZIP, uninstall for all users, install `home-menu-release-apk`, import ZIP | `df0e4de`, `4f3ae98`, merge `88db49f` |
| UT-23.4 | 1 | Ingredient reordering | NOT WORKING | up/down buttons, `IngredientOrder.move`, position = index, sorted on read | DEVELOPER TESTED (CI), USER RETEST REQUIRED | move, save, reopen | `4362222`, merge `4e33f7f` |
| UT-23.3 | 7 | Compact theme selector, 2 per row | needs redesign | dropdown + 2-column grid, 6 themes, instant apply, custom colours | DEVELOPER TESTED (CI), USER RETEST REQUIRED | check on phone + tablet | `8d3ae34`, merge `63f950b` |
| UT-23.10 | 5 | Shopping list to WhatsApp/Telegram with checkboxes | requested | Sharesheet, whole list or only remaining, ☐/☑ plain text (NOT interactive; WhatsApp has no checklists in normal messages; Telegram checklists cannot be created via share intent) | DEVELOPER TESTED (CI), USER RETEST REQUIRED | share to both apps | `8c4bab5`, merge `e4c16b2` |
| UT-23.5 | 2 | Tags | WORKING | regression via CI | USER REPORTED — NOT REPRODUCED (no issue) | — | — |
| UT-23.6 | 3 | Multiple dishes per meal | WORKING | migration + multi-dish tests in CI | USER REPORTED — NOT REPRODUCED (no issue) | — | — |
| UT-23.7 | 4 | ZIP/JSON import/export | WORKING (general) | DB-level round trips green | DEVELOPER TESTED | — | `51bf521` |
| UT-23.9 | 8 | Version info / update over install | version shown | versionCode now increases per CI run; versionName 1.2.0; applicationId unchanged; update-over-install check written, waits for the key | FIX IMPLEMENTED, BLOCKED (secrets) | after secrets: CI `update-check`, then user installs a newer release APK over the previous one | `df0e4de`, merge `88db49f` |

---

## C. Decisions

| Decision | Why |
|---|---|
| (Jordan) Null -> default when reading backups (`coerceInputValues`) | Real user file contains explicit nulls. Export output unchanged |
| (Jordan) Fixture read from the repo root, never copied/edited | Spec 23.2.7 |
| (#11) Signing only when `HOMEMENU_KEYSTORE_FILE` exists | Never generate a replacement key and call it compatible |
| (Steven) versionCode = CI run number, min 3 | Monotonic across all branches without manual bumps; above the last hard-coded 2. A local build (3) cannot be installed over a CI build (downgrade): use CI APKs on the phone |
| (Steven) New permanent key via a manual one-time workflow, returned as a 3-day artifact | No original key exists (old APKs used random debug keys). The key is new and is NOT claimed compatible with old installs; only the owner can store it in Secrets. Nothing secret is logged or committed; the key file lives only in the runner temp dir and is deleted |
| (Steven) User-facing APK = signed release APK; debug APK stays for tests | Debug signing remains random per runner; changing that would mean committing a key |
| (Steven) `update-check` fingerprints recipes by (id, name, createdAt) | A wiped and re-seeded DB gets new createdAt values, so data loss cannot pass unnoticed; firstInstallTime must stay unchanged (update, not reinstall) |
| (Steven) Old and new APK in `update-check` come from the same commit with versionCode N-1 / N | Robust and fast; migrations between schema versions are covered by `MigrationV1toV2Test` |

---

## D. Git history

### 2026-11-18 (Jordan)
- `task6-json-import-fix`: `47a6c79`, `51bf521`, `a0dd4cb`; PR #14 -> `modernization` `6ae5d81`; PR #1 -> `main` `4989e62`; docs `d67ac78`.

### 2026-11-19 (Steven)
- `51f6e4b` docs: Stage 2 re-verification (main).
- Branch `task8-safe-updates` from `51f6e4b`: `df0e4de` feat(task8) (build.gradle.kts, android.yml, generate-signing-key.yml, scripts/update_check.sh); `4f3ae98` fix(update-check) pipefail.
- PR #16 merged into `main` as `88db49f`. Then this docs commit.

---

## E. Testing

| Commit | Run | build (unit + Robolectric + lint) | API 29 | API 34 | release signing | update-check |
|---|---|---|---|---|---|---|
| `4989e62` main (Stage 2) | 44225182905 attempt 4 | PASSED | PASSED | PASSED | — | — |
| `df0e4de` task8 | 44449511580 | PASSED | PASSED | PASSED | skipped (no secrets) | skipped |
| `4f3ae98` task8 (push) | 44449567520 (run 97) | PASSED | PASSED | PASSED | skipped (no secrets; warning shown) | skipped |
| `4f3ae98` task8 (PR #16) | 44449587738 | PASSED | PASSED | PASSED | skipped | skipped |
| `88db49f` main merge | push run started automatically after merge | PENDING | PENDING | PENDING | skipped unless secrets | skipped unless secrets |

Not yet executed anywhere: the signed `assembleRelease`, apksigner certificate check and `scripts/update_check.sh` (they need the key). The script and those steps are therefore UNVERIFIED.

### PENDING TESTS AND CHECKS

| Check | Command / procedure | Branch / commit | Started | Status | Next action |
|---|---|---|---|---|---|
| CI on `main` after Task 8 merge | push run of workflow 412025045 | `main` `88db49f` | 2026-11-19 ~03:27 UTC | PENDING | `actions_list list_workflow_runs branch=main`; record result |
| Signed release build + certificate | `build` job steps "Decode release signing key" / "Assemble signed release APK" | first run after the secrets exist | — | BLOCKED (secrets) | after secrets: Actions -> Run workflow on `main`, check artifact `home-menu-release-apk` and `release-signing-certificate.txt` |
| Update over existing install (emulator) | job `update-check`, `bash scripts/update_check.sh old.apk new.apk` | same run | — | BLOCKED (secrets) | read job log, expect `PASSED: update installed over the existing app, data preserved`; fix script if it fails |
| Update over existing install (user device) | install release APK N, add data, install release APK N+k over it | — | — | PENDING (user) | user report |
| User device retests UT-23.2, 23.3, 23.4, 23.10 | user | — | — | PENDING (user) | — |

---

## F. Blockers and known issues

- **F2 Signing key (Task 8, UT-23.1, UT-23.9) — BLOCKED on the owner:** add 4 repository secrets `HOMEMENU_KEYSTORE_BASE64`, `HOMEMENU_KEYSTORE_PASSWORD`, `HOMEMENU_KEY_ALIAS`, `HOMEMENU_KEY_PASSWORD` (Settings -> Secrets and variables -> Actions). Either from an existing keystore (`base64 -w0 key.jks`) or by running Actions -> "Generate signing key (one time)" and copying the files of artifact `signing-key-SAVE-THEN-DELETE`. Keep the .jks + password outside GitHub; delete the artifact. The agent cannot set secrets. The new key cannot match APKs installed before: one last uninstall (after a ZIP export) is unavoidable; this is a one-time step, not a fix for future updates.
- **F13** GitHub Actions jobs not starting (2026-11-18): RESOLVED.
- **F11** Robolectric `android-all` download flake: re-run failed jobs.
- **F12** `AppFlowsTest.shoppingListIsBuiltFromMenu` timed out once on API 34; passed in all later runs.
- **F5** `TEST_REPORT.md` outdated; this file is the current test record.

---

## G. NEXT AGENT — START HERE

1. Check the `main` push run for `88db49f` (PENDING above) and record it in E.
2. Check whether the 4 signing secrets exist: the latest `build` job log shows either the warning "No HOMEMENU_KEYSTORE_BASE64 secret" or runs "Assemble signed release APK".
3. When they exist: run the workflow on `main` (workflow_dispatch), verify `home-menu-release-apk` + certificate, read the `update-check` log; fix `scripts/update_check.sh` / workflow if needed (likely spots: `adb root` on the image, DB path, timing of the first launch).
4. Then ask the user to do the one-time reinstall with the release APK and, with the next build, an update over it; mark Task 8 VERIFIED only after that succeeds with data kept.
5. Files: `app/build.gradle.kts`, `.github/workflows/android.yml`, `.github/workflows/generate-signing-key.yml`, `scripts/update_check.sh`, `data/db/AppDatabase.kt`.
