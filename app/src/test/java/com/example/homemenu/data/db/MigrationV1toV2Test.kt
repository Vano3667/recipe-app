package com.example.homemenu.data.db

import android.app.Application
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.io.File

/**
 * DB v1 -> v2 upgrade on a real SQLite file (F4 / Task 3 / Task 8).
 *
 * No exported v1 Room schema exists in the repository, so the v1 database is created here with the exact
 * SQL that Room 2.6 generates for the v1 entities (`Entities.kt` @ 87a602d). Room then opens it with
 * [AppDatabase.MIGRATIONS]; after migrating, Room itself validates every table, column, default value,
 * index and foreign key against the v2 entities and throws if anything differs, so a successful open is
 * already a full schema check. The tests then read the data back through the DAOs.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], application = Application::class)
class MigrationV1toV2Test {

    private lateinit var context: Context
    private val opened = mutableListOf<AppDatabase>()

    @Before fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @After fun tearDown() {
        opened.forEach { runCatching { it.close() } }
    }

    // ---------------------------------------------------------------------------------------------
    // v1 fixture
    // ---------------------------------------------------------------------------------------------

    private val v1Schema = listOf(
        "CREATE TABLE IF NOT EXISTS `products` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `baseUnit` TEXT NOT NULL, `department` TEXT NOT NULL, `isCustom` INTEGER NOT NULL)",
        "CREATE UNIQUE INDEX IF NOT EXISTS `index_products_name` ON `products` (`name`)",
        "CREATE TABLE IF NOT EXISTS `tags` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `colorHint` TEXT NOT NULL, `isCustom` INTEGER NOT NULL)",
        "CREATE UNIQUE INDEX IF NOT EXISTS `index_tags_name` ON `tags` (`name`)",
        "CREATE TABLE IF NOT EXISTS `recipes` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `photoFile` TEXT, `cookingTimeMin` INTEGER NOT NULL, `difficulty` TEXT NOT NULL, `category` TEXT NOT NULL, `cuisine` TEXT NOT NULL, `calories` INTEGER, `servings` INTEGER NOT NULL, `notes` TEXT NOT NULL, `createdAt` INTEGER NOT NULL)",
        "CREATE TABLE IF NOT EXISTS `ingredients` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `recipeId` INTEGER NOT NULL, `productId` INTEGER NOT NULL, `amount` REAL NOT NULL, `unit` TEXT NOT NULL, `position` INTEGER NOT NULL, FOREIGN KEY(`recipeId`) REFERENCES `recipes`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , FOREIGN KEY(`productId`) REFERENCES `products`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT )",
        "CREATE INDEX IF NOT EXISTS `index_ingredients_recipeId` ON `ingredients` (`recipeId`)",
        "CREATE INDEX IF NOT EXISTS `index_ingredients_productId` ON `ingredients` (`productId`)",
        "CREATE TABLE IF NOT EXISTS `steps` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `recipeId` INTEGER NOT NULL, `position` INTEGER NOT NULL, `text` TEXT NOT NULL, `photoFile` TEXT, FOREIGN KEY(`recipeId`) REFERENCES `recipes`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
        "CREATE INDEX IF NOT EXISTS `index_steps_recipeId` ON `steps` (`recipeId`)",
        "CREATE TABLE IF NOT EXISTS `recipe_tags` (`recipeId` INTEGER NOT NULL, `tagId` INTEGER NOT NULL, PRIMARY KEY(`recipeId`, `tagId`), FOREIGN KEY(`recipeId`) REFERENCES `recipes`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , FOREIGN KEY(`tagId`) REFERENCES `tags`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
        "CREATE INDEX IF NOT EXISTS `index_recipe_tags_tagId` ON `recipe_tags` (`tagId`)",
        "CREATE TABLE IF NOT EXISTS `menu_entries` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `date` TEXT NOT NULL, `slot` TEXT NOT NULL, `recipeId` INTEGER NOT NULL, `servings` INTEGER NOT NULL)",
        "CREATE UNIQUE INDEX IF NOT EXISTS `index_menu_entries_date_slot` ON `menu_entries` (`date`, `slot`)",
        "CREATE INDEX IF NOT EXISTS `index_menu_entries_recipeId` ON `menu_entries` (`recipeId`)",
        "CREATE TABLE IF NOT EXISTS `shopping_checks` (`key` TEXT NOT NULL, PRIMARY KEY(`key`))",
        "CREATE TABLE IF NOT EXISTS `user_settings` (`id` INTEGER NOT NULL, `themeMode` TEXT NOT NULL, `demoSeeded` INTEGER NOT NULL, PRIMARY KEY(`id`))",
        "CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)",
        "INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, 'v1-test-fixture')",
    )

    private val v1Data = listOf(
        "INSERT INTO products (id, name, baseUnit, department, isCustom) VALUES (1, 'Молоко', 'ML', 'DAIRY', 0)",
        "INSERT INTO products (id, name, baseUnit, department, isCustom) VALUES (2, 'Мука', 'G', 'GROCERY', 1)",
        "INSERT INTO tags (id, name, colorHint, isCustom) VALUES (1, 'Быстро', 'green', 0)",
        "INSERT INTO tags (id, name, colorHint, isCustom) VALUES (2, 'Моё', 'blue', 1)",
        "INSERT INTO recipes (id, name, photoFile, cookingTimeMin, difficulty, category, cuisine, calories, servings, notes, createdAt) " +
            "VALUES (1, 'Блины', 'p1.jpg', 30, 'MEDIUM', 'BREAKFAST', 'Русская', 250, 4, 'Заметка', 1700000000000)",
        "INSERT INTO recipes (id, name, photoFile, cookingTimeMin, difficulty, category, cuisine, calories, servings, notes, createdAt) " +
            "VALUES (2, 'Каша', NULL, 15, 'EASY', 'BREAKFAST', '', NULL, 2, '', 1700000001000)",
        "INSERT INTO ingredients (id, recipeId, productId, amount, unit, position) VALUES (1, 1, 2, 200.0, 'G', 1)",
        "INSERT INTO ingredients (id, recipeId, productId, amount, unit, position) VALUES (2, 1, 1, 500.0, 'ML', 0)",
        "INSERT INTO steps (id, recipeId, position, text, photoFile) VALUES (1, 1, 0, 'Смешать', NULL)",
        "INSERT INTO steps (id, recipeId, position, text, photoFile) VALUES (2, 1, 1, 'Жарить', 's1.jpg')",
        "INSERT INTO recipe_tags (recipeId, tagId) VALUES (1, 1)",
        "INSERT INTO recipe_tags (recipeId, tagId) VALUES (1, 2)",
        "INSERT INTO menu_entries (id, date, slot, recipeId, servings) VALUES (1, '2026-11-16', 'BREAKFAST', 1, 3)",
        "INSERT INTO menu_entries (id, date, slot, recipeId, servings) VALUES (2, '2026-11-16', 'DINNER', 2, 2)",
        "INSERT INTO shopping_checks (`key`) VALUES ('2026-W47|1|ML')",
        "INSERT INTO user_settings (id, themeMode, demoSeeded) VALUES (0, 'DARK', 1)",
    )

    private fun createV1(name: String): File {
        val file = context.getDatabasePath(name)
        file.parentFile?.mkdirs()
        file.delete()
        SQLiteDatabase.openOrCreateDatabase(file, null).use { db ->
            db.beginTransaction()
            try {
                (v1Schema + v1Data).forEach(db::execSQL)
                db.setTransactionSuccessful()
            } finally {
                db.endTransaction()
            }
            db.version = 1
        }
        return file
    }

    private fun open(name: String, migrations: Array<out Migration> = AppDatabase.MIGRATIONS): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, name)
            .addMigrations(*migrations)
            .allowMainThreadQueries()
            .setQueryExecutor { it.run() }
            .setTransactionExecutor { it.run() }
            .build()
            .also { opened += it }

    private fun SQLiteDatabase.columns(table: String): List<String> =
        rawQuery("PRAGMA table_info(`$table`)", null).use { c ->
            buildList { while (c.moveToNext()) add(c.getString(c.getColumnIndexOrThrow("name"))) }
        }

    private fun SQLiteDatabase.count(table: String): Int =
        rawQuery("SELECT COUNT(*) FROM `$table`", null).use { it.moveToFirst(); it.getInt(0) }

    // ---------------------------------------------------------------------------------------------
    // Tests
    // ---------------------------------------------------------------------------------------------

    @Test fun migration_1_2_passesRoomSchemaValidation_andKeepsAllData() = runBlocking<Unit> {
        createV1("mig_keep.db")
        val db = open("mig_keep.db")
        // Forces onUpgrade: migration + Room's schema validation against the v2 entities.
        assertThat(db.openHelper.writableDatabase.version).isEqualTo(AppDatabase.VERSION)

        assertThat(db.productDao().count()).isEqualTo(2)
        assertThat(db.tagDao().getAll().map { it.name }).containsExactly("Быстро", "Моё")
        assertThat(db.recipeDao().count()).isEqualTo(2)

        val blini = db.recipeDao().get(1)!!
        with(blini.recipe) {
            assertThat(name).isEqualTo("Блины")
            assertThat(photoFile).isEqualTo("p1.jpg")
            assertThat(cookingTimeMin).isEqualTo(30)
            assertThat(difficulty).isEqualTo("MEDIUM")
            assertThat(category).isEqualTo("BREAKFAST")
            assertThat(calories).isEqualTo(250)
            assertThat(servings).isEqualTo(4)
            assertThat(notes).isEqualTo("Заметка")
            assertThat(createdAt).isEqualTo(1_700_000_000_000L)
            // Legacy cuisine value is preserved in the DB even though the UI no longer shows it.
            assertThat(cuisine).isEqualTo("Русская")
            // New v2 columns get their defaults.
            assertThat(description).isEmpty()
            assertThat(prepTimeMin).isEqualTo(0)
            assertThat(ovenTempC).isNull()
        }
        val ingredients = blini.ingredients.sortedBy { it.ingredient.position }
        assertThat(ingredients.map { it.product.name }).containsExactly("Молоко", "Мука").inOrder()
        assertThat(ingredients.map { it.ingredient.amount }).containsExactly(500.0, 200.0).inOrder()
        assertThat(ingredients.map { it.ingredient.unit }).containsExactly("ML", "G").inOrder()
        assertThat(blini.steps.sortedBy { it.position }.map { it.text }).containsExactly("Смешать", "Жарить").inOrder()
        assertThat(blini.steps.first { it.position == 1 }.photoFile).isEqualTo("s1.jpg")
        assertThat(blini.tags.map { it.name }).containsExactly("Быстро", "Моё")

        // Every v1 meal becomes a meal with one dish at position 0.
        val menu = db.menuDao().getAll()
        assertThat(menu).hasSize(2)
        assertThat(menu.map { it.position }.distinct()).containsExactly(0)
        assertThat(db.menuDao().getSlot("2026-11-16", "BREAKFAST").single().let { it.recipeId to it.servings })
            .isEqualTo(1L to 3)

        val settings = db.settingsDao().get()!!
        assertThat(settings.themeMode).isEqualTo("DARK")
        assertThat(settings.demoSeeded).isTrue()
        assertThat(settings.themeId).isEqualTo("LEGACY")
        assertThat(settings.customColors).isNull()

        db.openHelper.readableDatabase.query("SELECT `key` FROM shopping_checks").use { c ->
            assertThat(c.count).isEqualTo(1)
            c.moveToFirst()
            assertThat(c.getString(0)).isEqualTo("2026-W47|1|ML")
        }
    }

    @Test fun afterMigration_aMealAcceptsSeveralDishes() = runBlocking<Unit> {
        createV1("mig_multi.db")
        val db = open("mig_multi.db")
        // Second dish in the same (date, slot): the v1 unique index would have rejected it.
        db.menuDao().insert(MenuEntryEntity(date = "2026-11-16", slot = "BREAKFAST", recipeId = 2, servings = 1, position = 1))
        val slot = db.menuDao().getSlot("2026-11-16", "BREAKFAST")
        assertThat(slot.map { it.recipeId }).containsExactly(1L, 2L).inOrder()
    }

    @Test fun failedMigration_leavesV1DatabaseUntouched() {
        val file = createV1("mig_fail.db")
        val broken = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `recipes` ADD COLUMN `description` TEXT NOT NULL DEFAULT ''")
                error("simulated migration failure")
            }
        }
        val db = open("mig_fail.db", arrayOf(broken))
        val failure = runCatching { db.openHelper.writableDatabase }.exceptionOrNull()
        assertThat(failure).isNotNull()
        db.close()

        SQLiteDatabase.openDatabase(file.path, null, SQLiteDatabase.OPEN_READONLY).use { raw ->
            assertThat(raw.version).isEqualTo(1)
            assertThat(raw.columns("recipes")).doesNotContain("description")
            assertThat(raw.count("recipes")).isEqualTo(2)
            assertThat(raw.count("ingredients")).isEqualTo(2)
            assertThat(raw.count("menu_entries")).isEqualTo(2)
        }

        // The untouched v1 file still upgrades normally with the real migration afterwards.
        val retry = open("mig_fail.db")
        assertThat(retry.openHelper.writableDatabase.version).isEqualTo(AppDatabase.VERSION)
        assertThat(runBlocking { retry.recipeDao().count() }).isEqualTo(2)
    }

    @Test fun preMigrationBackup_copiesV1File_andSkipsCurrentVersion() {
        val name = "mig_backup.db"
        createV1(name)
        val backupDir = File(context.filesDir, DatabaseSafety.BACKUP_DIR)
        backupDir.deleteRecursively()

        val backup = DatabaseSafety.backupBeforeMigration(context, name, AppDatabase.VERSION)
        assertThat(backup).isNotNull()
        assertThat(backup!!.exists()).isTrue()
        SQLiteDatabase.openDatabase(backup.path, null, SQLiteDatabase.OPEN_READONLY).use { raw ->
            assertThat(raw.version).isEqualTo(1)
            assertThat(raw.count("recipes")).isEqualTo(2)
            assertThat(raw.count("menu_entries")).isEqualTo(2)
        }

        // Once upgraded, no further backup is taken for the same version.
        val db = open(name)
        db.openHelper.writableDatabase
        db.close()
        assertThat(DatabaseSafety.backupBeforeMigration(context, name, AppDatabase.VERSION)).isNull()
    }

    @Test fun freshInstall_createsCurrentSchema() = runBlocking<Unit> {
        context.getDatabasePath("fresh.db").delete()
        val db = open("fresh.db")
        assertThat(db.openHelper.writableDatabase.version).isEqualTo(AppDatabase.VERSION)
        assertThat(db.recipeDao().count()).isEqualTo(0)
    }
}
