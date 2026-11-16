package com.example.homemenu.data.files

import android.content.Context
import android.net.Uri
import android.util.Base64
import android.util.Log
import androidx.core.content.FileProvider
import androidx.room.withTransaction
import com.example.homemenu.BuildConfig
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
import com.example.homemenu.domain.backup.BACKUP_DATA_FILE
import com.example.homemenu.domain.backup.BACKUP_MANIFEST_FILE
import com.example.homemenu.domain.backup.BACKUP_PHOTOS_DIR
import com.example.homemenu.domain.backup.BackupAnalyzer
import com.example.homemenu.domain.backup.BackupData
import com.example.homemenu.domain.backup.BackupIngredient
import com.example.homemenu.domain.backup.BackupJson
import com.example.homemenu.domain.backup.BackupManifest
import com.example.homemenu.domain.backup.BackupMenuEntry
import com.example.homemenu.domain.backup.BackupProduct
import com.example.homemenu.domain.backup.BackupRecipe
import com.example.homemenu.domain.backup.BackupSettings
import com.example.homemenu.domain.backup.BackupStep
import com.example.homemenu.domain.backup.BackupTag
import com.example.homemenu.domain.backup.DuplicateMode
import com.example.homemenu.domain.backup.ImportPreview
import com.example.homemenu.domain.backup.ImportReport
import com.example.homemenu.domain.backup.IncompatibleBackupException
import com.example.homemenu.domain.backup.ManifestFile
import com.example.homemenu.domain.model.Department
import com.example.homemenu.domain.model.MealSlot
import com.example.homemenu.domain.model.MeasureUnit
import com.example.homemenu.domain.model.Recipe
import com.example.homemenu.domain.model.ThemeMode
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.security.MessageDigest
import java.time.LocalDate
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipException
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton

data class ExportResult(val recipes: Int, val photos: Int, val menuEntries: Int)

/**
 * A backup file read into a private staging folder and validated. Nothing has been written to the database yet:
 * the user sees [preview] and either confirms ([BackupManager.commit]) or cancels ([BackupManager.discard]).
 */
class StagedImport internal constructor(
    val data: BackupData,
    val preview: ImportPreview,
    internal val dir: File,
    internal val photos: Map<String, File>,
)

/** Converts between domain recipes and the portable backup model. Pure — unit-tested. */
object BackupMapper {
    fun toBackup(r: Recipe, photoExists: (String) -> Boolean = { true }) = BackupRecipe(
        id = r.id, name = r.name, description = r.description,
        photo = r.photoFile?.takeIf(photoExists),
        cookingTimeMin = r.cookingTimeMin, prepTimeMin = r.prepTimeMin, ovenTempC = r.ovenTempC,
        difficulty = r.difficulty.name, category = r.category.name, calories = r.calories, servings = r.servings,
        ingredients = r.ingredients.map {
            BackupIngredient(it.product.name, it.product.baseUnit.name, it.product.department.name, it.amount, it.unit.name)
        },
        steps = r.steps.map { BackupStep(it.text, it.photoFile?.takeIf(photoExists)) },
        notes = r.notes, tags = r.tags.map { it.name }, createdAt = r.createdAt,
    )
}

@Singleton
class BackupManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val db: AppDatabase,
    private val photos: PhotoStorage,
) {

    // ---------------------------------------------------------------- export

    suspend fun buildFullBackup(includeImages: Boolean = false): BackupData {
        val exists: (String) -> Boolean = { photos.fileFor(it) != null }
        val recipes = db.recipeDao().getAll().map { BackupMapper.toBackup(it.toDomain(), exists) }
        val settings = db.settingsDao().get()
        val data = BackupData(
            appVersion = BuildConfig.VERSION_NAME,
            exportedAt = System.currentTimeMillis(),
            products = db.productDao().getAll().filter { it.isCustom }.map { BackupProduct(it.name, it.baseUnit, it.department) },
            tags = db.tagDao().getAll().map { BackupTag(it.name, it.colorHint, it.isCustom) },
            recipes = recipes,
            menu = db.menuDao().getAll().map { BackupMenuEntry(it.date, it.slot, it.recipeId, it.servings, it.position) },
            settings = BackupSettings(
                themeMode = ThemeMode.parse(settings?.themeMode).name,
                themeId = settings?.themeId,
                customColors = settings?.customColors,
            ),
        )
        if (!includeImages) return data
        val images = BackupAnalyzer.referencedPhotos(data).mapNotNull { name ->
            photos.fileFor(name)?.let { name to Base64.encodeToString(it.readBytes(), Base64.NO_WRAP) }
        }.toMap()
        return data.copy(images = images)
    }

    /** Self-contained ZIP: recipes.json + photos/ + manifest.json (SHA-256 of every file). */
    suspend fun exportZip(uri: Uri): ExportResult = withContext(Dispatchers.IO) {
        val data = buildFullBackup(includeImages = false)
        val out = context.contentResolver.openOutputStream(uri, "w") ?: throw IOException("Не удалось создать файл")
        val photoCount = out.use { writeZip(it, data) }
        ExportResult(data.recipes.size, photoCount, data.menu.size)
    }

    /** Single UTF-8 JSON file; photos are embedded as Base64 so the file restores on any device. */
    suspend fun exportJson(uri: Uri): ExportResult = withContext(Dispatchers.IO) {
        val data = buildFullBackup(includeImages = true)
        val out = context.contentResolver.openOutputStream(uri, "w") ?: throw IOException("Не удалось создать файл")
        out.use { it.write(BackupJson.encode(data).toByteArray(Charsets.UTF_8)) }
        ExportResult(data.recipes.size, data.images.size, data.menu.size)
    }

    /** Single recipe as an importable .zip, shared through FileProvider. */
    suspend fun recipeShareFile(recipe: Recipe): Uri = withContext(Dispatchers.IO) {
        val dir = File(context.cacheDir, "share").apply { mkdirs() }
        val file = File(dir, "${safeFileName(recipe.name)}.zip")
        val exists: (String) -> Boolean = { photos.fileFor(it) != null }
        val data = BackupData(appVersion = BuildConfig.VERSION_NAME, exportedAt = System.currentTimeMillis(), recipes = listOf(BackupMapper.toBackup(recipe, exists)))
        file.outputStream().use { writeZip(it, data) }
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    /** Plain text file (e.g. recipe as TXT). */
    suspend fun writeText(uri: Uri, text: String) = withContext(Dispatchers.IO) {
        val out = context.contentResolver.openOutputStream(uri, "w") ?: throw IOException("Не удалось создать файл")
        out.use { it.write(text.toByteArray(Charsets.UTF_8)) }
    }

    /** Returns the number of photos written. */
    private fun writeZip(output: OutputStream, data: BackupData): Int {
        val files = mutableListOf<ManifestFile>()
        var photoCount = 0
        ZipOutputStream(output.buffered()).use { zip ->
            fun put(path: String, bytes: ByteArray) {
                zip.putNextEntry(ZipEntry(path))
                zip.write(bytes)
                zip.closeEntry()
                files += ManifestFile(path, bytes.size.toLong(), sha256(bytes))
            }
            put(BACKUP_DATA_FILE, BackupJson.encode(data).toByteArray(Charsets.UTF_8))
            for (name in BackupAnalyzer.referencedPhotos(data)) {
                val f = photos.fileFor(name) ?: continue
                put(BACKUP_PHOTOS_DIR + name, f.readBytes())
                photoCount++
            }
            val manifest = BackupManifest(
                appVersion = data.appVersion, exportedAt = data.exportedAt,
                recipes = data.recipes.size, menuEntries = data.menu.size, photos = photoCount, files = files.toList(),
            )
            zip.putNextEntry(ZipEntry(BACKUP_MANIFEST_FILE))
            zip.write(BackupJson.encodeManifest(manifest).toByteArray(Charsets.UTF_8))
            zip.closeEntry()
        }
        return photoCount
    }

    // ---------------------------------------------------------------- import: stage + preview

    /** Reads and validates a ZIP or JSON backup into a staging folder. Does not touch the database. */
    suspend fun stage(uri: Uri): StagedImport = withContext(Dispatchers.IO) {
        val input = try {
            context.contentResolver.openInputStream(uri)
        } catch (e: Exception) {
            null
        } ?: throw IncompatibleBackupException("Не удалось открыть файл. Проверьте доступ к нему.")
        input.use { stageStream(it) }
    }

    suspend fun stageStream(input: InputStream): StagedImport {
        val dir = File(context.cacheDir, "import_${System.currentTimeMillis()}").apply { mkdirs() }
        try {
            val buffered = BufferedInputStream(input, 64 * 1024)
            buffered.mark(16)
            val head = ByteArray(4)
            var read = 0
            while (read < head.size) {
                val n = buffered.read(head, read, head.size - read)
                if (n < 0) break
                read += n
            }
            buffered.reset()
            if (read == 0) throw IncompatibleBackupException("Файл пустой")
            val isZip = read >= 2 && head[0] == 'P'.code.toByte() && head[1] == 'K'.code.toByte()
            return if (isZip) stageZip(buffered, dir) else stageJson(buffered, dir)
        } catch (e: Exception) {
            dir.deleteRecursively()
            throw e
        }
    }

    private suspend fun stageZip(input: InputStream, dir: File): StagedImport {
        val photosDir = File(dir, "photos").apply { mkdirs() }
        var json: ByteArray? = null
        var otherJson: ByteArray? = null
        var manifestBytes: ByteArray? = null
        val hashes = HashMap<String, String>()
        val warnings = mutableListOf<String>()
        var total = 0L
        try {
            ZipInputStream(input).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    val path = entry.name.replace('\\', '/').trimStart('/')
                    if (!entry.isDirectory && !path.contains("../")) {
                        val fileName = path.substringAfterLast('/')
                        when {
                            fileName == BACKUP_MANIFEST_FILE -> manifestBytes = zip.readLimited(MAX_JSON_BYTES)
                            fileName == BACKUP_DATA_FILE && json == null -> {
                                val bytes = zip.readLimited(MAX_JSON_BYTES)
                                json = bytes
                                hashes[BACKUP_DATA_FILE] = sha256(bytes)
                            }
                            fileName.endsWith(".json", ignoreCase = true) && otherJson == null -> otherJson = zip.readLimited(MAX_JSON_BYTES)
                            path.contains(BACKUP_PHOTOS_DIR) -> {
                                val safe = BackupAnalyzer.safePhotoName(fileName)
                                if (safe != null) {
                                    val target = File(photosDir, safe)
                                    val digest = MessageDigest.getInstance("SHA-256")
                                    target.outputStream().use { out -> total += zip.copyLimited(out, MAX_PHOTO_BYTES, digest) }
                                    hashes[BACKUP_PHOTOS_DIR + safe] = digest.digest().toHex()
                                } else warnings += "Пропущен файл с недопустимым именем: $path"
                            }
                        }
                        if (total > MAX_TOTAL_BYTES) throw IncompatibleBackupException("Архив слишком большой")
                    }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }
        } catch (e: ZipException) {
            throw IncompatibleBackupException("Архив повреждён или скачан не полностью")
        }
        val jsonBytes = json ?: otherJson ?: throw IncompatibleBackupException("В архиве нет данных рецептов (recipes.json)")

        manifestBytes?.let { bytes ->
            val manifest = BackupJson.decodeManifest(bytes.toString(Charsets.UTF_8).removePrefix(BOM))
            for (f in manifest.files) {
                val actual = hashes[f.path]
                when {
                    actual == null -> warnings += "В архиве не хватает файла ${f.path}"
                    !actual.equals(f.sha256, ignoreCase = true) ->
                        throw IncompatibleBackupException("Архив повреждён: не совпадает контрольная сумма ${f.path}")
                }
            }
        }
        val data = BackupJson.decode(jsonBytes.toString(Charsets.UTF_8).removePrefix(BOM))
        val available = validatePhotos(photosDir, warnings)
        return finishStage(data, dir, available, "ZIP", warnings)
    }

    private suspend fun stageJson(input: InputStream, dir: File): StagedImport {
        val text = input.readLimited(MAX_TOTAL_BYTES).toString(Charsets.UTF_8).removePrefix(BOM).trim()
        if (!text.startsWith("{")) throw IncompatibleBackupException("Файл не является резервной копией: ожидался ZIP-архив или JSON")
        val data = BackupJson.decode(text)
        val warnings = mutableListOf<String>()
        val photosDir = File(dir, "photos").apply { mkdirs() }
        for ((name, b64) in data.images) {
            val safe = BackupAnalyzer.safePhotoName(name)
            if (safe == null) { warnings += "Пропущено фото с недопустимым именем"; continue }
            val bytes = try { Base64.decode(b64, Base64.DEFAULT) } catch (e: IllegalArgumentException) { null }
            if (bytes == null || bytes.isEmpty()) { warnings += "Повреждено фото $safe"; continue }
            File(photosDir, safe).writeBytes(bytes)
        }
        val available = validatePhotos(photosDir, warnings)
        return finishStage(data.copy(images = emptyMap()), dir, available, "JSON", warnings)
    }

    private fun validatePhotos(photosDir: File, warnings: MutableList<String>): Map<String, File> {
        val ok = HashMap<String, File>()
        photosDir.listFiles()?.forEach { f ->
            val head = ByteArray(16)
            val n = f.inputStream().use { it.read(head) }
            if (n >= 12 && BackupAnalyzer.isImage(head)) ok[f.name] = f
            else { warnings += "Файл ${f.name} не является изображением и пропущен"; f.delete() }
        }
        return ok
    }

    private suspend fun finishStage(data: BackupData, dir: File, photos: Map<String, File>, kind: String, warnings: List<String>): StagedImport {
        if (data.recipes.isEmpty() && data.menu.isEmpty()) throw IncompatibleBackupException("В файле нет рецептов")
        val existing = existingByKey()
        val preview = BackupAnalyzer.analyze(data, existing, photos.keys, kind, warnings)
        return StagedImport(data, preview, dir, photos)
    }

    private suspend fun existingByKey(): Map<String, Long> =
        db.recipeDao().getNames().sortedBy { it.id }.associate { BackupAnalyzer.key(it.name) to it.id }.let { m ->
            // keep the oldest recipe for each name
            val result = HashMap<String, Long>()
            db.recipeDao().getNames().sortedBy { it.id }.forEach { result.putIfAbsent(BackupAnalyzer.key(it.name), it.id) }
            if (result.isEmpty()) m else result
        }

    fun discard(staged: StagedImport) {
        staged.dir.deleteRecursively()
    }

    // ---------------------------------------------------------------- import: commit

    private enum class Action { ADD, UPDATE, SKIP }
    private data class Plan(val recipe: BackupRecipe, val action: Action, val targetId: Long?, val name: String)

    /**
     * Writes the staged backup in ONE Room transaction (any failure → full rollback, copied photos removed),
     * then reads every written recipe back to verify it really is in the database.
     */
    suspend fun commit(staged: StagedImport, mode: DuplicateMode): ImportReport = withContext(Dispatchers.IO) {
        val data = staged.data
        val copied = mutableListOf<File>()
        val oldPhotos = mutableListOf<String>()
        try {
            val existing = existingByKey()
            val plans = data.recipes.filter { it.name.isNotBlank() }.map { r ->
                val dup = existing[BackupAnalyzer.key(r.name)]
                when {
                    dup == null -> Plan(r, Action.ADD, null, r.name.trim())
                    mode == DuplicateMode.SKIP -> Plan(r, Action.SKIP, dup, r.name.trim())
                    mode == DuplicateMode.UPDATE -> Plan(r, Action.UPDATE, dup, r.name.trim())
                    else -> Plan(r, Action.ADD, null, "${r.name.trim()} (импорт)")
                }
            }

            // Photos of recipes that will be written get fresh names, so nothing on the device is overwritten.
            val photoMap = HashMap<String, String>()
            plans.filter { it.action != Action.SKIP }
                .flatMap { p -> listOfNotNull(p.recipe.photo) + p.recipe.steps.mapNotNull { it.photo } }
                .toSet()
                .forEach { name ->
                    val src = staged.photos[name] ?: return@forEach
                    val ext = name.substringAfterLast('.', "jpg").lowercase().take(5)
                    val dest = File(photos.photosDir, "${UUID.randomUUID()}.$ext")
                    src.copyTo(dest)
                    copied += dest
                    photoMap[name] = dest.name
                }

            val warnings = staged.preview.warnings.toMutableList()
            var added = 0
            var updated = 0
            var skipped = 0
            var menuAdded = 0
            var menuSkipped = 0
            val expected = LinkedHashMap<Long, Triple<String, Int, Int>>()

            db.withTransaction {
                val productDao = db.productDao()
                val tagDao = db.tagDao()
                val recipeDao = db.recipeDao()
                val products = productDao.getAll().associateBy { BackupAnalyzer.key(it.name) }.toMutableMap()
                val tags = tagDao.getAll().associateBy { BackupAnalyzer.key(it.name) }.toMutableMap()

                suspend fun product(name: String, unit: String, department: String): ProductEntity {
                    val k = BackupAnalyzer.key(name)
                    products[k]?.let { return it }
                    val e = ProductEntity(
                        name = name.trim(), baseUnit = MeasureUnit.parse(unit).name,
                        department = Department.parse(department).name, isCustom = true,
                    )
                    val created = e.copy(id = productDao.insert(e))
                    products[k] = created
                    return created
                }

                suspend fun tag(name: String, color: String): TagEntity {
                    val k = BackupAnalyzer.key(name)
                    tags[k]?.let { return it }
                    val e = TagEntity(name = name.trim(), colorHint = color, isCustom = true)
                    val created = e.copy(id = tagDao.insert(e))
                    tags[k] = created
                    return created
                }

                data.products.filter { it.name.isNotBlank() }.forEach { product(it.name, it.unit, it.department) }
                data.tags.filter { it.name.isNotBlank() }.forEach { tag(it.name, it.color) }

                val idMap = HashMap<Long, Long>()
                for (p in plans) {
                    val r = p.recipe
                    if (p.action == Action.SKIP) {
                        idMap[r.id] = p.targetId!!
                        skipped++
                        continue
                    }
                    val old = if (p.action == Action.UPDATE) recipeDao.get(p.targetId!!) else null
                    val entity = RecipeEntity(
                        id = p.targetId ?: 0,
                        name = p.name,
                        photoFile = r.photo?.let { photoMap[it] },
                        cookingTimeMin = r.cookingTimeMin.coerceAtLeast(0),
                        difficulty = r.difficulty,
                        category = r.category,
                        cuisine = old?.recipe?.cuisine.orEmpty(),
                        calories = r.calories?.takeIf { it >= 0 },
                        servings = r.servings.coerceAtLeast(1),
                        notes = r.notes,
                        createdAt = if (r.createdAt > 0) r.createdAt else System.currentTimeMillis(),
                        description = r.description,
                        prepTimeMin = r.prepTimeMin.coerceAtLeast(0),
                        ovenTempC = r.ovenTempC?.takeIf { it > 0 },
                    )
                    val id = if (old != null) {
                        oldPhotos += listOfNotNull(old.recipe.photoFile) + old.steps.mapNotNull { it.photoFile }
                        recipeDao.updateRecipe(entity)
                        recipeDao.clearIngredients(entity.id); recipeDao.clearSteps(entity.id); recipeDao.clearTags(entity.id)
                        updated++
                        entity.id
                    } else {
                        added++
                        recipeDao.insertRecipe(entity)
                    }
                    idMap[r.id] = id
                    val ingredients = r.ingredients.filter { it.product.isNotBlank() }.mapIndexed { i, ing ->
                        val unit = MeasureUnit.parse(ing.unit)
                        val amount = if (unit.isAmountless || !ing.amount.isFinite()) 0.0 else ing.amount.coerceAtLeast(0.0)
                        IngredientEntity(recipeId = id, productId = product(ing.product, ing.productUnit, ing.department).id, amount = amount, unit = unit.name, position = i)
                    }
                    recipeDao.insertIngredients(ingredients)
                    val steps = r.steps.filter { it.text.isNotBlank() || (it.photo != null && photoMap.containsKey(it.photo)) }
                        .mapIndexed { i, s -> StepEntity(recipeId = id, position = i, text = s.text.trim(), photoFile = s.photo?.let { photoMap[it] }) }
                    recipeDao.insertSteps(steps)
                    recipeDao.insertTagRefs(r.tags.filter { it.isNotBlank() }.map { RecipeTagCrossRef(id, tag(it, "cream").id) }.distinct())
                    expected[id] = Triple(p.name, ingredients.size, steps.size)
                }

                // Menu: every dish is appended to its meal; the same recipe is not added twice to one meal.
                val menuDao = db.menuDao()
                for (m in data.menu.sortedWith(compareBy({ it.date }, { it.slot }, { it.position }))) {
                    val recipeId = idMap[m.recipeId]
                    val date = runCatching { LocalDate.parse(m.date) }.getOrNull()
                    val slot = MealSlot.entries.firstOrNull { it.name.equals(m.slot, true) }
                    if (recipeId == null || date == null || slot == null) { menuSkipped++; continue }
                    val meal = menuDao.getSlot(date.toString(), slot.name)
                    if (meal.any { it.recipeId == recipeId }) { menuSkipped++; continue }
                    menuDao.insert(
                        MenuEntryEntity(
                            date = date.toString(), slot = slot.name, recipeId = recipeId,
                            servings = m.servings.coerceAtLeast(1), position = (meal.maxOfOrNull { it.position } ?: -1) + 1,
                        ),
                    )
                    menuAdded++
                }

                data.settings?.let { s ->
                    val current = db.settingsDao().get() ?: UserSettingsEntity()
                    db.settingsDao().save(
                        current.copy(
                            themeMode = ThemeMode.parse(s.themeMode).name,
                            themeId = s.themeId ?: current.themeId,
                            customColors = s.customColors ?: current.customColors,
                        ),
                    )
                }
            }

            // Verification: read back what was written.
            val failed = mutableListOf<String>()
            for ((id, exp) in expected) {
                val full = db.recipeDao().get(id)
                if (full == null || full.ingredients.size != exp.second || full.steps.size != exp.third) failed += exp.first
            }
            val newNames = photoMap.values.toSet()
            if (failed.isEmpty()) oldPhotos.filter { it !in newNames }.forEach(photos::delete)
            ImportReport(
                added = added, updated = updated, skipped = skipped, photosRestored = photoMap.size,
                menuAdded = menuAdded, menuSkipped = menuSkipped, failed = failed, warnings = warnings,
                verified = failed.isEmpty(),
            )
        } catch (e: Exception) {
            Log.e(TAG, "Import failed, rolled back", e)
            copied.forEach { it.delete() }
            throw e
        } finally {
            staged.dir.deleteRecursively()
        }
    }

    companion object {
        private const val TAG = "HomeMenuBackup"
        private const val BOM = "\uFEFF"
        private const val MAX_JSON_BYTES = 64L * 1024 * 1024
        private const val MAX_PHOTO_BYTES = 32L * 1024 * 1024
        private const val MAX_TOTAL_BYTES = 1024L * 1024 * 1024

        fun safeFileName(name: String): String =
            name.replace(Regex("[^\\p{L}\\p{N}]+"), "_").trim('_').take(40).ifBlank { "recipe" }

        fun sha256(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256").digest(bytes).toHex()

        private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }

        private fun InputStream.readLimited(limit: Long): ByteArray {
            val out = ByteArrayOutputStream()
            copyLimited(out, limit, null)
            return out.toByteArray()
        }

        private fun InputStream.copyLimited(out: OutputStream, limit: Long, digest: MessageDigest?): Long {
            val buf = ByteArray(64 * 1024)
            var total = 0L
            while (true) {
                val n = read(buf)
                if (n < 0) break
                total += n
                if (total > limit) throw IncompatibleBackupException("Файл в архиве слишком большой")
                digest?.update(buf, 0, n)
                out.write(buf, 0, n)
            }
            return total
        }
    }
}
