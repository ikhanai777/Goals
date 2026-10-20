package com.goals.app.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.goals.app.GoalsApp
import com.goals.app.data.AppSettings
import com.goals.app.data.AreaEntity
import com.goals.app.data.EntryEntity
import com.goals.app.data.GoalEntity
import com.goals.app.data.GoalType
import com.goals.app.data.MilestoneEntity
import com.goals.app.data.ProjectEntity
import com.goals.app.data.backup.Backup
import com.goals.app.domain.formatAmount
import com.goals.app.domain.logicalDate
import com.goals.app.domain.tapIncrement
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalDateTime

data class UiMessage(val text: String, val undo: (() -> Unit)? = null)

class GoalsViewModel(application: Application) : AndroidViewModel(application) {
    private val container = (application as GoalsApp).container
    private val repo = container.repository

    private val clock = flow {
        while (true) {
            emit(LocalDateTime.now())
            delay(60_000)
        }
    }

    private val data = combine(repo.areas, repo.projects, repo.goals, repo.entries, repo.milestones) { a, p, g, e, m ->
        Data(a, p, g, e, m)
    }

    private val today = combine(clock, container.settings.settings) { now, s -> logicalDate(now, s.dayStartHour) }
        .distinctUntilChanged()

    val state: StateFlow<AppState> = combine(data, repo.reminders, container.settings.settings, today) { d, r, s, t ->
        buildAppState(t, s, d.areas, d.projects, d.goals, d.entries, d.milestones, r)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppState())

    private val messageChannel = Channel<UiMessage>(Channel.BUFFERED)
    val messages = messageChannel.receiveAsFlow()

    private val settings: AppSettings get() = state.value.settings
    private val todayDate: LocalDate get() = state.value.today

    /** The tap on a row's log control. Returns false when the goal needs an amount entered instead. */
    fun tapLog(item: GoalItem): Boolean {
        val goal = item.goal
        when (goal.type) {
            GoalType.CHECK -> launch {
                if (item.progress.skipped) {
                    repo.clearPeriod(goal, todayDate, settings.weekStart)
                } else if (item.progress.met) {
                    latestEntryInPeriod(item)?.let { repo.deleteEntry(it) }
                } else {
                    repo.log(goal.id, 1.0, todayDate)
                }
            }
            GoalType.COUNT -> launch { repo.log(goal.id, goal.tapIncrement(), todayDate) }
            GoalType.QUANTITY, GoalType.DURATION -> return false
        }
        return true
    }

    /** Swipe right: log one default increment, with undo. */
    fun quickLog(item: GoalItem) {
        val goal = item.goal
        if (goal.type == GoalType.CHECK && item.progress.met) return
        log(item, goal.tapIncrement(), todayDate)
    }

    fun log(item: GoalItem, value: Double, date: LocalDate) = launch {
        val entry = repo.log(item.goal.id, value, date)
        val label = if (item.goal.type == GoalType.CHECK) "Marked done" else "Logged ${item.goal.formatAmount(value)}"
        messageChannel.send(
            UiMessage("$label · ${item.goal.title}") {
                launch { repo.deleteEntry(entry) }
            },
        )
    }

    fun skip(item: GoalItem) = launch {
        if (item.progress.done) return@launch
        val entry = repo.skip(item.goal.id, todayDate)
        messageChannel.send(
            UiMessage("Skipped · ${item.goal.title}") {
                launch { repo.deleteEntry(entry) }
            },
        )
    }

    fun deleteEntry(entry: EntryEntity) = launch { repo.deleteEntry(entry) }

    fun archive(item: GoalItem, archived: Boolean) = launch { repo.setArchived(item.goal, archived) }

    fun duplicate(item: GoalItem) = launch { repo.duplicateGoal(item.goal) }

    fun deleteGoal(item: GoalItem) = launch { repo.deleteGoal(item.goal.id) }

    fun saveGoal(draft: GoalDraft) = launch {
        repo.saveGoal(draft.toEntity(), draft.toReminder())
    }

    fun saveProject(project: ProjectEntity) = launch { repo.saveProject(project) }

    fun archiveProject(project: ProjectEntity, archived: Boolean) =
        launch { repo.saveProject(project.copy(archived = archived)) }

    fun deleteProject(project: ProjectEntity) = launch { repo.deleteProject(project.id) }

    fun saveMilestone(milestone: MilestoneEntity) = launch { repo.saveMilestone(milestone) }

    fun toggleMilestone(milestone: MilestoneEntity) = launch { repo.toggleMilestone(milestone) }

    fun deleteMilestone(milestone: MilestoneEntity) = launch { repo.deleteMilestone(milestone) }

    fun updateSettings(transform: (AppSettings) -> AppSettings) = launch {
        container.settings.update(transform)
        container.reminderScheduler.rescheduleAll()
    }

    fun exportTo(uri: Uri) = launch {
        val result = runCatching {
            val json = repo.exportBackup().toJson()
            withContext(Dispatchers.IO) {
                getApplication<Application>().contentResolver.openOutputStream(uri, "wt")!!.use {
                    it.write(json.toByteArray())
                }
            }
        }
        messageChannel.send(UiMessage(if (result.isSuccess) "Exported" else "Export failed"))
    }

    fun importFrom(uri: Uri) = launch {
        val result = runCatching {
            val text = withContext(Dispatchers.IO) {
                getApplication<Application>().contentResolver.openInputStream(uri)!!.use {
                    it.readBytes().decodeToString()
                }
            }
            repo.importBackup(Backup.fromJson(text))
        }
        messageChannel.send(
            UiMessage(if (result.isSuccess) "Imported" else "Import failed: ${result.exceptionOrNull()?.message}"),
        )
    }

    fun clearAll() = launch {
        repo.clearAll()
        messageChannel.send(UiMessage("All data cleared"))
    }

    private fun latestEntryInPeriod(item: GoalItem): EntryEntity? {
        val period = item.progress.period
        return item.entries
            .filter { !it.isSkip && LocalDate.ofEpochDay(it.loggedFor) in period }
            .maxByOrNull { it.createdAt }
    }

    private fun launch(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }

    private data class Data(
        val areas: List<AreaEntity>,
        val projects: List<ProjectEntity>,
        val goals: List<GoalEntity>,
        val entries: List<EntryEntity>,
        val milestones: List<MilestoneEntity>,
    )
}
