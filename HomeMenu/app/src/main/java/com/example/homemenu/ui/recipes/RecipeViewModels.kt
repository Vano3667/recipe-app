package com.example.homemenu.ui.recipes

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.homemenu.data.files.BackupManager
import com.example.homemenu.data.files.PhotoStorage
import com.example.homemenu.data.repo.RecipeRepository
import com.example.homemenu.domain.RecipeQuery
import com.example.homemenu.domain.RecipeSearch
import com.example.homemenu.domain.ScalingUseCase
import com.example.homemenu.domain.model.Category
import com.example.homemenu.domain.model.Department
import com.example.homemenu.domain.model.Difficulty
import com.example.homemenu.domain.model.Ingredient
import com.example.homemenu.domain.model.MeasureUnit
import com.example.homemenu.domain.model.Product
import com.example.homemenu.domain.model.Recipe
import com.example.homemenu.domain.model.Step
import com.example.homemenu.domain.model.Tag
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class RecipesUiState(
    val loading: Boolean = true,
    val total: Int = 0,
    val visible: List<Recipe> = emptyList(),
    val tags: List<Tag> = emptyList(),
    val query: RecipeQuery = RecipeQuery(),
)

@HiltViewModel
class RecipesViewModel @Inject constructor(
    repo: RecipeRepository,
    private val search: RecipeSearch,
) : ViewModel() {
    private val query = MutableStateFlow(RecipeQuery())

    /** Selected recipe for the master-detail (expanded) layout. */
    val selectedId = MutableStateFlow<Long?>(null)

    val state: StateFlow<RecipesUiState> = combine(repo.observeRecipes(), repo.observeTags(), query) { recipes, tags, q ->
        RecipesUiState(loading = false, total = recipes.size, visible = search.apply(recipes, q), tags = tags, query = q)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RecipesUiState())

    fun updateQuery(transform: (RecipeQuery) -> RecipeQuery) = query.update(transform)
    fun select(id: Long?) { selectedId.value = id }
}

/** Detail + actions. Works for the nav route (id from SavedStateHandle) and for master-detail via [bind]. */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class RecipeDetailViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val repo: RecipeRepository,
    private val photos: PhotoStorage,
    private val backup: BackupManager,
    val scaling: ScalingUseCase,
) : ViewModel() {
    private val id = MutableStateFlow(savedState.get<Long>("id")?.takeIf { it > 0 })

    val recipe: StateFlow<Recipe?> = id.flatMapLatest { it?.let(repo::observeRecipe) ?: flowOf(null) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val loaded: StateFlow<Boolean> = recipe.map { true }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    fun bind(recipeId: Long) { id.value = recipeId }

    fun photoFile(name: String?): File? = photos.fileFor(name)

    suspend fun scheduledCount(recipeId: Long): Int = repo.scheduledCount(recipeId)

    fun delete(r: Recipe, clearSlots: Boolean, onDone: () -> Unit) = viewModelScope.launch {
        repo.delete(r.id, clearSlots)
        photos.delete(r.photoFile)
        r.steps.forEach { photos.delete(it.photoFile) }
        onDone()
    }

    suspend fun shareFile(r: Recipe): Uri = backup.recipeShareFile(r)
}

data class EditIngredient(val product: Product, val amountText: String, val unit: MeasureUnit)

data class EditForm(
    val id: Long = 0,
    val name: String = "",
    val photoFile: String? = null,
    val timeText: String = "",
    val difficulty: Difficulty = Difficulty.EASY,
    val category: Category = Category.DINNER,
    val cuisine: String = "",
    val caloriesText: String = "",
    val servingsText: String = "2",
    val ingredients: List<EditIngredient> = emptyList(),
    val steps: List<Step> = listOf(Step("")),
    val notes: String = "",
    val tagIds: Set<Long> = emptySet(),
    val createdAt: Long = 0,
)

data class EditErrors(val name: String? = null, val servings: String? = null, val ingredients: String? = null, val time: String? = null) {
    val isEmpty get() = name == null && servings == null && ingredients == null && time == null
}

object RecipeFormValidator {
    fun parseAmount(text: String): Double? = text.trim().replace(',', '.').toDoubleOrNull()

    fun validate(f: EditForm): EditErrors = EditErrors(
        name = if (f.name.isBlank()) "Введите название" else null,
        servings = f.servingsText.trim().toIntOrNull().let { if (it == null || it <= 0) "Порций должно быть больше 0" else null },
        ingredients = if (f.ingredients.any { (parseAmount(it.amountText) ?: 0.0) <= 0.0 }) "Количество каждого ингредиента должно быть больше 0" else null,
        time = f.timeText.trim().takeIf { it.isNotEmpty() }?.toIntOrNull().let { t ->
            if (f.timeText.isNotBlank() && (t == null || t < 0)) "Неверное время" else null
        },
    )

    fun toRecipe(f: EditForm, tags: List<Tag>): Recipe = Recipe(
        id = f.id,
        name = f.name.trim(),
        photoFile = f.photoFile,
        cookingTimeMin = f.timeText.trim().toIntOrNull() ?: 0,
        difficulty = f.difficulty,
        category = f.category,
        cuisine = f.cuisine.trim(),
        calories = f.caloriesText.trim().toIntOrNull()?.takeIf { it >= 0 },
        servings = f.servingsText.trim().toInt(),
        ingredients = f.ingredients.map { Ingredient(it.product, parseAmount(it.amountText)!!, it.unit) },
        steps = f.steps,
        notes = f.notes,
        tags = tags.filter { it.id in f.tagIds },
        createdAt = f.createdAt,
    )

    fun fromRecipe(r: Recipe) = EditForm(
        id = r.id, name = r.name, photoFile = r.photoFile, timeText = r.cookingTimeMin.takeIf { it > 0 }?.toString() ?: "",
        difficulty = r.difficulty, category = r.category, cuisine = r.cuisine, caloriesText = r.calories?.toString() ?: "",
        servingsText = r.servings.toString(),
        ingredients = r.ingredients.map { EditIngredient(it.product, formatAmount(it.amount), it.unit) },
        steps = r.steps.ifEmpty { listOf(Step("")) }, notes = r.notes, tagIds = r.tags.map { it.id }.toSet(), createdAt = r.createdAt,
    )

    fun formatAmount(a: Double): String = if (a == Math.floor(a)) a.toLong().toString() else a.toString().replace('.', ',')
}

@HiltViewModel
class RecipeEditViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val repo: RecipeRepository,
    private val photos: PhotoStorage,
) : ViewModel() {
    private val editId: Long = savedState.get<Long>("id")?.takeIf { it > 0 } ?: 0L

    private val _form = MutableStateFlow(EditForm())
    val form: StateFlow<EditForm> = _form.asStateFlow()
    private val _errors = MutableStateFlow(EditErrors())
    val errors: StateFlow<EditErrors> = _errors.asStateFlow()
    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    val products = repo.observeProducts().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val tags = repo.observeTags().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val events = Channel<EditEvent>(Channel.BUFFERED)
    val eventFlow = events.receiveAsFlow()

    val isNew get() = editId == 0L

    init {
        if (editId > 0) viewModelScope.launch {
            repo.getRecipe(editId)?.let { _form.value = RecipeFormValidator.fromRecipe(it) }
        }
    }

    fun update(transform: (EditForm) -> EditForm) {
        _form.update(transform)
        if (!_errors.value.isEmpty) _errors.value = RecipeFormValidator.validate(_form.value)
    }

    fun photoFile(name: String?) = photos.fileFor(name)
    fun cameraUri(): Uri = photos.newCameraUri()

    /** stepIndex == null → main recipe photo. */
    fun importPhoto(uri: Uri, stepIndex: Int? = null) = viewModelScope.launch {
        _busy.value = true
        runCatching { photos.importPhoto(uri) }
            .onSuccess { name ->
                update { f ->
                    if (stepIndex == null) f.copy(photoFile = name)
                    else f.copy(steps = f.steps.mapIndexed { i, s -> if (i == stepIndex) s.copy(photoFile = name) else s })
                }
            }
            .onFailure { events.send(EditEvent.Message(it.message ?: "Не удалось добавить фото")) }
        _busy.value = false
    }

    fun addCustomProduct(name: String, unit: MeasureUnit, department: Department, onAdded: (Product) -> Unit) = viewModelScope.launch {
        runCatching { repo.addProduct(name, unit, department) }
            .onSuccess(onAdded)
            .onFailure { events.send(EditEvent.Message(it.message ?: "Ошибка")) }
    }

    fun addCustomTag(name: String, color: String) = viewModelScope.launch {
        runCatching { repo.addTag(name, color) }
            .onSuccess { tag -> update { it.copy(tagIds = it.tagIds + tag.id) } }
            .onFailure { events.send(EditEvent.Message(it.message ?: "Ошибка")) }
    }

    fun save() = viewModelScope.launch {
        val f = _form.value
        val errs = RecipeFormValidator.validate(f)
        _errors.value = errs
        if (!errs.isEmpty) {
            events.send(EditEvent.Message("Проверьте отмеченные поля"))
            return@launch
        }
        runCatching { repo.save(RecipeFormValidator.toRecipe(f, tags.value)) }
            .onSuccess { events.send(EditEvent.Saved(it)) }
            .onFailure { events.send(EditEvent.Message(it.message ?: "Не удалось сохранить")) }
    }
}

sealed interface EditEvent {
    data class Saved(val id: Long) : EditEvent
    data class Message(val text: String) : EditEvent
}
