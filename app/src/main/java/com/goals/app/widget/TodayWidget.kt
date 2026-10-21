package com.goals.app.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.goals.app.MainActivity

/** Resizable list of what's due today, each with a one-tap log button. */
class TodayWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val data = todayWidgetData(loadAppState(context))
        provideContent { TodayWidgetContent(data) }
    }
}

class TodayWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TodayWidget()
}

/**
 * [scrollable] is false only for static previews: a plain column renders where the home screen's
 * remote list adapter isn't available.
 */
@Composable
fun TodayWidgetContent(data: TodayWidgetData, scrollable: Boolean = true) {
    val palette = WidgetPalette.of(data.style)
    Column(
        GlanceModifier
            .fillMaxSize()
            .background(palette.background)
            .cornerRadius(16.dp)
            .padding(horizontal = 10.dp, vertical = 10.dp),
    ) {
        Row(
            GlanceModifier.fillMaxWidth().padding(horizontal = 4.dp).clickable(actionStartActivity<MainActivity>()),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(GlanceModifier.defaultWeight()) {
                Text("Today", style = TextStyle(color = palette.text, fontSize = 16.sp, fontWeight = FontWeight.Bold))
                Text(data.dateLabel, style = TextStyle(color = palette.muted, fontSize = 12.sp))
            }
            Text(
                "${data.done}/${data.total}",
                style = TextStyle(
                    color = if (palette.fireIce) palette.warm else palette.cool,
                    fontSize = 15.sp, fontWeight = FontWeight.Bold,
                ),
            )
        }
        Spacer(GlanceModifier.height(8.dp))
        if (data.goals.isEmpty()) {
            Text(
                "Nothing due today",
                style = TextStyle(color = palette.muted, fontSize = 13.sp),
                modifier = GlanceModifier.padding(4.dp),
            )
        } else if (!scrollable) {
            Column(GlanceModifier.fillMaxSize()) {
                data.goals.take(6).forEach { goal ->
                    WidgetGoalRow(goal, palette, data.showStreaks)
                    Spacer(GlanceModifier.height(6.dp))
                }
            }
        } else {
            LazyColumn(GlanceModifier.fillMaxSize()) {
                items(data.goals, itemId = { it.id.hashCode().toLong() }) { goal ->
                    Column {
                        WidgetGoalRow(goal, palette, data.showStreaks)
                        Spacer(GlanceModifier.height(6.dp))
                    }
                }
            }
        }
    }
}

/** Re-renders every placed widget; called after any data change. */
suspend fun updateAllWidgets(context: Context) {
    TodayWidget().updateAll(context)
    GoalWidget().updateAll(context)
}
