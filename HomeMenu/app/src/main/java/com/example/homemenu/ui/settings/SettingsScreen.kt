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
import java.time.LocalDate

@Composable
fun SettingsRoute(vm: SettingsViewModel = hiltViewModel()) {
    val theme by vm.theme.collectAsState()
    val busy by vm.busy.collectAsState()
    val message by vm.message.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(message) {
        message?.let { snackbar.showSnackbar(it); vm.messageShown() }
    }
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        uri?.let(vm::export)
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let(vm::import)
    }
    Scaffold(snackbarHost = { SnackbarHost(snackbar) }, containerColor = MaterialTheme.colorScheme.background) { padding ->
        SettingsContent(
            theme = theme, busy = busy, modifier = Modifier.padding(padding),
            onTheme = vm::setTheme,
            onExport = { exportLauncher.launch("home-menu-backup-${LocalDate.now()}.zip") },
            onImport = { importLauncher.launch(arrayOf("application/zip", "application/octet-stream", "application/x-zip-compressed")) },
        )
    }
}

@Composable
fun SettingsContent(
    theme: ThemeMode,
    busy: Boolean,
    onTheme: (ThemeMode) -> Unit,
    onExport: () -> Unit,
    onImport: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            Modifier.widthIn(max = 720.dp).fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp).testTag("settings_screen"),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("Настройки", style = MaterialTheme.typography.headlineLarge)

            SettingsCard("Тема оформления") {
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
                    "Архив .zip с рецептами, меню и фотографиями. При импорте данные добавляются к существующим — ничего не удаляется.",
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Button(onClick = onExport, enabled = !busy, modifier = Modifier.testTag("export_backup")) {
                        Icon(Icons.Filled.FileUpload, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Экспорт")
                    }
                    OutlinedButton(onClick = onImport, enabled = !busy, modifier = Modifier.testTag("import_backup")) {
                        Icon(Icons.Filled.FileDownload, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Импорт")
                    }
                    if (busy) CircularProgressIndicator(Modifier.size(24.dp))
                }
            }

            SettingsCard("О приложении") {
                Text("Домашнее меню · версия ${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.bodyLarge)
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
