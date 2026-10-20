package com.goals.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.goals.app.data.GoalType
import com.goals.app.domain.formatDate
import com.goals.app.domain.formatNumber
import com.goals.app.domain.tapIncrement
import com.goals.app.ui.GoalItem
import com.goals.app.ui.theme.GoalsTheme
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/** Enter an amount (and optionally a date) to log against a goal. */
@Composable
fun LogDialog(
    item: GoalItem,
    today: LocalDate,
    allowDate: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (value: Double, date: LocalDate) -> Unit,
) {
    val goal = item.goal
    var text by remember { mutableStateOf(formatNumber(goal.tapIncrement())) }
    var date by remember { mutableStateOf(today) }
    var picking by remember { mutableStateOf(false) }
    val value = text.toDoubleOrNull()
    val valid = value != null && value > 0
    val unit = when (goal.type) {
        GoalType.DURATION -> "min"
        GoalType.QUANTITY -> goal.unit
        else -> null
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(goal.title, style = MaterialTheme.typography.titleMedium) },
        text = {
            Column {
                if (goal.type != GoalType.CHECK) {
                    OutlinedTextField(
                        value = text,
                        onValueChange = { text = it.filter { c -> c.isDigit() || c == '.' } },
                        singleLine = true,
                        label = { Text("Amount") },
                        suffix = unit?.let { { Text(it) } },
                        isError = !valid,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                if (allowDate) {
                    Spacer(Modifier.height(8.dp))
                    DateRow("Date", formatDate(date, today)) { picking = true }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(if (goal.type == GoalType.CHECK) 1.0 else value!!, date) }, enabled = goal.type == GoalType.CHECK || valid) {
                Text("Log")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
    if (picking) {
        DatePickerModal(date, maxDate = today, onDismiss = { picking = false }) { date = it; picking = false }
    }
}

@Composable
fun DateRow(label: String, value: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Row(
        modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.CalendarToday, null, Modifier.size(18.dp), tint = GoalsTheme.extra.muted)
        Spacer(Modifier.width(12.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerModal(
    initial: LocalDate?,
    maxDate: LocalDate? = null,
    onDismiss: () -> Unit,
    onPick: (LocalDate) -> Unit,
) {
    val state = rememberDatePickerState(
        initialSelectedDateMillis = initial?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli(),
        selectableDates = object : androidx.compose.material3.SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                maxDate == null || utcTimeMillis <= maxDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        },
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                state.selectedDateMillis?.let {
                    onPick(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate())
                } ?: onDismiss()
            }) { Text("OK") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    ) {
        DatePicker(state, showModeToggle = false)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerModal(hour: Int, minute: Int, onDismiss: () -> Unit, onPick: (Int, Int) -> Unit) {
    val state = rememberTimePickerState(hour, minute)
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = { onPick(state.hour, state.minute) }) { Text("OK") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        text = { TimePicker(state) },
    )
}

@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirm: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(confirm, color = MaterialTheme.colorScheme.error) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
