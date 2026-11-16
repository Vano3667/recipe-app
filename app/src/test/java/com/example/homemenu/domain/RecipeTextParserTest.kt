package com.example.homemenu.domain

import com.example.homemenu.domain.model.MeasureUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RecipeTextParserTest {

    @Test
    fun appShareFormat_allFieldsRecovered() {
        val text = """
            🍽 Сырники
            Нежные сырники на завтрак
            Завтрак · Легко
            Подготовка: 10 мин
            Время приготовления: 20 мин
            Духовка: 180 °C
            Порции: 4 порции
            Калорийность: 250 ккал на порцию

            Ингредиенты:
            • Творог — 500 г
            • Яйца — 2 шт
            • Соль — по вкусу

            Приготовление:
            1. Смешать творог и яйца.
            2. Обжарить на сковороде.

            Заметки: Подавать со сметаной.

            Теги: Завтрак, Быстро

            — из приложения «Домашнее меню»
        """.trimIndent()
        val r = RecipeTextParser.parse(text)
        assertEquals("Сырники", r.title)
        assertEquals("Нежные сырники на завтрак", r.description)
        assertEquals(10, r.prepTimeMin)
        assertEquals(20, r.cookingTimeMin)
        assertEquals(180, r.ovenTempC)
        assertEquals(4, r.servings)
        assertEquals(250, r.calories)
        assertEquals(listOf("Творог", "Яйца", "Соль"), r.ingredients.map { it.name })
        assertEquals(listOf(500.0, 2.0, 0.0), r.ingredients.map { it.amount })
        assertEquals(listOf(MeasureUnit.G, MeasureUnit.PCS, MeasureUnit.TO_TASTE), r.ingredients.map { it.unit })
        assertEquals(listOf("Смешать творог и яйца.", "Обжарить на сковороде."), r.steps)
        assertEquals("Подавать со сметаной.", r.notes)
        assertEquals(listOf("Завтрак", "Быстро"), r.tags)
        assertEquals(listOf("Завтрак · Легко"), r.unrecognized)
    }

    @Test
    fun messengerStyle_hashtagsStepPrefixesServingsInHeader() {
        val text = """
            Борщ

            Ингредиенты (на 6 порций):
            - 500 г говядины
            - 3 картофелины

            Способ приготовления:
            Шаг 1. Сварить бульон.
            Шаг 2: Добавить овощи.

            Время: 1 ч 30 мин
            #суп #обед
        """.trimIndent()
        val r = RecipeTextParser.parse(text)
        assertEquals("Борщ", r.title)
        assertEquals(6, r.servings)
        assertEquals(listOf("говядины", "картофелины"), r.ingredients.map { it.name })
        assertEquals(listOf(500.0, 3.0), r.ingredients.map { it.amount })
        assertEquals(listOf("Сварить бульон.", "Добавить овощи."), r.steps)
        assertEquals(90, r.cookingTimeMin)
        assertNull(r.prepTimeMin)
        assertEquals(listOf("суп", "обед"), r.tags)
    }

    @Test
    fun englishSections_unknownIngredientKept() {
        val text = "Pancakes\nIngredients:\n200 g flour\n2 eggs\na splash of milk\nInstructions:\nMix everything.\nFry.\n" +
            "Prep time: 5 min\nCooking time: 15 minutes\nServes 2"
        val r = RecipeTextParser.parse(text)
        assertEquals("Pancakes", r.title)
        assertEquals(3, r.ingredients.size)
        assertEquals(MeasureUnit.G, r.ingredients[0].unit)
        assertEquals(MeasureUnit.PCS, r.ingredients[1].unit)
        assertFalse(r.ingredients[2].recognized)
        assertEquals("a splash of milk", r.ingredients[2].name)
        assertEquals(listOf("Mix everything.", "Fry."), r.steps)
        assertEquals(5, r.prepTimeMin)
        assertEquals(15, r.cookingTimeMin)
        assertEquals(2, r.servings)
    }

    @Test
    fun unstructuredText_nothingInvented() {
        val r = RecipeTextParser.parse("Просто текст\nПодготовка: нарежьте лук")
        assertEquals("Просто текст", r.title)
        assertEquals("Подготовка: нарежьте лук", r.description)
        assertTrue(r.ingredients.isEmpty())
        assertTrue(r.steps.isEmpty())
        assertNull(r.prepTimeMin)
        assertNull(r.cookingTimeMin)
        assertNull(r.servings)
        assertNull(r.calories)
        assertTrue(r.tags.isEmpty())
        assertTrue(RecipeTextParser.parse("\n \n").isEmpty)
    }

    @Test
    fun stepsHeaderWithTimeIsCookingTime() {
        val r = RecipeTextParser.parse("Суп\nПриготовление: 40 мин\nНа 4 порции")
        assertEquals(40, r.cookingTimeMin)
        assertEquals(4, r.servings)
        assertTrue(r.steps.isEmpty())
    }

    @Test
    fun parseMinutesVariants() {
        assertEquals(45, RecipeTextParser.parseMinutes("45 мин"))
        assertEquals(90, RecipeTextParser.parseMinutes("1 ч 30 мин"))
        assertEquals(90, RecipeTextParser.parseMinutes("1,5 часа"))
        assertEquals(120, RecipeTextParser.parseMinutes("2 hours"))
        assertEquals(20, RecipeTextParser.parseMinutes("20"))
        assertNull(RecipeTextParser.parseMinutes("скоро"))
    }
}
