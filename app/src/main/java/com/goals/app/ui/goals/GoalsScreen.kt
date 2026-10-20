package com.goals.app.ui.goals

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.goals.app.ui.components.card
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.LaunchedEffect
import com.goals.app.domain.formatDate
import com.goals.app.ui.AppState
import com.goals.app.ui.GoalItem
import com.goals.app.ui.LibraryFilter
import com.goals.app.ui.LibrarySort
import com.goals.app.ui.ProjectItem
import com.goals.app.ui.buildLibrary
import com.goals.app.ui.components.AreaBar
import com.goals.app.ui.components.CadenceBadge
import com.goals.app.ui.components.EmptyState
import com.goals.app.ui.components.ProgressBar
import com.goals.app.ui.components.iconFor
import com.goals.app.ui.theme.GoalsTheme
import com.goals.app.ui.theme.NumericStyle
import java.time.LocalDate
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalsScreen(
    state: AppState,
    onOpenGoal: (String) -> Unit,
    onOpenProject: (String) -> Unit,
    onAdd: () -> Unit,
    initialFilter: LibraryFilter = LibraryFilter.ALL,
) {
    var filter by rememberSaveable { mutableStateOf(initialFilter) }
    var sort by rememberSaveable { mutableStateOf(LibrarySort.DUE) }
    var query by rememberSaveable { mutableStateOf("") }
    var searching by rememberSaveable { mutableStateOf(false) }
    var sortMenu by remember { mutableStateOf(false) }
    var expanded by rememberSaveable { mutableStateOf(setOf<String>()) }
    val library = buildLibrary(state, filter, sort, query)

    Scaffold(

        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onBackground,
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(
                title = {
                    if (searching) {
                        val focus = remember { FocusRequester() }
                        LaunchedEffect(Unit) { focus.requestFocus() }
                        BasicTextField(
                            value = query,
                            onValueChange = { query = it },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            modifier = Modifier.fillMaxWidth().focusRequester(focus),
                            decorationBox = { inner ->
                                if (query.isEmpty()) {
                                    Text("Search goals and projects", style = MaterialTheme.typography.bodyLarge, color = GoalsTheme.extra.muted)
                                }
                                inner()
                            },
                        )
                    } else {
                        Text("Goals", style = MaterialTheme.typography.titleLarge)
                    }
                },
                actions = {
                    if (searching) {
                        IconButton(onClick = { searching = false; query = "" }) { Icon(Icons.Rounded.Close, "Close search") }
                    } else {
                        IconButton(onClick = { searching = true }) { Icon(Icons.Rounded.Search, "Search") }
                    }
                    Box {
                        IconButton(onClick = { sortMenu = true }) { Icon(Icons.AutoMirrored.Rounded.Sort, "Sort") }
                        DropdownMenu(sortMenu, onDismissRequest = { sortMenu = false }) {
                            LibrarySort.entries.forEach { s ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            s.label,
                                            color = if (s == sort) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                        )
                                    },
                                    onClick = { sort = s; sortMenu = false },
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                items(LibraryFilter.entries) { f ->
                    FilterChip(
                        selected = f == filter,
                        onClick = { filter = f },
                        label = { Text(f.label, style = MaterialTheme.typography.labelMedium) },
                        modifier = Modifier.height(32.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.secondary,
                            selectedLabelColor = MaterialTheme.colorScheme.onSecondary,
                        ),
                    )
                }
            }
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (library.projects.isEmpty() && library.goals.isEmpty()) {
                    item {
                        EmptyState(
                            if (query.isNotBlank()) "No matches for \"$query\"" else "Nothing here yet",
                            if (filter == LibraryFilter.ARCHIVED || query.isNotBlank()) null else "Add a goal",
                            onAdd,
                        )
                    }
                }
                for (project in library.projects) {
                    val open = project.project.id in expanded
                    item(key = "p-${project.project.id}") {
                        ProjectRow(
                            project, state.today, open,
                            onOpen = { onOpenProject(project.project.id) },
                            onToggle = {
                                expanded = if (open) expanded - project.project.id else expanded + project.project.id
                            },
                        )
                    }
                    if (open) {
                        items(project.goals, key = { "pg-${it.goal.id}" }) { goal ->
                            LibraryGoalRow(goal, state.today, Modifier.padding(start = 16.dp)) { onOpenGoal(goal.goal.id) }
                        }
                    }
                }
                if (library.projects.isNotEmpty() && library.goals.isNotEmpty()) {
                    item(key = "standalone") {
                        Text(
                            "Goals",
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.padding(top = 12.dp, bottom = 2.dp),
                        )
                    }
                }
                items(library.goals, key = { "g-${it.goal.id}" }) { goal ->
                    LibraryGoalRow(goal, state.today) { onOpenGoal(goal.goal.id) }
                }
            }
        }
    }
}

@Composable
fun LibraryGoalRow(item: GoalItem, today: LocalDate, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = GoalsTheme.rowHeight)
            .card(shape = MaterialTheme.shapes.small)
            .clickable(onClick = onClick)
            .padding(end = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AreaBar(item.area?.colorIndex, Modifier.height(GoalsTheme.rowHeight - 16.dp))
        Spacer(Modifier.width(10.dp))
        Icon(iconFor(item.goal.icon), null, Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f).padding(vertical = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    item.goal.title, style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false),
                )
                Spacer(Modifier.width(6.dp))
                CadenceBadge(item)
            }
            Spacer(Modifier.height(6.dp))
            ProgressBar(item.progress.fraction, Modifier.fillMaxWidth())
        }
        Spacer(Modifier.width(12.dp))
        Column(horizontalAlignment = Alignment.End) {
            Text("${(item.progress.fraction * 100).roundToInt()}%", style = NumericStyle)
            item.goal.endDate?.let {
                Text(
                    formatDate(LocalDate.ofEpochDay(it), today),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (item.progress.overdue) scheme.error else GoalsTheme.extra.muted,
                )
            }
        }
    }
}

@Composable
private fun ProjectRow(
    item: ProjectItem,
    today: LocalDate,
    expanded: Boolean,
    onOpen: () -> Unit,
    onToggle: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = GoalsTheme.rowHeight)
            .card(shape = MaterialTheme.shapes.small)
            .clickable(onClick = onOpen),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AreaBar(item.area?.colorIndex, Modifier.height(GoalsTheme.rowHeight - 16.dp))
        Spacer(Modifier.width(10.dp))
        Icon(Icons.Rounded.Folder, null, Modifier.size(20.dp), tint = scheme.secondary)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f).padding(vertical = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    item.project.title, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = MaterialTheme.typography.titleSmall.fontWeight),
                    maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f),
                )
                Text("${(item.progress * 100).roundToInt()}%", style = NumericStyle)
            }
            Spacer(Modifier.height(6.dp))
            ProgressBar(item.progress, Modifier.fillMaxWidth(), color = scheme.secondary)
            Spacer(Modifier.height(4.dp))
            Text(
                listOfNotNull(
                    item.goals.count { !it.goal.archived }.let { if (it == 1) "1 goal" else "$it goals" },
                    "${item.milestones.count { it.completedAt != null }}/${item.milestones.size} milestones".takeIf { item.milestones.isNotEmpty() },
                    item.project.dueDate?.let { "Due ${formatDate(LocalDate.ofEpochDay(it), today)}" },
                ).joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = if ((item.daysLeft ?: 0) < 0) scheme.error else GoalsTheme.extra.muted,
                maxLines = 1,
            )
        }
        Box(
            Modifier.size(48.dp).clip(CircleShape).clickable(onClick = onToggle),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Rounded.ExpandMore,
                if (expanded) "Collapse ${item.project.title}" else "Expand ${item.project.title}",
                Modifier.size(20.dp).rotate(if (expanded) 180f else 0f),
                tint = GoalsTheme.extra.muted,
            )
        }
    }
}
