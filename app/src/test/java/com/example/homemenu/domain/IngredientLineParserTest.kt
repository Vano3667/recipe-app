package com.example.homemenu.domain

import com.example.homemenu.domain.model.MeasureUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class IngredientLineParserTest {

    private fun check(line: String, name: String, amount: Double, unit: MeasureUnit) {
        val p = IngredientLineParser.parseLine(line)
        assertTrue("recognized: $line", p.recognized)
        assertEquals("name: $line", name, p.name)
        assertEquals("amount: $line", amount, p.amount!!, 1e-9)
        assertEquals("unit: $line", unit, p.unit)
    }

    @Test
    fun specExample_keepsOrderAmountsAndUnits() {
        val text = "500 g chicken breast\n2 onions\n1 tsp salt\n200 ml cream\n3 cloves garlic"
        val lines = IngredientLineParser.parseLines(text)
        assertEquals(5, lines.size)
        assertEquals(listOf("chicken breast", "onions", "salt", "cream", "cloves garlic"), lines.map { it.name })
        assertEquals(listOf(500.0, 2.0, 1.0, 200.0, 3.0), lines.map { it.amount })
        assertEquals(
            listOf(MeasureUnit.G, MeasureUnit.PCS, MeasureUnit.TSP, MeasureUnit.ML, MeasureUnit.PCS),
            lines.map { it.unit },
        )
        assertTrue(lines.all { it.recognized })
    }

    @Test
    fun emptyLinesAndWhitespaceAreIgnored() {
        val lines = IngredientLineParser.parseLines("\n   \n  2 яйца  \n\n\t\n1 л молока\n")
        assertEquals(2, lines.size)
        assertEquals("яйца", lines[0].name)
        assertEquals("молока", lines[1].name)
        assertEquals(1000.0, lines[1].amount!!, 1e-9)
        assertEquals(MeasureUnit.ML, lines[1].unit)
    }

    @Test
    fun russianUnitsAndAmountFormats() {
        check("2 ст. л. сахара", "сахара", 2.0, MeasureUnit.TBSP)
        check("1 ч.л. соли", "соли", 1.0, MeasureUnit.TSP)
        check("1,5 кг муки", "муки", 1500.0, MeasureUnit.G)
        check("500г куриного филе", "куриного филе", 500.0, MeasureUnit.G)
        check("\u00bd ч.л. соды", "соды", 0.5, MeasureUnit.TSP)
        check("1/2 стакана", "стакана", 0.5, MeasureUnit.PCS)
        check("2-3 зубчика чеснока", "зубчика чеснока", 3.0, MeasureUnit.PCS)
        check("1 щепотка мускатного ореха", "мускатного ореха", 1.0, MeasureUnit.PINCH)
        check("2 столовые ложки масла", "масла", 2.0, MeasureUnit.TBSP)
    }

    @Test
    fun amountAtTheEnd() {
        check("Молоко \u2014 200 мл", "Молоко", 200.0, MeasureUnit.ML)
        check("Соль 1 ч. л.", "Соль", 1.0, MeasureUnit.TSP)
        check("Яйца 2", "Яйца", 2.0, MeasureUnit.PCS)
        check("Мука 2 сорта 300 г", "Мука 2 сорта", 300.0, MeasureUnit.G)
    }

    @Test
    fun toTaste() {
        check("Соль по вкусу", "Соль", 0.0, MeasureUnit.TO_TASTE)
        check("Соль, перец \u2014 по вкусу", "Соль, перец", 0.0, MeasureUnit.TO_TASTE)
        check("pepper to taste", "pepper", 0.0, MeasureUnit.TO_TASTE)
    }

    @Test
    fun bulletsAndNumberingAreStripped() {
        check("- 2 яйца", "яйца", 2.0, MeasureUnit.PCS)
        check("\u2022 100 г сыра", "сыра", 100.0, MeasureUnit.G)
        check("1) 200 мл сливок", "сливок", 200.0, MeasureUnit.ML)
    }

    @Test
    fun unrecognizedLinesAreKeptWhole() {
        val lines = IngredientLineParser.parseLines("Для соуса:\n500 г\nСахар 1 1/2 стакана")
        assertEquals(3, lines.size)
        lines.forEach {
            assertFalse(it.raw, it.recognized)
            assertNull(it.amount)
            assertNull(it.unit)
        }
        assertEquals("Для соуса:", lines[0].name)
        assertEquals("500 г", lines[1].name)
        assertEquals("Сахар 1 1/2 стакана", lines[2].name)
    }

    @Test
    fun duplicatesWithinPasteAndAgainstExisting() {
        val lines = IngredientLineParser.parseLines("100 г Соль\n1 ч. л. соль\n200 г сахар\n2 Ёжика\n1 ежика")
        val dup = IngredientLineParser.duplicateIndices(lines, existingNames = listOf("Сахар"))
        assertEquals(setOf(1, 2, 4), dup)
    }

    @Test
    fun parseAmountVariants() {
        assertEquals(2.5, IngredientLineParser.parseAmount("2,5")!!, 1e-9)
        assertEquals(1.5, IngredientLineParser.parseAmount("1 1/2")!!, 1e-9)
        assertEquals(1.5, IngredientLineParser.parseAmount("1\u00bd")!!, 1e-9)
        assertNull(IngredientLineParser.parseAmount("1/0"))
    }
}
