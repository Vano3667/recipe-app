package com.example.homemenu.ui.recipes

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.homemenu.domain.RecipeQuery
import com.example.homemenu.domain.RecipeSort
import com.example.homemenu.domain.SearchMode
import com.example.homemenu.domain.model.Category
import com.example.homemenu.domain.model.Difficulty
import com.example.homemenu.domain.model.Recipe
import com.example.homemenu.ui.components.EmptyState
import com.example.homemenu.ui.components.Illustration
import com.example.homemenu.ui.components.RecipePhoto
import com.example.homemenu.ui.components.TagChip
import java.io.File

@Composable
fun RecipesRoute(
    expanded: Boolean,
    onOpen: (Long) -> Unit,
    onAdd: () -> Unit,
    onEdit: (Long) -> Unit,
    photo: (String?) -> File?,
    vm: RecipesViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsState()
    val selected by vm.selectedId.collectAsState()
    if (!expanded) {
        RecipesListContent(state, vm::updateQuery, onOpen, onAdd, photo, selectedId = null)
        return
    }
    // Master-detail for wide screens.
    val effectiveSelected = selected?.takeIf { id -> state.visible.any { it.id == id } } ?: state.visible.firstOrNull()?.id
    Row(Modifier.fillMaxSize().testTag("master_detail")) {
        Box(Modifier.weight(0.4f).fillMaxHeight()) {
            RecipesListContent(state, vm::updateQuery, { vm.select(it) }, onAdd, photo, selectedId = effectiveSelected)
        }
        VerticalDivider()
        Box(Modifier.weight(0.6f).fillMaxHeight()) {
            if (effectiveSelected != null) {
                RecipeDetailRoute(onBack = null, onEdit = onEdit, onDeleted = { vm.select(null) }, recipeId = effectiveSelected)
            } else {
                EmptyState("Выберите рецепт", "Здесь появится карточка рецепта", Illustration.BOOK)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RecipesListContent(
    state: RecipesUiState,
    onQueryChange: ((RecipeQuery) -> RecipeQuery) -> Unit,
    onOpen: (Long) -> Unit,
    onAdd: () -> Unit,
    photo: (String?) -> File?,
    selectedId: Long?,
) {
    var showFilters by rememberSaveable { mutableStateOf(false) }
    var sortMenu by remember { mutableStateOf(false) }
    val q = state.query
    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAdd,
                icon = { Icon(Icons.Filled.Add, null) },
                text = { Text("Рецепт") },
                modifier = Modifier.testTag("add_recipe"),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            Text("Рецепты", style = MaterialTheme.typography.headlineLarge, modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp))
            OutlinedTextField(
                value = q.text,
                onValueChange = { t -> onQueryChange { it.copy(text = t) } },
                leadingIcon = { Icon(Icons.Filled.Search, null) },
                trailingIcon = {
                    Row {
                        if (q.text.isNotEmpty()) IconButton(onClick = { onQueryChange { it.copy(text = "") } }) { Icon(Icons.Filled.Close, "Очистить") }
                        IconButton(onClick = { showFilters = !showFilters }, modifier = Modifier.testTag("toggle_filters")) { Icon(Icons.Filled.FilterList, "Фильтры") }
                    }
                },
                placeholder = { Text(if (q.mode == SearchMode.NAME) "Поиск по названию" else "Например: курица, рис") },
                singleLine = true,
                shape = MaterialTheme.shapes.extraLarge,
                modifier = Modifier.fillMaxWidth().padding(16.dp).testTag("search_field"),
            )
            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(SearchMode.entries) { m ->
                    FilterChip(selected = q.mode == m, onClick = { onQueryChange { it.copy(mode = m) } }, label = { Text(m.title) })
                }
                item {
                    Box {
                        FilterChip(selected = false, onClick = { sortMenu = true }, label = { Text("Сортировка: ${q.sort.title}") })
                        DropdownMenu(sortMenu, onDismissRequest = { sortMenu = false }) {
                            RecipeSort.entries.forEach { s ->
                                DropdownMenuItem(text = { Text(s.title) }, onClick = { sortMenu = false; onQueryChange { it.copy(sort = s) } })
                            }
                        }
                    }
                }
            }
            AnimatedVisibility(showFilters) {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Text("Категория", style = MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Category.entries.forEach { c ->
                            FilterChip(q.categories.contains(c), onClick = { onQueryChange { it.copy(categories = it.categories.toggle(c)) } }, label = { Text(c.title) })
                        }
                    }
                    Text("Сложность", style = MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Difficulty.entries.forEach { d ->
                            FilterChip(q.difficulties.contains(d), onClick = { onQueryChange { it.copy(difficulties = it.difficulties.toggle(d)) } }, label = { Text(d.title) })
                        }
                    }
                    Text("Теги", style = MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        state.tags.forEach { t ->
                            FilterChip(q.tagIds.contains(t.id), onClick = { onQueryChange { it.copy(tagIds = it.tagIds.toggle(t.id)) } }, label = { Text(t.name) })
                        }
                    }
                    if (q.categories.isNotEmpty() || q.difficulties.isNotEmpty() || q.tagIds.isNotEmpty()) {
                        TextButton(onClick = { onQueryChange { it.copy(categories = emptySet(), difficulties = emptySet(), tagIds = emptySet()) } }) { Text("Сбросить фильтры") }
                    }
                }
            }
            when {
                state.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                state.total == 0 -> EmptyState(
                    "Пока нет рецептов", "Добавьте любимое блюдо — и оно всегда будет под рукой",
                    Illustration.BOOK, actionLabel = "Добавить рецепт", onAction = onAdd,
                )
                state.visible.isEmpty() -> EmptyState(
                    "Ничего не нашлось", "Попробуйте изменить запрос или фильтры", Illustration.SEARCH,
                    actionLabel = "Сбросить", onAction = { onQueryChange { RecipeQuery(sort = it.sort) } },
                )
                else -> LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.testTag("recipe_list"),
                ) {
                    items(state.visible, key = { it.id }) { r ->
                        RecipeCard(r, photo(r.photoFile), selected = r.id == selectedId, onClick = { onOpen(r.id) }, modifier = Modifier.animateItemPlacement())
                    }
                }
            }
        }
    }
}

private fun <T> Set<T>.toggle(v: T): Set<T> = if (v in this) this - v else this + v

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RecipeCard(r: Recipe, photo: File?, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth().clickable(onClick = onClick).testTag("recipe_card"),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            RecipePhoto(photo, Modifier.size(84.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(r.name, style = MaterialTheme.typography.titleLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (r.cookingTimeMin > 0) {
                        Icon(Icons.Filled.Timer, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(" ${r.cookingTimeMin} мин · ", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text("${r.category.title} · ${r.difficulty.title}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                }
                if (r.tags.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp), maxItemsInEachRow = 3) {
                        r.tags.take(3).forEach { TagChip(it) }
                    }
                }
            }
        }
    }
}
