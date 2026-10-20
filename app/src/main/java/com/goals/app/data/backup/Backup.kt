package com.goals.app.data.backup

import com.goals.app.data.AreaEntity
import com.goals.app.data.EntryEntity
import com.goals.app.data.GoalEntity
import com.goals.app.data.MilestoneEntity
import com.goals.app.data.ProjectEntity
import com.goals.app.data.ReminderEntity
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** The JSON export format. Dates are epoch days and timestamps epoch milliseconds, as stored. */
@Serializable
data class Backup(
    val version: Int,
    val exportedAt: Long,
    val areas: List<AreaEntity>,
    val projects: List<ProjectEntity>,
    val goals: List<GoalEntity>,
    val entries: List<EntryEntity>,
    val milestones: List<MilestoneEntity>,
    val reminders: List<ReminderEntity>,
) {
    fun toJson(): String = json.encodeToString(serializer(), this)

    companion object {
        const val VERSION = 1

        private val json = Json {
            prettyPrint = true
            ignoreUnknownKeys = true
            encodeDefaults = true
        }

        fun fromJson(text: String): Backup {
            val backup = json.decodeFromString(serializer(), text)
            require(backup.version <= VERSION) { "Backup was made by a newer version of the app" }
            return backup
        }
    }
}
