package com.example.homemenu.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {
    @Query("SELECT * FROM products ORDER BY name COLLATE NOCASE")
    fun observeAll(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products")
    suspend fun getAll(): List<ProductEntity>

    @Query("SELECT * FROM products WHERE name = :name LIMIT 1")
    suspend fun findByName(name: String): ProductEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(items: List<ProductEntity>): List<Long>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(item: ProductEntity): Long

    @Query("SELECT COUNT(*) FROM products")
    suspend fun count(): Int
}

@Dao
interface TagDao {
    @Query("SELECT * FROM tags ORDER BY isCustom, id")
    fun observeAll(): Flow<List<TagEntity>>

    @Query("SELECT * FROM tags")
    suspend fun getAll(): List<TagEntity>

    @Query("SELECT * FROM tags WHERE name = :name LIMIT 1")
    suspend fun findByName(name: String): TagEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(items: List<TagEntity>): List<Long>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(item: TagEntity): Long
}

@Dao
interface RecipeDao {
    @Transaction
    @Query("SELECT * FROM recipes")
    fun observeAll(): Flow<List<RecipeFull>>

    @Transaction
    @Query("SELECT * FROM recipes")
    suspend fun getAll(): List<RecipeFull>

    @Transaction
    @Query("SELECT * FROM recipes WHERE id = :id")
    fun observe(id: Long): Flow<RecipeFull?>

    @Transaction
    @Query("SELECT * FROM recipes WHERE id = :id")
    suspend fun get(id: Long): RecipeFull?

    @Query("SELECT COUNT(*) FROM recipes")
    suspend fun count(): Int

    @Insert
    suspend fun insertRecipe(recipe: RecipeEntity): Long

    @Update
    suspend fun updateRecipe(recipe: RecipeEntity)

    @Query("DELETE FROM recipes WHERE id = :id")
    suspend fun deleteRecipe(id: Long)

    @Insert
    suspend fun insertIngredients(items: List<IngredientEntity>)

    @Insert
    suspend fun insertSteps(items: List<StepEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTagRefs(items: List<RecipeTagCrossRef>)

    @Query("DELETE FROM ingredients WHERE recipeId = :recipeId")
    suspend fun clearIngredients(recipeId: Long)

    @Query("DELETE FROM steps WHERE recipeId = :recipeId")
    suspend fun clearSteps(recipeId: Long)

    @Query("DELETE FROM recipe_tags WHERE recipeId = :recipeId")
    suspend fun clearTags(recipeId: Long)
}

@Dao
interface MenuDao {
    @Query("SELECT * FROM menu_entries WHERE date BETWEEN :from AND :to ORDER BY date")
    fun observeRange(from: String, to: String): Flow<List<MenuEntryEntity>>

    @Query("SELECT * FROM menu_entries WHERE date BETWEEN :from AND :to")
    suspend fun getRange(from: String, to: String): List<MenuEntryEntity>

    @Query("SELECT * FROM menu_entries")
    suspend fun getAll(): List<MenuEntryEntity>

    @Query("SELECT * FROM menu_entries WHERE date = :date AND slot = :slot LIMIT 1")
    suspend fun find(date: String, slot: String): MenuEntryEntity?

    @Query("SELECT * FROM menu_entries WHERE id = :id")
    suspend fun get(id: Long): MenuEntryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: MenuEntryEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAllIgnore(entries: List<MenuEntryEntity>)

    @Update
    suspend fun update(entry: MenuEntryEntity)

    @Query("DELETE FROM menu_entries WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT COUNT(*) FROM menu_entries WHERE recipeId = :recipeId")
    suspend fun countForRecipe(recipeId: Long): Int

    @Query("DELETE FROM menu_entries WHERE recipeId = :recipeId")
    suspend fun deleteForRecipe(recipeId: Long)
}

@Dao
interface ShoppingDao {
    @Query("SELECT `key` FROM shopping_checks WHERE `key` LIKE :prefix || '%'")
    fun observeChecked(prefix: String): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun check(item: ShoppingCheckEntity)

    @Query("DELETE FROM shopping_checks WHERE `key` = :key")
    suspend fun uncheck(key: String)

    @Query("DELETE FROM shopping_checks WHERE `key` LIKE :prefix || '%'")
    suspend fun clear(prefix: String)
}

@Dao
interface SettingsDao {
    @Query("SELECT * FROM user_settings WHERE id = 0")
    fun observe(): Flow<UserSettingsEntity?>

    @Query("SELECT * FROM user_settings WHERE id = 0")
    suspend fun get(): UserSettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(settings: UserSettingsEntity)
}
