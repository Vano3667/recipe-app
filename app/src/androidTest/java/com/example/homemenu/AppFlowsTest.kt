package com.example.homemenu

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intending
import androidx.test.espresso.intent.matcher.IntentMatchers.isInternal
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.homemenu.ui.nav.TopDestination
import com.google.common.truth.Truth.assertThat
import org.hamcrest.CoreMatchers.not
import org.junit.Ignore
import org.junit.Test
import org.junit.Rule
import org.junit.runner.RunWith
import java.io.File

/**
 * End-to-end flows on a real emulator (GitHub Actions: API 29 phone + API 34 tablet).
 * The app seeds 15 demo recipes on first launch.
 */
@RunWith(AndroidJUnit4::class)
class AppFlowsTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    private fun tagPrefix(prefix: String) = SemanticsMatcher("testTag starts with $prefix") {
        it.config.getOrNull(SemanticsProperties.TestTag)?.startsWith(prefix) == true
    }

    /** On timeout the CI log names the missing element (ComposeTimeoutException alone doesn't). */
    private fun waitTag(tag: String, timeout: Long = 15_000) {
        try {
            rule.waitUntil(timeout) { rule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty() }
        } catch (e: Throwable) {
            throw AssertionError("Не появился элемент с тегом «$tag» за $timeout мс", e)
        }
    }

    /**
     * Waits until the element with [tag] is selected. A choice like the theme is first saved to the DB
     * in the background and only then reaches the screen, so waitForIdle() alone doesn't guarantee it.
     */
    private fun waitSelected(tag: String, timeout: Long = 15_000) {
        try {
            rule.waitUntil(timeout) {
                rule.onAllNodesWithTag(tag).fetchSemanticsNodes()
                    .any { it.config.getOrNull(SemanticsProperties.Selected) == true }
            }
        } catch (e: Throwable) {
            throw AssertionError("Элемент с тегом «$tag» не стал выбранным за $timeout мс", e)
        }
    }

    private fun go(dest: TopDestination) {
        waitTag("nav_${dest.name}")
        rule.onNodeWithTag("nav_${dest.name}").performClick()
        rule.waitForIdle()
    }

    private fun ensureMenuPlanned() {
        go(TopDestination.MENU)
        waitTag("generate_menu")
        rule.onNodeWithTag("generate_menu").performClick()
        waitTag("gen_confirm")
        rule.onNodeWithTag("gen_confirm").performClick()
        rule.waitUntil(15_000) { rule.onAllNodes(tagPrefix("cook_")).fetchSemanticsNodes().isNotEmpty() }
    }

    @Test fun darkThemeCanBeSelected() {
        go(TopDestination.SETTINGS)
        waitTag("theme_DARK")
        try {
            rule.onNodeWithTag("theme_DARK").performClick()
            waitSelected("theme_DARK")
            rule.onNodeWithTag("theme_DARK").assertIsSelected()
            // Screen still renders and navigation works in dark mode.
            go(TopDestination.RECIPES)
            waitTag("recipe_list")
        } finally {
            go(TopDestination.SETTINGS)
            waitTag("theme_SYSTEM")
            rule.onNodeWithTag("theme_SYSTEM").performClick()
            // Let the reset reach the DB before the activity closes; runCatching keeps the original failure visible.
            runCatching { waitSelected("theme_SYSTEM") }
        }
    }

    @Test fun menuToCookingOpensScaledRecipe() {
        ensureMenuPlanned()
        val cook = rule.onAllNodes(tagPrefix("cook_"))[0]
        runCatching { cook.performScrollTo() }
        cook.performClick()
        waitTag("recipe_detail")
        waitTag("servings_value")
    }

    @Test fun shoppingListIsBuiltFromMenu() {
        ensureMenuPlanned()
        go(TopDestination.SHOPPING)
        waitTag("mode_WEEK")
        rule.onNodeWithTag("mode_WEEK").performClick()
        waitTag("shopping_list")
        waitTag("shopping_progress_text")
        assertThat(rule.onAllNodes(tagPrefix("shopping_item_")).fetchSemanticsNodes()).isNotEmpty()
    }

    @Test fun addRecipeWithIngredient() {
        go(TopDestination.RECIPES)
        waitTag("add_recipe")
        rule.onNodeWithTag("add_recipe").performClick()
        waitTag("name_field")
        val name = "Тестовый салат ${System.currentTimeMillis() % 10_000}"
        rule.onNodeWithTag("name_field").performTextInput(name)
        rule.onNodeWithTag("servings_field").performTextClearance()
        rule.onNodeWithTag("servings_field").performTextInput("2")
        rule.onNodeWithTag("add_ingredient").performScrollTo().performClick()
        waitTag("product_search")
        rule.onNodeWithTag("product_search").performTextInput("Молоко")
        waitTag("product_Молоко")
        rule.onNodeWithTag("product_Молоко").performClick()
        waitTag("amount_field_0")
        rule.onNodeWithTag("amount_field_0").performScrollTo().performTextClearance()
        rule.onNodeWithTag("amount_field_0").performTextInput("200")
        rule.onNodeWithTag("save_recipe").performClick()
        // The editor closes and the saved recipe is shown: detail screen on a phone, list + detail pane on a tablet.
        try {
            rule.waitUntil(15_000) {
                rule.onAllNodesWithTag("name_field").fetchSemanticsNodes().isEmpty() &&
                    rule.onAllNodesWithText(name).fetchSemanticsNodes().isNotEmpty()
            }
        } catch (e: Throwable) {
            throw AssertionError("Рецепт «$name» не появился после сохранения", e)
        }
    }

    // TODO(фото): сценарий отключён вместе с добавлением фото. Снять @Ignore, когда будут выполнены шаги
    //  из TODO(фото) в RecipeEditScreen.kt (photoPicker, исправление PhotoStorage.importPhoto, разрешения).
    //  Заодно усилить последнюю проверку: onAllNodesWithText(name) находит название ещё в поле name_field,
    //  до сохранения, — лучше ждать закрытия редактора, как в addRecipeWithIngredient.
    @Ignore("TODO(фото): добавление фото временно отключено")
    @Test fun addRecipeWithPhotoFromGallery() {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        val img = File(ctx.cacheDir, "ui_test_photo.jpg")
        img.outputStream().use { out ->
            Bitmap.createBitmap(1600, 1200, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.rgb(212, 140, 140)) }
                .compress(Bitmap.CompressFormat.JPEG, 90, out)
        }
        val photosBefore = File(ctx.filesDir, "photos").listFiles()?.size ?: 0
        Intents.init()
        try {
            intending(not(isInternal())).respondWith(
                Instrumentation.ActivityResult(Activity.RESULT_OK, Intent().setData(Uri.fromFile(img))),
            )
            go(TopDestination.RECIPES)
            waitTag("add_recipe")
            rule.onNodeWithTag("add_recipe").performClick()
            waitTag("name_field")
            val name = "Тестовый салат ${System.currentTimeMillis() % 10_000}"
            rule.onNodeWithTag("name_field").performTextInput(name)
            rule.onNodeWithTag("servings_field").performTextClearance()
            rule.onNodeWithTag("servings_field").performTextInput("2")
            rule.onNodeWithTag("photo_gallery").performClick()
            rule.waitForIdle()
            rule.onNodeWithTag("add_ingredient").performScrollTo().performClick()
            waitTag("product_search")
            rule.onNodeWithTag("product_search").performTextInput("Молоко")
            waitTag("product_Молоко")
            rule.onNodeWithTag("product_Молоко").performClick()
            waitTag("amount_field_0")
            rule.onNodeWithTag("amount_field_0").performScrollTo().performTextClearance()
            rule.onNodeWithTag("amount_field_0").performTextInput("200")
            rule.onNodeWithTag("save_recipe").performClick()
            rule.waitUntil(15_000) { rule.onAllNodesWithText(name).fetchSemanticsNodes().isNotEmpty() }
            val photosAfter = File(ctx.filesDir, "photos").listFiles()?.size ?: 0
            assertThat(photosAfter).isGreaterThan(photosBefore)
        } finally {
            Intents.release()
        }
    }

    @Test fun layoutAdaptsToScreenWidth() {
        val widthDp = rule.activity.resources.configuration.screenWidthDp
        when {
            widthDp >= 840 -> {
                waitTag("nav_rail")
                waitTag("master_detail")
            }
            widthDp >= 600 -> waitTag("nav_rail")
            else -> waitTag("bottom_bar")
        }
    }
}
