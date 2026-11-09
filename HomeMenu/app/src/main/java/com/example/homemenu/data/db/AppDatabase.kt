package com.example.homemenu.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration

@Database(
    entities = [
        ProductEntity::class, TagEntity::class, RecipeEntity::class, IngredientEntity::class,
        StepEntity::class, RecipeTagCrossRef::class, MenuEntryEntity::class,
        ShoppingCheckEntity::class, UserSettingsEntity::class,
    ],
    version = AppDatabase.VERSION,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
    abstract fun tagDao(): TagDao
    abstract fun recipeDao(): RecipeDao
    abstract fun menuDao(): MenuDao
    abstract fun shoppingDao(): ShoppingDao
    abstract fun settingsDao(): SettingsDao

    companion object {
        const val VERSION = 1
        const val NAME = "home_menu.db"

        /**
         * Explicit migrations only (no destructive fallback, user data is precious).
         * Add `Migration(1, 2) { db -> db.execSQL(...) }` here when the schema changes;
         * exported schemas live in app/schemas for migration tests.
         */
        val MIGRATIONS: Array<Migration> = emptyArray()
    }
}
