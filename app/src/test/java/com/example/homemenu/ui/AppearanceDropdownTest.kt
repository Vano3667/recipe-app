package com.example.homemenu.ui

import android.app.Application
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onChildren
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.homemenu.ui.settings.AppearanceContent
import com.example.homemenu.ui.settings.AppearanceUi
import com.example.homemenu.ui.theme.AppThemeId
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** Task 7 / 23.3: compact theme dropdown with two themes per row. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], application = Application::class)
class AppearanceDropdownTest {
    @get:Rule val rule = createComposeRule()

    private fun show(state: AppearanceUi = AppearanceUi(), onSelect: (AppThemeId) -> Unit = {}) {
        rule.setContent {
            MaterialTheme {
                AppearanceContent(
                    state = state, onSelect = onSelect, onColor = { _, _ -> }, onBase = {},
                    onResetCustom = {}, onResetDefault = {},
                )
            }
        }
    }

    @Test fun themesAreHiddenUntilDropdownOpens() {
        show()
        rule.onNodeWithTag("theme_dropdown").assertExists()
        rule.onNodeWithTag("theme_grid").assertDoesNotExist()
        rule.onNodeWithTag("theme_card_WARM_CREAM").assertDoesNotExist()
    }

    @Test fun gridShowsTwoThemesPerRow() {
        show()
        rule.onNodeWithTag("theme_dropdown").performClick()
        val rows = (AppThemeId.entries.size + 1) / 2
        for (r in 0 until rows) {
            val expected = minOf(2, AppThemeId.entries.size - r * 2)
            rule.onNodeWithTag("theme_row_$r").onChildren().assertCountEquals(expected)
        }
        rule.onNodeWithTag("theme_row_$rows").assertDoesNotExist()
    }

    @Test fun choosingThemeAppliesItAndClosesGrid() {
        var chosen: AppThemeId? = null
        show(onSelect = { chosen = it })
        rule.onNodeWithTag("theme_dropdown").performClick()
        rule.onNodeWithTag("theme_card_WARM_CREAM").performClick()
        assertThat(chosen).isEqualTo(AppThemeId.WARM_CREAM)
        rule.onNodeWithTag("theme_grid").assertDoesNotExist()
    }

    @Test fun dropdownTogglesClosedAgain() {
        show(AppearanceUi(themeId = AppThemeId.MODERN_DARK))
        rule.onNodeWithTag("theme_dropdown").performClick()
        rule.onNodeWithTag("theme_grid").assertExists()
        rule.onNodeWithTag("theme_dropdown").performClick()
        rule.onNodeWithTag("theme_grid").assertDoesNotExist()
    }
}
