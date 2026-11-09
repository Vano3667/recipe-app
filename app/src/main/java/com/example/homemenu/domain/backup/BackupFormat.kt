package com.example.homemenu.domain.backup

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

const val BACKUP_FORMAT_VERSION = 1

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

@Serializable
data class BackupIngredient(
    val product: String,
    val productUnit: String = "G",
    val department: String = "OTHER",
    val amount: Double,
    val unit: String,
)

@Serializable
data class BackupStep(val text: String, val photo: String? = null)

@Serializable
data class BackupRecipe(
    val id: Long,
    val name: String,
    val photo: String? = null,
    val cookingTimeMin: Int = 0,
    val difficulty: String = "EASY",
    val category: String = "DINNER",
    val cuisine: String = "",
    val calories: Int? = null,
    val servings: Int = 1,
    val ingredients: List<BackupIngredient> = emptyList(),
    val steps: List<BackupStep> = emptyList(),
    val notes: String = "",
    val tags: List<String> = emptyList(),
    val createdAt: Long = 0L,
)

@Serializable
data class BackupMenuEntry(
    val date: String,
    val slot: String,
    val recipeId: Long,
    val servings: Int,
)

@Serializable
data class BackupSettings(val themeMode: String = "SYSTEM")

@Serializable
data class BackupData(
    @SerialName("formatVersion") val version: Int = BACKUP_FORMAT_VERSION,
    val app: String = "HomeMenu",
    val exportedAt: Long = 0L,
    val products: List<BackupProduct> = emptyList(),
    val tags: List<BackupTag> = emptyList(),
    val recipes: List<BackupRecipe> = emptyList(),
    val menu: List<BackupMenuEntry> = emptyList(),
    val settings: BackupSettings? = null,
)

object BackupJson {
    val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
        explicitNulls = false
    }

    fun encode(data: BackupData): String = json.encodeToString(BackupData.serializer(), data)

    /** Throws [IncompatibleBackupException] for unknown/newer versions or malformed JSON. */
    fun decode(text: String): BackupData {
        val version = try {
            json.parseToJsonElement(text).jsonObject["formatVersion"]?.jsonPrimitive?.int
        } catch (e: Exception) {
            throw IncompatibleBackupException("Файл recipes.json повреждён или не является резервной копией")
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
