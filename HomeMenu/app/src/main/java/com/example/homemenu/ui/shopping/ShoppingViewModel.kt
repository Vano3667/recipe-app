package com.example.homemenu.ui.shopping

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.homemenu.data.repo.MenuRepository
import com.example.homemenu.data.repo.RecipeRepository
import com.example.homemenu.data.repo.ShoppingRepository
import com.example.homemenu.domain.DepartmentGroup
import com.example.homemenu.domain.PlannedDish
import com.example.homemenu.domain.ShoppingListUseCase
import com.example.homemenu.ui.components.rangeLabel
import com.example.homemenu.ui.menu.DateRange
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

enum class ShoppingMode(val title: String) { DAY("День"), WEEK("Неделя"), RANGE("Период") }

data class ShoppingUiState(
    val loading: Boolean = true,
    val mode: ShoppingMode = ShoppingMode.WEEK,
    val range: DateRange = DateRange.weekOf(LocalDate.now()),
    val groups: List<DepartmentGroup> = emptyList(),
    val checked: Set<String> = emptySet(),
    val dishes: Int = 0,
) {
    val total: Int get() = groups.sumOf { it.items.size }
    val done: Int get() = groups.sumOf { g -> g.items.count { it.key in checked } }
    val progress: Float get() = if (total == 0) 0f else done.toFloat() / total
}

private data class Selection(val mode: ShoppingMode, val range: DateRange)

@HiltViewModel
class ShoppingViewModel @Inject constructor(
    private val menuRepo: MenuRepository,
    private val recipeRepo: RecipeRepository,
    private val shoppingRepo: ShoppingRepository,
    private val useCase: ShoppingListUseCase,
) : ViewModel() {

    private val selection = MutableStateFlow(Selection(ShoppingMode.WEEK, DateRange.weekOf(LocalDate.now())))

    @OptIn(ExperimentalCoroutinesApi::class)
    val state: StateFlow<ShoppingUiState> = selection.flatMapLatest { sel ->
        combine(
            menuRepo.observeRange(sel.range.from, sel.range.to),
            recipeRepo.observeRecipes(),
            shoppingRepo.observeChecked(sel.range.from, sel.range.to),
        ) { entries, recipes, checked ->
            val byId = recipes.associateBy { it.id }
            // Slots whose recipe was deleted are skipped.
            val dishes = entries.mapNotNull { e -> byId[e.recipeId]?.let { PlannedDish(it, e.servings) } }
            ShoppingUiState(false, sel.mode, sel.range, useCase.build(dishes), checked, dishes.size)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ShoppingUiState())

    fun setMode(mode: ShoppingMode) {
        val anchor = selection.value.range.from.let { if (selection.value.range.days.contains(LocalDate.now())) LocalDate.now() else it }
        val range = when (mode) {
            ShoppingMode.DAY -> DateRange.day(anchor)
            ShoppingMode.WEEK -> DateRange.weekOf(anchor)
            ShoppingMode.RANGE -> selection.value.range
        }
        selection.value = Selection(mode, range)
    }

    fun setRange(from: LocalDate, to: LocalDate) {
        selection.value = Selection(ShoppingMode.RANGE, DateRange(from, to))
    }

    fun previous() { selection.value = selection.value.let { it.copy(range = it.range.shift(-1)) } }
    fun next() { selection.value = selection.value.let { it.copy(range = it.range.shift(1)) } }

    fun toggle(key: String) {
        val s = state.value
        viewModelScope.launch { shoppingRepo.setChecked(s.range.from, s.range.to, key, key !in s.checked) }
    }

    fun clearChecks() {
        val r = state.value.range
        viewModelScope.launch { shoppingRepo.clearChecks(r.from, r.to) }
    }

    fun shareText(): String {
        val s = state.value
        return useCase.shareText("Покупки: ${rangeLabel(s.range.from, s.range.to)}", s.groups, s.checked)
    }
}
