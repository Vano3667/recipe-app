package com.example.homemenu.domain

import com.example.homemenu.domain.model.Category
import com.example.homemenu.domain.model.MealSlot
import com.example.homemenu.domain.model.MenuEntry
import com.example.homemenu.domain.model.Recipe
import java.time.LocalDate
import javax.inject.Inject
import kotlin.random.Random

data class GeneratorOptions(
    /** Recipe must have at least one of these tags (empty = any). Applied to all slots. */
    val tagIds: Set<Long> = emptySet(),
    /** Per-slot tag filters, e.g. breakfast → only "Быстрое". Overrides [tagIds] for that slot. */
    val slotTagIds: Map<MealSlot, Set<Long>> = emptyMap(),
    /** Allowed categories per slot (missing/empty = any). */
    val slotCategories: Map<MealSlot, Set<Category>> = emptyMap(),
    val slots: Set<MealSlot> = MealSlot.entries.toSet(),
    val servings: Int = 2,
)

data class GeneratedSlot(val date: LocalDate, val slot: MealSlot, val recipeId: Long, val servings: Int)

class MenuGeneratorUseCase @Inject constructor() {

    /** Fills only EMPTY slots. Slots without a matching recipe stay empty. */
    fun generate(
        days: List<LocalDate>,
        existing: List<MenuEntry>,
        recipes: List<Recipe>,
        options: GeneratorOptions,
        random: Random = Random.Default,
    ): List<GeneratedSlot> {
        val occupied = existing.map { it.date to it.slot }.toHashSet()
        val result = mutableListOf<GeneratedSlot>()
        val servings = options.servings.coerceAtLeast(1)
        for (day in days) {
            val usedToday = existing.filter { it.date == day }.map { it.recipeId }.toMutableSet()
            for (slot in MealSlot.entries) {
                if (slot !in options.slots || (day to slot) in occupied) continue
                val candidates = candidatesFor(slot, recipes, options)
                if (candidates.isEmpty()) continue
                val fresh = candidates.filter { it.id !in usedToday }
                val pick = (fresh.ifEmpty { candidates }).random(random)
                usedToday += pick.id
                result += GeneratedSlot(day, slot, pick.id, servings)
            }
        }
        return result
    }

    fun candidatesFor(slot: MealSlot, recipes: List<Recipe>, options: GeneratorOptions): List<Recipe> {
        val tags = options.slotTagIds[slot]?.takeIf { it.isNotEmpty() } ?: options.tagIds
        val categories = options.slotCategories[slot].orEmpty()
        return recipes.filter { r ->
            (tags.isEmpty() || r.tags.any { it.id in tags }) &&
                (categories.isEmpty() || r.category in categories)
        }
    }
}
