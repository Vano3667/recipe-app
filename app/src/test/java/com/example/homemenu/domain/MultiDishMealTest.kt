package com.example.homemenu.domain

import com.example.homemenu.F
import com.example.homemenu.domain.model.MealSlot
import com.example.homemenu.domain.model.MenuEntry
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.LocalDate
import kotlin.random.Random

/** Task 3: the use cases must not assume one dish per meal. */
class MultiDishMealTest {
    private val gen = MenuGeneratorUseCase()
    private val shopping = ShoppingListUseCase(ScalingUseCase())
    private val monday = LocalDate.of(2026, 11, 16)

    private val twoDishLunch = listOf(
        MenuEntry(1, monday, MealSlot.LUNCH, F.pancakes.id, 4, position = 0),
        MenuEntry(2, monday, MealSlot.LUNCH, F.omelette.id, 2, position = 1),
    )

    @Test fun `meal with several dishes is not refilled`() {
        val opts = GeneratorOptions(slots = setOf(MealSlot.LUNCH))
        val result = gen.generate(listOf(monday), twoDishLunch, F.all, opts, Random(1))
        assertThat(result).isEmpty()
    }

    @Test fun `every dish of a meal counts as used that day`() {
        val opts = GeneratorOptions(slots = setOf(MealSlot.BREAKFAST, MealSlot.LUNCH))
        repeat(20) { seed ->
            val result = gen.generate(listOf(monday), twoDishLunch, F.all, opts, Random(seed))
            assertThat(result.map { it.slot }).containsExactly(MealSlot.BREAKFAST)
            assertThat(result.single().recipeId).isNoneOf(F.pancakes.id, F.omelette.id)
        }
    }

    @Test fun `shopping list sums all dishes of one meal`() {
        val byId = F.all.associateBy { it.id }
        val dishes = twoDishLunch.mapNotNull { e -> byId[e.recipeId]?.let { PlannedDish(it, e.servings) } }
        val items = shopping.aggregate(dishes)
        // milk: 500 ml (pancakes, 4 of 4 servings) + 100 ml (omelette, 50 ml x 2) = 600 ml
        assertThat(items.single { it.product.name == "Молоко" }.text).isEqualTo("600 мл")
        // eggs: 2 + 3 x 2 = 8
        assertThat(items.single { it.product.name == "Яйцо куриное" }.amount).isEqualTo(ScaledAmount(8, false))
    }
}
