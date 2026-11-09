package com.example.homemenu

import com.example.homemenu.domain.model.Category
import com.example.homemenu.domain.model.Department
import com.example.homemenu.domain.model.Difficulty
import com.example.homemenu.domain.model.Ingredient
import com.example.homemenu.domain.model.MeasureUnit
import com.example.homemenu.domain.model.Product
import com.example.homemenu.domain.model.Recipe
import com.example.homemenu.domain.model.Step
import com.example.homemenu.domain.model.Tag

object F {
    val flour = Product(1, "Мука пшеничная", MeasureUnit.G, Department.GRAINS)
    val milk = Product(2, "Молоко", MeasureUnit.ML, Department.DAIRY)
    val egg = Product(3, "Яйцо куриное", MeasureUnit.PCS, Department.DAIRY)
    val chicken = Product(4, "Куриное филе", MeasureUnit.G, Department.MEAT)
    val rice = Product(5, "Рис", MeasureUnit.G, Department.GRAINS)
    val tomato = Product(6, "Помидоры", MeasureUnit.G, Department.VEGETABLES)
    val beet = Product(7, "Свёкла", MeasureUnit.G, Department.VEGETABLES)
    val sugar = Product(8, "Сахар", MeasureUnit.G, Department.GRAINS)

    val quick = Tag(1, "Быстро", "green")
    val veg = Tag(2, "Вегетарианское", "sage")
    val festive = Tag(3, "Праздничное", "rose")

    fun ing(p: Product, amount: Double, unit: MeasureUnit = p.baseUnit) = Ingredient(p, amount, unit)

    fun recipe(
        id: Long,
        name: String,
        servings: Int = 2,
        ingredients: List<Ingredient> = emptyList(),
        category: Category = Category.DINNER,
        tags: List<Tag> = emptyList(),
        time: Int = 30,
        createdAt: Long = id,
        calories: Int? = null,
        difficulty: Difficulty = Difficulty.EASY,
    ) = Recipe(
        id = id, name = name, cookingTimeMin = time, difficulty = difficulty, category = category,
        calories = calories, servings = servings, ingredients = ingredients,
        steps = listOf(Step("Шаг 1")), tags = tags, createdAt = createdAt,
    )

    val pancakes = recipe(
        1, "Блины", servings = 4, category = Category.BREAKFAST, tags = listOf(quick, veg), time = 40, calories = 250,
        ingredients = listOf(ing(flour, 200.0), ing(milk, 500.0), ing(egg, 2.0), ing(sugar, 1.0, MeasureUnit.TBSP)),
    )
    val chickenRice = recipe(
        2, "Курица с рисом", servings = 2, category = Category.LUNCH, tags = listOf(quick), time = 35, calories = 520,
        ingredients = listOf(ing(chicken, 300.0), ing(rice, 150.0), ing(tomato, 100.0)),
    )
    val borscht = recipe(
        3, "Борщ", servings = 6, category = Category.LUNCH, tags = listOf(festive), time = 120, calories = 180,
        ingredients = listOf(ing(beet, 300.0), ing(tomato, 200.0)),
    )
    val omelette = recipe(
        4, "Омлет", servings = 1, category = Category.BREAKFAST, tags = listOf(quick, veg), time = 10,
        ingredients = listOf(ing(egg, 3.0), ing(milk, 50.0)),
    )
    val all = listOf(pancakes, chickenRice, borscht, omelette)
}
