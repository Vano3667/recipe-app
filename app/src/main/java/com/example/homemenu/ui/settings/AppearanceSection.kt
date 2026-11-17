package com.example.homemenu.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.homemenu.ui.theme.AppThemeId
import com.example.homemenu.ui.theme.ColorMath
import com.example.homemenu.ui.theme.CustomAppearance
import com.example.homemenu.ui.theme.CustomRole
import com.example.homemenu.ui.theme.ThemePalette
import com.example.homemenu.ui.theme.ThemePalettes

@Composable
fun AppearanceSection(vm: AppearanceViewModel = hiltViewModel()) {
    val state by vm.state.collectAsState()
    AppearanceContent(
        state = state,
        onSelect = vm::select,
        onColor = vm::setCustomColor,
        onBase = vm::setCustomBase,
        onResetCustom = vm::resetCustom,
        onResetDefault = vm::resetToDefault,
    )
}

@Composable
fun AppearanceContent(
    state: AppearanceUi,
    onSelect: (AppThemeId) -> Unit,
    onColor: (CustomRole, Long) -> Unit,
    onBase: (AppThemeId) -> Unit,
    onResetCustom: () -> Unit,
    onResetDefault: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.testTag("appearance_section")) {
        Text(
            "Выберите тему из списка, она применится сразу. Выбор сохраняется после перезапуска и обновления.",
            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        ThemeDropdownHeader(
            id = state.themeId,
            palette = ThemePalettes.previewOf(state.themeId, state.custom),
            expanded = expanded,
            onClick = { expanded = !expanded },
        )
        if (expanded) {
            // Compact grid: two themes per row.
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.testTag("theme_grid")) {
                AppThemeId.entries.chunked(2).forEachIndexed { row, pair ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth().testTag("theme_row_$row"),
                    ) {
                        pair.forEach { id ->
                            ThemePreviewCard(
                                id = id,
                                palette = ThemePalettes.previewOf(id, state.custom),
                                selected = state.themeId == id,
                                onClick = { expanded = false; onSelect(id) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                        if (pair.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
        Text(
            AppThemeId.entries.first { it == state.themeId }.description,
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (state.themeId == AppThemeId.CUSTOM) {
            CustomColorsEditor(custom = state.custom, onColor = onColor, onBase = onBase, onReset = onResetCustom)
        }
        OutlinedButton(
            onClick = onResetDefault,
            enabled = state.themeId != AppThemeId.LEGACY,
            modifier = Modifier.testTag("appearance_reset"),
        ) { Text("Вернуть тему по умолчанию") }
    }
}

/** One-line selector: current theme's colours and name; tap to show or hide all themes. */
@Composable
private fun ThemeDropdownHeader(id: AppThemeId, palette: ThemePalette, expanded: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .border(1.dp, MaterialTheme.colorScheme.outline, shape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .testTag("theme_dropdown"),
    ) {
        listOf(palette.background, palette.primary, palette.secondaryContainer).forEach { c ->
            Box(
                Modifier.size(18.dp).clip(CircleShape).background(Color(c))
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape),
            )
            Spacer(Modifier.width(4.dp))
        }
        Spacer(Modifier.width(8.dp))
        Text(
            id.title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f),
            maxLines = 1, overflow = TextOverflow.Ellipsis,
        )
        Icon(
            if (expanded) Icons.Filled.ArrowDropUp else Icons.Filled.ArrowDropDown,
            contentDescription = if (expanded) "Свернуть список тем" else "Показать все темы",
        )
    }
}

/** Mini preview drawn in the theme's own colours, independent of the current theme. */
@Composable
private fun ThemePreviewCard(
    id: AppThemeId,
    palette: ThemePalette,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(14.dp)
    val borderColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
    Column(
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier
            .clip(shape)
            .background(Color(palette.background))
            .border(if (selected) 3.dp else 1.dp, borderColor, shape)
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton)
            .padding(8.dp)
            .testTag("theme_card_${id.name}"),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(5.dp),
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Color(palette.surfaceHigh)).padding(6.dp),
        ) {
            Box(Modifier.width(64.dp).height(7.dp).clip(RoundedCornerShape(4.dp)).background(Color(palette.onColor(palette.surfaceHigh))))
            Box(Modifier.width(40.dp).height(5.dp).clip(RoundedCornerShape(3.dp)).background(Color(palette.onVariant(palette.surfaceHigh))))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.width(44.dp).height(18.dp).clip(RoundedCornerShape(9.dp)).background(Color(palette.primary)),
            ) {
                Box(Modifier.width(20.dp).height(4.dp).clip(RoundedCornerShape(2.dp)).background(Color(palette.onColor(palette.primary))))
            }
            Box(Modifier.size(18.dp).clip(CircleShape).background(Color(palette.primaryContainer)))
            Box(Modifier.size(18.dp).clip(CircleShape).background(Color(palette.secondaryContainer)))
        }
        Text(
            (if (selected) "✓ " else "") + id.title,
            style = MaterialTheme.typography.labelLarge,
            color = Color(palette.onColor(palette.background)),
            maxLines = 2, overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun CustomColorsEditor(
    custom: CustomAppearance,
    onColor: (CustomRole, Long) -> Unit,
    onBase: (AppThemeId) -> Unit,
    onReset: () -> Unit,
) {
    val palette = remember(custom) { ThemePalettes.custom(custom) }
    var editing by remember { mutableStateOf<CustomRole?>(null) }
    var baseMenu by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.testTag("custom_colors")) {
        Text("Свои цвета", style = MaterialTheme.typography.titleMedium)
        Box {
            OutlinedButton(onClick = { baseMenu = true }, modifier = Modifier.testTag("custom_base")) {
                Text("Основа: ${custom.base.title}")
            }
            DropdownMenu(expanded = baseMenu, onDismissRequest = { baseMenu = false }) {
                AppThemeId.presets.forEach { id ->
                    DropdownMenuItem(text = { Text(id.title) }, onClick = { baseMenu = false; onBase(id) })
                }
            }
        }
        CustomRole.entries.forEach { role ->
            val value = ThemePalettes.roleColor(palette, role)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
                    .clickable(role = Role.Button) { editing = role }
                    .padding(vertical = 6.dp)
                    .testTag("custom_role_${role.name}"),
            ) {
                Box(
                    Modifier.size(32.dp).clip(CircleShape).background(Color(value))
                        .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape),
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(role.title, style = MaterialTheme.typography.bodyLarge)
                    Text(
                        ColorMath.toHex(value) + if (custom.colors.containsKey(role)) "" else " · из основы",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        ThemePalettes.contrastWarnings(palette).forEach { warning ->
            Text("⚠ $warning", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
        TextButton(onClick = onReset, modifier = Modifier.testTag("custom_reset")) { Text("Сбросить свои цвета") }
    }
    editing?.let { role ->
        ColorPickerDialog(
            title = role.title,
            initial = ThemePalettes.roleColor(palette, role),
            onDismiss = { editing = null },
            onConfirm = { color -> onColor(role, color); editing = null },
        )
    }
}

/** Colour picker: swatches + hue / saturation / brightness sliders; HEX field is optional. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ColorPickerDialog(title: String, initial: Long, onDismiss: () -> Unit, onConfirm: (Long) -> Unit) {
    val start = remember(initial) { ColorMath.toHsv(initial) }
    var hue by remember { mutableFloatStateOf(start[0]) }
    var sat by remember { mutableFloatStateOf(start[1]) }
    var value by remember { mutableFloatStateOf(start[2]) }
    var hexText by remember { mutableStateOf(ColorMath.toHex(initial)) }
    val color = ColorMath.fromHsv(hue, sat, value)
    fun setColor(c: Long) {
        val hsv = ColorMath.toHsv(c)
        hue = hsv[0]; sat = hsv[1]; value = hsv[2]
        hexText = ColorMath.toHex(c)
    }
    fun syncHex() { hexText = ColorMath.toHex(ColorMath.fromHsv(hue, sat, value)) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    Modifier.fillMaxWidth().height(56.dp).clip(RoundedCornerShape(12.dp)).background(Color(color))
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                        .testTag("picker_preview"),
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ThemePalettes.SWATCHES.forEach { swatch ->
                        Box(
                            Modifier.size(32.dp).clip(CircleShape).background(Color(swatch))
                                .border(if (swatch == color) 3.dp else 1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                                .clickable(role = Role.Button) { setColor(swatch) },
                        )
                    }
                }
                Text("Оттенок", style = MaterialTheme.typography.labelLarge)
                Box(
                    Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp))
                        .background(Brush.horizontalGradient((0..6).map { Color(ColorMath.fromHsv(it * 60f, 1f, 1f)) })),
                )
                Slider(value = hue, onValueChange = { hue = it; syncHex() }, valueRange = 0f..360f, modifier = Modifier.testTag("picker_hue"))
                Text("Насыщенность", style = MaterialTheme.typography.labelLarge)
                Slider(value = sat, onValueChange = { sat = it; syncHex() }, modifier = Modifier.testTag("picker_saturation"))
                Text("Яркость", style = MaterialTheme.typography.labelLarge)
                Slider(value = value, onValueChange = { value = it; syncHex() }, modifier = Modifier.testTag("picker_value"))
                OutlinedTextField(
                    value = hexText,
                    onValueChange = { t ->
                        hexText = t.take(7)
                        ColorMath.parseHex(t)?.let { c ->
                            val hsv = ColorMath.toHsv(c)
                            hue = hsv[0]; sat = hsv[1]; value = hsv[2]
                        }
                    },
                    label = { Text("HEX (необязательно)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("picker_hex"),
                )
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(color) }, modifier = Modifier.testTag("picker_ok")) { Text("Готово") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } },
    )
}
