package com.goals.app.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface GoalsDao {
    @Query("SELECT * FROM areas ORDER BY sortOrder")
    fun observeAreas(): Flow<List<AreaEntity>>

    @Query("SELECT * FROM projects ORDER BY archived, title COLLATE NOCASE")
    fun observeProjects(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM goals ORDER BY sortOrder, createdAt")
    fun observeGoals(): Flow<List<GoalEntity>>

    @Query("SELECT * FROM entries")
    fun observeEntries(): Flow<List<EntryEntity>>

    @Query("SELECT * FROM milestones ORDER BY sortOrder, dueDate")
    fun observeMilestones(): Flow<List<MilestoneEntity>>

    @Query("SELECT * FROM reminders")
    fun observeReminders(): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM goals WHERE id = :id")
    suspend fun goal(id: String): GoalEntity?

    @Query("SELECT * FROM reminders WHERE goalId = :goalId LIMIT 1")
    suspend fun reminderFor(goalId: String): ReminderEntity?

    @Query("SELECT * FROM reminders WHERE enabled = 1")
    suspend fun enabledReminders(): List<ReminderEntity>

    @Query("SELECT COUNT(*) FROM areas")
    suspend fun areaCount(): Int

    @Query("SELECT COALESCE(MAX(sortOrder), 0) FROM goals")
    suspend fun maxGoalSortOrder(): Int

    @Upsert suspend fun upsertArea(area: AreaEntity)
    @Upsert suspend fun upsertProject(project: ProjectEntity)
    @Upsert suspend fun upsertGoal(goal: GoalEntity)
    @Upsert suspend fun upsertEntry(entry: EntryEntity)
    @Upsert suspend fun upsertMilestone(milestone: MilestoneEntity)
    @Upsert suspend fun upsertReminder(reminder: ReminderEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertAreas(items: List<AreaEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertProjects(items: List<ProjectEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertGoals(items: List<GoalEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertEntries(items: List<EntryEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertMilestones(items: List<MilestoneEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertReminders(items: List<ReminderEntity>)

    @Delete suspend fun deleteEntry(entry: EntryEntity)
    @Delete suspend fun deleteMilestone(milestone: MilestoneEntity)

    @Query("DELETE FROM goals WHERE id = :id") suspend fun deleteGoal(id: String)
    @Query("DELETE FROM reminders WHERE goalId = :goalId") suspend fun deleteRemindersFor(goalId: String)
    @Query("UPDATE goals SET projectId = NULL WHERE projectId = :projectId") suspend fun detachGoals(projectId: String)
    @Query("DELETE FROM projects WHERE id = :id") suspend fun deleteProjectRow(id: String)

    @Query("DELETE FROM entries WHERE goalId = :goalId AND loggedFor BETWEEN :from AND :to")
    suspend fun deleteEntriesIn(goalId: String, from: Long, to: Long)

    @Query("SELECT * FROM areas") suspend fun allAreas(): List<AreaEntity>
    @Query("SELECT * FROM projects") suspend fun allProjects(): List<ProjectEntity>
    @Query("SELECT * FROM goals") suspend fun allGoals(): List<GoalEntity>
    @Query("SELECT * FROM entries") suspend fun allEntries(): List<EntryEntity>
    @Query("SELECT * FROM milestones") suspend fun allMilestones(): List<MilestoneEntity>
    @Query("SELECT * FROM reminders") suspend fun allReminders(): List<ReminderEntity>

    @Query("DELETE FROM entries") suspend fun clearEntries()
    @Query("DELETE FROM reminders") suspend fun clearReminders()
    @Query("DELETE FROM milestones") suspend fun clearMilestones()
    @Query("DELETE FROM goals") suspend fun clearGoals()
    @Query("DELETE FROM projects") suspend fun clearProjects()
    @Query("DELETE FROM areas") suspend fun clearAreas()

    @Transaction
    suspend fun deleteProject(id: String) {
        detachGoals(id)
        deleteProjectRow(id)
    }

    @Transaction
    suspend fun replaceAll(
        areas: List<AreaEntity>, projects: List<ProjectEntity>, goals: List<GoalEntity>,
        entries: List<EntryEntity>, milestones: List<MilestoneEntity>, reminders: List<ReminderEntity>,
    ) {
        clearAll()
        insertAreas(areas); insertProjects(projects); insertGoals(goals)
        insertEntries(entries); insertMilestones(milestones); insertReminders(reminders)
    }

    @Transaction
    suspend fun clearAll() {
        clearEntries(); clearReminders(); clearMilestones(); clearGoals(); clearProjects(); clearAreas()
    }
}

@Database(
    entities = [
        AreaEntity::class, ProjectEntity::class, GoalEntity::class,
        EntryEntity::class, MilestoneEntity::class, ReminderEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class GoalsDatabase : RoomDatabase() {
    abstract fun dao(): GoalsDao

    companion object {
        fun build(context: Context): GoalsDatabase =
            Room.databaseBuilder(context, GoalsDatabase::class.java, "goals.db").build()
    }
}
