# DEVELOPMENT_HANDOFF.md

Persistent continuation guide for AI coding agents working on this repository.
Read this file first, then `AGENT.md` (older notes, Russian; its "commit to main" rule is superseded).
Last updated: 2026-11-16 22:10 UTC (agent #7 "Jessica").

---

## A. Project overview

| Item | Value |
|---|---|
| Project | "Домашнее меню" (Home Menu), offline Android app: recipes, weekly menu, shopping list. UI Russian only. |
| Repository | `Vano3667/recipe-app` (private). Default `main`, integration branch `modernization`. Draft PR #1 `modernization` -> `main` (CI tracking, do not merge yet). |
| Stack | Kotlin 1.9.24, Jetpack Compose (BOM 2024.06.00) + Material 3, MVVM (`data`/`domain`/`ui`), Hilt 2.51.1, Navigation Compose 2.7.7, Coroutines/Flow |
| Database | Room 2.6.1 (KSP), file `home_menu.db`, schema **v2** on `modernization` (main: v1), `MIGRATION_1_2` additive, pre-migration copy in `DatabaseSafety.kt` |
| Other libs | kotlinx.serialization 1.6.3 (backup JSON, custom colours JSON), Coil 2.6.0 (photos in `filesDir/photos`) |
| Build | Gradle 8.7 (wrapper NOT committed; CI runs `gradle wrapper --gradle-version 8.7`), JDK 17, minSdk 29, target/compile 34 |
| applicationId | `com.example.homemenu` (must never change) |
| Version | `versionCode = 1`, `versionName = "1.0"` |
| Signing | No signingConfig. CI builds `assembleDebug` with the runner's random debug key (F2). |
| CI | `.github/workflows/android.yml`: job `build` (assembleDebug, testDebugUnitTest incl. Robolectric, lintDebug abortOnError) + jobs `instrumented (29, pixel_4)` and `instrumented (34, pixel_tablet)`. Runs on push and on pull_request (two runs per PR commit). ~4 min build, ~10 min instrumented. |
| Commands (CI) | `./gradlew assembleDebug testDebugUnitTest lintDebug`, `./gradlew connectedDebugAndroidTest` |
| Other docs | `README.md`, `DESIGN.md`, `TEST_REPORT.md` (results up to run #10, outdated), `ENVIRONMENT.md`, `CREDITS.md`, `AGENT.md` (outdated) |

### Important files (under `app/src/main/java/com/example/homemenu/`)
- Task 1: `domain/IngredientLineParser.kt`, `ui/recipes/BulkIngredientsDialog.kt`, `ui/recipes/RecipeEditScreen.kt`
- Task 3: `ui/menu/MenuScreen.kt`, `ui/menu/MenuViewModel.kt`, `data/repo/Repositories.kt` (MenuRepository)
- Task 4: `data/files/BackupManager.kt`, `domain/backup/*`, `ui/settings/SettingsViewModel.kt`
- Task 5: `ui/recipes/RecipeDetailRoute.kt`, `ui/recipes/RecipeDetailScreen.kt`, `domain/RecipeTextParser.kt`, `ui/recipes/RecipeTextImport.kt`, `ui/recipes/TextImportDialog.kt`, `ui/recipes/RecipesScreen.kt`
- Task 7: `ui/theme/ThemePalettes.kt` (pure), `ui/theme/Theme.kt`, `ui/theme/Color.kt` (legacy schemes, `tagColors`, `TagPalette`), `ui/MainViewModel.kt`, `MainActivity.kt`, `ui/settings/AppearanceViewModel.kt`, `ui/settings/AppearanceSection.kt`, `ui/settings/SettingsScreen.kt`
- DB: `data/db/AppDatabase.kt`, `data/db/Entities.kt` (`UserSettingsEntity.themeId`, `customColors`), `data/db/DatabaseSafety.kt`

### Working rules
- No local JDK/Gradle/SDK. Code changes go through the GitHub API; **CI is the only build/test environment**.
- **Per task: feature branch from `modernization` (e.g. `task7-themes`), commits there, draft PR into `modernization`, read its checks, merge only when `build` and both instrumented jobs are green in at least one run on the head commit.** `modernization` -> `main` only via PR #1 after all tasks are verified and migration is tested.
- Docs-only commits: `[skip ci]`. Multi-part changes: intermediate parts `[skip ci]`, last part without it.
- Never force-push, rewrite history, or commit keystores/passwords/tokens.
- Tooling: "get file contents" returns only the blob SHA (use it as `sha` for updates). Read bodies from commit diffs (`get_commit` full_patch). `87a602d` = original upload of every file; with `perPage=1, page=N` one file is returned (MainActivity = 11, MainViewModel = 28, MenuScreen = 32, MenuViewModel = 33, AppNav = 34, RecipeDetailScreen = 35, RecipeEditScreen = 36, RecipeViewModels = 37, RecipesScreen = 38, SettingsScreen = 39, SettingsViewModel = 40, Color.kt = 43, Theme.kt = 44). Files rewritten whole by agent #7 (their latest commit = full body): `ThemePalettes.kt` `2d39e8a`, `Theme.kt` `aab7df6`, `MainViewModel.kt` `a12c184`, `MainActivity.kt` `1cadd26`, `AppearanceViewModel.kt` `37cd525`, `AppearanceSection.kt` `8bc9e0d`, `SettingsScreen.kt` `e3dd579`, `ThemePalettesTest.kt` `eca6f24`, this file.
- Write one file per call; **never write two files of the same branch in parallel** (second write fails with 409).
- `search_code` (`repo:Vano3667/recipe-app`) returns code fragments but indexes only `main`.
- **CI visibility:** no Actions tool, but `pull_request_read` method `get_check_runs` on a PR returns the head commit's jobs with status/conclusion (no logs). Logs of a failed job: ask the user to open the job link (Actions tab) and paste the failing step.

---

## B. Task checklist

1. [ ] Bulk ingredient input — TESTED (CI green; manual UI check pending)
2. [ ] Tags redesign + full cuisine removal — IMPLEMENTED (leftover references remain)
3. [ ] Multiple recipes per meal — TESTED (CI green; manual UI check and migration test pending)
4. [ ] Complete ZIP and JSON export/import — IMPLEMENTED (round-trip/rollback tests missing)
5. [ ] Recipe sharing + text import — TESTED (CI green; manual Sharesheet/TXT check pending)
6. [ ] Fix existing import bug — BLOCKED, WAITING FOR USER FILE
7. [ ] Six themes + custom colours — TESTED (CI green, merged; hard-coded colour scan + UI test pending)
8. [ ] Safe APK updates — IN PROGRESS, signing BLOCKED

Supporting: migration test on real v1 DB; data preservation; UI consistency; release APK.

---

## C. Development status

Statuses: NOT STARTED / IN PROGRESS / IMPLEMENTED / TESTED / VERIFIED / BLOCKED. TESTED = CI build + unit + Robolectric + lint + instrumented green; VERIFIED would also need a manual check on a device.

**Global state (2026-11-16 22:10 UTC):**
- `d34b01d` (= code of `1b8484c`, all work of agents #1-#6): 3 CI runs. `build` green 3/3, API 29 green 3/3, API 34 green 2/3 (one failure, same code: flaky, see F7). **Tasks 1, 3, 5 compile and pass all existing tests.**
- `2d39e8a` (Task 7 on `task7-themes`): 2 runs. `build` green 2/2, API 29 green 2/2, API 34 green 1/2 (F7 again). Merged into `modernization` as `d71f480` (PR #2).

### Task 1 — Bulk ingredient input: TESTED
- Parser `domain/IngredientLineParser.kt` (`9da0cbc`) + `IngredientLineParserTest` (`0fec6c8`). Dialog `ui/recipes/BulkIngredientsDialog.kt` (`cfbd8d2`, `ab3c0f8`, `7fcf342`): "Вставить списком" (tag `bulk_ingredients`) -> paste (`bulk_input`) -> "Разобрать" (`bulk_parse`) -> editable preview rows, remove (`bulk_remove_i`), "not recognized" / "duplicate" hints, duplicates unchecked -> "Добавить (n)" (`bulk_add_all`). Order preserved. Single-add untouched. `BulkIngredientsTest` (`95cbf59`) green.
- Missing: Robolectric/instrumented test of the dialog; known risk: if `vm.addCustomProduct` fails and never calls back, remaining rows are not added.

### Task 2 — Tags + cuisine removal: IMPLEMENTED (leftovers)
- Cuisine removed from domain (`047d9e6`), edit state (`5961b4f`), detail (`4328ad3`), edit form (`5be7b4d`). Tags multi-select at bottom, removable chips, separate "new tag" action, case-insensitive dedupe (`aa8edfe`). Legacy DB column `recipes.cuisine` kept and preserved on edit.
- Code search on `main` found cuisine in: `data/Mappers.kt`, `data/seed/Seeder.kt`, `data/seed/SeedData.kt`, `domain/model/Models.kt` (`DEFAULT_CUISINES`), `ui/recipes/RecipeViewModels.kt`, `ui/recipes/RecipeEditScreen.kt` (`CuisineField`), `ui/recipes/RecipeDetailScreen.kt`, `data/repo/Repositories.kt`, `data/files/BackupManager.kt`, `domain/backup/BackupFormat.kt`, `data/db/Entities.kt`, `BackupJsonTest.kt`. Several are fixed on `modernization`; re-check each there via commit diffs.
- Missing: remove remaining references (`DEFAULT_CUISINES`, `CuisineField` if still defined, seed data, `RecipeSearch`/`RecipeQuery`, README); keep the DB column and backup v1 read compatibility; tests for tag select/create/remove.

### Task 3 — Multiple recipes per meal: TESTED
- Data (`047d9e6`, `aa8edfe`): `menu_entries.position`, non-unique `(date, slot)` index, addToSlot/replaceRecipe/reorder/move/clear with renumbering; same recipe twice in one meal refused.
- UI (`4c03f6a`, `2e76d8f`): per dish drag, up/down (`dish_up_<id>`, `dish_down_<id>`), replace (`dish_replace_<id>`), remove (`dish_remove_<id>`, "Убрать блюдо"), servings, cook; "Ещё блюдо" (`slot_add_<date>_<SLOT>`).
- Missing: check `MenuGeneratorUseCase` / `ShoppingListUseCase` for one-dish assumptions; tests for dishes()/reorder; migration test (F4).

### Task 4 — ZIP/JSON export/import: IMPLEMENTED
- Format v2 (manifest, SHA-256, Base64 images in JSON, v1 readable), staged import with preview / duplicate modes (skip, update, separate) / one transaction / photo rollback / read-back (`862a437`, `1bec32b`); Settings UI (`94891a7`, `16c5dfb`); `BackupJsonTest` v2 (`1a764a1`). Backup includes `themeId` and `customColors`.
- Missing: round trip with photos, failed-import rollback test, instrumented import read-back.

### Task 5 — Sharing + text import: TESTED
- Share text + Sharesheet + clipboard (`a8b6896`); TXT export (`c5faad8`, `27e4800`); text parser (`1e8f0b8`, tests `ac6bcb7`); import UI (`527450f`, `3bc4e46`, `1b8484c`), tests `RecipeTextImportTest` (`bbc9053`). Entry: Recipes header "Из текста" (`import_text`). Always creates a NEW recipe.
- Missing / optional: incoming `ACTION_SEND text/plain` intent-filter; pure share formatter + round-trip test; Robolectric test of the dialog. Known limitation: product/tag creation and recipe save are not one transaction.

### Task 6 — Existing import bug: BLOCKED — WAITING FOR USER FILE
- Requested by agents #5, #6, #7. When it arrives: reproduce in a unit test against `BackupManager` staging / `BackupJson.decode`, find root cause, fix, verify DB and photos.

### Task 7 — Themes + custom colours: TESTED (merged `d71f480`)
- `ui/theme/ThemePalettes.kt` (pure Kotlin): `AppThemeId` = LEGACY (original look, default, follows ThemeMode) + 6 presets LIGHT_MINIMAL "Светлая", WARM_CREAM "Кремовая", NATURAL_GREEN "Зелёная", MODERN_DARK "Тёмная", WARM_TERRACOTTA "Терракотовая", CONTEMPORARY_CONTRAST "Контрастная" + CUSTOM. `ThemePalette` (ARGB Long roles), `CustomRole` (9 roles: background, secondary background, surfaces, cards, buttons+titles = primary, primary accent = primaryContainer, secondary accent, text, secondary text), `CustomAppearance` (base preset + overrides, JSON `{"base":..,"ROLE":"#RRGGBB"}`, tolerant decode), `ColorMath` (WCAG contrast, readable on-colour, blend, hex, HSV), `contrastWarnings`.
- `Theme.kt`: `HomeMenuTheme(mode, themeId = null, customColors = null, content)`; `ThemePalette.toColorScheme()` fills all M3 roles, every on-colour >= 4.5:1 (falls back to white/near-black); `appIsDark()`; `isAppDark()` now by background luminance (works for all themes).
- `MainViewModel.appearance` (from `SettingsRepository.observeSettings()`), `MainActivity` applies it, system bar icons follow the theme.
- `AppearanceViewModel` writes immediately via `SettingsRepository.setAppearance`; custom colours kept when switching themes; `resetToDefault` -> LEGACY; `resetCustom`; `setCustomBase`.
- `AppearanceSection` (card "Оформление" below "Тема оформления"): preview cards drawn in each theme's colours (tags `theme_card_<ID>`), tap applies; reset button (`appearance_reset`); custom editor (`custom_colors`, `custom_base`, `custom_role_<ROLE>`, `custom_reset`) with contrast warnings; `ColorPickerDialog`: swatches, hue/saturation/brightness sliders (`picker_hue`, `picker_saturation`, `picker_value`), optional HEX (`picker_hex`), `picker_ok`.
- `SettingsContent` got optional `appearance` slot (null hides the card; `ComposeScreensTest` unchanged).
- Tests: `ThemePalettesTest` (10 unit tests) green.
- Missing: scan all screens for hard-coded colours (`Color(0x...)`, `Color.White/Black`, `tagColors(hint, dark)` callers) and switch them to `MaterialTheme.colorScheme`; Robolectric test of AppearanceContent (select theme, picker); manual check on device. Known: on cold start the first frame uses LEGACY until settings load (Eagerly-started StateFlow, short flash possible).

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
| Bulk parser pure domain code; unknown lines kept; duplicates only flagged | Testable, no silent data loss | 9da0cbc |
| New composable params always have defaults | Existing callers/tests compile unchanged | several |
| Text import as standalone dialog, always a new recipe; TXT UTF-8 with cp1251 fallback | Existing recipes untouched | 527450f, 3bc4e46 |
| LEGACY stays the default theme; 6 presets added beside it | Existing users see no change after the update | 6c9d3bc |
| Theme model is pure Kotlin with ARGB Long colours | Unit-testable without Compose/Robolectric | 6c9d3bc |
| Custom theme = preset base + overrides; on-colours computed, never stored | Text stays readable whatever the user picks; warnings shown | 6c9d3bc |
| Appearance has its own ViewModel and an optional slot in SettingsContent | No rewrite of SettingsViewModel; tests unaffected | 37cd525, e3dd579 |
| Appearance card below the light/dark card | `AppFlowsTest.darkThemeCanBeSelected` clicks `theme_DARK` without scrolling | e3dd579 |
| Feature branch + PR per task; CI read via PR check runs | Connection has PR access but no Actions tool | agent #7 |
| Pending: signing (F2); versioning proposal versionCode +1 per release, versionName 1.1.0 | — | — |

---

## E. Git history

- `main` @ `2ceef75` green (CI #11).
- Agent #1: `047d9e6`, `aa8edfe`, `862a437`, `1bec32b`. Agent #2: `44d94bf`. Agent #3: `94891a7`, `5961b4f`, `1a764a1`, `4328ad3`, `a8b6896`, `16c5dfb`, `5be7b4d` (CI #17 green). Agent #4: `3c0fac1`, `9da0cbc`, `0fec6c8`, `eb4350c`. Agent #5: `cfbd8d2`, `ab3c0f8`, `95cbf59`, `4c03f6a`, `2e76d8f`, `1f7fe64`. Agent #6: `1e8f0b8`, `ac6bcb7`, `b08eef6`, `c5faad8`, `27e4800`, `7fcf342`, `527450f`, `3bc4e46`, `bbc9053`, `1b8484c`, `5b91fda`.
- Agent #7 (2026-11-16 21:30-22:10 UTC):
  - draft PR #1 `modernization` -> `main` (CI tracking, open, do not merge)
  - `d34b01d` handoff rewrite (no `[skip ci]` on purpose -> first readable CI of agents #5/#6 work: green, API 34 flaky)
  - branch `task7-themes`: `6c9d3bc` ThemePalettes, `aab7df6` Theme, `a12c184` MainViewModel, `1cadd26` MainActivity, `37cd525` AppearanceViewModel, `8bc9e0d` AppearanceSection, `d7ec590` + `e3dd579` SettingsScreen, `eca6f24` ThemePalettesTest, `2d39e8a` ThemePalettes fix (CI green)
  - PR #2 `task7-themes` -> `modernization` merged: `d71f480`
  - this docs commit [skip ci]

---

## F. Blockers and known issues

- **F2 Signing (Task 8, user):** no release key; CI APKs use random debug keys and cannot update each other. If the installed app came from CI, export a backup ZIP first, then reinstall. Plan: permanent keystore in GitHub Secrets (`KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`), `signingConfigs.release`, CI `assembleRelease`. Waiting: how the current app was installed; does the user have a keystore.
- **F3 Import bug file (Task 6, user):** waiting for the failing file.
- **F4 Room schemas not committed:** needed for a 1->2 `MigrationTestHelper` test. Generate v1 schema from `2ceef75` in CI, commit `app/schemas/.../1.json`, add `room-testing`.
- **F5** `AGENT.md` and `TEST_REPORT.md` outdated.
- **F7 Flaky instrumented job on API 34 (confirmed):** the same commit failed API 34 in 1 of 3 runs (`d34b01d`) and 1 of 2 runs (`2d39e8a`), while other runs of the same code passed. Earlier seen in CI #18 (`AppFlowsTest.addRecipeWithIngredient`, "product_search" not found, emulator GPU errors). Logs of the new failures not read (no log access). Next step: ask the user for the failing test name from the job log, then harden waits in `AppFlowsTest` (longer timeout / waitUntil on the tablet layout) or add a retry for that job.
- **F8 resolved:** CI readable via PR check runs (see A).

---

## G. NEXT AGENT — START HERE

1. **State:** `modernization` @ `d71f480` + this docs commit. Tasks 1, 3, 5, 7 TESTED (CI green). Check PR #1 check runs for the merge commit `d71f480` first.
2. **Next action:** Task 2 leftovers on a new branch `task2-cuisine-cleanup`: grep (via `search_code` on `main` + commit diffs on `modernization`) for `cuisine`, `Cuisine`, `кухн`; remove `DEFAULT_CUISINES`, `CuisineField`, seed `cuisine` usage (keep `RecipeEntity.cuisine` column and backup v1 reading), update README; add tests for tag create/dedupe. PR into `modernization`.
3. **Then:** Task 7 hard-coded colour scan -> Task 4 round-trip/rollback tests -> F4 migration test -> F7 hardening -> Task 5 optional share intent -> Task 8 (after user answer) -> Task 6 (after user file). Update `TEST_REPORT.md`.
4. **Files to inspect first:** `domain/model/Models.kt`, `data/seed/SeedData.kt`, `data/seed/Seeder.kt`, `domain/RecipeSearch.kt`, `ui/recipes/RecipeEditScreen.kt` (`87a602d` page 36 + `5be7b4d`, `ab3c0f8`).
5. **Precautions:** never change `applicationId`; no `fallbackToDestructiveMigration`; keep legacy `cuisine` column; do not merge PR #1 until CI green and migration tested; no secrets in git; one file write at a time per branch.

---

## H. MANUAL CHECKS ON A DEVICE (not possible for agents)

- Task 1: Recipe edit -> "Вставить списком" -> paste 5 lines -> preview -> edit one, remove one -> add -> save -> reopen: order and amounts kept.
- Task 3: Menu -> one meal with 3 dishes -> reorder, replace, remove -> restart app -> same list.
- Task 5: Share -> "Как текст" (Sharesheet), "Скопировать текст", "Сохранить как TXT"; Recipes -> "Из текста" -> paste -> preview -> save; TXT in UTF-8 and windows-1251.
- Task 7: Settings -> "Оформление" -> each of the 6 themes applies at once on all tabs (recipes, menu, shopping, settings, dialogs); "Свои цвета" -> change background/buttons/text -> restart -> kept; "Вернуть тему по умолчанию"; look for unreadable text or colours that do not change.
