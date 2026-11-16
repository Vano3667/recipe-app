package com.example.homemenu.ui.recipes

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.homemenu.domain.ScaledIngredient
import com.example.homemenu.domain.model.Recipe
import com.example.homemenu.ui.components.RecipePhoto
import com.example.homemenu.ui.components.SectionTitle
import com.example.homemenu.ui.components.Stepper
import com.example.homemenu.ui.components.TagChip
import com.example.homemenu.ui.components.servingsWord
import java.io.File

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RecipeDetailContent(
    recipe: Recipe,
    servings: Int,
    onServingsChange: (Int) -> Unit,
    scaled: List<ScaledIngredient>,
    photo: (String?) -> File?,
    onBack: (() -> Unit)?,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onShareText: () -> Unit,
    onShareFile: () -> Unit,
    onCopyText: () -> Unit = {},
) {
    var shareMenu by remember { mutableStateOf(false) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(recipe.name, maxLines = 1) },
                navigationIcon = {
                    if (onBack != null) IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад") }
                },
                actions = {
                    Box {
                        IconButton(onClick = { shareMenu = true }, modifier = Modifier.testTag("share_recipe")) { Icon(Icons.Filled.Share, "Поделиться") }
                        DropdownMenu(expanded = shareMenu, onDismissRequest = { shareMenu = false }) {
                            DropdownMenuItem(text = { Text("Как текст") }, onClick = { shareMenu = false; onShareText() }, modifier = Modifier.testTag("share_text"))
                            DropdownMenuItem(text = { Text("Скопировать текст") }, onClick = { shareMenu = false; onCopyText() }, modifier = Modifier.testTag("copy_text"))
                            DropdownMenuItem(text = { Text("Как файл (.zip)") }, onClick = { shareMenu = false; onShareFile() })
                        }
                    }
                    IconButton(onClick = onEdit, modifier = Modifier.testTag("edit_recipe")) { Icon(Icons.Filled.Edit, "Изменить") }
                    IconButton(onClick = onDelete, modifier = Modifier.testTag("delete_recipe")) { Icon(Icons.Filled.Delete, "Удалить") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        BoxWithConstraints(Modifier.padding(padding).fillMaxSize()) {
            val twoColumns = maxWidth >= 600.dp
            val scroll = rememberScrollState()
            Column(Modifier.verticalScroll(scroll).padding(16.dp).testTag("recipe_detail")) {
                val header: @Composable () -> Unit = {
                    RecipePhoto(photo(recipe.photoFile), Modifier.fillMaxWidth().aspectRatio(4f / 3f), recipe.name)
                    Spacer(Modifier.height(12.dp))
                    Text(recipe.name, style = MaterialTheme.typography.headlineMedium)
                    if (recipe.description.isNotBlank()) {
                        Spacer(Modifier.height(6.dp))
                        Text(recipe.description, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(Modifier.height(8.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (recipe.cookingTimeMin > 0) AssistChip(onClick = {}, label = { Text("${recipe.cookingTimeMin} мин") }, leadingIcon = { Icon(Icons.Filled.Timer, null, Modifier.size(18.dp)) })
                        AssistChip(onClick = {}, label = { Text(recipe.difficulty.title) })
                        AssistChip(onClick = {}, label = { Text(recipe.category.title) })
                        if (recipe.prepTimeMin > 0) AssistChip(onClick = {}, label = { Text("Подготовка ${recipe.prepTimeMin} мин") })
                        recipe.ovenTempC?.let { AssistChip(onClick = {}, label = { Text("Духовка $it °C") }) }
                        recipe.calories?.let { AssistChip(onClick = {}, label = { Text("$it ккал/порц.") }, leadingIcon = { Icon(Icons.Filled.LocalFireDepartment, null, Modifier.size(18.dp)) }) }
                    }
                    if (recipe.tags.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) { recipe.tags.forEach { TagChip(it) } }
                    }
                }
                val ingredients: @Composable () -> Unit = {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer), shape = MaterialTheme.shapes.extraLarge) {
                        Column(Modifier.padding(16.dp)) {
                            SectionTitle("Ингредиенты")
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Порции:", style = MaterialTheme.typography.bodyLarge)
                                Spacer(Modifier.width(8.dp))
                                Stepper(servings, onServingsChange, testTagPrefix = "servings")
                            }
                            if (servings != recipe.servings) {
                                Text("Исходно на ${recipe.servings} ${servingsWord(recipe.servings)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Spacer(Modifier.height(8.dp))
                            if (scaled.isEmpty()) Text("Ингредиенты не указаны", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            scaled.forEachIndexed { i, s ->
                                Row(Modifier.fillMaxWidth().padding(vertical = 6.dp).testTag("ingredient_$i"), verticalAlignment = Alignment.CenterVertically) {
                                    Text(s.ingredient.product.name, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                                    AnimatedContent(s.text, label = "amount") { txt ->
                                        Text(txt, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.testTag("ingredient_amount_$i"))
                                    }
                                }
                                if (i < scaled.lastIndex) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            }
                        }
                    }
                }
                val steps: @Composable () -> Unit = {
                    Column {
                        SectionTitle("Приготовление")
                        recipe.steps.filter { it.text.isNotBlank() || it.photoFile != null }.forEachIndexed { i, step ->
                            Row(Modifier.padding(vertical = 8.dp)) {
                                Box(Modifier.size(32.dp), contentAlignment = Alignment.Center) {
                                    Text("${i + 1}", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                                }
                                Spacer(Modifier.width(8.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(step.text, style = MaterialTheme.typography.bodyLarge)
                                    photo(step.photoFile)?.let {
                                        Spacer(Modifier.height(8.dp))
                                        RecipePhoto(it, Modifier.fillMaxWidth().widthIn(max = 480.dp).aspectRatio(16f / 9f), "Фото шага ${i + 1}")
                                    }
                                }
                            }
                        }
                        if (recipe.notes.isNotBlank()) {
                            SectionTitle("Заметки")
                            Text(recipe.notes, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }

                if (twoColumns) {
                    Row(horizontalArrangement = Arrangement.spacedBy(24.dp), modifier = Modifier.testTag("detail_two_columns")) {
                        Column(Modifier.weight(0.42f)) { header(); Spacer(Modifier.height(16.dp)); ingredients() }
                        Column(Modifier.weight(0.58f)) { steps() }
                    }
                } else {
                    header(); Spacer(Modifier.height(16.dp)); ingredients(); Spacer(Modifier.height(16.dp)); steps()
                }
                Spacer(Modifier.height(32.dp))
            }
        }
    }
}
