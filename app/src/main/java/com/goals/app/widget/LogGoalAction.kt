package com.goals.app.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import com.goals.app.GoalsApp
import com.goals.app.data.GoalType
import com.goals.app.domain.tapIncrement
import java.time.LocalDate

/**
 * The log button on a widget row. Check goals toggle (or clear a skip); every other type adds its
 * default increment, since a widget can't show an amount dialog.
 */
class LogGoalAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val goalId = parameters[GoalIdKey] ?: return
        val repo = (context.applicationContext as GoalsApp).container.repository
        val state = loadAppState(context)
        val item = state.goal(goalId) ?: return
        val goal = item.goal
        val today = state.today
        when {
            item.progress.skipped -> repo.clearPeriod(goal, today, state.settings.weekStart)
            goal.type == GoalType.CHECK && item.progress.met -> {
                item.entries
                    .filter { !it.isSkip && LocalDate.ofEpochDay(it.loggedFor) in item.progress.period }
                    .maxByOrNull { it.createdAt }
                    ?.let { repo.deleteEntry(it) }
            }
            else -> repo.log(goal.id, goal.tapIncrement(), today)
        }
        updateAllWidgets(context)
    }
}

val GoalIdKey = ActionParameters.Key<String>("goalId")
