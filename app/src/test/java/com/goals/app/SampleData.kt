package com.goals.app

import com.goals.app.data.ALL_WEEKDAYS
import com.goals.app.data.AppSettings
import com.goals.app.data.AreaEntity
import com.goals.app.data.Cadence
import com.goals.app.data.EntryEntity
import com.goals.app.data.GoalEntity
import com.goals.app.data.GoalType
import com.goals.app.data.MilestoneEntity
import com.goals.app.data.ProjectEntity
import com.goals.app.data.ReminderEntity
import com.goals.app.ui.AppState
import com.goals.app.ui.buildAppState
import java.time.LocalDate
import kotlin.random.Random

/** A realistic, deterministic data set for screenshot and logic tests. Today is Tue, Oct 20 2026. */
object SampleData {
    val today: LocalDate = LocalDate.of(2026, 10, 20)

    private val areas = listOf(
        AreaEntity("health", "Health", "health", 0, 0),
        AreaEntity("work", "Work", "work", 1, 1),
        AreaEntity("learning", "Learning", "school", 2, 2),
        AreaEntity("finance", "Finance", "savings", 3, 3),
    )

    private fun goal(
        id: String, title: String, icon: String, type: GoalType, cadence: Cadence, target: Double,
        area: String?, unit: String? = null, increment: Double = 1.0, project: String? = null,
        start: LocalDate = today.minusDays(200), end: LocalDate? = null, weekdays: Int = ALL_WEEKDAYS,
    ) = GoalEntity(
        id, project, area, title, icon, null, type, cadence, target, unit, increment, weekdays,
        start.toEpochDay(), end?.toEpochDay(), false, 0, 0, 0,
    )

    val goals = listOf(
        goal("meditate", "Meditate 10 minutes", "meditate", GoalType.CHECK, Cadence.DAILY, 1.0, "health"),
        goal("water", "Drink 8 glasses of water", "water", GoalType.COUNT, Cadence.DAILY, 8.0, "health"),
        goal("read", "Read 30 minutes", "book", GoalType.DURATION, Cadence.DAILY, 30.0, "learning", increment = 10.0),
        goal("journal", "Journal", "write", GoalType.CHECK, Cadence.DAILY, 1.0, null),
        goal("spanish", "Spanish lesson", "language", GoalType.CHECK, Cadence.DAILY, 1.0, "learning", project = "spanish", weekdays = 0b0011111),
        goal("run", "Run 20 km", "run", GoalType.QUANTITY, Cadence.WEEKLY, 20.0, "health", unit = "km", increment = 5.0),
        goal("gym", "Gym sessions", "fitness", GoalType.CHECK, Cadence.WEEKLY, 3.0, "health"),
        goal("design", "Design sprint", "art", GoalType.DURATION, Cadence.WEEKLY, 300.0, "work", increment = 30.0, project = "portfolio"),
        goal("save", "Save $500", "savings", GoalType.QUANTITY, Cadence.MONTHLY, 500.0, "finance", unit = "$", increment = 50.0),
        goal("books", "Read 24 books", "book", GoalType.COUNT, Cadence.YEARLY, 24.0, "learning", start = LocalDate.of(2026, 1, 1)),
        goal(
            "cases", "Write 6 case studies", "write", GoalType.COUNT, Cadence.ONCE, 6.0, "work",
            project = "portfolio", start = LocalDate.of(2026, 10, 1), end = LocalDate.of(2026, 11, 10),
        ),
        goal(
            "taxes", "File quarterly taxes", "money", GoalType.CHECK, Cadence.ONCE, 1.0, "finance",
            start = LocalDate.of(2026, 10, 1), end = LocalDate.of(2026, 10, 15),
        ),
    )

    private val projects = listOf(
        ProjectEntity(
            "portfolio", "work", "Portfolio relaunch", "New site with case studies and a refreshed visual identity.",
            LocalDate.of(2026, 10, 1).toEpochDay(), LocalDate.of(2026, 11, 30).toEpochDay(), false, 0, 0,
        ),
        ProjectEntity(
            "spanish", "learning", "Spanish B1", null,
            LocalDate.of(2026, 6, 1).toEpochDay(), LocalDate.of(2027, 3, 1).toEpochDay(), false, 0, 0,
        ),
    )

    private val milestones = listOf(
        MilestoneEntity("m1", "portfolio", "Wireframes", LocalDate.of(2026, 10, 8).toEpochDay(), 1, 0),
        MilestoneEntity("m2", "portfolio", "Visual design", LocalDate.of(2026, 10, 24).toEpochDay(), null, 1),
        MilestoneEntity("m3", "portfolio", "Content", LocalDate.of(2026, 11, 12).toEpochDay(), null, 2),
        MilestoneEntity("m4", "portfolio", "Launch", LocalDate.of(2026, 11, 30).toEpochDay(), null, 3),
        MilestoneEntity("m5", "spanish", "A2 exam", LocalDate.of(2026, 9, 15).toEpochDay(), 1, 0),
        MilestoneEntity("m6", "spanish", "B1 mock test", LocalDate.of(2026, 10, 26).toEpochDay(), null, 1),
    )

    val entries: List<EntryEntity> = buildList {
        var n = 0
        fun add(goal: String, date: LocalDate, value: Double, skip: Boolean = false) {
            add(EntryEntity("e${n++}", goal, value, date.toEpochDay(), n.toLong(), null, skip))
        }
        val rnd = Random(7)
        // Meditate: patchy history, then a 12-day streak ending yesterday.
        for (d in 13..182) if (rnd.nextFloat() < 0.72f) add("meditate", today.minusDays(d.toLong()), 1.0)
        for (d in 1..12) add("meditate", today.minusDays(d.toLong()), 1.0)
        for (d in 1..40) add("water", today.minusDays(d.toLong()), (5 + rnd.nextInt(5)).toDouble())
        repeat(5) { add("water", today, 1.0) }
        for (d in 1..6) add("read", today.minusDays(d.toLong()), 30.0)
        add("read", today, 20.0)
        for (d in 0..20) add("journal", today.minusDays(d.toLong()), 1.0)
        add("spanish", today.minusDays(1), 1.0)
        // Run: twelve weeks of history, 12 km so far this week.
        for (w in 1..11) add("run", today.minusWeeks(w.toLong()), (14 + rnd.nextInt(10)).toDouble())
        add("run", today.minusDays(1), 7.0)
        add("run", today, 5.0)
        add("gym", today.minusDays(1), 1.0)
        for (w in 1..6) repeat(3) { add("gym", today.minusWeeks(w.toLong()), 1.0) }
        add("design", today.minusDays(1), 120.0)
        for (w in 1..3) add("design", today.minusWeeks(w.toLong()), (240 + rnd.nextInt(120)).toDouble())
        add("save", today.minusDays(15), 150.0)
        add("save", today.minusDays(5), 70.0)
        for (m in 1..9) add("save", today.minusMonths(m.toLong()), (380 + rnd.nextInt(200)).toDouble())
        for (m in 1..9) repeat(if (m % 3 == 0) 1 else 2) { add("books", LocalDate.of(2026, m, 10), 1.0) }
        add("books", today.minusDays(3), 1.0)
        add("cases", today.minusDays(9), 1.0)
        add("cases", today.minusDays(2), 1.0)
    }

    private val reminders = listOf(ReminderEntity("r1", "meditate", 7, 30, true))

    fun state(settings: AppSettings = AppSettings()): AppState =
        buildAppState(today, settings, areas, projects, goals, entries, milestones, reminders)
}
