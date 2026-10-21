package com.goals.app.widget

import android.content.Context
import com.goals.app.GoalsApp
import com.goals.app.data.AppSettings
import com.goals.app.data.Cadence
import com.goals.app.data.GoalType
import com.goals.app.data.ThemeStyle
import com.goals.app.domain.Pace
import com.goals.app.domain.formatProgress
import com.goals.app.domain.logicalDate
import com.goals.app.ui.AppState
import com.goals.app.ui.GoalItem
import com.goals.app.ui.TodayFilter
import com.goals.app.ui.buildAppState
import com.goals.app.ui.buildTodaySections
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/** One goal as a widget shows it: already formatted, so the widget UI does no domain work. */
data class WidgetGoal(
    val id: String,
    val title: String,
    val icon: String,
    val progressLabel: String,
    val fraction: Float,
    val done: Boolean,
    val isCheck: Boolean,
    val streak: Int,
    /** Fire & Ice accent: true for fire (overdue, behind, weekly-or-longer), false for ice. */
    val fire: Boolean,
    val cadence: Cadence,
)

data class TodayWidgetData(
    val dateLabel: String,
    val done: Int,
    val total: Int,
    /** Open goals first, then finished ones. */
    val goals: List<WidgetGoal>,
    val style: ThemeStyle,
    val showStreaks: Boolean,
)

private val headerDate = DateTimeFormatter.ofPattern("EEE, MMM d", Locale.getDefault())

fun GoalItem.toWidgetGoal(): WidgetGoal = WidgetGoal(
    id = goal.id,
    title = goal.title,
    icon = goal.icon,
    progressLabel = goal.formatProgress(progress.value),
    fraction = progress.fraction,
    done = progress.done,
    isCheck = goal.type == GoalType.CHECK,
    streak = progress.currentStreak,
    fire = progress.overdue || progress.pace == Pace.BEHIND || goal.cadence != Cadence.DAILY,
    cadence = goal.cadence,
)

fun todayWidgetData(state: AppState): TodayWidgetData {
    val sections = buildTodaySections(state, TodayFilter.TODAY)
    val open = sections.flatMap { it.open }.distinctBy { it.goal.id }
    val done = sections.flatMap { it.done }.distinctBy { it.goal.id }
    return TodayWidgetData(
        dateLabel = state.today.format(headerDate),
        done = done.size,
        total = open.size + done.size,
        goals = (open + done).map { it.toWidgetGoal() },
        style = state.settings.style,
        showStreaks = state.settings.showStreaks,
    )
}

/** Reads everything a widget needs straight from the database, outside any activity. */
suspend fun loadAppState(context: Context): AppState {
    val container = (context.applicationContext as GoalsApp).container
    val dao = container.database.dao()
    val settings: AppSettings = container.settings.settings.first()
    val today: LocalDate = logicalDate(LocalDateTime.now(), settings.dayStartHour)
    return buildAppState(
        today, settings, dao.allAreas(), dao.allProjects(), dao.allGoals(),
        dao.allEntries(), dao.allMilestones(), dao.allReminders(),
    )
}
