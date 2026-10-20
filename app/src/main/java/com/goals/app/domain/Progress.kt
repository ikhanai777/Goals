package com.goals.app.domain

import com.goals.app.data.Cadence
import com.goals.app.data.EntryEntity
import com.goals.app.data.GoalEntity
import com.goals.app.data.GoalType
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit

enum class Pace { NONE, ON_TRACK, BEHIND }

data class GoalProgress(
    val period: Period,
    /** Progress logged in the current period, skips excluded. */
    val value: Double,
    val target: Double,
    val met: Boolean,
    val skipped: Boolean,
    /** False for DAILY goals on a weekday they are not scheduled for. */
    val activeToday: Boolean,
    val pace: Pace,
    /** How far behind the expected pace, in the goal's unit. Zero unless [pace] is BEHIND. */
    val behindBy: Double,
    val currentStreak: Int,
    val bestStreak: Int,
    /** Share of finished periods whose target was met, or null before the first one ends. */
    val completionRate: Float?,
    val totalLogged: Double,
    /** A ONCE goal whose deadline has passed without being met. */
    val overdue: Boolean,
    /** Days until the deadline for ONCE goals, or until the period ends otherwise. */
    val daysLeft: Long,
) {
    val fraction: Float get() = if (target <= 0) 0f else (value / target).toFloat().coerceIn(0f, 1f)
    val done: Boolean get() = met || skipped
}

/** Longest history walked when computing streaks, to keep very old daily goals cheap. */
private const val MAX_PERIODS = 3660

fun computeProgress(
    goal: GoalEntity,
    entries: List<EntryEntity>,
    today: LocalDate,
    weekStart: DayOfWeek,
): GoalProgress {
    val start = LocalDate.ofEpochDay(goal.startDate)
    val end = goal.endDate?.let(LocalDate::ofEpochDay)
    val current = periodFor(goal.cadence, today, weekStart, start, end)

    val sums = HashMap<LocalDate, Double>()
    val skips = HashSet<LocalDate>()
    var total = 0.0
    for (e in entries) {
        val key = periodFor(goal.cadence, LocalDate.ofEpochDay(e.loggedFor), weekStart, start, end).start
        if (e.isSkip) {
            skips += key
        } else {
            sums[key] = (sums[key] ?: 0.0) + e.value
            total += e.value
        }
    }

    fun isMet(p: Period) = (sums[p.start] ?: 0.0) >= goal.target
    fun isKept(p: Period) = isMet(p) || p.start in skips

    val value = sums[current.start] ?: 0.0
    val met = value >= goal.target
    val skipped = current.start in skips
    val activeToday = goal.cadence != Cadence.DAILY || isActiveOn(goal.weekdays, today)

    var currentStreak = 0
    var bestStreak = 0
    var completionRate: Float? = null
    if (goal.cadence != Cadence.ONCE) {
        // Finished periods, oldest first, skipping days a DAILY goal is not scheduled for.
        val past = ArrayList<Period>()
        val last = minOf(today, end ?: today)
        var p = previousPeriod(goal.cadence, current)
        while (!p.end.isBefore(start) && past.size < MAX_PERIODS) {
            if (!p.start.isAfter(last) &&
                (goal.cadence != Cadence.DAILY || isActiveOn(goal.weekdays, p.start))
            ) {
                past += p
            }
            p = previousPeriod(goal.cadence, p)
        }
        past.reverse()

        var run = 0
        var kept = 0
        for (period in past) {
            if (isKept(period)) {
                run++; kept++
                bestStreak = maxOf(bestStreak, run)
            } else {
                run = 0
            }
        }
        // The current period only ever extends a streak; it cannot break one until it ends.
        val currentKept = activeToday && (met || skipped)
        currentStreak = run + if (currentKept) 1 else 0
        bestStreak = maxOf(bestStreak, currentStreak)
        val denominator = past.size + if (currentKept) 1 else 0
        if (denominator > 0) completionRate = (kept + if (currentKept) 1 else 0).toFloat() / denominator
    } else if (met) {
        completionRate = 1f
    }

    val pace: Pace
    var behindBy = 0.0
    val paced = goal.cadence != Cadence.DAILY && (goal.cadence != Cadence.ONCE || end != null)
    if (!paced || met || skipped) {
        pace = if (paced && met) Pace.ON_TRACK else Pace.NONE
    } else {
        val elapsed = (ChronoUnit.DAYS.between(current.start, today) + 1).coerceIn(1, current.days)
        val expected = goal.target * elapsed / current.days
        if (value < expected * 0.9) {
            pace = Pace.BEHIND
            behindBy = expected - value
        } else {
            pace = Pace.ON_TRACK
        }
    }

    val overdue = goal.cadence == Cadence.ONCE && end != null && today.isAfter(end) && !met
    val deadline = if (goal.cadence == Cadence.ONCE) end else current.end
    val daysLeft = deadline?.let { ChronoUnit.DAYS.between(today, it) } ?: Long.MAX_VALUE

    return GoalProgress(
        period = current,
        value = value,
        target = goal.target,
        met = met,
        skipped = skipped,
        activeToday = activeToday,
        pace = pace,
        behindBy = behindBy,
        currentStreak = currentStreak,
        bestStreak = bestStreak,
        completionRate = completionRate,
        totalLogged = total,
        overdue = overdue,
        daysLeft = daysLeft,
    )
}

/** Totals for the last [count] periods ending with the current one, oldest first. */
fun periodHistory(
    goal: GoalEntity,
    entries: List<EntryEntity>,
    today: LocalDate,
    weekStart: DayOfWeek,
    count: Int,
): List<Pair<Period, Double>> {
    val start = LocalDate.ofEpochDay(goal.startDate)
    val end = goal.endDate?.let(LocalDate::ofEpochDay)
    var p = periodFor(goal.cadence, today, weekStart, start, end)
    val periods = ArrayList<Period>(count)
    repeat(count) {
        periods += p
        p = previousPeriod(goal.cadence, p)
    }
    periods.reverse()
    return periods.map { period ->
        period to entries
            .filter { !it.isSkip && LocalDate.ofEpochDay(it.loggedFor) in period }
            .sumOf { it.value }
    }
}

/** Per-day totals keyed by date, for the daily heatmap. */
fun dailyTotals(entries: List<EntryEntity>): Map<LocalDate, Double> =
    entries.filter { !it.isSkip }
        .groupBy { LocalDate.ofEpochDay(it.loggedFor) }
        .mapValues { (_, list) -> list.sumOf { it.value } }

/** The amount one tap of the log control adds. */
fun GoalEntity.tapIncrement(): Double = when (type) {
    GoalType.CHECK -> 1.0
    else -> defaultIncrement
}
