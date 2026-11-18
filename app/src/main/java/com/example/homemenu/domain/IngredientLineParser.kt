package com.example.homemenu.domain

import com.example.homemenu.domain.model.MeasureUnit

/**
 * One line of a pasted ingredient list after parsing.
 * [recognized] = an amount (or "по вкусу") was understood. When false, the whole line is kept in [name]
 * with no amount/unit so the user can correct it manually: lines are never dropped silently.
 */
data class ParsedIngredientLine(
    val raw: String,
    val name: String,
    val amount: Double?,
    val unit: MeasureUnit?,
    val recognized: Boolean,
)

/**
 * Pure parser for bulk ingredient input (Task 1). No Android dependencies, fully unit-tested.
 *
 * Supported shapes (order of lines is preserved, empty lines are ignored):
 *  - amount first: "500 g chicken breast", "2 ст. л. сахара", "1,5 кг муки", "500г курицы", "½ ч.л. соды";
 *  - amount last: "Молоко — 200 мл", "Соль 1 ч. л.", "Яйца 2";
 *  - "по вкусу" / "to taste" anywhere: "Соль, перец — по вкусу" -> TO_TASTE, amount 0.
 * A number without a known unit means pieces ("2 onions" -> 2 шт). kg and l are converted to g and ml
 * because the app has no kg/l units. A range "2-3" keeps the upper bound.
 */
object IngredientLineParser {

    private val BULLET = Regex("^[\\-–—•*·▪●]+\\s*")
    private val NUMBERING = Regex("^\\d{1,2}[.)]\\s+")
    private const val NUM = "\\d+(?:[.,]\\d+)?"
    private const val AMOUNT =
        "(?:\\d+\\s+\\d+\\s*/\\s*\\d+|\\d+\\s*/\\s*\\d+|$NUM\\s*[-–]\\s*$NUM|\\d*[½¼¾⅓⅔]|$NUM)"
    private val LEADING = Regex("^($AMOUNT)\\s*(.*)$")
    private val TRAILING = Regex("^(.*\\S)\\s*[-–—:,]?\\s+($AMOUNT)\\s*(.*)$")
    private val TO_TASTE = Regex("(?iu)(по\\s+вкусу|to\\s+taste)")
    private val SPACES = Regex("\\s+")
    private const val EDGE_SEPARATORS = "-–—:,;. "

    private val FRACTIONS = mapOf('\u00bd' to 0.5, '\u00bc' to 0.25, '\u00be' to 0.75, '\u2153' to 1.0 / 3, '\u2154' to 2.0 / 3)

    /** Unit synonyms, normalized (lowercase, dots removed, single spaces) -> unit and multiplier. */
    private val UNITS: Map<String, Pair<MeasureUnit, Double>> = buildMap {
        fun add(unit: MeasureUnit, factor: Double, vararg keys: String) = keys.forEach { put(it, unit to factor) }
        add(MeasureUnit.G, 1.0, "г", "гр", "грамм", "грамма", "граммов", "g", "gr", "gram", "grams")
        add(MeasureUnit.G, 1000.0, "кг", "килограмм", "килограмма", "килограммов", "kg")
        add(MeasureUnit.ML, 1.0, "мл", "миллилитр", "миллилитра", "миллилитров", "ml")
        add(MeasureUnit.ML, 1000.0, "л", "литр", "литра", "литров", "l")
        add(MeasureUnit.PCS, 1.0, "шт", "штука", "штуки", "штук", "pcs", "pc", "piece", "pieces")
        add(
            MeasureUnit.TBSP, 1.0, "ст л", "стл", "столовая ложка", "столовые ложки", "столовых ложек",
            "tbsp", "tablespoon", "tablespoons",
        )
        add(
            MeasureUnit.TSP, 1.0, "ч л", "чл", "чайная ложка", "чайные ложки", "чайных ложек",
            "tsp", "teaspoon", "teaspoons",
        )
        add(MeasureUnit.PINCH, 1.0, "щепотка", "щепотки", "щепоток", "щепоть", "pinch", "pinches")
    }

    /** Every non-empty line becomes one entry, in the original order. */
    fun parseLines(text: String): List<ParsedIngredientLine> =
        text.lines().map { it.trim() }.filter { it.isNotEmpty() }.map { parseLine(it) }

    fun parseLine(line: String): ParsedIngredientLine {
        val raw = line.trim()
        val body = raw.replace(BULLET, "").replace(NUMBERING, "").replace(SPACES, " ").trim()
        val unrecognized = ParsedIngredientLine(raw, body.ifEmpty { raw }, null, null, false)
        if (body.isEmpty()) return unrecognized

        if (TO_TASTE.containsMatchIn(body)) {
            val name = cleanName(body.replace(TO_TASTE, " "))
            return if (name.isEmpty()) unrecognized else ParsedIngredientLine(raw, name, 0.0, MeasureUnit.TO_TASTE, true)
        }

        val leading = LEADING.matchEntire(body)
        val leadingAmount = leading?.let { parseAmount(it.groupValues[1]) }
        if (leading != null && leadingAmount != null) {
            val rest = leading.groupValues[2]
            val unit = matchUnit(rest)
            val name = cleanName(if (unit != null) unit.remainder else rest)
            if (name.isEmpty()) return unrecognized
            val u = unit?.unit ?: MeasureUnit.PCS
            val factor = unit?.factor ?: 1.0
            return ParsedIngredientLine(raw, name, leadingAmount * factor, u, true)
        }

        val trailing = TRAILING.matchEntire(body)
        val trailingAmount = trailing?.let { parseAmount(it.groupValues[2]) }
        if (trailing != null && trailingAmount != null) {
            val rest = trailing.groupValues[3].trim()
            val unit: UnitMatch? = if (rest.isEmpty()) null else (matchUnit(rest) ?: return unrecognized)
            val name = cleanName(trailing.groupValues[1] + " " + (unit?.remainder ?: ""))
            if (name.isEmpty()) return unrecognized
            val u = unit?.unit ?: MeasureUnit.PCS
            val factor = unit?.factor ?: 1.0
            return ParsedIngredientLine(raw, name, trailingAmount * factor, u, true)
        }

        return unrecognized
    }

    /** Amount text -> number. Supports "2", "2.5", "2,5", "1/2", "1 1/2", "2-3" (upper bound), "½", "1½". */
    fun parseAmount(text: String): Double? {
        val t = text.replace(',', '.').replace(SPACES, " ").trim()
        Regex("^(\\d+) (\\d+) ?/ ?(\\d+)$").matchEntire(t)?.let {
            val d = it.groupValues[3].toDouble()
            return if (d == 0.0) null else it.groupValues[1].toDouble() + it.groupValues[2].toDouble() / d
        }
        Regex("^(\\d+) ?/ ?(\\d+)$").matchEntire(t)?.let {
            val d = it.groupValues[2].toDouble()
            return if (d == 0.0) null else it.groupValues[1].toDouble() / d
        }
        Regex("^([\\d.]+) ?[-–] ?([\\d.]+)$").matchEntire(t)?.let { return it.groupValues[2].toDoubleOrNull() }
        Regex("^(\\d*)([½¼¾⅓⅔])$").matchEntire(t)?.let {
            val whole = it.groupValues[1].toDoubleOrNull() ?: 0.0
            return whole + FRACTIONS.getValue(it.groupValues[2][0])
        }
        return t.toDoubleOrNull()
    }

    /** Normalized name used for duplicate detection. */
    fun normalizeName(name: String): String =
        name.lowercase().replace('\u0451', '\u0435').replace(SPACES, " ").trim()

    /**
     * Indices of lines whose name repeats an earlier line of the same paste or one of [existingNames]
     * (ingredients already in the recipe). The UI marks them so the user decides; nothing is removed here.
     */
    fun duplicateIndices(lines: List<ParsedIngredientLine>, existingNames: Collection<String> = emptyList()): Set<Int> {
        val seen = existingNames.map { normalizeName(it) }.filter { it.isNotEmpty() }.toMutableSet()
        val result = mutableSetOf<Int>()
        lines.forEachIndexed { index, line ->
            val key = normalizeName(line.name)
            if (key.isNotEmpty() && !seen.add(key)) result += index
        }
        return result
    }

    private class UnitMatch(val unit: MeasureUnit, val factor: Double, val remainder: String)

    /** Tries 3, 2, then 1 leading words of [text] as a unit ("ст. л.", "чайная ложка", "г"). */
    private fun matchUnit(text: String): UnitMatch? {
        val tokens = text.trim().split(SPACES).filter { it.isNotEmpty() }
        for (n in minOf(3, tokens.size) downTo 1) {
            val key = tokens.take(n).joinToString(" ") { it.lowercase().replace(".", "").trimEnd(',') }.trim()
            val hit = UNITS[key] ?: UNITS[key.replace(" ", "")]
            if (hit != null) return UnitMatch(hit.first, hit.second, tokens.drop(n).joinToString(" "))
        }
        return null
    }

    private fun cleanName(text: String): String =
        text.replace(SPACES, " ").trim().trim { it in EDGE_SEPARATORS }.trim()
}
