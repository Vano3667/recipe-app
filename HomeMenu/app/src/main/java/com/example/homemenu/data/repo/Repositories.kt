package com.example.homemenu.data.repo

import androidx.room.withTransaction
import com.example.homemenu.data.db.AppDatabase
import com.example.homemenu.data.db.IngredientEntity
import com.example.homemenu.data.db.MenuEntryEntity
import com.example.homemenu.data.db.ProductEntity
import com.example.homemenu.data.db.RecipeEntity
import com.example.homemenu.data.db.RecipeTagCrossRef
import com.example.homemenu.data.db.ShoppingCheckEntity
import com.example.homemenu.data.db.StepEntity
import com.example.homemenu.data.db.TagEntity
import com.example.homemenu.data.db.UserSettingsEntity
import com.example.homemenu.data.toDomain
import com.example.homemenu.domain.GeneratedSlot
import com.example.homemenu.domain.model.Department
import com.example.homemenu.domain.model.MealSlot
import com.example.homemenu.domain.model.MeasureUnit
import com.example.homemenu.domain.model.MenuEntry
import com.example.homemenu.domain.model.Product
import com.example.homemenu.domain.model.Recipe
import com.example.homemenu.domain.model.Tag
import com.example.homemenu.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecipeRepository @Inject constructor(private val db: AppDatabase) {
    private val dao = db.recipeDao()

    fun observeRecipes(): Flow<List<Recipe>> = dao.observeAll().map { list -> list.map { it.toDomain() } }
    fun observeRecipe(id: Long): Flow<Recipe?> = dao.observe(id).map { it?.toDomain() }
    suspend fun getRecipe(id: Long): Recipe? = dao.get(id)?.toDomain()
    suspend fun getAll(): List<Recipe> = dao.getAll().map { it.toDomain() }

    fun observeProducts(): Flow<List<Product>> = db.productDao().observeAll().map { l -> l.map { it.toDomain() } }
    fun observeTags(): Flow<List<Tag>> = db.tagDao().observeAll().map { l -> l.map { it.toDomain() } }

    /** Insert (id == 0) or replace. Validates name and servings. Returns recipe id. */
    suspend fun save(recipe: Recipe): Long {
        require(recipe.name.isNotBlank()) { "Название обязательно" }
        require(recipe.servings > 0) { "Количество порций должно быть больше нуля" }
        return db.withTransaction {
            val entity = RecipeEntity(
                id = recipe.id,
                name = recipe.name.trim(),
                photoFile = recipe.photoFile,
                cookingTimeMin = recipe.cookingTimeMin.coerceAtLeast(0),
                difficulty = recipe.difficulty.name,
                category = recipe.category.name,
                cuisine = recipe.cuisine.trim(),
                calories = recipe.calories,
                servings = recipe.servings,
                notes = recipe.notes,
                createdAt = if (recipe.createdAt > 0) recipe.createdAt else System.currentTimeMillis(),
            )
            val id = if (recipe.id == 0L) dao.insertRecipe(entity) else {
                dao.updateRecipe(entity)
                dao.clearIngredients(recipe.id); dao.clearSteps(recipe.id); dao.clearTags(recipe.id)
                recipe.id
            }
            dao.insertIngredients(recipe.ingredients.mapIndexed { i, ing ->
                IngredientEntity(recipeId = id, productId = ing.product.id, amount = ing.amount, unit = ing.unit.name, position = i)
            })
            dao.insertSteps(recipe.steps.filter { it.text.isNotBlank() || it.photoFile != null }.mapIndexed { i, s ->
                StepEntity(recipeId = id, position = i, text = s.text.trim(), photoFile = s.photoFile)
            })
            dao.insertTagRefs(recipe.tags.map { RecipeTagCrossRef(id, it.id) })
            id
        }
    }

    suspend fun scheduledCount(recipeId: Long): Int = db.menuDao().countForRecipe(recipeId)

    /** Deletes the recipe and (optionally) clears every menu slot that used it. */
    suspend fun delete(recipeId: Long, clearSlots: Boolean = true) = db.withTransaction {
        if (clearSlots) db.menuDao().deleteForRecipe(recipeId)
        dao.deleteRecipe(recipeId)
    }

    suspend fun addProduct(name: String, unit: MeasureUnit, department: Department): Product {
        val trimmed = name.trim()
        require(trimmed.isNotEmpty()) { "Введите название продукта" }
        db.productDao().findByName(trimmed)?.let { return it.toDomain() }
        val id = db.productDao().insert(ProductEntity(name = trimmed, baseUnit = unit.name, department = department.name, isCustom = true))
        return Product(id, trimmed, unit, department, true)
    }

    suspend fun addTag(name: String, colorHint: String): Tag {
        val trimmed = name.trim()
        require(trimmed.isNotEmpty()) { "Введите название тега" }
        db.tagDao().findByName(trimmed)?.let { return it.toDomain() }
        val id = db.tagDao().insert(TagEntity(name = trimmed, colorHint = colorHint, isCustom = true))
        return Tag(id, trimmed, colorHint, true)
    }
}

@Singleton
class MenuRepository @Inject constructor(private val db: AppDatabase) {
    private val dao = db.menuDao()

    fun observeRange(from: LocalDate, to: LocalDate): Flow<List<MenuEntry>> =
        dao.observeRange(from.toString(), to.toString()).map { l -> l.map { it.toDomain() } }

    suspend fun getRange(from: LocalDate, to: LocalDate): List<MenuEntry> =
        dao.getRange(from.toString(), to.toString()).map { it.toDomain() }

    suspend fun setSlot(date: LocalDate, slot: MealSlot, recipeId: Long, servings: Int) {
        require(servings > 0)
        val existing = dao.find(date.toString(), slot.name)
        dao.upsert(MenuEntryEntity(id = existing?.id ?: 0, date = date.toString(), slot = slot.name, recipeId = recipeId, servings = servings))
    }

    suspend fun updateServings(entryId: Long, servings: Int) {
        if (servings <= 0) return
        dao.get(entryId)?.let { dao.update(it.copy(servings = servings)) }
    }

    suspend fun clear(entryId: Long) = dao.delete(entryId)

    /** Drag & drop: move to an empty slot, or swap with an occupied one. */
    suspend fun move(entryId: Long, toDate: LocalDate, toSlot: MealSlot) = db.withTransaction {
        val source = dao.get(entryId) ?: return@withTransaction
        if (source.date == toDate.toString() && source.slot == toSlot.name) return@withTransaction
        val target = dao.find(toDate.toString(), toSlot.name)
        dao.delete(source.id)
        if (target != null) {
            dao.delete(target.id)
            dao.upsert(target.copy(id = 0, date = source.date, slot = source.slot))
        }
        dao.upsert(source.copy(id = 0, date = toDate.toString(), slot = toSlot.name))
    }

    suspend fun applyGenerated(slots: List<GeneratedSlot>) {
        dao.insertAllIgnore(slots.map { MenuEntryEntity(date = it.date.toString(), slot = it.slot.name, recipeId = it.recipeId, servings = it.servings) })
    }
}

@Singleton
class ShoppingRepository @Inject constructor(private val db: AppDatabase) {
    fun periodPrefix(from: LocalDate, to: LocalDate) = "$from..$to#"

    fun observeChecked(from: LocalDate, to: LocalDate): Flow<Set<String>> {
        val prefix = periodPrefix(from, to)
        return db.shoppingDao().observeChecked(prefix).map { keys -> keys.map { it.removePrefix(prefix) }.toSet() }
    }

    suspend fun setChecked(from: LocalDate, to: LocalDate, itemKey: String, checked: Boolean) {
        val key = periodPrefix(from, to) + itemKey
        if (checked) db.shoppingDao().check(ShoppingCheckEntity(key)) else db.shoppingDao().uncheck(key)
    }

    suspend fun clearChecks(from: LocalDate, to: LocalDate) = db.shoppingDao().clear(periodPrefix(from, to))
}

@Singleton
class SettingsRepository @Inject constructor(private val db: AppDatabase) {
    fun observeTheme(): Flow<ThemeMode> = db.settingsDao().observe().map { ThemeMode.parse(it?.themeMode) }

    suspend fun setTheme(mode: ThemeMode) {
        val current = db.settingsDao().get() ?: UserSettingsEntity()
        db.settingsDao().save(current.copy(themeMode = mode.name))
    }

    suspend fun getTheme(): ThemeMode = ThemeMode.parse(db.settingsDao().get()?.themeMode)
}
