package com.example.homemenu.data.db

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Junction
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(tableName = "products", indices = [Index(value = ["name"], unique = true)])
data class ProductEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val baseUnit: String,
    val department: String,
    val isCustom: Boolean = false,
)

@Entity(tableName = "tags", indices = [Index(value = ["name"], unique = true)])
data class TagEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val colorHint: String,
    val isCustom: Boolean = false,
)

@Entity(tableName = "recipes")
data class RecipeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val photoFile: String? = null,
    val cookingTimeMin: Int = 0,
    val difficulty: String = "EASY",
    val category: String = "DINNER",
    /**
     * Legacy column from DB v1 («Кухня»). The cuisine classification was removed from the app;
     * the column stays so that existing databases migrate without rebuilding the table, and old values are kept.
     */
    val cuisine: String = "",
    val calories: Int? = null,
    val servings: Int = 1,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    // --- v2 ---
    @ColumnInfo(defaultValue = "") val description: String = "",
    @ColumnInfo(defaultValue = "0") val prepTimeMin: Int = 0,
    val ovenTempC: Int? = null,
)

@Entity(
    tableName = "ingredients",
    foreignKeys = [
        ForeignKey(entity = RecipeEntity::class, parentColumns = ["id"], childColumns = ["recipeId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = ProductEntity::class, parentColumns = ["id"], childColumns = ["productId"], onDelete = ForeignKey.RESTRICT),
    ],
    indices = [Index("recipeId"), Index("productId")],
)
data class IngredientEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val recipeId: Long,
    val productId: Long,
    val amount: Double,
    val unit: String,
    val position: Int,
)

@Entity(
    tableName = "steps",
    foreignKeys = [ForeignKey(entity = RecipeEntity::class, parentColumns = ["id"], childColumns = ["recipeId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("recipeId")],
)
data class StepEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val recipeId: Long,
    val position: Int,
    val text: String,
    val photoFile: String? = null,
)

@Entity(
    tableName = "recipe_tags",
    primaryKeys = ["recipeId", "tagId"],
    foreignKeys = [
        ForeignKey(entity = RecipeEntity::class, parentColumns = ["id"], childColumns = ["recipeId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = TagEntity::class, parentColumns = ["id"], childColumns = ["tagId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("tagId")],
)
data class RecipeTagCrossRef(val recipeId: Long, val tagId: Long)

/**
 * One dish of a meal. Since v2 a meal (date + slot) may contain several dishes, ordered by [position];
 * the (date, slot) index is no longer unique. No FK to recipes on purpose: deleting a scheduled recipe
 * asks the user to clear its slots first.
 */
@Entity(tableName = "menu_entries", indices = [Index(value = ["date", "slot"]), Index("recipeId")])
data class MenuEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String,
    val slot: String,
    val recipeId: Long,
    val servings: Int,
    @ColumnInfo(defaultValue = "0") val position: Int = 0,
)

@Entity(tableName = "shopping_checks")
data class ShoppingCheckEntity(
    @PrimaryKey val key: String,
)

@Entity(tableName = "user_settings")
data class UserSettingsEntity(
    @PrimaryKey val id: Int = 0,
    val themeMode: String = "SYSTEM",
    val demoSeeded: Boolean = false,
    // --- v2 ---
    /** Selected colour theme (AppThemeId name). LEGACY = the v1 look that follows [themeMode]. */
    @ColumnInfo(defaultValue = "LEGACY") val themeId: String = "LEGACY",
    /** User colour overrides as JSON (see CustomColors), null = none. */
    val customColors: String? = null,
)

data class IngredientWithProduct(
    @Embedded val ingredient: IngredientEntity,
    @Relation(parentColumn = "productId", entityColumn = "id")
    val product: ProductEntity,
)

data class RecipeFull(
    @Embedded val recipe: RecipeEntity,
    @Relation(entity = IngredientEntity::class, parentColumn = "id", entityColumn = "recipeId")
    val ingredients: List<IngredientWithProduct>,
    @Relation(parentColumn = "id", entityColumn = "recipeId")
    val steps: List<StepEntity>,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(RecipeTagCrossRef::class, parentColumn = "recipeId", entityColumn = "tagId"),
    )
    val tags: List<TagEntity>,
)
