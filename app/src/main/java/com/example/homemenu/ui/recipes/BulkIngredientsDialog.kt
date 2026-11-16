package com.example.homemenu.ui.recipes

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.homemenu.domain.IngredientLineParser
import com.example.homemenu.domain.model.Department
import com.example.homemenu.domain.model.MeasureUnit
import com.example.homemenu.domain.model.Product

/**
 * One editable row of the bulk-paste preview (Task 1).
 * [unit] == null: the parser found no unit; the product's base unit is used when the row is added.
 * [include] == false: the row is kept in the preview but not added (duplicates start unchecked).
 */
data class BulkRow(
    val name: String,
    val amountText: String,
    val unit: MeasureUnit?,
    val recognized: Boolean,
    val duplicate: Boolean,
    val include: Boolean,
)

/** Pure helpers for the bulk dialog, unit-tested in BulkIngredientsTest. */
object BulkIngredients {

    /** Every non-empty line becomes one row, in order; duplicates of each other or of [existingNames] start unchecked. */
    fun toRows(text: String, existingNames: Collection<String>): List<BulkRow> {
        val lines = IngredientLineParser.parseLines(text)
        val duplicates = IngredientLineParser.duplicateIndices(lines, existingNames)
        return lines.mapIndexed { i, line ->
            val amount = line.amount
            val amountless = line.unit?.isAmountless == true
            BulkRow(
                name = line.name,
                amountText = if (amount == null || amountless) "" else RecipeFormValidator.formatAmount(amount),
                unit = line.unit,
                recognized = line.recognized,
                duplicate = i in duplicates,
                include = i !in duplicates,
            )
        }
    }

    /** Rows that will be added, in preview order. Rows whose name was cleared are skipped. */
    fun accepted(rows: List<BulkRow>): List<BulkRow> = rows.filter { it.include && it.name.isNotBlank() }

    fun findProduct(name: String, products: List<Product>): Product? {
        val key = IngredientLineParser.normalizeName(name)
        return products.firstOrNull { IngredientLineParser.normalizeName(it.name) == key }
    }

    /** Base unit for a product created from a row: amountless units ("по вкусу") are not a product unit. */
    fun productUnit(row: BulkRow): MeasureUnit = row.unit?.takeUnless { it.isAmountless } ?: MeasureUnit.G

    fun toEditIngredient(row: BulkRow, product: Product): EditIngredient {
        val unit = row.unit ?: product.baseUnit
        return EditIngredient(product, if (unit.isAmountless) "" else row.amountText, unit)
    }

    /**
     * Appends [rows] to the form in their order. Known products are matched by normalized name; unknown ones
     * are created through [onAddProduct] one after another (each creation continues with the next row),
     * so asynchronous creation cannot reorder ingredients.
     */
    fun append(
        rows: List<BulkRow>,
        products: List<Product>,
        onChange: ((EditForm) -> EditForm) -> Unit,
        onAddProduct: (String, MeasureUnit, Department, (Product) -> Unit) -> Unit,
    ) {
        val known = products.toMutableList()
        fun next(i: Int) {
            if (i >= rows.size) return
            val row = rows[i]
            val add: (Product) -> Unit = { p ->
                known += p
                onChange { f -> f.copy(ingredients = f.ingredients + toEditIngredient(row, p)) }
                next(i + 1)
            }
            val found = findProduct(row.name, known)
            if (found != null) add(found) else onAddProduct(row.name.trim(), productUnit(row), Department.OTHER, add)
        }
        next(0)
    }
}

/** Step 1: paste a list. Step 2: check and correct the parsed rows. Then "Добавить" adds the checked rows. */
@Composable
fun BulkIngredientsDialog(existingNames: List<String>, onDismiss: () -> Unit, onConfirm: (List<BulkRow>) -> Unit) {
    var text by remember { mutableStateOf("") }
    var rows by remember { mutableStateOf<List<BulkRow>?>(null) }
    val preview = rows
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (preview == null) "Вставить списком" else "Проверьте ингредиенты") },
        text = {
            if (preview == null) {
                Column {
                    Text(
                        "Каждая строка — один ингредиент, например «500 г куриной грудки» или «Соль — по вкусу».",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = text, onValueChange = { text = it.take(10_000) },
                        placeholder = { Text("500 г куриной грудки\n2 луковицы\n1 ч. л. соли") },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 160.dp).testTag("bulk_input"),
                    )
                }
            } else if (preview.isEmpty()) {
                Text("Список пуст. Нажмите «Назад», чтобы вставить текст ещё раз.")
            } else {
                LazyColumn(Modifier.heightIn(max = 420.dp).testTag("bulk_preview")) {
                    itemsIndexed(preview) { i, row ->
                        BulkRowEditor(
                            row = row, index = i,
                            onChange = { r -> rows = rows?.toMutableList()?.also { it[i] = r } },
                            onRemove = { rows = rows?.filterIndexed { j, _ -> j != i } },
                        )
                    }
                }
            }
        },
        confirmButton = {
            if (preview == null) {
                TextButton(
                    onClick = { rows = BulkIngredients.toRows(text, existingNames) },
                    enabled = text.isNotBlank(), modifier = Modifier.testTag("bulk_parse"),
                ) { Text("Разобрать") }
            } else {
                val accepted = BulkIngredients.accepted(preview)
                TextButton(
                    onClick = { onConfirm(accepted) },
                    enabled = accepted.isNotEmpty(), modifier = Modifier.testTag("bulk_add_all"),
                ) { Text("Добавить (${accepted.size})") }
            }
        },
        dismissButton = {
            TextButton(onClick = { if (preview != null) rows = null else onDismiss() }) {
                Text(if (preview != null) "Назад" else "Отмена")
            }
        },
    )
}

@Composable
private fun BulkRowEditor(row: BulkRow, index: Int, onChange: (BulkRow) -> Unit, onRemove: () -> Unit) {
    var unitMenu by remember { mutableStateOf(false) }
    val amountless = row.unit?.isAmountless == true
    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = row.include, onCheckedChange = { onChange(row.copy(include = it)) },
                modifier = Modifier.testTag("bulk_include_$index"),
            )
            OutlinedTextField(
                value = row.name, onValueChange = { onChange(row.copy(name = it.take(60))) },
                singleLine = true, label = { Text("Продукт") },
                modifier = Modifier.weight(1f).testTag("bulk_name_$index"),
            )
            IconButton(onClick = onRemove, modifier = Modifier.testTag("bulk_remove_$index")) {
                Icon(Icons.Filled.Close, "Убрать строку")
            }
        }
        Row(Modifier.padding(start = 48.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = row.amountText,
                onValueChange = { v -> onChange(row.copy(amountText = v.filter { it.isDigit() || it == ',' || it == '.' }.take(7))) },
                enabled = !amountless, singleLine = true, placeholder = { Text("0") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.width(96.dp).testTag("bulk_amount_$index"),
            )
            Spacer(Modifier.width(4.dp))
            Box {
                TextButton(onClick = { unitMenu = true }, modifier = Modifier.testTag("bulk_unit_$index")) {
                    Text(row.unit?.label ?: "ед. продукта")
                }
                DropdownMenu(unitMenu, onDismissRequest = { unitMenu = false }) {
                    MeasureUnit.entries.forEach { u ->
                        DropdownMenuItem(
                            text = { Text(u.label) },
                            onClick = { unitMenu = false; onChange(row.copy(unit = u, amountText = if (u.isAmountless) "" else row.amountText)) },
                        )
                    }
                }
            }
        }
        if (!row.recognized) {
            Text(
                "Количество не распознано — укажите его вручную",
                color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = 48.dp),
            )
        }
        if (row.duplicate) {
            Text(
                "Похоже на повтор — отметьте, если всё же нужно добавить",
                color = MaterialTheme.colorScheme.tertiary, style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = 48.dp),
            )
        }
    }
}
