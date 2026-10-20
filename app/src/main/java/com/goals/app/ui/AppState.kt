package com.goals.app.ui

import com.goals.app.data.AppSettings
import com.goals.app.data.AreaEntity
import com.goals.app.data.Cadence
import com.goals.app.data.EntryEntity
import com.goals.app.data.GoalEntity
import com.goals.app.data.MilestoneEntity
import com.goals.app.data.ProjectEntity
import com.goals.app.data.ReminderEntity
import com.goals.app.domain.GoalProgress
import com.goals.app.domain.computeProgress
import java.time.LocalDate
import java.time.temporal.ChronoUnit

data class GoalItem(
    val goal: GoalEntity,
    val progress: GoalProgress,
    val entries: List<EntryEntity>,
    val area: AreaEntity?,
    val reminder: ReminderEntity?,
)

data class ProjectItem(
    val project: ProjectEntity,
    val area: AreaEntity?,
    val goals: List<GoalItem>,
    val milestones: List<MilestoneEntity>,
    /** Roll-up: average of active goals, or milestones completed when there are no goals. */
    val progress: Float,
    val daysLeft: Long?,
)

/** Everything the screens render, derived from the database and settings in one place. */
data class AppState(
    val loaded: Boolean = false,
    val today: LocalDate = LocalDate.now(),
    val settings: AppSettings = AppSettings(),
    val areas: List<AreaEntity> = emptyList(),
    val goals: List<GoalItem> = emptyList(),
    val projects: List<ProjectItem> = emptyList(),
) {
    fun goal(id: String): GoalItem? = goals.firstOrNull { it.goal.id == id }
    fun project(id: String): ProjectItem? = projects.firstOrNull { it.project.id == id }
}

fun buildAppState(
    today: LocalDate,
    settings: AppSettings,
    areas: List<AreaEntity>,
    projects: List<ProjectEntity>,
    goals: List<GoalEntity>,
    entries: List<EntryEntity>,
    milestones: List<MilestoneEntity>,
    reminders: List<ReminderEntity>,
): AppState {
    val areaById = areas.associateBy { it.id }
    val entriesByGoal = entries.groupBy { it.goalId }
    val reminderByGoal = reminders.associateBy { it.goalId }
    val goalItems = goals.map { goal ->
        val goalEntries = entriesByGoal[goal.id].orEmpty()
        GoalItem(
            goal = goal,
            progress = computeProgress(goal, goalEntries, today, settings.weekStart),
            entries = goalEntries,
            area = goal.areaId?.let(areaById::get),
            reminder = reminderByGoal[goal.id],
        )
    }
    val goalsByProject = goalItems.groupBy { it.goal.projectId }
    val milestonesByProject = milestones.groupBy { it.projectId }
    val projectItems = projects.map { project ->
        val children = goalsByProject[project.id].orEmpty()
        val projectMilestones = milestonesByProject[project.id].orEmpty()
        val active = children.filter { !it.goal.archived }
        val progress = when {
            active.isNotEmpty() -> active.map { it.progress.fraction }.average().toFloat()
            projectMilestones.isNotEmpty() ->
                projectMilestones.count { it.completedAt != null }.toFloat() / projectMilestones.size
            else -> 0f
        }
        ProjectItem(
            project = project,
            area = project.areaId?.let(areaById::get),
            goals = children,
            milestones = projectMilestones,
            progress = progress,
            daysLeft = project.dueDate?.let { ChronoUnit.DAYS.between(today, LocalDate.ofEpochDay(it)) },
        )
    }
    return AppState(true, today, settings, areas, goalItems, projectItems)
}

// ---- Today ----

enum class TodayFilter(val label: String) { TODAY("Today"), WEEK("Week"), MONTH("Month"), YEAR("Year") }

data class MilestoneItem(val milestone: MilestoneEntity, val project: ProjectEntity)

data class TodaySection(
    val key: String,
    val title: String,
    val open: List<GoalItem>,
    val done: List<GoalItem>,
    val milestones: List<MilestoneItem> = emptyList(),
    val alert: Boolean = false,
) {
    val total: Int get() = open.size + done.size
}

private fun GoalItem.isLive(today: LocalDate): Boolean {
    val g = goal
    if (g.archived || LocalDate.ofEpochDay(g.startDate).isAfter(today)) return false
    if (g.cadence == Cadence.ONCE) {
        // Completed one-off goals drop off once the day they were finished has passed.
        if (progress.met) return entries.any { !it.isSkip && it.loggedFor >= today.toEpochDay() }
        return true
    }
    return g.endDate == null || !LocalDate.ofEpochDay(g.endDate).isBefore(today)
}

fun buildTodaySections(state: AppState, filter: TodayFilter): List<TodaySection> {
    val today = state.today
    val live = state.goals.filter { it.isLive(today) }

    fun section(key: String, title: String, items: List<GoalItem>, alert: Boolean = false) =
        TodaySection(key, title, items.filter { !it.progress.done }, items.filter { it.progress.done }, alert = alert)

    fun byCadence(c: Cadence) = live.filter { it.goal.cadence == c }

    val sections = when (filter) {
        TodayFilter.TODAY -> {
            val overdueGoals = live.filter { it.progress.overdue }
            val overdueMilestones = state.projects.filter { !it.project.archived }.flatMap { p ->
                p.milestones.filter { m ->
                    m.completedAt == null && m.dueDate != null && m.dueDate < today.toEpochDay()
                }.map { MilestoneItem(it, p.project) }
            }
            val upcoming = state.projects.filter { !it.project.archived }.flatMap { p ->
                p.milestones.filter { m ->
                    m.completedAt == null && m.dueDate != null &&
                        m.dueDate in today.toEpochDay()..today.plusDays(7).toEpochDay()
                }.map { MilestoneItem(it, p.project) }
            }.sortedBy { it.milestone.dueDate }
            val deadlines = byCadence(Cadence.ONCE).filter { !it.progress.overdue }
                .sortedBy { it.goal.endDate ?: Long.MAX_VALUE }
            listOf(
                TodaySection("overdue", "Overdue", overdueGoals, emptyList(), overdueMilestones, alert = true),
                section("daily", "Daily", byCadence(Cadence.DAILY).filter { it.progress.activeToday }),
                section("week", "This week", byCadence(Cadence.WEEKLY)),
                section("month", "This month", byCadence(Cadence.MONTHLY)),
                section("year", "This year", byCadence(Cadence.YEARLY)),
                section("once", "Deadlines", deadlines),
                TodaySection("milestones", "Upcoming milestones", emptyList(), emptyList(), upcoming),
            )
        }
        TodayFilter.WEEK -> listOf(section("week", "This week", byCadence(Cadence.WEEKLY)))
        TodayFilter.MONTH -> listOf(section("month", "This month", byCadence(Cadence.MONTHLY)))
        TodayFilter.YEAR -> listOf(section("year", "This year", byCadence(Cadence.YEARLY)))
    }
    return sections.filter { it.total > 0 || it.milestones.isNotEmpty() }
}

/** Share of goals shown on Today that are done for their current period. */
fun overallCompletion(sections: List<TodaySection>): Float {
    val total = sections.sumOf { it.total }
    return if (total == 0) 0f else sections.sumOf { it.done.size }.toFloat() / total
}

// ---- Goals library ----

enum class LibraryFilter(val label: String) {
    ALL("All"), PROJECTS("Projects"), DAILY("Daily"), WEEKLY("Weekly"),
    MONTHLY("Monthly"), YEARLY("Yearly"), ONCE("Once"), ARCHIVED("Archived"),
}

enum class LibrarySort(val label: String) {
    DUE("Due date"), PROGRESS("Progress"), UPDATED("Recently updated"), NAME("Name"),
}

data class Library(val projects: List<ProjectItem>, val goals: List<GoalItem>)

fun buildLibrary(state: AppState, filter: LibraryFilter, sort: LibrarySort, query: String): Library {
    val q = query.trim()
    fun GoalItem.matches() = q.isEmpty() || goal.title.contains(q, ignoreCase = true)
    fun ProjectItem.matches() = q.isEmpty() || project.title.contains(q, ignoreCase = true) ||
        goals.any { it.matches() }

    val goalComparator: Comparator<GoalItem> = when (sort) {
        LibrarySort.DUE -> compareBy { it.goal.endDate ?: Long.MAX_VALUE }
        LibrarySort.PROGRESS -> compareBy { it.progress.fraction }
        LibrarySort.UPDATED -> compareByDescending { it.goal.updatedAt }
        LibrarySort.NAME -> compareBy(String.CASE_INSENSITIVE_ORDER) { it.goal.title }
    }
    val projectComparator: Comparator<ProjectItem> = when (sort) {
        LibrarySort.DUE -> compareBy { it.project.dueDate ?: Long.MAX_VALUE }
        LibrarySort.PROGRESS -> compareBy { it.progress }
        LibrarySort.UPDATED -> compareByDescending { it.project.updatedAt }
        LibrarySort.NAME -> compareBy(String.CASE_INSENSITIVE_ORDER) { it.project.title }
    }
    fun List<ProjectItem>.arrange() = filter { it.matches() }.sortedWith(projectComparator)
        .map { p -> p.copy(goals = p.goals.filter { !it.goal.archived }.sortedWith(goalComparator)) }
    fun List<GoalItem>.arrange() = filter { it.matches() }.sortedWith(goalComparator)

    val activeGoals = state.goals.filter { !it.goal.archived }
    val activeProjects = state.projects.filter { !it.project.archived }
    return when (filter) {
        LibraryFilter.ALL -> Library(activeProjects.arrange(), activeGoals.filter { it.goal.projectId == null }.arrange())
        LibraryFilter.PROJECTS -> Library(activeProjects.arrange(), emptyList())
        LibraryFilter.ARCHIVED -> Library(
            state.projects.filter { it.project.archived }.arrange(),
            state.goals.filter { it.goal.archived }.arrange(),
        )
        else -> {
            val cadence = Cadence.valueOf(filter.name)
            Library(emptyList(), activeGoals.filter { it.goal.cadence == cadence }.arrange())
        }
    }
}
