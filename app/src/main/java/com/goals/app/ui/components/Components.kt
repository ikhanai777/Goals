package com.goals.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.MoreTime
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.goals.app.data.GoalType
import com.goals.app.domain.badge
import com.goals.app.domain.formatProgress
import com.goals.app.ui.GoalItem
import com.goals.app.ui.theme.AreaColors
import com.goals.app.ui.theme.GoalsTheme
import com.goals.app.ui.theme.NumericStyle

@Composable
fun ProgressBar(
    fraction: Float,
    modifier: Modifier = Modifier,
    height: Dp = 4.dp,
    color: Color = MaterialTheme.colorScheme.primary,
    brush: Brush = SolidColor(color),
    glowColor: Color? = null,
) {
    val animated by animateFloatAsState(fraction.coerceIn(0f, 1f), tween(200), label = "progress")
    val track = GoalsTheme.extra.track
    Canvas(modifier.height(height)) {
        val r = CornerRadius(size.height / 2)
        drawRoundRect(track, cornerRadius = r)
        if (animated <= 0f) return@Canvas
        val w = size.width * animated
        if (glowColor != null) {
            // Halo around the filled part only.
            for (i in 1..3) {
                val g = size.height * i * 0.8f
                drawRoundRect(
                    glowColor.copy(alpha = 0.16f / i),
                    topLeft = Offset(-g / 2, -g / 2),
                    size = Size(w + g, size.height + g),
                    cornerRadius = CornerRadius(size.height / 2 + g / 2),
                )
            }
        }
        drawRoundRect(brush, size = Size(w, size.height), cornerRadius = r)
    }
}

@Composable
fun ProgressRing(
    fraction: Float,
    modifier: Modifier = Modifier,
    size: Dp = 28.dp,
    stroke: Dp = 3.dp,
    color: Color = MaterialTheme.colorScheme.primary,
    content: @Composable () -> Unit = {},
) {
    val animated by animateFloatAsState(fraction.coerceIn(0f, 1f), tween(200), label = "ring")
    val track = GoalsTheme.extra.track
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val w = stroke.toPx()
            val inset = w / 2
            val arcSize = Size(this.size.width - w, this.size.height - w)
            drawArc(track, 0f, 360f, false, Offset(inset, inset), arcSize, style = Stroke(w))
            drawArc(color, -90f, 360f * animated, false, Offset(inset, inset), arcSize, style = Stroke(w, cap = StrokeCap.Round))
        }
        content()
    }
}

/** Round checkbox for CHECK goals, "+" for the other types. */
@Composable
fun LogControl(item: GoalItem, onLog: () -> Unit, modifier: Modifier = Modifier) {
    val haptics = LocalHapticFeedback.current
    val scheme = MaterialTheme.colorScheme
    val fireIce = GoalsTheme.style.fireIce
    val accent = toneColor(item.tone())
    // Classic uses the cobalt container; Fire & Ice tints with the row's tone.
    val container = if (fireIce) accent.copy(alpha = 0.18f) else scheme.primaryContainer
    val onContainer = if (fireIce) accent else scheme.onPrimaryContainer
    val ring = if (fireIce) accent.copy(alpha = 0.7f) else scheme.outline
    val met = item.progress.met
    val description = "${item.goal.title}, ${item.goal.formatProgress(item.progress.value)}, " +
        when {
            item.goal.type == GoalType.CHECK && met -> "mark not done"
            item.goal.type == GoalType.CHECK -> "mark done"
            else -> "log progress"
        }
    Box(
        modifier
            .size(48.dp)
            .clip(CircleShape)
            .clickable(role = Role.Button) {
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onLog()
            }
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        when {
            item.goal.type == GoalType.CHECK && met -> Box(
                Modifier.size(24.dp).clip(CircleShape).background(scheme.primary),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Rounded.Check, null, Modifier.size(16.dp), tint = scheme.onPrimary) }
            item.progress.skipped -> Box(
                Modifier.size(24.dp).clip(CircleShape).border(1.5.dp, scheme.secondary, CircleShape),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Rounded.SkipNext, null, Modifier.size(14.dp), tint = scheme.secondary) }
            item.goal.type == GoalType.CHECK -> Box(
                Modifier.size(24.dp).clip(CircleShape).border(1.5.dp, ring, CircleShape),
            )
            else -> Box(
                Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(if (met) scheme.primary else container),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    when {
                        met -> Icons.Rounded.Check
                        item.goal.type == GoalType.DURATION -> Icons.Rounded.MoreTime
                        else -> Icons.Rounded.Add
                    },
                    null,
                    Modifier.size(18.dp),
                    tint = if (met) scheme.onPrimary else onContainer,
                )
            }
        }
    }
}

@Composable
fun AreaBar(colorIndex: Int?, modifier: Modifier = Modifier, color: Color? = null) {
    val fill = color ?: colorIndex?.let { AreaColors[it % AreaColors.size] } ?: Color.Transparent
    Box(
        modifier
            .width(4.dp)
            .fillMaxHeight()
            .then(if (color != null && GoalsTheme.style.fireIce) Modifier.glow(fill, 2.dp, spread = 8.dp, strength = 1.6f) else Modifier)
            .clip(RoundedCornerShape(2.dp))
            .background(fill),
    )
}

@Composable
fun StreakBadge(streak: Int, color: Color = MaterialTheme.colorScheme.secondary) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Rounded.LocalFireDepartment, null, Modifier.size(12.dp), tint = color)
        Text(
            "$streak",
            style = NumericStyle.copy(fontSize = MaterialTheme.typography.labelSmall.fontSize),
            color = color,
        )
    }
}

/** The compact Today row: area bar, icon, title, value, progress bar, streak and log control. */
@Composable
fun GoalRowContent(
    item: GoalItem,
    showStreak: Boolean,
    onOpen: () -> Unit,
    onLog: () -> Unit,
    modifier: Modifier = Modifier,
    meta: String? = null,
) {
    val scheme = MaterialTheme.colorScheme
    val fireIce = GoalsTheme.style.fireIce
    val p = item.progress
    val tone = item.tone()
    val accent = toneColor(tone)
    val barColor = when {
        p.overdue -> scheme.error
        p.met -> GoalsTheme.extra.success
        else -> scheme.primary
    }
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = GoalsTheme.rowHeight)
            .card(tone)
            .clickable(onClick = onOpen)
            .padding(start = 0.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AreaBar(
            item.area?.colorIndex, Modifier.height(GoalsTheme.rowHeight - 16.dp),
            color = if (fireIce && tone != Tone.NEUTRAL) accent else null,
        )
        Spacer(Modifier.width(10.dp))
        Icon(
            iconFor(item.goal.icon), null, Modifier.size(20.dp),
            tint = when {
                p.done -> GoalsTheme.extra.muted
                fireIce -> accent
                else -> scheme.onSurface
            },
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f).padding(vertical = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    item.goal.title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (p.done) GoalsTheme.extra.muted else scheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    item.goal.formatProgress(p.value), style = NumericStyle,
                    color = if (fireIce && !p.done) accent else GoalsTheme.extra.muted,
                )
            }
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (fireIce) {
                    ProgressBar(p.fraction, Modifier.weight(1f), brush = toneBrush(tone), glowColor = accent.takeIf { tone != Tone.NEUTRAL })
                } else {
                    ProgressBar(p.fraction, Modifier.weight(1f), color = barColor)
                }
                if (showStreak && p.currentStreak >= 2) {
                    Spacer(Modifier.width(8.dp))
                    StreakBadge(p.currentStreak, if (fireIce) accent else MaterialTheme.colorScheme.secondary)
                }
            }
            if (meta != null && GoalsTheme.rowHeight > 48.dp) {
                Spacer(Modifier.height(4.dp))
                Text(
                    meta,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (p.overdue) scheme.error else GoalsTheme.extra.muted,
                    maxLines = 1,
                )
            }
        }
        Spacer(Modifier.width(4.dp))
        LogControl(item, onLog)
    }
}

/** [GoalRowContent] with swipe right to log and swipe left to skip. Swipes snap back after acting. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeGoalRow(
    item: GoalItem,
    showStreak: Boolean,
    onOpen: () -> Unit,
    onLog: () -> Unit,
    onQuickLog: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier,
    meta: String? = null,
) {
    val haptics = LocalHapticFeedback.current
    val state = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.StartToEnd -> { haptics.performHapticFeedback(HapticFeedbackType.LongPress); onQuickLog() }
                SwipeToDismissBoxValue.EndToStart -> { haptics.performHapticFeedback(HapticFeedbackType.LongPress); onSkip() }
                SwipeToDismissBoxValue.Settled -> Unit
            }
            false
        },
    )
    SwipeToDismissBox(
        state = state,
        modifier = modifier.semantics {
            customActions = listOf(
                CustomAccessibilityAction("Log") { onQuickLog(); true },
                CustomAccessibilityAction("Skip") { onSkip(); true },
            )
        },
        enableDismissFromStartToEnd = !(item.goal.type == GoalType.CHECK && item.progress.met),
        enableDismissFromEndToStart = !item.progress.done,
        backgroundContent = {
            // Only while dragging: Fire & Ice cards are translucent and would show it at rest.
            if (state.dismissDirection == SwipeToDismissBoxValue.Settled) return@SwipeToDismissBox
            val toEnd = state.dismissDirection == SwipeToDismissBoxValue.StartToEnd
            val scheme = MaterialTheme.colorScheme
            Row(
                Modifier
                    .fillMaxSize()
                    .clip(MaterialTheme.shapes.small)
                    .background(if (toEnd) scheme.primary else scheme.secondary)
                    .padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = if (toEnd) Arrangement.Start else Arrangement.End,
            ) {
                val tint = if (toEnd) scheme.onPrimary else scheme.onSecondary
                Icon(if (toEnd) Icons.Rounded.Check else Icons.Rounded.SkipNext, null, Modifier.size(20.dp), tint = tint)
                Spacer(Modifier.width(6.dp))
                Text(if (toEnd) "Log" else "Skip", style = MaterialTheme.typography.labelMedium, color = tint)
            }
        },
    ) {
        GoalRowContent(item, showStreak, onOpen, onLog, meta = meta)
    }
}

@Composable
fun SectionHeader(
    title: String,
    count: String?,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurface,
    expanded: Boolean? = null,
    onToggle: (() -> Unit)? = null,
) {
    Row(
        modifier
            .fillMaxWidth()
            .then(if (onToggle != null) Modifier.clickable(onClick = onToggle) else Modifier)
            .padding(top = 16.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = MaterialTheme.typography.titleSmall, color = color)
        if (count != null) {
            Spacer(Modifier.width(6.dp))
            Text(count, style = NumericStyle, color = GoalsTheme.extra.muted)
        }
        Spacer(Modifier.weight(1f))
        if (expanded != null) {
            Icon(
                Icons.Rounded.ExpandMore, if (expanded) "Collapse" else "Expand",
                Modifier.size(20.dp).rotate(if (expanded) 180f else 0f),
                tint = GoalsTheme.extra.muted,
            )
        }
    }
}

@Composable
fun CadenceBadge(item: GoalItem) {
    Box(
        Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .padding(horizontal = 5.dp, vertical = 1.dp),
    ) {
        Text(
            item.goal.cadence.badge(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
        )
    }
}

@Composable
fun StatTile(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier
            .card()
            .padding(horizontal = 10.dp, vertical = 8.dp),
    ) {
        Text(value, style = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = "tnum"))
        Text(label, style = MaterialTheme.typography.labelSmall, color = GoalsTheme.extra.muted, maxLines = 1)
    }
}

@Composable
fun EmptyState(text: String, action: String?, onAction: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxWidth().padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text, style = MaterialTheme.typography.bodyMedium, color = GoalsTheme.extra.muted)
        if (action != null) {
            androidx.compose.material3.TextButton(onClick = onAction) { Text(action) }
        }
    }
}

/** Pill-shaped single-choice control; in Fire & Ice the selected segment burns orange. */
@Composable
fun <T> SegmentedControl(
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val style = GoalsTheme.style
    val shape = RoundedCornerShape(50)
    val edge = if (style.fireIce) style.ice.copy(alpha = 0.45f) else scheme.outline
    Row(
        modifier
            .fillMaxWidth()
            .height(38.dp)
            .then(if (style.fireIce) Modifier.glow(style.ice.copy(alpha = 0.5f), 19.dp, spread = 5.dp) else Modifier)
            .clip(shape)
            .background(if (style.fireIce) Color.White.copy(alpha = 0.05f) else scheme.background)
            .border(1.dp, edge, shape),
    ) {
        options.forEachIndexed { i, option ->
            val on = option == selected
            if (i > 0) Box(Modifier.width(1.dp).fillMaxHeight().background(edge))
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .then(
                        when {
                            !on -> Modifier
                            style.fireIce -> Modifier.background(style.fireBrush)
                            else -> Modifier.background(scheme.primary)
                        },
                    )
                    .clickable(role = Role.Tab) { onSelect(option) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label(option),
                    style = MaterialTheme.typography.labelLarge,
                    color = when {
                        on -> if (style.fireIce) Color.White else scheme.onPrimary
                        else -> scheme.onSurface
                    },
                )
            }
        }
    }
}
