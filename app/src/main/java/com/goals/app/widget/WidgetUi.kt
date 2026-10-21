package com.goals.app.widget

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.LinearProgressIndicator
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.goals.app.MainActivity
import com.goals.app.R
import com.goals.app.data.ThemeStyle
import androidx.glance.color.ColorProvider as DayNight

/** Widget colors for the app's two styles. Classic follows the system light/dark setting. */
class WidgetPalette(
    val background: ColorProvider,
    val card: ColorProvider,
    val text: ColorProvider,
    val muted: ColorProvider,
    val track: ColorProvider,
    /** Accent for calm/daily goals (cobalt in Classic, ice in Fire & Ice). */
    val cool: ColorProvider,
    /** Accent for urgent or longer goals (cobalt in Classic, fire in Fire & Ice). */
    val warm: ColorProvider,
    val streak: ColorProvider,
    val success: ColorProvider,
    val fireIce: Boolean,
) {
    fun accent(goal: WidgetGoal): ColorProvider = when {
        goal.done -> success
        goal.fire -> warm
        else -> cool
    }

    companion object {
        fun of(style: ThemeStyle): WidgetPalette = when (style) {
            ThemeStyle.CLASSIC -> {
                val cobalt = DayNight(Color(0xFF0047AB), Color(0xFF5B8DEF))
                WidgetPalette(
                    background = DayNight(Color.White, Color(0xFF0A0B0F)),
                    card = DayNight(Color(0xFFF5F6FA), Color(0xFF14161C)),
                    text = DayNight(Color(0xFF0A0B0F), Color(0xFFF2F3F7)),
                    muted = DayNight(Color(0xFF5B6070), Color(0xFF9AA0B0)),
                    track = DayNight(Color(0xFFE6E8F0), Color(0xFF1F222B)),
                    cool = cobalt,
                    warm = cobalt,
                    streak = DayNight(Color(0xFF6A5ACD), Color(0xFFA79BF0)),
                    success = DayNight(Color(0xFF1F7A4D), Color(0xFF5CC38E)),
                    fireIce = false,
                )
            }
            ThemeStyle.FIRE_ICE -> WidgetPalette(
                background = ColorProvider(Color(0xFF0C1522)),
                card = ColorProvider(Color(0xFF172234)),
                text = ColorProvider(Color(0xFFEAF2FB)),
                muted = ColorProvider(Color(0xFF8C9BB0)),
                track = ColorProvider(Color(0xFF2A3546)),
                cool = ColorProvider(Color(0xFF5BB8FF)),
                warm = ColorProvider(Color(0xFFFF7A2F)),
                streak = ColorProvider(Color(0xFFFF7A2F)),
                success = ColorProvider(Color(0xFF5BE3B0)),
                fireIce = true,
            )
        }
    }
}

fun openGoal(goalId: String) = actionStartActivity<MainActivity>(actionParametersOf(GoalIdKey to goalId))

fun logGoal(goalId: String) = actionRunCallback<LogGoalAction>(actionParametersOf(GoalIdKey to goalId))

/** Round log button: ring for an unchecked check goal, filled check when done, "+" otherwise. */
@Composable
fun WidgetLogButton(goal: WidgetGoal, palette: WidgetPalette, size: Dp = 40.dp, icon: Dp = 24.dp) {
    val (res, description) = when {
        goal.isCheck && goal.done -> R.drawable.widget_check to "Mark ${goal.title} not done"
        goal.isCheck -> R.drawable.widget_ring to "Mark ${goal.title} done"
        goal.done -> R.drawable.widget_check to "Log ${goal.title}"
        else -> R.drawable.widget_add to "Log ${goal.title}"
    }
    Box(
        GlanceModifier.size(size).cornerRadius(size / 2).clickable(logGoal(goal.id)),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            ImageProvider(res), description, GlanceModifier.size(icon),
            colorFilter = ColorFilter.tint(palette.accent(goal)),
        )
    }
}

@Composable
fun WidgetStreak(streak: Int, palette: WidgetPalette, fontSize: Int = 11, iconSize: Dp = 11.dp) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Image(
            ImageProvider(R.drawable.widget_flame), null, GlanceModifier.size(iconSize),
            colorFilter = ColorFilter.tint(palette.streak),
        )
        Text("$streak", style = TextStyle(color = palette.streak, fontSize = fontSize.sp, fontWeight = FontWeight.Medium))
    }
}

/** One compact goal row: accent bar, title, value, progress bar, streak and log button. */
@Composable
fun WidgetGoalRow(goal: WidgetGoal, palette: WidgetPalette, showStreak: Boolean) {
    val accent = palette.accent(goal)
    Row(
        GlanceModifier
            .fillMaxWidth()
            .background(palette.card)
            .cornerRadius(10.dp)
            .clickable(openGoal(goal.id)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(GlanceModifier.width(3.dp).height(30.dp).background(accent).cornerRadius(2.dp)) {}
        Spacer(GlanceModifier.width(10.dp))
        Column(GlanceModifier.defaultWeight().padding(vertical = 8.dp)) {
            Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    goal.title,
                    style = TextStyle(color = if (goal.done) palette.muted else palette.text, fontSize = 13.sp),
                    maxLines = 1,
                    modifier = GlanceModifier.defaultWeight(),
                )
                Spacer(GlanceModifier.width(6.dp))
                Text(
                    goal.progressLabel,
                    style = TextStyle(
                        color = if (palette.fireIce && !goal.done) accent else palette.muted,
                        fontSize = 12.sp, fontWeight = FontWeight.Medium,
                    ),
                    maxLines = 1,
                )
            }
            Spacer(GlanceModifier.height(5.dp))
            Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                LinearProgressIndicator(
                    progress = goal.fraction,
                    modifier = GlanceModifier.defaultWeight().height(4.dp),
                    color = accent,
                    backgroundColor = palette.track,
                )
                if (showStreak && goal.streak >= 2) {
                    Spacer(GlanceModifier.width(6.dp))
                    WidgetStreak(goal.streak, palette)
                }
            }
        }
        WidgetLogButton(goal, palette)
    }
}
