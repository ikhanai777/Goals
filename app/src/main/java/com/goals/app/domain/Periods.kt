package com.goals.app.domain

import com.goals.app.data.ALL_WEEKDAYS
import com.goals.app.data.Cadence
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

/** An inclusive date range that one tracker period covers. */
data class Period(val start: LocalDate, val end: LocalDate) {
    val days: Long get() = ChronoUnit.DAYS.between(start, end) + 1
    operator fun contains(date: LocalDate) = !date.isBefore(start) && !date.isAfter(end)
}

/** Far-future end used for ONCE goals without a deadline. */
val OPEN_END: LocalDate = LocalDate.of(9999, 12, 31)

/** The calendar day progress logged at [now] counts toward, honouring a late "day starts at" hour. */
fun logicalDate(now: LocalDateTime, dayStartHour: Int): LocalDate =
    if (now.hour < dayStartHour) now.toLocalDate().minusDays(1) else now.toLocalDate()

fun periodFor(
    cadence: Cadence,
    date: LocalDate,
    weekStart: DayOfWeek,
    goalStart: LocalDate,
    goalEnd: LocalDate?,
): Period = when (cadence) {
    Cadence.DAILY -> Period(date, date)
    Cadence.WEEKLY -> {
        val start = date.with(TemporalAdjusters.previousOrSame(weekStart))
        Period(start, start.plusDays(6))
    }
    Cadence.MONTHLY -> Period(date.withDayOfMonth(1), date.with(TemporalAdjusters.lastDayOfMonth()))
    Cadence.YEARLY -> Period(date.withDayOfYear(1), date.with(TemporalAdjusters.lastDayOfYear()))
    Cadence.ONCE -> Period(goalStart, goalEnd ?: OPEN_END)
}

fun nextPeriod(cadence: Cadence, period: Period): Period = when (cadence) {
    Cadence.DAILY -> Period(period.start.plusDays(1), period.end.plusDays(1))
    Cadence.WEEKLY -> Period(period.start.plusWeeks(1), period.end.plusWeeks(1))
    Cadence.MONTHLY -> {
        val start = period.start.plusMonths(1)
        Period(start, start.with(TemporalAdjusters.lastDayOfMonth()))
    }
    Cadence.YEARLY -> {
        val start = period.start.plusYears(1)
        Period(start, start.with(TemporalAdjusters.lastDayOfYear()))
    }
    Cadence.ONCE -> period
}

fun previousPeriod(cadence: Cadence, period: Period): Period = when (cadence) {
    Cadence.DAILY -> Period(period.start.minusDays(1), period.end.minusDays(1))
    Cadence.WEEKLY -> Period(period.start.minusWeeks(1), period.end.minusWeeks(1))
    Cadence.MONTHLY -> {
        val start = period.start.minusMonths(1)
        Period(start, start.with(TemporalAdjusters.lastDayOfMonth()))
    }
    Cadence.YEARLY -> {
        val start = period.start.minusYears(1)
        Period(start, start.with(TemporalAdjusters.lastDayOfYear()))
    }
    Cadence.ONCE -> period
}

/** Bit 0 is Monday, bit 6 is Sunday. */
fun DayOfWeek.bit(): Int = 1 shl (value - 1)

fun isActiveOn(weekdays: Int, date: LocalDate): Boolean =
    weekdays == ALL_WEEKDAYS || weekdays and date.dayOfWeek.bit() != 0
