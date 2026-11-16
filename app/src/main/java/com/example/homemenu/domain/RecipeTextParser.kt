package com.example.homemenu.domain

/**
 * Result of parsing free recipe text (Task 5: import from pasted text, messengers, TXT files).
 * Every field holds only what was found in the text; nothing is invented. Lines that could not be
 * placed are kept in [unrecognized] so the preview can show them for manual correction.
 */
data class ParsedRecipeText(
    val title: String,
    val description: String,
    val ingredients: List<ParsedIngredientLine>,
    val steps: List<String>,
    val prepTimeMin: Int?,
    val cookingTimeMin: Int?,
    val servings: Int?,
    val ovenTempC: Int?,
    val calories: Int?,
    val tags: List<String>,
    val notes: String,
    val unrecognized: List<String>,
) {
    val isEmpty: Boolean get() = title.isBlank() && ingredients.isEmpty() && steps.isEmpty()
}

/**
 * Pure, Android-free parser for recipe text. Tolerant of common shapes:
 *  - the app's own share text (`recipeShareText` in ui/recipes/RecipeDetailRoute.kt);
 *  - section headers «Ингредиенты», «Состав», «Приготовление», «Способ приготовления», "Ingredients", "Instructions"...;
 *  - key lines «Подготовка: 10 мин», «Время приготовления: 1 ч 30 мин», «Порции: 4», «На 4 порции», "Serves 2",
 *    «Духовка: 180 °C», «Калорийность: 250 ккал», «Теги: a, b», `#a #b`, «Заметки: ...»;
 *  - numbered or bulleted steps, "Шаг 1." / "Step 1:".
 * Ingredient lines go through [IngredientLineParser]; unknown ones stay as unrecognized ingredient rows.
 */
object RecipeTextParser {

    private const val SEP = "\\s*[:\\-–—]\\s*"
    private const val SEP_OPT = "(?:\\s*[:\\-–—]\\s*|\\s+)"

    private val SPACES = Regex("\\s+")
    private val TITLE_PREFIX = Regex("(?iu)^(?:рецепт|recipe|название|title)\\s*[:\\-–—]\\s*")
    private val LEADING_SYMBOLS = Regex("^[^\\p{L}\\p{N}«\"(]+")

    private val INGREDIENTS_HEADER = Regex(
        "(?iu)^(?:ингредиенты|ингридиенты|состав|продукты|вам понадобится|понадобится|что нужно|ingredients)" +
            "(?:\\s+на\\s+([^:]*)|\\s*\\(([^)]*)\\))?\\s*:?\\s*$",
    )
    private val STEPS_HEADER = Regex(
        "(?iu)^(?:приготовление|способ приготовления|ход приготовления|как готовить|инструкция|шаги|" +
            "method|instructions|directions|steps|preparation)\\s*(?::\\s*(.*))?$",
    )
    private val NOTES_HEADER = Regex("(?iu)^(?:заметки|заметка|примечание|примечания|совет|notes?|tips?)\\s*:\\s*(.*)$")

    private val PREP = Regex("(?iu)^(?:время\\s+подготовки|подготовка|prep(?:aration)?\\s+time|prep)$SEP(.+)$")
    private val COOK = Regex(
        "(?iu)^(?:общее\\s+время|время\\s+приготовления|время\\s+готовки|время|готовка|" +
            "cook(?:ing)?\\s+time|total\\s+time|time)$SEP(.+)$",
    )
    private val SERVINGS = Regex("(?iu)^(?:количество\\s+порций|порции|порций|порция|servings|serves|yield)$SEP_OPT(\\d+).*$")
    private val SERVINGS_NA = Regex("(?iu)^на\\s+(\\d+)\\s+(?:порци|персон|человек).*$")
    private val OVEN = Regex("(?iu)^(?:духовка|температура\\s+духовки|температура|oven)$SEP_OPT(\\d{2,3}).*$")
    private val CALORIES = Regex("(?iu)^(?:калорийность|калории|ккал|calories|kcal)$SEP_OPT(\\d+).*$")
    private val TAGS = Regex("(?iu)^(?:теги|тэги|метки|tags)$SEP(.+)$")
    private val HASHTAGS = Regex("^(?:#[\\p{L}\\p{N}_]+\\s*)+$")
    private val FOOTER = Regex("(?iu)из приложения\\s+«?домашнее меню")

    private val STEP_PREFIX = Regex("(?iu)^(?:(?:шаг|step)\\s*\\d+\\s*[.:)\\-–—]?\\s*|\\d{1,2}\\s*[.)]\\s+|[\\-–—•*·▪●]+\\s*)")
    private val BULLET = Regex("^[\\-–—•*·▪●]+\\s*")
    private val NUMBERED = Regex("^\\d{1,2}[.)]\\s+")
    private val CATEGORY_LINE = Regex("^[^·]{1,30} · [^·]{1,30}$")
    private val DIGITS = Regex("\\d+")

    private val HOURS = Regex("(?iu)(\\d+(?:[.,]\\d+)?)\\s*(?:ч|час|часа|часов|h|hr|hrs|hour|hours)(?!\\p{L})")
    private val MINUTES = Regex("(?iu)(\\d+)\\s*(?:м|мин|минут|минуты|минута|min|mins|minute|minutes)(?!\\p{L})")

    private enum class Section { HEADER, INGREDIENTS, STEPS, NOTES }

    private class Builder {
        var title = ""
        val description = mutableListOf<String>()
        val ingredients = mutableListOf<ParsedIngredientLine>()
        val steps = mutableListOf<String>()
        val notes = mutableListOf<String>()
        val tags = mutableListOf<String>()
        val unrecognized = mutableListOf<String>()
        var prep: Int? = null
        var cook: Int? = null
        var servings: Int? = null
        var oven: Int? = null
        var calories: Int? = null
        var section = Section.HEADER
    }

    fun parse(text: String): ParsedRecipeText {
        val b = Builder()
        val lines = text.replace("\r\n", "\n").replace('\r', '\n').lines()
        for (rawLine in lines) {
            val line = rawLine.replace('\u00a0', ' ').replace(SPACES, " ").trim()
            if (line.isEmpty() || FOOTER.containsMatchIn(line)) continue
            if (b.keyLine(line)) continue
            if (b.headerLine(line)) continue
            b.contentLine(line)
        }
        return ParsedRecipeText(
            title = b.title,
            description = b.description.joinToString("\n"),
            ingredients = b.ingredients.toList(),
            steps = b.steps.toList(),
            prepTimeMin = b.prep,
            cookingTimeMin = b.cook,
            servings = b.servings,
            ovenTempC = b.oven,
            calories = b.calories,
            tags = b.tags.toList(),
            notes = b.notes.joinToString("\n"),
            unrecognized = b.unrecognized.toList(),
        )
    }

    /** "45 мин", "1 ч 30 мин", "1,5 часа", "2 hours", bare "20" (minutes). Null when nothing is understood. */
    fun parseMinutes(text: String): Int? {
        val t = text.trim()
        val h = HOURS.find(t)?.groupValues?.get(1)?.replace(',', '.')?.toDoubleOrNull()
        val m = MINUTES.find(t)?.groupValues?.get(1)?.toIntOrNull()
        if (h == null && m == null) {
            return Regex("^(\\d{1,3})$").matchEntire(t)?.groupValues?.get(1)?.toIntOrNull()?.takeIf { it > 0 }
        }
        val total = Math.round((h ?: 0.0) * 60).toInt() + (m ?: 0)
        return total.takeIf { it > 0 }
    }

    /** Key-value lines are recognized in every section (times are often written after the steps). */
    private fun Builder.keyLine(line: String): Boolean {
        PREP.matchEntire(line)?.let { m -> parseMinutes(m.groupValues[1])?.let { prep = it; return true } }
        COOK.matchEntire(line)?.let { m -> parseMinutes(m.groupValues[1])?.let { cook = it; return true } }
        SERVINGS.matchEntire(line)?.let { m -> m.groupValues[1].toIntOrNull()?.takeIf { it > 0 }?.let { servings = it; return true } }
        SERVINGS_NA.matchEntire(line)?.let { m -> m.groupValues[1].toIntOrNull()?.takeIf { it > 0 }?.let { servings = it; return true } }
        OVEN.matchEntire(line)?.let { m -> m.groupValues[1].toIntOrNull()?.let { oven = it; return true } }
        CALORIES.matchEntire(line)?.let { m -> m.groupValues[1].toIntOrNull()?.let { calories = it; return true } }
        TAGS.matchEntire(line)?.let { m -> addTags(m.groupValues[1]); return true }
        if (HASHTAGS.matches(line)) { addTags(line); return true }
        return false
    }

    private fun Builder.headerLine(line: String): Boolean {
        NOTES_HEADER.matchEntire(line)?.let { m ->
            section = Section.NOTES
            m.groupValues[1].trim().takeIf { it.isNotEmpty() }?.let { notes += it }
            return true
        }
        INGREDIENTS_HEADER.matchEntire(line)?.let { m ->
            section = Section.INGREDIENTS
            val extra = m.groupValues[1].ifEmpty { m.groupValues[2] }
            if (servings == null) DIGITS.find(extra)?.value?.toIntOrNull()?.takeIf { it > 0 }?.let { servings = it }
            return true
        }
        STEPS_HEADER.matchEntire(line)?.let { m ->
            val rest = m.groupValues[1].trim()
            val minutes = if (rest.length <= 20) parseMinutes(rest) else null
            if (minutes != null) {
                if (cook == null) cook = minutes
            } else {
                section = Section.STEPS
                if (rest.isNotEmpty()) steps += rest
            }
            return true
        }
        return false
    }

    private fun Builder.contentLine(line: String) {
        when (section) {
            Section.INGREDIENTS -> ingredients += IngredientLineParser.parseLine(line)
            Section.STEPS -> line.replace(STEP_PREFIX, "").trim().takeIf { it.isNotEmpty() }?.let { steps += it }
            Section.NOTES -> notes += line
            Section.HEADER -> headerContent(line)
        }
    }

    private fun Builder.headerContent(line: String) {
        if (title.isEmpty()) {
            val t = line.replace(TITLE_PREFIX, "").replace(LEADING_SYMBOLS, "").trim()
            if (t.isNotEmpty()) { title = t; return }
        }
        if (BULLET.containsMatchIn(line)) {
            val parsed = IngredientLineParser.parseLine(line)
            if (parsed.recognized) { ingredients += parsed; return }
        }
        if (NUMBERED.containsMatchIn(line)) {
            line.replace(STEP_PREFIX, "").trim().takeIf { it.isNotEmpty() }?.let { steps += it; return }
        }
        if (CATEGORY_LINE.matches(line)) { unrecognized += line; return }
        description += line
    }

    private fun Builder.addTags(text: String) {
        text.split(',', ';', '#').map { it.trim().trim('#').trim() }.filter { it.isNotEmpty() }.forEach { tag ->
            if (tags.none { it.equals(tag, ignoreCase = true) }) tags += tag
        }
    }
}
