package com.example.homemenu.data.seed

import androidx.room.withTransaction
import com.example.homemenu.data.db.AppDatabase
import com.example.homemenu.data.db.IngredientEntity
import com.example.homemenu.data.db.ProductEntity
import com.example.homemenu.data.db.RecipeEntity
import com.example.homemenu.data.db.RecipeTagCrossRef
import com.example.homemenu.data.db.StepEntity
import com.example.homemenu.data.db.TagEntity
import com.example.homemenu.data.db.UserSettingsEntity
import com.example.homemenu.domain.model.MeasureUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Idempotent seeding:
 *  - catalog & tags: INSERT OR IGNORE on unique name → never duplicated on upgrade, new seed items get appended;
 *  - demo recipes: only once (flag in user_settings) and only if the recipes table is empty.
 * The seed list still carries a cuisine string per recipe; it is not used any more (cuisines were removed).
 */
@Singleton
class Seeder @Inject constructor(private val db: AppDatabase) {

    suspend fun seed() = db.withTransaction {
        db.productDao().insertAll(SeedData.products.map {
            ProductEntity(name = it.name, baseUnit = MeasureUnit.parse(it.unit).name, department = it.department, isCustom = false)
        })
        db.tagDao().insertAll(SeedData.tags.map { TagEntity(name = it.name, colorHint = it.color, isCustom = false) })

        val settings = db.settingsDao().get() ?: UserSettingsEntity()
        if (!settings.demoSeeded) {
            if (db.recipeDao().count() == 0) insertDemoRecipes()
            db.settingsDao().save(settings.copy(demoSeeded = true))
        }
    }

    private suspend fun insertDemoRecipes() {
        val products = db.productDao().getAll().associateBy { it.name }
        val tags = db.tagDao().getAll().associateBy { it.name }
        val now = System.currentTimeMillis()
        SeedData.recipes.forEachIndexed { index, r ->
            val id = db.recipeDao().insertRecipe(
                RecipeEntity(
                    name = r.name, cookingTimeMin = r.timeMin, difficulty = r.difficulty, category = r.category,
                    calories = r.calories, servings = r.servings,
                    createdAt = now - (SeedData.recipes.size - index) * 60_000L,
                ),
            )
            db.recipeDao().insertIngredients(r.ingredients.mapIndexed { pos, ing ->
                val product = products[ing.product] ?: error("Seed product missing: ${ing.product}")
                IngredientEntity(recipeId = id, productId = product.id, amount = ing.amount, unit = MeasureUnit.parse(ing.unit).name, position = pos)
            })
            db.recipeDao().insertSteps(r.steps.mapIndexed { pos, text -> StepEntity(recipeId = id, position = pos, text = text) })
            db.recipeDao().insertTagRefs(r.tags.mapNotNull { tags[it] }.map { RecipeTagCrossRef(id, it.id) })
        }
    }
}
