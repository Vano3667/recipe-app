package com.example.homemenu.data.files

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.room.withTransaction
import com.example.homemenu.data.db.AppDatabase
import com.example.homemenu.data.db.IngredientEntity
import com.example.homemenu.data.db.MenuEntryEntity
import com.example.homemenu.data.db.ProductEntity
import com.example.homemenu.data.db.RecipeEntity
import com.example.homemenu.data.db.RecipeTagCrossRef
import com.example.homemenu.data.db.StepEntity
import com.example.homemenu.data.db.TagEntity
import com.example.homemenu.data.db.UserSettingsEntity
import com.example.homemenu.data.toDomain
import com.example.homemenu.domain.backup.BackupData
import com.example.homemenu.domain.backup.BackupIngredient
import com.example.homemenu.domain.backup.BackupJson
import com.example.homemenu.domain.backup.BackupMenuEntry
import com.example.homemenu.domain.backup.BackupProduct
import com.example.homemenu.domain.backup.BackupRecipe
import com.example.homemenu.domain.backup.BackupSettings
import com.example.homemenu.domain.backup.BackupStep
import com.example.homemenu.domain.backup.BackupTag
import com.example.homemenu.domain.backup.IncompatibleBackupException
import com.example.homemenu.domain.model.Department
import com.example.homemenu.domain.model.MeasureUnit
import com.example.homemenu.domain.model.Recipe
import com.example.homemenu.domain.model.ThemeMode
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton

data class ImportResult(val recipes: Int, val photos: Int, val menuEntries: Int)

/** Converts between domain recipes and the portable backup model. Pure — unit-tested. */
object BackupMapper {
    fun toBackup(r: Recipe) = BackupRecipe(
        id = r.id, name = r.name, photo = r.photoFile, cookingTimeMin = r.cookingTimeMin,
        difficulty = r.difficulty.name, category = r.category.name, cuisine = r.cuisine, calories = r.calories,
        servings = r.servings,
        ingredients = r.ingredients.map {
            BackupIngredient(it.product.name, it.product.baseUnit.name, it.product.department.name, it.amount, it.unit.name)
        },
        steps = r.steps.map { BackupStep(it.text, it.photoFile) },
        notes = r.notes, tags = r.tags.map { it.name }, createdAt = r.createdAt,
    )
}

@Singleton
class BackupManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val db: AppDatabase,
    private val photos: PhotoStorage,
) {

    suspend fun buildFullBackup(): BackupData {
        val recipes = db.recipeDao().getAll().map { it.toDomain() }
        return BackupData(
            exportedAt = System.currentTimeMillis(),
            products = db.productDao().getAll().filter { it.isCustom }.map { BackupProduct(it.name, it.baseUnit, it.department) },
            tags = db.tagDao().getAll().filter { it.isCustom }.map { BackupTag(it.name, it.colorHint) },
            recipes = recipes.map(BackupMapper::toBackup),
            menu = db.menuDao().getAll().map { BackupMenuEntry(it.date, it.slot, it.recipeId, it.servings) },
            settings = BackupSettings(ThemeMode.parse(db.settingsDao().get()?.themeMode).name),
        )
    }

    suspend fun exportTo(uri: Uri) = withContext(Dispatchers.IO) {
        val data = buildFullBackup()
        val out = context.contentResolver.openOutputStream(uri, "w") ?: error("Не удалось создать файл")
        out.use { writeZip(it, data) }
    }

    /** Single recipe as an importable .zip, shared through FileProvider. */
    suspend fun recipeShareFile(recipe: Recipe): Uri = withContext(Dispatchers.IO) {
        val dir = File(context.cacheDir, "share").apply { mkdirs() }
        val safe = recipe.name.replace(Regex("[^\\p{L}\\p{N}]+"), "_").take(40).ifBlank { "recipe" }
        val file = File(dir, "$safe.zip")
        file.outputStream().use { writeZip(it, BackupData(exportedAt = System.currentTimeMillis(), recipes = listOf(BackupMapper.toBackup(recipe)))) }
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    private fun writeZip(output: OutputStream, data: BackupData) {
        ZipOutputStream(output.buffered()).use { zip ->
            zip.putNextEntry(ZipEntry("recipes.json"))
            zip.write(BackupJson.encode(data).toByteArray(Charsets.UTF_8))
            zip.closeEntry()
            val names = data.recipes.flatMap { r -> listOfNotNull(r.photo) + r.steps.mapNotNull { it.photo } }.toSet()
            for (name in names) {
                val f = photos.fileFor(name) ?: continue
                zip.putNextEntry(ZipEntry("photos/$name"))
                f.inputStream().use { it.copyTo(zip) }
                zip.closeEntry()
            }
        }
    }

    suspend fun importFrom(uri: Uri): ImportResult = withContext(Dispatchers.IO) {
        val input = context.contentResolver.openInputStream(uri) ?: error("Не удалось открыть файл")
        input.use { importZip(it) }
    }

    /**
     * Reads the zip into a temp dir first, validates the JSON, then merges inside ONE Room transaction
     * (any failure → full rollback, already-copied photos are removed).
     */
    suspend fun importZip(input: InputStream): ImportResult {
        val tmp = File(context.cacheDir, "import_${System.currentTimeMillis()}").apply { mkdirs() }
        val copiedPhotos = mutableListOf<File>()
        try {
            var json: String? = null
            ZipInputStream(input.buffered()).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    val name = entry.name
                    if (!entry.isDirectory && !name.contains("..")) {
                        if (name == "recipes.json") json = zip.readBytes().toString(Charsets.UTF_8)
                        else if (name.startsWith("photos/")) {
                            val target = File(tmp, name.removePrefix("photos/").substringAfterLast('/'))
                            target.outputStream().use { zip.copyTo(it) }
                        }
                    }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }
            val data = BackupJson.decode(json ?: throw IncompatibleBackupException("В архиве нет recipes.json"))

            // Photos (optional): copy with fresh names so nothing is overwritten.
            val photoMap = HashMap<String, String>()
            tmp.listFiles()?.forEach { f ->
                val newName = "${UUID.randomUUID()}.jpg"
                val dest = File(photos.photosDir, newName)
                f.copyTo(dest)
                copiedPhotos += dest
                photoMap[f.name] = newName
            }

            var menuCount = 0
            db.withTransaction {
                val productDao = db.productDao()
                val tagDao = db.tagDao()
                for (p in data.products) {
                    productDao.insertAll(listOf(ProductEntity(name = p.name, baseUnit = MeasureUnit.parse(p.unit).name, department = Department.parse(p.department).name, isCustom = true)))
                }
                for (t in data.tags) tagDao.insertAll(listOf(TagEntity(name = t.name, colorHint = t.color, isCustom = true)))

                val idMap = HashMap<Long, Long>()
                for (r in data.recipes) {
                    // Always a new id → no id conflicts with existing rows.
                    val newId = db.recipeDao().insertRecipe(
                        RecipeEntity(
                            name = r.name, photoFile = r.photo?.let { photoMap[it] }, cookingTimeMin = r.cookingTimeMin,
                            difficulty = r.difficulty, category = r.category, cuisine = r.cuisine, calories = r.calories,
                            servings = r.servings.coerceAtLeast(1), notes = r.notes,
                            createdAt = if (r.createdAt > 0) r.createdAt else System.currentTimeMillis(),
                        ),
                    )
                    idMap[r.id] = newId
                    val ingredients = r.ingredients.mapIndexed { i, ing ->
                        val product = productDao.findByName(ing.product) ?: run {
                            val pid = productDao.insert(ProductEntity(name = ing.product, baseUnit = MeasureUnit.parse(ing.productUnit).name, department = Department.parse(ing.department).name, isCustom = true))
                            ProductEntity(pid, ing.product, ing.productUnit, ing.department, true)
                        }
                        IngredientEntity(recipeId = newId, productId = product.id, amount = ing.amount, unit = MeasureUnit.parse(ing.unit).name, position = i)
                    }
                    db.recipeDao().insertIngredients(ingredients)
                    db.recipeDao().insertSteps(r.steps.mapIndexed { i, s -> StepEntity(recipeId = newId, position = i, text = s.text, photoFile = s.photo?.let { photoMap[it] }) })
                    val tagRefs = r.tags.map { name ->
                        val tag = tagDao.findByName(name) ?: TagEntity(tagDao.insert(TagEntity(name = name, colorHint = "cream", isCustom = true)), name, "cream", true)
                        RecipeTagCrossRef(newId, tag.id)
                    }
                    db.recipeDao().insertTagRefs(tagRefs)
                }
                // Menu: keep existing slots (IGNORE on conflict), remap recipe ids.
                val entries = data.menu.mapNotNull { m ->
                    idMap[m.recipeId]?.let { MenuEntryEntity(date = m.date, slot = m.slot, recipeId = it, servings = m.servings.coerceAtLeast(1)) }
                }
                db.menuDao().insertAllIgnore(entries)
                menuCount = entries.size
                data.settings?.let { s ->
                    val current = db.settingsDao().get() ?: UserSettingsEntity()
                    db.settingsDao().save(current.copy(themeMode = ThemeMode.parse(s.themeMode).name))
                }
            }
            return ImportResult(data.recipes.size, photoMap.size, menuCount)
        } catch (e: Exception) {
            copiedPhotos.forEach { it.delete() }
            throw e
        } finally {
            tmp.deleteRecursively()
        }
    }
}
