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

/** Name key for duplicate checks: case-insensitive (Cyrillic too), ё = е, collapsed spaces. */
fun nameKey(name: String): String = name.trim().lowercase().replace('ё', 'е').replace(Regex("\\s+"), " ")

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
            // The legacy cuisine value is preserved on edit (no longer editable).
            val legacyCuisine = if (recipe.id != 0L) dao.getEntity(recipe.id)?.cuisine.orEmpty() else ""
            val entity = RecipeEntity(
                id = recipe.id,
                name = recipe.name.trim(),
                photoFile = recipe.photoFile,
                cookingTimeMin = recipe.cookingTimeMin.coerceAtLeast(0),
                difficulty = recipe.difficulty.name,
                category = recipe.category.name,
                cuisine = legacyCuisine,
                calories = recipe.calories,
                servings = recipe.servings,
                notes = recipe.notes,
                createdAt = if (recipe.createdAt > 0) recipe.createdAt else System.currentTimeMillis(),
                description = recipe.description.trim(),
                prepTimeMin = recipe.prepTimeMin.coerceAtLeast(0),
                ovenTempC = recipe.ovenTempC?.takeIf { it > 0 },
            )
            val id = if (recipe.id == 0L) dao.insertRecipe(entity) else {
                dao.updateRecipe(entity)
                dao.clearIngredients(recipe.id); dao.clearSteps(recipe.id); dao.clearTags(recipe.id)
                recipe.id
            }
            dao.insertIngredients(recipe.ingredients.mapIndexed { i, ing ->
                IngredientEntity(
                    recipeId = id, productId = ing.product.id,
                    amount = if (ing.unit.isAmountless) 0.0 else ing.amount, unit = ing.unit.name, position = i,
                )
            })
            dao.insertSteps(recipe.steps.filter { it.text.isNotBlank() || it.photoFile != null }.mapIndexed { i, s ->
                StepEntity(recipeId = id, position = i, text = s.text.trim(), photoFile = s.photoFile)
            })
            dao.insertTagRefs(recipe.tags.distinctBy { it.id }.map { RecipeTagCrossRef(id, it.id) })
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
        val trimmed = name.trim().replace(Regex("\\s+"), " ")
        require(trimmed.isNotEmpty()) { "Введите название продукта" }
        findProduct(trimmed)?.let { return it }
        val base = if (unit.isAmountless || unit == MeasureUnit.PINCH || unit == MeasureUnit.TBSP || unit == MeasureUnit.TSP) MeasureUnit.G else unit
        val id = db.productDao().insert(ProductEntity(name = trimmed, baseUnit = base.name, department = department.name, isCustom = true))
        return Product(id, trimmed, base, department, true)
    }

    /** Case-insensitive lookup (SQLite NOCASE does not fold Cyrillic, so it is done here). */
    suspend fun findProduct(name: String): Product? {
        db.productDao().findByName(name.trim())?.let { return it.toDomain() }
        val key = nameKey(name)
        return db.productDao().getAll().firstOrNull { nameKey(it.name) == key }?.toDomain()
    }

    /** For bulk ingredient input: existing products are reused, missing ones are created as custom products. */
    suspend fun findOrCreateProducts(items: List<Pair<String, MeasureUnit>>): List<Product> = db.withTransaction {
        val byKey = db.productDao().getAll().associateBy { nameKey(it.name) }.toMutableMap()
        items.map { (name, unit) ->
            val key = nameKey(name)
            byKey[key]?.toDomain() ?: addProduct(name, unit, Department.OTHER).also { p ->
                byKey[key] = ProductEntity(p.id, p.name, p.baseUnit.name, p.department.name, true)
            }
        }
    }

    suspend fun addTag(name: String, colorHint: String): Tag {
        val trimmed = name.trim().replace(Regex("\\s+"), " ")
        require(trimmed.isNotEmpty()) { "Введите название тега" }
        findTag(trimmed)?.let { return it }
        val id = db.tagDao().insert(TagEntity(name = trimmed, colorHint = colorHint, isCustom = true))
        return Tag(id, trimmed, colorHint, true)
    }

    suspend fun findTag(name: String): Tag? {
        val key = nameKey(name)
        return db.tagDao().getAll().firstOrNull { nameKey(it.name) == key }?.toDomain()
    }
}

@Singleton
class MenuRepository @Inject constructor(private val db: AppDatabase) {
    private val dao = db.menuDao()

    fun observeRange(from: LocalDate, to: LocalDate): Flow<List<MenuEntry>> =
        dao.observeRange(from.toString(), to.toString()).map { l -> l.map { it.toDomain() } }

    suspend fun getRange(from: LocalDate, to: LocalDate): List<MenuEntry> =
        dao.getRange(from.toString(), to.toString()).map { it.toDomain() }

    /**
     * Adds a dish to the end of a meal. The same recipe is not added twice to one meal
     * (returns false), so a repeated tap cannot create duplicates.
     */
    suspend fun addToSlot(date: LocalDate, slot: MealSlot, recipeId: Long, servings: Int): Boolean {
        require(servings > 0)
        return db.withTransaction {
            val existing = dao.getSlot(date.toString(), slot.name)
            if (existing.any { it.recipeId == recipeId }) return@withTransaction false
            val position = (existing.maxOfOrNull { it.position } ?: -1) + 1
            dao.insert(MenuEntryEntity(date = date.toString(), slot = slot.name, recipeId = recipeId, servings = servings, position = position))
            true
        }
    }

    /** Kept for callers of the v1 API: adds the dish to the meal. */
    suspend fun setSlot(date: LocalDate, slot: MealSlot, recipeId: Long, servings: Int) {
        addToSlot(date, slot, recipeId, servings)
    }

    /** Replaces the recipe of one dish, keeping its place and servings. False if the meal already has that recipe. */
    suspend fun replaceRecipe(entryId: Long, recipeId: Long): Boolean = db.withTransaction {
        val entry = dao.get(entryId) ?: return@withTransaction false
        if (entry.recipeId == recipeId) return@withTransaction true
        val siblings = dao.getSlot(entry.date, entry.slot)
        if (siblings.any { it.id != entryId && it.recipeId == recipeId }) return@withTransaction false
        dao.update(entry.copy(recipeId = recipeId))
        true
    }

    /** Moves a dish up (delta = -1) or down (+1) inside its meal. */
    suspend fun reorder(entryId: Long, delta: Int) = db.withTransaction {
        val entry = dao.get(entryId) ?: return@withTransaction
        val list = dao.getSlot(entry.date, entry.slot).toMutableList()
        val from = list.indexOfFirst { it.id == entryId }
        val to = from + delta
        if (from < 0 || to !in list.indices) return@withTransaction
        val item = list.removeAt(from)
        list.add(to, item)
        dao.updateAll(list.mapIndexed { i, e -> e.copy(position = i) })
    }

    suspend fun updateServings(entryId: Long, servings: Int) {
        if (servings <= 0) return
        dao.get(entryId)?.let { dao.update(it.copy(servings = servings)) }
    }

    /** Removes one dish from its meal; the remaining dishes keep their order. */
    suspend fun clear(entryId: Long) = db.withTransaction {
        val entry = dao.get(entryId) ?: return@withTransaction
        dao.delete(entryId)
        renumber(entry.date, entry.slot)
    }

    /**
     * Drag & drop: the dish is appended to the target meal. Returns false (nothing changes)
     * if the target meal already contains this recipe.
     */
    suspend fun move(entryId: Long, toDate: LocalDate, toSlot: MealSlot): Boolean = db.withTransaction {
        val source = dao.get(entryId) ?: return@withTransaction false
        if (source.date == toDate.toString() && source.slot == toSlot.name) return@withTransaction true
        val target = dao.getSlot(toDate.toString(), toSlot.name)
        if (target.any { it.recipeId == source.recipeId }) return@withTransaction false
        val position = (target.maxOfOrNull { it.position } ?: -1) + 1
        dao.update(source.copy(date = toDate.toString(), slot = toSlot.name, position = position))
        renumber(source.date, source.slot)
        true
    }

    /** Auto-fill writes only to empty meals (the generator never proposes occupied ones). */
    suspend fun applyGenerated(slots: List<GeneratedSlot>) = db.withTransaction {
        val toInsert = slots.filter { dao.getSlot(it.date.toString(), it.slot.name).isEmpty() }
            .distinctBy { Triple(it.date, it.slot, it.recipeId) }
        dao.insertAll(toInsert.mapIndexed { _, it ->
            MenuEntryEntity(date = it.date.toString(), slot = it.slot.name, recipeId = it.recipeId, servings = it.servings, position = 0)
        })
    }

    private suspend fun renumber(date: String, slot: String) {
        val list = dao.getSlot(date, slot)
        val renumbered = list.mapIndexed { i, e -> e.copy(position = i) }
        if (renumbered != list) dao.updateAll(renumbered)
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

    fun observeSettings(): Flow<UserSettingsEntity> = db.settingsDao().observe().map { it ?: UserSettingsEntity() }

    suspend fun setTheme(mode: ThemeMode) = update { it.copy(themeMode = mode.name) }

    suspend fun getTheme(): ThemeMode = ThemeMode.parse(db.settingsDao().get()?.themeMode)

    suspend fun setAppearance(themeId: String, customColors: String?) = update { it.copy(themeId = themeId, customColors = customColors) }

    suspend fun get(): UserSettingsEntity = db.settingsDao().get() ?: UserSettingsEntity()

    private suspend fun update(transform: (UserSettingsEntity) -> UserSettingsEntity) = db.withTransaction {
        val current = db.settingsDao().get() ?: UserSettingsEntity()
        db.settingsDao().save(transform(current))
    }
}
