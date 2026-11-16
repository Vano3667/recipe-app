package com.example.homemenu.ui.recipes

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.homemenu.domain.ScaledIngredient
import com.example.homemenu.domain.model.Recipe
import com.example.homemenu.ui.components.servingsWord
import kotlinx.coroutines.launch

/**
 * Readable plain-text version of a recipe for messengers, e-mail and the clipboard.
 * Only fields that are filled in are written; nothing is invented.
 */
fun recipeShareText(r: Recipe, servings: Int, scaled: List<ScaledIngredient>): String = buildString {
    appendLine("🍽 ${r.name}")
    if (r.description.isNotBlank()) appendLine(r.description.trim())
    appendLine("${r.category.title} · ${r.difficulty.title}")
    if (r.prepTimeMin > 0) appendLine("Подготовка: ${r.prepTimeMin} мин")
    if (r.cookingTimeMin > 0) appendLine("Время приготовления: ${r.cookingTimeMin} мин")
    r.ovenTempC?.let { appendLine("Духовка: $it °C") }
    appendLine("Порции: $servings ${servingsWord(servings)}")
    r.calories?.let { appendLine("Калорийность: $it ккал на порцию") }
    if (scaled.isNotEmpty()) {
        appendLine()
        appendLine("Ингредиенты:")
        scaled.forEach { appendLine("• ${it.ingredient.product.name} — ${it.text}") }
    }
    val steps = r.steps.filter { it.text.isNotBlank() }
    if (steps.isNotEmpty()) {
        appendLine()
        appendLine("Приготовление:")
        steps.forEachIndexed { i, s -> appendLine("${i + 1}. ${s.text.trim()}") }
    }
    if (r.notes.isNotBlank()) { appendLine(); appendLine("Заметки: ${r.notes.trim()}") }
    if (r.tags.isNotEmpty()) { appendLine(); appendLine("Теги: " + r.tags.joinToString(", ") { it.name }) }
    appendLine()
    append("— из приложения «Домашнее меню»")
}

fun copyRecipeText(context: Context, label: String, text: String) {
    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    cm.setPrimaryClip(ClipData.newPlainText(label, text))
    Toast.makeText(context, "Рецепт скопирован", Toast.LENGTH_SHORT).show()
}

/**
 * Route/holder. [recipeId] given → master-detail pane; otherwise the id comes from navigation.
 * [initialServings] comes from the menu "Начать готовить".
 */
@Composable
fun RecipeDetailRoute(
    onBack: (() -> Unit)?,
    onEdit: (Long) -> Unit,
    onDeleted: () -> Unit,
    recipeId: Long? = null,
    initialServings: Int? = null,
    vm: RecipeDetailViewModel = hiltViewModel(key = recipeId?.let { "detail_$it" }),
) {
    LaunchedEffect(recipeId) { recipeId?.let(vm::bind) }
    val recipe by vm.recipe.collectAsState()
    val loaded by vm.loaded.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var deleteCount by remember { mutableStateOf<Int?>(null) }

    val r = recipe
    if (r == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (!loaded) CircularProgressIndicator() else Text("Рецепт не найден")
        }
        return
    }
    var servings by rememberSaveable(r.id) { mutableIntStateOf((initialServings ?: r.servings).coerceAtLeast(1)) }
    val scaled = remember(r, servings) { vm.scaling.scaleAll(r.ingredients, r.servings, servings) }

    RecipeDetailContent(
        recipe = r,
        servings = servings,
        onServingsChange = { servings = it },
        scaled = scaled,
        photo = vm::photoFile,
        onBack = onBack,
        onEdit = { onEdit(r.id) },
        onDelete = { scope.launch { deleteCount = vm.scheduledCount(r.id) } },
        onShareText = {
            // Standard Android Sharesheet: Telegram, WhatsApp, e-mail, "Save to Files" (TXT) and any other app.
            val send = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, r.name)
                putExtra(Intent.EXTRA_TEXT, recipeShareText(r, servings, scaled))
            }
            context.startActivity(Intent.createChooser(send, "Поделиться рецептом"))
        },
        onShareFile = {
            scope.launch {
                val uri = vm.shareFile(r)
                val send = Intent(Intent.ACTION_SEND).apply {
                    type = "application/zip"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(send, "Отправить файл рецепта"))
            }
        },
        onCopyText = { copyRecipeText(context, r.name, recipeShareText(r, servings, scaled)) },
    )

    deleteCount?.let { count ->
        DeleteRecipeDialog(
            name = r.name,
            scheduledCount = count,
            onDismiss = { deleteCount = null },
            onConfirm = { clear ->
                deleteCount = null
                vm.delete(r, clear, onDeleted)
            },
        )
    }
}

@Composable
fun DeleteRecipeDialog(name: String, scheduledCount: Int, onDismiss: () -> Unit, onConfirm: (clearSlots: Boolean) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Удалить рецепт?") },
        text = {
            Text(
                if (scheduledCount > 0)
                    "«$name» запланирован в меню ($scheduledCount). Очистить эти слоты вместе с удалением?"
                else "«$name» будет удалён вместе с фотографиями.",
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(true) }, modifier = Modifier.testTag("confirm_delete")) {
                Text(if (scheduledCount > 0) "Удалить и очистить" else "Удалить")
            }
        },
        dismissButton = {
            Row {
                if (scheduledCount > 0) TextButton(onClick = { onConfirm(false) }) { Text("Оставить слоты") }
                TextButton(onClick = onDismiss) { Text("Отмена") }
            }
        },
    )
}
