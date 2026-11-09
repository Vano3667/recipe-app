package com.example.homemenu.data

import com.example.homemenu.data.db.MenuEntryEntity
import com.example.homemenu.data.db.ProductEntity
import com.example.homemenu.data.db.RecipeFull
import com.example.homemenu.data.db.TagEntity
import com.example.homemenu.domain.model.Category
import com.example.homemenu.domain.model.Department
import com.example.homemenu.domain.model.Difficulty
import com.example.homemenu.domain.model.Ingredient
import com.example.homemenu.domain.model.MealSlot
import com.example.homemenu.domain.model.MeasureUnit
import com.example.homemenu.domain.model.MenuEntry
import com.example.homemenu.domain.model.Product
import com.example.homemenu.domain.model.Recipe
import com.example.homemenu.domain.model.Step
import com.example.homemenu.domain.model.Tag
import java.time.LocalDate

fun ProductEntity.toDomain() = Product(id, name, MeasureUnit.parse(baseUnit), Department.parse(department), isCustom)

fun TagEntity.toDomain() = Tag(id, name, colorHint, isCustom)

fun RecipeFull.toDomain() = Recipe(
    id = recipe.id,
    name = recipe.name,
    photoFile = recipe.photoFile,
    cookingTimeMin = recipe.cookingTimeMin,
    difficulty = Difficulty.parse(recipe.difficulty),
    category = Category.parse(recipe.category),
    cuisine = recipe.cuisine,
    calories = recipe.calories,
    servings = recipe.servings,
    ingredients = ingredients.sortedBy { it.ingredient.position }.map {
        Ingredient(it.product.toDomain(), it.ingredient.amount, MeasureUnit.parse(it.ingredient.unit))
    },
    steps = steps.sortedBy { it.position }.map { Step(it.text, it.photoFile) },
    notes = recipe.notes,
    tags = tags.map { it.toDomain() }.sortedBy { it.id },
    createdAt = recipe.createdAt,
)

fun MenuEntryEntity.toDomain() = MenuEntry(id, LocalDate.parse(date), MealSlot.parse(slot), recipeId, servings)
