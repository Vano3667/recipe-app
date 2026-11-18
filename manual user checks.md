
## 23. USER TESTING, REPORTED BUGS, AND ADDITIONAL REQUIREMENTS

This section contains new requirements and issues reported by the user after testing the current application. Treat these as mandatory development tasks.

Do not unnecessarily rewrite features that the user has already confirmed are working. Focus on reported problems, required improvements, independent verification, and regression prevention.

### 23.1. CRITICAL: APK Installation Failure — Signature Mismatch

The user reported that installing the newly built APK results in an error indicating that the application package signatures do not match.

Important context:
- The user has already uninstalled the previous application.
- The user is attempting to install the build that was available in the GitHub repository on **November 17, 2026, at 17:08 (UTC+11)**.

Required actions:
1. Inspect the repository history and identify the exact APK, commit, or build corresponding to the specified date and time.
2. Inspect the APK signing certificate and compare it with the current signing configuration.
3. Determine the actual cause of the signature mismatch.
4. Check whether the project uses a debug key, release keystore, environment-based signing, or another signing mechanism.
5. Restore the original signing configuration if the original key is available.
6. Never generate a replacement keystore and claim that it is compatible with the original application.
7. Do not claim that uninstalling the old application resolves signing compatibility for future updates.
8. Verify installation using the actual APK on an Android device or emulator, if available.

If the original signing key is missing, document this as a critical blocker in DEVELOPMENT_HANDOFF.md. Continue all unrelated development tasks.

Do not claim that APK update compatibility is verified until it has been tested successfully.

### 23.2. CRITICAL: Fix JSON Import Using the Existing Repository Fixture

The user reports that JSON import currently does not work.

An important test file already exists in the repository:
- It is located in the `main` branch.
- It is an example JSON file containing the ingredient list previously created by the user and ChatGPT together.
- The user explicitly expects the agent to locate and use this existing file.

Required actions:
1. Inspect the `main` branch and locate the existing JSON example.
2. Do not ask the user to upload the file again if it is accessible in the repository.
3. Use the original file as the primary regression-test fixture.
4. Inspect its actual structure, encoding, field names, data types, quantities, units, and ordering.
5. Reproduce the import failure using this exact file.
6. Identify and fix the underlying import problem.
7. Do not modify the original fixture merely to bypass the bug.
8. Import the original file and verify that all expected records are actually saved in the database.
9. Verify ingredient names, quantities, units, and original ordering.
10. Add an automated regression test using this fixture where technically possible.
11. Check that the fix does not break other supported import formats.

A successful file selection or parsing preview is not sufficient. Verify the actual database result after import.

### 23.3. Appearance Settings — Compact Theme Selector

The user reports that the current theme selection interface occupies too much vertical space.

Required UI changes:
1. Replace the current large theme selection layout with a compact dropdown-based selector.
2. Display theme options in a two-column arrangement, with two themes per row.
3. Reduce unnecessary vertical spacing and the overall height of the Appearance settings section.
4. Preserve all six existing themes:
   - Light Minimal
   - Warm Cream
   - Natural Green
   - Modern Dark
   - Warm Terracotta
   - Contemporary Contrast
5. Preserve theme previews.
6. Preserve instant theme application.
7. Preserve selected-theme persistence after restarting the application.
8. Preserve custom color selection and all existing custom color functionality.
9. Ensure that the compact layout works correctly on different Android screen sizes.

Do not unnecessarily rewrite the theme engine if it is already functioning correctly. Focus on the selection interface and regression testing.

### 23.4. Task 1 — Bulk Ingredient Input: Fix Ingredient Reordering

The user manually tested bulk ingredient input and reported:

- Adding ingredients as a list: WORKING.
- Parsing ingredients: WORKING.
- Deleting ingredients: WORKING.
- Moving/reordering ingredients: NOT WORKING.

Required actions:
1. Investigate the ingredient reordering implementation.
2. Check drag-and-drop behavior, event callbacks, state updates, and persistence.
3. Fix the ability to change ingredient order.
4. Ensure the displayed order updates immediately.
5. Ensure the new order is saved to persistent storage/database.
6. Close and reopen the recipe and verify that the new order is preserved.
7. Verify that editing, deleting, and adding ingredients still work correctly.

Do not rewrite the already working bulk-input functionality unnecessarily.

### 23.5. Task 2 — Tags: User Testing Report

The user reports that the tags functionality is working.

Required actions:
1. Preserve the current working implementation.
2. Avoid unnecessary rewrites.
3. Perform regression testing for:
   - Selecting existing tags.
   - Creating new tags.
   - Reusing existing tags.
   - Removing tags.
   - Saving and reopening recipes.
   - Persistence of tag assignments.
4. Verify that the old cuisine classification has been completely removed from the relevant UI and application logic.

### 23.6. Task 3 — Multiple Recipes per Meal: User Testing Report

The user reports that adding multiple dishes to one meal is working.

Required actions:
1. Preserve the current implementation.
2. Avoid unnecessary rewrites.
3. Perform regression testing for:
   - Adding multiple recipes to one meal.
   - Removing recipes.
   - Reordering recipes.
   - Saving and reopening meal plans.
   - Data persistence.
   - Compatibility with existing database migrations and older data.

### 23.7. Task 4 — ZIP and JSON Import/Export: User Testing Report

The user reports that the general import/export functionality is working.

However, this does NOT mean that the specific JSON import failure described in Section 23.2 has been resolved.

Required actions:
1. Verify ZIP export and import.
2. Verify JSON export and import.
3. Use the actual JSON fixture from the `main` branch as a mandatory test.
4. Verify data integrity after importing.
5. Verify ingredient names, quantities, units, ordering, tags, instructions, and recipe relationships.
6. Verify that attachments and images are preserved where supported.
7. Perform round-trip testing: export → import → compare the resulting data.
8. Verify that importing does not silently lose or corrupt existing data.

Do not mark all import/export functionality as fully verified until the relevant tests have passed.

### 23.8. Task 7 — Themes: User Testing Report

The user reports that themes are working, but the theme selection interface needs the redesign described in Section 23.3.

Required actions:
1. Preserve the working theme functionality.
2. Implement the compact dropdown selector.
3. Arrange theme options in two columns, with two themes per row.
4. Preserve all six themes.
5. Preserve custom colors, previews, instant application, and persistence.
6. Perform regression testing after the UI changes.

Do not unnecessarily rewrite the existing theme engine.

### 23.9. Task 8 — APK Version Information and Update Verification

The user reports that application version information is now displayed.

However, the user has not yet tested installing a new APK over the existing application. The user intends to test this after the next development iteration.

Required actions:
1. Verify that the application displays the correct version information.
2. Check `versionName` and `versionCode`.
3. Verify that `applicationId` remains unchanged.
4. Verify the signing configuration and certificate.
5. Investigate the signature mismatch described in Section 23.1.
6. Test installing a newer APK over the existing application, if the required original signing key is available.
7. Verify that existing user data and database contents remain intact after the update.
8. Do not claim update compatibility is verified until an actual successful update installation has been performed.

### 23.10. Shopping List Export to WhatsApp and Telegram — Interactive Checkboxes

The user wants to share the shopping list through WhatsApp and Telegram.

The important requirement is that shopping list items should ideally have checkboxes that can be marked directly in the messenger while shopping.

Required actions:
1. Inspect the current shopping list sharing/export implementation.
2. Test the available Android sharing mechanisms for WhatsApp and Telegram.
3. Investigate what interactive checklist functionality each messenger actually supports.
4. Check whether Telegram checklist/task-list functionality can be used through the available sharing or integration mechanisms.
5. Check WhatsApp's actual support for interactive checkboxes in ordinary shared messages.
6. Do not falsely claim that Unicode checkbox characters such as `☐` and `☑` are interactive.
7. If a messenger supports only ordinary text messages, provide the most practical alternative.
8. Consider a simple format using checkbox symbols, readable item names, quantities, and units.
9. Avoid introducing unnecessary servers, databases, or external infrastructure.

The expected user workflow is:

1. Open the shopping list in the application.
2. Tap Share/Export.
3. Select WhatsApp or Telegram.
4. Send the list to the desired chat.
5. Open the shared list while shopping.
6. Mark purchased items using whatever interactive functionality the selected messenger genuinely supports.

Prioritize a reliable and convenient implementation over a technically complicated workaround.

If direct interactive checkboxes are not supported by the messenger's available sharing interface, document the limitation clearly and implement the best practical alternative.

### 23.11. DEVELOPMENT_HANDOFF.md — User Testing and Reported Issues

Extend `DEVELOPMENT_HANDOFF.md` with a dedicated section:

`USER TESTING AND REPORTED ISSUES`

For every user-reported issue or manually tested feature, record:

- Test ID.
- Related task.
- Feature or issue description.
- User-reported result.
- Developer verification result.
- Current status.
- Required next action.
- Relevant commit hash.

Use these statuses consistently:

- `USER REPORTED — NOT REPRODUCED`
- `REPRODUCED`
- `FIX IN PROGRESS`
- `FIX IMPLEMENTED`
- `DEVELOPER TESTED`
- `USER RETEST REQUIRED`
- `VERIFIED`
- `BLOCKED`

Important rules:
1. Distinguish user testing from developer testing.
2. A feature reported as working by the user must not automatically be marked `DEVELOPER TESTED` or `VERIFIED`.
3. Preserve all reported issues between development sessions.
4. Update statuses when new evidence becomes available.
5. Record unresolved issues and blockers explicitly.
6. At the beginning of every new session, review this section before starting new work.

### 23.12. Priority Order for the Current Development Iteration

Execute tasks in the following priority order:

1. Fix JSON import using the actual existing fixture from the `main` branch.
2. Investigate the APK signing mismatch and identify the cause.
3. Fix ingredient reordering in bulk ingredient input.
4. Redesign the theme selector into a compact dropdown with two themes per row.
5. Verify ZIP and JSON import/export, including regression testing with the original JSON fixture.
6. Implement and verify practical shopping list sharing for WhatsApp and Telegram.
7. Verify APK versioning, signing, update compatibility, and data preservation.
8. Run regression tests for all previously implemented features.
9. Update `DEVELOPMENT_HANDOFF.md` with all results, pending tests, blockers, and next actions.
10. Commit and push completed changes to the existing GitHub repository.

Do not stop the entire development process because one task is blocked. Continue all independent tasks.

### 23.13. General Rules for User-Reported Changes

1. Treat the user's manual testing feedback as authoritative information about observed behavior, but independently reproduce and verify issues where possible.
2. Do not break features that the user has already confirmed are working.
3. Make targeted fixes instead of unnecessary rewrites.
4. Preserve existing user data and backward compatibility.
5. Add regression tests for every fixed bug where technically possible.
6. Update `DEVELOPMENT_HANDOFF.md` after meaningful changes.
7. Record all pending tests and continue independent work without waiting unnecessarily.
8. Never report a feature as fully verified if the required tests have not actually been completed.
9. If an essential external input is missing, document the blocker and continue with other tasks.
10. After completing this iteration, commit and push all safe, completed changes to the existing repository.
