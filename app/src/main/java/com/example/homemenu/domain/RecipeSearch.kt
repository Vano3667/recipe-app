package com.example.homemenu.domain

import com.example.homemenu.domain.model.Category
import com.example.homemenu.domain.model.Difficulty
import com.example.homemenu.domain.model.Recipe
import javax.inject.Inject

enum class SearchMode(val title: String) { NAME("По названию"), INGREDIENTS("По ингредиентам") }

enum class RecipeSort(val title: String) {
    DATE_ADDED("Новые"), COOKING_TIME("По времени"), ALPHABET("По алфавиту")
}

data class RecipeQuery(
    val text: String = "",
    val mode: SearchMode = SearchMode.NAME,
    val tagIds: Set<Long> = emptySet(),
    val categories: Set<Category> = emptySet(),
    val difficulties: Set<Difficulty> = emptySet(),
    val sort: RecipeSort = RecipeSort.DATE_ADDED,
)

class RecipeSearch @Inject constructor() {

    fun apply(recipes: List<Recipe>, q: RecipeQuery): List<Recipe> {
        val filtered = recipes.filter { r ->
            matchesText(r, q) &&
                (q.tagIds.isEmpty() || r.tags.any { it.id in q.tagIds }) &&
                (q.categories.isEmpty() || r.category in q.categories) &&
                (q.difficulties.isEmpty() || r.difficulty in q.difficulties)
        }
        return when (q.sort) {
            RecipeSort.DATE_ADDED -> filtered.sortedWith(compareByDescending<Recipe> { it.createdAt }.thenByDescending { it.id })
            RecipeSort.COOKING_TIME -> filtered.sortedWith(compareBy<Recipe> { it.cookingTimeMin }.thenBy { it.name.lowercase() })
            RecipeSort.ALPHABET -> filtered.sortedBy { it.name.lowercase() }
        }
    }

    private fun matchesText(r: Recipe, q: RecipeQuery): Boolean {
        val text = q.text.trim()
        if (text.isEmpty()) return true
        return when (q.mode) {
            SearchMode.NAME -> r.name.contains(text, ignoreCase = true)
            SearchMode.INGREDIENTS -> matchesIngredients(r, ingredientTokens(text))
        }
    }

    /** "курица и рис", "курица, рис" → ["курица", "рис"] */
    fun ingredientTokens(text: String): List<String> =
        text.lowercase()
            .split(',', ';', '+', ' ', '\n')
            .map { it.trim() }
            .filter { it.isNotEmpty() && it !in STOP_WORDS }

    /** A recipe matches when EVERY token matches at least one ingredient (simple Russian stemming). */
    fun matchesIngredients(r: Recipe, tokens: List<String>): Boolean {
        if (tokens.isEmpty()) return true
        val words = r.ingredients.flatMap { ing ->
            ing.product.name.lowercase().replace('ё', 'е').split(' ', '(', ')', '-', ',').filter { it.isNotBlank() }
        }
        return tokens.all { token ->
            val stem = stem(token)
            words.any { it.startsWith(stem) }
        }
    }

    fun stem(token: String): String {
        val t = token.replace('ё', 'е')
        return if (t.length <= 4) t else t.take((t.length - 2).coerceAtLeast(3))
    }

    private companion object {
        val STOP_WORDS = setOf("и", "с", "со", "или", "из", "на")
    }
}
