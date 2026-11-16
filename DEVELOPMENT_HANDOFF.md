# DEVELOPMENT_HANDOFF.md

Persistent continuation guide for AI coding agents working on this repository.
Read this file first, then `AGENT.md` (older notes, Russian; its "commit to main" rule is superseded).
Last updated: 2026-11-16 21:40 UTC (agent #7 "Jessica", recovery + documentation only, no app code changed).

---

## A. Project overview

| Item | Value |
|---|---|
| Project | "Домашнее меню" (Home Menu), offline Android app: recipes, weekly menu, shopping list. UI Russian only. |
| Repository | `Vano3667/recipe-app` (private). Default `main`, work branch `modernization`. Draft PR #1 `modernization` -> `main` (do not merge, see F8). |
| Stack | Kotlin 1.9.24, Jetpack Compose (BOM 2024.06.00) + Material 3, MVVM (`data`/`domain`/`ui`), Hilt 2.51.1, Navigation Compose 2.7.7, Coroutines/Flow |
| Database | Room 2.6.1 (KSP), file `home_menu.db`, schema **v2** on `modernization` (main: v1), `MIGRATION_1_2` additive, pre-migration copy in `DatabaseSafety.kt` |
| Other libs | kotlinx.serialization 1.6.3 (backup JSON), Coil 2.6.0 (photos in `filesDir/photos`, DB keeps file names) |
| Build | Gradle 8.7 (wrapper NOT committed; CI runs `gradle wrapper --gradle-version 8.7`), JDK 17, minSdk 29, target/compile 34 |
| applicationId | `com.example.homemenu` (must never change) |
| Version | `versionCode = 1`, `versionName = "1.0"` |
| Signing | No signingConfig. CI builds `assembleDebug` with the runner's random debug key (F2). |
| CI | `.github/workflows/android.yml`: job `build` (assembleDebug, testDebugUnitTest incl. Robolectric, lintDebug abortOnError) + job `instrumented` (connectedDebugAndroidTest, API 29 pixel_4 and API 34 pixel_tablet). ~4 min build, ~10 min instrumented. |
| Commands (CI) | `./gradlew assembleDebug testDebugUnitTest lintDebug`, `./gradlew connectedDebugAndroidTest` |
| Other docs | `README.md`, `DESIGN.md`, `TEST_REPORT.md` (results up to run #10), `ENVIRONMENT.md`, `CREDITS.md`, `AGENT.md` (outdated) |

### Important files (under `app/src/main/java/com/example/homemenu/`)
- `ui/recipes/RecipeEditScreen.kt` (form), `ui/recipes/BulkIngredientsDialog.kt` (Task 1, reused by Task 5), `ui/recipes/RecipeViewModels.kt`
- `ui/menu/MenuScreen.kt`, `ui/menu/MenuViewModel.kt` (Task 3); `data/repo/Repositories.kt` (MenuRepository addToSlot/replaceRecipe/reorder/move/clear)
- `data/files/BackupManager.kt`, `domain/backup/*` (Task 4)
- `domain/IngredientLineParser.kt` (Task 1 parser)
- Task 5: `ui/recipes/RecipeDetailRoute.kt` (share text, Sharesheet, clipboard, TXT export), `ui/recipes/RecipeDetailScreen.kt` (share menu), `domain/RecipeTextParser.kt`, `ui/recipes/RecipeTextImport.kt` (mapping + `TextImportViewModel`), `ui/recipes/TextImportDialog.kt`, `ui/recipes/RecipesScreen.kt` ("Из текста" entry)
- Task 7: `ui/theme/Theme.kt`, `ui/theme/Color.kt` (`TagPalette`), `ui/settings/SettingsScreen.kt`, SettingsRepository (`setAppearance`, `themeId`, `customColors`), `MainActivity.kt`
- DB: `data/db/AppDatabase.kt`, `data/db/Entities.kt`

### Working rules
- No local JDK/Gradle/SDK. Code changes go through the GitHub API; **CI is the only build/test environment**.
- Commit to `modernization`; merge to `main` only via PR after CI is green.
- Docs-only commits: `[skip ci]` (exception: a docs commit may omit it on purpose to trigger CI on the current code so PR #1 shows check runs, as agent #7 did). Multi-part changes: intermediate parts `[skip ci]`, last part without it.
- Never force-push, rewrite history, or commit keystores/passwords/tokens.
- Tooling: "get file contents" returns only the blob SHA (use it as `sha` for updates). Read bodies from commit diffs (`get_commit` full_patch). `87a602d` = original upload of every file; with `perPage=1, page=N` one file is returned (MenuScreen = 32, MenuViewModel = 33, AppNav = 34, RecipeDetailScreen = 35, RecipeEditScreen = 36, RecipeViewModels = 37, RecipesScreen = 38). Write one file per commit. This file was rewritten whole by agent #7: its full body = this commit's content.
- **Code search** (`search_code` with `repo:Vano3667/recipe-app`) works and returns code fragments, but indexes only the default branch `main`, not `modernization`.
- **CI visibility (new, agent #7):** agent #7's connection has no Actions tool, but CAN create and read pull requests. Draft PR #1 (`modernization` -> `main`) exists; `pull_request_read` with `get_check_runs` / `get_status` returns the checks of the PR head commit. Checks exist only if the head commit was not `[skip ci]`.
- User instruction (2026-11-17, agents #5/#6): do not wait for CI; keep writing; record everything so the next run can be checked. Agent #7 now can read CI through PR #1, so verify before stacking more code.

---

## B. Task checklist

1. [ ] Bulk ingredient input — IMPLEMENTED (UI unverified)
2. [ ] Tags redesign + full cuisine removal — IMPLEMENTED (leftover references remain)
3. [ ] Multiple recipes per meal — IMPLEMENTED (UI + data, UI unverified)
4. [ ] Complete ZIP and JSON export/import — IMPLEMENTED (tests missing)
5. [ ] Recipe sharing + text import — IMPLEMENTED (CI unverified)
6. [ ] Fix existing import bug — BLOCKED, WAITING FOR USER FILE
7. [ ] Six themes + custom colors — IN PROGRESS (storage only)
8. [ ] Safe APK updates — IN PROGRESS, signing BLOCKED

Supporting: migration test on real v1 DB; data preservation; UI consistency; tests; release APK.

---

## C. Development status

Statuses: NOT STARTED / IN PROGRESS / IMPLEMENTED / TESTED / VERIFIED / BLOCKED.

**Global state:** last known green: `5be7b4d` (CI #17, all jobs) and `0fec6c8` (CI #18 build job green; API 34 flaky, F7). Unverified since: agent #5 `cfbd8d2`..`2e76d8f`, agent #6 `1e8f0b8`..`1b8484c`. CI was triggered on `95cbf59`, `2e76d8f`, `ac6bcb7`, `27e4800`, `1b8484c`; no result read. Agent #7's docs commit (this one) is intentionally without `[skip ci]`, so CI runs on the full current code and its result is visible in PR #1.

### Task 1 — Bulk ingredient input: IMPLEMENTED (UI unverified)
- TESTED: `domain/IngredientLineParser.kt` (`9da0cbc`) + `IngredientLineParserTest` 9 tests (`0fec6c8`, green in CI #18). Leading/trailing amounts, fractions, ranges (upper bound), unicode fractions, RU/EN unit synonyms, kg/l -> g/ml, "по вкусу" -> TO_TASTE, bullets/numbering; unknown lines kept (`recognized=false`); `duplicateIndices()`.
- IMPLEMENTED (`cfbd8d2`, `ab3c0f8`): `ui/recipes/BulkIngredientsDialog.kt` — "Вставить списком" button next to "Добавить ингредиент" (tag `bulk_ingredients`). Dialog: paste (`bulk_input`) -> "Разобрать" (`bulk_parse`) -> preview rows with include checkbox, editable name / amount / unit, remove (`bulk_remove_i`), "not recognized" and "duplicate" hints -> "Добавить (n)" (`bulk_add_all`). Duplicates start unchecked. `BulkIngredients.append` keeps order. Single-add flow untouched.
- `7fcf342`: optional `initialText` / `confirmLabel` params (defaults = old behaviour) for reuse by Task 5.
- Unit tests `ui/BulkIngredientsTest.kt` (`95cbf59`), result unknown.
- Missing: Robolectric/instrumented test of the dialog; known risk: if `vm.addCustomProduct` fails and never calls back, remaining rows are not added.

### Task 2 — Tags + cuisine removal: IMPLEMENTED (leftovers)
- Cuisine removed from domain (`047d9e6`), edit state (`5961b4f`), detail (`4328ad3`), edit form (`5be7b4d`). Tags multi-select at bottom, removable chips, separate "new tag" action, case-insensitive dedupe. Legacy DB column `recipes.cuisine` kept (no table rebuild).
- `RecipesScreen.kt` has no cuisine filter (filters = category/difficulty/tags).
- Agent #7 code search on `main` (pre-modernization code) found cuisine in: `data/Mappers.kt`, `data/seed/Seeder.kt`, `data/seed/SeedData.kt` (`SeedRecipe.cuisine`), `domain/model/Models.kt` (`cuisine` field + `DEFAULT_CUISINES` list), `ui/recipes/RecipeViewModels.kt`, `ui/recipes/RecipeEditScreen.kt` (`CuisineField`), `ui/recipes/RecipeDetailScreen.kt`, `data/repo/Repositories.kt`, `data/files/BackupManager.kt`, `domain/backup/BackupFormat.kt`, `data/db/Entities.kt`, test `BackupJsonTest.kt`. Several are already fixed on `modernization`; each must be re-checked there via commit diffs (search does not index `modernization`).
- Missing: remove remaining references (`DEFAULT_CUISINES`, `CuisineField` composable if still defined, seed data, `RecipeSearch`/`RecipeQuery`, Settings, README); keep `Entities.cuisine` column and backup v1 read compatibility; tests for tag select/create/remove.

### Task 3 — Multiple recipes per meal: IMPLEMENTED (UI unverified)
- Data (`047d9e6`, `aa8edfe`): `menu_entries.position`, non-unique `(date, slot)` index, repository addToSlot/replaceRecipe/reorder/move/clear with renumbering; same recipe twice in one meal refused.
- UI (`4c03f6a` ViewModel, `2e76d8f` screen): `MenuUiState.dishes(date, slot)`; per dish: drag, up/down (`dish_up_<id>`, `dish_down_<id>`), replace (`dish_replace_<id>`), remove (`dish_remove_<id>`, contentDescription "Убрать блюдо", was "Очистить слот"), servings, cook. Empty meal: slot clickable (`slot_<date>_<SLOT>`); filled: "Ещё блюдо" (`slot_add_<date>_<SLOT>`).
- Missing: check `MenuGeneratorUseCase` / `ShoppingListUseCase` for one-dish assumptions; tests; migration test (F4).

### Task 4 — ZIP/JSON export/import: IMPLEMENTED
- Format v2 (manifest, SHA-256, Base64 images in JSON, v1 readable), staged import with preview / duplicate modes (skip, update, separate) / single transaction / photo rollback / read-back (`862a437`, `1bec32b`); Settings UI (`94891a7`, `16c5dfb`); `BackupJsonTest` v2 (`1a764a1`).
- Missing: round trip with photos, failed-import rollback test, instrumented import read-back.

### Task 5 — Sharing + text import: IMPLEMENTED (unverified)
- Share (`a8b6896`): `recipeShareText()` + Sharesheet (`ACTION_SEND` text/plain via `createChooser`) + clipboard. Single-recipe ZIP share exists.
- TXT export (`c5faad8`, `27e4800`): share menu item "Сохранить как TXT" (tag `export_txt`, optional param `onExportTxt`). `CreateDocument("text/plain")` registered before the early return; UTF-8 write on IO; toast; `recipeTxtFileName()` strips unsafe characters.
- Text parser (`1e8f0b8`, tests `ac6bcb7`): `domain/RecipeTextParser.kt`, pure, returns `ParsedRecipeText`; unknown lines kept in `unrecognized`; nothing invented.
- Import UI (`527450f`, `3bc4e46`, `1b8484c`): `RecipeTextImport` (pure mapping to `EditForm`, tag matching, rows missing amount), `TextImportViewModel` (creates products/tags, validates with `RecipeFormValidator`, always saves a NEW recipe), `TextImportDialog` (paste or open TXT: UTF-8 with windows-1251 fallback, BOM stripped, 1 MB cap; editable preview; ingredients via `BulkIngredientsDialog`). Entry: Recipes header button "Из текста" (tag `import_text`). Tests `ui/RecipeTextImportTest.kt` (`bbc9053`, 5 tests).
- Missing / optional: incoming `ACTION_SEND text/plain` intent-filter routed to `TextImportDialog`; pure share formatter + round-trip test; Robolectric test of the dialog.
- Known limitation: product/tag creation and recipe save are separate calls, not one transaction (leftover products/tags harmless).

### Task 6 — Existing import bug: BLOCKED — WAITING FOR USER FILE
- Requested again by agents #5, #6, #7. When it arrives: reproduce in a unit test against `BackupManager` staging / `BackupJson.decode`, find root cause, fix, verify DB and photos. Do not report as fixed before the original file imports and its data is verified.

### Task 7 — Themes + custom colors: IN PROGRESS (storage only)
- Done (`047d9e6`): `user_settings.themeId` (default LEGACY), `customColors` JSON, in backup v2, `SettingsRepository.setAppearance`.
- Missing: 6 palettes, provider in `Theme.kt`/`MainActivity.kt`, Settings > Appearance with previews, picker, contrast, reset, tests. Keep existing `ThemeMode` and test `darkThemeCanBeSelected` working. Plan in section G.

### Task 8 — Safe APK updates: IN PROGRESS, signing BLOCKED
- Done: build number in About (`16c5dfb`). Missing: build date `buildConfigField`, release signing from secrets, versionCode bump, update-over-install test (F2).

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
| Bulk parser pure domain code; unknown lines kept; duplicates only flagged | Testable, no silent data loss | 9da0cbc |
| Bulk dialog in its own file; products created sequentially via `onAddProduct` | Order preserved | cfbd8d2 |
| Menu: one drop target per meal; picker hides recipes already in the meal | Simple UI, no duplicates | 2e76d8f |
| New composable params always have defaults | Existing callers/tests compile unchanged | 2e76d8f, c5faad8, 1b8484c, 7fcf342 |
| Text import as standalone dialog, always creates a new recipe; TXT read UTF-8 with cp1251 fallback | No blind rewrite of edit screen; existing recipes untouched | 527450f, 3bc4e46, 1b8484c |
| CI read through draft PR #1 check runs | Connection has PR access but no Actions tool | agent #7 |
| Pending: signing (F2); versioning proposal versionCode +1 per release, versionName 1.1.0 | — | — |

---

## E. Git history

- `main` @ `2ceef75` green (CI #11). Last fully green on `modernization`: `5be7b4d` (CI #17).
- Agent #1: `047d9e6`, `aa8edfe`, `862a437`, `1bec32b` (data layer). Agent #2: `44d94bf` handoff.
- Agent #3: `94891a7`, `5961b4f`, `1a764a1`, `4328ad3`, `a8b6896`, `16c5dfb`, `5be7b4d` (CI #17 green).
- Agent #4: `3c0fac1`, `9da0cbc`, `0fec6c8` (CI #18), `eb4350c` handoff.
- Agent #5: `cfbd8d2`, `ab3c0f8`, `95cbf59` (CI), `4c03f6a`, `2e76d8f` (CI), `1f7fe64` handoff.
- Agent #6: `1e8f0b8`, `ac6bcb7` (CI), `b08eef6`, `c5faad8`, `27e4800` (CI), `7fcf342`, `527450f`, `3bc4e46`, `bbc9053`, `1b8484c` (CI), `5b91fda` handoff.
- Agent #7 (2026-11-16 ~21:30 UTC): opened draft PR #1 `modernization` -> `main` (CI tracking only, do not merge); this docs commit (without `[skip ci]`, triggers CI on the full current code).
- All pushes verified by the API response.

---

## F. Blockers and known issues

- **F2 Signing (Task 8, user):** no release key; CI APKs use random debug keys and cannot update each other. If the installed app came from CI, export a backup ZIP first, then reinstall. Plan: permanent keystore in GitHub Secrets (`KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`), `signingConfigs.release`, CI `assembleRelease`. Waiting: how the current app was installed; does the user have a keystore. Asked again by agent #7.
- **F3 Import bug file (Task 6, user):** waiting for the failing file. Asked again by agent #7.
- **F4 Room schemas not committed:** needed for a 1->2 `MigrationTestHelper` test. Generate v1 schema from `2ceef75` in CI, commit `app/schemas/.../1.json`, add `room-testing`.
- **F5** `AGENT.md` outdated; this file supersedes it.
- **F7 Flaky instrumented test on API 34:** CI #18 `AppFlowsTest.addRecipeWithIngredient` failed on pixel_tablet API 34 ("product_search" not found), emulator GPU errors. Passed on API 29 and in #17. Likely flakiness, not proven.
- **F8 CI results of agents #5/#6 unread — now readable:** use draft PR #1 (`pull_request_read` `get_check_runs`). First result expected on agent #7's docs commit (same code as `1b8484c`). Use section H. PR #1 must stay draft and unmerged until all checks are green and migration is tested.

---

## G. NEXT AGENT — START HERE

1. **State:** `modernization` head = agent #7 docs commit; latest code commit `1b8484c`. Last verified green `5be7b4d` / `0fec6c8`. Tasks 1, 3, 5 written but not CI-verified. Draft PR #1 open.
2. **First:** read PR #1 check runs (`pull_request_read`, method `get_check_runs`, pullNumber 1). If still running, wait. Go through section H. Fix compile/test failures with small commits (the last part of each fix without `[skip ci]`, so PR #1 shows the new result). Update `TEST_REPORT.md` and statuses here.
3. **Next action after CI is green:** Task 7 themes. Plan: `ui/theme/AppThemes.kt` (enum `AppThemeId` LEGACY + LIGHT_MINIMAL, WARM_CREAM, NATURAL_GREEN, MODERN_DARK, WARM_TERRACOTTA, CONTEMPORARY_CONTRAST, CUSTOM; each -> full Material3 `ColorScheme`), `ui/theme/CustomColors.kt` (JSON <-> roles, WCAG 4.5:1 contrast check with automatic on-color), `ui/settings/AppearanceSection.kt` (preview cards, apply immediately, reset, HSV picker without hex typing + optional hex), pure tests for palettes/contrast/serialization. Then search for hardcoded colors in `ui/`.
4. **Then:** Task 2 leftovers -> Task 4 round-trip/rollback tests -> F4 migration test -> Task 5 optional share intent -> Task 8 (after user answer) -> Task 6 (after user file).
5. **Files to inspect first:** `ui/theme/Theme.kt`, `ui/theme/Color.kt`, `MainActivity.kt`, `ui/settings/SettingsScreen.kt` (`16c5dfb`), `domain/model/Models.kt` (`DEFAULT_CUISINES`).
6. **Precautions:** never change `applicationId`; no `fallbackToDestructiveMigration`; keep legacy `cuisine` column; do not merge PR #1 until CI green and migration tested; no secrets in git.

---

## H. CHECKLIST FOR THE NEXT CI RUN

Run on agent #7 docs commit (code = `1b8484c`) or newer. If an item fails, the likely cause is listed.

**Compile (`assembleDebug`)**
- [ ] `BulkIngredientsDialog.kt` (`cfbd8d2`, `7fcf342`) — imports; `MeasureUnit.isAmountless`, `MeasureUnit.label`, `Product.baseUnit` must exist.
- [ ] `MenuScreen.kt` (`2e76d8f`) — `Icons.Filled.SwapHoriz` / `DragIndicator` need `material-icons-extended`; drag APIs.
- [ ] `RecipeTextParser.kt` — pure Kotlin; regexes `(?iu)`, `\p{L}`, lookahead; `Math.round(Double).toInt()`.
- [ ] `RecipeDetailRoute.kt` (`27e4800`) — `rememberLauncherForActivityResult` (activity-compose).
- [ ] `RecipeTextImport.kt` — `RecipeRepository.addProduct(name, unit, department): Product`, `addTag(name, color): Tag`, `save(Recipe): Long` callable from a coroutine; `TagPalette` indexable with `.size`.
- [ ] `TextImportDialog.kt` — `InputChip` (material3), `Step.copy(text=)`, `Tag.id/name`, `hiltViewModel()` for `TextImportViewModel`.
- [ ] `RecipesScreen.kt` — header Row and optional param only.

**Unit tests (`testDebugUnitTest`)**
- [ ] `IngredientLineParserTest` (was green), `BulkIngredientsTest` (`95cbf59`), `RecipeTextParserTest` (6 tests; if title fails check `LEADING_SYMBOLS` with emoji; if servings fails check `SERVINGS` vs "Порции: 4 порции"), `RecipeTextImportTest` (5 tests).
- [ ] Robolectric `ComposeScreensTest` / `LogicTest` — may reference "Очистить слот" or the old Recipes header.

**Lint (`lintDebug`, abortOnError)**
- [ ] New files: `NewApi` (none expected, minSdk 29), `Recycle`/`UseKtx` severity.

**Instrumented (API 29 / 34)**
- [ ] `AppFlowsTest` (F7 on API 34). Menu tests that clicked "Очистить слот" must use "Убрать блюдо" / `dish_remove_<id>`.

**Manual check on device when possible (Task 5)**
- [ ] Share -> "Как текст" opens the Sharesheet; "Скопировать текст" -> clipboard; "Сохранить как TXT" -> UTF-8 file.
- [ ] Recipes -> "Из текста" -> paste -> preview -> ingredients -> save -> new recipe opens; original unchanged.
- [ ] "Открыть TXT-файл" with UTF-8 and windows-1251 files.
