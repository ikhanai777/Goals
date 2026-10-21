package com.goals.app.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionStartActivity as startActivityIntent
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.LinearProgressIndicator
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row as GlanceRow
import androidx.glance.layout.Spacer as GlanceSpacer
import androidx.glance.layout.fillMaxSize as glanceFillMaxSize
import androidx.glance.layout.fillMaxWidth as glanceFillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding as glancePadding
import androidx.glance.layout.width as glanceWidth
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import com.goals.app.data.ThemeStyle
import com.goals.app.domain.cadenceDescription
import com.goals.app.ui.GoalItem
import com.goals.app.ui.components.EmptyState
import com.goals.app.ui.components.iconFor
import com.goals.app.ui.theme.AppBackground
import com.goals.app.ui.theme.GoalsTheme
import kotlinx.coroutines.launch
import androidx.compose.material3.Text as M3Text

private val GoalKey = stringPreferencesKey("goal_id")

/**
 * One chosen goal. At 1×1 it shows just the streak; larger sizes add the title, progress and a
 * log button.
 */
class GoalWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Exact
    override val stateDefinition = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val goalId = getAppWidgetState<Preferences>(context, id)[GoalKey]
        val state = loadAppState(context)
        val goal = goalId?.let(state::goal)?.takeIf { !it.goal.archived }
        val appWidgetId = GlanceAppWidgetManager(context).getAppWidgetId(id)
        provideContent {
            GoalWidgetContent(goal?.toWidgetGoal(), goal?.goal?.cadenceDescription(), state.settings.style, appWidgetId)
        }
    }
}

class GoalWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = GoalWidget()
}

private val SmallSize = DpSize(110.dp, 110.dp)

@Composable
fun GoalWidgetContent(goal: WidgetGoal?, subtitle: String?, style: ThemeStyle, appWidgetId: Int) {
    val palette = WidgetPalette.of(style)
    val size = LocalSize.current
    val small = size.width < SmallSize.width || size.height < 80.dp
    val base = GlanceModifier.glanceFillMaxSize().background(palette.background).cornerRadius(16.dp)
    if (goal == null) {
        // Deleted or archived goal: let the user pick another.
        Column(
            base.glancePadding(8.dp).clickable(
                startActivityIntent(
                    Intent(LocalContext.current, GoalWidgetConfigActivity::class.java)
                        .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId),
                ),
            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                "Choose a goal",
                style = TextStyle(color = palette.muted, fontSize = 12.sp, textAlign = TextAlign.Center),
            )
        }
        return
    }
    if (small) {
        Column(
            base.glancePadding(6.dp).clickable(openGoal(goal.id)),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            WidgetStreak(goal.streak, palette, fontSize = 22, iconSize = 20.dp)
            Text(
                goal.title,
                style = TextStyle(color = palette.muted, fontSize = 10.sp, textAlign = TextAlign.Center),
                maxLines = 1,
            )
        }
        return
    }
    val accent = palette.accent(goal)
    Column(base.glancePadding(12.dp).clickable(openGoal(goal.id))) {
        GlanceRow(GlanceModifier.glanceFillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(GlanceModifier.defaultWeight()) {
                Text(
                    goal.title,
                    style = TextStyle(color = palette.text, fontSize = 14.sp, fontWeight = FontWeight.Bold),
                    maxLines = 1,
                )
                if (subtitle != null) {
                    Text(subtitle, style = TextStyle(color = palette.muted, fontSize = 11.sp), maxLines = 1)
                }
            }
            WidgetLogButton(goal, palette, size = 44.dp, icon = 30.dp)
        }
        GlanceSpacer(GlanceModifier.height(6.dp))
        GlanceRow(GlanceModifier.glanceFillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                goal.progressLabel,
                style = TextStyle(color = accent, fontSize = 16.sp, fontWeight = FontWeight.Bold),
                modifier = GlanceModifier.defaultWeight(),
            )
            if (goal.streak >= 1) WidgetStreak(goal.streak, palette, fontSize = 13, iconSize = 13.dp)
        }
        GlanceSpacer(GlanceModifier.height(6.dp))
        LinearProgressIndicator(
            progress = goal.fraction,
            modifier = GlanceModifier.glanceFillMaxWidth().height(6.dp),
            color = accent,
            backgroundColor = palette.track,
        )
    }
}

/** Shown when a Goal widget is placed (or reconfigured): pick which goal it tracks. */
class GoalWidgetConfigActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val appWidgetId = intent?.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
            ?: AppWidgetManager.INVALID_APPWIDGET_ID
        // Backing out without a choice must not leave an empty widget on the home screen.
        setResult(RESULT_CANCELED, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId))
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }
        setContent {
            var goals by remember { mutableStateOf<List<GoalItem>?>(null) }
            var style by remember { mutableStateOf(ThemeStyle.CLASSIC) }
            val scope = rememberCoroutineScope()
            LaunchedEffect(Unit) {
                val state = loadAppState(this@GoalWidgetConfigActivity)
                style = state.settings.style
                goals = state.goals.filter { !it.goal.archived }
            }
            GoalsTheme(style = style) {
                Box(Modifier.fillMaxSize()) {
                    AppBackground()
                    GoalPicker(goals) { item ->
                        scope.launch {
                            val manager = GlanceAppWidgetManager(this@GoalWidgetConfigActivity)
                            val glanceId = manager.getGlanceIdBy(appWidgetId)
                            updateAppWidgetState(this@GoalWidgetConfigActivity, glanceId) { it[GoalKey] = item.goal.id }
                            GoalWidget().update(this@GoalWidgetConfigActivity, glanceId)
                            setResult(RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId))
                            finish()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GoalPicker(goals: List<GoalItem>?, onPick: (GoalItem) -> Unit) {
    LazyColumn(
        Modifier.fillMaxSize().safeDrawingPadding(),
        contentPadding = PaddingValues(16.dp),
    ) {
        item {
            M3Text("Choose a goal for this widget", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onBackground)
            Spacer(Modifier.size(12.dp))
        }
        if (goals?.isEmpty() == true) {
            item { EmptyState("Add a goal in the app first", null, {}) }
        }
        items(goals.orEmpty(), key = { it.goal.id }) { item ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable { onPick(item) }
                    .padding(vertical = 12.dp),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            ) {
                Icon(iconFor(item.goal.icon), null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(12.dp))
                androidx.compose.foundation.layout.Column(Modifier.weight(1f)) {
                    M3Text(item.goal.title, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onBackground)
                    M3Text(item.goal.cadenceDescription(), style = MaterialTheme.typography.bodySmall, color = GoalsTheme.extra.muted)
                }
            }
        }
    }
}
