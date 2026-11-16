# DEVELOPMENT_HANDOFF.md

Persistent continuation guide for AI coding agents working on this repository.
Read this file first, then `AGENT.md` (older notes, Russian; its "commit to main" rule is superseded).
Last updated: 2026-11-16 17:15 UTC (agent #6 "Frank", second batch).

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
- `ui/recipes/RecipeEditScreen.kt` (form), `ui/recipes/BulkIngredientsDialog.kt` (Task 1, reused by Task 5), `ui/recipes/RecipeViewModels.kt`
- `ui/menu/MenuScreen.kt`, `ui/menu/MenuViewModel.kt` (Task 3)
- `data/repo/Repositories.kt` (MenuRepository addToSlot/replaceRecipe/reorder/move/clear)
- `data/files/BackupManager.kt`, `domain/backup/*` (Task 4)
- `domain/IngredientLineParser.kt` (Task 1 parser)
- Task 5: `ui/recipes/RecipeDetailRoute.kt` (share text, Sharesheet, clipboard, TXT export), `ui/recipes/RecipeDetailScreen.kt` (share menu), `domain/RecipeTextParser.kt` (text parser), `ui/recipes/RecipeTextImport.kt` (mapping + `TextImportViewModel`), `ui/recipes/TextImportDialog.kt` (import UI), `ui/recipes/RecipesScreen.kt` ("Из текста" entry)

### Working rules
- No local JDK/Gradle/SDK. Code changes go through the GitHub API; **CI is the only build/test environment**.
- Commit to `modernization`; merge to `main` only via PR after CI is green.
- Docs-only commits: `[skip ci]`. Multi-part changes: intermediate parts `[skip ci]`, last part without it.
- Never force-push, rewrite history, or commit keystores/passwords/tokens.
- Tooling: "get file contents" returns only the blob SHA (use it as `sha` for updates). Read bodies from commit diffs (`get_commit` full_patch). `87a602d` = original upload of every file; with `perPage=1, page=N` one file is returned (MenuScreen = 32, MenuViewModel = 33, AppNav = 34, RecipeDetailScreen = 35, RecipeEditScreen = 36, RecipeViewModels = 37, RecipesScreen = 38). Write one file per commit. Full body of this file = latest commit diff that rewrote it whole (`b08eef6` and later rewrite the whole file).
- **User instruction (2026-11-17, repeated to agent #6): do not wait for CI; keep writing; record everything so the next run can be checked.** Agents #5 and #6 had no Actions tool, so CI results of their commits are unread.

---

## B. Task checklist

1. [ ] Bulk ingredient input — IMPLEMENTED (UI), parser TESTED
2. [ ] Tags redesign + full cuisine removal — IMPLEMENTED
3. [ ] Multiple recipes per meal — IMPLEMENTED (UI + data)
4. [ ] Complete ZIP and JSON export/import — IMPLEMENTED
5. [ ] Recipe sharing + text import — IMPLEMENTED (all parts written, CI unverified)
6. [ ] Fix existing import bug — BLOCKED, WAITING FOR USER FILE
7. [ ] Six themes + custom colors — IN PROGRESS (storage only)
8. [ ] Safe APK updates — IN PROGRESS, signing BLOCKED

Supporting: migration test on real v1 DB; data preservation; UI consistency; tests; release APK.

---

## C. Development status

Statuses: NOT STARTED / IN PROGRESS / IMPLEMENTED / TESTED / VERIFIED / BLOCKED.

**Global state:** last known green: `5be7b4d` (CI #17, all jobs) and `0fec6c8` (CI #18 build job green; API 34 flaky, F7). Unverified since: agent #5 `cfbd8d2`..`2e76d8f`, agent #6 `1e8f0b8`..`1b8484c`. CI was triggered on `95cbf59`, `2e76d8f`, `ac6bcb7`, `27e4800`, `1b8484c`; **no result read** (F8). See section H for the exact checklist.

### Task 1 — Bulk ingredient input: IMPLEMENTED (UI unverified)
- TESTED: `domain/IngredientLineParser.kt` (`9da0cbc`) + `IngredientLineParserTest` (`0fec6c8`, green in CI #18).
- IMPLEMENTED (`cfbd8d2`, `ab3c0f8`): `ui/recipes/BulkIngredientsDialog.kt` — "Вставить списком" button next to "Добавить ингредиент" (tag `bulk_ingredients`). Dialog: paste (`bulk_input`) -> "Разобрать" (`bulk_parse`) -> preview rows with include checkbox, editable name / amount / unit, remove (`bulk_remove_i`), "not recognized" and "duplicate" hints -> "Добавить (n)" (`bulk_add_all`). Duplicates start unchecked. `BulkIngredients.append` keeps order. Single-add flow untouched.
- `7fcf342` (agent #6): optional `initialText` / `confirmLabel` params (defaults = old behaviour) for reuse by Task 5.
- Unit tests `ui/BulkIngredientsTest.kt` (`95cbf59`), result unknown.
- Missing: Robolectric/instrumented test of the dialog; known risk: if `vm.addCustomProduct` fails and never calls back, remaining rows are not added.

### Task 2 — Tags + cuisine removal: IMPLEMENTED
- Cuisine removed from domain (`047d9e6`), edit state (`5961b4f`), detail (`4328ad3`), edit form (`5be7b4d`). Tags multi-select at bottom, removable chips, separate "new tag" action, case-insensitive dedupe. Legacy DB column `recipes.cuisine` kept.
- Checked by agent #6: `RecipesScreen.kt` has no cuisine filter (unchanged since `87a602d`, filters = category/difficulty/tags).
- Missing: grep remaining `cuisine`/`кухн` references (`domain/RecipeSearch.kt`, `RecipeQuery`, `DEFAULT_CUISINES` in domain model, seed data, Settings, tests, README), tests for tag select/create/remove.

### Task 3 — Multiple recipes per meal: IMPLEMENTED (UI unverified)
- Data (`047d9e6`, `aa8edfe`): `menu_entries.position`, non-unique `(date, slot)` index, repository addToSlot/replaceRecipe/reorder/move/clear with renumbering; same recipe twice in one meal refused.
- UI (`4c03f6a` ViewModel, `2e76d8f` screen): `MenuUiState.dishes(date, slot)`; per dish: drag, up/down (`dish_up_<id>`, `dish_down_<id>`), replace (`dish_replace_<id>`), remove (`dish_remove_<id>`, contentDescription "Убрать блюдо", was "Очистить слот"), servings, cook. Empty meal: slot clickable (`slot_<date>_<SLOT>`); filled: "Ещё блюдо" (`slot_add_<date>_<SLOT>`).
- Missing: check `MenuGeneratorUseCase` / `ShoppingListUseCase` for one-dish assumptions; tests; migration test (F4).

### Task 4 — ZIP/JSON export/import: IMPLEMENTED
- Format v2, staged import with preview/duplicate modes/single transaction/read-back (`862a437`, `1bec32b`); Settings UI (`94891a7`, `16c5dfb`); `BackupJsonTest` v2 (`1a764a1`).
- Missing: round trip with photos, failed-import rollback test, instrumented import read-back.

### Task 5 — Sharing + text import: IMPLEMENTED (unverified)
- Share (`a8b6896`): `recipeShareText()` + Sharesheet (`ACTION_SEND` text/plain via `createChooser`) + clipboard. Single-recipe ZIP share exists.
- TXT export (`c5faad8`, `27e4800`): share menu item "Сохранить как TXT" (tag `export_txt`, new optional param `onExportTxt` on `RecipeDetailContent`). `RecipeDetailRoute` uses `ActivityResultContracts.CreateDocument("text/plain")`, registered before the early return; text kept in `pendingTxt`; `writeTextToUri` writes UTF-8 on IO (mode "wt"); toast on success/failure. `recipeTxtFileName()` strips `\ / : * ? " < > |` and newlines.
- Text parser (`1e8f0b8`, tests `ac6bcb7`): `domain/RecipeTextParser.kt`, see decisions D.
- Import UI (`527450f`, `3bc4e46`, `1b8484c`):
  - `RecipeTextImport` (pure): `toForm(parsed, existingTags: List<Pair<Long,String>>)` fills only found fields; unrecognized lines appended to notes under "Не распознано при импорте:"; servings missing -> form default "2" with a visible hint; `matchTags` (existing ids + missing names, ё/case-insensitive, dedupe); `ingredientText`; `rowsMissingAmount`.
  - `TextImportViewModel` (Hilt): `save(form, rows, newTags, onSaved)` -> products matched by normalized name or created via `repo.addProduct(name, unit, Department.OTHER)` in row order; missing tags created via `repo.addTag(name, TagPalette[i % size])`; validated with `RecipeFormValidator`; saved with id = 0 (always a NEW recipe, existing recipes untouched).
  - `TextImportDialog`: step 1 paste or "Открыть TXT-файл" (`OpenDocument`, `text/plain`, `text/*`; UTF-8, fallback windows-1251 when U+FFFD appears, BOM stripped, 1 MB cap, 50 000 chars cap); step 2 editable preview (name, description, times, servings, oven, kcal, steps add/remove, tags removable incl. "(новый)", notes); step 3 ingredients in `BulkIngredientsDialog(initialText, confirmLabel="Сохранить рецепт")`; rows without amount refused with a toast. No ingredients -> "Сохранить" directly from step 2. Tags: `text_import_input`, `text_import_open`, `text_import_parse`, `text_import_preview`, `text_import_name`, `text_import_step_<i>`, `text_import_notes`, `text_import_next`, `text_import_save`.
  - Entry: Recipes screen header button "Из текста" (tag `import_text`), optional param `onImportText` on `RecipesListContent` (default null). After save: phone -> `onOpen(id)`, tablet -> `vm.select(id)`.
  - Tests `ui/RecipeTextImportTest.kt` (`bbc9053`, 5 tests).
- Missing / optional: incoming `ACTION_SEND text/plain` intent-filter in `AndroidManifest.xml` routed to `TextImportDialog(initialText = ...)` (needs MainActivity + AppNav change); move `recipeShareText` to a pure domain formatter and add a round-trip test format -> parse; Robolectric test of the dialog flow.
- Known limitation: product/tag creation and recipe save are separate repository calls, not one transaction; if the final save fails, created products/tags remain (harmless, reusable). Existing recipes are never modified.

### Task 6 — Existing import bug: BLOCKED — WAITING FOR USER FILE
- Requested from the user again by agents #5 and #6. Do not report as fixed until the original file imports and its data is verified.

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
| Text parser is pure domain code returning `ParsedRecipeText` (not `Recipe`) | No dependency on Recipe model/ViewModel | 1e8f0b8 |
| Text parser: key lines recognized in any section; "Приготовление: 40 мин" = cooking time; unplaced lines kept in `unrecognized`/description | Messenger texts put times after steps; never lose text | 1e8f0b8 |
| Text import as a standalone dialog on the Recipes screen (not inside RecipeEditScreen) | Avoids rewriting the 450-line edit screen blind; reuses EditForm + RecipeFormValidator + BulkIngredientsDialog, so validation and ingredient editing are identical | 527450f, 3bc4e46, 1b8484c |
| Text import always creates a new recipe | "Existing recipes remain unaffected" | 527450f |
| TXT read: UTF-8, fallback windows-1251 | Russian TXT files are often cp1251 | 3bc4e46 |
| All new composable params have defaults (`onExportTxt`, `onImportText`, `initialText`, `confirmLabel`) | Existing tests and callers compile unchanged | c5faad8, 1b8484c, 7fcf342 |
| Pending: signing (F2); versioning proposal versionCode +1 per release, versionName 1.1.0 | — | — |

---

## E. Git history

- Last fully green: `5be7b4d` (CI #17, all jobs). `main` @ `2ceef75` green (CI #11).
- Agent #1: `047d9e6`, `aa8edfe`, `862a437`, `1bec32b` (data layer).
- Agent #2: `44d94bf` handoff.
- Agent #3: `94891a7`, `5961b4f`, `1a764a1`, `4328ad3`, `a8b6896`, `16c5dfb`, `5be7b4d` (compile fix + UI; CI #17 green).
- Agent #4: `3c0fac1` handoff, `9da0cbc` parser, `0fec6c8` parser tests, `eb4350c` handoff.
- Agent #5: `cfbd8d2` [skip ci], `ab3c0f8` [skip ci], `95cbf59` (CI), `4c03f6a` [skip ci], `2e76d8f` (CI), `1f7fe64` handoff.
- Agent #6 (2026-11-16 16:58-17:15 UTC):
  - `1e8f0b8` domain/RecipeTextParser.kt (new) [skip ci]
  - `ac6bcb7` test RecipeTextParserTest (new) — CI triggered
  - `b08eef6` handoff [skip ci]
  - `c5faad8` RecipeDetailScreen: "Сохранить как TXT" menu item [skip ci]
  - `27e4800` RecipeDetailRoute: TXT export — CI triggered
  - `7fcf342` BulkIngredientsDialog: initialText/confirmLabel [skip ci]
  - `527450f` RecipeTextImport.kt (new: mapping + TextImportViewModel) [skip ci]
  - `3bc4e46` TextImportDialog.kt (new) [skip ci]
  - `bbc9053` test RecipeTextImportTest (new) [skip ci]
  - `1b8484c` RecipesScreen: "Из текста" entry — CI triggered (covers all of the above)
  - this docs commit [skip ci]
- All pushes verified by the API response (new commit SHA returned).

---

## F. Blockers and known issues

- **F2 Signing (Task 8, user):** no release key; CI APKs use random debug keys and cannot update each other. If the installed app came from CI, export a backup ZIP first, then reinstall. Plan: permanent keystore in GitHub Secrets (`KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`), `signingConfigs.release`, CI `assembleRelease`. Waiting: how the current app was installed; does the user have a keystore.
- **F3 Import bug file (Task 6, user):** waiting for the failing file. Everything else continues independently.
- **F4 Room schemas not committed:** needed for a 1->2 `MigrationTestHelper` test. Generate v1 schema from `2ceef75` in CI, commit `app/schemas/.../1.json`, add `room-testing`.
- **F5** `AGENT.md` outdated; this file supersedes it.
- **F7 Flaky instrumented test on API 34:** CI #18 `AppFlowsTest.addRecipeWithIngredient` failed on pixel_tablet API 34 ("product_search" not found), emulator GPU errors. Passed on API 29 and in #17. Likely flakiness, not proven.
- **F8 CI results of agent #5/#6 commits unread:** their GitHub connection exposes no Actions tools. Per user instruction, work continued without waiting. Use section H.

---

## G. NEXT AGENT — START HERE

1. **State:** `modernization` head = this docs commit; latest code commit `1b8484c`. Last verified green `5be7b4d` / `0fec6c8`. Tasks 1, 3, 5 written but not CI-verified.
2. **First:** read the CI run for `1b8484c` (it builds everything since `0fec6c8`). Go through section H. Fix compile/test failures with small commits. Update `TEST_REPORT.md` and statuses here.
3. **Next action after CI is green (or if CI is still unreadable, per user: keep writing):** Task 7 themes. Inspect `ui/theme/Theme.kt`, `ui/theme/Color.kt` (with `TagPalette`), `MainActivity.kt`, `ui/settings/SettingsScreen.kt` (`16c5dfb`), `data/repo` SettingsRepository (`setAppearance`, `themeId`, `customColors`). Plan: `ui/theme/AppThemes.kt` (enum `AppThemeId` LEGACY + LIGHT_MINIMAL, WARM_CREAM, NATURAL_GREEN, MODERN_DARK, WARM_TERRACOTTA, CONTEMPORARY_CONTRAST, CUSTOM; each -> full Material3 `ColorScheme`), `ui/theme/CustomColors.kt` (JSON <-> roles, contrast check WCAG 4.5:1 with automatic on-color), `ui/settings/AppearanceSection.kt` (preview cards, apply immediately, reset, HSV picker without hex typing + optional hex), pure tests for palettes/contrast/serialization.
4. **Then:** Task 2 leftovers grep -> Task 4 round-trip/rollback tests -> F4 migration test -> Task 5 optional share intent -> Task 8 (after user answer) -> Task 6 (after user file).
5. **Precautions:** never change `applicationId`; no `fallbackToDestructiveMigration`; keep legacy `cuisine` column; no merge to `main` until CI green and migration tested; no secrets in git.

---

## H. CHECKLIST FOR THE NEXT CI RUN (written for agent #7)

Run on `1b8484c` (or newer). For each item: if it fails, the most likely cause is listed.

**Compile (`assembleDebug`)**
- [ ] `BulkIngredientsDialog.kt` (`cfbd8d2`, `7fcf342`) — imports; `MeasureUnit.isAmountless`, `MeasureUnit.label`, `Product.baseUnit` must exist.
- [ ] `MenuScreen.kt` (`2e76d8f`) — `Icons.Filled.SwapHoriz` / `DragIndicator` need `material-icons-extended`; drag APIs.
- [ ] `RecipeTextParser.kt` — pure Kotlin; regexes use `(?iu)`, `\p{L}`, negative lookahead; `Math.round(Double).toInt()`.
- [ ] `RecipeDetailRoute.kt` (`27e4800`) — `androidx.activity.compose.rememberLauncherForActivityResult` (activity-compose must be a dependency; it is if MainActivity uses `setContent`).
- [ ] `RecipeTextImport.kt` — `RecipeRepository.addProduct(name, unit, department): Product` and `addTag(name, color): Tag` and `save(Recipe): Long` must be callable from a coroutine (suspend or not); `TagPalette` in `ui.theme` must support `[index]` and `.size` (List or Array).
- [ ] `TextImportDialog.kt` — `InputChip` (material3), `Step.copy(text=)`, `Tag.id/name`, `hiltViewModel()` for `TextImportViewModel`.
- [ ] `RecipesScreen.kt` — only added a header Row and optional param.

**Unit tests (`testDebugUnitTest`)**
- [ ] `IngredientLineParserTest` (was green).
- [ ] `BulkIngredientsTest` (`95cbf59`).
- [ ] `RecipeTextParserTest` (6 tests, `ac6bcb7`). If `appShareFormat_allFieldsRecovered` fails on the title, check `LEADING_SYMBOLS` with the emoji; if on servings, check `SERVINGS` vs "Порции: 4 порции".
- [ ] `RecipeTextImportTest` (5 tests, `bbc9053`).
- [ ] Robolectric `ComposeScreensTest` / `LogicTest` — may reference old menu texts ("Очистить слот") or the Recipes header layout.

**Lint (`lintDebug`, abortOnError)**
- [ ] New files: unused imports are warnings only; check for `NewApi` (none expected, minSdk 29) and `Recycle`/`UseKtx` severity in the lint config.

**Instrumented (API 29 / 34)**
- [ ] `AppFlowsTest` (F7 flakiness on API 34). Menu tests that clicked "Очистить слот" must use "Убрать блюдо" / `dish_remove_<id>`.

**Manual check on device when possible (Task 5)**
- [ ] Recipe -> Share -> "Как текст" opens the Sharesheet (Telegram/WhatsApp/e-mail).
- [ ] "Скопировать текст" -> toast, clipboard contains the recipe.
- [ ] "Сохранить как TXT" -> file picker, file is UTF-8 and readable.
- [ ] Recipes -> "Из текста" -> paste that TXT text -> preview fields match -> ingredients step -> save -> new recipe opens; original recipe unchanged.
- [ ] "Открыть TXT-файл" with a UTF-8 and a windows-1251 file.
