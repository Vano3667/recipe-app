package com.example.homemenu.data.files

import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.homemenu.data.db.AppDatabase
import com.example.homemenu.data.db.RecipeEntity
import com.example.homemenu.data.toDomain
import com.example.homemenu.domain.UserFixture
import com.example.homemenu.domain.backup.BackupData
import com.example.homemenu.domain.backup.BackupJson
import com.example.homemenu.domain.backup.DuplicateMode
import com.example.homemenu.domain.model.MeasureUnit
import com.example.homemenu.domain.model.Recipe
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.OutputStream

/**
 * Task 6 / user checks 23.2 and 23.7: the user's real JSON file goes through the real import path
 * (BackupManager.stageStream -> commit) into a Room database, and the database content is checked.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], application = Application::class)
class FixtureImportDatabaseTest {

    private lateinit var context: Context
    private val dbs = mutableListOf<AppDatabase>()

    @Before fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @After fun tearDown() {
        dbs.forEach { runCatching { it.close() } }
    }

    private fun newDb(): AppDatabase =
        Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build().also { dbs += it }

    private fun manager(db: AppDatabase) = BackupManager(context, db, PhotoStorage(context))

    private suspend fun importFixture(db: AppDatabase) =
        manager(db).let { m -> m.commit(UserFixture.file().inputStream().use { m.stageStream(it) }, DuplicateMode.SKIP) }

    private suspend fun recipes(db: AppDatabase): List<Recipe> = db.recipeDao().getAll().map { it.toDomain() }.sortedBy { it.id }

    @Test fun `fixture is imported into the database and verified`() = runBlocking {
        val db = newDb()
        val report = importFixture(db)
        assertThat(report.failed).isEmpty()
        assertThat(report.verified).isTrue()
        assertThat(report.added).isEqualTo(UserFixture.RECIPES)

        val all = recipes(db)
        assertThat(all).hasSize(UserFixture.RECIPES)
        assertThat(all.sumOf { it.ingredients.size }).isEqualTo(UserFixture.INGREDIENTS)
        assertThat(all.sumOf { it.steps.size }).isEqualTo(UserFixture.STEPS)

        val first = all.first { it.name == "Сырники" }
        assertThat(first.ingredients.map { it.product.name })
            .containsExactly("яйцо", "рисовой муки", "брикетного творога 5%", "масла гхи", "подсластитель по вкусу").inOrder()
        assertThat(first.ingredients.map { it.amount }).containsExactly(1.0, 30.0, 180.0, 2.0, 0.0).inOrder()
        assertThat(first.ingredients.map { it.unit })
            .containsExactly(MeasureUnit.PCS, MeasureUnit.G, MeasureUnit.G, MeasureUnit.G, MeasureUnit.TO_TASTE).inOrder()
        assertThat(first.calories).isEqualTo(443)
        assertThat(first.steps).hasSize(6)
        assertThat(first.tags.map { it.name }).containsExactly("Неделя 1", "Понедельник", "Завтрак")

        val last = all.first { it.name == "Индейка, тушеная в сливках с брокколи" }
        assertThat(last.ingredients.map { it.product.name })
            .containsExactly("филе грудки индейки", "лука", "брокколи", "масла гхи", "соль", "специи по вкусу", "сливок 10%").inOrder()

        val tagNames = db.tagDao().getAll().map { it.name }
        assertThat(tagNames).containsAtLeast("Неделя 1", "Неделя 4", "Завтрак", "Второй завтрак", "Обед", "Полдник", "Ужин", "Воскресенье")
        assertThat(all.count { r -> r.tags.any { it.name == "Неделя 2" } }).isEqualTo(35)
    }

    @Test fun `second import with skip adds nothing and loses nothing`() = runBlocking {
        val db = newDb()
        importFixture(db)
        val before = recipes(db)
        val report = importFixture(db)
        assertThat(report.added).isEqualTo(0)
        assertThat(report.skipped).isEqualTo(UserFixture.RECIPES)
        val after = recipes(db)
        assertThat(after).hasSize(before.size)
        assertThat(after.sumOf { it.ingredients.size }).isEqualTo(UserFixture.INGREDIENTS)
    }

    @Test fun `json export then import gives the same recipes`() = runBlocking {
        val source = newDb()
        importFixture(source)
        val json = BackupJson.encode(manager(source).buildFullBackup(includeImages = true))

        val target = newDb()
        val m = manager(target)
        val report = m.commit(m.stageStream(ByteArrayInputStream(json.toByteArray(Charsets.UTF_8))), DuplicateMode.SKIP)
        assertThat(report.verified).isTrue()
        assertThat(snapshot(recipes(target))).isEqualTo(snapshot(recipes(source)))
    }

    @Test fun `zip export with a photo then import keeps the photo`() = runBlocking {
        val source = newDb()
        importFixture(source)
        val photos = PhotoStorage(context)
        val jpeg = byteArrayOf(
            0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE0.toByte(), 0x00, 0x10, 0x4A, 0x46, 0x49, 0x46, 0x00, 0x01,
            0x01, 0x00, 0x00, 0x01, 0x00, 0x01, 0x00, 0x00, 0xFF.toByte(), 0xD9.toByte(),
        )
        File(photos.photosDir, "fixture_photo.jpg").writeBytes(jpeg)
        val firstId = source.recipeDao().getNames().minOf { it.id }
        val entity: RecipeEntity = source.recipeDao().getEntity(firstId)!!
        source.recipeDao().updateRecipe(entity.copy(photoFile = "fixture_photo.jpg"))

        val sourceManager = manager(source)
        val data = sourceManager.buildFullBackup(includeImages = false)
        val zip = ByteArrayOutputStream()
        BackupManager::class.java.getDeclaredMethod("writeZip", OutputStream::class.java, BackupData::class.java)
            .apply { isAccessible = true }
            .invoke(sourceManager, zip, data)

        val target = newDb()
        val m = manager(target)
        val report = m.commit(m.stageStream(ByteArrayInputStream(zip.toByteArray())), DuplicateMode.SKIP)
        assertThat(report.verified).isTrue()
        assertThat(report.photosRestored).isEqualTo(1)
        val imported = recipes(target)
        assertThat(snapshot(imported)).isEqualTo(snapshot(recipes(source)))
        val withPhoto = imported.filter { it.photoFile != null }
        assertThat(withPhoto).hasSize(1)
        assertThat(withPhoto.single().name).isEqualTo(entity.name)
        assertThat(photos.fileFor(withPhoto.single().photoFile)!!.readBytes()).isEqualTo(jpeg)
    }

    /** Everything that must survive a round trip, independent of database ids. */
    private fun snapshot(list: List<Recipe>) = list.sortedBy { it.createdAt * 1000 + it.id }.map { r ->
        listOf(
            r.name, r.description, r.cookingTimeMin, r.prepTimeMin, r.ovenTempC, r.difficulty, r.category, r.calories,
            r.servings, r.notes, r.createdAt,
            r.ingredients.map { Triple(it.product.name, it.amount, it.unit) },
            r.steps.map { it.text },
            r.tags.map { it.name }.sorted(),
        )
    }
}
