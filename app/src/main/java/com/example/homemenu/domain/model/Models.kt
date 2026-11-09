package com.example.homemenu.domain.model

import java.time.LocalDate

enum class MeasureUnit(val label: String) {
    G("г"), ML("мл"), PCS("шт"), TBSP("ст. л."), TSP("ч. л."), PINCH("щепотка");

    companion object {
        fun parse(code: String): MeasureUnit = when (code.lowercase()) {
            "g" -> G
            "ml" -> ML
            "pcs" -> PCS
            "tbsp" -> TBSP
            "tsp" -> TSP
            "pinch" -> PINCH
            else -> entries.firstOrNull { it.name.equals(code, ignoreCase = true) } ?: G
        }
    }
}

enum class Department(val title: String, val order: Int) {
    VEGETABLES("Овощи", 1),
    FRUITS("Фрукты", 2),
    MEAT("Мясо и птица", 3),
    FISH("Рыба и морепродукты", 4),
    DAIRY("Молочные продукты", 5),
    GRAINS("Крупы и макароны", 6),
    LEGUMES("Бобовые", 7),
    NUTS("Орехи и сухофрукты", 8),
    SPICES("Специи и приправы", 9),
    SAUCES("Соусы и масла", 10),
    BAKERY("Хлеб и выпечка", 11),
    PANTRY("Бакалея", 12),
    OTHER("Другое", 99);

    companion object {
        fun parse(code: String): Department =
            entries.firstOrNull { it.name.equals(code, ignoreCase = true) } ?: OTHER
    }
}

enum class Difficulty(val title: String) {
    EASY("Легко"), MEDIUM("Средне"), HARD("Сложно");

    companion object {
        fun parse(code: String) = entries.firstOrNull { it.name.equals(code, true) } ?: EASY
    }
}

enum class Category(val title: String) {
    BREAKFAST("Завтрак"), LUNCH("Обед"), DINNER("Ужин"), DESSERT("Десерт");

    companion object {
        fun parse(code: String) = entries.firstOrNull { it.name.equals(code, true) } ?: DINNER
    }
}

enum class MealSlot(val title: String) {
    BREAKFAST("Завтрак"), LUNCH("Обед"), DINNER("Ужин"), SNACK("Перекус");

    /** Category suggested for auto-generation when the user asks to respect slots. */
    val defaultCategory: Category?
        get() = when (this) {
            BREAKFAST -> Category.BREAKFAST
            LUNCH -> Category.LUNCH
            DINNER -> Category.DINNER
            SNACK -> null
        }

    companion object {
        fun parse(code: String) = entries.firstOrNull { it.name.equals(code, true) } ?: BREAKFAST
    }
}

data class Product(
    val id: Long,
    val name: String,
    val baseUnit: MeasureUnit,
    val department: Department,
    val isCustom: Boolean = false,
)

data class Tag(
    val id: Long,
    val name: String,
    val colorHint: String,
    val isCustom: Boolean = false,
)

data class Ingredient(
    val product: Product,
    val amount: Double,
    val unit: MeasureUnit,
)

data class Step(
    val text: String,
    val photoFile: String? = null,
)

data class Recipe(
    val id: Long,
    val name: String,
    val photoFile: String? = null,
    val cookingTimeMin: Int = 0,
    val difficulty: Difficulty = Difficulty.EASY,
    val category: Category = Category.DINNER,
    val cuisine: String = "",
    val calories: Int? = null,
    val servings: Int = 1,
    val ingredients: List<Ingredient> = emptyList(),
    val steps: List<Step> = emptyList(),
    val notes: String = "",
    val tags: List<Tag> = emptyList(),
    val createdAt: Long = 0L,
)

data class MenuEntry(
    val id: Long,
    val date: LocalDate,
    val slot: MealSlot,
    val recipeId: Long,
    val servings: Int,
)

enum class ThemeMode(val title: String) {
    SYSTEM("Как в системе"), LIGHT("Светлая"), DARK("Тёмная");

    companion object {
        fun parse(code: String?) = entries.firstOrNull { it.name.equals(code, true) } ?: SYSTEM
    }
}

val DEFAULT_CUISINES = listOf(
    "русская", "европейская", "итальянская", "греческая", "узбекская",
    "американская", "французская", "грузинская", "японская", "китайская",
    "мексиканская", "индийская", "тайская",
)
