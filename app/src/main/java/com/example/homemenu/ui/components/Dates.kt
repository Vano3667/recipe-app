package com.example.homemenu.ui.components

import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

val RU: Locale = Locale.forLanguageTag("ru-RU")
private val DAY_MONTH = DateTimeFormatter.ofPattern("d MMMM", RU)
private val DAY_MONTH_SHORT = DateTimeFormatter.ofPattern("d MMM", RU)

fun LocalDate.dayMonth(): String = format(DAY_MONTH)
fun LocalDate.dayMonthShort(): String = format(DAY_MONTH_SHORT)
fun LocalDate.weekdayName(): String = dayOfWeek.getDisplayName(TextStyle.FULL, RU).replaceFirstChar { it.uppercase() }

fun rangeLabel(from: LocalDate, to: LocalDate): String =
    if (from == to) "${from.weekdayName()}, ${from.dayMonth()}" else "${from.dayMonthShort()} — ${to.dayMonthShort()}"

fun LocalDate.toUtcMillis(): Long = atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
fun Long.utcMillisToDate(): LocalDate = Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()

@Composable
fun RangePickerDialog(from: LocalDate, to: LocalDate, onDismiss: () -> Unit, onConfirm: (LocalDate, LocalDate) -> Unit) {
    val state = rememberDateRangePickerState(initialSelectedStartDateMillis = from.toUtcMillis(), initialSelectedEndDateMillis = to.toUtcMillis())
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    val s = state.selectedStartDateMillis
                    if (s != null) {
                        val e = state.selectedEndDateMillis ?: s
                        onConfirm(s.utcMillisToDate(), e.utcMillisToDate())
                    } else onDismiss()
                },
                modifier = Modifier.testTag("range_confirm"),
            ) { Text("Готово") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } },
    ) {
        DateRangePicker(state = state, modifier = Modifier.weight(1f), title = { Text("  Выберите период") })
    }
}
