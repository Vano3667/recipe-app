package com.example.homemenu.domain

import com.example.homemenu.F
import com.example.homemenu.domain.model.Department
import com.example.homemenu.domain.model.MeasureUnit
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ShoppingListUseCaseTest {
    private val useCase = ShoppingListUseCase(ScalingUseCase())

    private fun List<ShoppingItem>.find(name: String, unit: MeasureUnit? = null) =
        single { it.product.name == name && (unit == null || it.unit == unit) }

    @Test fun `same product and unit are summed across recipes`() {
        // pancakes: 500 ml milk / 4 servings -> 4 servings = 500; omelette: 50 ml / 1 -> 2 servings = 100
        val items = useCase.aggregate(listOf(PlannedDish(F.pancakes, 4), PlannedDish(F.omelette, 2)))
        assertThat(items.find("Молоко").text).isEqualTo("600 мл")
        // eggs: 2 + 6 = 8
        assertThat(items.find("Яйцо куриное").amount).isEqualTo(ScaledAmount(8, false))
    }

    @Test fun `rounding happens once after summation`() {
        // 2 eggs / 4 servings at 1 serving = 0.5; twice -> 1.0 exactly (not ~2)
        val items = useCase.aggregate(listOf(PlannedDish(F.pancakes, 1), PlannedDish(F.pancakes, 1)))
        assertThat(items.find("Яйцо куриное").amount).isEqualTo(ScaledAmount(1, false))
    }

    @Test fun `fractional total is approximate`() {
        // 2 eggs / 4 servings at 3 = 1.5
        val items = useCase.aggregate(listOf(PlannedDish(F.pancakes, 3)))
        assertThat(items.find("Яйцо куриное").text).isEqualTo("~2 шт")
    }

    @Test fun `different units of one product stay separate`() {
        val sweet = F.recipe(10, "Сироп", servings = 1, ingredients = listOf(F.ing(F.sugar, 100.0, MeasureUnit.G)))
        val items = useCase.aggregate(listOf(PlannedDish(F.pancakes, 4), PlannedDish(sweet, 1)))
        assertThat(items.filter { it.product.name == "Сахар" }.map { it.unit }).containsExactly(MeasureUnit.TBSP, MeasureUnit.G)
    }

    @Test fun `dishes with non positive servings are ignored`() {
        assertThat(useCase.aggregate(listOf(PlannedDish(F.borscht, 0), PlannedDish(F.borscht, -2)))).isEmpty()
    }

    @Test fun `groups follow store department order and items are alphabetical`() {
        val groups = useCase.build(listOf(PlannedDish(F.chickenRice, 2), PlannedDish(F.borscht, 6), PlannedDish(F.pancakes, 4)))
        val orders = groups.map { it.department.order }
        assertThat(orders).isInOrder()
        assertThat(groups.first().department).isEqualTo(Department.VEGETABLES)
        val veg = groups.first { it.department == Department.VEGETABLES }.items.map { it.product.name }
        assertThat(veg).containsExactly("Помидоры", "Свёкла").inOrder()
        // tomatoes: 100 (chicken) + 200 (borscht) = 300
        assertThat(groups.flatMap { it.items }.find("Помидоры").text).isEqualTo("300 г")
    }

    @Test fun `empty plan gives empty list`() {
        assertThat(useCase.build(emptyList())).isEmpty()
    }

    @Test fun `share text marks bought items`() {
        val groups = useCase.build(listOf(PlannedDish(F.omelette, 1)))
        val eggKey = groups.flatMap { it.items }.find("Яйцо куриное").key
        val text = useCase.shareText("Покупки", groups, setOf(eggKey))
        assertThat(text).contains("☑ Яйцо куриное — 3 шт")
        assertThat(text).contains("☐ Молоко — 50 мл")
        assertThat(text).contains("Молочные продукты:")
    }

    @Test fun `item key is stable per product and unit`() {
        val item = useCase.aggregate(listOf(PlannedDish(F.omelette, 1))).find("Молоко")
        assertThat(item.key).isEqualTo("2|ML")
    }
}
