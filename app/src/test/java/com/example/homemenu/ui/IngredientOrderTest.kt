package com.example.homemenu.ui

import com.example.homemenu.ui.recipes.IngredientOrder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class IngredientOrderTest {

    private val list = listOf("Мука", "Яйца", "Молоко", "Соль")

    @Test fun moveUp() {
        assertEquals(listOf("Мука", "Молоко", "Яйца", "Соль"), IngredientOrder.move(list, 2, 1))
    }

    @Test fun moveDown() {
        assertEquals(listOf("Яйца", "Мука", "Молоко", "Соль"), IngredientOrder.move(list, 0, 1))
    }

    @Test fun moveFirstToLast() {
        assertEquals(listOf("Яйца", "Молоко", "Соль", "Мука"), IngredientOrder.move(list, 0, 3))
    }

    @Test fun outOfRangeKeepsList() {
        assertSame(list, IngredientOrder.move(list, 0, -1))
        assertSame(list, IngredientOrder.move(list, 3, 4))
        assertSame(list, IngredientOrder.move(list, 1, 1))
    }

    @Test fun keepsAllItems() {
        val moved = IngredientOrder.move(list, 3, 0)
        assertEquals(list.size, moved.size)
        assertEquals(list.toSet(), moved.toSet())
        assertEquals("Соль", moved.first())
    }
}
