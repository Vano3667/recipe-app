package com.example.homemenu.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.homemenu.BuildConfig
import com.example.homemenu.domain.model.ThemeMode
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.homemenu.domain.backup.DuplicateMode
import com.example.homemenu.domain.backup.ImportPreview
import com.example.homemenu.domain.backup.ImportReport
import java.time.LocalDate

@Composable
fun SettingsRoute(vm: SettingsViewModel = hiltViewModel()) {
    val theme by vm.theme.collectAsState()
    val busy by vm.busy.collectAsState()
    val message by vm.message.collectAsState()
    val staged by vm.staged.collectAsState()
    val report by vm.report.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(message) {
        message?.let { snackbar.showSnackbar(it); vm.messageShown() }
    }
    val exportZipLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        uri?.let(vm::exportZip)
    }
    val exportJsonLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri?.let(vm::exportJson)
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let(vm::stageImport)
    }
    Scaffold(snackbarHost = { SnackbarHost(snackbar) }, containerColor = MaterialTheme.colorScheme.background) { padding ->
        SettingsContent(
            theme = theme, busy = busy, modifier = Modifier.padding(padding),
            onTheme = vm::setTheme,
            onExport = { exportZipLauncher.launch("home-menu-backup-${LocalDate.now()}.zip") },
            onExportJson = { exportJsonLauncher.launch("home-menu-backup-${LocalDate.now()}.json") },
            onImport = {
                importLauncher.launch(
                    arrayOf("application/zip", "application/x-zip-compressed", "application/json", "text/plain", "application/octet-stream"),
                )
            },
            appearance = { AppearanceSection() },
        )
    }
    staged?.let { s ->
        ImportPreviewDialog(preview = s.preview, onConfirm = vm::confirmImport, onCancel = vm::cancelImport)
    }
    report?.let { r -> ImportReportDialog(report = r, onDismiss = vm::reportShown) }
}

@Composable
fun ImportPreviewDialog(preview: ImportPreview, onConfirm: (DuplicateMode) -> Unit, onCancel: () -> Unit) {
    var mode by remember { mutableStateOf(DuplicateMode.SKIP) }
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text("Импорт: проверьте данные") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Файл: ${preview.sourceKind}, версия формата ${preview.formatVersion}")
                Text("Рецептов: ${preview.recipes} (новых: ${preview.newRecipes})")
                Text("Ингредиентов: ${preview.ingredients}, шагов: ${preview.steps}, тегов: ${preview.tags}")
                Text("Блюд в меню: ${preview.menuEntries}")
                Text("Фото: ${preview.photosAvailable} из ${preview.photosReferenced}")
                preview.warnings.forEach { Text("⚠ $it", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                if (preview.duplicates.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Text("Уже есть в приложении: ${preview.duplicates.size}", style = MaterialTheme.typography.titleSmall)
                    Text(
                        preview.duplicates.take(5).joinToString(", ") { it.name } + if (preview.duplicates.size > 5) "…" else "",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Column(Modifier.selectableGroup()) {
                        DuplicateMode.entries.forEach { m ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                                    .selectable(selected = mode == m, onClick = { mode = m }, role = Role.RadioButton)
                                    .padding(vertical = 4.dp)
                                    .testTag("duplicate_${m.name}"),
                            ) {
                                RadioButton(selected = mode == m, onClick = null)
                                Spacer(Modifier.width(8.dp))
                                Column {
                                    Text(m.title, style = MaterialTheme.typography.bodyLarge)
                                    Text(m.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                    if (mode == DuplicateMode.UPDATE) {
                        Text(
                            "Внимание: совпадающие рецепты будут перезаписаны данными из файла.",
                            color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(mode) }, modifier = Modifier.testTag("confirm_import")) { Text("Импортировать") }
        },
        dismissButton = { TextButton(onClick = onCancel) { Text("Отмена") } },
    )
}

@Composable
fun ImportReportDialog(report: ImportReport, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (report.verified) "Импорт завершён" else "Импорт завершён с ошибками") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(report.summary())
                if (report.verified) Text("Данные проверены: все рецепты сохранены в приложении.")
                if (report.failed.isNotEmpty()) {
                    Text("Не удалось проверить: " + report.failed.joinToString(", "), color = MaterialTheme.colorScheme.error)
                }
                report.warnings.forEach { Text("⚠ $it", style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss, modifier = Modifier.testTag("import_report_ok")) { Text("Готово") } },
    )
}

/**
 * [appearance] = content of the «Оформление» card (themes + custom colours). Null hides the card,
 * so tests that render SettingsContent without Hilt keep working.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsContent(
    theme: ThemeMode,
    busy: Boolean,
    onTheme: (ThemeMode) -> Unit,
    onExport: () -> Unit,
    onImport: () -> Unit,
    modifier: Modifier = Modifier,
    onExportJson: () -> Unit = {},
    appearance: (@Composable () -> Unit)? = null,
) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            Modifier.widthIn(max = 720.dp).fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp).testTag("settings_screen"),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("Настройки", style = MaterialTheme.typography.headlineLarge)

            if (appearance != null) {
                SettingsCard("Оформление") { appearance() }
            }

            SettingsCard("Тема оформления") {
                if (appearance != null) {
                    Text(
                        "Светлый и тёмный режим относятся к теме «Классическая».",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Column(Modifier.selectableGroup()) {
                    ThemeMode.entries.forEach { m ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                                .selectable(selected = theme == m, onClick = { onTheme(m) }, role = Role.RadioButton)
                                .padding(vertical = 6.dp)
                                .testTag("theme_${m.name}"),
                        ) {
                            RadioButton(selected = theme == m, onClick = null)
                            Spacer(Modifier.width(12.dp))
                            Text(m.title, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            }

            SettingsCard("Резервная копия") {
                Text(
                    "ZIP — архив с рецептами, меню, тегами и фотографиями. JSON — один файл, фото внутри. Перед импортом покажем, что будет добавлено, и спросим, что делать с совпадающими рецептами.",
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onExport, enabled = !busy, modifier = Modifier.testTag("export_backup")) {
                        Icon(Icons.Filled.FileUpload, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Экспорт ZIP")
                    }
                    OutlinedButton(onClick = onExportJson, enabled = !busy, modifier = Modifier.testTag("export_json")) {
                        Icon(Icons.Filled.FileUpload, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("JSON")
                    }
                    OutlinedButton(onClick = onImport, enabled = !busy, modifier = Modifier.testTag("import_backup")) {
                        Icon(Icons.Filled.FileDownload, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Импорт")
                    }
                    if (busy) CircularProgressIndicator(Modifier.size(24.dp))
                }
            }

            SettingsCard("О приложении") {
                Text("Домашнее меню · версия ${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.bodyLarge)
                Text("Сборка ${BuildConfig.VERSION_CODE}", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.testTag("about_build"))
                Text(
                    "Рецепты, меню на неделю и список покупок. Работает полностью офлайн, данные хранятся только на устройстве.\nШрифты Lora и Nunito — SIL Open Font License. Иллюстрации нарисованы в коде (CC0).",
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun SettingsCard(title: String, content: @Composable () -> Unit) {
    Card(
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(8.dp))
            content()
        }
    }
}
