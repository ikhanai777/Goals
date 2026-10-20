package com.goals.app.ui.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.goals.app.data.GoalsRepository
import com.goals.app.data.ProjectEntity
import com.goals.app.domain.formatDate
import com.goals.app.ui.AppState
import com.goals.app.ui.components.DatePickerModal
import com.goals.app.ui.components.DateRow
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectEditorScreen(
    state: AppState,
    initial: ProjectEntity?,
    onBack: () -> Unit,
    onSave: (ProjectEntity) -> Unit,
) {
    val today = state.today
    var title by remember { mutableStateOf(initial?.title.orEmpty()) }
    var notes by remember { mutableStateOf(initial?.notes.orEmpty()) }
    var areaId by remember { mutableStateOf(initial?.areaId) }
    var start by remember { mutableStateOf(initial?.startDate?.let(LocalDate::ofEpochDay) ?: today) }
    var due by remember { mutableStateOf(initial?.dueDate?.let(LocalDate::ofEpochDay)) }
    var picking by remember { mutableStateOf<String?>(null) }
    var attempted by remember { mutableStateOf(false) }
    val titleError = attempted && title.isBlank()
    val dateError = due?.isBefore(start) == true

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back") } },
                title = { Text(if (initial == null) "New project" else "Edit project", style = MaterialTheme.typography.titleLarge) },
                actions = {
                    TextButton(onClick = {
                        attempted = true
                        if (title.isNotBlank() && !dateError) {
                            val now = System.currentTimeMillis()
                            onSave(
                                ProjectEntity(
                                    id = initial?.id ?: GoalsRepository.newId(),
                                    areaId = areaId,
                                    title = title.trim(),
                                    notes = notes.trim().ifEmpty { null },
                                    startDate = start.toEpochDay(),
                                    dueDate = due?.toEpochDay(),
                                    archived = initial?.archived ?: false,
                                    createdAt = initial?.createdAt ?: now,
                                    updatedAt = now,
                                ),
                            )
                        }
                    }) { Text("Save") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).imePadding().verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it.take(80) },
                label = { Text("Title") },
                singleLine = true,
                isError = titleError,
                supportingText = if (titleError) ({ Text("Title is required") }) else null,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth(),
            )
            DateRow("Start", formatDate(start, today)) { picking = "start" }
            DateRow("Due", due?.let { formatDate(it, today) } ?: "No due date") { picking = "due" }
            if (dateError) Text("Due date is before the start date", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            Text("Area", style = MaterialTheme.typography.titleSmall)
            AreaPicker(state.areas, areaId) { areaId = it }
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes") },
                minLines = 3,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
    when (picking) {
        "start" -> DatePickerModal(start, onDismiss = { picking = null }) { start = it; picking = null }
        "due" -> DatePickerModal(due ?: today, onDismiss = { picking = null }) { due = it; picking = null }
    }
}
