package com.example.homemenu.ui

import com.example.homemenu.domain.model.MeasureUnit
import com.example.homemenu.ui.recipes.BulkIngredients
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class BulkIngredientsTest {

    private val sample = """
        500 g chicken breast

        2 onions
           1 tsp salt   
        200 ml cream
        3 cloves garlic
    """.trimIndent()

    @Test fun everyNonEmptyLineBecomesOneRowInOrder() {
        val rows = BulkIngredients.toRows(sample, emptyList())
        assertThat(rows.map { it.name }).containsExactly("chicken breast", "onions", "salt", "cream", "cloves garlic").inOrder()
    }

    @Test fun amountsAndUnitsAreKept() {
        val rows = BulkIngredients.toRows(sample, emptyList())
        assertThat(rows[0].amountText).isEqualTo("500")
        assertThat(rows[0].unit).isEqualTo(MeasureUnit.G)
        assertThat(rows[1].amountText).isEqualTo("2")
        assertThat(rows[1].unit).isEqualTo(MeasureUnit.PCS)
        assertThat(rows[2].unit).isEqualTo(MeasureUnit.TSP)
        assertThat(rows[3].amountText).isEqualTo("200")
        assertThat(rows[3].unit).isEqualTo(MeasureUnit.ML)
    }

    @Test fun fractionalAmountUsesComma() {
        val row = BulkIngredients.toRows("1,5 кг муки", emptyList()).single()
        assertThat(row.amountText).isEqualTo("1500")
        assertThat(row.unit).isEqualTo(MeasureUnit.G)
        val half = BulkIngredients.toRows("½ ч. л. соды", emptyList()).single()
        assertThat(half.amountText).isEqualTo("0,5")
    }

    @Test fun unrecognizedLineIsKeptNotDropped() {
        val rows = BulkIngredients.toRows("что-нибудь для украшения", emptyList())
        assertThat(rows).hasSize(1)
        assertThat(rows[0].recognized).isFalse()
        assertThat(rows[0].amountText).isEmpty()
        assertThat(rows[0].include).isTrue()
        assertThat(BulkIngredients.accepted(rows)).hasSize(1)
    }

    @Test fun toTasteHasNoAmount() {
        val row = BulkIngredients.toRows("Соль — по вкусу", emptyList()).single()
        assertThat(row.unit).isEqualTo(MeasureUnit.TO_TASTE)
        assertThat(row.amountText).isEmpty()
        assertThat(BulkIngredients.productUnit(row)).isEqualTo(MeasureUnit.G)
    }

    @Test fun duplicatesStartUncheckedAndAreNotAdded() {
        val rows = BulkIngredients.toRows("2 яйца\n200 мл молока\n3 яйца", existingNames = listOf("Молока"))
        assertThat(rows.map { it.duplicate }).containsExactly(false, true, true).inOrder()
        assertThat(BulkIngredients.accepted(rows).map { it.name }).containsExactly("яйца")
        val forced = rows.mapIndexed { i, r -> if (i == 2) r.copy(include = true) else r }
        assertThat(BulkIngredients.accepted(forced)).hasSize(2)
    }

    @Test fun rowsWithClearedNameAreSkipped() {
        val rows = BulkIngredients.toRows("2 onions\n1 tsp salt", emptyList())
        val edited = listOf(rows[0].copy(name = "  "), rows[1])
        assertThat(BulkIngredients.accepted(edited).map { it.name }).containsExactly("salt")
    }
}
