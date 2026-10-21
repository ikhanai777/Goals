package com.goals.app

import android.content.Context
import android.widget.FrameLayout
import android.widget.RemoteViews
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.glance.GlanceId
import androidx.glance.appwidget.ExperimentalGlanceRemoteViewsApi
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.compose
import androidx.glance.appwidget.provideContent
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import com.goals.app.data.AppSettings
import com.goals.app.data.ThemeStyle
import com.goals.app.domain.cadenceDescription
import com.goals.app.widget.GoalWidgetContent
import com.goals.app.widget.TodayWidgetContent
import com.goals.app.widget.todayWidgetData
import com.goals.app.widget.toWidgetGoal
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Renders the widgets through Glance into real RemoteViews and captures them on a mock home screen. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w393dp-h852dp-xxhdpi", application = android.app.Application::class)
class WidgetScreenshotTest {
    @get:Rule val compose = createComposeRule()

    private val context: Context get() = ApplicationProvider.getApplicationContext()

    /** Wraps fixed content so it can go through Glance's real RemoteViews pipeline. */
    private class Preview(private val content: @Composable () -> Unit) : GlanceAppWidget() {
        override suspend fun provideGlance(context: Context, id: GlanceId) = provideContent { content() }
    }

    @OptIn(ExperimentalGlanceRemoteViewsApi::class)
    private fun render(size: DpSize, content: @Composable () -> Unit): RemoteViews =
        runBlocking { Preview(content).compose(context, size = size) }

    private fun widgets(style: ThemeStyle): List<Pair<DpSize, RemoteViews>> {
        val state = SampleData.state(AppSettings(style = style))
        val run = state.goal("run")!!
        val meditate = state.goal("meditate")!!
        val today = DpSize(340.dp, 330.dp)
        val wide = DpSize(220.dp, 110.dp)
        val small = DpSize(96.dp, 96.dp)
        return listOf(
            today to render(today) { TodayWidgetContent(todayWidgetData(state), scrollable = false) },
            wide to render(wide) { GoalWidgetContent(run.toWidgetGoal(), run.goal.cadenceDescription(), style, 1) },
            small to render(small) { GoalWidgetContent(meditate.toWidgetGoal(), null, style, 2) },
        )
    }

    @Composable
    private fun Hosted(size: DpSize, views: RemoteViews, tag: String) {
        AndroidView(
            factory = { c -> FrameLayout(c).also { parent -> parent.addView(views.apply(c, parent)) } },
            modifier = Modifier.size(size).testTag(tag),
        )
    }

    private fun homeScreen(name: String, style: ThemeStyle) {
        val (today, wide, small) = widgets(style)
        compose.setContent {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Brush.verticalGradient(listOf(Color(0xFF3E5C88), Color(0xFF7A6FA8), Color(0xFFC79A84)))),
            ) {
                Column(
                    Modifier.padding(top = 48.dp).align(Alignment.TopCenter),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Hosted(today.first, today.second, "today")
                    Row(horizontalArrangement = Arrangement.spacedBy(20.dp), verticalAlignment = Alignment.CenterVertically) {
                        Hosted(wide.first, wide.second, "goal")
                        Hosted(small.first, small.second, "streak")
                    }
                }
            }
        }
        compose.onRoot().captureRoboImage("build/outputs/roborazzi/$name.png")
    }

    @Test fun widgets_classic() {
        homeScreen("16_widgets_classic", ThemeStyle.CLASSIC)
        // The picker previews shipped in res/drawable-nodpi come from these captures.
        compose.onNodeWithTag("today").captureRoboImage("build/outputs/roborazzi/widget_preview_today.png")
        compose.onNodeWithTag("goal").captureRoboImage("build/outputs/roborazzi/widget_preview_goal.png")
    }

    @Test fun widgets_fire_ice() = homeScreen("17_widgets_fire_ice", ThemeStyle.FIRE_ICE)
}
