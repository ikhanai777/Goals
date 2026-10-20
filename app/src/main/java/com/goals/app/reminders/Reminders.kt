package com.goals.app.reminders

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.goals.app.GoalsApp
import com.goals.app.MainActivity
import com.goals.app.R
import com.goals.app.data.Cadence
import com.goals.app.data.GoalEntity
import com.goals.app.data.GoalType
import com.goals.app.data.ReminderEntity
import com.goals.app.domain.computeProgress
import com.goals.app.domain.formatAmount
import com.goals.app.domain.formatProgress
import com.goals.app.domain.isActiveOn
import com.goals.app.domain.logicalDate
import com.goals.app.domain.tapIncrement
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

private const val CHANNEL_REMINDERS = "reminders"
private const val EXTRA_REMINDER_ID = "reminderId"
private const val EXTRA_GOAL_ID = "goalId"
private const val ACTION_FIRE = "com.goals.app.REMINDER"
private const val ACTION_LOG = "com.goals.app.LOG"
private const val ACTION_SNOOZE = "com.goals.app.SNOOZE"
private const val SNOOZE_MILLIS = 60 * 60 * 1000L

/** Schedules one alarm per enabled reminder, at its next occurrence on a day the goal is active. */
class ReminderScheduler(private val context: Context) {
    private val alarms = context.getSystemService(AlarmManager::class.java)

    fun createChannels() {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_REMINDERS, "Reminders", NotificationManager.IMPORTANCE_DEFAULT),
        )
    }

    suspend fun rescheduleAll() {
        val app = context.applicationContext as GoalsApp
        val dao = app.container.database.dao()
        val enabled = app.container.settings.settings.first().notificationsEnabled
        val goals = dao.allGoals().associateBy { it.id }
        for (reminder in dao.allReminders()) {
            val goal = goals[reminder.goalId]
            if (enabled && reminder.enabled && goal != null && !goal.archived) {
                schedule(reminder, goal, LocalDateTime.now())
            } else {
                alarms.cancel(firePendingIntent(reminder.id, reminder.goalId))
            }
        }
    }

    fun schedule(reminder: ReminderEntity, goal: GoalEntity, after: LocalDateTime) {
        val next = nextOccurrence(reminder, goal, after) ?: return
        setAlarm(next.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(), firePendingIntent(reminder.id, goal.id))
    }

    fun snooze(reminderId: String, goalId: String) {
        setAlarm(System.currentTimeMillis() + SNOOZE_MILLIS, firePendingIntent(reminderId, goalId, snooze = true))
    }

    private fun setAlarm(atMillis: Long, intent: PendingIntent) {
        val exact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarms.canScheduleExactAlarms()
        if (exact) {
            alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMillis, intent)
        } else {
            alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMillis, intent)
        }
    }

    private fun firePendingIntent(reminderId: String, goalId: String, snooze: Boolean = false): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java)
            .setAction(ACTION_FIRE)
            .putExtra(EXTRA_REMINDER_ID, reminderId)
            .putExtra(EXTRA_GOAL_ID, goalId)
        val code = reminderId.hashCode() + if (snooze) 1 else 0
        return PendingIntent.getBroadcast(
            context, code, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    companion object {
        fun nextOccurrence(reminder: ReminderEntity, goal: GoalEntity, after: LocalDateTime): LocalDateTime? {
            val time = LocalTime.of(reminder.hour, reminder.minute)
            val end = goal.endDate?.let(LocalDate::ofEpochDay)
            var day = after.toLocalDate()
            repeat(8) {
                val candidate = day.atTime(time)
                if (end != null && day.isAfter(end)) return null
                val active = goal.cadence != Cadence.DAILY || isActiveOn(goal.weekdays, day)
                if (candidate.isAfter(after) && active) return candidate
                day = day.plusDays(1)
            }
            return null
        }
    }
}

/** Shows a reminder notification unless the goal is already done for this period, then re-arms. */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val reminderId = intent.getStringExtra(EXTRA_REMINDER_ID) ?: return
        val goalId = intent.getStringExtra(EXTRA_GOAL_ID) ?: return
        val app = context.applicationContext as GoalsApp
        goAsync(app.container.scope) {
            val dao = app.container.database.dao()
            val goal = dao.goal(goalId) ?: return@goAsync
            val reminder = dao.allReminders().firstOrNull { it.id == reminderId } ?: return@goAsync
            if (goal.archived || !reminder.enabled) return@goAsync
            val settings = app.container.settings.settings.first()
            val now = LocalDateTime.now()
            app.container.reminderScheduler.schedule(reminder, goal, now)
            if (!settings.notificationsEnabled) return@goAsync
            val today = logicalDate(now, settings.dayStartHour)
            val entries = dao.allEntries().filter { it.goalId == goal.id }
            val progress = computeProgress(goal, entries, today, settings.weekStart)
            if (!progress.done) showNotification(context, goal, reminderId, goal.formatProgress(progress.value))
        }
    }
}

/** Handles the "Done / +N" and "Snooze" buttons without opening the app. */
class ReminderActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val goalId = intent.getStringExtra(EXTRA_GOAL_ID) ?: return
        val reminderId = intent.getStringExtra(EXTRA_REMINDER_ID) ?: return
        val app = context.applicationContext as GoalsApp
        NotificationManagerCompat.from(context).cancel(goalId.hashCode())
        when (intent.action) {
            ACTION_SNOOZE -> app.container.reminderScheduler.snooze(reminderId, goalId)
            ACTION_LOG -> goAsync(app.container.scope) {
                val goal = app.container.database.dao().goal(goalId) ?: return@goAsync
                val settings = app.container.settings.settings.first()
                val today = logicalDate(LocalDateTime.now(), settings.dayStartHour)
                app.container.repository.log(goal.id, goal.tapIncrement(), today)
            }
        }
    }
}

/** Alarms are cleared on reboot and drift on clock changes, so re-arm them. */
class RescheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext as GoalsApp
        goAsync(app.container.scope) { app.container.reminderScheduler.rescheduleAll() }
    }
}

private fun BroadcastReceiver.goAsync(scope: CoroutineScope, block: suspend () -> Unit) {
    val pending = goAsync()
    scope.launch {
        try {
            block()
        } finally {
            pending.finish()
        }
    }
}

private fun showNotification(context: Context, goal: GoalEntity, reminderId: String, progressLabel: String) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
    ) {
        return
    }
    fun action(action: String, code: Int): PendingIntent = PendingIntent.getBroadcast(
        context,
        goal.id.hashCode() + code,
        Intent(context, ReminderActionReceiver::class.java)
            .setAction(action)
            .putExtra(EXTRA_GOAL_ID, goal.id)
            .putExtra(EXTRA_REMINDER_ID, reminderId),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
    val open = PendingIntent.getActivity(
        context, goal.id.hashCode(),
        Intent(context, MainActivity::class.java).putExtra(MainActivity.EXTRA_GOAL_ID, goal.id),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
    val logLabel = if (goal.type == GoalType.CHECK) "Done" else "+${goal.formatAmount(goal.tapIncrement())}"
    val notification = NotificationCompat.Builder(context, CHANNEL_REMINDERS)
        .setSmallIcon(R.drawable.ic_notification)
        .setColor(ContextCompat.getColor(context, R.color.cobalt))
        .setContentTitle(goal.title)
        .setContentText(progressLabel)
        .setContentIntent(open)
        .setAutoCancel(true)
        .addAction(0, logLabel, action(ACTION_LOG, 2))
        .addAction(0, "Snooze 1 h", action(ACTION_SNOOZE, 3))
        .build()
    NotificationManagerCompat.from(context).notify(goal.id.hashCode(), notification)
}
