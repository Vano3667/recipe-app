package com.example.homemenu.domain

import com.example.homemenu.F
import com.example.homemenu.domain.model.Category
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class RecipeSearchTest {
    private val search = RecipeSearch()
    private fun names(q: RecipeQuery) = search.apply(F.all, q).map { it.name }

    @Test fun `empty query returns everything newest first`() {
        assertThat(names(RecipeQuery())).containsExactly("Омлет", "Борщ", "Курица с рисом", "Блины").inOrder()
    }

    @Test fun `name search is case insensitive substring`() {
        assertThat(names(RecipeQuery(text = "бЛИ"))).containsExactly("Блины")
        assertThat(names(RecipeQuery(text = "  рис  "))).containsExactly("Курица с рисом")
    }

    @Test fun `ingredient search requires every token`() {
        val q = RecipeQuery(text = "курица, рис", mode = SearchMode.INGREDIENTS)
        assertThat(names(q)).containsExactly("Курица с рисом")
        assertThat(names(RecipeQuery(text = "молоко и яйцо", mode = SearchMode.INGREDIENTS)))
            .containsExactly("Блины", "Омлет")
    }

    @Test fun `ingredient search ignores yo and stop words`() {
        assertThat(names(RecipeQuery(text = "свекла", mode = SearchMode.INGREDIENTS))).containsExactly("Борщ")
        assertThat(names(RecipeQuery(text = "свёкла", mode = SearchMode.INGREDIENTS))).containsExactly("Борщ")
        assertThat(search.ingredientTokens("курица и рис")).containsExactly("курица", "рис").inOrder()
    }

    @Test fun `unknown ingredient finds nothing`() {
        assertThat(names(RecipeQuery(text = "ананас", mode = SearchMode.INGREDIENTS))).isEmpty()
    }

    @Test fun `filters by tag category and difficulty`() {
        assertThat(names(RecipeQuery(tagIds = setOf(F.festive.id)))).containsExactly("Борщ")
        assertThat(names(RecipeQuery(categories = setOf(Category.BREAKFAST), sort = RecipeSort.ALPHABET))).containsExactly("Блины", "Омлет").inOrder()
        assertThat(names(RecipeQuery(tagIds = setOf(F.veg.id), categories = setOf(Category.LUNCH)))).isEmpty()
    }

    @Test fun `sort by time and alphabet`() {
        assertThat(names(RecipeQuery(sort = RecipeSort.COOKING_TIME))).containsExactly("Омлет", "Курица с рисом", "Блины", "Борщ").inOrder()
        assertThat(names(RecipeQuery(sort = RecipeSort.ALPHABET))).containsExactly("Блины", "Борщ", "Курица с рисом", "Омлет").inOrder()
    }
}
