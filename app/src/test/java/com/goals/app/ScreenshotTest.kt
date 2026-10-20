package com.goals.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import com.goals.app.data.AppSettings
import com.goals.app.data.Cadence
import com.goals.app.data.Density
import com.goals.app.data.GoalType
import com.goals.app.data.ThemeMode
import com.goals.app.data.ThemeStyle
import com.goals.app.ui.theme.AppBackground
import com.goals.app.ui.AddButton
import com.goals.app.ui.AppNavigationBar
import com.goals.app.ui.AppState
import com.goals.app.ui.GoalDraft
import com.goals.app.ui.RowActions
import com.goals.app.ui.TodayFilter
import com.goals.app.ui.detail.GoalDetailScreen
import com.goals.app.ui.editor.GoalEditorScreen
import com.goals.app.ui.editor.QuickAddContent
import com.goals.app.ui.goals.GoalsScreen
import com.goals.app.ui.project.ProjectDetailScreen
import com.goals.app.ui.settings.SettingsScreen
import com.goals.app.ui.theme.GoalsTheme
import com.goals.app.ui.today.TodayScreen
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Renders each MVP screen with sample data. Output goes to build/outputs/roborazzi. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w393dp-h852dp-xxhdpi", application = android.app.Application::class)
class ScreenshotTest {
    @get:Rule val compose = createComposeRule()

    private val noRows = RowActions({}, {}, {}, {})

    private fun shot(
        name: String,
        theme: ThemeMode = ThemeMode.LIGHT,
        density: Density = Density.COMFORTABLE,
        style: ThemeStyle = ThemeStyle.CLASSIC,
        content: @Composable BoxScope.(AppState) -> Unit,
    ) {
        val state = SampleData.state(AppSettings(theme = theme, density = density, style = style))
        compose.setContent {
            GoalsTheme(theme, density, style = style) {
                Box(Modifier.fillMaxSize()) {
                    AppBackground()
                    content(state)
                }
            }
        }
        compose.onRoot().captureRoboImage("build/outputs/roborazzi/$name.png")
    }

    /** The app's bottom bar and add button around a tab's content, as GoalsAppRoot lays them out. */
    @Composable
    private fun Chrome(route: String, fab: Boolean = true, content: @Composable () -> Unit) {
        Scaffold(
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onBackground,
            bottomBar = { AppNavigationBar(route) {} },
            floatingActionButton = { if (fab) AddButton({}, {}) },
        ) { padding -> Box(Modifier.padding(padding)) { content() } }
    }

    @Test fun today_light() = shot("01_today_light") { s ->
        Chrome("today") { TodayScreen(s, TodayFilter.TODAY, {}, noRows, {}, {}, {}) }
    }

    @Test fun today_dark() = shot("02_today_dark", ThemeMode.DARK) { s ->
        Chrome("today") { TodayScreen(s, TodayFilter.TODAY, {}, noRows, {}, {}, {}) }
    }

    @Test fun today_compact() = shot("03_today_compact", density = Density.COMPACT) { s ->
        Chrome("today") { TodayScreen(s, TodayFilter.TODAY, {}, noRows, {}, {}, {}) }
    }

    @Test fun goals_library() = shot("04_goals_library") { s ->
        Chrome("goals") { GoalsScreen(s, {}, {}, {}) }
    }

    @Test fun goal_detail_daily() = shot("05_goal_detail_daily") { s ->
        GoalDetailScreen(s, s.goal("meditate")!!, {}, {}, {}, {}, {}, {}, {}, {}, {})
    }

    @Test fun goal_detail_weekly_dark() = shot("06_goal_detail_weekly_dark", ThemeMode.DARK) { s ->
        GoalDetailScreen(s, s.goal("run")!!, {}, {}, {}, {}, {}, {}, {}, {}, {})
    }

    @Test fun project_detail() = shot("07_project_detail") { s ->
        ProjectDetailScreen(s, s.project("portfolio")!!, noRows, {}, {}, {}, {}, {}, {}, {}, {})
    }

    @Test fun quick_add() = shot("08_quick_add") { s ->
        Chrome("today") { TodayScreen(s, TodayFilter.TODAY, {}, noRows, {}, {}, {}) }
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.32f)))
        Surface(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
            shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
        ) {
            Column(Modifier.navigationBarsPadding()) {
                Box(
                    Modifier.align(Alignment.CenterHorizontally).padding(vertical = 12.dp)
                        .width(32.dp).height(4.dp).clip(RoundedCornerShape(2.dp))
                        .background(MaterialTheme.colorScheme.outline),
                )
                QuickAddContent(
                    GoalDraft(title = "Run 20 km", cadence = Cadence.WEEKLY, type = GoalType.QUANTITY, target = "20", unit = "km"),
                    s.today, {}, {}, {}, autoFocus = false,
                )
            }
        }
    }

    @Test fun goal_editor() = shot("09_goal_editor") { s ->
        GoalEditorScreen(s, GoalDraft.from(s.goal("read")!!).copy(reminderOn = true, reminderHour = 21), {}, {})
    }

    @Test fun settings() = shot("10_settings") { s ->
        Chrome("settings", fab = false) { SettingsScreen(s.settings, "1.0.0", {}, {}, {}, {}) }
    }

    @Test fun fire_today() = shot("11_fire_today", style = ThemeStyle.FIRE_ICE) { s ->
        Chrome("today") { TodayScreen(s, TodayFilter.TODAY, {}, noRows, {}, {}, {}) }
    }

    @Test fun fire_goals_library() = shot("12_fire_goals_library", style = ThemeStyle.FIRE_ICE) { s ->
        Chrome("goals") { GoalsScreen(s, {}, {}, {}) }
    }

    @Test fun fire_goal_detail() = shot("13_fire_goal_detail", style = ThemeStyle.FIRE_ICE) { s ->
        GoalDetailScreen(s, s.goal("run")!!, {}, {}, {}, {}, {}, {}, {}, {}, {})
    }

    @Test fun fire_project_detail() = shot("14_fire_project_detail", style = ThemeStyle.FIRE_ICE) { s ->
        ProjectDetailScreen(s, s.project("portfolio")!!, noRows, {}, {}, {}, {}, {}, {}, {}, {})
    }

    @Test fun fire_settings() = shot("15_fire_settings", style = ThemeStyle.FIRE_ICE) { s ->
        Chrome("settings", fab = false) { SettingsScreen(s.settings, "1.0.0", {}, {}, {}, {}) }
    }
}
