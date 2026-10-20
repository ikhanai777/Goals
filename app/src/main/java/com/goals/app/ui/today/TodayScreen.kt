package com.goals.app.ui.today

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
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.goals.app.ui.components.card
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.goals.app.data.Cadence
import com.goals.app.data.MilestoneEntity
import com.goals.app.domain.Pace
import com.goals.app.domain.formatAmount
import com.goals.app.domain.formatDate
import com.goals.app.domain.formatDaysLeft
import com.goals.app.ui.AppState
import com.goals.app.ui.GoalItem
import com.goals.app.ui.MilestoneItem
import com.goals.app.ui.RowActions
import com.goals.app.ui.TodayFilter
import com.goals.app.ui.buildTodaySections
import com.goals.app.ui.components.EmptyState
import com.goals.app.ui.components.SegmentedControl
import com.goals.app.ui.components.ProgressRing
import com.goals.app.ui.components.SectionHeader
import com.goals.app.ui.components.SwipeGoalRow
import com.goals.app.ui.overallCompletion
import com.goals.app.ui.theme.GoalsTheme
import com.goals.app.ui.theme.NumericStyle
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

private val headerDate = DateTimeFormatter.ofPattern("EEE, MMM d", Locale.getDefault())

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodayScreen(
    state: AppState,
    filter: TodayFilter,
    onFilter: (TodayFilter) -> Unit,
    rows: RowActions,
    onToggleMilestone: (MilestoneEntity) -> Unit,
    onOpenProject: (String) -> Unit,
    onAdd: () -> Unit,
) {
    val sections = buildTodaySections(state, filter)
    val completion = overallCompletion(sections)
    var expandedDone by rememberSaveable { mutableStateOf(setOf<String>()) }
    var collapsed by rememberSaveable { mutableStateOf(setOf<String>()) }

    Scaffold(

        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onBackground,
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(state.today.format(headerDate), style = MaterialTheme.typography.titleLarge)
                    }
                },
                actions = {
                    ProgressRing(completion, size = 32.dp, stroke = 3.dp) {
                        Text(
                            "${(completion * 100).roundToInt()}",
                            style = NumericStyle.copy(fontSize = MaterialTheme.typography.labelSmall.fontSize),
                        )
                    }
                    Spacer(Modifier.width(16.dp))
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
        },
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            item(key = "filter") {
                SegmentedControl(TodayFilter.entries, filter, { it.label }, onFilter)
            }
            if (sections.isEmpty()) {
                item(key = "empty") {
                    EmptyState(
                        if (filter == TodayFilter.TODAY) "Nothing due today" else "No ${filter.label.lowercase()}ly goals",
                        "Add a goal",
                        onAdd,
                    )
                }
            }
            for (section in sections) {
                val isCollapsed = section.key in collapsed
                item(key = "h-${section.key}") {
                    SectionHeader(
                        section.title,
                        if (section.total > 0) "${section.done.size}/${section.total}" else "${section.milestones.size}",
                        color = if (section.alert) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                        expanded = !isCollapsed,
                        onToggle = { collapsed = if (isCollapsed) collapsed - section.key else collapsed + section.key },
                    )
                }
                if (isCollapsed) continue
                items(section.open, key = { "${section.key}-${it.goal.id}" }) { item ->
                    TodayRow(state, item, rows)
                }
                items(section.milestones, key = { "${section.key}-m-${it.milestone.id}" }) { m ->
                    MilestoneRow(m, state.today, onToggle = { onToggleMilestone(m.milestone) }, onOpen = { onOpenProject(m.project.id) })
                }
                if (section.done.isNotEmpty()) {
                    val showDone = section.key in expandedDone
                    item(key = "d-${section.key}") {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clip(MaterialTheme.shapes.small)
                                .clickable {
                                    expandedDone = if (showDone) expandedDone - section.key else expandedDone + section.key
                                }
                                .padding(vertical = 6.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Rounded.Check, null, Modifier.size(14.dp), tint = GoalsTheme.extra.success)
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "Done (${section.done.size})" + if (showDone) "" else " · show",
                                style = MaterialTheme.typography.labelMedium,
                                color = GoalsTheme.extra.muted,
                            )
                        }
                    }
                    if (showDone) {
                        items(section.done, key = { "${section.key}-done-${it.goal.id}" }) { item ->
                            TodayRow(state, item, rows)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TodayRow(state: AppState, item: GoalItem, rows: RowActions) {
    SwipeGoalRow(
        item = item,
        showStreak = state.settings.showStreaks,
        onOpen = { rows.onOpen(item) },
        onLog = { rows.onLog(item) },
        onQuickLog = { rows.onQuickLog(item) },
        onSkip = { rows.onSkip(item) },
        meta = rowMeta(item),
    )
}

/** Secondary line: pace for long periods, deadline for one-off goals. */
fun rowMeta(item: GoalItem): String? {
    val p = item.progress
    return when {
        item.goal.cadence == Cadence.ONCE && item.goal.endDate != null -> formatDaysLeft(p.daysLeft)
        p.pace == Pace.BEHIND -> "${item.goal.formatAmount(p.behindBy)} behind pace · ${formatDaysLeft(p.daysLeft).replace("Due today", "ends today")}"
        else -> null
    }
}

@Composable
private fun MilestoneRow(item: MilestoneItem, today: LocalDate, onToggle: () -> Unit, onOpen: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val due = item.milestone.dueDate?.let(LocalDate::ofEpochDay)
    val overdue = due != null && due.isBefore(today)
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = GoalsTheme.rowHeight)
            .card(shape = MaterialTheme.shapes.small)
            .clickable(onClick = onOpen)
            .padding(start = 14.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.Flag, null, Modifier.size(20.dp), tint = scheme.secondary)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(item.milestone.title, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                listOfNotNull(item.project.title, due?.let { formatDate(it, today) }).joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = if (overdue) scheme.error else GoalsTheme.extra.muted,
                maxLines = 1,
            )
        }
        Box(
            Modifier.size(48.dp).clip(CircleShape).clickable(onClick = onToggle),
            contentAlignment = Alignment.Center,
        ) {
            Box(Modifier.size(22.dp).clip(CircleShape).border(1.5.dp, scheme.secondary, CircleShape))
        }
    }
}
