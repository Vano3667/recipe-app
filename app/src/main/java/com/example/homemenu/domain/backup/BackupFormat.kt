package com.example.homemenu.domain.backup

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Backup format (documented in BACKUP_FORMAT.md).
 *  v1 — app 1.0: zip with recipes.json + photos/.
 *  v2 — app 1.1: + manifest.json with SHA-256 of every file, recipe description / prep time / oven temperature,
 *       dish order inside a meal, appearance settings, optional Base64 images for the standalone JSON export.
 * The reader accepts every version from 1 to [BACKUP_FORMAT_VERSION].
 *
 * Files written by other tools (or by hand) may contain explicit `null` for fields that have a default here,
 * e.g. `"cookingTimeMin": null`, `"amount": null` for "to taste" ingredients. The reader treats such a null
 * as "use the default" (see [BackupJson.json]); the real-world fixture `recipes_from_budget_classic_1500kcal.json`
 * depends on this.
 */
const val BACKUP_FORMAT_VERSION = 2
const val BACKUP_FORMAT_ID = "home-menu-backup"
const val BACKUP_DATA_FILE = "recipes.json"
const val BACKUP_MANIFEST_FILE = "manifest.json"
const val BACKUP_PHOTOS_DIR = "photos/"

class IncompatibleBackupException(message: String) : Exception(message)

@Serializable
data class BackupProduct(
    val name: String,
    val unit: String,
    val department: String,
    val custom: Boolean = true,
)

@Serializable
data class BackupTag(val name: String, val color: String, val custom: Boolean = true)

/** Ingredient order = order in the list. A missing/null [amount] (e.g. unit TO_TASTE) is read as 0. */
@Serializable
data class BackupIngredient(
    val product: String,
    val productUnit: String = "G",
    val department: String = "OTHER",
    val amount: Double = 0.0,
    val unit: String = "G",
)

/** [photo] is a file name inside photos/ (zip) or a key of [BackupData.images] (json), never a device path. */
@Serializable
data class BackupStep(val text: String, val photo: String? = null)

@Serializable
data class BackupRecipe(
    val id: Long,
    val name: String,
    val description: String = "",
    val photo: String? = null,
    val cookingTimeMin: Int = 0,
    val prepTimeMin: Int = 0,
    val ovenTempC: Int? = null,
    val difficulty: String = "EASY",
    val category: String = "DINNER",
    val calories: Int? = null,
    val servings: Int = 1,
    val ingredients: List<BackupIngredient> = emptyList(),
    val steps: List<BackupStep> = emptyList(),
    val notes: String = "",
    val tags: List<String> = emptyList(),
    val createdAt: Long = 0L,
)

/** One dish of a meal; [recipeId] refers to [BackupRecipe.id] inside the same file. */
@Serializable
data class BackupMenuEntry(
    val date: String,
    val slot: String,
    val recipeId: Long,
    val servings: Int,
    val position: Int = 0,
)

@Serializable
data class BackupSettings(
    val themeMode: String = "SYSTEM",
    val themeId: String? = null,
    val customColors: String? = null,
)

@Serializable
data class BackupData(
    @SerialName("formatVersion") val version: Int = BACKUP_FORMAT_VERSION,
    val format: String = BACKUP_FORMAT_ID,
    val app: String = "HomeMenu",
    val appVersion: String = "",
    val exportedAt: Long = 0L,
    val products: List<BackupProduct> = emptyList(),
    val tags: List<BackupTag> = emptyList(),
    val recipes: List<BackupRecipe> = emptyList(),
    val menu: List<BackupMenuEntry> = emptyList(),
    val settings: BackupSettings? = null,
    /** Standalone JSON export only: photo file name → Base64 image. Empty inside a zip (photos live in photos/). */
    val images: Map<String, String> = emptyMap(),
)

@Serializable
data class ManifestFile(val path: String, val size: Long, val sha256: String)

@Serializable
data class BackupManifest(
    val format: String = BACKUP_FORMAT_ID,
    val formatVersion: Int = BACKUP_FORMAT_VERSION,
    val app: String = "HomeMenu",
    val appVersion: String = "",
    val exportedAt: Long = 0L,
    val recipes: Int = 0,
    val menuEntries: Int = 0,
    val photos: Int = 0,
    val files: List<ManifestFile> = emptyList(),
)

object BackupJson {
    /**
     * [coerceInputValues]: an explicit `null` for a non-nullable property that has a default is read as that
     * default instead of failing the whole import (Task 6 / user check 23.2).
     */
    @OptIn(ExperimentalSerializationApi::class)
    val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
        explicitNulls = false
        coerceInputValues = true
    }

    fun encode(data: BackupData): String = json.encodeToString(BackupData.serializer(), data)

    fun encodeManifest(m: BackupManifest): String = json.encodeToString(BackupManifest.serializer(), m)

    fun decodeManifest(text: String): BackupManifest = try {
        json.decodeFromString(BackupManifest.serializer(), text)
    } catch (e: Exception) {
        throw IncompatibleBackupException("Файл manifest.json повреждён")
    }

    /** Throws [IncompatibleBackupException] for unknown/newer versions or malformed JSON. */
    fun decode(text: String): BackupData {
        val version = try {
            json.parseToJsonElement(text).jsonObject["formatVersion"]?.jsonPrimitive?.int
        } catch (e: Exception) {
            throw IncompatibleBackupException("Файл повреждён или не является резервной копией «Домашнего меню»")
        } ?: throw IncompatibleBackupException("В резервной копии не указана версия формата")
        if (version < 1 || version > BACKUP_FORMAT_VERSION) {
            throw IncompatibleBackupException("Версия резервной копии ($version) не поддерживается")
        }
        return try {
            json.decodeFromString(BackupData.serializer(), text)
        } catch (e: SerializationException) {
            throw IncompatibleBackupException("Не удалось прочитать резервную копию: ${e.message}")
        } catch (e: IllegalArgumentException) {
            throw IncompatibleBackupException("Не удалось прочитать резервную копию: ${e.message}")
        }
    }
}
