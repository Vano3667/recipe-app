package com.example.homemenu.domain

import com.example.homemenu.domain.model.Department
import com.example.homemenu.domain.model.MeasureUnit
import com.example.homemenu.domain.model.Product
import com.example.homemenu.domain.model.Recipe
import javax.inject.Inject

/** A recipe scheduled with a specific number of servings. */
data class PlannedDish(val recipe: Recipe, val servings: Int)

data class ShoppingItem(
    val product: Product,
    val unit: MeasureUnit,
    val amount: ScaledAmount,
) {
    /** Stable key used to persist the "bought" checkbox. */
    val key: String get() = "${product.id}|${unit.name}"
    val text: String get() = amount.format(unit)
}

data class DepartmentGroup(val department: Department, val items: List<ShoppingItem>)

class ShoppingListUseCase @Inject constructor(
    private val scaling: ScalingUseCase,
) {
    /**
     * Sums ingredients per (product, unit). Different units of one product stay on
     * separate lines (no conversion). Rounding up happens once, after summation.
     */
    fun aggregate(dishes: List<PlannedDish>): List<ShoppingItem> {
        val sums = LinkedHashMap<Pair<Long, MeasureUnit>, Pair<Product, Double>>()
        for (dish in dishes) {
            if (dish.servings <= 0 || dish.recipe.servings <= 0) continue
            for (ing in dish.recipe.ingredients) {
                val raw = scaling.scaleRaw(ing.amount, dish.recipe.servings, dish.servings)
                val key = ing.product.id to ing.unit
                val prev = sums[key]
                sums[key] = ing.product to ((prev?.second ?: 0.0) + raw)
            }
        }
        return sums.map { (key, value) ->
            ShoppingItem(value.first, key.second, AmountRounding.roundUp(value.second))
        }
    }

    /** Groups by store department, departments ordered as in the store, items alphabetically. */
    fun groupByDepartment(items: List<ShoppingItem>): List<DepartmentGroup> =
        items.groupBy { it.product.department }
            .toSortedMap(compareBy { it.order })
            .map { (dep, list) ->
                DepartmentGroup(dep, list.sortedWith(compareBy<ShoppingItem> { it.product.name.lowercase() }.thenBy { it.unit.ordinal }))
            }

    fun build(dishes: List<PlannedDish>): List<DepartmentGroup> = groupByDepartment(aggregate(dishes))

    /**
     * Plain-text list for messengers (WhatsApp, Telegram, e-mail). Uses ☐/☑ symbols because neither
     * WhatsApp messages nor Android share intents support interactive checklists.
     *
     * @param onlyRemaining when true, bought items and departments with nothing left are omitted.
     */
    fun shareText(
        title: String,
        groups: List<DepartmentGroup>,
        bought: Set<String>,
        onlyRemaining: Boolean = false,
    ): String = buildString {
        appendLine("🛒 $title")
        val total = groups.sumOf { it.items.size }
        val done = groups.sumOf { g -> g.items.count { it.key in bought } }
        for (g in groups) {
            val items = if (onlyRemaining) g.items.filter { it.key !in bought } else g.items
            if (items.isEmpty()) continue
            appendLine()
            appendLine(g.department.title + ":")
            for (item in items) {
                val mark = if (item.key in bought) "☑" else "☐"
                appendLine("$mark ${item.product.name} — ${item.text}")
            }
        }
        if (onlyRemaining && total > 0 && done == total) {
            appendLine()
            appendLine("✅ Всё куплено")
        } else if (!onlyRemaining && done > 0) {
            appendLine()
            appendLine("Куплено: $done из $total")
        }
    }.trimEnd()
}
