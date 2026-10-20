package com.goals.app

import android.app.Application
import android.content.Context
import com.goals.app.data.GoalsDatabase
import com.goals.app.data.GoalsRepository
import com.goals.app.data.SettingsRepository
import com.goals.app.reminders.ReminderScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Manual dependency container; small enough that a DI framework would add more than it saves. */
class AppContainer(context: Context) {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val database = GoalsDatabase.build(context)
    val settings = SettingsRepository(context)
    val reminderScheduler = ReminderScheduler(context)
    val repository = GoalsRepository(database.dao(), onRemindersChanged = { reminderScheduler.rescheduleAll() })
}

class GoalsApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        container.reminderScheduler.createChannels()
        container.scope.launch { container.repository.seedAreasIfEmpty() }
    }
}
