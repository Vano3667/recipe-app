package com.example.homemenu.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

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
        const val VERSION = 2
        const val NAME = "home_menu.db"

        /**
         * v1 → v2. Only additive changes (ALTER TABLE ADD COLUMN + index swap), no table is dropped or rebuilt:
         *  - recipes: description, prepTimeMin, ovenTempC;
         *  - menu_entries: position; (date, slot) index becomes non-unique → several dishes per meal.
         *    Every existing single-dish meal simply becomes a meal with one dish at position 0;
         *  - user_settings: themeId (LEGACY keeps the v1 look), customColors.
         * Room runs a migration inside a transaction, so a failure leaves the v1 database untouched.
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `recipes` ADD COLUMN `description` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `recipes` ADD COLUMN `prepTimeMin` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `recipes` ADD COLUMN `ovenTempC` INTEGER")
                db.execSQL("ALTER TABLE `menu_entries` ADD COLUMN `position` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("DROP INDEX IF EXISTS `index_menu_entries_date_slot`")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_menu_entries_date_slot` ON `menu_entries` (`date`, `slot`)")
                db.execSQL("ALTER TABLE `user_settings` ADD COLUMN `themeId` TEXT NOT NULL DEFAULT 'LEGACY'")
                db.execSQL("ALTER TABLE `user_settings` ADD COLUMN `customColors` TEXT")
            }
        }

        /** Explicit migrations only (no destructive fallback — user data is precious). */
        val MIGRATIONS: Array<Migration> = arrayOf(MIGRATION_1_2)
    }
}
