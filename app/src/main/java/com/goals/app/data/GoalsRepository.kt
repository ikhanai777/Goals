package com.goals.app.data

import com.goals.app.data.backup.Backup
import com.goals.app.domain.periodFor
import java.time.DayOfWeek
import java.time.LocalDate
import java.util.UUID

/** Write operations shared by the UI and the notification actions. Reads come from [GoalsDao] flows. */
class GoalsRepository(
    private val dao: GoalsDao,
    private val onRemindersChanged: suspend () -> Unit,
    private val now: () -> Long = System::currentTimeMillis,
) {
    val areas = dao.observeAreas()
    val projects = dao.observeProjects()
    val goals = dao.observeGoals()
    val entries = dao.observeEntries()
    val milestones = dao.observeMilestones()
    val reminders = dao.observeReminders()

    suspend fun seedAreasIfEmpty() {
        if (dao.areaCount() > 0) return
        dao.insertAreas(
            listOf(
                AreaEntity(newId(), "Health", "health", 0, 0),
                AreaEntity(newId(), "Work", "work", 1, 1),
                AreaEntity(newId(), "Learning", "school", 2, 2),
                AreaEntity(newId(), "Finance", "savings", 3, 3),
                AreaEntity(newId(), "Personal", "person", 4, 4),
            ),
        )
    }

    suspend fun saveGoal(goal: GoalEntity, reminder: ReminderEntity?) {
        val existing = dao.goal(goal.id)
        val sortOrder = existing?.sortOrder ?: (dao.maxGoalSortOrder() + 1)
        dao.upsertGoal(goal.copy(sortOrder = sortOrder, updatedAt = now(), createdAt = existing?.createdAt ?: now()))
        dao.deleteRemindersFor(goal.id)
        if (reminder != null) dao.upsertReminder(reminder.copy(goalId = goal.id))
        onRemindersChanged()
    }

    suspend fun setArchived(goal: GoalEntity, archived: Boolean) {
        dao.upsertGoal(goal.copy(archived = archived, updatedAt = now()))
        onRemindersChanged()
    }

    suspend fun deleteGoal(id: String) {
        dao.deleteGoal(id)
        onRemindersChanged()
    }

    suspend fun duplicateGoal(goal: GoalEntity) {
        val copy = goal.copy(id = newId(), title = "${goal.title} (copy)", createdAt = now())
        saveGoal(copy, dao.reminderFor(goal.id)?.copy(id = newId()))
    }

    suspend fun log(goalId: String, value: Double, date: LocalDate, note: String? = null): EntryEntity {
        val entry = EntryEntity(newId(), goalId, value, date.toEpochDay(), now(), note, isSkip = false)
        dao.upsertEntry(entry)
        touch(goalId)
        return entry
    }

    /** Clears every entry (including skips) in the period containing [date], e.g. to uncheck a goal. */
    suspend fun clearPeriod(goal: GoalEntity, date: LocalDate, weekStart: DayOfWeek) {
        val period = periodFor(
            goal.cadence, date, weekStart,
            LocalDate.ofEpochDay(goal.startDate), goal.endDate?.let(LocalDate::ofEpochDay),
        )
        dao.deleteEntriesIn(goal.id, period.start.toEpochDay(), period.end.toEpochDay())
        touch(goal.id)
    }

    suspend fun skip(goalId: String, date: LocalDate): EntryEntity {
        val entry = EntryEntity(newId(), goalId, 0.0, date.toEpochDay(), now(), null, isSkip = true)
        dao.upsertEntry(entry)
        touch(goalId)
        return entry
    }

    suspend fun deleteEntry(entry: EntryEntity) = dao.deleteEntry(entry)

    suspend fun saveProject(project: ProjectEntity) =
        dao.upsertProject(project.copy(updatedAt = now()))

    suspend fun deleteProject(id: String) = dao.deleteProject(id)

    suspend fun saveMilestone(milestone: MilestoneEntity) = dao.upsertMilestone(milestone)

    suspend fun deleteMilestone(milestone: MilestoneEntity) = dao.deleteMilestone(milestone)

    suspend fun toggleMilestone(milestone: MilestoneEntity) =
        dao.upsertMilestone(milestone.copy(completedAt = if (milestone.completedAt == null) now() else null))

    suspend fun exportBackup(): Backup = Backup(
        version = Backup.VERSION,
        exportedAt = now(),
        areas = dao.allAreas(),
        projects = dao.allProjects(),
        goals = dao.allGoals(),
        entries = dao.allEntries(),
        milestones = dao.allMilestones(),
        reminders = dao.allReminders(),
    )

    /** Replaces all data with the backup's contents. */
    suspend fun importBackup(backup: Backup) {
        dao.replaceAll(
            backup.areas, backup.projects, backup.goals,
            backup.entries, backup.milestones, backup.reminders,
        )
        onRemindersChanged()
    }

    suspend fun clearAll() {
        dao.clearAll()
        seedAreasIfEmpty()
        onRemindersChanged()
    }

    private suspend fun touch(goalId: String) {
        dao.goal(goalId)?.let { dao.upsertGoal(it.copy(updatedAt = now())) }
    }

    companion object {
        fun newId(): String = UUID.randomUUID().toString()
    }
}
