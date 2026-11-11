package com.example.homemenu.ui.recipes

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.homemenu.domain.model.Category
import com.example.homemenu.domain.model.DEFAULT_CUISINES
import com.example.homemenu.domain.model.Department
import com.example.homemenu.domain.model.Difficulty
import com.example.homemenu.domain.model.MeasureUnit
import com.example.homemenu.domain.model.Product
import com.example.homemenu.domain.model.Step
import com.example.homemenu.domain.model.Tag
import com.example.homemenu.ui.components.PhotoPickerField
import com.example.homemenu.ui.components.SectionTitle
import com.example.homemenu.ui.components.TagDot
import com.example.homemenu.ui.theme.TagPalette
import kotlinx.coroutines.launch

@Composable
fun RecipeEditRoute(onClose: () -> Unit, onSaved: (Long) -> Unit, vm: RecipeEditViewModel = hiltViewModel()) {
    val form by vm.form.collectAsState()
    val errors by vm.errors.collectAsState()
    val busy by vm.busy.collectAsState()
    val products by vm.products.collectAsState()
    val tags by vm.tags.collectAsState()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        vm.eventFlow.collect { e ->
            when (e) {
                is EditEvent.Saved -> onSaved(e.id)
                // Отдельная корутина: showSnackbar() ждёт, пока сообщение скроется, и не должен
                // задерживать событие Saved (переход после сохранения).
                is EditEvent.Message -> launch { snackbar.showSnackbar(e.text) }
            }
        }
    }

    RecipeEditContent(
        isNew = vm.isNew, form = form, errors = errors, busy = busy, products = products, tags = tags,
        snackbar = snackbar, onChange = vm::update, onSave = vm::save, onClose = onClose,
        onAddProduct = { n, u, d, done -> vm.addCustomProduct(n, u, d, done) },
        onAddTag = vm::addCustomTag,
        // Фото: главное фото рецепта (stepIndex == null) и фото шагов; галерея (системный Photo Picker) и камера.
        photoPicker = { name, stepIndex, tagPrefix ->
            PhotoPickerField(
                photo = vm.photoFile(name),
                onPicked = { vm.importPhoto(it, stepIndex) },
                onRemove = {
                    vm.update { f ->
                        if (stepIndex == null) f.copy(photoFile = null)
                        else f.copy(steps = f.steps.mapIndexed { i, s -> if (i == stepIndex) s.copy(photoFile = null) else s })
                    }
                },
                createCameraUri = vm::cameraUri,
                tagPrefix = tagPrefix,
            )
        },
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RecipeEditContent(
    isNew: Boolean,
    form: EditForm,
    errors: EditErrors,
    busy: Boolean,
    products: List<Product>,
    tags: List<Tag>,
    snackbar: SnackbarHostState,
    onChange: ((EditForm) -> EditForm) -> Unit,
    onSave: () -> Unit,
    onClose: () -> Unit,
    onAddProduct: (String, MeasureUnit, Department, (Product) -> Unit) -> Unit,
    onAddTag: (String, String) -> Unit,
    // Слот выбора фото (галерея / камера). null — блоки фото не показываются (например, в Robolectric-тестах).
    //  stepIndex == null — главное фото рецепта (теги "photo_*"), иначе фото шага (теги "step_photo_<i>_*").
    photoPicker: @Composable ((name: String?, stepIndex: Int?, tagPrefix: String) -> Unit)? = null,
) {
    var pickProduct by remember { mutableStateOf(false) }
    var newTag by remember { mutableStateOf(false) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isNew) "Новый рецепт" else "Редактирование") },
                navigationIcon = { IconButton(onClick = onClose) { Icon(Icons.Filled.Close, "Закрыть") } },
                actions = { TextButton(onClick = onSave, enabled = !busy, modifier = Modifier.testTag("save_recipe")) { Text("Сохранить") } },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize().imePadding().verticalScroll(rememberScrollState())
                .padding(16.dp).widthIn(max = 840.dp),
        ) {
            if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            // Главное фото рецепта — над названием.
            if (photoPicker != null) {
                photoPicker(form.photoFile, null, "photo")
                Spacer(Modifier.height(12.dp))
            }
            OutlinedTextField(
                value = form.name, onValueChange = { v -> onChange { it.copy(name = v.take(120)) } },
                label = { Text("Название *") }, isError = errors.name != null,
                supportingText = errors.name?.let { { Text(it) } }, singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("name_field"),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = form.timeText, onValueChange = { v -> onChange { it.copy(timeText = v.filter(Char::isDigit).take(4)) } },
                    label = { Text("Время, мин") }, isError = errors.time != null, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = form.servingsText, onValueChange = { v -> onChange { it.copy(servingsText = v.filter { c -> c.isDigit() || c == '-' }.take(3)) } },
                    label = { Text("Порции *") }, isError = errors.servings != null,
                    supportingText = errors.servings?.let { { Text(it) } }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f).testTag("servings_field"),
                )
                OutlinedTextField(
                    value = form.caloriesText, onValueChange = { v -> onChange { it.copy(caloriesText = v.filter(Char::isDigit).take(5)) } },
                    label = { Text("ккал/порц.") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f),
                )
            }
            Text("Категория", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Category.entries.forEach { c -> FilterChip(form.category == c, { onChange { it.copy(category = c) } }, { Text(c.title) }) }
            }
            Text("Сложность", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Difficulty.entries.forEach { d -> FilterChip(form.difficulty == d, { onChange { it.copy(difficulty = d) } }, { Text(d.title) }) }
            }
            CuisineField(form.cuisine) { v -> onChange { it.copy(cuisine = v.take(40)) } }

            SectionTitle("Теги")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                tags.forEach { t ->
                    FilterChip(
                        selected = t.id in form.tagIds,
                        onClick = { onChange { f -> f.copy(tagIds = if (t.id in f.tagIds) f.tagIds - t.id else f.tagIds + t.id) } },
                        label = { Text(t.name) }, leadingIcon = { TagDot(t.colorHint) },
                    )
                }
                FilterChip(false, { newTag = true }, { Text("Новый тег") }, leadingIcon = { Icon(Icons.Filled.Add, null) })
            }

            SectionTitle("Ингредиенты")
            errors.ingredients?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            form.ingredients.forEachIndexed { i, ing ->
                IngredientRow(
                    ing,
                    onAmount = { v -> onChange { f -> f.copy(ingredients = f.ingredients.mapIndexed { j, x -> if (j == i) x.copy(amountText = v) else x }) } },
                    onUnit = { u -> onChange { f -> f.copy(ingredients = f.ingredients.mapIndexed { j, x -> if (j == i) x.copy(unit = u) else x }) } },
                    onRemove = { onChange { f -> f.copy(ingredients = f.ingredients.filterIndexed { j, _ -> j != i }) } },
                    index = i,
                )
            }
            OutlinedButton(onClick = { pickProduct = true }, modifier = Modifier.testTag("add_ingredient")) {
                Icon(Icons.Filled.Add, null); Spacer(Modifier.width(6.dp)); Text("Добавить ингредиент")
            }

            SectionTitle("Шаги")
            form.steps.forEachIndexed { i, step ->
                Card(
                    Modifier.fillMaxWidth().padding(vertical = 6.dp), shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Шаг ${i + 1}", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                            IconButton(onClick = { onChange { f -> f.copy(steps = f.steps.swap(i, i - 1)) } }, enabled = i > 0) { Icon(Icons.Filled.KeyboardArrowUp, "Выше") }
                            IconButton(onClick = { onChange { f -> f.copy(steps = f.steps.swap(i, i + 1)) } }, enabled = i < form.steps.lastIndex) { Icon(Icons.Filled.KeyboardArrowDown, "Ниже") }
                            IconButton(onClick = { onChange { f -> f.copy(steps = f.steps.filterIndexed { j, _ -> j != i }.ifEmpty { listOf(Step("")) }) } }) { Icon(Icons.Filled.Delete, "Удалить шаг") }
                        }
                        OutlinedTextField(
                            value = step.text,
                            onValueChange = { v -> onChange { f -> f.copy(steps = f.steps.mapIndexed { j, s -> if (j == i) s.copy(text = v.take(2000)) else s }) } },
                            placeholder = { Text("Опишите, что сделать") },
                            modifier = Modifier.fillMaxWidth().heightIn(min = 80.dp).testTag("step_field_$i"),
                        )
                        // Фото шага — под текстом шага.
                        if (photoPicker != null) {
                            Spacer(Modifier.height(8.dp))
                            photoPicker(step.photoFile, i, "step_photo_$i")
                        }
                    }
                }
            }
            OutlinedButton(onClick = { onChange { it.copy(steps = it.steps + Step("")) } }) {
                Icon(Icons.Filled.Add, null); Spacer(Modifier.width(6.dp)); Text("Добавить шаг")
            }

            SectionTitle("Заметки")
            OutlinedTextField(
                value = form.notes, onValueChange = { v -> onChange { it.copy(notes = v.take(4000)) } },
                modifier = Modifier.fillMaxWidth().heightIn(min = 100.dp), placeholder = { Text("Советы, замены, история рецепта…") },
            )
            Spacer(Modifier.height(16.dp))
            Button(onClick = onSave, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text("Сохранить рецепт") }
            Spacer(Modifier.height(48.dp))
        }
    }

    if (pickProduct) {
        ProductPickerDialog(
            products = products,
            onDismiss = { pickProduct = false },
            onPick = { p ->
                pickProduct = false
                onChange { it.copy(ingredients = it.ingredients + EditIngredient(p, "", p.baseUnit)) }
            },
            onCreate = { name, unit, dep ->
                pickProduct = false
                onAddProduct(name, unit, dep) { p -> onChange { it.copy(ingredients = it.ingredients + EditIngredient(p, "", p.baseUnit)) } }
            },
        )
    }
    if (newTag) {
        NewTagDialog(onDismiss = { newTag = false }, onCreate = { n, c -> newTag = false; onAddTag(n, c) })
    }
}

private fun <T> List<T>.swap(a: Int, b: Int): List<T> {
    if (a !in indices || b !in indices) return this
    val m = toMutableList(); val t = m[a]; m[a] = m[b]; m[b] = t; return m
}

@Composable
private fun CuisineField(value: String, onChange: (String) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Column {
        OutlinedTextField(
            value = value, onValueChange = onChange, label = { Text("Кухня") }, singleLine = true,
            trailingIcon = { IconButton(onClick = { open = true }) { Icon(Icons.Filled.KeyboardArrowDown, "Список кухонь") } },
            modifier = Modifier.fillMaxWidth(),
        )
        DropdownMenu(open, onDismissRequest = { open = false }) {
            DEFAULT_CUISINES.forEach { c -> DropdownMenuItem(text = { Text(c.replaceFirstChar { it.uppercase() }) }, onClick = { open = false; onChange(c) }) }
        }
    }
}

@Composable
private fun IngredientRow(ing: EditIngredient, onAmount: (String) -> Unit, onUnit: (MeasureUnit) -> Unit, onRemove: () -> Unit, index: Int) {
    var unitMenu by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(ing.product.name, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        OutlinedTextField(
            value = ing.amountText,
            onValueChange = { v -> onAmount(v.filter { it.isDigit() || it == ',' || it == '.' }.take(7)) },
            singleLine = true, modifier = Modifier.width(96.dp).testTag("amount_field_$index"),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), placeholder = { Text("0") },
        )
        Column {
            TextButton(onClick = { unitMenu = true }) { Text(ing.unit.label) }
            DropdownMenu(unitMenu, onDismissRequest = { unitMenu = false }) {
                MeasureUnit.entries.forEach { u -> DropdownMenuItem(text = { Text(u.label) }, onClick = { unitMenu = false; onUnit(u) }) }
            }
        }
        IconButton(onClick = onRemove) { Icon(Icons.Filled.Close, "Убрать ингредиент") }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProductPickerDialog(
    products: List<Product>,
    onDismiss: () -> Unit,
    onPick: (Product) -> Unit,
    onCreate: (String, MeasureUnit, Department) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var creating by remember { mutableStateOf(false) }
    var unit by remember { mutableStateOf(MeasureUnit.G) }
    var dep by remember { mutableStateOf(Department.OTHER) }
    val filtered = remember(query, products) {
        val q = query.trim().lowercase().replace('ё', 'е')
        products.filter { q.isEmpty() || it.name.lowercase().replace('ё', 'е').contains(q) }.sortedBy { it.name }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (creating) "Новый продукт" else "Выберите продукт") },
        text = {
            Column {
                OutlinedTextField(
                    value = query, onValueChange = { query = it.take(60) }, singleLine = true,
                    label = { Text(if (creating) "Название" else "Поиск") },
                    modifier = Modifier.fillMaxWidth().testTag("product_search"),
                )
                if (creating) {
                    Text("Единица", style = MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        MeasureUnit.entries.forEach { u -> FilterChip(unit == u, { unit = u }, { Text(u.label) }) }
                    }
                    Text("Отдел магазина", style = MaterialTheme.typography.labelLarge)
                    LazyColumn(Modifier.heightIn(max = 220.dp)) {
                        items(Department.entries) { d ->
                            FilterChip(dep == d, { dep = d }, { Text(d.title) })
                        }
                    }
                } else {
                    LazyColumn(Modifier.heightIn(max = 320.dp).testTag("product_list")) {
                        items(filtered, key = { it.id }) { p ->
                            ListItem(
                                headlineContent = { Text(p.name) },
                                supportingContent = { Text(p.department.title) },
                                modifier = Modifier.fillMaxWidth().testTag("product_${p.name}").clickable { onPick(p) },
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (creating) TextButton(onClick = { if (query.isNotBlank()) onCreate(query.trim(), unit, dep) }, enabled = query.isNotBlank()) { Text("Добавить") }
            else TextButton(onClick = { creating = true }) { Text("Свой продукт") }
        },
        dismissButton = { TextButton(onClick = { if (creating) creating = false else onDismiss() }) { Text(if (creating) "Назад" else "Отмена") } },
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NewTagDialog(onDismiss: () -> Unit, onCreate: (String, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var color by remember { mutableStateOf(TagPalette.first()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Новый тег") },
        text = {
            Column {
                OutlinedTextField(name, { name = it.take(30) }, label = { Text("Название") }, singleLine = true)
                Spacer(Modifier.height(8.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TagPalette.forEach { c -> FilterChip(color == c, { color = c }, { TagDot(c) }) }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onCreate(name.trim(), color) }, enabled = name.isNotBlank()) { Text("Создать") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } },
    )
}
