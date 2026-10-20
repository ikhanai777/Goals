package com.goals.app.ui.detail

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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.goals.app.data.Cadence
import com.goals.app.data.EntryEntity
import com.goals.app.data.GoalType
import com.goals.app.domain.Pace
import com.goals.app.domain.dailyTotals
import com.goals.app.domain.cadenceDescription
import com.goals.app.domain.formatAmount
import com.goals.app.domain.formatDate
import com.goals.app.domain.formatDaysLeft
import com.goals.app.domain.formatProgress
import com.goals.app.domain.periodHistory
import com.goals.app.ui.AppState
import com.goals.app.ui.GoalItem
import com.goals.app.ui.components.LogControl
import com.goals.app.ui.components.ProgressRing
import com.goals.app.ui.components.StatTile
import com.goals.app.ui.components.iconFor
import com.goals.app.ui.theme.AreaColors
import com.goals.app.ui.theme.GoalsTheme
import com.goals.app.ui.theme.NumericStyle
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.IsoFields
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalDetailScreen(
    state: AppState,
    item: GoalItem,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onLog: () -> Unit,
    onAddPastEntry: () -> Unit,
    onDeleteEntry: (EntryEntity) -> Unit,
    onDuplicate: () -> Unit,
    onArchive: (Boolean) -> Unit,
    onDelete: () -> Unit,
    onOpenProject: (String) -> Unit,
) {
    val goal = item.goal
    val p = item.progress
    val today = state.today
    var menu by remember { mutableStateOf(false) }
    var notesOpen by remember { mutableStateOf(false) }
    val project = goal.projectId?.let(state::project)

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back") } },
                title = {},
                actions = {
                    IconButton(onClick = onEdit) { Icon(Icons.Rounded.Edit, "Edit") }
                    Box {
                        IconButton(onClick = { menu = true }) { Icon(Icons.Rounded.MoreVert, "More") }
                        DropdownMenu(menu, onDismissRequest = { menu = false }) {
                            DropdownMenuItem(text = { Text("Duplicate") }, onClick = { menu = false; onDuplicate() })
                            DropdownMenuItem(
                                text = { Text(if (goal.archived) "Unarchive" else "Archive") },
                                onClick = { menu = false; onArchive(!goal.archived) },
                            )
                            DropdownMenuItem(
                                text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                                onClick = { menu = false; onDelete() },
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 32.dp),
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(iconFor(goal.icon), null, Modifier.size(24.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(10.dp))
                    Text(goal.title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                }
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    item.area?.let { Chip(it.name, AreaColors[it.colorIndex % AreaColors.size]) }
                    project?.let { Chip(it.project.title, MaterialTheme.colorScheme.secondary) { onOpenProject(it.project.id) } }
                    if (goal.archived) Chip("Archived", GoalsTheme.extra.muted)
                }
                Spacer(Modifier.height(4.dp))
                Text(goal.cadenceDescription(), style = MaterialTheme.typography.bodySmall, color = GoalsTheme.extra.muted)
                item.reminder?.takeIf { it.enabled }?.let {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp)) {
                        Icon(Icons.Rounded.Notifications, null, Modifier.size(12.dp), tint = GoalsTheme.extra.muted)
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "%02d:%02d".format(it.hour, it.minute),
                            style = MaterialTheme.typography.bodySmall, color = GoalsTheme.extra.muted,
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            item { CurrentPeriodCard(item, today, onLog) }

            item {
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    val tile = Modifier.weight(1f)
                    if (goal.cadence == Cadence.ONCE) {
                        StatTile("Progress", "${(p.fraction * 100).roundToInt()}%", tile)
                        StatTile("Logged", goal.formatAmount(p.totalLogged), tile)
                        StatTile("Entries", "${item.entries.count { !it.isSkip }}", tile)
                    } else {
                        StatTile("Streak", "${p.currentStreak}", tile)
                        StatTile("Best", "${p.bestStreak}", tile)
                        StatTile("Rate", p.completionRate?.let { "${(it * 100).roundToInt()}%" } ?: "—", tile)
                        StatTile("Total", goal.formatAmount(p.totalLogged), tile)
                    }
                }
            }

            if (goal.cadence != Cadence.ONCE) {
                item {
                    Spacer(Modifier.height(20.dp))
                    Text("History", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(8.dp))
                    History(state, item)
                }
            }

            item {
                Spacer(Modifier.height(20.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Entries", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                    TextButton(onClick = onAddPastEntry) { Text("Add past entry") }
                }
            }
            val recent = item.entries.sortedWith(compareByDescending<EntryEntity> { it.loggedFor }.thenByDescending { it.createdAt }).take(50)
            if (recent.isEmpty()) {
                item {
                    Text("No entries yet", style = MaterialTheme.typography.bodySmall, color = GoalsTheme.extra.muted)
                }
            }
            items(recent, key = { it.id }) { entry ->
                EntryRow(item, entry, today) { onDeleteEntry(entry) }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }

            if (!goal.notes.isNullOrBlank()) {
                item {
                    Spacer(Modifier.height(20.dp))
                    Row(
                        Modifier.fillMaxWidth().clickable { notesOpen = !notesOpen }.padding(vertical = 6.dp),
                    ) {
                        Text("Why this matters", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                        Text(if (notesOpen) "Hide" else "Show", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    }
                    if (notesOpen) Text(goal.notes, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
private fun Chip(text: String, color: androidx.compose.ui.graphics.Color, onClick: (() -> Unit)? = null) {
    Row(
        Modifier
            .clip(MaterialTheme.shapes.extraSmall)
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(6.dp).clip(MaterialTheme.shapes.extraSmall).background(color))
        Spacer(Modifier.width(6.dp))
        Text(text, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun CurrentPeriodCard(item: GoalItem, today: LocalDate, onLog: () -> Unit) {
    val goal = item.goal
    val p = item.progress
    val scheme = MaterialTheme.colorScheme
    val periodLabel = when (goal.cadence) {
        Cadence.DAILY -> "Today"
        Cadence.WEEKLY -> "This week"
        Cadence.MONTHLY -> today.month.getDisplayName(TextStyle.FULL, Locale.getDefault())
        Cadence.YEARLY -> "${today.year}"
        Cadence.ONCE -> goal.endDate?.let { "Due ${formatDate(LocalDate.ofEpochDay(it), today)}" } ?: "Overall"
    }
    val (paceText, paceColor) = when {
        p.overdue -> formatDaysLeft(p.daysLeft) to scheme.error
        p.skipped -> "Skipped" to scheme.secondary
        p.met -> "Target met" to GoalsTheme.extra.success
        !p.activeToday -> "Rest day" to GoalsTheme.extra.muted
        p.pace == Pace.BEHIND -> "${goal.formatAmount(p.behindBy)} behind pace" to GoalsTheme.extra.warning
        p.pace == Pace.ON_TRACK -> "On track" to GoalsTheme.extra.success
        else -> (if (goal.cadence == Cadence.DAILY) "Not done yet" else formatDaysLeft(p.daysLeft)) to GoalsTheme.extra.muted
    }
    Row(
        Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(scheme.surfaceContainerLow)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ProgressRing(p.fraction, size = 88.dp, stroke = 8.dp, color = if (p.met) GoalsTheme.extra.success else scheme.primary) {
            Text("${(p.fraction * 100).roundToInt()}%", style = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = "tnum"))
        }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(periodLabel, style = MaterialTheme.typography.labelMedium, color = GoalsTheme.extra.muted)
            goal.formatProgress(p.value).takeIf { it.isNotEmpty() }?.let {
                Text(it, style = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = "tnum"))
            }
            Text(paceText, style = MaterialTheme.typography.bodySmall, color = paceColor)
            Spacer(Modifier.height(8.dp))
            if (goal.type == GoalType.CHECK) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    LogControl(item, onLog, Modifier.size(40.dp))
                    Text(if (p.met) "Done" else "Mark done", style = MaterialTheme.typography.labelLarge)
                }
            } else {
                Button(
                    onClick = onLog,
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                    modifier = Modifier.heightIn(min = 36.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = scheme.primary),
                ) { Text("Log progress", style = MaterialTheme.typography.labelLarge) }
            }
        }
    }
}

private val monthShort = DateTimeFormatter.ofPattern("MMM", Locale.getDefault())

@Composable
private fun History(state: AppState, item: GoalItem) {
    val goal = item.goal
    val settings = state.settings
    when (goal.cadence) {
        Cadence.DAILY -> {
            val totals = dailyTotals(item.entries)
            val rate = item.progress.completionRate?.let { "${(it * 100).roundToInt()} percent" } ?: "no data yet"
            Heatmap(
                totals, goal.target, state.today, settings.weekStart,
                description = "Last 26 weeks of ${goal.title}, completion rate $rate",
            )
        }
        Cadence.YEARLY -> {
            val months = (1..12).map { m ->
                val start = LocalDate.of(state.today.year, m, 1)
                val end = start.withDayOfMonth(start.lengthOfMonth())
                item.entries.filter { !it.isSkip && it.loggedFor in start.toEpochDay()..end.toEpochDay() }.sumOf { it.value }
            }
            BarChart(
                values = months,
                labels = (1..12).map { LocalDate.of(2000, it, 1).format(monthShort).take(1) },
                target = goal.target / 12,
                description = "Monthly totals for ${state.today.year}, target ${goal.formatAmount(goal.target / 12)} per month",
            )
        }
        else -> {
            val history = periodHistory(goal, item.entries, state.today, settings.weekStart, 12)
            val met = history.count { it.second >= goal.target }
            BarChart(
                values = history.map { it.second },
                labels = history.mapIndexed { i, (period, _) ->
                    if (i % 3 != 2) "" else when (goal.cadence) {
                        Cadence.WEEKLY -> "W${period.start.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR)}"
                        else -> period.start.format(monthShort)
                    }
                },
                target = goal.target,
                description = "Last 12 periods, target met in $met of them",
            )
        }
    }
}

@Composable
private fun EntryRow(item: GoalItem, entry: EntryEntity, today: LocalDate, onDelete: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 44.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            formatDate(LocalDate.ofEpochDay(entry.loggedFor), today),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.width(96.dp),
        )
        Text(
            when {
                entry.isSkip -> "Skipped"
                item.goal.type == GoalType.CHECK -> "Done"
                else -> "+" + item.goal.formatAmount(entry.value)
            },
            style = NumericStyle,
            color = if (entry.isSkip) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.width(8.dp))
        Text(
            entry.note.orEmpty(), style = MaterialTheme.typography.bodySmall, color = GoalsTheme.extra.muted,
            maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onDelete) {
            Icon(Icons.Rounded.Close, "Delete entry", Modifier.size(18.dp), tint = GoalsTheme.extra.muted)
        }
    }
}
