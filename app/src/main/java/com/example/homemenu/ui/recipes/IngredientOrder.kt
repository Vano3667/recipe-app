package com.example.homemenu.ui.recipes

/**
 * Task 1 / 23.4: changing the order of ingredients in the recipe edit form.
 * The saved order is the list order (RecipeRepository.save writes position = index),
 * so moving an item in the form list is enough for the new order to be stored.
 */
object IngredientOrder {

    /** Moves the element at [from] to [to]. Out-of-range or equal indices return the list unchanged. */
    fun <T> move(list: List<T>, from: Int, to: Int): List<T> {
        if (from !in list.indices || to !in list.indices || from == to) return list
        val m = list.toMutableList()
        val item = m.removeAt(from)
        m.add(to, item)
        return m
    }
}
