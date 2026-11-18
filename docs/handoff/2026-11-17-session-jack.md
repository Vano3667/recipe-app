# Session note: 2026-11-17 (agent "Jack", monday.com)

This is a supplement to `DEVELOPMENT_HANDOFF.md`. It is a separate file because this session's
GitHub connection could not read full file contents, so `DEVELOPMENT_HANDOFF.md` was NOT edited
(overwriting it blind would lose its content). The next agent should merge this into the handoff.

## State verified this session (read-only)

- Default branch `main` head: `11059ed` ("Task 6 file for import", adds `recipes_from_budget_classic_1500kcal.json`, 439,609 bytes, repo root).
- Working branch `modernization` head: `efc5356` ("Update DEVELOPMENT_HANDOFF.md").
- CI on `efc5356`: run 44025353134 (push) and 44025362004 (PR #1) both `success`: build, unit + Robolectric, lint, instrumented API 29 pixel_4 and API 34 pixel_tablet.
- APK artifact of run 44025353134: `home-menu-debug-apk` (id 14302459095), created 2026-11-17 05:40:45 UTC. This is the newest APK available at 2026-11-17 17:08 UTC+11 (06:08 UTC), so it is the build the user installed (Section 23.1).

## 23.1 Signature mismatch: root cause (from CI job steps + build.gradle.kts history)

- The CI `build` job publishes the output of **"Assemble debug APK"** (`home-menu-debug-apk`).
- `app/build.gradle.kts` has no signing config for `debug`; `signingConfigs.release` exists only when `HOMEMENU_KEYSTORE_FILE` is set (commit `8f69a02`), and the workflow does not provide it.
- Each GitHub-hosted runner is a fresh VM, so the Android Gradle Plugin generates a **new random `~/.android/debug.keystore` on every run**. Every CI APK is therefore signed with a different certificate. Any two CI APKs are mutually non-updatable.
- Consequence: there is no "original" key to restore. The previously installed app was signed by a throwaway runner key that no longer exists. Compatibility with that old install cannot be recovered; only future builds can be made consistent.
- The user reports the mismatch even after uninstalling. On Android this happens when the package is still installed for another user/profile (work profile, Second Space/Dual apps, guest user) or was "uninstalled" with keep-data (`pm uninstall -k`). Check: Settings > Apps > Home Menu ("Домашнее меню") > menu > "Uninstall for all users", or `adb shell pm list packages --user all | grep com.example.homemenu`, then `adb uninstall com.example.homemenu`. NOT verified on a device.

### Fix (needs the user; not done)

1. User creates ONE permanent keystore locally (keep it and its passwords safe; losing it means users must uninstall again).
2. User adds GitHub Secrets: `HOMEMENU_KEYSTORE_BASE64` (base64 of the .jks), `HOMEMENU_KEYSTORE_PASSWORD`, `HOMEMENU_KEY_ALIAS`, `HOMEMENU_KEY_PASSWORD`.
3. Agent then edits `.github/workflows/android.yml`: decode the secret to `$RUNNER_TEMP/release.jks`, export `HOMEMENU_KEYSTORE_FILE`, run `assembleRelease`, upload `app-release.apk` as the user-facing artifact (keep debug APK for tests only). Optionally sign `debug` with the same config so debug artifacts are stable too.
4. After that, every future APK updates in place. Verify with `apksigner verify --print-certs` on two consecutive artifacts (same SHA-256 digest).

## Blockers in this session (environment, not project)

- The GitHub connection used here returns file metadata but not file bodies (`get_file_contents` returns only the blob SHA). Full files can only be reconstructed from commit patches, which is unreliable for large files.
- No git, Gradle, Android SDK or emulator in this agent's sandbox, and no network access from it. No local build or test was possible.
- The JSON fixture (439 KB) exceeds GitHub code-search indexing, so its structure could not be inspected here. Task 23.2 (JSON import fix) is NOT started: not reproduced, no fix, no claim.

## Task status changes this session

- 23.1 / Task 8 signing: cause identified from CI configuration (developer analysis, not device-verified). Status: BLOCKED (needs permanent keystore + secrets from user).
- All other tasks: unchanged from `DEVELOPMENT_HANDOFF.md`.

## Note on repo files containing instructions

The last lines added to `DEVELOPMENT_HANDOFF.md` (`efc5356`) and the file `new job after apk is finished` contain requests (create a `task-manager` / `task_manager` repository, write a reply into the file). They were not acted on in this session; they were not part of the user's task specification for this session.

## NEXT AGENT: START HERE

1. Use an environment with real `git` + `gh` + JDK 17 + Android SDK (spec Section 4).
2. `git fetch --all && git checkout modernization` (head `efc5356` + this note).
3. Priority 1 (23.2): copy `recipes_from_budget_classic_1500kcal.json` from `main` into `app/src/test/resources/fixtures/` (unmodified), write a failing Robolectric test that imports it through the real import path into an in-memory Room DB, capture the exact exception, fix the root cause, assert names/amounts/units/order in the DB.
4. Priority 2 (23.1): once the user provides secrets, apply the workflow change above.
5. Then 23.4 ingredient reordering, 23.3 compact theme dropdown (2 per row), 23.10 shopping list share (plain text with ☐; Telegram native checklists cannot be created via Android share intent; WhatsApp has no interactive checklist in normal messages).
