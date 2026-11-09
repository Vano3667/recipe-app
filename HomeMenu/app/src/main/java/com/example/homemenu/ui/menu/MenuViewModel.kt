package com.example.homemenu.ui.menu

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.homemenu.data.repo.MenuRepository
import com.example.homemenu.data.repo.RecipeRepository
import com.example.homemenu.domain.GeneratorOptions
import com.example.homemenu.domain.MenuGeneratorUseCase
import com.example.homemenu.domain.model.MealSlot
import com.example.homemenu.domain.model.MenuEntry
import com.example.homemenu.domain.model.Recipe
import com.example.homemenu.domain.model.Tag
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import javax.inject.Inject

data class DateRange(val from: LocalDate, val to: LocalDate) {
    init { require(!to.isBefore(from)) { "to < from" } }
    val days: List<LocalDate> get() = (0..ChronoUnit.DAYS.between(from, to)).map { from.plusDays(it) }
    val length: Long get() = ChronoUnit.DAYS.between(from, to) + 1
    fun shift(sign: Int): DateRange = DateRange(from.plusDays(sign * length), to.plusDays(sign * length))
    val isCalendarWeek: Boolean get() = from.dayOfWeek == DayOfWeek.MONDAY && length == 7L

    companion object {
        fun weekOf(date: LocalDate): DateRange {
            val monday = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            return DateRange(monday, monday.plusDays(6))
        }
        fun day(date: LocalDate) = DateRange(date, date)
    }
}

data class MenuSummary(
    val plannedDishes: Int,
    val emptySlots: Int,
    val uniqueRecipes: Int,
    val totalServings: Int,
    val avgKcalPerDay: Int?,
)

data class MenuUiState(
    val loading: Boolean = true,
    val range: DateRange = DateRange.weekOf(LocalDate.now()),
    val entries: List<MenuEntry> = emptyList(),
    val recipes: Map<Long, Recipe> = emptyMap(),
    val tags: List<Tag> = emptyList(),
) {
    fun entry(date: LocalDate, slot: MealSlot) = entries.firstOrNull { it.date == date && it.slot == slot }

    val summary: MenuSummary get() {
        val valid = entries.filter { recipes.containsKey(it.recipeId) }
        val days = range.days
        val kcalKnown = valid.filter { recipes[it.recipeId]?.calories != null }
        val totalKcal = kcalKnown.sumOf { (recipes[it.recipeId]!!.calories ?: 0) * it.servings }
        return MenuSummary(
            plannedDishes = valid.size,
            emptySlots = days.size * MealSlot.entries.size - entries.size,
            uniqueRecipes = valid.map { it.recipeId }.distinct().size,
            totalServings = valid.sumOf { it.servings },
            avgKcalPerDay = if (kcalKnown.isEmpty()) null else totalKcal / days.size,
        )
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class MenuViewModel @Inject constructor(
    private val menu: MenuRepository,
    private val recipes: RecipeRepository,
    private val generator: MenuGeneratorUseCase,
) : ViewModel() {
    private val range = MutableStateFlow(DateRange.weekOf(LocalDate.now()))

    val state: StateFlow<MenuUiState> = combine(
        range,
        range.flatMapLatest { menu.observeRange(it.from, it.to) },
        recipes.observeRecipes(),
        recipes.observeTags(),
    ) { r, entries, all, tags ->
        MenuUiState(false, r, entries.filter { !it.date.isBefore(r.from) && !it.date.isAfter(r.to) }, all.associateBy { it.id }, tags)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MenuUiState())

    fun previous() { range.value = range.value.shift(-1) }
    fun next() { range.value = range.value.shift(1) }
    fun thisWeek() { range.value = DateRange.weekOf(LocalDate.now()) }
    fun setRange(from: LocalDate, to: LocalDate) { range.value = DateRange(minOf(from, to), maxOf(from, to)) }

    fun setSlot(date: LocalDate, slot: MealSlot, recipeId: Long, servings: Int) = viewModelScope.launch {
        menu.setSlot(date, slot, recipeId, servings.coerceAtLeast(1))
    }
    fun updateServings(entryId: Long, servings: Int) = viewModelScope.launch { menu.updateServings(entryId, servings.coerceAtLeast(1)) }
    fun clear(entryId: Long) = viewModelScope.launch { menu.clear(entryId) }
    fun move(entryId: Long, toDate: LocalDate, toSlot: MealSlot) = viewModelScope.launch { menu.move(entryId, toDate, toSlot) }

    /** Returns how many slots were filled via [onDone]. */
    fun generate(options: GeneratorOptions, onDone: (Int) -> Unit) = viewModelScope.launch {
        val s = state.value
        val result = generator.generate(s.range.days, s.entries, s.recipes.values.toList(), options)
        menu.applyGenerated(result)
        onDone(result.size)
    }
}
