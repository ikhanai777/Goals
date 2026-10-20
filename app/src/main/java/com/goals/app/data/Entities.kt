package com.goals.app.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

enum class GoalType { CHECK, COUNT, QUANTITY, DURATION }

enum class Cadence { DAILY, WEEKLY, MONTHLY, YEARLY, ONCE }

@Serializable
@Entity(tableName = "areas")
data class AreaEntity(
    @PrimaryKey val id: String,
    val name: String,
    val icon: String,
    val colorIndex: Int,
    val sortOrder: Int,
)

@Serializable
@Entity(tableName = "projects", indices = [Index("areaId")])
data class ProjectEntity(
    @PrimaryKey val id: String,
    val areaId: String?,
    val title: String,
    val notes: String?,
    /** Epoch day. */
    val startDate: Long,
    /** Epoch day, or null for open-ended projects. */
    val dueDate: Long?,
    val archived: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
)

/**
 * A goal and its tracker. The spec models these as two 1:1 tables; the MVP keeps them in one
 * row since every goal has exactly one tracker.
 */
@Serializable
@Entity(tableName = "goals", indices = [Index("projectId"), Index("areaId")])
data class GoalEntity(
    @PrimaryKey val id: String,
    val projectId: String?,
    val areaId: String?,
    val title: String,
    val icon: String,
    val notes: String?,
    val type: GoalType,
    val cadence: Cadence,
    /** Times per period for CHECK, total amount per period otherwise. Minutes for DURATION. */
    val target: Double,
    val unit: String?,
    val defaultIncrement: Double,
    /** Bitmask of active weekdays (bit 0 = Monday) for DAILY goals; 127 means every day. */
    val weekdays: Int,
    /** Epoch day. */
    val startDate: Long,
    /** Epoch day; the deadline for ONCE goals, optional end for recurring ones. */
    val endDate: Long?,
    val archived: Boolean,
    val sortOrder: Int,
    val createdAt: Long,
    val updatedAt: Long,
)

@Serializable
@Entity(
    tableName = "entries",
    indices = [Index("goalId"), Index("loggedFor")],
    foreignKeys = [
        ForeignKey(GoalEntity::class, ["id"], ["goalId"], onDelete = ForeignKey.CASCADE),
    ],
)
data class EntryEntity(
    @PrimaryKey val id: String,
    val goalId: String,
    val value: Double,
    /** Epoch day the progress counts toward. */
    val loggedFor: Long,
    val createdAt: Long,
    val note: String?,
    val isSkip: Boolean,
)

@Serializable
@Entity(
    tableName = "milestones",
    indices = [Index("projectId")],
    foreignKeys = [
        ForeignKey(ProjectEntity::class, ["id"], ["projectId"], onDelete = ForeignKey.CASCADE),
    ],
)
data class MilestoneEntity(
    @PrimaryKey val id: String,
    val projectId: String,
    val title: String,
    val dueDate: Long?,
    val completedAt: Long?,
    val sortOrder: Int,
)

@Serializable
@Entity(
    tableName = "reminders",
    indices = [Index("goalId")],
    foreignKeys = [
        ForeignKey(GoalEntity::class, ["id"], ["goalId"], onDelete = ForeignKey.CASCADE),
    ],
)
data class ReminderEntity(
    @PrimaryKey val id: String,
    val goalId: String,
    val hour: Int,
    val minute: Int,
    val enabled: Boolean,
)

const val ALL_WEEKDAYS = 0b1111111
