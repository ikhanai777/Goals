package com.goals.app.domain

import com.goals.app.SampleData
import com.goals.app.data.ALL_WEEKDAYS
import com.goals.app.data.Cadence
import com.goals.app.data.EntryEntity
import com.goals.app.data.GoalEntity
import com.goals.app.data.GoalType
import com.goals.app.data.backup.Backup
import com.goals.app.ui.TodayFilter
import com.goals.app.ui.buildTodaySections
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime

class ProgressTest {
    private val today = LocalDate.of(2026, 10, 20) // Tuesday

    private fun goal(
        cadence: Cadence,
        target: Double = 1.0,
        type: GoalType = GoalType.CHECK,
        start: LocalDate = today.minusDays(60),
        end: LocalDate? = null,
        weekdays: Int = ALL_WEEKDAYS,
    ) = GoalEntity(
        "g", null, null, "Goal", "flag", null, type, cadence, target, null, 1.0, weekdays,
        start.toEpochDay(), end?.toEpochDay(), false, 0, 0, 0,
    )

    private var n = 0
    private fun entry(date: LocalDate, value: Double = 1.0, skip: Boolean = false) =
        EntryEntity("e${n++}", "g", value, date.toEpochDay(), 0, null, skip)

    private fun progress(goal: GoalEntity, entries: List<EntryEntity>, weekStart: DayOfWeek = DayOfWeek.MONDAY) =
        computeProgress(goal, entries, today, weekStart)

    @Test
    fun periodsRespectWeekStart() {
        assertEquals(Period(LocalDate.of(2026, 10, 19), LocalDate.of(2026, 10, 25)),
            periodFor(Cadence.WEEKLY, today, DayOfWeek.MONDAY, today, null))
        assertEquals(Period(LocalDate.of(2026, 10, 18), LocalDate.of(2026, 10, 24)),
            periodFor(Cadence.WEEKLY, today, DayOfWeek.SUNDAY, today, null))
        assertEquals(Period(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31)),
            periodFor(Cadence.MONTHLY, today, DayOfWeek.MONDAY, today, null))
        assertEquals(31, periodFor(Cadence.YEARLY, today, DayOfWeek.MONDAY, today, null).end.dayOfMonth)
    }

    @Test
    fun lateDayStartCountsTowardPreviousDay() {
        assertEquals(today.minusDays(1), logicalDate(today.atTime(3, 30), dayStartHour = 4))
        assertEquals(today, logicalDate(today.atTime(4, 0), dayStartHour = 4))
        assertEquals(today, logicalDate(LocalDateTime.of(today, java.time.LocalTime.MIDNIGHT), 0))
    }

    @Test
    fun dailyStreakCountsBackFromYesterdayAndIncludesTodayOnlyWhenDone() {
        val g = goal(Cadence.DAILY)
        val history = (1..5).map { entry(today.minusDays(it.toLong())) } + entry(today.minusDays(7))
        assertEquals(5, progress(g, history).currentStreak)
        assertEquals(6, progress(g, history + entry(today)).currentStreak)
        assertEquals(6, progress(g, history + entry(today)).bestStreak)
    }

    @Test
    fun missedDayBreaksStreakButSkipKeepsIt() {
        val g = goal(Cadence.DAILY)
        val withGap = listOf(entry(today.minusDays(1)), entry(today.minusDays(3)))
        assertEquals(1, progress(g, withGap).currentStreak)
        val withSkip = withGap + entry(today.minusDays(2), 0.0, skip = true)
        assertEquals(3, progress(g, withSkip).currentStreak)
    }

    @Test
    fun inactiveWeekdaysDoNotBreakDailyStreak() {
        // Weekdays only; the weekend of Oct 17–18 must not count as missed.
        val weekdays = 0b0011111
        val g = goal(Cadence.DAILY, weekdays = weekdays)
        val entries = listOf(16L, 15L, 14L, 19L).map { entry(LocalDate.of(2026, 10, it.toInt())) }
        assertEquals(4, progress(g, entries).currentStreak)
        assertFalse(computeProgress(g, emptyList(), LocalDate.of(2026, 10, 18), DayOfWeek.MONDAY).activeToday)
    }

    @Test
    fun weeklyCountTargetAndPace() {
        val g = goal(Cadence.WEEKLY, target = 3.0)
        val p = progress(g, listOf(entry(today.minusDays(1))))
        assertEquals(1.0, p.value, 0.0)
        assertFalse(p.met)
        // Day 2 of 7: expected 0.86, so one session is on track.
        assertEquals(Pace.ON_TRACK, p.pace)
        val late = computeProgress(g, listOf(entry(today.minusDays(1))), LocalDate.of(2026, 10, 24), DayOfWeek.MONDAY)
        assertEquals(Pace.BEHIND, late.pace)
    }

    @Test
    fun weeklyStreakAcrossPeriods() {
        val g = goal(Cadence.WEEKLY, target = 2.0, start = today.minusWeeks(3))
        val entries = (1..3).flatMap { w -> List(2) { entry(today.minusWeeks(w.toLong())) } }
        val p = progress(g, entries)
        assertEquals(3, p.currentStreak)
        assertEquals(1f, p.completionRate)
    }

    @Test
    fun monthlyAmountTracksBehindPace() {
        val g = goal(Cadence.MONTHLY, target = 500.0, type = GoalType.QUANTITY)
        val p = progress(g, listOf(entry(today.minusDays(3), 150.0)))
        assertEquals(Pace.BEHIND, p.pace)
        assertEquals(500.0 * 20 / 31 - 150.0, p.behindBy, 0.01)
        assertEquals(11, p.daysLeft)
    }

    @Test
    fun onceGoalOverdueAfterDeadline() {
        val g = goal(Cadence.ONCE, target = 6.0, type = GoalType.COUNT, end = today.minusDays(1))
        val p = progress(g, listOf(entry(today.minusDays(5), 2.0)))
        assertTrue(p.overdue)
        assertEquals(-1, p.daysLeft)
        assertEquals(0, p.currentStreak)
        assertNull(progress(goal(Cadence.ONCE, end = today.plusDays(3)), emptyList()).completionRate)
    }

    @Test
    fun newGoalHasNoCompletionRate() {
        val p = progress(goal(Cadence.DAILY, start = today), emptyList())
        assertNull(p.completionRate)
        assertEquals(0, p.currentStreak)
    }

    @Test
    fun todaySectionsGroupByCadenceAndSplitDone() {
        val sections = buildTodaySections(SampleData.state(), TodayFilter.TODAY)
        val keys = sections.map { it.key }
        assertEquals(listOf("overdue", "daily", "week", "month", "year", "once", "milestones"), keys)
        val daily = sections.first { it.key == "daily" }
        assertTrue(daily.done.any { it.goal.id == "journal" })
        assertTrue(daily.open.any { it.goal.id == "meditate" })
        assertTrue(sections.first { it.key == "overdue" }.open.any { it.goal.id == "taxes" })
    }

    @Test
    fun backupRoundTrips() {
        val backup = Backup(1, 0, emptyList(), emptyList(), SampleData.goals, SampleData.entries, emptyList(), emptyList())
        assertEquals(backup, Backup.fromJson(backup.toJson()))
    }
}
