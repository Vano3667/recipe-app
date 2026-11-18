package com.example.homemenu.ui

import com.example.homemenu.domain.RecipeTextParser
import com.example.homemenu.domain.model.MeasureUnit
import com.example.homemenu.ui.recipes.BulkIngredients
import com.example.homemenu.ui.recipes.BulkRow
import com.example.homemenu.ui.recipes.EditForm
import com.example.homemenu.ui.recipes.RecipeTextImport
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RecipeTextImportTest {

    private val text = """
        Сырники
        Нежные на завтрак
        Завтрак · Легко
        Подготовка: 10 мин
        Время приготовления: 20 мин
        Порции: 4
        Ингредиенты:
        • Творог — 500 г
        • Соль — по вкусу
        Приготовление:
        1. Смешать.
        2. Обжарить.
        Заметки: Со сметаной.
        Теги: Завтрак, Быстро, быстро
    """.trimIndent()

    @Test
    fun toForm_fillsOnlyFoundFields_keepsUnrecognizedInNotes() {
        val p = RecipeTextParser.parse(text)
        val f = RecipeTextImport.toForm(p, existingTags = listOf(7L to "завтрак", 8L to "Ужин"))
        assertEquals("Сырники", f.name)
        assertEquals("Нежные на завтрак", f.description)
        assertEquals("20", f.timeText)
        assertEquals("10", f.prepTimeText)
        assertEquals("4", f.servingsText)
        assertEquals("", f.ovenTempText)
        assertEquals("", f.caloriesText)
        assertEquals(listOf("Смешать.", "Обжарить."), f.steps.map { it.text })
        assertEquals("Со сметаной.\n\n${RecipeTextImport.UNRECOGNIZED_HEADER}\nЗавтрак · Легко", f.notes)
        assertEquals(setOf(7L), f.tagIds)
        assertTrue(f.ingredients.isEmpty())
        assertEquals(0L, f.id)
    }

    @Test
    fun matchTags_existingAndMissingDeduplicated() {
        val p = RecipeTextParser.parse(text)
        val (ids, missing) = RecipeTextImport.matchTags(p.tags, listOf(7L to "Завтрак"))
        assertEquals(setOf(7L), ids)
        assertEquals(listOf("Быстро"), missing)
    }

    @Test
    fun ingredientText_reparsesToSameRowsInOrder() {
        val p = RecipeTextParser.parse(text)
        val rows = BulkIngredients.toRows(RecipeTextImport.ingredientText(p), emptyList())
        assertEquals(listOf("Творог", "Соль"), rows.map { it.name })
        assertEquals(listOf("500", ""), rows.map { it.amountText })
        assertEquals(listOf(MeasureUnit.G, MeasureUnit.TO_TASTE), rows.map { it.unit })
        assertTrue(RecipeTextImport.rowsMissingAmount(rows).isEmpty())
    }

    @Test
    fun rowsMissingAmount_flagsOnlyRowsThatNeedAnAmount() {
        val rows = listOf(
            BulkRow("молоко", "", MeasureUnit.ML, recognized = false, duplicate = false, include = true),
            BulkRow("соль", "", MeasureUnit.TO_TASTE, recognized = true, duplicate = false, include = true),
            BulkRow("мука", "0", MeasureUnit.G, recognized = true, duplicate = false, include = true),
            BulkRow("яйца", "2", null, recognized = true, duplicate = false, include = true),
        )
        assertEquals(listOf("молоко", "мука"), RecipeTextImport.rowsMissingAmount(rows).map { it.name })
    }

    @Test
    fun toForm_withoutServings_usesFormDefault() {
        val f = RecipeTextImport.toForm(RecipeTextParser.parse("Чай\nПриготовление:\nЗаварить."), emptyList())
        assertEquals(EditForm().servingsText, f.servingsText)
        assertEquals(listOf("Заварить."), f.steps.map { it.text })
        assertEquals("", f.notes)
    }
}
