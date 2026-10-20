package com.goals.app.domain

import com.goals.app.data.Cadence
import com.goals.app.data.GoalEntity
import com.goals.app.data.GoalType
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToLong

private val shortDate = DateTimeFormatter.ofPattern("MMM d", Locale.getDefault())
private val shortDateYear = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.getDefault())

fun formatNumber(value: Double): String =
    if (value == value.roundToLong().toDouble()) value.roundToLong().toString()
    else String.format(Locale.getDefault(), "%.1f", value)

fun formatMinutes(minutes: Double): String {
    val total = minutes.roundToLong()
    val h = total / 60
    val m = total % 60
    return when {
        h == 0L -> "${m}m"
        m == 0L -> "${h}h"
        else -> "${h}h ${m}m"
    }
}

/** A single amount in the goal's unit, e.g. "5 km", "1h 30m", "3". */
fun GoalEntity.formatAmount(value: Double): String = when (type) {
    GoalType.DURATION -> formatMinutes(value)
    GoalType.CHECK, GoalType.COUNT -> formatNumber(value)
    GoalType.QUANTITY -> listOfNotNull(formatNumber(value), unit?.takeIf { it.isNotBlank() }).joinToString(" ")
}

/** Compact "value / target" label shown on rows, e.g. "12 / 20 km". */
fun GoalEntity.formatProgress(value: Double): String = when (type) {
    GoalType.CHECK -> if (target <= 1.0) (if (value >= 1.0) "Done" else "") else "${formatNumber(value)} / ${formatNumber(target)}"
    GoalType.DURATION -> "${formatMinutes(value)} / ${formatMinutes(target)}"
    GoalType.COUNT -> "${formatNumber(value)} / ${formatNumber(target)}"
    GoalType.QUANTITY -> "${formatNumber(value)} / ${formatAmount(target)}"
}

fun Cadence.label(): String = when (this) {
    Cadence.DAILY -> "Daily"
    Cadence.WEEKLY -> "Weekly"
    Cadence.MONTHLY -> "Monthly"
    Cadence.YEARLY -> "Yearly"
    Cadence.ONCE -> "Once"
}

fun Cadence.badge(): String = when (this) {
    Cadence.DAILY -> "D"
    Cadence.WEEKLY -> "W"
    Cadence.MONTHLY -> "M"
    Cadence.YEARLY -> "Y"
    Cadence.ONCE -> "1×"
}

private fun Cadence.perLabel(): String = when (this) {
    Cadence.DAILY -> "per day"
    Cadence.WEEKLY -> "per week"
    Cadence.MONTHLY -> "per month"
    Cadence.YEARLY -> "per year"
    Cadence.ONCE -> "in total"
}

fun GoalType.label(): String = when (this) {
    GoalType.CHECK -> "Check"
    GoalType.COUNT -> "Count"
    GoalType.QUANTITY -> "Amount"
    GoalType.DURATION -> "Time"
}

/** Human description of the tracker, e.g. "Weekly · 3× per week" or "Daily · 30m per day". */
fun GoalEntity.cadenceDescription(): String {
    val amount = when (type) {
        GoalType.CHECK -> if (target <= 1.0) null else "${formatNumber(target)}×"
        else -> formatAmount(target)
    }
    val days = if (cadence == Cadence.DAILY) weekdaysLabel(weekdays) else null
    return listOfNotNull(
        cadence.label(),
        amount?.let { "$it ${cadence.perLabel()}" },
        days,
    ).joinToString(" · ")
}

fun weekdaysLabel(mask: Int): String? = when (mask) {
    0b1111111 -> null
    0b0011111 -> "Weekdays"
    0b1100000 -> "Weekends"
    else -> java.time.DayOfWeek.entries
        .filter { mask and it.bit() != 0 }
        .joinToString(" ") { it.name.take(2).lowercase().replaceFirstChar(Char::titlecase) }
}

fun formatDate(date: LocalDate, today: LocalDate): String = when (date) {
    today -> "Today"
    today.plusDays(1) -> "Tomorrow"
    today.minusDays(1) -> "Yesterday"
    else -> if (date.year == today.year) date.format(shortDate) else date.format(shortDateYear)
}

fun formatDaysLeft(days: Long): String = when {
    days < 0 -> "${-days}d overdue"
    days == 0L -> "Due today"
    days == 1L -> "1 day left"
    else -> "$days days left"
}
