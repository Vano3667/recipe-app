package com.example.homemenu.ui.menu

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.homemenu.domain.GeneratorOptions
import com.example.homemenu.domain.model.MealSlot
import com.example.homemenu.domain.model.MenuEntry
import com.example.homemenu.domain.model.Recipe
import com.example.homemenu.domain.model.Tag
import com.example.homemenu.ui.components.EmptyState
import com.example.homemenu.ui.components.Illustration
import com.example.homemenu.ui.components.RangePickerDialog
import com.example.homemenu.ui.components.Stepper
import com.example.homemenu.ui.components.TagDot
import com.example.homemenu.ui.components.dayMonth
import com.example.homemenu.ui.components.rangeLabel
import com.example.homemenu.ui.components.weekdayName
import kotlinx.coroutines.launch
import java.time.LocalDate
import kotlin.math.roundToInt

private data class SlotKey(val date: LocalDate, val slot: MealSlot)
private data class PickTarget(val date: LocalDate, val slot: MealSlot)

@Composable
fun MenuRoute(onCook: (recipeId: Long, servings: Int) -> Unit, onAddRecipe: () -> Unit, vm: MenuViewModel = hiltViewModel()) {
    val state by vm.state.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    MenuContent(
        state = state,
        snackbar = snackbar,
        onPrev = vm::previous, onNext = vm::next, onThisWeek = vm::thisWeek, onRange = vm::setRange,
        onSetSlot = vm::setSlot, onServings = vm::updateServings, onClear = vm::clear, onMove = vm::move,
        onGenerate = { opts ->
            vm.generate(opts) { n ->
                scope.launch { snackbar.showSnackbar(if (n > 0) "Заполнено слотов: $n" else "Нет подходящих рецептов или свободных слотов") }
            }
        },
        onCook = onCook,
        onAddRecipe = onAddRecipe,
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MenuContent(
    state: MenuUiState,
    snackbar: SnackbarHostState,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onThisWeek: () -> Unit,
    onRange: (LocalDate, LocalDate) -> Unit,
    onSetSlot: (LocalDate, MealSlot, Long, Int) -> Unit,
    onServings: (Long, Int) -> Unit,
    onClear: (Long) -> Unit,
    onMove: (Long, LocalDate, MealSlot) -> Unit,
    onGenerate: (GeneratorOptions) -> Unit,
    onCook: (Long, Int) -> Unit,
    onAddRecipe: () -> Unit,
) {
    var showRange by remember { mutableStateOf(false) }
    var showGenerate by remember { mutableStateOf(false) }
    var pick by remember { mutableStateOf<PickTarget?>(null) }

    // Drag & drop state (root coordinates).
    val bounds = remember { mutableStateMapOf<SlotKey, Rect>() }
    var dragging by remember { mutableStateOf<MenuEntry?>(null) }
    var dragPos by remember { mutableStateOf(Offset.Zero) }
    var rootOffset by remember { mutableStateOf(Offset.Zero) }
    val hover = dragging?.let { bounds.entries.firstOrNull { (_, r) -> r.contains(dragPos) }?.key }
    val moveState = rememberUpdatedState(onMove)

    Scaffold(snackbarHost = { SnackbarHost(snackbar) }, containerColor = MaterialTheme.colorScheme.background) { padding ->
        Box(Modifier.padding(padding).fillMaxSize().onGloballyPositioned { rootOffset = it.positionInRoot() }) {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
                Text("Меню", style = MaterialTheme.typography.headlineLarge)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onPrev, modifier = Modifier.testTag("menu_prev")) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Назад") }
                    Text(rangeLabel(state.range.from, state.range.to), style = MaterialTheme.typography.titleMedium, modifier = Modifier.testTag("menu_range"))
                    IconButton(onClick = onNext, modifier = Modifier.testTag("menu_next")) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Вперёд") }
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(state.range.isCalendarWeek && state.range.days.contains(LocalDate.now()), onThisWeek, { Text("Эта неделя") })
                    FilterChip(!state.range.isCalendarWeek, { showRange = true }, { Text("Период…") }, leadingIcon = { Icon(Icons.Filled.DateRange, null, Modifier.size(18.dp)) })
                    FilledTonalButton(onClick = { showGenerate = true }, modifier = Modifier.testTag("generate_menu")) {
                        Icon(Icons.Filled.AutoAwesome, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Автоподбор")
                    }
                }
                Spacer(Modifier.height(12.dp))
                when {
                    state.loading -> Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                    state.recipes.isEmpty() -> EmptyState(
                        "Нечего планировать", "Сначала добавьте хотя бы один рецепт",
                        Illustration.CALENDAR, Modifier.heightIn(min = 360.dp), "Добавить рецепт", onAddRecipe,
                    )
                    else -> {
                        SummaryCard(state.summary)
                        Spacer(Modifier.height(12.dp))
                        if (state.entries.isEmpty()) {
                            Text(
                                "Меню на этот период пустое. Нажмите «+» в слоте или «Автоподбор». Долгое нажатие на блюдо — перетащить.",
                                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(Modifier.height(8.dp))
                        }
                        BoxWithConstraints {
                            val wide = maxWidth >= 600.dp
                            val dayWidth = if (wide) ((maxWidth - 12.dp * 2) / 3).coerceAtLeast(260.dp) else maxWidth
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                state.range.days.forEach { day ->
                                    DayCard(
                                        day = day, width = dayWidth, state = state, hover = hover, draggingId = dragging?.id,
                                        onBounds = { slot, r -> bounds[SlotKey(day, slot)] = r },
                                        onAdd = { slot -> pick = PickTarget(day, slot) },
                                        onServings = onServings, onClear = onClear, onCook = onCook,
                                        onDragStart = { entry, start -> dragging = entry; dragPos = start },
                                        onDrag = { delta -> dragPos += delta },
                                        onDragEnd = {
                                            val e = dragging
                                            val target = bounds.entries.firstOrNull { (_, r) -> r.contains(dragPos) }?.key
                                            if (e != null && target != null && (target.date != e.date || target.slot != e.slot)) moveState.value(e.id, target.date, target.slot)
                                            dragging = null
                                        },
                                        onDragCancel = { dragging = null },
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(32.dp))
                    }
                }
            }
            // Floating "ghost" of the dragged dish.
            dragging?.let { e ->
                val name = state.recipes[e.recipeId]?.name ?: "Блюдо"
                Surface(
                    shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.primaryContainer, shadowElevation = 8.dp,
                    modifier = Modifier.offset { IntOffset((dragPos.x - rootOffset.x - 60.dp.toPx()).roundToInt(), (dragPos.y - rootOffset.y - 28.dp.toPx()).roundToInt()) }
                        .width(200.dp).testTag("drag_ghost"),
                ) { Text(name, Modifier.padding(12.dp), maxLines = 1, overflow = TextOverflow.Ellipsis) }
            }
        }
    }

    if (showRange) RangePickerDialog(state.range.from, state.range.to, { showRange = false }) { f, t -> showRange = false; onRange(f, t) }
    if (showGenerate) GenerateDialog(state.tags, onDismiss = { showGenerate = false }) { showGenerate = false; onGenerate(it) }
    pick?.let { target ->
        RecipePickerDialog(
            title = "${target.slot.title}, ${target.date.dayMonth()}",
            recipes = state.recipes.values.sortedBy { it.name },
            preferred = target.slot.defaultCategory,
            onDismiss = { pick = null },
            onPick = { r, servings -> pick = null; onSetSlot(target.date, target.slot, r.id, servings) },
        )
    }
}

@Composable
private fun SummaryCard(s: MenuSummary) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer), shape = MaterialTheme.shapes.extraLarge, modifier = Modifier.fillMaxWidth().testTag("menu_summary")) {
        Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            SummaryItem("${s.plannedDishes}", "блюд", Modifier.weight(1f))
            SummaryItem("${s.uniqueRecipes}", "рецептов", Modifier.weight(1f))
            SummaryItem("${s.totalServings}", "порций", Modifier.weight(1f))
            SummaryItem(s.avgKcalPerDay?.toString() ?: "—", "ккал/день", Modifier.weight(1f))
        }
    }
}

@Composable
private fun SummaryItem(value: String, label: String, modifier: Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSecondaryContainer)
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSecondaryContainer)
    }
}

@Composable
private fun DayCard(
    day: LocalDate,
    width: androidx.compose.ui.unit.Dp,
    state: MenuUiState,
    hover: SlotKey?,
    draggingId: Long?,
    onBounds: (MealSlot, Rect) -> Unit,
    onAdd: (MealSlot) -> Unit,
    onServings: (Long, Int) -> Unit,
    onClear: (Long) -> Unit,
    onCook: (Long, Int) -> Unit,
    onDragStart: (MenuEntry, Offset) -> Unit,
    onDrag: (Offset) -> Unit,
    onDragEnd: () -> Unit,
    onDragCancel: () -> Unit,
) {
    val today = day == LocalDate.now()
    Card(
        modifier = Modifier.width(width).testTag("day_$day"),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = if (today) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
    ) {
        Column(Modifier.padding(12.dp)) {
            Text("${day.weekdayName()}, ${day.dayMonth()}" + if (today) " · сегодня" else "", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(6.dp))
            MealSlot.entries.forEach { slot ->
                val entry = state.entry(day, slot)
                val key = SlotKey(day, slot)
                val isHover = hover == key
                val bg by animateColorAsState(
                    if (isHover) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer, label = "slotBg",
                )
                var slotTopLeft by remember { mutableStateOf(Offset.Zero) }
                Surface(
                    color = bg, shape = MaterialTheme.shapes.large,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        .onGloballyPositioned { c -> val r = c.boundsInRoot(); slotTopLeft = r.topLeft; onBounds(slot, r) }
                        .alpha(if (entry != null && entry.id == draggingId) 0.4f else 1f)
                        .testTag("slot_${day}_${slot.name}")
                        .let { m ->
                            if (entry == null) m.clickable { onAdd(slot) }
                            else m.pointerInput(entry.id, day, slot) {
                                detectDragGesturesAfterLongPress(
                                    onDragStart = { off -> onDragStart(entry, slotTopLeft + off) },
                                    onDrag = { change, amount -> change.consume(); onDrag(amount) },
                                    onDragEnd = onDragEnd,
                                    onDragCancel = onDragCancel,
                                )
                            }
                        },
                ) {
                    SlotContent(slot, entry, entry?.let { state.recipes[it.recipeId] }, onAdd, onServings, onClear, onCook)
                }
            }
        }
    }
}

@Composable
private fun SlotContent(
    slot: MealSlot,
    entry: MenuEntry?,
    recipe: Recipe?,
    onAdd: (MealSlot) -> Unit,
    onServings: (Long, Int) -> Unit,
    onClear: (Long) -> Unit,
    onCook: (Long, Int) -> Unit,
) {
    Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
        Text(slot.title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (entry == null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Add, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                Text(" Добавить блюдо", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodyMedium)
            }
            return@Column
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.DragIndicator, "Перетащить (долгое нажатие)", tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(18.dp))
            Text(
                recipe?.name ?: "Рецепт удалён", style = MaterialTheme.typography.titleMedium, maxLines = 2,
                overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f).padding(start = 4.dp),
            )
            IconButton(onClick = { onClear(entry.id) }, modifier = Modifier.size(32.dp)) { Icon(Icons.Filled.Close, "Очистить слот", Modifier.size(18.dp)) }
        }
        if (recipe != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Stepper(entry.servings, { onServings(entry.id, it) }, testTagPrefix = "slot_servings_${entry.id}")
                Spacer(Modifier.weight(1f))
                TextButton(onClick = { onCook(recipe.id, entry.servings) }, modifier = Modifier.testTag("cook_${entry.id}")) {
                    Icon(Icons.Filled.PlayArrow, null, Modifier.size(18.dp)); Text("Готовить")
                }
            }
        }
    }
}

@Composable
fun RecipePickerDialog(
    title: String,
    recipes: List<Recipe>,
    preferred: com.example.homemenu.domain.model.Category?,
    onDismiss: () -> Unit,
    onPick: (Recipe, Int) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var servings by remember { mutableIntStateOf(2) }
    val list = remember(query, recipes) {
        recipes.filter { query.isBlank() || it.name.contains(query.trim(), true) }
            .sortedWith(compareByDescending<Recipe> { it.category == preferred }.thenBy { it.name })
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                OutlinedTextField(query, { query = it }, label = { Text("Поиск") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 8.dp)) {
                    Text("Порции: "); Stepper(servings, { servings = it }, testTagPrefix = "pick_servings")
                }
                LazyColumn(Modifier.heightIn(max = 320.dp).testTag("recipe_picker")) {
                    items(list, key = { it.id }) { r ->
                        ListItem(
                            headlineContent = { Text(r.name) },
                            supportingContent = { Text("${r.category.title} · ${r.cookingTimeMin} мин") },
                            modifier = Modifier.clickable { onPick(r, servings) }.testTag("pick_${r.name}"),
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } },
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GenerateDialog(tags: List<Tag>, onDismiss: () -> Unit, onConfirm: (GeneratorOptions) -> Unit) {
    var selectedTags by remember { mutableStateOf(emptySet<Long>()) }
    var slots by remember { mutableStateOf(MealSlot.entries.toSet()) }
    var byCategory by remember { mutableStateOf(true) }
    var servings by remember { mutableIntStateOf(2) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Автоподбор меню") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text("Заполнятся только пустые слоты выбранного периода.", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(8.dp))
                Text("Слоты", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    MealSlot.entries.forEach { s -> FilterChip(s in slots, { slots = if (s in slots) slots - s else slots + s }, { Text(s.title) }) }
                }
                Text("Теги (любой из выбранных)", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    tags.forEach { t ->
                        FilterChip(t.id in selectedTags, { selectedTags = if (t.id in selectedTags) selectedTags - t.id else selectedTags + t.id }, { Text(t.name) }, leadingIcon = { TagDot(t.colorHint) })
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(byCategory, { byCategory = it }, modifier = Modifier.testTag("gen_by_category"))
                    Text("Завтраки — из «Завтраков», обеды — из «Обедов»…")
                }
                Row(verticalAlignment = Alignment.CenterVertically) { Text("Порции: "); Stepper(servings, { servings = it }, testTagPrefix = "gen_servings") }
            }
        },
        confirmButton = {
            TextButton(
                enabled = slots.isNotEmpty(),
                onClick = {
                    onConfirm(
                        GeneratorOptions(
                            tagIds = selectedTags,
                            slotCategories = if (byCategory) MealSlot.entries.mapNotNull { s -> s.defaultCategory?.let { s to setOf(it) } }.toMap() else emptyMap(),
                            slots = slots, servings = servings,
                        ),
                    )
                },
                modifier = Modifier.testTag("gen_confirm"),
            ) { Text("Подобрать") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } },
    )
}
