package com.example.homemenu.domain

import com.example.homemenu.F
import com.example.homemenu.domain.model.Category
import com.example.homemenu.domain.model.MealSlot
import com.example.homemenu.domain.model.MenuEntry
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.LocalDate
import kotlin.random.Random

class MenuGeneratorUseCaseTest {
    private val gen = MenuGeneratorUseCase()
    private val monday = LocalDate.of(2026, 11, 9)
    private val week = (0L..6L).map { monday.plusDays(it) }

    @Test fun `fills every empty slot when any recipe fits`() {
        val result = gen.generate(week, emptyList(), F.all, GeneratorOptions(), Random(1))
        assertThat(result).hasSize(7 * MealSlot.entries.size)
    }

    @Test fun `occupied slots are never overwritten`() {
        val existing = listOf(MenuEntry(1, monday, MealSlot.LUNCH, F.borscht.id, 4))
        val result = gen.generate(listOf(monday), existing, F.all, GeneratorOptions(), Random(2))
        assertThat(result.map { it.slot }).doesNotContain(MealSlot.LUNCH)
        assertThat(result).hasSize(MealSlot.entries.size - 1)
    }

    @Test fun `slot categories restrict candidates`() {
        val opts = GeneratorOptions(
            slotCategories = MealSlot.entries.mapNotNull { s -> s.defaultCategory?.let { s to setOf(it) } }.toMap(),
        )
        val result = gen.generate(week, emptyList(), F.all, opts, Random(3))
        val byId = F.all.associateBy { it.id }
        result.filter { it.slot == MealSlot.BREAKFAST }.forEach { assertThat(byId.getValue(it.recipeId).category).isEqualTo(Category.BREAKFAST) }
        result.filter { it.slot == MealSlot.LUNCH }.forEach { assertThat(byId.getValue(it.recipeId).category).isEqualTo(Category.LUNCH) }
        // No DINNER recipes in fixtures -> dinner slots stay empty; snack has no category restriction.
        assertThat(result.none { it.slot == MealSlot.DINNER }).isTrue()
        assertThat(result.count { it.slot == MealSlot.SNACK }).isEqualTo(7)
    }

    @Test fun `tag filter keeps only tagged recipes`() {
        val opts = GeneratorOptions(tagIds = setOf(F.festive.id))
        val result = gen.generate(week, emptyList(), F.all, opts, Random(4))
        assertThat(result.map { it.recipeId }.toSet()).containsExactly(F.borscht.id)
    }

    @Test fun `no candidates leaves slots empty`() {
        val opts = GeneratorOptions(tagIds = setOf(999L))
        assertThat(gen.generate(week, emptyList(), F.all, opts)).isEmpty()
        assertThat(gen.generate(week, emptyList(), emptyList(), GeneratorOptions())).isEmpty()
    }

    @Test fun `only chosen slots are filled with requested servings`() {
        val opts = GeneratorOptions(slots = setOf(MealSlot.DINNER), servings = 3)
        val result = gen.generate(week, emptyList(), F.all, opts, Random(5))
        assertThat(result.map { it.slot }.toSet()).containsExactly(MealSlot.DINNER)
        assertThat(result.map { it.servings }.toSet()).containsExactly(3)
    }

    @Test fun `servings are at least one`() {
        val result = gen.generate(listOf(monday), emptyList(), F.all, GeneratorOptions(servings = 0), Random(6))
        assertThat(result.all { it.servings == 1 }).isTrue()
    }

    @Test fun `avoids repeating a recipe within one day when possible`() {
        repeat(20) { seed ->
            val result = gen.generate(listOf(monday), emptyList(), F.all, GeneratorOptions(), Random(seed))
            assertThat(result.map { it.recipeId }.distinct()).hasSize(4)
        }
    }

    @Test fun `generation is deterministic for the same seed`() {
        val a = gen.generate(week, emptyList(), F.all, GeneratorOptions(), Random(42))
        val b = gen.generate(week, emptyList(), F.all, GeneratorOptions(), Random(42))
        assertThat(a).isEqualTo(b)
    }
}
