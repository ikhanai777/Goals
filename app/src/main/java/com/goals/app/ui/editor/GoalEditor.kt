package com.goals.app.ui.editor

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.goals.app.data.AreaEntity
import com.goals.app.data.Cadence
import com.goals.app.data.GoalType
import com.goals.app.domain.bit
import com.goals.app.domain.formatDate
import com.goals.app.domain.label
import com.goals.app.ui.AppState
import com.goals.app.ui.GoalDraft
import com.goals.app.ui.GoalDraft.Field
import com.goals.app.ui.components.DateRow
import com.goals.app.ui.components.DatePickerModal
import com.goals.app.ui.components.GoalIcons
import com.goals.app.ui.components.TimePickerModal
import com.goals.app.ui.theme.AreaColors
import com.goals.app.ui.theme.GoalsTheme
import java.time.DayOfWeek
import java.time.LocalDate

private val UnitSuggestions = listOf("km", "pages", "$", "kg", "steps", "words")

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun <T> ChipRow(options: List<T>, selected: T, label: (T) -> String, onSelect: (T) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        options.forEach { option ->
            FilterChip(
                selected = option == selected,
                onClick = { onSelect(option) },
                label = { Text(label(option), style = MaterialTheme.typography.labelMedium) },
                modifier = Modifier.height(32.dp),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.secondary,
                    selectedLabelColor = MaterialTheme.colorScheme.onSecondary,
                ),
            )
        }
    }
}

private fun targetLabel(draft: GoalDraft): String = when (draft.type) {
    GoalType.CHECK -> when (draft.cadence) {
        Cadence.WEEKLY -> "Times per week"
        Cadence.MONTHLY -> "Times per month"
        Cadence.YEARLY -> "Times per year"
        else -> "Times"
    }
    GoalType.DURATION -> "Target minutes"
    else -> "Target"
}

/** Check goals only need a target when they recur on a period longer than a day. */
private fun showsTarget(draft: GoalDraft) =
    draft.type != GoalType.CHECK || draft.cadence in setOf(Cadence.WEEKLY, Cadence.MONTHLY, Cadence.YEARLY)

@Composable
private fun TargetFields(draft: GoalDraft, errors: Map<Field, String>, onChange: (GoalDraft) -> Unit) {
    if (!showsTarget(draft)) return
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = draft.target,
            onValueChange = { v -> onChange(draft.copy(target = v.filter { it.isDigit() || it == '.' })) },
            label = { Text(targetLabel(draft)) },
            placeholder = { if (draft.type == GoalType.CHECK) Text("1") },
            singleLine = true,
            isError = Field.TARGET in errors,
            supportingText = errors[Field.TARGET]?.let { { Text(it) } },
            suffix = if (draft.type == GoalType.DURATION) ({ Text("min") }) else null,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.weight(1f),
        )
        if (draft.type == GoalType.QUANTITY) {
            OutlinedTextField(
                value = draft.unit,
                onValueChange = { onChange(draft.copy(unit = it.take(12))) },
                label = { Text("Unit") },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
        }
    }
    if (draft.type == GoalType.QUANTITY && draft.unit.isBlank()) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            UnitSuggestions.forEach { u ->
                Text(
                    u,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .clip(MaterialTheme.shapes.extraSmall)
                        .clickable { onChange(draft.copy(unit = u)) }
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun DeadlineRow(draft: GoalDraft, today: LocalDate, errors: Map<Field, String>, onPick: () -> Unit) {
    val label = if (draft.cadence == Cadence.ONCE) "Deadline" else "End date"
    DateRow(label, draft.endDate?.let { formatDate(it, today) } ?: if (draft.cadence == Cadence.ONCE) "Pick a date" else "No end", onClick = onPick)
    errors[Field.END_DATE]?.let {
        Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
    }
}

/** Bottom-sheet content for creating a goal with just a title, cadence, type and target. */
@Composable
fun QuickAddContent(
    draft: GoalDraft,
    today: LocalDate,
    onChange: (GoalDraft) -> Unit,
    onMoreOptions: () -> Unit,
    onCreate: () -> Unit,
    autoFocus: Boolean = true,
) {
    var attempted by remember { mutableStateOf(false) }
    var picking by remember { mutableStateOf(false) }
    val errors = if (attempted) draft.errors() else emptyMap()
    val focus = remember { FocusRequester() }
    if (autoFocus) LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }

    Column(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 16.dp).imePadding(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("New goal", style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(
            value = draft.title,
            onValueChange = { onChange(draft.copy(title = it)) },
            placeholder = { Text("e.g. Read 30 minutes") },
            singleLine = true,
            isError = Field.TITLE in errors,
            supportingText = errors[Field.TITLE]?.let { { Text(it) } },
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            modifier = Modifier.fillMaxWidth().focusRequester(focus),
        )
        ChipRow(Cadence.entries, draft.cadence, { it.label() }) { onChange(draft.copy(cadence = it)) }
        ChipRow(GoalType.entries, draft.type, { it.label() }) { onChange(draft.copy(type = it)) }
        TargetFields(draft, errors, onChange)
        if (draft.cadence == Cadence.ONCE) DeadlineRow(draft, today, errors) { picking = true }
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onMoreOptions) { Text("More options ›") }
            Spacer(Modifier.weight(1f))
            Button(onClick = {
                attempted = true
                if (draft.errors().isEmpty()) onCreate()
            }) { Text("Create") }
        }
    }
    if (picking) {
        DatePickerModal(draft.endDate ?: today, onDismiss = { picking = false }) {
            onChange(draft.copy(endDate = it)); picking = false
        }
    }
}

/** Full-screen editor with every goal option, used for "More options" and for editing. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun GoalEditorScreen(
    state: AppState,
    initial: GoalDraft,
    onBack: () -> Unit,
    onSave: (GoalDraft) -> Unit,
) {
    var draft by remember { mutableStateOf(initial) }
    var attempted by remember { mutableStateOf(false) }
    var pickingStart by remember { mutableStateOf(false) }
    var pickingEnd by remember { mutableStateOf(false) }
    var pickingTime by remember { mutableStateOf(false) }
    val errors = if (attempted) draft.errors() else emptyMap()
    val today = state.today
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back") } },
                title = { Text(if (initial.id == null) "New goal" else "Edit goal", style = MaterialTheme.typography.titleLarge) },
                actions = {
                    TextButton(onClick = {
                        attempted = true
                        if (draft.errors().isEmpty()) onSave(draft)
                    }) { Text("Save") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                value = draft.title,
                onValueChange = { draft = draft.copy(title = it) },
                label = { Text("Title") },
                singleLine = true,
                isError = Field.TITLE in errors,
                supportingText = errors[Field.TITLE]?.let { { Text(it) } },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth(),
            )
            Label("Tracker")
            ChipRow(Cadence.entries, draft.cadence, { it.label() }) { draft = draft.copy(cadence = it) }
            ChipRow(GoalType.entries, draft.type, { it.label() }) { draft = draft.copy(type = it) }
            TargetFields(draft, errors) { draft = it }
            if (draft.type != GoalType.CHECK) {
                OutlinedTextField(
                    value = draft.increment,
                    onValueChange = { v -> draft = draft.copy(increment = v.filter { it.isDigit() || it == '.' }) },
                    label = { Text("Amount per tap") },
                    placeholder = { Text(if (draft.type == GoalType.DURATION) "15" else "1") },
                    singleLine = true,
                    isError = Field.INCREMENT in errors,
                    supportingText = { Text(errors[Field.INCREMENT] ?: "Added when you swipe or tap +") },
                    suffix = if (draft.type == GoalType.DURATION) ({ Text("min") }) else null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Divider()
            Label("Schedule")
            DateRow("Start", formatDate(draft.startDate, today)) { pickingStart = true }
            DeadlineRow(draft, today, errors) { pickingEnd = true }
            if (draft.endDate != null && draft.cadence != Cadence.ONCE) {
                TextButton(onClick = { draft = draft.copy(endDate = null) }) { Text("Remove end date") }
            }
            if (draft.cadence == Cadence.DAILY) {
                WeekdayPicker(draft.weekdays, state.settings.weekStart) { draft = draft.copy(weekdays = it) }
            }

            Divider()
            Label("Organize")
            AreaPicker(state.areas, draft.areaId) { draft = draft.copy(areaId = it) }
            val projects = state.projects.filter { !it.project.archived }
            if (projects.isNotEmpty()) {
                ChipRow(
                    listOf<String?>(null) + projects.map { it.project.id },
                    draft.projectId,
                    { id -> id?.let { state.project(it)?.project?.title } ?: "No project" },
                ) { draft = draft.copy(projectId = it) }
            }
            IconPicker(draft.icon) { draft = draft.copy(icon = it) }

            Divider()
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Notifications, null, Modifier.size(18.dp), tint = GoalsTheme.extra.muted)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Reminder", style = MaterialTheme.typography.bodyMedium)
                    if (draft.reminderOn) {
                        Text(
                            "%02d:%02d".format(draft.reminderHour, draft.reminderMinute),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clickable { pickingTime = true },
                        )
                    }
                }
                Switch(
                    checked = draft.reminderOn,
                    onCheckedChange = { on ->
                        if (on && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            permission.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                        draft = if (on && !draft.reminderOn && initial.reminderId == null) {
                            val m = state.settings.defaultReminderMinutes
                            draft.copy(reminderOn = true, reminderHour = m / 60, reminderMinute = m % 60)
                        } else {
                            draft.copy(reminderOn = on)
                        }
                    },
                )
            }

            Divider()
            OutlinedTextField(
                value = draft.notes,
                onValueChange = { draft = draft.copy(notes = it) },
                label = { Text("Why this matters") },
                minLines = 2,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(24.dp))
        }
    }
    if (pickingStart) DatePickerModal(draft.startDate, onDismiss = { pickingStart = false }) { draft = draft.copy(startDate = it); pickingStart = false }
    if (pickingEnd) DatePickerModal(draft.endDate ?: today, onDismiss = { pickingEnd = false }) { draft = draft.copy(endDate = it); pickingEnd = false }
    if (pickingTime) {
        TimePickerModal(draft.reminderHour, draft.reminderMinute, onDismiss = { pickingTime = false }) { h, m ->
            draft = draft.copy(reminderHour = h, reminderMinute = m); pickingTime = false
        }
    }
}

@Composable
private fun Label(text: String) {
    Text(text, style = MaterialTheme.typography.titleSmall)
}

@Composable
private fun Divider() = HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

@Composable
private fun WeekdayPicker(mask: Int, weekStart: DayOfWeek, onChange: (Int) -> Unit) {
    val days = (0 until 7).map { weekStart.plus(it.toLong()) }
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        days.forEach { day ->
            val on = mask and day.bit() != 0
            Box(
                Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(if (on) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerLow)
                    .clickable {
                        val next = mask xor day.bit()
                        if (next != 0) onChange(next)
                    },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    day.name.take(1),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (on) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AreaPicker(areas: List<AreaEntity>, selected: String?, onSelect: (String?) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        FilterChip(
            selected = selected == null,
            onClick = { onSelect(null) },
            label = { Text("No area", style = MaterialTheme.typography.labelMedium) },
            modifier = Modifier.height(32.dp),
        )
        areas.forEach { area ->
            FilterChip(
                selected = area.id == selected,
                onClick = { onSelect(area.id) },
                label = { Text(area.name, style = MaterialTheme.typography.labelMedium) },
                leadingIcon = {
                    Box(Modifier.size(8.dp).clip(CircleShape).background(AreaColors[area.colorIndex % AreaColors.size]))
                },
                modifier = Modifier.height(32.dp),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                ),
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun IconPicker(selected: String, onSelect: (String) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        GoalIcons.forEach { (key, icon) ->
            val on = key == selected
            Box(
                Modifier
                    .size(40.dp)
                    .clip(MaterialTheme.shapes.small)
                    .then(if (on) Modifier.border(1.5.dp, MaterialTheme.colorScheme.primary, MaterialTheme.shapes.small) else Modifier)
                    .clickable { onSelect(key) },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    icon, key, Modifier.size(20.dp),
                    tint = if (on) MaterialTheme.colorScheme.primary else GoalsTheme.extra.muted,
                )
            }
        }
    }
}
