package com.example.homemenu.ui.recipes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.homemenu.data.repo.RecipeRepository
import com.example.homemenu.domain.IngredientLineParser
import com.example.homemenu.domain.ParsedRecipeText
import com.example.homemenu.domain.model.Department
import com.example.homemenu.domain.model.Step
import com.example.homemenu.ui.theme.TagPalette
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Pure mapping from parsed recipe text (Task 5) to the edit form used everywhere else.
 * Only values found in the text are filled in; unrecognized lines are kept in the notes.
 * Unit-tested in RecipeTextImportTest.
 */
object RecipeTextImport {
    const val UNRECOGNIZED_HEADER = "Не распознано при импорте:"

    /** [existingTags] = (id, name) of tags already in the app. Ingredients are handled separately as [BulkRow]s. */
    fun toForm(p: ParsedRecipeText, existingTags: List<Pair<Long, String>>): EditForm {
        val notes = listOfNotNull(
            p.notes.trim().takeIf { it.isNotEmpty() },
            p.unrecognized.takeIf { it.isNotEmpty() }?.let { UNRECOGNIZED_HEADER + "\n" + it.joinToString("\n") },
        ).joinToString("\n\n")
        return EditForm(
            name = p.title.trim().take(120),
            description = p.description.trim().take(1000),
            timeText = p.cookingTimeMin?.takeIf { it > 0 }?.toString() ?: "",
            prepTimeText = p.prepTimeMin?.takeIf { it > 0 }?.toString() ?: "",
            ovenTempText = p.ovenTempC?.takeIf { it > 0 }?.toString() ?: "",
            caloriesText = p.calories?.takeIf { it >= 0 }?.toString() ?: "",
            servingsText = p.servings?.takeIf { it > 0 }?.toString() ?: EditForm().servingsText,
            steps = p.steps.map { Step(it.take(2000)) }.ifEmpty { listOf(Step("")) },
            notes = notes.take(4000),
            tagIds = matchTags(p.tags, existingTags).first,
        )
    }

    /** Existing tag ids that match [names] (case/ё-insensitive) and the names that have no tag yet, deduplicated. */
    fun matchTags(names: List<String>, existing: List<Pair<Long, String>>): Pair<Set<Long>, List<String>> {
        val ids = linkedSetOf<Long>()
        val missing = mutableListOf<String>()
        names.forEach { n ->
            val key = IngredientLineParser.normalizeName(n)
            if (key.isEmpty()) return@forEach
            val hit = existing.firstOrNull { IngredientLineParser.normalizeName(it.second) == key }
            if (hit != null) ids += hit.first
            else if (missing.none { IngredientLineParser.normalizeName(it) == key }) missing += n.trim().take(30)
        }
        return ids to missing
    }

    /** Ingredient section as lines for [BulkIngredientsDialog] (re-parsed there into editable rows). */
    fun ingredientText(p: ParsedRecipeText): String = p.ingredients.joinToString("\n") { it.raw }

    /** Rows that RecipeFormValidator would reject: an amount is required but missing or zero. */
    fun rowsMissingAmount(rows: List<BulkRow>): List<BulkRow> = rows.filter { r ->
        val amountless = r.unit?.isAmountless == true
        !amountless && (RecipeFormValidator.parseAmount(r.amountText) ?: 0.0) <= 0.0
    }
}

/** Saves a recipe confirmed in [TextImportDialog]. Existing recipes are never touched: always a new recipe. */
@HiltViewModel
class TextImportViewModel @Inject constructor(
    private val repo: RecipeRepository,
) : ViewModel() {
    val products = repo.observeProducts().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val tags = repo.observeTags().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun clearError() { _error.value = null }

    /**
     * Products are matched by normalized name or created (in row order), missing tags are created,
     * the form is validated like the edit screen and saved as a new recipe (id = 0).
     */
    fun save(form: EditForm, rows: List<BulkRow>, newTags: List<String>, onSaved: (Long) -> Unit) {
        if (_busy.value) return
        viewModelScope.launch {
            _busy.value = true
            val result = runCatching {
                val known = products.value.toMutableList()
                val ingredients = rows.map { row ->
                    val product = BulkIngredients.findProduct(row.name, known)
                        ?: repo.addProduct(row.name.trim(), BulkIngredients.productUnit(row), Department.OTHER).also { known += it }
                    BulkIngredients.toEditIngredient(row, product)
                }
                val created = newTags.mapIndexed { i, name -> repo.addTag(name, TagPalette[i % TagPalette.size]) }
                val allTags = tags.value + created
                val f = form.copy(id = 0, ingredients = ingredients, tagIds = form.tagIds + created.map { it.id })
                val errs = RecipeFormValidator.validate(f)
                if (!errs.isEmpty) error(listOfNotNull(errs.name, errs.servings, errs.ingredients, errs.time).joinToString("; "))
                repo.save(RecipeFormValidator.toRecipe(f, allTags))
            }
            _busy.value = false
            result.onSuccess { id -> onSaved(id) }
                .onFailure { _error.value = it.message ?: "Не удалось сохранить рецепт" }
        }
    }
}
