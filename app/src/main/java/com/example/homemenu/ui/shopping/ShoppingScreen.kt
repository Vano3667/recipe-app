package com.example.homemenu.ui.shopping

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.homemenu.domain.ShoppingItem
import com.example.homemenu.ui.components.EmptyState
import com.example.homemenu.ui.components.Illustration
import com.example.homemenu.ui.components.RangePickerDialog
import com.example.homemenu.ui.components.rangeLabel
import java.time.LocalDate

@Composable
fun ShoppingRoute(onOpenMenu: () -> Unit, vm: ShoppingViewModel = hiltViewModel()) {
    val state by vm.state.collectAsState()
    val context = LocalContext.current
    var askShare by remember { mutableStateOf(false) }
    val share: (Boolean) -> Unit = { onlyRemaining ->
        val send = Intent(Intent.ACTION_SEND).setType("text/plain")
            .putExtra(Intent.EXTRA_SUBJECT, "Список покупок")
            .putExtra(Intent.EXTRA_TEXT, vm.shareText(onlyRemaining))
        context.startActivity(Intent.createChooser(send, "Поделиться списком"))
    }
    ShoppingContent(
        state = state,
        onMode = vm::setMode, onPrev = vm::previous, onNext = vm::next, onRange = vm::setRange,
        onToggle = vm::toggle, onClear = vm::clearChecks,
        onShare = {
            // Ask only when the choice matters: the list is partly ticked.
            if (state.done > 0 && state.done < state.total) askShare = true else share(false)
        },
        onOpenMenu = onOpenMenu,
    )
    if (askShare) {
        AlertDialog(
            onDismissRequest = { askShare = false },
            title = { Text("Что отправить?") },
            text = { Text("Куплено ${state.done} из ${state.total}.") },
            confirmButton = {
                TextButton(onClick = { askShare = false; share(true) }, modifier = Modifier.testTag("share_remaining")) {
                    Text("Только некупленное")
                }
            },
            dismissButton = {
                TextButton(onClick = { askShare = false; share(false) }, modifier = Modifier.testTag("share_all")) {
                    Text("Весь список")
                }
            },
        )
    }
}

@Composable
fun ShoppingContent(
    state: ShoppingUiState,
    onMode: (ShoppingMode) -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onRange: (LocalDate, LocalDate) -> Unit,
    onToggle: (String) -> Unit,
    onClear: () -> Unit,
    onShare: () -> Unit,
    onOpenMenu: () -> Unit,
) {
    var showRange by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), contentAlignment = Alignment.TopCenter) {
        Column(Modifier.widthIn(max = 840.dp).fillMaxSize().padding(horizontal = 16.dp).testTag("shopping_screen")) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 16.dp)) {
                Text("Покупки", style = MaterialTheme.typography.headlineLarge, modifier = Modifier.weight(1f))
                IconButton(onClick = onClear, enabled = state.done > 0) { Icon(Icons.Filled.RestartAlt, "Снять отметки") }
                IconButton(onClick = onShare, enabled = state.total > 0, modifier = Modifier.testTag("share_shopping")) { Icon(Icons.Filled.Share, "Поделиться") }
            }
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                ShoppingMode.entries.forEachIndexed { i, m ->
                    SegmentedButton(
                        selected = state.mode == m,
                        onClick = { if (m == ShoppingMode.RANGE) showRange = true else onMode(m) },
                        shape = SegmentedButtonDefaults.itemShape(i, ShoppingMode.entries.size),
                        modifier = Modifier.testTag("mode_${m.name}"),
                    ) { Text(m.title) }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onPrev) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Назад") }
                Text(rangeLabel(state.range.from, state.range.to), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                IconButton(onClick = { showRange = true }) { Icon(Icons.Filled.DateRange, "Выбрать период") }
                IconButton(onClick = onNext) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Вперёд") }
            }
            when {
                state.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                state.total == 0 -> EmptyState(
                    "Список пуст", "Запланируйте блюда в меню — продукты появятся здесь автоматически",
                    Illustration.BASKET, Modifier.fillMaxSize(), "Открыть меню", onOpenMenu,
                )
                else -> {
                    Text("Куплено ${state.done} из ${state.total} · блюд: ${state.dishes}", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.testTag("shopping_progress_text"))
                    Spacer(Modifier.height(6.dp))
                    LinearProgressIndicator(progress = { state.progress }, modifier = Modifier.fillMaxWidth().height(8.dp).testTag("shopping_progress"))
                    Spacer(Modifier.height(8.dp))
                    LazyColumn(Modifier.fillMaxSize().testTag("shopping_list"), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(state.groups, key = { it.department.name }) { g ->
                            Card(shape = MaterialTheme.shapes.extraLarge, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                                Column(Modifier.padding(vertical = 8.dp)) {
                                    Text(g.department.title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
                                    g.items.forEach { item -> ShoppingRow(item, item.key in state.checked) { onToggle(item.key) } }
                                }
                            }
                        }
                        item { Spacer(Modifier.height(24.dp)) }
                    }
                }
            }
        }
    }
    if (showRange) RangePickerDialog(state.range.from, state.range.to, { showRange = false }) { f, t -> showRange = false; onRange(f, t) }
}

@Composable
private fun ShoppingRow(item: ShoppingItem, checked: Boolean, onToggle: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onToggle).padding(horizontal = 4.dp).testTag("shopping_item_${item.product.name}"),
    ) {
        Checkbox(checked = checked, onCheckedChange = { onToggle() })
        Text(
            item.product.name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f),
            textDecoration = if (checked) TextDecoration.LineThrough else null,
            color = if (checked) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
        )
        Text(item.text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(end = 16.dp).testTag("shopping_amount_${item.product.name}"))
    }
}
