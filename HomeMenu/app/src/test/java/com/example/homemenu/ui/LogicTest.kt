package com.example.homemenu.ui

import com.example.homemenu.F
import com.example.homemenu.data.files.PhotoStorage
import com.example.homemenu.domain.model.MealSlot
import com.example.homemenu.domain.model.MeasureUnit
import com.example.homemenu.domain.model.MenuEntry
import com.example.homemenu.ui.menu.DateRange
import com.example.homemenu.ui.menu.MenuUiState
import com.example.homemenu.ui.recipes.EditForm
import com.example.homemenu.ui.recipes.EditIngredient
import com.example.homemenu.ui.recipes.RecipeFormValidator
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test
import java.time.LocalDate

class RecipeFormValidatorTest {
    private val ok = EditForm(name = "Блины", servingsText = "4", ingredients = listOf(EditIngredient(F.milk, "500", MeasureUnit.ML)))

    @Test fun `valid form has no errors`() {
        assertThat(RecipeFormValidator.validate(ok).isEmpty).isTrue()
        assertThat(RecipeFormValidator.validate(ok.copy(timeText = "")).isEmpty).isTrue()
    }

    @Test fun `blank name is an error`() {
        assertThat(RecipeFormValidator.validate(ok.copy(name = "   ")).name).isNotNull()
    }

    @Test fun `servings must be positive integer`() {
        listOf("0", "-1", "abc", "", "2.5").forEach {
            assertThat(RecipeFormValidator.validate(ok.copy(servingsText = it)).servings).isNotNull()
        }
    }

    @Test fun `ingredient amount must be positive`() {
        listOf("0", "-5", "", "много").forEach {
            val f = ok.copy(ingredients = listOf(EditIngredient(F.milk, it, MeasureUnit.ML)))
            assertThat(RecipeFormValidator.validate(f).ingredients).isNotNull()
        }
    }

    @Test fun `comma decimal is accepted`() {
        assertThat(RecipeFormValidator.parseAmount("1,5")).isEqualTo(1.5)
        assertThat(RecipeFormValidator.parseAmount(" 0.25 ")).isEqualTo(0.25)
    }

    @Test fun `bad time is an error`() {
        assertThat(RecipeFormValidator.validate(ok.copy(timeText = "час")).time).isNotNull()
        assertThat(RecipeFormValidator.validate(ok.copy(timeText = "-3")).time).isNotNull()
    }

    @Test fun `very long name is still valid`() {
        assertThat(RecipeFormValidator.validate(ok.copy(name = "Очень ".repeat(200))).isEmpty).isTrue()
    }
}

class DateRangeTest {
    private val wed = LocalDate.of(2026, 11, 11)

    @Test fun `week starts on monday`() {
        val w = DateRange.weekOf(wed)
        assertThat(w.from).isEqualTo(LocalDate.of(2026, 11, 9))
        assertThat(w.to).isEqualTo(LocalDate.of(2026, 11, 15))
        assertThat(w.days).hasSize(7)
        assertThat(w.isCalendarWeek).isTrue()
    }

    @Test fun `shift moves by range length`() {
        val r = DateRange(wed, wed.plusDays(2))
        assertThat(r.shift(1)).isEqualTo(DateRange(wed.plusDays(3), wed.plusDays(5)))
        assertThat(r.shift(-1)).isEqualTo(DateRange(wed.minusDays(3), wed.minusDays(1)))
        assertThat(r.isCalendarWeek).isFalse()
    }

    @Test fun `single day range`() {
        assertThat(DateRange.day(wed).days).containsExactly(wed)
    }

    @Test fun `range across month and year`() {
        val r = DateRange(LocalDate.of(2026, 12, 30), LocalDate.of(2027, 1, 2))
        assertThat(r.length).isEqualTo(4)
    }

    @Test fun `inverted range is rejected`() {
        assertThrows(IllegalArgumentException::class.java) { DateRange(wed, wed.minusDays(1)) }
    }
}

class MenuSummaryTest {
    private val mon = LocalDate.of(2026, 11, 9)

    @Test fun `summary ignores deleted recipes and counts calories per serving`() {
        val state = MenuUiState(
            loading = false,
            range = DateRange(mon, mon.plusDays(1)),
            entries = listOf(
                MenuEntry(1, mon, MealSlot.BREAKFAST, F.pancakes.id, 2), // 250 * 2
                MenuEntry(2, mon, MealSlot.LUNCH, F.chickenRice.id, 1), // 520
                MenuEntry(3, mon.plusDays(1), MealSlot.LUNCH, 999, 3), // deleted recipe
            ),
            recipes = listOf(F.pancakes, F.chickenRice).associateBy { it.id },
        )
        val s = state.summary
        assertThat(s.plannedDishes).isEqualTo(2)
        assertThat(s.uniqueRecipes).isEqualTo(2)
        assertThat(s.totalServings).isEqualTo(3)
        assertThat(s.avgKcalPerDay).isEqualTo((500 + 520) / 2)
        assertThat(state.entry(mon, MealSlot.LUNCH)?.id).isEqualTo(2)
    }

    @Test fun `no calories gives null average`() {
        val state = MenuUiState(loading = false, range = DateRange.day(mon), entries = listOf(MenuEntry(1, mon, MealSlot.SNACK, F.omelette.id, 1)), recipes = mapOf(F.omelette.id to F.omelette))
        assertThat(state.summary.avgKcalPerDay).isNull()
    }
}

class PhotoSampleSizeTest {
    @Test fun `large photos are downsampled by power of two`() {
        assertThat(PhotoStorage.sampleSize(4000, 3000, 1080)).isEqualTo(2)
        assertThat(PhotoStorage.sampleSize(8000, 6000, 1080)).isEqualTo(4)
        assertThat(PhotoStorage.sampleSize(1000, 800, 1080)).isEqualTo(1)
    }
}
