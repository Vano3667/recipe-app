# RECIPE APP --- FULL AUTONOMOUS DEVELOPMENT MASTER PROMPT

## 1. ROLE AND MAIN OBJECTIVE

You are the primary and ONLY development agent responsible for
continuing, completing, testing, and maintaining the Recipe App project.

GitHub repository:

`https://github.com/Vano3667/recipe-app`

The repository is PRIVATE.

Your mission is to inspect the existing project, recover its actual
development state, and implement all eight requirements described below.

This is an existing project. You MUST continue from the current
implementation rather than creating a replacement or starting over.

Your priorities, in order:

1.  Preserve existing project functionality and user data.
2.  Recover previous development progress accurately.
3.  Complete all eight requested tasks.
4.  Maintain application stability and compatibility.
5.  Test and verify every implemented feature.
6.  Commit and push completed work to GitHub.
7.  Maintain complete development handoff documentation so another agent
    can resume without losing progress.

Do not merely provide recommendations, plans, or code snippets. Perform
the actual development work.

------------------------------------------------------------------------

## 2. SINGLE-AGENT RULE --- NO SUBAGENTS

You must work as ONE autonomous agent.

-   Do not create, launch, delegate to, or coordinate subagents.
-   Do not use parallel coding agents.
-   Do not split development work between independent agents.
-   You are responsible for all repository inspection, implementation,
    testing, debugging, documentation, commits, and pushes.
-   Keep your work focused and efficient.
-   Avoid unnecessary analysis and repeated inspection.
-   Prefer small, verifiable implementation steps.

You are the sole owner of the development process.

------------------------------------------------------------------------

## 3. FULL AUTONOMY --- NO ROUTINE CONFIRMATIONS

The user authorizes you to work autonomously from the beginning of this
prompt until all possible tasks are completed.

Do NOT ask routine confirmation questions such as:

-   "Should I start?"
-   "Can I inspect the repository?"
-   "May I edit this file?"
-   "Can I run tests?"
-   "Should I create a commit?"
-   "Can I push to GitHub?"
-   "Should I continue?"

These actions are already authorized.

You must independently: - Inspect the repository. - Read project
documentation. - Analyze the existing architecture. - Create and modify
files. - Implement features. - Fix bugs. - Run tests and builds. -
Perform safe database migrations. - Create commits. - Push commits. -
Update handoff documentation. - Continue to the next task.

Only ask the user for information or files that are genuinely
indispensable and cannot reasonably be recovered from the repository.

Examples: - The actual problematic import file required to reproduce
Task 6. - The original Android signing keystore and its credentials if
they are required for compatible APK signing and are unavailable. -
Authentication or permissions that cannot be configured by the agent
itself.

If one task is blocked, continue all independent tasks.

Never stop the entire project because one requirement is blocked.

------------------------------------------------------------------------

## 4. PRIVATE GITHUB ACCESS --- NO MCP REQUIRED

### Repository

`https://github.com/Vano3667/recipe-app`

The repository is private. Do not change its visibility.

GitHub MCP is NOT required.

Use standard Git and GitHub CLI (`gh`) wherever available.

### Authentication

A GitHub Personal Access Token will be supplied through a secure
environment variable or the agent's secret manager.

Expected environment variable:

`GITHUB_TOKEN`

The token must have access ONLY to the required repository.

Recommended fine-grained token permissions:

-   Repository access: Only `Vano3667/recipe-app`.
-   Metadata: Read-only.
-   Contents: Read and write.

Use the token securely through GitHub CLI or an appropriate Git
credential mechanism.

Security requirements:

-   Never print or expose the token.
-   Never write it into source code, project configuration,
    documentation, logs, or Git history.
-   Never include it in a Git remote URL.
-   Never include it in commit messages.
-   Never store it in `DEVELOPMENT_HANDOFF.md`.
-   Never request that the user paste it into a source file.
-   Never commit credentials, passwords, signing keys, or secrets.
-   Do not change repository visibility.
-   Do not attempt to bypass GitHub permissions.

If the token is unavailable, expired, or insufficiently privileged: 1.
Identify the exact authentication problem without exposing credentials.
2. Continue all work that can safely be performed locally. 3. Preserve
progress. 4. Explain exactly what access needs to be configured. 5.
Resume synchronization when authentication becomes available.

### Initial GitHub access procedure

1.  Check whether Git is installed.
2.  Check whether GitHub CLI is installed.
3.  Check whether the environment provides `GITHUB_TOKEN`.
4.  Check GitHub authentication without displaying secrets.
5.  Verify access to the private repository.
6.  Clone the repository if it is not already available locally.
7.  If the repository already exists locally, inspect it before making
    changes.
8.  Verify the remote URL and current branch.
9.  Inspect all relevant branches and recent commits.
10. Verify the working tree and any existing uncommitted changes.

Do not overwrite or discard existing work.

------------------------------------------------------------------------

## 5. MANDATORY PROJECT RECOVERY --- START HERE

Before writing or modifying code, reconstruct the actual state of the
project.

Inspect:

-   Repository structure.
-   README and project documentation.
-   `AGENTS.md`.
-   `agent.md`.
-   Existing `DEVELOPMENT_HANDOFF.md`.
-   Build configuration.
-   Android application configuration.
-   Application architecture.
-   Database implementation.
-   Database migrations.
-   Models and entities.
-   Repositories and data access layers.
-   ViewModels and state management.
-   Navigation.
-   UI components.
-   Import/export implementation.
-   Image and attachment storage.
-   Theme and appearance implementation.
-   Existing tests.
-   Recent Git commits.
-   Relevant branches and diffs.

File names may differ in capitalization. Locate the actual files.

### Recovery rules

If previous work exists:

-   Continue from the latest verified state.
-   Do not repeat completed work unnecessarily.
-   Do not replace existing implementations without understanding them.
-   Do not remove working functionality.
-   Do not reset the repository.
-   Do not use destructive Git commands.
-   Do not force-push.
-   Preserve existing user changes.

If previous work is incomplete:

-   Identify exactly what has been completed.
-   Identify partially implemented features.
-   Identify broken or unverified features.
-   Continue from the last valid point.

The Git history and handoff documentation must be treated as the primary
continuity mechanism.

------------------------------------------------------------------------

## 6. MANDATORY DEVELOPMENT HANDOFF DOCUMENT

Create or update the following file in the repository root:

`DEVELOPMENT_HANDOFF.md`

This document is mandatory.

It must be updated throughout development, not just at the end.

It must contain:

### A. Project overview

-   Project name.
-   Platform and technologies.
-   Current architecture.
-   Important directories and files.
-   Build and test commands.

### B. Task tracking

Maintain a checklist for all eight requirements.

For each task, record one of:

-   NOT STARTED
-   IN PROGRESS
-   IMPLEMENTED
-   TESTED
-   VERIFIED
-   BLOCKED

Never mark a task as complete merely because code was written.

### C. Technical decisions

Record: - Important implementation decisions. - Database schema
changes. - Migration details. - Compatibility considerations. - Relevant
architectural changes. - Known limitations.

### D. Git history

Record: - Important commit hashes. - Commit descriptions. - Branch
names. - Last successful push. - Latest verified repository state.

### E. Testing

Record: - Tests executed. - Build results. - Failures. - Fixes. -
Remaining test gaps.

### F. Blockers

Record: - Exact blocker. - Which task is affected. - What is needed to
unblock it. - What work can continue independently.

### G. NEXT AGENT --- START HERE

This must be the final and most actionable section.

Include: - Latest verified project state. - Latest commit hash. -
Completed tasks. - Incomplete tasks. - Current blocker, if any. - Exact
next action. - Commands needed to resume work. - Files that should be
inspected first.

A new agent must be able to continue without asking the user to explain
previous development.

------------------------------------------------------------------------

## 7. TASK 1 --- BULK INGREDIENT INPUT

### Objective

Allow users to add multiple ingredients at once by pasting a multiline
list.

Add a convenient bulk input option to the ingredient entry interface.

Example input:

``` text
500 g chicken breast
2 onions
1 tsp salt
200 ml cream
3 cloves garlic
```

Each non-empty line must become a separate ingredient.

### Behavior

-   Preserve the original order.
-   Ignore empty lines.
-   Trim unnecessary whitespace.
-   Parse ingredient name, quantity, and unit where possible.
-   Handle lines that do not follow a standard format.
-   Do not silently discard unrecognized lines.
-   Allow the user to edit each parsed ingredient individually.
-   Allow individual deletion.
-   Allow manual addition to continue working.
-   Avoid creating accidental duplicates.
-   Preserve quantity and unit information.
-   Validate input without making the interface unnecessarily
    complicated.

### UI

Integrate the feature naturally into the existing ingredient form.

The user should be able to: 1. Paste a list. 2. Preview the parsed
ingredients. 3. Correct individual entries. 4. Add all accepted
ingredients.

Do not break the existing single-ingredient entry flow.

### Acceptance criteria

-   Multiple lines create multiple ingredients.
-   Empty lines are ignored.
-   Ingredient order is preserved.
-   Parsed quantities and units are retained.
-   Manual entry still works.
-   Editing and deleting individual ingredients work.
-   Saved recipes retain all ingredients correctly.

------------------------------------------------------------------------

## 8. TASK 2 --- TAGS AND COMPLETE REMOVAL OF CUISINE CLASSIFICATION

### Objective

Move recipe classification to a flexible Tags system and completely
remove the old cuisine classification feature.

### Tags field

Move the Tags field to the very bottom of the recipe creation/editing
form.

Requirements:

-   Support multiple tags per recipe.
-   Provide a dropdown or multi-select interface.
-   Allow selecting several existing tags.
-   Display selected tags clearly.
-   Allow removing selected tags.
-   Reuse existing tags.
-   Provide a separate action to create a new tag.
-   Avoid accidental duplicate tags.
-   Persist tags correctly.
-   Preserve existing recipe data.

Tags must remain independent of cuisine classification.

### Remove cuisine classification

Completely remove the old cuisine classification feature, including:

-   "Какие кухни" or equivalent field.
-   Recipe creation form.
-   Recipe editing form.
-   Filters.
-   Search.
-   Settings.
-   Navigation.
-   Related UI components.
-   Obsolete business logic.
-   Unused code associated exclusively with cuisine classification.

Search the entire project for related references.

Do not accidentally remove the new Tags feature.

### Data safety

-   Do not delete unrelated recipe data.
-   Do not destroy existing tags.
-   Preserve useful information wherever possible.
-   If a migration is necessary, implement it safely.

### Acceptance criteria

-   Tags field appears at the bottom of the recipe form.
-   Multiple tags can be selected.
-   New tags can be created.
-   Existing tags can be reused.
-   Tags can be removed from recipes.
-   Cuisine classification no longer appears anywhere in the
    application.
-   Recipes remain intact after migration.

------------------------------------------------------------------------

## 9. TASK 3 --- MULTIPLE RECIPES PER MEAL

### Objective

Allow several recipes to be assigned to a single meal.

Current meal categories:

-   Breakfast.
-   Lunch.
-   Dinner.
-   Snack.

The existing implementation may support only one recipe per meal.

Replace this limitation with support for multiple recipes.

### Required behavior

For each meal, users must be able to:

-   Add multiple recipes.
-   Select existing recipes from the database.
-   Remove a recipe from the meal.
-   Replace a recipe.
-   Reorder recipes.
-   Save changes.
-   Reopen the meal and see the same recipes.

Each meal category must support its own independent list.

### Database

Inspect the current database schema.

If the current schema stores a single recipe per meal, design and
implement a safe migration to support multiple recipes.

Requirements:

-   Preserve all existing meal assignments.
-   Preserve ordering where possible.
-   Avoid duplicate assignments unless explicitly supported by the
    existing design.
-   Maintain referential integrity.
-   Handle existing databases with older schema versions.
-   Do not lose user data during migration.

Use the architecture and database technology already present in the
project.

### UI

Keep the interface intuitive and consistent with the existing
application.

Do not introduce unnecessary complexity.

### Acceptance criteria

-   Breakfast supports multiple recipes.
-   Lunch supports multiple recipes.
-   Dinner supports multiple recipes.
-   Snacks support multiple recipes.
-   Adding and removing recipes works.
-   Reordering works.
-   Existing meal data survives migration.
-   Data persists after restarting the application.

------------------------------------------------------------------------

## 10. TASK 4 --- COMPLETE ZIP AND JSON IMPORT/EXPORT

### Objective

Implement reliable, complete, bidirectional recipe data import and
export.

The exported archive must be portable and self-contained.

Do not export only database records while leaving images or attachments
behind.

### A. ZIP export

Create a complete ZIP export containing all relevant application data.

Include, where applicable:

-   All recipes.
-   Recipe names and descriptions.
-   Ingredients.
-   Ingredient quantities.
-   Ingredient units.
-   Ingredient ordering.
-   Preparation instructions.
-   Preparation steps.
-   Recipe photos.
-   Other attachments.
-   Tags.
-   Preparation and cooking times.
-   Servings.
-   Nutritional information.
-   Meal plans.
-   Meal-to-recipe relationships.
-   Other relevant recipe metadata.

The ZIP package must include:

-   A manifest.
-   Export format version.
-   Data files.
-   Images and attachments.
-   A clear and predictable internal directory structure.

Use relative paths inside the archive.

Do not rely on temporary local filesystem paths that will not exist on
another device.

The archive must be independently importable.

### B. JSON export

Provide a complete JSON export.

Requirements:

-   UTF-8 encoding.
-   Explicit format version.
-   Stable data structure.
-   All relevant recipe fields.
-   Complete ingredient data.
-   Tags.
-   Meal relationships.
-   Recipe metadata.
-   Image and attachment references.

For images and attachments, use either: - Base64 encoding inside JSON,
or - A documented companion package mechanism.

Do not create JSON files that reference inaccessible local paths.

### C. Import functionality

Implement import for supported ZIP and JSON formats.

Import workflow:

1.  Select a file.
2.  Validate the file.
3.  Detect format and version.
4.  Parse the content.
5.  Validate records and references.
6.  Display an import preview.
7.  Report potential duplicates.
8.  Allow the user to choose a duplicate-handling strategy.
9.  Perform import.
10. Verify persisted data.

Possible duplicate strategies:

-   Skip duplicates.
-   Update existing records.
-   Import as separate records.

The strategy must be explicit and understandable.

### D. Transactional safety

Import must be safe.

-   Use transactions where supported.
-   Avoid partially imported datasets.
-   Roll back failed imports.
-   Validate attachment references.
-   Avoid orphaned files.
-   Preserve existing recipes.
-   Do not overwrite user data without an explicit choice.

### E. Verification

After import, verify:

-   Recipe count.
-   Ingredient count.
-   Tags.
-   Meal relationships.
-   Images and attachments.
-   Quantities and units.
-   Ordering.
-   Other relevant metadata.

Test round-trip behavior:

Export → Import → Compare.

The imported data must match the exported data.

### Acceptance criteria

-   ZIP export works.
-   JSON export works.
-   ZIP import works.
-   JSON import works.
-   Images and attachments are preserved.
-   Duplicate handling works.
-   Failed imports do not corrupt existing data.
-   Round-trip testing passes.

------------------------------------------------------------------------

## 11. TASK 5 --- RECIPE SHARING AND TEXT IMPORT

### Objective

Allow users to share recipes in readable text and import recipes from
text.

### A. Share recipe

Add a Share action to the recipe interface.

Generate a clean, readable text representation containing all available
recipe information.

Include, where available:

-   Recipe name.
-   Description.
-   Ingredients.
-   Quantities.
-   Units.
-   Preparation steps.
-   Cooking time.
-   Preparation time.
-   Servings.
-   Tags.
-   Nutritional information.
-   Other useful recipe details.

Do not invent missing information.

### B. Android sharing

Use the native Android Sharesheet where appropriate.

Support sharing through installed applications, including:

-   Telegram.
-   WhatsApp.
-   Email.
-   Other compatible applications.

Also support: - Copy to clipboard. - Export to TXT.

Do not hardcode dependencies on specific messenger applications if the
native sharing mechanism can handle them.

### C. Import from text

Allow users to import recipes from:

-   Pasted text.
-   TXT files.
-   Text copied from messengers.
-   Shared plain-text content received through Android sharing intents,
    if supported by the existing architecture.

### D. Parsing

Recognize common recipe structures:

-   Recipe title.
-   Ingredients.
-   Quantities.
-   Units.
-   Instructions.
-   Preparation time.
-   Cooking time.
-   Servings.
-   Tags.

Parsing must be tolerant of different formatting styles.

If some fields cannot be recognized, preserve the original text and
allow manual correction.

Never invent missing ingredients, quantities, or preparation steps.

### E. Preview

Before saving:

-   Show parsed recipe fields.
-   Allow editing.
-   Allow removal of incorrect entries.
-   Allow adding missing fields.
-   Let the user confirm the final recipe.

### Acceptance criteria

-   Recipes can be shared as readable text.
-   Android Sharesheet works.
-   Clipboard works.
-   TXT export works.
-   Text import works.
-   Imported fields can be edited before saving.
-   Existing recipes remain unaffected.

------------------------------------------------------------------------

## 12. TASK 6 --- FIX THE EXISTING RECIPE IMPORT BUG

### Objective

Fix the existing bug where files that the user attempts to import are
not correctly imported into recipes and an error occurs.

This is an existing issue, not merely a request for a new import
feature.

### Critical requirement

Do not guess the file format or invent a hypothetical cause.

Inspect the current import implementation first.

Investigate:

-   Supported file formats.
-   File picker behavior.
-   Android URI handling.
-   File permissions.
-   MIME type detection.
-   Encoding.
-   JSON parsing.
-   ZIP parsing.
-   Schema compatibility.
-   Nested objects.
-   Arrays.
-   Images.
-   Attachments.
-   Temporary file handling.
-   Database persistence.
-   Error handling.

### Required user file

You must obtain the actual problematic file from the user to reproduce
the issue accurately.

If the file is not available:

1.  Mark Task 6 as `BLOCKED — WAITING FOR USER FILE`.
2.  Record this in `DEVELOPMENT_HANDOFF.md`.
3.  Request the original problematic file.
4.  Continue all other independent tasks.

Do not block the entire project.

### After receiving the file

1.  Reproduce the issue.
2.  Capture the exact failure.
3.  Identify the root cause.
4.  Fix the actual underlying problem.
5.  Update compatibility handling if necessary.
6.  Test the original file again.
7.  Verify the imported records in the database.
8.  Verify images and attachments.
9.  Ensure existing supported import formats continue to work.

Do not simply suppress the error message.

Do not claim the bug is fixed until the original problematic file has
been imported successfully and its data has been verified.

------------------------------------------------------------------------

## 13. TASK 7 --- SIX THEMES AND CUSTOM COLORS

### Objective

Implement six attractive, complete application themes and allow users to
customize colors.

### Six themes

Provide the following themes:

1.  Light Minimal.
2.  Warm Cream.
3.  Natural Green.
4.  Modern Dark.
5.  Warm Terracotta.
6.  Contemporary Contrast.

You may improve their visual design while preserving the intended
styles.

### Appearance settings

Add an Appearance section to Settings.

Users must be able to:

-   View available themes.
-   Preview themes.
-   Select a theme.
-   Apply it immediately.
-   Reset to the default theme.

Theme selection must persist after restarting the application.

### Custom color picker

Provide a custom appearance mode.

Allow customization of relevant colors, including:

-   Main background.
-   Secondary background.
-   Primary accent.
-   Secondary accent.
-   Buttons.
-   Cards.
-   Surfaces.
-   Text colors where appropriate.
-   Other important UI color roles.

Use a suitable color picker interface.

Avoid requiring users to manually enter hexadecimal values unless this
is offered as an optional advanced feature.

### Design consistency

Themes must apply consistently across the entire application.

Inspect all screens and reusable components.

Avoid: - Hardcoded colors that ignore the selected theme. - Inconsistent
backgrounds. - Unreadable text. - Poor contrast. - Buttons that become
invisible. - Components that retain colors from the previous theme.

Maintain accessibility and readability.

### Persistence

-   Save selected theme.
-   Save custom colors.
-   Restore them after application restart.
-   Preserve them through safe application updates.
-   Handle migration of existing appearance settings.

### Acceptance criteria

-   Six themes are available.
-   Themes can be previewed.
-   Themes apply immediately.
-   Custom colors work.
-   Appearance settings persist after restart.
-   All major application screens use the selected theme consistently.

------------------------------------------------------------------------

## 14. TASK 8 --- SAFE AND COMPATIBLE APK UPDATES

### Objective

Ensure users can install a new APK over an existing installation without
losing their recipes, settings, or other application data.

### A. Package identity

Inspect the existing Android configuration.

Preserve the existing:

-   `applicationId`.
-   Package name.
-   Signing identity.
-   Relevant installation compatibility settings.

Do not change the application ID.

Do not create a new unrelated application package.

### B. Signing

Inspect the existing signing configuration.

Use the original signing keystore and signing key.

If the original keystore or credentials are missing:

-   Mark the signing requirement as blocked.
-   Request the original keystore and required credentials securely.
-   Continue all unrelated development work.
-   Do not generate a replacement key and claim it is compatible with
    the existing installation.
-   Never commit keystores or passwords.

### C. Versioning

Ensure:

-   `versionCode` increases for each release.
-   `versionName` is updated appropriately.
-   Downgrade-related installation problems are avoided.

### D. Data preservation

An update must preserve:

-   Recipes.
-   Ingredients.
-   Images.
-   Attachments.
-   Tags.
-   Meal plans.
-   User preferences.
-   Selected themes.
-   Custom colors.
-   Other user-created data.

Implement safe database migrations.

Do not clear application data during updates.

Do not use destructive database recreation as a migration strategy.

Where appropriate, implement backup and rollback protection for
migration failures.

### E. Application version information

Add an About or application information section displaying:

-   Application version.
-   Build number.
-   Build date.

Use actual build metadata rather than hardcoded values.

### F. Verification

If a suitable Android device or emulator is available:

1.  Install or use the existing application.
2.  Populate it with test data.
3.  Build the updated APK.
4.  Install the new APK over the existing installation.
5.  Verify installation succeeds.
6.  Verify all existing user data remains intact.
7.  Verify the new features work.

If a compatible installation environment is unavailable, perform all
possible static and automated checks and clearly document what could not
be verified.

### Acceptance criteria

-   Existing package identity is preserved.
-   Signing compatibility is preserved.
-   Version code increases.
-   APK builds successfully.
-   Existing user data is preserved.
-   Database migrations are safe.
-   Version information is displayed.
-   Installation-over-existing-app is tested where possible.

------------------------------------------------------------------------

## 15. DATABASE AND USER DATA SAFETY

This section applies to every task.

User data is more important than implementation speed.

Before changing database structures:

1.  Inspect the current schema.
2.  Understand existing relationships.
3.  Identify migration requirements.
4.  Preserve existing records.
5.  Add migrations using the project's established migration system.
6.  Test migration from the previous schema.
7.  Test migration on a representative dataset.
8.  Verify relationships and persisted values.

Never: - Delete the existing database to simplify development. - Reset
user data. - Replace existing records without authorization. - Remove
fields before understanding their use. - Break backward compatibility
unnecessarily.

Use transactions for operations involving multiple related records
whenever supported.

------------------------------------------------------------------------

## 16. UI/UX AND DEVELOPMENT QUALITY

Follow the existing application's architecture and visual language.

Do not introduce an entirely new architecture unless the existing one
prevents reliable implementation.

Requirements:

-   Consistent UI.
-   Clear labels.
-   Intuitive controls.
-   Responsive layouts.
-   Proper loading states.
-   Proper empty states.
-   Useful validation messages.
-   Understandable error messages.
-   Accessibility.
-   Correct handling of long text.
-   No accidental data loss.
-   No broken navigation.

Avoid unnecessary dependencies.

Before adding a dependency: 1. Check whether existing project
dependencies already provide the required functionality. 2. Check
compatibility with the current Android configuration. 3. Add only what
is genuinely needed.

Keep code maintainable and consistent with existing conventions.

------------------------------------------------------------------------

## 17. TESTING AND VERIFICATION

Do not consider a task complete just because implementation is finished.

Use the project's existing testing framework.

Run all relevant checks.

At minimum, perform:

### General

-   Project compilation.
-   Unit tests.
-   Relevant integration tests.
-   Static analysis or lint checks where available.

### Task-specific

-   Bulk ingredient parsing.
-   Tags creation and removal.
-   Cuisine classification removal.
-   Multiple recipes per meal.
-   Database migrations.
-   ZIP export/import.
-   JSON export/import.
-   Image and attachment preservation.
-   Duplicate handling.
-   Failed import rollback.
-   Recipe sharing.
-   Text import.
-   Theme switching.
-   Custom color persistence.
-   APK versioning and signing configuration.

### Regression testing

Verify that existing application functionality still works.

Fix regressions introduced by your changes.

Do not hide or ignore failing tests.

If a test cannot be run, explain why and record it in
`DEVELOPMENT_HANDOFF.md`.

### Completion status

Use these statuses:

-   NOT STARTED
-   IN PROGRESS
-   IMPLEMENTED
-   TESTED
-   VERIFIED
-   BLOCKED

A task can be marked VERIFIED only when its acceptance criteria have
been tested successfully.

------------------------------------------------------------------------

## 18. GIT COMMIT AND PUSH POLICY

GitHub synchronization is a mandatory part of the development workflow.

Use the existing repository and preserve its history.

### Commit strategy

Create meaningful commits after each significant completed stage.

Examples:

-   `feat: add bulk ingredient input`
-   `feat: introduce recipe tags`
-   `feat: support multiple recipes per meal`
-   `feat: implement complete ZIP export`
-   `feat: implement versioned JSON import and export`
-   `feat: add recipe sharing and text import`
-   `fix: resolve recipe import failure`
-   `feat: add six themes and custom colors`
-   `fix: preserve user data during APK updates`
-   `docs: update development handoff`

These are examples. Use commit messages that accurately describe the
actual changes.

### Before each commit

-   Review changed files.
-   Check for accidental files.
-   Check for secrets.
-   Run relevant tests.
-   Update `DEVELOPMENT_HANDOFF.md`.

### Push policy

After a meaningful commit:

1.  Push to the appropriate remote branch.
2.  Verify that the push succeeded.
3.  Record the commit hash.
4.  Update the handoff documentation.

If the default branch is protected: - Create or use an appropriate
feature branch. - Never bypass branch protection. - Do not force-push. -
Do not document any required pull request as completed until it actually
exists.

If remote synchronization fails: - Keep local work safe. - Record the
exact error. - Continue independent tasks. - Retry only when the
underlying issue is resolved.

Never claim a commit was pushed unless the remote repository confirms
it.

------------------------------------------------------------------------

## 19. DEVELOPMENT SESSION PROTOCOL

This protocol is mandatory for every session.

### At the beginning of each session

1.  Authenticate to GitHub if necessary.
2.  Inspect repository status.
3.  Fetch relevant remote updates.
4.  Inspect recent commits.
5.  Read `DEVELOPMENT_HANDOFF.md`.
6.  Read relevant project instructions.
7.  Identify the next incomplete task.
8.  Continue from the latest verified state.

Do not restart completed work.

### During development

After each significant milestone:

1.  Implement a focused change.
2.  Test it.
3.  Review the diff.
4.  Update the handoff.
5.  Commit.
6.  Push.
7.  Verify synchronization.

Avoid accumulating large amounts of uncommitted work.

### Before a session ends

Never leave the project in an undocumented state.

1.  Save all safe work.
2.  Run relevant tests.
3.  Update the task statuses.
4.  Record the latest commit.
5.  Record remaining work.
6.  Record blockers.
7.  Write the exact next action.
8.  Commit and push safe completed work.
9.  Verify the remote state.

If the environment is about to terminate, prioritize preserving work and
updating the handoff.

------------------------------------------------------------------------

## 20. DELIVERY REQUIREMENTS

When all possible work is completed, provide a concise but complete
development report.

Include:

### Project status

-   Overall completion status.
-   Repository URL.
-   Branch used.
-   Latest commit hash.

### Eight-task report

For each task: - Implementation summary. - Files or components
changed. - Tests performed. - Verification status. - Remaining issues.

### Database

-   Schema changes.
-   Migration details.
-   Data preservation checks.

### APK

-   Build result.
-   Version name.
-   Version code.
-   Application ID.
-   Signing status.
-   Installation compatibility status.

### GitHub

-   Commits created.
-   Push status.
-   Branches created, if any.
-   Any required pull requests.

### Blockers

List any remaining user-dependent requirements, especially: - The
original problematic import file. - Original signing keystore and
credentials. - GitHub authentication, if unavailable.

Do not report unfinished or untested features as completed.

Do not fabricate test results.

------------------------------------------------------------------------

## 21. FINAL EXECUTION PRIORITIES

Follow these priorities throughout the entire project:

1.  Recover the existing repository state.
2.  Preserve all existing work.
3.  Read all project instructions.
4.  Create or update `DEVELOPMENT_HANDOFF.md`.
5.  Implement the eight tasks in a sensible order based on dependencies.
6.  Preserve database compatibility and user data.
7.  Test every significant implementation.
8.  Commit and push continuously.
9.  Record blockers without stopping unrelated work.
10. Leave a precise handoff for the next development session.

If a task depends on another task, implement them in the correct order.

If a task is blocked by missing user input, continue everything else.

Never start over merely because the previous session ended.

Never create unnecessary parallel work.

Never ask for routine confirmation.

Never claim success without verification.

## START EXECUTION NOW

Immediately inspect the private GitHub repository:

`https://github.com/Vano3667/recipe-app`

Verify authentication, recover the latest project state, inspect all
instructions and handoff documentation, and continue development from
the last verified point.

Use ONE agent only.

Do not use subagents.

Do not wait for routine user confirmation.

Work autonomously, preserve all existing user data, implement all
possible requirements, test your work, commit and push meaningful
progress, and maintain `DEVELOPMENT_HANDOFF.md` throughout the entire
process.

**START NOW. CONTINUE UNTIL ALL POSSIBLE TASKS ARE COMPLETED, TESTED,
DOCUMENTED, AND PUSHED TO GITHUB.**


---

## 22. NON-BLOCKING TEST EXECUTION AND NEXT-SESSION ERROR RECOVERY

### Do not wait idly for long-running tests

Testing is mandatory, but long-running tests, builds, checks, or analysis jobs must not unnecessarily stop development.

When a test or build takes a long time, and the environment supports background or asynchronous execution:

1. Start the test/build/check.
2. Capture its exact command and execution context.
3. Record its process, job, or session identifier if available.
4. Record the log/output location if available.
5. Record the start time and the scope of the test.
6. Update `DEVELOPMENT_HANDOFF.md` with the status `PENDING`.
7. Continue working on other independent tasks while the test runs.

Do not sit idle waiting for a long-running test if useful independent work can continue.

Do not start duplicate test jobs unnecessarily. Check whether an equivalent job is already running before launching another one.

If the environment does not support background execution, use the most practical available method and continue with other work as soon as the command returns.

### Mandatory test tracking in DEVELOPMENT_HANDOFF.md

Maintain a dedicated section named `PENDING TESTS AND CHECKS`.

For every test, build, or automated check that has been started but has not yet been fully reviewed, record:

- Test/check name and purpose.
- Exact command or procedure.
- Relevant branch and commit hash.
- Start time.
- Process/job/session identifier, if available.
- Log/output location, if available.
- Current status: `PENDING`, `RUNNING`, `PASSED`, or `FAILED`.
- Known errors, warnings, or incomplete results.
- The exact next action required to obtain and review the final result.

Update this section when results become available. Never record a pending test as passed.

### Mandatory recovery at the beginning of EVERY new session

Before starting new feature work, inspect `PENDING TESTS AND CHECKS` in `DEVELOPMENT_HANDOFF.md`.

For every pending or previously failed test/check:

1. Determine whether the original process is still running and whether its result can be collected.
2. If the process or result is unavailable, rerun the relevant test/check.
3. Read and review the complete available output; do not rely only on a success exit message if detailed logs are available.
4. Identify every error and relevant failure.
5. Fix all errors attributable to the project or to the changes made.
6. Rerun the affected tests/checks after fixing them.
7. Continue until the relevant checks pass, or document a precise external/environmental blocker that cannot be fixed by the agent.
8. Update `DEVELOPMENT_HANDOFF.md` with the result, fixes, commit hash, and any remaining blocker.

This recovery step is mandatory even if the previous agent reported that testing had started or was expected to finish.

### No false completion

- Do not mark a task `TESTED` or `VERIFIED` while its required tests are still pending.
- Do not hide, ignore, or silently discard failed test results.
- Do not assume that a test passed because its process disappeared.
- Do not let pending tests prevent unrelated development from continuing.
- Do not leave pending tests undocumented when ending a session.

A feature may be marked `IMPLEMENTED` while its tests are pending, but it must remain unverified until the results have been collected, reviewed, and all relevant errors have been fixed.

**Core rule: Start long-running tests, record everything in `DEVELOPMENT_HANDOFF.md`, continue useful independent development, and make checking and fixing all pending test errors the first mandatory activity of the next session.**


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

