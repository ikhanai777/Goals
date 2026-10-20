package com.goals.app.ui

import com.goals.app.data.ALL_WEEKDAYS
import com.goals.app.data.Cadence
import com.goals.app.data.GoalEntity
import com.goals.app.data.GoalType
import com.goals.app.data.GoalsRepository
import com.goals.app.data.ReminderEntity
import com.goals.app.domain.formatNumber
import java.time.LocalDate

/** Editable form state shared by the quick-add sheet and the full goal editor. */
data class GoalDraft(
    val id: String? = null,
    val title: String = "",
    val cadence: Cadence = Cadence.DAILY,
    val type: GoalType = GoalType.CHECK,
    val target: String = "",
    val unit: String = "",
    val increment: String = "",
    val weekdays: Int = ALL_WEEKDAYS,
    val startDate: LocalDate = LocalDate.now(),
    val endDate: LocalDate? = null,
    val areaId: String? = null,
    val projectId: String? = null,
    val icon: String = "flag",
    val notes: String = "",
    val reminderOn: Boolean = false,
    val reminderHour: Int = 9,
    val reminderMinute: Int = 0,
    val reminderId: String? = null,
    val archived: Boolean = false,
    val createdAt: Long = 0,
) {
    enum class Field { TITLE, TARGET, INCREMENT, END_DATE }

    /** CHECK goals default to once per period; other types need an explicit target. */
    private val targetValue: Double?
        get() = if (type == GoalType.CHECK && target.isBlank()) 1.0 else target.toDoubleOrNull()

    private val incrementValue: Double?
        get() = if (increment.isBlank()) defaultIncrement(type) else increment.toDoubleOrNull()

    fun errors(): Map<Field, String> = buildMap {
        if (title.isBlank()) put(Field.TITLE, "Title is required")
        else if (title.length > 80) put(Field.TITLE, "Keep it under 80 characters")
        val t = targetValue
        if (t == null || t <= 0) put(Field.TARGET, "Enter a target above 0")
        val i = incrementValue
        if (i == null || i <= 0) put(Field.INCREMENT, "Enter an amount above 0")
        if (endDate != null && endDate.isBefore(startDate)) put(Field.END_DATE, "End date is before the start date")
        if (cadence == Cadence.ONCE && endDate == null) put(Field.END_DATE, "Pick a deadline")
    }

    fun toEntity(now: Long = System.currentTimeMillis()): GoalEntity = GoalEntity(
        id = id ?: GoalsRepository.newId(),
        projectId = projectId,
        areaId = areaId,
        title = title.trim(),
        icon = icon,
        notes = notes.trim().ifEmpty { null },
        type = type,
        cadence = cadence,
        target = targetValue ?: 1.0,
        unit = unit.trim().ifEmpty { null }.takeIf { type == GoalType.QUANTITY },
        defaultIncrement = incrementValue ?: 1.0,
        weekdays = if (cadence == Cadence.DAILY) weekdays else ALL_WEEKDAYS,
        startDate = startDate.toEpochDay(),
        endDate = endDate?.toEpochDay(),
        archived = archived,
        sortOrder = 0,
        createdAt = createdAt.takeIf { it > 0 } ?: now,
        updatedAt = now,
    )

    fun toReminder(): ReminderEntity? = if (!reminderOn) null else ReminderEntity(
        id = reminderId ?: GoalsRepository.newId(),
        goalId = id.orEmpty(),
        hour = reminderHour,
        minute = reminderMinute,
        enabled = true,
    )

    companion object {
        fun defaultIncrement(type: GoalType): Double = when (type) {
            GoalType.DURATION -> 15.0
            else -> 1.0
        }

        fun from(item: GoalItem): GoalDraft {
            val g = item.goal
            return GoalDraft(
                id = g.id,
                title = g.title,
                cadence = g.cadence,
                type = g.type,
                target = formatNumber(g.target),
                unit = g.unit.orEmpty(),
                increment = formatNumber(g.defaultIncrement),
                weekdays = g.weekdays,
                startDate = LocalDate.ofEpochDay(g.startDate),
                endDate = g.endDate?.let(LocalDate::ofEpochDay),
                areaId = g.areaId,
                projectId = g.projectId,
                icon = g.icon,
                notes = g.notes.orEmpty(),
                reminderOn = item.reminder?.enabled == true,
                reminderHour = item.reminder?.hour ?: 9,
                reminderMinute = item.reminder?.minute ?: 0,
                reminderId = item.reminder?.id,
                archived = g.archived,
                createdAt = g.createdAt,
            )
        }
    }
}
