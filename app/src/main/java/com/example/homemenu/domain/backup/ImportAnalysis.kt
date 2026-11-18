package com.example.homemenu.domain.backup

/** What to do with a recipe from the file whose name matches an existing recipe. */
enum class DuplicateMode(val title: String, val description: String) {
    SKIP("Пропустить", "Существующие рецепты останутся без изменений"),
    UPDATE("Обновить", "Существующие рецепты будут заменены данными из файла"),
    COPY("Как копии", "Рецепты из файла добавятся отдельно, с пометкой «(импорт)»"),
}

data class DuplicateInfo(val backupId: Long, val name: String, val existingId: Long)

data class ImportPreview(
    val formatVersion: Int,
    val sourceKind: String,
    val recipes: Int,
    val ingredients: Int,
    val steps: Int,
    val tags: Int,
    val menuEntries: Int,
    val photosReferenced: Int,
    val photosAvailable: Int,
    val missingPhotos: List<String>,
    val duplicates: List<DuplicateInfo>,
    val hasSettings: Boolean,
    val warnings: List<String>,
) {
    val newRecipes: Int get() = recipes - duplicates.size
}

data class ImportReport(
    val added: Int,
    val updated: Int,
    val skipped: Int,
    val photosRestored: Int,
    val menuAdded: Int,
    val menuSkipped: Int,
    val failed: List<String>,
    val warnings: List<String>,
    val verified: Boolean,
) {
    fun summary(): String = buildString {
        append("Добавлено рецептов: $added")
        if (updated > 0) append(", обновлено: $updated")
        if (skipped > 0) append(", пропущено: $skipped")
        append(". Фото: $photosRestored. Блюд в меню: $menuAdded")
        if (menuSkipped > 0) append(" (пропущено повторов: $menuSkipped)")
        append('.')
    }
}

/** Pure analysis of a decoded backup against the current database. Unit-tested. */
object BackupAnalyzer {
    fun key(name: String): String = name.trim().lowercase().replace('ё', 'е').replace(Regex("\\s+"), " ")

    fun referencedPhotos(data: BackupData): Set<String> =
        data.recipes.flatMap { r -> listOfNotNull(r.photo) + r.steps.mapNotNull { it.photo } }.toSet()

    /**
     * @param existingByKey existing recipe ids by [key] of their names.
     * @param availablePhotos photo names that are present in the file and passed validation.
     */
    fun analyze(
        data: BackupData,
        existingByKey: Map<String, Long>,
        availablePhotos: Set<String>,
        sourceKind: String,
        extraWarnings: List<String> = emptyList(),
    ): ImportPreview {
        val warnings = extraWarnings.toMutableList()
        val referenced = referencedPhotos(data)
        val missing = referenced.filter { it !in availablePhotos }.sorted()
        if (missing.isNotEmpty()) warnings += "Нет фото в файле: ${missing.size} — рецепты будут импортированы без них"
        val ids = data.recipes.map { it.id }.toSet()
        val orphanMenu = data.menu.count { it.recipeId !in ids }
        if (orphanMenu > 0) warnings += "Записей меню без рецепта в файле: $orphanMenu — они будут пропущены"
        val blank = data.recipes.count { it.name.isBlank() }
        if (blank > 0) warnings += "Рецептов без названия: $blank — они будут пропущены"
        val duplicates = data.recipes.filter { it.name.isNotBlank() }.mapNotNull { r ->
            existingByKey[key(r.name)]?.let { DuplicateInfo(r.id, r.name, it) }
        }
        return ImportPreview(
            formatVersion = data.version,
            sourceKind = sourceKind,
            recipes = data.recipes.count { it.name.isNotBlank() },
            ingredients = data.recipes.sumOf { it.ingredients.size },
            steps = data.recipes.sumOf { it.steps.size },
            tags = (data.tags.map { key(it.name) } + data.recipes.flatMap { r -> r.tags.map(::key) }).toSet().size,
            menuEntries = data.menu.size - orphanMenu,
            photosReferenced = referenced.size,
            photosAvailable = referenced.count { it in availablePhotos },
            missingPhotos = missing,
            duplicates = duplicates,
            hasSettings = data.settings != null,
            warnings = warnings,
        )
    }

    /** Accepts JPEG, PNG, WebP, GIF and HEIF/AVIF by their signature bytes. */
    fun isImage(head: ByteArray): Boolean {
        if (head.size < 12) return false
        fun at(i: Int, vararg b: Int) = b.indices.all { head[i + it] == b[it].toByte() }
        return at(0, 0xFF, 0xD8, 0xFF) ||
            at(0, 0x89, 0x50, 0x4E, 0x47) ||
            (at(0, 0x52, 0x49, 0x46, 0x46) && at(8, 0x57, 0x45, 0x42, 0x50)) ||
            at(0, 0x47, 0x49, 0x46, 0x38) ||
            at(4, 0x66, 0x74, 0x79, 0x70)
    }

    /** Only plain file names are accepted for photos (no folders, no ".."), so nothing escapes photos/. */
    fun safePhotoName(name: String): String? {
        val n = name.substringAfterLast('/').substringAfterLast('\\')
        return n.takeIf { it.isNotBlank() && it != "." && it != ".." && !it.contains("..") && it.length <= 128 }
    }
}
