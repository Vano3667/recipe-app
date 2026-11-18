package com.example.homemenu.ui.recipes

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.homemenu.domain.ParsedRecipeText
import com.example.homemenu.domain.RecipeTextParser
import com.example.homemenu.domain.model.Step
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.nio.charset.Charset

private const val MAX_TEXT_CHARS = 50_000
private const val MAX_FILE_BYTES = 1_000_000

/** Reads a TXT document: UTF-8 first, windows-1251 if UTF-8 decoding produced replacement characters. */
suspend fun readTextFromUri(context: Context, uri: Uri): String? = withContext(Dispatchers.IO) {
    runCatching {
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: error("no input stream")
        if (bytes.size > MAX_FILE_BYTES) error("file too large")
        val utf8 = String(bytes, Charsets.UTF_8)
        val text = if (utf8.contains('\uFFFD')) String(bytes, Charset.forName("windows-1251")) else utf8
        text.removePrefix("\uFEFF")
    }.getOrNull()
}

/**
 * Task 5: import a recipe from pasted text, a messenger message or a TXT file.
 * Nothing is saved until the user confirms the last step; the result is always a new recipe.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TextImportDialog(
    onDismiss: () -> Unit,
    onSaved: (Long) -> Unit,
    initialText: String = "",
    vm: TextImportViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val tags by vm.tags.collectAsState()
    val busy by vm.busy.collectAsState()
    val error by vm.error.collectAsState()
    var text by remember { mutableStateOf(initialText.take(MAX_TEXT_CHARS)) }
    var message by remember { mutableStateOf<String?>(null) }
    var parsed by remember { mutableStateOf<ParsedRecipeText?>(null) }
    var form by remember { mutableStateOf<EditForm?>(null) }
    var newTags by remember { mutableStateOf<List<String>>(emptyList()) }
    var ingredientsStep by remember { mutableStateOf(false) }

    LaunchedEffect(error) {
        error?.let { Toast.makeText(context, it, Toast.LENGTH_LONG).show(); vm.clearError() }
    }
    val openTxt = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            val read = readTextFromUri(context, uri)
            if (read == null) message = "Не удалось прочитать файл" else { text = read.take(MAX_TEXT_CHARS); message = null }
        }
    }
    val save: (List<BulkRow>) -> Unit = { rows ->
        form?.let { f -> vm.save(f, rows, newTags) { id -> onSaved(id) } }
    }

    val f = form
    val p = parsed
    if (f == null || p == null) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Рецепт из текста") },
            text = {
                Column {
                    Text(
                        "Вставьте рецепт из мессенджера или откройте TXT-файл. Перед сохранением всё можно проверить и исправить.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = text, onValueChange = { text = it.take(MAX_TEXT_CHARS); message = null },
                        placeholder = { Text("Борщ\n\nИнгредиенты:\n500 г говядины\n\nПриготовление:\n1. …") },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 180.dp, max = 360.dp).testTag("text_import_input"),
                    )
                    TextButton(
                        onClick = { openTxt.launch(arrayOf("text/plain", "text/*")) },
                        modifier = Modifier.testTag("text_import_open"),
                    ) { Text("Открыть TXT-файл") }
                    message?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val result = RecipeTextParser.parse(text)
                        if (result.isEmpty) {
                            message = "В тексте не найден рецепт: нет названия, ингредиентов и шагов"
                        } else {
                            val existing = tags.map { it.id to it.name }
                            parsed = result
                            newTags = RecipeTextImport.matchTags(result.tags, existing).second
                            form = RecipeTextImport.toForm(result, existing)
                        }
                    },
                    enabled = text.isNotBlank(), modifier = Modifier.testTag("text_import_parse"),
                ) { Text("Разобрать") }
            },
            dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } },
        )
        return
    }

    if (ingredientsStep) {
        BulkIngredientsDialog(
            existingNames = emptyList(),
            initialText = RecipeTextImport.ingredientText(p),
            confirmLabel = "Сохранить рецепт",
            onDismiss = { ingredientsStep = false },
            onConfirm = { rows ->
                val missing = RecipeTextImport.rowsMissingAmount(rows)
                if (missing.isNotEmpty()) {
                    Toast.makeText(context, "Укажите количество: " + missing.joinToString { it.name }, Toast.LENGTH_LONG).show()
                } else if (!busy) save(rows)
            },
        )
        return
    }

    fun upd(t: (EditForm) -> EditForm) { form = form?.let(t) }
    val existingById = tags.associateBy { it.id }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Проверьте рецепт") },
        text = {
            Column(Modifier.heightIn(max = 480.dp).verticalScroll(rememberScrollState()).testTag("text_import_preview")) {
                OutlinedTextField(
                    value = f.name, onValueChange = { v -> upd { it.copy(name = v.take(120)) } },
                    label = { Text("Название *") }, singleLine = true, isError = f.name.isBlank(),
                    modifier = Modifier.fillMaxWidth().testTag("text_import_name"),
                )
                OutlinedTextField(
                    value = f.description, onValueChange = { v -> upd { it.copy(description = v.take(1000)) } },
                    label = { Text("Описание") }, modifier = Modifier.fillMaxWidth(),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NumberField(f.timeText, "Время, мин", 4, Modifier.weight(1f)) { v -> upd { it.copy(timeText = v) } }
                    NumberField(f.prepTimeText, "Подготовка", 4, Modifier.weight(1f)) { v -> upd { it.copy(prepTimeText = v) } }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NumberField(f.servingsText, "Порции *", 3, Modifier.weight(1f)) { v -> upd { it.copy(servingsText = v) } }
                    NumberField(f.ovenTempText, "Духовка, °C", 3, Modifier.weight(1f)) { v -> upd { it.copy(ovenTempText = v) } }
                    NumberField(f.caloriesText, "ккал", 5, Modifier.weight(1f)) { v -> upd { it.copy(caloriesText = v) } }
                }
                if (p.servings == null) {
                    Text("Порции в тексте не найдены — проверьте значение", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.tertiary)
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    if (p.ingredients.isEmpty()) "Ингредиенты не найдены — их можно добавить потом в редакторе"
                    else "Ингредиентов: ${p.ingredients.size} — проверка на следующем шаге",
                    style = MaterialTheme.typography.labelLarge,
                )
                Spacer(Modifier.height(8.dp))
                Text("Шаги", style = MaterialTheme.typography.labelLarge)
                f.steps.forEachIndexed { i, s ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = s.text,
                            onValueChange = { v -> upd { it.copy(steps = it.steps.mapIndexed { j, x -> if (j == i) x.copy(text = v.take(2000)) else x }) } },
                            placeholder = { Text("Шаг ${i + 1}") },
                            modifier = Modifier.weight(1f).testTag("text_import_step_$i"),
                        )
                        IconButton(onClick = { upd { it.copy(steps = it.steps.filterIndexed { j, _ -> j != i }.ifEmpty { listOf(Step("")) }) } }) {
                            Icon(Icons.Filled.Close, "Удалить шаг")
                        }
                    }
                }
                TextButton(onClick = { upd { it.copy(steps = it.steps + Step("")) } }) { Text("Добавить шаг") }
                if (f.tagIds.isNotEmpty() || newTags.isNotEmpty()) {
                    Text("Теги", style = MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        f.tagIds.forEach { id ->
                            val name = existingById[id]?.name ?: return@forEach
                            InputChip(
                                selected = true, onClick = { upd { it.copy(tagIds = it.tagIds - id) } }, label = { Text(name) },
                                trailingIcon = { Icon(Icons.Filled.Close, "Убрать тег $name", Modifier.size(16.dp)) },
                            )
                        }
                        newTags.forEach { name ->
                            InputChip(
                                selected = false, onClick = { newTags = newTags - name }, label = { Text("$name (новый)") },
                                trailingIcon = { Icon(Icons.Filled.Close, "Не создавать тег $name", Modifier.size(16.dp)) },
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = f.notes, onValueChange = { v -> upd { it.copy(notes = v.take(4000)) } },
                    label = { Text("Заметки") }, modifier = Modifier.fillMaxWidth().heightIn(min = 80.dp).testTag("text_import_notes"),
                )
            }
        },
        confirmButton = {
            val ready = f.name.isNotBlank() && (f.servingsText.toIntOrNull() ?: 0) > 0 && !busy
            if (p.ingredients.isNotEmpty()) {
                TextButton(onClick = { ingredientsStep = true }, enabled = ready, modifier = Modifier.testTag("text_import_next")) {
                    Text("Далее: ингредиенты")
                }
            } else {
                TextButton(onClick = { save(emptyList()) }, enabled = ready, modifier = Modifier.testTag("text_import_save")) {
                    Text("Сохранить")
                }
            }
        },
        dismissButton = { TextButton(onClick = { form = null; parsed = null }) { Text("Назад") } },
    )
}

@Composable
private fun NumberField(value: String, label: String, maxLen: Int, modifier: Modifier, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value, onValueChange = { v -> onChange(v.filter(Char::isDigit).take(maxLen)) },
        label = { Text(label) }, singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = modifier,
    )
}
