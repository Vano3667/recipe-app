# DEVELOPMENT_HANDOFF.md

Persistent continuation guide for AI coding agents working on this repository.
Read this file first, then `AGENT.md` (older notes, Russian; its "commit to main" rule is superseded).
Last updated: 2026-11-16 16:30 UTC (agent #5 "Betty").

---

## A. Project overview

| Item | Value |
|---|---|
| Project | "Домашнее меню" (Home Menu), offline Android app: recipes, weekly menu, shopping list. UI Russian only. |
| Repository | `Vano3667/recipe-app` (private). Default `main`, work branch `modernization`. |
| Stack | Kotlin 1.9.24, Jetpack Compose (BOM 2024.06.00) + Material 3, MVVM (`data`/`domain`/`ui`), Hilt 2.51.1, Navigation Compose 2.7.7, Coroutines/Flow |
| Database | Room 2.6.1 (KSP), file `home_menu.db`, schema **v2** on `modernization` (main: v1) |
| Other libs | kotlinx.serialization 1.6.3 (backup JSON), Coil 2.6.0 (photos in `filesDir/photos`, DB keeps file names) |
| Build | Gradle 8.7 (wrapper NOT committed; CI runs `gradle wrapper --gradle-version 8.7`), JDK 17, minSdk 29, target/compile 34 |
| applicationId | `com.example.homemenu` (must never change) |
| Version | `versionCode = 1`, `versionName = "1.0"` |
| Signing | No signingConfig. CI builds `assembleDebug` with the runner's random debug key (F2). |
| CI | `.github/workflows/android.yml`: job `build` (assembleDebug, testDebugUnitTest incl. Robolectric, lintDebug abortOnError) + job `instrumented` (connectedDebugAndroidTest, API 29 pixel_4 and API 34 pixel_tablet). ~4 min build, ~10 min instrumented. |
| Commands (CI) | `./gradlew assembleDebug testDebugUnitTest lintDebug`, `./gradlew connectedDebugAndroidTest` |
| Other docs | `README.md`, `DESIGN.md`, `TEST_REPORT.md` (results up to run #10), `ENVIRONMENT.md`, `CREDITS.md`, `AGENT.md` |

### Important files
- `ui/recipes/RecipeEditScreen.kt` (form), `ui/recipes/BulkIngredientsDialog.kt` (Task 1), `ui/recipes/RecipeViewModels.kt`
- `ui/menu/MenuScreen.kt`, `ui/menu/MenuViewModel.kt` (Task 3)
- `data/repo/Repositories.kt` (MenuRepository addToSlot/replaceRecipe/reorder/move/clear)
- `data/files/BackupManager.kt`, `domain/backup/*` (Task 4)
- `domain/IngredientLineParser.kt` (Task 1 parser)

### Working rules
- No local JDK/Gradle/SDK. Code changes go through the GitHub API; **CI is the only build/test environment**.
- Commit to `modernization`; merge to `main` only via PR after CI is green.
- Docs-only commits: `[skip ci]`. Multi-part changes: intermediate parts `[skip ci]`, last part without it.
- Never force-push, rewrite history, or commit keystores/passwords/tokens.
- Tooling: "get file contents" returns only the blob SHA (use it as `sha` for updates). Read bodies from commit diffs (`get_commit` full_patch). `87a602d` = original upload of every file; with `perPage=1, page=N` one file is returned (RecipeEditScreen = 36, MenuScreen = 32, MenuViewModel = 33). Write one file per commit.
- **User instruction (2026-11-17, agent #5): write the next tasks immediately without waiting for CI results; fix failures afterwards.** Agent #5's connection has no Actions tool, so CI results of its commits are unread.

---

## B. Task checklist

1. [ ] Bulk ingredient input — IMPLEMENTED (UI), parser TESTED
2. [ ] Tags redesign + full cuisine removal — IMPLEMENTED
3. [ ] Multiple recipes per meal — IMPLEMENTED (UI + data)
4. [ ] Complete ZIP and JSON export/import — IMPLEMENTED
5. [ ] Recipe sharing + text import — IN PROGRESS
6. [ ] Fix existing import bug — BLOCKED, WAITING FOR USER FILE
7. [ ] Six themes + custom colors — IN PROGRESS (storage only)
8. [ ] Safe APK updates — IN PROGRESS, signing BLOCKED

Supporting: migration test on real v1 DB; data preservation; UI consistency; tests; release APK.

---

## C. Development status

Statuses: NOT STARTED / IN PROGRESS / IMPLEMENTED / TESTED / VERIFIED / BLOCKED.

**Global state:** last known green: `5be7b4d` (CI #17, all jobs) and `0fec6c8` (CI #18 build job green; API 34 flaky, F7). Agent #5 pushed `cfbd8d2`..`2e76d8f` (Task 1 UI, Task 3 UI). CI ran on `95cbf59` and `2e76d8f`; **results not read yet** (F8).

### Task 1 — Bulk ingredient input: IMPLEMENTED (UI unverified)
- TESTED: `domain/IngredientLineParser.kt` (`9da0cbc`) + `IngredientLineParserTest` (`0fec6c8`, green in CI #18).
- IMPLEMENTED (`cfbd8d2`, `ab3c0f8`): `ui/recipes/BulkIngredientsDialog.kt` — "Вставить списком" button next to "Добавить ингредиент" (tag `bulk_ingredients`). Dialog: paste (`bulk_input`) -> "Разобрать" (`bulk_parse`) -> preview rows with include checkbox, editable name / amount / unit, remove (`bulk_remove_i`), "not recognized" and "duplicate" hints -> "Добавить (n)" (`bulk_add_all`). Duplicates (within paste or vs ingredients already in the form) start unchecked. `BulkIngredients.append` matches products by normalized name and creates missing ones through the existing `onAddProduct` one after another, so order is preserved. Single-add flow untouched.
- Unit tests `ui/BulkIngredientsTest.kt` (`95cbf59`), result unknown.
- Missing: Robolectric/instrumented test of the dialog; known risk: if `vm.addCustomProduct` fails and never calls back, remaining rows are not added (check `RecipeEditViewModel.addCustomProduct`).

### Task 2 — Tags + cuisine removal: IMPLEMENTED
- Cuisine removed from domain (`047d9e6`), edit state (`5961b4f`), detail (`4328ad3`), edit form (`5be7b4d`). Tags multi-select at bottom, removable chips, separate "new tag" action, case-insensitive dedupe. Legacy DB column `recipes.cuisine` kept.
- Missing: grep remaining `cuisine`/`кухн` references (RecipeSearch, RecipesScreen filters, seed, tests, README), tests for tag select/create/remove.

### Task 3 — Multiple recipes per meal: IMPLEMENTED (UI unverified)
- Data (`047d9e6`, `aa8edfe`): `menu_entries.position`, non-unique `(date, slot)` index, repository addToSlot/replaceRecipe/reorder/move/clear with renumbering; same recipe twice in one meal refused.
- UI (`4c03f6a` ViewModel, `2e76d8f` screen): `MenuUiState.dishes(date, slot)`; each meal lists its dishes; per dish: long-press drag to another meal, up/down (`dish_up_<id>`, `dish_down_<id>`), replace (`dish_replace_<id>`, picker without servings), remove (`dish_remove_<id>`, contentDescription now "Убрать блюдо", was "Очистить слот"), servings, cook. Empty meal: whole slot clickable (tag `slot_<date>_<SLOT>` unchanged); filled meal: "Ещё блюдо" (`slot_add_<date>_<SLOT>`). Picker hides recipes already in the meal; refusals show a snackbar. `emptySlots` now counts empty meals.
- Missing: check `MenuGeneratorUseCase` / `ShoppingListUseCase` for one-dish assumptions; tests for dishes()/summary and UI; migration test (F4). If tests click "Очистить слот" or the slot itself to open the cook button, update them.

### Task 4 — ZIP/JSON export/import: IMPLEMENTED
- Format v2, staged import with preview/duplicate modes/single transaction/read-back (`862a437`, `1bec32b`); Settings UI (`94891a7`, `16c5dfb`); `BackupJsonTest` v2 (`1a764a1`).
- Missing: round trip with photos, failed-import rollback test, instrumented import read-back.

### Task 5 — Sharing + text import: IN PROGRESS
- Done: readable share text + clipboard in `ui/recipes/RecipeDetailRoute.kt` (`a8b6896`).
- Missing: Sharesheet `ACTION_SEND text/plain` check, TXT export, pure share formatter + tests, text recipe parser (reuse `IngredientLineParser`) + editable preview, TXT import.

### Task 6 — Existing import bug: BLOCKED — WAITING FOR USER FILE
- Requested from the user again by agent #5. Do not report as fixed until the original file imports and its data is verified.

### Task 7 — Themes + custom colors: IN PROGRESS (storage only)
- Done: `user_settings.themeId` (default LEGACY), `customColors` JSON, in backup v2, `SettingsRepository.setAppearance`.
- Missing: 6 palettes, provider in `Theme.kt`/`MainActivity.kt`, Settings > Appearance, picker, contrast, reset, tests.

### Task 8 — Safe APK updates: IN PROGRESS, signing BLOCKED
- Done: build number in About (`16c5dfb`). Missing: build date field, release signing from secrets, versionCode bump, update-over-install test (F2).

### Supporting — Migration 1->2: IMPLEMENTED (no migration test yet, F4)

---

## D. Technical decisions

| Decision | Reason | Commit |
|---|---|---|
| Keep legacy `recipes.cuisine` column | No table rebuild, old data kept | 047d9e6 |
| Multi-dish meals = several `menu_entries` per (date, slot) by `position` | Additive migration | 047d9e6 |
| Pre-migration DB byte copy (`DatabaseSafety`) | Safety net | 047d9e6 |
| Backup v2 ZIP (`recipes.json` + `photos/` + `manifest.json` SHA-256) / JSON with Base64 images | Self-contained; v1 readable | 862a437, 1bec32b |
| Two-phase import, one transaction, read-back | No partial imports | 1bec32b |
| Duplicates by normalized recipe name | Ids differ between devices | 862a437 |
| Theme in DB `user_settings` | Survives updates, in backup | 047d9e6 |
| `TO_TASTE` amount 0 | "по вкусу" lines | 047d9e6 |
| Detail screen split into Screen + Route files | API rejects large writes | 4328ad3, a8b6896 |
| Bulk parser is pure domain code; unknown lines kept, never dropped; duplicates only flagged | Testable, no silent data loss | 9da0cbc |
| Bulk dialog in its own file; products created sequentially via existing `onAddProduct` | Small files; order preserved without ViewModel changes | cfbd8d2 |
| Menu: one drop target per meal, drag handle per dish; picker hides recipes already in the meal | Simple UI, duplicates prevented in UI and repository | 2e76d8f |
| `MenuContent` new params `onReplace`/`onReorder` have defaults; `entry()` kept | Existing tests keep compiling | 2e76d8f, 4c03f6a |
| Pending: signing (F2); versioning proposal versionCode +1 per release, versionName 1.1.0 | — | — |

---

## E. Git history

- Last fully green: `5be7b4d` (CI #17, all jobs). `main` @ `2ceef75` green (CI #11).
- Agent #1: `047d9e6`, `aa8edfe`, `862a437`, `1bec32b` (data layer).
- Agent #2: `44d94bf` handoff.
- Agent #3: `94891a7`, `5961b4f`, `1a764a1`, `4328ad3`, `a8b6896`, `16c5dfb`, `5be7b4d` (compile fix + UI; CI #17 green).
- Agent #4: `3c0fac1` handoff, `9da0cbc` parser, `0fec6c8` parser tests, `eb4350c` handoff.
- Agent #5 (2026-11-16 16:18-16:30 UTC):
  - `cfbd8d2` BulkIngredientsDialog.kt (new) [skip ci]
  - `ab3c0f8` RecipeEditScreen: "Вставить списком" [skip ci]
  - `95cbf59` BulkIngredientsTest — CI triggered, result unread
  - `4c03f6a` MenuViewModel multi-dish [skip ci]
  - `2e76d8f` MenuScreen multi-dish — CI triggered, result unread
  - this docs commit [skip ci]
- All pushes verified by the API response (new commit SHA returned).

---

## F. Blockers and known issues

- **F2 Signing (Task 8, user):** no release key; CI APKs use random debug keys and cannot update each other. If the installed app came from CI, export a backup ZIP first, then reinstall. Plan: permanent keystore in GitHub Secrets (`KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`), `signingConfigs.release`, CI `assembleRelease`. Waiting: how the current app was installed; does the user have a keystore.
- **F3 Import bug file (Task 6, user):** waiting for the failing file. Everything else continues independently.
- **F4 Room schemas not committed:** needed for a 1->2 `MigrationTestHelper` test. Generate v1 schema from `2ceef75` in CI, commit `app/schemas/.../1.json`, add `room-testing`.
- **F5** `AGENT.md` outdated; this file supersedes it.
- **F7 Flaky instrumented test on API 34:** CI #18 `AppFlowsTest.addRecipeWithIngredient` failed on pixel_tablet API 34 ("Не появился элемент с тегом «product_search»"), emulator GPU errors. Passed on API 29 and in #17. Likely flakiness, not proven.
- **F8 CI results of agent #5 commits unread:** agent #5's GitHub connection exposes no Actions tools. Per user instruction, work continued without waiting. Next agent with Actions access: read runs for `95cbf59` and `2e76d8f` first and fix any compile/test errors (likely spots: `BulkIngredientsDialog.kt` imports, `MenuScreen.kt` `SwapHoriz` icon (needs material-icons-extended, already used for `DragIndicator`), tests referencing "Очистить слот").

---

## G. NEXT AGENT — START HERE

1. **State:** `modernization` head = this docs commit; latest code commit `2e76d8f`. Last verified green `5be7b4d` / `0fec6c8`. Tasks 1 and 3 UI written but not CI-verified.
2. **First:** read CI for `2e76d8f` (and `95cbf59`). Fix compile/test failures. Update `TEST_REPORT.md`.
3. **Next action (per user: keep writing, fix later):** Task 5 — pure `domain/RecipeTextFormatter.kt` (share text, move logic from `RecipeDetailRoute.kt`) + tests; Sharesheet `Intent.ACTION_SEND` text/plain with `Intent.createChooser`; TXT export via `ActivityResultContracts.CreateDocument("text/plain")`; `domain/RecipeTextParser.kt` (title, sections «Ингредиенты»/«Приготовление», times, servings, tags; ingredient lines via `IngredientLineParser`; unknown text kept in notes) + editable preview that opens the edit form prefilled.
4. **Then:** Task 7 themes -> Task 2 leftovers grep -> Task 4 round-trip/rollback tests -> F4 migration test -> Task 8 (after user answer) -> Task 6 (after user file).
5. **Files to inspect first:** `ui/recipes/RecipeDetailRoute.kt` (a8b6896), `ui/recipes/RecipeViewModels.kt` (87a602d page 37 + 5961b4f), `ui/nav/AppNav.kt` (87a602d page 34), `ui/theme/Theme.kt`, `ui/theme/Color.kt`, `MainActivity.kt`.
6. **Precautions:** never change `applicationId`; no `fallbackToDestructiveMigration`; keep legacy `cuisine` column; no merge to `main` until CI green and migration tested; no secrets in git.
