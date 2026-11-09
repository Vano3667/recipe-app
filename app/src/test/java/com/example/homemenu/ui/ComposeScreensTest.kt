package com.example.homemenu.ui

import android.app.Application
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.homemenu.F
import com.example.homemenu.domain.PlannedDish
import com.example.homemenu.domain.ScalingUseCase
import com.example.homemenu.domain.ShoppingListUseCase
import com.example.homemenu.domain.model.MealSlot
import com.example.homemenu.domain.model.MenuEntry
import com.example.homemenu.domain.model.ThemeMode
import com.example.homemenu.ui.menu.DateRange
import com.example.homemenu.ui.menu.MenuContent
import com.example.homemenu.ui.menu.MenuUiState
import com.example.homemenu.ui.recipes.RecipeDetailContent
import com.example.homemenu.ui.settings.SettingsContent
import com.example.homemenu.ui.shopping.ShoppingContent
import com.example.homemenu.ui.shopping.ShoppingUiState
import com.example.homemenu.ui.theme.HomeMenuTheme
import com.example.homemenu.ui.theme.isAppDark
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.time.LocalDate

/** JVM (Robolectric) UI tests of the stateless screen contents. Run with `./gradlew testDebugUnitTest`. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], application = Application::class)
class ComposeScreensTest {
    @get:Rule val rule = createComposeRule()
    private val scaling = ScalingUseCase()
    private val shopping = ShoppingListUseCase(scaling)
    private val monday = LocalDate.of(2026, 11, 9)

    @Test fun recipeScalerUpdatesAmounts() {
        rule.setContent {
            HomeMenuTheme(ThemeMode.LIGHT) {
                var servings by remember { mutableStateOf(F.pancakes.servings) }
                RecipeDetailContent(
                    recipe = F.pancakes, servings = servings, onServingsChange = { servings = it },
                    scaled = scaling.scaleAll(F.pancakes.ingredients, F.pancakes.servings, servings),
                    photo = { null }, onBack = {}, onEdit = {}, onDelete = {}, onShareText = {}, onShareFile = {},
                )
            }
        }
        rule.onNodeWithTag("ingredient_amount_0").assertTextEquals("200 г")
        rule.onNodeWithTag("servings_minus").performSemanticsAction(SemanticsActions.OnClick)
        rule.waitForIdle()
        rule.onNodeWithTag("servings_minus").performSemanticsAction(SemanticsActions.OnClick)
        rule.waitForIdle()
        rule.onNodeWithTag("ingredient_amount_0").assertTextEquals("100 г")
        rule.onNodeWithTag("ingredient_amount_3").assertTextEquals("~1 ст. л.")
    }

    @Test fun shoppingListShowsAggregatedItemsAndToggles() {
        val groups = shopping.build(listOf(PlannedDish(F.pancakes, 4), PlannedDish(F.omelette, 2)))
        val toggled = mutableListOf<String>()
        rule.setContent {
            HomeMenuTheme(ThemeMode.LIGHT) {
                ShoppingContent(
                    state = ShoppingUiState(loading = false, range = DateRange.weekOf(monday), groups = groups, dishes = 2),
                    onMode = {}, onPrev = {}, onNext = {}, onRange = { _, _ -> }, onToggle = { toggled += it },
                    onClear = {}, onShare = {}, onOpenMenu = {},
                )
            }
        }
        rule.onNodeWithTag("shopping_amount_Молоко", useUnmergedTree = true).assertTextEquals("600 мл")
        rule.onNodeWithTag("shopping_item_Молоко").performSemanticsAction(SemanticsActions.OnClick)
        assertThat(toggled).containsExactly("2|ML")
        rule.onNodeWithText("Молочные продукты", substring = true).assertIsDisplayed()
    }

    @Test fun emptyShoppingListOffersMenu() {
        var opened = false
        rule.setContent {
            HomeMenuTheme(ThemeMode.LIGHT) {
                ShoppingContent(
                    state = ShoppingUiState(loading = false, range = DateRange.weekOf(monday)),
                    onMode = {}, onPrev = {}, onNext = {}, onRange = { _, _ -> }, onToggle = {},
                    onClear = {}, onShare = {}, onOpenMenu = { opened = true },
                )
            }
        }
        rule.onNodeWithText("Открыть меню").performSemanticsAction(SemanticsActions.OnClick)
        assertThat(opened).isTrue()
    }

    @Test fun menuCookButtonPassesPlannedServings() {
        var cooked: Pair<Long, Int>? = null
        rule.setContent {
            HomeMenuTheme(ThemeMode.LIGHT) {
                MenuContent(
                    state = MenuUiState(
                        loading = false, range = DateRange.weekOf(monday),
                        entries = listOf(MenuEntry(11, monday, MealSlot.BREAKFAST, F.pancakes.id, 3)),
                        recipes = F.all.associateBy { it.id },
                    ),
                    snackbar = remember { SnackbarHostState() },
                    onPrev = {}, onNext = {}, onThisWeek = {}, onRange = { _, _ -> },
                    onSetSlot = { _, _, _, _ -> }, onServings = { _, _ -> }, onClear = {}, onMove = { _, _, _ -> },
                    onGenerate = {}, onCook = { id, s -> cooked = id to s }, onAddRecipe = {},
                )
            }
        }
        rule.onNodeWithTag("cook_11").performClick()
        assertThat(cooked).isEqualTo(F.pancakes.id to 3)
    }

    @Test fun settingsRenderInDarkThemeAndSwitchTheme() {
        var chosen: ThemeMode? = null
        rule.setContent {
            HomeMenuTheme(ThemeMode.DARK) {
                assertThat(isAppDark()).isTrue()
                SettingsContent(theme = ThemeMode.DARK, busy = false, onTheme = { chosen = it }, onExport = {}, onImport = {})
            }
        }
        rule.onNodeWithTag("settings_screen").assertIsDisplayed()
        rule.onNodeWithTag("theme_LIGHT").performClick()
        assertThat(chosen).isEqualTo(ThemeMode.LIGHT)
    }

    @Config(qualifiers = "w1280dp-h800dp-land")
    @Test fun tabletRecipeDetailUsesTwoColumns() {
        rule.setContent {
            HomeMenuTheme(ThemeMode.LIGHT) {
                RecipeDetailContent(
                    recipe = F.chickenRice, servings = 2, onServingsChange = {},
                    scaled = scaling.scaleAll(F.chickenRice.ingredients, 2, 2),
                    photo = { null }, onBack = null, onEdit = {}, onDelete = {}, onShareText = {}, onShareFile = {},
                )
            }
        }
        rule.onNodeWithTag("detail_two_columns").assertExists()
    }
}
