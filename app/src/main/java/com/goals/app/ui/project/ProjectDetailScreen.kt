package com.goals.app.ui.project

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.goals.app.data.GoalsRepository
import com.goals.app.data.MilestoneEntity
import com.goals.app.domain.formatDate
import com.goals.app.domain.formatDaysLeft
import com.goals.app.ui.AppState
import com.goals.app.ui.ProjectItem
import com.goals.app.ui.RowActions
import com.goals.app.ui.components.DatePickerModal
import com.goals.app.ui.components.EmptyState
import com.goals.app.ui.components.ProgressBar
import com.goals.app.ui.components.SwipeGoalRow
import com.goals.app.ui.theme.GoalsTheme
import com.goals.app.ui.theme.NumericStyle
import com.goals.app.ui.today.rowMeta
import java.time.LocalDate
import kotlin.math.roundToInt

private val Tabs = listOf("Overview", "Goals", "Milestones", "Notes")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectDetailScreen(
    state: AppState,
    item: ProjectItem,
    rows: RowActions,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onAddGoal: () -> Unit,
    onArchive: (Boolean) -> Unit,
    onDelete: () -> Unit,
    onSaveMilestone: (MilestoneEntity) -> Unit,
    onToggleMilestone: (MilestoneEntity) -> Unit,
    onDeleteMilestone: (MilestoneEntity) -> Unit,
    initialTab: Int = 0,
) {
    val project = item.project
    val today = state.today
    var tab by rememberSaveable { mutableStateOf(initialTab) }
    var menu by remember { mutableStateOf(false) }

    Scaffold(

        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onBackground,
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back") } },
                title = {},
                actions = {
                    IconButton(onClick = onEdit) { Icon(Icons.Rounded.Edit, "Edit project") }
                    Box {
                        IconButton(onClick = { menu = true }) { Icon(Icons.Rounded.MoreVert, "More") }
                        DropdownMenu(menu, onDismissRequest = { menu = false }) {
                            DropdownMenuItem(
                                text = { Text(if (project.archived) "Unarchive" else "Archive") },
                                onClick = { menu = false; onArchive(!project.archived) },
                            )
                            DropdownMenuItem(
                                text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                                onClick = { menu = false; onDelete() },
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Column(Modifier.padding(horizontal = 16.dp)) {
                Text(project.title, style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(2.dp))
                Text(
                    listOfNotNull(
                        formatDate(LocalDate.ofEpochDay(project.startDate), today) + (project.dueDate?.let { " – " + formatDate(LocalDate.ofEpochDay(it), today) } ?: ""),
                        item.daysLeft?.let { formatDaysLeft(it) },
                    ).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = if ((item.daysLeft ?: 0) < 0) MaterialTheme.colorScheme.error else GoalsTheme.extra.muted,
                )
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ProgressBar(item.progress, Modifier.weight(1f), height = 8.dp, color = MaterialTheme.colorScheme.secondary)
                    Spacer(Modifier.width(10.dp))
                    Text("${(item.progress * 100).roundToInt()}%", style = NumericStyle)
                }
                Spacer(Modifier.height(8.dp))
            }
            PrimaryTabRow(selectedTabIndex = tab, containerColor = Color.Transparent) {
                Tabs.forEachIndexed { i, title ->
                    Tab(
                        selected = tab == i,
                        onClick = { tab = i },
                        modifier = Modifier.height(40.dp),
                        text = { Text(title, style = MaterialTheme.typography.labelMedium) },
                    )
                }
            }
            when (tab) {
                0 -> Overview(state, item, rows)
                1 -> GoalsTab(state, item, rows, onAddGoal)
                2 -> MilestonesTab(item, today, onSaveMilestone, onToggleMilestone, onDeleteMilestone)
                else -> Text(
                    project.notes?.takeIf { it.isNotBlank() } ?: "No notes. Tap the edit button to add some.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (project.notes.isNullOrBlank()) GoalsTheme.extra.muted else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(16.dp),
                )
            }
        }
    }
}

@Composable
private fun Overview(state: AppState, item: ProjectItem, rows: RowActions) {
    val next = item.goals.filter { !it.goal.archived && !it.progress.done }.take(3)
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (item.milestones.isNotEmpty()) {
            item {
                Text("Milestones", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(10.dp))
                MilestoneTimeline(item.milestones, state.today)
                Spacer(Modifier.height(12.dp))
            }
        }
        item { Text("Next up", style = MaterialTheme.typography.titleSmall) }
        if (next.isEmpty()) {
            item { Text("All caught up", style = MaterialTheme.typography.bodySmall, color = GoalsTheme.extra.muted) }
        }
        items(next, key = { it.goal.id }) { goal ->
            SwipeGoalRow(
                goal, state.settings.showStreaks,
                onOpen = { rows.onOpen(goal) }, onLog = { rows.onLog(goal) },
                onQuickLog = { rows.onQuickLog(goal) }, onSkip = { rows.onSkip(goal) },
                meta = rowMeta(goal),
            )
        }
    }
}

/** Dots on a line in due-date order: filled cobalt when done, slate-blue outline when upcoming. */
@Composable
private fun MilestoneTimeline(milestones: List<MilestoneEntity>, today: LocalDate) {
    val sorted = milestones.sortedBy { it.dueDate ?: Long.MAX_VALUE }.take(6)
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val track = GoalsTheme.extra.track
    val background = MaterialTheme.colorScheme.background
    Column {
        Canvas(Modifier.fillMaxWidth().height(16.dp)) {
            val y = size.height / 2
            val step = if (sorted.size <= 1) 0f else size.width / (sorted.size - 1)
            drawLine(track, Offset(0f, y), Offset(size.width, y), 2.dp.toPx())
            sorted.forEachIndexed { i, m ->
                val x = if (sorted.size == 1) size.width / 2 else (i * step).coerceIn(6.dp.toPx(), size.width - 6.dp.toPx())
                if (m.completedAt != null) {
                    drawCircle(primary, 6.dp.toPx(), Offset(x, y))
                } else {
                    drawCircle(background, 6.dp.toPx(), Offset(x, y))
                    drawCircle(secondary, 5.dp.toPx(), Offset(x, y), style = Stroke(2.dp.toPx()))
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        Row(Modifier.fillMaxWidth()) {
            sorted.forEach { m ->
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(m.title, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    m.dueDate?.let {
                        Text(
                            formatDate(LocalDate.ofEpochDay(it), today),
                            style = MaterialTheme.typography.labelSmall,
                            color = GoalsTheme.extra.muted,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GoalsTab(state: AppState, item: ProjectItem, rows: RowActions, onAddGoal: () -> Unit) {
    val goals = item.goals.filter { !it.goal.archived }
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (goals.isEmpty()) {
            item { EmptyState("No goals in this project", "Add a goal", onAddGoal) }
        }
        items(goals, key = { it.goal.id }) { goal ->
            SwipeGoalRow(
                goal, state.settings.showStreaks,
                onOpen = { rows.onOpen(goal) }, onLog = { rows.onLog(goal) },
                onQuickLog = { rows.onQuickLog(goal) }, onSkip = { rows.onSkip(goal) },
                meta = rowMeta(goal),
            )
        }
        if (goals.isNotEmpty()) {
            item {
                TextButton(onClick = onAddGoal) {
                    Icon(Icons.Rounded.Add, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Add goal")
                }
            }
        }
    }
}

@Composable
private fun MilestonesTab(
    item: ProjectItem,
    today: LocalDate,
    onSave: (MilestoneEntity) -> Unit,
    onToggle: (MilestoneEntity) -> Unit,
    onDelete: (MilestoneEntity) -> Unit,
) {
    var title by rememberSaveable { mutableStateOf("") }
    var due by remember { mutableStateOf<LocalDate?>(null) }
    var picking by remember { mutableStateOf(false) }
    val scheme = MaterialTheme.colorScheme
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        items(item.milestones, key = { it.id }) { m ->
            val done = m.completedAt != null
            val overdue = !done && m.dueDate != null && m.dueDate < today.toEpochDay()
            Row(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = GoalsTheme.rowHeight)
                    .card(shape = MaterialTheme.shapes.small),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier.size(48.dp).clip(CircleShape).clickable { onToggle(m) },
                    contentAlignment = Alignment.Center,
                ) {
                    if (done) {
                        Box(Modifier.size(22.dp).clip(CircleShape).background(scheme.primary), contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.Check, "Mark not done", Modifier.size(14.dp), tint = scheme.onPrimary)
                        }
                    } else {
                        Box(Modifier.size(22.dp).clip(CircleShape).border(1.5.dp, scheme.secondary, CircleShape))
                    }
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        m.title, style = MaterialTheme.typography.bodyMedium,
                        color = if (done) GoalsTheme.extra.muted else scheme.onSurface,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                    m.dueDate?.let {
                        Text(
                            formatDate(LocalDate.ofEpochDay(it), today),
                            style = MaterialTheme.typography.bodySmall,
                            color = if (overdue) scheme.error else GoalsTheme.extra.muted,
                        )
                    }
                }
                IconButton(onClick = { onDelete(m) }) {
                    Icon(Icons.Rounded.Close, "Delete milestone", Modifier.size(18.dp), tint = GoalsTheme.extra.muted)
                }
            }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    placeholder = { Text("New milestone") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = { picking = true }) {
                    Icon(
                        Icons.Rounded.CalendarToday, "Due date",
                        tint = if (due != null) scheme.primary else GoalsTheme.extra.muted,
                    )
                }
                IconButton(
                    enabled = title.isNotBlank(),
                    onClick = {
                        onSave(
                            MilestoneEntity(
                                GoalsRepository.newId(), item.project.id, title.trim(),
                                due?.toEpochDay(), null, item.milestones.size,
                            ),
                        )
                        title = ""
                        due = null
                    },
                ) { Icon(Icons.Rounded.Add, "Add milestone") }
            }
            due?.let {
                Text("Due ${formatDate(it, today)}", style = MaterialTheme.typography.bodySmall, color = GoalsTheme.extra.muted)
            }
        }
    }
    if (picking) DatePickerModal(due ?: today, onDismiss = { picking = false }) { due = it; picking = false }
}
