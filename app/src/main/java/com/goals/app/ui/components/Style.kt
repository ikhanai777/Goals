package com.goals.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.goals.app.data.Cadence
import com.goals.app.domain.Pace
import com.goals.app.ui.GoalItem
import com.goals.app.ui.theme.GoalsTheme

/**
 * Accent family for a row in the Fire & Ice style. FIRE marks what needs heat (overdue, behind
 * pace, and longer weekly-to-yearly pushes); ICE marks calm daily habits; NEUTRAL is finished work.
 */
enum class Tone { FIRE, ICE, NEUTRAL }

fun GoalItem.tone(): Tone = when {
    progress.done -> Tone.NEUTRAL
    progress.overdue || progress.pace == Pace.BEHIND -> Tone.FIRE
    goal.cadence == Cadence.DAILY -> Tone.ICE
    else -> Tone.FIRE
}

/** Solid accent for text and icons. Classic style always uses the primary color. */
@Composable
fun toneColor(tone: Tone): Color {
    val style = GoalsTheme.style
    if (!style.fireIce) return MaterialTheme.colorScheme.primary
    return when (tone) {
        Tone.FIRE -> style.fire
        Tone.ICE -> style.ice
        Tone.NEUTRAL -> GoalsTheme.extra.muted
    }
}

/** Fill for progress bars and highlighted controls. */
@Composable
fun toneBrush(tone: Tone): Brush {
    val style = GoalsTheme.style
    if (!style.fireIce) return SolidColor(MaterialTheme.colorScheme.primary)
    return when (tone) {
        Tone.FIRE -> style.fireBrush
        Tone.ICE -> style.iceBrush
        Tone.NEUTRAL -> SolidColor(GoalsTheme.extra.muted)
    }
}

/** Soft halo drawn outside the bounds: a few widening strokes with falling opacity. */
fun Modifier.glow(color: Color, radius: Dp, spread: Dp = 6.dp, strength: Float = 1f): Modifier = drawBehind {
    val steps = 4
    val r = radius.toPx()
    for (i in 1..steps) {
        val grow = spread.toPx() * i / steps
        drawRoundRect(
            color.copy(alpha = color.alpha * strength * 0.22f * (1f - (i - 1f) / steps)),
            topLeft = Offset(-grow / 2, -grow / 2),
            size = Size(size.width + grow, size.height + grow),
            cornerRadius = CornerRadius(r + grow / 2),
            style = Stroke(grow),
        )
    }
}

/**
 * Card surface used for rows and tiles. Classic: a flat tinted surface. Fire & Ice: frosted glass
 * with a glowing border in the row's [tone].
 */
@Composable
fun Modifier.card(tone: Tone? = null, shape: Shape = MaterialTheme.shapes.small, radius: Dp = 8.dp): Modifier {
    val style = GoalsTheme.style
    if (!style.fireIce) return clip(shape).background(MaterialTheme.colorScheme.surfaceContainerLow)
    val edge = when (tone) {
        null, Tone.NEUTRAL -> Color.White.copy(alpha = 0.10f)
        else -> toneColor(tone).copy(alpha = 0.75f)
    }
    val halo = if (tone == null || tone == Tone.NEUTRAL) this else glow(edge, radius)
    return halo
        .clip(shape)
        // A mostly opaque dark base keeps embers in the backdrop from crossing text.
        .background(Color(0xE6101826))
        .background(Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.07f), Color.White.copy(alpha = 0.02f))))
        .border(1.dp, edge, shape)
}
